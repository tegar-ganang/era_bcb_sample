package org.docsfree.core;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import com.extentech.ExtenXLS.*;
import com.extentech.toolkit.*;
import com.extentech.comm.*;
import org.docsfree.core.management.Metrics;
import org.docsfree.core.management.MetricsMBean;
import org.docsfree.core.plugin.*;
import org.docsfree.legacy.auth.AclEntry;
import org.docsfree.legacy.auth.User;
import org.docsfree.legacy.auth.UserFactory;
import org.docsfree.legacy.jsp.FileUploadBean;
import org.docsfree.plugin.*;
import org.json.JSONObject;
import com.extentech.ExtenXLS.web.MemeDocument;
import com.extentech.ExtenXLS.web.WebDoc;
import com.extentech.ExtenXLS.web.WebWorkBook;
import com.extentech.luminet.ServeConnection;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.*;
import javax.servlet.http.*;

/** Expose the Document as RESTful Web Services
 * ------------------------------------------------------------
 * 
 * At this time, the servlet does not strictly follow the REST architecture,
 * because it allows GET commands to modify resource state.
 * 
 * The primary argument against this usage is that web crawlers or other unintended
 * system calls might result in unintended consequences.
 * 
 * However, our security mechanism is responsible for authenticating and controlling
 * resource modification. It is our contention that resource modification is
 * best controlled through a security implementation, not determined by a protocol
 * method.
 * 
 * It does expose the WorkBook methods as RESTful URLs.
 * 
 * URLs are in the format:
 * http://host.domain.tld/servletalias/{load method}/{resource id}/{return format}/{resource type}/{command}/{resource name}/{resource value(s)}
 *  
 * <td onClick=a(this)" FORMULASTRING='SUM(a1+a3)'>whatever</td>
 * 
 * <menuitem onClick =command(delete)>
 *  
 *  getTarget()
 *  
 *  "/xml/sheet/delete/Sheet1";
 *  
 *  function command(commandstring){
 *  	thing.getTarget()
 *  	ajaxcomm.doit(commandstring);
 *  }
 *  
 *  deleteSheet('sheet1');
 *  
 *  // these methods ONLY update UI
 *  function resizeColumn(xml){
 *  	// call the ajax
 *  	if(!flag) 
 *  	else
 *  // 	deal with xml
 *  	
 *  }
 *  
 *  ajaxcomm()
 *  	request( "/xml/sheet/delete/Sheet1", deleteSheet);
 *  }
 *  
 *  
 * example 1: 
 * insert a new sheet named "New Sheet" at index 0
 *  http://127.0.0.1:8080/doc/id/1113/xml/sheet/add/New Sheet/0
 * 
 * example 2:
 * insert a new cell at "New Sheet!A1" with the initial value of "Hello World!"
 *  http://127.0.0.1:8080/doc/id/1113/xml/cell/get/New Sheet!A1/
 * 	
 * example 3:
 * insert a new cell at "New Sheet!A2" with the initial value of "=SUM(10+11)"
 *  http://127.0.0.1:8080/doc/id/1113/xls/cell/add/New Sheet!A2/=SUM(10+11)
 * 
 * example 4:
 * runs the cellbinder to merge data with loaded doc
 *  http://127.0.0.1:8080/doc/id/1310/json/cellbinder/bind/1314?year=2008
 * 
 * example 5:
 * gets a whole row of JavaScript Cell Objects from row 1:
 *	http://127.0.0.1:8080/doc/id/1113/json/cellrange/get/Sheet!A1:VU1
 *
 *Reload plugins
 *http://127.0.0.1:8080/doc/plugins/null/xml/plugin/reload/
 *
 *Unload plugin
 *http://127.0.0.1:8080/doc/plugins/null/xml/plugin/unload/pluginToUnload
 *
 *  
 *  More info on JSON:
 *		http://www.json.org/
 * 
 *  /doc/post/null/xml/import/
	/doc/id/null/xls/import/
 * 
 * For more info see: http://en.wikipedia.org/wiki/Representational_State_Transfer
 */
public abstract class DocumentServletBase extends HttpServlet {

    private static final long serialVersionUID = -921915614348207470L;

    private static String PLUGINDIR = "WEB-INF/plugins/";

    WorkBookCommander wbean;

    DocumentLoader loader;

    private Metrics metrics;

    String redir = "/grid/grid.jsp";

    protected final String servicetype;

    PluginLoader pLoader;

    /** Creates {@link User} objects pre-configure for the E360 DB.
     * @deprecated this should be handled by the E360 auth.
     */
    private UserFactory userFactory;

    public DocumentServletBase(String type) {
        servicetype = type;
    }

    /** do servlet init
     * ------------------------------------------------------------
     * 
     * @author John [ Sep 7, 2007 ]
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() {
        DocumentHandle bkx = new WorkBookHandle();
        Logger.logInfo("DocumentServletBase initialize doc test SUCCESS:" + bkx.toString());
        ServletContext context = getServletContext();
        wbean = new WorkBookCommander();
        pLoader = new PluginLoader(PLUGINDIR);
        wbean.setPluginLoader(pLoader);
        loader = new DocumentLoader(getServletContext(), servicetype);
        String s = getInitParameter("org.docsfree.legacy.doc_cache_timeout");
        if (s != null) {
            loader.setWorkbookTimeoutMs(new Integer(s).intValue());
        }
        s = getInitParameter("org.docsfree.legacy.store_doc_in_db");
        if (s != null) loader.setStoreBookAsFile(!(new Boolean(s).booleanValue()));
        s = getInitParameter("org.docsfree.legacy.store_doc_as_xml");
        if (s != null) loader.setStoreAsXML(new Boolean(s).booleanValue());
        try {
            metrics = (Metrics) context.getAttribute("org.docsfree.core.management.Metrics");
        } catch (ClassCastException e) {
        }
        if (metrics == null) {
            metrics = new MetricsMBean();
            context.setAttribute("org.docsfree.core.management.Metrics", metrics);
        }
        try {
            userFactory = (UserFactory) context.getAttribute("org.docsfree.legacy.UserFactory");
        } catch (ClassCastException e) {
        }
        if (userFactory == null) {
            userFactory = new UserFactory();
            userFactory.setConn(DatabaseManager.getConnectionNoX(getServletContext()));
            context.setAttribute("org.docsfree.legacy.UserFactory", userFactory);
        }
    }

    /**
     * Handles posts to the WBS.  In cases where we have an empty value param and a post
     * the value is in the post and should be appended to the map of params
     * ------------------------------------------------------------
     * 
     * @author Nicholas [ May 2, 2007 ]
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * Handles the majority of the REST calls when data not embedded in POST stream
     * ------------------------------------------------------------
     * 
     * @author Administrator [ May 2, 2007 ]
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        runService(request, response);
    }

    /**
     * this is the main service method
     * ------------------------------------------------------------
     * 
     * @author john [ Nov 6, 2009 ]
     * @param request
     * @param response
     * @param stype is the service type doc, workbook, or presentation
     * @throws ServletException
     * @throws IOException
     */
    public void runService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        metrics.incrementCounter("docs_num_requests");
        metrics.setCounter("docs in active memory", loader.getWorkbooksInMemory());
        metrics.setCounter("docs in disk cache", loader.getWorkbooksInDiskCache());
        Object returnObj = null;
        String requestURI = request.getRequestURI();
        User usr = (User) request.getUserPrincipal();
        String rqx = ((ServeConnection) request).getRawRequest();
        Map<String, Object> parameters = DocumentServletBase.parseRequest(requestURI, rqx);
        parameters.put(WorkBookCommander.LOADER, loader);
        parameters.put(WorkBookCommander.REQUEST, request);
        parameters.put(WorkBookCommander.SERVERCONFIG, getServletContext());
        String outputType = (String) parameters.get(WorkBookCommander.FORMAT);
        HttpSession session = request.getSession();
        Connection dbcon = DatabaseManager.getConnectionNoX(getServletContext());
        try {
            if (usr == null && rqx.indexOf("login") == -1) {
                usr = getUser(parameters, request, response);
            }
        } catch (AuthException e) {
            response.sendError(response.SC_UNAUTHORIZED);
        }
        String stx = getServletContext().getInitParameter("storage_auth_ticket_name");
        if (request.getParameter(stx) != null) {
            usr.setVal(stx, request.getParameter(stx));
        }
        boolean ispublic = false;
        boolean nullid = false;
        parameters.put(WorkBookCommander.USER, usr);
        String loadBy = (String) parameters.get(WorkBookCommander.LOADMETHOD);
        if (loadBy.equals("plugins")) {
            if (parameters.get(WorkBookCommander.COMMAND).equals("getinfo")) ispublic = true;
        }
        MessageListener messenger = getMessageClient(request, usr);
        parameters.put(WorkBookCommander.MESSAGINGCLIENT, messenger);
        if (parameters.get(WorkBookCommander.COMMAND).toString().equals("preload")) {
            preloadBook(parameters, request, response);
            return;
        }
        String meme_id = "-1";
        try {
            Object ox = parameters.get(WorkBookCommander.ID);
            if (ox != null) {
                nullid = ox.toString().equals("null");
                meme_id = ox.toString();
            }
            ispublic = loader.isPublic(parameters.get(WorkBookCommander.ID).toString());
        } catch (Exception e) {
            try {
                if (parameters.get(WorkBookCommander.GUID) != null) {
                    ispublic = true;
                }
            } catch (Exception ex) {
                ;
            }
        }
        if (usr == null) {
            if ((ispublic) || (parameters.get(WorkBookCommander.COMMAND).equals("getform")) || (parameters.get(WorkBookCommander.COMMAND).equals("postform"))) {
                usr = loader.getStorage().getAuth().getAnonUser(getServletContext());
            } else {
                if (nullid) {
                    Logger.logInfo("New User Signup");
                    metrics.incrementCounter("wbs_new_signups");
                } else {
                    metrics.incrementCounter("wbs_auth_failures");
                    try {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "DocumentServletBase Request Authentication Failure");
                    } catch (Exception e) {
                        throw new ServletException("DocumentServletBase Request Authentication Failure:" + parameters.toString());
                    }
                    return;
                }
            }
        }
        parameters.put(WorkBookCommander.LOADER, loader);
        parameters.put(WorkBookCommander.SERVLET, this);
        parameters.put(WorkBookCommander.REQUEST, request);
        parameters.put(WorkBookCommander.USER, usr);
        parameters.put(WorkBookCommander.SERVERCONFIG, getServletContext());
        try {
            MemeDocument doc = null;
            if ((loadBy.equals("id") || loadBy.equals("guid")) && !nullid) {
                String gui_id = null;
                if (parameters.get(WorkBookCommander.GUID) != null) {
                    gui_id = parameters.get(WorkBookCommander.GUID).toString();
                    meme_id = gui_id;
                }
                if (loadBy.equals("guid")) {
                    if (requestURI.indexOf("getmessages") == -1) {
                        if (session.getAttribute("org.docsfree.legacy.ticket") != null) {
                            usr.setVal("ticket", session.getAttribute("org.docsfree.legacy.ticket"));
                        }
                        doc = loader.getRemoteWorkBook(new Integer(gui_id), (User) usr, messenger, true, parameters);
                        extractImages(doc, meme_id, ((User) usr).getId(), false);
                    } else {
                        doc = loader.getRemoteWorkBook(new Integer(gui_id), (User) usr, messenger, false, parameters);
                        if (doc == null) return;
                    }
                } else {
                    meme_id = parameters.get(WorkBookCommander.ID).toString();
                    if (meme_id.equals("-1")) {
                        doc = loader.getNewWorkBook((User) usr, messenger);
                        meme_id = String.valueOf(doc.getMemeId());
                    } else if (request.getParameter("reload") != null) {
                        doc = loader.resetDocument(new Integer(meme_id), (User) usr, messenger);
                    } else {
                        loader.setServiceType(servicetype);
                        if (requestURI.indexOf("getmessages") == -1) {
                            doc = (MemeDocument) loader.getDocument(new Integer(meme_id), (User) usr, messenger, true);
                        } else {
                            doc = (MemeDocument) loader.getDocument(new Integer(meme_id), (User) usr, messenger, false);
                            if (doc == null) return;
                        }
                    }
                }
                if (!ispublic) {
                    if ((doc.getOwnerId() != ((User) usr).getId()) && (!((User) usr).checkAccess("meme_" + meme_id, AclEntry.READ)) && (!((User) usr).checkAccess("meme_" + meme_id, AclEntry.APPEND)) && (!((User) usr).checkAccess("meme_" + meme_id, AclEntry.OWNER)) && (!((User) usr).checkAccess("meme_" + meme_id, AclEntry.UPDATE))) {
                        returnObj = WorkBookCommander.returnXMLResponse("DocumentServletBase: Access Denied");
                        sendTextResponse(request, response, (String) returnObj, outputType);
                        return;
                    }
                }
                if (doc == null) return;
                if ((requestURI.indexOf("getmessages") == -1) && (requestURI.indexOf("refresh") == -1)) {
                    String rl = "" + getLogDate() + "||" + usr.toString() + "||" + usr.getId() + "||" + requestURI.substring(requestURI.indexOf("doc") + 8);
                    doc.getRESTlog().add(rl);
                }
            } else if (loadBy.equals("plugins") || parameters.get(WorkBookCommander.PLUGINNAME).equals("system")) {
                try {
                    handleManagePlugins(usr, parameters, returnObj, request, response);
                } catch (Exception e) {
                    metrics.incrementCounter("doc_plugin_manager_errors");
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString());
                    Logger.logErr("DocumentServletBase.handleManagePlugins failed :" + e.toString());
                    return;
                }
            } else if (loadBy.equals("rdf")) {
            }
            if (loadBy.equals("postdata") || loadBy.equals("url")) {
                int midx = -1;
                try {
                    if (loadBy.equals("postdata")) doc = getDocumentFromPost(dbcon, usr, request, response); else if (loadBy.equals("url")) doc = getDocumentFromURL(dbcon, usr, request, response);
                    doc.setSharingAccess(AclEntry.PRIVATE);
                    if (loader.getConnection() == null) {
                        dbcon = DatabaseManager.getConnectionNoX(getServletContext());
                        loader.setConnection(dbcon);
                    }
                    if (getInitParameter("org.docsfree.legacy.store_doc_in_db") != null) {
                        doc.setStoreBookAsFile(getInitParameter("org.docsfree.legacy.store_doc_in_db").equals("false"));
                    }
                    midx = loader.storeNewWorkBook(doc, (User) usr, messenger);
                    meme_id = String.valueOf(midx);
                    parameters.put(WorkBookCommander.WORKBOOK, doc);
                    returnObj = String.valueOf(meme_id);
                    try {
                        extractImages(doc, meme_id, ((User) usr).getId(), true);
                    } catch (Exception e) {
                    }
                    ((User) usr).initOwnedACL();
                } catch (Exception ex) {
                    Logger.logErr("Reading Uploaded Document Failed.", ex);
                    sendTextResponse(request, response, WorkBookCommander.returnXMLErrorResponse("Reading Uploaded Document Failed: " + ex.toString()), "text");
                    return;
                }
                if (doc == null) {
                    Logger.logErr("loading doc after Document import failed");
                    sendTextResponse(request, response, WorkBookCommander.returnXMLErrorResponse("Uploading Document Failed. Doc is null after upload."), "text");
                    return;
                }
                sendEditDocumentRedirect(doc, midx, response);
                return;
            }
            parameters.put(WorkBookCommander.WORKBOOK, doc);
            if (returnObj == null) {
                returnObj = runPlugin(parameters);
            }
            if (returnObj != null) {
                if (returnObj instanceof JSONMessage) {
                    String ptype = (String) parameters.get(WorkBookCommander.PLUGINNAME);
                    if (ptype != "messaging") {
                        JSONMessage message = (JSONMessage) returnObj;
                        try {
                            if (message.getType().equals("error")) {
                                sendErrorResponse(request, response, message.getMessageBody().toString(), outputType);
                                return;
                            } else if (message.getType().equals("chat")) {
                                messenger.sendMessageToOthers(new Integer(meme_id), message);
                            } else {
                                messenger.sendMessageToOthers(new Integer(meme_id), message);
                            }
                        } catch (Exception e) {
                            metrics.incrementCounter("wbs_messaging_errors");
                            Logger.logErr("Sending JSON Message to others failed.", e);
                        }
                        returnObj = message.getMessageBody().toString();
                    }
                } else if (outputType.equalsIgnoreCase("PNG")) {
                    sendPNGResponse(request, response, (File) returnObj);
                    return;
                }
                sendTextResponse(request, response, (String) returnObj, outputType);
            } else {
                sendMimeResponse(doc, request, response, (String) returnObj, outputType, returnObj);
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DocumentServletBase Request Failed for: " + parameters + " Return Object: " + returnObj);
                } catch (Exception e) {
                    metrics.incrementCounter("wbs_service_errors");
                    Logger.logErr("DocumentServletBase.service failed :" + e.toString());
                }
            }
        } catch (Throwable ex) {
            metrics.incrementCounter("wbs_service_errors");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
            Logger.logErr("DocumentServletBase.service failed :" + ex.toString());
        }
    }

    public static final String getLogDate() {
        Timestamp t = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat();
        df.applyPattern("yyyy-MM-dd hh:mm:ss z");
        return df.format(t);
    }

    /**
     * a new document has been created, redirect to editing location
     * 
     * @param doc
     * @param meme_id
     * @param response
     */
    public static void sendEditDocumentRedirect(MemeDocument doc, int meme_id, HttpServletResponse response) throws Exception {
        response.sendRedirect("/grid/grid.jsp?maximize=true&meme_id=" + meme_id + "&page_name=Page1");
    }

    void handleManagePlugins(User usr, Map parameters, Object returnObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (parameters.get(WorkBookCommander.COMMAND).equals("login")) {
            returnObj = runPlugin(parameters).toString();
            sendTextResponse(request, response, returnObj.toString(), "xml");
            return;
        }
        if (parameters.get(WorkBookCommander.COMMAND).equals("getinfo")) {
            returnObj = WorkBookCommander.returnXMLResponse("" + pLoader.describePlugins());
            sendTextResponse(request, response, returnObj.toString(), "xml");
            return;
        }
        if (usr.checkAccess("plugin_control", AclEntry.UPDATE)) {
            if (parameters.get(WorkBookCommander.COMMAND).equals("reload")) {
                pLoader.initializePlugins();
                returnObj = WorkBookCommander.returnXMLResponse("true");
            } else if (parameters.get(WorkBookCommander.COMMAND).equals("unload")) {
                returnObj = WorkBookCommander.returnXMLResponse("" + pLoader.unloadPlugin((String) parameters.get(WorkBookCommander.RESOURCE)));
            }
        } else {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "DocumentServletBase Request Authentication Failure");
            } catch (Exception e) {
                throw new ServletException("DocumentServletBase Request Authentication Failure:" + parameters.toString());
            }
            return;
        }
    }

    /**
     * send the document out to a stream using various output mime types
	 * 
	 * override in subclasses to provide for custom mime types such as XLSX and DOC
	 * 
     * @param doc
     * @param request
     * @param response
     * @param returnText
     * @param outputType
     * @param returnObj
     * @throws Exception
     */
    void sendMimeResponse(DocumentHandle doc, HttpServletRequest request, HttpServletResponse response, String returnText, String outputType, Object returnObj) throws Exception {
        if (outputType.equalsIgnoreCase("PDF")) {
            sendPDFResponse(request, response, doc);
            return;
        } else if (outputType.equalsIgnoreCase("PNG")) {
            sendPNGResponse(request, response, (File) returnObj);
            return;
        } else if (outputType.equalsIgnoreCase("ZIP")) {
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
    }

    /**
     * Preload the doc, this is currently handled differently 
     * than the plugins as it needs to occur before
     * rights permissions can occur
     * 
     * maybe?  not sure, may be able to move this out.
     * 
     * @param parameters
     * @param request
     * @param response
     */
    private void preloadBook(Map<String, Object> parameters, HttpServletRequest request, HttpServletResponse response) {
        String theId = (String) parameters.get(WorkBookCommander.GUID);
        User usr = (User) request.getUserPrincipal();
        MessageListener messenger = getMessageClient(request, usr);
        Object returnObj;
        String outputType = (String) parameters.get(WorkBookCommander.FORMAT);
        if (theId == null) theId = parameters.get(WorkBookCommander.ID).toString();
        MemeDocument doc;
        try {
            doc = loader.getRemoteWorkBook(new Integer(theId), (User) usr, messenger, true, parameters);
        } catch (StorageException e1) {
            returnObj = WorkBookCommander.returnXMLResponse("authorization failure");
            try {
                sendTextResponse(request, response, (String) returnObj, outputType);
            } catch (Exception e) {
            }
            return;
        }
        theId = doc.getMemeId() + "";
        extractImages(doc, theId, ((User) usr).getId(), false);
        try {
            JSONObject retob = new JSONObject();
            retob.put("meme_id", theId);
            sendTextResponse(request, response, retob.toString(), outputType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * Attempts to run a plugin, returns null if the plugin does not exist
     * 
     * checks user security based on plugin being run
     * ------------------------------------------------------------
     * 
     * @author Nicholas [ May 15, 2007 ]
     * @return
     */
    public Object runPlugin(Map parameters) {
        String resourceType = (String) parameters.get(WorkBookCommander.PLUGINNAME);
        User usr = (User) parameters.get(WorkBookCommander.USER);
        Plugin p = pLoader.getPlugin(resourceType);
        if (p != null) {
            p.setStorageAuth(loader.getStorage().getAuth());
            Object o = p.executeCommand(parameters, p);
            return o;
        }
        return null;
    }

    /** Return error response
     *  ------------------------------------------------------------
     * 
     * @author John McMahon [ Feb 1, 2007 ]
     * @param request
     * @param response
     */
    public static void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, String returnText, String outputType) throws Exception {
        OutputStream out = response.getOutputStream();
        byte[] bbuf = returnText.getBytes("UTF-8");
        if (outputType.equals("xml")) {
            response.setContentType("text/xml");
        } else if (outputType.equals("xhtml")) {
            response.setContentType("text/xml");
        } else if (outputType.equals("txt") || outputType.equals("csv")) {
            response.setContentType("text/plain");
        } else if (outputType.equals("html")) {
            response.setContentType("text/html");
        }
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (!outputType.equals("json")) {
            response.setContentLength(bbuf.length);
            response.setHeader("expires", "Sun, 30 March 1969 7:00:00 GMT");
            BufferedOutputStream bout = new BufferedOutputStream(out);
            bout.write(bbuf);
            bout.flush();
            bout.close();
        } else {
            response.addHeader("Content-Encoding", "gzip");
            BufferedOutputStream bout = new BufferedOutputStream(new GZIPOutputStream(out));
            for (int t = 0; t < bbuf.length; t++) {
                bout.write(bbuf[t]);
            }
            bout.flush();
            bout.close();
        }
    }

    /** Return text response
     *  ------------------------------------------------------------
     * 
     * @author John McMahon [ Feb 1, 2007 ]
     * @param request
     * @param response
     */
    public static void sendTextResponse(HttpServletRequest request, HttpServletResponse response, String returnText, String outputType) throws Exception {
        OutputStream out = response.getOutputStream();
        byte[] bbuf = returnText.getBytes("UTF-8");
        if (outputType.equals("xml") || outputType.equals("xhtml")) {
            response.setContentType("text/xml");
        } else if (outputType.equals("txt") || outputType.equals("csv")) {
            response.setContentType("text/plain");
        } else if (outputType.equals("html")) {
            response.setContentType("text/html");
        }
        if (!outputType.equals("json")) {
            response.setContentLength(bbuf.length);
            response.setHeader("expires", "Sun, 30 March 1969 7:00:00 GMT");
            BufferedOutputStream bout = new BufferedOutputStream(out);
            bout.write(bbuf);
            bout.flush();
            bout.close();
        } else {
            response.addHeader("Content-Encoding", "gzip");
            BufferedOutputStream bout = new BufferedOutputStream(new GZIPOutputStream(out));
            for (int t = 0; t < bbuf.length; t++) {
                bout.write(bbuf[t]);
            }
            bout.flush();
            bout.close();
        }
    }

    /** Return PNG
     *  ------------------------------------------------------------
     * 
     * @author NicholasRab
     * @param request
     * @param response
     */
    public static void sendPNGResponse(HttpServletRequest request, HttpServletResponse response, File file) throws Exception {
        OutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        response.setHeader("expires", "Sun, 30 March 1969 7:00:00 GMT");
        FileInputStream fin = new FileInputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(out);
        for (int t = 0; t < file.length(); t++) {
            bout.write(fin.read());
        }
        bout.flush();
        bout.close();
        ;
    }

    /** Return PDF
     *  ------------------------------------------------------------
     * 
     * @author John McMahon [ November 5, 2009 ]
     * @param request
     * @param response
     */
    public static void sendPDFResponse(HttpServletRequest request, HttpServletResponse response, DocumentHandle doc) throws Exception {
        OutputStream out = response.getOutputStream();
        response.setContentType("application/pdf");
        response.setHeader("expires", "Sun, 30 March 1969 7:00:00 GMT");
        String width = request.getParameter("print_width");
        String height = request.getParameter("print_height");
        int w = -1, h = -1;
        try {
            if (width != null) w = Integer.parseInt(width);
            if (height != null) h = Integer.parseInt(height);
        } catch (Exception e) {
            ;
        }
        File fx = null;
        if ((w > -1) && (h > -1)) if (doc instanceof WebWorkBook) fx = ExtenXLS.getPDF((WebWorkBook) doc, h, w); else if (doc instanceof WebWorkBook) fx = ExtenXLS.getPDF((WebWorkBook) doc);
        FileInputStream fin = new FileInputStream(fx);
        BufferedOutputStream bout = new BufferedOutputStream(out);
        for (int t = 0; t < fx.length(); t++) {
            bout.write(fin.read());
        }
        bout.flush();
        bout.close();
    }

    /**
     * Gets the attached file from a post and returns as a byte array
     * ------------------------------------------------------------
     * 
     * @author Administrator [ May 2, 2007 ]
     * @param request
     */
    public MemeDocument getDocumentFromPost(Connection conn, User user, HttpServletRequest request, HttpServletResponse response, File tempfile, String filename) throws Exception {
        Logger.logInfo("DocServlet.getDocumentFromPost uploaded:" + filename);
        WebDoc doc = null;
        FileReader fincheck = new FileReader(tempfile);
        if (tempfile.length() > 100) {
            char[] cbuf = new char[100];
            fincheck.read(cbuf);
            String finch = new String(cbuf);
            if (finch.indexOf("<Document Name=") > -1) {
                doc = new WebDoc();
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from EXML file:" + tempfile);
                ExtenDOC.getDOC(doc, new FileInputStream(tempfile));
            } else if (finch.indexOf("<Document Name=") > -1) {
                doc = new WebDoc();
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from EXML file:" + tempfile);
                ExtenDOC.getDOC(doc, new FileInputStream(tempfile));
            } else if (finch.indexOf("<?xml version=") == 0) {
                tempfile.delete();
                sendTextResponse(request, response, "Upload Error: Unsupported XML Input Type. Please upload ExtenXLS XML files.", "txt");
            } else {
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from binary file:" + tempfile);
                doc = new WebDoc(conn, tempfile);
            }
        } else {
            tempfile.delete();
            sendTextResponse(request, response, "Upload Error: File size smaller than minimum upload.", "txt");
        }
        doc.setOwnerId(user.getId());
        filename = com.extentech.toolkit.StringTool.convertJavaStyletoFriendlyConvention(filename);
        filename = com.extentech.toolkit.StringTool.proper(filename);
        doc.setName(filename);
        String media_dir = "media/" + user.getId();
        doc.setOutputDir(media_dir);
        doc.getProperties().put("media_dir", media_dir);
        return doc;
    }

    /**
     * create open from URL capability for example to load a Google Doc from a url like:
    
 		https://spreadsheets.google.com/fm?id=tmTKEMf4-QT1VAbH341Y4iQ.02513249933416883934.52369393895076100&hl=en&fmcmd=4
     
     * @param request
     */
    public MemeDocument getDocumentFromURL(Connection conn, User user, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String filename = request.getParameter("url");
        WebWorkBook doc = null;
        doc = new WebWorkBook(conn, new URL(filename));
        Logger.logInfo("DocServlet.getDocumentFromURL uploaded:" + filename);
        return doc;
    }

    /**
     * Gets the attached file from a post and returns as a byte array
     * ------------------------------------------------------------
     * 
     * @author Administrator [ May 2, 2007 ]
     * @param request
     */
    public MemeDocument getDocumentFromPost(Connection conn, User user, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String filename = "", tid = null;
        FileUploadBean parser = new FileUploadBean();
        File tempdir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
        File tempfile = File.createTempFile("upload", ".tmp", tempdir);
        tempfile.deleteOnExit();
        parser.doUploadToFile(tempfile, request);
        filename = parser.getFileName();
        tid = parser.getField("target_id");
        if (filename.toLowerCase().endsWith(".doc")) return getDocumentFromPost(conn, user, request, response, tempfile, filename);
        Logger.logInfo("DocumentServletBase.getWorkBookFromPost uploaded:" + filename);
        MemeDocument doc = null;
        FileReader fincheck = new FileReader(tempfile);
        if (tempfile.length() > 100) {
            char[] cbuf = new char[100];
            fincheck.read(cbuf);
            String finch = new String(cbuf);
            if (finch.indexOf("<WorkBook Name=") > -1) {
                doc = new WebWorkBook();
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from EXML file:" + tempfile);
                ExtenDOC.getDOC(doc, new FileInputStream(tempfile));
            } else if (finch.indexOf("<Document Name=") > -1) {
                doc = new WebWorkBook();
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from EXML file:" + tempfile);
                ExtenDOC.getDOC(doc, new FileInputStream(tempfile));
            } else if (finch.indexOf("<?xml version=") == 0) {
                tempfile.delete();
                sendTextResponse(request, response, "Upload Error: Unsupported XML Input Type. Please upload ExtenXLS XML files.", "txt");
            } else {
                Logger.logInfo("DocumentServletBase.getWorkBookFromPost initializing file from binary file:" + tempfile);
                doc = new WebWorkBook(conn, tempfile);
            }
        } else {
            tempfile.delete();
            sendTextResponse(request, response, "Upload Error: File size smaller than minimum upload.", "txt");
        }
        if (tid != null) {
            WebWorkBook target_book = null;
            Integer target_midi = new Integer(-1);
            MessageListener ml = null;
            DocumentLoader wload = this.loader;
            try {
                target_midi = new Integer(tid);
                target_book = (WebWorkBook) wload.getDocument((Integer) target_midi, user, ml, true);
            } catch (Exception e) {
                try {
                    target_book = (WebWorkBook) wload.getRemoteWorkBook(target_midi, user, ml, true, null);
                } catch (StorageException e1) {
                    Logger.logErr("DocumentServletBase.getWorkBookFromPost error getting target book:" + doc.toString(), e1);
                }
            }
            if (target_book != null) {
                WorkSheetHandle[] newsheets = ((WebWorkBook) doc).getWorkSheets();
                for (int t = 0; t < newsheets.length; t++) {
                    String sn = newsheets[t].getSheetName();
                    target_book.addSheetFromWorkBookWithFormatting(((WebWorkBook) doc).getBook(), sn, sn);
                }
            }
        }
        doc.setOwnerId(user.getId());
        filename = com.extentech.toolkit.StringTool.convertJavaStyletoFriendlyConvention(filename);
        filename = com.extentech.toolkit.StringTool.proper(filename);
        doc.setName(filename);
        String media_dir = "media/" + user.getId();
        doc.setOutputDir(media_dir);
        doc.getProperties().put("media_dir", media_dir);
        Logger.logInfo("DocumentServletBase.getWorkBookFromPost successfully uploaded:" + doc.toString());
        return doc;
    }

    /** extract the images from the doc and link as medialink
     * 
     * 
     * @author John [ Oct 18, 2007 ]
     * @param bkx
         @param storeRefInDB, normally willl be true, but for guid loading (ie external storage of doc, ignore the database insert)
         @param midi meme id.  If storing as a reference this needs to be parseable as an int
     */
    public void extractImages(Document bkx, String midi, int uid, boolean storeRefInDb) {
        log("Extracting images from: " + bkx);
    }

    /**Convert the URL into RESTful Commands
     *  ------------------------------------------------------------
     * 
     * REST format is http://{serveraddress}/{doc}/{loadby}/{id_value}/{return_format}/{plugin_name}/{plugin_method}/{resource}/
     * 
     * eg: 
     * insert a new sheet named "New Sheet" at index 0
     *  http://127.0.0.1:8080/doc/id/1113/xml/sheet/add/New Sheet/0
     * 
     * eg:
     * insert a new cell at "New Sheet!A1" with the initial value of "Hello World!"
     *  http://127.0.0.1:8080/doc/id/1113/xls/cell/add/New Sheet!A1/Hello World!
     * 	
     * eg:
     * insert a new cell at "New Sheet!A2" with the initial value of "=SUM(10+11)"
     *  http://127.0.0.1:8080/doc/id/1113/xls/cell/add/New Sheet!A2/=SUM(10+11)							  
     * 
     * @author John McMahon [ Feb 1, 2007 ]
     * @param reqstr
     */
    public static Map<String, Object> parseRequest(String reqstr, String raw) {
        Map<String, Object> ret = new HashMap<String, Object>();
        String[] cmds = StringTool.getTokensUsingDelim(reqstr, "/");
        if (cmds.length > 3) ret.put(WorkBookCommander.LOADMETHOD, cmds[4]);
        if (cmds.length > 5) ret.put(cmds[4], cmds[5]);
        if (cmds.length > 6) ret.put(WorkBookCommander.FORMAT, cmds[6]);
        if (cmds.length > 7) ret.put(WorkBookCommander.PLUGINNAME, cmds[7]);
        if (cmds.length > 8) ret.put(WorkBookCommander.COMMAND, cmds[8]);
        if (cmds.length > 9) {
            ret.put(WorkBookCommander.RESOURCE, cmds[9]);
        }
        if (cmds.length > 10) {
            if (raw.indexOf("%2F") > -1) {
                cmds[10] = raw.substring(raw.lastIndexOf("/") + 1);
            }
            ret.put(WorkBookCommander.VALUE, cmds[10]);
        }
        if (cmds[8].equals("add") || cmds[8].equals("setlink")) {
            cmds[9] = com.extentech.toolkit.StringTool.replaceText(cmds[9], " ", "%20");
            int cmdlen = cmds[9].length() + 1;
            raw = raw.substring(raw.indexOf(cmds[9]) + cmdlen);
            ret.put(WorkBookCommander.EXTRAPATH, raw);
        }
        return ret;
    }

    private ChatMessageClient getMessageClient(HttpServletRequest request, User u) {
        HttpSession sesh = request.getSession();
        ChatMessageClient cmc = (ChatMessageClient) sesh.getAttribute("org.docsfree.legacy.message_client");
        if (cmc == null) {
            ChatMessageClient clnt = new ChatMessageClient(u);
            sesh.setAttribute("org.docsfree.legacy.message_client", clnt);
            return clnt;
        }
        return cmc;
    }

    /** Get a valid logged in user -- TODO: replace encrypted cred.
     * ------------------------------------------------------------
     * 
     * @author John [ Aug 25, 2007 ]
     * @param request
     * @param response
     * @return
     */
    private User getUser(Map<String, Object> parameters, HttpServletRequest request, HttpServletResponse response) throws AuthException {
        Object rex = request.getSession().getAttribute("org.docsfree.legacy.user");
        if (rex != null) return (User) rex;
        if (!loader.getStorage().getAuth().getProviderName().equalsIgnoreCase("sheetster")) {
            User user = userFactory.getUser();
            loader.getStorage().getAuth().loginLocalUser(parameters, user, "N@PASSW@rD", true, true);
            return user;
        }
        String stx = getInitParameter("org.docsfree.legacy.auth_ticket");
        if ((request.getParameter("username") != null) || (request.getParameter(stx) != null)) {
            User user = userFactory.getUser();
            HttpSession session = request.getSession();
            try {
                String unm = request.getParameter("username");
                String pwx = request.getParameter("password");
                String upas = null;
                if (stx != null) upas = request.getParameter(stx);
                if (upas != null) try {
                    String seshid = session.getId();
                    String serverKey = session.getServletContext().getInitParameter("org.docsfree.legacy.server_key");
                    String decrptd = UserFactory.decryptUserPass(upas, serverKey);
                    String[] up = com.extentech.toolkit.StringTool.getTokensUsingDelim(decrptd, "|");
                    unm = up[0];
                    pwx = up[1];
                } catch (Exception ex) {
                    try {
                        sendTextResponse(request, response, "Authentication Failure", "xhtml");
                    } catch (Exception e) {
                        Logger.logErr("WBS: Problem getting user " + user.getUserName() + ".", e);
                    }
                }
                user.setUserName(unm);
                loader.getStorage().getAuth().loginLocalUser(parameters, user, pwx, true, true);
                session.removeAttribute("org.docsfree.legacy.refpage");
                return user;
            } catch (AuthException e) {
                session.removeAttribute("org.docsfree.legacy.user");
                metrics.incrementCounter("failed_logins");
                try {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "DocumentServletBase.getUser() Authentication Failure.");
                } catch (Exception ex) {
                    Logger.logErr("WBS: Problem getting user " + user.getUserName() + ".", ex);
                }
            }
        }
        return null;
    }

    public String toString() {
        return "Extentech Document Services Servlet";
    }

    /** ------------------------------------------------------------
     * 
     * @author john [ Jul 17, 2009 ]
     * @return Returns the loader.
     */
    public DocumentLoader getLoader() {
        return loader;
    }

    /** ------------------------------------------------------------
     * 
     * @author john [ Jul 17, 2009 ]
     * @param loader The loader to set.
     */
    public void setLoader(DocumentLoader loader) {
        loader = loader;
    }

    /** ------------------------------------------------------------
     * 
     * @author john [ Sep 4, 2009 ]
     * @return Returns the pLoader.
     */
    public PluginLoader getPluginLoader() {
        return pLoader;
    }

    /** ------------------------------------------------------------
     * 
     * @author john [ Sep 4, 2009 ]
     * @param loader The pLoader to set.
     */
    public void setPluginLoader(PluginLoader loader) {
        pLoader = loader;
    }
}
