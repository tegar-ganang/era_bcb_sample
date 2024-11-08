package org.jpedal.color;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.utils.LogWriter;

/**
 * handle Separation ColorSpace - thanks to Tom for some of the code -
 * reproduced with permissions
 */
public class SeparationColorSpace extends GenericColorSpace {

    protected GenericColorSpace altCS;

    protected ColorMapping colorMapper;

    private float[] domain;

    public SeparationColorSpace() {
    }

    public SeparationColorSpace(PdfObjectReader currentPdfFile, String currentColorspace, Map colorValues) {
        value = ColorSpaces.Separation;
        processColorToken(currentPdfFile, currentColorspace, colorValues);
    }

    protected void processColorToken(PdfObjectReader currentPdfFile, String currentColorspace, Map colorValues) {
        String altColorSpaceName = "", transformName = "";
        Map altColorSpace = new Hashtable();
        Map tintTransform = new Hashtable();
        byte[] stream = null;
        float[] range = null;
        domain = null;
        StringTokenizer sep_tokens = new StringTokenizer(currentColorspace, " ");
        if (sep_tokens.countTokens() > 3) {
            String sepKeyWord = sep_tokens.nextToken();
            if (value == ColorSpaces.Separation) {
                pantoneName = sep_tokens.nextToken().substring(1);
                componentCount = 1;
            } else {
                String colorantWord = sep_tokens.nextToken();
                componentCount = 0;
                while (true) {
                    colorantWord = sep_tokens.nextToken();
                    if (colorantWord.equals("]")) break;
                    componentCount++;
                }
            }
            altColorSpaceName = sep_tokens.nextToken();
            if (altColorSpaceName.equals("[")) {
                while (altColorSpaceName.indexOf("]") == -1) altColorSpaceName = altColorSpaceName + " " + sep_tokens.nextToken();
            }
            transformName = sep_tokens.nextToken();
            if (sep_tokens.hasMoreTokens()) {
                if (transformName.equals("0")) {
                    altColorSpaceName = altColorSpaceName + " 0 R";
                    altColorSpace = currentPdfFile.readObject(altColorSpaceName, false, null);
                    transformName = "";
                    sep_tokens.nextToken();
                } else transformName = transformName + " ";
                if (value == ColorSpaces.Separation) {
                    while (sep_tokens.hasMoreTokens()) transformName = transformName + sep_tokens.nextToken() + " ";
                } else {
                    while (sep_tokens.hasMoreTokens()) {
                        String nextToken = sep_tokens.nextToken();
                        transformName = transformName + nextToken + " ";
                        if (nextToken.equals("R") | (nextToken.equals("<<"))) break;
                    }
                }
                transformName = transformName.trim();
                if (transformName.endsWith("R")) tintTransform = currentPdfFile.readObject(transformName, false, null);
                if (altColorSpaceName.endsWith("R")) {
                    Map ColObj = currentPdfFile.readObject(altColorSpaceName, false, null);
                    altColorSpaceName = (String) ColObj.get("rawValue");
                }
            }
            stream = currentPdfFile.readStream(transformName, true);
        } else {
            pantoneName = currentColorspace.substring(11).trim();
            int firstR = pantoneName.indexOf("R");
            if (pantoneName.startsWith("/")) {
                pantoneName = pantoneName.substring(1);
                int altIndex = pantoneName.indexOf("/");
                if (altIndex != -1) {
                    altColorSpaceName = pantoneName.substring(altIndex).trim();
                    pantoneName = pantoneName.substring(0, altIndex).trim();
                }
            } else if (firstR != -1) {
                altColorSpaceName = pantoneName.substring(firstR + 1).trim();
                pantoneName = currentPdfFile.getValue(pantoneName.substring(0, firstR));
                if (altColorSpaceName.endsWith("R")) altColorSpaceName = currentPdfFile.getValue(altColorSpaceName);
            }
            if (altColorSpaceName.length() == 0) {
                Iterator ii = colorValues.keySet().iterator();
                altColorSpaceName = "";
                while (ii.hasNext()) {
                    String next = (String) ii.next();
                    if (!next.equals("rawValue")) {
                        altColorSpaceName = altColorSpaceName + next;
                    }
                }
            }
            tintTransform = (Map) colorValues.get(altColorSpaceName);
            if (tintTransform == null) tintTransform = colorValues;
        }
        altCS = ColorspaceDecoder.getColorSpaceInstance(false, null, altColorSpaceName, altColorSpace, currentPdfFile);
        if ((pantoneName != null) && (pantoneName.indexOf("#") != -1)) {
            StringBuffer newValue = new StringBuffer();
            int nameLength = pantoneName.length();
            for (int i = 0; i < nameLength; i++) {
                char c = pantoneName.charAt(i);
                if (c == '#') {
                    String hexValue = pantoneName.substring(i + 1, i + 3);
                    newValue.append((char) Integer.parseInt(hexValue, 16));
                    i = i + 2;
                } else {
                    newValue.append(c);
                }
            }
            pantoneName = newValue.toString();
        }
        colorMapper = new ColorMapping(currentPdfFile, tintTransform, stream, range);
        domain = colorMapper.getDomain();
    }

    /**private method to do the calculation*/
    private void setColor(float value) {
        try {
            int elements = 1;
            if (domain != null) elements = domain.length / 2;
            float[] values = new float[elements];
            for (int j = 0; j < elements; j++) values[j] = value;
            String[] operand = colorMapper.getOperand(values);
            altCS.setColor(operand, operand.length);
        } catch (Exception e) {
        }
    }

    /** set color (translate and set in alt colorspace */
    public void setColor(String[] operand, int opCount) {
        setColor(Float.parseFloat(operand[0]));
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
	 * convert separation stream to RGB and return as an image
	  */
    public BufferedImage dataToRGB(byte[] data, int w, int h) {
        BufferedImage image = null;
        try {
            image = createImage(w, h, data);
        } catch (Exception ee) {
            image = null;
            LogWriter.writeLog("Couldn't convert Separation colorspace data: " + ee);
        }
        return image;
    }

    /**
	 * turn raw data into an image
	 */
    private BufferedImage createImage(int w, int h, byte[] rgb) {
        BufferedImage image;
        int byteCount = rgb.length;
        float[] lookuptable = new float[256];
        for (int i = 0; i < 255; i++) lookuptable[i] = -1;
        for (int i = 0; i < byteCount; i++) {
            int value = (rgb[i] & 255);
            if (lookuptable[value] == -1) {
                setColor(value / 255f);
                lookuptable[value] = ((Color) this.getColor()).getRed();
            }
            rgb[i] = (byte) lookuptable[value];
        }
        int[] bands = { 0 };
        DataBuffer dataBuf = new DataBufferByte(rgb, rgb.length);
        image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Raster raster = Raster.createInterleavedRaster(dataBuf, w, h, w, 1, bands, null);
        image.setData(raster);
        return image;
    }

    /**
	 * create rgb index for color conversion
	 */
    public byte[] convertIndexToRGB(byte[] data) {
        byte[] newdata = new byte[3 * 256];
        try {
            int outputReached = 0;
            String[] opValues = new String[1];
            Color currentCol = null;
            String[] operand;
            int byteCount = data.length;
            float[] values = new float[componentCount];
            for (int i = 0; i < byteCount; i = i + componentCount) {
                if (this.componentCount == 1) {
                    opValues = new String[1];
                    opValues[1] = "" + (data[i] & 255);
                    setColor(opValues, 1);
                    currentCol = (Color) this.getColor();
                } else {
                    for (int j = 0; j < componentCount; j++) values[componentCount - 1 - j] = (data[i + j] & 255) / 255f;
                    operand = colorMapper.getOperand(values);
                    altCS.setColor(operand, operand.length);
                    currentCol = (Color) altCS.getColor();
                }
                newdata[outputReached] = (byte) currentCol.getBlue();
                outputReached++;
                newdata[outputReached] = (byte) currentCol.getGreen();
                outputReached++;
                newdata[outputReached] = (byte) currentCol.getRed();
                outputReached++;
            }
        } catch (Exception ee) {
            System.out.println(ee);
            LogWriter.writeLog("Exception  " + ee + " converting colorspace");
        }
        return newdata;
    }

    /**
	 * get color
	 */
    public PdfPaint getColor() {
        return altCS.getColor();
    }

    /**
	 * get alt colorspace for separation colorspace
	 */
    public GenericColorSpace getAltColorSpace() {
        return altCS;
    }
}
