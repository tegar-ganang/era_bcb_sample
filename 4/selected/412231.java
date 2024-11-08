package cwi.SVGGraphics;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

/**
* Wrapper around the file chooser widget of Swing to generate a new svg file. Takes
* care of checking an overwrite of an old file, keeps the previous directory for
* writing, etc. (Why aren't these things part of the Swing widget, b.t.w.?)
*
* @author Ivan Herman
*/
class FilePicker {

    static String dir = null;

    /**
  * Main entry: get an svg file for writing. At start up, the default directory
  * is the system property "user.dir". Afterwards, the previous directory is used.
  *
  * @return stream to an svg file, or null if user has cancelled.
  */
    static PrintStream getSVGFile() {
        File file;
        if (dir == null) dir = System.getProperty("user.dir");
        FileFilter fileFilter = new FileFilter() {

            public boolean accept(File aFile) {
                return (aFile.isDirectory() || aFile.getName().endsWith(".svg"));
            }

            public String getDescription() {
                return "SVG Files (*" + ".svg" + ")";
            }
        };
        while (true) {
            JFileChooser fileChooser = new JFileChooser(dir);
            fileChooser.addChoosableFileFilter(fileFilter);
            fileChooser.setFileFilter(fileFilter);
            int option = fileChooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                if (file.getName().endsWith(".svg")) {
                    dir = fileChooser.getCurrentDirectory().getAbsolutePath();
                    if (file.exists()) {
                        int i = JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (i != JOptionPane.YES_OPTION) continue;
                    }
                    return openFile(file.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(null, "The selected file must be of type .svg", "Invalid file type", JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }
            } else {
                return null;
            }
        }
    }

    private static PrintStream openFile(String name) {
        try {
            FileOutputStream f = new FileOutputStream(name, false);
            return new PrintStream(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Exception raised when opening the file: " + e.toString(), "Could not open file", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
    }
}
