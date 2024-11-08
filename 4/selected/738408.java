package fi.hip.gb.utils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * This is a class that offers functions for loading images.
 * 
 * @author Mika Kaki
 */
public class ImageUtils {

    private static final Component _component = new Container();

    private static final MediaTracker _tracker = new MediaTracker(_component);

    static {
        Frame f = new Frame();
        f.add(_component);
        f.pack();
    }

    public static void showImageInFrame(Image image, String title) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        String size = w + " x " + h;
        JFrame f = new JFrame("Image viewer: " + title);
        JLabel l = new JLabel(size, new ImageIcon(image), SwingConstants.LEFT);
        l.setHorizontalTextPosition(SwingConstants.LEFT);
        f.getContentPane().add(l);
        f.pack();
        f.setVisible(true);
    }

    /**
     * Returns the image that will be loaded from the gifFile. The example shows
     * how this function may be used: <br>
     * <code>Image i = loadResourceImage(getClass(), "gifs/anImage.gif")</code>
     * <br>
     * In the example the image will be retrieved from a file which is located
     * in a directory 'gifs' which is located in that directory where the class
     * is located. This method may be used in applets, since it does not use any
     * methods which would have restricted access for security reasons.
     * 
     * @param baseClass
     *            Base class whose resource is to be loaded.
     * 
     * @param imageFileName
     *            Name of the resource file.
     * @return An image which is stored in the 'imageFileName' or 'null' if an
     *         error is encountered.
     */
    public static Image loadResourceImage(Class baseClass, String imageFileName) {
        try {
            InputStream imageStream = baseClass.getResourceAsStream(imageFileName);
            Image i = loadImage(imageStream);
            imageStream.close();
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads the image from the given file. The format of the image must be one
     * of the formats that the toolkit supports, typically jpg or gif.
     * 
     * @param filename filename of the image
     * @return The loaded image or 'null' if an error was encountered.
     */
    public static Image loadImage(String filename) {
        try {
            InputStream in = new FileInputStream(filename);
            Image i = loadImage(in);
            in.close();
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads the image from the given stream. The format of the image must be
     * one of the formats that the toolkit supports, typically jpg or gif.
     * 
     * @param imageStream 
     * @return The loaded image or 'null' if an error was encountered.
     */
    public static Image loadImage(InputStream imageStream) {
        byte[] buffer = new byte[1024];
        try {
            BufferedInputStream in = new BufferedInputStream(imageStream);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            in.close();
            out.flush();
            buffer = out.toByteArray();
            if (buffer.length == 0) {
                System.err.println("warning: image is zero-length");
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Image image = Toolkit.getDefaultToolkit().createImage(buffer);
        if (image == null) return null;
        loadImage(image);
        return image;
    }

    /**
     * Saves the given image to the target stream using JPEG format. The quality
     * is a floating point value between 0.0 and 1.0. 0.0 meaning the lowes
     * quality and smalles file size.
     * @param img image to be saved
     * @param target output for the jpeg image
     * @param quality quality between 0 and 1
     * @throws IOException
     */
    public static void saveAsJPEG(Image img, OutputStream target, float quality) throws IOException {
        BufferedImage bImg = toJPEGStoreableBufferedImage(img);
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(target);
        JPEGEncodeParam param = null;
        try {
            param = encoder.getDefaultJPEGEncodeParam(bImg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (param != null) {
            param.setQuality(quality, false);
            encoder.setJPEGEncodeParam(param);
        }
        encoder.encode(bImg);
    }

    public static Dimension getDimensionOfJPEG(String fileName) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(fileName));
            byte markerStart = in.readByte();
            byte markerEnd = in.readByte();
            int marker = ((markerStart << 8) | markerEnd);
            marker &= 0xffff;
            while (!(marker >= 0xffc0 && marker <= 0xffcf)) {
                markerStart = markerEnd;
                markerEnd = in.readByte();
                marker = ((markerStart << 8) | markerEnd);
                marker &= 0xffff;
            }
            short height = in.readShort();
            short width = in.readShort();
            in.close();
            return new Dimension(width, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns an integer array of pixel values of the given image. The array
     * will have size of image.width * image.height. The elements (integer
     * numbers) of the array (pixels) will contain four components of the color:
     * alpha, red, green and blue. Each value may be accessed as follows:
     * 
     * <pre>
     * int alpha = (pixel &gt;&gt; 24) &amp; 0xff;int red   = (pixel &gt;&gt; 16) &amp; 0xff;
     *  int green = (pixel &gt;&gt;  8) &amp; 0xff;
     *  int blue  = (pixel      ) &amp; 0xff;
     *  
     * </pre>
     * @param image
     * @return pixel array
     */
    public static int[] getPixels(Image image) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        int[] pixels = new int[w * h];
        PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            System.err.println("image fetch aborted or errored");
            return null;
        }
        return pixels;
    }

    /**
     * Creates an empty image with the specified dimensions.
     * @param width width in pixels
     * @param height height in pixels
     * @return new image
     */
    public static Image createImage(int width, int height) {
        return _component.createImage(width, height);
    }

    /**
     * Creates an image out of an pixel array. This assumes RGB color model
     * where the value for each color is coded as follows:
     * 
     * <pre>
     * int alpha = (pixel &gt;&gt; 24) &amp; 0xff;int red   = (pixel &gt;&gt; 16) &amp; 0xff;
     *  int green = (pixel &gt;&gt;  8) &amp; 0xff;
     *  int blue  = (pixel      ) &amp; 0xff;
     *  
     * </pre>
     * @param width width in pixels
     * @param height height in pixels
     * @param pixels initial pixels
     * @return new image
     */
    public static Image createImageFrom(int width, int height, int[] pixels) {
        Image img = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(width, height, pixels, 0, width));
        loadImage(img);
        return img;
    }

    /**
     * Creates an image out of an array of gray scale pixel values. The
     * resulting image is a standard RGB image, but all the values are, of
     * course, grays.
     * @param width width in pixels
     * @param height height in pixels
     * @param pixels initial pixels
     * @return new image
     */
    public static Image createImageFrom(int width, int height, byte[] pixels) {
        int[] rgbPixels = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int rgb = 0;
            rgb |= (0xff << 24);
            rgb |= (pixel << 16) & (0xff << 16);
            rgb |= (pixel << 8) & (0xff << 8);
            rgb |= (pixel << 0) & (0xff << 0);
            rgbPixels[i] = rgb;
        }
        return createImageFrom(width, height, rgbPixels);
    }

    /**
     * Returns gray scale pixels of the given image. Each pixel has a value
     * between 0 and 255. The size of the resulting array is image.width *
     * image.height.
     * @param image original image
     * @return pixels of the gray scale version of the image
     */
    public static byte[] getGrayScalePixels(Image image) {
        int[] pixels = getPixels(image);
        byte[] result = new byte[pixels.length];
        for (int i = 0; i < result.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;
            int value = (int) ((double) (red + green + blue) / 3d);
            result[i] = (byte) value;
        }
        return result;
    }

    /**
     * Tresholds the given array of pixels so that the pixel value is set to 255
     * if the original value of it is between lowerLimit and upperLimit.
     * Otherwise the
     * 
     * pixel is set to 0.
     * @param pixels
     * @param lowerLimit
     * @param upperLimit
     */
    public static void tresholdGrayScalePixels(byte[] pixels, int lowerLimit, int upperLimit) {
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i] & 0xff;
            if (value >= lowerLimit && value <= upperLimit) pixels[i] = (byte) 255; else pixels[i] = 0;
        }
    }

    /**
     * Crops the image using the given coordinates. Returns a new image instance
     * containing the selected pixels. The original image remains actually
     * untouched.
     * @param  image
     * @param x left x cordinate
     * @param w width of the cropped image
     * @param y left y cordinate
     * @param h height of the cropped image
     * @return new image
     */
    public static Image cropImage(Image image, int x, int w, int y, int h) {
        int width = image.getWidth(null);
        int pixels[] = getPixels(image);
        int result[] = new int[w * h];
        for (int r = y; r < y + h; r++) {
            for (int c = x; c < x + w; c++) {
                int ri = ((r - y) * w) + (c - x);
                result[ri] = pixels[r * width + c];
            }
        }
        return createImageFrom(w, h, result);
    }

    /**
     * Scales the given image to the given size. If the new size does not fit
     * the propotions of the original picture, then the propotions will be
     * distorted. Returns a new image, with the specified dimensions.
     * @param image
     * @param width
     * @param height
     * @return scaled image
     */
    public static Image scaleImage(Image image, int width, int height) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        double scalingX = (double) width / (double) w;
        double scalingY = (double) height / (double) h;
        return scaleImage(image, scalingX, scalingY);
    }

    /**
     * Scales the given image to relative new size. If the new size does not fit
     * the propotions of the original picture, then the propotions will be
     * distorted. Returns a new image instance, whose dimensions are:
     * image.width * scalingX = width image.height * scalingY = height.
     * @param image
     * @param scalingX
     * @param scalingY
     * @return scaled image
     */
    public static Image scaleImage(Image image, double scalingX, double scalingY) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        AffineTransform at = AffineTransform.getScaleInstance(scalingX, scalingY);
        Image result = _component.createImage((int) (width * scalingX), (int) (height * scalingY));
        Graphics2D g = (Graphics2D) result.getGraphics();
        g.drawImage(image, at, null);
        g.dispose();
        return result;
    }

    /**
     * Load the specified image using a media tracker. This function will wait for
     * the image at most 10 secs.
     * @param image image to be loaded
     **/
    private static void loadImage(Image image) {
        synchronized (_tracker) {
            _tracker.addImage(image, 0);
            try {
                _tracker.waitForID(0, 10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _tracker.removeImage(image, 0);
        }
    }

    /**
     * 
     * @param img
     * @return
     */
    private static BufferedImage toJPEGStoreableBufferedImage(Image img) {
        if (img instanceof BufferedImage && ((BufferedImage) img).getType() == BufferedImage.TYPE_3BYTE_BGR) return (BufferedImage) img;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = result.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        loadImage(result);
        return result;
    }
}
