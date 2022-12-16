/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2021 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.List;

import static com.sun.jna.platform.win32.WinReg.*;
import static qz.utils.SystemUtilities.*;

import static java.nio.file.attribute.AclEntryPermission.*;
import static java.nio.file.attribute.AclEntryFlag.*;

public class WindowsUtilities {
    protected static final Logger log = LogManager.getLogger(WindowsUtilities.class);
    private static final String THEME_REG_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String SPOOLER_REG_KEY = "Software\\Microsoft\\Windows NT\\CurrentVersion\\Print\\Printers";
    private static final String TRAY_REG_CHEVRON_KEY = "Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\TrayNotify";
    private static final String TRAY_REG_POLICY_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\Explorer";
    private static final String AUTHENTICATED_USERS_SID = "S-1-5-11";
    private static final int WINDOWS_10_BUILD_NUMBER = 10000;
    private static Boolean isWow64;
    private static Integer pid;
    private static HashMap<String, Path> printerSpoolerLocations = new HashMap<>();

    private static String defaultSpoolerLocation;

    public static boolean isDarkDesktop() {
        // 0 = Dark Theme.  -1/1 = Light Theme
        Integer regVal;
        if((regVal = getRegInt(HKEY_CURRENT_USER, THEME_REG_KEY, "AppsUseLightTheme")) != null) {
            // Fallback on apps theme
            return regVal == 0;
        }
        return false;
    }

    public static boolean isDarkTaskbar() {
        // -1/0 = Dark Theme.  1 = Light Theme
        Integer regVal;
        if((regVal = getRegInt(HKEY_CURRENT_USER, THEME_REG_KEY, "SystemUsesLightTheme")) != null) {
            // Prefer system theme
            return regVal == 0;
        }
        return true;
    }

    public static Version getOsVersion() {
        WinNT.OSVERSIONINFO versionInfo = new WinNT.OSVERSIONINFO();
        // GetVersionEx is deprecated, but has no sane replacement. https://learn.microsoft.com/en-us/windows/win32/api/sysinfoapi/nf-sysinfoapi-getversionexa
        if (!Kernel32.INSTANCE.GetVersionEx(versionInfo)) throw new RuntimeException();

        String build = "";
        if (versionInfo.dwBuildNumber.longValue() >= WINDOWS_10_BUILD_NUMBER) {
            // UBR or "Update Build Revision" was introduced in win10/server 2016. It reflects the monthly rollup version number.
            build = getRegInt(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "UBR").toString();
        }

        return Version.forIntegers(
                versionInfo.dwMajorVersion.intValue(),
                versionInfo.dwMinorVersion.intValue(),
                versionInfo.dwBuildNumber.intValue()
        ).setBuildMetadata(build);
    }

    /**
     * The human-readable display version of the Windows machine
     */
    public static String getOsDisplayVersion() {
        try {
            // Product name is the 'real' name of the os, e.g. Windows 10 Home
            String productName = getRegString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ProductName");
            Version version = SystemUtilities.getOsVersion();
            String extraInfo = "";

            if (version.getPatchVersion() < WINDOWS_10_BUILD_NUMBER) {
                WinNT.OSVERSIONINFO versionInfo = new WinNT.OSVERSIONINFO();
                Kernel32.INSTANCE.GetVersionEx(versionInfo);
                // CSD is the servicePack string in long form e.g. Service Pack 3
                extraInfo += " " + Native.toString(versionInfo.szCSDVersion);
            } else {
                // ReleaseID was both introduced and retired for Windows 10. If 'DisplayVersion' exists, we can ignore ReleaseId, as it will be '2009' forever
                int releaseID = Integer.parseInt(getRegString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ReleaseId"));
                // DisplayVersion is the last 2 digits of the year, followed by H1 or H2 depending on the year-half. e.g. 22H2
                if (Advapi32Util.registryValueExists(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "DisplayVersion")) {
                    extraInfo += " Version: " + getRegString(HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "DisplayVersion");
                } else {
                    extraInfo += " Release: " + releaseID;
                }
            }
            return productName + " " + version.toString().replace("+", ".") + extraInfo;
        } catch(Exception e) {
            log.warn("Couldn't get detailed OS version info, using cli fallback {}", e.getMessage());
        }
        try {
            // The ver command an internal command of cmd.exe. It must be executed through cmd
            String ver = ShellUtilities.executeRaw(new String[] {"cmd.exe", "/c", "ver"}).replaceAll("\\n", "");
            if(!ver.trim().isEmpty()) {
                return ver;
            }
            throw new Exception("Empty output received from \"ver\" command");
        } catch(Exception e) {
            log.warn("CLI fallback failed {}", e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Check known configurations which hide the Windows SystemTray area
     */
    public static boolean isHiddenSystemTray() {
        // Windows 11 22H2+: Check chevron is visible (assume "1" if key is missing)
        Integer chevronVisibility = getRegInt(HKEY_CURRENT_USER, TRAY_REG_CHEVRON_KEY, "SystemTrayChevronVisibility");
        if(chevronVisibility == null) chevronVisibility = 1;

        // Windows 2003+: Check user policy (assume "0" if key is missing)
        Integer explorerPolicy = getRegInt(HKEY_CURRENT_USER, TRAY_REG_POLICY_KEY, "NoTrayItemsDisplay");
        if(explorerPolicy == null) explorerPolicy = 0;

        // Return true if either flag is set
        return chevronVisibility == 0 || explorerPolicy == 1;
    }

    public static double getScaleFactor() {
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("9.0.0"))) {
            WinDef.HDC hdc = GDI32.INSTANCE.CreateCompatibleDC(null);
            if (hdc != null) {
                int actual = GDI32.INSTANCE.GetDeviceCaps(hdc, 10 /* VERTRES */);
                int logical = GDI32.INSTANCE.GetDeviceCaps(hdc, 117 /* DESKTOPVERTRES */);
                GDI32.INSTANCE.DeleteDC(hdc);
                if (logical != 0 && logical/actual > 1) {
                    return (double)logical/actual;
                }
            }
        }
        return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0d;
    }

    public static Path getSpoolerLocation(String printerName) throws FileNotFoundException {
        // TODO: If the spooler restarts, the spooler location could change, detect spooler restart
        if (printerSpoolerLocations.containsKey(printerName)) return printerSpoolerLocations.get(printerName);

        String regValue = getRegString(HKEY_LOCAL_MACHINE, SPOOLER_REG_KEY + printerName, "SpoolDirectory");
        if (regValue == null || regValue.isEmpty()) {
            if (defaultSpoolerLocation == null || defaultSpoolerLocation.isEmpty()) {
                defaultSpoolerLocation = getRegString(HKEY_LOCAL_MACHINE, SPOOLER_REG_KEY, "DefaultSpoolDirectory");
            }
            regValue = defaultSpoolerLocation;
        }
        Path spoolerLocation = Paths.get(regValue);
        if (regValue == null || regValue.isEmpty() || !Files.isDirectory(spoolerLocation)) throw new FileNotFoundException("Failed to locate spooler output.");
        printerSpoolerLocations.put(printerName, spoolerLocation);
        return spoolerLocation;
    }

    // gracefully swallow InvocationTargetException
    public static Integer getRegInt(HKEY root, String key, String value) {
        try {
            if (Advapi32Util.registryKeyExists(root, key) && Advapi32Util.registryValueExists(root, key, value)) {
                return Advapi32Util.registryGetIntValue(root, key, value);
            }
        } catch(Exception e) {
            log.warn("Couldn't get registry value {}\\\\{}\\\\{}", getHkeyName(root), key, value);
        }
        return null;
    }

    // gracefully swallow InvocationTargetException
    public static String getRegString(HKEY root, String key, String value) {
        try {
            if (Advapi32Util.registryKeyExists(root, key) && Advapi32Util.registryValueExists(root, key, value)) {
                return Advapi32Util.registryGetStringValue(root, key, value);
            }
        } catch(Exception e) {
            log.warn("Couldn't get registry value {}\\\\{}\\\\{}", getHkeyName(root), key, value);
        }
        return null;
    }

    /**
     * Deletes all matching data values directly beneath the specified key
     */
    public static boolean deleteRegData(HKEY root, String key, String data) {
        boolean success = true;
        if (Advapi32Util.registryKeyExists(root, key)) {
            for(Map.Entry<String, Object> entry : Advapi32Util.registryGetValues(root, key).entrySet()) {
                if(entry.getValue().equals(data)) {
                    try {
                        Advapi32Util.registryDeleteValue(root, key, entry.getKey());
                    } catch(Exception e) {
                        log.warn("Couldn't delete value {}\\\\{}\\\\{}", getHkeyName(root), key, entry.getKey());
                        success = false;
                    }
                }
            }
        }
        return success;
    }

    // gracefully swallow InvocationTargetException
    public static String[] getRegMultiString(HKEY root, String key, String value) {
        try {
            if (Advapi32Util.registryKeyExists(root, key) && Advapi32Util.registryValueExists(root, key, value)) {
                return Advapi32Util.registryGetStringArray(root, key, value);
            }
        } catch(Exception e) {
            log.warn("Couldn't get registry value {}\\{}\\{}", root, key, value);
        }
        return null;
    }

    // gracefully swallow InvocationTargetException
    public static boolean deleteRegKey(HKEY root, String key) {
        try {
            if (Advapi32Util.registryKeyExists(root, key)) {
                Advapi32Util.registryDeleteKey(root, key);
                return true;
            }
        } catch(Exception e) {
            log.warn("Couldn't delete value {}\\\\{}", getHkeyName(root), key);
        }
        return false;
    }

    // gracefully swallow InvocationTargetException
    public static boolean deleteRegValue(HKEY root, String key, String value) {
        try {
            if (Advapi32Util.registryValueExists(root, key, value)) {
                Advapi32Util.registryDeleteValue(root, key, value);
                return true;
            }
        } catch(Exception e) {
            log.warn("Couldn't delete value {}\\\\{}\\\\{}", getHkeyName(root), key, value);
        }
        return false;
    }

    /**
     * Adds a registry entry at <code>key</code>/<code>0</code>, incrementing as needed
     */
    public static boolean addNumberedRegValue(HKEY root, String key, Object data) {
        try {
            // Recursively create keys as needed
            String partialKey = "";
            for(String section : key.split("\\\\")) {
                if (partialKey.isEmpty()) {
                    partialKey += section;
                } else {
                    partialKey += "\\" + section;
                }
                if(!Advapi32Util.registryKeyExists(root, partialKey)) {
                    Advapi32Util.registryCreateKey(root, partialKey);
                }
            }
            // Make sure it doesn't already exist
            for(Map.Entry<String, Object> entry : Advapi32Util.registryGetValues(root, key).entrySet())  {
                if(entry.getValue().equals(data)) {
                    log.info("Registry data {}\\\\{}\\\\{} already has {}, skipping.", getHkeyName(root), key, entry.getKey(), data);
                    return true;
                }
            }
            // Find the next available number and iterate
            int counter=0;
            while(Advapi32Util.registryValueExists(root, key, counter + "")) {
                counter++;
            }
            String value = String.valueOf(counter);
            if (data instanceof String) {
                Advapi32Util.registrySetStringValue(root, key, value, (String)data);
            } else if (data instanceof Integer) {
                Advapi32Util.registrySetIntValue(root, key, value, (Integer)data);
            } else {
                throw new Exception("Registry values of type "  + data.getClass() + " aren't supported");
            }
            return true;
        } catch(Exception e) {
            log.error("Could not write numbered registry value at {}\\\\{}", getHkeyName(root), key, e);
        }
        return false;
    }

    public static boolean addRegValue(HKEY root, String key, String value, Object data) {
        try {
            // Recursively create keys as needed
            String partialKey = "";
            for(String section : key.split("\\\\")) {
                if (partialKey.isEmpty()) {
                    partialKey += section;
                } else {
                    partialKey += "\\" + section;
                }
                if(!Advapi32Util.registryKeyExists(root, partialKey)) {
                    Advapi32Util.registryCreateKey(root, partialKey);
                }
            }
            if (data instanceof String) {
                Advapi32Util.registrySetStringValue(root, key, value, (String)data);
            } else if (data instanceof Integer) {
                Advapi32Util.registrySetIntValue(root, key, value, (Integer)data);
            } else {
                throw new Exception("Registry values of type "  + data.getClass() + " aren't supported");
            }
            return true;
        } catch(Exception e) {
            log.error("Could not write registry value {}\\\\{}\\\\{}", getHkeyName(root), key, value, e);
        }
        return false;
    }

    /**
     * Use reflection to get readable <code>HKEY</code> name, useful for debugging errors
     */
    private static String getHkeyName(HKEY hkey) {
        for(Field f : WinReg.class.getFields()) {
            if (f.getName().startsWith("HKEY_")) {
                try {
                    if (f.get(HKEY.class).equals(hkey)) {
                        return f.getName();
                    }
                } catch(IllegalAccessException e) {
                    log.warn("Can't get name of HKEY", e);
                }
            }
        }
        return "UNKNOWN";
    }

    public static void setWritable(Path path) {
        try {
            UserPrincipal authenticatedUsers = path.getFileSystem().getUserPrincipalLookupService()
                    .lookupPrincipalByGroupName(Advapi32Util.getAccountBySid(AUTHENTICATED_USERS_SID).name);
            AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);

            // Create ACL to give "Authenticated Users" "modify" access
            AclEntry entry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(authenticatedUsers)
                    .setFlags(DIRECTORY_INHERIT,
                              FILE_INHERIT)
                    .setPermissions(WRITE_NAMED_ATTRS,
                                    WRITE_ATTRIBUTES,
                                    DELETE,
                                    WRITE_DATA,
                                    READ_ACL,
                                    APPEND_DATA,
                                    READ_ATTRIBUTES,
                                    READ_DATA,
                                    EXECUTE,
                                    SYNCHRONIZE,
                                    READ_NAMED_ATTRS)
                    .build();

            List<AclEntry> acl = view.getAcl();
            acl.add(0, entry); // insert before any DENY entries
            view.setAcl(acl);
        } catch(IOException e) {
            log.warn("Could not set writable: {}", path, e);
        }
    }

    static String getHostName() {
        String hostName = null;
        try {
            // GetComputerName() is limited to 15 chars, use GetComputerNameEx instead
            char buffer[] = new char[255];
            IntByReference lpnSize = new IntByReference(buffer.length);
            Kernel32.INSTANCE.GetComputerNameEx(WinBase.COMPUTER_NAME_FORMAT.ComputerNameDnsHostname, buffer, lpnSize);
            hostName = Native.toString(buffer).toUpperCase(Locale.ENGLISH); // Force uppercase for backwards compatibility
        } catch(Throwable ignore) {}
        if(hostName == null || hostName.trim().isEmpty()) {
            log.warn("Couldn't get hostname using Kernel32, will fallback to environmental variable COMPUTERNAME instead");
            hostName = System.getenv("COMPUTERNAME"); // always uppercase
        }
        return hostName;
    }

    public static boolean nativeFileCopy(Path source, Path destination) {
        try {
            ShellAPI.SHFILEOPSTRUCT op = new ShellAPI.SHFILEOPSTRUCT();
            op.wFunc = ShellAPI.FO_COPY;
            op.fFlags = Shell32.FOF_NOCOPYSECURITYATTRIBS | Shell32.FOF_NOCONFIRMATION;
            op.pFrom = op.encodePaths(new String[] {source.toString()});
            op.pTo = op.encodePaths(new String[] {destination.toString()});
            return Shell32.INSTANCE.SHFileOperation(op) == 0 && op.fAnyOperationsAborted == false;
        } catch(Throwable t) {
            log.warn("Unable to perform native file copy using JNA", t);
        }
        return false;
    }

    public static boolean elevatedFileCopy(Path source, Path destination) {
        // Recursively start powershell.exe, but elevated
        String args = String.format("'Copy-Item',-Path,'%s',-Destination,'%s'", source, destination);
        String[] command = {"Start-Process", "powershell.exe", "-ArgumentList", args, "-Wait", "-Verb", "RunAs"};
        return ShellUtilities.execute("powershell.exe", "-command", String.join(" ", command));
    }

    static int getProcessId() {
        if(pid == null) {
            try {
                pid = Kernel32.INSTANCE.GetCurrentProcessId();
            }
            catch(UnsatisfiedLinkError | NoClassDefFoundError e) {
                log.warn("Could not obtain process ID.  This usually means JNA isn't working.  Returning -1.");
                pid = -1;
            }
        }
        return pid;
    }

    public static boolean isWindowsXP() {
        return isWindows() && OS_NAME.contains("xp");
    }


    /**
     * Detect 32-bit JVM on 64-bit Windows
     * @return
     */
    public static boolean isWow64() {
        if(isWow64 == null) {
            isWow64 = false;
            if (SystemUtilities.isWindows()) {
                if (SystemUtilities.getJreArch() != JreArch.X86_64) {
                    isWow64 = System.getenv("PROGRAMFILES(x86)") != null;
                }
            }
        }
        return isWow64;
    }
}
