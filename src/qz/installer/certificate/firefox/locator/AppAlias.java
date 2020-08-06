package qz.installer.certificate.firefox.locator;

import java.util.Locale;

public enum AppAlias {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            // Alias([Vendor], Name)
            new Alias(null, "Firefox", "org.mozilla.firefox"), // macOS, Linux
            new Alias("Mozilla", "Mozilla Firefox", "org.mozilla.firefox"), // Windows
            new Alias("Mozilla", "SeaMonkey", "org.mozilla.seamonkey"),
            new Alias("Waterfox", "Waterfox", "net.waterfox.waterfoxcurrent"),
            new Alias("Mozilla", "Pale Moon", "org.mozilla.palemoon"),
            new Alias("Mozilla", "IceCat", "org.gnu.icecat")
    );
    Alias[] aliases;
    AppAlias(Alias... aliases) {
        this.aliases = aliases;
    }

    public Alias[] getAliases() {
        return aliases;
    }

    public boolean setBundleId(AppInfo appInfo) {
        if (appInfo.getName() != null && !appInfo.isBlacklisted()) {
            for (Alias alias : aliases) {
                if (appInfo.getName().toLowerCase(Locale.ENGLISH).matches(alias.name.toLowerCase(Locale.ENGLISH))) {
                    appInfo.setBundleId(alias.bundleId);
                    return true;
                }
            }
        }
        return false;
    }

    public static class Alias {
        public String vendor;
        public String name;
        public String bundleId;
        public String posix;

        public Alias(String vendor, String name, String bundleId) {
            this.name = name;
            this.posix = name.replaceAll(" ", "").toLowerCase(Locale.ENGLISH);
            this.vendor = vendor;
            this.bundleId = bundleId;
        }
    }
}
