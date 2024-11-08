package goldengate.common.digest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Class implementing digest like MD5, SHA1. MD5 is based on the Fast MD5
 * implementation, without C library support, but can be revert to JVM native
 * digest.<br><br>
 * 
 * Some performance reports: (done using java -server option)
 * <ul>
 * <li>File based only:</li>
 * <ul><li>FastMD5 in C is almost the fastest (+20%), while FastMD5 in Java is the slowest (-20%) and JVM version is in the middle.</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=7, SHA256=11, SHA512=25</li>
 * </ul>
 * <li>Buffer based only:</li>
 * <ul><li>JVM version is the fastest (+20%), while FastMD5 in C or in Java are the same (-20% than JVM).</li>
 * <li>If ADLER32 is the referenced time ADLER32=1, CRC32=2.5, MD5=4, SHA1=8, SHA256=13, SHA384=29, SHA512=31</li>
 * </ul></ul>
 * 
 * @author Frederic Bregier
 *
 */
public class FilesystemBasedDigest {

    protected MD5 md5 = null;

    protected Checksum checksum = null;

    protected MessageDigest digest = null;

    protected DigestAlgo algo = null;

    /**
     * Constructor of an independent Digest
     * @param algo
     * @throws NoSuchAlgorithmException
     */
    public FilesystemBasedDigest(DigestAlgo algo) throws NoSuchAlgorithmException {
        initialize(algo);
    }

    /**
     * (Re)Initialize the digest
     * @throws NoSuchAlgorithmException
     */
    public void initialize() throws NoSuchAlgorithmException {
        if (algo == DigestAlgo.MD5 && useFastMd5) {
            md5 = new MD5();
            return;
        }
        switch(algo) {
            case ADLER32:
                checksum = new Adler32();
                return;
            case CRC32:
                checksum = new CRC32();
                return;
            case MD5:
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
                String algoname = algo.name;
                try {
                    digest = MessageDigest.getInstance(algoname);
                } catch (NoSuchAlgorithmException e) {
                    throw new NoSuchAlgorithmException(algo + " Algorithm not supported by this JVM", e);
                }
                return;
            default:
                throw new NoSuchAlgorithmException(algo.name + " Algorithm not supported by this JVM");
        }
    }

    /**
     * (Re)Initialize the digest
     * @param algo
     * @throws NoSuchAlgorithmException
     */
    public void initialize(DigestAlgo algo) throws NoSuchAlgorithmException {
        this.algo = algo;
        initialize();
    }

    /**
     * Update the digest with new bytes
     * @param bytes
     * @param offset
     * @param length
     */
    public void Update(byte[] bytes, int offset, int length) {
        if (md5 != null) {
            md5.Update(bytes, offset, length);
            return;
        }
        switch(algo) {
            case ADLER32:
            case CRC32:
                checksum.update(bytes, offset, length);
                return;
            case MD5:
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
                digest.update(bytes, offset, length);
                return;
        }
    }

    /**
     * 
     * @return the digest in array of bytes
     */
    public byte[] Final() {
        if (md5 != null) {
            return md5.Final();
        }
        switch(algo) {
            case ADLER32:
            case CRC32:
                return Long.toOctalString(checksum.getValue()).getBytes();
            case MD5:
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
                return digest.digest();
        }
        return null;
    }

    /**
     * Initialize the MD5 support
     * @param mustUseFastMd5 True will use FastMD5 support, False will use JVM native MD5
     * @param path If useFastMD5 is True, if path is not null, specify the C Library (optional), 
     *          else if null will use the Java implementation
     * @return True if the native library is loaded
     */
    public static boolean initializeMd5(boolean mustUseFastMd5, String path) {
        useFastMd5 = mustUseFastMd5;
        fastMd5Path = path;
        if (fastMd5Path == null) {
            return MD5.initNativeLibrary(true);
        } else {
            if (useFastMd5) {
                return MD5.initNativeLibrary(fastMd5Path);
            } else {
                return MD5.initNativeLibrary(true);
            }
        }
    }

    /**
     * All Algo that Digest Class could handle
     * @author Frederic Bregier
     *
     */
    public static enum DigestAlgo {

        CRC32("CRC32", 11), ADLER32("ADLER32", 9), MD5("MD5", 16), MD2("MD2", 16), SHA1("SHA-1", 20), SHA256("SHA-256", 32), SHA384("SHA-384", 48), SHA512("SHA-512", 64);

        public String name;

        public int byteSize;

        /**
         * 
         * @return the length in bytes of one Digest
         */
        public int getByteSize() {
            return byteSize;
        }

        /**
         * 
         * @return the length in Hex form of one Digest
         */
        public int getHexSize() {
            return byteSize * 2;
        }

        private DigestAlgo(String name, int byteSize) {
            this.name = name;
            this.byteSize = byteSize;
        }
    }

    /**
     * Should a file MD5 be computed using FastMD5
     */
    public static boolean useFastMd5 = false;

    /**
     * If using Fast MD5, should we used the binary JNI library, empty meaning
     * no. FastMD5 is up to 50% fastest than JVM, but JNI library might be not better
     * than java FastMD5.
     */
    public static String fastMd5Path = null;

    /**
     *
     * @param dig1
     * @param dig2
     * @return True if the two digest are equals
     */
    public static final boolean digestEquals(byte[] dig1, byte[] dig2) {
        return MessageDigest.isEqual(dig1, dig2);
    }

    /**
     *
     * @param dig1
     * @param dig2
     * @return True if the two digest are equals
     */
    public static final boolean digestEquals(String dig1, byte[] dig2) {
        byte[] bdig1 = getFromHex(dig1);
        return MessageDigest.isEqual(bdig1, dig2);
    }

    /**
     * get the byte array of the MD5 for the given FileInterface using Nio
     * access
     *
     * @param f
     * @return the byte array representing the MD5
     * @throws IOException
     */
    public static byte[] getHashMd5Nio(File f) throws IOException {
        if (useFastMd5) {
            return MD5.getHashNio(f);
        }
        return getHash(f, true, DigestAlgo.MD5);
    }

    /**
     * get the byte array of the MD5 for the given FileInterface using standard
     * access
     *
     * @param f
     * @return the byte array representing the MD5
     * @throws IOException
     */
    public static byte[] getHashMd5(File f) throws IOException {
        if (useFastMd5) {
            return MD5.getHash(f);
        }
        return getHash(f, false, DigestAlgo.MD5);
    }

    /**
     * get the byte array of the SHA-1 for the given FileInterface using Nio
     * access
     *
     * @param f
     * @return the byte array representing the SHA-1
     * @throws IOException
     */
    public static byte[] getHashSha1Nio(File f) throws IOException {
        return getHash(f, true, DigestAlgo.SHA1);
    }

    /**
     * get the byte array of the SHA-1 for the given FileInterface using
     * standard access
     *
     * @param f
     * @return the byte array representing the SHA-1
     * @throws IOException
     */
    public static byte[] getHashSha1(File f) throws IOException {
        return getHash(f, false, DigestAlgo.SHA1);
    }

    /**
     * Internal function for No NIO InputStream support
     * @param in
     * @param algo
     * @param buf
     * @return the digest
     * @throws IOException
     */
    private static byte[] getHashNoNio(InputStream in, DigestAlgo algo, byte[] buf) throws IOException {
        Checksum checksum = null;
        int size = 0;
        switch(algo) {
            case ADLER32:
                checksum = new Adler32();
            case CRC32:
                if (checksum == null) {
                    checksum = new CRC32();
                }
                while ((size = in.read(buf)) >= 0) {
                    checksum.update(buf, 0, size);
                }
                in.close();
                buf = null;
                buf = Long.toOctalString(checksum.getValue()).getBytes();
                checksum = null;
                break;
            case MD5:
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
                String algoname = algo.name;
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance(algoname);
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException(algo + " Algorithm not supported by this JVM", e);
                }
                while ((size = in.read(buf)) >= 0) {
                    digest.update(buf, 0, size);
                }
                in.close();
                buf = null;
                buf = digest.digest();
                digest = null;
                break;
            default:
                throw new IOException(algo.name + " Algorithm not supported by this JVM");
        }
        return buf;
    }

    /**
     * Get the Digest for the file using the specified algorithm using access through NIO or not 
     * @param f
     * @param nio
     * @param algo
     * @return the digest
     * @throws IOException
     */
    public static byte[] getHash(File f, boolean nio, DigestAlgo algo) throws IOException {
        if (!f.exists()) {
            throw new FileNotFoundException(f.toString());
        }
        if (algo == DigestAlgo.MD5 && useFastMd5) {
            if (nio) {
                return MD5.getHashNio(f);
            } else {
                return MD5.getHash(f);
            }
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
            if (nio) {
                FileChannel fileChannel = in.getChannel();
                ByteBuffer bb = ByteBuffer.wrap(buf);
                Checksum checksum = null;
                int size = 0;
                switch(algo) {
                    case ADLER32:
                        checksum = new Adler32();
                    case CRC32:
                        if (checksum == null) {
                            checksum = new CRC32();
                        }
                        while ((size = fileChannel.read(bb)) >= 0) {
                            checksum.update(buf, 0, size);
                            bb.clear();
                        }
                        fileChannel.close();
                        fileChannel = null;
                        bb = null;
                        buf = Long.toOctalString(checksum.getValue()).getBytes();
                        checksum = null;
                        break;
                    case MD5:
                    case MD2:
                    case SHA1:
                    case SHA256:
                    case SHA384:
                    case SHA512:
                        String algoname = algo.name;
                        MessageDigest digest = null;
                        try {
                            digest = MessageDigest.getInstance(algoname);
                        } catch (NoSuchAlgorithmException e) {
                            throw new IOException(algo + " Algorithm not supported by this JVM", e);
                        }
                        while ((size = fileChannel.read(bb)) >= 0) {
                            digest.update(buf, 0, size);
                            bb.clear();
                        }
                        fileChannel.close();
                        fileChannel = null;
                        bb = null;
                        buf = digest.digest();
                        digest = null;
                        break;
                    default:
                        throw new IOException(algo.name + " Algorithm not supported by this JVM");
                }
            } else {
                buf = getHashNoNio(in, algo, buf);
                in = null;
                close_me = null;
                return buf;
            }
            in = null;
            close_me = null;
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
     * Get the Digest for the file using the specified algorithm using access through NIO or not 
     * @param stream
     * @param algo
     * @return the digest
     * @throws IOException
     */
    public static byte[] getHash(InputStream stream, DigestAlgo algo) throws IOException {
        if (stream == null) {
            throw new FileNotFoundException();
        }
        if (algo == DigestAlgo.MD5 && useFastMd5) {
            return MD5.getHash(stream);
        }
        try {
            long buf_size = 65536;
            byte[] buf = new byte[(int) buf_size];
            buf = getHashNoNio(stream, algo, buf);
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
     * Get hash with given {@link ChannelBuffer} (from Netty)
     * 
     * @param buffer
     * @param algo
     * @return the hash
     * @throws IOException 
     */
    public static byte[] getHash(ChannelBuffer buffer, DigestAlgo algo) throws IOException {
        Checksum checksum = null;
        byte[] bytes = null;
        switch(algo) {
            case ADLER32:
                checksum = new Adler32();
            case CRC32:
                if (checksum == null) {
                    checksum = new CRC32();
                }
                bytes = new byte[buffer.readableBytes()];
                buffer.getBytes(buffer.readerIndex(), bytes);
                checksum.update(bytes, 0, bytes.length);
                bytes = null;
                bytes = Long.toOctalString(checksum.getValue()).getBytes();
                checksum = null;
                return bytes;
            case MD5:
                if (useFastMd5) {
                    MD5 md5 = new MD5();
                    md5.Update(buffer);
                    bytes = md5.Final();
                    md5 = null;
                    return bytes;
                }
            case MD2:
            case SHA1:
            case SHA256:
            case SHA384:
            case SHA512:
                String algoname = algo.name;
                bytes = new byte[buffer.readableBytes()];
                buffer.getBytes(buffer.readerIndex(), bytes);
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance(algoname);
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException(algoname + " Algorithm not supported by this JVM", e);
                }
                digest.update(bytes, 0, bytes.length);
                bytes = digest.digest();
                digest = null;
                return bytes;
            default:
                throw new IOException(algo.name + " Algorithm not supported by this JVM");
        }
    }

    /**
     * Get hash with given {@link ChannelBuffer} (from Netty)
     *
     * @param buffer
     *            ChannelBuffer to use to get the hash
     * @return the hash
     */
    public static byte[] getHashMd5(ChannelBuffer buffer) {
        try {
            return getHash(buffer, DigestAlgo.MD5);
        } catch (IOException e) {
            MD5 md5 = new MD5();
            md5.Update(buffer);
            byte[] bytes = md5.Final();
            md5 = null;
            return bytes;
        }
    }

    /**
     * Internal representation of Hexadecimal Code
     */
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Get the hexadecimal representation as a String of the array of bytes
     *
     * @param hash
     * @return the hexadecimal representation as a String of the array of bytes
     */
    public static final String getHex(byte[] hash) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[hash[i] >>> 4 & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

    /**
     * Get the array of bytes representation of the hexadecimal String
     *
     * @param hex
     * @return the array of bytes representation of the hexadecimal String
     */
    public static final byte[] getFromHex(String hex) {
        byte from[] = hex.getBytes();
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

    private static byte[] salt = { 'G', 'o', 'l', 'd', 'e', 'n', 'G', 'a', 't', 'e' };

    /**
     * Crypt a password
     * @param pwd to crypt
     * @return the crypted password
     * @throws IOException 
     */
    public static final String passwdCrypt(String pwd) {
        if (useFastMd5) {
            return MD5.passwdCrypt(pwd);
        }
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(DigestAlgo.MD5.name);
        } catch (NoSuchAlgorithmException e) {
            return MD5.passwdCrypt(pwd);
        }
        byte[] bpwd = pwd.getBytes();
        for (int i = 0; i < 16; i++) {
            digest.update(bpwd, 0, bpwd.length);
            digest.update(salt, 0, salt.length);
        }
        byte[] buf = digest.digest();
        digest = null;
        return getHex(buf);
    }

    /**
     * Crypt a password 
     * @param pwd to crypt
     * @return the crypted password
     * @throws IOException 
     */
    public static final byte[] passwdCrypt(byte[] pwd) {
        if (useFastMd5) {
            return MD5.passwdCrypt(pwd);
        }
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(DigestAlgo.MD5.name);
        } catch (NoSuchAlgorithmException e) {
            return MD5.passwdCrypt(pwd);
        }
        for (int i = 0; i < 16; i++) {
            digest.update(pwd, 0, pwd.length);
            digest.update(salt, 0, salt.length);
        }
        byte[] buf = digest.digest();
        digest = null;
        return buf;
    }

    /**
     * 
     * @param pwd
     * @param cryptPwd
     * @return True if the pwd is comparable with the cryptPwd
     * @throws IOException 
     */
    public static final boolean equalPasswd(String pwd, String cryptPwd) {
        String asHex;
        asHex = passwdCrypt(pwd);
        return cryptPwd.equals(asHex);
    }

    /**
     * 
     * @param pwd
     * @param cryptPwd
     * @return True if the pwd is comparable with the cryptPwd
     */
    public static final boolean equalPasswd(byte[] pwd, byte[] cryptPwd) {
        byte[] bytes;
        bytes = passwdCrypt(pwd);
        return Arrays.equals(cryptPwd, bytes);
    }

    /**
     * Test function
     *
     * @param argv
     *            with 2 arguments as filename to hash and full path to the
     *            Native Library
     * @throws IOException 
     */
    public static void main(String argv[]) throws IOException {
        if (argv.length < 1) {
            useFastMd5 = false;
            initializeMd5(false, null);
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                passwdCrypt("Ceci est mon password!");
            }
            System.err.println("Final passwd crypted in " + (System.currentTimeMillis() - start) + "ms is: " + passwdCrypt("Ceci est mon password!"));
            System.err.println("Not enough argument: <full path to the filename to hash> ");
            return;
        }
        initializeMd5(true, null);
        File file = new File(argv[0]);
        System.out.println("FileInterface: " + file.getAbsolutePath());
        byte[] bmd5;
        useFastMd5 = false;
        long start = System.currentTimeMillis();
        try {
            bmd5 = getHashMd5Nio(file);
        } catch (IOException e1) {
            System.err.println("Cannot compute " + DigestAlgo.MD5.name + " for " + argv[1]);
            return;
        }
        long end = System.currentTimeMillis();
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
        }
        System.out.println("Start testing");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashMd5Nio(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.MD5.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.MD5.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        useFastMd5 = true;
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashMd5Nio(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.MD5.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio Fast " + DigestAlgo.MD5.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        useFastMd5 = false;
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashMd5(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.MD5.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.MD5.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        useFastMd5 = true;
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashMd5(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.MD5.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Fast " + DigestAlgo.MD5.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashSha1Nio(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA1.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.SHA1.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHashSha1(file);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA1.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.SHA1.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, true, DigestAlgo.SHA256);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA256.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.SHA256.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.SHA256);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA256.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.SHA256.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, true, DigestAlgo.SHA512);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA512.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.SHA512.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.SHA512);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA512.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.SHA512.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, true, DigestAlgo.CRC32);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.CRC32.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.CRC32.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.CRC32);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.CRC32.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.CRC32.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, true, DigestAlgo.ADLER32);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.ADLER32.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo Nio JVM " + DigestAlgo.ADLER32.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.ADLER32);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.ADLER32.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.ADLER32.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.MD2);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.MD2.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.MD2.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                bmd5 = getHash(file, false, DigestAlgo.SHA384);
            } catch (IOException e1) {
                System.err.println("Cannot compute " + DigestAlgo.SHA384.name + " for " + argv[1]);
                return;
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Algo JVM " + DigestAlgo.SHA384.name + " is " + getHex(bmd5) + "(" + bmd5.length + ")" + " in " + (end - start) + " ms");
    }
}
