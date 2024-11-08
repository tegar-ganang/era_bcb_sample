package org.pentaho.PentahoAdmin.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static void copyFile(String input, String output) {
        try {
            FileChannel srcChannel = new FileInputStream("srcFilename").getChannel();
            FileChannel dstChannel = new FileOutputStream("dstFilename").getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
        }
    }
}
