/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2021 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.installer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

import static qz.common.Constants.PROPS_FILE;

public class TaskKiller {
    protected static final Logger log = LogManager.getLogger(TaskKiller.class);
    private static final String[] JAR_NAMES = {
            PROPS_FILE + ".jar",
            "qz.App", // v2.2.0...
            "qz.ws.PrintSocketServer" // v2.0.0...v2.1.6
    };
    private static final String[] KILL_PID_CMD_POSIX = { "kill", "-9" };
    private static final String[] KILL_PID_CMD_WIN32 = { "taskkill.exe", "/F", "/PID" };
    private static final String[] KILL_PID_CMD = SystemUtilities.isWindows() ? KILL_PID_CMD_WIN32 : KILL_PID_CMD_POSIX;

    /**
     * Kills all QZ Tray processes, being careful not to kill itself
     */
    public static boolean killAll() {
        boolean success = true;

        // Disable service until reboot
        if(SystemUtilities.isMac()) {
            ShellUtilities.execute("/bin/launchctl", "unload", MacInstaller.LAUNCH_AGENT_PATH);
        }

        // Use jcmd to get all java processes
        HashSet<Integer> pids = findPidsJcmd();
        if(!SystemUtilities.isWindows()) {
            // Fallback to pgrep, needed for macOS (See JDK-8319589, JDK-8197387)
            pids.addAll(findPidsPgrep());
        } else if(WindowsUtilities.isSystemAccount()) {
            // Fallback to powershell, needed for Windows
            pids.addAll(findPidsPwsh());
        }

        // Careful not to kill ourselves ;)
        pids.remove(SystemUtilities.getProcessId());

        // Kill each PID
        String[] killPid = new String[KILL_PID_CMD.length + 1];
        System.arraycopy(KILL_PID_CMD, 0, killPid, 0, KILL_PID_CMD.length);
        for (Integer pid : pids) {
            killPid[killPid.length - 1] = pid.toString();
            success = success && ShellUtilities.execute(killPid);
        }

        return success;
    }

    private static Path getJcmdPath() throws IOException {
        Path jcmd;
        if(SystemUtilities.isWindows()) {
            jcmd = SystemUtilities.getJarParentPath().resolve("runtime/bin/jcmd.exe");
        } else if (SystemUtilities.isMac()) {
            jcmd = SystemUtilities.getJarParentPath().resolve("../PlugIns/Java.runtime/Contents/Home/bin/jcmd");
        } else {
            jcmd = SystemUtilities.getJarParentPath().resolve("runtime/bin/jcmd");
        }
        if(!jcmd.toFile().exists()) {
            log.error("Could not find {}", jcmd);
            throw new IOException("Could not find jcmd, we can't use it for detecting running instances");
        }
        return jcmd;
    }


    static final String[] PWSH_QUERY = { "powershell.exe", "-Command", "\"(Get-CimInstance Win32_Process -Filter \\\"Name = 'java.exe' OR Name = 'javaw.exe'\\\").Where({$_.CommandLine -like '*%s*'}).ProcessId\"" };

    /**
     * Leverage powershell.exe when run as SYSTEM to workaround https://github.com/qzind/tray/issues/1360
     * TODO: Remove when jcmd is patched to work as SYSTEM account
     */
    private static HashSet<Integer> findPidsPwsh() {
        HashSet<Integer> foundPids = new HashSet<>();

        for(String jarName : JAR_NAMES) {
            String[] pwshQuery = PWSH_QUERY.clone();
            int lastIndex = pwshQuery.length - 1;
            // Format the last element to contain the jarName
            pwshQuery[lastIndex] = String.format(pwshQuery[lastIndex], jarName);
            String stdout = ShellUtilities.executeRaw(pwshQuery);
            String[] lines = stdout.split("\\s*\\r?\\n");
            for(String line : lines) {
                if(line.trim().isEmpty()) {
                    // Don't try to process blank lines
                    continue;
                }

                int pid = parsePid(line);
                if (pid >= 0) {
                    foundPids.add(pid);
                } else {
                    log.warn("Could not parse PID value.  Full line: '{}', Full output: '{}'", line, stdout);
                }
            }
        }

        return foundPids;
    }

    /**
     * Use pgrep to fetch all PIDs to workaround https://github.com/openjdk/jdk/pull/25824
     * TODO: Remove when jcmd is patched to work properly on macOS
     */
    private static HashSet<Integer> findPidsPgrep() {
        HashSet<Integer> foundPids = new HashSet<>();

        for(String jarName : JAR_NAMES) {
            String stdout = ShellUtilities.executeRaw("pgrep", "-f", jarName);
            String[] lines = stdout.split("\\s*\\r?\\n");
            for(String line : lines) {
                if(line.trim().isEmpty()) {
                    // Don't try to process blank lines
                    continue;
                }

                int pid = parsePid(line);
                if (pid >= 0) {
                    foundPids.add(pid);
                } else {
                    log.warn("Could not parse PID value.  Full line: '{}', Full output: '{}'", line, stdout);
                }
            }
        }

        return foundPids;
    }


    /**
     * Uses jcmd to fetch all PIDs that match this product
     */
    private static HashSet<Integer> findPidsJcmd() {
        HashSet<Integer> foundPids = new HashSet<>();

        String stdout;
        String[] lines;
        try {
            stdout = ShellUtilities.executeRaw(getJcmdPath().toString(), "-l");
            if(stdout == null) {
               log.error("Error calling '{}' {}", getJcmdPath(), "-l");
               return foundPids;
            }
            lines = stdout.split("\\r?\\n");
        } catch(Exception e) {
            log.error(e);
            return foundPids;
        }

        for(String line : lines) {
            if (line.trim().isEmpty()) {
                // Don't try to process blank lines
                continue;
            }
            // e.g. "35446 C:\Program Files\QZ Tray\qz-tray.jar"
            String[] parts = line.split(" ", 2);
            int pid = parsePid(parts);
            if (pid >= 0) {
                String args = parseArgs(parts);
                if (args == null) {
                    log.warn("Found PID value '{}' but no args to match.  Full line: '{}', Full output: '{}'", pid, line, stdout);
                    continue;
                }
                for(String jarName : JAR_NAMES) {
                    if (args.contains(jarName)) {
                        foundPids.add(pid);
                        break; // continue parent loop
                    }
                }
            } else {
                log.warn("Could not parse PID value.  Full line: '{}', Full output: '{}'", line, stdout);
            }
        }

        return foundPids;
    }

    // Returns the second index of a String[], trimmed
    private static String parseArgs(String[] input) {
        if(input != null) {
            if(input.length == 2) {
                return input[1].trim();
            }
        }
        return null;
    }

    // Parses an int value form the first index of a String[], returning -1 if something went wrong
    private static int parsePid(String[] input) {
        if(input != null) {
            if(input.length == 2) {
                return parsePid(input[0]);
            }
        }
        return -1;
    }

    // Parses an int value form the provided string, returning -1 if something went wrong
    private static int parsePid(String input) {
        String pidString = input.trim();
        if(StringUtils.isNumeric(pidString)) {
            return Integer.parseInt(pidString);
        }
        return -1;
    }
}
