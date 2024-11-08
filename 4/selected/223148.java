package com.rhythm.commons.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import static com.rhythm.base.Preconditions.assertNotNull;
import static com.rhythm.base.Preconditions.assertArgument;

public class Files {

    public static final String ZIP_FILE_EXTENSION = ".zip";

    public static final String FILE_SEPARATOR = File.separator;

    private Files() {
    }

    /**
     * Returns <code>true</code> if the given file exists, otherwise
     * <code>false</code>
     * 
     * @param path a path
     * @return <code>true</code> if the given file exists, otherwise
     * <code>false</code>
     * @see File
     */
    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /**
     * Returns <code>true</code> if the given file name exists in the directory, otherwise
     * <code>false</code>
     *
     * @param dir  a not -nullable directory
     * @param fileName a not-nullable file name
     * @return <code>true</code> if the given file exists in the directory, otherwise
     * <code>false</code>
     */
    public static boolean exists(File dir, String fileName) {
        assertNotNull(dir);
        if (!dir.exists()) {
            return false;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized File makeZip(File[] files, String fileName) throws IOException {
        byte[] buffer = new byte[18024];
        if (Files.exists(fileName)) {
            throw new IOException("The file [" + fileName + "] already exists.");
        }
        if (!fileName.endsWith(".zip")) {
            fileName += ".zip";
        }
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        ZipOutputStream zipOutputStream = null;
        InputStream inputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            zipOutputStream = new ZipOutputStream(bufferedOutputStream);
            zipOutputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
            for (File file : files) {
                inputStream = new BufferedInputStream(new FileInputStream(file));
                zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
                zipOutputStream.closeEntry();
                inputStream.close();
            }
        } finally {
            Streams.close(zipOutputStream);
            Streams.close(bufferedOutputStream);
            Streams.close(fileOutputStream);
            Streams.close(inputStream);
        }
        return new File(fileName);
    }

    /**
     * Returns a {@code String} array of all the files in the give {@code File} if it is a valid .zip file.  If the {@code File} given
     * is not a .zip file an IOException will be thrown or if the {@code File} given is null a NullPointerException will be thrown.
     *
     * @param file notnullable
     * @return
     * @throws java.io.IOException if {@code File} is not a .zip file
     * @throws NullPointerException if {@code file} is null
     */
    public static synchronized String[] listZipContents(File file) throws IOException {
        assertNotNull(file, "Cannot list the contents of a null zip file. ");
        ZipFile zipFile = new ZipFile(file);
        String[] files = new String[zipFile.size()];
        int entry = 0;
        for (Enumeration entries = zipFile.entries(); entries.hasMoreElements(); ) {
            files[entry] = ((ZipEntry) entries.nextElement()).getName();
            entry++;
        }
        zipFile.close();
        return files;
    }

    /**
     * Creates a new diretory given a {@code String} as the path.  If the path given does
     * not end with the {@code FILE_SEPARATOR} one will be added.  It will also create
     * any parent directories if the don't exist in the given path.
     *<p>
     * This method guarantees the following if returned without an exception<br/>
     * 1.) Requested directory and sub-directories where created (file.mkdirs()) <br/>
     * 1.) The directory exists.<br/>
     * 2.) The created file is a directory.<br/>
     * </p>
     * @param path not nullable
     * @return the created file.
     * @throws java.io.IOException if the call to mkdirs returns false.
     * @throws java.io.IOException if the directory doesn't exist.
     * @throws java.io.IOException if the created file is not a directory.
     * @see file.mkdirs()
     */
    public static synchronized File newDirectory(String path) throws IOException {
        assertNotNull(path, "Cannot create a new directory with a null path");
        path = ((path.endsWith(FILE_SEPARATOR) ? path : path + FILE_SEPARATOR));
        File file = new File(path);
        assertArgument(file.mkdirs(), new IOException("Failed to make the directory [" + path + "] check that the path name is valid."));
        assertArgument(file.exists(), new FileNotFoundException("Directorty was created but reported as non-existing when checking the path of [" + path + "]."));
        assertArgument(file.isDirectory(), new IOException("Directorty was created and found but was reported as a non-directory when checking the path of [" + path + "]."));
        return file;
    }

    /**
     * Creates a new file with the given name in the given directory.  It creates the directory doesn't exists.
     *<p>
     * This method guarantees the following if returned without an exception<br/>
     * 1.) Requested directory and sub-directories where created (file.createNewFile()) <br/>
     * 1.) The file exists.<br/>
     * 2.) The created file is a file.<br/>
     * </p>
     * @param dir not nullable
     * @param name not nullable
     * @return the created file.
     * @throws java.io.IOException if the call to createNewFile returns false.
     * @throws java.io.IOException if the file doesn't exist.
     * @throws java.io.IOException if the created file is not a file.
     * @see file.mkdirs()
     */
    public static synchronized File newFile(File dir, String name) throws IOException {
        assertNotNull(dir);
        assertNotNull(name);
        assertArgument(name.length() > 0, "Invalid file name");
        if (!dir.exists()) {
            dir = newDirectory(dir.getCanonicalPath());
        }
        File file = new File(dir.getCanonicalPath() + FILE_SEPARATOR + name);
        assertArgument(file.createNewFile(), new IOException("Failed to make the file [" + file.getCanonicalPath() + "] check that the path name is valid."));
        assertArgument(file.exists(), new FileNotFoundException("Directorty was created but reported as non-existing when checking the path of [" + file.getCanonicalPath() + "]."));
        assertArgument(file.isFile(), new IOException("Directorty was created and found but was reported as a non-directory when checking the path of [" + file.getCanonicalPath() + "]."));
        return file;
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file
     * to the specified destination file.
     * The directory holding the destination file is created if it does not exist.
     * If the destination file exists, then this method will overwrite it.
     *
     * @param srcFile  an existing file to copy, must not be <code>null</code>
     * @param destFile  the new file, must not be <code>null</code>
     * @return
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     */
    public static synchronized boolean copyFile(File destFile, File srcFile) throws IOException {
        assertNotNull(srcFile, "Source must not be null");
        assertNotNull(destFile, "Destination must not be null");
        assertArgument(srcFile.exists(), "Source '" + srcFile + "' does not exist");
        assertArgument(srcFile.isFile(), "Source '" + srcFile + "' exists but is a not a file");
        assertArgument(!srcFile.getCanonicalPath().equals(destFile.getCanonicalPath()), "Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
            if (destFile.getParentFile().mkdirs() == false) {
                throw new IOException("Destination '" + destFile + "' directory cannot be created");
            }
        }
        if (destFile.exists() && destFile.canWrite() == false) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        return doCopyFile(srcFile, destFile, false);
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile  the validated source file, must not be <code>null</code>
     * @param destFile  the validated destination file, must not be <code>null</code>
     * @param preserveFileDate  whether to preserve the file date
     * @throws IOException if an error occurs
     */
    private static synchronized boolean doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            destFile = new File(destFile + FILE_SEPARATOR + srcFile.getName());
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
        return destFile.exists();
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory  directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        emptyDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException in case deletion is unsuccessful
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

    /**
     * Empties a directory without deleting it.  This will force delete all
     * {@code File}s in the given directory.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void emptyDirectory(File directory) throws IOException {
        assertNotNull(directory, "Cannot empty the directory of a null argument");
        assertArgument(directory.exists(), "The directory [" + directory + "] does not exist.");
        assertArgument(directory.isDirectory(), "The path [" + directory + "] is not a directory.");
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of " + directory);
        }
        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException es) {
                exception = es;
            }
        }
        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file  file or directory to delete, can be <code>null</code>
     * @return <code>true</code> if the file or directory was deleted, otherwise
     * <code>false</code>
     *
     * @since Commons IO 1.4
     */
    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                emptyDirectory(file);
            }
        } catch (Exception e) {
        }
        try {
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Moves a file.
     * <p>
     * When the destination file is on another file system, do a "copy and delete".
     *
     * @param srcFile the file to be moved
     * @param destFile the destination file
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void moveFile(File srcFile, File destFile) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destFile.exists()) {
            throw new IOException("Destination '" + destFile + "' already exists");
        }
        if (destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' is a directory");
        }
        boolean rename = srcFile.renameTo(destFile);
        if (!rename) {
            copyFile(destFile, srcFile);
            if (!srcFile.delete()) {
                deleteQuietly(destFile);
                throw new IOException("Failed to delete original file '" + srcFile + "' after copy to '" + destFile + "'");
            }
        }
    }

    /**
     * Moves a file to a directory.
     *
     * @param srcFile the file to be moved
     * @param destDir the destination file
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void moveFileToDirectory(File srcFile, File destDir, boolean createDestDir) throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination directory must not be null");
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination directory '" + destDir + "' does not exist [createDestDir=" + createDestDir + "]");
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination '" + destDir + "' is not a directory");
        }
        moveFile(srcFile, new File(destDir, srcFile.getName()));
    }

    /**
     * Moves a file or directory to the destination directory.
     * <p>
     * When the destination is on another file system, do a "copy and delete".
     *
     * @param src the file or directory to be moved
     * @param destDir the destination directory
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void moveToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!src.exists()) {
            throw new FileNotFoundException("Source '" + src + "' does not exist");
        }
        if (src.isDirectory()) {
            moveDirectoryToDirectory(src, destDir, createDestDir);
        } else {
            moveFileToDirectory(src, destDir, createDestDir);
        }
    }

    /**
     * Copies a whole directory to a new location preserving the file dates.
     * <p>
     * This method copies the specified directory and all its child
     * directories and files to the specified destination.
     * The destination is the new location and name of the directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.1
     */
    public static void copyDirectory(File srcDir, File destDir) throws IOException {
        copyDirectory(srcDir, destDir, true);
    }

    /**
     * Copies a whole directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be <code>null</code>
     * @param destDir  the new directory, must not be <code>null</code>
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.1
     */
    public static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException {
        copyDirectory(srcDir, destDir, null, preserveFileDate);
    }

    public static void copyDirectory(File srcDir, File destDir, FileFilter filter, boolean preserveFileDate) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (srcDir.exists() == false) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (srcDir.isDirectory() == false) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }
        List exclusionList = null;
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
            File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList(srcFiles.length);
                for (int i = 0; i < srcFiles.length; i++) {
                    File copiedFile = new File(destDir, srcFiles[i].getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
    }

    /**
     * Internal copy directory method.
     *
     * @param srcDir  the validated source directory, must not be <code>null</code>
     * @param destDir  the validated destination directory, must not be <code>null</code>
     * @param filter  the filter to apply, null means copy all directories and files
     * @param preserveFileDate  whether to preserve the file date
     * @param exclusionList  List of files and directories to exclude from the copy, may be null
     * @throws IOException if an error occurs
     * @since Commons IO 1.1
     */
    private static void doCopyDirectory(File srcDir, File destDir, FileFilter filter, boolean preserveFileDate, List exclusionList) throws IOException {
        if (destDir.exists()) {
            if (destDir.isDirectory() == false) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (destDir.mkdirs() == false) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
            if (preserveFileDate) {
                destDir.setLastModified(srcDir.lastModified());
            }
        }
        if (destDir.canWrite() == false) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        File[] files = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
        if (files == null) {
            throw new IOException("Failed to list contents of " + srcDir);
        }
        for (int i = 0; i < files.length; i++) {
            File copiedFile = new File(destDir, files[i].getName());
            if (exclusionList == null || !exclusionList.contains(files[i].getCanonicalPath())) {
                if (files[i].isDirectory()) {
                    doCopyDirectory(files[i], copiedFile, filter, preserveFileDate, exclusionList);
                } else {
                    doCopyFile(files[i], copiedFile, preserveFileDate);
                }
            }
        }
    }

    /**
     * Moves a directory.
     * <p>
     * When the destination directory is on another file system, do a "copy and delete".
     *
     * @param srcDir the directory to be moved
     * @param destDir the destination directory
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void moveDirectory(File srcDir, File destDir) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcDir.exists()) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir + "' is not a directory");
        }
        if (destDir.exists()) {
            throw new IOException("Destination '" + destDir + "' already exists");
        }
        boolean rename = srcDir.renameTo(destDir);
        if (!rename) {
            copyDirectory(srcDir, destDir);
            deleteDirectory(srcDir);
            if (srcDir.exists()) {
                throw new IOException("Failed to delete original directory '" + srcDir + "' after copy to '" + destDir + "'");
            }
        }
    }

    /**
     * Moves a directory to another directory.
     *
     * @param src the file to be moved
     * @param destDir the destination file
     * @param createDestDir If <code>true</code> create the destination directory,
     * otherwise if <code>false</code> throw an IOException
     * @throws NullPointerException if source or destination is <code>null</code>
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void moveDirectoryToDirectory(File src, File destDir, boolean createDestDir) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination directory must not be null");
        }
        if (!destDir.exists() && createDestDir) {
            destDir.mkdirs();
        }
        if (!destDir.exists()) {
            throw new FileNotFoundException("Destination directory '" + destDir + "' does not exist [createDestDir=" + createDestDir + "]");
        }
        if (!destDir.isDirectory()) {
            throw new IOException("Destination '" + destDir + "' is not a directory");
        }
        moveDirectory(src, new File(destDir, src.getName()));
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = null;
        byte[] bytes = null;
        try {
            is = new FileInputStream(file);
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("File is too large");
            }
            bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return bytes;
    }
}
