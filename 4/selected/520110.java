package BA.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ExportHandler extends AbstractHandler {

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("Content-type", "application/force-download");
        response.setHeader("Content-disposition", "attachment");
        response.setHeader("filename", "export.txt");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        InputStream x = baseRequest.getInputStream();
        StringWriter writer = new StringWriter();
        IOUtils.copy(x, writer);
        String theString = writer.toString();
        System.out.println(theString);
        response.getWriter().println(request.getParameter("file").replace("*", "\n"));
    }
}
