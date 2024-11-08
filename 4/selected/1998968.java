package com.cbsgmbh.xi.af.as2.jca;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;
import com.cbsgmbh.xi.af.as2.jca.factories.SenderFactory;
import com.cbsgmbh.xi.af.as2.sender.Sender;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.lib.ra.cci.XIInteraction;
import com.sap.aii.af.lib.ra.cci.XIInteractionSpec;

public class CCIInteraction implements XIInteraction {

    private static final BaseTracer baseTracer = new BaseTracerSapImpl(CCIInteraction.class.getName(), TracerCategories.APP_ADAPTER_HTTP);

    private javax.resource.cci.Connection connection;

    private SPIManagedConnection spiManagedConnection;

    @SuppressWarnings("unused")
    private CCIRecordFactory cciRecordFactory = null;

    private SPIManagedConnectionFactory spiManagedConnectionFactory = null;

    public CCIInteraction(javax.resource.cci.Connection cciConnection) throws ResourceException {
        final Tracer tracer = baseTracer.entering("CCIInteraction(Connection cciConnection)");
        this.connection = cciConnection;
        this.spiManagedConnection = ((com.cbsgmbh.xi.af.as2.jca.CCIConnection) this.connection).getSPIManagedConnection();
        if (cciConnection == null || spiManagedConnection == null) {
            throw new ResourceException("Invalid Connection data received...");
        }
        this.spiManagedConnectionFactory = (SPIManagedConnectionFactory) this.spiManagedConnection.getManagedConnectionFactory();
        this.cciRecordFactory = this.spiManagedConnectionFactory.getCCIRecordFactory();
        tracer.leaving();
    }

    public XIInteractionSpec getXIInteractionSpec() throws NotSupportedException {
        final Tracer tracer = baseTracer.entering("getInteractionSpec()");
        tracer.leaving();
        return new CCIInteractionSpec();
    }

    public void clearWarnings() throws ResourceException {
    }

    public void close() throws ResourceException {
        this.connection = null;
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
        tracer.info("interactionSpec is not null.");
        if (!(interactionSpec instanceof XIInteractionSpec)) {
            ResourceException re = new ResourceException("Provided interactionSpec is not valid.");
            tracer.throwing(re);
            throw re;
        }
        tracer.info("interactionSpec is instance of XIInteractionSpec.");
        XIInteractionSpec xiInteractionSpec = (XIInteractionSpec) interactionSpec;
        String xiMethod = xiInteractionSpec.getFunctionName();
        tracer.info("xiMethod = " + xiMethod);
        if (xiMethod.compareTo(XIInteractionSpec.SEND) == 0) {
            Sender sender = SenderFactory.createSendSender();
            tracer.info("Created sendSender.");
            outRecord = sender.send(interactionSpec, inRecord, this.spiManagedConnection, this.spiManagedConnection.getChannelId());
            tracer.info("Sent sendSender.");
        } else if (xiMethod.compareTo(XIInteractionSpec.CALL) == 0) {
            Sender sender = SenderFactory.createCallSender();
            tracer.info("Created callSender.");
            outRecord = sender.send(interactionSpec, inRecord, this.spiManagedConnection, this.spiManagedConnection.getChannelId());
            tracer.info("Sent callSender.");
        } else {
            ResourceException re = new ResourceException("Unknown function name in interactionSpec: " + xiMethod);
            tracer.info("ResourceException created.");
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
        return this.connection;
    }

    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }
}
