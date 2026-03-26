package qz.installer.apps.locator;

import qz.common.Sluggable;

import java.util.Arrays;
import java.util.Locale;

public enum AppFamily {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            new AppVariant("Mozilla", "Mozilla Firefox", "org.mozilla.firefox"),
            new AppVariant("Mozilla", "Firefox Developer Edition", "org.mozilla.firefoxdeveloperedition"),
            new AppVariant("Mozilla", "Firefox Nightly", "org.mozilla.nightly"),
            new AppVariant("Mozilla", "LibreWolf", "org.mozilla.librewolf"),
            new AppVariant("Waterfox", "Waterfox", "net.waterfox.waterfoxcurrent"),
            new AppVariant("Mozilla", "IceCat", "org.gnu.icecat")
    ),
    CHROMIUM(
            new AppVariant("Google", "Google Chrome", "com.google.Chrome"),
            new AppVariant("Microsoft", "Microsoft Edge", "com.microsoft.Edge"),
            new AppVariant("Brave", "Brave Browser", "com.brave.Browser"),
            new AppVariant("Chromium", "Chromium", "org.chromium.Chromium")
    );
    final AppVariant[] appVariants;
    AppFamily(AppVariant... appVariants) {
        this.appVariants = appVariants;
    }

    public AppVariant[] getVariants() {
        return appVariants;
    }

    public static AppVariant findVariant(AppFamily appFamily, String appName, boolean stripVendor) {
        if (appName != null) {
            for (AppVariant appVariant : appFamily.appVariants) {
                if (appName.toLowerCase(Locale.ENGLISH).matches(appVariant.getName(stripVendor).toLowerCase(Locale.ENGLISH))) {
                    return appVariant;
                }
            }
        }
        return null;
    }

    public static class AppVariant {
        private final String vendor;
        private final String name;
        private final String bundleId;
        private final String slug;

        private AppFamily appFamily;

        private AppVariant(String vendor, String name, String bundleId) {
            this.name = name;
            this.vendor = vendor;
            this.bundleId = bundleId;
            this.slug = Sluggable.slugOf(getName(true));
        }

        public AppFamily getAppFamily() {
            if (appFamily == null) {
                appFamily = Arrays.stream(AppFamily.values())
                        .filter(aa -> Arrays.stream(aa.getVariants()).anyMatch(a -> a == this))
                        .findFirst()
                        .orElse(null);
            }
            return appFamily;
        }

        public String getVendor() {
            return vendor;
        }

        public String getName() {
            return name;
        }

        /**
         * Remove vendor prefix if exists
         *   "Mozilla Firefox" --> "Firefox"
         *   "Microsoft Edge" --> "Edge"
         *   "Google Chrome" --> "Chrome"
         *   "Brave Browser" --> "Brave"
         */
        public String getName(boolean stripVendor) {
            // Strip "Browser" from "Brave Browser"
            if(stripVendor && (name.endsWith("Browser") || name.endsWith("Classic"))) {
                return name.substring(0, name.length() - 7).trim();
            }
            if(stripVendor && name.startsWith(vendor) && !name.equals(vendor)) {
                return name.substring(vendor.length()).trim();
            }
            return name;
        }

        public String getBundleId() {
            return bundleId;
        }

        public String getSlug() {
            return slug;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
