/*
 * The MIT License
 *
 * Copyright 2020 dyorgio.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.dyorgio.jna.platform.mac;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author dyorgio
 */
public interface Foundation extends Library {

    Foundation INSTANCE = Native.load("Foundation", Foundation.class);

    NativeLong class_getInstanceVariable(NativeLong classPointer, String name);

    NativeLong object_getIvar(NativeLong target, NativeLong ivar);

    NativeLong objc_getClass(String className);

    NativeLong objc_allocateClassPair(NativeLong superClass, String name, long extraBytes);

    void objc_registerClassPair(NativeLong clazz);

    NativeLong class_createInstance(NativeLong clazz, int extraBytes);

    boolean class_addMethod(NativeLong clazz, Pointer selector, Callback callback, String types);

    NativeLong objc_msgSend(NativeLong receiver, Pointer selector);

    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, Pointer obj);

    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, NativeLong objAddress);

    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, boolean boolArg);

    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, double doubleArg);

    // Used by NSObject.performSelectorOnMainThread
    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, Pointer selectorDst, NativeLong objAddress, boolean wait);

    // Used by NSString.fromJavaString
    NativeLong objc_msgSend(NativeLong receiver, Pointer selector, byte[] bytes, int len, long encoding);

    Pointer sel_registerName(String selectorName);
}
