package de.schwarzrot.epgmgr.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import ca.odell.glazedlists.matchers.Matcher;
import de.schwarzrot.vdr.data.domain.EpgEvent;

public class Event4ChannelMatcher implements Matcher<EpgEvent> {

    private Set<String> channels = new HashSet<String>();

    public Event4ChannelMatcher(Collection<String> channels) {
        this.channels.addAll(channels);
    }

    public boolean matches(EpgEvent evt) {
        if (evt == null) return false;
        if (channels.isEmpty()) return true;
        String channel = evt.getChannel().getName();
        return channels.contains(channel);
    }
}
