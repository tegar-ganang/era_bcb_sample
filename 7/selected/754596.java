package net.sourceforge.myvd.protocol.ldap.mina.asn1.primitives;

import java.io.Serializable;
import net.sourceforge.myvd.protocol.ldap.mina.asn1.codec.DecoderException;

/**
 * Implement the Bit String primitive type. A BitString is internally stored as
 * an array of int.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BitString implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A null MutableString */
    public static final BitString EMPTY_STRING = new BitString();

    public static final boolean STREAMED = true;

    /** The default length of an BitString */
    private static final int DEFAULT_LENGTH = 8;

    /** The number of unused ints */
    private int nbUnusedBits;

    /** Tells if the OctetString is streamed or not */
    private boolean isStreamed;

    /** The string is stored in a byte array */
    private byte[] bytes;

    /** Actual length of the byte array */
    private int nbBytes;

    /** Actual length of the bit string */
    private int nbBits;

    /**
     * Creates a BitString, with a default length.
     */
    public BitString() {
        bytes = new byte[DEFAULT_LENGTH];
        nbBytes = 0;
        isStreamed = false;
        nbUnusedBits = 0;
        nbBits = 0;
    }

    /**
     * Creates a BitString with a specific length (length is the number of
     * bytes).
     * 
     * @param length
     *            The BitString length (it's a number of bits)
     */
    public BitString(int length) {
        nbBits = length;
        nbBytes = (length / 8) + (((length % 8) != 0) ? 1 : 0);
        nbUnusedBits = length % 8;
        if (nbBytes > DEFAULT_LENGTH) {
            isStreamed = true;
            bytes = new byte[nbBytes];
        } else {
            isStreamed = false;
            bytes = new byte[nbBytes];
        }
    }

    /**
     * Creates a streamed BitString with a specific length. Actually, it's just
     * a simple BitString. TODO Implement streaming.
     * 
     * @param length
     *            The BitString length, in number of bits
     * @param isStreamed
     *            Tells if the BitString must be streamed or not
     */
    public BitString(int length, boolean isStreamed) {
        nbBits = length;
        this.isStreamed = isStreamed;
        nbBytes = (length / 8) + (((length % 8) != 0) ? 1 : 0);
        nbUnusedBits = length % 8;
        if (isStreamed) {
            bytes = new byte[nbBytes];
        } else {
            bytes = new byte[nbBytes];
        }
    }

    /**
     * Creates a BitString with a value.
     * 
     * @param bytes
     *            The value to store. The first byte contains the number of
     *            unused bits
     */
    public BitString(byte[] bytes) {
        nbBytes = bytes.length - 1;
        if (nbBytes > DEFAULT_LENGTH) {
            isStreamed = true;
            bytes = new byte[nbBytes];
        } else {
            isStreamed = false;
            bytes = new byte[nbBytes];
        }
        setBytes(bytes, nbBytes);
    }

    /**
     * Set the value into the bytes.
     * 
     * @param bytes
     *            The bytes to copy
     * @param nbBytes
     *            Number of bytes to copy
     */
    private void setBytes(byte[] bytes, int nbBytes) {
        nbUnusedBits = bytes[0] & 0x07;
        nbBits = (nbBytes * 8) - nbUnusedBits;
        for (int i = 0; i < nbBytes; i++) {
            this.bytes[i] = bytes[i + 1];
        }
    }

    /**
     * Set a new BitString in the BitString. It will replace the old BitString,
     * and reset the current length with the new one.
     * 
     * @param bytes
     *            The string to store
     */
    public void setData(byte[] bytes) {
        if ((bytes == null) || (bytes.length == 0)) {
            nbBits = -1;
            return;
        }
        int nbBytes = bytes.length - 1;
        if ((nbBytes > DEFAULT_LENGTH) && (bytes.length < nbBytes)) {
            bytes = new byte[nbBytes];
        }
        setBytes(bytes, nbBytes);
    }

    /**
     * Get the representation of a BitString
     * 
     * @return A byte array which represent the BitString
     */
    public byte[] getData() {
        return bytes;
    }

    /**
     * Get the number of unused bits
     * 
     * @return A byte which represent the number of unused bits
     */
    public byte getUnusedBits() {
        return (byte) nbUnusedBits;
    }

    /**
     * Get the bit stored into the BitString at a specific position? The
     * position start at 0, which is on the left : With '1001 000x', where x is
     * an unused bit, ^ ^ ^^ | | || | | |+---- getBit(7) -> DecoderException | |
     * +----- getBit(6) = 0 | +---------- getBit(2) = 0 +------------ getBit(0) =
     * 1
     * 
     * @param pos
     *            The position of the requested bit.
     * @return <code>true</code> if the bit is set, <code>false</code>
     *         otherwise
     */
    public boolean getBit(int pos) throws DecoderException {
        if (pos > nbBits) {
            throw new DecoderException("Cannot get a bit at position " + pos + " when the BitString contains only " + nbBits + " ints");
        }
        int posInt = pos / 8;
        int bitNumber = 7 - (pos % 8);
        int res = bytes[posInt] & (1 << bitNumber);
        return res != 0;
    }

    /**
     * Return a native String representation of the BitString.
     * 
     * @return A String representing the BitString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        try {
            for (int i = 0; i < nbBits; i++) {
                if (getBit(i)) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
        } catch (DecoderException de) {
            return "Invalid BitString";
        }
        return sb.toString();
    }

    /**
     * Tells if the OctetString is streamed or not
     * 
     * @return <code>true</code> if the OctetString is streamed.
     */
    public boolean isStreamed() {
        return isStreamed;
    }
}
