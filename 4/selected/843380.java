package org.exist.http.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.IndexInfo;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.http.BadRequestException;
import org.exist.http.Descriptor;
import org.exist.http.NotFoundException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.xacml.AccessContext;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.util.MimeTable;
import org.exist.util.MimeType;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.Constants;
import org.exist.xquery.XPathException;
import org.exist.xupdate.Modification;
import org.exist.xupdate.XUpdateProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author wolf
 *
 */
public class RESTServer {

    protected static final String NS = "http://exist.sourceforge.net/NS/exist";

    protected static final String XUPDATE_NS = "http://www.xmldb.org/xupdate";

    protected static final Logger LOG = Logger.getLogger(RESTServer.class);

    protected static final Properties defaultProperties = new Properties();

    static {
        defaultProperties.setProperty(OutputKeys.INDENT, "yes");
        defaultProperties.setProperty(OutputKeys.ENCODING, "UTF-8");
        defaultProperties.setProperty(EXistOutputKeys.EXPAND_XINCLUDES, "yes");
        defaultProperties.setProperty(EXistOutputKeys.HIGHLIGHT_MATCHES, "elements");
        defaultProperties.setProperty(EXistOutputKeys.PROCESS_XSL_PI, "yes");
    }

    private static final String QUERY_ERROR_HEAD = "<html>" + "<head>" + "<title>Query Error</title>" + "<style type=\"text/css\">" + ".errmsg {" + "  border: 1px solid black;" + "  padding: 15px;" + "  margin-left: 20px;" + "  margin-right: 20px;" + "}" + "h1 { color: #C0C0C0; }" + ".path {" + "  padding-bottom: 10px;" + "}" + ".high { " + "  color: #666699; " + "  font-weight: bold;" + "}" + "</style>" + "</head>" + "<body>" + "<h1>XQuery Error</h1>";

    private String formEncoding;

    private String containerEncoding;

    private boolean useDynamicContentType;

    public RESTServer(String formEncoding, String containerEncoding, boolean useDynamicContentType) {
        this.formEncoding = formEncoding;
        this.containerEncoding = containerEncoding;
        this.useDynamicContentType = useDynamicContentType;
    }

    /**
     * Handle GET request. In the simplest case just returns the document or
     * binary resource specified in the path. If the path leads to a collection,
     * a listing of the collection contents is returned. If it resolves to a binary
     * resource with mime-type "application/xquery", this resource will be
     * loaded and executed by the XQuery engine.
     *
     * Use Cases -
     * 1) GET an XQuery resource, the resource is executed and the results are returned.
     * 2) GET a resource or collection listing
     * 3) GET with special querystring parameters. The query is loaded and executed from the querystring and the results are returned.
     *
     *
     * These querystring parameters are available for all GET's -
     * 
     * <ul>
     *	<li>_xsl: an URI pointing to an XSL stylesheet that will be applied to the returned XML.</li>
     * 
     * 	<li>_indent: if set to "yes", the returned XML will be pretty-printed.</li>
     * 
     * 	<li>_endoding: set's the character encoding scheme</li>
     * </ul>
     *
     *
     * Special parameters for when a query is sent in the querystring - 
     *
     * <ul>
     * 	<li>_xpath or _query: if specified, the given query is executed on the current resource or collection.</li>
     *
     * 	<li>_howmany: defines how many items from the query result will be returned.</li>
     *
     * 	<li>_start: a start offset into the result set.</li>
     *
     * 	<li>_wrap: if set to "yes", the query results will be wrapped into a exist:result element.</li>
     * </ul>
     *
     *
     * These parameters are available when GET'ing a stored XQuery -
     * <ul>
     *	<li>_source: if set to "yes" and a resource with mime-type "application/xquery" is requested
     * 	then the xquery will not be executed, instead the source of the document will be returned.
     * 	Must be enabled in descriptor.xml with the following syntax 
     * 	<xquery-app><allow-source><xquery path="/db/mycollection/myquery.xql"/></allow-source></xquery-app></li>
     * </ul>
     *
     *
     * @param broker
     * @param request
     * @param response
     * @param path
     * @throws BadRequestException
     * @throws PermissionDeniedException
     * @throws NotFoundException
     */
    public void doGet(DBBroker broker, HttpServletRequest request, HttpServletResponse response, String path) throws BadRequestException, PermissionDeniedException, NotFoundException, IOException {
        Properties outputProperties = new Properties();
        String option;
        if (request.getCharacterEncoding() == null) request.setCharacterEncoding(formEncoding);
        if ((option = request.getParameter("_indent")) != null) {
            outputProperties.setProperty(OutputKeys.INDENT, option);
        }
        String stylesheet;
        if ((stylesheet = request.getParameter("_xsl")) != null) {
            if (stylesheet.equals("no")) {
                outputProperties.setProperty(EXistOutputKeys.PROCESS_XSL_PI, "no");
                outputProperties.remove(EXistOutputKeys.STYLESHEET);
                stylesheet = null;
            } else {
                outputProperties.setProperty(EXistOutputKeys.STYLESHEET, stylesheet);
                LOG.debug("stylesheet = " + stylesheet);
            }
        } else {
            outputProperties.setProperty(EXistOutputKeys.PROCESS_XSL_PI, "yes");
        }
        String encoding;
        if ((encoding = request.getParameter("_encoding")) != null) {
            outputProperties.setProperty(OutputKeys.ENCODING, encoding);
        } else {
            encoding = "UTF-8";
        }
        RESTResource resource = null;
        try {
            resource = RESTResourceFactory.getResource(path, broker, request, response, formEncoding, containerEncoding);
            if (resource != null) {
                if (resource.isXQuery()) {
                    boolean source = false;
                    if ((option = request.getParameter("_source")) != null) source = option.equals("yes");
                    Descriptor descriptor = Descriptor.getDescriptorSingleton();
                    if (source && descriptor != null) {
                        if (descriptor.allowSourceXQuery(path)) {
                            writeResourceAs(resource, broker, stylesheet, encoding, "text/plain", outputProperties, response);
                        } else {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permission to view XQuery source for: " + path + " denied. Must be explicitly defined in descriptor.xml");
                            return;
                        }
                    } else {
                        try {
                            String result = resource.execute(outputProperties);
                            encoding = outputProperties.getProperty(OutputKeys.ENCODING, encoding);
                            String mimeType = outputProperties.getProperty(OutputKeys.MEDIA_TYPE, "text/html");
                            writeResponse(response, result, mimeType, encoding);
                        } catch (XPathException e) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            writeResponse(response, formatXPathException(null, path, e), "text/html", encoding);
                        }
                    }
                    return;
                }
            }
            String query = request.getParameter("_xpath");
            if (query == null) query = request.getParameter("_query");
            if (query != null) {
                LOG.debug("query = " + query);
                int howmany = 10;
                int start = 1;
                boolean wrap = true;
                String p_howmany = request.getParameter("_howmany");
                if (p_howmany != null) {
                    try {
                        howmany = Integer.parseInt(p_howmany);
                    } catch (NumberFormatException nfe) {
                        throw new BadRequestException("Parameter _howmany should be an int");
                    }
                }
                String p_start = (String) request.getParameter("_start");
                if (p_start != null) {
                    try {
                        start = Integer.parseInt(p_start);
                    } catch (NumberFormatException nfe) {
                        throw new BadRequestException("Parameter _start should be an int");
                    }
                }
                if ((option = request.getParameter("_wrap")) != null) wrap = option.equals("yes");
                try {
                    RESTXQueryStringResource xqResource = new RESTXQueryStringResource(query, path, broker, request, response, formEncoding, containerEncoding, howmany, start, wrap);
                    String result = xqResource.execute(outputProperties);
                    encoding = outputProperties.getProperty(OutputKeys.ENCODING, encoding);
                    String mimeType = outputProperties.getProperty(OutputKeys.MEDIA_TYPE, "text/html");
                    writeResponse(response, result, mimeType, encoding);
                } catch (XPathException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    writeResponse(response, formatXPathException(query, path, e), "text/html", encoding);
                }
            } else {
                if (resource == null) {
                    RESTCollection collection = RESTCollectionFactory.getCollection(path, broker);
                    if (collection != null) {
                        writeResponse(response, collection.serialize(defaultProperties), "text/xml", encoding);
                    } else {
                        throw new NotFoundException("Document " + path + " not found");
                    }
                } else {
                    writeResourceAs(resource, broker, stylesheet, encoding, null, outputProperties, response);
                }
            }
        } finally {
            if (resource != null) resource.releaseReadLock();
        }
    }

    private void writeResourceAs(RESTResource resource, DBBroker broker, String stylesheet, String encoding, String asMimeType, Properties outputProperties, HttpServletResponse response) throws BadRequestException, PermissionDeniedException, IOException {
        if (resource.isXML()) {
            try {
                resource.serializeResponse(stylesheet, encoding, outputProperties, asMimeType, useDynamicContentType);
            } catch (SAXException saxe) {
                LOG.warn(saxe);
                throw new BadRequestException("Error while serializing XML: " + saxe.getMessage());
            }
        } else {
            resource.binaryResponse(asMimeType);
        }
    }

    /**
     * Handle HEAD request
     * 
     * @param broker
     * @param request
     * @param response
     * @param path
     */
    public void doHead(DBBroker broker, HttpServletRequest request, HttpServletResponse response, String path) throws BadRequestException, PermissionDeniedException, NotFoundException, IOException {
        RESTResource resource = null;
        try {
            resource = RESTResourceFactory.getResource(path, broker, request, response, formEncoding, containerEncoding);
            if (resource == null) {
                throw new NotFoundException("Resource " + path + " not found");
            }
            response.setContentType(resource.getMimeType());
            response.setContentLength(resource.getContentLength());
            response.addDateHeader("Last-Modified", resource.getLastModified());
            response.addDateHeader("Created", resource.getCreated());
        } finally {
            if (resource != null) resource.releaseReadLock();
        }
    }

    /**
     * Handles POST requests. If the path leads to a binary resource with
     * mime-type "application/xquery", that resource will be read and executed
     * by the XQuery engine. Otherwise, the request content is loaded and parsed
     * as XML. It may either contain an XUpdate or a query request.
     *
     * Use Cases -
     * 1) POST to an existing XQuery document which is exceuted
     * 2) POST a Query document onto a resource (file or collection), containing either XQuery or XPath
     * 3) POST an XUpdate document onto a resource (either a file or collection)
     *
     * @param broker
     * @param request
     * @param response
     * @param path
     * @throws BadRequestException
     * @throws PermissionDeniedException
     */
    public void doPost(DBBroker broker, HttpServletRequest request, HttpServletResponse response, String path) throws BadRequestException, PermissionDeniedException, IOException {
        if (request.getCharacterEncoding() == null) request.setCharacterEncoding(formEncoding);
        Properties outputProperties = new Properties(defaultProperties);
        RESTResource resource = null;
        try {
            resource = RESTResourceFactory.getResource(path, broker, request, response, formEncoding, containerEncoding);
            if (resource != null) {
                if (resource.isXQuery()) {
                    try {
                        String result = resource.execute(outputProperties);
                        String encoding = outputProperties.getProperty(OutputKeys.ENCODING, "UTF-8");
                        String mimeType = outputProperties.getProperty(OutputKeys.MEDIA_TYPE, "text/html");
                        writeResponse(response, result, mimeType, encoding);
                    } catch (XPathException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        writeResponse(response, formatXPathException(null, path, e), "text/html", "UTF-8");
                    }
                    return;
                }
            }
            String requestContent = getRequestContent(request);
            Document doc = createDocument(requestContent);
            Element root = doc.getDocumentElement();
            String rootNS = root.getNamespaceURI();
            if (rootNS != null && rootNS.equals(NS)) {
                if (root.getLocalName().equals("query")) {
                    String mime = "text/xml";
                    String option = root.getAttribute("mime");
                    if ((option != null) && (!option.equals(""))) {
                        mime = option;
                    }
                    try {
                        String result = executeQueryDocument(path, root, broker, request, response);
                        writeResponse(response, result, mime, outputProperties.getProperty(OutputKeys.ENCODING, "UTF-8"));
                    } catch (XPathException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        writeResponse(response, formatXPathException(null, path, e), "text/html", "UTF-8");
                    }
                }
            } else if (rootNS != null && rootNS.equals(XUPDATE_NS)) {
                String result = executeXUpdate(path, requestContent, resource, broker);
                writeResponse(response, result, "text/xml", "UTF-8");
            } else {
                throw new BadRequestException("Unknown XML root element: " + root.getNodeName());
            }
        } finally {
            if (resource != null) resource.releaseReadLock();
        }
    }

    /**
     * Process a Query Document
     * 
     * <query start="" max="" enclose="" mime="">
     * 	<properties>
     * 		<property>
     * 			<name/>
     * 			<value/>
     * 		</property>
     * 	</properties>
     * 	<text/>
     * </query>
     * 
     * @param documentElement	The document element of the Query document
     * @param broker	The database broker
     * @param transaction	The transaction to use for execution
     * 
     * @return The String result of executing the query
     */
    private String executeQueryDocument(String path, Element documentElement, DBBroker broker, HttpServletRequest request, HttpServletResponse response) throws XPathException, BadRequestException {
        int howmany = 10;
        int start = 1;
        boolean enclose = true;
        String query = null;
        Properties outputProperties = new Properties(defaultProperties);
        String option = documentElement.getAttribute("start");
        if (option != null) {
            try {
                start = Integer.parseInt(option);
            } catch (NumberFormatException e) {
            }
        }
        option = documentElement.getAttribute("max");
        if (option != null) {
            try {
                howmany = Integer.parseInt(option);
            } catch (NumberFormatException e) {
            }
        }
        option = documentElement.getAttribute("enclose");
        if (option != null) {
            if (option.equals("no")) enclose = false;
        }
        NodeList children = documentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNamespaceURI().equals(NS)) {
                if (child.getLocalName().equals("properties")) {
                    Node node = child.getFirstChild();
                    while (node != null) {
                        if (node.getNodeType() == Node.ELEMENT_NODE && node.getNamespaceURI().equals(NS) && node.getLocalName().equals("property")) {
                            Element property = (Element) node;
                            String key = property.getAttribute("name");
                            String value = property.getAttribute("value");
                            LOG.debug(key + " = " + value);
                            if (key != null && value != null) outputProperties.setProperty(key, value);
                        }
                        node = node.getNextSibling();
                    }
                } else if (child.getLocalName().equals("text")) {
                    StringBuffer buf = new StringBuffer();
                    Node next = child.getFirstChild();
                    while (next != null) {
                        if (next.getNodeType() == Node.TEXT_NODE || next.getNodeType() == Node.CDATA_SECTION_NODE) {
                            buf.append(next.getNodeValue());
                        }
                        next = next.getNextSibling();
                    }
                    query = buf.toString();
                }
            }
        }
        if (query != null) {
            RESTXQueryStringResource resource = new RESTXQueryStringResource(query, path, broker, request, response, formEncoding, containerEncoding, howmany, start, enclose);
            return resource.execute(outputProperties);
        } else {
            throw new BadRequestException("No query specified");
        }
    }

    /**
     * Executes an XUpdate
     * 
     * @param path	The path of the resource to perform the XUpdate against
     * @param xupdate	The XUpdate itself
     * @param resource	The RESTResource to perform this update against or null if a collection
     * @param broker	The database broker to use for this XUpdate
     * 
     * @return An XML String describing the result of the XUpdate operation
     * <exist:modifications xmlns:exist="http://exist.sourceforge.net/NS/exist" count="">
	 * 	[n] modifications processed.
	 * </exist:modifications>
     */
    private String executeXUpdate(String path, String xupdate, RESTResource resource, DBBroker broker) throws BadRequestException, PermissionDeniedException {
        LOG.debug("Got xupdate request: " + xupdate);
        DocumentSet docs = new DocumentSet();
        if (resource != null) {
            if (resource instanceof RESTDBResource) {
                docs.add((DocumentImpl) resource.getResource());
            } else {
                throw new BadRequestException("XUpdate's are not permitted against non database resources.");
            }
        } else {
            RESTCollection collection = RESTCollectionFactory.getCollection(path, broker);
            if (collection instanceof RESTDBCollection) {
                ((Collection) collection.getCollection()).allDocs(broker, docs, true, true);
            } else {
                throw new BadRequestException("XUpdate's are not permitted against non database resources.");
            }
        }
        TransactionManager transact = broker.getBrokerPool().getTransactionManager();
        Txn transaction = transact.beginTransaction();
        try {
            XUpdateProcessor processor = new XUpdateProcessor(broker, docs, AccessContext.REST);
            Modification modifications[] = processor.parse(new InputSource(new StringReader(xupdate)));
            long mods = 0;
            for (int i = 0; i < modifications.length; i++) {
                mods += modifications[i].process(transaction);
                broker.flush();
            }
            transact.commit(transaction);
            return "<?xml version='1.0'?>\n" + "<exist:modifications xmlns:exist='" + NS + "' count='" + mods + "'>" + mods + "modifications processed.</exist:modifications>";
        } catch (SAXException e) {
            transact.abort(transaction);
            Exception cause = e;
            if (e.getException() != null) cause = e.getException();
            LOG.debug("SAX exception while parsing request: " + cause.getMessage(), cause);
            throw new BadRequestException("SAX exception while parsing request: " + cause.getMessage());
        } catch (ParserConfigurationException e) {
            transact.abort(transaction);
            throw new BadRequestException("Parser exception while parsing request: " + e.getMessage());
        } catch (XPathException e) {
            transact.abort(transaction);
            throw new BadRequestException("Query exception while parsing request: " + e.getMessage());
        } catch (IOException e) {
            transact.abort(transaction);
            throw new BadRequestException("IO exception while parsing request: " + e.getMessage());
        } catch (EXistException e) {
            transact.abort(transaction);
            throw new BadRequestException(e.getMessage());
        } catch (PermissionDeniedException e) {
            transact.abort(transaction);
            throw e;
        } catch (LockException e) {
            transact.abort(transaction);
            throw new PermissionDeniedException(e.getMessage());
        }
    }

    /**
     * Creates an input source from a URL location with an optional
     * known charset.
     */
    private InputSource createInputSource(String charset, URI location) throws java.io.IOException {
        if (charset == null) {
            return new InputSource(location.toASCIIString());
        } else {
            InputSource source = new InputSource(new InputStreamReader(location.toURL().openStream(), charset));
            source.setSystemId(location.toASCIIString());
            return source;
        }
    }

    /**
     * Handles PUT requests. The request content is stored as a new resource at
     * the specified location. If the resource already exists, it is overwritten if the
     * user has write permissions.
     *
     * The resource type depends on the content type specified in the HTTP header.
     * The content type will be looked up in the global mime table. If the corresponding
     * mime type is not a know XML mime type, the resource will be stored as a binary
     * resource.
     *
     * @param broker
     * @param tempFile The temp file from which the PUT will get its content
     * @param pathUri The path to which the file should be stored
     * @param request
     * @param response
     * @throws BadRequestException
     * @throws PermissionDeniedException
     */
    public void doPut(DBBroker broker, File tempFile, XmldbURI pathUri, HttpServletRequest request, HttpServletResponse response) throws BadRequestException, PermissionDeniedException, IOException {
        if (!RESTResourceFactory.isDBPath(pathUri.toString())) throw new BadRequestException("PUT method may only be used for database resources or collections");
        if (tempFile == null) throw new BadRequestException("No request content found for PUT");
        TransactionManager transact = broker.getBrokerPool().getTransactionManager();
        Txn transaction = transact.beginTransaction();
        try {
            XmldbURI docUri = pathUri.lastSegment();
            XmldbURI collUri = pathUri.removeLastSegment();
            if (docUri == null || collUri == null) {
                transact.abort(transaction);
                throw new BadRequestException("Bad path: " + pathUri);
            }
            Collection collection = broker.getCollection(collUri);
            if (collection == null) {
                LOG.debug("creating collection " + collUri);
                collection = broker.getOrCreateCollection(transaction, collUri);
                broker.saveCollection(transaction, collection);
            }
            MimeType mime;
            String contentType = request.getContentType();
            String charset = null;
            if (contentType != null) {
                int semicolon = contentType.indexOf(';');
                if (semicolon > 0) {
                    contentType = contentType.substring(0, semicolon).trim();
                    int equals = contentType.indexOf('=', semicolon);
                    if (equals > 0) {
                        String param = contentType.substring(semicolon + 1, equals).trim();
                        if (param.compareToIgnoreCase("charset=") == 0) {
                            charset = param.substring(equals + 1).trim();
                        }
                    }
                }
                mime = MimeTable.getInstance().getContentType(contentType);
            } else {
                mime = MimeTable.getInstance().getContentTypeFor(docUri);
                if (mime != null) contentType = mime.getName();
            }
            if (mime == null) mime = MimeType.BINARY_TYPE;
            if (mime.isXMLType()) {
                URI url = tempFile.toURI();
                IndexInfo info = collection.validateXMLResource(transaction, broker, docUri, createInputSource(charset, url));
                info.getDocument().getMetadata().setMimeType(contentType);
                collection.store(transaction, broker, info, createInputSource(charset, url), false);
                response.sendError(HttpServletResponse.SC_OK, "Document " + docUri + " stored.");
            } else {
                FileInputStream is = new FileInputStream(tempFile);
                collection.addBinaryResource(transaction, broker, docUri, is, contentType, (int) tempFile.length());
                is.close();
                response.sendError(HttpServletResponse.SC_OK, "Document " + docUri + " stored as binary resource.");
            }
            transact.commit(transaction);
        } catch (SAXParseException e) {
            transact.abort(transaction);
            throw new BadRequestException("Parsing exception at " + e.getLineNumber() + "/" + e.getColumnNumber() + ": " + e.toString());
        } catch (SAXException e) {
            transact.abort(transaction);
            Exception o = e.getException();
            if (o == null) o = e;
            throw new BadRequestException("Parsing exception: " + o.getMessage());
        } catch (EXistException e) {
            transact.abort(transaction);
            throw new BadRequestException("Internal error: " + e.getMessage());
        } catch (TriggerException e) {
            transact.abort(transaction);
            throw new PermissionDeniedException(e.getMessage());
        } catch (LockException e) {
            transact.abort(transaction);
            throw new PermissionDeniedException(e.getMessage());
        }
    }

    /**
     * Handle DELETE request
     * 
     * @param broker
     * @param request
     * @param response
     * @param path
     */
    public void doDelete(DBBroker broker, String path, HttpServletResponse response) throws BadRequestException, PermissionDeniedException, NotFoundException, IOException {
        if (!RESTResourceFactory.isDBPath(path)) {
            throw new BadRequestException("DELETE method may only be used for database resources or collections");
        }
        TransactionManager transact = broker.getBrokerPool().getTransactionManager();
        Txn txn = transact.beginTransaction();
        try {
            XmldbURI pathUri = XmldbURI.create(path);
            Collection collection = broker.getCollection(pathUri);
            if (collection != null) {
                LOG.debug("removing collection " + pathUri);
                broker.removeCollection(txn, collection);
                response.sendError(HttpServletResponse.SC_OK, "Collection " + pathUri + " removed.");
            } else {
                DocumentImpl doc = (DocumentImpl) broker.getXMLResource(pathUri);
                if (doc == null) {
                    transact.abort(txn);
                    throw new NotFoundException("No document or collection found " + "for path: " + pathUri);
                } else {
                    LOG.debug("removing document " + pathUri);
                    if (doc.getResourceType() == DocumentImpl.BINARY_FILE) doc.getCollection().removeBinaryResource(txn, broker, pathUri.lastSegment()); else doc.getCollection().removeXMLResource(txn, broker, pathUri.lastSegment());
                    response.sendError(HttpServletResponse.SC_OK, "Document " + pathUri + " removed.");
                }
            }
            transact.commit(txn);
        } catch (TriggerException e) {
            transact.abort(txn);
            throw new PermissionDeniedException("Trigger failed: " + e.getMessage());
        } catch (LockException e) {
            transact.abort(txn);
            throw new PermissionDeniedException("Could not acquire lock: " + e.getMessage());
        } catch (TransactionException e) {
            transact.abort(txn);
            LOG.warn("Transaction aborted: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to create an XML Document from a String
     * 
     * @param content	The XML String content
     * 
     * @return The XML Document
     */
    private Document createDocument(String content) throws UnsupportedEncodingException, IOException, BadRequestException {
        InputSource src = new InputSource(new StringReader(content));
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.parse(src);
        } catch (Exception e) {
            LOG.warn(e);
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * Get's the content of a Request
     * 
     * @param request	The HttpServletRequest whoose content should be retreived
     * 
     * @return The content of the Request as a String
     */
    private String getRequestContent(HttpServletRequest request) throws IOException, UnsupportedEncodingException {
        String encoding = request.getCharacterEncoding();
        if (encoding == null) encoding = "UTF-8";
        InputStream is = request.getInputStream();
        Reader reader = new InputStreamReader(is, encoding);
        StringWriter content = new StringWriter();
        char ch[] = new char[4096];
        int len = 0;
        while ((len = reader.read(ch)) > -1) content.write(ch, 0, len);
        String xml = content.toString();
        return xml;
    }

    /**
     * @param query
     * @param e
     */
    private String formatXPathException(String query, String path, XPathException e) {
        StringWriter writer = new StringWriter();
        writer.write(QUERY_ERROR_HEAD);
        writer.write("<p class=\"path\"><span class=\"high\">Path</span>: ");
        writer.write("<a href=\"");
        writer.write(path);
        writer.write("\">");
        writer.write(path);
        writer.write("</a></p>");
        writer.write("<p class=\"errmsg\">");
        writer.write(e.getMessage());
        writer.write("</p>");
        if (query != null) {
            writer.write("<p><span class=\"high\">Query</span>:</p><pre>");
            writer.write(query);
            writer.write("</pre>");
        }
        writer.write("</body></html>");
        return writer.toString();
    }

    private void writeResponse(HttpServletResponse response, String data, String contentType, String encoding) throws IOException {
        if (!response.isCommitted()) {
            if (contentType != null) {
                int semicolon = contentType.indexOf(';');
                if (semicolon != Constants.STRING_NOT_FOUND) {
                    contentType = contentType.substring(0, semicolon);
                }
                response.setContentType(contentType + "; charset=" + encoding);
            }
            OutputStream is = response.getOutputStream();
            is.write(data.getBytes(encoding));
        }
    }
}
