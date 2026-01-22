package qz.installer.apps.firefox;

import com.github.zafarkhaja.semver.Version;
import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;

public class LinuxFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    static final Version REQUIRED_FIREFOX_VERSION = Version.parse("65.0.0");
    static final PolicyType[] SUPPORTED_POLICY_TYPES = new PolicyType[]{ PolicyType.JSON };

    // FIXME:  Where do other browsers store this?
    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "/etc/firefox/policies/policies.json",
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
}
