package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import qz.build.provision.params.Os;
import qz.installer.Installer;
import qz.installer.apps.exception.UnsupportedPolicyException;
import qz.installer.apps.locator.AppFamily;
import qz.utils.SystemUtilities;

import java.util.*;
import java.util.stream.Stream;

import static qz.installer.Installer.PrivilegeLevel.*;
import static qz.installer.apps.policy.PolicyState.Type.*;
import static qz.installer.apps.policy.PolicyInstaller.PolicyLocator.*;

public abstract class PolicyTestDispatcher {
    private static final Logger log = LogManager.getLogger(PolicyTestDispatcher.class);
    private static final Installer.PrivilegeLevel scope = SystemUtilities.isAdmin()? SYSTEM:USER;

    private int testCounter = 0;

    static Object[][] addAppVariants(List<Object[]> tests, AppFamily.AppVariant ... appVariants) {
        List<Object[]> retMatrix = new ArrayList<>();
        Arrays.stream(appVariants).forEach(appVariant -> {
            // prepend appVariant
            tests.stream().map(testRow -> new ArrayList<>(List.of(testRow))).forEach(variantRow -> {
                variantRow.add(0, appVariant);
                retMatrix.add(variantRow.toArray(new Object[0]));
            });
        });
        return retMatrix.toArray(new Object[0][]);
    }

    static Object[][] addAppVariants(List<Object[]> tests, AppFamily ... appFamilies) {
        return addAppVariants(tests, Arrays.stream(appFamilies).flatMap(appFamily -> Stream.of(appFamily.getVariants())).toArray(AppFamily.AppVariant[]::new));
    }

    @SuppressWarnings("UnusedReturnValue")
    static PolicyState testAppsPolicyInstall(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        return testAppsPolicyInstall(appVariant, AppType.NATIVE, type, name, value);
    }

    @SuppressWarnings("UnusedReturnValue")
    static PolicyState testAppsPolicyInstall(AppFamily.AppVariant appVariant, AppType appType, PolicyState.Type type, String name, Object value) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueInstall(appVariant, appType, name, value);
            case ARRAY:
                return testAppsPolicyArrayInstall(appVariant, appType, name, value);
            case MAP:
                return testAppsPolicyMapInstall(appVariant, appType, name, value);
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    static PolicyState testAppsPolicyUninstall(AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        return testAppsPolicyInstall(appVariant, AppType.NATIVE, type, name, value);
    }

    @SuppressWarnings("UnusedReturnValue,SameParameterValue")
    static PolicyState testAppsPolicyUninstall(AppFamily.AppVariant appVariant, AppType appType, PolicyState.Type type, String name, Object value) {
        switch(type) {
            case VALUE:
                return testAppsPolicyValueUninstall(appVariant, appType, name, value);
            case ARRAY:
                return testAppsPolicyArrayUninstall(appVariant, appType, name, value);
            case MAP:
                return testAppsPolicyMapUninstall(appVariant, appType, name, value);
        }
        return null;
    }

    private static PolicyState testAppsPolicyArrayInstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);
        PolicyState state = policyInstaller.install(ARRAY, name, value);
        assertState(state);

        // Intentionally add the first element a second time (this will show a duplicate log)
        Object[] array = (Object[])value;
        state = policyInstaller.install(ARRAY, name, array[0]);
        assertState(state);

        List<Object> returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify there is one, and only one, match.
        for (Object element: array) {
            int firstIndex = returnedList.indexOf(element);
            if (firstIndex == -1) Assert.fail("No match found for value: " + element);
            List<Object> sublist = returnedList.subList(firstIndex + 1, returnedList.size());
            if (sublist.contains(element)) Assert.fail("Duplicate matches found for value: " + element);
        }
        return state;
    }

    private static PolicyState testAppsPolicyArrayUninstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);
        PolicyState state;
        List<Object> returnedList;

        // If there are 2 or more items, we can check for over-deletion
        Object[] array = (Object[])value;
        if (array.length > 1) {
            // Remove the first element
            state = policyInstaller.uninstall(ARRAY, name, array[0]);
            assertState(state);
            returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

            // Verify the second element wasn't erroneously deleted
            Assert.assertTrue(returnedList.contains(array[1]));
        }

        // Remove the remaining items, including the value we already removed again
        state = policyInstaller.uninstall(ARRAY, name, value);
        assertState(state);
        returnedList = Arrays.asList(policyInstaller.getEntries(state.reset()));

        // Verify none of our values remain in the array
        for (Object element: array) {
            int firstIndex = returnedList.indexOf(element);
            if (firstIndex != -1) Assert.fail("Failed to remove element: " + element);
        }
        return state;
    }

    private static PolicyState testAppsPolicyValueInstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);

        PolicyState state = policyInstaller.install(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assertEqual(returnedValue, value);
        return state;
    }

    private static PolicyState testAppsPolicyValueUninstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);

        PolicyState state = policyInstaller.uninstall(VALUE, name, value);
        assertState(state);

        Object returnedValue = policyInstaller.getValue(state.reset());
        assertEqual(returnedValue, null);
        return state;
    }
    
    @SuppressWarnings("unchecked")
    private static PolicyState testAppsPolicyMapInstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);
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
    private static PolicyState testAppsPolicyMapUninstall(AppFamily.AppVariant appVariant, AppType appType, String name, Object value) {
        PolicyInstaller policyInstaller = new PolicyInstaller(scope, appVariant, appType);
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
                //todo: Add dedupe tests for arrays in maps
                continue;
            }
            assertEqual(outerValue, entry.getValue());
        }
        return true;
    }

    /***
     * Returns true if
     */
    @SuppressWarnings("unchecked")
    private static boolean mapExcludesMap(Map<String, Object> baseMap, Map<String, Object> compareMap) {
        if (baseMap == null || compareMap == null) return true;

        for (Map.Entry<String, Object> baseEntry : baseMap.entrySet()) {
            String baseKey = baseEntry.getKey();
            Object baseValue = baseEntry.getValue();

            if (compareMap.containsKey(baseKey)) {
                Object compareValue = compareMap.get(baseKey);

                if (baseValue instanceof Map && compareValue instanceof Map) {
                    // recurse
                    if (!mapExcludesMap((Map<String, Object>) baseValue, (Map<String, Object>) compareValue)) {
                        return false;
                    }
                } else if (baseValue instanceof Object[] && compareValue instanceof Object[]) {
                    Object[] baseArray = (Object[]) baseValue;
                    Object[] compareArray = (Object[]) compareValue;

                    for (Object baseItem : baseArray) {
                        if (Arrays.asList(compareArray).contains(baseItem)) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private static void assertEqual(Object actual, Object expected) {
        if(SystemUtilities.isWindows()) {
            // Windows registry is incapable of returning a boolean
            if(expected instanceof Boolean && actual instanceof Integer) {
                Integer intReturned = (Integer)actual;
                switch(intReturned) {
                    case 0:
                    case 1:
                        // treat as boolean
                        actual = intReturned != 0;
                        break;
                    default:
                        // let it fail
                }
            }
        }
        if(SystemUtilities.isMac()) {
            if(expected instanceof Integer && actual instanceof Boolean) {
                // cli limitations for maps
                expected = expected.equals(1);
            }
            if(expected instanceof Float && actual instanceof String) {
                // cli puts quotes around floats for no reason :/
                actual = Float.parseFloat((String)actual);
            }
        }
        if (!Objects.equals(expected, actual)) {
            log.warn(
                    "Expected value: {} (type: {}), returned value: {} (type: {})",
                    expected,
                    expected == null ? "null" : expected.getClass().getName(),
                    actual,
                    actual == null ? "null" : actual.getClass().getName()
            );
            Assert.fail("Return value mismatch");
        }
    }

    @SuppressWarnings("SameParameterValue")
    static void skipIf(boolean ... conditions) throws SkipException {
        for(boolean condition : conditions) if(!condition) return;
        throw new SkipException(String.format("Skipping test for Os: '%s', PrivilegeLevel: '%s'", SystemUtilities.getOs(), scope));
    }

    @SuppressWarnings("SameParameterValue")
    void runTests(boolean doInstall, boolean doUninstall, AppFamily.AppVariant appVariant, PolicyState.Type type, String name, Object value) {
        for(AppType appType : AppType.collect(appVariant.getAppFamily())) {
            if (doInstall) {
                testAppsPolicyInstall(appVariant, appType, type, name, value);
            }
            if (doUninstall) {
                testAppsPolicyUninstall(appVariant, appType, type, name, value);
            }
            testCounter++;
        }
        skipIf(testCounter == 0);
    }
}
