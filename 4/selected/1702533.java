package jpatch.boundary.action;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import jpatch.VersionInfo;
import jpatch.auxilary.*;
import jpatch.boundary.*;
import jpatch.boundary.settings.Settings;

public final class SaveAsAction extends AbstractAction {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private boolean bSaveAs;

    public SaveAsAction(boolean saveAs) {
        super("", new ImageIcon(ClassLoader.getSystemResource("jpatch/images/save.png")));
        bSaveAs = saveAs;
        if (saveAs) {
            putValue(Action.SHORT_DESCRIPTION, "Save As...");
        } else {
            putValue(Action.SHORT_DESCRIPTION, "Save");
        }
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (bSaveAs) {
            saveAs();
        } else {
            save();
        }
    }

    public boolean saveAs() {
        JFileChooser fileChooser = new JFileChooser(Settings.getInstance().directories.jpatchFiles);
        fileChooser.addChoosableFileFilter(FileFilters.JPATCH);
        if (fileChooser.showSaveDialog(MainFrame.getInstance()) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (FileFilters.getExtension(file).equals("")) {
                file = new File(file.getPath() + ".jpt");
            }
            if (write(file)) {
                return true;
            }
        }
        return false;
    }

    public boolean save() {
        File file;
        if (MainFrame.getInstance().getAnimation() != null) file = MainFrame.getInstance().getAnimation().getFile(); else file = MainFrame.getInstance().getModel().getFile();
        if (file != null) return write(file); else return saveAs();
    }

    private boolean write(File file) {
        String filename = file.getPath();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(byteArrayOutputStream);
        try {
            StringBuffer xml = null;
            if (MainFrame.getInstance().getAnimation() != null) {
                MainFrame.getInstance().getAnimation().xml(out, "\t");
            } else {
                xml = MainFrame.getInstance().getModel().xml("\t");
            }
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(filename));
                BufferedWriter writer = new BufferedWriter(new FileWriter(filename + "~"));
                char[] buffer = new char[65536];
                int charsRead = 0;
                while ((charsRead = reader.read(buffer)) > 0) writer.write(buffer, 0, charsRead);
                reader.close();
                writer.close();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<jpatch version=\"" + VersionInfo.ver + "\">\n");
            if (xml != null) writer.write(xml.toString()); else writer.write(byteArrayOutputStream.toString());
            writer.write("</jpatch>\n");
            writer.close();
            MainFrame.getInstance().getUndoManager().setChange(false);
            if (MainFrame.getInstance().getAnimation() != null) MainFrame.getInstance().getAnimation().setFile(file); else MainFrame.getInstance().getModel().setFile(file);
            MainFrame.getInstance().setFilename(file.getName());
            return true;
        } catch (IOException ioException) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), "Unable to save file \"" + filename + "\"\n" + ioException, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
