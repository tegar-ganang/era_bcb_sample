package net.sf.peervibes.utils.p2pdebug;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.events.GetPeersEvent;
import net.sf.peervibes.protocols.membership.events.InitMembershipEvent;
import net.sf.peervibes.protocols.p2p.events.BroadcastSendableEvent;
import net.sf.peervibes.protocols.p2p.events.ForwardEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.protocols.p2p.events.RouteSendableEvent;
import net.sf.peervibes.protocols.tman.events.UpdateViewEvent;
import net.sf.peervibes.protocols.tman.events.ViewReturnEvent;
import net.sf.peervibes.test.events.PingEvent;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> P2PDebug session.
 * <br>
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class P2PDebugSession extends Session implements InitializableSession {

    private boolean logP2PInitEvent;

    private boolean logBroadcastSendableEvent;

    private boolean logRouteSendableEvent;

    private boolean logForwardEvent;

    private boolean logInitMembershipEvent;

    private boolean logGetPeersEvent;

    private boolean logUpdateViewEvent;

    private boolean logViewReturnEvent;

    private boolean logSendableEvent;

    private boolean logPingEvent;

    public static final String DEFAULT_LOG_FILE = "P2PDebugSession_log.txt";

    private String logFile;

    private PrintStream out;

    public P2PDebugSession(Layer layer) {
        super(layer);
        this.logP2PInitEvent = false;
        this.logBroadcastSendableEvent = false;
        this.logRouteSendableEvent = false;
        this.logForwardEvent = false;
        this.logInitMembershipEvent = false;
        this.logGetPeersEvent = false;
        this.logUpdateViewEvent = false;
        this.logViewReturnEvent = false;
        this.logSendableEvent = false;
        this.logPingEvent = false;
        this.logFile = P2PDebugSession.DEFAULT_LOG_FILE;
    }

    public void init(SessionProperties params) {
        if (params.containsKey("logP2PInitEvent")) this.logP2PInitEvent = params.getBoolean("logP2PInitEvent");
        if (params.containsKey("logBroadcastSendableEvent")) this.logBroadcastSendableEvent = params.getBoolean("logBroadcastSendableEvent");
        if (params.containsKey("logRouteSendableEvent")) this.logRouteSendableEvent = params.getBoolean("logRouteSendableEvent");
        if (params.containsKey("logForwardEvent")) this.logForwardEvent = params.getBoolean("logForwardEvent");
        if (params.containsKey("logInitMembershipEvent")) this.logInitMembershipEvent = params.getBoolean("logInitMembershipEvent");
        if (params.containsKey("logGetPeersEvent")) this.logGetPeersEvent = params.getBoolean("logGetPeersEvent");
        if (params.containsKey("logUpdateViewEvent")) this.logUpdateViewEvent = params.getBoolean("logUpdateViewEvent");
        if (params.containsKey("logViewReturnEvent")) this.logViewReturnEvent = params.getBoolean("logViewReturnEvent");
        if (params.containsKey("logSendableEvent")) this.logSendableEvent = params.getBoolean("logSendableEvent");
        if (params.containsKey("logPingEvent")) this.logPingEvent = params.getBoolean("logPingEvent");
        if (params.containsKey("file")) this.logFile = params.getString("file");
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose) event);
        } else if (event instanceof P2PInitEvent && this.logP2PInitEvent) {
            handleP2PInitEvent((P2PInitEvent) event);
        } else if (event instanceof BroadcastSendableEvent && this.logBroadcastSendableEvent) {
            handleBroadcastSendableEvent((BroadcastSendableEvent) event);
        } else if (event instanceof RouteSendableEvent && this.logRouteSendableEvent) {
            handleRouteSendableEvent((RouteSendableEvent) event);
        } else if (event instanceof ForwardEvent && this.logForwardEvent) {
            handleForwardEvent((ForwardEvent) event);
        } else if (event instanceof InitMembershipEvent && this.logInitMembershipEvent) {
            handleInitMembershipEvent((InitMembershipEvent) event);
        } else if (event instanceof GetPeersEvent && this.logGetPeersEvent) {
            handleGetPeersEvent((GetPeersEvent) event);
        } else if (event instanceof PingEvent && this.logPingEvent) {
            handlePingEvent((PingEvent) event);
        } else if (event instanceof UpdateViewEvent && this.logUpdateViewEvent) {
            handleUpdateViewEvent((UpdateViewEvent) event);
        } else if (event instanceof ViewReturnEvent && this.logViewReturnEvent) {
            handleViewReturnEvent((ViewReturnEvent) event);
        } else if (event instanceof SendableEvent && this.logSendableEvent) {
            handleGenericSendableEvent((SendableEvent) event);
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handlePingEvent(PingEvent event) {
        this.dumpToLog("PingEvent " + this.getEventBasics(event));
    }

    private void handleGenericSendableEvent(SendableEvent event) {
        this.dumpToLog(event.getClass().getName() + " " + this.getEventBasics(event) + " " + getSendableEventBasics(event) + " Message size: " + event.getMessage().length() + "bytes");
    }

    private void handleUpdateViewEvent(UpdateViewEvent event) {
        this.dumpToLog("UpdateViewEvent " + this.getEventBasics(event) + " " + getSendableEventBasics(event) + " Message size: " + event.getMessage().length() + "bytes");
    }

    private void handleViewReturnEvent(ViewReturnEvent event) {
        this.dumpToLog("ViewReturnEvent " + this.getEventBasics(event) + " " + getSendableEventBasics(event) + " Message size: " + event.getMessage().length() + "bytes");
    }

    private void handleGetPeersEvent(GetPeersEvent event) {
        String list = "";
        if (event.getAnswerCollection() == null) list = "none"; else for (Peer p : event.getAnswerCollection()) list = list + p.toString() + " ";
        this.dumpToLog("GetPeersEvent " + this.getEventBasics(event) + " N: " + event.getN() + " exception: " + event.getException() + " answer: " + list);
    }

    private void handleInitMembershipEvent(InitMembershipEvent event) {
        this.dumpToLog("InitMembershipEvent " + this.getEventBasics(event) + " contactNode: " + event.getContactNode() + " sucess: " + event.getSuccess());
    }

    private void handleForwardEvent(ForwardEvent event) {
        RouteSendableEvent rse = (RouteSendableEvent) event.getEvent();
        this.dumpToLog("ForwardEvent " + this.getEventBasics(event) + " payload: [" + "RouteSendableEvent " + this.getEventBasics(event.getEvent()) + " " + this.getSendableEventBasics(rse) + " Original Sender: " + rse.getOriginalSender() + " Sender: " + rse.getSender() + " Goal: " + rse.getDestiny() + "]");
    }

    private void handleRouteSendableEvent(RouteSendableEvent event) {
        this.dumpToLog("RouteSendableEvent " + this.getEventBasics(event) + " " + this.getSendableEventBasics(event) + " Original Sender: " + event.getOriginalSender() + " Sender: " + event.getSender() + " Goal: " + event.getDestiny());
    }

    @SuppressWarnings("unchecked")
    private void handleBroadcastSendableEvent(BroadcastSendableEvent event) {
        String list = "";
        if (event.dest instanceof Collection) {
            for (Object o : (Collection) event.dest) list = list + o + " ";
            list = "Expanded destinations: " + list;
        }
        this.dumpToLog("BroadcastSendableEvent " + this.getEventBasics(event) + " " + this.getSendableEventBasics(event) + " Original Sender: " + event.getOriginalSender() + " Sender: " + event.getSender() + " " + list);
    }

    private void handleP2PInitEvent(P2PInitEvent event) {
        this.dumpToLog("P2PInitEvent " + this.getEventBasics(event) + " localPeer: " + event.getLocalPeer());
    }

    private void handleChannelClose(ChannelClose event) {
        this.out.close();
    }

    private void handleChannelInit(ChannelInit event) {
        try {
            this.out = new PrintStream(this.logFile, "a");
        } catch (UnsupportedEncodingException e) {
            this.out = System.out;
        } catch (FileNotFoundException e) {
            this.out = System.out;
        }
    }

    private String getEventBasics(Event e) {
        return "<(Channel:" + e.getChannel().getChannelID() + "),(Direction:" + (e.getDir() == Direction.DOWN ? "DOWN" : "UP") + "), (Source:" + (e.getSourceSession() != null ? e.getSourceSession().getClass().getName() : "null") + ")>";
    }

    private String getSendableEventBasics(SendableEvent e) {
        return "<(src:" + e.source + "),(dest:" + e.dest + ")>";
    }

    private void dumpToLog(String desc) {
        this.out.println(new Date().toString() + " " + desc);
    }
}
