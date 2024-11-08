package org.jpedal.color;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import org.jpedal.utils.LogWriter;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGCodec;

/**
 * handle CalRGBColorSpace
 */
public class CalRGBColorSpace extends GenericColorSpace {

    private int r, g, b;

    private final double[][] xyzrgb = { { 3.240449, -1.537136, -0.498531 }, { -0.969265, 1.876011, 0.041556 }, { 0.055643, -0.204026, 1.057229 } };

    /**cache for values to stop recalculation*/
    private float lastC = -255, lastI = -255, lastE = -255;

    public CalRGBColorSpace(String whitepoint, String blackpoint, String matrix, String gamma) {
        cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        setCIEValues(whitepoint, blackpoint, null, matrix, gamma);
        value = ColorSpaces.CalRGB;
    }

    /**
	 * convert to RGB and return as an image
	  */
    public final BufferedImage dataToRGB(byte[] data, int width, int height) {
        BufferedImage image = null;
        DataBuffer db = new DataBufferByte(data, data.length);
        int size = width * height;
        try {
            for (int i = 0; i < size * 3; i = i + 3) {
                float cl = db.getElemFloat(i);
                float ca = db.getElemFloat(i + 1);
                float cb = db.getElemFloat(i + 2);
                convertToRGB(cl, ca, cb);
                db.setElem(i, r);
                db.setElem(i + 1, g);
                db.setElem(i + 2, b);
            }
            int[] bands = { 0, 1, 2 };
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Raster raster = Raster.createInterleavedRaster(db, width, height, width * 3, 3, bands, null);
            image.setData(raster);
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
        }
        return image;
    }

    /**
	 * set CalRGB color (in terms of rgb)
	 */
    public final void setColor(String[] number_values, int items) {
        float[] A = { 1.0f, 1.0f, 1.0f };
        if (items == 3) {
            for (int i = 0; i < items; i++) {
                A[i] = Float.parseFloat(number_values[2 - i]);
                if (A[i] > 1) return;
            }
        }
        convertToRGB(A[0], A[1], A[2]);
        this.currentColor = new PdfColor(r, g, b);
    }

    private final void convertToRGB(float C, float I, float E) {
        if ((lastC == C) && (lastI == I) && (lastE == E)) {
        } else {
            r = (int) (C * 255);
            g = (int) (I * 255);
            b = (int) (E * 255);
            lastC = C;
            lastI = I;
            lastE = E;
        }
    }

    /**
     * <p>
     * Convert DCT encoded image bytestream to sRGB
     * </p>
     * <p>
     * It uses the internal Java classes and the Adobe icm to convert CMYK and
     * YCbCr-Alpha - the data is still DCT encoded.
     * </p>
     * <p>
     * The Sun class JPEGDecodeParam.java is worth examining because it contains
     * lots of interesting comments
     * </p>
     * <p>
     * I tried just using the new IOImage.read() but on type 3 images, all my
     * clipping code stopped working so I am still using 1.3
     * </p>
     */
    public BufferedImage JPEGToRGBImageXXX(byte[] data, int w, int h, String decodeArray) {
        BufferedImage image = null;
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(data);
            JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
            Raster currentRaster = decoder.decodeAsRaster();
            final int width = currentRaster.getWidth();
            final int height = currentRaster.getHeight();
            final int imgSize = width * height;
            DataBuffer db = currentRaster.getDataBuffer();
            for (int i = 0; i < imgSize * 3; i = i + 3) {
                float cl = db.getElemFloat(i) / 255;
                float ca = db.getElemFloat(i + 1) / 255;
                float cb = db.getElemFloat(i + 2) / 255;
                convertToRGB(cl, ca, cb);
                db.setElem(i, r);
                db.setElem(i + 1, g);
                db.setElem(i + 2, b);
            }
            int[] bands = { 0, 1, 2 };
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Raster raster = Raster.createInterleavedRaster(db, width, height, width * 3, 3, bands, null);
            image.setData(raster);
            in.close();
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
        }
        return image;
    }

    /**
	 * convert Index to RGB
	  */
    public final byte[] convertIndexToRGB(byte[] index) {
        return index;
    }

    /**convenience method used to check value within bounds*/
    private double clip(double value) {
        if (value < 0) value = 0;
        if (value > 1) value = 1;
        return value;
    }
}
