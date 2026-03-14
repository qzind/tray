package qz.installer.apps.policy.locator;

import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.PolicyInstaller;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LinuxPolicyLocator implements PolicyInstaller.PolicyLocator {
    final static String CHROMIUM_POLICY_PATTERN = "%s/policies/managed/%s.json";
    final static String FIREFOX_POLICY_PATTERN = "%s/policies/policies.json";
    final static Path SYSTEM_PREFIX = Paths.get("/etc");
    final static Path USER_PREFIX = Paths.get(System.getProperty("user.home")).resolve(".config");

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
        AppAlias appAlias = alias.getAppAlias();
        switch(appAlias) {
            case CHROMIUM:
                // /etc/chromium/, /etc/opt/chrome/
                return Paths.get(String.format(CHROMIUM_POLICY_PATTERN, getPrefix(alias, scope), Constants.PROPS_FILE));
            case FIREFOX:
                // /etc/firefox/
                return Paths.get(String.format(FIREFOX_POLICY_PATTERN, getPrefix(alias, scope)));
        }
        throw new UnsupportedOperationException(String.format("PolicyLocator for %s on Linux has not yet implemented", appAlias));
    }

    private static Path getPrefix(AppAlias.Alias alias, Installer.PrivilegeLevel scope) {
        switch(scope) {
            case SYSTEM:
                switch(alias.getAppAlias()) {
                    case CHROMIUM:
                        return alias.getSlug().equals("chromium")
                                // first-party: /etc/chromium
                                ? SYSTEM_PREFIX.resolve(alias.getSlug())
                                // third-party: /etc/opt/chrome
                                : SYSTEM_PREFIX.resolve("opt").resolve(alias.getSlug());
                    case FIREFOX:
                    default:
                        // /etc/firefox
                        return SYSTEM_PREFIX;
                }
            case USER:
                // TODO: placeholder: not yet supported
                return USER_PREFIX.resolve(alias.getSlug()); // ~/.config/chromium
            default:
                throw new UnsupportedOperationException(String.format("Scope %s is not yet supported", scope));
        }
    }
}

