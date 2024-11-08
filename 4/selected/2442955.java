package goldengate.common.digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Fast implementation of RSA's MD5 hash generator in Java JDK Beta-2 or higher.
 * <p>
 * Originally written by Santeri Paavolainen, Helsinki Finland 1996.<br>
 * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
 * Many changes Copyright (c) 2002 - 2005 Timothy W Macinta<br>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 * <p>
 * See http://www.twmacinta.com/myjava/fast_md5.php for more information on this
 * file and the related files.
 * <p>
 * This was originally a rather straight re-implementation of the reference
 * implementation given in RFC1321 by RSA. It passes the MD5 test suite as
 * defined in RFC1321.
 * <p>
 * Many optimizations made by Timothy W Macinta. Reduced time to checksum a test
 * file in Java alone to roughly half the time taken compared with
 * java.security.MessageDigest (within an intepretter). Also added an optional
 * native method to reduce the time even further. See
 * http://www.twmacinta.com/myjava/fast_md5.php for further information on the
 * time improvements achieved.
 * <p>
 * Some bug fixes also made by Timothy W Macinta.
 * <p>
 * Please note: I (Timothy Macinta) have put this code in the com.twmacinta.util
 * package only because it came without a package. I was not the the original
 * author of the code, although I did optimize it (substantially) and fix some
 * bugs.
 * <p>
 * This Java class has been derived from the RSA Data Security, Inc. MD5
 * Message-Digest Algorithm and its reference implementation.
 * <p>
 * This class will attempt to use a native method to quickly compute checksums
 * when the appropriate native library is available. On Linux, this library
 * should be named "MD5.so" and on Windows it should be named "MD5.dll". The
 * code will attempt to locate the library in the following locations in the
 * order given:
 *
 * <ol>
 * <li>The path specified by the system property
 * "com.twmacinta.util.MD5.NATIVE_LIB_FILE" (be sure to include "MD5.so" or
 * "MD5.dll" as appropriate at the end of the path).
 * <li>A platform specific directory beneath the "lib/arch/" directory. On Linux
 * for x86, this is "lib/arch/linux_x86/". On Windows for x86, this is
 * "lib/arch/win32_x86/".
 * <li>Within the "lib/" directory.
 * <li>Within the current directory.
 * </ol>
 *
 * <p>
 * If the library is not found, the code will fall back to the default (slower)
 * Java code.
 * <p>
 * As a side effect of having the code search for the native library,
 * SecurityExceptions might be thrown on JVMs that have a restrictive
 * SecurityManager. The initialization code attempts to silently discard these
 * exceptions and continue, but many SecurityManagers will attempt to notify the
 * user directly of all SecurityExceptions thrown. Consequently, the code has
 * provisions for skipping the search for the native library. Any of these
 * provisions may be used to skip the search as long as they are performed
 * <i>before</i> the first instance of a com.twmacinta.util.MD5 object is
 * constructed (note that the convenience stream objects will implicitly create
 * an MD5 object).
 * <p>
 * The first option is to set the system property
 * "com.twmacinta.util.MD5.NO_NATIVE_LIB" to "true" or "1". Unfortunately,
 * SecurityManagers may also choose to disallow system property setting, so this
 * won't be of use in all cases.
 * <p>
 * The second option is to call com.twmacinta.util.MD5.initNativeLibrary(true)
 * before any MD5 objects are constructed.
 *
 * @author Santeri Paavolainen <sjpaavol@cc.helsinki.fi>
 * @author Timothy W Macinta (twm@alum.mit.edu) (optimizations and bug fixes)
 * @author Frederic Bregier Bregier (add NIO support and dynamic library path
 *         loading)
 */
public class MD5 {

    /**
     * MD5 state
     */
    private MD5State state;

    /**
     * If Final() has been called, finals is set to the current finals state.
     * Any Update() causes this to be set to null.
     */
    private MD5State finals;

    /**
     * Padding for Final()
     */
    private static byte padding[] = { (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private static boolean native_lib_loaded = false;

    private static boolean native_lib_init_pending = true;

    private static String native_lib_path = "D:/somewhere/goldengate/lib/arch/win32_x86/MD5.dll";

    /**
     * Initialize MD5 internal state (object can be reused just by calling
     * Init() after every Final()
     */
    public synchronized void Init() {
        state = new MD5State();
        finals = null;
    }

    /**
     * Class constructor
     */
    public MD5() {
        if (native_lib_init_pending) {
            _initNativeLibrary();
        }
        Init();
    }

    /**
     * Initialize class, and update hash with ob.toString()
     *
     * @param ob
     *            Object, ob.toString() is used to update hash after
     *            initialization
     */
    public MD5(Object ob) {
        this();
        Update(ob.toString());
    }

    private void Decode(byte buffer[], int shift, int[] out) {
        out[0] = buffer[shift] & 0xff | (buffer[shift + 1] & 0xff) << 8 | (buffer[shift + 2] & 0xff) << 16 | buffer[shift + 3] << 24;
        out[1] = buffer[shift + 4] & 0xff | (buffer[shift + 5] & 0xff) << 8 | (buffer[shift + 6] & 0xff) << 16 | buffer[shift + 7] << 24;
        out[2] = buffer[shift + 8] & 0xff | (buffer[shift + 9] & 0xff) << 8 | (buffer[shift + 10] & 0xff) << 16 | buffer[shift + 11] << 24;
        out[3] = buffer[shift + 12] & 0xff | (buffer[shift + 13] & 0xff) << 8 | (buffer[shift + 14] & 0xff) << 16 | buffer[shift + 15] << 24;
        out[4] = buffer[shift + 16] & 0xff | (buffer[shift + 17] & 0xff) << 8 | (buffer[shift + 18] & 0xff) << 16 | buffer[shift + 19] << 24;
        out[5] = buffer[shift + 20] & 0xff | (buffer[shift + 21] & 0xff) << 8 | (buffer[shift + 22] & 0xff) << 16 | buffer[shift + 23] << 24;
        out[6] = buffer[shift + 24] & 0xff | (buffer[shift + 25] & 0xff) << 8 | (buffer[shift + 26] & 0xff) << 16 | buffer[shift + 27] << 24;
        out[7] = buffer[shift + 28] & 0xff | (buffer[shift + 29] & 0xff) << 8 | (buffer[shift + 30] & 0xff) << 16 | buffer[shift + 31] << 24;
        out[8] = buffer[shift + 32] & 0xff | (buffer[shift + 33] & 0xff) << 8 | (buffer[shift + 34] & 0xff) << 16 | buffer[shift + 35] << 24;
        out[9] = buffer[shift + 36] & 0xff | (buffer[shift + 37] & 0xff) << 8 | (buffer[shift + 38] & 0xff) << 16 | buffer[shift + 39] << 24;
        out[10] = buffer[shift + 40] & 0xff | (buffer[shift + 41] & 0xff) << 8 | (buffer[shift + 42] & 0xff) << 16 | buffer[shift + 43] << 24;
        out[11] = buffer[shift + 44] & 0xff | (buffer[shift + 45] & 0xff) << 8 | (buffer[shift + 46] & 0xff) << 16 | buffer[shift + 47] << 24;
        out[12] = buffer[shift + 48] & 0xff | (buffer[shift + 49] & 0xff) << 8 | (buffer[shift + 50] & 0xff) << 16 | buffer[shift + 51] << 24;
        out[13] = buffer[shift + 52] & 0xff | (buffer[shift + 53] & 0xff) << 8 | (buffer[shift + 54] & 0xff) << 16 | buffer[shift + 55] << 24;
        out[14] = buffer[shift + 56] & 0xff | (buffer[shift + 57] & 0xff) << 8 | (buffer[shift + 58] & 0xff) << 16 | buffer[shift + 59] << 24;
        out[15] = buffer[shift + 60] & 0xff | (buffer[shift + 61] & 0xff) << 8 | (buffer[shift + 62] & 0xff) << 16 | buffer[shift + 63] << 24;
    }

    private native void TransformJni(int[] stat, byte buffer[], int shift, int length);

    private void Transform(MD5State stat, byte buffer[], int shift, int[] decode_buf) {
        int a = stat.state[0], b = stat.state[1], c = stat.state[2], d = stat.state[3], x[] = decode_buf;
        Decode(buffer, shift, decode_buf);
        a += (b & c | ~b & d) + x[0] + 0xd76aa478;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + x[1] + 0xe8c7b756;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + x[2] + 0x242070db;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + x[3] + 0xc1bdceee;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + x[4] + 0xf57c0faf;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + x[5] + 0x4787c62a;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + x[6] + 0xa8304613;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + x[7] + 0xfd469501;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + x[8] + 0x698098d8;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + x[9] + 0x8b44f7af;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + x[10] + 0xffff5bb1;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + x[11] + 0x895cd7be;
        b = (b << 22 | b >>> 10) + c;
        a += (b & c | ~b & d) + x[12] + 0x6b901122;
        a = (a << 7 | a >>> 25) + b;
        d += (a & b | ~a & c) + x[13] + 0xfd987193;
        d = (d << 12 | d >>> 20) + a;
        c += (d & a | ~d & b) + x[14] + 0xa679438e;
        c = (c << 17 | c >>> 15) + d;
        b += (c & d | ~c & a) + x[15] + 0x49b40821;
        b = (b << 22 | b >>> 10) + c;
        a += (b & d | c & ~d) + x[1] + 0xf61e2562;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + x[6] + 0xc040b340;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + x[11] + 0x265e5a51;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + x[0] + 0xe9b6c7aa;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + x[5] + 0xd62f105d;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + x[10] + 0x02441453;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + x[15] + 0xd8a1e681;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + x[4] + 0xe7d3fbc8;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + x[9] + 0x21e1cde6;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + x[14] + 0xc33707d6;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + x[3] + 0xf4d50d87;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + x[8] + 0x455a14ed;
        b = (b << 20 | b >>> 12) + c;
        a += (b & d | c & ~d) + x[13] + 0xa9e3e905;
        a = (a << 5 | a >>> 27) + b;
        d += (a & c | b & ~c) + x[2] + 0xfcefa3f8;
        d = (d << 9 | d >>> 23) + a;
        c += (d & b | a & ~b) + x[7] + 0x676f02d9;
        c = (c << 14 | c >>> 18) + d;
        b += (c & a | d & ~a) + x[12] + 0x8d2a4c8a;
        b = (b << 20 | b >>> 12) + c;
        a += (b ^ c ^ d) + x[5] + 0xfffa3942;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + x[8] + 0x8771f681;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + x[11] + 0x6d9d6122;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + x[14] + 0xfde5380c;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + x[1] + 0xa4beea44;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + x[4] + 0x4bdecfa9;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + x[7] + 0xf6bb4b60;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + x[10] + 0xbebfbc70;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + x[13] + 0x289b7ec6;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + x[0] + 0xeaa127fa;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + x[3] + 0xd4ef3085;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + x[6] + 0x04881d05;
        b = (b << 23 | b >>> 9) + c;
        a += (b ^ c ^ d) + x[9] + 0xd9d4d039;
        a = (a << 4 | a >>> 28) + b;
        d += (a ^ b ^ c) + x[12] + 0xe6db99e5;
        d = (d << 11 | d >>> 21) + a;
        c += (d ^ a ^ b) + x[15] + 0x1fa27cf8;
        c = (c << 16 | c >>> 16) + d;
        b += (c ^ d ^ a) + x[2] + 0xc4ac5665;
        b = (b << 23 | b >>> 9) + c;
        a += (c ^ (b | ~d)) + x[0] + 0xf4292244;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + x[7] + 0x432aff97;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + x[14] + 0xab9423a7;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + x[5] + 0xfc93a039;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + x[12] + 0x655b59c3;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + x[3] + 0x8f0ccc92;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + x[10] + 0xffeff47d;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + x[1] + 0x85845dd1;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + x[8] + 0x6fa87e4f;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + x[15] + 0xfe2ce6e0;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + x[6] + 0xa3014314;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + x[13] + 0x4e0811a1;
        b = (b << 21 | b >>> 11) + c;
        a += (c ^ (b | ~d)) + x[4] + 0xf7537e82;
        a = (a << 6 | a >>> 26) + b;
        d += (b ^ (a | ~c)) + x[11] + 0xbd3af235;
        d = (d << 10 | d >>> 22) + a;
        c += (a ^ (d | ~b)) + x[2] + 0x2ad7d2bb;
        c = (c << 15 | c >>> 17) + d;
        b += (d ^ (c | ~a)) + x[9] + 0xeb86d391;
        b = (b << 21 | b >>> 11) + c;
        stat.state[0] += a;
        stat.state[1] += b;
        stat.state[2] += c;
        stat.state[3] += d;
    }

    /**
     * Updates hash with the bytebuffer given (using at maximum length bytes
     * from that buffer)
     *
     * @param stat
     *            Which state is updated
     * @param buffer
     *            Array of bytes to be hashed
     * @param offset
     *            Offset to buffer array
     * @param length
     *            Use at maximum `length' bytes (absolute maximum is
     *            buffer.length)
     */
    public void Update(MD5State stat, byte buffer[], int offset, int length) {
        int index, partlen, i, start;
        finals = null;
        int newlength = length;
        if (newlength - offset > buffer.length) {
            newlength = buffer.length - offset;
        }
        index = (int) (stat.count & 0x3f);
        stat.count += newlength;
        partlen = 64 - index;
        if (newlength >= partlen) {
            if (native_lib_loaded) {
                if (partlen == 64) {
                    partlen = 0;
                } else {
                    for (i = 0; i < partlen; i++) {
                        stat.buffer[i + index] = buffer[i + offset];
                    }
                    TransformJni(stat.state, stat.buffer, 0, 64);
                }
                i = partlen + ((newlength - partlen) / 64) * 64;
                int transformLength = newlength - partlen;
                int transformOffset = partlen + offset;
                final int MAX_LENGTH = 65536;
                while (true) {
                    if (transformLength > MAX_LENGTH) {
                        TransformJni(stat.state, buffer, transformOffset, MAX_LENGTH);
                        transformLength -= MAX_LENGTH;
                        transformOffset += MAX_LENGTH;
                    } else {
                        TransformJni(stat.state, buffer, transformOffset, transformLength);
                        break;
                    }
                }
            } else {
                int[] decode_buf = new int[16];
                if (partlen == 64) {
                    partlen = 0;
                } else {
                    for (i = 0; i < partlen; i++) {
                        stat.buffer[i + index] = buffer[i + offset];
                    }
                    Transform(stat, stat.buffer, 0, decode_buf);
                }
                for (i = partlen; i + 63 < newlength; i += 64) {
                    Transform(stat, buffer, i + offset, decode_buf);
                }
            }
            index = 0;
        } else {
            i = 0;
        }
        if (i < newlength) {
            start = i;
            for (; i < newlength; i++) {
                stat.buffer[index + i - start] = buffer[i + offset];
            }
        }
    }

    /**
     * Plain update, updates this object
     *
     * @param buffer
     * @param offset
     * @param length
     */
    public void Update(byte buffer[], int offset, int length) {
        Update(state, buffer, offset, length);
    }

    /**
     * Plain update, updates this object
     *
     * @param buffer
     * @param length
     */
    public void Update(byte buffer[], int length) {
        Update(state, buffer, 0, length);
    }

    /**
     * Updates hash with given array of bytes
     *
     * @param buffer
     *            Array of bytes to use for updating the hash
     */
    public void Update(byte buffer[]) {
        Update(buffer, 0, buffer.length);
    }

    /**
     * Updates hash with a single byte
     *
     * @param b
     *            Single byte to update the hash
     */
    public void Update(byte b) {
        byte buffer[] = new byte[1];
        buffer[0] = b;
        Update(buffer, 1);
    }

    /**
     * Update buffer with given string. Note that because the version of the
     * s.getBytes() method without parameters is used to convert the string to a
     * byte array, the results of this method may be different on different
     * platforms. The s.getBytes() method converts the string into a byte array
     * using the current platform's default character set and may therefore have
     * different results on platforms with different default character sets. If
     * a version that works consistently across platforms with different default
     * character sets is desired, use the overloaded version of the Update()
     * method which takes a string and a character encoding.
     *
     * @param s
     *            String to be update to hash (is used as s.getBytes())
     */
    public void Update(String s) {
        byte chars[] = s.getBytes();
        Update(chars, chars.length);
    }

    /**
     * Update buffer with given string using the given encoding. If the given
     * encoding is null, the encoding "ISO8859_1" is used.
     *
     * @param s
     *            String to be update to hash (is used as
     *            s.getBytes(charset_name))
     * @param charset_name
     *            The character set to use to convert s to a byte array, or null
     *            if the "ISO8859_1" character set is desired.
     * @exception java.io.UnsupportedEncodingException
     *                If the named charset is not supported.
     */
    public void Update(String s, String charset_name) throws java.io.UnsupportedEncodingException {
        String newcharset = charset_name;
        if (newcharset == null) {
            newcharset = "ISO8859_1";
        }
        byte chars[] = s.getBytes(newcharset);
        Update(chars, chars.length);
    }

    /**
     * Update buffer with a single integer (only & 0xff part is used, as a byte)
     *
     * @param i
     *            Integer value, which is then converted to byte as i & 0xff
     */
    public void Update(int i) {
        Update((byte) (i & 0xff));
    }

    /**
     * Updates hash with given {@link ChannelBuffer} (from Netty)
     *
     * @param buffer
     *            ChannelBuffer to use for updating the hash
     */
    public void Update(ChannelBuffer buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        Update(state, bytes, 0, bytes.length);
    }

    private byte[] Encode(int input[], int len) {
        int i, j;
        byte out[];
        out = new byte[len];
        for (i = j = 0; j < len; i++, j += 4) {
            out[j] = (byte) (input[i] & 0xff);
            out[j + 1] = (byte) (input[i] >>> 8 & 0xff);
            out[j + 2] = (byte) (input[i] >>> 16 & 0xff);
            out[j + 3] = (byte) (input[i] >>> 24 & 0xff);
        }
        return out;
    }

    /**
     * Returns array of bytes (16 bytes) representing hash as of the current
     * state of this object. Note: getting a hash does not invalidate the hash
     * object, it only creates a copy of the real state which is finalized.
     *
     * @return Array of 16 bytes, the hash of all updated bytes
     */
    public synchronized byte[] Final() {
        byte bits[];
        int index, padlen;
        MD5State fin;
        if (finals == null) {
            fin = new MD5State(state);
            int[] count_ints = { (int) (fin.count << 3), (int) (fin.count >> 29) };
            bits = Encode(count_ints, 8);
            index = (int) (fin.count & 0x3f);
            padlen = index < 56 ? 56 - index : 120 - index;
            Update(fin, padding, 0, padlen);
            Update(fin, bits, 0, 8);
            finals = fin;
        }
        return Encode(finals.state, 16);
    }

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Turns array of bytes into string representing each byte as unsigned hex
     * number.
     *
     * @param hash
     *            Array of bytes to convert to hex-string
     * @return Generated hex string
     */
    public static final String asHex(byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[hash[i] >>> 4 & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

    /**
     * Turns String into array of bytes representing each couple of unsigned hex
     * number as one byte.
     *
     * @param buf
     *            hex string
     * @return Array of bytes converted from hex-string
     */
    public static final byte[] asByte(String buf) {
        byte from[] = buf.getBytes();
        byte hash[] = new byte[from.length / 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            byte code1 = from[x++];
            byte code2 = from[x++];
            if (code1 >= HEX_CHARS[10]) {
                code1 -= HEX_CHARS[10] - 10;
            } else {
                code1 -= HEX_CHARS[0];
            }
            if (code2 >= HEX_CHARS[10]) {
                code2 -= HEX_CHARS[10] - 10;
            } else {
                code2 -= HEX_CHARS[0];
            }
            hash[i] = (byte) ((code1 << 4) + code2);
        }
        return hash;
    }

    /**
     * Returns 32-character hex representation of this objects hash
     *
     * @return String of this object's hash
     */
    public String asHex() {
        return asHex(Final());
    }

    /**
     * Disable lib loading (True) or enable it (false)
     *
     * @param disallow_lib_loading
     * @return True if the native Library is loaded, else False
     */
    public static final synchronized boolean initNativeLibrary(boolean disallow_lib_loading) {
        if (disallow_lib_loading) {
            native_lib_init_pending = false;
            return false;
        }
        return _initNativeLibrary();
    }

    /**
     * Initialize the Native Library from path
     *
     * @param libpath
     * @return True if the native Library is loaded, else False
     */
    public static final synchronized boolean initNativeLibrary(String libpath) {
        native_lib_path = libpath;
        native_lib_init_pending = true;
        return _initNativeLibrary();
    }

    /**
     * Internal Function to initialize if needed the Native Library support
     *
     * @return True if the native Library is loaded, else False
     */
    private static final synchronized boolean _initNativeLibrary() {
        if (!native_lib_init_pending) {
            return native_lib_loaded;
        }
        native_lib_loaded = _loadNativeLibrary();
        native_lib_init_pending = false;
        return native_lib_loaded;
    }

    /**
     * Try to load the Native Library
     *
     * @return True if the native Library is loaded, else False
     */
    private static final synchronized boolean _loadNativeLibrary() {
        try {
            String prop = System.getProperty("com.twmacinta.util.MD5.NO_NATIVE_LIB");
            if (prop != null) {
                prop = prop.trim();
                if (prop.equalsIgnoreCase("true") || prop.equals("1")) {
                    return false;
                }
            }
            File fstring;
            prop = native_lib_path;
            if (prop != null) {
                fstring = new File(prop);
                if (fstring.canRead()) {
                    System.load(fstring.getAbsolutePath());
                    return true;
                }
            }
            File f;
            prop = System.getProperty("com.twmacinta.util.MD5.NATIVE_LIB_FILE");
            if (prop != null) {
                f = new File(prop);
                if (f.canRead()) {
                    System.load(f.getAbsolutePath());
                    return true;
                }
            }
            String os_name = System.getProperty("os.name");
            String os_arch = System.getProperty("os.arch");
            if (os_name == null || os_arch == null) {
                return false;
            }
            os_name = os_name.toLowerCase();
            os_arch = os_arch.toLowerCase();
            File arch_lib_path = null;
            String arch_libfile_suffix = null;
            if (os_name.equals("linux") && (os_arch.equals("x86") || os_arch.equals("i386") || os_arch.equals("i486") || os_arch.equals("i586") || os_arch.equals("i686"))) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "linux_x86");
                arch_libfile_suffix = ".so";
            } else if (os_name.equals("linux") && os_arch.equals("amd64")) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "linux_amd64");
                arch_libfile_suffix = ".so";
            } else if (os_name.startsWith("windows ") && (os_arch.equals("x86") || os_arch.equals("i386") || os_arch.equals("i486") || os_arch.equals("i586") || os_arch.equals("i686"))) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "win32_x86");
                arch_libfile_suffix = ".dll";
            } else if (os_name.startsWith("windows ") && os_arch.equals("amd64")) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "win_amd64");
                arch_libfile_suffix = ".dll";
            } else if (os_name.startsWith("mac os x") && os_arch.equals("ppc")) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "darwin_ppc");
                arch_libfile_suffix = ".jnilib";
            } else if (os_name.startsWith("mac os x") && (os_arch.equals("x86") || os_arch.equals("i386") || os_arch.equals("i486") || os_arch.equals("i586") || os_arch.equals("i686"))) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "darwin_x86");
                arch_libfile_suffix = ".jnilib";
            } else if (os_name.startsWith("mac os x") && os_arch.equals("x86_64")) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "darwin_x86_64");
                arch_libfile_suffix = ".jnilib";
            } else if (os_name.equals("freebsd") && (os_arch.equals("x86") || os_arch.equals("i386") || os_arch.equals("i486") || os_arch.equals("i586") || os_arch.equals("i686"))) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "freebsd_x86");
                arch_libfile_suffix = ".so";
            } else if (os_name.equals("freebsd") && os_arch.equals("amd64")) {
                arch_lib_path = new File(new File(new File("lib"), "arch"), "freebsd_amd64");
                arch_libfile_suffix = ".so";
            } else {
                arch_libfile_suffix = ".so";
            }
            String fname = "MD5" + arch_libfile_suffix;
            if (arch_lib_path != null) {
                f = new File(arch_lib_path, fname);
                if (f.canRead()) {
                    System.load(f.getAbsolutePath());
                    return true;
                }
            }
            f = new File(new File("lib"), fname);
            if (f.canRead()) {
                System.load(f.getAbsolutePath());
                return true;
            }
            f = new File(fname);
            if (f.canRead()) {
                System.load(f.getAbsolutePath());
                return true;
            }
        } catch (SecurityException e) {
            System.err.println("Can't do native library: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Can't do native library: " + e.getMessage());
        }
        return false;
    }

    /**
     * Calculates and returns the hash of the contents of the given file.
     *
     * @param f
     *            FileInterface to hash
     * @return the hash from the given file
     * @throws IOException
     **/
    public static byte[] getHash(File f) throws IOException {
        InputStream close_me = null;
        try {
            long buf_size = f.length();
            if (buf_size < 512) {
                buf_size = 512;
            }
            if (buf_size > 65536) {
                buf_size = 65536;
            }
            byte[] buf = new byte[(int) buf_size];
            FileInputStream in = new FileInputStream(f);
            close_me = in;
            MD5 md5 = new MD5();
            int read = 0;
            while ((read = in.read(buf)) >= 0) {
                md5.Update(md5.state, buf, 0, read);
            }
            in.close();
            in = null;
            close_me = null;
            buf = null;
            buf = md5.Final();
            return buf;
        } catch (IOException e) {
            if (close_me != null) {
                try {
                    close_me.close();
                } catch (Exception e2) {
                }
            }
            throw e;
        }
    }

    /**
     * Calculates and returns the hash of the contents of the given file using
     * Nio file access.
     *
     * @param f
     *            for the FileInterface
     * @return the hash from the FileInterface with NIO access
     * @throws IOException
     **/
    public static byte[] getHashNio(File f) throws IOException {
        if (!f.exists()) {
            throw new FileNotFoundException(f.toString());
        }
        InputStream close_me = null;
        try {
            long buf_size = f.length();
            if (buf_size < 512) {
                buf_size = 512;
            }
            if (buf_size > 65536) {
                buf_size = 65536;
            }
            byte[] buf = new byte[(int) buf_size];
            FileInputStream in = new FileInputStream(f);
            close_me = in;
            FileChannel fileChannel = in.getChannel();
            ByteBuffer bb = ByteBuffer.wrap(buf);
            int read = 0;
            MD5 md5 = new MD5();
            read = fileChannel.read(bb);
            while (read > 0) {
                md5.Update(md5.state, buf, 0, read);
                bb.clear();
                read = fileChannel.read(bb);
            }
            fileChannel.close();
            fileChannel = null;
            in = null;
            close_me = null;
            bb = null;
            buf = null;
            buf = md5.Final();
            md5 = null;
            return buf;
        } catch (IOException e) {
            if (close_me != null) {
                try {
                    close_me.close();
                } catch (Exception e2) {
                }
            }
            throw e;
        }
    }

    /**
     * Calculates and returns the hash of the contents of the given stream.
     *
     * @param stream
     *            Stream to hash
     * @return the hash from the given stream
     * @throws IOException
     **/
    public static byte[] getHash(InputStream stream) throws IOException {
        try {
            long buf_size = 65536;
            byte[] buf = new byte[(int) buf_size];
            MD5 md5 = new MD5();
            int read = 0;
            while ((read = stream.read(buf)) >= 0) {
                md5.Update(md5.state, buf, 0, read);
            }
            stream.close();
            buf = null;
            buf = md5.Final();
            return buf;
        } catch (IOException e) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e2) {
                }
            }
            throw e;
        }
    }

    /**
     * Test if both hashes are equal
     *
     * @param hash1
     * @param hash2
     * @return true iff the first 16 bytes of both hash1 and hash2 are equal;
     *         both hash1 and hash2 are null; or either hash array is less than
     *         16 bytes in length and their lengths and all of their bytes are
     *         equal.
     **/
    public static boolean hashesEqual(byte[] hash1, byte[] hash2) {
        return Arrays.equals(hash1, hash2);
    }

    private static byte[] salt = { 'G', 'o', 'l', 'd', 'e', 'n', 'G', 'a', 't', 'e' };

    /**
     * Crypt a password
     * @param pwd to crypt
     * @return the crypted password
     */
    public static final String passwdCrypt(String pwd) {
        MD5 md5 = new MD5();
        byte[] bpwd = pwd.getBytes();
        for (int i = 0; i < 16; i++) {
            md5.Update(md5.state, bpwd, 0, bpwd.length);
            md5.Update(md5.state, salt, 0, salt.length);
        }
        return md5.asHex();
    }

    /**
     * Crypt a password
     * @param bpwd to crypt
     * @return the crypted password
     */
    public static final byte[] passwdCrypt(byte[] bpwd) {
        MD5 md5 = new MD5();
        for (int i = 0; i < 16; i++) {
            md5.Update(md5.state, bpwd, 0, bpwd.length);
            md5.Update(md5.state, salt, 0, salt.length);
        }
        return md5.Final();
    }

    /**
     * 
     * @param pwd
     * @param cryptPwd
     * @return True if the pwd is comparable with the cryptPwd
     */
    public static final boolean equalPasswd(String pwd, String cryptPwd) {
        String asHex = passwdCrypt(pwd);
        return cryptPwd.equals(asHex);
    }

    /**
     * 
     * @param pwd
     * @param cryptPwd
     * @return True if the pwd is comparable with the cryptPwd
     */
    public static final boolean equalPasswd(byte[] pwd, byte[] cryptPwd) {
        byte[] bytes = passwdCrypt(pwd);
        return Arrays.equals(cryptPwd, bytes);
    }

    /**
     * Test function
     *
     * @param argv
     *            with 2 arguments as filename to hash and full path to the
     *            Native Library
     */
    public static void main(String argv[]) {
        long start = System.currentTimeMillis();
        if (argv.length < 1) {
            boolean nativeLib = false;
            nativeLib = initNativeLibrary(true);
            if (!nativeLib) {
                System.err.println("Native library cannot be load from " + "D:\\NEWJARS\\gglib\\win32\\MD5.dll");
            } else {
                System.out.println("Native library loaded from " + "D:\\NEWJARS\\gglib\\win32\\MD5.dll");
            }
            for (int i = 0; i < 1000000; i++) {
                passwdCrypt("Ceci est mon password!");
            }
            System.err.println("Final passwd crypted in " + (System.currentTimeMillis() - start) + "ms is: " + passwdCrypt("Ceci est mon password!"));
            System.err.println("Not enough argument: <full path to the filename to hash> [<full path to the native library>]");
            return;
        }
        boolean nativeLib = false;
        if (argv.length == 2) {
            nativeLib = initNativeLibrary(argv[1]);
            if (!nativeLib) {
                System.err.println("Native library cannot be load from " + argv[1]);
            } else {
                System.out.println("Native library loaded from " + argv[1]);
            }
        }
        File file = new File(argv[0]);
        byte[] bmd5;
        try {
            bmd5 = getHashNio(file);
        } catch (IOException e1) {
            bmd5 = null;
        }
        if (bmd5 != null) {
            if (nativeLib) {
                System.out.println("FileInterface MD5 is " + asHex(bmd5) + " using Native Library in " + (System.currentTimeMillis() - start) + " ms");
            } else {
                System.out.println("FileInterface MD5 is " + asHex(bmd5) + " using Java version in " + (System.currentTimeMillis() - start) + " ms");
            }
        } else {
            System.err.println("Cannot compute md5 for " + argv[1]);
        }
    }
}
