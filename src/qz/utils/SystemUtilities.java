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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.sun.jna.platform.win32.WinReg.*;

/**
 * Utility class for OS detection functions.
 *
 * @author Tres Finocchiaro
 */
public class SystemUtilities {

    // Name of the os, i.e. "Windows XP", "Mac OS X"
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final Logger log = LoggerFactory.getLogger(TrayManager.class);
    private static final Locale defaultLocale = Locale.getDefault();

    private static Boolean darkDesktop;
    private static Boolean darkTaskbar;
    private static String uname;
    private static String linuxRelease;
    private static String classProtocol;
    private static Version osVersion;
    private static String jarPath;


    /**
     * @return Lowercase version of the operating system name
     * identified by {@code System.getProperty("os.name");}.
     */
    public static String getOS() {
        return OS_NAME;
    }

    /**
     * Call to workaround Locale-specific bugs (See issue #680)
     * Please call <code>restoreLocale()</code> as soon as possible
     */
    public static synchronized void swapLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }

    public static synchronized void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    public static String toISO(Date d) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        df.setTimeZone(tz);
        return df.format(d);
    }

    public static String timeStamp() {
        return toISO(new Date());
    }

    public static Version getOSVersion() {
        if (osVersion == null) {
            String version = System.getProperty("os.version");
            // Windows is missing patch release, read it from registry
            if (isWindows()) {
                String patch = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ReleaseId");
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

    public static boolean isAdmin() {
        if (SystemUtilities.isWindows()) {
            return ShellUtilities.execute("net", "session");
        } else {
            return ShellUtilities.executeRaw("whoami").trim().equals("root");
        }
    }

    /**
     * Handle Java versioning nuances
     * To eventually be replaced with <code>java.lang.Runtime.Version</code> (JDK9+)
     */
    public static Version getJavaVersion() {
        String version = System.getProperty("java.version");
        String[] parts = version.split("\\D+");

        int major = 1;
        int minor = 0;
        int patch = 0;
        String meta = "";

        try {
            switch(parts.length) {
                default:
                case 4:
                    meta = parts[3];
                case 3:
                    patch = Integer.parseInt(parts[2]);
                case 2:
                    minor = Integer.parseInt(parts[1]);
                    major = Integer.parseInt(parts[0]);
                    break;
                case 1:
                    major = Integer.parseInt(parts[0]);
                    if (major <= 8) {
                        // Force old 1.x style formatting
                        minor = major;
                        major = 1;
                    }
            }
        } catch(NumberFormatException e) {
            log.warn("Could not parse Java version \"{}\"", e);
        }
        if(meta.trim().isEmpty()) {
            return Version.forIntegers(major, minor, patch);
        } else {
            return Version.forIntegers(major, minor, patch).setBuildMetadata(meta);
        }
    }

    /**
     * Determines the currently running Jar's absolute path on the local filesystem
     *
     * @return A String value representing the absolute path to the currently running
     * jar
     */
    public static String detectJarPath() {
        try {
            String jarPath = new File(SystemUtilities.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getCanonicalPath();
            // Fix characters that get URL encoded when calling getPath()
            return URLDecoder.decode(jarPath, "UTF-8");
        } catch(IOException ex) {
            log.error("Unable to determine Jar path", ex);
        }
        return null;
    }

    /**
     * Returns the jar which we will create a shortcut for
     *
     * @return The path to the jar path which has been set
     */
    public static String getJarPath() {
        if (jarPath == null) {
            jarPath = detectJarPath();
        }
        return jarPath;
    }

    /**
     * Returns the app's path, based on the jar location
     * or null if no .jar is found (such as running from IDE)
     * @return
     */
    public static Path detectAppPath() {
        String jarPath = detectJarPath();
        if (jarPath != null) {
            File jar = new File(jarPath);
            if (jar.getPath().endsWith(".jar") && jar.exists()) {
                return Paths.get(jar.getParent());
            }
        }
        return null;
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

    public static boolean isWindowsXP() { return OS_NAME.contains("win") && OS_NAME.contains("xp"); }

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

    public static boolean isDarkTaskbar() {
        return isDarkTaskbar(false);
    }

    public static boolean isDarkTaskbar(boolean recheck) {
        if(darkTaskbar == null || recheck) {
            if (isWindows()) {
                darkTaskbar = WindowsUtilities.isDarkTaskbar();
            } else if(isMac()) {
                // Ignore, we'll set the template flag using JNA
                darkTaskbar = false;
            } else {
                // Linux doesn't differentiate; return the cached darkDesktop value
                darkTaskbar = isDarkDesktop();
            }
        }
        return darkTaskbar.booleanValue();
    }

    public static boolean isDarkDesktop() {
        return isDarkDesktop(false);
    }

    public static boolean isDarkDesktop(boolean recheck) {
        if (darkDesktop == null || recheck) {
            // Check for Dark Mode on MacOS
            if (isMac()) {
                darkDesktop = MacUtilities.isDarkDesktop();
            } else if (isWindows()) {
                darkDesktop = WindowsUtilities.isDarkDesktop();
            } else {
                darkDesktop = UbuntuUtilities.isDarkMode();
            }
        }
        return darkDesktop.booleanValue();
    }

    public static void adjustThemeColors() {
        Constants.WARNING_COLOR = isDarkDesktop() ? Constants.WARNING_COLOR_DARK : Constants.WARNING_COLOR_LITE;
        Constants.TRUSTED_COLOR = isDarkDesktop() ? Constants.TRUSTED_COLOR_DARK : Constants.TRUSTED_COLOR_LITE;
    }

    public static boolean prefersMaskTrayIcon() {
        if (Constants.MASK_TRAY_SUPPORTED) {
            if (SystemUtilities.isMac()) {
                // Assume a pid of -1 is a broken JNA
                return MacUtilities.getProcessID() != -1;
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
            if(isDarkDesktop() && darkulaThemeNeeded) {
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

    public static void appendProperty(String property, String value) {
        appendProperty(property, value, File.pathSeparator);
    }

    public static void appendProperty(String property, String value, String delimiter) {
        String currentValue = System.getProperty(property);
        System.setProperty(property, currentValue == null ? value : currentValue + delimiter + value);
    }

    public static boolean isJDK() {
        String path = System.getProperty("sun.boot.library.path");
        if(path != null) {
            String javacPath = "";
            if(path.endsWith(File.separator + "bin")) {
                javacPath = path;
            } else {
                int libIndex = path.lastIndexOf(File.separator + "lib");
                if(libIndex > 0) {
                    javacPath = path.substring(0, libIndex) + File.separator + "bin";
                }
            }
            if(!javacPath.isEmpty()) {
                return new File(javacPath, "javac").exists() || new File(javacPath, "javac.exe").exists();
            }
        }
        return false;
    }
}
