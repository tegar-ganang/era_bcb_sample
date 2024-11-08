package com.mindbright.security.mac;

import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.DigestException;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.InvalidAlgorithmParameterException;
import com.mindbright.jca.security.spec.AlgorithmParameterSpec;
import com.mindbright.jce.crypto.spec.SecretKeySpec;
import com.mindbright.jce.crypto.MacSpi;

/**
 * Class implementing message authentication through keyed hashing
 * (referred to as HMAC) described in rfc2104
 */
public class HMAC extends MacSpi {

    protected int macLength;

    protected int hashLength;

    protected int blockSize;

    protected byte[] k_ipad;

    protected byte[] k_opad;

    protected byte[] innerHash;

    protected byte[] outerHash;

    protected MessageDigest inner;

    protected MessageDigest outer;

    protected MessageDigest innerClone;

    protected MessageDigest outerClone;

    protected HMAC(String hashAlgorithm) {
        try {
            inner = MessageDigest.getInstance(hashAlgorithm);
            outer = MessageDigest.getInstance(hashAlgorithm);
        } catch (Exception e) {
            throw new Error("Error in HMAC: " + e);
        }
        macLength = inner.getDigestLength();
        blockSize = 64;
        hashLength = macLength;
        innerHash = new byte[hashLength];
        outerHash = new byte[hashLength];
        k_ipad = new byte[blockSize];
        k_opad = new byte[blockSize];
    }

    private final void init(byte[] key) {
        inner.reset();
        outer.reset();
        int keyLen = key.length;
        if (keyLen > blockSize) {
            inner.update(key);
            key = inner.digest();
            inner.reset();
            keyLen = blockSize;
        }
        System.arraycopy(key, 0, k_ipad, 0, keyLen);
        System.arraycopy(key, 0, k_opad, 0, keyLen);
        for (int i = 0; i < blockSize; i++) {
            k_ipad[i] ^= (byte) 0x36;
            k_opad[i] ^= (byte) 0x5c;
        }
        updatePads();
        if (inner instanceof Cloneable) {
            try {
                innerClone = (MessageDigest) inner.clone();
                outerClone = (MessageDigest) outer.clone();
            } catch (CloneNotSupportedException e) {
                innerClone = null;
                outerClone = null;
            }
        }
    }

    protected final byte[] engineDoFinal() {
        byte[] hmac;
        try {
            inner.digest(innerHash, 0, hashLength);
            outer.update(innerHash, 0, hashLength);
            outer.digest(outerHash, 0, hashLength);
        } catch (DigestException e) {
            throw new Error("Error/bug in HMAC, buffer too short");
        }
        hmac = new byte[macLength];
        System.arraycopy(outerHash, 0, hmac, 0, macLength);
        updatePads();
        return hmac;
    }

    protected final int engineGetMacLength() {
        return macLength;
    }

    protected final void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(((SecretKeySpec) key).getEncoded());
    }

    protected final void engineReset() {
        updatePads();
    }

    protected final void engineUpdate(byte input) {
        engineUpdate(new byte[] { input }, 0, 1);
    }

    protected final void engineUpdate(byte[] input, int offset, int len) {
        inner.update(input, offset, len);
    }

    private final void updatePads() {
        if (innerClone != null) {
            try {
                inner = (MessageDigest) innerClone.clone();
                outer = (MessageDigest) outerClone.clone();
            } catch (CloneNotSupportedException e) {
                innerClone = null;
                outerClone = null;
                updatePads();
            }
        } else {
            inner.reset();
            outer.reset();
            inner.update(k_ipad);
            outer.update(k_opad);
        }
    }
}
