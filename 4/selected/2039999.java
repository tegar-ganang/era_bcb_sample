package com.art.anette.client.network;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.net.ssl.*;
import javax.swing.*;
import com.art.anette.client.controller.BasicController;
import com.art.anette.client.controller.ClientConfiguration;
import com.art.anette.client.database.ClientDBControl;
import com.art.anette.client.main.Global;
import com.art.anette.client.main.ProgressUtils;
import com.art.anette.client.ui.controls.Statusbar;
import com.art.anette.common.Utils;
import com.art.anette.common.VersionUtils;
import com.art.anette.common.logging.ClientLogRecord;
import com.art.anette.common.logging.LogController;
import com.art.anette.common.logging.Logger;
import com.art.anette.common.logging.SerializingReportHandler;
import com.art.anette.common.network.*;
import com.art.anette.datamodel.dataobjects.simple.Employee;
import com.art.anette.exceptions.DBDirtyException;
import com.art.anette.exceptions.LoginFailedException;
import com.art.anette.exceptions.NetworkException;
import com.art.anette.server.network.ClientThread;
import static com.art.anette.server.network.NetworkControl.logNetworkPacket;
import static com.art.anette.server.network.NetworkControl.prettyPrintObjectForLogging;

/**
 * Diese Klasse erstellt eine Network-Verbindung zwischen einem Client und dem
 * Server.
 *
 * @author Markus Groß
 */
public final class NetworkControl extends Thread {

    /**
     * Die Sprach-Resource, enthält die Übersetzungen der Strings.
     */
    private static final ResourceBundle lang = ResourceBundle.getBundle("com/art/anette/client/ui/resources/lang");

    /**
     * Die Instanz-Variable der Klasse.
     */
    private static NetworkControl instance;

    /**
     * Der SSL-Socket der Verbindung zum Server.
     */
    private SSLSocket socket;

    /**
     * Datenobjekte, die ausgesendet wird.
     */
    private ObjectOutputStream dataOut;

    /**
     * Datenobjekte, die vom Server erhalten wird.
     */
    private ObjectInputStream dataIn;

    /**
     * Die Statusleiste des Clients.
     */
    private Statusbar statusBar;

    /**
     * Login-Daten des Benutzers.
     */
    private LoginRequest lr;

    /**
     * Die Factory für den SSL-Socket.
     */
    private SSLSocketFactory factory;

    private ClientDBControl cdbc;

    private static final Logger logger = LogController.getLogger(NetworkControl.class);

    private static final boolean LOG_TRAFFIC = Boolean.parseBoolean(System.getProperty("anetteLogNetwork"));

    private final Object inLock = new Object();

    private final Object outLock = new Object();

    private boolean closed;

    private List<NetworkListener> listeners = new ArrayList<NetworkListener>();

    /**
     * Initialisiert die Werte die für die Netzwerkverbindung benötigt werden,
     * und eine Verbindung mit dem Server wrid aufgebaut.
     */
    private NetworkControl() {
        super("Reconnector");
    }

    /**
     * Erzeugt eine Instanz des Netzwerk-Controllers. Die Funktion stellt sicher,
     * dass es nur eine Instanz gibt.
     *
     * @return Instanz des Netzwerk-Controllers.
     */
    public static synchronized NetworkControl getInstance() {
        if (instance == null) {
            Utils.checkForDuplicateCall();
            instance = new NetworkControl();
        }
        return instance;
    }

    /**
     * Setzt die Login-Daten und setzt die Streams der SyncUp und
     * SyncDown-Threads auf den Ein- und Ausgabe-Strom des Sockets.
     *
     * @param lr   Login-Daten.
     * @param cdbc Der Client Datenbank-Controller.
     */
    public void setLoginData(final LoginRequest lr, final ClientDBControl cdbc) {
        this.lr = lr;
        this.cdbc = cdbc;
        setStreams();
    }

    /**
     * Wenn die Netzwerkverbindung einmal unterbrochen ist, versucht der Thread
     * sich in einem bestimmten zeitlichen Abstand neu zu verbinden und neu
     * einzuloggen.
     */
    @SuppressWarnings({ "EmptyCatchBlock" })
    @Override
    public void run() {
        while (true) {
            if (!isConnected()) {
                try {
                    reconnect();
                } catch (NetworkException ex) {
                } catch (LoginFailedException ex) {
                } catch (DBDirtyException ex) {
                }
            }
            try {
                sleep(Global.SERVER_RECONNECT_SLEEP * 1000);
            } catch (InterruptedException ex) {
                shutdown(false);
                break;
            }
        }
    }

    /**
     * Versucht eine Verbindung mit dem Server aufzubauen.
     *
     * @throws NetworkException Wenn die Verbindung zum Server
     *                          fehlschlägt, wird dieser Fehler geworfen.
     */
    private void connect(boolean quiet) throws NetworkException {
        final String hostname = getServerName();
        final int port = getServerPort();
        disconnect(false, true, true);
        try {
            fireEvent(NetworkEvent.NEType.connecting);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (statusBar != null) {
                        statusBar.setConnecting();
                    }
                }
            });
            socket = (SSLSocket) factory.createSocket(hostname, port);
            final ObjectOutputStream localOut = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            localOut.flush();
            final ObjectInputStream localIn = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            initialHandshake(localIn, localOut);
            synchronized (inLock) {
                dataIn = localIn;
            }
            synchronized (outLock) {
                dataOut = localOut;
            }
            closed = false;
            logger.info("Connected to " + hostname + ':' + port);
            fireEvent(NetworkEvent.NEType.connected);
        } catch (IOException ex) {
            fireEvent(NetworkEvent.NEType.connectingError);
            disconnect(false, quiet, true);
            throw new NetworkException("Could not connect to server " + hostname + ':' + port + "! Error=" + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            fireEvent(NetworkEvent.NEType.connectingError);
            disconnect(false, false, true);
            throw new NetworkException("Could not connect to server " + hostname + ':' + port + "! Error=" + ex.getMessage(), ex);
        }
    }

    private void fireEvent(NetworkEvent.NEType type, String email) {
        NetworkEvent event = new NetworkEvent(type, getServerName(), getServerPort(), email);
        for (NetworkListener listener : listeners) {
            listener.statusChange(event);
        }
    }

    private void fireEvent(NetworkEvent.NEType type) {
        fireEvent(type, null);
    }

    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }

    private static int getServerPort() {
        return Integer.parseInt(ClientConfiguration.getInstance().getProperty("server.port"));
    }

    private static String getServerName() {
        return ClientConfiguration.getInstance().getProperty("server.address");
    }

    /**
     * See ClientThread.initialHandshake for a documentation of the protokoll.
     *
     * @param dataIn
     * @param dataOut
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void initialHandshake(ObjectInputStream dataIn, ObjectOutputStream dataOut) throws IOException, ClassNotFoundException {
        final long start = System.currentTimeMillis();
        dataOut.writeUnshared(ClientThread.HANDSHAKE_CLIENT);
        dataOut.writeInt(VersionUtils.getMajor());
        dataOut.writeInt(VersionUtils.getMinor());
        dataOut.writeLong(start);
        dataOut.flush();
        Object o = dataIn.readUnshared();
        if (!ClientThread.HANDSHAKE_SERVER.equals(o)) {
            throw new IOException("The server didn't send the correct hand-shake:" + o);
        }
        int major = dataIn.readInt();
        int minor = dataIn.readInt();
        long time = dataIn.readLong();
        logger.info("The server runs " + major + '.' + minor + " with a time diff of " + (start - time) / 1000 + "s. RTT is " + (System.currentTimeMillis() - start) + "ms");
        if (major < VersionUtils.getMajor()) {
            throw new IOException("The server is older than this client. Strange.");
        } else {
            final String myVersionString = VersionUtils.getMajor() + "." + VersionUtils.getMinor();
            final String serverVersionString = major + "." + minor;
            if (major > VersionUtils.getMajor()) {
                ProgressUtils.errorMessage(lang.getString("TooOld"), myVersionString, serverVersionString);
                System.exit(1);
            } else {
                if (minor > VersionUtils.getMinor()) {
                    ProgressUtils.infoMessage(lang.getString("MayUpdate"), myVersionString, serverVersionString);
                }
            }
        }
    }

    /**
     * Versucht eine Verbindung zum Server aufzubauen und sich einzuloggen,
     * sofern die erforderlichen Login-Daten gesetzt sind.
     *
     * @throws NetworkException     Wird geworfen, falls die Verbindung
     *                              zum Server fehlschlägt.
     * @throws LoginFailedException Wird geworfen, falls der Login
     *                              fehlschlägt.
     */
    public void reconnect() throws NetworkException, LoginFailedException, DBDirtyException {
        ProgressUtils.Monitor monitor = null;
        try {
            connect(true);
            fireEvent(NetworkEvent.NEType.loggingIn);
            monitor = ProgressUtils.createProgress(lang.getString("ProgressLoggingIn"));
            Employee e = login(lr);
            cdbc.setOwner(e, false);
            monitor.finish();
            monitor = null;
            fireEvent(NetworkEvent.NEType.loggedIn);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (statusBar != null) {
                        statusBar.setOnline();
                    }
                }
            });
        } catch (LoginFailedException ex) {
            lr = null;
            fireEvent(NetworkEvent.NEType.loginError);
            disconnect(false, false, true);
            throw ex;
        } finally {
            if (monitor != null) {
                monitor.finish();
            }
        }
    }

    /**
     * Schließt die Verbindung zum Server.
     *
     * @param byUser            true bedeutet, die Methode wurde durch das Beenden des
     * @param eventAlreadyFired
     */
    public void disconnect(final boolean byUser, boolean quiet, boolean eventAlreadyFired) {
        try {
            if (!quiet) {
                if (!closed) {
                    logger.info("disconnected " + (byUser ? "by user" : "by error"), new Throwable("Stack trace"));
                }
            }
            closed = true;
            if (dataOut != null) {
                dataOut.close();
            }
            if (dataIn != null) {
                dataIn.close();
            }
            if (socket != null) {
                socket.close();
            }
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (statusBar != null) {
                        statusBar.setOffline();
                    }
                    if (cdbc != null && !byUser) {
                        for (Employee e : cdbc.getEmployees(cdbc.getCompany())) {
                            e.setOnlineStatus(false);
                        }
                    }
                }
            });
            if (!eventAlreadyFired) {
                if (byUser) {
                    fireEvent(NetworkEvent.NEType.disconnectedByUser);
                } else {
                    fireEvent(NetworkEvent.NEType.disconnectedByError);
                }
            }
        } catch (IOException ex) {
        } finally {
            dataIn = null;
            dataOut = null;
            socket = null;
        }
    }

    /**
     * Liefert zurück, ob eine Netzwerkverbindung zum Server besteht.
     *
     * @return True, falls eine Verbindung zum Server besteht.
     */
    public boolean isConnected() {
        return dataIn != null && dataOut != null;
    }

    /**
     * Intialisiert die SSL-Verbindung.
     *
     * @throws Exception Falls ein Fehler bei dem Erstellen der SSL-Verbindung
     *                   auftritt.
     */
    public void initSSL() throws SSLException {
        try {
            char[] passphrase = "secure".toCharArray();
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(getClass().getResourceAsStream("/ssl-client.trusts"), passphrase);
            TrustManagerFactory trust = TrustManagerFactory.getInstance("SunX509");
            trust.init(keystore);
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = trust.getTrustManagers();
            context.init(null, trustManagers, null);
            factory = context.getSocketFactory();
        } catch (KeyStoreException e) {
            throw new SSLException(e);
        } catch (IOException e) {
            throw new SSLException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SSLException(e);
        } catch (CertificateException e) {
            throw new SSLException(e);
        } catch (KeyManagementException e) {
            throw new SSLException(e);
        }
    }

    /**
     * Stellt einen synchronen Request an den Server. Dies wird für den Login-,
     * Registrierungsvorgang und Abteilungs-Abfrage benötigt.
     *
     * @param r Der Request.
     * @return Die Antwort auf den Request.
     * @throws NetworkException Falls die Anfrage fehlschlägt.
     */
    private Response synchroneousRequest(final Request r) throws NetworkException {
        try {
            send(r);
            return receive();
        } catch (ClassNotFoundException ex) {
            throw new NetworkException("Can't send a request to the server.", ex);
        } catch (IOException ex) {
            throw new NetworkException("Can't send a request to the server.", ex);
        }
    }

    public Response receive() throws ClassNotFoundException, IOException {
        synchronized (inLock) {
            final Response obj = (Response) dataIn.readUnshared();
            logReceived(obj);
            return obj;
        }
    }

    public void send(Request obj) throws IOException {
        synchronized (outLock) {
            logSend(obj);
            dataOut.writeUnshared(obj);
            dataOut.reset();
            dataOut.flush();
        }
    }

    /**
     * Sendet einen Login-Request an den Server und gibt den eingeloggten
     * Mitarbeiter als Datenobjekt zurück.
     *
     * @param lr Login-Request.
     * @return Das Employee-Objekt.
     * @throws NetworkException     Tritt auf, wenn keine Verbindung zum
     *                              Server aufgebaut werden konnte.
     * @throws LoginFailedException Tritt auf, wenn der Mitarbeiter
     *                              nicht eingeloggt werden konnte.
     */
    public Employee login(final LoginRequest lr) throws DBDirtyException, NetworkException, LoginFailedException {
        if (this.lr == null && !isConnected()) {
            connect(false);
        }
        fireEvent(NetworkEvent.NEType.loggingIn);
        final Response response = synchroneousRequest(lr);
        if (response instanceof ExceptionResponse) {
            fireEvent(NetworkEvent.NEType.loginError);
            ExceptionResponse exceptionResponse = (ExceptionResponse) response;
            Throwable t = exceptionResponse.getThrowable();
            if (t instanceof LoginFailedException) {
                throw (LoginFailedException) t;
            } else if (t instanceof DBDirtyException) {
                throw (DBDirtyException) t;
            } else {
                throw new LoginFailedException("Login response undefined!");
            }
        } else if (response instanceof DataResponse) {
            DataResponse dataResponse = (DataResponse) response;
            if (dataResponse.getObjects().size() != 1) {
                fireEvent(NetworkEvent.NEType.loginError);
                throw new LoginFailedException("Login response undefined!");
            }
            Object obj = dataResponse.getObjects().get(0);
            if (!(obj instanceof Employee)) {
                fireEvent(NetworkEvent.NEType.loginError);
                throw new LoginFailedException("Login response undefined!");
            }
            Employee employee = (Employee) obj;
            {
                final List<ClientLogRecord> list = SerializingReportHandler.getInstance().read();
                for (ClientLogRecord clientLogRecord : list) {
                    final Response o1 = synchroneousRequest(clientLogRecord);
                    if (!(o1 instanceof VoidResponse)) {
                        fireEvent(NetworkEvent.NEType.loginError);
                        throw new LoginFailedException("Problem sending exception to the server.");
                    }
                }
                SerializingReportHandler.getInstance().clear();
                if (!list.isEmpty()) {
                    logger.info(String.format("Sent %d exceptions to the server.", list.size()));
                }
            }
            setStreams();
            fireEvent(NetworkEvent.NEType.loggedIn, employee.getEmail());
            return employee;
        } else {
            fireEvent(NetworkEvent.NEType.loginError);
            throw new LoginFailedException("Login response undefined!");
        }
    }

    /**
     * Setzt die Status-Leiste.
     *
     * @param statusBar Status-Leiste.
     */
    public void setStatusBar(final Statusbar statusBar) {
        this.statusBar = statusBar;
    }

    /**
     * Beendet die Netzwerverbindung.
     *
     * @param byUser true, falls der Client durch eine Benutzeraktion beendet
     *               wird.
     */
    public void shutdown(boolean byUser) {
        interrupt();
        setStatusBar(null);
        fireEvent(NetworkEvent.NEType.shutdown);
        disconnect(byUser, false, true);
    }

    /**
     * Setzt den Ein- und Ausgabe-Strom des SyncUp- und SyncDown-Threads.
     * Dadurch werden diese aufgewecket und beginnen zu arbeiten.
     */
    public void setStreams() {
        if (cdbc != null) {
            cdbc.getSyncDownThread().wakeup();
            cdbc.getSyncUpThread().wakeup();
        }
    }

    public static void logReceived(Response response) {
        if (LOG_TRAFFIC) {
            logNetworkPacket(logger, "From server", response);
        }
        if (response instanceof DataResponse) {
            DataResponse dataResponse = (DataResponse) response;
            for (Object o : dataResponse.getObjects()) {
                BasicController.getInstance().addHistory("RCV " + prettyPrintObjectForLogging(o));
            }
        }
    }

    public static void logSend(Request request) {
        if (LOG_TRAFFIC) {
            logNetworkPacket(logger, "To server", request);
        }
        if (request instanceof DataRequest) {
            DataRequest dataRequest = (DataRequest) request;
            BasicController.getInstance().addHistory("SND " + prettyPrintObjectForLogging(dataRequest.getObject()));
        }
    }

    public static String getServerDescr() {
        return getServerName() + ':' + getServerPort();
    }
}
