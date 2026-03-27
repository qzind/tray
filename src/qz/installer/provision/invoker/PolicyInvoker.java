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

public class PolicyInvoker implements Invokable {
    final Step step;

    public PolicyInvoker(Step step) {
        this.step = step;
    }

    @Override
    public boolean invoke() throws Exception {
        Installer.PrivilegeLevel scope = SystemUtilities.isAdmin() ? Installer.PrivilegeLevel.SYSTEM : Installer.PrivilegeLevel.USER;
        PolicyState.Type type = PolicyState.Type.parse(step.getFormat(), PolicyState.Type.VALUE);
        Object values;
        switch(type) {
            case ARRAY:
                if(step.getData().startsWith("[*")) {
                    values = step.getData();
                    break;
                }
                try {
                    values = toList(new JSONArray(step.getData())).toArray();
                } catch(JSONException ignore) {
                    // Treat non-arrays as single elements
                    values = step.getData();
                }
                break;
            case MAP:
                values = toMap(new JSONObject(step.getData()));
                break;
            case VALUE:
            default:
                values = step.getData();
                break;
        }

        boolean success = true;
        for(AppFamily.AppVariant appVariant : step.getApp().getVariants()) {
            PolicyInstaller installer = new PolicyInstaller(scope, appVariant);
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

    public static List<Object> toList(JSONArray array) throws Exception {
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
        return list;
    }
}
