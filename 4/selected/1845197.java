package net.sf.warpcore.cms.servlets;

import java.util.*;
import java.io.*;
import net.sf.warpcore.ejb.UniquePK;
import java.rmi.*;
import javax.ejb.*;
import javax.naming.*;
import javax.rmi.*;
import javax.servlet.*;
import javax.servlet.http.*;
import net.sf.warpcore.domperignon.common.*;
import net.sf.warpcore.domperignon.api.*;
import net.sf.wedgetarian.util.*;
import net.sf.warpcore.cms.value.*;
import net.sf.warpcore.cms.entity.property.PropertyDomain;
import net.sf.warpcore.cms.entity.content.ContentStore;
import net.sf.warpcore.cms.entity.content.ContentType;
import net.sf.warpcore.cms.webfrontend.git.*;

public class WarpInjectorServlet extends HttpServlet {

    private static final String BLACK = "#000000";

    private static final String RED = "#ff0000";

    private static final String GREEN = "#00ff00";

    private static final String YELLOW = "#ffff00";

    private static final String ORANGE = "#ff9900";

    ServletHelper helper;

    String deployRoot;

    public void init() throws ServletException {
        helper = new ServletHelper();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Git git = Git.getCurrent(req.getSession());
        PrintWriter out = helper.getPrintWriter(resp);
        MessageBundle messageBundle = new MessageBundle("net.sf.warpcore.cms/servlets/InjectorServletMessages");
        Locale locale = req.getLocale();
        helper.header(out, messageBundle, locale);
        if (git.getUser() == null) {
            helper.notLoggedIn(out, messageBundle, locale);
        } else {
            out.println("<form name=\"myform\" enctype=\"multipart/form-data\" method=\"post\" action=\"" + req.getRequestURI() + "?id=" + req.getParameter("id") + "\">");
            out.println(messageBundle.getMessage("Please select a file for upload", locale) + ":&nbsp;&nbsp");
            out.println("<input name=\"userfile\" type=\"file\"><br>");
            out.println("<input type=\"submit\" value=\"" + messageBundle.getMessage("Upload", locale) + "\">");
            out.println("<input type=\"reset\" value=\"" + messageBundle.getMessage("Reset", locale) + "\">");
            out.println("</form>");
        }
        helper.footer(out);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Git git = Git.getCurrent(req.getSession());
        GitComponentReader gitReader = git.getComponentReader("warpinjector");
        String id = req.getParameter("id");
        GitElement element = gitReader.getElement(id);
        String path = (String) element.getAttribute("targetdir");
        File folder = new File(path);
        PrintWriter out = helper.getPrintWriter(resp);
        MessageBundle messageBundle = new MessageBundle("net.sf.warpcore.cms/servlets/InjectorServletMessages");
        Locale locale = req.getLocale();
        helper.header(out, messageBundle, locale);
        if (git.getUser() == null) {
            helper.notLoggedIn(out, messageBundle, locale);
        } else {
            try {
                MultiPartRequest request = new MultiPartRequest(req);
                FileInfo info = request.getFileInfo("userfile");
                File file = info.getFile();
                out.println("tempfile found: " + file.getPath() + "<br>");
                String fileName = info.getFileName();
                File target = new File(folder, fileName);
                out.println("copying tempfile to: " + target.getPath() + "<br>");
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream fos = new FileOutputStream(target);
                byte buf[] = new byte[1024];
                int n;
                while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                fis.close();
                fos.close();
                out.println("copy successful - deleting old tempfile<br>");
                out.println("deletion result. " + file.delete() + "<p>");
                out.println(messageBundle.getMessage("Done. The file {0} has been uploaded", new String[] { "'" + fileName + "'" }, locale));
                out.println("<p><a href=\"" + req.getRequestURI() + "?id=" + req.getParameter("id") + "\">" + messageBundle.getMessage("Click here to import another file.", locale) + "</a>");
            } catch (Exception ex) {
                out.println(messageBundle.getMessage("An error occured: {0}", new String[] { ex.getMessage() }, locale));
            }
        }
        helper.footer(out);
    }

    private void print(PrintWriter out, String message) throws IOException {
        print(out, message, BLACK);
    }

    private void print(PrintWriter out, String message, boolean br) throws IOException {
        print(out, message, BLACK, br);
    }

    private void print(PrintWriter out, String message, String color) throws IOException {
        print(out, message, color, true);
    }

    private void print(PrintWriter out, String message, String color, boolean br) throws IOException {
        out.print("<font face=\"Helvetica,Arial\" size=\"-1\" color=\"" + color + "\"><b>" + message + "</b></font>");
        if (br) {
            out.println("<br>");
        }
        out.flush();
    }
}
