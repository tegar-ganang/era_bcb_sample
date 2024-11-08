package org.slasoi.common.messaging.pubsub;

import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.util.Base64;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class PubSubManager {

    private static final Logger log = Logger.getLogger(PubSubManager.class);

    private static final String CHANNEL_NAME_PREFIX = "org.slasoi.";

    private EncodingStrategy encodingStrategy;

    private Encode messageEncode;

    private final Settings settings;

    private Listener listener;

    protected Settings getSettings() {
        return settings;
    }

    public PubSubManager(Settings settings) throws MessagingException {
        listener = new Listener();
        this.settings = settings;
        setupEncode();
    }

    /**
     * Connects to the server.
     * Note that this method is typically already called in constructor.
     *
     * @throws MessagingException
     */
    protected abstract void connect() throws MessagingException;

    /**
     * Gets connection id.
     *
     * @return connection id.
     */
    public abstract String getId() throws MessagingException;

    /**
     * Creates new channel to publish messages to.
     *
     * @param channel name of the new channel.
     */
    public abstract void createChannel(Channel channel) throws MessagingException;

    /**
     * Creates a channel if not exist and subscribes to it.
     *
     * @param channel Channel to create and subscribe to.
     * @throws MessagingException
     */
    public void createAndSubscribe(Channel channel) throws MessagingException {
        if (!isChannel(channel.getName())) {
            createChannel(channel);
            log.debug(String.format("Channel %s created.", channel.getName()));
        }
        subscribe(channel.getName());
        log.debug(String.format("Channel %s subscribed.", channel.getName()));
    }

    /**
     * Checks if channel already exists.
     *
     * @param channel name of the channel.
     * @return true if channel exists, otherwise false.
     */
    public abstract boolean isChannel(String channel) throws MessagingException;

    /**
     * Deletes a channel.
     *
     * @param channel name of the channel to delete.
     */
    public abstract void deleteChannel(String channel) throws MessagingException;

    /**
     * Publishes the message. The channel the message is published to is defined in the message.
     *
     * @param message message to be published.
     */
    public abstract void publish(PubSubMessage message) throws MessagingException;

    /**
     * Subscribes to the specified channel.
     *
     * @param channel channel to subscribe to.
     */
    public abstract void subscribe(String channel) throws MessagingException;

    /**
     * Gets a list of the current subscriptions for the logged in user's JID
     *
     * @return a list of subscriptions
     * @throws MessagingException
     */
    public abstract List<Subscription> getSubscriptions() throws MessagingException;

    /**
     * Unsubscribes from the specified channel.
     *
     * @param channel channel to unsubscribe from.
     * @throws MessagingException
     */
    public abstract void unsubscribe(String channel) throws MessagingException;

    /**
     * Adds a message listener that filters messages based on channel name.
     *
     * @param messageListener message listener.
     * @param channelNames    list of channels to filter. If this parameter is null, no messages are filtered.
     * @throws MessagingException
     */
    public void addMessageListener(MessageListener messageListener, String[] channelNames) throws MessagingException {
        listener.addListener(messageListener, channelNames);
    }

    /**
     * Adds a message listener that does not filter any messages.
     *
     * @param messageListener message listener.
     */
    public void addMessageListener(MessageListener messageListener) throws MessagingException {
        addMessageListener(messageListener, null);
    }

    /**
     * Removes message listener.
     *
     * @param messageListener message listener.
     */
    public void removeMessageListener(MessageListener messageListener) {
        listener.removeListener(messageListener);
    }

    /**
     * Closes the connection.
     */
    public abstract void close() throws MessagingException;

    /**
     * Gets the value of the setting.
     *
     * @param setting setting.
     * @return the value of the setting
     */
    public String getSetting(Setting setting) {
        return settings.getSetting(setting);
    }

    /**
     * Sets the value of the setting.
     *
     * @param setting setting.
     * @param value   value of the setting.
     */
    public void setSetting(Setting setting, String value) {
        settings.setSetting(setting, value);
    }

    protected void fireMessageEvent(MessageEvent messageEvent) {
        listener.fireMessageEvent(messageEvent);
    }

    protected String prepareChannelName(String channelName) {
        return CHANNEL_NAME_PREFIX + channelName;
    }

    protected String extractChannelName(String channelName) {
        return channelName.substring(CHANNEL_NAME_PREFIX.length(), channelName.length());
    }

    /**
     * Encodes the message to string for transferring.
     *
     * @param message message.
     * @return Encoded message.
     */
    protected String encode(PubSubMessage message) {
        return messageEncode.encode(message);
    }

    /**
     * Decodes the message.
     *
     * @param message message to decode.
     * @return decoded message.
     */
    protected PubSubMessage decode(String message) {
        return messageEncode.decode(message);
    }

    private void setupEncode() {
        String setting = getSetting(Setting.encryption);
        encodingStrategy = EncodingStrategy.BASE64;
        if (setting.equals("xml")) {
            encodingStrategy = EncodingStrategy.XSTREAM;
        } else if (setting.equals("base64")) {
            encodingStrategy = EncodingStrategy.BASE64;
        }
        switch(encodingStrategy) {
            case BASE64:
                messageEncode = new Base64Encode();
                break;
            case XSTREAM:
                messageEncode = new XStreamEncode();
                break;
        }
    }

    private enum EncodingStrategy {

        XSTREAM, BASE64
    }

    private interface Encode {

        public String encode(PubSubMessage message);

        public PubSubMessage decode(String message);
    }

    private class XStreamEncode implements Encode {

        private final XStream xstream;

        public XStreamEncode() {
            xstream = new XStream();
        }

        public String encode(PubSubMessage message) {
            return xstream.toXML(message);
        }

        public PubSubMessage decode(String message) {
            return (PubSubMessage) xstream.fromXML(message);
        }
    }

    private class Base64Encode implements Encode {

        public String encode(PubSubMessage message) {
            return Base64.encodeObject(message);
        }

        public PubSubMessage decode(String message) {
            return (PubSubMessage) Base64.decodeToObject(message);
        }
    }

    private class Listener {

        private HashMap<MessageListener, List<String>> listeners;

        public Listener() {
            listeners = new HashMap<MessageListener, List<String>>();
        }

        public void addListener(MessageListener messageListener, String[] channels) {
            if (channels != null) {
                ArrayList<String> channelsToAdd = new ArrayList<String>();
                for (String channel : channels) {
                    channelsToAdd.add(channel);
                }
                listeners.put(messageListener, channelsToAdd);
            } else {
                listeners.put(messageListener, null);
            }
        }

        public void removeListener(MessageListener messageListener) {
            listeners.remove(messageListener);
        }

        public void fireMessageEvent(MessageEvent messageEvent) {
            Iterator<MessageListener> itor = listeners.keySet().iterator();
            while (itor.hasNext()) {
                MessageListener messageListener = itor.next();
                List<String> channels = listeners.get(messageListener);
                if (channels != null) {
                    if (channels.contains(messageEvent.getMessage().getChannelName())) {
                        messageListener.processMessage(messageEvent);
                    }
                } else {
                    messageListener.processMessage(messageEvent);
                }
            }
        }
    }
}
