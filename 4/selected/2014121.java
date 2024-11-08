package gnu.java.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A filter input stream that decodes data encoded in the Base-64
 * encoding scheme.
 *
 * @author Casey Marshall (rsdio@metastatic.org)
 */
public class Base64InputStream extends FilterInputStream {

    /** Base-64 digits. */
    private static final String BASE_64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** Base-64 padding character. */
    private static final char BASE_64_PAD = '=';

    /** Decoding state. */
    private int state;

    /** Intermediate decoded value. */
    private int temp;

    /** EOF flag. */
    private boolean eof;

    private final byte[] one = new byte[1];

    /**
   * Create a new Base-64 input stream. The input bytes must be the
   * ASCII characters A-Z, a-z, 0-9, + and /, with optional whitespace,
   * and will be decoded into a byte stream.
   *
   * @param in The source of Base-64 input.
   */
    public Base64InputStream(InputStream in) {
        super(in);
        state = 0;
        temp = 0;
        eof = false;
    }

    /**
   * Decode a single Base-64 string to a byte array.
   *
   * @param base64 The Base-64 encoded data.
   * @return The decoded bytes.
   * @throws IOException If the given data do not compose a valid Base-64
   *  sequence.
   */
    public static byte[] decode(String base64) throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(base64.getBytes()));
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) (base64.length() / 0.666));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        return out.toByteArray();
    }

    public int available() {
        return 0;
    }

    public int read() throws IOException {
        if (read(one) == 1) return one[0];
        return -1;
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        if (eof) return -1;
        int count = 0;
        while (count < len) {
            int i;
            while (Character.isWhitespace((char) (i = in.read()))) ;
            int pos = BASE_64.indexOf((char) i);
            if (pos >= 0) {
                switch(state) {
                    case 0:
                        temp = pos << 2;
                        state = 1;
                        break;
                    case 1:
                        buf[count++] = (byte) (temp | (pos >>> 4));
                        temp = (pos & 0x0F) << 4;
                        state = 2;
                        break;
                    case 2:
                        buf[count++] = (byte) (temp | (pos >>> 2));
                        temp = (pos & 0x03) << 6;
                        state = 3;
                        break;
                    case 3:
                        buf[count++] = (byte) (temp | pos);
                        state = 0;
                        break;
                }
            } else if (i == BASE_64_PAD) {
                switch(state) {
                    case 0:
                    case 1:
                        throw new IOException("malformed Base-64 input");
                    case 2:
                        while (Character.isWhitespace((char) (i = in.read()))) ;
                        if (i != BASE_64_PAD) throw new IOException("malformed Base-64 input");
                    case 3:
                        while (Character.isWhitespace((char) (i = in.read()))) ;
                }
                eof = true;
                break;
            } else {
                if (state != 0) throw new IOException("malformed Base-64 input");
                eof = true;
                break;
            }
        }
        return count;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int markLimit) {
    }

    public void reset() throws IOException {
        throw new IOException("reset not supported");
    }

    public long skip(long n) throws IOException {
        long skipped;
        for (skipped = 0; skipped < n; skipped++) if (read() == -1) break;
        return skipped;
    }
}
