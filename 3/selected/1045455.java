package ps.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    byte[] md5Bytes;

    public MD5(byte[] md5Bytes) {
        this.md5Bytes = md5Bytes;
    }

    public byte[] getBytes() {
        return md5Bytes;
    }

    @Override
    public boolean equals(Object o) {
        boolean ret = false;
        if (o != null && o instanceof MD5) {
            byte[] oBytes = ((MD5) o).getBytes();
            if (oBytes != null && oBytes.length == md5Bytes.length) {
                ret = true;
                for (int i = 0; i < oBytes.length; i++) {
                    if (oBytes[i] != md5Bytes[i]) {
                        ret = false;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static byte[] generateAuthId(String userName, String password) {
        byte[] ret = new byte[16];
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            String str = userName + password;
            messageDigest.update(str.getBytes());
            ret = messageDigest.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return ret;
    }
}
