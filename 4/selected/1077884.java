package com.cbsgmbh.xi.af.edifact.jca;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;
import com.cbsgmbh.xi.af.edifact.jca.factories.SenderFactory;
import com.cbsgmbh.xi.af.edifact.sender.Sender;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.ra.cci.NWInteraction;
import com.sap.aii.af.ra.cci.XIInteractionSpec;

public class CCIInteraction implements NWInteraction {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_Adapter_Http/com/cbsgmbh/opi2/xi/af/edifact/jca/CCIInteraction.java#1 $";

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_ADAPTER_HTTP);

    private CCIConnection cciConnection;

    private SPIManagedConnection spiManagedConnection;

    public CCIInteraction(Connection connection) throws ResourceException {
        final Tracer tracer = baseTracer.entering("CCIInteraction(Connection connection)");
        cciConnection = (CCIConnection) connection;
        spiManagedConnection = cciConnection.getSPIManagedConnection();
        if (cciConnection == null || spiManagedConnection == null) {
            throw new ResourceException("Invalid Connection data received...");
        }
        tracer.leaving();
    }

    public InteractionSpec getInteractionSpec(String arg0) throws NotSupportedException {
        final Tracer tracer = baseTracer.entering("getInteractionSpec(String arg0)");
        tracer.leaving();
        return new CCIInteractionSpec();
    }

    public void clearWarnings() throws ResourceException {
    }

    public void close() throws ResourceException {
        this.cciConnection = null;
    }

    /**
	 * This is the most important method for interaction with the partner system
	 * 
	 * @param interactionSpec
	 * @param inRecord
	 * @return outRecord
	 */
    public Record execute(InteractionSpec interactionSpec, Record inRecord) throws ResourceException {
        final Tracer tracer = baseTracer.entering("execute(InteractionSpec interactionSpec, Record inRecord)");
        Record outRecord = null;
        if (interactionSpec == null) {
            ResourceException re = new ResourceException("No interactionSpec provided.");
            tracer.throwing(re);
            throw re;
        }
        if (!(interactionSpec instanceof XIInteractionSpec)) {
            ResourceException re = new ResourceException("Provided interactionSpec is not valid.");
            tracer.throwing(re);
            throw re;
        }
        XIInteractionSpec xiInteractionSpec = (XIInteractionSpec) interactionSpec;
        String xiMethod = xiInteractionSpec.getFunctionName();
        if (xiMethod.compareTo(XIInteractionSpec.SEND) == 0) {
            Sender sender = SenderFactory.createSendSender();
            outRecord = sender.send(interactionSpec, inRecord, this.spiManagedConnection, this.spiManagedConnection.getChannelId());
        } else if (xiMethod.compareTo(XIInteractionSpec.CALL) == 0) {
            Sender sender = SenderFactory.createCallSender();
            outRecord = sender.send(interactionSpec, inRecord, this.spiManagedConnection, this.spiManagedConnection.getChannelId());
        } else {
            ResourceException re = new ResourceException("Unknown function name in interactionSpec: " + xiMethod);
            tracer.throwing(re);
            throw re;
        }
        tracer.leaving();
        return outRecord;
    }

    public boolean execute(InteractionSpec arg0, Record arg1, Record arg2) throws ResourceException {
        final Tracer tracer = baseTracer.entering("execute(InteractionSpec interactionSpec, Record arg1, Record arg2)");
        tracer.leaving();
        return true;
    }

    public Connection getConnection() {
        return this.cciConnection;
    }

    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }
}
