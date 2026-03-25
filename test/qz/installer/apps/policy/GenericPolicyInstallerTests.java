package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppAlias.*;

public class GenericPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(GenericPolicyInstallerTests.class);

    private static final HashMap<String, Object> testHashMap = new HashMap<>(Map.of(
        "firstKey", "value 1",
        "secondKey", "value 2"
    ));
    static {
        if (SystemUtilities.isLinux()) {
            testHashMap.put("thirdKey", new Object[] {"element 1", "element 2"}); //arrays in maps are only supported on linux
        }
    }

    static Object[][] genericTests = {
            {VALUE, "testBool", true},                                      //boolean
            {VALUE, "testInt", 1234},                                       //integer
            {VALUE, "testFloat", 123.4f},                                   //decimal
            {VALUE, "testString", "test"},                                  //string
            {ARRAY, "testArray", new Object[] {"element 1", "element 2"}},  //2-item array
            {ARRAY, "testArray", new Object[] {"element 1"}},               //1-item array
            {MAP, "testMap", testHashMap},                                  //map from map
            {MAP, "testMap", new Object[] {"firstKey", "value 1"}}          //map from array
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
    public void testGenericPolicies(Alias alias, PolicyState.Type type, String name, Object value) {
        if (value instanceof Float && SystemUtilities.isWindows()) {
            throw new SkipException("Float values are not supported on Windows");
        }
        PolicyInstallerTestDispatcher.dispatchInstallTest(alias, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(alias, type, name, value);
    }
}
