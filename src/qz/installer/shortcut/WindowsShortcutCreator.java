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

import com.sun.jna.platform.win32.Win32Exception;
import mslinks.ShellLinkException;
import mslinks.ShellLinkHelper;
import qz.common.Constants;
import qz.installer.WindowsSpecialFolders;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * @author Tres Finocchiaro
 */
public class WindowsShortcutCreator extends ShortcutCreator {
    private static String SHORTCUT_NAME = Constants.ABOUT_TITLE + ".lnk";

    public void createDesktopShortcut() {
        createShortcut(WindowsSpecialFolders.DESKTOP.toString());
    }

    public boolean canAutoStart() {
        try {
            return Files.exists(Paths.get(WindowsSpecialFolders.COMMON_STARTUP.toString(), SHORTCUT_NAME));
        } catch(Win32Exception e) {
            log.warn("An exception occurred locating the startup folder; autostart cannot be determined.", e);
        }
        return false;
    }

    private void createShortcut(String folderPath) {
        try {
            ShellLinkHelper.createLink(getAppPath(), folderPath + File.separator + SHORTCUT_NAME);
        }
        catch(ShellLinkException | IOException ex) {
            log.warn("Error creating desktop shortcut", ex);
        }
    }

    /**
     * Calculates .exe path from .jar
     * fixme: overlaps SystemUtilities.getAppPath
     */
    private static String getAppPath() {
        return SystemUtilities.getJarPath().toString().replaceAll(".jar$", ".exe");
    }
}
