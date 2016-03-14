/**
 * @author Antoni Ten Monro's
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 * Copyright (C) 2013 Antoni Ten Monro's
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.common;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Provides a simple and efficient way for concatenating byte arrays, similar
 * in purpose to <code>StringBuilder</code>. Objects of this class are not
 * thread safe and include no synchronization
 *
 * @author Antoni Ten Monro's
 */

@SuppressWarnings("UnusedDeclaration") //Library class, may be used outside of project context
public final class ByteArrayBuilder {

    private ArrayList<byte[]> buffer;

    private int length = 0;

    private byte[] contents = null;

    /**
     * Gives the number of bytes currently stored in this <code>ByteArrayBuilder</code>
     *
     * @return the number of bytes in the <code>ByteArrayBuilder</code>
     */
    public int getLength() {
        return length;
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code> and sets initial capacity to 10
     */
    public ByteArrayBuilder() {
        buffer = new ArrayList<>(10);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code> and sets initial capacity to
     * <code>initialCapacity</code>
     *
     * @param initialCapacity the initial capacity of the <code>ByteArrayBuilder</code>
     */
    public ByteArrayBuilder(int initialCapacity) {
        buffer = new ArrayList<>(initialCapacity);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code>, sets initial capacity to 10
     * and appends <code>initialContents</code>
     *
     * @param initialContents the initial contents of the ByteArrayBuilder
     */
    public ByteArrayBuilder(byte[] initialContents) {
        this();
        append(initialContents);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code>, sets initial capacity to
     * <code>initialContents</code> and appends <code>initialContents</code>
     *
     * @param initialContents the initial contents of the <code>ByteArrayBuilder</code>
     * @param initialCapacity the initial capacity of the <code>ByteArrayBuilder</code>
     */
    public ByteArrayBuilder(byte[] initialContents, int initialCapacity) {
        this(initialCapacity);
        append(initialContents);
    }

    private void resetContents() {
        contents = null;
    }

    /**
     * Empties the <code>ByteArrayBuilder</code>
     */
    public void clear() {
        length = 0;
        resetContents();
        buffer.clear();
    }

    /**
     * Appends a new byte array to this <code>ByteArrayBuilder</code>.
     * Returns this same object to allow chaining calls
     *
     * @param bytes the byte array to append
     * @return this <code>ByteArrayBuilder</code>
     */
    public final ByteArrayBuilder append(byte[] bytes) {
        resetContents();
        length += bytes.length;
        buffer.add(bytes);
        return this;
    }

    /**
     * Convenience method for append(byte[]) combined with a StringBuffer of specified
     * charset
     *
     * @param string  the String to append
     * @param charset the Charset of the String
     * @return this <code>ByteArrayBuilder</code>
     */
    public final ByteArrayBuilder append(String string, Charset charset) throws UnsupportedEncodingException {
        return append(string.getBytes(charset.name()));
    }

    /**
     * Convenience method for append(byte[]) combined with a String of specified
     * charset
     *
     * @param stringBuilder the StringBuilder to append
     * @param charset       the Charset of the StringBuilder
     * @return this <code>ByteArrayBuilder</code>
     */
    public final ByteArrayBuilder append(StringBuilder stringBuilder, Charset charset) throws UnsupportedEncodingException {
        return append(stringBuilder.toString(), charset);
    }

    /**
     * Returns the full contents of this <code>ByteArrayBuilder</code> as
     * a single <code>byte</code> array. The result is cached, so multiple
     * calls with no changes to the contents of the <code>ByteArrayBuilder</code>
     * are efficient.
     *
     * @return The contents of this <code>ByteArrayBuilder</code> as a single <code>byte</code> array
     */
    public byte[] getByteArray() {
        if (contents == null) {
            contents = new byte[getLength()];
            int pos = 0;
            for(byte[] bs : buffer) {
                System.arraycopy(bs, 0, contents, pos, bs.length);
                pos += bs.length;
            }
        }

        return contents;
    }
}
