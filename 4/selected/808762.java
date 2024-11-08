package tuwien.auto.eicl;

import java.net.*;
import java.util.Vector;
import java.util.List;
import tuwien.auto.eicl.event.DisconnectEvent;
import tuwien.auto.eicl.event.EICLEventListener;
import tuwien.auto.eicl.event.FrameEvent;
import tuwien.auto.eicl.struct.cemi.CEMI_L_DATA;
import tuwien.auto.eicl.struct.cemi.CEMI;
import tuwien.auto.eicl.struct.eibnetip.Connect_Request;
import tuwien.auto.eicl.struct.eibnetip.Connect_Response;
import tuwien.auto.eicl.struct.eibnetip.Connectionstate_Response;
import tuwien.auto.eicl.struct.eibnetip.Connectionstate_Request;
import tuwien.auto.eicl.struct.eibnetip.Disconnect_Request;
import tuwien.auto.eicl.struct.eibnetip.Disconnect_Response;
import tuwien.auto.eicl.struct.eibnetip.EIBnetIPPacket;
import tuwien.auto.eicl.struct.eibnetip.CEMI_Connection_Ack;
import tuwien.auto.eicl.struct.eibnetip.CEMI_Connection_Request;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;
import tuwien.auto.eicl.struct.eibnetip.util.HPAI;
import tuwien.auto.eicl.util.*;

/**
 * 
 * <p>
 * This class represents an EIBnet/IP connection which is used to exchange cEMI
 * messages. This class is able to handle Tunnelling as well as Management
 * connections. The CEMIConnectionTypeInfoContainer classes passed to the
 * constructor are needed for selecting the desired connection type. Like
 * Discoverer, it runs as an independent thread. Again, this is hidden from the
 * user. Connection Heartbeating is also managed autonomously.
 * <p>
 * Two different send modes are provided.The first, "immediate send", sends out
 * the requested message and returns immediately after sending. It is up to the
 * programmer to check upon the progress of the transmission using the
 * getState() method. The second send mode, "wait for confirm", waits until both
 * the ACK and the cEMI CON message have been received successfully (with
 * acknowledge bit set). If this was not achieved within an interval of 5
 * seconds an exception is thrown.
 * <p>
 * The programmer has the possibility to register event handlers that are called
 * for each cEMI packet that reaches the socket and when the server closes the
 * connection.
 * <p>
 * Finally the disconnect method closes the connection, and stops the heartbeat
 * messaging. All registered event handlers are invoked with the disconnect
 * event.
 * <p>
 * 
 * @see tuwien.auto.eicl.Discoverer
 * @see tuwien.auto.eicl.Heartbeat
 * @see tuwien.auto.eicl.event.EICLEventListener
 * @see tuwien.auto.eicl.event.FrameEvent
 * @see tuwien.auto.eicl.event.DisconnectEvent
 * @see tuwien.auto.eicl.TunnellingConnectionType
 * @see tuwien.auto.eicl.CEMIConnectionTypeInfoContainer
 * @author Bernhard Erb
 */
public class CEMI_Connection implements Runnable {

    private static final int CONNECT_TIMEOUT = 5;

    private static final int SEND_TIMEOUT = 3;

    private static final int DISCONNECT_TIMEOUT = 5;

    /**
     * In this send mode the sendFrame() method blocks until the Ack has been
     * received.
     */
    public static final boolean WAIT_FOR_CONFIRM = true;

    /**
     * In this send mode the CEMI_Connection#sendFrame() method immediate
     * returns after processing the frame. Use the CEMI_Connection#getStatus()
     * method for following the packet state.
     */
    public static final boolean IMMEDIATE_SEND = false;

    /**
     * Satus byte: Message was delivered successfully.
     */
    public static final byte OK = 0;

    /**
     * Status byte: No Ack was received so far.
     */
    public static final byte AWAITING_ACK = 1;

    /**
     * Status byte: Ack was received, but cEMI confirmation is missing
     */
    public static final byte AWAITING_CEMI_CON = 2;

    /**
     * Status byte: The connection is in wron mode. No information can be given
     * about the packet state
     */
    public static final byte WRONG_MODE = (byte) 0xFF;

    private Thread receiveThread;

    private DatagramSocket socket;

    private Connected_Server server = new Connected_Server();

    private boolean sendMode = WAIT_FOR_CONFIRM;

    private byte sendState = OK;

    private String error = "";

    private Vector events = new Vector();

    private Heartbeat heartbeat;

    private Thread heartbeatThread;

    private boolean stopThread = false;

    private CEMIConnectionTypeInfoContainer connection;

    private final QueueDispatcher dispatcher = new QueueDispatcher();

    /**
     * Sends out a connect request message and waits for response. If the
     * connection has been established successfully the constructor starts the
     * receiving threads and heart beating. Otherwise a exception is thrown.
     * 
     * @param _DatagramSocket
     *            An open socket
     * @param _Server
     *            The server to which connect
     * @param _Connection
     *            The Object which includes all relevant connection options
     * @throws EICLException
     *             Forwards the SocketException
     */
    public CEMI_Connection(DatagramSocket _DatagramSocket, InetSocketAddress _Server, CEMIConnectionTypeInfoContainer _Connection) throws EICLException {
        socket = _DatagramSocket;
        receiveThread = new Thread(this);
        receiveThread.start();
        connection = _Connection;
        long time = 0;
        if (_Server.isUnresolved()) {
            throw new EICLException("Unresolved server address");
        }
        try {
            Connect_Request request = new Connect_Request(connection.getConnectionTypeCode(), connection.getConnectionOptions(), socket.getLocalPort());
            DatagramPacket packet = new DatagramPacket(request.toByteArray(), request.toByteArray().length, _Server);
            socket.send(packet);
        } catch (Exception ex) {
            this.stopThread();
            throw new EICLException(ex.getMessage());
        }
        synchronized (this) {
            try {
                time = System.currentTimeMillis();
                this.wait(CONNECT_TIMEOUT * 1000);
            } catch (InterruptedException ex) {
            }
        }
        if (System.currentTimeMillis() - time >= CONNECT_TIMEOUT * 1000) {
            if (error.equals("")) {
                this.stopThread();
                throw new EICLException("Connection error: No answer from Server");
            }
        } else {
            if (error.equals("")) {
                heartbeat = new Heartbeat(server, socket, this);
                heartbeatThread = new Thread(heartbeat);
                heartbeatThread.start();
            } else {
                this.stopThread();
                if (heartbeat != null) {
                    heartbeat.stopThread();
                }
                throw new EICLException(error);
            }
        }
    }

    /**
     * Sends out a connect request message and waits for response. If the
     * connection has been established successfully the constructor starts the
     * receiving threads and heart beating. Otherwise a exception is thrown.
     * 
     * @param _Server
     *            The server to which connect
     * @param _Connection
     *            The Object which includes all relevant connection options
     * @throws EICLException
     */
    public CEMI_Connection(InetSocketAddress _Server, CEMIConnectionTypeInfoContainer _Connection) throws EICLException {
        this(Util.openNextPort(), _Server, _Connection);
    }

    /**
     * Adds a frame listener that is called on each incoming cEMI packets
     * 
     * @param eventListener
     */
    public void addFrameListener(EICLEventListener eventListener) {
        events.add(eventListener);
    }

    /**
     * Removes the frame listener
     * 
     * @param eventListener
     */
    public void removeFrameListener(EICLEventListener eventListener) {
        events.add(eventListener);
    }

    /**
     * Sends out the requested cEMI packet. Depending on the send mode it
     * returns immediately or after successful sending. (Tunnelling Ack received
     * and cEMI CON received).
     * 
     * @param _CEMI
     *            the cEMI frame to send
     * @param _Mode
     *            send mode
     * @throws EICLException
     *             if something goes wrong
     */
    public void sendFrame(CEMI _CEMI, boolean _Mode) throws EICLException {
        sendMode = _Mode;
        if (sendMode == IMMEDIATE_SEND && sendState < AWAITING_CEMI_CON) throw new EICLException("Last packet ack not received yet");
        if (sendMode == IMMEDIATE_SEND) sendState = WRONG_MODE;
        long time = 0;
        CEMI_Connection_Request request = new CEMI_Connection_Request(connection.getRequest(), server.getChannelid(), server.getSendSequence(), _CEMI);
        try {
            DatagramPacket packet = new DatagramPacket(request.toByteArray(), request.toByteArray().length, server.getDataEndpoint());
            socket.send(packet);
        } catch (Exception ex) {
            throw new EICLException(ex.getMessage());
        }
        if (sendMode == WAIT_FOR_CONFIRM) {
            synchronized (this) {
                try {
                    time = System.currentTimeMillis();
                    wait(SEND_TIMEOUT * 1000);
                } catch (InterruptedException ex) {
                }
            }
            if (System.currentTimeMillis() - time >= SEND_TIMEOUT * 1000) throw new EICLException("Send error: Could not send packet");
        } else {
            sendState = AWAITING_ACK;
        }
    }

    /**
     * Returns the current state of delivery of the sent packet, if the tunnel
     * is in immediate send mode, otherwise constantly returns WRONG_MODE.
     * 
     * @return the state (see constants)
     */
    public byte getState() {
        return sendState;
    }

    /**
     * Closes the connection and stops the heart beat thread. Then invokes all
     * registered event handlers with the given message.
     * 
     * @param _Message
     *            the message which is communicated to the event handlers
     * @throws EICLException
     *             If the wasn't sent successfully
     */
    public void disconnect(String _Message) throws EICLException {
        long time = 0;
        Disconnect_Request request = new Disconnect_Request(server.getChannelid(), socket.getLocalPort());
        try {
            DatagramPacket packet = new DatagramPacket(request.toByteArray(), request.toByteArray().length, server.getControlEndpoint());
            socket.send(packet);
        } catch (Exception ex) {
            throw new EICLException(ex.getMessage());
        }
        synchronized (this) {
            try {
                time = System.currentTimeMillis();
                wait(DISCONNECT_TIMEOUT * 1000);
            } catch (InterruptedException ex) {
            }
        }
        if (System.currentTimeMillis() - time > DISCONNECT_TIMEOUT * 1000) {
            stopThread();
            socket.close();
            _Message = ("Disconnect error: Request timed out");
        }
        cleanUp(_Message);
    }

    private void cleanUp(String _Message) {
        heartbeat.stopThread();
        heartbeatThread.interrupt();
        Object[] list = events.toArray();
        for (int i = 0; i < list.length; i++) {
            if (list[i] instanceof EICLEventListener) ((EICLEventListener) list[i]).serverDisconnected(new DisconnectEvent(this, _Message));
        }
    }

    private void stopThread() {
        stopThread = true;
        socket.close();
    }

    /**
     * The thread run method, waits for incoming messages. Called on startup.
     * Don't use this.
     */
    public void run() {
        byte[] buffer = new byte[128];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        while (!stopThread) {
            try {
                socket.receive(datagramPacket);
                EIBnetIPPacket packet = new EIBnetIPPacket(datagramPacket.getData(), datagramPacket.getLength());
                if (packet.getServiceType() == EIBNETIP_Constants.CONNECT_RESPONSE) {
                    Connect_Response response = new Connect_Response(packet.getBody());
                    if (response.getStatus() != EIBNETIP_Constants.E_NO_ERROR) {
                        error = ("Connect error: " + Util.getStatusString(response.getStatus()));
                    } else {
                        server.setControlEndpoint(response.getDataEndPoint());
                        server.setDataEndpoint(response.getDataEndPoint());
                        server.setChannelid((byte) response.getChannelID());
                    }
                    synchronized (this) {
                        this.notify();
                    }
                } else if (packet.getServiceType() == EIBNETIP_Constants.CONNECTIONSTATE_RESPONSE) {
                    Connectionstate_Response response = new Connectionstate_Response(packet.getBody());
                    if (response.getStatus() != EIBNETIP_Constants.E_NO_ERROR) throw new EICLException("Connectionstate error: " + Util.getStatusString(response.getStatus()));
                    synchronized (heartbeat) {
                        heartbeat.notify();
                    }
                } else if (packet.getServiceType() == EIBNETIP_Constants.DISCONNECT_RESPONSE) {
                    Disconnect_Response response = new Disconnect_Response(packet.getBody());
                    if (response.getStatus() != EIBNETIP_Constants.E_NO_ERROR) throw new EICLException("Disconnect error: " + Util.getStatusString(response.getStatus()));
                    synchronized (this) {
                        stopThread();
                        this.notify();
                    }
                } else if (packet.getServiceType() == connection.getAck()) {
                    CEMI_Connection_Ack request = new CEMI_Connection_Ack(packet.getBody());
                    if (request.getChannelid() == server.getChannelid() && request.getSequencecounter() == server.getSendSequence()) {
                        server.incSendSequence();
                        sendState = AWAITING_CEMI_CON;
                    } else {
                    }
                } else if (packet.getServiceType() == connection.getRequest()) {
                    CEMI_Connection_Request request = new CEMI_Connection_Request(connection.getRequest(), packet.getBody());
                    if (request.getChannelid() == server.getChannelid()) {
                        if (request.getSequenceNumber() == server.getReceiveSequence() || request.getSequenceNumber() == server.getReceiveSequence() - 1) {
                            CEMI_Connection_Ack ack = new CEMI_Connection_Ack(connection.getAck(), request.getChannelid(), request.getSequenceNumber(), EIBNETIP_Constants.E_NO_ERROR);
                            DatagramPacket data = new DatagramPacket(ack.toByteArray(), ack.toByteArray().length, server.getControlEndpoint());
                            socket.send(data);
                            if (request.getSequenceNumber() == server.getReceiveSequence()) {
                                server.incReceiveSequence();
                                if (request.getCemi().getMessageCode() != CEMI_L_DATA.MC_L_DATACON) {
                                    FrameEvent event = new FrameEvent(this, request.getCemi());
                                    dispatcher.queue(event);
                                } else if (request.getCemi().getMessageCode() == CEMI_L_DATA.MC_L_DATACON) {
                                    if (((CEMI_L_DATA) request.getCemi()).isPositiveConfirmation()) {
                                        if (sendMode == IMMEDIATE_SEND) {
                                            sendState = OK;
                                        } else {
                                            synchronized (this) {
                                                this.notify();
                                            }
                                        }
                                        FrameEvent event = new FrameEvent(this, request.getCemi());
                                        dispatcher.queue(event);
                                    }
                                } else {
                                }
                            }
                        }
                    }
                } else if (packet.getServiceType() == EIBNETIP_Constants.DISCONNECT_REQUEST) {
                    Disconnect_Request request = new Disconnect_Request(packet.getBody());
                    Disconnect_Response response;
                    if (request.getChannelID() == server.getChannelid() && server.getControlEndpoint().equals(datagramPacket.getSocketAddress())) {
                        response = new Disconnect_Response(server.getChannelid(), EIBNETIP_Constants.E_NO_ERROR);
                        cleanUp("Disconnect requested by server");
                    } else {
                        response = new Disconnect_Response(request.getChannelID(), EIBNETIP_Constants.E_CONNECTION_ID);
                    }
                    DatagramPacket datapacket = new DatagramPacket(response.toByteArray(), response.toByteArray().length, datagramPacket.getSocketAddress());
                    socket.send(datapacket);
                }
            } catch (Exception ex) {
                stopThread = true;
            }
        }
    }

    /**
     * The class takes care of distributing the received packets to the event listeners.
     * 
     * @author jburk
     * 
     */
    class QueueDispatcher implements Runnable {

        private final List queue = new Vector();

        QueueDispatcher() {
            Thread me = new Thread(this, "Receive-Queue-Dispatcher");
            me.setDaemon(true);
            me.start();
        }

        public synchronized void queue(FrameEvent event) {
            queue.add(event);
            notifyAll();
        }

        public void run() {
            while (true) {
                while (queue.size() > 0) {
                    try {
                        FrameEvent event = (FrameEvent) queue.remove(0);
                        Object[] list = events.toArray();
                        for (int i = 0; i < list.length; i++) {
                            if (list[i] instanceof EICLEventListener) ((EICLEventListener) list[i]).newFrameReceived(event);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}

/**
 * Inner container for connection attributes like control and data endpoint,
 * channelid and sequence numbers
 * 
 * @author bernhard erb
 * 
 */
class Connected_Server {

    private InetSocketAddress dataendpoint;

    private InetSocketAddress controlendpoint;

    private short channelid;

    private boolean active;

    private short receiveSequenceNumber;

    private short sendSequenceNumber;

    /**
     * Get the current sequence number for incomming packets
     * 
     * @return The receive sequence number as short
     */
    public short getReceiveSequence() {
        return receiveSequenceNumber;
    }

    /**
     * Get the current sequence number for outgoing packets
     * 
     * @return The send sequence number as short
     */
    public short getSendSequence() {
        return sendSequenceNumber;
    }

    /**
     * Increment the receive sequence number
     */
    public void incReceiveSequence() {
        receiveSequenceNumber++;
        if (receiveSequenceNumber > 255) receiveSequenceNumber = 0;
    }

    /**
     * Increment the send sequence number
     */
    public void incSendSequence() {
        sendSequenceNumber++;
        if (sendSequenceNumber > 255) sendSequenceNumber = 0;
    }

    /**
     * Set this object active or reset it.
     * 
     * @param _Active
     *            True indicates active, false reset.
     */
    public void setActive(boolean _Active) {
        active = _Active;
    }

    /**
     * The active flag indicates wheter the connection is active of was reseted.
     * 
     * @return Is this connection object active?
     */
    public boolean getActive() {
        return active;
    }

    /**
     * Set the data endpoint
     * 
     * @param _DataEndpoint
     *            the endpoint
     */
    public void setDataEndpoint(HPAI _DataEndpoint) {
        dataendpoint = new InetSocketAddress(_DataEndpoint.getAddressString(), _DataEndpoint.getPort());
    }

    /**
     * Set the control endpoint
     * 
     * @param _ControlEndpoint
     *            the endpoint
     */
    public void setControlEndpoint(HPAI _ControlEndpoint) {
        controlendpoint = new InetSocketAddress(_ControlEndpoint.getAddressString(), _ControlEndpoint.getPort());
    }

    /**
     * Set the channelid
     * 
     * @param _Channelid
     *            The channelid of this object
     */
    public void setChannelid(byte _Channelid) {
        channelid = _Channelid;
    }

    /**
     * Get the channelid
     * 
     * @return The channelid of this object
     */
    public short getChannelid() {
        return channelid;
    }

    /**
     * Get the data endpoint
     * 
     * @return the data endpoint
     */
    public InetSocketAddress getDataEndpoint() {
        return new InetSocketAddress(dataendpoint.getAddress(), dataendpoint.getPort());
    }

    /**
     * Get the control endpoint
     * 
     * @return the control endpoint
     */
    public InetSocketAddress getControlEndpoint() {
        return new InetSocketAddress(controlendpoint.getAddress(), controlendpoint.getPort());
    }
}

/**
 * Created on Apr 19, 2005
 * <p>
 * This class implements the EIBnet/IP connection heart beating. In an interval
 * of 1 minute, it sends connection state request messages. If the attempt was
 * not successful it retransmits the message two times, with an interval of 10
 * seconds. If after this period no answer was signaled to the thread it closes
 * the current connection. Incoming connection state acks are signaled by the
 * receive method.
 * 
 * @author Bernhard Erb
 * @see tuwien.auto.eicl.CEMI_Connection
 */
class Heartbeat implements Runnable {

    private Connectionstate_Request request;

    private boolean received = false;

    private boolean stop = false;

    private Connected_Server connected_Server;

    private DatagramPacket packet;

    private DatagramSocket socket;

    private CEMI_Connection tunnel;

    /**
     * Initlializes a new heart beat thread, and starts it
     * 
     * @param _Connected_Server
     *            the connected server object with control- and data endpoint
     * @param _DatagramSocket
     *            the tunnel socket (only used for sending)
     * @param _Tunnel
     *            the tunnel object (used for disconnecting)
     */
    public Heartbeat(Connected_Server _Connected_Server, DatagramSocket _DatagramSocket, CEMI_Connection _Tunnel) {
        connected_Server = _Connected_Server;
        socket = _DatagramSocket;
        tunnel = _Tunnel;
    }

    /**
     * Signalizes the thread that a connection state response has been received.
     */
    public void received() {
        received = true;
    }

    /**
     * Stops the receiving thread.
     */
    public void stopThread() {
        stop = true;
        socket.close();
    }

    /**
     * Sends messages in the above mentioned intervals.
     */
    public void run() {
        try {
            request = new Connectionstate_Request(connected_Server.getChannelid(), socket.getLocalPort());
            packet = new DatagramPacket(request.getByteArray(), request.getByteArray().length, connected_Server.getControlEndpoint());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (!stop) {
            try {
                received = false;
                synchronized (this) {
                    try {
                        wait(EIBNETIP_Constants.HEARTBEAT_REQUEST_TIMEOUT * 1000);
                    } catch (InterruptedException ex) {
                        received = true;
                    }
                }
                int nr = 0;
                long time = 0;
                while (received == false && nr < 3) {
                    socket.send(packet);
                    synchronized (this) {
                        try {
                            time = System.currentTimeMillis();
                            wait(EIBNETIP_Constants.CONNECTIONSTATE_REQUEST_TIMEOUT * 1000);
                        } catch (InterruptedException ex) {
                        }
                        if (System.currentTimeMillis() - time <= EIBNETIP_Constants.CONNECTIONSTATE_REQUEST_TIMEOUT * 1000) received = true;
                    }
                    nr++;
                }
                if (nr == 3) {
                    tunnel.disconnect("host unreachable");
                    stop = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
