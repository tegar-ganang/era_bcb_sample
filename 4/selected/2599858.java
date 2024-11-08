package tuwien.auto.eicl.struct.eibnetip;

import java.io.*;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;

/**
 * <p>
 * This class represents an EIBnet/IP Connectionstate Response. This message is
 * sent in response to an Connectionstate Request. The status byte informs about
 * the request success.
 * 
 * @author Bernhard Erb
 * @see tuwien.auto.eicl.struct.eibnetip.Connectionstate_Request
 * @see tuwien.auto.eicl.CEMI_Connection
 */
public class Connectionstate_Response {

    private short channelid;

    private short status;

    /**
     * Creates a new Connectionstate Response by parsing a byte array. Pass all
     * bytes after the EIBnet/IP header to this constructor.
     * 
     * @param _Buffer
     *            The byte array starting after the EIBnet/IP header.
     */
    public Connectionstate_Response(byte[] _Buffer) {
        ByteArrayInputStream bais = new ByteArrayInputStream(_Buffer);
        channelid = (short) bais.read();
        status = (short) bais.read();
    }

    /**
     * Returns the encapsulated channel identifier
     * 
     * @return The connection channel identifier.
     */
    public short getChannelid() {
        return channelid;
    }

    /**
     * Get the messages errorcode / status.
     * 
     * @return the statusbyte
     */
    public short getStatus() {
        return status;
    }

    /**
     * Get a human readable representation of the statusbyte.
     * 
     * @return The status in a string representation.
     */
    public String getStatusString() {
        String ret = "";
        switch(status) {
            case EIBNETIP_Constants.E_NO_ERROR:
                ret = "Disconnect successfull";
                break;
            case EIBNETIP_Constants.E_CONNECTION_ID:
                ret = "Wrong connection id";
                break;
            default:
                ret = "Disconnect error";
                break;
        }
        return ret;
    }
}
