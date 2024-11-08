package org.homeunix.thecave.moss.swing;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.homeunix.thecave.moss.util.FileFunctions;

/**
 * A class which helps set up 'smart' file choosers.  These file choosers will
 * remember the last selected location, check for read / write access to selected
 * files, append default extensions if desired, etc.
 * 
 * @author wyatt
 *
 */
public class MossSmartFileChooser {

    private MossSmartFileChooser() {
    }

    private static File startLocation;

    /**
	 * Shows the open dialog, with English translation strings.
	 * @param frame The calling frame
	 * @param filter The FileFilter to use.
	 * @return
	 */
    public static File showOpenDialog(JFrame frame, FileFilter filter) {
        return showOpenDialog(frame, filter, "Open File...", "OK", "Cannot write to selected file", "Cannot read from selected file", "Error");
    }

    /**
	 * Shows the open dialog, with custom translation strings.
	 * @param frame The calling frame
	 * @param filter The FileFilter to use.
	 * @param title The title of the dialog
	 * @param okTranslation The translation for the OK button.
	 * @param cannotWriteDataFileTranslation The translation for "Cannot write to the selected file".
	 * @param cannotReadDataFileTranslation  The translation for "Cannot read from the selected file".
	 * @param errorTranslation The translation for "Error"
	 * @return
	 */
    public static File showOpenDialog(JFrame frame, FileFilter filter, String title, String okTranslation, String cannotWriteDataFileTranslation, String cannotReadDataFileTranslation, String errorTranslation) {
        File file = null;
        final JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(filter);
        if (startLocation != null) jfc.setCurrentDirectory(startLocation);
        jfc.setDialogTitle(title);
        if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            if (!jfc.getSelectedFile().exists()) {
            } else if (!FileFunctions.isFolderWritable(jfc.getSelectedFile().getParentFile())) {
                String[] options = new String[1];
                options[0] = okTranslation;
                JOptionPane.showOptionDialog(frame, cannotWriteDataFileTranslation, errorTranslation, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            } else if (!jfc.getSelectedFile().canRead()) {
                String[] options = new String[1];
                options[0] = okTranslation;
                JOptionPane.showOptionDialog(frame, cannotReadDataFileTranslation, errorTranslation, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            } else {
                file = jfc.getSelectedFile();
            }
            startLocation = file;
            if (file != null) return new File(file.getAbsolutePath());
        }
        return null;
    }

    /**
	 * Shows the save dialog, with English translation strings.
	 * @param frame The calling frame
	 * @param filter The FileFilter to use
	 * @param defaultExtension The default extension.  If this is set, we wil ensure 
	 * that it is appended to the file name.  If this is null, we ignore it. 
	 * @return
	 */
    public static File showSaveFileDialog(JFrame frame, FileFilter filter, String extension) {
        return showSaveFileDialog(frame, filter, extension, "Save File...", "OK", "Cancel", "Replace", "Cannot write to selected file", "Error", "The selected file already exists.\nDo you want to overwrite it?", "Overwrite File?");
    }

    /**
	 * Shows the save file dialog, with custom translation strings.
	 * @param frame The calling frame
	 * @param filter The FileFilter to use
	 * @param defaultExtension The default extension.  If this is set, we wil ensure 
	 * that it is appended to the file name.  If this is null, we ignore it. 
	 * @param title The title of the file chooser
	 * @param okTranslation The translation for the OK button.
	 * @param cancelTranslation The translation for the Cancel button.
	 * @param replaceTranslation The translation for the Replace button.
	 * @param cannotWriteDataFileTranslation The translation for the message that the selected file is not writable.
	 * @param error The translation for "Error".
	 * @param promptOverwriteFileTranslation The translation for the message that the file exists, and will be overwritten.
	 * @param promptOverwriteFileTitleTranslation The translation for the title of the overwrite warning. 
	 * @return
	 */
    public static File showSaveFileDialog(JFrame frame, FileFilter filter, String defaultExtension, String title, String okTranslation, String cancelTranslation, String replaceTranslation, String cannotWriteDataFileTranslation, String error, String promptOverwriteFileTranslation, String promptOverwriteFileTitleTranslation) {
        File file = null;
        final JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setFileFilter(filter);
        if (startLocation != null) jfc.setCurrentDirectory(startLocation);
        jfc.setDialogTitle(title);
        if (jfc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            if (jfc.getSelectedFile().exists()) {
                String[] options = new String[2];
                options[0] = replaceTranslation;
                options[1] = cancelTranslation;
                int reply = JOptionPane.showOptionDialog(frame, promptOverwriteFileTranslation, promptOverwriteFileTitleTranslation, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (reply == 1) {
                    return null;
                }
            }
            if (!FileFunctions.isFolderWritable(jfc.getSelectedFile().getParentFile())) {
                String[] options = new String[1];
                options[0] = okTranslation;
                JOptionPane.showOptionDialog(null, cannotWriteDataFileTranslation, error, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            } else {
                String location = jfc.getSelectedFile().getAbsolutePath();
                if (defaultExtension != null) {
                    if (!jfc.getSelectedFile().getName().toLowerCase().endsWith(defaultExtension)) location += defaultExtension;
                }
                file = new File(location);
            }
            startLocation = file;
            if (file != null) return new File(file.getAbsolutePath());
        }
        return null;
    }
}
