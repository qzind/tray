package qz.installer.apps;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.locator.AppInfo;
import qz.installer.apps.locator.AppLocator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Locate all apps registered as <code>AppAlias</code>
 * This will spawn a bunch an instance of any app found.
 * Set <code>SKIP_BROWSER_SPAWN=true</code> to skip spawning apps.
 */
public class AppLocatorTests {
    private static final Logger log = LogManager.getLogger(AppLocatorTests.class);

    private final HashMap<AppAlias,HashSet<AppInfo>> foundApps = new HashMap<>();
    private static final boolean SKIP_BROWSER_SPAWN = false;

    @BeforeClass
    public void setup() {
        for (AppAlias app : AppAlias.values()) {
            foundApps.put(app, AppLocator.getInstance().locate(app));
        }
    }

    @DataProvider(name = "apps")
    public Object[][] apps() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppAlias alias : AppAlias.values()) {
            retMatrix.add(new Object[]{alias});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "apps", priority = 1)
    public void findAppTests(AppAlias app) {
        Assert.assertFalse(foundApps.get(app).isEmpty());

        // Make sure the app exists and we found version information
        for (AppInfo appInfo : foundApps.get(app)) {
            log.info("[{}] found as '{}' at '{}', Version: '{}'", app.name(), appInfo.getName(true), appInfo.getPath(), appInfo.getVersion());
            Assert.assertFalse(appInfo.getAlias().getName().isBlank());
            Assert.assertTrue(appInfo.getPath().toFile().exists());
            Assert.assertTrue(appInfo.getExePath().toFile().exists());
            Assert.assertNotEquals(appInfo.getVersion(), Version.parse("0.0.0"));
        }
    }

    @Test(dataProvider = "apps", priority = 2)
    public void stoppedAppTests(AppAlias app) {
        // A developer's workstation will likely have at least one app started
        // but extremely unlikely to have all apps started.  Pass if
        // at least one app is not started.
        int foundAppCount = foundApps.get(app).size();
        int runningAppCount = 0;
        HashSet<Path> runningPaths = AppLocator.getRunningPaths(foundApps.get(app));
        for (AppInfo appInfo : foundApps.get(app)) {
            for(Path runningPath : runningPaths) {
                if(appInfo.getExePath().equals(runningPath)) {
                    log.info("[{}] found running as '{}'", app, appInfo.getExePath());
                    runningAppCount++;
                }
            }
        }
        Assert.assertTrue(runningAppCount < foundAppCount);
    }

    @Test(dataProvider = "apps", priority = 3)
    public void startAppTests(AppAlias app) {
        if(SKIP_BROWSER_SPAWN) throw new SkipException("Skipping per request");
        for (AppInfo appInfo : foundApps.get(app)) {
            log.info("[{}] spawning from '{}'", app.name(), appInfo.getExePath());
            Assert.assertTrue(spawnProcess(appInfo.getExePath()));
        }
    }

    @Test(dataProvider = "apps", priority = 4)
    public void findRunningAppsTests(AppAlias app) {
        if(SKIP_BROWSER_SPAWN) throw new SkipException("Skipping per request");
        HashSet<Path> runningPaths = AppLocator.getRunningPaths(foundApps.get(app));
        outer:
        for (AppInfo appInfo : foundApps.get(app)) {
            for(Path runningPath : runningPaths) {
                if(appInfo.getExePath().equals(runningPath)) {
                    log.info("[{}] found running at '{}'", app, appInfo.getExePath());
                    continue outer;
                }
            }
            Assert.fail(String.format("Unable to find a running [%s] process matching '%s'", app, appInfo.getExePath()));
        }
    }

    private static boolean spawnProcess(Path p) {
        try {
            if(!p.toFile().exists()) {
                throw new IOException(p + " does not exist");
            }
            ProcessBuilder pb = new ProcessBuilder(p.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            pb.start();
        } catch (IOException e) {
            log.error("Failed to start process {}", p, e);
            return false;
        }
        return true;
    }
}
