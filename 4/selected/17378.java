package com.cbsgmbh.xi.af.as2.jca;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import com.cbsgmbh.xi.af.as2.as2.Util;
import com.cbsgmbh.xi.af.as2.util.AS2Util;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.service.administration.api.cpa.CPAFactory;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.engine.interfaces.connector.ManagedConnectionFactoryActivation;

/**
 * For OUTBOUND processing only; INBOUND processing is achieved by WebListener servlet
 * @author developer
 *
 */
@SuppressWarnings({ "serial", "deprecation", "unused", "unchecked" })
public class SPIManagedConnectionFactory implements ManagedConnectionFactory, ManagedConnectionFactoryActivation, Serializable {

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(SPIManagedConnectionFactory.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    private ChannelConfiguration channelConfiguration = null;

    private SPIManagedConnection spiManagedConnection;

    private String adapterNamespace;

    private String adapterType;

    public static final String ADAPTER_NAME = AS2Util.ADAPTER_NAME;

    public static final String ADAPTER_NAMESPACE = AS2Util.ADAPTER_NAMESPACE;

    private PrintWriter printWriter;

    private CCIRecordFactory cciRecordFactory = null;

    public SPIManagedConnectionFactory() {
        super();
    }

    public Object createConnectionFactory() throws ResourceException {
        final Tracer tracer = baseTracer.entering("createConnectionFactory()");
        CCIConnectionFactory cciConnectionFactory;
        try {
            cciConnectionFactory = new CCIConnectionFactory(this, null);
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
        return cciConnectionFactory;
    }

    public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
        final Tracer tracer = baseTracer.entering("createConnectionFactory(ConnectionManager connectionManager)");
        CCIConnectionFactory cciConnectionFactory;
        try {
            cciConnectionFactory = new CCIConnectionFactory(this, connectionManager);
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        try {
            if (channelConfiguration == null) {
                channelConfiguration = new ChannelConfiguration(ADAPTER_NAME, ADAPTER_NAMESPACE);
                channelConfiguration.registerAdapter(this);
            }
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Channel monitoring failed..." + e.toString());
        }
        tracer.leaving();
        return cciConnectionFactory;
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        final Tracer tracer = baseTracer.entering("createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)");
        String channelId;
        Channel channel;
        boolean supportsLocalTransactions = false;
        try {
            SPIConnectionRequestInfo spiConnectionRequestInfo = (SPIConnectionRequestInfo) connectionRequestInfo;
            channelId = spiConnectionRequestInfo.getChannelId();
            tracer.info("Channel ID: " + channelId);
            channel = (Channel) CPAFactory.getInstance().getLookupManager().getCPAObject(CPAObjectType.CHANNEL, channelId);
            PasswordCredential passwordCredential = Util.getPasswordCredential(this, subject, connectionRequestInfo);
            ;
            String adapterStatus = channel.getValueAsString("adapterStatus");
            tracer.info("Adapter status: " + adapterStatus);
            if (adapterStatus != null && adapterStatus.equals("active")) {
                spiManagedConnection = new SPIManagedConnection(this, passwordCredential, supportsLocalTransactions, channelId, channel);
            } else {
                String errorMessage = "SPI Connection could not be created. Reason: Channel status is set to inactive.";
                ResourceException resourceException = new ResourceException(errorMessage);
                this.finalize();
                throw resourceException;
            }
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.error(throwable.toString());
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
        return spiManagedConnection;
    }

    void destroyManagedConnection(String channelID) throws ResourceException {
        final Tracer tracer = baseTracer.entering("destroyManagedConnection(String channelID)");
        if (this.spiManagedConnection != null) {
            this.spiManagedConnection.destroy(true);
        }
        tracer.leaving();
    }

    public ManagedConnection matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        final Tracer tracer = baseTracer.entering("matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo connectionRequestInfo)");
        tracer.leaving();
        return null;
    }

    public boolean equals(Object obj) {
        final Tracer tracer = baseTracer.entering("equals(Object obj)");
        if (obj == null) return false;
        if (obj instanceof SPIManagedConnectionFactory) {
            int hash1 = ((SPIManagedConnectionFactory) obj).hashCode();
            int hash2 = hashCode();
            tracer.leaving();
            return hash1 == hash2;
        } else {
            tracer.leaving();
            return false;
        }
    }

    public int hashCode() {
        int hash = 0;
        String propset = ADAPTER_NAMESPACE + ADAPTER_NAME;
        hash = propset.hashCode();
        return hash;
    }

    public ResourceAdapter getResourceAdapter() {
        return null;
    }

    public void setResourceAdapter(ResourceAdapter resourceadapter) {
        return;
    }

    public String getAdapterNamespace() {
        return ADAPTER_NAMESPACE;
    }

    public void setAdapterNamespace(String adapterNamespace) {
        final Tracer tracer = baseTracer.entering("setAdapterNamespace(String adapterNamespace)");
        this.adapterNamespace = adapterNamespace;
        tracer.leaving();
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

    public String getAdapterType() {
        return ADAPTER_NAME;
    }

    public void start() {
        final Tracer tracer = baseTracer.entering("start()");
        try {
            channelConfiguration = new ChannelConfiguration(ADAPTER_NAME, ADAPTER_NAMESPACE);
            channelConfiguration.registerAdapter(this);
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Channel monitoring initialization failed..." + e.toString());
        }
        try {
            this.cciRecordFactory = new CCIRecordFactory();
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Unable to create XI message factory. Adapter cannot not start the inbound processing!");
        }
        tracer.leaving();
    }

    public void stop() {
        final Tracer tracer = baseTracer.entering("stop()");
        try {
            channelConfiguration.stop();
        } catch (Exception e) {
            tracer.catched(e);
            tracer.error("Channel monitoring deregistration failed..." + e.toString());
        }
        tracer.leaving();
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        this.printWriter = printWriter;
    }

    CCIRecordFactory getCCIRecordFactory() {
        return this.cciRecordFactory;
    }
}
