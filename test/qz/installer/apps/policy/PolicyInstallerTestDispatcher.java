package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import qz.installer.Installer;
import qz.installer.apps.exception.UnsupportedPolicyException;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.Installer.PrivilegeLevel.*;
import static qz.installer.apps.policy.PolicyState.Type.*;

public class PolicyInstallerTestDispatcher {
    private static final Logger log = LogManager.getLogger(PolicyInstallerTestDispatcher.class);

    final static Installer.PrivilegeLevel scope = SystemUtilities.isAdmin()? SYSTEM:USER;

    public static Object[][] constructTestMatrix(ArrayList<Object[]> tests, ArrayList<AppFamily.AppVariant> appVariants) {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppFamily.AppVariant appVariant : appVariants) {
            for (Object[] testRow: tests) {
                retMatrix.add(new Object[] {appVariant, testRow[0], testRow[1], testRow[2]});
            }
        }
        return retMatrix.toArray(new Object[0][]);
    }

    public static PolicyState dispatchInstallTest(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueInstall(appVariant, name, value);
            case ARRAY:
                return testAppsPolicyArrayInstall(appVariant, name, value);
            case MAP:
                return testAppsPolicyMapInstall(appVariant, name, value);
        }
        return null;
    }

    public static PolicyState dispatchUninstallTest(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueUninstall(appVariant, name, value);
            case ARRAY:
                return testAppsPolicyArrayUninstall(appVariant, name, value);
            case MAP:
                return testAppsPolicyMapUninstall(appVariant, name, value);
        }
        return null;
    }

    private static PolicyState testAppsPolicyArrayInstall(AppFamily.AppVariant appVariant, String name, Object value) {
        Object[] values = (Object[])value;
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);
        PolicyState state = policyInstaller.install(ARRAY, name, values);
        assertState(state);

        // Intentionally add the first element a second time
        state = policyInstaller.install(ARRAY, name, values[0]);
        assertState(state);

        List<Object> returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify there is one, and only one, match.
        for (Object element: values) {
            int firstIndex = returnedList.indexOf(element);
            if (firstIndex == -1) Assert.fail("No match found for value: " + element);
            List<Object> sublist = returnedList.subList(firstIndex + 1, returnedList.size());
            if (sublist.contains(element)) Assert.fail("Duplicate matches found for value: " + element);
        }
        return state;
    }

    private static PolicyState testAppsPolicyArrayUninstall(AppFamily.AppVariant appVariant, String name, Object value) {
        Object[] values = (Object[])value;
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);
        PolicyState state;
        List<Object> returnedList;

        // If there are 2 or more items, we can check for over-deletion
        if (values.length > 1) {
            // Remove the first element
            state = policyInstaller.uninstall(ARRAY, name, values[0]);
            assertState(state);
            returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

            // Verify the second element wasn't erroneously deleted
            Assert.assertTrue(returnedList.contains(values[1]));
        }

        // Remove the remaining items, including the value we already removed again
        state = policyInstaller.uninstall(ARRAY, name, values);
        assertState(state);
        returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify none of our values remain in the array
        for (Object element: values) {
            int firstIndex = returnedList.indexOf(element);
            if (firstIndex != -1) Assert.fail("Failed to remove element: " + element);
        }
        return state;
    }

    private static PolicyState testAppsPolicyValueInstall(AppFamily.AppVariant appVariant, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);

        PolicyState state = policyInstaller.install(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assertEqual(returnedValue, value);
        return state;
    }

    private static PolicyState testAppsPolicyValueUninstall(AppFamily.AppVariant appVariant, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);

        PolicyState state = policyInstaller.uninstall(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assertEqual(returnedValue, null);
        return state;
    }
    
    @SuppressWarnings("unchecked")
    private static PolicyState testAppsPolicyMapInstall(AppFamily.AppVariant appVariant, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);
        PolicyState state;

        HashMap<String, Object> intendedMap = new HashMap<>();

        if (value instanceof Object[]) {
            // Test install(Map, name, subkey, value) syntax
            Object [] values = (Object[])value;
            state = policyInstaller.install(MAP, name, values[0], values[1]);
            intendedMap.put((String)values[0], values[1]);
        } else {
            state = policyInstaller.install(MAP, name, value);
            intendedMap.putAll((Map<String, ?>)value);
        }
        assertState(state);

        Map<String, Object> returnedMap = policyInstaller.getMap(state.reset());
        Assert.assertTrue(mapContainsMap(returnedMap, intendedMap));
        return state;
    }

    @SuppressWarnings("unchecked")
    private static PolicyState testAppsPolicyMapUninstall(AppFamily.AppVariant appVariant, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant);
        PolicyState state;

        HashMap<String, Object> intendedMap = new HashMap<>();

        if (value instanceof Object[]) {
            // Test install(Map, name, subkey, value) syntax
            Object [] values = (Object[])value;
            state = policyInstaller.uninstall(MAP, name, values[0], values[1]);
            intendedMap.put((String)values[0], values[1]);
        } else {
            state = policyInstaller.uninstall(MAP, name, value);
            intendedMap.putAll((Map<String, ?>)value);
        }
        assertState(state);

        Map<String, Object> returnedMap = policyInstaller.getMap(state.reset());
        Assert.assertTrue(mapExcludesMap(returnedMap, intendedMap));
        return state;
    }

    private static void assertState(PolicyState state) {
        if (state.hasFailed()) {
            state.log();
            if (state.exception instanceof UnsupportedPolicyException) {
                throw new SkipException(state.reason);
            }
            Assert.fail(state.reason);
        }
    }

    /***
     * Returns true if all k,v pairs in innerMap exist in outerMap
     */
    private static boolean mapContainsMap(Map<String, Object> outerMap, HashMap<String, Object> innerMap) {
        for (Map.Entry<String, Object> entry: innerMap.entrySet()) {
            Object outerValue = outerMap.get(entry.getKey());
            if (outerValue == null) return false;
            if (outerValue instanceof Object[]) {
                //todo
                continue;
            }
            assertEqual(outerValue, entry.getValue());
        }
        return true;
    }

    /***
     * Returns true of there are no keys that overlap between the two maps
     */
    private static boolean mapExcludesMap(Map<String, Object> mapA, HashMap<String, Object> mapB) {
        for (String key : mapA.keySet()) {
            if (mapB.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    private static void assertEqual(Object returnedValue, Object value) {
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
