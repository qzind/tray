/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import java.io.*;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Properties;

/**
 * Utility class for creating, querying and removing startup shortcuts and
 * desktop shortcuts.
 *
 * @author Tres Finocchiaro
 */
public abstract class DeployUtilities {

    // System logger
    protected static final Logger log = LoggerFactory.getLogger(DeployUtilities.class);

    // Default shortcut name to create
    static private final String DEFAULT_SHORTCUT_NAME = "Java Shortcut";

    // Newline character, which changes between Unix and Windows
    static private final String NEWLINE = SystemUtilities.isWindows()? "\r\n":"\n";

    private String jarPath;
    private String shortcutName;

    /**
     * Creates a startup item for the current OS. Automatically detects the OS
     * and places the startup item in the user's startup area respectively; to
     * be auto-launched when the user first logs in to the desktop.
     *
     * @return Returns <code>true</code> if the startup item was created
     */
    public abstract boolean createStartupShortcut();

    /**
     * Test whether or not a startup shortcut for the specified shortcutName
     * exists on this system
     *
     * @return true if a startup shortcut exists on this system, false otherwise
     */
    public abstract boolean hasStartupShortcut();

    /**
     * Test whether or not a desktop shortcut for the specified shortcutName
     * exists on this system
     *
     * @return true if a desktop shortcut exists on this system, false otherwise
     */
    public abstract boolean hasDesktopShortcut();

    /**
     * Creates a startup for the current OS. Automatically detects the OS and
     * places the shortcut item on the user's Desktop.
     *
     * @return Returns <code>true</code> if the startup item was created
     */
    public abstract boolean createDesktopShortcut();

    /**
     * Removes a startup item for the current OS. Automatically detects the OS
     * and removes the startup item from the user's startup area respectively.
     *
     * @return Returns <code>true</code> if the startup item was removed
     */
    public abstract boolean removeStartupShortcut();

    /**
     * Removes a desktop shortcut for the current OS. Automatically detects the
     * OS and removes the shortcut from the current user's Desktop.
     *
     * @return Returns <code>true</code> if the Desktop shortcut item was
     * removed
     */
    public abstract boolean removeDesktopShortcut();

    /**
     * Single function to be used to dynamically create various shortcut types
     *
     * @param toggleType ToggleType.STARTUP or ToggleType.DESKTOP
     * @return Whether or not the shortcut creation was successful
     */
    public boolean createShortcut(ToggleType toggleType) {
        switch(toggleType) {
            case STARTUP:
                return hasShortcut(ToggleType.STARTUP) || createStartupShortcut();
            case DESKTOP:
                return hasShortcut(ToggleType.DESKTOP) || createDesktopShortcut();
            default:
                log.warn("Sorry, creating {} shortcuts are not yet supported", toggleType);
                return false;
        }
    }

    /**
     * Single function to be used to dynamically check if a shortcut already exists
     *
     * @param toggleType Shortcut type, i.e. <code>ToggleType.STARTUP</code> or <code>ToggleType.DESKTOP</code>
     * @return Whether or not the shortcut already exists
     */
    private boolean hasShortcut(ToggleType toggleType) {
        boolean hasShortcut = false;
        switch(toggleType) {
            case STARTUP:
                hasShortcut = hasStartupShortcut();
                break;
            case DESKTOP:
                hasShortcut = hasDesktopShortcut();
                break;
            default:
                log.warn("Sorry, checking for {} shortcuts are not yet supported", toggleType);
        }

        if (hasShortcut) {
            log.info("The {} shortcut for {} ({}) exists", toggleType, getShortcutName(), getJarPath());
        }

        return hasShortcut;
    }

    /**
     * Single function to be used to dynamically remove various shortcut types
     *
     * @param toggleType ToggleType.STARTUP or ToggleType.DESKTOP
     * @return Whether or not the shortcut removal was successful
     */
    public boolean removeShortcut(ToggleType toggleType) {
        switch(toggleType) {
            case STARTUP:
                return !hasShortcut(ToggleType.STARTUP) || removeStartupShortcut();
            case DESKTOP:
                return !hasShortcut(ToggleType.DESKTOP) || removeDesktopShortcut();
            default:
                log.warn("Sorry, removing {} shortcuts are not yet supported", toggleType);
                return false;
        }
    }

    /**
     * Parses the parent directory from an absolute file URL. This will not work
     * with relative paths.<code>
     * // Good:
     * getWorkingPath("C:\Folder\MyFile.jar");
     * <p/>
     * // Bad:
     * getWorkingPath("C:\Folder\SubFolder\..\MyFile.jar");
     * </code>
     *
     * @param filePath Absolute path to a jar file
     * @return The calculated working path value, or an empty string if one
     * could not be determined
     */
    private static String getParentDirectory(String filePath) {
        // Working path should always default to the JARs parent folder
        int lastSlash = filePath.lastIndexOf(File.separator);
        return lastSlash < 0? "":filePath.substring(0, lastSlash);
    }

    public String getParentDirectory() {
        return getParentDirectory(getJarPath());
    }


    public void setShortcutName(String shortcutName) {
        if (shortcutName != null) {
            this.shortcutName = shortcutName;
        }
    }

    public String getShortcutName() {
        return shortcutName == null? DEFAULT_SHORTCUT_NAME:shortcutName;
    }

    /**
     * Detects the OS and creates the appropriate shortcut creator
     *
     * @return The appropriate shortcut creator for the currently running OS
     */
    public static DeployUtilities getSystemShortcutCreator() {
        if (SystemUtilities.isWindows()) {
            return new WindowsDeploy();
        } else if (SystemUtilities.isMac()) {
            return new MacDeploy();
        } else {
            return new LinuxDeploy();
        }
    }

    /**
     * Returns whether or not a file exists
     *
     * @param filePath The full path to a file
     * @return True if the specified filePath exists.  False otherwise.
     */
    static boolean fileExists(String filePath) {
        try {
            return new File(filePath).exists();
        }
        catch(SecurityException e) {
            log.error("SecurityException while checking for file {}", filePath, e);
            return false;
        }
    }

    /**
     * Deletes the specified file
     *
     * @param filePath The full file path to be deleted
     * @return Whether or not the file deletion was successful
     */
    static boolean deleteFile(String filePath) {
        File f = new File(filePath);
        try {
            return f.delete();
        }
        catch(SecurityException e) {
            log.error("Error while deleting: {}", filePath, e);
        }
        return false;
    }

    /**
     * Writes the contents of <code>String[] array</code> to the specified
     * <code>filePath</code> used for creating launcher shortcuts for both
     * Windows and Linux taking OS-specific newlines into account
     *
     * @param filePath Absolute file path to be written to
     * @param array    Array of lines to be written
     * @return Whether or not the write was successful
     */
    static boolean writeArrayToFile(String filePath, String[] array) {
        log.info("Writing array contents to file: {}: \n{}", filePath, Arrays.toString(array));
        BufferedWriter writer = null;
        boolean returnVal = false;
        try {
            writer = new BufferedWriter(new FileWriter(new File(filePath)));
            for(String line : array) {
                writer.write(line + NEWLINE);
            }
            returnVal = true;
        }
        catch(IOException e) {
            log.error("Could not write file: {}", filePath, e);
        }
        finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                setExecutable(filePath);
            }
            catch(IOException ignore) { }
        }
        return returnVal;
    }

    /**
     * Sets the executable permission flag for a file. This only works on
     * Linux/Unix.
     *
     * @param filePath The full file path to set the execute flag on
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean setExecutable(String filePath) {
        if (!SystemUtilities.isWindows()) {
            try {
                File f = new File(filePath);
                f.setExecutable(true);
                return true;
            }
            catch(SecurityException e) {
                log.error("Unable to set file as executable: {}", filePath, e);
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Gets the path to qz-tray.properties
     */
    private static String detectPropertiesPath() {
        // Use supplied path from IDE or command line
        // i.e  -DsslPropertiesFile=C:\qz-tray.properties
        String override = System.getProperty("sslPropertiesFile");
        if (override != null) {
            return override;
        }

        String jarPath = detectJarPath();
        String propFile = Constants.PROPS_FILE + ".properties";
        return getParentDirectory(jarPath) + File.separator + propFile;
    }

    /**
     * Returns a properties object containing the SSL properties infor
     */
    public static Properties loadTrayProperties() {
        Properties trayProps = new Properties();
        String trayPropsPath = DeployUtilities.detectPropertiesPath();
        log.info("Main properties file " + trayPropsPath);

        File propsFile = new File(trayPropsPath);
        try(FileInputStream inputStream = new FileInputStream(propsFile)) {
            trayProps.load(inputStream);
            return trayProps;
        }
        catch(IOException e) {
            e.printStackTrace();
            log.warn("Failed to load properties file!");
            return null;
        }
    }

    /**
     * Determines the currently running Jar's absolute path on the local filesystem
     *
     * @return A String value representing the absolute path to the currently running
     * jar
     */
    private static String detectJarPath() {
        try {
            String jarPath = new File(DeployUtilities.class.getProtectionDomain()
                                    .getCodeSource().getLocation().getPath()).getCanonicalPath();
            // Fix characters that get URL encoded when calling getPath()
            return URLDecoder.decode(jarPath, "UTF-8");
        }
        catch(IOException ex) {
            log.error("Unable to determine Jar path", ex);
        }
        return null;
    }

    /**
     * Returns the jar which we will create a shortcut for
     *
     * @return The path to the jar path which has been set
     */
    public String getJarPath() {
        if (jarPath == null) {
            jarPath = detectJarPath();
        }
        return jarPath;
    }

    /**
     * Small Enum for differentiating "desktop" and "startup"
     */
    public enum ToggleType {
        STARTUP, DESKTOP;

        /**
         * Returns the English description of this object
         *
         * @return The string "startup" or "desktop"
         */
        @Override
        public String toString() {
            return getName();
        }

        /**
         * Returns the English description of this object
         *
         * @return The string "startup" or "desktop"
         */
        public String getName() {
            return this.name() == null? null:this.name().toLowerCase();
        }
    }
}
