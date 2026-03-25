package qz.installer.apps.locator;

import qz.common.Sluggable;

import java.util.Arrays;
import java.util.Locale;

public enum AppAlias {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            new Alias("Mozilla", "Mozilla Firefox", "org.mozilla.firefox"),
            new Alias("Mozilla", "Firefox Developer Edition", "org.mozilla.firefoxdeveloperedition"),
            new Alias("Mozilla", "Firefox Nightly", "org.mozilla.nightly"),
            new Alias("Mozilla", "LibreWolf", "org.mozilla.librewolf"),
            new Alias("Waterfox", "Waterfox", "net.waterfox.waterfoxcurrent"),
            new Alias("Mozilla", "Pale Moon", "org.mozilla.palemoon"),
            new Alias("Mozilla", "IceCat", "org.gnu.icecat")
    ),
    CHROMIUM(
            new Alias("Google", "Google Chrome", "com.google.Chrome"),
            new Alias("Microsoft", "Microsoft Edge", "com.microsoft.Edge"),
            new Alias("Brave", "Brave Browser", "com.brave.Browser"),
            new Alias("Chromium", "Chromium", "org.chromium.Chromium")
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
        private final String slug;

        private AppAlias appAlias;

        private Alias(String vendor, String name, String bundleId) {
            this.name = name;
            this.vendor = vendor;
            this.bundleId = bundleId;
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
