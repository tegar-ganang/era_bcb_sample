package org.apache.axis.transport.mail;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPFault;
import org.apache.axis.server.AxisServer;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.util.Properties;

public class MailWorker implements Runnable {

    protected static Log log = LogFactory.getLog(MailWorker.class.getName());

    private MailServer server;

    private MimeMessage mimeMessage;

    private static String transportName = "Mail";

    private Properties prop = new Properties();

    private Session session = Session.getDefaultInstance(prop, null);

    /**
     * Constructor for MailWorker
     * @param server
     * @param mimeMessage
     */
    public MailWorker(MailServer server, MimeMessage mimeMessage) {
        this.server = server;
        this.mimeMessage = mimeMessage;
    }

    /**
     * The main workhorse method.
     */
    public void run() {
        AxisServer engine = MailServer.getAxisServer();
        MessageContext msgContext = new MessageContext(engine);
        Message requestMsg;
        StringBuffer soapAction = new StringBuffer();
        StringBuffer fileName = new StringBuffer();
        StringBuffer contentType = new StringBuffer();
        StringBuffer contentLocation = new StringBuffer();
        Message responseMsg = null;
        try {
            msgContext.setTargetService(null);
        } catch (AxisFault fault) {
        }
        msgContext.setResponseMessage(null);
        msgContext.reset();
        msgContext.setTransportName(transportName);
        responseMsg = null;
        try {
            try {
                parseHeaders(mimeMessage, contentType, contentLocation, soapAction);
                msgContext.setProperty(Constants.MC_REALPATH, fileName.toString());
                msgContext.setProperty(Constants.MC_RELATIVE_PATH, fileName.toString());
                msgContext.setProperty(Constants.MC_JWS_CLASSDIR, "jwsClasses");
                String soapActionString = soapAction.toString();
                if (soapActionString != null) {
                    msgContext.setUseSOAPAction(true);
                    msgContext.setSOAPActionURI(soapActionString);
                }
                requestMsg = new Message(mimeMessage.getInputStream(), false, contentType.toString(), contentLocation.toString());
                msgContext.setRequestMessage(requestMsg);
                engine.invoke(msgContext);
                responseMsg = msgContext.getResponseMessage();
                if (responseMsg == null) {
                    throw new AxisFault(Messages.getMessage("nullResponse00"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                AxisFault af;
                if (e instanceof AxisFault) {
                    af = (AxisFault) e;
                    log.debug(Messages.getMessage("serverFault00"), af);
                } else {
                    af = AxisFault.makeFault(e);
                }
                responseMsg = msgContext.getResponseMessage();
                if (responseMsg == null) {
                    responseMsg = new Message(af);
                } else {
                    try {
                        SOAPEnvelope env = responseMsg.getSOAPEnvelope();
                        env.clearBody();
                        env.addBodyElement(new SOAPFault((AxisFault) e));
                    } catch (AxisFault fault) {
                    }
                }
            }
            String replyTo = ((InternetAddress) mimeMessage.getReplyTo()[0]).getAddress();
            String sendFrom = ((InternetAddress) mimeMessage.getAllRecipients()[0]).getAddress();
            String subject = "Re: " + mimeMessage.getSubject();
            writeUsingSMTP(msgContext, server.getHost(), sendFrom, replyTo, subject, responseMsg);
        } catch (Exception e) {
            e.printStackTrace();
            log.debug(Messages.getMessage("exception00"), e);
        }
        if (msgContext.getProperty(MessageContext.QUIT_REQUESTED) != null) {
            try {
                server.stop();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Send the soap request message to the server
     * 
     * @param msgContext
     * @param smtpHost
     * @param sendFrom
     * @param replyTo
     * @param output
     * @throws Exception
     */
    private void writeUsingSMTP(MessageContext msgContext, String smtpHost, String sendFrom, String replyTo, String subject, Message output) throws Exception {
        SMTPClient client = new SMTPClient();
        client.connect(smtpHost);
        System.out.print(client.getReplyString());
        int reply = client.getReplyCode();
        if (!SMTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            AxisFault fault = new AxisFault("SMTP", "( SMTP server refused connection )", null, null);
            throw fault;
        }
        client.login(smtpHost);
        System.out.print(client.getReplyString());
        reply = client.getReplyCode();
        if (!SMTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            AxisFault fault = new AxisFault("SMTP", "( SMTP server refused connection )", null, null);
            throw fault;
        }
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(sendFrom));
        msg.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(replyTo));
        msg.setDisposition(MimePart.INLINE);
        msg.setSubject(subject);
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
        output.writeTo(out);
        msg.setContent(out.toString(), output.getContentType(msgContext.getSOAPConstants()));
        ByteArrayOutputStream out2 = new ByteArrayOutputStream(8 * 1024);
        msg.writeTo(out2);
        client.setSender(sendFrom);
        System.out.print(client.getReplyString());
        client.addRecipient(replyTo);
        System.out.print(client.getReplyString());
        Writer writer = client.sendMessageData();
        System.out.print(client.getReplyString());
        writer.write(out2.toString());
        writer.flush();
        writer.close();
        System.out.print(client.getReplyString());
        if (!client.completePendingCommand()) {
            System.out.print(client.getReplyString());
            AxisFault fault = new AxisFault("SMTP", "( Failed to send email )", null, null);
            throw fault;
        }
        System.out.print(client.getReplyString());
        client.logout();
        client.disconnect();
    }

    /**
     * Read all mime headers, returning the value of Content-Length and
     * SOAPAction.
     * @param mimeMessage         InputStream to read from
     * @param contentType The content type.
     * @param contentLocation The content location
     * @param soapAction StringBuffer to return the soapAction into
     */
    private void parseHeaders(MimeMessage mimeMessage, StringBuffer contentType, StringBuffer contentLocation, StringBuffer soapAction) throws Exception {
        contentType.append(mimeMessage.getContentType());
        contentLocation.append(mimeMessage.getContentID());
        String values[] = mimeMessage.getHeader(HTTPConstants.HEADER_SOAP_ACTION);
        if (values != null) soapAction.append(values[0]);
    }
}
