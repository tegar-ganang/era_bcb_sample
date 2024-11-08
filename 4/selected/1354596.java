package paymentsimulatorgui;

import java.io.File;
import java.util.StringTokenizer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class Utils {

    private static String lastpath = ".";

    public static String[] split(String line) {
        StringTokenizer st = new StringTokenizer(line);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreElements()) {
            ret[i] = st.nextToken();
            i++;
        }
        return ret;
    }

    public static String getLastPath() {
        return lastpath;
    }

    public static void setLastPath(String p) {
        lastpath = p;
    }

    /**
     * Utility for save and save as dialogs
     * @param title dialog title
     * @param filename with extension
     * @param filedescr description of file type
     * @param extension 
     * @param checkexist true if need to check for overwrites
     * @return File object to be saved to or null
     */
    public static File saveFile(String title, String path, String filedescr, String extension, boolean checkexist) {
        File f = new File(lastpath, path);
        JFileChooser fc = new JFileChooser(path);
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter(filedescr, extension));
        fc.setSelectedFile(f);
        int option = fc.showSaveDialog(null);
        if (JFileChooser.APPROVE_OPTION == option) {
            File newfile = fc.getSelectedFile();
            boolean cansave = true;
            if (checkexist && newfile.exists()) {
                int saveresult = JOptionPane.showConfirmDialog(null, "File already exists, do you really want to overwrite?");
                if (saveresult != JOptionPane.YES_OPTION) {
                    cansave = false;
                }
            }
            if (cansave) {
                Utils.lastpath = newfile.getParent();
                return newfile;
            }
        }
        return null;
    }

    public static File loadFile(String title, String filedescr, String extension) {
        return loadFileOrDirectory(title, filedescr, extension, true);
    }

    public static File loadDirectory(String title) {
        return loadFileOrDirectory(title, null, null, false);
    }

    public static File[] loadMultipleFiles(String title, String filedescr, String extension) {
        JFileChooser chooser = new JFileChooser(Utils.getLastPath());
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        if (extension != null) {
            chooser.setFileFilter(new FileNameExtensionFilter(filedescr + " (*." + extension + ")", extension));
        }
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] ret = chooser.getSelectedFiles();
            lastpath = ret[0].getAbsolutePath();
            return ret;
        } else return null;
    }

    private static File loadFileOrDirectory(String title, String filedescr, String extension, boolean loadfile) {
        JFileChooser chooser = new JFileChooser(Utils.lastpath);
        chooser.setDialogTitle(title);
        if (loadfile) chooser.setFileSelectionMode(JFileChooser.FILES_ONLY); else chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (extension != null) {
            chooser.setFileFilter(new FileNameExtensionFilter(filedescr + " (*." + extension + ")", extension));
        }
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newfile = chooser.getSelectedFile();
            if (loadfile) lastpath = newfile.getParent(); else lastpath = newfile.getAbsolutePath();
            return newfile;
        }
        return null;
    }
}

class FileNameExtensionFilter extends FileFilter {

    private final String description;

    private final String[] extensions;

    private final String[] lowerCaseExtensions;

    public FileNameExtensionFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("Extensions must be non-null and not empty");
        }
        this.description = description;
        this.extensions = new String[extensions.length];
        this.lowerCaseExtensions = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] == null || extensions[i].length() == 0) {
                throw new IllegalArgumentException("Each extension must be non-null and not empty");
            }
            this.extensions[i] = extensions[i];
            lowerCaseExtensions[i] = extensions[i].toLowerCase();
        }
    }

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String fileName = f.getName();
            int i = fileName.lastIndexOf('.');
            if (i > 0 && i < fileName.length() - 1) {
                String desiredExtension = fileName.substring(i + 1).toLowerCase();
                for (String extension : lowerCaseExtensions) {
                    if (desiredExtension.equals(extension)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

    public String[] getExtensions() {
        String[] result = new String[extensions.length];
        System.arraycopy(extensions, 0, result, 0, extensions.length);
        return result;
    }
}
