package org.dyorgio.jna.platform.mac;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

public interface CoreFoundation extends com.sun.jna.platform.mac.CoreFoundation {
    CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);

    void CFDictionaryAddValue(com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef theDict, PointerType key, PointerType value);
    void CFDictionaryGetKeysAndValues(com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef theDict, PointerByReference keys, Pointer[] values);
}