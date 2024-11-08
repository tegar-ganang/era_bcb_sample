package com.ienjinia.vc.tools;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

public class CharEditor extends JFrame implements CharDataListener {

    private CharData charData;

    private ShowScreen showScreen;

    private CharColorSelector colorSelector;

    private CharSelector charSelector;

    private JButton flipVerticalBtn;

    private JButton flipHorizontalBtn;

    private JButton rotateClockwiseBtn;

    private JButton rotateCounterclockwiseBtn;

    private JButton openBtn;

    private JButton saveBtn;

    private JButton saveAsBtn;

    private boolean modified;

    private String filename = "(no name)";

    private File currentDirectory;

    public CharEditor() throws IOException {
        charData = new CharData();
        charData.addCharDataListener(this);
        colorSelector = new CharColorSelector(charData);
        charSelector = new CharSelector(charData);
        showScreen = new ShowScreen(charData, colorSelector, charSelector);
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(showScreen);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(charSelector);
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        flipVerticalBtn = new JButton("Flip Vertical");
        panel2.add(flipVerticalBtn);
        flipHorizontalBtn = new JButton("Flip Horizontal");
        panel2.add(flipHorizontalBtn);
        rotateClockwiseBtn = new JButton("Rotate Clockwise");
        panel2.add(rotateClockwiseBtn);
        rotateCounterclockwiseBtn = new JButton("Rotate Counterclockwise");
        panel2.add(rotateCounterclockwiseBtn);
        panel.add(panel2);
        panel.add(colorSelector);
        c.add(panel);
        panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        openBtn = new JButton("Open...");
        panel2.add(openBtn);
        saveBtn = new JButton("Save");
        saveBtn.setEnabled(false);
        panel2.add(saveBtn);
        saveAsBtn = new JButton("Save as...");
        panel2.add(saveAsBtn);
        panel.add(panel2);
        pack();
        flipVerticalBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                charData.flipVertical(charSelector.getCharIndex());
            }
        });
        flipHorizontalBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                charData.flipHorizontal(charSelector.getCharIndex());
            }
        });
        rotateClockwiseBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                charData.rotateClockwise(charSelector.getCharIndex());
            }
        });
        rotateCounterclockwiseBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                charData.rotateCounterclockwise(charSelector.getCharIndex());
            }
        });
        openBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (modified) {
                    int option = JOptionPane.showConfirmDialog(CharEditor.this, "There are unsaved changes in " + filename + ". Discard?", "Unsaved changes!", JOptionPane.YES_NO_OPTION);
                    if (option != JOptionPane.YES_OPTION) return;
                }
                try {
                    open();
                    modified = false;
                } catch (IOException ex) {
                    showError("Can't open " + filename, ex);
                }
            }
        });
        saveBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    charData.writeData(currentDirectory.getAbsolutePath() + File.separator + filename);
                    modified = false;
                } catch (IOException ex) {
                    showError("Can't save " + filename, ex);
                }
            }
        });
        saveAsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    saveAs();
                    modified = false;
                } catch (IOException ex) {
                    showError("Can't save " + filename, ex);
                }
            }
        });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (modified) {
                    int option = JOptionPane.showConfirmDialog(CharEditor.this, "There are unsaved changes in " + filename + ". Exit anyway?", "Unsaved changes!", JOptionPane.YES_NO_OPTION);
                    if (option != JOptionPane.YES_OPTION) return;
                }
                System.exit(0);
            }
        });
        setSize(new Dimension(1000, 750));
        setVisible(true);
        modified = false;
    }

    public void charDataChanged() {
        modified = true;
    }

    private void saveAs() throws IOException {
        JFileChooser chooser = new JFileChooser();
        if (currentDirectory != null) chooser.setCurrentDirectory(currentDirectory);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                return f.getName().endsWith(".cmap") || f.isDirectory();
            }

            public String getDescription() {
                return "IENJINIA Character Map";
            }
        });
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        if (f.exists()) {
            int option = JOptionPane.showConfirmDialog(this, "There is already a file with that name. " + "Overwrite?", "File with same name exists!", JOptionPane.YES_NO_OPTION);
            if (option != JOptionPane.YES_OPTION) return;
        }
        currentDirectory = f.getParentFile();
        String path = f.getAbsolutePath();
        if (!path.endsWith(".cmap")) path += ".cmap";
        charData.writeData(path);
        filename = f.getName();
        if (!filename.endsWith(".cmap")) filename += ".cmap";
        saveBtn.setEnabled(true);
    }

    private void open() throws IOException {
        JFileChooser chooser = new JFileChooser();
        if (currentDirectory != null) chooser.setCurrentDirectory(currentDirectory);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                return f.getName().endsWith(".cmap") || f.isDirectory();
            }

            public String getDescription() {
                return "IENJINIA Character Map";
            }
        });
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        currentDirectory = f.getParentFile();
        String path = f.getAbsolutePath();
        if (!path.endsWith(".cmap")) path += ".cmap";
        charData.readData(path);
        filename = f.getName();
        if (!filename.endsWith(".cmap")) filename += ".cmap";
        saveBtn.setEnabled(true);
    }

    private void showError(String title, Throwable t) {
        JTextArea jta = new JTextArea();
        jta.setColumns(40);
        jta.setEditable(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        jta.setText(t.toString());
        JScrollPane jsp = new JScrollPane(jta);
        JOptionPane.showMessageDialog(this, jsp, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws IOException {
        new CharEditor();
    }
}
