package sk.yw.azetclient.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import sk.yw.azetclient.Main;
import sk.yw.azetclient.azet.AzetMessage;
import sk.yw.azetclient.azet.AzetMessageBean;
import sk.yw.azetclient.model.Buddy;
import sk.yw.azetclient.model.Message;
import sk.yw.azetclient.model.MessageThread;
import sk.yw.azetclient.model.MessageBean;

/**
 *
 * @author error216
 */
public class MessageThreadStorage {

    private static final Logger logger = Logger.getLogger(MessageThreadStorage.class);

    private static final String SCHEMA_SYSTEM_RESOURCE_PATH = "sk/yw/azetclient/storage/MessageThread.xsd";

    private static final DateFormat SCHEMA_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String DOCUMENT_NAMESPACE_URI = "http://azetclient.yw.sk/ns/MessageThread";

    private static final String AZET_CLIENT_MESSAGE_TYPE = "sk.yw.azetclient.azet.AzetMessage";

    private static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    static {
        SCHEMA_DATE_FORMAT.setCalendar(new GregorianCalendar(TimeZone.getTimeZone("GMT+00"), Main.SLOVAK_LOCALE));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(new StreamSource(MessageThreadStorage.class.getClassLoader().getResource(SCHEMA_SYSTEM_RESOURCE_PATH).openStream()));
            documentBuilderFactory.setSchema(schema);
        } catch (SAXException ex) {
            logger.error("Unable to parse: " + SCHEMA_SYSTEM_RESOURCE_PATH, ex);
        } catch (IOException ex) {
            logger.error("Unable to read: " + SCHEMA_SYSTEM_RESOURCE_PATH, ex);
        }
    }

    private static class MessageThreadBean {

        private int id;

        private String name;

        private Buddy initialSender;

        private Buddy initialReceiver;

        private String messageType;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Buddy getInitialSender() {
            return initialSender;
        }

        public void setInitialSender(Buddy initialSender) {
            this.initialSender = initialSender;
        }

        public Buddy getInitialReceiver() {
            return initialReceiver;
        }

        public void setInitialReceiver(Buddy initialReceiver) {
            this.initialReceiver = initialReceiver;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }

    /**
     * Creates new instance of MessageThreadStorage.
     */
    public MessageThreadStorage() {
    }

    /**
     * Creates message thread containing all the contents of given input stream.
     * Input stream must be valid against
     * sk/yw/azetclient/storage/MessageThread.xsd.
     * 
     * Given input stream is closed thereafter.
     * 
     * @param  in  input stream to read from
     * @return  full message thread
     * @throws  sk.yw.azetclient.StorageException  if exception occurs while
     * parsing the stream
     */
    public MessageThread loadFull(InputStream in) throws StorageException {
        logger.debug("Calling method loadFull");
        MessageThread thread = null;
        try {
            DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
            Document document = parser.parse(in);
            Node node = document.getDocumentElement().getFirstChild();
            MessageThreadBean bean = loadThreadInfo(node);
            thread = new MessageThread(bean.getId());
            thread.setName(bean.getName());
            while ((node = findElement(node)) != null) {
                if ("AzetMessage".equals(node.getNodeName())) {
                    AzetMessageBean message = loadAzetMessage(node);
                    if (!thread.isAddPossible(message)) {
                        logger.debug("Message " + message.getId() + " is not reply to previous message.");
                    }
                    thread.forceAddMessage(message);
                }
                node = node.getNextSibling();
            }
        } catch (ParserConfigurationException ex) {
            throw new StorageException("Bad parser configuration.", ex);
        } catch (SAXException ex) {
            throw new StorageException("Unable to parse stream.", ex);
        } catch (IOException ex) {
            throw new StorageException("Unable to read from stream", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new StorageException("Unable to close the stream", ex);
            }
        }
        return thread;
    }

    /**
     * Creates message thread containing last two messages from the given input
     * stream. This is useful when checking if add of message is possible. Input
     * stream must be valid against sk/yw/azetclient/storage/MessageThread.xsd.
     * 
     * Given input stream is closed thereafter.
     * 
     * @param  in  input stream to read from
     * @return  minimal message thread
     * @throws  sk.yw.azetclient.StorageException  if exception occurs while
     * parsing the stream
     */
    public MessageThread loadMinimal(InputStream in) throws StorageException {
        logger.debug("Calling method loadMinimal");
        MessageThread thread = null;
        try {
            DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
            Document document = parser.parse(in);
            Node node = document.getDocumentElement().getFirstChild();
            MessageThreadBean info = loadThreadInfo(node);
            thread = new MessageThread(info.getId());
            thread.setName(info.getName());
            Node node1 = findElement(node);
            Node node2 = findElement(node1.getNextSibling());
            node = node2.getNextSibling();
            while ((node = findElement(node)) != null) {
                node1 = node2;
                node2 = node;
                node = node2.getNextSibling();
            }
            if ("AzetMessage".equals(node1.getNodeName())) {
                thread.forceAddMessage(loadAzetMessage(node1));
            }
            if ("AzetMessage".equals(node2.getNodeName())) {
                thread.forceAddMessage(loadAzetMessage(node2));
            }
        } catch (ParserConfigurationException ex) {
            throw new StorageException("Bad parser configuration.", ex);
        } catch (SAXException ex) {
            throw new StorageException("Unable to parse stream.", ex);
        } catch (IOException ex) {
            throw new StorageException("Unable to read from stream", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                throw new StorageException("Unable to close the stream", ex);
            }
        }
        return thread;
    }

    private static MessageThreadBean loadThreadInfo(Node node) {
        MessageThreadBean info = new MessageThreadBean();
        node = findElement(node, "Id");
        info.setId(convertToInt(findText(node)));
        node = node.getNextSibling();
        node = findElement(node, "Name");
        info.setName(findText(node));
        node = node.getNextSibling();
        return info;
    }

    private static AzetMessageBean loadAzetMessage(Node node) {
        Node subnode = node.getFirstChild();
        AzetMessageBean message = new AzetMessageBean();
        loadMessage(message, subnode);
        while ((subnode = findElement(subnode)) != null) {
            if ("Sign".equals(subnode.getNodeName())) {
                message.setSign(findText(subnode));
            } else if ("TimeOfPrevious".equals(subnode.getNodeName())) {
                message.setTimeOfPrevious(parseDate(findText(subnode)));
            } else if ("TextOfPrevious".equals(subnode.getNodeName())) {
                message.setTextOfPrevious(findText(subnode));
            } else if ("LastFragment".equals(subnode.getNodeName())) {
                message.setLastFragment(findText(subnode));
            }
            subnode = subnode.getNextSibling();
        }
        return message;
    }

    private static MessageBean loadMessage(MessageBean message, Node node) {
        node = findElement(node, "Id");
        message.setId(convertToLong(findText(node)));
        node = node.getNextSibling();
        node = findElement(node, "Sender");
        message.setSender(loadBuddy(node));
        node = node.getNextSibling();
        node = findElement(node, "Receiver");
        message.setReceiver(loadBuddy(node));
        node = node.getNextSibling();
        node = findElement(node, "SendTime");
        message.setSendTime(parseDate(findText(node)));
        node = node.getNextSibling();
        node = findElement(node, "Content");
        message.setContent(findText(node));
        node = node.getNextSibling();
        return message;
    }

    private static Buddy loadBuddy(Node node) {
        Buddy buddy = new Buddy();
        buddy.setName(findText(node));
        return buddy;
    }

    private static Node findElement(Node node) {
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            logger.debug("Searching for element.");
            node = node.getNextSibling();
        }
        if (node == null) logger.debug("Unable to find element."); else logger.debug("Found element: " + node.getNodeName());
        return node;
    }

    private static Node findElement(Node node, String nodeName) {
        while (node != null && (node.getNodeType() != Node.ELEMENT_NODE || !nodeName.equals(node.getNodeName()))) {
            logger.debug("Searching for element: " + nodeName + "; current node name: " + node.getNodeName());
            node = node.getNextSibling();
        }
        if (node == null) logger.error("Unable to find element: " + nodeName); else logger.debug("Found element: " + nodeName);
        return node;
    }

    private static String findText(Node node) {
        Node subnode = node.getFirstChild();
        while (subnode != null && (subnode.getNodeType() != Node.TEXT_NODE)) {
            logger.debug("Searching for text; current node name: " + node.getNodeName());
            subnode = subnode.getNextSibling();
        }
        if (subnode != null) {
            logger.debug("Found text: '" + subnode.getNodeValue() + "'");
            return subnode.getNodeValue().trim();
        } else {
            logger.error("Unable to find text.");
            return null;
        }
    }

    private static int convertToInt(String integer) {
        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException ex) {
            logger.error("Invalid number format: " + integer, ex);
        }
        return 0;
    }

    private static long convertToLong(String longInteger) {
        try {
            return Long.parseLong(longInteger);
        } catch (NumberFormatException ex) {
            logger.error("Invalid number format: " + longInteger, ex);
        }
        return 0;
    }

    private static Date parseDate(String date) {
        try {
            return SCHEMA_DATE_FORMAT.parse(date);
        } catch (ParseException ex) {
            logger.error("Invalid date format: " + date, ex);
        }
        return null;
    }

    public void save(MessageThread thread, OutputStream out) throws StorageException {
        logger.debug("Calling method save");
        XMLWriter writer = new IndentingXMLWriter(out, "1.0", "UTF-8");
        try {
            writer.startDocument();
            writer.startElement(null, "MessageThread");
            writer.startElement(null, "Id");
            writer.characters(String.valueOf(thread.getId()));
            writer.endElement();
            writer.startElement(null, "Name");
            writer.characters(thread.getName());
            writer.endElement();
            for (Message message : thread.getMessages()) {
                if (message instanceof AzetMessage) {
                    writer.startElement(null, "AzetMessage");
                    saveMessage(writer, message);
                    AzetMessage azetMessage = (AzetMessage) message;
                    if (azetMessage.getSign() != null) {
                        writer.startElement(null, "Sign");
                        writer.characters(azetMessage.getSign());
                        writer.endElement();
                    }
                    if (azetMessage.getTimeOfPrevious() != null) {
                        writer.startElement(null, "TimeOfPrevious");
                        writer.characters(SCHEMA_DATE_FORMAT.format(azetMessage.getTimeOfPrevious()));
                        writer.endElement();
                    }
                    if (azetMessage.getTextOfPrevious() != null) {
                        writer.startElement(null, "TextOfPrevious");
                        writer.characters(azetMessage.getTextOfPrevious());
                        writer.endElement();
                    }
                    if (azetMessage.getLastFragment() != null) {
                        writer.startElement(null, "LastFragment");
                        writer.characters(azetMessage.getLastFragment());
                        writer.endElement();
                    }
                    writer.endElement();
                } else {
                    throw new UnsupportedOperationException("Message type not supported");
                }
            }
            writer.endElement();
            writer.endDocument();
        } catch (XMLWriterException ex) {
            throw new StorageException("Unable to store thread: " + thread.getName(), ex);
        } finally {
            try {
                writer.close();
            } catch (XMLWriterException ex) {
                throw new StorageException("Unable to store thread: " + thread.getName(), ex);
            }
        }
    }

    private static void saveMessage(XMLWriter writer, Message message) throws XMLWriterException {
        writer.startElement(null, "Id");
        writer.characters(String.valueOf(message.getId()));
        writer.endElement();
        writer.startElement(null, "Sender");
        writer.characters(message.getSender().getName());
        writer.endElement();
        writer.startElement(null, "Receiver");
        writer.characters(message.getReceiver().getName());
        writer.endElement();
        writer.startElement(null, "SendTime");
        writer.characters(SCHEMA_DATE_FORMAT.format(message.getSendTime()));
        writer.endElement();
        writer.startElement(null, "Content");
        writer.characters(message.getContent());
        writer.endElement();
    }
}
