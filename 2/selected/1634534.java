package org.marre.sms.transport.clickatell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.marre.sms.SmsAddress;
import org.marre.sms.SmsConcatMessage;
import org.marre.sms.SmsConstants;
import org.marre.sms.SmsDcs;
import org.marre.sms.SmsException;
import org.marre.sms.SmsMessage;
import org.marre.sms.SmsPdu;
import org.marre.sms.SmsPduUtil;
import org.marre.sms.SmsUdhElement;
import org.marre.sms.SmsUdhUtil;
import org.marre.sms.SmsUserData;
import org.marre.sms.transport.SmsTransport;
import org.marre.util.StringUtil;

/**
 * An SmsTransport that sends the SMS with clickatell over HTTP.
 * <p>
 * It is developed to use the "Clickatell HTTP API v. 2.2.4".
 * <p>
 * 
 * Known limitations:<br>
 * - Cannot send 8-Bit messages without an UDH.<br>
 * - DCS is not supported. Only UCS2, 7bit, 8bit and SMS class 0 or 1.<br>
 * - Cannot set validity period (not implemented)<br>
 * - Doesn't acknowledge the TON or NPI, everything is sent as NPI_ISDN_TELEPHONE and TON_INTERNATIONAL.<br>
 * 
 * @author Markus Eriksson
 * @version $Id: ClickatellTransport.java 410 2006-03-13 19:48:31Z c95men $
 */
public class ClickatellTransport implements SmsTransport {

    private static Logger log_ = LoggerFactory.getLogger(ClickatellTransport.class);

    private String username_;

    private String password_;

    private String apiId_;

    private String sessionId_;

    private String protocol_;

    /** Required feature "Text". Set by default. */
    public static final int FEAT_TEXT = 0x0001;

    /** Required feature "8-bit messaging". Set by default. */
    public static final int FEAT_8BIT = 0x0002;

    /** Required feature "udh (binary)". Set by default. */
    public static final int FEAT_UDH = 0x0004;

    /** Required feature "ucs2/unicode". Set by default. */
    public static final int FEAT_UCS2 = 0x0008;

    /** Required feature "alpha originator (sender id)". */
    public static final int FEAT_ALPHA = 0x0010;

    /** Required feature "numeric originator (sender id)". */
    public static final int FEAT_NUMBER = 0x0020;

    /** Required feature "reply to an mt message with a numeric sender id". */
    public static final int FEAT_REPLY = 0x0040;

    /** Required feature "Flash messaging". */
    public static final int FEAT_FLASH = 0x0200;

    /** Required feature "Delivery acknowledgements". */
    public static final int FEAT_DELIVACK = 0x2000;

    /** Required feature "Concatenation". Set by default. */
    public static final int FEAT_CONCAT = 0x4000;

    /** The default required features as explained in HTTP API v224. */
    public static final int FEAT_DEFAULT = 0x400F;

    /**
     * Sends a request to clickatell.
     * 
     * @param url the url to clickatell
     * @param requestString parameters to send
     * @return An array of responses (sessionid or msgid)
     * @throws ClickatellException
     * @throws IOException
     */
    private String[] sendRequest(String url, String requestString) throws ClickatellException, IOException {
        String response = null;
        MessageFormat responseFormat = new MessageFormat("{0}: {1}");
        List idList = new LinkedList();
        try {
            log_.debug("sendRequest: posting : " + requestString + " to " + url);
            URL requestURL = new URL(url);
            URLConnection urlConn = requestURL.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            PrintWriter pw = new PrintWriter(urlConn.getOutputStream());
            pw.print(requestString);
            pw.flush();
            pw.close();
            InputStream is = urlConn.getInputStream();
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(is));
            while ((response = responseReader.readLine()) != null) {
                Object[] objs = responseFormat.parse(response);
                if ("ERR".equalsIgnoreCase((String) objs[0])) {
                    MessageFormat errorFormat = new MessageFormat("{0}: {1}, {2}");
                    Object[] errObjs = errorFormat.parse(response);
                    String errorNo = (String) errObjs[1];
                    String description = (String) errObjs[2];
                    throw new ClickatellException("Clickatell error. Error " + errorNo + ", " + description, Integer.parseInt(errorNo));
                }
                log_.debug("sendRequest: Got ID : " + ((String) objs[1]));
                idList.add(objs[1]);
            }
            responseReader.close();
        } catch (ParseException ex) {
            throw new ClickatellException("Unexpected response from Clickatell. : " + response, ClickatellException.ERROR_UNKNOWN);
        }
        return (String[]) idList.toArray(new String[idList.size()]);
    }

    private String[] sendRequestWithRetry(String url, String requestString) throws SmsException, IOException {
        String[] msgIds;
        try {
            msgIds = sendRequest(url, requestString);
        } catch (ClickatellException ex) {
            switch(ex.getErrId()) {
                case ClickatellException.ERROR_AUTH_FAILED:
                case ClickatellException.ERROR_SESSION_ID_EXPIRED:
                    connect();
                    try {
                        msgIds = sendRequest(url, requestString);
                    } catch (ClickatellException ex2) {
                        throw new SmsException(ex2.getMessage());
                    }
                    break;
                case ClickatellException.ERROR_UNKNOWN:
                default:
                    throw new SmsException(ex.getMessage());
            }
        }
        return msgIds;
    }

    /**
     * Initializes the transport.
     * <p>
     * It expects the following properties in properties param:
     * 
     * <pre>
     *       smsj.clickatell.username - clickatell username
     *       smsj.clickatell.password - clickatell password
     *       smsj.clickatell.apiid    - clickatell apiid
     *       smsj.clickatell.protocol - http or https
     * </pre>
     * 
     * @param properties
     *            Properties to initialize the library
     * @throws SmsException
     *             If not given the needed params
     */
    public void init(Properties properties) throws SmsException {
        username_ = properties.getProperty("smsj.clickatell.username");
        password_ = properties.getProperty("smsj.clickatell.password");
        apiId_ = properties.getProperty("smsj.clickatell.apiid");
        protocol_ = properties.getProperty("smsj.clickatell.protocol", "http");
        if ((username_ == null) || (password_ == null) || (apiId_ == null)) {
            throw new SmsException("Incomplete login information for clickatell");
        }
        if (!(protocol_.equals("http") || protocol_.equals("https"))) {
            throw new SmsException("Unsupported protocol : " + protocol_);
        }
    }

    /**
     * Sends an auth command to clickatell to get an session id that can be used
     * later.
     * @throws SmsException
     *             If we fail to authenticate to clickatell or if we fail to
     *             connect.
     * @throws IOException 
     */
    public void connect() throws SmsException, IOException {
        String[] response = null;
        String url = protocol_ + "://api.clickatell.com/http/auth";
        String requestString;
        requestString = "api_id=" + apiId_;
        requestString += "&user=" + username_;
        requestString += "&password=" + password_;
        try {
            response = sendRequest(url, requestString);
        } catch (ClickatellException ex) {
            throw new SmsException(ex);
        }
        sessionId_ = response[0];
    }

    /**
     * 
     */
    private String buildSendRequest(SmsUserData ud, byte[] udhData, SmsAddress dest, SmsAddress sender) throws SmsException {
        String requestString;
        int reqFeat = 0;
        requestString = "session_id=" + sessionId_;
        requestString += "&to=" + dest.getAddress();
        if (SmsUdhUtil.isConcat(ud, udhData)) {
            requestString += "&concat=3";
            reqFeat |= FEAT_CONCAT;
        }
        if (sender != null) {
            requestString += "&from=" + sender.getAddress();
            reqFeat |= (sender.getTypeOfNumber() == SmsConstants.TON_ALPHANUMERIC) ? FEAT_ALPHA : FEAT_NUMBER;
        }
        if (ud.getDcs().getMessageClass() == SmsDcs.MSG_CLASS_0) {
            requestString += "&msg_type=SMS_FLASH";
            reqFeat |= FEAT_FLASH;
        }
        if ((udhData == null) || (udhData.length == 0)) {
            switch(ud.getDcs().getAlphabet()) {
                case SmsDcs.ALPHABET_8BIT:
                    throw new SmsException("Clickatell API cannot send 8 bit encoded messages without UDH");
                case SmsDcs.ALPHABET_UCS2:
                    String udStr = StringUtil.bytesToHexString(ud.getData());
                    requestString += "&unicode=1";
                    requestString += "&text=" + udStr;
                    reqFeat |= FEAT_UCS2;
                    break;
                case SmsDcs.ALPHABET_GSM:
                    String msg = SmsPduUtil.readSeptets(ud.getData(), ud.getLength());
                    try {
                        requestString += "&text=" + URLEncoder.encode(msg, "ISO-8859-1");
                    } catch (UnsupportedEncodingException e) {
                        throw new SmsException("Failed to encode text to ISO-8859-1");
                    }
                    reqFeat |= FEAT_TEXT;
                    break;
                default:
                    throw new SmsException("Unsupported data coding scheme");
            }
        } else {
            String udStr;
            String udhStr;
            switch(ud.getDcs().getAlphabet()) {
                case SmsDcs.ALPHABET_8BIT:
                    udStr = StringUtil.bytesToHexString(ud.getData());
                    udhStr = StringUtil.bytesToHexString(udhData);
                    requestString += "&udh=" + udhStr;
                    requestString += "&text=" + udStr;
                    reqFeat |= FEAT_UDH | FEAT_8BIT;
                    break;
                case SmsDcs.ALPHABET_UCS2:
                    udStr = StringUtil.bytesToHexString(ud.getData());
                    udhStr = StringUtil.bytesToHexString(udhData);
                    requestString += "&unicode=1";
                    requestString += "&udh=" + udhStr;
                    requestString += "&text=" + udStr;
                    reqFeat |= FEAT_UDH | FEAT_UCS2;
                    break;
                case SmsDcs.ALPHABET_GSM:
                    throw new SmsException("Clickatell API cannot send 7 bit encoded messages with UDH");
                default:
                    throw new SmsException("Unsupported data coding scheme");
            }
        }
        requestString += "&req_feat=" + reqFeat;
        return requestString;
    }

    /**
     * More effective sending of SMS.
     * 
     * @param msg
     * @param receiver
     * @param sender
     * @throws SmsException
     */
    private String[] sendConcatMessage(SmsConcatMessage msg, SmsAddress receiver, SmsAddress sender) throws SmsException, IOException {
        String url = protocol_ + "://api.clickatell.com/http/sendmsg";
        SmsUserData userData = msg.getUserData();
        SmsUdhElement[] udhElements = msg.getUdhElements();
        byte[] udhData = SmsUdhUtil.toByteArray(udhElements);
        String requestString = buildSendRequest(userData, udhData, receiver, sender);
        return sendRequestWithRetry(url, requestString);
    }

    /**
     * Sends an sendmsg command to clickatell.
     * 
     * @param pdu
     * @param receiver
     * @param sender
     * @throws SmsException
     *             If clickatell sends an error message, unexpected response or
     *             if we fail to connect.
     */
    private String send(SmsPdu pdu, SmsAddress receiver, SmsAddress sender) throws SmsException, IOException {
        String url = protocol_ + "://api.clickatell.com/http/sendmsg";
        SmsUserData userData = pdu.getUserData();
        byte[] udhData = pdu.getUserDataHeaders();
        String requestString = buildSendRequest(userData, udhData, receiver, sender);
        return sendRequestWithRetry(url, requestString)[0];
    }

    /**
     * Sends an SMS Message.
     * 
     * @param msg
     * @param receiver
     * @param sender
     * @throws SmsException
     * @return Message ids
     */
    public String send(SmsMessage msg, SmsAddress receiver, SmsAddress sender) throws SmsException, IOException {
        String[] msgIds;
        if (receiver.getTypeOfNumber() == SmsConstants.TON_ALPHANUMERIC) {
            throw new SmsException("Cannot sent SMS to an ALPHANUMERIC address");
        }
        if (sessionId_ == null) {
            throw new SmsException("Must connect before sending");
        }
        if (msg instanceof SmsConcatMessage) {
            msgIds = sendConcatMessage((SmsConcatMessage) msg, receiver, sender);
        } else {
            SmsPdu[] msgPdu = msg.getPdus();
            msgIds = new String[msgPdu.length];
            for (int i = 0; i < msgPdu.length; i++) {
                msgIds[i] = send(msgPdu[i], receiver, sender);
            }
        }
        return null;
    }

    /**
     * Disconnect from clickatell.
     * 
     * Not needed for the clickatell API
     * 
     * @throws SmsException Never
     * @throws IOException Never
     */
    public void disconnect() {
    }

    /**
     * Pings the clickatell service
     * 
     * Not needed for the clickatell API
     * 
     * @throws SmsException Never
     * @throws IOException Never
     */
    public void ping() throws SmsException, IOException {
        String[] response = null;
        String url = protocol_ + "://api.clickatell.com/http/ping";
        String requestString;
        requestString = "session_id=" + sessionId_;
        try {
            response = sendRequest(url, requestString);
        } catch (ClickatellException ex) {
            throw new SmsException(ex);
        }
    }
}
