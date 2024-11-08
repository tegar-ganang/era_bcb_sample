package jstella.desktop;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import jstella.cart.*;
import java.awt.*;
import java.io.*;
import jstella.core.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import jstella.runner.*;
import jstella.runner.Intercessor;
import static jstella.core.JSConstants.*;
import static jstella.runner.JStellaRunnerUtil.*;
import static jstella.desktop.JStellaDesktop.*;

/**
 *
 * @author  J.L. Allen
 */
public class JStellaPlayer extends javax.swing.JFrame implements Intercessor.IfcIntercessorClient {

    private static final long serialVersionUID = -4623932099666097348L;

    private static final String WINDOW_TITLE = "JStella " + JSConstants.JSTELLA_VERSION;

    private String myJavaVersion = "<VERSION>";

    private Intercessor myIntercessor = null;

    private File myCurrentROMFile = null;

    private File myCurrentROMDirectory = null;

    private String myStateDirectory = "";

    private String myDefaultStateName = "";

    private boolean myJoystickEnabled = false;

    /** Creates new form JStellaMain */
    public JStellaPlayer() {
        System.out.println("Initializing player...");
        initComponents();
        myJavaVersion = System.getProperty("java.version");
        System.out.println("Java version: " + myJavaVersion);
        System.out.println("JStella version: " + JSTELLA_VERSION);
        this.setTitle(WINDOW_TITLE);
        myIntercessor = new Intercessor(this);
        myIntercessor.applyConfiguration(getConfiguration());
        myIntercessor.setAutoPauseMode(true);
        initAboutDialog();
        updateOptions();
        updateCartridgeStatus();
    }

    private void initComponents() {
        DialogAbout = new javax.swing.JDialog();
        PanelAboutSouth = new javax.swing.JPanel();
        ButtonAboutOK = new javax.swing.JButton();
        TPAbout = new javax.swing.JTextPane();
        ButtonGroupChannels = new javax.swing.ButtonGroup();
        ButtonGroupTVType = new javax.swing.ButtonGroup();
        ButtonGroupP0Difficulty = new javax.swing.ButtonGroup();
        ButtonGroupP1Difficulty = new javax.swing.ButtonGroup();
        MBMain = new javax.swing.JMenuBar();
        MenuFile = new javax.swing.JMenu();
        MILoadROM = new javax.swing.JMenuItem();
        SepFileA = new javax.swing.JSeparator();
        MISaveState = new javax.swing.JMenuItem();
        MILoadState = new javax.swing.JMenuItem();
        SepFileB = new javax.swing.JSeparator();
        MIExit = new javax.swing.JMenuItem();
        MenuSwitches = new javax.swing.JMenu();
        MIReset = new javax.swing.JMenuItem();
        MISelect = new javax.swing.JMenuItem();
        MenuTVType = new javax.swing.JMenu();
        RBMIBWTelevision = new javax.swing.JRadioButtonMenuItem();
        RBMIColorTelevision = new javax.swing.JRadioButtonMenuItem();
        MenuPlayer0Difficulty = new javax.swing.JMenu();
        RBMIPlayer0Amateur = new javax.swing.JRadioButtonMenuItem();
        RBMIPlayer0Professional = new javax.swing.JRadioButtonMenuItem();
        MenuPlayer1Difficulty = new javax.swing.JMenu();
        RBMIPlayer1Amateur = new javax.swing.JRadioButtonMenuItem();
        RBMIPlayer1Professional = new javax.swing.JRadioButtonMenuItem();
        MenuOptions = new javax.swing.JMenu();
        CBMIPaused = new javax.swing.JCheckBoxMenuItem();
        SepOptionsA = new javax.swing.JSeparator();
        CBMISoundEnabled = new javax.swing.JCheckBoxMenuItem();
        MenuSoundChannels = new javax.swing.JMenu();
        RBMIMonoSound = new javax.swing.JRadioButtonMenuItem();
        RBMIStereoSound = new javax.swing.JRadioButtonMenuItem();
        SepOptionsB = new javax.swing.JSeparator();
        CBMIPhosphorEnabled = new javax.swing.JCheckBoxMenuItem();
        CBMILetterBoxMode = new javax.swing.JCheckBoxMenuItem();
        SepOptionsC = new javax.swing.JSeparator();
        MIConfigure = new javax.swing.JMenuItem();
        MenuControls = new javax.swing.JMenu();
        MICBJoystickEnabled = new javax.swing.JCheckBoxMenuItem();
        MIVirtualJoystick = new javax.swing.JMenuItem();
        MenuHelp = new javax.swing.JMenu();
        MIHelpContents = new javax.swing.JMenuItem();
        MIAbout = new javax.swing.JMenuItem();
        DialogAbout.setTitle("About JStella");
        DialogAbout.setAlwaysOnTop(true);
        DialogAbout.setLocationByPlatform(true);
        DialogAbout.setModal(true);
        DialogAbout.setResizable(false);
        ButtonAboutOK.setText("OK");
        ButtonAboutOK.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonAboutOKActionPerformed(evt);
            }
        });
        PanelAboutSouth.add(ButtonAboutOK);
        DialogAbout.getContentPane().add(PanelAboutSouth, java.awt.BorderLayout.SOUTH);
        TPAbout.setBorder(null);
        TPAbout.setEditable(false);
        TPAbout.setFocusable(false);
        TPAbout.setOpaque(false);
        DialogAbout.getContentPane().add(TPAbout, java.awt.BorderLayout.CENTER);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("JStella");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        MenuFile.setMnemonic('F');
        MenuFile.setText("File");
        MILoadROM.setMnemonic('O');
        MILoadROM.setText("Open and play cartridge file");
        MILoadROM.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MILoadROMActionPerformed(evt);
            }
        });
        MenuFile.add(MILoadROM);
        MenuFile.add(SepFileA);
        MISaveState.setMnemonic('S');
        MISaveState.setText("Save current game");
        MISaveState.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISaveStateActionPerformed(evt);
            }
        });
        MenuFile.add(MISaveState);
        MILoadState.setMnemonic('L');
        MILoadState.setText("Load a saved game");
        MILoadState.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MILoadStateActionPerformed(evt);
            }
        });
        MenuFile.add(MILoadState);
        MenuFile.add(SepFileB);
        MIExit.setMnemonic('x');
        MIExit.setText("Exit JStella");
        MIExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIExitActionPerformed(evt);
            }
        });
        MenuFile.add(MIExit);
        MBMain.add(MenuFile);
        MenuSwitches.setMnemonic('S');
        MenuSwitches.setText("Switches");
        MenuSwitches.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuSwitchesActionPerformed(evt);
            }
        });
        MIReset.setMnemonic('R');
        MIReset.setText("Reset (F1)");
        MIReset.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIResetActionPerformed(evt);
            }
        });
        MenuSwitches.add(MIReset);
        MISelect.setMnemonic('S');
        MISelect.setText("Select (F2)");
        MISelect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MISelectActionPerformed(evt);
            }
        });
        MenuSwitches.add(MISelect);
        MenuTVType.setMnemonic('T');
        MenuTVType.setText("TV Type");
        ButtonGroupTVType.add(RBMIBWTelevision);
        RBMIBWTelevision.setMnemonic('B');
        RBMIBWTelevision.setText("Black and white (F3)");
        RBMIBWTelevision.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIBWTelevisionActionPerformed(evt);
            }
        });
        MenuTVType.add(RBMIBWTelevision);
        ButtonGroupTVType.add(RBMIColorTelevision);
        RBMIColorTelevision.setMnemonic('C');
        RBMIColorTelevision.setSelected(true);
        RBMIColorTelevision.setText("Color (F4)");
        RBMIColorTelevision.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIColorTelevisionActionPerformed(evt);
            }
        });
        MenuTVType.add(RBMIColorTelevision);
        MenuSwitches.add(MenuTVType);
        MenuPlayer0Difficulty.setMnemonic('0');
        MenuPlayer0Difficulty.setText("Left player difficulty");
        ButtonGroupP0Difficulty.add(RBMIPlayer0Amateur);
        RBMIPlayer0Amateur.setMnemonic('B');
        RBMIPlayer0Amateur.setSelected(true);
        RBMIPlayer0Amateur.setText("B (amateur) (F5)");
        RBMIPlayer0Amateur.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIPlayer0AmateurActionPerformed(evt);
            }
        });
        MenuPlayer0Difficulty.add(RBMIPlayer0Amateur);
        ButtonGroupP0Difficulty.add(RBMIPlayer0Professional);
        RBMIPlayer0Professional.setMnemonic('A');
        RBMIPlayer0Professional.setText("A (professional) (F6)");
        RBMIPlayer0Professional.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIPlayer0ProfessionalActionPerformed(evt);
            }
        });
        MenuPlayer0Difficulty.add(RBMIPlayer0Professional);
        MenuSwitches.add(MenuPlayer0Difficulty);
        MenuPlayer1Difficulty.setMnemonic('1');
        MenuPlayer1Difficulty.setText("Right player difficulty");
        ButtonGroupP1Difficulty.add(RBMIPlayer1Amateur);
        RBMIPlayer1Amateur.setMnemonic('B');
        RBMIPlayer1Amateur.setSelected(true);
        RBMIPlayer1Amateur.setText("B (amateur) (F7)");
        RBMIPlayer1Amateur.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIPlayer1AmateurActionPerformed(evt);
            }
        });
        MenuPlayer1Difficulty.add(RBMIPlayer1Amateur);
        ButtonGroupP1Difficulty.add(RBMIPlayer1Professional);
        RBMIPlayer1Professional.setMnemonic('A');
        RBMIPlayer1Professional.setText("A (professional) (F8)");
        RBMIPlayer1Professional.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIPlayer1ProfessionalActionPerformed(evt);
            }
        });
        MenuPlayer1Difficulty.add(RBMIPlayer1Professional);
        MenuSwitches.add(MenuPlayer1Difficulty);
        MBMain.add(MenuSwitches);
        MenuOptions.setMnemonic('O');
        MenuOptions.setText("Options");
        CBMIPaused.setMnemonic('P');
        CBMIPaused.setText("Pause");
        CBMIPaused.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CBMIPausedActionPerformed(evt);
            }
        });
        MenuOptions.add(CBMIPaused);
        MenuOptions.add(SepOptionsA);
        CBMISoundEnabled.setMnemonic('S');
        CBMISoundEnabled.setSelected(true);
        CBMISoundEnabled.setText("Sound enabled");
        CBMISoundEnabled.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CBMISoundEnabledActionPerformed(evt);
            }
        });
        MenuOptions.add(CBMISoundEnabled);
        MenuSoundChannels.setMnemonic('c');
        MenuSoundChannels.setText("Audio channels");
        ButtonGroupChannels.add(RBMIMonoSound);
        RBMIMonoSound.setMnemonic('M');
        RBMIMonoSound.setSelected(true);
        RBMIMonoSound.setText("Mono");
        RBMIMonoSound.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIMonoSoundActionPerformed(evt);
            }
        });
        MenuSoundChannels.add(RBMIMonoSound);
        ButtonGroupChannels.add(RBMIStereoSound);
        RBMIStereoSound.setMnemonic('S');
        RBMIStereoSound.setText("Stereo");
        RBMIStereoSound.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RBMIStereoSoundActionPerformed(evt);
            }
        });
        MenuSoundChannels.add(RBMIStereoSound);
        MenuOptions.add(MenuSoundChannels);
        MenuOptions.add(SepOptionsB);
        CBMIPhosphorEnabled.setMnemonic('f');
        CBMIPhosphorEnabled.setText("Anti-flicker mode");
        CBMIPhosphorEnabled.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CBMIPhosphorEnabledActionPerformed(evt);
            }
        });
        MenuOptions.add(CBMIPhosphorEnabled);
        CBMILetterBoxMode.setMnemonic('L');
        CBMILetterBoxMode.setText("Letterbox mode");
        CBMILetterBoxMode.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CBMILetterBoxModeActionPerformed(evt);
            }
        });
        MenuOptions.add(CBMILetterBoxMode);
        MenuOptions.add(SepOptionsC);
        MIConfigure.setMnemonic('C');
        MIConfigure.setText("Configure...");
        MIConfigure.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIConfigureActionPerformed(evt);
            }
        });
        MenuOptions.add(MIConfigure);
        MBMain.add(MenuOptions);
        MenuControls.setMnemonic('C');
        MenuControls.setText("Controls");
        MICBJoystickEnabled.setSelected(true);
        MICBJoystickEnabled.setText("Joystick enabled");
        MICBJoystickEnabled.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MICBJoystickEnabledActionPerformed(evt);
            }
        });
        MenuControls.add(MICBJoystickEnabled);
        MIVirtualJoystick.setMnemonic('j');
        MIVirtualJoystick.setText("Toggle virtual joystick");
        MIVirtualJoystick.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIVirtualJoystickActionPerformed(evt);
            }
        });
        MenuControls.add(MIVirtualJoystick);
        MBMain.add(MenuControls);
        MenuHelp.setMnemonic('H');
        MenuHelp.setText("Help");
        MIHelpContents.setMnemonic('H');
        MIHelpContents.setText("Help contents");
        MIHelpContents.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIHelpContentsActionPerformed(evt);
            }
        });
        MenuHelp.add(MIHelpContents);
        MIAbout.setMnemonic('A');
        MIAbout.setText("About");
        MIAbout.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MIAboutActionPerformed(evt);
            }
        });
        MenuHelp.add(MIAbout);
        MBMain.add(MenuHelp);
        setJMenuBar(MBMain);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 509) / 2, (screenSize.height - 435) / 2, 509, 435);
    }

    private void MIExitActionPerformed(java.awt.event.ActionEvent evt) {
        JStellaDesktop.closeFrame(this);
    }

    private void CBMILetterBoxModeActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setLetterBoxMode(CBMILetterBoxMode.isSelected());
    }

    private void MIHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {
        JStellaHelp.runJStellaHelp(this);
    }

    private void MIVirtualJoystickActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.toggleVirtualJoystick(this);
    }

    private void MILoadStateActionPerformed(java.awt.event.ActionEvent evt) {
        loadState();
    }

    private void MISaveStateActionPerformed(java.awt.event.ActionEvent evt) {
        saveState();
    }

    private void MISelectActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.emulateSelectPress();
    }

    private void MIConfigureActionPerformed(java.awt.event.ActionEvent evt) {
        boolean zChanged = JStellaDesktop.showConfigurationDialog(this);
        if (zChanged == true) {
            myIntercessor.updateTelevisionMode();
        }
    }

    private void RBMIColorTelevisionActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setTVTypeBW(false);
    }

    private void RBMIBWTelevisionActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setTVTypeBW(true);
    }

    private void RBMIPlayer1ProfessionalActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setPlayer1Amateur(false);
    }

    private void RBMIPlayer1AmateurActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setPlayer1Amateur(true);
    }

    private void RBMIPlayer0ProfessionalActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setPlayer0Amateur(false);
    }

    private void RBMIPlayer0AmateurActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setPlayer0Amateur(true);
    }

    private void CBMIPausedActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setPausedByPlayer(CBMIPaused.isSelected());
    }

    private void MenuSwitchesActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void MIResetActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.emulateResetPress();
    }

    private void RBMIStereoSoundActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setStereoSound(true);
    }

    private void RBMIMonoSoundActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setStereoSound(false);
    }

    private void CBMIPhosphorEnabledActionPerformed(java.awt.event.ActionEvent evt) {
        if (myIntercessor != null) {
            myIntercessor.setPhosphorEnabled(CBMIPhosphorEnabled.isSelected());
        }
    }

    private void CBMISoundEnabledActionPerformed(java.awt.event.ActionEvent evt) {
        myIntercessor.setSoundEnabled(CBMISoundEnabled.isSelected());
    }

    private void ButtonAboutOKActionPerformed(java.awt.event.ActionEvent evt) {
        DialogAbout.setVisible(false);
    }

    private void MIAboutActionPerformed(java.awt.event.ActionEvent evt) {
        DialogAbout.setLocationRelativeTo(this);
        DialogAbout.setSize(400, 200);
        DialogAbout.setVisible(true);
    }

    private void MILoadROMActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            loadROM();
        } catch (JSException e) {
            respondToException(e);
        }
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        JStellaDesktop.closeFrame(this);
    }

    private void MICBJoystickEnabledActionPerformed(java.awt.event.ActionEvent evt) {
        myJoystickEnabled = MICBJoystickEnabled.isSelected();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        final String zROMDir;
        if (args.length > 0) {
            zROMDir = args[0];
        } else {
            zROMDir = "";
        }
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                JStellaPlayer zJSM = new JStellaPlayer();
                zJSM.setCurrentROMDirectory(new File(zROMDir));
                zJSM.setVisible(true);
            }
        });
    }

    private void setCurrentROMDirectory(File aROMDir) {
        myCurrentROMDirectory = aROMDir;
    }

    private File getDefaultROMDirectory() {
        String zDefaultROMDir = getConfiguration().get(CONFIG_KEY_DEFAULT_ROM_DIR);
        if (zDefaultROMDir == null) zDefaultROMDir = "";
        return new File(zDefaultROMDir);
    }

    private File getCurrentROMDirectory() {
        if (myCurrentROMDirectory == null) {
            myCurrentROMDirectory = getDefaultROMDirectory();
        }
        return myCurrentROMDirectory;
    }

    private void initAboutDialog() {
        SimpleAttributeSet zSAS = new SimpleAttributeSet();
        zSAS.addAttribute(StyleConstants.Alignment, StyleConstants.ALIGN_CENTER);
        zSAS.addAttribute(StyleConstants.Bold, Boolean.valueOf(true));
        StringBuffer zSBuf = new StringBuffer();
        zSBuf.append("" + JSTELLA_LONGTITLE + "\n");
        zSBuf.append("Version: " + JSTELLA_VERSION + "\n");
        zSBuf.append("" + JSTELLA_BYLINE_CORE + "\n");
        zSBuf.append("" + JSTELLA_BYLINE_JAVA + "\n");
        zSBuf.append("" + JSTELLA_HTTP + "\n");
        try {
            StyledDocument zSDoc = TPAbout.getStyledDocument();
            zSDoc.remove(0, zSDoc.getLength());
            zSDoc.insertString(0, zSBuf.toString(), null);
            zSDoc.setParagraphAttributes(0, zSDoc.getLength(), zSAS, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public boolean respondToException(JSException e) {
        myIntercessor.showDefaultExceptionResponse(e, this);
        return true;
    }

    private void updateOptions() {
        CBMISoundEnabled.setSelected(myIntercessor.isSoundEnabled());
        CBMIPhosphorEnabled.setSelected(myIntercessor.isPhosphorEnabled());
        if (myIntercessor.isStereoSound() == true) {
            RBMIStereoSound.setSelected(true);
        } else {
            RBMIMonoSound.setSelected(true);
        }
        CBMIPaused.setSelected(myIntercessor.isPausedByPlayer());
        if (myIntercessor.isTVTypeBW() == true) {
            RBMIBWTelevision.setSelected(true);
        } else {
            RBMIColorTelevision.setSelected(true);
        }
        if (myIntercessor.isPlayer0Amateur() == true) {
            RBMIPlayer0Amateur.setSelected(true);
        } else {
            RBMIPlayer0Professional.setSelected(true);
        }
        if (myIntercessor.isPlayer1Amateur() == true) {
            RBMIPlayer1Amateur.setSelected(true);
        } else {
            RBMIPlayer1Professional.setSelected(true);
        }
        CBMILetterBoxMode.setSelected(myIntercessor.getLetterBoxMode());
    }

    private void initDefaultStateName(String aPlainName) {
        if (aPlainName.equals("") == false) {
            myDefaultStateName = aPlainName + ".jssg";
        } else {
            myDefaultStateName = DEFAULT_STATE_NAME;
        }
    }

    public void playJStellaGame(JStellaGame aGame) {
        String zPlainFilename = getFilenameWithoutExtension(aGame.getGameFilename());
        initDefaultStateName(zPlainFilename);
        myIntercessor.playJStellaGame(aGame);
        this.setVisible(true);
    }

    /**
     * (Rename this method)
     * This plays the given ROM.
     * @param aROMData the ROM data
     * @param aPlainFilename the name that will be the default state filename.  It should not have an extension.
     * @param aConfigMap the game's particular configuration (overrides general config)
     */
    public void loadROM(byte[] aROMData, String aPlainFilename, java.util.Map<String, String> aConfigMap) {
        try {
            initDefaultStateName(aPlainFilename);
            ByteArrayInputStream zBAIS = new ByteArrayInputStream(aROMData);
            myIntercessor.playROM(zBAIS);
            if (aConfigMap != null) {
                myIntercessor.applyConfiguration(aConfigMap);
            }
            zBAIS.close();
            this.setVisible(true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadROM(File aROMFile) throws IOException {
        FileInputStream zFIS = new FileInputStream(aROMFile);
        myCurrentROMFile = aROMFile;
        System.out.println("Loading ROM : " + aROMFile.toString());
        myIntercessor.playROM(zFIS);
        zFIS.close();
        String zFileNameWithoutExtension = JStellaRunnerUtil.getFilenameWithoutExtension(aROMFile);
        initDefaultStateName(zFileNameWithoutExtension);
    }

    private void loadROM() throws JSException {
        try {
            String zCurrentDir = "";
            configureFileBrowser(false, false, true, getCurrentROMDirectory(), null, JSFileNameExtensionFilter.FILTER_ROMS);
            int zResult = getFileBrowser().showOpenDialog(this);
            if (zResult == JFileChooser.APPROVE_OPTION) {
                loadROM(getFileBrowser().getSelectedFile());
                setCurrentROMDirectory(getFileBrowser().getCurrentDirectory());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        updateCartridgeStatus();
    }

    private File showSaveDialog(String aDefaultDirectory, String aDefaultFileName, JSFileNameExtensionFilter aFilter) {
        File zReturn = null;
        JFileChooser zFileBrowser = getFileBrowser();
        configureFileBrowser(false, false, true, new File(aDefaultDirectory), new File(aDefaultFileName), aFilter);
        boolean zChooseAgain = false;
        do {
            int zResult = zFileBrowser.showSaveDialog(this);
            if (zResult == JFileChooser.APPROVE_OPTION) {
                if (zFileBrowser.getSelectedFile().exists() == true) {
                    int zConfirmResult = JOptionPane.showConfirmDialog(this, "" + zFileBrowser.getSelectedFile().getName() + " already exists.  Do you wish to overwrite?");
                    if (zConfirmResult == JOptionPane.NO_OPTION) {
                        zChooseAgain = true;
                    } else if (zConfirmResult == JOptionPane.YES_OPTION) {
                        zReturn = zFileBrowser.getSelectedFile();
                        zChooseAgain = false;
                    } else {
                        zChooseAgain = false;
                    }
                } else {
                    zReturn = zFileBrowser.getSelectedFile();
                }
            } else {
                zChooseAgain = false;
            }
        } while (zChooseAgain == true);
        return zReturn;
    }

    private int isSaveAcceptable(File aFileToSave) {
        int zReturn = JOptionPane.YES_OPTION;
        return zReturn;
    }

    private void loadState() {
        try {
            String zCurrentDir = "";
            if (!myStateDirectory.trim().equals("")) {
                zCurrentDir = myStateDirectory;
            } else if (getConfiguration().containsKey(CONFIG_KEY_DEFAULT_STATE_DIR) == true) {
                zCurrentDir = getConfiguration().get(CONFIG_KEY_DEFAULT_STATE_DIR);
            }
            configureFileBrowser(false, false, true, new File(zCurrentDir), null, JSFileNameExtensionFilter.FILTER_JSTELLA_STATE);
            int zResult = getFileBrowser().showOpenDialog(this);
            if (zResult == JFileChooser.APPROVE_OPTION) {
                FileInputStream zFIS = new FileInputStream(getFileBrowser().getSelectedFile());
                myIntercessor.loadStateFromStream(zFIS);
                updateOptions();
                updateCartridgeStatus();
                myStateDirectory = getFileBrowser().getCurrentDirectory().getPath();
                myDefaultStateName = getFileBrowser().getSelectedFile().getName();
            }
        } catch (java.io.ObjectStreamException e) {
            JOptionPane.showMessageDialog(this, "Saved game is incompatible with this version of JStella", "Error loading game", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Could not load saved game: " + getFileBrowser().getSelectedFile().getName(), "Error loading game", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Could not load saved game: " + getFileBrowser().getSelectedFile().getName(), "Error loading game", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        updateCartridgeStatus();
    }

    private void saveState() {
        try {
            JOptionPane.showMessageDialog(this, "Because JStella is still in development, saved games may not be compatible with future releases of JStella", "Warning", JOptionPane.WARNING_MESSAGE);
            String zDefaultDirectory = "";
            if (!myStateDirectory.trim().equals("")) {
                zDefaultDirectory = myStateDirectory;
            } else if (getConfiguration().containsKey(CONFIG_KEY_DEFAULT_STATE_DIR) == true) {
                zDefaultDirectory = getConfiguration().get(CONFIG_KEY_DEFAULT_STATE_DIR);
            }
            File zSelectedFile = showSaveDialog(zDefaultDirectory, myDefaultStateName, JSFileNameExtensionFilter.FILTER_JSTELLA_STATE);
            if (zSelectedFile != null) {
                FileOutputStream zFOS = new FileOutputStream(zSelectedFile);
                myIntercessor.saveStateToStream(zFOS);
                myStateDirectory = getFileBrowser().getCurrentDirectory().getPath();
                myDefaultStateName = zSelectedFile.getName();
                JOptionPane.showMessageDialog(this, "Game saved");
            }
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save current game to file: " + getFileBrowser().getSelectedFile().getName(), "Error saving game", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateCartridgeStatus() {
        boolean zCartridgeLoaded = (myIntercessor.getCartridge() != null);
        MISaveState.setEnabled(zCartridgeLoaded);
    }

    public void displayCanvas(JPanel aCanvas) {
        this.getContentPane().add(aCanvas, java.awt.BorderLayout.CENTER);
        aCanvas.revalidate();
        aCanvas.requestFocusInWindow();
    }

    private void updatePauseStatus() {
        if (myIntercessor.isPausedByPlayer() == true) {
            this.setTitle(WINDOW_TITLE + " (paused)");
        } else {
            this.setTitle(WINDOW_TITLE);
        }
    }

    public void informUserOfPause(boolean aIsPaused) {
        updatePauseStatus();
    }

    public void updateSwitches() {
        updateOptions();
    }

    /**
     * This alters the appearance of the player depending on whether or not it
     * is being run in "Classic" or "Repository" mode.
     * @param aRepositoryMode true if this being run in repository mode, false if in classic mode
     */
    public void updateRepositoryMode(boolean aRepositoryMode) {
        System.out.println("updateRepositoryMode(" + aRepositoryMode + ")");
        MIExit.setText(aRepositoryMode ? STRING_EXIT_TO_REPOSITORY : STRING_EXIT_JSTELLA);
    }

    public java.util.Map<String, String> getConfiguration() {
        return JStellaDesktop.getConfiguration();
    }

    public void updateGUI() {
        updateOptions();
    }

    private javax.swing.JButton ButtonAboutOK;

    private javax.swing.ButtonGroup ButtonGroupChannels;

    private javax.swing.ButtonGroup ButtonGroupP0Difficulty;

    private javax.swing.ButtonGroup ButtonGroupP1Difficulty;

    private javax.swing.ButtonGroup ButtonGroupTVType;

    private javax.swing.JCheckBoxMenuItem CBMILetterBoxMode;

    private javax.swing.JCheckBoxMenuItem CBMIPaused;

    private javax.swing.JCheckBoxMenuItem CBMIPhosphorEnabled;

    private javax.swing.JCheckBoxMenuItem CBMISoundEnabled;

    private javax.swing.JDialog DialogAbout;

    private javax.swing.JMenuBar MBMain;

    private javax.swing.JMenuItem MIAbout;

    private javax.swing.JCheckBoxMenuItem MICBJoystickEnabled;

    private javax.swing.JMenuItem MIConfigure;

    private javax.swing.JMenuItem MIExit;

    private javax.swing.JMenuItem MIHelpContents;

    private javax.swing.JMenuItem MILoadROM;

    private javax.swing.JMenuItem MILoadState;

    private javax.swing.JMenuItem MIReset;

    private javax.swing.JMenuItem MISaveState;

    private javax.swing.JMenuItem MISelect;

    private javax.swing.JMenuItem MIVirtualJoystick;

    private javax.swing.JMenu MenuControls;

    private javax.swing.JMenu MenuFile;

    private javax.swing.JMenu MenuHelp;

    private javax.swing.JMenu MenuOptions;

    private javax.swing.JMenu MenuPlayer0Difficulty;

    private javax.swing.JMenu MenuPlayer1Difficulty;

    private javax.swing.JMenu MenuSoundChannels;

    private javax.swing.JMenu MenuSwitches;

    private javax.swing.JMenu MenuTVType;

    private javax.swing.JPanel PanelAboutSouth;

    private javax.swing.JRadioButtonMenuItem RBMIBWTelevision;

    private javax.swing.JRadioButtonMenuItem RBMIColorTelevision;

    private javax.swing.JRadioButtonMenuItem RBMIMonoSound;

    private javax.swing.JRadioButtonMenuItem RBMIPlayer0Amateur;

    private javax.swing.JRadioButtonMenuItem RBMIPlayer0Professional;

    private javax.swing.JRadioButtonMenuItem RBMIPlayer1Amateur;

    private javax.swing.JRadioButtonMenuItem RBMIPlayer1Professional;

    private javax.swing.JRadioButtonMenuItem RBMIStereoSound;

    private javax.swing.JSeparator SepFileA;

    private javax.swing.JSeparator SepFileB;

    private javax.swing.JSeparator SepOptionsA;

    private javax.swing.JSeparator SepOptionsB;

    private javax.swing.JSeparator SepOptionsC;

    private javax.swing.JTextPane TPAbout;

    public void runExtraLoopRoutines() {
    }
}
