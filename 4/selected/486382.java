package de.schwarzrot.epgmgr.support;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEvent;
import de.schwarzrot.vdr.data.domain.EpgEvent;

public class Event2ChannelList extends TransformedList<EpgEvent, String> {

    public Event2ChannelList(EventList<EpgEvent> source) {
        super(source);
        source.addListEventListener(this);
    }

    @Override
    public void listChanged(ListEvent<EpgEvent> listChanges) {
        updates.forwardEvent(listChanges);
    }

    @Override
    public String get(int idx) {
        return source.get(idx).getChannel().getName();
    }

    @Override
    protected boolean isWritable() {
        return false;
    }
}
