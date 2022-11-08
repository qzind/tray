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

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibC;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Helper functions for both Linux and Unix
 */
public class UnixUtilities {
    private static final Logger log = LogManager.getLogger(UnixUtilities.class);
    private static final String[] OS_NAME_KEYS = {"NAME", "DISTRIB_ID"};
    private static final String[] OS_VERSION_KEYS = {"VERSION", "DISTRIB_RELEASE"};
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };
    private static final String[] OS_RELEASE_FILES = {"/etc/os-release", "/usr/lib/os-release", "/etc/lsb-release", "/etc/redhat-release"};
    private static String uname;
    private static String unixRelease;
    private static String unixVersion;
    private static Integer pid;
    private static String foundElevator;

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
     * Returns the name of the OS, trying to obtain distro information if available
     */
    public static String getOsDisplayName() {
        if (unixRelease == null) {
            try {
                Map<String,String> map = getReleaseMap();
                for (String nameKey: OS_NAME_KEYS) {
                    if (map.containsKey(nameKey)) {
                        unixRelease = UnixUtilities.getReleaseMap().get(nameKey);
                        break;
                    }
                }
            } catch(FileNotFoundException e) {
                log.warn("Could not find a suitable os-release file {}", Arrays.toString(OS_RELEASE_FILES));
            }
            if(unixRelease == null) {
                log.warn("Could not find name key {} in files {}",  Arrays.toString(OS_NAME_KEYS), Arrays.toString(OS_RELEASE_FILES));
                unixRelease = System.getProperty("os.name", "Unknown");
            }
        }
        return unixRelease;
    }

    /**
     * The human-readable display version of the Linux/Unix OS
     */
    public static String getOsDisplayVersion() {
        if (unixVersion == null) {
            try {
                Map<String,String> map = getReleaseMap();
                for(String versionKey : OS_VERSION_KEYS) {
                    if (map.containsKey(versionKey)) {
                        unixVersion = UnixUtilities.getReleaseMap().get(versionKey);
                        break;
                    }
                }
            }
            catch(FileNotFoundException e) {
                log.warn("Could not find a suitable os-release file {}", Arrays.toString(OS_RELEASE_FILES));
            }
            if(unixVersion == null) {
                log.warn("Could not find version key {} in files {}",  Arrays.toString(OS_VERSION_KEYS), Arrays.toString(OS_RELEASE_FILES));
                // If we can't get version info from a file, run the "lsb_release" command
                String lsbRelease = ShellUtilities.executeRaw(new String[] {"lsb_release", "-ds"}).trim();
                if(!lsbRelease.isEmpty()) {
                    unixVersion = lsbRelease;
                } else {
                    unixVersion = System.getProperty("os.version", "0.0.0");
                }
            }
        }
        return unixVersion;
    }

    private static Map<String, String> getReleaseMap() throws FileNotFoundException {
        HashMap<String,String> map = new HashMap<>();
        Path release = findOsReleaseFile();
        String result = ShellUtilities.executeRaw(
                new String[] {"cat", release.toString()}
        );

        String[] results = result.split("\n");
        for (String line: results) {
            String[] tokens = line.split("=", 2);
            if (tokens.length != 2) continue;
            map.put(tokens[0], tokens[1].replaceAll("\"", ""));
        }
        return map;
    }

    private static String findElevator() throws IOException {
        if(foundElevator == null) {
            for(String elevator : KNOWN_ELEVATORS) {
                if (ShellUtilities.execute("which", elevator)) {
                    foundElevator = elevator;
                    break;
                }
            }
            throw new IOException("Can't find an installed utility " + Arrays.toString(KNOWN_ELEVATORS) + " to elevate permissions.");
        }
        return foundElevator;
    }

    private static Path findOsReleaseFile() throws FileNotFoundException {
        // Search by name for the supported distros, in order of preference
        for(String release : OS_RELEASE_FILES) {
            Path path = Paths.get(release);
            if (Files.exists(path)) return path;
        }
        Stream<Path> s;
        try {
            s = Files.find(
                    // If that fails, try to find any *-release file
                    Paths.get("/etc/"),
                    1,
                    (path, basicFileAttributes) -> path.getFileName().toString().endsWith("-release"),
                    FileVisitOption.FOLLOW_LINKS
            );
            // If no element is found this will throw a NoSuchElementException
            return s.findFirst().get();
        } catch(Exception ignore) {}
        throw new FileNotFoundException("Could not find os-release file");
    }

    public static boolean elevatedFileCopy(Path source, Path destination) {
        // Don't prompt if it's not needed
        try {
            // Note: preserveFileDate=false per https://github.com/qzind/tray/issues/1011
            FileUtils.copyFile(source.toFile(), destination.toFile(), false);
            return true;
        } catch(IOException ignore) {}

        try {
            String[] command = {findElevator(), "cp", source.toString(), destination.toString()};
            return ShellUtilities.execute(command);
        } catch(IOException io) {
            log.error("Copy failed.  You'll have do this manually.", io);
        }
        return false;
    }

    /**
     * Runs a shell command to determine if "Dark" desktop theme is enabled
     * @return true if enabled, false if not
     */
    public static boolean isDarkMode() {
        return !ShellUtilities.execute(new String[] { "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme" }, new String[] { "dark" }, true, true).isEmpty();
    }

    public static double getScaleFactor() {
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("11.0.0"))) {
            return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        }
        return GtkUtilities.getScaleFactor();
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
        if(!SystemUtilities.isLinux()) return false;
        return unixRelease != null && getOsDisplayName().contains("Fedora");
    }
}
