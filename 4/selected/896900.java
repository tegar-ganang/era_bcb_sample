package net.sf.peervibes.test.appl;

import java.net.SocketAddress;
import java.util.Scanner;
import java.util.UUID;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.membership.events.InitMembershipEvent;
import net.sf.peervibes.protocols.p2p.events.ForwardEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.protocols.p2p.events.RouteSendableEvent;
import net.sf.peervibes.test.events.TestRouteSendableEvent;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> Routing test application session.
 * <br>
 * This session implements the basic operation of the routing test application
 * layer with manual control.
 * <br>
 * Observation: Still under test...
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class ManualRoutingTestApplSession extends Session implements InitializableSession {

    private Channel channel;

    private Peer localNode;

    private SocketAddress contactNode;

    private long sequenceNumber;

    private ScreenReader sr;

    private Thread slave;

    private boolean interactive;

    public ManualRoutingTestApplSession(Layer layer) {
        super(layer);
        this.channel = null;
        this.localNode = null;
        this.contactNode = null;
        this.sequenceNumber = 0;
        this.interactive = false;
    }

    public void init(SessionProperties parameters) {
        if (parameters.containsKey("contact")) {
            try {
                this.contactNode = ParseUtils.parseSocketAddress(parameters.getString("contact"), null, 0);
            } catch (Exception e) {
                this.contactNode = null;
            }
        }
        if (parameters.containsKey("interactive")) this.interactive = parameters.getBoolean("interactive");
    }

    public void handle(Event event) {
        if (event instanceof TestRouteSendableEvent) {
            handleTestRouteSensableEvent((TestRouteSendableEvent) event);
        } else if (event instanceof ForwardEvent) {
            handleForwardEvent((ForwardEvent) event);
        } else if (event instanceof P2PInitEvent) {
            handleP2PInitEvent((P2PInitEvent) event);
        } else if (event instanceof InitMembershipEvent) {
            handleInitMembershipEvent((InitMembershipEvent) event);
        } else if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose) event);
        } else {
            try {
                event.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleForwardEvent(ForwardEvent event) {
        RouteSendableEvent routed = null;
        try {
            routed = (RouteSendableEvent) event.getEvent().cloneEvent();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        if (routed != null && routed instanceof TestRouteSendableEvent) {
            long sequenceNumberReceived = routed.getMessage().popLong();
            System.out.println("Routing message (" + routed.getOriginalSender() + ":" + sequenceNumberReceived + ") for destiny " + routed.getDestiny() + " for Peer: " + routed.dest);
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleInitMembershipEvent(InitMembershipEvent event) {
        if (event.getDir() == Direction.UP) {
            if (event.getSuccess()) {
                System.err.println("Received a init OK from the Membership Layer.");
                if (this.interactive) {
                    System.err.println("Console on");
                    sr = new ScreenReader(this.channel);
                    slave = new Thread(this.sr);
                    slave.start();
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

    private void handleP2PInitEvent(P2PInitEvent event) {
        this.localNode = event.getLocalPeer();
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

    private void handleTestRouteSensableEvent(TestRouteSendableEvent event) {
        if (event.getDir() == Direction.UP) {
            long sequenceNumberReceived = event.getMessage().popLong();
            System.out.println("Received message (" + event.getOriginalSender() + ":" + sequenceNumberReceived + ") for destiny " + event.getDestiny());
        } else {
            if (event.dest == null) {
                event.setOriginalSender(this.localNode);
                event.getMessage().pushLong(this.sequenceNumber);
                System.out.println("Sending message (" + event.getOriginalSender() + ":" + this.sequenceNumber + ") for destiny " + event.getDestiny());
                this.sequenceNumber++;
            }
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private class ScreenReader implements Runnable {

        private Channel appiaCh;

        private Scanner sc = new Scanner(System.in);

        public ScreenReader(Channel channel) {
            this.appiaCh = channel;
        }

        public void run() {
            String line;
            while (true) {
                System.out.print("CMD: ");
                line = sc.nextLine();
                if (line.trim().equalsIgnoreCase("send")) ;
                UUID id = UUID.fromString(sc.nextLine().trim());
                line = sc.nextLine();
                try {
                    TestRouteSendableEvent trse = new TestRouteSendableEvent(channel, Direction.DOWN, null);
                    trse.setDestiny(id);
                    trse.getMessage().pushString(line.trim());
                    trse.asyncGo(this.appiaCh, Direction.DOWN);
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
