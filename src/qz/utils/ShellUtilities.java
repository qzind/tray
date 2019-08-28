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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.PrinterResolution;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for managing all {@code Runtime.exec(...)} functions.
 *
 * @author Tres Finocchiaro
 */
public class ShellUtilities {

    private static final Logger log = LoggerFactory.getLogger(ShellUtilities.class);

    // Shell environment overrides.  null = don't override
    public static String[] envp = null;

    // Make sure all shell calls are LANG=en_US.UTF-8
    static {
        if (!SystemUtilities.isWindows()) {
            // Cache existing; permit named overrides w/o full clobber
            Map<String, String> env = new HashMap<>(System.getenv());
            if (SystemUtilities.isMac()) {
                // Enable LANG overrides
                env.put("SOFTWARE", "");
            }
            // Functional equivalent of "export LANG=en_US.UTF-8"
            env.put("LANG", "C");
            String[] envp = new String[env.size()];
            int i = 0;
            for (Map.Entry<String, String> o : env.entrySet())
                envp[i++] = o.getKey() + "=" + o.getValue();

            ShellUtilities.envp = envp;
        }
    }

    public static boolean execute(String[] commandArray) {
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
        if(!silent) {
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
                        if (s.toLowerCase().contains(search.toLowerCase().trim())) {
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

    /**
     * Executes a synchronous shell command and return the raw character result.
     *
     * @param commandArray  array of shell commands to execute
     * @return The entire raw standard output of command
     */
    public static String executeRaw(String[] commandArray) {
        log.debug("Executing: {}", Arrays.toString(commandArray));
        InputStreamReader in = null;
        try {
            Process p = Runtime.getRuntime().exec(commandArray, envp);
            in = new InputStreamReader(p.getInputStream(), Charsets.UTF_8);
            StringBuilder out = new StringBuilder();
            int c;
            while((c=in.read()) != -1)
                out.append((char)c);

            return out.toString();
        } catch(IOException ex) {
            log.error("IOException executing: {} envp: {}", Arrays.toString(commandArray), Arrays.toString(envp), ex);
        } finally {
            if (in != null) {
                try { in.close(); } catch(Exception ignore) {}
            }
        }

        return "";
    }

    /**
     * Gets the computer's "hostname" from command line
     */
    public static String getHostName() {
        return execute(new String[] {"hostname"}, new String[]{""});
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

        return execute(new String[] {"osascript", "-e", scriptBody});
    }

    /**
     * Checks that the currently running OS is Apple and executes a native
     * AppleScript macro against the OS. Returns true if the
     * supplied searchValues are found within the standard output.
     *
     * @param scriptBody   AppleScript text to execute
     * @param searchValues List of stdout strings to search for
     * @return true if the supplied searchValues are found within the standard output.
     */
    public static boolean executeAppleScript(String scriptBody, String ... searchValues) {
        if (!SystemUtilities.isMac()) {
            log.error("AppleScript can only be invoked from Apple OS");
            return false;
        }

        // Empty string returned by execute(...) means the values weren't found
        return !execute(new String[] {"osascript", "-e", scriptBody},
                        searchValues).isEmpty();
    }

    public static boolean setRegistryDWORD(String keyPath, String name, int data) {
        if (!SystemUtilities.isWindows()) {
            log.error("Reg commands can only be invoked from Windows");
            return false;
        }

        String reg = System.getenv("windir") + "\\system32\\reg.exe";
        return execute(
                new String[] {
                        reg, "add", keyPath, "/f", "/v", name, "/t", "REG_DWORD", "/d", "" + data
                }
        );
    }

    public static String getRegistryString(String keyPath, String name) {
        String match = "REG_SZ";
        if (!SystemUtilities.isWindows()) {
            log.error("Reg commands can only be invoked from Windows");
            return null;
        }

        String reg = System.getenv("windir") + "\\system32\\reg.exe";
        String stdout = execute(
                new String[] {
                        reg, "query", keyPath, "/v", name
                },
                new String[] {match}
        );

        if (!stdout.isEmpty()) {
            // Return the last element
            String[] parts = stdout.split("\\s+");
            return parts[parts.length - 1];
        }

        return null;
    }

    public static int getRegistryDWORD(String keyPath, String name) {
        return getRegistryDWORD(keyPath, name, false);
    }

    public static int getRegistryDWORD(String keyPath, String name, boolean silent) {
        String match = "0x";
        if (!SystemUtilities.isWindows()) {
            log.error("Reg commands can only be invoked from Windows");
            return -1;
        }

        String reg = System.getenv("windir") + "\\system32\\reg.exe";
        String stdout = execute(
                new String[] {
                        reg, "query", keyPath, "/v", name
                },
                new String[] {match},
                true,
                silent
        );

        // Parse stdout looking for hex (i.e. "0x1B")
        if (!Objects.equals(stdout, "")) {
            for(String part : stdout.split(" ")) {
                if (part.startsWith(match)) {
                    try {
                        return Integer.parseInt(part.trim().split(match)[1], 16);
                    }
                    catch(NumberFormatException ignore) {}
                }
            }
        }

        return -1;
    }

    /**
     * Opens the specified path in the system-default file browser.  Works around several OS limitations:
     *  - Apple tries to launch <code>.app</code> bundle directories as applications rather than browsing contents
     *  - Linux has mixed support for <code>Desktop.getDesktop()</code>.  Adds <code>xdg-open</code> fallback.
     * @param path The directory to browse
     * @throws IOException
     */
    public static void browseDirectory(String path) throws IOException {
        File directory = new File(path);
        if (SystemUtilities.isMac()) {
            // Mac tries to open the .app rather than browsing it.  Instead, pass a child with -R to select it in finder
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                // Get first child
                File child = files[0];
                if (ShellUtilities.execute(new String[] {"open", "-R", child.getCanonicalPath()})) {
                    return;
                }
            }
        } else {
            try {
                // The default, java recommended usage
                Desktop d = Desktop.getDesktop();
                d.open(directory);
                return;
            } catch (IOException io) {
                if (SystemUtilities.isLinux()) {
                    // Fallback on xdg-open for Linux
                    if (ShellUtilities.execute(new String[] {"xdg-open", path})) {
                        return;
                    }
                }
                throw io;
            }
        }
        throw new IOException("Unable to open " + path);
    }

    /**
     * Executes a native Registry delete/query command against the OS
     *
     * @param keyPath  The path to the containing registry key
     * @param function "delete", or "query"
     * @param name     the registry name to add, delete or query
     * @return true if the return code is zero
     */
    public static boolean executeRegScript(String keyPath, String function, String name) {
        return executeRegScript(keyPath, function, name, null);
    }

    /**
     * Executes a native Registry add/delete/query command against the OS
     *
     * @param keyPath  The path to the containing registry key
     * @param function "add", "delete", or "query"
     * @param name     the registry name to add, delete or query
     * @param data     the registry data to add when using the "add" function
     * @return true if the return code is zero
     */
    public static boolean executeRegScript(String keyPath, String function, String name, String data) {
        if (!SystemUtilities.isWindows()) {
            log.error("Reg commands can only be invoked from Windows");
            return false;
        }

        String reg = System.getenv("windir") + "\\system32\\reg.exe";
        if ("delete".equals(function)) {
            return execute(new String[] {
                    reg, function, keyPath, "/v", name, "/f"
            });
        } else if ("add".equals(function)) {
            return execute(new String[] {
                    reg, function, keyPath, "/v", name, "/d", data, "/f"
            });
        } else if ("query".equals(function)) {
            return execute(new String[] {
                    reg, function, keyPath, "/v", name
            });
        } else {
            log.error("Reg operation {} not supported.", function);
            return false;
        }
    }
}
