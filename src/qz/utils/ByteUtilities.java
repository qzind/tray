/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import qz.common.ByteArrayBuilder;
import qz.common.Constants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Place for all raw static byte conversion functions.
 * Especially useful for converting hexadecimal strings to byte arrays,
 * byte arrays to hexadecimal strings,
 * byte arrays to integer array conversions, etc.
 *
 * @author Tres Finocchiaro
 */
public class ByteUtilities {

    /**
     * Converts a hexadecimal string to a byte array.
     * <p/>
     * This is especially useful for special characters that are appended via
     * JavaScript, specifically "\0" or the {@code NUL} character, which
     * will terminate a JavaScript string early.
     *
     * @param hex Base 16 String to covert to byte array.
     */
    public static byte[] hexStringToByteArray(String hex) throws NumberFormatException {
        byte[] data = new byte[0];
        if (hex != null && !hex.isEmpty()) {
            String[] split;
            if (hex.length() > 2) {
                if (hex.length() >= 3 && hex.contains("x")) {
                    hex = hex.startsWith("x")? hex.substring(1):hex;
                    hex = hex.endsWith("x")? hex.substring(0, hex.length() - 1):hex;
                    split = hex.split("x");
                } else {
                    split = hex.split("(?<=\\G..)");
                }

                data = new byte[split.length];
                for(int i = 0; i < split.length; i++) {
                    Integer signedByte = Integer.parseInt(split[i], 16);
                    data[i] = (byte)(signedByte & 0xFF);
                }
            } else if (hex.length() == 2) {
                data = new byte[] {Byte.parseByte(hex)};
            }
        }

        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, true);
    }

    /**
     * Converts an array of bytes to its hexadecimal form.
     *
     * @param bytes     Bytes to be converted.
     * @param upperCase Whether the hex string should be UPPER or lower case.
     */
    public static String bytesToHex(byte[] bytes, boolean upperCase) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for(int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = Constants.HEXES_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = Constants.HEXES_ARRAY[v & 0x0F];
        }

        if (upperCase) {
            return new String(hexChars);
        }
        return new String(hexChars).toLowerCase();
    }

    /**
     * Iterates through byte array finding matches of {@code match} inside {@code target}.
     * <p/>
     * TODO: Make this natively Iterable.
     *
     * @param target Byte array to search.
     * @param match  Sub-array to match inside {@code target}.
     * @return Array of starting indices for matched values.
     */
    public static Integer[] indicesOfMatches(byte[] target, byte[] match) {
        if (target == null || match == null || target.length == 0
                || match.length == 0 || match.length > target.length) {
            return new Integer[0];
        }

        LinkedList<Integer> indexes = new LinkedList<>();

        // Find instances of byte list
        outer:
        for(int i = 0; i < target.length - match.length + 1; i++) {
            for(int j = 0; j < match.length; j++) {
                if (target[i + j] != match[j]) {
                    continue outer;
                }
            }

            indexes.add(i);
        }

        return indexes.toArray(new Integer[indexes.size()]);
    }

    /**
     * Splits the {@code src} byte array after every {@code count}-th instance of the supplied {@code pattern} byte array.
     * <p/>
     * This is useful for large print batches that need to be split up,
     * (for example) after the P1 or ^XO command has been issued.
     * <p/>
     * TODO:
     * A rewrite of this would be a proper {@code Iteratable} interface
     * paired with an {@code Iterator} that does this automatically
     * and would eliminate the need for a {@code indicesOfMatches()} function.
     *
     * @param src     Array to split.
     * @param pattern Pattern to determine where split should occur.
     * @param count   Number of matches between splits.
     */
    public static List<ByteArrayBuilder> splitByteArray(byte[] src, byte[] pattern, int count) throws NullPointerException, IndexOutOfBoundsException, ArrayStoreException {
        if (count < 1) { throw new IllegalArgumentException("Count cannot be less than 1"); }

        List<ByteArrayBuilder> byteArrayList = new ArrayList<>();
        ByteArrayBuilder builder = new ByteArrayBuilder();

        Integer[] split = indicesOfMatches(src, pattern);

        int counted = 1;
        int prev = 0;

        for(int i : split) {
            //copy everything from the last pattern (or the start) to the end of this pattern
            byte[] temp = new byte[i - prev + pattern.length];
            System.arraycopy(src, prev, temp, 0, temp.length);
            builder.append(temp);

            //if we have 'count' matches, add it to list and start a new builder
            if (counted < count) {
                counted++;
            } else {
                byteArrayList.add(builder);
                builder = new ByteArrayBuilder();
                counted = 1;
            }

            prev = i + pattern.length;
        }

        //include any builder matches below 'count'
        if (!byteArrayList.contains(builder) && builder.getLength() > 0 ) {
            byteArrayList.add(builder);
        }

        return byteArrayList;
    }

    /**
     * Converts an integer array to a String representation of a hexadecimal number.
     *
     * @param raw Numbers to be converted to hex.
     * @return Hex string representation.
     */
    public static String getHexString(int[] raw) {
        if (raw == null) { return null; }

        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for(final int i : raw) {
            hex.append(Constants.HEXES.charAt((i & 0xF0) >> 4)).append(Constants.HEXES.charAt((i & 0x0F)));
        }

        return hex.toString();
    }

}
