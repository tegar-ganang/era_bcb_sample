package net.jini.jeri.ssl;

import com.sun.jini.action.GetLongAction;
import com.sun.jini.jeri.internal.connection.BasicServerConnManager;
import com.sun.jini.jeri.internal.connection.ServerConnManager;
import com.sun.jini.jeri.internal.runtime.Util;
import com.sun.jini.logging.Levels;
import com.sun.jini.thread.Executor;
import com.sun.jini.thread.GetThreadPoolAction;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;
import net.jini.core.constraint.InvocationConstraints;
import net.jini.io.UnsupportedConstraintException;
import net.jini.jeri.Endpoint;
import net.jini.jeri.RequestDispatcher;
import net.jini.jeri.ServerEndpoint.ListenContext;
import net.jini.jeri.ServerEndpoint.ListenCookie;
import net.jini.jeri.ServerEndpoint.ListenEndpoint;
import net.jini.jeri.ServerEndpoint.ListenHandle;
import net.jini.jeri.ServerEndpoint;
import net.jini.jeri.connection.InboundRequestHandle;
import net.jini.jeri.connection.ServerConnection;
import net.jini.security.AuthenticationPermission;
import net.jini.security.Security;
import net.jini.security.SecurityContext;

/**
 * Provides the implementation of SslServerEndpoint so that the implementation
 * can be inherited by HttpsServerEndpoint without revealing the inheritance in
 * the public API.
 * 
 * @author Sun Microsystems, Inc.
 */
class SslServerEndpointImpl extends Utilities {

    /** Server logger */
    static final Logger logger = serverLogger;

    /**
	 * The maximum time a session should be used before expiring -- non-final to
	 * facilitate testing. Use 24 hours to allow the client, which uses 23.5
	 * hours, to renegotiate a new session before the server timeout.
	 */
    static long maxServerSessionDuration = ((Long) Security.doPrivileged(new GetLongAction("com.sun.jini.jeri.ssl.maxServerSessionDuration", 24 * 60 * 60 * 1000))).longValue();

    /**
	 * Executes a Runnable in a system thread -- used for listener accept
	 * threads.
	 */
    static final Executor systemExecutor = (Executor) Security.doPrivileged(new GetThreadPoolAction(false));

    /** The default server connection manager. */
    private static final ServerConnManager defaultServerConnectionManager = new BasicServerConnManager();

    /** The associated server endpoint. */
    final ServerEndpoint serverEndpoint;

    /** The server subject, or null if the server is anonymous. */
    final Subject serverSubject;

    /**
	 * The principals to use for authentication, or null if the server is
	 * anonymous.
	 */
    final Set serverPrincipals;

    /**
	 * The host name that clients should use to connect to this server, or null
	 * if enumerateListenEndpoints should compute the default.
	 */
    final String serverHost;

    /** The server port */
    final int port;

    /** The socket factory for use in the associated Endpoint. */
    final SocketFactory socketFactory;

    /** The server socket factory. */
    final ServerSocketFactory serverSocketFactory;

    /**
	 * The permissions needed to authenticate when listening on this endpoint,
	 * or null if the server is anonymous.
	 */
    Permission[] listenPermissions;

    /** The listen endpoint. */
    private final ListenEndpoint listenEndpoint;

    /** The factory for creating JSSE sockets -- set by sslInit */
    private SSLSocketFactory sslSocketFactory;

    /**
	 * The authentication manager for the SSLContext for this endpoint -- set by
	 * sslInit.
	 */
    private ServerAuthManager authManager;

    /** The server connection manager. */
    ServerConnManager serverConnectionManager = defaultServerConnectionManager;

    /** Creates an instance of this class. */
    SslServerEndpointImpl(ServerEndpoint serverEndpoint, Subject serverSubject, X500Principal[] serverPrincipals, String serverHost, int port, SocketFactory socketFactory, ServerSocketFactory serverSocketFactory) {
        this.serverEndpoint = serverEndpoint;
        boolean useCurrentSubject = serverSubject == null;
        if (useCurrentSubject) {
            final AccessControlContext acc = AccessController.getContext();
            serverSubject = (Subject) AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    return Subject.getSubject(acc);
                }
            });
        }
        this.serverPrincipals = (serverPrincipals == null) ? computePrincipals(serverSubject) : checkPrincipals(serverPrincipals);
        if (this.serverPrincipals == null) {
            listenPermissions = null;
        } else {
            listenPermissions = new AuthenticationPermission[this.serverPrincipals.size()];
            int i = 0;
            for (Iterator iter = this.serverPrincipals.iterator(); iter.hasNext(); i++) {
                Principal p = (Principal) iter.next();
                listenPermissions[i] = new AuthenticationPermission(Collections.singleton(p), null, "listen");
            }
        }
        if (this.serverPrincipals == null || (useCurrentSubject && serverPrincipals != null && !hasListenPermissions())) {
            this.serverSubject = null;
            this.listenPermissions = null;
        } else {
            this.serverSubject = serverSubject;
        }
        this.serverHost = serverHost;
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this.port = port;
        this.socketFactory = socketFactory;
        this.serverSocketFactory = serverSocketFactory;
        listenEndpoint = createListenEndpoint();
    }

    /** Computes the principals in the subject available for authentication */
    private static Set computePrincipals(Subject subject) {
        if (subject == null) {
            return null;
        }
        X500PrivateCredential[] credentials = (X500PrivateCredential[]) AccessController.doPrivileged(new SubjectCredentials.GetAllPrivateCredentialsAction(subject));
        Set result = SubjectCredentials.getPrincipals(subject, ANY_KEY_ALGORITHM, credentials);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            for (Iterator iter = result.iterator(); iter.hasNext(); ) {
                Principal p = (Principal) iter.next();
                try {
                    sm.checkPermission(new AuthenticationPermission(Collections.singleton(p), null, "listen"));
                } catch (SecurityException e) {
                    logger.log(Levels.HANDLED, "compute principals for server endpoint " + "caught exception", e);
                    iter.remove();
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
	 * Returns true if the caller has AuthenticationPermission for listen on
	 * this endpoint.
	 */
    private boolean hasListenPermissions() {
        try {
            checkListenPermissions(false);
            return true;
        } catch (SecurityException e) {
            logger.log(Levels.HANDLED, "check listen permissions for server endpoint " + "caught exception", e);
            return false;
        }
    }

    /**
	 * Checks that principals is not empty and contains no nulls, and returns it
	 * as a set. Returns null if no principals are specified.
	 */
    private static Set checkPrincipals(X500Principal[] principals) {
        if (principals.length == 0) {
            return null;
        }
        Set result = new HashSet(principals.length);
        for (int i = principals.length; --i >= 0; ) {
            X500Principal p = principals[i];
            if (p == null) {
                throw new NullPointerException("Server principal cannot be null");
            }
            result.add(p);
        }
        return result;
    }

    /**
	 * Initializes the sslSocketFactory and authManager fields. Wait to do this
	 * until needed, because creating the SSLContext requires initializing the
	 * secure random number generator, which can be time consuming.
	 */
    private void sslInit() {
        assert Thread.holdsLock(this);
        SSLContextInfo info = getServerSSLContextInfo(serverSubject, serverPrincipals);
        sslSocketFactory = info.sslContext.getSocketFactory();
        authManager = (ServerAuthManager) info.authManager;
    }

    /** Returns the SSLSocketFactory, calling sslInit if needed. */
    final SSLSocketFactory getSSLSocketFactory() {
        synchronized (this) {
            if (sslSocketFactory == null) {
                sslInit();
            }
        }
        return sslSocketFactory;
    }

    /** Returns the ServerAuthManager, calling sslInit if needed. */
    final ServerAuthManager getAuthManager() {
        synchronized (this) {
            if (authManager == null) {
                sslInit();
            }
        }
        return authManager;
    }

    /** Returns a hash code value for this object. */
    public int hashCode() {
        return getClass().hashCode() ^ System.identityHashCode(serverSubject) ^ (serverPrincipals == null ? 0 : serverPrincipals.hashCode()) ^ (serverHost == null ? 0 : serverHost.hashCode()) ^ port ^ (socketFactory != null ? socketFactory.hashCode() : 0) ^ (serverSocketFactory != null ? serverSocketFactory.hashCode() : 0);
    }

    /**
	 * Two instances of this class are equal if they have the same actual class;
	 * have server subjects that compare equal using ==; have server principals
	 * that are either both null or are equal when compared as the elements of a
	 * Set; have the same server host and port; have socket factories that are
	 * either both null, or have the same actual class and are equal; and have
	 * server socket factories that are either both null, or have the same
	 * actual class and are equal.
	 */
    public boolean equals(Object object) {
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        SslServerEndpointImpl other = (SslServerEndpointImpl) object;
        return serverSubject == other.serverSubject && safeEquals(serverPrincipals, other.serverPrincipals) && safeEquals(serverHost, other.serverHost) && port == other.port && Util.sameClassAndEquals(socketFactory, other.socketFactory) && Util.sameClassAndEquals(serverSocketFactory, other.serverSocketFactory);
    }

    /** Returns a string representation of this object. */
    public String toString() {
        return getClassName(this) + fieldsToString();
    }

    /** Returns a string representation of the fields of this object. */
    final String fieldsToString() {
        return "[" + (serverPrincipals == null ? "" : serverPrincipals.toString() + ", ") + (serverHost == null ? "" : serverHost + ":") + port + (serverSocketFactory != null ? ", " + serverSocketFactory : "") + (socketFactory != null ? ", " + socketFactory : "") + "]";
    }

    final InvocationConstraints checkConstraints(InvocationConstraints constraints) throws UnsupportedConstraintException {
        try {
            checkListenPermissions(false);
        } catch (SecurityException e) {
            if (logger.isLoggable(Levels.FAILED)) {
                logThrow(logger, Levels.FAILED, SslServerEndpoint.class, "checkConstraints", "check constraints for {0}\nwith {1}\nthrows", new Object[] { this, constraints }, e);
            }
            throw e;
        }
        Set clientPrincipals = getClientPrincipals(constraints);
        if (clientPrincipals == null) {
            clientPrincipals = Collections.singleton(UNKNOWN_PRINCIPAL);
        }
        Map serverKeyTypes = new HashMap();
        List certPaths = SubjectCredentials.getCertificateChains(serverSubject);
        if (certPaths != null) {
            for (int i = certPaths.size(); --i >= 0; ) {
                CertPath chain = (CertPath) certPaths.get(i);
                X509Certificate cert = SubjectCredentials.firstX509Cert(chain);
                X500Principal principal = SubjectCredentials.getPrincipal(serverSubject, cert);
                if (principal != null) {
                    Collection keyTypes = (Collection) serverKeyTypes.get(principal);
                    if (keyTypes == null) {
                        keyTypes = new ArrayList(1);
                        serverKeyTypes.put(principal, keyTypes);
                    }
                    keyTypes.add(cert.getPublicKey().getAlgorithm());
                }
            }
        }
        String[] suites = getSupportedCipherSuites();
        for (int suiteIndex = suites.length; --suiteIndex >= 0; ) {
            String suite = suites[suiteIndex];
            String suiteKeyType = getKeyAlgorithm(suite);
            Iterator sIter = (serverPrincipals == null ? Collections.EMPTY_SET : serverPrincipals).iterator();
            X500Principal server;
            do {
                if (sIter.hasNext()) {
                    server = (X500Principal) sIter.next();
                    assert server != null;
                    Collection keyTypes = (Collection) serverKeyTypes.get(server);
                    if (keyTypes == null || !keyTypes.contains(suiteKeyType)) {
                        continue;
                    }
                } else {
                    server = null;
                }
                Iterator cIter = clientPrincipals.iterator();
                Principal client;
                do {
                    if (cIter.hasNext()) {
                        client = (Principal) cIter.next();
                        assert client != null;
                    } else {
                        client = null;
                    }
                    InvocationConstraints unfulfilledConstraints = getUnfulfilledConstraints(suite, client, server, constraints);
                    if (unfulfilledConstraints != null) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "check constraints for {0}\n" + "with {1}\nreturns {2}", new Object[] { serverEndpoint, constraints, unfulfilledConstraints });
                        }
                        return unfulfilledConstraints;
                    }
                } while (client != null);
            } while (server != null);
        }
        UnsupportedConstraintException uce = new UnsupportedConstraintException("Constraints are not supported: " + constraints);
        if (logger.isLoggable(Levels.FAILED)) {
            logThrow(logger, Levels.FAILED, SslServerEndpoint.class, "checkConstraints", "check constraints for {0}\nwith {1}\nthrows", new Object[] { serverEndpoint, constraints }, uce);
        }
        throw uce;
    }

    /**
	 * Returns null if the constraints are not supported, else any integrity
	 * constraints required or preferred by the arguments.
	 */
    static InvocationConstraints getUnfulfilledConstraints(String cipherSuite, Principal client, Principal server, InvocationConstraints constraints) {
        boolean supported = false;
        for (int i = 2; --i >= 0; ) {
            boolean integrity = i == 0;
            ConnectionContext context = ConnectionContext.getInstance(cipherSuite, client, server, integrity, false, constraints);
            if (context != null) {
                if (context.getIntegrityRequired()) {
                    return INTEGRITY_REQUIRED;
                } else if (context.getIntegrityPreferred()) {
                    return INTEGRITY_PREFERRED;
                } else {
                    supported = true;
                }
            }
        }
        return supported ? InvocationConstraints.EMPTY : null;
    }

    final Endpoint enumerateListenEndpoints(ListenContext listenContext) throws IOException {
        Exception exception = null;
        try {
            String resolvedHost = this.serverHost;
            if (resolvedHost == null) {
                InetAddress localAddr;
                try {
                    localAddr = (InetAddress) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                        public Object run() throws UnknownHostException {
                            return InetAddress.getLocalHost();
                        }
                    });
                } catch (PrivilegedActionException e) {
                    UnknownHostException uhe = (UnknownHostException) e.getCause();
                    if (logger.isLoggable(Levels.FAILED)) {
                        logThrow(logger, Levels.FAILED, this.getClass(), "enumerateListenEndpoints", "InetAddress.getLocalHost() throws", null, uhe);
                    }
                    try {
                        InetAddress.getLocalHost();
                    } catch (UnknownHostException te) {
                        throw te;
                    }
                    throw new UnknownHostException("Host name cleared due to " + "insufficient caller " + "permissions");
                }
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    try {
                        sm.checkConnect(localAddr.getHostName(), -1);
                    } catch (SecurityException e) {
                        exception = e;
                        throw new SecurityException("Access to resolve local host denied");
                    }
                }
                resolvedHost = localAddr.getHostAddress();
            }
            Endpoint result = createEndpoint(resolvedHost, checkCookie(listenContext.addListenEndpoint(listenEndpoint)));
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "enumerate listen endpoints for {0}\nreturns {1}", new Object[] { this, result });
            }
            return result;
        } finally {
            if (exception != null && logger.isLoggable(Levels.FAILED)) {
                logThrow(logger, Levels.FAILED, SslServerEndpointImpl.class, "enumerateListenEndpoints", "enumerate listen endpoints for {0}\nthrows", new Object[] { this }, exception);
            }
        }
    }

    /** Creates a listen endpoint for this server endpoint. */
    ListenEndpoint createListenEndpoint() {
        return new SslListenEndpoint();
    }

    /**
	 * Creates an endpoint for this server endpoint corresponding to the
	 * specified server host and listen cookie.
	 */
    Endpoint createEndpoint(String serverHost, SslListenCookie cookie) {
        return SslEndpoint.getInstance(serverHost, cookie.getPort(), socketFactory);
    }

    /**
	 * Checks that the argument is a valid listen cookie for this server
	 * endpoint.
	 */
    private SslListenCookie checkCookie(ListenCookie cookie) {
        if (!(cookie instanceof SslListenCookie)) {
            throw new IllegalArgumentException("Cookie must be of type SslListenCookie: " + cookie);
        }
        SslListenCookie sslListenCookie = ((SslListenCookie) cookie);
        ListenEndpoint cookieListenEndpoint = sslListenCookie.getListenEndpoint();
        if (!listenEndpoint.equals(cookieListenEndpoint)) {
            throw new IllegalArgumentException("Cookie has wrong listen endpoint: found " + cookieListenEndpoint + ", expected " + listenEndpoint);
        }
        return sslListenCookie;
    }

    /**
	 * Check for permission to listen on this endpoint, but only checking socket
	 * permissions if checkSocket is true.
	 */
    final void checkListenPermissions(boolean checkSocket) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (checkSocket) {
                sm.checkListen(port);
            }
            if (listenPermissions != null) {
                for (int i = listenPermissions.length; --i >= 0; ) {
                    sm.checkPermission(listenPermissions[i]);
                }
            }
        }
    }

    /** Implements ListenEndpoint */
    class SslListenEndpoint extends Utilities implements ListenEndpoint {

        public void checkPermissions() {
            checkListenPermissions(true);
        }

        public ListenHandle listen(RequestDispatcher requestDispatcher) throws IOException {
            if (requestDispatcher == null) {
                throw new NullPointerException("Request dispatcher cannot be null");
            }
            checkCredentials();
            ServerSocket serverSocket = serverSocketFactory != null ? serverSocketFactory.createServerSocket(port) : new ServerSocket(port);
            return createListenHandle(requestDispatcher, serverSocket);
        }

        /**
		 * Check that the subject has credentials for the principals specified
		 * when the server endpoint was created.
		 */
        private void checkCredentials() throws UnsupportedConstraintException {
            if (serverSubject == null) {
                return;
            }
            checkListenPermissions(false);
            Set principals = serverSubject.getPrincipals();
            Map progress = new HashMap(serverPrincipals.size());
            for (Iterator i = serverPrincipals.iterator(); i.hasNext(); ) {
                X500Principal p = (X500Principal) i.next();
                if (!principals.contains(p)) {
                    throw new UnsupportedConstraintException("Missing principal: " + p);
                }
                progress.put(p, X500Principal.class);
            }
            X500PrivateCredential[] privateCredentials = (X500PrivateCredential[]) AccessController.doPrivileged(new SubjectCredentials.GetAllPrivateCredentialsAction(serverSubject));
            List certPaths = SubjectCredentials.getCertificateChains(serverSubject);
            if (certPaths != null) {
                for (int i = certPaths.size(); --i >= 0; ) {
                    CertPath chain = (CertPath) certPaths.get(i);
                    X509Certificate firstCert = firstX509Cert(chain);
                    X500Principal p = firstCert.getSubjectX500Principal();
                    if (progress.containsKey(p)) {
                        try {
                            checkValidity(chain, null);
                        } catch (CertificateException e) {
                            progress.put(p, e);
                            continue;
                        }
                        progress.put(p, CertPath.class);
                        for (int j = privateCredentials.length; --j >= 0; ) {
                            X509Certificate cert = privateCredentials[j].getCertificate();
                            if (firstCert.equals(cert)) {
                                progress.remove(p);
                                break;
                            }
                        }
                    }
                }
            }
            if (!progress.isEmpty()) {
                X500Principal p = (X500Principal) progress.keySet().iterator().next();
                Object result = progress.get(p);
                if (result == X500Principal.class) {
                    throw new UnsupportedConstraintException("Missing public credentials: " + p);
                } else if (result == CertPath.class) {
                    throw new UnsupportedConstraintException("Missing private credentials: " + p);
                } else {
                    throw new UnsupportedConstraintException("Problem with certificates: " + p + "\n" + result, (CertificateException) result);
                }
            }
        }

        /**
		 * Creates a listen handle for the specified dispatcher and server
		 * socket.
		 */
        ListenHandle createListenHandle(RequestDispatcher requestDispatcher, ServerSocket serverSocket) throws IOException {
            return new SslListenHandle(requestDispatcher, serverSocket);
        }

        /** Returns a hash code value for this object. */
        public int hashCode() {
            return getClass().hashCode() ^ System.identityHashCode(serverSubject) ^ (serverPrincipals == null ? 0 : serverPrincipals.hashCode()) ^ port ^ (serverSocketFactory != null ? serverSocketFactory.hashCode() : 0);
        }

        /**
		 * Two instances of this class are equal if they have the same actual
		 * class; have server subjects that compare equal using <code>==</code>;
		 * have server principals that are either both <code>null</code> or
		 * compare equal using <code>equals</code>; have the same port; and have
		 * server socket factories that are both null, or have the same actual
		 * class and are equal. Note that the server host and socket factory are
		 * ignored.
		 */
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object == null || getClass() != object.getClass()) {
                return false;
            }
            SslServerEndpointImpl other = ((SslListenEndpoint) object).getImpl();
            return serverSubject == other.serverSubject && safeEquals(serverPrincipals, other.serverPrincipals) && port == other.port && Util.sameClassAndEquals(serverSocketFactory, other.serverSocketFactory);
        }

        /**
		 * Returns the SslServerEndpointImpl associated with this listen
		 * endpoint.
		 */
        private SslServerEndpointImpl getImpl() {
            return SslServerEndpointImpl.this;
        }
    }

    /** Implements ListenHandle */
    class SslListenHandle extends Utilities implements ListenHandle {

        /** The request handler */
        private final RequestDispatcher requestDispatcher;

        /** The server socket used to accept connections */
        final ServerSocket serverSocket;

        /** The security context at the time of the listen. */
        private final SecurityContext securityContext;

        /** Whether the listen handle has been closed. */
        private boolean closed = false;

        /** Set of connections created by this listen handle */
        private final Set connections = new HashSet();

        /** Used to throttle accept failures */
        private long acceptFailureTime = 0;

        private int acceptFailureCount;

        /** Creates a listen handle */
        SslListenHandle(RequestDispatcher requestDispatcher, ServerSocket serverSocket) throws IOException {
            this.requestDispatcher = requestDispatcher;
            this.serverSocket = serverSocket;
            securityContext = Security.getContext();
            systemExecutor.execute(new Runnable() {

                public void run() {
                    acceptLoop();
                }
            }, toString());
            logger.log(Level.FINE, "created {0}", this);
        }

        /** Handles new socket connections. */
        final void acceptLoop() {
            while (true) {
                Socket socket = null;
                Throwable exception;
                SslServerConnection connection = null;
                try {
                    socket = serverSocket.accept();
                    try {
                        socket.setTcpNoDelay(true);
                    } catch (SocketException e) {
                    }
                    try {
                        socket.setKeepAlive(true);
                    } catch (SocketException e) {
                    }
                    connection = serverConnection(socket);
                    synchronized (this) {
                        if (closed) {
                            try {
                                connection.closeInternal(false);
                            } catch (IOException e) {
                            }
                            break;
                        }
                        connections.add(connection);
                    }
                    final SslServerConnection finalConnection = connection;
                    AccessController.doPrivileged(securityContext.wrap(new PrivilegedAction() {

                        public Object run() {
                            handleConnection(finalConnection, requestDispatcher);
                            return null;
                        }
                    }), securityContext.getAccessControlContext());
                    continue;
                } catch (Exception e) {
                    exception = e;
                } catch (Error e) {
                    exception = e;
                }
                boolean closedSync;
                synchronized (this) {
                    closedSync = closed;
                }
                if (!closedSync && logger.isLoggable(Level.INFO)) {
                    String msg = "handling connection {0} throws";
                    Object arg = connection;
                    if (connection == null) {
                        msg = "accepting connection on {0} throws";
                        arg = this;
                    }
                    logThrow(logger, Level.INFO, SslListenHandle.class, "acceptLoop", msg, new Object[] { arg }, exception);
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }
                if (closedSync) {
                    break;
                }
                boolean knownFailure = (exception instanceof Exception || exception instanceof OutOfMemoryError || exception instanceof NoClassDefFoundError);
                if (!(knownFailure && continueAfterAcceptFailure(exception))) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                    }
                    if (!knownFailure) {
                        throw (Error) exception;
                    } else {
                        return;
                    }
                }
            }
        }

        /**
		 * Throttles the accept loop after ServerSocket.accept throws an
		 * exception, and decides whether to continue at all. The current code
		 * is borrowed from the JRMP implementation; it always continues, but it
		 * delays the loop after bursts of failed accepts.
		 */
        private boolean continueAfterAcceptFailure(Throwable t) {
            final int NFAIL = 10;
            final int NMSEC = 5000;
            long now = System.currentTimeMillis();
            if (acceptFailureTime == 0L || (now - acceptFailureTime) > NMSEC) {
                acceptFailureTime = now;
                acceptFailureCount = 0;
            } else {
                acceptFailureCount++;
                if (acceptFailureCount >= NFAIL) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            return true;
        }

        /** Returns a string representation of this object. */
        public String toString() {
            return getClassName(this) + "[" + serverHost + ":" + getPort() + "]";
        }

        /** Returns a connection for the specified socket. */
        SslServerConnection serverConnection(Socket socket) throws IOException {
            return new SslServerConnection(this, socket);
        }

        /** Handles a newly accepted server connection. */
        void handleConnection(SslServerConnection connection, RequestDispatcher requestDispatcher) {
            serverConnectionManager.handleConnection(connection, requestDispatcher);
        }

        /** Returns the port on which this handle is listening. */
        private int getPort() {
            return serverSocket.getLocalPort();
        }

        public synchronized void close() {
            if (!closed) {
                logger.log(Level.FINE, "closing {0}", this);
                closed = true;
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
                for (Iterator i = connections.iterator(); i.hasNext(); ) {
                    SslServerConnection connection = (SslServerConnection) i.next();
                    try {
                        connection.closeInternal(false);
                    } catch (IOException e) {
                    }
                    i.remove();
                }
            }
        }

        /**
		 * Called when a connection is closed without a call to close on this
		 * listener.
		 */
        synchronized void noteConnectionClosed(SslServerConnection connection) {
            connections.remove(connection);
        }

        public ListenCookie getCookie() {
            return new SslListenCookie(getPort());
        }
    }

    /** Implements ListenCookie */
    final class SslListenCookie implements ListenCookie {

        private final int port;

        SslListenCookie(int port) {
            this.port = port;
        }

        /** Returns the port on which the associated handle is listening. */
        final int getPort() {
            return port;
        }

        /**
		 * Returns the listen endpoint associated with this listen cookie.
		 */
        final ListenEndpoint getListenEndpoint() {
            return listenEndpoint;
        }
    }

    /** Implements ServerConnection */
    class SslServerConnection extends Utilities implements ServerConnection {

        /** The listen handle that accepted this connection */
        private final SslListenHandle listenHandle;

        /** The JSSE socket used for communication */
        final SSLSocket sslSocket;

        /** The inbound request handle for this connection. */
        private final InboundRequestHandle requestHandle = new InboundRequestHandle() {
        };

        /**
		 * The session for this connection's socket, or null if not retrieved
		 * yet. Check that the current session matches to prevent new
		 * handshakes.
		 */
        private SSLSession session;

        /**
		 * The client subject -- depends on session being set. This instance is
		 * read-only.
		 */
        private Subject clientSubject;

        /** The client principal -- depends on session being set. */
        private X500Principal clientPrincipal;

        /** The server principal -- depends on session being set. */
        private X500Principal serverPrincipal;

        /**
		 * The authentication permission required for this connection, or null
		 * if the server is anonymous -- depends on session being set.
		 */
        private AuthenticationPermission authPermission;

        /** The cipher suite -- depends on session being set. */
        private String cipherSuite;

        /** True if the connection has been closed. */
        boolean closed;

        /** Creates a server connection */
        SslServerConnection(SslListenHandle listenHandle, Socket socket) throws IOException {
            this.listenHandle = listenHandle;
            sslSocket = (SSLSocket) getSSLSocketFactory().createSocket(socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
            sslSocket.setEnabledCipherSuites(getSupportedCipherSuites());
            sslSocket.setUseClientMode(false);
            sslSocket.setWantClientAuth(true);
            logger.log(Level.FINE, "created {0}", this);
        }

        public String toString() {
            String sessionString;
            synchronized (this) {
                sessionString = session == null ? "" : session + ", ";
            }
            return getClassName(this) + "[" + sessionString + serverHost + ":" + sslSocket.getLocalPort() + "<=" + sslSocket.getInetAddress().getHostName() + ":" + sslSocket.getPort() + "]";
        }

        public InputStream getInputStream() throws IOException {
            return sslSocket.getInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return sslSocket.getOutputStream();
        }

        public SocketChannel getChannel() {
            return null;
        }

        public InboundRequestHandle processRequestData(InputStream in, OutputStream out) {
            if (in == null || out == null) {
                throw new NullPointerException("Arguments cannot be null");
            }
            SecurityException exception;
            try {
                long now = System.currentTimeMillis();
                decacheSession();
                long create = session.getCreationTime();
                long expiration = create + maxServerSessionDuration;
                if (expiration < create) {
                    expiration = Long.MAX_VALUE;
                }
                if (expiration < now) {
                    session.invalidate();
                    throw new SecurityException("Session has expired");
                }
                if (serverPrincipal != null) {
                    getAuthManager().checkCredentials(session, clientSubject);
                }
                return requestHandle;
            } catch (SecurityException e) {
                exception = e;
            } catch (GeneralSecurityException e) {
                exception = new SecurityException(e.getMessage());
                exception.initCause(e);
            }
            try {
                out.close();
            } catch (IOException e2) {
            }
            if (logger.isLoggable(Levels.FAILED)) {
                logThrow(logger, Levels.FAILED, SslServerConnection.class, "processRequestData", "process request data for session {0}\nclient {1}\n" + "throws", new Object[] { session, subjectString(clientSubject) }, exception);
            }
            throw exception;
        }

        /**
		 * Make sure the cached session is up to date, and set session-related
		 * fields if needed.
		 */
        private void decacheSession() {
            synchronized (this) {
                SSLSession socketSession = sslSocket.getSession();
                if (session == socketSession) {
                    return;
                } else if (session != null) {
                    throw new SecurityException("New handshake occurred on socket");
                }
                session = socketSession;
                sslSocket.setEnableSessionCreation(false);
                cipherSuite = session.getCipherSuite();
                if ("NULL".equals(getKeyExchangeAlgorithm(cipherSuite))) {
                    throw new SecurityException("Handshake failed");
                }
                clientSubject = getClientSubject(sslSocket);
                clientPrincipal = clientSubject != null ? ((X500Principal) clientSubject.getPrincipals().iterator().next()) : null;
                X509Certificate serverCert = getAuthManager().getServerCertificate(session);
                serverPrincipal = serverCert != null ? serverCert.getSubjectX500Principal() : null;
                if (serverPrincipal != null) {
                    authPermission = new AuthenticationPermission(Collections.singleton(serverPrincipal), (clientPrincipal != null ? Collections.singleton(clientPrincipal) : null), "accept");
                }
            }
        }

        /**
		 * Returns the read-only <code>Subject</code> associated with the client
		 * host connected to the other end of the connection on the specified
		 * <code>SSLSocket</code>. Returns null if the client is anonymous.
		 */
        private Subject getClientSubject(SSLSocket socket) {
            SSLSession session = socket.getSession();
            try {
                Certificate[] certificateChain = session.getPeerCertificates();
                if (certificateChain != null && certificateChain.length > 0 && certificateChain[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certificateChain[0];
                    return new Subject(true, Collections.singleton(cert.getSubjectX500Principal()), Collections.singleton(getCertFactory().generateCertPath(Arrays.asList(certificateChain))), Collections.EMPTY_SET);
                }
            } catch (SSLPeerUnverifiedException e) {
            } catch (CertificateException e) {
                logger.log(Levels.HANDLED, "get client subject caught exception", e);
            }
            return null;
        }

        public void checkPermissions(InboundRequestHandle requestHandle) {
            check(requestHandle);
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkAccept(sslSocket.getInetAddress().getHostAddress(), sslSocket.getPort());
                    if (authPermission != null) {
                        sm.checkPermission(authPermission);
                    }
                } catch (SecurityException e) {
                    if (logger.isLoggable(Levels.FAILED)) {
                        logThrow(logger, Levels.FAILED, SslServerConnection.class, "checkPermissions", "check permissions for {0} throws", new Object[] { this }, e);
                    }
                    throw e;
                }
            }
        }

        /**
		 * Checks that the argument is the request handle for this connection.
		 */
        private void check(InboundRequestHandle requestHandle) {
            if (requestHandle == null) {
                throw new NullPointerException("Request handle cannot be null");
            } else if (requestHandle != this.requestHandle) {
                throw new IllegalArgumentException("Wrong request handle: found " + requestHandle + ", expected " + this.requestHandle);
            }
        }

        public InvocationConstraints checkConstraints(InboundRequestHandle requestHandle, InvocationConstraints constraints) throws UnsupportedConstraintException {
            check(requestHandle);
            if (constraints == null) {
                throw new NullPointerException("Constraints cannot be null");
            }
            InvocationConstraints result = getUnfulfilledConstraints(cipherSuite, clientPrincipal, serverPrincipal, constraints);
            if (result == null) {
                UnsupportedConstraintException uce = new UnsupportedConstraintException("Constraints are not supported: " + constraints);
                if (logger.isLoggable(Levels.FAILED)) {
                    logThrow(logger, Levels.FAILED, SslServerConnection.class, "checkConstraints", "check constraints for {0}\nwith {1}\n" + "throws", new Object[] { SslServerConnection.this, constraints }, uce);
                }
                throw uce;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "check constraints for {0}\nwith {1}\nreturns {2}", new Object[] { SslServerConnection.this, constraints, result });
            }
            return result;
        }

        public void populateContext(InboundRequestHandle requestHandle, Collection context) {
            check(requestHandle);
            Util.populateContext(context, sslSocket.getInetAddress());
            Util.populateContext(context, clientSubject);
        }

        public void close() throws IOException {
            closeInternal(true);
        }

        /**
		 * Like close, but does not call noteConnectionClosed unless
		 * removeFromListener is true.
		 */
        void closeInternal(boolean removeFromListener) throws IOException {
            synchronized (this) {
                if (closed) {
                    return;
                }
                logger.log(Level.FINE, "closing {0}", this);
                closed = true;
                sslSocket.close();
            }
            if (removeFromListener) {
                listenHandle.noteConnectionClosed(this);
            }
        }
    }
}
