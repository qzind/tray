package qz.installer.apps.policy.installer;

import com.sun.jna.platform.win32.WinReg;

import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.utils.WindowsUtilities;

import java.nio.file.Path;

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
}
