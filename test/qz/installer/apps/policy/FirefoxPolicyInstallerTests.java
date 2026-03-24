package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;

import java.util.ArrayList;
import java.util.Collections;

public class FirefoxPolicyInstallerTests{
    private static final Logger log = LogManager.getLogger(FirefoxPolicyInstallerTests.class);

    static Object[][] firefoxTests = {
            //todo
    };

    @DataProvider(name = "firefoxPolicyTests")
    public Object[][] firefoxPolicyTests() {
        ArrayList<AppAlias.Alias> testAliases = new ArrayList<>();
        Collections.addAll(testAliases, AppAlias.FIREFOX.getAliases());
        return PolicyInstallerTestDispatcher.constructTestMatrix(firefoxTests, testAliases);
    }

    @Test(dataProvider = "firefoxPolicyTests")
    public void testFirefoxPolicies(AppAlias.Alias alias, String name, Object value, PolicyState.Type type) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, name, value, type);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, name, value, type);
    }
}
