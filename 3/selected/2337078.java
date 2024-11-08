package org.openymsg.legacy.network.challenge;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class ChallengeResponseUtility {

    private static final String Y64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789._";

    /**
     * Yahoo uses its own custom variation on Base64 encoding (although a little birdy tells me this routine actually
     * comes from the Apple Mac?)
     * 
     * For those not familiar with Base64 etc, all this does is treat an array of bytes as a bit stream, sectioning the
     * stream up into six bit slices, which can be represented by the 64 characters in the 'table' Y64 above. In this
     * fashion raw binary data can be expressed as valid 7 bit printable ASCII - although the size of the data will
     * expand by 25% - three bytes (24 bits) taking up four ASCII characters. Now obviously the bit stream will
     * terminate mid way through an ASCII character if the input array size isn't evenly divisible by 3. To flag this,
     * either one or two dashes are appended to the output. A single dash if we're two over, and two dashes if we're
     * only one over. (No dashes are appended if the input size evenly divides by 3.)
     */
    public static final String yahoo64(byte[] buffer) {
        int limit = buffer.length - (buffer.length % 3);
        StringBuffer out = new StringBuffer();
        int[] buff = new int[buffer.length];
        for (int i = 0; i < buffer.length; i++) buff[i] = buffer[i] & 0xff;
        for (int i = 0; i < limit; i += 3) {
            out.append(Y64.charAt(buff[i] >> 2));
            out.append(Y64.charAt(((buff[i] << 4) & 0x30) | (buff[i + 1] >> 4)));
            out.append(Y64.charAt(((buff[i + 1] << 2) & 0x3c) | (buff[i + 2] >> 6)));
            out.append(Y64.charAt(buff[i + 2] & 0x3f));
        }
        int i = limit;
        switch(buff.length - i) {
            case 1:
                out.append(Y64.charAt(buff[i] >> 2));
                out.append(Y64.charAt(((buff[i] << 4) & 0x30)));
                out.append("--");
                break;
            case 2:
                out.append(Y64.charAt(buff[i] >> 2));
                out.append(Y64.charAt(((buff[i] << 4) & 0x30) | (buff[i + 1] >> 4)));
                out.append(Y64.charAt(((buff[i + 1] << 2) & 0x3c)));
                out.append("-");
                break;
        }
        return out.toString();
    }

    /**
     * Return the MD5 or a string and byte array.
     * 
     * @throws NoSuchAlgorithmException
     */
    public static final byte[] md5(String s) throws NoSuchAlgorithmException {
        return md5(s.getBytes());
    }

    public static final synchronized byte[] md5(byte[] buff) throws NoSuchAlgorithmException {
        MessageDigest md5Obj = MessageDigest.getInstance("MD5");
        md5Obj.reset();
        return md5Obj.digest(buff);
    }

    public static final byte[] md5Crypt(String k, String s) throws NoSuchAlgorithmException {
        return UnixCrypt.crypt(k, s).getBytes();
    }
}
