/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.certificate.firefox.locator;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.certificate.firefox.locator.AppAlias.Alias;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsAppLocator extends AppLocator{
    protected static final Logger log = LogManager.getLogger(MacAppLocator.class);

    private static final String[] WIN32_PID_QUERY = {"wmic.exe", "process", "where", null, "get", "processid"};
    private static final int WIN32_PID_QUERY_INPUT_INDEX = 3;

    private static final String[] WIN32_PATH_QUERY = {"wmic.exe", "process", "where", null, "get", "ExecutablePath"};
    private static final int WIN32_PATH_QUERY_INPUT_INDEX = 3;

    private static String REG_TEMPLATE = "Software\\%s%s\\%s%s";

    @Override
    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        for (Alias alias : appAlias.aliases) {
            if (alias.getVendor() != null) {
                String[] suffixes = new String[]{ "", " ESR"};
                String[] prefixes = new String[]{ "", "WOW6432Node\\"};
                for (String suffix : suffixes) {
                    for (String prefix : prefixes) {
                        String key = String.format(REG_TEMPLATE, prefix, alias.getVendor(), alias.getName(), suffix);
                        AppInfo appInfo = getAppInfo(alias, key, suffix);
                        if (appInfo != null && !appList.contains(appInfo)) {
                            appList.add(appInfo);
                        }
                    }
                }
            }
        }
        return appList;
    }

    @Override
    public ArrayList<String> getPids(ArrayList<String> processNames) {
        ArrayList<String> pidList = new ArrayList<>();

        if (processNames.isEmpty()) return pidList;

        WIN32_PID_QUERY[WIN32_PID_QUERY_INPUT_INDEX] = "(Name='" + String.join("' OR Name='", processNames) + "')";
        String[] response = ShellUtilities.executeRaw(WIN32_PID_QUERY).split("[\\r\\n]+");

        // Add all found pids
        for(String line : response) {
            String pid = line.trim();
            if(StringUtils.isNumeric(pid.trim())) {
                pidList.add(pid);
            }
        }

        if(WindowsUtilities.isWindowsXP()) {
            // Cleanup XP crumbs per https://stackoverflow.com/q/12391655/3196753
            File f = new File("TempWmicBatchFile.bat");
            if(f.exists()) {
                f.deleteOnExit();
            }
        }

        return pidList;
    }

    @Override
    public ArrayList<Path> getPidPaths(ArrayList<String> pids) {
        ArrayList<Path> pathList = new ArrayList<>();

        for(String pid : pids) {
            WIN32_PATH_QUERY[WIN32_PATH_QUERY_INPUT_INDEX] = "ProcessId=" + pid;
            String[] response = ShellUtilities.executeRaw(WIN32_PATH_QUERY).split("\\s*\\r?\\n");
            if (response.length > 1) {
                try {
                    pathList.add(Paths.get(response[1]).toRealPath());
                } catch(IOException e) {
                    log.warn("Could not locate process " + pid);
                }
            }
        }
        return pathList;
    }

    /**
     * Use a proprietary Firefox-only technique for getting "PathToExe" registry value
     */
    private static AppInfo getAppInfo(Alias alias, String key, String suffix) {
        String version = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key, "CurrentVersion");
        if (version != null) {
            version = version.split(" ")[0]; // chop off (x86 ...)
            if (!suffix.isEmpty()) {
                if (key.endsWith(suffix)) {
                    key = key.substring(0, key.length() - suffix.length());
                }
                version = version + suffix;
            }
            String exePath = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key + " " + version + "\\bin", "PathToExe");

            if (exePath != null) {
                // SemVer: Replace spaces in suffixes with dashes
                version = version.replaceAll(" ", "-");
                return new AppInfo(alias, Paths.get(exePath), version);
            } else {
                log.warn("Couldn't locate \"PathToExe\" for \"{}\" in \"{}\", skipping", alias.getName(), key);
            }
        }
        return null;
    }
}
