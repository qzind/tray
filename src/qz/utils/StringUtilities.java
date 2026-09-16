package qz.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;

public class StringUtilities {
    private static final Logger log = LogManager.getLogger(StringUtilities.class);

    public static final String[] HTML_ENTITIES = {"&", "<", ">", "\"", "'", "/"};
    public static final String[] HTML_REPLACED =  {"&amp;", "&lt;", "&gt;", "&quot;", "&apos;", "&sol;"};

    public static String escapeHtmlEntities(String text) {
        return StringUtils.replaceEach(text, HTML_ENTITIES, HTML_REPLACED);
    }

    /**
     * A clone of <code>Charset.forName()</code>, but with a "legacy" helper to simulate old JDK behavior
     */
    public static Charset getCharset(String charsetName) {
        if("legacy".equalsIgnoreCase(charsetName)) {
            if (SystemUtilities.isWindows()) {
                Charset legacyCharset =  WindowsUtilities.LEGACY_CHARSET;
                log.info("Selecting legacy charset '{}'", legacyCharset);
            }
            Charset defaultCharset = Charset.defaultCharset();
            log.warn("Legacy charset is only available on Windows, using default charset '{}' instead", defaultCharset);
            return defaultCharset;
        }
        return Charset.forName(charsetName);
    }
}
