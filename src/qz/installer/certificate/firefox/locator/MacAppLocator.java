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
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MacAppLocator extends AppLocator {
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

        private void set(Node node, AppLocator info) {
            switch(this) {
                case NAME: info.setName(node.getTextContent()); break;
                case PATH: info.setPath(node.getTextContent()); break;
                case VERSION: info.setVersion(node.getTextContent()); break;
                default: throw new UnsupportedOperationException(this.name() + " not supported");
            }
            wants = false;
        }
    }

    public boolean isBlacklisted() {
        for (String item : BLACKLIST) {
            if (path != null && path.matches(Pattern.quote(item))) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<AppLocator> findApp(AppAlias appAlias) {
        ArrayList<AppLocator> appList = new ArrayList<>();
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
            MacAppLocator info = new MacAppLocator();
            for (int j = 0; j < dict.getLength(); j++) {
                Node node = dict.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    for (SiblingNode sibling : SiblingNode.values()) {
                        if (sibling.wants) {
                            sibling.set(node, info);
                            break;
                        } else if(sibling.isKey(node)) {
                            break;
                        }
                    }
                }
            }
            if (appAlias.matches(info)) {
                appList.add(info);
            }
        }
        return appList;
    }
}
