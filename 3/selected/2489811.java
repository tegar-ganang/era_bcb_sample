package ow.id;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An ID with arbitrary length.
 * Note that internal representation is big endian.
 */
public final class ID implements java.io.Externalizable, Comparable<ID> {

    public static final boolean USE_INT_ARRAY = false;

    private static final Logger logger = Logger.getLogger("id");

    private static final int MAX_SIZE = 127;

    private static MessageDigest md = null;

    private static final String mdAlgoName = "SHA1";

    static {
        try {
            md = MessageDigest.getInstance(mdAlgoName);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    private int size;

    private byte[] value;

    private volatile BigInteger bigInteger;

    private volatile int hashCode;

    /**
	 * A constructor. Do not copy the given byte array and keep it.
	 *
	 * @param id ID in big endian.
	 * @param size ID size in byte.
	 */
    private ID(byte[] id, int size) {
        if (size > MAX_SIZE) {
            logger.log(Level.WARNING, "size set as " + MAX_SIZE + " even though the give size is " + size + ".");
            size = MAX_SIZE;
        }
        this.size = size;
        this.value = new byte[size];
        int idLength = Math.min(id.length, size);
        System.arraycopy(id, 0, this.value, size - idLength, idLength);
        this.init();
    }

    private void init() {
        this.bigInteger = new BigInteger(1, this.value);
        int hashedvalue = 0;
        int pos = 24;
        for (int i = 0; i < this.size; i++) {
            hashedvalue ^= (this.value[i] << pos);
            if (pos <= 0) pos = 24; else pos -= 8;
        }
        this.hashCode = hashedvalue;
    }

    private static ID canonicalize(ID obj) {
        return obj;
    }

    public ID copy(int newSize) {
        return canonicalize(new ID(this.value, newSize));
    }

    /**
	 * Returns a new ID instance with the value specified by the given byte array.
	 *
	 * @param id value in big endian.
	 * @param size ID size in byte.
	 */
    public static ID getID(byte[] id, int size) {
        byte[] value = new byte[size];
        int copyLen = Math.min(id.length, size);
        int fromIdx = id.length - 1;
        int toIdx = size - 1;
        for (int i = 0; i < copyLen; i++) {
            value[toIdx--] = id[fromIdx--];
        }
        return canonicalize(new ID(value, size));
    }

    /**
	 * Returns a new ID instance with the value specified by the given BigInteger.
	 */
    public static ID getID(BigInteger id, int size) {
        if (id.compareTo(BigInteger.ZERO) < 0) {
            id = id.add(BigInteger.ONE.shiftLeft(size * 8));
        }
        byte[] value = id.toByteArray();
        return getID(value, size);
    }

    /**
	 * Returns a new ID instance with the value given as a String.
	 *
	 * @param hexString ID string.
	 * @param size size of the ID in byte.
	 * @return generated ID.
	 */
    public static ID getID(String hexString, int size) {
        if (hexString.length() < size * 2) {
            throw new IllegalArgumentException("Given ID is too short: " + hexString);
        }
        byte[] id = new byte[size];
        for (int i = 0, idx = (size - 1) * 2; i < size; i++, idx -= 2) {
            int b = Integer.parseInt(hexString.substring(idx, idx + 2), 16);
            id[size - 1 - i] = (byte) (b & 0xff);
        }
        return canonicalize(new ID(id, size));
    }

    private static Random rnd = new Random();

    /**
	 * Returns a new ID having random value.
	 */
    public static ID getRandomID(int size) {
        byte[] value = new byte[size];
        rnd.nextBytes(value);
        return canonicalize(new ID(value, size));
    }

    /**
	 * Returns a newly generated ID with a hashed value of the specified byte array.
	 * The size of ID is 20.
	 */
    public static ID getSHA1BasedID(byte[] bytes) {
        return getSHA1BasedID(bytes, 20);
    }

    /**
	 * Returns a newly generated ID with a hashed value of the specified byte array.
	 * Maximum size of ID is 160 bit (20 byte) because the hashing algorithm is SHA1.
	 *
	 * @param sizeInByte the size of generated ID in byte (<= 20).
	 */
    public static ID getSHA1BasedID(byte[] bytes, int sizeInByte) {
        byte[] value;
        synchronized (md) {
            value = md.digest(bytes);
        }
        if (sizeInByte > value.length) {
            throw new IllegalArgumentException("size is too large: " + sizeInByte + " > " + value.length);
        }
        return canonicalize(new ID(value, sizeInByte));
    }

    /**
	 * Returns a newly generated ID based on the hashcode of the specified object.
	 * Maximum size of ID is 160 bit (20 byte) because the hashing algorithm is SHA1.
	 *
	 * @param sizeInByte the size of generated ID in byte (<= 20).
	 */
    public static ID getHashcodeBasedID(Object obj, int sizeInByte) {
        int hashcode = obj.hashCode();
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) ((hashcode >>> ((3 - i) * 8)) & 0xff);
        }
        return getSHA1BasedID(bytes, sizeInByte);
    }

    /**
	 * An utility method which parses an ID specified by a string or a hexadecimal number.
	 * A string is converted the corresponding ID by being hashed with SHA1.
	 */
    public static ID parseID(String arg, int size) {
        ID key = null;
        if (arg.startsWith("id:")) {
            key = getID(arg.substring(3), size);
        } else {
            try {
                key = getSHA1BasedID(arg.getBytes("UTF-8"), size);
            } catch (UnsupportedEncodingException e) {
            }
        }
        return key;
    }

    /**
	 * Length of this ID in byte.
	 */
    public int getSize() {
        return this.size;
    }

    public byte[] getValue() {
        return this.value;
    }

    /**
	 * Returns bits.
	 *
	 * @param from starting index from LSB.
	 * @param len number of bits.
	 */
    public int getBits(int from, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            int index = from + i;
            if (index >= 0) {
                if (this.bigInteger.testBit(from + i)) {
                    result |= (1 << i);
                }
            }
        }
        return result;
    }

    /**
	 * Returns an ID whose value is (this << n).
	 */
    public ID shiftLeft(int n) {
        return getID(this.toBigInteger().shiftLeft(n), this.size);
    }

    /**
	 * Returns an ID whose value is (this >> n).
	 */
    public ID shiftRight(int n) {
        return getID(this.toBigInteger().shiftRight(n), this.size);
    }

    /**
	 * Returns an ID whose value is equivalent to this ID
	 * with the designated bit set.
	 */
    public ID setBit(int n) {
        return getID(this.toBigInteger().setBit(n), this.size);
    }

    /**
	 * Returns an ID whose value is equivalent to this ID
	 * with the designated bit cleared.
	 */
    public ID clearBit(int n) {
        return getID(this.toBigInteger().clearBit(n), this.size);
    }

    public static int matchLengthFromMSB(ID a, ID b) {
        int aRemainingSize = a.getSize();
        int bRemainingSize = b.getSize();
        int aIndex = 0, bIndex = 0;
        int matchBytes = 0;
        int matchBits = 8;
        while (aRemainingSize > bRemainingSize) {
            int v = 0xff & a.value[aIndex];
            if (v != 0) {
                while (v != 0) {
                    v >>>= 1;
                    matchBits--;
                }
                return matchBytes * 8 + matchBits;
            }
            matchBytes++;
            aIndex++;
            aRemainingSize--;
        }
        while (bRemainingSize > aRemainingSize) {
            int v = 0xff & b.value[bIndex];
            if (v != 0) {
                while (v != 0) {
                    v >>>= 1;
                    matchBits--;
                }
                return matchBytes * 8 + matchBits;
            }
            matchBytes++;
            bIndex++;
            bRemainingSize--;
        }
        for (int i = 0; i < aRemainingSize; i++) {
            int va = 0xff & a.value[aIndex];
            int vb = 0xff & b.value[bIndex];
            if (va != vb) {
                int xored = va ^ vb;
                while (xored != 0) {
                    xored >>>= 1;
                    matchBits--;
                }
                return matchBytes * 8 + matchBits;
            }
            matchBytes++;
            aIndex++;
            bIndex++;
        }
        return matchBytes * 8;
    }

    public BigInteger toBigInteger() {
        return this.bigInteger;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ID)) return false;
        ID other = (ID) obj;
        if (this.size != other.size) {
            return false;
        }
        for (int i = 0; i < this.size; i++) {
            if (this.value[i] != other.value[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        return this.hashCode;
    }

    /** Return the String representation of this ID instance. */
    public String toString() {
        return this.toString(0);
    }

    /** Return the String representation of this ID instance. */
    public String toString(int verboseLevel) {
        int numOfDigits;
        if (verboseLevel < 0) numOfDigits = Math.min(2, this.size); else numOfDigits = this.size;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.size; i++) {
            int b = 0xff & this.value[i];
            if (b < 16) sb.append("0");
            sb.append(Integer.toHexString(b));
            if (--numOfDigits <= 0) break;
        }
        return sb.toString();
    }

    /**
	 * A public constructor with no argument required to implement Externalizable interface.
	 */
    public ID() {
    }

    public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
        out.writeByte(this.size);
        out.write(this.value, 0, this.size);
    }

    public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
        this.size = in.readByte();
        this.value = new byte[this.size];
        in.readFully(this.value);
        this.init();
    }

    public int compareTo(ID other) {
        return this.toBigInteger().compareTo(other.toBigInteger());
    }
}
