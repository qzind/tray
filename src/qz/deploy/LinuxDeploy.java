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
import qz.common.Constants;
import qz.utils.ShellUtilities;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author Tres Finocchiaro
 */
public class LinuxDeploy extends DeployUtilities {

    private static final Logger log = LoggerFactory.getLogger(LinuxDeploy.class);

    @Override
    public boolean createStartupShortcut() {
        ShellUtilities.execute(new String[] {
                "mkdir", "-p", System.getProperty("user.home") + "/.config/autostart/"
        });
        return createShortcut(System.getProperty("user.home") + "/.config/autostart/");
    }

    @Override
    public boolean createDesktopShortcut() {
        return createShortcut(System.getProperty("user.home") + "/Desktop/");
    }

    @Override
    public boolean removeStartupShortcut() {
        return deleteFile(System.getProperty("user.home") + "/.config/autostart/" + getShortcutName());
    }

    @Override
    public boolean removeDesktopShortcut() {
        return deleteFile(System.getProperty("user.home") + "/Desktop/" + getShortcutName());
    }


    @Override
    public boolean hasStartupShortcut() {
        String target = System.getProperty("user.home") + "/.config/autostart/";
        upgradeLegacyShortcut(target);
        return fileExists(target + getShortcutName());
    }

    @Override
    public boolean hasDesktopShortcut() {
        String target = System.getProperty("user.home") + "/Desktop/";
        upgradeLegacyShortcut(target);
        return fileExists(target + getShortcutName());
    }

    /**
     * Creates a Linux ".desktop" shortcut
     *
     * @param target target location of shortcut
     * @return Whether or not the shortcut was created successfully
     */
    public boolean createShortcut(String target) {
       return ShellUtilities.execute(new String[] {
               "cp", getAppPath(), target
       });
    }

    @Override
    public String getShortcutName() {
        return Constants.PROPS_FILE + ".desktop";
    }

    /**
     * Returns the path to the jar executable or desktop launcher
     * @return
     */
    public String getAppPath() {
        return "/usr/share/applications/" + getShortcutName();
    }

    /**
     * Upgrade 1.9 shortcut to new 2.0 format
     * @return
     */
    private boolean upgradeLegacyShortcut(String target) {
        String shortcut = target + Constants.ABOUT_TITLE + ".desktop";
        if (fileExists(shortcut)) {
            if (ShellUtilities.execute(new String[] { "rm", shortcut })) {
                return createShortcut(target);
            }
        }
        return false;
    }
}

