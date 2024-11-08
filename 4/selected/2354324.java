package uk.org.sgj.SGJNifty.Files;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.io.IOException;
import java.util.*;
import java.io.*;

public class FileUtils {

    public static File selectFileToOpen(String extDescription, String extension, File lastDir, File lastFile) {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(extDescription, extension);
        fc.setFileFilter(filter);
        if (lastDir == null) {
            lastDir = new File(java.lang.System.getProperties().getProperty("user.dir"));
        }
        fc.setCurrentDirectory(lastDir);
        if (lastFile != null) {
            fc.setSelectedFile(lastFile);
        }
        int returnVal = fc.showOpenDialog(null);
        File file = null;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            String fname = file.getPath();
            if (!file.canRead()) {
                JOptionPane.showMessageDialog(null, "This file is not readable; we can't open it.", "Can't open file!", JOptionPane.ERROR_MESSAGE);
                file = null;
            }
        }
        return (file);
    }

    public static File selectFileToSave(String extDescription, String extension, File lastDir, File lastFile) {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(extDescription, extension);
        fc.setFileFilter(filter);
        fc.setCurrentDirectory(lastDir);
        if (lastFile != null) {
            fc.setSelectedFile(lastFile);
        }
        int returnVal = fc.showSaveDialog(null);
        File file = null;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            String fname = file.getPath();
            if (!fname.endsWith("." + extension)) {
                file = new File(fname.concat("." + extension));
            }
            if (file.exists()) {
                if (file.canWrite()) {
                    int choice = JOptionPane.showConfirmDialog(null, "This file already exists--do you want to overwrite it?", "Overwrite existing file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) {
                        file = null;
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "This file already exists and is read-only; we can't overwrite it.", "Can't save!", JOptionPane.ERROR_MESSAGE);
                    file = null;
                }
            }
        }
        return (file);
    }
}
