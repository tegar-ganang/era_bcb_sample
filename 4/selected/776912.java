package com.hanhuy.scurp.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import com.hanhuy.common.EnumUtil;
import com.hanhuy.scurp.server.CertificateGenerator;
import com.hanhuy.scurp.server.PasswordServer;

/**
 *
 * @author pfnguyen
 */
public class DatabaseFile {

    private static final Logger log = Logger.getLogger(DatabaseFile.class.getName());

    private static final int DATABASE_VERSION = 2;

    public static enum Fields {

        FOLDER_ENTRY, PASSWORD_ENTRY, FOLDER_ID, ENTRY_NAME, LOGIN_URL, USERNAME, ENCRYPTED_PASSWORD, COMMENTS, ALLOWED_URL, AUTOTYPE_SEQUENCE
    }

    public static final String[] JCE_PBE_ALGORITHMS = new String[] { "PBEWithSHA1AndDESede", "PBEWithSHA1AndRC2_40", "PBEWithMD5AndTripleDES" };

    public static final String[] BC_PBE_ALGORITHMS = new String[] { "PBEWithSHA1AndRC2", "PBEWithSHA256And128BitAES-CBC-BC", "PBEWithSHA256And192BitAES-CBC-BC", "PBEWithSHA256And256BitAES-CBC-BC", "PBEWithSHAAnd128BitAES-CBC-BC", "PBEWithSHAAnd128BitRC2-CBC", "PBEWithSHAAnd192BitAES-CBC-BC", "PBEWithSHAAnd2-KeyTripleDES-CBC", "PBEWithSHAAnd256BitAES-CBC-BC", "PBEWithSHAAnd3-KeyTripleDES-CBC", "PBEWithSHAAnd40BitRC2-CBC", "PBEWithSHAAndIDEA-CBC", "PBEWithSHAAndTwofish-CBC" };

    public static final String[] JCE_SYMMETRIC_ALGORITHMS = new String[] { "AES", "Blowfish" };

    public static final String[] BC_SYMMETRIC_ALGORITHMS = new String[] { "Twofish", "Serpent", "Camellia", "CAST6" };

    private int version;

    private File file;

    private final Charset charset = Charset.forName("UTF-8");

    private final ByteBuffer tmp = ByteBuffer.allocate(16384);

    private PasswordServer passwordServer;

    private static final String SYMMETRIC_ALGORITHM = "AES";

    private static final String PBE_ALGORITHM = "PBEWithSHA1AndRC2_40";

    private static final int PBE_ITERATION_COUNT = 128;

    private static final char[] VERIFICATION_STRING = "SCURP".toCharArray();

    private static final int VERIFICATION_OFFSET = 128;

    private static final int DATA_OFFSET = 256;

    private static final int DATALEN_OFFSET = 250;

    private static final int SERIAL_OFFSET = 184;

    private static final int IV_OFFSET = 192;

    private long serialNumber = 0;

    private final String path;

    private Key symKey;

    private LinkedHashMap<X509Certificate, PrivateKey> keyPairs = new LinkedHashMap<X509Certificate, PrivateKey>();

    private final List<PasswordEntry> entries = new ArrayList<PasswordEntry>();

    private final List<Folder> folders = new ArrayList<Folder>();

    public DatabaseFile(String path) {
        file = new File(path);
        this.path = path;
    }

    public void changePassword(char[] password) throws IOException {
        FileChannel c = null;
        if (!file.exists()) throw new FileNotFoundException(path);
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        c = f.getChannel();
        try {
            ByteBuffer buf = ByteBuffer.allocate(128);
            c.position(0);
            Key key = generatePBEKey(password);
            Cipher pbeCipher = getPBECipher(key);
            AlgorithmParameters params = pbeCipher.getParameters();
            byte[] paramBuf = params.getEncoded();
            byte[] encKey = pbeCipher.doFinal(getKey().getEncoded());
            c.position(0);
            buf.clear();
            buf.put((byte) paramBuf.length);
            buf.put(paramBuf);
            buf.put((byte) encKey.length);
            buf.put(encKey);
            buf.flip();
            c.write(buf);
        } catch (GeneralSecurityException e) {
            log.log(Level.WARNING, "Unable to change password", e);
            IOException ioe = new IOException("Unable to change password: " + e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } finally {
            c.close();
        }
    }

    public void reload() throws IOException {
        FileChannel c = null;
        try {
            c = new RandomAccessFile(file, "r").getChannel();
            ByteBuffer b = ByteBuffer.allocate(8);
            c.position(SERIAL_OFFSET);
            c.read(b);
            b.flip();
            serialNumber = b.getLong();
            loadData(c);
            saveLastKnownGood(c);
        } finally {
            if (c != null) c.close();
        }
    }

    public void open(char[] password) throws IOException {
        FileChannel c = null;
        try {
            if (!file.exists()) throw new FileNotFoundException(path);
            c = new RandomAccessFile(file, "r").getChannel();
            ByteBuffer b = ByteBuffer.allocate(256);
            c.read(b);
            b.flip();
            byte length;
            length = b.get();
            byte[] paramBuf = new byte[length];
            b.get(paramBuf);
            length = b.get();
            byte[] encKey = new byte[length];
            b.get(encKey);
            Key key = generatePBEKey(password);
            Cipher pbeCipher = getPBECipher(key, paramBuf);
            byte[] keyBuf = pbeCipher.doFinal(encKey);
            loadSymmetricKey(keyBuf);
            b.position(VERIFICATION_OFFSET);
            length = b.get();
            byte[] verification = new byte[length];
            b.get(verification);
            version = b.getInt();
            if (DATABASE_VERSION < version) {
                throw new IOException("Database version too new: " + version);
            }
            log.info("Loading database version: " + version);
            b.position(SERIAL_OFFSET);
            serialNumber = b.getLong();
            log.info("DB serial number: " + serialNumber);
            char[] verificationText = decryptText(verification);
            if (!Arrays.equals(VERIFICATION_STRING, verificationText)) {
                throw new IOException("Unable to decrypt database: verification failed: " + new String(verificationText));
            }
            loadData(c);
            saveLastKnownGood(c);
        } catch (GeneralSecurityException e) {
            log.log(Level.WARNING, "Unable to decrypt password", e);
            IOException ioe = new IOException("Unable to decrypt database");
            ioe.initCause(e);
            log.log(Level.WARNING, "Unable to decrypt database", e);
            throw ioe;
        } finally {
            if (c != null) c.close();
        }
    }

    private void saveLastKnownGood(FileChannel c) throws IOException {
        FileOutputStream lkg = new FileOutputStream(getPath() + ".lkg", false);
        FileChannel lkgChan = lkg.getChannel();
        try {
            lkgChan.truncate(c.size());
            c.transferTo(0, c.size(), lkgChan);
        } finally {
            lkgChan.close();
        }
    }

    /**
     * Verify a user's password.
     * @return a boolean indicating validity of password
     * @throws FileNotFoundException if the underlying file has gone missing.
     * @throws StaleException if the underlying file has changed, only
     *         thrown upon successful password verification.
     */
    public boolean verifyPassword(char[] password) throws FileNotFoundException, StaleException {
        FileChannel c = null;
        try {
            c = new RandomAccessFile(file, "r").getChannel();
            ByteBuffer b = ByteBuffer.allocate(256);
            c.read(b);
            b.flip();
            byte length;
            length = b.get();
            byte[] paramBuf = new byte[length];
            b.get(paramBuf);
            length = b.get();
            byte[] encKey = new byte[length];
            b.get(encKey);
            Key key = generatePBEKey(password);
            Cipher pbeCipher = getPBECipher(key, paramBuf);
            byte[] keyBuf = pbeCipher.doFinal(encKey);
            loadSymmetricKey(keyBuf);
            b.position(VERIFICATION_OFFSET);
            length = b.get();
            byte[] verification = new byte[length];
            b.get(verification);
            version = b.getInt();
            char[] verificationText = decryptText(verification);
            if (!Arrays.equals(VERIFICATION_STRING, verificationText)) {
                throw new IOException("Unable to decrypt database: verification failed: " + new String(verificationText));
            }
            b.position(SERIAL_OFFSET);
            long dbSerial = b.getLong();
            if (dbSerial != serialNumber) {
                throw new StaleException("Serial number doesn't match, refresh database");
            }
            return true;
        } catch (GeneralSecurityException e) {
            log.log(Level.WARNING, "Unable to decrypt database", e);
        } catch (FileNotFoundException e) {
            log.log(Level.WARNING, "Underlying database file is missing", e);
            throw e;
        } catch (IOException e) {
            log.log(Level.SEVERE, "IO Error: " + e.getMessage(), e);
        } finally {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    public void create(char[] password) throws IOException {
        FileChannel c = null;
        try {
            Key key = generatePBEKey(password);
            Cipher pbeCipher = getPBECipher(key);
            AlgorithmParameters params = pbeCipher.getParameters();
            byte[] paramBuf = params.getEncoded();
            symKey = generateSymmetricKey();
            byte[] encKey = pbeCipher.doFinal(symKey.getEncoded());
            byte[] verification = encrypt(VERIFICATION_STRING.clone());
            ByteBuffer buf = ByteBuffer.allocate(8192);
            c = new FileOutputStream(file).getChannel();
            SecureRandom r = new SecureRandom();
            r.nextBytes(buf.array());
            buf.limit(buf.capacity());
            c.write(buf);
            c.position(0);
            buf.clear();
            buf.put((byte) paramBuf.length);
            buf.put(paramBuf);
            buf.put((byte) encKey.length);
            buf.put(encKey);
            buf.flip();
            c.write(buf);
            buf.clear();
            c.position(VERIFICATION_OFFSET);
            buf.put((byte) verification.length);
            buf.put(verification);
            buf.putInt(DATABASE_VERSION);
            buf.flip();
            c.write(buf);
            version = DATABASE_VERSION;
            buf.clear();
            c.position(SERIAL_OFFSET);
            buf.putLong(serialNumber);
            buf.flip();
            c.write(buf);
            buf.clear();
            c.position(IV_OFFSET);
            buf.put((byte) 0);
            buf.flip();
            c.write(buf);
            buf.clear();
            c.position(DATALEN_OFFSET);
            buf.putInt(0);
            buf.flip();
            c.write(buf);
            buf.clear();
            c.position(DATA_OFFSET);
            buf.putInt(0);
            buf.putInt(0);
            buf.putInt(0);
            buf.flip();
            c.write(buf);
        } catch (GeneralSecurityException e) {
            log.log(Level.SEVERE, "Unable to initialize crypto", e);
            IOException ioe = new IOException("Unable to initialize crypto");
            ioe.initCause(e);
            throw ioe;
        } finally {
            if (c != null) c.close();
        }
    }

    public String getPath() {
        return path;
    }

    public List<PasswordEntry> getEntries() {
        return entries;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public Key getKey() {
        return symKey;
    }

    /**
     * Convenience method, encrypts a piece of data in ECB mode.  Do not
     * use for data that is larger than a single block, or not within
     * another layer of encryption.
     */
    public byte[] encrypt(byte[] data) {
        byte[] encrypted = null;
        try {
            Cipher c = getSymmetricCipher(Cipher.ENCRYPT_MODE, getKey());
            encrypted = c.doFinal(data);
        } catch (GeneralSecurityException e) {
            log.log(Level.SEVERE, "Unable to encrypt data", e);
        }
        return encrypted;
    }

    public byte[] encrypt(char[] data) {
        CharBuffer cbuf = CharBuffer.wrap(data);
        ByteBuffer buf = charset.encode(cbuf);
        Arrays.fill(data, (char) 0);
        byte[] clear = new byte[buf.remaining()];
        buf.get(clear);
        Arrays.fill(buf.array(), (byte) 0);
        byte[] encrypted = encrypt(clear);
        Arrays.fill(clear, (byte) 0);
        return encrypted;
    }

    public byte[] decrypt(byte[] encrypted) {
        byte[] decrypted = null;
        try {
            Cipher c = getSymmetricCipher(Cipher.DECRYPT_MODE, getKey());
            decrypted = c.doFinal(encrypted);
        } catch (GeneralSecurityException e) {
            log.log(Level.SEVERE, "Unable to decrypt data", e);
        }
        return decrypted;
    }

    public char[] decryptText(byte[] encrypted) {
        ByteBuffer buf = ByteBuffer.wrap(decrypt(encrypted));
        CharBuffer cbuf = charset.decode(buf);
        buf.clear();
        Arrays.fill(buf.array(), (byte) 0);
        return cbuf.array();
    }

    private void loadData(FileChannel c) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(16384);
        c.position(IV_OFFSET);
        buf.limit(64);
        c.read(buf);
        buf.flip();
        byte ivlen = buf.get();
        AlgorithmParameters ivspec = null;
        if (ivlen > 0) {
            byte[] encIv = new byte[ivlen];
            buf.get(encIv);
            byte[] iv = decrypt(encIv);
            try {
                AlgorithmParameters params = AlgorithmParameters.getInstance(SYMMETRIC_ALGORITHM);
                params.init(iv);
                ivspec = params;
            } catch (NoSuchAlgorithmException e) {
                IOException ioe = new IOException("This should never happen");
                ioe.initCause(e);
                throw ioe;
            }
        }
        if (ivspec == null) log.info("New database loaded, not reading data"); else readData(c, buf, ivspec);
    }

    private int ensureAvailable(FileChannel c, Cipher cipher, ByteBuffer encrypted, ByteBuffer decrypted, int len, int limit) throws IOException {
        int r = 0;
        if (limit < 0) throw new IOException("Read overflow");
        if (limit == 0 && decrypted.remaining() < len) {
            try {
                byte[] fin = cipher.doFinal();
                if (fin != null && fin.length > 0) {
                    decrypted.compact();
                    decrypted.put(fin);
                    decrypted.flip();
                }
            } catch (IllegalBlockSizeException e) {
                log.log(Level.SEVERE, "Unable to decrypt final block", e);
                IOException ioe = new IOException("Unable to decrypt final block: " + e.getMessage());
                ioe.initCause(e);
                throw ioe;
            } catch (BadPaddingException e) {
                log.log(Level.SEVERE, "Unable to decrypt final block", e);
                IOException ioe = new IOException("Unable to decrypt final block: " + e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
        } else if (len > limit && decrypted.remaining() < len) {
            String msg = "Len > Limit: " + len + ":" + limit + " (" + decrypted.remaining() + ")";
            log.log(Level.SEVERE, msg);
            throw new IOException(msg);
        }
        while (decrypted.remaining() < len) {
            decrypted.compact();
            decrypted.limit(decrypted.position());
            encrypted.compact();
            if (limit > 0 && limit < encrypted.remaining()) encrypted.limit(encrypted.position() + limit);
            int read = c.read(encrypted);
            limit -= read;
            r += read;
            encrypted.flip();
            byte[] clear = cipher.update(encrypted.array(), encrypted.position(), encrypted.remaining());
            encrypted.position(encrypted.limit());
            if (clear != null && clear.length > 0) {
                decrypted.limit(decrypted.capacity());
                decrypted.put(clear);
            }
            decrypted.flip();
        }
        return r;
    }

    private void readData(FileChannel c, ByteBuffer buf, AlgorithmParameters iv) throws IOException {
        buf.clear();
        c.position(DATALEN_OFFSET);
        buf.limit(4);
        c.read(buf);
        buf.flip();
        int datalen = buf.getInt();
        log.fine("Encrypted data length: " + datalen);
        int remaining = datalen;
        buf.clear();
        buf.limit(12);
        c.position(DATA_OFFSET);
        c.read(buf);
        buf.flip();
        int keyPairCount = buf.getInt();
        log.finest("Loading keypairs: " + keyPairCount);
        int folderCount = buf.getInt();
        log.finest("Loading folders: " + folderCount);
        int entryCount = buf.getInt();
        log.finest("Loading entries: " + entryCount);
        try {
            Cipher symCipher = getCBCSymmetricCipher(getKey(), iv);
            keyPairs.clear();
            folders.clear();
            entries.clear();
            ByteBuffer cleartext = ByteBuffer.allocate(buf.capacity());
            cleartext.limit(0);
            for (int i = 0; i < keyPairCount; i++) {
                int read;
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, 2, remaining);
                int len = cleartext.getShort();
                read = ensureAvailable(c, symCipher, buf, cleartext, len, remaining);
                remaining -= read;
                byte[] certBuf = new byte[len];
                cleartext.get(certBuf);
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, 2, remaining);
                len = cleartext.getShort();
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, len, remaining);
                byte[] keyBuf = new byte[len];
                cleartext.get(keyBuf);
                if (CertificateGenerator.isEnabled()) {
                    X509Certificate cert = CertificateGenerator.loadCertificate(certBuf);
                    PrivateKey key = CertificateGenerator.loadKey(keyBuf);
                    keyPairs.put(cert, key);
                }
            }
            for (int i = 0; i < folderCount; i++) {
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, 2, remaining);
                short len = cleartext.getShort();
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, len, remaining);
                ByteBuffer b = cleartext.slice();
                cleartext.position(cleartext.position() + len);
                b.limit(len);
                deserializeFolder(b);
            }
            for (int i = 0; i < entryCount; i++) {
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, 2, remaining);
                short len = cleartext.getShort();
                remaining -= ensureAvailable(c, symCipher, buf, cleartext, len, remaining);
                ByteBuffer b = cleartext.slice();
                cleartext.position(cleartext.position() + len);
                b.limit(len);
                deserializeEntry(b);
            }
            log.info("Database loaded successfully");
        } catch (GeneralSecurityException e) {
            log.log(Level.WARNING, "Unable to decrypt database", e);
            IOException ioe = new IOException("Unable to decrypt database");
            ioe.initCause(e);
            throw ioe;
        } catch (IllegalArgumentException e) {
            log.log(Level.WARNING, "Unable to decrypt database", e);
            IOException ioe = new IOException("Unable to decrypt database");
            ioe.initCause(e);
            throw ioe;
        } catch (IllegalStateException e) {
            log.log(Level.WARNING, "Unable to decrypt database", e);
            IOException ioe = new IOException("Unable to decrypt database");
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void save() throws IOException, StaleException {
        log.finest("Saving database");
        FileChannel c = null;
        try {
            ByteBuffer buf = ByteBuffer.allocate(16384);
            c = new RandomAccessFile(file, "rw").getChannel();
            if (version < DATABASE_VERSION) {
                log.info("Database version was: " + version);
                log.info("Updating database version to: " + DATABASE_VERSION);
                buf.clear();
                byte[] verification = encrypt(VERIFICATION_STRING.clone());
                c.position(VERIFICATION_OFFSET);
                buf.put((byte) verification.length);
                buf.put(verification);
                buf.putInt(DATABASE_VERSION);
                buf.flip();
                c.write(buf);
                version = DATABASE_VERSION;
            }
            c.position(SERIAL_OFFSET);
            buf.clear();
            buf.limit(8);
            c.read(buf);
            buf.flip();
            long dbSerial = buf.getLong();
            if (dbSerial != serialNumber) throw new StaleException("Serial number doesn't match, reload db");
            buf.clear();
            buf.putLong(++serialNumber);
            buf.flip();
            c.position(SERIAL_OFFSET);
            c.write(buf);
            c.position(DATA_OFFSET);
            buf.clear();
            log.finest("Saving keypairs: " + keyPairs.size());
            buf.putInt(keyPairs.size());
            log.finest("Saving folders: " + folders.size());
            buf.putInt(folders.size());
            log.finest("Saving entries: " + entries.size());
            buf.putInt(entries.size());
            buf.flip();
            c.write(buf);
            long pos = c.position();
            byte[] iv;
            try {
                Cipher symCipher = getCBCSymmetricCipher(getKey());
                AlgorithmParameters params = symCipher.getParameters();
                iv = params.getEncoded();
                for (Map.Entry<X509Certificate, PrivateKey> pair : keyPairs.entrySet()) {
                    buf.clear();
                    byte[] encrypted;
                    byte[] encoded = pair.getKey().getEncoded();
                    short len = (short) encoded.length;
                    tmp.clear();
                    tmp.putShort(len);
                    tmp.put(encoded);
                    tmp.flip();
                    encrypted = symCipher.update(tmp.array(), tmp.position(), tmp.remaining());
                    if (encrypted != null && encrypted.length > 0) buf.put(encrypted);
                    encoded = pair.getValue().getEncoded();
                    len = (short) encoded.length;
                    tmp.clear();
                    tmp.putShort(len);
                    tmp.put(encoded);
                    tmp.flip();
                    encrypted = symCipher.update(tmp.array(), tmp.position(), tmp.remaining());
                    if (encrypted != null && encrypted.length > 0) buf.put(encrypted);
                    buf.flip();
                    c.write(buf);
                }
                for (Folder f : folders) {
                    buf.clear();
                    ByteBuffer b = serialize(f);
                    byte[] encrypted = symCipher.update(b.array(), b.position(), b.remaining());
                    buf.put(encrypted);
                    buf.flip();
                    c.write(buf);
                }
                for (PasswordEntry e : entries) {
                    buf.clear();
                    ByteBuffer b = serialize(e);
                    byte[] encrypted = symCipher.update(b.array(), b.position(), b.remaining());
                    if (encrypted != null && encrypted.length > 0) buf.put(encrypted);
                    buf.flip();
                    c.write(buf);
                }
                byte[] finalBlock = symCipher.doFinal();
                if (finalBlock != null && finalBlock.length > 0) {
                    buf.clear();
                    buf.put(finalBlock);
                    buf.flip();
                    c.write(buf);
                }
            } catch (GeneralSecurityException e) {
                log.log(Level.WARNING, "Unable to encrypt database", e);
                IOException ioe = new IOException("Unable to encrypt database");
                ioe.initCause(e);
                throw ioe;
            }
            buf.clear();
            int len = (int) (c.position() - pos);
            buf.putInt(len);
            buf.flip();
            c.position(DATALEN_OFFSET);
            c.write(buf);
            buf.clear();
            byte[] encIv = encrypt(iv);
            buf.put((byte) encIv.length);
            buf.put(encIv);
            buf.flip();
            c.position(IV_OFFSET);
            c.write(buf);
            log.info("Database saved successfully");
        } finally {
            if (c != null) c.close();
        }
    }

    private ByteBuffer serialize(Folder f) throws IOException {
        tmp.clear();
        tmp.putShort((short) 0);
        int pos = tmp.position();
        tmp.put((byte) Fields.FOLDER_ENTRY.ordinal());
        tmp.putInt(f.getId());
        tmp.put((byte) Fields.FOLDER_ID.ordinal());
        tmp.putInt(f.getParentId());
        tmp.put((byte) Fields.ENTRY_NAME.ordinal());
        encodeShortString(tmp, f.getName());
        if (f.getComments() != null) {
            tmp.put((byte) Fields.COMMENTS.ordinal());
            encodeLongString(tmp, f.getComments());
        }
        int limit = tmp.position();
        short len = (short) (limit - pos);
        tmp.putShort(0, len);
        tmp.limit(limit);
        tmp.rewind();
        return tmp;
    }

    private void deserializeFolder(ByteBuffer data) {
        Folder f = new Folder();
        boolean isFolder = false;
        while (data.remaining() > 0) {
            byte b = data.get();
            Fields field = EnumUtil.invertEnumOrdinal(b, Fields.class);
            switch(field) {
                case FOLDER_ENTRY:
                    {
                        isFolder = true;
                        int id = data.getInt();
                        f.setId(id);
                        break;
                    }
                case FOLDER_ID:
                    {
                        int id = data.getInt();
                        f.setParentId(id);
                        break;
                    }
                case ENTRY_NAME:
                    {
                        f.setName(getShortString(data));
                        break;
                    }
                case COMMENTS:
                    {
                        f.setComments(getLongString(data));
                        break;
                    }
                default:
                    throw new IllegalArgumentException("Unexpected field: " + field);
            }
        }
        if (!isFolder) {
            log.severe("Item is not a folder!");
            throw new IllegalStateException("Processed item is not a folder");
        }
        folders.add(f);
    }

    private ByteBuffer serialize(PasswordEntry e) throws IOException {
        tmp.clear();
        tmp.putShort((short) 0);
        int pos = tmp.position();
        tmp.put((byte) Fields.PASSWORD_ENTRY.ordinal());
        tmp.put((byte) Fields.ENTRY_NAME.ordinal());
        encodeShortString(tmp, e.getEntryName());
        tmp.put((byte) Fields.FOLDER_ID.ordinal());
        tmp.putInt(e.getFolderId());
        tmp.put((byte) Fields.ENCRYPTED_PASSWORD.ordinal());
        tmp.put((byte) e.getEncryptedPassword().length);
        tmp.put(e.getEncryptedPassword());
        if (e.getUserName() != null) {
            tmp.put((byte) Fields.USERNAME.ordinal());
            encodeShortString(tmp, e.getUserName());
        }
        if (e.getComments() != null) {
            tmp.put((byte) Fields.COMMENTS.ordinal());
            encodeLongString(tmp, e.getComments());
        }
        if (e.getLoginURL() != null) {
            tmp.put((byte) Fields.LOGIN_URL.ordinal());
            encodeShortString(tmp, e.getLoginURL().toString());
        }
        if (e.getAutoTypeSequence() != null) {
            tmp.put((byte) Fields.AUTOTYPE_SEQUENCE.ordinal());
            encodeShortString(tmp, e.getAutoTypeSequence());
        }
        for (URI uri : e.getAllowedURLs()) {
            tmp.put((byte) Fields.ALLOWED_URL.ordinal());
            encodeShortString(tmp, uri.toString());
        }
        int limit = tmp.position();
        short len = (short) (limit - pos);
        tmp.putShort(0, len);
        tmp.limit(limit);
        tmp.rewind();
        return tmp;
    }

    private void deserializeEntry(ByteBuffer data) {
        PasswordEntry e = new PasswordEntry();
        boolean isPasswordEntry = false;
        while (data.remaining() > 0) {
            byte b = data.get();
            Fields field = EnumUtil.invertEnumOrdinal(b, Fields.class);
            switch(field) {
                case PASSWORD_ENTRY:
                    isPasswordEntry = true;
                    break;
                case ALLOWED_URL:
                    {
                        String uri = getShortString(data);
                        try {
                            URI u = new URI(uri);
                            e.getAllowedURLs().add(u);
                        } catch (URISyntaxException ex) {
                            throw new IllegalArgumentException(uri, ex);
                        }
                        break;
                    }
                case ENCRYPTED_PASSWORD:
                    {
                        byte len = data.get();
                        byte[] pw = new byte[len];
                        data.get(pw);
                        e.setEncryptedPassword(pw);
                        break;
                    }
                case LOGIN_URL:
                    {
                        String uri = getShortString(data);
                        try {
                            URI u = new URI(uri);
                            e.setLoginURL(u);
                        } catch (URISyntaxException ex) {
                            throw new IllegalArgumentException(uri, ex);
                        }
                        break;
                    }
                case USERNAME:
                    {
                        e.setUserName(getShortString(data));
                        break;
                    }
                case FOLDER_ID:
                    {
                        int id = data.getInt();
                        e.setFolderId(id);
                        break;
                    }
                case ENTRY_NAME:
                    {
                        e.setEntryName(getShortString(data));
                        break;
                    }
                case COMMENTS:
                    {
                        e.setComments(getLongString(data));
                        break;
                    }
                case AUTOTYPE_SEQUENCE:
                    e.setAutoTypeSequence(getShortString(data));
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected field: " + field);
            }
        }
        if (!isPasswordEntry) {
            log.severe("Item is not a password entry!");
            throw new IllegalStateException("Parsed item is not a password entry");
        }
        entries.add(e);
    }

    private String getShortString(ByteBuffer b) {
        int len = b.get() & 0xff;
        int limit = b.limit();
        b.limit(b.position() + len);
        String s = charset.decode(b).toString();
        b.limit(limit);
        return s;
    }

    private String getLongString(ByteBuffer b) {
        short len = b.getShort();
        int limit = b.limit();
        b.limit(b.position() + len);
        String s = charset.decode(b).toString();
        b.limit(limit);
        return s;
    }

    private void encodeShortString(ByteBuffer b, String s) {
        ByteBuffer encoded = charset.encode(s);
        b.put((byte) encoded.remaining());
        b.put(encoded);
    }

    private void encodeLongString(ByteBuffer b, String s) {
        ByteBuffer encoded = charset.encode(s);
        b.putShort((short) encoded.remaining());
        b.put(encoded);
    }

    private Cipher getSymmetricCipher(int mode, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM);
        c.init(mode, key);
        return c;
    }

    private Cipher getCBCSymmetricCipher(Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM + "/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c;
    }

    private Cipher getCBCSymmetricCipher(Key key, AlgorithmParameters iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM + "/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key, iv);
        return c;
    }

    private void loadSymmetricKey(byte[] keyData) {
        symKey = new SecretKeySpec(keyData, SYMMETRIC_ALGORITHM);
    }

    private Key generateSymmetricKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
        return kg.generateKey();
    }

    private Cipher getPBECipher(Key key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Cipher c = Cipher.getInstance(PBE_ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        return c;
    }

    private Cipher getPBECipher(Key key, byte[] paramBuf) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameters params = AlgorithmParameters.getInstance(PBE_ALGORITHM);
        params.init(paramBuf);
        Cipher c = Cipher.getInstance(PBE_ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key, params);
        return c;
    }

    private Key generatePBEKey(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom r = new SecureRandom();
        byte[] salt = new byte[16];
        r.nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBE_ITERATION_COUNT);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBE_ALGORITHM);
        Key key = skf.generateSecret(spec);
        spec.clearPassword();
        Arrays.fill(password, (char) 0);
        return key;
    }

    public Map.Entry<X509Certificate, PrivateKey> getCAKeyPair() {
        if (keyPairs.size() > 0) {
            return keyPairs.entrySet().iterator().next();
        }
        return null;
    }

    public Map.Entry<X509Certificate, PrivateKey> getServerKeyPair() {
        if (keyPairs.size() > 1) {
            Iterator<Map.Entry<X509Certificate, PrivateKey>> i = keyPairs.entrySet().iterator();
            i.next();
            return i.next();
        }
        return null;
    }

    public void setCAKeyPair(X509Certificate caCert, PrivateKey caKey) {
        if (keyPairs.size() == 0) keyPairs.put(caCert, caKey); else if (keyPairs.size() == 1) {
            keyPairs.clear();
            keyPairs.put(caCert, caKey);
        } else if (keyPairs.size() > 1) {
            Map.Entry<X509Certificate, PrivateKey> serverPair;
            Iterator<Map.Entry<X509Certificate, PrivateKey>> i = keyPairs.entrySet().iterator();
            i.next();
            serverPair = i.next();
            keyPairs.clear();
            keyPairs.put(caCert, caKey);
            keyPairs.put(serverPair.getKey(), serverPair.getValue());
        }
    }

    /**
     *  cannot be called if a CA keypair has not been set first.
     */
    public void setServerKeyPair(X509Certificate serverCert, PrivateKey serverKey) {
        if (keyPairs.size() == 0) throw new IllegalStateException("Set the CA keypair first"); else if (keyPairs.size() == 1) {
            keyPairs.put(serverCert, serverKey);
        } else if (keyPairs.size() > 1) {
            Iterator i = keyPairs.entrySet().iterator();
            i.next();
            i.next();
            i.remove();
            keyPairs.put(serverCert, serverKey);
        }
    }

    public void removeServerKeyPair() {
        Map.Entry<X509Certificate, PrivateKey> pair = getServerKeyPair();
        if (pair != null) keyPairs.remove(pair.getKey());
    }

    public void removeCAKeyPair() {
        keyPairs.clear();
    }

    public PasswordServer getPasswordServer() {
        return passwordServer;
    }

    public void setPasswordServer(PasswordServer passwordServer) {
        this.passwordServer = passwordServer;
    }

    public File getFile() {
        return file;
    }

    public static class StaleException extends Exception {

        StaleException(String msg) {
            super(msg);
        }
    }
}
