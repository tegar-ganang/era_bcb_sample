package fi.hip.gb.disk.transport.webdav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.slide.common.NamespaceAccessToken;
import org.apache.slide.common.SlideException;
import org.apache.slide.content.Content;
import org.apache.slide.content.NodeRevisionDescriptor;
import org.apache.slide.content.NodeRevisionDescriptors;
import org.apache.slide.lock.Lock;
import org.apache.slide.lock.NodeLock;
import org.apache.slide.macro.DeleteMacroException;
import org.apache.slide.security.AccessDeniedException;
import org.apache.slide.security.NodePermission;
import org.apache.slide.security.Security;
import org.apache.slide.structure.LinkedObjectNotFoundException;
import org.apache.slide.structure.ObjectNode;
import org.apache.slide.structure.ObjectNotFoundException;
import org.apache.slide.structure.Structure;
import org.apache.slide.util.Messages;
import org.apache.slide.webdav.WebdavException;
import org.apache.slide.webdav.WebdavServletConfig;
import org.apache.slide.webdav.method.FineGrainedLockingMethod;
import org.apache.slide.webdav.method.GetMethod;
import org.apache.slide.webdav.util.PreconditionViolationException;
import org.apache.slide.webdav.util.WebdavStatus;
import org.apache.slide.webdav.util.WebdavUtils;
import fi.hip.gb.disk.FileManager;
import fi.hip.gb.disk.conf.Config;
import fi.hip.gb.disk.info.FileInfo;
import fi.hip.gb.disk.info.LocalInfoBean;
import fi.hip.gb.disk.info.Scheduler;

/**
 * Extends the Jakarta Slide's GET-method with our
 * own methods for distributed storage instead
 * of local directory. Extends directory listing and
 * adds operations over such as delete, exists through
 * web browser.
 * 
 * @author Juho Karppinen
 */
public class DiskGetMethod extends GetMethod implements FineGrainedLockingMethod {

    /** HTTP Date format pattern (RFC 2068, 822, 1123) */
    public static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    /** Date formatter */
    private static final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    /** Scheduler shows the status of the network, updated every time the state is refreshed */
    private static Scheduler scheduler = null;

    private static final Log log = LogFactory.getLog(DiskGetMethod.class);

    /**
     * Constructor.
     *
     * @param token     the token for accessing the namespace
     * @param config    configuration of the WebDAV servlet
     */
    public DiskGetMethod(NamespaceAccessToken token, WebdavServletConfig config) {
        super(token, config);
    }

    /**
     * Execute request using superclass implemetation, this is only for logging
     * purposes.
     *
     * @exception WebdavException Can't access resource
     */
    protected void executeRequest() throws WebdavException {
        if (this.resourcePath.endsWith("index.html")) {
            this.resourcePath = this.resourcePath.substring(0, this.resourcePath.indexOf(("index.html")));
        }
        String remotePath = this.resourcePath.substring(this.config.getScope().length());
        if (this.req.getQueryString() != null && this.req.getQueryString().length() > 0) {
            try {
                if (this.req.getQueryString().startsWith("res=")) {
                    String res = this.req.getQueryString().substring(4);
                    if (res.equals("gblogo_330.jpg") || res.startsWith("resources/")) sendFile(new FileInputStream(new File("./webapps/gb-disk/" + res)), new File(res).getName(), null);
                } else if ("clear".equals(this.req.getQueryString())) {
                    if (Config.isFrontEnd()) FileManager.clearFinished();
                    response("Cache cleared", 1);
                } else if ("delete".equals(this.req.getQueryString())) {
                    macro.delete(slideToken, this.requestUri, null, null);
                    response("File " + this.resourcePath + " deleted", 1);
                } else if ("play".equals(this.req.getQueryString())) {
                    StringBuffer sb = WebdavDiskStore.getM3u(this.requestUri);
                    log.info("Playlist for " + this.resourcePath + "\n" + sb.toString());
                    this.sendFile(new ByteArrayInputStream(sb.toString().getBytes()), null, "audio/x-mpegurl");
                } else if ("check".equals(this.req.getQueryString())) {
                    FileManager fm = new FileManager();
                    fm.exists(remotePath);
                    fm.run();
                    response("Refreshing state. Please wait...", 2);
                } else if ("exists".equals(this.req.getQueryString())) {
                    if (new WebdavDiskStore().objectExists(this.resourcePath)) resp.getOutputStream().write(1); else resp.getOutputStream().write(0);
                    resp.getOutputStream().close();
                } else if ("errors".equals(this.req.getQueryString())) {
                    LocalInfoBean bean = FileManager.findLocalFile(this.resourcePath);
                    if (bean != null) {
                        response(bean.getErrorMessage().getMessage(), -1);
                    } else {
                        response("No errors for file " + this.resourcePath, -1);
                    }
                } else if ("refresh".equals(this.req.getQueryString())) {
                    Config.getGroupInfosys().refreshState();
                    scheduler = Config.getScheduler();
                    response("Refreshing state. Please wait...", 2);
                }
            } catch (DeleteMacroException dme) {
                SlideException exception = (SlideException) dme.enumerateExceptions().nextElement();
                if (exception instanceof PreconditionViolationException) {
                    try {
                        sendPreconditionViolation((PreconditionViolationException) exception);
                    } catch (IOException ex) {
                        int statusCode = WebdavStatus.SC_INTERNAL_SERVER_ERROR;
                        sendError(statusCode, ex);
                        throw new WebdavException(statusCode);
                    }
                } else {
                    int statusCode = getErrorCode(exception);
                    sendError(statusCode, exception);
                    throw new WebdavException(statusCode);
                }
                throw new WebdavException(WebdavStatus.SC_ACCEPTED, false);
            } catch (Exception e) {
                sendError(505, e);
                throw new WebdavException(505);
            }
            return;
        } else if (WebdavDiskStore.getDiskFilePath(this.resourcePath).startsWith("/" + "login")) {
            response("<h1>Welcome!</h1> You are now logged in with write access.", 2);
            return;
        }
        log.info("Getting " + this.resourcePath);
        if (false) {
            try {
                sendFile(new FileInputStream(new File("./webapps/gb-disk/" + remotePath)), null, null);
                return;
            } catch (IOException e1) {
                throw new WebdavException(404);
            }
        }
        FileManager fm = null;
        File fileToGet = new File(Config.getSiloDir() + remotePath);
        if (Config.isFrontEnd() && !fileToGet.exists()) {
            FileInfo info = Config.getFileInfosys().findFile(remotePath);
            if (info != null && info.isDirectory() == false) {
                try {
                    fm = new FileManager();
                    fm.get(remotePath, fileToGet);
                    Thread t = new Thread(fm);
                    t.start();
                    t.join();
                } catch (Exception e) {
                    log.error("Failed to fetch the file " + remotePath + " from GB-DISK: " + e.getMessage(), e);
                    sendError(505, e);
                    throw new WebdavException(505);
                }
                Exception e = fm.getCurrentFile().getErrorMessage();
                if (e != null) {
                    sendError(404, e.getMessage());
                    throw new WebdavException(404);
                }
            }
        }
        super.executeRequest();
        if (fm != null) {
            fm.perf.done();
        }
    }

    /**
     * Gives a html response to the operation.
     * <p>
     * If our current path starts with /login/x/y then our target url is folder /x/y instead
     * of root folder. For other urls we take the parent directory.
     * @param msg message to the user
     * @param redirect seconds to wait until redirection back to the previous page, -1 disables 
     */
    private void response(String msg, int redirect) {
        try {
            String BASE = "";
            if (Config.getPathInfo().length() > 0) BASE += "/" + Config.getPathInfo();
            String url = WebdavUtils.getAbsolutePath(this.resourcePath, req, config);
            String loginUrl = BASE + "/login";
            if (url.startsWith(loginUrl)) {
                url = BASE + url.substring(loginUrl.length());
            } else {
                url = url.substring(0, url.lastIndexOf("/"));
            }
            if (url.length() == 0) url = "/";
            PrintWriter display = this.resp.getWriter();
            this.resp.setContentType("text/html");
            display.println("<html><head>");
            display.println("<html><head>");
            if (redirect >= 0) {
                display.println("<META HTTP-EQUIV=\"REFRESH\" CONTENT=\"" + redirect + "; URL=" + url + "\">");
            }
            display.println("</head><body>");
            display.println("<p>" + msg + "</p>");
            display.println("<p><a href=\"" + url + "\">Back to GB-DISK '" + url + "'</a></p>");
            display.println("</html>");
            display.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Sends a file to the client.
     * @param file the file stream to send
     * @param filename name to the donwloader, use null for no content-disposition header
     * @param contentType type of the file, use null for application/unknown
     * @throws IOException
     */
    private void sendFile(InputStream file, String filename, String contentType) throws IOException {
        log.info("sending file " + filename);
        InputStream in = null;
        OutputStream out = null;
        try {
            if (filename != null) {
                if (contentType == null) {
                    if (filename.endsWith(".jpg")) contentType = "image/jpeg"; else if (filename.endsWith(".html")) contentType = "text/html"; else if (filename.endsWith(".css")) contentType = "text/css"; else {
                        contentType = "application/unknown";
                        this.resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    }
                    this.resp.setContentType(contentType);
                }
            }
            out = new BufferedOutputStream(this.resp.getOutputStream());
            in = new BufferedInputStream(file);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            return;
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
            if (out != null) try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    protected void displayDirectory() throws IOException {
        String directoryBrowsing = config.getInitParameter("directory-browsing");
        if ("true".equalsIgnoreCase(directoryBrowsing)) {
            try {
                this.generateList();
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (ObjectNotFoundException e) {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            } catch (LinkedObjectNotFoundException e) {
                resp.sendError(WebdavStatus.SC_NOT_FOUND);
            } catch (SlideException e) {
                resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }
    }

    @SuppressWarnings("unchecked")
    public void generateList() throws IOException, SlideException {
        ObjectNode object = this.structure.retrieve(slideToken, resourcePath);
        String name = object.getUri();
        String currentPath = name.substring(this.config.getScope().length());
        resp.setContentType("text/html; charset=\"UTF-8\"");
        Content content = this.token.getContentHelper();
        Lock lock = this.token.getLockHelper();
        Security security = this.token.getSecurityHelper();
        Structure structure = this.token.getStructureHelper();
        int trim = name.length();
        if (!name.endsWith("/")) trim += 1;
        if (name.equals("/")) trim = 1;
        PrintWriter writer = new PrintWriter(resp.getWriter());
        writer.print("<html>\r\n");
        writer.print("<head>\r\n");
        writer.print("<meta http-equiv=\"Content-type\" content=\"text/html; charset=UTF-8\" >\r\n");
        writer.print("</meta>\r\n");
        writer.print("<title>");
        writer.print("GridBlocks DISK: " + currentPath + "/");
        writer.print("</title>\r\n</head>\r\n");
        writer.print("<body bgcolor=\"white\">\r\n");
        writer.print("<table width=\"90%\" cellspacing=\"0\" cellpadding=\"5\" align=\"center\">\r\n");
        writer.print("<tr><td colspan=\"4\"><h1>\r\n<strong>");
        writer.print("<img src=\"" + this.req.getContextPath() + "/?res=gblogo_330.jpg\">");
        if (Config.getGroupInfosys() != null) {
            writer.print("\n DISK the Distributed Storage");
        } else {
            writer.print("\n DISK the Personal Storage");
        }
        writer.print("</strong>\r\n</h1></td>\r\n");
        writer.print("<td colspan=\"2\" align=right><a href=" + this.req.getContextPath() + "/login" + currentPath + ">[login]</a>");
        if (Config.getGroupInfosys() != null) {
            writer.print(" / <a href=" + this.req.getContextPath() + currentPath + "/?refresh>[refresh state]</a>");
        }
        if (Config.isFrontEnd()) {
            writer.print(" / <a href=" + this.req.getContextPath() + currentPath + "/?clear>[clear cache]</a>");
        }
        writer.print(" / <a href=" + this.req.getContextPath() + currentPath + "/?play>[play]</a>");
        writer.print("</td></tr>\r\n");
        if (Config.getGroupInfosys() != null) {
            if (scheduler == null) {
                scheduler = Config.getScheduler();
            }
            writer.print("<tr><td colspan=\"6\">\n");
            if (scheduler != null) {
                if (this.slideToken.getCredentialsToken().isTrusted()) writer.println(scheduler.toStringWithAuthInfo()); else writer.println(scheduler.toString());
            } else {
                writer.print("Error: scheduler instance is null");
            }
            writer.print("</td></tr>\n");
        }
        writer.print("<tr><td colspan=\"6\" >\r\n");
        if (Config.isGuestWriteable() || this.slideToken.getCredentialsToken().isTrusted()) {
            String actionPath = currentPath;
            if (actionPath.length() == 0) actionPath += "/";
            writer.print("<h3>Upload files</h3>\r\n");
            writer.print("<p>\n");
            writer.println("<form method=\"post\" " + "action=\"" + this.req.getContextPath() + actionPath + "\" " + "enctype=\"multipart/form-data\">" + "<input type=\"file\" name=\"file1\">" + "<input type=\"submit\" name=\"Send\">" + "</form></p>\n");
            writer.println("<p>Or connect with a WebDAV client to <a href=\"" + this.req.getRequestURL().toString() + "\">" + this.req.getRequestURL().toString() + "</a></p>");
        } else {
            writer.print("<strong>No uploading allowed until logged in.</strong>.");
        }
        writer.print("</td></tr>\r\n");
        writer.print("<tr><td colspan=\"6\"><h3>\r\n<strong>");
        writer.print("<p>\r\n");
        writer.print(Messages.format("org.apache.slide.webdav.GetMethod.directorylistingfor", ": "));
        String path = "/";
        String fullpath = WebdavUtils.getAbsolutePath(config.getScope(), req, config) + currentPath;
        System.out.println(fullpath + ":");
        for (String p : fullpath.split("/")) {
            boolean root = p.length() == 0;
            boolean leaf = fullpath.length() == 0 || currentPath.equals("/") || fullpath.equals(path + p);
            path += p;
            if (path.equals("/" + Config.getPathInfo())) {
                p = "# /" + p;
            }
            if (leaf) {
                writer.print(p);
            } else {
                writer.print("<a href=\"" + path + "\">");
                writer.print(p);
                writer.print("</a>");
                if (!root) {
                    writer.print("/");
                    path += "/";
                }
            }
        }
        writer.print("</p>\r\n");
        writer.print("</strong>\r\n</h3>\r\n");
        Enumeration<NodePermission> permissionsList = null;
        Enumeration<NodeLock> locksList = null;
        try {
            permissionsList = security.enumeratePermissions(slideToken, object.getUri());
            locksList = lock.enumerateLocks(slideToken, object.getUri(), false);
        } catch (SlideException e) {
        }
        if (false && org.apache.slide.util.Configuration.useIntegratedSecurity()) {
            displayPermissions(permissionsList, writer, false);
        }
        displayLocks(locksList, writer, false);
        writer.print("<table cellspacing=\"0\" width=\"100%\">");
        writer.print("<tr bgcolor=\"#cccccc\">\r\n");
        writer.print("<td align=\"left\" colspan=\"2\">");
        writer.print("<font size=\"+1\"><strong>");
        writer.print(Messages.message("org.apache.slide.webdav.GetMethod.filename"));
        writer.print("</strong></font></td>\r\n");
        writer.print("<td align=\"center\"><font size=\"+1\"><strong>");
        writer.print("Op");
        writer.print("</strong></font></td>\r\n");
        writer.print("<td align=\"center\"><font size=\"+1\"><strong>");
        writer.print(Messages.message("org.apache.slide.webdav.GetMethod.size"));
        writer.print("</strong></font></td>\r\n");
        writer.print("<td align=\"right\"><font size=\"+1\"><strong>");
        writer.print("Status");
        writer.print("</strong></font></td>\r\n");
        writer.print("<td align=\"right\"><font size=\"+1\"><strong>");
        writer.print(Messages.message("org.apache.slide.webdav.GetMethod.lastModified"));
        writer.print("</strong></font></td>\r\n");
        writer.print("</tr>\r\n");
        Enumeration<ObjectNode> resources = structure.getChildren(slideToken, object);
        Vector<NodeRevisionDescriptor> sorted = new Vector<NodeRevisionDescriptor>();
        while (resources.hasMoreElements()) {
            ObjectNode o = resources.nextElement();
            try {
                NodeRevisionDescriptors revisionDescriptors = content.retrieve(slideToken, o.getUri());
                NodeRevisionDescriptor currentDescriptor = content.retrieve(slideToken, revisionDescriptors);
                if (currentDescriptor != null) {
                    currentDescriptor.setName(o.getUri());
                    sorted.add(currentDescriptor);
                } else {
                    log.info("null descriptor for " + o.getUri());
                }
            } catch (SlideException e) {
            }
        }
        Collections.sort(sorted, new RevisionSorter());
        long totalSize = 0;
        boolean shade = false;
        for (NodeRevisionDescriptor currentDescriptor : sorted) {
            String currentResource = currentDescriptor.getName();
            permissionsList = null;
            locksList = null;
            String fileName = currentResource.substring(trim);
            String url = null;
            long size = -1;
            Date modified = null;
            String status = "&nbsp;";
            try {
                permissionsList = security.enumeratePermissions(slideToken, currentResource);
                locksList = lock.enumerateLocks(slideToken, currentResource, false);
            } catch (SlideException e) {
            }
            if (WebdavDiskStore.isErrFilename(currentResource)) {
                continue;
            }
            if (currentDescriptor != null) {
                if (WebdavUtils.isCollection(currentDescriptor)) {
                    fileName += "/";
                } else if (WebdavUtils.isRedirectref(currentDescriptor)) {
                    fileName += "*";
                }
                url = WebdavUtils.getAbsolutePath(currentResource, req, config);
                if (WebdavUtils.isCollection(currentDescriptor) == false && WebdavUtils.isRedirectref(currentDescriptor) == false) {
                    size = currentDescriptor.getContentLength();
                    totalSize += size;
                }
                modified = currentDescriptor.getLastModifiedAsDate();
                if (Config.isFrontEnd()) {
                    String remotePath = currentResource.substring(this.config.getScope().length());
                    LocalInfoBean localInfo = FileManager.findLocalFile(remotePath);
                    if (localInfo != null) {
                        status = localInfo.getStatus();
                    }
                } else {
                    status = "local";
                }
            }
            File errFile = WebdavDiskStore.getErrFile(currentResource);
            if (errFile.exists()) {
                if (!status.equals("&nbsp;")) status += ", ";
                status += "<a href=\"" + url.substring(0, url.lastIndexOf("/")) + "/" + errFile.getName() + "\">errors</a>";
            }
            if (fileName != null) {
                displayLocks(locksList, writer, shade);
                displayFile(writer, fileName, url, size, modified, status, shade);
                shade = !shade;
            }
        }
        writer.print("<tr><td colspan=\"6\">&nbsp;</td></tr>\r\n");
        writer.print("<tr><td colspan=\"3\" bgcolor=\"#cccccc\">");
        writer.print("<font size=\"-1\">");
        writer.print("GridBlocks DISK & ");
        writer.print(Messages.message("org.apache.slide.webdav.GetMethod.version"));
        writer.print("</font></td>\r\n");
        writer.print("<td colspan=\"1\" align=\"right\" bgcolor=\"#cccccc\">");
        writer.print("<font size=\"-1\">");
        writer.print(renderSize(totalSize));
        writer.print("</font></td>\r\n");
        writer.print("<td colspan=\"2\" align=\"right\" bgcolor=\"#cccccc\">");
        writer.print("<font size=\"-1\">");
        writer.print(formatter.format(new Date()));
        writer.print("</font></td></tr>\r\n");
        writer.print("</table>");
        writer.print("</td></tr></table>\r\n");
        writer.print("</body>\r\n");
        writer.print("</html>\r\n");
        writer.flush();
    }

    private void displayFile(PrintWriter writer, String name, String url, long size, Date modified, String status, boolean shade) {
        writer.print("<tr");
        if (shade) {
            writer.print(" bgcolor=\"dddddd\"");
        } else {
            writer.print(" bgcolor=\"eeeeee\"");
        }
        writer.print(">\r\n");
        writer.print("<td align=\"left\" colspan=\"2\">&nbsp;&nbsp;\r\n");
        writer.print("<a href=\"");
        writer.print(url);
        writer.print("\"><tt>");
        writer.print(name);
        writer.print("</tt></a></td>\r\n");
        writer.print("<td align=\"right\"><tt>\n");
        if (name.endsWith("/") == false) {
            if (Config.isFrontEnd()) {
                writer.print("<a href=\"");
                writer.print(url);
                writer.print("?check\">check</a> ");
            }
            writer.print("<a href=\"");
            writer.print(url);
            writer.print("?delete\">del</a>");
        }
        writer.print("</tt></td>\r\n");
        writer.print("<td align=\"right\"><tt>");
        if (size > 0) writer.print(renderSize(size)); else writer.print("&nbsp;");
        writer.print("</tt></td>\r\n");
        writer.print("<td align=\"right\"><tt>");
        writer.print(status);
        writer.print("</tt></td>\r\n");
        writer.print("<td align=\"right\"><tt>");
        if (modified != null) {
            writer.print(formatter.format(modified));
        } else {
            writer.print("&nbsp;");
        }
        writer.print("</tt></td>\r\n");
        writer.print("</tr>\r\n");
    }

    /**
     * Display an ACL list.
     *
     * @param permissionsList   the list of NodePermission objects
     * @param writer            the output will be appended to this writer
     * @param shade             whether the row should be displayed darker
     */
    protected void displayPermissions(Enumeration<NodePermission> permissionsList, PrintWriter writer, boolean shade) throws IOException {
        boolean hideAcl = false;
        String hideAclStr = config.getInitParameter("directory-browsing-hide-acl");
        if ("true".equalsIgnoreCase(hideAclStr)) hideAcl = true;
        if (!hideAcl) {
            if ((permissionsList != null) && (permissionsList.hasMoreElements())) {
                writer.print("<tr" + (shade ? " bgcolor=\"eeeeee\"" : " bgcolor=\"dddddd\"") + ">\r\n");
                writer.print("<td align=\"left\" colspan=\"5\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.aclinfo"));
                writer.print("</b></tt></td>\r\n");
                writer.print("</tr>\r\n");
                writer.print("<tr");
                if (!shade) {
                    writer.print(" bgcolor=\"dddddd\"");
                } else {
                    writer.print(" bgcolor=\"eeeeee\"");
                }
                writer.print(">\r\n");
                writer.print("<td align=\"left\" colspan=\"2\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.subject"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"left\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.action"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"right\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.inheritable"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"right\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.deny"));
                writer.print("</b></tt></td>\r\n");
                writer.print("</tr>\r\n");
                while (permissionsList.hasMoreElements()) {
                    writer.print("<tr" + (shade ? " bgcolor=\"eeeeee\"" : " bgcolor=\"dddddd\"") + ">\r\n");
                    NodePermission currentPermission = permissionsList.nextElement();
                    writer.print("<td align=\"left\" colspan=\"2\"><tt>");
                    writer.print(currentPermission.getSubjectUri());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"left\"><tt>");
                    writer.print(currentPermission.getActionUri());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"right\"><tt>");
                    writer.print(currentPermission.isInheritable());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"right\"><tt>");
                    writer.print(currentPermission.isNegative());
                    writer.print("</tt></td>\r\n");
                    writer.print("</tr>\r\n");
                }
            }
        }
    }

    /**
     * Display a lock list.
     *
     * @param locksList   the list of NodeLock objects
     * @param writer      the output will be appended to this writer
     * @param shade       whether the row should be displayed darker
     */
    protected void displayLocks(Enumeration<NodeLock> locksList, PrintWriter writer, boolean shade) throws IOException {
        String hideLocksStr = config.getInitParameter("directory-browsing-hide-locks");
        if (Boolean.parseBoolean(hideLocksStr)) {
            if ((locksList != null) && (locksList.hasMoreElements())) {
                writer.print("<tr" + (shade ? " bgcolor=\"eeeeee\"" : " bgcolor=\"dddddd\"") + ">\r\n");
                writer.print("<td align=\"left\" colspan=\"5\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.locksinfo"));
                writer.print("</b></tt></td>\r\n");
                writer.print("</tr>\r\n");
                writer.print("<tr");
                if (!shade) {
                    writer.print(" bgcolor=\"dddddd\"");
                } else {
                    writer.print(" bgcolor=\"eeeeee\"");
                }
                writer.print(">\r\n");
                writer.print("<td align=\"left\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.subject"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"left\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.type"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"right\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.expiration"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"right\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.inheritable"));
                writer.print("</b></tt></td>\r\n");
                writer.print("<td align=\"right\"><tt><b>");
                writer.print(Messages.message("org.apache.slide.webdav.GetMethod.exclusive"));
                writer.print("</b></tt></td>\r\n");
                writer.print("</tr>\r\n");
                while (locksList.hasMoreElements()) {
                    writer.print("<tr" + (shade ? " bgcolor=\"eeeeee\"" : " bgcolor=\"dddddd\"") + ">\r\n");
                    NodeLock currentLock = locksList.nextElement();
                    writer.print("<td align=\"left\"><tt>");
                    writer.print(currentLock.getSubjectUri());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"left\"><tt>");
                    writer.print(currentLock.getTypeUri());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"right\"><tt>");
                    writer.print(formatter.format(currentLock.getExpirationDate()));
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"right\"><tt>");
                    writer.print(currentLock.isInheritable());
                    writer.print("</tt></td>\r\n");
                    writer.print("<td align=\"right\"><tt>");
                    writer.print(currentLock.isExclusive());
                    writer.print("</tt></td>\r\n");
                }
            }
        }
    }

    /**
     * Render the specified file size (in bytes).
     *
     * @param size File size (in bytes)
     */
    protected String renderSize(long size) {
        long leftSide = size / 1024;
        long rightSide = (size % 1024) / 103;
        if ((leftSide == 0) && (rightSide == 0) && (size > 0)) rightSide = 1;
        return ("" + leftSide + "." + rightSide + " kb");
    }

    class RevisionSorter implements Comparator {

        public int compare(Object o1, Object o2) {
            NodeRevisionDescriptor d1 = (NodeRevisionDescriptor) o1;
            NodeRevisionDescriptor d2 = (NodeRevisionDescriptor) o2;
            boolean f1 = WebdavUtils.isCollection(d1);
            boolean f2 = WebdavUtils.isCollection(d2);
            if (f1 && !f2) {
                return -1;
            } else if (f2 && !f1) {
                return 1;
            }
            return d1.getName().compareTo(d2.getName());
        }
    }

    ;
}
