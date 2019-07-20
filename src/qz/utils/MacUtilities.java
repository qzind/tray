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

import com.apple.OSXAdapterWrapper;
import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.ui.component.IconCache;

import java.awt.*;
import java.lang.reflect.Method;

/**
 * Utility class for MacOS specific functions.
 *
 * @author Tres Finocchiaro
 */
public class MacUtilities {

    private static final Logger log = LoggerFactory.getLogger(IconCache.class);
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
            OSXAdapterWrapper.setAboutHandler(MacUtilities.class, MacUtilities.class.getDeclaredMethod("showAboutDialog"));
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
            OSXAdapterWrapper.setQuitHandler(MacUtilities.class, MacUtilities.class.getDeclaredMethod("showExitPrompt"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs a shell command to determine if "Dark" desktop theme is enabled
     * @return true if enabled, false if not
     */
    public static boolean isDarkMode() {
        return !ShellUtilities.execute(new String[] { "defaults", "read", "-g", "AppleInterfaceStyle" }, new String[] { "Dark" }, true, true).isEmpty();
    }

    public static int getScaleFactor() {
        // Java 9+ per JDK-8172962
        if (Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("9.0.0"))) {
            GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration graphicsConfig = graphicsDevice.getDefaultConfiguration();
            return (int)graphicsConfig.getDefaultTransform().getScaleX();
        }
        // Java 7, 8
        try {
            // Use reflection to avoid compile errors on non-macOS environments
            Object screen = Class.forName("sun.awt.CGraphicsDevice").cast(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
            Method getScaleFactor = screen.getClass().getDeclaredMethod("getScaleFactor");
            Object obj = getScaleFactor.invoke(screen);
            if (obj instanceof Integer) {
                return ((Integer)obj).intValue();
            }
        } catch (Exception e) {
            log.warn("Unable to determine screen scale factor.  Defaulting to 1.", e);
        }
        return 1;
    }

    public static int getProcessID() {
        return CLibrary.INSTANCE.getpid();
    }

    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
        int getpid ();
    }

}
