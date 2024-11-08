package foa.apps;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;

/**
 * @author Fabio Giannetti
 * @version 0.0.1
*/
public class NewAction extends AbstractAction {

    private String fileName;

    private Container container;

    private MainMenuBar menu;

    private XSLTWriter writer;

    public NewAction(Container container, XSLTWriter writer, MainMenuBar menu) {
        super("New...", null);
        this.container = container;
        this.menu = menu;
        this.writer = writer;
    }

    public void actionPerformed(ActionEvent e) {
        if (menu.activeProject) {
            int resSave = JOptionPane.showConfirmDialog(container, "Current Project NOT Saved ! Save it ?", "Save Project", JOptionPane.YES_NO_CANCEL_OPTION);
            if (resSave == 0) {
                if (menu.currentProjectFileName.equals("")) {
                    JFileChooser fc = new JFileChooser();
                    FoaFileFilter filter = new FoaFileFilter("xsl", "eXtensible Stylesheet Language Files");
                    fc.setFileFilter(filter);
                    fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
                    int action = fc.showSaveDialog(container);
                    if (action == JFileChooser.APPROVE_OPTION) {
                        fileName = fc.getSelectedFile().getAbsolutePath();
                        if (!fileName.endsWith(".xsl")) fileName += ".xsl";
                        if ((new File(fileName)).exists()) {
                            int resOver = JOptionPane.showConfirmDialog(container, "This file already exists ! Overwrite ?", "File Already Exists", JOptionPane.OK_CANCEL_OPTION);
                            if (resOver == 0) {
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
                    } else {
                        menu.activeProject = false;
                        menu.currentProjectFileName = "";
                    }
                } else {
                    String fileSep = System.getProperty("file.separator");
                    writer.setAbsolutePath(fileName.substring(0, fileName.lastIndexOf(fileSep) + 1));
                    try {
                        writer.writeXSLFile(fileName);
                        JOptionPane.showMessageDialog(container, "Project File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(container, "Error: Project File NOT Saved !", "Error !", JOptionPane.ERROR_MESSAGE);
                    }
                    JFileChooser fc = new JFileChooser();
                    FoaFileFilter filter = new FoaFileFilter("xsl", "eXtensible Stylesheet Language Files");
                    fc.setFileFilter(filter);
                    fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
                    int action = fc.showDialog(container, "New Project Name");
                    if (action == JFileChooser.APPROVE_OPTION) {
                        fileName = fc.getSelectedFile().getAbsolutePath();
                        if (!fileName.endsWith(".xsl")) fileName += ".xsl";
                        if ((new File(fileName)).exists()) {
                            int resOver = JOptionPane.showConfirmDialog(container, "This file already exists ! Overwrite ?", "File Already Exists", JOptionPane.OK_CANCEL_OPTION);
                            if (resOver == 0) {
                                JOptionPane.showMessageDialog(container, "Project File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(container, "Project Saving Aborted !", "Message", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                        menu.activeProject = true;
                        menu.currentProjectFileName = fileName;
                        activateItemsButtons();
                        menu.currentProjectPath = fileName.substring(0, fileName.lastIndexOf(System.getProperty("file.separator")) + 1);
                        System.err.println("Project Path: " + menu.currentProjectPath);
                        menu.getFlowDirector().getContentManager().resetContent();
                        menu.getFlowDirector().getContentSequenceManager().resetContentSequences();
                        menu.getAttributeDirector().getAttributeManager().resetAttributes();
                        menu.getLayoutDirector().getPageManager().startPages();
                        menu.getLayoutDirector().getPageSequenceManager().startPageSequences();
                        menu.getBrickDirector().getBrickManager().startBricks();
                    }
                }
            }
        } else {
            JFileChooser fc = new JFileChooser();
            FoaFileFilter filter = new FoaFileFilter("xsl", "eXtensible Stylesheet Language Files");
            fc.setFileFilter(filter);
            fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
            int action = fc.showDialog(container, "New Project Name");
            if (action == JFileChooser.APPROVE_OPTION) {
                fileName = fc.getSelectedFile().getAbsolutePath();
                if (!fileName.endsWith(".xsl")) fileName += ".xsl";
                if ((new File(fileName)).exists()) {
                    int resOver = JOptionPane.showConfirmDialog(container, "This file already exists ! Overwrite ?", "File Already Exists", JOptionPane.OK_CANCEL_OPTION);
                    if (resOver == 0) {
                        JOptionPane.showMessageDialog(container, "Project File Saved !", "Message", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(container, "Project Saving Aborted !", "Message", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                menu.activeProject = true;
                menu.currentProjectFileName = fileName;
                activateItemsButtons();
                menu.currentProjectPath = fileName.substring(0, fileName.lastIndexOf(System.getProperty("file.separator")) + 1);
                System.err.println("Project Path: " + menu.currentProjectPath);
                menu.getFlowDirector().getContentManager().resetContent();
                menu.getFlowDirector().getContentSequenceManager().resetContentSequences();
                menu.getAttributeDirector().getAttributeManager().resetAttributes();
                menu.getLayoutDirector().getPageManager().startPages();
                menu.getLayoutDirector().getPageSequenceManager().startPageSequences();
                menu.getBrickDirector().getBrickManager().startBricks();
            } else {
                menu.activeProject = false;
                menu.currentProjectFileName = "";
            }
        }
    }

    private void activateItemsButtons() {
        menu.closeProjItem.setEnabled(true);
        menu.saveAsProjButton.setEnabled(true);
        menu.saveAsProjItem.setEnabled(true);
        menu.saveProjButton.setEnabled(true);
        menu.saveProjItem.setEnabled(true);
        menu.closeProjItem.setEnabled(true);
        if (menu.xsltSetted) menu.genFOItem.setEnabled(true);
    }
}
