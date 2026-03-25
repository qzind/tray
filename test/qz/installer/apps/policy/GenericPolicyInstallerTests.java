package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;

import java.util.*;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppAlias.*;

public class GenericPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(GenericPolicyInstallerTests.class);

    static Object[][] genericTests = {
            {"testBool", true, VALUE},                                      //boolean
            {"testInt", 1234, VALUE},                                       //integer
            {"testFloat", 123.4, VALUE},                                    //decimal
            {"testString", "test", VALUE},                                  //string
            {"testArray", new Object[] {"element 1", "element 2"}, ARRAY},  //2-item array
            {"testArray", new Object[] {"element 1"}, ARRAY},               //1-item array
            {"testMap", new HashMap<String, Object>(Map.of(                 //map from map
                    "firstKey", "value 1",
                    "secondKey", "value 2"
                    )), MAP},
            {"testMap", new Object[] {"firstKey", "value 1"}, MAP}             //map from array
    };

    @DataProvider(name = "genericPolicyTests")
    public Object[][] genericPolicyTests() {
        ArrayList<AppAlias.Alias> testAliases = new ArrayList<>();
        for (AppAlias appAlias : AppAlias.values()) {
            Collections.addAll(testAliases,appAlias.getAliases());
        }
        return PolicyInstallerTestDispatcher.constructTestMatrix(genericTests, testAliases);
    }

    @Test(dataProvider = "genericPolicyTests")
    public void testGenericPolicies(Alias alias, String name, Object value, PolicyState.Type type) {
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, name, value, type);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, name, value, type);
    }
}
