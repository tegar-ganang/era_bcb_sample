package es.rediris.searchy.server;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import de.fmui.spheon.jsoap.Fault;
import de.fmui.spheon.jsoap.Entry;
import de.fmui.spheon.jsoap.Body;
import de.fmui.spheon.jsoap.SoapParser;
import de.fmui.spheon.jsoap.Envelope;
import de.fmui.spheon.jsoap.SoapConfig;
import de.fmui.spheon.jsoap.SoapException;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import es.rediris.searchy.server.acl.Rule;
import es.rediris.searchy.server.acl.AccessControl;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 * 
 * <p> NanoHTTPD version 1.01,
 * Copyright &copy; 2001 Jarno Elonen (elonen@iki.fi, http://iki.fi/elonen/)
 *
 * Modified by David F. Barrero
 *
 * <p><b>Features & limitations: </b><ul>
 * 
 *    <li> Only one Java file </li>
 *    <li> Java 1.1 compatible </li>
 *    <li> Released as open source, Modified BSD licence </li>
 *    <li> No fixed config files, logging, authorization etc. (Implement yourself if you need them.) </li>
 *    <li> Supports parameter parsing of GET and POST methods </li>
 *    <li> Supports both dynamic content and file serving </li>
 *    <li> Never caches anything </li>
 *    <li> Doesn't limit bandwidth, request time or simultaneous connections </li>
 *    <li> Default code serves files and shows all HTTP parameters and headers</li>
 *    <li> File server supports directory listing, index.html and index.htm </li>
 *    <li> File server does the 301 redirection trick for directories without '/'</li>
 *    <li> File server supports simple skipping for files (continue download) </li>
 *    <li> File server uses current directory as a web root </li>
 *    <li> File server serves also very long files without memory overhead </li>
 *    <li> Contains a built-in list of most common mime types </li>
 * 
 * </ul>
 * 
 * <p><b>Ways to use: </b><ul>
 * 
 *    <li> Run as a standalone app, serves files from current directory and shows requests</li>
 *    <li> Subclass serve() and embed to your own program </li>
 *    <li> Call serveFile() from serve() with your own base directory </li>
 * 
 * </ul>
 * 
 * See the end of the source file for distribution license
 * (Modified BSD licence)
 */
public class NanoHTTPD {

    static Logger logger = Logger.getLogger(NanoHTTPD.class);

    /**
	 * Override this to customize the server.<p>
	 * 
	 * (By default, this delegates to serveFile() and allows directory listing.)
	 * 
	 * @param uri	Percent-decoded URI without parameters, for example "/index.cgi"
	 * @param method	"GET", "POST" etc.
	 * @param parms	Parsed, percent decoded parameters from URI and, in case of POST, data.
	 * @param header	Header entries, percent decoded
	 *
	 * @return HTTP response, see class Response for details
	 */
    public Response serve(String uri, String method, Properties header, Properties parms, String data) {
        Envelope env = null;
        Object target = null;
        try {
            Envelope foo = SoapParser.read(sc, data);
            Entry operation = foo.getBody().getRootChild();
            if (operation == null) {
                env = new Envelope();
                env.addFault(new Fault(env.getBody(), "env:Client", "No method found.", sc.getActor(), "No method found."));
                logger.warn("No method found");
                return new Response(HTTP_BADREQUEST, MIME_XML, env.toString());
            }
            String methode = operation.getLocal();
            String objekt = "";
            objekt = sc.getService(operation.getNamespace(operation.getPrefix()));
            if ((objekt == null) || (objekt.equals(""))) {
                env = new Envelope();
                env.addFault(new Fault(env.getBody(), "env:Server", "Service '" + operation.getNamespace(operation.getPrefix()) + "' unknown.", sc.getActor(), "Service '" + operation.getNamespace(operation.getPrefix()) + "' unknown."));
                logger.info("Request with unknown URI " + operation.getNamespace(operation.getPrefix()));
                return new Response(HTTP_BADREQUEST, MIME_XML, env.toString());
            }
            synchronized (targets) {
                target = targets.pop();
            }
            env = SoapParser.execute(sc, foo, target);
        } catch (SoapException ex) {
            logger.debug("Toy aqui");
            Fault f = ex.getFault();
            Envelope envelope = new Envelope();
            envelope.addFault(f);
            env = envelope;
        } catch (EmptyStackException ex) {
            logger.warn("Service stack empty, please, repport this fact");
            Fault f = new Fault((Body) null, "Server", "Server Busy", "", (String) null);
            Envelope envelope = new Envelope();
            envelope.addFault(f);
            return new Response(HTTP_SERVICEUNAVAILABLE, MIME_XML, f.toString());
        } catch (Exception ex) {
            logger.error("Unknown error while executing SOAP call. " + ex);
        } finally {
            synchronized (targets) {
                targets.push(target);
            }
        }
        if (env == null) {
            logger.warn("A bad formated SOAP request has been recived");
            logger.warn("\tData: " + data);
            Fault f = new Fault((Body) null, "Client", "Bad SOAP message", "", (String) null);
            Envelope envelope = new Envelope();
            envelope.addFault(f);
            return new Response(HTTP_BADREQUEST, MIME_XML, f.toString());
        }
        if (env.hasFault()) {
            Fault f = env.getFault();
            logger.debug("There was some error while invoking service");
            logger.debug("\tSOAP Code : " + f.getFaultcode());
            logger.debug("\tSOAP Code : " + f.getFaultstring());
            return new Response(HTTP_BADREQUEST, MIME_XML, f.toString());
        }
        String responsePayload = env.toString();
        Response r = new Response(HTTP_OK, MIME_XML, responsePayload);
        r.addHeader("Content-Length", "" + responsePayload.length());
        return r;
    }

    /**
	 * HTTP response.
	 * Return one of these from serve().
	 */
    public class Response {

        /**
		 * Default constructor: response = HTTP_OK, data = mime = 'null'
		 */
        public Response() {
            this.status = HTTP_OK;
        }

        /**
		 * Basic constructor.
		 */
        public Response(String status, String mimeType, String data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
		 * Adds given line to the header.
		 */
        public void addHeader(String name, String value) {
            header.put(name, value);
        }

        /**
		 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
		 */
        public String status;

        /**
		 * MIME type of content, e.g. "text/html"
		 */
        public String mimeType;

        /**
		 * Data of the response, may be null.
		 */
        public String data;

        /**
		 * Headers for the HTTP response. Use addHeader()
		 * to add lines.
		 */
        public Properties header = new Properties();
    }

    /**
     * Class name of the web service implementation, only
     * used in a request scope web service to keep the object
     * defined in service.xml.
     */
    private static final String jClass = "es.rediris.searchy.ws.SOAPSearchyImpl";

    /**
	 * URI associated with Searchy web service
	 */
    private static final String URI = "urn:mace:rediris.es:searchy";

    /**
     * Objects that implement SOAP services,
     * only used in application scope to avoid multithread problems
     * in the implementing objects
     */
    private Stack targets = new Stack();

    /** index of the object we are accessing */
    private int index;

    /** Limit of connections acepted */
    private int maxConn;

    /** Number of SOAP request being attended */
    private int currentConections = 0;

    /** Semaphore used to syncronize connections counting */
    private Object semaphore = new Object();

    /** SOAP config */
    private SoapConfig sc = new SoapConfig();

    /** Socket timeout, default to 1000 miliseconds */
    private int timeout = 1000;

    /**
	 * Some HTTP response status codes
	 */
    public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently", HTTP_BADREQUEST = "400 Bad Request", HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found", HTTP_METHODNOTALLOWED = "405 Method Not Allowed", HTTP_LENGTHREQUIRED = "411 Length Required", HTTP_UNSUPPORTEDMEDIATYPE = "415 Unsupported Media Type", HTTP_INTERNALERROR = "500 Internal Server Error", HTTP_NOTIMPLEMENTED = "501 Not Implemented", HTTP_SERVICEUNAVAILABLE = "503 Service Unavailable";

    /**
	 * Common mime types for dynamic content
	 */
    public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html", MIME_XML = "text/xml";

    /** This object contains acl all stuff */
    protected static AccessControl access = null;

    protected void setACL(AccessControl access) {
        logger.debug("Metiendo access");
        this.access = access;
    }

    /**
	 * Starts a HTTP server to given port.<p>
	 * Throws an IOException if the socket is already in use
	 */
    public NanoHTTPD(int port, int maxConn, int timeout, AccessControl access) throws IOException {
        myTcpPort = port;
        this.maxConn = maxConn;
        this.timeout = timeout;
        this.access = access;
        sc.addService(this.URI, this.jClass);
        for (int i = 0; i < maxConn; i++) {
            try {
                Object object;
                Class temp = Class.forName(jClass);
                object = (Object) temp.newInstance();
                targets.push(object);
            } catch (ClassNotFoundException e) {
                logger.fatal("Class not found. " + e.getMessage());
                System.exit(-1);
            } catch (InstantiationException e) {
                logger.fatal("Class not instantiated. " + e.getMessage());
                System.exit(-1);
            } catch (IllegalAccessException e) {
                logger.fatal("Illegal access. " + e.getMessage());
                System.exit(-1);
            } catch (Exception e) {
                logger.fatal("Unknown error while creating service objects");
                logger.fatal(e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        final ServerSocket ss = new ServerSocket(myTcpPort);
        logger.info("Server ready");
        System.out.println("Hit Ctrl-C to stop server\n\n");
        try {
            currentConections = 0;
            while (true) {
                new HTTPSession(ss.accept());
            }
        } catch (IOException ioe) {
        }
    }

    /**
	 * Handles one session, i.e. parses the HTTP request
	 * and returns the response.
	 */
    private class HTTPSession implements Runnable {

        private Socket mySocket;

        private BufferedReader myIn;

        public HTTPSession(Socket s) {
            mySocket = s;
            logger.info("Connection from " + s.getInetAddress().getCanonicalHostName());
            if (currentConections >= maxConn) {
                logger.warn("Max connections reached");
                try {
                    sendError(HTTP_SERVICEUNAVAILABLE, "Sorry, too many clients, try later");
                } catch (InterruptedException e) {
                    return;
                }
            }
            try {
                if (!access.check(s.getInetAddress())) {
                    logger.info("Unauthorized access from " + s.getInetAddress());
                    sendError(HTTP_FORBIDDEN, "You are not allow to access this service");
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (semaphore) {
                currentConections++;
            }
            try {
                mySocket.setSoTimeout(timeout);
            } catch (SocketException e) {
                logger.warn("Could not set socket timeout");
            }
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

        public void run() {
            try {
                int pointer;
                InputStream is = mySocket.getInputStream();
                if (is == null) return;
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String algo = in.readLine();
                pointer = algo.length() + 1;
                StringTokenizer st = new StringTokenizer(algo);
                if (!st.hasMoreTokens()) sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                String method = st.nextToken();
                if (!st.hasMoreTokens()) sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                String uri = decodePercent(st.nextToken());
                Properties parms = new Properties();
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                }
                Properties header = new Properties();
                String line = in.readLine();
                pointer += line.length() + 1;
                while (line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    header.put(line.substring(0, p).trim(), line.substring(p + 1).trim());
                    line = in.readLine();
                    pointer += line.length() + 1;
                }
                String data = null;
                if (!method.equalsIgnoreCase("POST")) sendError(HTTP_METHODNOTALLOWED, "This server only supports POST method");
                int size = 0;
                if ((header.getProperty("ContentType") != MIME_XML) && (header.getProperty("ContentType") != null)) sendError(HTTP_UNSUPPORTEDMEDIATYPE, "This server only supports SOAP messages");
                try {
                    size = Integer.parseInt(header.getProperty("Content-Length"));
                } catch (NumberFormatException e) {
                    sendError(HTTP_LENGTHREQUIRED, "BAD REQUEST: Incorrect or not present Content-length header");
                }
                char foo[] = new char[size];
                int foobar = in.read(foo, 0, size);
                if (foobar == -1) sendError(HTTP_BADREQUEST, "");
                data = new String(foo);
                Response r = serve(uri, method, header, parms, data);
                if (r == null) {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else if (r.status != HTTP_OK) {
                    sendSoapError(r.status, r.data);
                } else {
                    sendResponse(r.status, r.mimeType, r.header, r.data);
                }
                in.close();
            } catch (SocketTimeoutException ie) {
                logger.info("Socket timeout exceeded");
                try {
                    mySocket.close();
                } catch (Throwable e) {
                }
            } catch (IOException ioe) {
                logger.debug("IO error while sending response");
                try {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                } catch (Throwable t) {
                    logger.debug("Error while sending error message");
                }
            } catch (InterruptedException ie) {
            } finally {
                synchronized (semaphore) {
                    currentConections--;
                }
            }
        }

        /**
		 * Decodes the percent encoding scheme. <br/>
		 * For example: "an+example%20string" -> "an example string"
		 */
        private String decodePercent(String str) throws InterruptedException {
            try {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    switch(c) {
                        case '+':
                            sb.append(' ');
                            break;
                        case '%':
                            sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                            i += 2;
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                return new String(sb.toString().getBytes());
            } catch (Exception e) {
                sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
                return null;
            }
        }

        /**
		 * Decodes parameters in percent-encoded URI-format
		 * ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
		 * adds them to given Properties.
		 */
        private void decodeParms(String parms, Properties p) throws InterruptedException {
            if (parms == null) return;
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
            }
        }

        /**
		 * Returns an error message as a HTTP response and
		 * throws InterruptedException to stop furhter request processing.
		 */
        private void sendError(String status, String msg) throws InterruptedException {
            sendResponse(status, MIME_PLAINTEXT, (Properties) null, msg);
            throw new InterruptedException();
        }

        private void sendSoapError(String status, String msg) throws InterruptedException {
            String header = "<?xml version=\"1.0\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Body>";
            String footer = "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
            sendResponse(status, MIME_XML, null, header + msg + footer);
        }

        /**
		 * Sends given response to the socket.
		 */
        private void sendResponse(String status, String mime, Properties header, String payload) {
            InputStream data = new ByteArrayInputStream(payload.getBytes());
            try {
                if (status == null) throw new Error("sendResponse(): Status can't be null.");
                OutputStream out = mySocket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");
                if (mime != null) pw.print("Content-Type: " + mime + "\r\n");
                if (header == null || header.getProperty("Date") == null) pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                if (header != null) {
                    Enumeration e = header.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }
                pw.print("\r\n");
                pw.flush();
                if (data != null) {
                    byte[] buff = new byte[2048];
                    int read = 2048;
                    while (read == 2048) {
                        read = data.read(buff, 0, 2048);
                        out.write((byte[]) buff, 0, read);
                    }
                }
                out.flush();
                out.close();
                if (data != null) data.close();
            } catch (IOException ioe) {
                logger.debug("Could not write on socket");
                try {
                    mySocket.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    ;

    /**
	 * URL-encodes everything between "/"-characters.
	 * Encodes spaces as '%20' instead of '+'.
	 */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) newUri += "/"; else if (tok.equals(" ")) newUri += "%20"; else try {
                newUri += URLEncoder.encode(tok, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("UTF-8 encoding not supported");
            }
        }
        return newUri;
    }

    private int myTcpPort;

    private File myFileDir;

    /**
	 * GMT date formatter
	 */
    private static java.text.SimpleDateFormat gmtFrmt;

    static {
        gmtFrmt = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static void main(String argv[]) {
        Logger logger = Logger.getLogger(NanoHTTPD.class);
        String configFile = es.rediris.searchy.util.Util.getConfigFile();
        NanoHTTPD server;
        Document doc = null;
        Node node;
        String name = "";
        AccessControl access = new AccessControl();
        int port = 33333;
        int timeout = 60000;
        int handlerCount = 3;
        System.out.println("Server loading config file " + configFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            doc = builder.parse(configFile);
        } catch (SAXParseException e) {
            return;
        } catch (SAXException e) {
            System.out.println("Error parsing config file");
            return;
        } catch (IOException e) {
            System.out.println("I/O error accesing config file. " + e.getMessage());
            return;
        } catch (ParserConfigurationException e) {
            System.out.println("Parser could not be built. " + e.getMessage());
            return;
        }
        String str;
        node = es.rediris.searchy.util.DOMUtil.getNodeByName(doc, "transport");
        if (node == null) {
            System.out.println("transport section not found");
            return;
        }
        String log4jConf = es.rediris.searchy.util.DOMUtil.getNodeValueByName(node, "log-config");
        if (log4jConf != null) {
            System.out.println("Setting up logging system using file " + log4jConf);
            try {
                PropertyConfigurator.configure(log4jConf);
            } catch (Exception e) {
                System.out.println("IO error accesing " + log4jConf + " , please chech config file");
                System.out.println("Logging system could not been correcty initialized");
            }
        } else {
            System.out.println("Logging could not been initialited");
        }
        try {
            str = es.rediris.searchy.util.DOMUtil.getNodeValueByName(node, "timeout");
            timeout = java.lang.Integer.parseInt(str.trim());
            str = es.rediris.searchy.util.DOMUtil.getNodeValueByName(node, "max-connections");
            handlerCount = java.lang.Integer.parseInt(str.trim());
            str = es.rediris.searchy.util.DOMUtil.getNodeValueByName(node, "port");
            port = java.lang.Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            logger.error("Expected numeric valour in an element of the config file, using defaults.");
        } catch (Exception e) {
            logger.error("Unknown error while reading config file");
        }
        NodeList listACL = ((Element) node).getElementsByTagName("acl");
        ;
        switch(listACL.getLength()) {
            case 0:
                logger.info("No ACL found, allowing all incoming connections");
                access.addDefault(true);
                break;
            case 1:
                Element acl = (Element) listACL.item(0);
                String defAction = acl.getAttribute("default");
                if (defAction.equals("")) {
                    logger.info("No default action defined, setting default action to allow");
                    access.addDefault(true);
                } else if (defAction.equals("allow")) access.addDefault(true); else if (defAction.equals("deny")) access.addDefault(false); else {
                    logger.info("Not understood action " + defAction + " setting default action to allow");
                    access.addDefault(true);
                }
                NodeList ruleList = acl.getElementsByTagName("rule");
                for (int i = 0; i < ruleList.getLength(); i++) {
                    boolean permission;
                    String ipString, netmaskString, action;
                    Rule rule;
                    InetAddress ip = null, netmask = null;
                    Element ruleElement = (Element) ruleList.item(i);
                    ipString = ruleElement.getAttribute("ip").trim();
                    if (ipString.equals("")) {
                        logger.warn("Ignoring rule, ip attribute not found in ACL rule");
                        continue;
                    }
                    try {
                        ip = InetAddress.getByName(ipString);
                    } catch (UnknownHostException e) {
                        logger.error(ipString + " is not a valid name (DNS error?), ignoring rule");
                        continue;
                    }
                    action = ruleElement.getAttribute("action").trim();
                    if (action.equals("")) {
                        logger.warn("Ignoring rule, action attribute not found in ACL rule");
                        continue;
                    } else if (action.equals("allow")) permission = true; else if (action.equals("deny")) permission = false; else {
                        logger.warn(action + " not understood, ignoring rule");
                        continue;
                    }
                    netmaskString = ruleElement.getAttribute("netmask");
                    if (netmaskString.equals("")) {
                        logger.info("ACL rule: " + ipString + ", " + action);
                        rule = new Rule(ip, permission);
                    } else {
                        try {
                            netmask = InetAddress.getByName(netmaskString);
                            logger.info("ACL rule: " + ipString + ", " + netmaskString + ", " + action);
                            rule = new Rule(ip, netmask, permission);
                        } catch (UnknownHostException e) {
                            logger.error(netmaskString + " is not a valid netmask, ignoring rule");
                            continue;
                        }
                    }
                    access.addRule(rule);
                }
                logger.info("ACL default action: " + defAction);
                break;
            default:
                logger.info("Incorrect ACL section");
                break;
        }
        try {
            server = new NanoHTTPD(port, handlerCount, timeout, access);
            server.setACL(access);
            logger.info("Agent prepared and listening new requests");
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            logger.fatal("Agent has not been initialized, check config file. " + e.getMessage());
            System.exit(-1);
        } catch (Exception e) {
            logger.fatal("Server has not been initialized or has died. " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Now serving files in port " + port + " from \"" + new File("").getAbsolutePath() + "\"");
        System.out.println("Hit Enter to stop.\n");
        try {
            System.in.read();
        } catch (Throwable t) {
        }
        ;
    }
}
