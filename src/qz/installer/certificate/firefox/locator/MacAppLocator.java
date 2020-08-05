package qz.installer.certificate.firefox.locator;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.utils.ShellUtilities;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MacAppLocator extends AppLocator{
    protected static final Logger log = LoggerFactory.getLogger(MacAppLocator.class);

    private static String[] BLACKLIST = new String[]{ "/Volumes/", "/.Trash/", "/Applications (Parallels)/" };

    /**
     * Helper class for finding key/value siblings from the DDM
     */
    private enum SiblingNode {
        NAME("_name"),
        PATH("path"),
        VERSION("version");

        private String key;
        private boolean wants;

        SiblingNode(String key) {
            this.key = key;
            this.wants = false;
        }

        private boolean isKey(Node node) {
            if (node.getNodeName().equals("key") && node.getTextContent().equals(key)) {
                this.wants = true;
                return true;
            }
            return false;
        }

        private void set(Node node, AppInfo info) {
            switch(this) {
                case NAME: info.setName(node.getTextContent()); break;
                case PATH: info.setPath(Paths.get(node.getTextContent())); break;
                case VERSION: info.setVersion(node.getTextContent()); break;
                default: throw new UnsupportedOperationException(this.name() + " not supported");
            }
            wants = false;
        }
    }

    @Override
    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        Document doc;

        try {
            // system_profile benchmarks about 30% better than lsregister
            Process p = Runtime.getRuntime().exec(new String[] {"system_profiler", "SPApplicationsDataType", "-xml"}, ShellUtilities.envp);
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(p.getInputStream());
        } catch(IOException | ParserConfigurationException | SAXException e) {
            log.warn("Could not retrieve app listing for {}", appAlias.name(), e);
            return appList;
        }
        doc.normalizeDocument();

        NodeList nodeList = doc.getElementsByTagName("dict");
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList dict = nodeList.item(i).getChildNodes();
            AppInfo appInfo = new AppInfo();
            for (int j = 0; j < dict.getLength(); j++) {
                Node node = dict.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    for (SiblingNode sibling : SiblingNode.values()) {
                        if (sibling.wants) {
                            sibling.set(node, appInfo);
                            break;
                        } else if(sibling.isKey(node)) {
                            break;
                        }
                    }
                }
            }
            if (appAlias.setBundleId(appInfo)) {
                appList.add(appInfo);
            }
        }

        for(AppInfo appInfo : appList) {
            // Mark blacklisted locations
            for(String listEntry : BLACKLIST) {
                if (appInfo.getPath() != null && appInfo.getPath().toString().matches(Pattern.quote(listEntry))) {
                    appInfo.setBlacklisted(true);
                }
            }
            // Calculate exePath
            appInfo.setExePath(getExePath(appInfo));
        }
        return appList;
    }

    @Override
    public ArrayList<Path> getPidPaths(ArrayList<String> pids) {
        ArrayList<Path> processPaths = new ArrayList();
        for (String pid : pids) {
            Pointer buf = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
            SystemB.INSTANCE.proc_pidpath(Integer.parseInt(pid), buf, SystemB.PROC_PIDPATHINFO_MAXSIZE);
            processPaths.add(Paths.get(buf.getString(0).trim()));
        }
        return processPaths;
    }

    /**
     * Calculate executable path by parsing Contents/Info.plist
     */
    private static Path getExePath(AppInfo appInfo) {
        Path plist = appInfo.getPath().resolve("Contents/Info.plist");
        Document doc;
        try {
            if(!plist.toFile().exists()) {
                log.warn("Could not locate plist file for {}: {}",  appInfo.getName(), plist);
                return null;
            }
            // Convert potentially binary plist files to XML
            Process p = Runtime.getRuntime().exec(new String[] {"plutil", "-convert", "xml1", plist.toAbsolutePath().toString(), "-o", "-"}, ShellUtilities.envp);
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(p.getInputStream());
        } catch(IOException | ParserConfigurationException | SAXException e) {
            log.warn("Could not parse plist file for {}: {}", appInfo.getName(), plist, e);
            return null;
        }
        doc.normalizeDocument();

        boolean upNext = false;
        NodeList nodeList = doc.getElementsByTagName("dict");
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList dict = nodeList.item(i).getChildNodes();
            for(int j = 0; j < dict.getLength(); j++) {
                Node node = dict.item(j);
                if ("key".equals(node.getNodeName()) && node.getTextContent().equals("CFBundleExecutable")) {
                    upNext = true;
                } else if (upNext && "string".equals(node.getNodeName())) {
                    return appInfo.getPath().resolve("Contents/MacOS/" + node.getTextContent());
                }
            }
        }
        return null;
    }

    private interface SystemB extends Library {
        SystemB INSTANCE = Native.loadLibrary("System", SystemB.class);
        int PROC_ALL_PIDS = 1;
        int PROC_PIDPATHINFO_MAXSIZE = 1024 * 4;
        int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);
        int proc_listpids(int type, int typeinfo, int[] buffer, int buffersize);
        int proc_pidpath(int pid, Pointer buffer, int buffersize);
    }
}
