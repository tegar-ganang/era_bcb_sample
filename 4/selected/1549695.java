package org.shaitu.easyphoto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class FileUtil {

    /**
     * Get the extension of a file.
     */
    public static String getExtName(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * copy file 
     * @param sourceFile source file
     * @param destFile destination file
     */
    public static void copyFile(File sourceFile, File destFile) {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get working dir
     * @return working dir
     */
    public static String getWorkingDir() {
        return System.getProperty("user.dir") + System.getProperty("file.separator");
    }

    /**
     * get fileSize description
     * @param fileSize file size, unit byte
     * @return description, unit K
     */
    public static String getFileSizeDescription(long fileSize) {
        return String.valueOf(fileSize / 1024) + "K";
    }
}
