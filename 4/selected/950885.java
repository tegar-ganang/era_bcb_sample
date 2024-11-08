package org.jpedal.color;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.objects.GraphicsState;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;

/**
 * Provides Color functionality and conversion for pdf
 * decoding
 */
public class GenericColorSpace implements Cloneable, Serializable {

    /**any intent*/
    protected String intent = null;

    /**any pattern maps*/
    protected Map patternValues = null;

    /**size for indexed colorspaces*/
    protected int size = 0;

    /**holds cmyk values if present*/
    protected float c = -1, y = -1, m = -1, k = -1;

    /**matrices for calculating CIE XYZ colour*/
    protected float[] W, G, Ma, B, R;

    /**defines rgb colorspace*/
    protected static ColorSpace rgbCS;

    public static final String cb = "<color ";

    public static final String ce = "</color>";

    protected int value = ColorSpaces.DeviceRGB;

    /**conversion Op for translating rasters or images*/
    private static ColorConvertOp CSToRGB = null;

    protected ColorSpace cs;

    protected PdfPaint currentColor = new PdfColor(0, 0, 0);

    /**rgb colormodel*/
    protected static ColorModel rgbModel = null;

    /**currently does nothing but added so we can introduce
     * profile matching
     */
    private static ICC_Profile ICCProfile = null;

    protected boolean failed = false;

    /**initialise all the colorspaces when first needed */
    private static void initCMYKColorspace() throws PdfException {
        try {
            if (ICCProfile == null) {
                rgbModel = new ComponentColorModel(rgbCS, new int[] { 8, 8, 8 }, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            } else {
                int compCount = rgbCS.getNumComponents();
                int[] values = new int[compCount];
                for (int i = 0; i < compCount; i++) values[i] = 8;
                rgbModel = new ComponentColorModel(rgbCS, values, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
            }
            ICC_Profile p = ICC_Profile.getInstance(GenericColorSpace.class.getResourceAsStream("/org/jpedal/res/cmm/cmyk.icm"));
            ICC_ColorSpace cmykCS = new ICC_ColorSpace(p);
            CSToRGB = new ColorConvertOp(cmykCS, rgbCS, ColorSpaces.hints);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e.getMessage() + " initialising color components");
            throw new PdfException("[PDF] Unable to create CMYK colorspace. Check cmyk.icm in jar file");
        }
    }

    public boolean isInvalid() {
        return failed;
    }

    static {
        if (ICCProfile != null) {
            System.out.println("setup " + ICCProfile);
            rgbCS = new ICC_ColorSpace(ICCProfile);
        } else rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    }

    /**
	 * get size
	 */
    public int getIndexSize() {
        return size;
    }

    /**
	 * get color
	 */
    public PdfPaint getColor() {
        return currentColor;
    }

    /**return the set Java colorspace*/
    public ColorSpace getColorSpace() {
        return cs;
    }

    public GenericColorSpace() {
        cs = rgbCS;
    }

    /**
	 * clone graphicsState
	 */
    public final Object clone() {
        Object o = null;
        try {
            o = super.clone();
        } catch (Exception e) {
            System.out.println(e);
        }
        return o;
    }

    /**any indexed colormap*/
    protected byte[] IndexedColorMap = null;

    /**pantone name if present*/
    public String pantoneName = null;

    /**number of colors*/
    protected int componentCount = 3;

    /**handle to graphics state / only set and used by Pattern*/
    protected GraphicsState gs;

    protected int pageHeight;

    /**
	 * <p>Convert DCT encoded image bytestream to sRGB</p>
	 * <p>It uses the internal Java classes
	 * and the Adobe icm to convert CMYK and YCbCr-Alpha - the data is still DCT encoded.</p>
	 * <p>The Sun class JPEGDecodeParam.java is worth examining because it contains lots 
	 * of interesting comments</p>
	 * <p>I tried just using the new IOImage.read() but on type 3 images, all my clipping code 
	 * stopped working so I am still using 1.3</p>
	 */
    protected final BufferedImage nonRGBJPEGToRGBImage(byte[] data, int w, int h, String decodeArray) {
        boolean isProcessed = false;
        BufferedImage image = null;
        ByteArrayInputStream in = null;
        ImageReader iir = null;
        ImageInputStream iin = null;
        try {
            if (CSToRGB == null) initCMYKColorspace();
            CSToRGB = new ColorConvertOp(cs, rgbCS, ColorSpaces.hints);
            in = new ByteArrayInputStream(data);
            int cmykType = getJPEGTransform(data);
            Iterator iterator = ImageIO.getImageReadersByFormatName("JPEG");
            while (iterator.hasNext()) {
                Object o = iterator.next();
                iir = (ImageReader) o;
                if (iir.canReadRaster()) break;
            }
            ImageIO.setUseCache(false);
            iin = ImageIO.createImageInputStream((in));
            iir.setInput(iin, true);
            Raster ras = iir.readRaster(0, null);
            if (decodeArray != null) {
                if (decodeArray.indexOf("1 0 1 0 1 0 1 0") != -1) {
                    DataBuffer buf = ras.getDataBuffer();
                    int count = buf.getSize();
                    for (int ii = 0; ii < count; ii++) buf.setElem(ii, 255 - buf.getElem(ii));
                } else if (decodeArray.indexOf("0 1 0 1 0 1 0 1") != -1) {
                } else if (decodeArray.indexOf("0.0 1.0 0.0 1.0 0.0 1.0 0.0 1.0") != -1) {
                } else if (decodeArray.length() > 0) {
                }
            }
            if (cs.getNumComponents() == 4) {
                isProcessed = true;
                try {
                    if (cmykType == 2) {
                        image = ColorSpaceConvertor.algorithmicConvertCMYKImageToRGB(ras.getDataBuffer(), w, h, false);
                    } else {
                        WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(w, h);
                        CSToRGB.filter(ras, rgbRaster);
                        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                        image.setData(rgbRaster);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (cmykType != 0) {
                image = iir.read(0);
                isProcessed = true;
            }
            if (!isProcessed) {
                WritableRaster rgbRaster;
                in = new ByteArrayInputStream(data);
                com.sun.image.codec.jpeg.JPEGImageDecoder decoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(in);
                Raster currentRaster = decoder.decodeAsRaster();
                int colorType = decoder.getJPEGDecodeParam().getEncodedColorID();
                int width = currentRaster.getWidth();
                int height = currentRaster.getHeight();
                if (colorType == 4) {
                    rgbRaster = rgbModel.createCompatibleWritableRaster(width, height);
                    CSToRGB.filter(currentRaster, rgbRaster);
                    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    image.setData(rgbRaster);
                } else {
                    LogWriter.writeLog("COLOR_ID_YCbCrA image");
                    in = new ByteArrayInputStream(data);
                    decoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(in);
                    image = decoder.decodeAsBufferedImage();
                    image = ColorSpaceConvertor.convertToRGB(image);
                }
            }
        } catch (Exception ee) {
            image = null;
            ee.printStackTrace();
            LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
        } catch (Error err) {
            if (iir != null) iir.dispose();
            if (iin != null) {
                try {
                    iin.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

    /**Toms routien to read the image type - you can also use 
	 * int colorType = decoder.getJPEGDecodeParam().getEncodedColorID();
	 */
    protected final int getJPEGTransform(byte[] data) {
        int xform = 0;
        for (int i = 0, imax = data.length - 2; i < imax; ) {
            int type = data[i + 1] & 0xff;
            i += 2;
            if (type == 0x01 || (0xd0 <= type && type <= 0xda)) {
            } else if (type == 0xda) {
                i = i + ((data[i] & 0xff) << 8) + (data[i + 1] & 0xff);
                while (true) {
                    for (; i < imax; i++) if ((data[i] & 0xff) == 0xff && data[i + 1] != 0) break;
                    int rst = data[i + 1] & 0xff;
                    if (0xd0 <= rst && rst <= 0xd7) i += 2; else break;
                }
            } else {
                if (type == 0xee) {
                    if (data[i + 2] == 'A' && data[i + 3] == 'd' && data[i + 4] == 'o' && data[i + 5] == 'b' && data[i + 6] == 'e') {
                        xform = data[i + 13] & 0xff;
                        break;
                    }
                }
                i = i + ((data[i] & 0xff) << 8) + (data[i + 1] & 0xff);
            }
        }
        return xform;
    }

    public void setIndex(byte[] IndexedColorMap, int size) {
        this.IndexedColorMap = IndexedColorMap;
        this.size = size;
    }

    public void setIndex(String CMap, String name, int count) {
        StringBuffer rawValues = new StringBuffer();
        this.size = count;
        if (CMap.startsWith("(\\")) {
            StringTokenizer octal_values = new StringTokenizer(CMap, "(\\)");
            while (octal_values.hasMoreTokens()) {
                int next_value = Integer.parseInt(octal_values.nextToken(), 8);
                String hex_value = Integer.toHexString(next_value);
                if (hex_value.length() < 2) rawValues.append("0");
                rawValues.append(hex_value);
            }
        } else if (CMap.startsWith("(")) {
        } else {
            if (CMap.startsWith("<")) CMap = CMap.substring(1, CMap.length() - 1).trim();
            rawValues = new StringBuffer(CMap);
        }
        int total_components = 1;
        if ((name.indexOf("RGB") != -1) | (name.indexOf("ICC") != -1)) total_components = 3; else if (name.indexOf("CMYK") != -1) total_components = 4;
        IndexedColorMap = new byte[(count + 1) * total_components];
        rawValues = Strip.stripAllSpaces(rawValues);
        for (int entries = 0; entries < count + 1; entries++) {
            for (int comp = 0; comp < total_components; comp++) {
                int p = (entries * total_components * 2) + (comp * 2);
                int col_value = Integer.parseInt(rawValues.substring(p, p + 2), 16);
                IndexedColorMap[(entries * total_components) + comp] = (byte) col_value;
            }
        }
    }

    /**
	 * lookup a component for index colorspace
	 */
    protected int getIndexedColorComponent(int count) {
        int value = 255;
        if (IndexedColorMap != null) {
            value = IndexedColorMap[count];
            if (value < 0) value = 256 + value;
        }
        return value;
    }

    /**return indexed COlorMap
		 */
    public byte[] getIndexedMap() {
        return IndexedColorMap;
    }

    /**
	 * convert color value to sRGB color
	 */
    public void setColor(String[] value, int operandCount) {
    }

    /**
	 * convert byte[] datastream JPEG to an image in RGB
	 */
    public BufferedImage JPEGToRGBImage(byte[] data, int w, int h, String decodeArray) {
        BufferedImage image = null;
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(data);
            if (PdfDecoder.use13jPEGConversion) {
                com.sun.image.codec.jpeg.JPEGImageDecoder decoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(bis);
                image = decoder.decodeAsBufferedImage();
                decoder = null;
            } else {
                ImageIO.setUseCache(false);
                image = ImageIO.read(bis);
            }
            if (image != null) image = ColorSpaceConvertor.convertToRGB(image);
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Problem reading JPEG: " + ee);
            ee.printStackTrace();
        }
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }

    /**
	 * convert byte[] datastream JPEG to an image in RGB
	 * @throws PdfException 
	 */
    public BufferedImage JPEG2000ToRGBImage(byte[] data) throws PdfException {
        BufferedImage image = null;
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(data);
            ImageReader iir = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG2000").next();
            ImageInputStream iin = ImageIO.createImageInputStream(in);
            try {
                iir.setInput(iin, true);
                image = iir.read(0);
                iir.dispose();
                iin.close();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            image = ColorSpaceConvertor.convertToRGB(image);
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Problem reading JPEG 2000: " + ee);
            throw new PdfException("Exception with JPEG2000 image - please ensure imageio.jar (from JAI library) on classpath");
        } catch (Error ee2) {
            image = null;
            ee2.printStackTrace();
            LogWriter.writeLog("Problem reading JPEG 2000: " + ee2);
            throw new PdfException("Error with JPEG2000 image - please ensure imageio.jar (from JAI library) on classpath");
        }
        return image;
    }

    /**
	 * convert color content of data to sRGB data
	  */
    public BufferedImage dataToRGB(byte[] data, int w, int h) {
        int[] bands = { 0, 1, 2 };
        DataBuffer db = new DataBufferByte(data, data.length);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Raster raster = Raster.createInterleavedRaster(db, w, h, w * 3, 3, bands, null);
        image.setData(raster);
        return image;
    }

    /**
	 * convert image to sRGB image
	  */
    public BufferedImage BufferedImageToRGBImage(BufferedImage image) {
        return image;
    }

    /**get colorspace ID*/
    public int getID() {
        return value;
    }

    /**
	 * create a CIE values for conversion to RGB colorspace
	 */
    public final void setCIEValues(String whitepoint, String blackpoint, String range, String matrix, String gamma) {
        cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        float[] R = { -100f, 100f, -100.0f, 100.0f };
        float[] W = { 0.0f, 1.0f, 0.0f };
        float[] B = { 0.0f, 0.0f, 0.0f };
        float[] G = { 1.0f, 1.0f, 1.0f };
        float[] Ma = { 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f };
        if (whitepoint != null) {
            StringTokenizer matrix_values = new StringTokenizer(whitepoint, "[] ");
            int i = 0;
            while (matrix_values.hasMoreTokens()) {
                W[i] = Float.parseFloat(matrix_values.nextToken());
                i++;
            }
        }
        if (blackpoint != null) {
            StringTokenizer matrix_values = new StringTokenizer(blackpoint, "[] ");
            int i = 0;
            while (matrix_values.hasMoreTokens()) {
                B[i] = Float.parseFloat(matrix_values.nextToken());
                i++;
            }
        }
        if (range != null) {
            StringTokenizer matrix_values = new StringTokenizer(range, "[] ");
            int i = 0;
            while (matrix_values.hasMoreTokens()) {
                R[i] = Float.parseFloat(matrix_values.nextToken());
                i++;
            }
        }
        if (matrix != null) {
            StringTokenizer matrix_values = new StringTokenizer(matrix, "[] ");
            int i = 0;
            while (matrix_values.hasMoreTokens()) {
                Ma[i] = Float.parseFloat(matrix_values.nextToken());
                i++;
            }
        }
        if (gamma != null) {
            StringTokenizer matrix_values = new StringTokenizer(gamma, "[] ");
            int i = 0;
            while (matrix_values.hasMoreTokens()) {
                G[i] = Float.parseFloat(matrix_values.nextToken());
                i++;
            }
        }
        this.G = G;
        this.Ma = Ma;
        this.W = W;
        this.B = B;
        this.R = R;
    }

    /**
	 * convert 4 component index to 3
	  */
    protected final byte[] convert4Index(byte[] data) {
        return convertIndex(data, 4);
    }

    /**
	 * convert 4 component index to 3
	  */
    protected final byte[] convertIndex(byte[] data, int compCount) {
        try {
            int width = data.length / compCount;
            int height = 1;
            DataBuffer db = new DataBufferByte(data, data.length);
            int[] bands;
            WritableRaster raster;
            int[] bands4 = { 0, 1, 2, 3 };
            int[] bands3 = { 0, 1, 2 };
            if (compCount == 4) bands = bands4; else bands = bands3;
            raster = Raster.createInterleavedRaster(db, width, height, width * compCount, compCount, bands, null);
            if (CSToRGB == null) initCMYKColorspace();
            CSToRGB = new ColorConvertOp(cs, rgbCS, ColorSpaces.hints);
            WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(width, height);
            CSToRGB.filter(raster, rgbRaster);
            int size = width * height * 3;
            data = new byte[size];
            DataBuffer convertedData = rgbRaster.getDataBuffer();
            for (int ii = 0; ii < size; ii++) data[ii] = (byte) convertedData.getElem(ii);
        } catch (Exception ee) {
            LogWriter.writeLog("Exception  " + ee + " converting colorspace");
        }
        return data;
    }

    /**
	 * convert Index to RGB
	  */
    public byte[] convertIndexToRGB(byte[] index) {
        return index;
    }

    /**
	 * get an xml string with the color info
	 */
    public String getXMLColorToken() {
        String colorToken = "";
        if (c == -1) {
            if (currentColor instanceof Color) {
                Color col = (Color) currentColor;
                float c = (255 - col.getRed()) / 255f;
                float m = (255 - col.getGreen()) / 255f;
                float y = (255 - col.getBlue()) / 255f;
                float k = c;
                if (k < m) k = m;
                if (k < y) k = y;
                if (pantoneName == null) colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' >"; else colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' pantoneName='" + pantoneName + "' >";
            } else {
                colorToken = GenericColorSpace.cb + "type='shading'>";
            }
        } else {
            if (pantoneName == null) colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' >"; else colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' pantoneName='" + pantoneName + "' >";
        }
        return colorToken;
    }

    /**
	 * pass in list of patterns
	 */
    public void setPattern(Map currentPatternValues, int pageHeight) {
        this.patternValues = currentPatternValues;
        this.pageHeight = pageHeight;
    }

    /** used by generic decoder to asign color*/
    public void setColor(PdfPaint col) {
        this.currentColor = col;
    }

    /**return number of values used for color (ie 3 for rgb)*/
    public int getColorComponentCount() {
        return componentCount;
    }

    /**pattern colorspace needs access to graphicsState*/
    public void setGS(GraphicsState currentGraphicsState) {
        this.gs = currentGraphicsState;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}
