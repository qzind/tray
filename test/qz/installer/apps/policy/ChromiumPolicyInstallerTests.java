package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;

import java.util.ArrayList;
import java.util.Collections;

public class ChromiumPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(ChromiumPolicyInstallerTests.class);

    static Object[][] chromiumTests = {
            //todo
    };

    @DataProvider(name = "chromiumPolicyTests")
    public Object[][] chromiumPolicyTests() {
        ArrayList<AppAlias.Alias> testAliases = new ArrayList<>();
        Collections.addAll(testAliases, AppAlias.CHROMIUM.getAliases());
        return PolicyInstallerTestDispatcher.constructTestMatrix(chromiumTests, testAliases);
    }

    @Test(dataProvider = "chromiumPolicyTests")
    public void testChromiumPolicies(AppAlias.Alias alias, String name, Object value, PolicyState.Type type) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, name, value, type);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, name, value, type);
    }
}
