package qz.installer.apps.policy;

import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.provision.params.Os;
import qz.common.Sluggable;
import qz.installer.Installer;
import qz.installer.apps.exception.UnsupportedPolicyException;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.installer.LinuxPolicyInstaller;
import qz.installer.apps.policy.installer.MacPolicyInstaller;
import qz.installer.apps.policy.installer.WindowsPolicyInstaller;
import qz.installer.apps.policy.locator.LinuxPolicyLocator;
import qz.installer.apps.policy.locator.MacPolicyLocator;
import qz.installer.apps.policy.locator.WindowsPolicyLocator;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.*;

public class PolicyInstaller {
    public enum Phase implements Sluggable {
        INSTALL,
        UNINSTALL;
        public String slug() {
            return Sluggable.slugOf(this);
        }
    }

    public interface PrimitivePolicyInstaller {
        PolicyState putValue(PolicyState state, Object value);
        PolicyState removeValue(PolicyState state);
        PolicyState putEntries(PolicyState state, Object ... values);
        PolicyState removeEntries(PolicyState state, Object ... values);
        PolicyState putMap(PolicyState state, Map<String, Object> map);
        Object getValue(PolicyState state);
        Object[] getEntries(PolicyState state);
        Map<String, Object> getMap(PolicyState state);

        @SuppressWarnings("unchecked")
        static PolicyState putMap(PrimitivePolicyInstaller primitive, PolicyState state, Object ... values) {
            for(Object value : values) {
                if(primitive.putMap(state, (Map<String,Object>)value).hasFailed()) {
                    break;
                }
            }
            return state;
        }

        static PolicyState removeMap(PrimitivePolicyInstaller primitive, PolicyState state, Object ... values) {
            for(Object value : values) {
                Map<String,Object> provided = objectToMap(value);
                Map<String,Object> remaining = primitive.getMap(state);
                if (remaining == null) return state.setSucceeded("suspiciously succeeded, the map is null");
                int removeCount = 0;
                for(String key : provided.keySet()) {
                    if(!remaining.containsKey(key)) continue;
                    if (provided.get(key) instanceof Object[] && remaining.get(key) instanceof Object[]) {
                        int size = ((Object[])remaining.get(key)).length;
                        HashSet<Object> remainingSet = new HashSet<>(List.of((Object[])remaining.get(key)));
                        for(Object o : List.of((Object[])provided.get(key))) {
                            remainingSet.remove(o);
                        }

                        if (remainingSet.isEmpty()) {
                            remaining.remove(key);
                        } else {
                            remaining.put(key, remainingSet.toArray());
                        }

                        if(size != remainingSet.size()) {
                            removeCount++;
                        }
                    } else if (remaining.remove(key) != null) {
                        removeCount++;
                    }
                }

                if (removeCount == 0) return state.setSucceeded("nothing to remove");
                if (primitive.removeValue(state).hasFailed()) return state; // delete failed
                if (remaining.isEmpty()) return state.setSucceeded("nothing to write");
                if (primitive.putMap(state, remaining).hasFailed()) return state;
            }
            return state;
        }
    }

    public interface PolicyLocator {
        Path getLocation(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant);
    }

    final private static Logger log = LogManager.getLogger(PolicyInstaller.class);

    final private Os os;
    final private Installer.PrivilegeLevel scope;
    final private AppFamily.AppVariant appVariant;

    final private PrimitivePolicyInstaller primitive;
    final private PolicyLocator locator;

    public PolicyInstaller(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant) {
        this(SystemUtilities.getOs(), scope, appVariant);
    }

    public PolicyInstaller(Os os, Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant) {
        this.os = os;
        this.scope = scope;
        this.appVariant = appVariant;

        this.primitive = constructPrimitiveInstaller();
        this.locator = constructPolicyLocator();
    }

    public PolicyState install(PolicyState.Type type, String name, Object ... values) {
        PolicyState state = createPolicyState(Phase.INSTALL, type, name);

        if(isProhibited()) {
            return state.setFailed(new UnsupportedPolicyException("User mode policies are not yet supported on Linux")).log();
        }

        if(values.length < 1) {
            return state.setFailed("no policy value was provided").log();
        }
        switch(state.getType()) {
            case ARRAY:
                return primitive.putEntries(state, values).log();
            case MAP:
                return PrimitivePolicyInstaller.putMap(primitive, state, pairToMapArray(values)).log();
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
            return state.setFailed(new UnsupportedPolicyException("User mode policies are not yet supported on Linux")).log();
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
                return PrimitivePolicyInstaller.removeMap(primitive, state, pairToMapArray(values)).log();
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

    /**
     * If the first two <code>values</code> are a String and Object,
     * wraps them into a new <code>HashMap&lt;String,Object&gt;[]</code>
     * otherwise returns them unmodified.
     */
    private static Object[] pairToMapArray(Object ... values) {
        if(values[0] instanceof String && values.length == 2) {
            // convert to HashMap
            HashMap<String, Object> value = new HashMap<>();
            value.put((String)values[0], values[1]);
            // wrap as Object[]
            return new Object[] { value };
        }
        return values;
    }

    /**
     * Safe cast of <code>Double</code> to <code>Float</code> for consistency across platforms.
     * Returns <code>value</code> unmodified if it is not a <code>Double</code>.
     */
    public static Object normalizeFloats(Object value) {
        if (value instanceof Double) {
            value = ((Double)value).floatValue();
        }
        return value;
    }

    /**
     * Safe cast of <code>Object</code> to <code>Map&lt;String,Object&gt;</code>
     * falling back to an empty <code>Map</code> on failure.
     */
    @SuppressWarnings("unchecked,rawtypes")
    private static Map<String, Object> objectToMap(Object value) {
        if(value instanceof Map) {
            Map map = (Map)value;
            Iterator<Object> iterator = map.keySet().iterator();
            if(iterator.hasNext() && iterator.next() instanceof String) {
                return (Map<String,Object>)value;
            }
        }
        log.warn("Could not cast '{}' to Map<String,Object>, returning empty Map instead", value.getClass());
        return new HashMap<>();
    }

    private PolicyLocator constructPolicyLocator() {
        switch(os) {
            case WINDOWS:
                return new WindowsPolicyLocator();
            case MAC:
                return new MacPolicyLocator();
            case LINUX:
            default:
                return new LinuxPolicyLocator();
        }
    }

    private PolicyState createPolicyState(Phase phase, PolicyState.Type type, String name) {
        WinReg.HKEY hkey = null;
        if(os == Os.WINDOWS) {
            hkey = scope == Installer.PrivilegeLevel.SYSTEM ?
                    WinReg.HKEY_LOCAL_MACHINE :
                    WinReg.HKEY_CURRENT_USER;
        }
        return new PolicyState(scope, appVariant, phase, type, name, locator.getLocation(scope, appVariant), hkey);
    }

    private boolean isProhibited() {
        return os == Os.LINUX && scope == Installer.PrivilegeLevel.USER;
    }

    //
    // Package-private methods, for unit tests only
    //

    Object getValue(PolicyState state) {
        return primitive.getValue(state);
    }

    Object[] getEntries(PolicyState state) {
        return primitive.getEntries(state);
    }

    Map<String, Object> getMap(PolicyState state) {
        return primitive.getMap(state);
    }
}
