package qz.installer.apps;

import qz.installer.Installer;
import qz.installer.apps.firefox.*;
import qz.utils.SystemUtilities;

/**
 * Firefox supports two main methods for installing policies:
 * <ul>
 *     <li>Enterprise policies via <code>.plist</code> (macOS) or registry (windows)</li>
 *     <li>Flat file <code>policies.json</code> (Linux, macOS, Windows)</li>
 * </ul>
 * For maximum compatibility, we'll try both, so implementing classes must implement both with
 * fallback behavior for <code>install()</code> and <code>uninstall()</code>.
 * <br>
 * Prior to the above policies methods, the legacy technique was to run an "autoconfig"
 * scripts, which is not supported by this class.
 */
public abstract class FirefoxPolicyInstaller {
    static FirefoxPolicyInstaller INSTANCE = getInstance();

    public abstract boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values);
    public abstract boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values);

    public static FirefoxPolicyInstaller getInstance() {
        if(INSTANCE == null) {
            switch(SystemUtilities.getOs()) {
                case WINDOWS:
                    INSTANCE = new WindowsFirefoxPolicyInstaller();
                    break;
                case MAC:
                    INSTANCE = new MacFirefoxPolicyInstaller();
                    break;
                case LINUX:
                default:
                    INSTANCE = new LinuxFirefoxPolicyInstaller();
            }
        }
        return INSTANCE;
    }

}
