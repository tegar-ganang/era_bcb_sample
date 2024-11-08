package au.edu.qut.yawl.editor.foundations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtilities {

    public static final String PLUGIN_DIRECTORY = System.getProperty("user.dir") + System.getProperty("file.separator") + "YAWLEditorPlugins";

    public static final String ICON_PLUGIN_DIRECTORY = PLUGIN_DIRECTORY + System.getProperty("file.separator") + "TaskIcons";

    /**
   * Moves one file to another. Note that if a file already exists with the same
   * name as <code>targetFile</code> this method will overwrite its contents.
   * @param sourceFile
   * @param targetFile
   * @throws IOException
   */
    public static void move(String sourceFile, String targetFile) throws IOException {
        copy(sourceFile, targetFile);
        new File(sourceFile).delete();
    }

    /**
   * Copies one file to another. Note that if a file already exists with the same
   * name as <code>targetFile</code> this method will overwrite its contents.
   * @param sourceFile
   * @param targetFile
   * @throws IOException
   */
    public static void copy(String sourceFile, String targetFile) throws IOException {
        FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
        FileChannel targetChannel = new FileOutputStream(targetFile).getChannel();
        targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        targetChannel.close();
    }

    /**
   * Strips the extension from a filename, assuming that extensions follow the
   * standard convention of being the text following the last '.' character
   * in the filemane.
   * @param fileName
   * @return The filename sans its extension
   */
    public static String stripFileExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}
