package ebgeo.maprequest;

import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.PNGDecodeParam;
import ebgeo.maprequest.cs.MRCoordinateSystem;
import ebgeo.maprequest.cs.MRDatum;
import ebgeo.maprequest.cs.MRProjection;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.swing.BoundedRangeModel;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A map service to extract maps from Open Street Map.
 *
 * @see http://www.openstreetmap.org/
 */
public class OSMMapService extends MapService {

    private boolean configured = false;

    private int maxZoom = 15;

    private static final int tileWidth = 256;

    private static final int tileHeight = 256;

    private HashMap layerURLs = new HashMap();

    private BufferedImage image;

    private BoundedRangeModel progress;

    private Component parentComponent;

    private String tempDirectory;

    protected int addTextHeight = 20;

    private String addText = "";

    private boolean running = false;

    private Log log;

    private String mapDescription;

    private String baseURL;

    private MapImageParameters imageParams;

    protected HttpClient client = null;

    private OSMMapService.MapDownloaderThread downloader = null;

    /** Creates a new instance of OMSMapService */
    public OSMMapService(Properties props) {
        log = LogFactory.getLog(ArcIMSMapService.class);
        rootLayer = new Layer("ROOT", "ROOT", true);
        this.mapDescription = props.getProperty("description");
        baseURL = props.getProperty("baseURL");
        int layernum = 0;
        boolean finished = false;
        while (!finished) {
            String layerName = props.getProperty("layer.name." + layernum, "");
            if (!layerName.equals("")) {
                String layerURL = props.getProperty("layer.tileurl." + layernum, "");
                Layer layer = new Layer(layerName, layerURL, true);
                if (layernum != 0) layer.setVisible(false);
                rootLayer.addSubLayer(layer);
                layernum++;
            } else {
                finished = true;
            }
        }
        MRDatum datumObj = MRDatum.getInstance("WGS84");
        MRProjection projectionObj = MRProjection.getInstance("mercator");
        setCs(new MRCoordinateSystem(datumObj, projectionObj));
        client = MapRequest.getInstance().getHttpClient();
        if (props.getProperty("addText") != null) setAddText(props.getProperty("addText"));
    }

    public String getDriverDescription() {
        return "Open Street Map";
    }

    public String getMapDescription() {
        return "Open Street Map";
    }

    public String getMapInfoURL() {
        return "http://www.openstreetmap.org/";
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean configure() {
        configured = true;
        return true;
    }

    public List<Layer> getLayers() {
        return rootLayer.getSubLayers();
    }

    public double getMinXExtent() {
        return -180.0;
    }

    public double getMinYExtent() {
        return -85.0511;
    }

    public double getMaxXExtent() {
        return 180.0;
    }

    public double getMaxYExtent() {
        return 85.0511;
    }

    public double getInitialMinX() {
        return -180.0;
    }

    public double getInitialMaxX() {
        return 180.0;
    }

    public double getInitialMinY() {
        return -85.0511;
    }

    public double getInitialMaxY() {
        return 85.0511;
    }

    private double calculateTileSizeX(int zoom) {
        double result = 360.0;
        for (int loop = 0; loop < zoom; loop++) result = result / 2.0;
        return result;
    }

    private double calculateTileSizeY(int zoom) {
        double result = 85.0511 * 2;
        for (int loop = 0; loop < zoom; loop++) result = result / 2.0;
        return result;
    }

    private int calculateTileX(double x, int zoom) {
        double tileSize = calculateTileSizeX(zoom);
        return (int) ((x + 180.0) / tileSize);
    }

    private int calculateTileY(double y, int zoom) {
        double tileSize = calculateTileSizeY(zoom);
        return (int) ((85.0511 - y) / tileSize);
    }

    private int calculateZoom(double tileWidth) {
        int result = 0;
        boolean done = false;
        double currentSize = 360.0;
        while (!done) {
            if (tileWidth >= currentSize) {
                done = true;
                if (Math.abs(tileWidth - currentSize) > 0.000001) {
                    result--;
                    if (result < 0) result = 0;
                }
            } else {
                result++;
                currentSize = currentSize / 2.0;
                if (result > maxZoom) {
                    result = maxZoom;
                    done = true;
                }
            }
        }
        return result;
    }

    public MapImageParameters validateMapImageParameters(MapImageParameters params) {
        double minXExtent = getMinXExtent();
        double minYExtent = getMinYExtent();
        double maxXExtent = getMaxXExtent();
        double maxYExtent = getMaxYExtent();
        double minx = params.getMinX();
        double miny = params.getMinY();
        double maxx = params.getMaxX();
        double maxy = params.getMaxY();
        int width = params.getWidth();
        int height = params.getHeight();
        if (width < tileWidth * 2) width = tileWidth * 2;
        if (height < tileHeight * 2) height = tileHeight * 2;
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
        int screensx = (int) (width / tileWidth);
        if ((tileWidth * screensx) < width) screensx++;
        int screensy = (int) (height / tileHeight);
        if ((tileHeight * screensy) < height) screensy++;
        width = screensx * tileWidth;
        height = screensy * tileHeight;
        double tilex = (maxx - minx) / (double) screensx;
        double tiley = (maxy - miny) / (double) screensy;
        int zoom = calculateZoom(tilex);
        tilex = calculateTileSizeX(zoom);
        tiley = calculateTileSizeY(zoom);
        minx = midx - (screensx * tilex / 2.0);
        maxx = midx + (screensx * tilex / 2.0);
        miny = midy - (screensy * tiley / 2.0);
        maxy = midy + (screensy * tiley / 2.0);
        int multiplier = (int) Math.floor(minx / tilex);
        minx = tilex * multiplier;
        multiplier = (int) Math.floor(maxx / tilex);
        maxx = tilex * multiplier;
        multiplier = (int) Math.floor(miny / tiley);
        miny = tiley * multiplier;
        multiplier = (int) Math.floor(maxy / tiley);
        maxy = tiley * multiplier;
        return new MapImageParameters(minx, miny, maxx, maxy, width, height);
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
                downloader = new OSMMapService.MapDownloaderThread(main);
            }
        }
    }

    public void stopBuildUpMap() {
    }

    public String getCountry() {
        return "";
    }

    public String getState() {
        return "";
    }

    public int getImageHeight() {
        if (addText != null) return imageParams.getHeight() + addTextHeight; else return imageParams.getHeight();
    }

    public int getImageWidth() {
        return imageParams.getWidth();
    }

    public void setMapImageParameters(MapImageParameters imageParams) {
        this.imageParams = this.validateMapImageParameters(imageParams);
    }

    public MapImageParameters getMapImageParameters() {
        return imageParams;
    }

    public boolean isAnyLayersSelected() {
        boolean result = false;
        for (int loop = 0; loop < rootLayer.getSubLayers().size() && !result; loop++) {
            Layer layer = rootLayer.getSubLayers().get(loop);
            result = layer.isVisible();
        }
        return result;
    }

    protected List<MapSegment> createSegments(MapImageParameters params, Graphics2D graphics) {
        double minx = params.getMinX();
        double miny = params.getMinY();
        double maxx = params.getMaxX();
        double maxy = params.getMaxY();
        int width = params.getWidth();
        int height = params.getHeight();
        ArrayList<MapSegment> segments = new ArrayList<MapSegment>();
        int screensx = (int) (width / tileWidth);
        int screensy = (int) (height / tileHeight);
        double tilex = (maxx - minx) / (double) screensx;
        double tiley = (maxy - miny) / (double) screensy;
        double pixelx = tilex / tileWidth;
        double pixely = tiley / tileHeight;
        int zoom = calculateZoom(tilex);
        int tileStartX = calculateTileX(minx, zoom);
        int tileStartY = calculateTileY(maxy, zoom);
        for (int loopx = 0; loopx < screensx; loopx++) {
            for (int loopy = 0; loopy < screensy; loopy++) {
                int xoffset = loopx * tileWidth;
                int yoffset = (screensy - loopy - 1) * tileHeight;
                MapSegment segment = new MapSegment(loopx, loopy, tileStartX + loopx, tileStartY + (screensy - loopy - 1), 0, 0, tileWidth, tileHeight, xoffset, yoffset, "" + zoom + "-" + (tileStartX + loopx) + "," + (tileStartY + (screensy - loopy - 1)) + ".png");
                segments.add(segment);
                graphics.setColor(Color.RED);
                graphics.drawRect(xoffset, yoffset, tileWidth - 1, tileHeight - 1);
                if (parentComponent != null) parentComponent.repaint();
            }
        }
        return segments;
    }

    protected boolean renderSegment(MapSegment segment, Graphics2D graphics, String tempDirectory) {
        boolean success = true;
        int mapX = imageParams.getWidth();
        int mapY = imageParams.getHeight();
        if (getAddText() != null) {
            mapY += addTextHeight;
        }
        graphics.setColor(Color.WHITE);
        graphics.fillRect(segment.getXOffset(), segment.getYOffset(), segment.getWidth(), segment.getHeight());
        RenderedImage image = null;
        log.info("Reading segment " + segment.getFilename());
        PNGDecodeParam decodeParam = new PNGDecodeParam();
        decodeParam.setSuppressAlpha(false);
        try {
            FileInputStream in = new FileInputStream(tempDirectory + File.separator + segment.getFilename());
            ImageDecoder decoder = new com.sun.media.jai.codecimpl.PNGImageDecoder(in, decodeParam);
            image = (RenderedImage) decoder.decodeAsRenderedImage();
        } catch (IOException e) {
            log.error("File problem", e);
            success = false;
        } catch (Exception e) {
            log.error("Some other problem", e);
            success = false;
        }
        if (image != null && success) {
            AffineTransform transform = new AffineTransform(1f, 0f, 0f, 1f, segment.getXOffset(), segment.getYOffset());
            log.info("Pasting segment to " + segment.getXOffset() + "," + segment.getYOffset());
            graphics.drawRenderedImage(image, transform);
            if (parentComponent != null) {
                parentComponent.repaint();
            }
        } else {
            log.error("Unable to render map segment " + segment.getFilename() + "." + "Perhaps there is a problem on the remote site.");
            success = false;
        }
        if (!success) segment.setComplete(false);
        return success;
    }

    protected boolean deleteSegments(String outputDirectory, List<MapSegment> segments) {
        boolean success = false;
        for (int loop = 0; loop < segments.size(); loop++) {
            MapSegment segment = segments.get(loop);
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

    protected boolean requestMap(int zoom, int segmentX, int segmentY, String outputDirectory, String filename) {
        boolean success = true;
        String url = null;
        boolean found = false;
        for (int loop = 0; loop < rootLayer.getSubLayers().size() && !found; loop++) {
            Layer layer = rootLayer.getSubLayers().get(loop);
            if (layer.isVisible()) {
                url = layer.getID() + "/" + zoom + "/" + segmentX + "/" + segmentY + ".png";
                found = true;
            }
        }
        try {
            success = downloadImage(url, outputDirectory, filename);
        } catch (Exception e) {
            log.error("Problem requesting map.", e);
            success = false;
        }
        return (success);
    }

    protected boolean downloadImage(String url, String outputDirectory, String filename) {
        boolean success = true;
        boolean found = false;
        int attempts = 0;
        log.info("Requesting image URL " + url);
        while (success && !found && (attempts < 5)) {
            found = true;
            try {
                GetMethod get = new GetMethod(url);
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
                log.error("Failed to get image", e);
                success = false;
            }
            attempts++;
        }
        return (success);
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

        protected boolean drawSegments(List<MapSegment> segments, int zoom, Graphics2D graphics, String outputDirectory) {
            boolean success = true;
            ArrayList<MapSegment> incompleteSegments = new ArrayList<MapSegment>(segments);
            while ((incompleteSegments.size() > 0) && success) {
                int index;
                log.info(incompleteSegments.size() + " segments remaining");
                index = 0;
                MapSegment segment = (MapSegment) incompleteSegments.get(index);
                success = requestMap(zoom, (int) segment.getMinX(), (int) segment.getMinY(), outputDirectory, segment.getFilename());
                if (success) {
                    if (renderSegment(segment, graphics, outputDirectory)) {
                        segment.setComplete(true);
                        incompleteSegments.remove(index);
                    } else {
                        segment.addError("Problem rendering segment");
                        if (segment.getErrorCount() > 1) {
                            incompleteSegments.remove(index);
                            success = true;
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
                        log.info("Sleeping for 1 second.");
                        Thread.sleep(500);
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
            int screensx = (int) (imageParams.getWidth() / tileWidth);
            int screensy = (int) (imageParams.getHeight() / tileHeight);
            Graphics2D graphics = image.createGraphics();
            List<MapSegment> segments = createSegments(imageParams, graphics);
            double tilex = (maxx - minx) / (double) screensx;
            int zoom = calculateZoom(tilex);
            progress.setMaximum(segments.size() + 3);
            progress.setValue(1);
            if (drawSegments(segments, zoom, graphics, tempDirectory)) {
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
