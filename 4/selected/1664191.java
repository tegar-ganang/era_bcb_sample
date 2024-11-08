package commands;

import brickcad.Brick;
import brickcad.Command;
import brickcad.ParentAppFrame;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class SaveModel extends Command {

    protected JFileChooser fc;

    protected File file = null;

    protected int returnVal;

    protected FileOutputStream output;

    protected ObjectOutputStream object_out;

    private ParentAppFrame parent;

    public SaveModel(Brick theModel, ParentAppFrame parent) {
        this.theModel = theModel;
        this.parent = parent;
        fc = new JFileChooser();
    }

    public SaveModel() {
        this(null, null);
    }

    @Override
    public void execute() {
        if (theModel.getFile() == null) {
            if (saveDialog() == false) return;
            theModel.setFile(file);
        }
        theModel.setChanged(false);
        saveFile();
    }

    protected boolean saveDialog() {
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

    protected void saveFile() {
        try {
            output = new FileOutputStream(theModel.getFile());
            object_out = new ObjectOutputStream(output);
            object_out.writeObject(theModel);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    protected boolean saveChanges() {
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
