package qz.installer.apps.policy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.common.Constants;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.List;

import static qz.common.Constants.ABOUT_URL;
import static qz.installer.apps.policy.PolicyState.Type.*;

public class ChromiumPolicyInstallerTests extends PolicyTestDispatcher {
    @DataProvider(name = "chromiumPoliciesData")
    public Object[][] chromiumPoliciesData() {
        Object[][] tests = new Object[][] {
                { ARRAY, "URLAllowlist", new Object[] { String.format("%s://*", Constants.DATA_DIR) } },
                { ARRAY, "LocalNetworkAccessAllowedForUrls", new Object[] { "[*.]" + SystemUtilities.parseRootDomain(ABOUT_URL) } },
                { VALUE, "IncognitoModeAvailability", false }
        };
        return addAppVariants(List.of(tests), AppFamily.CHROMIUM);
    }

    @Test(dataProvider = "chromiumPoliciesData")
    public void testChromiumPolicies(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        testAppsPolicyInstall(appVariant, type, name, value);
        testAppsPolicyUninstall(appVariant, type, name, value);
    }
}
