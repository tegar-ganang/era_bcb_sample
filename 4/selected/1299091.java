package com.planes.automirror;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 *
 * @author Steve Tousignant (planestraveler)
 */
public class MirrorDisponibility extends HttpServlet {

    private static final int BUFFER_SIZE = 10240;

    private static final String VERSION = "0.0.2";

    private static String IP = "0.0.0.0";

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String basePath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String requestedURL = requestURI.replace(basePath + "/", "");
        Map<String, Statistique> stats = MirrorManager.getInstance().getURLStatistics();
        Set<String> presentBaseDir = null;
        presentBaseDir = MirrorManager.getInstance().getPresentBaseDir();
        if (requestedURL.length() <= 1) {
            buildHome(request, response);
        } else {
            if (presentBaseDir.contains(requestedURL)) {
                buildLocallyStockedFiles(request, response);
            } else {
                if (stats.containsKey(requestedURL)) {
                    PrintWriter out = response.getWriter();
                    response.setContentType("text/html;charset=UTF-8");
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Detail on " + requestedURL + "</title>");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("RequestedURL = " + requestedURL + "<br/>");
                    stats.get(requestedURL).buildTree(out);
                    buildFoot(out);
                    out.println("</body>");
                    out.println("</html>");
                    out.close();
                } else {
                    boolean alreadyTold = false;
                    DownloadTask downloadTask = MirrorManager.getInstance().downloadFromThis(requestedURL);
                    InputStream ins = downloadTask.getInputStream();
                    Thread task = new Thread(downloadTask);
                    task.setName("download : " + requestedURL);
                    task.start();
                    log(MessageFormat.format("Started to write to output at {0,date, long}", new Date()));
                    OutputStream outputFile = response.getOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read = ins.read(buffer);
                    log(MessageFormat.format("{0,number,integer} bytes was read", read));
                    while (read >= 0) {
                        if (read > 0 && !alreadyTold) {
                            if (downloadTask.getContentLength() > 0) {
                                System.out.println(requestedURL + " is " + downloadTask.getContentLength() + " long");
                                response.setContentLength(downloadTask.getContentLength());
                            }
                            if (downloadTask.getContentType() != null) {
                                response.setContentType(downloadTask.getContentType());
                            }
                            alreadyTold = true;
                        }
                        outputFile.write(buffer, 0, read);
                        read = ins.read(buffer);
                    }
                    log(MessageFormat.format("finished to write to output at {0,date,long}", new Date()));
                    ins.close();
                    outputFile.close();
                }
            }
        }
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }

    public void init() throws ServletException {
        Enumeration configKeys = getInitParameterNames();
        while (configKeys.hasMoreElements()) {
            String names = (String) configKeys.nextElement();
            getInitParameter(names);
        }
        super.init();
    }

    public void initMirrorStats(ServletConfig config) {
        Enumeration params = config.getInitParameterNames();
        while (params.hasMoreElements()) {
            String key = (String) params.nextElement();
            MirrorManager.getInstance().setURL(config.getInitParameter(key));
        }
    }

    public void initWhereWeAre() {
        InputStream ins = null;
        try {
            URL checkip = new URL("http://checkip.dyndns.org/");
            ins = checkip.openStream();
            byte buffer[] = new byte[512];
            int read = ins.read(buffer);
            String content = new String(buffer, 0, read, "ISO-8859-1");
            int before = content.indexOf(":") + 1;
            int after = content.indexOf("<", before);
            IP = content.substring(before, after).trim();
        } catch (IOException e) {
            Logger.getLogger("autosync-mirror.WhereWeAre").log(Level.SEVERE, "Error trying to fetch ip address", e);
        } finally {
            try {
                ins.close();
            } catch (IOException e) {
            }
        }
    }

    public void init(ServletConfig config) throws ServletException {
        initWhereWeAre();
        initMirrorStats(config);
        super.init(config);
    }

    private void buildHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String basePath = request.getContextPath();
        Set<String> presentBaseDir = MirrorManager.getInstance().getPresentBaseDir();
        Map<String, Statistique> stats = MirrorManager.getInstance().getURLStatistics();
        PrintWriter out = response.getWriter();
        response.setContentType("text/html;charset=UTF-8");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet MirrorDisponibility</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Servlet MirrorDisponibility at " + request.getContextPath() + "</h1>");
        out.println(request.getRequestURI() + "<br/>");
        out.println("We are at the ip : " + IP + "<br/>");
        out.println("<table border='0' cellspacing='0'>");
        for (String dir : presentBaseDir) {
            out.println("<tr>");
            out.println("  <td><a href='./" + dir + "'>" + dir + "</td>");
            out.println("</tr>");
        }
        out.println("</table>");
        out.println("<table border='1' cellspacing='0' cellpadding='2'>");
        for (Map.Entry<String, Statistique> stat : stats.entrySet()) {
            out.println("  <tr>");
            String link;
            if (stat.getValue().isTestDone()) {
                link = "<a href='" + basePath + "/" + stat.getKey() + "'>" + stat.getKey() + "</a>";
            } else {
                link = stat.getKey();
            }
            out.println("    <td>" + link + "</td>");
            out.println("    <td>" + stat.getValue().getSpeed() + "</td>");
            out.println("    <td>" + stat.getValue().isTestDone() + "</td>");
            out.println("    <td>" + (stat.getValue().getMessage() == null ? "Online" : "Offline") + "</td>");
            if (stat.getValue().getMessage() != null) {
                out.println("    <td>" + stat.getValue().getMessage() + "</td>");
            }
            out.println("  </tr>");
        }
        out.println("</table>");
        buildFoot(out);
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private void buildLocallyStockedFiles(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String basePath = request.getContextPath();
        String requestedURL = request.getRequestURI().replace(basePath + "/", "");
        PrintWriter out = response.getWriter();
        response.setContentType("text/html;charset=UTF-8");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Detail on " + requestedURL + "</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("RequestedURL = " + requestedURL);
        out.println("<br>List on local network<br>");
        out.println(System.getProperty("user.dir") + File.separator + requestedURL);
        File dir = new File(System.getProperty("user.dir") + File.separator + requestedURL);
        File[] files = dir.listFiles();
        if (files != null) {
            out.println("<table border='0'>");
            out.println("<tr>");
            out.println("  <th>File name</th>");
            out.println("  <th>Size</th>");
            out.println("  <th>Modified Date</th>");
            out.println("</tr>");
            for (File file : files) {
                out.println("<tr>");
                out.println("  <td>" + file.getName() + "</td>");
                out.println("  <td>" + file.length() + "</td>");
                out.println("  <td>" + new java.util.Date(file.lastModified()) + "</td>");
                out.println("</tr>");
            }
            out.println("</table>");
        }
        buildFoot(out);
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private void buildFoot(PrintWriter out) {
        out.println("<center>");
        out.println("<hr width='50%'>");
        out.println("Autosync-Mirror Version " + VERSION + "<br>");
        out.println("<a href='http://autosync-mirror.sourceforge.net/'>Project website</a><br>");
        out.println("<a href='http://sourceforge.net/projects/autosync-mirror/'>Project Page</a><br>");
        out.println("<a href='http://www.hostip.info/'>Geolocalisation done with hostip.info</a><br>");
        out.println("</center>");
    }

    public void destroy() {
        MirrorManager.getInstance().stopStats();
    }
}
