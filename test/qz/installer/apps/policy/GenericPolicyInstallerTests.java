package qz.installer.apps.policy;

import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppFamily.*;

public class GenericPolicyInstallerTests {
    static ArrayList<Object[]> genericTests = new ArrayList<>();
    static {
        HashMap<String, Object> testHashMap = new HashMap<>(Map.of(
                "firstKey", "value 1",
                "secondKey", "value 2"
        ));

        if (SystemUtilities.isLinux()) {
            testHashMap.put("thirdKey", new Object[] {"element 1", "element 2"}); //arrays in maps are only supported on linux
        }

        genericTests.add(new Object[] {VALUE, "testBool", true});                                     //boolean
        genericTests.add(new Object[] {VALUE, "testInt", 1234});                                      //integer
        genericTests.add(new Object[] {VALUE, "testFloat", 123.4f});                                  //decimal
        genericTests.add(new Object[] {VALUE, "testString", "test"});                                 //string
        genericTests.add(new Object[] {ARRAY, "testArray", new Object[] {"element 1", "element 2"}}); //2-item array
        genericTests.add(new Object[] {ARRAY, "testArray", new Object[] {"element 1"}});              //1-item array
        genericTests.add(new Object[] {MAP, "testMap", testHashMap});                                 //map from map
        genericTests.add(new Object[] {MAP, "testMap", new Object[] {"firstKey", "value 1"}});        //map from array
    }

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
