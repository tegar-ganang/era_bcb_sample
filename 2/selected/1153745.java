package com.lowagie.text;

import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An <CODE>Jpeg</CODE> is the representation of a graphic element (JPEG)
 * that has to be inserted into the document
 *
 * @see		Element
 * @see		Image
 */
public class Jpeg extends Image {

    /** This is a type of marker. */
    public static final int NOT_A_MARKER = -1;

    /** This is a type of marker. */
    public static final int VALID_MARKER = 0;

    /** Acceptable Jpeg markers. */
    public static final int[] VALID_MARKERS = { 0xC0, 0xC1, 0xC2 };

    /** This is a type of marker. */
    public static final int UNSUPPORTED_MARKER = 1;

    /** Unsupported Jpeg markers. */
    public static final int[] UNSUPPORTED_MARKERS = { 0xC3, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF };

    /** This is a type of marker. */
    public static final int NOPARAM_MARKER = 2;

    /** Jpeg markers without additional parameters. */
    public static final int[] NOPARAM_MARKERS = { 0xD0, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8, 0x01 };

    /** Marker value */
    public static final int M_APP0 = 0xE0;

    /** Marker value */
    public static final int M_APP2 = 0xE2;

    /** Marker value */
    public static final int M_APPE = 0xEE;

    /** sequence that is used in all Jpeg files */
    public static final byte JFIF_ID[] = { 0x4A, 0x46, 0x49, 0x46, 0x00 };

    private byte[][] icc;

    Jpeg(Image image) {
        super(image);
    }

    /**
     * Constructs a <CODE>Jpeg</CODE>-object, using an <VAR>url</VAR>.
     *
     * @param		url			the <CODE>URL</CODE> where the image can be found
     * @throws BadElementException
     * @throws IOException
     */
    public Jpeg(URL url) throws BadElementException, IOException {
        super(url);
        processParameters();
    }

    /**
     * Constructs a <CODE>Jpeg</CODE>-object from memory.
     *
     * @param		img		the memory image
     * @throws BadElementException
     * @throws IOException
     */
    public Jpeg(byte[] img) throws BadElementException, IOException {
        super((URL) null);
        rawData = img;
        originalData = img;
        processParameters();
    }

    /**
     * Constructs a <CODE>Jpeg</CODE>-object from memory.
     *
     * @param		img			the memory image.
     * @param		width		the width you want the image to have
     * @param		height		the height you want the image to have
     * @throws BadElementException
     * @throws IOException
     */
    public Jpeg(byte[] img, float width, float height) throws BadElementException, IOException {
        this(img);
        scaledWidth = width;
        scaledHeight = height;
    }

    /**
     * Reads a short from the <CODE>InputStream</CODE>.
     *
     * @param	is		the <CODE>InputStream</CODE>
     * @return	an int
     * @throws IOException
     */
    private static final int getShort(InputStream is) throws IOException {
        return (is.read() << 8) + is.read();
    }

    /**
     * Returns a type of marker.
     *
     * @param	marker      an int
     * @return	a type: <VAR>VALID_MARKER</CODE>, <VAR>UNSUPPORTED_MARKER</VAR> or <VAR>NOPARAM_MARKER</VAR>
     */
    private static final int marker(int marker) {
        for (int i = 0; i < VALID_MARKERS.length; i++) {
            if (marker == VALID_MARKERS[i]) {
                return VALID_MARKER;
            }
        }
        for (int i = 0; i < NOPARAM_MARKERS.length; i++) {
            if (marker == NOPARAM_MARKERS[i]) {
                return NOPARAM_MARKER;
            }
        }
        for (int i = 0; i < UNSUPPORTED_MARKERS.length; i++) {
            if (marker == UNSUPPORTED_MARKERS[i]) {
                return UNSUPPORTED_MARKER;
            }
        }
        return NOT_A_MARKER;
    }

    /**
     * This method checks if the image is a valid JPEG and processes some parameters.
     * @throws BadElementException
     * @throws IOException
     */
    private void processParameters() throws BadElementException, IOException {
        type = JPEG;
        originalType = ORIGINAL_JPEG;
        InputStream is = null;
        try {
            String errorID;
            if (rawData == null) {
                is = url.openStream();
                errorID = url.toString();
            } else {
                is = new java.io.ByteArrayInputStream(rawData);
                errorID = "Byte array";
            }
            if (is.read() != 0xFF || is.read() != 0xD8) {
                throw new BadElementException(errorID + " is not a valid JPEG-file.");
            }
            boolean firstPass = true;
            int len;
            while (true) {
                int v = is.read();
                if (v < 0) throw new IOException("Premature EOF while reading JPG.");
                if (v == 0xFF) {
                    int marker = is.read();
                    if (firstPass && marker == M_APP0) {
                        firstPass = false;
                        len = getShort(is);
                        if (len < 16) {
                            Utilities.skip(is, len - 2);
                            continue;
                        }
                        byte bcomp[] = new byte[JFIF_ID.length];
                        int r = is.read(bcomp);
                        if (r != bcomp.length) throw new BadElementException(errorID + " corrupted JFIF marker.");
                        boolean found = true;
                        for (int k = 0; k < bcomp.length; ++k) {
                            if (bcomp[k] != JFIF_ID[k]) {
                                found = false;
                                break;
                            }
                        }
                        if (!found) {
                            Utilities.skip(is, len - 2 - bcomp.length);
                            continue;
                        }
                        Utilities.skip(is, 2);
                        int units = is.read();
                        int dx = getShort(is);
                        int dy = getShort(is);
                        if (units == 1) {
                            dpiX = dx;
                            dpiY = dy;
                        } else if (units == 2) {
                            dpiX = (int) (dx * 2.54f + 0.5f);
                            dpiY = (int) (dy * 2.54f + 0.5f);
                        }
                        Utilities.skip(is, len - 2 - bcomp.length - 7);
                        continue;
                    }
                    if (marker == M_APPE) {
                        len = getShort(is) - 2;
                        byte[] byteappe = new byte[len];
                        for (int k = 0; k < len; ++k) {
                            byteappe[k] = (byte) is.read();
                        }
                        if (byteappe.length >= 12) {
                            String appe = new String(byteappe, 0, 5, "ISO-8859-1");
                            if (appe.equals("Adobe")) {
                                invert = true;
                            }
                        }
                        continue;
                    }
                    if (marker == M_APP2) {
                        len = getShort(is) - 2;
                        byte[] byteapp2 = new byte[len];
                        for (int k = 0; k < len; ++k) {
                            byteapp2[k] = (byte) is.read();
                        }
                        if (byteapp2.length >= 14) {
                            String app2 = new String(byteapp2, 0, 11, "ISO-8859-1");
                            if (app2.equals("ICC_PROFILE")) {
                                int order = byteapp2[12] & 0xff;
                                int count = byteapp2[13] & 0xff;
                                if (icc == null) icc = new byte[count][];
                                icc[order - 1] = byteapp2;
                            }
                        }
                        continue;
                    }
                    firstPass = false;
                    int markertype = marker(marker);
                    if (markertype == VALID_MARKER) {
                        Utilities.skip(is, 2);
                        if (is.read() != 0x08) {
                            throw new BadElementException(errorID + " must have 8 bits per component.");
                        }
                        scaledHeight = getShort(is);
                        setTop(scaledHeight);
                        scaledWidth = getShort(is);
                        setRight(scaledWidth);
                        colorspace = is.read();
                        bpc = 8;
                        break;
                    } else if (markertype == UNSUPPORTED_MARKER) {
                        throw new BadElementException(errorID + ": unsupported JPEG marker: " + marker);
                    } else if (markertype != NOPARAM_MARKER) {
                        Utilities.skip(is, getShort(is) - 2);
                    }
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        plainWidth = getWidth();
        plainHeight = getHeight();
        if (icc != null) {
            int total = 0;
            for (int k = 0; k < icc.length; ++k) {
                if (icc[k] == null) {
                    icc = null;
                    return;
                }
                total += icc[k].length - 14;
            }
            byte[] ficc = new byte[total];
            total = 0;
            for (int k = 0; k < icc.length; ++k) {
                System.arraycopy(icc[k], 14, ficc, total, icc[k].length - 14);
                total += icc[k].length - 14;
            }
            try {
                ICC_Profile icc_prof = ICC_Profile.getInstance(ficc);
                tagICC(icc_prof);
            } catch (IllegalArgumentException e) {
            }
            icc = null;
        }
    }
}
