package se.sics.cooja.mspmote.interfaces;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.*;
import se.sics.cooja.mspmote.SkyMote;

/**
 * @author Fredrik Osterlind
 */
@ClassDescription("M25P80 Flash")
public class SkyFlash extends MoteInterface {

    private static Logger logger = Logger.getLogger(SkyFlash.class);

    public int SIZE = 1024 * 1024;

    private SkyMote mote = null;

    protected CoojaM25P80 m24p80 = null;

    public SkyFlash(Mote mote) {
        this.mote = (SkyMote) mote;
        this.m24p80 = new CoojaM25P80(this.mote.getCPU());
        this.mote.skyNode.setFlash(this.m24p80);
    }

    /**
   * Write ID header to start of flash.
   *
   * @param id ID
   */
    public void writeIDheader(int id) {
        byte[] idHeader = new byte[4];
        idHeader[0] = (byte) 0xad;
        idHeader[1] = (byte) 0xde;
        idHeader[2] = (byte) (id >> 8);
        idHeader[3] = (byte) (id & 0xff);
        try {
            m24p80.seek(0);
            m24p80.write(idHeader);
        } catch (IOException e) {
            logger.fatal("Exception when writing ID header: " + e);
        }
    }

    public JPanel getInterfaceVisualizer() {
        JPanel panel = new JPanel();
        final JButton uploadButton = new JButton("Upload file");
        panel.add(uploadButton);
        final JButton downloadButton = new JButton("Store to file");
        panel.add(downloadButton);
        if (GUI.isVisualizedInApplet()) {
            uploadButton.setEnabled(false);
            uploadButton.setToolTipText("Not available in applet mode");
            downloadButton.setEnabled(false);
            downloadButton.setToolTipText("Not available in applet mode");
        }
        uploadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                byte[] fileData = readDialogFileBytes(GUI.getTopParentContainer());
                if (fileData != null) {
                    if (fileData.length > CoojaM25P80.SIZE) {
                        logger.fatal("Too large data file: " + fileData.length + " > " + CoojaM25P80.SIZE);
                        return;
                    }
                    try {
                        m24p80.seek(0);
                        m24p80.write(fileData);
                        logger.info("Done! (" + fileData.length + " bytes written to Flash)");
                    } catch (IOException ex) {
                        logger.fatal("Exception: " + ex);
                    }
                }
            }
        });
        downloadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    byte[] data = new byte[CoojaM25P80.SIZE];
                    m24p80.seek(0);
                    m24p80.readFully(data);
                    writeDialogFileBytes(GUI.getTopParentContainer(), data);
                } catch (IOException ex) {
                    logger.fatal("Data download failed: " + ex.getMessage(), ex);
                }
            }
        });
        Observer observer;
        this.addObserver(observer = new Observer() {

            public void update(Observable obs, Object obj) {
            }
        });
        panel.putClientProperty("intf_obs", observer);
        return panel;
    }

    public void releaseInterfaceVisualizer(JPanel panel) {
        Observer observer = (Observer) panel.getClientProperty("intf_obs");
        if (observer == null) {
            logger.fatal("Error when releasing panel, observer is null");
            return;
        }
        this.deleteObserver(observer);
    }

    public Collection<Element> getConfigXML() {
        return null;
    }

    public void setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    }

    public static void writeDialogFileBytes(Component parent, byte[] data) {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showSaveDialog(GUI.getTopParentContainer());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File saveFile = fc.getSelectedFile();
        if (saveFile.exists()) {
            String s1 = "Overwrite";
            String s2 = "Cancel";
            Object[] options = { s1, s2 };
            int n = JOptionPane.showOptionDialog(GUI.getTopParentContainer(), "A file with the same name already exists.\nDo you want to remove it?", "Overwrite existing file?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, s1);
            if (n != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (saveFile.exists() && !saveFile.canWrite()) {
            logger.fatal("No write access to file: " + saveFile);
            return;
        }
        try {
            FileOutputStream outStream = new FileOutputStream(saveFile);
            outStream.write(data);
            outStream.close();
        } catch (Exception ex) {
            logger.fatal("Could not write to file: " + saveFile);
            return;
        }
    }

    /**
   * Opens a file dialog and returns the contents of the selected file or null if dialog aborted.
   *
   * @param parent Dialog parent, may be null
   * @return Binary contents of user selected file
   */
    public static byte[] readDialogFileBytes(Container parent) {
        File file = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new java.io.File("."));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Select data file");
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
        } else {
            return null;
        }
        long fileSize = file.length();
        byte[] fileData = new byte[(int) fileSize];
        FileInputStream fileIn;
        DataInputStream dataIn;
        int offset = 0;
        int numRead = 0;
        try {
            fileIn = new FileInputStream(file);
            dataIn = new DataInputStream(fileIn);
            while (offset < fileData.length && (numRead = dataIn.read(fileData, offset, fileData.length - offset)) >= 0) {
                offset += numRead;
            }
            dataIn.close();
            fileIn.close();
        } catch (Exception ex) {
            logger.debug("Exception ex: " + ex);
            return null;
        }
        return fileData;
    }
}
