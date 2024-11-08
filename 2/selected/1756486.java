package tac.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openstreetmap.gui.jmapviewer.interfaces.MapSource;
import org.openstreetmap.gui.jmapviewer.interfaces.MapSpace;
import tac.mapsources.MapSourcesManager;
import tac.mapsources.impl.OsmMapSources.OsmHikingMap;
import tac.program.Logging;
import tac.program.model.EastNorthCoordinate;

public class MapSourceTypeDetector {

    public static final EastNorthCoordinate C_NEY_YORK = new EastNorthCoordinate(40.75, -73.88);

    public static final EastNorthCoordinate C_BERLIN = new EastNorthCoordinate(52.50, 13.39);

    public static final EastNorthCoordinate C_PRAHA = new EastNorthCoordinate(50.00, 14.41);

    public static final EastNorthCoordinate C_VIENNA = new EastNorthCoordinate(48.20, 16.37);

    public static final EastNorthCoordinate C_SEOUL = new EastNorthCoordinate(37.55, 126.98);

    public static final SecureRandom RND = new SecureRandom();

    static URL url;

    static HttpURLConnection c;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        Logging.configureLogging();
        MapSourcesManager.loadMapSourceProperties();
        testMapSource(OsmHikingMap.class, C_BERLIN);
    }

    public static void testMapSource(Class<? extends MapSource> mapSourceClass, EastNorthCoordinate coordinate) {
        MapSource ms;
        try {
            ms = mapSourceClass.newInstance();
            testMapSource(ms, coordinate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testMapSource(String mapSourceName, EastNorthCoordinate coordinate) {
        MapSource ms = MapSourcesManager.getSourceByName(mapSourceName);
        if (ms != null) testMapSource(ms, coordinate);
    }

    public static void testMapSource(MapSource mapSource, EastNorthCoordinate coordinate) {
        try {
            System.out.println("Testing " + mapSource.toString());
            int zoom = mapSource.getMinZoom() + ((mapSource.getMaxZoom() - mapSource.getMinZoom()) / 2);
            MapSpace mapSpace = mapSource.getMapSpace();
            int tilex = mapSpace.cLonToX(coordinate.lon, zoom) / mapSpace.getTileSize();
            int tiley = mapSpace.cLatToY(coordinate.lat, zoom) / mapSpace.getTileSize();
            url = new URL(mapSource.getTileUrl(zoom, tilex, tiley));
            System.out.println("Sample url: " + url);
            c = (HttpURLConnection) url.openConnection();
            System.out.println("Connecting...");
            c.connect();
            System.out.println("Connection established - response HTTP " + c.getResponseCode());
            if (c.getResponseCode() != 200) return;
            String contentType = c.getContentType();
            System.out.print("Image format          : ");
            if ("image/png".equals(contentType)) System.out.println("png"); else if ("image/jpeg".equals(contentType)) System.out.println("jpg"); else System.out.println("unknown");
            String eTag = c.getHeaderField("ETag");
            boolean eTagSupported = (eTag != null);
            if (eTagSupported) {
                System.out.println("eTag                  : " + eTag);
                testIfNoneMatch();
            } else System.out.println("eTag                  : -");
            long date = c.getDate();
            if (date == 0) System.out.println("Date time             : -"); else System.out.println("Date time             : " + new Date(date));
            long exp = c.getExpiration();
            if (exp == 0) System.out.println("Expiration time       : -"); else System.out.println("Expiration time       : " + new Date(exp));
            long modified = c.getLastModified();
            if (modified == 0) System.out.println("Last modified time    : not set"); else System.out.println("Last modified time    : " + new Date(modified));
            testIfModified();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("\n");
    }

    private static void testIfNoneMatch() throws Exception {
        String eTag = c.getHeaderField("ETag");
        InputStream in = c.getInputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        do {
            read = in.read(buffer);
            if (read > 0) md5.update(buffer, 0, read);
        } while (read < 0);
        byte[] digest = md5.digest();
        String hexDigest = getHexString(digest);
        if (hexDigest.equals(eTag)) System.out.print("eTag content          : md5 hex string");
        String quotedHexDigest = "\"" + hexDigest + "\"";
        if (quotedHexDigest.equals(eTag)) System.out.print("eTag content          : quoted md5 hex string");
        HttpURLConnection c2 = (HttpURLConnection) url.openConnection();
        c2.addRequestProperty("If-None-Match", eTag);
        c2.connect();
        int code = c2.getResponseCode();
        System.out.print("If-None-Match response: ");
        boolean supported = (code == 304);
        System.out.println(b2s(supported) + " - " + code + " (" + c2.getResponseMessage() + ")");
    }

    private static void testIfModified() throws IOException {
        HttpURLConnection c2 = (HttpURLConnection) url.openConnection();
        c2.setIfModifiedSince(System.currentTimeMillis() + 1000);
        c2.connect();
        int code = c2.getResponseCode();
        System.out.print("If-Modified-Since     : ");
        boolean supported = (code == 304);
        System.out.println(b2s(supported) + " - " + code + " (" + c2.getResponseMessage() + ")");
    }

    static void printHeaders() {
        System.out.println("\nHeaders:");
        for (Map.Entry<String, List<String>> entry : c.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            for (String elem : entry.getValue()) {
                if (key != null) System.out.print(key + " = ");
                System.out.println(elem);
            }
        }
        System.out.println();
    }

    private static String b2s(boolean b) {
        if (b) return "supported"; else return "unsupported";
    }

    static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    public static String getHexString(byte[] raw) throws UnsupportedEncodingException {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII");
    }
}
