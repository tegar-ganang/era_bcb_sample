package org.ujac.print.tag;

import java.io.File;
import java.io.IOException;
import org.ujac.print.AttributeDefinition;
import org.ujac.print.AttributeDefinitionMap;
import org.ujac.print.BaseDocumentTag;
import org.ujac.print.DocumentHandlerException;
import org.ujac.util.io.FileUtils;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Name: OverlayTag<br>
 * Description: A tag for overlaying a specified existing PDF page with dynamic content.
 * 
 * @author lauerc
 */
public class OverlayTag extends BaseDocumentTag {

    /** The item's name. */
    public static final String TAG_NAME = "overlay";

    /** Definition of the 'source' attribute. */
    private static final AttributeDefinition IMAGE_SOURCE = CommonAttributes.IMAGE_SOURCE.cloneAttrDef("The location of the document to overlay at resource loader.");

    /** Definition of the 'source' attribute. */
    private static final AttributeDefinition IMAGE_PAGE = CommonAttributes.IMAGE_PAGE.cloneAttrDef("The number of the page from the source document to overlay, defaults to 1.");

    /** Definition of the 'x' attribute. */
    private static final AttributeDefinition IMAGE_X = CommonAttributes.IMAGE_X.cloneAttrDef("The horizontal offset for the overlayed PDF. " + "If not defined, the left margin defines the offset.");

    /** Definition of the 'x' style attribute. */
    private static final AttributeDefinition IMAGE_X_STYLE = CommonStyleAttributes.IMAGE_X.cloneAttrDef("The horizontal offset for the overlayed PDF. " + "If not defined, the left margin defines the offset.");

    /** Definition of the 'y' attribute. */
    private static final AttributeDefinition IMAGE_Y = CommonAttributes.IMAGE_Y.cloneAttrDef("The vertical offset for the overlayed PDF. " + "If not defined, the bottom margin defines the offset.");

    /** Definition of the 'y' style attribute. */
    private static final AttributeDefinition IMAGE_Y_STYLE = CommonStyleAttributes.IMAGE_Y.cloneAttrDef("The vertical offset for the overlayed PDF. " + "If not defined, the bottom margin defines the offset.");

    /** The path to the overlay source. */
    private Object source = null;

    /** The number of the page to overlay. */
    private int page = 0;

    /**
   * Constructs a OverlayTag instance with no specific attributes.
   */
    public OverlayTag() {
        super(TAG_NAME);
    }

    /**
   * Gets a brief description for the item.
   * @return The item's description.
   */
    public String getDescription() {
        return "Places the contents of the specified PDF source as the background " + "of the document. This contents will get overlayed by the output of the " + "following tags.";
    }

    /**
   * Gets the list of supported attributes.
   * @return The attribute definitions.
   */
    protected AttributeDefinitionMap buildSupportedAttributes() {
        return super.buildSupportedAttributes().addDefinition(CommonAttributes.RENDERED).addDefinition(IMAGE_SOURCE).addDefinition(IMAGE_PAGE).addDefinition(IMAGE_X).addDefinition(IMAGE_Y).addDefinition(CommonAttributes.PAGE_ROTATE).addDefinition(CommonAttributes.MARGIN_LEFT).addDefinition(CommonAttributes.MARGIN_RIGHT).addDefinition(CommonAttributes.MARGIN_TOP).addDefinition(CommonAttributes.MARGIN_BOTTOM);
    }

    /**
   * Gets the list of supported style attributes.
   * @return The attribute definitions.
   */
    protected AttributeDefinitionMap buildSupportedStyleAttributes() {
        return super.buildSupportedStyleAttributes().addDefinition(IMAGE_X_STYLE).addDefinition(IMAGE_Y_STYLE).addDefinition(CommonAttributes.PAGE_ROTATE).addDefinition(CommonAttributes.MARGIN_LEFT).addDefinition(CommonAttributes.MARGIN_RIGHT).addDefinition(CommonAttributes.MARGIN_TOP).addDefinition(CommonAttributes.MARGIN_BOTTOM);
    }

    /**
   * Opens the item.
   * @exception DocumentHandlerException Thrown in case something went wrong while opening the document item.
   */
    public void openItem() throws DocumentHandlerException {
        super.openItem();
        if (!isValid()) {
            return;
        }
        this.source = evalAttribute(CommonAttributes.IMAGE_SOURCE);
        this.page = getIntegerAttribute(CommonAttributes.IMAGE_PAGE, 1, true, null);
        String rotateAttr = getStringAttribute(CommonAttributes.IMAGE_ROTATE, true, CommonStyleAttributes.IMAGE_ROTATE);
        boolean rotate = false;
        if (rotateAttr != null) {
            rotate = new Boolean(rotateAttr).booleanValue();
        }
        byte[] content = null;
        try {
            if (source instanceof byte[]) {
                content = (byte[]) source;
            } else if (source instanceof File) {
                content = FileUtils.loadFile((File) source);
            } else {
                content = documentHandler.loadResource(source.toString());
            }
            PdfWriter writer = (PdfWriter) documentHandler.getDocumentWriter();
            PdfReader reader = new PdfReader(content);
            if (page < 0) {
                throw new DocumentHandlerException(locator(), "The page number must be 1 at least!");
            }
            PdfImportedPage importedPage = writer.getImportedPage(reader, page);
            Rectangle bbox = importedPage.getBoundingBox();
            Rectangle oldBox = new Rectangle(bbox);
            if (isAttributeDefined(CommonAttributes.MARGIN_LEFT, CommonStyleAttributes.MARGIN_LEFT)) {
                float margin = getDimensionAttribute(CommonAttributes.MARGIN_LEFT, true, CommonStyleAttributes.MARGIN_LEFT);
                bbox.setLeft(margin);
            }
            if (isAttributeDefined(CommonAttributes.MARGIN_RIGHT, CommonStyleAttributes.MARGIN_RIGHT)) {
                float margin = getDimensionAttribute(CommonAttributes.MARGIN_RIGHT, true, CommonStyleAttributes.MARGIN_RIGHT);
                bbox.setRight(oldBox.getRight() - margin);
            }
            if (isAttributeDefined(CommonAttributes.MARGIN_BOTTOM, CommonStyleAttributes.MARGIN_BOTTOM)) {
                float margin = getDimensionAttribute(CommonAttributes.MARGIN_BOTTOM, true, CommonStyleAttributes.MARGIN_BOTTOM);
                bbox.setBottom(margin);
            }
            if (isAttributeDefined(CommonAttributes.MARGIN_TOP, CommonStyleAttributes.MARGIN_TOP)) {
                float margin = getDimensionAttribute(CommonAttributes.MARGIN_TOP, true, CommonStyleAttributes.MARGIN_TOP);
                bbox.setTop(oldBox.getTop() - margin);
            }
            importedPage.setBoundingBox(bbox);
            float left = bbox.getLeft() * -1;
            float bottom = bbox.getBottom() * -1;
            if (rotate) {
                importedPage.setMatrix(0.0F, 1.0F, -1.0F, 0.0F, oldBox.getHeight(), 0.0F);
                left *= -1.0F;
            }
            if (isAttributeDefined(CommonAttributes.IMAGE_X, CommonStyleAttributes.IMAGE_X)) {
                left = getFloatAttribute(CommonAttributes.IMAGE_X, true, CommonStyleAttributes.IMAGE_X);
            }
            if (isAttributeDefined(CommonAttributes.IMAGE_Y, CommonStyleAttributes.IMAGE_Y)) {
                bottom = getFloatAttribute(CommonAttributes.IMAGE_Y, true, CommonStyleAttributes.IMAGE_Y);
            }
            writer.getDirectContentUnder().addTemplate(importedPage, left, bottom);
        } catch (IllegalArgumentException ex) {
            throw new DocumentHandlerException(locator(), "The page #" + page + " could not be loaded from document " + source + "'.", ex);
        } catch (IOException ex) {
            throw new DocumentHandlerException(locator(), "Unable to load overlay document from location '" + source + "': " + ex.getMessage(), ex);
        }
    }

    /**
   * Closes the item.
   * @exception DocumentHandlerException Thrown in case something went wrong while closing the document item.
   */
    public void closeItem() throws DocumentHandlerException {
    }
}
