package org.homeunix.thecave.moss.swing.file;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.homeunix.thecave.moss.util.FileFunctions;

public class SmartFileChooser {

    private SmartFileChooser() {
    }

    public static File showSmartOpenDialog(JFrame frame, File startLocation, FileFilter filter) {
        return showSmartOpenDialog(frame, startLocation, filter, "Open File...", "OK", "Cannot write to selected file", "Cannot read from selected file", "Error");
    }

    public static File showSmartOpenDialog(JFrame frame, File startLocation, FileFilter filter, String title, String okTranslation, String cannotWriteDataFileTranslation, String cannotReadDataFileTranslation, String errorTranslation) {
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
            return file;
        }
        return null;
    }

    public static File showSaveFileDialog(JFrame frame, File startLocation, FileFilter filter, String extension) {
        return showSaveFileDialog(frame, startLocation, filter, extension, "Save File...", "OK", "Cancel", "Replace", "Cannot write to selected file", "The selected file already exists.\nDo you want to overwrite it?", "Error");
    }

    public static File showSaveFileDialog(JFrame frame, File startLocation, FileFilter filter, String extension, String title, String okTranslation, String cancelTranslation, String replaceTranslation, String cannotWriteDataFileTranslation, String promptOverwriteFileTranslation, String errorTranslation) {
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
                int reply = JOptionPane.showOptionDialog(frame, promptOverwriteFileTranslation, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                if (reply == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
            }
            if (!FileFunctions.isFolderWritable(jfc.getSelectedFile().getParentFile())) {
                String[] options = new String[1];
                options[0] = okTranslation;
                JOptionPane.showOptionDialog(null, cannotWriteDataFileTranslation, errorTranslation, JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            } else {
                String location = jfc.getSelectedFile().getAbsolutePath();
                if (extension != null) {
                    if (!jfc.getSelectedFile().getName().toLowerCase().endsWith(extension)) location += extension;
                }
                file = new File(location);
            }
            return file;
        }
        return null;
    }
}
