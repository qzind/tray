package qz.installer.apps.policy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.common.Constants;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.ArrayList;
import java.util.Collections;

import static qz.common.Constants.ABOUT_URL;
import static qz.installer.apps.policy.PolicyState.Type.*;

public class ChromiumPolicyInstallerTests {
    static ArrayList<Object[]> chromiumTests = new ArrayList<>();

    static {
        chromiumTests.add(new Object[]{ARRAY, "URLAllowlist", new Object[] {String.format("%s://*", Constants.DATA_DIR)}});
        chromiumTests.add(new Object[]{ARRAY, "LocalNetworkAccessAllowedForUrls", "[*.]" + SystemUtilities.parseRootDomain(ABOUT_URL)});
    }

    @DataProvider(name = "chromiumPolicyTests")
    public Object[][] chromiumPolicyTests() {
        ArrayList<AppFamily.AppVariant> testAppVariants = new ArrayList<>();
        Collections.addAll(testAppVariants, AppFamily.CHROMIUM.getVariants());
        return PolicyInstallerTestDispatcher.constructTestMatrix(chromiumTests, testAppVariants);
    }

    @Test(dataProvider = "chromiumPolicyTests")
    public void testGenericPolicies(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(appVariant, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(appVariant, type, name, value);
    }
}
