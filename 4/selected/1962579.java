package org.jpedal.color;

import java.util.Map;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.utils.LogWriter;
import javax.imageio.ImageReader;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/**
 * handle Device ColorSpace
 */
public class DeviceNColorSpace extends SeparationColorSpace {

    public DeviceNColorSpace() {
    }

    public DeviceNColorSpace(PdfObjectReader currentPdfFile, String currentColorspace, Map colorValues) {
        value = ColorSpaces.DeviceN;
        processColorToken(currentPdfFile, currentColorspace, colorValues);
    }

    /** set color (translate and set in alt colorspace */
    public void setColor(String[] operand, int opCount) {
        try {
            float[] values = new float[opCount];
            for (int j = 0; j < opCount; j++) values[j] = Float.parseFloat(operand[j]);
            operand = colorMapper.getOperand(values);
            altCS.setColor(operand, operand.length);
        } catch (Exception e) {
        }
    }

    /**
	 * convert separation stream to RGB and return as an image
	  */
    public BufferedImage dataToRGB(byte[] data, int w, int h) {
        BufferedImage image = null;
        try {
            image = createImage(w, h, data);
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Couldn't convert DeviceN colorspace data: " + ee);
        }
        return image;
    }

    /**
         * convert data stream to srgb image
         */
    public BufferedImage JPEGToRGBImage(byte[] data, int ww, int hh, String decodeArray) {
        BufferedImage image = null;
        ByteArrayInputStream in = null;
        ImageReader iir = null;
        ImageInputStream iin = null;
        try {
            in = new ByteArrayInputStream(data);
            iir = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG").next();
            ImageIO.setUseCache(false);
            iin = ImageIO.createImageInputStream((in));
            iir.setInput(iin, true);
            Raster r = iir.readRaster(0, null);
            int w = r.getWidth(), h = r.getHeight();
            DataBufferByte rgb = (DataBufferByte) r.getDataBuffer();
            image = createImage(w, h, rgb.getData());
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
            ee.printStackTrace();
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

    /**
         * turn raw data into an image
         */
    private BufferedImage createImage(int w, int h, byte[] rawData) {
        BufferedImage image;
        byte[] rgb = new byte[w * h * 3];
        int byteCount = rawData.length / componentCount;
        String[] values = new String[componentCount];
        int j = 0, j2 = 0;
        for (int i = 0; i < byteCount; i++) {
            if (j >= rawData.length) break;
            for (int comp = 0; comp < componentCount; comp++) {
                float value = ((rawData[j] & 255) / 255f);
                values[componentCount - comp - 1] = "" + value;
                j++;
            }
            setColor(values, componentCount);
            int foreground = altCS.currentColor.getRGB();
            rgb[j2] = (byte) ((foreground >> 16) & 0xFF);
            rgb[j2 + 1] = (byte) ((foreground >> 8) & 0xFF);
            rgb[j2 + 2] = (byte) ((foreground) & 0xFF);
            j2 = j2 + 3;
        }
        int[] bands = { 0, 1, 2 };
        DataBuffer dataBuf = new DataBufferByte(rgb, rgb.length);
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Raster raster = Raster.createInterleavedRaster(dataBuf, w, h, w * 3, 3, bands, null);
        image.setData(raster);
        return image;
    }
}
