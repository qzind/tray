package qz.utils.linux;

import qz.utils.linux.gtk.GtkTheme;
import qz.utils.linux.kde.KdeTheme;

import java.util.Arrays;

public class DeUtils {
    private static final DeType deType = getDeType();

    /**
     * Uses a live desktop-environment-specific technique determine if the Desktop is "dark mode".
     */
    public static boolean isDarkDesktop() {
        if (getDeTheme() == null) {
            return false;
        }
        return getDeTheme().isDarkDesktop();
    }

    public static DeTheme getDeTheme() {
        return getDeTheme(getDeType());
    }

    static DeTheme getDeTheme(DeType deType) {
        return switch (deType) {
            case KDE ->  new KdeTheme();
            case GNOME -> new GtkTheme();
            default -> null;
        };
    }

    public static DeType getDeType() {
        if(deType != null) {
            return deType;
        }
        return Arrays.stream(DeType.values())
                .filter(DeType::isLikely)
                .findFirst()
                .orElse(DeType.UNKNOWN);
    }
}
