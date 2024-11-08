package ossobook.client.io.forwarder;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ossobook.Messages;
import ossobook.client.OssoBook;
import ossobook.client.util.Configuration;

/**
 * This is the client part of the forwarder used for the data synchronization.
 *
 * <p>
 * The forwarder is started automatically started when the program launches (see
 * {@link OssoBook#main(String[])}). The forwarder can be configured in the main
 * configuration file (<code>ossobook.config</code>), using the following
 * parameters:<br/>
 * <dl>
 * <dt>tunnel.disabled</dt>
 * <dd>determines whether to completely disable the tunnel. Useful for local
 * testing (i.e. when running the global database locally) and not having the
 * server side tunnel running.</dd>
 * <dt>tunnel.x.listenport</dt>
 * <dd>the port on which the tunnel listens. This is directly used when creating
 * the local connection to the tunnel, and only serves to allow using another
 * port if the default one (51337) is already in use by another program.</dd>
 * <dt>tunnel.x.backendport</dt>
 * <dd>the port on which to connect to the global server on which the tunnel is
 * running. Note that it is assumed the server is the same as the one defined in
 * the <code>global.host</code> setting.</dd>
 * <dt>tunnel.x.authkey</dt>
 * <dd>the authentication key to send when connecting with the server side end
 * of the tunnel.</dd>
 * </dl>
 * </p>
 *
 * <p>
 * The 'x' in the specific configurations is a numeric value, starting with 0.
 * This numbering must be continuous for all to be parsed, as the parser will
 * stop as soon as there is no configuration for one number.
 * </p>
 *
 * <p>
 * This class somewhat follows a singleton pattern, just that it does not make
 * the resulting instance accessible. Or rather, there will never be an instance
 * of the <code>Tunnel</code> class itself, only of its private inner classes
 * used to do the actual forwarding work.
 *
 * @author fnuecke
 */
public final class Forwarder {

    /**
	 * Logging.
	 */
    private static final Log log = LogFactory.getLog(Forwarder.class);

    /**
	 * Remember whether the tunnel has been initialized successfully.
	 */
    private static boolean initialized = false;

    /**
	 * Do not allow instantiation.
	 */
    private Forwarder() {
    }

    /**
	 * Initializes the tunnel, opening a server socket and waiting for new
	 * connections which to forward.
	 *
	 * <p>
	 * Note that calling this function more than once has no effect (only one
	 * tunnel can run at a time).
	 */
    public static synchronized void init() {
        if (!initialized && "true".equals(Configuration.config.getProperty("forwarder.enabled"))) {
            for (int i = 0; i < Integer.MAX_VALUE; ++i) {
                String backendAddress = Configuration.config.getProperty(String.format("forwarder.%d.backendaddress", i));
                String backendPort = Configuration.config.getProperty(String.format("forwarder.%d.backendport", i));
                String listenPort = Configuration.config.getProperty(String.format("forwarder.%d.listenport", i));
                String authKey = Configuration.config.getProperty(String.format("forwarder.%d.authkey", i));
                if (authKey != null && authKey.equals("!")) {
                    authKey = null;
                }
                if (backendAddress == null && backendPort == null && listenPort == null) {
                    break;
                } else if (backendAddress == null || backendPort == null || listenPort == null) {
                    log.warn(String.format(Messages.getString("Forwarder.1"), i));
                } else {
                    try {
                        new Acceptor(Integer.parseInt(listenPort), backendAddress, Integer.parseInt(backendPort), authKey).start();
                        log.info(String.format(Messages.getString("Forwarder.2"), listenPort, backendAddress, backendPort, authKey));
                        initialized = true;
                    } catch (NumberFormatException e) {
                        log.warn(String.format(Messages.getString("Forwarder.3"), i));
                    } catch (IOException e) {
                        log.error(String.format(Messages.getString("Forwarder.4"), i), e);
                    } catch (UnrecoverableKeyException e) {
                        log.error(String.format(Messages.getString("Forwarder.5"), i), e);
                    } catch (CertificateException e) {
                        log.error(String.format(Messages.getString("Forwarder.5"), i), e);
                    } catch (NoSuchAlgorithmException e) {
                        log.error(String.format(Messages.getString("Forwarder.5"), i), e);
                    } catch (KeyStoreException e) {
                        log.error(String.format(Messages.getString("Forwarder.5"), i), e);
                    } catch (KeyManagementException e) {
                        log.error(String.format(Messages.getString("Forwarder.5"), i), e);
                    }
                }
            }
        }
    }

    /**
	 * Whether the forwarders were successfully initialized, i.e. whether the
	 * forwarders are not disabled and the {@link #init()} function called.
	 *
	 * @return <code>true</code> if the forwarders are running.
	 */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
	 * Internal class used to wait for new connections which to forward to the
	 * back-end.
	 */
    private static class Acceptor extends Thread {

        /**
		 * Server socket used to listen for new connections.
		 */
        private ServerSocket server;

        /**
		 * The host name of the back-end server to which to connect and forward
		 * data to.
		 */
        private final String backendAddress;

        /**
		 * The port on which the back-end server listens.
		 */
        private final int backendPort;

        /**
		 * Key used for authenticating self with server end of forwarder.
		 */
        private final String key;

        /**
		 * Factory used to generate SSL sockets when accepting connections.
		 */
        private SSLSocketFactory socketFactory;

        /**
		 * Creates the acceptor instance waiting for new connections.
		 *
		 * @param listenPort the port on which to listen for incoming connections.
		 * @param backendAddress the backend address to which to forward data from incoming connections.
		 * @param backendPort the port to which to connect at the backend address.
		 * @param key the key to use when connecting to the backend.
		 * @throws IOException
		 *             if the server socket creation fails.
		 * @throws java.security.KeyManagementException if SSL goes kablooie.
		 * @throws java.security.KeyStoreException if SSL goes kablooie.
		 * @throws java.security.NoSuchAlgorithmException if SSL goes kablooie.
		 * @throws java.security.UnrecoverableKeyException if SSL goes kablooie.
		 * @throws java.security.cert.CertificateException if SSL goes kablooie.
		 */
        public Acceptor(int listenPort, String backendAddress, int backendPort, String key) throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
            this.backendAddress = backendAddress;
            this.backendPort = backendPort;
            this.key = key;
            this.server = ServerSocketFactory.getDefault().createServerSocket(listenPort);
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };
            SSLContext sc = SSLContext.getInstance("SSLv3");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream inputStream = new FileInputStream(Configuration.CONFIG_DIR + "tunnel.jks");
            try {
                ks.load(inputStream, "ossobook".toCharArray());
            } catch (IOException e) {
                throw e;
            } catch (NoSuchAlgorithmException e) {
                throw e;
            } catch (CertificateException e) {
                throw e;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            kmf.init(ks, "ossobook".toCharArray());
            sc.init(kmf.getKeyManagers(), trustAllCerts, null);
            socketFactory = sc.getSocketFactory();
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public void run() {
            try {
                while (!server.isClosed()) {
                    Socket client = server.accept();
                    try {
                        SSLSocket backend = (SSLSocket) socketFactory.createSocket(backendAddress, backendPort);
                        backend.startHandshake();
                        if (key != null && key.length() > 0) {
                            backend.getOutputStream().write((key + "\n").getBytes());
                        }
                        new Pipe(client, backend).start();
                        new Pipe(backend, client).start();
                    } catch (IOException e) {
                        LogFactory.getLog(Forwarder.class).error(e, e);
                        if (client != null && client.isConnected()) {
                            try {
                                client.close();
                            } catch (IOException e1) {
                                LogFactory.getLog(Forwarder.class).error(e1, e1);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LogFactory.getLog(Forwarder.class).error(e, e);
            } finally {
                if (!server.isClosed()) {
                    try {
                        server.close();
                    } catch (IOException e) {
                        LogFactory.getLog(Forwarder.class).error(e, e);
                    }
                }
            }
        }
    }

    /**
	 * Internal class used to forward single connections. Two created for each
	 * connection created, one for each direction.
	 */
    private static class Pipe extends Thread {

        /**
		 * The incoming socket, i.e. the socket from which to read data.
		 */
        private final Socket from;

        /**
		 * The outgoing socket, i.e. the socket to which to write data.
		 */
        private final Socket to;

        /**
		 * New forwarder instance connecting the two given sockets by
		 * continually forwarding data read from the <code>from</code> socket to
		 * the <code>to</code> socket.
		 *
		 * @param from
		 *            the socket from which to read the data.
		 * @param to
		 *            the socket to which to write the data read from the
		 *            <code>from</code> socket.
		 */
        public Pipe(Socket from, Socket to) {
            this.from = from;
            this.to = to;
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public void run() {
            try {
                while (!to.isClosed() && !from.isClosed()) {
                    to.getOutputStream().write(from.getInputStream().read());
                }
            } catch (IOException e) {
            } finally {
                if (!from.isClosed()) {
                    try {
                        from.close();
                    } catch (IOException e1) {
                        LogFactory.getLog(Forwarder.class).error(e1, e1);
                    }
                }
                if (!to.isClosed()) {
                    try {
                        to.close();
                    } catch (IOException e1) {
                        LogFactory.getLog(Forwarder.class).error(e1, e1);
                    }
                }
            }
        }
    }
}
