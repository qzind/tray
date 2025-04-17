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
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        // Get the matching PIDs
        ArrayList<Integer> pids;
        try {
            pids = findTrayPids();
        } catch(IOException e) {
            log.error("Failed to retrieve PIDs for {}", Constants.ABOUT_TITLE, e);
            return false;
        }

        // Kill each PID
        String[] killPid = new String[KILL_PID_CMD.length + 1];
        for (Integer pid : pids) {
            killPid[killPid.length - 1] = pid.toString();
            success = success && ShellUtilities.execute(killPid);
        }

        return success;
    }

    private static Path getJcmdPath() throws IOException {
        Path jcmd;
        if(SystemUtilities.isWindows()) {
            jcmd = SystemUtilities.getJarParentPath().resolve("/runtime/bin/jcmd.exe");
        } else if (SystemUtilities.isMac()) {
            jcmd = SystemUtilities.getJarParentPath().resolve("../PlugIns/Java.runtime/Contents/Home/bin/jcmd");
        } else {
            jcmd = SystemUtilities.getJarParentPath().resolve("../PlugIns/Java.runtime/Contents/Home/bin/jcmd");
        }
        if(!jcmd.toFile().exists()) {
            throw new IOException("Could not find jcmd, we can't stop running instances");
        }
        return jcmd;
    }


    /**
     * Uses jcmd to fetch all PIDs that match this product
     */
    private static ArrayList<Integer> findTrayPids() throws IOException {
        ArrayList<Integer> foundProcs = new ArrayList<>();

        String[] stdout = ShellUtilities.executeRaw(getJcmdPath().toString(), "-l").split("\\r?\\n");
        if(stdout == null || stdout.length == 0) {
            throw new IOException("Error calling '" + getJcmdPath() + "' -l");
        }
        for(String line : stdout) {
            // e.g. "35446 C:\Program Files\QZ Tray\qz-tray.jar"
            String[] parts = line.split(" ", 1);
            if (parts.length >= 2) {
                Integer proc = Integer.parseInt(parts[0]);
                String command = parts[1];
                // Handle running from IDE such as IntelliJ
                if(command.contains(PrintSocketServer.class.getCanonicalName())) {
                    foundProcs.add(proc);
                    continue;
                }
                // Handle "qz-tray.jar"
                for(String jarName : JAR_NAMES) {
                    if(command.contains(jarName));
                    foundProcs.add(proc);
                    break; // continue parent loop
                }
            }
        }

        // Careful not to kill ourselves ;)
        foundProcs.remove(SystemUtilities.getProcessId());

        return foundProcs;
    }
}
