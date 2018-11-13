/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.utils;

import com.apple.OSXAdapter;
import qz.common.TrayManager;
import qz.ui.IconCache;

import java.awt.*;

/**
 * Utility class for MacOS specific functions.
 *
 * @author Tres Finocchiaro
 */
public class MacUtilities {

    private static Dialog aboutDialog;
    private static TrayManager trayManager;

    public static void showAboutDialog() {
        if (aboutDialog != null) { aboutDialog.setVisible(true); }
    }

    public static void showExitPrompt() {
        if (trayManager != null) { trayManager.exit(0); }
    }

    /**
     * Adds a listener to register the Apple "About" dialog to call {@code setVisible()} on the specified Dialog
     */
    public static void registerAboutDialog(Dialog aboutDialog) {
        MacUtilities.aboutDialog = aboutDialog;

        try {
            OSXAdapter.setAboutHandler(MacUtilities.class, MacUtilities.class.getDeclaredMethod("showAboutDialog"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a listener to register the Apple "Quit" to call {@code trayManager.exit(0)}
     */
    public static void registerQuitHandler(TrayManager trayManager) {
        MacUtilities.trayManager = trayManager;

        try {
            OSXAdapter.setQuitHandler(MacUtilities.class, MacUtilities.class.getDeclaredMethod("showExitPrompt"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true if <code>defaults read -g AppleInterfaceStyle</code> has an exit status of <code>0</code> (i.e. _not_ returning "key not found").
     */
    public static boolean isDarkMode() {
        return ShellUtilities.execute(new String[] { "defaults", "read", "-g", "AppleInterfaceStyle" });
    }

    /**
     * Replaces the cached tray icons with white versions if necessary
     * to accommodate macOS 10.14+ dark mode support
     *
     * @param iconCache The icons which have been cached
     */
    public static void fixTrayIcons(IconCache iconCache) {
        boolean darkMode = isDarkMode();
        if (SystemUtilities.isMac()) {
            for(IconCache.Icon i : IconCache.getTypes()) {
                if (i.isTrayIcon() && darkMode && ColorUtilities.isBlack(iconCache.getImage(i))) {
                    iconCache.invertColors(i);
                }
            }
        }
    }

}
