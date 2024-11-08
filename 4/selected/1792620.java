package tuwien.auto.eicl.struct.eibnetip;

import java.io.*;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;

/**
 * <p>
 * A EIBnet/IP disconnect response is sent in answer to a disconnect request.
 * This class holds all necessary data, and is able to parse or create a new
 * disconnect response.
 * 
 * @author Bernhard Erb
 * @see tuwien.auto.eicl.struct.eibnetip.Disconnect_Request
 */
public class Disconnect_Response {

    private short channelid;

    private short status;

    /**
     * Use this constructor to create a new disconnect response.
     * 
     * @param _Channelid
     *            The channel ID of the connection being closed
     * @param _Status
     *            The error code, indicating whether the connection was closed
     *            successfully.
     */
    public Disconnect_Response(short _Channelid, short _Status) {
        channelid = _Channelid;
        status = _Status;
    }

    /**
     * Use this constructor to parse a disconnect response from a byte array.
     * The data fields can then be accessed through the get methods. Pass
     * everything after the EIBnet/IP header to this constructor.
     * 
     * @param buffer
     *            the byte array
     */
    public Disconnect_Response(byte[] buffer) {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        channelid = (short) bais.read();
        status = (short) bais.read();
    }

    /**
     * This method returns the channel identifier byte.
     * 
     * @return The channelid of the connection being closed.
     */
    public short getChannelid() {
        return channelid;
    }

    /**
     * Get the message as byte array.
     * 
     * @return The byte array representation.
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) channelid);
        baos.write((byte) status);
        return baos.toByteArray();
    }

    /**
     * This method returns a human readable status message.
     * 
     * @return status string
     */
    public String getStatusString() {
        String ret = "";
        switch(status) {
            case EIBNETIP_Constants.E_NO_ERROR:
                ret = "Disconnect successful";
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

    /**
     * Use this method to get the message status(error code) as byte.
     * 
     * @return status short
     */
    public short getStatus() {
        return status;
    }
}
