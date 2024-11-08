package ru.ipo.dces.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Илья
 * Date: 18.12.2008
 * Time: 20:30:01
 */
public class FileSystemUtils {

    private FileSystemUtils() {
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static void ensureFileHasPath(File file) {
        file.getParentFile().mkdirs();
    }

    public static boolean copyFile(File from, File tu) {
        final int BUFFER_SIZE = 4096;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            FileInputStream in = new FileInputStream(from);
            FileOutputStream out = new FileOutputStream(tu);
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
