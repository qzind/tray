package qz.installer.apps.policy.installer;

import com.sun.jna.platform.win32.WinReg;

import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.WindowsUtilities;

import java.nio.file.Path;
import java.util.*;

public class WindowsPolicyInstaller implements PolicyInstaller.PrimitivePolicyInstaller {
    @Override

    public PolicyState putValue(PolicyState state, Object value) {
        WinReg.HKEY root = state.getHkey();
        String path = state.getLocation().toString();
        String name = state.getName();
        return state.setSucceeded(WindowsUtilities.addRegValue(root, path, name, value));
    }

    @Override
    public PolicyState removeValue(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        String key = state.getLocation().toString();
        String name = state.getName();
        switch(state.getType()) {
            case ARRAY:
            case MAP:
                return state.setSucceeded(WindowsUtilities.deleteRegKeyRecursively(root, key));
            case VALUE:
            default:
                return state.setSucceeded(WindowsUtilities.deleteRegValue(root, key, name));
        }
    }

    @Override
    public PolicyState putEntries(PolicyState state, Object ... values) {
        WinReg.HKEY root = state.getHkey();
        String key = state.getLocation().toString();
        // name is unused since it's part of the key
        for(Object value : values) {
            if(!WindowsUtilities.addNumberedRegValue(root, key, value)) {
                return state.setFailed();
            }
        }
        return state;
    }

    @Override
    public PolicyState removeEntries(PolicyState state, Object ... values) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();
        for(Object value : values) {
            if(!WindowsUtilities.deleteNumberedRegValue(root, key.toString(), value)) {
                return state.setFailed(String.format("Unable to delete %s from %s\\%s", value, WindowsUtilities.getHkeyName(root), key));
            }
        }
        return state;
    }

    @Override
    public PolicyState putMap(PolicyState state, Map<String,Object> map) {
        WinReg.HKEY root = state.getHkey();
        Path regKey = state.getLocation();

        for(Map.Entry<String, Object> mapEntry : map.entrySet()) {
            String mapKey = mapEntry.getKey();
            Object value = mapEntry.getValue();

            if(value instanceof Object[]) {
                String arrayKey = regKey.resolve(mapKey).toString();
                for(Object arrayValue : (Object[])value) {
                    if(!WindowsUtilities.addNumberedRegValue(root, arrayKey, arrayValue)) {
                        return state.setFailed(String.format("Error writing '%s'='%s' to %s\\%s", mapKey, Arrays.toString((Object[])value), WindowsUtilities.getHkeyName(root), regKey));
                    }
                }
                continue;
            }

            if(!WindowsUtilities.addRegValue(root, regKey.toString(), mapKey, value)) {
                return state.setFailed(String.format("Error writing '%s'='%s' to %s\\%s", mapKey, value, WindowsUtilities.getHkeyName(root), regKey));
            }
        }
        return state;
    }

    @Override
    public Object getValue(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();

        return state.failIfNull(WindowsUtilities.getRegValue(root, key.toString(), state.getName()));
    }

    @Override
    public Object[] getEntries(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();

        ArrayList<Object> values = new ArrayList<>();

        // Iterate over the numerical values and add them to the arraylist
        int index = 0;
        Object value;
        // val is assigned, and the loop is continued if there was a non-null value returned
        while ((value = WindowsUtilities.getRegValue(root, key.toString(), Integer.toString(index))) != null) {
            values.add(value);
            index++;
        }
        // this will never be null, but will show a log when empty
        return state.failIfNull(values.toArray(new Object[0]));
    }

    @Override
    public Map<String,Object> getMap(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path regKey = state.getLocation();

        TreeMap<String, Object> treeMap = WindowsUtilities.getRegistryValues(root, regKey.toString());
        HashMap<String,Object> map = new HashMap<>(state.failIfNull(treeMap));

        String[] subKeys = WindowsUtilities.getRegistryKeys(root, regKey.toString());
        if(subKeys != null) {
            for(String subKey : subKeys) {
                ArrayList<Object> values = new ArrayList<>();
                Object value;
                int index = 0;
                String arrayKey = regKey.resolve(subKey).toString();
                while ((value = WindowsUtilities.getRegValue(root, arrayKey, String.valueOf(index))) != null) {
                    values.add(value);
                    index++;
                }
                if(!values.isEmpty()) {
                    map.put(subKey, values.toArray(new Object[0]));
                }
            }
        }

        return map;
    }
}
