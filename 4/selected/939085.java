package freemail.documents;

import java.io.*;
import java.math.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import freemail.*;
import freemail.util.*;

/**
 * <p>The class encapsulating a Freemail mailstore.  A mailstore contains the
 * user's SSK and encryption keys along with a record of the channels in use by
 * the user and messages in those channels.</p>
 *
 * @see Channel
 *
 * @author Adam Thomason
 **/
public class Mailstore extends FreemailDocument {

    /**
     * The document version created and processed by this class.
     **/
    private static final double LATEST_VERSION = 0.1;

    /**
     * Creates a new mailstore.
     *
     * @param alias the human-friendly name of the user.
     * @param privateSSKKey the private SSK key for the user.
     * @param publicSSKKey the public SSK key for the user.
     * @param requesteePrivateDHKey the inbound private Diffie-Hellman key for the
     *     user.
     * @param requesteePublicDHKey the private Diffie-Hellman key for the user.
     * @param requesteeDHParamSpec the Diffie-Hellman parameters used to generate the
     *     private key.
     * @param dynamicWindowPointerAddress the Freenet key where the dynamic
     *     window prefix is published and updated.
     * @param dynamicWindowPrefix the subspace prefix for the dynamic-window
     *     channel initiation request keys.
     * @param dynamicWindowIndex the initial slot index in the dynamic window
     *     subspace.
     **/
    public Mailstore(String alias, String privateSSKKey, String publicSSKKey, PrivateKey requesteePrivateDHKey, PublicKey requesteePublicDHKey, DHParameterSpec requesteeDHParamSpec, String dynamicWindowPointerAddress, String dynamicWindowPrefix, int dynamicWindowIndex) {
        version = LATEST_VERSION;
        this.alias = alias;
        this.privateSSKKey = privateSSKKey;
        this.publicSSKKey = publicSSKKey;
        this.requesteePrivateDHKey = requesteePrivateDHKey;
        this.requesteePublicDHKey = requesteePublicDHKey;
        this.requesteeDHParamSpec = requesteeDHParamSpec;
        lastFixedRequestIndex = 0;
        this.dynamicWindowPointerAddress = dynamicWindowPointerAddress;
        this.dynamicWindowPrefix = dynamicWindowPrefix;
        lastDynamicRequestIndex = dynamicWindowIndex;
        channels = Collections.synchronizedList(new LinkedList());
        requests = Collections.synchronizedList(new LinkedList());
        messages = Collections.synchronizedList(new LinkedList());
    }

    /**
     * Constructs a mailstore from the specified XML string.
     *
     * @param xml the XML to parse.
     * @exception Exception if a failure occurs during the parsing.
     **/
    public Mailstore(String xml) throws DocumentException {
        super(xml);
    }

    /**
     * Constructs a mailstore from an XML disk file.
     *
     * @param file the file to parse.
     * @exception Exception if a failure occurs during the parsing.
     **/
    public Mailstore(File file) throws DocumentException {
        super(file);
    }

    /**
     * Parses a mailstore from the XML element.
     *
     * @exception DocumentException if the parse fails or a required field is
     *     missing.
     **/
    protected void decompose() throws DocumentException {
        if (!getRootTitle().equals("mailstore")) {
            throw new DocumentException("Document is not an mailstore");
        }
        version = doubleValue(getSingletonSubelement("version"));
        if (version > LATEST_VERSION) {
            throw new DocumentException("Unknown mailstore version: (" + version + " > " + LATEST_VERSION + ")");
        }
        alias = getSingletonSubelement("alias");
        privateSSKKey = getSingletonSubelement("privateSSKKey");
        publicSSKKey = getSingletonSubelement("publicSSKKey");
        String encodedRequesteePrivateDHKey = getSingletonSubelement("requesteePrivateDHKey");
        try {
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decode(encodedRequesteePrivateDHKey));
            KeyFactory keyFactory = KeyFactory.getInstance(Constants.KEY_AGREEMENT_PROTOCOL);
            requesteePrivateDHKey = keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (Exception e) {
            throw new DocumentException("Error reconstructing requestee DH key", e);
        }
        String encodedRequesteePublicDHKey = getSingletonSubelement("requesteePublicDHKey");
        try {
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decode(encodedRequesteePublicDHKey));
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            requesteePublicDHKey = keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            throw new DocumentException("Error reconstructing requestee DH key", e);
        }
        try {
            String encodedRequesteeDHParamSpecG = getSingletonSubelement("requesteeDHParamSpecG");
            String encodedRequesteeDHParamSpecP = getSingletonSubelement("requesteeDHParamSpecP");
            BigInteger requesteeDHParamSpecP = new BigInteger(encodedRequesteeDHParamSpecP, 16);
            BigInteger requesteeDHParamSpecG = new BigInteger(encodedRequesteeDHParamSpecG, 16);
            requesteeDHParamSpec = new DHParameterSpec(requesteeDHParamSpecP, requesteeDHParamSpecG);
        } catch (Exception e) {
            throw new DocumentException("Error reconstructiong requestee DH parameters", e);
        }
        lastFixedRequestIndex = intValue(getSingletonSubelement("lastFixedRequestIndex"));
        dynamicWindowPointerAddress = getSingletonSubelement("dynamicWindowPointerAddress");
        dynamicWindowPrefix = getSingletonSubelement("dynamicWindowPrefix");
        lastDynamicRequestIndex = intValue(getSingletonSubelement("lastDynamicRequestIndex"));
        int i;
        channels = new LinkedList();
        NodeList channelNodes = getRootElement().getElementsByTagName("channel");
        for (i = 0; i < channelNodes.getLength(); i++) {
            Channel channel = new Channel((Element) channelNodes.item(i));
            channels.add(channel);
        }
        requests = new LinkedList();
        NodeList requestNodes = getRootElement().getElementsByTagName("request");
        for (i = 0; i < requestNodes.getLength(); i++) {
            Request request = new Request((Element) requestNodes.item(i));
            requests.add(request);
        }
        messages = new LinkedList();
        NodeList messageNodes = getRootElement().getElementsByTagName("message");
        for (i = 0; i < messageNodes.getLength(); i++) {
            Message message = new Message((Element) messageNodes.item(i));
            messages.add(message);
        }
    }

    /**
     * Builds a DOM tree containing the document data.
     *
     * @exception DocumentException if document creation fails.
     **/
    public void compose() throws DocumentException {
        super.compose();
        try {
            rootElement = document.createElement("mailstore");
            addTextNode(rootElement, "version", (new Float(version)).toString());
            addTextNode(rootElement, "alias", alias);
            addTextNode(rootElement, "privateSSKKey", privateSSKKey);
            addTextNode(rootElement, "publicSSKKey", publicSSKKey);
            String encodedRequesteePrivateDHKey = Base64.encode(requesteePrivateDHKey.getEncoded());
            addTextNode(rootElement, "requesteePrivateDHKey", encodedRequesteePrivateDHKey);
            String encodedRequesteePublicDHKey = Base64.encode(requesteePublicDHKey.getEncoded());
            addTextNode(rootElement, "requesteePublicDHKey", encodedRequesteePublicDHKey);
            addTextNode(rootElement, "requesteeDHParamSpecG", requesteeDHParamSpec.getG().toString(16));
            addTextNode(rootElement, "requesteeDHParamSpecP", requesteeDHParamSpec.getP().toString(16));
            addTextNode(rootElement, "lastFixedRequestIndex", (new Integer(lastFixedRequestIndex)).toString());
            addTextNode(rootElement, "dynamicWindowPointerAddress", dynamicWindowPointerAddress);
            addTextNode(rootElement, "dynamicWindowPrefix", dynamicWindowPrefix);
            addTextNode(rootElement, "lastDynamicRequestIndex", (new Integer(lastDynamicRequestIndex)).toString());
            Iterator iter;
            iter = channels.iterator();
            while (iter.hasNext()) {
                Channel channel = (Channel) iter.next();
                channel.compose(document);
                rootElement.appendChild(channel.getRootElement());
            }
            iter = requests.iterator();
            while (iter.hasNext()) {
                Request request = (Request) iter.next();
                request.compose(document);
                rootElement.appendChild(request.getRootElement());
            }
            iter = messages.iterator();
            while (iter.hasNext()) {
                Message message = (Message) iter.next();
                message.compose(document);
                rootElement.appendChild(message.getRootElement());
            }
        } catch (Exception e) {
            throw new DocumentException("Mailstore construction error", e);
        }
    }

    /**
     * Adds a channel to the list of those to check during updates.
     **/
    public void addChannel(Channel channel) {
        channels.add(channel);
    }

    /**
     * Retrieves a specific channel.
     *
     * @param contactPublicSSKKey the public SSK key of the contact who
     *     controls the box.
     * @return the channel, or null if no such channel exists.
     **/
    public Channel getChannel(String contactPublicSSKKey) {
        Iterator iter = channels.iterator();
        while (iter.hasNext()) {
            Channel channel = (Channel) iter.next();
            if (channel.getContactPublicSSKKey().equals(contactPublicSSKKey)) {
                return channel;
            }
        }
        return null;
    }

    /**
     * Fetches a list of the channels for this mailstore.
     *
     * @return a List of Channel objects.
     **/
    public List getChannels() {
        return channels;
    }

    /**
     * Checks if the specified channel already exists.
     *
     * @param contactPublicSSKKey the public SSK key of the contact who
     *     controls the box.
     * @return true if the channel already exists.
     **/
    public boolean hasChannel(String contactPublicSSKKey) {
        return (getChannel(contactPublicSSKKey) != null);
    }

    /**
     * Removes any channels matching the parameters.  It is not considered
     * an error if no box matches.
     *
     * @param contactPublicSSKKey the public SSK key of the contact who
     *     controls the box.
     **/
    public void removeChannel(String contactPublicSSKKey) {
        Iterator iter = channels.iterator();
        while (iter.hasNext()) {
            Channel channel = (Channel) iter.next();
            if (channel.getContactPublicSSKKey().equals(contactPublicSSKKey)) {
                iter.remove();
            }
        }
    }

    /**
     * Adds an incoming request to the list.
     **/
    public void addRequest(Request request) {
        requests.add(request);
    }

    /**
     * Retrieves a specific request.
     *
     * @param publicSSKKey the public SSK key of the requester.
     *
     * @return the request, or null if a matching request is not found.
     **/
    public Request getRequest(String publicSSKKey) {
        Iterator iter = requests.iterator();
        while (iter.hasNext()) {
            Request request = (Request) iter.next();
            if (request.getRequesterPublicSSKKey().equals(publicSSKKey)) {
                return request;
            }
        }
        return null;
    }

    /**
     * Fetches a list of the requests for this mailstore.
     *
     * @return a List of Request objects.
     **/
    public List getRequests() {
        return requests;
    }

    /**
     * Checks if the specified request already exists.
     *
     * @param publicSSKKey the public SSK key of the requester.
     * @return true if the request already exists.
     **/
    public boolean hasRequest(String publicSSKKey) {
        return (getRequest(publicSSKKey) != null);
    }

    /**
     * Removes any requests matching the parameters.  It is not considered
     * an error if no box matches.
     *
     * @param publicSSKKey the public SSK key of the requester.
     **/
    public void removeRequest(String publicSSKKey) {
        Iterator iter = requests.iterator();
        while (iter.hasNext()) {
            Request request = (Request) iter.next();
            if (request.getRequesterPublicSSKKey().equals(publicSSKKey)) {
                iter.remove();
            }
        }
    }

    /**
     * Adds an incoming message to the list.
     **/
    public void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * Retrieves a specific message.
     *
     * @param id the message identifier.
     *
     * @return the message, or null if a matching message is not found.
     **/
    public Message getMessage(String id) {
        Iterator iter = messages.iterator();
        while (iter.hasNext()) {
            Message message = (Message) iter.next();
            if (message.getId().equals(publicSSKKey)) {
                return message;
            }
        }
        return null;
    }

    /**
     * Fetches a list of the messages for this mailstore.
     *
     * @return a List of Message objects.
     **/
    public List getMessages() {
        return messages;
    }

    /**
     * Checks if the specified message already exists.
     *
     * @param id the message identifier.
     * @return true if the message already exists.
     **/
    public boolean hasMessage(String id) {
        return (getMessage(id) != null);
    }

    /**
     * Removes any messages matching the parameters.  It is not considered
     * an error if no box matches.
     *
     * @param id the message identifier.
     **/
    public void removeMessage(String id) {
        Iterator iter = messages.iterator();
        while (iter.hasNext()) {
            Message message = (Message) iter.next();
            if (message.getId().equals(id)) {
                iter.remove();
            }
        }
    }

    /**
     * @return the document format version.
     **/
    public double getVersion() {
        return version;
    }

    /**
     * @return the user's alias.
     **/
    public String getAlias() {
        return alias;
    }

    /**
     * Sets the user's alias.
     *
     * @param alias the new alias.
     **/
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the user's private SSK key.
     **/
    public String getPrivateSSKKey() {
        return privateSSKKey;
    }

    /**
     * @return the user's public SSK key.
     **/
    public String getPublicSSKKey() {
        return publicSSKKey;
    }

    /**
     * @return the private DH key for incoming requests.
     **/
    public PrivateKey getRequesteePrivateDHKey() {
        return requesteePrivateDHKey;
    }

    /**
     * @return the public DH key for incoming requests.
     **/
    public PublicKey getRequesteePublicDHKey() {
        return requesteePublicDHKey;
    }

    /**
     * @return the DH parameters for incoming requests.
     **/
    public DHParameterSpec getRequesteeDHParamSpec() {
        return requesteeDHParamSpec;
    }

    /**
     * @return the user's most recent fixed-window prosposal index.
     **/
    public int getLastFixedRequestIndex() {
        return lastFixedRequestIndex;
    }

    /**
     * Updates the value of the last fixed-window request index for this store.
     *
     * @param index the new index.
     **/
    public void setLastFixedRequestIndex(int index) {
        this.lastFixedRequestIndex = index;
    }

    /**
     * @return the Freenet key where the dynamic window prefix is published
     *     and updated.
     **/
    public String getDynamicWindowPointerAddress() {
        return dynamicWindowPointerAddress;
    }

    /**
     * Sets the most current dynamic-window channel initiation request prefix.
     *
     * @param prefix the new prefix.
     **/
    public void setDynamicWindowPrefix(String prefix) {
        dynamicWindowPrefix = prefix;
    }

    /**
     * @return the most current dynamic-window channel initiation request
     *     prefix.
     **/
    public String getDynamicWindowPrefix() {
        return dynamicWindowPrefix;
    }

    /**
     * @return the user's most recent dynamic-window prosposal index.
     **/
    public int getLastDynamicRequestIndex() {
        return lastDynamicRequestIndex;
    }

    /**
     * Updates the value of the last dynamic-window request index for this store.
     *
     * @param index the new index.
     **/
    public void setLastDynamicRequestIndex(int index) {
        this.lastDynamicRequestIndex = index;
    }

    /**
     * The version of the document format.
     **/
    protected double version;

    /**
     * The human-friendly handle by which the user will be known.
     **/
    protected String alias;

    /**
     * The private SSK key for the user.
     **/
    protected String privateSSKKey;

    /**
     * The public SSK key for the user.
     **/
    protected String publicSSKKey;

    /**
     * The private Diffie-Hellman key for incoming requests.
     **/
    protected PrivateKey requesteePrivateDHKey;

    /**
     * The public Diffie-Hellman key for incoming requests.
     **/
    protected PublicKey requesteePublicDHKey;

    /**
     * The Diffie-Hellman parameters used to generate the keypair.
     **/
    protected DHParameterSpec requesteeDHParamSpec;

    /**
     * The index of the most recently observed fixed-window channel request
     * index.
     **/
    protected int lastFixedRequestIndex;

    /**
     * The Freenet key where the dynamic window prefix is published and updated.
     **/
    protected String dynamicWindowPointerAddress;

    /**
     * The most current dynamic-window channel initiation request prefix.
     **/
    protected String dynamicWindowPrefix;

    /**
     * The index of the most recently observed dynamic-window channel request
     * index.
     **/
    protected int lastDynamicRequestIndex;

    /**
     * The channels specified in the mailstore.
     **/
    protected List channels;

    /**
     * The incoming channel requests specified in the mailstore.
     **/
    protected List requests;

    /**
     * The messages saved in the mailstore.
     **/
    protected List messages;
}
