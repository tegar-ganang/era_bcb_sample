package org.proteinshader.gui.utils;

import org.proteinshader.gui.enums.ImageFormatEnum;
import javax.swing.*;
import javax.swing.event.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;

/*******************************************************************************
Extends JFileChooser by requiring that an ImageFormatEnum be given to the 
constructor.

<br /><br />
The ImageFormatEnum is used to add a file filter, and can also be used to add
the correct filename extension to a file to be saved.
*******************************************************************************/
public class ImageFileChooser extends JFileChooser {

    /** The default filename for saving a file is canvas. */
    public static final String DEFAULT_FILENAME = "canvas";

    private ImageFormatEnum m_format;

    /***************************************************************************
    Constructs a ImageFileChooser.

    @param format     an image file format.
    @param directory  the directory to open the chooser in.
    ***************************************************************************/
    public ImageFileChooser(ImageFormatEnum format, String directory) {
        this(format, new File(directory));
    }

    /***************************************************************************
    Constructs a ImageFileChooser.

    @param format     an image file format.
    @param directory  the directory to open the chooser in.
    ***************************************************************************/
    public ImageFileChooser(ImageFormatEnum format, File directory) {
        super(directory);
        m_format = format;
        setFileFilter(new FileExtensionFilter(m_format.getExtension()));
    }

    /***************************************************************************
    Sets the default filename for saving a file and then calls on the
    showSaveDialog(c) of the super class, JFileChooser, to pop open a 
    "Save File" file chooser dialog.
    
    @param c  the parent component.
    @return  The return state on popdown can be one of three JFileChooser
             constants: APPROVE_OPTION, CANCEL_OPTION, or ERROR_OPTION.
    ***************************************************************************/
    public int showSaveDialog(Component c) {
        String filename = DEFAULT_FILENAME + m_format.getExtension();
        File file = new File(this.getCurrentDirectory(), filename);
        setSelectedFile(file);
        return super.showSaveDialog(c);
    }

    /***************************************************************************
    After calling on getSelectedFile() of the JFileChooser parent class, this
    method will check that the filename has the correct extension based on the
    image format type (unless the file is null, in which case null will still
    be returned).

    @return The selected file with the correct extension.
    ***************************************************************************/
    public File getSelectedFileWithFormatExt() {
        try {
            File file = getSelectedFile();
            file = addExtensionIfNeeded(file);
            if (file != null && file.exists()) {
                int option = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "File Already Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (option != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
            return file;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "The filename could not be resolved.\n" + ioe.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    /***************************************************************************
    Checks if a file has the correct 3 letter extension after the last dot
    (based on the correct extension for the format set by the constructor).

    <br /><br />
    If the file given as an argument is null, this method will return null.
    
    @param file  the file to save.
    @return  The file with the correct extension for the image format.
    @throws IOException  if an error occurs while trying to obtain the
                         cannonical path for the file.
    ***************************************************************************/
    private File addExtensionIfNeeded(File file) throws IOException {
        if (file != null) {
            String path = file.getCanonicalPath(), extension = m_format.getExtension();
            if (!path.endsWith(extension)) {
                if (extension.equals(".jpg") && path.endsWith(".jpeg")) {
                    path = path.substring(0, path.length() - 5);
                }
                file = new File(path + extension);
            }
        }
        return file;
    }
}
