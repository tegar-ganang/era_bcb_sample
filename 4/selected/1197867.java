package tuwien.auto.eicl.struct.eibnetip;

import java.io.*;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;
import tuwien.auto.eicl.struct.eibnetip.util.HPAI;
import tuwien.auto.eicl.util.*;

/**
 * <p>
 * This class encapsulates a representation of a EIBnet/IP Disconnect Request
 * message. This message is sent by the requesting side for closing a
 * established connection, and is answered with a Disconnect Response message.
 * 
 * @see tuwien.auto.eicl.struct.eibnetip.Disconnect_Response
 * @author Bernhard Erb
 */
public class Disconnect_Request {

    private short channelid;

    private short reserved;

    private HPAI endpoint;

    /**
     * This constructor parses a byte array. It is used for incoming requests.
     * Pass all bytes after the EIBnet/IP header to this constructor.
     * 
     * @param _Disconnect_Request
     *            The byte array starting after EIBnet/IP header
     * @throws EICLException
     *             forwards the IOException and HPAI EICLException
     * 
     * @see HPAI
     */
    public Disconnect_Request(byte[] _Disconnect_Request) throws EICLException {
        ByteArrayInputStream bais = new ByteArrayInputStream(_Disconnect_Request);
        channelid = (short) bais.read();
        reserved = (short) bais.read();
        byte[] buffer = new byte[8];
        try {
            bais.read(buffer);
        } catch (IOException ex) {
            throw new EICLException(ex.getMessage());
        }
        endpoint = new HPAI(buffer);
    }

    /**
     * This constructor is used for outgoing requests. It creates a new Request
     * with the given parameters. Use this for outgoing requests.
     * 
     * @param _Channelid
     *            the channelid
     * @param _LocalPort
     *            the local client port
     * @throws EICLException
     *             forwards the EICLException thrown by the HPAI constructor
     * @see HPAI
     */
    public Disconnect_Request(short _Channelid, int _LocalPort) throws EICLException {
        channelid = _Channelid;
        endpoint = new HPAI(_LocalPort);
    }

    /**
     * The byte array representation of this message.
     * 
     * @return The byte array of this message.
     * @throws EICLException
     *             forwards IOException
     */
    public byte[] toByteArray() throws EICLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            EIBnetIPPacket header = new EIBnetIPPacket(EIBNETIP_Constants.DISCONNECT_REQUEST, (EIBNETIP_Constants.HEADER_SIZE_10 + endpoint.getStructLength() + 2));
            baos.write(header.toByteArray());
            baos.write((byte) channelid & 0x00FF);
            baos.write((byte) reserved & 0x00FF);
            baos.write(endpoint.toByteArray());
        } catch (IOException ex) {
            throw new EICLException(ex.getMessage());
        }
        return baos.toByteArray();
    }

    /**
     * Returns the channel ID of the connection being closed.
     * 
     * @return The connection channel ID.
     */
    public short getChannelID() {
        return channelid;
    }

    /**
     * Returns the reserved byte of this message (never used).
     * 
     * @return The reserved byte.
     */
    public short getReserved() {
        return reserved;
    }

    /**
     * Get the client end point.
     * 
     * @return Client end point.
     */
    public HPAI getEndPoint() {
        return endpoint;
    }
}
