package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;

public class DeUtils {
    private static final Logger log = LogManager.getLogger(DeUtils.class);

    /**
     * Uses a live desktop-environment-specific technique determine if the Desktop is "dark mode".
     */
    public static boolean isDarkDesktop() {
        String colorScheme = ShellUtilities.executeRawSilently("gdbus", "call", "--session",
                                          "--dest", "org.freedesktop.portal.Desktop",
                                          "--object-path", "/org/freedesktop/portal/desktop",
                                          "--method", "org.freedesktop.portal.Settings.ReadOne",
                                          "org.freedesktop.appearance", "color-scheme");

        try {
            // (<uint32 0>,)
            String isolated = colorScheme.trim().split("\\s+")[1].replaceAll("[>,)]+$", "");
            // 0: No preference, 1: Prefer dark, 2: Prefer light
            return Integer.parseInt(isolated) == 1;
        } catch(NumberFormatException | IndexOutOfBoundsException nfe) {
            log.warn("Unable to parse color scheme from '{}'", colorScheme, nfe);
        }
        return false;
    }
}
