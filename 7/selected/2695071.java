package net.sourceforge.xconf.toolbox;

import java.util.Arrays;

/**
 * Simple utility class to perform Base64 byte array encoding and decoding.
 *
 * @author  Tom Czarniecki
 */
public class Base64 {

    /** Chunk size per RFC 2045 section 6.8. */
    public static final int CHUNK_SIZE = 76;

    /** Chunk separator per RFC 2045 section 2.1. */
    public static final String CHUNK_SEPARATOR = "\r\n";

    private static final char PADCHAR = '=';

    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private final char[] encodeSet;

    private final int[] decodeSet;

    public Base64() {
        encodeSet = CHARSET.toCharArray();
        decodeSet = new int[255];
        Arrays.fill(decodeSet, -1);
        for (int i = 0; i < encodeSet.length; i++) {
            decodeSet[encodeSet[i]] = i;
        }
    }

    /**
     * Returns a base64 encoded representation of the given byte array.
     */
    public String encode(byte[] ba) {
        return new String(encodeToArray(ba));
    }

    /**
     * Returns a base64-chunked representation of the given byte array.
     */
    public String encodeChunked(byte[] ba) {
        char[] output = encodeToArray(ba);
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < output.length; i += CHUNK_SIZE) {
            if (i > 0) {
                buf.append(CHUNK_SEPARATOR);
            }
            int writeLength = Math.min(CHUNK_SIZE, output.length - i);
            buf.append(output, i, writeLength);
        }
        return buf.toString();
    }

    /**
     * Returns the byte array represented by the given base64 encoded string.
     *
     * @throws IllegalArgumentException
     *      If the length of the encoded string is not a multiple of 4
     *      the removal of all non-coding characters.
     */
    public byte[] decode(String encoded) {
        char[] cbuf = new char[encoded.length()];
        int[] counts = removeNonCodingCharacters(encoded, cbuf);
        int inputLength = counts[0];
        int paddingCount = counts[1];
        if ((inputLength % 4) != 0) {
            throw new IllegalArgumentException("Encoded length after removal of non-coding characters (" + inputLength + ") is not a multiple of 4.");
        }
        return decodeFromArray(cbuf, inputLength, paddingCount);
    }

    private char[] encodeToArray(byte[] ba) {
        boolean padDouble = false;
        boolean padSingle = false;
        int[] source = new int[3];
        int[] result = new int[4];
        int index = 0;
        char[] output = new char[((ba.length - 1) / 3 + 1) << 2];
        for (int i = 0; i < ba.length; i += 3) {
            source[0] = ba[i] & 0xff;
            if (i + 1 < ba.length) {
                source[1] = ba[i + 1] & 0xff;
            } else {
                padDouble = true;
                source[1] = 0;
            }
            if (i + 2 < ba.length) {
                source[2] = ba[i + 2] & 0xff;
            } else {
                padSingle = true;
                source[2] = 0;
            }
            split3to4(source, result);
            output[index++] = encodeSet[result[0]];
            output[index++] = encodeSet[result[1]];
            output[index++] = padDouble ? PADCHAR : encodeSet[result[2]];
            output[index++] = padSingle ? PADCHAR : encodeSet[result[3]];
        }
        return output;
    }

    private byte[] decodeFromArray(char[] cbuf, int inputLength, int paddingCount) {
        boolean padDouble = false;
        boolean padSingle = false;
        int[] source = new int[4];
        int[] result = new int[3];
        int index = 0;
        byte[] output = new byte[((inputLength * 6) >> 3) - paddingCount];
        for (int i = 0; i < inputLength; i += 4) {
            source[0] = decodeSet[cbuf[i]];
            source[1] = decodeSet[cbuf[i + 1]];
            if (cbuf[i + 2] == PADCHAR) {
                padDouble = true;
                source[2] = 0;
            } else {
                source[2] = decodeSet[cbuf[i + 2]];
            }
            if (cbuf[i + 3] == PADCHAR) {
                padSingle = true;
                source[3] = 0;
            } else {
                source[3] = decodeSet[cbuf[i + 3]];
            }
            join4to3(source, result);
            output[index++] = (byte) result[0];
            if (!padDouble) {
                output[index++] = (byte) result[1];
            }
            if (!padSingle) {
                output[index++] = (byte) result[2];
            }
        }
        return output;
    }

    /**
     * Split AAAAAABB BBBBCCCC CCDDDDDD
     * into 00AAAAAA 00BBBBBB 00CCCCCC 00DDDDDD
     */
    static void split3to4(int[] source, int[] result) {
        result[0] = source[0] >>> 2;
        result[1] = ((source[0] & 0x3) << 4) | (source[1] >>> 4);
        result[2] = ((source[1] & 0xf) << 2) | (source[2] >>> 6);
        result[3] = source[2] & 0x3f;
    }

    /**
     * Join 00AAAAAA 00BBBBBB 00CCCCCC 00DDDDDD
     * into AAAAAABB BBBBCCCC CCDDDDDD
     */
    static void join4to3(int[] source, int[] result) {
        result[0] = (source[0] << 2) | (source[1] >>> 4);
        result[1] = ((source[1] & 0xf) << 4) | (source[2] >>> 2);
        result[2] = ((source[2] & 0x3) << 6) | source[3];
    }

    /**
     * Strip out all non-Base64 characters from input string.
     * This includes all non-tail padding characters.
     */
    private int[] removeNonCodingCharacters(String input, char[] cbuf) {
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (isCodingCharacter(ch)) {
                cbuf[index++] = ch;
            }
        }
        int paddingCount = 0;
        for (int i = input.length() - 1; (i >= 0) && (paddingCount < 2); i--) {
            char ch = input.charAt(i);
            if (ch == PADCHAR) {
                cbuf[index++] = PADCHAR;
                paddingCount++;
            } else if (isCodingCharacter(ch)) {
                break;
            }
        }
        return new int[] { index, paddingCount };
    }

    private boolean isCodingCharacter(char ch) {
        try {
            return (decodeSet[ch] > -1);
        } catch (Exception e) {
            return false;
        }
    }
}
