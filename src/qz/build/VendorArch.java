package qz.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Each JDK provider uses their own architecture aliases
 * e.g. Adopt uses "x64" whereas BellSoft uses "amd64".
 */
public enum VendorArch {
    // Values must contain underscore

    // AMD64
    ADOPT_AMD64("x64", "amd64", "x86_64", "x64"),
    BELL_AMD64("amd64", "amd64", "x86_64", "x64"),

    // ARM64
    ADOPT_AARCH64("aarch64", "aarch64", "arm64"),
    BELL_AARCH64("aarch64", "aarch64", "arm64");

    private static final Logger log = LogManager.getLogger(VendorArch.class);

    String use;
    String[] matches;
    VendorArch(String use, String ... matches) {
        this.use = use;
        this.matches = matches;
    }

    public static String match(String vendor, String arch, String fallback) {
        if(arch != null && vendor != null) {
            for(VendorArch alias : values()) {
                String vendorPrefix = alias.name().split("_")[0].toLowerCase(Locale.ROOT);
                if (vendor.toLowerCase(Locale.ROOT).startsWith(vendorPrefix)) {
                    for(String match : alias.matches) {
                        if (arch.equalsIgnoreCase(match)) {
                            log.info("Arch provided: {} matches: {}", arch, match);
                            return alias.use;
                        }
                    }
                }
            }
        }
        log.warn("Arch provided couldn't be matched: {} falling back to: {}", arch, fallback);
        return fallback;
    }

}
