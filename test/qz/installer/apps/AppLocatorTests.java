package qz.installer.apps;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.locator.ResolvedApp;
import qz.installer.apps.locator.AppLocator;

import java.io.IOException;
import java.util.*;

/**
 * Locate all apps registered as <code>AppAlias</code>
 * This will spawn a bunch an instance of any app found.
 * Set <code>SKIP_APP_SPAWN=true</code> to skip spawning apps.
 */
public class AppLocatorTests {
    private static final Logger log = LogManager.getLogger(AppLocatorTests.class);

    private final Installer installer = Installer.getInstance();
    private final HashMap<AppFamily,HashSet<ResolvedApp>> resolvedAppsCache = new HashMap<>();
    private static final boolean SKIP_APP_SPAWN = false;

    /**
     * Lazy init allows first caller to provide accurate benchmarking values to TestNG
     */
    public HashSet<ResolvedApp> findResolvedApps(AppFamily app) {
        if(resolvedAppsCache.get(app) == null) {
            resolvedAppsCache.put(app, AppLocator.getInstance().locate(app));
        }
        return resolvedAppsCache.get(app);
    }

    @DataProvider(name = "apps")
    public Object[][] apps() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppFamily alias : AppFamily.values()) {
            retMatrix.add(new Object[]{alias});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "apps", priority = 1)
    public void findAppTests(AppFamily app) {
        Assert.assertFalse(findResolvedApps(app).isEmpty());

        // Make sure the app exists and we found version information
        for (ResolvedApp resolvedApp : findResolvedApps(app)) {
            log.info("[{}] found as '{}' at '{}' installed to '{}', Version: '{}'", app.name(), resolvedApp.getName(true), resolvedApp.getExePath(), resolvedApp.getAppPath(), resolvedApp.getVersion());
            Assert.assertFalse(resolvedApp.getAlias().getName().isBlank());
            Assert.assertTrue(resolvedApp.getAppPath().toFile().exists());
            Assert.assertTrue(resolvedApp.getExePath().toFile().exists());
            Assert.assertNotEquals(resolvedApp.getVersion(), Version.parse("0.0.0"));
        }
    }

    @Test(dataProvider = "apps", priority = 2)
    public void stoppedAppTests(AppFamily app) {
        // A developer's workstation will likely have at least one app started
        // but extremely unlikely to have all apps started.  Pass if
        // at least one app is not started.
        int foundAppCount = findResolvedApps(app).size();
        HashMap<String,ResolvedApp> runningApps = AppLocator.getInstance().getRunningApps(findResolvedApps(app), null);
        for(Map.Entry<String,ResolvedApp> runningApp : runningApps.entrySet()) {
            String pid = runningApp.getKey();
            ResolvedApp resolvedApp = runningApp.getValue();
            log.info("[{}] found running as pid {} at '{}'", app, pid, resolvedApp.getExePath());
        }
        // An app can have multiple processes, dedupe
        HashSet<ResolvedApp> uniqueApps = new HashSet<>(runningApps.values());
        Assert.assertTrue(uniqueApps.size() < foundAppCount);
    }

    @Test(dataProvider = "apps", priority = 3)
    public void startAppTests(AppFamily app) {
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        for (ResolvedApp resolvedApp : findResolvedApps(app)) {
            log.info("[{}] Spawning '{}' from '{}'", app.name(), resolvedApp.getName(true), Arrays.toString(resolvedApp.getExeCommand()));
            try {
               installer.spawn(resolvedApp);
               assert(true);
            } catch(Exception e) {
               assert(false);
            }
        }
    }

    @Test(dataProvider = "apps", priority = 4)
    public void waitForApps(AppFamily app) {
        int ms = 200;
        boolean slept = true;
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        for (ResolvedApp ignored : findResolvedApps(app)) {
            // Give each app time to start
            try {
                Thread.sleep(ms);
            } catch(InterruptedException e) {
                slept = false;
            }
            Assert.assertTrue(slept, "Slept for " + ms + "ms");
        }
    }

    @Test(dataProvider = "apps", priority = 5)
    public void findRunningAppsTests(AppFamily app) {
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        HashMap<String,ResolvedApp> runningApps = AppLocator.getInstance().getRunningApps(findResolvedApps(app), null);
        for(Map.Entry<String,ResolvedApp> runningApp : runningApps.entrySet()) {
            String pid = runningApp.getKey();
            ResolvedApp resolvedApp = runningApp.getValue();
            log.info("[{}] found running as pid {} at '{}'", app, pid, resolvedApp.getExePath());
        }
        int runningCount = new HashSet<>(runningApps.values()).size();
        int foundCount = findResolvedApps(app).size();
        Assert.assertEquals(foundCount, runningCount, String.format("[%s] Running app count %s must be equal to found app count %s.", app.name(), runningCount, foundCount));
    }
}
