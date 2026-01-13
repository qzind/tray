package qz.installer.apps.chromium;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.utils.ShellUtilities;

import java.util.HashSet;

public class MacChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(MacChromiumPolicyInstaller.class);

    private static final String[] MACOS_POLICY_LOCATIONS = {
            "%s/Library/Preferences/com.google.Chrome.plist",
            "%s/Library/Preferences/com.microsoft.Edge.plist"
    };

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String unscopedLocation : MACOS_POLICY_LOCATIONS) {
            String location = String.format(unscopedLocation, scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") : "");
            log.info("Installing Chromium policy {} to {}...", policyName, location);

            for(String value : values) {
                String found = ShellUtilities.executeRaw(new String[]{"/usr/bin/defaults", "read", location, policyName}, true);
                if(found.contains(value)) {
                    log.info("Chromium policy {} '{}' already exists at location {}, skipping", policyName, value, location);
                    continue;
                }
                ShellUtilities.execute("/usr/bin/defaults", "write", location, policyName, "-array-add", value);
            }
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String unscopedLocation : MACOS_POLICY_LOCATIONS) {
            String location = scope == Installer.PrivilegeLevel.USER ? System.getProperty("user.home") + unscopedLocation : unscopedLocation;
            log.info("Removing Chromium policy {} from {}...", policyName, location);

            String found = ShellUtilities.executeRaw(new String[]{"/usr/bin/defaults", "read", location, policyName}, true);
            if(found.isEmpty()) {
                log.info("Chromium policy {} was not found at location {}, skipping", policyName, location);
                return false;
            }

            // Dedupe and cleanup values
            // WARNING: This logic is intended only for plist arrays
            String[] lines = found.split("[\r?\n]+");
            HashSet<String> foundValues = new HashSet<>();
            for(String line : lines) {
                String deserialized = line.trim().replaceAll("[,()]", "");
                // Isolate value from inside quotes, (e.g. "qz://*")
                if(deserialized.startsWith("\"") && deserialized.endsWith("\"")) {
                    foundValues.add(deserialized.replaceAll("^\"|\"$", ""));
                }
            }

            for(String value : values) {
                if(foundValues.contains(value)) {
                    log.info("Found Chromium policy {} value '{}', marking for removal", policyName, value);
                    foundValues.remove(value);
                }
            }

            /*
             * "defaults write" offers "-array-add" (additive) and "-array" (destructive) and
             * although "-array" (destructive) would be more succinct when rewriting the entire policy,
             * strict formatting and serialization makes this more volatile, so we delete the entire
             * array and rewrite the values one-by-one instead
             */
            if(!ShellUtilities.execute("/usr/bin/defaults", "delete", location, policyName)) {
                log.warn("Unable to delete Chromium policy {} from {}", policyName, location);
                return false;
            }

            if(foundValues.isEmpty()) {
                // We're empty; delete the array
                log.info("Removed Chromium policy {} from {} by removing entire key", policyName, location);
            } else {
                // Rewrite our array with the values removed
                for(String value : foundValues) {
                    if(!ShellUtilities.execute("/usr/bin/defaults", "write", location, policyName, "-array-add", value)) {
                        log.warn("Unable to add Chromium policy {} {} value: {}", policyName, location, value);
                        return false;
                    }
                }
                log.info("Removed Chromium policy {} at {} while preserving existing entries: {}", policyName, location, foundValues.toString());
            }
        }
        return true;
    }
}
