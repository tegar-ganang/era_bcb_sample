package org.josef.util;

import static org.josef.annotations.Status.Stage.DEVELOPMENT;
import static org.josef.annotations.Status.UnitTests.COMPLETE;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.josef.annotations.Review;
import org.josef.annotations.Reviews;
import org.josef.annotations.Status;

/**
 * Base64 encoding and decoding.
 * <br>Base64 encoding allows encoding binary data to printable ASCII and vice
 * versa. For a technical background on Base64 encoding check out the article at
 * WikiPedia by clicking <a href="http://en.wikipedia.org/wiki/Base64">here</a>.
 * Note: This class supports an alphabet where the characters at index 63 and 64
 * are respectively a '+' and a '/'. Line breaks at variable positions are
 * supported. Characters outside the 64 printable character range are treated as
 * illegal.
 * @author Kees Schotanus
 * @version 1.0 $Revision: 2840 $
 */
@Status(stage = DEVELOPMENT, unitTests = COMPLETE)
@Reviews({ @Review(by = "Kees Schotanus", at = "2010-09-23", reason = "Initial review"), @Review(by = "Kees Schotanus", at = "2010-10-01", reason = "Additional review since binary data and line breaks were handled incorrectly") })
public final class Base64 {

    /**
     * Maximum line length of a Base64 encoded line according to RFC 2045.
     */
    public static final int MAXIMUM_ENCODED_LINE_LENGHT = 76;

    /**
     * Constant line length to be used when Base64 encoded output should not
     * contain line breaks.
     * <br>The value is computed using the following expression:<br>
     * Integer.MAX_VALUE - Integer.MAX_VALUE % 4;
     */
    public static final int NO_LINE_BREAKS_LENGTH = 2147483644;

    /**
     * Elementary group size of unencoded characters.
     * A triplet of unencoded characters is encoded to a quartet of encoded
     * characters.
     */
    private static final int TRIPLET = 3;

    /**
     * Elementary group size of Base64 encoded characters.
     * A quartet of encoded characters is unencoded to a triplet of unencoded
     * characters.
     */
    private static final int QUARTET = 4;

    /**
     * Remainder of three when performing a modulo operation.
     */
    private static final int REMAINDER_OF_3 = 3;

    /**
     * Shift of 4 positions.
     */
    private static final int SHIFT_4 = 4;

    /**
     * Shift of 6 positions.
     */
    private static final int SHIFT_6 = 6;

    /**
     * Mask to filter all the bits except for the most right 8 bits.
     */
    private static final int BITS_1_TO_8_MASK = 0xFF;

    /**
     * Mask to filter all the bits except for the most right 6 bits.
     */
    private static final int BITS_1_TO_6_MASK = 0x3F;

    /**
     * Mask to filter all the bits except for bits 3 to 6 (1 based).
     */
    private static final int BITS_3_TO_6_MASK = 0x3C;

    /**
     * Mask to filter all the bits except for bits 5 to 6 (1 based).
     */
    private static final int BITS_5_TO_6_MASK = 0x30;

    /**
     * A byte that is not legal in Base64 encoded bytes.
     */
    private static final byte ILLEGAL_BYTE = -1;

    /**
     * A byte that represents a whitespace character.
     */
    private static final byte WHITESPACE_BYTE = -2;

    /**
     * Private constructor prevents creation of an instance outside this class.
     */
    private Base64() {
    }

    /**
     * The standard Base64 encode table.
     */
    private static final byte[] BASE64_ENCODE = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/' };

    /**
     * The standard Base64 decode table.
     * <br>Note: The table supports values up to and including 127.
     */
    private static final byte[] BASE64_DECODE = new byte[Byte.MAX_VALUE + 1];

    /**
     * Initialize the decode table from the Base64 encode table.
     */
    static {
        Arrays.fill(BASE64_DECODE, ILLEGAL_BYTE);
        for (int i = 0; i < BASE64_ENCODE.length; ++i) {
            BASE64_DECODE[BASE64_ENCODE[i]] = (byte) i;
        }
        BASE64_DECODE['\t'] = WHITESPACE_BYTE;
        BASE64_DECODE['\n'] = WHITESPACE_BYTE;
        BASE64_DECODE['\r'] = WHITESPACE_BYTE;
        BASE64_DECODE[' '] = WHITESPACE_BYTE;
    }

    /**
     * Encodes the information stored in the file identified by the supplied
     * fileName.
     * @param fileName The name of the file to decode the data from.
     * @param lineSize The length of the lines in the encoded output, normally
     *  either 64 or 76 (but must be divisible by 4).
     *  <br>After every group of lineSize bytes, a CR/LF pair is added to the
     *  file.
     * @return The decoded information from the file with the supplied fileName.
     * @throws IOException When the unencoded file could not be read.
     * @throws NullPointerException When the supplied fileName is null.
     * @throws IllegalArgumentException When the supplied fileName is empty or
     *  when the supplied lineSize is not divisible by 4 or when the lineSize
     *  &lt;= 0.
     * @see #encode(byte[], int)
     */
    public static byte[] encodeFromFile(final String fileName, final int lineSize) throws IOException {
        CDebug.checkParameterNotEmpty(fileName, "fileName");
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream unencoded = null;
        try {
            final File file = new File(fileName);
            unencoded = new ByteArrayOutputStream((int) file.length());
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            final byte[] input = new byte[10 * 10];
            int readBytes = bufferedInputStream.read(input);
            while (readBytes != -1) {
                unencoded.write(input, 0, readBytes);
                readBytes = bufferedInputStream.read(input);
            }
            return encode(unencoded.toByteArray(), lineSize);
        } finally {
            InputOutputUtil.close(bufferedInputStream, fileInputStream, unencoded);
        }
    }

    /**
     * Encodes the supplied input to a file with the supplied fileName.
     * @param input The data to encode.
     * @param fileName The name of the file to write the encoded data to.
     * @param lineSize The length of the lines in the encoded output, normally
     *  either 64 or 76 (but must be divisible by 4).
     *  <br>After every group of lineSize bytes, a CR/LF pair is added to the
     *  file.
     * @throws IOException When the encoded bytes could not be written.
     * @throws NullPointerException When either the supplied input array or the
     *  supplied fileName is null.
     * @throws IllegalArgumentException When the supplied fileName is empty or
     *  when the supplied lineSize is not divisible by 4 or when the lineSize
     *  &lt;= 0.
     * @see #encode(byte[], int)
     */
    public static void encodeToFile(final byte[] input, final String fileName, final int lineSize) throws IOException {
        CDebug.checkParameterNotNull(input, "input");
        CDebug.checkParameterNotEmpty(fileName, "fileName");
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            bufferedOutputStream.write(Base64.encode(input, lineSize));
        } finally {
            InputOutputUtil.close(bufferedOutputStream, fileOutputStream);
        }
    }

    /**
     * Encodes the supplied input to a Base64 encoding.
     * <br>Encoding comes down to converting 3 input bytes into 4 output bytes.
     * The 3 input bytes contain 4 groups of 6 bits. For every group of 6 bits
     * the accompanying Base64 characters is looked up and added to the output.
     * When the input does not consist of a multiple of 3 bytes, pad characters
     * are used in the output so the output always contains a multiple of 4
     * characters (neglecting whitespace characters).
     * @param input The input to encode.
     * @param lineSize The length of the lines in the encoded output, normally
     *  either 64 or 76.
     *  <br>After this length a CR/LF pair is written to the encoded output. The
     *  line size must be divisible by 4.
     * @return The Base64 encoded version of the supplied input.
     *  <br>The returned array may be empty but will never be null.
     * @throws NullPointerException When the supplied input array is null.
     * @throws IllegalArgumentException When the supplied lineSize is not
     *  divisible by 4 or when the lineSize &lt;= 0.
     */
    public static byte[] encode(final byte[] input, final int lineSize) {
        CDebug.checkParameterNotNull(input, "input");
        CDebug.checkParameterNotNull(lineSize, "lineSize");
        CDebug.checkParameterTrue(lineSize > 0 && lineSize % QUARTET == 0, "Line size must be > 0 and divisible by 4 but is:" + lineSize);
        final byte[] output = new byte[getEncodedOutputSize(input.length, lineSize)];
        int outputIndex = 0;
        int currentOutputLineLength = 0;
        for (int index = 0; index < input.length; index++) {
            switch(index % TRIPLET) {
                case 0:
                    output[outputIndex++] = BASE64_ENCODE[(input[index] & BITS_1_TO_8_MASK) >>> 2];
                    ++currentOutputLineLength;
                    break;
                case 1:
                    output[outputIndex++] = BASE64_ENCODE[((input[index - 1] << SHIFT_4) & BITS_5_TO_6_MASK) | ((input[index] & BITS_1_TO_8_MASK) >>> SHIFT_4)];
                    ++currentOutputLineLength;
                    break;
                case 2:
                    output[outputIndex++] = BASE64_ENCODE[((input[index - 1] << 2) & BITS_3_TO_6_MASK) | ((input[index] & BITS_1_TO_8_MASK) >>> SHIFT_6)];
                    output[outputIndex++] = BASE64_ENCODE[input[index] & BITS_1_TO_6_MASK];
                    currentOutputLineLength += 2;
                    break;
                default:
                    assert false;
            }
            if (currentOutputLineLength == lineSize && index + 1 != input.length) {
                output[outputIndex++] = (byte) '\r';
                output[outputIndex++] = (byte) '\n';
                currentOutputLineLength = 0;
            }
        }
        if (input.length % TRIPLET == 1) {
            output[outputIndex++] = BASE64_ENCODE[(input[input.length - 1] << SHIFT_4) & BITS_1_TO_6_MASK];
            output[outputIndex++] = '=';
            output[outputIndex++] = '=';
        }
        if (input.length % TRIPLET == 2) {
            output[outputIndex++] = BASE64_ENCODE[(input[input.length - 1] << 2) & BITS_1_TO_6_MASK];
            output[outputIndex++] = '=';
        }
        return output;
    }

    /**
     * Determines the size in bytes that would be required to encode the
     * supplied unencoded input.
     * <br>The rule is that after every lineSize bytes a line break must be
     * written, except for the last line.
     * @param inputLength The length of the unencoded input.
     * @param lineSize The maximum size of a single encoded line.
     * @return The size in bytes that would be required to encode the supplied
     *  unencoded input.
     */
    private static int getEncodedOutputSize(final int inputLength, final int lineSize) {
        if (inputLength == 0) {
            return 0;
        }
        final int outputSizeWithoutBreaks = (inputLength + 2 - ((inputLength + 2) % TRIPLET)) * QUARTET / TRIPLET;
        final int lineBreaks = (outputSizeWithoutBreaks - 1) / lineSize;
        return outputSizeWithoutBreaks + 2 * lineBreaks;
    }

    /**
     * Decodes the Base64 encoded data from the supplied fileName.
     * @param fileName The name of the file to decode the data from.
     * @return The decoded information from the file with the supplied fileName.
     * @throws IOException When the decoded bytes could not be read.
     * @throws NullPointerException When the supplied fileName is null.
     * @throws IllegalArgumentException When the supplied fileName is empty.
     * @see #decode(byte[])
     */
    public static byte[] decodeFromFile(final String fileName) throws IOException {
        CDebug.checkParameterNotEmpty(fileName, "fileName");
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayOutputStream encoded = null;
        try {
            final File file = new File(fileName);
            encoded = new ByteArrayOutputStream((int) file.length());
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            final byte[] input = new byte[10 * 10];
            int readBytes = bufferedInputStream.read(input);
            while (readBytes != -1) {
                encoded.write(input, 0, readBytes);
                readBytes = bufferedInputStream.read(input);
            }
            return decode(encoded.toByteArray());
        } finally {
            InputOutputUtil.close(bufferedInputStream, fileInputStream, encoded);
        }
    }

    /**
     * Decodes the supplied input to a file with the supplied fileName.
     * @param input The data to decode.
     * @param fileName The name of the file to write the decoded data to.
     * @throws IOException When the decoded bytes could not be written.
     * @throws NullPointerException When either the supplied input array or the
     *  supplied fileName is null.
     * @throws IllegalArgumentException When the supplied fileName is empty.
     * @see #decode(byte[])
     */
    public static void decodeToFile(final byte[] input, final String fileName) throws IOException {
        CDebug.checkParameterNotNull(input, "input");
        CDebug.checkParameterNotEmpty(fileName, "fileName");
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            bufferedOutputStream.write(Base64.decode(input));
        } finally {
            InputOutputUtil.close(bufferedOutputStream, fileOutputStream);
        }
    }

    /**
     * Decodes the supplied Base64 encoded input.
     * <br>Technically the following happens here:
     * <ol>
     *   <li>Every input byte is looked up in a decode table, giving 6 bits</li>
     *   <li>4 groups of 6 bits are converted to 3 bytes</li>
     *   <li>The 3 bytes are added to the output</li>
     * </ol>
     * @param input The input to decode.
     * @return The decoded version of the supplied Base64 encoded input.
     *  <br>The returned array may be empty but will never be null.
     * @throws NullPointerException When the supplied input array is null.
     * @throws IllegalArgumentException When the supplied input contains an
     *  invalid character.
     */
    public static byte[] decode(final byte[] input) {
        CDebug.checkParameterNotNull(input, "input");
        final ByteArrayOutputStream output = new ByteArrayOutputStream(input.length * TRIPLET / QUARTET);
        int nonWhiteSpaceInputCounter = 0;
        byte previousDecodedSixBits = 0;
        for (int i = 0; i < input.length; ++i) {
            checkForValidBase64Encoding(input, i);
            final byte decodedSixBits = BASE64_DECODE[input[i]];
            if (decodedSixBits != WHITESPACE_BYTE && input[i] != '=') {
                switch(nonWhiteSpaceInputCounter++ % QUARTET) {
                    case 0:
                        break;
                    case 1:
                        output.write((previousDecodedSixBits << 2) | (decodedSixBits >>> SHIFT_4));
                        break;
                    case 2:
                        output.write((previousDecodedSixBits << SHIFT_4) | (decodedSixBits >>> 2));
                        break;
                    case REMAINDER_OF_3:
                        output.write((previousDecodedSixBits << SHIFT_6) | decodedSixBits);
                        break;
                    default:
                        assert true;
                }
                previousDecodedSixBits = decodedSixBits;
            }
        }
        return output.toByteArray();
    }

    /**
     * Checks that the supplied input array at the supplied offset contains a
     * valid Base64 encoded byte.
     * A Base64 byte is properly encoded when it represents a:
     * <ol>
     *   <li>Base64 character</li>
     *   <li>White space character</li>
     *   <li>Pad character</li>
     * </ol>
     * Note: Pad characters may only exist at the end of the input (last or
     * second to last) and are not followed by non pad characters.
     * @param input The Base64 encoded byte array to check for validity at the
     *  supplied offset.
     * @param offset Offset into the supplied input array.
     * @throws IllegalArgumentException When the supplied input does not contain
     *  a valid Base64 encoded byte at the supplied offset.
     */
    private static void checkForValidBase64Encoding(final byte[] input, final int offset) {
        assert input != null : "Input array may not be null!";
        assert offset >= 0 && offset < input.length : "Offset is out of bounds!";
        if (input[offset] < 0) {
            throw new IllegalArgumentException(String.format("The Base64 encoded input contains an invalid byte:%d at position:%d", input[offset], offset));
        }
        if (input[offset] == '=') {
            if (offset < input.length - 2 || (offset == input.length - 2 && input[offset + 1] != '=')) {
                throw new IllegalArgumentException(String.format("The Base64 encoded input contains a pad character at invalid position:%d", offset));
            }
        } else {
            if (BASE64_DECODE[input[offset]] == ILLEGAL_BYTE) {
                throw new IllegalArgumentException(String.format("The Base64 encoded input contains an invalid byte:%d at position:%d", input[offset], offset));
            }
        }
    }
}
