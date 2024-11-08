package net.sf.appia.jgcs.protocols.remote;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.NetworkUndeliveredEvent;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.remote.RemoteViewEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import org.apache.log4j.Logger;

/**
 * This class defines a RemoteAddressSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class RemoteAddressSession extends Session implements InitializableSession {

    private static Logger logger = Logger.getLogger(RemoteAddressSession.class);

    private static final long DEFAULT_TIMER_PERIOD = 1000;

    private SocketAddress[] addresses = null;

    private int nextAddrRank = 0;

    private List<SendableEvent> pendingEvents = null;

    private String groupID = null;

    private long timerPeriod = DEFAULT_TIMER_PERIOD;

    /**
	 * Creates a new RemoteAddressSession.
	 * @param layer
	 */
    public RemoteAddressSession(Layer layer) {
        super(layer);
        pendingEvents = new ArrayList<SendableEvent>();
    }

    public void init(SessionProperties params) {
        if (params.containsKey("group")) groupID = params.getString("group");
        if (params.containsKey("timer_period")) timerPeriod = params.getLong("timer_period");
    }

    @Override
    public void handle(Event event) {
        try {
            if (event instanceof RemoteViewEvent) handleRemoteViewEvent((RemoteViewEvent) event); else if (event instanceof SendableEvent) handleSendableEvent((SendableEvent) event); else if (event instanceof RetrieveAddressTimer) handleTimer((RetrieveAddressTimer) event); else if (event instanceof ChannelInit) handleChannelInit((ChannelInit) event); else if (event instanceof ChannelClose) handleChannelClose((ChannelClose) event); else if (event instanceof NetworkUndeliveredEvent) handleUndelivered((NetworkUndeliveredEvent) event); else event.go();
        } catch (AppiaException e) {
            logger.debug("Error sending event: " + e);
        }
    }

    private void handleTimer(RetrieveAddressTimer timer) throws AppiaEventException {
        new RemoteViewEvent(timer.getChannel(), Direction.DOWN, this, new Group(groupID)).go();
        timer.go();
    }

    private void handleUndelivered(NetworkUndeliveredEvent event) throws AppiaEventException {
        event.getFailedAddress();
        event.go();
    }

    private void handleChannelClose(ChannelClose close) throws AppiaException {
        new RetrieveAddressTimer(timerPeriod, close.getChannel(), Direction.DOWN, this, EventQualifier.OFF).go();
        close.go();
    }

    private void handleChannelInit(ChannelInit init) throws AppiaException {
        new RetrieveAddressTimer(timerPeriod, init.getChannel(), Direction.DOWN, this, EventQualifier.ON).go();
        init.go();
    }

    private void handleRemoteViewEvent(RemoteViewEvent event) throws AppiaEventException {
        if (logger.isDebugEnabled()) logger.debug("Received remote view event. Addresses are: " + event.getAddresses());
        addresses = event.getAddresses();
        nextAddrRank = 0;
        event.go();
        trySendMessages();
    }

    private void handleSendableEvent(SendableEvent event) throws AppiaEventException {
        if (event.getDir() == Direction.DOWN) {
            if (!hasAddressList()) {
                pendingEvents.add(event);
                new RemoteViewEvent(event.getChannel(), Direction.DOWN, this, new Group(groupID)).go();
            } else if (event.dest == null) {
                event.dest = getAddressRoundRobin();
            }
            if (pendingEvents.isEmpty()) {
                if (event.dest != null) {
                    if (logger.isDebugEnabled()) logger.debug("Sending to address: " + event.dest);
                    event.go();
                } else {
                    pendingEvents.add(event);
                }
            } else pendingEvents.add(event);
            trySendMessages();
        } else {
            event.go();
        }
    }

    private void trySendMessages() throws AppiaEventException {
        if (pendingEvents.isEmpty() || addresses == null) return;
        final Iterator<SendableEvent> it = pendingEvents.iterator();
        SendableEvent ev = null;
        while (it.hasNext()) {
            ev = it.next();
            if (ev.dest == null) ev.dest = getAddressRoundRobin();
            if (logger.isDebugEnabled()) logger.debug("Sending to address: " + ev.dest);
            ev.go();
            it.remove();
        }
    }

    private boolean hasAddressList() {
        return addresses != null && addresses.length > 0;
    }

    private SocketAddress getAddressRoundRobin() {
        int next = nextAddrRank++;
        if (next >= addresses.length) next = 0;
        return addresses[next];
    }
}
