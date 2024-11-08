package kuasar.plugin.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Jesus Navalon i Pastor <jnavalon at redhermes dot net>
 */
public final class Utils {

    private static final int BUFFER_SIZE = 1024;

    public static String getSHA1(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] digest;
            long i = 0;
            long size = file.length();
            String hash = "";
            byte[] data = new byte[size > BUFFER_SIZE ? BUFFER_SIZE : (int) (size)];
            while (bis.read(data) > 0) {
                md.update(data);
                i += data.length;
                if (size - i < BUFFER_SIZE) data = new byte[(int) (size - i)];
            }
            digest = md.digest();
            for (byte aux : digest) {
                int b = aux & 0xff;
                if (Integer.toHexString(b).length() == 1) {
                    hash += "0";
                }
                hash += Integer.toHexString(b);
            }
            return hash;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}
