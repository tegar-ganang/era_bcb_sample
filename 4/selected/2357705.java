package dk.syscall.yamsie.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility function for working with files and directories.
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void copyFile(File src, File dst) {
        try {
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[1024];
                int i = 0;
                while ((i = fis.read(buf)) != -1) fos.write(buf, 0, i);
            } catch (IOException e) {
                throw e;
            } finally {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            }
        } catch (IOException e) {
            logger.error("Error coping file from " + src + " to " + dst, e);
        }
    }

    /**
	 * Deleting a whole directory tree (including the directory itself).
	 * @param path directory
	 * @return true, if successful
	 */
    public static boolean deleteDirectory(File path) throws IOException {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    if (!files[i].delete()) {
                        return false;
                    }
                }
            }
        }
        return path.delete();
    }

    public static String getExtension(String filename) {
        int extIdx = filename.lastIndexOf('.');
        if (extIdx <= 0) {
            return filename;
        }
        return filename.substring(extIdx + 1);
    }
}
