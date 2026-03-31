package qz.installer.apps;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.locator.AppLocator;
import qz.installer.apps.locator.ResolvedApp;

import java.util.HashSet;

// sudo ant -Dtestng.pattern="**/RestartTest.class" testng</code>
public class FirefoxRestartWarningTests {
    private final boolean TEST_ENABLED = false;

    @Test
    public void firefoxRestartWarningLaunchesRestartPage() throws InterruptedException {
        if(!TEST_ENABLED) {
            throw new SkipException("Skipping");
        }

        HashSet<ResolvedApp> resolvedApps = AppLocator.getInstance().locate(AppFamily.FIREFOX);
        Assert.assertFalse(resolvedApps.isEmpty(), "No Firefox installs were found");

        for(ResolvedApp resolvedApp : resolvedApps) {
            Assert.assertTrue(
                    resolvedApp.issueRestartWarning(),
                    String.format("Failed to launch restart warning for '%s'", resolvedApp.getName(true))
            );
        }
        Thread.sleep(5000);
    }
}
