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
import java.util.concurrent.FutureTask;

/**
 *
 * @author dyorgio
 */
@SuppressWarnings("Convert2Lambda")
class FutureTaskCallback<T> extends NSObject {

    private static final NativeLong futureTaskCallbackClass = Foundation.INSTANCE.objc_allocateClassPair(objectClass, FutureTaskCallback.class.getSimpleName(), 0);
    private static final Pointer futureTaskCallbackSel = Foundation.INSTANCE.sel_registerName("futureTaskCallback");
    private static final Callback registerFutureTaskCallback;

    static {
        startNativeAppMainThread();
        registerFutureTaskCallback = new Callback() {
            @SuppressWarnings("unused")
            public void callback(Pointer self, Pointer selector) {
                if (selector.equals(futureTaskCallbackSel)) {
                    FutureTaskCallback action;

                    synchronized (callbackMap) {
                        action = callbackMap.remove(Pointer.nativeValue(self));
                    }

                    if (action != null) {
                        action.callable.run();
                    }
                }
            }
        };

        if (!Foundation.INSTANCE.class_addMethod(futureTaskCallbackClass,
                futureTaskCallbackSel, registerFutureTaskCallback, "v@:")) {
            throw new RuntimeException("Error initializing FutureTaskCallback as a objective C class");
        }

        Foundation.INSTANCE.objc_registerClassPair(futureTaskCallbackClass);
    }

    private static final HashMap<Long, FutureTaskCallback> callbackMap = new HashMap<Long, FutureTaskCallback>();

    private final FutureTask<T> callable;

    @SuppressWarnings("LeakingThisInConstructor")
    private FutureTaskCallback(FutureTask<T> callable) {
        super(Foundation.INSTANCE.class_createInstance(futureTaskCallbackClass, 0));
        this.callable = callable;
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

    static <T> void performOnMainThread(FutureTask<T> futureTask, boolean waitUntilDone) {
        new FutureTaskCallback(futureTask).performSelectorOnMainThread(futureTaskCallbackSel, null, waitUntilDone);
    }
}
