package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;

import java.util.ArrayList;
import java.util.Collections;

import static qz.installer.apps.policy.PolicyState.Type.*;

public class FirefoxPolicyInstallerTests{
    private static final Logger log = LogManager.getLogger(FirefoxPolicyInstallerTests.class);

    static Object[][] firefoxTests = {
            {MAP, "Certificates", new Object[] {"ImportEnterpriseRoots", true}},
    };

    @DataProvider(name = "firefoxPolicyTests")
    public Object[][] firefoxPolicyTests() {
        ArrayList<AppAlias.Alias> testAliases = new ArrayList<>();
        Collections.addAll(testAliases, AppAlias.FIREFOX.getAliases());
        return PolicyInstallerTestDispatcher.constructTestMatrix(firefoxTests, testAliases);
    }

    @Test(dataProvider = "firefoxPolicyTests")
    public void testGenericPolicies(AppAlias.Alias alias, PolicyState.Type type, String name, Object value) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, type, name, value);
    }
}
