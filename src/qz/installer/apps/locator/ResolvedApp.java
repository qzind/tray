package qz.installer.apps.locator;

import com.github.zafarkhaja.semver.Version;

import java.awt.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.AppVersionParser;
import qz.installer.apps.locator.AppFamily.AppVariant;

/**
 * Container class for installed app information
 */
public class ResolvedApp {
    private static final Logger log = LogManager.getLogger(ResolvedApp.class);

    private final AppVariant appVariant;
    private final Path appPath;
    private final Path exePath;
    private final Version version;
    private final String[] exeCommand;

    public ResolvedApp(AppVariant appVariant, Path appPath, Path exePath, Version version, String ... exeParams) {
        this.appVariant = appVariant;
        this.appPath = appPath;
        this.exePath = exePath;
        this.version = version;
        this.exeCommand = constructCommand(appVariant, exePath, exeParams);
    }

    public ResolvedApp(AppVariant appVariant, Path appPath, Path exePath, String version, String ... exeParams) {
        this(appVariant, appPath, exePath, AppVersionParser.parse(version), exeParams);
    }

    public ResolvedApp(AppVariant appVariant, Path exePath, Version version) {
        this(appVariant, exePath.getParent(), exePath, version);
    }

    public ResolvedApp(AppVariant appVariant, Path appPath, Path exePath, String version) {
        this(appVariant, appPath, exePath, AppVersionParser.parse(version));
    }

    public AppVariant getAlias() {
        return appVariant;
    }

    public String getName(boolean stripVendor) {
        return appVariant.getName(stripVendor);
    }

    public Path getExePath() {
        return exePath;
    }

    public Path getAppPath() {
        return appPath;
    }

    public Version getVersion() {
        return version;
    }

    public String[] getExeCommand() {
        return exeCommand;
    }

    public boolean exists() {
        return exePath.toFile().exists() && appPath.toFile().exists();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof ResolvedApp && appPath != null) {
            return appPath.equals(((ResolvedApp)o).getAppPath());
        }
        return false;
    }

    /**
     * The command needed to start the application including parameters
     * e.g. { "/usr/bin/firefox" }, or  { "/usr/bin/flatpak", "run", "org.mozilla.firefox" }
     */
    public static String[] constructCommand(AppVariant appVariant, Path exePath, String ... exeParams) {
        List<String> command = new LinkedList<>();
        command.add(exePath.toString());
        command.addAll(List.of(exeParams));
        if(GraphicsEnvironment.isHeadless()) {
            switch(appVariant.getAppFamily()) {
                case FIREFOX:
                    command.add("-headless");
                    break;
                case CHROMIUM:
                    command.add("--headless");
                    break;
                default:
                    log.warn("No headless parameters configured for [{}] found as '{}' at '{}'; commands may terminate prematurely", appVariant.getAppFamily(), appVariant.getName(true), exePath);
            }
        }
        return command.toArray(new String[0]);
    }


    public static boolean issueRestartWarning(HashMap<String,ResolvedApp> runningPaths, ResolvedApp resolvedApp) {
        for(Map.Entry<String,ResolvedApp> runningApp : runningPaths.entrySet()) {
            if(runningApp.getValue().equals(resolvedApp)) {
                return runningApp.getValue().issueRestartWarning();
            }
        }
        return false;
    }

    public boolean issueRestartWarning() {
        try {
            if(this.getAlias().getAppFamily() == AppFamily.FIREFOX) {
                // TODO: Replace with "/restart" page
                Installer.getInstance().spawn(this.getExePath().toString(), "-private", "about:restartrequired");
                return true;
            }
            throw new UnsupportedOperationException(String.format("Restart pages are not yet supported for '%s'", this));
        } catch(Exception e) {
            log.warn("Unable to spawn '{}', will need to be restarted manually for changes to take effect", this);
        }
        return false;
    }

    @Override
    public String toString() {
        return appVariant + " " + appPath;
    }
}
