package eu.popeye.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.security.KeyPair;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import eu.popeye.application.context.ProfileParser;
import eu.popeye.middleware.dataSharing.DataSharingFactory;
import eu.popeye.middleware.dataSharing.DataSharingManager;
import eu.popeye.middleware.dataSharing.SharedSpace;
import eu.popeye.middleware.groupmanagement.management.Workgroup;
import eu.popeye.middleware.usermanagement.UserManagement;
import eu.popeye.middleware.usermanagement.exception.BaseProfileAuthentificationFailedException;
import eu.popeye.middleware.usermanagement.exception.BaseProfileNotInitializedException;
import eu.popeye.middleware.workspacemanagement.Workspace;
import eu.popeye.middleware.workspacemanagement.WorkspaceManagement;
import eu.popeye.middleware.workspacemanagement.WorkspaceManagementImpl;
import eu.popeye.middleware.workspacemanagement.exception.WorkspaceAlreadyJoinedException;
import eu.popeye.middleware.workspacemanagement.exception.WorkspaceException;
import eu.popeye.middleware.workspacemanagement.exception.WorkspaceNotExistException;
import eu.popeye.networkabstraction.communication.ApplicationMessageListener;
import eu.popeye.networkabstraction.communication.CommunicationChannel;
import eu.popeye.networkabstraction.communication.basic.BSMProvider;
import eu.popeye.networkabstraction.communication.basic.util.InitializationException;
import eu.popeye.networkabstraction.communication.basic.util.WorkgroupAlreadyCreatedException;
import eu.popeye.networkabstraction.communication.basic.util.WorkgroupNotExistException;
import eu.popeye.networkabstraction.communication.message.PopeyeMessage;
import eu.popeye.provisionalContext.ProvisionalContext;
import eu.popeye.security.SecurityMsgServer;
import eu.popeye.security.accesscontrol.captiveportal.CaptivePortalDialog;
import eu.popeye.security.management.ToolBox;
import eu.popeye.ui.MainViewFrame;
import eu.popeye.ui.Mainbar;
import eu.popeye.ui.ModifyProfileDlg;
import eu.popeye.ui.SplashScreen;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import eu.popeye.context.management.ContextManagementServices;

/**
 * The default implementation of IApplicationFramework. This is the central
 * logic node of the Popeye application. All plugins and services should
 * communicate through an instance of this class.
 *
 * @author Paolo Gianrossi <paolo.gianrossi@softeco.it>
 * @see IApplicationFramework
 */
public class ApplicationFramework {

    private Mainbar mainbar;

    private MainViewFrame mainframe;

    private WorkspaceManagement wsManagement = null;

    private UserManagement usrManagement = null;

    private boolean debugMode = false;

    private boolean sandraTest = false;

    private ProvisionalContext pctx = null;

    private static ApplicationFramework instance = null;

    private static FileLock lck;

    public static SplashScreen splash;

    public ProvisionalContext getPctx() {
        return pctx;
    }

    private void initializeUIComponents() {
        splash.setProgress(35, "Creating Mainbar");
        this.setMainbar(new Mainbar());
        splash.setProgress(70, "Creating Mainport");
        this.setMainframe(new MainViewFrame());
        splash.setProgress(80, "Initializing UI");
        getMainframe().setLocation(getMainbar().getX(), getMainbar().getY() + getMainbar().getHeight() + 26);
        getMainframe().setSize(this.getMainbar().getWidth(), this.getMainframe().getHeight());
    }

    public void editUserPrefs() {
        new ModifyProfileDlg(this.getMainbar()).setVisible(true);
    }

    public Mainbar getMainbar() {
        return this.mainbar;
    }

    public void setMainbar(Mainbar mainbar) {
        this.mainbar = mainbar;
    }

    public MainViewFrame getMainframe() {
        return mainframe;
    }

    public void setMainframe(MainViewFrame mainframe) {
        this.mainframe = mainframe;
    }

    private synchronized void initializeFramework() {
        if (PropertiesLoader.isCPEnabled()) {
            KeyPair kp = ToolBox.generateKeyPair();
            CaptivePortalDialog dlg = new CaptivePortalDialog(null, true, "Captive Portal");
            dlg.setVisible(true);
            if (dlg.answer) {
                eu.popeye.security.accesscontrol.captiveportal.CaptivePortal.runCaptivePortal(dlg.username_, dlg.password_, dlg.email_, dlg.fullname_, "popai", true, kp);
            }
        }
        splash.setProgress(12, "Loading properties");
        File ldd = new File(PropertiesLoader.getLocalDataPath());
        if (ldd.exists() && !ldd.isDirectory()) {
            System.err.println("Cannot create directory!\nContinue at own risk ;)");
        }
        if (!ldd.exists()) {
            if (ldd.mkdirs()) {
                System.err.println("Local Data Path Created");
            } else {
                System.err.println("Local Data Path Creation Failed");
            }
        }
        splash.setProgress(16, "Initializing System");
        SystemInitialization.initialize();
        splash.setProgress(22, "Initializing User Management");
        usrManagement = UserManagement.getInstance();
        splash.setProgress(35, "Starting Context Management System");
        try {
            SecurityMsgServer.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Could not start security msg server");
        }
    }

    public void initializeWorkspaceAndContext() {
        wsManagement = WorkspaceManagementImpl.getInstance();
        try {
            usrManagement.initContext(BSMProvider.getInstance().getBasicServicesManager());
        } catch (InitializationException e1) {
            e1.printStackTrace();
        }
        Workgroup contextGroup = null;
        try {
            contextGroup = BSMProvider.getInstance().getBasicServicesManager().getWorkgroupManager().createWorkgroup("ProvisionalContext", null);
        } catch (WorkgroupAlreadyCreatedException ex) {
            try {
                contextGroup = BSMProvider.getInstance().getBasicServicesManager().getWorkgroupManager().joinWorkgroup("ProvisionalContext", null);
            } catch (WorkgroupNotExistException ex2) {
                ex2.printStackTrace();
            } catch (InitializationException ex2) {
                ex2.printStackTrace();
            }
        } catch (InitializationException ex) {
            ex.printStackTrace();
        }
        contextGroup.createNamedCommunicationChannel("ProvisionalContext");
        CommunicationChannel cc = contextGroup.getNamedCommunicationChannel("ProvisionalContext");
        cc.addApplicationMessageListener(new ApplicationMessageListener() {

            public void onMessage(PopeyeMessage msg) {
                System.out.println("New for context" + msg);
            }
        });
        String homeDirectory = PropertiesLoader.getLocalDataPath() + "tmp/";
        DataSharingManager dataSharingManager;
        try {
            dataSharingManager = DataSharingFactory.getDataSharingManager("__Ctx", homeDirectory, BSMProvider.getInstance().getBasicServicesManager());
            String sharedSpaceName = "ProvisionalContext";
            int maxSpace = 100;
            SharedSpace sharedSpace = dataSharingManager.joinSharedSpace(sharedSpaceName, maxSpace, contextGroup);
            pctx = new ProvisionalContext(sharedSpace);
        } catch (InitializationException e) {
            e.printStackTrace();
        }
    }

    public synchronized void logoutFromWs(String workspaceName) {
        WorkspaceManagementImpl.getInstance().leaveWorkspace(workspaceName);
    }

    /** Creates a new instance of ApplicationFramework */
    private ApplicationFramework() {
        String[] argv = null;
        if (argv != null) {
            int c;
            String initFile = "";
            LongOpt[] longOpts = new LongOpt[3];
            StringBuffer sb = new StringBuffer();
            longOpts[0] = new LongOpt("init-file", LongOpt.REQUIRED_ARGUMENT, null, 2);
            longOpts[1] = new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 3);
            longOpts[2] = new LongOpt("sandra-test", LongOpt.NO_ARGUMENT, null, 4);
            Getopt g = new Getopt("Popeye", argv, "-:", longOpts);
            while ((c = g.getopt()) != -1) {
                switch(c) {
                    case 2:
                        initFile = g.getOptarg();
                        break;
                    case 3:
                        this.debugMode = (g.getOptarg() == null || Boolean.parseBoolean(g.getOptarg()));
                        break;
                    case 4:
                        this.sandraTest = true;
                        break;
                }
            }
        }
        splash.setProgress(5, "Initializing Framework...");
        initializeFramework();
        splash.setProgress(30, "Initializing Application...");
        initializeUIComponents();
        splash.setProgress(100, null);
        splash.setScreenVisible(false);
    }

    public static ApplicationFramework getInstance() {
        if (instance == null) {
            instance = new ApplicationFramework();
        }
        return instance;
    }

    public void login(String username, String password) throws IOException, BaseProfileAuthentificationFailedException {
        File dir = new File(PropertiesLoader.getLocalDataPath());
        String[] files = dir.list();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String fl = files[i];
                if (!fl.endsWith("_BaseProfile.lol")) {
                    continue;
                }
                String[] matches = fl.split("_");
                String match = matches[0];
                if (match.equals(username)) {
                    try {
                        UserManagement.getInstance().deserializeBaseProfile(match, password);
                    } catch (ClassNotFoundException ex) {
                        break;
                    }
                    this.getMainbar().getLoginStatus().setText("Welcome, " + match);
                    ProfileParser pr = new ProfileParser();
                    try {
                        pr.setProfile(UserManagement.getInstance().getBaseProfile().getData());
                    } catch (BaseProfileNotInitializedException ex) {
                        ex.printStackTrace();
                    }
                    pr.parseProfile();
                    ApplicationFramework.getInstance().initializeWorkspaceAndContext();
                    try {
                        ApplicationFramework.getInstance().getPctx().shareProfile(UserManagement.getInstance().getUserName(), pr.getSource());
                    } catch (BaseProfileNotInitializedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void joinWsAfterMaybeCreate(String workspaceName) {
        Workspace ws;
        if (WorkspaceManagementImpl.getInstance().listWorkspaces().contains(workspaceName)) {
            try {
                ws = WorkspaceManagementImpl.getInstance().joinWorkspace(workspaceName);
                getMainframe().openJoinedWsInMainFrame(ws);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainbar, "Couldn't join Workspace.\nTry later.", "Workspace Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                ws = WorkspaceManagementImpl.getInstance().createWorkspace(workspaceName);
                getMainframe().openJoinedWsInMainFrame(ws);
            } catch (WorkspaceException e) {
                JOptionPane.showMessageDialog(mainbar, "Couldn't create Workspace.\nTry later.", "Workspace Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void quit() {
        this.logout();
        System.exit(0);
    }

    public void MainbarVisible(boolean v) {
        this.getMainbar().setVisible(v);
    }

    /**
     * Logout current user from the Popeye system.
     * @return true if successful, false otherwise
     */
    public synchronized void logout() {
        try {
            UserManagement.getInstance().getBaseProfile();
        } catch (BaseProfileNotInitializedException ex) {
            System.err.println("No need to logout...");
            return;
        }
        Object[] o = WorkspaceManagementImpl.getInstance().getJoinedWorkspaces().toArray();
        for (int i = 0; i < o.length; i++) {
            logoutFromWs(((Workspace) o[i]).getWorkspaceName());
        }
        this.getMainframe().setVisible(false);
        try {
            getPctx().deleteProfile(UserManagement.getInstance().getUserName());
        } catch (BaseProfileNotInitializedException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) {
        final String argv[] = args;
        try {
            lck = new FileOutputStream(System.getProperty("java.io.tmpdir") + File.separator + "popeye.lock").getChannel().tryLock();
        } catch (FileNotFoundException ex) {
            System.err.println("Cannot create lock file. Will continue nevertheless.");
        } catch (IOException ex) {
            System.err.println("Cannot create lock file. Will continue nevertheless.");
        }
        if (lck == null) {
            JOptionPane.showMessageDialog(null, "Another instance of popeye was detected running. Close that one first.", "Popeye", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        splash = new SplashScreen(new ImageIcon(ApplicationFramework.class.getResource("/eu/popeye/resources/splashScreen.jpg")));
        splash.setLocationRelativeTo(null);
        splash.setProgressMax(100);
        splash.setScreenVisible(true);
        ApplicationFramework.getInstance().MainbarVisible(true);
    }
}
