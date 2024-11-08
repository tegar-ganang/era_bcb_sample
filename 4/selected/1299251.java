package org.orbeon.oxf.processor.scratchpad;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.ProcessorInputOutputInfo;
import org.orbeon.oxf.processor.serializer.HttpBinarySerializer;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.util.LoggerFactory;
import org.orbeon.oxf.xml.XMLUtils;
import org.orbeon.oxf.xml.XPathUtils;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * The PDF Template processor reads a PDF template and performs textual annotations on it.
 */
public class PDFTemplateProcessor extends HttpBinarySerializer {

    private static Logger logger = LoggerFactory.createLogger(PDFTemplateProcessor.class);

    public static String DEFAULT_CONTENT_TYPE = "application/pdf";

    public static final String PDF_TEMPLATE_MODEL_NAMESPACE_URI = "http://www.orbeon.com/oxf/pdf-template/model";

    public PDFTemplateProcessor() {
        addInputInfo(new ProcessorInputOutputInfo("model", PDF_TEMPLATE_MODEL_NAMESPACE_URI));
        addInputInfo(new ProcessorInputOutputInfo("instance"));
    }

    protected String getDefaultContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    protected void readInput(PipelineContext pipelineContext, ProcessorInput input, Config config, OutputStream outputStream) {
        org.dom4j.Document configDocument = readCacheInputAsDOM4J(pipelineContext, "model");
        org.dom4j.Document instanceDocument = readInputAsDOM4J(pipelineContext, "instance");
        try {
            PdfReader reader = new PdfReader(URLFactory.createURL((XPathUtils.selectStringValue(configDocument, "/*/template/@href"))));
            int pageCount = reader.getNumberOfPages();
            Rectangle psize = reader.getPageSize(1);
            float width = psize.width();
            float height = psize.height();
            Document document = new Document(psize, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            for (int p = 1; p <= pageCount; p++) {
                document.newPage();
                PdfImportedPage page1 = writer.getImportedPage(reader, p);
                cb.addTemplate(page1, 0, 0);
                String xPath = (p == 1) ? "/*/field[@page = 1 or not (@page)]" : ("/*/field[@page = " + p + "]");
                for (Iterator i = XPathUtils.selectIterator(configDocument, xPath); i.hasNext(); ) {
                    Element e = (Element) i.next();
                    Map namespaceMap = XMLUtils.getNamespaceContext(e);
                    String leftPosition = e.attributeValue("left-position");
                    String topPosition = e.attributeValue("top-position");
                    String spacing = e.attributeValue("spacing");
                    String fontFamily = e.attributeValue("font-family");
                    String fontSize = e.attributeValue("font-size");
                    String size = e.attributeValue("size");
                    String ref = e.attributeValue("ref");
                    BaseFont baseFont = BaseFont.createFont(fontFamily, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                    cb.beginText();
                    {
                        cb.setFontAndSize(baseFont, Float.parseFloat(fontSize));
                        float xPosition = Float.parseFloat(leftPosition);
                        float yPosition = height - Float.parseFloat(topPosition);
                        float space = Float.parseFloat(spacing);
                        String text = XPathUtils.selectStringValue(instanceDocument, ref, namespaceMap);
                        if (text != null) {
                            int len = Math.min(text.length(), (size != null) ? Integer.parseInt(size) : Integer.MAX_VALUE);
                            for (int j = 0; j < len; j++) cb.showTextAligned(PdfContentByte.ALIGN_CENTER, text.substring(j, j + 1), xPosition + ((float) j) * space, yPosition, 0);
                        }
                    }
                    cb.endText();
                }
            }
            document.close();
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }
}
