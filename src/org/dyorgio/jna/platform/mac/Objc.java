package org.dyorgio.jna.platform.mac;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public interface Objc extends Library {
    Objc INSTANCE = Native.load("objc", Objc.class);
    NativeLong objc_lookUpClass(String name);
    Pointer sel_getUid(String str);
    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, Object... args);
}