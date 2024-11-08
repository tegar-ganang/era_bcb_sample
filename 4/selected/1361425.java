package org.ezfusion.gui.www;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ezfusion.dataobject.Ticket;
import org.ezfusion.serviceint.EZServlet;
import org.ezfusion.serviceint.SrvMgr;
import org.osgi.service.http.HttpService;

public class FileServer extends HttpServlet implements EZServlet {

    private static final long serialVersionUID = -2648193014164018562L;

    private HttpService http;

    private Ticket serv1Reg;

    private SrvMgr srvMgr;

    private Ticket httpTck;

    private String alias;

    public FileServer(SrvMgr sm) {
        srvMgr = sm;
        httpTck = new Ticket();
        http = (HttpService) srvMgr.getService("httpservice", "", httpTck);
        registerServlet();
    }

    public void stopServer() {
        unregisterServlet();
        srvMgr.releaseService(httpTck);
        srvMgr.unregisterService(serv1Reg);
    }

    public String getAlias() {
        return alias;
    }

    private void registerServlet() {
        try {
            Hashtable<String, String> initparams = new Hashtable<String, String>();
            initparams.put("name", "eZFusion file server");
            alias = "ezfileserver";
            http.registerServlet("/" + alias, (Servlet) this, initparams, null);
            serv1Reg = srvMgr.registerService(HttpServlet.class.getName(), this, "ezfileserver");
        } catch (Exception e) {
            System.out.println("WARNING, An error occured during servlet registration : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void unregisterServlet() {
        try {
            http.unregister("/" + alias);
        } catch (Exception e) {
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        ServletOutputStream out = rsp.getOutputStream();
        String fileName = req.getParameter("file");
        String pathName = "/org/ezfusion/gui/www/" + fileName;
        InputStream resourceAsStream = getClass().getResourceAsStream(pathName);
        String contentType = getServletContext().getMimeType(pathName);
        if (contentType != null) rsp.setContentType(contentType); else if (pathName.endsWith(".js")) rsp.setContentType("text/javascript"); else if (pathName.endsWith(".css")) rsp.setContentType("text/css"); else if (pathName.endsWith(".html")) rsp.setContentType("text/html"); else rsp.setContentType("application/octet-stream");
        int lastSep = fileName.lastIndexOf("/");
        rsp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName.substring(lastSep + 1, fileName.length()) + "\"");
        try {
            byte[] buf = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = resourceAsStream.read(buf)) != -1) out.write(buf, 0, bytesRead);
        } catch (FileNotFoundException e) {
            out.println("File not found: " + fileName);
        } catch (IOException e) {
            out.println("Problem sending file " + pathName + ": " + e.getMessage());
        } finally {
            if (resourceAsStream != null) resourceAsStream.close();
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        rsp.getWriter().println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!-- an error has occured -->\n" + "<!-- post method should not be called -->");
    }
}
