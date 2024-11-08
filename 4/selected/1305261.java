package com.sun.sgs.tutorial.server.lesson6;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedReference;

/**
 * Simple example of channel operations in the Project Darkstar Server.
 * <p>
 * Extends the {@code HelloEcho} example by joining clients to two
 * channels.
 */
public class HelloChannels implements Serializable, AppListener {

    /** The version of the serialized form of this class. */
    private static final long serialVersionUID = 1L;

    /** The {@link Logger} for this class. */
    private static final Logger logger = Logger.getLogger(HelloChannels.class.getName());

    static final String CHANNEL_1_NAME = "Foo";

    static final String CHANNEL_2_NAME = "Bar";

    /** 
     * The first {@link Channel}.  The second channel is looked up
     * by name.
     */
    private ManagedReference<Channel> channel1 = null;

    /**
     * {@inheritDoc}
     * <p>
     * Creates the channels.  Channels persist across server restarts,
     * so they only need to be created here in {@code initialize}.
     */
    public void initialize(Properties props) {
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel c1 = channelMgr.createChannel(CHANNEL_1_NAME, null, Delivery.RELIABLE);
        channel1 = AppContext.getDataManager().createReference(c1);
        channelMgr.createChannel(CHANNEL_2_NAME, new HelloChannelsChannelListener(), Delivery.RELIABLE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a {@link HelloChannelsSessionListener} for the
     * logged-in session.
     */
    public ClientSessionListener loggedIn(ClientSession session) {
        logger.log(Level.INFO, "User {0} has logged in", session.getName());
        return new HelloChannelsSessionListener(session, channel1);
    }
}
