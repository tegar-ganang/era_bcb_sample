package com.cbsgmbh.xi.af.edifact.module.transform.configuration;

import javax.naming.NamingException;
import com.cbsgmbh.xi.af.edifact.module.transform.util.ErrorHelper;
import com.cbsgmbh.xi.af.edifact.module.transform.util.Helper;
import com.cbsgmbh.xi.af.edifact.module.transform.xsdStore.SchemaStore;
import com.cbsgmbh.xi.af.edifact.util.EdifactUtil;
import com.cbsgmbh.xi.af.edifact.util.ModuleUtil;
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

public abstract class ConfigurationEdifactAdapterSapBase extends ConfigurationAbstractImpl {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_TransformModule/com/cbsgmbh/opi2/xi/af/edifact/module/transform/configuration/ConfigurationEdifactAdapterSapBase.java#1 $";

    private static BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_TRANSFORM);

    public ConfigurationEdifactAdapterSapBase(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException {
        initialize(schemaStore, message, channel, binding, moduleContext);
    }

    /**
     * This message delivers the configuration for both message directions. <p/>
     * In case of message direction outbound, information (e.g. the XI parties)
     * are extracted from the message and appended to the configuration object.
     * <p/> In case of message direction inbound the message header fields have
     * to be initialized (XI parties). The XI parties are normalized to DUNS
     * party numbers resp. GLNs.
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
    protected abstract void initialize(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException;

    protected void retrieveXsdDocument() throws ModuleException {
        final Tracer tracer = baseTracer.entering("retrieveXsdDocument(final Tracer tracer)");
        this.xsdDocument = this.schemaStore.getDocument(this.xsdName);
        checkValueForExistence(tracer, this.xsdDocument, "xsdDocument");
        tracer.leaving();
    }

    protected void normalizeXiParties() throws ModuleException, CPAException {
        final Tracer tracer = baseTracer.entering("normalizeXiParties()");
        if ((Helper.checkForInt(this.unbSenderIdQualifier)) && (Integer.valueOf(this.unbSenderIdQualifier).intValue() == EdifactUtil.UNB_QUALIFIER_DUNS)) {
            this.fromUnbParty = ModuleUtil.getAlternativePartyIdentifier(EdifactUtil.CPA_PARTY_AGENCY_DUNS, EdifactUtil.CPA_PARTY_SCHEMA_DUNS, this.fromXiParty);
        } else if ((Helper.checkForInt(unbSenderIdQualifier)) && (Integer.valueOf(this.unbSenderIdQualifier).intValue() == EdifactUtil.UNB_QUALIFIER_GLN)) {
            this.fromUnbParty = ModuleUtil.getAlternativePartyIdentifier(EdifactUtil.CPA_PARTY_AGENCY_GLN, EdifactUtil.CPA_PARTY_SCHEMA_GLN, this.fromXiParty);
        } else {
            this.fromUnbParty = ModuleUtil.getInterchangePartyIdentifier(this.unbSenderIdQualifier, this.fromXiParty);
        }
        if ((this.fromUnbParty == null) || (this.fromUnbParty.trim().equals(""))) {
            ErrorHelper.logErrorAndThrow(tracer, "this.fromUnbParty could not be determined. ");
        } else tracer.info("this.fromUnbParty is set to : " + this.fromUnbParty);
        if (this.messageType.equals(EdifactUtil.MSG_TYPE_EDIFACT)) {
            if ((Helper.checkForInt(this.unbReceiverIdQualifier)) && (Integer.valueOf(this.unbReceiverIdQualifier).intValue() == EdifactUtil.UNB_QUALIFIER_DUNS)) {
                this.toUnbParty = ModuleUtil.getAlternativePartyIdentifier(EdifactUtil.CPA_PARTY_AGENCY_DUNS, EdifactUtil.CPA_PARTY_SCHEMA_DUNS, this.toXiParty);
            } else if ((Helper.checkForInt(this.unbReceiverIdQualifier)) && (Integer.valueOf(this.unbReceiverIdQualifier).intValue() == EdifactUtil.UNB_QUALIFIER_GLN)) {
                this.toUnbParty = ModuleUtil.getAlternativePartyIdentifier(EdifactUtil.CPA_PARTY_AGENCY_GLN, EdifactUtil.CPA_PARTY_SCHEMA_GLN, this.toXiParty);
            } else {
                this.toUnbParty = ModuleUtil.getInterchangePartyIdentifier(this.unbReceiverIdQualifier, this.toXiParty);
            }
            if ((this.toUnbParty == null) || (this.toUnbParty.trim().equals(""))) {
                ErrorHelper.logErrorAndThrow(tracer, "this.toUnbParty could not be determined. ");
            } else tracer.info("this.toUnbParty is set to : " + this.toUnbParty);
        }
        tracer.leaving();
    }

    protected void setUnbPartyInformation(Channel channel) throws CPAException {
        final Tracer tracer = baseTracer.entering("setUnbPartyInformation(Channel channel, final Tracer tracer)");
        this.unbSenderIdQualifier = channel.getValueAsString(UNB_ID_QUALF_SND);
        if ((this.unbSenderIdQualifier == null) || (this.unbSenderIdQualifier.trim().equals(""))) {
            this.unbSenderIdQualifier = EdifactUtil.CPA_PARTY_AGENCY_UNBID_DEFAULT_PREFIX;
            tracer.info("Channel parameter {0} is not set, default value is set to: {1}", new Object[] { UNB_ID_QUALF_SND, this.unbSenderIdQualifier });
        } else tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { UNB_ID_QUALF_SND, this.unbSenderIdQualifier });
        this.unbReceiverIdQualifier = channel.getValueAsString(UNB_ID_QUALF_REC);
        if ((this.unbReceiverIdQualifier == null) || (this.unbReceiverIdQualifier.trim().equals(""))) {
            this.unbReceiverIdQualifier = EdifactUtil.CPA_PARTY_AGENCY_UNBID_DEFAULT_PREFIX;
            tracer.info("Channel parameter {0} is not set, default value is set to: {1}", new Object[] { UNB_ID_QUALF_REC, this.unbReceiverIdQualifier });
        } else tracer.info("Channel parameter read : key = {0}, value = {1}", new Object[] { UNB_ID_QUALF_REC, this.unbReceiverIdQualifier });
        tracer.leaving();
    }

    protected void getXsdNameFromChannel(Channel channel) throws CPAException, ModuleException {
        final Tracer tracer = baseTracer.entering("getXsdNameFromChannel(Channel channel)");
        this.xsdName = channel.getValueAsString(XSD_NAME);
        checkValueForExistence(tracer, this.xsdName, "xsdName", "channel");
        tracer.leaving();
    }

    protected void getMessageDirection(Message message) throws ModuleException {
        final Tracer tracer = baseTracer.entering("getChannelInformation(Message message)");
        this.messageDirection = message.getMessageDirection().toString();
        checkValueForExistence(tracer, this.messageDirection, "messageDirection");
        tracer.leaving();
    }
}
