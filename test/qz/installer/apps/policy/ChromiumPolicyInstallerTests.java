package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.common.Constants;
import qz.installer.apps.locator.AppAlias;
import qz.utils.SystemUtilities;

import java.util.ArrayList;
import java.util.Collections;

import static qz.common.Constants.ABOUT_URL;
import static qz.installer.apps.policy.PolicyState.Type.*;

public class ChromiumPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(ChromiumPolicyInstallerTests.class);

    static Object[][] chromiumTests = {
            {ARRAY, "URLAllowlist", new Object[] {String.format("%s://*", Constants.DATA_DIR)}},
            {VALUE, "LocalNetworkAccessAllowedForUrls", "[*.]" + SystemUtilities.parseRootDomain(ABOUT_URL)},
    };

    @DataProvider(name = "chromiumPolicyTests")
    public Object[][] chromiumPolicyTests() {
        ArrayList<AppAlias.Alias> testAliases = new ArrayList<>();
        Collections.addAll(testAliases, AppAlias.CHROMIUM.getAliases());
        return PolicyInstallerTestDispatcher.constructTestMatrix(chromiumTests, testAliases);
    }

    @Test(dataProvider = "chromiumPolicyTests")
    public void testGenericPolicies(AppAlias.Alias alias, PolicyState.Type type, String name, Object value) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, type, name, value);
    }
}
