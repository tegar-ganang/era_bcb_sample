package com.objectwave.tools;

import java.io.*;

public class DumpHex {

    /**
     */
    public static void main(String[] args) {
        try {
            FileInputStream fin = new FileInputStream(args[0]);
            BufferedInputStream bin = new BufferedInputStream(fin);
            byte[] bytes = new byte[1024];
            int read;
            Writer writer = new PrintWriter(System.out);
            while ((read = bin.read(bytes)) > -1) hexDump(bytes, read, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    *  This method will print a hex dump of the given byte array to the given
    *  output stream.  Each line of the output will be 2-digit hex numbers,
    *  separated by single spaces, followed by the characters corresponding to
    *  those hex numbers, or a '.' if the given character is unprintable.  Each of
    *  these numbers will correspond to a byte of the byte array.
    *
    *  @author Steve Sinclair
    *  @param bytes the byte array to write
    *  @param writer the destination for the output.
    *  @exception java.io.IOException thrown if there's an error writing strings to the writer.
    */
    public static void hexDump(final byte[] bytes, int read, final java.io.Writer writer) throws java.io.IOException {
        final int width = 16;
        for (int i = 0; i < read; i += width) {
            int limit = (i + width > read) ? read - i : width;
            int j;
            StringBuffer literals = new StringBuffer(width);
            StringBuffer hex = new StringBuffer(width * 3);
            for (j = 0; j < limit; ++j) {
                int aByte = bytes[i + j];
                if (aByte < 0) aByte = 0xff + aByte + 1;
                if (aByte < 0x10) hex.append('0');
                hex.append(Integer.toHexString(aByte));
                hex.append(' ');
                if (aByte >= 32 && aByte < 128) literals.append((char) aByte); else literals.append('.');
            }
            for (; j < width; ++j) {
                literals.append(" ");
                hex.append("-- ");
            }
            hex.append(' ');
            hex.append(literals);
            hex.append('\n');
            writer.write(hex.toString());
        }
    }
}
