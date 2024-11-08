package com.cbsgmbh.xi.af.as2.jca;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import com.cbsgmbh.xi.af.as2.as2.Util;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.service.cpa.Channel;

@SuppressWarnings("unchecked")
public class SPIManagedConnection implements ManagedConnection {

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(SPIManagedConnection.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    private SPIManagedConnectionFactory spiManagedConnectionFactory;

    private boolean destroyed = false;

    @SuppressWarnings("unused")
    private Channel channel;

    private String channelId;

    private Set connectionSet;

    private SPIConnectionEventListener connectionEventListeners;

    private PasswordCredential passwordCredential;

    private PrintWriter printWriter;

    SPIManagedConnection(SPIManagedConnectionFactory spiManagedConnectionFactory, PasswordCredential passwordCredential, boolean supportsLocalTransactions, String channelId, Channel channel) throws ResourceException {
        Tracer tracer = baseTracer.entering("SPIManagedConnection(SPIManagedConnectionFactory spiManagedConnectionFactory, PasswordCredential passwordCredential, boolean supportsLocalTransactions, String channelId, Channel channel)");
        try {
            this.spiManagedConnectionFactory = spiManagedConnectionFactory;
            this.passwordCredential = passwordCredential;
            this.channelId = channelId;
            this.channel = channel;
            this.connectionSet = new HashSet();
            this.connectionEventListeners = new SPIConnectionEventListener(this);
            tracer.info("Connection established...");
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
    }

    public void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        connectionEventListeners.addConnectorListener(connectionEventListener);
    }

    public void removeConnectionEventListener(ConnectionEventListener connectionEventListener) {
        Tracer tracer = baseTracer.entering("removeConnectionEventListener(ConnectionEventListener connectionEventListener)");
        connectionEventListeners.removeConnectorListener(connectionEventListener);
        tracer.leaving();
    }

    public void associateConnection(Object connection) throws ResourceException {
        final Tracer tracer = baseTracer.entering("associateConnection(Object connection");
        if (!destroyed) {
            if (connection instanceof CCIConnection) {
                CCIConnection cciConnection = (CCIConnection) connection;
                cciConnection.associateConnection(this);
            } else {
                throw new javax.resource.spi.IllegalStateException("Unsupported connection received: " + connection);
            }
        }
        tracer.leaving();
    }

    public void cleanup() throws ResourceException {
        Tracer tracer = baseTracer.entering("cleanup()");
        try {
            if (!destroyed) {
                Iterator it = connectionSet.iterator();
                while (it.hasNext()) {
                    CCIConnection cciConnection = (CCIConnection) it.next();
                    cciConnection.invalidate();
                }
            }
            connectionSet.clear();
            destroy();
            tracer.info("Connections removed...");
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
    }

    public void destroy() throws ResourceException {
        Tracer tracer = baseTracer.entering("destroy()");
        destroyed = true;
        this.spiManagedConnectionFactory = null;
        tracer.leaving();
    }

    void destroy(boolean fromManagedConnectionFactory) throws ResourceException {
        Tracer tracer = baseTracer.entering("destroy(boolean fromMCF)");
        if (!destroyed) {
            try {
                destroyed = true;
                Iterator it = this.connectionSet.iterator();
                while (it.hasNext()) {
                    CCIConnection cciConection = (CCIConnection) it.next();
                    cciConection.invalidate();
                }
                this.connectionSet.clear();
                tracer.info("Connections removed...");
            } catch (Throwable throwable) {
                tracer.catched(throwable);
                tracer.error(throwable.toString());
                throw new ResourceException(throwable.toString());
            }
        }
        tracer.leaving();
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        Tracer tracer = baseTracer.entering("getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)");
        CCIConnection cciConnection;
        try {
            PasswordCredential connPasswordCredential = Util.getPasswordCredential(this.spiManagedConnectionFactory, subject, connectionRequestInfo);
            if (isPasswordCredentialEqual(passwordCredential, connPasswordCredential)) {
                cciConnection = new CCIConnection(this);
                addConnection(cciConnection);
            } else {
                throw new javax.resource.spi.SecurityException("Authentication error");
            }
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
        return cciConnection;
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new SPIManagedConnectionMetaData(this);
    }

    boolean isDestroyed() {
        return destroyed;
    }

    void sendEvent(int eventType, Exception ex) {
        connectionEventListeners.sendEvent(eventType, ex, null);
    }

    void sendEvent(int eventType, Exception ex, Object connectionHandle) {
        connectionEventListeners.sendEvent(eventType, ex, connectionHandle);
    }

    void removeConnection(CCIConnection cciConnection) {
        Tracer tracer = baseTracer.entering("removeConnection(CCIConnection cciConnection)");
        connectionSet.remove(cciConnection);
        String strTrace = (connectionSet.isEmpty() ? "Connection set is empty." : "Connection set still contains connections");
        tracer.info(strTrace);
        tracer.leaving();
    }

    void addConnection(CCIConnection connection) {
        Tracer tracer = baseTracer.entering("addConnection(CCIConnection cciConnection)");
        connectionSet.add(connection);
        tracer.leaving();
    }

    public boolean isPasswordCredentialEqual(PasswordCredential passwordCredential1, PasswordCredential passwordCredential2) {
        final Tracer tracer = baseTracer.entering("isPasswordCredentialEqual(PasswordCredential passwordCredential1, PasswordCredential passwordCredential2)");
        boolean equals = false;
        if (passwordCredential1.equals(passwordCredential2)) {
            equals = true;
        } else if ((passwordCredential1 == null) && !(passwordCredential2 == null)) {
            equals = false;
        } else if ((passwordCredential2 == null) && !(passwordCredential1 == null)) {
            equals = false;
        } else if (!Util.isEqualString(passwordCredential1.getUserName(), passwordCredential2.getUserName())) {
            equals = false;
        } else {
            String password1 = null;
            String password2 = null;
            if (!(passwordCredential1 == null)) password1 = passwordCredential1.getPassword().toString();
            if (!(passwordCredential2 == null)) password2 = passwordCredential2.getPassword().toString();
            equals = Util.isEqualString(password1, password2);
        }
        tracer.leaving();
        return equals;
    }

    PasswordCredential getPasswordCredential() {
        return passwordCredential;
    }

    SPIManagedConnectionFactory getManagedConnectionFactory() {
        return spiManagedConnectionFactory;
    }

    public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
        spiManagedConnectionFactory = (SPIManagedConnectionFactory) managedConnectionFactory;
    }

    String getChannelId() {
        return this.channelId;
    }

    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("XAResource is not supported");
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("LocalTransaction is not supported");
    }

    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        this.printWriter = printWriter;
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }
}
