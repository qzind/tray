package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static qz.installer.apps.policy.PolicyState.Type.*;

public class FirefoxPolicyInstallerTests{
    private static final Logger log = LogManager.getLogger(FirefoxPolicyInstallerTests.class);

    static Object[][] firefoxTests = {
            {MAP, "Certificates", new Object[] {"ImportEnterpriseRoots", true}},
            {MAP, "Certificates", new Object[] {"Install", new Object[] { new File("./test.crt").toString() }}}
    };

    @DataProvider(name = "firefoxPolicyTests")
    public Object[][] firefoxPolicyTests() {
        ArrayList<AppFamily.AppVariant> testAppVariants = new ArrayList<>();
        Collections.addAll(testAppVariants, AppFamily.FIREFOX.getVariants());
        return PolicyInstallerTestDispatcher.constructTestMatrix(firefoxTests, testAppVariants);
    }

    @Test(dataProvider = "firefoxPolicyTests")
    public void testGenericPolicies(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(appVariant, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(appVariant, type, name, value);
    }
}
