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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.ssl.Base64;
import org.joor.Reflect;
import org.joor.ReflectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.common.TrayManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Utility class for OS detection functions.
 *
 * @author Tres Finocchiaro
 */
public class SystemUtilities {
    static final String OS_NAME = System.getProperty("os.name");
    static final String OS_ARCH = System.getProperty("os.arch");
    private static final OsType OS_TYPE = getOsType(OS_NAME);
    private static final JreArch JRE_ARCH = getJreArch(OS_ARCH);
    private static final Logger log = LogManager.getLogger(TrayManager.class);
    private static final Locale defaultLocale = Locale.getDefault();

    static {
        if(!isWindows() && !isMac()) {
            // Force hid4java to use libusb: https://github.com/qzind/tray/issues/853
            try {
                Reflect.on("org.hid4java.jna.HidApi").set("useLibUsbVariant", true);
            } catch(ReflectException ignore) {}
        }
    }

    private static Boolean darkDesktop;
    private static Boolean darkTaskbar;
    private static Boolean hasMonocle;
    private static String classProtocol;
    private static Version osVersion;
    private static String osName;
    private static String osDisplayVersion;
    private static Path jarPath;
    private static Integer pid;

    public enum OsType {
        MAC,
        WINDOWS,
        LINUX,
        SOLARIS,
        UNKNOWN
    }

    public enum JreArch {
        X86,
        X86_64,
        ARM, // 32-bit
        AARCH64,
        RISCV,
        PPC,
        UNKNOWN
    }

    public static OsType getOsType() {
        return OS_TYPE;
    }

    public static OsType getOsType(String os) {
        if(os != null) {
            String osLower = os.toLowerCase(Locale.ENGLISH);
            if (osLower.contains("win")) {
                return OsType.WINDOWS;
            } else if (osLower.contains("mac")) {
                return OsType.MAC;
            } else if (osLower.contains("linux")) {
                return OsType.LINUX;
            } else if (osLower.contains("sunos")) {
                return OsType.SOLARIS;
            }
        }
        return OsType.UNKNOWN;
    }

    public static JreArch getJreArch() {
        return JRE_ARCH;
    }

    public static JreArch getJreArch(String arch) {
        if(arch != null) {
            String archLower = arch.toLowerCase(Locale.ENGLISH);
            if (archLower.equals("arm")) {
                return JreArch.ARM;
            }
            if (archLower.contains("amd64") || archLower.contains("x86_64")) {
                return JreArch.X86_64;
            }
            if (archLower.contains("86")) { // x86, i386, i486, i586, i686
                return JreArch.X86;
            }
            if (archLower.startsWith("aarch") || archLower.startsWith("arm")) {
                return JreArch.AARCH64;
            }
            if (archLower.startsWith("riscv") || archLower.startsWith("rv")) {
                return JreArch.RISCV;
            }
            if (archLower.startsWith("ppc") || archLower.startsWith("power")) {
                return JreArch.PPC;
            }
        }
        return JreArch.UNKNOWN;
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

    /**
     * The semantic version of the OS (e.g. "1.2.3")
     */
    public static Version getOsVersion() {
        if (osVersion == null) {
            try {
                switch(OS_TYPE) {
                    case WINDOWS:
                        // Windows is missing patch release, read it from registry
                        osVersion = WindowsUtilities.getOsVersion();
                        break;
                    default:
                        String version = System.getProperty("os.version", "0.0.0");
                        while(version.split("\\.").length < 3) {
                            version += ".0";
                        }
                        osVersion = Version.valueOf(version);
                }
            } catch(ParseException | IllegalArgumentException e) {
                log.warn("Unable to parse OS version as a semantic version", e);
                osVersion = Version.forIntegers(0, 0, 0);
            }
        }
        return osVersion;
    }

    /**
     * The human-readable display version of the OS (e.g. "22.04.1 LTS (Jammy Jellyfish)")
     */
    public static String getOsDisplayVersion() {
        if (osDisplayVersion == null) {
            switch(OS_TYPE) {
                case WINDOWS:
                    osDisplayVersion = WindowsUtilities.getOsDisplayVersion();
                    break;
                case MAC:
                    osDisplayVersion = MacUtilities.getOsDisplayVersion();
                    break;
                case LINUX:
                    osDisplayVersion = UnixUtilities.getOsDisplayVersion();
                    break;
                default:
                    osDisplayVersion = System.getProperty("os.version", "0.0.0");
            }
        }
        return osDisplayVersion;
    }

    /**
     * The human-readable display name of the OS (e.g. "Windows 10" or "Ubuntu")
     */
    public static String getOsDisplayName() {
        if(osName == null) {
            switch(OS_TYPE) {
                case LINUX:
                    // "Linux" is too generic, get the flavor (e.g. Ubuntu, Fedora)
                    osName = UnixUtilities.getOsDisplayName();
                    break;
                default:
                    osName = System.getProperty("os.name", "Unknown");
            }
        }
        return osName;
    }

    public static boolean isAdmin() {
        switch(OS_TYPE) {
            case WINDOWS:
                return ShellUtilities.execute("net", "session");
            default:
                return whoami().equals("root");
        }
    }

    public static String whoami() {
        String whoami = System.getProperty("user.name");
        if(whoami == null || whoami.trim().isEmpty()) {
            // Fallback on Command line
            whoami = ShellUtilities.executeRaw("whoami").trim();
        }
        return whoami;
    }

    public static Version getJavaVersion() {
        return getJavaVersion(System.getProperty("java.version"));
    }

    /**
     * Call a java command (e.g. java) with "--version" and parse the output
     * The double dash "--" is since JDK9 but important to send the command output to stdout
     */
    public static Version getJavaVersion(Path javaCommand) {
        return getJavaVersion(ShellUtilities.executeRaw(javaCommand.toString(), "--version"));
    }

    public static int getProcessId() {
        if(pid == null) {
            // Try Java 9+
            if(Constants.JAVA_VERSION.getMajorVersion() >= 9) {
                pid = getProcessIdJigsaw();
            }
            // Try JNA
            if(pid == null || pid == -1) {
                pid = SystemUtilities.isWindows() ? WindowsUtilities.getProcessId() : UnixUtilities.getProcessId();
            }
        }
        return pid;
    }

    private static int getProcessIdJigsaw() {
        try {
            Class processHandle = Class.forName("java.lang.ProcessHandle");
            Method current = processHandle.getDeclaredMethod("current");
            Method pid = processHandle.getDeclaredMethod("pid");
            Object processHandleInstance = current.invoke(processHandle);
            Object pidValue = pid.invoke(processHandleInstance);
            if(pidValue instanceof Long) {
                return ((Long)pidValue).intValue();
            }
        } catch(Throwable t) {
            log.warn("Could not get process ID using Java 9+, will attempt to fallback to JNA", t);
        }
        return -1;
    }

    /**
     * Handle Java versioning nuances
     * To eventually be replaced with <code>java.lang.Runtime.Version</code> (JDK9+)
     */
    public static Version getJavaVersion(String version) {
        String[] parts = version.trim().split("\\D+");

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
     * todo: make this return a sane directory for running via ide
     *
     * @return A String value representing the absolute path to the currently running
     * jar
     */
    public static Path getJarPath() {
        // jarPath won't change, send the cached value if we have it
        if (jarPath != null) return jarPath;
        try {
            String url = URLDecoder.decode(SystemUtilities.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            jarPath = new File(url).toPath();
            if (jarPath == null) return null;
            jarPath = jarPath.toAbsolutePath();
        } catch(InvalidPathException | UnsupportedEncodingException ex) {
            log.error("Unable to determine Jar path", ex);
        }
        return jarPath;
    }

    /**
     * Returns the folder containing the running jar
     * or null if no .jar is found (such as running from IDE)
     */
    public static Path getJarParentPath(){
        Path path = getJarPath();
        if (path == null || path.getParent() == null) return null;
        return path.getParent();
    }

    /**
     * Returns the jar's parent path, or a fallback if we're not a jar
     */
    public static Path getJarParentPath(String relativeFallback) {
        return getJarParentPath().resolve(SystemUtilities.isJar() ? "": relativeFallback).normalize();
    }

    /**
     * Returns the app's path, calculated from the jar location
     * or working directory if none can be found
     */
    public static Path getAppPath() {
        Path appPath = getJarParentPath();
        if(appPath == null) {
            // We should never get here
            appPath = Paths.get(System.getProperty("user.dir"));
        }

        // Assume we're installed and running from /Applications/QZ Tray.app/Contents/Resources/qz-tray.jar
        if(appPath.endsWith("Resources")) {
            return appPath.getParent().getParent();
        }
        // For all other use-cases, qz-tray.jar is installed in the root of the application
        return appPath;
    }

    /**
     * Determine if the current Operating System is Windows
     *
     * @return {@code true} if Windows, {@code false} otherwise
     */
    public static boolean isWindows() {
        return OS_TYPE == OsType.WINDOWS;
    }

    /**
     * Determine if the current Operating System is Mac OS
     *
     * @return {@code true} if Mac OS, {@code false} otherwise
     */
    public static boolean isMac() {
        return OS_TYPE == OsType.MAC;
    }

    /**
     * Determine if the current Operating System is Linux
     *
     * @return {@code true} if Linux, {@code false} otherwise
     */
    public static boolean isLinux() {
        return OS_TYPE == OsType.LINUX;
    }

    /**
     * Determine if the current Operating System is Unix
     *
     * @return {@code true} if Unix, {@code false} otherwise
     */
    public static boolean isUnix() {
        if(OS_NAME != null) {
            String osLower = OS_NAME.toLowerCase(Locale.ENGLISH);
            return OS_TYPE == OsType.MAC || OS_TYPE == OsType.SOLARIS || OS_TYPE == OsType.LINUX ||
                    osLower.contains("nix") || osLower.indexOf("aix") > 0;
        }
        return false;
    }

    /**
     * Determine if the current Operating System is Solaris
     *
     * @return {@code true} if Solaris, {@code false} otherwise
     */
    public static boolean isSolaris() {
        return OS_TYPE == OsType.SOLARIS;
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
                darkDesktop = UnixUtilities.isDarkMode();
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
                return getProcessId() != -1;
            } else if (SystemUtilities.isWindows() && SystemUtilities.getOsVersion().getMajorVersion() >= 10) {
                return true;
            }
        }
        return false;
    }

    public static boolean setSystemLookAndFeel() {
        try {
            UIManager.getDefaults().put("Button.showMnemonics", Boolean.TRUE);
            boolean darculaThemeNeeded = true;
            if(!isMac() && (isUnix() && UnixUtilities.isDarkMode())) {
                darculaThemeNeeded = false;
            }
            if(isDarkDesktop() && darculaThemeNeeded) {
                UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            adjustThemeColors();
            return true;
        } catch (Throwable t) {
            log.warn("Error getting the default look and feel");
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
        // Assume 0,0 are bad coordinates
        if (position == null || (position.getX() == 0 && position.getY() == 0)) {
            log.debug("Invalid dialog position provided: {}, we'll center on first monitor instead", position);
            dialog.setLocationRelativeTo(null);
            return;
        }

        //adjust for dpi scaling
        double dpiScale = getWindowScaleFactor();
        if (dpiScale == 0) {
            log.debug("Invalid window scale value: {}, we'll center on the primary monitor instead", dpiScale);
            dialog.setLocationRelativeTo(null);
            return;
        }

        Rectangle rect = new Rectangle((int)(position.x * dpiScale), (int)(position.y * dpiScale), dialog.getWidth(), dialog.getHeight());
        rect.translate(-dialog.getWidth() / 2, -dialog.getHeight() / 2);
        Point p = new Point((int)rect.getCenterX(), (int)rect.getCenterY());
        log.debug("Calculated dialog centered at: {}", p);

        if (!isWindowLocationValid(rect)) {
            log.debug("Dialog position provided is out of bounds: {}, we'll center on the primary monitor instead", p);
            dialog.setLocationRelativeTo(null);
            return;
        }

        dialog.setLocation(rect.getLocation());
    }

    /**
     * Validates if a given rectangle is within screen bounds
     */
    public static boolean isWindowLocationValid(Rectangle window) {
        if(GraphicsEnvironment.isHeadless()) {
            return false;
        }

        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        Area area = new Area();
        for(GraphicsDevice gd : devices) {
            for(GraphicsConfiguration gc : gd.getConfigurations()) {
                area.add(new Area(gc.getBounds()));
            }
        }
        return area.contains(window);
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
        // Linux/Unix on JDK11 requires JNA calls to Gdk
        return UnixUtilities.getScaleFactor();
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
        return UnixUtilities.getScaleFactor() > 1;
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

    /**
     * Todo:
     * @return true if running from a jar, false if running from IDE
     */
    public static boolean isInstalled() {
        Path path = getJarParentPath();
        if(path == null) {
            return false;
        }
        // Assume dist or out are signs we're running from some form of build directory
        return !path.endsWith("dist") && !path.endsWith("out");
    }

    /**
     * Allows in-line insertion of a property before another
     * @param value the end of a value to insert before, assumes to end with File.pathSeparator
     */
    public static void insertPathProperty(String property, String value, String insertBefore) {
        insertPathProperty(property, value, File.pathSeparator, insertBefore);
    }

    private static void insertPathProperty(String property, String value, String delimiter, String insertBefore) {
        String currentValue = System.getProperty(property);
        if(currentValue == null || currentValue.trim().isEmpty()) {
            // Set it directly, there's nothing there
            System.setProperty(property, value);
            return;
        }
        // Blindly split on delimiter, safe according to POSIX standards
        // See also: https://stackoverflow.com/a/29213487/3196753
        String[] paths = currentValue.split(delimiter);
        StringBuilder finalProperty = new StringBuilder();
        boolean inserted = false;
        for(String path : paths) {
            if(!inserted && path.endsWith(insertBefore)) {
                finalProperty.append(value + delimiter);
                inserted = true;
            }
            finalProperty.append(path + delimiter);
        }
        // Add to end if delimiter wasn't found
        if(!inserted) {
            finalProperty.append(value);
        }
        // Truncate trailing delimiter
        if(StringUtils.endsWith(finalProperty, delimiter)) {
            finalProperty.setLength(finalProperty.length() - delimiter.length());
        }
        System.setProperty(property, finalProperty.toString());
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

    public static boolean hasMonocle() {
        if(hasMonocle == null) {
            try {
                Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory");
                hasMonocle = true;
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                hasMonocle = false;
            }
        }
        return hasMonocle;
    }

    public static final Version[] JDK_8266929_VERSIONS = {
            Version.valueOf("11.0.11"),
            Version.valueOf("1.8.0+291"),
            Version.valueOf("1.8.0+292")
    };

    /**
     * Fixes JDK-8266929 by clearing the oidTable
     * See also: https://github.com/qzind/tray/issues/814
     */
    public static void clearAlgorithms() {
        boolean needsPatch = false;
        for(Version affected : JDK_8266929_VERSIONS) {
            if(affected.getMajorVersion() == 1) {
                // Java 1.8 honors build/update information
                if(affected.compareWithBuildsTo(Constants.JAVA_VERSION) == 0) {
                    needsPatch = true;
                }
            } else if (affected.compareTo(Constants.JAVA_VERSION) == 0) {
                // Java 9.0+ ignores build/update information
                needsPatch = true;
            }
        }
        if(!needsPatch) {
            log.debug("Skipping JDK-8266929 patch for {}", Constants.JAVA_VERSION);
            return;
        }
        try {
            log.info("Applying JDK-8266929 patch");
            Class<?> algorithmIdClass = Class.forName("sun.security.x509.AlgorithmId");
            java.lang.reflect.Field oidTableField = algorithmIdClass.getDeclaredField("oidTable");
            oidTableField.setAccessible(true);
            // Set oidTable to null
            oidTableField.set(algorithmIdClass, null);
            // Java 1.8
            if(Constants.JAVA_VERSION.getMajorVersion() == 1) {
                java.lang.reflect.Field initOidTableField = algorithmIdClass.getDeclaredField("initOidTable");
                initOidTableField.setAccessible(true);
                // Set init flag back to false
                initOidTableField.set(algorithmIdClass, false);
            }
            log.info("Successfully applied JDK-8266929 patch");
        } catch (Exception e) {
            log.warn("Unable to apply JDK-8266929 patch.  Some algorithms may fail.", e);
        }
    }

    public static String getHostName() {
        String hostName = SystemUtilities.isWindows() ? WindowsUtilities.getHostName() : UnixUtilities.getHostName();
        if(hostName == null || hostName.trim().isEmpty()) {
            log.warn("Couldn't get hostname using internal techniques, will fallback to command line instead");
            hostName = ShellUtilities.getHostName().toUpperCase(); // uppercase to match others
        }
        return hostName;
    }

    /**
     * A challenge which can only be calculated by an app installed and running on this machine
     * Calculates two bytes:
     * - First byte is a salted version of the timestamp of qz-tray.jar
     * - Second byte is a throw-away byte
     * - Bytes are converted to Base64 and returned as a String
     */
    public static String calculateSaltedChallenge() {
        int salt = new Random().nextInt(9);
        long salted = (calculateChallenge() * 10) + salt;
        long obfuscated = salted * new Random().nextInt(9);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
        buffer.putLong(obfuscated);
        buffer.putLong(0, salted);
        return new String(Base64.encodeBase64(buffer.array(), false), StandardCharsets.UTF_8);
    }

    private static long calculateChallenge() {
        if(getJarPath() != null) {
            if (getJarPath().toFile().exists()) {
                return getJarPath().toFile().lastModified();
            }
        }
        return -1L; // Fallback when running from IDE
    }

    /**
     * Decodes challenge string to see if it originated from this application
     * - Base64 string is decoded into two bytes
     * - First byte is unsalted
     * - Second byte is ignored
     * - If unsalted value of first byte matches the timestamp of qz-tray.jar, return true
     * - If unsalted value doesn't match or if any exceptions occurred, we assume the message is invalid
     */
    public static boolean validateSaltedChallenge(String message) {
        try {
            log.info("Attempting to validating challenge: {}", message);
            byte[] decoded = Base64.decodeBase64(message);
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
            buffer.put(decoded);
            // Explicit cast, per https://github.com/qzind/tray/issues/1055
            ((Buffer)buffer).flip();//need flip
            long salted = buffer.getLong(0); // only first byte matters
            long challenge = salted / 10L;
            return challenge == calculateChallenge();
        } catch(Exception ignore) {
            log.warn("An exception occurred validating challenge: {}", message, ignore);
        }
        return false;
    }

    /**
     * Cross-platform SystemTray detector
     */
    public static boolean isSystemTraySupported(boolean headless) {
        if(!headless) {
            switch(getOsType()) {
                case WINDOWS:
                    if(WindowsUtilities.isHiddenSystemTray()) {
                        return false;
                    }
                    break;
                case MAC:
                    break;
                default:
                    // Linux System Tray support is abysmal, always use TaskbarTrayIcon
                    return false;
            }
            return SystemTray.isSupported();
        }
        return false;
    }
}
