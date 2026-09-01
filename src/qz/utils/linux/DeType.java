package qz.utils.linux;

import qz.utils.ShellUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for identifying a Desktop environment in Linux or a Linux-like system by
 * querying dbus or other tools
 */
public enum DeType {
    KDE("Enabled:\\s*1.*?\\bScale:\\s*(\\d+(?:\\.\\d+)?)",
        "gdbus", "call", "--session",
        "--dest" , "org.kde.KWin",
        "--object-path", "/KWin",
        "--method", "org.kde.KWin.supportInformation"),
    GNOME("\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
          "gdbus", "call", "--session",
           "--dest", "org.gnome.Mutter.DisplayConfig",
           "--object-path", "/org/gnome/Mutter/DisplayConfig",
           "--method", "org.gnome.Mutter.DisplayConfig.GetCurrentState"),
    XFCE("<(\\d+)>", "gdbus", "call", "--session",
            "--dest", "org.xfce.Xfconf",
            "--object-path", "/org/xfce/Xfconf",
            "--method", "org.xfce.Xfconf.GetProperty",
            "xsettings", "/Gdk/WindowScalingFactor"),
    COSMIC("output\\s+\"[^\"]+\"\\s+enabled=#true\\b.*?\\bscale\\s+(\\d+(?:\\.\\d+)?)",
           "cosmic-randr", "list", "--kdl"),
    UNKNOWN(null);

    private final String pattern;
    private final String[] scaleFactorCalls;
    private static DeType instance;
    private static double scaleFactor;

    DeType(String pattern, String ... scaleFactorCalls) {
        this.pattern = pattern;
        this.scaleFactorCalls = scaleFactorCalls;
    }

    static DeType getDeType() {
        if(instance == null) {
            for(DeType deType : DeType.values()) {
                if(deType.pattern == null) {
                    continue;
                }
                Pattern pattern = Pattern.compile(deType.pattern);
                Matcher matcher = pattern.matcher(ShellUtilities.executeRawSilently(deType.scaleFactorCalls));
                if (matcher.find()) {
                    try {
                        scaleFactor = Double.parseDouble(matcher.group(1));
                        instance = deType;
                    }
                    catch(NumberFormatException ignore) {}
                }
            }
        }
        return instance == null ? UNKNOWN : instance;
    }

    static double getScaleFactor() {
        return scaleFactor;
    }
}
