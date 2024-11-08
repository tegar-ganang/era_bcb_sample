package hr.fer.rasip.obex;

import hr.fer.rasip.bluesec.BCC;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.obex.HeaderSet;
import org.javabluetooth.debug.Debug;

/**
 * This class implements <code>javax.obex.HeaderSet</code> interface
 * It is used for getting and setting headers, and for authentication
 * @author mduric
 */
public class OBEXHeaderSet implements HeaderSet {

    int max_id_no;

    int[] h_id = null;

    Vector h_val = null;

    int respCode = 0;

    /**
	 * Default constructor for OBEXHeaderSet
	 * Initializes structures for holding header IDs and header values
	 */
    public OBEXHeaderSet() {
        max_id_no = 15;
        h_id = new int[max_id_no];
        h_val = new Vector();
    }

    /**
	 * method for hex-print of bytes
	 * @param x - input byte
	 * @return String that contains hexadecimal representation of byte <code>x</code>
	 */
    public static String hex(byte x) {
        String[] digit = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
        return "0x" + digit[(x & 0xf0) >> 4] + digit[x & 0x0f];
    }

    /**
	 * Private method for sending headers
	 * 
	 * @param headers - header set to sent
	 * @param out - DataOutputStream connection to the client/server
	 */
    public static void sendHeaders(HeaderSet headers, DataOutputStream out) {
        try {
            int[] hIDs = headers.getHeaderList();
            for (int i = 0; i < hIDs.length; i++) {
                Debug.println(BCC.DEBUG_LEVEL_INFO, "Sending header with header ID " + OBEXHeaderSet.hex((byte) hIDs[i]));
                out.writeByte(hIDs[i]);
                switch(hIDs[i] & 0xc0) {
                    case 0x00:
                        {
                            int len = ((String) headers.getHeader(hIDs[i])).length();
                            byte[] lenb = { (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff) };
                            out.write(lenb);
                            out.writeChars((String) headers.getHeader(hIDs[i]));
                            Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Unicode text sent");
                            break;
                        }
                    case 0x40:
                        int len = ((byte[]) headers.getHeader(hIDs[i])).length;
                        byte[] lenb = { (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff) };
                        out.write(lenb);
                        out.write((byte[]) headers.getHeader(hIDs[i]));
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "byte sequnce sent");
                        break;
                    case 0x80:
                        out.writeByte(((Integer) headers.getHeader(hIDs[i])).intValue());
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "byte sent");
                        break;
                    case 0xc0:
                        out.writeInt(((Integer) headers.getHeader(hIDs[i])).intValue());
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Integer sent");
                        break;
                }
                Debug.println(BCC.DEBUG_LEVEL_INFO, "Header sent");
            }
        } catch (IOException ioe) {
            Debug.println(BCC.DEBUG_LEVEL_ERROR, "Error while sending headers: " + ioe.getMessage());
        }
    }

    /**
	 * Private method for receiving headers
	 * 
	 * @param headers - header set that is received
	 * @param in - DataInputStream connection to the client/server
	 * 
	 * @return number of received bytes
	 */
    public static int receiveHeader(HeaderSet headers, DataInputStream in) {
        int offset = 0;
        try {
            byte hID = in.readByte();
            Debug.println(BCC.DEBUG_LEVEL_INFO, "Received Header ID: " + OBEXHeaderSet.hex(hID));
            switch(hID & 0xc0) {
                case 0x00:
                    {
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Received Unicode text");
                        int strLen = ((int) in.readByte()) << 8;
                        strLen |= (int) in.readByte();
                        char[] recC = new char[strLen];
                        for (int i = 0; i < strLen; i++) {
                            recC[i] = in.readChar();
                        }
                        headers.setHeader(hID, new String(recC));
                        offset = 3 + strLen;
                        break;
                    }
                case 0x40:
                    {
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Received byte sequence");
                        int bLen = ((int) in.readByte()) << 8;
                        bLen |= (int) in.readByte();
                        byte[] recB = new byte[bLen];
                        for (int i = 0; i < bLen; i++) {
                            recB[i] = (byte) in.readByte();
                        }
                        headers.setHeader(hID, recB);
                        offset = 3 + bLen;
                        break;
                    }
                case 0x80:
                    {
                        Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Received byte");
                        byte[] recB = { (byte) in.readByte() };
                        headers.setHeader(hID, recB);
                        offset = 2;
                        break;
                    }
                case 0xc0:
                    Debug.println(BCC.DEBUG_LEVEL_VERBOSE, "Received Integer");
                    Integer recB = new Integer(in.readInt());
                    headers.setHeader(hID, recB);
                    offset = 5;
                    break;
            }
        } catch (IOException ioe) {
            Debug.println(BCC.DEBUG_LEVEL_ERROR, "Error while receiving headers:" + ioe.getMessage());
            return 100;
        }
        return offset;
    }

    /**
	 * Method for getting total number of bytes used by header sets
	 * @return number of bytes required by this header set
	 * @throws IOException - in case of unrecognized header type
	 */
    public int getHeaderLength() throws IOException {
        int bNo = 0;
        int[] hIDs = getHeaderList();
        bNo = hIDs.length;
        for (int i = 0; i < hIDs.length; i++) {
            switch(h_id[i] & 0xc0) {
                case 0x00:
                    bNo += 2 + ((String) h_val.elementAt(i)).length();
                    break;
                case 0x40:
                    bNo += 2 + ((byte[]) h_val.elementAt(i)).length;
                    break;
                case 0x80:
                    bNo++;
                    break;
                case 0xc0:
                    bNo += 4;
                    break;
                default:
                    throw new IOException("Unrecognized header ID type");
            }
        }
        return bNo;
    }

    /**
	 * Method for getting number of set header sets
	 */
    public int getHeadersNo() {
        return h_val.size();
    }

    /**
	 * Sets the authentication challenge header. The <code>realm</code> will be 
	 * encoded based upon the default encoding scheme used by the implementation to 
	 * encode strings. Therefore, the encoding scheme used to encode the 
	 * <code>realm</code> is application dependent.
	 * @param realm - a short description that describes what password to use; if 
	 * <code>null</code> no realm will be sent in the authentication challenge header
	 * @param userID - if <code>true</code>, a user ID is required in the reply; 
	 * if <code>false</code>, no user ID is required
	 * @param access - if <code>true</code> then full access will be granted if 
	 * successful; if <code>false</code> then read-only access will be granted 
	 * if successful
	 */
    public void createAuthenticationChallenge(String realm, boolean userID, boolean access) {
        Debug.println(BCC.DEBUG_LEVEL_ERROR, "method not implemented");
    }

    /**
	 * Retrieves the value of the header identifier provided. 
	 * @param headerID - the header identifier whose value is to be returned
	 * @return the value of the header provided or <code>null</code> if the header 
	 * identifier specified is not part of this <code>HeaderSet</code> object
	 * @throws IOException - if an error occurred in the transport layer during the 
	 * operation or if the connection has been closed
	 * @throws IllegalArgumentException - if the <code>headerID</code> is not one 
	 * defined in this interface or any of the user-defined headers
	 */
    public Object getHeader(int headerID) throws IOException {
        int i;
        for (i = 0; h_id[i] != headerID && i < h_val.size(); i++) ;
        if (i == h_val.size()) {
            throw new IllegalArgumentException("Header ID " + headerID + " is not defined");
        } else if (i > h_val.size()) {
            return null;
        } else {
            return h_val.elementAt(i);
        }
    }

    /**
	 * Retrieves the list of headers that may be retrieved via the <code>getHeader</code>
	 * method that will not return <code>null</code>. In other words, this method 
	 * returns all the headers that are available in this object.
	 * @return the array of headers that are set in this object or <code>null</code> 
	 * if no headers are available
	 * @throws IOException - if an error occurred in the transport layer during the 
	 * operation or the connection has been closed
	 */
    public int[] getHeaderList() throws IOException {
        int[] headerList = new int[h_val.size()];
        for (int i = 0; i < h_val.size(); i++) {
            headerList[i] = h_id[i];
        }
        return headerList;
    }

    /**
	 * Returns the response code received from the server. Response codes are 
	 * defined in the <code>ResponseCodes</code> class.
	 * @return the response code retrieved from the server
	 * @throws IOException - if an error occurred in the transport layer during the 
	 * transaction; if this method is called on a <code>HeaderSet</code> object 
	 * created by calling <code>createHeaderSet()</code> in a <code>ClientSession</code>
	 * object; if an OBEX server created this object 
	 */
    public int getResponseCode() throws IOException {
        return respCode;
    }

    /**
	 * Sets the value of the header identifier to the value provided. The type of 
	 * object must correspond to the Java type defined in the description of this 
	 * interface. If <code>null</code> is passed as the <code>headerValue</code>
	 * then the header will be removed from the set of headers to include in the 
	 * next request.
	 * @param headerID - the identifier to include in the message
	 * @param headerValue - the value of the header identifier
	 * @throws IllegalArgumentException - if the header identifier provided is not 
	 * one defined in this interface or a user-defined header; if the type of 
	 * <code>headerValue</code> is not the correct Java type as defined in the 
	 * description of this interface
	 */
    public void setHeader(int headerID, Object headerValue) {
        int i;
        for (i = 0; h_id[i] != headerID && i < h_val.size(); i++) ;
        if (h_val.size() == max_id_no) {
            Debug.println(BCC.DEBUG_LEVEL_WARN, "Max number of header sets reached - change " + "the value of OBEXHeaderSet.max_id_no if needed");
        } else {
            if (headerValue == null) {
                for (int j = i; j < (h_val.size() - 1); j++) {
                    h_id[j] = h_id[j + 1];
                }
                if (i < h_val.size()) {
                    h_val.remove(i);
                    Debug.println(BCC.DEBUG_LEVEL_INFO, "Removed header with " + "header ID " + OBEXHeaderSet.hex((byte) headerID));
                } else {
                    Debug.println(BCC.DEBUG_LEVEL_WARN, "Header with header ID " + OBEXHeaderSet.hex((byte) headerID) + " does not exist in this header set - nothing to remove");
                }
            } else {
                if (i == h_val.size()) h_id[i] = headerID;
                if ((headerID & 0xc0) != 0x00 && headerValue instanceof String) {
                    int pos = 0;
                    int hvLen = ((String) headerValue).length();
                    byte[] typeHV = new byte[hvLen];
                    for (pos = 0; pos < hvLen; pos++) {
                        typeHV[pos] = (byte) (((String) headerValue).charAt(pos) & 0xff);
                    }
                    h_val.add(typeHV);
                } else {
                    h_val.add(headerValue);
                }
            }
        }
    }
}
