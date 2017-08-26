package qz.common;

import com.github.zafarkhaja.semver.Version;

import java.awt.*;

/**
 * Created by robert on 7/9/2014.
 */
public class Constants {
    public static final String HEXES = "0123456789ABCDEF";
    public static final char[] HEXES_ARRAY = HEXES.toCharArray();
    public static final int BYTE_BUFFER_SIZE = 8192;
    public static final Version VERSION = Version.valueOf("2.1.0-RC1");
    public static final Version JAVA_VERSION = Version.valueOf(System.getProperty("java.version").replaceFirst("_", "-"));

    /* QZ-Tray Constants */
    public static final String BLOCK_FILE = "blocked";
    public static final String ALLOW_FILE = "allowed";
    public static final String TEMP_FILE = "temp";
    public static final String LOG_FILE = "debug";
    public static final String PROPS_FILE = "qz-tray"; // .properties extension is assumed
    public static final String PREFS_FILE = "prefs"; // .properties extension is assumed
    public static final String DATA_DIR = "qz";
    public static final String SHARED_DATA_DIR = "shared";
    public static final int LOG_SIZE = 524288;
    public static final int LOG_ROTATIONS = 5;

    public static final int BORDER_PADDING = 10;

    public static final String ABOUT_TITLE = "QZ Tray";
    public static final String ABOUT_URL = "https://qz.io";
    public static final String ABOUT_COMPANY = "QZ Industries, LLC";

    public static final String TRUSTED_PUBLISHER = String.format("Verified by %s", Constants.ABOUT_COMPANY);
    public static final String UNTRUSTED_PUBLISHER = "Untrusted website";

    public static final String PROBE_REQUEST = "getProgramName";
    public static final String PROBE_RESPONSE = ABOUT_TITLE;

    public static final String PREFS_NOTIFICATIONS = "tray.notifications";
    public static final String PREFS_HEADLESS = "tray.headless";

    public static final String WHITE_LIST = "Permanently allowed \"%s\" to access local resources";
    public static final String BLACK_LIST = "Permanently blocked \"%s\" from accessing local resources";

    public static final String WHITE_SITES = "Sites permanently allowed access";
    public static final String BLACK_SITES = "Sites permanently blocked from access";

    public static final String ALLOWED = "Allowed";
    public static final String BLOCKED = "Blocked";

    public static final long VALID_SIGNING_PERIOD = 15 * 60 * 1000; //millis
    public static final int EXPIRY_WARN = 30;   // days
    public static final Color WARNING_COLOR = Color.RED;
    public static final Color TRUSTED_COLOR = Color.BLUE;

    public static final long MEMORY_PER_PRINT = 512; //MB

    public static final String RAW_PRINT = ABOUT_TITLE + " Raw Print";
    public static final String IMAGE_PRINT = ABOUT_TITLE + " Pixel Print";
    public static final String PDF_PRINT = ABOUT_TITLE + " PDF Print";
    public static final String HTML_PRINT = ABOUT_TITLE + " HTML Print";

    public static final Integer[] WSS_PORTS = {8181, 8282, 8383, 8484};
    public static final Integer[] WS_PORTS = {8182, 8283, 8384, 8485};
    public static final Integer[] CUPS_RSS_PORTS = {8586, 8687, 8788, 8889};

}
