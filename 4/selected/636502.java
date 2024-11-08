package tuwien.auto.calimero.knxnetip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXInvalidResponseException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.servicetype.ErrorCodes;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceAck;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;
import tuwien.auto.calimero.knxnetip.util.CRI;
import tuwien.auto.calimero.log.LogLevel;

/**
 * KNXnet/IP connection for KNX local device management.
 * <p>
 * The communication on OSI layer 4 is done using UDP.<br>
 * 
 * @author B. Malinowsky
 */
public class KNXnetIPDevMgmt extends ConnectionImpl {

    /**
	 * Connection type used to configure a KNXnet/IP device.
	 * <p>
	 */
    public static final short DEVICE_MGMT_CONNECTION = 0x03;

    private static final int CONFIGURATION_REQ_TIMEOUT = 10;

    /**
	 * Creates a new KNXnet/IP device management connection to a remote device.
	 * <p>
	 * 
	 * @param localEP the local endpoint to use for communication channel
	 * @param serverCtrlEP the remote server control endpoint used for connect request
	 * @param useNAT <code>true</code> to use a NAT (network address translation) aware
	 *        communication mechanism, <code>false</code> to use the default way
	 * @throws KNXException on socket communication error
	 * @throws KNXTimeoutException on no connect response before connect timeout
	 * @throws KNXRemoteException if response indicates an error condition at the server
	 *         concerning the request
	 * @throws KNXInvalidResponseException if connect response is in wrong format
	 */
    public KNXnetIPDevMgmt(InetSocketAddress localEP, InetSocketAddress serverCtrlEP, boolean useNAT) throws KNXException {
        responseTimeout = CONFIGURATION_REQ_TIMEOUT;
        serviceRequest = KNXnetIPHeader.DEVICE_CONFIGURATION_REQ;
        serviceAck = KNXnetIPHeader.DEVICE_CONFIGURATION_ACK;
        maxSendAttempts = 4;
        try {
            final CRI cri = CRI.createRequest(DEVICE_MGMT_CONNECTION, null);
            connect(localEP, serverCtrlEP, cri, useNAT);
        } catch (final KNXFormatException ignore) {
        }
    }

    /**
	 * Sends a cEMI device management frame to the remote server communicating with this
	 * endpoint.
	 * <p>
	 * 
	 * @param frame cEMI device management message of type {@link CEMIDevMgmt} to send
	 */
    public void send(CEMI frame, BlockingMode mode) throws KNXTimeoutException, KNXConnectionClosedException {
        if (!(frame instanceof CEMIDevMgmt)) throw new KNXIllegalArgumentException("unsupported cEMI type");
        super.send(frame, mode);
    }

    public String getName() {
        return "KNXnet/IP DM " + ctrlEP.getAddress().getHostAddress();
    }

    void handleService(KNXnetIPHeader h, byte[] data, int offset) throws KNXFormatException, IOException {
        final int svc = h.getServiceType();
        if (svc == KNXnetIPHeader.DEVICE_CONFIGURATION_REQ) {
            ServiceRequest req;
            try {
                req = PacketHelper.getServiceRequest(h, data, offset);
            } catch (final KNXFormatException e) {
                req = PacketHelper.getEmptyServiceRequest(h, data, offset);
                final byte[] junk = new byte[h.getTotalLength() - h.getStructLength() - 4];
                System.arraycopy(data, offset + 4, junk, 0, junk.length);
                logger.warn("received dev.mgmt request with unknown cEMI part " + DataUnitBuilder.toHex(junk, " "), e);
            }
            final short seq = req.getSequenceNumber();
            if (req.getChannelID() == getChannelID() && seq == getSeqNoRcv()) {
                final short status = h.getVersion() == KNXNETIP_VERSION_10 ? ErrorCodes.NO_ERROR : ErrorCodes.VERSION_NOT_SUPPORTED;
                final byte[] buf = PacketHelper.toPacket(new ServiceAck(KNXnetIPHeader.DEVICE_CONFIGURATION_ACK, getChannelID(), seq, status));
                final DatagramPacket p = new DatagramPacket(buf, buf.length, dataEP.getAddress(), dataEP.getPort());
                socket.send(p);
                incSeqNoRcv();
                if (status == ErrorCodes.VERSION_NOT_SUPPORTED) {
                    close(ConnectionCloseEvent.INTERNAL, "protocol version changed", LogLevel.ERROR, null);
                    return;
                }
                final CEMI cemi = req.getCEMI();
                if (cemi == null) return;
                final short mc = cemi.getMessageCode();
                if (mc == CEMIDevMgmt.MC_PROPINFO_IND || mc == CEMIDevMgmt.MC_RESET_IND) fireFrameReceived(cemi); else if (mc == CEMIDevMgmt.MC_PROPREAD_CON || mc == CEMIDevMgmt.MC_PROPWRITE_CON) {
                    fireFrameReceived(cemi);
                    setStateNotify(OK);
                }
            } else logger.warn("received dev.mgmt request channel-ID " + req.getChannelID() + ", receive-sequence " + seq + ", expected " + getSeqNoRcv() + " - ignored");
        } else logger.warn("received unknown frame (service type 0x" + Integer.toHexString(svc) + ") - ignored");
    }
}
