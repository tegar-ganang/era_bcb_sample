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
public class ArcIMSMapService extends MapService {

    protected String mapDescription = "";

    protected String projection = "latitude/longitude";

    protected String datum = "WGS84";

    protected boolean coordsysSpecified = false;

    protected double oziAdjustNorthing = 0.0;

    protected double oziAdjustEasting = 0.0;

    protected String baseURL = null;

    protected String infoURL = "";

    protected String requestURL = null;

    protected String imageURL = null;

    protected String serviceName = null;

    protected String catURL = null;

    protected String servletURL = "/servlet/com.esri.esrimap.Esrimap";

    protected String refererURL = null;

    protected String serviceOrder = null;

    protected String preferredServices = null;

    protected int mapDelay = 5;

    protected String imageFormat = "PNG";

    boolean supportsCatalog = true;

    protected boolean generateSegmentsRandomly = true;

    protected double minXExtent = 0.0d;

    protected double minYExtent = 0.0d;

    protected double maxXExtent = 0.0d;

    protected double maxYExtent = 0.0d;

    protected double initialMinX = 0.0d;

    protected double initialMaxX = 0.0d;

    protected double initialMinY = 0.0d;

    protected double initialMaxY = 0.0d;

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
    public ArcIMSMapService(Properties props) {
        log = LogFactory.getLog(ArcIMSMapService.class);
        rootLayer = new Layer("ROOT", "ROOT", true);
        this.mapDescription = props.getProperty("description");
        baseURL = props.getProperty("baseURL");
        if (props.getProperty("servletURL") != null) servletURL = props.getProperty("servletURL");
        if (servletURL != null) {
            catURL = baseURL + servletURL + "?ServiceName=catalog";
        }
        if (props.getProperty("serviceName") != null) {
            serviceName = props.getProperty("serviceName");
            supportsCatalog = false;
        }
        if (props.getProperty("requestURL") != null) {
            requestURL = props.getProperty("requestURL");
            supportsCatalog = false;
        } else {
            if (!supportsCatalog) requestURL = baseURL + servletURL + "?ServiceName=" + serviceName;
        }
        if (props.getProperty("imageURL") != null) imageURL = props.getProperty("imageURL");
        if (props.getProperty("refererURL") != null) refererURL = props.getProperty("refererURL"); else refererURL = baseURL;
        if (props.getProperty("infoURL") != null) infoURL = props.getProperty("infoURL"); else infoURL = baseURL;
        if (supportsCatalog) {
            if (props.getProperty("serviceOrder") != null) serviceOrder = props.getProperty("serviceOrder");
            if (props.getProperty("preferredServices") != null) {
                preferredServices = props.getProperty("preferredServices");
                if (serviceOrder == null) serviceOrder = preferredServices;
            }
        }
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
        if (props.getProperty("projection") != null) {
            projection = props.getProperty("projection");
            coordsysSpecified = true;
        } else {
            if (props.getProperty("oziMapProjectionName") != null) projection = props.getProperty("oziMapProjectionName");
        }
        if (props.getProperty("datum") != null) {
            datum = props.getProperty("datum");
            coordsysSpecified = true;
        } else {
            if (props.getProperty("oziMapDatumName") != null) datum = props.getProperty("oziMapDatumName");
        }
        MRDatum datumObj = MRDatum.getInstance(datum);
        MRProjection projectionObj = MRProjection.getInstance(projection);
        String zone = null;
        if (props.getProperty("zone") != null) zone = props.getProperty("zone");
        if (zone != null) projectionObj.setZone(zone);
        String hemisphere = null;
        if (props.getProperty("hemisphere") != null) hemisphere = props.getProperty("hemisphere");
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

    public java.util.List<Layer> getLayers() {
        return rootLayer.getSubLayers();
    }

    /**
     * Extract the XML reply component from the ArcWeb response.
     *
     * @param response The response from the web server.
     * @return Returns only the XML reply component of the response.
     **/
    public String extractXML(String response) {
        String xml = null;
        if (response != null && response.indexOf("<?xml") >= 0) {
            xml = response.substring(response.indexOf("<?xml"), (response.indexOf("ARCXML>", response.indexOf("<?xml")) + 6) + 1);
        }
        return xml;
    }

    protected HttpMethodBase buildRequest(String url, String xml, boolean forRequestMap) {
        PostMethod post = new PostMethod(url);
        try {
            post.setRequestBody(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            log.error("Failed to send bytes", ex);
        }
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

    private HashMap parseServicesString(String services) {
        HashMap result = new HashMap();
        if (services != null) {
            StringTokenizer tok = new StringTokenizer(services, ",");
            while (tok.hasMoreTokens()) {
                String service = tok.nextToken();
                result.put(service, new Boolean(true));
            }
        }
        return result;
    }

    private void addOrderedServices(Vector services) {
        if (serviceOrder != null) {
            StringTokenizer tok = new StringTokenizer(serviceOrder, ",");
            while (tok.hasMoreTokens()) {
                String serviceName = tok.nextToken();
                boolean found = false;
                for (int loop = 0; loop < services.size() && !found; loop++) {
                    Layer layer = (Layer) services.get(loop);
                    if (layer.getID().equals(serviceName)) {
                        found = true;
                        rootLayer.addSubLayer((Layer) services.remove(loop));
                    }
                }
            }
        }
        for (int loop = 0; loop < services.size(); loop++) {
            Layer layer = (Layer) services.get(loop);
            rootLayer.addSubLayer(layer);
        }
    }

    /**
     * Read the catalog for an ArcIMS map server and find out what services it
     * offers.
     **/
    protected boolean readCatalog() {
        boolean success = true;
        Vector serviceLayers = new Vector();
        String cachedFileName = getShortName() + "-catalog.xml";
        CachedFile cachedFile = new CachedFile(cachedFileName);
        String reply = null;
        if (cachedFile.isStale()) {
            String servicesCommand = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<GETCLIENTSERVICES />";
            HttpMethodBase request = buildRequest(catURL, servicesCommand, false);
            String tempReply = sendRequest(request);
            log.info("Reply was:\r\n" + tempReply);
            reply = extractXML(tempReply);
            reply = fixEncoding(reply);
            cachedFile.setContent(reply);
        } else {
            reply = cachedFile.getContent();
        }
        try {
            HashMap enabled = this.parseServicesString(preferredServices);
            DocumentBuilder buidler = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = buidler.parse(new ByteArrayInputStream(reply.getBytes("UTF-8")));
            Element root = doc.getDocumentElement();
            Element response = (Element) (root.getElementsByTagName("RESPONSE").item(0));
            Element services = (Element) (response.getElementsByTagName("SERVICES").item(0));
            NodeList service = services.getElementsByTagName("SERVICE");
            for (int loop = 0; loop < service.getLength() && loop < 20; loop++) {
                Element thisServ = (Element) (service.item(loop));
                String name = thisServ.getAttribute("NAME");
                String access = thisServ.getAttribute("ACCESS");
                String type = thisServ.getAttribute("TYPE");
                String status = thisServ.getAttribute("STATUS");
                if (access.equalsIgnoreCase("public") && type.equalsIgnoreCase("imageserver") && status.equalsIgnoreCase("enabled")) {
                    Layer layer = null;
                    if (enabled.get(name) != null) layer = new Layer("Service " + name, name, true); else layer = new Layer("Service " + name, name, false);
                    serviceLayers.add(layer);
                    Element image = (Element) (thisServ.getElementsByTagName("IMAGE").item(0));
                    String imageType = image.getAttribute("TYPE");
                    if (imageType.toLowerCase().indexOf("png") >= 0) imageFormat = "PNG"; else if (imageType.toLowerCase().indexOf("jpg") >= 0) imageFormat = "JPG"; else if (imageType.toLowerCase().indexOf("gif") >= 0) imageFormat = "GIF";
                }
            }
        } catch (Exception e) {
            log.error("failed", e);
            success = false;
        }
        addOrderedServices(serviceLayers);
        return success;
    }

    private MRCoordinateSystem guessCoordinateSystem(String id, String wkt) {
        MRCoordinateSystem result = null;
        if (id != null) {
            MRDatum datumObj = MRDatum.getInstanceForArcIMSName(id);
            MRProjection projectionObj = MRProjection.getInstanceForArcIMSName(id);
            if (datumObj != null && projectionObj != null) {
                coordsysSpecified = true;
                result = new MRCoordinateSystem(datumObj, projectionObj);
            }
        }
        if (result == null && wkt != null) {
            result = new MRCoordinateSystem(wkt);
        }
        return result;
    }

    protected boolean extractLayers(Layer parentLayer) {
        boolean success = true;
        String reply = null;
        String cachedFileName = null;
        if (supportsCatalog) {
            cachedFileName = getShortName() + "_" + parentLayer.getID() + ".xml";
        } else {
            cachedFileName = getShortName() + "_layerinfo.xml";
        }
        CachedFile cachedFile = new CachedFile(cachedFileName);
        if (cachedFile.isStale()) {
            String url;
            String xmlCommand = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<ARCXML version=\"1.1\">\r\n" + "<REQUEST>\r\n" + "<GET_SERVICE_INFO renderer=\"false\" extensions=\"false\" fields=\"false\" />\r\n" + "</REQUEST>\r\n" + "</ARCXML>";
            if (!supportsCatalog) url = requestURL; else url = baseURL + servletURL + "?ServiceName=" + parentLayer.getID();
            HttpMethodBase request = buildRequest(url, xmlCommand, false);
            String tempReply = sendRequest(request);
            log.info("Reply was:\r\n" + tempReply);
            reply = extractXML(tempReply);
            cachedFile.setContent(reply);
        } else {
            reply = cachedFile.getContent();
        }
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
            Element coordsys = (Element) (serviceinfo.getElementsByTagName("FEATURECOORDSYS").item(0));
            if (!coordsysSpecified && (coordsys.hasAttribute("id") || coordsys.hasAttribute("string"))) {
                MRCoordinateSystem newcs = guessCoordinateSystem(coordsys.getAttribute("id"), coordsys.getAttribute("string"));
                if (newcs != null) {
                    coordsysSpecified = true;
                    setCs(newcs);
                }
            }
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
                for (int loop = 0; loop < respLayers.getLength(); loop++) {
                    Element layerInfo = (Element) (respLayers.item(loop));
                    Layer layer = new Layer(layerInfo.getAttribute("name"), layerInfo.getAttribute("id"), new Boolean(layerInfo.getAttribute("visible")).booleanValue());
                    parentLayer.addSubLayer(layer);
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

    protected boolean extractCapabilities() {
        boolean success = true;
        rootLayer = new Layer("ROOT", "ROOT", true);
        if (supportsCatalog) {
            readCatalog();
        } else {
            rootLayer.addSubLayer(new Layer("Default", serviceName, true));
        }
        for (int loop = 0; loop < rootLayer.getSubLayers().size() && success == true; loop++) {
            Layer layer = (Layer) rootLayer.getSubLayers().get(loop);
            success = extractLayers(layer);
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
        for (int srvLoop = 0; srvLoop < rootLayer.getSubLayers().size() && success; srvLoop++) {
            Layer parentLayer = (Layer) rootLayer.getSubLayers().get(srvLoop);
            if (parentLayer.isVisible()) {
                String url = null;
                if (supportsCatalog) {
                    url = baseURL + servletURL + "?ServiceName=" + parentLayer.getID();
                } else {
                    url = requestURL;
                }
                StringBuffer xmlCommand = new StringBuffer();
                double outputMinX = 0.0;
                double outputMinY = 0.0;
                double outputMaxX = 0.0;
                double outputMaxY = 0.0;
                String mapURL = null;
                xmlCommand.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><ARCXML version=\"1.1\">\r\n" + "<REQUEST>\r\n" + "<GET_IMAGE>\r\n" + "<PROPERTIES>\r\n" + "<ENVELOPE minx=\"" + minx + "\" " + "miny=\"" + miny + "\" " + "maxx=\"" + maxx + "\" " + "maxy=\"" + maxy + "\" />\r\n" + "<IMAGESIZE height=\"" + height + "\" width=\"" + width + "\" />\r\n" + "<LAYERLIST >\r\n");
                for (int loop = 0; loop < parentLayer.getSubLayers().size(); loop++) {
                    Layer layer = (Layer) parentLayer.getSubLayers().get(loop);
                    xmlCommand.append("<LAYERDEF id=\"" + layer.getID() + "\" visible=\"");
                    if (layer.isVisible()) xmlCommand.append("true"); else xmlCommand.append("false");
                    xmlCommand.append("\" />\r\n");
                }
                xmlCommand.append("</LAYERLIST>\r\n" + "<BACKGROUND color=\"255,255,255\" transcolor=\"255,255,255\" />\r\n" + "</PROPERTIES>\r\n" + "</GET_IMAGE>\r\n" + "</REQUEST>\r\n" + "</ARCXML>\r\n");
                HttpMethodBase request = buildRequest(url, xmlCommand.toString(), true);
                String reply = extractXML(sendRequest(request));
                if (reply != null && reply.length() > 0) reply = fixEncoding(reply); else success = false;
                if (success) {
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
                        success = downloadImage(mapURL, outputDirectory, filename + "_" + srvLoop);
                    } catch (Exception e) {
                        log.error("Problem requesting map.", e);
                        success = false;
                    }
                }
                if (log.isInfoEnabled()) {
                    log.info("Reply is:\r\n" + reply);
                    log.info("URL is " + mapURL);
                    log.info("Output envelope is " + outputMinX + "," + outputMinY + " " + outputMaxX + "," + outputMaxY);
                }
            }
        }
        return (success);
    }

    protected boolean downloadImage(String url, String outputDirectory, String filename) {
        boolean success = true;
        boolean found = false;
        int attempts = 0;
        if (imageURL != null) {
            url = imageURL + url.substring(url.indexOf("/", url.indexOf("//") + 2));
        }
        log.info("Requesting image URL " + url);
        while (success && !found && (attempts < 5)) {
            found = true;
            try {
                GetMethod get = new GetMethod(url);
                get.setRequestHeader("Referer", refererURL);
                get.setRequestHeader("Accept-Language", "en");
                get.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");
                get.setRequestHeader("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/x-shockwave-flash, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                get.setRequestHeader("Pragma", "no-cache");
                client.executeMethod(get);
                if ((get.getStatusCode() == 301) || (get.getStatusCode() == 302)) {
                    url = get.getResponseHeader("location").getValue();
                    found = false;
                } else {
                    InputStream in = get.getResponseBodyAsStream();
                    byte[] data = new byte[1024];
                    int length;
                    java.io.FileOutputStream out = new java.io.FileOutputStream(outputDirectory + File.separator + filename);
                    while ((length = in.read(data)) >= 0) out.write(data, 0, length);
                    out.flush();
                    in.close();
                    out.close();
                    get.releaseConnection();
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
            attempts++;
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
        if (!(getImageFormat().equals("PNG") || getImageFormat().equals("JPG") || getImageFormat().equals("GIF"))) {
            log.error("Error: unsupported image format " + getImageFormat());
            success = false;
        }
        int mapX = imageParams.getWidth();
        int mapY = imageParams.getHeight();
        if (getAddText() != null) {
            mapY += addTextHeight;
        }
        graphics.setColor(Color.WHITE);
        graphics.fillRect(segment.getXOffset(), segment.getYOffset(), segment.getWidth(), segment.getHeight());
        for (int srvLoop = 0; srvLoop < rootLayer.getSubLayers().size() && success; srvLoop++) {
            Layer layer = (Layer) rootLayer.getSubLayers().get(srvLoop);
            if (layer.isVisible()) {
                try {
                    RenderedImage image = null;
                    log.info("Reading segment " + segment.getFilename() + "_" + srvLoop);
                    if (getImageFormat().equals("PNG")) {
                        PNGDecodeParam decodeParam = new PNGDecodeParam();
                        decodeParam.setSuppressAlpha(false);
                        FileInputStream in = new FileInputStream(tempDirectory + File.separator + segment.getFilename() + "_" + srvLoop);
                        ImageDecoder decoder = new com.sun.media.jai.codecimpl.PNGImageDecoder(in, decodeParam);
                        image = (RenderedImage) decoder.decodeAsRenderedImage();
                    } else {
                        File in = new File(tempDirectory + File.separator + segment.getFilename() + "_" + srvLoop);
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
                        log.error("Unable to render map segment " + segment.getFilename() + "_" + srvLoop + "." + "Perhaps there is a problem on the remote site.");
                        success = false;
                    }
                } catch (Exception e) {
                    log.error("failed", e);
                    success = false;
                }
            }
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
                downloader = new ArcIMSMapService.MapDownloaderThread(main);
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
        boolean result = false;
        for (int loop = 0; loop < rootLayer.getSubLayers().size() && !result; loop++) {
            Layer service = rootLayer.getSubLayers().get(loop);
            if (service.isVisible()) {
                for (int loop2 = 0; loop2 < service.getSubLayers().size() && !result; loop2++) {
                    Layer layer = service.getSubLayers().get(loop2);
                    result = layer.isVisible();
                }
            }
        }
        return result;
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
                success = requestMap(segment.getMinX(), segment.getMinY(), segment.getMaxX(), segment.getMaxY(), segment.getWidth(), segment.getHeight(), outputDirectory, segment.getFilename());
                if (success) {
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
