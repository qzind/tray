package qz.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Each JDK provider uses their own os aliases
 * e.g. Adopt uses "mac" whereas BellSoft uses "macos".
 */
public enum VendorOs {
    // Values must contain underscore

    // MacOS
    ADOPT_MACOS("mac", "mac"),
    BELL_MACOS("macos", "mac");

    // Windows
    // (skip, all vendors use "windows")

    // Linux
    // (skip, all vendors use "linux")

    private static final Logger log = LoggerFactory.getLogger(VendorArch.class);

    String use;
    String[] matches;
    VendorOs(String use, String ... matches) {
        this.use = use;
        this.matches = matches;
    }

    public static String match(String vendor, String os) {
        if(os != null && vendor != null) {
            for(VendorOs alias : values()) {
                String vendorPrefix = alias.name().split("_")[0].toLowerCase(Locale.ROOT);
                if (vendor.toLowerCase(Locale.ROOT).startsWith(vendorPrefix)) {
                    for(String match : alias.matches) {
                        if (os.equalsIgnoreCase(match)) {
                            log.info("OS provided: {} matches: {}", os, match);
                            return alias.use;
                        }
                    }
                }
            }
        }
        return os;
    }

}