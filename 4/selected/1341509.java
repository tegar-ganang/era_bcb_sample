package com.nimbusinformatics.genomicstransfer.security;

import com.nimbusinformatics.genomicstransfer.util.Strings;
import org.apache.commons.io.FileUtils;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Arrays;

public class KeyManager {

    private static final String DATA_ENCRYPTION_ALGORITHM = "DESede";

    private static final String KEY_ENCRYPTION_ALGORITHM = "PBEWithMD5AndDES";

    private static final String KEY_STORAGE_DIR = ".genomicstransfer";

    private static final String SECRET_KEY_FILE = "secret.key";

    private final File keyStorageDir;

    private final File secretKeyFile;

    private final KeyManagerUI ui;

    private Key secretKey;

    public KeyManager(KeyManagerUI ui) {
        this.ui = ui;
        keyStorageDir = new File(System.getProperty("user.home"), KEY_STORAGE_DIR);
        secretKeyFile = new File(keyStorageDir, SECRET_KEY_FILE);
    }

    public Cipher createEncryptCipher() {
        try {
            Cipher cipher = Cipher.getInstance(DATA_ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    public boolean loadOrGenerateKey() {
        if (secretKeyFile.exists() && secretKeyFile.isFile()) {
            return loadKey();
        }
        return generateKey();
    }

    private boolean generateKey() {
        try {
            if (secretKeyFile.exists() && !ui.confirm("The secret key '" + secretKeyFile + "' already exists. Overwrite it?")) {
                return false;
            }
            FileUtils.forceMkdir(keyStorageDir);
            char[] password = ui.createPassword();
            if (password == null) {
                return false;
            }
            try {
                PBEKeySpec passwordKeySpec = new PBEKeySpec(password);
                SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ENCRYPTION_ALGORITHM);
                SecretKey passwordKey = factory.generateSecret(passwordKeySpec);
                Cipher cipher = Cipher.getInstance(KEY_ENCRYPTION_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, passwordKey);
                KeyGenerator generator = KeyGenerator.getInstance(DATA_ENCRYPTION_ALGORITHM);
                secretKey = generator.generateKey();
                writeSecretKey(cipher);
            } finally {
                Arrays.fill(password, '\0');
            }
            ui.information(Strings.getSecretKeyGeneratedMessage(keyStorageDir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private boolean loadKey() {
        try {
            if (!secretKeyFile.exists() || !secretKeyFile.isFile()) {
                return false;
            }
            while (true) {
                char[] password = ui.getPassword();
                if (password == null) {
                    return false;
                }
                try {
                    PBEKeySpec passwordKeySpec = new PBEKeySpec(password);
                    SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ENCRYPTION_ALGORITHM);
                    SecretKey passwordKey = factory.generateSecret(passwordKeySpec);
                    loadSecretKey(passwordKey);
                    return true;
                } catch (InvalidKeyException e) {
                    ui.error(Strings.getInvalidPassword());
                } finally {
                    Arrays.fill(password, '\0');
                }
            }
        } catch (Exception e) {
            ui.error(Strings.getErrorLoadingSecretKey(e.getLocalizedMessage()));
            return false;
        }
    }

    private void loadSecretKey(SecretKey passwordKey) throws Exception {
        ObjectInputStream privateStream = new ObjectInputStream(FileUtils.openInputStream(secretKeyFile));
        try {
            SealedObject object = (SealedObject) privateStream.readObject();
            secretKey = (Key) object.getObject(passwordKey);
        } finally {
            privateStream.close();
        }
    }

    private void writeSecretKey(Cipher cipher) throws Exception {
        SealedObject object = new SealedObject(secretKey, cipher);
        ObjectOutputStream privateStream = new ObjectOutputStream(FileUtils.openOutputStream(secretKeyFile));
        try {
            privateStream.writeObject(object);
        } finally {
            privateStream.close();
        }
    }
}
