package org.openscience.jmol;

import javax.swing.*;

public class RecentFilesDialog extends JDialog implements java.awt.event.WindowListener, java.awt.event.ActionListener {

    private static JmolResourceHandler jrh;

    private boolean ready = false;

    private String fileName = "BUG";

    private String fileType = "BUG";

    private static final int MAX_FILES = 10;

    private JButton okBut;

    private String[] files = new String[MAX_FILES];

    private String[] fileTypes = new String[MAX_FILES];

    private JList fileList;

    private static JmolResourceHandler rch = new JmolResourceHandler("RecentFiles");

    java.util.Properties props;

    /** Creates a hidden recent files dialog **/
    public RecentFilesDialog(java.awt.Frame boss) {
        super(boss, rch.getString("windowTitle"), true);
        props = new java.util.Properties();
        getFiles();
        getContentPane().setLayout(new java.awt.BorderLayout());
        okBut = new JButton(rch.getString("okLabel"));
        okBut.addActionListener(this);
        getContentPane().add("South", okBut);
        fileList = new JList(files);
        fileList.setSelectedIndex(0);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getContentPane().add("Center", fileList);
        setLocationRelativeTo(boss);
        pack();
    }

    private void getFiles() {
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(Jmol.HistoryPropsFile);
            props.load(in);
            for (int i = 0; i < MAX_FILES; i++) {
                files[i] = props.getProperty("recentFilesFile" + i);
                fileTypes[i] = props.getProperty("recentFilesType" + i);
            }
        } catch (java.io.IOException e) {
            System.err.println("RecentFiles: Error opening history!");
        }
    }

    /** 
     * Adds this file and type to the history. If already present,
     * this file is premoted to the top position.  
     */
    public void addFile(String name, String type) {
        int currentPosition = -1;
        for (int i = 0; i < MAX_FILES; i++) {
            if (files[i] != null && files[i].equals(name)) {
                currentPosition = i;
            }
        }
        if (currentPosition == 0) {
            return;
        }
        if (currentPosition > 0) {
            for (int i = currentPosition; i < MAX_FILES - 1; i++) {
                files[i] = files[i + 1];
                fileTypes[i + 1] = fileTypes[i];
            }
        }
        for (int j = MAX_FILES - 2; j >= 0; j--) {
            files[j + 1] = files[j];
            fileTypes[j + 1] = fileTypes[j];
        }
        files[0] = name;
        fileTypes[0] = type;
        fileList.setListData(files);
        fileList.setSelectedIndex(0);
        pack();
        saveList();
    }

    /** Saves the list to the history file. Called automaticaly when files are added **/
    public void saveList() {
        for (int i = 0; i < 10; i++) {
            if (files[i] != null) {
                props.setProperty("recentFilesFile" + i, files[i]);
                props.setProperty("recentFilesType" + i, fileTypes[i]);
            }
        }
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(Jmol.HistoryPropsFile);
            props.store(out, Jmol.HistroyFileHeader);
        } catch (java.io.IOException e) {
            System.err.println("Error saving history!! " + e);
        }
    }

    /** This method will block until a file has been picked.  
     *   @returns String The name of the file picked or null if the action was aborted.
    **/
    public String getFile() {
        while (!ready) {
        }
        ;
        return fileName;
    }

    /** This method will block until a file has been picked.
        @returns String The type of the file picked or null if the action was aborted.
    **/
    public String getFileType() {
        while (!ready) {
        }
        ;
        return fileType;
    }

    public void windowClosing(java.awt.event.WindowEvent e) {
        fileName = null;
        fileType = null;
        hide();
        ready = true;
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        fileName = files[fileList.getSelectedIndex()];
        fileType = fileTypes[fileList.getSelectedIndex()];
        hide();
        ready = true;
    }

    public void windowClosed(java.awt.event.WindowEvent e) {
    }

    public void windowOpened(java.awt.event.WindowEvent e) {
        ready = false;
    }

    public void windowIconified(java.awt.event.WindowEvent e) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent e) {
    }

    public void windowActivated(java.awt.event.WindowEvent e) {
    }

    public void windowDeactivated(java.awt.event.WindowEvent e) {
    }
}
