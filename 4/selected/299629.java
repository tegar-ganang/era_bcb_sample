package tico.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import tico.components.resources.TFileUtils;

/**
 * Static class that manages the current editing project internal files.
 * 
 * @author Pablo Mu�oz
 * @version 1.0 Nov 20, 2006
 */
public class TFileHandler {

    private static final String CURRENT_BASE_DIRECTORY_PATH = "current";

    private static final File CURRENT_BASE_DIRECTORY = new File(CURRENT_BASE_DIRECTORY_PATH);

    private static String currentDirectoryPath;

    private static File currentDirectory;

    static {
        if (!CURRENT_BASE_DIRECTORY.exists()) CURRENT_BASE_DIRECTORY.mkdirs();
        int i = 0;
        currentDirectory = new File(CURRENT_BASE_DIRECTORY_PATH + File.separator + i++);
        while (currentDirectory.exists()) currentDirectory = new File(CURRENT_BASE_DIRECTORY_PATH + File.separator + i++);
        currentDirectoryPath = currentDirectory.getAbsolutePath();
        currentDirectory.mkdirs();
    }

    /**
	 * Imports the specified <code>file</code> to the internal application
	 * structure.
	 * 
	 * @param fileAbsolutePath The specified <code>file</code> fileAbsolutePath
	 * @return The new internal file
	 * @throws IOException If there is any problem importing the file
	 */
    public static File importFile(String fileAbsolutePath) throws IOException {
        File file = new File(fileAbsolutePath);
        return TFileHandler.importFile(file);
    }

    /**
	 * Imports the specified <code>file</code> to the internal application
	 * structure.
	 * 
	 * @param file The specified <code>file</code>
	 * @return The new internal file
	 * @throws IOException If there is any problem importing the file
	 */
    public static File importFile(File file) throws IOException {
        String directoryPath = currentDirectoryPath;
        if (TFileUtils.isImageFile(file)) {
            directoryPath += File.separator + "image";
        }
        if (TFileUtils.isSoundFile(file)) {
            directoryPath += File.separator + "sound";
        }
        File directory = new File(directoryPath);
        if (!directory.exists()) directory.mkdirs();
        return TFileHandler.importFile(file, directory);
    }

    /**
	 * Removes the specified <code>file</code> from the internal application
	 * structure.
	 * 
	 * @param fileAbsolutePath The specified <code>file</code> fileAbsolutePath
	 */
    public static void remove(String fileAbsolutePath) {
        File file = new File(fileAbsolutePath);
        TFileHandler.remove(file);
    }

    private static File importFile(File srcFile, File dstDir) throws IOException {
        File newFile = new File(dstDir, srcFile.getName());
        String baseFilename = srcFile.getName();
        if ((!dstDir.exists()) || (!dstDir.canWrite())) throw new IOException("Invalid destination directory");
        if ((!srcFile.exists()) || (srcFile.isDirectory())) throw new IOException("Invalid input file");
        if (!srcFile.canRead()) throw new IOException("Input file can't be read");
        int fileCount = 1;
        while (newFile.exists()) newFile = new File(dstDir, TFileUtils.getFilename(baseFilename) + "_" + (fileCount++) + "." + TFileUtils.getExtension(baseFilename));
        newFile.createNewFile();
        copyFile(srcFile, newFile);
        return newFile;
    }

    private static void remove(File file) {
        file.delete();
    }

    /**
	 * Returns the editor internal <code>currentDirectory</code>.
	 * 
	 * @return The editor internal <code>currentDirectory</code>
	 */
    public static File getCurrentDirectory() {
        return currentDirectory;
    }

    /**
	 * Returns the editor internal <code>currentDirectoryPath</code>.
	 * 
	 * @return The editor internal <code>currentDirectoryPath</code>
	 */
    public static String getCurrentDirectoryPath() {
        return currentDirectoryPath;
    }

    /**
	 * Converts the specified internal <code>file</code> absolute path to a
	 * current directory partial path.
	 * 
	 * @param file The specified internal <code>file</code>
	 * @return The <code>file</code> partial path
	 */
    public static String convertToPartial(File file) {
        return convertToPartial(file.getAbsolutePath());
    }

    /**
	 * Converts the specified internal <code>file</code> absolute path to a
	 * current directory partial path.
	 * 
	 * @param path The specified internal <code>file</code> path
	 * @return The <code>file</code> partial path
	 */
    public static String convertToPartial(String path) {
        return removeDirectoryPath(currentDirectoryPath, path);
    }

    /**
	 * Removes the specified <code>directory</code> from the specified
	 * <code>path</code>.
	 * 
	 * @param directory The specified <code>directory</code>
	 * @param path The specified <code>path</code>
	 * @return Specified 
	 */
    public static String removeDirectoryPath(String directory, String path) {
        if (path.substring(0, directory.length()).equals(directory)) {
            return path.substring(directory.length() + 1);
        }
        return null;
    }

    /**
	 * Converts the specified internal <code>file</code> partial path to
	 * an absolute path adding the current directory path.
	 * 
	 * @param path The specified internal <code>file</code> partial path
	 * @return The <code>file</code> absolute path
	 */
    public static String convertToAbsolute(String path) {
        return currentDirectoryPath + File.separator + path;
    }

    /**
	 * Deletes the current directory and all its contents.
	 */
    public static void deleteCurrentDirectory() {
        deleteDirectory(currentDirectory);
        currentDirectory.delete();
    }

    /**
	 * Deletes all the current directory contents.
	 */
    public static void cleanCurrentDirectory() {
        deleteDirectory(currentDirectory);
    }

    /**
	 * Deletes the specified <code>directory</code> and all its contents.
	 * 
	 * @param directory The specified <code>directory</code>
	 */
    public static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) deleteDirectory(files[i]);
            files[i].delete();
        }
    }

    /**
	 * Copies the content of an <code>in</code> file to an
	 * <code>out</code> file.
	 * 
	 * @param in The <code>in</code> file
	 * @param out The <code>out</code> file
	 * @throws IOException If there is a problem with any of both files
	 */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        destinationChannel.close();
    }
}
