package jaxlib.prmi;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import jaxlib.arc.zip.DeflatedOutputStream;
import jaxlib.arc.zip.DeflaterFlushMode;
import jaxlib.arc.zip.DeflaterProperties;
import jaxlib.arc.zip.DeflateStrategy;
import jaxlib.arc.zip.InflatedInputStream;
import jaxlib.beans.DefaultDynamicBeanParameters;
import jaxlib.beans.DynamicBeanInvocationHandler;
import jaxlib.beans.DynamicBeanParameters;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.pds.PDSInputStream;
import jaxlib.io.stream.pds.PDSOutputStream;
import jaxlib.io.stream.pds.PDSType;
import jaxlib.lang.StackTraces;
import jaxlib.logging.XLogger;
import jaxlib.net.socket.SocketServerConnection;
import jaxlib.security.auth.SocketCredential;
import jaxlib.security.auth.login.LoginContextFactory;
import jaxlib.util.CheckArg;
import jaxlib.util.Strings;

/**
 * An instance of this class handles a single connection incoming to a PRMI server.
 * <p>
 * The connection is partially non-blocking if it has been constructed with a non-blocking
 * {@link SocketServerConnection}: If a request has been handled and there is not at least one byte
 * immediately available in the stream, then the connection instance will return control to the 
 * {@link PRMIServerConnectionHandler} which in turn returns control to the the 
 * {@link jaxlib.net.socket.SocketServer}.<br/>
 * The connection blocks while the current request or the response packet have not been completely
 * transmitted.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: PRMIServerConnection.java 1587 2006-01-27 00:16:05Z joerg_wassmer $
 */
public class PRMIServerConnection extends Object implements Callable<Object>, Closeable {

    private static final boolean DEBUG = false;

    private static InputStream createSocketInputStream(Socket socket, int bufferSize) throws IOException {
        CheckArg.notNegative(bufferSize, "bufferSize");
        SocketChannel ch = socket.getChannel();
        if ((ch != null) && !ch.isBlocking()) ch.configureBlocking(true);
        InputStream in = socket.getInputStream();
        return (bufferSize > 0) ? new BufferedXInputStream(in, bufferSize) : in;
    }

    private static OutputStream createSocketOutputStream(Socket socket) throws IOException {
        SocketChannel ch = socket.getChannel();
        if (ch != null) ch.configureBlocking(true);
        OutputStream out = socket.getOutputStream();
        return out;
    }

    private boolean authenticated = false;

    private final boolean blocking;

    private boolean deflated = false;

    private InputStream in;

    private InputStream originalIn;

    private OutputStream out;

    private PDSOutputStream.Output pdsOutput;

    private Socket socket;

    private SocketServerConnection socketConnection;

    private Locale locale;

    private LoginContext loginContext;

    private LoginContextFactory loginContextFactory;

    private Subject subject;

    private DynamicBeanInvocationHandler invocationHandler;

    private final Logger logger;

    /**
   * Handled by PRMItoJMXConnectorServer
   */
    volatile String jmxConnectionId;

    private SocketAddress localSocketAddress;

    private SocketAddress remoteSocketAddress;

    public PRMIServerConnection(InputStream in, OutputStream out, LoginContextFactory loginContextFactory, DynamicBeanInvocationHandler invocationHandler, int outputBufferSize, int maxOutputBufferSize, Logger logger) {
        super();
        CheckArg.notNull(in, "in");
        CheckArg.notNull(out, "out");
        CheckArg.notNull(invocationHandler, "invocationHandler");
        if (logger == null) logger = XLogger.getLogger("net");
        this.blocking = true;
        this.in = in;
        this.invocationHandler = invocationHandler;
        this.logger = logger;
        this.loginContextFactory = loginContextFactory;
        this.originalIn = in;
        this.out = out;
        this.pdsOutput = new PDSOutputStream.Output(out, outputBufferSize, maxOutputBufferSize);
    }

    public PRMIServerConnection(Socket socket, LoginContextFactory loginContextFactory, DynamicBeanInvocationHandler invocationHandler, int inputBufferSize, int outputBufferSize, int maxOutputBufferSize, Logger logger) throws IOException {
        this(createSocketInputStream(socket, inputBufferSize), createSocketOutputStream(socket), loginContextFactory, invocationHandler, outputBufferSize, maxOutputBufferSize, logger);
        this.socket = socket;
        this.localSocketAddress = socket.getLocalSocketAddress();
        this.remoteSocketAddress = socket.getRemoteSocketAddress();
    }

    public PRMIServerConnection(SocketChannel socketChannel, LoginContextFactory loginContextFactory, DynamicBeanInvocationHandler invocationHandler, int inputBufferSize, int outputBufferSize, int maxOutputBufferSize, Logger logger) throws IOException {
        this(socketChannel.socket(), loginContextFactory, invocationHandler, inputBufferSize, outputBufferSize, maxOutputBufferSize, logger);
    }

    public PRMIServerConnection(SocketServerConnection connection, LoginContextFactory loginContextFactory, DynamicBeanInvocationHandler invocationHandler, int inputBufferSize, int outputBufferSize, int maxOutputBufferSize, Logger logger) throws IOException {
        super();
        CheckArg.notNull(connection, "connection");
        CheckArg.notNull(invocationHandler, "invocationHandler");
        if (logger == null) logger = XLogger.getLogger("net");
        this.blocking = connection.isBlocking();
        this.invocationHandler = invocationHandler;
        this.localSocketAddress = connection.getInfo().getLocalAddress();
        this.remoteSocketAddress = connection.getInfo().getRemoteAddress();
        this.logger = logger;
        this.loginContextFactory = loginContextFactory;
        this.originalIn = connection.getInputStream();
        this.out = connection.getOutputStream();
        this.socket = connection.getUnderlyingSocketChannel().socket();
        this.socketConnection = connection;
        this.in = (inputBufferSize > 0) ? new BufferedXInputStream(this.originalIn, inputBufferSize) : this.originalIn;
        this.pdsOutput = new PDSOutputStream.Output(this.out, outputBufferSize, maxOutputBufferSize);
    }

    protected LoginContext createLoginContext(final String loginName, final String keyAlgoName, final Object key) throws IOException, LoginException {
        LoginContextFactory loginContextFactory = this.loginContextFactory;
        if (loginContextFactory == null) {
            if (DEBUG) this.logger.finest("no LoginContextFactory");
            return null;
        }
        if (DEBUG) this.logger.finest("enter");
        char[] password = null;
        if (key != null) {
            if ("plain".equals(keyAlgoName)) password = ((String) key).toCharArray(); else throw new IOException("Unsupported encryption algorithm: " + keyAlgoName);
        }
        PRMIServerConnection.CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl(loginName, password);
        return this.loginContextFactory.createLoginContext(null, callbackHandler);
    }

    public SocketAddress getRemoteSocketAddress() {
        Socket socket = this.socket;
        return (socket == null) ? null : socket.getRemoteSocketAddress();
    }

    private boolean checkEOFAndClose(PDSInputStream in) throws IOException {
        if (in.eof()) {
            in.closeInstance();
            return true;
        } else {
            in.closeInstance();
            return false;
        }
    }

    private PDSOutputStream createOutput() throws IOException {
        final PDSOutputStream.Output out = this.pdsOutput;
        if (out != null) return new PDSOutputStream(out); else return null;
    }

    private boolean processRequest() throws IOException, LoginException, Exception {
        InputStream rawIn = this.in;
        if (rawIn == null) return false;
        final PDSInputStream in;
        try {
            in = PDSInputStream.open(rawIn);
        } catch (SocketTimeoutException ex) {
            if (DEBUG) this.logger.log(Level.FINER, "time out", ex);
            ex = null;
            rawIn = null;
            close();
            return false;
        } catch (IOException ex) {
            if (DEBUG) this.logger.log(Level.FINER, "exception", ex);
            Socket socket = this.socket;
            if ((socket == null) || !socket.isClosed()) {
                throw ex;
            } else {
                ex = null;
                rawIn = null;
                close();
                return false;
            }
        }
        if (in == null) {
            rawIn = null;
            close();
            return false;
        } else {
            final int firstByte = in.readUInt8();
            if (this.authenticated) {
                switch(firstByte) {
                    case PRMI.LOGIN_REQUEST:
                        return processLoginRequest(in);
                    case PRMI.LOGOUT_REQUEST:
                        return processLogoutRequest(in);
                    case PRMI.KEEPALIVE_REQUEST:
                        return processKeepAliveRequest(in);
                    case PRMI.GETATTRIBUTE_REQUEST:
                        return processGetAttributeRequest(in);
                    case PRMI.SETATTRIBUTE_REQUEST:
                        return processSetAttributeRequest(in);
                    case PRMI.METHODCALL_REQUEST:
                        return processMethodCallRequest(in);
                    case PRMI.DEFLATE_REQUEST:
                        return processDeflateRequest(in);
                    default:
                        {
                            return processIllegalRequest(in, StackTraces.toString(new IOException("Illegal request type: " + Integer.toString(firstByte))));
                        }
                }
            } else {
                switch(firstByte) {
                    case PRMI.LOGIN_REQUEST:
                        return processLoginRequest(in);
                    case PRMI.DEFLATE_REQUEST:
                        return processDeflateRequest(in);
                    default:
                        {
                            return processIllegalRequest(in, StackTraces.toString(new IOException("Expected login or deflate request but got: " + Integer.toString(firstByte))));
                        }
                }
            }
        }
    }

    private boolean processDeflateRequest(PDSInputStream in) throws IOException {
        int compressionLevel = in.readUInt8();
        if (checkEOFAndClose(in)) {
            handleDeflateRequest(compressionLevel);
            return true;
        } else {
            handleIllegalRequest(null);
            return false;
        }
    }

    private boolean processGetAttributeRequest(PDSInputStream in) throws IOException {
        try {
            final String objectName = in.readString();
            final String attributeName = in.readString();
            checkEOFAndClose(in);
            handleGetAttributeRequest(objectName, attributeName);
            return true;
        } catch (IOException ex) {
            if (DEBUG) logger.log(Level.FINEST, "exception", ex);
            throw ex;
        } catch (Exception ex) {
            if (DEBUG) logger.log(Level.FINEST, "exception", ex);
            return false;
        }
    }

    private boolean processIllegalRequest(PDSInputStream in, String msg) throws IOException {
        if (DEBUG) logger.finest("enter");
        in.closeInstance();
        handleIllegalRequest(msg);
        return false;
    }

    private boolean processKeepAliveRequest(PDSInputStream in) throws IOException {
        if (DEBUG) logger.finest("enter");
        if (checkEOFAndClose(in)) {
            handleKeepAliveRequest();
            return true;
        } else {
            handleIllegalRequest(null);
            return false;
        }
    }

    private boolean processLoginRequest(PDSInputStream in) throws IOException, LoginException {
        if (DEBUG) logger.finest("enter");
        PDSOutputStream.Output pdsOutput = this.pdsOutput;
        if (pdsOutput == null) throw new AsynchronousCloseException();
        pdsOutput.setShiftTimeZone(in.isShiftTimeZone());
        String username = in.readString();
        String language = in.readString();
        String country = in.readString();
        String encryptionAlgo = in.readString();
        Object key = ((encryptionAlgo == null) || "plain".equals(encryptionAlgo)) ? in.readString() : in.readInt8Array();
        if (DEBUG) {
            logger.finest("username=" + username + "; country=" + country + "; language=" + language + "; algo=" + encryptionAlgo + "; key=" + key);
        }
        boolean eof = in.eof();
        if (DEBUG) logger.finest("closing in");
        in.closeInstance();
        if (DEBUG) logger.finest("in closed");
        if (false && !eof) {
            username = null;
            language = null;
            country = null;
            encryptionAlgo = null;
            key = null;
            if (DEBUG) logger.finest("illegal request");
            handleIllegalRequest(null);
            return false;
        } else {
            if (Strings.isBlank(username)) username = null;
            if (Strings.isBlank(encryptionAlgo)) encryptionAlgo = null;
            if (Strings.isBlank(language)) language = null;
            if (Strings.isBlank(country)) country = null;
            Locale defaultLocale = Locale.getDefault();
            if ((language == null) && (country == null)) {
                this.locale = defaultLocale;
            } else {
                this.locale = new Locale((language == null) ? defaultLocale.getLanguage() : language, (country == null) ? defaultLocale.getCountry() : country);
            }
            if (DEBUG) logger.finest("handling...");
            handleLoginRequest(username, country, language, encryptionAlgo, key);
            return true;
        }
    }

    private boolean processLogoutRequest(PDSInputStream in) throws IOException {
        if (DEBUG) logger.finest("enter");
        if (checkEOFAndClose(in)) handleLogoutRequest(); else handleIllegalRequest(null);
        return false;
    }

    private boolean processMethodCallRequest(PDSInputStream in) throws Exception {
        if (DEBUG) logger.finest("enter");
        final String objectName = in.readString();
        final String operationName = in.readString();
        PRMIServerConnection.DynamicBeanParametersImpl params = new DynamicBeanParametersImpl(this, in, this.localSocketAddress, this.remoteSocketAddress);
        boolean ok = false;
        try {
            handleMethodCallRequest(objectName, operationName, params);
            ok = true;
            return true;
        } finally {
            if (DEBUG) logger.finest("exit");
            try {
                params.close();
                checkEOFAndClose(in);
            } catch (Exception ex) {
                if (ok) throw ex;
            }
        }
    }

    private boolean processSetAttributeRequest(PDSInputStream in) throws IOException {
        if (DEBUG) logger.finest("enter");
        try {
            String objectName = in.readString();
            String attributeName = in.readString();
            Object value = in.read();
            if (!(value instanceof PDSInputStream.Table) && !(value instanceof InputStream)) checkEOFAndClose(in);
            handleSetAttributeRequest(objectName, attributeName, value);
            objectName = null;
            attributeName = null;
            value = null;
            checkEOFAndClose(in);
            return true;
        } catch (IOException ex) {
            if (DEBUG) logger.log(Level.FINEST, "exception", ex);
            throw ex;
        } catch (Exception ex) {
            if (DEBUG) logger.log(Level.FINEST, "exception", ex);
            return false;
        }
    }

    private void runConnectionLoop() throws Exception {
        final boolean authenticated = this.authenticated;
        while (authenticated == this.authenticated) {
            if (processRequest()) {
                PDSOutputStream.Output out = this.pdsOutput;
                if (out != null) {
                    out.flush();
                    out = null;
                    if (this.blocking) {
                        continue;
                    } else {
                        InputStream in = this.in;
                        if (in != null) {
                            final int available = (!this.deflated && (in instanceof BufferedXInputStream)) ? ((BufferedXInputStream) in).bufferSize() : in.available();
                            if (available > 0) continue; else return;
                        } else {
                            this.authenticated = false;
                            close();
                            return;
                        }
                    }
                } else {
                    this.authenticated = false;
                    close();
                    return;
                }
            } else {
                PDSOutputStream.Output out = this.pdsOutput;
                if (out != null) {
                    out.flush();
                    out = null;
                }
                if (!this.authenticated) close();
                return;
            }
        }
        if (!this.authenticated) close();
    }

    private void runConnectionLoopAsSubject() throws Exception {
        Subject subject = this.subject;
        if (subject != null) {
            try {
                Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {

                    public Object run() throws Exception {
                        PRMIServerConnection.this.runConnectionLoop();
                        return null;
                    }
                });
            } catch (PrivilegedActionException ex) {
                throw ex.getException();
            }
        } else {
            throw new AsynchronousCloseException();
        }
    }

    /**
   * Writes a response packet which consists of a single boolean value.
   */
    private void writeBooleanResponse(final int responseCode, final boolean v) throws IOException {
        final PDSOutputStream out = createOutput();
        out.writeInt8(responseCode);
        out.writeBoolean(v);
        out.closeInstance();
    }

    /**
   * Writes a response packet which contains no data.
   */
    private void writeEmptyResponse(final int responseCode) throws IOException {
        final PDSOutputStream out = createOutput();
        out.writeInt8(responseCode);
        out.closeInstance();
    }

    /**
   * Writes a ERROR_RESPONSE packet.
   */
    private void writeErrorResponse(String msg) throws IOException {
        final PDSOutputStream out = createOutput();
        out.writeInt8(PRMI.ERROR_RESPONSE);
        out.writeUTFArray(msg);
        out.closeInstance();
    }

    protected void handleDeflateRequest(int compressionLevel) throws IOException {
        if (compressionLevel > 0) {
            InputStream in = this.in;
            InputStream originalIn = this.originalIn;
            if ((in == null) || (originalIn == null)) throw new AsynchronousCloseException();
            final boolean deflateSupported = !this.deflated && (in != originalIn) && (in instanceof BufferedXInputStream);
            writeBooleanResponse(PRMI.DEFLATE_RESPONSE, deflateSupported);
            if (deflateSupported) {
                PDSOutputStream.Output pdsOut = this.pdsOutput;
                if (pdsOut == null) throw new AsynchronousCloseException();
                final int bufferCapacity = pdsOut.bufferCapacity();
                final int maxBufferCapacity = pdsOut.getMaxBufferCapacity();
                OutputStream socketOut = this.out;
                if (socketOut == null) throw new AsynchronousCloseException();
                DeflaterProperties p = new DeflaterProperties();
                p.setCompressionLevel(compressionLevel);
                p.setFlushMode(DeflaterFlushMode.SYNC);
                DeflatedOutputStream zout = new DeflatedOutputStream(socketOut, p);
                pdsOut.setOut(zout, true, false);
                BufferedXInputStream xin = (BufferedXInputStream) in;
                InflatedInputStream zin = new InflatedInputStream(originalIn, 8192, false);
                xin.setIn(zin, false, false);
                this.deflated = true;
            }
        }
    }

    protected void handleGetAttributeRequest(final String objectName, final String attributeName) throws Exception {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Strings.concat("objectName    = ", objectName, "\nattributeName = ", attributeName));
        }
        DynamicBeanInvocationHandler invocationHandler = this.invocationHandler;
        if (invocationHandler != null) {
            Object v;
            try {
                if (DEBUG) logger.finest("calling invocation handler");
                v = invocationHandler.getAttribute(objectName, attributeName);
                if (DEBUG) logger.finest("calling invocation handler success");
            } catch (Throwable ex) {
                try {
                    writeErrorResponse(StackTraces.toString(ex));
                } finally {
                    if (ex instanceof Error) throw (Error) ex; else if (ex instanceof Exception) throw (Exception) ex; else throw new RuntimeException(ex);
                }
            }
            final PDSOutputStream out = createOutput();
            out.writeInt8(PRMI.GETATTRIBUTE_RESPONSE);
            out.writeValue(v);
            out.closeInstance();
        } else {
            if (DEBUG) logger.finest("no invocation handler");
        }
    }

    protected void handleIllegalRequest(String msg) throws IOException {
        if (DEBUG) logger.finest("");
        writeErrorResponse((msg == null) ? StackTraces.toString(new IOException("Illegal request")) : msg);
        close();
    }

    protected void handleKeepAliveRequest() throws IOException {
        if (this.authenticated) writeEmptyResponse(PRMI.KEEPALIVE_RESPONSE); else handleIllegalRequest(null);
    }

    protected void handleLoginRequest(final String username, String country, String language, final String keyEncryptionAlgoName, final Object key) throws IOException, LoginException {
        if (this.logger.isLoggable(Level.FINE)) this.logger.fine(Strings.concat("userName = ", username, ", algo = ", keyEncryptionAlgoName));
        if (this.authenticated) {
            if (DEBUG) logger.finest("already authenticated");
            handleIllegalRequest("Already logged in");
        } else {
            Locale defaultLocale = Locale.getDefault();
            if ((country == null) && (language == null)) {
                this.locale = defaultLocale;
            } else {
                if (country == null) country = defaultLocale.getCountry();
                if (language == null) language = defaultLocale.getLanguage();
                this.locale = new Locale(country, language);
            }
            if (DEBUG) logger.finest("authenticating...");
            int result = PRMI.LOGIN_OK;
            Exception ex = null;
            try {
                LoginContext loginContext = createLoginContext(username, keyEncryptionAlgoName, key);
                this.loginContext = loginContext;
                if (DEBUG) logger.finest("LoginContext: " + loginContext);
                if (loginContext == null) {
                    result = PRMI.LOGIN_OK;
                } else {
                    Subject subject = new Subject();
                    Socket socket = this.socket;
                    if (socket != null) subject.getPublicCredentials().add(new SocketCredential(socket));
                    loginContext.login();
                    this.subject = loginContext.getSubject();
                    result = PRMI.LOGIN_OK;
                }
                this.authenticated = true;
            } catch (FailedLoginException e) {
                result = PRMI.LOGIN_UNKNOWN_USER;
                ex = e;
                throw e;
            } catch (AccountExpiredException e) {
                result = PRMI.LOGIN_ACCOUNT_EXPIRED;
                ex = e;
                throw e;
            } catch (AccountLockedException e) {
                result = PRMI.LOGIN_ACCOUNT_LOCKED;
                ex = e;
                throw e;
            } catch (LoginException e) {
                result = PRMI.LOGIN_ERROR;
                ex = e;
                throw e;
            } catch (SecurityException e) {
                result = PRMI.LOGIN_ERROR;
                ex = e;
                throw e;
            } finally {
                if (DEBUG) logger.log(Level.FINEST, "login result: " + result, ex);
                String msg = (ex == null) ? null : StackTraces.toString(ex);
                this.authenticated = result == PRMI.LOGIN_OK;
                PDSOutputStream out = createOutput();
                out.writeInt8(PRMI.LOGIN_RESPONSE);
                out.writeInt8(result);
                out.writeUTFArray(msg);
                out.closeInstance();
            }
        }
    }

    protected void handleLogoutRequest() throws IOException {
        if (this.authenticated) {
            this.authenticated = false;
            this.subject = null;
            writeEmptyResponse(PRMI.LOGOUT_RESPONSE);
        } else {
            handleIllegalRequest(null);
        }
    }

    protected void handleMethodCallRequest(String objectName, String operationName, DynamicBeanParametersImpl parameters) throws Exception {
        if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(Strings.concat("objectName    = ", objectName, "\noperationName = ", operationName));
        }
        DynamicBeanInvocationHandler invocationHandler = this.invocationHandler;
        if (invocationHandler != null) {
            Object result;
            try {
                if (DEBUG) logger.finest("calling invocation handler");
                result = invocationHandler.invoke(objectName, operationName, parameters);
                try {
                    if (result == null) parameters.getPDSResponse().writeVoid(); else parameters.getPDSResponse().writeValue(result);
                    if (DEBUG) logger.finest("calling invocation handler success");
                } catch (Exception ex) {
                    throw new IOException("Exception while writing result of method call:" + "\n  object name  = " + objectName + "\n  operation    = " + operationName + "\n  result       = " + result).initCause(ex);
                }
            } catch (Throwable ex) {
                logger.log(Level.WARNING, "Exception in method call", ex);
                try {
                    if (parameters.response == null) {
                        writeErrorResponse(StackTraces.toString(ex));
                    } else {
                        parameters.response.writeInt8(PRMI.ERROR_RESPONSE);
                        parameters.response.writeUTFArray(StackTraces.toString(ex));
                    }
                } finally {
                    if (ex instanceof Error) throw (Error) ex; else if (ex instanceof Exception) throw (Exception) ex; else throw new RuntimeException(ex);
                }
            }
        }
    }

    protected void handleSetAttributeRequest(final String objectName, final String attributeName, final Object value) throws Exception {
        if (this.logger.isLoggable(Level.FINE)) {
            this.logger.fine(Strings.concat("objectName    = ", objectName, "\nattributeName = ", attributeName, "\nvalue         = ", String.valueOf(value)));
        }
        DynamicBeanInvocationHandler invocationHandler = this.invocationHandler;
        if (invocationHandler != null) {
            try {
                if (DEBUG) this.logger.finest("calling invocation handler");
                invocationHandler.setAttribute(objectName, attributeName, value);
                if (DEBUG) this.logger.finest("calling invocation handler success");
            } catch (Throwable ex) {
                try {
                    writeErrorResponse(StackTraces.toString(ex));
                } finally {
                    if (ex instanceof Error) throw (Error) ex; else if (ex instanceof Exception) throw (Exception) ex; else throw new RuntimeException(ex);
                }
            }
            writeEmptyResponse(PRMI.SETATTRIBUTE_RESPONSE);
        } else {
            if (DEBUG) this.logger.finest("no invocation handler");
        }
    }

    /**
   * Handles either the full connection or all packet immediately available in the input strem, depending on 
   * the blocking mode of this connection instance.
   *
   * @see #setBlockingMode(boolean)
   */
    public synchronized Object call() throws IOException {
        SocketAddress remoteAddr = getRemoteSocketAddress();
        if ((remoteAddr != null) && this.logger.isLoggable(Level.FINE)) this.logger.fine("Beginning handling socket: ".concat(remoteAddr.toString())); else if (DEBUG) this.logger.finest("enter");
        Throwable ex = null;
        boolean closed = false;
        try {
            while (isOpen()) {
                if (this.subject == null) runConnectionLoop(); else runConnectionLoopAsSubject();
                if (!this.blocking) break;
            }
        } catch (Throwable ex2) {
            ex = ex2;
        } finally {
            if ((ex != null) || isBlocking()) {
                closed = true;
                try {
                    close();
                } catch (Throwable ex2) {
                    if (ex == null) ex = ex2;
                }
            }
        }
        if (ex == null) {
            if (closed && (remoteAddr != null) && this.logger.isLoggable(Level.FINE)) this.logger.log(Level.FINE, "Connection closed: ".concat(remoteAddr.toString()), ex); else if (DEBUG) this.logger.log(Level.FINEST, "releasing non-blocking connection");
            return null;
        } else {
            if ((remoteAddr != null) && this.logger.isLoggable(Level.FINE)) {
                this.logger.log(Level.FINE, "Connection closed because of exception: ".concat(remoteAddr.toString()), ex);
            } else if (DEBUG) {
                this.logger.log(Level.FINEST, "error", ex);
            }
            return null;
        }
    }

    public void close() throws IOException {
        if (DEBUG) logger.finest("enter");
        InputStream in = this.in;
        PDSOutputStream.Output pdsOut = this.pdsOutput;
        OutputStream out = this.out;
        Socket socket = this.socket;
        SocketServerConnection socketConnection = this.socketConnection;
        this.authenticated = false;
        this.in = null;
        this.invocationHandler = null;
        this.locale = null;
        this.loginContext = null;
        this.loginContextFactory = null;
        this.originalIn = null;
        this.out = null;
        this.pdsOutput = null;
        this.socket = null;
        this.socketConnection = null;
        this.subject = null;
        Exception ex = null;
        if (pdsOut != null) {
            try {
                pdsOut.close();
            } catch (Exception ex2) {
                if (ex == null) ex = ex2;
            }
            pdsOut = null;
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception ex2) {
                if (ex == null) ex = ex2;
            }
            out = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (Exception ex2) {
                if (ex == null) ex = ex2;
            }
            in = null;
        }
        if (socketConnection != null) {
            socket = null;
            try {
                socketConnection.close();
            } catch (Exception ex2) {
                if (ex == null) ex = ex2;
            }
            socketConnection = null;
        } else if (socket != null) {
            try {
                socket.close();
            } catch (Exception ex2) {
                if (ex == null) ex = ex2;
            }
            socket = null;
        }
        if (DEBUG) logger.log(Level.FINEST, "return", ex);
        if (ex != null) {
            if (ex instanceof IOException) throw (IOException) ex; else if (ex instanceof RuntimeException) throw (RuntimeException) ex; else throw new RuntimeException(ex);
        }
    }

    public Locale getLocale() {
        return this.locale;
    }

    public final Socket getSocket() {
        return this.socket;
    }

    public final boolean isBlocking() {
        return this.blocking;
    }

    public final boolean isOpen() {
        return this.in != null;
    }

    private static final class CallbackHandlerImpl extends Object implements CallbackHandler {

        private final String loginName;

        private final char[] password;

        CallbackHandlerImpl(String loginName, char[] password) {
            super();
            this.loginName = loginName;
            this.password = password;
        }

        public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
            Callback unsupported = null;
            for (Callback c : callbacks) {
                if (c instanceof PasswordCallback) ((PasswordCallback) c).setPassword(password); else if (c instanceof NameCallback) ((NameCallback) c).setName(loginName); else if (c != null) unsupported = c;
            }
            if (unsupported != null) throw new UnsupportedCallbackException(unsupported);
        }
    }

    /**
   * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
   * @since   JaXLib 1.0
   */
    private static final class DynamicBeanParametersImpl extends DefaultDynamicBeanParameters implements DynamicBeanParameters, Serializable {

        /**
     * @since JaXLib 1.0
     */
        private static final long serialVersionUID = 1L;

        private static final Object UNAVAILABLE = new Object();

        private static final Object UNINITIALIZED = new Object();

        private static final long VM_ID = System.nanoTime();

        private static final AtomicLong stackIdCounter = new AtomicLong(1);

        private static final ThreadLocal<ArrayList<DynamicBeanParametersImpl>> stack = new ThreadLocal<ArrayList<DynamicBeanParametersImpl>>();

        private transient PRMIServerConnection connection;

        private transient PDSInputStream in;

        private final transient SocketAddress localSocketAddress;

        private transient PDSType[] parameterTypes;

        private final transient SocketAddress remoteSocketAddress;

        private transient PDSOutputStream response;

        private transient Object lock = new Object();

        private transient long stackId;

        private transient Thread ownerThread;

        DynamicBeanParametersImpl(PRMIServerConnection connection, PDSInputStream in, SocketAddress localSocketAddress, SocketAddress remoteSocketAddress) throws IOException {
            super();
            PDSType[] parameterTypes = in.readTypeArray();
            if ((parameterTypes == null) || (parameterTypes.length == 0)) {
                this.parameterTypes = PDSType.EMPTY_ARRAY;
            } else {
                this.in = in;
                this.values = new Object[parameterTypes.length];
                Arrays.fill(this.values, UNINITIALIZED);
            }
            this.connection = connection;
            this.localSocketAddress = localSocketAddress;
            this.remoteSocketAddress = remoteSocketAddress;
            this.ownerThread = Thread.currentThread();
        }

        /**
     * @serialData
     */
        private void readObject(final ObjectInputStream in) throws ClassNotFoundException, IOException {
            in.defaultReadObject();
            this.stackId = in.readLong();
            if (in.readLong() != VM_ID) {
                throw new InvalidObjectException("An instance of this class can be deserialized only within the connection thread of the VM" + " which created it, the class must have been loaded by the same classloader, and the" + " connection must not have been closed.");
            }
        }

        /**
     * @serialData
     */
        private Object readResolve() throws InvalidObjectException {
            ArrayList<DynamicBeanParametersImpl> list = stack.get();
            if (list != null) {
                for (int i = list.size(); --i >= 0; ) {
                    DynamicBeanParametersImpl p = list.get(i);
                    if (p.stackId == this.stackId) return p;
                }
            }
            throw new InvalidObjectException("An instance of this class can be deserialized only within the connection thread of the VM" + " which created it, the class must have been loaded by the same classloader, and the" + " connection must not have been closed.");
        }

        /**
     * @serialData
     */
        private void writeObject(final ObjectOutputStream out) throws IOException {
            if (Thread.currentThread() != this.ownerThread) {
                if (this.ownerThread == null) {
                    throw new IOException("Can not serialize parameters of closed request");
                } else {
                    throw new IllegalThreadStateException("Instances of this class can only be serialized by the thread of the connection which created it");
                }
            }
            long stackId = this.stackId;
            stackId = this.stackId;
            if (stackId == 0) {
                this.stackId = stackId = stackIdCounter.incrementAndGet();
                ArrayList<DynamicBeanParametersImpl> list = stack.get();
                if (list == null) {
                    list = new ArrayList<DynamicBeanParametersImpl>();
                    stack.set(list);
                }
                list.add(this);
            } else if (stackId == 1) {
                throw new IOException("Can not serialize parameters of closed request");
            }
            out.defaultWriteObject();
            out.writeLong(stackId);
            out.writeLong(VM_ID);
        }

        void close() throws IOException {
            this.connection = null;
            this.values = null;
            this.in = null;
            this.ownerThread = null;
            this.parameterTypes = null;
            final long stackId = this.stackId;
            this.stackId = 1;
            if ((stackId != 0) && (stackId != 1)) {
                final ArrayList<DynamicBeanParametersImpl> list = stack.get();
                if (list != null) {
                    for (int i = list.size(); --i >= 0; ) {
                        if (list.get(i).stackId == stackId) {
                            list.remove(i);
                            break;
                        }
                    }
                }
            }
            PDSOutputStream response = this.response;
            if (response != null) {
                this.response = null;
                response.closeInstance();
            }
        }

        @Override
        public Object get(int index) {
            final Object[] values = this.values;
            if (values == null) throw new IllegalStateException("closed");
            Object v = values[index];
            if (v == DynamicBeanParametersImpl.UNINITIALIZED) {
                for (int i = 0; i < index; i++) {
                    Object a = values[i];
                    if (a == DynamicBeanParametersImpl.UNINITIALIZED) {
                        try {
                            values[i] = a = this.in.read();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        if (a instanceof InputStream) {
                            throw new IllegalStateException("The parameter at index " + index + " is not available because there is an InputStream " + "parameter before which has not been read now.");
                        } else if (a instanceof PDSInputStream.Table) {
                            throw new IllegalStateException("The parameter at index " + index + " is not available because there is a table " + "parameter before which has not been read now.");
                        }
                    }
                }
                try {
                    values[index] = v = this.in.read();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else if (v == DynamicBeanParametersImpl.UNAVAILABLE) {
                throw new IllegalStateException("The parameter at index " + index + " already has been called and can not be called again, " + "because it is a stream or a table.");
            }
            if ((v instanceof InputStream) || (v instanceof PDSInputStream.Table)) values[index] = DynamicBeanParametersImpl.UNAVAILABLE;
            return v;
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return this.localSocketAddress;
        }

        @Override
        public PDSOutputStream getPDSResponse() throws IOException {
            PDSOutputStream response = this.response;
            if (response == null) {
                PRMIServerConnection connection = this.connection;
                if (connection != null) {
                    this.response = response = connection.createOutput();
                    response.writeInt8(PRMI.METHODCALL_RESPONSE);
                } else {
                    throw new IOException("closed");
                }
            }
            return response;
        }

        @Override
        public Object getResponse() throws IOException {
            return getPDSResponse();
        }

        @Override
        public DataOutput getResponseDataOutput() throws IOException {
            return getPDSResponse();
        }

        @Override
        public ObjectOutput getResponseObjectOutput() throws IOException {
            return getPDSResponse();
        }

        @Override
        public OutputStream getResponseOutputStream() throws IOException {
            return getPDSResponse();
        }

        @Override
        public SocketAddress getRemoteSocketAddress() {
            return this.remoteSocketAddress;
        }
    }
}
