package org.tagbox.engine.action.soap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.tagbox.engine.AttachmentFactory;
import org.tagbox.engine.Component;
import org.tagbox.engine.TagBoxConfigurationException;
import org.tagbox.engine.TagBoxException;
import org.tagbox.engine.TagBoxProcessingException;
import org.tagbox.engine.TagEnvironment;
import org.tagbox.engine.action.EvaluateAction;
import org.tagbox.soap.Connection;
import org.tagbox.soap.ConnectionFactory;
import org.tagbox.soap.Message;
import org.tagbox.soap.MessageFactory;
import org.tagbox.soap.MessagePart;
import org.tagbox.soap.SoapException;
import org.tagbox.util.Attachment;
import org.tagbox.util.Log;
import org.tagbox.xml.NodeFinder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

public class SendSoapMessage extends EvaluateAction {

    protected static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    protected NodeFinder soapfinder;

    /** profile is extracted from the configuration */
    protected String profile;

    private AttachmentFactory attachmentFactory;

    private ConnectionFactory connectionFactory;

    protected static String SIGN_USING_XWS_SEC = "xws-security";

    protected static String SIGN_USING_WS4J = "ws4j";

    protected static String SECURE_METHOD = SIGN_USING_XWS_SEC;

    protected class Destination {

        private String url;

        public Destination(String url) {
            this.url = url;
        }

        public String getDestination() {
            return url;
        }
    }

    protected class HeaderInfo {

        private String name;

        private String value;

        public HeaderInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    protected class AttachmentInfo {

        private File file;

        private Object content;

        private String contentType;

        private String location;

        public AttachmentInfo(String href, String contentType, String location) throws IOException {
            this.contentType = contentType;
            this.location = location;
            if (contentType == null) {
                file = new File(href);
            } else if (contentType.equalsIgnoreCase("text/xml")) {
                content = new StreamSource(href);
            } else if (contentType.equalsIgnoreCase("text/plain")) {
                Reader reader = new BufferedReader(new FileReader(href));
                StringWriter writer = new StringWriter();
                while (reader.ready()) {
                    writer.write(reader.read());
                }
                content = writer.toString();
            } else {
                content = new FileInputStream(href);
            }
        }

        public AttachmentInfo(Node attachment, String location) {
            content = new DOMSource(attachment);
            this.contentType = "text/xml";
            this.location = location;
        }

        public File getFile() {
            return file;
        }

        public Object getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        public String getLocation() {
            return location;
        }
    }

    public SendSoapMessage() {
        soapfinder = new NodeFinder(SOAP_ENVELOPE_NS);
    }

    public void init(Component ctxt, Element config) throws TagBoxException {
        super.init(ctxt, config);
        NodeFinder finder = new NodeFinder(null);
        profile = finder.getElementValue(config, "profile");
        attachmentFactory = ctxt.getConfiguration().getAttachmentFactory();
        try {
            connectionFactory = ConnectionFactory.getInstance();
        } catch (SoapException exc) {
            throw new TagBoxConfigurationException("failed to create SOAP connection or message factory", exc);
        }
    }

    protected Destination createDestination(Element e, TagEnvironment env) throws TagBoxException, SoapException {
        String url = e.getAttribute("url");
        if (url.equals("")) throw new TagBoxProcessingException(e, "missing 'url' attribute");
        url = evaluate(url, env, e);
        return new Destination(url);
    }

    protected Connection createConnection(Element e, TagEnvironment env) throws TagBoxException, SoapException {
        return connectionFactory.createConnection();
    }

    protected Message createMessage(Connection connection, Element e, TagEnvironment env) throws TagBoxException, SoapException {
        MessageFactory messageFactory = connection.createMessageFactory(profile);
        Message msg = messageFactory.createMessage();
        Element msgElement = soapfinder.getElement(e, "Envelope");
        msg.setMessageBody(msgElement);
        return msg;
    }

    protected List getAttachments(Element e, TagEnvironment env) throws TagBoxException {
        NodeIterator it = env.getDescender().getElements(e, "attachment");
        List attachmentList = new ArrayList();
        for (Element attachment = (Element) it.nextNode(); attachment != null; attachment = (Element) it.nextNode()) {
            String location = attachment.getAttribute("location");
            if (location.equals("")) throw new TagBoxProcessingException(attachment, "SOAP attachment missing required attribute: location");
            location = evaluate(location, env, attachment);
            AttachmentInfo info;
            String file = attachment.getAttribute("href");
            if (file.equals("")) {
                Node frag = e.getOwnerDocument().createDocumentFragment();
                for (Node n = attachment.getFirstChild(); n != null; n = n.getNextSibling()) frag.appendChild(n.cloneNode(true));
                info = new AttachmentInfo(frag, location);
            } else {
                file = evaluate(file, env, attachment);
                String contentType = attachment.getAttribute("content-type");
                if (contentType.equals("")) contentType = null; else contentType = evaluate(contentType, env, attachment);
                try {
                    info = new AttachmentInfo(file, contentType, location);
                } catch (IOException exc) {
                    throw new TagBoxProcessingException(e, "attachment error", exc);
                }
            }
            attachmentList.add(info);
            e.removeChild(attachment);
        }
        return attachmentList;
    }

    protected List getHeaders(Element e, TagEnvironment env) throws TagBoxException {
        NodeIterator it = env.getDescender().getElements(e, "mime-header");
        List headers = new ArrayList();
        for (Element header = (Element) it.nextNode(); header != null; header = (Element) it.nextNode()) {
            String name = header.getAttribute("name");
            name = evaluate(name, env, header);
            String value = header.getAttribute("value");
            value = evaluate(value, env, header);
            headers.add(new HeaderInfo(name, value));
            e.removeChild(header);
        }
        return headers;
    }

    public void process(Element e, TagEnvironment env) throws TagBoxException {
        env.getDescender().descend(e, env);
        List attachments = getAttachments(e, env);
        List headers = getHeaders(e, env);
        Document doc = e.getOwnerDocument();
        Node result = doc.createDocumentFragment();
        try {
            String secureMethod = e.getAttribute("secure-method");
            if (secureMethod != null && secureMethod.equals(SIGN_USING_WS4J)) {
                SECURE_METHOD = secureMethod;
            }
            Destination dest = createDestination(e, env);
            Connection connection = createConnection(e, env);
            Message msg = createMessage(connection, e, env);
            for (Iterator nit = attachments.iterator(); nit.hasNext(); ) {
                AttachmentInfo attachment = (AttachmentInfo) nit.next();
                MessagePart part;
                if (attachment.getContent() == null) part = msg.createAttachment(attachment.getFile()); else part = msg.createAttachment(attachment.getContent(), attachment.getContentType());
                msg.setAttachment(attachment.getLocation(), part);
            }
            for (Iterator nit = headers.iterator(); nit.hasNext(); ) {
                HeaderInfo header = (HeaderInfo) nit.next();
                msg.setHeader(header.getName(), header.getValue());
            }
            Message response = send(msg, connection, dest);
            if (response != null) {
                Node soapRes = response.getMessageBody().getContentAsXML();
                if (soapRes != null) result.appendChild(doc.importNode(soapRes, true));
                Iterator nit = response.getAttachmentLocations().iterator();
                while (nit.hasNext()) {
                    String location = (String) nit.next();
                    MessagePart part = response.getAttachment(location);
                    Attachment attachment;
                    try {
                        attachment = attachmentFactory.saveAttachment(part);
                    } catch (IOException exc) {
                        throw new TagBoxProcessingException(e, "failed to save SOAP response attachment", exc);
                    }
                    env.setAttachment(attachment.getName(), attachment);
                    Log.trace("saved attachment: " + attachment);
                }
            }
        } catch (SoapException exc) {
            throw new TagBoxProcessingException(e, "failed to send SOAP message", exc);
        }
        e.getParentNode().replaceChild(result, e);
    }

    protected Message send(Message msg, Connection connection, Destination dest) throws SoapException, TagBoxException {
        String url = dest.getDestination();
        Log.trace("sending SOAP message to " + url);
        return connection.send(msg, url);
    }

    protected String getSecureMethod() {
        return SECURE_METHOD;
    }
}
