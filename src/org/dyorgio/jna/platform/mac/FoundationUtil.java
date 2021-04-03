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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 *
 * @author dyorgio
 */
public final class FoundationUtil {

    private static final Foundation FOUNDATION = Foundation.INSTANCE;

    public static final NativeLong NULL = new NativeLong(0l);

    private FoundationUtil() {
    }

    public static boolean isNull(NativeLong id) {
        return NULL.equals(id);
    }

    public static boolean isNull(NSObject object) {
        return NULL.equals(object.id);
    }

    public static boolean isFalse(NativeLong id) {
        return NULL.equals(id);
    }

    public static boolean isTrue(NativeLong id) {
        return !NULL.equals(id);
    }

    public static NativeLong invoke(NativeLong id, String selector) {
        return FOUNDATION.objc_msgSend(id, Foundation.INSTANCE.sel_registerName(selector));
    }

    public static NativeLong invoke(NativeLong id, String selector, boolean boolArg) {
        return FOUNDATION.objc_msgSend(id, Foundation.INSTANCE.sel_registerName(selector), boolArg);
    }

    public static NativeLong invoke(NativeLong id, String selector, double doubleArg) {
        return FOUNDATION.objc_msgSend(id, Foundation.INSTANCE.sel_registerName(selector), doubleArg);
    }

    public static NativeLong invoke(NativeLong id, String selector, NativeLong objAddress) {
        return FOUNDATION.objc_msgSend(id, Foundation.INSTANCE.sel_registerName(selector), objAddress);
    }

    public static NativeLong invoke(NativeLong id, Pointer selectorPointer) {
        return FOUNDATION.objc_msgSend(id, selectorPointer);
    }

    public static NativeLong invoke(NativeLong id, Pointer selectorPointer, NativeLong objAddress) {
        return FOUNDATION.objc_msgSend(id, selectorPointer, objAddress);
    }

    public static void runOnMainThreadAndWait(Runnable runnable) throws InterruptedException, ExecutionException {
        runOnMainThread(runnable, true);
    }

    public static FutureTask runOnMainThread(Runnable runnable, boolean waitUntilDone) {
        FutureTask futureTask = new FutureTask(runnable, null);
        FutureTaskCallback.performOnMainThread(futureTask, waitUntilDone);
        return futureTask;
    }

    public static <T> T callOnMainThreadAndWait(Callable<T> callable) throws InterruptedException, ExecutionException {
        return callOnMainThread(callable, true).get();
    }

    public static <T> FutureTask<T> callOnMainThread(Callable<T> callable, boolean waitUntilDone) {
        FutureTask<T> futureTask = new FutureTask(callable);
        FutureTaskCallback.performOnMainThread(futureTask, waitUntilDone);
        return futureTask;
    }
}
