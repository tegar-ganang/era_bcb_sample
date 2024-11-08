package foa.apps;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
 * @author Fabio Giannetti
 * @version 0.0.1
*/
public class SaveAsAction extends AbstractAction {

    private String fileName;

    private Container container;

    private XSLTWriter writer;

    private MainMenuBar menu;

    public SaveAsAction(Container container, XSLTWriter writer, MainMenuBar menu) {
        super("Save As...", null);
        this.container = container;
        this.writer = writer;
        this.menu = menu;
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        FoaFileFilter filter = new FoaFileFilter("xsl", "eXtensible Stylesheet Language Files");
        fc.setFileFilter(filter);
        fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int action = fc.showSaveDialog(container);
        if (action == JFileChooser.APPROVE_OPTION) {
            fileName = fc.getSelectedFile().getAbsolutePath();
            if (!fileName.endsWith(".xsl")) fileName += ".xsl";
            if ((new File(fileName)).exists()) {
                int result = JOptionPane.showConfirmDialog(container, "This file already exists ! Overwrite ?", "File Already Exists", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == 0) {
                    menu.setCurrentProjectFileName(fileName);
                    String fileSep = System.getProperty("file.separator");
                    writer.setAbsolutePath(fileName.substring(0, fileName.lastIndexOf(fileSep) + 1));
                    try {
                        writer.writeXSLFile(fileName);
                        JOptionPane.showMessageDialog(container, "Project File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        System.out.println("Writing error");
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(container, "Project Saving Aborted !", "Message", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                menu.setCurrentProjectFileName(fileName);
                String fileSep = System.getProperty("file.separator");
                writer.setAbsolutePath(fileName.substring(0, fileName.lastIndexOf(fileSep) + 1));
                try {
                    writer.writeXSLFile(fileName);
                    JOptionPane.showMessageDialog(container, "Project File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(container, "Error: Project File NOT Saved !", "Error !", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
}