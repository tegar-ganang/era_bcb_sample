package si.mk.k3.util;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import si.mk.util.KFileFilter;

/**
 * This class simpifies collectiong file names for Open, Save and Save
 * As operations. It takes care about dialogs, extensions and existing
 * files.
 */
public class OpenSaveUtility {

    private KFileFilter filter;

    private String[] extensions;

    private FileFilter selectedFilter;

    /**
     * Construct the object.
     *
     * @param extensions list of acceptable extensions. They must not be preceeded by '.'.
     * @param description description of file type to open
     * @see ExtensionFileFilter
     */
    public OpenSaveUtility(String[] extensions, String description) {
        this.extensions = extensions;
        filter = new KFileFilter(extensions, description);
    }

    /** Returns file for Open operation or null if the file does not exist. */
    public File getOpenFileName(File path) {
        File file = getFile(path, true);
        if (file != null) {
            file = addExtension(file);
            if (!file.exists()) {
                JOptionPane.showMessageDialog(null, "File does not exist: " + file.getAbsolutePath(), "Error!", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return file;
    }

    /** Returns file for Open operation or null if no valid file name
     * was selected by the user.
     */
    public File getSaveFileName(File currentFile, File path, boolean saveAs) {
        File file = null;
        if (saveAs || (currentFile == null)) {
            file = getFile(path, false);
            if (file == null) {
                return null;
            }
            if (file.exists()) {
                Object[] options = { "Yes", "No" };
                int selection = JOptionPane.showOptionDialog(null, "File '" + file.getName() + "' already exists. Overwrite it?", "Warning!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (selection != 0) {
                    return null;
                }
            }
            file = addExtension(file);
            return file;
        } else {
            return currentFile;
        }
    }

    /** Opens JFileChooser and returns a file. If no file is
     * selected (user pressed Cancel button), null is returned. */
    public File getFile(File path, boolean isOpenDialog) {
        File retVal = null;
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(filter);
        if (path != null) {
            chooser.setCurrentDirectory(path);
        }
        int result = 0;
        if (isOpenDialog) {
            result = chooser.showOpenDialog(null);
        } else {
            result = chooser.showSaveDialog(null);
        }
        if (result == JFileChooser.APPROVE_OPTION) {
            retVal = chooser.getSelectedFile();
            selectedFilter = chooser.getFileFilter();
        }
        return retVal;
    }

    /** If there is only one extension specified, it is appended to file name. */
    private File addExtension(File file) {
        if (extensions.length == 1) {
            if (selectedFilter != null) {
                String filterDescription = selectedFilter.getDescription().toLowerCase();
                if (filterDescription.indexOf(extensions[0].toLowerCase()) == -1) {
                    return file;
                }
            }
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i < 0) {
                file = new File(file.getAbsolutePath() + '.' + extensions[0]);
            }
        }
        return file;
    }
}
