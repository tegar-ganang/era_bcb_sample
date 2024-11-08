package puggle.ui;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import puggle.Indexer.Indexer;
import puggle.Indexer.IndexProperties;
import puggle.Resources.Resources;
import puggle.Util.Util;

/**
 *
 * @author  gvasil
 */
public class IndexerPanel extends javax.swing.JPanel {

    /** Creates new form IndexerPanel */
    public IndexerPanel() {
        this.init();
        initComponents();
        this.indexPropertiesPanel.setProperties(this.indexProperties);
    }

    private void initComponents() {
        toolBarPanel = new javax.swing.JPanel();
        mainToolBar = new javax.swing.JToolBar();
        newButton = new javax.swing.JButton();
        newPortableButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        aboutButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        indexPropertiesPanel = new puggle.ui.IndexPropertiesPanel();
        actionsPanel = new javax.swing.JPanel();
        actionsLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        textArea = new javax.swing.JTextArea();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        toolBarPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 1, 1));
        mainToolBar.setFloatable(false);
        mainToolBar.setAlignmentY(0.48387095F);
        newButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/filenew.png")));
        newButton.setToolTipText("Create Index");
        newButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(newButton);
        newPortableButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/filenew-portable.png")));
        newPortableButton.setToolTipText("Create Portable Index");
        newPortableButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPortableButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(newPortableButton);
        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/fileopen.png")));
        openButton.setToolTipText("Open Index");
        openButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(openButton);
        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/help-about.png")));
        aboutButton.setToolTipText("About");
        aboutButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(aboutButton);
        exitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/exit.png")));
        exitButton.setToolTipText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });
        mainToolBar.add(exitButton);
        toolBarPanel.add(mainToolBar);
        add(toolBarPanel);
        add(jSeparator1);
        add(indexPropertiesPanel);
        actionsLabel.setBackground(new java.awt.Color(102, 102, 255));
        actionsLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        actionsLabel.setForeground(new java.awt.Color(255, 255, 255));
        actionsLabel.setText("Actions");
        actionsLabel.setOpaque(true);
        progressBar.setBackground(new java.awt.Color(255, 255, 255));
        progressBar.setForeground(new java.awt.Color(51, 255, 51));
        textArea.setColumns(20);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Monospaced", 0, 10));
        textArea.setLineWrap(true);
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(null);
        textArea.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        textArea.setEnabled(false);
        startButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/start.png")));
        startButton.setText("Start");
        startButton.setToolTipText("Start Indexing");
        startButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });
        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stop.png")));
        stopButton.setText("Stop");
        stopButton.setToolTipText("Stop Indexing");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout actionsPanelLayout = new org.jdesktop.layout.GroupLayout(actionsPanel);
        actionsPanel.setLayout(actionsPanelLayout);
        actionsPanelLayout.setHorizontalGroup(actionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, actionsLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 590, Short.MAX_VALUE).add(actionsPanelLayout.createSequentialGroup().addContainerGap().add(progressBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE).addContainerGap()).add(actionsPanelLayout.createSequentialGroup().addContainerGap().add(textArea, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE).addContainerGap()).add(org.jdesktop.layout.GroupLayout.TRAILING, actionsPanelLayout.createSequentialGroup().addContainerGap(410, Short.MAX_VALUE).add(startButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(stopButton).addContainerGap()));
        actionsPanelLayout.setVerticalGroup(actionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(actionsPanelLayout.createSequentialGroup().add(actionsLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 33, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(textArea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(actionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(stopButton).add(startButton)).addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        add(actionsPanel);
    }

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.stopButton.setEnabled(false);
        this.indexer.stop();
        this.indexPropertiesPanel.setProperties(indexProperties);
    }

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {
        File indexDir = null;
        File[] dataDirsFile = null;
        dataDirsFile = this.indexProperties.getDataDirectories();
        if (dataDirsFile.length == 0) {
            JOptionPane.showMessageDialog(this, "No indexing folders have been added.", "Indexing error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File logFile = null;
        PrintStream logStream = null;
        try {
            logFile = Resources.getLogFile();
            logStream = new PrintStream(new FileOutputStream(logFile));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Using standard output for logging.", ex.getMessage(), JOptionPane.INFORMATION_MESSAGE);
            logStream = System.out;
        }
        indexDir = new File(Resources.getIndexCanonicalPath());
        try {
            this.indexer = new Indexer(indexDir, this.indexProperties);
        } catch (IOException ex) {
            int opt = JOptionPane.showConfirmDialog(this, "Force unlock?", ex.getMessage(), JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                try {
                    this.indexer = new Indexer(indexDir, this.indexProperties, true);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Unspecified error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Index successfully unlocked", "Report", JOptionPane.INFORMATION_MESSAGE);
            } else if (opt == JOptionPane.NO_OPTION) {
                return;
            }
        }
        this.indexer.setDataDirectories(dataDirsFile);
        this.indexer.setLogger(new JLogger(logStream, this.textArea));
        this.indexer.setProgressBar(this.progressBar);
        this.startButton.setEnabled(false);
        this.indexPropertiesPanel.setEnabled(false);
        this.newButton.setEnabled(false);
        this.openButton.setEnabled(false);
        this.aboutButton.setEnabled(false);
        this.exitButton.setEnabled(false);
        this.newPortableButton.setEnabled(false);
        this.stopButton.setEnabled(true);
        this.IndexerThread = new Thread(this.indexer);
        this.IndexerThread.start();
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    IndexerThread.join();
                    indexer.optimize();
                    indexer.close();
                    indexer = null;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                indexPropertiesPanel.setEnabled(true);
                newButton.setEnabled(true);
                openButton.setEnabled(true);
                aboutButton.setEnabled(true);
                exitButton.setEnabled(true);
                newPortableButton.setEnabled(true);
                indexPropertiesPanel.setProperties(indexProperties);
            }
        });
        t.start();
    }

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String fileName = "";
        File file = null;
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(new java.io.File(Resources.getApplicationDirectoryCanonicalPath()));
        fc.setDialogTitle("New Index Directory");
        fc.setApproveButtonToolTipText("Create Index Directory");
        while (true) {
            int returnVal = fc.showDialog(this, "Create");
            if (returnVal == JFileChooser.CANCEL_OPTION) {
                return;
            } else if (returnVal == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
                try {
                    fileName = file.getCanonicalPath();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (file.exists()) {
                    Object[] options = { "Open Index", "Overwrite Index" };
                    int opt = JOptionPane.showOptionDialog(this, "Index directory '" + fileName + "' already exists.\n" + "Do you wish to open existing Index or Overwrite?\n" + "(Choosing to overwrite will delete existing Index)", "Error Creating Index Directory", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
                    if (opt == JOptionPane.NO_OPTION) {
                        if (Util.deleteDir(file) == false) {
                            JOptionPane.showMessageDialog(this, "Directory '" + fileName + "' cannot be deleted.", "Error Creating Index Directory", JOptionPane.ERROR_MESSAGE, this.imageControl.getErrorIcon());
                        }
                    }
                }
                this.close();
                try {
                    Resources.setIndex(file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                this.init();
                this.indexPropertiesPanel.setProperties(this.indexProperties);
                return;
            }
        }
    }

    private void newPortableButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") > 0) {
            JOptionPane.showMessageDialog(this, "Sorry, indexing of portable devices is currenly supported\n" + "only in MS Windows operating systems.", "Invalid operation", JOptionPane.ERROR_MESSAGE, this.imageControl.getErrorIcon());
            return;
        }
        File[] roots = File.listRoots();
        File root = (File) JOptionPane.showInputDialog(this, "Drive letter:", "Select device to index", JOptionPane.QUESTION_MESSAGE, this.imageControl.getQuestionIcon(), roots, new JComboBox());
        if (root == null) {
            return;
        }
        File index = null;
        try {
            index = new File(root.getCanonicalPath() + File.separator + ".puggle");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (index == null) {
            return;
        } else if (index.exists()) {
            Object[] options = { "Open Index", "Overwrite Index" };
            int opt = JOptionPane.showOptionDialog(this, "Index directory '" + index + "' already exists.\n" + "Do you wish to open existing Index or Overwrite?\n" + "(Choosing to overwrite will delete existing Index)", "Error Creating Index Directory", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
            if (opt == JOptionPane.NO_OPTION) {
                if (Util.deleteDir(index) == false) {
                    JOptionPane.showMessageDialog(this, "Directory '" + index + "' cannot be deleted.", "Error Creating Index Directory", JOptionPane.ERROR_MESSAGE, this.imageControl.getErrorIcon());
                    return;
                }
                if (index.mkdir() == false) {
                    JOptionPane.showMessageDialog(this, "Cannot create directory '" + index.getAbsolutePath() + "'.", "Error Creating Index Directory", JOptionPane.ERROR_MESSAGE, this.imageControl.getErrorIcon());
                    return;
                }
            }
        }
        this.close();
        try {
            Resources.setIndex(index);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.init();
        this.indexProperties.setFilesystemRoot(root.getAbsolutePath());
        this.indexProperties.setPath(root);
        this.indexProperties.setPortable(true);
        this.indexPropertiesPanel.setProperties(this.indexProperties);
    }

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.close();
        System.exit(1);
    }

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {
        AboutPanel panel = new AboutPanel();
        JDialog dialog = new JDialog((java.awt.Frame) null, "About", true);
        dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/help-about.png")));
        dialog.getContentPane().add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    public void init() {
        this.imageControl = ImageControl.getImageControl();
        this.indexProperties = new IndexProperties(new File(Resources.getApplicationPropertiesCanonicalPath()));
        this.image_filetypes = this.indexProperties.getImageFiletypes();
        this.document_filetypes = this.indexProperties.getDocumentFiletypes();
        this.misc_filetypes = this.indexProperties.getMusicFiletypes();
    }

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(new java.io.File(Resources.getApplicationDirectoryCanonicalPath()));
        fc.setDialogTitle("Select Index Directory");
        boolean error = true;
        while (error == true) {
            error = false;
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (file.getParent() == null) {
                    file = new File(file.getPath() + ".puggle");
                }
                boolean exists = Indexer.indexExists(file);
                String directory = file.getPath();
                if (exists == true) {
                    this.close();
                    try {
                        Resources.setIndex(file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    this.init();
                    this.indexPropertiesPanel.setProperties(this.indexProperties);
                    JOptionPane.showMessageDialog(this, "Index directory '" + directory + "' successfully loaded.", "Open Index Directory", JOptionPane.INFORMATION_MESSAGE, this.imageControl.getInfoIcon());
                } else {
                    JOptionPane.showMessageDialog(this, "Directory '" + directory + "' is not a valid index.", "Error Opening Index Directory", JOptionPane.ERROR_MESSAGE, this.imageControl.getErrorIcon());
                    error = true;
                }
            }
        }
    }

    /**
     * Return true if indexing is in progress
     */
    public boolean isIndexing() {
        if (this.IndexerThread != null && (this.IndexerThread.isAlive())) {
            return true;
        }
        return false;
    }

    public void close() {
        if (this.indexer != null) {
            this.indexer.stop();
            this.indexer.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    private javax.swing.JButton aboutButton;

    private javax.swing.JLabel actionsLabel;

    private javax.swing.JPanel actionsPanel;

    private javax.swing.JButton exitButton;

    private puggle.ui.IndexPropertiesPanel indexPropertiesPanel;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JToolBar mainToolBar;

    private javax.swing.JButton newButton;

    private javax.swing.JButton newPortableButton;

    private javax.swing.JButton openButton;

    private javax.swing.JProgressBar progressBar;

    private javax.swing.JButton startButton;

    private javax.swing.JButton stopButton;

    private javax.swing.JTextArea textArea;

    private javax.swing.JPanel toolBarPanel;

    private IndexProperties indexProperties;

    private ImageControl imageControl;

    private Thread IndexerThread = null;

    private Indexer indexer = null;

    private String image_filetypes = "";

    private String document_filetypes = "";

    private String misc_filetypes = "";

    private String music_filetypes = "";
}
