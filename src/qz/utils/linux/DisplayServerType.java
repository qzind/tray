package qz.utils.linux;

import qz.utils.ShellUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for identifying a Desktop environment in Linux or a Linux-like system by
 * querying dbus or other tools
 */
public enum DisplayServerType {
    KWIN("Enabled:\\s*1.*?Scale:\\s*(\\d+(?:\\.\\d+)?)",
         "gdbus", "call", "--session",
         "--dest" , "org.kde.KWin",
         "--object-path", "/KWin",
         "--method", "org.kde.KWin.supportInformation"),
    MUTTER("\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
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
    private static DisplayServerType instance;

    DisplayServerType(String pattern, String ... scaleFactorCalls) {
        this.pattern = pattern;
        this.scaleFactorCalls = scaleFactorCalls;
    }

    public double getScaleFactor() {
         if(this.pattern != null) {
            Pattern pattern = Pattern.compile(this.pattern);
            Matcher matcher = pattern.matcher(ShellUtilities.executeRawSilently(this.scaleFactorCalls));
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                }
                catch(NumberFormatException ignore) {}
            }
        }
        return 0.0;
    }

    static DisplayServerType getDeType() {
        if(instance == null) {
            switch(LinuxUtilities.getDesktopEnvironment()) {
                case KDE -> instance = KWIN;
                case GNOME ->  instance = MUTTER;
                case XFCE -> instance = XFCE;
                case COSMIC -> instance = COSMIC;
                case UNKNOWN -> instance = UNKNOWN;
            }
        }
        return instance == null ? UNKNOWN : instance;
    }
}
