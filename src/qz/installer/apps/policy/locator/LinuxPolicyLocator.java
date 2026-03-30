package qz.installer.apps.policy.locator;

import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.PolicyInstaller;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LinuxPolicyLocator implements PolicyInstaller.PolicyLocator {
    final static String CHROMIUM_POLICY_PATTERN = "%s/policies/managed/%s.json";

    final static String DEFAULT_FIREFOX_POLICY_PATTERN = "%s/policies/policies.json";
    final static String FLATPAK_FIREFOX_POLICY_PATTERN = "%s/current/active/files/etc/%s/policies/policies.json";

    final static Path DEFAULT_SYSTEM_PREFIX = Paths.get("/etc");
    final static Path FLATPAK_SYSTEM_PREFIX = Paths.get("/var/lib/flatpak/app");

    final static Path DEFAULT_USER_PREFIX = Paths.get(System.getProperty("user.home")).resolve(".config");
    final static Path FLATPAK_USER_PREFIX = Paths.get(System.getProperty("user.home")).resolve(".local/share/flatpak/app");

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType appType) {
        AppFamily appFamily = appVariant.getAppFamily();
        Path prefix = getPrefix(scope, appVariant, appType);
        switch(appFamily) {
            case CHROMIUM:
                // /etc/chromium/, /etc/opt/chrome/
                if(appType != AppType.DEFAULT) {
                    // TODO: Add AppType support
                    throw new UnsupportedOperationException(String.format("AppType '%s' is not yet supported for '%s'", appType, appVariant));
                }
                return Paths.get(String.format(CHROMIUM_POLICY_PATTERN, prefix, Constants.PROPS_FILE));
            case FIREFOX:
                switch(appType) {
                    case FLATPAK:
                        // /var/lib/flatpak/app/org.mozilla.firefox/current/active/files/etc/firefox/
                        return Paths.get(String.format(FLATPAK_FIREFOX_POLICY_PATTERN, prefix, appVariant.getSlug()));
                    case APPIMAGE: // untested
                    case SNAP:
                    case DEFAULT:
                    default:
                        // /etc/firefox/
                        return Paths.get(String.format(DEFAULT_FIREFOX_POLICY_PATTERN, prefix));
                }

        }
        throw new UnsupportedOperationException(String.format("PolicyLocator for %s on Linux has not yet implemented", appFamily));
    }

    private static Path getPrefix(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType appType) {
        switch(scope) {
            case SYSTEM:
                switch(appVariant.getAppFamily()) {
                    case CHROMIUM:
                        // TODO: Add AppType support
                        return appVariant.getSlug().equals("chromium")
                                // first-party: /etc/chromium
                                ? DEFAULT_SYSTEM_PREFIX.resolve(appVariant.getSlug())
                                // third-party: /etc/opt/chrome
                                : DEFAULT_SYSTEM_PREFIX.resolve("opt").resolve(appVariant.getSlug());
                    case FIREFOX:
                    default:
                        switch(appType) {
                            case FLATPAK:
                                // /var/lib/flatpak/app/org.mozilla.firefox
                                return FLATPAK_SYSTEM_PREFIX.resolve(appVariant.getBundleId());
                            case APPIMAGE: // untested
                            case SNAP:
                            case DEFAULT:
                            default:
                                // /etc/firefox
                                return DEFAULT_SYSTEM_PREFIX.resolve(appVariant.getSlug());
                        }

                }
            case USER:
                switch(appVariant.getAppFamily()) {
                    case FIREFOX:
                        switch(appType) {
                            case FLATPAK:
                                // ~/.local/share/flatpak/app/org.mozilla.firefox"
                                return FLATPAK_USER_PREFIX.resolve(appVariant.getBundleId());
                            case APPIMAGE:
                            case SNAP:
                            case DEFAULT:
                            default:
                                // fallthrough
                        }
                    case CHROMIUM:
                    default:
                        // fallthrough
                }
                // TODO: placeholder: not yet supported
                return DEFAULT_USER_PREFIX.resolve(appVariant.getSlug()); // ~/.config/chromium
            default:
                throw new UnsupportedOperationException(String.format("Scope %s is not yet supported", scope));
        }
    }
}

