package net.maizegenetics;

import net.maizegenetics.analysis.*;
import net.maizegenetics.reports.*;
import net.maizegenetics.util.TasselFileFilter;
import net.maizegenetics.util.preferences.Preferences;
import net.maizegenetics.ui.preferences.PreferencesDialog;
import pal.alignment.*;
import pal.alignment.Alignment;
import pal.misc.Identifier;
import pal.datatype.Nucleotides;
import java.awt.*;
import java.awt.image.ImageProducer;
import java.awt.event.*;
import java.io.*;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.net.URL;
import javax.swing.*;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.db.SequenceDB;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.BioException;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.AlphabetManager;
import gov.usda.gdpc.browser.BrowserSettings;
import gov.usda.gdpc.browser.Browser;

/**
 * Current revision: $Revision: 1.15 $
 * On branch $Name:  $
 * Latest change by $Author: dkroon $ on $Date: 2007/04/23 19:01:13 $
 *
 *
 */
public class QTPAnalyzerFrame extends JFrame {

    static final String version = "2.0.1 ";

    final String versionDate = "April 23, 2007 ";

    DataTreePanel theDataTreePanel;

    DataControlPanel theDataControlPanel;

    AnalysisControlPanel theAnalysisControlPanel;

    ResultControlPanel theResultControlPanel;

    Settings theSettings;

    private String tasselSettings = "TasselSettings";

    private String tasselDataFile = "TasselDataFile";

    private String phylipFileSuffix = "phy";

    private String genericFileSuffix = "txt";

    private int fastaNameLength = 10;

    private long lastProgressPaint = 0;

    private String dataTreeLoadFailed = "Unable to open the saved data tree.  The file format of this version is " + "incompatible with other versions.";

    static final String GENOTYPE_DATA_NEEDED = "Please select genotypic data from the data tree.";

    static final String RESULTS_DATA_NEEDED = "Please select results data from the data tree.";

    JFileChooser filerSave = new JFileChooser();

    JFileChooser filerOpen = new JFileChooser();

    JPanel mainPanel = new JPanel();

    JPanel dataTreePanelPanel = new JPanel();

    JPanel reportPanel = new JPanel();

    JPanel optionsPanel = new JPanel();

    JPanel optionsPanelPanel = new JPanel();

    JPanel modeSelectorsPanel = new JPanel();

    JPanel buttonPanel = new JPanel();

    BorderLayout wholeFrameLayout = new BorderLayout();

    BorderLayout mainPanelLayout = new BorderLayout();

    GridLayout buttonPanelLayout = new GridLayout();

    GridLayout optionsPanelLayout = new GridLayout(2, 1);

    BorderLayout dataTreePanelPanelLayout = new BorderLayout();

    BorderLayout reportPanelLayout = new BorderLayout();

    JSplitPane dataTreeReportMainPanelsSplitPanel = new JSplitPane();

    JSplitPane dataTreeReportPanelsSplitPanel = new JSplitPane();

    JScrollPane reportPanelScrollPane = new JScrollPane();

    JTextArea reportPanelTextArea = new JTextArea();

    JScrollPane mainPanelScrollPane = new JScrollPane();

    JPanel mainDisplayPanel = new JPanel();

    ThreadedJTextArea mainPanelTextArea = new ThreadedJTextArea();

    JTextField statusBar = new JTextField();

    JButton helpButton = new JButton();

    JButton resultButton = new JButton();

    JButton saveButton = new JButton();

    JButton dataButton = new JButton();

    JButton deleteButton = new JButton();

    JButton printButton = new JButton();

    JButton analysisButton = new JButton();

    JProgressBar jProgressBar = new JProgressBar();

    JPopupMenu mainPopupMenu = new JPopupMenu();

    JMenuBar jMenuBar = new JMenuBar();

    JMenu fileMenu = new JMenu();

    JMenu toolsMenu = new JMenu();

    JMenuItem saveMainMenuItem = new JMenuItem();

    JCheckBoxMenuItem matchCheckBoxMenuItem = new JCheckBoxMenuItem();

    JMenuItem openCompleteDataTreeMenuItem = new JMenuItem();

    JMenuItem openDataMenuItem = new JMenuItem();

    JMenuItem openMultipeAlignmentFileMenuItem = new JMenuItem();

    JMenuItem saveSelectedDataTreeMenuItem = new JMenuItem();

    JMenuItem saveCompleteDataTreeMenuItem = new JMenuItem();

    JMenuItem saveDataTreeAsMenuItem = new JMenuItem();

    JMenuItem exitMenuItem = new JMenuItem();

    JMenu helpMenu = new JMenu();

    JMenuItem helpMenuItem = new JMenuItem();

    JMenuItem contigencyMenuItem = new JMenuItem();

    JMenuItem preferencesMenuItem = new JMenuItem();

    JMenuItem aboutMenuItem = new JMenuItem();

    PreferencesDialog thePreferencesDialog;

    String UserComments = "";

    public QTPAnalyzerFrame(boolean debug) {
        try {
            loadSettings();
            theDataTreePanel = new DataTreePanel(this, true, debug);
            theDataTreePanel.setToolTipText("Data Tree Panel");
            theDataControlPanel = new DataControlPanel(this, theDataTreePanel);
            theAnalysisControlPanel = new AnalysisControlPanel(this, theDataTreePanel);
            theResultControlPanel = new ResultControlPanel(this, theDataTreePanel);
            theResultControlPanel.setToolTipText("Report Panel");
            initializeMyFrame();
            setIcon();
            initDataMode();
            this.setTitle("TASSEL (Trait Analysis by aSSociation, Evolution, and Linkage) " + this.version);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setProgressBar(int percent) {
        this.jProgressBar.setValue(percent);
        java.util.Date theDate = new java.util.Date();
        if ((theDate.getTime() - lastProgressPaint) > 1000) {
            System.err.println("percent=" + percent);
            Graphics g = this.getGraphics();
            this.paint(g);
            lastProgressPaint = theDate.getTime();
        }
    }

    void setIcon() {
        URL url = this.getClass().getResource("/Logo_small.png");
        if (url == null) return;
        Image img = null;
        try {
            img = createImage((ImageProducer) url.getContent());
        } catch (Exception e) {
        }
        if (img != null) setIconImage(img);
    }

    private void initDataMode() {
        buttonPanel.removeAll();
        buttonPanel.add(theDataControlPanel, null);
        dataTreePanelPanel.removeAll();
        dataTreePanelPanel.add(theDataTreePanel, BorderLayout.CENTER);
        this.validate();
        repaint();
    }

    private void initAnalysisMode() {
        buttonPanel.removeAll();
        buttonPanel.add(theAnalysisControlPanel, null);
        dataTreePanelPanel.removeAll();
        dataTreePanelPanel.add(theDataTreePanel, BorderLayout.CENTER);
        this.validate();
        repaint();
    }

    private void initResultMode() {
        buttonPanel.removeAll();
        buttonPanel.add(theResultControlPanel, null);
        dataTreePanelPanel.removeAll();
        dataTreePanelPanel.add(theDataTreePanel, BorderLayout.CENTER);
        this.validate();
        repaint();
    }

    public void initializeMyFrame() throws Exception {
        this.getContentPane().setLayout(wholeFrameLayout);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(new Dimension(screenSize.width * 19 / 20, screenSize.height * 19 / 20));
        this.setTitle("TASSEL (Trait Analysis by aSSociation, Evolution, and Linkage)");
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
        });
        filerSave.setDialogType(JFileChooser.SAVE_DIALOG);
        mainPanel.setLayout(mainPanelLayout);
        dataTreeReportPanelsSplitPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
        optionsPanelPanel.setLayout(new BorderLayout());
        dataTreePanelPanel.setLayout(dataTreePanelPanelLayout);
        dataTreePanelPanel.setToolTipText("Data Tree Panel");
        reportPanel.setLayout(reportPanelLayout);
        reportPanelTextArea.setEditable(true);
        reportPanelTextArea.setToolTipText("Report Panel");
        reportPanelTextArea.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                noteTextArea_mouseReleased(e);
            }
        });
        reportPanelTextArea.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                noteTextArea_keyTyped(e);
            }
        });
        mainPanelTextArea.setDoubleBuffered(true);
        mainPanelTextArea.setEditable(false);
        mainPanelTextArea.setFont(new java.awt.Font("Monospaced", 0, 12));
        mainPanelTextArea.setToolTipText("Main Panel");
        mainPanelTextArea.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                mainTextArea_mouseClicked(e);
            }
        });
        statusBar.setBackground(Color.lightGray);
        statusBar.setBorder(null);
        statusBar.setText("Program Status");
        modeSelectorsPanel.setLayout(new GridBagLayout());
        modeSelectorsPanel.setMinimumSize(new Dimension(380, 32));
        modeSelectorsPanel.setPreferredSize(new Dimension(700, 32));
        URL imageURL = QTPAnalyzerFrame.class.getResource("images/help1.gif");
        ImageIcon helpIcon = null;
        if (imageURL != null) helpIcon = new ImageIcon(imageURL);
        if (helpIcon != null) helpButton.setIcon(helpIcon);
        helpButton.setMargin(new Insets(0, 0, 0, 0));
        helpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpButton_actionPerformed(e);
            }
        });
        helpButton.setBackground(Color.white);
        helpButton.setMinimumSize(new Dimension(20, 20));
        helpButton.setToolTipText("Help me!!");
        resultButton.setText("Results");
        resultButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                resultButton_actionPerformed(e);
            }
        });
        resultButton.setMargin(new Insets(2, 2, 2, 2));
        imageURL = QTPAnalyzerFrame.class.getResource("images/Results.gif");
        ImageIcon resultsIcon = null;
        if (imageURL != null) resultsIcon = new ImageIcon(imageURL);
        if (resultsIcon != null) resultButton.setIcon(resultsIcon);
        resultButton.setPreferredSize(new Dimension(90, 25));
        resultButton.setMinimumSize(new Dimension(87, 25));
        resultButton.setMaximumSize(new Dimension(90, 25));
        resultButton.setBackground(Color.white);
        saveButton.setMargin(new Insets(0, 0, 0, 0));
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveButton_actionPerformed(e);
            }
        });
        saveButton.setBackground(Color.white);
        saveButton.setMinimumSize(new Dimension(20, 20));
        saveButton.setToolTipText("Save selected data to text file");
        imageURL = QTPAnalyzerFrame.class.getResource("images/save1.gif");
        ImageIcon saveIcon = null;
        if (imageURL != null) saveIcon = new ImageIcon(imageURL);
        if (saveIcon != null) saveButton.setIcon(saveIcon);
        dataButton.setBackground(Color.white);
        dataButton.setMaximumSize(new Dimension(90, 25));
        dataButton.setMinimumSize(new Dimension(87, 25));
        dataButton.setPreferredSize(new Dimension(90, 25));
        imageURL = QTPAnalyzerFrame.class.getResource("images/DataSeq.gif");
        ImageIcon dataSeqIcon = null;
        if (imageURL != null) dataSeqIcon = new ImageIcon(imageURL);
        if (dataSeqIcon != null) dataButton.setIcon(dataSeqIcon);
        dataButton.setMargin(new Insets(2, 2, 2, 2));
        dataButton.setText("Data");
        dataButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dataButton_actionPerformed(e);
            }
        });
        jProgressBar.setBackground(Color.white);
        jProgressBar.setForeground(Color.red);
        jProgressBar.setPreferredSize(new Dimension(100, 16));
        jProgressBar.setStringPainted(true);
        printButton.setBackground(Color.white);
        printButton.setToolTipText("Print selected datum");
        imageURL = QTPAnalyzerFrame.class.getResource("images/print1.gif");
        ImageIcon printIcon = null;
        if (imageURL != null) printIcon = new ImageIcon(imageURL);
        if (printIcon != null) printButton.setIcon(printIcon);
        printButton.setMargin(new Insets(0, 0, 0, 0));
        printButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                printButton_actionPerformed(e);
            }
        });
        analysisButton.setText("Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        analysisButton.setMargin(new Insets(2, 2, 2, 2));
        imageURL = QTPAnalyzerFrame.class.getResource("images/Analysis.gif");
        ImageIcon analysisIcon = null;
        if (imageURL != null) analysisIcon = new ImageIcon(imageURL);
        if (analysisIcon != null) analysisButton.setIcon(analysisIcon);
        analysisButton.setPreferredSize(new Dimension(90, 25));
        analysisButton.setMinimumSize(new Dimension(87, 25));
        analysisButton.setMaximumSize(new Dimension(90, 25));
        analysisButton.setBackground(Color.white);
        deleteButton.setOpaque(true);
        deleteButton.setForeground(Color.RED);
        deleteButton.setText("Delete");
        deleteButton.setFont(new java.awt.Font("Dialog", 1, 12));
        deleteButton.setToolTipText("Delete Dataset");
        deleteButton.setMargin(new Insets(2, 2, 2, 2));
        deleteButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                theDataTreePanel.deleteSelectedNodes();
            }
        });
        optionsPanel.setLayout(optionsPanelLayout);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanel.setMinimumSize(new Dimension(300, 34));
        buttonPanel.setPreferredSize(new Dimension(300, 34));
        buttonPanelLayout.setHgap(0);
        buttonPanelLayout.setVgap(0);
        optionsPanel.setToolTipText("Options Panel");
        optionsPanelLayout.setVgap(0);
        mainPopupMenu.setInvoker(this);
        saveMainMenuItem.setText("Save");
        matchCheckBoxMenuItem.setText("Match");
        matchCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                matchCheckBoxMenuItem_actionPerformed(e);
            }
        });
        fileMenu.setText("File");
        toolsMenu.setText("Tools");
        saveCompleteDataTreeMenuItem.setText("Save Data Tree");
        saveCompleteDataTreeMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveCompleteDataTreeMenuItem_actionPerformed(e);
            }
        });
        saveDataTreeAsMenuItem.setText("Save data tree as ...");
        saveDataTreeAsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveDataTreeMenuItem_actionPerformed(e);
            }
        });
        openCompleteDataTreeMenuItem.setText("Open Data Tree");
        openCompleteDataTreeMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openCompleteDataTreeMenuItem_actionPerformed(e);
            }
        });
        openDataMenuItem.setText("Open ...");
        openDataMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openDataMenuItem_actionPerformed(e);
            }
        });
        openMultipeAlignmentFileMenuItem.setText("Open Multiple Alignment File...");
        openMultipeAlignmentFileMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                openMultipleAlignmentFile_actionPerformed(e);
            }
        });
        saveSelectedDataTreeMenuItem.setText("Save Selected Dataset As...");
        saveSelectedDataTreeMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveSelectedDataTreeMenuItem_actionPerformed(e);
            }
        });
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exitMenuItem_actionPerformed(e);
            }
        });
        helpMenu.setText("Help");
        helpMenuItem.setText("Help Manual");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpMenuItem_actionPerformed(e);
            }
        });
        contigencyMenuItem.setText("Contigency Test");
        contigencyMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                contigencyMenuItem_actionPerformed(e);
            }
        });
        preferencesMenuItem.setText("Set Preferences");
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                preferencesMenuItem_actionPerformed(e);
            }
        });
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpAbout_actionPerformed(e);
            }
        });
        this.getContentPane().add(dataTreeReportMainPanelsSplitPanel, BorderLayout.CENTER);
        dataTreeReportMainPanelsSplitPanel.add(optionsPanelPanel, JSplitPane.TOP);
        optionsPanelPanel.add(dataTreeReportPanelsSplitPanel, BorderLayout.CENTER);
        dataTreeReportPanelsSplitPanel.add(dataTreePanelPanel, JSplitPane.TOP);
        dataTreePanelPanel.add(theDataTreePanel, BorderLayout.CENTER);
        dataTreeReportPanelsSplitPanel.add(reportPanel, JSplitPane.BOTTOM);
        reportPanel.add(reportPanelScrollPane, BorderLayout.CENTER);
        reportPanelScrollPane.getViewport().add(reportPanelTextArea, null);
        dataTreeReportMainPanelsSplitPanel.add(mainPanel, JSplitPane.BOTTOM);
        mainPanel.add(mainDisplayPanel, BorderLayout.CENTER);
        mainDisplayPanel.setLayout(new BorderLayout());
        mainPanelScrollPane.getViewport().add(mainPanelTextArea, null);
        mainDisplayPanel.add(mainPanelScrollPane, BorderLayout.CENTER);
        mainPanelScrollPane.getViewport().add(mainPanelTextArea, null);
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);
        this.getContentPane().add(optionsPanel, BorderLayout.NORTH);
        optionsPanel.add(modeSelectorsPanel, null);
        modeSelectorsPanel.add(resultButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 0, 1, 0), 0, 0));
        modeSelectorsPanel.add(dataButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 1, 0), 0, 0));
        modeSelectorsPanel.add(analysisButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 0, 1, 0), 0, 0));
        modeSelectorsPanel.add(helpButton, new GridBagConstraints(8, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 1, 2), 0, 0));
        modeSelectorsPanel.add(printButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 1, 0), 0, 0));
        modeSelectorsPanel.add(saveButton, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 1, 0), 0, 0));
        modeSelectorsPanel.add(deleteButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 20, 1, 2), 0, 0));
        modeSelectorsPanel.add(jProgressBar, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.VERTICAL, new Insets(6, 252, 6, 8), 57, 0));
        optionsPanel.add(buttonPanel, null);
        mainPopupMenu.add(matchCheckBoxMenuItem);
        mainPopupMenu.add(saveMainMenuItem);
        jMenuBar.add(fileMenu);
        jMenuBar.add(toolsMenu);
        jMenuBar.add(helpMenu);
        fileMenu.add(saveCompleteDataTreeMenuItem);
        fileMenu.add(openCompleteDataTreeMenuItem);
        fileMenu.add(saveDataTreeAsMenuItem);
        fileMenu.add(openDataMenuItem);
        fileMenu.add(openMultipeAlignmentFileMenuItem);
        fileMenu.add(saveSelectedDataTreeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        toolsMenu.add(contigencyMenuItem);
        toolsMenu.add(preferencesMenuItem);
        helpMenu.add(helpMenuItem);
        helpMenu.add(aboutMenuItem);
        this.setJMenuBar(jMenuBar);
        dataTreeReportMainPanelsSplitPanel.setDividerLocation(this.getSize().width / 4);
        dataTreeReportPanelsSplitPanel.setDividerLocation(this.getSize().height / 2);
    }

    private void helpAbout_actionPerformed(ActionEvent e) {
        QTPAnalyzerFrame_AboutBox dlg = new QTPAnalyzerFrame_AboutBox(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.show();
    }

    public void sendMessage(String text) {
        statusBar.setForeground(Color.BLACK);
        statusBar.setText(text);
    }

    public void sendErrorMessage(String text) {
        statusBar.setForeground(Color.RED);
        statusBar.setText(text);
    }

    public void setMainText(String text) {
        mainPanelTextArea.start(text);
    }

    public void setMainText(StringBuffer text) {
        mainPanelTextArea.setDoubleBuffered(false);
        mainPanelTextArea.start(text.toString());
    }

    public void setNoteText(String text) {
        reportPanelTextArea.setText(text);
    }

    public Settings getSettings() {
        return theSettings;
    }

    private void loadSettings() {
        try {
            FileInputStream fis = new FileInputStream(this.tasselSettings);
            ObjectInputStream ois = new ObjectInputStream(fis);
            theSettings = (Settings) ois.readObject();
            fis.close();
        } catch (IOException e) {
            theSettings = new Settings();
            sendErrorMessage("Settings could not be loaded.  Default settings will be used.");
        } catch (ClassNotFoundException e1) {
            theSettings = new Settings();
            sendErrorMessage("Settings could not be loaded.  Default settings will be used.");
        }
        filerOpen.setCurrentDirectory(new File(theSettings.openDir));
        filerSave.setCurrentDirectory(new File(theSettings.saveDir));
        theSettings.resetGeneAndPolyInclusion();
    }

    private void saveSettings() {
        try {
            FileOutputStream fos = new FileOutputStream(this.tasselSettings);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(theSettings);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            sendErrorMessage("Settings could not be saved.");
        }
    }

    /**
     * Provides a save filer that remembers the last location something was saved to
     */
    public File getSaveFile() {
        File saveFile = null;
        int returnVal = filerSave.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            saveFile = filerSave.getSelectedFile();
            theSettings.saveDir = filerSave.getCurrentDirectory().getPath();
        }
        return saveFile;
    }

    /**
     * Provides a open filer that remember the last location something was opened from
     */
    public File getOpenFile() {
        File openFile = null;
        int returnVal = filerOpen.showOpenDialog(this);
        System.out.println("returnVal = " + returnVal);
        System.out.println("JFileChooser.OPEN_DIALOG " + JFileChooser.OPEN_DIALOG);
        if (returnVal == JFileChooser.OPEN_DIALOG || returnVal == JFileChooser.APPROVE_OPTION) {
            openFile = filerOpen.getSelectedFile();
            System.out.println("openFile = " + openFile);
            theSettings.openDir = filerOpen.getCurrentDirectory().getPath();
        }
        return openFile;
    }

    void this_windowClosing(WindowEvent e) {
        exitMenuItem_actionPerformed(null);
    }

    public void addDatum(String dataParent, String dataName, String comments) {
        theDataTreePanel.addDatum(dataParent, dataName, comments);
    }

    public void addDatum(String dataParent, String dataName, Alignment align, String comments) {
        theDataTreePanel.addDatum(dataParent, dataName, align, comments);
    }

    public void addDatum(String dataParent, String dataName, Object align, String comments) {
        theDataTreePanel.addDatum(dataParent, dataName, align, comments);
    }

    public void addMenu(JMenu menu) {
        jMenuBar.add(menu);
        this.setJMenuBar(jMenuBar);
    }

    private void saveDataMenuItem_actionPerformed(ActionEvent e) {
        try {
            theDataTreePanel.prepareForSaving();
            FileOutputStream fos = new FileOutputStream(this.tasselDataFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(theDataTreePanel);
            oos.flush();
            fos.close();
            theDataTreePanel.setParentFrame(this);
            sendMessage("Data saved to " + this.tasselDataFile);
        } catch (IOException ee) {
            sendMessage("Data could not be saved: " + ee);
        }
    }

    private void saveDataTree(String file) {
        try {
            theDataTreePanel.prepareForSaving();
            File theFile = new File(file);
            if (file.indexOf(".zip") == -1) {
                theFile = new File(file + ".zip");
            }
            FileOutputStream fos = new FileOutputStream(theFile);
            java.util.zip.ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry thisEntry = new ZipEntry("DATA");
            zos.putNextEntry(thisEntry);
            ObjectOutputStream oos = new ObjectOutputStream(zos);
            oos.writeObject(theDataTreePanel);
            oos.flush();
            zos.closeEntry();
            fos.close();
            theDataTreePanel.setParentFrame(this);
            sendMessage("Data saved to " + theFile.getAbsolutePath());
        } catch (IOException ee) {
            sendErrorMessage("Data could not be saved: " + ee);
        }
    }

    private void readDataMenuItem_actionPerformed(ActionEvent e) {
        File dataFile = new File(this.tasselDataFile);
        if (dataFile.exists()) {
            readDataTree(this.tasselDataFile);
        } else if (readDataTree("QPGADataFile.zip") == false) {
            readDataTree("QPGADataFile");
        }
    }

    private boolean readDataTree(String file) {
        boolean loadedDataTreePanel = false;
        DataTreePanel oldDataTreePanel = theDataTreePanel;
        try {
            if (file.endsWith("zip")) {
                FileInputStream fis = new FileInputStream(file);
                java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(fis);
                zis.getNextEntry();
                ObjectInputStream ois = new ObjectInputStream(zis);
                try {
                    theDataTreePanel = (DataTreePanel) ois.readObject();
                    loadedDataTreePanel = true;
                } catch (InvalidClassException ice) {
                    JOptionPane.showMessageDialog(this, dataTreeLoadFailed, "Incompatible File Format - Zip", JOptionPane.INFORMATION_MESSAGE);
                }
                fis.close();
            } else {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                try {
                    theDataTreePanel = (DataTreePanel) ois.readObject();
                    loadedDataTreePanel = true;
                } catch (InvalidClassException ice) {
                    JOptionPane.showMessageDialog(this, dataTreeLoadFailed, "Incompatible File Format", JOptionPane.INFORMATION_MESSAGE);
                }
                fis.close();
            }
            if (loadedDataTreePanel) {
                sendMessage("Data loaded.");
            }
        } catch (Exception ee) {
            theDataTreePanel = new DataTreePanel(this, true);
            JOptionPane.showMessageDialog(this, dataTreeLoadFailed + ee, "Incompatible File Format", JOptionPane.INFORMATION_MESSAGE);
            sendErrorMessage("Data tree could not be loaded.");
            return false;
        }
        if (loadedDataTreePanel) {
            dataTreePanelPanel.remove(oldDataTreePanel);
        }
        theDataTreePanel.setParentFrame(this);
        dataTreePanelPanel.add(theDataTreePanel, BorderLayout.CENTER);
        theDataControlPanel.setDataTreePanel(theDataTreePanel);
        theAnalysisControlPanel.setDataTreePanel(theDataTreePanel);
        theResultControlPanel.setDataTreePanel(theDataTreePanel);
        this.validate();
        repaint();
        return loadedDataTreePanel;
    }

    private void dataButton_actionPerformed(ActionEvent e) {
        initDataMode();
    }

    private void analysisButton_actionPerformed(ActionEvent e) {
        initAnalysisMode();
    }

    private void resultButton_actionPerformed(ActionEvent e) {
        initResultMode();
    }

    private void saveButton_actionPerformed(ActionEvent e) {
        File theFile = getSaveFile();
        if (theFile == null) return;
        Object[] data = theDataTreePanel.getSelectedData();
        try {
            FileWriter fw = new FileWriter(theFile);
            PrintWriter pw = new PrintWriter(fw);
            for (int i = 0; i < data.length; i++) {
                if (data[i] instanceof Genotype) {
                    pw.println(data[i].toString());
                } else if (data[i] instanceof pal.misc.TableReport) {
                    TableReportUtils tbu = new TableReportUtils((pal.misc.TableReport) data[i]);
                    pw.println(tbu.toDelimitedString("\t"));
                } else {
                    pw.println(data[i].toString());
                }
                pw.println("");
            }
            fw.flush();
            fw.close();
        } catch (Exception ee) {
            System.err.println("saveButton_actionPerformed:" + ee);
        }
        this.statusBar.setText("Datasets were saved to " + theFile.getName());
    }

    private void printButton_actionPerformed(ActionEvent e) {
        Object[] data = theDataTreePanel.getSelectedData();
        try {
            for (int i = 0; i < data.length; i++) {
                PrintTextArea pta = new PrintTextArea(this);
                pta.printThis(data[i].toString());
            }
        } catch (Exception ee) {
            System.err.println("printButton_actionPerformed:" + ee);
        }
        this.statusBar.setText("Datasets were sent to the printer");
    }

    private void menuHelpAbout_actionPerformed(ActionEvent e) {
        HelpDialog theHelpDialog = new HelpDialog(this);
        theHelpDialog.show();
    }

    private void helpButton_actionPerformed(ActionEvent e) {
        HelpDialog theHelpDialog = new HelpDialog(this);
        theHelpDialog.show();
    }

    private void mainTextArea_mouseClicked(MouseEvent e) {
        if (e.getModifiers() == Event.META_MASK) {
            mainPanelTextArea.add(mainPopupMenu);
            mainPopupMenu.show(mainPanelTextArea, e.getX(), e.getY());
        }
    }

    private void matchCheckBoxMenuItem_actionPerformed(ActionEvent e) {
        theSettings.matchChar = matchCheckBoxMenuItem.isSelected();
    }

    private void openCompleteDataTreeMenuItem_actionPerformed(ActionEvent e) {
        String dataFileName = this.tasselDataFile + ".zip";
        File dataFile = new File(dataFileName);
        if (dataFile.exists()) {
            readDataTree(dataFileName);
        } else if (new File("QPGADataFile").exists()) {
            readDataTree("QPGADataFile");
        } else {
            JOptionPane.showMessageDialog(this, "There are no stored data files in " + theSettings.saveDir + ".  Try openining data tree file via File/Open...");
        }
    }

    private void openDataMenuItem_actionPerformed(ActionEvent e) {
        File f = getOpenFile();
        if (f != null) {
            readDataTree(f.getAbsolutePath());
        }
    }

    private void openMultipleAlignmentFile_actionPerformed(ActionEvent actionEvent) {
        String userMessage = "Could not find file.";
        boolean fileFound = false;
        ArrayList allNames = new ArrayList();
        ArrayList allSequences = new ArrayList();
        FileInputStream fastaFileInputStream = null;
        File aFile = null;
        int seqIndex = 0;
        while (!fileFound) {
            seqIndex = 0;
            JFileChooser fileChooser = new JFileChooser(theSettings.openDir);
            String[] fastaSuffixes = { "fa", "fasta" };
            fileChooser.addChoosableFileFilter(new TasselFileFilter(fastaSuffixes));
            int userChoice = fileChooser.showOpenDialog(this);
            if (userChoice == JFileChooser.APPROVE_OPTION) {
                aFile = fileChooser.getSelectedFile();
            } else if (userChoice == JFileChooser.CANCEL_OPTION) {
                return;
            }
            if (aFile == null) {
                JOptionPane.showMessageDialog(this, userMessage);
            }
            if (aFile.exists()) {
                try {
                    fastaFileInputStream = new FileInputStream(aFile);
                    fileFound = true;
                } catch (FileNotFoundException fnfe) {
                    JOptionPane.showMessageDialog(this, userMessage);
                }
            }
            BufferedInputStream is = new BufferedInputStream(fastaFileInputStream);
            Alphabet dnaAlphabet = AlphabetManager.alphabetForName("DNA");
            SequenceDB db = null;
            try {
                db = SeqIOTools.readFasta(is, dnaAlphabet);
            } catch (BioException e) {
                e.printStackTrace();
            }
            SequenceIterator si = db.sequenceIterator();
            while (si.hasNext()) {
                Sequence seq = null;
                try {
                    seq = si.nextSequence();
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                } catch (BioException e) {
                    e.printStackTrace();
                }
                String seqName = seq.getName();
                int nameLength = seqName.length();
                String theName = (nameLength < fastaNameLength) ? seqName.substring(0, nameLength) : seqName.substring(0, fastaNameLength);
                Identifier theID = new Identifier(theName);
                System.out.println("seqName = " + seqName);
                String sequence = seq.seqString();
                System.out.println("sequence = " + sequence);
                allNames.add(seqIndex, theID);
                allSequences.add(seqIndex++, sequence.toUpperCase());
            }
        }
        int totalTaxa = seqIndex - 1;
        Identifier[] theIDs = new Identifier[totalTaxa];
        String[] theSeqs = new String[totalTaxa];
        for (int i = 0; i < totalTaxa; i++) {
            theIDs[i] = (Identifier) allNames.get(i);
            System.out.println("theIDs = " + theIDs[i]);
            theSeqs[i] = (String) allSequences.get(i);
            System.out.println("theSeqs[i] = " + theSeqs[i]);
        }
        SimpleAnnotatedAlignment theNewAlignment = new SimpleAnnotatedAlignment(theIDs, theSeqs, "-N", new Nucleotides());
        String comments = "Raw Sequence \nNumber of Sequences: " + totalTaxa + "\nNumber of Sites: " + theSeqs[0].length() + "\nDataType: " + theNewAlignment.getDataType().getDescription() + "\nFileName: " + aFile.toString();
        this.addDatum("Genes", aFile.toString(), comments);
        this.addDatum(aFile.toString(), "Raw", theNewAlignment, comments);
    }

    private void saveSelectedDataTreeMenuItem_actionPerformed(ActionEvent e) {
        Object[] data = theDataTreePanel.getSelectedData();
        if (data.length != 1) {
            JOptionPane.showMessageDialog(this, "Please select one item on the data tree to save");
            return;
        }
        System.out.println("data[0].getClass().getName(): " + data[0].getClass().getName());
        JFileChooser fileChooser = null;
        String preferredDir = theSettings.saveDir;
        if (preferredDir != null) {
            fileChooser = new JFileChooser(preferredDir);
        }
        File aFile = null;
        TasselFileFilter fileFilter = new TasselFileFilter();
        AnnotationAlignment aa = null;
        if ((data[0] instanceof AnnotationAlignment) && !(data[0] instanceof AminoAcidAnnotatedAlignment)) {
            aa = (AnnotationAlignment) data[0];
            fileFilter.addExtension(phylipFileSuffix);
            fileFilter.setDescription("Phylip Format");
            fileChooser.setFileFilter(fileFilter);
            aFile = getFilenameFromUser(fileChooser, phylipFileSuffix);
            if (aFile != null) {
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new FileWriter(aFile));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                AlignmentUtils.printSequential(aa, out);
                out.flush();
                out.close();
            }
        } else if (data[0] instanceof Alignment) {
            Alignment anAlignment = (Alignment) data[0];
            fileFilter.addExtension(genericFileSuffix);
            fileFilter.setDescription("Text Output");
            fileChooser.setFileFilter(fileFilter);
            aFile = getFilenameFromUser(fileChooser, genericFileSuffix);
            if (aFile != null) {
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new FileWriter(aFile));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                AlignmentUtils.printPlain(anAlignment, out);
                out.flush();
                out.close();
            }
        }
    }

    private void saveDataTreeMenuItem_actionPerformed(ActionEvent e) {
        File f = getSaveFile();
        if (f != null) {
            saveDataTree(f.getAbsolutePath());
        }
    }

    private void saveCompleteDataTreeMenuItem_actionPerformed(ActionEvent e) {
        saveDataTree(this.tasselDataFile + ".zip");
    }

    private void exitMenuItem_actionPerformed(ActionEvent e) {
        saveSettings();
        Preferences.getInstance().writePropertiesFile();
        theDataControlPanel.saveGDPCSettings();
        System.exit(0);
    }

    private void contigencyMenuItem_actionPerformed(ActionEvent e) {
        ContigencyDialog theContigencyDialog = new ContigencyDialog(this, "Contigency/Fisher Exact Test", false, theSettings);
        theContigencyDialog.show();
    }

    private void preferencesMenuItem_actionPerformed(ActionEvent e) {
        if (thePreferencesDialog == null) {
            thePreferencesDialog = new PreferencesDialog(this, "Preferences");
            thePreferencesDialog.pack();
        }
        thePreferencesDialog.show();
    }

    private void helpMenuItem_actionPerformed(ActionEvent e) {
        HelpDialog theHelpDialog = new HelpDialog(this);
        theHelpDialog.show();
    }

    private void noteTextArea_keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE || e.getKeyChar() == KeyEvent.VK_DELETE) {
            if (UserComments != "") UserComments = UserComments.substring(0, UserComments.length() - 1);
        } else {
            UserComments = UserComments + e.getKeyChar();
        }
    }

    private void noteTextArea_mouseReleased(MouseEvent e) {
        System.out.println(UserComments);
    }

    private File getFilenameFromUser(JFileChooser fileChooser, String fileSuffix) {
        if (fileSuffix.indexOf('.') == -1) {
            fileSuffix = "." + fileSuffix;
        }
        File aFile = null;
        int userChoice = 0;
        String aFileName = null;
        outer: do {
            aFileName = null;
            userChoice = fileChooser.showSaveDialog(this);
            if (userChoice == JFileChooser.CANCEL_OPTION) {
                break;
            }
            File selectedFile = fileChooser.getSelectedFile();
            aFileName = selectedFile.getAbsolutePath();
            if (userChoice == JFileChooser.APPROVE_OPTION) {
                boolean notUniquelyNamed = true;
                while (notUniquelyNamed) {
                    if (aFileName.indexOf('.') == -1) {
                        aFileName = aFileName + "." + this.phylipFileSuffix;
                    }
                    aFile = new File(aFileName);
                    if (aFile.exists()) {
                        int returnVal = JOptionPane.showOptionDialog(this, "This file already exists.  " + "Do you wish to overwrite it?", "Overwrite File Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                        if (returnVal == JOptionPane.CANCEL_OPTION) {
                            return null;
                        }
                        if (returnVal == JOptionPane.NO_OPTION) {
                            aFileName = null;
                            continue outer;
                        }
                        if (returnVal == JOptionPane.YES_OPTION) {
                            notUniquelyNamed = false;
                        }
                    } else {
                        notUniquelyNamed = false;
                    }
                }
            }
        } while (aFileName == null);
        return aFile;
    }

    public void updateMainDisplayPanel(JPanel panel) {
        mainDisplayPanel.removeAll();
        mainDisplayPanel.add(panel, BorderLayout.CENTER);
        mainDisplayPanel.repaint();
    }
}
