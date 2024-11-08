package edu.psu.its.lionshare.util;

import com.limegroup.gnutella.xml.LimeXMLProperties;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImageIndexer {

    private static final Log LOG = LogFactory.getLog(ImageIndexer.class);

    public static final int THUMBNAIL_SIZE_100 = 100;

    public static final int THUMBNAIL_SIZE_255 = 255;

    public static final int THUMBNAIL_SIZE_MAX = 1000;

    private static final int MAX_WIDTH = 1000;

    private static final int MAX_HEIGHT = 1000;

    public static final String EXTENSION = "jpg";

    public static String getImagePath(String fid, boolean thumb) {
        String parent = LimeXMLProperties.instance().getPath();
        String path = parent + "thumbnails" + File.separator;
        if (thumb) {
            path = path + "b" + fid + "." + EXTENSION;
        } else {
            path = path + "a" + fid + "." + EXTENSION;
        }
        return path;
    }

    public static synchronized BufferedImage getThumbnail(java.io.File file) {
        ImageReader reader = null;
        String ext = FileUtility.getFileExtension(file);
        if (ext == null) {
            return null;
        }
        Iterator iter = ImageIO.getImageReadersByFormatName(ext);
        while (iter.hasNext()) {
            reader = (ImageReader) iter.next();
        }
        BufferedImage image = null;
        try {
            ImageReadParam parm = new ImageReadParam();
            parm.setSourceSubsampling(1, 1, 0, 0);
            ImageInputStream stream = ImageIO.createImageInputStream(file);
            reader.setInput(stream, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            float aspect = reader.getAspectRatio(0);
            image = reader.read(0, parm);
            stream.flush();
            stream.close();
            reader.dispose();
            int divisor = 1;
            if (aspect > 1.0) {
                divisor = width;
            } else {
                divisor = height;
            }
            double ratio_2 = (THUMBNAIL_SIZE_100 / (double) divisor);
            int thumb_width = (int) (width * ratio_2);
            int thumb_height = (int) (height * ratio_2);
            BufferedImage thumber2 = new BufferedImage(thumb_width, thumb_height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) thumber2.getGraphics();
            g2d.drawImage(image, 0, 0, thumb_width, thumb_height, null);
            image.flush();
            return thumber2;
        } catch (Exception e) {
        } finally {
            try {
                image.flush();
            } catch (Exception e2) {
            }
        }
        return null;
    }

    public static synchronized BufferedImage indexImage(java.io.File file, long fid, long vdid) {
        File dest = new File(LimeXMLProperties.instance().getPath() + File.separator + "thumbnails");
        if (!dest.exists()) {
            dest.mkdirs();
        }
        ImageReader reader = null;
        ImageWriter writer = null;
        BufferedImage image = null;
        Iterator iter = ImageIO.getImageReadersByFormatName(FileUtility.getFileExtension(file));
        while (iter.hasNext()) {
            reader = (ImageReader) iter.next();
        }
        iter = ImageIO.getImageWritersByFormatName(EXTENSION);
        if (iter.hasNext()) {
            writer = (ImageWriter) iter.next();
        }
        if (reader != null && writer != null) {
            try {
                ImageReadParam parm = new ImageReadParam();
                parm.setSourceSubsampling(1, 1, 0, 0);
                ImageInputStream stream = ImageIO.createImageInputStream(file);
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                float aspect = reader.getAspectRatio(0);
                image = reader.read(0, parm);
                stream.flush();
                stream.close();
                reader.dispose();
                int divisor = 1;
                if (aspect > 1.0) {
                    divisor = width;
                } else {
                    divisor = height;
                }
                double ratio_2 = (THUMBNAIL_SIZE_100 / (double) divisor);
                double ratio_3 = (THUMBNAIL_SIZE_255 / (double) divisor);
                int thumb_width = (int) (width * ((double) MAX_WIDTH / (double) divisor));
                int thumb_height = (int) (height * ((double) MAX_HEIGHT / (double) divisor));
                if ((width < MAX_WIDTH) || (height < MAX_HEIGHT)) {
                    thumb_width = width;
                    thumb_height = height;
                }
                BufferedImage actual = new BufferedImage(thumb_width, thumb_height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = (Graphics2D) actual.getGraphics();
                g2d.drawImage(image, 0, 0, thumb_width, thumb_height, null);
                String path = LimeXMLProperties.instance().getPath() + File.separator + "thumbnails" + File.separator + "a" + (new Long(fid)).toString() + "." + EXTENSION;
                IIOImage img = new IIOImage(actual, null, null);
                ImageOutputStream iout = ImageIO.createImageOutputStream(new File(path));
                writer.setOutput(iout);
                writer.write(img);
                actual.flush();
                iout.flush();
                iout.close();
                g2d.finalize();
                thumb_width = (int) (width * ratio_3);
                thumb_height = (int) (height * ratio_3);
                BufferedImage thumber1 = new BufferedImage(thumb_width, thumb_height, BufferedImage.TYPE_INT_RGB);
                g2d = (Graphics2D) thumber1.getGraphics();
                g2d.drawImage(image, 0, 0, thumb_width, thumb_height, null);
                thumb_width = (int) (width * ratio_2);
                thumb_height = (int) (height * ratio_2);
                BufferedImage thumber2 = new BufferedImage(thumb_width, thumb_height, BufferedImage.TYPE_INT_RGB);
                g2d = (Graphics2D) thumber2.getGraphics();
                g2d.drawImage(image, 0, 0, thumb_width, thumb_height, null);
                List thumbnails = new ArrayList();
                thumbnails.add(thumber2);
                path = LimeXMLProperties.instance().getPath() + File.separator + "thumbnails" + File.separator + "b" + (new Long(fid)).toString() + "." + EXTENSION;
                img = new IIOImage(thumber1, thumbnails, null);
                iout = ImageIO.createImageOutputStream(new File(path));
                writer.setOutput(iout);
                writer.write(img);
                iout.flush();
                iout.close();
                writer.dispose();
                g2d.finalize();
                thumber1.flush();
                image.flush();
                return thumber2;
            } catch (Exception e) {
                if (image != null) {
                    image.flush();
                }
                LOG.trace(" ", e);
            }
        }
        return null;
    }

    public static void removeImage(String id) {
        File file = new File(LimeXMLProperties.instance().getPath() + File.separator + "thumbnails" + File.separator + "b" + id + "." + EXTENSION);
        if (file.exists()) {
            file.delete();
        }
        file = new File(LimeXMLProperties.instance().getPath() + File.separator + "thumbnails" + File.separator + "b" + id + "." + EXTENSION);
        if (file.exists()) {
            file.delete();
        }
    }

    public static synchronized String encodeImage(Image thumbnail) {
        String base64_image = "Invalid Image";
        if (thumbnail != null) {
            Iterator iter = null;
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageOutputStream iout = ImageIO.createImageOutputStream(out);
                iter = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = null;
                if (iter.hasNext()) {
                    writer = (ImageWriter) iter.next();
                }
                if (writer != null) {
                    writer.setOutput(iout);
                    writer.write((BufferedImage) thumbnail);
                    writer.dispose();
                }
                byte[] array = out.toByteArray();
                base64_image = codec.Base64.encode(array);
                thumbnail.flush();
                thumbnail.flush();
                iout.flush();
                iout.close();
                out.flush();
            } catch (Exception e) {
            }
        }
        return base64_image;
    }

    public static synchronized BufferedImage decodeImage(String base64_image) {
        try {
            byte[] byte_image = codec.Base64.decode(base64_image);
            ByteArrayInputStream bin = new ByteArrayInputStream(byte_image);
            ImageInputStream istream = ImageIO.createImageInputStream(bin);
            BufferedImage img = ImageIO.read(istream);
            bin.close();
            return img;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e + " Error reading image");
            }
        }
        return null;
    }

    public static synchronized String getBase64Preview(String id) {
        String path = ImageIndexer.getImagePath(id, true);
        java.io.File image_file = new java.io.File(path);
        BufferedImage image = null;
        ImageReader reader = null;
        Iterator iter = ImageIO.getImageReadersByFormatName(ImageIndexer.EXTENSION);
        if (iter.hasNext()) {
            reader = (ImageReader) iter.next();
        }
        if (reader != null) {
            try {
                ImageInputStream stream = ImageIO.createImageInputStream(image_file);
                reader.setInput(stream, true, true);
                image = reader.readThumbnail(0, 0);
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e + " Error reading image");
                }
            }
        }
        return ImageIndexer.encodeImage(image);
    }
}
