package qz.build.jlink;

import com.github.zafarkhaja.semver.Version;
import qz.build.JLink;

/**
 * Handling of java vendors
 */
public enum Vendor implements Parsable {
    ECLIPSE("Eclipse", "Adoptium", "adoptium", "temurin", "adoptopenjdk"),
    BELLSOFT("BellSoft", "Liberica", "bellsoft", "liberica"),
    IBM("IBM", "Semeru", "ibm", "semeru"),
    MICROSOFT("Microsoft", "OpenJDK", "microsoft"),
    AMAZON("Amazon", "Corretto", "amazon", "corretto"),
    AZUL("Azul", "Zulu", "azul", "zulu");

    public String vendorName;
    public String productName;
    public final String[] matches;
    Vendor(String vendorName, String productName, String ... matches) {
        this.matches = matches;
        this.vendorName = vendorName;
        this.productName = productName;
    }

    public static Vendor parse(String value, Vendor fallback) {
        return Parsable.parse(Vendor.class, value, fallback, true);
    }

    public static Vendor parse(String value) {
        return Parsable.parse(Vendor.class, value);
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getProductName() {
        return productName;
    }

    /**
     * Map Vendor to Arch value
     */
    public String getUrlArch(Arch arch) {
        switch(arch) {
            case AARCH64:
                // All vendors seem to use "aarch64" universally
                return "aarch64";
            case AMD64:
                switch(this) {
                    // BellSoft uses "amd64"
                    case BELLSOFT:
                        return "amd64";
                }
            default:
                return "x64";
        }
    }

    /**
     * Map Vendor to Platform name
     */
    public String getUrlPlatform(Platform platform) {
        switch(platform) {
            case MAC:
                switch(this) {
                    case BELLSOFT:
                        return "macos";
                    case MICROSOFT:
                        return "macOS";
                    case AMAZON:
                    case AZUL:
                        return "macosx";
                }
            default:
                return platform.value();
        }
    }

    /**
     * Map Vendor and Platform to file extension
     */
    public String getUrlExtension(Platform platform) {
        switch(this) {
            case BELLSOFT:
                switch(platform) {
                    case LINUX:
                        return "tar.gz";
                    default:
                        // BellSoft uses "zip" for mac and windows platforms
                        return "zip";
                }
            default:
                switch(platform) {
                    case WINDOWS:
                        return "zip";
                    default:
                        return "tar.gz";
                }
        }
    }

    public String getUrlJavaVersion(Version javaSemver) {
        switch(this) {
            case MICROSOFT:
            case AZUL:
                // Return shorted version (Microsoft, Azul suppresses the build information from URLs)
                return javaSemver.toString().split("\\+")[0];
            case AMAZON:
                // Return lengthened version (Corretto formats major.minor.patch.build.number, e.g. 11.0.17.8.1)
                String[] parts = javaSemver.toString().split("\\+");
                String javaVersion = parts[0];
                //
                String buildAndNumber = parts[1];
                if(!buildAndNumber.contains(".")) {
                    // Append ".1" if ".number" is missing
                    buildAndNumber += ".1";
                }
                return String.format("%s.%s", javaVersion, buildAndNumber);
        }
        // All others seem to prefer "+" replaced with "_"
        return javaSemver.toString().replaceAll("\\+", "_");
    }
}

