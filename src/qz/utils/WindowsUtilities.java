package qz.utils;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.platform.win32.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.List;
import java.util.Map;

import static com.sun.jna.platform.win32.WinReg.*;

import static java.nio.file.attribute.AclEntryPermission.*;
import static java.nio.file.attribute.AclEntryFlag.*;

public class WindowsUtilities {
    protected static final Logger log = LoggerFactory.getLogger(WindowsUtilities.class);
    private static final String AUTHENTICATED_USERS_SID = "S-1-5-11";

    public static boolean isDarkMode() {
        String key = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

        // 0 = Dark Theme.  -1/1 = Light Theme
        Integer regVal;
        if((regVal = getRegInt(HKEY_CURRENT_USER, key, "SystemUsesLightTheme")) != null) {
            // Prefer system theme
            return regVal == 0;
        } else if((regVal = getRegInt(HKEY_CURRENT_USER, key, "AppsUseLightTheme")) != null) {
            // Fallback on apps theme
            return regVal == 0;
        }
        return false;
    }

    public static int getScaleFactor() {
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("9.0.0"))) {
            WinDef.HDC hdc = GDI32.INSTANCE.CreateCompatibleDC(null);
            if (hdc != null) {
                int actual = GDI32.INSTANCE.GetDeviceCaps(hdc, 10 /* VERTRES */);
                int logical = GDI32.INSTANCE.GetDeviceCaps(hdc, 117 /* DESKTOPVERTRES */);
                GDI32.INSTANCE.DeleteDC(hdc);
                if (logical != 0 && logical/actual > 1) {
                    return logical/actual;
                }
            }
        }
        return (int)(Toolkit.getDefaultToolkit().getScreenResolution() / 96.0);
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
}
