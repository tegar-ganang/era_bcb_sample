package start;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import jaxb.logger.Loggerconf;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.MessageProp;
import encryption.EncryptionManager;
import authentication.AuthHandler;

/**
 * A simple logger server: it accepts connection just by strongbox server's principal
 * It is identified by strongbox/logger principal so make sure to create a principal with
 * this id and password "hellLogger03"
 */
public class StrongboxLogger {

    private static final String CONF_FILENAME = "logger.xml";

    private final String STRONGBOX_SERVER;

    private final String LOGGER_PRINCIPAL;

    private final String LOGGER_PASSWORD;

    private final int LOGGER_PORT;

    private final String LOGGER_CONF = "LogServer";

    private final String LOG_FILE = "./logs/" + (new SimpleDateFormat("yyyy-MM-dd-")).format(new Date()) + "remotelog.txt";

    public void initConf() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            conf = new Loggerconf();
            System.out.print("Logger port: ");
            conf.setPort(Integer.parseInt(in.readLine()));
            System.out.println("Kerberos server address: ");
            conf.setKerberos(in.readLine());
            System.out.print("Logger service principal(with @ suffix): ");
            conf.setServiceName(in.readLine());
            System.out.print("Logger service password: ");
            conf.setPassword(in.readLine());
            System.out.print("Strongbox server principal(with @ suffix): ");
            conf.setStrongboxPrinc(in.readLine());
            conf.saveToFile(CONF_FILENAME);
            (new File("./logs")).mkdir();
            BufferedReader inConf = new BufferedReader(new InputStreamReader(StrongboxLogger.class.getResourceAsStream("/LoggerServer.config")));
            PrintWriter outConf = new PrintWriter(new FileOutputStream("LoggerServer.config"), true);
            String read = null;
            while ((read = inConf.readLine()) != null) outConf.write(read + "\n");
            inConf.close();
            outConf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StrongboxLogger() {
        if (!(new File(CONF_FILENAME).exists())) initConf();
        conf = Loggerconf.getFromFile(CONF_FILENAME);
        STRONGBOX_SERVER = conf.getStrongboxPrinc();
        LOGGER_PRINCIPAL = conf.getServiceName();
        LOGGER_PORT = conf.getPort();
        LOGGER_PASSWORD = conf.getPassword();
    }

    public void initServer() throws LoginException, SecurityException, IOException {
        System.setProperty("java.security.krb5.realm", conf.getServiceName().split("@")[1]);
        System.setProperty("java.security.krb5.kdc", conf.getKerberos());
        System.setProperty("java.security.auth.login.config", "LoggerServer.config");
        lc = new LoginContext(LOGGER_CONF, new AuthHandler(LOGGER_PRINCIPAL, LOGGER_PASSWORD));
        lc.login();
        FileHandler fh = new FileHandler(LOG_FILE, true);
        fh.setFormatter(new SimpleFormatter());
        logger = Logger.getLogger("StrongBoxLogger");
        logger.addHandler(fh);
    }

    public void service() throws IOException, GSSException {
        ServerSocket ss = new ServerSocket(LOGGER_PORT);
        boolean authOK = false;
        Socket s = null;
        while (!authOK) {
            try {
                s = ss.accept();
                logger.info("Received connection from :" + s.getInetAddress());
                inStream = new DataInputStream(s.getInputStream());
                outStream = new DataOutputStream(s.getOutputStream());
                try {
                    Subject.doAsPrivileged(lc.getSubject(), new PrivilegedExceptionAction<Integer>() {

                        public Integer run() throws GSSException, IOException {
                            GSSManager manager = GSSManager.getInstance();
                            context = manager.createContext((GSSCredential) null);
                            initSecureContext(context);
                            return null;
                        }
                    }, null);
                    String client = context.getSrcName().toString();
                    if (client.equals(STRONGBOX_SERVER)) {
                        authOK = true;
                        logger.info("Strongbox server connected!");
                    } else {
                        logger.warning(client + " connected instead of strongbox server: closing connection!");
                        s.close();
                    }
                } catch (PrivilegedActionException e) {
                    e.printStackTrace();
                    logger.warning("Login failed!!");
                }
            } catch (IOException e1) {
                System.err.println(e1.getMessage());
            }
        }
        EncryptionManager em = new EncryptionManager(context, new MessageProp(0, true), 0);
        servicing = true;
        while (servicing) {
            try {
                String message = new String(em.readEncMessage(inStream));
                String[] splitted = message.split("/");
                String op = splitted[0];
                String mess = "";
                for (int i = 1; i < splitted.length; i++) mess += splitted[i];
                if (op.equals("I")) logger.info(mess); else if (op.equals("W")) logger.warning(mess); else if (op.equals("S")) logger.severe(mess);
            } catch (IOException ex) {
                servicing = false;
                logger.info("Connection with strongbox server closed");
            }
        }
    }

    private byte[] initSecureContext(GSSContext context) throws GSSException, IOException {
        byte[] token = null;
        while (!context.isEstablished()) {
            token = new byte[inStream.readInt()];
            inStream.readFully(token);
            token = context.acceptSecContext(token, 0, token.length);
            if (token != null) {
                outStream.writeInt(token.length);
                outStream.write(token);
                outStream.flush();
            }
        }
        return token;
    }

    public static void main(String args[]) {
        StrongboxLogger server = new StrongboxLogger();
        try {
            server.initServer();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        server.logger.info("Logger server started");
        try {
            server.service();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (GSSException e) {
            System.err.println(e.getMessage());
        }
    }

    private Loggerconf conf;

    private DataInputStream inStream;

    private DataOutputStream outStream;

    private Logger logger;

    private LoginContext lc;

    private boolean servicing;

    private GSSContext context;
}
