package qz.utils.linux.compositor;

import qz.utils.linux.LinuxUtilities;
import qz.utils.linux.compositor.dispatcher.*;

import java.util.function.Supplier;

/**
 * Map of Linux compositors to their respective CLI dispatchers for obtaining environment-specific
 * information such as scale factor or dark/light desktop themes
 */
public enum Compositor {
    COSMIC(Cosmic::new),
    HYPRLAND(Hyprland::new),
    KWIN(Kwin::new),
    MATE(Mate::new),
    MUFFIN(Muffin::new),
    MUTTER(Mutter::new),
    XFCE(Xfce::new),
    UNKNOWN(Unknown::new);

    private static Compositor compositor;
    private static Dispatcher dispatcher;

    private final Supplier<Dispatcher> supplier;

    Compositor(Supplier<Dispatcher> supplier) {
        this.supplier = supplier;
    }

    public static Compositor detectCompositor() {
        if(compositor == null) {
            compositor = switch(LinuxUtilities.getDesktopEnvironment()) {
                case CINNAMON -> MUFFIN;
                case COSMIC -> COSMIC;
                case GNOME ->  MUTTER;
                case HYPRLAND -> HYPRLAND;
                case KDE -> KWIN;
                case MATE ->  MATE;
                case XFCE -> XFCE;
                default -> UNKNOWN;
            };
        }
        return compositor;
    }

    public static Dispatcher getDispatcher() {
        if(dispatcher == null) {
            dispatcher = detectCompositor().supplier.get();
        }
        return dispatcher;
    }
}
