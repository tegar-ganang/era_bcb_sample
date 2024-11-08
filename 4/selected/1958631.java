package net.sf.peervibes.protocols.floodbroadcast;

import net.sf.peervibes.protocols.p2p.events.*;
import net.sf.peervibes.protocols.membership.events.GetPeersEvent;
import net.sf.appia.core.*;
import net.sf.peervibes.utils.Peer;
import net.sf.appia.core.events.channel.PeriodicTimer;
import java.util.*;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;

public class FloodBroadcastSession extends Session implements InitializableSession {

    /**
	 * map containing all the processed messages of the last (_ttl / 1000) seconds
	 */
    private List<UUID> _messageList;

    /**
	 * list containing the unprocessed events
	 */
    private List<BroadcastSendableEvent> _eventList;

    /**
	 * sample size (from xml)
	 */
    private int _sampleSize;

    /**
	 * time to maintaining nodes in _messageList (from xml)
	 */
    private int _ttl;

    /**
	 * timer which clears the _messageList
	 */
    private PeriodicTimer _timer;

    /**
	 * current channel
	 */
    private Channel _channel;

    /**
	 * Local instance of this Peer
	 */
    private Peer _localPeer;

    /**
	 * @param layer
	 */
    public FloodBroadcastSession(Layer layer) {
        super(layer);
        _messageList = new LinkedList<UUID>();
        _eventList = new LinkedList<BroadcastSendableEvent>();
        _localPeer = null;
        _sampleSize = 0;
        _ttl = 0;
    }

    /**
	 * Extracts important information from the xml.
	 * 
	 * @param params
	 */
    public void init(SessionProperties params) {
        if (params.containsKey("timeToLive")) _ttl = params.getInt("timeToLive");
        if (params.containsKey("sampleSize")) _sampleSize = params.getInt("sampleSize");
    }

    /**
	 * Main event handler.
	 * 
	 * @param event
	 */
    public void handle(Event event) {
        if (event instanceof BroadcastSendableEvent) handleBroadcast((BroadcastSendableEvent) event); else if (event instanceof PeriodicTimer) handleTimer((PeriodicTimer) event); else if (event instanceof GetPeersEvent) handleGetPeers((GetPeersEvent) event); else if (event instanceof ChannelInit) handleChannelInit((ChannelInit) event); else if (event instanceof ChannelClose) handleChannelClose((ChannelClose) event); else if (event instanceof P2PInitEvent) handleP2PInit((P2PInitEvent) event); else {
            try {
                event.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
	 * Inits the channel and saves its name.
	 * 
	 * @param event
	 */
    private void handleChannelInit(ChannelInit event) {
        try {
            _channel = event.getChannel();
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Closes the channel.
	 * 
	 * @param event
	 */
    private void handleChannelClose(ChannelClose event) {
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Upon P2P initialization, extracts local Peer information and starts the timer.
	 * 
	 * @param event
	 */
    private void handleP2PInit(P2PInitEvent event) {
        try {
            if (event.getDir() == Direction.DOWN) {
                if (_localPeer == null) {
                    _localPeer = event.getLocalPeer();
                    _timer = new PeriodicTimer("FloodBroadCastTimer", _ttl, _channel, Direction.DOWN, this, EventQualifier.ON);
                    _timer.go();
                }
            }
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        } catch (AppiaException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Each _ttl seconds, it cleans HALF of the _messageList 
	 * 
	 * @param event
	 */
    public void handleTimer(PeriodicTimer event) {
        try {
            int size = _messageList.size();
            size /= 2;
            for (int i = 0; i < size; i++) _messageList.remove(i);
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Extracts the messageID, if the ID is present in the _messageList,
	 * then the message has already been sent, return. 
	 * 
	 * If the ID doesn't exist, then a new GetPeersEvent is sent down, 
	 * and the event which contains the message is added to the _eventList 
	 * to be processed once the GetPeersEvent arrives (see handleGetPeers below).
	 * 
	 * @param event
	 */
    public void handleBroadcast(BroadcastSendableEvent event) {
        try {
            UUID id = event.getMsgID();
            if (_messageList.contains(id)) return;
            _eventList.add(event);
            GetPeersEvent peers = new GetPeersEvent(_channel, Direction.DOWN, this, _sampleSize, event.getSender());
            peers.init();
            peers.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Once a GetPeers event is received, the first message in the _eventList
	 * can be processed and sent to the neighbours.
	 * 
	 * @param event
	 */
    public void handleGetPeers(GetPeersEvent event) {
        try {
            Iterator<Peer> itPeer = event.getIterator();
            if (_eventList.isEmpty()) return;
            BroadcastSendableEvent eventToSend = _eventList.remove(0);
            BroadcastSendableEvent eventForThisPeer;
            while (itPeer.hasNext()) {
                eventForThisPeer = (BroadcastSendableEvent) eventToSend.clone();
                eventForThisPeer.source = _localPeer;
                eventForThisPeer.dest = itPeer.next();
                eventForThisPeer.init();
                eventForThisPeer.go();
            }
            _messageList.add(eventToSend.getMsgID());
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
    }
}
