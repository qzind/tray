package qz.installer.apps.policy.locator;

import qz.installer.Installer;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.locator.AppFamily;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MacPolicyLocator implements PolicyInstaller.PolicyLocator {
    // e.g. "%s/Library/Preferences/com.google.Chrome.plist"
    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/Library/Preferences/%s.plist";

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType ignore) {
        return Paths.get(String.format(MANAGED_POLICY_PATH_PATTERN,
                                       scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : "",
                                       appVariant.getBundleId()));
    }
}
