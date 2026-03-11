package qz.installer.apps.policy;

import com.sun.jna.platform.win32.WinReg;
import qz.build.provision.params.Os;
import qz.installer.Installer;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.installer.LinuxPolicyInstaller;
import qz.installer.apps.policy.installer.MacPolicyInstaller;
import qz.installer.apps.policy.installer.WindowsPolicyInstaller;
import qz.installer.apps.policy.locator.LinuxChromiumPolicyLocator;
import qz.installer.apps.policy.locator.MacChromiumPolicyLocator;
import qz.installer.apps.policy.locator.WindowsChromiumPolicyLocator;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PolicyInstaller {
    public enum Phase {
        INSTALL,
        UNINSTALL;
    }

    public interface PrimitivePolicyInstaller {
        PolicyState putValue(PolicyState state, Object value);
        PolicyState removeValue(PolicyState state);
        PolicyState putEntries(PolicyState state, Object ... values);
        PolicyState removeEntries(PolicyState state, Object ... values);
        PolicyState putMap(PolicyState state, Map<String, Object> map);
        Object getValue(PolicyState state);
        Object[] getEntries(PolicyState state);
        HashMap<String, Object> getMap(PolicyState state);

        @SuppressWarnings("unchecked")
        static PolicyState putMap(PrimitivePolicyInstaller primitive, PolicyState state, Object ... values) {
            for(Object value : values) {
                if(primitive.putMap(state, (HashMap<String,Object>)value).hasFailed()) {
                    break;
                }
            }
            return state;
        }

        @SuppressWarnings("unchecked")
        static PolicyState removeMap(PrimitivePolicyInstaller primitive, PolicyState state, Object ... values) {
            for(Object value : values) {
                HashMap<String,Object> provided = (HashMap<String,Object>)value;
                HashMap<String,Object> remaining = primitive.getMap(state);
                if (remaining == null) return state.setSucceeded("suspiciously succeeded, the map is null");
                int removeCount = 0;
                for(String key : provided.keySet())
                    if (remaining.remove(key) != null)
                        removeCount++;

                if (removeCount == 0)  return state.setSucceeded("nothing to remove");
                if (!primitive.removeValue(state).hasFailed()) return state; // delete failed
                if (remaining.isEmpty())  return state.setSucceeded("nothing to write");
                if (primitive.putMap(state, remaining).hasFailed()) return state;
            }
            return state;
        }
    }

    public interface PolicyLocator {
        Path getLocation(Installer.PrivilegeLevel scope, AppAlias.Alias alias);
    }

    final private Os os;
    final private Installer.PrivilegeLevel scope;
    final private AppAlias.Alias alias;

    final PrimitivePolicyInstaller primitive;
    final PolicyLocator locator;

    public PolicyInstaller(Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
        this(SystemUtilities.getOs(), scope, alias);
    }

    public PolicyInstaller(Os os, Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
        this.os = os;
        this.scope = scope;
        this.alias = alias;

        this.primitive = constructPrimitiveInstaller();
        this.locator = constructPolicyLocator();
    }

    public PolicyState install(PolicyState.Type type, String name, Object ... values) {
        PolicyState state = createPolicyState(Phase.INSTALL, type, name);

        if(isProhibited()) {
            return state.setFailed("User mode policies are not yet supported on Linux").log();
        }

        if(values.length < 1) {
            return state.setFailed("no policy value was provided").log();
        }
        switch(state.getType()) {
            case ARRAY:
                return primitive.putEntries(state, values).log();
            case MAP:
                return PrimitivePolicyInstaller.putMap(primitive, state, values).log();
            case VALUE:
            default:
                if(values.length > 1) {
                    return state.setFailed("only one value is allowed").log();
                }
                return primitive.putValue(state, values[0]).log();
        }
    }

    public PolicyState uninstall(PolicyState.Type type, String name, Object ... values) {
        PolicyState state = createPolicyState(Phase.UNINSTALL, type, name);

        if(isProhibited()) {
            return state.setFailed("User mode policies are not yet supported on Linux").log();
        }

        switch(state.getType()) {
            case ARRAY:
                if(values.length < 1) {
                    return state.setFailed("no policy values were provided").log();
                }
                return primitive.removeEntries(state, values).log();
            case MAP:
                if(values.length < 1) {
                    return state.setFailed("no policy values were provided").log();
                }
                return PrimitivePolicyInstaller.removeMap(primitive, state, values);
            case VALUE:
            default:
                if(values.length > 1) {
                    return state.setFailed("policy values were provided, but none were expected").log();
                }
                return primitive.removeValue(state).log();
        }
    }

    private PrimitivePolicyInstaller constructPrimitiveInstaller() {
        switch(os) {
            case WINDOWS:
                return new WindowsPolicyInstaller();
            case MAC:
                return new MacPolicyInstaller();
            case LINUX:
            default:
                return new LinuxPolicyInstaller();
        }
    }

    private PolicyLocator constructPolicyLocator() {
        switch(alias.getAppAlias()) {
            case CHROMIUM:
                switch(os) {
                    case WINDOWS:
                        return new WindowsChromiumPolicyLocator();
                    case MAC:
                        return new MacChromiumPolicyLocator();
                    case LINUX:
                    default:
                        return new LinuxChromiumPolicyLocator();
                }
            case FIREFOX:
                // TODO: Implement firefox
            default:
                throw new UnsupportedOperationException();
        }
    }

    private PolicyState createPolicyState(Phase phase, PolicyState.Type type, String name) {
        WinReg.HKEY hkey = null;
        if(os == Os.WINDOWS) {
            hkey = scope == Installer.PrivilegeLevel.SYSTEM ?
                    WinReg.HKEY_LOCAL_MACHINE :
                    WinReg.HKEY_CURRENT_USER;
        }
        return new PolicyState(alias, phase, type, name, locator.getLocation(scope, alias), hkey);
    }

    private boolean isProhibited() {
        return os == Os.LINUX && scope == Installer.PrivilegeLevel.USER;
    }
}
