package com.cbsgmbh.xi.af.edifact.http.helpers;

import java.io.IOException;
import com.cbsgmbh.xi.af.edifact.util.EdifactUtil;
import com.cbsgmbh.xi.af.edifact.util.ModuleExceptionEEDM;
import com.cbsgmbh.xi.af.edifact.util.ModuleUtil;
import com.cbsgmbh.xi.af.http.util.HTTPUtil;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.cbsgmbh.xi.af.trace.helpers.Tracer;
import com.sap.aii.af.mp.module.ModuleException;
import com.sap.aii.af.mp.processor.ModuleProcessorLocal;
import com.sap.aii.af.service.cpa.CPAException;
import com.sap.aii.af.service.cpa.CPAObjectType;
import com.sap.aii.af.service.cpa.Channel;
import com.sap.aii.af.service.cpa.InboundRuntimeLookup;
import com.sap.aii.af.service.cpa.LookupManager;

public class XiChannelLookupSapImpl implements XiChannelLookup, Traceable {

    private BaseTracer baseTracer = null;

    private final ModuleProcessorLocal moduleProcessor;

    public XiChannelLookupSapImpl(ModuleProcessorLocal moduleProcessor) {
        this.moduleProcessor = moduleProcessor;
    }

    public XiChannel lookupChannel(final RequestData requestData, final String fromAS2Party, final String toAS2Party) throws ModuleExceptionEEDM, IOException, CPAException {
        final Tracer tracer = this.baseTracer.entering("lookupChannel(final RequestData requestData, final String fromAS2Party, final String toAS2Party)");
        final String channelId = getChannelID(requestData, fromAS2Party, toAS2Party);
        final LookupManager lookupManager = LookupManager.getInstance();
        tracer.info("LookupManager lookupManager created", lookupManager);
        final Channel channel = (Channel) lookupManager.getCPAObject(CPAObjectType.CHANNEL, channelId);
        tracer.info("Channel channel created", channel);
        tracer.leaving();
        return new XiChannelSapImpl(channelId, channel, this.moduleProcessor);
    }

    /**
     * Internal helper method to normalize 'from' and 'to' field of multipart
     * message Lookup of channelID via InboundRuntimeLookup
     * 
     * @throws IOException,
     *             CPAException, ModuleExceptionEEDM
     */
    protected String getChannelID(final RequestData requestData, final String xiPartyFrom, final String xiPartyTo) throws IOException, CPAException, ModuleExceptionEEDM {
        final Tracer tracer = this.baseTracer.entering("getChannelID(final RequestData requestData, final String xiPartyFrom, final String xiPartyTo)");
        String channelID = null;
        Channel channelName = null;
        InboundRuntimeLookup channelLookup = null;
        channelLookup = new InboundRuntimeLookup(EdifactUtil.ADAPTER_NAME, EdifactUtil.ADAPTER_NAMESPACE, xiPartyFrom, xiPartyTo, requestData.getRequestFromService(), requestData.getRequestToService(), requestData.getRequestInterface(), requestData.getRequestNamespace());
        if (channelLookup.getChannel() != null) {
            tracer.info("Channel lookup successful");
            channelName = channelLookup.getChannel();
            channelID = channelName.getObjectId();
            tracer.info("InboundRuntimeLookup channel retrieved {0}", channelID);
        } else {
            tracer.error("The channel cannot be determined. Reason: No agreement binding for the combination available: " + "AN {0}, ANS {1}, FP {2}, TP {3}, FS {4}, TS {5}, IF {6}, NS {7}", new Object[] { EdifactUtil.ADAPTER_NAME, EdifactUtil.ADAPTER_NAMESPACE, xiPartyFrom, xiPartyTo, requestData.getRequestFromService(), requestData.getRequestToService(), requestData.getRequestInterface(), requestData.getRequestNamespace() });
        }
        tracer.leaving();
        return channelID;
    }

    public BaseTracer getBaseTracer() {
        return this.baseTracer;
    }

    public void setBaseTracer(final BaseTracer baseTracer) {
        this.baseTracer = baseTracer;
    }
}
