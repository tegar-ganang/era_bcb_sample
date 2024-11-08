package org.nexopenframework.ide.eclipse.commons.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p></p>
 * 
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public abstract class FileUtils {

    /**
	 * Copies a resource from within the plugin to a destination in the
	 * workspace.
	 * 
	 * @param src
	 *            the path of the resource within the plugin
	 * @param dest
	 *            the destination path within the workspace
	 * @throws CoreException 
	 */
    public static void copyFromPlugin(final IPath src, final IFile dest, final Bundle bundle) throws IOException, CoreException {
        final InputStream in = FileLocator.openStream(bundle, src, false);
        dest.create(in, true, null);
    }

    /**
	 * @param sourceDirectory
	 * @param destinationDirectory
	 * @throws IOException
	 */
    public static void copyDirectoryStructure(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!sourceDirectory.exists()) throw new IOException((new StringBuffer("Source directory doesn't exists (")).append(sourceDirectory.getAbsolutePath()).append(").").toString());
        final File files[] = sourceDirectory.listFiles();
        String sourcePath = sourceDirectory.getAbsolutePath();
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            String dest = file.getAbsolutePath();
            dest = dest.substring(sourcePath.length() + 1);
            File destination = new File(destinationDirectory, dest);
            if (file.isFile()) {
                destination = destination.getParentFile();
                copyFileToDirectory(file, destination);
            } else if (file.isDirectory()) {
                if (!destination.exists() && !destination.mkdirs()) throw new IOException((new StringBuffer("Could not create destination directory '")).append(destination.getAbsolutePath()).append("'.").toString());
                copyDirectoryStructure(file, destination);
            } else {
                throw new IOException((new StringBuffer("Unknown file type: ")).append(file.getAbsolutePath()).toString());
            }
        }
    }

    /**
	 * @param source
	 * @param destinationDirectory
	 * @throws IOException
	 */
    public static void copyFileToDirectory(File source, File destinationDirectory) throws IOException {
        if (destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
            throw new IllegalArgumentException("Destination is not a directory");
        }
        copyFile(source, new File(destinationDirectory, source.getName()));
        return;
    }

    /**
	 * @param source
	 * @param destinationDirectory
	 * @throws IOException
	 */
    public static void copyFileToDirectoryIfModified(File source, File destinationDirectory) throws IOException {
        if (destinationDirectory.exists() && !destinationDirectory.isDirectory()) {
            throw new IllegalArgumentException("Destination is not a directory");
        }
        copyFileIfModified(source, new File(destinationDirectory, source.getName()));
        return;
    }

    /**
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
    public static void copyFileIfModified(File source, File destination) throws IOException {
        if (destination.lastModified() < source.lastModified()) copyFile(source, destination);
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
	 *            an existing file to copy, must not be null
	 * @param destFile
	 *            the new file, must not be null
	 * 
	 * @throws NullPointerException
	 *             if source or destination is null
	 * @throws IOException
	 *             if source or destination is invalid
	 * @throws IOException
	 *             if an IO error occurs during copying
	 * @see #copyFileToDirectory
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
	 *            an existing file to copy, must not be null
	 * @param destFile
	 *            the new file, must not be null
	 * @param preserveFileDate
	 *            true if the file date of the copy should be the same as the
	 *            original
	 * 
	 * @throws NullPointerException
	 *             if source or destination is null
	 * @throws IOException
	 *             if source or destination is invalid
	 * @throws IOException
	 *             if an IO error occurs during copying
	 * @see #copyFileToDirectory
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
	 *            the validated source file, not null
	 * @param destFile
	 *            the validated destination file, not null
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

    /**
     * <p>
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * </p>
     * <p>
     * The difference between File.delete() and this method are:
     * </p>
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     * @param file file or directory to delete.
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Recursively delete a directory.
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
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
     * Clean a directory without deleting it.
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
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
}
