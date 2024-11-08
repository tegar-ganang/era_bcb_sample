package org.actioncenters.listeners.contributionsservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.actioncenters.cometd.clientservlet.CometClientServlet;
import org.actioncenters.core.contribution.data.IUserMessage;
import org.actioncenters.core.contribution.data.IValueObject;
import org.apache.log4j.Logger;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerSession;

/**
 * This class is a common ancestor for listeners and provides implementation of methods used by all listeners.
 *
 * @author amametjanov
 */
public abstract class AbstractListener {

    /** Logger. */
    private static Logger log = Logger.getLogger(AbstractListener.class.getName());

    /**
     * Checks if someone is listening on the specified channel.
     *
     * @param channel
     *            the channel
     *
     * @return true, if someone is listening
     */
    protected boolean isAnybodyListening(String channel) {
        return true;
    }

    /**
     * Publish.
     *
     * @param vos the vos
     * @param channelName the channel name
     */
    protected void publish(List<IValueObject> vos, String channelName) {
        if (log.isDebugEnabled()) {
            log.debug("Publishing Message. Channel = " + channelName + " Value Object = " + vos.get(0).toString());
        }
        if (!channelName.contains("//")) {
            if (CometClientServlet.getBayeux() != null) {
                ServerChannel channel = CometClientServlet.getBayeux().getChannel(channelName);
                if (channel == null && hasWildcardListener(channelName)) {
                    CometClientServlet.getBayeux().createIfAbsent(channelName, new ConfigurableServerChannel.Initializer() {

                        @Override
                        public void configureChannel(ConfigurableServerChannel channel) {
                        }
                    });
                    channel = CometClientServlet.getBayeux().getChannel(channelName);
                }
                if (channel != null) {
                    Map<String, Object> msg = JSONConverter.convertToJSON(vos.get(0));
                    for (int i = 1; i < vos.size(); i++) {
                        msg.put("msg" + (i + 1), JSONConverter.convertToJSON(vos.get(i)));
                    }
                    channel.publish(CometClientServlet.getClient(), msg, channelName);
                }
            }
        }
    }

    /**
     * Publish the specified contribution on the specified channel.
     *
     * @param vo
     *            the value object
     * @param channelName
     *            the channel name
     */
    protected void publish(IValueObject vo, String channelName) {
        List<IValueObject> vos = new ArrayList<IValueObject>();
        vos.add(vo);
        publish(vos, channelName);
    }

    /**
     * Checks if the specified channel name has a wildcard (**) in it.
     *
     * @param channelName a channel name
     * @return true, if the channel has a wildcard, and false otherwise
     */
    private boolean hasWildcardListener(String channelName) {
        boolean returnValue = false;
        String wildcardString = "/";
        StringTokenizer tokenizer = new StringTokenizer(channelName, "/");
        while (tokenizer.hasMoreTokens() && !returnValue) {
            wildcardString = wildcardString + tokenizer.nextToken() + "/";
            returnValue = CometClientServlet.getBayeux().getChannel(wildcardString + "**") != null;
        }
        return returnValue;
    }

    /**
     * Notifies a client with the specified user message on the specified channel.
     *
     * @param userMessage
     *          the user message
     * @param channelName
     *          the channel name
     */
    protected void notify(IUserMessage userMessage, String channelName) {
        if (log.isDebugEnabled()) {
            log.debug("Sending notification.  Channel = " + channelName + " Value Object = " + userMessage.toString());
        }
        ServerSession client = CometClientServlet.getBayeux().getSession(userMessage.getClientId());
        client.deliver(CometClientServlet.getClient(), channelName, JSONConverter.convertToJSON(userMessage), null);
    }
}
