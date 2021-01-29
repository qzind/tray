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

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;

/**
 *
 * @author dyorgio
 */
public class NSObject {

    static final NativeLong objectClass = Foundation.INSTANCE.objc_getClass("NSObject");
    protected static final Pointer allocSel = Foundation.INSTANCE.sel_registerName("alloc");
    protected static final Pointer initSel = Foundation.INSTANCE.sel_registerName("init");
    protected static final Pointer releaseSel = Foundation.INSTANCE.sel_registerName("release");
    protected static final Pointer performSelectorOnMainThread$withObject$waitUntilDoneSel
            = Foundation.INSTANCE.sel_registerName("performSelectorOnMainThread:withObject:waitUntilDone:");

    final NativeLong id;

    public NSObject(NativeLong id) {
        this.id = id;
    }

    public final NativeLong getId() {
        return id;
    }

    public void release() {
        Foundation.INSTANCE.objc_msgSend(id, releaseSel);
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    public void performSelectorOnMainThread(Pointer selector, NativeLong object, boolean waitUntilDone) {
        Foundation.INSTANCE.objc_msgSend(id, //
                NSObject.performSelectorOnMainThread$withObject$waitUntilDoneSel, //
                selector, object, waitUntilDone);
    }

    static volatile boolean initialized = false;

    static void startNativeAppMainThread() {
        if (!initialized) {
            synchronized (NSObject.objectClass) {
                if (!initialized) {
                    try {
                        if(EventQueue.isDispatchThread()) {
                            Toolkit.getDefaultToolkit();
                        } else {
                            SwingUtilities.invokeAndWait(() -> Toolkit.getDefaultToolkit());
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (InvocationTargetException ex) {
                        // ignore
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }
}
