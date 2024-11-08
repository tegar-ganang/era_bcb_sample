package uk.co.platosys.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains static methods for a handful of file utilities that are
 * (probably for good reason)  missing from the java.io package.
 * @author edward
 */
public class FileTools {

    /**
     * This method checks to see whether a file is usable as
     * a writable folder, typically as a file upload location. If the folder
     * doesn't exist, it tries to create it (and any parents).
     * @param folder
     * @return
     */
    public static boolean mkfolder(File folder) {
        if (folder.exists()) {
            if (folder.canWrite()) {
                return true;
            } else {
                return false;
            }
        } else if (folder.mkdirs()) {
            if (folder.canWrite()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
    * Recursively deletes files and directories and their contents (equivalent to rm -r )
    * (assumes no permission issues, doesn't trap them yet);
    */
    public static void delete(File file) {
        if (!file.isDirectory()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                delete(files[i]);
            }
            file.delete();
        }
    }

    /**this removes spaces and any funny characters from the supplied string, but keeps dots and underscores
     * 
     * handy to process strings to make them more useful as cross-platform filenames  
     * 
     * @param string
     * @return
     */
    public static String removeFunnyCharacters(String string) {
        StringBuffer buffer = new StringBuffer();
        String dotString = "._";
        char dot = dotString.charAt(0);
        char us = dotString.charAt(1);
        for (int i = 0; i < string.length(); i++) {
            char x = string.charAt(i);
            if (Character.isLetterOrDigit(x)) {
                buffer.append(x);
            }
            if (x == dot) {
                buffer.append(x);
            }
            if (x == us) {
                buffer.append(x);
            }
        }
        return new String(buffer);
    }

    /**
         * simple file copy utility
         * @param fromFile
         * @param toFile
         * @throws java.io.IOException
         */
    public static void copy(File fromFile, File toFile) throws IOException {
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFile.getAbsolutePath());
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFile.getAbsolutePath());
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFile.getAbsolutePath());
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        String parent = toFile.getParent();
        if (parent == null) parent = System.getProperty("user.dir");
        File dir = new File(parent);
        if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
        if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
        if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
            }
        }
    }
}
