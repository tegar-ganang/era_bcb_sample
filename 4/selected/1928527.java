package eduburner.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.media.jai.codec.FileSeekableStream;

/**
 * 
 *   也可以看看olio的实现
 */
public class ImageScaler {

    private static final Logger logger;

    private ImageInfo imageInfo;

    public static final String JPEG_MIME_TYPE = "image/jpeg";

    public static final String PNG_MIME_TYPE = "image/png";

    public static final String GIF_MIME_TYPE = "image/gif";

    public static final List<String> THUMBNAIL_MIME_TYPES;

    public static final String JPEG_FORMAT = "JPEG";

    public static final String PNG_FORMAT = "PNG";

    public static final String GIF_FORMAT = "GIF";

    public static final List<String> THUMBNAIL_FORMATS;

    private float encodingQuality;

    static {
        logger = LoggerFactory.getLogger(ImageScaler.class);
        THUMBNAIL_MIME_TYPES = Lists.newArrayList();
        THUMBNAIL_FORMATS = Lists.newArrayList();
        THUMBNAIL_MIME_TYPES.add("image/jpeg");
        THUMBNAIL_MIME_TYPES.add("image/png");
        THUMBNAIL_MIME_TYPES.add("image/gif");
        THUMBNAIL_FORMATS.add("JPEG");
        THUMBNAIL_FORMATS.add("PNG");
        THUMBNAIL_FORMATS.add("GIF");
    }

    public static class WidthHeightHelper {

        private int width;

        private int height;

        public WidthHeightHelper(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    public ImageScaler() {
        imageInfo = new ImageInfo();
        encodingQuality = 0.8F;
    }

    public void resizeImage(File source, File resized, int newWidth, int newHeight) throws IOException {
        PlanarImage originalImage = JAI.create("stream", new FileSeekableStream(source));
        final int originalWidth = originalImage.getWidth();
        final int originalHeight = originalImage.getHeight();
        double hRatio = ((double) newHeight / (double) originalHeight);
        double wRatio = ((double) newWidth / (double) originalWidth);
        double scale = Math.min(hRatio, wRatio);
        final ParameterBlock parameterBlock = new ParameterBlock();
        parameterBlock.addSource(originalImage);
        parameterBlock.add(scale);
        parameterBlock.add(scale);
        parameterBlock.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));
        final RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        final PlanarImage newImage = JAI.create("SubsampleAverage", parameterBlock, renderingHints);
        OutputStream outputStream = new FileOutputStream(resized);
        JAI.create("encode", newImage, outputStream, "PNG", null);
        outputStream.close();
    }

    public boolean checkToolkit() {
        try {
            Toolkit.getDefaultToolkit();
        } catch (Throwable e) {
            logger.error("Unable to acquire AWT default toolkit - thumbnails will not be displayed. Check DISPLAY variable or use setting -Djava.awt.headless=true.", e);
            return false;
        }
        return true;
    }

    public Thumbnail retrieveOrCreateThumbNail(File originalFile, File thumbnailFile, int maxWidth, int maxHeight) throws MalformedURLException {
        FileInputStream originalFileStream = null;
        try {
            Thumbnail thumbnail = null;
            try {
                originalFileStream = new FileInputStream(originalFile);
                thumbnail = retrieveOrCreateThumbNail(((InputStream) (originalFileStream)), originalFile.getName(), thumbnailFile, maxWidth, maxHeight);
            } catch (FileNotFoundException e) {
                logger.error("Unable to create thumbnail: file not found: " + originalFile.getAbsolutePath());
                return null;
            }
            return thumbnail;
        } finally {
            try {
                originalFileStream.close();
            } catch (IOException e) {
                logger.warn("io exception caught", e);
            }
        }
    }

    public Thumbnail retrieveOrCreateThumbNail(InputStream originalFileStream, String fileName, File thumbnailFile, int maxWidth, int maxHeight) throws MalformedURLException {
        Thumbnail thumbnail = getThumbnail(thumbnailFile, fileName);
        if (thumbnail == null) try {
            thumbnail = createThumbnail(originalFileStream, thumbnailFile, maxWidth, maxHeight, fileName);
        } catch (Throwable e) {
            logger.error("Unable to create thumbnail image of attachment", e);
            return null;
        }
        return thumbnail;
    }

    private BufferedImage scaleImage(Image originalImage, int maxWidth, int maxHeight) {
        WidthHeightHelper widthHeightHelper = determineScaleSize(maxWidth, maxHeight, originalImage);
        BufferedImage thumbnailImage = new BufferedImage(widthHeightHelper.getWidth(), widthHeightHelper.getHeight(), 1);
        Graphics2D graphics2D = thumbnailImage.createGraphics();
        Map<Key, Object> hints = Maps.newHashMap();
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2D.setRenderingHints(hints);
        graphics2D.setBackground(Color.white);
        graphics2D.fillRect(0, 0, widthHeightHelper.getWidth(), widthHeightHelper.getHeight());
        graphics2D.drawImage(originalImage, 0, 0, widthHeightHelper.getWidth(), widthHeightHelper.getHeight(), null);
        return thumbnailImage;
    }

    private WidthHeightHelper determineScaleSize(int maxWidth, int maxHeight, Image image) {
        return determineScaleSize(maxWidth, maxHeight, image.getWidth(null), image.getHeight(null));
    }

    private Thumbnail createThumbnail(InputStream originalFile, File thumbnailFile, int maxWidth, int maxHeight, String fileName) throws IOException, FileNotFoundException {
        Image originalImage = getImage(originalFile);
        BufferedImage scaledImage = scaleImage(originalImage, maxWidth, maxHeight);
        int height = scaledImage.getHeight();
        int width = scaledImage.getWidth();
        storeThumbImage(scaledImage, thumbnailFile);
        return new Thumbnail(height, width, fileName);
    }

    private Thumbnail getThumbnail(File thumbnailFile, String filename) throws MalformedURLException {
        if (thumbnailFile.exists()) {
            Image thumbImage = getImage(thumbnailFile);
            return new Thumbnail(thumbImage.getHeight(null), thumbImage.getWidth(null), filename);
        } else {
            return null;
        }
    }

    public Image getImage(File file) throws MalformedURLException {
        Image image = Toolkit.getDefaultToolkit().getImage(getUrl(file));
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
            logger.error("Unsuccessful attempt to load image with fileName: " + file.getName(), e);
        }
        return image;
    }

    private URL getUrl(File file) throws MalformedURLException {
        try {
            return file.toURI().toURL();
        } catch (NoSuchMethodError e) {
            return file.toURL();
        }
    }

    public Image getImage(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = 0;
        byte buf[] = new byte[1024];
        while ((i = is.read(buf)) != -1) out.write(buf, 0, i);
        byte fileBytes[] = out.toByteArray();
        Image image = Toolkit.getDefaultToolkit().createImage(fileBytes);
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
            logger.error("Unsuccessful attempt to load image input stream from webdav", e);
        }
        return image;
    }

    public void setEncodingQuality(float f) {
        if (f > 1.0F || f < 0.0F) {
            throw new IllegalArgumentException("Invalid quality setting '" + f + "', value must be between 0 and 1. ");
        } else {
            encodingQuality = f;
            return;
        }
    }

    private void storeThumbImage(BufferedImage scaledImage, File thumbnailFile) throws FileNotFoundException {
        OutputStream fout = new BufferedOutputStream(new FileOutputStream(thumbnailFile));
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(fout);
        JPEGEncodeParam encodeParams = encoder.getDefaultJPEGEncodeParam(scaledImage);
        encodeParams.setQuality(encodingQuality, false);
        encoder.setJPEGEncodeParam(encodeParams);
        try {
            encoder.encode(scaledImage);
            fout.close();
        } catch (IOException e) {
            logger.error("Error encoding the thumbnail image of attachment.", e);
        }
    }

    public WidthHeightHelper determineScaleSize(int maxWidth, int maxHeight, int imageWidth, int imageHeight) {
        if (maxHeight > imageHeight && maxWidth > imageWidth) return new WidthHeightHelper(imageWidth, imageHeight);
        double thumbRatio = (double) maxWidth / (double) maxHeight;
        double imageRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < imageRatio) return new WidthHeightHelper(maxWidth, (int) ((double) maxWidth / imageRatio)); else return new WidthHeightHelper((int) ((double) maxHeight * imageRatio), maxHeight);
    }

    public boolean isFileSupportedImage(File file) {
        try {
            return isFileSupportedImage(((InputStream) (new FileInputStream(file))));
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    public boolean isFileSupportedImage(InputStream inputStream) {
        boolean flag;
        try {
            imageInfo.setInput(inputStream);
            imageInfo.isValidImage();
            flag = THUMBNAIL_FORMATS.contains(imageInfo.getFormatName());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
                logger.error("io exception caught", e);
            }
        }
        return flag;
    }
}
