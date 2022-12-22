package qz.build;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * Each JDK provider uses their own url format
 */
public enum VendorUrlPattern {
    // FIXME: I think this should be a hashmap now
    ADOPT(JLink.Vendor.ECLIPSE, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s/OpenJDK%sU-jdk_%s_%s_%s_%s.%s"),
    SEMERU(JLink.Vendor.IBM, "https://github.com/ibmruntimes/semeru%s-binaries/releases/download/jdk-%s_%s-%s/ibm-semeru-open-jdk_%s_%s_%s_%s-%s.%s"),
    MICROSOFT(JLink.Vendor.MICROSOFT, "https://aka.ms/download-jdk/microsoft-jdk-%s-%s-%s.%s"),
    BELL(JLink.Vendor.BELLSOFT, "https://download.bell-sw.com/java/%s/bellsoft-jdk%s-%s-%s.%s");

    private static final Logger log = LogManager.getLogger(VendorUrlPattern.class);

    JLink.Vendor vendor;
    String pattern;
    VendorUrlPattern(JLink.Vendor vendor, String pattern) {
        this.vendor = vendor;
        this.pattern = pattern;
    }

    public static VendorUrlPattern getPattern(JLink.Vendor vendor) {
        for(VendorUrlPattern pattern : values()) {
            if (vendor.equals(pattern.vendor)) {
                return pattern;
            }
        }

        log.warn("Vendor provided couldn't be matched: {} will fallback to default: {}", vendor, BELL);
        return BELL;
    }

    public static String format(JLink.Vendor vendor, JLink.Arch arch, JLink.Platform platform, String gcEngine, Version javaSemver, String gcVer) throws UnsupportedEncodingException {
        VendorUrlPattern pattern = VendorUrlPattern.getPattern(vendor);
        String urlArch = vendor.getUrlArch(arch);
        String fileExt = vendor.getUrlExtension(platform);
        String urlPlatform = vendor.getUrlPlatform(platform);
        String javaVersionUnderscore = javaSemver.toString().replaceAll("\\+", "_");
        String javaVersionUrlEncode = URLEncoder.encode(javaSemver.toString(), "UTF-8");

        int javaMajor = javaSemver.getMajorVersion();
        switch(pattern) {
            case BELL:
                return String.format(pattern.pattern, javaVersionUrlEncode, javaVersionUrlEncode, urlPlatform, urlArch, fileExt);
            case SEMERU:
                return String.format(pattern.pattern, javaMajor, javaVersionUrlEncode, gcEngine, gcVer, urlArch, urlPlatform, javaVersionUnderscore, gcEngine, gcVer, fileExt);
            case MICROSOFT:
                String javaShortened = javaVersionUnderscore.split("_")[0];
                return String.format(pattern.pattern, javaShortened, urlPlatform, urlArch, fileExt);
            case ADOPT:
            default:
                return String.format(pattern.pattern, javaMajor, javaVersionUrlEncode, javaMajor, urlArch, urlPlatform, gcEngine, javaVersionUnderscore, fileExt);
        }
    }

}
