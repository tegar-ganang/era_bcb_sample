package org.granite.tide.seam;

import static org.jboss.seam.annotations.Install.FRAMEWORK;
import org.granite.gravity.Gravity;
import org.granite.tide.async.AsyncPublisher;
import org.granite.tide.async.TideChannel;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.ServletLifecycle;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.Message;

/**
 * Async publisher using Gravity to send messages to the client
 * 
 * @author William DRAI
 */
@Name("org.granite.tide.seam.async.publisher")
@Install(precedence = FRAMEWORK, classDependencies = { "org.granite.gravity.Gravity" })
@Scope(ScopeType.STATELESS)
@BypassInterceptors
@AutoCreate
public class SeamAsyncPublisher implements AsyncPublisher {

    private static final long serialVersionUID = -5395975397632138270L;

    public static final String DESTINATION_NAME = "seamAsync";

    private Gravity getGravity() {
        return (Gravity) ServletLifecycle.getServletContext().getAttribute("org.granite.gravity.Gravity");
    }

    private TideChannel getChannel(String sessionId) {
        TideChannel channel = null;
        Gravity gravity = getGravity();
        if (gravity != null) {
            String channelId = "tide.channel." + sessionId;
            channel = (TideChannel) gravity.getChannel(channelId);
            if (channel == null) {
                channel = new TideChannel(gravity);
                gravity.registerChannel(channel);
            }
        }
        return channel;
    }

    public void publishMessage(String sessionId, Object body) {
        TideChannel channel = getChannel(sessionId);
        if (channel != null) {
            Message message = new AsyncMessage();
            message.setHeader(AsyncMessage.SUBTOPIC_HEADER, "tide.events." + sessionId);
            message.setClientId(channel.getClientId());
            message.setDestination(DESTINATION_NAME);
            message.setBody(body);
            getGravity().publishMessage(channel, message);
        }
    }
}
