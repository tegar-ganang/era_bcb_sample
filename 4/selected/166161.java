package com.makeabyte.jhosting.server.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileSystem {

    private static Log log = LogFactory.getLog(FileSystem.class);

    /**
	    * Recursively copy a folder or a single file.
	    * 
	    * @param src The copy source
	    * @param dest The copy destination
	    */
    public static void copy(File src, File dest) throws IOException {
        log.info("Copying " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        if (!src.exists()) throw new IOException("File not found: " + src.getAbsolutePath());
        if (!src.canRead()) throw new IOException("Source not readable: " + src.getAbsolutePath());
        if (src.isDirectory()) {
            if (!dest.exists()) if (!dest.mkdirs()) throw new IOException("Could not create direcotry: " + dest.getAbsolutePath());
            String children[] = src.list();
            for (String child : children) {
                File src1 = new File(src, child);
                File dst1 = new File(dest, child);
                copy(src1, dst1);
            }
        } else {
            FileInputStream fin = null;
            FileOutputStream fout = null;
            byte[] buffer = new byte[4096];
            int bytesRead;
            fin = new FileInputStream(src);
            fout = new FileOutputStream(dest);
            while ((bytesRead = fin.read(buffer)) >= 0) fout.write(buffer, 0, bytesRead);
            if (fin != null) {
                fin.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }

    /**
	    * Deletes the specified file. If the specified file is a directory
	    * then a recursive delete operation is performed.
	    * 
	    * @param file The file or directory to delete.
	    */
    public static void delete(File file) throws IOException {
        log.info("Deleting file: " + file.getAbsolutePath());
        if (!file.canWrite()) {
            String error = new StringBuilder("File not writable: ").append(file.getAbsolutePath()).toString();
            throw new IOException(error);
        }
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                File f = new File(file, child);
                if (f.isDirectory()) delete(f); else if (!f.delete()) log.error("Error deleting file: " + file.getAbsolutePath());
            }
        }
        if (!file.delete()) log.error("Error deleting file: " + file.getAbsolutePath());
    }

    /**
	    * Walks the given directory backwards looking for empty directories to delete.
	    * 
	    * @param directory The directory to clean 
	    * @param topLevel Specifies a top level directory, which if reached, aborts the clean operation
	    */
    public static void clean(File directory, File topLevel) {
        log.info("Cleaning directory " + directory.getAbsolutePath());
        if (directory.getAbsoluteFile().equals(topLevel.getAbsoluteFile())) return;
        if (directory.isDirectory()) {
            String[] children = directory.list();
            if (children.length > 0) return;
            if (!directory.delete()) log.error("Error cleaning classpath. Could not delete " + directory.getAbsolutePath());
            clean(new File(directory.getParent()), topLevel);
        }
    }

    /**
	    * Read a file as a byte array.
	    * 
	    * @param file The fully qualified file system path to the class
	    * @return The byte array
	    * @throws IOException
	    */
    public static byte[] getBytes(File file) throws IOException {
        FileInputStream fi = new FileInputStream(file);
        byte[] result = new byte[fi.available()];
        fi.read(result);
        return result;
    }

    /**
	    * Write a byte array to disk
	    * 
	    * @param file The file to store the byte array as
	    * @param source The source/bytes to write
	    * @throws IOException
	    */
    public static void writeBytes(File file, byte[] source) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(source);
        fos.close();
    }
}
