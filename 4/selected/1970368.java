package basys.eib;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.lang.Integer;
import basys.eib.exceptions.EIBConnectionNotPossibleException;

/**
 * debug routines
 */
class Debug {

    private static boolean noDebug = true;

    public static final void printFrame(byte[] f) {
        if (noDebug) return;
        System.out.println("frame length: " + f.length);
        for (int i = 0; i < f.length; i++) {
            if (Integer.toHexString(((int) f[i]) & 255).length() == 1) System.out.print(" ");
            System.out.print("    0x" + Integer.toHexString(((int) f[i]) & 255) + " ");
        }
        System.out.println();
        for (int i = 0; i < f.length; i++) System.out.print(new DecimalFormat("00000000 ").format(Integer.decode(Integer.toBinaryString(((int) f[i]) & 255))));
        System.out.println();
    }
}

/**
 * KNXnetIPNotSupportedException
 * thrown on a not supported connection/transmission mode
 */
class KNXnetIPNotSupportedException extends Exception {

    public KNXnetIPNotSupportedException() {
        super();
    }

    public KNXnetIPNotSupportedException(String message) {
        super(message);
    }

    public KNXnetIPNotSupportedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public KNXnetIPNotSupportedException(Throwable cause) {
        super(cause);
    }
}

/**
 * KNXnetIPInvalidDataException
 * thrown on wrong data format
 */
class KNXnetIPInvalidDataException extends Exception {

    public KNXnetIPInvalidDataException() {
        super();
    }

    public KNXnetIPInvalidDataException(String message) {
        super(message);
    }

    public KNXnetIPInvalidDataException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public KNXnetIPInvalidDataException(Throwable cause) {
        super(cause);
    }
}

/**
 * KNXnetIPHeader
 * common header for all KNXnet/IP data packets or frames
 */
class KNXnetIPHeader {

    public KNXnetIPHeader(short serviceType, int msgSize) throws KNXnetIPNotSupportedException {
        if (serviceType < CONNECT_REQUEST || serviceType > DISCONNECT_RESPONSE && serviceType != TUNNELLING_REQUEST && serviceType != TUNNELLING_ACK) throw new KNXnetIPNotSupportedException("Not supported service type");
        type = serviceType;
        size = msgSize;
    }

    public KNXnetIPHeader(byte[] buf) throws KNXnetIPInvalidDataException {
        if (buf[0] != HEADER_SIZE_10 || buf[1] != EIBNETIP_VERSION) throw new KNXnetIPInvalidDataException("Wrong header");
        type = (short) ((buf[2] << 8) | ((int) buf[3] & 255));
        if ((type < CONNECT_REQUEST || type > DISCONNECT_RESPONSE) && type != TUNNELLING_REQUEST && type != TUNNELLING_ACK) throw new KNXnetIPInvalidDataException("Not supported service type");
        size = ((buf[4] << 8) | ((int) buf[5] & 255)) - HEADER_SIZE_10;
        if (size < 0) throw new KNXnetIPInvalidDataException("Negative message body size");
    }

    public int getHeaderSize() {
        return HEADER_SIZE_10;
    }

    public void setMsgSize(int msgSize) {
        size = msgSize;
    }

    public int getMsgSize() {
        return size;
    }

    public short getServiceType() {
        return type;
    }

    public byte[] getHeader() {
        byte[] header = new byte[6];
        header[0] = HEADER_SIZE_10;
        header[1] = EIBNETIP_VERSION;
        header[2] = (byte) (type >>> 8);
        header[3] = (byte) (type & 255);
        header[4] = (byte) ((size + HEADER_SIZE_10) >>> 8);
        header[5] = (byte) ((size + HEADER_SIZE_10) & 255);
        return header;
    }

    public static final short CONNECT_REQUEST = 0x205;

    public static final short CONNECT_RESPONSE = 0x206;

    public static final short CONNECTIONSTATE_REQUEST = 0x207;

    public static final short CONNECTIONSTATE_RESPONSE = 0x208;

    public static final short DISCONNECT_REQUEST = 0x209;

    public static final short DISCONNECT_RESPONSE = 0x20A;

    public static final short TUNNELLING_REQUEST = 0x420;

    public static final short TUNNELLING_ACK = 0x421;

    private short type = 0;

    private int size = 0;

    private static final byte HEADER_SIZE_10 = 0x06;

    private static final byte EIBNETIP_VERSION = 0x10;
}

/**
 * HPAI
 * Host Protocol Address Information (HPAI) 
 * for KNXnet/IP host protocol IP version 4, UDP/TCP
 */
class HPAI {

    public HPAI(boolean useUDP, String hostIP, int hostPort) throws KNXnetIPNotSupportedException {
        if (useUDP) protocol = IPV4_UDP; else throw new KNXnetIPNotSupportedException("TCP host protocol requested");
        try {
            byte[] addr = InetAddress.getByName(hostIP).getAddress();
            ip1 = addr[0];
            ip2 = addr[1];
            ip3 = addr[2];
            ip4 = addr[3];
        } catch (Exception e) {
            System.out.println("HPAI initialization: " + e);
        }
        port = hostPort;
    }

    public HPAI(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        if (buf[0] != HPAI_SIZE) throw new KNXnetIPInvalidDataException("Wrong header");
        if (buf[1] != IPV4_UDP) throw new KNXnetIPNotSupportedException("Only UDP host protocol supported");
        protocol = buf[1];
        ip1 = buf[2];
        ip2 = buf[3];
        ip3 = buf[4];
        ip4 = buf[5];
        port = (((int) buf[6] & 255) << 8) | (((int) buf[7]) & 255);
    }

    public byte getSize() {
        return HPAI_SIZE;
    }

    public byte[] getHPAI() {
        byte[] hpai = new byte[HPAI_SIZE];
        hpai[0] = HPAI_SIZE;
        hpai[1] = protocol;
        hpai[2] = (byte) ip1;
        hpai[3] = (byte) ip2;
        hpai[4] = (byte) ip3;
        hpai[5] = (byte) ip4;
        hpai[6] = (byte) (port >>> 8);
        hpai[7] = (byte) (port & 255);
        return hpai;
    }

    public String getIP() {
        return (ip1 & 0xff) + "." + (ip2 & 0xff) + "." + (ip3 & 0xff) + "." + (ip4 & 0xff);
    }

    public int getPort() {
        return port;
    }

    private byte protocol = IPV4_UDP;

    private int ip1 = 0, ip2 = 0, ip3 = 0, ip4 = 0;

    private int port = 0;

    private static final byte HPAI_SIZE = 0x08;

    private static final byte IPV4_UDP = 0x01;

    private static final byte IPV4_TCP = 0x02;
}

/**
 * CRI
 * Connection Request Information structure
 */
class CRI {

    public CRI(byte connectionType, short KNXLayer) throws KNXnetIPNotSupportedException {
        if (connectionType != TUNNEL_CONNECTION) throw new KNXnetIPNotSupportedException("Only tunnelling is supported");
        type = connectionType;
        if (KNXLayer != TUNNEL_LINKLAYER) throw new KNXnetIPNotSupportedException("Only KNX linklayer tunnelling is supported");
        layer = KNXLayer;
    }

    byte[] getCRI() {
        byte[] cri = new byte[CRI_SIZE];
        cri[0] = CRI_SIZE;
        cri[1] = type;
        cri[2] = (byte) (layer & 255);
        return cri;
    }

    public static final byte DEVICE_MGMT_CONNECTION = 0x03;

    public static final byte TUNNEL_CONNECTION = 0x04;

    public static final byte REMLOG_CONNECTION = 0x06;

    public static final byte REMCONF_CONNECTION = 0x07;

    public static final byte OBJSVR_CONNECTION = 0x08;

    public static final short TUNNEL_LINKLAYER = 0x02;

    public static final short TUNNEL_RAW = 0x04;

    public static final short TUNNEL_BUSMONITOR = 0x80;

    private static final byte CRI_SIZE = 0x04;

    private byte type = TUNNEL_CONNECTION;

    private short layer = TUNNEL_LINKLAYER;
}

/**
 * CRD
 * Connection Response Description structure
 */
class CRD {

    public CRD(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        if (buf[0] != CRD_SIZE) throw new KNXnetIPInvalidDataException("Wrong header");
        if (buf[1] < DEVICE_MGMT_CONNECTION || buf[1] > OBJSVR_CONNECTION) throw new KNXnetIPInvalidDataException("Invalid connection type");
        if (buf[1] != TUNNEL_CONNECTION) throw new KNXnetIPNotSupportedException("Not supported connection type");
        addr = ((buf[2] << 8) | ((int) buf[3] & 255));
    }

    public int getIndividualAddress() {
        return addr;
    }

    public byte getConnectionType() {
        return type;
    }

    public static final byte DEVICE_MGMT_CONNECTION = 0x03;

    public static final byte TUNNEL_CONNECTION = 0x04;

    public static final byte REMLOG_CONNECTION = 0x06;

    public static final byte REMCONF_CONNECTION = 0x07;

    public static final byte OBJSVR_CONNECTION = 0x08;

    private static final byte CRD_SIZE = 0x04;

    private byte type = TUNNEL_CONNECTION;

    private int addr = 0;
}

/**
 * KNXnetIPFrameBase
 * base class for KNXnet/IP protocol frames
 */
abstract class KNXnetIPFrameBase {

    KNXnetIPFrameBase() {
        super();
    }

    /**
	 * wait a certain timespan for a matching frame from the socket
	 * exception always indicates a connection timeout, even tough there can be
	 * a more serious event (broken connection...)
	 */
    public boolean receive(DatagramSocket socket) throws EIBConnectionNotPossibleException {
        byte[] buf = new byte[512];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        long start = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() > (start + responseTime)) throw new EIBConnectionNotPossibleException("KNXnet/IP error: connection timeout");
            try {
                synchronized (socket) {
                    socket.setSoTimeout(responseTime);
                    socket.receive(p);
                }
            } catch (SocketTimeoutException ste) {
                throw new EIBConnectionNotPossibleException("KNXnet/IP error: connection timeout");
            } catch (Exception e) {
                throw new EIBConnectionNotPossibleException(e.getMessage());
            }
            try {
                fillFrame((byte[]) buf.clone());
                invalidPacket = null;
                return true;
            } catch (KNXnetIPInvalidDataException e) {
                invalidPacket = buf;
                return false;
            } catch (KNXnetIPNotSupportedException e) {
                throw new EIBConnectionNotPossibleException("KNXnet/IP error: unsupported mode");
            }
        }
    }

    /**
	 * send the frame over socket 
	 */
    public boolean send(DatagramSocket socket, String ipAddr, int port) {
        try {
            byte[] buf = getFrame();
            DatagramPacket p = new DatagramPacket(buf, buf.length, InetAddress.getByName(ipAddr), port);
            socket.send(p);
        } catch (Exception e) {
            System.out.println("Datagram error: " + e);
            return false;
        }
        return true;
    }

    /**
	 * try to fill the object with data out of buf[]
	 */
    public abstract void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException;

    /**
	 * return the byte frame
	 */
    public byte[] getFrame() {
        System.out.println("getFrame NYI");
        return null;
    }

    /**
	 * generates a copy from buf from position startPos on
	 */
    protected byte[] copy(byte[] buf, int startPos) {
        byte[] tmp = new byte[buf.length - startPos];
        System.arraycopy(buf, startPos, tmp, 0, tmp.length);
        return tmp;
    }

    /**
	 * concatenates buf1 and buf2
	 */
    protected byte[] concat(byte[] buf1, byte[] buf2) {
        if (buf2 != null) {
            byte[] tmp = new byte[buf1.length + buf2.length];
            System.arraycopy(buf1, 0, tmp, 0, buf1.length);
            System.arraycopy(buf2, 0, tmp, buf1.length, buf2.length);
            return tmp;
        }
        return (byte[]) (buf1.clone());
    }

    public byte[] getInvalidPacket() {
        return invalidPacket;
    }

    private byte[] invalidPacket;

    protected int responseTime = 0;

    protected static final byte E_NO_ERROR = 0x00;
}

/**
 * KNXnetIPConnectionFrame
 * common base for KNXnet/IP connection classes
 */
abstract class KNXnetIPConnectionFrame extends KNXnetIPFrameBase {

    KNXnetIPConnectionFrame() {
        super();
        responseTime = CONNECT_REQUEST_TIMEOUT;
    }

    public int getChannelID() {
        return channelID;
    }

    public byte getStatus() {
        return status;
    }

    protected KNXnetIPHeader header;

    protected int channelID;

    protected byte status;

    protected static final int CONNECT_REQUEST_TIMEOUT = 10000;

    protected static final byte E_CONNECTION_TYPE = 0x22;

    protected static final byte E_CONNECTION_OPTION = 0x23;

    protected static final byte E_NO_MORE_CONNECTIONS = 0x24;
}

/**
 * KNXnetIPConRequest
 * connection request frame for a KNXnet/IP connection
 */
class KNXnetIPConRequest extends KNXnetIPConnectionFrame {

    KNXnetIPConRequest(String clientIP, int clientPort) {
        super();
        try {
            header = new KNXnetIPHeader(KNXnetIPHeader.CONNECT_REQUEST, 0);
            ctrl = new HPAI(true, clientIP, clientPort);
            data = new HPAI(true, clientIP, clientPort);
            cri = new CRI(CRI.TUNNEL_CONNECTION, CRI.TUNNEL_LINKLAYER);
        } catch (KNXnetIPNotSupportedException e) {
            System.out.println("Wrong initialization: " + e);
        }
    }

    public byte[] getFrame() {
        byte[] ctrlBuf = ctrl.getHPAI();
        byte[] dataBuf = data.getHPAI();
        byte[] criBuf = cri.getCRI();
        int msgLength = ctrlBuf.length + dataBuf.length + criBuf.length;
        header.setMsgSize(msgLength);
        return concat(concat(concat(header.getHeader(), ctrlBuf), dataBuf), criBuf);
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        System.out.println("NYI");
    }

    private HPAI ctrl;

    private HPAI data;

    private CRI cri;
}

/**
 * KNXnetIPConResponse
 * answer frame to KNXnet/IP connection request
 */
class KNXnetIPConResponse extends KNXnetIPConnectionFrame {

    KNXnetIPConResponse() {
        super();
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        header = new KNXnetIPHeader(buf);
        if (header.getServiceType() != KNXnetIPHeader.CONNECT_RESPONSE) throw new KNXnetIPInvalidDataException("Wrong data packet");
        channelID = buf[header.getHeaderSize()];
        status = buf[header.getHeaderSize() + 1];
        if (status != E_NO_ERROR) return;
        hpai = new HPAI(copy(buf, header.getHeaderSize() + 2));
        crd = new CRD(copy(buf, header.getHeaderSize() + 2 + hpai.getSize()));
    }

    public HPAI getDataEndPoint() {
        return hpai;
    }

    public CRD getDesc() {
        return crd;
    }

    private HPAI hpai;

    private CRD crd;
}

/**
  * KNXnetIPDisconRequest
  * KNXnet/IP disconnection request frame
  */
class KNXnetIPDisconRequest extends KNXnetIPConnectionFrame {

    KNXnetIPDisconRequest(int channelID, String clientIP, int clientPort) {
        super();
        channel = channelID;
        try {
            ctrl = new HPAI(true, clientIP, clientPort);
            header = new KNXnetIPHeader(KNXnetIPHeader.DISCONNECT_REQUEST, 2 + ctrl.getSize());
        } catch (KNXnetIPNotSupportedException e) {
            System.out.println("Wrong initialization: " + e);
        }
    }

    public byte[] getFrame() {
        byte[] buf = new byte[2];
        buf[0] = (byte) channel;
        buf[1] = 0;
        return concat(concat(header.getHeader(), buf), ctrl.getHPAI());
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        System.out.println("NYI");
    }

    private int channel = 0;

    private HPAI ctrl;
}

/**
 * KNXnetIPDisconResponse
 * KNXnet/IP disconnection response frame
 */
class KNXnetIPDisconResponse extends KNXnetIPConnectionFrame {

    KNXnetIPDisconResponse() {
        super();
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        header = new KNXnetIPHeader(buf);
        if (header.getServiceType() != KNXnetIPHeader.DISCONNECT_RESPONSE) throw new KNXnetIPInvalidDataException("Wrong data packet");
        channelID = buf[header.getHeaderSize()];
        status = buf[header.getHeaderSize() + 1];
    }
}

/**
 * KNXnetIPConstateRequest
 * KNXnet/IP connection state request frame
 * Note: structure is basically the same as disconnection request
 */
class KNXnetIPConstateRequest extends KNXnetIPConnectionFrame {

    KNXnetIPConstateRequest(int channelID, String clientIP, int clientPort) {
        super();
        channel = channelID;
        try {
            ctrl = new HPAI(true, clientIP, clientPort);
            header = new KNXnetIPHeader(KNXnetIPHeader.CONNECTIONSTATE_REQUEST, 2 + ctrl.getSize());
        } catch (KNXnetIPNotSupportedException e) {
            System.out.println("Wrong initialization: " + e);
        }
    }

    public byte[] getFrame() {
        byte[] buf = new byte[2];
        buf[0] = (byte) channel;
        buf[1] = 0;
        return concat(concat(header.getHeader(), buf), ctrl.getHPAI());
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        System.out.println("NYI");
    }

    private int channel = 0;

    private HPAI ctrl;
}

/**
 * KNXnetIPConstateResponse
 * KNXnet/IP connection state response frame
 * Note: structure is basically the same as disconnection response
 */
class KNXnetIPConstateResponse extends KNXnetIPConnectionFrame {

    KNXnetIPConstateResponse() {
        super();
        responseTime = CONNECTIONSTATE_REQUEST_TIMEOUT;
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        header = new KNXnetIPHeader(buf);
        if (header.getServiceType() != KNXnetIPHeader.CONNECTIONSTATE_RESPONSE) throw new KNXnetIPInvalidDataException("Wrong data packet");
        channelID = buf[header.getHeaderSize()];
        status = buf[header.getHeaderSize() + 1];
    }

    private static final int CONNECTIONSTATE_REQUEST_TIMEOUT = 10000;

    public static final byte E_CONNECTION_ID = 0x21;

    public static final byte E_DATA_CONNECTION = 0x26;

    public static final byte E_KNX_CONNECTION = 0x27;
}

/**
 * CEMIFrame
 * common external message interface frame 
 * for medium independent KNX messages (cEMI specification)
 */
class CEMIFrame {

    CEMIFrame(short msgCode, EIBFrame eibFrame) {
        code = msgCode;
        eib = eibFrame;
    }

    CEMIFrame(byte[] buf) {
        fillFrame(buf);
    }

    public byte[] getcEMI() {
        int[] frame = eib.getFrame();
        int infoLen = frame.length - 6;
        byte[] buf = new byte[9 + infoLen];
        buf[0] = (byte) code;
        buf[1] = 0;
        buf[2] = (byte) frame[0];
        if (eib.getAck() != 0xCC) buf[2] += 1;
        buf[3] = (byte) (frame[5] & (15 << 4));
        buf[4] = (byte) frame[1];
        buf[5] = (byte) frame[2];
        buf[6] = (byte) frame[3];
        buf[7] = (byte) frame[4];
        buf[8] = (byte) (frame[5] & 15);
        for (int i = 0; i < infoLen; i++) buf[i + 9] = (byte) (frame[i + 6] & 255);
        return buf;
    }

    public void fillFrame(byte[] buf) {
        code = (short) (buf[0] & 255);
        int i = buf[1];
        if (i != 0) System.out.println("cEMI frame contains additional information - ignored");
        i += 2;
        int[] eibFrame = new int[9 + buf[i + 6]];
        int ctrl1 = buf[i++] & 255;
        eibFrame[0] = ctrl1;
        int ctrl2 = buf[i++] & 255;
        eibFrame[1] = buf[i++] & 255;
        eibFrame[2] = buf[i++] & 255;
        eibFrame[3] = buf[i++] & 255;
        eibFrame[4] = buf[i++] & 255;
        eibFrame[5] = ctrl2 & 0xF0;
        int dataLength = buf[i++];
        eibFrame[5] |= dataLength & 0xF;
        for (int cnt = 0; cnt <= dataLength; cnt++) eibFrame[cnt + 6] = buf[i++] & 255;
        eibFrame[dataLength + 7] = 0;
        if ((ctrl1 & 1) == 1) {
            eibFrame[dataLength + 8] = 0xC;
        } else {
            eibFrame[dataLength + 8] = 0xCC;
        }
        eib = new EIBFrame(eibFrame);
    }

    public EIBFrame getEIBFrame() {
        return eib;
    }

    public short getMsgCode() {
        return code;
    }

    private short code = 0;

    private EIBFrame eib = null;

    public static final short L_Data_req = 0x11;

    public static final short L_Data_con = 0x2E;

    public static final short L_Data_ind = 0x29;
}

/**
 * KNXnetIPConnHeader
 * header structure used in tunnel frames
 */
class KNXnetIPConnHeader {

    KNXnetIPConnHeader(int channID, int seqCounter) {
        this(channID, seqCounter, (byte) 0);
    }

    KNXnetIPConnHeader(int channID, int seqCounter, byte error) {
        channelID = channID;
        count = seqCounter;
        err = error;
    }

    KNXnetIPConnHeader(byte[] buf) throws KNXnetIPInvalidDataException {
        if (buf[0] != HEADER_SIZE) throw new KNXnetIPInvalidDataException("Wrong header");
        channelID = buf[1];
        count = buf[2];
        err = buf[3];
    }

    public byte getHeaderSize() {
        return HEADER_SIZE;
    }

    public byte[] getConnHeader() {
        byte[] header = new byte[HEADER_SIZE];
        header[0] = HEADER_SIZE;
        header[1] = (byte) (channelID & 255);
        header[2] = (byte) (count & 255);
        header[3] = err;
        return header;
    }

    public int getCounter() {
        return count;
    }

    private int channelID = 0;

    private int count = 0;

    private byte err = 0;

    private static final byte HEADER_SIZE = 0x04;
}

/**
 * KNXnetIPTunnelRequest
 * tunnel a cEMI telegram frame 
 */
class KNXnetIPTunnelRequest extends KNXnetIPFrameBase {

    KNXnetIPTunnelRequest() {
        super();
    }

    KNXnetIPTunnelRequest(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        fillFrame(buf);
    }

    KNXnetIPTunnelRequest(int channelID, int seqCounter, CEMIFrame frame) {
        super();
        byte[] f = frame.getcEMI();
        connh = new KNXnetIPConnHeader(channelID, seqCounter);
        try {
            header = new KNXnetIPHeader(KNXnetIPHeader.TUNNELLING_REQUEST, f.length + connh.getHeaderSize());
        } catch (KNXnetIPNotSupportedException e) {
        }
        cEMI = frame;
    }

    public byte[] getFrame() {
        return concat(concat(header.getHeader(), connh.getConnHeader()), cEMI.getcEMI());
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        header = new KNXnetIPHeader(buf);
        if (header.getServiceType() != KNXnetIPHeader.TUNNELLING_REQUEST) throw new KNXnetIPInvalidDataException("Wrong data packet");
        connh = new KNXnetIPConnHeader(copy(buf, header.getHeaderSize()));
        cEMI = new CEMIFrame(copy(buf, header.getHeaderSize() + connh.getHeaderSize()));
    }

    public CEMIFrame getCEMIFrame() {
        return cEMI;
    }

    public KNXnetIPConnHeader getConnHeader() {
        return connh;
    }

    private KNXnetIPHeader header;

    private KNXnetIPConnHeader connh;

    private CEMIFrame cEMI;
}

/**
 * KNXnetIPTunnelAck
 * confirmation for a tunnelled data packet
 */
class KNXnetIPTunnelAck extends KNXnetIPFrameBase {

    KNXnetIPTunnelAck() {
        super();
        responseTime = TUNNELLING_REQUEST_TIMEOUT;
    }

    KNXnetIPTunnelAck(int channelID, int seqCounter, byte error) {
        super();
        connh = new KNXnetIPConnHeader(channelID, seqCounter, error);
        try {
            header = new KNXnetIPHeader(KNXnetIPHeader.TUNNELLING_ACK, connh.getHeaderSize());
        } catch (KNXnetIPNotSupportedException e) {
        }
    }

    public byte[] getFrame() {
        return concat(header.getHeader(), connh.getConnHeader());
    }

    public void fillFrame(byte[] buf) throws KNXnetIPInvalidDataException, KNXnetIPNotSupportedException {
        header = new KNXnetIPHeader(buf);
        if (header.getServiceType() != KNXnetIPHeader.TUNNELLING_ACK) throw new KNXnetIPInvalidDataException("Wrong data packet");
        connh = new KNXnetIPConnHeader(copy(buf, header.getHeaderSize()));
    }

    public KNXnetIPConnHeader getConnHeader() {
        return connh;
    }

    private KNXnetIPHeader header;

    private KNXnetIPConnHeader connh;

    private static final int TUNNELLING_REQUEST_TIMEOUT = 1000;
}
