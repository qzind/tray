package qz.installer;

import com.sun.jna.platform.win32.Kernel32;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.MacUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static qz.common.Constants.PROPS_FILE;

public class TaskKiller {
    protected static final Logger log = LoggerFactory.getLogger(TaskKiller.class);
    private static final String[] TRAY_PID_QUERY_POSIX = {"pgrep", "-f", PROPS_FILE + ".jar" };
    private static final String[] KILL_PID_CMD_POSIX = {"kill", "-9", ""/*pid placeholder*/};

    private static final String[] TRAY_PID_QUERY_WIN32 = {"wmic.exe", "process", "where", "CommandLine like '%" + PROPS_FILE + ".jar" + "%'", "get", "processid" };
    private static final String[] KILL_PID_CMD_WIN32 = {"taskkill.exe", "/F", "/PID", "" /*pid placeholder*/ };

    /**
     * Kills all QZ Tray processes, being careful not to kill itself
     */
    public static boolean killAll() {
        boolean success = true;

        ArrayList<String> javaProcs;
        String[] trayProcs;
        int selfProc;
        String[] killCmd;
        // Disable service until reboot
        if(SystemUtilities.isMac()) {
            ShellUtilities.execute("launchctl", "unload", MacInstaller.LAUNCH_AGENT_PATH);
        }
        if(SystemUtilities.isWindows()) {
            // Windows may be running under javaw.exe (normal) or java.exe (terminal)
            javaProcs = AppLocator.getInstance().getPids("java.exe", "javaw.exe");
            trayProcs = ShellUtilities.executeRaw(TRAY_PID_QUERY_WIN32).split("\\s*\\r?\\n");
            selfProc = Kernel32.INSTANCE.GetCurrentProcessId();
            killCmd = KILL_PID_CMD_WIN32;
        } else {
            javaProcs = AppLocator.getInstance().getPids( "java");
            trayProcs = ShellUtilities.executeRaw(TRAY_PID_QUERY_POSIX).split("\\s*\\r?\\n");
            selfProc = MacUtilities.getProcessID(); // Works for Linux too
            killCmd = KILL_PID_CMD_POSIX;
        }
        if (!javaProcs.isEmpty()) {
            // Find intersections of java and qz-tray.jar
            List<String> intersections = new ArrayList<>(Arrays.asList(trayProcs));
            intersections.retainAll(javaProcs);

            // Remove any instances created by this installer
            intersections.remove("" + selfProc);

            // Kill whatever's left
            for (String pid : intersections) {
                // isNumeric() needed for Windows; filters whitespace, headers
                if(StringUtils.isNumeric(pid)) {
                    // Set last command to the pid
                    killCmd[killCmd.length -1] = pid;
                    success = success && ShellUtilities.execute(killCmd);
                }
            }
        }

        // Use jcmd to kill class processes too, such as through the IDE
        if(SystemUtilities.isJDK()) {
            String[] procs = ShellUtilities.executeRaw("jcmd", "-l").split("\\r?\\n");
            for(String proc : procs) {
                String[] parts = proc.split(" ", 1);
                if (parts.length >= 2 && parts[1].contains(PrintSocketServer.class.getCanonicalName())) {
                    killCmd[killCmd.length - 1] = parts[0].trim();
                    success = success && ShellUtilities.execute(killCmd);
                }
            }
        }

        if(SystemUtilities.isWindowsXP()) {
            File f = new File("TempWmicBatchFile.bat");
            if(f.exists()) {
                f.deleteOnExit();
            }
        }

        return success;
    }
}
