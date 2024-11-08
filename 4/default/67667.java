import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

class JDir2HTML extends JFrame implements ActionListener, HyperlinkListener {

    private File directoryToList;

    private File outputFile;

    private String HTMLOutput;

    private JButton directoryButton;

    private JButton fileButton;

    private JButton aboutButton;

    private JDialog aboutWindow;

    private JButton closeButton2 = new JButton("Close");

    private JButton closeButton;

    private JButton generateButton;

    private final int WRITABLE_FILE = 1;

    private final int INVALID_FILE = 2;

    private final int NO_FILE = 3;

    public final String VERSION = "0.2 (2006-11-26)";

    public JDir2HTML() {
        super("Generate a directory listing");
        JPanel descriptionPane = new JPanel();
        descriptionPane.setLayout(new BoxLayout(descriptionPane, BoxLayout.PAGE_AXIS));
        descriptionPane.setBorder(BorderFactory.createEmptyBorder(12, 10, 9, 10));
        JLabel appDescription = new JLabel("Generate a directory listing and store it in an HTML file.");
        descriptionPane.add(appDescription);
        add(descriptionPane, BorderLayout.PAGE_START);
        JPanel actionPane = new JPanel();
        actionPane.setLayout(new GridBagLayout());
        add(actionPane, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        c.weightx = 1;
        c.ipadx = 6;
        JLabel directoryLabel = new JLabel("Source directory:");
        directoryLabel.setLabelFor(directoryButton);
        c.gridx = 0;
        c.gridy = 0;
        actionPane.add(directoryLabel, c);
        directoryButton = new JButton("Choose a directory...");
        directoryButton.addActionListener(this);
        c.gridx = 1;
        c.gridy = 0;
        actionPane.add(directoryButton, c);
        JLabel fileLabel = new JLabel("Target file:");
        fileLabel.setLabelFor(fileButton);
        c.gridx = 0;
        c.gridy = 1;
        actionPane.add(fileLabel, c);
        fileButton = new JButton("Choose a file...");
        fileButton.addActionListener(this);
        c.gridx = 1;
        c.gridy = 1;
        actionPane.add(fileButton, c);
        actionPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel appButtonPane = new JPanel();
        BoxLayout appButtonBox = new BoxLayout(appButtonPane, BoxLayout.LINE_AXIS);
        appButtonPane.setLayout(appButtonBox);
        appButtonPane.setBorder(BorderFactory.createEmptyBorder(9, 12, 12, 12));
        aboutButton = new JButton("About");
        aboutButton.addActionListener(this);
        appButtonPane.add(aboutButton);
        appButtonPane.add(Box.createRigidArea(new Dimension(12, 0)));
        appButtonPane.add(Box.createHorizontalGlue());
        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        appButtonPane.add(closeButton);
        appButtonPane.add(Box.createRigidArea(new Dimension(12, 0)));
        generateButton = new JButton("Generate listing");
        generateButton.addActionListener(this);
        appButtonPane.add(generateButton);
        add(appButtonPane, BorderLayout.PAGE_END);
        setLookAndFeel();
        setResizable(false);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose a directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int chooserStatus = fileChooser.showDialog(this, "Choose directory");
        if (chooserStatus == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.getSelectedFile().isDirectory()) {
                if (fileChooser.getSelectedFile().canRead()) {
                    directoryToList = fileChooser.getSelectedFile();
                    directoryButton.setText(directoryToList.toString());
                    this.pack();
                } else {
                    JOptionPane.showMessageDialog(this, "You don't have permission to read the\n" + "selected directory, so an index cannot be\n" + "created right now. Please check your\n" + "security settings and try again, or choose\n" + "another directory.", "No permission to read from directory.", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "The chosen directory is not a real directory or\n" + "doesn't exist. Please choose another directory.", "Directory error", JOptionPane.ERROR_MESSAGE);
                chooseDirectory();
            }
        }
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose a file name");
        int chooserStatus = fileChooser.showDialog(this, "Select target file");
        if (chooserStatus == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            switch(fileStatus(selectedFile)) {
                case WRITABLE_FILE:
                    if (selectedFile.exists()) {
                        Object[] overwriteOptions = { "Overwrite", "Choose another file", "Don't choose a file" };
                        int overwrite = JOptionPane.showOptionDialog(this, "The file you've chosen already exists.\n" + "Do you want to overwrite this file?", "Overwrite file?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, overwriteOptions, overwriteOptions[1]);
                        if (overwrite == 0) {
                            setOutputFile(selectedFile);
                        } else if (overwrite == 1) {
                            chooseFile();
                        }
                    } else {
                        setOutputFile(selectedFile);
                    }
                    break;
                case INVALID_FILE:
                    chooseFile();
            }
        }
    }

    private int fileStatus(File testFile) {
        if (testFile == null) {
            return NO_FILE;
        }
        if (testFile.exists()) {
            if (testFile.canWrite()) {
                return WRITABLE_FILE;
            } else {
                JOptionPane.showMessageDialog(this, "The file you've chosen already exists,\n" + "but you have no permission to modify or\n" + "replace it. Please choose another file\n" + "name.", "File not writable", JOptionPane.ERROR_MESSAGE);
                return INVALID_FILE;
            }
        } else {
            if (new File(testFile.getParent()).canWrite()) {
                return WRITABLE_FILE;
            } else {
                JOptionPane.showMessageDialog(this, "The file you've chosen does not exist and\n" + "cannot be created. Please try using a\n" + "directory in which you have permission\n" + "to create new files.", "Cannot create file", JOptionPane.ERROR_MESSAGE);
                return INVALID_FILE;
            }
        }
    }

    private void setOutputFile(File newFile) {
        outputFile = newFile;
        fileButton.setText(newFile.toString());
        this.pack();
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) browse(e.getDescription());
    }

    public void actionPerformed(ActionEvent event) {
        Object eventSource = event.getSource();
        if (eventSource == closeButton) {
            System.exit(0);
        } else if (eventSource == directoryButton) {
            chooseDirectory();
        } else if (eventSource == fileButton) {
            chooseFile();
        } else if (eventSource == generateButton) {
            if (directoryToList == null) {
                JOptionPane.showMessageDialog(this, "You haven't selected a directory to list.\n" + "Please select a directory and try again.", "No source directory selected", JOptionPane.ERROR_MESSAGE);
            } else if (!directoryToList.exists()) {
                JOptionPane.showMessageDialog(this, "The directory you've selected doesn't\n" + "exist. Please select a directory that\n" + "does and try again.", "Directory doesn't exist", JOptionPane.ERROR_MESSAGE);
            } else if (!directoryToList.canRead()) {
                JOptionPane.showMessageDialog(this, "You haven't got permission to read the\n" + "selected directory. Please check your\n" + "security settings and try again.", "No permission to read from directory", JOptionPane.ERROR_MESSAGE);
            } else {
                switch(fileStatus(outputFile)) {
                    case WRITABLE_FILE:
                        saveListing();
                        break;
                    case NO_FILE:
                        JOptionPane.showMessageDialog(this, "You haven't selected a file to store\n" + "the listing in. Please select a file and\n" + "try again.", "No target file selected", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (eventSource == aboutButton) {
            showAboutDialog();
        } else if (eventSource == closeButton2) {
            aboutWindow.dispose();
        }
    }

    private void browse(String url) {
        try {
            Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (java.net.URISyntaxException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
    }

    private void showAboutDialog() {
        aboutWindow = new JDialog(this, "About JDir2HTML");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel aboutHeading = new JLabel("<html><big>JDir2HTML</big> v" + VERSION + "</html>");
        aboutHeading.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        gbc.anchor = gbc.WEST;
        gbc.weightx = 1;
        panel.add(aboutHeading);
        gbc.insets = new Insets(16, 0, 0, 0);
        gbc.anchor = gbc.CENTER;
        gbc.weightx = 1;
        panel.add(closeButton2, gbc);
        closeButton2.addActionListener(this);
        aboutWindow.add(panel, BorderLayout.PAGE_START);
        JEditorPane aboutInfo = new JEditorPane("text/html", "<html><b>Please report bugs at</b> <tt>&lt;<a href=http://code.google.com/p/jdir2html/>http://code.google.com/p/jdir2html/</a>&gt;</tt><br><b>or send them by email to</b> <tt>&lt;<a href=mailto:sander.dijkhuis@gmail.com>sander.dijkhuis@gmail.com</a>&gt;</tt>.</html>");
        aboutInfo.setEditable(false);
        aboutInfo.setBackground(aboutWindow.getBackground());
        aboutInfo.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        aboutWindow.add(aboutInfo, BorderLayout.CENTER);
        aboutInfo.addHyperlinkListener(this);
        JTextArea copyrightText = new JTextArea("Copyright (c) 2006, Sander Dijkhuis <mailto:sander.dijkhuis@gmail.com> \n" + "                and Mnix <mailto:mnix.code@gmail.com>\n" + "All rights reserved.\n" + "Redistribution and use in source and binary forms, with or without\n" + "modification, are permitted provided that the following conditions are met:\n\n" + "    * Redistributions of source code must retain the above copyright\n" + "      notice, this list of conditions and the following disclaimer.\n" + "    * Redistributions in binary form must reproduce the above copyright\n" + "      notice, this list of conditions and the following disclaimer in the\n" + "      documentation and/or other materials provided with the distribution.\n" + "    * Neither the name of the software's authors nor the names of its\n" + "      contributors may be used to endorse or promote products derived from\n" + "      this software without specific prior written permission.\n\n" + "THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY\n" + "EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" + "WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" + "DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY\n" + "DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n" + "(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n" + "LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\n" + "ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" + "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.");
        copyrightText.setFont(new Font("Monospaced", Font.PLAIN, 10));
        copyrightText.setEditable(false);
        copyrightText.setLineWrap(false);
        copyrightText.setWrapStyleWord(true);
        copyrightText.setRows(9);
        JScrollPane copyrightScroller = new JScrollPane(copyrightText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        copyrightScroller.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));
        aboutWindow.add(copyrightScroller, BorderLayout.SOUTH);
        aboutWindow.pack();
        aboutWindow.setLocationRelativeTo(this);
        aboutWindow.setResizable(false);
        aboutWindow.setVisible(true);
    }

    private void addToOutput(String HTMLCode) {
        HTMLOutput += HTMLCode + "\n";
    }

    private void saveListing() {
        HTMLOutput = "<!doctype html>\n<meta http-equiv=Content-Type content=\"text/html;charset=UTF-8\">\n";
        try {
            addToOutput("<title>Directory listing of " + directoryToList.toString() + "</title>");
            addToOutput("<h1><code>" + directoryToList.toString() + "</code></h1>");
            addToOutput("<p>Directory listing generated at <i>" + new Date() + "</i> by JDir2HTML.\n<hr>");
            addListing(directoryToList);
            FileWriter file = new FileWriter(outputFile);
            file.write(HTMLOutput);
            file.close();
            JOptionPane.showMessageDialog(this, "The directory listing has successfully\n" + "been created and saved.", "Yay, I did it again!", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "<html>An error occured when trying to write the file. Please try again.<br><br>If you think this is an error in the program, please submit a bug report. See \"About\" for more information.</html>", "Writing error", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }

    private void addListing(File currentDirectory) {
        try {
            if (currentDirectory != null) {
                File[] directoryContents = currentDirectory.listFiles();
                if (directoryContents.length > 0) {
                    addToOutput("<ul>");
                }
                for (int i = 0; i < directoryContents.length; i++) {
                    File file = directoryContents[i];
                    if (file.isDirectory()) {
                        addToOutput("<li><b>" + file.getName() + "</b>");
                        addListing(file);
                    } else {
                        addToOutput("<li>" + file.getName());
                    }
                }
                if (directoryContents.length > 0) {
                    addToOutput("</ul>");
                }
            }
        } catch (Exception e) {
        }
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            System.err.println("Couldn't use the cross-platform look and feel: " + e);
        }
    }

    public static void main(String[] args) {
        JDir2HTML jd2h = new JDir2HTML();
    }
}
