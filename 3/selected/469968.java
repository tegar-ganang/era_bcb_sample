package gnu.java.security.provider;

import gnu.java.security.der.DER;
import gnu.java.security.der.DERReader;
import gnu.java.security.der.DERValue;
import gnu.java.security.der.DERWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Random;

public class DSASignature extends SignatureSpi {

    private DSAPublicKey publicKey;

    private DSAPrivateKey privateKey;

    private final MessageDigest digest;

    private final SecureRandom random;

    public DSASignature() throws NoSuchAlgorithmException {
        random = new SecureRandom();
        digest = MessageDigest.getInstance("SHA1");
    }

    private void init() {
        digest.reset();
    }

    public void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (publicKey instanceof DSAPublicKey) this.publicKey = (DSAPublicKey) publicKey; else throw new InvalidKeyException();
        init();
    }

    public void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof DSAPrivateKey) this.privateKey = (DSAPrivateKey) privateKey; else throw new InvalidKeyException("not a DSA private key");
        init();
    }

    public void engineInitSign(PrivateKey privateKey, SecureRandom random) throws InvalidKeyException {
        if (privateKey instanceof DSAPrivateKey) this.privateKey = (DSAPrivateKey) privateKey; else throw new InvalidKeyException("not a DSA private key");
        appRandom = random;
        init();
    }

    public void engineUpdate(byte b) throws SignatureException {
        digest.update(b);
    }

    public void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        digest.update(b, off, len);
    }

    public byte[] engineSign() throws SignatureException {
        if (privateKey == null) throw new SignatureException("not initialized for signing");
        try {
            BigInteger g = privateKey.getParams().getG();
            BigInteger p = privateKey.getParams().getP();
            BigInteger q = privateKey.getParams().getQ();
            BigInteger x = privateKey.getX();
            BigInteger k = new BigInteger(159, appRandom != null ? appRandom : random);
            BigInteger r = g.modPow(k, p);
            r = r.mod(q);
            byte bytes[] = digest.digest();
            BigInteger sha = new BigInteger(1, bytes);
            BigInteger s = sha.add(x.multiply(r));
            s = s.multiply(k.modInverse(q)).mod(q);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ArrayList seq = new ArrayList(2);
            seq.add(0, new DERValue(DER.INTEGER, r));
            seq.add(1, new DERValue(DER.INTEGER, s));
            DERWriter.write(bout, new DERValue(DER.CONSTRUCTED | DER.SEQUENCE, seq));
            return bout.toByteArray();
        } catch (IOException ioe) {
            SignatureException se = new SignatureException();
            se.initCause(ioe);
            throw se;
        } catch (ArithmeticException ae) {
            SignatureException se = new SignatureException();
            se.initCause(ae);
            throw se;
        }
    }

    public int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        byte tmp[] = engineSign();
        if (tmp.length > len) throw new SignatureException("output buffer too short");
        System.arraycopy(tmp, 0, outbuf, offset, tmp.length);
        return tmp.length;
    }

    public boolean engineVerify(byte[] sigBytes) throws SignatureException {
        try {
            DERReader in = new DERReader(sigBytes);
            DERValue val = in.read();
            if (!val.isConstructed()) throw new SignatureException("badly formed signature");
            BigInteger r = (BigInteger) in.read().getValue();
            BigInteger s = (BigInteger) in.read().getValue();
            BigInteger g = publicKey.getParams().getG();
            BigInteger p = publicKey.getParams().getP();
            BigInteger q = publicKey.getParams().getQ();
            BigInteger y = publicKey.getY();
            BigInteger w = s.modInverse(q);
            byte bytes[] = digest.digest();
            BigInteger sha = new BigInteger(1, bytes);
            BigInteger u1 = w.multiply(sha).mod(q);
            BigInteger u2 = r.multiply(w).mod(q);
            BigInteger v = g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);
            if (v.equals(r)) return true; else return false;
        } catch (IOException ioe) {
            SignatureException se = new SignatureException("badly formed signature");
            se.initCause(ioe);
            throw se;
        }
    }

    public void engineSetParameter(String param, Object value) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    public void engineSetParameter(AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
        throw new InvalidParameterException();
    }

    public Object engineGetParameter(String param) throws InvalidParameterException {
        throw new InvalidParameterException();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
