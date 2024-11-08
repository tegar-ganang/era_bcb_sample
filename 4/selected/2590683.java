package vavi.net.im.protocol.oscar.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import vavi.net.im.protocol.Connection;
import vavi.net.im.protocol.oscar.flap.FlapConstants;
import vavi.net.im.protocol.oscar.flap.FlapHeader;
import vavi.net.im.protocol.oscar.snac.SNACPacket;
import vavi.net.im.protocol.oscar.util.TLV;

/**
 * @author mike
 */
public abstract class Command {

    /** */
    protected FlapHeader flapHdr;

    /** */
    protected SNACPacket snacPacket;

    /** */
    protected List<TLV> tlvList;

    /** */
    protected byte[] data;

    /**
     * Default constructor
     */
    public Command() {
    }

    /**
     * Constructor
     * @param hdr FlapHeader
     * @param snac SNACPacket
     * @param tlvs list of TLVs
     * @param dataArray command data, not part of the above params
     */
    public Command(FlapHeader hdr, SNACPacket snac, List<TLV> tlvs, byte[] dataArray) {
        flapHdr = hdr;
        snacPacket = snac;
        tlvList = tlvs;
        data = dataArray;
    }

    /**
     * Writes the command data to the output stream.  This method must be implemented
     * by all class extending this class.  Each command class will know what data it
     * has and wants to write/send to AIM.
     * @param os Output stream
     * @throws IOException
     */
    public abstract void writeCommandData(OutputStream os) throws IOException;

    /**
     * method to get the byte array representing the command
     */
    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (data == null) {
            writeCommandData(baos);
            if ((tlvList != null) && !tlvList.isEmpty()) {
                for (TLV tlv : tlvList) {
                    baos.write(tlv.getBytes());
                }
            }
            data = baos.toByteArray();
        }
        ByteArrayOutputStream cmdOS = new ByteArrayOutputStream();
        int cmdDataLen = 0;
        if (snacPacket != null) {
            cmdDataLen += SNACPacket.SNACPACKET_LENGTH;
        }
        if (data != null) {
            cmdDataLen += data.length;
        }
        flapHdr.setDataLength(cmdDataLen);
        cmdOS.write(flapHdr.getBytes());
        if (snacPacket != null) {
            cmdOS.write(snacPacket.getBytes());
        }
        cmdOS.write(data, 0, data.length);
        byte[] cmdBytes = cmdOS.toByteArray();
        return cmdBytes;
    }

    /**
     * Return string representation of the Command object
     * @return string representation of the Command object
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append(flapHdr).append("\n");
        if (snacPacket != null) {
            sb.append(snacPacket).append("\n");
        }
        if ((tlvList != null) && !tlvList.isEmpty()) {
            for (TLV tlv : tlvList) {
                sb.append(tlv.toString());
            }
        }
        return sb.toString();
    }

    /**
     * Gets the next incoming command from the AOL server, thru the connection
     * @param conn Connection
     * @return Command from the AOL server
     * @throws IOException
     * @throws InterruptedException
     */
    public static Command getCommand(Connection conn) throws IOException, InterruptedException {
        byte[] cmdData = null;
        FlapHeader hdr = null;
        SNACPacket pkt = null;
        List<TLV> tlvs = null;
        try {
            hdr = FlapHeader.getHeader(conn);
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        }
        if (hdr != null) {
            int dataLength = hdr.getDataLength();
            cmdData = new byte[dataLength];
            int count = 0;
            while (count != cmdData.length) {
                try {
                    count += conn.read(cmdData, count, cmdData.length - count);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (hdr.getChannelId() == FlapConstants.FLAP_CHANNEL_SNAC) {
                pkt = new SNACPacket(cmdData);
                cmdData = null;
            } else if (hdr.getChannelId() == FlapConstants.FLAP_CHANNEL_DISCONNECT) {
                tlvs = TLV.getTLVs(cmdData, 0);
            }
        }
        Command cmd = null;
        if (hdr != null) {
            cmd = new BaseCommand(hdr, pkt, tlvs, cmdData);
        }
        return cmd;
    }

    protected void addTLV(TLV tlv) {
        if (tlvList == null) {
            tlvList = new LinkedList<TLV>();
        }
        tlvList.add(tlv);
    }

    protected byte[] getTLVBytes() {
        byte[] tlvBytes = null;
        if ((tlvList != null) && !tlvList.isEmpty()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                for (TLV tlv : tlvList) {
                    baos.write(tlv.getBytes());
                }
            } catch (IOException e) {
            }
            tlvBytes = baos.toByteArray();
        }
        return tlvBytes;
    }

    /**
     * Get the TLV for the type requested from the Command
     * @param tlvid TLV type id
     * @return TLV object or null if matching TLV type not found
     */
    public TLV getTLV(int tlvid) {
        TLV tlv = null;
        if ((tlvList != null) && !tlvList.isEmpty()) {
            for (TLV element : tlvList) {
                if (tlvid == element.getType()) {
                    tlv = element;
                    break;
                }
            }
        }
        return tlv;
    }

    /**
     * Get the FlapHeader for this command
     * @return FlapHeader for this command
     */
    public FlapHeader getFlapHdr() {
        return flapHdr;
    }

    /**
     * Get the SNACPacket from this command
     * @return SNACPacket for this command
     */
    public SNACPacket getSNAC() {
        return snacPacket;
    }

    /**
     * Get the channel id for the Command
     * @return channel id from the FlapHeader
     */
    public byte getChannel() {
        return flapHdr.getChannelId();
    }

    /**
     * Get the Family Id for the Command
     * @return family id from the SNACPacket, or -1 if no SNACPacket available
     */
    public int getFamily() {
        if (snacPacket != null) {
            return snacPacket.getFamilyId();
        } else {
            return -1;
        }
    }

    /**
     * Get the Sub type from the Command
     * @return sub type id from the SNACPacket, of -1 if no SNACPacket available
     */
    public int getSubType() {
        if (snacPacket != null) {
            return snacPacket.getSubTypeId();
        } else {
            return -1;
        }
    }

    /** */
    public byte[] getData() {
        return data;
    }

    /** */
    public byte[] getSNACData() {
        if (snacPacket == null) {
            return null;
        } else {
            return snacPacket.getSNACData();
        }
    }
}
