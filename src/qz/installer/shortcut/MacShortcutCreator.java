/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.installer.shortcut;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.common.Constants;
import qz.utils.MacUtilities;
import qz.utils.SystemUtilities;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Tres Finocchiaro
 */
class MacShortcutCreator extends ShortcutCreator {

    private static final Logger log = LogManager.getLogger(MacShortcutCreator.class);
    private static final Path SHORTCUT_PATH = Paths.get(System.getProperty("user.home") + "/Desktop/" + Constants.ABOUT_TITLE);


    public static void main(String ... args) {
        boolean can = getInstance().canAutoStart();
    }
    /**
     * Verify LaunchAgents plist file exists and parse it to verify it's enabled
     */
    @Override
    public boolean canAutoStart() {
        // plist is stored as io.qz.plist
        Path plistPath = Paths.get("/Library/LaunchAgents", MacUtilities.getBundleId() +  ".plist");

        if (Files.exists(plistPath)) {
            try {
                Document doc = MacUtilities.createXmlDocument(plistPath);
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

    public void createDesktopShortcut() {
        try {
            if(SHORTCUT_PATH.toFile().exists() && !SHORTCUT_PATH.toFile().delete()) {
                throw new IOException(String.format("Unable to delete '%s', we can't create a the shortcut", SHORTCUT_PATH));
            }
            if(SystemUtilities.getJarPath() == null) {
                throw new IOException(String.format("Unable to create '%s', the jar location is null", SHORTCUT_PATH));
            }
            Files.createSymbolicLink(SHORTCUT_PATH, SystemUtilities.isInstalled() ?
                    SystemUtilities.getAppPath() : SystemUtilities.getJarPath());
        } catch(IOException e) {
            log.warn("Could not create desktop shortcut {}", SHORTCUT_PATH, e);
        }
    }
}
