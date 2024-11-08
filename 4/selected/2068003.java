package de.nava.informa.impl.jdo;

import java.util.Collection;
import de.nava.informa.core.ChannelGroupIF;
import de.nava.informa.core.ChannelIF;

/**
 * Implementation for Castor's JDO of the ChannelGroupIF interface.
 * 
 * @author Niko Schmuck (niko@nava.de) 
 */
public class ChannelGroup extends de.nava.informa.impl.basic.ChannelGroup implements ChannelGroupIF, java.io.Serializable {

    public ChannelGroup() {
        super();
    }

    public void addChannel(Channel channel) {
        add((ChannelIF) channel);
    }

    public Collection getChannels() {
        return channels;
    }
}
