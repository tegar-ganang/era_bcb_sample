package gov.lanl.ockham.service;

import gov.lanl.ockham.iesrdata.IESRCollection;
import gov.lanl.ockham.iesrdata.IESRFormatException;
import gov.lanl.ockham.iesrdata.IESRService;
import gov.lanl.ockham.iesrdata.XMLUtil;
import gov.lanl.registryclient.parser.SerializationException;
import gov.lanl.util.properties.PropertiesUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation for accepting put/post request
 */
public class RegistryRecordHandler extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final int BUFFER_SIZE = 16 * 1024;

    public static final String DBCONF = "properties";

    private ServiceRegistryDB registry;

    /**
	 * init is called one time when the Servlet is loaded. This is the place
	 * where one-time initialization is done. Specifically, we load the
	 * implementation class name from web.xml
	 * 
	 * @param config
	 *            servlet configuration information
	 * @exception ServletException
	 *                there was a problem with initialization
	 */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            String propFile = config.getServletContext().getInitParameter(DBCONF);
            this.registry = new ServiceRegistryDB(PropertiesUtil.loadConfigByPath(propFile));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage());
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        BufferedReader reader = request.getReader();
        StringWriter writer = new StringWriter();
        char[] buffer = new char[BUFFER_SIZE];
        int len = 0;
        while ((len = reader.read(buffer)) != -1) writer.write(buffer, 0, len);
        String result = writer.toString();
        try {
            String type = XMLUtil.getType(new ByteArrayInputStream(result.getBytes()));
            if ("Collection".equals(type)) {
                IESRCollection coll = new IESRCollection();
                coll.read(new ByteArrayInputStream(result.getBytes()));
                registry.putRecord(coll.getIdentifier(), result, "Collection");
            } else if ("Service".equals(type)) {
                IESRService service = new IESRService();
                service.read(new ByteArrayInputStream(result.getBytes()));
                registry.putRecord(service.getIdentifier(), result, "Service");
            } else throw new ServletException("not a recoganized format");
        } catch (SerializationException ex) {
            throw new ServletException(ex);
        } catch (ServiceException ex) {
            throw new ServletException(ex);
        } catch (IESRFormatException ex) {
            throw new ServletException(ex);
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getPathInfo() == null) {
            response.setContentType("text/plain");
            PrintWriter writer = response.getWriter();
            writer.println("not a valid request");
            writer.println("request should take one of following format:");
            writer.println("(1) HTTP GET baseurl/${identifier}");
            writer.println("(2) HTTP POST baseurl ${content}");
            writer.println("(3) HTTP Delete baseurl/${identifier}");
            writer.close();
            return;
        }
        String identifier = request.getPathInfo().substring(1);
        PrintWriter writer = response.getWriter();
        try {
            String result = registry.getRecord(identifier);
            response.setContentType("text/xml");
            writer.print(result);
            writer.close();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IdDoesNotExistException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException ex) {
            throw new ServletException(ex);
        }
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String identifier = request.getPathInfo().substring(1);
        try {
            registry.deleteRecord(identifier);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (ServiceException ex) {
            throw new ServletException(ex);
        }
    }
}
