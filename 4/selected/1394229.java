package jhomenet.commons.utils;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import jhomenet.commons.cfg.Environment;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public final class FileUtils {

    /**
	 * Define the root logger.
	 */
    private static Logger logger = Logger.getLogger(FileUtils.class.getName());

    /**
	 * Define known file extensions.
	 */
    public static enum FileExtensions {

        /**
		 * Excel file extension
		 */
        XML(".xml"), /**
		 * Text file extension
		 */
        TEXT(".txt");

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
        @Override
        public String toString() {
            return getExtension();
        }
    }

    /**
	 * Keep a list of search paths.
	 */
    private static List<String> searchFolders = new ArrayList<String>();

    /**
	 * Statically initialize any variables.
	 */
    static {
    }

    /**
	 * Non-instantiable class. 
	 */
    private FileUtils() {
    }

    /**
	 * Create a file given the directory and filename. A few checks of the to-be created file are performed
	 * before the actual file is created. This includes:
	 * 
	 * 	1) Checking that the filename is valid. If the filename is found to be invalid, an exception
	 * 	   is thrown.
	 * 	2) Checking that the directory is valid. If it's found to be invalid, the directory is set the
	 * 	   project's project folder.
	 * 	3) Checking that the filename ends with the desired file extension. If the filename doesn't end with
	 * 	   the desired extension, the desired extension is added. 
	 * 
	 * @param directory
	 * @param filename
	 * @param defaultDirectory
	 * @param desiredFileExtension
	 * @return A newly created file
	 */
    public static File getFile(String directory, String filename, String defaultDirectory, FileExtensions desiredFileExtension) {
        if (directory == null || directory.equals("null") || directory.length() <= 0) if (defaultDirectory != null) directory = defaultDirectory; else directory = new File("").getAbsolutePath();
        File f = new File(directory);
        if (!f.exists()) f.mkdir();
        if (filename == null || filename.length() <= 0) throw new IllegalArgumentException("Invalid filename");
        if (desiredFileExtension != null) {
            if (!filename.endsWith(desiredFileExtension.getExtension())) filename = filename.concat(desiredFileExtension.getExtension());
        }
        return new File(directory + Environment.SEPARATOR + filename);
    }

    /**
	 * 
	 * @param directory
	 * @param filename
	 * @param desiredFileExtension
	 * @return
	 */
    public static File getFile(String directory, String filename, FileExtensions desiredFileExtension) {
        return FileUtils.getFile(directory, filename, null, desiredFileExtension);
    }

    /**
	 * 
	 * @param directory
	 * @param filename
	 * @param defaultDirectory
	 * @return
	 */
    public static File getFile(String directory, String filename, String defaultDirectory) {
        return FileUtils.getFile(directory, filename, defaultDirectory, null);
    }

    /**
	 * 
	 * @param directory
	 * @param filename
	 * @return
	 */
    public static File getFile(String directory, String filename) {
        return FileUtils.getFile(directory, filename, null, null);
    }

    /**
	 * Back up an existing file given the type of extension.
	 * 
	 * @param fileToBackup
	 * @param extension
	 * @throws IOException
	 */
    public static void backupFile(File fileToBackup, FileExtensions extension) throws IOException {
        String originalDirectory = fileToBackup.getPath().substring(0, fileToBackup.getPath().lastIndexOf(Environment.SEPARATOR));
        String originalFilename = fileToBackup.getName();
        if (!originalFilename.endsWith(extension.getExtension())) abort("File doesn't end with an " + extension.getExtension() + " extension");
        String basename = originalFilename.substring(0, originalFilename.length() - extension.getExtensionLength());
        String backupFilename = basename + "-" + FormatUtils.dateFormat.format(new Date(System.currentTimeMillis())) + extension.getExtension();
        File backupFile = new File(originalDirectory + Environment.SEPARATOR + backupFilename);
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
	 * @param configuration
	 * @throws IOException
	 */
    public static void copy(File from_file, File to_file) throws IOException {
        from_file = checkFile(from_file);
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
	 * Check the validity of a file. This includes performing the following checks:
	 * 
	 * 	1) Check if the file actually exists
	 * 	2) Check if the file is actually a file and not a pointer to a directory
	 * 	3) Check if the file can be read from
	 * 	4) Check that the file length is greater than 0
	 * 
	 * If any one of these checks fails, then an exception is thrown to indicate an
	 * error in the file.
	 * 
	 * @param file The file to check
	 * @return 
	 * @throws IOException Thrown if any of the file checks fail
	 */
    public static File checkFile(File file) throws IOException {
        if (!file.exists()) {
            String filename = file.getName();
            File tmp = null;
            for (String folder : searchFolders) {
                tmp = new File(folder + Environment.SEPARATOR + filename);
                if (tmp != null && tmp.exists()) {
                    file = tmp;
                    break;
                }
            }
            abort("File does not exist: " + file.getPath());
        }
        if (!file.isFile()) abort("Filename does not point to a file");
        if (!file.canRead()) abort("Cannot read from file");
        if (file.length() <= 0) abort("Zero length file");
        return file;
    }

    /**
	 * Check the validity of a file.
	 * 
	 * @param fullPath
	 * @return
	 * @throws IOException
	 */
    public static File checkFile(String fullPath) throws IOException {
        return checkFile(new File(fullPath));
    }

    /**
	 * Check the validity of a file.
	 * 
	 * @param directory
	 * @param filename
	 * @param configuration
	 * @return
	 * @throws IOException
	 */
    public static File checkFile(String directory, String filename) throws IOException {
        return checkFile(new File(directory + Environment.SEPARATOR + filename));
    }

    /**
	 * 
	 * @param fullPath
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
    public static String getDirectory(String fullPath) {
        int directoryIndex = fullPath.lastIndexOf(Environment.SEPARATOR);
        if (directoryIndex != -1) {
            return fullPath.substring(0, directoryIndex);
        } else {
            return null;
        }
    }

    /**
	 * Create a new directory.
	 *
	 * @param fullPath
	 * @throws IOException
	 */
    public static void createDirectory(String fullPath) throws IOException {
        File folder = new File(fullPath);
        folder.mkdir();
    }

    /**
	 * 
	 * @param folder
	 * @return
	 * @throws IOException
	 */
    public static File checkFolder(File folder) throws IOException {
        if (folder == null) throw new IllegalArgumentException("Folder reference cannot be null");
        if (!folder.exists()) {
            abort("Folder does not exist");
        }
        if (folder.isFile()) abort("Folder references a file");
        if (!folder.canRead()) abort("Cannot read from folder");
        return folder;
    }

    /**
	 * 
	 * @param fullPath
	 * @return
	 */
    public static String getFilename(String fullPath) {
        int directoryIndex = fullPath.lastIndexOf(Environment.SEPARATOR);
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
