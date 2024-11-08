package org.elmarweber.sf.appletrailerfs;

import static org.elmarweber.sf.appletrailerfs.PathNames.changeExtension;
import static org.elmarweber.sf.appletrailerfs.PathNames.getTrailerFilename;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elmarweber.sf.appletrailerfs.FileSystemEntry.Type;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Contains functions to generate covers for movies and trailers.
 * 
 * 
 * @author Elmar Weber (appletrailerfs@elmarweber.org)
 */
public class CoverGenerator {

    private static Log log = LogFactory.getLog(CoverGenerator.class);

    private static final String TRAILER_JPG_SUFFIX = "/images/trailers";

    private static final String OSD_COVER_JPG_SUFFIX = "/osd/movies/images";

    private static final String OSD_COVER_CACHE_DIR = "./osd_cover_cache";

    private static final String OSD_THUMB_CACHE_DIR = "./osd_thumb_cache";

    static {
        PropertiesUtil.setPropertiesStreamName(System.getProperty("moviejukebox.properties") != null ? System.getProperty("moviejukebox.properties") : "moviejukebox.properties");
    }

    public static FileSystemEntry generateWdtvOsdCover(JSONObject movie) throws HttpException, IOException {
        if (!movie.has("posterlarge")) {
            log.warn("Movie " + movie.getString("title") + " has no large poster");
        } else {
            String url = movie.getString("posterlarge");
            String cacheName = DigestUtils.shaHex(url);
            File file = new File(OSD_COVER_CACHE_DIR + "/" + cacheName);
            if (file.exists()) {
                log.info("Using cached large poster for movie " + movie.getString("title") + " from " + file);
            } else {
                log.debug("Getting large poster for movie " + movie.getString("title") + " from " + url);
                FileUtils.writeByteArrayToFile(file, HTTPUtils.getContent(url));
            }
            BufferedImage image;
            try {
                image = ImageIO.read(file);
            } catch (Exception ex) {
                log.error("Could not load image: " + ex.toString());
                image = null;
            }
            if (image == null) {
                log.warn("Image is errornous, skipping");
                FileUtils.deleteQuietly(file);
            } else {
                FileSystemEntry entry = generateOsdCover(image);
                entry.setFilename("movie.cover.jpg");
                return entry;
            }
        }
        return null;
    }

    public static List<FileSystemEntry> generateWdtvTrailerCovers(JSONObject movie) throws IOException {
        String url = movie.getString("poster");
        if (StringUtils.isEmpty(url)) {
            log.warn("Movie " + movie.getString("title") + " has no poster to use for thumbnails");
            return null;
        }
        String cacheName = DigestUtils.shaHex(url);
        File file = new File(OSD_THUMB_CACHE_DIR + "/" + cacheName);
        if (file.exists()) {
            log.debug("Getting poster for movie " + movie.getString("title") + " from cached file " + file);
        } else {
            log.debug("Getting poster for movie " + movie.getString("title") + " from " + url);
            FileUtils.writeByteArrayToFile(file, HTTPUtils.getContent(url));
        }
        List<FileSystemEntry> entries = generateTrailerCovers(movie, file);
        return entries;
    }

    private static List<FileSystemEntry> generateTrailerCovers(JSONObject movie, File imageFile) throws IOException {
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (Exception ex) {
            log.error("could not read image file " + imageFile);
            return null;
        }
        List<FileSystemEntry> entries = new ArrayList<FileSystemEntry>();
        BufferedImage overlay = ImageIO.read(new File("./templates/overlay.png"));
        for (int i = 0; i < movie.getJSONArray("trailers").size(); i++) {
            JSONObject trailer = movie.getJSONArray("trailers").getJSONObject(i);
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
            File file = File.createTempFile("trailercover", null);
            ImageIO.write(trailerImage, "JPG", file);
            String md5Name = FtpHosting.hashFileForFtp(file);
            String url = FtpHosting.getUrl(TRAILER_JPG_SUFFIX + "/" + md5Name);
            File ftpFile = new File(FtpHosting.getLocalPath(TRAILER_JPG_SUFFIX + "/" + md5Name));
            FileUtils.copyFile(file, ftpFile);
            FileUtils.deleteQuietly(file);
            log.debug("Generated thumbnail for trailer " + trailer.getString("filename") + " to " + ftpFile);
            FileSystemEntry entry = new FileSystemEntry();
            entry.setType(Type.REMOTE);
            entry.setFilename(changeExtension(getTrailerFilename(movie, trailer), "jpg"));
            entry.setContentLength(ftpFile.length());
            entry.setUrl(url);
            entries.add(entry);
        }
        return entries;
    }

    private static FileSystemEntry generateOsdCover(BufferedImage image) throws IOException {
        BufferedImage osdImage = image;
        osdImage = GraphicTools.createReflectedPicture(osdImage, "posters");
        osdImage = GraphicTools.create3DPicture(osdImage, "posters", "right");
        osdImage = GraphicTools.scaleToSizeBestFit(350, 590, osdImage);
        File file = new File("./tmp.jpg");
        ImageIO.write(osdImage, "JPG", file);
        String md5Name = FtpHosting.hashFileForFtp(file);
        String url = FtpHosting.getUrl(OSD_COVER_JPG_SUFFIX + "/" + md5Name);
        FileUtils.copyFile(file, new File(FtpHosting.getLocalPath(OSD_COVER_JPG_SUFFIX + "/" + md5Name)));
        FileSystemEntry entry = new FileSystemEntry();
        entry.setType(Type.REMOTE);
        entry.setContentLength(file.length());
        entry.setUrl(url);
        FileUtils.deleteQuietly(file);
        return entry;
    }
}
