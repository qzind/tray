package qz.installer.provision.invoker;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.build.provision.Step;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.SystemUtilities;

import java.util.*;

import static qz.installer.apps.policy.PolicyInstaller.PolicyLocator.*;

public class PolicyInvoker implements Invokable {
    final Step step;
    final PolicyState.Type type;

    public PolicyInvoker(Step step) {
        this.step = step;
        this.type = PolicyState.Type.parse(step.getFormat(), PolicyState.Type.VALUE);
    }

    @Override
    public boolean invoke() throws Exception {
        Installer.PrivilegeLevel scope = SystemUtilities.isAdmin() ? Installer.PrivilegeLevel.SYSTEM : Installer.PrivilegeLevel.USER;

        Object values;
        Object stepData = step.getDataObject();
        if(stepData instanceof String) {
            values = parseValuesFromString(step.getDataString());
        } else if(stepData instanceof JSONArray) {
            values = toList((JSONArray)stepData);
        } else if(stepData instanceof  JSONObject) {
            values = toMap((JSONObject)stepData);
        } else {
            // primitive, etc
            values = stepData;
        }

        AppType[] appTypes = AppType.collect(step.getApp());
        if(appTypes.length == 0) {
            return false;
        }
        boolean success = true;
        for(AppFamily.AppVariant appVariant : step.getApp().getVariants()) {
            for(AppType appType : appTypes) {
                PolicyInstaller installer = new PolicyInstaller(scope, appVariant, appType);
                PolicyState state;
                switch(step.getPhase()) {
                    case UNINSTALL:
                        if (values instanceof Object[]) {
                            state = installer.uninstall(type, step.getName(), (Object[])values);
                        } else {
                            state = installer.uninstall(type, step.getName(), values);
                        }
                        if (state.hasFailed()) {
                            success = false;
                        }
                        break;
                    case INSTALL:
                    case CERTGEN:
                    default:
                        if (values instanceof Object[]) {
                            state = installer.install(type, step.getName(), (Object[])values);
                        } else {
                            state = installer.install(type, step.getName(), values);
                        }
                        if (state.hasFailed()) {
                            success = false;
                        }
                }
            }
        }
        return success;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(JSONObject json) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }
            map.put(key, value);
        }
        return map;
    }

    public static Object[] toList(JSONArray array) throws Exception {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }
            list.add(value);
        }
        return list.toArray();
    }

    /**
     * Parse a String JSON value with special handling for nested JSON
     */
    private Object parseValuesFromString(String dataString) throws Exception {
        switch(type) {
            case MAP:
                return toMap(new JSONObject(dataString));
            case ARRAY:
                if (!dataString.startsWith("[*")) {
                    try {
                        return toList(new JSONArray(dataString));
                    } catch(JSONException ignore) {}
                }
                // fallthrough: treat non-arrays as single elements
            case VALUE:
            default:
        }
        return dataString;
    }

}
