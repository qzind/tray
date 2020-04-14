package qz.installer;

import com.sun.jna.platform.win32.Kernel32;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.MacUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static qz.common.Constants.ABOUT_TITLE;
import static qz.common.Constants.PROPS_FILE;
import static qz.installer.Installer.InstallType.PREINSTALL;

public class TaskControl {
    protected static final Logger log = LoggerFactory.getLogger(TaskControl.class);
    private static final String[] JAVA_PID_QUERY_POSIX = {"pgrep", "java" };
    private static final String[] TRAY_PID_QUERY_POSIX = {"pgrep", "-f", PROPS_FILE + ".jar" };
    private static final String[] KILL_PID_CMD_POSIX = {"kill", "-9", ""/*pid placeholder*/};

    private static final String[] POSIX_PID_QUERY = {"ps", "-C", null, "-o", "comm", "pid"};
    private static final int POSIX_PID_QUERY_INPUT_INDEX = 2;

    private static final String[] WIN32_PID_QUERY = {"wmic.exe", "process", "where", null, "get", "processid,", "parentprocessid" };
    private static final int WIN32_PID_QUERY_INPUT_INDEX = 3;

    private static final String[] WIN32_PATH_QUERY = {"wmic.exe", "process", "where", null, "get", "ExecutablePath"};
    private static final int WIN32_PATH_QUERY_INPUT_INDEX = 3;

    private static final String[] JAVA_PID_QUERY_WIN32 = {"wmic.exe", "process", "where", "Name like '%java%'", "get", "processid" };
    private static final String[] TRAY_PID_QUERY_WIN32 = {"wmic.exe", "process", "where", "CommandLine like '%" + PROPS_FILE + ".jar" + "%'", "get", "processid" };
    private static final String[] KILL_PID_CMD_WIN32 = {"taskkill.exe", "/F", "/PID", "" /*pid placeholder*/ };

    /**
     * Kills all QZ Tray processes, being careful not to kill itself
     */
    public static boolean killAll() {
        boolean success = true;

        String[] javaProcs;
        String[] trayProcs;
        int selfProc;
        String[] killCmd;
        if(SystemUtilities.isWindows()) {
            javaProcs = ShellUtilities.executeRaw(JAVA_PID_QUERY_WIN32).split("\\s*\\r?\\n");
            trayProcs = ShellUtilities.executeRaw(TRAY_PID_QUERY_WIN32).split("\\s*\\r?\\n");
            selfProc = Kernel32.INSTANCE.GetCurrentProcessId();
            killCmd = KILL_PID_CMD_WIN32;
        } else {
            javaProcs = ShellUtilities.executeRaw(JAVA_PID_QUERY_POSIX).split("\\s*\\r?\\n");
            trayProcs = ShellUtilities.executeRaw(TRAY_PID_QUERY_POSIX).split("\\s*\\r?\\n");
            selfProc = MacUtilities.getProcessID(); // Works for Linux too
            killCmd = KILL_PID_CMD_POSIX;
        }
        if (javaProcs.length > 0) {
            // Find intersections of java and qz-tray.jar
            List<String> intersections = new ArrayList<>(Arrays.asList(trayProcs));
            intersections.retainAll(Arrays.asList(javaProcs));

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

    public static String[] locateProcessPath(String ProcessName, boolean exactMatch) {
        String[] response = null;
        String[] PIDArray = getRootPID(ProcessName,exactMatch);
        //todo foreach this and pass a proper array
        if (SystemUtilities.isWindows()) {
            WIN32_PATH_QUERY[WIN32_PATH_QUERY_INPUT_INDEX] = "ProcessId=" + PIDArray[0];
            response = ShellUtilities.executeRaw(WIN32_PATH_QUERY).split("\\s*\\r?\\n");
            if (response.length < 2) return ArrayUtils.EMPTY_STRING_ARRAY;
            response = Arrays.copyOfRange(response, 1, response.length);
        } else {
            try {
                response = new String[] {Paths.get("/proc/", PIDArray[0], "/exe").toRealPath().toString()};
            }
            catch(IOException e) {
                //todo handle this
                log.error(e.getMessage());
            }
        }
        return new String[] {response[0]};
    }

    public static String[] getRootPID(String ProcessName, boolean exactMatch) {
        String[] response = null;
        if (SystemUtilities.isWindows()) {
            String matchString;
            if (exactMatch) {
                matchString = "Name='" + ProcessName + "'";
            } else {
                matchString = "Name like '%" + ProcessName + "%'";
            }
            WIN32_PID_QUERY[WIN32_PID_QUERY_INPUT_INDEX] = matchString;
            response = ShellUtilities.executeRaw(WIN32_PID_QUERY).split("\\s*(,|\\s)\\s*");
        } else {
            POSIX_PID_QUERY[POSIX_PID_QUERY_INPUT_INDEX] = ProcessName;
            response = ShellUtilities.executeRaw(POSIX_PID_QUERY).split("\\s*(,|\\s)\\s*");
        }

        ArrayList<String> PIDList = new ArrayList<>();
        ArrayList<String> ParentPIDList = new ArrayList<>();
        for (int i = 2; i < response.length; i += 2) {
            ParentPIDList.add(response[i]);
            PIDList.add(response[i + 1]);
        }

        // Remove all PIDs that have a parent of the same name
        for (int i = PIDList.size() - 1; i >= 0; i--) {
            if (PIDList.contains(ParentPIDList.get(i))) {
                ParentPIDList.remove(i);
                PIDList.remove(i);
            }
        }

        return PIDList.toArray(String[]::new);
    }
}
