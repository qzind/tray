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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinNT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.certificate.firefox.locator.AppAlias.Alias;
import qz.utils.WindowsUtilities;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsAppLocator extends AppLocator{
    protected static final Logger log = LogManager.getLogger(MacAppLocator.class);

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
        ArrayList<String> javaPids = new ArrayList<>();
        Tlhelp32.PROCESSENTRY32 pe32 = new Tlhelp32.PROCESSENTRY32();
        pe32.dwSize = new WinNT.DWORD(pe32.size());

        // Fetch a snapshot of all processes
        WinNT.HANDLE hSnapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinNT.DWORD(0));
        if (hSnapshot.equals(WinNT.INVALID_HANDLE_VALUE)) {
            log.warn("Process snapshot has invalid handle");
            return javaPids;
        }

        if (Kernel32.INSTANCE.Process32First(hSnapshot, pe32)) {
            do {
                String processName = Native.toString(pe32.szExeFile);
                if(processNames.contains(processName.toLowerCase(Locale.ENGLISH))) {
                    javaPids.add(pe32.th32ProcessID.toString());
                }
            } while (Kernel32.INSTANCE.Process32Next(hSnapshot, pe32));
        }

        Kernel32.INSTANCE.CloseHandle(hSnapshot);
        return javaPids;
    }


    @Override
    public ArrayList<Path> getPidPaths(ArrayList<String> pids) {
        ArrayList<Path> paths = new ArrayList<>();

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
            paths.add(Paths.get(Native.WCHAR_SIZE == 1 ?
                                        buffer.getString(0) :
                                        buffer.getWideString(0)));
        }
        return paths;
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
