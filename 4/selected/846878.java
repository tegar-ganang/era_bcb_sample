package net.sourceforge.fo3d.extensions.examples;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.fo3d.extensions.ExtensionXMLElementHandler;
import net.sourceforge.fo3d.util.DOMUtil;
import net.sourceforge.fo3d.util.Vector3;
import org.apache.commons.io.IOUtils;
import org.apache.fop.pdf.PDF3DAnnotation;
import org.apache.fop.pdf.PDF3DStream;
import org.apache.fop.pdf.PDFObject;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.pdf.FO3DPDFRenderer;
import org.apache.fop.util.ColorUtil;
import org.w3c.dom.Element;

/**
 * Class for handling "annotations" XML tags within FO3D's extension
 * environment.
 */
public class AnnotationHandler implements ExtensionXMLElementHandler {

    /**
     * Supported extension tag name(s)
     */
    protected final String extTagName = "annotations";

    protected static final String ANNOT_RESOURCE_NAME = "ext-annotation-sphere";

    /**
     * Filename of JavaScript file to be loaded once for every 3D annotation
     * containing at least one annotations extension tag.
     */
    protected static final String JS_LIB_FILE = "/resources/extensions/annotations/annotations.js";

    /**
     * Filename of annotation model to be loaded for every single
     * link annotation in the scene.
     */
    protected static final String ANNOT_MODEL_FILE = "/resources/extensions/annotations/ball.u3d";

    /**
     * Static list, containing 3D stream object PDF-Reference-Strings
     * for which we've already added the library JavaScirpt code.
     * (to prohibit duplicate loading of library code)
     */
    protected static List libLoadedList = new ArrayList();

    /**
     * Cache for JavaScript library code
     * (for reducing slow file access)
     */
    protected static String cachedLibJSCode = null;

    /**
     * Default constructor
     */
    public AnnotationHandler() {
    }

    /** {@inheritDoc} */
    public String getSupportedElementName() {
        return extTagName;
    }

    /** {@inheritDoc} */
    public void handleElement(RendererContext context, Element el, PDF3DAnnotation annot) throws Exception {
        String arg;
        Double defScale;
        arg = DOMUtil.getAttribute(el, "scale");
        defScale = arg.length() > 0 ? new Double(arg) : new Double(1.0);
        Color defColor = null;
        arg = DOMUtil.getAttribute(el, "color");
        if (arg.length() > 0) {
            defColor = ColorUtil.parseColorString(context.getUserAgent(), arg);
        }
        List links = DOMUtil.getChildElementsByTagName(el, "link");
        if (links.size() > 0) {
            PDF3DStream stream = annot.getStreamSafely();
            FO3DPDFRenderer renderer = (FO3DPDFRenderer) context.getRenderer();
            PDFObject resource = renderer.add3DResourceStream(getClass().getResource(ANNOT_MODEL_FILE).toExternalForm());
            stream.addResource(ANNOT_RESOURCE_NAME, resource);
            if (!libLoadedList.contains(stream.referencePDF())) {
                stream.addJSCode(getLibJSCode());
                libLoadedList.add(stream.referencePDF());
            }
            Double scale;
            Color color;
            Vector3 pos;
            String destination;
            for (Iterator it = links.iterator(); it.hasNext(); ) {
                Element link = (Element) it.next();
                destination = DOMUtil.getAttribute(link, "href", true);
                pos = DOMUtil.get3DVectorAttribute(link, "at", true);
                color = defColor;
                arg = DOMUtil.getAttribute(link, "color");
                if (arg.length() > 0) {
                    color = ColorUtil.parseColorString(context.getUserAgent(), arg);
                }
                scale = defScale;
                arg = DOMUtil.getAttribute(link, "scale");
                if (arg.length() > 0) {
                    scale = new Double(arg);
                }
                stream.addJSCode("\nadd3DLinkAnnotation('" + ANNOT_RESOURCE_NAME + "'," + pos.toPDF3DJSString() + ",'" + destination + "'," + scale + ",new Color(" + (color.getRed() / 255d) + "," + (color.getGreen() / 255d) + "," + (color.getBlue() / 255d) + "));");
            }
            stream.addJSCode("\nreInitView(" + stream.getDefaultViewIndex() + ");");
        }
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
