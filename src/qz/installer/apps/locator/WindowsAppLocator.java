/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.apps.locator;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.apps.locator.AppAlias.Alias;
import qz.utils.WindowsUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;

public class WindowsAppLocator extends AppLocator{
    protected static final Logger log = LogManager.getLogger(WindowsAppLocator.class);

    @Override
    public HashSet<AppInfo> locate(AppAlias appAlias) {
        HashSet<AppInfo> appList = new HashSet<>();
        for (Alias alias : appAlias.aliases) {
            if (alias.getVendor() != null) {
                appList.addAll(locate(alias));
            }
        }
        return appList;
    }

    final String START_MENU_SHELL_TEMPLATE = "SOFTWARE\\Clients\\StartMenuInternet\\%s\\shell\\open\\command";

    /**
     * Locate Chromium-based browsers; may work with other apps as well
     * e.g.
     *    <pre>
     *      [HKEY_LOCAL_MACHINE\SOFTWARE\Clients\StartMenuInternet\Microsoft Edge\shell\open\command]
     *      @ = "C:\Program Files (x86)\Microsoft\\Edge\\Application\msedge.exe"
     *    </pre>
     */
    private HashSet<AppInfo> locate(Alias alias) {
        HashSet<AppInfo> found = new HashSet<>();

        // Chrome can be installed use-wide or system-wide
        WinReg.HKEY[] hives = {WinReg.HKEY_LOCAL_MACHINE, WinReg.HKEY_CURRENT_USER };

        // The relative paths to the Uninstall folders
        String[] basePaths = {
                "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\%s",
                "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\%s"
        };

        for(WinReg.HKEY hive : hives) {
            for(String basePath : basePaths) {
                String appName = alias.getName(false);
                String regPath = String.format(basePath, appName);
                boolean keyExists = WindowsUtilities.registryKeyExists(hive, regPath);
                if(alias.getAppAlias() == AppAlias.FIREFOX) {
                    // Legacy Firefox uses non-standard registry names (e.g. "Mozilla Firefox 68.12.0 ESR (x64 en-US)")
                    String firstMatch = firstKeyStartsWith(hive, String.format(basePath, ""), appName);
                    if(firstMatch != null) {
                        regPath = String.format(basePath, firstMatch);
                        keyExists = true;
                    }
                }
                if(!keyExists) {
                    continue;
                }

                String versionString = WindowsUtilities.getRegString(hive, regPath, "DisplayVersion");
                String installLocation = WindowsUtilities.getRegString(hive, regPath, "InstallLocation");
                Path installPath = WindowsUtilities.cleanRegPath(installLocation);

                // Uninstall doesn't tell us where the executable is, let's check the start menu instead
                String startMenuPath = String.format(START_MENU_SHELL_TEMPLATE, appName);
                String exeLocation = WindowsUtilities.getRegString(hive, startMenuPath, "" /* Default value */);

                // We found an entry, but no exe, let's try to make and educated guess
                if(installPath != null && exeLocation == null) {
                    // first, blindly check for app.exe
                    Path exeGuess = installPath.resolve(alias.getSlug() + ".exe");
                    if(exeGuess.toFile().exists()) {
                        exeLocation = exeGuess.toString();
                    } else {
                        // next, fallback on the icon path
                        String displayIcon = WindowsUtilities.getRegString(hive, regPath, "DisplayIcon");
                        if(displayIcon != null && displayIcon.contains(".exe")) {
                            exeLocation = WindowsUtilities.cleanRegPath(displayIcon).toString();
                        }
                    }
                }

                if(versionString == null || versionString.isBlank()) continue;

                if(installPath != null && exeLocation != null) {
                    Path exePath = WindowsUtilities.cleanRegPath(exeLocation);

                    if(exePath.toAbsolutePath().toString().toLowerCase(Locale.ENGLISH)
                            .contains(installPath.toAbsolutePath().toString().toLowerCase(Locale.ENGLISH))) {
                        // We have a match
                        found.add(new AppInfo(alias, installPath, exePath, versionString));
                    }
                }

            }
        }
        return found;
    }


    private String firstKeyStartsWith(WinReg.HKEY hive, String basePath, String appName) {
        String[] keys = WindowsUtilities.getRegistryKeys(hive, basePath);
        if(keys == null) {
            return null;
        }

        for(String key : keys) {
            //String keyName = key.substring(key.lastIndexOf("\\"));
            if(key.startsWith("{")) {
                // skip {00000000-0000-0000-0000-000000000000}
                continue;
            }
            if(key.startsWith(appName)) {
                log.info("Matched '{}' at '{}'", appName, key);
                return key;
            }
        }

        return null;
    }

    @Override
    public HashSet<String> getPids(HashSet<String> processNames) {
        HashSet<String> pidList = new HashSet<>();

        if (processNames.isEmpty()) return pidList;

        Tlhelp32.PROCESSENTRY32 pe32 = new Tlhelp32.PROCESSENTRY32();
        pe32.dwSize = new WinNT.DWORD(pe32.size());

        // Fetch a snapshot of all processes
        WinNT.HANDLE hSnapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinNT.DWORD(0));
        if (hSnapshot.equals(WinNT.INVALID_HANDLE_VALUE)) {
            log.warn("Process snapshot has invalid handle");
            return pidList;
        }

        if (Kernel32.INSTANCE.Process32First(hSnapshot, pe32)) {
            do {
                String processName = Native.toString(pe32.szExeFile);
                if(processNames.contains(processName.toLowerCase(Locale.ENGLISH))) {
                    pidList.add(pe32.th32ProcessID.toString());
                }
            } while (Kernel32.INSTANCE.Process32Next(hSnapshot, pe32));
        }

        Kernel32.INSTANCE.CloseHandle(hSnapshot);
        return pidList;
    }

    @Override
    public HashSet<Path> getPidPaths(HashSet<String> pids) {
        HashSet<Path> pathList = new HashSet<>();

        for(String pid : pids) {
            WinNT.HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, false, Integer.parseInt(pid));
            if (hProcess == null) {
                log.warn("Handle for PID {} is missing, skipping.", pid);
                continue;
            }

            int bufferSize = WinNT.MAX_PATH;
            Pointer buffer = new Memory(bufferSize * Native.WCHAR_SIZE);

            if (Psapi.INSTANCE.GetModuleFileNameEx(hProcess, null, buffer, bufferSize) == 0) {
                log.warn("Full path to PID {} is empty, skipping.", pid);
                Kernel32.INSTANCE.CloseHandle(hProcess);
                continue;
            }

            Kernel32.INSTANCE.CloseHandle(hProcess);
            pathList.add(Paths.get(Native.WCHAR_SIZE == 1 ?
                                        buffer.getString(0) :
                                        buffer.getWideString(0)));
        }
        return pathList;
    }
}
