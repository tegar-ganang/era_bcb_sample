import java.io.FileNotFoundException;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileFilter;
import java.awt.Color;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.util.StringTokenizer;

/**
 * @author  Shane Santner
 * This class provides the GUI for dvd-homevideo.
 */
public class GUI extends javax.swing.JFrame {

    public GUI() {
        initComponents();
        spnSeconds.setModel(spnSecondsSize);
        spnMinutes.setModel(spnMinutesSize);
        if (!checkForProg(DependentPrograms)) MessageBox("You do not appear to have the necessary\n" + "programs installed for dvd-homevideo to operate\n" + "correctly.  Please check the README file for the\n" + "list of dependencies.  dvd-homevideo will not\n" + "function correctly until all dependencies are installed!", 1);
        if (!checkForModules()) MessageBox("You do not have the correct modules loaded\n" + "for dvd-homevideo to function properly.  Please\n" + "check the README file for further instructions!", 1);
        if (getJMenuBar() != null) (this.getJMenuBar()).setOpaque(false);
        File ProjectDir = new File(System.getProperty("user.home") + "/.dvd-homevideo");
        if (!ProjectDir.exists()) {
            MessageBox("All projects will be automatically saved to: " + System.getProperty("user.home") + "/.dvd-homevideo/", 1);
            ProjectDir.mkdir();
        }
        File ProjProperties = new File(System.getProperty("user.home") + "/.dvd-homevideo/properties");
        if (!ProjProperties.exists()) {
            try {
                BufferedWriter projProp = new BufferedWriter(new FileWriter(ProjProperties));
                projProp.close();
            } catch (IOException ex) {
                SaveStackTrace.printTrace(strOutputDir, ex);
                MessageBox("IO Error writing to properties file...in GUI.java\n" + ex.toString(), 0);
                ex.printStackTrace();
            }
        }
        ReadProjProperties();
    }

    javax.swing.ImageIcon image = new javax.swing.ImageIcon(getClass().getResource("/logo_32x32.png"));

    protected String strOutputDir;

    protected boolean blnBegin;

    protected String[] DependentPrograms = { "dvgrab", "transcode", "mplex", "dvd-menu", "dvdauthor", "growisofs", "ffmpeg", "lame", "sox" };

    private void initComponents() {
        grpQuality = new javax.swing.ButtonGroup();
        fcOpen = new javax.swing.JFileChooser();
        grpFormat = new javax.swing.ButtonGroup();
        grpAspectRatio = new javax.swing.ButtonGroup();
        fcMenuOpen = new javax.swing.JFileChooser();
        grpMenuFormat = new javax.swing.ButtonGroup();
        grpMenuAspectRatio = new javax.swing.ButtonGroup();
        grpMenuMode = new javax.swing.ButtonGroup();
        grpMenuIgnore = new javax.swing.ButtonGroup();
        pnlGUI = new javax.swing.JPanel();
        lblMinutes = new javax.swing.JLabel();
        spnMinutes = new javax.swing.JSpinner();
        lblSeconds = new javax.swing.JLabel();
        spnSeconds = new javax.swing.JSpinner();
        chkQuality = new javax.swing.JCheckBox();
        rdSuper = new javax.swing.JRadioButton();
        rdGood = new javax.swing.JRadioButton();
        rdAverage = new javax.swing.JRadioButton();
        btnStart = new javax.swing.JButton();
        btnExit = new javax.swing.JButton();
        chkMenu = new javax.swing.JCheckBox();
        lblPicture = new javax.swing.JLabel();
        txtPicture = new javax.swing.JTextField();
        lblAudio = new javax.swing.JLabel();
        txtAudio = new javax.swing.JTextField();
        btnOpen_Picture = new javax.swing.JButton();
        prgCapture = new javax.swing.JProgressBar();
        prgConvert = new javax.swing.JProgressBar();
        prgAuthor = new javax.swing.JProgressBar();
        btnOpen_Audio = new javax.swing.JButton();
        lblTextFile = new javax.swing.JLabel();
        txtTextFile = new javax.swing.JTextField();
        btnOpen_TextFile = new javax.swing.JButton();
        txtOutputDir = new javax.swing.JTextField();
        lblOutputDir = new javax.swing.JLabel();
        txtTitle = new javax.swing.JTextField();
        lblTitle = new javax.swing.JLabel();
        btnOpen_OutputDir = new javax.swing.JButton();
        chkBurn = new javax.swing.JCheckBox();
        lblCaptureProg = new javax.swing.JLabel();
        lblConvertProg = new javax.swing.JLabel();
        lblAuthorProg = new javax.swing.JLabel();
        lblCapture = new javax.swing.JLabel();
        lblConvert = new javax.swing.JLabel();
        lblAuthor = new javax.swing.JLabel();
        sprCapConvert = new javax.swing.JSeparator();
        sprMenuAuthor = new javax.swing.JSeparator();
        spTextArea = new javax.swing.JScrollPane();
        txtAreaOutput = new javax.swing.JTextArea();
        rdNTSC = new javax.swing.JRadioButton();
        rdPAL = new javax.swing.JRadioButton();
        rd4_3 = new javax.swing.JRadioButton();
        rd16_9 = new javax.swing.JRadioButton();
        lblFormat = new javax.swing.JLabel();
        lblAspectRatio = new javax.swing.JLabel();
        sprAspectRatio = new javax.swing.JSeparator();
        btnPlay = new javax.swing.JButton();
        txtStatus = new javax.swing.JTextField();
        menuBarMain = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuOpen = new javax.swing.JMenuItem();
        sprOpen = new javax.swing.JSeparator();
        menuSave = new javax.swing.JMenuItem();
        sprSave = new javax.swing.JSeparator();
        menuExit = new javax.swing.JMenuItem();
        menuTools = new javax.swing.JMenu();
        menuRd_IgnoreNone = new javax.swing.JRadioButtonMenuItem();
        menuRd_IgnoreCap = new javax.swing.JRadioButtonMenuItem();
        menuRd_IgnoreCapConv = new javax.swing.JRadioButtonMenuItem();
        menuRd_IgnoreCapConvMenu = new javax.swing.JRadioButtonMenuItem();
        sprIgnore = new javax.swing.JSeparator();
        menuProjProp = new javax.swing.JMenu();
        menuRdNTSC = new javax.swing.JRadioButtonMenuItem();
        menuRdPAL = new javax.swing.JRadioButtonMenuItem();
        sprMenuFormatAspect = new javax.swing.JSeparator();
        menuRd_4_3 = new javax.swing.JRadioButtonMenuItem();
        menuRd_16_9 = new javax.swing.JRadioButtonMenuItem();
        sprMenuThread = new javax.swing.JSeparator();
        menuChkThread = new javax.swing.JCheckBoxMenuItem();
        menuHelp = new javax.swing.JMenu();
        menuInternet = new javax.swing.JMenuItem();
        menuREADME = new javax.swing.JMenuItem();
        menuBug = new javax.swing.JMenuItem();
        menuAbout = new javax.swing.JMenuItem();
        fcOpen.setName("fcOpen");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("dvd-homevideo");
        setIconImage(image.getImage());
        setName("dvd-homevideo");
        setResizable(false);
        pnlGUI.setLayout(null);
        pnlGUI.setToolTipText("dvd-homevideo");
        pnlGUI.setMaximumSize(new java.awt.Dimension(510, 620));
        pnlGUI.setMinimumSize(new java.awt.Dimension(510, 620));
        pnlGUI.setName("pnlGUI");
        pnlGUI.setOpaque(false);
        pnlGUI.setPreferredSize(new java.awt.Dimension(510, 620));
        lblMinutes.setText("Capture Time in Minutes");
        lblMinutes.setName("lblMinutes");
        pnlGUI.add(lblMinutes);
        lblMinutes.setBounds(20, 10, 154, 15);
        spnMinutes.setToolTipText("Minutes Portion of the Capture Time");
        spnMinutes.setMinimumSize(new java.awt.Dimension(35, 20));
        spnMinutes.setName("spnMinutes");
        spnMinutes.setNextFocusableComponent(spnSeconds);
        spnMinutes.setPreferredSize(new java.awt.Dimension(35, 20));
        spnMinutes.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnMinutesStateChanged(evt);
            }
        });
        pnlGUI.add(spnMinutes);
        spnMinutes.setBounds(140, 30, 35, 20);
        lblSeconds.setText("Capture Time in Seconds");
        lblSeconds.setName("lblSeconds");
        pnlGUI.add(lblSeconds);
        lblSeconds.setBounds(20, 50, 155, 15);
        spnSeconds.setToolTipText("Seconds Portion of the Capture Time");
        spnSeconds.setName("spnSeconds");
        spnSeconds.setNextFocusableComponent(chkQuality);
        spnSeconds.setPreferredSize(new java.awt.Dimension(35, 20));
        spnSeconds.setRequestFocusEnabled(false);
        spnSeconds.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnSecondsStateChanged(evt);
            }
        });
        pnlGUI.add(spnSeconds);
        spnSeconds.setBounds(140, 70, 35, 20);
        chkQuality.setText("Quality for DVD compression");
        chkQuality.setToolTipText("Check this box to enable quality control");
        chkQuality.setName("chkQuality");
        chkQuality.setNextFocusableComponent(rdSuper);
        chkQuality.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                chkQualityKeyTyped(evt);
            }
        });
        chkQuality.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chkQualityMouseClicked(evt);
            }
        });
        pnlGUI.add(chkQuality);
        chkQuality.setBounds(260, 10, 208, 23);
        grpQuality.add(rdSuper);
        rdSuper.setSelected(true);
        rdSuper.setText("Super");
        rdSuper.setToolTipText("Best Quality...recommended");
        rdSuper.setEnabled(false);
        rdSuper.setName("rdSuper");
        rdSuper.setNextFocusableComponent(rdGood);
        pnlGUI.add(rdSuper);
        rdSuper.setBounds(280, 30, 60, 23);
        grpQuality.add(rdGood);
        rdGood.setText("Good");
        rdGood.setToolTipText("Good Quality...a little faster");
        rdGood.setEnabled(false);
        rdGood.setName("rdGood");
        rdGood.setNextFocusableComponent(rdAverage);
        pnlGUI.add(rdGood);
        rdGood.setBounds(280, 50, 58, 23);
        grpQuality.add(rdAverage);
        rdAverage.setText("Average");
        rdAverage.setToolTipText("Nothing special...but fast");
        rdAverage.setEnabled(false);
        rdAverage.setName("rdAverage");
        rdAverage.setNextFocusableComponent(rdNTSC);
        pnlGUI.add(rdAverage);
        rdAverage.setBounds(280, 70, 75, 23);
        btnStart.setText("Start");
        btnStart.setToolTipText("Start Capturing Video from your Digital Camcorder");
        btnStart.setEnabled(false);
        btnStart.setName("btnStart");
        btnStart.setNextFocusableComponent(btnExit);
        btnStart.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnStartKeyTyped(evt);
            }
        });
        btnStart.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnStartMouseClicked(evt);
            }
        });
        pnlGUI.add(btnStart);
        btnStart.setBounds(250, 400, 70, 25);
        btnExit.setText("Exit");
        btnExit.setToolTipText("Exit dvd-homevideo");
        btnExit.setName("btnExit");
        btnExit.setNextFocusableComponent(btnPlay);
        btnExit.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnExitKeyTyped(evt);
            }
        });
        btnExit.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnExitMouseClicked(evt);
            }
        });
        pnlGUI.add(btnExit);
        btnExit.setBounds(330, 400, 70, 25);
        chkMenu.setText("Custom DVD Menu");
        chkMenu.setToolTipText("Enables Custom Picture and Audio for DVD Background");
        chkMenu.setName("chkMenu");
        chkMenu.setNextFocusableComponent(txtPicture);
        chkMenu.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                chkMenuKeyTyped(evt);
            }
        });
        chkMenu.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chkMenuMouseClicked(evt);
            }
        });
        pnlGUI.add(chkMenu);
        chkMenu.setBounds(10, 170, 144, 23);
        lblPicture.setText("Path to DVD Menu Picture");
        lblPicture.setEnabled(false);
        lblPicture.setName("lblPicture");
        pnlGUI.add(lblPicture);
        lblPicture.setBounds(10, 200, 160, 15);
        txtPicture.setEnabled(false);
        txtPicture.setName("txtPicture");
        txtPicture.setNextFocusableComponent(btnOpen_Picture);
        pnlGUI.add(txtPicture);
        txtPicture.setBounds(10, 220, 170, 19);
        lblAudio.setText("Path to DVD Menu Audio");
        lblAudio.setEnabled(false);
        lblAudio.setName("lblAudio");
        pnlGUI.add(lblAudio);
        lblAudio.setBounds(10, 250, 155, 15);
        txtAudio.setEnabled(false);
        txtAudio.setName("txtAudio");
        txtAudio.setNextFocusableComponent(btnOpen_Audio);
        pnlGUI.add(txtAudio);
        txtAudio.setBounds(10, 270, 170, 19);
        btnOpen_Picture.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open.gif")));
        btnOpen_Picture.setEnabled(false);
        btnOpen_Picture.setName("btnOpen_Picture");
        btnOpen_Picture.setNextFocusableComponent(txtAudio);
        btnOpen_Picture.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnOpen_PictureKeyTyped(evt);
            }
        });
        btnOpen_Picture.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnOpen_PictureMouseClicked(evt);
            }
        });
        pnlGUI.add(btnOpen_Picture);
        btnOpen_Picture.setBounds(190, 220, 30, 25);
        prgCapture.setEnabled(false);
        prgCapture.setName("prgCapture");
        pnlGUI.add(prgCapture);
        prgCapture.setBounds(10, 360, 148, 14);
        prgConvert.setEnabled(false);
        prgConvert.setName("prgConvert");
        pnlGUI.add(prgConvert);
        prgConvert.setBounds(180, 360, 148, 14);
        prgAuthor.setEnabled(false);
        prgAuthor.setName("prgAuthor");
        pnlGUI.add(prgAuthor);
        prgAuthor.setBounds(350, 360, 148, 14);
        btnOpen_Audio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open.gif")));
        btnOpen_Audio.setEnabled(false);
        btnOpen_Audio.setName("btnOpen_Audio");
        btnOpen_Audio.setNextFocusableComponent(txtOutputDir);
        btnOpen_Audio.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnOpen_AudioKeyTyped(evt);
            }
        });
        btnOpen_Audio.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnOpen_AudioMouseClicked(evt);
            }
        });
        pnlGUI.add(btnOpen_Audio);
        btnOpen_Audio.setBounds(190, 270, 30, 25);
        lblTextFile.setText("Path to Text File (optional)");
        lblTextFile.setToolTipText("This text file specifies the chapter titles for the DVD");
        lblTextFile.setName("lblTextFile");
        pnlGUI.add(lblTextFile);
        lblTextFile.setBounds(260, 270, 165, 15);
        txtTextFile.setName("txtTextFile");
        txtTextFile.setNextFocusableComponent(btnOpen_TextFile);
        pnlGUI.add(txtTextFile);
        txtTextFile.setBounds(260, 290, 200, 19);
        btnOpen_TextFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open.gif")));
        btnOpen_TextFile.setName("btnOpen_TextFile");
        btnOpen_TextFile.setNextFocusableComponent(btnStart);
        btnOpen_TextFile.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnOpen_TextFileKeyTyped(evt);
            }
        });
        btnOpen_TextFile.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnOpen_TextFileMouseClicked(evt);
            }
        });
        pnlGUI.add(btnOpen_TextFile);
        btnOpen_TextFile.setBounds(470, 290, 30, 25);
        txtOutputDir.setName("txtOutputDir");
        txtOutputDir.setNextFocusableComponent(btnOpen_OutputDir);
        txtOutputDir.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                txtOutputDirFocusLost(evt);
            }
        });
        pnlGUI.add(txtOutputDir);
        txtOutputDir.setBounds(260, 190, 200, 19);
        lblOutputDir.setText("Path to the Output Directory");
        lblOutputDir.setName("lblOutputDir");
        pnlGUI.add(lblOutputDir);
        lblOutputDir.setBounds(260, 170, 177, 15);
        txtTitle.setName("txtTitle");
        txtTitle.setNextFocusableComponent(txtTextFile);
        txtTitle.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                txtTitleFocusLost(evt);
            }
        });
        pnlGUI.add(txtTitle);
        txtTitle.setBounds(260, 240, 200, 19);
        lblTitle.setText("Enter a Title for the DVD");
        lblTitle.setName("lblTitle");
        pnlGUI.add(lblTitle);
        lblTitle.setBounds(260, 220, 153, 15);
        btnOpen_OutputDir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open.gif")));
        btnOpen_OutputDir.setName("btnOpen_OutputDir");
        btnOpen_OutputDir.setNextFocusableComponent(txtTitle);
        btnOpen_OutputDir.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                btnOpen_OutputDirKeyTyped(evt);
            }
        });
        btnOpen_OutputDir.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnOpen_OutputDirMouseClicked(evt);
            }
        });
        pnlGUI.add(btnOpen_OutputDir);
        btnOpen_OutputDir.setBounds(470, 190, 30, 25);
        chkBurn.setText("Burn to DVD");
        chkBurn.setName("chkBurn");
        chkBurn.setNextFocusableComponent(chkMenu);
        pnlGUI.add(chkBurn);
        chkBurn.setBounds(340, 120, 103, 23);
        lblCaptureProg.setText("0%");
        lblCaptureProg.setEnabled(false);
        lblCaptureProg.setName("lblCaptureProg");
        pnlGUI.add(lblCaptureProg);
        lblCaptureProg.setBounds(20, 380, 40, 15);
        lblConvertProg.setText("0%");
        lblConvertProg.setEnabled(false);
        lblConvertProg.setName("lblConvertProg");
        pnlGUI.add(lblConvertProg);
        lblConvertProg.setBounds(190, 380, 40, 15);
        lblAuthorProg.setText("0%");
        lblAuthorProg.setEnabled(false);
        lblAuthorProg.setName("lblAuthorProg");
        pnlGUI.add(lblAuthorProg);
        lblAuthorProg.setBounds(360, 380, 40, 15);
        lblCapture.setText("Capture Progress");
        lblCapture.setEnabled(false);
        lblCapture.setName("lblCapture");
        pnlGUI.add(lblCapture);
        lblCapture.setBounds(10, 340, 107, 15);
        lblConvert.setText("Conversion Progress");
        lblConvert.setEnabled(false);
        lblConvert.setName("lblConvert");
        pnlGUI.add(lblConvert);
        lblConvert.setBounds(180, 340, 129, 15);
        lblAuthor.setText("Authoring DVD");
        lblAuthor.setEnabled(false);
        lblAuthor.setName("lblAuthor");
        pnlGUI.add(lblAuthor);
        lblAuthor.setBounds(350, 340, 95, 15);
        sprCapConvert.setMinimumSize(new java.awt.Dimension(50, 10));
        sprCapConvert.setName("sprCapConvert");
        sprCapConvert.setPreferredSize(new java.awt.Dimension(50, 5));
        pnlGUI.add(sprCapConvert);
        sprCapConvert.setBounds(10, 160, 490, 5);
        sprMenuAuthor.setMinimumSize(new java.awt.Dimension(50, 10));
        sprMenuAuthor.setName("sprMenuAuthor");
        sprMenuAuthor.setPreferredSize(new java.awt.Dimension(50, 5));
        pnlGUI.add(sprMenuAuthor);
        sprMenuAuthor.setBounds(10, 330, 490, 5);
        spTextArea.setAutoscrolls(true);
        spTextArea.setName("spTextArea");
        txtAreaOutput.setEditable(false);
        txtAreaOutput.setLineWrap(true);
        txtAreaOutput.setToolTipText("Output during program execution");
        txtAreaOutput.setWrapStyleWord(true);
        txtAreaOutput.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        txtAreaOutput.setName("txtAreaOutput");
        spTextArea.setViewportView(txtAreaOutput);
        pnlGUI.add(spTextArea);
        spTextArea.setBounds(20, 470, 470, 140);
        grpFormat.add(rdNTSC);
        rdNTSC.setSelected(true);
        rdNTSC.setText("NTSC");
        rdNTSC.setName("rdNTSC");
        rdNTSC.setNextFocusableComponent(rdPAL);
        pnlGUI.add(rdNTSC);
        rdNTSC.setBounds(20, 130, 58, 23);
        grpFormat.add(rdPAL);
        rdPAL.setText("PAL");
        rdPAL.setName("rdPAL");
        rdPAL.setNextFocusableComponent(rd4_3);
        pnlGUI.add(rdPAL);
        rdPAL.setBounds(90, 130, 60, 23);
        grpAspectRatio.add(rd4_3);
        rd4_3.setSelected(true);
        rd4_3.setText("4:3");
        rd4_3.setName("rd4_3");
        rd4_3.setNextFocusableComponent(rd16_9);
        pnlGUI.add(rd4_3);
        rd4_3.setBounds(190, 130, 50, 23);
        grpAspectRatio.add(rd16_9);
        rd16_9.setText("16:9");
        rd16_9.setName("rd16_9");
        rd16_9.setNextFocusableComponent(chkBurn);
        pnlGUI.add(rd16_9);
        rd16_9.setBounds(240, 130, 60, 23);
        lblFormat.setText("Video Format");
        lblFormat.setName("lblFormat");
        pnlGUI.add(lblFormat);
        lblFormat.setBounds(20, 110, 110, 15);
        lblAspectRatio.setText("Aspect Ratio");
        lblAspectRatio.setName("lblAspectRatio");
        pnlGUI.add(lblAspectRatio);
        lblAspectRatio.setBounds(190, 110, 100, 15);
        sprAspectRatio.setMinimumSize(new java.awt.Dimension(50, 10));
        sprAspectRatio.setName("sprAspectRatio");
        sprAspectRatio.setPreferredSize(new java.awt.Dimension(50, 5));
        pnlGUI.add(sprAspectRatio);
        sprAspectRatio.setBounds(10, 100, 490, 5);
        btnPlay.setText("Play");
        btnPlay.setEnabled(false);
        btnPlay.setNextFocusableComponent(spnMinutes);
        btnPlay.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnPlayMouseClicked(evt);
            }
        });
        pnlGUI.add(btnPlay);
        btnPlay.setBounds(410, 400, 70, 25);
        txtStatus.setBackground(new java.awt.Color(153, 153, 153));
        txtStatus.setEditable(false);
        txtStatus.setFont(new java.awt.Font("Dialog", 1, 12));
        txtStatus.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtStatus.setText("Status");
        txtStatus.setMaximumSize(new java.awt.Dimension(59, 25));
        txtStatus.setMinimumSize(new java.awt.Dimension(59, 25));
        pnlGUI.add(txtStatus);
        txtStatus.setBounds(410, 430, 70, 30);
        getContentPane().add(pnlGUI, new java.awt.GridBagConstraints());
        menuBarMain.setName("menuBarMain");
        menuFile.setMnemonic('f');
        menuFile.setText("File");
        menuFile.setName("menuFile");
        menuOpen.setMnemonic('o');
        menuOpen.setText("Open");
        menuOpen.setName("menuOpen");
        menuOpen.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuOpenMousePressed(evt);
            }
        });
        menuFile.add(menuOpen);
        sprOpen.setName("sprOpen");
        menuFile.add(sprOpen);
        menuSave.setMnemonic('s');
        menuSave.setText("Save");
        menuSave.setName("menuSave");
        menuSave.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuSaveMousePressed(evt);
            }
        });
        menuFile.add(menuSave);
        sprSave.setName("sprSave");
        menuFile.add(sprSave);
        menuExit.setMnemonic('x');
        menuExit.setText("Exit");
        menuExit.setBorderPainted(false);
        menuExit.setFocusable(true);
        menuExit.setName("menuExit");
        menuExit.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuExitMousePressed(evt);
            }
        });
        menuFile.add(menuExit);
        menuBarMain.add(menuFile);
        menuTools.setMnemonic('t');
        menuTools.setText("Tools");
        menuTools.setName("menuTools");
        grpMenuIgnore.add(menuRd_IgnoreNone);
        menuRd_IgnoreNone.setText("Reset");
        menuRd_IgnoreNone.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuRd_IgnoreNoneStateChanged(evt);
            }
        });
        menuTools.add(menuRd_IgnoreNone);
        grpMenuIgnore.add(menuRd_IgnoreCap);
        menuRd_IgnoreCap.setText("Skip Capture");
        menuRd_IgnoreCap.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuRd_IgnoreCapStateChanged(evt);
            }
        });
        menuTools.add(menuRd_IgnoreCap);
        grpMenuIgnore.add(menuRd_IgnoreCapConv);
        menuRd_IgnoreCapConv.setText("Skip Capture/Transcode");
        menuRd_IgnoreCapConv.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuRd_IgnoreCapConvStateChanged(evt);
            }
        });
        menuTools.add(menuRd_IgnoreCapConv);
        grpMenuIgnore.add(menuRd_IgnoreCapConvMenu);
        menuRd_IgnoreCapConvMenu.setText("Skip Capture/Transcode/Menu");
        menuRd_IgnoreCapConvMenu.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menuRd_IgnoreCapConvMenuStateChanged(evt);
            }
        });
        menuTools.add(menuRd_IgnoreCapConvMenu);
        menuTools.add(sprIgnore);
        menuProjProp.setText("Project Properties (Default)");
        grpMenuFormat.add(menuRdNTSC);
        menuRdNTSC.setSelected(true);
        menuRdNTSC.setText("NTSC");
        menuRdNTSC.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                menuRdNTSCItemStateChanged(evt);
            }
        });
        menuProjProp.add(menuRdNTSC);
        grpMenuFormat.add(menuRdPAL);
        menuRdPAL.setText("PAL");
        menuRdPAL.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                menuRdPALItemStateChanged(evt);
            }
        });
        menuProjProp.add(menuRdPAL);
        menuProjProp.add(sprMenuFormatAspect);
        grpMenuAspectRatio.add(menuRd_4_3);
        menuRd_4_3.setSelected(true);
        menuRd_4_3.setText("4:3");
        menuRd_4_3.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                menuRd_4_3ItemStateChanged(evt);
            }
        });
        menuProjProp.add(menuRd_4_3);
        grpMenuAspectRatio.add(menuRd_16_9);
        menuRd_16_9.setText("16:9");
        menuRd_16_9.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                menuRd_16_9ItemStateChanged(evt);
            }
        });
        menuProjProp.add(menuRd_16_9);
        menuProjProp.add(sprMenuThread);
        menuChkThread.setText("Enable Multi-Threading");
        menuChkThread.setToolTipText("EXPERIMENTAL!!! Very unstable!!!  This allows dvgrab and transcode to run at the same time...speeding up the process by as much as 10%");
        menuChkThread.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                menuChkThreadItemStateChanged(evt);
            }
        });
        menuProjProp.add(menuChkThread);
        menuTools.add(menuProjProp);
        menuBarMain.add(menuTools);
        menuHelp.setMnemonic('h');
        menuHelp.setText("Help");
        menuHelp.setName("menuHelp");
        menuInternet.setText("dvd-homevideo website");
        menuInternet.setName("menuInternet");
        menuInternet.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuInternetMousePressed(evt);
            }
        });
        menuHelp.add(menuInternet);
        menuREADME.setText("README File");
        menuREADME.setName("menuREADME");
        menuREADME.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuREADMEMousePressed(evt);
            }
        });
        menuHelp.add(menuREADME);
        menuBug.setText("Submit Bug");
        menuBug.setName("menuBug");
        menuBug.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuBugMousePressed(evt);
            }
        });
        menuHelp.add(menuBug);
        menuAbout.setText("About dvd-homevideo");
        menuAbout.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuAboutMousePressed(evt);
            }
        });
        menuHelp.add(menuAbout);
        menuBarMain.add(menuHelp);
        setJMenuBar(menuBarMain);
        pack();
    }

    private void menuRd_IgnoreNoneStateChanged(javax.swing.event.ChangeEvent evt) {
        if (menuRd_IgnoreNone.isSelected()) {
            lblMinutes.setEnabled(true);
            spnMinutes.setEnabled(true);
            lblSeconds.setEnabled(true);
            spnSeconds.setEnabled(true);
            chkQuality.setEnabled(true);
            lblFormat.setEnabled(true);
            rdNTSC.setEnabled(true);
            rdPAL.setEnabled(true);
            lblAspectRatio.setEnabled(true);
            rd4_3.setEnabled(true);
            rd16_9.setEnabled(true);
            chkMenu.setEnabled(true);
        }
        enableStartButton();
    }

    private void menuRd_IgnoreCapConvMenuStateChanged(javax.swing.event.ChangeEvent evt) {
        if (menuRd_IgnoreCapConv.isSelected()) {
            lblMinutes.setEnabled(false);
            spnMinutes.setEnabled(false);
            lblSeconds.setEnabled(false);
            spnSeconds.setEnabled(false);
            chkQuality.setEnabled(false);
            lblFormat.setEnabled(false);
            rdNTSC.setEnabled(false);
            rdPAL.setEnabled(false);
            lblAspectRatio.setEnabled(false);
            rd4_3.setEnabled(false);
            rd16_9.setEnabled(false);
            chkMenu.setEnabled(false);
        }
        enableStartButton();
    }

    private void menuRd_IgnoreCapConvStateChanged(javax.swing.event.ChangeEvent evt) {
        if (menuRd_IgnoreCapConv.isSelected()) {
            lblMinutes.setEnabled(false);
            spnMinutes.setEnabled(false);
            lblSeconds.setEnabled(false);
            spnSeconds.setEnabled(false);
            chkQuality.setEnabled(false);
            lblFormat.setEnabled(false);
            rdNTSC.setEnabled(false);
            rdPAL.setEnabled(false);
            lblAspectRatio.setEnabled(false);
            rd4_3.setEnabled(false);
            rd16_9.setEnabled(false);
            chkMenu.setEnabled(true);
        }
        enableStartButton();
    }

    private void menuRd_IgnoreCapStateChanged(javax.swing.event.ChangeEvent evt) {
        if (menuRd_IgnoreCap.isSelected()) {
            lblMinutes.setEnabled(false);
            spnMinutes.setEnabled(false);
            lblSeconds.setEnabled(false);
            spnSeconds.setEnabled(false);
            chkQuality.setEnabled(true);
            lblFormat.setEnabled(true);
            rdNTSC.setEnabled(true);
            rdPAL.setEnabled(true);
            lblAspectRatio.setEnabled(true);
            rd4_3.setEnabled(true);
            rd16_9.setEnabled(true);
            chkMenu.setEnabled(true);
        }
        enableStartButton();
    }

    private void txtOutputDirFocusLost(java.awt.event.FocusEvent evt) {
        enableStartButton();
    }

    private void txtTitleFocusLost(java.awt.event.FocusEvent evt) {
        enableStartButton();
    }

    private void btnOpen_OutputDirKeyTyped(java.awt.event.KeyEvent evt) {
        fcOpen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fcOpen.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) txtOutputDir.setText(fcOpen.getSelectedFile().getPath());
    }

    private void btnOpen_OutputDirMouseClicked(java.awt.event.MouseEvent evt) {
        fcOpen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fcOpen.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) txtOutputDir.setText(fcOpen.getSelectedFile().getPath());
    }

    private void spnSecondsStateChanged(javax.swing.event.ChangeEvent evt) {
        enableStartButton();
    }

    private void spnMinutesStateChanged(javax.swing.event.ChangeEvent evt) {
        enableStartButton();
    }

    private void btnStartKeyTyped(java.awt.event.KeyEvent evt) {
        if (evt.getKeyChar() == evt.VK_ENTER) {
            try {
                WriteSession();
            } catch (IOException e) {
            }
            blnBegin = true;
            btnPlay.setEnabled(false);
        }
    }

    private void btnExitKeyTyped(java.awt.event.KeyEvent evt) {
        if (evt.getKeyChar() == evt.VK_ENTER) ExitDVDHomevideo();
    }

    private void menuChkThreadItemStateChanged(java.awt.event.ItemEvent evt) {
        WriteProjProperties();
    }

    private void menuAboutMousePressed(java.awt.event.MouseEvent evt) {
        MessageBox("dvd-homevideo, version 0.4\n" + "Created by: Shane Santner", 1);
    }

    /**
     * This is the code that handles the user clicking the Exit button.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void btnExitMouseClicked(java.awt.event.MouseEvent evt) {
        ExitDVDHomevideo();
    }

    /**
     * This is the code that handles the user changing the state of one
     * of the menu radio buttions.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void menuRd_16_9ItemStateChanged(java.awt.event.ItemEvent evt) {
        WriteProjProperties();
    }

    /**
     * This is the code that handles the user changing the state of one
     * of the menu radio buttions.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void menuRd_4_3ItemStateChanged(java.awt.event.ItemEvent evt) {
        WriteProjProperties();
    }

    /**
     * This is the code that handles the user changing the state of one
     * of the menu radio buttions.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void menuRdPALItemStateChanged(java.awt.event.ItemEvent evt) {
        WriteProjProperties();
    }

    /**
     * This is the code that handles the user changing the state of one
     * of the menu radio buttions.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void menuRdNTSCItemStateChanged(java.awt.event.ItemEvent evt) {
        WriteProjProperties();
    }

    /**
     * This is the code that handles the user clicking the Play button.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void btnPlayMouseClicked(java.awt.event.MouseEvent evt) {
        try {
            String[] cmd = { "kaffeine", "dvd:" + strOutputDir + "/DVD/" };
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            try {
                String[] cmd = { "xine", "dvd:" + strOutputDir + "/DVD/" };
                Process p = Runtime.getRuntime().exec(cmd);
            } catch (IOException io) {
                MessageBox("It appears that you do not have xine or kaffeine installed.\n", 1);
            }
        }
    }

    /**
     * This is the code that handles the user clicking on the SubmitBug button
     * in the Help menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuBugMousePressed(java.awt.event.MouseEvent evt) {
        try {
            String[] cmd = { "firefox", "http://sourceforge.net/tracker/?func=add&group_id=129878&atid=716141" };
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            try {
                String[] cmd = { "konqueror", "http://sourceforge.net/tracker/?func=add&group_id=129878&atid=716141" };
                Process p = Runtime.getRuntime().exec(cmd);
            } catch (IOException io) {
                MessageBox("It appears that you do not have firefox or konqueror installed.\n" + "Open up your favorite web browser and paste:\n" + "http://sourceforge.net/tracker/?func=add&group_id=129878&atid=716141\n" + "in the address bar to submit a bug for dvd-homevideo.", 1);
            }
        }
    }

    /**
     * This is the code that handles the user clicking the Open button
     * from the File menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuOpenMousePressed(java.awt.event.MouseEvent evt) {
        File home = new File(System.getProperty("user.home") + "/.dvd-homevideo");
        fcMenuOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fcMenuOpen.addChoosableFileFilter(new CustomFileFilter(".xml"));
        fcMenuOpen.setCurrentDirectory(home);
        int returnVal = fcMenuOpen.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) ReadSession(fcMenuOpen.getSelectedFile().getPath());
        enableStartButton();
    }

    /**
     * This is the code that handles the user clicking the Save button
     * from the File menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuSaveMousePressed(java.awt.event.MouseEvent evt) {
        try {
            WriteSession();
        } catch (IOException e) {
        }
    }

    /**
     * This is the code that handles the user clicking the dvd-homevideo website
     * button from the Help menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuInternetMousePressed(java.awt.event.MouseEvent evt) {
        try {
            String[] cmd = { "firefox", "http://dvd-homevideo.sourceforge.net/" };
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            try {
                String[] cmd = { "konqueror", "http://dvd-homevideo.sourceforge.net/" };
                Process p = Runtime.getRuntime().exec(cmd);
            } catch (IOException io) {
                MessageBox("It appears that you do not have firefox or konqueror installed.\n" + "Open up your favorite web browser and paste:\n" + "http://dvd-homevideo.sourceforge.net/\n" + "in the address bar for help with running dvd-homevideo.", 1);
            }
        }
    }

    /**
     * This is the code that handles the user clicking the README file
     * button from the Help menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuREADMEMousePressed(java.awt.event.MouseEvent evt) {
        try {
            String[] cmd = { "firefox", "http://dvd-homevideo.sourceforge.net/README.html" };
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            try {
                String[] cmd = { "konqueror", "http://dvd-homevideo.sourceforge.net/README.html" };
                Process p = Runtime.getRuntime().exec(cmd);
            } catch (IOException io) {
                MessageBox("It appears that you do not have firefox or konqueror installed.\n" + "Open up your favorite web browser and paste:\n" + "http://dvd-homevideo.sourceforge.net/README.html\n" + "in the address bar for help with running dvd-homevideo.", 1);
            }
        }
    }

    /**
     * This is the code that handles the user clicking the Exit button
     * from the File menu.
     * @param   evt    The mousePressed event handled by this method
     */
    private void menuExitMousePressed(java.awt.event.MouseEvent evt) {
        ExitDVDHomevideo();
    }

    /**
     * This is the code that handles the user clicking the Start button.
     * @param   evt    The mouseClicked event handled by this method
     */
    private void btnStartMouseClicked(java.awt.event.MouseEvent evt) {
        try {
            WriteSession();
        } catch (IOException e) {
        }
        blnBegin = true;
        btnPlay.setEnabled(false);
    }

    /**
     * This is the code that handles the user typing Return on the
     * Open button to search for a title file to use for dvd-menu.
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_TextFileKeyTyped(java.awt.event.KeyEvent evt) {
        fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fcOpen.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) txtTextFile.setText(fcOpen.getSelectedFile().getPath());
    }

    /**
     * This is the code that handles the user typing Return on the
     * Open button to search for an output directory to locate an
     * audio file.
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_AudioKeyTyped(java.awt.event.KeyEvent evt) {
        if (chkMenu.isSelected()) {
            fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fcOpen.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) txtAudio.setText(fcOpen.getSelectedFile().getPath());
        }
    }

    /**
     * This is the code that handles the user typing Return on the
     * Open button to search for an output directory to locate a
     * picture in.
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_PictureKeyTyped(java.awt.event.KeyEvent evt) {
        if (chkMenu.isSelected() == true) {
            fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fcOpen.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) txtPicture.setText(fcOpen.getSelectedFile().getPath());
        }
    }

    /**
     * This is the code that handles the user typing the Space bar
     * on the check box for the menu
     * @param   evt    The KeyTyped event handled by this method
     */
    private void chkMenuKeyTyped(java.awt.event.KeyEvent evt) {
        if (chkMenu.isSelected()) {
            lblPicture.setEnabled(false);
            txtPicture.setEnabled(false);
            btnOpen_Picture.setEnabled(false);
            lblAudio.setEnabled(false);
            txtAudio.setEnabled(false);
            btnOpen_Audio.setEnabled(false);
        } else {
            lblPicture.setEnabled(true);
            txtPicture.setEnabled(true);
            btnOpen_Picture.setEnabled(true);
            lblAudio.setEnabled(true);
            txtAudio.setEnabled(true);
            btnOpen_Audio.setEnabled(true);
        }
    }

    /**
     * This is the code that handles the user typing the Space bar
     * on the check box for the quality group of radio buttons
     * @param   evt    The KeyTyped event handled by this method
     */
    private void chkQualityKeyTyped(java.awt.event.KeyEvent evt) {
        if (chkQuality.isSelected()) {
            rdSuper.setEnabled(false);
            rdGood.setEnabled(false);
            rdAverage.setEnabled(false);
        } else {
            rdSuper.setEnabled(true);
            rdGood.setEnabled(true);
            rdAverage.setEnabled(true);
        }
    }

    /**
     * This is the code that handles the user clicking on the
     * Open button to search for an output directory to locate a
     * picture in.
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_PictureMouseClicked(java.awt.event.MouseEvent evt) {
        if (chkMenu.isSelected() == true) {
            fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fcOpen.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) txtPicture.setText(fcOpen.getSelectedFile().getPath());
        }
    }

    /**
     * This is the code that handles the user clicking on the
     * Open button to search for an output directory to locate an
     * audio file in.
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_AudioMouseClicked(java.awt.event.MouseEvent evt) {
        if (chkMenu.isSelected()) {
            fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fcOpen.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) txtAudio.setText(fcOpen.getSelectedFile().getPath());
        }
    }

    /**
     * This is the code that handles the user clicking on the
     * Open button to search for a title file to be used for
     * the background menu of the DVD
     * @param   evt    The KeyTyped event handled by this method
     */
    private void btnOpen_TextFileMouseClicked(java.awt.event.MouseEvent evt) {
        fcOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fcOpen.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) txtTextFile.setText(fcOpen.getSelectedFile().getPath());
    }

    /**
     * This is the code that handles the user clicking on the
     * check box to select the quality of video conversion for
     * their DVD
     * @param   evt    The KeyTyped event handled by this method
     */
    private void chkQualityMouseClicked(java.awt.event.MouseEvent evt) {
        if (chkQuality.isSelected()) {
            rdSuper.setEnabled(true);
            rdGood.setEnabled(true);
            rdAverage.setEnabled(true);
        } else {
            rdSuper.setEnabled(false);
            rdGood.setEnabled(false);
            rdAverage.setEnabled(false);
        }
    }

    /**
     * This is the code that handles the user clicking on the
     * check box to specify details for the DVD menu
     * @param   evt    The KeyTyped event handled by this method
     */
    private void chkMenuMouseClicked(java.awt.event.MouseEvent evt) {
        if (chkMenu.isSelected()) {
            lblPicture.setEnabled(true);
            txtPicture.setEnabled(true);
            btnOpen_Picture.setEnabled(true);
            lblAudio.setEnabled(true);
            txtAudio.setEnabled(true);
            btnOpen_Audio.setEnabled(true);
        } else {
            lblPicture.setEnabled(false);
            txtPicture.setEnabled(false);
            btnOpen_Picture.setEnabled(false);
            lblAudio.setEnabled(false);
            txtAudio.setEnabled(false);
            btnOpen_Audio.setEnabled(false);
        }
    }

    public void enableStartButton() {
        int tempMinutes, tempSeconds, tempTotal;
        String tempStrTitle, tempStrOutputDir;
        tempMinutes = ((Integer) spnMinutes.getValue()).intValue();
        tempSeconds = ((Integer) spnSeconds.getValue()).intValue();
        tempTotal = tempSeconds + tempMinutes;
        tempStrTitle = txtTitle.getText();
        tempStrOutputDir = txtOutputDir.getText();
        if (tempTotal >= 1 && !tempStrOutputDir.equals("") && !tempStrTitle.equals("")) {
            strOutputDir = txtOutputDir.getText() + "/" + txtTitle.getText();
            btnStart.setEnabled(true);
        } else if (!tempStrOutputDir.equals("") && !tempStrTitle.equals("") && (menuRd_IgnoreCap.isSelected() || menuRd_IgnoreCapConv.isSelected() || menuRd_IgnoreCapConvMenu.isSelected())) {
            strOutputDir = txtOutputDir.getText() + "/" + txtTitle.getText();
            btnStart.setEnabled(true);
        } else btnStart.setEnabled(false);
    }

    /**
     * On startup, checks to ensure that the raw1394 module is loaded.
     * This module is only needed to capture audio and video from the
     * dv camcorder.
     * return   A boolean indicating if the module is present
     */
    public boolean checkForModules() {
        try {
            String line;
            String modules = "cat /proc/modules | grep raw1394";
            String[] module_cmd = { "/bin/sh", "-c", modules };
            Process p = Runtime.getRuntime().exec(module_cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            line = in.readLine();
            p.waitFor();
            if (line == null) return false; else return true;
        } catch (Exception ioe) {
            return false;
        }
    }

    /**
     * On startup, checks to ensure that all dependent programs are installed
     * @param   Prog[]  List of dependent programs needed for dvd-homevideo
     * @return  A boolean to determine if an error occurred in the function
     */
    public boolean checkForProg(String[] Prog) {
        int i = 0;
        int j = 0;
        String build_list = "";
        String[] uninstalled = new String[Prog.length];
        while (true) {
            try {
                while (i < Prog.length) {
                    Process p = Runtime.getRuntime().exec(Prog[i]);
                    p.destroy();
                    i++;
                }
                for (i = 0; i < uninstalled.length; i++) {
                    if (uninstalled[i] != null) build_list += uninstalled[i] + "\n";
                }
                if (uninstalled[0] == null) return true; else MessageBox("The following programs are not installed or not in " + "your path!\n" + build_list, 0);
                return false;
            } catch (IOException ioe) {
                uninstalled[j] = Prog[i];
                i++;
                j++;
            }
        }
    }

    /**
     * Displays a message box with the supplied text and yes/no options
     * @param   message    The message to display in the box
     */
    public int MessageBox(String message) {
        return JOptionPane.showConfirmDialog(null, message, "dvd-homevideo", JOptionPane.YES_NO_OPTION);
    }

    /**
     * Displays a message box with the supplied text and type
     * @param   message    The message to display in the box
     * @param   type       The type of message (Info, Warning or Error)
     */
    public void MessageBox(String message, int type) {
        JOptionPane.showMessageDialog(null, message, "dvd-homevideo", type);
    }

    /**
     * Displays a message box with the supplied text and type
     * @param   message    The message to display in the box
     * @param   type       The type of message (Info, Warning or Error)
     * @param   picture    Picture to display in the MessageBox
     */
    public void MessageBox(String message, int type, String picture) {
        JOptionPane.showMessageDialog(null, message, "dvd-homevideo", type, new javax.swing.ImageIcon(getClass().getResource(picture)));
    }

    /**
     * Check dvd-homevideo for runtime errors
     * @param   fileName    This is a log file to parse through, looking for
     *                      the keyword "Error"
     * @return   A boolean indicating if an error was found
     */
    public boolean ErrorCheck(String fileName) {
        String line;
        StringTokenizer st;
        String testToken;
        boolean error = false;
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            while ((line = in.readLine()) != null) {
                if (line.equals(":-( /dev/dvd: media is not recognized as recordable DVD: 9")) {
                    MessageBox("Non-recoverable error occurred." + "\nClass Name: " + new Exception().getStackTrace()[1].getClassName() + "\nMethod Name: " + new Exception().getStackTrace()[1].getMethodName() + "\nError was: " + line, 0);
                    return true;
                }
                st = new StringTokenizer(line, "*,;:'-~\t ");
                while (st.hasMoreTokens()) {
                    testToken = st.nextToken();
                    if (testToken.equalsIgnoreCase("Error")) {
                        MessageBox("Non-recoverable error occurred." + "\nClass Name: " + new Exception().getStackTrace()[1].getClassName() + "\nMethod Name: " + new Exception().getStackTrace()[1].getMethodName() + "\nError was: " + line, 0);
                        error = true;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("Can not find " + fileName + "\n" + ex.toString(), 0);
            ex.printStackTrace();
            return true;
        } catch (IOException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("IO Error\n" + ex.toString(), 0);
            ex.printStackTrace();
            return true;
        }
        return error;
    }

    /**
     * Update the Status textbox
     * @param   typeColor   red, green, or grey
     * @param   typeUpdate  Values should be PASS, FAIL, Status
     */
    public void UpdateStatus(Color typeColor, String typeUpdate) {
        txtStatus.setBackground(typeColor);
        txtStatus.setText(typeUpdate);
        if (typeUpdate.equals("FAIL")) txtStatus.setToolTipText("Look at dvd-homevideo.err and files" + " in the log directory for possible" + " reasons why dvd-homevideo failed.");
        if (!typeUpdate.equals("Status")) blnBegin = false;
    }

    /**
     * This method should be called every time the application exits normally.  This allows the
     * current session to be saved before quiting dvd-homevideo.
     */
    public void ExitDVDHomevideo() {
        try {
            WriteSession();
            WriteProjProperties();
            System.exit(0);
        } catch (IOException e) {
        }
    }

    /**
     * This method retrieves user specific information from the 
     * properties xml file in the users ~/.dvd-homevideo directory
     */
    public void ReadProjProperties() {
        File home = new File(System.getProperty("user.home") + "/.dvd-homevideo/properties");
        String line, token;
        StringTokenizer st;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(home));
            while ((line = reader.readLine()) != null) {
                st = new StringTokenizer(line, "<>");
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (token.equals("menuRdNTSC")) {
                        line = reader.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        menuRdNTSC.setSelected(Boolean.parseBoolean(token));
                        menuRdPAL.setSelected(!Boolean.parseBoolean(token));
                        rdNTSC.setSelected(menuRdNTSC.isSelected());
                        rdPAL.setSelected(menuRdPAL.isSelected());
                    } else if (token.equals("menuRd_4_3")) {
                        line = reader.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        menuRd_4_3.setSelected(Boolean.parseBoolean(token));
                        menuRd_16_9.setSelected(!Boolean.parseBoolean(token));
                        rd4_3.setSelected(menuRd_4_3.isSelected());
                        rd16_9.setSelected(menuRd_16_9.isSelected());
                    } else if (token.equals("menuChkThread")) {
                        line = reader.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        menuChkThread.setSelected(Boolean.parseBoolean(token));
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("IO Error in ReadProjectProperties in GUI.java\n" + ex.toString(), 0);
            ex.printStackTrace();
        }
    }

    /**
     * This method stores user specific information in a properties xml file
     * in the users ~/.dvd-homevideo directory
     */
    public void WriteProjProperties() {
        File home = new File(System.getProperty("user.home") + "/.dvd-homevideo/properties");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(home));
            writer.write("<dvd-homevideo properties>");
            writer.newLine();
            writer.write("\t<JRadioButtonMenuItem>");
            writer.newLine();
            writer.write("\t\t<menuRdNTSC>");
            writer.newLine();
            writer.write("\t\t\t<selected>" + menuRdNTSC.isSelected() + "</selected>");
            writer.newLine();
            writer.write("\t\t</menuRdNTSC>");
            writer.newLine();
            writer.write("\t\t<menuRd_4_3>");
            writer.newLine();
            writer.write("\t\t\t<selected>" + menuRd_4_3.isSelected() + "</selected>");
            writer.newLine();
            writer.write("\t\t</menuRd_4_3>");
            writer.newLine();
            writer.write("\t</JRadioButonMenuItem>");
            writer.newLine();
            writer.write("\t<JCheckBoxMenuItem>");
            writer.newLine();
            writer.write("\t\t<menuChkThread>");
            writer.newLine();
            writer.write("\t\t\t<selected>" + menuChkThread.isSelected() + "</selected>");
            writer.newLine();
            writer.write("\t\t</menuChkThread>");
            writer.newLine();
            writer.write("\t</JCheckBoxMenuItem>");
            writer.newLine();
            writer.close();
            rdNTSC.setSelected(menuRdNTSC.isSelected());
            rdPAL.setSelected(menuRdPAL.isSelected());
            rd4_3.setSelected(menuRd_4_3.isSelected());
            rd16_9.setSelected(menuRd_16_9.isSelected());
        } catch (IOException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("IO Error in WriteProjectProperties in GUI.java\n" + ex.toString(), 0);
            ex.printStackTrace();
        }
    }

    /**
     * Opens a previous dvd-homevideo session by reading an xml file
     * @param   xmlPath    Path to the xml file
     */
    public void ReadSession(String xmlPath) {
        String line, token;
        StringTokenizer st;
        boolean done = false;
        int i;
        javax.swing.JLabel[] lbl_widgets = { lblMinutes, lblSeconds, lblFormat, lblAspectRatio, lblPicture, lblAudio, lblOutputDir, lblTitle, lblTextFile };
        javax.swing.JSpinner[] spn_widgets = { spnMinutes, spnSeconds };
        javax.swing.JCheckBox[] chk_widgets = { chkQuality, chkMenu, chkBurn };
        javax.swing.JRadioButton[] rd_widgets = { rdSuper, rdGood, rdAverage, rdNTSC, rdPAL, rd4_3, rd16_9 };
        javax.swing.JTextField[] txt_widgets = { txtPicture, txtAudio, txtOutputDir, txtTitle, txtTextFile };
        javax.swing.JButton[] btn_widgets = { btnOpen_Picture, btnOpen_Audio, btnOpen_OutputDir, btnOpen_TextFile };
        javax.swing.JRadioButtonMenuItem[] chkMenu_widgets = { menuRd_IgnoreCap, menuRd_IgnoreCapConv, menuRd_IgnoreCapConvMenu };
        try {
            BufferedReader in = new BufferedReader(new FileReader(xmlPath));
            line = in.readLine();
            st = new StringTokenizer(line, "<>");
            token = st.nextToken();
            if (!token.equals("dvd-homevideo")) {
                MessageBox("Invalid File Format!", 1);
                done = true;
            }
            while (((line = in.readLine()) != null) && !done) {
                st = new StringTokenizer(line, "<>");
                if (!st.nextToken().equals("/dvd-homevideo")) token = st.nextToken();
                if (token.equals("JLabel")) {
                    for (i = 0; i < lbl_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        lbl_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        in.readLine();
                    }
                } else if (token.equals("JSpinner")) {
                    for (i = 0; i < spn_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        spn_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        spn_widgets[i].setValue(Integer.parseInt(token));
                        in.readLine();
                    }
                } else if (token.equals("JCheckBox")) {
                    for (i = 0; i < chk_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        chk_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        chk_widgets[i].setSelected(Boolean.parseBoolean(token));
                        in.readLine();
                    }
                } else if (token.equals("JRadioButton")) {
                    for (i = 0; i < rd_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        rd_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        rd_widgets[i].setSelected(Boolean.parseBoolean(token));
                        in.readLine();
                    }
                } else if (token.equals("JTextField")) {
                    for (i = 0; i < txt_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        txt_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        if (!token.substring(1, 4).equals("tex")) txt_widgets[i].setText(token);
                        in.readLine();
                    }
                } else if (token.equals("JButton")) {
                    for (i = 0; i < btn_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        btn_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        in.readLine();
                    }
                } else if (token.equals("JRadioButtonMenuItem")) {
                    for (i = 0; i < chkMenu_widgets.length; i++) {
                        in.readLine();
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        chkMenu_widgets[i].setEnabled(Boolean.parseBoolean(token));
                        line = in.readLine();
                        st = new StringTokenizer(line, "<>");
                        st.nextToken();
                        st.nextToken();
                        token = st.nextToken();
                        chkMenu_widgets[i].setSelected(Boolean.parseBoolean(token));
                        in.readLine();
                    }
                } else {
                    lblCapture.setEnabled(false);
                    lblConvert.setEnabled(false);
                    lblAuthor.setEnabled(false);
                    lblCaptureProg.setEnabled(false);
                    lblConvertProg.setEnabled(false);
                    lblAuthorProg.setEnabled(false);
                    prgCapture.setEnabled(false);
                    prgConvert.setEnabled(false);
                    prgAuthor.setEnabled(false);
                    lblCaptureProg.setText("0%");
                    lblConvertProg.setText("0%");
                    lblAuthorProg.setText("0%");
                }
            }
        } catch (IOException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("IO Error while reading the xml file" + "\nfor the requested dvd-homevideo session.\n" + ex.toString(), 0);
            ex.printStackTrace();
        }
    }

    /**
     * Saves the state of all widgets on the GUI form in an xml file
     * in the users ~/.dvd-homevideo directory
     */
    public void WriteSession() throws IOException {
        int i;
        javax.swing.JLabel[] lbl_widgets = { lblMinutes, lblSeconds, lblFormat, lblAspectRatio, lblPicture, lblAudio, lblOutputDir, lblTitle, lblTextFile };
        javax.swing.JSpinner[] spn_widgets = { spnMinutes, spnSeconds };
        javax.swing.JCheckBox[] chk_widgets = { chkQuality, chkMenu, chkBurn };
        javax.swing.JRadioButton[] rd_widgets = { rdSuper, rdGood, rdAverage, rdNTSC, rdPAL, rd4_3, rd16_9 };
        javax.swing.JTextField[] txt_widgets = { txtPicture, txtAudio, txtOutputDir, txtTitle, txtTextFile };
        javax.swing.JButton[] btn_widgets = { btnOpen_Picture, btnOpen_Audio, btnOpen_OutputDir, btnOpen_TextFile };
        javax.swing.JRadioButtonMenuItem[] chkMenu_widgets = { menuRd_IgnoreCap, menuRd_IgnoreCapConv, menuRd_IgnoreCapConvMenu };
        try {
            String home = System.getProperty("user.home");
            File msgboxSave = new File(home + "/.dvd-homevideo/properties");
            BufferedWriter msgboxSaveWriter = new BufferedWriter(new FileWriter(home + "/.dvd-homevideo/properties"));
            msgboxSaveWriter.close();
            BufferedWriter writer = new BufferedWriter(new FileWriter(home + "/.dvd-homevideo/" + txtTitle.getText() + ".xml"));
            writer.write("<dvd-homevideo>");
            writer.newLine();
            writer.write("\t<JLabel>");
            writer.newLine();
            for (i = 0; i < lbl_widgets.length; i++) {
                writer.write("\t\t<" + lbl_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + lbl_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t</" + lbl_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JLabel>");
            writer.newLine();
            writer.write("\t<JSpinner>");
            writer.newLine();
            for (i = 0; i < spn_widgets.length; i++) {
                writer.write("\t\t<" + spn_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + spn_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t\t<value>" + spn_widgets[i].getValue() + "</value>");
                writer.newLine();
                writer.write("\t\t</" + spn_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JSpinner>");
            writer.newLine();
            writer.write("\t<JCheckBox>");
            writer.newLine();
            for (i = 0; i < chk_widgets.length; i++) {
                writer.write("\t\t<" + chk_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + chk_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t\t<selected>" + chk_widgets[i].isSelected() + "</selected>");
                writer.newLine();
                writer.write("\t\t</" + chk_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JCheckBox>");
            writer.newLine();
            writer.write("\t<JRadioButton>");
            writer.newLine();
            for (i = 0; i < rd_widgets.length; i++) {
                writer.write("\t\t<" + rd_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + rd_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t\t<selected>" + rd_widgets[i].isSelected() + "</selected>");
                writer.newLine();
                writer.write("\t\t</" + rd_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JRadioButton>");
            writer.newLine();
            writer.write("\t<JTextField>");
            writer.newLine();
            for (i = 0; i < txt_widgets.length; i++) {
                writer.write("\t\t<" + txt_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + txt_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t\t<text>" + txt_widgets[i].getText() + "</text>");
                writer.newLine();
                writer.write("\t\t</" + txt_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JTextField>");
            writer.newLine();
            writer.write("\t<JButton>");
            writer.newLine();
            for (i = 0; i < btn_widgets.length; i++) {
                writer.write("\t\t<" + btn_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + btn_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t</" + btn_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JButton>");
            writer.newLine();
            writer.write("\t<JRadioButtonMenuItem>");
            writer.newLine();
            for (i = 0; i < chkMenu_widgets.length; i++) {
                writer.write("\t\t<" + chkMenu_widgets[i].getName() + ">");
                writer.newLine();
                writer.write("\t\t\t<enabled>" + chkMenu_widgets[i].isEnabled() + "</enabled>");
                writer.newLine();
                writer.write("\t\t\t<selected>" + chkMenu_widgets[i].isSelected() + "</selected>");
                writer.newLine();
                writer.write("\t\t</" + chkMenu_widgets[i].getName() + ">");
                writer.newLine();
            }
            writer.write("\t</JRadioButtonMenuItem>");
            writer.newLine();
            writer.write("</dvd-homevideo>");
            writer.newLine();
            writer.close();
        } catch (IOException ex) {
            SaveStackTrace.printTrace(strOutputDir, ex);
            MessageBox("IO Error while writing the xml file" + "\nfor this dvd-homevideo session.\n" + ex.toString(), 0);
            ex.printStackTrace();
        }
    }

    private javax.swing.JButton btnExit;

    private javax.swing.JButton btnOpen_Audio;

    private javax.swing.JButton btnOpen_OutputDir;

    private javax.swing.JButton btnOpen_Picture;

    private javax.swing.JButton btnOpen_TextFile;

    protected javax.swing.JButton btnPlay;

    private javax.swing.JButton btnStart;

    protected javax.swing.JCheckBox chkBurn;

    protected javax.swing.JCheckBox chkMenu;

    protected javax.swing.JCheckBox chkQuality;

    private javax.swing.JFileChooser fcMenuOpen;

    private javax.swing.JFileChooser fcOpen;

    private javax.swing.ButtonGroup grpAspectRatio;

    private javax.swing.ButtonGroup grpFormat;

    private javax.swing.ButtonGroup grpMenuAspectRatio;

    private javax.swing.ButtonGroup grpMenuFormat;

    private javax.swing.ButtonGroup grpMenuIgnore;

    private javax.swing.ButtonGroup grpMenuMode;

    private javax.swing.ButtonGroup grpQuality;

    private javax.swing.JLabel lblAspectRatio;

    private javax.swing.JLabel lblAudio;

    protected javax.swing.JLabel lblAuthor;

    protected javax.swing.JLabel lblAuthorProg;

    protected javax.swing.JLabel lblCapture;

    protected javax.swing.JLabel lblCaptureProg;

    protected javax.swing.JLabel lblConvert;

    protected javax.swing.JLabel lblConvertProg;

    private javax.swing.JLabel lblFormat;

    private javax.swing.JLabel lblMinutes;

    private javax.swing.JLabel lblOutputDir;

    private javax.swing.JLabel lblPicture;

    private javax.swing.JLabel lblSeconds;

    private javax.swing.JLabel lblTextFile;

    private javax.swing.JLabel lblTitle;

    private javax.swing.JMenuItem menuAbout;

    protected javax.swing.JMenuBar menuBarMain;

    private javax.swing.JMenuItem menuBug;

    protected javax.swing.JCheckBoxMenuItem menuChkThread;

    protected javax.swing.JMenuItem menuExit;

    protected javax.swing.JMenu menuFile;

    protected javax.swing.JMenu menuHelp;

    protected javax.swing.JMenuItem menuInternet;

    protected javax.swing.JMenuItem menuOpen;

    private javax.swing.JMenu menuProjProp;

    protected javax.swing.JMenuItem menuREADME;

    private javax.swing.JRadioButtonMenuItem menuRdNTSC;

    private javax.swing.JRadioButtonMenuItem menuRdPAL;

    private javax.swing.JRadioButtonMenuItem menuRd_16_9;

    private javax.swing.JRadioButtonMenuItem menuRd_4_3;

    protected javax.swing.JRadioButtonMenuItem menuRd_IgnoreCap;

    protected javax.swing.JRadioButtonMenuItem menuRd_IgnoreCapConv;

    protected javax.swing.JRadioButtonMenuItem menuRd_IgnoreCapConvMenu;

    protected javax.swing.JRadioButtonMenuItem menuRd_IgnoreNone;

    protected javax.swing.JMenuItem menuSave;

    protected javax.swing.JMenu menuTools;

    private javax.swing.JPanel pnlGUI;

    protected javax.swing.JProgressBar prgAuthor;

    protected javax.swing.JProgressBar prgCapture;

    protected javax.swing.JProgressBar prgConvert;

    protected javax.swing.JRadioButton rd16_9;

    protected javax.swing.JRadioButton rd4_3;

    protected javax.swing.JRadioButton rdAverage;

    protected javax.swing.JRadioButton rdGood;

    protected javax.swing.JRadioButton rdNTSC;

    protected javax.swing.JRadioButton rdPAL;

    protected javax.swing.JRadioButton rdSuper;

    private javax.swing.JScrollPane spTextArea;

    protected javax.swing.JSpinner spnMinutes;

    protected javax.swing.JSpinner spnSeconds;

    private javax.swing.JSeparator sprAspectRatio;

    private javax.swing.JSeparator sprCapConvert;

    private javax.swing.JSeparator sprIgnore;

    private javax.swing.JSeparator sprMenuAuthor;

    private javax.swing.JSeparator sprMenuFormatAspect;

    private javax.swing.JSeparator sprMenuThread;

    protected javax.swing.JSeparator sprOpen;

    protected javax.swing.JSeparator sprSave;

    protected javax.swing.JTextArea txtAreaOutput;

    protected javax.swing.JTextField txtAudio;

    protected javax.swing.JTextField txtOutputDir;

    protected javax.swing.JTextField txtPicture;

    protected javax.swing.JTextField txtStatus;

    protected javax.swing.JTextField txtTextFile;

    protected javax.swing.JTextField txtTitle;

    protected javax.swing.SpinnerNumberModel spnSecondsSize = new javax.swing.SpinnerNumberModel(0, 0, 59, 1);

    protected javax.swing.SpinnerNumberModel spnMinutesSize = new javax.swing.SpinnerNumberModel(0, 0, 64, 1);
}
