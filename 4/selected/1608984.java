package org.mitre.rt.common.util.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Copys the source file to the destination file, and returns the destination
 * file.
 */
public class CopyFile {

    /**
	 * Copys the source file to the destination file, and returns the destination
	 * file.
         * 
	 */
    public static File copyFile(String sourceFile, String destFile) throws IOException {
        File src = new File(sourceFile);
        FileInputStream fis = new FileInputStream(src);
        File dest = new File(destFile);
        FileOutputStream fos = new FileOutputStream(dest);
        int val = -1;
        while ((val = fis.read()) != -1) {
            fos.write(val);
        }
        fos.flush();
        fos.close();
        fis.close();
        long lastModified = src.lastModified();
        dest.setLastModified(lastModified);
        return dest;
    }

    /**
         * Copy from one file to another. Allow caller to specify how to handle 
         * overwriting existing files.
         * @param fromFilePath The source file to copy
         * @param toFilePath the destination file to copy to
         * @param overwrite overwrite or not
         * @throws java.io.IOException thrown if an error occurs
         */
    public static void copyFile(String fromFilePath, String toFilePath, boolean overwrite) throws IOException {
        File fromFile = new File(fromFilePath);
        File toFile = new File(toFilePath);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFilePath);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFilePath);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFilePath);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!overwrite) {
                throw new IOException(toFilePath + " already exists!");
            }
            if (!toFile.canWrite()) {
                throw new IOException("FileCopy: destination file is unwriteable: " + toFilePath);
            }
            String parent = toFile.getParent();
            if (parent == null) {
                parent = System.getProperty("user.dir");
            }
            File dir = new File(parent);
            if (!dir.exists()) {
                throw new IOException("FileCopy: destination directory doesn't exist: " + parent);
            }
            if (dir.isFile()) {
                throw new IOException("FileCopy: destination is not a directory: " + parent);
            }
            if (!dir.canWrite()) {
                throw new IOException("FileCopy: destination directory is unwriteable: " + parent);
            }
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            long lastModified = fromFile.lastModified();
            toFile.setLastModified(lastModified);
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Copies the source file to the destination file, and returns the destination
	 * file.
	 */
    public static File copyFile(File sourceFile, String destFile) throws IOException {
        FileInputStream fis = new FileInputStream(sourceFile);
        File dest = new File(destFile);
        if (dest.exists()) dest.delete();
        FileOutputStream fos = new FileOutputStream(dest);
        int val = -1;
        while ((val = fis.read()) != -1) {
            fos.write(val);
        }
        fos.flush();
        fos.close();
        fis.close();
        long lastModified = sourceFile.lastModified();
        dest.setLastModified(lastModified);
        return dest;
    }
}
