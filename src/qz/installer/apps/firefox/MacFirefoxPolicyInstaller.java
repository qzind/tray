package qz.installer.apps.firefox;

import qz.installer.Installer;
import qz.installer.apps.FirefoxPolicyInstaller;

public class MacFirefoxPolicyInstaller extends FirefoxPolicyInstaller {
    private static final String[] MACOS_POLICY_LOCATIONS = {
            "%s/Library/Preferences/org.mozilla.firefox.plist",
            "%s/Library/Preferences/org.mozilla.firefoxdeveloperedition.plist",
            "%s/Library/Preferences/org.mozilla.nightly.plist",
            "%s/Library/Preferences/net.waterfox.waterfoxcurrent.plist",
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
