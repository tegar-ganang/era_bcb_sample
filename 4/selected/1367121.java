package org.scribble.model;

/**
 * This class represents the list of channels declared within
 * a Scribble definition.
 */
public class ChannelList extends Activity {

    private static final long serialVersionUID = 1532837321091960033L;

    /**
	 * This method returns the list of channels.
	 * 
	 * @return The list of channels
	 */
    @Reference(containment = true)
    public java.util.List<Channel> getChannels() {
        return (m_channels);
    }

    private java.util.List<Channel> m_channels = new ContainmentList<Channel>(this, Channel.class);
}
