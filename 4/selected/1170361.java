package org.xaware.server.engine.instruction.bizcomps;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.springframework.core.io.Resource;
import org.w3c.tools.codec.Base64FormatException;
import org.xaware.server.engine.channel.smtp.SmtpBizDriver;
import org.xaware.server.engine.channel.smtp.SmtpChannelSpecification;
import org.xaware.server.engine.channel.smtp.SmtpTemplate;
import org.xaware.server.engine.channel.smtp.SmtpTemplateFactory;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.engine.exceptions.XAwareProcessingException;
import org.xaware.server.engine.instruction.bizcomps.config.BizDriverConfig;
import org.xaware.server.engine.instruction.bizcomps.smtp.SmtpConfigTranslator;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.EncodingHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * SMTP BizComponents send e-mail messages from one defined mail server to another. They support
 * common e-mail attributes such as carbon copy lists, subject lines, and the ability to send
 * attachments. One common use of the SMTP BizComponent is to send attachments from one data
 * integration process to a mail server where the attachment can be accessed by another data
 * integration process. You can use the SMTP BizComponent in conjunction with a POP3 BizComponent to
 * receive mail messages from a mail server if the receipt of messages is necessary for your
 * application.
 * 
 * XA-Designer provides multiple SMTP BizComponent templates for use when building the SMTP portion
 * of your data integration application. Because the SMTP BizComponent accesses an Internet mail
 * service directly, no BizDriver is necessary for this BizComponent to execute.
 * 
 * To execute the SMTP BizComponent, you must have access to the mail server and a User connection.
 * These values must be included with the xa:server, the xa:uid, and xa:password attributes.
 * Commonly, port 25 is used for SMTP services; however, if your mail server uses another port
 * number, you will have to include that port number as the value of the xa:port attribute of the
 * xa:request element.
 * 
 * <p>
 * Supported Elements:
 * <li>xa:attachment</li>
 * <li>xa:body</li>
 * <li>xa:bcc</li>
 * <li>xa:bcclist</li>
 * <li>xa:cc</li>
 * <li>xa:cclist</li>
 * <li>xa:content</li>
 * <li>xa:description</li>
 * <li>xa:from</li>
 * <li>xa:fromlist</li>
 * <li>xa:input</li>
 * <li>xa:multiple_attachments</li>
 * <li>xa:param</li>
 * <li>xa:request</li>
 * <li>xa:subject</li>
 * <li>xa:to</li>
 * <li>xa:tolist</li>
 * </p>
 * 
 * <p>
 * Supported Attributes:
 * <li>xa:bizcomptype</li>
 * <li>xa:data_path</li>
 * <li>xa:datatype</li>
 * <li>xa:default</li>
 * <li>xa:description</li>
 * <li>xa:file</li>
 * <li>xa:input_type</li>
 * <li>xa:mimetype</li>
 * <li>xa:name</li>
 * <li>xa:on_substitute_failure</li>
 * </p>
 * 
 * @author dwieland
 */
public class SmtpBizCompInst extends BaseBizComponentInst {

    /** Instance of logger */
    XAwareLogger logger = XAwareLogger.getXAwareLogger(SmtpBizCompInst.class.getName());

    private static final Namespace ns = XAwareConstants.xaNamespace;

    private static final String DEFAULT_ENCODING_CHARSET = "UTF-8";

    private static final String MIMETYPE = "mimetype";

    private SmtpTemplateFactory m_smtpTemplateFactory;

    private SmtpTemplate m_template;

    private SmtpConfigTranslator m_translator;

    private Transport m_transport;

    /**
     * Base constructor
     * 
     */
    public SmtpBizCompInst() {
        super(false);
    }

    /**
     * String representation of who we are
     * 
     * @see org.xaware.server.engine.instruction.bizcomps.BaseBizComponentInst#getName()
     */
    public String getName() {
        return "SmtpBizCompInst";
    }

    @Override
    protected Element performOneUnitOfWork() throws XAwareException {
        try {
            final MimeMessage message = initializeMimeMessageWithAddresses();
            message.setSubject(m_translator.getSubject());
            BodyPart messageBodyPart = new MimeBodyPart();
            final Multipart multipart = new MimeMultipart();
            messageBodyPart.setText(m_translator.getBody());
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            boolean hasAttachments = addAttachments(message, multipart);
            if (m_translator.hasBody() || hasAttachments) {
                message.setContent(multipart);
            }
            message.saveChanges();
            m_transport.sendMessage(message, message.getAllRecipients());
            setNothingLeftToDo();
        } catch (MessagingException e) {
            String msg = "SMTP failed to send: " + e.getMessage();
            lf.severe(msg);
            throw new XAwareProcessingException(msg);
        }
        return scriptNode.getElement();
    }

    /**
     * loop thru attachments building and adding them to the message.
     * 
     * @param message
     * @param messageBodyPart
     * @param multipart
     * @throws XAwareException 
     */
    private boolean addAttachments(MimeMessage message, Multipart multipart) throws XAwareException {
        boolean hasAttachments = false;
        BodyPart messageBodyPart = null;
        Element[] attachmentElements = m_translator.getAttachmentElements();
        try {
            for (int i = 0; i < attachmentElements.length; i++) {
                Element attachElem = attachmentElements[i];
                messageBodyPart = new MimeBodyPart();
                String filename = attachElem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_NAME, ns);
                messageBodyPart.setFileName(filename);
                String fileLocation = attachElem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_FILENAME, ns);
                final String sContent = processAttachmentElement(attachElem).toString();
                String sContentType = attachElem.getAttributeValue(MIMETYPE, ns);
                if (sContentType == null || sContentType.equals("")) {
                    sContentType = "text/plain";
                }
                if (sContentType.equals("text/xml")) {
                    messageBodyPart.setContent(sContent, sContentType);
                    ensureMsgBodyPartHasFileName(messageBodyPart, "file.xml");
                } else if (sContentType.equals("text/plain")) {
                    ensureMsgBodyPartHasFileName(messageBodyPart, "file.txt");
                    if (fileLocation == null && sContent == null) {
                        throw new XAwareProcessingException("No file attribute or text content element was specified." + " Unable to process the attachment");
                    } else if (sContent != null && sContent.length() > 0) {
                        messageBodyPart.setContent(sContent, sContentType);
                    } else if (fileLocation != null) {
                        DataHandler attachmentDataHandler = getDataHandlerForAttachment(fileLocation);
                        messageBodyPart.setDataHandler(attachmentDataHandler);
                    }
                } else {
                    if (fileLocation == null) {
                        throw new XAwareProcessingException("No file attribute was specified for the PDF attachment.  Unable to process the attachment");
                    }
                    final String extension = fileLocation.substring(fileLocation.indexOf("."));
                    ensureMsgBodyPartHasFileName(messageBodyPart, "file" + extension);
                    DataHandler attachmentDataHandler = getDataHandlerForAttachment(fileLocation);
                    messageBodyPart.setDataHandler(attachmentDataHandler);
                }
                multipart.addBodyPart(messageBodyPart);
                hasAttachments = true;
            }
        } catch (MessagingException e) {
            throw new XAwareProcessingException("Error adding attachment: " + e.getMessage());
        }
        return hasAttachments;
    }

    /**
     * @param messageBodyPart
     * @param fName
     * @throws MessagingException
     */
    private void ensureMsgBodyPartHasFileName(BodyPart messageBodyPart, String fName) throws MessagingException {
        if ((messageBodyPart.getFileName() == null || messageBodyPart.getFileName().length() == 0)) {
            messageBodyPart.setFileName(fName);
        }
    }

    /**
     * Initialize a MimeMessage with addresses from SmtpConfigTranslator.
     * 
     * @return MimeMessage with addresses set
     * @throws XAwareConfigurationException
     * @throws XAwareSubstitutionException
     * @throws XAwareConfigMissingException
     * @throws XAwareException
     * @throws MessagingException
     */
    private MimeMessage initializeMimeMessageWithAddresses() throws XAwareConfigurationException, XAwareSubstitutionException, XAwareConfigMissingException, XAwareException, MessagingException {
        String authValue = m_bizDriver.getChannelSpecification().getProperty(SmtpChannelSpecification.AUTH_KEY);
        final Properties props = new Properties();
        props.setProperty(SmtpChannelSpecification.AUTH_KEY, authValue);
        final Session session = Session.getDefaultInstance(props, null);
        final MimeMessage message = new MimeMessage(session);
        Address[] toAddr = m_translator.getToAddr();
        message.addRecipients(Message.RecipientType.TO, toAddr);
        Address[] ccAddr = m_translator.getCcAddr();
        if (ccAddr.length != 0) {
            message.addRecipients(Message.RecipientType.CC, ccAddr);
        }
        Address[] bccAddr = m_translator.getBccAddr();
        if (bccAddr.length != 0) {
            message.addRecipients(Message.RecipientType.BCC, bccAddr);
        }
        Address[] fromAddr = m_translator.getFromAddr();
        message.addFrom(fromAddr);
        Address[] replyAddr = m_translator.getBccAddr();
        if (replyAddr.length != 0) {
            message.setReplyTo(replyAddr);
        }
        return message;
    }

    @Override
    protected void releaseResources() throws XAwareException {
    }

    /**
     * Initialize template and get transport for sending messages.
     */
    @Override
    protected void setupResources() throws XAwareException {
        m_translator.initializeVariables();
        m_transport = m_template.getMailTransport();
    }

    /**
     * Parse our XML and set up our configuration
     * 
     * @see org.xaware.server.engine.instruction.bizcomps.BaseBizComponentInst#transferConfigInfo()
     */
    @Override
    protected void transferConfigInfo() throws XAwareException {
        this.substituteAllElements(m_requestElem, this.m_inputElem);
        this.scriptNode.getContext().substituteAllAttributes(m_requestElem, null, this.scriptNode.getEffectiveSubstitutionFailureLevel());
        this.addElementToPreprocess(m_requestElem);
        Element root = scriptNode.getElement();
        BizDriverConfig bizDriverConfig = new BizDriverConfig(root, lf, getBizCompContext());
        m_bizDriver = m_bizDriverFactory.createBizDriverInstance(bizDriverConfig, getBizCompContext());
        m_template = m_smtpTemplateFactory.getTemplate((SmtpBizDriver) m_bizDriver, scriptNode);
        m_translator = new SmtpConfigTranslator(m_requestElem, getBizCompContext(), logger);
    }

    /**
     * Set our Template Factory. This will be called by the framework?
     * 
     * @param smtpTemplateFactory
     */
    public final void setSmtpTemplateFactory(SmtpTemplateFactory p_smtpTemplateFactory) {
        m_smtpTemplateFactory = p_smtpTemplateFactory;
    }

    private StringBuffer processAttachmentElement(final Element attachElement) throws XAwareProcessingException {
        final Iterator iter = attachElement.getChildren().iterator();
        final StringBuffer sOutput = new StringBuffer(1024);
        while (iter.hasNext()) {
            final Element e = (Element) iter.next();
            if (e.getName().equals(XAwareConstants.BIZCOMPONENT_ELEMENT_CONTENT) == false) {
                continue;
            }
            sOutput.append(processContentElement(e));
        }
        return sOutput;
    }

    /**
     * Gets the content of content element and creates a StringBuffer
     * 
     * @param contentElement    
     * @return StringBuffer
     */
    private StringBuffer processContentElement(final Element contentElement) throws XAwareProcessingException {
        StringBuffer sDataBuffer = new StringBuffer(1024);
        sDataBuffer = processXAContentElement(contentElement);
        String dataType = contentElement.getAttributeValue(XAwareConstants.XAWARE_ATTR_DATATYPE, ns);
        if (dataType == null) {
            dataType = XAwareConstants.XAWARE_DATATYPE_XML;
        }
        if (dataType.equals(XAwareConstants.XAWARE_DATATYPE_BINARY)) {
            StringBuffer sContent = null;
            try {
                sContent = new StringBuffer(1024);
                sContent.append(EncodingHelper.decodeBase64(sDataBuffer.toString()));
            } catch (final Base64FormatException e) {
                throw new XAwareProcessingException("Base64FormatException decoding buffer:" + e.getMessage());
            }
            return sContent;
        }
        return sDataBuffer;
    }

    /**
     * Gets the content of content element and creates a StringBuffer
     * 
     * @param contentElement    
     * @return StringBuffer
     */
    protected StringBuffer processXAContentElement(final Element contentElement) {
        final StringBuffer sContent = new StringBuffer(1024);
        final Iterator iter = contentElement.getContent().iterator();
        while (iter.hasNext()) {
            final Object obj = iter.next();
            if (obj instanceof CDATA) {
                sContent.append(((CDATA) obj).getText());
            } else if (obj instanceof Element) {
                final Element tempelem = (Element) obj;
                final String encodeValue = tempelem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_ENCODE, ns);
                if ((encodeValue != null) && (encodeValue.length() > 0) && (encodeValue.equals(XAwareConstants.XAWARE_YES))) {
                    try {
                        sContent.append(URLEncoder.encode(elemToString(tempelem), DEFAULT_ENCODING_CHARSET));
                    } catch (final UnsupportedEncodingException caughtEx) {
                        lf.debug(caughtEx);
                    }
                } else {
                    sContent.append(elemToString(tempelem));
                }
            } else if ((obj instanceof Text) || (obj instanceof String)) {
                String sobj = null;
                if (obj instanceof Text) {
                    sobj = ((Text) obj).getText();
                } else {
                    sobj = (String) obj;
                }
                sContent.append(sobj);
            }
        }
        return sContent;
    }

    /**
     * convert and element and all its children to a string
     * 
     * @param elem
     * @return
     */
    private String elemToString(final Element elem) {
        if (elem == null) {
            return "";
        }
        final XMLOutputter out = new XMLOutputter();
        return out.outputString(elem);
    }

    /**
     * Use the ResourceHelper to identify the actual file location and create
     * a handler from that.  Return the handler or 
     * @param fName
     * @return
     */
    private DataHandler getDataHandlerForAttachment(String fName) throws XAwareProcessingException {
        DataHandler attachmentDataHandler = null;
        Resource resource = ResourceHelper.getResource(fName);
        try {
            URL resourceUrl = resource.getURL();
            if (resourceUrl.openConnection() != null) {
                attachmentDataHandler = new DataHandler(resourceUrl);
            } else {
                throw new XAwareProcessingException("unable to open attachment " + fName);
            }
        } catch (IOException e1) {
            try {
                File resourceFile = resource.getFile();
                attachmentDataHandler = new DataHandler(new FileDataSource(resourceFile));
            } catch (final IOException e) {
                throw new XAwareProcessingException("Error occurred reading attachment file " + fName + ": " + e);
            }
        }
        return attachmentDataHandler;
    }

    @Override
    protected XAwareLogger getLogger() {
        return lf;
    }
}
