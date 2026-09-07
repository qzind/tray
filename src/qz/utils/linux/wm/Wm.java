package qz.utils.linux.wm;

import qz.utils.linux.LinuxUtilities;

import java.util.function.Supplier;

/**
 * Map of Linux window managers to their respective CLI dispatchers for obtaining environment-specific
 * information such as scale factor or dark/light desktop themes
 */
public enum Wm {
    COSMIC(Cosmic::new),
    HYPRLAND(Hyprland::new),
    KWIN(Kwin::new),
    MATE(Mate::new),
    MUFFIN(Muffin::new),
    MUTTER(Mutter::new),
    XFCE(Xfce::new),
    UNKNOWN(Unknown::new);

    private static Wm wm;
    private static Dispatcher dispatcher;

    private final Supplier<Dispatcher> supplier;

    Wm(Supplier<Dispatcher> supplier) {
        this.supplier = supplier;
    }

    public static Wm detectWm() {
        if(wm == null) {
            wm = switch(LinuxUtilities.getDesktopEnvironment()) {
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
        return wm;
    }

    public static Dispatcher getDispatcher() {
        if(dispatcher == null) {
            dispatcher = detectWm().supplier.get();
        }
        return dispatcher;
    }
}
