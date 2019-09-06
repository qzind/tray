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

import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.deploy.DeployUtilities;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Utility class for OS detection functions.
 *
 * @author Tres Finocchiaro
 */
public class SystemUtilities {

    // Name of the os, i.e. "Windows XP", "Mac OS X"
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final Logger log = LoggerFactory.getLogger(TrayManager.class);

    private static Boolean darkMode;
    private static String uname;
    private static String linuxRelease;
    private static String classProtocol;
    private static Version osVersion;


    /**
     * @return Lowercase version of the operating system name
     * identified by {@code System.getProperty("os.name");}.
     */
    public static String getOS() {
        return OS_NAME;
    }

    public static Version getOSVersion() {
        if (osVersion == null) {
            String version = System.getProperty("os.version");
            // Windows is missing patch release, read it from registry
            if (isWindows()) {
                String patch = ShellUtilities.getRegistryString("HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ReleaseId");
                if (patch != null) {
                    version += "." + patch.trim();
                }
            }
            while (version.split("\\.").length < 3) {
                version += ".0";
            }
            osVersion = Version.valueOf(version);
        }
        return osVersion;
    }


    /**
     * Provides a JDK9-friendly wrapper around the inconsistent and poorly standardized Java internal versioning.
     * This may eventually be superseded by <code>java.lang.Runtime.Version</code>, but the codebase will first need to be switched to JDK9 level.
     * @return Semantically formatted Java Runtime version
     */
    public static Version getJavaVersion() {
        String version = System.getProperty("java.version");
        String[] parts = version.split("\\D+");
        switch (parts.length) {
            case 0:
                return Version.forIntegers(1, 0, 0);
            case 1:
                // Assume JDK9 format
                return Version.forIntegers(1, Integer.parseInt(parts[0]), 0);
            case 2:
                // Unknown format
                return Version.forIntegers(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 0);
            case 3:
                // Assume JDK8 and lower; missing build metadata
                return Version.forIntegers(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            case 4:
            default:
                // Assume JDK8 and lower format
                return Version.forIntegers(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).setBuildMetadata(parts[3]);
        }
    }


    /**
     * Retrieve OS-specific Application Data directory such as:
     * {@code C:\Users\John\AppData\Roaming\qz} on Windows
     * -- or --
     * {@code /Users/John/Library/Application Support/qz} on Mac
     * -- or --
     * {@code /home/John/.qz} on Linux
     *
     * @return Full path to the Application Data directory
     */
    public static String getDataDirectory() {
        String parent;
        String folder = Constants.DATA_DIR;

        if (isWindows()) {
            parent = System.getenv("APPDATA");
        } else if (isMac()) {
            parent = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support";
        } else if (isUnix()) {
            parent = System.getProperty("user.home");
            folder = "." + folder;
        } else {
            parent = System.getProperty("user.dir");
        }

        return parent + File.separator + folder;
    }

    /**
     * Returns the OS shared data directory for FileIO operations. Must match
     * that defined in desktop installer scripts, which create directories
     * and grant read/write access to normal users.
     * access.
     * @return
     */
    public static String getSharedDataDirectory() {
        String parent;

        if (isWindows()) {
            parent = System.getenv("PROGRAMDATA");
        } else if (isMac()) {
            parent = "/Library/Application Support/";
        } else {
            parent = "/srv/";
        }

        return parent + File.separator + Constants.DATA_DIR;
    }

    public static String getSharedDirectory() {
        String parent = DeployUtilities.getSystemShortcutCreator().getParentDirectory();
        String folder = Constants.SHARED_DATA_DIR;

        return parent + File.separator + folder;
    }

    /**
     * Detect 32-bit JVM on 64-bit Windows
     * @return
     */
    public static boolean isWow64() {
        String arch = System.getProperty("os.arch");
        return isWindows() && !arch.contains("x86_64") && !arch.contains("amd64") && System.getenv("PROGRAMFILES(x86)") != null;
    }

    /**
     * Determine if the current Operating System is Windows
     *
     * @return {@code true} if Windows, {@code false} otherwise
     */
    public static boolean isWindows() {
        return (OS_NAME.contains("win"));
    }

    /**
     * Determine if the current Operating System is Mac OS
     *
     * @return {@code true} if Mac OS, {@code false} otherwise
     */
    public static boolean isMac() {
        return (OS_NAME.contains("mac"));
    }

    /**
     * Determine if the current Operating System is Linux
     *
     * @return {@code true} if Linux, {@code false} otherwise
     */
    public static boolean isLinux() {
        return (OS_NAME.contains("linux"));
    }

    /**
     * Determine if the current Operating System is Unix
     *
     * @return {@code true} if Unix, {@code false} otherwise
     */
    public static boolean isUnix() {
        return (OS_NAME.contains("mac") || OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.indexOf("aix") > 0 || OS_NAME.contains("sunos"));
    }

    /**
     * Determine if the current Operating System is Solaris
     *
     * @return {@code true} if Solaris, {@code false} otherwise
     */
    public static boolean isSolaris() {
        return (OS_NAME.contains("sunos"));
    }

    /**
     * Returns whether the output of {@code uname -a} shell command contains "Ubuntu"
     *
     * @return {@code true} if this OS is Ubuntu
     */
    public static boolean isUbuntu() {
        getUname();
        return uname != null && uname.contains("Ubuntu");
    }

    /**
     * Returns whether the output of <code>cat /etc/redhat-release/code> shell command contains "Fedora"
     *
     * @return {@code true} if this OS is Fedora
     */
    public static boolean isFedora() {
        getLinuxRelease();
        return linuxRelease != null && linuxRelease.contains("Fedora");
    }

    /**
     * Returns the output of {@code cat /etc/lsb-release} or equivalent
     *
     * @return the output of the command or null if not running Linux
     */
    public static String getLinuxRelease() {
        if (isLinux() && linuxRelease == null) {
            String[] releases = {"/etc/lsb-release", "/etc/redhat-release"};
            for(String release : releases) {
                String result = ShellUtilities.execute(
                        new String[] {"cat", release},
                        null
                );
                if (!result.isEmpty()) {
                    linuxRelease = result;
                    break;
                }
            }
        }

        return linuxRelease;
    }

    /**
     * Returns the output of {@code uname -a} shell command, useful for parsing the Linux Version
     *
     * @return the output of {@code uname -a}, or null if not running Linux
     */
    public static String getUname() {
        if (isLinux() && uname == null) {
            uname = ShellUtilities.execute(
                    new String[] {"uname", "-a"},
                    null
            );
        }

        return uname;
    }

    public static boolean isDarkMode() {
        return isDarkMode(false);
    }

    public static boolean isDarkMode(boolean recheck) {
        if (darkMode == null || recheck) {
            // Check for Dark Mode on MacOS
            if (isMac()) {
                darkMode = MacUtilities.isDarkMode();
            } else if (isWindows()) {
                darkMode = WindowsUtilities.isDarkMode();
            } else {
                darkMode = UbuntuUtilities.isDarkMode();
            }
        }
        return darkMode.booleanValue();
    }

    public static void adjustThemeColors() {
        Constants.WARNING_COLOR = isDarkMode() ? Constants.WARNING_COLOR_DARK : Constants.WARNING_COLOR_LITE;
        Constants.TRUSTED_COLOR = isDarkMode() ? Constants.TRUSTED_COLOR_DARK : Constants.TRUSTED_COLOR_LITE;
    }

    public static boolean prefersMaskTrayIcon() {
        if (Constants.MASK_TRAY_SUPPORTED) {
            if (SystemUtilities.isMac()) {
                return true;
            } else if (SystemUtilities.isWindows() && SystemUtilities.getOSVersion().getMajorVersion() >= 10) {
                return true;
            }
        }
        return false;
    }

    public static boolean setSystemLookAndFeel() {
        try {
            UIManager.getDefaults().put("Button.showMnemonics", Boolean.TRUE);
            boolean darkulaThemeNeeded = true;
            if(!isMac() && (isUnix() && UbuntuUtilities.isDarkMode())) {
                darkulaThemeNeeded = false;
            }
            if(isDarkMode() && darkulaThemeNeeded) {
                UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            adjustThemeColors();
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(SystemUtilities.class).warn("Error getting the default look and feel");
        }
        return false;
    }

    /**
     * Attempts to center a dialog provided a center point from a web browser at 96-dpi
     * Useful for tracking a browser window on multiple-monitor setups
     * @param dialog A dialog whom's width and height are used for calculating center-fit position
     * @param position The center point of a screen as calculated from a web browser at 96-dpi
     * @return <code>true</code> if the operation is successful
     */
    public static void centerDialog(Dialog dialog, Point position) {
        if (position == null || position.getX() == 0 || position.getY() == 0) {
            log.debug("Invalid dialog position provided: {}, we'll center on first monitor instead", position);
            dialog.setLocationRelativeTo(null);
            return;
        }

        //adjust for dpi scaling
        double dpiScale = getWindowScaleFactor();
        if (dpiScale == 0) {
            log.debug("Invalid window scale value: {}, we'll center on first monitor instead", dpiScale);
            dialog.setLocationRelativeTo(null);
            return;
        }

        Point p = new Point((int)(position.getX() * dpiScale), (int)(position.getY() * dpiScale));

        //account for own size when centering
        p.translate((int)(-dialog.getWidth() / 2.0), (int)(-dialog.getHeight() / 2.0));
        log.debug("Calculated dialog centered at: {}", p);
        dialog.setLocation(p);
    }

    /**
     * Shim for detecting window screen-placement scaling
     * See issues #284, #448
     * @return Logical dpi scale as dpi/96
     */
    public static double getWindowScaleFactor() {
        // MacOS is always 1
        if (isMac()) {
            return 1;
        }
        // Windows/Linux on JDK8 honors scaling
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("11.0.0"))) {
            return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        }
        // Windows on JDK11 is always 1
        if(isWindows()) {
            return 1;
        }
        // Linux on JDK11 requires JNA calls to Gdk
        return UbuntuUtilities.getScaleFactor();
    }

    /**
     * Detects if HiDPI is enabled
     * Warning: Due to behavioral differences between OSs, JDKs and poor
     * detection techniques this function should only be used to fix rare
     * edge-case bugs.
     *
     * See also SystemUtilities.getWindowScaleFactor()
     * @return true if HiDPI is detected
     */
    public static boolean isHiDPI() {
        if(isMac()) {
            return MacUtilities.getScaleFactor() > 1;
        } else if(isWindows()) {
            return WindowsUtilities.getScaleFactor() > 1;
        }
        // Fallback to a JNA Gdk technique
        return UbuntuUtilities.getScaleFactor() > 1;
    }

    /**
     * Detects if running from IDE or jar
     * @return true if running from a jar, false if running from IDE
     */
    public static boolean isJar() {
        if (classProtocol == null) {
            classProtocol = SystemUtilities.class.getResource("").getProtocol();
        }
        return "jar".equals(classProtocol);
    }
}
