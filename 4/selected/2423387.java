package com.endlessloopsoftware.elsutils.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * Endless Loop Software Utilities
 * Copyright (c) 2003, Endless Loop Software, Inc.
 *
 *  @author     $Author: schoaff $
 *  @date      	$Date: 2006-03-09 09:42:46 -0500 (Thu, 09 Mar 2006) $
 *  @version    $Id: FileHelpers.java 2 2006-03-09 14:42:46Z schoaff $
 *
 */
public class FileHelpers {

    /****
     * Select a directory in which to store project related files
     * @param title Title to place in file chooser dialog
     * @param filename default filename
     * @param filetype type of file for error messages
     * @param suffix file suffix for filter
     * @param startDir default directory
     * @param parent parent frame for error messages
     * @param enclosingDir true iff we should surround new file with a new folder of same name
     * @return created file
     * @throws FileCreateException
     */
    public static File newFile(String title, String filename, String filetype, String suffix, File startDir, JFrame parent, boolean enclosingDir) throws FileCreateException {
        JFileChooser jNewStudyChooser = new JFileChooser();
        File newFile = null;
        File dirFile;
        String projectPath = null;
        String projectName = null;
        FileFilter newFileFilter = new ExtensionFileFilter(title, suffix);
        jNewStudyChooser.addChoosableFileFilter(newFileFilter);
        jNewStudyChooser.setDialogTitle(title);
        jNewStudyChooser.setSelectedFile(new File(filename));
        if (startDir == null) {
            jNewStudyChooser.setCurrentDirectory(new File("./"));
        } else {
            jNewStudyChooser.setCurrentDirectory(startDir);
        }
        try {
            if (JFileChooser.APPROVE_OPTION == jNewStudyChooser.showSaveDialog(parent)) {
                projectPath = jNewStudyChooser.getSelectedFile().getParent();
                projectName = jNewStudyChooser.getSelectedFile().getName();
                if (enclosingDir) {
                    if (projectName.indexOf(".") != -1) {
                        projectName = projectName.substring(0, projectName.indexOf("."));
                    }
                    try {
                        String folder = projectPath.substring(projectPath.lastIndexOf(File.separator) + 1);
                        if (!folder.equals(projectName)) {
                            dirFile = new File(projectPath, projectName);
                            dirFile.mkdir();
                            projectPath = dirFile.getPath();
                        }
                    } catch (SecurityException e) {
                        JOptionPane.showMessageDialog(parent, "Unable to create directory.", "New File Error", JOptionPane.ERROR_MESSAGE);
                        throw new FileCreateException(false);
                    }
                }
                newFile = new File(projectPath, projectName);
                newFile = ((ExtensionFileFilter) newFileFilter).getCorrectFileName(newFile);
                try {
                    if (!newFile.createNewFile()) {
                        int confirm = JOptionPane.showConfirmDialog(parent, "<HTML><h2>" + filetype + " File already exists at this location.</h2>" + "<p>Shall I overwrite it?</p></html>", "Overwrite " + filetype + " File", JOptionPane.OK_CANCEL_OPTION);
                        if (confirm != JOptionPane.OK_OPTION) {
                            throw new FileCreateException(false);
                        }
                    }
                } catch (IOException ex) {
                    throw new FileCreateException(true);
                }
            }
        } catch (FileCreateException e) {
            if (e.report) {
                JOptionPane.showMessageDialog(parent, "Unable to create " + filetype + " file.");
            }
            newFile = null;
        }
        return newFile;
    }

    /****
     * Remove commas for printing strings to csv files
     * @param s string to format
     * @return formatted string
     */
    public static String formatForCSV(String s) {
        if (s != null) {
            while (s.indexOf(",") != -1) {
                s = s.substring(0, s.indexOf(",")) + s.substring(s.indexOf(",") + 1);
            }
            while (s.indexOf(" ") != -1) {
                s = s.substring(0, s.indexOf(" ")) + "_" + s.substring(s.indexOf(" ") + 1);
            }
        }
        return s;
    }

    /**
     *
     *
     * @param filename param
     *
     * @return returns
     *
     * @throws IOException throws
     * @throws FileNotFoundException throws
     */
    public static String readFile(File f) throws IOException, FileNotFoundException {
        long n = f.length();
        char[] cbuf = new char[(int) n];
        FileReader fr = new FileReader(f);
        fr.read(cbuf);
        fr.close();
        return (new String(cbuf));
    }
}
