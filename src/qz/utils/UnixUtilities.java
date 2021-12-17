/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2021 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibC;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper functions for both Linux and MacOS
 */
public class UnixUtilities {
    private static final Logger log = LogManager.getLogger(UnixUtilities.class);
    private static String uname;
    private static String linuxRelease;
    private static Integer pid;

    static String getHostName() {
        String hostName = null;
        try {
            byte[] bytes = new byte[255];
            if (LibC.INSTANCE.gethostname(bytes, bytes.length) == 0) {
                hostName = Native.toString(bytes);
            }
        } catch(Throwable ignore) {}
        return hostName;
    }

    static int getProcessId() {
        if(pid == null) {
            try {
                pid = UnixUtilities.CLibrary.INSTANCE.getpid();
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
        int getpid();
    }

    /**
     * Returns the output of {@code uname -a} shell command, useful for parsing the Linux Version
     *
     * @return the output of {@code uname -a}, or null if not running Linux
     */
    public static String getUname() {
        if (SystemUtilities.isUnix() && uname == null) {
            uname = ShellUtilities.execute(
                    new String[] {"uname", "-a"},
                    null
            );
        }

        return uname;
    }

    /**
     * Returns the output of {@code cat /etc/lsb-release} or equivalent
     *
     * @return the output of the command or null if not running Linux
     */
    public static String getLinuxRelease() {
        if (SystemUtilities.isLinux() && linuxRelease == null) {
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
     * Returns whether the output of {@code uname -a} shell command contains "Ubuntu"
     *
     * @return {@code true} if this OS is Ubuntu
     */
    public static boolean isUbuntu() {
        if(!SystemUtilities.isLinux()) {
            return false;
        }
        getUname();
        return uname != null && uname.contains("Ubuntu");
    }


    /**
     * Returns whether the output of <code>cat /etc/redhat-release/code> shell command contains "Fedora"
     *
     * @return {@code true} if this OS is Fedora
     */
    public static boolean isFedora() {
        if(!SystemUtilities.isLinux()) {
            return false;
        }
        getLinuxRelease();
        return linuxRelease != null && linuxRelease.contains("Fedora");
    }
}
