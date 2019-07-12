package qz.common;

import com.github.zafarkhaja.semver.Version;
import qz.utils.SystemUtilities;

import java.awt.*;
import java.io.File;
import java.util.function.Supplier;

import static qz.common.I18NLoader.gettext;

/**
 * Created by robert on 7/9/2014.
 */
public class Constants {
    public static final PropertyHelper USER_PREFS = new PropertyHelper(SystemUtilities.getDataDirectory() + File.separator + Constants.PREFS_FILE + ".properties", true);
    static {
        I18NLoader.setup(USER_PREFS);
    }
    public static final String HEXES = "0123456789ABCDEF";
    public static final char[] HEXES_ARRAY = HEXES.toCharArray();
    public static final int BYTE_BUFFER_SIZE = 8192;
    public static final Version VERSION = Version.valueOf("2.1.0-RC7");
    public static final Version JAVA_VERSION = SystemUtilities.getJavaVersion();
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");

    /* QZ-Tray Constants */
    public static final String BLOCK_FILE = "blocked";
    public static final String ALLOW_FILE = "allowed";
    public static final String TEMP_FILE = "temp";
    public static final String LOG_FILE = "debug";
    public static final String PROPS_FILE = "qz-tray"; // .properties extension is assumed
    public static final String PREFS_FILE = "prefs"; // .properties extension is assumed
    public static final String AUTOSTART_FILE = ".autostart";
    public static final String DATA_DIR = "qz";
    public static final String SHARED_DATA_DIR = "shared";
    public static final int LOG_SIZE = 524288;
    public static final int LOG_ROTATIONS = 5;

    public static final int BORDER_PADDING = 10;

    public static final String ABOUT_TITLE = "QZ Tray";
    public static final String ABOUT_URL = "https://qz.io";
    public static final String ABOUT_COMPANY = "QZ Industries, LLC";

    public static final String TRUSTED_PUBLISHER = String.format(gettext("Verified by %s"), Constants.ABOUT_COMPANY);
    public static final String UNTRUSTED_PUBLISHER = gettext("Untrusted website");

    public static final String PROBE_REQUEST = "getProgramName";
    public static final String PROBE_RESPONSE = ABOUT_TITLE;

    public static final String PREFS_NOTIFICATIONS = "tray.notifications";
    public static final String PREFS_HEADLESS = "tray.headless";

    public static String WHITE_LIST = gettext("Permanently allowed \"%s\" to access local resources");
    public static String BLACK_LIST = gettext("Permanently blocked \"%s\" from accessing local resources");
    public static String BLACK_LIST_PROMPT = gettext("Permanently block \"%s\" from accessing local resources?");

    public static String WHITE_SITES = gettext("Sites permanently allowed access");
    public static String BLACK_SITES = gettext("Sites permanently blocked from access");

    public static String ALLOWED = gettext("Allowed");
    public static String BLOCKED = gettext("Blocked");

    public static final long VALID_SIGNING_PERIOD = 15 * 60 * 1000; //millis
    public static final int EXPIRY_WARN = 30;   // days
    public static Color WARNING_COLOR = Color.RED;
    public static Color TRUSTED_COLOR = Color.BLUE;
    public static Color WARNING_COLOR_LIGHTER = Color.decode("#EB6261");
    public static Color TRUSTED_COLOR_LIGHTER = Color.decode("#589DF6");

    public static final long MEMORY_PER_PRINT = 512; //MB

    public static String RAW_PRINT = String.format(gettext("%s Raw Print"), ABOUT_TITLE);
    public static String IMAGE_PRINT = String.format(gettext("%s Pixel Print"), ABOUT_TITLE);
    public static String PDF_PRINT = String.format(gettext("%s PDF Print"), ABOUT_TITLE);
    public static String HTML_PRINT = String.format(gettext("%s HTML Print"), ABOUT_TITLE);

    public static final Integer[] WSS_PORTS = {8181, 8282, 8383, 8484};
    public static final Integer[] WS_PORTS = {8182, 8283, 8384, 8485};
    public static final Integer[] CUPS_RSS_PORTS = {8586, 8687, 8788, 8889};

    public static final String SANDBOX_DIR = "/sandbox";
    public static final String NOT_SANDBOX_DIR = "/shared";
    public static final int FILE_LISTENER_DEFAULT_LINES = 10;
}
