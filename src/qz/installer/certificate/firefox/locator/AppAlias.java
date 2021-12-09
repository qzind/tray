package qz.installer.certificate.firefox.locator;

import java.util.Locale;

public enum AppAlias {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            new Alias("Mozilla", "Mozilla Firefox", "org.mozilla.firefox", true),
            new Alias("Mozilla", "Firefox Developer Edition", "org.mozilla.firefoxdeveloperedition", true),
            new Alias("Mozilla", "Firefox Nightly", "org.mozilla.nightly", true),
            new Alias("Mozilla", "SeaMonkey", "org.mozilla.seamonkey", false),
            new Alias("Waterfox", "Waterfox", "net.waterfox.waterfoxcurrent", true),
            new Alias("Waterfox", "Waterfox Classic", "org.waterfoxproject.waterfox classic", false),
            new Alias("Mozilla", "Pale Moon", "org.mozilla.palemoon", false),
            // IceCat is technically enterprise ready, but not officially distributed for macOS, Windows
            new Alias("Mozilla", "IceCat", "org.gnu.icecat", false)
    );
    Alias[] aliases;
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
        private String vendor;
        private String name;
        private String bundleId;
        private boolean enterpriseReady;
        private String posix;

        public Alias(String vendor, String name, String bundleId, boolean enterpriseReady) {
            this.name = name;
            this.vendor = vendor;
            this.bundleId = bundleId;
            this.enterpriseReady = enterpriseReady;
            this.posix = getName(true).replaceAll(" ", "").toLowerCase(Locale.ENGLISH);
        }

        public String getVendor() {
            return vendor;
        }

        public String getName() {
            return name;
        }

        /**
         * Remove vendor prefix if exists
         */
        public String getName(boolean stripVendor) {
            if(stripVendor && "Mozilla".equals(vendor) && name.startsWith(vendor)) {
                return name.substring(vendor.length()).trim();
            }
            return name;
        }

        public String getBundleId() {
            return bundleId;
        }

        public String getPosix() {
            return posix;
        }

        /**
         * Returns whether or not the app is known to recognizes enterprise policies, such as GPO
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
