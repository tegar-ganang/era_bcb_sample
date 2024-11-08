package org.marre.sms.transport.pswincom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Properties;
import org.marre.sms.SmsAddress;
import org.marre.sms.SmsConstants;
import org.marre.sms.SmsDcs;
import org.marre.sms.SmsException;
import org.marre.sms.SmsMessage;
import org.marre.sms.SmsPdu;
import org.marre.sms.SmsPduUtil;
import org.marre.sms.SmsTextMessage;
import org.marre.sms.SmsUserData;
import org.marre.sms.transport.SmsTransport;
import org.marre.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple transport for the pswin xml protocol.
 * 
 * See http://www.pswin.com/ for more information.
 * 
 * <pre>
 * Available properties:
 * smsj.pswincom.username
 * smsj.pswincom.password
 * smsj.pswincom.server - server address (default is "sms.pswin.com")
 * smsj.pswincom.port - port (default is "1111")
 * </pre>
 *  
 * @author Markus
 * @version $Id: PsWinXmlTransport.java 410 2006-03-13 19:48:31Z c95men $
 */
public class PsWinXmlTransport implements SmsTransport {

    private static Logger log_ = LoggerFactory.getLogger(PsWinXmlTransport.class);

    private PsWinXmlResponseParser responseParser_;

    private String username_;

    private String password_;

    private String server_;

    private int port_;

    /**
     * Initializes the pswin transport.
     * 
     * @see org.marre.sms.transport.SmsTransport#init(java.util.Properties)
     */
    public void init(Properties props) throws SmsException {
        username_ = props.getProperty("smsj.pswincom.username");
        password_ = props.getProperty("smsj.pswincom.password");
        server_ = props.getProperty("smsj.pswincom.server", "sms.pswin.com");
        port_ = Integer.parseInt(props.getProperty("smsj.pswincom.port", "1111"));
        log_.debug("init() : username = " + username_);
        log_.debug("init() : password = " + password_);
        log_.debug("init() : server = " + server_);
        log_.debug("init() : port = " + port_);
        if ((username_ == null) || (password_ == null)) {
            throw new SmsException("Incomplete login information for pswincom");
        }
        responseParser_ = new PsWinXmlResponseParser();
    }

    private void addMsg(StringWriter xmlStringWriter, SmsPdu smsPdu, SmsAddress dest, SmsAddress sender) throws SmsException {
        SmsUserData userData = smsPdu.getUserData();
        xmlStringWriter.write("<MSG>\r\n");
        xmlStringWriter.write("<RCPREQ>Y</RCPREQ>\r\n");
        switch(smsPdu.getDcs().getAlphabet()) {
            case SmsDcs.ALPHABET_UCS2:
                xmlStringWriter.write("<OP>9</OP>\r\n");
                xmlStringWriter.write("<TEXT>");
                xmlStringWriter.write(StringUtil.bytesToHexString(userData.getData()));
                xmlStringWriter.write("</TEXT>\r\n");
                break;
            case SmsDcs.ALPHABET_GSM:
                xmlStringWriter.write("<TEXT>");
                xmlStringWriter.write(SmsPduUtil.readSeptets(userData.getData(), userData.getLength()));
                xmlStringWriter.write("</TEXT>\r\n");
                break;
            case SmsDcs.ALPHABET_8BIT:
                xmlStringWriter.write("<OP>8</OP>\r\n");
                xmlStringWriter.write("<TEXT>");
                xmlStringWriter.write(StringUtil.bytesToHexString(smsPdu.getUserDataHeaders()) + StringUtil.bytesToHexString(userData.getData()));
                xmlStringWriter.write("</TEXT>\r\n");
                break;
            default:
                throw new SmsException("Unsupported alphabet");
        }
        xmlStringWriter.write("<RCV>");
        xmlStringWriter.write(dest.getAddress());
        xmlStringWriter.write("</RCV>\r\n");
        if (sender != null) {
            xmlStringWriter.write("<SND>");
            xmlStringWriter.write(sender.getAddress());
            xmlStringWriter.write("</SND>\r\n");
        }
        if (smsPdu.getDcs().getMessageClass() == SmsDcs.MSG_CLASS_0) {
            xmlStringWriter.write("<CLASS>");
            xmlStringWriter.write("0");
            xmlStringWriter.write("</CLASS>\r\n");
        }
        xmlStringWriter.write("</MSG>\r\n");
    }

    private void addTextMsg(StringWriter xmlStringWriter, SmsTextMessage msg, SmsAddress dest, SmsAddress sender) throws SmsException {
        SmsUserData userData = msg.getUserData();
        xmlStringWriter.write("<MSG>\r\n");
        switch(userData.getDcs().getAlphabet()) {
            case SmsDcs.ALPHABET_UCS2:
                xmlStringWriter.write("<OP>9</OP>\r\n");
                xmlStringWriter.write("<TEXT>");
                xmlStringWriter.write(StringUtil.bytesToHexString(userData.getData()));
                xmlStringWriter.write("</TEXT>\r\n");
                break;
            case SmsDcs.ALPHABET_GSM:
                xmlStringWriter.write("<TEXT>");
                xmlStringWriter.write(msg.getText());
                xmlStringWriter.write("</TEXT>\r\n");
                break;
            default:
                throw new SmsException("Unsupported alphabet");
        }
        xmlStringWriter.write("<RCV>");
        xmlStringWriter.write(dest.getAddress());
        xmlStringWriter.write("</RCV>\r\n");
        if (sender != null) {
            xmlStringWriter.write("<SND>");
            xmlStringWriter.write(sender.getAddress());
            xmlStringWriter.write("</SND>\r\n");
        }
        if (userData.getDcs().getMessageClass() == SmsDcs.MSG_CLASS_0) {
            xmlStringWriter.write("<CLASS>");
            xmlStringWriter.write("0");
            xmlStringWriter.write("</CLASS>\r\n");
        }
        xmlStringWriter.write("</MSG>\r\n");
    }

    /**
     * Creates a pswin xml document and writes it to the given outputstream.
     * 
     * @param os
     * @param msg
     * @param dest
     * @param sender
     * @throws IOException
     * @throws SmsException
     */
    private void writeXmlTo(OutputStream os, SmsMessage msg, SmsAddress dest, SmsAddress sender) throws IOException, SmsException {
        StringWriter xmlWriter = new StringWriter(1024);
        xmlWriter.write("<?xml version=\"1.0\"?>\r\n");
        xmlWriter.write("<SESSION>\r\n");
        xmlWriter.write("<CLIENT>" + username_ + "</CLIENT>\r\n");
        xmlWriter.write("<PW>" + password_ + "</PW>\r\n");
        xmlWriter.write("<MSGLST>\r\n");
        if (msg instanceof SmsTextMessage) {
            addTextMsg(xmlWriter, (SmsTextMessage) msg, dest, sender);
        } else {
            SmsPdu[] msgPdu = msg.getPdus();
            for (int i = 0; i < msgPdu.length; i++) {
                addMsg(xmlWriter, msgPdu[i], dest, sender);
            }
        }
        xmlWriter.write("</MSGLST>\r\n");
        xmlWriter.write("</SESSION>\r\n");
        String xmlDoc = xmlWriter.toString();
        os.write(xmlDoc.getBytes());
    }

    /**
     * Sends the given xml request to pswin for processing.
     * 
     * @param xmlReq
     * @throws IOException
     * @throws SmsException
     */
    private void sendReqToPsWinCom(byte[] xmlReq) throws IOException, SmsException {
        Socket xmlSocket = null;
        OutputStream os = null;
        InputStream is = null;
        try {
            xmlSocket = new Socket(server_, port_);
            os = xmlSocket.getOutputStream();
            is = xmlSocket.getInputStream();
            os.write(xmlReq);
            PsWinXmlResponse response = responseParser_.parse(is);
        } finally {
            if (os != null) try {
                os.close();
            } catch (Exception e) {
            }
            if (is != null) try {
                is.close();
            } catch (Exception e) {
            }
            if (xmlSocket != null) try {
                xmlSocket.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Send.
     * 
     * @param msg 
     * @param dest 
     * @param sender 
     * @return Internal message id. 
     * @throws SmsException 
     * @throws IOException 
     * 
     * @see org.marre.sms.transport.SmsTransport#send()
     */
    public String send(SmsMessage msg, SmsAddress dest, SmsAddress sender) throws SmsException, IOException {
        byte[] xmlReq;
        if (dest.getTypeOfNumber() == SmsConstants.TON_ALPHANUMERIC) {
            throw new SmsException("Cannot sent SMS to ALPHANUMERIC address");
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            writeXmlTo(baos, msg, dest, sender);
            baos.close();
            xmlReq = baos.toByteArray();
        } catch (IOException ex) {
            throw new SmsException("Failed to build xml request", ex);
        }
        sendReqToPsWinCom(xmlReq);
        return null;
    }

    /**
     * Connect.
     * 
     * @see org.marre.sms.transport.SmsTransport#connect()
     */
    public void connect() {
    }

    /**
     * Disconnect.
     * 
     * @see org.marre.sms.transport.SmsTransport#disconnect()
     */
    public void disconnect() {
    }

    /**
     * Ping.
     * 
     * @see org.marre.sms.transport.SmsTransport#ping()
     */
    public void ping() {
    }
}
