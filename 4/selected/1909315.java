package org.oobench.ejb.common.deploy;

import java.io.*;

/**
 * File utilities for copying and removing files.
 * @author <a href="mailto:schween@snafu.de">Sven C. Koehler</a>
 */
public class FileUtils {

    private FileUtils() {
    }

    /**
     * Copy file.  Permissions are not copied, yet.
     * 
     * @param sourceFile source File
     * @param destFile destination File
     * @param overwrite whether destination should be overwritten
     *
     * @throws DirNotFoundException thrown, if destination directory does
     * not exist.
     * @throws FileNotFoundException thrown, if source file does not
     * exist.
     * @throws FileExistsAlreadyException thrown, if file exists already
     * and overwrite is false.
     */
    public static void copyFile(File sourceFile, File destFile, boolean overwrite) throws IOException, DirNotFoundException, FileNotFoundException, FileExistsAlreadyException {
        File destDir = new File(destFile.getParent());
        if (!destDir.exists()) {
            throw new DirNotFoundException(destDir.getAbsolutePath());
        }
        if (!sourceFile.exists()) {
            throw new FileNotFoundException(sourceFile.getAbsolutePath());
        }
        if (!overwrite && destFile.exists()) {
            throw new FileExistsAlreadyException(destFile.getAbsolutePath());
        }
        FileInputStream in = new FileInputStream(sourceFile);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[8 * 1024];
        int count = 0;
        do {
            out.write(buffer, 0, count);
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
        in.close();
        out.close();
    }

    /**
     * Create empty file, if it does not yet exist.  
     * 
     * @param file file to create
     */
    public static void createFile(File file) throws IOException {
        if (!file.exists()) {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(new byte[0]);
            fos.close();
        }
    }

    /**
     * Recursively delete directory.
     * 
     * @param dir directory to delete
     */
    public static void removeDir(File dir) throws IOException {
        String[] fileList = dir.list();
        for (int i = 0; i < fileList.length; i++) {
            String fileName = fileList[i];
            File f = new File(dir, fileName);
            if (f.isDirectory()) {
                removeDir(f);
            } else {
                if (!f.delete()) {
                    throw new IOException("Unable to delete file " + f.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Unable to delete directory " + dir.getAbsolutePath());
        }
    }
}
