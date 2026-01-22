package qz.installer.apps;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.chromium.WindowsChromiumPolicyInstaller;
import qz.installer.apps.firefox.*;
import qz.installer.apps.firefox.locator.AppAlias;
import qz.installer.apps.firefox.locator.AppInfo;
import qz.installer.apps.firefox.locator.AppLocator;
import qz.utils.SystemUtilities;

import java.util.ArrayList;

/**
 * Firefox supports three main methods for installing enterprise policies:
 * <table>
 *     <th><td><strong>macOS</strong></td><td><strong>Windows</strong></td><td><strong>Linux</strong></td></th>
 *     <tr><td><code>PLIST</code></td><td>x</td><td></td><td></td></tr>
 *     <tr><td><code>REGISTRY</code><td></td><td>x</td><td></td></tr>
 *     <tr><td><code>JSON</code></td><td>x</td><td>x</td><td>x</td></tr>
 * </table>
 * Implementing classes MUST implement all possible methods preferring fallback behavior
 * for <code>install()</code> and <code>uninstall()</code> respectively.
 * <br><br>
 * Prior to the above policy methods, the legacy technique was to run an "autoconfig"
 * scripts, which is still available for very old Firefox versions to install certificates.  See
 * <code>FirefoxPolicyInstaller.supportsEnterprisePolicies(...)</code> and
 * </code><code>LegacyFirefoxCertificateInstaller</code> for details.
 */
public abstract class FirefoxPolicyInstaller {
    private static final Logger log = LogManager.getLogger(FirefoxPolicyInstaller.class);

    public enum PolicyType {
       PLIST,
       REGISTRY,
       JSON;
    }

    static FirefoxPolicyInstaller INSTANCE = getInstance();
    public abstract boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values);
    public abstract boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values);
    public abstract Version getRequiredFirefoxVersion();
    public abstract PolicyType[] getSupportedPolicyTypes();
    public abstract boolean hasConflict();

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

    public static boolean supportsEnterprisePolicies(AppInfo appInfo) {
        return appInfo.getVersion().isHigherThanOrEquivalentTo(getInstance().getRequiredFirefoxVersion());
    }

    public static void main(String ... args) {
        ArrayList<AppInfo> foundApps = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        for(AppInfo app : foundApps) {
            log.info("Found {}", app.getPath());
        }
    }
}
