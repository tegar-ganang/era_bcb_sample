package com.cbsgmbh.xi.af.edifact.module.transform.configuration;

import javax.naming.NamingException;
import org.w3c.dom.Document;
import com.cbsgmbh.xi.af.edifact.module.transform.util.ErrorHelper;
import com.cbsgmbh.xi.af.edifact.module.transform.xsdStore.SchemaStore;
import com.cbsgmbh.xi.af.edifact.util.EdifactUtil;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracerSapImpl;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.cbsgmbh.xi.af.trace.helpers.TracerCategories;
import com.sap.aii.af.mp.module.ModuleContext;
import com.sap.aii.af.mp.module.ModuleData;
import com.sap.aii.af.mp.module.ModuleException;
import com.sap.aii.af.ra.ms.api.Message;
import com.sap.aii.af.ra.ms.api.MessageDirection;
import com.sap.aii.af.service.cpa.Binding;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.LookupManager;
import com.sap.aii.af.service.headermapping.HeaderMappingException;

public class ConfigurationSapImpl extends ConfigurationAbstractImpl {

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_TransformModule/com/cbsgmbh/opi2/xi/af/edifact/module/transform/configuration/ConfigurationSapImpl.java#1 $";

    private static BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_TRANSFORM);

    protected ConfigurationSettings realSettings;

    /**
     * Constructor which receives the schema information
     * 
     * @param hmSchemas
     *            HashMap including the xsd names and corresponding document
     *            objects
     * @throws HeaderMappingException 
     * @throws CPAException 
     * @throws ModuleException 
     * @throws NamingException 
     */
    public ConfigurationSapImpl(SchemaStore schemaStore, ModuleContext moduleContext, Message message, ModuleData inputModuleData) throws Exception {
        this.schemaStore = schemaStore;
        lookupConfiguration(moduleContext, message, inputModuleData);
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
     * @param inputModuleData
     *            the data transferred to the module
     * @return void
     * @throws NamingException
     * @throws ModuleException
     * @throws CPAException
     * @throws HeaderMappingException
     */
    private void lookupConfiguration(ModuleContext moduleContext, Message message, ModuleData inputModuleData) throws NamingException, ModuleException, CPAException, HeaderMappingException {
        final Tracer tracer = baseTracer.entering("lookupConfiguration(ModuleContext moduleContext, Message message, ModuleData inputModuleData)");
        LookupManager lookupManager = LookupManager.getInstance();
        tracer.info("LookupManager lookupManager created");
        String channelID = moduleContext.getChannelID();
        checkValueForExistence(tracer, channelID, "channelID");
        Channel channel = (Channel) lookupManager.getCPAObject(CPAObjectType.CHANNEL, channelID);
        if (channel == null) ErrorHelper.logErrorAndThrow(tracer, "Channel object could not be retrieved. "); else tracer.info("Channel channel created : {0}", channel.getChannelName());
        Binding binding = lookupManager.getBindingByChannelId(channelID);
        if (binding == null) ErrorHelper.logErrorAndThrow(tracer, "Binding object could not be generated. "); else tracer.info("Binding binding created");
        String adapterType = channel.getAdapterType();
        tracer.info("Adapter type is: {0}", adapterType);
        if ((adapterType != null) && (adapterType.equals(EdifactUtil.ADAPTER_NAME))) {
            if (message.getMessageDirection().toString().equals(MessageDirection.OUTBOUND.toString())) realSettings = new ConfigurationEdifactAdapterToXiSapImpl(this.schemaStore, message, channel, binding, moduleContext); else realSettings = new ConfigurationEdifactAdapterFromXiSapImpl(this.schemaStore, message, channel, binding, moduleContext);
        } else {
            if (message.getMessageDirection().toString().equals(MessageDirection.OUTBOUND.toString())) realSettings = new ConfigurationGenericAdapterToXiSapImpl(this.schemaStore, message, channel, binding, moduleContext); else realSettings = new ConfigurationGenericAdapterFromXiSapImpl(this.schemaStore, message, channel, binding, moduleContext);
        }
        tracer.leaving();
    }

    public String getAdapterStatus() {
        return this.realSettings.getAdapterStatus();
    }

    public String getAperakExpectedDelfor() {
        return this.realSettings.getAperakExpectedDelfor();
    }

    public String getAperakExpectedDesadv() {
        return this.realSettings.getAperakExpectedDesadv();
    }

    public String getAperakExpectedInvoic() {
        return this.realSettings.getAperakExpectedInvoic();
    }

    public String getAperakExpectedOrder() {
        return this.realSettings.getAperakExpectedOrder();
    }

    public String getAperakExpectedOrdrsp() {
        return this.realSettings.getAperakExpectedOrdrsp();
    }

    public String getCharacterSet() {
        return this.realSettings.getCharacterSet();
    }

    public String getFromAS2Party() {
        return this.realSettings.getFromAS2Party();
    }

    public String getFromUnbParty() {
        return this.realSettings.getFromUnbParty();
    }

    public String getFromXiParty() {
        return this.realSettings.getFromXiParty();
    }

    public String getTestIndicator() {
        return this.realSettings.getTestIndicator();
    }

    public String getToAS2Party() {
        return this.realSettings.getToAS2Party();
    }

    public String getToUnbParty() {
        return this.realSettings.getToUnbParty();
    }

    public String getToXiParty() {
        return this.realSettings.getToXiParty();
    }

    public String getUnbReceiverIdQualifier() {
        return this.realSettings.getUnbReceiverIdQualifier();
    }

    public String getUnbReceiverRoutingAddress() {
        return this.realSettings.getUnbReceiverRoutingAddress();
    }

    public String getUnbSenderIdQualifier() {
        return this.realSettings.getUnbSenderIdQualifier();
    }

    public String getUnbSenderRoutingAddress() {
        return this.realSettings.getUnbSenderRoutingAddress();
    }

    public String getVersion() {
        return this.realSettings.getVersion();
    }

    public String getXsdName() {
        return this.realSettings.getXsdName();
    }

    public String getXsdNames() {
        return this.realSettings.getXsdNames();
    }

    public boolean isDuplicateCheck() {
        return this.realSettings.isDuplicateCheck();
    }

    public void setCharacterSet(String characterSet) {
        realSettings.setCharacterSet(characterSet);
    }

    public String getAdapterType() {
        return this.realSettings.getAdapterType();
    }

    public String getMessageDirection() {
        return this.realSettings.getMessageDirection();
    }

    public SchemaStore getSchemaStore() {
        return this.realSettings.getSchemaStore();
    }

    public Document getXsdDocument() {
        return this.realSettings.getXsdDocument();
    }

    public boolean getSchemaValidation() {
        return this.realSettings.getSchemaValidation();
    }
}
