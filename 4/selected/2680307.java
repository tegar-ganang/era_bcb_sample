package org.gromurph.javascore;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.UIManager.LookAndFeelInfo;
import org.gromurph.javascore.actions.*;
import org.gromurph.javascore.gui.*;
import org.gromurph.javascore.manager.RegattaManager;
import org.gromurph.javascore.manager.ReportViewer;
import org.gromurph.javascore.model.Regatta;
import org.gromurph.util.*;
import org.gromurph.util.swingworker.SwingWorker;

public class JavaScore extends JFrame implements PropertyChangeListener, ActionListener, WindowListener {

    private static final String SPLASH_GRAPHIC = "/images/SplashGraphic.jpg";

    private static ResourceBundle res = null;

    private static ResourceBundle resUtil = null;

    private static long sStartTimeStatic;

    private Regatta fRegatta = null;

    private static SplashScreen sSplash;

    private static DialogBaseEditor sDialogRegatta;

    private static DialogBaseEditor sDialogPreferences;

    private static DialogReportTabs sDialogReportOptions;

    private static DialogEntryTreeEditor sDialogEntries;

    private static DialogRaceEditor sDialogRaces;

    JMenuBar fMenuBar = new JMenuBar();

    JMenu fMenuFile = new JMenu();

    JMenuItem fItemNew = new JMenuItem();

    JMenuItem fItemOpen = new JMenuItem();

    JMenuItem fItemSave = new JMenuItem();

    JMenuItem fItemSaveAs = new JMenuItem();

    JMenuItem fItemRestore = new JMenuItem();

    JMenuItem fItemRecent1 = new JMenuItem();

    JMenuItem fItemRecent2 = new JMenuItem();

    JMenuItem fItemRecent3 = new JMenuItem();

    JMenu fItemImport = new JMenu();

    JMenuItem fItemPreferences = new JMenuItem();

    JMenuItem fItemExit = new JMenuItem();

    JMenu fMenuReports = new JMenu();

    JMenuItem fItemShowReports = new JMenuItem();

    JMenuItem fItemReportOptions = new JMenuItem();

    JMenu fMenuDivisions = new JMenu();

    Action fActionMasterDivisions = new ActionEditMasterDivisions();

    Action fActionFleets = new ActionEditFleets();

    Action fActionSubDivisions = new ActionEditSubDivisions();

    JMenu fMenuHelp = new JMenu();

    JMenu fItemLocale = new JMenu();

    JMenuItem fItemHelp = new JMenuItem();

    Action fActionImportEntries = new ActionImportEntries();

    Action fActionImportResults = new ActionImportResults();

    Action fActionImportMarkRoundings = new ActionImportMarkRoundings();

    Action fActionExport = new ActionExport();

    Action fActionAbout = new ActionAbout();

    Action fActionSplitFleet = new ActionSplitFleet();

    JPanel fToolBar = new JPanel();

    JButton fButtonRegatta;

    JButton fButtonEntries;

    JButton fButtonRaces;

    static {
        sStartTimeStatic = System.currentTimeMillis();
        ToolTipManager.sharedInstance().setDismissDelay(2000);
        String iniLocale = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.LOCALE_PROPERTY);
        if (iniLocale != null) Util.initLocale(iniLocale);
        res = JavaScoreProperties.getResources();
        resUtil = Util.getResources();
        String laf = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.LOOKANDFEEL_PROPERTY);
        if (laf == null) {
            laf = UIManager.getSystemLookAndFeelClassName();
        }
        JavaScore.setLookAndFeel(laf);
    }

    public static void main(String args[]) {
        Util.checkJreVersion("1.6.0", "Java 6.0");
        sSplash = new SplashScreen(SPLASH_GRAPHIC, JavaScoreProperties.getVersion(), JavaScoreProperties.getRelease());
        new Timer(4000, new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                hideSplash();
            }
        }).start();
        getInstance();
        try {
            Util.setDumpTitle(res.getString("MainTitle") + " " + JavaScoreProperties.getVersion());
            if (args.length > 0) {
                try {
                    Regatta reg = RegattaManager.readRegattaFromDisk(Util.getWorkingDirectory(), args[0]);
                    getInstance().setRegatta(reg);
                } catch (Exception e) {
                }
            }
            getInstance().setVisible(true);
        } catch (Exception e) {
            Util.showError(e, true);
        }
    }

    public static JavaScore getInstance() {
        if (sInstance == null) sInstance = new JavaScore();
        return sInstance;
    }

    public static void hideSplash() {
        if (sSplash != null) sSplash.setVisible(false);
        sSplash = null;
    }

    private static JavaScore sInstance;

    public void updateTitle() {
        StringBuffer title = new StringBuffer();
        title.append(res.getString("MainTitle"));
        title.append(" ");
        title.append(JavaScoreProperties.getVersion());
        title.append(": ");
        if (fRegatta != null) {
            title.append(fRegatta.toString());
        } else {
            title.append(res.getString("MainMessageNoRegattaTag"));
        }
        setTitle(title.toString());
    }

    private void runSwingWorkers() {
        Thread workerThread = new Thread() {

            public void run() {
                List<SwingWorker> workers = new ArrayList<SwingWorker>(10);
                workers.add(swFileChooser);
                workers.add(swRegatta);
                workers.add(swRaces);
                workers.add(swReport);
                workers.add(swReportOptions);
                workers.add(swEntries);
                workers.add(swDivisions);
                workers.add(swFleets);
                workers.add(swSubdivisions);
                workers.add(swPreferences);
                for (Iterator iw = workers.iterator(); iw.hasNext(); ) {
                    SwingWorker w = (SwingWorker) iw.next();
                    Object wo = null;
                    try {
                        w.start();
                        wo = w.get();
                        System.out.println("sw done: " + (wo == null ? "null" : wo.getClass().getName()));
                    } catch (Exception e) {
                        System.out.println("sw failed: " + (wo == null ? "null" : wo.getClass().getName()));
                    }
                }
            }
        };
        workerThread.start();
    }

    private JavaScore() {
        getContentPane().setLayout(new BorderLayout(0, 0));
        HelpManager.getInstance().setPrimarySource(this);
        HelpManager.getInstance().setMainHelpSet(JavaScoreProperties.HELP_SET);
        HelpManager.getInstance().enableWindowHelp(this, JavaScoreProperties.HELP_ROOT, this);
        addMenus();
        addToolbar();
        runSwingWorkers();
        updateEnabled();
        JavaScoreProperties.getInstance().updateBrowserLauncher();
        hideSplash();
        this.addWindowListener(this);
        pack();
        int winX = getToolkit().getScreenSize().width;
        int winY = getToolkit().getScreenSize().height;
        int prefX = getSize().width;
        this.setLocation(winX / 2 - prefX / 2, (int) (winY * .10));
        System.out.println(MessageFormat.format(res.getString("MainMessageContructorCompleted"), new Object[] { new Long(System.currentTimeMillis() - sStartTimeStatic) }));
        System.out.print(res.getString("MainMessageLocale"));
        System.out.print(": ");
        System.out.println(Locale.getDefault());
        updateTitle();
        Util.getImageIcon(this, JavaScoreProperties.PROTESTFLAG_ICON);
        Util.getImageIcon(this, HelpManager.CONTEXTHELP_ICON);
    }

    private void updateEnabled() {
        if ((sDialogEntries != null && sDialogEntries.isVisible()) || (sDialogRaces != null && sDialogRaces.isVisible()) || (sDialogPreferences != null && sDialogPreferences.isVisible()) || (sDialogReportOptions != null && sDialogReportOptions.isVisible()) || (sDialogRegatta != null && sDialogRegatta.isVisible())) {
            fButtonRegatta.setEnabled(false);
            fButtonEntries.setEnabled(false);
            fButtonRaces.setEnabled(false);
        } else {
            fButtonRegatta.setEnabled(true);
            boolean haveRegatta = (fRegatta != null);
            fButtonEntries.setEnabled(haveRegatta);
            fButtonRaces.setEnabled(haveRegatta);
            fMenuReports.setEnabled(haveRegatta);
            fItemSave.setEnabled(haveRegatta);
            fItemSaveAs.setEnabled(haveRegatta);
            fItemRestore.setEnabled(haveRegatta);
            fActionExport.setEnabled(haveRegatta);
            fItemImport.setEnabled(haveRegatta);
            fActionImportEntries.setEnabled(haveRegatta);
            fActionImportResults.setEnabled(haveRegatta);
            fActionImportMarkRoundings.setEnabled(haveRegatta);
            fItemReportOptions.setEnabled(haveRegatta && !fRegatta.isFinal());
            if (fActionSubDivisions != null) {
                fActionSubDivisions.setEnabled(haveRegatta);
                fActionMasterDivisions.setEnabled(true);
                fActionFleets.setEnabled(haveRegatta);
                fActionSplitFleet.setEnabled(haveRegatta && fRegatta.isSplitFleet());
            }
        }
    }

    private void addToolbar() {
        getContentPane().add(fToolBar, BorderLayout.CENTER);
        fToolBar.setLayout(new FlowLayout(FlowLayout.CENTER));
        fButtonRegatta = new JButton(res.getString("MainButtonRegatta"));
        fButtonRegatta.setName("fButtonRegatta");
        fButtonRegatta.setEnabled(false);
        HelpManager.getInstance().registerHelpTopic(fButtonRegatta, "main.buttonRegatta");
        fToolBar.add(fButtonRegatta);
        fButtonEntries = new JButton(res.getString("MainButtonEntries"));
        fButtonEntries.setName("fButtonEntries");
        fButtonEntries.setMnemonic('E');
        HelpManager.getInstance().registerHelpTopic(fButtonEntries, "main.buttonEntries");
        fToolBar.add(fButtonEntries);
        fButtonRaces = new JButton(res.getString("MainButtonRace"));
        fButtonRaces.setName("fButtonRaces");
        fButtonRaces.setMnemonic('R');
        HelpManager.getInstance().registerHelpTopic(fButtonRaces, "main.buttonRaces");
        fToolBar.add(fButtonRaces);
        if (!Util.isMac()) {
            Icon buttonIcon = Util.getImageIcon(this, Util.OPEN_ICON);
            if (buttonIcon != null) fButtonRegatta.setIcon(buttonIcon);
            buttonIcon = Util.getImageIcon(this, Util.EDIT_ICON);
            if (buttonIcon != null) fButtonEntries.setIcon(buttonIcon);
            buttonIcon = Util.getImageIcon(this, JavaScoreProperties.FINISHES_ICON);
            if (buttonIcon != null) fButtonRaces.setIcon(buttonIcon);
        }
        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JComponent csh = HelpManager.getInstance().createCSHButton();
        csh.setToolTipText(res.getString("MainLabelContextHelpToolTip"));
        getContentPane().add(helpPanel, BorderLayout.EAST);
        helpPanel.add(csh);
        fButtonRegatta.setToolTipText(res.getString("MainButtonRegattaToolTip"));
        fButtonEntries.setToolTipText(res.getString("MainButtonEntriesToolTip"));
        fButtonRaces.setToolTipText(res.getString("RaceButtonToolTip"));
    }

    private void addMenus() {
        addMenuFile();
        addMenuReports();
        addMenuDivisions();
        addMenuHelp();
        setJMenuBar(fMenuBar);
    }

    private void addMenuDivisions() {
        fMenuDivisions.setText(res.getString("MenuDivisions"));
        fMenuDivisions.setMnemonic(res.getString("MenuDivisionsMnemonic").charAt(0));
        fMenuBar.add(fMenuDivisions);
        HelpManager.getInstance().registerHelpTopic(fMenuDivisions, "main.menuMasterFiles");
        fMenuDivisions.setEnabled(true);
        fMenuDivisions.add(fActionMasterDivisions);
        fActionMasterDivisions.setEnabled(true);
        fMenuDivisions.add(fActionFleets);
        fActionFleets.setEnabled(false);
        fMenuDivisions.add(fActionSubDivisions);
        fActionSubDivisions.setEnabled(false);
        fMenuDivisions.add(fActionSplitFleet);
        fActionSplitFleet.setEnabled(false);
    }

    private void addMenuFile() {
        fMenuFile.setText(resUtil.getString("FileMenu"));
        fMenuFile.setName("fMenuFile");
        fMenuFile.setMnemonic(resUtil.getString("FileMenuMnemonic").charAt(0));
        HelpManager.getInstance().registerHelpTopic(fMenuFile, "main.fMenuFile");
        fMenuFile.add(fItemNew);
        fItemNew.setName("fItemNew");
        fItemNew.setText(res.getString("MenuNewRegatta"));
        fItemNew.setMnemonic(res.getString("MenuNewRegattaMnemonic").charAt(0));
        fMenuFile.add(fItemOpen);
        fItemOpen.setName("fItemOpen");
        fItemOpen.setText(res.getString("MenuOpenRegatta") + "...");
        fItemOpen.setMnemonic(res.getString("MenuOpenRegattaMnemonic").charAt(0));
        fItemOpen.setEnabled(false);
        fMenuFile.add(fItemSave);
        fItemSave.setName("fItemSave");
        fItemSave.setText(res.getString("MenuSaveRegatta"));
        fItemSave.setMnemonic(res.getString("MenuSaveRegattaMnemonic").charAt(0));
        fMenuFile.add(fItemSaveAs);
        fItemSaveAs.setName("fItemSaveAs");
        fItemSaveAs.setText(res.getString("MenuSaveRegattaAs") + "...");
        fItemSaveAs.setMnemonic(res.getString("MenuSaveRegattaAsMnemonic").charAt(0));
        fMenuFile.add(fItemRestore);
        fItemRestore.setName("fItemRestore");
        fItemRestore.setText(res.getString("MenuRestoreRegatta") + "...");
        fItemRestore.setMnemonic(res.getString("MenuRestoreRegattaMnemonic").charAt(0));
        fMenuFile.addSeparator();
        fItemImport.setText(res.getString("MenuImport"));
        fItemImport.setName("fItemImport");
        fItemImport.setMnemonic(res.getString("MenuImportMnemonic").charAt(0));
        fMenuFile.add(fItemImport);
        fItemImport.add(fActionImportEntries);
        fItemImport.add(fActionImportResults);
        fItemImport.add(fActionImportMarkRoundings);
        fMenuFile.setName("fMenuFile");
        fMenuFile.add(fActionExport);
        fMenuFile.addSeparator();
        fItemPreferences.setName("fItemPreferences");
        fItemPreferences.setText(res.getString("MenuPreferences") + "...");
        fItemPreferences.setMnemonic(res.getString("MenuPreferencesMnemonic").charAt(0));
        fMenuFile.add(fItemPreferences);
        fMenuFile.addSeparator();
        fMenuFile.add(fItemExit);
        fItemExit.setName("fItemExit");
        fItemExit.setText(resUtil.getString("ExitMenu"));
        fItemExit.setMnemonic(resUtil.getString("ExitMenuMnemonic").charAt(0));
        fMenuFile.addSeparator();
        fItemRecent1.setActionCommand("1");
        fItemRecent2.setActionCommand("2");
        fItemRecent3.setActionCommand("3");
        fMenuFile.add(fItemRecent1);
        fMenuFile.add(fItemRecent2);
        fMenuFile.add(fItemRecent3);
        updateRecentMenuItems(null, null);
        fMenuBar.add(fMenuFile);
    }

    private void addMenuReports() {
        fMenuReports.setText(res.getString("MenuReports"));
        fMenuReports.setMnemonic(res.getString("MenuReportsMnemonic").charAt(0));
        fMenuBar.add(fMenuReports);
        HelpManager.getInstance().registerHelpTopic(fMenuReports, "main.fMenuReports");
        fItemShowReports.setText(res.getString("MenuShowReports"));
        fItemShowReports.setMnemonic(res.getString("MenuShowReportsMnemonic").charAt(0));
        fMenuReports.add(fItemShowReports);
        fItemReportOptions.setText(res.getString("MenuEditReportOptions") + "...");
        fItemReportOptions.setMnemonic(res.getString("MenuEditReportOptionsMnemonic").charAt(0));
        fMenuReports.add(fItemReportOptions);
    }

    private void addMenuHelp() {
        fMenuHelp.setText(resUtil.getString("HelpMenu"));
        fMenuHelp.setMnemonic(resUtil.getString("HelpMnemonic").charAt(0));
        HelpManager.getInstance().registerHelpTopic(fMenuHelp, "main.fMenuHelp");
        fItemHelp.setText(res.getString("MenuHelpJavaScore"));
        fItemHelp.setMnemonic(res.getString("MenuHelpJavaScoreMnemonic").charAt(0));
        fMenuHelp.add(fItemHelp);
        JMenuItem mm = HelpManager.getInstance().createCSHMenuItem();
        mm.setText(res.getString("MenuWhatsThis"));
        mm.setMnemonic(res.getString("MenuWhatsThisMnemonic").charAt(0));
        fMenuHelp.add(mm);
        fMenuHelp.addSeparator();
        fMenuHelp.add(fActionAbout);
        fMenuBar.add(fMenuHelp);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == fButtonRegatta) fButtonRegatta_actionPerformed(); else if (event.getSource() == fButtonEntries) fButtonEntries_actionPerformed(); else if (event.getSource() == fButtonRaces) fButtonRaces_actionPerformed(); else if (event.getSource() == fItemNew) fItemNew_actionPerformed(); else if (event.getSource() == fItemSave) fItemSave_actionPerformed(event); else if (event.getSource() == fItemSaveAs) fItemSaveAs_actionPerformed(event); else if (event.getSource() == fItemOpen) fItemOpen_actionPerformed(); else if (event.getSource() == fItemRestore) fItemRestore_actionPerformed(); else if (event.getSource() == fItemHelp) fItemHelp_actionPerformed(); else if (event.getSource() == fItemExit) fItemExit_actionPerformed(); else if (event.getSource() == fItemShowReports) fItemShowReports_actionPerformed(); else if (event.getSource() == fItemReportOptions) fItemReportOptions_actionPerformed(); else if (event.getSource() == fItemPreferences) fItemPreferences_actionPerformed(); else if (event.getSource() == fItemRecent1) fItemRecent_actionPerformed(event); else if (event.getSource() == fItemRecent2) fItemRecent_actionPerformed(event); else if (event.getSource() == fItemRecent3) fItemRecent_actionPerformed(event);
        updateEnabled();
    }

    public void start() {
        fButtonRegatta.addActionListener(this);
        fButtonEntries.addActionListener(this);
        fButtonRaces.addActionListener(this);
        fItemNew.addActionListener(this);
        fItemSave.addActionListener(this);
        fItemSaveAs.addActionListener(this);
        fItemOpen.addActionListener(this);
        fItemRestore.addActionListener(this);
        fItemHelp.addActionListener(this);
        fItemExit.addActionListener(this);
        fItemRecent1.addActionListener(this);
        fItemRecent2.addActionListener(this);
        fItemRecent3.addActionListener(this);
        fItemShowReports.addActionListener(this);
        fItemReportOptions.addActionListener(this);
        fItemPreferences.addActionListener(this);
        if (sDialogEntries != null && sDialogEntries.isVisible()) sDialogEntries.start();
        if (sDialogRaces != null && sDialogRaces.isVisible()) sDialogRaces.start();
        if (sDialogRegatta != null && sDialogRegatta.isVisible()) sDialogRegatta.startUp();
        if (sDialogPreferences != null && sDialogPreferences.isVisible()) sDialogPreferences.startUp();
    }

    public void stop() {
        fButtonRegatta.removeActionListener(this);
        fButtonEntries.removeActionListener(this);
        fButtonRaces.removeActionListener(this);
        fItemNew.removeActionListener(this);
        fItemSave.removeActionListener(this);
        fItemSaveAs.removeActionListener(this);
        fItemOpen.removeActionListener(this);
        fItemRestore.removeActionListener(this);
        fItemHelp.removeActionListener(this);
        fItemExit.removeActionListener(this);
        fItemRecent1.removeActionListener(this);
        fItemRecent2.removeActionListener(this);
        fItemRecent3.removeActionListener(this);
        fItemShowReports.removeActionListener(this);
        fItemReportOptions.removeActionListener(this);
        fItemPreferences.removeActionListener(this);
        if (sDialogEntries != null && sDialogEntries.isVisible()) sDialogEntries.stop();
        if (sDialogRaces != null && sDialogRaces.isVisible()) sDialogRaces.stop();
        if (sDialogRegatta != null && sDialogRegatta.isVisible()) sDialogRegatta.shutDown();
        if (sDialogPreferences != null && sDialogPreferences.isVisible()) sDialogPreferences.shutDown();
    }

    public void setRegatta(Regatta newRegatta) {
        Regatta currentRegatta = JavaScoreProperties.getRegatta();
        if (currentRegatta == newRegatta) return;
        if (currentRegatta != null) currentRegatta.removePropertyChangeListener(getInstance());
        if (sDialogEntries != null && sDialogEntries.isVisible()) sDialogEntries.setVisible(false);
        if (sDialogRaces != null && sDialogRaces.isVisible()) sDialogRaces.setVisible(false);
        if (sDialogRegatta != null && sDialogRegatta.isVisible()) sDialogRegatta.setVisible(false);
        JavaScoreProperties.setRegatta(newRegatta);
        fRegatta = newRegatta;
        if (newRegatta != null && sInstance != null) {
            newRegatta.addPropertyChangeListener(getInstance());
            getInstance().updateTitle();
        }
    }

    public void windowClosing(WindowEvent event) {
        if (event.getSource() == this) fItemExit_actionPerformed();
    }

    public void windowActivated(WindowEvent event) {
    }

    public void windowDeactivated(WindowEvent event) {
    }

    public void windowDeiconified(WindowEvent event) {
    }

    public void windowIconified(WindowEvent event) {
    }

    public void windowOpened(WindowEvent event) {
    }

    public void windowClosed(WindowEvent event) {
    }

    public void propertyChange(PropertyChangeEvent ev) {
        if (ev.getSource() == fRegatta) {
            if (ev.getPropertyName().equals(Regatta.NAME_PROPERTY) && sDialogRegatta != null) {
                sDialogRegatta.setTitle(res.getString("MainButtonRegattaTitle") + ": " + ((String) ev.getNewValue()));
            }
        }
    }

    ReportViewer fReportViewer = null;

    public static void backgroundSave() {
        if (sInstance == null || JavaScoreProperties.getRegatta() == null) return;
        final Regatta regatta = (Regatta) JavaScoreProperties.getRegatta().clone();
        if ((regatta.getSaveName() == null) || regatta.getSaveName().equals(Regatta.NONAME)) {
            getInstance().fItemSaveAs_actionPerformed(null);
        } else {
            final Throwable tempStack = new Throwable();
            SwingWorker bkgd = new SwingWorker() {

                Regatta lRegatta = regatta;

                Throwable inStack = tempStack;

                public Object construct() {
                    try {
                        getInstance().regattaSave(lRegatta, lRegatta.getSaveDirectory(), lRegatta.getSaveName());
                    } catch (Exception e) {
                        tempStack.printStackTrace();
                        Util.showError(e, res.getString("MainMessageBackgroundSaveFailed"), true, inStack);
                    }
                    return null;
                }
            };
            bkgd.start();
        }
    }

    public static void subWindowClosing() {
        if (sInstance != null) sInstance.updateEnabled();
    }

    private void fItemRecent_actionPerformed(ActionEvent event) {
        JMenuItem menu = (JMenuItem) event.getSource();
        String num = menu.getActionCommand();
        String dir = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.RECENTDIRBASE_PROPERTY + num);
        String regfile = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.RECENTBASE_PROPERTY + num);
        if (regfile != null && regfile.length() > 0) {
            if (dir != null && dir.length() > 0) Util.setWorkingDirectory(dir);
            openRegatta(regfile);
        }
    }

    private void updateRecentMenuItems(String d, String f) {
        if (d != null) JavaScoreProperties.getInstance().pushRegattaFile(d, f);
        String reg1 = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.RECENT1_PROPERTY);
        String reg2 = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.RECENT2_PROPERTY);
        String reg3 = JavaScoreProperties.getInstance().getProperty(JavaScoreProperties.RECENT3_PROPERTY);
        if (reg1.length() > 0) {
            fItemRecent1.setText(reg1);
            fItemRecent1.setVisible(true);
        } else {
            fItemRecent1.setVisible(false);
        }
        if (reg2.length() > 0) {
            fItemRecent2.setText(reg2);
            fItemRecent2.setVisible(true);
        } else {
            fItemRecent2.setVisible(false);
        }
        if (reg3.length() > 0) {
            fItemRecent3.setText(reg3);
            fItemRecent3.setVisible(true);
        } else {
            fItemRecent3.setVisible(false);
        }
    }

    public void updateReports(boolean b) {
        if (fReportViewer == null) {
            try {
                fReportViewer = (ReportViewer) swReport.get();
            } catch (Exception e) {
            }
        }
        if (fReportViewer.getRegatta() != fRegatta) {
            fReportViewer.setRegatta(fRegatta);
        }
        fReportViewer.updateReports(b);
    }

    private void fItemShowReports_actionPerformed() {
        if (fReportViewer == null) updateReports(false);
        fReportViewer.setRegatta(fRegatta);
        fReportViewer.setVisible(true);
    }

    private void fItemReportOptions_actionPerformed() {
        if (sDialogReportOptions == null) {
            try {
                sDialogReportOptions = (DialogReportTabs) swReportOptions.get();
            } catch (Exception e) {
            }
        }
        sDialogReportOptions.setRegatta(fRegatta);
        openSubWindow(sDialogReportOptions);
    }

    private void fItemPreferences_actionPerformed() {
        if (sDialogPreferences == null) {
            try {
                sDialogPreferences = (DialogBaseEditor) swPreferences.get();
            } catch (Exception e) {
            }
        }
        openSubWindow(sDialogPreferences);
    }

    private void fItemHelp_actionPerformed() {
        HelpManager.getInstance().setHelpTopic(JavaScoreProperties.HELP_ROOT);
    }

    private void fItemExit_actionPerformed() {
        if (fRegatta == null) System.exit(0);
        int option = JOptionPane.showConfirmDialog(null, res.getString("MainMessageConfirmExitText"), res.getString("MainTitleConfirmExit"), JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        } else {
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }
    }

    private void fItemNew_actionPerformed() {
        setRegatta(new Regatta());
        updateEnabled();
        fButtonRegatta_actionPerformed();
    }

    private void fItemRestore_actionPerformed() {
        String fileName = fRegatta.getSaveName();
        if (fileName.startsWith("/")) fileName = fileName.substring(1);
        File f = new File(Util.getWorkingDirectory() + fileName + BAK);
        long lastMod = f.lastModified();
        String msg = res.getString("MainMessageDiscardChange");
        if (Util.confirm(MessageFormat.format(msg, new Object[] { new Date(lastMod) }))) {
            try {
                System.out.println(res.getString("MainMessageRestoring"));
                Regatta reg = RegattaManager.readRegattaFromDisk(Util.getWorkingDirectory(), fileName + BAK);
                reg.setSaveName(fileName);
                setRegatta(reg);
                updateReports(true);
                System.out.println(res.getString("MainMessageRestoringFinished"));
            } catch (Exception e) {
                Util.showError(e, true);
                setRegatta(null);
            }
        }
    }

    private void fItemOpen_actionPerformed() {
        setRegatta(null);
        String startDir = Util.getWorkingDirectory();
        String fileName = RegattaManager.selectOpenRegattaDialog(res.getString("MenuOpenRegatta"), startDir);
        openRegatta(fileName);
    }

    private void openRegatta(String fileName) {
        setRegatta(null);
        if ((Util.getWorkingDirectory() != null) && (fileName != null)) {
            try {
                System.out.println(res.getString("MainMessageLoadingRegattaStart") + " " + Util.getWorkingDirectory() + fileName);
                Regatta reg = RegattaManager.readRegattaFromDisk(Util.getWorkingDirectory(), fileName);
                if (reg == null) {
                    JOptionPane.showMessageDialog(this, MessageFormat.format(res.getString("InvalidRegattaFile"), new Object[] { Util.getWorkingDirectory() + fileName }), res.getString("InvalidRegattaFileTitle"), JOptionPane.ERROR_MESSAGE);
                } else if (reg.isFinal()) {
                    JOptionPane.showMessageDialog(null, res.getString("RegattaMessageFinalOnLoad"), res.getString("RegattaTitleFinalOnLoad"), JOptionPane.WARNING_MESSAGE);
                }
                checkVersion(reg);
                setRegatta(reg);
                regattaBackup();
                System.out.println(res.getString("MainMessageFinishedLoading"));
                updateRecentMenuItems(Util.getWorkingDirectory(), fileName);
            } catch (java.io.FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, MessageFormat.format(resUtil.getString("FileNotFound"), new Object[] { Util.getWorkingDirectory() + fileName }), resUtil.getString("FileNotFoundTitle"), JOptionPane.ERROR_MESSAGE);
                setRegatta(null);
            } catch (Exception ex) {
                Util.showError(ex, true);
                setRegatta(null);
            }
        }
        updateEnabled();
        backgroundSave();
    }

    private void fItemSaveAs_actionPerformed(ActionEvent event) {
        String fileName = RegattaManager.selectSaveRegattaDialog(res.getString("MainTitleSaveAs"), fRegatta);
        if (fileName != null) {
            try {
                regattaSave(fRegatta, Util.getWorkingDirectory(), fileName);
                regattaBackup();
                updateRecentMenuItems(Util.getWorkingDirectory(), fileName);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, MessageFormat.format(res.getString("MainMessageSaveFailed"), new Object[] { fRegatta.getSaveDirectory() + fRegatta.getSaveName(), e.toString() }), res.getString("MainMessageTryAgainMessage"), JOptionPane.ERROR_MESSAGE);
                fItemSaveAs_actionPerformed(event);
            }
        }
    }

    private void fItemSave_actionPerformed(ActionEvent event) {
        if ((fRegatta.getSaveName() == null) || fRegatta.getSaveName().equals(Regatta.NONAME)) {
            fItemSaveAs_actionPerformed(event);
        } else {
            try {
                regattaSave(fRegatta, fRegatta.getSaveDirectory(), fRegatta.getSaveName());
                regattaBackup();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, MessageFormat.format(res.getString("MainMessageSaveFailed"), new Object[] { fRegatta.getSaveDirectory() + fRegatta.getSaveName(), e.toString() }), res.getString("MainMessageTryAgainMessage"), JOptionPane.ERROR_MESSAGE);
                fItemSaveAs_actionPerformed(event);
            }
        }
    }

    public void regattaSave() throws IOException {
        regattaSave(fRegatta, fRegatta.getSaveDirectory(), fRegatta.getSaveName());
    }

    /**
	 * saves the regatta and regenerates reports, in a separate thread
	 * 
	 * @param reg
	 *            the regatta to be saved
	 * @param dir
	 *            the directory name
	 * @param name
	 *            the file name of the regatta
	 * 
	 * @throws IOException
	 *             if unable to save the regatta
	 */
    private void regattaSave(Regatta reg, String dir, String name) throws IOException {
        System.out.println(MessageFormat.format(res.getString("MainMessageSavingRegatta"), new Object[] { dir + name }));
        fItemShowReports.setEnabled(false);
        reg.scoreRegatta();
        if (reg.isFinal()) {
            JOptionPane.showMessageDialog(null, res.getString("RegattaMessageFinalOnSave"), res.getString("RegattaTitleFinalOnSave"), JOptionPane.WARNING_MESSAGE);
        } else {
            new RegattaManager(reg).writeRegattaToDisk(dir, name);
            updateReports(false);
            System.out.println(res.getString("MainMessageFinishedSaving"));
        }
        fItemShowReports.setEnabled(true);
    }

    public void checkVersion(Regatta reg) {
        String readVersion = reg.getSaveVersion();
        if (readVersion == null) return;
        int comp = readVersion.compareTo(JavaScoreProperties.getVersion());
        if (comp > 0) {
            String msg = MessageFormat.format(res.getString("RegattaMessageNewerVersion"), new Object[] { readVersion, JavaScoreProperties.getVersion() });
            JOptionPane.showMessageDialog(null, msg, res.getString("RegattaTitleNewerVersion"), JOptionPane.WARNING_MESSAGE);
            return;
        } else if (comp < 0) {
            String msg = MessageFormat.format(res.getString("RegattaMessageOlderVersion"), new Object[] { readVersion, JavaScoreProperties.getVersion() });
            JOptionPane.showMessageDialog(null, msg, res.getString("RegattaTitleOlderVersion"), JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    private String BAK = ".bak";

    /**
	 * creates the regatta backup file
	 */
    private void regattaBackup() {
        SwingWorker sw = new SwingWorker() {

            Regatta lRegatta = fRegatta;

            public Object construct() {
                String fullName = lRegatta.getSaveDirectory() + lRegatta.getSaveName();
                System.out.println(MessageFormat.format(res.getString("MainMessageBackingUp"), new Object[] { fullName + BAK }));
                try {
                    FileInputStream fis = new FileInputStream(new File(fullName));
                    FileOutputStream fos = new FileOutputStream(new File(fullName + BAK));
                    int bufsize = 1024;
                    byte[] buffer = new byte[bufsize];
                    int n = 0;
                    while ((n = fis.read(buffer, 0, bufsize)) >= 0) fos.write(buffer, 0, n);
                    fos.flush();
                    fos.close();
                } catch (java.io.IOException ex) {
                    Util.showError(ex, true);
                }
                return null;
            }
        };
        sw.start();
    }

    private void fButtonRegatta_actionPerformed() {
        if (sDialogRegatta == null) {
            try {
                sDialogRegatta = (DialogBaseEditor) swRegatta.get();
            } catch (Exception e) {
            }
        }
        if (fRegatta == null) {
            fItemOpen_actionPerformed();
        }
        if (fRegatta != null) {
            sDialogRegatta.setObject(fRegatta);
            sDialogRegatta.setTitle(MessageFormat.format(res.getString("MainButtonRegattaTitle"), new Object[] { fRegatta.toString() }));
            openSubWindow(sDialogRegatta);
        } else {
            JOptionPane.showMessageDialog(this, res.getString("MainMessageNoOpenRegatta"));
        }
    }

    private void fButtonEntries_actionPerformed() {
        if (sDialogEntries == null) {
            try {
                sDialogEntries = (DialogEntryTreeEditor) swEntries.get();
            } catch (Exception e) {
            }
        }
        sDialogEntries.setRegatta(fRegatta);
        openSubWindow(sDialogEntries);
    }

    private void fButtonRaces_actionPerformed() {
        if (sDialogRaces == null) {
            try {
                sDialogRaces = (DialogRaceEditor) swRaces.get();
            } catch (Exception e) {
            }
        }
        fRegatta.getRaces().sort();
        sDialogRaces.setMasterList(fRegatta.getRaces(), MessageFormat.format(res.getString("MainButtonRacesTitleStart"), new Object[] { fRegatta.toString() }));
        openSubWindow(sDialogRaces);
    }

    private void openSubWindow(JDialog d) {
        d.setVisible(true);
        updateEnabled();
    }

    public void setVisible(boolean vis) {
        if (vis) {
            if (!isVisible()) {
                start();
            }
        } else {
            if (isVisible()) {
                stop();
            }
        }
        super.setVisible(vis);
    }

    public SwingWorker swFileChooser = new SwingWorker() {

        public Object construct() {
            JFileChooser f = null;
            try {
                f = RegattaManager.getFileChooser();
            } catch (Exception e) {
                Util.showError(e, true);
            }
            fItemOpen.setEnabled(f != null);
            return f;
        }
    };

    private SwingWorker swRegatta = new SwingWorker() {

        public Object construct() {
            try {
                DialogBaseEditor dialog = new DialogBaseEditor(JavaScore.this, false);
                dialog.setLocation(Util.getLocationToCenterOnScreen(dialog));
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swReportOptions = new SwingWorker() {

        public Object construct() {
            try {
                DialogReportTabs dialog = new DialogReportTabs(JavaScore.this, false);
                dialog.setLocation(Util.getLocationToCenterOnScreen(dialog));
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swReport = new SwingWorker() {

        public Object construct() {
            try {
                ReportViewer report = new ReportViewer(fRegatta);
                return report;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swEntries = new SwingWorker() {

        public Object construct() {
            try {
                DialogEntryTreeEditor dialog = new DialogEntryTreeEditor(JavaScore.this, false);
                dialog.setSize(1000, 480);
                HelpManager.getInstance().registerHelpTopic(dialog, "entrylist");
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swRaces = new SwingWorker() {

        public Object construct() {
            try {
                DialogRaceEditor dialog = new DialogRaceEditor(JavaScore.this, false);
                HelpManager.getInstance().registerHelpTopic(dialog, "racelist");
                dialog.setSize(640, 450);
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swPreferences = new SwingWorker() {

        public Object construct() {
            try {
                JavaScoreProperties ro = JavaScoreProperties.getInstance();
                DialogBaseEditor dialog = new DialogBaseEditor(null, "JavaScore Preferences", false);
                dialog.setObject(ro);
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swDivisions = new SwingWorker() {

        public Object construct() {
            try {
                DialogDivisionListEditor dialog = new DialogDivisionListEditor(null, false);
                dialog.setSize(580, 400);
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swSubdivisions = new SwingWorker() {

        public Object construct() {
            try {
                DialogDivisionListEditor dialog = new DialogDivisionListEditor(null, false);
                dialog.setSize(580, 400);
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    private SwingWorker swFleets = new SwingWorker() {

        public Object construct() {
            try {
                DialogDivisionListEditor dialog = new DialogDivisionListEditor(null, false);
                dialog.setSize(580, 400);
                return dialog;
            } catch (Exception e) {
                Util.showError(e, true);
            }
            return null;
        }
    };

    public DialogDivisionListEditor getDivisionListEditor(String nickname) {
        try {
            if (nickname.equals("subdivisions")) return (DialogDivisionListEditor) swSubdivisions.get(); else if (nickname.equals("divisionlist")) return (DialogDivisionListEditor) swDivisions.get(); else if (nickname.equals("fleets")) return (DialogDivisionListEditor) swFleets.get(); else return null;
        } catch (Exception e) {
            Util.showError(e, true);
            return null;
        }
    }

    public static void setLookAndFeel(String uiName) {
        LookAndFeel current = UIManager.getLookAndFeel();
        if (uiName.equals(current.getName())) return;
        LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < lafs.length; ++i) {
            if (uiName.equals(lafs[i].getName())) {
                try {
                    UIManager.setLookAndFeel(lafs[i].getClassName());
                    if (getInstance() != null) SwingUtilities.updateComponentTreeUI(getInstance());
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(null, MessageFormat.format(JavaScoreProperties.res.getString("MainMessageBadLookAndFeel"), new Object[] { uiName }), JavaScoreProperties.res.getString("MainTitleBadLookAndFeel"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
