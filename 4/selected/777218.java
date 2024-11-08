package org.jwebsocket.plugins.channels;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.config.JWebSocketCommonConstants;
import org.jwebsocket.config.JWebSocketConfig;
import org.jwebsocket.config.JWebSocketServerConstants;
import org.jwebsocket.config.xml.ChannelConfig;
import org.jwebsocket.security.SecurityFactory;
import org.jwebsocket.security.User;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;

/**
 * Manager class responsible for all the channel operations within the
 * jWebSocket server system.
 * 
 * @author puran
 * @version $Id: ChannelManager.java 1425 2011-01-04 18:56:34Z fivefeetfurther $
 */
public class ChannelManager {

    /** id for the logger channel */
    private static final String LOGGER_CHANNEL_ID = "jws.logger.channel";

    /** id for the admin channel */
    private static final String ADMIN_CHANNEL_ID = "jws.admin.channel";

    /** settings key strings */
    private static final String USE_PERSISTENT_STORE = "use_persistent_store";

    private static final String ALLOW_NEW_CHANNELS = "allow_new_channels";

    /** persistent storage objects */
    private final ChannelStore mChannelStore = new BaseChannelStore();

    private final SubscriberStore mSubscriberStore = new BaseSubscriberStore();

    private final PublisherStore mPublisherStore = new BasePublisherStore();

    /** in-memory store maps */
    private final Map<String, Channel> mSystemChannels = new ConcurrentHashMap<String, Channel>();

    private final Map<String, Channel> mPrivateChannels = new ConcurrentHashMap<String, Channel>();

    private final Map<String, Channel> mPublicChannels = new ConcurrentHashMap<String, Channel>();

    private Map<String, String> mChannelPluginSettings = null;

    /**
	 * Logger channel that publish all the logs in jWebSocket system
	 */
    private static Channel mLoggerChannel = null;

    /**
	 * admin channel to monitor channel plugin activity
	 */
    private static Channel mAdminChannel = null;

    /** setting to check for persistent storage or not */
    public static boolean usePersistentStore;

    /** setting to check if new channel creation or registration is allowed */
    public static boolean allowNewChannels;

    private ChannelManager(Map<String, String> aSettings) {
        this.mChannelPluginSettings = new ConcurrentHashMap<String, String>(aSettings);
        String lUsePersisentStore = mChannelPluginSettings.get(USE_PERSISTENT_STORE);
        if (lUsePersisentStore != null && lUsePersisentStore.equals("true")) {
            usePersistentStore = true;
        }
        String lAllowNewChannels = mChannelPluginSettings.get(ALLOW_NEW_CHANNELS);
        if (lAllowNewChannels != null && lAllowNewChannels.equals("true")) {
            allowNewChannels = true;
        }
    }

    /**
	 * @return the static manager instance
	 */
    public static ChannelManager getChannelManager(Map<String, String> aSettings) {
        return new ChannelManager(aSettings);
    }

    /**
	 * Starts the system channels within the jWebSocket system configured via
	 * jWebSocket.xml, Note that it doesn't insert the system channels to the
	 * channel store.
	 *
	 * @throws ChannelLifeCycleException
	 *             if exception starting the system channels
	 */
    public void startSystemChannels() throws ChannelLifeCycleException {
        User lRoot = SecurityFactory.getRootUser();
        JWebSocketConfig lConfig = JWebSocketConfig.getConfig();
        for (ChannelConfig lCfg : lConfig.getChannels()) {
            if (lCfg.isSystemChannel()) {
                lCfg.validate();
                if (LOGGER_CHANNEL_ID.equals(lCfg.getId())) {
                    mLoggerChannel = new Channel(lCfg);
                    mLoggerChannel.start(lRoot.getLoginname());
                } else if (ADMIN_CHANNEL_ID.equals(lCfg.getId())) {
                    mAdminChannel = new Channel(lCfg);
                    mAdminChannel.start(lRoot.getLoginname());
                } else {
                    Channel lChannel = new Channel(lCfg);
                    lChannel.start(lRoot.getLoginname());
                    mSystemChannels.put(lChannel.getId(), lChannel);
                }
            }
        }
    }

    /**
	 * Stops all the system channels running in the system and clears the system
	 * channels map
	 */
    public void stopSystemChannels() throws ChannelLifeCycleException {
        User root = SecurityFactory.getRootUser();
        for (Map.Entry<String, Channel> entry : mSystemChannels.entrySet()) {
            Channel channel = entry.getValue();
            channel.stop(root.getLoginname());
        }
        mSystemChannels.clear();
        if (mLoggerChannel != null) {
            mLoggerChannel.stop(root.getLoginname());
        }
        if (mAdminChannel != null) {
            mAdminChannel.stop(root.getLoginname());
        }
    }

    /**
	 * Returns the channel registered in the jWebSocket system based on channel
	 * id it does a various lookup and then if it doesn't find anywhere from the
	 * memory it loads the channel from the database. If it doesn' find anything
	 * then it returns the null object
	 *
	 * @param aChannelId
	 * @return channel object, null if not found
	 */
    public Channel getChannel(String aChannelId) {
        if (mSystemChannels.containsKey(aChannelId)) {
            return mSystemChannels.get(aChannelId);
        }
        if (mPrivateChannels.containsKey(aChannelId)) {
            return mPrivateChannels.get(aChannelId);
        }
        if (mPublicChannels.containsKey(aChannelId)) {
            return mPublicChannels.get(aChannelId);
        }
        if (usePersistentStore) {
            Channel channel = mChannelStore.getChannel(aChannelId);
            if (channel != null) {
                mPublicChannels.put(aChannelId, channel);
            }
            return channel;
        }
        return null;
    }

    /**
	 * Register the given channel to the list of channels maintained by the
	 * jWebSocket system.
	 *
	 * @param aChannel
	 *            the channel to store.
	 */
    public void registerChannel(Channel aChannel) {
        if (aChannel.isSystemChannel()) {
            mSystemChannels.put(aChannel.getId(), aChannel);
        } else if (aChannel.isPrivateChannel()) {
            mPrivateChannels.put(aChannel.getId(), aChannel);
        } else {
            mPublicChannels.put(aChannel.getId(), aChannel);
        }
        if (usePersistentStore) {
            mChannelStore.storeChannel(aChannel);
        }
    }

    /**
	 * Returns the registered subscriber object for the given subscriber id
	 *
	 * @param aSubscriberId
	 *            the subscriber id
	 * @return the subscriber object
	 */
    public Subscriber getSubscriber(String aSubscriberId) {
        return mSubscriberStore.getSubscriber(aSubscriberId);
    }

    /**
	 * Stores the registered subscriber information in the channel store
	 *
	 * @param aSubscriber
	 *            the subscriber to register
	 */
    public void storeSubscriber(Subscriber aSubscriber) {
        mSubscriberStore.storeSubscriber(aSubscriber);
    }

    /**
	 * Removes the given subscriber information from channel store
	 *
	 * @param aSubscriber
	 *            the subscriber object
	 */
    public void removeSubscriber(Subscriber aSubscriber) {
        mSubscriberStore.removeSubscriber(aSubscriber.getId());
    }

    /**
	 * Returns the registered publisher for the given publisher id
	 *
	 * @param aPublisherId
	 *            the publisher id
	 * @return the publisher object
	 */
    public Publisher getPublisher(String aPublisherId) {
        return mPublisherStore.getPublisher(aPublisherId);
    }

    /**
	 * Stores the given publisher to the channel store
	 *
	 * @param publisher
	 *            the publisher object to store
	 */
    public void storePublisher(Publisher aPublisher) {
        mPublisherStore.storePublisher(aPublisher);
    }

    /**
	 * Removes the publisher from the channel store permanently
	 *
	 * @param aPublisher
	 *            the publisher to remove
	 */
    public void removePublisher(Publisher aPublisher) {
        mPublisherStore.removePublisher(aPublisher.getId());
    }

    /**
	 * Returns the instance of the logger channel.If not initialized for some
	 * reason returns null.
	 *
	 * @return the logger channel
	 */
    public static Channel getLoggerChannel() {
        return mLoggerChannel;
    }

    /**
	 * Returns the instance of the admin channel. If not initialized for some
	 * reasons returns null.
	 *
	 * @return the admin channel
	 */
    public static Channel getAdminChannel() {
        return mAdminChannel;
    }

    public void publishToLoggerChannel(Token aToken) {
        Channel lLoggerChannel = getLoggerChannel();
        if (lLoggerChannel != null) {
            lLoggerChannel.broadcastToken(aToken);
        }
    }

    /**
	 * Returns the error token
	 *
	 * @param aConnector
	 *            the target connector object
	 * @param aChannelId
	 *            the channelId
	 * @param aMessage
	 *            the error message
	 * @return the error token
	 */
    public Token getErrorToken(WebSocketConnector aConnector, String aChannelId, String aMessage) {
        Token logToken = getBaseChannelResponse(aConnector, aChannelId);
        logToken.setString("event", "error");
        logToken.setString("error", aMessage);
        return logToken;
    }

    /**
	 * Returns the basic response token for a channel
	 *
	 * @param aConnector
	 *            the target connector object
	 * @param aChannel
	 *            the target channel
	 * @return the base token of type channel
	 */
    public Token getBaseChannelResponse(WebSocketConnector aConnector, String aChannel) {
        Token channelToken = TokenFactory.createToken("channel");
        channelToken.setString("sourceId", aConnector.getId());
        channelToken.setString("channelId", aChannel);
        return channelToken;
    }

    public Token getChannelSuccessToken(WebSocketConnector aConnector, String aChannel, ChannelEventEnum aEventType) {
        Token lToken = getBaseChannelResponse(aConnector, aChannel);
        String lEvent = "";
        switch(aEventType) {
            case LOGIN:
                lEvent = "login";
                break;
            case AUTHORIZE:
                lEvent = "authorize";
                break;
            case PUBLISH:
                lEvent = "publish";
                break;
            case SUSCRIBE:
                lEvent = "subscribe";
            case UNSUSCRIBE:
                lEvent = "unsuscribe";
                break;
            default:
                break;
        }
        lToken.setString("event", lEvent);
        lToken.setString("status", "ok");
        return lToken;
    }

    /**
	 * @return the system channels
	 */
    public Map<String, Channel> getSystemChannels() {
        return Collections.unmodifiableMap(mSystemChannels);
    }

    /**
	 * @return the private channels
	 */
    public Map<String, Channel> getPrivateChannels() {
        return Collections.unmodifiableMap(mPrivateChannels);
    }

    /**
	 * @return the public channels
	 */
    public Map<String, Channel> getPublicChannels() {
        return Collections.unmodifiableMap(mPublicChannels);
    }
}
