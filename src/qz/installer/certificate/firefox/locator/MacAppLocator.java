package qz.installer.certificate.firefox.locator;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MacAppLocator extends AppLocator{
    protected static final Logger log = LogManager.getLogger(MacAppLocator.class);

    private static String[] BLACKLISTED_PATHS = new String[]{"/Volumes/", "/.Trash/", "/Applications (Parallels)/" };

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
                return true;
            }
            return false;
        }
    }

    @Override
    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        Document doc;

        try {
            // system_profile benchmarks about 30% better than lsregister
            Process p = Runtime.getRuntime().exec(new String[] {"system_profiler", "SPApplicationsDataType", "-xml"}, ShellUtilities.envp);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // don't let the <!DOCTYPE> fail parsing per https://github.com/qzind/tray/issues/809
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            doc = dbf.newDocumentBuilder().parse(p.getInputStream());
        } catch(IOException | ParserConfigurationException | SAXException e) {
            log.warn("Could not retrieve app listing for {}", appAlias.name(), e);
            return appList;
        }
        doc.normalizeDocument();

        NodeList nodeList = doc.getElementsByTagName("dict");
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList dict = nodeList.item(i).getChildNodes();
            HashMap<SiblingNode, String> foundApp = new HashMap<>();
            for (int j = 0; j < dict.getLength(); j++) {
                Node node = dict.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    for (SiblingNode sibling : SiblingNode.values()) {
                        if (sibling.wants) {
                            foundApp.put(sibling, node.getTextContent());
                            sibling.wants = false;
                            break;
                        } else if(sibling.isKey(node)) {
                            sibling.wants = true;
                            break;
                        }
                    }
                }
            }
            AppAlias.Alias alias;
            if((alias = AppAlias.findAlias(appAlias, foundApp.get(SiblingNode.NAME), true)) != null) {
                appList.add(new AppInfo(alias, Paths.get(foundApp.get(SiblingNode.PATH)),
                        getExePath(foundApp.get(SiblingNode.PATH)), foundApp.get(SiblingNode.VERSION)
                ));
            }
        }

        // Remove blacklisted paths
        Iterator<AppInfo> appInfoIterator = appList.iterator();
        while(appInfoIterator.hasNext()) {
            AppInfo appInfo = appInfoIterator.next();
            for(String listEntry : BLACKLISTED_PATHS) {
                if (appInfo.getPath() != null && appInfo.getPath().toString().contains(listEntry)) {
                    appInfoIterator.remove();
                }
            }
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
    private static Path getExePath(String appPath) {
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
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(p.getInputStream());
        } catch(IOException | ParserConfigurationException | SAXException e) {
            log.warn("Could not parse plist file for {}: {}", appPath, appPath, e);
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
                    return path.resolve("Contents/MacOS/" + node.getTextContent());
                }
            }
        }
        return null;
    }

    private interface SystemB extends Library {
        SystemB INSTANCE = Native.load("System", SystemB.class);
        int PROC_ALL_PIDS = 1;
        int PROC_PIDPATHINFO_MAXSIZE = 1024 * 4;
        int sysctlbyname(String name, Pointer oldp, IntByReference oldlenp, Pointer newp, int newlen);
        int proc_listpids(int type, int typeinfo, int[] buffer, int buffersize);
        int proc_pidpath(int pid, Pointer buffer, int buffersize);
    }
}
