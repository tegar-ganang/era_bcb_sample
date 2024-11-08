package com.ericdaugherty.mail.server.utils;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * This code is based partly on the workaround suggested at:
 * http://forums.sun.com/thread.jspa?threadID=439695&messageID=2917510
 * Chunking is always used on windows systems without regard to the CPU
 * architecture, the files being used are on a local drive or a mapped network
 * one, declared using UNC or not. Also, it is higly unlikely that a message
 * will be greater that 2GB, so there should be no problems using this facility.
 *
 * @author Andreas Kyrmegalos
 */
public class FileUtils {

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            if (System.getProperty("os.name").toUpperCase().indexOf("WIN") != -1) {
                int maxCount = (64 * 1024 * 1024) - (32 * 1024);
                long size = inChannel.size();
                long position = 0;
                while (position < size) {
                    position += inChannel.transferTo(position, maxCount, outChannel);
                }
            } else {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
