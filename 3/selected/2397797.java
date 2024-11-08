package com.mindbright.security.keystore;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.math.BigInteger;
import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.KeyStoreSpi;
import com.mindbright.jca.security.PrivateKey;
import com.mindbright.jca.security.KeyFactory;
import com.mindbright.jca.security.KeyStoreException;
import com.mindbright.jca.security.UnrecoverableKeyException;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.cert.Certificate;
import com.mindbright.jca.security.cert.CertificateException;
import com.mindbright.jca.security.spec.DSAPrivateKeySpec;
import com.mindbright.jca.security.spec.RSAPrivateCrtKeySpec;
import com.mindbright.jce.crypto.Cipher;
import com.mindbright.jce.crypto.Mac;
import com.mindbright.jce.crypto.spec.SecretKeySpec;
import com.mindbright.jce.crypto.spec.IvParameterSpec;
import com.mindbright.asn1.ASN1DER;
import com.mindbright.asn1.ASN1Object;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1CharString;
import com.mindbright.security.x509.Attribute;
import com.mindbright.security.x509.X509Certificate;
import com.mindbright.security.pkcs7.ContentInfo;
import com.mindbright.security.pkcs7.EncryptedData;
import com.mindbright.security.pkcs8.PrivateKeyInfo;
import com.mindbright.security.pkcs8.EncryptedPrivateKeyInfo;
import com.mindbright.security.pkcs12.PFX;
import com.mindbright.security.pkcs12.AuthenticatedSafe;
import com.mindbright.security.pkcs12.SafeContents;
import com.mindbright.security.pkcs12.SafeBag;
import com.mindbright.security.pkcs12.PKCS12PbeParams;
import com.mindbright.security.pkcs12.CertBag;
import com.mindbright.util.HexDump;

public class PKCS12KeyStore extends KeyStoreSpi {

    private Hashtable privateKeys;

    private Hashtable certificates;

    private Hashtable name2id;

    private Vector aliases;

    public PKCS12KeyStore() {
        ASN1OIDRegistry.addModule("com.mindbright.security.pkcs12");
        ASN1OIDRegistry.register("1.3.6.1.4.1.311.17.1", "ASN1BMPString");
        privateKeys = new Hashtable();
        certificates = new Hashtable();
        aliases = new Vector();
        name2id = new Hashtable();
    }

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        String localKeyId = (String) name2id.get(alias);
        if (localKeyId == null) {
            return null;
        }
        EncryptedPrivateKeyInfo epki = (EncryptedPrivateKeyInfo) privateKeys.get(localKeyId);
        return extractPrivateKey(epki, password);
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        return null;
    }

    public Certificate engineGetCertificate(String alias) {
        if (alias == null) return null;
        X509Certificate x509Cert = null;
        byte[] derCert = null;
        String localKeyId = (String) name2id.get(alias);
        if (localKeyId == null) {
            localKeyId = alias;
        }
        derCert = (byte[]) certificates.get(localKeyId);
        if (derCert != null) {
            x509Cert = new X509Certificate(derCert);
        }
        return x509Cert;
    }

    public Date engineGetCreationDate(String alias) {
        return null;
    }

    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
    }

    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
    }

    public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
    }

    public void engineDeleteEntry(String alias) throws KeyStoreException {
    }

    public Enumeration engineAliases() {
        return aliases.elements();
    }

    public boolean engineContainsAlias(String alias) {
        return aliases.contains(alias);
    }

    public int engineSize() {
        return aliases.size();
    }

    public boolean engineIsKeyEntry(String alias) {
        return (name2id.get(alias) != null);
    }

    public boolean engineIsCertificateEntry(String alias) {
        return (!engineIsKeyEntry(alias) && certificates.get(alias) != null);
    }

    public String engineGetCertificateAlias(Certificate cert) {
        return null;
    }

    public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
    }

    public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        try {
            ASN1DER ber = new ASN1DER();
            PFX pfx = new PFX();
            ber.decode(stream, pfx);
            checkMac(pfx, password);
            AuthenticatedSafe authSafe = new AuthenticatedSafe();
            ASN1OctetString data = pfx.getDataContent();
            ByteArrayInputStream ba = new ByteArrayInputStream(data.getRaw());
            ber.decode(ba, authSafe);
            for (int i = 0; i < authSafe.getCount(); i++) {
                ContentInfo ci = authSafe.getContentInfo(i);
                String cit = ci.contentType.getString();
                if (cit.equals("1.2.840.113549.1.7.1")) {
                    data = (ASN1OctetString) ci.content.getValue();
                    processSafeContents(data.getRaw());
                } else if (cit.equals("1.2.840.113549.1.7.6")) {
                    EncryptedData ed = (EncryptedData) ci.content.getValue();
                    String alg = ed.encryptedContentInfo.contentEncryptionAlgorithm.algorithmName();
                    byte[] enc = ed.encryptedContentInfo.encryptedContent.getRaw();
                    PKCS12PbeParams params = (PKCS12PbeParams) ed.encryptedContentInfo.contentEncryptionAlgorithm.parameters.getValue();
                    byte[] salt = params.salt.getRaw();
                    int iterations = params.iterations.getValue().intValue();
                    byte[] dec = new byte[enc.length];
                    doCipher(Cipher.DECRYPT_MODE, password, enc, enc.length, dec, salt, iterations, alg);
                    processSafeContents(dec);
                } else {
                    throw new IOException("ContentInfo type not supported: " + cit);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void checkMac(PFX pfx, char[] password) throws CertificateException {
        if (!pfx.macData.isSet()) {
            return;
        }
        try {
            String digAlg = pfx.macData.mac.digestAlgorithm.algorithm.getString();
            Mac mac = Mac.getInstance(digAlg);
            byte[] data = pfx.getDataContent().getRaw();
            byte[] storedDig = pfx.macData.mac.digest.getRaw();
            byte[] salt = pfx.macData.macSalt.getRaw();
            int iterations = pfx.macData.getIterations();
            byte[] key = deriveKey(password, 20, salt, iterations, 3, digAlg);
            mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
            mac.update(data);
            byte[] dataDig = mac.doFinal();
            for (int i = 0; i < storedDig.length; i++) {
                if (dataDig[i] != storedDig[i]) {
                    throw new CertificateException("MAC check failed");
                }
            }
        } catch (Exception e) {
            throw new CertificateException("Error when verifying MAC: " + e.getMessage());
        }
    }

    private void processSafeContents(byte[] scBer) throws IOException, NoSuchAlgorithmException {
        ByteArrayInputStream ba = new ByteArrayInputStream(scBer);
        SafeContents sc = new SafeContents();
        ASN1DER ber = new ASN1DER();
        ber.decode(ba, sc);
        for (int j = 0; j < sc.getCount(); j++) {
            SafeBag safeBag = sc.getSafeBag(j);
            String friendlyName = getAttribute(safeBag, "1.2.840.113549.1.9.20");
            String localKeyId = getAttribute(safeBag, "1.2.840.113549.1.9.21");
            if (friendlyName != null) {
                if (localKeyId != null) {
                    name2id.put(friendlyName, localKeyId);
                }
                if (!aliases.contains(friendlyName)) {
                    aliases.addElement(friendlyName);
                }
            } else if (localKeyId != null) {
                name2id.put(localKeyId, localKeyId);
                if (!aliases.contains(localKeyId)) {
                    aliases.addElement(localKeyId);
                }
            }
            switch(safeBag.getBagType()) {
                case SafeBag.TYPE_PKCS8_SHROUDED_KEYBAG:
                    EncryptedPrivateKeyInfo keyBag = (EncryptedPrivateKeyInfo) safeBag.bagValue.getValue();
                    privateKeys.put(localKeyId, keyBag);
                    break;
                case SafeBag.TYPE_CERTBAG:
                    CertBag cb = (CertBag) safeBag.bagValue.getValue();
                    byte[] derCert = ((ASN1OctetString) cb.certValue.getValue()).getRaw();
                    if (localKeyId == null) {
                        localKeyId = friendlyName;
                    }
                    if (localKeyId != null) certificates.put(localKeyId, derCert);
                    break;
                default:
                    throw new IOException("SafeBag type not supported: " + safeBag.bagId.getString());
            }
        }
    }

    private static PrivateKey extractPrivateKey(EncryptedPrivateKeyInfo keyBag, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        ASN1DER ber = new ASN1DER();
        PKCS12PbeParams params = (PKCS12PbeParams) keyBag.encryptionAlgorithm.parameters.getValue();
        String alg = keyBag.encryptionAlgorithm.algorithmName();
        byte[] enc = keyBag.encryptedData.getRaw();
        byte[] salt = params.salt.getRaw();
        int iterations = params.iterations.getValue().intValue();
        byte[] dec = new byte[enc.length];
        doCipher(Cipher.DECRYPT_MODE, password, enc, enc.length, dec, salt, iterations, alg);
        return extractPrivateKey(dec);
    }

    public static class DSAyx extends ASN1Sequence {

        public ASN1Integer x;

        public ASN1Integer y;

        public DSAyx() {
            y = new ASN1Integer();
            x = new ASN1Integer();
            addComponent(y);
            addComponent(x);
        }
    }

    public static PrivateKey extractPrivateKey(byte[] berPrivateKeyInfo) throws UnrecoverableKeyException {
        ASN1DER ber = new ASN1DER();
        ByteArrayInputStream ba = new ByteArrayInputStream(berPrivateKeyInfo);
        PrivateKeyInfo pki = new PrivateKeyInfo();
        try {
            ber.decode(ba, pki);
            boolean isrsakey = true;
            try {
                String alg = pki.privateKeyAlgorithm.algorithmName().toLowerCase();
                if (alg.indexOf("dsa") >= 0) isrsakey = false;
            } catch (Throwable t) {
            }
            ba = new ByteArrayInputStream(pki.privateKey.getRaw());
            if (isrsakey) {
                com.mindbright.security.pkcs1.RSAPrivateKey rsa = new com.mindbright.security.pkcs1.RSAPrivateKey();
                ber.decode(ba, rsa);
                BigInteger n, e, d, p, q, pe, qe, u;
                n = rsa.modulus.getValue();
                e = rsa.publicExponent.getValue();
                d = rsa.privateExponent.getValue();
                p = rsa.prime1.getValue();
                q = rsa.prime2.getValue();
                pe = rsa.exponent1.getValue();
                qe = rsa.exponent2.getValue();
                u = rsa.coefficient.getValue();
                RSAPrivateCrtKeySpec prvSpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, pe, qe, u);
                KeyFactory keyFact = KeyFactory.getInstance("RSA");
                return keyFact.generatePrivate(prvSpec);
            }
            BigInteger x = null;
            try {
                ASN1Integer dsax = new ASN1Integer();
                ber.decode(ba, dsax);
                x = dsax.getValue();
            } catch (Throwable t) {
            }
            if (x == null) {
                DSAyx dsayx = new DSAyx();
                ber.decode(new ByteArrayInputStream(pki.privateKey.getRaw()), dsayx);
                x = dsayx.x.getValue();
            }
            com.mindbright.security.pkcs1.DSAParams params = (com.mindbright.security.pkcs1.DSAParams) pki.privateKeyAlgorithm.parameters.getValue();
            DSAPrivateKeySpec prvSpec = new DSAPrivateKeySpec(x, params.p.getValue(), params.q.getValue(), params.g.getValue());
            KeyFactory keyFact = KeyFactory.getInstance("DSA");
            return keyFact.generatePrivate(prvSpec);
        } catch (Exception e) {
            throw new UnrecoverableKeyException(e.getMessage());
        }
    }

    private String getAttribute(SafeBag safeBag, String attrType) {
        int cnt = safeBag.bagAttributes.getCount();
        String value = null;
        for (int i = 0; i < cnt; i++) {
            Attribute a = (Attribute) safeBag.bagAttributes.getComponent(i);
            if (attrType.equals(a.type.getString())) {
                ASN1Object v = a.values.getComponent(0);
                if (v instanceof ASN1CharString) {
                    value = ((ASN1CharString) v).getValue();
                } else if (v instanceof ASN1OctetString) {
                    value = HexDump.toString(((ASN1OctetString) v).getRaw());
                } else {
                    value = v.toString();
                }
            }
        }
        return value;
    }

    private static byte[] deriveKey(char[] password, int keyLen, byte[] salt, int iterations, int id, String digAlg) {
        try {
            MessageDigest digest = MessageDigest.getInstance(digAlg);
            int u = digest.getDigestLength();
            byte[] Ai = new byte[u];
            byte[] key = new byte[keyLen];
            int v = 64;
            byte[] pb = new byte[password.length * 2 + 2];
            int sl = ((salt.length + v - 1) / v) * v;
            int pl = ((pb.length + v - 1) / v) * v;
            byte[] D = new byte[v];
            byte[] I = new byte[sl + pl];
            byte[] B = new byte[v];
            int i;
            for (i = 0; i < password.length; i++) {
                pb[i * 2] = (byte) (password[i] >>> 8);
                pb[(i * 2) + 1] = (byte) (password[i] & 0xff);
            }
            for (i = 0; i < v; i++) {
                D[i] = (byte) id;
            }
            for (i = 0; i < sl; i++) {
                I[i] = salt[i % salt.length];
            }
            for (i = 0; i < pl; i++) {
                I[sl + i] = pb[i % pb.length];
            }
            int cnt = 0;
            BigInteger one = BigInteger.valueOf(1L);
            byte[] ijRaw = new byte[v];
            while (true) {
                digest.update(D);
                digest.update(I);
                digest.digest(Ai, 0, u);
                for (i = 1; i < iterations; i++) {
                    digest.update(Ai);
                    digest.digest(Ai, 0, u);
                }
                int n = ((u > (keyLen - cnt)) ? keyLen - cnt : u);
                System.arraycopy(Ai, 0, key, cnt, n);
                cnt += n;
                if (cnt >= keyLen) {
                    break;
                }
                for (i = 0; i < v; i++) {
                    B[i] = Ai[i % u];
                }
                BigInteger Bplus1 = (new BigInteger(1, B)).add(one);
                for (i = 0; i < I.length; i += v) {
                    System.arraycopy(I, i, ijRaw, 0, v);
                    BigInteger Ij = new BigInteger(1, ijRaw);
                    Ij = Ij.add(Bplus1);
                    ijRaw = unsignedBigIntToBytes(Ij, v);
                    System.arraycopy(ijRaw, 0, I, i, v);
                }
            }
            return key;
        } catch (Exception e) {
            throw new Error("Error in PKCS12.deriveKey: " + e);
        }
    }

    private static byte[] unsignedBigIntToBytes(BigInteger bi, int size) {
        byte[] tmp = bi.toByteArray();
        byte[] tmp2 = null;
        if (tmp.length > size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, tmp.length - size, tmp2, 0, size);
        } else if (tmp.length < size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, 0, tmp2, size - tmp.length, tmp.length);
        } else {
            tmp2 = tmp;
        }
        return tmp2;
    }

    private static byte[] deriveKeyPKCS5(char[] password, byte[] salt, int iterations, int dklen, String digAlg) {
        try {
            MessageDigest digest = MessageDigest.getInstance(digAlg);
            byte[] pass = new byte[password.length];
            for (int i = 0; i < password.length; i++) pass[i] = (byte) (password[i] & 0xff);
            digest.update(pass);
            digest.update(salt);
            byte[] dig = digest.digest();
            int len = dig.length;
            for (int i = 1; i < iterations; i++) {
                digest.update(dig);
                digest.digest(dig, 0, len);
            }
            byte[] ret = new byte[dklen];
            System.arraycopy(dig, 0, ret, 0, dklen);
            return ret;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private static void doCipher(int mode, char[] password, byte[] input, int len, byte[] output, byte[] salt, int iterations, String cipherType) throws NoSuchAlgorithmException {
        try {
            int keyLen = 40;
            boolean pkcs5 = false;
            String hashType = "SHA";
            if (cipherType.equals("pbeWithSHAAnd3-KeyTripleDES-CBC")) {
                keyLen = 192;
                cipherType = "3DES/CBC/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHAAnd2-KeyTripleDES-CBC")) {
                keyLen = 128;
                cipherType = "3DES/CBC/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHAAnd40BitRC2-CBC")) {
                cipherType = "RC2/CBC/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHAAnd128BitRC2-CBC")) {
                keyLen = 128;
                cipherType = "RC2/CBC/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHAAnd40BitRC4")) {
                cipherType = "RC4/OFB/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHAAnd128BitRC4")) {
                keyLen = 128;
                cipherType = "RC4/OFB/PKCS5Padding";
            } else if (cipherType.equals("pbeWithSHA1AndRC2-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "RC2/CBC/PKCS5Padding";
                hashType = "SHA1";
            } else if (cipherType.equals("pbeWithMD2AndRC2-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "RC2/CBC/PKCS5Padding";
                hashType = "MD2";
            } else if (cipherType.equals("pbeWithMD5AndRC2-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "RC2/CBC/PKCS5Padding";
                hashType = "MD5";
            } else if (cipherType.equals("pbeWithMD2AndDES-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "DES/CBC/PKCS5Padding";
                hashType = "MD2";
            } else if (cipherType.equals("pbeWithMD5AndDES-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "DES/CBC/PKCS5Padding";
                hashType = "MD5";
            } else if (cipherType.equals("pbeWithSHA1AndDES-CBC")) {
                pkcs5 = true;
                keyLen = 64;
                cipherType = "DES/CBC/PKCS5Padding";
                hashType = "SHA1";
            }
            keyLen /= 8;
            Cipher cipher = Cipher.getInstance(cipherType);
            cipherType = cipher.getAlgorithm();
            byte[] key;
            byte[] iv;
            if (pkcs5) {
                byte[] dk = deriveKeyPKCS5(password, salt, iterations, 16, hashType);
                key = new byte[8];
                System.arraycopy(dk, 0, key, 0, 8);
                iv = new byte[8];
                System.arraycopy(dk, 8, iv, 0, 8);
            } else {
                key = deriveKey(password, keyLen, salt, iterations, 1, hashType);
                iv = deriveKey(password, 8, salt, iterations, 2, hashType);
            }
            cipher.init(mode, new SecretKeySpec(key, cipherType), new IvParameterSpec(iv));
            cipher.doFinal(input, 0, len, output, 0);
        } catch (InvalidKeyException e) {
            throw new Error("Invalid key derived in " + "PKCS12KeyStore.doCipher: " + e);
        }
    }
}
