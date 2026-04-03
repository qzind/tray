package qz.installer.apps.policy.locator;

import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.PolicyInstaller;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LinuxPolicyLocator implements PolicyInstaller.PolicyLocator {
    final static String CHROMIUM_POLICY_PATTERN = "%s/policies/managed/%s.json";
    final static String FIREFOX_POLICY_PATTERN = "%s/policies/policies.json";

    final static Path DEFAULT_SYSTEM_PREFIX = Paths.get("/etc");
    final static String SNAP_SYSTEM_PREFIX_PATTERN = "/var/snap/%s/current";

    final static String FLATPAK_PREFIX_PATTERN = "%s/flatpak/extension/%s.Extension.%s/%s/1/";
    final static String FLATPAK_SYSTEM_PREFIX = "/var/lib/";
    final static String FLATPAK_USER_PREFIX = String.format("%s/.local/share", System.getProperty("user.home"));

    public static void main(String ... args) {
        LinuxPolicyLocator locator = new LinuxPolicyLocator();
        for(Installer.PrivilegeLevel scope : new Installer.PrivilegeLevel[] { Installer.PrivilegeLevel.USER, Installer.PrivilegeLevel.SYSTEM }) {
            for(AppFamily.AppVariant appVariant : AppFamily.CHROMIUM.getVariants()) {
                for(AppType appType : PolicyInstaller.PolicyLocator.AppType.values()) {
                    try {
                        System.err.printf("%s\n  Type: %s\n  Scope: %s\n  Path:  '%s'\n", appVariant, appType, scope, locator.getLocation(scope, appVariant, appType));
                    }
                    catch(Exception e) {
                        System.out.printf(" %s%n", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public Path getLocation(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType appType) {
        AppFamily appFamily = appVariant.getAppFamily();
        Path prefix = getPrefix(scope, appVariant, appType);
        switch(appFamily) {
            case CHROMIUM:
                // /etc/chromium/, /etc/opt/chrome/
                switch(appType) {
                    case FLATPAK: // different prefix, same policy pattern
                    case SNAP: // different prefix, same policy pattern
                    case NATIVE:
                        return Paths.get(String.format(CHROMIUM_POLICY_PATTERN, prefix, Constants.PROPS_FILE));
                    default:
                }
            case FIREFOX:
                switch(appType) {
                    case SNAP: // identical path as default
                    case NATIVE:
                        // /etc/firefox/
                        return Paths.get(String.format(FIREFOX_POLICY_PATTERN, prefix));
                    case FLATPAK: // see https://github.com/mozilla/policy-templates/discussions/1301
                    default:
                }
        }
        throw unsupportedException(scope, appType, appVariant);
    }

    private static Path getPrefix(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType appType) {
        switch(scope) {
            case SYSTEM:
                switch(appVariant.getAppFamily()) {
                    case CHROMIUM:
                        switch(appType) {
                            case FLATPAK:
                                return Paths.get(String.format(FLATPAK_PREFIX_PATTERN, FLATPAK_SYSTEM_PREFIX, appVariant.getBundleId(), Constants.PROPS_FILE, SystemUtilities.getArch()));
                            case SNAP:
                                return Paths.get(String.format(SNAP_SYSTEM_PREFIX_PATTERN, appVariant.getSlug()));
                            case NATIVE:
                                return appVariant.getSlug().equals("chromium")
                                        // first-party: /etc/chromium
                                        ? DEFAULT_SYSTEM_PREFIX.resolve(appVariant.getSlug())
                                        // third-party: /etc/opt/chrome
                                        : DEFAULT_SYSTEM_PREFIX.resolve("opt").resolve(appVariant.getSlug());
                            default: // unsupported
                        }
                    case FIREFOX:
                    default:
                        switch(appType) {
                            case SNAP:
                            case NATIVE:
                                // /etc/firefox
                                return DEFAULT_SYSTEM_PREFIX.resolve(appVariant.getSlug());
                            default: // unsupported
                        }
                }
            case USER:
                switch(appVariant.getAppFamily()) {
                    case CHROMIUM:
                        switch(appType) {
                            case FLATPAK:
                                return Paths.get(String.format(FLATPAK_PREFIX_PATTERN, FLATPAK_USER_PREFIX, appVariant.getBundleId(), Constants.PROPS_FILE, SystemUtilities.getArch()));
                            case NATIVE:
                            default: // unsupported
                        }
                    case FIREFOX:
                    default: // unsupported
                }
        }
        throw unsupportedException(scope, appType, appVariant);
    }

    private static UnsupportedOperationException unsupportedException(Installer.PrivilegeLevel scope, AppType appType, AppFamily.AppVariant appVariant) {
        return new UnsupportedOperationException(String.format("Scope '%s' for AppType '%s' is not yet supported for '%s'", scope, appType, appVariant));
    }
}

