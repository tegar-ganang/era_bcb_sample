package com.gever.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

public class Codes {

    public Codes() {
    }

    public static char[] encode(byte abyte0[]) {
        char ac[] = new char[((abyte0.length + 2) / 3) * 4];
        int i = 0;
        for (int j = 0; i < abyte0.length; j += 4) {
            boolean flag = false;
            boolean flag1 = false;
            int k = 0xff & abyte0[i];
            k <<= 8;
            if (i + 1 < abyte0.length) {
                k |= 0xff & abyte0[i + 1];
                flag1 = true;
            }
            k <<= 8;
            if (i + 2 < abyte0.length) {
                k |= 0xff & abyte0[i + 2];
                flag = true;
            }
            ac[j + 3] = alphabet[flag ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 2] = alphabet[flag1 ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 1] = alphabet[k & 0x3f];
            k >>= 6;
            ac[j + 0] = alphabet[k & 0x3f];
            i += 3;
        }
        return ac;
    }

    public static byte[] decode(char ac[]) {
        int i = ac.length;
        for (int j = 0; j < ac.length; j++) if (ac[j] > '\377' || codes[ac[j]] < 0) i--;
        int k = (i / 4) * 3;
        if (i % 4 == 3) k += 2;
        if (i % 4 == 2) k++;
        byte abyte0[] = new byte[k];
        int l = 0;
        int i1 = 0;
        int j1 = 0;
        for (int k1 = 0; k1 < ac.length; k1++) {
            byte byte0 = ac[k1] <= '\377' ? codes[ac[k1]] : -1;
            if (byte0 >= 0) {
                i1 <<= 6;
                l += 6;
                i1 |= byte0;
                if (l >= 8) {
                    l -= 8;
                    abyte0[j1++] = (byte) (i1 >> l & 0xff);
                }
            }
        }
        if (j1 != abyte0.length) throw new Error("Miscalculated data length (wrote " + j1 + " instead of " + abyte0.length + ")"); else return abyte0;
    }

    public static void doEncode(String s) {
        File file = new File(s);
        if (!file.exists()) {
            System.exit(0);
        } else {
            byte abyte0[] = readBytes(file);
            char ac[] = encode(abyte0);
            writeChars(file, ac);
        }
    }

    public static void doDecode(String s) {
        File file = new File(s);
        if (!file.exists()) {
            System.exit(0);
        } else {
            char ac[] = readChars(file);
            byte abyte0[] = decode(ac);
            writeBytes(file, abyte0);
        }
    }

    private static byte[] readBytes(File file) {
        ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
        try {
            FileInputStream fileinputstream = new FileInputStream(file);
            BufferedInputStream bufferedinputstream = new BufferedInputStream(fileinputstream);
            int i = 0;
            byte abyte0[] = new byte[16384];
            while ((i = bufferedinputstream.read(abyte0)) != -1) if (i > 0) bytearrayoutputstream.write(abyte0, 0, i);
            bufferedinputstream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return bytearrayoutputstream.toByteArray();
    }

    private static char[] readChars(File file) {
        CharArrayWriter chararraywriter = new CharArrayWriter();
        try {
            FileReader filereader = new FileReader(file);
            BufferedReader bufferedreader = new BufferedReader(filereader);
            int i = 0;
            char ac[] = new char[16384];
            while ((i = bufferedreader.read(ac)) != -1) if (i > 0) chararraywriter.write(ac, 0, i);
            bufferedreader.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return chararraywriter.toCharArray();
    }

    private static void writeBytes(File file, byte abyte0[]) {
        try {
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            BufferedOutputStream bufferedoutputstream = new BufferedOutputStream(fileoutputstream);
            bufferedoutputstream.write(abyte0);
            bufferedoutputstream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void writeChars(File file, char ac[]) {
        try {
            FileWriter filewriter = new FileWriter(file);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            bufferedwriter.write(ac);
            bufferedwriter.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static char alphabet[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();

    private static byte codes[];

    static {
        codes = new byte[256];
        for (int i = 0; i < 256; i++) {
            codes[i] = -1;
        }
        for (int j = 65; j <= 90; j++) {
            codes[j] = (byte) (j - 65);
        }
        for (int k = 97; k <= 122; k++) {
            codes[k] = (byte) ((26 + k) - 97);
        }
        for (int l = 48; l <= 57; l++) {
            codes[l] = (byte) ((52 + l) - 48);
        }
        codes[43] = 62;
        codes[47] = 63;
    }
}
