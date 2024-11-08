package tuwien.auto.eicl.struct.eibnetip;

import java.io.*;
import tuwien.auto.eicl.util.*;

/**
 * <p>
 * This class is the implementation of the EIBNET/IP Tunnelling Acknowledge
 * message. It can be used for parsing as well as creating new Tunnelling Ack
 * messages. A Tunnelling Ack is sent in answer to a Tunnelling Request, and
 * acknowledges therefore the reception of the message over the IP channel, not
 * the successful transmission on the EIB bus.
 * 
 * @see tuwien.auto.eicl.struct.eibnetip.CEMI_Connection_Request
 * @see tuwien.auto.eicl.CEMI_Connection
 * @author Bernhard Erb
 */
public class CEMI_Connection_Ack {

    private short structlength;

    private short channelid;

    private short sequencecounter;

    private short status;

    private short ackType;

    /**
     * Initializes a new object for sending. All needed values are passed as
     * parameters.
     * 
     * @param _Channelid
     *            The current connection channelid.
     * @param _Sequencecounter
     *            The sending sequence counter.
     * @param _Status
     *            The error status.
     * @param _AckType
     *            The acknowledge message code. (Tunnelling Ack vs. Management
     *            Ack)
     */
    public CEMI_Connection_Ack(short _AckType, short _Channelid, short _Sequencecounter, short _Status) {
        structlength = 4;
        channelid = _Channelid;
        sequencecounter = _Sequencecounter;
        status = _Status;
        ackType = _AckType;
    }

    /**
     * Initializes a new object by parsing a byte array. Pass all bytes after
     * the EIBnet/IP header.
     * 
     * @param _Tunnelling_Ack
     *            The message body byte array (after EIBnet/IP header)
     */
    public CEMI_Connection_Ack(byte[] _Tunnelling_Ack) {
        ByteArrayInputStream bais = new ByteArrayInputStream(_Tunnelling_Ack);
        structlength = (short) bais.read();
        channelid = (short) bais.read();
        sequencecounter = (short) bais.read();
        status = (short) bais.read();
    }

    /**
     * Returns a byte representation of the message. If something goes wrong an
     * Exception is returned.
     * 
     * @return Byte array representation of this message
     * @throws EICLException
     *             Forwards the IOException.
     */
    public byte[] toByteArray() throws EICLException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            EIBnetIPPacket header = new EIBnetIPPacket(ackType, 4 + 4 + 2);
            baos.write(header.toByteArray());
            baos.write((byte) structlength & 0x00FF);
            baos.write((byte) channelid & 0x00FF);
            baos.write((byte) sequencecounter & 0x00FF);
            baos.write((byte) status & 0x00FF);
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new EICLException(ex.getMessage());
        }
    }

    /**
     * Returns the encapsulated connection channel ID.
     * 
     * @return channelid
     */
    public short getChannelid() {
        return channelid;
    }

    /**
     * Returns the message sequence counter.
     * 
     * @return the sequence counter
     */
    public short getSequencecounter() {
        return sequencecounter;
    }

    /**
     * Returns the error status of this message
     * 
     * @return the error status.
     */
    public short getStatus() {
        return status;
    }

    /**
     * Offers a human readable status string.
     * 
     * @return the status string
     */
    public String getStatusString() {
        return Util.getStatusString(status);
    }
}
