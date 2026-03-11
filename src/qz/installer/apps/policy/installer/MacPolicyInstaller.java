package qz.installer.apps.policy.installer;

import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.PlistUtils;

import java.util.*;

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
        return state.setSucceeded(PlistUtils.writeArray(state.getLocation(), state.getName(), Arrays.asList(values), true));
    }

    @Override
    public PolicyState removeEntries(PolicyState state, Object ... values) {
        Collection<Object> existing = PlistUtils.getArray(state.getLocation(), state.getName(), true);
        if (existing.isEmpty()) {
            return state.setSucceeded("skipping, policy file was not found or empty");
        }
        // Remove just our own entries
        existing.removeAll(List.of(values));
        // Write remaining entries back
        // Note: dedupe: true will delete what's there first, so this may result in the policyName being removed entirely
        return putEntries(state, existing.toArray());
    }

    @Override
    public PolicyState putMap(PolicyState state, Map<String,Object> map) {
        return state.setSucceeded(PlistUtils.writeMap(state.getLocation(), state.getName(), map));
    }

    @Override
    public Object getValue(PolicyState state) {
        return PlistUtils.getValue(state.getLocation(), state.getName());
    }

    @Override
    public Object[] getEntries(PolicyState state) {
        return PlistUtils.getArray(state.getLocation(), state.getName());
    }

    @Override
    public Map<String,Object> getMap(PolicyState state) {
        Map<String,Object> map = PlistUtils.getMap(state.getLocation(), state.getName());
        if(map != null) {
            state.setSucceeded(map.isEmpty()? String.format("Plist '%s' is missing map entry for '%s', returning an empty map", state.getLocation(), state.getName()) : null);
        }
        state.setFailed(String.format("An unexpected error occurred obtaining map value '%s' from '%s'",state.getName(),  state.getLocation()));
        return new HashMap<>();
    }
}
