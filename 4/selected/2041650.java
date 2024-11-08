package wsdl2doc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * Some methods helping with files.
 * 
 * @author Christoph
 */
public class FileUtils {

    /**
	 * Create a new directory, inclduing ancestor directories if necessary.
	 * 
	 * @param path
	 *            Path to the directory
	 * @throws EnhancedException
	 *             thrown when creating failed
	 */
    public static void createDirectory(String path) {
        boolean success = (new File(path)).mkdirs();
        if (!success) {
        }
    }

    /**
	 * Converts a DOS path with \ delimeters to a java path with //.
	 * 
	 * @param dosPath
	 *            a DOS path
	 * @return a java path
	 */
    public static String convertDosToJavaPath(String dosPath) {
        return dosPath.replace('\\', '/');
    }

    /**
	 * Copies the content of one file to another.
	 * 
	 * @param in
	 *            input file
	 * @param out
	 *            output file
	 * @throws EnhancedException
	 *             error
	 */
    public static void copyFile(File in, File out) throws EnhancedException {
        try {
            FileChannel sourceChannel = new FileInputStream(in).getChannel();
            FileChannel destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            sourceChannel.close();
            destinationChannel.close();
        } catch (Exception e) {
            throw new EnhancedException("Could not copy file " + in.getAbsolutePath() + " to " + out.getAbsolutePath() + ".", e);
        }
    }

    /**
	 * Copies a dirctory with all its files and subdirectories.
	 * 
	 * @param file
	 *            a directory reference
	 * @throws EnhancedException failure
	 */
    public static void copyDirectory(File in, String targetDirectoryName) throws EnhancedException {
        if (in.isDirectory()) {
            File[] children = in.listFiles();
            createDirectory(targetDirectoryName);
            for (int i = 0; i < children.length; i++) {
                if (children[i].isDirectory()) {
                    copyDirectory(children[i], targetDirectoryName + "//" + children[i].getName());
                } else {
                    copyFile(children[i], new File(targetDirectoryName + "//" + children[i].getName()));
                }
            }
        }
    }
}
