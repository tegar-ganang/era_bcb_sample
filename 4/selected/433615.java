package org.tcpfile.net;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.mina.common.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tcpfile.main.Misc;
import sun.misc.BASE64Encoder;

/**
 * Static library for handling of ByteArrays.
 * @author Stivo
 *
 */
public class ByteArray {

    private static Logger log = LoggerFactory.getLogger(ByteArray.class);

    private ByteArray() {
    }

    public static byte[] zip(byte[] input) {
        log.trace("Zipping input with length " + Misc.prettyPrintSize(input.length));
        byte[] returns = gzip(input);
        if (returns != null) log.trace("Done zipping output length: " + Misc.prettyPrintSize(returns.length));
        return returns;
    }

    public static byte[] unzip(byte[] input) {
        log.trace("Unzipping input with length " + Misc.prettyPrintSize(input.length));
        byte[] returns = gunzip(input);
        if (returns != null) log.trace("Done unzipping output length: " + Misc.prettyPrintSize(returns.length));
        return returns;
    }

    /**
	 * Takes a byte array and zips it using GZIPOutputStream
	 * @param input
	 * @return
	 */
    public static byte[] gzip(byte[] input) {
        try {
            ByteArrayOutputStream bla = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(bla);
            gzos.write(input);
            gzos.finish();
            input = new byte[5];
            byte[] output = bla.toByteArray();
            bla.close();
            gzos.close();
            return output;
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * Unzips a given byte array.
	 * Needs about 4 times less memory than unzip 2 and is about as much faster...
	 * dont know why
	 * @param input A GZIPped byte[]
	 * @return unzipped byte[]
	 */
    public static byte[] gunzip(byte[] input) {
        try {
            ByteArrayInputStream bla = new ByteArrayInputStream(input);
            GZIPInputStream gzis = new GZIPInputStream(bla);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = gzis.read(buf)) > 0) out.write(buf, 0, read);
            out.flush();
            byte[] output = out.toByteArray();
            gzis.close();
            bla.close();
            out.close();
            return output;
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * Copies the given part of a byte[] into a new array
	 * @param input The array which is copied from
	 * @param start
	 * @param end
	 * @return 
	 */
    public static byte[] copyfromto(byte[] input, int start) {
        return copyfromto(input, start, Integer.MAX_VALUE);
    }

    /**
	 * Copies the given part of a byte[] into a new array
	 * @param input The array which is copied from
	 * @param start
	 * @param end
	 * @return 
	 */
    public static byte[] copyfromto(byte[] input, int start, int end) {
        if (end > input.length) end = input.length;
        byte[] output = new byte[end - start];
        System.arraycopy(input, start, output, 0, output.length);
        return output;
    }

    /**
	 * Appends one array to another.
	 * @param input
	 * @param input2
	 * @return
	 */
    public static byte[] concat(byte[] input, byte[] input2) {
        byte[] output = new byte[input.length + input2.length];
        System.arraycopy(input, 0, output, 0, input.length);
        System.arraycopy(input2, 0, output, input.length, input2.length);
        return output;
    }

    /**
	 * @deprecated for the server
	 */
    public static byte[] oldtoByteArray(Serializable obj) {
        try {
            ByteArrayOutputStream bla = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bla);
            out.writeObject(obj);
            return bla.toByteArray();
        } catch (IOException e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * @deprecated for the server
	 */
    public static Object oldtoObject(byte[] input) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(input);
            ObjectInputStream in = new ObjectInputStream(bis);
            Object gelezen_veld = in.readObject();
            in.close();
            return gelezen_veld;
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * Takes a serializable Object and returns it as a byte array
	 * @param obj
	 * @return
	 */
    public static byte[] toByteArray(Object obj) {
        try {
            ByteArrayOutputStream bla = new ByteArrayOutputStream();
            ObjectOutputStream out = new MinaObjectOutputStream(bla);
            out.writeObject(obj);
            return bla.toByteArray();
        } catch (IOException e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * Takes a serialized object in form of a byte array and returns the object
	 * @param input the serialized object
	 * @return Object! Needs casting. Returns null in case of an error
	 */
    public static Serializable toObject(byte[] input) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(input);
            ObjectInputStream in = new MinaObjectInputStream(bis);
            Object gelezen_veld = in.readObject();
            in.close();
            return (Serializable) gelezen_veld;
        } catch (Exception e) {
            log.warn("", e);
        }
        return null;
    }

    /**
	 * Does return a byte[] from the given Hex String
	 * Has been tested and should work for most cases (unlike its predecessor)
	 * @param hex
	 * @return
	 */
    public static byte[] getBytesfromHexString(String hex) {
        hex = hex.replace(":", "").toLowerCase();
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    /**
	 * Lets use dumpbytes64 from now on
	 * For Use with the server still needed
	 * @param bs
	 * @return
	 */
    public static String dumpBytes16(byte[] bs) {
        return dumpBytes(bs).replace(":", "").toLowerCase();
    }

    /**
	 * Uses the Sun Base64 Encoder to encode a byte array to a String
	 * @param bs the input
	 * @return the String, for Example: pkxuNd0vHOM4CIkUqWW5ryCrF/o=
	 */
    public static String dumpBytes64(byte[] bs) {
        BASE64Encoder be = new BASE64Encoder();
        return be.encode(bs);
    }

    private static final String digits = "0123456789ABCDEF";

    /**
	 * Returns a byte array in Hex Format
	 * @param bs
	 * @return
	 */
    public static String dumpBytes(byte[] bs) {
        if (bs == null) return "";
        if (bs.length == 0) return "";
        StringBuffer ret = new StringBuffer(bs.length * 3);
        for (int i = 0; i < bs.length; i++) {
            int v = bs[i] & 0xff;
            ret.append(digits.charAt(v >> 4));
            ret.append(digits.charAt(v & 0xf));
            ret.append(":");
        }
        String ret2 = Misc.cutEnd(ret.toString(), 1).toUpperCase();
        return ret2;
    }

    public static boolean startsWith(byte[] header, byte[] full) {
        int i = 0;
        for (byte b : header) {
            if (b != full[i++]) return false;
        }
        return true;
    }

    public static boolean startsWith(byte[] header, ByteBuffer bb) {
        int i = 0;
        for (byte b : header) {
            if (b != bb.get(i++)) return false;
        }
        return true;
    }

    public static boolean equal(byte[] b1, byte[] b2) {
        return Arrays.equals(b1, b2);
    }

    public static byte[] intAsBytes(int bla) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) bla;
            bla = bla / 256;
        }
        return b;
    }

    public static int retrieveIntfromBytes(byte[] b) {
        int bla = 0;
        int blu = 0;
        int mal = 1;
        for (int i = 3; i >= 0; i--) {
            blu = b[i];
            if (blu < 0) blu = 256 + blu;
            bla += blu * mal;
            mal *= 256;
        }
        return bla;
    }

    public static byte[] getSecureRandomBytes(int length) {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[length];
        sr.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getRandomBytes(int length) {
        Random sr = new Random();
        byte[] bytes = new byte[length];
        sr.nextBytes(bytes);
        return bytes;
    }
}

class MinaObjectOutputStream extends ObjectOutputStream {

    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        if (desc.getClass().isPrimitive()) {
            write(0);
            super.writeClassDescriptor(desc);
        } else {
            write(1);
            String replace = "org.tcpfile.";
            String out = desc.getName();
            if (out.startsWith(replace)) {
                out = out.substring(replace.length());
                out = "x." + out;
            }
            writeUTF(out);
        }
    }

    protected MinaObjectOutputStream() throws IOException, SecurityException {
        super();
    }

    protected MinaObjectOutputStream(OutputStream os) throws IOException, SecurityException {
        super(os);
    }
}

class MinaObjectInputStream extends ObjectInputStream {

    public MinaObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        int type = read();
        if (type < 0) {
            throw new EOFException();
        }
        switch(type) {
            case 0:
                return super.readClassDescriptor();
            case 1:
                String className = readUTF();
                className = className.startsWith("x.") ? "org.tcpfile." + className.substring(2) : className;
                Class<?> clazz = Class.forName(className);
                return ObjectStreamClass.lookup(clazz);
            default:
                throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
        }
    }
}
