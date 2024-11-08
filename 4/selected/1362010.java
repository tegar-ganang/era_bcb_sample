package ddss.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Collection of static methods for converting strings between different
 * formats and to and from byte arrays
 *
 *
 */
public class StringConverter {

    public static Date getDate(String strDate) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        Date date;
        try {
            date = df.parse(strDate);
        } catch (ParseException e) {
            return null;
        }
        return date;
    }

    public static Date getDateWS(String strDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        Date date;
        try {
            date = df.parse(strDate);
        } catch (ParseException e) {
            return null;
        }
        return date;
    }

    private static final char HEXCHAR[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String HEXINDEX = "0123456789abcdef0123456789ABCDEF";

    /**
     * Converts a String into a byte array by using a big-endian two byte
     * representation of each char value in the string.
     */
    byte[] stringToFullByteArray(String s) {
        int length = s.length();
        byte[] buffer = new byte[length * 2];
        int c;
        for (int i = 0; i < length; i++) {
            c = s.charAt(i);
            buffer[i * 2] = (byte) ((c & 0x0000ff00) >> 8);
            buffer[i * 2 + 1] = (byte) (c & 0x000000ff);
        }
        return buffer;
    }

    /**
     * Compacts a hexadecimal string into a byte array
     *
     *
     * param s hexadecimal string
     *
     * return byte array for the hex string
     * throws IOException
     */
    public static byte[] hexToByte(String s) throws IOException {
        int l = s.length() / 2;
        byte data[] = new byte[l];
        int j = 0;
        if (s.length() % 2 != 0) {
            throw new IOException("hexadecimal string with odd number of characters");
        }
        for (int i = 0; i < l; i++) {
            char c = s.charAt(j++);
            int n, b;
            n = HEXINDEX.indexOf(c);
            if (n == -1) {
                throw new IOException("hexadecimal string contains non hex character");
            }
            b = (n & 0xf) << 4;
            c = s.charAt(j++);
            n = HEXINDEX.indexOf(c);
            b += (n & 0xf);
            data[i] = (byte) b;
        }
        return data;
    }

    /**
     * Converts a byte array into a hexadecimal string
     *
     *
     * param b byte array
     *
     * return hex string
     */
    public static String byteToHex(byte b[]) {
        int len = b.length;
        char[] s = new char[len * 2];
        for (int i = 0, j = 0; i < len; i++) {
            int c = ((int) b[i]) & 0xff;
            s[j++] = HEXCHAR[c >> 4 & 0xf];
            s[j++] = HEXCHAR[c & 0xf];
        }
        return new String(s);
    }

    public static String byteToString(byte[] b, String charset) {
        try {
            return (charset == null) ? new String(b) : new String(b, charset);
        } catch (Exception e) {
        }
        return null;
    }

    public static int unicodeToAscii(OutputStream b, String s, boolean doubleSingleQuotes) throws IOException {
        int count = 0;
        if ((s == null) || (s.length() == 0)) {
            return 0;
        }
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                if ((i < len - 1) && (s.charAt(i + 1) == 'u')) {
                    b.write(c);
                    b.write('u');
                    b.write('0');
                    b.write('0');
                    b.write('5');
                    b.write('c');
                    count += 6;
                } else {
                    b.write(c);
                    count++;
                }
            } else if ((c >= 0x0020) && (c <= 0x007f)) {
                b.write(c);
                count++;
                if (c == '\'' && doubleSingleQuotes) {
                    b.write(c);
                    count++;
                }
            } else {
                b.write('\\');
                b.write('u');
                b.write(HEXCHAR[(c >> 12) & 0xf]);
                b.write(HEXCHAR[(c >> 8) & 0xf]);
                b.write(HEXCHAR[(c >> 4) & 0xf]);
                b.write(HEXCHAR[c & 0xf]);
                count += 6;
            }
        }
        return count;
    }

    public static String asciiToUnicode(byte[] s, int offset, int length) {
        if (length == 0) {
            return "";
        }
        char b[] = new char[length];
        int j = 0;
        for (int i = 0; i < length; i++) {
            byte c = s[offset + i];
            if (c == '\\' && i < length - 5) {
                byte c1 = s[offset + i + 1];
                if (c1 == 'u') {
                    i++;
                    int k = HEXINDEX.indexOf(s[offset + (++i)]) << 12;
                    k += HEXINDEX.indexOf(s[offset + (++i)]) << 8;
                    k += HEXINDEX.indexOf(s[offset + (++i)]) << 4;
                    k += HEXINDEX.indexOf(s[offset + (++i)]);
                    b[j++] = (char) k;
                } else {
                    b[j++] = (char) c;
                }
            } else {
                b[j++] = (char) c;
            }
        }
        return new String(b, 0, j);
    }

    public static String asciiToUnicode(String s) {
        if ((s == null) || (s.indexOf("\\u") == -1)) {
            return s;
        }
        int len = s.length();
        char b[] = new char[len];
        int j = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\\' && i < len - 5) {
                char c1 = s.charAt(i + 1);
                if (c1 == 'u') {
                    i++;
                    int k = HEXINDEX.indexOf(s.charAt(++i)) << 12;
                    k += HEXINDEX.indexOf(s.charAt(++i)) << 8;
                    k += HEXINDEX.indexOf(s.charAt(++i)) << 4;
                    k += HEXINDEX.indexOf(s.charAt(++i));
                    b[j++] = (char) k;
                } else {
                    b[j++] = c;
                }
            } else {
                b[j++] = c;
            }
        }
        return new String(b, 0, j);
    }

    public static int writeUTF(String str, OutputStream out) throws IOException {
        int strlen = str.length();
        int c, count = 0;
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                out.write(c);
                count++;
            } else if (c > 0x07FF) {
                out.write(0xE0 | ((c >> 12) & 0x0F));
                out.write(0x80 | ((c >> 6) & 0x3F));
                out.write(0x80 | ((c >> 0) & 0x3F));
                count += 3;
            } else {
                out.write(0xC0 | ((c >> 6) & 0x1F));
                out.write(0x80 | ((c >> 0) & 0x3F));
                count += 2;
            }
        }
        return count;
    }

    public static int getUTFSize(String s) {
        int len = (s == null) ? 0 : s.length();
        int l = 0;
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                l++;
            } else if (c > 0x07FF) {
                l += 3;
            } else {
                l += 2;
            }
        }
        return l;
    }

    /**
     * Using a Reader and a Writer, returns a String from an InputStream.
     */
    public static String inputStreamToString(InputStream x, int length) throws IOException {
        InputStreamReader in = new InputStreamReader(x);
        StringWriter writer = new StringWriter();
        int blocksize = 8 * 1024;
        char buffer[] = new char[blocksize];
        for (int left = length; left > 0; ) {
            int read = in.read(buffer, 0, left > blocksize ? blocksize : left);
            if (read == -1) {
                break;
            }
            writer.write(buffer, 0, read);
            left -= read;
        }
        writer.close();
        return writer.toString();
    }

    public static String toQuotedString(String s, char quoteChar, boolean extraQuote) {
        if (s == null) {
            return null;
        }
        int count = extraQuote ? count(s, quoteChar) : 0;
        int len = s.length();
        char[] b = new char[2 + count + len];
        int i = 0;
        int j = 0;
        b[j++] = quoteChar;
        for (; i < len; i++) {
            char c = s.charAt(i);
            b[j++] = c;
            if (extraQuote && c == quoteChar) {
                b[j++] = c;
            }
        }
        b[j] = quoteChar;
        return new String(b);
    }

    static int count(String s, char c) {
        int count = 0;
        if (s != null) {
            for (int i = s.length(); --i >= 0; ) {
                char chr = s.charAt(i);
                if (chr == c) {
                    count++;
                }
            }
        }
        return count;
    }
}
