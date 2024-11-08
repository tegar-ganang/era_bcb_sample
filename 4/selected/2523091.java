package jather;

import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.ChannelFactory;
import org.jgroups.JChannel;
import org.jgroups.JChannelFactory;

/**
 * The common method implementations for members of a JGroup.
 * 
 * @author neil
 * 
 */
public class GroupMember {

    /**
	 * The default channel name to use if not set.
	 */
    public static final String DEFAULT_CLUSTER_NAME = "Jather";

    private static final Log log = LogFactory.getLog(GroupMember.class);

    private ChannelFactory channelFactory;

    private JChannel channel;

    private URL channelUrlProps;

    private String channelStringProps;

    private String clusterName;

    /**
	 * The factory for creating new channels.
	 * 
	 * @return the channelFactory
	 */
    public ChannelFactory getChannelFactory() throws ChannelException {
        if (channelFactory == null) {
            if (getChannelUrlProps() != null) {
                channelFactory = new JChannelFactory(getChannelUrlProps());
            } else if (getChannelStringProps() != null) {
                channelFactory = new JChannelFactory(getChannelStringProps());
            } else {
                channelFactory = new JChannelFactory(new JChannel().getProperties());
            }
        }
        return channelFactory;
    }

    /**
	 * Get the current channel or create a new one.
	 * 
	 * @return the channel
	 */
    public JChannel getChannel() throws ChannelException {
        if (channel == null) {
            channel = (JChannel) getChannelFactory().createChannel();
            channel.setOpt(Channel.AUTO_RECONNECT, true);
            channel.setOpt(Channel.AUTO_GETSTATE, true);
        }
        return channel;
    }

    /**
	 * Get the connected channel, creating a new one and connecting it if required.
	 * 
	 * @return the connected channel
	 */
    public JChannel getConnectedChannel() throws ChannelException {
        JChannel channel = getChannel();
        if (!channel.isConnected()) {
            log.debug("Connecting channel:" + channel);
            channel.connect(getClusterName());
        }
        return channel;
    }

    /**
	 * Set the current channel.
	 * 
	 * @param channel
	 *            the channel to set
	 */
    protected void setChannel(JChannel channel) {
        this.channel = channel;
    }

    /**
	 * Get the URL based properties to use for this channel.
	 * 
	 * @return the channelUrlProps which may be null if the channel does not use
	 *         URL properties.
	 */
    public URL getChannelUrlProps() {
        return channelUrlProps;
    }

    /**
	 * Set the URL based properties to use for this channel.
	 * 
	 * @param channelUrlProps
	 *            the channelUrlProps to set or null if they are not to be used.
	 */
    public void setChannelUrlProps(URL channelUrlProps) {
        this.channelUrlProps = channelUrlProps;
    }

    /**
	 * Get the string based properties to use for this channel.
	 * 
	 * @return the channelStringProps which may be null if the channel does not
	 *         use explicit string properties.
	 */
    public String getChannelStringProps() {
        return channelStringProps;
    }

    /**
	 * Set the string based properties to use for this channel.
	 * 
	 * @param channelStringProps
	 *            the channelStringProps to set or null if they are not to be
	 *            used.
	 */
    public void setChannelStringProps(String channelStringProps) {
        this.channelStringProps = channelStringProps;
    }

    /**
	 * Set the channel factory to use.
	 * 
	 * @param channelFactory
	 *            the channelFactory to set
	 */
    protected void setChannelFactory(ChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    /**
	 * @return the clusterName
	 */
    public String getClusterName() {
        if (clusterName == null) {
            clusterName = DEFAULT_CLUSTER_NAME;
        }
        return clusterName;
    }

    /**
	 * @param clusterName the clusterName to set
	 */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
	 * Close channel if open.
	 */
    public void closeChannel() {
        if (channel != null && channel.isOpen()) {
            channel.close();
            channel = null;
        }
    }
}
