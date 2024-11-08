package jp.co.baka.pachinko.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.internet.MimeUtility;

public class Crypt {

    private Crypt() {
    }

    /**
     * 暗号化
     *
     * @param key
     * @param text
     * @return
     */
    public static String encrypt(String key, String text) {
        String encrypted = null;
        try {
            SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, sksSpec);
            byte[] b = cipher.doFinal(text.getBytes());
            encrypted = encodeBase64(b);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return encrypted;
    }

    /**
     * 復号化
     *
     * @param key
     * @param encrypted
     * @return
     */
    public static String decrypt(String key, String encrypted) {
        byte[] decrypted = null;
        try {
            SecretKeySpec sksSpec = new SecretKeySpec(key.getBytes(), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, sksSpec);
            decrypted = cipher.doFinal(decodeBase64(encrypted));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return new String(decrypted);
    }

    /**
     * base64 encode
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String encodeBase64(byte[] data) throws Exception {
        ByteArrayOutputStream forEncode = new ByteArrayOutputStream();
        OutputStream toBase64 = MimeUtility.encode(forEncode, "base64");
        toBase64.write(data);
        toBase64.close();
        return forEncode.toString("iso-8859-1");
    }

    /**
     * base64 decode
     *
     * @param base64
     * @return
     * @throws Exception
     */
    public static byte[] decodeBase64(String base64) throws Exception {
        InputStream fromBase64 = MimeUtility.decode(new ByteArrayInputStream(base64.getBytes()), "base64");
        byte[] buf = new byte[1024];
        ByteArrayOutputStream toByteArray = new ByteArrayOutputStream();
        for (int len = -1; (len = fromBase64.read(buf)) != -1; ) toByteArray.write(buf, 0, len);
        return toByteArray.toByteArray();
    }
}
