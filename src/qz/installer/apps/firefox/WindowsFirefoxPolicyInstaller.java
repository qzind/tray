package qz.installer.apps.firefox;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.platform.win32.WinReg;
import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;
import qz.installer.apps.locator.AppAlias;
import qz.utils.WindowsUtilities;

public class WindowsFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    static final Version REQUIRED_FIREFOX_VERSION = Version.parse("62.0.0");
    static final PolicyType[] SUPPORTED_POLICY_TYPES = new PolicyType[]{ PolicyType.REGISTRY, PolicyType.JSON };

    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "%s/distribution/policies.json",
    };

    private static final String[] MANAGED_POLICY_REGISTRY_PATTERNS = {
            "SOFTWARE\\Policies\\%s\\%s",
    };

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String... values) {
        throw new UnsupportedOperationException("This shouldn't happen");
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String... values) {
        throw new UnsupportedOperationException("This shouldn't happen");
    }

    @Override
    public Version getRequiredFirefoxVersion() {
        return REQUIRED_FIREFOX_VERSION;
    }

    @Override
    public PolicyType[] getSupportedPolicyTypes() {
        return SUPPORTED_POLICY_TYPES;
    }

    @Override
    public boolean hasConflict() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean installOsSpecificPolicy(AppAlias.Alias alias, Installer.PrivilegeLevel scope, String policy, String value) {
        //AppAlias.Alias alias = AppAlias.FIREFOX.getAliases()[0]; // fixme
        //policy = "Certificates/ImportEnterpriseRoots"; // fixme

        // Firefox may use nesting for policies e.g. "Certificates/ImportEnterpriseRoots"
        // We'll need to isolate the parent
        String scoped = "";
        int lastSlashIndex = policy.lastIndexOf('/');
        if(lastSlashIndex != -1) {
            policy = policy.substring(lastSlashIndex + 1);  // e.g. "ImportEnterpriseRoots"
            scoped = policy.substring(0, lastSlashIndex);
            scoped = "/" + scoped; // e.g. "/Certificates"
            scoped = scoped.replace('/', '\\'); // e.g. "\Certificates"
        }
        boolean success = true;
        for(String pattern : MANAGED_POLICY_REGISTRY_PATTERNS) {
            String key = String.format(pattern, alias.getVendor(), alias.getName(true) + scoped);

            WinReg.HKEY regScope = scope == Installer.PrivilegeLevel.SYSTEM ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER;
            if(WindowsUtilities.addRegValue(regScope, key, "Comment", POLICY_AUDIT_MESSAGE)) {
                success = success && WindowsUtilities.addRegValue(regScope, key, policy, 1);
            }
        }
        return success;
    }
}
