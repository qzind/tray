package qz.installer.apps.chromium;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.installer.apps.MacPreferenceInstaller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MacChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(MacChromiumPolicyInstaller.class);

    private static final String[] MANAGED_POLICY_PATH_PATTERNS = {
            "%s/Library/Preferences/com.google.Chrome.plist",
            "%s/Library/Preferences/com.microsoft.Edge.plist"
    };

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String pattern : MANAGED_POLICY_PATH_PATTERNS) {
            String location = String.format(pattern, scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : "");
            log.info("Installing Chromium policy {} to {}...", policyName, location);
            for(String value : values) {
                MacPreferenceInstaller.appendArray(Paths.get(location), policyName, Collections.singletonList(value), true);
            }
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String pattern : MANAGED_POLICY_PATH_PATTERNS) {
            Path plist = Paths.get(String.format(pattern, scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : ""));
            log.info("Removing Chromium policy {} from {}...", policyName, plist);
            Collection<String> existing = MacPreferenceInstaller.getArray(plist, policyName, true);

            if(existing.isEmpty()) {
                log.info("Chromium policy {} was not found or empty at location {}, skipping", policyName, plist);
                return true;
            }

            // Remove just our own entries
            existing.removeAll(List.of(values));
            // Write remaining entries back
            // Note: dedupe: true will delete what's there first, so this may result in the policyName being removed entirely
            MacPreferenceInstaller.appendArray(plist, policyName, existing, true);
        }
        return true;
    }
}
