package org.eugenes.net;

import java.io.*;
import java.util.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import org.apache.axis.MessageContext;
import org.apache.axis.session.Session;
import org.apache.axis.transport.http.AxisHttpSession;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.utils.Messages;

public class ArgosAxisServlet extends AxisServlet {

    protected void reportServiceInfo(HttpServletResponse response, PrintWriter writer, SOAPService service, String serviceName) {
        response.setContentType("text/html");
        ServletContext sctx = getServletConfig().getServletContext();
        if (sctx != null) try {
            String index = sctx.getInitParameter("index." + serviceName);
            if (index == null) index = "index.html";
            index = sctx.getRealPath(index);
            File file = new File(index);
            if (file.exists()) {
                String s;
                DataInputStream ds = new DataInputStream(new FileInputStream(file));
                while ((s = ds.readLine()) != null) writer.println(s);
                ds.close();
            }
        } catch (Exception e) {
        }
        writer.println("<h1>" + service.getName() + "</h1>");
        writer.println("<p>" + Messages.getMessage("axisService00") + "</p>");
        writer.println("<i>" + Messages.getMessage("perhaps00") + "</i>");
        writer.println("<br> service name: " + serviceName);
    }
}
