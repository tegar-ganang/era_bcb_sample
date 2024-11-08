package org.granite.context;

import java.util.HashMap;
import java.util.Map;
import flex.messaging.messages.Message;

/**
 * @author Franck WOLFF
 */
public abstract class AMFContext {

    private Map<String, Object> customResponseHeaders = new HashMap<String, Object>();

    public abstract Message getRequest();

    public String getChannelId() {
        Message message = getRequest();
        if (message != null) {
            Object id = message.getHeader(Message.ENDPOINT_HEADER);
            if (id instanceof String) return (String) id;
        }
        return null;
    }

    public Map<String, Object> getCustomResponseHeaders() {
        return customResponseHeaders;
    }
}
