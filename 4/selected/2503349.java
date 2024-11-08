package com.cbsgmbh.xi.af.edifact.module.transform.configuration;

import javax.naming.NamingException;
import com.cbsgmbh.xi.af.edifact.module.transform.util.ErrorHelper;
import com.cbsgmbh.xi.af.edifact.module.transform.xsdStore.SchemaStore;
import com.cbsgmbh.xi.af.edifact.util.EdifactUtil;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.mp.module.ModuleContext;
import com.sap.aii.af.mp.module.ModuleException;
import com.sap.aii.af.ra.ms.api.Message;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.headermapping.HeaderMappingException;

public abstract class ConfigurationGenericAdapterSapBase extends ConfigurationAbstractImpl {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_TransformModule/com/cbsgmbh/opi2/xi/af/edifact/module/transform/configuration/ConfigurationGenericAdapterSapBase.java#1 $";

    private static BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_TRANSFORM);

    public ConfigurationGenericAdapterSapBase(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException {
        initialize(schemaStore, message, channel, binding, moduleContext);
    }

    /**
     * This message delivers the configuration for both message directions from
     * the module context. In case of beeing called from any technical adapter,
     * e.g. the File Adapter, the Transform Module must read its configuration
     * parameters from the module context. In case of message direction inbound
     * the message header fields have to be initialized (XI parties).
     * 
     * @param moduleContext
     *            module context
     * @param message
     *            the transferred message
     * @param channel
     *            the channel object
     * @return void
     * @throws NamingException
     * @throws ModuleException
     * @throws CPAException
     * @throws HeaderMappingException
     */
    public abstract void initialize(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException;

    protected void getMessageDirection(Message message) throws ModuleException {
        final Tracer tracer = baseTracer.entering("getMessageDirection(Message message)");
        this.messageDirection = message.getMessageDirection().toString();
        checkValueForExistence(tracer, this.messageDirection, "messageDirection");
        tracer.leaving();
    }

    protected void getUnbSenderAndReceiverIds(ModuleContext moduleContext) throws ModuleException {
        final Tracer tracer = baseTracer.entering("getUnbSenderAndReceiverIds(ModuleContext moduleContext)");
        this.fromUnbParty = moduleContext.getContextData(UNB_FROM_PARTY);
        if ((this.fromUnbParty == null) || (this.fromUnbParty.trim().equals(""))) {
            tracer.error("Module context parameter reading error : key {0}", UNB_FROM_PARTY);
            String errorMessage = "From UNB party could not be retrieved from module context. ";
            ModuleException me = new ModuleException(errorMessage);
            tracer.throwing(me);
            throw me;
        }
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { UNB_FROM_PARTY, this.fromUnbParty });
        this.toUnbParty = moduleContext.getContextData(UNB_TO_PARTY);
        if ((this.toUnbParty == null) || (this.toUnbParty.trim().equals(""))) {
            tracer.error("Module context parameter reading error : key {0}", UNB_TO_PARTY);
            String errorMessage = "To UNB party could not be retrieved from module context. ";
            ModuleException me = new ModuleException(errorMessage);
            tracer.throwing(me);
            throw me;
        }
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { UNB_TO_PARTY, this.toUnbParty });
        tracer.leaving();
    }

    protected void setUnbPartyInformation(ModuleContext moduleContext) {
        final Tracer tracer = baseTracer.entering("setUnbPartyInformation(ModuleContext moduleContext)");
        this.unbSenderIdQualifier = moduleContext.getContextData(UNB_ID_QUALF_SND);
        if ((this.unbSenderIdQualifier == null) || (this.unbSenderIdQualifier.trim().equals(""))) {
            this.unbSenderIdQualifier = EdifactUtil.CPA_PARTY_AGENCY_UNBID_DEFAULT_PREFIX;
            tracer.info("Module context parameter {0} is not set, default value is set to: {1}", new Object[] { UNB_ID_QUALF_SND, this.unbSenderIdQualifier });
        } else tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { UNB_ID_QUALF_SND, this.unbSenderIdQualifier });
        this.unbReceiverIdQualifier = moduleContext.getContextData(UNB_ID_QUALF_REC);
        if ((this.unbReceiverIdQualifier == null) || (this.unbReceiverIdQualifier.trim().equals(""))) {
            this.unbReceiverIdQualifier = EdifactUtil.CPA_PARTY_AGENCY_UNBID_DEFAULT_PREFIX;
            tracer.info("Module context parameter {0} is not set, default value is set to: {1}", new Object[] { UNB_ID_QUALF_REC, this.unbReceiverIdQualifier });
        } else tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { UNB_ID_QUALF_REC, this.unbReceiverIdQualifier });
        tracer.leaving();
    }

    protected void retrieveXsdDocument() throws ModuleException {
        final Tracer tracer = baseTracer.entering("retrieveXsdDocument(final Tracer tracer)");
        this.xsdDocument = this.schemaStore.getDocument(this.xsdName);
        if (this.xsdDocument == null) {
            ErrorHelper.logErrorAndThrow(tracer, "Xsd document could not be retrieved.");
        } else tracer.info("XSD document has been extracted.");
        tracer.leaving();
    }

    protected void getXsdNameFromModuleContext(ModuleContext moduleContext) throws ModuleException {
        final Tracer tracer = baseTracer.entering("getXsdNameFromModuleContext(ModuleContext moduleContext)");
        this.xsdName = moduleContext.getContextData(XSD_NAME);
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { XSD_NAME, this.xsdName });
        if ((this.xsdName == null) || (this.xsdName.trim().equals(""))) {
            tracer.error("Module context parameter reading error : key {0}", XSD_NAME);
            String errorMessage = "xsdName entry could not be retrieved from module context. ";
            ModuleException me = new ModuleException(errorMessage);
            tracer.throwing(me);
            throw me;
        }
        tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { XSD_NAME, this.xsdName });
        tracer.leaving();
    }

    protected void getChannelInformation(Message message) throws ModuleException {
        final Tracer tracer = baseTracer.entering("getChannelInformation(Message message, final Tracer tracer)");
        this.messageDirection = message.getMessageDirection().toString();
        if ((this.messageDirection == null) || (this.messageDirection.trim().equals(""))) {
            ErrorHelper.logErrorAndThrow(tracer, "Message direction could not be retrieved from message");
        }
        tracer.leaving();
    }
}
