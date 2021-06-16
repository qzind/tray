package org.dyorgio.jna.platform.mac;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public class NSApplication extends NSObject {
    static NativeLong klass = Objc.INSTANCE.objc_lookUpClass("NSApplication");
    static Pointer sharedApplication = Objc.INSTANCE.sel_getUid("sharedApplication");
    static Pointer activateIgnoringOtherApps = Objc.INSTANCE.sel_getUid("activateIgnoringOtherApps:");

    public static NSApplication sharedApplication() {
        return new NSApplication(Objc.INSTANCE.objc_msgSend(klass, sharedApplication));
    }

    public NSApplication(NativeLong handle) {
        super(handle);
    }

    public void activateIgnoringOtherApps(boolean flag) {
        Objc.INSTANCE.objc_msgSend(this.getId(), activateIgnoringOtherApps, flag);
    }
}