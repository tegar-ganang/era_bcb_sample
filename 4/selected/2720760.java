package com.itbs.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Copies files.
 * With minor modifications from original.
 * @author http://www.java2s.com/Code/Java/File-Input-Output/Copyingafileusingchannelsandbuffers.htm
 * @since Mar 25, 2008 5:14:38 PM
 */
public class FileCopy {

    private static final int BSIZE = 1024 * 64;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("arguments: sourcefile destfile");
            System.exit(1);
        }
        copyFile(args[0], args[1], false);
    }

    public static void copyFile(String from, String to, boolean append) throws IOException {
        FileChannel in = new FileInputStream(from).getChannel();
        FileChannel out = new FileOutputStream(to, append).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
        while (in.read(buffer) != -1) {
            buffer.flip();
            out.write(buffer);
            buffer.clear();
        }
    }
}
