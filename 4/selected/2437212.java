package com.webreach.mirth.connectors.ihe;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import com.misyshealthcare.connect.ihe.configuration.ConfigurationLoader;
import com.webreach.mirth.model.MessageObject;
import com.webreach.mirth.server.Constants;
import com.webreach.mirth.server.controllers.AlertController;
import com.webreach.mirth.server.controllers.MessageObjectController;
import com.webreach.mirth.server.controllers.MonitoringController;
import com.webreach.mirth.server.controllers.MonitoringController.ConnectorType;
import com.webreach.mirth.server.controllers.MonitoringController.Event;
import com.webreach.mirth.server.util.CompiledScriptCache;
import com.webreach.mirth.server.util.JavaScriptScopeUtil;

public class IheMessageDispatcher extends AbstractMessageDispatcher {

    private IheConnector connector;

    private CompiledScriptCache compiledScriptCache = CompiledScriptCache.getInstance();

    private MessageObjectController messageObjectController = MessageObjectController.getInstance();

    private AlertController alertController = AlertController.getInstance();

    private MonitoringController monitoringController = MonitoringController.getInstance();

    private static ConnectorType CONNECTOR_TYPE = ConnectorType.WRITER;

    public IheMessageDispatcher(IheConnector connector) {
        super(connector);
        this.connector = connector;
        monitoringController.updateStatus(connector, CONNECTOR_TYPE, Event.INITIALIZED);
    }

    public void doDispatch(UMOEvent event) throws Exception {
        monitoringController.updateStatus(connector, CONNECTOR_TYPE, Event.BUSY);
        if (logger.isDebugEnabled()) {
            logger.debug("Dispatch event: " + event);
        }
        MessageObject messageObject = messageObjectController.getMessageObjectFromEvent(event);
        if (messageObject == null) {
            return;
        }
        try {
            Context context = Context.enter();
            Scriptable scope = new ImporterTopLevel(context);
            JavaScriptScopeUtil.buildScope(scope, messageObject, logger);
            scope.put("configuration", scope, ConfigurationLoader.getInstance());
            Script compiledScript = compiledScriptCache.getCompiledScript(this.connector.getScriptId());
            if (compiledScript == null) {
                logger.warn("script could not be found in cache");
                messageObjectController.setError(messageObject, Constants.ERROR_414, "Script not found in cache", null);
            } else {
                compiledScript.exec(context, scope);
                String response = "Script execution successful";
                if (messageObject.getResponseMap().containsKey(messageObject.getConnectorName())) {
                    response = (String) messageObject.getResponseMap().get(messageObject.getConnectorName());
                }
                messageObjectController.setSuccess(messageObject, response);
            }
        } catch (Exception e) {
            logger.debug("Error dispatching event: " + e.getMessage(), e);
            alertController.sendAlerts(((IheConnector) connector).getChannelId(), Constants.ERROR_414, "Error executing script", e);
            messageObjectController.setError(messageObject, Constants.ERROR_414, "Error executing script: ", e);
            connector.handleException(e);
        } finally {
            monitoringController.updateStatus(connector, CONNECTOR_TYPE, Event.DONE);
        }
    }

    public void doDispose() {
    }

    public UMOMessage doSend(UMOEvent event) throws Exception {
        doDispatch(event);
        return event.getMessage();
    }

    public Object getDelegateSession() throws UMOException {
        return null;
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        return null;
    }
}
