package org.icepdf.core.pobjects;

import org.icepdf.core.io.BitStream;
import org.icepdf.core.io.ConservativeSizingByteArrayOutputStream;
import org.icepdf.core.io.SeekableInputConstrainedWrapper;
import org.icepdf.core.pobjects.filters.*;
import org.icepdf.core.pobjects.graphics.*;
import org.icepdf.core.util.Defs;
import org.icepdf.core.util.ImageCache;
import org.icepdf.core.util.Library;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import java.io.*;
import java.lang.reflect.Method;
import org.icepdf.core.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The Stream class is responsible for decoding stream contents and returning
 * either an images object or a byte array depending on the content.  The Stream
 * object worker method is decode which is responsible for decoding the content
 * stream, which is if the first step of the rendering process.  Once a Stream
 * is decoded it is either returned as an image object or a byte array that is
 * then processed further by the ContentParser.
 *
 * @since 1.0
 */
public class Stream extends Dictionary {

    private static final Logger logger = Logger.getLogger(Stream.class.toString());

    private SeekableInputConstrainedWrapper streamInput;

    private ImageCache image = null;

    private final Object imageLock = new Object();

    private boolean isCCITTFaxDecodeWithoutEncodedByteAlign = false;

    private int CCITTFaxDecodeColumnWidthMismatch = 0;

    private Reference pObjectReference = null;

    private static boolean scaleImages;

    static {
        scaleImages = Defs.sysPropertyBoolean("org.icepdf.core.scaleImages", true);
    }

    private static final int[] GRAY_1_BIT_INDEX_TO_RGB_REVERSED = new int[] { 0xFFFFFFFF, 0xFF000000 };

    private static final int[] GRAY_1_BIT_INDEX_TO_RGB = new int[] { 0xFF000000, 0xFFFFFFFF };

    private static final int[] GRAY_2_BIT_INDEX_TO_RGB = new int[] { 0xFF000000, 0xFF555555, 0xFFAAAAAA, 0xFFFFFFFF };

    private static final int[] GRAY_4_BIT_INDEX_TO_RGB = new int[] { 0xFF000000, 0xFF111111, 0xFF222222, 0xFF333333, 0xFF444444, 0xFF555555, 0xFF666666, 0xFF777777, 0xFF888888, 0xFF999999, 0xFFAAAAAA, 0xFFBBBBBB, 0xFFCCCCCC, 0xFFDDDDDD, 0xFFEEEEEE, 0xFFFFFFFF };

    private static final int JPEG_ENC_UNKNOWN_PROBABLY_YCbCr = 0;

    private static final int JPEG_ENC_RGB = 1;

    private static final int JPEG_ENC_CMYK = 2;

    private static final int JPEG_ENC_YCbCr = 3;

    private static final int JPEG_ENC_YCCK = 4;

    private static final int JPEG_ENC_GRAY = 5;

    private static String[] JPEG_ENC_NAMES = new String[] { "JPEG_ENC_UNKNOWN_PROBABLY_YCbCr", "JPEG_ENC_RGB", "JPEG_ENC_CMYK", "JPEG_ENC_YCbCr", "JPEG_ENC_YCCK", "JPEG_ENC_GRAY" };

    /**
     * Create a new instance of a Stream.
     *
     * @param l                  library containing a hash of all document objects
     * @param h                  hashtable of parameters specific to the Stream object.
     * @param streamInputWrapper Accessor to stream byte data
     */
    public Stream(Library l, Hashtable h, SeekableInputConstrainedWrapper streamInputWrapper) {
        super(l, h);
        streamInput = streamInputWrapper;
    }

    /**
     * Sets the PObject referece for this stream.  The reference number and
     * generation is need by the encryption algorithm.
     */
    public void setPObjectReference(Reference reference) {
        pObjectReference = reference;
    }

    /**
     * Gets the parent PObject reference for this stream.
     *
     * @return Reference number of parent PObject.
     * @see #setPObjectReference(org.icepdf.core.pobjects.Reference)
     */
    public Reference getPObjectReference() {
        return pObjectReference;
    }

    boolean isImageSubtype() {
        Object subtype = library.getObject(entries, "Subtype");
        return subtype != null && subtype.equals("Image");
    }

    /**
     * Utility Method to check if the <code>memoryNeeded</code> can be allowcated,
     * and if not, try and free up the needed amount of memory
     *
     * @param memoryNeeded
     */
    private boolean checkMemory(int memoryNeeded) {
        return library.memoryManager.checkMemory(memoryNeeded);
    }

    /**
     * Utility method for decoding the byte stream using the decode algorithem
     * specified by the filter parameter
     * <p/>
     * The memory manger is called every time a stream is being decoded with an
     * estimated size of the decoded stream.  Because many of the Filter
     * algorithms use compression,  further research must be done to try and
     * find the average amount of memory used by each of the algorithms.
     */
    public InputStream getInputStreamForDecodedStreamBytes() {
        if (streamInput == null || streamInput.getLength() < 1) {
            return null;
        }
        long streamLength = streamInput.getLength();
        int memoryNeeded = (int) streamLength;
        checkMemory(memoryNeeded);
        streamInput.prepareForCurrentUse();
        InputStream input = streamInput;
        int bufferSize = Math.min(Math.max((int) streamLength, 64), 16 * 1024);
        input = new java.io.BufferedInputStream(input, bufferSize);
        if (library.securityManager != null) {
            input = library.getSecurityManager().getEncryptionInputStream(getPObjectReference(), library.getSecurityManager().getDecryptionKey(), input, true);
        }
        Vector filterNames = getFilterNames();
        if (filterNames == null) return input;
        for (int i = 0; i < filterNames.size(); i++) {
            String filterName = filterNames.elementAt(i).toString();
            if (filterName.equals("FlateDecode") || filterName.equals("/Fl") || filterName.equals("Fl")) {
                input = new FlateDecode(library, entries, input);
                memoryNeeded *= 2;
            } else if (filterName.equals("LZWDecode") || filterName.equals("/LZW") || filterName.equals("LZW")) {
                input = new LZWDecode(new BitStream(input));
                memoryNeeded *= 2;
            } else if (filterName.equals("ASCII85Decode") || filterName.equals("/A85") || filterName.equals("A85")) {
                input = new ASCII85Decode(input);
                memoryNeeded *= 2;
            } else if (filterName.equals("ASCIIHexDecode") || filterName.equals("/AHx") || filterName.equals("AHx")) {
                input = new ASCIIHexDecode(input);
                memoryNeeded /= 2;
            } else if (filterName.equals("RunLengthDecode") || filterName.equals("/RL") || filterName.equals("RL")) {
                input = new RunLengthDecode(input);
                memoryNeeded *= 2;
            } else if (filterName.equals("CCITTFaxDecode") || filterName.equals("/CCF") || filterName.equals("CCF")) {
            } else if (filterName.equals("DCTDecode") || filterName.equals("/DCT") || filterName.equals("DCT")) {
            } else if (filterName.equals("JPXDecode")) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "UNSUPPORTED:" + filterName + " " + entries);
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        logger.log(Level.FINE, "Problem closing stream for unsupported JPXDecode");
                    }
                }
                return null;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("UNSUPPORTED:" + filterName + " " + entries);
                }
            }
        }
        checkMemory(memoryNeeded);
        return input;
    }

    private byte[] getDecodedStreamBytes() {
        InputStream input = getInputStreamForDecodedStreamBytes();
        if (input == null) return null;
        try {
            int outLength = Math.max(1024, (int) streamInput.getLength());
            ConservativeSizingByteArrayOutputStream out = new ConservativeSizingByteArrayOutputStream(outLength, library.memoryManager);
            byte[] buffer = new byte[(outLength > 1024) ? 4096 : 1024];
            while (true) {
                int read = input.read(buffer);
                if (read <= 0) break;
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            input.close();
            byte[] ret = out.toByteArray();
            return ret;
        } catch (IOException e) {
            logger.log(Level.FINE, "Problem decoding stream bytes: ", e);
        }
        return null;
    }

    /**
     * This is similar to getDecodedStreamBytes(), except that the returned byte[]
     * is not necessarily exactly sized, and may be larger. Therefore the returned
     * Integer gives the actual valid size
     *
     * @return Object[] { byte[] data, Integer sizeActualData }
     */
    private Object[] getDecodedStreamBytesAndSize(int presize) {
        InputStream input = getInputStreamForDecodedStreamBytes();
        if (input == null) return null;
        try {
            int outLength;
            if (presize > 0) outLength = presize; else outLength = Math.max(1024, (int) streamInput.getLength());
            ConservativeSizingByteArrayOutputStream out = new ConservativeSizingByteArrayOutputStream(outLength, library.memoryManager);
            byte[] buffer = new byte[(outLength > 1024) ? 4096 : 1024];
            while (true) {
                int read = input.read(buffer);
                if (read <= 0) break;
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            input.close();
            int size = out.size();
            boolean trimmed = out.trim();
            byte[] data = out.relinquishByteArray();
            Object[] ret = new Object[] { data, size };
            return ret;
        } catch (IOException e) {
            logger.log(Level.FINE, "Problem decoding stream bytes: ", e);
        }
        return null;
    }

    private boolean shouldUseCCITTFaxDecode() {
        Vector filterNames = getFilterNames();
        if (filterNames == null) return false;
        for (int i = 0; i < filterNames.size(); i++) {
            String filterName = filterNames.elementAt(i).toString();
            if (filterName.equals("CCITTFaxDecode") || filterName.equals("/CCF") || filterName.equals("CCF")) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldUseDCTDecode() {
        Vector filterNames = getFilterNames();
        if (filterNames == null) return false;
        for (int i = 0; i < filterNames.size(); i++) {
            String filterName = filterNames.elementAt(i).toString();
            if (filterName.equals("DCTDecode") || filterName.equals("/DCT") || filterName.equals("DCT")) {
                return true;
            }
        }
        return false;
    }

    private Vector getFilterNames() {
        Vector filterNames = null;
        Object o = library.getObject(entries, "Filter");
        if (o instanceof Name) {
            filterNames = new Vector();
            filterNames.addElement(o);
        } else if (o instanceof Vector) {
            filterNames = (Vector) o;
        }
        return filterNames;
    }

    /**
     * Despose of references to images and decoded byte streams.
     * Memory optimization
     */
    public void dispose(boolean cache) {
        if (streamInput != null) {
            if (!cache) {
                try {
                    streamInput.dispose();
                } catch (IOException e) {
                    logger.log(Level.FINE, "Error disposing stream.", e);
                }
                streamInput = null;
            }
        }
        synchronized (imageLock) {
            if (image != null) {
                image.dispose(cache, (streamInput != null));
                if (!cache || !image.isCachedSomehow()) {
                    image = null;
                    isCCITTFaxDecodeWithoutEncodedByteAlign = false;
                    CCITTFaxDecodeColumnWidthMismatch = 0;
                }
            }
        }
    }

    /**
     * The DCTDecode filter decodes grayscale or color image data that has been
     * encoded in the JPEG baseline format.  Because DCTDecode only deals
     * with images, the instance of image is update instead of decoded
     * stream.
     */
    private void dctDecode(int width, int height, PColorSpace colourSpace, int bitspercomponent, Bitmap smaskImage, Bitmap maskImage, int[] maskMinRGB, int[] maskMaxRGB) {
        InputStream input = getInputStreamForDecodedStreamBytes();
        final int MAX_BYTES_TO_READ_FOR_ENCODING = 2048;
        BufferedInputStream bufferedInput = new BufferedInputStream(input, MAX_BYTES_TO_READ_FOR_ENCODING);
        bufferedInput.mark(MAX_BYTES_TO_READ_FOR_ENCODING);
        int jpegEncoding = Stream.JPEG_ENC_UNKNOWN_PROBABLY_YCbCr;
        try {
            byte[] data = new byte[MAX_BYTES_TO_READ_FOR_ENCODING];
            int dataRead = bufferedInput.read(data);
            bufferedInput.reset();
            if (dataRead > 0) jpegEncoding = getJPEGEncoding(data, dataRead);
        } catch (IOException ioe) {
            logger.log(Level.FINE, "Problem determining JPEG type: ", ioe);
        }
        Bitmap tmpImage = null;
        if (tmpImage == null) {
            try {
            } catch (Exception e) {
                logger.log(Level.FINE, "Problem loading JPEG image via JPEGImageDecoder: ", e);
            }
        }
        try {
            bufferedInput.close();
        } catch (IOException e) {
            logger.log(Level.FINE, "Error closing image stream.", e);
        }
        if (tmpImage == null) {
            try {
                Object javax_media_jai_RenderedOp_op = null;
                try {
                    if (true) throw new Exception("DCT decoding (JAI) not yet supported");
                } catch (Exception e) {
                }
                if (javax_media_jai_RenderedOp_op != null) {
                    if (jpegEncoding == JPEG_ENC_CMYK && bitspercomponent == 8) {
                    } else if (jpegEncoding == JPEG_ENC_YCCK && bitspercomponent == 8) {
                    } else {
                        Class roClass = Class.forName("javax.media.jai.RenderedOp");
                        Method roGetAsBufferedImage = roClass.getMethod("getAsBufferedImage");
                        tmpImage = (Bitmap) roGetAsBufferedImage.invoke(javax_media_jai_RenderedOp_op);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Problem loading JPEG image via JAI: ", e);
            }
            try {
                input.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "Problem closing image stream. ", e);
            }
        }
        if (tmpImage == null) {
            try {
                if (true) throw new Exception("dct decode (toolkit) not yet supported");
            } catch (Exception e) {
                logger.log(Level.FINE, "Problem loading JPEG image via Toolkit: ", e);
            }
        }
        synchronized (imageLock) {
            if (image == null) {
                image = new ImageCache(library);
            }
            image.setImage(tmpImage);
        }
    }

    private static int getJPEGEncoding(byte[] data, int dataLength) {
        int jpegEncoding = JPEG_ENC_UNKNOWN_PROBABLY_YCbCr;
        boolean foundAPP14 = false;
        byte compsTypeFromAPP14 = 0;
        boolean foundSOF = false;
        int numCompsFromSOF = 0;
        boolean foundSOS = false;
        int numCompsFromSOS = 0;
        int index = 0;
        while (true) {
            if (index >= dataLength) break;
            if (data[index] != ((byte) 0xFF)) break;
            if (foundAPP14 && foundSOF) break;
            byte segmentType = data[index + 1];
            index += 2;
            if (segmentType == ((byte) 0xD8)) {
                continue;
            }
            int length = (((data[index] << 8)) & 0xFF00) + (((int) data[index + 1]) & 0xFF);
            if (segmentType == ((byte) 0xEE)) {
                if (length >= 14) {
                    foundAPP14 = true;
                    compsTypeFromAPP14 = data[index + 13];
                }
            } else if (segmentType == ((byte) 0xC0)) {
                foundSOF = true;
                numCompsFromSOF = (((int) data[index + 7]) & 0xFF);
            } else if (segmentType == ((byte) 0xDA)) {
                foundSOS = true;
                numCompsFromSOS = (((int) data[index + 2]) & 0xFF);
            }
            index += length;
        }
        if (foundAPP14 && foundSOF) {
            if (compsTypeFromAPP14 == 0) {
                if (numCompsFromSOF == 1) jpegEncoding = JPEG_ENC_GRAY;
                if (numCompsFromSOF == 3) jpegEncoding = JPEG_ENC_RGB; else if (numCompsFromSOF == 4) jpegEncoding = JPEG_ENC_CMYK;
            } else if (compsTypeFromAPP14 == 1) {
                jpegEncoding = JPEG_ENC_YCbCr;
            } else if (compsTypeFromAPP14 == 2) {
                jpegEncoding = JPEG_ENC_YCCK;
            }
        } else if (foundSOS) {
            if (numCompsFromSOS == 1) jpegEncoding = JPEG_ENC_GRAY;
        }
        return jpegEncoding;
    }

    /**
     * Gets the decoded Byte stream of the Stream object.
     *
     * @return decoded Byte stream
     */
    public byte[] getBytes() {
        byte[] data = getDecodedStreamBytes();
        if (data == null) data = new byte[0];
        return data;
    }

    public Bitmap getImage(int fill, Resources resources, boolean allowScaling) {
        PColorSpace colourSpace = null;
        Object o = library.getObject(entries, "ColorSpace");
        if (resources != null) {
            colourSpace = resources.getColorSpace(o);
        }
        if (colourSpace == null) {
            colourSpace = new DeviceGray(library, null);
        }
        boolean imageMask = isImageMask();
        if (imageMask) allowScaling = false;
        int bitspercomponent = library.getInt(entries, "BitsPerComponent");
        if (imageMask && bitspercomponent == 0) bitspercomponent = 1;
        int width = library.getInt(entries, "Width");
        int height = library.getInt(entries, "Height");
        int colorSpaceCompCount = colourSpace.getNumComponents();
        Vector decode = (Vector) library.getObject(entries, "Decode");
        if (decode == null) {
            decode = new Vector();
            decode.addElement(new Float(0));
            decode.addElement(new Float(1));
        }
        Bitmap smaskImage = null;
        Bitmap maskImage = null;
        int[] maskMinRGB = null;
        int[] maskMaxRGB = null;
        int maskMinIndex = -1;
        int maskMaxIndex = -1;
        Object smaskObj = library.getObject(entries, "SMask");
        Object maskObj = library.getObject(entries, "Mask");
        if (smaskObj instanceof Stream) {
            Stream smaskStream = (Stream) smaskObj;
            if (smaskStream.isImageSubtype()) smaskImage = smaskStream.getImage(fill, resources, false);
        }
        if (smaskImage != null) {
            allowScaling = false;
        }
        if (maskObj != null && smaskImage == null) {
            if (maskObj instanceof Stream) {
                Stream maskStream = (Stream) maskObj;
                if (maskStream.isImageSubtype()) maskImage = maskStream.getImage(fill, resources, false);
            } else if (maskObj instanceof Vector) {
                Vector maskVector = (Vector) maskObj;
                int[] maskMinOrigCompsInt = new int[colorSpaceCompCount];
                int[] maskMaxOrigCompsInt = new int[colorSpaceCompCount];
                for (int i = 0; i < colorSpaceCompCount; i++) {
                    if ((i * 2) < maskVector.size()) maskMinOrigCompsInt[i] = ((Number) maskVector.get(i * 2)).intValue();
                    if ((i * 2 + 1) < maskVector.size()) maskMaxOrigCompsInt[i] = ((Number) maskVector.get(i * 2 + 1)).intValue();
                }
                if (colourSpace instanceof Indexed) {
                    Indexed icolourSpace = (Indexed) colourSpace;
                    int[] colors = icolourSpace.accessColorTable();
                    if (colors != null && maskMinOrigCompsInt.length >= 1 && maskMaxOrigCompsInt.length >= 1) {
                        maskMinIndex = maskMinOrigCompsInt[0];
                        maskMaxIndex = maskMaxOrigCompsInt[0];
                        if (maskMinIndex >= 0 && maskMinIndex < colors.length && maskMaxIndex >= 0 && maskMaxIndex < colors.length) {
                            int minColor = colors[maskMinOrigCompsInt[0]];
                            int maxColor = colors[maskMaxOrigCompsInt[0]];
                            maskMinRGB = new int[] { Color.red(minColor), Color.green(minColor), Color.blue(minColor) };
                            maskMaxRGB = new int[] { Color.red(maxColor), Color.green(maxColor), Color.blue(maxColor) };
                        }
                    }
                } else {
                    PColorSpace.reverseInPlace(maskMinOrigCompsInt);
                    PColorSpace.reverseInPlace(maskMaxOrigCompsInt);
                    float[] maskMinOrigComps = new float[colorSpaceCompCount];
                    float[] maskMaxOrigComps = new float[colorSpaceCompCount];
                    colourSpace.normaliseComponentsToFloats(maskMinOrigCompsInt, maskMinOrigComps, (1 << bitspercomponent) - 1);
                    colourSpace.normaliseComponentsToFloats(maskMaxOrigCompsInt, maskMaxOrigComps, (1 << bitspercomponent) - 1);
                    int minColor = colourSpace.getColor(maskMinOrigComps);
                    int maxColor = colourSpace.getColor(maskMaxOrigComps);
                    PColorSpace.reverseInPlace(maskMinOrigComps);
                    PColorSpace.reverseInPlace(maskMaxOrigComps);
                    maskMinRGB = new int[] { Color.red(minColor), Color.green(minColor), Color.blue(minColor) };
                    maskMaxRGB = new int[] { Color.red(maxColor), Color.green(maxColor), Color.blue(maxColor) };
                }
            }
        }
        Bitmap img = getImage(colourSpace, fill, width, height, colorSpaceCompCount, bitspercomponent, imageMask, decode, smaskImage, maskImage, maskMinRGB, maskMaxRGB, maskMinIndex, maskMaxIndex);
        if (img != null) {
            img = putIntoImageCache(img, width, height, allowScaling);
        }
        return img;
    }

    private Bitmap getImage(PColorSpace colourSpace, int fill, int width, int height, int colorSpaceCompCount, int bitspercomponent, boolean imageMask, Vector decode, Bitmap smaskImage, Bitmap maskImage, int[] maskMinRGB, int[] maskMaxRGB, int maskMinIndex, int maskMaxIndex) {
        byte[] baCCITTFaxData = null;
        if (image == null) {
            if (true) throw new RuntimeException("FAX decode not yet implemented");
        }
        if (image != null) {
            checkMemory(width * height * Math.max(colorSpaceCompCount, 4));
            Bitmap img = null;
            synchronized (imageLock) {
                if (image != null) img = image.readImage();
            }
            if (img != null) return img;
        }
        if (baCCITTFaxData == null) {
            try {
                if (true) throw new Exception("FAX decode not yet supported");
            } catch (Exception e) {
                logger.log(Level.FINE, "Error building image raster.", e);
            }
        }
        Bitmap im = parseImage(width, height, colourSpace, imageMask, fill, bitspercomponent, decode, baCCITTFaxData, smaskImage, maskImage, maskMinRGB, maskMaxRGB);
        return im;
    }

    /**
     * Parses the image stream and creates a Java Images object based on the
     * the given stream and the supporting paramaters.
     *
     * @param width         dimension of new image
     * @param height        dimension of new image
     * @param colorSpace    colour space of image
     * @param imageMask     true if the image has a imageMask, false otherwise
     * @param fill          colour pased in via graphic state, used to fill in background
     * @param bitsPerColour number of bits used in a colour
     * @param decode        Decode attribute values from PObject
     * @return valid java image from the PDF stream
     */
    private Bitmap parseImage(int width, int height, PColorSpace colorSpace, boolean imageMask, int fill, int bitsPerColour, Vector decode, byte[] baCCITTFaxData, Bitmap smaskImage, Bitmap maskImage, int[] maskMinRGB, int[] maskMaxRGB) {
        int[] imageBits = new int[width];
        int fillRGB = fill;
        int colorSpaceCompCount = colorSpace.getNumComponents();
        boolean isDeviceRGB = colorSpace instanceof DeviceRGB;
        boolean isDeviceGray = colorSpace instanceof DeviceGray;
        int maxColourValue = ((1 << bitsPerColour) - 1);
        int f[] = new int[colorSpaceCompCount];
        float ff[] = new float[colorSpaceCompCount];
        int imageMaskValue = ((Number) decode.elementAt(0)).intValue();
        float[] decodeArray = null;
        if (decode != null) {
            decodeArray = new float[decode.size()];
            for (int i = 0; i < decodeArray.length; i++) {
                decodeArray[i] = ((Number) decode.elementAt(i)).floatValue();
            }
        }
        int memoryNeeded = (width * height * 4);
        checkMemory(memoryNeeded);
        Bitmap bim = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        int bitsPerRow = width * colorSpaceCompCount * bitsPerColour;
        int extraBitsPerRow = bitsPerRow & 0x7;
        if (CCITTFaxDecodeColumnWidthMismatch > 0) {
            int bitsGivenPerRow = (width + CCITTFaxDecodeColumnWidthMismatch) * colorSpaceCompCount * bitsPerColour;
            int bitsRelevant = bitsPerRow;
            extraBitsPerRow = bitsGivenPerRow - bitsRelevant;
        }
        BitStream in = null;
        if (baCCITTFaxData != null) {
            in = new BitStream(new ByteArrayInputStream(baCCITTFaxData));
        } else {
            InputStream dataInput = getInputStreamForDecodedStreamBytes();
            if (dataInput == null) return null;
            in = new BitStream(dataInput);
        }
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (imageMask) {
                        int bit = in.getBits(bitsPerColour);
                        bit = (bit == imageMaskValue) ? fillRGB : 0x00000000;
                        imageBits[x] = bit;
                    } else {
                        int red = 255;
                        int blue = 255;
                        int green = 255;
                        int alpha = 255;
                        if (colorSpaceCompCount == 1) {
                            int bit = in.getBits(bitsPerColour);
                            if (decodeArray != null) {
                                if (decodeArray[0] > decodeArray[1]) {
                                    bit = (bit == maxColourValue) ? 0x00000000 : maxColourValue;
                                }
                            }
                            if (isDeviceGray) {
                                if (bitsPerColour == 1) bit = GRAY_1_BIT_INDEX_TO_RGB[bit]; else if (bitsPerColour == 2) bit = GRAY_2_BIT_INDEX_TO_RGB[bit]; else if (bitsPerColour == 4) bit = GRAY_4_BIT_INDEX_TO_RGB[bit]; else if (bitsPerColour == 8) {
                                    bit = ((bit << 24) | (bit << 16) | (bit << 8) | bit);
                                }
                                imageBits[x] = bit;
                            } else {
                                f[0] = bit;
                                colorSpace.normaliseComponentsToFloats(f, ff, maxColourValue);
                                int color = colorSpace.getColor(ff);
                                imageBits[x] = color;
                            }
                        } else if (colorSpaceCompCount == 3) {
                            if (isDeviceRGB) {
                                red = in.getBits(bitsPerColour);
                                green = in.getBits(bitsPerColour);
                                blue = in.getBits(bitsPerColour);
                                imageBits[x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
                            } else {
                                for (int i = 0; i < colorSpaceCompCount; i++) {
                                    f[i] = in.getBits(bitsPerColour);
                                }
                                PColorSpace.reverseInPlace(f);
                                colorSpace.normaliseComponentsToFloats(f, ff, maxColourValue);
                                int color = colorSpace.getColor(ff);
                                imageBits[x] = color;
                            }
                        } else if (colorSpaceCompCount == 4) {
                            for (int i = 0; i < colorSpaceCompCount; i++) {
                                f[i] = in.getBits(bitsPerColour);
                            }
                            PColorSpace.reverseInPlace(f);
                            colorSpace.normaliseComponentsToFloats(f, ff, maxColourValue);
                            int color = colorSpace.getColor(ff);
                            imageBits[x] = color;
                        } else {
                            imageBits[x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
                        }
                    }
                }
                if (extraBitsPerRow > 0 && (isCCITTFaxDecodeWithoutEncodedByteAlign == false || CCITTFaxDecodeColumnWidthMismatch > 0)) {
                    in.getBits(extraBitsPerRow);
                }
            }
            in.close();
            in = null;
            if (smaskImage != null || maskImage != null || maskMinRGB != null || maskMaxRGB != null) {
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Error parsing image.", e);
        }
        return bim;
    }

    private Bitmap putIntoImageCache(Bitmap bim, int width, int height, boolean allowScaling) {
        if (image == null) {
            image = new ImageCache(library);
        }
        boolean setIsScaledOnImageCache = false;
        if (allowScaling && scaleImages && !image.isScaled()) {
            int bimPixelSize = 4;
            boolean canScale = checkMemory(Math.max(width, bim.getWidth()) * Math.max(height, bim.getHeight()) * Math.max(4, bimPixelSize));
            if (canScale) {
                bim = ImageCache.scaleBufferedImage(bim, width, height);
                setIsScaledOnImageCache = true;
            }
        }
        synchronized (imageLock) {
            if (image == null) {
                image = new ImageCache(library);
            }
            if (setIsScaledOnImageCache) image.setIsScaled(true);
            image.setImage(bim);
            bim = image.readImage();
        }
        return bim;
    }

    /**
     * Does the image have an ImageMask.
     */
    public boolean isImageMask() {
        Object o = library.getObject(entries, "ImageMask");
        return (o != null) ? (o.toString().equals("true") ? true : false) : false;
    }

    public boolean getBlackIs1(Library library, Hashtable decodeParmsDictionary) {
        Boolean blackIs1 = getBlackIs1OrNull(library, decodeParmsDictionary);
        if (blackIs1 != null) return blackIs1.booleanValue();
        return false;
    }

    /**
     * If BlackIs1 was not specified, then return null, instead of the
     * default value of false, so we can tell if it was given or not
     */
    public Boolean getBlackIs1OrNull(Library library, Hashtable decodeParmsDictionary) {
        Object blackIs1Obj = library.getObject(decodeParmsDictionary, "BlackIs1");
        if (blackIs1Obj != null) {
            if (blackIs1Obj instanceof Boolean) {
                return (Boolean) blackIs1Obj;
            } else if (blackIs1Obj instanceof String) {
                String blackIs1String = (String) blackIs1Obj;
                if (blackIs1String.equalsIgnoreCase("true")) return Boolean.TRUE; else if (blackIs1String.equalsIgnoreCase("t")) return Boolean.TRUE; else if (blackIs1String.equals("1")) return Boolean.TRUE; else if (blackIs1String.equalsIgnoreCase("false")) return Boolean.FALSE; else if (blackIs1String.equalsIgnoreCase("f")) return Boolean.FALSE; else if (blackIs1String.equals("0")) return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Return a string description of the object.  Primarly used for debugging.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append("STREAM= ");
        sb.append(entries);
        if (getPObjectReference() != null) {
            sb.append("  ");
            sb.append(getPObjectReference());
        }
        return sb.toString();
    }
}
