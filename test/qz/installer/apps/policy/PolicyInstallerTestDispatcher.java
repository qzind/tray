package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.Installer.PrivilegeLevel;
import static qz.installer.Installer.PrivilegeLevel.SYSTEM;
import static qz.installer.Installer.PrivilegeLevel.USER;
import static qz.installer.apps.locator.AppAlias.Alias;
import static qz.installer.apps.policy.PolicyState.Type.*;

public class PolicyInstallerTestDispatcher {
    private static final Logger log = LogManager.getLogger(PolicyInstallerTestDispatcher.class);

    final static PrivilegeLevel scope = SystemUtilities.isAdmin()? SYSTEM:USER;

    public static Object[][] constructTestMatrix(Object[][] tests, ArrayList<Alias> aliases) {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (Alias alias: aliases) {
            for (Object[] testRow: tests) {
                retMatrix.add(new Object[] {alias, testRow[0], testRow[1], testRow[2]});
            }
        }
        return retMatrix.toArray(new Object[0][]);
    }

    public static PolicyState dispatchInstallTest(Alias alias, String name, Object value, PolicyState.Type type) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueInstall(alias, name, value);
            case ARRAY:
                return testAppsPolicyArrayInstall(alias, name, (Object[])value);
            case MAP:
                return testAppsPolicyMapInstall(alias, name, value);
        }
        return null;
    }

    public static PolicyState dispatchUninstallTest(Alias alias, String name, Object value, PolicyState.Type type) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueUninstall(alias, name, value);
            case ARRAY:
                return testAppsPolicyArrayUninstall(alias, name, (Object[])value);
            case MAP:
                return testAppsPolicyMapUninstall(alias, name, value);
        }
        return null;
    }

    private static PolicyState testAppsPolicyArrayInstall(Alias alias, String name, Object[] values) {
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
        return state;
    }

    private static PolicyState testAppsPolicyArrayUninstall(Alias alias, String name, Object[] values) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);
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
        for (Object value: values) {
            int firstIndex = returnedList.indexOf(value);
            if (firstIndex != -1) Assert.fail("Failed to remove element: " + value);
        }
        return state;
    }

    private static PolicyState testAppsPolicyValueInstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.install(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assetEqual(returnedValue, value);
        return state;
    }

    private static PolicyState testAppsPolicyValueUninstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);

        PolicyState state = policyInstaller.uninstall(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assetEqual(returnedValue, null);
        return state;
    }
    
    @SuppressWarnings("unchecked")
    private static PolicyState testAppsPolicyMapInstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);
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
    private static PolicyState testAppsPolicyMapUninstall(Alias alias, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, alias);
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
            if (!outerValue.equals(entry.getValue())){
                log.warn("Mismatch found for entry:{}. Expected:{}, Returned:{}",
                         entry.getKey(),
                         entry.getValue(),
                         outerValue);
                return false;
            }
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
