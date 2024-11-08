package org.monet.docservice.docprocessor.pdf.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.monet.docservice.core.util.StreamHelper;

public class PdfSignerUtils {

    static final String PLACE_HOLDER = "Monet placeholder for signature {4F4ACE05-A286-408e-8E7E-26FC2A05B07E}";

    public static byte[] getPlaceHolderArr(int maxLength) {
        byte[] placeHolder = new byte[maxLength / 2];
        System.arraycopy(PLACE_HOLDER.getBytes(), 0, placeHolder, 0, PLACE_HOLDER.getBytes().length);
        return placeHolder;
    }

    static final char[] NIBBLE_TO_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String byteArrayToHexString(byte[] bArr) {
        StringBuffer sb = new StringBuffer();
        if (bArr != null) {
            for (int i = 0; i < bArr.length; i++) {
                int b = bArr[i] & 0xff;
                try {
                    sb.append(NIBBLE_TO_CHAR[(b >>> 4) & 0x0f]);
                    sb.append(NIBBLE_TO_CHAR[b & 0x0f]);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                }
            }
            return (new String(sb));
        } else {
            return ("null");
        }
    }

    private static boolean matches(MappedByteBuffer bb, byte[] sought, int pos) {
        for (int j = 0; j < sought.length; ++j) {
            if (sought[j] != bb.get(pos + j)) {
                return false;
            }
        }
        return true;
    }

    private static void replace(MappedByteBuffer bb, byte[] sought, byte[] replacement, int pos) {
        for (int j = 0; j < sought.length; ++j) {
            byte b = (j < replacement.length) ? replacement[j] : (byte) ' ';
            bb.put(pos + j, b);
        }
    }

    private static void searchAndReplace(MappedByteBuffer bb, byte[] sought, byte[] replacement, int sz) {
        int replacementsCount = 0;
        for (int pos = 0; pos <= sz - sought.length; ++pos) {
            if (matches(bb, sought, pos)) {
                replace(bb, sought, replacement, pos);
                pos += sought.length - 1;
                ++replacementsCount;
            }
        }
    }

    private static void patch(File f, byte[] sought, byte[] replacement) throws IOException {
        RandomAccessFile raf = null;
        FileChannel fc = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
            int sz = (int) fc.size();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_WRITE, 0, sz);
            searchAndReplace(bb, sought, replacement, sz);
            bb.force();
        } finally {
            StreamHelper.close(fc);
            StreamHelper.close(raf);
        }
    }

    public static void replace(File file, byte[] sought, byte[] replacement) throws Exception {
        if (sought.length != replacement.length) {
            throw new Exception("sought length must match replacement length");
        }
        patch(file, sought, replacement);
    }
}
