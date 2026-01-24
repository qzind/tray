package qz.installer.apps.firefox;

import com.github.zafarkhaja.semver.Version;
import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;

public class WindowsFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    static final Version REQUIRED_FIREFOX_VERSION = Version.parse("62.0.0");
    static final PolicyType[] SUPPORTED_POLICY_TYPES = new PolicyType[]{ PolicyType.REGISTRY, PolicyType.JSON };

    // FIXME: Why isn't this dynamic?
    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "SOFTWARE\\Policies\\Mozilla\\Firefox\\%s",
            "SOFTWARE\\Policies\\Mozilla\\Firefox Developer Edition\\%s",
            "SOFTWARE\\Policies\\Mozilla\\Firefox Nightly\\%s",
            "SOFTWARE\\Policies\\Waterfox\\Waterfox\\%s",
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
