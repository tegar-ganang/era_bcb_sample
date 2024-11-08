package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Common KNXnet/IP acknowledge structure, used to send acknowledges over established
 * KNXnet/IP communication channels.
 * <p>
 * An acknowledge is sent in reply to a service request, to confirm the reception of the
 * request over the IP communication channel. Note, it doesn't tell anything about
 * delivery on the KNX network.
 * 
 * @see tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest
 */
public class ServiceAck extends ServiceType {

    private static final int CONN_HEADER_SIZE = 4;

    private final short channelid;

    private final short seq;

    private final short status;

    /**
	 * Creates a new service acknowledge out of a byte array.
	 * <p>
	 * 
	 * @param serviceType service acknowledge type identifier describing the acknowledge
	 *        in <code>data</code>, 0 &lt;= type &lt;= 0xFFFF
	 * @param data byte array containing a service acknowledge structure
	 * @param offset start offset of acknowledge in <code>data</code>
	 * @throws KNXFormatException if buffer is too short for acknowledge, on unsupported
	 *         service type or connection header structure
	 */
    public ServiceAck(int serviceType, byte[] data, int offset) throws KNXFormatException {
        super(serviceType);
        if (serviceType < 0 || serviceType > 0xffff) throw new KNXIllegalArgumentException("service ack type out of range [0..0xffff]");
        if (data.length - offset < CONN_HEADER_SIZE) throw new KNXFormatException("buffer too short for service ack");
        int i = offset;
        if ((data[i++] & 0xFF) != CONN_HEADER_SIZE) throw new KNXFormatException("unsupported connection header");
        channelid = (short) (data[i++] & 0xFF);
        seq = (short) (data[i++] & 0xFF);
        status = (short) (data[i++] & 0xFF);
    }

    /**
	 * Creates a new service acknowledge.
	 * <p>
	 * 
	 * @param serviceType service acknowledge type identifier, 0 &lt;= type &lt;= 0xFFFF
	 * @param channelID channel ID of communication this ack belongs to, 0 &lt;= id &lt;=
	 *        255
	 * @param seqNumber the sequence number of the communication channel, value
	 *        corresponds to the received request, 0 &lt;= number &lt;= 255
	 * @param status status code to the corresponding request, 0 &lt;= status &lt;= 255
	 */
    public ServiceAck(int serviceType, int channelID, int seqNumber, int status) {
        super(serviceType);
        if (serviceType < 0 || serviceType > 0xffff) throw new KNXIllegalArgumentException("service ack type out of range [0..0xffff]");
        if (channelID < 0 || channelID > 0xff) throw new KNXIllegalArgumentException("channel ID out of range [0..0xff]");
        if (seqNumber < 0 || seqNumber > 0xff) throw new KNXIllegalArgumentException("sequence number out of range [0..0xff]");
        if (status < 0 || status > 0xff) throw new KNXIllegalArgumentException("status code out of range [0..0xff]");
        channelid = (short) channelID;
        seq = (short) seqNumber;
        this.status = (short) status;
    }

    /**
	 * Returns the service type identifier of the acknowledge.
	 * <p>
	 * 
	 * @return service type as unsigned short
	 */
    public final int getServiceType() {
        return svcType;
    }

    /**
	 * Returns the communication channel identifier associated with the acknowledge.
	 * <p>
	 * 
	 * @return communication channel ID as unsigned byte
	 */
    public final short getChannelID() {
        return channelid;
    }

    /**
	 * Returns the sequence number.
	 * 
	 * @return sequence number as unsigned byte
	 */
    public final short getSequenceNumber() {
        return seq;
    }

    /**
	 * Returns the status code of the acknowledge, regarding the corresponding request.
	 * <p>
	 * 
	 * @return status code as unsigned byte
	 */
    public final short getStatus() {
        return status;
    }

    /**
	 * Returns a textual representation of the status code.
	 * <p>
	 * 
	 * @return short description of status as string
	 */
    public String getStatusString() {
        return ErrorCodes.getErrorMessage(status);
    }

    short getStructLength() {
        return CONN_HEADER_SIZE;
    }

    byte[] toByteArray(ByteArrayOutputStream os) {
        os.write(CONN_HEADER_SIZE);
        os.write(channelid);
        os.write(seq);
        os.write(status);
        return os.toByteArray();
    }
}
