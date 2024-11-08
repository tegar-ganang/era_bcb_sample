package com.peterdamen.gis;

import com.peterdamen.interfaces.MapHandler;
import com.peterdamen.web.HTTPResponse;
import com.peterdamen.web.URLDownloader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 22 $
 */
public class GoogleMapHandler implements MapHandler {

    private static Log log = LogFactory.getLog(GoogleMapHandler.class);

    /**
     * DOCUMENT ME!
     *
     * @param Parameters
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception
     *             DOCUMENT ME!
     */
    public HTTPResponse generateMap(Map<String, String> Parameters) throws Exception {
        String mime = "image/jpeg";
        String type = "jpg";
        int width = Integer.parseInt((String) Parameters.get("WIDTH"));
        int height = Integer.parseInt((String) Parameters.get("HEIGHT"));
        String bbox = (String) Parameters.get("BBOX");
        int westsouth = bbox.indexOf(",");
        int southeast = bbox.indexOf(",", westsouth + 1);
        int eastnorth = bbox.indexOf(",", southeast + 1);
        double west = Double.parseDouble(bbox.substring(0, westsouth));
        double south = Double.parseDouble(bbox.substring(westsouth + 1, southeast));
        double east = Double.parseDouble(bbox.substring(southeast + 1, eastnorth));
        double north = Double.parseDouble(bbox.substring(eastnorth + 1));
        if (Parameters.containsKey("FORMAT")) {
            mime = (String) Parameters.get("FORMAT");
            if (mime.contains("jpg") || mime.contains("jpeg")) {
                type = "jpg";
            }
            if (mime.contains("gif")) {
                type = "gif";
            }
            if (mime.contains("png")) {
                type = "png";
            }
            log.debug("Output format changed to: " + mime + " (" + type + ")");
        }
        BoundingBox box = new BoundingBox(new GISPosition(north, west), new GISPosition(south, east));
        BoundingBox result = new BoundingBox();
        if (width > 2000) {
            width = 2000;
        }
        if (height > 2000) {
            height = 2000;
        }
        String URL = URLGenerator.generateURL(box, width, height, result);
        byte[] image = null;
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try {
            BufferedImage imageCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            BufferedImage imageImage = null;
            baos = new ByteArrayOutputStream();
            bais = new ByteArrayInputStream(URLDownloader.downloadURL(URL));
            imageImage = ImageIO.read(bais);
            int sx1 = -1;
            int sy1 = -1;
            int sx2 = -1;
            int sy2 = -1;
            if (result.width() > box.width()) {
                double widthScale = (box.width() / result.width());
                double heightScale = (box.height() / result.height());
                log.debug(" Scaling Up: horizontally " + widthScale + "%, vertically " + heightScale + "%");
                int halfX = imageImage.getWidth() / 2;
                int halfY = imageImage.getHeight() / 2;
                sx1 = halfX - new Double(halfX * widthScale).intValue();
                sy1 = halfY - new Double(halfY * heightScale).intValue();
                sx2 = halfX + new Double(halfX * widthScale).intValue();
                sy2 = halfY + new Double(halfY * heightScale).intValue();
            } else {
                double widthScale = (box.width() / result.width());
                double heightScale = (box.height() / result.height());
                log.debug(" Scaling Down: horizontally " + widthScale + "%, vertically " + heightScale + "%");
                int halfX = imageImage.getWidth() / 2;
                int halfY = imageImage.getHeight() / 2;
                sx1 = halfX - new Double(halfX * widthScale).intValue();
                sy1 = halfY - new Double(halfY * heightScale).intValue();
                sx2 = halfX + new Double(halfX * widthScale).intValue();
                sy2 = halfY + new Double(halfY * heightScale).intValue();
            }
            imageCanvas.getGraphics().drawImage(imageImage, 0, 0, width, height, sx1, sy1, sx2, sy2, null);
            ImageIO.write(imageCanvas, type, baos);
            image = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        HTTPResponse response = new HTTPResponse();
        response.setMimeType(mime);
        response.setContent(image);
        return response;
    }

    /**
     * DOCUMENT ME!
     *
     * @param Parameters
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception
     *             DOCUMENT ME!
     */
    public HTTPResponse generateCapabilities(Map<String, String> Parameters) throws Exception {
        InputStream fr = null;
        fr = new FileInputStream("capabilities.xml");
        if (fr == null) {
            log.debug("Couldn't find capabilities resource in filesystem. Attempting to load from file from package.");
            fr = ClassLoader.getSystemResourceAsStream("capabilities.xml");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (fr.available() > 0) {
            baos.write(fr.read());
        }
        HTTPResponse response = new HTTPResponse();
        response.setMimeType("application/vnd.ogc.wms_xml");
        response.setContent(baos.toByteArray());
        fr.close();
        baos.close();
        return response;
    }
}
