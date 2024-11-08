package com.corratech.opensuite.zimbrahandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import com.corratech.opensuite.broker.utils.BrokerUtils;
import com.opensuite.bind.base.Content;
import com.opensuite.bind.event.Event;
import com.opensuite.bind.services.eventmanagement.ProcessEventRequest;
import com.zimbra.cs.extension.ExtensionHttpHandler;

public class OpensuiteHandler extends ExtensionHttpHandler {

    private static final String INV_ID = "id";

    private static final String NEW_ID = "newId";

    private static Marshaller marshaller;

    private static String zimbraSoapServiceURL;

    public static String getZimbraSoapServiceURL() {
        if (zimbraSoapServiceURL == null || zimbraSoapServiceURL.equals("")) {
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                zimbraSoapServiceURL = "http://" + ip + "/service/soap";
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return zimbraSoapServiceURL;
    }

    public OpensuiteHandler() {
    }

    private void modifyCreateAppointmentRequest(Node body, Node hdr) {
        try {
            SOAPMessage reply = sendSoap(getZimbraSoapServiceURL(), body.getFirstChild(), hdr.getFirstChild());
            String invId = "";
            if (reply != null) {
                invId = ((Element) reply.getSOAPBody().getFirstChild()).getAttribute("invId");
                System.out.println("Invite id=" + invId);
            } else System.out.println("No invite id");
            ((Element) body.getFirstChild()).setAttribute(INV_ID, invId);
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private void modifyModifyAppointmentRequest(Node body, Node hdr) {
        try {
            SOAPMessage reply = sendSoap(getZimbraSoapServiceURL(), body.getFirstChild(), hdr.getFirstChild());
            String invId = "";
            if (reply != null) {
                invId = ((Element) reply.getSOAPBody().getFirstChild()).getAttribute("invId");
            } else System.out.println("No new id");
            ((Element) body.getFirstChild()).setAttribute(NEW_ID, invId);
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            Element element = convert(req);
            Node body = element.getElementsByTagName("soap:Body").item(0);
            Node hdr = element.getElementsByTagName("soap:Header").item(0);
            if (((Element) body).getElementsByTagName("CreateAppointmentRequest").item(0) != null) {
                modifyCreateAppointmentRequest(body, hdr);
            } else if (((Element) body).getElementsByTagName("ModifyAppointmentRequest").item(0) != null) {
                modifyModifyAppointmentRequest(body, hdr);
            }
            sendNotify(body.getFirstChild());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    }

    private static Document getDocument(Object obj) throws JAXBException, ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        getMarshaller().marshal(obj, doc);
        return doc;
    }

    private Element convert(HttpServletRequest req) throws IOException, ParserConfigurationException, SAXException {
        int len = req.getContentLength();
        byte[] buffer;
        if (len == -1) {
            buffer = readUntilEOF(req.getInputStream());
        } else {
            buffer = new byte[len];
            readFully(req.getInputStream(), buffer, 0, len);
        }
        InputStream in = new ByteArrayInputStream(buffer);
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        docBuilderFactory.setValidating(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);
        return doc.getDocumentElement();
    }

    private String getOrganizer(Element element) {
        return element.getElementsByTagName("or") != null ? element.getElementsByTagName("or").item(0).getAttributes().getNamedItem("a").getNodeValue() : null;
    }

    private SOAPMessage sendNotify(Node payLoad) throws MalformedURLException, IOException, SOAPException, JAXBException, ParserConfigurationException, SAXException {
        ProcessEventRequest eventRequest = new ProcessEventRequest();
        eventRequest.setBCName("Zimbra");
        Event event = new Event();
        event.setTS(System.currentTimeMillis());
        event.setGeneratorBCName("Zimbra");
        Content content = new Content();
        event.setContent(content);
        eventRequest.setUserLogin(getOrganizer((Element) payLoad));
        eventRequest.setEvent(event);
        Notify notify = new Notify();
        NotificationMessageHolderType holderType = new NotificationMessageHolderType();
        Message message = new Message();
        message.setAny(eventRequest);
        holderType.setMessage(message);
        TopicExpressionType topic = new TopicExpressionType();
        topic.setDialect("http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple");
        topic.getContent().add("CALENDARSSYNC");
        holderType.setTopic(topic);
        notify.getNotificationMessage().add(holderType);
        Document notifyDoc = getDocument(notify);
        Node tmp = notifyDoc.importNode(payLoad, true);
        notifyDoc.getElementsByTagNameNS("http://opensuite.com/bind/event", "content").item(0).appendChild(tmp);
        return sendSoap(BrokerUtils.getProperties().getProperty("BROKER.ENDPOINT_WSDL"), notifyDoc.getDocumentElement(), null);
    }

    private SOAPMessage sendSoap(String url, Node bodyContent, Node headerContent) throws SOAPException, JAXBException, ParserConfigurationException, IOException, SAXException {
        SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = soapConnFactory.createConnection();
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        if (headerContent != null) {
            SOAPHeader soapHeader = soapMessage.getSOAPHeader();
            Node tmp = soapHeader.getOwnerDocument().importNode(headerContent, true);
            soapHeader.appendChild(tmp);
        }
        SOAPBody soapBody = envelope.getBody();
        Node tmp = soapBody.getOwnerDocument().importNode(bodyContent, true);
        envelope.getBody().appendChild(tmp);
        soapMessage.saveChanges();
        System.out.println("\nREQUEST:\n");
        soapMessage.writeTo(System.out);
        System.out.println();
        System.out.println("SOAP Service URL: " + url);
        SOAPMessage reply = connection.call(soapMessage, url);
        System.out.println("\nRESPONSE:\n");
        if (reply != null) reply.writeTo(System.out);
        System.out.println();
        return reply;
    }

    /**
	 * read until EOF is reached
	 */
    protected byte[] readUntilEOF(InputStream input) throws IOException {
        final int SIZE = 2048;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(SIZE);
        byte[] buffer = new byte[SIZE];
        int n = 0;
        while ((n = input.read(buffer, 0, SIZE)) > 0) baos.write(buffer, 0, n);
        return baos.toByteArray();
    }

    protected void readFully(InputStream in, byte b[], int off, int len) throws IOException {
        if (len < 0) throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) throw new java.io.EOFException();
            n += count;
        }
    }

    private static Marshaller getMarshaller() throws JAXBException {
        if (marshaller == null) {
            marshaller = JAXBContext.newInstance("org.oasis_open.docs.wsn.b_2:com.opensuite.bind.services.eventmanagement", OpensuiteHandler.class.getClassLoader()).createMarshaller();
        }
        return marshaller;
    }
}
