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
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
        //todo check for startup flag in LaunchAgents
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
