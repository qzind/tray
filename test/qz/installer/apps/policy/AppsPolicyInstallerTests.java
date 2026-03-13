package qz.installer.apps.policy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.installer.apps.locator.AppAlias;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import java.util.*;

import static qz.installer.Installer.*;

public class AppsPolicyInstallerTests {
    private static final Logger log = LogManager.getLogger(AppsPolicyInstallerTests.class);

    /**
     * constructs a test matrix of [AppAlias, PolicyState, PolicyName, Values[]]
     * There must be at least 2 values
     */
    @DataProvider(name = "policyArrays")
    public Object[][] policyArrays() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppAlias.Alias alias : AppAlias.CHROMIUM.getAliases()) {
            retMatrix.add(new Object[] {alias, PolicyState.Type.ARRAY, "URLAllowlist", new Object[]{"qz://", "pp://"}});
        }

        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "policyArrays", priority = 1)
    public void testAppsPolicyArrayInstall(AppAlias.Alias alias, PolicyState.Type type, String name, Object[] values) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        PolicyState state = policyInstaller.install(type, name, values);
        assertState(state);

        // Intentionally add the first element a second time
        state = policyInstaller.install(type, name, values[0]);
        assertState(state);

        List<Object> returnedList = Arrays.asList(policyInstaller.primitive.getEntries(state.reset()));

        // Verify there is one, and only one, match.
        for (Object value: values) {
            int firstIndex = returnedList.indexOf(value);
            if (firstIndex == -1) Assert.fail("No match found for value: " + value);
            List<Object> sublist = returnedList.subList(firstIndex + 1, returnedList.size());
            if (sublist.contains(value)) Assert.fail("Duplicate matches found for value: " + value);
        }
    }

    @Test(dataProvider = "policyArrays", priority = 2)
    public void testAppsPolicyArrayUninstall(AppAlias.Alias alias, PolicyState.Type type, String name, Object[] values) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        // Remove the first element
        PolicyState state = policyInstaller.uninstall(type, name, values[0]);
        assertState(state);
        List<Object> returnedList = Arrays.asList(policyInstaller.primitive.getEntries(state.reset()));

        // Verify the second element wasn't erroneously deleted
        assert(returnedList.contains(values[1]));

        // Remove the remaining items, including the value we already removed again
        state = policyInstaller.uninstall(type, name, values);
        assertState(state);
        returnedList = Arrays.asList(policyInstaller.primitive.getEntries(state.reset()));

        // Verify none of our values remain in the array
        for (Object value: values) {
            int firstIndex = returnedList.indexOf(value);
            if (firstIndex != -1) Assert.fail("Failed to remove element: " + value);
        }
    }

    /**
     * constructs a test matrix of [AppAlias, PolicyState, PolicyName, Value]
     */
    @DataProvider(name = "policyBooleans")
    public Object[][] policyBooleans() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppAlias.Alias alias : AppAlias.CHROMIUM.getAliases()) {
            retMatrix.add(new Object[] {alias, PolicyState.Type.VALUE, "SafeBrowsingEnabled", true});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "policyBooleans", priority = 1)
    public void testAppsPolicyBooleanInstall(AppAlias.Alias alias, PolicyState.Type type, String name, Object value) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        PolicyState state = policyInstaller.install(type, name, value);
        assertState(state);
        Object returnedValue = policyInstaller.primitive.getValue(state.reset());

        assetEqual(returnedValue, value);
    }

    @Test(dataProvider = "policyBooleans", priority = 2)
    public void testAppsPolicyBooleanUninstall(AppAlias.Alias alias, PolicyState.Type type, String name, Object value) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        PolicyState state = policyInstaller.uninstall(type, name, value);
        assertState(state);
        Object returnedValue = policyInstaller.primitive.getValue(state.reset());

        assetEqual(returnedValue, null);
    }

    /**
     * constructs a test matrix of [AppAlias, PolicyState, PolicyName, Values]
     */
    @DataProvider(name = "policyMaps")
    public Object[][] policyMaps() {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (AppAlias.Alias alias : AppAlias.FIREFOX.getAliases()) {
            retMatrix.add(new Object[] {alias, PolicyState.Type.MAP, "Certificate", "ImportEnterpriseRoots", true});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "policyMaps", priority = 1)
    public void testAppsPolicyMapInstall(AppAlias.Alias alias, PolicyState.Type type, String name, String subKey, Object value) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        PolicyState state = policyInstaller.install(type, name, subKey, value);
        assertState(state);

        Map<String, Object> map = policyInstaller.primitive.getMap(state.reset());
        assert(map.containsKey(subKey));
    }

    @Test(dataProvider = "policyMaps", priority = 2)
    public void testAppsPolicyMapUninstall(AppAlias.Alias alias, PolicyState.Type type, String name, String subKey, Object value) {
        PolicyInstaller policyInstaller = createPolicyInstaller(alias);

        PolicyState state = policyInstaller.uninstall(type, name, subKey, value);
        assertState(state);

        Map<String, Object> map = policyInstaller.primitive.getMap(state.reset());
        assert(!map.containsKey(subKey));
    }

    private PolicyInstaller createPolicyInstaller(AppAlias.Alias alias) {
        PrivilegeLevel privilegeLevel = SystemUtilities.isAdmin() ? PrivilegeLevel.SYSTEM : PrivilegeLevel.USER;
        try {
            return new PolicyInstaller(privilegeLevel, alias);
        } catch(Exception e) {
            throw new SkipException("");
        }
    }

    private void assertState(PolicyState state) {
        if (state.hasFailed()) {
            state.log();
            Assert.fail(state.reason);
        }
    }

    private void assetEqual(Object returnedValue, Object value) {
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
