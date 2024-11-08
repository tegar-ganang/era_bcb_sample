package net.sf.appia.jgcs.protocols.top;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.jgcs.AppiaMessage;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;

public class JGCSGroupEvent extends GroupSendableEvent {

    public JGCSGroupEvent(Channel channel, int dir, Session source, Group group, ViewID view_id) throws AppiaEventException {
        super(channel, dir, source, group, view_id);
    }

    public JGCSGroupEvent(AppiaMessage message) {
        super(message);
    }

    public JGCSGroupEvent() {
        super(new AppiaMessage());
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " SourceSession: " + this.getSourceSession() + " Direction: " + (this.getDir() == Direction.UP ? "UP" : "DOWN") + " Channel: " + this.getChannel().getChannelID();
    }
}
