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
import com.sun.jna.platform.mac.CoreFoundation;
import java.nio.charset.Charset;

/**
 *
 * @author dyorgio
 */
public class NSString extends NSObject {

    public static final Charset UTF_16LE_CHARSET = Charset.forName("UTF-16LE");

    private static final NativeLong stringCls = Foundation.INSTANCE.objc_getClass("NSString");
    private static final Pointer stringSel = Foundation.INSTANCE.sel_registerName("string");
    private static final Pointer initWithBytesLengthEncodingSel = Foundation.INSTANCE.sel_registerName("initWithBytes:length:encoding:");
    private static final long NSUTF16LittleEndianStringEncoding = 0x94000100;

    public NSString(String string) {
        this(fromJavaString(string));
    }

    public NSString(NativeLong id) {
        super(id);
    }

    @Override
    public String toString() {
        if (FoundationUtil.isNull(this)) {
            return null;
        }
        CoreFoundation.CFStringRef cfString = new CoreFoundation.CFStringRef(new Pointer(id.longValue()));
        try {
            return CoreFoundation.INSTANCE.CFStringGetLength(cfString).intValue() > 0 ? cfString.stringValue() : "";
        } finally {
            cfString.release();
        }
    }

    private static NativeLong fromJavaString(String s) {
        if (s.isEmpty()) {
            return Foundation.INSTANCE.objc_msgSend(stringCls, stringSel);
        }

        byte[] utf16Bytes = s.getBytes(UTF_16LE_CHARSET);
        return Foundation.INSTANCE.objc_msgSend(Foundation.INSTANCE.objc_msgSend(stringCls, allocSel),
                initWithBytesLengthEncodingSel, utf16Bytes, utf16Bytes.length, NSUTF16LittleEndianStringEncoding);
    }
}
