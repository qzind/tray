package qz.installer.apps.firefox;

import com.github.zafarkhaja.semver.Version;
import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;
import qz.installer.apps.locator.AppAlias;

public class LinuxFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    static final Version REQUIRED_FIREFOX_VERSION = Version.parse("65.0.0");
    static final PolicyType[] SUPPORTED_POLICY_TYPES = new PolicyType[]{ PolicyType.JSON };

    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "/etc/%s/policies/policies.json",
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
        return false;
    }
}
