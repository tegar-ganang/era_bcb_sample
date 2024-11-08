package net.sourceforge.fo3d.extensions.examples;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.fo3d.extensions.ExtensionXMLElementHandler;
import net.sourceforge.fo3d.javascript.JavaScriptUtil;
import net.sourceforge.fo3d.util.DOMUtil;
import net.sourceforge.fo3d.util.Vector3;
import org.apache.commons.io.IOUtils;
import org.apache.fop.pdf.FO3DPDFDocument;
import org.apache.fop.pdf.PDF3DAnnotation;
import org.apache.fop.pdf.PDF3DStream;
import org.apache.fop.pdf.PDFImage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.pdf.ImageRenderedAdapter;
import org.apache.fop.render.pdf.PDFRendererContextConstants;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageBuffered;
import org.w3c.dom.Element;

/**
 * Class for handling "textsprite" XML tags within FO3D's extension
 * environment.
 */
public class TextSpriteHandler implements ExtensionXMLElementHandler {

    /**
     * Supported extension tag name(s)
     */
    protected String extTagName = "textsprite";

    /**
     * Font size used for rendering text images
     */
    protected static final int FONT_SIZE = 32;

    /**
     * Filename of JavaScript file to be loaded once for every 3D annotation
     * containing at least one annotations extension tag.
     */
    protected static final String JS_LIB_FILE = "/resources/extensions/textsprite/textsprite.js";

    /**
     * Prefix for textsprite resource names
     */
    protected static final String RESOURCE_NAME_PREFIX = "ext-textsprite-";

    /**
     * Static list, containing 3D stream object PDF-Reference-Strings
     * for which we've already added the library JavaScirpt code.
     * (to prohibit duplicate loading of library code)
     */
    protected static List libLoadedList = new ArrayList();

    /**
     * Cache for JavaScript library code
     */
    protected static String cachedLibJSCode = null;

    protected static int textSpriteCount = 0;

    /**
     * Default constructor
     */
    public TextSpriteHandler() {
    }

    /** {@inheritDoc} */
    public String getSupportedElementName() {
        return extTagName;
    }

    /** {@inheritDoc} */
    public void handleElement(RendererContext context, Element el, PDF3DAnnotation annot) throws Exception {
        Vector3 pos = DOMUtil.get3DVectorAttribute(el, "at", true);
        String text = el.getTextContent().trim();
        if (text.length() == 0) {
            return;
        }
        String intDest = DOMUtil.getAttribute(el, "internal-destination");
        String resName = RESOURCE_NAME_PREFIX + textSpriteCount++;
        BufferedImage bi = getTextImage(text);
        FO3DPDFDocument pdfDoc = (FO3DPDFDocument) context.getProperty(PDFRendererContextConstants.PDF_DOCUMENT);
        PDFResourceContext resContext = (PDFResourceContext) context.getProperty(PDFRendererContextConstants.PDF_CONTEXT);
        ImageInfo imgInfo = new ImageInfo(null, null);
        ImageSize imgSize = new ImageSize();
        imgSize.setSizeInPixels(bi.getWidth(), bi.getHeight());
        imgSize.setResolution(72);
        imgInfo.setSize(imgSize);
        ImageBuffered img = new ImageBuffered(imgInfo, bi, null);
        PDFImage pdfimage = new ImageRenderedAdapter(img, resName);
        PDFXObject xobj = pdfDoc.addImage(resContext, pdfimage);
        PDF3DStream stream = annot.getStreamSafely();
        stream.addResource(resName, xobj);
        if (!libLoadedList.contains(stream.referencePDF())) {
            stream.addJSCode(getLibJSCode());
            libLoadedList.add(stream.referencePDF());
        }
        stream.addJSCode("\naddTextSprite('" + JavaScriptUtil.escapeString(resName) + "'," + pos.toPDF3DJSString() + (intDest.length() > 0 ? ",'" + JavaScriptUtil.escapeString(intDest) + "'" : "") + ");");
    }

    /**
     * Renders an image containing the given Text
     * 
     * @param text String containing text to be rendered
     * @param out OutputStrem object for writing the generated image to.
     * @throws IOException
     */
    protected BufferedImage getTextImage(String text) {
        Color bgColor = Color.black;
        Color fontColor = Color.white;
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE);
        text = text.replace("\r", "");
        String[] paragraphs = text.split("\n");
        FontRenderContext frc = new FontRenderContext(null, true, true);
        TextLayout layout;
        Rectangle2D bounds;
        double tmpW = 0d, tmpH = 0d;
        for (int i = 0; i < paragraphs.length; ++i) {
            layout = new TextLayout(paragraphs[i].trim() + " ", font, frc);
            bounds = layout.getBounds();
            tmpW = bounds.getWidth() > tmpW ? bounds.getWidth() : tmpW;
            tmpH += layout.getAscent() + layout.getDescent();
            if ((i + 1) != paragraphs.length) {
                tmpH += layout.getLeading();
            }
        }
        int border = 5;
        int width = ((int) Math.ceil(tmpW)) + 2 * border;
        int height = ((int) Math.ceil(tmpH)) + 2 * border;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D graphics = bi.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setBackground(bgColor);
        graphics.clearRect(0, 0, bi.getWidth(), bi.getHeight());
        graphics.setPaint(fontColor);
        float posX = (float) border;
        float posY = (float) border;
        for (int i = 0; i < paragraphs.length; ++i) {
            layout = new TextLayout(paragraphs[i], font, frc);
            posY += layout.getAscent();
            layout.draw(graphics, posX, posY);
            posY += layout.getDescent() + layout.getLeading();
        }
        return bi;
    }

    /**
     * Returns JavaScript library code.
     * 
     * @return JavaScript code
     * @throws IOException
     */
    protected String getLibJSCode() throws IOException {
        if (cachedLibJSCode == null) {
            InputStream is = getClass().getResourceAsStream(JS_LIB_FILE);
            StringWriter output = new StringWriter();
            IOUtils.copy(is, output);
            cachedLibJSCode = output.toString();
        }
        return cachedLibJSCode;
    }
}
