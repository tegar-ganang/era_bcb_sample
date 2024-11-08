package com.volantis.mps.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.volantis.mcs.servlet.MarinerServletApplication;
import com.volantis.mcs.servlet.MarinerServletRequestContext;
import com.volantis.mcs.marlin.sax.MarlinSAXHelper;
import com.volantis.mps.attachment.DeviceMessageAttachment;
import com.volantis.mps.attachment.MessageAttachments;
import com.volantis.mps.message.MessageException;
import com.volantis.mps.message.MultiChannelMessage;
import com.volantis.mps.message.MultiChannelMessageImpl;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.MessageRecipients;
import com.volantis.mps.recipient.RecipientException;
import com.volantis.mps.session.Session;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.mps.assembler.ContentUtilities;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import org.xml.sax.InputSource;

/**
 * A sample servlet that send messages using MPS. See the MpsRecipient.html
 * page for details on the form. The following request parameters are used to
 * collect recipient and message information:
 *
 * <dl>
 *
 * <dt>recipients</dt>
 *
 * <dd>list of recipients to send to</dd>
 *
 * <dt>device</dt>
 *
 * <dd>list of devices for each recipient (empty list item will use default
 * device)</dd>
 *
 * <dt>type</dt>
 *
 * <dd>type of recipient (to, cc, bcc)</dd>
 *
 * <dt>channel</dt>
 *
 * <dd>channel to send to (smtpc, mmsc, smsc or as set up in config file)</dd>
 *
 * <dt>subject</dt>
 *
 * <dd>message subject</dd>
 *
 * <dt>url</dt>
 *
 * <dd>the url to send as the message</dd>
 *
 * <dt>xml</dt>
 *
 * <dd>the XDIME xml to parse and send as message. Only used if url is
 * empty</dd>
 *
 * <dt>attachment</dt>
 *
 * <dd>list of attachments either a path to a local file or a URL</dd>
 *
 * <dt>attachmentValueType</dt>
 *
 * <dd>list of the type of attachments (1 = File, 2= URL)</dd>
 *
 * <dt>attachmentChannel</dt>
 *
 * <dd>channel this attachment will get attached to</dd>
 *
 * <dt>attachmentDevice</dt>
 *
 * <dd>sevice message for attaching attachment to</dd>
 *
 * <dt>attachmentMimeType</dt>
 *
 * <dd>content/MIME type of this attachment</dd>
 *
 * </dl>
 *
 * <p>Note that this sample does not perform localization of exceptions or
 * other messages.</p>
 */
public class RunMps extends HttpServlet {

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(RunMps.class);

    /**
     * The exception localizer instance for this class.
     */
    private static final ExceptionLocalizer localizer = LocalizationFactory.createExceptionLocalizer(RunMps.class);

    private final String RECIPIENTS = "to";

    private final String CCRECIPIENTS = "cc";

    private final String BCCRECIPIENTS = "bcc";

    /**
     * The HTML we use to generate the response.
     */
    private static final String HTML_MESSAGE_START = "<html><body>";

    /**
     * The HTML we use to generate the response.
     */
    private static final String HTML_MESSAGE_END = "</body></html>";

    /**
     * The XDIME we use to generate the response.
     */
    private static final String XDIME_MESSAGE_START = "<canvas layoutName=\"/error.mlyt\">" + "<pane name=\"error\">";

    /**
     * The XDIME we use to generate the response.
     */
    private static final String XDIME_MESSAGE_END = "</pane></canvas>";

    /**
     * The MSA to initialise MCS.
     */
    MarinerServletApplication mpsTest;

    /**
     * Initializes the new instance.
     */
    public RunMps() {
    }

    /**
     * Init the MarinerServletApplication as MPS
     */
    public void init() throws ServletException {
        super.init();
        mpsTest = MarinerServletApplication.getInstance(getServletConfig().getServletContext(), true);
    }

    /**
     * Collects recipient information from the servlet request, set up the MPS
     * recipients and session and then send the message.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MessageRecipients recipients = null;
        MessageRecipients ccRecipients = null;
        MessageRecipients bccRecipients = null;
        try {
            recipients = getRecipients(request, RECIPIENTS);
            ccRecipients = getRecipients(request, CCRECIPIENTS);
            bccRecipients = getRecipients(request, BCCRECIPIENTS);
            if (recipients == null) {
                throw new RecipientException("No recipients could be found");
            }
        } catch (Exception ae) {
            logger.error("Error loading recipient set", ae);
            writeError(request, response, ae);
            return;
        }
        String requestEncoding = request.getCharacterEncoding();
        if (requestEncoding == null) {
            requestEncoding = "ISO-8859-1";
        }
        String url = request.getParameter("url");
        MultiChannelMessage message = null;
        String characterSet = request.getParameter("charset");
        if (characterSet.equalsIgnoreCase("null")) {
            characterSet = null;
        }
        boolean genMessage = false;
        if (request.getParameter("genMsg") != null && request.getParameter("genMsg").equalsIgnoreCase("on")) {
            genMessage = true;
        }
        String subject = request.getParameter("subject");
        if (characterSet != null) {
            subject = ContentUtilities.convertEncoding(subject, requestEncoding, characterSet);
        }
        if (!(url.equals(""))) {
            message = new MultiChannelMessageImpl(new URL(url), subject, characterSet);
        } else {
            String xml = request.getParameter("xml");
            if (characterSet != null) {
                xml = ContentUtilities.convertEncoding(xml, requestEncoding, characterSet);
            }
            message = new MultiChannelMessageImpl(xml, subject, characterSet);
        }
        try {
            message.addAttachments(getAttachments(request));
        } catch (Exception ee) {
            logger.error("Failed to attach attachments", ee);
        }
        MessageRecipient fromUser = null;
        MessageRecipients failures = null;
        File outputFile = null;
        try {
            fromUser = new MessageRecipient();
            fromUser.setAddress(new InternetAddress("mps@volantis.com"));
            Session session = new Session();
            session.addRecipients("toList", recipients);
            session.addRecipients("ccList", ccRecipients);
            session.addRecipients("bccList", bccRecipients);
            if (genMessage) {
                outputFile = saveMessages(session, message, "toList", "ccList", "bccList", fromUser);
            }
            failures = session.send(message, "toList", "ccList", "bccList", fromUser);
        } catch (Exception rec) {
            logger.error("Error sending message ", rec);
            writeError(request, response, rec);
            return;
        }
        writeMesg(request, response, buildSuccessMessage(failures, outputFile));
    }

    /**
     * Generates a file output containing all information about generated messages
     *
     * @param session   Current session
     * @param mcm       MultiChannelMessage object
     * @param toList    The list of 'TO' recipients
     * @param ccList    The list of 'CC' recipients
     * @param bccList   The list of 'BCC' recipients
     * @param fromUser  The user from which the message is from
     *
     * @return  The <code>File</code> object that refers to the log file
     *
     * @throws Exception    Catch all exception
     */
    private File saveMessages(Session session, MultiChannelMessage mcm, String toList, String ccList, String bccList, MessageRecipient fromUser) throws Exception {
        String charSet = "UTF-8";
        if (mcm.getCharacterEncoding() != null) {
            charSet = mcm.getCharacterEncoding();
        }
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File temp = File.createTempFile("RUNMPS_", "_GeneratedOutput_" + charSet + ".txt", tempDir);
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(temp), "UTF8");
        List devices = new ArrayList(10);
        osw.write("Generated output\n");
        osw.write("-------------------------------------------------------\n");
        osw.write("MultiChannelMessage:\n");
        osw.write("Subject: " + mcm.getSubject() + "\n");
        osw.write("Content: " + mcm.getMessage() + "\n");
        osw.write("URL    : " + mcm.getMessageURL() + "\n");
        osw.write("Charset: " + mcm.getCharacterEncoding() + "\n");
        osw.write("-------------------------------------------------------\n");
        osw.write("Sender\n");
        osw.write("Address: " + fromUser.getAddress() + "\n");
        osw.write("MSISDN : " + fromUser.getMSISDN() + "\n");
        osw.write("Channel: " + fromUser.getChannelName() + "\n");
        osw.write("Device : " + fromUser.getDeviceName() + "\n");
        osw.write("\nMessageRecipients\n");
        MessageRecipients mrs = session.getRecipients(toList);
        osw.write("\n'TO' recipients\n");
        Iterator it = mrs.getIterator();
        while (it.hasNext()) {
            MessageRecipient mr = (MessageRecipient) it.next();
            osw.write("Address: " + mr.getAddress() + "\n");
            osw.write("MSISDN : " + mr.getMSISDN() + "\n");
            osw.write("Channel: " + mr.getChannelName() + "\n");
            osw.write("Device : " + mr.getDeviceName() + "\n\n");
            if (!devices.contains(mr.getDeviceName())) {
                devices.add(mr.getDeviceName());
            }
        }
        mrs = session.getRecipients(ccList);
        osw.write("\n'CC' recipients\n");
        it = mrs.getIterator();
        while (it.hasNext()) {
            MessageRecipient mr = (MessageRecipient) it.next();
            osw.write("Address: " + mr.getAddress() + "\n");
            osw.write("MSISDN : " + mr.getMSISDN() + "\n");
            osw.write("Channel: " + mr.getChannelName() + "\n");
            osw.write("Device : " + mr.getDeviceName() + "\n\n");
            if (!devices.contains(mr.getDeviceName())) {
                devices.add(mr.getDeviceName());
            }
        }
        mrs = session.getRecipients(bccList);
        osw.write("\n'BCC' recipients\n");
        it = mrs.getIterator();
        while (it.hasNext()) {
            MessageRecipient mr = (MessageRecipient) it.next();
            osw.write("Address: " + mr.getAddress() + "\n");
            osw.write("MSISDN : " + mr.getMSISDN() + "\n");
            osw.write("Channel: " + mr.getChannelName() + "\n");
            osw.write("Device : " + mr.getDeviceName() + "\n\n");
            if (!devices.contains(mr.getDeviceName())) {
                devices.add(mr.getDeviceName());
            }
        }
        osw.write("-------------------------------------------------------\n");
        it = devices.iterator();
        while (it.hasNext()) {
            String device = (String) it.next();
            osw.write("Message generation for device: " + device + "\n");
            osw.write("\nGenerate as string\n");
            try {
                String ret = new String(mcm.generateTargetMessageAsString(device).getBytes("utf-8"), "utf-8");
                osw.write(ret);
                osw.write("\n");
            } catch (MessageException e) {
                osw.write("ERROR: " + e.getMessage() + "\n");
            }
            osw.write("\nGenerate as URL\n");
            try {
                URL ret = mcm.generateTargetMessageAsURL(device, null);
                osw.write(ret.toString());
                osw.write("\n");
            } catch (MessageException e) {
                osw.write("ERROR: " + e.getMessage() + "\n");
            }
            osw.write("\nGenerate as Mime\n");
            try {
                MimeMultipart ret = mcm.generateTargetMessageAsMimeMultipart(device);
                writeMimeMultipart(ret, osw);
                osw.write("\n");
            } catch (MessageException e) {
                osw.write("ERROR: " + e.getMessage() + "\n");
            }
        }
        osw.write("-------------------------------------------------------\n");
        osw.flush();
        osw.close();
        logger.info("Generated output placed in: " + temp.getAbsolutePath());
        return temp;
    }

    /**
     * Write the contents of the <code>MimeMultipart</code> message to the
     * specified output writer
     *
     * @param mimeMultipart     The <code>MimeMultipart</code> object to log
     * @param osw               The writer object to write to
     *
     * @throws MessageException Thrown by errors accessing the multipart content
     */
    private void writeMimeMultipart(MimeMultipart mimeMultipart, OutputStreamWriter osw) throws MessageException {
        try {
            int count = mimeMultipart.getCount();
            osw.write("MimeMultipart Object has " + count + " parts\n");
            MimeBodyPart b;
            Object o;
            InputStream in;
            InputStreamReader inr;
            for (int k = 0; k < count; k++) {
                b = (MimeBodyPart) mimeMultipart.getBodyPart(k);
                osw.write("Body Part index: " + k + " ID: " + b.getContentID() + "\n");
                osw.write("MD5: " + b.getContentMD5() + "\n");
                osw.write("Content Type: " + b.getContentType() + "\n");
                osw.write("Description: " + b.getDescription() + "\n");
                osw.write("Disposition: " + b.getDisposition() + "\n");
                osw.write("Encoding: " + b.getEncoding() + "\n");
                osw.write("Filename: " + b.getFileName() + "\n");
                osw.write("Line Count: " + String.valueOf(b.getLineCount()) + "\n");
                osw.write("Size: " + String.valueOf(b.getSize()) + "\n");
                o = b.getContent();
                osw.write("MimeBodyPart Object: " + o.getClass().getName() + "\n");
                if (o instanceof String) {
                    osw.write("Value is String type" + "\n");
                    String s = (String) o;
                    osw.write("Value: " + s + "\n");
                } else if (o instanceof MimeMultipart) {
                    osw.write(">>>>>" + "\n");
                    writeMimeMultipart((MimeMultipart) o, osw);
                    osw.write("<<<<<" + "\n");
                } else {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Value: ");
                    in = b.getInputStream();
                    inr = new InputStreamReader(in, "UTF-8");
                    int l = inr.read();
                    while (l != -1) {
                        sb.append((char) l);
                        l = inr.read();
                    }
                    osw.write(sb.toString());
                }
            }
        } catch (Exception e) {
            throw new MessageException(e.getMessage());
        }
    }

    /**
     * Uses the canvas defined by failures start/end to build a message.
     *
     * @param failures List of MessageRecipients that failed
     * @return String String of XML that represnet HTML
     */
    private String buildSuccessMessage(MessageRecipients failures, File log) {
        StringBuffer mesg = new StringBuffer();
        mesg.append("<h1>MPS Test Complete. Messages sent</h1>");
        if (log != null) {
            mesg.append("<h3>Output written to " + log.getAbsolutePath() + "</h3>");
        }
        Iterator itr = failures.getIterator();
        if (itr.hasNext()) {
            mesg.append("<h3>Failures are</h3>");
            while (itr.hasNext()) {
                MessageRecipient rec = (MessageRecipient) itr.next();
                try {
                    String reason = rec.getFailureReason();
                    if (rec.getAddress() != null) {
                        mesg.append(rec.getAddress().toString()).append(" (");
                    } else if (rec.getMSISDN() != null) {
                        mesg.append(rec.getMSISDN()).append(" (");
                    } else {
                        mesg.append("UNKNOWN-ADDRESS").append(" (");
                    }
                    if (reason == null) {
                        mesg.append("no reason specified");
                    } else {
                        mesg.append(reason);
                    }
                    mesg.append(')');
                } catch (Exception e) {
                }
                mesg.append("<br />");
            }
        }
        return mesg.toString();
    }

    /**
     * Creates a MessageAttachments object from the parameters coming in from
     * the HTTP request.
     *
     * @param request
     * @return MessageAttachments
     */
    private MessageAttachments getAttachments(HttpServletRequest request) {
        String attachment[] = request.getParameterValues("attachment");
        String attachmentValueType[] = request.getParameterValues("attachmentValueType");
        String attachmentChannel[] = request.getParameterValues("attachmentChannel");
        String attachmentDevice[] = request.getParameterValues("attachmentDevice");
        String attachmentMimeType[] = request.getParameterValues("attachmentMimeType");
        MessageAttachments messageAttachments = new MessageAttachments();
        for (int i = 0; i < attachment.length; i++) {
            if (!attachment[i].equals("")) {
                DeviceMessageAttachment dma = new DeviceMessageAttachment();
                try {
                    dma.setChannelName(attachmentChannel[i]);
                    dma.setDeviceName(attachmentDevice[i]);
                    dma.setValue(attachment[i]);
                    dma.setValueType(Integer.parseInt(attachmentValueType[i]));
                    if (!attachmentMimeType[i].equals("")) {
                        dma.setMimeType(attachmentMimeType[i]);
                    }
                    messageAttachments.addAttachment(dma);
                } catch (MessageException me) {
                    logger.error("Failed to create attachment for " + attachment[i], me);
                }
            }
        }
        return messageAttachments;
    }

    /**
     * Loads a recipient set from the ServletRequest by looking at parameters
     * "recipients" and "device". If "device" is "" or there are fewer devices
     * than recipients then no device is specified for the recipient. If the
     * channel is specified as SMS or WAPPush then the MSISDN of the recipient
     * is set rather than the address.
     *
     * @param request The servletRequest
     * @param inType  The type of recipients we are trying to load
     *                (to,cc,bcc);
     * @return MessageRecipients A list of recipients for the inType specified
     * @throws RecipientException If there is a problem extracting the
     *                            recipients from the request
     * @throws AddressException   If there is a problem extracting the
     *                            recipients from the request
     */
    private MessageRecipients getRecipients(HttpServletRequest request, String inType) throws RecipientException, AddressException {
        String[] names = request.getParameterValues("recipients");
        String[] devices = request.getParameterValues("device");
        String[] type = request.getParameterValues("type");
        String[] channel = request.getParameterValues("channel");
        MessageRecipients messageRecipients = new MessageRecipients();
        for (int i = 0; i < type.length; i++) {
            if (type[i].equals(inType)) {
                if (!names[i].equals("")) {
                    MessageRecipient messageRecipient = new MessageRecipient();
                    if (channel.length > i && !channel[i].equals("")) {
                        messageRecipient.setChannelName(channel[i]);
                        if (channel[i].equals("smsc")) {
                            messageRecipient.setMSISDN(names[i]);
                        } else if (channel[i].equals("wappush")) {
                            messageRecipient.setMSISDN(names[i]);
                        } else if (channel[i].equals("mmsc") && names[i].charAt(0) == '+') {
                            messageRecipient.setMSISDN(names[i]);
                        } else {
                            messageRecipient.setAddress(new InternetAddress(names[i]));
                        }
                    } else {
                        messageRecipient.setChannelName("smtp");
                    }
                    if (devices.length > i) {
                        String device = devices[i];
                        if (!device.equals("")) {
                            messageRecipient.setDeviceName(device);
                        }
                    }
                    messageRecipients.addRecipient(messageRecipient);
                }
            }
        }
        return messageRecipients;
    }

    /**
     * Writes an error out to the response using the canvas defined in
     * failureStart and failureEnd. The layout "error" with a pne error must be
     * defined in the repository for this to work.
     *
     * @param request
     * @param response
     * @param except   The exception to write out
     */
    private void writeError(HttpServletRequest request, HttpServletResponse response, Exception except) {
        try {
            MarinerServletRequestContext msrc = new MarinerServletRequestContext(getServletConfig().getServletContext(), request, response);
            StringWriter writer = new StringWriter();
            writer.write(XDIME_MESSAGE_START);
            writer.write("<h1>MPS error occured</h1>");
            writer.write("<h3>Check Servlet and Volantis log for more information</h3>");
            writer.write(XDIME_MESSAGE_END);
            MarlinSAXHelper.parse(msrc, MarlinSAXHelper.getDefaultErrorHandler(), new InputSource(new StringReader(writer.toString())));
        } catch (Exception e) {
            try {
                response.setContentType("text/html; charset=\"UTF-8\"");
                OutputStream os = response.getOutputStream();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
                out.println(HTML_MESSAGE_START);
                out.println("<h1>MPS error occured</h1>");
                out.println("<h3>Check Servlet and Volantis log for more information</h3>");
                out.println(HTML_MESSAGE_END);
            } catch (IOException ie) {
                logger.error("Failed to write error because of ", ie);
            }
        }
    }

    /**
     * Writes a message out to the HTTP response. Uses the failureStart and
     * failureEnd as XDIME for the message. The layout error with a pane called
     * error must be defined in the repository.
     *
     * @param request
     * @param response
     * @param mesg     The message to write out
     */
    private void writeMesg(HttpServletRequest request, HttpServletResponse response, String mesg) {
        try {
            MarinerServletRequestContext msrc = new MarinerServletRequestContext(getServletConfig().getServletContext(), request, response);
            MarlinSAXHelper.parse(msrc, MarlinSAXHelper.getDefaultErrorHandler(), new InputSource(new StringReader(XDIME_MESSAGE_START + mesg + XDIME_MESSAGE_END)));
        } catch (Exception e) {
            try {
                response.setContentType("text/html; charset=\"UTF-8\"");
                OutputStream os = response.getOutputStream();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
                out.println(mesg);
            } catch (IOException ie) {
                logger.error("Failed to write error because of ", ie);
            }
        }
    }
}
