package org.geoforge.lang.util.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;

/**
 *
 * @author bantchao
 *
 * email: bantchao_AT_gmail.com
 * ... please remove "_AT_" from the above string to get the right email address
 *
 */
public class FileHandlerLogger extends FileHandler {

    private static final String _F_S_STR_NAME_FOLDER_PUBLIC_ = "public";

    private static final String _F_S_STR_NAME_FOLDER_PRIVATE_ = ".private";

    private static String[] STRS_LOGS_PARENT_FOLDERS;

    private static String STR_APPLI_VERSION_TRANSFORMED;

    private static final String _F_STR_NAME_LOG_PREFIX_ = "runtime";

    private static final String _F_STR_SUFFIX_LOG_ = "." + "log";

    private static final String _F_STR_NAMELOGJAVA = FileHandlerLogger._F_STR_NAME_LOG_PREFIX_ + "Java" + FileHandlerLogger._F_STR_SUFFIX_LOG_;

    private static final String _F_STR_NAMELOGCPP = FileHandlerLogger._F_STR_NAME_LOG_PREFIX_ + "Cpp" + FileHandlerLogger._F_STR_SUFFIX_LOG_;

    protected static String _STR_PATH_LOG_DIR_PARENT_ = null;

    private static FileHandlerLogger _INSTANCE = null;

    public static void s_init(String[] strsLogsParentFolder, String strVersionTransformed) {
        FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS = strsLogsParentFolder;
        FileHandlerLogger.STR_APPLI_VERSION_TRANSFORMED = strVersionTransformed;
        File fleVersion = null;
        try {
            fleVersion = s_getOrCreateVersionDir();
            if (fleVersion == null) {
                String str = "fleVersion == null";
                System.err.println(str);
                JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            String str = exc.getMessage();
            System.err.println(str);
            JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        FileHandlerLogger._STR_PATH_LOG_DIR_PARENT_ = fleVersion.getAbsolutePath();
        FileHandlerLogger._s_createFolderChild(fleVersion, FileHandlerLogger._F_S_STR_NAME_FOLDER_PUBLIC_);
        FileHandlerLogger._s_createFolderChild(fleVersion, FileHandlerLogger._F_S_STR_NAME_FOLDER_PRIVATE_);
    }

    private static void _s_createFolderChild(File fleParent, String strNameChild) {
        File fleChild = new File(fleParent, strNameChild);
        if (fleChild.exists()) {
            if (fleChild.isDirectory()) return;
            String str = "! fleChild.isDirectory():" + fleChild.getAbsolutePath();
            System.err.println(str);
            JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        if (!fleChild.mkdir()) {
            String str = "! fleChild.mkdir():" + fleChild.getAbsolutePath();
            System.err.println(str);
            JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static String s_getPathAbsDirLogPublic() {
        return FileHandlerLogger._STR_PATH_LOG_DIR_PARENT_ + java.io.File.separator + FileHandlerLogger._F_S_STR_NAME_FOLDER_PUBLIC_;
    }

    public static String s_getPathAbsDirLogPrivate() {
        return FileHandlerLogger._STR_PATH_LOG_DIR_PARENT_ + java.io.File.separator + FileHandlerLogger._F_S_STR_NAME_FOLDER_PRIVATE_;
    }

    public static String s_getPathAbsFileLogJava() {
        return FileHandlerLogger.s_getPathAbsDirLogPublic() + java.io.File.separator + FileHandlerLogger._F_STR_NAMELOGJAVA;
    }

    public static String s_getPathAbsFileLogCpp() {
        return FileHandlerLogger.s_getPathAbsDirLogPublic() + FileHandlerLogger._F_S_STR_NAME_FOLDER_PUBLIC_ + java.io.File.separator + FileHandlerLogger._F_STR_NAMELOGCPP;
    }

    public static FileHandlerLogger s_getInstance() {
        if (FileHandlerLogger._INSTANCE == null) {
            try {
                FileHandlerLogger._INSTANCE = new FileHandlerLogger();
            } catch (Exception exc) {
                exc.printStackTrace();
                String str = exc.getMessage();
                System.err.println(str);
                JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        return FileHandlerLogger._INSTANCE;
    }

    private FileHandlerLogger() throws IOException, SecurityException {
        super(FileHandlerLogger.s_getPathAbsFileLogJava());
        super.setFormatter(new SimpleFormatter());
    }

    public static File s_getOrCreateVersionDir() throws Exception {
        File fleParent = new File(System.getProperty("user.home"));
        if (fleParent == null) {
            String str = "fleParent == null";
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleParent.isDirectory()) {
            String str = "! fleParent.isDirectory(), fleParent.getAbsolutePath()=" + fleParent.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleParent.canRead() || !fleParent.canWrite()) {
            String str = "!fleParent.canRead() || !fleParent.canWrite(), fleParent.getAbsolutePath()=" + fleParent.getAbsolutePath();
            System.err.println(str);
            fleParent = new File(".");
        }
        File fleCur = null;
        for (int i = 0; i < FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS.length - 1; i++) {
            fleCur = new File(fleParent, FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS[i]);
            if (fleCur == null) {
                String str = "fleCur == null";
                str += "\n" + "fleParent.getAbsolutePath()=" + fleParent.getAbsolutePath();
                str += "\n" + "FileHandlerShr.STRS_LOGS_PARENT_FOLDERS[i]=" + FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS[i];
                System.err.println(str);
                throw new Exception(str);
            }
            if (!fleCur.exists()) {
                fleCur.mkdir();
            } else if (!fleCur.isDirectory()) {
                String str = "! fleCur.isDirectory()";
                str += "\n" + "fleCur.getAbsolutePath()=" + fleCur.getAbsolutePath();
                System.err.println(str);
                throw new Exception(str);
            }
            fleParent = fleCur;
        }
        fleCur = new File(fleParent, FileHandlerLogger.STR_APPLI_VERSION_TRANSFORMED);
        if (!fleCur.exists()) {
            fleCur.mkdir();
        } else if (!fleCur.isDirectory()) {
            String str = "! fleCur.isDirectory()";
            str += "\n" + "fleCur.getAbsolutePath()=" + fleCur.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        return fleCur;
    }

    public static File s_getVersionDir() throws Exception {
        File fleParent = new File(System.getProperty("user.home"));
        if (fleParent == null) {
            String str = "fleParent == null";
            str += "\n" + "System.getProperty(\"user.home\")=" + System.getProperty("user.home");
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleParent.isDirectory()) {
            String str = "Not a directory: " + fleParent.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleParent.canRead() || !fleParent.canWrite()) {
            String str = "Cannot read or/and write: " + fleParent.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        File fleCur = null;
        for (int i = 0; i < FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS.length - 1; i++) {
            fleCur = new File(fleParent, FileHandlerLogger.STRS_LOGS_PARENT_FOLDERS[i]);
            if (fleCur == null) {
                String str = "Got nil file: " + "PropNamingEtk.STRS_LOGS_PARENT_FOLDERS[" + i + "]";
                System.err.println(str);
                throw new Exception(str);
            }
            if (!fleCur.exists()) {
                String str = "file does not exist: " + fleCur.getAbsolutePath();
                System.err.println(str);
                throw new Exception(str);
            }
            if (!fleCur.isDirectory()) {
                String str = "file is not a directory: " + fleCur.getAbsolutePath();
                System.err.println(str);
                throw new Exception(str);
            }
            fleParent = fleCur;
        }
        fleCur = new File(fleParent, FileHandlerLogger.STR_APPLI_VERSION_TRANSFORMED);
        if (fleCur == null) {
            String str = "Got nil file: " + "PropNamingEtk.STR_APPLI_VERSION_TRANSFORMED";
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleCur.exists()) {
            String str = "file does not exist: " + fleCur.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        if (!fleCur.isDirectory()) {
            String str = "file dis not a directory: " + fleCur.getAbsolutePath();
            System.err.println(str);
            throw new Exception(str);
        }
        return fleCur;
    }
}
