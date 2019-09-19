/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.installer.shortcut;

import mslinks.ShellLink;
import qz.common.Constants;
import qz.installer.WindowsInstaller;
import qz.installer.WindowsSpecialFolders;
import qz.utils.SystemUtilities;

import java.io.IOException;
import java.nio.file.*;

/**
 * @author Tres Finocchiaro
 */
public class WindowsShortcutCreator extends ShortcutCreator {
    private static String SHORTCUT_NAME = Constants.PROPS_FILE + ".lnk";

    public void createDesktopShortcut() {
        createShortcut(WindowsSpecialFolders.DESKTOP.toString());
    }

    public boolean canAutoStart() {
        return Files.exists(Paths.get(WindowsSpecialFolders.COMMON_STARTUP.toString(), SHORTCUT_NAME));
    }

    private void createShortcut(String folderPath) {
        try {
            ShellLink.createLink(getAppPath(), folderPath + SHORTCUT_NAME);
        }
        catch(IOException ex) {
            log.warn("Error creating desktop shortcut", ex);
        }
    }

    /**
     * Calculates .exe path from .jar
     */
    private static String getAppPath() {
        return SystemUtilities.getJarPath().replaceAll(".jar$", ".exe");
    }
}
