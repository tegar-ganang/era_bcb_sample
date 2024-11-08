package com.intel.gpe.security;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;

/**
 * This class implements a hex decoder, decoding a string with hex-characters
 * into the binary form.
 * 
 * @author
 * @version $Id: Hex.java,v 1.1 2005/09/30 08:40:50 lukichev Exp $
 *  
 */
public class Hex {

    private static final char hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encode(byte[] dataStr) {
        StringWriter w = new StringWriter();
        for (int i = 0; i < dataStr.length; i++) {
            int b = dataStr[i];
            w.write(hex[((b >> 4) & 0xF)]);
            w.write(hex[((b >> 0) & 0xF)]);
        }
        return w.toString();
    }

    public static byte[] decode(String dataStr) {
        if ((dataStr.length() & 0x01) == 0x01) dataStr = new String(dataStr + "0");
        BigInteger cI = new BigInteger(dataStr, 16);
        byte[] data = cI.toByteArray();
        return data;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: HexStrToBin enc/dec <infileName> <outfilename>");
            System.exit(1);
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream in = new FileInputStream(args[1]);
            int len = 0;
            byte buf[] = new byte[1024];
            while ((len = in.read(buf)) > 0) os.write(buf, 0, len);
            in.close();
            os.close();
            byte[] data = null;
            if (args[0].equals("dec")) data = decode(os.toString()); else {
                String strData = encode(os.toByteArray());
                data = strData.getBytes();
            }
            FileOutputStream fos = new FileOutputStream(args[2]);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
