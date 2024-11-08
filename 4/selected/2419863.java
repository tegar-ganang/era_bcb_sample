package net.sf.jimo.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class FileUtil {

    public static void delTree(File file) {
        if (file.isDirectory()) {
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                File f = new File(file, files[i]);
                delTree(f);
            }
        }
        if (!file.delete()) {
            System.err.println("ERROR deleting " + file.getAbsolutePath());
        }
    }

    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        int b;
        while ((b = in.read()) != -1) out.write(b);
        out.flush();
        out.close();
    }
}
