package com.protomatter.util;

import java.io.*;

/**
 *  A binary data printer.  This is basically a pretty-printer for
 *  binary data.
 */
public class ByteDisplay {

    private static String zero_String = "0";

    private static String dot_String = ".";

    private static String space_String = " ";

    private static String twoSpace_String = "  ";

    private static String threeSpace_String = "   ";

    private static String slashN_String = "\n";

    private static String header = "offset  hex data                                          string data\n";

    /**
   *  Display the given byte.
   */
    public static String displayBytes(int b) {
        byte[] thebyte = new byte[1];
        thebyte[0] = (byte) b;
        return displayBytes(thebyte);
    }

    /**
   *  Display the given byte array.
   */
    public static String displayBytes(byte[] b) {
        return displayBytes(b, 0, b.length);
    }

    /**
   *  Display the given byte array, starting at the given offset
   *  and ending after the given length.
   */
    public static String displayBytes(byte[] b, int off, int len) {
        StringBuffer buf = new StringBuffer(2048);
        int end = off + len;
        buf.append(header);
        int i, j, c;
        for (i = off; i < end; i += 16) {
            if (i < 10000) buf.append(zero_String);
            if (i < 1000) buf.append(zero_String);
            if (i < 100) buf.append(zero_String);
            if (i < 10) buf.append(zero_String);
            buf.append(String.valueOf(i));
            buf.append(threeSpace_String);
            for (j = i; j < end && j < i + 16; j++) {
                c = b[j] & 0xff;
                if (c < 16) buf.append(zero_String);
                buf.append(Integer.toHexString(c));
                buf.append(space_String);
            }
            if (j == end) {
                while (j++ < i + 16) buf.append(threeSpace_String);
            }
            buf.append(twoSpace_String);
            for (j = i; j < end && j < i + 16; j++) {
                c = b[j] & 0xff;
                if (c < 32 || c > 127) buf.append(dot_String); else buf.append((char) c);
            }
            buf.append(slashN_String);
        }
        return buf.toString();
    }

    /**
   *  Display a file's contents.  Takes the first command-line
   *  argument as the name of the file to display.
   */
    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: ByteDisplay filename");
            System.exit(0);
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileInputStream in = new FileInputStream(new File(args[0]));
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = in.read(buffer)) != -1) bytes.write(buffer, 0, read);
            System.out.println("Read " + bytes.size() + " bytes from " + args[0]);
            System.out.println(displayBytes(bytes.toByteArray()));
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
