package eis.examples.acconnector2010;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eis.iilang.Percept;
import eis.iilang.Identifier;
import eis.iilang.Numeral;

/**
 * Represents a single connection to a MASSim-server. Each controllable entity (cowboy)
 * can have one connection.
 * 
 * @author tristanbehrens
 *
 */
public class Connection extends Socket implements Runnable {

    protected static DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory.newInstance();

    protected static TransformerFactory transformerfactory = TransformerFactory.newInstance();

    private InputStream inputstream = null;

    private OutputStream outputstream = null;

    /** The username of the entity. */
    private String username = null;

    /** The password. */
    private String password = null;

    /** A listener that is to be informed once there is an incoming message. */
    private ConnectionListener listener = null;

    /** Stores whether the connection is executing. Executing means listening for messages. */
    private boolean executing = true;

    /** Contains the last action-id. It is sent if there is an action. Used for blocking action-methods. */
    private String actionId = null;

    /** 
	 * Establishes a socket-connection to the MASSim-server.
	 * 
	 * @param listener is the object that is to be provided with incoming messages.
	 * @param host is the hostname (URL or ip-address) of the MASSim-server.
	 * @param port is the port of the MASSim-Server.
	 * @throws UnknownHostException is thrown if the host is unknown.
	 * @throws IOException is thrown if the connection cannot be established.
	 */
    public Connection(ConnectionListener listener, String host, int port) throws UnknownHostException, IOException {
        super(host, port);
        this.listener = listener;
        inputstream = this.getInputStream();
        outputstream = this.getOutputStream();
    }

    /**
	 * Transforms an XML-document to a string.
	 * 
	 * @param node is an XML-node.
	 * @return the String representation.
	 */
    public static String xmlToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Sends an authentication-message to the server and waits for the reply.
	 * 
	 * @param username the username of the cowboy.
	 * @param password the password of the cowboy.
	 * @return true on success.
	 */
    public boolean authenticate(String username, String password) {
        Document doc = null;
        try {
            doc = documentbuilderfactory.newDocumentBuilder().newDocument();
            Element root = doc.createElement("message");
            root.setAttribute("type", "auth-request");
            doc.appendChild(root);
            Element auth = doc.createElement("authentication");
            auth.setAttribute("username", username);
            auth.setAttribute("password", password);
            root.appendChild(auth);
        } catch (ParserConfigurationException e) {
            System.err.println("unable to create new document for authentication.");
            return false;
        }
        try {
            sendDocument(doc);
        } catch (IOException e1) {
            System.out.println("Sending document failed.");
            return false;
        }
        Document reply;
        try {
            reply = receiveDocument();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Element root = reply.getDocumentElement();
        if (root == null) return false;
        if (!root.getAttribute("type").equalsIgnoreCase("auth-response")) return false;
        NodeList nl = root.getChildNodes();
        Element authresult = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Element.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase("authentication")) {
                authresult = (Element) n;
                break;
            }
        }
        if (!authresult.getAttribute("result").equalsIgnoreCase("ok")) return false;
        this.username = username;
        this.password = password;
        return true;
    }

    /**
	 * Sends off an action-message.
	 * 
	 * @param action the action to be sent.
	 * @return true on success.
	 */
    public boolean act(String action) {
        Document doc = null;
        while (actionId == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }
        }
        try {
            doc = documentbuilderfactory.newDocumentBuilder().newDocument();
            Element root = doc.createElement("message");
            root.setAttribute("type", "action");
            doc.appendChild(root);
            Element auth = doc.createElement("action");
            auth.setAttribute("id", actionId);
            auth.setAttribute("type", action);
            root.appendChild(auth);
        } catch (ParserConfigurationException e) {
            System.err.println("unable to create new document for authentication.");
            return false;
        }
        try {
            sendDocument(doc);
        } catch (IOException e) {
            System.out.println("Sending action-message failed");
            return false;
        }
        actionId = null;
        return true;
    }

    /** 
	 * Receives a document
	 * 
	 * @return the received document.
	 * @throws IOException is thrown if reception failed.
	 */
    public Document receiveDocument() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read = inputstream.read();
        while (read != 0) {
            if (read == -1) {
                throw new IOException();
            }
            buffer.write(read);
            try {
                read = inputstream.read();
            } catch (IOException e) {
                throw new IOException("Reading from input-stream failed.");
            }
        }
        byte[] raw = buffer.toByteArray();
        Document doc;
        try {
            doc = documentbuilderfactory.newDocumentBuilder().parse(new ByteArrayInputStream(raw));
        } catch (SAXException e) {
            throw new IOException("Error parsing");
        } catch (IOException e) {
            throw new IOException("Error parsing");
        } catch (ParserConfigurationException e) {
            throw new IOException("Error parsing");
        }
        return doc;
    }

    /** 
	 * Sends a document.
	 * 
	 * @param doc is the document to be sent.
	 * @throws IOException is thrown if the document could not be sent.s
	 */
    private void sendDocument(Document doc) throws IOException {
        try {
            transformerfactory.newTransformer().transform(new DOMSource(doc), new StreamResult(outputstream));
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            transformerfactory.newTransformer().transform(new DOMSource(doc), new StreamResult(temp));
            outputstream.write(0);
            outputstream.flush();
        } catch (TransformerConfigurationException e) {
            throw new IOException("transformer config error");
        } catch (TransformerException e) {
            throw new IOException("transformer error");
        } catch (IOException e) {
            throw new IOException();
        }
    }

    /**
	 * Listen for incoming messages.
	 * 
	 */
    public void run() {
        while (executing) {
            try {
                Document doc = this.receiveDocument();
                LinkedList<Percept> percepts = evaluateDocument(doc);
                listener.handleMessage(this, percepts);
            } catch (IOException e) {
                LinkedList<Percept> percepts = new LinkedList<Percept>();
                percepts.add(new Percept("connectionLost"));
                listener.handleMessage(this, percepts);
                executing = false;
            }
        }
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Transforms a document into a data-container.
	 * 
	 * @param doc is the document to be transformed.
	 * @return the result of the transformation.
	 */
    private LinkedList<Percept> evaluateDocument(Document doc) {
        LinkedList<Percept> ret = new LinkedList<Percept>();
        Element root = doc.getDocumentElement();
        String type = root.getAttribute("type");
        if (type.equals("sim-start")) {
            NodeList nodes = root.getChildNodes();
            Node sim = null;
            for (int a = 0; a < nodes.getLength(); a++) {
                if (nodes.item(a).getNodeName().equals("simulation")) sim = nodes.item(a);
            }
            assert sim != null;
            NamedNodeMap attributes = sim.getAttributes();
            Percept percept = null;
            percept = new Percept("simStart");
            ret.add(percept);
            percept = new Percept("corralPos", new Numeral(new Integer(attributes.getNamedItem("corralx0").getNodeValue())), new Numeral(new Integer(attributes.getNamedItem("corraly0").getNodeValue())), new Numeral(new Integer(attributes.getNamedItem("corralx1").getNodeValue())), new Numeral(new Integer(attributes.getNamedItem("corraly1").getNodeValue())));
            ret.add(percept);
            percept = new Percept("gridSize", new Numeral(new Integer(attributes.getNamedItem("gsizex").getNodeValue())), new Numeral(new Integer(attributes.getNamedItem("gsizey").getNodeValue())));
            ret.add(percept);
            percept = new Percept("simId", new Identifier(attributes.getNamedItem("id").getNodeValue()));
            ret.add(percept);
            percept = new Percept("lineOfSight", new Numeral(new Integer(attributes.getNamedItem("lineOfSight").getNodeValue())));
            ret.add(percept);
            percept = new Percept("opponent", new Identifier(attributes.getNamedItem("opponent").getNodeValue()));
            ret.add(percept);
            percept = new Percept("steps", new Numeral(new Integer(attributes.getNamedItem("steps").getNodeValue())));
            ret.add(percept);
            return ret;
        } else if (type.equals("sim-end")) {
            NodeList children = root.getChildNodes();
            Node result = null;
            for (int a = 0; a < children.getLength(); a++) {
                if (children.item(a).getNodeName().equals("sim-result")) result = children.item(a);
            }
            assert result != null;
            NamedNodeMap attributes = result.getAttributes();
            Percept percept = null;
            percept = new Percept("simEnd");
            ret.add(percept);
            percept = new Percept("result", new Identifier(attributes.getNamedItem("result").getNodeValue()));
            ret.add(percept);
            percept = new Percept("averageScore", new Numeral(new Integer(attributes.getNamedItem("averageScore").getNodeValue())));
            ret.add(percept);
            return ret;
        } else if (type.equals("bye")) {
            ret.add(new Percept("bye"));
            return ret;
        } else if (type.equals("request-action")) {
            NodeList children = root.getChildNodes();
            Node perceptNode = null;
            for (int a = 0; a < children.getLength(); a++) {
                if (children.item(a).getNodeName().equals("perception")) perceptNode = children.item(a);
            }
            assert perceptNode != null;
            NamedNodeMap attributes = perceptNode.getAttributes();
            actionId = attributes.getNamedItem("id").getNodeValue();
            int step = new Integer(attributes.getNamedItem("step").getNodeValue());
            NodeList cellNodes = perceptNode.getChildNodes();
            for (int a = 0; a < cellNodes.getLength(); a++) {
                if (cellNodes.item(a).getNodeName().equals("cell")) {
                    Node cellNode = cellNodes.item(a);
                    NamedNodeMap attributesCell = cellNode.getAttributes();
                    String item = null;
                    for (int b = 0; b < cellNode.getChildNodes().getLength(); b++) {
                        Node nab = cellNodes.item(a).getChildNodes().item(b);
                        String str = nab.getNodeName();
                        if (str.equals("empty")) item = "empty"; else if (str.equals("agent")) {
                            String agentType = nab.getAttributes().getNamedItem("type").getNodeValue();
                            item = "agent" + agentType;
                        } else if (str.equals("corral")) {
                            String corralType = nab.getAttributes().getNamedItem("type").getNodeValue();
                            item = "corral" + corralType;
                        } else if (str.equals("obstacle")) {
                            item = "obstacle";
                        } else if (str.equals("switch")) {
                            item = "switch";
                        } else if (str.equals("fence")) {
                            String fenceOpen = nab.getAttributes().getNamedItem("open").getNodeValue();
                            if (fenceOpen.equals("true")) item = "fenceopen"; else item = "fenceclosed";
                        } else if (str.equals("cow")) {
                            item = "cow";
                        } else if (str.equals("#text")) {
                        } else assert false : "Unknown " + str;
                    }
                    assert type != null;
                    Percept cell = new Percept("cell", new Numeral(new Integer(attributesCell.getNamedItem("x").getNodeValue())), new Numeral(new Integer(attributesCell.getNamedItem("y").getNodeValue())), new Identifier(item), new Numeral(step));
                    ret.add(cell);
                }
            }
            Percept percept = null;
            percept = new Percept("pos", new Numeral(new Integer(attributes.getNamedItem("posx").getNodeValue())), new Numeral(new Integer(attributes.getNamedItem("posy").getNodeValue())), new Numeral(step));
            ret.add(percept);
            percept = new Percept("cowsInCorral", new Numeral(new Integer(attributes.getNamedItem("cowsInCorral").getNodeValue())), new Numeral(step));
            ret.add(percept);
            percept = new Percept("currentStep", new Numeral(new Integer(attributes.getNamedItem("step").getNodeValue())));
            ret.add(percept);
            return ret;
        } else {
            assert false : "Unknown type " + type;
        }
        return null;
    }

    /**
	 * Returns the state of the execution.
	 * 
	 * @return the state of the execution.
	 */
    public boolean isExecuting() {
        return executing;
    }

    /**
	 * Closes the connection.
	 */
    public void close() throws IOException {
        executing = false;
        super.close();
    }
}
