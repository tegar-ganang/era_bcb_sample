package foa.apps;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 * @author Fabio Giannetti
 * @version 0.0.1
*/
public class SaveSettingsAction extends AbstractAction {

    private String fileName;

    private Container container;

    private FOACfgWriter writer;

    private MainMenuBar menu;

    public SaveSettingsAction(Container container, MainMenuBar menu) {
        super("Save Settings...", null);
        this.container = container;
        this.writer = new FOACfgWriter(menu);
        this.menu = menu;
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        FoaFileFilter filter = new FoaFileFilter("foa", "FOA Configuration Files");
        fc.setFileFilter(filter);
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int action = fc.showSaveDialog(container);
        if (action == JFileChooser.APPROVE_OPTION) {
            fileName = fc.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".foa")) fileName += ".foa";
            if ((new File(fileName)).exists()) {
                int result = JOptionPane.showConfirmDialog(container, "This file already exists ! Overwrite ?", "File Already Exists", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == 0) {
                    try {
                        writer.writeCfgFile(fileName);
                        JOptionPane.showMessageDialog(container, "Configuration File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        System.out.println("Writing error");
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(container, "Configuration File Saving Aborted !", "Message", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                try {
                    writer.writeCfgFile(fileName);
                    JOptionPane.showMessageDialog(container, "Configuration File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(container, "Error: Configuration File NOT Saved !", "Error !", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
}
