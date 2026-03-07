package qz.installer.apps.chromium;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.installer.apps.MacPreferenceInstaller;
import qz.installer.apps.locator.AppAlias;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MacChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(MacChromiumPolicyInstaller.class);

    // e.g. "%s/Library/Preferences/com.google.Chrome.plist"
    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/Library/Preferences/%s.plist";

    @Override
    public boolean install(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        Path plist = Paths.get(calculateLocation(scope, alias, policyName, isArray));
        log.info("Installing Chromium ({}) policy {} to {}...", alias.getName(false), policyName, plist);

            if(isArray) {
                if (!MacPreferenceInstaller.appendArray(plist, policyName, Arrays.asList(values), true)) {
                    return false;
                }
            } else {
                return MacPreferenceInstaller.write(plist, policyName, values);
            }

        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        Path plist = Paths.get(calculateLocation(scope, alias, policyName, isArray));
        log.info("Removing Chromium ({}) policy {} from {}...", alias.getName(false), policyName, plist);
        if(isArray) {
            Collection<Object> existing = MacPreferenceInstaller.getArray(plist, policyName, true);
            if (existing.isEmpty()) {
                log.info("Chromium ({}) policy file was not found or empty at {}, skipping...", alias.getName(false), plist);
                return true;
            }

            // Remove just our own entries
            existing.removeAll(List.of(values));
            // Write remaining entries back
            // Note: dedupe: true will delete what's there first, so this may result in the policyName being removed entirely
            MacPreferenceInstaller.appendArray(plist, policyName, existing, true);
        } else {
            MacPreferenceInstaller.delete(plist, policyName);
        }
        return true;
    }

    @Override
    public String calculateLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray) {
        return String.format(MANAGED_POLICY_PATH_PATTERN,
                             scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : "",
                             alias.getBundleId());
    }
}
