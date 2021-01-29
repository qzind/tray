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
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.util.HashMap;

/**
 *
 * @author dyorgio
 */
@SuppressWarnings("Convert2Lambda")
public final class ActionCallback extends NSObject {

    private static final NativeLong actionCallbackClass = Foundation.INSTANCE.objc_allocateClassPair(objectClass, ActionCallback.class.getSimpleName(), 0);
    private static final Pointer actionCallbackSel = Foundation.INSTANCE.sel_registerName("actionCallback");
    private static final Pointer setTargetSel = Foundation.INSTANCE.sel_registerName("setTarget:");
    private static final Pointer setActionSel = Foundation.INSTANCE.sel_registerName("setAction:");
    private static final Callback registerActionCallback;

    static {
        startNativeAppMainThread();
        registerActionCallback = new Callback() {
            @SuppressWarnings("unused")
            public void callback(Pointer self, Pointer selector) {
                if (selector.equals(actionCallbackSel)) {
                    ActionCallback action;

                    synchronized (callbackMap) {
                        action = callbackMap.get(Pointer.nativeValue(self));
                    }

                    if (action != null) {
                        action.runnable.run();
                    }
                }
            }
        };

        if (!Foundation.INSTANCE.class_addMethod(actionCallbackClass,
                actionCallbackSel, registerActionCallback, "v@:")) {
            throw new RuntimeException("Error initializing ActionCallback as a objective C class");
        }

        Foundation.INSTANCE.objc_registerClassPair(actionCallbackClass);
    }

    private static final HashMap<Long, ActionCallback> callbackMap = new HashMap<Long, ActionCallback>();

    private final Runnable runnable;

    @SuppressWarnings("LeakingThisInConstructor")
    public ActionCallback(Runnable callable) {
        super(Foundation.INSTANCE.class_createInstance(actionCallbackClass, 0));
        this.runnable = callable;
        synchronized (callbackMap) {
            callbackMap.put(getId().longValue(), this);
        }
    }

    @Override
    public void release() {
        synchronized (callbackMap) {
            callbackMap.remove(getId().longValue());
        }
        super.release();
    }

    public void installActionOnNSControl(NativeLong nsControl) {
        Foundation.INSTANCE.objc_msgSend(nsControl, setTargetSel, id);
        Foundation.INSTANCE.objc_msgSend(nsControl, setActionSel, actionCallbackSel);
    }
}
