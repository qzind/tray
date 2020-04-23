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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;
import qz.utils.WindowsUtilities;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsAppLocator extends AppLocator{
    protected static final Logger log = LoggerFactory.getLogger(MacAppLocator.class);

    private static final String[] WIN32_PID_QUERY = {"wmic.exe", "process", "where", null, "get", "parentprocessid,", "processid"};
    private static final int WIN32_PID_QUERY_INPUT_INDEX = 3;

    private static final String[] WIN32_PATH_QUERY = {"wmic.exe", "process", "where", null, "get", "ExecutablePath"};
    private static final int WIN32_PATH_QUERY_INPUT_INDEX = 3;

    private static String REG_TEMPLATE = "Software\\%s%s\\%s%s";

    @Override
    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        for (AppAlias.Alias alias : appAlias.aliases) {
            if (alias.vendor != null) {
                String[] suffixes = new String[]{ "", " ESR"};
                String[] prefixes = new String[]{ "", "WOW6432Node\\"};
                for (String suffix : suffixes) {
                    for (String prefix : prefixes) {
                        String key = String.format(REG_TEMPLATE, prefix, alias.vendor, alias.name, suffix);
                        AppInfo appInfo = getAppInfo(alias.name, key, suffix);
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
    public ArrayList<String> getPids(boolean exactMatch, ArrayList<String> processNames) {
        String[] response;
        ArrayList<String> pidList = new ArrayList<>();

        ArrayList<String> parentPIDList = new ArrayList<>();

        String matchPrefix = exactMatch ? "Name='" : "Name like '/%";
        String matchSufix = exactMatch ? "'" : "/%'";

        if (processNames.isEmpty()) return pidList;

        String matchString = "(" + matchPrefix;
        matchString += String.join(matchSufix + " OR " + matchPrefix, processNames);
        matchString += matchSufix + ")";

        WIN32_PID_QUERY[WIN32_PID_QUERY_INPUT_INDEX] = matchString;
        response = ShellUtilities.executeRaw(WIN32_PID_QUERY).split("[\\r\\n]+");

        // Skip the first result (the first row is column headers)
        for (int i = 1; i < response.length; i++) {
            String[] row =  response[i].split("[\\s,]+");

            parentPIDList.add(row[0]);
            pidList.add(row[1]);
        }

        // Remove all processes that are child to another process in this set
        for (int i = pidList.size() - 1; i >= 0; i--){
            if (pidList.contains(parentPIDList.get(i))) {
                pidList.remove(i);
                parentPIDList.remove(i);
            }
        }
        return pidList;
    }

    @Override
    public ArrayList<Path> locateProcessPaths(boolean exactMatch, ArrayList<String> pids) {
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

    private static AppInfo getAppInfo(String name, String key, String suffix) {
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
                String path = new File(exePath).getParent();
                version = version.replaceAll(" ", "-");
                return new AppInfo(name, path, exePath, version);
            }
        }
        return null;
    }
}
