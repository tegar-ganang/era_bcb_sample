package net.sf.appia.jgcs.protocols.services;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.loopBack.LoopBackLayer;

/**
 * Class LoopBackSession provides the dynamic behavior for the
 * LoopBack Protocol
 *
 * @author Hugo Miranda
 * @see    LoopBackLayer
 * @see    Session
 */
public class FifoLoopBackSession extends Session {

    private int myRank;

    private Endpt myEndpt;

    /**
	 * Default Constructor.
	 */
    public FifoLoopBackSession(Layer l) {
        super(l);
    }

    /**
	 * Event Dispatcher.
	 * @param e Received event.
	 */
    public void handle(Event e) {
        if (e instanceof ChannelInit) {
        }
        if (e instanceof View) {
            myRank = ((View) e).ls.my_rank;
            myEndpt = ((View) e).vs.view[myRank];
        }
        if (e instanceof Send) {
            SendableEvent ev = (SendableEvent) e;
            if (ev.dest != null && ((int[]) ev.dest)[0] == myRank) {
                handleGroupSendableEvent((GroupSendableEvent) e);
                return;
            }
        } else {
            if (e instanceof GroupSendableEvent && e.getDir() == Direction.DOWN) {
                handleGroupSendableEvent((GroupSendableEvent) e);
            }
        }
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println("Error sending event");
        }
    }

    private void handleGroupSendableEvent(GroupSendableEvent e) {
        GroupSendableEvent cloned = null;
        try {
            cloned = (GroupSendableEvent) e.cloneEvent();
        } catch (CloneNotSupportedException ex) {
            System.err.println("Error sending event");
        }
        cloned.setDir(Direction.invert(cloned.getDir()));
        cloned.setSourceSession(this);
        cloned.dest = e.source;
        cloned.source = myEndpt;
        cloned.orig = myRank;
        cloned.setChannel(e.getChannel());
        try {
            cloned.init();
            cloned.go();
        } catch (AppiaEventException ex) {
            System.err.println("Error Sending event");
        }
    }

    public void boundSessions(Channel channel) {
    }
}
