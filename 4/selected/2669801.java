package com.cbsgmbh.xi.af.as2.jca;

import java.io.Serializable;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.lib.ra.cci.XIConnectionFactory;
import com.sap.aii.af.lib.ra.cci.XIConnectionSpec;
import com.sap.aii.af.lib.ra.cci.XIRecordFactory;

public class CCIConnectionFactory implements XIConnectionFactory, Serializable, Referenceable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(CCIConnectionFactory.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    private ManagedConnectionFactory managedConnectionFactory;

    private ConnectionManager connectionManager = null;

    private Reference reference;

    private XIRecordFactory xiRecordFactory;

    /**
     * The default constructor is not used by the PI AF but was implemented to be
     * conform with the JCA specification.
     */
    public CCIConnectionFactory() throws ResourceException {
        final Tracer tracer = baseTracer.entering("CciConnectionFactory()");
        SPIManagedConnectionFactory spiManagedConnectionFactory = new SPIManagedConnectionFactory();
        this.managedConnectionFactory = spiManagedConnectionFactory;
        this.xiRecordFactory = new CCIRecordFactory();
        this.connectionManager = new SPIConnectionManager();
        tracer.leaving();
    }

    /**
     * This method is not used by the PI AF but was implemented to be
     * conform with the JCA specification.
     */
    public CCIConnectionFactory(ManagedConnectionFactory managedConnectionFactory) throws ResourceException {
        final Tracer tracer = baseTracer.entering("CciConnectionFactory(ManagedConnectionFactory managedConnectionFactory");
        SPIManagedConnectionFactory spiManagedConnectionFactory = null;
        if (managedConnectionFactory == null) spiManagedConnectionFactory = new SPIManagedConnectionFactory(); else spiManagedConnectionFactory = (SPIManagedConnectionFactory) managedConnectionFactory;
        this.managedConnectionFactory = spiManagedConnectionFactory;
        this.xiRecordFactory = new CCIRecordFactory();
        this.connectionManager = new SPIConnectionManager();
        tracer.leaving();
    }

    public CCIConnectionFactory(ManagedConnectionFactory managedConnectionFactory, ConnectionManager connectionManager) throws ResourceException {
        final Tracer tracer = baseTracer.entering("CCIConnectionFactory(ManagedConnectionFactory managedConnectionFactory, ConnectionManager connectionManager)");
        SPIManagedConnectionFactory spiManagedConnectionFactory = null;
        if (managedConnectionFactory == null) spiManagedConnectionFactory = new SPIManagedConnectionFactory(); else spiManagedConnectionFactory = (SPIManagedConnectionFactory) managedConnectionFactory;
        this.managedConnectionFactory = spiManagedConnectionFactory;
        this.xiRecordFactory = new CCIRecordFactory();
        if (connectionManager == null) {
            tracer.info("No Connection Manager received...");
            this.connectionManager = new SPIConnectionManager();
        } else {
            this.connectionManager = connectionManager;
        }
        tracer.leaving();
    }

    public XIConnectionSpec getXIConnectionSpec() {
        final Tracer tracer = baseTracer.entering("getConnectionSpec()");
        tracer.leaving();
        return new CCIConnectionSpec();
    }

    public Connection getConnection() throws ResourceException {
        final Tracer tracer = baseTracer.entering("getConnection()");
        Connection connection;
        try {
            connection = (Connection) connectionManager.allocateConnection(managedConnectionFactory, null);
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.throwing(throwable);
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
        return connection;
    }

    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        final Tracer tracer = baseTracer.entering("getConnection(ConnectionSpec connectionSpec)");
        Connection connection;
        try {
            CCIConnectionSpec cciConnectionSpec = (CCIConnectionSpec) connectionSpec;
            ConnectionRequestInfo connectionRequestInfo = new SPIConnectionRequestInfo(cciConnectionSpec.getUserName(), cciConnectionSpec.getPassword(), cciConnectionSpec.getChannelId());
            connection = (Connection) connectionManager.allocateConnection(managedConnectionFactory, connectionRequestInfo);
        } catch (Throwable throwable) {
            tracer.catched(throwable);
            tracer.throwing(throwable);
            throw new ResourceException(throwable.toString());
        }
        tracer.leaving();
        return connection;
    }

    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new CCIAdapterMetaData();
    }

    public XIRecordFactory getXIRecordFactory() throws ResourceException {
        return new CCIRecordFactory();
    }

    public RecordFactory getRecordFactory() throws NotSupportedException, ResourceException {
        return this.xiRecordFactory;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public Reference getReference() throws NamingException {
        return this.reference;
    }
}
