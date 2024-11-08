package org.apache.ws.security.message.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.DOM2Writer;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.ws.security.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * UsernameToken according to WS Security specifications, UsernameToken profile.
 * 
 * Enhanced to support digest password type for username token signature
 * 
 * @author Davanum Srinivas (dims@yahoo.com)
 * @author Werner Dittmann (Werner.Dittmann@t-online.de)
 */
public class UsernameToken {

    private static Log log = LogFactory.getLog(UsernameToken.class.getName());

    public static final String PASSWORD_TYPE = "passwordType";

    private String raw_password;

    protected Element element = null;

    protected Element elementUsername = null;

    protected Element elementPassword = null;

    protected Element elementNonce = null;

    protected Element elementCreated = null;

    protected Element elementSalt = null;

    protected Element elementIteration = null;

    protected String passwordType = null;

    protected boolean hashed = true;

    private static SecureRandom random = null;

    private static int DEFAULT_ITERATION = 1000;

    public static final QName TOKEN = new QName(WSConstants.WSSE_NS, WSConstants.USERNAME_TOKEN_LN);

    static {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
    }

    /**
     * Constructs a <code>UsernameToken</code> object and parses the
     * <code>wsse:UsernameToken</code> element to initialize it.
     * 
     * @param elem
     *            the <code>wsse:UsernameToken</code> element that contains
     *            the UsernameToken data
     * @throws WSSecurityException
     */
    public UsernameToken(Element elem) throws WSSecurityException {
        this.element = elem;
        QName el = new QName(this.element.getNamespaceURI(), this.element.getLocalName());
        if (!el.equals(TOKEN)) {
            throw new WSSecurityException(WSSecurityException.INVALID_SECURITY_TOKEN, "badTokenType00", new Object[] { el });
        }
        elementUsername = (Element) WSSecurityUtil.getDirectChild(element, "Username", WSConstants.WSSE_NS);
        elementPassword = (Element) WSSecurityUtil.getDirectChild(element, "Password", WSConstants.WSSE_NS);
        elementNonce = (Element) WSSecurityUtil.getDirectChild(element, "Nonce", WSConstants.WSSE_NS);
        elementCreated = (Element) WSSecurityUtil.getDirectChild(element, "Created", WSConstants.WSU_NS);
        elementSalt = (Element) WSSecurityUtil.getDirectChild(element, "Salt", WSConstants.WSSE11_NS);
        elementIteration = (Element) WSSecurityUtil.getDirectChild(element, "Interation", WSConstants.WSSE11_NS);
        if (elementUsername == null) {
            throw new WSSecurityException(WSSecurityException.INVALID_SECURITY_TOKEN, "badTokenType01", new Object[] { el });
        }
        if (elementSalt != null) {
            if (elementPassword != null) {
                throw new WSSecurityException(WSSecurityException.INVALID_SECURITY_TOKEN, "badTokenType01", new Object[] { el });
            }
            return;
        }
        hashed = false;
        if (elementPassword != null) {
            passwordType = elementPassword.getAttribute("Type");
        }
        if (passwordType != null && passwordType.equals(WSConstants.PASSWORD_DIGEST)) {
            hashed = true;
            if (elementNonce == null || elementCreated == null) {
                throw new WSSecurityException(WSSecurityException.INVALID_SECURITY_TOKEN, "badTokenType01", new Object[] { el });
            }
        }
    }

    /**
     * Constructs a <code>UsernameToken</code> object according to the defined
     * parameters. <p/> This constructs set the password encoding to
     * {@link WSConstants#PASSWORD_DIGEST}
     * 
     * @param doc
     *            the SOAP envelope as <code>Document</code>
     */
    public UsernameToken(boolean milliseconds, Document doc) {
        this(milliseconds, doc, WSConstants.PASSWORD_DIGEST);
    }

    /**
     * Constructs a <code>UsernameToken</code> object according to the defined
     * parameters. <p/>
     * 
     * @param doc
     *            the SOAP envelope as <code>Document</code>
     * @param pwType
     *            the required password encoding, either
     *            {@link WSConstants#PASSWORD_DIGEST} or
     *            {@link WSConstants#PASSWORD_TEXT} or <code>null</code> if no
     *            password required
     */
    public UsernameToken(boolean milliseconds, Document doc, String pwType) {
        this.element = doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.USERNAME_TOKEN_LN);
        WSSecurityUtil.setNamespace(this.element, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX);
        this.elementUsername = doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.USERNAME_LN);
        WSSecurityUtil.setNamespace(this.elementUsername, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX);
        this.elementUsername.appendChild(doc.createTextNode(""));
        element.appendChild(elementUsername);
        if (pwType != null) {
            this.elementPassword = doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.PASSWORD_LN);
            WSSecurityUtil.setNamespace(this.elementPassword, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX);
            this.elementPassword.appendChild(doc.createTextNode(""));
            element.appendChild(elementPassword);
            hashed = false;
            passwordType = pwType;
            if (passwordType.equals(WSConstants.PASSWORD_DIGEST)) {
                hashed = true;
                addNonce(doc);
                addCreated(milliseconds, doc);
            }
        }
    }

    /**
     * Creates and adds a Nonce element to this UsernameToken
     */
    public void addNonce(Document doc) {
        if (elementNonce != null) {
            return;
        }
        byte[] nonceValue = new byte[16];
        random.nextBytes(nonceValue);
        this.elementNonce = doc.createElementNS(WSConstants.WSSE_NS, "wsse:" + WSConstants.NONCE_LN);
        WSSecurityUtil.setNamespace(this.elementNonce, WSConstants.WSSE_NS, WSConstants.WSSE_PREFIX);
        this.elementNonce.appendChild(doc.createTextNode(Base64.encode(nonceValue)));
        element.appendChild(elementNonce);
    }

    /**
     * Creates and adds a Created element to this UsernameToken
     */
    public void addCreated(boolean milliseconds, Document doc) {
        if (elementCreated != null) {
            return;
        }
        DateFormat zulu = null;
        if (milliseconds) {
            zulu = new XmlSchemaDateFormat();
        } else {
            zulu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            zulu.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        Calendar rightNow = Calendar.getInstance();
        this.elementCreated = doc.createElementNS(WSConstants.WSU_NS, WSConstants.WSU_PREFIX + ":" + WSConstants.CREATED_LN);
        WSSecurityUtil.setNamespace(this.elementCreated, WSConstants.WSU_NS, WSConstants.WSU_PREFIX);
        this.elementCreated.appendChild(doc.createTextNode(zulu.format(rightNow.getTime())));
        element.appendChild(elementCreated);
    }

    /**
     * Adds and optionally creates a Salt element to this UsernameToken.
     * 
     * If the <code>saltCalue</code> is <code>null</code> the the method
     * generates a new salt. Otherwise it uses the the given value.
     * 
     * @param doc
     *            The Document for the UsernameToken
     * @param saltValue
     *            The salt to add, if null generate a new salt value
     * @param mac
     *            If <code>true</code> then an optionally generated value is
     *            usable for a MAC
     * @return Returns the added salt
     */
    public byte[] addSalt(Document doc, byte[] saltValue, boolean mac) {
        if (saltValue == null) {
            saltValue = generateSalt(mac);
        }
        this.elementSalt = doc.createElementNS(WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX + WSConstants.SALT_LN);
        WSSecurityUtil.setNamespace(this.elementSalt, WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX);
        this.elementSalt.appendChild(doc.createTextNode(Base64.encode(saltValue)));
        element.appendChild(elementSalt);
        return saltValue;
    }

    /**
     * Creates and adds a Iteration element to this UsernameToken
     */
    public void addIteration(Document doc, int iteration) {
        String text = "" + iteration;
        this.elementIteration = doc.createElementNS(WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX + WSConstants.ITERATION_LN);
        WSSecurityUtil.setNamespace(this.elementIteration, WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX);
        this.elementIteration.appendChild(doc.createTextNode(text));
        element.appendChild(elementIteration);
        return;
    }

    /**
     * Get the user name.
     * 
     * @return the data from the user name element.
     */
    public String getName() {
        return nodeString(this.elementUsername);
    }

    /**
     * Set the user name.
     * 
     * @param name
     *            sets a text node containing the use name into the user name
     *            element.
     */
    public void setName(String name) {
        Text node = getFirstNode(this.elementUsername);
        node.setData(name);
    }

    /**
     * Get the nonce.
     * 
     * @return the data from the nonce element.
     */
    public String getNonce() {
        return nodeString(this.elementNonce);
    }

    /**
     * Get the created timestamp.
     * 
     * @return the data from the created time element.
     */
    public String getCreated() {
        return nodeString(this.elementCreated);
    }

    /**
     * Gets the password string. This is the password as it is in the password
     * element of a username token. Thus it can be either plain text or the
     * password digest value.
     * 
     * @return the password string or <code>null</code> if no such node
     *         exists.
     */
    public String getPassword() {
        return nodeString(this.elementPassword);
    }

    /**
     * Get the Salt value of this UsernameToken.
     * 
     * @return Returns the binary Salt value or <code>null</code> if no Salt
     *         value is available in the username token.
     * @throws WSSecurityException
     */
    public byte[] getSalt() throws WSSecurityException {
        String salt = nodeString(this.elementSalt);
        if (salt != null) {
            return Base64.decode(nodeString(this.elementSalt));
        }
        return null;
    }

    /**
     * Get the Iteration value of this UsernameToken.
     * 
     * @return Returns the Iteration value. If no Iteration was specified in the
     *         username token the default value according to the specification
     *         is returned.
     */
    public int getIteration() {
        String iter = nodeString(this.elementIteration);
        if (iter != null) {
            return Integer.parseInt(iter);
        }
        return DEFAULT_ITERATION;
    }

    /**
     * Get the hashed indicator. If the indicator is
     * <code>true> the password of the
     * <code>UsernameToken</code> was encoded using
     * {@link WSConstants#PASSWORD_DIGEST}
     *
     * @return the hashed indicator.
     */
    public boolean isHashed() {
        return hashed;
    }

    /**
     * @return Returns the passwordType.
     */
    public String getPasswordType() {
        return passwordType;
    }

    /**
     * Sets the password string. This function sets the password in the
     * <code>UsernameToken</code> either as plain text or encodes the password
     * according to the WS Security specifications, UsernameToken profile, into
     * a password digest.
     * 
     * @param pwd
     *            the password to use
     */
    public void setPassword(String pwd) {
        if (pwd == null) {
            throw new IllegalArgumentException("pwd == null");
        }
        raw_password = pwd;
        Text node = getFirstNode(this.elementPassword);
        try {
            if (!hashed) {
                node.setData(pwd);
                this.elementPassword.setAttribute("Type", WSConstants.PASSWORD_TEXT);
            } else {
                node.setData(doPasswordDigest(getNonce(), getCreated(), pwd));
                this.elementPassword.setAttribute("Type", WSConstants.PASSWORD_DIGEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the raw (plain text) password used to compute secret key.
     * 
     * @param raw_password the raw_password to set
     */
    public void setRawPassword(String raw_password) {
        this.raw_password = raw_password;
    }

    public static String doPasswordDigest(String nonce, String created, String password) {
        String passwdDigest = null;
        try {
            byte[] b1 = Base64.decode(nonce);
            byte[] b2 = created.getBytes("UTF-8");
            byte[] b3 = password.getBytes("UTF-8");
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
            int i = 0;
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;
            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;
            System.arraycopy(b3, 0, b4, offset, b3.length);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(b4);
            passwdDigest = Base64.encode(sha.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return passwdDigest;
    }

    /**
     * Returns the first text node of an element.
     * 
     * @param e
     *            the element to get the node from
     * @return the first text node or <code>null</code> if node is null or is
     *         not a text node
     */
    private Text getFirstNode(Element e) {
        Node node = e.getFirstChild();
        return ((node != null) && node instanceof Text) ? (Text) node : null;
    }

    /**
     * Returns the data of an element as String or null if either the the element
     * does not contain a Text node or the node is empty.
     * 
     * @param e
     *            DOM element
     * @return Element text node data as String
     */
    private String nodeString(Element e) {
        if (e != null) {
            Text node = getFirstNode(e);
            if (node != null) {
                return node.getData();
            }
        }
        return null;
    }

    /**
     * Returns the dom element of this <code>UsernameToken</code> object.
     * 
     * @return the <code>wsse:UsernameToken</code> element
     */
    public Element getElement() {
        return this.element;
    }

    /**
     * Returns the string representation of the token.
     * 
     * @return a XML string representation
     */
    public String toString() {
        return DOM2Writer.nodeToString((Node) this.element);
    }

    /**
     * Gets the id.
     * 
     * @return the value of the <code>wsu:Id</code> attribute of this username
     *         token
     */
    public String getID() {
        return this.element.getAttributeNS(WSConstants.WSU_NS, "Id");
    }

    /**
     * Set the id of this username token.
     * 
     * @param id
     *            the value for the <code>wsu:Id</code> attribute of this
     *            username token
     */
    public void setID(String id) {
        String prefix = WSSecurityUtil.setNamespace(this.element, WSConstants.WSU_NS, WSConstants.WSU_PREFIX);
        this.element.setAttributeNS(WSConstants.WSU_NS, prefix + ":Id", id);
    }

    /**
     * Gets the secret key as per WS-Trust spec. This method uses default setting
     * to generate the secret key. These default values are suitable for .NET
     * WSE.
     * 
     * @return a secret key constructed from information contained in this
     *         username token
     */
    public byte[] getSecretKey() {
        return getSecretKey(WSConstants.WSE_DERIVED_KEY_LEN, WSConstants.LABEL_FOR_DERIVED_KEY);
    }

    /**
     * Gets the secret key as per WS-Trust spec.
     * 
     * @param keylen
     *            How many bytes to generate for the key
     * @param labelString
     *            the label used to generate the seed
     * @return a secret key constructed from information contained in this
     *         username token
     */
    public byte[] getSecretKey(int keylen, String labelString) {
        byte[] key = null;
        try {
            Mac mac = Mac.getInstance("HMACSHA1");
            byte[] password = raw_password.getBytes("UTF-8");
            byte[] label = labelString.getBytes("UTF-8");
            byte[] nonce = Base64.decode(getNonce());
            byte[] created = getCreated().getBytes("UTF-8");
            byte[] seed = new byte[label.length + nonce.length + created.length];
            int offset = 0;
            System.arraycopy(label, 0, seed, offset, label.length);
            offset += label.length;
            System.arraycopy(nonce, 0, seed, offset, nonce.length);
            offset += nonce.length;
            System.arraycopy(created, 0, seed, offset, created.length);
            key = P_hash(password, seed, mac, keylen);
            if (log.isDebugEnabled()) {
                log.debug("password   :" + Base64.encode(password));
                log.debug("label      :" + Base64.encode(label));
                log.debug("nonce      :" + Base64.encode(nonce));
                log.debug("created    :" + Base64.encode(created));
                log.debug("seed       :" + Base64.encode(seed));
                log.debug("Key        :" + Base64.encode(key));
            }
        } catch (Exception e) {
            return null;
        }
        return key;
    }

    /**
     * This static method generates a derived key as defined in WSS Username
     * Token Profile.
     * 
     * @param password
     *            The password to include in the key generation
     * @param salt
     *            The Salt value
     * @param iteration
     *            The Iteration value. If zero (0) is given the method uses the
     *            default value
     * @return Returns the derived key a byte array
     * @throws WSSecurityException
     */
    public static byte[] generateDerivedKey(String password, byte[] salt, int iteration) throws WSSecurityException {
        if (iteration == 0) {
            iteration = DEFAULT_ITERATION;
        }
        byte[] pwBytes = password.getBytes();
        byte[] pwSalt = new byte[salt.length + pwBytes.length];
        System.arraycopy(pwBytes, 0, pwSalt, 0, pwBytes.length);
        System.arraycopy(salt, 0, pwSalt, pwBytes.length, salt.length);
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(0, "noSHA1availabe");
        }
        sha.reset();
        byte[] K = sha.digest(pwSalt);
        for (int i = 2; i <= iteration; i++) {
            sha.reset();
            K = sha.digest(K);
        }
        return K;
    }

    /**
     * This static method generates a 128 bit salt value as defined in WSS
     * Username Token Profile.
     * 
     * @param useForMac
     *            If <code>true</code> define the Salt for use in a MAC
     * @return Returns the 128 bit salt value as byte array
     */
    public static byte[] generateSalt(boolean useForMac) {
        byte[] saltValue = new byte[16];
        random.nextBytes(saltValue);
        if (useForMac) {
            saltValue[15] = 0x01;
        } else {
            saltValue[15] = 0x02;
        }
        return saltValue;
    }

    /**
     * P_hash as defined in RFC 2246 for TLS.
     * 
     * @param secret
     *            is the key for the HMAC
     * @param seed
     *            the seed value to start the generation - A(0)
     * @param mac
     *            the HMAC algorithm
     * @param required
     *            number of bytes to generate
     * @return a byte array that contains a secret key
     * @throws Exception
     */
    private static byte[] P_hash(byte[] secret, byte[] seed, Mac mac, int required) throws Exception {
        byte[] out = new byte[required];
        int offset = 0, tocpy;
        byte[] A, tmp;
        A = seed;
        SecretKeySpec key = new SecretKeySpec(secret, "HMACSHA1");
        mac.init(key);
        while (required > 0) {
            mac.update(A);
            A = mac.doFinal();
            mac.update(A);
            mac.update(seed);
            tmp = mac.doFinal();
            tocpy = min(required, tmp.length);
            System.arraycopy(tmp, 0, out, offset, tocpy);
            offset += tocpy;
            required -= tocpy;
        }
        return out;
    }

    /**
     * helper method.
     * <p/>
     *
     * @param a
     * @param b
     * @return
     */
    private static int min(int a, int b) {
        return (a > b) ? b : a;
    }
}
