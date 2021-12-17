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
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.LinuxInstaller;

/**
 * @author Tres Finocchiaro
 */
class LinuxShortcutCreator extends ShortcutCreator {

    private static final Logger log = LogManager.getLogger(LinuxShortcutCreator.class);
    private static String DESKTOP = System.getProperty("user.home") + "/Desktop/";

    public boolean canAutoStart() {
        return Files.exists(Paths.get(LinuxInstaller.STARTUP_DIR, LinuxInstaller.SHORTCUT_NAME));
    }
    public void createDesktopShortcut() {
        copyShortcut(LinuxInstaller.APP_LAUNCHER, DESKTOP);
    }

    private static void copyShortcut(String source, String target) {
        try {
            Files.copy(Paths.get(source), Paths.get(target));
        } catch(IOException e) {
            log.warn("Error creating shortcut {}", target, e);
        }
    }
}

