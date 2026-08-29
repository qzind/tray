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
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.gtk.GtkUtilities;
import qz.utils.kde.KdeUtilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Helper functions for both Linux and Unix
 */
public class UnixUtilities {
    public enum DesktopEnvironment {
        KDE("kreadconfig6", "kreadconfig5"), // keep before gsettings, keep descending
        GNOME("gsettings"),
        UNKNOWN;

        public static String binaryFound = "false";

        final String[] binaries;
        DesktopEnvironment(String ... binaries) {
            this.binaries = binaries;
        }

        boolean isLikely() {
            for(String binary : binaries) {
                if(ShellUtilities.execute("which", binary)) {
                    binaryFound = binary;
                    return true;
                }
            }
            return false;
        }
    }

    private static final Logger log = LogManager.getLogger(UnixUtilities.class);

    private static final String[] OS_FAMILY_KEYS = {"ID_LIKE", "ID" };
    private static final String[] OS_NAME_KEYS = {"NAME", "DISTRIB_ID"};
    private static final String[] OS_VERSION_KEYS = {"VERSION", "VERSION_ID", "DISTRIB_RELEASE"};
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };
    private static final String[] OS_RELEASE_FILES = {"/etc/os-release", "/usr/lib/os-release", "/etc/lsb-release", "/etc/redhat-release"};
    private static String distroFamily; // "debian", "arch", etc
    private static String displayName;
    private static String displayVersion;
    private static Integer pid;
    private static String foundElevator;
    private static final DesktopEnvironment desktop = getDesktopEnvironment();

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
        int getpid();
    }

    public static String getDistroFamily() {
        if(distroFamily == null) {
            try {
                Map<String,String> map = getReleaseMap();
                for(String distroKey : OS_FAMILY_KEYS) {
                    if (map.containsKey(distroKey)) {
                        distroFamily = map.get(distroKey);
                        break;
                    }
                }
            } catch(IOException ignore) {}
            if(distroFamily == null) {
                distroFamily = distroSlug(getDisplayName());
                if(distroFamily.isEmpty()) {
                    distroFamily = "unknown";
                }
                log.warn("Unable to detect distribution family, falling back to '{}' (calculated from '{}')", distroFamily, displayName);
            }
        }
        return distroFamily;
    }

    private static String distroSlug(String name) {
        if(name != null) {
            String osLower = name.replace("\"", "").toLowerCase(Locale.ENGLISH);
            if (osLower.startsWith("linux")) {
                // Fix "linux" before the name
                osLower = osLower.substring(5).trim();
            }
            return osLower.split("[\\s-]+")[0]; // return the first part of the name
        }
        return "";
    }

    /**
     * Returns the name of the OS, trying to obtain distro information if available
     */
    public static String getDisplayName() {
        if (displayName == null) {
            try {
                Map<String,String> map = getReleaseMap();
                for (String nameKey: OS_NAME_KEYS) {
                    if (map.containsKey(nameKey)) {
                        displayName = map.get(nameKey);
                        break;
                    }
                }
            } catch(IOException e) {
                log.warn("Could not find a suitable os-release file {}", Arrays.toString(OS_RELEASE_FILES));
            }
            if(displayName == null) {
                log.warn("Could not find name key {} in files {}",  Arrays.toString(OS_NAME_KEYS), Arrays.toString(OS_RELEASE_FILES));
                displayName = System.getProperty("os.name", "Unknown");
            }
        }
        return displayName;
    }

    /**
     * The human-readable display version of the Linux/Unix OS
     */
    public static String getOsDisplayVersion() {
        if (displayVersion == null) {
            try {
                Map<String,String> map = getReleaseMap();
                for(String versionKey : OS_VERSION_KEYS) {
                    if (map.containsKey(versionKey)) {
                        displayVersion = map.get(versionKey);
                        break;
                    }
                }
            }
            catch(IOException e) {
                log.warn("Could not find a suitable os-release file {}", Arrays.toString(OS_RELEASE_FILES));
            }
            if(displayVersion == null) {
                log.warn("Could not find version key {} in files {}",  Arrays.toString(OS_VERSION_KEYS), Arrays.toString(OS_RELEASE_FILES));
                // If we can't get version info from a file, run the "lsb_release" command
                String lsbRelease = ShellUtilities.executeRaw(new String[] {"lsb_release", "-ds"}).trim();
                if(!lsbRelease.isEmpty()) {
                    displayVersion = lsbRelease.replace("\"", "");
                } else {
                    displayVersion = System.getProperty("os.version", "0.0.0");
                }
            }
        }
        return displayVersion;
    }

    private static Map<String, String> getReleaseMap() throws IOException {
        HashMap<String,String> map = new HashMap<>();
        BufferedReader reader = null;
        try {
            Path release = findOsReleaseFile();
            reader = new BufferedReader(new FileReader(release.toFile()));
            String line;
            while((line = reader.readLine()) != null) {
                String[] tokens = line.split("=", 2);
                if (tokens.length != 2) continue;
                map.put(tokens[0], tokens[1].replaceAll("\"", ""));
            }
        } finally{
            if(reader != null) {
                reader.close();
            }
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

    public static boolean isDarkDesktop() {
        return switch(desktop) {
            case GNOME -> GtkUtilities.isDarkDesktop();
            case KDE -> KdeUtilities.isDarkDesktop();
            default -> false;
        };
    }

    static DesktopEnvironment getDesktopEnvironment() {
        if(desktop != null) {
            return desktop;
        }
        if(SystemUtilities.isMac()) {
            return DesktopEnvironment.UNKNOWN;
        }
        return Arrays.stream(DesktopEnvironment.values())
                .filter(DesktopEnvironment::isLikely)
                .findFirst()
                .orElse(DesktopEnvironment.UNKNOWN);
    }

    public static double getScaleFactor() {
        return GtkUtilities.getScaleFactor();
    }

    /**
     * Returns true only if the OS display name contains the word "ubuntu"
     * TODO: This may cause improper assumptions for kubuntu, xubuntu, etc
     */
    public static boolean isUbuntu() {
        if(!SystemUtilities.isLinux()) return false;
        return getDisplayName().toLowerCase().contains("ubuntu");
    }

    /**
     * Returns true if detected OS family is Debian or Debian-like.
     */
    public static boolean isDebian() {
        if(!SystemUtilities.isLinux()) return false;
        return getDistroFamily().equals("debian") || getDistroFamily().equals("ubuntu");
    }

    public static boolean isFedora() {
        if(!SystemUtilities.isLinux()) return false;
        return getDistroFamily().equals("fedora");
    }

    public static boolean isArch() {
        if(!SystemUtilities.isLinux()) return false;
        return getDistroFamily().equals("arch");
    }
}
