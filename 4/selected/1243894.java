package tuwien.auto.eicl.struct.eibnetip;

import java.io.*;
import tuwien.auto.eicl.struct.cemi.*;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;
import tuwien.auto.eicl.util.*;

/**
 * <p>
 * This class is the implementation of the EIBnet/IP Tunnelling Request message.
 * This class can be used for parsing and creating messages. A Tunnelling
 * Request is used to tunnel a cEMI message. A Tunnelling Response confirms the
 * correct reception of the Tunnelling Request. Note that this does not ensure
 * packet delivery on the medium.
 * 
 * @see tuwien.auto.eicl.struct.eibnetip.CEMI_Connection_Ack
 * @see tuwien.auto.eicl.struct.cemi.CEMI
 * @author Bernhard Erb
 */
public class CEMI_Connection_Request {

    private short structlength;

    private short channelid;

    private short sequencecounter;

    private short reserved;

    private CEMI cemi;

    private short requestType;

    /**
     * Initializes a new tunnelling request for sending.
     * 
     * @param _Channelid
     *            The current connection channel id.
     * @param _Sequencecounter
     *            The current sending sequence counter.
     * @param _CemiFrame
     *            The message to be processed on the medium.
     * @param _RequestType
     *            The request message code (Tunnelling Request vs. Management
     *            Request)
     */
    public CEMI_Connection_Request(short _RequestType, short _Channelid, short _Sequencecounter, CEMI _CemiFrame) {
        channelid = _Channelid;
        sequencecounter = _Sequencecounter;
        cemi = _CemiFrame;
        reserved = 0;
        structlength = 4;
        requestType = _RequestType;
    }

    /**
     * Initializes a new object by parsing a byte array. Pass all bytes after
     * the EIBnet/IP header to this constructor. If the conversion wasn't
     * successful an Exception is thrown.
     * 
     * @param _Tunneling_Request
     *            The EIBnet/IP message body (after EIBnet/IP header)
     * @param _RequestType
     *            The request message code (Tunnelling Request vs. Management
     *            Request)
     * @throws EICLException
     *             Forwards the cEMI L DATA constructor exception.
     */
    public CEMI_Connection_Request(short _RequestType, byte[] _Tunneling_Request) throws EICLException {
        ByteArrayInputStream bais = new ByteArrayInputStream(_Tunneling_Request);
        structlength = (short) bais.read();
        channelid = (short) bais.read();
        sequencecounter = (short) bais.read();
        reserved = (short) bais.read();
        requestType = _RequestType;
        byte[] help = new byte[128];
        bais.read(help, 0, 128);
        if (requestType == EIBNETIP_Constants.TUNNELLING_REQUEST) cemi = new CEMI_L_DATA(help); else throw new EICLException("Management not yet implemented");
    }

    /**
     * Returns the byte array representation of this message.
     * 
     * @return The message as byte array.
     * @throws EICLException
     *             Forwards the IOException.
     */
    public byte[] toByteArray() throws EICLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            EIBnetIPPacket header = new EIBnetIPPacket(requestType, 4 + cemi.getStructLength() + EIBNETIP_Constants.HEADER_SIZE_10);
            baos.write(header.toByteArray());
            baos.write((byte) structlength);
            baos.write((byte) channelid);
            baos.write((byte) sequencecounter);
            baos.write((byte) reserved);
            baos.write(cemi.toByteArray());
        } catch (IOException ex) {
            throw new EICLException(ex.getMessage());
        }
        return baos.toByteArray();
    }

    /**
     * Returns the encapsulated channel ID
     * 
     * @return The current channel ID.
     */
    public short getChannelid() {
        return channelid;
    }

    /**
     * Returns the sequence number.
     * 
     * @return the sequence number.
     */
    public short getSequenceNumber() {
        return sequencecounter;
    }

    /**
     * Returns the reserved byte of this message.
     * 
     * @return The reserved byte.
     */
    public short getReserved() {
        return reserved;
    }

    /**
     * Returns the message body as cEMI object
     * 
     * @return The cEMI message contained in this request.
     */
    public CEMI getCemi() {
        return cemi;
    }
}
