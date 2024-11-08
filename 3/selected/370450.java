package sun.security.provider;

import java.io.*;
import java.util.*;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.*;
import java.security.spec.DSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import sun.security.util.Debug;
import sun.security.util.DerValue;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.x509.AlgIdDSA;

/**
 * The Digital Signature Standard (using the Digital Signature
 * Algorithm), as described in fips186 of the National Instute of
 * Standards and Technology (NIST), using fips180-1 (SHA-1).
 *
 * @author Benjamin Renaud
 *
 * @version 1.88, 02/02/00
 *
 * @see DSAPublicKey
 * @see DSAPrivateKey
 */
public final class DSA extends Signature {

    private static final boolean debug = false;

    private DSAParams params;

    private BigInteger presetP, presetQ, presetG;

    private BigInteger presetY;

    private BigInteger presetX;

    private MessageDigest dataSHA;

    private int[] Kseed;

    private byte[] KseedAsByteArray;

    private int[] previousKseed;

    private SecureRandom signingRandom;

    /**
     * Construct a blank DSA object. It can generate keys, but must be
     * initialized before being usable for signing or verifying.
     */
    public DSA() throws NoSuchAlgorithmException {
        super("SHA1withDSA");
        dataSHA = MessageDigest.getInstance("SHA");
    }

    /**
     * Initialize the DSA object with a DSA private key.
     * 
     * @param privateKey the DSA private key
     * 
     * @exception InvalidKeyException if the key is not a valid DSA private
     * key.
     */
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (!(privateKey instanceof java.security.interfaces.DSAPrivateKey)) {
            throw new InvalidKeyException("not a DSA private key: " + privateKey);
        }
        java.security.interfaces.DSAPrivateKey priv = (java.security.interfaces.DSAPrivateKey) privateKey;
        this.presetX = priv.getX();
        initialize(priv.getParams());
    }

    /**
     * Initialize the DSA object with a DSA public key.
     * 
     * @param publicKey the DSA public key.
     * 
     * @exception InvalidKeyException if the key is not a valid DSA public
     * key.
     */
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (!(publicKey instanceof java.security.interfaces.DSAPublicKey)) {
            throw new InvalidKeyException("not a DSA public key: " + publicKey);
        }
        java.security.interfaces.DSAPublicKey pub = (java.security.interfaces.DSAPublicKey) publicKey;
        this.presetY = pub.getY();
        initialize(pub.getParams());
    }

    private void initialize(DSAParams params) throws InvalidKeyException {
        dataSHA.reset();
        setParams(params);
    }

    /**
     * Sign all the data thus far updated. The signature is formatted
     * according to the Canonical Encoding Rules, returned as a DER
     * sequence of Integer, r and s.
     *
     * @return a signature block formatted according to the Canonical
     * Encoding Rules.
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     * 
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineVerify
     */
    protected byte[] engineSign() throws SignatureException {
        BigInteger k = generateK(presetQ);
        BigInteger r = generateR(presetP, presetQ, presetG, k);
        BigInteger s = generateS(presetX, presetQ, r, k);
        try {
            DerOutputStream outseq = new DerOutputStream(100);
            outseq.putInteger(r);
            outseq.putInteger(s);
            DerValue result = new DerValue(DerValue.tag_Sequence, outseq.toByteArray());
            return result.toByteArray();
        } catch (IOException e) {
            throw new SignatureException("error encoding signature");
        }
    }

    /**
     * Verify all the data thus far updated. 
     *
     * @param signature the alledged signature, encoded using the
     * Canonical Encoding Rules, as a sequence of integers, r and s.
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     *
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineSign 
     */
    protected boolean engineVerify(byte[] signature) throws SignatureException {
        return engineVerify(signature, 0, signature.length);
    }

    /**
     * Verify all the data thus far updated. 
     *
     * @param signature the alledged signature, encoded using the
     * Canonical Encoding Rules, as a sequence of integers, r and s.
     *
     * @param offset the offset to start from in the array of bytes.
     *
     * @param length the number of bytes to use, starting at offset.
     *
     * @exception SignatureException if the signature object was not
     * properly initialized, or if another exception occurs.
     *
     * @see sun.security.DSA#engineUpdate
     * @see sun.security.DSA#engineSign 
     */
    protected boolean engineVerify(byte[] signature, int offset, int length) throws SignatureException {
        BigInteger r = null;
        BigInteger s = null;
        try {
            DerInputStream in = new DerInputStream(signature, offset, length);
            DerValue[] values = in.getSequence(2);
            r = values[0].getBigInteger();
            s = values[1].getBigInteger();
        } catch (IOException e) {
            throw new SignatureException("invalid encoding for signature");
        }
        if ((r.compareTo(BigInteger.ZERO) == 1) && (r.compareTo(presetQ) == -1) && (s.compareTo(BigInteger.ZERO) == 1) && (s.compareTo(presetQ) == -1)) {
            BigInteger w = generateW(presetP, presetQ, presetG, s);
            BigInteger v = generateV(presetY, presetP, presetQ, presetG, w, r);
            return v.equals(r);
        } else {
            throw new SignatureException("invalid signature: out of range values");
        }
    }

    private void reset() {
        dataSHA.reset();
    }

    BigInteger generateR(BigInteger p, BigInteger q, BigInteger g, BigInteger k) {
        BigInteger temp = g.modPow(k, p);
        return temp.remainder(q);
    }

    BigInteger generateS(BigInteger x, BigInteger q, BigInteger r, BigInteger k) {
        byte[] s2 = dataSHA.digest();
        BigInteger temp = new BigInteger(1, s2);
        BigInteger k1 = k.modInverse(q);
        BigInteger s = x.multiply(r);
        s = temp.add(s);
        s = k1.multiply(s);
        return s.remainder(q);
    }

    BigInteger generateW(BigInteger p, BigInteger q, BigInteger g, BigInteger s) {
        return s.modInverse(q);
    }

    BigInteger generateV(BigInteger y, BigInteger p, BigInteger q, BigInteger g, BigInteger w, BigInteger r) {
        byte[] s2 = dataSHA.digest();
        BigInteger temp = new BigInteger(1, s2);
        temp = temp.multiply(w);
        BigInteger u1 = temp.remainder(q);
        BigInteger u2 = (r.multiply(w)).remainder(q);
        BigInteger t1 = g.modPow(u1, p);
        BigInteger t2 = y.modPow(u2, p);
        BigInteger t3 = t1.multiply(t2);
        BigInteger t5 = t3.remainder(p);
        return t5.remainder(q);
    }

    BigInteger generateK(BigInteger q) {
        BigInteger k = null;
        if (Kseed != null && !Arrays.equals(Kseed, previousKseed)) {
            k = generateK(Kseed, q);
            if (k.signum() > 0 && k.compareTo(q) < 0) {
                previousKseed = new int[Kseed.length];
                System.arraycopy(Kseed, 0, previousKseed, 0, Kseed.length);
                return k;
            }
        }
        SecureRandom random = getSigningRandom();
        while (true) {
            int[] seed = new int[5];
            for (int i = 0; i < 5; i++) seed[i] = random.nextInt();
            k = generateK(seed, q);
            if (k.signum() > 0 && k.compareTo(q) < 0) {
                previousKseed = new int[seed.length];
                System.arraycopy(seed, 0, previousKseed, 0, seed.length);
                return k;
            }
        }
    }

    private SecureRandom getSigningRandom() {
        if (signingRandom == null) {
            if (appRandom != null) signingRandom = appRandom; else signingRandom = new SecureRandom();
        }
        return signingRandom;
    }

    /**
     * Compute k for a DSA signature.
     *
     * @param seed the seed for generating k. This seed should be
     * secure. This is what is refered to as the KSEED in the DSA
     * specification.
     *
     * @param g the g parameter from the DSA key pair.
     */
    BigInteger generateK(int[] seed, BigInteger q) {
        int[] t = { 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0, 0x67452301 };
        int[] tmp = DSA.SHA_7(seed, t);
        byte[] tmpBytes = new byte[tmp.length * 4];
        for (int i = 0; i < tmp.length; i++) {
            int k = tmp[i];
            for (int j = 0; j < 4; j++) {
                tmpBytes[(i * 4) + j] = (byte) (k >>> (24 - (j * 8)));
            }
        }
        BigInteger k = new BigInteger(1, tmpBytes).mod(q);
        return k;
    }

    private static final int round1_kt = 0x5a827999;

    private static final int round2_kt = 0x6ed9eba1;

    private static final int round3_kt = 0x8f1bbcdc;

    private static final int round4_kt = 0xca62c1d6;

    /**
    * Computes set 1 thru 7 of SHA-1 on m1. */
    static int[] SHA_7(int[] m1, int[] h) {
        int[] W = new int[80];
        System.arraycopy(m1, 0, W, 0, m1.length);
        int temp = 0;
        for (int t = 16; t <= 79; t++) {
            temp = W[t - 3] ^ W[t - 8] ^ W[t - 14] ^ W[t - 16];
            W[t] = ((temp << 1) | (temp >>> (32 - 1)));
        }
        int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4];
        for (int i = 0; i < 20; i++) {
            temp = ((a << 5) | (a >>> (32 - 5))) + ((b & c) | ((~b) & d)) + e + W[i] + round1_kt;
            e = d;
            d = c;
            c = ((b << 30) | (b >>> (32 - 30)));
            b = a;
            a = temp;
        }
        for (int i = 20; i < 40; i++) {
            temp = ((a << 5) | (a >>> (32 - 5))) + (b ^ c ^ d) + e + W[i] + round2_kt;
            e = d;
            d = c;
            c = ((b << 30) | (b >>> (32 - 30)));
            b = a;
            a = temp;
        }
        for (int i = 40; i < 60; i++) {
            temp = ((a << 5) | (a >>> (32 - 5))) + ((b & c) | (b & d) | (c & d)) + e + W[i] + round3_kt;
            e = d;
            d = c;
            c = ((b << 30) | (b >>> (32 - 30)));
            b = a;
            a = temp;
        }
        for (int i = 60; i < 80; i++) {
            temp = ((a << 5) | (a >>> (32 - 5))) + (b ^ c ^ d) + e + W[i] + round4_kt;
            e = d;
            d = c;
            c = ((b << 30) | (b >>> (32 - 30)));
            b = a;
            a = temp;
        }
        int[] md = new int[5];
        md[0] = h[0] + a;
        md[1] = h[1] + b;
        md[2] = h[2] + c;
        md[3] = h[3] + d;
        md[4] = h[4] + e;
        return md;
    }

    /**
     * This implementation recognizes the following parameter:<dl>
     *
     * <dt><tt>Kseed</tt> 
     * 
     * <dd>a byte array.
     *
     * </dl>
     *
     * @deprecated
     */
    protected void engineSetParameter(String key, Object param) {
        if (key.equals("KSEED")) {
            if (param instanceof byte[]) {
                Kseed = byteArray2IntArray((byte[]) param);
                KseedAsByteArray = (byte[]) param;
            } else {
                debug("unrecognized param: " + key);
                throw new InvalidParameterException("Kseed not a byte array");
            }
        } else {
            throw new InvalidParameterException("invalid parameter");
        }
    }

    /**
     * Return the value of the requested parameter. Recognized
     * parameters are: 
     *
     * <dl>
     *
     * <dt><tt>Kseed</tt> 
     * 
     * <dd>a byte array.
     *
     * </dl>
     *
     * @return the value of the requested parameter.
     *
     * @see java.security.SignatureEngine 
     *
     * @deprecated
     */
    protected Object engineGetParameter(String key) {
        if (key.equals("KSEED")) {
            return KseedAsByteArray;
        } else {
            return null;
        }
    }

    /**
     * Set the algorithm object.
     */
    private void setParams(DSAParams params) throws InvalidKeyException {
        if (params == null) throw new InvalidKeyException("DSA public key lacks parameters");
        this.params = params;
        this.presetP = params.getP();
        this.presetQ = params.getQ();
        this.presetG = params.getG();
    }

    /**
     * Update a byte to be signed or verified.
     *
     * @param b the byte to updated.
     */
    protected void engineUpdate(byte b) {
        dataSHA.update(b);
    }

    /**
     * Update an array of bytes to be signed or verified.
     * 
     * @param data the bytes to be updated.
     */
    protected void engineUpdate(byte[] data, int off, int len) {
        dataSHA.update(data, off, len);
    }

    /**
     * Return a human readable rendition of the engine.
     */
    public String toString() {
        String printable = "DSA Signature";
        if (presetP != null && presetQ != null && presetG != null) {
            printable += "\n\tp: " + Debug.toHexString(presetP);
            printable += "\n\tq: " + Debug.toHexString(presetQ);
            printable += "\n\tg: " + Debug.toHexString(presetG);
        } else {
            printable += "\n\t P, Q or G not initialized.";
        }
        if (presetY != null) {
            printable += "\n\ty: " + Debug.toHexString(presetY);
        }
        if (presetY == null && presetX == null) {
            printable += "\n\tUNINIIALIZED";
        }
        return printable;
    }

    private int[] byteArray2IntArray(byte[] byteArray) {
        int j = 0;
        byte[] newBA;
        int mod = byteArray.length % 4;
        switch(mod) {
            case 3:
                newBA = new byte[byteArray.length + 1];
                break;
            case 2:
                newBA = new byte[byteArray.length + 2];
                break;
            case 1:
                newBA = new byte[byteArray.length + 3];
                break;
            default:
                newBA = new byte[byteArray.length + 0];
                break;
        }
        System.arraycopy(byteArray, 0, newBA, 0, byteArray.length);
        int[] newSeed = new int[newBA.length / 4];
        for (int i = 0; i < newBA.length; i += 4) {
            newSeed[j] = newBA[i + 3] & 0xFF;
            newSeed[j] |= (newBA[i + 2] << 8) & 0xFF00;
            newSeed[j] |= (newBA[i + 1] << 16) & 0xFF0000;
            newSeed[j] |= (newBA[i + 0] << 24) & 0xFF000000;
            j++;
        }
        return newSeed;
    }

    private static void debug(Exception e) {
        if (debug) {
            e.printStackTrace();
        }
    }

    private static void debug(String s) {
        if (debug) {
            System.err.println(s);
        }
    }
}
