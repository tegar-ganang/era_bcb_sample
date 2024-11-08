package pl.JMediaNFOCreator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class MD5Checksum {

    public static byte[] createChecksum(String fileName) {
        InputStream fis;
        MessageDigest complete = null;
        try {
            fis = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return complete.digest();
    }

    public static String getMD5Checksum(String filename) {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
