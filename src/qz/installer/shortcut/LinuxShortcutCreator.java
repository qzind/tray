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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.LinuxInstaller;
import qz.utils.SystemUtilities;

import static qz.installer.LinuxInstaller.*;

/**
 * @author Tres Finocchiaro
 */
class LinuxShortcutCreator extends ShortcutCreator {
    private static final Logger log = LogManager.getLogger(LinuxShortcutCreator.class);
    private final Path desktopDir;
    private final String appLauncherDir;
    private final String startupDir;

    public LinuxShortcutCreator() {
        boolean systemWide = SystemUtilities.isInstalledSystemWide();
        appLauncherDir =  systemWide ? SYSTEM_APP_LAUNCHER : String.format(USER_APP_LAUNCHER, System.getProperty("user.home"));
        startupDir = systemWide ? SYSTEM_STARTUP_DIR : String.format(LinuxInstaller.USER_STARTUP_DIR, System.getProperty("user.home"));
        desktopDir = Paths.get(System.getProperty("user.home"), "Desktop");
    }

    public boolean canAutoStart() {
        if(SystemUtilities.isInstalled()) {
            return Files.exists(Paths.get(
                    startupDir,
                    LinuxInstaller.SHORTCUT_NAME));
        }
        return false;
    }
    public void createDesktopShortcut() {
        if(SystemUtilities.isInstalled()) {
            Path shortcut = Paths.get(appLauncherDir, SHORTCUT_NAME);
            copyShortcut(shortcut, desktopDir.resolve(SHORTCUT_NAME));
        } else {
            log.warn("Skipping creation of Desktop icon, we don't appear to be installed");
        }
    }

    private static void copyShortcut(Path source, Path target) {
        try {
            Files.copy(source, target);
        } catch(IOException e) {
            log.warn("Error creating shortcut {}", target, e);
        }
    }
}

