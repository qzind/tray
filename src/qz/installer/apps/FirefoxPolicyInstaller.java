package qz.installer.apps;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.firefox.*;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.locator.AppInfo;
import qz.installer.apps.locator.AppLocator;
import qz.utils.SystemUtilities;

import java.util.HashSet;

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

    public static final String POLICY_AUDIT_MESSAGE = "Managed policy installed by " + Constants.ABOUT_TITLE + " on " + SystemUtilities.timeStamp();

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

    public boolean supportsEnterprisePolicies(AppInfo appInfo) {
        return appInfo.getVersion().isHigherThanOrEquivalentTo(getInstance().getRequiredFirefoxVersion());
    }

    public boolean installJsonPolicy() {
        throw new UnsupportedOperationException("shouldn't get here");
    }

    public abstract boolean installOsSpecificPolicy(AppAlias.Alias alias, Installer.PrivilegeLevel scope, String policy, String value);

    public static void main(String ... args) {
        HashSet<AppInfo> foundApps = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        for(AppInfo app : foundApps) {
            log.info("Found {}", app.getPath());
            // FIXME: 'value' must be boolean on macOS
            getInstance().installOsSpecificPolicy(app.getAlias(), Installer.PrivilegeLevel.USER, "Certificates/ImportEnterpriseRoots", "1");
        }
    }
}
