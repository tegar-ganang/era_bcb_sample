package de.burlov.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.engines.SerpentEngine;
import de.burlov.bouncycastle.io.CryptInputStream;
import de.burlov.bouncycastle.io.CryptOutputStream;

/**
 * @author paul
 * 
 */
public class CryptUtils {

    /**
	 * Verschluesselt ein Datenblock mit Serpent Cipher in AEAD/EAX Modus.
	 * 
	 * @param cleartext
	 * @param key
	 * @return
	 */
    public static byte[] encrypt(byte[] cleartext, byte[] key) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            CryptOutputStream out = new CryptOutputStream(bout, new SerpentEngine(), key);
            out.write(cleartext);
            out.close();
            return bout.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key) throws IOException {
        CryptInputStream in = new CryptInputStream(new ByteArrayInputStream(ciphertext), new SerpentEngine(), key);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(in, bout);
        return bout.toByteArray();
    }
}
