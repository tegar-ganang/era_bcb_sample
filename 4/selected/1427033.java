package com.c2b2.ipoint.processing.webdav;

import com.c2b2.ipoint.business.UserServices;
import com.c2b2.ipoint.model.User;
import com.c2b2.ipoint.processing.PortalRequest;
import com.c2b2.ipoint.processing.PortalRequestException;
import com.c2b2.ipoint.model.DocumentRepository;
import com.c2b2.ipoint.model.HibernateUtil;
import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.Property;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.catalina.util.MD5Encoder;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.URLEncoder;
import org.apache.catalina.util.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import sun.misc.BASE64Decoder;

/**
 * Servlet which provides support for WebDAV level 2.
 * 
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 * 
 * @author Remy Maucherat
 */
public class WebdavServlet extends HttpServlet {

    private static final String METHOD_HEAD = "HEAD";

    private static final String METHOD_PROPFIND = "PROPFIND";

    private static final String METHOD_PROPPATCH = "PROPPATCH";

    private static final String METHOD_MKCOL = "MKCOL";

    private static final String METHOD_COPY = "COPY";

    private static final String METHOD_MOVE = "MOVE";

    private static final String METHOD_PUT = "PUT";

    private static final String METHOD_GET = "GET";

    private static final String METHOD_OPTIONS = "OPTIONS";

    private static final String METHOD_DELETE = "DELETE";

    /**
   * MD5 message digest provider.
   */
    protected static MessageDigest md5Helper;

    /**
   * The MD5 helper object for this class.
   */
    protected static final MD5Encoder md5Encoder = new MD5Encoder();

    /**
   * Default depth is infite.
   */
    private static final int INFINITY = 3;

    /**
   * PROPFIND - Specify a property mask.
   */
    private static final int FIND_BY_PROPERTY = 0;

    /**
   * PROPFIND - Display all properties.
   */
    private static final int FIND_ALL_PROP = 1;

    /**
   * PROPFIND - Return property names.
   */
    private static final int FIND_PROPERTY_NAMES = 2;

    /**
   * size of the io-buffer
   */
    private static int BUF_SIZE = 50000;

    /**
   * Default namespace.
   */
    protected static final String DEFAULT_NAMESPACE = "DAV:";

    /**
   * Simple date format for the creation date ISO representation (partial).
   */
    protected static final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
   * indicates that the store is readonly ?
   */
    private static final boolean readOnly = false;

    static {
        creationDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
   * Array containing the safe characters set.
   */
    protected static URLEncoder urlEncoder;

    static {
        urlEncoder = new URLEncoder();
        urlEncoder.addSafeCharacter('-');
        urlEncoder.addSafeCharacter('_');
        urlEncoder.addSafeCharacter('.');
        urlEncoder.addSafeCharacter('*');
        urlEncoder.addSafeCharacter('/');
    }

    private ResourceLocks fResLocks = null;

    private IWebdavStorage fStore = null;

    private static final String DEBUG_PARAMETER = "storeDebug";

    private static int fdebug = -1;

    private WebdavStoreFactory fFactory;

    private Hashtable fParameter;

    /**
   * Initialize this servlet.
   */
    public void init() throws ServletException {
        try {
            md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
        String clazz = getServletConfig().getInitParameter("ResourceHandlerImplementation");
        try {
            fFactory = new WebdavStoreFactory(WebdavServlet.class.getClassLoader().loadClass(clazz));
            fParameter = new Hashtable();
            Enumeration initParameterNames = getServletConfig().getInitParameterNames();
            while (initParameterNames.hasMoreElements()) {
                String key = (String) initParameterNames.nextElement();
                fParameter.put(key, getServletConfig().getInitParameter(key));
            }
            fStore = fFactory.getStore();
            fResLocks = new ResourceLocks();
            String debugString = (String) fParameter.get(DEBUG_PARAMETER);
            if (debugString == null) {
                fdebug = 0;
            } else {
                fdebug = Integer.parseInt(debugString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /**
   * Handles the special WebDAV methods.
   */
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod();
        if (fdebug == 1) {
            System.out.println("-----------");
            System.out.println("WebdavServlet\n request: method = " + method);
            System.out.println("Zeit: " + System.currentTimeMillis());
            System.out.println("path: " + getRelativePath(req));
            System.out.println("-----------");
            Enumeration e = req.getHeaderNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("header: " + s + " " + req.getHeader(s));
            }
            e = req.getAttributeNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("attribute: " + s + " " + req.getAttribute(s));
            }
            e = req.getParameterNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("parameter: " + s + " " + req.getParameter(s));
            }
        }
        try {
            PortalRequest.startPortalRequest(req, resp);
            if (PortalRequest.getCurrentRequest().getCurrentUser().isGuest()) {
                if (req.getHeader("Authorization") == null) {
                    resp.setHeader("WWW-Authenticate", "Basic realm=\"iPoint\"");
                    resp.sendError(resp.SC_UNAUTHORIZED, "Please Login to iPoint Portal");
                    return;
                } else {
                    BASE64Decoder decoder = new BASE64Decoder();
                    String header = req.getHeader("Authorization");
                    int basic = header.indexOf("Basic");
                    int passwordStart = basic + 6;
                    String decodedAuth = new String(decoder.decodeBuffer(header.substring(passwordStart)));
                    String auth[] = decodedAuth.split(":");
                    UserServices us = new UserServices();
                    User user = us.authenticate(auth[0], auth[1]);
                    if (user == null || user.isGuest() || !user.isActiveUser()) {
                        resp.setHeader("WWW-Authenticate", "Basic realm=\"iPoint\"");
                        resp.sendError(resp.SC_UNAUTHORIZED, "Please Login to iPoint Portal");
                        return;
                    }
                }
            } else {
            }
            fStore.begin(req.getUserPrincipal(), fParameter);
            fStore.checkAuthentication();
            resp.setStatus(WebdavStatus.SC_OK);
            try {
                if (method.equals(METHOD_PROPFIND)) {
                    doPropfind(req, resp);
                } else if (method.equals(METHOD_PROPPATCH)) {
                    doProppatch(req, resp);
                } else if (method.equals(METHOD_MKCOL)) {
                    doMkcol(req, resp);
                } else if (method.equals(METHOD_COPY)) {
                    doCopy(req, resp);
                } else if (method.equals(METHOD_MOVE)) {
                    doMove(req, resp);
                } else if (method.equals(METHOD_PUT)) {
                    doPut(req, resp);
                } else if (method.equals(METHOD_GET)) {
                    doGet(req, resp, true);
                } else if (method.equals(METHOD_OPTIONS)) {
                    doOptions(req, resp);
                } else if (method.equals(METHOD_HEAD)) {
                    doHead(req, resp);
                } else if (method.equals(METHOD_DELETE)) {
                    doDelete(req, resp);
                } else {
                    super.service(req, resp);
                }
                fStore.commit();
                PortalRequest.getCurrentRequest().endPortalRequest();
            } catch (IOException e) {
                e.printStackTrace();
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                fStore.rollback();
                throw new ServletException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                PortalRequest.getCurrentRequest().abandonPortalRequest();
            } catch (PortalRequestException pre) {
                pre.printStackTrace();
            }
            throw new ServletException(e);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
   * goes recursive through all folders. used by propfind
   * 
   */
    private void recursiveParseProperties(String currentPath, HttpServletRequest req, XMLWriter generatedXML, int propertyFindType, Vector properties, int depth) throws IOException, ServletException {
        parseProperties(req, generatedXML, currentPath, propertyFindType, properties);
        String[] names;
        if (currentPath.equals("/")) {
            try {
                List<DocumentRepository> documentRepositories = DocumentRepository.getAllDocumentRepositories();
                List<DocumentRepository> visibleDocumentRepositories = new ArrayList<DocumentRepository>();
                for (DocumentRepository documentRepository : documentRepositories) {
                    if (documentRepository.isVisibleTo(PortalRequest.getCurrentRequest().getCurrentUser())) {
                        visibleDocumentRepositories.add(documentRepository);
                    } else {
                    }
                }
                names = new String[visibleDocumentRepositories.size()];
                int i = 0;
                for (DocumentRepository documentRepository : visibleDocumentRepositories) {
                    int lastSlash = documentRepository.getDirectoryPath().lastIndexOf(File.separator);
                    names[i] = documentRepository.getDirectoryPath().substring(lastSlash + 1) + "/";
                    i++;
                }
            } catch (PersistentModelException pme) {
                throw new ServletException("Could not retrieve set of Document Repositories from persistent Store");
            }
        } else {
            names = fStore.getChildrenNames(currentPath);
        }
        if ((names != null) && (depth > 0)) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String newPath = currentPath;
                if (!(newPath.endsWith("/"))) {
                    newPath += "/";
                }
                newPath += name;
                recursiveParseProperties(newPath, req, generatedXML, propertyFindType, properties, depth - 1);
            }
        }
    }

    /**
   * overwrites propNode and type, parsed from xml input stream
   * 
   * @param propNode
   * @param type
   * @param req
   * @throws ServletException
   */
    private void getPropertyNodeAndType(Node propNode, int type, ServletRequest req) throws ServletException {
        if (req.getContentLength() != 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            try {
                Document document = documentBuilder.parse(new InputSource(req.getInputStream()));
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch(currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            if (currentNode.getNodeName().endsWith("prop")) {
                                type = FIND_BY_PROPERTY;
                                propNode = currentNode;
                            }
                            if (currentNode.getNodeName().endsWith("propname")) {
                                type = FIND_PROPERTY_NAMES;
                            }
                            if (currentNode.getNodeName().endsWith("allprop")) {
                                type = FIND_ALL_PROP;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
            }
        } else {
            type = FIND_ALL_PROP;
        }
    }

    private String getParentPath(String path) {
        int slash = path.lastIndexOf('/');
        if (slash != -1) {
            return path.substring(0, slash);
        }
        return null;
    }

    /**
   * Return JAXP document builder instance.
   */
    private DocumentBuilder getDocumentBuilder() throws ServletException {
        DocumentBuilder documentBuilder = null;
        DocumentBuilderFactory documentBuilderFactory = null;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServletException("jaxp failed");
        }
        return documentBuilder;
    }

    /**
   * Return the relative path associated with this servlet.
   * 
   * @param request
   *            The servlet request we are processing
   */
    protected String getRelativePath(HttpServletRequest request) {
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            String result = (String) request.getAttribute("javax.servlet.include.path_info");
            if (result == null) result = (String) request.getAttribute("javax.servlet.include.servlet_path");
            if ((result == null) || (result.equals(""))) result = "/";
            return (result);
        }
        String result = request.getPathInfo();
        if (result == null) {
            if (request.getRequestURI().equals(request.getContextPath() + "/webdav")) {
                result = "/";
            } else {
                result = request.getServletPath();
            }
        }
        if ((result == null) || (result.equals(""))) {
            result = "/";
        }
        return (result);
    }

    private Vector getPropertiesFromXML(Node propNode) {
        Vector properties;
        properties = new Vector();
        NodeList childList = propNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node currentNode = childList.item(i);
            switch(currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    String nodeName = currentNode.getNodeName();
                    String propertyName = null;
                    if (nodeName.indexOf(':') != -1) {
                        propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
                    } else {
                        propertyName = nodeName;
                    }
                    properties.addElement(propertyName);
                    break;
            }
        }
        return properties;
    }

    /**
   * reads the depth header from the request and returns it as a int
   * 
   * @param req
   * @return
   */
    private int getDepth(HttpServletRequest req) {
        int depth = INFINITY;
        String depthStr = req.getHeader("Depth");
        if (depthStr != null) {
            if (depthStr.equals("0")) {
                depth = 0;
            } else if (depthStr.equals("1")) {
                depth = 1;
            } else if (depthStr.equals("infinity")) {
                depth = INFINITY;
            }
        }
        return depth;
    }

    /**
   * removes a / at the end of the path string, if present
   * 
   * @param path
   * @return the path without trailing /
   */
    private String getCleanPath(String path) {
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    /**
   * OPTIONS Method.</br>
   * 
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String lockOwner = "doOptions" + System.currentTimeMillis() + req.toString();
        String path = getRelativePath(req);
        if (fResLocks.lock(path, lockOwner, false, 0)) {
            try {
                resp.addHeader("DAV", "1, 2");
                String methodsAllowed = determineMethodsAllowed(path, fStore.objectExists(path), fStore.isFolder(path));
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
            } finally {
                fResLocks.unlock(path, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
   * PROPFIND Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doPropfind(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("DAV", "1, 2");
        resp.addHeader("MS-Author-Via", "DAV");
        String lockOwner = "doPropfind" + System.currentTimeMillis() + req.toString();
        String path = getRelativePath(req);
        if (!isInRepositoryVisibleToCurrentUser(path)) {
            resp.sendError(resp.SC_FORBIDDEN);
            return;
        }
        int depth = getDepth(req);
        if (fResLocks.lock(path, lockOwner, false, depth)) {
            try {
                if (!fStore.objectExists(path)) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
                Vector properties = null;
                path = getCleanPath(getRelativePath(req));
                int propertyFindType = FIND_ALL_PROP;
                Node propNode = null;
                getPropertyNodeAndType(propNode, propertyFindType, req);
                if (propertyFindType == FIND_BY_PROPERTY) {
                    properties = getPropertiesFromXML(propNode);
                }
                resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
                resp.setContentType("text/xml; charset=\"utf-8\"");
                XMLWriter generatedXML = new XMLWriter(resp.getWriter());
                generatedXML.writeXMLHeader();
                generatedXML.writeElement("d", "multistatus" + generateNamespaceDeclarations(), XMLWriter.OPENING);
                if (depth == 0) {
                    parseProperties(req, generatedXML, path, propertyFindType, properties);
                } else {
                    recursiveParseProperties(path, req, generatedXML, propertyFindType, properties, depth);
                }
                generatedXML.writeElement("d", "multistatus", XMLWriter.CLOSING);
                generatedXML.sendData();
            } finally {
                fResLocks.unlock(path, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
   * PROPPATCH Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doProppatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
   * GET Method
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp, boolean includeBody) throws ServletException, IOException {
        String lockOwner = "doGet" + System.currentTimeMillis() + req.toString();
        String path = getRelativePath(req);
        if (!isInRepositoryVisibleToCurrentUser(path)) {
            resp.sendError(resp.SC_FORBIDDEN);
            return;
        }
        if (fResLocks.lock(path, lockOwner, false, 0)) {
            try {
                if (fStore.isResource(path)) {
                    if (path.endsWith("/") || (path.endsWith("\\"))) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                    } else {
                        long lastModified = fStore.getLastModified(path).getTime();
                        resp.setDateHeader("last-modified", lastModified);
                        long resourceLength = fStore.getResourceLength(path);
                        if (resourceLength > 0) {
                            if (resourceLength <= Integer.MAX_VALUE) {
                                resp.setContentLength((int) resourceLength);
                            } else {
                                resp.setHeader("content-length", "" + resourceLength);
                            }
                        }
                        String mimeType = getServletContext().getMimeType(path);
                        if (mimeType != null) {
                            resp.setContentType(mimeType);
                        }
                        if (includeBody) {
                            OutputStream out = resp.getOutputStream();
                            InputStream in = fStore.getResourceContent(path);
                            try {
                                int read = -1;
                                byte[] copyBuffer = new byte[BUF_SIZE];
                                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                                    out.write(copyBuffer, 0, read);
                                }
                            } finally {
                                in.close();
                                out.flush();
                                out.close();
                            }
                        }
                    }
                } else {
                    if (includeBody && fStore.isFolder(path)) {
                        OutputStream out = resp.getOutputStream();
                        String[] children = fStore.getChildrenNames(path);
                        StringBuffer childrenTemp = new StringBuffer();
                        childrenTemp.append("Contents of this Folder:\n");
                        for (int i = 0; i < children.length; i++) {
                            childrenTemp.append(children[i]);
                            childrenTemp.append("\n");
                        }
                        out.write(childrenTemp.toString().getBytes());
                    } else {
                        if (!fStore.objectExists(path)) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                        }
                    }
                }
            } finally {
                fResLocks.unlock(path, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
   * HEAD Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp, false);
    }

    /**
   * MKCOL Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doMkcol(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getContentLength() != 0) {
            resp.sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
        } else {
            String path = getRelativePath(req);
            String parentPath = getParentPath(path);
            if (!readOnly && isInRepositoryEditableByCurrentUser(parentPath)) {
                String lockOwner = "doMkcol" + System.currentTimeMillis() + req.toString();
                if (fResLocks.lock(path, lockOwner, true, 0)) {
                    try {
                        if (parentPath != null && fStore.isFolder(parentPath)) {
                            if (!fStore.objectExists(path)) {
                                fStore.createFolder(path);
                            } else {
                                String methodsAllowed = determineMethodsAllowed(path, true, fStore.isFolder(path));
                                resp.addHeader("Allow", methodsAllowed);
                                resp.sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
                            }
                        } else {
                            resp.sendError(WebdavStatus.SC_CONFLICT);
                        }
                    } finally {
                        fResLocks.unlock(path, lockOwner);
                    }
                } else {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            }
        }
    }

    /**
   * DELETE Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRelativePath(req);
        if (!readOnly && !isDocumentRepository(path) && isInRepositoryEditableByCurrentUser(path)) {
            String lockOwner = "doDelete" + System.currentTimeMillis() + req.toString();
            if (fResLocks.lock(path, lockOwner, true, -1)) {
                try {
                    Hashtable errorList = new Hashtable();
                    deleteResource(path, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } finally {
                    fResLocks.unlock(path, lockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
   * Process a POST request for the specified resource.
   * 
   * @param req
   *            The servlet request we are processing
   * @param resp
   *            The servlet response we are creating
   * 
   * @exception IOException
   *                if an input/output error occurs
   * @exception ServletException
   *                if a servlet-specified error occurs
   */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRelativePath(req);
        if (!readOnly && isInRepositoryEditableByCurrentUser(path)) {
            String parentPath = getParentPath(path);
            String lockOwner = "doPut" + System.currentTimeMillis() + req.toString();
            if (fResLocks.lock(path, lockOwner, true, -1)) {
                try {
                    if (parentPath != null && fStore.isFolder(parentPath) && !fStore.isFolder(path)) {
                        if (!fStore.objectExists(path)) {
                            fStore.createResource(path);
                            resp.setStatus(HttpServletResponse.SC_CREATED);
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }
                        fStore.setResourceContent(path, req.getInputStream(), null, null);
                        resp.setContentLength((int) fStore.getResourceLength(path));
                    } else {
                        resp.sendError(WebdavStatus.SC_CONFLICT);
                    }
                } finally {
                    fResLocks.unlock(path, lockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
   * COPY Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doCopy(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRelativePath(req);
        if (!readOnly) {
            String lockOwner = "doCopy" + System.currentTimeMillis() + req.toString();
            if (fResLocks.lock(path, lockOwner, false, -1)) {
                try {
                    copyResource(req, resp);
                } finally {
                    fResLocks.unlock(path, lockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
   * MOVE Method.
   * 
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws ServletException
   * @throws IOException
   */
    protected void doMove(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getRelativePath(req);
        if (!readOnly && !isDocumentRepository(path) && isInRepositoryEditableByCurrentUser(path)) {
            String lockOwner = "doMove" + System.currentTimeMillis() + req.toString();
            if (fResLocks.lock(path, lockOwner, false, -1)) {
                try {
                    if (copyResource(req, resp)) {
                        Hashtable errorList = new Hashtable();
                        deleteResource(path, errorList, req, resp);
                        if (!errorList.isEmpty()) {
                            sendReport(req, resp, errorList);
                        }
                    } else {
                        resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                    }
                } finally {
                    fResLocks.unlock(path, lockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
   * Generate the namespace declarations.
   */
    private String generateNamespaceDeclarations() {
        return " xmlns:d=\"" + DEFAULT_NAMESPACE + "\"";
    }

    /**
   * Copy a resource.
   * 
   * @param req
   *            Servlet request
   * @param resp
   *            Servlet response
   * @return boolean true if the copy is successful
   */
    private boolean copyResource(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String destinationPath = req.getHeader("Destination");
        if (destinationPath == null) {
            resp.sendError(WebdavStatus.SC_BAD_REQUEST);
            return false;
        }
        destinationPath = RequestUtil.URLDecode(destinationPath, "UTF8");
        int protocolIndex = destinationPath.indexOf("://");
        if (protocolIndex >= 0) {
            int firstSeparator = destinationPath.indexOf("/", protocolIndex + 4);
            if (firstSeparator < 0) {
                destinationPath = "/";
            } else {
                destinationPath = destinationPath.substring(firstSeparator);
            }
        } else {
            String hostName = req.getServerName();
            if ((hostName != null) && (destinationPath.startsWith(hostName))) {
                destinationPath = destinationPath.substring(hostName.length());
            }
            int portIndex = destinationPath.indexOf(":");
            if (portIndex >= 0) {
                destinationPath = destinationPath.substring(portIndex);
            }
            if (destinationPath.startsWith(":")) {
                int firstSeparator = destinationPath.indexOf("/");
                if (firstSeparator < 0) {
                    destinationPath = "/";
                } else {
                    destinationPath = destinationPath.substring(firstSeparator);
                }
            }
        }
        destinationPath = normalize(destinationPath);
        String contextPath = req.getContextPath();
        if ((contextPath != null) && (destinationPath.startsWith(contextPath))) {
            destinationPath = destinationPath.substring(contextPath.length());
        }
        String pathInfo = req.getPathInfo();
        if (pathInfo != null) {
            String servletPath = req.getServletPath();
            if ((servletPath != null) && (destinationPath.startsWith(servletPath))) {
                destinationPath = destinationPath.substring(servletPath.length());
            }
        }
        String path = getRelativePath(req);
        if (!isInRepositoryVisibleToCurrentUser(path)) {
            resp.sendError(resp.SC_FORBIDDEN);
            return false;
        }
        if (path.equals(destinationPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        boolean overwrite = true;
        String overwriteHeader = req.getHeader("Overwrite");
        if (overwriteHeader != null) {
            overwrite = overwriteHeader.equalsIgnoreCase("T");
        }
        String lockOwner = "copyResource" + System.currentTimeMillis() + req.toString();
        if (fResLocks.lock(destinationPath, lockOwner, true, -1)) {
            try {
                if (!fStore.objectExists(path)) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return false;
                }
                boolean exists = fStore.objectExists(destinationPath);
                Hashtable errorList = new Hashtable();
                if (overwrite) {
                    if (exists) {
                        deleteResource(destinationPath, errorList, req, resp);
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }
                } else {
                    if (exists) {
                        resp.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                        return false;
                    } else {
                        resp.setStatus(WebdavStatus.SC_CREATED);
                    }
                }
                if (isInRepositoryEditableByCurrentUser(destinationPath)) {
                    copy(path, destinationPath, errorList, req, resp);
                    if (!errorList.isEmpty()) {
                        sendReport(req, resp, errorList);
                    }
                } else {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                    return false;
                }
            } finally {
                fResLocks.unlock(destinationPath, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
        return true;
    }

    /**
   * copies the specified resource(s) to the specified destination.
   * preconditions must be handled by the caller. Standard status codes must
   * be handled by the caller. a multi status report in case of errors is
   * created here.
   * 
   * @param sourcePath
   *            path from where to read
   * @param destinationPath
   *            path where to write
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @throws IOException
   *             if an error in the underlying store occurs
   * @throws ServletException
   */
    private void copy(String sourcePath, String destinationPath, Hashtable errorList, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (fStore.isResource(sourcePath)) {
            fStore.createResource(destinationPath);
            fStore.setResourceContent(destinationPath, fStore.getResourceContent(sourcePath), null, null);
        } else {
            if (fStore.isFolder(sourcePath)) {
                copyFolder(sourcePath, destinationPath, errorList, req, resp);
            } else {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            }
        }
    }

    /**
   * helper method of copy() recursively copies the FOLDER at source path to
   * destination path
   * 
   * @param sourcePath
   * @param destinationPath
   * @param errorList
   * @param req
   * @param resp
   * @throws IOException
   * @throws ServletException
   */
    private void copyFolder(String sourcePath, String destinationPath, Hashtable errorList, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        fStore.createFolder(destinationPath);
        boolean infiniteDepth = true;
        if (req.getHeader("depth") != null) {
            if (req.getHeader("depth").equals("0")) {
                infiniteDepth = false;
            }
        }
        if (infiniteDepth) {
            String[] children = fStore.getChildrenNames(sourcePath);
            for (int i = children.length - 1; i >= 0; i--) {
                children[i] = "/" + children[i];
                try {
                    if (fStore.isResource(sourcePath + children[i])) {
                        fStore.createResource(destinationPath + children[i]);
                        fStore.setResourceContent(destinationPath + children[i], fStore.getResourceContent(sourcePath + children[i]), null, null);
                    } else {
                        copyFolder(sourcePath + children[i], destinationPath + children[i], errorList, req, resp);
                    }
                } catch (IOException e) {
                    errorList.put(destinationPath + children[i], new Integer(WebdavStatus.SC_INTERNAL_SERVER_ERROR));
                }
            }
        }
    }

    /**
   * deletes the recources at "path"
   * 
   * @param path
   * @param errorList
   * @param req
   * @param resp
   * @throws IOException
   * @throws ServletException
   */
    private void deleteResource(String path, Hashtable errorList, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.setStatus(WebdavStatus.SC_NO_CONTENT);
        if (!readOnly && !isDocumentRepository(path) && isInRepositoryEditableByCurrentUser(path)) {
            if (fStore.isResource(path)) {
                fStore.removeObject(path);
            } else {
                if (fStore.isFolder(path)) {
                    deleteFolder(path, errorList, req, resp);
                    fStore.removeObject(path);
                } else {
                    resp.sendError(WebdavStatus.SC_NOT_FOUND);
                }
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    /**
   * 
   * helper method of deleteResource() deletes the folder and all of its
   * contents
   * 
   * @param path
   *            the folder to be deleted
   * @param req
   *            HttpServletRequest
   * @param resp
   *            HttpServletResponse
   * @return
   * @throws IOException
   * @throws ServletException
   */
    private void deleteFolder(String path, Hashtable errorList, HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String[] children = fStore.getChildrenNames(path);
        for (int i = children.length - 1; i >= 0; i--) {
            children[i] = "/" + children[i];
            try {
                if (fStore.isResource(path + children[i])) {
                    fStore.removeObject(path + children[i]);
                } else {
                    deleteFolder(path + children[i], errorList, req, resp);
                    fStore.removeObject(path + children[i]);
                }
            } catch (IOException e) {
                errorList.put(path + children[i], new Integer(WebdavStatus.SC_INTERNAL_SERVER_ERROR));
            }
        }
    }

    /**
   * Return a context-relative path, beginning with a "/", that represents the
   * canonical version of the specified path after ".." and "." elements are
   * resolved out. If the specified path attempts to go outside the boundaries
   * of the current context (i.e. too many ".." path elements are present),
   * return <code>null</code> instead.
   * 
   * @param path
   *            Path to be normalized
   */
    protected String normalize(String path) {
        if (path == null) return null;
        String normalized = path;
        if (normalized.equals("/.")) return "/";
        if (normalized.indexOf('\\') >= 0) normalized = normalized.replace('\\', '/');
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0) break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 1);
        }
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0) break;
            normalized = normalized.substring(0, index) + normalized.substring(index + 2);
        }
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0) break;
            if (index == 0) return (null);
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
        }
        return (normalized);
    }

    /**
   * Send a multistatus element containing a complete error report to the
   * client.
   * 
   * @param req
   *            Servlet request
   * @param resp
   *            Servlet response
   * @param errorList
   *            List of error to be displayed
   */
    private void sendReport(HttpServletRequest req, HttpServletResponse resp, Hashtable errorList) throws ServletException, IOException {
        resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
        String absoluteUri = req.getRequestURI();
        String relativePath = getRelativePath(req);
        XMLWriter generatedXML = new XMLWriter();
        generatedXML.writeXMLHeader();
        generatedXML.writeElement("d", "multistatus" + generateNamespaceDeclarations(), XMLWriter.OPENING);
        Enumeration pathList = errorList.keys();
        while (pathList.hasMoreElements()) {
            String errorPath = (String) pathList.nextElement();
            int errorCode = ((Integer) errorList.get(errorPath)).intValue();
            generatedXML.writeElement("d", "response", XMLWriter.OPENING);
            generatedXML.writeElement("d", "href", XMLWriter.OPENING);
            String toAppend = errorPath.substring(relativePath.length());
            if (!toAppend.startsWith("/")) toAppend = "/" + toAppend;
            generatedXML.writeText(absoluteUri + toAppend);
            generatedXML.writeElement("d", "href", XMLWriter.CLOSING);
            generatedXML.writeElement("d", "status", XMLWriter.OPENING);
            generatedXML.writeText("HTTP/1.1 " + errorCode + " " + WebdavStatus.getStatusText(errorCode));
            generatedXML.writeElement("d", "status", XMLWriter.CLOSING);
            generatedXML.writeElement("d", "response", XMLWriter.CLOSING);
        }
        generatedXML.writeElement("d", "multistatus", XMLWriter.CLOSING);
        Writer writer = resp.getWriter();
        writer.write(generatedXML.toString());
        writer.close();
    }

    /**
   * Propfind helper method.
   * 
   * @param req
   *            The servlet request
   * @param generatedXML
   *            XML response to the Propfind request
   * @param path
   *            Path of the current resource
   * @param type
   *            Propfind type
   * @param propertiesVector
   *            If the propfind type is find properties by name, then this
   *            Vector contains those properties
   */
    private void parseProperties(HttpServletRequest req, XMLWriter generatedXML, String path, int type, Vector propertiesVector) throws IOException {
        String creationdate = getISOCreationDate(fStore.getCreationDate(path).getTime());
        boolean isFolder = fStore.isFolder(path);
        String lastModified = getISOCreationDate(fStore.getLastModified(path).getTime());
        String resourceLength = String.valueOf(fStore.getResourceLength(path));
        generatedXML.writeElement("d", "response", XMLWriter.OPENING);
        String status = new String("HTTP/1.1 " + WebdavStatus.SC_OK + " " + WebdavStatus.getStatusText(WebdavStatus.SC_OK));
        generatedXML.writeElement("d", "href", XMLWriter.OPENING);
        String href = req.getContextPath() + "/webdav";
        if ((href.endsWith("/")) && (path.startsWith("/"))) href += path.substring(1); else href += path;
        if ((isFolder) && (!href.endsWith("/"))) href += "/";
        generatedXML.writeText(rewriteUrl(href));
        generatedXML.writeElement("d", "href", XMLWriter.CLOSING);
        String resourceName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) resourceName = resourceName.substring(lastSlash + 1);
        switch(type) {
            case FIND_ALL_PROP:
                generatedXML.writeElement("d", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("d", "prop", XMLWriter.OPENING);
                generatedXML.writeProperty("d", "creationdate", creationdate);
                generatedXML.writeElement("d", "displayname", XMLWriter.OPENING);
                generatedXML.writeData(resourceName);
                generatedXML.writeElement("d", "displayname", XMLWriter.CLOSING);
                if (!isFolder) {
                    generatedXML.writeProperty("d", "getcontentlength", resourceLength);
                    String contentType = getServletContext().getMimeType(path);
                    if (contentType != null) {
                        generatedXML.writeProperty("d", "getcontenttype", contentType);
                    }
                    generatedXML.writeProperty("d", "getetag", getETag(path, resourceLength, lastModified));
                    generatedXML.writeElement("d", "resourcetype", XMLWriter.NO_CONTENT);
                } else {
                    generatedXML.writeElement("d", "resourcetype", XMLWriter.OPENING);
                    generatedXML.writeElement("d", "collection", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("d", "resourcetype", XMLWriter.CLOSING);
                }
                generatedXML.writeElement("d", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("d", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "propstat", XMLWriter.CLOSING);
                break;
            case FIND_PROPERTY_NAMES:
                generatedXML.writeElement("d", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("d", "prop", XMLWriter.OPENING);
                generatedXML.writeElement("d", "creationdate", XMLWriter.NO_CONTENT);
                if (!isFolder) {
                    generatedXML.writeElement("d", "getcontentlanguage", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("d", "getcontentlength", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("d", "getcontenttype", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("d", "getetag", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement("d", "getlastmodified", XMLWriter.NO_CONTENT);
                }
                generatedXML.writeElement("d", "resourcetype", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("d", "source", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("d", "lockdiscovery", XMLWriter.NO_CONTENT);
                generatedXML.writeElement("d", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("d", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "propstat", XMLWriter.CLOSING);
                break;
            case FIND_BY_PROPERTY:
                Vector propertiesNotFound = new Vector();
                generatedXML.writeElement("d", "propstat", XMLWriter.OPENING);
                generatedXML.writeElement("d", "prop", XMLWriter.OPENING);
                Enumeration properties = propertiesVector.elements();
                while (properties.hasMoreElements()) {
                    String property = (String) properties.nextElement();
                    if (property.equals("creationdate")) {
                        generatedXML.writeProperty(null, "creationdate", creationdate);
                    } else if (property.equals("displayname")) {
                        generatedXML.writeElement("d", "displayname", XMLWriter.OPENING);
                        generatedXML.writeData(resourceName);
                        generatedXML.writeElement("d", "displayname", XMLWriter.CLOSING);
                    } else if (property.equals("getcontentlanguage")) {
                        if (isFolder) {
                            propertiesNotFound.addElement(property);
                        } else {
                            generatedXML.writeElement("d", "getcontentlanguage", XMLWriter.NO_CONTENT);
                        }
                    } else if (property.equals("getcontentlength")) {
                        if (isFolder) {
                            propertiesNotFound.addElement(property);
                        } else {
                            generatedXML.writeProperty("d", "getcontentlength", resourceLength);
                        }
                    } else if (property.equals("getcontenttype")) {
                        if (isFolder) {
                            propertiesNotFound.addElement(property);
                        } else {
                            generatedXML.writeProperty(null, "getcontenttype", getServletContext().getMimeType(path));
                        }
                    } else if (property.equals("getetag")) {
                        if (isFolder) {
                            propertiesNotFound.addElement(property);
                        } else {
                            generatedXML.writeProperty("d", "getetag", getETag(path, resourceLength, lastModified));
                        }
                    } else if (property.equals("getlastmodified")) {
                        if (isFolder) {
                            propertiesNotFound.addElement(property);
                        } else {
                            generatedXML.writeProperty("d", "getlastmodified", lastModified);
                        }
                    } else if (property.equals("resourcetype")) {
                        if (isFolder) {
                            generatedXML.writeElement("d", "resourcetype", XMLWriter.OPENING);
                            generatedXML.writeElement("d", "collection", XMLWriter.NO_CONTENT);
                            generatedXML.writeElement("d", "resourcetype", XMLWriter.CLOSING);
                        } else {
                            generatedXML.writeElement("d", "resourcetype", XMLWriter.NO_CONTENT);
                        }
                    } else if (property.equals("source")) {
                        generatedXML.writeProperty("d", "source", "");
                    } else {
                        propertiesNotFound.addElement(property);
                    }
                }
                generatedXML.writeElement("d", "prop", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "status", XMLWriter.OPENING);
                generatedXML.writeText(status);
                generatedXML.writeElement("d", "status", XMLWriter.CLOSING);
                generatedXML.writeElement("d", "propstat", XMLWriter.CLOSING);
                Enumeration propertiesNotFoundList = propertiesNotFound.elements();
                if (propertiesNotFoundList.hasMoreElements()) {
                    status = new String("HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " " + WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND));
                    generatedXML.writeElement("d", "propstat", XMLWriter.OPENING);
                    generatedXML.writeElement("d", "prop", XMLWriter.OPENING);
                    while (propertiesNotFoundList.hasMoreElements()) {
                        generatedXML.writeElement("d", (String) propertiesNotFoundList.nextElement(), XMLWriter.NO_CONTENT);
                    }
                    generatedXML.writeElement("d", "prop", XMLWriter.CLOSING);
                    generatedXML.writeElement("d", "status", XMLWriter.OPENING);
                    generatedXML.writeText(status);
                    generatedXML.writeElement("d", "status", XMLWriter.CLOSING);
                    generatedXML.writeElement("d", "propstat", XMLWriter.CLOSING);
                }
                break;
        }
        generatedXML.writeElement("d", "response", XMLWriter.CLOSING);
    }

    /**
   * Get the ETag associated with a file.
   * 
   */
    protected String getETag(String path, String resourceLength, String lastModified) throws IOException {
        return "W/\"" + resourceLength + "-" + lastModified + "\"";
    }

    /**
   * URL rewriter.
   * 
   * @param path
   *            Path which has to be rewiten
   */
    protected String rewriteUrl(String path) {
        return urlEncoder.encode(path);
    }

    /**
   * Get creation date in ISO format.
   */
    private String getISOCreationDate(long creationDate) {
        StringBuffer creationDateValue = new StringBuffer(creationDateFormat.format(new Date(creationDate)));
        return creationDateValue.toString();
    }

    /**
   * Determines the methods normally allowed for the resource.
   * 
   */
    private String determineMethodsAllowed(String uri, boolean exists, boolean isFolder) {
        StringBuffer methodsAllowed = new StringBuffer();
        try {
            if (exists) {
                methodsAllowed.append("OPTIONS, GET, HEAD, POST, DELETE, TRACE");
                methodsAllowed.append(", PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND");
                if (isFolder) {
                    methodsAllowed.append(", PUT");
                }
                return methodsAllowed.toString();
            }
        } catch (Exception e) {
        }
        methodsAllowed.append("OPTIONS, MKCOL, PUT, LOCK");
        return methodsAllowed.toString();
    }

    /**
   * Checks whether the directory is an iPoint Document Repository
   * @param path the path of the directory relative to the document root directory
   * @return boolean true if the directory is an iPoint document repository
   */
    private boolean isDocumentRepository(String path) {
        Pattern p = Pattern.compile("/");
        Matcher m = p.matcher(path);
        int slashCount = 0;
        while (m.find()) {
            slashCount++;
        }
        if (slashCount == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
   * Checks whether the current user has edit rights for the Document Repository which the file is stored in
   * @param filePath path of the file relative to the root
   * @return true if the current user has edit rights
   * @throws ServletException
   */
    private boolean isInRepositoryEditableByCurrentUser(String filePath) throws ServletException {
        boolean editableRepository = false;
        if (filePath.startsWith("/")) {
            if (filePath.length() == 1) {
                return editableRepository;
            } else {
                int secondSlash = filePath.indexOf("/", 1);
                String rootPath = Property.getPropertyValue("MediaRepository");
                String repositoryPath;
                if (secondSlash == -1) {
                    repositoryPath = rootPath + File.separator + filePath.substring(1);
                } else {
                    repositoryPath = rootPath + File.separator + filePath.substring(1, secondSlash);
                }
                try {
                    DocumentRepository documentRepository = DocumentRepository.findByDirectoryPath(repositoryPath);
                    editableRepository = documentRepository.isEditableBy(PortalRequest.getCurrentRequest().getCurrentUser());
                    return editableRepository;
                } catch (PersistentModelException pme) {
                    pme.printStackTrace();
                    throw new ServletException("Unable to retrieve document repository from persistent store");
                }
            }
        } else {
            throw new ServletException("Relative file path does not start with a forward slash");
        }
    }

    /**
   * Checks whether the current user has edit rights for the Document Repository which the file is stored in
   * @param filePath path of the file relative to the root
   * @return true if the current user has edit rights
   * @throws ServletException
   */
    private boolean isInRepositoryVisibleToCurrentUser(String filePath) throws ServletException {
        boolean result = false;
        if (filePath.startsWith("/")) {
            if (filePath.length() == 1) {
                result = true;
            } else {
                int secondSlash = filePath.indexOf("/", 1);
                String rootPath = Property.getPropertyValue("MediaRepository");
                String repositoryPath;
                if (secondSlash == -1) {
                    repositoryPath = rootPath + File.separator + filePath.substring(1);
                } else {
                    repositoryPath = rootPath + File.separator + filePath.substring(1, secondSlash);
                }
                try {
                    DocumentRepository documentRepository = DocumentRepository.findByDirectoryPath(repositoryPath);
                    result = documentRepository.isVisibleTo(PortalRequest.getCurrentRequest().getCurrentUser());
                } catch (PersistentModelException pme) {
                    pme.printStackTrace();
                    throw new ServletException("Unable to retrieve document repository from persistent store");
                }
            }
        } else {
            throw new ServletException("Relative file path does not start with a forward slash");
        }
        return result;
    }
}
