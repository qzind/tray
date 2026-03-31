package qz.installer.apps.policy;

import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.provision.params.Os;
import qz.common.Sluggable;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.installer.LinuxPolicyInstaller;
import qz.installer.apps.policy.installer.MacPolicyInstaller;
import qz.installer.apps.policy.installer.WindowsPolicyInstaller;
import qz.installer.apps.policy.locator.LinuxPolicyLocator;
import qz.installer.apps.policy.locator.MacPolicyLocator;
import qz.installer.apps.policy.locator.WindowsPolicyLocator;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

import java.nio.file.Path;
import java.util.*;

public class PolicyInstaller {
    public enum Phase implements Sluggable {
        INSTALL,
        UNINSTALL,
        QUERY;
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
        enum AppType {
            NATIVE, // used by all platforms
            FLATPAK, // linux only
            SNAP; // linux only

            /**
             * Identifies if this <code>AppFamily</code> policy type is supported on this
             * <code>Os</code> and <code>PrivilegeLevel</code>
             */
            @SuppressWarnings("BooleanMethodIsAlwaysInverted")
            boolean isSupported(AppFamily appFamily) {
                if(!List.of(new AppFamily[] { AppFamily.CHROMIUM, AppFamily.FIREFOX }).contains(appFamily)) {
                    return false;
                }

                switch(this) {
                    case NATIVE:
                        return !SystemUtilities.isLinux() || SystemUtilities.isAdmin();
                    case FLATPAK:
                        return SystemUtilities.isLinux() && appFamily == AppFamily.CHROMIUM;
                    case SNAP:
                        return UnixUtilities.isUbuntu() && SystemUtilities.isAdmin();
                    default:
                        return false;
                }
            }

            @SuppressWarnings("BooleanMethodIsAlwaysInverted")
            public boolean isSupported(AppFamily.AppVariant appVariant) {
                return isSupported(appVariant.getAppFamily());
            }
        }

        Path getLocation(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, AppType appType);
    }

    final private static Logger log = LogManager.getLogger(PolicyInstaller.class);

    final private Os os;
    final private Installer.PrivilegeLevel scope;
    final private PolicyLocator.AppType appType;
    final private AppFamily.AppVariant appVariant;

    final private PrimitivePolicyInstaller primitive;
    final private PolicyLocator locator;


    public PolicyInstaller(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, PolicyLocator.AppType appType) {
        this(SystemUtilities.getOs(), scope, appVariant, appType);
    }

    public PolicyInstaller(Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant) {
        this(SystemUtilities.getOs(), scope, appVariant, PolicyLocator.AppType.NATIVE);
    }

    public PolicyInstaller(Os os, Installer.PrivilegeLevel scope, AppFamily.AppVariant appVariant, PolicyLocator.AppType appType) {
        this.os = os;
        this.scope = scope;
        this.appType = appType;
        this.appVariant = appVariant;

        this.primitive = constructPrimitiveInstaller();
        this.locator = constructPolicyLocator();
    }

    /**
     * If an Object[] is received through this Object overload, cast it to an Object[] before
     * forwarding to the varargs overload. All non-array values pass along unchanged.
     */
    public PolicyState install(PolicyState.Type type, String name, Object value) {
        Object[] unboxed = value instanceof Object[] ? (Object[])value: new Object[]{value};
        return install(type, name, unboxed);
    }

    public PolicyState install(PolicyState.Type type, String name, Object ... values) {
        PolicyState state = createPolicyState(Phase.INSTALL, type, name);

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

    /**
     * If an Object[] is received through this Object overload, cast it to an Object[] before
     * forwarding to the varargs overload. All non-array values pass along unchanged.
     */
    public PolicyState uninstall(PolicyState.Type type, String name, Object value) {
        Object[] unboxed = value instanceof Object[] ? (Object[])value: new Object[]{value};
        return uninstall(type, name, unboxed);
    }

    public PolicyState uninstall(PolicyState.Type type, String name, Object ... values) {
        PolicyState state = createPolicyState(Phase.UNINSTALL, type, name);

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

    public Map<String, Object> readMap(PolicyState.Type type, String name) {
        return getMap(createPolicyState(Phase.QUERY, type, name));
    }

    @SuppressWarnings("unused")
    public Object readValue(PolicyState.Type type, String name) {
        return getValue(createPolicyState(Phase.QUERY, type, name));
    }

    @SuppressWarnings("unused")
    public Object[] readArray(PolicyState.Type type, String name) {
        return getEntries(createPolicyState(Phase.QUERY, type, name));
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
        return new PolicyState(scope, appVariant, phase, type, name, locator.getLocation(scope, appVariant, appType), hkey);
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
