package multivalent.std.adaptor.pdf;

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.*;
import java.awt.color.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import phelps.imageio.plugins.Fax;
import phelps.awt.color.ColorSpaceCMYK;

/**
	Interpret PDF image types, from objects or inline (content stream):
	DCT (JPEG), CCITT FAX (Group 3, Group 3 2D, Group 4), raw samples (bit depth 1,2,4,8), JBIG2.
	Does no cacheing; always creates new image.

	@see javax.imageio.*
	@see phelps.imageio.plugins.Fax

	@version $Revision: 982 $ $Date: 2006-09-14 16:02:14 -0400 (Thu, 14 Sep 2006) $
*/
public class Images {

    static final boolean DEBUG = true && PDF.DEBUG;

    private Images() {
    }

    /**
	Constructs new BufferedImage from dictionary attributes and data in stream.
	@param imgdict   image XObject, or Map with {@link PDFReader#STREAM_DATA} key set for inline images
  */
    public static BufferedImage createImage(Map imgdict, InputStream in, Color fillcolor, PDFReader pdfr) throws IOException {
        assert imgdict != null && in != null && "Image".equals(imgdict.get("Subtype")) || imgdict.get("Subtype") == null;
        BufferedImage img;
        int w = ((Number) pdfr.getObject(imgdict.get("Width"))).intValue(), h = ((Number) pdfr.getObject(imgdict.get("Height"))).intValue();
        String filter = getFilter(imgdict, pdfr);
        if ("DCTDecode".equals(filter)) img = createJPEG(imgdict, in); else if ("JPXDecode".equals(filter)) img = createJPEG2000(in); else if ("CCITTFaxDecode".equals(filter)) img = createFAX(imgdict, in, fillcolor, pdfr); else if ("JBIG2Decode".equals(filter)) img = createJBIG2(); else {
            img = createRaw(imgdict, w, h, in, fillcolor, pdfr);
        }
        if (img == null) return null;
        assert w == img.getWidth() : "width=" + img.getWidth() + " vs param " + w;
        assert h == img.getHeight() : "height=" + img.getHeight() + " vs param " + h;
        return img;
    }

    /**
	Scale and rotate according to affine transform, adjusting image origin from PDF lower-left to Java upper-left.
	@see #createImage(Map, InputStream, Color, PDFReader)
  */
    public static BufferedImage createScaledImage(Map imgdict, InputStream in, AffineTransform ctm, Color fillcolor, PDFReader pdfr) throws IOException {
        assert imgdict != null && in != null && ctm != null && "Image".equals(imgdict.get("Subtype")) || imgdict.get("Subtype") == null;
        BufferedImage img = null;
        try {
            img = createImage(imgdict, in, fillcolor, pdfr);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        if (img == null) return null;
        int w = ((Number) pdfr.getObject(imgdict.get("Width"))).intValue(), h = ((Number) pdfr.getObject(imgdict.get("Height"))).intValue();
        AffineTransform iat;
        if (ctm.getScaleX() != 0.0 && false) {
            Point2D srcpt = new Point2D.Double(w, h), transpt = new Point2D.Double();
            srcpt.setLocation(1.0, 1.0);
            ctm.deltaTransform(srcpt, transpt);
            System.out.println(srcpt + " => " + transpt + " in " + w + "X" + h + " " + ctm);
            double xscale = ctm.getScaleX(), yscale = ctm.getScaleY();
            iat = new AffineTransform(xscale / w, ctm.getShearY(), ctm.getShearX(), -(yscale / h + (yscale < 0.0 ? -1.0 : 1.0) / h), transpt.getX() >= 0.0 ? 0.0 : -transpt.getX(), transpt.getY() >= 0.0 ? 0.0 : -transpt.getY());
        } else if (ctm.getScaleX() != 0.0) {
            double xscale = ctm.getScaleX(), yscale = ctm.getScaleY();
            iat = new AffineTransform(xscale / w, ctm.getShearY(), ctm.getShearX(), -(yscale / h + (yscale < 0.0 ? -1.0 : 1.0) / h), 0.0, 0.0);
            iat = new AffineTransform(xscale / w, 0.0, 0.0, -(yscale + (yscale < 0.0 ? -1.0 : 1.0)) / h, 0.0, 0.0);
            if (iat.getScaleX() < 0.0) iat.translate(-w, 0);
            if (iat.getScaleY() < 0.0) iat.translate(0, -h);
        } else {
            double xshear = ctm.getShearX(), yshear = ctm.getShearY();
            iat = new AffineTransform(0.0, yshear / w, -xshear / h, 0.0, 0.0, 0.0);
            if (iat.getShearX() < 0.0) iat.translate(0, -h);
            if (iat.getShearY() < 0.0) iat.translate(-w, 0);
        }
        try {
            if ("CCITTFaxDecode".equals(getFilter(imgdict, pdfr))) {
                img = Fax.scale(img, iat);
            } else {
                AffineTransformOp aop = new AffineTransformOp(iat, AffineTransformOp.TYPE_BILINEAR);
                img = aop.filter(img, null);
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
            System.out.println(ioe);
            System.err.println(imgdict.get("Name") + " " + getFilter(imgdict, pdfr) + " " + w + "X" + h + ", w/" + ctm + " => " + iat);
        }
        return img;
    }

    /** Process inline image into Node. */
    public static BufferedImage createScaledInline(CompositeInputStream in, Map csres, AffineTransform ctm, Color fillcolor, PDFReader pdfr) throws IOException {
        Map iidict = PDFReader.readInlineImage(in);
        InputStream iin = pdfr.getInputStream(iidict);
        Object csobj = iidict.get("ColorSpace");
        if (csres != null && csres.get(csobj) != null) iidict.put("ColorSpace", csres.get(csobj));
        BufferedImage img = Images.createScaledImage(iidict, iin, ctm, fillcolor, pdfr);
        iin.close();
        assert img != null : "bad INLINE IMG " + iidict;
        return img;
    }

    /**
	Return image part of filter, which may be in a cascade, or <code>null</code> if none.
	Expands abbreviations ("DCT" => "DCTDecode", "CCF" => "CCITTFaxDecode").
	For example, from <code>[ASCII85Decode CCF]</code>, returns <code>CCITTFaxDecode</code>.
  */
    public static String getFilter(Map imgdict, PDFReader pdfr) throws IOException {
        Object attr = imgdict.get("Filter");
        if (pdfr != null) attr = pdfr.getObject(attr);
        String f;
        if (attr == null) f = null; else if (attr.getClass() == PDFReader.CLASS_NAME) f = (String) attr; else {
            assert attr.getClass() == PDFReader.CLASS_ARRAY;
            Object[] oa = (Object[]) attr;
            Object o = oa.length > 0 ? oa[oa.length - 1] : null;
            if (pdfr != null) o = pdfr.getObject(o);
            f = (String) o;
        }
        if ("DCT".equals(f)) f = "DCTDecode"; else if ("CCF".equals(f)) f = "CCITTFaxDecode";
        return "DCTDecode".equals(f) || "CCITTFaxDecode".equals(f) || "JBIG2Decode".equals(f) || "JPXDecode".equals(f) ? f : null;
    }

    /**
	Returns image's /DecodeParms, or <code>null</code> if none (or {@link PDFReader#OBJECT_NULL}).
	If /DecodeParms is an array, the one corresponding to the image is always the last array element.
  */
    public static Map getDecodeParms(Map imgdict, PDFReader pdfr) throws IOException {
        Object o = pdfr.getObject(imgdict.get("DecodeParms"));
        Map dp = (Map) (o == null ? null : o.getClass() == PDFReader.CLASS_DICTIONARY ? o : pdfr.getObject(((Object[]) o)[((Object[]) o).length - 1]));
        return (dp != PDFReader.OBJECT_NULL ? dp : null);
    }

    /** Hand off to ImageIO. */
    static BufferedImage createJPEG(Map imgdict, InputStream in) throws IOException {
        assert imgdict != null && in != null;
        if (in.markSupported()) in.mark(1024);
        BufferedImage img;
        ImageReader iir = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG").next();
        ImageIO.setUseCache(false);
        ImageInputStream iin = ImageIO.createImageInputStream(in);
        iir.setInput(iin, true);
        try {
            img = iir.read(0);
        } catch (IOException e) {
            img = null;
            System.err.println("Couldn't read JPEG: " + e);
        }
        iir.dispose();
        iin.close();
        in.reset();
        return img;
    }

    /** Hand off to ImageIO. */
    static BufferedImage createJPEG2000(InputStream in) throws IOException {
        assert in != null;
        if (in.markSupported()) in.mark(1024);
        BufferedImage img;
        ImageReader iir = (ImageReader) ImageIO.getImageReadersByFormatName("JPEG2000").next();
        ImageIO.setUseCache(false);
        ImageInputStream iin = ImageIO.createImageInputStream(in);
        iir.setInput(iin, true);
        try {
            img = iir.read(0);
        } catch (IOException e) {
            img = null;
            System.err.println("Couldn't read JPEG2000: " + e);
        }
        iir.dispose();
        iin.close();
        in.reset();
        return img;
    }

    /** Decode parameters from PDF dictionary and pass on to {@link phelps.imageio.plugins.Fax}. */
    static BufferedImage createFAX(Map imgdict, InputStream in, Color fillcolor, PDFReader pdfr) throws IOException {
        assert imgdict != null && in != null;
        Map dp = getDecodeParms(imgdict, pdfr);
        if (dp == null) new HashMap(3);
        Object o;
        int K = ((o = pdfr.getObject(dp.get("K"))) instanceof Number ? ((Number) o).intValue() : 0);
        if (K > 0) {
            PDF.sampledata("Group 3 mixed");
        }
        int cols = ((o = pdfr.getObject(dp.get("Columns"))) instanceof Number ? ((Number) o).intValue() : 1728);
        assert cols >= 1;
        int height = ((Number) pdfr.getObject(imgdict.get("Height"))).intValue();
        int rows = ((o = pdfr.getObject(dp.get("Rows"))) instanceof Number ? ((Number) o).intValue() : -height);
        boolean EndOfLine = ((o = pdfr.getObject(dp.get("EndOfLine"))) instanceof Boolean ? ((Boolean) o).booleanValue() : false);
        boolean EndOfBlock = ((o = pdfr.getObject(dp.get("EndOfBlock"))) instanceof Boolean ? ((Boolean) o).booleanValue() : true);
        boolean EncodedByteAlign = ((o = pdfr.getObject(dp.get("EncodedByteAlign"))) instanceof Boolean ? ((Boolean) o).booleanValue() : false);
        boolean BlackIs1 = ((o = pdfr.getObject(dp.get("BlackIs1"))) instanceof Boolean ? ((Boolean) o).booleanValue() : false);
        int DamagedRowsBeforeError = ((o = pdfr.getObject(dp.get("DamagedRowsBeforeError"))) instanceof Number ? ((Number) o).intValue() : 0);
        boolean swapbw = (java.util.Arrays.equals(PDFReader.A10, (Object[]) pdfr.getObject(imgdict.get("Decode"))));
        byte white = (byte) (BlackIs1 ^ swapbw ? 1 : 0);
        BufferedImage img = Fax.decode(K, cols, rows, EndOfLine, EndOfBlock, EncodedByteAlign, white, DamagedRowsBeforeError, in);
        boolean mask = ((o = pdfr.getObject(imgdict.get("ImageMask"))) instanceof Boolean ? ((Boolean) o).booleanValue() : false);
        if (mask && !Color.BLACK.equals(fillcolor)) {
            ColorModel cm = new IndexColorModel(1, 2, new int[] { 0, fillcolor.getRGB() }, 0, true, 0, DataBuffer.TYPE_BYTE);
            img = new BufferedImage(cm, img.getRaster(), false, new java.util.Hashtable());
        }
        return img;
    }

    /** Not implemented. */
    static BufferedImage createJBIG2() {
        PDF.sampledata("JBIG2");
        return null;
    }

    /**
	Create image from raw samples, in various bit depths (8, 4, 2, 1), in a variety of color spaces, with various numbers of samples per component (4,3,1).
  */
    static BufferedImage createRaw(Map imgdict, int w, int h, InputStream in, Color fillcolor, PDFReader pdfr) throws IOException {
        assert imgdict != null && w > 0 && h > 0 && in != null && fillcolor != null && pdfr != null;
        Boolean mask = (Boolean) pdfr.getObject(pdfr.getObject(imgdict.get("ImageMask")));
        int bpc = (mask == Boolean.TRUE ? 1 : ((Number) pdfr.getObject(pdfr.getObject(imgdict.get("BitsPerComponent")))).intValue());
        Object[] decode = (Object[]) pdfr.getObject(imgdict.get("Decode"));
        ColorModel cm = createRawColorModel(pdfr.getObject(imgdict.get("ColorSpace")), mask, bpc, fillcolor, pdfr);
        int spd = (cm instanceof IndexColorModel ? 1 : cm.getNumComponents());
        byte[] rawdata = readRawData(in, w * h * spd, decode);
        if (bpc == 8 && cm.getColorSpace() instanceof ColorSpaceCMYK) {
            rawdata = transcode4to3(rawdata, w, h, bpc);
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            spd = 3;
        }
        if (rawdata.length < (w * h * spd * bpc + bpc - 1) / 8) {
            System.out.println("short data: " + rawdata.length + " < " + (w * h * spd * bpc + bpc - 1) / 8 + ": " + w + "x" + h + " * " + spd + " @ " + bpc + " bpp");
            System.out.println(imgdict);
            return null;
        }
        WritableRaster r = createRawRaster(rawdata, w, h, bpc, spd);
        BufferedImage img = null;
        try {
            img = new BufferedImage(cm, r, false, new java.util.Hashtable());
        } catch (Exception e) {
            System.err.println(e);
            System.out.println("color model = " + cm);
            SampleModel sm = r.getSampleModel();
            System.out.println("sample model = " + sm);
            System.out.println("sample model instance of ComponentSampleModel " + (sm instanceof ComponentSampleModel));
            System.out.println("num bands = " + sm.getNumBands() + " ==? " + cm.getNumComponents() + " cm num comp");
            int[] nbits = cm.getComponentSize();
            for (int i = 0; i < nbits.length; i++) System.out.println("  " + sm.getSampleSize(i) + " >=? " + nbits[i]);
            System.out.println(r.getTransferType() + " ==? " + sm.getTransferType());
        }
        return img;
    }

    private static ColorModel createRawColorModel(Object csref, Boolean mask, int bpc, Color fillcolor, PDFReader pdfr) throws IOException {
        Object csobj = pdfr.getObject(csref);
        ColorSpace cs = pdfr.getColorSpace(csref, null, null);
        assert cs != null || (mask != null && mask.booleanValue());
        ColorModel cm;
        if (mask != null && mask.booleanValue()) {
            assert bpc == 1;
            cm = new IndexColorModel(1, 2, new int[] { fillcolor.getRGB(), 0 }, 0, true, 1, DataBuffer.TYPE_BYTE);
        } else if (csobj.getClass() == PDFReader.CLASS_ARRAY && ("Indexed".equals(((Object[]) csobj)[0]) || "I".equals(((Object[]) csobj)[0]))) {
            Object[] oa = (Object[]) csobj;
            int hival = ((Number) pdfr.getObject(oa[2])).intValue();
            Object cmap = pdfr.getObject(oa[3]);
            assert cmap.getClass() == PDFReader.CLASS_DICTIONARY || cmap.getClass() == PDFReader.CLASS_STRING;
            byte[] samp;
            if (cmap.getClass() == PDFReader.CLASS_DICTIONARY) {
                samp = pdfr.getInputStreamData(oa[3], false, false);
            } else {
                assert cmap.getClass() == PDFReader.CLASS_STRING;
                StringBuffer sb = (StringBuffer) cmap;
                samp = new byte[sb.length()];
                for (int i = 0, imax = sb.length(); i < imax; i++) samp[i] = (byte) (sb.charAt(i) & 0xff);
            }
            Object base = pdfr.getObject(oa[1]);
            if (null == base || "DeviceRGB".equals(base) || "RGB".equals(base)) {
            } else {
                ColorSpace bcs = pdfr.getColorSpace(base, null, null);
                int spd = bcs.getNumComponents(), sampcnt = samp.length / spd;
                byte[] brgb = new byte[sampcnt * 3];
                float[] fsamp = new float[spd];
                for (int i = 0, is = 0, id = 0; i < sampcnt; i++) {
                    for (int j = 0; j < spd; j++) fsamp[j] = (samp[is++] & 0xff) / 256f;
                    float[] rgb = bcs.toRGB(fsamp);
                    for (int j = 0; j < 3; j++) brgb[id++] = (byte) (rgb[j] * 256f);
                }
                samp = brgb;
            }
            cm = new IndexColorModel(bpc, Math.min(1 << bpc, hival + 1), samp, 0, false);
        } else if (cs.getNumComponents() == 1 && bpc < 8) {
            if (bpc == 1) cm = new IndexColorModel(1, 2, new int[] { 0x000000, 0xffffff }, 0, false, -1, DataBuffer.TYPE_BYTE); else if (bpc == 2) cm = new IndexColorModel(2, 4, new int[] { 0x000000, 0x404040, 0xc0c0c0, 0xffffff }, 0, false, -1, DataBuffer.TYPE_BYTE); else cm = new IndexColorModel(4, 16, new int[] { 0x000000, 0x111111, 0x222222, 0x333333, 0x444444, 0x555555, 0x666666, 0x777777, 0x888888, 0x999999, 0xaaaaaa, 0xbbbbbb, 0xcccccc, 0xdddddd, 0xeeeeee, 0xffffff }, 0, false, -1, DataBuffer.TYPE_BYTE);
        } else if (bpc == 8 || bpc == 4) {
            cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        } else {
            int spd = cs.getNumComponents();
            assert (spd == 3 || spd == 4) && (bpc == 2 || bpc == 1) : "bpc=" + bpc;
            int po2 = (1 << bpc);
            byte[] b = new byte[(1 << (bpc * spd)) * spd];
            float red = 0f, green = 0f, blue = 0f, black = 0f, inc = (float) (1f / po2);
            float[] comp = new float[4];
            Color c;
            int i = 0;
            for (int r = 0; r < po2; r++, red += inc, green = 0f, blue = 0f, black = 0f) {
                comp[0] = red;
                for (int g = 0; g < po2; g++, green += inc, blue = 0f, black = 0f) {
                    comp[1] = green;
                    for (int bl = 0; bl < po2; bl++, blue += inc, black = 0f) {
                        comp[2] = blue;
                        for (int k = 0; k < po2; k++, black += inc) {
                            comp[3] = black;
                            c = new Color(cs, comp, 0f);
                            b[i++] = (byte) c.getRed();
                            b[i++] = (byte) c.getGreen();
                            b[i++] = (byte) c.getBlue();
                            if (spd == 3) break;
                        }
                    }
                }
            }
            assert i == b.length : i + " vs " + b.length;
            cm = new IndexColorModel(bpc, b.length / spd, b, 0, false);
        }
        return cm;
    }

    private static byte[] readRawData(InputStream in, int estlength, Object[] decode) throws IOException {
        byte[] buf = new byte[8 * 1024];
        ByteArrayOutputStream bout = new ByteArrayOutputStream(estlength);
        for (int hunk; (hunk = in.read(buf)) != -1; ) bout.write(buf, 0, hunk);
        bout.close();
        byte[] rawdata = bout.toByteArray();
        if (decode != null) {
            double[] da = new double[decode.length];
            for (int i = 0, imax = decode.length; i < imax; i++) da[i] = ((Number) decode[i]).doubleValue();
            if (da.length == 2 && da[0] == 1.0 && da[1] == 0.0) {
                for (int i = 0, imax = rawdata.length; i < imax; i++) rawdata[i] ^= 0xff;
            }
        }
        return rawdata;
    }

    /**
	Work around Java AffineTransformOp bug on 4-component color spaces by transcoding data to RGB.
	Just handles 8-bit CMYK case for now.
  */
    private static byte[] transcode4to3(byte[] data, int w, int h, int bpc) {
        assert bpc == 8 : bpc;
        assert data.length == w * h * 4 : data.length + " vs " + (w * h * 4);
        byte[] newdata = new byte[w * h * 3];
        for (int i = 0, imax = data.length, j = 0; i < imax; i += 4, j += 3) {
            int k = data[i + 3] & 0xff;
            newdata[j] = (byte) (255 - Math.min(255, (data[i] & 0xff) + k));
            newdata[j + 1] = (byte) (255 - Math.min(255, (data[i + 1] & 0xff) + k));
            newdata[j + 2] = (byte) (255 - Math.min(255, (data[i + 2] & 0xff) + k));
        }
        return newdata;
    }

    private static WritableRaster createRawRaster(byte[] rawdata, int w, int h, int bpc, int spd) {
        WritableRaster r;
        int[] offs = new int[spd];
        for (int i = 0, imax = offs.length; i < imax; i++) offs[i] = i;
        if (bpc == 8 || (bpc == 2 && spd == 4)) {
            r = Raster.createInterleavedRaster(new DataBufferByte(rawdata, rawdata.length), w, h, w * spd, spd, offs, null);
        } else if (spd == 1) {
            assert bpc == 4 || bpc == 2 || bpc == 1;
            r = Raster.createPackedRaster(new DataBufferByte(rawdata, rawdata.length), w, h, bpc, null);
        } else if (bpc == 4 || (bpc == 1 && spd == 4)) {
            byte[] newdata = new byte[w * spd * h];
            for (int y = 0, newi = 0, base = 0, stride = (w * spd * bpc + 7) / 8; y < h; y++, base = y * stride) {
                for (int x = 0; x < w; x += 2) {
                    byte b = rawdata[base++];
                    newdata[newi++] = (byte) ((b >> 4) & 0xf);
                    if (x + 1 < w) newdata[newi++] = (byte) ((b & 0xf));
                }
            }
            r = Raster.createInterleavedRaster(new DataBufferByte(newdata, newdata.length), w, h, w * spd, spd, offs, null);
            PDF.sampledata("4 bpc x 2 bytes: " + rawdata.length + " => " + newdata.length);
        } else if (bpc == 2) {
            byte[] newdata = new byte[w * h];
            int valid = 0, vbpc = 0;
            for (int y = 0, newi = 0, base = 0, stride = (w * spd * bpc + 7) / 8; y < h; y++, base = y * stride) {
                for (int x = 0; x < w; x++) {
                    if (valid < 6) {
                        vbpc = (vbpc << 8) | rawdata[base++];
                        valid += 8;
                    }
                    newdata[newi++] = (byte) ((vbpc >> (valid - 6)) & 0x3f);
                    valid -= 6;
                }
            }
            r = Raster.createInterleavedRaster(new DataBufferByte(newdata, newdata.length), w, h, w * spd, spd, new int[] { 0 }, null);
            PDF.sampledata("2 bpc packed BYTE: " + rawdata.length + " => " + newdata.length);
        } else {
            assert bpc == 1;
            byte[] newdata = new byte[w * h];
            int valid = 0, vbpc = 0;
            for (int y = 0, newi = 0, base = 0, stride = (w * spd * bpc + 7) / 8; y < h; y++, base = y * stride) {
                for (int x = 0; x < w; x++) {
                    if (valid < 3) {
                        vbpc = (vbpc << 8) | rawdata[base++];
                        valid += 8;
                    }
                    newdata[newi++] = (byte) ((vbpc >> (valid - 3)) & 7);
                    valid -= 3;
                }
            }
            r = Raster.createInterleavedRaster(new DataBufferByte(newdata, newdata.length), w, h, w * spd, spd, new int[] { 0 }, null);
            PDF.sampledata("1 bit packed byte: " + rawdata.length + " => " + newdata.length);
        }
        return r;
    }
}
