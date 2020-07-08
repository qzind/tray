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
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility class for MacOS specific functions.
 *
 * @author Tres Finocchiaro
 */
public class MacUtilities {

    private static final Logger log = LoggerFactory.getLogger(IconCache.class);
    private static Dialog aboutDialog;
    private static TrayManager trayManager;
    private static String bundleId;
    private static Boolean supportsTemplateIcons;
    private static Version MACOS_VERSION_TEMPLATE_REQUIRED = Version.valueOf("10.16.0");
    private static Version[] JAVA_VERSION_TEMPLATE_SUPPORTED = new Version[]{
            Version.valueOf("17.0.0+5"),
            Version.valueOf("11.999.999") // TODO
    };

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
     * Calculates CFBundleIdentifier for macOS
     * @return
     */
    public static String getBundleId() {
        if(bundleId == null) {
            ArrayList<String> parts = new ArrayList(Arrays.asList(Constants.ABOUT_URL.split("/")));
            for(String part : parts) {
                if(part.contains(".")) {
                    // Try to use this section as the .com, etc
                    String[] domain = part.toLowerCase().split("\\.");
                    // Convert to reverse-domain syntax
                    for(int i = domain.length -1; i >= 0; i--) {
                        // Skip "www", "www2", etc
                        if(i == 0 && domain[i].startsWith("www")) {
                            break;
                        }
                        bundleId = (bundleId == null ? "" : bundleId) + domain[i] + ".";
                    }
                }
            }
        }
        if(bundleId != null) {
            bundleId += Constants.PROPS_FILE;
        } else {
            bundleId = "io.qz.fallback." + Constants.PROPS_FILE;
        }
        return bundleId;
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
    public static boolean isDarkDesktop() {
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
        try {
            return CLibrary.INSTANCE.getpid();
        } catch(UnsatisfiedLinkError | NoClassDefFoundError e) {
            log.warn("Could not obtain process ID.  This usually means JNA isn't working.  Returning -1.");
        }
        return -1;
    }

    /**
     * Prior to Big Sur Beta, the system tray honored the Desktop dark/lite theme.
     * Starting with Big Sur, special consideration needs to be made to prevent the tray icon
     * from disappearing into the taskbar.
     *
     * Set also <code>MacUtilities.javaSupportsTemplateIcon()</code>
     */
    public static boolean isTemplateIconRequired() {
        return SystemUtilities.getOSVersion().greaterThanOrEqualTo(MACOS_VERSION_TEMPLATE_REQUIRED);
    }

    /**
     * Template icon support since 17.0.0+5 or any backport.
     *
     * See also: https://bugs.openjdk.java.net/browse/JDK-8252015
     */
    public static boolean javaSupportsTemplateIcon() {
        if(supportsTemplateIcons == null) {
            for(Version supportAdded : JAVA_VERSION_TEMPLATE_SUPPORTED) {
                if(Constants.JAVA_VERSION.getMajorVersion() > 17) {
                    // Assume this is a base feature after JDK 17
                    supportsTemplateIcons = true;
                } else if (Constants.JAVA_VERSION.getMajorVersion() == supportAdded.getMajorVersion()) {
                    // Only compare if major versions match
                    supportsTemplateIcons = Constants.JAVA_VERSION.compareWithBuildsTo(supportAdded) >= 0;
                }
            }
        }
        return supportsTemplateIcons;
    }

    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
        int getpid ();
    }

}
