package qz.installer.apps.policy.locator;

import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.PolicyInstaller;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LinuxChromiumPolicyLocator implements PolicyInstaller.PolicyLocator {
    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/policies/managed/%s.json";

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
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
        return Paths.get(String.format(MANAGED_POLICY_PATH_PATTERN, prefix.resolve(alias.getSlug()), Constants.PROPS_FILE));
    }
}

