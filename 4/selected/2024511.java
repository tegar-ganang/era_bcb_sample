package net.sf.euphony.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.sf.euphony.Log;

public class TransferFile {

    public static void transfer(File src, File dest, boolean removeSrc) throws FileNotFoundException, IOException {
        Log.warning("source: " + src);
        Log.warning("dest: " + dest);
        if (!src.canRead()) {
            throw new IOException("can not read src file: " + src);
        }
        if (!dest.getParentFile().isDirectory()) {
            if (!dest.getParentFile().mkdirs()) {
                throw new IOException("failed to make directories: " + dest.getParent());
            }
        }
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        FileChannel fcin = fis.getChannel();
        FileChannel fcout = fos.getChannel();
        Log.warning("starting transfer from position " + fcin.position() + " to size " + fcin.size());
        fcout.transferFrom(fcin, 0, fcin.size());
        Log.warning("closing streams and channels");
        fcin.close();
        fcout.close();
        fis.close();
        fos.close();
        if (removeSrc) {
            Log.warning("deleting file " + src);
            src.delete();
        }
    }

    public static void transfer(File src, File dest) throws FileNotFoundException, IOException {
        transfer(src, dest, false);
    }

    public static void transfer(String src, String dest, boolean removeSrc) throws FileNotFoundException, IOException {
        transfer(new File(src), new File(dest), removeSrc);
    }

    public static void transfer(String src, String dest) throws FileNotFoundException, IOException {
        transfer(src, dest, false);
    }

    public static void main(String[] args) {
        try {
            transfer(args[0], args[1], false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
