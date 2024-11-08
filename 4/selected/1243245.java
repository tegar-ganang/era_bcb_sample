package net.sourceforge.jaulp.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.sourceforge.jaulp.file.copy.CopyFileUtils;
import net.sourceforge.jaulp.file.create.CreateFileUtils;
import net.sourceforge.jaulp.file.exceptions.DirectoryAllreadyExistsException;
import net.sourceforge.jaulp.file.exceptions.FileIsADirectoryException;
import net.sourceforge.jaulp.file.read.ReadFileUtils;
import net.sourceforge.jaulp.file.rename.RenameFileUtils;

/**
 * Utility class for the use of File object. Most methods are set to deprecated and has gone to the appropriate class.
 *
 * @version 1.0
 * @author Asterios Raptis
 */
public class FileUtils {

    /**
	 * Instantiates a new file utils.
	 */
    protected FileUtils() {
        super();
    }

    /**
 * Copys a source file to the destination file.
 *
 * @param source The source file.
 * @param destination The destination file.
 * @return true, if successful
 * @throws IOException Is thrown if an error occurs by reading or writing.
 * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
 * @deprecated Use instead the method CopyFileUtils.copyFile(source, destination).
 */
    public static boolean copyFile(final File source, final File destination) throws IOException, FileIsADirectoryException {
        return CopyFileUtils.copyFile(source, destination);
    }

    /**
     * Creates a new directory.
     *
     * @param dir
     *            The directory to create.
     * @return Returns true if the directory was created otherwise false.
     * @throws DirectoryAllreadyExistsException
     *             Thrown if the directory all ready exists.
     * @deprecated Use instead the method CreateFileUtils.createDirectory(File).
     */
    public static boolean createDirectory(final File dir) throws DirectoryAllreadyExistsException {
        return CreateFileUtils.createDirectory(dir);
    }

    /**
     * Creates an empty file if the File does not exists otherwise it lets the file as it is.
     *
     * @param file
     *            the file.
     * @return true, if successful.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @deprecated Use instead the method CreateFileUtils.createFile(File).
     */
    public static boolean createFile(final File file) throws IOException {
        return CreateFileUtils.createFile(file);
    }

    /**
     * Downloads Data from the given URI.
     *
     * @param uri
     *            The URI from where to download.
     * @return Returns a byte array or null.
     */
    public static byte[] download(final URI uri) {
        final File tmpFile = new File(uri);
        return ReadFileUtils.getFilecontentAsByteArray(tmpFile);
    }

    /**
     * Gets the absolut path without the filename.
     *
     * @param file
     *            the file.
     * @return 's the absolut path without filename.
     */
    public static String getAbsolutPathWithoutFilename(final File file) {
        return RenameFileUtils.getAbsolutPathWithoutFilename(file);
    }

    /**
     * Gets the current absolut path without the dot and slash.
     *
     * @return 's the current absolut path without the dot and slash.
     */
    public static String getCurrentAbsolutPathWithoutDotAndSlash() {
        File currentAbsolutPath = new File(".");
        return currentAbsolutPath.getAbsolutePath().substring(0, currentAbsolutPath.getAbsolutePath().length() - 2);
    }

    /**
     * Gets the filename with the absolute path prefix.
     *
     * @param file
     *            the file.
     * @return the filename prefix.
     */
    public static String getFilenamePrefix(final File file) {
        final String fileName = file.getAbsolutePath();
        final int ext_index = fileName.lastIndexOf(".");
        final String fileNamePrefix;
        if (ext_index != -1) {
            fileNamePrefix = fileName.substring(0, ext_index);
        } else {
            fileNamePrefix = fileName;
        }
        return fileNamePrefix;
    }

    /**
     * Gets the filename without the extension or null if the given file object is a directory.
     *
     * @param file
     *            the file.
     * @return the filename without the extension or null if the given file object is a directory.
     */
    public static String getFilenameWithoutExtension(final File file) {
        if (!file.isDirectory()) {
            final String fileName = file.getName();
            final int ext_index = fileName.lastIndexOf(".");
            final String fileNamePrefix;
            if (ext_index != -1) {
                fileNamePrefix = fileName.substring(0, ext_index);
            } else {
                fileNamePrefix = fileName;
            }
            return fileNamePrefix;
        }
        return null;
    }

    /**
     * Gets the filename suffix or null if no suffix exists or the given file object is a directory.
     *
     * @param file
     *            the file.
     * @return 's the filename suffix or null if no suffix exists or the given file object is a directory.
     */
    public static String getFilenameSuffix(final File file) {
        if (!file.isDirectory()) {
            final String fileName = file.getAbsolutePath();
            final int ext_index = fileName.lastIndexOf(".");
            final String fileNameSuffix;
            if (ext_index != -1) {
                fileNameSuffix = fileName.substring(ext_index, fileName.length());
            } else {
                fileNameSuffix = null;
            }
            return fileNameSuffix;
        }
        return null;
    }

    /**
     * Not yet implemented. Checks if the given file is open.
     *
     * @param file
     *            The file to check.
     * @return Return true if the file is open otherwise false.
     */
    public static boolean isOpen(final File file) {
        throw new UnsupportedOperationException("The method isOpen is not jet supported in this Version.");
    }
}
