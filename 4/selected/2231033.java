package net.sourceforge.pplay.digraphmain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import net.sourceforge.pplay.digraph.*;
import net.sourceforge.pplay.digraphfile.*;
import net.sourceforge.pplay.digraphview.*;

/**
 *
 * @author  Zach Heath
 * @version
 */
public class FrameUtility {

    /** Creates new FrameUtility */
    public FrameUtility() {
    }

    public static void loadDigraph(mainFrame parentFrame, DigraphView digraphView) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(parentFrame.getFilePath()));
        fileChooser.setFileFilter(new DigraphFileFilter());
        int returnCondition = fileChooser.showOpenDialog(parentFrame);
        if (returnCondition == JFileChooser.APPROVE_OPTION) {
            File newfile = fileChooser.getSelectedFile();
            parentFrame.setFilePath(newfile.getPath());
            DigraphFile digraphFile = new DigraphFile();
            try {
                Digraph newDigraph = digraphFile.loadDigraph(newfile);
                digraphView.setDigraph(newDigraph);
                parentFrame.setSavedOnce(true);
                digraphView.setDigraphDirty(false);
            } catch (DigraphFileException exep) {
                JOptionPane.showMessageDialog(parentFrame, "Error Loading File:\n" + exep.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            } catch (DigraphException exep) {
                JOptionPane.showMessageDialog(parentFrame, "Error Setting new Digraph:\n" + exep.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (returnCondition == JFileChooser.CANCEL_OPTION) {
        } else {
            ;
        }
    }

    public static void saveDigraph(mainFrame parentFrame, DigraphView digraphView, File tobeSaved) {
        DigraphFile digraphFile = new DigraphFile();
        DigraphTextFile digraphTextFile = new DigraphTextFile();
        try {
            if (!DigraphFile.DIGRAPH_FILE_EXTENSION.equals(getExtension(tobeSaved))) {
                tobeSaved = new File(tobeSaved.getPath() + "." + DigraphFile.DIGRAPH_FILE_EXTENSION);
            }
            File dtdFile = new File(tobeSaved.getParent() + "/" + DigraphFile.DTD_FILE);
            if (!dtdFile.exists()) {
                File baseDigraphDtdFile = parentFrame.getDigraphDtdFile();
                if (baseDigraphDtdFile != null && baseDigraphDtdFile.exists()) {
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dtdFile));
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(baseDigraphDtdFile));
                        while (bis.available() > 1) {
                            bos.write(bis.read());
                        }
                        bis.close();
                        bos.close();
                    } catch (IOException ex) {
                        System.out.println("Unable to Write Digraph DTD File: " + ex.getMessage());
                    }
                } else {
                    System.out.println("Unable to Find Base Digraph DTD File: ");
                }
            }
            Digraph digraph = digraphView.getDigraph();
            digraphFile.saveDigraph(tobeSaved, digraph);
            String fileName = tobeSaved.getName();
            int extensionIndex = fileName.lastIndexOf(".");
            if (extensionIndex > 0) {
                fileName = fileName.substring(0, extensionIndex + 1) + "txt";
            } else {
                fileName = fileName + ".txt";
            }
            File textFile = new File(tobeSaved.getParent() + "/" + fileName);
            digraphTextFile.saveDigraph(textFile, digraph);
            digraphView.setDigraphDirty(false);
            parentFrame.setFilePath(tobeSaved.getPath());
            parentFrame.setSavedOnce(true);
        } catch (DigraphFileException exep) {
            JOptionPane.showMessageDialog(parentFrame, "Error Saving File:\n" + exep.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        } catch (DigraphException exep) {
            JOptionPane.showMessageDialog(parentFrame, "Error Retrieving Digraph from View:\n" + exep.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void saveDigraph(mainFrame parentFrame, DigraphView digraphView) {
        File tobeSaved;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(parentFrame.getFilePath()));
        fileChooser.setFileFilter(new DigraphFileFilter());
        int returnCondition = fileChooser.showSaveDialog(parentFrame);
        if (returnCondition == JFileChooser.APPROVE_OPTION) {
            File newfile = fileChooser.getSelectedFile();
            saveDigraph(parentFrame, digraphView, newfile);
        } else if (returnCondition == JFileChooser.CANCEL_OPTION) ; else ;
    }

    /**
     * Return the extension portion of the file's name .
     *
     */
    public static String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
            ;
        }
        return null;
    }
}
