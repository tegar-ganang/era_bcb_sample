package org.paccman.ui.main;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import org.paccman.calc.CalculatorFrame;
import org.paccman.controller.AccountController;
import org.paccman.controller.DocumentController;
import org.paccman.controller.PaccmanView;
import org.paccman.db.PaccmanDao;
import org.paccman.preferences.ui.MainPrefs;
import org.paccman.tools.FileUtils;
import org.paccman.ui.*;
import org.paccman.ui.accounts.AccountFormTab;
import org.paccman.ui.banks.BankFormTab;
import org.paccman.ui.categories.CategoryFormTab;
import org.paccman.ui.paymentmethods.PaymentMethodFormTab;
import org.paccman.ui.payees.PayeeFormTab;
import org.paccman.ui.schedules.ScheduleFormTab;
import org.paccman.ui.transactions.TransactionFormTab;
import org.paccman.ui.welcome.WelcomePaneTab;
import org.paccman.xml.PaccmanIOException;
import static org.paccman.ui.main.ContextMain.*;
import static org.paccman.ui.main.Actions.ActionResult.*;
import org.paccman.xml.PaccmanFileOld;

/**
 *
 * @author  joao
 */
public class Main extends javax.swing.JFrame implements PaccmanView {

    private boolean saveToDatabase = false;

    private boolean saveToXml = true;

    private boolean readFromXml = false;

    private void initComponents() {
        mainTabbedPane = new javax.swing.JTabbedPane();
        toolbarsPanel = new javax.swing.JPanel();
        mainToolBar = new javax.swing.JToolBar();
        newBtn = new javax.swing.JButton();
        openBtn = new javax.swing.JButton();
        saveBtn = new javax.swing.JButton();
        mainToolBar.add(new JToolBar.Separator());
        quitBtn = new javax.swing.JButton();
        navigatorToolBar = new javax.swing.JToolBar();
        backBtn = new javax.swing.JButton();
        forwardBtn = new javax.swing.JButton();
        mainMenuBar = new javax.swing.JMenuBar();
        fileMnu = new javax.swing.JMenu();
        newMnu = new javax.swing.JMenuItem();
        openMnu = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        closeMnu = new javax.swing.JMenuItem();
        saveMnu = new javax.swing.JMenuItem();
        saveAsMnu = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        propertiesMnu = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        quitMnu = new javax.swing.JMenuItem();
        optionMnu = new javax.swing.JMenu();
        showStartDialog = new javax.swing.JCheckBoxMenuItem();
        openLastFileMnu = new javax.swing.JCheckBoxMenuItem();
        toolsMnu = new javax.swing.JMenu();
        calculatorMnu = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(NO_DOCUMENT_TITLE);
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/org/paccman/ui/resources/images/euro.png")).getImage());
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }

            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        getContentPane().add(mainTabbedPane, java.awt.BorderLayout.CENTER);
        toolbarsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        newBtn.setAction(newAction);
        newBtn.setText("");
        mainToolBar.add(newBtn);
        openBtn.setAction(openAction);
        openBtn.setText("");
        mainToolBar.add(openBtn);
        saveBtn.setAction(saveAction);
        saveBtn.setText("");
        mainToolBar.add(saveBtn);
        quitBtn.setAction(quitAction);
        quitBtn.setText("");
        mainToolBar.add(quitBtn);
        toolbarsPanel.add(mainToolBar);
        navigatorToolBar.setVisible(false);
        backBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/paccman/ui/resources/images/go_back.png")));
        backBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });
        navigatorToolBar.add(backBtn);
        forwardBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/paccman/ui/resources/images/go_forward.png")));
        forwardBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardBtnActionPerformed(evt);
            }
        });
        navigatorToolBar.add(forwardBtn);
        toolbarsPanel.add(navigatorToolBar);
        getContentPane().add(toolbarsPanel, java.awt.BorderLayout.NORTH);
        fileMnu.setText("File");
        newMnu.setAction(newAction);
        fileMnu.add(newMnu);
        openMnu.setAction(new OpenAction());
        fileMnu.add(openMnu);
        fileMnu.add(jSeparator1);
        closeMnu.setAction(new CloseAction());
        fileMnu.add(closeMnu);
        saveMnu.setAction(saveAction);
        fileMnu.add(saveMnu);
        saveAsMnu.setAction(saveAsAction);
        fileMnu.add(saveAsMnu);
        fileMnu.add(jSeparator2);
        propertiesMnu.setAction(new PropertiesAction());
        fileMnu.add(propertiesMnu);
        fileMnu.add(jSeparator3);
        quitMnu.setAction(quitAction);
        fileMnu.add(quitMnu);
        mainMenuBar.add(fileMnu);
        optionMnu.setText("Options");
        showStartDialog.setSelected(MainPrefs.getShowStartDialog());
        showStartDialog.setText("Show start dialog");
        showStartDialog.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showStartDialogActionPerformed(evt);
            }
        });
        optionMnu.add(showStartDialog);
        openLastFileMnu.setSelected(MainPrefs.getOpenLastSelectedFile());
        openLastFileMnu.setText("Open last file");
        openLastFileMnu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLastFileMnuActionPerformed(evt);
            }
        });
        optionMnu.add(openLastFileMnu);
        mainMenuBar.add(optionMnu);
        toolsMnu.setText("Tools");
        calculatorMnu.setText("Calculator");
        calculatorMnu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calculatorMnuActionPerformed(evt);
            }
        });
        toolsMnu.add(calculatorMnu);
        mainMenuBar.add(toolsMnu);
        setJMenuBar(mainMenuBar);
        pack();
    }

    @Deprecated
    public TransactionFormTab getTransactionFormTab() {
        return transactionFormTab;
    }

    CalculatorFrame calculatorFrame = new CalculatorFrame();

    private void calculatorMnuActionPerformed(java.awt.event.ActionEvent evt) {
        calculatorFrame.setVisible(true);
    }

    private void forwardBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, ":TODO: NOT IMPLEMENTED YET", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, ":TODO: NOT IMPLEMENTED YET", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void openLastFileMnuActionPerformed(java.awt.event.ActionEvent evt) {
        MainPrefs.setOpenLastSelectedFile(!MainPrefs.getOpenLastSelectedFile());
    }

    @Deprecated
    private int confirmSave() {
        assert (getDocumentController() != null) && getDocumentController().isHasChanged();
        return JOptionPane.showConfirmDialog(this, "Do you want to save the changes ?", "Confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    @Deprecated
    private File selectSaveFile() {
        assert (getDocumentController() != null);
        PaccmanFileChooser pfc = new PaccmanFileChooser();
        if (pfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            return pfc.getSelectedFile();
        } else {
            return null;
        }
    }

    @Deprecated
    private File selectOpenFile() {
        JFileChooser fc = new PaccmanFileChooser();
        int s = fc.showOpenDialog(this);
        if (s == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    @Deprecated
    public Actions.ActionResult closeDocument() {
        assert isDocumentEdited() : "Can not close if no document is loaded";
        if (getDocumentController().isHasChanged()) {
            int save = confirmSave();
            if (save == JOptionPane.CANCEL_OPTION) {
                return CANCEL;
            } else if (save == JOptionPane.YES_OPTION) {
                Actions.ActionResult saveDiag;
                if (getDocumentController().getFile() == null) {
                    saveDiag = saveAsDocument();
                } else {
                    saveDiag = saveDocument();
                }
                if (saveDiag != OK) {
                    return saveDiag;
                }
            }
        }
        mainTabbedPane.removeAll();
        setDocumentController(null);
        documentControllerUpdated();
        return OK;
    }

    @Deprecated
    class ReadWorker extends SwingWorker<Object, Object> {

        File file;

        Main main;

        public ReadWorker(File file, Main main) {
            this.file = file;
            this.main = main;
        }

        @Override
        protected Object doInBackground() throws Exception {
            DocumentController newDocumentController = new DocumentController();
            if (readFromXml) {
                PaccmanFileOld paccmanFile = new PaccmanFileOld();
                logger.fine("Opening file " + file.getAbsolutePath());
                paccmanFile.read(file, newDocumentController);
                logger.fine("File opened");
                File fileOut = new File(file.getAbsolutePath() + new SimpleDateFormat("-yyMMddHHmmss").format(new Date()));
                try {
                    FileUtils.copyFile(file, fileOut);
                    logger.fine("Copied file to " + fileOut.getAbsolutePath());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } catch (IOException ex) {
                    throw new PaccmanIOException("Failed to make a save copy of the file");
                }
            } else {
                PaccmanDao db = new PaccmanDao(new File(file.getAbsolutePath()).getPath() + "db");
                try {
                    db.load(newDocumentController);
                } catch (SQLException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            newDocumentController.registerView(main);
            newDocumentController.notifyChange();
            setDocumentController(newDocumentController);
            getDocumentController().setFile(file);
            String path = file.getParent();
            MainPrefs.putDataDirectory(path);
            try {
                MainPrefs.addFilenameToMru(file.getCanonicalPath());
                MainPrefs.setLastSelectedFile(file.getCanonicalPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void process(List<Object> chunks) {
        }

        @Override
        protected void done() {
            super.done();
            documentControllerUpdated();
            getDocumentController().notifyChange();
        }
    }

    @Deprecated
    public void openFile(File file) throws PaccmanIOException {
        ReadWorker rw = new ReadWorker(file, this);
        rw.execute();
    }

    @Deprecated
    private Actions.ActionResult doOpenFile(File filein) {
        try {
            openFile(filein);
            return OK;
        } catch (PaccmanIOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to open file '" + filein.getAbsolutePath() + "' (" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
            return FAILED;
        }
    }

    @Deprecated
    public Actions.ActionResult openDocument(File file) {
        if (isDocumentEdited()) {
            Actions.ActionResult closeDiag = closeDocument();
            if (closeDiag != OK) {
                return closeDiag;
            }
        }
        Actions.ActionResult result = FAILED;
        File fileToOpen = file == null ? selectOpenFile() : file;
        while (result == FAILED) {
            if (fileToOpen == null) {
                return CANCEL;
            } else {
                result = doOpenFile(fileToOpen);
                if (result == FAILED) {
                    fileToOpen = selectOpenFile();
                }
            }
        }
        return result;
    }

    @Deprecated
    public Actions.ActionResult newDocument() {
        if (isDocumentEdited()) {
            Actions.ActionResult closeDiag = closeDocument();
            if (closeDiag != OK) {
                return closeDiag;
            }
        }
        String title = (String) JOptionPane.showInputDialog(this, "Enter the title for the new account file", "Document title", JOptionPane.QUESTION_MESSAGE, null, null, "NewDocument");
        if (title != null) {
            setDocumentController(new DocumentController());
            getDocumentController().getDocument().setTitle(title);
            getDocumentController().setHasChanged(true);
            documentControllerUpdated();
            return OK;
        } else {
            return CANCEL;
        }
    }

    /**
     * Saves the current document to the specified location.
     * @param saveFile The location where the document is saved.
     * @return {@link OK} or {@link FAILED}.
     * @deprecated 
     */
    @Deprecated
    public Actions.ActionResult saveDocument(File saveFile) {
        if (saveToXml) {
            PaccmanFileOld pf = new PaccmanFileOld();
            try {
                pf.write(saveFile, getDocumentController());
            } catch (PaccmanIOException pie) {
                pie.printStackTrace();
                return FAILED;
            }
        }
        if (saveToDatabase) {
            PaccmanDao db = new PaccmanDao(new File(saveFile.getAbsolutePath()).getPath() + "db");
            try {
                db.save(getDocumentController());
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            } catch (URISyntaxException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return OK;
    }

    @Deprecated
    public Actions.ActionResult saveDocument() {
        assert isDocumentEdited() : "'save' should not be called when no document loaded";
        assert getDocumentController().getFile() != null : "'save' should be called when the document has a file";
        if (saveDocument(getDocumentController().getFile()) == OK) {
            getDocumentController().setFile(getDocumentController().getFile());
            setDocumentChanged(false);
            return OK;
        } else {
            return FAILED;
        }
    }

    @Deprecated
    public Actions.ActionResult saveAsDocument() {
        assert isDocumentEdited() : "'saveAs' should not be called when no document loaded";
        File saveFile = selectSaveFile();
        if (saveFile == null) {
            return CANCEL;
        }
        if (saveDocument(saveFile) == OK) {
            getDocumentController().setFile(saveFile);
            setDocumentChanged(false);
            return OK;
        } else {
            return FAILED;
        }
    }

    @Deprecated
    public Actions.ActionResult quit() {
        if (isDocumentEdited()) {
            Actions.ActionResult closeDiag = closeDocument();
            if (closeDiag != OK) {
                return closeDiag;
            }
        }
        if ((getState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            MainPrefs.setMaximized(true);
        } else {
            MainPrefs.setMaximized(false);
            MainPrefs.putLocation(getLocationOnScreen());
            MainPrefs.putSize(getSize());
        }
        System.exit(0);
        return OK;
    }

    @Deprecated
    public abstract class PaccmanAction extends AbstractAction {

        public PaccmanAction(String name, Icon icon, boolean enabled) {
            super(name, icon);
            setEnabled(enabled);
        }

        public PaccmanAction(String name, Icon icon) {
            super(name, icon);
        }
    }

    @Deprecated
    public final class OpenAction extends PaccmanAction {

        public OpenAction() {
            super("Open", new javax.swing.ImageIcon(OpenAction.class.getResource("/org/paccman/ui/resources/images/open.png")));
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            openDocument(null);
        }
    }

    @Deprecated
    public final class CloseAction extends PaccmanAction {

        public CloseAction() {
            super("Close", new javax.swing.ImageIcon(SaveAction.class.getResource("/org/paccman/ui/resources/images/close.png")), false);
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            closeDocument();
        }
    }

    @Deprecated
    public final class SaveAction extends PaccmanAction {

        public SaveAction() {
            super("Save", new javax.swing.ImageIcon(SaveAction.class.getResource("/org/paccman/ui/resources/images/save.png")), false);
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (getDocumentController().getFile() != null) {
                saveDocument();
            } else {
                saveAsDocument();
            }
        }
    }

    @Deprecated
    public final class SaveAsAction extends PaccmanAction {

        public SaveAsAction() {
            super("Save as...", new javax.swing.ImageIcon(SaveAction.class.getResource("/org/paccman/ui/resources/images/save_as.png")), false);
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            saveAsDocument();
        }
    }

    @Deprecated
    public final class QuitAction extends PaccmanAction {

        public QuitAction() {
            super("Quit", new javax.swing.ImageIcon(SaveAction.class.getResource("/org/paccman/ui/resources/images/exit.png")));
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            quit();
        }
    }

    @Deprecated
    public final class PropertiesAction extends PaccmanAction {

        public PropertiesAction() {
            super("Properties", new javax.swing.ImageIcon(SaveAction.class.getResource("/org/paccman/ui/resources/images/properties.png")), false);
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            PropertiesFrame pf = new PropertiesFrame();
            getDocumentController().registerView(pf);
            pf.onChange(getDocumentController());
            pf.setVisible(true);
        }
    }

    @Deprecated
    private void showStartDialogActionPerformed(java.awt.event.ActionEvent evt) {
        MainPrefs.setShowStartDialog(!MainPrefs.getShowStartDialog());
    }

    @Deprecated
    public void gotoAccountTransactionTab(AccountController account) {
        transactionFormTab.setSelectedAccount(account);
        mainTabbedPane.setSelectedComponent(transactionFormTab);
    }

    @Deprecated
    public void gotoWelcomeTab() {
        mainTabbedPane.setSelectedComponent(welcomePane);
    }

    @Deprecated
    private int showQuitDialog() {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to quit ?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            return JOptionPane.YES_OPTION;
        } else {
            return JOptionPane.NO_OPTION;
        }
    }

    public javax.swing.JButton backBtn;

    public javax.swing.JMenuItem calculatorMnu;

    public javax.swing.JMenuItem closeMnu;

    public javax.swing.JMenu fileMnu;

    public javax.swing.JButton forwardBtn;

    public javax.swing.JSeparator jSeparator1;

    public javax.swing.JSeparator jSeparator2;

    public javax.swing.JSeparator jSeparator3;

    public javax.swing.JMenuBar mainMenuBar;

    public javax.swing.JTabbedPane mainTabbedPane;

    public javax.swing.JToolBar mainToolBar;

    public javax.swing.JToolBar navigatorToolBar;

    public javax.swing.JButton newBtn;

    public javax.swing.JMenuItem newMnu;

    public javax.swing.JButton openBtn;

    public javax.swing.JCheckBoxMenuItem openLastFileMnu;

    public javax.swing.JMenuItem openMnu;

    public javax.swing.JMenu optionMnu;

    public javax.swing.JMenuItem propertiesMnu;

    public javax.swing.JButton quitBtn;

    public javax.swing.JMenuItem quitMnu;

    public javax.swing.JMenuItem saveAsMnu;

    public javax.swing.JButton saveBtn;

    public javax.swing.JMenuItem saveMnu;

    public javax.swing.JCheckBoxMenuItem showStartDialog;

    public javax.swing.JPanel toolbarsPanel;

    public javax.swing.JMenu toolsMnu;

    /**
     * Called when the DocumentController changes
     */
    @Deprecated
    private void documentControllerUpdated() {
        if (getDocumentController() == null) {
            mainTabbedPane.removeAll();
            setTitle(getTitleString());
        } else {
            showTabbedPanes();
            getDocumentController().registerView(this);
            getDocumentController().notifyChange();
        }
    }

    @Deprecated
    public static void setDocumentChanged(boolean changed) {
        getDocumentController().setHasChanged(changed);
        main.onChange(getDocumentController());
    }

    WelcomePaneTab welcomePane;

    AccountFormTab accountsPanel;

    BankFormTab bankFormTab;

    CategoryFormTab categoryFormTab;

    TransactionFormTab transactionFormTab;

    PayeeFormTab payeeFormTab;

    PaymentMethodFormTab paymentMethodFormTab;

    ScheduleFormTab scheduleFormTab;

    void showTabbedPanes() {
        assert getDocumentController() != null : "The document controller must exists";
        welcomePane = new WelcomePaneTab();
        mainTabbedPane.addTab("Welcome", welcomePane);
        welcomePane.registerToDocumentCtrl();
        transactionFormTab = new TransactionFormTab();
        mainTabbedPane.addTab("Transactions", transactionFormTab);
        transactionFormTab.registerToDocumentCtrl();
        accountsPanel = new AccountFormTab();
        mainTabbedPane.addTab("Accounts", accountsPanel);
        accountsPanel.registerToDocumentCtrl();
        bankFormTab = new BankFormTab();
        mainTabbedPane.addTab("Banks", bankFormTab);
        bankFormTab.registerToDocumentCtrl();
        categoryFormTab = new CategoryFormTab();
        mainTabbedPane.addTab("Categories", categoryFormTab);
        categoryFormTab.registerToDocumentCtrl();
        payeeFormTab = new PayeeFormTab();
        mainTabbedPane.addTab("Payees", payeeFormTab);
        payeeFormTab.registerToDocumentCtrl();
        paymentMethodFormTab = new PaymentMethodFormTab();
        mainTabbedPane.addTab("Payment Method", paymentMethodFormTab);
        paymentMethodFormTab.registerToDocumentCtrl();
        scheduleFormTab = new ScheduleFormTab();
        mainTabbedPane.addTab("Schedules", scheduleFormTab);
        scheduleFormTab.registerToDocumentCtrl();
    }

    void hideTabbedPanes() {
        mainTabbedPane.removeAll();
    }

    Actions.NewAction newAction = new Actions.NewAction();

    Actions.OpenAction openAction = new Actions.OpenAction();

    Actions.CloseAction closeAction = new Actions.CloseAction();

    Actions.SaveAction saveAction = new Actions.SaveAction();

    Actions.SaveAsAction saveAsAction = new Actions.SaveAsAction();

    Actions.QuitAction quitAction = new Actions.QuitAction();

    public void onChange(org.paccman.controller.Controller controller) {
        closeMnu.getAction().setEnabled(isDocumentEdited());
        saveMnu.getAction().setEnabled(isDocumentEdited() && (getDocumentController().isHasChanged()));
        saveAsMnu.getAction().setEnabled(isDocumentEdited());
        propertiesMnu.getAction().setEnabled(isDocumentEdited());
        setTitle(getTitleString());
    }

    private static final String NO_DOCUMENT_TITLE = "[No document]";

    private String getTitleString() {
        if (getDocumentController() == null) {
            return NO_DOCUMENT_TITLE;
        } else {
            if (getDocumentController().isHasChanged()) {
                return getDocumentController().getDocument().getTitle() + "*";
            } else {
                return getDocumentController().getDocument().getTitle();
            }
        }
    }

    Logger logger = org.paccman.tools.Logger.getDefaultLogger(Main.class);

    private void initMyComponents() {
        optionMnu.addSeparator();
        optionMnu.add(new LookAndFeelMenu(this));
        calculatorFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    StartOption startOption = new StartOption();

    /**
     * Constructor for <code>Main<code> 
     */
    public Main() {
        initComponents();
        initMyComponents();
    }

    /**
     * Constructor for <code>Main<code> which takes the arguments passed in the command 
     * line.
     * @param args The arguments passed in the command line.
     */
    public Main(String[] args) {
        this();
        startOption.parse(args);
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
    }

    private void formWindowOpened(java.awt.event.WindowEvent evt) {
        String fileToOpen = null;
        if (startOption.getFilename() != null) {
            fileToOpen = startOption.getFilename();
        }
        if (fileToOpen != null) {
            Actions.ActionResult res = Actions.doOpenFile(new File(fileToOpen));
            switch(res) {
                case OK:
                    break;
                case FAILED:
                    JOptionPane.showMessageDialog(Main.getMain(), "Failed to open file", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        }
    }

    /**
     * <em>The</em> Main
     * @return Main.
     */
    public static Main getMain() {
        return main;
    }

    static Main main;

    /**
     * Main entry point for PAccMan.
     * @param args Options and arguments for PAccMan. See StartOption#parse(args).
     */
    public static void main(String[] args) {
        final String[] lArgs = args;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                main = new Main(lArgs);
                main.setLocation(MainPrefs.getLocation());
                if (MainPrefs.isMaximized()) {
                    main.setState(MAXIMIZED_BOTH);
                } else {
                    main.setSize(MainPrefs.getSize());
                }
                main.setVisible(true);
            }
        });
    }
}
