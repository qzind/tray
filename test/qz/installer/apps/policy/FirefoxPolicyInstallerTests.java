package qz.installer.apps.policy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.List;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.policy.PolicyInstaller.PolicyLocator.*;

public class FirefoxPolicyInstallerTests extends PolicyTestDispatcher {
    @DataProvider(name = "firefoxPoliciesData")
    public Object[][] firefoxPoliciesData() {
        Object[][] tests;
        if (SystemUtilities.isLinux()) {
            tests = new Object[][] {
                    // arrays in maps are only supported on linux
                    { AppType.DEFAULT, MAP, "Certificates", new Object[] {"Install", new Object[] {new File("policy-testing.crt").toString() } } },
                    { AppType.FLATPAK, MAP, "Certificates", new Object[] {"Install", new Object[] {new File("policy-testing.crt").toString() } } }
            };
        } else {
            tests = new Object[][] {
                    { AppType.DEFAULT, MAP, "Certificates", new Object[] {"ImportEnterpriseRoots", true } }
            };
        }
        return PolicyTestDispatcher.addAppVariants(List.of(tests), AppFamily.FIREFOX);
    }

    @Test(dataProvider = "firefoxPoliciesData")
    public void testFirefoxPolicies(AppFamily.AppVariant appVariant, AppType appType, PolicyState.Type type, String name, Object value) {
        testAppsPolicyInstall(appVariant, appType, type, name, value);
        testAppsPolicyUninstall(appVariant, appType, type, name, value);
    }
}
