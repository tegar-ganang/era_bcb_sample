package com.ek.mitapp.util;

import java.io.*;
import java.util.Date;
import org.apache.log4j.Logger;
import com.ek.mitapp.*;

/**
 * TODO: Class description.
 * <br>
 * Id: $Id: FileUtils.java 1664 2006-04-11 14:38:53Z dhirwinjr $
 *
 * @author dhirwinjr
 */
public final class FileUtils {

    /**
	 * Define the root logger.
	 */
    private static Logger logger = Logger.getLogger(FileUtils.class.getName());

    private static enum FileExtensions {

        Excel(".xls"), Text(".txt");

        private String extension;

        /**
		 * 
		 * @param extension
		 */
        private FileExtensions(String extension) {
            this.extension = extension;
        }

        /**
		 * 
		 * @return
		 */
        public String getExtension() {
            return extension;
        }

        /**
		 * 
		 * @return
		 */
        public int getExtensionLength() {
            return extension.length();
        }

        /**
		 * @see java.lang.Object#toString()
		 */
        public String toString() {
            return getExtension();
        }
    }

    /**
	 * Non-instantiable class. 
	 */
    private FileUtils() {
    }

    /**
	 * Backup an existing Excel file.
	 * 
	 * @param fileToBackup
	 * @throws IOException
	 */
    public static void backupExcelFile(File fileToBackup) throws IOException {
        backupFile(fileToBackup, FileExtensions.Excel);
    }

    /**
	 * Back up an existing file given the type of extension.
	 * 
	 * @param fileToBackup
	 * @param extension
	 * @throws IOException
	 */
    public static void backupFile(File fileToBackup, FileExtensions extension) throws IOException {
        String originalDirectory = fileToBackup.getPath().substring(0, fileToBackup.getPath().lastIndexOf(AppSettings.SEPARATOR));
        String originalFilename = fileToBackup.getName();
        if (!originalFilename.endsWith(extension.getExtension())) abort("File doesn't end with an " + extension.getExtension() + " extension");
        String basename = originalFilename.substring(0, originalFilename.length() - extension.getExtensionLength());
        String backupFilename = basename + "-" + AppSettings.defaultDateFormat.format(new Date(System.currentTimeMillis())) + extension.getExtension();
        File backupFile = new File(originalDirectory + AppSettings.SEPARATOR + backupFilename);
        logger.debug("Backing up file:");
        logger.debug("   Original filename: " + fileToBackup.getPath());
        logger.debug("   Backup filename: " + backupFile.getPath());
        copy(fileToBackup, backupFile);
    }

    /**
	 * Copy one file to another.
	 * 
	 * @param from_file
	 * @param to_file
	 * @throws IOException
	 */
    public static void copy(File from_file, File to_file) throws IOException {
        if (!from_file.exists()) abort("FileCopy: no such source file: " + from_file.getName());
        if (!from_file.isFile()) abort("FileCopy: can't copy directory: " + from_file.getName());
        if (!from_file.canRead()) abort("FileCopy: source file is unreadable: " + from_file.getName());
        if (to_file.isDirectory()) to_file = new File(to_file, from_file.getName());
        if (to_file.exists()) {
            if (!to_file.canWrite()) abort("FileCopy: destination file is unwriteable: " + to_file.getName());
        } else {
            String parent = to_file.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) abort("FileCopy: destination directory doesn't exist: " + parent);
            if (dir.isFile()) abort("FileCopy: destination is not a directory: " + parent);
            if (!dir.canWrite()) abort("FileCopy: destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(from_file);
            to = new FileOutputStream(to_file);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytes_read);
            }
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static void checkFile(File file) throws IOException {
        if (!file.exists()) abort("File does not exist: " + file.getPath());
        if (!file.isFile()) abort("Filename does not point to a file");
        if (!file.canRead()) abort("Cannot read from file");
        if (file.length() <= 0) abort("Zero length file");
    }

    /**
	 * 
	 * @param fullPath
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
    public static String getDirectory(String fullPath) {
        int directoryIndex = fullPath.lastIndexOf(AppSettings.SEPARATOR);
        if (directoryIndex != -1) {
            return fullPath.substring(0, directoryIndex);
        } else {
            return null;
        }
    }

    /**
	 * 
	 * @param fullPath
	 * @return
	 */
    public static String getFilename(String fullPath) {
        int directoryIndex = fullPath.lastIndexOf(AppSettings.SEPARATOR);
        if (directoryIndex != -1) {
            return fullPath.substring(directoryIndex + 1, fullPath.length());
        } else {
            return fullPath;
        }
    }

    /**
	 * A convenience method to throw an exception
	 * 
	 * @param msg
	 * @throws IOException
	 */
    private static void abort(String msg) throws IOException {
        throw new IOException(msg);
    }
}
