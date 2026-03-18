package qz.installer.apps.policy.locator;

import qz.installer.Installer;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.PolicyInstaller;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WindowsPolicyLocator implements PolicyInstaller.PolicyLocator {
    // e.g. "SOFTWARE\\Policies\\Google\\Chrome\\%s",
    private static final String MANAGED_POLICY_PATH_PATTERN = "SOFTWARE\\Policies\\%s\\%s";

    /**
     * Calculates the registry key path.
     */
    @Override
    public Path getLocation(Installer.PrivilegeLevel unused, AppAlias.Alias alias) {
        return Paths.get(String.format(MANAGED_POLICY_PATH_PATTERN, alias.getVendor(), alias.getName(true)));
    }
}
