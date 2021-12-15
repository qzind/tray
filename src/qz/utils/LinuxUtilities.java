package qz.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;

public class LinuxUtilities {
    private static final Logger log = LogManager.getLogger(LinuxUtilities.class);

    private static String foundElevator = null;
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };

    public static boolean elevatedFileCopy(Path source, Path destination) {
        // Don't prompt if it's not needed
        try {
            FileUtils.copyFile(source.toFile(), destination.toFile());
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
}
