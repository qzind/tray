package qz.installer.apps.policy.installer;

import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.PlistUtils;

import java.util.*;

import static qz.installer.apps.policy.PolicyInstaller.PrimitivePolicyInstaller.*;

public class MacPolicyInstaller implements PolicyInstaller.PrimitivePolicyInstaller {
    @Override
    public PolicyState putValue(PolicyState state, Object value) {
        return state.setSucceeded(PlistUtils.write(state.getLocation(), state.getName(), value));
    }

    @Override
    public PolicyState removeValue(PolicyState state) {
        return state.setSucceeded(PlistUtils.delete(state.getLocation(), state.getName()));
    }

    @Override
    public PolicyState putEntries(PolicyState state, Object ... values) {
        // we always dedupe to clean up previous installs
        Collection<Object> existing = new LinkedHashSet<>(Arrays.asList(PlistUtils.getArray(state.getLocation(), state.getName())));
        existing.addAll(Arrays.asList(values));
        if(removeValue(state).hasFailed())  {
            return state;
        }
        return state.setSucceeded(PlistUtils.writeArray(state.getLocation(), state.getName(), existing));
    }

    @Override
    public PolicyState removeEntries(PolicyState state, Object ... values) {
        Collection<Object> existing = new LinkedHashSet<>(Arrays.asList(PlistUtils.getArray(state.getLocation(), state.getName())));
        if (existing.isEmpty()) {
            return state.setSucceeded("skipping, policy file was not found or empty");
        }
        // Remove values specified
        existing.removeAll(Arrays.asList(values));
        // Clear out the old array
        if(removeValue(state).hasFailed()) {
            return state;
        }
        return putEntries(state, existing.toArray());
    }

    @Override
    public PolicyState putMap(PolicyState state, Map<String,Object> map) {
        return state.setSucceeded(PlistUtils.writeMap(state.getLocation(), state.getName(), mergeMap(getMap(state), map, true)));
    }

    @Override
    public Object getValue(PolicyState state) {
        return state.failIfNull(PlistUtils.getValue(state.getLocation(), state.getName()));
    }

    @Override
    public Object[] getEntries(PolicyState state) {
        return state.failIfNull(PlistUtils.getArray(state.getLocation(), state.getName()));
    }

    @Override
    public Map<String,Object> getMap(PolicyState state) {
        Map<String,Object> map = PlistUtils.getMap(state.getLocation(), state.getName());
        return state.failIfNull(map);
    }
}
