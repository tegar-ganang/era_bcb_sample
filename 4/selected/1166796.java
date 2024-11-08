package net.jetrix.messages;

import net.jetrix.*;
import java.util.*;

/**
 * List of spectators in a channel.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public class SpectatorListMessage extends Message {

    private String channel;

    private Collection<String> spectators;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Collection<String> getSpectators() {
        return spectators;
    }

    public void setSpectators(Collection<String> spectators) {
        this.spectators = spectators;
    }
}
