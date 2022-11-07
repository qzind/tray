package qz.utils;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class LinuxUtilities {
    private static final Logger log = LogManager.getLogger(LinuxUtilities.class);
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };

    private static String FOUND_ELEVATOR = null;

    private static String findElevator() throws IOException {
        if(FOUND_ELEVATOR == null) {
            for(String elevator : KNOWN_ELEVATORS) {
                if (ShellUtilities.execute("which", elevator)) {
                    FOUND_ELEVATOR = elevator;
                    break;
                }
            }
            throw new IOException("Can't find an installed utility " + Arrays.toString(KNOWN_ELEVATORS) + " to elevate permissions.");
        }
        return FOUND_ELEVATOR;
    }

    public static boolean elevatedFileCopy(Path source, Path destination) {
        // Don't prompt if it's not needed
        try {
            // Note: preserveFileDate=false per https://github.com/qzind/tray/issues/1011
            FileUtils.copyFile(source.toFile(), destination.toFile(), false);
            return true;
        } catch(IOException ignore) {}

        try {
            String[] command = {findElevator(), "cp", source.toString(), destination.toString()};
            return ShellUtilities.execute(command);
        } catch(IOException io) {
            log.error("Copy failed.  You'll have do this manually.", io);
        }
        return false;
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
