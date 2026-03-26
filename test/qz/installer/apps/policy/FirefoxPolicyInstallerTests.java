package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static qz.installer.apps.policy.PolicyState.Type.*;

public class FirefoxPolicyInstallerTests{
    static ArrayList<Object[]> firefoxTests = new ArrayList<>();

    static {
        if (SystemUtilities.isLinux()) {
            //arrays in maps are only supported on linux
            firefoxTests.add(new Object[] {MAP, "Certificates", new Object[] {"Install", new Object[] {new File("./test.crt").toString()}}});
        } else {
            firefoxTests.add(new Object[] {MAP, "Certificates", new Object[] {"ImportEnterpriseRoots", true}});
        }
    }

    @DataProvider(name = "firefoxPolicyTests")
    public Object[][] firefoxPolicyTests() {
        ArrayList<AppFamily.AppVariant> testAppVariants = new ArrayList<>();
        Collections.addAll(testAppVariants, AppFamily.FIREFOX.getVariants());
        return PolicyInstallerTestDispatcher.constructTestMatrix(firefoxTests.toArray(new Object[0][]), testAppVariants);
    }

    @Test(dataProvider = "firefoxPolicyTests")
    public void testGenericPolicies(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(appVariant, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(appVariant, type, name, value);
    }
}
