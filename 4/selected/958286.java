package sun.awt.qt;

import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import java.awt.image.*;
import java.io.*;
import sun.awt.image.*;

/**
 * <code>QtImageDecoder</code> uses QT's native decoders to decode image
 * formats (GIF, JPEG, PNG). An instance is instantiated by 
 * <code>QtImageDecoderFactory</code>. This class is optimized to load images
 * faster by utilizing the QT's native decoders. If the applications performs
 * image manipulation then the performance is no better than the java decoders.
 * We acheive high performance by allowing the QT's native decoders to
 * create a QImage, which we then pass it to QtImageRepresentation. We
 * eliminate decoding in java as well as calling <code>set*Pixels()</code>,
 * which gives us a significant improvemement in the image load time.
 */
class QtImageDecoder extends ImageDecoder {

    private static final DirectColorModel RGB24_DCM = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);

    private static final DirectColorModel RGB32_DCM = new DirectColorModel(32, 0xff000000, 0xff0000, 0xff00, 0xff);

    /**
     * Image format. This is one of the constants defined in
     * <code>sun.awt.image.ImageDecoderFactory</code>
     */
    protected String imgFormat;

    /**
     * Width of the image 
     */
    protected int width;

    /**
     * Height of the image
     */
    protected int height;

    /**
     * The color model used by the image 
     */
    protected ColorModel colorModel;

    static {
        initIDs();
    }

    QtImageDecoder(InputStreamImageSource src, InputStream is, String imgFormat) {
        super(src, is);
        this.imgFormat = imgFormat;
        this.width = -1;
        this.height = -1;
        this.colorModel = null;
    }

    public boolean sendPixels(int pixels[], int y) {
        int count = setPixels(0, y, pixels.length, 1, this.colorModel, pixels, 0, pixels.length);
        if (count <= 0) {
            aborted = true;
        }
        return !aborted;
    }

    public boolean sendPixels(byte pixels[], int y) {
        int count = setPixels(0, y, pixels.length, 1, this.colorModel, pixels, 0, pixels.length);
        if (count <= 0) {
            aborted = true;
        }
        return !aborted;
    }

    /**
     * Produce an image from the stream.
     */
    public void produceImage() throws IOException, ImageFormatException {
        int imageHandle = 0;
        try {
            byte[] buffer = getEncodedImage();
            imageHandle = decodeImage(buffer, buffer.length);
            if (imageHandle == 0) throw new ImageFormatException("Qt Image decoding error");
            if (!aborted) {
                setDimensions(this.width, this.height);
                setColorModel(this.colorModel);
                int count = imageComplete(imageHandle, ImageConsumer.STATICIMAGEDONE, true);
                if (count > 0) {
                    if (this.colorModel.getPixelSize() == 8) {
                        byte[] pixels = new byte[this.width];
                        for (int y = 0; y < this.height; y++) {
                            getPixels(imageHandle, y, pixels);
                            sendPixels(pixels, y);
                        }
                    } else {
                        int[] pixels = new int[this.width];
                        for (int y = 0; y < this.height; y++) {
                            getPixels(imageHandle, y, pixels);
                            sendPixels(pixels, y);
                        }
                    }
                }
                imageComplete(ImageConsumer.STATICIMAGEDONE, true);
            }
        } catch (IOException e) {
            if (!aborted) {
                throw e;
            }
        } finally {
            close();
            if (imageHandle != 0) disposeImage(imageHandle);
        }
    }

    /**
     * Creates a byte array of the encoded/compressed image stream
     */
    protected byte[] getEncodedImage() throws IOException {
        BufferedInputStream bis = null;
        if (input instanceof BufferedInputStream) bis = (BufferedInputStream) input; else bis = new BufferedInputStream(input);
        byte[] buffer = null;
        int bytes_read = 0;
        int bytes_available = 0;
        ByteArrayOutputStream baos = null;
        while ((bytes_available = bis.available()) > 0) {
            if (buffer != null) {
                if (baos == null) {
                    baos = new ByteArrayOutputStream(bytes_read + bytes_available);
                }
                baos.write(buffer, 0, bytes_read);
            }
            buffer = new byte[bytes_available];
            bytes_read = bis.read(buffer);
            if (bytes_read == -1) {
                break;
            }
        }
        if (baos != null) {
            if (bytes_read > 0) {
                baos.write(buffer, 0, bytes_read);
            }
            buffer = baos.toByteArray();
        }
        return buffer;
    }

    protected int setPixels(int x, int y, int w, int h, ColorModel model, byte pix[], int off, int scansize) {
        source.latchConsumers(this);
        ImageConsumerQueue cq = null;
        int count = 0;
        ImageConsumer consumer = null;
        while ((cq = nextConsumer(cq)) != null) {
            consumer = cq.getConsumer();
            if (!(consumer instanceof QtImageRepresentation)) {
                consumer.setPixels(x, y, w, h, model, pix, off, scansize);
                count++;
            }
        }
        return count;
    }

    protected int setPixels(int x, int y, int w, int h, ColorModel model, int pix[], int off, int scansize) {
        source.latchConsumers(this);
        ImageConsumerQueue cq = null;
        int count = 0;
        ImageConsumer consumer = null;
        while ((cq = nextConsumer(cq)) != null) {
            consumer = cq.getConsumer();
            if (!(consumer instanceof QtImageRepresentation)) {
                consumer.setPixels(x, y, w, h, model, pix, off, scansize);
                count++;
            }
        }
        return count;
    }

    protected int imageComplete(int imageHandle, int status, boolean done) {
        source.latchConsumers(this);
        if (done) {
            finished = true;
            source.doneDecoding(this);
        }
        ImageConsumerQueue cq = null;
        int count = 0;
        ImageConsumer consumer = null;
        while ((cq = nextConsumer(cq)) != null) {
            consumer = cq.getConsumer();
            if (consumer instanceof QtImageRepresentation) {
                ((QtImageRepresentation) consumer).setNativeImage(imageHandle);
                consumer.imageComplete(status);
            } else {
                count++;
            }
        }
        return count;
    }

    protected int imageComplete(int status, boolean done) {
        source.latchConsumers(this);
        if (done) {
            finished = true;
            source.doneDecoding(this);
        }
        ImageConsumerQueue cq = null;
        int count = 0;
        ImageConsumer consumer = null;
        while ((cq = nextConsumer(cq)) != null) {
            consumer = cq.getConsumer();
            if (!(consumer instanceof QtImageRepresentation)) {
                consumer.imageComplete(status);
                count++;
            }
        }
        return count;
    }

    /**
     * Decode the image data and return a native representation of the
     * decoded pixels. The method should also set the basic attributes of
     * the image like image dimensions and color model.
     *
     * @param data encoded image stream
     * @param length number of bytes in the image stream, that should be 
     *        used to decode
     *
     * @return a handle that represents the decoded image
     */
    private native int decodeImage(byte[] data, int length);

    /**
     * Disposes the refernce to the image handle. This should freeup any
     * resources allocated on the native side.
     *
     * @param imageHandle image handle
     */
    private native void disposeImage(int imageHandle);

    /**
     * Get the image pixels. This is called for indexed color model images
     *
     * @param imageHandle image handle
     * @param y the scan line. This should be a zero based index and should be
     *        less than the height of the image
     * @param pixels array to hold the pixels. This length of the should be
     *        atleast the image's width.
     */
    private static native void getPixels(int imageHandle, int y, byte[] pixels);

    /**
     * Get the image pixels. This is called for direct color model images
     *
     * @param imageHandle image handle
     * @param y the scan line. This should be a zero based index and should be
     *        less than the height of the image
     * @param pixels array to hold the pixels. This length of the should be
     *        atleast the image's width.
     */
    private static native void getPixels(int imageHandle, int y, int[] pixels);

    /**
     * Perform class initializations
     */
    private static native void initIDs();
}
