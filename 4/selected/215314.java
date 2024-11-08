package at.fhj.utils.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * @author Ilya Boyandin
 *
 * $Revision: 1.3 $
 */
public class FileUtils {

    public static String getExtension(String fileName) {
        final int dot = fileName.lastIndexOf('.');
        if (dot > -1) {
            return fileName.substring(dot + 1);
        }
        return "";
    }

    public static String cutOffExtension(String fileName) {
        final int dot = fileName.lastIndexOf('.');
        if (dot > -1) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }

    /**
   * Returns the file name WITHOUT the path but WITH extension  
   */
    public static String getFilename(String filePath) {
        if (filePath == null) {
            return null;
        }
        final int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (slash > -1) {
            filePath = filePath.substring(slash + 1);
        }
        return filePath;
    }

    /**
   * Returns the file name WITHOUT the path and WITHOUT extension 
   */
    public static String getFilenameOnly(String filePath) {
        if (filePath == null) {
            return null;
        }
        final int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        final int dot = filePath.lastIndexOf('.');
        if (slash > -1 && dot > slash) {
            filePath = filePath.substring(slash + 1, dot);
        } else if (slash > -1) {
            filePath = filePath.substring(slash + 1);
        } else if (dot > -1) {
            filePath = filePath.substring(0, dot);
        }
        return filePath;
    }

    public static void copy(String srcFile, String destFile) throws IOException {
        copy(new File(srcFile), new File(destFile));
    }

    public static void copy(File srcFile, File destFile) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            final byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) >= 0) {
                out.write(buf, 0, read);
            }
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ioe) {
            }
            try {
                if (out != null) out.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static String addSeparator(String path) {
        if (path.endsWith(File.separator)) {
            return path;
        } else {
            return path + File.separator;
        }
    }
}
