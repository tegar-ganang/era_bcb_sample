package edu.columbia.hypercontent.util;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import java.awt.image.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.color.ColorSpace;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import edu.columbia.hypercontent.vocabulary.HyC;
import edu.columbia.hypercontent.util.codec.GIFImageDecoder;
import edu.columbia.hypercontent.util.codec.GIFImageEncoder;
import edu.columbia.hypercontent.util.codec.NeuQuant;
import edu.columbia.hypercontent.ContentTypes;
import edu.columbia.filesystem.File;
import edu.columbia.filesystem.FileSystemException;
import org.apache.commons.codec.binary.Base64;
import org.apache.batik.ext.awt.image.codec.PNGImageEncoder;
import org.apache.batik.ext.awt.image.codec.PNGImageDecoder;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: Jan 13, 2004
 * Time: 3:57:14 PM
 * To change this template use Options | File Templates.
 */
public class ImageUtil {

    public static final String JPEG_FORMAT = "jpg";

    public static final String GIF_FORMAT = "gif";

    public static final String PNG_FORMAT = "png";

    public static final String THUMBNAIL_FORMAT = "thumbnail";

    static ColorConvertOp convert = new ColorConvertOp(null);

    static ColorModel aRGB = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);

    static ColorModel RGB = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x00000000);

    static ColorModel Gray = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

    static ColorModel aGray = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false, ColorModel.BITMASK, DataBuffer.TYPE_BYTE);

    static Reader[] readers = new Reader[] { new JPEGReader(), new GIFReader(), new PNGReader() };

    public static BufferedImage scaleToFactor(BufferedImage image, double scalingFactor, boolean smooth) {
        int w = (int) Math.round(scalingFactor * image.getWidth());
        int h = (int) Math.round(scalingFactor * image.getHeight());
        return scaleToSize(image, w, h, smooth);
    }

    public static Reader getReaderForMimeType(String mimetype) {
        for (int i = 0; i < readers.length; i++) {
            if (ContentTypes.areEquivalent(mimetype, readers[i].getMimeType())) {
                return readers[i];
            }
        }
        return null;
    }

    public static BufferedImage read(File file) throws FileSystemException, IOException {
        return read(new BufferedInputStream(file.getInputStream()), ContentTypes.getContentType(file.getName()));
    }

    public static BufferedImage read(java.io.File file) throws IOException {
        return read(new BufferedInputStream(new FileInputStream(file)), ContentTypes.getContentType(file.getName()));
    }

    public static BufferedImage read(InputStream stream, String contentType) throws IOException {
        try {
            Reader reader = getReaderForMimeType(contentType);
            if (reader == null && !contentType.startsWith("image/")) {
                reader = getReaderForMimeType("image/" + contentType);
            }
            if (reader != null) {
                BufferedImage image = reader.read(stream);
                return image;
            }
            return null;
        } finally {
            stream.close();
        }
    }

    public static Writer getWriterForMimeType(String mimetype) {
        if (ContentTypes.areEquivalent("image/jpeg", mimetype)) {
            return new JPEGWriter();
        } else if (ContentTypes.areEquivalent("image/gif", mimetype)) {
            return new GIFWriter();
        } else if (ContentTypes.areEquivalent("image/png", mimetype)) {
            return new PNGWriter();
        }
        return null;
    }

    public static BufferedImage scaleToSize(BufferedImage image, int size, boolean smooth) {
        int w = image.getWidth();
        int h = image.getHeight();
        int dim = (w > h) ? w : h;
        if (dim != size) {
            double scale = (double) size / dim;
            return scaleToFactor(image, scale, smooth);
        }
        return image;
    }

    public static BufferedImage scaleToSize(BufferedImage image, int width, int height, boolean smooth) {
        BufferedImage scaleme = image;
        if (!isRGB(scaleme) || isIndexed(image)) {
            scaleme = convertToRGB(image, true);
        }
        GraphicsConfiguration gc = scaleme.createGraphics().getDeviceConfiguration();
        int transparency = image.getColorModel().getTransparency();
        BufferedImage result = gc.createCompatibleImage(width, height, transparency);
        Graphics2D g2 = result.createGraphics();
        if (image.getWidth() > width) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        if (smooth) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        g2.drawImage(scaleme, 0, 0, width, height, null);
        g2.dispose();
        image.flush();
        result.flush();
        if (isGrayscale(image)) {
            result = convertToGrayscale(result, true);
        } else if (isIndexed(image)) {
            IndexColorModel icm = (IndexColorModel) image.getColorModel();
            int palletteSize = icm.getMapSize();
            Color trans = null;
            if (icm.getTransparentPixel() >= 0) {
                trans = new Color(icm.getRGB(icm.getTransparentPixel()));
            }
            result = convertToIndexed(result, palletteSize, trans);
        }
        return result;
    }

    protected static void printImageProfile(RenderedImage image) {
        StringBuffer sb = new StringBuffer();
        sb.append("Width ").append(image.getWidth() + "; ").append("Height ");
        sb.append(image.getHeight() + "; ").append("RGB ").append(isRGB(image) + "; ");
        sb.append("Grayscale ").append(isGrayscale(image) + "; ");
        sb.append("Indexed ").append(isIndexed(image) + "; ");
        sb.append("Alpha ").append(hasAlpha(image) + "");
        System.out.println(sb.toString());
    }

    public static BufferedImage convertToIndexed(BufferedImage image, int paletteSize, Color transparent) {
        boolean isIndexed = image.getColorModel() instanceof IndexColorModel;
        BufferedImage result = image;
        if (!isIndexed || (((IndexColorModel) image.getColorModel()).getMapSize() > paletteSize)) {
            BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D gr = temp.createGraphics();
            gr.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            gr.drawImage(image, 0, 0, null);
            byte[] pixels = ((DataBufferByte) temp.getRaster().getDataBuffer()).getData();
            int nPix = pixels.length / 3;
            int[] indexedPixels = new int[nPix];
            NeuQuant nq = new NeuQuant(pixels, pixels.length, 10, paletteSize);
            gr.dispose();
            byte[] colorTab = nq.process();
            boolean[] usedEntry = new boolean[paletteSize];
            for (int i = 0; i < colorTab.length; i += 3) {
                byte b = colorTab[i];
                colorTab[i] = colorTab[i + 2];
                colorTab[i + 2] = b;
                usedEntry[i / 3] = false;
            }
            int k = 0;
            for (int i = 0; i < nPix; i++) {
                int index = nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
                usedEntry[index] = true;
                indexedPixels[i] = (byte) index;
            }
            int bits = (int) Math.ceil(Math.log(paletteSize) / Math.log(2));
            int trans = -1;
            if (isIndexed && (transparent == null)) {
                IndexColorModel icm = (IndexColorModel) image.getColorModel();
                transparent = new Color(icm.getRGB(icm.getTransparentPixel()));
            }
            if (transparent != null) {
                int r = transparent.getRed();
                int g = transparent.getGreen();
                int b = transparent.getBlue();
                int dmin = 256 * 256 * 256;
                int len = colorTab.length;
                for (int i = 0; i < len; ) {
                    int dr = r - (colorTab[i++] & 0xff);
                    int dg = g - (colorTab[i++] & 0xff);
                    int db = b - (colorTab[i] & 0xff);
                    int d = dr * dr + dg * dg + db * db;
                    int index = i / 3;
                    if (usedEntry[index] && (d < dmin)) {
                        dmin = d;
                        trans = index;
                    }
                    i++;
                }
            }
            IndexColorModel model = new IndexColorModel(bits, colorTab.length / 3, colorTab, 0, false, trans);
            Color t = null;
            if (model.getTransparentPixel() >= 0) {
                t = new Color(model.getRGB(model.getTransparentPixel()));
            }
            image.flush();
            result = new BufferedImage(image.getWidth(), image.getHeight(), image.TYPE_BYTE_INDEXED, model);
            result.getRaster().setPixels(0, 0, image.getWidth(), image.getHeight(), indexedPixels);
        }
        return result;
    }

    public static boolean isRGB(RenderedImage image) {
        return image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB;
    }

    public static boolean isGrayscale(RenderedImage image) {
        return image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY;
    }

    public static boolean isIndexed(RenderedImage image) {
        return (image.getColorModel() instanceof IndexColorModel);
    }

    public static boolean hasAlpha(RenderedImage image) {
        return image.getColorModel().hasAlpha();
    }

    public static BufferedImage convertToGrayscale(BufferedImage image, boolean keepAlpha) {
        if (!isGrayscale(image) || isIndexed(image) || (!keepAlpha && hasAlpha(image))) {
            ColorModel cm = (keepAlpha && hasAlpha(image)) ? aGray : Gray;
            BufferedImage result = convert.createCompatibleDestImage(image, cm);
            result = convert.filter(image, result);
            return result;
        }
        return image;
    }

    public static BufferedImage convertToRGB(BufferedImage image, boolean keepAlpha) {
        if (!isRGB(image) || isIndexed(image) || (!keepAlpha && hasAlpha(image))) {
            ColorModel cm = (keepAlpha && hasAlpha(image)) ? aRGB : RGB;
            BufferedImage temp = convert.createCompatibleDestImage(image, cm);
            temp = convert.filter(image, temp);
            BufferedImage result = temp;
            if (isIndexed(image)) {
                IndexColorModel icm = (IndexColorModel) image.getColorModel();
                int trans = icm.getTransparentPixel();
                if (trans >= 0) {
                    result = convert.createCompatibleDestImage(image, cm);
                    Graphics2D g2d = result.createGraphics();
                    Color c = new Color(icm.getRGB(trans));
                    g2d.setColor(c);
                    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                    g2d.drawImage(temp, 0, 0, null);
                    g2d.dispose();
                }
            }
            image.flush();
            temp.flush();
            return result;
        }
        return image;
    }

    public static void addThumbnail(BufferedImage image, Resource metadata) throws IOException {
        Writer writer = getThumbnailWriter();
        BufferedImage thumage = scaleToSize(image, 128, true);
        byte[] thumbnail = writer.getBytes(thumage);
        String encoded = encodeBytes(thumbnail);
        if (metadata.hasProperty(HyC.Thumbnail)) {
            removeThumbnail(metadata);
        }
        Resource thumb = metadata.getModel().createResource();
        thumb.addProperty(HyC.data, encoded);
        thumb.addProperty(HyC.width, thumage.getWidth());
        thumb.addProperty(HyC.height, thumage.getHeight());
        thumb.addProperty(HyC.format, writer.getMimeType());
        metadata.addProperty(HyC.Thumbnail, thumb);
    }

    public static Writer getThumbnailWriter() {
        JPEGWriter writer = new JPEGWriter();
        writer.quality = 0.83f;
        return writer;
    }

    public static void removeThumbnail(Resource metadata) {
        if (metadata.hasProperty(HyC.Thumbnail)) {
            Resource thumb = metadata.getProperty(HyC.Thumbnail).getResource();
            thumb.removeProperties();
            metadata.removeAll(HyC.Thumbnail);
        }
    }

    public static byte[] getRawThumbnail(Resource metadata) {
        byte[] b = new byte[0];
        if (metadata.hasProperty(HyC.Thumbnail)) {
            b = decodeBytes(metadata.getProperty(HyC.Thumbnail).getResource().getProperty(HyC.data).getString());
        }
        return b;
    }

    public static BufferedImage getThumbnail(Resource metadata) throws IOException {
        if (metadata.hasProperty(HyC.Thumbnail)) {
            InputStream data = new ByteArrayInputStream(getRawThumbnail(metadata));
            String type = metadata.getProperty(HyC.Thumbnail).getResource().getProperty(HyC.format).getString();
            return read(data, type);
        }
        return null;
    }

    protected static final String encodeBytes(byte[] bytes) {
        byte[] encoded = Base64.encodeBase64(bytes);
        return new String(encoded);
    }

    protected static final byte[] decodeBytes(String input) {
        return Base64.decodeBase64(input.getBytes());
    }

    public static void main(String[] args) {
        try {
            String file = "/Volumes/Users/alex/Desktop/image test/test.jpg";
            for (int i = 0; i < 1000; i++) {
                try {
                    BufferedImage image = read(new java.io.File(file));
                    image = convertToIndexed(image, 256, null);
                    image = convertToRGB(image, false);
                    image = scaleToFactor(image, .5, true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public abstract static class Reader {

        public abstract String getMimeType();

        public abstract BufferedImage read(InputStream in) throws IOException;
    }

    public static class GIFReader extends Reader {

        public String getMimeType() {
            return "image/gif";
        }

        public BufferedImage read(InputStream in) throws IOException {
            GIFImageDecoder decoder = new GIFImageDecoder(in, null);
            return convertRenderedImage(decoder.decodeAsRenderedImage());
        }
    }

    public static class JPEGReader extends Reader {

        public String getMimeType() {
            return "image/jpeg";
        }

        public BufferedImage read(InputStream in) throws IOException {
            JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
            return decoder.decodeAsBufferedImage();
        }
    }

    public static class PNGReader extends Reader {

        public String getMimeType() {
            return "image/png";
        }

        public BufferedImage read(InputStream in) throws IOException {
            PNGImageDecoder decoder = new PNGImageDecoder(in, null);
            return convertRenderedImage(decoder.decodeAsRenderedImage());
        }
    }

    public static BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    public abstract static class Writer {

        public abstract String getExtension();

        public abstract String getMimeType();

        public abstract void write(BufferedImage image, OutputStream out) throws IOException;

        public byte[] getBytes(BufferedImage image) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            write(image, output);
            return output.toByteArray();
        }
    }

    public static class GIFWriter extends Writer {

        public String getExtension() {
            return "gif";
        }

        public String getMimeType() {
            return "image/gif";
        }

        protected String format = "gif";

        int paletteSize = 256;

        Color transparent = null;

        public void write(BufferedImage image, OutputStream out) throws IOException {
            image = convertToIndexed(image, paletteSize, transparent);
            GIFImageEncoder encoder = new GIFImageEncoder(image, out);
            encoder.encode();
        }
    }

    public static class JPEGWriter extends Writer {

        public String getExtension() {
            return "jpg";
        }

        public String getMimeType() {
            return "image/jpeg";
        }

        public float quality = 1.0f;

        protected String format = "jpg";

        public void write(BufferedImage image, OutputStream out) throws IOException {
            if (isGrayscale(image)) {
                image = convertToGrayscale(image, false);
            } else {
                image = convertToRGB(image, false);
            }
            JPEGEncodeParam param = JPEGCodec.getDefaultJPEGEncodeParam(image);
            param.setQuality(quality, false);
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out, param);
            encoder.encode(image);
        }
    }

    public static class PNGWriter extends Writer {

        public String getExtension() {
            return "png";
        }

        public String getMimeType() {
            return "image/png";
        }

        protected String format = "png";

        public boolean indexed = false;

        public int paletteSize = 256;

        public void write(BufferedImage image, OutputStream out) throws IOException {
            if (!indexed) {
                if (isGrayscale(image)) {
                    image = convertToGrayscale(image, true);
                } else {
                    image = convertToRGB(image, true);
                }
            } else if (indexed) {
                image = convertToIndexed(image, paletteSize, null);
            }
            PNGImageEncoder encoder = new PNGImageEncoder(out, null);
            encoder.encode(image);
        }
    }
}
