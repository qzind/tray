package qz.installer.apps.firefox;

import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;
import qz.installer.apps.firefox.locator.AppAlias;

public class WindowsFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    private static final String[] WINDOWS_POLICY_LOCATIONS = {
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
}
