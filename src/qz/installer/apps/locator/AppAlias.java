package qz.installer.apps.locator;

import qz.common.Sluggable;

import java.util.Arrays;
import java.util.Locale;

public enum AppAlias {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            new Alias("Mozilla", "Mozilla Firefox", "org.mozilla.firefox", true),
            new Alias("Mozilla", "Firefox Developer Edition", "org.mozilla.firefoxdeveloperedition", true),
            new Alias("Mozilla", "Firefox Nightly", "org.mozilla.nightly", true),
            new Alias("Mozilla", "LibreWolf", "org.mozilla.librewolf", true),
            new Alias("Mozilla", "SeaMonkey", "org.mozilla.seamonkey", false),
            new Alias("Waterfox", "Waterfox", "net.waterfox.waterfoxcurrent", true),
            new Alias("Waterfox", "Waterfox Classic", "org.waterfoxproject.waterfox classic", false),
            new Alias("Mozilla", "Pale Moon", "org.mozilla.palemoon", false),
            // IceCat is technically enterprise ready, but not officially distributed for macOS, Windows
            new Alias("Mozilla", "IceCat", "org.gnu.icecat", false)
    ),
    CHROMIUM(
            new Alias("Google", "Google Chrome", "com.google.Chrome", true),
            new Alias("Microsoft", "Microsoft Edge", "com.microsoft.Edge", true),
            new Alias("Brave", "Brave Browser", "com.brave.Browser", true),
            new Alias("Chromium", "Chromium", "org.chromium.Chromium", true)
    );
    final Alias[] aliases;
    AppAlias(Alias... aliases) {
        this.aliases = aliases;
    }

    public Alias[] getAliases() {
        return aliases;
    }

    public static Alias findAlias(AppAlias appAlias, String appName, boolean stripVendor) {
        if (appName != null) {
            for (Alias alias : appAlias.aliases) {
                if (appName.toLowerCase(Locale.ENGLISH).matches(alias.getName(stripVendor).toLowerCase(Locale.ENGLISH))) {
                    return alias;
                }
            }
        }
        return null;
    }

    public static class Alias {
        private final String vendor;
        private final String name;
        private final String bundleId;
        private final boolean enterpriseReady;
        private final String slug;

        private AppAlias appAlias;

        private Alias(String vendor, String name, String bundleId, boolean enterpriseReady) {
            this.name = name;
            this.vendor = vendor;
            this.bundleId = bundleId;
            this.enterpriseReady = enterpriseReady;
            this.slug = Sluggable.slugOf(getName(true));
        }

        public AppAlias getAppAlias() {
            if (appAlias == null) {
                appAlias = Arrays.stream(AppAlias.values())
                        .filter(aa -> Arrays.stream(aa.getAliases()).anyMatch(a -> a == this))
                        .findFirst()
                        .orElse(null);
            }
            return appAlias;
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
         *   "Brave Browser" --> "Brave Browser"
         */
        public String getName(boolean stripVendor) {
            if(stripVendor && name.startsWith(vendor) && !name.equals(vendor)) {
                String stripped = name.substring(vendor.length()).trim();
                // Don't strip "Brave" from "Brave Browser", don't return an empty string
                if(!stripped.isEmpty() && !stripped.equalsIgnoreCase("browser")) {
                    return stripped;
                }
            }
            return name;
        }

        public String getBundleId() {
            return bundleId;
        }

        public String getSlug() {
            return slug;
        }

        /**
         * Returns whether the app is known to recognize enterprise policies, such as GPO
         */
        public boolean isEnterpriseReady() {
            return enterpriseReady;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
