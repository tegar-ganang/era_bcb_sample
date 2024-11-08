package ru.ecom.jbossinstaller.jetty;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: esinev
 * Date: Feb 22, 2008
 * Time: 2:15:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceServlet extends HttpServlet {

    protected void service(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        String uri = aRequest.getRequestURI();
        if ("/".equals(uri)) uri = "/JBossInstaller.html";
        StringTokenizer st = new StringTokenizer(uri, "/");
        String last = "no";
        while (st.hasMoreTokens()) {
            last = st.nextToken();
        }
        InputStream in = getClass().getResourceAsStream("/" + last);
        byte[] buf = new byte[1024];
        int readed;
        OutputStream out = aResponse.getOutputStream();
        while ((readed = in.read(buf, 0, 1024)) > 0) {
            out.write(buf, 0, readed);
        }
        in.close();
        out.flush();
    }
}
