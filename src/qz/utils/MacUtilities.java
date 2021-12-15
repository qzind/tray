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
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.dyorgio.jna.platform.mac.ActionCallback;
import org.dyorgio.jna.platform.mac.Foundation;
import org.dyorgio.jna.platform.mac.FoundationUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.ui.component.IconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Utility class for MacOS specific functions.
 *
 * @author Tres Finocchiaro
 */
public class MacUtilities {

    private static final Logger log = LogManager.getLogger(IconCache.class);
    private static Dialog aboutDialog;
    private static TrayManager trayManager;
    private static String bundleId;
    private static Integer pid;
    private static Boolean jdkSupportsTemplateIcon;
    private static boolean templateIconForced = false;

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
                    String[] domain = part.toLowerCase(Locale.ENGLISH).split("\\.");
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

    static int getProcessId() {
        if(pid == null) {
            try {
                pid = CLibrary.INSTANCE.getpid();
            }
            catch(UnsatisfiedLinkError | NoClassDefFoundError e) {
                log.warn("Could not obtain process ID.  This usually means JNA isn't working.  Returning -1.");
                pid = -1;
            }
        }
        return pid;
    }

    private interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);
        int getpid ();
    }

    /**
     * Checks for presence of JDK-8252015 using reflection
     */
    public static boolean jdkSupportsTemplateIcon() {
        if(jdkSupportsTemplateIcon == null) {
            try {
                // before JDK-8252015: setNativeImage(long, long, boolean)
                // after  JDK-8252015: setNativeImage(long, long, boolean, boolean)
                Class.forName("sun.lwawt.macosx.CTrayIcon").getDeclaredMethod("setNativeImage", long.class, long.class, boolean.class, boolean.class);
                jdkSupportsTemplateIcon = true;
            }
            catch(ClassNotFoundException | NoSuchMethodException ignore) {
                jdkSupportsTemplateIcon = false;
            }
        }
        return jdkSupportsTemplateIcon;
    }

    public static void toggleTemplateIcon(TrayIcon icon) {
        // Check if icon has a menu
        if (icon.getPopupMenu() == null) {
            throw new IllegalStateException("PopupMenu needs to be set on TrayIcon first");
        }
        // Check if icon is on SystemTray
        if (icon.getImage() == null) {
            throw new IllegalStateException("TrayIcon needs to be added on SystemTray first");
        }
        // Check if icon is on SystemTray
        if (!Arrays.asList(SystemTray.getSystemTray().getTrayIcons()).contains(icon)) {
            throw new IllegalStateException("TrayIcon needs to be added on SystemTray first");
        }

        // Prevent second invocation; causes icon to disappear
        if(templateIconForced) {
            return;
        } else {
            templateIconForced = true;
        }

        try {
            Field ptrField = Class.forName("sun.lwawt.macosx.CFRetainedResource").getDeclaredField("ptr");
            ptrField.setAccessible(true);

            Field field = TrayIcon.class.getDeclaredField("peer");
            field.setAccessible(true);
            long cTrayIconAddress = ptrField.getLong(field.get(icon));

            long cPopupMenuAddressTmp = 0;
            if (icon.getPopupMenu() != null) {
                field = MenuComponent.class.getDeclaredField("peer");
                field.setAccessible(true);
                cPopupMenuAddressTmp = ptrField.getLong(field.get(icon.getPopupMenu()));
            }
            final long cPopupMenuAddress = cPopupMenuAddressTmp;

            final NativeLong statusItem = FoundationUtil.invoke(new NativeLong(cTrayIconAddress), "theItem");
            NativeLong awtView = FoundationUtil.invoke(statusItem, "view");
            final NativeLong image = Foundation.INSTANCE.object_getIvar(awtView, Foundation.INSTANCE.class_getInstanceVariable(FoundationUtil.invoke(awtView, "class"), "image"));
            FoundationUtil.invoke(image, "setTemplate:", true);
            FoundationUtil.runOnMainThreadAndWait(() -> {
                FoundationUtil.invoke(statusItem, "setView:", FoundationUtil.NULL);
                NativeLong target;
                if (SystemUtilities.getOSVersion().greaterThanOrEqualTo(Version.forIntegers(10, 10))) {
                    target = FoundationUtil.invoke(statusItem, "button");
                } else {
                    target = statusItem;
                }
                FoundationUtil.invoke(target, "setImage:", image);
                //FoundationUtil.invoke(statusItem, "setLength:", length);

                if (cPopupMenuAddress != 0) {
                    FoundationUtil.invoke(statusItem, "setMenu:", FoundationUtil.invoke(new NativeLong(cPopupMenuAddress), "menu"));
                } else {
                    new ActionCallback(() -> {
                        final ActionListener[] listeners = icon.getActionListeners();
                        final int now = (int) System.currentTimeMillis();
                        for (int i = 0; i < listeners.length; i++) {
                            final int iF = i;
                            SwingUtilities.invokeLater(() -> listeners[iF].actionPerformed(new ActionEvent(icon, now + iF, null)));
                        }
                    }).installActionOnNSControl(target);
                }
            });
        } catch (Throwable ignore) {}
    }

    public static boolean nativeFileCopy(Path source, Path destination) {
        try {
            // AppleScript's "duplicate" requires an existing destination
            if (!destination.toFile().isDirectory()) {
                // To perform this in a single operation in AppleScript, the source and dest
                // file names must match.  Copy to a temp directory first to retain desired name.
                Path tempFile = Files.createTempDirectory("qz_cert_").resolve(destination.getFileName());
                log.debug("Copying {} to {} to obtain the desired name", source, tempFile);
                source = Files.copy(source, tempFile);
                destination = destination.getParent();
            }
            return ShellUtilities.executeAppleScript(
                    "tell application \"Finder\" to duplicate " +
                            "file (POSIX file \"" + source + "\" as alias) " +
                            "to folder (POSIX file \"" + destination + "\" as alias) " +
                            "with replacing");
        } catch(Throwable t) {
            log.warn("Unable to perform native file copy using AppleScript", t);
        }
        return false;
    }

}
