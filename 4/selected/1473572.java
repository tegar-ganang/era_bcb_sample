package shttp.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.File;
import java.io.FileInputStream;
import shttp.utils.StringSplitter;

/** 
 *
 * @author  dmlarsson
 * @version 
 */
public class FileSystemServlet extends HttpServlet {

    private File docRoot;

    private ServletContext context;

    private String[] defFiles;

    /** Initializes the servlet.
    */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String tmp = config.getInitParameter("root");
        if (tmp == null) this.docRoot = new File("html" + File.separator); else this.docRoot = new File(tmp);
        this.context = config.getServletContext();
        tmp = config.getInitParameter("default");
        if (tmp != null) this.defFiles = StringSplitter.splitSemiColonList(tmp); else this.defFiles = new String[0];
    }

    /** Destroys the servlet.
    */
    public void destroy() {
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
    * @param request servlet request
    * @param response servlet response
    */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        String uri = request.getRequestURI();
        String mt = context.getMimeType(uri);
        if (mt == null) response.setContentType("text/html"); else response.setContentType(mt);
        if (!File.separator.equals("/")) {
        }
        int defcnt = 0;
        boolean tryDefault = false;
        if (defFiles.length > 0 && uri.endsWith("/")) tryDefault = true;
        File rootFile = new File(docRoot, uri);
        File sendFile;
        if (tryDefault) sendFile = new File(rootFile, defFiles[defcnt++]); else sendFile = rootFile;
        FileInputStream in;
        while (true) {
            try {
                in = new FileInputStream(sendFile);
                break;
            } catch (java.io.FileNotFoundException e) {
                if (defcnt > defFiles.length || !tryDefault) {
                    response.sendError(404, e.toString());
                    return;
                }
                sendFile = new File(rootFile, defFiles[defcnt++]);
            }
        }
        response.setContentLength((int) sendFile.length());
        java.io.OutputStream out = response.getOutputStream();
        byte[] b = new byte[1024];
        int cnt;
        while ((cnt = in.read(b, 0, b.length)) > -1) out.write(b, 0, cnt);
        in.close();
        out.close();
    }

    /** Handles the HTTP <code>GET</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
    * @param request servlet request
    * @param response servlet response
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Enables access to an underlying filesystem.";
    }
}
