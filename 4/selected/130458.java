package com.lewisshell.helpyourself.util;

import java.io.*;
import java.nio.channels.*;

/**
 * @author RichardL
 */
public class FileUtil {

    public static void writeFile(File inFile, OutputStream out, int maxBufferSize) throws IOException {
        int size = (int) inFile.length();
        InputStream in = new FileInputStream(inFile);
        byte[] buf = new byte[size < maxBufferSize ? (int) size : maxBufferSize];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
    }

    public static void copy(File src, File dst) throws IOException {
        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel dstChannel = new FileOutputStream(dst).getChannel();
        try {
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            try {
                srcChannel.close();
            } finally {
                dstChannel.close();
            }
        }
    }

    public static void copyToDir(File src, File dir) throws IOException {
        copy(src, new File(dir, src.getName()));
    }
}
