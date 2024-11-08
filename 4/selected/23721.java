package edu.whitman.halfway.jigs.gui.smallprog;

import org.apache.log4j.Logger;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import edu.whitman.halfway.util.*;
import edu.whitman.halfway.jigs.*;

public class GuiInfoExtractor extends JFrame implements ActionListener, InfoStatusInterface {

    private Logger log = Logger.getLogger(GuiInfoExtractor.class);

    private int numImages = 0;

    private JProgressBar progressBar;

    private JTextField currentImage;

    private JTextField currentDirectory;

    private JTextField maxDepthField;

    private JCheckBox recursiveButton;

    private JCheckBox overwriteButton;

    private JButton startButton;

    private JButton stopButton;

    private JFileChooser fileDialog;

    private ExtractorThread infoExtractor;

    private int numDone;

    private int totalNumTasks;

    public GuiInfoExtractor() {
        this.setTitle("Image Information Extractor");
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                runOnClose();
            }
        });
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.weighty = 0.6;
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Directory:"));
        p.setLayout(gridbag);
        JLabel l = new JLabel("Image Directory:");
        l.setForeground(Color.black);
        l.setHorizontalAlignment(JLabel.LEFT);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        gridbag.setConstraints(l, c);
        p.add(l);
        currentDirectory = new JTextField(System.getProperty("user.dir"), 40);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        gridbag.setConstraints(currentDirectory, c);
        p.add(currentDirectory);
        JButton b = new JButton("Browse");
        b.setActionCommand("browse");
        b.addActionListener(this);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        gridbag.setConstraints(b, c);
        p.add(b);
        recursiveButton = new JCheckBox("Include Subdirectories", true);
        recursiveButton.setForeground(Color.black);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        gridbag.setConstraints(recursiveButton, c);
        p.add(recursiveButton);
        l = new JLabel("Max Depth:");
        l.setForeground(Color.black);
        l.setHorizontalAlignment(JLabel.RIGHT);
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.6;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        gridbag.setConstraints(l, c);
        p.add(l);
        maxDepthField = new JTextField("20", 4);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        gridbag.setConstraints(maxDepthField, c);
        p.add(maxDepthField);
        overwriteButton = new JCheckBox("Overwrite Description Files", true);
        overwriteButton.setForeground(Color.black);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        gridbag.setConstraints(overwriteButton, c);
        p.add(overwriteButton);
        startButton = new JButton("Start");
        startButton.setActionCommand("start");
        startButton.addActionListener(this);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        gridbag.setConstraints(startButton, c);
        p.add(startButton);
        stopButton = new JButton("Stop");
        stopButton.setActionCommand("stop");
        stopButton.addActionListener(this);
        stopButton.setEnabled(false);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        gridbag.setConstraints(stopButton, c);
        p.add(stopButton);
        rootPanel.add(p);
        p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Status:"));
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        progressBar = new JProgressBar(0, 10);
        progressBar.setMinimumSize(new Dimension(160, 20));
        progressBar.setPreferredSize(new Dimension(160, 20));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setEnabled(false);
        sb.add(progressBar);
        p.add(sb);
        sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        l = new JLabel("Processing Image File:");
        l.setForeground(Color.black);
        l.setHorizontalAlignment(JLabel.LEFT);
        sb.add(l);
        currentImage = new JTextField("");
        currentImage.setEditable(false);
        currentImage.setBackground(Color.white);
        sb.add(currentImage);
        p.add(sb);
        rootPanel.add(p);
        this.getContentPane().add(rootPanel);
        this.pack();
        this.show();
        File cwd = new File(System.getProperty("user.dir"));
        if (cwd != null) fileDialog = new JFileChooser(cwd); else fileDialog = new JFileChooser();
        fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    private void stopMole() {
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        setNumTasksCompleted(0);
        if (infoExtractor != null) infoExtractor.kill();
    }

    private void startMole() {
        setNumTasksCompleted(0);
        File rootDir = new File(currentDirectory.getText());
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Image directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            currentDirectory.selectAll();
            return;
        }
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        infoExtractor = new ExtractorThread(this, rootDir, overwriteButton.isSelected());
        infoExtractor.start();
    }

    private void browseDirectory() {
        int retValue = fileDialog.showOpenDialog(this);
        if (retValue == JFileChooser.APPROVE_OPTION) {
            File file = fileDialog.getSelectedFile();
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            currentDirectory.setText(file.getAbsolutePath());
        } else {
            log.debug("Open command cancelled by user.");
        }
    }

    public void setTotalNumTasks(int num) {
        totalNumTasks = num;
        progressBar.setMaximum(num);
        numImages = num;
    }

    public void setNumTasksCompleted(int num) {
        numDone = num;
        progressBar.setValue(num);
    }

    public void setTaskStatus(String s) {
        currentImage.setText(s);
    }

    public void allTasksCompleted() {
        setNumTasksCompleted(totalNumTasks);
        setTaskStatus("Finished Processing " + numImages + " Images.");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    public void anotherTaskCompleted() {
        setNumTasksCompleted(numDone + 1);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("start")) {
            totalNumTasks = 0;
            numDone = 0;
            startMole();
        } else if (e.getActionCommand().equals("stop")) {
            stopMole();
        } else if (e.getActionCommand().equals("browse")) {
            browseDirectory();
        }
    }

    private void runOnClose() {
        stopMole();
        System.exit(0);
    }

    public static void main(String[] args) {
        Log4jConfig.doConfig();
        new GuiInfoExtractor();
    }

    private void writeChar() {
        for (int i = 30; i < 250; i++) {
            System.out.print("" + i + " :" + (char) i + ": ");
            if ((i + 1) % 5 == 0) {
                System.out.println();
            }
        }
    }
}
