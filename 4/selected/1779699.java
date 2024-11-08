package org.slasoi.infrastructure.monitoring.pubsub.listeners;

import org.apache.log4j.Logger;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubManager;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.infrastructure.monitoring.InfrastructureMonitoringAgent;
import org.slasoi.infrastructure.monitoring.pubsub.ErrorResponse;
import org.slasoi.infrastructure.monitoring.pubsub.PubSubRequest;
import org.slasoi.infrastructure.monitoring.pubsub.PubSubResponse;
import org.slasoi.infrastructure.monitoring.pubsub.handlers.ConfigureMonitoringHandler;
import org.slasoi.infrastructure.monitoring.pubsub.handlers.MonitoringFeaturesRequestHandler;
import org.slasoi.infrastructure.monitoring.pubsub.messages.MonitoringFeaturesRequest;
import org.slasoi.infrastructure.monitoring.pubsub.messages.RegisterServiceRequest;
import org.slasoi.infrastructure.monitoring.pubsub.messages.RemoveServiceRequest;
import org.slasoi.infrastructure.monitoring.utils.Utils;
import java.util.Date;

public class ConfigurationChannelListener implements org.slasoi.common.messaging.pubsub.MessageListener {

    private static Logger log = Logger.getLogger(ConfigurationChannelListener.class);

    public void processMessage(MessageEvent messageEvent) {
        String payload = null;
        PubSubRequest request = null;
        try {
            PubSubMessage message = messageEvent.getMessage();
            payload = message.getPayload();
            log.trace(String.format("New message arrived on the channel %s. Message payload: %s", message.getChannelName(), payload));
            try {
                request = PubSubRequest.fromJson(payload);
            } catch (Exception e) {
                throw new Exception(String.format("Invalid pubsub message received: %s", e.getMessage()));
            }
            log.trace("Request type: " + request.getRequestType());
            PubSubResponse response;
            String requestType = request.getRequestType();
            if (requestType.equals("RegisterServiceRequest")) {
                RegisterServiceRequest registerServiceRequest = RegisterServiceRequest.fromJson(payload);
                response = ConfigureMonitoringHandler.registerService(registerServiceRequest);
            } else if (requestType.equals("MonitoringFeaturesRequest")) {
                MonitoringFeaturesRequest monitoringFeaturesRequest = MonitoringFeaturesRequest.fromJson(payload);
                response = MonitoringFeaturesRequestHandler.processRequest(monitoringFeaturesRequest);
            } else if (requestType.equals("RemoveServiceRequest")) {
                RemoveServiceRequest removeSrvReq = RemoveServiceRequest.fromJson(payload);
                response = ConfigureMonitoringHandler.removeService(removeSrvReq);
            } else {
                return;
            }
            String responsePayload = response.toJson();
            publishMessage(responsePayload);
            log.trace(String.format("Request %s processed successfully.", request.getMessageId()));
            if (log.isTraceEnabled()) {
                log.trace("Response:\n" + responsePayload.substring(0, Math.min(responsePayload.length(), 1000)));
            }
        } catch (Exception e) {
            log.error(String.format("Error encountered while processing PubSub request %s: %s", Utils.shortenString(payload, 1000), e.getMessage()));
            ErrorResponse errorResponse = new ErrorResponse();
            String inReplyTo = "";
            if (request != null) {
                inReplyTo = request.getMessageId();
            }
            errorResponse.setInReplyTo(inReplyTo);
            errorResponse.setTimestamp(new Date());
            errorResponse.setExceptionMessage((e.getMessage() != null) ? e.getMessage() : e.toString());
            try {
                String responsePayload = errorResponse.toJson();
                publishMessage(responsePayload);
            } catch (Exception e1) {
                log.error("Error encountered while sending error response: " + e.getMessage());
            }
        }
    }

    private void publishMessage(String payload) throws MessagingException {
        PubSubManager pubSubManager = InfrastructureMonitoringAgent.getInstance().getPubSubManager();
        Channel monitoringDataRequestChannel = InfrastructureMonitoringAgent.getInstance().getConfigurationChannel();
        PubSubMessage pubSubMessage = new PubSubMessage(monitoringDataRequestChannel.getName(), payload);
        pubSubManager.publish(pubSubMessage);
    }
}
