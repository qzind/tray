package qz.installer.apps;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.locator.AppInfo;
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

    private final HashMap<AppAlias,HashSet<AppInfo>> appInfoCache = new HashMap<>();
    private static final boolean SKIP_APP_SPAWN = false;

    /**
     * Lazy init allows first caller to provide accurate benchmarking values to TestNG
     */
    public HashSet<AppInfo> findAppInfo(AppAlias app) {
        if(appInfoCache.get(app) == null) {
            appInfoCache.put(app, AppLocator.getInstance().locate(app));
        }
        return appInfoCache.get(app);
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
        Assert.assertFalse(findAppInfo(app).isEmpty());

        // Make sure the app exists and we found version information
        for (AppInfo appInfo : findAppInfo(app)) {
            log.info("[{}] found as '{}' at '{}' installed to '{}', Version: '{}'", app.name(), appInfo.getName(true), appInfo.getExePath(), appInfo.getAppPath(), appInfo.getVersion());
            Assert.assertFalse(appInfo.getAlias().getName().isBlank());
            Assert.assertTrue(appInfo.getAppPath().toFile().exists());
            Assert.assertTrue(appInfo.getExePath().toFile().exists());
            Assert.assertNotEquals(appInfo.getVersion(), Version.parse("0.0.0"));
        }
    }

    @Test(dataProvider = "apps", priority = 2)
    public void stoppedAppTests(AppAlias app) {
        // A developer's workstation will likely have at least one app started
        // but extremely unlikely to have all apps started.  Pass if
        // at least one app is not started.
        int foundAppCount = findAppInfo(app).size();
        HashMap<String, AppInfo> runningApps = AppLocator.getInstance().getRunningApps(findAppInfo(app), null);
        for(Map.Entry<String, AppInfo> runningApp : runningApps.entrySet()) {
            String pid = runningApp.getKey();
            AppInfo appInfo = runningApp.getValue();
            log.info("[{}] found running as pid {} at '{}'", app, pid, appInfo.getExePath());
        }
        // An app can have multiple processes, dedupe
        HashSet<AppInfo> uniqueApps = new HashSet<>(runningApps.values());
        Assert.assertTrue(uniqueApps.size() < foundAppCount);
    }

    @Test(dataProvider = "apps", priority = 3)
    public void startAppTests(AppAlias app) {
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        for (AppInfo appInfo : findAppInfo(app)) {
            log.info("[{}] spawning from '{}'", app.name(), appInfo.getExePath());
            Assert.assertTrue(spawnProcess(appInfo));
        }
    }

    @Test(dataProvider = "apps", priority = 4)
    public void waitForApps(AppAlias app) {
        int ms = 200;
        boolean slept = true;
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        for (AppInfo ignored : findAppInfo(app)) {
            // Give each app 200ms to wake up
            try {
                Thread.sleep(ms);
            } catch(InterruptedException e) {
                slept = false;
            }
            Assert.assertTrue(slept, "Slept for " + ms + "ms");
        }
    }

    @Test(dataProvider = "apps", priority = 5)
    public void findRunningAppsTests(AppAlias app) {
        if(SKIP_APP_SPAWN) throw new SkipException("Skipping per request");
        HashMap<String, AppInfo> runningApps = AppLocator.getInstance().getRunningApps(findAppInfo(app), null);
        for(Map.Entry<String, AppInfo> runningApp : runningApps.entrySet()) {
            String pid = runningApp.getKey();
            AppInfo appInfo = runningApp.getValue();
            log.info("[{}] found running as pid {} at '{}'", app, pid, appInfo.getExePath());
        }
        Assert.assertFalse(runningApps.isEmpty(), String.format("Unable to find a running [%s] process.  Running app count must NOT be empty.", app.name()));
    }

    /**
     * TODO: Move this somewhere useful and combine with Installer.getInstance().spawn();
     */
    private static boolean spawnProcess(AppInfo appInfo) {
        String[] command = appInfo.getExeCommand();
        try {
            if(!appInfo.exists()) {
                throw new IOException(appInfo.getExePath() + " or " + appInfo.getAppPath() + " does not exist");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            pb.start();
        } catch (IOException e) {
            log.error("Failed to start process '{}'", String.join(", ", command), e);
            return false;
        }
        return true;
    }
}
