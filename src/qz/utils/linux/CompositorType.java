package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    KWIN6("^.*?(\\d+(?:\\.\\d+)?).*$",
         "kreadconfig6", "--file", "kdeglobals", "--group", "KScreen", "--key", "ScaleFactor"),

    KWIN5("^.*?(\\d+(?:\\.\\d+)?).*$",
          "kreadconfig5", "--file", "kdeglobals", "--group", "KScreen", "--key", "ScaleFactor"),

    MATE("^.*?(\\d+(?:\\.\\d+)?).*$",
         "gsettings", "get", "org.mate.interface", "window-scaling-factor"),

    MUFFIN("\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
           "gdbus", "call", "--session",
           "--dest", "org.cinnamon.Muffin.DisplayConfig",
           "--object-path", "/org/cinnamon/Muffin/DisplayConfig",
           "--method", "org.cinnamon.Muffin.DisplayConfig.GetCurrentState"),

    MUTTER("(?:uint32\\s+)?(\\d+(?:\\.\\d+)?)",
           "gsettings", "get", "org.gnome.desktop.interface", "scaling-factor"),

    XFCE("^.*?(\\d+(?:\\.\\d+)?).*$", "xfconf-query", "-c", "xsettings", "-p", "/Gdk/WindowScalingFactor"),

    UNKNOWN(null);

    private static final Logger log = LogManager.getLogger(CompositorType.class);
    private final String pattern;
    private final String[] scaleFactorCalls;
    private static CompositorType[] compositors;

    CompositorType(String pattern, String ... scaleFactorCalls) {
        this.pattern = pattern;
        this.scaleFactorCalls = scaleFactorCalls;
    }

    static double getBestScaleFactor(boolean isSilent) {
        double scaleFactor = 0;
        for(CompositorType compositorType : CompositorType.getCompositors()) {
            log.info("Trying to get scale factor using {}", compositorType);
            if((scaleFactor = compositorType.getScaleFactor(isSilent)) != 0) {
                break;
            }
        }
        return scaleFactor;
    }

    private double getScaleFactor(boolean isSilent) {
        if(this != UNKNOWN && this.pattern != null) {
            Pattern pattern = Pattern.compile(this.pattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(ShellUtilities.executeRaw(this.scaleFactorCalls, isSilent));
            if (matcher.find() && matcher.groupCount() > 0) {
                try {
                    return Double.parseDouble(matcher.group(1));
                }
                catch(NumberFormatException ignore) {}
            }
        }
        return 0.0;
    }

    public static CompositorType[] getCompositors() {
        if(compositors == null) {
            compositors = switch(LinuxUtilities.getDesktopEnvironment()) {
                case CINNAMON -> setCompositors(MUFFIN);
                case COSMIC -> setCompositors(COSMIC);
                case HYPRLAND -> setCompositors(HYPRLAND);
                case KDE -> setCompositors(KWIN6, KWIN5, MUTTER);
                case MATE ->  setCompositors(MATE);
                case XFCE -> setCompositors(XFCE);
                default -> setCompositors(MUTTER); // always fallback to gsettings
            };
        }
        return compositors;
    }

    static CompositorType[] setCompositors(CompositorType ... compositors) {
        return CompositorType.compositors = compositors;
    }
}
