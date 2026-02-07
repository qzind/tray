package qz.installer.apps.locator;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.utils.ShellUtilities;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/*
 * Apple's XML structure
 *  <array>
 *      <dict>
 *          <array>
 *              <dict>
 *                  <dict>
 *                      <key>_name</key>
 *                      <string>Firefox</string>
 *                      <key>info</key>
 *                      <string>Firefox 146.0</string>
 *                      <key>path</key>
 *                      <string>/Applications/Firefox.app</string>
 *                      <key>version</key>
 *                      <string>146.0</string>
 *                  </dict>
 *              </dict>
 *              <dict>
 *                  <!-- ... -->
 *              </dict>
 *          </array>
 *     <dict>
 *  </array>
 */

/**
 * Parses XML output from <code>system_profiler</code> to get application info
 */
public class MacAppLocator extends AppLocator {
    protected static final Logger log = LogManager.getLogger(MacAppLocator.class);

    // If apps are found here, ignore them
    private static final String[] IGNORE_PATHS = {
            "/Volumes/",
            "/.Trash/",
            "/Applications (Parallels)/"
    };

    @Override
    public HashSet<AppInfo> locate(AppAlias appAlias) {
        HashSet<AppInfo> appList = new HashSet<>();
        try {
            // system_profile benchmarks about 30% better than lsregister
            Process p = Runtime.getRuntime().exec(new String[] {"system_profiler", "SPApplicationsDataType", "-xml"}, ShellUtilities.envp);
            List<Node> dicts = getApplicationDicts(createCompatibleDocument(p.getInputStream()));

            for(Node dict : dicts) {
                String name = getSiblingValue(dict, "_name");
                if (name == null) continue;
                String path = getSiblingValue(dict, "path");
                if (path == null) continue;
                String version = getSiblingValue(dict, "version");
                if (version == null) continue;

                AppAlias.Alias alias;
                // For some reason Firefox is called "Firefox.app"
                // All others are called "Google Chrome.app", "Microsoft Edge.app", etc
                boolean stripVendor = appAlias == AppAlias.FIREFOX;
                if ((alias = AppAlias.findAlias(appAlias, name, stripVendor)) != null) {
                    appList.add(new AppInfo(alias, Paths.get(path), parseExePath(path), version));
                }
            }
        } catch(ParserConfigurationException | IOException | SAXException e) {
            log.warn("Something went wrong getting app info for {}", appAlias);
        }

        // Cleanup bad paths such as mount points, Trash, Parallels' shared apps
        appList.removeIf(app ->
                                 Arrays.stream(IGNORE_PATHS).anyMatch(app.getPath().toString()::contains)
        );

        // Remove "EdgeUpdater" and friends
        appList.removeIf(app ->
                                 Arrays.stream(appAlias.aliases).map(alias -> String.format("%sUpdater", alias.getName(true))).anyMatch(
                                         updater -> app.getPath().toString().contains(updater)
                                 )
        );

        return appList;
    }


    /**
     * Use JNA to obtain the
     */
    @Override
    public HashSet<Path> getPidPaths(HashSet<String> pids) {
        HashSet<Path> processPaths = new HashSet<>();
        for (String pid : pids) {
            Pointer buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
            SystemB.INSTANCE.proc_pidpath(Integer.parseInt(pid), buf, SystemB.PROC_PIDPATHINFO_MAXSIZE);
            processPaths.add(Paths.get(buf.getString(0).trim()));
        }
        return processPaths;
    }

    /**
     * Special key/string sibling handler
     */
    public static String getSiblingValue(Node dict, String keyValue) {
        NodeList pairs = dict.getChildNodes();
        for(int i = 0; i < pairs.getLength(); i++) {
            Node key = pairs.item(i);
            // Find <key>_name</name>, <key>path</key>, <key>version</key>, etc
            if("key".equals(key.getNodeName()) && keyValue.equals(key.getTextContent())) {
                Node value = key.getNextSibling();
                // Only support string types for now
                if("string".equals(value.getNodeName())) {
                    String textContent = value.getTextContent();
                    if(textContent != null && !textContent.isBlank()) {
                        return textContent;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Use a shortcut system to search for <code>&lt;key&gt;_name&lt;/key&gt;</code> and then build our app list
     * from each parent <code>&lt;dict&gt;</code> node.
     * <p>
     * Apple uses several nested levels of <code>&lt;dict&gt;</code>, so although this two-pass approach is slightly more
     * expensive than a one-pass sibling search, it's much easier to read and debug, so we'll accept the slight performance
     * hit for our own sanity.
     */
    public static List<Node> getApplicationDicts(Document doc) {
        List<Node> dicts = new ArrayList<>();

        NodeList keys = doc.getElementsByTagName("key");
        for(int i = 0; i < keys.getLength(); i++) {
            Node node = keys.item(i);
            if("_name".equals(node.getTextContent())) {
                Node parent = node.getParentNode();
                if("dict".equals(parent.getNodeName())) {
                    // We've found a structure that looks like this, add it!
                    //
                    // <dict>
                    //   <key>_name</key>
                    //   <string>Firefox</string>
                    //   <!-- ... -->
                    // </dict>
                    dicts.add(parent);
                }
            }
        }
        return dicts;
    }

    /**
     * Calculate executable path by parsing Contents/Info.plist
     */
    private static Path parseExePath(String appPath) {
        Path path = Paths.get(appPath).toAbsolutePath().normalize();
        Path plist = path.resolve("Contents/Info.plist");
        Document doc;
        try {
            if(!plist.toFile().exists()) {
                log.warn("Could not locate plist file for {}: {}",  appPath, plist);
                return null;
            }
            // Convert potentially binary plist files to XML
            Process p = Runtime.getRuntime().exec(new String[] {"plutil", "-convert", "xml1", plist.toString(), "-o", "-"}, ShellUtilities.envp);
            doc = createCompatibleDocument(p.getInputStream());
        } catch(IOException | ParserConfigurationException | SAXException e) {
            log.warn("Could not parse plist file for {}: {}", appPath, plist, e);
            return null;
        }

        //boolean upNext = false;
        NodeList nodeList = doc.getElementsByTagName("dict");
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList dict = nodeList.item(i).getChildNodes();
            for(int j = 0; j < dict.getLength(); j++) {
                Node node = dict.item(j);
                if ("key".equals(node.getNodeName()) && node.getTextContent().equals("CFBundleExecutable")) {
                    Node value = node.getNextSibling();
                    if("string".equals(value.getNodeName())) {
                        String textContent = value.getTextContent();
                        if(textContent != null && !textContent.isBlank()) {
                            return path.resolve("Contents/MacOS/" + textContent);
                        }
                    }
                    // If we found the key but not a value, abort
                    return null;
                }
            }
        }
        return null;
    }


    @SuppressWarnings("unused")
    private interface SystemB extends Library {
        SystemB INSTANCE = Native.load("System", SystemB.class);
        int PROC_ALL_PIDS = 1;
        int PROC_PIDPATHINFO_MAXSIZE = 1024 * 4;
        @SuppressWarnings("UnusedReturnValue")
        int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);
        @SuppressWarnings("UnusedReturnValue")
        int proc_listpids(int type, int typeinfo, int[] buffer, int buffersize);
        @SuppressWarnings("UnusedReturnValue")
        int proc_pidpath(int pid, Pointer buffer, int buffersize);
    }

    private static Document createCompatibleDocument(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf  = DocumentBuilderFactory.newInstance();
        // don't let the <!DOCTYPE> fail parsing per https://github.com/qzind/tray/issues/809
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // fix erroneous "\r\n", ignored unless setValidating(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setValidating(true);
        Document doc = dbf.newDocumentBuilder().parse(is);
        doc.normalizeDocument();
        return doc;
    }
}
