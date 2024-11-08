package start;

import javax.security.auth.Subject;
import javax.security.auth.login.*;
import org.ietf.jgss.GSSException;
import netTools.IPManager;
import netTools.PortManager;
import netTools.logs.RemoteLogger;
import netTools.mail.StrongMail;
import authentication.AuthHandler;
import authentication.SecureServer;
import authorization.ResourceManager;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.FilePolicyModule;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import jaxb.server.LoggerConf;

/**
 * Strongbox server is the beginning and the end of everything!
 * It performs an initialization phase in which it proves its identity to KDC and obtains valid
 * credentials to do its work. It then initialize the PDP with a valid policy file and Resource Managers
 * as well. After initialization it waits for incoming connections, passing them to Secure Servers 
 * threads which it creates so it can return waiting for new connections.
 * MasterServer also initialize server log so every connection can be tracked.
 * 
 * @author pasquale
 *
 */
public class StrongboxServer {

    public String USER_ROOT_DIR;

    public String POLICY_PATH;

    public String POLICY_FILE = "policy.xml";

    public String LOG_PATH;

    public static final int CONNECTION_TIMEOUT = 120000;

    public static final int ACCEPT_TRANSFER_TIMEOUT = 60000;

    public static final int TRANSFER_TIMEOUT = 30000;

    public void initServer() throws IOException {
        conf = new ConfigManager();
        try {
            conf.initConf();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
        POLICY_PATH = conf.getFolderConf().getRoot() + "/" + conf.getFolderConf().getPolicies();
        LOG_PATH = conf.getFolderConf().getRoot() + "/" + conf.getFolderConf().getLogs() + "/" + (new SimpleDateFormat("yyyy-MM-dd-")).format(new Date()) + "log.txt";
        System.setProperty("java.security.krb5.kdc", conf.getKerberosServer());
        System.setProperty("java.security.auth.login.config", conf.getJaasConf().getJaasConfFilename());
        String princName = conf.getKerberosPrinc();
        System.setProperty("java.security.krb5.realm", princName.split("@")[1]);
        String princPass = conf.getPrincipalPasswd();
        try {
            lc = new LoginContext(conf.getJaasConf().getJaasRule(), new AuthHandler(princName, princPass));
            lc.login();
        } catch (LoginException e) {
            System.err.println("Service authentication failed!");
            System.exit(-1);
        }
        System.out.println("Service authentication succedeed");
        FilePolicyModule policyModule = new FilePolicyModule();
        Set<FilePolicyModule> policyModules = new HashSet<FilePolicyModule>();
        PolicyFinder policyFinder = new PolicyFinder();
        policyModule.addPolicy(POLICY_PATH + "/" + POLICY_FILE);
        policyModules.add(policyModule);
        policyFinder.setModules(policyModules);
        CurrentEnvModule envMod = new CurrentEnvModule();
        AttributeFinder attrFinder = new AttributeFinder();
        List<CurrentEnvModule> attrModules = new ArrayList<CurrentEnvModule>();
        attrModules.add(envMod);
        attrFinder.setModules(attrModules);
        pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));
        USER_ROOT_DIR = conf.getFolderConf().getRoot() + "/" + conf.getFolderConf().getUser() + "/";
        String folders[] = new File(USER_ROOT_DIR).list();
        managers = new ResourceManager[folders.length];
        for (int i = 0; i < managers.length; i++) {
            if (!folders[i].startsWith(".")) managers[i] = new ResourceManager(conf.getFolderConf().getRoot() + "/" + conf.getFolderConf().getConfig() + "/", folders[i], pdp);
        }
        System.out.println("PDP and managers initialized");
        try {
            FileHandler fh = new FileHandler(LOG_PATH, true);
            fh.setFormatter(new SimpleFormatter());
            logger = Logger.getLogger("StrongboxServer");
            logger.addHandler(fh);
            logLevel = conf.getLogger().getSecureLevel();
            if (logLevel != 0) {
                LoggerConf lc = conf.getLogger();
                mailOnLog = lc.isAdminMail();
                rl = new RemoteLogger(this.lc.getSubject());
                try {
                    rl.connect(lc.getLoggerServer(), lc.getLoggerPort(), lc.getLoggerService());
                } catch (IOException e1) {
                    logger.warning(e1.getMessage());
                    System.exit(-1);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GSSException e) {
            e.printStackTrace();
        }
        ipManager = new IPManager(conf.getIPConf().getNTriesBan(), conf.getIPConf().getBanTimeM(), conf.getIPConf().isAdminMail());
        mailOnBan = ipManager.mailEnabled();
        portManager = new PortManager(conf.getPortConf().getBase(), conf.getPortConf().getRange());
        if (ipManager.mailEnabled() || mailOnLog) emergencyMail = new StrongMail(conf.getMailAddress(), LOG_PATH, conf.getMailPasswd(), conf.getMailServer());
    }

    public void service() throws IOException {
        logger.info("Server started servicing on port " + conf.getServerPort());
        if (logLevel != 0) rl.info("Server started servicing on port " + conf.getServerPort());
        ServerSocket ss = new ServerSocket(conf.getServerPort());
        while (true) {
            Socket socket = ss.accept();
            if (!(ipManager.isBanned(socket.getInetAddress()))) {
                socket.setSoTimeout(CONNECTION_TIMEOUT);
                (new Thread(new SecureServer(socket, this))).start();
            } else socket.close();
        }
    }

    public String getRoot() {
        return conf.getFolderConf().getRoot();
    }

    public String getUserFoldersRoot() {
        return conf.getFolderConf().getUser();
    }

    public PortManager getPortManager() {
        return portManager;
    }

    public Subject getSubject() {
        return lc.getSubject();
    }

    public ResourceManager[] getResourceManagers() {
        return managers;
    }

    public Logger getServerLogger() {
        return logger;
    }

    public IPManager getIPManager() {
        return ipManager;
    }

    public RemoteLogger getRemoteLogger() {
        return rl;
    }

    public boolean getEncMode() {
        return conf.getEncMode();
    }

    public int getLogLevel() {
        return logLevel;
    }

    public StrongMail getMail() {
        return emergencyMail;
    }

    public boolean logMailEnabled() {
        return mailOnLog;
    }

    public boolean ipMailEnabled() {
        return mailOnBan;
    }

    public static void main(String[] args) {
        StrongboxServer server = new StrongboxServer();
        try {
            server.initServer();
            server.service();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LoginContext lc;

    private PDP pdp;

    private ResourceManager[] managers;

    private Logger logger;

    private RemoteLogger rl;

    private PortManager portManager;

    private IPManager ipManager;

    private StrongMail emergencyMail;

    private int logLevel;

    public ConfigManager conf;

    private boolean mailOnLog = false;

    private boolean mailOnBan = false;
}
