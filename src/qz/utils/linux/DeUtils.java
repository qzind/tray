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
        return switch(deType) {
            case GNOME -> GtkTheme.isDarkDesktop();
            case KDE -> KdeTheme.isDarkDesktop();
            default -> false;
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
