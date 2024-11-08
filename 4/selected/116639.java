package ossobook.client.io.tunnel;

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
import ossobook.client.OssoBook;
import ossobook.client.config.ConfigurationOssobook;

/**
 * This is the client part of the tunnel used for the data synchronization.
 * 
 * <p>
 * The tunnel is started automatically started when the program launches (see
 * {@link OssoBook#main(String[])}). The tunnel can be configured in the main
 * configuration file (<code>ossobook.config</code>), using the following
 * parameters:<br/>
 * <dl>
 * <dt>tunnel.disabled</dt>
 * <dd>determines whether to completely disable the tunnel. Useful for local
 * testing (i.e. when running the global database locally) and not having the
 * serverside tunnel running.</dd>
 * <dt>tunnel.listenport</dt>
 * <dd>the port on which the tunnel listens. This is directly used when creating
 * the local connection to the tunnel, and only serves to allow using another
 * port if the default one (51337) is already in use by another program.</dd>
 * <dt>tunnel.backendport</dt>
 * <dd>the port on which to connect to the global server on which the tunnel is
 * running. Note that it is assumed the server is the same as the one defined in
 * the <code>global.host</code> setting.</dd>
 * <dt>tunnel.authkey</dt>
 * <dd>the authentification key to send when connecting with the server side end
 * of the tunnel.</dd>
 * </dl>
 * 
 * <p>
 * This class somewhat follows a singleton pattern, just that it does not make
 * the resulting instance accessible. Or rather, there will never be an instance
 * of the <code>Tunnel</code> class itself, only of its private inner classes
 * used to do the actual forwarding work.
 * 
 * @author fnuecke
 */
public class Tunnel {

    /**
	 * Remember whether the tunnel has been initialized successfully.
	 */
    private static boolean initialized = false;

    /**
	 * Do not allow instantiation.
	 */
    private Tunnel() {
    }

    /**
	 * Initializes the tunnel, opening a server socket and waiting for new
	 * connections which to forward.
	 * 
	 * <p>
	 * Note that calling this function more than once has no effect (only one
	 * tunnel can run at a time).
	 * 
	 * @return <code>true</code> if the tunnel was started successfully or was
	 *         already running, <code>false</code> if the startup failed.
	 */
    public static synchronized boolean init() {
        if (!initialized) {
            try {
                new Acceptor().start();
                initialized = true;
            } catch (IOException e) {
                System.out.println("Failed starting tunnel.");
                e.printStackTrace();
            }
        }
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
        private String backendHost;

        /**
		 * The port on which the back-end server listens.
		 */
        private int backendPort;

        /**
		 * Creates the acceptor instance waiting for new connections.
		 * 
		 * @throws IOException
		 *             if the server socket creation fails.
		 */
        public Acceptor() throws IOException {
            backendHost = OssoBook.config.getProperty("global.host");
            backendPort = Integer.parseInt(OssoBook.config.getProperty("tunnel.backendport"));
            server = ServerSocketFactory.getDefault().createServerSocket(Integer.parseInt(OssoBook.config.getProperty("tunnel.listenport")));
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public void run() {
            try {
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
                ks.load(new FileInputStream(ConfigurationOssobook.CONFIG_DIR + "tunnel.jks"), "ossobook".toCharArray());
                kmf.init(ks, "ossobook".toCharArray());
                sc.init(kmf.getKeyManagers(), trustAllCerts, null);
                SSLSocketFactory sf = sc.getSocketFactory();
                while (true) {
                    Socket client = server.accept();
                    try {
                        SSLSocket backend = (SSLSocket) sf.createSocket(backendHost, backendPort);
                        backend.startHandshake();
                        String hash = OssoBook.config.getProperty("tunnel.authkey");
                        if (!hash.equals("")) {
                            backend.getOutputStream().write((hash + "\n").getBytes());
                        }
                        new Forwarder(client, backend).start();
                        new Forwarder(backend, client).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (client != null && client.isConnected()) {
                            try {
                                client.close();
                            } catch (IOException e1) {
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } finally {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Internal class used to forward single connections. Two created for each
	 * connection created, one for each direction.
	 */
    private static class Forwarder extends Thread {

        /**
		 * The incoming socket, i.e. the socket from which to read data.
		 */
        private Socket from;

        /**
		 * The outgoing socket, i.e. the socket to which to write data.
		 */
        private Socket to;

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
        public Forwarder(Socket from, Socket to) {
            this.from = from;
            this.to = to;
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public void run() {
            try {
                while (true) {
                    to.getOutputStream().write(from.getInputStream().read());
                }
            } catch (IOException e) {
                if (from.isConnected()) {
                    try {
                        from.close();
                    } catch (IOException e1) {
                    }
                }
                if (to.isConnected()) {
                    try {
                        to.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
    }
}
