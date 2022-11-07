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

import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for managing all {@code Runtime.exec(...)} functions.
 *
 * @author Tres Finocchiaro
 */
public class ShellUtilities {

    private static final Logger log = LogManager.getLogger(ShellUtilities.class);

    // Shell environment overrides.  null = don't override
    public static String[] envp = null;

    // Make sure all shell calls are LANG=en_US.UTF-8
    static {
        if (!SystemUtilities.isWindows()) {
            // Cache existing; permit named overrides w/o full clobber
            Map<String,String> env = new HashMap<>(System.getenv());
            if (SystemUtilities.isMac()) {
                // Enable LANG overrides
                env.put("SOFTWARE", "");
            }
            // Functional equivalent of "export LANG=en_US.UTF-8"
            env.put("LANG", "C");
            String[] envp = new String[env.size()];
            int i = 0;
            for(Map.Entry<String,String> o : env.entrySet())
                envp[i++] = o.getKey() + "=" + o.getValue();

            ShellUtilities.envp = envp;
        }
    }

    public static boolean elevateCopy(Path source, Path dest) {
        source = source.toAbsolutePath().normalize();
        dest = dest.toAbsolutePath().normalize();

        if(SystemUtilities.isWindows()) {
            // JNA/Explorer will prompt if insufficient access
            if(!WindowsUtilities.nativeFileCopy(source, dest)) {
                // Fallback to a powershell trick
                return WindowsUtilities.elevatedFileCopy(source, dest);
            }
            return true;
        } else if(SystemUtilities.isMac()){
            // JNA/Finder will prompt if insufficient access
            return MacUtilities.nativeFileCopy(source, dest);
        } else {
            // No reliable JNA method; Use pkexec/gksu/etc
            return UnixUtilities.elevatedFileCopy(source, dest);
        }
    }

    public static boolean execute(String... commandArray) {
        return execute(commandArray, false);
    }

    /**
     * Executes a synchronous shell command and returns true if the {@code Process.exitValue()} is {@code 0}.
     *
     * @param commandArray array of command pieces to supply to the shell environment to e executed as a single command
     * @return {@code true} if {@code Process.exitValue()} is {@code 0}, otherwise {@code false}.
     */
    public static boolean execute(String[] commandArray, boolean silent) {
        if (!silent) {
            log.debug("Executing: {}", Arrays.toString(commandArray));
        }
        try {
            // Create and execute our new process
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            // Consume output to prevent deadlock
            while (p.getInputStream().read() != -1) {}
            p.waitFor();
            return p.exitValue() == 0;
        }
        catch(InterruptedException ex) {
            log.warn("InterruptedException waiting for a return value: {} envp: {}", Arrays.toString(commandArray), Arrays.toString(envp), ex);
        }
        catch(IOException ex) {
            log.error("IOException executing: {} envp: {}", Arrays.toString(commandArray), Arrays.toString(envp), ex);
        }

        return false;
    }

    /**
     * Executes a synchronous shell command and return the result.
     *
     * @param commandArray array of shell commands to execute
     * @param searchFor    array of return values to look for, case sensitivity matters
     * @return The first matching string value
     */
    public static String execute(String[] commandArray, String[] searchFor) {
        return execute(commandArray, searchFor, true, false);
    }

    /**
     * Executes a synchronous shell command and return the result.
     *
     * @param commandArray  array of shell commands to execute
     * @param searchFor     array of return values to look for, or {@code null}
     *                      to return the first line of standard output
     * @param caseSensitive whether or not to perform case-sensitive search
     * @return The first matching an element of {@code searchFor}, unless
     * {@code searchFor} is null ,then the first line of standard output
     */
    public static String execute(String[] commandArray, String[] searchFor, boolean caseSensitive, boolean silent) {
        if (!silent) {
            log.debug("Executing: {}", Arrays.toString(commandArray));
        }
        BufferedReader stdInput = null;
        try {
            // Create and execute our new process
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            stdInput = new BufferedReader(new InputStreamReader(p.getInputStream(), Charsets.UTF_8));
            String s;
            while((s = stdInput.readLine()) != null) {
                if (searchFor == null) {
                    return s.trim();
                }
                for(String search : searchFor) {
                    if (caseSensitive) {
                        if (s.contains(search.trim())) {
                            return s.trim();
                        }
                    } else {
                        if (s.toLowerCase(Locale.ENGLISH).contains(search.toLowerCase(Locale.ENGLISH).trim())) {
                            return s.trim();
                        }
                    }
                }
            }
        }
        catch(IOException ex) {
            log.error("IOException executing: {} envp: {}", Arrays.toString(commandArray), Arrays.toString(envp), ex);
        }
        finally {
            if (stdInput != null) {
                try { stdInput.close(); } catch(Exception ignore) {}
            }
        }

        return "";
    }

    public static String executeRaw(String ... commandArray) {
        return executeRaw(commandArray, false);
    }

    /**
     * Executes a synchronous shell command and return the raw character result.
     *
     * @param commandArray array of shell commands to execute
     * @return The entire raw standard output of command
     */
    public static String executeRaw(String[] commandArray, boolean silent) {
        if(!silent) {
            log.debug("Executing: {}", Arrays.toString(commandArray));
        }

        InputStreamReader in = null;
        try {
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            if(SystemUtilities.isWindows() && commandArray.length > 0 && commandArray[0].startsWith("wmic")) {
                // Fix deadlock on old Windows versions https://stackoverflow.com/a/13367685/3196753
                p.getOutputStream().close();
            }
            in = new InputStreamReader(p.getInputStream(), Charsets.UTF_8);
            StringBuilder out = new StringBuilder();
            int c;
            while((c = in.read()) != -1)
                out.append((char)c);

            return out.toString();
        }
        catch(IOException ex) {
            if(!silent) {
                log.error("IOException executing: {} envp: {}", Arrays.toString(commandArray), Arrays.toString(envp), ex);
            }
        }
        finally {
            if (in != null) {
                try { in.close(); } catch(Exception ignore) {}
            }
        }

        return "";
    }

    /**
     * Gets the computer's "hostname" from command line
     *
     * This should only be used as a fallback for when JNA is not available,
     * see <code>SystemUtilities.getHostName()</code> instead.
     */
    static String getHostName() {
        return execute(new String[] {"hostname"}, new String[] {""});
    }

    /**
     * Checks that the currently running OS is Apple and executes a native
     * AppleScript macro against the OS. Returns true if the
     * {@code Process.exitValue()} is {@code 0}.
     *
     * @param scriptBody AppleScript to execute
     * @return true if the {@code Process.exitValue()} is {@code 0}.
     */
    public static boolean executeAppleScript(String scriptBody) {
        if (!SystemUtilities.isMac()) {
            log.error("AppleScript can only be invoked from Apple OS");
            return false;
        }

        return execute("osascript", "-e", scriptBody);
    }

    public static void browseAppDirectory() {
        browseDirectory(SystemUtilities.getJarParentPath());
    }

    public static void browseDirectory(String directory) {
        browseDirectory(new File(directory));
    }

    public static void browseDirectory(Path path) {
        browseDirectory(path.toFile());
    }

    public static void browseDirectory(File directory) {
        try {
            if (!SystemUtilities.isMac()) {
                Desktop.getDesktop().open(directory);
            } else {
                // Mac tries to open the .app rather than browsing it.  Instead, pass a child to select it in finder
                File[] files = directory.listFiles();
                if (files != null && files.length > 0) {
                    try {
                        // Use browseFileDirectory (JDK9+) via reflection
                        Method m = Desktop.class.getDeclaredMethod("browseFileDirectory", File.class);
                        m.invoke(Desktop.getDesktop(), files[0].getCanonicalFile());
                    }
                    catch(ReflectiveOperationException e) {
                        // Fallback to open -R
                        ShellUtilities.execute("open", "-R", files[0].getCanonicalPath());
                    }
                }
            }
        }
        catch(IOException io) {
            if (SystemUtilities.isLinux()) {
                // Fallback on xdg-open for Linux
                ShellUtilities.execute("xdg-open", directory.getPath());
            }
        }
    }
}
