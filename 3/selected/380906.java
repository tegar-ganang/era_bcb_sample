package alto.sec.x509;

import alto.io.u.Hex;
import alto.sec.util.*;
import java.io.IOException;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represent the Key Identifier ASN.1 object.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class KeyIdentifier {

    public static final byte[] Encode(PublicKey pubKey) throws IOException {
        DerValue algAndKey = new DerValue(pubKey.getEncoded());
        if (algAndKey.tag != DerValue.tag_Sequence) throw new IOException("PublicKey value is not a valid " + "X.509 public key"); else {
            AlgorithmId algid = AlgorithmId.parse(algAndKey.data.getDerValue());
            byte[] key = algAndKey.data.getUnalignedBitString().toByteArray();
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e3) {
                throw new IOException("SHA1 not supported");
            }
            md.update(key);
            return md.digest();
        }
    }

    private byte[] octetString;

    /**
     * Create a KeyIdentifier with the passed bit settings.
     *
     * @param octetString the octet string identifying the key identifier.
     */
    public KeyIdentifier(byte[] octetString) {
        this.octetString = octetString.clone();
    }

    /**
     * Create a KeyIdentifier from the DER encoded value.
     *
     * @param val the DerValue
     */
    public KeyIdentifier(DerValue val) throws IOException {
        octetString = val.getOctetString();
    }

    /**
     * Creates a KeyIdentifier from a public-key value.
     *
     * <p>From RFC2459: Two common methods for generating key identifiers from
     * the public key are:
     * <ol>
     * <li>The keyIdentifier is composed of the 160-bit SHA-1 hash of the
     * value of the BIT STRING subjectPublicKey (excluding the tag,
     * length, and number of unused bits).
     * <p>
     * <li>The keyIdentifier is composed of a four bit type field with
     * the value 0100 followed by the least significant 60 bits of the
     * SHA-1 hash of the value of the BIT STRING subjectPublicKey.
     * </ol>
     * <p>This method supports method 1.
     *
     * @param pubKey the public key from which to construct this KeyIdentifier
     * @throws IOException on parsing errors
     */
    public KeyIdentifier(PublicKey pubKey) throws IOException {
        super();
        this.octetString = Encode(pubKey);
    }

    /**
     * Return the value of the KeyIdentifier as byte array.
     */
    public byte[] getIdentifier() {
        return octetString.clone();
    }

    /**
     * Returns a printable representation of the KeyUsage.
     */
    public String toString() {
        String s = "KeyIdentifier [\n";
        s += Hex.encode(octetString);
        s += "]\n";
        return (s);
    }

    /**
     * Write the KeyIdentifier to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the object to.
     * @exception IOException
     */
    void encode(DerOutputStream out) throws IOException {
        out.putOctetString(octetString);
    }

    /**
     * Returns a hash code value for this object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        int retval = 0;
        for (int i = 0; i < octetString.length; i++) retval += octetString[i] * i;
        return retval;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KeyIdentifier)) return false;
        return java.util.Arrays.equals(octetString, ((KeyIdentifier) other).getIdentifier());
    }
}
