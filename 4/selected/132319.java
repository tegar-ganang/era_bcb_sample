package com.oktiva.util;

import java.io.*;
import java.io.FileWriter;

/** File utility class.<p>
 * Original code from http://forum.java.sun.com
 * @version $Id: FileUtil.java,v 1.1 2005/01/17 20:11:19 itamarc Exp $
 */
public final class FileUtil {

    /**
	 * Allows cross-platform compatibility.
	 */
    public static final String lineFeed = System.getProperty("line.separator");

    /**
	 * This class shouldn't be instantiated.
	 */
    private FileUtil() {
    }

    /**
	 * Copy files and/or directories.
	 *
	 * @param src source file or directory
	 * @param dest destination file or directory
	 * @throws IOException if operation fails
	 */
    public static final void copy(File src, File dest) throws IOException {
        FileInputStream source = null;
        FileOutputStream destination = null;
        byte[] buffer;
        int bytes_read;
        if (!src.exists()) {
            throw new IOException("Source not found: " + src);
        }
        if (!src.canRead()) {
            throw new IOException("Source is unreadable: " + src);
        }
        if (src.isFile()) {
            if (!dest.exists()) {
                File parentdir = parent(dest);
                if (!parentdir.exists()) {
                    parentdir.mkdir();
                }
            } else if (dest.isDirectory()) {
                dest = new File(dest + File.separator + src);
            }
        } else if (src.isDirectory()) {
            if (dest.isFile()) {
                throw new IOException("Cannot copy directory " + src + " to file " + dest);
            }
            if (!dest.exists()) {
                dest.mkdir();
            }
        }
        if (src.isFile()) {
            try {
                source = new FileInputStream(src);
                destination = new FileOutputStream(dest);
                buffer = new byte[1024];
                while (true) {
                    bytes_read = source.read(buffer);
                    if (bytes_read == -1) {
                        break;
                    }
                    destination.write(buffer, 0, bytes_read);
                }
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e) {
                    }
                }
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else if (src.isDirectory()) {
            String targetfile, target, targetdest;
            String[] files = src.list();
            for (int i = 0; i < files.length; i++) {
                targetfile = files[i];
                target = src + File.separator + targetfile;
                targetdest = dest + File.separator + targetfile;
                if ((new File(target)).isDirectory()) {
                    copy(new File(target), new File(targetdest));
                } else {
                    try {
                        source = new FileInputStream(target);
                        destination = new FileOutputStream(targetdest);
                        buffer = new byte[1024];
                        while (true) {
                            bytes_read = source.read(buffer);
                            if (bytes_read == -1) {
                                break;
                            }
                            destination.write(buffer, 0, bytes_read);
                        }
                    } finally {
                        if (source != null) {
                            try {
                                source.close();
                            } catch (IOException e) {
                            }
                        }
                        if (destination != null) {
                            try {
                                destination.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    /**
	 * Deletes a file or a directory along with all files and directories
	 * it contains.
	 *
	 * @param file the file or directory to delete
	 * @throws IOException if operation fails
	 */
    public static final void deleteFile(File file) throws IOException {
        if (file == null) {
            throw new IOException("Parameter file can not be 'null'");
        }
        if (file.isDirectory()) {
            String[] dirListing = file.list();
            int len = dirListing.length;
            for (int i = 0; i < len; i++) {
                deleteFile(new File(dirListing[i]));
            }
        }
        if (!file.delete()) {
            throw new IOException("Could not delete: " + file.getCanonicalPath());
        }
    }

    /**
	 * File.getParent() can return null when the file is specified without
	 * a directory or is in the root directory. This method handles those
	 * cases.
	 *
	 * @param file the target file to analyze
	 * @return the parent directory as a file
	 */
    private static final File parent(File file) {
        String dirname = file.getParent();
        if (dirname == null) {
            if (file.isAbsolute()) {
                return new File(File.separator);
            } else {
                return new File(System.getProperty("user.dir"));
            }
        }
        return new File(dirname);
    }

    /**
	 * Returns a string containing the contents of a file.
	 *
	 * @param file the file to read
	 * @return the contents of the file
	 */
    public static final String readFile(File file) throws IOException {
        return readFile(new FileReader(file));
    }

    /**
	 * Returns a string containing the contents of a file.
	 *
	 * @param filename the name of the file
	 * @return the contents of the file
	 */
    public static final String readFile(String filename) throws IOException {
        return readFile(new FileReader(filename));
    }

    /**
	 * Returns a string containing the contents of a file.
	 *
	 * @param fileReader the FileReader to use
	 * @return the contents of the file
	 */
    public static final String readFile(FileReader fileReader) throws IOException {
        StringBuffer buf = new StringBuffer();
        int ch;
        BufferedReader reader = new BufferedReader(fileReader);
        while ((ch = reader.read()) != -1) {
            buf.append((char) ch);
        }
        return buf.toString();
    }

    /**
	 * Returns a string array containing the contents of a file (one line
	 * en each array item).
	 *
	 * @param file the file to read
	 * @return the contents of the file split in an array
	 */
    public static final String[] readFileAsArray(File file) throws IOException {
        String text = readFile(new FileReader(file));
        return text.split("\n");
    }

    /**
	 * Returns a string array containing the contents of a file (one line
	 * en each array item).
	 *
	 * @param filename the name of the file
	 * @return the contents of the file split in an array
	 */
    public static final String[] readFileAsArray(String filename) throws IOException {
        String text = readFile(filename);
        return text.split("\n");
    }

    /**
	 * Returns a string array containing the contents of a file (one line
	 * en each array item).
	 *
	 * @param fileReader the FileReader to use
	 * @return the contents of the file split in an array
	 */
    public static final String[] readFileAsArray(FileReader fileReader) throws IOException {
        String text = readFile(fileReader);
        return text.split("\n");
    }

    /**
	 * Write a string to a file.
	 *
	 * @param text The string to write to the file.
	 * @param file The file to write.
	 * @throws IOException If the write operation fails.
	 */
    public static final void writeFile(String text, File file) throws IOException {
        writeFile(text, new FileWriter(file));
    }

    /**
	 * Write a string to a file.
	 *
	 * @param text The string to write to the file.
	 * @param filename The name of the file to write.
	 * @throws IOException If the write operation fails.
	 */
    public static final void writeFile(String text, String filename) throws IOException {
        writeFile(text, new FileWriter(filename));
    }

    /**
	 * Write a string to a file.
	 *
	 * @param text The string to write to the file.
	 * @param fileWriter The FileWriter to use
	 * @throws IOException If the write operation fails.
	 */
    public static final void writeFile(String text, FileWriter fileWriter) throws IOException {
        PrintWriter out = new PrintWriter(fileWriter);
        out.write(text);
        out.close();
    }
}
