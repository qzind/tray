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

import com.ibm.icu.text.ArabicShapingException;
import org.apache.commons.ssl.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.printer.action.raw.PixelGrid;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Place for all raw static byte conversion functions.
 * Especially useful for converting hexadecimal strings to byte arrays,
 * byte arrays to hexadecimal strings,
 * byte arrays to integer array conversions, etc.
 *
 * @author Tres Finocchiaro
 */
public class ByteUtilities {
    private static final Logger log = LogManager.getLogger(ByteUtilities.class);

    public enum Endian {
        BIG, LITTLE
    }

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

    public static String toString(PrintingUtilities.Flavor flavor, byte[] bytes) {
        switch(flavor) {
            case BASE64:
                return Base64.encodeBase64String(bytes);
            case HEX:
                return ByteUtilities.toHexString(bytes);
            case PLAIN:
                break;
            default:
                log.warn("ByteUtilities.toString(...) does not support {}, defaulting to {}", flavor, PrintingUtilities.Flavor.PLAIN);
        }
        return new String(bytes);
    }

    public static String toHexString(byte[] bytes) {
        return toHexString(bytes, true);
    }

    /**
     * Converts an array of bytes to its hexadecimal form.
     *
     * @param bytes     Bytes to be converted.
     * @param upperCase Whether the hex string should be UPPER or lower case.
     */
    public static String toHexString(byte[] bytes, boolean upperCase) {
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
        return new String(hexChars).toLowerCase(Locale.ENGLISH);
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
     * Gets the first index in {@code target} of matching bytes from {@code match}
     *
     * @param target Byte array to search for matches
     * @param match Byte match searched
     * @return First matching index from {@code target} array or {@code null} if no matches
     */
    public static Integer firstMatchingIndex(byte[] target, byte[] match) {
        return firstMatchingIndex(target, match, 0);
    }

    /**
     * Gets the first index in {@code target} of matching bytes from {@code match} where the index is equal or greater than {@code fromIndex}
     *
     * @param target Byte array to search for matches
     * @param match Byte match searched
     * @param fromIndex Offset index in {@code target} array (inclusive)
     * @return First matching index after {@code fromIndex} from {@code target} array or {@code null} if no matches
     */
    public static Integer firstMatchingIndex(byte[] target, byte[] match, int fromIndex) {
        Integer[] indices = indicesOfMatches(target, match);
        for(Integer idx : indices) {
            if (idx >= fromIndex) {
                return idx;
            }
        }

        return null;
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
        if (!byteArrayList.contains(builder) && builder.getLength() > 0) {
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
    public static String toHexString(int[] raw) {
        if (raw == null) { return null; }

        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for(final int i : raw) {
            hex.append(Constants.HEXES.charAt((i & 0xF0) >> 4)).append(Constants.HEXES.charAt((i & 0x0F)));
        }

        return hex.toString();
    }

    public static int parseBytes(byte[] bytes, int startIndex, int length, Endian endian) {
        int parsed = 0;

        byte[] lenBytes = new byte[length];
        System.arraycopy(bytes, startIndex, lenBytes, 0, length);

        if (endian == Endian.BIG) {
            for(int b = 0; b < length; b++) {
                parsed <<= 8;
                parsed += (int)lenBytes[b];
            }
        } else { //LITTLE endian
            for(int b = length - 1; b >= 0; b--) {
                parsed <<= 8;
                parsed += (int)lenBytes[b];
            }
        }

        return parsed;
    }

    public static int[] unwind(int bitwiseCode) {
        int bitPopulation = Integer.bitCount(bitwiseCode);
        int[] matches = new int[bitPopulation];
        int mask = 1;

        while(bitPopulation > 0) {
            if ((mask & bitwiseCode) > 0) {
                matches[--bitPopulation] = mask;
            }
            mask <<= 1;
        }
        return matches;
    }

    public static boolean numberEquals(Object val1, Object val2) {
        try {
            if(val1 == null || val2 == null) {
                return val1 == val2;
            } else if(val1.getClass() == val2.getClass()) {
                return val1.equals(val2);
            } else if(val1 instanceof Long) {
                return val1.equals(Long.parseLong(val2.toString()));
            } else if(val2 instanceof Long) {
                return val2.equals(Long.parseLong(val1.toString()));
            } else {
                return Double.parseDouble(val1.toString()) == Double.parseDouble(val2.toString());
            }
        } catch(NumberFormatException nfe) {
            log.warn("Cannot not compare [{} = '{}'].  Reason: {} {}", val1, val2, nfe.getClass().getName(), nfe.getMessage());
        }
        return false;
    }

    public static byte[] toByteArray(String string, Charset encoding) throws ArabicShapingException, IOException {
        if(encoding == null) {
            log.warn("String encoding was not provided for byte array conversion, default encoding will be used instead");
            return string.getBytes();
        }
        if(encoding.name().equals("IBM864")) {
            // We parse name elsewhere, so this will also match "cp864", "ibm864", "ibm-864", "864", "csIBM864"
            return ArabicConversionUtilities.convertToIBM864(string);
        }
        return string.getBytes(encoding);
    }

    public static byte[] toByteArray(PixelGrid pixelGrid) {
        log.info("Packing bits...");
        // Arrays elements are always initialized with default values, i.e. 0
        byte[] byteArray = new byte[pixelGrid.size() / 8];
        // Convert every eight zero's to a full byte, in decimal
        for(int i = 0; i < byteArray.length; i++) {
            for(int k = 0; k < 8; k++) {
                byteArray[i] |= (byte)((pixelGrid.get(8 * i + k)? 1:0) << 7 - k);
            }
        }
        return byteArray;
    }

    /**
     * Converts a series of bytes from one encoding to another using String conversion
     */
    public static byte[] seekConversion(byte[] bytes, Charset srcEncoding, Charset destEncoding) {
        if (srcEncoding == null) {
            return bytes;
        }
        if (srcEncoding.equals(destEncoding)) {
            log.warn("Provided source encoding and destination encoding are the same, skipping");
            return bytes;
        }
        return new String(bytes, srcEncoding).getBytes(destEncoding);
    }
}
