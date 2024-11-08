package tuwien.auto.calimero.knxnetip.servicetype;

import java.io.ByteArrayOutputStream;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.util.CRD;
import tuwien.auto.calimero.knxnetip.util.HPAI;

/**
 * Represents a KNXnet/IP connect response message.
 * <p>
 * The connect response is sent in answer to a connect request to inform the client about
 * the connection status and to supply necessary information.<br>
 * It is addressed to the client's control endpoint.<br>
 * Note, that only on a successful connect response, as indicated by the response status,
 * the communication channel ID, connect response data and server data endpoint carry
 * valid data.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 * @see tuwien.auto.calimero.knxnetip.servicetype.ConnectRequest
 */
public class ConnectResponse extends ServiceType {

    private final short status;

    private short channelid;

    private CRD crd;

    private HPAI endpt;

    /**
	 * Creates a connect response out of a byte array.
	 * <p>
	 * 
	 * @param data byte array containing a connect response structure
	 * @param offset start offset of response in <code>data</code>
	 * @throws KNXFormatException if no connect response was found or invalid structure
	 */
    public ConnectResponse(byte[] data, int offset) throws KNXFormatException {
        super(KNXnetIPHeader.CONNECT_RES);
        if (data.length - offset < 2) throw new KNXFormatException("buffer too short for response");
        int i = offset;
        channelid = (short) (data[i++] & 0xff);
        status = (short) (data[i++] & 0xff);
        if (status == ErrorCodes.NO_ERROR) {
            endpt = new HPAI(data, i);
            crd = CRD.createResponse(data, i + endpt.getStructLength());
        }
    }

    /**
	 * Creates a connect response indicating no success or some error condition.
	 * <p>
	 * 
	 * @param status status code giving information of refusal to the corresponding
	 *        request, 0 &lt;= status &lt;= 255
	 */
    public ConnectResponse(short status) {
        super(KNXnetIPHeader.CONNECT_RES);
        if (status < 0 || status > 0xFF) throw new KNXIllegalArgumentException("status code out of range [0..255]");
        this.status = status;
    }

    /**
	 * Creates a connect response with full customization of response information.
	 * <p>
	 * 
	 * @param channelID communication channel ID to use for the connection, 0 &lt;= id
	 *        &lt;= 255
	 * @param status status code information to the corresponding request, 0 &lt;= status
	 *        &lt;= 255
	 * @param dataEndpoint data endpoint of the server use for communication
	 * @param responseData connection type response data
	 */
    public ConnectResponse(short channelID, short status, HPAI dataEndpoint, CRD responseData) {
        super(KNXnetIPHeader.CONNECT_RES);
        if (channelID < 0 || channelID > 0xFF) throw new KNXIllegalArgumentException("channel ID out of range [0..255]");
        if (status < 0 || status > 0xFF) throw new KNXIllegalArgumentException("status code out of range [0..255]");
        channelid = channelID;
        this.status = status;
        endpt = dataEndpoint;
        crd = responseData;
    }

    /**
	 * Returns the communication channel ID on a successful established connection.
	 * <p>
	 * The ID is not valid otherwise, check the status code of the response.
	 * 
	 * @return communication channel identifier as unsigned byte
	 */
    public final short getChannelID() {
        return channelid;
    }

    /**
	 * Returns the connection response data supplied in the response on a successful
	 * established connection.
	 * <p>
	 * The CRD is <code>null</code> on error, check the status code of the response
	 * before.
	 * 
	 * @return CRD, or <code>null</code>
	 */
    public final CRD getCRD() {
        return crd;
    }

    /**
	 * Returns the server data endpoint used for the communication on a successful
	 * established connection.
	 * <p>
	 * The HPAI is <code>null</code> otherwise, check the status code of the response.
	 * 
	 * @return data endpoint in a HPAI, or <code>null</code>
	 */
    public final HPAI getDataEndpoint() {
        return endpt;
    }

    /**
	 * Returns the status code, indicating success or some error condition.
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
        switch(status) {
            case ErrorCodes.NO_ERROR:
                return "the connection was established successfully";
            case ErrorCodes.CONNECTION_TYPE:
                return "the requested connection type is not supported";
            case ErrorCodes.CONNECTION_OPTION:
                return "one or more connection options are not supported";
            case ErrorCodes.NO_MORE_CONNECTIONS:
                return "could not accept new connection (maximum reached)";
            case ErrorCodes.TUNNELING_LAYER:
                return "the requested tunneling layer is not supported";
            default:
                return "unknown status";
        }
    }

    short getStructLength() {
        int len = 2;
        if (endpt != null && crd != null) len += endpt.getStructLength() + crd.getStructLength();
        return (short) len;
    }

    byte[] toByteArray(ByteArrayOutputStream os) {
        os.write(channelid);
        os.write(status);
        if (endpt != null && crd != null) {
            byte[] buf = endpt.toByteArray();
            os.write(buf, 0, buf.length);
            buf = crd.toByteArray();
            os.write(buf, 0, buf.length);
        }
        return os.toByteArray();
    }
}
