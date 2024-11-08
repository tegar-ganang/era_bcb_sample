package com.codechimp.jmtf.mtf;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author NBTCIA7
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MTFToolClass {

    public static int byteToShort(byte[] b) {
        return byteToInt(b);
    }

    public static int byteToInt(byte[] b) {
        int ret = 0;
        for (int i = b.length - 1; i >= 0; i--) {
            int tb = b[i];
            if (tb < 0) {
                tb = 255 - (Math.abs(tb) - 1);
            }
            ret += tb;
            if (i > 0) {
                ret <<= 8;
            }
        }
        return ret;
    }

    public static long byteToLong(byte[] b) {
        long ret = 0;
        for (int i = b.length - 1; i >= 0; i--) {
            int tb = b[i];
            if (tb < 0) {
                tb = 255 - (Math.abs(tb) - 1);
            }
            ret += tb;
            if (i > 0) {
                ret <<= 8;
            }
        }
        return ret;
    }

    public static String byteToString(byte[] b) {
        return MTFToolClass.byteToString(b, false);
    }

    public static String byteToString(byte[] b, boolean compressedBytes) {
        String str = "";
        try {
            if (compressedBytes) {
                for (int i = 0; i < b.length; i += 2) {
                    byte[] b2 = new byte[2];
                    b2[0] = b[i];
                    b2[1] = b[i + 1];
                    int iChar = MTFToolClass.byteToShort(b2);
                    str += (char) iChar;
                }
            } else {
                for (int i = 0; i < b.length; i++) {
                    str += (char) b[i];
                }
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            str = "";
        }
        return str;
    }

    public static String getBytesAsString(byte[] b) {
        String str = "";
        for (int i = 0; i < b.length; i++) {
            if (str.length() > 0) {
                str += " ";
            }
            str += Integer.toHexString((int) b[i]);
        }
        return str;
    }
}
