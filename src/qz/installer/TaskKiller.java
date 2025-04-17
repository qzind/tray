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
import qz.common.Constants;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static qz.common.Constants.PROPS_FILE;

public class TaskKiller {
    protected static final Logger log = LogManager.getLogger(TaskKiller.class);
    private static final String[] JAR_NAMES = { PROPS_FILE + ".jar" };
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
            // Fallback to pgrep (See liberica/QZ-23)
            pids.addAll(findPidsPgrep());
        }

        // Kill each PID
        String[] killPid = new String[KILL_PID_CMD.length + 1];
        System.arraycopy(KILL_PID_CMD, 0, killPid, 0, KILL_PID_CMD.length);
        for (Integer pid : pids) {
            killPid[killPid.length - 1] = pid.toString();
            log.debug("Found {} running as PID {}, calling '{}'", Constants.ABOUT_TITLE, pid, Arrays.toString(killPid));
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
            log.error("Could not find find {}", jcmd);
            throw new IOException("Could not find jcmd, we can't use it for detecting running instances");
        }
        return jcmd;
    }

    private static HashSet<Integer> findPidsPgrep() {
        HashSet<Integer> foundPids = new HashSet<>();

        for(String jarName : JAR_NAMES) {
            String[] pids = ShellUtilities.executeRaw("pgrep", "-f", jarName).split("\\s*\\r?\\n");
            for(String pid : pids) {
                if (StringUtils.isNumeric(pid)) {
                    foundPids.add(Integer.parseInt(pid));
                }
            }
        }

        // Careful not to kill ourselves ;)
        foundPids.remove(SystemUtilities.getProcessId());

        return foundPids;
    }


    /**
     * Uses jcmd to fetch all PIDs that match this product
     */
    private static HashSet<Integer> findPidsJcmd() {
        HashSet<Integer> foundPids = new HashSet<>();

        String[] stdout = {};
        try {
            stdout = ShellUtilities.executeRaw(getJcmdPath().toString(), "-l").split("\\r?\\n");
            if(stdout == null || stdout.length == 0) {
               log.error("Error calling '{}' {}", getJcmdPath(), "-l");
            }
        } catch(Exception e) {
            log.error(e);
            return foundPids;
        }

        for(String line : stdout) {
            // e.g. "35446 C:\Program Files\QZ Tray\qz-tray.jar"
            String[] parts = line.split(" ", 1);
            if (parts.length >= 2 && StringUtils.isNumeric(parts[0])) {
                Integer proc = Integer.parseInt(parts[0]);
                String command = parts[1];
                // Handle running from IDE such as IntelliJ
                if(command.contains(PrintSocketServer.class.getCanonicalName())) {
                    foundPids.add(proc);
                    continue;
                }
                // Handle "qz-tray.jar"
                for(String jarName : JAR_NAMES) {
                    if(command.contains(jarName));
                    foundPids.add(proc);
                    break; // continue parent loop
                }
            }
        }

        // Careful not to kill ourselves ;)
        foundPids.remove(SystemUtilities.getProcessId());

        return foundPids;
    }
}
