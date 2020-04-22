package qz.installer.certificate.firefox.locator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.utils.ShellUtilities;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MacAppLocator {
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
                case PATH: info.setPath(node.getTextContent()); break;
                case VERSION: info.setVersion(node.getTextContent()); break;
                default: throw new UnsupportedOperationException(this.name() + " not supported");
            }
            wants = false;
        }
    }

    public static ArrayList<AppInfo> findApp(AppAlias appAlias) {
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
            if (appAlias.matches(appInfo)) {
                appList.add(appInfo);
            }
        }

        for(AppInfo appInfo : appList) {
            // Mark blacklisted locations
            for(String listEntry : BLACKLIST) {
                if (appInfo.getPath() != null && appInfo.getPath().matches(Pattern.quote(listEntry))) {
                    appInfo.setBlacklisted(true);
                }
            }
            // Calculate exePath
            appInfo.setExePath(getExePath(appInfo));
        }
        return appList;
    }

    /**
     * Calculate executable path by parsing Contents/Info.plist
     */
    private static String getExePath(AppInfo appInfo) {
        File plist = new File(appInfo.getPath(), "Contents/Info.plist");
        Document doc;
        try {
            if(!plist.exists()) {
                log.warn("Could not locate plist file for {}: {}",  appInfo.getName(), plist);
                return null;
            }
            // Convert potentially binary plist files to XML
            Process p = Runtime.getRuntime().exec(new String[] {"plutil", "-convert", "xml1", plist.getCanonicalPath(), "-o", "-"}, ShellUtilities.envp);
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
                    return String.format(appInfo.getPath() + "/Contents/MacOS/" + node.getTextContent());
                }
            }
        }
        return null;
    }
}
