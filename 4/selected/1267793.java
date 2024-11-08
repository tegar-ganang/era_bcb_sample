package org.fit.pdfdom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.exceptions.WrappedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

/**
 * A generic tree of boxes created from a PDF file. It processes the PDF document and calls
 * the appropriate abstract methods in order to render a page, text box, etc. The particular
 * implementations are expected to implement these actions in order to build the resulting
 * document tree.
 * 
 * @author burgetr
 */
public abstract class PDFBoxTree extends PDFTextStripper {

    /** Length units used in the generated CSS */
    public static final String UNIT = "pt";

    /** Known font names that are recognized in the PDF files */
    protected static String[] cssFontFamily = { "Times New Roman", "Times", "Garamond", "Helvetica", "Arial", "Arial Narrow", "Verdana", "Courier New", "MS Sans Serif" };

    /** Known font subtypes recognized in PDF files */
    protected static String[] pdFontType = { "normal", "roman", "bold", "italic", "bolditalic" };

    /** Font weights corresponding to the font subtypes in {@link PDFDomTree#pdFontType} */
    protected static String[] cssFontWeight = { "normal", "normal", "bold", "normal", "bold" };

    /** Font styles corresponding to the font subtypes in {@link PDFDomTree#pdFontType} */
    protected static String[] cssFontStyle = { "normal", "normal", "normal", "italic", "italic" };

    /** When set to <code>true</code>, the graphics in the PDF file will be ignored. */
    protected boolean disableGraphics = false;

    /** When set to <code>true</code>, the embedded images will be ignored. */
    protected boolean disableImages = false;

    /** When set to <code>true</code>, the image data will not be transferred to the HTML data: url. */
    protected boolean disableImageData = false;

    /** First page to be processed */
    protected int startPage;

    /** Last page to be processed */
    protected int endPage;

    /** The PDF page currently being processed */
    protected PDPage pdpage;

    /** Current text coordinates (the coordinates of the last encountered text box). */
    protected float cur_x;

    /** Current text coordinates (the coordinates of the last encountered text box). */
    protected float cur_y;

    /** Previous positioned text. */
    protected TextPosition lastText = null;

    /** The text box currently being created. */
    protected StringBuilder textLine;

    /** Current graphics path */
    protected Vector<RectPath> graphicsPath;

    /** The style of the future box being modified by the operators */
    protected BoxStyle style;

    /** The style of the text line being created */
    protected BoxStyle curstyle;

    /** The current stroking color set by the operators */
    protected String strokingColor;

    /** The current graphics line width set by the operators */
    protected float lineWidth;

    public PDFBoxTree() throws IOException {
        super.setSortByPosition(true);
        super.setSuppressDuplicateOverlappingText(true);
        init();
    }

    /**
     * Internal initialization.
     * @throws ParserConfigurationException
     */
    private void init() {
        style = new BoxStyle(UNIT);
        textLine = new StringBuilder();
        strokingColor = null;
        lineWidth = 0;
        graphicsPath = new Vector<RectPath>();
        startPage = 0;
        endPage = Integer.MAX_VALUE;
    }

    /**
     * Processes all pages of the PDF document with the parser.
     * @param document The document to be processed.
     * @throws IOException
     */
    public void processDocument(PDDocument document) throws IOException {
        processDocument(document, 0, Integer.MAX_VALUE);
    }

    /**
     * Processes a range of pages of the PDF document with the parser.
     * @param document The document to be processed.
     * @param startPage The first page to be processed.
     * @param endPage The last page to be processed 
     * @throws IOException
     */
    public void processDocument(PDDocument document, int startPage, int endPage) throws IOException {
        resetEngine();
        this.startPage = startPage;
        this.endPage = endPage;
        if (document.isEncrypted()) {
            try {
                document.decrypt("");
            } catch (CryptographyException e) {
                throw new WrappedIOException("Error decrypting document, details: ", e);
            } catch (InvalidPasswordException e) {
                throw new WrappedIOException("Error: document is encrypted", e);
            }
        }
        List<?> allPages = document.getDocumentCatalog().getAllPages();
        for (int i = startPage; i <= endPage; i++) {
            if (i >= 0 && i < allPages.size()) {
                PDPage page = (PDPage) allPages.get(i);
                PDStream contents = page.getContents();
                if (contents != null) {
                    processPage(page, contents.getStream());
                }
            } else if (i >= allPages.size()) break;
        }
    }

    protected void processPage(PDPage page, COSStream content) throws IOException {
        pdpage = page;
        startNewPage();
        processStream(page, page.findResources(), content);
        finishBox();
    }

    /**
     * Checks whether the graphics processing is disabled.
     * @return <code>true</code> when the graphics processing is disabled in the parser configuration.
     */
    public boolean getDisableGraphics() {
        return disableGraphics;
    }

    /**
     * Disables the processing of the graphic operators in the PDF files.
     * @param disableGraphics when set to <code>true</code> the graphics is ignored in the source file.
     */
    public void setDisableGraphics(boolean disableGraphics) {
        this.disableGraphics = disableGraphics;
    }

    /**
     * Checks whether processing of embedded images is disabled.
     * @return <code>true</code> when the processing of embedded images is disabled in the parser configuration.
     */
    public boolean getDisableImages() {
        return disableImages;
    }

    /**
     * Disables the processing of images contained in the PDF files.
     * @param disableImages when set to <code>true</code> the images are ignored in the source file.
     */
    public void setDisableImages(boolean disableImages) {
        this.disableImages = disableImages;
    }

    /**
     * Checks whether the copying of image data is disabled.
     * @return <code>true</code> when the copying of image data is disabled in the parser configuration.
     */
    public boolean getDisableImageData() {
        return disableImageData;
    }

    /**
     * Disables the copying the image data to the resulting DOM tree.
     * @param disableImageData when set to <code>true</code> the image data is not copied to the document tree.
     * The eventual <code>img</code> elements will have an empty <code>src</code> attribute. 
     */
    public void setDisableImageData(boolean disableImageData) {
        this.disableImageData = disableImageData;
    }

    @Override
    public int getStartPage() {
        return startPage;
    }

    @Override
    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    @Override
    public int getEndPage() {
        return endPage;
    }

    @Override
    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }

    /**
     * Adds a new page to the resulting document and makes it a current (active) page.
     */
    protected abstract void startNewPage();

    /**
     * Creates a new text box in the current page. The style and position of the text are contained
     * in the {@link PDFBoxTree#curstyle} property. 
     * @param data The text contents.
     */
    protected abstract void renderText(String data);

    /**
     * Adds a rectangle to the current page on the specified position.
     * @param x the X coordinate of the rectangle
     * @param y the Y coordinate of the rectangle
     * @param width the width coordinate of the rectangle
     * @param height the height coordinate of the rectangle
     * @param stroke should there be a stroke around?
     * @param fill should the rectangle be filled?
     */
    protected abstract void renderRectangle(float x, float y, float width, float height, boolean stroke, boolean fill);

    /**
     * Adds an image to the current page.
     * @param x the X coordinate of the image
     * @param y the Y coordinate of the image
     * @param width the width coordinate of the image
     * @param height the height coordinate of the image
     * @param type the image type: <code>"png"</code> or <code>"jpeg"</code>
     * @param data the image data depending on the specified type
     * @return
     */
    protected abstract void renderImage(float x, float y, float width, float height, String mimetype, byte[] data);

    @Override
    protected void processOperator(PDFOperator operator, List<COSBase> arguments) throws IOException {
        String operation = operator.getOperation();
        if (operation.equals("g")) {
            float gray = floatValue(arguments.get(0));
            style.setColor(colorString(gray, gray, gray));
        } else if (operation.equals("G")) {
            float gray = floatValue(arguments.get(0));
            strokingColor = colorString(gray, gray, gray);
        } else if (operation.equals("rg")) {
            style.setColor(colorString(floatValue(arguments.get(0)), floatValue(arguments.get(1)), floatValue(arguments.get(2))));
        } else if (operation.equals("RG")) {
            strokingColor = colorString(floatValue(arguments.get(0)), floatValue(arguments.get(1)), floatValue(arguments.get(2)));
        } else if (operation.equals("scn")) {
            if (arguments.size() == 3) {
                style.setColor(colorString(floatValue(arguments.get(0)), floatValue(arguments.get(1)), floatValue(arguments.get(2))));
            } else System.err.println("Warning: scn: unsupported color specification: " + arguments);
        } else if (operation.equals("SCN")) {
            if (arguments.size() == 3) {
                strokingColor = colorString(floatValue(arguments.get(0)), floatValue(arguments.get(1)), floatValue(arguments.get(2)));
            } else System.err.println("Warning: SCN: unsupported color specification: " + arguments);
        } else if (operation.equals("Tw")) {
            style.setWordSpacing(getLength(arguments.get(0)));
        } else if (operation.equals("Tc")) {
            style.setLetterSpacing(getLength(arguments.get(0)));
        } else if (operation.equals("re")) {
            if (!disableGraphics) {
                if (arguments.size() == 4) {
                    float x = getLength(arguments.get(0));
                    float y = getLength(arguments.get(1));
                    float width = getLength(arguments.get(2));
                    float height = getLength(arguments.get(3));
                    Matrix spos = new Matrix();
                    spos.setValue(2, 0, x);
                    spos.setValue(2, 1, y);
                    Matrix epos = new Matrix();
                    epos.setValue(2, 0, x + width);
                    epos.setValue(2, 1, y + height);
                    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                    Matrix sposXctm = spos.multiply(ctm);
                    Matrix eposXctm = epos.multiply(ctm);
                    width = eposXctm.getXPosition() - sposXctm.getXPosition();
                    height = eposXctm.getYPosition() - sposXctm.getYPosition();
                    graphicsPath.add(new RectPath(sposXctm.getXPosition(), sposXctm.getYPosition(), width, height));
                }
            }
        } else if (operation.equals("f") || operation.equals("F") || operation.equals("f*")) {
            for (RectPath rect : graphicsPath) {
                renderRectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), false, true);
            }
            graphicsPath.removeAllElements();
        } else if (operation.equals("s") || operation.equals("S")) {
            for (RectPath rect : graphicsPath) {
                renderRectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), true, false);
            }
            graphicsPath.removeAllElements();
        } else if (operation.equals("B") || operation.equals("B*") || operation.equals("b") || operation.equals("b*")) {
            for (RectPath rect : graphicsPath) {
                renderRectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), true, true);
            }
            graphicsPath.removeAllElements();
        } else if (operation.equals("n")) {
            graphicsPath.removeAllElements();
        } else if (operation.equals("Do")) {
            if (!disableImages) {
                COSName objectName = (COSName) arguments.get(0);
                Map<?, ?> xobjects = getResources().getXObjects();
                PDXObject xobject = (PDXObject) xobjects.get(objectName.getName());
                if (xobject instanceof PDXObjectImage) {
                    PDXObjectImage image = (PDXObjectImage) xobject;
                    byte[] data = getImageData(image);
                    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                    float x = ctm.getXPosition();
                    float y = ctm.getYPosition();
                    float width = ctm.getXScale();
                    float height = ctm.getYScale();
                    if (width < 0) {
                        width = -width;
                        x -= width;
                    }
                    if (height < 0) {
                        height = -height;
                        y -= height;
                    }
                    String mime;
                    if (image.getSuffix().equalsIgnoreCase("jpg") || image.getSuffix().equalsIgnoreCase("jpeg")) mime = "image/jpeg"; else mime = "image/png";
                    renderImage(x, y, width, height, mime, data);
                }
            }
        }
        super.processOperator(operator, arguments);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        if (!text.getCharacter().trim().isEmpty()) {
            cur_x = text.getX();
            cur_y = text.getY();
            int distx = 0;
            int disty = 0;
            if (lastText != null) {
                distx = (int) (text.getX() - (lastText.getX() + lastText.getWidth()));
                disty = (int) (text.getY() - lastText.getY());
            }
            boolean split = lastText == null || distx > 1 || distx < -6 || Math.abs(disty) > 1;
            updateStyle(style, text);
            if (!style.equals(curstyle)) split = true;
            if (split) {
                if (lastText != null) finishBox();
                curstyle = new BoxStyle(style);
                curstyle.setLeft(cur_x);
                curstyle.setTop(cur_y - text.getFontSizeInPt());
            }
            textLine.append(text.getCharacter());
            lastText = text;
        }
    }

    /**
     * Finishes the current box - empties the text line buffer and creates a DOM element from it.
     */
    protected void finishBox() {
        if (textLine.length() > 0) {
            renderText(textLine.toString());
            textLine = new StringBuilder();
        }
    }

    /**
     * Updates the text style according to a new text position
     * @param bstyle the style to be updated
     * @param text the text position
     */
    protected void updateStyle(BoxStyle bstyle, TextPosition text) {
        String font = text.getFont().getBaseFont();
        String family = null;
        String weight = null;
        String fstyle = null;
        bstyle.setFontSize(text.getFontSizeInPt());
        if (font != null) {
            for (int i = 0; i < pdFontType.length; i++) {
                if (font.toLowerCase().lastIndexOf(pdFontType[i]) >= 0) {
                    weight = cssFontWeight[i];
                    fstyle = cssFontStyle[i];
                    break;
                }
            }
            if (weight != null) bstyle.setFontWeight(weight); else bstyle.setFontWeight(cssFontWeight[0]);
            if (fstyle != null) bstyle.setFontStyle(fstyle); else bstyle.setFontStyle(cssFontStyle[0]);
            for (int i = 0; i < cssFontFamily.length; i++) {
                if (font.toLowerCase().lastIndexOf(cssFontFamily[i].toLowerCase()) >= 0) {
                    family = cssFontFamily[i];
                    break;
                }
            }
            if (family != null) bstyle.setFontFamily(family);
        }
    }

    /**
     * Obtains the media box valid for the current page.
     * @return the media box rectangle
     */
    protected PDRectangle getCurrentMediaBox() {
        PDRectangle layout = pdpage.getMediaBox();
        if (layout == null) {
            PDPageNode curpage;
            do {
                curpage = pdpage.getParent();
                if (curpage != null) layout = curpage.getMediaBox();
            } while (layout == null && curpage != null);
        }
        return layout;
    }

    /**
     * Transforms a length according to a current transformation matrix.
     */
    protected float transformLength(float w) {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        Matrix m = new Matrix();
        m.setValue(2, 0, w);
        return m.multiply(ctm).getXPosition();
    }

    /**
     * Obtains a number from a PDF number value
     * @param value the PDF value of the Integer or Fload type
     * @return the corresponging numeric value
     */
    protected int intValue(COSBase value) {
        if (value instanceof COSNumber) return ((COSNumber) value).intValue(); else return 0;
    }

    /**
     * Obtains a number from a PDF number value
     * @param value the PDF value of the Integer or Float type
     * @return the corresponging numeric value
     */
    protected float floatValue(COSBase value) {
        if (value instanceof COSNumber) return ((COSNumber) value).floatValue(); else return 0;
    }

    /**
     * Obtains a length in points from a PDF number value
     * @param value the PDF value of the Integer or Fload type
     * @return the resulting length in points
     */
    protected float getLength(COSBase value) {
        return floatValue(value);
    }

    /**
     * Obtains a string from a PDF value
     * @param value the PDF value of the String, Integer or Float type
     * @return the corresponging string value
     */
    protected String stringValue(COSBase value) {
        if (value instanceof COSString) return ((COSString) value).getString(); else if (value instanceof COSNumber) return String.valueOf(((COSNumber) value).floatValue()); else return "";
    }

    /**
     * Creates a CSS rgb() specification from the color component values.
     * @param ir red value (0..255)
     * @param ig green value (0..255)
     * @param ib blue value (0..255)
     * @return the rgb() string
     */
    protected String colorString(int ir, int ig, int ib) {
        return String.format("#%02x%02x%02x", ir, ig, ib);
    }

    /**
     * Creates a CSS rgb() specification from the color component values.
     * @param r red value (0..1)
     * @param g green value (0..1)
     * @param b blue value (0..1)
     * @return the rgb() string
     */
    protected String colorString(float r, float g, float b) {
        return colorString((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    /**
     * Obtains the image data from a PDF image object
     * @param image the source image
     * @return the resulting image data
     * @throws IOException
     */
    protected byte[] getImageData(PDXObjectImage image) throws IOException {
        if (image instanceof PDJpeg) {
            List<String> DCT_FILTERS = new ArrayList<String>();
            DCT_FILTERS.add(COSName.DCT_DECODE.getName());
            DCT_FILTERS.add(COSName.DCT_DECODE_ABBREVIATION.getName());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream data = image.getPDStream().getPartiallyFilteredStream(DCT_FILTERS);
            byte[] buf = new byte[1024];
            int amountRead = -1;
            while ((amountRead = data.read(buf)) != -1) os.write(buf, 0, amountRead);
            os.close();
            return os.toByteArray();
        } else {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            image.write2OutputStream(os);
            os.close();
            return os.toByteArray();
        }
    }
}
