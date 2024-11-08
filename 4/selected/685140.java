package commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import brickcad.Brick;
import brickcad.Command;
import brickcad.ParentAppFrame;

public class NewModel extends Command {

    private JFileChooser fc;

    private File file = null;

    private int returnVal;

    private FileOutputStream output;

    private ObjectOutputStream object_out;

    private ParentAppFrame parent;

    public NewModel(Brick theModel, ParentAppFrame parent) {
        this.theModel = theModel;
        this.parent = parent;
        fc = new JFileChooser();
    }

    public NewModel() {
        this(null, null);
    }

    public void execute() {
        if (saveChanges() == false) return;
        parent.setBrick(new Brick());
        parent.update();
    }

    private boolean saveDialog() {
        int overwriteReturnValue = -1;
        returnVal = fc.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            if (file.exists()) {
                overwriteReturnValue = JOptionPane.showConfirmDialog(parent, file.toString() + " already exists.\nDo you want to replace it?", "File Already Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            }
            if (overwriteReturnValue == JOptionPane.NO_OPTION) {
                return false;
            } else return true;
        } else return false;
    }

    private void saveFile() {
        try {
            output = new FileOutputStream(theModel.getFile());
            object_out = new ObjectOutputStream(output);
            object_out.writeObject(theModel);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private boolean saveChanges() {
        String fileName = "";
        int optionalSave;
        if (theModel.isChanged() == true) {
            if (theModel.getFile() == null) fileName = "Untitled"; else fileName = theModel.getFile().toString();
            optionalSave = JOptionPane.showConfirmDialog(parent, "Do you want to save " + "the changes made to " + fileName + "?", "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (optionalSave == JOptionPane.YES_OPTION) {
                if (saveDialog() == false) return false;
                theModel.setFile(file);
                theModel.setChanged(false);
                saveFile();
            } else if (optionalSave == JOptionPane.CANCEL_OPTION) return false;
        }
        return true;
    }
}
