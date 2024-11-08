package net.cyb0rk.vcrypt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {

    public static void encryptFile(String input, String output, String pwd) throws Exception {
        CipherOutputStream out;
        InputStream in;
        Cipher cipher;
        SecretKey key;
        byte[] byteBuffer;
        cipher = Cipher.getInstance("DES");
        key = new SecretKeySpec(pwd.getBytes(), "DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        in = new FileInputStream(input);
        out = new CipherOutputStream(new FileOutputStream(output), cipher);
        byteBuffer = new byte[1024];
        for (int n; (n = in.read(byteBuffer)) != -1; out.write(byteBuffer, 0, n)) ;
        in.close();
        out.close();
    }

    public static void decryptFile(String input, String output, String pwd) throws Exception {
        CipherInputStream in;
        OutputStream out;
        Cipher cipher;
        SecretKey key;
        byte[] byteBuffer;
        cipher = Cipher.getInstance("DES");
        key = new SecretKeySpec(pwd.getBytes(), "DES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        in = new CipherInputStream(new FileInputStream(input), cipher);
        out = new FileOutputStream(output);
        byteBuffer = new byte[1024];
        for (int n; (n = in.read(byteBuffer)) != -1; out.write(byteBuffer, 0, n)) ;
        in.close();
        out.close();
    }
}
