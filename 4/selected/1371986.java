package com.servengine.image;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.geom.AffineTransform;
import java.io.*;

public class ThumbnailUtils {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(ThumbnailUtils.class.getName());

    static String EMPTY = "";

    static MediaTracker tracker;

    @SuppressWarnings("rawtypes")
    static Class cls;

    public ThumbnailUtils() {
    }

    public static void writeJPG(BufferedImage bufferedimage, OutputStream os, float quality) throws IOException {
        JPEGImageEncoder jpegimageencoder = JPEGCodec.createJPEGEncoder(os);
        JPEGEncodeParam jpegencodeparam = jpegimageencoder.getDefaultJPEGEncodeParam(bufferedimage);
        jpegencodeparam.setQuality(quality, false);
        jpegimageencoder.setJPEGEncodeParam(jpegencodeparam);
        jpegimageencoder.encode(bufferedimage);
        os.close();
        jpegimageencoder = null;
        System.gc();
    }

    public static void writeJPG(BufferedImage bufferedimage, File file, float quality) throws IOException {
        writeJPG(bufferedimage, new FileOutputStream(file), quality);
    }

    public static BufferedImage getScaledimage(Image image, Dimension dimension) {
        int oldwidth = image.getWidth(null);
        int oldheight = image.getHeight(null);
        float aspectratio = ((float) oldwidth) / ((float) oldheight);
        int l;
        int i1;
        if (oldwidth > dimension.width) {
            l = dimension.width;
            i1 = (int) ((float) l / aspectratio);
            if (i1 > dimension.height) {
                i1 = dimension.height;
                l = (int) (aspectratio * (float) i1);
            }
        } else if (oldheight > dimension.height) {
            i1 = dimension.height;
            l = (int) (aspectratio * (float) i1);
            if (l > dimension.width) {
                l = dimension.width;
                i1 = (int) ((float) l / aspectratio);
            }
        } else {
            l = oldwidth;
            i1 = oldheight;
        }
        Image newimage = loadImage(image.getScaledInstance(l, i1, l <= 640 || i1 <= 480 ? Image.SCALE_FAST : Image.SCALE_DEFAULT));
        image.flush();
        image = newimage;
        BufferedImage bufferedimage = new BufferedImage(l, i1, 1);
        java.awt.Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.drawImage(image, 0, 0, l, i1, null);
        image.flush();
        image = null;
        return bufferedimage;
    }

    public static BufferedImage getCroppedSquaredImage(Image image, Dimension dimension) {
        if (dimension.width > image.getWidth(null) || dimension.height > image.getHeight(null)) {
            log.warn("Image smaller than size. Returning same image.");
            BufferedImage bufferedimage = new BufferedImage(image.getWidth(null), image.getHeight(null), 1);
            return bufferedimage;
        }
        if (dimension.width < image.getWidth(null) && dimension.height < image.getHeight(null)) {
            if ((float) dimension.width / (float) image.getWidth(null) < (float) dimension.height / (float) image.getHeight(null)) dimension.height *= ((float) image.getHeight(null) / (float) image.getWidth(null)); else if ((float) dimension.width / (float) image.getWidth(null) > (float) dimension.height / (float) image.getHeight(null)) dimension.width *= ((float) image.getWidth(null) / (float) image.getHeight(null));
            image = getScaledimage(image, dimension);
        }
        int oldWidth = image.getWidth(null);
        int oldHeight = image.getHeight(null);
        int size = oldWidth > oldHeight ? oldHeight : oldWidth;
        Toolkit tk = Toolkit.getDefaultToolkit();
        ImageProducer source = image.getSource();
        ImageFilter cropImageFilter = new CropImageFilter(oldWidth / 2 - size / 2, oldHeight / 2 - size / 2, size, size);
        BufferedImage bufferedimage = new BufferedImage(size, size, 1);
        java.awt.Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.drawImage(tk.createImage(new FilteredImageSource(source, cropImageFilter)), 0, 0, size, size, null);
        return bufferedimage;
    }

    public static BufferedImage getCroppedImage(Image image, Dimension dimension) {
        if (dimension.width > image.getWidth(null) || dimension.height > image.getHeight(null)) {
            log.warn("Image smaller than size. Returning same image.");
            BufferedImage bufferedimage = new BufferedImage(image.getWidth(null), image.getHeight(null), 1);
            return bufferedimage;
        }
        if (dimension.width < image.getWidth(null) && dimension.height < image.getHeight(null)) {
            float scaleRatio = image.getWidth(null) - dimension.width > image.getHeight(null) - dimension.height ? (float) image.getHeight(null) / (float) dimension.height : (float) image.getWidth(null) / (float) dimension.width;
            Dimension scaleDimension = new Dimension();
            scaleDimension.height = (int) (image.getHeight(null) / scaleRatio);
            scaleDimension.width = (int) (image.getWidth(null) / scaleRatio);
            image = getScaledimage(image, scaleDimension);
        }
        int oldWidth = image.getWidth(null);
        int oldHeight = image.getHeight(null);
        Toolkit tk = Toolkit.getDefaultToolkit();
        ImageProducer source = image.getSource();
        ImageFilter cropImageFilter = new CropImageFilter(oldWidth / 2 - dimension.width / 2, oldHeight / 2 - dimension.height / 2, dimension.width, dimension.height);
        BufferedImage bufferedimage = new BufferedImage(dimension.width, dimension.height, 1);
        java.awt.Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.drawImage(tk.createImage(new FilteredImageSource(source, cropImageFilter)), 0, 0, dimension.width, dimension.height, null);
        return bufferedimage;
    }

    public static Thumbnail getThumbImage(File file, Dimension dimension) throws IOException {
        Image obj = getImage(file);
        if (obj == null) throw new IllegalArgumentException("Can't get scaled image of a null Image object (" + file.getAbsolutePath() + ")");
        Thumbnail thumbnail = new Thumbnail();
        thumbnail.origw = obj.getWidth(null);
        ;
        thumbnail.origh = obj.getHeight(null);
        ;
        thumbnail.nw = (int) dimension.getWidth();
        thumbnail.nh = (int) dimension.getHeight();
        thumbnail.bi = getScaledimage(obj, dimension);
        return thumbnail;
    }

    @Deprecated
    public static Image getImage(InputStream inputstream) throws IOException {
        return JPEGtoImage(inputstream);
    }

    public static Image JPEGtoImage(InputStream inputstream) throws IOException {
        {
            JPEGImageDecoder jpegimagedecoder = JPEGCodec.createJPEGDecoder(inputstream);
            Image image = jpegimagedecoder.decodeAsBufferedImage();
            inputstream.close();
            return image;
        }
    }

    public static Image getImage(File file) throws IOException {
        String s = file.getName();
        int i = s.length() - 4;
        Image obj = null;
        if (s.regionMatches(true, i, ".jpg", 0, 4)) {
            FileInputStream fileinputstream = new FileInputStream(file);
            JPEGImageDecoder jpegimagedecoder = JPEGCodec.createJPEGDecoder(fileinputstream);
            obj = jpegimagedecoder.decodeAsBufferedImage();
            fileinputstream.close();
        } else if (s.regionMatches(true, i, ".gif", 0, 4)) obj = loadImage(Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath())); else if (s.regionMatches(true, i, ".bmp", 0, 4)) obj = BMPUtils.load(file); else try {
            obj = Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath());
            if (tracker == null) tracker = new MediaTracker(new Container());
            tracker.addImage(obj, 0);
            tracker.checkID(0);
            tracker.waitForAll();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return obj;
    }

    static Image loadImage(Image image) {
        try {
            if (tracker == null) tracker = new MediaTracker(new Container());
            tracker.addImage(image, 0);
            tracker.waitForAll();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return null;
        }
        if (image.getWidth(null) <= 0) return null; else return image;
    }

    static boolean checkVersion() {
        String s = System.getProperty("java.version");
        if (s.compareTo("1.2") < 0) {
            return false;
        } else {
            return true;
        }
    }

    static void copyFile(File file, File file1) throws IOException {
        byte abyte0[] = new byte[512];
        FileInputStream fileinputstream = new FileInputStream(file);
        FileOutputStream fileoutputstream = new FileOutputStream(file1);
        int i;
        while ((i = fileinputstream.read(abyte0)) > 0) fileoutputstream.write(abyte0, 0, i);
        fileinputstream.close();
        fileoutputstream.close();
    }

    public static BufferedImage rotate(Image image, int degrees) {
        AffineTransform identity = new AffineTransform();
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), 1);
        java.awt.Graphics2D g2d = bufferedImage.createGraphics();
        AffineTransform trans = new AffineTransform();
        trans.setTransform(identity);
        trans.rotate(Math.toRadians(degrees));
        g2d.drawImage(image, trans, null);
        image.flush();
        bufferedImage.flush();
        return bufferedImage;
    }
}
