package aaronr.utils;

import java.io.*;
import java.nio.channels.*;

/**
 * @author AaronR
 */
public class FileUtils {

    public static void copyFile(File source, File dest) throws IOException {
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel from = null;
        FileChannel to = null;
        try {
            from = new FileInputStream(source).getChannel();
            to = new FileOutputStream(dest).getChannel();
            to.transferFrom(from, 0, from.size());
        } finally {
            if (from != null) {
                from.close();
            }
            if (to != null) {
                to.close();
            }
        }
    }

    public static void copyFile(InputStream source, OutputStream dest) throws IOException {
        byte[] buf = new byte[1024];
        int i1 = 0;
        while ((i1 = source.read(buf)) != -1) {
            dest.write(buf, 0, i1);
        }
    }

    public static void recursivelyDelete(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                recursivelyDelete(files[i]);
            }
        }
        file.delete();
    }
}
