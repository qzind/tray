package qz.installer.apps.policy.installer;

import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.PlistUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
            return state.setSkipped("policy file was not found or empty");
        }
        // Remove just our own entries
        existing.removeAll(List.of(values));
        // Write remaining entries back
        // Note: dedupe: true will delete what's there first, so this may result in the policyName being removed entirely
        return putEntries(state, existing, true);
    }
}
