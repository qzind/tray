package qz.build.jlink;

import com.github.zafarkhaja.semver.Version;
import qz.build.provision.params.Arch;

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
            case ARM32:
                switch(this) {
                    case BELLSOFT:
                        return "arm32-vfp-hflt";
                    case AZUL:
                        return "aarch32hf";
                    case MICROSOFT:
                    case IBM:
                        throw new UnsupportedOperationException("Vendor does not provide builds for this architecture");
                    case AMAZON:
                    case ECLIPSE:
                    default:
                        return "arm";
                }
            case RISCV64:
                return "riscv64";
            case X86:
                switch(this) {
                    case AZUL:
                        return "i686";
                    case BELLSOFT:
                        return "i586";
                    case ECLIPSE:
                    case IBM:
                        return "x86-32";
                    case AMAZON:
                    default:
                        return "x86";
                }
            case X86_64:
            default:
                switch(this) {
                    // BellSoft uses "amd64"
                    case BELLSOFT:
                        return "amd64";
                }
                return "x64";
        }
    }

    public String getApiPlatform(Platform platform) {
        switch(this) {
            case BELLSOFT:
                // Assume they're the same unless we know otherwise
                return String.format("os=%s",getUrlPlatform(platform));
            case ECLIPSE:
            default:
                throw new UnsupportedOperationException(String.format("Filtering API by os is not yet supported for this vendor (%s)", this));
        }
    }

    public String getApiMajorVersion(Version version) {
        switch(this) {
            case BELLSOFT:
                // Assume they're the same unless we know otherwise
                return String.format("version-feature=%d", version.majorVersion());
            case ECLIPSE:
            default:
                throw new UnsupportedOperationException(String.format("Filtering API by major version is not yet supported for this vendor (%s)", this));
        }
    }

    public String getApiArch(Arch arch) {
        switch(this) {
            case BELLSOFT:
                switch(arch) {
                    case ARM32:
                    case AARCH64:
                        return String.format("arch=arm&bitness=%s", arch.getBitness());
                    case X86:
                    case X86_64:
                        return String.format("arch=x86&bitness=%s", arch.getBitness());
                    case RISCV32:
                    case RISCV64:
                        return String.format("arch=riscv&bitness=%s", arch.getBitness());
                    case PPC64:
                        return String.format("arch=ppc&bitness=%s", arch.getBitness());
                }
            case ECLIPSE:
            default:
                throw new UnsupportedOperationException(String.format("Filtering API by arch '%s' is not yet supported for this vendor (%s)", arch, this));
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

