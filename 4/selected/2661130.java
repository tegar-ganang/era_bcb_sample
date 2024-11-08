package com.cbsgmbh.xi.af.edifact.http.helpers;

import com.cbsgmbh.xi.af.edifact.util.EdifactUtil;
import com.cbsgmbh.xi.af.trace.helpers.BaseTracer;
import com.sap.aii.af.ra.ms.api.Message;
import com.sap.aii.af.service.administration.api.monitoring.ChannelDirection;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManager;
import com.sap.aii.af.service.administration.api.monitoring.MonitoringManagerFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContext;
import com.sap.aii.af.service.administration.api.monitoring.ProcessContextFactory;
import com.sap.aii.af.service.administration.api.monitoring.ProcessState;

public class ChannelMonitoringSapImpl implements ChannelMonitoring, Traceable {

    private BaseTracer baseTracer = null;

    private ProcessContextFactory.ParamSet ps;

    private MonitoringManager mm = null;

    private ProcessContext pc = null;

    public void processingStarted(final XiMessage xiMessage, final XiChannel xiChannel) {
        final Message sapMessage = ((XiMessageSapImpl) xiMessage).getSapMessage();
        this.mm = MonitoringManagerFactory.getInstance().getMonitoringManager();
        this.ps = ProcessContextFactory.getParamSet().message(sapMessage).channel(((XiChannelSapImpl) xiChannel).getChannel());
        if (this.ps != null) this.pc = ProcessContextFactory.getInstance().createProcessContext(this.ps);
        reportOk("Message processing started.");
    }

    public void processingSuccessfull() {
        reportOk("Message processing successfully finished.");
    }

    public void exceptionOccured() {
        reportError("Exception occured during message processing.");
    }

    public boolean canReport() {
        return (this.pc != null) && (this.mm != null);
    }

    protected void reportError(final String message) {
        if (canReport()) this.mm.reportProcessStatus(EdifactUtil.ADAPTER_NAMESPACE, EdifactUtil.ADAPTER_NAME, ChannelDirection.SENDER, ProcessState.ERROR, message, this.pc);
    }

    protected void reportOk(final String message) {
        if (canReport()) this.mm.reportProcessStatus(EdifactUtil.ADAPTER_NAMESPACE, EdifactUtil.ADAPTER_NAME, ChannelDirection.SENDER, ProcessState.OK, message, this.pc);
    }

    public BaseTracer getBaseTracer() {
        return this.baseTracer;
    }

    public void setBaseTracer(final BaseTracer baseTracer) {
        this.baseTracer = baseTracer;
    }
}
