package huf.io;

import huf.data.Container;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * File-related utility methods.
 */
public class FileUtils {

    /** No instantiation. */
    private FileUtils() {
    }

    /**
	 * Copy file from source to destination.
	 *
	 * @param src source file
	 * @param dst destination file
	 * @throws java.io.FileNotFoundException when source file does not exist
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(File src, File dst) throws IOException {
        FileInputStream fIn = null;
        FileOutputStream fOut = null;
        FileChannel sIn = null;
        FileChannel sOut = null;
        try {
            fIn = new FileInputStream(src);
            try {
                fOut = new FileOutputStream(dst);
                try {
                    sIn = fIn.getChannel();
                    sOut = fOut.getChannel();
                    sOut.transferFrom(sIn, 0, sIn.size());
                } finally {
                    if (sIn != null) {
                        sIn.close();
                    }
                    if (sOut != null) {
                        sOut.close();
                    }
                }
            } finally {
                if (fOut != null) {
                    fOut.close();
                }
            }
        } finally {
            if (fIn != null) {
                fIn.close();
            }
        }
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source array
	 * @param out destination file
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(byte[] in, File out) throws IOException {
        copy(new ByteArrayInputStream(in), out);
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination file
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, File out) throws IOException {
        FileOutputStream os = new FileOutputStream(out);
        copy(in, os, 50000);
        os.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination file
	 * @param bufSize copy buffer size (in bytes)
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, File out, int bufSize) throws IOException {
        FileOutputStream os = new FileOutputStream(out);
        copy(in, os, bufSize);
        os.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination file
	 * @param buffer copy buffer
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, File out, byte[] buffer) throws IOException {
        FileOutputStream os = new FileOutputStream(out);
        copy(in, os, buffer);
        os.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source file
	 * @param out destination stream
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(File in, OutputStream out) throws IOException {
        InputStream is = new FileInputStream(in);
        copy(is, out, 50000);
        is.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source file
	 * @param out destination stream
	 * @param bufSize copy buffer size (in bytes)
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(File in, OutputStream out, int bufSize) throws IOException {
        InputStream is = new FileInputStream(in);
        copy(is, out, bufSize);
        is.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source file
	 * @param out destination stream
	 * @param buffer copy buffer
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(File in, OutputStream out, byte[] buffer) throws IOException {
        InputStream is = new FileInputStream(in);
        copy(is, out, buffer);
        is.close();
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination stream
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, 50000);
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination stream
	 * @param bufSize copy buffer size (in bytes)
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, OutputStream out, int bufSize) throws IOException {
        copy(in, out, new byte[bufSize]);
    }

    /**
	 * Copy data from source to destination.
	 *
	 * @param in source stream
	 * @param out destination stream
	 * @param buffer copy buffer
	 * @throws IOException when an error occured while copying
	 */
    public static void copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int i = in.read(buffer);
        while (i > 0) {
            out.write(buffer, 0, i);
            i = in.read(buffer);
        }
    }

    /**
	 * Read contents of a file into byte array.
	 *
	 * @param in file
	 * @return byte array with contents of a file
	 * @throws IOException when I/O error occurs
	 */
    public static byte[] read(File in) throws IOException {
        byte[] buf = new byte[(int) in.length()];
        FileInputStream fin = new FileInputStream(in);
        int idx = 0;
        int len = 0;
        while ((len = fin.read(buf, idx, buf.length - idx)) > 0) {
            idx += len;
        }
        fin.close();
        return buf;
    }

    /**
	 * Read contents of a stream into byte array.
	 *
	 * @param in file
	 * @return byte array with contents of a file
	 * @throws IOException when I/O error occurs
	 */
    public static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos);
        return baos.toByteArray();
    }

    public static void rmrf(String dirName) throws IOException {
        rmrf(new File(dirName));
    }

    public static void rmrf(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        checkDirExists(dir, true);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                rmrf(file);
            } else {
                if (!file.delete()) {
                    throw new IOException("Unable to remove: " + file.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Unable to remove: " + dir.getAbsolutePath());
        }
    }

    /**
	 * This is a case-insensitive method that returns real path to specified directory.
	 *
	 * @param path wanted path
	 * @return real path
	 * @throws IOException if changing to specified directory is impossible
	 */
    public static File caseInsensitiveDir(File baseDir, String path) throws IOException {
        File dir = baseDir;
        for (String pathElement : path.toLowerCase().replaceAll("\\\\", "/").split("/")) {
            boolean foundElement = false;
            for (String file : dir.list()) {
                if (file.toLowerCase().equals(pathElement)) {
                    dir = new File(dir, file);
                    foundElement = true;
                    break;
                }
            }
            if (!foundElement) {
                throw new IOException("Unable to find subdirectory of " + dir + ": " + pathElement);
            }
        }
        return dir;
    }

    /**
	 * Compute MD5 sum of specified file.
	 *
	 * @param file file
	 * @return MD5 sum
	 * @throws IOException when an error occured while computing MD5 sum
	 */
    public static byte[] md5(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] md5 = md5(in);
        in.close();
        return md5;
    }

    /**
	 * Compute MD5 sum of specified stream.
	 *
	 * @param in input stream
	 * @return MD5 sum
	 * @throws IOException when an error occured while computing MD5 sum
	 */
    public static byte[] md5(InputStream in) throws IOException {
        MD5InputStream md5in = new MD5InputStream(in);
        copy(md5in, new DevNullOutputStream());
        return md5in.getMD5();
    }

    /**
	 * Checks if two files are equal.
	 *
	 * @param file1 first file to test
	 * @param file2 second file to test
	 * @return <code>true</code> if two files are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(File file1, File file2) throws IOException {
        return isEqual(file1, file2, 50000);
    }

    /**
	 * Checks if two files are equal.
	 *
	 * @param file1 first file to test
	 * @param file2 second file to test
	 * @param bufSize helper buffers size
	 * @return <code>true</code> if two files are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(File file1, File file2, int bufSize) throws IOException {
        return isEqual(file1, file2, new byte[bufSize], new byte[bufSize]);
    }

    /**
	 * Checks if two files are equal.
	 *
	 * @param file1 first file to test
	 * @param file2 second file to test
	 * @param buffer1 first helper buffer
	 * @param buffer2 second helper buffer
	 * @return <code>true</code> if two files are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(File file1, File file2, byte[] buffer1, byte[] buffer2) throws IOException {
        InputStream is1 = new FileInputStream(file1);
        InputStream is2 = new FileInputStream(file2);
        return isEqual(new BufferedInputStream(is1), new BufferedInputStream(is2), buffer1, buffer2);
    }

    /**
	 * Checks if two streams are equal.
	 *
	 * @param stream1 first stream to test
	 * @param stream2 second stream to test
	 * @return <code>true</code> if two streams are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(InputStream stream1, InputStream stream2) throws IOException {
        return isEqual(stream1, stream2, 50000);
    }

    /**
	 * Checks if two streams are equal.
	 *
	 * @param stream1 first stream to test
	 * @param stream2 second stream to test
	 * @param bufSize helper buffers size
	 * @return <code>true</code> if two streams are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(InputStream stream1, InputStream stream2, int bufSize) throws IOException {
        return isEqual(stream1, stream2, new byte[bufSize], new byte[bufSize]);
    }

    /**
	 * Checks if two streams are equal.
	 *
	 * @param stream1 first stream to test
	 * @param stream2 second stream to test
	 * @param buffer1 first helper buffer
	 * @param buffer2 second helper buffer
	 * @return <code>true</code> if two streams are equal or <code>false</code> if they're not
	 * @throws IOException when an error occured while comparing
	 */
    public static boolean isEqual(InputStream stream1, InputStream stream2, byte[] buffer1, byte[] buffer2) throws IOException {
        int i = 0;
        int i0 = 0;
        int i1 = stream1.read(buffer1);
        int i2 = stream2.read(buffer2);
        while (i1 > 0 && i2 > 0) {
            i0 = i1 > i2 ? i2 : i1;
            for (i = 0; i < i0; i++) {
                if (buffer1[i] != buffer2[i]) {
                    return false;
                }
            }
            i1 = stream1.read(buffer1);
            i2 = stream1.read(buffer2);
        }
        return i1 <= 0 && i2 <= 0;
    }

    /**
	 * Universal code-saver method for checking validity of file name.
	 *
	 * @param name file name
	 * @param writable is file or directory expected to be writable
	 * @return <code>true</code> if file exists and is (optionally) writable, <code>false</code> otherwise
	 */
    public static boolean doesFileExists(String name, boolean writable) {
        try {
            checkFileExists(name, writable);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
	 * Universal code-saver method for checking validity of file name.
	 *
	 * @param name file name
	 * @param writable is file or directory expected to be writable
	 * @return File verified object
	 * @throws IOException when one of the performed checks fails
	 */
    public static File checkFileExists(String name, boolean writable) throws IOException {
        return checkExists(new File(name), false, writable);
    }

    /**
	 * Universal code-saver method for checking validity of file name.
	 *
	 * @param file file
	 * @param writable is file or directory expected to be writable
	 * @return <code>true</code> if file exists and is (optionally) writable, <code>false</code> otherwise
	 */
    public static boolean doesFileExists(File file, boolean writable) {
        try {
            checkFileExists(file, writable);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
	 * Universal code-saver method for checking validity of file name.
	 *
	 * @param file file
	 * @param writable is file or directory expected to be writable
	 * @return File verified object
	 * @throws IOException when one of the performed checks fails
	 */
    public static File checkFileExists(File file, boolean writable) throws IOException {
        return checkExists(file, false, writable);
    }

    /**
	 * Universal code-saver method for checking validity of directory name.
	 *
	 * @param name directory name
	 * @param writable is file or directory expected to be writable
	 * @return <code>true</code> if directory exists and is (optionally) writable, <code>false</code> otherwise
	 */
    public static boolean doesDirExists(String name, boolean writable) {
        try {
            checkDirExists(name, writable);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
	 * Universal code-saver method for checking validity of directory name.
	 *
	 * @param name directory name
	 * @param writable is file or directory expected to be writable
	 * @return File verified object
	 * @throws IOException when one of the performed checks fails
	 */
    public static File checkDirExists(String name, boolean writable) throws IOException {
        return checkExists(new File(name), true, writable);
    }

    /**
	 * Universal code-saver method for checking validity of directory name.
	 *
	 * @param dir directory
	 * @param writable is file or directory expected to be writable
	 * @return <code>true</code> if directory exists and is (optionally) writable, <code>false</code> otherwise
	 */
    public static boolean doesDirExist(File dir, boolean writable) {
        try {
            checkDirExists(dir, writable);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
	 * Universal code-saver method for checking validity of directory name.
	 *
	 * @param dir directory
	 * @param writable is file or directory expected to be writable
	 * @return File verified object
	 * @throws IOException when one of the performed checks fails
	 */
    public static File checkDirExists(File dir, boolean writable) throws IOException {
        return checkExists(dir, true, writable);
    }

    /**
	 * Universal code-saver method for checking validity of file and directory name.
	 *
	 * @param file file or directory
	 * @param isDirectory should it be a directory (<code>true</code>) or file (<code>false</code>)
	 * @param writable is file or directory expected to be writable
	 * @return <code>true</code> if file or directory exists and is (optionally) writable,
	 *        <code>false</code> otherwise
	 */
    public static boolean doesExist(File file, boolean isDirectory, boolean writable) {
        try {
            checkExists(file, isDirectory, writable);
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
	 * Universal code-saver method for checking validity of file and directory name.
	 *
	 * @param file file or directory
	 * @param isDirectory should it be a directory (<code>true</code>) or file (<code>false</code>)
	 * @param writable is file or directory expected to be writable
	 * @return File verified object
	 * @throws IOException when one of the performed checks fails
	 */
    public static File checkExists(File file, boolean isDirectory, boolean writable) throws IOException {
        if (file.isDirectory() != isDirectory) {
            throw new IOException("Not a " + (isDirectory ? "directory" : "regular file") + ": " + file.getAbsolutePath());
        }
        if (!file.exists()) {
            throw new IOException((isDirectory ? "Directory" : "File") + " doesn't exist: " + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new IOException("Unable to read " + (isDirectory ? "directory" : "file") + ": " + file.getAbsolutePath());
        }
        if (writable && !file.canWrite()) {
            throw new IOException("Unable to write to " + (isDirectory ? "directory" : "file") + ": " + file.getAbsolutePath());
        }
        return file;
    }

    public static File createFile(File file) throws IOException {
        if (file.exists()) {
            return checkFileExists(file, true);
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Unable to create directory: " + file.getParent());
        }
        boolean created = false;
        try {
            created = file.createNewFile();
        } catch (IOException ioe) {
            throw new IOException("Error while creating file: " + file, ioe);
        }
        if (!created) {
            throw new IOException("Unable to create file: " + file);
        }
        return file;
    }

    /**
	 * Convert file path to relative path from specified directory
	 *
	 * @param baseDir base directory
	 * @param file absolute path
	 * @return relative path
	 * @throws IOException when I/O error occurs
	 */
    public static String toRelativePath(File baseDir, File file) throws IOException {
        Container<File> baseDirPath = toPathList(baseDir.getCanonicalFile());
        Container<File> filePath = toPathList(file.getCanonicalFile());
        int shorterPathCount = baseDirPath.getSize() < filePath.getSize() ? baseDirPath.getSize() : filePath.getSize();
        int numCommonPathElements = 0;
        for (; numCommonPathElements < shorterPathCount && baseDirPath.getAt(numCommonPathElements).equals(filePath.getAt(numCommonPathElements)); numCommonPathElements++) {
        }
        StringBuilder buf = new StringBuilder();
        for (int i = numCommonPathElements; i < baseDirPath.getSize(); i++) {
            if (buf.length() != 0) {
                buf.append("/");
            }
            buf.append("..");
        }
        for (int i = numCommonPathElements; i < filePath.getSize() - 1; i++) {
            if (buf.length() != 0) {
                buf.append("/");
            }
            buf.append(filePath.getAt(i).getName());
        }
        if (baseDirPath.getSize() < filePath.getSize()) {
            if (buf.length() != 0) {
                buf.append("/");
            }
            buf.append(filePath.getAt(filePath.getSize() - 1).getName());
        }
        return buf.toString();
    }

    private static Container<File> toPathList(File f) {
        Container<File> path = new Container<File>();
        File file = f;
        do {
            path.addAt(0, file);
        } while ((file = file.getParentFile()) != null);
        return path;
    }

    /**
	 * Convert arbitrary name to valid file name.
	 *
	 * @param name
	 * @return filesystem-compatible name
	 */
    public static String toFileName(String name) {
        char[] ch = name.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (!Character.isLetterOrDigit(ch[i])) {
                ch[i] = '_';
            }
        }
        return new String(ch);
    }

    /**
	 * Check if specified file or directory is inside or inside one of sub-directories of specified directory.
	 *
	 * @param parent parent directory
	 * @param child checked file
	 * @return <code>true</code> if <code>child</code> is inside <code>parent</code> directory,
	 * 		<code>false</code> otherwise
	 * @throws IOException when a filesystem operation fails
	 */
    public static boolean isInside(File parent, File child) throws IOException {
        if (parent == null) {
            throw new IllegalArgumentException("Parent may not be null");
        }
        if (child == null) {
            throw new IllegalArgumentException("Child may not be null");
        }
        if (parent.getPath().length() == 0) {
            throw new IllegalArgumentException("Parent may not be empty pathname");
        }
        if (child.getPath().length() == 0) {
            throw new IllegalArgumentException("Child may not be empty pathname");
        }
        return child.getCanonicalFile().getPath().indexOf(parent.getCanonicalFile().getPath()) == 0;
    }
}
