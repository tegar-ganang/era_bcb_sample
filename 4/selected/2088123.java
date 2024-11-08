package org.yawlfoundation.yawl.editor.foundations;

import org.yawlfoundation.yawl.editor.YAWLEditor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.security.CodeSource;
import java.util.prefs.Preferences;

public class FileUtilities {

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

    public static void backup(String sourceFile, String targetFile) throws IOException {
        File file = new File(sourceFile);
        if (file.exists()) copy(sourceFile, targetFile);
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
        int lastDotPos = fileName.lastIndexOf('.');
        return (lastDotPos > -1) ? fileName.substring(0, lastDotPos) : fileName;
    }

    private static final String HOME_DIR = setHomeDir();

    private static final String RELATIVE_PLUGIN_PATH = "YAWLEditorPlugins";

    public static final String ABSOLUTE_PLUGIN_DIRECTORY = HOME_DIR + RELATIVE_PLUGIN_PATH;

    private static final String TASK_ICON_PATH = "TaskIcons";

    private static final String RELATIVE_TASK_ICON_PATH = RELATIVE_PLUGIN_PATH + System.getProperty("file.separator") + TASK_ICON_PATH;

    public static final String ABSOLUTE_TASK_ICON_PATH = HOME_DIR + RELATIVE_TASK_ICON_PATH;

    public static String getAbsoluteTaskIconPath() {
        return Preferences.userNodeForPackage(YAWLEditor.class).get("TaskIconsFilePath", ABSOLUTE_TASK_ICON_PATH);
    }

    /**
   * Given the relative path of an icon, returns the absolute path
   * where it will be found in the current environment.
   * @param relativeIconPath
   * @return
   */
    public static String getAbsoluteTaskIconPath(String relativeIconPath) {
        return ABSOLUTE_TASK_ICON_PATH + System.getProperty("file.separator") + relativeIconPath;
    }

    /**
   * Given the absolte path of an icon, returns just the relative 
   * path to the icon that is independant of the absolute task icon plugin path.
   * @param absoluteIconPath
   * @return
   */
    public static String getRelativeTaskIconPath(String absoluteIconPath) {
        assert absoluteIconPath.startsWith(ABSOLUTE_TASK_ICON_PATH + System.getProperty("file.separator"));
        return absoluteIconPath.substring((ABSOLUTE_TASK_ICON_PATH + System.getProperty("file.separator")).length());
    }

    private static final String EXTENDED_ATTRIBUTE_PATH = "ExtendedAttributeProperties";

    private static final String DECOMPOSITION_EXTENDED_ATTRIBUTE_PROPERTIES = "DecompositionProperties";

    private static final String VARIABLE_EXTENDED_ATTRIBUTE_PROPERTIES = "VariableProperties";

    private static final String RELATIVE_EXTENDED_ATTRIBUTE_PATH = RELATIVE_PLUGIN_PATH + System.getProperty("file.separator") + EXTENDED_ATTRIBUTE_PATH;

    public static final String ABSOLUTE_EXTENDED_ATTRIBUTE_PATH = HOME_DIR + RELATIVE_EXTENDED_ATTRIBUTE_PATH;

    /**
   * Returns the absolute path of the decomposition extended attribute properties plugin file
   * @return
   */
    public static String getDecompositionPropertiesExtendeAttributePath() {
        return ABSOLUTE_EXTENDED_ATTRIBUTE_PATH + System.getProperty("file.separator") + DECOMPOSITION_EXTENDED_ATTRIBUTE_PROPERTIES;
    }

    /**
   * Returns the absolute path of the variable extended attribute properties plugin file
   * @return
   */
    public static String getVariablePropertiesExtendedAttributePath() {
        return ABSOLUTE_EXTENDED_ATTRIBUTE_PATH + System.getProperty("file.separator") + VARIABLE_EXTENDED_ATTRIBUTE_PROPERTIES;
    }

    private static String setHomeDir() {
        String result = "";
        try {
            Class editorClass = Class.forName("org.yawlfoundation.yawl.editor.YAWLEditor");
            CodeSource source = editorClass.getProtectionDomain().getCodeSource();
            if (source != null) {
                URL location = source.getLocation();
                String path = URLDecoder.decode(location.getPath(), "UTF-8");
                if (path.charAt(2) == ':') path = path.substring(1);
                int lastSep = path.lastIndexOf('/');
                if (lastSep > -1) result = path.substring(0, lastSep + 1);
                if (File.separatorChar != '/') result = result.replace('/', File.separatorChar);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = System.getProperty("user.dir");
        }
        return result;
    }

    public static String getHomeDir() {
        return HOME_DIR;
    }
}
