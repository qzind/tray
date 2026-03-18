package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.Installer.*;

import static qz.installer.Installer.PrivilegeLevel.*;
import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.locator.AppAlias.*;

public class AppsPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(AppsPolicyInstallerTests.class);

    final static PrivilegeLevel scope = SystemUtilities.isAdmin() ? SYSTEM : USER;

    static Object[][] chromeTests = {
        {"SafeBrowsingEnabled", true},
        {"DefaultNotificationsSetting", 2},
        {"PolicyTestFloat", 5.5},
        {"DefaultDownloadDirectory", "/home/${user_name}/Downloads"},
        {"URLAllowlist", new Object[] {"qz://", "pp://"}},
    };
    static Object[][] firefoxTests = {
        {"DisableSafeMode", true},
        {"PrivateBrowsingModeAvailability", 2},
        {"PolicyTestFloat", 5.5},
        {"DefaultDownloadDirectory", "${home}/Downloads"},
        {"HttpAllowlist", new Object[] {"http://example.org", "http://example.edu"}},
    };

    static HashMap<AppAlias, Object[][]> testMap;
    static {
        testMap = new HashMap<>(Map.of(
                CHROMIUM, chromeTests,
                FIREFOX, firefoxTests
        ));
    }

    /**
     * Constructs a test matrix of [Alias, valueType, PolicyName, Value]
     * Note: Array tests assume at least 2 values
     */
    @DataProvider(name = "policyTests")
    public Object[][] policyArrays() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppAlias appAlias : AppAlias.values()) {
            Object[][] tests = testMap.get(appAlias);
            if (tests == null) Assert.fail("No tests have been included for " + appAlias);
            for (Alias alias : appAlias.getAliases()) {
                for (Object[] testRow: tests) {
                    retMatrix.add(new Object[] {alias, testRow[0], testRow[1]});
                }
            }
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "policyTests")
    public void testAppPolicy(Alias alias, String name, Object value) {
        if (value instanceof Object[]) {
            testAppsPolicyArrayInstall(alias, name, (Object[])value);
            testAppsPolicyArrayUninstall(alias, name, (Object[])value);
        } else if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            testAppsPolicyValueInstall(alias, name, value);
            testAppsPolicyValueUninstall(alias, name, value);
        } else {
            Assert.fail("Bad type: " + (value == null ? "null" : value.getClass().getName()));
        }
    }

    public void testAppsPolicyArrayInstall(Alias alias, String name, Object[] values) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);
        PolicyState state = policyInstaller.install(ARRAY, name, values);
        assertState(state);

        // Intentionally add the first element a second time
        state = policyInstaller.install(ARRAY, name, values[0]);
        assertState(state);

        List<Object> returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify there is one, and only one, match.
        for (Object value: values) {
            int firstIndex = returnedList.indexOf(value);
            if (firstIndex == -1) Assert.fail("No match found for value: " + value);
            List<Object> sublist = returnedList.subList(firstIndex + 1, returnedList.size());
            if (sublist.contains(value)) Assert.fail("Duplicate matches found for value: " + value);
        }
    }

    public void testAppsPolicyArrayUninstall(Alias alias, String name, Object[] values) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        // Remove the first element
        PolicyState state = policyInstaller.uninstall(ARRAY, name, values[0]);
        assertState(state);
        List<Object> returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify the second element wasn't erroneously deleted
        assert(returnedList.contains(values[1]));

        // Remove the remaining items, including the value we already removed again
        state = policyInstaller.uninstall(ARRAY, name, values);
        assertState(state);
        returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify none of our values remain in the array
        for (Object value: values) {
            int firstIndex = returnedList.indexOf(value);
            if (firstIndex != -1) Assert.fail("Failed to remove element: " + value);
        }
    }

    public void testAppsPolicyValueInstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.install(VALUE, name, value);
        assertState(state);
        Object returnedValue = policyInstaller.getValue(state.reset());

        assetEqual(returnedValue, value);
    }

    public void testAppsPolicyValueUninstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.uninstall(VALUE, name, value);
        assertState(state);
        Object returnedValue = policyInstaller.getValue(state.reset());

        assetEqual(returnedValue, null);
    }

    /**
     * Constructs a test matrix of [Alias, PolicyName, Values]
     * todo: add to test map
     */
    @DataProvider(name = "policyMaps")
    public Object[][] policyMaps() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (Alias alias : FIREFOX.getAliases()) {
            retMatrix.add(new Object[] {alias, "Certificate", "ImportEnterpriseRoots", true});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "policyMaps", priority = 1)
    public void testAppsPolicyMapInstall(Alias alias, String name, String subKey, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.install(MAP, name, subKey, value);
        assertState(state);

        Map<String, Object> map = policyInstaller.getMap(state.reset());
        assert(map.containsKey(subKey));
    }

    @Test(dataProvider = "policyMaps", priority = 2)
    public void testAppsPolicyMapUninstall(Alias alias, String name, String subKey, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.uninstall(MAP, name, subKey, value);
        assertState(state);

        Map<String, Object> map = policyInstaller.getMap(state.reset());
        assert(!map.containsKey(subKey));
    }

    private static void assertState(PolicyState state) {
        if (state.hasFailed()) {
            state.log();
            Assert.fail(state.reason);
        }
    }

    private static void assetEqual(Object returnedValue, Object value) {
        if(SystemUtilities.isWindows()) {
            // Windows registry is incapable of returning a boolean
            if(value instanceof Boolean && returnedValue instanceof Integer) {
                Integer intReturned = (Integer)returnedValue;
                switch(intReturned) {
                    case 0:
                    case 1:
                        // treat as boolean
                        returnedValue = intReturned != 0;
                        break;
                    default:
                        // let it fail
                }
            }
        }
        if (!Objects.equals(value, returnedValue)) {
            log.warn(
                    "Expected value: {} (type: {}), returned value: {} (type: {})",
                    value,
                    value == null ? "null" : value.getClass().getName(),
                    returnedValue,
                    returnedValue == null ? "null" : returnedValue.getClass().getName()
            );
            Assert.fail("Return value mismatch");
        }
    }
}
