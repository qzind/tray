package qz.build.jlink;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import static qz.build.jlink.Vendor.*;

/**
 * Each JDK provider uses their own url format
 */
public class Url {
    static HashMap<Vendor, String> VENDOR_URL_MAP = new HashMap<>();
    static {
        VENDOR_URL_MAP.put(BELLSOFT, "https://download.bell-sw.com/java/%s/bellsoft-jdk%s-%s-%s.%s");
        VENDOR_URL_MAP.put(ECLIPSE, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s/OpenJDK%sU-jdk_%s_%s_%s_%s.%s");
        VENDOR_URL_MAP.put(IBM, "https://github.com/ibmruntimes/semeru%s-binaries/releases/download/jdk-%s_%s-%s/ibm-semeru-open-jdk_%s_%s_%s_%s-%s.%s");
        VENDOR_URL_MAP.put(MICROSOFT, "https://aka.ms/download-jdk/microsoft-jdk-%s-%s-%s.%s");
        VENDOR_URL_MAP.put(AMAZON, "https://corretto.aws/downloads/resources/%s/amazon-corretto-%s-%s-%s.%s");
        VENDOR_URL_MAP.put(AZUL, "https://cdn.azul.com/zulu%s/bin/zulu%s-ca-jdk%s-%s_%s.%s");
    }

    private static final Logger log = LogManager.getLogger(Url.class);

    Vendor vendor;
    String pattern;
    public Url(Vendor vendor) {
        this.vendor = vendor;
        if(!VENDOR_URL_MAP.containsKey(vendor)) {
            throw new UnsupportedOperationException(String.format("Vendor provided '%s' couldn't be matched to a URL pattern, aborting.", vendor));
        }
        pattern = VENDOR_URL_MAP.get(vendor);
    }

    /**
     * Fix URLs for beta/alpha/nightly release
     */
    public void applyBetaPattern() {
        switch(vendor) {
            case ECLIPSE:
                /*
                    BEFORE: jdk-21.0.1+12/
                    AFTER:  jdk-21.0.1+12-ea-beta/

                    BEFORE: OpenJDK21U-jdk_riscv64_linux_hotspot_21-0-1-12.tar.gz
                    AFTER:  OpenJDK21U-jdk_riscv64_linux_hotspot_ea_21-0-1-12.tar.gz
                 */
                pattern = pattern.replaceAll("%s/OpenJDK%sU-jdk_%s_%s_%s", "%s-ea-beta/OpenJDK%sU-jdk_%s_%s_%s_ea");
                break;
            default:
                throw new UnsupportedOperationException("Vendor " + vendor + " is missing a configuration beta URLs");
        }
    }

    public String format(Arch arch, Platform platform, String gcEngine, Version javaSemver, String javaVersion, String gcVer) throws UnsupportedEncodingException {
        Url pattern = new Url(vendor);
        String urlArch = vendor.getUrlArch(arch);
        String fileExt = vendor.getUrlExtension(platform);
        String urlPlatform = vendor.getUrlPlatform(platform);
        String urlJavaVersion = vendor.getUrlJavaVersion(javaSemver);

        // Convert "+" to "%2B"
        String urlJavaVersionEncode = URLEncoder.encode(javaVersion, "UTF-8");

        int javaMajor = javaSemver.getMajorVersion();

        switch(arch) {
            // TODO: Remove when RISCV is offered as stable
            case RISCV64:
                applyBetaPattern();
            default:
                // Do nothing
        }

        switch(vendor) {
            case BELLSOFT:
                return String.format(pattern.pattern, urlJavaVersionEncode, urlJavaVersionEncode, urlPlatform, urlArch, fileExt);
            case ECLIPSE:
                return String.format(pattern.pattern, javaMajor, urlJavaVersionEncode, javaMajor, urlArch, urlPlatform, gcEngine, urlJavaVersion, fileExt);
            case IBM:
                return String.format(pattern.pattern, javaMajor, urlJavaVersionEncode, gcEngine, gcVer, urlArch, urlPlatform, urlJavaVersion, gcEngine, gcVer, fileExt);
            case MICROSOFT:
                return String.format(pattern.pattern, urlJavaVersion, urlPlatform, urlArch, fileExt);
            case AMAZON:
                return String.format(pattern.pattern, urlJavaVersion, urlJavaVersion, urlPlatform, urlArch, fileExt);
            case AZUL:
                // Special handling of Linux aarch64
                String embedded = platform == Platform.LINUX ? "-embedded" : "";
                return String.format(pattern.pattern, embedded, gcVer, urlJavaVersion, urlPlatform, urlArch, fileExt);
            default:
                throw new UnsupportedOperationException(String.format("URL pattern for '%s' (%s) is missing a format implementation.", vendor, pattern));
        }
    }

}
