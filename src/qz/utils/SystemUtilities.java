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

import dorkbox.util.OSUtil;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.deploy.DeployUtilities;

import javax.swing.*;
import java.io.File;

/**
 * Utility class for OS detection functions.
 *
 * @author Tres Finocchiaro
 */
public class SystemUtilities {

    // Name of the os, i.e. "Windows XP", "Mac OS X"
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    private static String uname;
    private static String linuxRelease;


    /**
     * @return Lowercase version of the operating system name
     * identified by {@code System.getProperty("os.name");}.
     */
    public static String getOS() {
        return OS_NAME;
    }


    /**
     * Retrieve OS-specific Application Data directory such as:
     * {@code C:\Users\John\AppData\Roaming\.qz} on Windows
     * -- or --
     * {@code /Users/John/Library/Application Support/.qz} on Mac
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
        return (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.indexOf("aix") > 0 || OS_NAME.contains("sunos"));
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

    public static void setSystemLookAndFeel() {
        if (OSUtil.Linux.isUbuntu() && (OSUtil.DesktopEnv.get().equals(OSUtil.DesktopEnv.Env.Unity) ||
                OSUtil.DesktopEnv.get().equals(OSUtil.DesktopEnv.Env.Unity7))) return;
        try {
            UIManager.getDefaults().put("Button.showMnemonics", Boolean.TRUE);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return;
        }
        catch(Exception e) {
            LoggerFactory.getLogger(SystemUtilities.class).warn("Error getting the default look and feel");
        }
    }
}
