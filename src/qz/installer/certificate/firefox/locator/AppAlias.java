package qz.installer.certificate.firefox.locator;

import java.util.Locale;

public enum AppAlias {
    // Tor Browser intentionally excluded; Tor's proxy blocks localhost connections
    FIREFOX(
            // Alias([Vendor], Name)
            new Alias("Firefox"), // macOS, Linux
            new Alias("Mozilla", "Mozilla Firefox"), // Windows
            new Alias("Mozilla", "SeaMonkey"),
            new Alias("Mozilla", "Waterfox"),
            new Alias("Mozilla", "Pale Moon"),
            new Alias("Mozilla", "IceCat")
    );
    Alias[] aliases;
    AppAlias(Alias... aliases) {
        this.aliases = aliases;
    }

    public Alias[] getAliases() {
        return aliases;
    }

    public boolean matches(AppInfo appInfo) {
        if (appInfo.getName() != null && !appInfo.isBlacklisted()) {
            for (Alias alias : aliases) {
                if (appInfo.getName().toLowerCase(Locale.ENGLISH).matches(alias.name.toLowerCase(Locale.ENGLISH))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class Alias {
        public String vendor;
        public String name;
        public String posix;
        public Alias(String name) {
            this.name = name;
            this.posix = name.replaceAll(" ", "").toLowerCase(Locale.ENGLISH);
        }
        public Alias(String vendor, String name) {
            this(name);
            this.vendor = vendor;
        }
    }
}
