package qz.utils.linux;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.UnixUtilities;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class Elevator {
    private static final Logger log = LogManager.getLogger(Elevator.class);
    private static final String[] KNOWN_ELEVATORS = {"pkexec", "gksu", "gksudo", "kdesudo" };

    private static String foundElevator;

    private static String findElevator() throws IOException {
        if(foundElevator == null) {
            for(String elevator : KNOWN_ELEVATORS) {
                if (ShellUtilities.execute("which", elevator)) {
                    foundElevator = elevator;
                    break;
                }
            }
            if(foundElevator == null) {
                throw new IOException("Can't find an installed utility " + Arrays.toString(KNOWN_ELEVATORS) + " to elevate permissions.");
            }
        }
        return foundElevator;
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
}
