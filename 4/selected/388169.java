package glowaxes.servlet;

import glowaxes.util.AutoRefreshMap;
import glowaxes.util.ChartRegistry;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * The Class ImageServlet.
 * 
 * @author <a href="mailto:eddie@tinyelements.com">Eddie Moojen</a>
 * @version $Id: ImageServlet.java 340 2011-02-10 22:17:01Z nejoom $
 */
public class ImageServlet extends HttpServlet {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7911703707279239730L;

    /** The logger. */
    private static Logger logger = Logger.getLogger(ImageServlet.class.getName());

    /**
     * Gets the url pattern.
     * 
     * @param context
     *            the context
     * 
     * @return the url pattern
     * 
     * @throws JDOMException
     *             the JDOM exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public static String getUrlPattern(ServletContext context) throws JDOMException, IOException {
        String webConfig = "/WEB-INF/web.xml";
        InputStream input = context.getResourceAsStream(webConfig);
        if (input == null) throw new RuntimeException("web.xml file cannot be found");
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(input);
        Element root = doc.getRootElement();
        List children = root.getChildren("servlet-mapping");
        Iterator i = children.iterator();
        while (i.hasNext()) {
            Element e = ((Element) i.next());
            logger.error(e.getName());
            String name = e.getChildText("servlet-name");
            if (name.equals("ImageServlet")) return e.getChildText("url-pattern").replace("*", "").replace("/", "");
        }
        return "image-out";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String file = request.getPathInfo().substring(1);
        if (file == null) throw new RuntimeException("id parameter is not defined (null)");
        String contentType = URLConnection.guessContentTypeFromName(file);
        if (contentType == null || !contentType.startsWith("image")) {
            return;
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            long ms = System.currentTimeMillis();
            while (ChartRegistry.SINGLETON.getChart(file) == null) {
                Thread.sleep(10);
            }
            logger.info("Image " + file + " registered in [" + (System.currentTimeMillis() - ms) + "]");
            long t0 = System.currentTimeMillis();
            while (ChartRegistry.SINGLETON.getChart(file).length == 0 && System.currentTimeMillis() - t0 < 60 * 1000) {
                Thread.sleep(100);
            }
            if (ChartRegistry.SINGLETON.getChart(file).length == 0) {
                throw new RuntimeException("Image did not get generated");
            }
            logger.info("Image " + file + " produced in " + (System.currentTimeMillis() - t0) + " ms");
            input = new ByteArrayInputStream(ChartRegistry.SINGLETON.getChart(file));
            int contentLength = input.available();
            response.reset();
            response.setContentLength(contentLength);
            response.setContentType(contentType);
            response.setHeader("Content-disposition", "inline; filename=\"" + file + "\"");
            output = new BufferedOutputStream(response.getOutputStream());
            while (contentLength-- > 0) {
                output.write(input.read());
            }
            AutoRefreshMap.getAutoRefreshMap("chartRegistry").printLog();
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
