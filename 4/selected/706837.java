package gui.junitTestViewer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.StyledDocument;
import net.sf.jhighlighter.kits.JavaHighlightKit;
import net.sf.jhighlighter.ui.FScrollPane;
import testGenerator.Controller;
import testGenerator.utilities.FileSaver;

/**
 * Provides frame for viewing the final test.
 * @author William Whitney
 */
public class TestViewer extends JFrame {

    private static final long serialVersionUID = 1L;

    private TestViewer thisItem = this;

    private JPanel mainPanel;

    private final String testStr;

    private final Controller controller;

    /**
     * Default constructor.
     * @param control
     * @param testStr  
     */
    public TestViewer(Controller control, String testStr) {
        this.controller = control;
        this.testStr = testStr;
        this.setupFrameProperties();
        this.add(getTestDisplay(testStr), BorderLayout.CENTER);
        this.add(getButtonBar(), BorderLayout.SOUTH);
        this.setVisible(true);
    }

    /**
     * Setup the frame properties.
     */
    private void setupFrameProperties() {
        this.setTitle("Generated Test");
        this.setAlwaysOnTop(true);
        this.setSize(600, 700);
        this.mainPanel = new JPanel();
        BorderLayout layout = new BorderLayout();
        this.mainPanel.setLayout(layout);
        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Adds scroll panel for code.
     */
    private JPanel getTestDisplay(String testStr) {
        JPanel panel = new JPanel();
        BorderLayout layout = new BorderLayout();
        panel.setLayout(layout);
        JavaHighlightKit kit = new JavaHighlightKit();
        JTextPane cArea = new JTextPane((StyledDocument) kit.createDefaultDocument());
        cArea.setText(testStr);
        cArea.setEditable(false);
        FScrollPane sPane = new FScrollPane(cArea);
        panel.add(sPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Adds a close button to window.
     */
    private JPanel getButtonBar() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JButton addBtn = new JButton("Copy to Clipboard");
        addBtn.addActionListener(getCopyClipboardListener());
        panel.add(addBtn);
        JButton saveBtn = new JButton("Save to File");
        saveBtn.addActionListener(getSaveBtnListener());
        panel.add(saveBtn);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        panel.add(closeBtn);
        return panel;
    }

    public ActionListener getSaveBtnListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        return true;
                    }

                    @Override
                    public String getDescription() {
                        return "All Files";
                    }
                });
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                boolean done = false;
                while (!done) {
                    fc.setSelectedFile(new File(fc.getCurrentDirectory().getAbsolutePath() + File.separator + controller.getClassName() + "Test.java"));
                    int returnVal = fc.showSaveDialog(thisItem);
                    File selectedFile = fc.getSelectedFile();
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        if (selectedFile.exists()) {
                            int result = JOptionPane.showConfirmDialog(thisItem, "File Already Exists!\n Would you like to overwrite it?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (JOptionPane.YES_OPTION == result) {
                                FileSaver fileSaver = new FileSaver(testStr, selectedFile);
                                done = true;
                                JOptionPane.showMessageDialog(thisItem, "File Saved!", "File Writer", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            FileSaver fileSaver = new FileSaver(testStr, selectedFile);
                            done = true;
                            JOptionPane.showMessageDialog(thisItem, "File Saved!", "File Writer", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        done = true;
                    }
                }
            }
        };
    }

    public ActionListener getCopyClipboardListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                Clipboard clipboard = getToolkit().getSystemClipboard();
                StringSelection clipContents = new StringSelection(testStr);
                clipboard.setContents(clipContents, new ClipboardOwner() {

                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    }
                });
            }
        };
    }
}
