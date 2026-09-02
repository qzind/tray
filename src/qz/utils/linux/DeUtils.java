package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;

public class DeUtils {
    private static final Logger log = LogManager.getLogger(DeUtils.class);
    private static boolean darkDesktopDetectionFailed = false;

    /**
     * Uses a live desktop-environment-specific technique determine if the Desktop is "dark mode".
     */
    public static boolean isDarkDesktop() {
        if(darkDesktopDetectionFailed) {
            return false;
        }

        String colorScheme = ShellUtilities.executeRawSilently("gdbus", "call", "--session",
                                          "--dest", "org.freedesktop.portal.Desktop",
                                          "--object-path", "/org/freedesktop/portal/desktop",
                                          "--method", "org.freedesktop.portal.Settings.ReadOne",
                                          "org.freedesktop.appearance", "color-scheme");

        try {
            // (<uint32 0>,)
            String[] parts = colorScheme.trim().split("\\s+");
            if(parts.length > 1) {
                String isolated = parts[1].replaceAll("[>,)]+$", "");
                // 0 = No preference; 1 = Prefer dark; 2 = Prefer light
                return Integer.parseInt(isolated) == 1;
            }
        } catch(NumberFormatException nfe) {
            log.warn("Unable to parse color scheme from '{}': {}", colorScheme, nfe.getMessage());
        }
        darkDesktopDetectionFailed = true;
        log.warn("Can't detect color scheme, defaulting to light; We won't try again until the app is restarted.");
        return false;
    }
}
