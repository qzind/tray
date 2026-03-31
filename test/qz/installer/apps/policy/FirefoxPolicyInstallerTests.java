package qz.installer.apps.policy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.List;

import static qz.installer.apps.policy.PolicyState.Type.*;

public class FirefoxPolicyInstallerTests extends PolicyTestDispatcher {
    @DataProvider(name = "firefoxPoliciesData")
    public Object[][] firefoxPoliciesData() {
        Object[][] tests;
        if (SystemUtilities.isLinux()) {
            tests = new Object[][] {
                    // arrays in maps are only supported on linux
                    { MAP, "Certificates", new Object[] {"Install", new Object[] {new File("policy-testing.crt").toString() } } },
            };
        } else {
            tests = new Object[][] {
                    { MAP, "Certificates", new Object[] {"ImportEnterpriseRoots", true } }
            };
        }
        return PolicyTestDispatcher.addAppVariants(List.of(tests), AppFamily.FIREFOX);
    }

    @Test(dataProvider = "firefoxPoliciesData")
    public void testFirefoxPolicies(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        runTests(true, true, appVariant, type, name, value);
    }
}
