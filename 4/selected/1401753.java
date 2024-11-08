package uk.org.ogsadai.common.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * General file utilities.
 *
 * @author The OGSA-DAI Project Team.
 */
public class FileUtilities {

    /** Copyright notice. */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh, 2007-2012.";

    /**
     * Reads the content of a file and returns it as a string.
     * 
     * @param path
     *            the path of the file to read
     * @return content of the file
     * @throws IOException
     *             if there was an error opening or reading from the file
     */
    public static String readFileToString(String path) throws IOException {
        return readToString(new FileReader(path));
    }

    /**
     * Reads from a reader and returns the contents as a string.
     * 
     * @param reader
     *            reader to read from
     * @throws IOException
     *             if there was an error reading or writing the contents
     */
    public static String readToString(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        readAndWriteToWriter(reader, writer);
        return writer.toString();
    }

    /**
     * Reads from a reader and writes the contents to the given writer.
     * 
     * @param reader
     *            reader to read from
     * @param writer
     *            destination of contents
     * @throws IOException
     *             if there was an error reading or writing the contents
     */
    public static void readAndWriteToWriter(Reader reader, Writer writer) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        char[] buffer = new char[2048];
        int chars = 0;
        while ((chars = in.read(buffer)) >= 0) {
            writer.write(buffer, 0, chars);
        }
        in.close();
    }

    /**
     * Reads a character file on the specified path and writes the contents to
     * the given writer.
     * 
     * @param path
     *            path of the file whose contents to read
     * @param writer
     *            destination of contents
     * @throws IOException
     *             if there was an error opening or reading from the file
     */
    public static void readFileAndWriteToWriter(String path, Writer writer) throws IOException {
        readAndWriteToWriter(new FileReader(path), writer);
    }

    /**
     * Copy a file.
     * 
     * @param src
     *     Source file.
     * @param dest
     *     Destination file.
     * @throws FileNotFoundException
     *     If the source or destination cannot be found.
     * @throws IOException
     *     If any other problems arise.
     */
    public static void copyFile(File src, File dest) throws FileNotFoundException, IOException {
        InputStream inStr = null;
        try {
            inStr = new FileInputStream(src);
        } catch (java.io.FileNotFoundException e) {
            throw new FileNotFoundException(src);
        }
        OutputStream outStr = null;
        try {
            outStr = new FileOutputStream(dest);
        } catch (java.io.FileNotFoundException e) {
            inStr.close();
        }
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStr.read(buffer)) > 0) {
                outStr.write(buffer, 0, length);
            }
        } finally {
            inStr.close();
            outStr.close();
        }
    }

    /**
     * Copy files from one directory to another. This does not copy 
     * sub-directories.
     * 
     * @param src
     *     Source directory.
     * @param dest
     *     Destination directory.
     * @throws FileNotFoundException
     *     If the source cannot be found.
     * @throws IOException
     *     If any other problems arise.
     */
    public static void copyDirectory(File src, File dest) throws FileNotFoundException, IOException {
        copyFiles(src, dest, true);
    }

    /**
     * Copy files from one directory to another. This does not copy 
     * sub-directories.
     * 
     * @param src
     *     Source directory.
     * @param dest
     *     Destination directory.
     * @param replace
     *     If <code>true</code> then <code>dest</code> will be initially
     *     deleted and recreated. Else the original contents will be left.
     * @throws FileNotFoundException
     *     If the source or destination (if <code>replace</code> is set,
     *     cannot be found.
     * @throws IOException
     *     If any other problems arise.
     */
    public static void copyFiles(File src, File dest, boolean replace) throws FileNotFoundException, IOException {
        if (!src.exists()) {
            throw new FileNotFoundException(src);
        }
        if (replace) {
            if (dest.exists()) {
                deleteFile(dest);
            }
            dest.mkdir();
        } else {
            if (!dest.exists()) {
                throw new FileNotFoundException(dest);
            }
        }
        File[] srcFiles = src.listFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            File file = srcFiles[i];
            if (file.isFile()) {
                String fileName = file.getName();
                copyFile(file, new File(dest, fileName));
            }
        }
    }

    /**
     * Delete a file or directory and, if a directory, all its
     * descendants.
     * 
     * @param file
     *     File or directory to remove.
     */
    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i]);
                }
            } else {
                file.delete();
            }
        }
        file.delete();
    }
}
