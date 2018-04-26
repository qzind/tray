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

import org.apache.commons.lang3.ArrayUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simple and efficient way for concatenating byte arrays, similar
 * in purpose to <code>StringBuilder</code>. Objects of this class are not
 * thread safe and include no synchronization
 *
 * @author Antoni Ten Monro's
 */
@SuppressWarnings("UnusedDeclaration") //Library class
public final class ByteArrayBuilder {

    private List<Byte> buffer;


    /**
     * Creates a new <code>ByteArrayBuilder</code> and sets initial capacity to 10
     */
    public ByteArrayBuilder() {
        this(null);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code> and sets initial capacity to
     * <code>initialCapacity</code>
     *
     * @param initialCapacity the initial capacity of the <code>ByteArrayBuilder</code>
     */
    public ByteArrayBuilder(int initialCapacity) {
        this(null, initialCapacity);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code>, sets initial capacity to 10
     * and appends <code>initialContents</code>
     *
     * @param initialContents the initial contents of the ByteArrayBuilder
     */
    public ByteArrayBuilder(byte[] initialContents) {
        this(initialContents, 16);
    }

    /**
     * Creates a new <code>ByteArrayBuilder</code>, sets initial capacity to
     * <code>initialContents</code> and appends <code>initialContents</code>
     *
     * @param initialContents the initial contents of the <code>ByteArrayBuilder</code>
     * @param initialCapacity the initial capacity of the <code>ByteArrayBuilder</code>
     */
    public ByteArrayBuilder(byte[] initialContents, int initialCapacity) {
        buffer = new ArrayList<>(initialCapacity);
        if (initialContents != null) {
            append(initialContents);
        }
    }

    /**
     * Empties the <code>ByteArrayBuilder</code>
     */
    public void clear() {
        buffer.clear();
    }

    /**
     * Clear a portion of the <code>ByteArrayBuilder</code>
     *
     * @param startIndex Starting index, inclusive
     * @param endIndex   Ending index, exclusive
     */
    public final void clearRange(int startIndex, int endIndex) {
        buffer.subList(startIndex, endIndex).clear();
    }

    /**
     * Gives the number of bytes currently stored in this <code>ByteArrayBuilder</code>
     *
     * @return the number of bytes in the <code>ByteArrayBuilder</code>
     */
    public int getLength() {
        return buffer.size();
    }

    /**
     * Appends a new byte array to this <code>ByteArrayBuilder</code>.
     * Returns this same object to allow chaining calls
     *
     * @param bytes the byte array to append
     * @return this <code>ByteArrayBuilder</code>
     */
    public final ByteArrayBuilder append(byte[] bytes) {
        for(byte b : bytes) {
            buffer.add(b);
        }
        return this;
    }

    public final ByteArrayBuilder append(List<Byte> bytes) {
        for(byte b : bytes) {
            buffer.add(b);
        }
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
     * a single <code>byte</code> array.
     *
     * @return The contents of this <code>ByteArrayBuilder</code> as a single <code>byte</code> array
     */
    public byte[] getByteArray() {
        return ArrayUtils.toPrimitive(buffer.toArray(new Byte[buffer.size()]));
    }
}
