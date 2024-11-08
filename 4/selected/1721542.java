package org.elmarweber.sf.appletrailerfs;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

public class GenerateCovers {

    private static Log log = LogFactory.getLog(GenerateCovers.class);

    private static final String TRAILER_JPG_URL = "http://appletrailerfs.elmarweber.org/files/images/trailers";

    private static final String OSD_COVER_JPG_URL = "http://appletrailerfs.elmarweber.org/files/osd/movies/images";

    public static void main(String[] args) throws Exception {
        PropertiesUtil.setPropertiesStreamName(System.getProperty("moviejukebox.properties") != null ? System.getProperty("moviejukebox.properties") : "moviejukebox.properties");
        JSONArray movies = new JSONArray(FileUtils.readFileToString(new File("./database.json")));
        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            {
                String uri = movie.getString("poster");
                String md5Uri = DigestUtils.md5Hex(uri).toUpperCase();
                File file = new File("atfs_cache/" + md5Uri);
                if (!file.exists()) {
                    log.info("Getting poster for movie " + movie.getString("title") + " from " + uri);
                    FileUtils.writeByteArrayToFile(file, HTTPUtils.getContent(uri));
                    generateTrailerCovers(movie, file);
                }
            }
            {
                if (!movie.has("posterlarge")) {
                    log.warn("Movie " + movie.getString("title") + " has no large poster");
                } else {
                    String uri = movie.getString("posterlarge");
                    String md5Uri = DigestUtils.md5Hex(uri).toUpperCase();
                    File file = new File("coverlarge_cache/" + md5Uri);
                    if (!file.exists()) {
                        log.info("Getting poster for movie " + movie.getString("title") + " from " + uri);
                        FileUtils.writeByteArrayToFile(file, HTTPUtils.getContent(uri));
                        BufferedImage image;
                        try {
                            image = ImageIO.read(file);
                        } catch (Exception ex) {
                            log.error("Could not load image: " + ex.toString());
                            image = null;
                        }
                        if (image == null) {
                            log.warn("Image is errornous, deleting " + file);
                            FileUtils.deleteQuietly(file);
                        } else {
                            generateOsdCover(movie, image);
                        }
                    }
                }
            }
        }
        FileUtils.writeStringToFile(new File("./database.json"), movies.toString());
    }

    private static void generateTrailerCovers(JSONObject movie, File imageFile) throws IOException, JSONException {
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (Exception ex) {
            log.error("could not read image file " + imageFile);
            return;
        }
        BufferedImage overlay = ImageIO.read(new File("./templates/overlay.png"));
        for (int i = 0; i < movie.getJSONArray("trailers").length(); i++) {
            JSONObject trailer = movie.getJSONArray("trailers").getJSONObject(i);
            log.debug("Generating thumbnail for trailer " + trailer.getString("filename"));
            String type = trailer.getString("type");
            String resolution = trailer.getString("resolution");
            BufferedImage trailerImage = new BufferedImage(120, 180, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = (Graphics2D) trailerImage.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(image, 0, 0, null);
            g.drawImage(overlay, 0, 0, null);
            g.setFont(Font.decode("Verdana").deriveFont(30f));
            g.drawString(type, 5, 32);
            g.setFont(Font.decode("Verdana").deriveFont(26f));
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D area = fm.getStringBounds(resolution, g);
            g.drawString(resolution, (int) (trailerImage.getWidth() - area.getWidth()) / 2, 170);
            File file = new File("./tmp.jpg");
            ImageIO.write(trailerImage, "JPG", file);
            String md5Name = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file)).toUpperCase();
            String url = TRAILER_JPG_URL + "/" + md5Name;
            String urlMd5 = DigestUtils.md5Hex(url).toUpperCase();
            File ftpFile = new File("./ftp/files/images/trailers/" + md5Name);
            FileUtils.copyFile(file, ftpFile);
            FileUtils.copyFile(file, new File("./atfs_cache/" + urlMd5));
            trailer.put("jpgurl", url);
            trailer.put("jpgurlsize", file.length());
            FileUtils.deleteQuietly(file);
            log.debug("Generated thumbnail for trailer " + trailer.getString("filename") + " to " + ftpFile);
        }
    }

    private static void generateOsdCover(JSONObject movie, BufferedImage image) throws IOException, JSONException {
        BufferedImage osdImage = image;
        osdImage = GraphicTools.createReflectedPicture(osdImage, "posters");
        osdImage = GraphicTools.create3DPicture(osdImage, "posters", "right");
        osdImage = GraphicTools.scaleToSizeBestFit(350, 590, osdImage);
        File file = new File("./tmp.jpg");
        ImageIO.write(osdImage, "JPG", file);
        String md5Name = DigestUtils.md5Hex(FileUtils.readFileToByteArray(file)).toUpperCase();
        String url = OSD_COVER_JPG_URL + "/" + md5Name;
        String urlMd5 = DigestUtils.md5Hex(url).toUpperCase();
        FileUtils.copyFile(file, new File("./ftp/files/osd/movies/images/" + md5Name));
        FileUtils.copyFile(file, new File("./atfs_cache/" + urlMd5));
        movie.put("osdcoverurl", url);
        movie.put("osdcoverurlsize", file.length());
        FileUtils.deleteQuietly(file);
    }
}
