package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Represents a KNXnet/IP disconnect response.
 * <p>
 * Such response is sent in reply to a disconnect request to complete the termination of
 * the logical connection between a client and server. It provides the final status after
 * the closing sequence, indicating success or failure.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author Bernhard Erb
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.DisconnectRequest
 */
public class DisconnectResponse extends ServiceType {

    private final short channelid;

    private final short status;

    /**
	 * Creates a disconnect response out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a disconnect response structure
	 * @param offset start offset of response in <code>data</code>
	 * @throws KNXFormatException if found structure is too short
	 */
    public DisconnectResponse(byte[] data, int offset) throws KNXFormatException {
        super(KNXnetIPHeader.DISCONNECT_RES);
        if (data.length - offset < 2) throw new KNXFormatException("buffer too short for disconnect response");
        channelid = (short) (data[offset] & 0xFF);
        status = (short) (data[offset + 1] & 0xFF);
    }

    /**
	 * Creates a new disconnect response for the terminating connection with the given
	 * channel ID.
	 * <p>
	 * 
	 * @param channelID communication channel ID passed in the disconnect request, 0 &lt;=
	 *        id &lt;= 255
	 * @param status status code giving information of the final state, 0 &lt;= status
	 *        &lt;= 255
	 */
    public DisconnectResponse(short channelID, short status) {
        super(KNXnetIPHeader.DISCONNECT_RES);
        if (channelID < 0 || channelID > 0xFF) throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
        if (status < 0 || status > 0xFF) throw new KNXIllegalArgumentException("status code out of range [0..255]");
        channelid = channelID;
        this.status = status;
    }

    /**
	 * Returns the communication channel identifier, matching the ID in the corresponding
	 * disconnect request for closing the connection.
	 * <p>
	 * 
	 * @return communication channel ID as unsigned byte
	 */
    public final short getChannelID() {
        return channelid;
    }

    /**
	 * Returns the status code, signaling the final state.
	 * <p>
	 * 
	 * @return status code as unsigned byte
	 */
    public final short getStatus() {
        return status;
    }

    short getStructLength() {
        return 2;
    }

    byte[] toByteArray(ByteArrayOutputStream os) {
        os.write(channelid);
        os.write(status);
        return os.toByteArray();
    }
}
