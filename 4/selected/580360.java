package com.apelon.beans.apelfilechooser;

import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

/**
 * Customized file chooser.
 * 
 * @author: matt Munz
 */
public class ApelFileChooser extends JFileChooser {

    public ApelFileChooser() {
        super();
    }

    public ApelFileChooser(File currentDirectory) {
        super(currentDirectory);
    }

    public ApelFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
    }

    public ApelFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
    }

    public ApelFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
    }

    public ApelFileChooser(FileSystemView fsv) {
        super(fsv);
    }

    /**
   * Creates a platform-appropriate file chooser
   * contains platform-specific code to work around a bug that should be fixed in java 1.4
   *
   * @author: Matt Munz
   */
    public static ApelFileChooser createChooser(String path) {
        if (osIsWindows()) {
            return new ApelFileChooser(path, new WindowsAltFileSystemView());
        } else {
            return new ApelFileChooser(path);
        }
    }

    public static boolean osIsWindows() {
        return ((System.getProperty("os.name")).indexOf("Windows") != -1);
    }

    /**
   * Overrides approveSelection to put custom checks.
   *
   * checks to see if file has no extension.
   * if so, tries to append extension and use that. 
   *
   * @author: Matt Munz
   */
    public void approveSelection() {
        if (!accept(getSelectedFile())) {
            if (getExtension(getSelectedFile()) == null) {
                if (getFileFilter() instanceof ApelFileFilter) {
                    ApelFileFilter filter = (ApelFileFilter) getFileFilter();
                    Enumeration extensions = filter.acceptedExtensions().elements();
                    if (extensions.hasMoreElements()) {
                        String curExtension = (String) extensions.nextElement();
                        String filePath = getSelectedFile().getPath() + "." + curExtension.toLowerCase();
                        setSelectedFile(new File(filePath));
                        approveSelection();
                        return;
                    }
                }
            }
            String message = "File type should be " + getFileFilter().getDescription();
            String title = "Invalid File Type";
            JOptionPane.showMessageDialog(getParent(), message, title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (getDialogType() == SAVE_DIALOG) {
            if (!canSaveFile(getSelectedFile())) {
                return;
            }
        }
        super.approveSelection();
    }

    public String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
   * Checks whether the file can be saved.
   * @return boolean
   */
    protected boolean canSaveFile(File file) {
        if (file.exists()) {
            if (file.canWrite()) {
                if (JOptionPane.showConfirmDialog(getParent(), "File already exists. Do you want to overwrite it?", "File Exists", JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
                    return true;
                } else {
                    return false;
                }
            } else {
                JOptionPane.showMessageDialog(getParent(), "Cannot write to file " + file.getName(), "Check File ", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
   * Overides method from super.
   * @param file File
   */
    public void setSelectedFile(File file) {
        File f = file;
        if (getDialogType() == SAVE_DIALOG) {
            if (file.isDirectory()) {
                f = getFileSystemView().createFileObject(file, getSelectedFile().getName());
            }
        }
        super.setSelectedFile(f);
    }
}
