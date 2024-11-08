package repokeep.controls;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.image.*;
import java.awt.event.*;
import repokeep.dataitems.*;

/**
 * The main class
 * 
 * @author JLA
 */
public class RepoKeepMain extends javax.swing.JFrame {

    public static final String REPO_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n";

    private PackageListBoxModel myPackageListBoxModel = new PackageListBoxModel();

    private RKRepository myRepository = new RKRepository();

    private java.util.List<RepoKeepEditField> myEditFieldList = new java.util.ArrayList<RepoKeepEditField>();

    /** Creates new form InstaRepoMain */
    public RepoKeepMain() {
        initComponents();
        myAppListBox.setModel(myPackageListBoxModel);
        setupPanel();
    }

    private void initComponents() {
        myPackagePopupMenu = new javax.swing.JPopupMenu();
        myPopupAddPackageMenuItem = new javax.swing.JMenuItem();
        myPopupEditPackageMenuItem = new javax.swing.JMenuItem();
        myFileChooser = new javax.swing.JFileChooser();
        myMainSplitPane = new javax.swing.JSplitPane();
        myAppScrollPane = new javax.swing.JScrollPane();
        myAppListBox = new javax.swing.JList();
        myCenterPanel = new javax.swing.JPanel();
        myNorthPanel = new javax.swing.JPanel();
        myDisclaimerLabel = new javax.swing.JLabel();
        myMenuBar = new javax.swing.JMenuBar();
        myFileMenu = new javax.swing.JMenu();
        mySaveRepositoryMenuItem = new javax.swing.JMenuItem();
        myLoadRepositoryMenuItem = new javax.swing.JMenuItem();
        myImportInstallerMenuItem = new javax.swing.JMenuItem();
        myExportInstallerMenuItem = new javax.swing.JMenuItem();
        myPackageMenu = new javax.swing.JMenu();
        myAddPackageMenuItem = new javax.swing.JMenuItem();
        myEditPackageMenuItem = new javax.swing.JMenuItem();
        myPopupAddPackageMenuItem.setText("Add new package");
        myPopupAddPackageMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myPopupAddPackageMenuItemActionPerformed(evt);
            }
        });
        myPackagePopupMenu.add(myPopupAddPackageMenuItem);
        myPopupEditPackageMenuItem.setText("Edit package");
        myPopupEditPackageMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myPopupEditPackageMenuItemActionPerformed(evt);
            }
        });
        myPackagePopupMenu.add(myPopupEditPackageMenuItem);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("RepoKeep - Installer Repository Editor");
        myMainSplitPane.setDividerLocation(150);
        myAppListBox.setComponentPopupMenu(myPackagePopupMenu);
        myAppListBox.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        myAppListBox.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                myAppListBoxMousePressed(evt);
            }
        });
        myAppScrollPane.setViewportView(myAppListBox);
        myMainSplitPane.setLeftComponent(myAppScrollPane);
        myCenterPanel.setLayout(new java.awt.GridBagLayout());
        myMainSplitPane.setRightComponent(myCenterPanel);
        getContentPane().add(myMainSplitPane, java.awt.BorderLayout.CENTER);
        myDisclaimerLabel.setText("Warning : this version has tons of bugs.  It's best not to overwrite anything with it.");
        myNorthPanel.add(myDisclaimerLabel);
        getContentPane().add(myNorthPanel, java.awt.BorderLayout.PAGE_START);
        myFileMenu.setText("File");
        mySaveRepositoryMenuItem.setText("Save repository");
        mySaveRepositoryMenuItem.setEnabled(false);
        myFileMenu.add(mySaveRepositoryMenuItem);
        myLoadRepositoryMenuItem.setText("Load repository");
        myLoadRepositoryMenuItem.setEnabled(false);
        myFileMenu.add(myLoadRepositoryMenuItem);
        myImportInstallerMenuItem.setText("Import Installer XML");
        myImportInstallerMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myImportInstallerMenuItemActionPerformed(evt);
            }
        });
        myFileMenu.add(myImportInstallerMenuItem);
        myExportInstallerMenuItem.setText("Export to Installer XML");
        myExportInstallerMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myExportInstallerMenuItemActionPerformed(evt);
            }
        });
        myFileMenu.add(myExportInstallerMenuItem);
        myMenuBar.add(myFileMenu);
        myPackageMenu.setText("Package");
        myAddPackageMenuItem.setText("Add package");
        myAddPackageMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myAddPackageMenuItemActionPerformed(evt);
            }
        });
        myPackageMenu.add(myAddPackageMenuItem);
        myEditPackageMenuItem.setText("Edit package");
        myEditPackageMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myEditPackageMenuItemActionPerformed(evt);
            }
        });
        myPackageMenu.add(myEditPackageMenuItem);
        myMenuBar.add(myPackageMenu);
        setJMenuBar(myMenuBar);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 938) / 2, (screenSize.height - 624) / 2, 938, 624);
    }

    private void myPopupEditPackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        editSelectedPackage();
    }

    private void myAppListBoxMousePressed(java.awt.event.MouseEvent evt) {
    }

    private void myPopupAddPackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        addNewPackage();
    }

    private void myImportInstallerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        int zResult = myFileChooser.showOpenDialog(this);
        if (zResult == JFileChooser.APPROVE_OPTION) {
            setRepository(RepoKeepXMLUtil.importRepository(myFileChooser.getSelectedFile()));
            updatePackageListBox();
        }
        int zPackageCount = myRepository.getPackageList().size();
        System.out.println("" + zPackageCount + " packages");
    }

    private void myExportInstallerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            int zResult = myFileChooser.showSaveDialog(this);
            if (zResult == JFileChooser.APPROVE_OPTION) {
                boolean zProceed = true;
                if (myFileChooser.getSelectedFile().exists()) {
                    int zOverwriteResult = JOptionPane.showConfirmDialog(this, "" + myFileChooser.getSelectedFile().getName() + " exists.  Overwrite?", "File already exists", JOptionPane.YES_NO_OPTION);
                    if (zOverwriteResult != JOptionPane.YES_OPTION) {
                        zProceed = false;
                    }
                }
                if (zProceed == true) {
                    String zXMLString = REPO_XML_HEADER + myRepository.toXMLString();
                    FileWriter zFW = new FileWriter(myFileChooser.getSelectedFile());
                    zFW.write(zXMLString);
                    zFW.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void myAddPackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        addNewPackage();
    }

    private void myEditPackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        editSelectedPackage();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new RepoKeepMain().setVisible(true);
            }
        });
    }

    private void editSelectedPackage() {
        if (myAppListBox.getSelectedValue() instanceof RKPackage) {
            editPackage((RKPackage) myAppListBox.getSelectedValue());
        }
    }

    private void editPackage(RKPackage aPackage) {
        RepoKeepPackageEditor.runPackageEditor(this, aPackage);
    }

    private void addNewPackage() {
        myRepository.getPackageList().add(new RKPackage());
        updatePackageListBox();
    }

    private void updatePackageListBox() {
        myPackageListBoxModel.updateListBox();
    }

    public void setRepository(RKRepository aRepository) {
        myRepository = aRepository;
        myEditFieldList.clear();
        setupPanel();
    }

    private void setupPanel() {
        myCenterPanel.removeAll();
        myEditFieldList.clear();
        myEditFieldList.add(RepoKeepEditField.addEditField("Source name", myCenterPanel, 2, myRepository.getValueMap(), RKRepository.KEY_NAME));
        myEditFieldList.add(RepoKeepEditField.addEditField("Source description", myCenterPanel, 3, myRepository.getValueMap(), RKRepository.KEY_DESCRIPTION));
        myEditFieldList.add(RepoKeepEditField.addEditField("Source category", myCenterPanel, 4, myRepository.getValueMap(), RKRepository.KEY_CATEGORY));
        myEditFieldList.add(RepoKeepEditField.addEditField("Maintainer", myCenterPanel, 5, myRepository.getValueMap(), RKRepository.KEY_MAINTAINER));
        myEditFieldList.add(RepoKeepEditField.addEditField("Source email", myCenterPanel, 6, myRepository.getValueMap(), RKRepository.KEY_CONTACT));
        myEditFieldList.add(RepoKeepEditField.addEditField("Source 'More Info' URL", myCenterPanel, 7, myRepository.getValueMap(), RKRepository.KEY_URL));
        myEditFieldList.add(RepoKeepEditField.addEditField("Sponsor", myCenterPanel, 8, myRepository.getValueMap(), RKRepository.KEY_SPONSOR));
        myCenterPanel.validate();
        myCenterPanel.repaint();
    }

    private javax.swing.JMenuItem myAddPackageMenuItem;

    private javax.swing.JList myAppListBox;

    private javax.swing.JScrollPane myAppScrollPane;

    private javax.swing.JPanel myCenterPanel;

    private javax.swing.JLabel myDisclaimerLabel;

    private javax.swing.JMenuItem myEditPackageMenuItem;

    private javax.swing.JMenuItem myExportInstallerMenuItem;

    private javax.swing.JFileChooser myFileChooser;

    private javax.swing.JMenu myFileMenu;

    private javax.swing.JMenuItem myImportInstallerMenuItem;

    private javax.swing.JMenuItem myLoadRepositoryMenuItem;

    private javax.swing.JSplitPane myMainSplitPane;

    private javax.swing.JMenuBar myMenuBar;

    private javax.swing.JPanel myNorthPanel;

    private javax.swing.JMenu myPackageMenu;

    private javax.swing.JPopupMenu myPackagePopupMenu;

    private javax.swing.JMenuItem myPopupAddPackageMenuItem;

    private javax.swing.JMenuItem myPopupEditPackageMenuItem;

    private javax.swing.JMenuItem mySaveRepositoryMenuItem;

    private class PackageListBoxModel extends AbstractListModel {

        public int getSize() {
            return myRepository.getPackageList().size();
        }

        public Object getElementAt(int index) {
            return myRepository.getPackageList().get(index);
        }

        public void updateListBox() {
            this.fireContentsChanged(this, 0, getSize());
        }
    }
}
