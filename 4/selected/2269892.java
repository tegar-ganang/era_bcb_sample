package net.sf.peervibes.test.protocols.stateobserver;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.StringTokenizer;
import net.sf.peervibes.protocols.p2p.events.BroadcastSendableEvent;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.test.protocols.stateobserver.events.StateCaptureEvent;
import net.sf.peervibes.utils.Peer;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

/**
 * The <i>peer-to-peer communication</i> bottom session.
 * <br>

 * The primary goal of this session is to handle BroadcastSendableEvents to coordinate
 * node to take a picture of the internal state of the membership protocol (or other
 * protocol that are ready to receive the event provided by this layer).
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class StateObserverSession extends Session implements InitializableSession {

    private Peer myID;

    private boolean activeNode = false;

    private int broadcastInterval = 10;

    private Channel channel;

    private PrintStream file = null;

    private int count;

    private int epoch;

    public StateObserverSession(Layer layer) {
        super(layer);
        this.count = 0;
        this.epoch = 0;
    }

    public void init(SessionProperties args) {
        if (args.containsKey("active")) this.activeNode = args.getBoolean("active");
        if (args.containsKey("interval")) this.broadcastInterval = args.getInt("nterval");
    }

    public void handle(Event event) {
        if (event instanceof BroadcastSendableEvent) {
            if (event.getDir() == Direction.DOWN) handleDownBroadcastSendableEvent((BroadcastSendableEvent) event); else handleUpBroadcastSendableEvent((BroadcastSendableEvent) event);
        } else if (event instanceof P2PInitEvent) {
            this.myID = ((P2PInitEvent) event).getLocalPeer();
            try {
                StringTokenizer st = new StringTokenizer(this.myID.toString(), "::");
                st.nextToken();
                this.file = new PrintStream(st.nextToken().replace("/", "") + "_" + st.nextToken() + ".txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                this.file = System.out;
            }
            this.channel = event.getChannel();
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleDownBroadcastSendableEvent(BroadcastSendableEvent event) {
        if (this.activeNode && this.count == this.broadcastInterval) {
            event.getMessage().pushInt(++this.epoch);
            event.getMessage().pushBoolean(true);
            this.count = 0;
        } else {
            event.getMessage().pushBoolean(false);
            this.count++;
        }
    }

    private void handleUpBroadcastSendableEvent(BroadcastSendableEvent event) {
        if (event.getMessage().popBoolean()) {
            int epoch = event.getMessage().popInt();
            if (epoch > this.epoch) {
                this.epoch = epoch;
                try {
                    EchoEvent ee = new EchoEvent(new StateCaptureEvent(this.epoch, file, this.channel, Direction.DOWN, this), this.channel, Direction.UP, this);
                    ee.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
