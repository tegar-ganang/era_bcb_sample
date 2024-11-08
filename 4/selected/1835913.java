package net.sf.peervibes.protocols.p2p.init;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.peervibes.protocols.p2p.events.P2PInitEvent;
import net.sf.peervibes.utils.InitializablePeer;
import net.sf.peervibes.utils.Peer;

/**
 * The <i>peer-to-peer communication</i> P2PInit session.
 * <br>

 * The primary goal of this session is to handle the ChannelInit Event, this is
 * done by starting the process to register a local socket which will be used
 * by the p2p communication framework. Notice that the local ip/port information
 * should be provided to this layer in its configuration.
 * <br>
 * Upon the reception of the reply for the RegisterSocketEvent this layer generates
 * the appropriate Peer instance using this information and a random generated UUID
 * (optionally, a specific UUID can be provided to the configuration of this layer)
 * and puts this instance in a P2PInitEvent which it generates and sends transversing
 * the Appia stack going DOWN.
 *
 * @version 0.1
 * @author Joao Leitao
 */
public class P2PInitSession extends Session implements InitializableSession {

    public static int DEFAULT_LOCAL_PORT = 5000;

    public static String DEFAULT_PEER_TYPE = "net.sf.peervibes.utils.Peer";

    private Channel channel;

    private Peer localPeer;

    private InetSocketAddress localHost;

    private UUID localID;

    private boolean fromTop;

    private String peerType;

    private SessionProperties params;

    private boolean initialNode = false;

    public static boolean debug = true;

    public P2PInitSession(Layer layer) {
        super(layer);
        this.channel = null;
        this.localPeer = null;
        this.localHost = null;
        this.localID = null;
        this.fromTop = false;
        this.peerType = P2PInitSession.DEFAULT_PEER_TYPE;
        this.params = null;
    }

    public void init(SessionProperties params) {
        this.params = params;
        try {
            if (params.containsKey("localhost")) this.localHost = ParseUtils.parseSocketAddress(params.getProperty("localhost"), null, DEFAULT_LOCAL_PORT);
            if (params.containsKey("localid")) this.localID = UUID.fromString(params.getString("localid"));
            if (params.containsKey("fromtop")) this.fromTop = params.getBoolean("fromtop");
            if (params.containsKey("peertype")) this.peerType = params.getString("peertype");
            if (params.containsKey("debug")) P2PInitSession.debug = params.getBoolean("debug");
            if (params.containsKey("initial")) initialNode = params.getBoolean("initial");
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit) event);
            return;
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose) event);
            return;
        } else if (event instanceof RegisterSocketEvent) {
            handleRegisterSocketEvent((RegisterSocketEvent) event);
            return;
        }
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleRegisterSocketEvent(RegisterSocketEvent event) {
        try {
            event.go();
            if (event.getDir() == Direction.UP) {
                if (debug) {
                    System.err.println("RSE received: " + event.getLocalSocketAddress() + " status accepted is: " + event.isAccepted());
                }
                if (this.localID == null) this.localPeer = (Peer) Class.forName(this.peerType).getConstructor(new Class[] { SocketAddress.class }).newInstance(new Object[] { event.getLocalSocketAddress() }); else this.localPeer = (Peer) Class.forName(this.peerType).getConstructor(new Class[] { SocketAddress.class, UUID.class }).newInstance(new Object[] { event.getLocalSocketAddress(), this.localID });
                if (this.params != null && this.localPeer instanceof InitializablePeer) {
                    ((InitializablePeer) this.localPeer).init(this.params);
                }
                P2PInitEvent pinit = new P2PInitEvent(this.channel, Direction.DOWN, this, this.localPeer);
                if (this.fromTop) {
                    EchoEvent echo = new EchoEvent(pinit, this.channel, Direction.UP, this);
                    echo.init();
                    echo.go();
                } else {
                    pinit.init();
                    pinit.go();
                }
            }
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        } catch (AppiaException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SecurityException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleChannelClose(ChannelClose event) {
        try {
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleChannelInit(ChannelInit event) {
        this.channel = event.getChannel();
        if (!initialNode && localHost != null) localHost = new InetSocketAddress(localHost.getAddress(), RegisterSocketEvent.FIRST_AVAILABLE);
        try {
            event.go();
            RegisterSocketEvent rse = new RegisterSocketEvent(this.channel, Direction.DOWN, this);
            rse.localHost = (localHost != null) ? localHost.getAddress() : null;
            rse.port = (localHost != null) ? localHost.getPort() : RegisterSocketEvent.FIRST_AVAILABLE;
            rse.init();
            rse.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
}
