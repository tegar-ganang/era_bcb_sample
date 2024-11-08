package de.nava.informa.impl.castorjdo;

import java.util.Collection;
import de.nava.informa.core.ChannelGroupIF;

/**
 * Implementation for Castor's JDO of the ChannelGroupIF interface.
 * 
 * @author Niko Schmuck (niko@nava.de) 
 */
public class ChannelGroup extends de.nava.informa.impl.basic.ChannelGroup implements ChannelGroupIF, java.io.Serializable {

    public ChannelGroup() {
        super();
    }

    public ChannelGroup(String name) {
        super(name);
    }

    public void addChannel(Channel channel) {
        add(channel);
    }

    public Collection getChannels() {
        return getAll();
    }

    public void addChildren(ChannelGroup channelGroup) {
        addChild(channelGroup);
    }
}
