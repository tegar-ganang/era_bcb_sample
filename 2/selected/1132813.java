package org.mars_sim.msp.ui.swing.tool.map;

import org.mars_sim.msp.core.Coordinates;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * Access the Mars maps provided by the The Unites States Geological
 * Survey - Astrogeology Team and The Planetary Data System - Imaging
 * Node. Specifically from the Planetary Data Systems (PDS) Mars
 * Explorer. Behind their web server is a Solaris application called
 * MapMaker that generates the maps.
 * @see <a href="http://www-pdsimage.wr.usgs.gov/PDS/public/mapmaker/faq.htm">PDS Mars Explorer</a>
 */
public class USGSMarsMap implements Map, ActionListener {

    private static String CLASS_NAME = "org.mars_sim.msp.ui.standard.tool.map.USGSMarsMap";

    private static Logger logger = Logger.getLogger(CLASS_NAME);

    public static final String TYPE = "USGS map";

    public static final double HALF_MAP_ANGLE = .06106D;

    public static final int MAP_HEIGHT = 11458;

    public static final int MAP_WIDTH = 22916;

    public static final double PIXEL_RHO = (double) MAP_HEIGHT / Math.PI;

    public static final double HALF_MAP_ANGLE_DEG = 150D / 64D;

    private static final String psdUrl = "http://www.mapaplanet.org";

    private static final String psdCgi = "/explorer-bin/explorer.cgi";

    private static final String map = "Mars";

    private static final String layers = "mars_viking_merged";

    private static final String info = "NO";

    private static final String advoption = "YES";

    private static final String lines = "2668";

    private static final String samples = "720";

    private static final String sizeSelector = "resolution";

    private static final String Resolution = "64";

    private static final String R = "1";

    private static final String G = "2";

    private static final String B = "3";

    private static final String projection = "MERC";

    private static final String grid = "none";

    private static final String stretch = "none";

    private static final String resamp_method = "nearest_neighbor";

    private static final String center = "0";

    private static final String defaultcenter = "on";

    private static final String center_lat = "0";

    private boolean imageDone = false;

    private Component component;

    private boolean goodConnection = false;

    private boolean connectionTimeout = false;

    private Image img;

    private Timer connectionTimer = null;

    /** Constructs a USGSMarsMap object */
    public USGSMarsMap() {
    }

    /** Constructs a USGSMarsMap object 
     *  @param comp the map's container component
     */
    public USGSMarsMap(Component comp) {
        component = comp;
    }

    /** 
     * Creates a 2D map at a given center point.
     *
     * @param newCenter the center location.
     * @throws Exception if error in drawing map.
     */
    public void drawMap(Coordinates newCenter) {
        connectionTimeout = false;
        double lat = 90D - Math.toDegrees(newCenter.getPhi());
        double lon = 360D - Math.toDegrees(newCenter.getTheta());
        if (lon > 180D) lon = lon - 360D;
        startPdsImageRetrieval(lon, lat);
        connectionTimer = new Timer(10000, this);
        connectionTimer.start();
    }

    /**
     * Checks if the connection has timed out.
     * @return boolean
     */
    public boolean isConnectionTimeout() {
        return connectionTimeout;
    }

    /** determines if a requested map is complete 
     *  @return true if requested map is complete
     */
    public boolean isImageDone() {
        return imageDone;
    }

    /** Returns map image 
     *  @return map image
     */
    public Image getMapImage() {
        return img;
    }

    /**
     * Starts the PDS image retrieval process.
     * @param lat the latitude of the center of the image.
     * @param lon the longitude of the center of the image.
     * @throws IOException if there is an IO problem.
     */
    private void startPdsImageRetrieval(double lat, double lon) {
        imageDone = false;
        goodConnection = false;
        URL url = null;
        url = getPDSURL(lat, lon);
        HttpURLConnection urlCon;
        try {
            urlCon = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        new PDSConnectionManager(urlCon, this);
    }

    /**
     * Determines the URL for the USGS PDS server.
     * @param lat the latitude
     * @param lon the longitude
     * @return URL the URL created.
     * @throws Exception if the URL is malformed.
     */
    private URL getPDSURL(double lat, double lon) {
        DecimalFormat formatter = new DecimalFormat("0.000");
        double westSide = lon + HALF_MAP_ANGLE_DEG;
        if (westSide > 180D) westSide = (westSide - 180D) - 180D;
        double eastSide = lon - HALF_MAP_ANGLE_DEG;
        if (eastSide < -180D) eastSide = (eastSide + 180D) + 180D;
        double northSide = lat + HALF_MAP_ANGLE_DEG;
        if (northSide > 90D) northSide = 90D + (90D - northSide);
        double southSide = lat - HALF_MAP_ANGLE_DEG;
        if (southSide < -90D) southSide = -90D + (-90D - southSide);
        StringBuilder urlBuff = new StringBuilder(psdUrl + psdCgi + "?");
        urlBuff.append("map=" + map);
        urlBuff.append("&layers=" + layers);
        urlBuff.append("&info=" + info);
        urlBuff.append("&advoption=" + advoption);
        urlBuff.append("&lines=" + lines);
        urlBuff.append("&samples=" + samples);
        urlBuff.append("&sizeSelector=" + sizeSelector);
        urlBuff.append("&Resolution=" + Resolution);
        urlBuff.append("&R=" + R);
        urlBuff.append("&G=" + G);
        urlBuff.append("&B=" + B);
        urlBuff.append("&projection=" + projection);
        urlBuff.append("&grid=" + grid);
        urlBuff.append("&stretch=" + stretch);
        urlBuff.append("&resamp_method=" + resamp_method);
        urlBuff.append("&north=").append(formatter.format(northSide));
        urlBuff.append("&west=").append(formatter.format(westSide));
        urlBuff.append("&east=").append(formatter.format(eastSide));
        urlBuff.append("&south=").append(formatter.format(southSide));
        urlBuff.append("&center=" + center);
        urlBuff.append("&defaultcenter=" + defaultcenter);
        urlBuff.append("&center_lat=" + center_lat);
        try {
            return new URL(urlBuff.toString());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Reads HTML data from the connection and determines the image's URL.
     * @param connection the URL connection to use.
     * @throws IOException if there is an IO problem.
     */
    private void connectionEstablished(URLConnection connection) throws IOException {
        goodConnection = true;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String imageSrc = "";
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.indexOf("View and Save") > -1) {
                    String line2 = in.readLine();
                    if (line2 != null) {
                        int startIndex = line2.indexOf("/explorer");
                        int endIndex = line2.indexOf("jpg") + 3;
                        String relativeUrl = line2.substring(startIndex, endIndex);
                        imageSrc = psdUrl + relativeUrl;
                    }
                }
            }
            URL imageUrl = new URL(imageSrc);
            img = (Toolkit.getDefaultToolkit().getImage(imageUrl));
            waitForMapLoaded();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new IOException("Internet connection required");
        }
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
        connectionTimer.stop();
        connectionTimeout = !goodConnection;
    }

    /** Wait for USGS map image to load */
    private void waitForMapLoaded() {
        MediaTracker tracker = new MediaTracker(component);
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "waitForMapLoaded()", e);
        }
        imageDone = true;
    }

    /**
     * Internal class for connecting to the USGS PDS image server.  
     * Uses its own thread.
     */
    private class PDSConnectionManager implements Runnable {

        private Thread connectionThread = null;

        private URLConnection connection = null;

        private USGSMarsMap map = null;

        /**
         * Constructor
         * @param connection the URL connection to use.
         * @param map the parent map class.
         */
        private PDSConnectionManager(URLConnection connection, USGSMarsMap map) {
            this.connection = connection;
            this.map = map;
            if ((connectionThread == null) || (!connectionThread.isAlive())) {
                connectionThread = new Thread(this, "HTTP connection");
                connectionThread.start();
            }
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                connection.connect();
                map.connectionEstablished(connection);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to connect to: " + e.getMessage());
            }
        }
    }
}
