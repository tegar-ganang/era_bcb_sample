package net.sf.keytabgui.model.utils;

import java.io.*;
import java.nio.channels.*;

public class FileUtils {

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileOutputStream fos = new FileOutputStream(out);
        FileChannel outChannel = fos.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
            fos.flush();
            fos.close();
        }
    }

    public static void main(String args[]) throws IOException {
        FileUtils.copyFile(new File(args[0]), new File(args[1]));
    }
}
