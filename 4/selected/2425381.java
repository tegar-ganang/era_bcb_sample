package data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author kronenwe
 * 
 */
public class CrawlerCrypto {

    protected SecretKeySpec skeySpec;

    /**
	 * Singleton: Referenz.
	 */
    private static CrawlerCrypto instance;

    /**
	 * Singleton: Zugriff.
	 * 
	 * @return Die einzige Audiorecorder-Instanz in diesem Prozess.
	 */
    public static CrawlerCrypto getInstance() {
        if (instance == null) {
            instance = new CrawlerCrypto();
        }
        return instance;
    }

    private void error(String t) {
        System.out.println("ERROR: VennMakerCrypto:\t" + t);
    }

    /**
	 * Generate a AES key, store the key in memory and write the key to a file
	 * 
	 * @param keyfile
	 *           name of the key file
	 */
    public void generateKeyAES(String keyfile) {
        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            SecretKey skey = kgen.generateKey();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keyfile));
            out.writeObject(skey);
            out.close();
            out = new ObjectOutputStream(new FileOutputStream(keyfile));
            out.writeObject(skey);
            out.close();
        } catch (NoSuchAlgorithmException exn) {
            exn.printStackTrace();
        } catch (FileNotFoundException exn) {
            error("writing key-file");
        } catch (IOException exn) {
            exn.printStackTrace();
        }
    }

    /**
	 * Load a AES key from a file
	 * 
	 * @param keyfile
	 *           name of the key file
	 */
    public void getKeyAESfromFile(String keyfile) {
        ObjectInputStream keyIn;
        try {
            keyIn = new ObjectInputStream(new FileInputStream(keyfile));
            SecretKey key = (SecretKey) keyIn.readObject();
            keyIn.close();
            byte[] raw = key.getEncoded();
            skeySpec = new SecretKeySpec(raw, "AES");
        } catch (FileNotFoundException exn) {
            error("key-file not found");
        } catch (IOException exn) {
            exn.printStackTrace();
        } catch (ClassNotFoundException exn) {
            exn.printStackTrace();
        }
    }

    /**
	 * Encode a string
	 * 
	 * @param source
	 *           string string to encode
	 */
    public String encodeAES(String input) {
        byte[] bytes = null;
        byte[] out = null;
        try {
            bytes = input.getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException exn1) {
            exn1.printStackTrace();
        }
        Cipher c;
        try {
            c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, skeySpec);
            out = c.doFinal(bytes);
        } catch (NoSuchAlgorithmException exn1) {
            error("no AES key found");
        } catch (NoSuchPaddingException exn1) {
            error("no AES key found");
        } catch (InvalidKeyException exn) {
            error("no AES key found");
        } catch (IllegalBlockSizeException exn) {
            exn.printStackTrace();
        } catch (BadPaddingException exn) {
            exn.printStackTrace();
        }
        String result = null;
        try {
            result = new String(out, "ISO8859_1");
        } catch (UnsupportedEncodingException exn) {
            error("encode AES failure");
        }
        return result;
    }

    /**
	 * Decode a string
	 * 
	 * @param source
	 *           encoded string encoded string to decode
	 */
    public String decodeAES(String input) {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(input.getBytes("ISO8859_1"));
        } catch (UnsupportedEncodingException exn1) {
            exn1.printStackTrace();
        }
        Cipher c;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, skeySpec);
            CipherInputStream cis = new CipherInputStream(is, c);
            for (int b; (b = cis.read()) != -1; ) bos.write(b);
            cis.close();
        } catch (NoSuchAlgorithmException exn) {
            exn.printStackTrace();
        } catch (NoSuchPaddingException exn) {
            exn.printStackTrace();
        } catch (InvalidKeyException exn) {
            exn.printStackTrace();
        } catch (IOException exn) {
            exn.printStackTrace();
        }
        String result = null;
        try {
            result = new String(((ByteArrayOutputStream) bos).toByteArray(), "ISO8859_1");
        } catch (UnsupportedEncodingException exn) {
            exn.printStackTrace();
        }
        return result;
    }
}
