package qz.installer.apps.chromium;

import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.utils.WindowsUtilities;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(WindowsChromiumPolicyInstaller.class);

    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "SOFTWARE\\Policies\\Google\\Chrome\\%s",
            "SOFTWARE\\Policies\\Microsoft\\Edge\\%s",
    };

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        WinReg.HKEY root = scope == Installer.PrivilegeLevel.SYSTEM ? HKEY_LOCAL_MACHINE : HKEY_CURRENT_USER;
        for(String pattern : MANAGED_POLICY_PATH_PATTERNS) {
            String key = String.format(pattern, policyName);
            log.info("Installing Chromium policy {}...", key);
            for(String value : values) {
                WindowsUtilities.addNumberedRegValue(root, key, value);
            }
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        WinReg.HKEY root = scope == Installer.PrivilegeLevel.SYSTEM ? HKEY_LOCAL_MACHINE : HKEY_CURRENT_USER;
        for(String pattern : MANAGED_POLICY_PATH_PATTERNS) {
            String key = String.format(pattern, policyName);
            log.info("Removing Chromium policy {}...", key);
            for(String value : values) {
                WindowsUtilities.removeNumberedRegValue(root, key, value);
            }
        }
        return true;
    }
}
