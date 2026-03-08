package qz.installer.apps.policy.locator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.installer.MacPolicyInstaller;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.PolicyState;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MacChromiumPolicyLocator implements PolicyInstaller.PolicyLocator {
    // e.g. "%s/Library/Preferences/com.google.Chrome.plist"
    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/Library/Preferences/%s.plist";

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
        return Paths.get(String.format(MANAGED_POLICY_PATH_PATTERN,
                             scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : "",
                             alias.getBundleId()));
    }
}
