package xtom.parser.examples.rss;

/**
 * The class that holds the RSS Feed.
 * @author taras
 * @version $Revision: 1.1 $
 * @since
 */
public class Feed {

    String version;

    Channel channel;

    public Feed(Channel c, String v) {
        this.version = v;
        this.channel = c;
    }

    /**
	 * 
	 */
    public Feed() {
    }

    /**
	 * @return Returns the channel.
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * @param channel The channel to set.
	 */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
	 * @return Returns the version.
	 */
    public String getVersion() {
        return version;
    }

    /**
	 * @param version The version to set.
	 */
    public void setVersion(String version) {
        this.version = version;
    }
}
