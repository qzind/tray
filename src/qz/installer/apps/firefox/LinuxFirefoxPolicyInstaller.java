package qz.installer.apps.firefox;

import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;

public class LinuxFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    private static final String[] LINUX_POLICY_LOCATIONS = {
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
}
