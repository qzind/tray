package qz.build.jlink;


import qz.utils.SystemUtilities;

/**
 * Handling of platform names as they would appear in a URL
 * Values added must also be added to <code>ArgValue.JLINK --platform</code> values
 */
public enum Platform implements Parsable {
    MAC("mac"),
    WINDOWS("windows"),
    LINUX("linux");

    public final String[] matches;
    Platform(String ... matches) { this.matches = matches; }

    public static Platform parse(String value, Platform fallback) {
        return Parsable.parse(Platform.class, value, fallback);
    }

    public static Platform parse(String value) {
        return Parsable.parse(Platform.class, value);
    }

    public static Platform getCurrentPlatform() {
        switch(SystemUtilities.getOsType()) {
            case MAC:
                return Platform.MAC;
            case WINDOWS:
                return Platform.WINDOWS;
            case LINUX:
            default:
                return Platform.LINUX;
        }
    }
}
