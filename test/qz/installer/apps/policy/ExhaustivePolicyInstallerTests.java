package qz.installer.apps.policy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppFamily.*;

/**
 * Tests one of each <code>PolicyState.Type</code> as well as each primitive type
 * <code>String</code>, <code>int</code>, <code>float</code>, </code><code>boolean</code>
 */
@SuppressWarnings("ExtractMethodRecommender")
public class ExhaustivePolicyInstallerTests extends PolicyTestDispatcher {
    @DataProvider(name = "exhaustivePoliciesData")
    public Object[][] exhaustivePoliciesData() {
        Object[][] baseline = new Object[][] {
                { VALUE, "testBool", true },
                { VALUE, "testInt", 1234 },
                { VALUE, "testString", "test" },
                { ARRAY, "testArray", new Object[] { "element 1", "element 2" } }, // 2-element array
                { ARRAY, "testArray", new Object[] { "element 1" } }, // 1-element array
                { MAP, "testMapString", new HashMap<>(Map.of("firstKey", "value 1", "secondKey", "value 2" ))}, // map from map string
                { MAP, "testMapInt", new HashMap<>(Map.of("firstKey", 1, "secondKey", 2 ))}, // map from map int
                { MAP, "testMapBool", new HashMap<>(Map.of("firstKey", true, "secondKey", false ))}, // map from map boolean
        };

        List<Object[]> allTests = new ArrayList<>(List.of(baseline));

        if (!SystemUtilities.isWindows()) {
            // floats aren't supported on Windows
            Object[][] floatTests = new Object[][] {
                    { VALUE, "testFloat", 123.4f },
                    { MAP, "testMapFloat", new HashMap<>(Map.of("firstKey", 1.1f, "secondKey", 2.2f ))}, // map from map float
            };
            allTests.addAll(List.of(floatTests));
        }

        if (SystemUtilities.isLinux()) {
            // arrays in maps are only supported on linux
            Object[][] linuxOnly = new Object[][] {
                    { MAP, "testMapArrayString", new Object[] {"StringArray", new Object[] {"firstElement", "secondElement"}}},
                    { MAP, "testMapArrayString", new Object[] {"IntArray", new Object[] {111, 222}}},
                    { MAP, "testMapArrayString", new Object[] {"BoolArray", new Object[] {true, false}}},
            };
            allTests.addAll(List.of(linuxOnly));
        }

        // Lots of tests: Only add the first variant of each type
        return addAppVariants(allTests, Arrays.stream(AppFamily.values()).map(
                appFamily -> appFamily.getVariants()[0]
        ).toArray(AppVariant[]::new));
    }

    @Test(dataProvider = "exhaustivePoliciesData")
    public void exhaustivePoliciesTests(AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        testAppsPolicyInstall(appVariant, type, name, value);
        testAppsPolicyUninstall(appVariant, type, name, value);
    }
}
