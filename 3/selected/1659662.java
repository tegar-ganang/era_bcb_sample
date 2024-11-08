package com.mindbright.security.keystore;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import com.mindbright.jca.security.Key;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.KeyStoreSpi;
import com.mindbright.jca.security.PublicKey;
import com.mindbright.jca.security.KeyStoreException;
import com.mindbright.jca.security.UnrecoverableKeyException;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jca.security.NoSuchAlgorithmException;
import com.mindbright.jca.security.interfaces.DSAPublicKey;
import com.mindbright.jca.security.interfaces.RSAPublicKey;
import com.mindbright.jca.security.cert.Certificate;
import com.mindbright.jca.security.cert.CertificateException;
import com.mindbright.jce.crypto.Cipher;
import com.mindbright.jce.crypto.Mac;
import com.mindbright.jce.crypto.ShortBufferException;
import com.mindbright.jce.crypto.spec.SecretKeySpec;
import com.mindbright.jce.crypto.spec.IvParameterSpec;
import com.mindbright.asn1.ASN1DER;
import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.security.x509.X509Certificate;
import com.mindbright.security.pkcs8.EncryptedPrivateKeyInfo;
import com.mindbright.bdb.DBHash;

public class NetscapeKeyStore extends KeyStoreSpi {

    public static final int TYPE_VERSION = 0;

    public static final int TYPE_CERTIFICATE = 1;

    public static final int TYPE_NICKNAME = 2;

    public static final int TYPE_SUBJECT = 3;

    public static final int TYPE_REVOCATION = 4;

    public static final int TYPE_KEYREVOCATION = 5;

    public static final int TYPE_SMIMEPROFILE = 6;

    public static final int TYPE_CONTENTVER = 7;

    public class DBEntry {

        protected byte[] data;

        public int type;

        public int version;

        public int flags;

        protected int rPos;

        protected DBEntry(byte[] data) {
            this.data = data;
            this.rPos = 0;
            this.type = readByte();
            this.version = readByte();
            this.flags = readByte();
        }

        public final int readByte() {
            return ((int) data[rPos++]) & 0xff;
        }

        public final int readShort() {
            int b1 = readByte();
            int b2 = readByte();
            return ((b1 << 8) + (b2 << 0));
        }

        public final byte[] readRaw(int len) {
            byte[] raw = new byte[len];
            readRaw(raw, 0, len);
            return raw;
        }

        public final void readRaw(byte[] raw, int off, int len) {
            System.arraycopy(data, rPos, raw, off, len);
            rPos += len;
        }
    }

    public class CertEntry extends DBEntry {

        public int sslFlags;

        public int emailFlags;

        public int oSignFlags;

        public byte[] certificate;

        public String nickName;

        public CertEntry(byte[] data) {
            super(data);
            sslFlags = readShort();
            emailFlags = readShort();
            oSignFlags = readShort();
            int certLen = readShort();
            int nickLen = readShort();
            certificate = readRaw(certLen);
            nickName = new String(readRaw(nickLen - 1));
        }
    }

    public class KeyEntry extends DBEntry {

        public byte[] salt;

        public String nickName;

        public byte[] encryptedKey;

        public KeyEntry(byte[] data) {
            super(data);
            salt = readRaw(version);
            nickName = new String(readRaw(flags - 1));
            rPos++;
            encryptedKey = readRaw(data.length - rPos);
        }
    }

    private static final String[] CERT_FILES = { "cert8.db", "cert7.db", "Certificates8", "Certificates7" };

    private static final String[] KEY_FILES = { "key3.db", "Key Database3" };

    private DBHash certdb;

    private DBHash keydb;

    private Hashtable certificates;

    public NetscapeKeyStore() {
        ASN1OIDRegistry.addModule("com.mindbright.security.pkcs12");
        ASN1OIDRegistry.register("1.2.840.113549.1.12.5.1.3", "com.mindbright.security.pkcs12.PKCS12PbeParams");
        certdb = new DBHash();
        keydb = new DBHash();
        certificates = new Hashtable();
    }

    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyEntry keyEntry = getKeyEntry(alias);
        if (!passwordCheck(password)) {
            throw new UnrecoverableKeyException("Invalid password");
        }
        if (keyEntry != null) {
            try {
                EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo();
                ASN1DER ber = new ASN1DER();
                ByteArrayInputStream ba = new ByteArrayInputStream(keyEntry.encryptedKey);
                ber.decode(ba, epki);
                byte[] enc = epki.encryptedData.getRaw();
                byte[] dec = new byte[enc.length];
                do3DESCipher(Cipher.DECRYPT_MODE, password, enc, 0, enc.length, dec, globalSalt(), keyEntry.salt);
                ba = new ByteArrayInputStream(dec);
                return PKCS12KeyStore.extractPrivateKey(dec);
            } catch (IOException e) {
                throw new UnrecoverableKeyException(e.getMessage());
            }
        }
        return null;
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        return null;
    }

    public Certificate engineGetCertificate(String alias) {
        CertEntry cert = (CertEntry) certificates.get(alias);
        if (cert != null) {
            return new X509Certificate(cert.certificate);
        }
        return null;
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
        return certificates.keys();
    }

    public boolean engineContainsAlias(String alias) {
        return (certificates.get(alias) != null);
    }

    public int engineSize() {
        return certificates.size();
    }

    public boolean engineIsKeyEntry(String alias) {
        return (getKeyEntry(alias) != null);
    }

    public boolean engineIsCertificateEntry(String alias) {
        return !engineIsKeyEntry(alias) && (certificates.get(alias) != null);
    }

    public String engineGetCertificateAlias(Certificate cert) {
        return null;
    }

    public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
    }

    public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        if (!(stream instanceof ByteArrayInputStream)) {
            throw new IOException("Parameter 'stream' must be a ByteArrayInputStream");
        }
        byte[] dirB = new byte[stream.available()];
        stream.read(dirB);
        String dirName = new String(dirB);
        String certFile = null;
        String keyFile = null;
        int i;
        for (i = 0; i < CERT_FILES.length; i++) {
            if ((new File(dirName + File.separator + CERT_FILES[i])).exists()) {
                certFile = CERT_FILES[i];
                break;
            }
        }
        for (i = 0; i < KEY_FILES.length; i++) {
            if ((new File(dirName + File.separator + KEY_FILES[i])).exists()) {
                keyFile = KEY_FILES[i];
                break;
            }
        }
        certdb.loadAll(dirName + File.separator + certFile);
        keydb.loadAll(dirName + File.separator + keyFile);
        Enumeration keys = certdb.keys();
        while (keys.hasMoreElements()) {
            DBHash.DBT dbt = (DBHash.DBT) keys.nextElement();
            if (dbt.key[0] == 0x01) {
                CertEntry cert = new CertEntry(dbt.data);
                certificates.put(cert.nickName, cert);
            }
        }
        if (!passwordCheck(password)) {
            throw new IOException("Invalid password");
        }
    }

    private KeyEntry getKeyEntry(String alias) {
        Certificate cert = engineGetCertificate(alias);
        KeyEntry keyEntry = null;
        if (cert != null) {
            PublicKey pk = cert.getPublicKey();
            byte[] keyKey = null;
            if (pk instanceof RSAPublicKey) {
                RSAPublicKey rpk = (RSAPublicKey) pk;
                keyKey = rpk.getModulus().toByteArray();
            } else if (pk instanceof DSAPublicKey) {
                DSAPublicKey dpk = (DSAPublicKey) pk;
                keyKey = dpk.getY().toByteArray();
            }
            byte[] keyData = keydb.get(keyKey);
            if (keyData == null && keyKey != null && keyKey[0] == 0x00) {
                byte[] b = new byte[keyKey.length - 1];
                System.arraycopy(keyKey, 1, b, 0, b.length);
                keyData = keydb.get(b);
            }
            if (keyData != null) {
                keyEntry = new KeyEntry(keyData);
            }
        }
        return keyEntry;
    }

    private static byte[] deriveKey(char[] password, byte[] globalSalt, byte[] entrySalt) throws InvalidKeyException, NoSuchAlgorithmException, ShortBufferException {
        Mac hmac = Mac.getInstance("HmacSHA1");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[] key = new byte[40];
        byte[] pwd = new byte[password.length];
        for (int i = 0; i < pwd.length; i++) {
            pwd[i] = (byte) password[i];
        }
        sha1.update(globalSalt);
        sha1.update(pwd);
        byte[] hp = sha1.digest();
        byte[] pes = new byte[20];
        System.arraycopy(entrySalt, 0, pes, 0, entrySalt.length);
        sha1.update(hp);
        sha1.update(entrySalt);
        byte[] chp = sha1.digest();
        hmac.init(new SecretKeySpec(chp, hmac.getAlgorithm()));
        hmac.update(pes);
        hmac.update(entrySalt);
        hmac.doFinal(key, 0);
        hmac.update(pes);
        byte[] tk = hmac.doFinal();
        hmac.update(tk);
        hmac.update(entrySalt);
        hmac.doFinal(key, 20);
        return key;
    }

    private static void do3DESCipher(int mode, char[] password, byte[] input, int off, int len, byte[] output, byte[] globalSalt, byte[] entrySalt) throws NoSuchAlgorithmException {
        try {
            Cipher cipher = Cipher.getInstance("3DES/CBC/PKCS5Padding");
            byte[] keymaterial = deriveKey(password, globalSalt, entrySalt);
            byte[] key = new byte[24];
            byte[] iv = new byte[8];
            System.arraycopy(keymaterial, 0, key, 0, 24);
            System.arraycopy(keymaterial, 32, iv, 0, 8);
            cipher.init(mode, new SecretKeySpec(key, cipher.getAlgorithm()), new IvParameterSpec(iv));
            cipher.doFinal(input, off, len, output, 0);
        } catch (Exception e) {
            throw new Error("Error in NetscapeKeyStore.do3DESCipher: " + e);
        }
    }

    private byte[] globalSalt() {
        return keydb.get("global-salt");
    }

    private boolean passwordCheck(char[] password) throws NoSuchAlgorithmException {
        if (password == null) return true;
        byte[] keyData = keydb.get("password-check");
        if (keyData == null) return true;
        KeyEntry pwdCheck = new KeyEntry(keyData);
        if (pwdCheck == null) {
            return true;
        }
        int off = pwdCheck.encryptedKey.length - 16;
        byte[] dec = new byte[16];
        do3DESCipher(Cipher.DECRYPT_MODE, password, pwdCheck.encryptedKey, off, 16, dec, globalSalt(), pwdCheck.salt);
        return "password-check".equals(new String(dec, 0, 14));
    }
}
