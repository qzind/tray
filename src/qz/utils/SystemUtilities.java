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
import org.slf4j.LoggerFactory;
import qz.common.Constants;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for OS detection functions.
 *
 * @author Tres Finocchiaro
 */
public class SystemUtilities {

    // Name of the os, i.e. "Windows XP", "Mac OS X"
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static ConcurrentHashMap<String,String> libVersionMap = new ConcurrentHashMap<>();

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
            default:
                // Assume JDK8 and lower format
                return Version.forIntegers(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
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

    public static String getLibVersion (String name, String packageName) {
        //todo try getting name from classloader
        //if not, look at the jar metadata
        if (libVersionMap == null){
            libVersionMap = getVersionMap();
        }
        return libVersionMap.get(name);
    }

    private static ConcurrentHashMap<String,String> getVersionMap () {
        //Results by <lib name, version>
        final ConcurrentHashMap<String,String> resultMap = new ConcurrentHashMap<>();
        try {
            final ArrayList<Thread> threads = new ArrayList<>();
            //Hack to get a ref to our jar
            URI jarLocation = new URI("jar:" + SystemUtilities.class.getProtectionDomain().getCodeSource().getLocation().toString());
            //This jdk1.7x nio util lets us look into the jar, without it we would need ZipStream
            FileSystem fs = FileSystems.newFileSystem(jarLocation, new HashMap<String,String>());

            Files.walkFileTree(fs.getPath("/META-INF/maven"), new HashSet<FileVisitOption>(), 3, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".properties")) {
                        //launch a thread to scrape our data, keep a ref to the thread
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    List<String> data = Files.readAllLines(file, Charset.defaultCharset());
                                    String id = data.get(4);
                                    id = id.substring(id.lastIndexOf('=') + 1);
                                    String version = data.get(2);
                                    version = version.substring(version.lastIndexOf('=') + 1);
                                    resultMap.put(id, version);
                                }
                                catch(Exception ignore) {}
                            }
                        });
                        threads.add(t);
                        t.start();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            //Wait for all threads to finish
            for(Thread t : threads) t.join();

            for(Map.Entry<String,String> item : resultMap.entrySet()) {
                System.out.println(item.getKey() + " : " + item.getValue());
            }
        } catch(Exception ignore) {
            return null;
        }

        return resultMap;
    }

    public static boolean setSystemLookAndFeel() {
        try {
            UIManager.getDefaults().put("Button.showMnemonics", Boolean.TRUE);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(SystemUtilities.class).warn("Error getting the default look and feel");
        }
        return false;
    }
}
