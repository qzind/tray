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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qz.common.Constants;
import qz.utils.ShellUtilities;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
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

    @Override
    public boolean hasStartupShortcut() {
        removeLegacyStartup();

        String parent = "/Library/LaunchAgents";
        String[] parts = Constants.ABOUT_URL.split("/");
        parts = parts[parts.length - 1].split("\\.");
        String pListName = parts[1] + "." + parts[0] + "." + Constants.PROPS_FILE + ".plist";

        Path p = Paths.get(parent, pListName);

        if (!Files.exists(p)) {
            log.warn("No plist file found");
            return false;
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(p.toFile());
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("dict");
            nList = nList.item(0).getChildNodes();

            for (int n = 0; n < nList.getLength(); n++) {
                Node nNode = nList.item(n);
                if (nNode.getTextContent().equals("RunAtLoad") && nNode.getNodeName().equals("key")) {
                    //If we get an index out of bounds here, who cares, it will fall into the catch.
                    return nList.item(n+1).getNodeName().equals("true");
                }
            }
        } catch(Exception e) {
            log.error("Error reading plist file");
            return false;
        }
        return true;
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
