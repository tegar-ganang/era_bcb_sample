package org.metastatic.jessie.pki.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import org.metastatic.jessie.pki.der.DER;
import org.metastatic.jessie.pki.der.DERReader;
import org.metastatic.jessie.pki.der.DERValue;
import org.metastatic.jessie.pki.der.DERWriter;
import org.metastatic.jessie.pki.der.OID;

public abstract class RSA extends SignatureSpi implements Cloneable {

    /**
   * digestAlgorithm OBJECT IDENTIFIER ::=
   *   { iso(1) member-body(2) US(840) rsadsi(113549) digestAlgorithm(2) }
   */
    protected static final OID DIGEST_ALGORITHM = new OID("1.2.840.113549.2");

    protected final OID digestAlgorithm;

    protected final MessageDigest md;

    protected RSAPrivateKey signerKey;

    protected RSAPublicKey verifierKey;

    protected RSA(MessageDigest md, OID digestAlgorithm) {
        super();
        this.md = md;
        this.digestAlgorithm = digestAlgorithm;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    protected Object engineGetParameter(String param) {
        throw new UnsupportedOperationException("deprecated");
    }

    protected void engineSetParameter(String param, Object value) {
        throw new UnsupportedOperationException("deprecated");
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (!(privateKey instanceof RSAPrivateKey)) throw new InvalidKeyException();
        verifierKey = null;
        signerKey = (RSAPrivateKey) privateKey;
    }

    protected void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        engineInitSign(privateKey);
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (!(publicKey instanceof RSAPublicKey)) throw new InvalidKeyException();
        signerKey = null;
        verifierKey = (RSAPublicKey) publicKey;
    }

    protected void engineUpdate(byte b) throws SignatureException {
        if (signerKey == null && verifierKey == null) throw new SignatureException("not initialized");
        md.update(b);
    }

    protected void engineUpdate(byte[] buf, int off, int len) throws SignatureException {
        if (signerKey == null && verifierKey == null) throw new SignatureException("not initialized");
        md.update(buf, off, len);
    }

    protected byte[] engineSign() throws SignatureException {
        if (signerKey == null) throw new SignatureException("not initialized for signing");
        ArrayList digestAlg = new ArrayList(2);
        digestAlg.add(new DERValue(DER.OBJECT_IDENTIFIER, digestAlgorithm));
        digestAlg.add(new DERValue(DER.NULL, null));
        ArrayList digestInfo = new ArrayList(2);
        digestInfo.add(new DERValue(DER.SEQUENCE, digestAlg));
        digestInfo.add(new DERValue(DER.OCTET_STRING, md.digest()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DERWriter.write(out, new DERValue(DER.SEQUENCE, digestInfo));
        } catch (IOException ioe) {
            throw new SignatureException(ioe.toString());
        }
        byte[] buf = out.toByteArray();
        md.reset();
        int k = signerKey.getModulus().bitLength();
        k = (k >>> 3) + ((k & 7) == 0 ? 0 : 1);
        if (buf.length < k - 3) {
            throw new SignatureException("RSA modulus too small");
        }
        byte[] d = new byte[k];
        d[1] = 0x01;
        for (int i = 2; i < k - buf.length - 1; i++) d[i] = (byte) 0xFF;
        System.arraycopy(buf, 0, d, k - buf.length, buf.length);
        BigInteger eb = new BigInteger(d);
        byte[] ed = eb.modPow(signerKey.getPrivateExponent(), signerKey.getModulus()).toByteArray();
        if (ed.length < k) {
            byte[] b = new byte[k];
            System.arraycopy(eb, 0, b, k - ed.length, ed.length);
            ed = b;
        } else if (ed.length > k) {
            if (ed.length != k + 1) {
                throw new SignatureException("modPow result is larger than the modulus");
            }
            byte[] b = new byte[k];
            System.arraycopy(ed, 1, b, 0, k);
            ed = b;
        }
        return ed;
    }

    protected int engineSign(byte[] out, int off, int len) throws SignatureException {
        if (out == null || off < 0 || len < 0 || off + len > out.length) throw new SignatureException("illegal output argument");
        byte[] result = engineSign();
        if (result.length > len) throw new SignatureException("not enough space for signature");
        System.arraycopy(result, 0, out, off, result.length);
        return result.length;
    }

    protected boolean engineVerify(byte[] sig) throws SignatureException {
        if (verifierKey == null) throw new SignatureException("not initialized for verifying");
        if (sig == null) throw new SignatureException("no signature specified");
        int k = verifierKey.getModulus().bitLength();
        k = (k >>> 3) + ((k & 7) == 0 ? 0 : 1);
        if (sig.length != k) throw new SignatureException("signature is the wrong size (expecting " + k + " bytes, got " + sig.length + ")");
        BigInteger ed = new BigInteger(1, sig);
        byte[] eb = ed.modPow(verifierKey.getPublicExponent(), verifierKey.getModulus()).toByteArray();
        int i = 0;
        if (eb[0] == 0x00) {
            for (i = 1; i < eb.length && eb[i] == 0x00; i++) ;
            if (i == 1) throw new SignatureException("wrong RSA padding");
            i--;
        } else if (eb[0] == 0x01) {
            for (i = 1; i < eb.length && eb[i] != 0x00; i++) if (eb[i] != (byte) 0xFF) throw new IllegalArgumentException("wrong RSA padding");
        } else throw new SignatureException("wrong RSA padding type");
        byte[] d = new byte[eb.length - i - 1];
        System.arraycopy(eb, i + 1, d, 0, eb.length - i - 1);
        DERReader der = new DERReader(d);
        try {
            DERValue val = der.read();
            if (val.getTag() != DER.SEQUENCE) throw new SignatureException("failed to parse DigestInfo");
            val = der.read();
            if (val.getTag() != DER.SEQUENCE) throw new SignatureException("failed to parse DigestAlgorithmIdentifier");
            boolean sequenceIsBer = val.getLength() == 0;
            val = der.read();
            if (val.getTag() != DER.OBJECT_IDENTIFIER) throw new SignatureException("failed to parse object identifier");
            if (!val.getValue().equals(digestAlgorithm)) throw new SignatureException("digest algorithms do not match");
            val = der.read();
            if (val.getTag() != DER.NULL) throw new SignatureException("cannot handle digest parameters");
            if (sequenceIsBer) der.skip(1);
            val = der.read();
            if (val.getTag() != DER.OCTET_STRING) throw new SignatureException("failed to parse Digest");
            return MessageDigest.isEqual(md.digest(), (byte[]) val.getValue());
        } catch (IOException ioe) {
            throw new SignatureException(ioe.toString());
        }
    }

    protected boolean engineVerify(byte[] sig, int off, int len) throws SignatureException {
        if (sig == null || off < 0 || len < 0 || off + len > sig.length) throw new SignatureException("illegal parameter");
        byte[] buf = new byte[len];
        System.arraycopy(sig, off, buf, 0, len);
        return engineVerify(buf);
    }
}
