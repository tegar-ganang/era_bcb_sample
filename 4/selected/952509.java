package sti.installer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Utils {

    public static Process execute(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            return p;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static void copyDir(File in, File out) {
        File[] folder = in.listFiles();
        for (int i = 0; i < in.length(); i++) {
            if (folder[i].isDirectory()) {
                File newFolder = new File(out, folder[i].getName());
                newFolder.mkdir();
                copyDir(new File(in, folder[i].getName()), newFolder);
            } else {
                try {
                    copyFile(folder[i], new File(out, folder[i].getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String args[]) {
        copyDir(new File("J:\\Rapidsvn"), new File("J:\\test3"));
    }
}
