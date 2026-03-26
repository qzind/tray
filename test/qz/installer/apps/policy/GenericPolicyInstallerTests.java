package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppFamily.*;

public class GenericPolicyInstallerTests {
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
        ArrayList<AppVariant> testAppVariants = new ArrayList<>();
        for (AppFamily appFamily : AppFamily.values()) {
            Collections.addAll(testAppVariants, appFamily.getVariants());
        }
        return PolicyInstallerTestDispatcher.constructTestMatrix(genericTests, testAppVariants);
    }

    @Test(dataProvider = "genericPolicyTests")
    public void testGenericPolicies(AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        if (value instanceof Float && SystemUtilities.isWindows()) {
            throw new SkipException("Float values are not supported on Windows");
        }
        PolicyInstallerTestDispatcher.dispatchInstallTest(appVariant, type, name, value);
        PolicyInstallerTestDispatcher.dispatchUninstallTest(appVariant, type, name, value);
    }
}
