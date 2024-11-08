package basys.eib;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apache.log4j.Logger;
import basys.eib.event.EIBFrameEvent;
import basys.eib.exceptions.EIBConnectionNotPossibleException;

/**
 * KNXnetIPConnection
 * Implements part of the KNXnet/IP specification version 1.0
 * a KNX protocol on top of IP networks for interconnection of KNX devices 
 * 
 * Supports tunneling on KNX linklayer on TP medium
 * Notes: not yet supported KNXnet/IP features are handled by an 
 * 		  internal exception (KNXnetIPNotSupportedException) 
 */
public class KNXnetIPConnection extends EIBConnection {

    public KNXnetIPConnection() {
        super();
    }

    /**
	 * Accessor for KNXnetIPConnection singelton
	 * @return an instance of this class
	 */
    public static EIBConnection getEIBConnection() {
        if (con == null) {
            con = new KNXnetIPConnection();
        }
        logger.debug("KNXnet/IP connection requested");
        return con;
    }

    public String getConnectionType() {
        return "KNXnet/IP Connection";
    }

    /**
	 * try to connect to KNXnet/IP server
	 */
    public void connect() throws EIBConnectionNotPossibleException {
        if (!isConnected()) {
            KNXnetIPAddressDialog dlg = new KNXnetIPAddressDialog(serverCtrlIP);
            dlg.setModal(true);
            dlg.setVisible(true);
            if (dlg.isCanceled()) throw new EIBConnectionNotPossibleException("Connect was canceled by user");
            serverCtrlIP = dlg.getAddress();
            serverDataIP = "";
            serverDataPort = 0;
            channelID = 0;
            serverSeqCounter = 0;
            localSeqCounter = 0;
            if (socket != null) try {
                socket.close();
            } catch (Exception e) {
            }
            try {
                socket = new DatagramSocket(localPort);
            } catch (Exception e) {
                System.out.println(e);
            }
            if (socket == null) {
                throw new EIBConnectionNotPossibleException("Port is busy");
            }
            try {
                KNXnetIPConRequest request = new KNXnetIPConRequest(InetAddress.getLocalHost().getHostAddress(), localPort);
                request.send(socket, serverCtrlIP, serverCtrlPort);
            } catch (Exception e) {
                logger.debug("Datagram error: " + e);
                return;
            }
            KNXnetIPConResponse response = new KNXnetIPConResponse();
            receive(response);
            if (response.getStatus() != KNXnetIPConResponse.E_NO_ERROR) throw new EIBConnectionNotPossibleException("KNXnet/IP error: server refused connection");
            frames.clear();
            channelID = response.getChannelID();
            serverDataIP = response.getDataEndPoint().getIP();
            serverDataPort = response.getDataEndPoint().getPort();
            int ia = response.getDesc().getIndividualAddress();
            individualAddress = new EIBPhaddress((ia >>> 12) & 0x0F, (ia >>> 8) & 0x0F, ia & 0xFF);
            setConnected(true);
            logger.debug("KNXnet/IP connection opened");
            conState = new HeartbeatMonitor();
            conState.start();
            receiver = new KNXnetIPReceiver();
            receiver.start();
        }
    }

    /**
	 * try to connect to KNXnet/IP server
	 */
    public void connect(String IP) throws EIBConnectionNotPossibleException {
        if (!isConnected()) {
            serverCtrlIP = IP;
            serverDataIP = "";
            serverDataPort = 0;
            channelID = 0;
            serverSeqCounter = 0;
            localSeqCounter = 0;
            if (socket != null) try {
                socket.close();
            } catch (Exception e) {
            }
            try {
                socket = new DatagramSocket(localPort);
            } catch (Exception e) {
            }
            if (socket == null) {
                throw new EIBConnectionNotPossibleException("Port is busy");
            }
            try {
                KNXnetIPConRequest request = new KNXnetIPConRequest(InetAddress.getLocalHost().getHostAddress(), localPort);
                request.send(socket, serverCtrlIP, serverCtrlPort);
            } catch (Exception e) {
                logger.debug("Datagram error: " + e);
                return;
            }
            KNXnetIPConResponse response = new KNXnetIPConResponse();
            receive(response);
            if (response.getStatus() != KNXnetIPConResponse.E_NO_ERROR) throw new EIBConnectionNotPossibleException("KNXnet/IP error: server refused connection");
            frames.clear();
            channelID = response.getChannelID();
            serverDataIP = response.getDataEndPoint().getIP();
            serverDataPort = response.getDataEndPoint().getPort();
            int ia = response.getDesc().getIndividualAddress();
            individualAddress = new EIBPhaddress((ia >>> 12) & 0x0F, (ia >>> 8) & 0x0F, ia & 0xFF);
            setConnected(true);
            logger.debug("KNXnet/IP connection opened");
            conState = new HeartbeatMonitor();
            conState.start();
            receiver = new KNXnetIPReceiver();
            receiver.start();
        }
    }

    /**
	 * disconnect from KNXnet/IP server
	 */
    public void disconnect() {
        if (!isConnected()) return;
        synchronized (socket) {
            try {
                KNXnetIPDisconRequest request = new KNXnetIPDisconRequest(channelID, InetAddress.getLocalHost().getHostAddress(), localPort);
                request.send(socket, serverCtrlIP, serverCtrlPort);
            } catch (Exception e) {
                System.out.println("Error en desconexio de la pasarela");
                logger.debug("Datagram error: " + e);
                return;
            }
            KNXnetIPDisconResponse response = new KNXnetIPDisconResponse();
            try {
                receive(response);
            } catch (EIBConnectionNotPossibleException e) {
                logger.debug("Error on closing KNXnet/IP connection");
            }
        }
        frames.clear();
        conState.interrupt();
        receiver.interrupt();
        try {
            if (receiver != Thread.currentThread()) receiver.join(2000);
            if (conState != Thread.currentThread()) conState.join(2000);
        } catch (InterruptedException e) {
        }
        socket.close();
        setConnected(false);
        logger.debug("KNXnet/IP connection closed");
    }

    /**
	 * send an EIBFrame as encapsulated cEMI frame within
	 * IP packets (tunnelling) 
	 */
    public void sendEIBFrame(EIBFrame ef) {
        if (!isConnected()) {
            System.out.println("KNXnet/IP server not connected");
            return;
        }
        CEMIFrame frame = new CEMIFrame(CEMIFrame.L_Data_req, ef);
        try {
            synchronized (socket) {
                KNXnetIPTunnelRequest request = new KNXnetIPTunnelRequest(channelID, getLocalSeqCounter(), frame);
                Debug.printFrame(request.getFrame());
                request.send(socket, serverDataIP, serverDataPort);
                KNXnetIPTunnelAck ack = new KNXnetIPTunnelAck();
                try {
                    receive(ack);
                } catch (EIBConnectionNotPossibleException e) {
                    request.send(socket, serverDataIP, serverDataPort);
                    receive(ack);
                }
                incLocalSeqCounter();
            }
        } catch (Exception e) {
            logger.debug("Datagram sending error: " + e);
        }
    }

    /**
	 * receiving wait loop for object frame 
	 * invalid datagrams are saved in queue
	 * method is only left through exception
	 */
    private void receive(KNXnetIPFrameBase obj) throws EIBConnectionNotPossibleException {
        while (!obj.receive(socket)) frames.add(obj.getInvalidPacket());
    }

    private synchronized void incLocalSeqCounter() {
        localSeqCounter = ++localSeqCounter & 255;
    }

    private synchronized int getLocalSeqCounter() {
        return localSeqCounter;
    }

    private synchronized void incServerSeqCounter() {
        serverSeqCounter = ++serverSeqCounter & 255;
    }

    private synchronized int getServerSeqCounter() {
        return serverSeqCounter;
    }

    private synchronized void setServerSeqCounter(int c) {
        serverSeqCounter = c;
    }

    private static EIBConnection con = null;

    private DatagramSocket socket;

    private String serverCtrlIP = "knxnetserver.myhome.org";

    private int serverCtrlPort = 3671;

    private String serverDataIP = "";

    private int serverDataPort = 0;

    private int localPort = 3675;

    private int channelID = 0;

    private int serverSeqCounter = 0;

    private int localSeqCounter = 0;

    private HeartbeatMonitor conState;

    private KNXnetIPReceiver receiver;

    private static Logger logger = Logger.getLogger(EIBGatewayConnection.class);

    /**
	 * HeartbeatMonitor 
	 * thread implementation for heartbeat monitoring 
	 * (CONNECTIONSTATE_REQUEST / CONNECTIONSTATE_RESPONSE)
	 */
    class HeartbeatMonitor extends Thread {

        public HeartbeatMonitor() {
            try {
                state = new KNXnetIPConstateRequest(channelID, InetAddress.getLocalHost().getHostAddress(), localPort);
                Debug.printFrame(state.getFrame());
            } catch (UnknownHostException e) {
            }
        }

        public void run() {
            while (true) {
                if (isInterrupted()) break;
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    int i = 0;
                    for (; i < MAX_REPEAT_REQUESTS; i++) {
                        synchronized (socket) {
                            logger.debug("Sending connection state request");
                            state.send(socket, serverCtrlIP, serverCtrlPort);
                            KNXnetIPConstateResponse res = new KNXnetIPConstateResponse();
                            try {
                                receive(res);
                                if (res.getStatus() == KNXnetIPConstateResponse.E_NO_ERROR) break; else System.out.println("Connectionstate response error 0x" + Integer.toHexString(res.getStatus() & 255));
                            } catch (EIBConnectionNotPossibleException e) {
                            }
                        }
                    }
                    if (i == MAX_REPEAT_REQUESTS) {
                        System.out.println("Communication failure: " + "no heartbeat response - closing KNXnet/IP connection");
                    }
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
        }

        private KNXnetIPConstateRequest state;

        private static final int HEARTBEAT_INTERVAL = 60000;

        private static final byte MAX_REPEAT_REQUESTS = 4;
    }

    /**
	 * KNXnetIPReceiver
	 * socket listener thread which
	 * - waits for incoming KNXnet/IP data packets,
	 * - observes the frame list and 
	 * - notifies event listener of new eibframes
	 */
    class KNXnetIPReceiver extends Thread {

        public KNXnetIPReceiver() {
        }

        public void run() {
            while (true) {
                if (isInterrupted()) break;
                try {
                    KNXnetIPTunnelRequest req = new KNXnetIPTunnelRequest() {

                        {
                            responseTime = 50;
                        }
                    };
                    receive(req);
                    byte[] fr = req.getFrame();
                    byte[] tmp = new byte[fr.length + 2];
                    System.arraycopy(fr, 0, tmp, 0, fr.length);
                    tmp[tmp.length - 1] = (byte) req.getCEMIFrame().getEIBFrame().getAck();
                    frames.addElement(tmp);
                } catch (EIBConnectionNotPossibleException e) {
                }
                while (frames.size() > 0) {
                    try {
                        KNXnetIPTunnelRequest frame = new KNXnetIPTunnelRequest((byte[]) frames.elementAt(0));
                        if (frame.getCEMIFrame().getMsgCode() == CEMIFrame.L_Data_con) {
                        } else if (frame.getCEMIFrame().getMsgCode() != CEMIFrame.L_Data_ind) logger.debug("tunnel request in queue with unexpected cEMI msg code: 0x" + Integer.toHexString(frame.getCEMIFrame().getMsgCode()));
                        boolean discard = false;
                        if (frame.getConnHeader().getCounter() == getServerSeqCounter() - 1) discard = true; else if (frame.getConnHeader().getCounter() != getServerSeqCounter()) {
                            logger.debug("sequence counter of server out of sync - corrected");
                            logger.debug("mine: " + Integer.toString(getServerSeqCounter()) + ", server: " + Integer.toString(frame.getConnHeader().getCounter()));
                            setServerSeqCounter(frame.getConnHeader().getCounter());
                        }
                        KNXnetIPTunnelAck conAck = new KNXnetIPTunnelAck(channelID, getServerSeqCounter(), KNXnetIPFrameBase.E_NO_ERROR);
                        conAck.send(socket, serverDataIP, serverDataPort);
                        if (!discard) {
                            incServerSeqCounter();
                            notifyListeners(frame.getCEMIFrame().getEIBFrame());
                        }
                    } catch (KNXnetIPInvalidDataException e) {
                        try {
                            KNXnetIPHeader header = new KNXnetIPHeader((byte[]) frames.elementAt(0));
                            short type = header.getServiceType();
                            if (type == KNXnetIPHeader.DISCONNECT_REQUEST) {
                                System.out.println("DISCONNECT_REQUEST in receiver - disconnect");
                                disconnect();
                                break;
                            } else if (type == KNXnetIPHeader.CONNECTIONSTATE_RESPONSE) logger.debug("CONNECTIONSTATE_RESPONSE in receiver");
                        } catch (KNXnetIPInvalidDataException ide) {
                            logger.debug("Unknown (invalid) data packet received: " + e);
                            Debug.printFrame((byte[]) frames.elementAt(0));
                        }
                    } catch (KNXnetIPNotSupportedException e) {
                        logger.debug("Unsupported frame received:" + e);
                        Debug.printFrame((byte[]) frames.elementAt(0));
                    }
                    frames.removeElementAt(0);
                }
            }
        }

        private void notifyListeners(EIBFrame frame) {
            EIBFrameEvent event = new EIBFrameEvent(this, frame);
            for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) ((EIBFrameListener) e.nextElement()).frameReceived(event);
        }
    }
}
