package ebgeo.maprequest;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.color.*;
import java.awt.Graphics2D;
import javax.swing.*;
import com.sun.media.jai.codec.*;
import javax.imageio.*;
import ebgeo.maprequest.cs.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.cookie.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.logging.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * A MapService that talks to ArcIMS based web sites.
 */
public class ParameterisedMapService extends MapService {

    protected String mapDescription = "";

    protected String projection = "latitude/longitude";

    protected String datum = "WGS84";

    protected double oziAdjustNorthing = 0.0;

    protected double oziAdjustEasting = 0.0;

    protected String baseURL = null;

    protected String infoURL = "";

    protected String requestURL = null;

    protected String refererURL = null;

    protected int mapDelay = 5;

    protected String imageFormat = "PNG";

    protected boolean generateSegmentsRandomly = true;

    protected double minXExtent = 0.0d;

    protected double minYExtent = 0.0d;

    protected double maxXExtent = 0.0d;

    protected double maxYExtent = 0.0d;

    protected double initialMinX = 0.0d;

    protected double initialMaxX = 0.0d;

    protected double initialMinY = 0.0d;

    protected double initialMaxY = 0.0d;

    protected Vector layers;

    protected int addTextHeight = 20;

    protected int maxWidth = 0;

    protected int maxHeight = 0;

    protected int minWidth = 100;

    protected int minHeight = 100;

    boolean configured = false;

    private String country = null;

    private String state = null;

    private MapImageParameters imageParams = null;

    private String tempDirectory;

    private BufferedImage image;

    private BoundedRangeModel progress;

    private Component parentComponent;

    private boolean running = false;

    private Vector cookieCutterURLs = null;

    protected HttpClient client = null;

    private ArcIMSMapService.MapDownloaderThread downloader = null;

    private Log log;

    /** Creates a new instance of MapRequest */
    public ParameterisedMapService(Properties props) {
        log = LogFactory.getLog(ArcIMSMapService.class);
        layers = new Vector();
        this.mapDescription = props.getProperty("description");
        baseURL = props.getProperty("baseURL");
        requestURL = props.getProperty("requestURL");
        refererURL = props.getProperty("refererURL");
        if (props.getProperty("infoURL") != null) infoURL = props.getProperty("infoURL");
        if (props.getProperty("maxWidth") != null) maxWidth = Integer.parseInt(props.getProperty("maxWidth"));
        if (props.getProperty("maxHeight") != null) maxHeight = Integer.parseInt(props.getProperty("maxHeight"));
        if (props.getProperty("minXExtent") != null) minXExtent = Double.parseDouble(props.getProperty("minXExtent"));
        if (props.getProperty("minYExtent") != null) minYExtent = Double.parseDouble(props.getProperty("minYExtent"));
        if (props.getProperty("maxXExtent") != null) maxXExtent = Double.parseDouble(props.getProperty("maxXExtent"));
        if (props.getProperty("maxYExtent") != null) maxYExtent = Double.parseDouble(props.getProperty("maxYExtent"));
        if (props.getProperty("mapDelay") != null) mapDelay = Integer.parseInt(props.getProperty("mapDelay"));
        if (props.getProperty("imageFormat") != null) imageFormat = props.getProperty("imageFormat");
        if (props.getProperty("country") != null) country = props.getProperty("country"); else country = "";
        if (props.getProperty("state") != null) state = props.getProperty("state"); else state = "";
        if (props.getProperty("projection") != null) projection = props.getProperty("projection"); else {
            if (props.getProperty("oziMapProjectionName") != null) projection = props.getProperty("oziMapProjectionName");
        }
        if (props.getProperty("datum") != null) datum = props.getProperty("datum"); else {
            if (props.getProperty("oziMapDatumName") != null) datum = props.getProperty("oziMapDatumName");
        }
        MRDatum datumObj = MRDatum.getInstance(datum);
        MRProjection projectionObj = new MRProjection().getInstance(projection);
        String zone = null;
        if (props.getProperty("zone") != null) zone = props.getProperty("zone"); else {
            if (props.getProperty("oziZone") != null) zone = props.getProperty("oziZone");
        }
        if (zone != null) projectionObj.setZone(zone);
        String hemisphere = null;
        if (props.getProperty("hemisphere") != null) hemisphere = props.getProperty("hemisphere"); else {
            if (props.getProperty("oziHemisphere") != null) hemisphere = props.getProperty("oziHemisphere");
        }
        if (hemisphere != null) projectionObj.setHemisphere(hemisphere);
        setCs(new MRCoordinateSystem(datumObj, projectionObj));
        if (props.getProperty("addText") != null) setAddText(props.getProperty("addText"));
        client = MapRequest.getInstance().getHttpClient();
        if (props.getProperty("cookieName") != null) {
            String name = props.getProperty("cookieName");
            String value = props.getProperty("cookieValue");
            String url = baseURL.substring(baseURL.indexOf("://") + 3);
            int index = url.lastIndexOf("/");
            if (index > 0) url = url.substring(0, index);
            Cookie cookie = new Cookie(url, name, value, "/", 86400, false);
            client.getState().addCookie(cookie);
        }
        int loop = 0;
        boolean finished = false;
        while (!finished) {
            if (props.getProperty("cookieCutterURL." + loop) != null) {
                if (loop == 0) cookieCutterURLs = new Vector();
                cookieCutterURLs.add(props.getProperty("cookieCutterURL." + loop));
                loop++;
            } else {
                finished = true;
            }
        }
    }

    public String getDriverDescription() {
        return "ARCWeb map requester";
    }

    public Vector getLayers() {
        return layers;
    }

    /**
     * Extract the XML reply component from the ArcWeb response.
     *
     * @param response The response from the web server.
     * @return Returns only the XML reply component of the response.
     **/
    public String extractXML(String response) {
        String xml = null;
        if (response.indexOf("<?xml") >= 0) {
            xml = response.substring(response.indexOf("<?xml"), (response.indexOf("ARCXML>", response.indexOf("<?xml")) + 6) + 1);
        }
        return xml;
    }

    protected HttpMethodBase buildRequest(String xml) {
        PostMethod post = new PostMethod(requestURL);
        post.addParameter("ArcXMLRequest", xml);
        post.addParameter("JavaScriptFunction", "parent.MapFrame.processXML");
        post.addParameter("BgColor", "#000000");
        post.addParameter("FormCharset", "ISO-8859-1");
        post.addParameter("RedirectURL", "");
        post.addParameter("HeaderFile", "");
        post.addParameter("FooterFile", "");
        post.setRequestHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
        post.setRequestHeader("Referer", refererURL);
        post.setRequestHeader("Accept-Language", "en");
        post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");
        post.setRequestHeader("Pragma", "no-cache");
        return post;
    }

    private String fixEncoding(String in) {
        String reply = in;
        int index;
        if ((index = in.indexOf("encoding=\"UTF8\"")) >= 0) {
            reply = in.substring(0, index) + "encoding=\"UTF-8\"" + in.substring(index + 15, in.length());
            log.info("Changed UTF8 to UTF-8. Result is");
            log.info(reply);
        }
        return reply;
    }

    protected boolean extractCapabilities() {
        boolean success = true;
        String xmlCommand = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<ARCXML version=\"1.1\">\r\n" + "<REQUEST>\r\n" + "<GET_SERVICE_INFO renderer=\"false\" extensions=\"false\" fields=\"false\" />\r\n" + "</REQUEST>\r\n" + "</ARCXML>";
        HttpMethodBase request = buildRequest(xmlCommand);
        String tempReply = sendRequest(request);
        log.info("Reply was:\r\n" + tempReply);
        String reply = extractXML(tempReply);
        if (reply == null) {
            log.error("Failed to extract capabilities for " + this.getMapDescription());
            success = false;
            return false;
        }
        log.info("Reply is:\r\n" + reply);
        reply = fixEncoding(reply);
        try {
            DocumentBuilder buidler = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = buidler.parse(new ByteArrayInputStream(reply.getBytes("UTF-8")));
            Element root = doc.getDocumentElement();
            Element response = (Element) (root.getElementsByTagName("RESPONSE").item(0));
            Element serviceinfo = (Element) (response.getElementsByTagName("SERVICEINFO").item(0));
            Element properties = (Element) (serviceinfo.getElementsByTagName("PROPERTIES").item(0));
            NodeList envelopes = properties.getElementsByTagName("ENVELOPE");
            for (int loop = 0; loop < envelopes.getLength(); loop++) {
                Element envelope = (Element) (envelopes.item(loop));
                if (envelope.getAttribute("name").equalsIgnoreCase("initial_extent")) {
                    if (minXExtent == 0.0d) minXExtent = Double.parseDouble(envelope.getAttribute("minx"));
                    if (minYExtent == 0.0d) minYExtent = Double.parseDouble(envelope.getAttribute("miny"));
                    if (maxXExtent == 0.0d) maxXExtent = Double.parseDouble(envelope.getAttribute("maxx"));
                    if (maxYExtent == 0.0d) maxYExtent = Double.parseDouble(envelope.getAttribute("maxy"));
                    if (initialMinX == 0.0d) initialMinX = Double.parseDouble(envelope.getAttribute("minx"));
                    if (initialMinY == 0.0d) initialMinY = Double.parseDouble(envelope.getAttribute("miny"));
                    if (initialMaxX == 0.0d) initialMaxX = Double.parseDouble(envelope.getAttribute("maxx"));
                    if (initialMaxY == 0.0d) initialMaxY = Double.parseDouble(envelope.getAttribute("maxy"));
                }
            }
            NodeList respLayers = serviceinfo.getElementsByTagName("LAYERINFO");
            if (respLayers != null) {
                layers = new Vector();
                for (int loop = 0; loop < respLayers.getLength(); loop++) {
                    Element layerInfo = (Element) (respLayers.item(loop));
                    Layer layer = new Layer(layerInfo.getAttribute("name"), layerInfo.getAttribute("id"), new Boolean(layerInfo.getAttribute("visible")).booleanValue());
                    layers.add(layer);
                    Element fclass = (Element) (layerInfo.getElementsByTagName("FCLASS").item(0));
                    Element envelope = null;
                    if (fclass != null) envelope = (Element) (fclass.getElementsByTagName("ENVELOPE").item(0)); else envelope = (Element) (layerInfo.getElementsByTagName("ENVELOPE").item(0));
                    if (envelope != null) {
                        double layerMinX = Double.parseDouble(envelope.getAttribute("minx"));
                        double layerMinY = Double.parseDouble(envelope.getAttribute("miny"));
                        double layerMaxX = Double.parseDouble(envelope.getAttribute("maxx"));
                        double layerMaxY = Double.parseDouble(envelope.getAttribute("maxy"));
                        if (layerMinX < minXExtent) minXExtent = layerMinX;
                        if (layerMinY < minYExtent) minYExtent = layerMinY;
                        if (layerMaxX > maxXExtent) maxXExtent = layerMaxX;
                        if (layerMaxY > minYExtent) maxYExtent = layerMaxY;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract properties.", e);
            success = false;
        }
        return success;
    }

    /**
     * Get the maximum width that can be requested from this service.
     **/
    protected int getMaxWidth() {
        return maxWidth;
    }

    /**
     * Get the maximum height that can be requested from this service.
     **/
    protected int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Get the number of seconds to sleep between map image requests.
     **/
    protected int getMapDelay() {
        return mapDelay;
    }

    protected String getImageFormat() {
        return imageFormat;
    }

    /**
     * Check if map segments (where a map needs multiple segments) should be
     * generated in a ramdom order.
     *
     * @return Returns true if the segments should be generated in a random
     *   order, or false if they can be generated sequentially.
     **/
    protected boolean isGenerateSegmentsRandomly() {
        return generateSegmentsRandomly;
    }

    protected String sendRequest(HttpMethodBase request) {
        String reply = null;
        try {
            client.executeMethod(request);
            byte[] buffer;
            buffer = request.getResponseBody();
            reply = new String(buffer, "UTF-8");
            request.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reply;
    }

    protected Vector getDefaultLayers() {
        return new Vector();
    }

    protected boolean requestMap(double minx, double miny, double maxx, double maxy, int width, int height, String outputDirectory, String filename) {
        boolean success = true;
        StringBuffer xmlCommand = new StringBuffer();
        double outputMinX = 0.0;
        double outputMinY = 0.0;
        double outputMaxX = 0.0;
        double outputMaxY = 0.0;
        String mapURL = null;
        xmlCommand.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><ARCXML version=\"1.1\">\r\n" + "<REQUEST>\r\n" + "<GET_IMAGE>\r\n" + "<PROPERTIES>\r\n" + "<ENVELOPE minx=\"" + minx + "\" " + "miny=\"" + miny + "\" " + "maxx=\"" + maxx + "\" " + "maxy=\"" + maxy + "\" />\r\n" + "<IMAGESIZE height=\"" + height + "\" width=\"" + width + "\" />\r\n" + "<LAYERLIST >\r\n");
        for (int loop = 0; loop < layers.size(); loop++) {
            Layer layer = (Layer) layers.get(loop);
            xmlCommand.append("<LAYERDEF id=\"" + layer.getID() + "\" visible=\"");
            if (layer.isVisible()) xmlCommand.append("true"); else xmlCommand.append("false");
            xmlCommand.append("\" />\r\n");
        }
        xmlCommand.append("</LAYERLIST>\r\n" + "<BACKGROUND color=\"255,255,255\" transcolor=\"255,255,255\" />\r\n" + "</PROPERTIES>\r\n" + "</GET_IMAGE>\r\n" + "</REQUEST>\r\n" + "</ARCXML>\r\n");
        HttpMethodBase request = buildRequest(xmlCommand.toString());
        String reply = extractXML(sendRequest(request));
        reply = fixEncoding(reply);
        try {
            DocumentBuilder buidler = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = buidler.parse(new ByteArrayInputStream(reply.getBytes("ISO-8859-1")));
            Element root = doc.getDocumentElement();
            Element response = (Element) (root.getElementsByTagName("RESPONSE").item(0));
            Element image = (Element) (response.getElementsByTagName("IMAGE").item(0));
            Element envelope = (Element) (image.getElementsByTagName("ENVELOPE").item(0));
            Element outputImage = (Element) (image.getElementsByTagName("OUTPUT").item(0));
            outputMinX = Double.parseDouble(envelope.getAttribute("minx"));
            outputMinY = Double.parseDouble(envelope.getAttribute("miny"));
            outputMaxX = Double.parseDouble(envelope.getAttribute("maxx"));
            outputMaxY = Double.parseDouble(envelope.getAttribute("maxy"));
            mapURL = outputImage.getAttribute("url");
            success = downloadImage(mapURL, outputDirectory, filename);
        } catch (Exception e) {
            log.error("Problem requesting map.", e);
            success = false;
        }
        if (log.isInfoEnabled()) {
            log.info("Reply is:\r\n" + reply);
            log.info("URL is " + mapURL);
            log.info("Output envelope is " + outputMinX + "," + outputMinY + " " + outputMaxX + "," + outputMaxY);
        }
        return (success);
    }

    protected boolean downloadImage(String url, String outputDirectory, String filename) {
        boolean success = true;
        log.info("Requesting image URL " + url);
        try {
            GetMethod get = new GetMethod(url);
            get.setRequestHeader("Referer", refererURL);
            get.setRequestHeader("Accept-Language", "en");
            get.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");
            get.setRequestHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            get.setRequestHeader("Pragma", "no-cache");
            client.executeMethod(get);
            InputStream in = get.getResponseBodyAsStream();
            byte[] data = new byte[1024];
            int length;
            java.io.FileOutputStream out = new java.io.FileOutputStream(outputDirectory + File.separator + filename);
            while ((length = in.read(data)) >= 0) out.write(data, 0, length);
            out.flush();
            in.close();
            out.close();
            get.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return (success);
    }

    protected boolean configureCookies() {
        boolean success = true;
        for (int loop = 0; loop < cookieCutterURLs.size(); loop++) {
            String url = (String) cookieCutterURLs.get(loop);
            log.info("Requesting cookie cutter URL " + url);
            try {
                GetMethod get = new GetMethod(url);
                get.setRequestHeader("Referer", refererURL);
                get.setRequestHeader("Accept-Language", "en");
                get.setRequestHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                get.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");
                get.setRequestHeader("Pragma", "no-cache");
                sendRequest(get);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }
        return (success);
    }

    protected Vector createSegments(MapImageParameters params, Graphics2D graphics) {
        int counter = 0;
        Vector segments = new Vector();
        int screensx = (int) (params.getWidth() / getMaxWidth());
        int screensy = (int) (params.getHeight() / getMaxHeight());
        double pixelx = (params.getMaxX() - params.getMinX()) / (double) params.getWidth();
        double pixely = (params.getMaxY() - params.getMinY()) / (double) params.getHeight();
        for (int loopx = 0; loopx < screensx; loopx++) {
            for (int loopy = 0; loopy < screensy; loopy++) {
                int xoffset = loopx * getMaxWidth();
                int yoffset = (screensy - loopy - 1) * getMaxHeight();
                MapSegment segment = new MapSegment(loopx, loopy, params.getMinX() + ((double) (loopx * getMaxWidth()) * pixelx), params.getMinY() + ((double) (loopy * getMaxHeight()) * pixely), params.getMinX() + ((double) ((loopx + 1) * getMaxWidth()) * pixelx), params.getMinY() + ((double) ((loopy + 1) * getMaxHeight()) * pixely), getMaxWidth(), getMaxHeight(), xoffset, yoffset, "" + counter + "." + getImageFormat().toLowerCase());
                segments.add(segment);
                graphics.setColor(Color.RED);
                graphics.drawRect(xoffset, yoffset, getMaxWidth() - 1, getMaxHeight() - 1);
                if (parentComponent != null) parentComponent.repaint();
                counter++;
            }
        }
        return segments;
    }

    protected boolean deleteSegments(String outputDirectory, Vector segments) {
        boolean success = false;
        for (int loop = 0; loop < segments.size(); loop++) {
            MapSegment segment = (MapSegment) segments.get(loop);
            File file = new java.io.File(outputDirectory, segment.getFilename());
            try {
                log.info("Deleting " + segment.getFilename() + " in directory " + outputDirectory);
                file.delete();
            } catch (Exception e) {
                log.error("Failed to delete " + segment.getFilename(), e);
                success = false;
            }
        }
        return success;
    }

    protected boolean renderAddText(Graphics2D graphics) {
        boolean success = true;
        int width = getImageWidth();
        int height = getImageHeight();
        if (getAddText() != null) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, height - addTextHeight, width, addTextHeight);
            graphics.setColor(Color.BLACK);
            Font font = new Font("SansSerif", Font.PLAIN, 10);
            graphics.setFont(font);
            graphics.drawString(getAddText(), 5, height - 3);
            if (parentComponent != null) parentComponent.repaint();
        }
        return success;
    }

    protected boolean renderSegment(MapSegment segment, Graphics2D graphics, String tempDirectory) {
        boolean success = true;
        if (getImageFormat().equals("PNG") || getImageFormat().equals("JPG") || getImageFormat().equals("GIF")) {
            int mapX = imageParams.getWidth();
            int mapY = imageParams.getHeight();
            if (getAddText() != null) {
                mapY += addTextHeight;
            }
            try {
                RenderedImage image = null;
                log.info("Reading segment " + segment.getFilename());
                if (getImageFormat().equals("PNG")) {
                    PNGDecodeParam decodeParam = new PNGDecodeParam();
                    decodeParam.setSuppressAlpha(true);
                    FileInputStream in = new FileInputStream(tempDirectory + File.separator + segment.getFilename());
                    ImageDecoder decoder = new com.sun.media.jai.codecimpl.PNGImageDecoder(in, decodeParam);
                    image = (RenderedImage) decoder.decodeAsRenderedImage();
                } else {
                    File in = new File(tempDirectory + File.separator + segment.getFilename());
                    image = ImageIO.read(in);
                }
                if (image != null) {
                    AffineTransform transform = new AffineTransform(1f, 0f, 0f, 1f, segment.getXOffset(), segment.getYOffset());
                    log.info("Pasting segment to " + segment.getXOffset() + "," + segment.getYOffset());
                    graphics.drawRenderedImage(image, transform);
                    if (parentComponent != null) {
                        parentComponent.repaint();
                    }
                } else {
                    log.error("Unable to render map segment " + segment.getFilename() + ".\r\n" + "Perhaps there is a problem on the remote site.");
                    success = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        } else {
            log.error("Error: unsupported image format " + getImageFormat());
            success = false;
        }
        if (!success) segment.setComplete(false);
        return success;
    }

    /**
     * Build up a map by requesting an appropriate number of smaller maps
     * appropriate for this service.
     **/
    public void buildUpMap(BufferedImage image, String tempDirectory, Component parentComponent, BoundedRangeModel progress, MainWindow main) {
        log.info("Build up map...");
        synchronized (this) {
            if (running) {
                log.error("Error: Currently running.");
            } else {
                running = true;
                this.image = image;
                this.tempDirectory = tempDirectory;
                this.progress = progress;
                this.parentComponent = parentComponent;
            }
        }
    }

    public void stopBuildUpMap() {
        if (running) {
            synchronized (this) {
                if (running) {
                    downloader.stopProcessing();
                }
            }
        }
    }

    public String getMapDescription() {
        return mapDescription;
    }

    public boolean configure() {
        boolean success = true;
        try {
            if (cookieCutterURLs != null) configureCookies();
            if (extractCapabilities()) {
                configured = true;
            } else {
                success = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setLayers(Vector layers) {
        this.layers = layers;
    }

    public double getInitialMinY() {
        return initialMinY;
    }

    public double getInitialMaxY() {
        return initialMaxY;
    }

    public double getInitialMinX() {
        return initialMinX;
    }

    public double getInitialMaxX() {
        return initialMaxX;
    }

    public double getMaxXExtent() {
        return maxXExtent;
    }

    public double getMaxYExtent() {
        return maxYExtent;
    }

    public double getMinXExtent() {
        return minXExtent;
    }

    public double getMinYExtent() {
        return minYExtent;
    }

    public MapImageParameters validateMapImageParameters(MapImageParameters params) {
        double minx = params.getMinX();
        double miny = params.getMinY();
        double maxx = params.getMaxX();
        double maxy = params.getMaxY();
        int width = params.getWidth();
        int height = params.getHeight();
        if (width < minWidth) width = minWidth;
        if (height < minHeight) height = minHeight;
        if (minx < minXExtent) minx = minXExtent;
        if (miny < minYExtent) miny = minYExtent;
        if (maxx > maxXExtent) maxx = maxXExtent;
        if (maxy > maxYExtent) maxy = maxYExtent;
        if (maxx < minXExtent) maxx = minXExtent;
        if (maxy < minYExtent) maxy = minYExtent;
        if (minx > maxXExtent) minx = maxXExtent;
        if (miny > maxYExtent) miny = maxYExtent;
        double midx = (minx + maxx) / 2.0;
        double midy = (miny + maxy) / 2.0;
        int screensx = (int) (width / getMaxWidth());
        if ((getMaxWidth() * screensx) < width) screensx++;
        int screensy = (int) (height / getMaxHeight());
        if ((getMaxHeight() * screensy) < height) screensy++;
        width = screensx * getMaxWidth();
        height = screensy * getMaxHeight();
        double pixelx = (maxx - minx) / (double) width;
        double pixely = (maxy - miny) / (double) height;
        pixely = pixelx;
        minx = midx - (pixelx * ((double) width / 2.0));
        maxx = midx + (pixelx * ((double) width / 2.0));
        miny = midy - (pixelx * ((double) height / 2.0));
        maxy = midy + (pixelx * ((double) height / 2.0));
        return new MapImageParameters(minx, miny, maxx, maxy, width, height);
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public int getImageHeight() {
        if (addText != null) return imageParams.getHeight() + addTextHeight; else return imageParams.getHeight();
    }

    public int getImageWidth() {
        return imageParams.getWidth();
    }

    public void setMapImageParameters(MapImageParameters imageParams) {
        this.imageParams = validateMapImageParameters(imageParams);
    }

    public MapImageParameters getMapImageParameters() {
        return imageParams;
    }

    public String getMapInfoURL() {
        return infoURL;
    }

    public boolean isAnyLayersSelected() {
        return false;
    }

    public class MapDownloaderThread extends Thread {

        MainWindow mainWindow = null;

        boolean stopped = false;

        public MapDownloaderThread(MainWindow mainWindow) {
            this.mainWindow = mainWindow;
            setDaemon(true);
            start();
        }

        public void stopProcessing() {
            stopped = true;
            interrupt();
        }

        protected boolean drawSegments(Vector segments, Graphics2D graphics, String outputDirectory) {
            boolean success = true;
            Vector incompleteSegments = (Vector) segments.clone();
            while ((incompleteSegments.size() > 0) && success) {
                int index;
                log.info(incompleteSegments.size() + " segments remaining");
                if (isGenerateSegmentsRandomly()) index = (int) (Math.random() * incompleteSegments.size()); else index = 0;
                MapSegment segment = (MapSegment) incompleteSegments.get(index);
                if (requestMap(segment.getMinX(), segment.getMinY(), segment.getMaxX(), segment.getMaxY(), segment.getWidth(), segment.getHeight(), outputDirectory, segment.getFilename())) {
                    if (renderSegment(segment, graphics, outputDirectory)) {
                        segment.setComplete(true);
                        incompleteSegments.remove(index);
                    } else {
                        segment.addError("Problem rendering segment");
                        if (segment.getErrorCount() > 1) {
                            log.error("This segment has failed too many " + "times. Aborting.");
                            success = false;
                        }
                    }
                } else {
                    segment.addError("Failed to request map.");
                    if (segment.getErrorCount() > 1) {
                        log.error("This segment has failed too many " + "times. Aborting.");
                        success = false;
                    }
                }
                if (incompleteSegments.size() > 0) {
                    try {
                        log.info("Sleeping for " + getMapDelay() + " seconds.");
                        Thread.sleep(getMapDelay() * 1000);
                    } catch (InterruptedException e) {
                        success = false;
                    }
                }
                if (segment.isComplete() && (progress != null)) progress.setValue(progress.getValue() + 1);
                if (isInterrupted()) {
                    success = false;
                }
            }
            return success;
        }

        public void run() {
            String error = "";
            boolean success = true;
            double minx = imageParams.getMinX();
            double miny = imageParams.getMinY();
            double maxx = imageParams.getMaxX();
            double maxy = imageParams.getMaxY();
            int width = imageParams.getWidth();
            int height = imageParams.getHeight();
            int screensx = (int) (imageParams.getWidth() / getMaxWidth());
            int screensy = (int) (imageParams.getHeight() / getMaxHeight());
            Graphics2D graphics = image.createGraphics();
            Vector segments = createSegments(imageParams, graphics);
            progress.setMaximum(segments.size() + 3);
            progress.setValue(1);
            if (drawSegments(segments, graphics, tempDirectory)) {
                if (renderAddText(graphics)) {
                    progress.setValue(progress.getValue() + 1);
                    deleteSegments(tempDirectory, segments);
                    progress.setValue(progress.getValue() + 1);
                } else {
                    error = "Failed to add text to map.";
                    success = false;
                }
            } else {
                if (!stopped) {
                    error = "Failed to request map segments.";
                    success = false;
                }
            }
            image = null;
            progress = null;
            parentComponent = null;
            tempDirectory = null;
            running = false;
            if (success) mainWindow.mapDone(); else mainWindow.mapFailed(error);
        }
    }
}
