package com.itextpdf.text.pdf;

import java.io.IOException;
import com.itextpdf.text.error_messages.MessageLocalization;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;

/** Represents an imported page.
 *
 * @author Paulo Soares
 */
public class PdfImportedPage extends com.itextpdf.text.pdf.PdfTemplate {

    PdfReaderInstance readerInstance;

    int pageNumber;

    /**
     * True if the imported page has been copied to a writer.
     * @since iText 5.0.4
     */
    protected boolean toCopy = true;

    PdfImportedPage(PdfReaderInstance readerInstance, PdfWriter writer, int pageNumber) {
        this.readerInstance = readerInstance;
        this.pageNumber = pageNumber;
        this.writer = writer;
        bBox = readerInstance.getReader().getPageSize(pageNumber);
        setMatrix(1, 0, 0, 1, -bBox.getLeft(), -bBox.getBottom());
        type = TYPE_IMPORTED;
    }

    /** Reads the content from this <CODE>PdfImportedPage</CODE>-object from a reader.
     *
     * @return self
     *
     */
    public PdfImportedPage getFromReader() {
        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    /** Always throws an error. This operation is not allowed.
     * @param image dummy
     * @param a dummy
     * @param b dummy
     * @param c dummy
     * @param d dummy
     * @param e dummy
     * @param f dummy
     * @throws DocumentException  dummy */
    public void addImage(Image image, float a, float b, float c, float d, float e, float f) throws DocumentException {
        throwError();
    }

    /** Always throws an error. This operation is not allowed.
     * @param template dummy
     * @param a dummy
     * @param b dummy
     * @param c dummy
     * @param d dummy
     * @param e dummy
     * @param f  dummy */
    public void addTemplate(PdfTemplate template, float a, float b, float c, float d, float e, float f) {
        throwError();
    }

    /** Always throws an error. This operation is not allowed.
     * @return  dummy */
    public PdfContentByte getDuplicate() {
        throwError();
        return null;
    }

    /**
     * Gets the stream representing this page.
     *
     * @param	compressionLevel	the compressionLevel
     * @return the stream representing this page
     * @since	2.1.3	(replacing the method without param compressionLevel)
     */
    PdfStream getFormXObject(int compressionLevel) throws IOException {
        return readerInstance.getFormXObject(pageNumber, compressionLevel);
    }

    public void setColorFill(PdfSpotColor sp, float tint) {
        throwError();
    }

    public void setColorStroke(PdfSpotColor sp, float tint) {
        throwError();
    }

    PdfObject getResources() {
        return readerInstance.getResources(pageNumber);
    }

    /** Always throws an error. This operation is not allowed.
     * @param bf dummy
     * @param size dummy */
    public void setFontAndSize(BaseFont bf, float size) {
        throwError();
    }

    /**
     * Always throws an error. This operation is not allowed.
     * @param group New value of property group.
     * @since	2.1.6
     */
    public void setGroup(PdfTransparencyGroup group) {
        throwError();
    }

    void throwError() {
        throw new RuntimeException(MessageLocalization.getComposedMessage("content.can.not.be.added.to.a.pdfimportedpage"));
    }

    PdfReaderInstance getPdfReaderInstance() {
        return readerInstance;
    }

    /**
	 * Checks if the page has to be copied.
	 * @return true if the page has to be copied.
	 * @since iText 5.0.4
	 */
    public boolean isToCopy() {
        return toCopy;
    }

    /**
	 * Indicate that the resources of the imported page have been copied.
	 * @since iText 5.0.4
	 */
    public void setCopied() {
        toCopy = false;
    }
}
