package de.herberlin.webapp.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.herberlin.webapp.db.Service;
import de.herberlin.webapp.image.transform.Transformer;

public class GetImageController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private Log logger = LogFactory.getLog(getClass());

    private DateFormat format = null;

    public void init() {
        format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer imageId = getImageId(req);
        String type = getType(req);
        logger.debug("imageId=" + imageId + ", type=" + type);
        if (imageId == null) {
            logger.debug("Unable to parse image id from url!");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = new File(getTempDir(), type + "_" + imageId + ".jpg");
        if (!file.exists()) {
            GalleryItem item = Service.getItem(GalleryItem.class, imageId);
            if (item == null) {
                logger.debug("Item not found for id: " + imageId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            byte[] bytes;
            try {
                bytes = transform(item, getImageDescription(type));
            } catch (Exception e) {
                logger.error(e, e);
                bytes = item.getData();
            }
            in2Out(new ByteArrayInputStream(bytes), new FileOutputStream(file));
            logger.debug("Image created:" + file);
        }
        String eTag = req.getHeader("If-None-Match");
        if (eTag != null && eTag.equals(getETag(file))) {
            logger.debug("Sending not modified for ETag header: " + eTag);
            resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        String lastModified = req.getHeader("If-Modified-Since");
        if (lastModified != null) {
            try {
                Date lastModDate = format.parse(lastModified);
                if (lastModDate != null && !lastModDate.before(new Date(file.lastModified()))) {
                    logger.debug("Sending not modified for If-Modified-Since header: " + lastModified);
                    resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            } catch (Exception e) {
                logger.warn(e, e);
            }
        }
        resp.setContentType("image/jpg");
        resp.setContentLength((int) file.length());
        resp.setHeader("Last-Modified", format.format(new Date(file.lastModified() + 1000)));
        resp.setHeader("ETag", getETag(file));
        in2Out(new FileInputStream(file), resp.getOutputStream());
    }

    private String getETag(File file) {
        return "h" + file.lastModified();
    }

    private byte[] transform(GalleryItem item, ImageDescription des) throws Exception {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(item.getData()));
        BufferedImage target = new BufferedImage(des.getWidth(), des.getHeight(), original.getType());
        if (ImageDescription.KEEP_ASPECT.equals(des)) {
            if (original.getWidth() < original.getHeight()) {
                target = new BufferedImage(des.getHeight(), des.getWidth(), original.getType());
            }
        }
        Transformer transformer;
        transformer = (Transformer) des.getTransformerClass().newInstance();
        target = transformer.transform(original, target);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(target, "jpg", out);
        return out.toByteArray();
    }

    private void in2Out(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }

    private String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    private Integer getImageId(HttpServletRequest req) {
        String uri = req.getRequestURI();
        int start = uri.lastIndexOf('_');
        int end = uri.lastIndexOf('.');
        if (start < 0) {
            logger.info("Start not found for uri: " + uri);
            return null;
        }
        if (end < 0) {
            logger.info("end not found for uri: " + uri);
            return null;
        }
        if (start >= end) {
            logger.info("start>=end for uri:" + uri);
        }
        try {
            return new Integer(uri.substring(start + 1, end));
        } catch (Exception e) {
            logger.error("Cant parse: " + uri + " to integer.", e);
            return null;
        }
    }

    private String getType(HttpServletRequest req) {
        String uri = req.getRequestURI();
        int start = uri.lastIndexOf('/');
        int end = uri.lastIndexOf('_');
        if (start < 0) {
            logger.info("Start not found for uri: " + uri);
            return null;
        }
        if (end < 0) {
            logger.info("end not found for uri: " + uri);
            return null;
        }
        if (start >= end) {
            logger.info("start>=end for uri:" + uri);
        }
        return uri.substring(start + 1, end);
    }

    private ImageDescription getImageDescription(String type) {
        return ImageDescription.valueOf(type);
    }
}
