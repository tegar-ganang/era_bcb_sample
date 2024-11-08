package org.ujac.print.tag;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.ujac.print.AttributeDefinition;
import org.ujac.print.AttributeDefinitionMap;
import org.ujac.print.BaseDocumentTag;
import org.ujac.print.ChildDefinition;
import org.ujac.print.ChildDefinitionMap;
import org.ujac.print.Condition;
import org.ujac.print.DocumentHandlerException;
import org.ujac.print.DocumentMetaData;
import org.ujac.print.ElementContainer;
import org.ujac.print.JavascriptContainer;
import org.ujac.print.NewPageHandler;
import org.ujac.print.tag.acroform.BaseAcroFieldTag;
import org.ujac.print.tag.graphics.GraphicsTag;
import org.ujac.util.CollectionUtils;
import org.ujac.util.template.TemplateContext;
import com.lowagie.text.DocWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPageEvent;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Name: SegmentTag<br>
 * Description: A class handling &lt;segment&gt; tags.<br>
 * 
 * @author lauerc
 */
public class SegmentTag extends BaseLoopTag implements Condition, ElementContainer, JavascriptContainer, NewPageHandler {

    /** Definition of the 'loop-variable' attribute. */
    protected static final AttributeDefinition LOOP_VARIABLE = CommonAttributes.LOOP_VARIABLE.cloneAttrDef().setRequired(false);

    /** Definition of the 'sequence' attribute. */
    protected static final AttributeDefinition SEQUENCE = CommonAttributes.SEQUENCE.cloneAttrDef().setRequired(false);

    /** The item's name. */
    public static final String TAG_NAME = "segment";

    /** The original iText document. */
    private Document originalDocument = null;

    /** The original document writer. */
    private DocWriter originalDocumentWriter = null;

    /** The original template context. */
    private TemplateContext originalTemplateContext = null;

    /** The original resource registry. */
    private Map<Object, Object> originalResourceRegistry = null;

    /** The original page event handler. */
    private PdfPageEvent originalPageEventHandler = null;

    /** The original page size. */
    private Rectangle originalPageSize = null;

    /** The byte array stream to write the segment to. */
    private ByteArrayOutputStream segmentStream = null;

    /** The iText document. */
    private Document segmentDocument = null;

    /** The document writer. */
    private DocWriter segmentWriter = null;

    /** The segment context. */
    private TemplateContext segmentTemplateContext = null;

    /** The segment page size. */
    private Rectangle segmentPageSize = null;

    /** The resource registry. */
    private Map<Object, Object> resourceRegistry = null;

    /** The page event handler. */
    private PdfPageEvent segmentPageEventHandler = null;

    /** The page counter. */
    @SuppressWarnings("unused")
    private int pageCounter = 0;

    /** The line spacing attribute. */
    private float lineSpacing = DEFAULT_LINE_SPACING;

    /** The left page margin. */
    private float marginLeft = 0.0F;

    /** The right page margin. */
    private float marginRight = 0.0F;

    /** The top page margin. */
    private float marginTop = 0.0F;

    /** The bottom page margin. */
    private float marginBottom = 0.0F;

    /**
   * Constructs a SegmentTag instance with no specific attributes.
   */
    public SegmentTag() {
        super(TAG_NAME);
    }

    /**
   * Gets a brief description for the item.
   * @return The item's description.
   */
    public String getDescription() {
        return "Inserts a sub document into the main document.";
    }

    /**
   * Gets the list of supported attributes.
   * @return The attribute definitions.
   */
    protected AttributeDefinitionMap buildSupportedAttributes() {
        return super.buildSupportedAttributes().addDefinition(LOOP_VARIABLE).addDefinition(SEQUENCE).addDefinition(CommonAttributes.TRIM_BODY).addDefinition(CommonAttributes.PAGE_SIZE).addDefinition(CommonAttributes.PAGE_WIDTH).addDefinition(CommonAttributes.PAGE_HEIGHT).addDefinition(CommonAttributes.PAGE_ROTATE).addDefinition(CommonAttributes.MARGIN_LEFT).addDefinition(CommonAttributes.MARGIN_RIGHT).addDefinition(CommonAttributes.MARGIN_TOP).addDefinition(CommonAttributes.MARGIN_BOTTOM).addDefinition(CommonAttributes.BACKGROUND_COLOR).addDefinition(CommonAttributes.LINE_SPACING);
    }

    /**
   * Gets the list of supported style attributes.
   * @return The attribute definitions.
   */
    protected AttributeDefinitionMap buildSupportedStyleAttributes() {
        return super.buildSupportedStyleAttributes().addDefinition(CommonStyleAttributes.PAGE_SIZE).addDefinition(CommonStyleAttributes.PAGE_WIDTH).addDefinition(CommonStyleAttributes.PAGE_HEIGHT).addDefinition(CommonStyleAttributes.PAGE_ROTATE).addDefinition(CommonStyleAttributes.MARGIN_LEFT).addDefinition(CommonStyleAttributes.MARGIN_RIGHT).addDefinition(CommonStyleAttributes.MARGIN_TOP).addDefinition(CommonStyleAttributes.MARGIN_BOTTOM).addDefinition(CommonStyleAttributes.BACKGROUND_COLOR).addDefinition(CommonStyleAttributes.LINE_SPACING);
    }

    /**
   * Gets the list of supported childs.
   * @return The child definitions.
   */
    public ChildDefinitionMap buildSupportedChilds() {
        return super.buildSupportedChilds().addDefinition(new ChildDefinition(HeaderTag.class, 0, 1)).addDefinition(new ChildDefinition(FooterTag.class, 0, 1)).addDefinition(new ChildDefinition(StickyTag.class, 0, 1)).addDefinition(new ChildDefinition(BaseFontTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(IfTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ElseTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(LogTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(SwitchTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ForeachTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ChapterTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(PhraseTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ParagraphTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(TableTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(PdfTableTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(PrintTableTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(SetFormatTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ResourceBundleTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(AnchorTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(AnnotationTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ImageTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(BarcodeTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(DatamatrixTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(BarcodePdf417Tag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(MetaTag.class, 0, 1)).addDefinition(new ChildDefinition(ListTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(OverlayTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(BoxTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(SetPropertyTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(DefineTableTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(AddRowTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(AssertTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ImportTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(MacroTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(NewLineTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(NewPageTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(OutlineTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(ColumnTextTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(BaseAcroFieldTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(GraphicsTag.class, 0, ChildDefinition.UNLIMITED)).addDefinition(new ChildDefinition(JavascriptTag.class, 0, ChildDefinition.UNLIMITED));
    }

    /**
   * Opens the item.
   * @exception DocumentHandlerException Thrown in case something went wrong while opening the document item.
   */
    public void openItem() throws DocumentHandlerException {
        if (!isAttributeDefined(LOOP_VARIABLE)) {
            setLoopVariable("segmentIdx");
        }
        if (!isAttributeDefined(SEQUENCE)) {
            setSequenceHolder(CollectionUtils.createArrayList(new Integer(1)));
        }
        super.openItem();
        ElementContainer elementContainer = getElementContainer();
        if (isAttributeDefined(CommonAttributes.LINE_SPACING, CommonStyleAttributes.LINE_SPACING)) {
            this.lineSpacing = getDimensionAttribute(CommonAttributes.LINE_SPACING, true, CommonStyleAttributes.LINE_SPACING);
        } else if (elementContainer != null) {
            this.lineSpacing = elementContainer.getLineSpacing();
        } else {
            this.lineSpacing = DEFAULT_LINE_SPACING;
        }
        originalDocument = documentHandler.getDocument();
        originalDocumentWriter = documentHandler.getDocumentWriter();
        originalTemplateContext = documentHandler.getTemplateContext();
        originalResourceRegistry = documentHandler.getResourceRegistry();
        originalPageEventHandler = documentHandler.getPageEventHandler();
        originalPageSize = originalDocument.getPageSize();
        String sizeAttr = getStringAttribute(CommonAttributes.PAGE_SIZE, true, CommonStyleAttributes.PAGE_SIZE);
        if (sizeAttr != null) {
            try {
                Field sizeField = PageSize.class.getField(sizeAttr);
                segmentPageSize = (Rectangle) sizeField.get(null);
            } catch (Exception e) {
                throw new DocumentHandlerException(locator(), "Unable to set page size: " + e.getMessage(), e);
            }
        } else {
            if (isAttributeDefined(CommonAttributes.PAGE_WIDTH) && isAttributeDefined(CommonAttributes.PAGE_HEIGHT)) {
                float width = getFloatAttribute(CommonAttributes.PAGE_WIDTH, true, CommonStyleAttributes.PAGE_WIDTH);
                float height = getFloatAttribute(CommonAttributes.PAGE_HEIGHT, true, CommonStyleAttributes.PAGE_HEIGHT);
                segmentPageSize = new Rectangle(width, height);
            }
        }
        if (segmentPageSize != null) {
            String rotateAttr = getStringAttribute(CommonAttributes.ROTATE, true, CommonStyleAttributes.ROTATE);
            boolean rotate = false;
            if (rotateAttr != null) {
                rotate = new Boolean(rotateAttr).booleanValue();
            }
            Color bgColor = getColorAttribute(CommonAttributes.BACKGROUND_COLOR, true, CommonStyleAttributes.BACKGROUND_COLOR);
            if (rotate) {
                segmentPageSize = segmentPageSize.rotate();
            }
            if (bgColor != null) {
                segmentPageSize.setBackgroundColor(bgColor);
            }
        } else {
            segmentPageSize = originalPageSize;
        }
        if (isAttributeDefined(CommonAttributes.MARGIN_LEFT)) {
            marginLeft = getFloatAttribute(CommonAttributes.MARGIN_LEFT, true, CommonStyleAttributes.MARGIN_LEFT);
        } else {
            marginLeft = originalDocument.leftMargin();
        }
        if (isAttributeDefined(CommonAttributes.MARGIN_RIGHT)) {
            marginRight = getFloatAttribute(CommonAttributes.MARGIN_RIGHT, true, CommonStyleAttributes.MARGIN_RIGHT);
        } else {
            marginRight = originalDocument.rightMargin();
        }
        if (isAttributeDefined(CommonAttributes.MARGIN_TOP)) {
            marginTop = getFloatAttribute(CommonAttributes.MARGIN_TOP, true, CommonStyleAttributes.MARGIN_TOP);
        } else {
            marginTop = originalDocument.topMargin();
        }
        if (isAttributeDefined(CommonAttributes.MARGIN_BOTTOM)) {
            marginBottom = getFloatAttribute(CommonAttributes.MARGIN_BOTTOM, true, CommonStyleAttributes.MARGIN_BOTTOM);
        } else {
            marginBottom = originalDocument.bottomMargin();
        }
        try {
            if (segmentPageSize.getRotation() != 0) {
                originalDocument.setPageSize(new Rectangle(segmentPageSize.getHeight(), segmentPageSize.getWidth()));
            } else {
                originalDocument.setPageSize(segmentPageSize);
            }
            originalDocument.newPage();
        } catch (Exception ex) {
            throw new DocumentHandlerException(locator(), "Unable to start new page: " + ex.getMessage(), ex);
        }
        this.segmentPageEventHandler = documentHandler.createPageEventHandler();
        ((PdfWriter) originalDocumentWriter).setPageEvent(null);
        pageCounter = 0;
    }

    /**
   * Closes the item.
   * @exception DocumentHandlerException Thrown in case something went wrong while closing the document item.
   */
    public void closeItem() throws DocumentHandlerException {
        super.closeItem();
        try {
            originalDocument.setPageSize(originalPageSize);
            originalDocument.newPage();
        } catch (Exception ex) {
            throw new DocumentHandlerException(locator(), "Unable to start new page: " + ex.getMessage(), ex);
        }
        documentHandler.setTemplateContext(originalTemplateContext);
        documentHandler.setDocumentAndWriter(originalDocument, originalDocumentWriter, originalResourceRegistry, originalPageEventHandler);
    }

    /**
   * Pre-processes the child items of the foreach item.
   * @return Tells, whether or not to repeat the child items, returning false skips the execution
   *   for the current item.
   * @exception DocumentHandlerException Thrown in case something went wrong.
   */
    protected boolean preRepeatChildItems() throws DocumentHandlerException {
        segmentDocument = new Document(segmentPageSize, marginLeft, marginRight, marginTop, marginBottom);
        this.segmentTemplateContext = new TemplateContext(documentHandler.getTemplateContext());
        documentHandler.setTemplateContext(segmentTemplateContext);
        segmentStream = new ByteArrayOutputStream();
        resourceRegistry = new HashMap<Object, Object>();
        try {
            segmentWriter = PdfWriter.getInstance(segmentDocument, segmentStream);
            DocumentMetaData metaData = documentHandler.getMetaData();
            if (metaData != null) {
                ((PdfWriter) segmentWriter).setPDFXConformance(metaData.getPdfxConformance());
                ((PdfWriter) segmentWriter).setPdfVersion(metaData.getPdfVersion());
            }
        } catch (DocumentException ex) {
            throw new DocumentHandlerException(locator(), "Unable to create segment writer: " + ex.getMessage(), ex);
        }
        documentHandler.setDocumentAndWriter(segmentDocument, segmentWriter, resourceRegistry, segmentPageEventHandler);
        return true;
    }

    /**
   * Post-processes the child items of the foreach item.
   * @exception DocumentHandlerException Thrown in case something went wrong.
   */
    protected void postRepeatChildItems() throws DocumentHandlerException {
        if (segmentDocument == null) {
            return;
        }
        segmentDocument.close();
        try {
            byte[] content = segmentStream.toByteArray();
            PdfReader reader = new PdfReader(content);
            int numPages = reader.getNumberOfPages();
            PdfWriter writer = (PdfWriter) originalDocumentWriter;
            for (int page = 1; page <= numPages; page++) {
                PdfImportedPage importedPage = writer.getImportedPage(reader, page);
                PdfContentByte directContent = writer.getDirectContent();
                directContent.addTemplate(importedPage, 0, 0);
                if ((page != numPages) || hasNext()) {
                    originalDocument.setPageSize(importedPage.getBoundingBox());
                    originalDocument.newPage();
                }
                ++pageCounter;
            }
            documentHandler.setTemplateContext(originalTemplateContext);
            documentHandler.setDocumentAndWriter(originalDocument, originalDocumentWriter, originalResourceRegistry, originalPageEventHandler);
        } catch (IOException ex) {
            throw new DocumentHandlerException(locator(), "Unable to read segment document: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new DocumentHandlerException(locator(), "Document processing failed: " + ex.getMessage(), ex);
        }
    }

    /**
   * Adds a element to the container.
   * @param item The document item.
   * @param element The element to be added.
   * @exception DocumentHandlerException If something went wrong when adding the element.
   */
    public void addElement(BaseDocumentTag item, Element element) throws DocumentHandlerException {
        if (!isValid()) {
            return;
        }
        try {
            Document document = getDocument(true);
            if (item instanceof ParagraphTag) {
                ParagraphTag tag = (ParagraphTag) item;
                float leading = tag.getLeading();
                float lineSpacing = tag.getLineSpacing();
                extendLeading((Paragraph) element, item.getFont().getFont(), leading, lineSpacing);
            }
            document.add(element);
        } catch (DocumentException ex) {
            throw new DocumentHandlerException(locator(), "Unable to add element <" + item.getName() + "> to the " + getName() + ": " + ex.getMessage(), ex);
        }
    }

    /**
   * Tells whether the element container is a top level container or not.
   * @return true if the element container is a top level container, else false.
   */
    public boolean isTopLevel() {
        return true;
    }

    /**
   * Gets the line spacing.
   * @return The current line spacing.
   */
    public float getLineSpacing() {
        return lineSpacing;
    }

    /**
   * Gets the leading
   * @return The current leading.
   */
    public float getLeading() {
        return DEFAULT_LEADING;
    }

    /**
   * Gets the iText document.
   * @return The iText document.
   */
    public Document getDocument() {
        return getDocument(true);
    }

    /**
   * Setter method for property document.
   * @param document The document to set.
   */
    protected void setDocument(Document document) {
        this.segmentDocument = document;
    }

    /**
   * Gets the iText document.
   * @param openDocumentRequired True, if an open document is required, else false. 
   * @return The iText document.
   */
    public Document getDocument(boolean openDocumentRequired) {
        if (openDocumentRequired) {
            if (!segmentDocument.isOpen()) {
                segmentDocument.open();
            }
        }
        return segmentDocument;
    }

    /**
   * Handles a new page request.
   * @param size The new document size, null if the size should remain the same.
   * @param margins The new document margins, null if the margins should remain the same.
   * @return A reference to the document. 
   * @exception DocumentHandlerException In case something went wrong.
   */
    public Document handleNewPage(Rectangle size, Rectangle margins) throws DocumentHandlerException {
        try {
            if (size != null) {
                segmentDocument.setPageSize(size);
            }
            if (margins != null) {
                segmentDocument.setMargins(margins.getBorderWidthLeft(), margins.getBorderWidthRight(), margins.getBorderWidthTop(), margins.getBorderWidthBottom());
            }
            segmentDocument.newPage();
        } catch (Exception ex) {
            throw new DocumentHandlerException(locator(), "Unable to start a new page: " + ex.getMessage(), ex);
        }
        return segmentDocument;
    }

    /**
   * @see org.ujac.print.JavascriptContainer#addJavaScript(java.lang.String)
   */
    public void addJavaScript(String code) {
        PdfWriter pdfWriter = (PdfWriter) documentHandler.getDocumentWriter();
        pdfWriter.addJavaScript(code);
    }
}
