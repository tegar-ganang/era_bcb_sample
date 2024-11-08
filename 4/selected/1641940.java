package com.cbsgmbh.xi.af.edifact.module.transform.configuration;

import javax.naming.NamingException;
import com.cbsgmbh.xi.af.edifact.module.transform.xsdStore.SchemaStore;
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

public class ConfigurationGenericAdapterToXiSapImpl extends ConfigurationGenericAdapterSapBase {

    public ConfigurationGenericAdapterToXiSapImpl(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException {
        super(schemaStore, message, channel, binding, moduleContext);
    }

    private static final String VERSION_ID = "$Id://OPI2_EDIFACT_TransformModule/com/cbsgmbh/opi2/xi/af/edifact/module/transform/configuration/ConfigurationGenericAdapterToXiSapImpl.java#1 $";

    private static BaseTracer baseTracer = new BaseTracerSapImpl(VERSION_ID, TracerCategories.APP_TRANSFORM);

    public void initialize(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext) throws NamingException, ModuleException, CPAException, HeaderMappingException {
        final Tracer tracer = baseTracer.entering("initialize(SchemaStore schemaStore, Message message, Channel channel, Binding binding, ModuleContext moduleContext)");
        this.schemaStore = schemaStore;
        this.adapterType = channel.getAdapterType();
        getChannelInformation(message);
        getXsdNameFromModuleContext(moduleContext);
        retrieveXsdDocument();
        this.version = moduleContext.getContextData(EDIFACT_VERSION);
        if ((this.version == null) || (this.version.trim().equals(""))) {
            tracer.error("Module context parameter reading error : key {0}", EDIFACT_VERSION);
            String errorMessage = "EDIFACT version could not be retrieved from module context. ";
            ModuleException me = new ModuleException(errorMessage);
            tracer.throwing(me);
            throw me;
        }
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { EDIFACT_VERSION, this.version });
        String strDuplicateCheck = moduleContext.getContextData(DUPLICATE_CHECK);
        this.duplicateCheck = !strDuplicateCheck.equals("0");
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { DUPLICATE_CHECK, "" + this.duplicateCheck });
        setUnbPartyInformation(moduleContext);
        getUnbSenderAndReceiverIds(moduleContext);
        getMessageDirection(message);
        String strSchemaValidation = moduleContext.getContextData(SCHEMA_VALIDATION);
        this.schemaValidation = !strSchemaValidation.equals("0");
        tracer.info("Module context parameter read : key = {0}, value = {1}", new Object[] { SCHEMA_VALIDATION, String.valueOf(this.schemaValidation) });
        tracer.info("Set messageType in configuration to: " + this.messageType);
        tracer.leaving();
    }
}
