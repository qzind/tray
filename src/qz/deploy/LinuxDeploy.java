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

import java.awt.Toolkit;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.*;

/**
 * @author Tres Finocchiaro
 */
class LinuxDeploy extends DeployUtilities {

    private static final Logger log = LoggerFactory.getLogger(LinuxDeploy.class);

    private static String STARTUP = "/etc/xdg/autostart/";
    private static String DESKTOP = System.getProperty("user.home") + "/Desktop/";
    private static String DELETED_ICON = "process-stop";

    private String appLauncher = "/usr/share/applications/" + getShortcutName();

    @Override
    public boolean createStartupShortcut() {
        return copyShortcut(appLauncher, STARTUP);
    }

    @Override
    public boolean createDesktopShortcut() {
        return copyShortcut(appLauncher, DESKTOP);
    }

    @Override
    public boolean removeStartupShortcut() {
        return writeArrayToFile(STARTUP + getShortcutName(), new String[]{
                "[Desktop Entry]",
                "Type=Application",
                "Name=" + Constants.ABOUT_TITLE + " (Disabled)",
                "Exec=/bin/true",
                "Icon=" + DELETED_ICON,
                "Terminal=false",
        });
    }

    @Override
    public boolean removeDesktopShortcut() {
        return deleteFile(DESKTOP + getShortcutName());
    }


    @Override
    public boolean hasStartupShortcut() {
        try {
            String contents = FileUtilities.readLocalFile(STARTUP + getShortcutName());
            return !contents.contains("Icon=" + DELETED_ICON);
        } catch (Exception ex) {
            log.warn("Unable to read startup shortcut {}", STARTUP + getShortcutName(), ex);
        }
        return false;
    }

    @Override
    public boolean hasDesktopShortcut() {
        // Upgrade legacy "QZ Tray.desktop" shortcuts
        String shortcut = DESKTOP + Constants.ABOUT_TITLE + ".desktop";
        if (fileExists(shortcut)) {
            if (ShellUtilities.execute(new String[] { "rm", shortcut })) {
                copyShortcut(appLauncher, DESKTOP);
            }
        }
        return fileExists(DESKTOP + getShortcutName());
    }

    private static boolean copyShortcut(String source, String target) {
       return ShellUtilities.execute(new String[] {
               "cp", source, target
       });
    }

    @Override
    public void setShortcutName(String name) {
        super.setShortcutName(name);
        // Fix window titles on Gnome 3 per JDK-6528430
        try {
            Toolkit t = Toolkit.getDefaultToolkit();
            Field f = t.getClass().getDeclaredField("awtAppClassName");
            f.setAccessible(true);
            f.set(t, name); 
        }
        catch (Exception ignore) {}
    }

    @Override
    public String getShortcutName() {
        return Constants.PROPS_FILE + ".desktop";
    }
}

