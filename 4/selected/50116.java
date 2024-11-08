package tuwien.auto.calimero.knxnetip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.servicetype.ErrorCodes;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceAck;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;
import tuwien.auto.calimero.knxnetip.util.TunnelCRI;
import tuwien.auto.calimero.log.LogLevel;

/**
 * KNXnet/IP connection for KNX tunneling.
 * <p>
 * The tunneling protocol specifies a point-to-point exchange of KNX frames over an IP
 * network connection between two KNXnet/IP devices - client and server.<br>
 * Up to now, only the client side is implemented.<br>
 * The communication on OSI layer 4 is done using UDP.<br>
 * 
 * @author B. Malinowsky
 */
public class KNXnetIPTunnel extends ConnectionImpl {

    /**
	 * Connection type used to tunnel between two KNXnet/IP devices (client / server).
	 * <p>
	 */
    public static final short TUNNEL_CONNECTION = 0x04;

    /**
	 * Tunneling on busmonitor layer, establishes a busmonitor tunnel to the KNX network.
	 * <p>
	 */
    public static final short BUSMONITOR_LAYER = 0x80;

    /**
	 * Tunneling on link layer, establishes a link layer tunnel to the KNX network.
	 * <p>
	 */
    public static final short LINK_LAYER = 0x02;

    /**
	 * Tunneling on raw layer, establishes a raw tunnel to the KNX network.
	 * <p>
	 */
    public static final short RAW_LAYER = 0x04;

    private static final int TUNNELING_REQ_TIMEOUT = 10;

    private final int layer;

    /**
	 * Creates a new KNXnet/IP tunneling connection to a remote server.
	 * <p>
	 * Establishing a raw tunneling layer ({@link #RAW_LAYER}) is not supported yet.<br>
	 * 
	 * @param KNXLayer KNX tunneling layer (e.g. {@link #LINK_LAYER})
	 * @param localEP specifies the local endpoint with the socket address to be used by
	 *        the tunnel
	 * @param serverCtrlEP control endpoint of the server to establish connection to
	 * @param useNAT <code>true</code> to use a NAT (network address translation) aware
	 *        communication mechanism, <code>false</code> to use the default way
	 * @throws KNXException on socket communication error
	 * @throws KNXTimeoutException on no connect response before connect timeout
	 * @throws KNXRemoteException if response indicates an error condition at the server
	 *         concerning the request
	 * @throws KNXInvalidResponseException if connect response is in wrong format
	 */
    public KNXnetIPTunnel(short KNXLayer, InetSocketAddress localEP, InetSocketAddress serverCtrlEP, boolean useNAT) throws KNXException {
        if (KNXLayer == RAW_LAYER) throw new KNXIllegalArgumentException("raw tunnel to KNX network not supported");
        if (KNXLayer != LINK_LAYER && KNXLayer != BUSMONITOR_LAYER) throw new KNXIllegalArgumentException("unknown KNX layer");
        layer = KNXLayer;
        responseTimeout = TUNNELING_REQ_TIMEOUT;
        serviceRequest = KNXnetIPHeader.TUNNELING_REQ;
        serviceAck = KNXnetIPHeader.TUNNELING_ACK;
        maxSendAttempts = 2;
        connect(localEP, serverCtrlEP, new TunnelCRI(KNXLayer), useNAT);
    }

    /**
	 * Sends a cEMI frame to the remote server communicating with this endpoint.
	 * <p>
	 * Sending in busmonitor mode is not permitted.<br>
	 * 
	 * @param frame cEMI message to send, the expected cEMI type is according to the used
	 *        tunneling layer
	 */
    public void send(CEMI frame, BlockingMode mode) throws KNXTimeoutException, KNXConnectionClosedException {
        if (layer == BUSMONITOR_LAYER) throw new KNXIllegalStateException("send not permitted in busmonitor mode");
        if (!(frame instanceof CEMILData)) throw new KNXIllegalArgumentException("unsupported cEMI type");
        super.send(frame, mode);
    }

    public String getName() {
        return "KNXnet/IP tunnel " + ctrlEP.getAddress().getHostAddress();
    }

    void handleService(KNXnetIPHeader h, byte[] data, int offset) throws KNXFormatException, IOException {
        final int svc = h.getServiceType();
        if (svc == KNXnetIPHeader.TUNNELING_REQ) {
            ServiceRequest req;
            try {
                req = PacketHelper.getServiceRequest(h, data, offset);
            } catch (final KNXFormatException e) {
                req = PacketHelper.getEmptyServiceRequest(h, data, offset);
                final byte[] junk = new byte[h.getTotalLength() - h.getStructLength() - 4];
                System.arraycopy(data, offset + 4, junk, 0, junk.length);
                logger.warn("received tunneling request with unknown cEMI part " + DataUnitBuilder.toHex(junk, " "), e);
            }
            if (req.getChannelID() != getChannelID()) {
                logger.warn("received wrong request channel-ID " + req.getChannelID() + ", expected " + getChannelID() + " - ignored");
                return;
            }
            final short seq = req.getSequenceNumber();
            if (seq == getSeqNoRcv() || seq + 1 == getSeqNoRcv()) {
                final short status = h.getVersion() == KNXNETIP_VERSION_10 ? ErrorCodes.NO_ERROR : ErrorCodes.VERSION_NOT_SUPPORTED;
                final byte[] buf = PacketHelper.toPacket(new ServiceAck(KNXnetIPHeader.TUNNELING_ACK, getChannelID(), seq, status));
                final DatagramPacket p = new DatagramPacket(buf, buf.length, dataEP.getAddress(), dataEP.getPort());
                socket.send(p);
                if (status == ErrorCodes.VERSION_NOT_SUPPORTED) {
                    close(ConnectionCloseEvent.INTERNAL, "protocol version changed", LogLevel.ERROR, null);
                    return;
                }
            } else logger.warn("tunneling request invalid receive-sequence " + seq + ", expected " + getSeqNoRcv());
            if (seq == getSeqNoRcv()) {
                incSeqNoRcv();
                final CEMI cemi = req.getCEMI();
                if (cemi == null) return;
                if (cemi.getMessageCode() == CEMILData.MC_LDATA_IND || cemi.getMessageCode() == CEMIBusMon.MC_BUSMON_IND) fireFrameReceived(cemi); else if (cemi.getMessageCode() == CEMILData.MC_LDATA_CON) {
                    fireFrameReceived(cemi);
                    setStateNotify(OK);
                } else if (cemi.getMessageCode() == CEMILData.MC_LDATA_REQ) logger.warn("received L-Data request - ignored");
            }
        } else logger.warn("received unknown frame (service type 0x" + Integer.toHexString(svc) + ") - ignored");
    }
}
