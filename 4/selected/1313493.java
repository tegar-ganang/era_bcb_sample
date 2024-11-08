package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Common service request structure, used to send requests over established KNXnet/IP
 * communication channels.
 * <p>
 * Such a service request is used for tunnel or device management connections. It carries
 * a cEMI frame containing the actual KNX frame data.<br>
 * A service request is contained in the body of a KNXnet/IP frame.
 * 
 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceAck
 */
public class ServiceRequest extends ServiceType {

    private static final int CONN_HEADER_SIZE = 4;

    private final short channelid;

    private final short seq;

    private CEMI cemi;

    /**
	 * Creates a new service request out of a byte array.
	 * <p>
	 * 
	 * @param serviceType service request type identifier describing the request in
	 *        <code>data</code>, 0 &lt;= type &lt;= 0xFFFF
	 * @param data byte array containing a service request structure
	 * @param offset start offset in bytes of request in <code>data</code>
	 * @param length the length in bytes of the whole request contained in
	 *        <code>data</code>
	 * @throws KNXFormatException if buffer is too short for request, on unsupported
	 *         service type or connection header structure
	 */
    public ServiceRequest(int serviceType, byte[] data, int offset, int length) throws KNXFormatException {
        this(serviceType, data, offset, length, null);
        if (svcType == KNXnetIPHeader.TUNNELING_REQ) cemi = CEMIFactory.create(data, offset + CONN_HEADER_SIZE, length - CONN_HEADER_SIZE); else if (svcType == KNXnetIPHeader.DEVICE_CONFIGURATION_REQ) cemi = new CEMIDevMgmt(data, offset + CONN_HEADER_SIZE, length - CONN_HEADER_SIZE); else throw new KNXIllegalArgumentException("unsupported service request type");
    }

    /**
	 * Creates a new service request.
	 * <p>
	 * 
	 * @param serviceType service request type identifier, 0 &lt;= type &lt;= 0xFFFF
	 * @param channelID channel ID of communication this request belongs to, 0 &lt;= id
	 *        &lt;= 255
	 * @param seqNumber the sending sequence number of the communication channel, 0 &lt;=
	 *        number &lt;= 255
	 * @param frame cEMI frame carried with the request
	 */
    public ServiceRequest(int serviceType, int channelID, int seqNumber, CEMI frame) {
        super(serviceType);
        if (serviceType < 0 || serviceType > 0xffff) throw new KNXIllegalArgumentException("service request out of range [0..0xffff]");
        if (channelID < 0 || channelID > 0xff) throw new KNXIllegalArgumentException("channel ID out of range [0..0xff]");
        if (seqNumber < 0 || seqNumber > 0xff) throw new KNXIllegalArgumentException("sequence number out of range [0..0xff]");
        channelid = (short) channelID;
        seq = (short) seqNumber;
        cemi = CEMIFactory.copy(frame);
    }

    ServiceRequest(int serviceType, byte[] data, int offset, int length, CEMI frame) throws KNXFormatException {
        super(serviceType);
        if (length < CONN_HEADER_SIZE + 1) throw new KNXFormatException("buffer too short for service request");
        final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
        if (is.read() != CONN_HEADER_SIZE) throw new KNXFormatException("unsupported connection header");
        channelid = (short) is.read();
        seq = (short) is.read();
        is.read();
        cemi = frame;
    }

    /**
	 * Returns the service type identifier of the request.
	 * <p>
	 * 
	 * @return service type as unsigned short
	 */
    public final int getServiceType() {
        return svcType;
    }

    /**
	 * Returns the communication channel identifier associated with the request.
	 * <p>
	 * 
	 * @return communication channel ID as unsigned byte
	 */
    public final short getChannelID() {
        return channelid;
    }

    /**
	 * Returns the sequence number of the sending endpoint.
	 * <p>
	 * 
	 * @return sequence number as unsigned byte
	 */
    public final short getSequenceNumber() {
        return seq;
    }

    /**
	 * Returns the cEMI frame carried by the request.
	 * <p>
	 * 
	 * @return a cEMI type
	 */
    public final CEMI getCEMI() {
        return CEMIFactory.copy(cemi);
    }

    short getStructLength() {
        return (short) (CONN_HEADER_SIZE + (cemi != null ? cemi.getStructLength() : 0));
    }

    byte[] toByteArray(ByteArrayOutputStream os) {
        os.write(CONN_HEADER_SIZE);
        os.write(channelid);
        os.write(seq);
        os.write(0);
        final byte[] buf = cemi.toByteArray();
        os.write(buf, 0, buf.length);
        return os.toByteArray();
    }
}
