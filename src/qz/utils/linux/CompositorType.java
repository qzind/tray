package qz.utils.linux;

import qz.utils.ShellUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for identifying a Desktop environment in Linux or a Linux-like system by
 * querying dbus or other cli tools
 */
public enum CompositorType {
    COSMIC("output\\s+\"[^\"]+\"\\s+enabled=#true\\b.*?\\bscale\\s+(\\d+(?:\\.\\d+)?)",
           "cosmic-randr", "list", "--kdl"),

    HYPRLAND("(?s)Monitor\\s+.*?\\bscale:\\s*([0-9.]+)",
            "hyprctl", "monitors"),

    KWIN("Enabled:\\s*1.*?Scale:\\s*(\\d+(?:\\.\\d+)?)",
         "gdbus", "call", "--session",
         "--dest" , "org.kde.KWin",
         "--object-path", "/KWin",
         "--method", "org.kde.KWin.supportInformation"),

    MATE(".*",
         "gsettings", "get", "org.mate.interface", "window-scaling-factor"),

    MUFFIN("\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
           "gdbus", "call", "--session",
           "--dest", "org.cinnamon.Muffin.DisplayConfig",
           "--object-path", "/org/cinnamon/Muffin/DisplayConfig",
           "--method", "org.cinnamon.Muffin.DisplayConfig.GetCurrentState"),

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

    UNKNOWN(null);

    private final String pattern;
    private final String[] scaleFactorCalls;
    private static CompositorType instance;

    CompositorType(String pattern, String ... scaleFactorCalls) {
        this.pattern = pattern;
        this.scaleFactorCalls = scaleFactorCalls;
    }

    public double getScaleFactor() {
        return getScaleFactor(true);
    }

    double getScaleFactor(boolean isSilent) {
        if(this.pattern != null) {
            Pattern pattern = Pattern.compile(this.pattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(ShellUtilities.executeRaw(this.scaleFactorCalls, isSilent));
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                }
                catch(NumberFormatException ignore) {}
            }
        }
        return 0.0;
    }

    static CompositorType getDe() {
        if(instance == null) {
            switch(LinuxUtilities.getDesktopEnvironment()) {
                case CINNAMON -> instance = MUFFIN;
                case COSMIC -> instance = COSMIC;
                case GNOME ->  instance = MUTTER;
                case HYPRLAND -> instance = HYPRLAND;
                case KDE -> instance = KWIN;
                case MATE ->  instance = MATE;
                case XFCE -> instance = XFCE;
                case UNKNOWN -> instance = UNKNOWN;
            }
        }
        return instance == null ? UNKNOWN : instance;
    }
}
