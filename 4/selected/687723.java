package com.billdimmick.merkabah;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author Bill Dimmick <me@billdimmick.com>
 */
public abstract class AWSSecurityAdapter {

    private static final Log log = LogFactory.getLog(AWSSecurityAdapter.class);

    private String signingAlgorithm = "SHA1withRSA";

    /**
	 * Gets the AWS Access Identifier for use in providing identity to SQS.
	 * @return
	 */
    public abstract String getAWSAccessId();

    /**
	 * Gets the AWS Secret Key for use in authenticating requests to SQS.
	 * @return
	 */
    protected abstract String getAWSSecretKey();

    protected abstract Certificate getCertificate(String name);

    protected abstract PublicKey getPublicKey(String name);

    protected abstract PrivateKey getPrivateKey(String name);

    public String getSigningAlgorithm() {
        return this.signingAlgorithm;
    }

    public void setSigningAlgorithm(final String algorithm) {
        this.signingAlgorithm = algorithm;
    }

    protected String sign(final String data, final String key, final String algorithm) {
        Validate.notNull(data, "Provided data cannot be null.");
        Validate.notNull(key, "Provided key name cannot be null.");
        Validate.notNull(getSigningAlgorithm(), "Signing algorithm is null - cannot proceed.");
        final PrivateKey pk = getPrivateKey(key);
        Validate.notNull(pk, String.format("Provided key(%s) does not exist.", key));
        try {
            final Signature signature = Signature.getInstance(getSigningAlgorithm());
            signature.initSign(pk);
            signature.update(data.getBytes());
            final byte[] result = Base64.encodeBase64(signature.sign());
            return new String(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Cannot find instance of algorithm '%s'", getSigningAlgorithm()), e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Provided key of algorithm '%s' doesn't work with signing algorithm '%s'", pk.getAlgorithm(), getSigningAlgorithm()), e);
        } catch (SignatureException e) {
            throw new IllegalStateException(String.format("Error generating signature", e));
        }
    }

    protected boolean verify(final String data, final String key, final String algorithm) throws CryptographicFailureException {
        Validate.notNull(data, "Provided data cannot be null.");
        Validate.notNull(key, "Provided key name cannot be null.");
        final PublicKey pk = getPublicKey(key);
        if (pk == null) {
            throw new CryptographicFailureException("VerifyFailed", "Validation of this message signature failed");
        }
        Certificate cert = null;
        if (pk == null) {
            cert = getCertificate(key);
        }
        try {
            final Signature signature = Signature.getInstance(algorithm);
            if (pk != null) {
                signature.initVerify(pk);
            } else {
                signature.initVerify(cert);
            }
            return signature.verify(Base64.decodeBase64(data.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Cannot find instance of algorithm '%s'", algorithm), e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Provided key of algorithm '%s' doesn't work with signing algorithm '%s'", pk.getAlgorithm(), algorithm), e);
        } catch (SignatureException e) {
            throw new IllegalStateException(String.format("Error validating signature", e));
        }
    }

    protected String encrypt(final String data, final String key) throws CryptographicFailureException {
        Validate.notNull(data, "Provided data cannot be null.");
        Validate.notNull(key, "Provided key name cannot be null.");
        final PublicKey pk = getPublicKey(key);
        if (pk == null) {
            throw new CryptographicFailureException("PublicKeyNotFound", String.format("Cannot find public key '%s'", key));
        }
        try {
            final Cipher cipher = Cipher.getInstance(pk.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            final ByteArrayInputStream bin = new ByteArrayInputStream(data.getBytes());
            final CipherInputStream cin = new CipherInputStream(bin, cipher);
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy(cin, bout);
            return new String(Base64.encodeBase64(bout.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Cannot find instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException(String.format("Cannot build instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Cannot build instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot build in-memory cipher copy", e);
        }
    }

    protected String decrypt(final String data, final String key) throws CryptographicFailureException {
        Validate.notNull(data, "Provided data cannot be null.");
        Validate.notNull(key, "Provided key name cannot be null.");
        final PrivateKey pk = getPrivateKey(key);
        if (pk == null) {
            throw new CryptographicFailureException("PrivateKeyNotFound", String.format("Cannot find private key '%s'", key));
        }
        try {
            final Cipher cipher = Cipher.getInstance(pk.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, pk);
            final ByteArrayInputStream bin = new ByteArrayInputStream(Base64.decodeBase64(data.getBytes()));
            final CipherInputStream cin = new CipherInputStream(bin, cipher);
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            IOUtils.copy(cin, bout);
            return new String(bout.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Cannot find instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException(String.format("Cannot build instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(String.format("Cannot build instance of algorithm '%s'", pk.getAlgorithm()), e);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot build in-memory cipher copy", e);
        }
    }

    protected String hmac(final String value, final String method) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac hmac = Mac.getInstance(method);
        final SecretKey key = new SecretKeySpec(getAWSSecretKey().getBytes(), "RAW");
        hmac.init(key);
        final byte[] signature = hmac.doFinal(value.getBytes());
        return new String(Base64.encodeBase64(signature));
    }
}
