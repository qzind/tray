/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.common.Constants;
import qz.utils.ShellUtilities;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Tres Finocchiaro
 */
class MacDeploy extends DeployUtilities {

    private static final Logger log = LoggerFactory.getLogger(MacDeploy.class);

    private String desktopShortcut = System.getProperty("user.home") + "/Desktop/" + getShortcutName();

    @Override
    public String getJarPath() {
        String jarPath = super.getJarPath();
        try {
            jarPath = URLDecoder.decode(jarPath, "UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            log.error("Error decoding URL: {}", jarPath, e);
        }

        return jarPath;
    }

    private String getJarName() {
        return new File(getJarPath()).getName();
    }

    /**
     * Verify LaunchAgents plist file exists and parse it to verify it's enabled
     */
    @Override
    public boolean canAutoStart() {
        // FIXME: removeLegacyStartup should only run once per machine
        removeLegacyStartup();

        // plist is stored as io.qz.plist
        String parent = "/Library/LaunchAgents";
        String[] parts = Constants.ABOUT_URL.split("/");
        parts = parts[parts.length - 1].split("\\.");
        String plist = parts[1] + "." + parts[0] + "." + Constants.PROPS_FILE + ".plist";
        Path plistPath = Paths.get(parent, plist);

        if (Files.exists(plistPath)) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(plistPath.toFile());
                doc.getDocumentElement().normalize();

                NodeList dictList = doc.getElementsByTagName("dict");

                // Loop to find "RunAtLoad" key, then the adjacent key
                boolean foundItem = false;
                if (dictList.getLength() > 0) {
                    NodeList children = dictList.item(0).getChildNodes();
                    for(int n = 0; n < children.getLength(); n++) {
                        Node item = children.item(n);
                        // Apple stores booleans as adjacent tags to their owner
                        if (foundItem) {
                            String nodeName = children.item(n).getNodeName();
                            log.debug("Found RunAtLoad value {}", nodeName);
                            return "true".equals(nodeName);
                        }
                        if (item.getNodeName().equals("key") && item.getTextContent().equals("RunAtLoad")) {
                            log.debug("Found RunAtLoad key in {}", plistPath);
                            foundItem = true;
                        }
                    }
                }
                log.warn("RunAtLoad was not in plist {}, autostart will not work.", plistPath);
            }
            catch(SAXException | IOException | ParserConfigurationException e) {
                log.warn("Error reading plist {}, autostart will not work.", plistPath, e);
            }
        } else {
            log.warn("No plist {} found, autostart will not work", plistPath);
        }
        return false;
    }

    @Override
    public boolean createDesktopShortcut() {
        return ShellUtilities.execute(new String[] {"ln", "-sf", getAppPath(), desktopShortcut});
    }

    private boolean removeLegacyStartup() {
        return ShellUtilities.executeAppleScript(
                "tell application \"System Events\" to delete "
                        + "every login item where name is \"" + getShortcutName() + "\" or "
                        + "name is \"" + getJarName() + "\""
        );
    }

    /**
     * Returns path to executable jar or app bundle
     */
    private String getAppPath() {
        String target = getJarPath();
        if (target.contains("/Applications/")) {
            // Use the parent folder instead i.e. "/Applications/QZ Tray.app"
            File f = new File(getJarPath());
            if (f.getParent() != null) {
                return f.getParent();
            }
        }
        return target;
    }
}
