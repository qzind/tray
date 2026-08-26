package qz.utils.kde;

import qz.utils.ShellUtilities;
import qz.utils.UnixUtilities;

public class KdeUtilities {
    public static boolean isDarkDesktop() {
        // Get background color in r,g,b
        String raw = ShellUtilities.executeRaw(UnixUtilities.DesktopEnvironment.binaryFound, "--file", "kdeglobals", "--group", "Colors:Window", "--key", "BackgroundNormal");
        String[] parts = raw.trim().split(",");

        // Apply luminance calculation
        if(parts.length == 3) {
            try {
                double luminance =
                        Integer.parseInt(parts[0]) * 0.2126 + // red
                                Integer.parseInt(parts[1]) * 0.7152 + // green
                                Integer.parseInt(parts[2]) * 0.0722; // blue
                // Return true if luminance is below the "dark" threshold
                return luminance < 128;
            } catch(Exception ignore) {}
        }
        return false;
    }
}
