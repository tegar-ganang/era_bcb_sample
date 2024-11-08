package tuwien.auto.eicl.struct.eibnetip;

import tuwien.auto.eicl.struct.eibnetip.util.CRI_CRD;
import tuwien.auto.eicl.struct.eibnetip.util.EIBNETIP_Constants;
import tuwien.auto.eicl.struct.eibnetip.util.HPAI;
import tuwien.auto.eicl.util.*;

/**
 * <p>
 * This class is used to parse a EIBnet/IP Connect Response. A Connect Response
 * is sent in answer to a Connect Request. The status byte informs about the
 * request success.
 * 
 * @see tuwien.auto.eicl.struct.eibnetip.Connect_Request
 * @see tuwien.auto.eicl.CEMI_Connection
 * @author Bernhard Erb
 */
public class Connect_Response {

    short channelid;

    byte status;

    HPAI dataendpoint;

    CRI_CRD crd;

    /**
     * This constructor inits the object by parsing a received byte array
     * message. Pass all bytes after the EIBnet/IP Header to this constructor.
     * 
     * @param _Data
     *            The byte array starting after the EIBnet/IP header.
     * @throws EICLException
     *             If an error occured by parsing the encapsulated fields.
     */
    public Connect_Response(byte[] _Data) throws EICLException {
        channelid = _Data[0];
        status = _Data[1];
        if (status == EIBNETIP_Constants.E_NO_ERROR) {
            byte[] hpai = new byte[8];
            for (int i = 2; i < 10; i++) {
                hpai[i - 2] = _Data[i];
            }
            dataendpoint = new HPAI(hpai);
            hpai = new byte[4];
            for (int i = 10; i < _Data.length; i++) {
                hpai[i - 10] = _Data[i];
            }
            crd = new CRI_CRD(hpai);
        }
    }

    /**
     * Returns a human readable representation of the error codes.
     * 
     * @return The error in a human readable form.
     */
    public String getStatusString() {
        switch(status) {
            case (EIBNETIP_Constants.E_CONNECTION_TYPE):
                return ("Connectiontype not supported");
            case (EIBNETIP_Constants.E_CONNECTION_OPTION):
                return ("Connection option not supported");
            case (EIBNETIP_Constants.E_NO_MORE_CONNECTIONS):
                return ("No more Connections supported");
            case (EIBNETIP_Constants.E_NO_ERROR):
                return ("Everything ok..... go on");
            default:
                return ("unknown status");
        }
    }

    /**
     * Extracts the message status byte. The status byte gives information about
     * the request success.
     * 
     * @return Returns the status byte
     */
    public short getStatus() {
        return status;
    }

    /**
     * Returns the connection channel ID (1 byte) if the connection was
     * established successfully.
     * 
     * @return The connection channel identifier.
     */
    public short getChannelID() {
        return channelid;
    }

    /**
     * Get the Connection request information data block
     * 
     * @return The connection request information data block.
     * @see CRI_CRD
     */
    public CRI_CRD getCRICRD() {
        return crd;
    }

    /**
     * Get the servers data end point.
     * 
     * @return data endpoint
     */
    public HPAI getDataEndPoint() {
        return dataendpoint;
    }
}
