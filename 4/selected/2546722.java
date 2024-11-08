package net.sf.peervibes.test.appl;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Random;
import java.util.UUID;
import org.apache.log4j.Logger;
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
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.events.InitMembershipEvent;
import net.sf.peervibes.protocols.p2p.events.ForwardEvent;
import net.sf.peervibes.protocols.p2p.events.RouteSendableEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.test.events.SendTimer;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> Routing test application session.
 * <br>
 * This session implements the basic operation of the routing test application
 * layer.
 * <br>
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class RoutingTestApplSession extends Session implements InitializableSession {

    public static final long DEFAULT_SEND_PERIOD = 30000;

    public static final double DEFAULT_SEND_PROBABILITY = 0.25;

    private Channel channel;

    private Peer localNode;

    private SocketAddress contactNode;

    private long sendPeriod;

    private double sendProbability;

    private Random rand;

    private long sequenceNumber;

    private PrintStream sent;

    private PrintStream received;

    private PrintStream forwarded;

    private static Logger log = Logger.getLogger(BroadcastTestApplSession.class);

    public RoutingTestApplSession(Layer layer) {
        super(layer);
        this.channel = null;
        this.localNode = null;
        this.contactNode = null;
        this.sendPeriod = DEFAULT_SEND_PERIOD;
        this.sendProbability = DEFAULT_SEND_PROBABILITY;
        this.rand = new Random();
        this.sequenceNumber = 0;
    }

    public void init(SessionProperties parameters) {
        if (parameters.containsKey("contact")) {
            try {
                this.contactNode = ParseUtils.parseSocketAddress(parameters.getString("contact"), null, 0);
            } catch (Exception e) {
                this.contactNode = null;
            }
        }
        if (parameters.containsKey("period")) this.sendPeriod = parameters.getLong("period");
        if (parameters.containsKey("probability")) this.sendProbability = parameters.getDouble("probability");
    }

    public void handle(Event event) {
        if (event instanceof RouteSendableEvent) {
            handleRouteSensableEvent((RouteSendableEvent) event);
        } else if (event instanceof ForwardEvent) {
            handleForwardEvent((ForwardEvent) event);
        } else if (event instanceof SendTimer) {
            handleSentTimer((SendTimer) event);
        } else if (event instanceof P2PInitEvent) {
            handleP2PInitEvent((P2PInitEvent) event);
        } else if (event instanceof InitMembershipEvent) {
            handleInitMembershipEvent((InitMembershipEvent) event);
        } else if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose) event);
        } else {
            log.warn("Unrequested event: \"" + event.getClass().getName() + "\".");
            try {
                event.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleForwardEvent(ForwardEvent event) {
        RouteSendableEvent routed = (RouteSendableEvent) event.getEvent();
        long sequenceNumberReceived = routed.getMessage().popLong();
        log.info("FORWARD " + routed.getOriginalSender() + " " + sequenceNumberReceived + " " + routed.getMsgID().toString() + " to " + routed.getDestiny() + " from " + routed.source + " through " + routed.dest);
        this.forwarded.println(routed.getOriginalSender() + " " + sequenceNumberReceived + " " + routed.getMsgID().toString() + " to " + routed.getDestiny() + " from " + routed.source + " through " + routed.dest);
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleInitMembershipEvent(InitMembershipEvent event) {
        if (event.getDir() == Direction.UP) {
            if (event.getSuccess()) {
                try {
                    SendTimer st = new SendTimer("RTAS_SendTimer", this.sendPeriod, this.channel, Direction.DOWN, this, EventQualifier.ON);
                    st.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                } catch (AppiaException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("Received a init timeout from the Membership Layer.");
            }
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleSentTimer(SendTimer event) {
        if (event.getDir() == Direction.UP && rand.nextDouble() < this.sendProbability) {
            try {
                RouteSendableEvent rse = new RouteSendableEvent(this.channel, Direction.DOWN, this, this.localNode, UUID.randomUUID());
                Message msg = rse.getMessage();
                msg.pushLong(this.sequenceNumber);
                rse.go();
                log.info("SEND " + this.localNode + " " + this.sequenceNumber + " " + rse.getMsgID() + " to " + rse.getDestiny());
                this.sent.println(this.localNode + " " + this.sequenceNumber + " " + rse.getMsgID() + " to " + rse.getDestiny());
                this.sequenceNumber++;
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleChannelClose(ChannelClose event) {
        this.channel = null;
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleChannelInit(ChannelInit event) {
        this.channel = event.getChannel();
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void initializeFiles() {
        try {
            if (this.sent != null) this.sent.close();
            if (this.received != null) this.sent.close();
            this.sent = new PrintStream(this.localNode.getAddress().toString().replace("/", "").replace(":", "_") + "_send.txt");
            this.received = new PrintStream(this.localNode.getAddress().toString().replace("/", "").replace(":", "_") + "_recv.txt");
            this.forwarded = new PrintStream(this.localNode.getAddress().toString().replace("/", "").replace(":", "_") + "_forward.txt");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleP2PInitEvent(P2PInitEvent event) {
        this.localNode = event.getLocalPeer();
        this.initializeFiles();
        log.debug("INIT " + this.localNode);
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
        if (this.contactNode != null) {
            try {
                InitMembershipEvent ime = new InitMembershipEvent(this.channel, Direction.DOWN, this, new Peer(this.contactNode));
                ime.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRouteSensableEvent(RouteSendableEvent event) {
        if (event.getDir() == Direction.UP) {
            long sequenceNumberReceived = event.getMessage().popLong();
            log.info("RECV " + event.getOriginalSender() + " " + sequenceNumberReceived + " " + event.getMsgID().toString() + " to " + event.getDestiny());
            this.received.println(event.getOriginalSender() + " " + sequenceNumberReceived + " " + event.getMsgID() + " to " + event.getDestiny());
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
}
