package qz.installer.provision.invoker;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.provision.Step;
import qz.build.provision.params.Type;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public abstract class InvokableResource implements Invokable {
    static final Logger log = LogManager.getLogger(InvokableResource.class);

    public static File dataToFile(Step step) throws IOException {
        Path resourcePath = Paths.get(step.getData());
        if(resourcePath.isAbsolute() || step.usingPath()) {
            return pathResourceToFile(step);
        }
        if(step.usingClass()) {
            return classResourceToFile(step);
        }
        return null;
    }

    /**
     * Resolves the resource directly from file
     */
    private static File pathResourceToFile(Step step) {
        String resourcePath = step.getData();
        Path dataPath = Paths.get(resourcePath);
        return dataPath.isAbsolute() ? dataPath.toFile() : step.getRelativePath().resolve(resourcePath).toFile();
    }

    /**
     * Copies resource from JAR to a temp file for use in installation
     */
    private static File classResourceToFile(Step step) throws IOException {
        // Resource may be inside the jar
        InputStream in = step.getRelativeClass().getResourceAsStream("resources/" + step.getData());
        if(in == null) {
            log.warn("Resource '{}' is missing, skipping step", step.getData());
            return null;
        }
        String suffix = "_" + Paths.get(step.getData()).getFileName().toString();
        File destination = File.createTempFile(Constants.DATA_DIR + "_provision_", suffix);
        Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        IOUtils.closeQuietly(in);

        // Set scripts executable
        if(step.getType() == Type.SCRIPT && !SystemUtilities.isWindows()) {
            destination.setExecutable(true, false);
        }
        return destination;
    }
}
