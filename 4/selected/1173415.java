package guiLogics;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import keypair.OpenSave;

/**
 * Saves execution text to a file.
 * Loads execution from a text file to a textfield.
 * 
 * @author Petri Tuononen
 * @since 8.2.2009
 */
public class LoadSaveExec {

    OpenSave openSave;

    JFrame frame;

    JTextArea textArea;

    File file;

    /**
	 * Constructor.
	 * @param frame
	 * @param textArea
	 */
    public LoadSaveExec(JFrame frame, JTextArea textArea) {
        this.frame = frame;
        this.textArea = textArea;
    }

    /**
	 * Saves execution text to a file.
	 */
    public void saveExecToFile() {
        File file = chooseFileToSave();
        if (file != null) {
            String[] splits = file.getName().split("\\.");
            String extension = splits[splits.length - 1];
            if (!extension.equals("txt")) {
                String newFileName = file.getName() + ".txt";
                int length = file.getPath().length() - file.getName().length();
                String newFilePath = file.getPath().substring(0, length) + newFileName;
                file = new File(newFilePath);
            }
            try {
                BufferedWriter fileOut = new BufferedWriter(new FileWriter(file));
                fileOut.write(textArea.getText());
                fileOut.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Writing execution into a file failed.", "File write error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	 * One of the fastest methods to read a file.
	 * Uses FileChannel with direct ByteBuffer and byte array reads.
	 * This method reduces the amount of data copying and enables
	 * the JVM to read new data into the application's own array without
	 * going through multiple intermediate buffers.
	 */
    public void loadExecFromFile() {
        File file = chooseFileToLoad();
        if (file != null) {
            textArea.setText("");
            long file_size = file.length();
            int byteSize = (int) file_size;
            FileInputStream f = null;
            try {
                f = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(frame, "File " + file.getName() + " not found.", "File load error", JOptionPane.ERROR_MESSAGE);
            }
            FileChannel ch = f.getChannel();
            ByteBuffer bb = ByteBuffer.allocateDirect(131072);
            byte[] barray = new byte[byteSize];
            long checkSum = 0L;
            int nRead, nGet;
            try {
                while ((nRead = ch.read(bb)) != -1) {
                    if (nRead == 0) continue;
                    bb.position(0);
                    bb.limit(nRead);
                    while (bb.hasRemaining()) {
                        nGet = Math.min(bb.remaining(), byteSize);
                        bb.get(barray, 0, nGet);
                        for (int i = 0; i < nGet; i++) checkSum += barray[i];
                    }
                    textArea.setText(new String(barray));
                    textArea.setCaretPosition(0);
                    bb.clear();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error in reading a file " + file.getName(), "File read error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	 * Returns the user selected file.
	 * File to load.
	 * @return file File to load.
	 */
    private File chooseFileToLoad() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new ExecutionFileFilter());
        int result = fc.showOpenDialog(frame);
        switch(result) {
            case JFileChooser.APPROVE_OPTION:
                if (fc.getSelectedFile() != null) {
                    file = fc.getSelectedFile();
                } else {
                    JOptionPane.showMessageDialog(frame, "No file was selected", "File selection info", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case JFileChooser.CANCEL_OPTION:
                break;
            case JFileChooser.ERROR_OPTION:
                JOptionPane.showMessageDialog(frame, "An error occured while selecting a file to load", "File selection error", JOptionPane.ERROR_MESSAGE);
                break;
        }
        return file;
    }

    /**
	 * Returns the user selected file.
	 * @return file File to save.
	 */
    private File chooseFileToSave() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new ExecutionFileFilter());
        int result = fc.showSaveDialog(frame);
        switch(result) {
            case JFileChooser.APPROVE_OPTION:
                if (fc.getSelectedFile() != null) {
                    file = fc.getSelectedFile();
                } else {
                    JOptionPane.showMessageDialog(frame, "No file was selected", "File selection info", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case JFileChooser.CANCEL_OPTION:
                break;
            case JFileChooser.ERROR_OPTION:
                JOptionPane.showMessageDialog(frame, "An error occured while selecting a file to save", "File selection error", JOptionPane.ERROR_MESSAGE);
                break;
        }
        return file;
    }
}
