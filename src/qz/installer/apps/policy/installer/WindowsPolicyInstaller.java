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
        return state.setSucceeded(WindowsUtilities.deleteRegValue(root, key, name));
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
        Path key = state.getLocation();

        for(Map.Entry<String, Object> mapEntry : map.entrySet()) {
            if(!WindowsUtilities.addRegValue(root, key.toString(), mapEntry.getKey(), mapEntry.getValue())) {
                return state.setFailed(String.format("Error writing '%s'='%s' to %s\\%s", mapEntry.getKey(), mapEntry.getValue(), WindowsUtilities.getHkeyName(root), key));
            }
        }
        return state;
    }

    @Override
    public Object getValue(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();

        return WindowsUtilities.getRegValue(root, key.toString(), state.getName());
    }

    @Override
    public Object[] getEntries(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();

        ArrayList<Object> values = new ArrayList<>();

        // Iterate over the numerical values and add them to the arraylist
        int index = 0;
        Object val;
        // val is assigned, and the loop is continued if there was a non-null value returned
        while ((val = WindowsUtilities.getRegValue(root, key.toString(), Integer.toString(index))) != null) {
            values.add(val);
            index++;
        }
        return values.toArray(new Object[0]);
    }

    @Override
    public HashMap<String,Object> getMap(PolicyState state) {
        WinReg.HKEY root = state.getHkey();
        Path key = state.getLocation();

        HashMap<String, Object> values = new HashMap<>();
        TreeMap<String, Object> treeMap = WindowsUtilities.getRegistryValues(root, key.toString());

        if(treeMap != null) {
            values.putAll(treeMap);
        }

        return values;
    }
}
