package be.vds.jtb.taskmanager.controller;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import be.vds.jtb.taskmanager.model.TaskManagerFacade;

public class SaveTasksAsXMLAction extends AbstractAction {

    private JFrame parentFrame;

    private TaskManagerFacade facade;

    public SaveTasksAsXMLAction(JFrame parentFrame, TaskManagerFacade facade) {
        super("Save");
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        this.parentFrame = parentFrame;
        this.facade = facade;
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(parentFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(".xml")) {
                path += ".xml";
            }
            File ff = new File(path);
            boolean canSave = false;
            if (!ff.exists()) {
                try {
                    ff.createNewFile();
                    canSave = true;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                int i = JOptionPane.showConfirmDialog(parentFrame, "This file already exist. Do you want to overwrite?", "File already exist", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                canSave = i == JOptionPane.OK_OPTION;
            }
            if (canSave) facade.saveTasks(ff);
        }
    }
}
