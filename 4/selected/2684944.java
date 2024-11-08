package javaclient3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javaclient3.structures.PlayerMsgHdr;
import javaclient3.structures.mcom.PlayerMcomConfig;
import javaclient3.structures.mcom.PlayerMcomData;
import javaclient3.xdr.OncRpcException;
import javaclient3.xdr.XdrBufferDecodingStream;
import javaclient3.xdr.XdrBufferEncodingStream;

/**
 * The mcom interface is designed for exchanging information between clients.
 * A client sends a message of a given "type" and "channel". This device
 * stores adds the message to that channel's stack. A second client can then
 * request data of a given "type" and "channel". Push, Pop, Read, and Clear
 * operations are defined, but their semantics can vary, based on the stack
 * discipline of the underlying driver. For example, the lifomcom driver
 * enforces a last-in-first-out stack.
 * @deprecated Removed from Player. Use {@link OpaqueInterface opaque} instead.
 * @author Radu Bogdan Rusu
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class MComInterface extends PlayerDevice {

    private static final boolean isDebugging = PlayerClient.isDebugging;

    private Logger logger = Logger.getLogger(MComInterface.class.getName());

    private PlayerMcomData pmdata;

    private boolean readyPmdata = false;

    /**
     * Constructor for MComInterface.
     * @param pc a reference to the PlayerClient object
     */
    public MComInterface(PlayerClient pc) {
        super(pc);
    }

    /**
     * Read the MCOM data.
     */
    public synchronized void readData(PlayerMsgHdr header) {
        try {
            this.timestamp = header.getTimestamp();
            byte[] buffer = new byte[12];
            is.readFully(buffer, 0, 12);
            pmdata = new PlayerMcomData();
            XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
            xdr.beginDecoding();
            pmdata.setFull((char) xdr.xdrDecodeByte());
            int dataCount = xdr.xdrDecodeInt();
            xdr.endDecoding();
            xdr.close();
            buffer = new byte[MCOM_DATA_LEN];
            is.readFully(buffer, 0, dataCount);
            pmdata.setData_count(dataCount);
            pmdata.setData(new String(buffer).toCharArray());
            if ((dataCount % 4) != 0) is.readFully(buffer, 0, 4 - (dataCount % 4));
            readyPmdata = true;
        } catch (IOException e) {
            throw new PlayerException("[MCom] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[MCOM] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }

    /**
     * Configuration request to the device.
     * <br><br>
     * @param pmconfig a PlayerMcomConfig structure filled with the
     *           required data
     * @param whichReq the appropriate request (PUSH, POP, READ, CLEAR,
     *        SET_CAPACITY)
     */
    public void sendConfigReq(PlayerMcomConfig pmconfig, int whichReq) {
        try {
            int total = 12 + 4 + pmconfig.getChannel_count() + 8 + 4 + pmconfig.getData().getData_count();
            sendHeader(PLAYER_MSGTYPE_REQ, whichReq, total);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(24);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pmconfig.getCommand());
            xdr.xdrEncodeInt(pmconfig.getType());
            xdr.xdrEncodeInt(pmconfig.getChannel_count());
            xdr.xdrEncodeByte((byte) pmconfig.getChannel_count());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (Exception e) {
            String subtype = "";
            switch(whichReq) {
                case PLAYER_MCOM_PUSH:
                    {
                        subtype = "PLAYER_MCOM_PUSH";
                        break;
                    }
                case PLAYER_MCOM_POP:
                    {
                        subtype = "PLAYER_MCOM_POP";
                        break;
                    }
                case PLAYER_MCOM_READ:
                    {
                        subtype = "PLAYER_MCOM_READ";
                        break;
                    }
                case PLAYER_MCOM_CLEAR:
                    {
                        subtype = "PLAYER_MCOM_CLEAR";
                        break;
                    }
                case PLAYER_MCOM_SET_CAPACITY:
                    {
                        subtype = "PLAYER_MCOM_SET_CAPACITY";
                        break;
                    }
                default:
                    {
                        logger.log(Level.FINEST, "[MCom] : Couldn't send " + subtype + " command: " + e.toString());
                    }
            }
        }
    }

    /**
     * Configuration request: Push (PLAYER_MCOM_PUSH_REQ)
     */
    public void Push(int type, String channel, char[] dataT) {
    }

    /**
     * Configuration request: Pop (PLAYER_MCOM_POP_REQ)
     */
    public void Pop(int type, String channel) {
    }

    /**
     * Configuration request: Read (PLAYER_MCOM_READ_REQ)
     */
    public void Read(int type, String channel) {
    }

    /**
     * Configuration request: Clear (PLAYER_MCOM_CLEAR_REQ)
     */
    public void Clear(int type, String channel) {
    }

    /**
     * Configuration request: Set capacity (PLAYER_MCOM_SET_CAPACITY_REQ)
     */
    public void setCapacity(int type, String channel, char capacity) {
        char[] dataT = new char[MCOM_DATA_LEN];
        dataT[0] = capacity;
    }

    /**
     * Handle acknowledgement response messages (threaded mode).
     * @param size size of the payload
     */
    public void handleResponse(int size) {
        if (size == 0) {
            if (isDebugging) System.err.println("[MCom][Debug] : Unexpected response of size 0!");
            return;
        }
        try {
        } catch (Exception e) {
            logger.log(Level.FINEST, "[MCom] : Error when reading payload " + e.toString());
        }
    }

    /**
     * Get the MCom data.
     * @return an object of type PlayerMcomData containing the requested data
     */
    public PlayerMcomData getData() {
        return this.pmdata;
    }

    /**
     * Check if data is available.
     * @return true if ready, false if not ready
     */
    public boolean isDataReady() {
        if (readyPmdata) {
            readyPmdata = false;
            return true;
        }
        return false;
    }
}
