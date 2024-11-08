package jtinymediav1.jtinymedia.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Administrator
 */
public class JTM_Util {

    public static void doFileCopy(File src, File dest) throws FileNotFoundException, IOException {
        RandomAccessFile source = new RandomAccessFile(src, "r");
        RandomAccessFile destination = new RandomAccessFile(dest, "rw");
        while (destination.length() < source.length()) {
            destination.write(source.read());
        }
        source.close();
        destination.close();
    }
}
