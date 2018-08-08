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

    private String autostartToggle = getAppPath() + "/.autostart";
    private String desktopShortcut = System.getProperty("user.home") + "/Desktop/" + getShortcutName();

    @Override
    public boolean setAutostart(boolean autostart) {
        return writeAutostart(autostart ? "0" : "1");
    }

    @Override
    public boolean isAutostart() {
        removeLegacyStartup();
        try {
            return FileUtilities.readLocalFile(autostartToggle).contains("1");
        } catch (Exception err) {
            log.warn("Could not read .autostart file", err);
        }
        return false;
    }

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

    private boolean writeAutostart(String mode) {
        try(BufferedWriter w = new BufferedWriter(new FileWriter(autostartToggle))) {
            w.write(mode);
            return true;
        } catch(Exception err) {
            log.warn("Could not write to .autostart", err);
        }
        return false;
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
