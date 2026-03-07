package qz.installer.apps.chromium;

import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.installer.apps.locator.AppAlias;
import qz.utils.WindowsUtilities;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(WindowsChromiumPolicyInstaller.class);

    // e.g. "SOFTWARE\\Policies\\Google\\Chrome\\%s",
    private static final String MANAGED_POLICY_PATH_PATTERN = "SOFTWARE\\Policies\\%s\\%s\\%s";

    @Override
    public boolean install(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        WinReg.HKEY root = scope == Installer.PrivilegeLevel.SYSTEM ? HKEY_LOCAL_MACHINE : HKEY_CURRENT_USER;
        String key = calculateLocation(scope, alias, policyName, isArray);
        log.info("Installing Chromium ({}) policy {}...", alias.getName(false), key);

        for(Object value : values) {
            if (isArray) {
                // Assume all policy arrays are numbered
                if(!WindowsUtilities.addNumberedRegValue(root, key, 0, value)) {
                    return false;
                }
            } else {// exit early/antipattern: registry key can only occur once
               return WindowsUtilities.addRegValue(root, key, policyName, value);
            }
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        WinReg.HKEY root = scope == Installer.PrivilegeLevel.SYSTEM ? HKEY_LOCAL_MACHINE : HKEY_CURRENT_USER;
        String key = calculateLocation(scope, alias, policyName, isArray);
        log.info("Removing Chromium ({}) policy {}...", alias.getName(false), key);
        for(Object value : values) {
            if (isArray) {
                // Assume all policy arrays are numbered
                if(!WindowsUtilities.deleteNumberedRegValue(root, key, value)) {
                    return false;
                }
            } else {
                if(!WindowsUtilities.deleteRegValue(root, key, policyName)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Calculates the registry key path.
     * If <code>isArray</code> is true, appends policyName to key path; otherwise, trims the trailing slash.
     */
    @Override
    public String calculateLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray) {
        String key = String.format(MANAGED_POLICY_PATH_PATTERN, alias.getVendor(), alias.getName(true), isArray ? policyName : "");
        if(key.endsWith("\\")) {
            // trim trailing "\"
            key = key.substring(0, key.length() - 1);
        }
        return key;
    }
}
