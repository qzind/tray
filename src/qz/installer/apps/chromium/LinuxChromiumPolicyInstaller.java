package qz.installer.apps.chromium;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.installer.apps.locator.AppAlias;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static qz.installer.apps.LinuxJsonInstaller.*;

public class LinuxChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(LinuxChromiumPolicyInstaller.class);

    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/policies/managed/%s.json";

    // TODO:
    // 2. Decide if we should use a symlink to a JSON file in /opt/qz-tray or if we should
    //    write each file individually
    // 3. JSON should support boolean/int/string and maybe array
    @Override
    public boolean install(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        if(scope != Installer.PrivilegeLevel.SYSTEM) {
            // TODO: Remove this if Linux user-level policies can be confirmed
            log.info("Skipping installation of Chromium ({}) policy {}, not supported at PrivilegeLevel '{}'...", alias.getName(false), policyName, scope);
            return false;
        }
        File location = new File(calculateLocation(scope, alias, policyName, isArray));
        log.info("Installing Chromium ({}) policy {} to {}...", alias.getName(false), policyName, location);
        if(isArray) {
            // treat as an array
            return writeJsonArray(location, policyName, values);
        }
        return writeJsonValues(location, policyName, values);
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values) {
        if(scope != Installer.PrivilegeLevel.SYSTEM) {
            // TODO: Remove this if Linux user-level policies can be confirmed
            log.info("Skipping removal of Chromium ({}) policy {}, not supported at PrivilegeLevel '{}'...", alias.getName(false), policyName, scope);
            return false;
        }

        File location = new File(calculateLocation(scope, alias, policyName, isArray));
        log.info("Removing Chromium ({}) policy {} from {}...", alias.getName(false), policyName, location);
        if(!removeJsonValue(location, policyName)) {
            log.warn("Unable to remove Chromium ({}) policy {}", alias.getName(false), location);
            return false;
        }
        return true;
    }

    @Override
    public String calculateLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray) {
        Path prefix;
        switch(scope) {
            case SYSTEM:
                prefix = Paths.get("/");
                switch(alias.getSlug()) {
                    case "chromium":
                        // OS-provided are stored in /etc/<name>
                        prefix = prefix.resolve("etc");
                        break;
                    case "chrome":
                    case "edge":
                    case "brave":
                    default:
                        // 3rd-party provided are stored in /etc/opt/<name>
                        prefix = prefix.resolve("etc").resolve("opt");
                }
                break;
            case USER:
                // ~/.config
                prefix = Paths.get(System.getProperty("user.home")).resolve(".config");
                break;
            default:
                throw new UnsupportedOperationException(String.format("Scope %s is not yet supported", scope));
        }
        return String.format(MANAGED_POLICY_PATH_PATTERN, prefix.resolve(alias.getSlug()), Constants.PROPS_FILE);
    }
}

