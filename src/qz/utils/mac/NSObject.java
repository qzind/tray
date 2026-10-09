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
package qz.utils.mac;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author dyorgio
 */
public class NSObject {
    protected static final Pointer allocSel = Foundation.INSTANCE.sel_registerName("alloc");
    protected static final Pointer releaseSel = Foundation.INSTANCE.sel_registerName("release");

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
    @SuppressWarnings({"FinalizeDeclaration", "removal"})
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }
}
