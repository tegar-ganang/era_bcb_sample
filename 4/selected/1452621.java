package com.cbsgmbh.xi.af.edifact.jca;

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
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.ra.cci.NWConnectionFactory;

public class CCIConnectionFactory implements NWConnectionFactory, Serializable, Referenceable {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_Adapter_Http/com/cbsgmbh/opi2/xi/af/edifact/jca/CCIConnectionFactory.java#1 $";

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_ADAPTER_HTTP);

    private ManagedConnectionFactory managedConnectionFactory;

    private ConnectionManager connectionManager = null;

    private Reference reference;

    public CCIConnectionFactory(ManagedConnectionFactory managedConnectionFactory, ConnectionManager connectionManager) {
        final Tracer tracer = baseTracer.entering("CCIConnectionFactory(ManagedConnectionFactory managedConnectionFactory, ConnectionManager connectionManager)");
        this.managedConnectionFactory = managedConnectionFactory;
        if (connectionManager == null) {
            tracer.info("No Connection Manager received...");
            this.connectionManager = new SPIConnectionManager();
        } else {
            this.connectionManager = connectionManager;
        }
        tracer.leaving();
    }

    public ConnectionSpec getConnectionSpec(String connectionType) {
        final Tracer tracer = baseTracer.entering("getConnectionSpec(String type)");
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

    public RecordFactory getRecordFactory() throws ResourceException {
        return new CCIRecordFactory();
    }

    public RecordFactory getRecordFactory(String arg0) throws NotSupportedException, ResourceException {
        return new CCIRecordFactory();
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public Reference getReference() throws NamingException {
        return reference;
    }
}
