package org.freehep.graphicsio.pdf;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.freehep.graphicsio.ImageConstants;
import org.freehep.util.io.ASCII85OutputStream;
import org.freehep.util.io.ASCIIHexOutputStream;
import org.freehep.util.io.CountedByteOutputStream;
import org.freehep.util.io.FinishableOutputStream;
import org.freehep.util.io.FlateOutputStream;

/**
 * This class allows you to write/print into a PDFStream. Several methods are
 * available to specify the content of a page, image. This class performs some
 * error checking, while writing the stream.
 * <p>
 * The stream allows to write dictionary entries. The /Length entry is written
 * automatically, referencing an object which will also be written just after
 * the stream is closed and the length is calculated.
 * <p>
 * 
 * @author Mark Donszelmann
 * @version $Id: PDFStream.java 12626 2007-06-08 22:23:13Z duns $
 */
public class PDFStream extends PDFDictionary implements PDFConstants {

    private String name;

    private PDFObject object;

    private boolean dictionaryOpen;

    private OutputStream[] stream;

    private CountedByteOutputStream byteCountStream;

    private String[] encode;

    PDFStream(PDF pdf, PDFByteWriter writer, String name, PDFObject parent, String[] encode) throws IOException {
        super(pdf, writer);
        this.name = name;
        object = parent;
        if (object == null) System.err.println("PDFWriter: 'PDFStream' cannot have a null parent");
        dictionaryOpen = true;
        this.encode = encode;
    }

    /**
     * Starts the stream, writes out the filters using the preset encoding, and
     * encodes the stream.
     */
    private void startStream() throws IOException {
        startStream(encode);
    }

    /**
     * Starts the stream, writes out the filters using the given encoding, and
     * encodes the stream.
     */
    private void startStream(String[] encode) throws IOException {
        if (dictionaryOpen) {
            PDFName[] filters = decodeFilters(encode);
            if (filters != null) entry("Filter", filters);
            super.close();
            dictionaryOpen = false;
            out.printPlain("stream\n");
            byteCountStream = new CountedByteOutputStream(out);
            stream = openFilters(byteCountStream, encode);
        }
    }

    private void write(int b) throws IOException {
        startStream();
        stream[0].write(b);
    }

    private void write(byte[] b) throws IOException {
        for (int i = 0; i < b.length; i++) {
            write((int) b[i]);
        }
    }

    private static PDFName[] decodeFilters(String[] encode) {
        PDFName[] filters = null;
        if ((encode != null) && (encode.length != 0)) {
            filters = new PDFName[encode.length];
            for (int i = 0; i < filters.length; i++) {
                filters[i] = new PDFName(encode[encode.length - i - 1] + "Decode");
            }
        }
        return filters;
    }

    private static OutputStream[] openFilters(OutputStream s, String[] filters) {
        OutputStream[] os;
        if ((filters != null) && (filters.length != 0)) {
            os = new OutputStream[filters.length + 1];
            os[os.length - 1] = s;
            for (int i = os.length - 2; i >= 0; i--) {
                if (filters[i].equals("ASCIIHex")) {
                    os[i] = new ASCIIHexOutputStream(os[i + 1]);
                } else if (filters[i].equals("ASCII85")) {
                    os[i] = new ASCII85OutputStream(os[i + 1]);
                } else if (filters[i].equals("Flate")) {
                    os[i] = new FlateOutputStream(os[i + 1]);
                } else if (filters[i].equals("DCT")) {
                    os[i] = os[i + 1];
                } else {
                    System.err.println("PDFWriter: unknown stream format: " + filters[i]);
                }
            }
        } else {
            os = new OutputStream[1];
            os[0] = s;
        }
        return os;
    }

    private static void closeFilters(OutputStream[] s) throws IOException {
        for (int i = 0; i < s.length - 1; i++) {
            s[i].flush();
            if (s[i] instanceof FinishableOutputStream) {
                ((FinishableOutputStream) s[i]).finish();
            }
        }
        s[s.length - 1].flush();
    }

    private void write(String s) throws IOException {
        byte[] b = s.getBytes("ISO-8859-1");
        for (int i = 0; i < b.length; i++) {
            write(b[i]);
        }
    }

    void close() throws IOException {
        closeFilters(stream);
        stream = null;
        out.printPlain("\nendstream");
        out.println();
        object.close();
        if (gStates > 0) {
            System.err.println("PDFStream: unbalanced saves()/restores(), too many saves: " + gStates);
        }
    }

    String getName() {
        return name;
    }

    public int getLength() {
        return byteCountStream.getCount();
    }

    public void print(String s) throws IOException {
        write(s);
    }

    public void println(String s) throws IOException {
        write(s);
        write(EOL);
    }

    public void comment(String comment) throws IOException {
        println("% " + comment);
    }

    private int gStates = 0;

    public void save() throws IOException {
        println("q");
        gStates++;
    }

    public void restore() throws IOException {
        if (gStates <= 0) {
            System.err.println("PDFStream: unbalanced saves()/restores(), too many restores");
        }
        gStates--;
        println("Q");
    }

    public void matrix(AffineTransform xform) throws IOException {
        matrix(xform.getScaleX(), xform.getShearY(), xform.getShearX(), xform.getScaleY(), xform.getTranslateX(), xform.getTranslateY());
    }

    public void matrix(double m00, double m10, double m01, double m11, double m02, double m12) throws IOException {
        println(PDFUtil.fixedPrecision(m00) + " " + PDFUtil.fixedPrecision(m10) + " " + PDFUtil.fixedPrecision(m01) + " " + PDFUtil.fixedPrecision(m11) + " " + PDFUtil.fixedPrecision(m02) + " " + PDFUtil.fixedPrecision(m12) + " cm");
    }

    public void width(double width) throws IOException {
        println(PDFUtil.fixedPrecision(width) + " w");
    }

    public void cap(int capStyle) throws IOException {
        println(capStyle + " J");
    }

    public void join(int joinStyle) throws IOException {
        println(joinStyle + " j");
    }

    public void mitterLimit(double limit) throws IOException {
        println(PDFUtil.fixedPrecision(limit) + " M");
    }

    public void dash(int[] dash, double phase) throws IOException {
        print("[");
        for (int i = 0; i < dash.length; i++) {
            print(" " + PDFUtil.fixedPrecision(dash[i]));
        }
        println("] " + PDFUtil.fixedPrecision(phase) + " d");
    }

    public void dash(float[] dash, double phase) throws IOException {
        print("[");
        for (int i = 0; i < dash.length; i++) {
            print(" " + PDFUtil.fixedPrecision(dash[i]));
        }
        println("] " + PDFUtil.fixedPrecision(phase) + " d");
    }

    public void flatness(double flatness) throws IOException {
        println(PDFUtil.fixedPrecision(flatness) + " i");
    }

    public void state(PDFName stateDictionary) throws IOException {
        println(stateDictionary + " gs");
    }

    public void cubic(double x1, double y1, double x2, double y2, double x3, double y3) throws IOException {
        println(PDFUtil.fixedPrecision(x1) + " " + PDFUtil.fixedPrecision(y1) + " " + PDFUtil.fixedPrecision(x2) + " " + PDFUtil.fixedPrecision(y2) + " " + PDFUtil.fixedPrecision(x3) + " " + PDFUtil.fixedPrecision(y3) + " c");
    }

    public void cubicV(double x2, double y2, double x3, double y3) throws IOException {
        println(PDFUtil.fixedPrecision(x2) + " " + PDFUtil.fixedPrecision(y2) + " " + PDFUtil.fixedPrecision(x3) + " " + PDFUtil.fixedPrecision(y3) + " v");
    }

    public void cubicY(double x1, double y1, double x3, double y3) throws IOException {
        println(PDFUtil.fixedPrecision(x1) + " " + PDFUtil.fixedPrecision(y1) + " " + PDFUtil.fixedPrecision(x3) + " " + PDFUtil.fixedPrecision(y3) + " y");
    }

    public void move(double x, double y) throws IOException {
        println(PDFUtil.fixedPrecision(x) + " " + PDFUtil.fixedPrecision(y) + " m");
    }

    public void line(double x, double y) throws IOException {
        println(PDFUtil.fixedPrecision(x) + " " + PDFUtil.fixedPrecision(y) + " l");
    }

    public void closePath() throws IOException {
        println("h");
    }

    public void rectangle(double x, double y, double width, double height) throws IOException {
        println(PDFUtil.fixedPrecision(x) + " " + PDFUtil.fixedPrecision(y) + " " + PDFUtil.fixedPrecision(width) + " " + PDFUtil.fixedPrecision(height) + " re");
    }

    public void stroke() throws IOException {
        println("S");
    }

    public void closeAndStroke() throws IOException {
        println("s");
    }

    public void fill() throws IOException {
        println("f");
    }

    public void fillEvenOdd() throws IOException {
        println("f*");
    }

    public void fillAndStroke() throws IOException {
        println("B");
    }

    public void fillEvenOddAndStroke() throws IOException {
        println("B*");
    }

    public void closeFillAndStroke() throws IOException {
        println("b");
    }

    public void closeFillEvenOddAndStroke() throws IOException {
        println("b*");
    }

    public void endPath() throws IOException {
        println("n");
    }

    public void clip() throws IOException {
        println("W");
    }

    public void clipEvenOdd() throws IOException {
        println("W*");
    }

    private boolean textOpen = false;

    public void beginText() throws IOException {
        if (textOpen) System.err.println("PDFStream: nested beginText() not allowed.");
        println("BT");
        textOpen = true;
    }

    public void endText() throws IOException {
        if (!textOpen) System.err.println("PDFStream: unbalanced use of beginText()/endText().");
        println("ET");
        textOpen = false;
    }

    public void charSpace(double charSpace) throws IOException {
        println(PDFUtil.fixedPrecision(charSpace) + " Tc");
    }

    public void wordSpace(double wordSpace) throws IOException {
        println(PDFUtil.fixedPrecision(wordSpace) + " Tw");
    }

    public void scale(double scale) throws IOException {
        println(PDFUtil.fixedPrecision(scale) + " Tz");
    }

    public void leading(double leading) throws IOException {
        println(PDFUtil.fixedPrecision(leading) + " TL");
    }

    private boolean fontWasSet = false;

    public void font(PDFName fontName, double size) throws IOException {
        println(fontName + " " + PDFUtil.fixedPrecision(size) + " Tf");
        fontWasSet = true;
    }

    public void rendering(int mode) throws IOException {
        println(mode + " Tr");
    }

    public void rise(double rise) throws IOException {
        println(PDFUtil.fixedPrecision(rise) + " Ts");
    }

    public void text(double x, double y) throws IOException {
        println(PDFUtil.fixedPrecision(x) + " " + PDFUtil.fixedPrecision(y) + " Td");
    }

    public void textLeading(double x, double y) throws IOException {
        println(PDFUtil.fixedPrecision(x) + " " + PDFUtil.fixedPrecision(y) + " TD");
    }

    public void textMatrix(double a, double b, double c, double d, double e, double f) throws IOException {
        println(PDFUtil.fixedPrecision(a) + " " + PDFUtil.fixedPrecision(b) + " " + PDFUtil.fixedPrecision(c) + " " + PDFUtil.fixedPrecision(d) + " " + PDFUtil.fixedPrecision(e) + " " + PDFUtil.fixedPrecision(f) + " Tm");
    }

    public void textLine() throws IOException {
        println("T*");
    }

    public void show(String text) throws IOException {
        if (!fontWasSet) System.err.println("PDFStream: cannot use Text Showing operator before font is set.");
        if (!textOpen) System.err.println("PDFStream: Text Showing operator only allowed inside Text section.");
        println("(" + PDFUtil.escape(text) + ") Tj");
    }

    public void showLine(String text) throws IOException {
        if (!fontWasSet) System.err.println("PDFStream: cannot use Text Showing operator before font is set.");
        if (!textOpen) System.err.println("PDFStream: Text Showing operator only allowed inside Text section.");
        println("(" + PDFUtil.escape(text) + ") '");
    }

    public void showLine(double wordSpace, double charSpace, String text) throws IOException {
        if (!fontWasSet) System.err.println("PDFStream: cannot use Text Showing operator before font is set.");
        if (!textOpen) System.err.println("PDFStream: Text Showing operator only allowed inside Text section.");
        println(PDFUtil.fixedPrecision(wordSpace) + " " + PDFUtil.fixedPrecision(charSpace) + " (" + PDFUtil.escape(text) + ") \"");
    }

    public void show(Object[] array) throws IOException {
        print("[");
        for (int i = 0; i < array.length; i++) {
            Object object = array[i];
            if (object instanceof String) {
                print(" (" + PDFUtil.escape(object.toString()) + ")");
            } else if (object instanceof Integer) {
                print(" " + ((Integer) object).intValue());
            } else if (object instanceof Double) {
                print(" " + ((Double) object).doubleValue());
            } else {
                System.err.println("PDFStream: input array of operator TJ may only contain objects of type 'String', 'Integer' or 'Double'");
            }
        }
        println("] TJ");
    }

    public void glyph(double wx, double wy) throws IOException {
        println(PDFUtil.fixedPrecision(wx) + " " + PDFUtil.fixedPrecision(wy) + " d0");
    }

    public void glyph(double wx, double wy, double llx, double lly, double urx, double ury) throws IOException {
        println(PDFUtil.fixedPrecision(wx) + " " + PDFUtil.fixedPrecision(wy) + " " + PDFUtil.fixedPrecision(llx) + " " + PDFUtil.fixedPrecision(lly) + " " + PDFUtil.fixedPrecision(urx) + " " + PDFUtil.fixedPrecision(ury) + " d1");
    }

    public void colorSpace(PDFName colorSpace) throws IOException {
        println(colorSpace + " cs");
    }

    public void colorSpaceStroke(PDFName colorSpace) throws IOException {
        println(colorSpace + " CS");
    }

    public void colorSpace(double[] color) throws IOException {
        for (int i = 0; i < color.length; i++) {
            print(" " + color[i]);
        }
        println(" scn");
    }

    public void colorSpaceStroke(double[] color) throws IOException {
        for (int i = 0; i < color.length; i++) {
            print(" " + color[i]);
        }
        println(" SCN");
    }

    public void colorSpace(double[] color, PDFName name) throws IOException {
        if (color != null) {
            for (int i = 0; i < color.length; i++) {
                print(PDFUtil.fixedPrecision(color[i]) + " ");
            }
        }
        println(name + " scn");
    }

    public void colorSpaceStroke(double[] color, PDFName name) throws IOException {
        if (color != null) {
            for (int i = 0; i < color.length; i++) {
                print(PDFUtil.fixedPrecision(color[i]) + " ");
            }
        }
        println(name + " SCN");
    }

    public void colorSpace(double g) throws IOException {
        println(PDFUtil.fixedPrecision(g) + " g");
    }

    public void colorSpaceStroke(double g) throws IOException {
        println(PDFUtil.fixedPrecision(g) + " G");
    }

    public void colorSpace(double r, double g, double b) throws IOException {
        println(PDFUtil.fixedPrecision(r) + " " + PDFUtil.fixedPrecision(g) + " " + PDFUtil.fixedPrecision(b) + " rg");
    }

    public void colorSpaceStroke(double r, double g, double b) throws IOException {
        println(PDFUtil.fixedPrecision(r) + " " + PDFUtil.fixedPrecision(g) + " " + PDFUtil.fixedPrecision(b) + " RG");
    }

    public void colorSpace(double c, double m, double y, double k) throws IOException {
        println(PDFUtil.fixedPrecision(c) + " " + PDFUtil.fixedPrecision(m) + " " + PDFUtil.fixedPrecision(y) + " " + PDFUtil.fixedPrecision(k) + " k");
    }

    public void colorSpaceStroke(double c, double m, double y, double k) throws IOException {
        println(PDFUtil.fixedPrecision(c) + " " + PDFUtil.fixedPrecision(m) + " " + PDFUtil.fixedPrecision(y) + " " + PDFUtil.fixedPrecision(k) + " K");
    }

    public void shade(PDFName name) throws IOException {
        println(name + " sh");
    }

    /**
     * returns the decode-format for an image -format
     *
     * @param encode {@link ImageConstants#ZLIB} or {@link ImageConstants#JPG}
     * @return {@link #decodeFilters(String[])}
     */
    private PDFName[] getFilterName(String encode) {
        if (ImageConstants.ZLIB.equals(encode)) {
            return decodeFilters(new String[] { ImageConstants.ENCODING_FLATE, ImageConstants.ENCODING_ASCII85 });
        }
        if (ImageConstants.JPG.equals(encode)) {
            return decodeFilters(new String[] { ImageConstants.ENCODING_DCT, ImageConstants.ENCODING_ASCII85 });
        }
        throw new IllegalArgumentException("unknown image encoding " + encode + " for PDFStream");
    }

    /**
     * Image convenience function (see Table 4.35). Ouputs the data of the image
     * using "DeviceRGB" colorspace, and the requested encodings
     *
     * @param image Image to write
     * @param bkg Background color, null for transparent image
     * @param encode {@link org.freehep.graphicsio.ImageConstants#ZLIB} or {@link org.freehep.graphicsio.ImageConstants#JPG}
     * @throws java.io.IOException thrown by ImageBytes
     */
    public void image(RenderedImage image, Color bkg, String encode) throws IOException {
        ImageBytes bytes = new ImageBytes(image, bkg, encode, ImageConstants.COLOR_MODEL_RGB);
        entry("Width", image.getWidth());
        entry("Height", image.getHeight());
        entry("ColorSpace", pdf.name("DeviceRGB"));
        entry("BitsPerComponent", 8);
        entry("Filter", getFilterName(bytes.getFormat()));
        write(bytes.getBytes());
    }

    public void imageMask(RenderedImage image, String encode) throws IOException {
        ImageBytes bytes = new ImageBytes(image, null, encode, ImageConstants.COLOR_MODEL_A);
        entry("Width", image.getWidth());
        entry("Height", image.getHeight());
        entry("BitsPerComponent", 8);
        entry("ColorSpace", pdf.name("DeviceGray"));
        entry("Filter", getFilterName(bytes.getFormat()));
        write(bytes.getBytes());
    }

    /**
     * Inline Image convenience function (see Table 4.39 and 4.40). Ouputs the
     * data of the image using "DeviceRGB" colorspace, and the requested
     * encoding.

     * @param image Image to write
     * @param bkg Background color, null for transparent image
     * @param encode {@link org.freehep.graphicsio.ImageConstants#ZLIB} or {@link org.freehep.graphicsio.ImageConstants#JPG}
     * @throws java.io.IOException thrown by ImageBytes
     */
    public void inlineImage(RenderedImage image, Color bkg, String encode) throws IOException {
        ImageBytes bytes = new ImageBytes(image, bkg, ImageConstants.JPG, ImageConstants.COLOR_MODEL_RGB);
        println("BI");
        imageInfo("Width", image.getWidth());
        imageInfo("Height", image.getHeight());
        imageInfo("ColorSpace", pdf.name("DeviceRGB"));
        imageInfo("BitsPerComponent", 8);
        imageInfo("Filter", getFilterName(bytes.getFormat()));
        print("ID\n");
        write(bytes.getBytes());
        println("\nEI");
    }

    private void imageInfo(String key, int number) throws IOException {
        println("/" + key + " " + number);
    }

    private void imageInfo(String key, PDFName name) throws IOException {
        println("/" + key + " " + name);
    }

    private void imageInfo(String key, Object[] array) throws IOException {
        print("/" + key + " [");
        for (int i = 0; i < array.length; i++) {
            print(" " + array[i]);
        }
        println("]");
    }

    /**
     * Draws the <i>points</i> of the shape using path <i>construction</i>
     * operators. The path is neither stroked nor filled.
     * 
     * @return true if even-odd winding rule should be used, false if non-zero
     *         winding rule should be used.
     */
    public boolean drawPath(Shape s) throws IOException {
        PDFPathConstructor path = new PDFPathConstructor(this);
        return path.addPath(s);
    }

    public void xObject(PDFName name) throws IOException {
        println(name + " Do");
    }

    private boolean compatibilityOpen = false;

    public void beginCompatibility() throws IOException {
        if (compatibilityOpen) System.err.println("PDFStream: nested use of Compatibility sections not allowed.");
        println("BX");
        compatibilityOpen = true;
    }

    public void endCompatibility() throws IOException {
        if (!compatibilityOpen) System.err.println("PDFStream: unbalanced use of begin/endCompatibilty().");
        println("EX");
        compatibilityOpen = false;
    }
}
