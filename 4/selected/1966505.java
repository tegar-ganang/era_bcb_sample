package net.sf.peervibes.protocols.tman;

import java.util.Collection;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.events.GetPeersEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.protocols.tman.events.SwitchRankFunctionEvent;
import net.sf.peervibes.protocols.tman.events.UpdateViewEvent;
import net.sf.peervibes.protocols.tman.events.ViewReturnEvent;
import net.sf.peervibes.protocols.tman.timers.UpdateViewTimer;
import net.sf.peervibes.utils.CartesianPeer;

public class TManSession extends Session implements InitializableSession {

    private int _c;

    private RankFunction _rankFunction;

    private CartesianPeer _myDescriptor;

    private View _myView;

    private Channel _thisChan;

    private String _timerID;

    private long _viewUpdateTime;

    private double _lastUpdateID;

    private Boolean _waitingForGetPeers;

    public TManSession(Layer layer) {
        super(layer);
        _c = 4;
        _rankFunction = RankFunction.getFunction("PROXIMITY");
        _myDescriptor = null;
        _myView = new View();
        _timerID = "tmanPeriodicTimer";
        _viewUpdateTime = 3000;
        _lastUpdateID = -1.0;
        _waitingForGetPeers = false;
    }

    public void init(SessionProperties params) {
        if (params.containsKey("viewsize")) _c = params.getInt("viewsize");
        if (params.containsKey("rankfunction")) _rankFunction = RankFunction.getFunction(params.getString("rankfunction"));
        if (params.containsKey("updatetime")) _viewUpdateTime = params.getLong("updatetime");
    }

    public void handle(Event ev) {
        if (ev instanceof ChannelInit) handleChannelInit((ChannelInit) ev); else if (ev instanceof ChannelClose) handleChannelClose((ChannelClose) ev); else if (ev instanceof UpdateViewEvent) handleUpdateEvent((UpdateViewEvent) ev); else if (ev instanceof ViewReturnEvent) handleViewReturn((ViewReturnEvent) ev); else if (ev instanceof GetPeersEvent) handleGetPeers((GetPeersEvent) ev); else if (ev instanceof UpdateViewTimer) handleUpdateTimer((UpdateViewTimer) ev); else if (ev instanceof P2PInitEvent) handleP2PInit((P2PInitEvent) ev); else if (ev instanceof SwitchRankFunctionEvent) handleSwitchFunction((SwitchRankFunctionEvent) ev); else try {
            System.out.println("Received unexpected event!");
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleChannelInit(ChannelInit e) {
        try {
            e.go();
            _thisChan = e.getChannel();
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void handleChannelClose(ChannelClose e) {
        try {
            e.go();
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void handleUpdateEvent(UpdateViewEvent e) {
        try {
            e.go();
            if (e.getDir() == Direction.UP) {
                _myView.merge(e.getView());
                View viewToSend = new View(_myView);
                _myView = _rankFunction.getBestView(_myView, _myDescriptor, _c);
                ViewReturnEvent vre = new ViewReturnEvent(_thisChan, Direction.DOWN, this, viewToSend, e.getID());
                vre.dest = e.source;
                vre.source = e.dest;
                vre.go();
            }
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void handleViewReturn(ViewReturnEvent e) {
        try {
            e.go();
            if (e.getID() == _lastUpdateID) _myView = _rankFunction.getBestView(e.getView(), _myDescriptor, _c);
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleGetPeers(GetPeersEvent e) {
        try {
            e.go();
            if (e.getDir() == Direction.UP && _waitingForGetPeers) {
                _waitingForGetPeers = false;
                _myView.merge(new View((Collection<CartesianPeer>) e.getAnswer()));
                if (!_myView.isEmpty()) {
                    _lastUpdateID = Math.random();
                    View viewToSend = new View(_myView);
                    viewToSend.add(_myDescriptor);
                    UpdateViewEvent uve = new UpdateViewEvent(_thisChan, Direction.DOWN, this, viewToSend, _lastUpdateID);
                    uve.dest = _rankFunction.getBestPeer(_myView, _myDescriptor).getAddress();
                    uve.source = _myDescriptor.getAddress();
                    uve.go();
                }
                _myView = _rankFunction.getBestView(_myView, _myDescriptor, _c);
            }
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void handleSwitchFunction(SwitchRankFunctionEvent e) {
        try {
            e.go();
            if (e.getDir() == Direction.UP) {
                if (e.getID() == -1) _rankFunction = RankFunction.getFunction(e.getName()); else _rankFunction = RankFunction.getFunction(e.getID());
            }
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void handleP2PInit(P2PInitEvent e) {
        try {
            e.go();
            if (e.getDir() == Direction.DOWN) {
                if (_myDescriptor == null) {
                    _myDescriptor = (CartesianPeer) e.getLocalPeer();
                    UpdateViewTimer uvt = new UpdateViewTimer(_timerID, _viewUpdateTime, _thisChan, Direction.DOWN, this, EventQualifier.ON);
                    uvt.go();
                }
            }
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        } catch (AppiaException e2) {
            e2.printStackTrace();
        }
    }

    private void handleUpdateTimer(UpdateViewTimer e) {
        try {
            e.go();
            sendNewGetPeersEvent();
        } catch (AppiaEventException e1) {
            e1.printStackTrace();
        }
    }

    private void sendNewGetPeersEvent() throws AppiaEventException {
        _waitingForGetPeers = true;
        GetPeersEvent gpe = new GetPeersEvent(2 * _c, _myDescriptor);
        gpe.setDir(Direction.DOWN);
        gpe.setChannel(_thisChan);
        gpe.setSourceSession(this);
        gpe.init();
        gpe.go();
    }
}
