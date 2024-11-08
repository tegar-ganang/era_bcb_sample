package geopms;

import java.io.*;
import java.security.Key;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.*;

/**
 *
 * @author lazarus
 */
public class GeoPMSSecure {

    private static Logger logger = Logger.getLogger("com.geores.geopms");

    private String password = "";

    /** Creates a new instance of GeoPMSSecure */
    public GeoPMSSecure(String pw) {
        password = pw;
    }

    private static void encode(byte[] bytes, OutputStream out, String pass) throws Exception {
        Cipher c = Cipher.getInstance("DES");
        Key k = new SecretKeySpec(pass.getBytes(), "DES");
        c.init(Cipher.ENCRYPT_MODE, k);
        OutputStream cos = new CipherOutputStream(out, c);
        cos.write(bytes);
        cos.close();
    }

    private static byte[] decode(InputStream is, String pass) throws Exception {
        Cipher c = Cipher.getInstance("DES");
        Key k = new SecretKeySpec(pass.getBytes(), "DES");
        c.init(Cipher.DECRYPT_MODE, k);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CipherInputStream cis = new CipherInputStream(is, c);
        for (int b; (b = cis.read()) != -1; ) bos.write(b);
        cis.close();
        return bos.toByteArray();
    }

    public byte[] getSecureData(byte[] sdata) {
        byte[] data = {};
        InputStream is = new ByteArrayInputStream(sdata);
        try {
            data = decode(is, password);
        } catch (Exception e) {
            logger.severe("Decoding error: " + e);
        }
        return data;
    }

    public byte[] setSecureData(String data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            encode(data.getBytes(), out, password);
        } catch (Exception e) {
            logger.severe("Encoding error: " + e);
        }
        return out.toByteArray();
    }
}
