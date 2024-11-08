package net.sf.peervibes.test.appl;

import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketAddress;
import java.util.Random;
import java.util.Scanner;
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
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.test.events.BroadcastTestEvent;
import net.sf.peervibes.test.events.SendTimer;
import net.sf.peervibes.test.events.StartOperationEvent;
import net.sf.peervibes.test.protocols.stateobserver.events.StateCaptureEvent;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> broadcast test application session.
 * <br>
 * This session implements the basic operation of the broadcast test application
 * layer.
 * <br>
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class BroadcastTestApplSession extends Session implements InitializableSession {

    public static final long DEFAULT_SEND_PERIOD = 30000;

    public static final double DEFAULT_SEND_PROBABILITY = 0.25;

    private Channel channel;

    private Peer localNode;

    private SocketAddress contactNode;

    private long sendPeriod;

    private double sendProbability;

    private Random rand;

    private long sequenceNumber;

    private boolean interactive;

    private Thread cmdLine;

    private Class startEvent;

    private PrintStream sent;

    private PrintStream received;

    private static Logger log = Logger.getLogger(BroadcastTestApplSession.class);

    public BroadcastTestApplSession(Layer layer) {
        super(layer);
        this.channel = null;
        this.localNode = null;
        this.contactNode = null;
        this.sendPeriod = DEFAULT_SEND_PERIOD;
        this.sendProbability = DEFAULT_SEND_PROBABILITY;
        this.rand = new Random();
        this.sequenceNumber = 0;
        this.interactive = false;
        this.cmdLine = null;
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
        if (parameters.containsKey("interactive")) this.interactive = parameters.getBoolean("interactive");
        if (parameters.containsKey("event")) {
            try {
                this.startEvent = Class.forName(parameters.getString("event"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                this.startEvent = Event.class;
            }
        } else {
            this.startEvent = Event.class;
        }
    }

    public void handle(Event event) {
        if (event instanceof BroadcastTestEvent) {
            handleBroadcastTestEvent((BroadcastTestEvent) event);
        } else if (event instanceof StartOperationEvent) {
            handleStartOperationEvent((StartOperationEvent) event);
        } else if (event instanceof SendTimer) {
            handleSentTimer((SendTimer) event);
        } else if (event instanceof StateCaptureEvent) {
            handleStateCaptureEvent((StateCaptureEvent) event);
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

    private void handleStateCaptureEvent(StateCaptureEvent event) {
        try {
            log.fatal("Logging information for epoch " + event.getEpoch());
            this.initializeTempFiles(event.getEpoch() + 1);
            event.go();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleInitMembershipEvent(InitMembershipEvent event) {
        if (event.getDir() == Direction.UP) {
            if (event.getSuccess()) {
                try {
                    SendTimer st = new SendTimer("BTAS_SendTimer", this.sendPeriod, this.channel, Direction.DOWN, this, EventQualifier.ON);
                    st.go();
                    if (this.interactive) {
                        System.out.println("Interactive Mode.");
                        this.cmdLine = new Thread(new Console(this.channel));
                        this.cmdLine.start();
                    }
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
                BroadcastTestEvent bse = new BroadcastTestEvent(this.channel, Direction.DOWN, this, this.localNode);
                Message msg = bse.getMessage();
                msg.pushLong(this.sequenceNumber);
                bse.go();
                log.info("SEND " + this.localNode + " " + this.sequenceNumber + " " + bse.getMsgID());
                this.sent.println(this.localNode + " " + this.sequenceNumber + " " + bse.getMsgID());
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

    private void initializeTempFiles(int epoch) {
        try {
            if (this.sent != null) this.sent.close();
            if (this.received != null) this.sent.close();
            this.sent = new PrintStream(this.localNode.getAddress().toString().replace("/", "").replace(":", "_") + "_send_" + epoch + ".txt");
            this.received = new PrintStream(this.localNode.getAddress().toString().replace("/", "").replace(":", "_") + "_recv_" + epoch + ".txt");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleP2PInitEvent(P2PInitEvent event) {
        this.localNode = event.getLocalPeer();
        this.initializeTempFiles(1);
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

    private void handleBroadcastTestEvent(BroadcastTestEvent event) {
        if (event.getDir() == Direction.UP) {
            long sequenceNumberReceived = event.getMessage().popLong();
            log.info("RECV " + event.getOriginalSender() + " " + sequenceNumberReceived + " " + event.getMsgID().toString());
            this.received.println(event.getOriginalSender() + " " + sequenceNumberReceived + " " + event.getMsgID());
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleStartOperationEvent(StartOperationEvent event) {
        if (event.getDir() == Direction.UP) {
            Event ev = null;
            try {
                ev = (Event) this.startEvent.getConstructor(new Class[] {}).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                ev = new Event();
            }
            ev.setChannel(this.channel);
            ev.setDir(Direction.DOWN);
            ev.setSourceSession(this);
            try {
                ev.init();
                ev.go();
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

    private class Console implements Runnable {

        private Channel appiaCh;

        private Scanner sc = new Scanner(System.in);

        public Console(Channel channel) {
            this.appiaCh = channel;
        }

        public void run() {
            String line;
            while (true) {
                System.out.print("cmd: ");
                line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("help")) {
                    this.printHelp();
                } else if (line.equalsIgnoreCase("start")) {
                    this.sendStartOperation();
                } else System.out.println("Unknown command: '" + line + "'");
            }
        }

        private void sendStartOperation() {
            StartOperationEvent soe = new StartOperationEvent();
            try {
                soe.asyncGo(this.appiaCh, Direction.DOWN);
            } catch (AppiaEventException e) {
                System.err.println("Could not send event.");
                e.printStackTrace();
            }
        }

        private void printHelp() {
            System.out.println("Available commands:\nhelp: display this message.\nstart: sends a StartOperationEvent to all nodes in the system.");
        }
    }
}
