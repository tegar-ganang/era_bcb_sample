package org.jpedal.color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import org.jpedal.utils.LogWriter;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

/**
 * handle LabColorSpace
 */
public class LabColorSpace extends GenericColorSpace {

    private int r, g, b;

    private float lastL = -1, lastA = 65536, lastBstar;

    /**holds values we have already calculated to speed-up*/
    private Hashtable cache = new Hashtable();

    private final float C1 = 108f / 841f;

    private final float C2 = 4f / 29f;

    private final float C3 = 6f / 29f;

    private final float C4 = 100f / 255f;

    private final float C5 = 128f;

    public LabColorSpace(String whitepoint, String blackpoint, String range) {
        value = ColorSpaces.Lab;
        setCIEValues(whitepoint, blackpoint, range, null, null);
    }

    /**
	 * convert Index to RGB
	  */
    public byte[] convertIndexToRGB(byte[] index) {
        int size = index.length;
        float cl, ca, cb;
        for (int i = 0; i < size; i = i + 3) {
            cl = (index[i] & 255) * C4;
            ca = (index[i + 1] & 255) - C5;
            cb = (index[i + 2] & 255) - C5;
            convertToRGB(cl, ca, cb);
            index[i] = (byte) r;
            index[i + 1] = (byte) g;
            index[i + 2] = (byte) b;
        }
        return index;
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
    public BufferedImage JPEGToRGBImage(byte[] data, int w, int h, String decodeArray) {
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
                float cl = db.getElemFloat(i) * C4;
                float ca = db.getElemFloat(i + 1) - C5;
                float cb = db.getElemFloat(i + 2) - C5;
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
	 * convert LAB stream to RGB and return as an image
	  */
    public BufferedImage dataToRGB(byte[] data, int width, int height) {
        BufferedImage image = null;
        DataBuffer db = new DataBufferByte(data, data.length);
        final int imgSize = width * height;
        try {
            for (int i = 0; i < imgSize * 3; i = i + 3) {
                float cl = db.getElemFloat(i) * C4;
                float ca = db.getElemFloat(i + 1) - C5;
                float cb = db.getElemFloat(i + 2) - C5;
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

    /**convert numbers to rgb values*/
    private void convertToRGB(float l, float a, float bstar) {
        if (l < 0) l = 0; else if (l > 100) l = 100;
        if (a < R[0]) a = R[0]; else if (a > R[1]) a = R[1];
        if (bstar < R[2]) bstar = R[2]; else if (bstar > R[3]) bstar = R[3];
        if ((lastL == l) && (lastA == a) && (lastBstar == bstar)) {
        } else {
            int indexL = (int) l;
            int indexA = (int) (a - R[0]);
            int indexB = (int) (bstar - R[2]);
            Integer key = new Integer((indexL << 16) + (indexA << 8) + indexB);
            Object value = cache.get(key);
            if (value != null) {
                int raw = ((Integer) value).intValue();
                r = ((raw >> 16) & 255);
                g = ((raw >> 8) & 255);
                b = ((raw) & 255);
            } else {
                double val1 = (l + 16d) / 116d;
                double[] vals = new double[3];
                vals[0] = val1 + (a / 500d);
                vals[1] = val1;
                vals[2] = val1 - (bstar / 200d);
                float[] out = new float[3];
                for (int j = 0; j < 3; j++) {
                    if (vals[j] >= C3) out[j] = (float) (W[j] * vals[j] * vals[j] * vals[j]); else out[j] = (float) (W[j] * C1 * (vals[j] - C2));
                }
                out = cs.toRGB(out);
                r = (int) (out[0] * 255);
                g = (int) (out[1] * 255);
                b = (int) (out[2] * 255);
                if (r < 0) r = 0;
                if (g < 0) g = 0;
                if (b < 0) b = 0;
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                int raw = (r << 16) + (g << 8) + b;
                cache.put(key, new Integer(raw));
            }
            lastL = l;
            lastA = a;
            lastBstar = bstar;
        }
    }

    /**set color*/
    public final void setColor(String[] operand, int length) {
        float l = Float.parseFloat(operand[2]);
        float a = Float.parseFloat(operand[1]);
        float Bstar = Float.parseFloat(operand[0]);
        convertToRGB(l, a, Bstar);
        this.currentColor = new PdfColor(r, g, b);
    }
}
