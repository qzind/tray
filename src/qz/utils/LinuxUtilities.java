package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class LinuxUtilities {
    private static final Logger log = LogManager.getLogger(LinuxUtilities.class);

    private static String foundElevator = null;
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };

    public static boolean elevatedFileCopy(Path source, Path destination) {
        // Don't prompt if it's not needed
        try {
            // Note: preserveFileDate=false per https://github.com/qzind/tray/issues/1011
            FileUtils.copyFile(source.toFile(), destination.toFile(), false);
            return true;
        } catch(IOException ignore) {}
        if(foundElevator == null) {
            for(String elevator : KNOWN_ELEVATORS) {
                if(ShellUtilities.execute("which", elevator)) {
                    foundElevator = elevator;
                }
            }
        }
        if(foundElevator == null) {
            log.error("Can't find an installed utility to elevate permissions.  You'll have to do this step manually.");
            return false;
        }

        String[] command = {foundElevator, "cp", source.toString(), destination.toString()};
        return ShellUtilities.execute(command);
    }

    /**
     * Runs a shell command to determine if "Dark" desktop theme is enabled
     * @return true if enabled, false if not
     */
    public static boolean isDarkMode() {
        return !ShellUtilities.execute(new String[] { "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme" }, new String[] { "dark" }, true, true).isEmpty();
    }

    public static double getScaleFactor() {
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("11.0.0"))) {
            return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        }
        return GtkUtilities.getScaleFactor();
    }
}
