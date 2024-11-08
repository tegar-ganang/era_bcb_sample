package org.jpxx.commons.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * 
 * @author Jun Li lijun@jpxx.org (http://www.jpxx.org)
 * @version 1.0.0 $ org.jpxx.common.util.FileToolkit.java $ $ Date:
 *          2009-5-30 $
 */
public class FileUtils {

    /**
     * remove file or recursive remove folder
     * 
     * @param f
     *            file or folder to remove
     */
    public static void recursiveRm(File f) {
        if (f.isDirectory()) {
            String[] filenames = f.list();
            for (int i = 0; i < filenames.length; i++) recursiveRm(new File(f, filenames[i]));
            f.delete();
        } else {
            f.delete();
        }
    }

    /**
     * create all missing folders and return true, if folder on success
     * 
     * @param f
     *            - folder to create
     */
    public static boolean mkDirs(File f) {
        boolean ok = false;
        if (f.isDirectory()) ok = true; else if (f.exists()) ok = false; else {
            ok = f.mkdirs();
        }
        return ok;
    }

    /**
     * writes a stream into a file. Existing files will be overwritten .
     * 
     * @param outputFile
     *            - target file to be created
     * @param in
     *            - stream to be written to file, Stream will be closed.
     * @return <code>true</code> on success
     */
    public static boolean writeToFile(File outputFile, InputStream in) {
        return writeToFile(outputFile, in, true);
    }

    /**
     * writes a stream into a file. Existing files will be overwritten .
     * 
     * @param outputFile
     *            - target file to be created
     * @param in
     *            - stream to be written to file
     * @param closeIn
     *            if true, in-Stream will be closed
     * @return <code>true</code> on success
     */
    public static boolean writeToFile(File outputFile, InputStream in, boolean closeIn) {
        boolean ok = false;
        OutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int cnt = 0;
            if (in != null) cnt = in.read(buffer);
            while (cnt > 0) {
                out.write(buffer, 0, cnt);
                cnt = in.read(buffer);
            }
            ok = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (closeIn && (in != null)) try {
                in.close();
            } catch (IOException e) {
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
            }
        }
        return ok;
    }

    /**
     * create all missing parent dirs
     * 
     * @param fileName
     * @return true on success
     */
    public static boolean mkParentDirs(String fileName) {
        boolean ok = false;
        File f = new File(fileName);
        File parent = f.getParentFile();
        if (parent == null) ok = true; else ok = parent.mkdirs();
        return ok;
    }

    /**
     * @param file
     * @return content of file (in default encodeing) or null on error
     */
    public static String readContent(File file) {
        String result = null;
        try {
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(buffer);
            in.close();
            result = new String(buffer);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return result;
    }

    /**
     * read stream into a string, the stream is closed.
     * 
     * @param in
     * @return content of stream as string (in default encoding) or null on
     *         error
     */
    public static String readContent(InputStream in) {
        String result = null;
        byte[] buffer = new byte[4096];
        try {
            StringBuffer sbuf = new StringBuffer();
            int cnt = in.read(buffer);
            while (cnt > 0) {
                String str = new String(buffer, 0, cnt);
                sbuf.append(str);
                cnt = in.read(buffer);
            }
            result = sbuf.toString();
        } catch (IOException e) {
        }
        try {
            in.close();
        } catch (IOException e1) {
        }
        return result;
    }

    /**
     * @param conflictOutputFile
     * @param newManifest
     */
    public static void writeToFile(File outputFile, String content) {
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        writeToFile(outputFile, stream);
    }

    /**
     * List all files in a directory. In UNIX, directory is also a file.
     * 
     * @param path
     * @return
     */
    public static List<String> listAllFiles(String path) {
        list = new ArrayList<String>();
        listAllFile(path);
        return list;
    }

    private static List<String> list = null;

    private static synchronized void listAllFile(String path) {
        if (list == null) {
            list = new ArrayList<String>();
        }
        File file = new File(path);
        String[] array = null;
        String sTemp = "";
        if (!file.isDirectory()) {
            return;
        }
        array = file.list();
        if (array.length > 0) {
            for (int i = 0; i < array.length; i++) {
                sTemp = path + array[i];
                sTemp = sTemp.replace('\\', '/');
                file = new File(sTemp);
                if (file.getName().startsWith(".")) {
                    continue;
                }
                if (file.isDirectory()) {
                    list.add(sTemp + "/");
                    listAllFile(sTemp + "/");
                } else {
                    list.add(sTemp);
                }
            }
        }
    }

    public static void delete(String file) throws IOException {
        File f = new File(file);
        if (f.isDirectory()) {
            deleteDirectory(f);
        } else {
            f.delete();
        }
    }

    /**
     * Deletes a directory recursively.
     * 
     * @param directory
     *            directory to delete
     * @throws IOException
     *             in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Cleans a directory without deleting it.
     * 
     * @param directory
     *            directory to clean
     * @throws IOException
     *             in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }
        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of " + directory);
        }
        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }
        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all
     * sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li>
     * </ul>
     * 
     * @param file
     *            file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException
     *             if the directory is <code>null</code>
     * @throws FileNotFoundException
     *             if the file was not found
     * @throws IOException
     *             in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    public static void copy(String resDir, String destDir) throws IOException {
        File dir = new File(resDir);
        File resFiles[] = dir.listFiles();
        if (resFiles == null) {
            return;
        }
        for (int i = 0; i < resFiles.length; i++) {
            File f = resFiles[i];
            if (f.isFile()) {
                copyFile(f.getAbsolutePath(), destDir);
            } else {
                String destF = destDir + File.separator + f.getName();
                if (f.getName().startsWith(".")) {
                    continue;
                }
                File temp = new File(destF);
                if (!temp.exists()) {
                    temp.mkdirs();
                }
                copy(f.getAbsolutePath(), destF);
            }
        }
    }

    public static void copyFile(String srcFile, String destDir) throws IOException {
        File f = new File(srcFile);
        if (!f.exists()) {
            return;
        }
        copyFileToDirectory(f, new File(destDir));
    }

    /**
     * Copies a file to a directory preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to a file of
     * the same name in the specified destination directory. The destination
     * directory is created if it does not exist. If the destination file
     * exists, then this method will overwrite it.
     * 
     * @param srcFile
     *            an existing file to copy, must not be <code>null</code>
     * @param destDir
     *            the directory to place the copy in, must not be
     *            <code>null</code>
     * 
     * @throws NullPointerException
     *             if source or destination is null
     * @throws IOException
     *             if source or destination is invalid
     * @throws IOException
     *             if an IO error occurs during copying
     * @see #copyFile(File, File, boolean)
     */
    public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
        copyFileToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies a file to a directory optionally preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to a file of
     * the same name in the specified destination directory. The destination
     * directory is created if it does not exist. If the destination file
     * exists, then this method will overwrite it.
     * 
     * @param srcFile
     *            an existing file to copy, must not be <code>null</code>
     * @param destDir
     *            the directory to place the copy in, must not be
     *            <code>null</code>
     * @param preserveFileDate
     *            true if the file date of the copy should be the same as the
     *            original
     * 
     * @throws NullPointerException
     *             if source or destination is <code>null</code>
     * @throws IOException
     *             if source or destination is invalid
     * @throws IOException
     *             if an IO error occurs during copying
     * @see #copyFile(File, File, boolean)
     * @since Commons IO 1.3
     */
    public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate) throws IOException {
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (destDir.exists() && destDir.isDirectory() == false) {
            throw new IllegalArgumentException("Destination '" + destDir + "' is not a directory");
        }
        copyFile(srcFile, new File(destDir, srcFile.getName()), preserveFileDate);
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the
     * specified destination file. The directory holding the destination file is
     * created if it does not exist. If the destination file exists, then this
     * method will overwrite it.
     * 
     * @param srcFile
     *            an existing file to copy, must not be <code>null</code>
     * @param destFile
     *            the new file, must not be <code>null</code>
     * 
     * @throws NullPointerException
     *             if source or destination is <code>null</code>
     * @throws IOException
     *             if source or destination is invalid
     * @throws IOException
     *             if an IO error occurs during copying
     * @see #copyFileToDirectory(File, File)
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        copyFile(srcFile, destFile, true);
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the
     * specified destination file. The directory holding the destination file is
     * created if it does not exist. If the destination file exists, then this
     * method will overwrite it.
     * 
     * @param srcFile
     *            an existing file to copy, must not be <code>null</code>
     * @param destFile
     *            the new file, must not be <code>null</code>
     * @param preserveFileDate
     *            true if the file date of the copy should be the same as the
     *            original
     * 
     * @throws NullPointerException
     *             if source or destination is <code>null</code>
     * @throws IOException
     *             if source or destination is invalid
     * @throws IOException
     *             if an IO error occurs during copying
     * @see #copyFileToDirectory(File, File, boolean)
     */
    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (srcFile.exists() == false) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
            if (destFile.getParentFile().mkdirs() == false) {
                throw new IOException("Destination '" + destFile + "' directory cannot be created");
            }
        }
        if (destFile.exists() && destFile.canWrite() == false) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        doCopyFile(srcFile, destFile, preserveFileDate);
    }

    /**
     * Internal copy file method.
     * 
     * @param srcFile
     *            the validated source file, must not be <code>null</code>
     * @param destFile
     *            the validated destination file, must not be <code>null</code>
     * @param preserveFileDate
     *            whether to preserve the file date
     * @throws IOException
     *             if an error occurs
     */
    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }
        FileInputStream input = new FileInputStream(srcFile);
        try {
            FileOutputStream output = new FileOutputStream(destFile);
            try {
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }
        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }
        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified());
        }
    }
}
