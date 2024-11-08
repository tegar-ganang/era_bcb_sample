package glowaxes.servlet;

import glowaxes.glyphs.Rasterizer;
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
 * The Class RasterizeServlet. See rasterizer.jsp for example uses.
 * 
 * @author <a href="mailto:eddie@tinyelements.com">Eddie Moojen</a>
 * @version $Id: RasterizeServlet.java 184 2009-07-16 11:28:22Z nejoom $
 */
public class RasterizeServlet extends HttpServlet {

    /** The logger. */
    private static Logger logger = Logger.getLogger(RasterizeServlet.class.getName());

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

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
        logger.error("reasterizeServlet started");
        String url = request.getParameter("url");
        if (url == null) throw new RuntimeException("url parameter is not defined (null)"); else if (url.indexOf("svg") == -1) throw new RuntimeException("url parameter not pointing to svg file");
        logger.error("reasterizeServlet init");
        Rasterizer myRasterizer = new Rasterizer();
        myRasterizer.setSource(url);
        myRasterizer.setResult(request, response);
        String file = myRasterizer.getFileOut();
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
            while (ChartRegistry.SINGLETON.getChart(file).length == 0 && System.currentTimeMillis() - t0 < 10 * 1000) {
                Thread.sleep(100);
            }
            if (ChartRegistry.SINGLETON.getChart(file).length == 0) {
                throw new RuntimeException("Image did not get generated");
            }
            logger.info("Image " + file + " produced in [" + (System.currentTimeMillis() - t0) + "]");
            input = new ByteArrayInputStream(ChartRegistry.SINGLETON.getChart(file));
            int contentLength = input.available();
            response.reset();
            response.setContentLength(contentLength);
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
