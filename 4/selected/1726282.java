package crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * simple password based encryption demo
 * 
 * @author executor
 */
public class Main {

    /**
	 * @param args[0]
	 *            -e (encrypt) or -d (decrypt)
	 * @param args[1]
	 *            path to file to encrypt/decrypt
	 * @param args[2]:
	 *            password to use for encryption/decryption
	 */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("not enough arguments");
            System.exit(-1);
        }
        new Main(args);
    }

    private Cipher cipher = null;

    /**
	 * @param args
	 *            same as static main method
	 */
    public Main(String[] args) {
        boolean encrypt = false;
        if (args[0].compareTo("-e") == 0) {
            encrypt = true;
        } else if (args[0].compareTo("-d") == 0) {
            encrypt = false;
        } else {
            System.out.println("first argument is invalid");
            System.exit(-2);
        }
        char[] password = new char[args[2].length()];
        for (int i = 0; i < args[2].length(); i++) {
            password[i] = (char) args[2].getBytes()[i];
        }
        try {
            InitializeCipher(encrypt, password);
        } catch (Exception e) {
            System.out.println("error initializing cipher");
            System.exit(-3);
        }
        try {
            InputStream is = new FileInputStream(args[1]);
            OutputStream os;
            int read, max = 10;
            byte[] buffer = new byte[max];
            if (encrypt) {
                os = new FileOutputStream(args[1] + ".enc");
                os = new CipherOutputStream(os, cipher);
            } else {
                os = new FileOutputStream(args[1] + ".dec");
                is = new CipherInputStream(is, cipher);
            }
            read = is.read(buffer);
            while (read != -1) {
                os.write(buffer, 0, read);
                read = is.read(buffer);
            }
            while (read == max) ;
            os.close();
            is.close();
            System.out.println(new String(buffer));
        } catch (Exception e) {
            System.out.println("error encrypting/decrypting message:");
            e.printStackTrace();
            System.exit(-4);
        }
        System.out.println("done");
    }

    /**
	 * initializes the class member 'cipher' to a valid cipher
	 * 
	 * @param encrypt
	 *            indicates whether the cipher will be used for encryption or
	 *            decryption
	 * @param password
	 *            the password used for the cipher
	 * @throws Exception
	 */
    private void InitializeCipher(boolean encrypt, char[] password) throws Exception {
        PBEKeySpec pbeKeySpec;
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        byte[] salt = { (byte) 0xa8, (byte) 0x12, (byte) 0x3c, (byte) 0x8a, (byte) 0x72, (byte) 0x12, (byte) 0xa1, (byte) 0x52 };
        int count = 20;
        pbeParamSpec = new PBEParameterSpec(salt, count);
        pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);
        cipher = Cipher.getInstance("PBEWithMD5AndDES");
        if (encrypt) {
            cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        }
    }
}
