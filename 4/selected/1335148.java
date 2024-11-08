package org.jpedal.color;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.utils.LogWriter;

/**
 * handle ICCColorSpace
 */
public class ICCColorSpace extends GenericColorSpace {

    private int[] a1, b1, c1;

    public ICCColorSpace(PdfObjectReader currentPdfFile, String currentColorspace) {
        a1 = new int[256];
        b1 = new int[256];
        c1 = new int[256];
        for (int i = 0; i < 256; i++) {
            a1[i] = -1;
            b1[i] = -1;
            c1[i] = -1;
        }
        value = ColorSpaces.ICC;
        cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int pointer = currentColorspace.indexOf("/ICCBased");
        String object_ref = currentColorspace.substring(pointer + 9);
        pointer = object_ref.indexOf("]");
        if (pointer != -1) object_ref = object_ref.substring(0, pointer - 1);
        byte[] icc_data = currentPdfFile.readStream(object_ref.trim(), true);
        if (icc_data == null) LogWriter.writeLog("Error in reading ICC data with ref " + object_ref); else {
            try {
                cs = new ICC_ColorSpace(ICC_Profile.getInstance(icc_data));
            } catch (Exception e) {
                LogWriter.writeLog("[PDF] Problem " + e.getMessage() + " with ICC data ");
                failed = true;
            }
        }
        componentCount = cs.getNumComponents();
    }

    /**set color*/
    public final void setColor(String[] operand, int size) {
        float[] values = new float[size];
        int[] lookup = new int[size];
        for (int i = 0; i < size; i++) {
            float val = Float.parseFloat(operand[i]);
            values[size - i - 1] = val;
            lookup[size - i - 1] = (int) (val * 255);
        }
        if ((size == 3) && (a1[lookup[0]] != -1) && (b1[lookup[1]] != -1) && (c1[lookup[2]] != -1)) {
            currentColor = new PdfColor(a1[lookup[0]], b1[lookup[1]], c1[lookup[2]]);
        } else {
            values = cs.toRGB(values);
            currentColor = new PdfColor(values[0], values[1], values[2]);
            if (size == 3) {
                a1[lookup[0]] = (int) (values[0] * 255);
                b1[lookup[1]] = (int) (values[1] * 255);
                c1[lookup[2]] = (int) (values[2] * 255);
            }
        }
    }

    /**
	 * convert Index to RGB
	  */
    public byte[] convertIndexToRGB(byte[] data) {
        if (componentCount == 4) return convert4Index(data); else return data;
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
        int type = getJPEGTransform(data);
        if (this.componentCount == 3 && type == 0 && intent != null && intent.equals("/RelativeColorimetric")) {
            return algorithmicICCToRGB(data, w, h, false);
        } else return nonRGBJPEGToRGBImage(data, w, h, null);
    }

    public BufferedImage algorithmicICCToRGB(byte[] data, int w, int h, boolean debug) {
        BufferedImage image = null;
        ImageReader iir = null;
        ImageInputStream iin = null;
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        try {
            iir = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG").next();
            ImageIO.setUseCache(false);
            iin = ImageIO.createImageInputStream((in));
            iir.setInput(iin, true);
            Raster ras = iir.readRaster(0, null);
            DataBuffer buffer = ras.getDataBuffer();
            byte[] new_data = new byte[w * h * 3];
            int pixelCount = w * h * 3;
            float lastR = 0, lastG = 0, lastB = 0;
            int pixelReached = 0;
            float lastIn1 = -1, lastIn2 = -1, lastIn3 = -1;
            for (int i = 0; i < pixelCount; i = i + 3) {
                float in1 = ((buffer.getElemFloat(i))) / 255f;
                float in2 = ((buffer.getElemFloat(1 + i))) / 255f;
                float in3 = ((buffer.getElemFloat(2 + i))) / 255f;
                float[] outputValues = new float[3];
                if ((lastIn1 == in1) && (lastIn2 == in2) && (lastIn3 == in3)) {
                } else {
                    if (debug) System.out.println(in1 + " " + in2 + " " + in3);
                    float[] inputValues = { in1, in2, in3 };
                    outputValues = cs.toRGB(inputValues);
                    outputValues = inputValues;
                    lastR = (outputValues[0] * 255);
                    lastG = (outputValues[1] * 255);
                    lastB = (outputValues[2] * 255);
                    lastIn1 = in1;
                    lastIn2 = in2;
                    lastIn3 = in3;
                }
                new_data[pixelReached++] = (byte) lastR;
                new_data[pixelReached++] = (byte) lastG;
                new_data[pixelReached++] = (byte) lastB;
            }
            try {
                int[] bands = { 0, 1, 2 };
                DataBuffer db = new DataBufferByte(new_data, new_data.length);
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Raster raster = Raster.createInterleavedRaster(db, w, h, w * 3, 3, bands, null);
                image.setData(raster);
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
                LogWriter.writeLog("Exception " + e + " with 24 bit RGB image");
            }
        } catch (Exception e) {
        }
        try {
            in.close();
            iir.dispose();
            iin.close();
        } catch (Exception ee) {
            LogWriter.writeLog("Problem closing  " + ee);
        }
        return image;
    }
}
