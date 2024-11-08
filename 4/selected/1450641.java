package org.smslib.smsserver.interfaces;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.smslib.OutboundMessage;
import org.smslib.smsserver.InterfaceTypes;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This interface uses xml-files to read outgoing messages and write inbound
 * messages.<br />
 * Every file contains neither <u>ONE</u> inbound <b>or</b> <u>ONE</u>
 * outbound message.
 * <hr />
 * The DTDs for the xml files containing a inbound message:<br />
 * 
 * <pre>
 * &lt;!ELEMENT message (originator, text, receive_date)&gt;
 * &lt;!ATTLIST message
 *   id		ID	#REQUIRED
 *   gateway_id	CDATA	#REQUIRED
 *   type		CDATA	#IMPLIED
 *   encoding	CDATA	#IMPLIED &gt;
 * &lt;!ELEMENT originator (#PCDATA)&gt;
 * &lt;!ELEMENT text (#PCDATA)&gt;
 * &lt;!ELEMENT receive_date (#PCDATA)&gt; 
 * </pre>
 * 
 * <hr />
 * The DTDs for the xml files containing a outgoing message:<br />
 * 
 * <pre>
 * &lt;!ELEMENT message (recipient, text, originator, create_date?)&gt;
 * &lt;!ATTLIST message 
 *    id	 	 ID      #REQUIRED
 *    gateway_id	 CDATA	#IMPLIED
 *    status         CDATA  &quot;U&quot; 
 *    encoding       CDATA	&quot;7&quot;
 *    priority       CDATA	&quot;N&quot;
 *    ref_no         CDATA	#IMPLIED
 *    status_report  CDATA	#IMPLIED
 *    flash_sms      CDATA	#IMPLIED
 *    src_port       CDATA	#IMPLIED
 *    dst_port       CDATA	#IMPLIED &gt; 
 * &lt;!ELEMENT recipient (#PCDATA)&gt;
 * &lt;!ELEMENT text (#PCDATA)&gt;
 * &lt;!ELEMENT create_date (#PCDATA)&gt;
 * &lt;!ELEMENT originator (#PCDATA)&gt;
 * </pre>
 * 
 * @author Sebastian Just
 */
public class Xml extends org.smslib.smsserver.AInterface {

    public static final String sOutSentDirectory = "sent";

    public static final String sOutFailedDirectory = "failed";

    public static final String sOutBrokenDirectory = "broken";

    private static final SimpleDateFormat fileSdf = new SimpleDateFormat("yyyyMMddHHmmss-S");

    private static final SimpleDateFormat iso8601Sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
	 * Formats a string ISO8601 compliant.
	 * 
	 * @param date
	 *            the date to format
	 * @return a string with a ISO8601 compliant date
	 */
    protected String getDateAsISO8601(Date date) {
        String result = iso8601Sdf.format(date);
        StringBuilder sb = new StringBuilder(result.length() + 1);
        sb.append(result.substring(0, result.length() - 2));
        sb.append(":");
        sb.append(result.substring(result.length() - 2));
        return sb.toString();
    }

    /**
	 * Creates a date from a ISO8601 string
	 * 
	 * @param string
	 *            The string to parse
	 * @return A date
	 */
    protected Date getISO8601AsDate(String string) {
        StringBuilder sb = new StringBuilder(string);
        sb.replace(string.length() - 3, string.length() - 2, "");
        try {
            return iso8601Sdf.parse(sb.toString());
        } catch (ParseException e) {
            logWarn("Can't parse " + string + " as ISO8601 date!");
            return null;
        }
    }

    private File inDirectory;

    private File outDirectory;

    private File outFailedDirectory;

    private File outSentDirectory;

    private Map processedFiles;

    private File outBrokenDirectory;

    public Xml(String infId, Properties props, org.smslib.smsserver.SMSServer server, InterfaceTypes type) {
        super(infId, props, server, type);
        description = "Interface for xml input/output files";
        processedFiles = new HashMap();
        inDirectory = new File(getProperty("in") == null ? "." : getProperty("in"));
        outDirectory = new File(getProperty("out") == null ? "." : getProperty("out"));
        if (isInbound()) {
            if (!inDirectory.isDirectory() || !inDirectory.canWrite()) {
                throw new IllegalArgumentException(infId + ".in isn't a directory or isn't write-/readable!");
            }
            try {
                writeInboundDTD(inDirectory);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if (isOutbound()) {
            if (!outDirectory.isDirectory() || !outDirectory.canRead() || !outDirectory.canWrite()) {
                throw new IllegalArgumentException(infId + ".out isn't a directory or isn't write-/readable!");
            }
            outSentDirectory = new File(outDirectory, sOutSentDirectory);
            if (!outSentDirectory.isDirectory()) {
                if (!outSentDirectory.mkdir()) {
                    throw new IllegalArgumentException("Can't create directory '" + outSentDirectory);
                }
            }
            outFailedDirectory = new File(outDirectory, sOutFailedDirectory);
            if (!outFailedDirectory.isDirectory()) {
                if (!outFailedDirectory.mkdir()) {
                    throw new IllegalArgumentException("Can't create directory '" + outFailedDirectory);
                }
            }
            outBrokenDirectory = new File(outDirectory, sOutBrokenDirectory);
            if (!outBrokenDirectory.isDirectory()) {
                if (!outBrokenDirectory.mkdir()) {
                    throw new IllegalArgumentException("Can't create directory '" + outBrokenDirectory);
                }
            }
            try {
                writeOutboundDTD(outDirectory);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private void writeInboundDTD(File in) throws IOException {
        File dtd = new File(in, "smssvr_in.dtd");
        if (!dtd.exists()) {
            Writer w = new BufferedWriter(new FileWriter(dtd));
            String CRLF = System.getProperty("line.separator");
            w.write(" <!ELEMENT message (originator, text, receive_date)>");
            w.write(CRLF);
            w.write("   <!ATTLIST message");
            w.write(CRLF);
            w.write("       id		ID	#REQUIRED");
            w.write(CRLF);
            w.write("       gateway_id	CDATA	#REQUIRED");
            w.write(CRLF);
            w.write("       type		CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("       encoding	CDATA	#IMPLIED >");
            w.write(CRLF);
            w.write("     <!ELEMENT originator (#PCDATA)>");
            w.write(CRLF);
            w.write("     <!ELEMENT text (#PCDATA)>");
            w.write(CRLF);
            w.write("     <!ELEMENT receive_date (#PCDATA)>");
            w.write(CRLF);
            w.flush();
            w.close();
        }
    }

    private void writeOutboundDTD(File out) throws IOException {
        File dtd = new File(out, "smssvr_out.dtd");
        if (!dtd.exists()) {
            Writer w = new BufferedWriter(new FileWriter(dtd));
            String CRLF = System.getProperty("line.separator");
            w.write(" <!ELEMENT message (recipient, text, originator, create_date?)>");
            w.write(CRLF);
            w.write("   <!ATTLIST message ");
            w.write(CRLF);
            w.write("      id	 	 ID      #REQUIRED");
            w.write(CRLF);
            w.write("      gateway_id	 CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("      status         CDATA  \"U\" ");
            w.write(CRLF);
            w.write("      encoding       CDATA	\"7\"");
            w.write(CRLF);
            w.write("      priority       CDATA	\"N\"");
            w.write(CRLF);
            w.write("      ref_no         CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("      status_report  CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("      flash_sms      CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("      src_port       CDATA	#IMPLIED");
            w.write(CRLF);
            w.write("      dst_port       CDATA	#IMPLIED> ");
            w.write(CRLF);
            w.write("   <!ELEMENT recipient (#PCDATA)>");
            w.write(CRLF);
            w.write("   <!ELEMENT text (#PCDATA)>");
            w.write(CRLF);
            w.write("   <!ELEMENT create_date (#PCDATA)>");
            w.write(CRLF);
            w.write("   <!ELEMENT originator (#PCDATA)>");
            w.write(CRLF);
            w.flush();
            w.close();
        }
    }

    /**
	 * Adds the given InboundMessage to the given document.
	 * 
	 * @param xmldoc
	 *            The document in which the message is written
	 * @param m
	 *            The message to add to the docment
	 */
    private void addMessageToDocument(Document xmldoc, org.smslib.InboundMessage m) {
        Element message = null;
        Element originatorElement = null;
        Node originatorNode = null;
        Element textElement = null;
        Node textNode = null;
        Element timeElement = null;
        Node timeNode = null;
        message = xmldoc.createElement("message");
        message.setAttribute("id", m.getId());
        message.setAttribute("gateway_id", m.getGatewayId());
        String type = null;
        if (m.getType() == org.smslib.MessageTypes.INBOUND) {
            type = "I";
        } else if (m.getType() == org.smslib.MessageTypes.STATUSREPORT) {
            type = "S";
        }
        if (type != null) {
            message.setAttributeNS(null, "type", type);
        }
        String encoding = null;
        if (m.getEncoding() == org.smslib.MessageEncodings.ENC7BIT) {
            encoding = "7";
        } else if (m.getEncoding() == org.smslib.MessageEncodings.ENC8BIT) {
            encoding = "8";
        } else if (m.getEncoding() == org.smslib.MessageEncodings.ENCUCS2) {
            encoding = "U";
        }
        if (encoding != null) {
            message.setAttributeNS(null, "encoding", encoding);
        }
        originatorNode = xmldoc.createTextNode(m.getOriginator());
        originatorElement = xmldoc.createElementNS(null, "originator");
        originatorElement.appendChild(originatorNode);
        textNode = xmldoc.createTextNode(m.getText());
        textElement = xmldoc.createElementNS(null, "text");
        textElement.appendChild(textNode);
        timeNode = xmldoc.createTextNode(getDateAsISO8601(m.getDate()));
        timeElement = xmldoc.createElementNS(null, "receive_date");
        timeElement.appendChild(timeNode);
        message.appendChild(originatorElement);
        message.appendChild(textElement);
        message.appendChild(timeElement);
        xmldoc.appendChild(message);
    }

    public void CallReceived(String gtwId, String callerId) {
    }

    public List getMessagesToSend() throws Exception {
        List messageList = new ArrayList();
        File[] outFiles = outDirectory.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return (f.getAbsolutePath().endsWith(".xml") && !processedFiles.containsValue(f));
            }
        });
        for (int i = 0; i < outFiles.length; i++) {
            try {
                OutboundMessage msg = readDocument(outFiles[i]);
                if (msg == null) {
                    throw new IllegalArgumentException("Missing required fieldes!");
                }
                messageList.add(msg);
                processedFiles.put(msg, outFiles[i]);
            } catch (IllegalArgumentException e) {
                logWarn("Skipping outgoing file " + outFiles[i].getAbsolutePath() + ": File is not valid: " + e.getLocalizedMessage());
                File brokenFile = new File(outBrokenDirectory, outFiles[i].getName());
                if (!outFiles[i].renameTo(brokenFile)) {
                    System.out.println("Exists: " + outFiles[i].exists());
                    logError("Can't move " + outFiles[i] + " to " + brokenFile);
                }
            }
        }
        return messageList;
    }

    public void markMessage(org.smslib.OutboundMessage msg) throws Exception {
        if (msg == null) {
            return;
        }
        File f = (File) processedFiles.get(msg);
        File newF = null;
        if (msg.getMessageStatus() == org.smslib.MessageStatuses.SENT) {
            newF = new File(outSentDirectory, f.getName());
        } else if (msg.getMessageStatus() == org.smslib.MessageStatuses.FAILED) {
            newF = new File(outFailedDirectory, f.getName());
        }
        if (f.renameTo(newF)) {
            logInfo(f + " marked.");
        } else {
            logWarn("Can't move " + f + " to " + newF);
        }
        processedFiles.remove(msg);
    }

    public void MessagesReceived(List msgList) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        for (int i = 0; i < msgList.size(); i++) {
            org.smslib.InboundMessage msg = (org.smslib.InboundMessage) msgList.get(i);
            if ((msg.getType() == org.smslib.MessageTypes.INBOUND) || (msg.getType() == org.smslib.MessageTypes.STATUSREPORT)) {
                Document xmldoc = db.newDocument();
                addMessageToDocument(xmldoc, msg);
                String fileName = fileSdf.format(new java.util.Date()) + ".xml";
                logInfo("Writing inbound files to " + fileName);
                writeDocument(xmldoc, new File(inDirectory, fileName));
            }
        }
    }

    /**
	 * Tries to read from the given filename the outbound message
	 * 
	 * @param file
	 *            The file to read from
	 * @return Outbound message read form the file
	 * @throws IllegalArgumentExcpetion
	 *             Is thrown if there's something wrong with the content of the
	 *             file
	 * @throws IOExcpetion
	 *             Is throws if there's an I/O problem while reading the file
	 */
    private OutboundMessage readDocument(File file) throws IOException, IllegalArgumentException {
        Document xmldoc = null;
        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            dbfac.setValidating(true);
            DocumentBuilder db = dbfac.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException arg0) throws SAXException {
                    throw new IllegalArgumentException(arg0);
                }

                public void fatalError(SAXParseException arg0) throws SAXException {
                    throw new IllegalArgumentException(arg0);
                }

                public void warning(SAXParseException arg0) throws SAXException {
                    throw new IllegalArgumentException(arg0);
                }
            });
            xmldoc = db.parse(file);
            DocumentType outDoctype = xmldoc.getDoctype();
            if (!"message".equals(outDoctype.getName())) {
                throw new IllegalArgumentException("Wrong DOCTYPE - Have to be message!");
            }
            if (!"smsvr_out.dtd".equals(outDoctype.getSystemId())) {
                throw new IllegalArgumentException("Wrong SystemId in DOCTYPE - Have to be smsvr_out.dtd!");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        NodeList rnl = xmldoc.getElementsByTagName("message");
        if (rnl == null || rnl.getLength() != 1) {
            throw new IllegalArgumentException("Wrong root element or root element count!");
        }
        return readNode(rnl.item(0));
    }

    /**
	 * Reads a given node and tries to parse it
	 * 
	 * @param n
	 *            The node to parse
	 * @return An outboundmessage if the node was parsable or null
	 */
    private OutboundMessage readNode(Node n) {
        if ("message".equals(n.getNodeName())) {
            String recipient = null;
            String text = null;
            String originator = null;
            Element e = (Element) n;
            NodeList cnl = n.getChildNodes();
            for (int i = 0; i < cnl.getLength(); i++) {
                Node en = cnl.item(i);
                if ("recipient".equals(cnl.item(i).getNodeName())) {
                    recipient = en.getTextContent();
                } else if ("text".equals(cnl.item(i).getNodeName())) {
                    text = en.getTextContent();
                } else if ("originator".equals(cnl.item(i).getNodeName())) {
                    originator = en.getTextContent();
                }
            }
            OutboundMessage outMsg = new OutboundMessage(recipient, text);
            outMsg.setFrom(originator);
            outMsg.setId(e.getAttribute("id"));
            if (!"".equals(e.getAttribute("create_date"))) {
                outMsg.setDate(getISO8601AsDate(e.getAttribute("create_date")));
            }
            if (!"".equals(e.getAttribute("gateway_id"))) {
                outMsg.setGatewayId(e.getAttribute("gateway_id"));
            }
            String priority = e.getAttribute("priority");
            if ("L".equalsIgnoreCase(priority)) {
                outMsg.setPriority(org.smslib.MessagePriorities.LOW);
            } else if ("N".equalsIgnoreCase(priority)) {
                outMsg.setPriority(org.smslib.MessagePriorities.NORMAL);
            } else if ("H".equalsIgnoreCase(priority)) {
                outMsg.setPriority(org.smslib.MessagePriorities.HIGH);
            }
            String encoding = e.getAttribute("encoding");
            if ("7".equals(encoding)) {
                outMsg.setEncoding(org.smslib.MessageEncodings.ENC7BIT);
            } else if ("8".equals(encoding)) {
                outMsg.setEncoding(org.smslib.MessageEncodings.ENC8BIT);
            } else {
                outMsg.setEncoding(org.smslib.MessageEncodings.ENCUCS2);
            }
            if ("1".equals(e.getAttribute("status_report"))) {
                outMsg.setStatusReport(true);
            }
            if ("1".equals(e.getAttribute("flash_sms"))) {
                outMsg.setFlashSms(true);
            }
            if (!"".equals(e.getAttribute("src_port"))) {
                outMsg.setSrcPort(Integer.parseInt(e.getAttribute("src_port")));
            }
            if (!"".equals(e.getAttribute("dst_port"))) {
                outMsg.setDstPort(Integer.parseInt(e.getAttribute("dst_port")));
            }
            return outMsg;
        }
        return null;
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

    /**
	 * Writes the given document to the geiven filename. <br />
	 * The DTD smssvr_in.dtd is added, too.
	 * 
	 * @param doc
	 *            The document to serialize
	 * @param fileName
	 *            The file in which the document should be serialized
	 */
    private void writeDocument(Document doc, File fileName) throws IOException, TransformerFactoryConfigurationError, TransformerException {
        FileOutputStream fos = new FileOutputStream(fileName);
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        trans.setOutputProperty(OutputKeys.STANDALONE, "yes");
        trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "smssvr_in.dtd");
        StreamResult result = new StreamResult(fos);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        fos.close();
    }
}
