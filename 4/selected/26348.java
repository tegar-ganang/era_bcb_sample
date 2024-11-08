package com.itextpdf.devoxx.sections;

import java.io.FileOutputStream;
import java.io.IOException;
import com.itextpdf.devoxx.properties.Dimensions;
import com.itextpdf.devoxx.properties.Dimensions.Dimension;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Creates a PDF with content that is imported:
 * Foreword, Steering and program committee, whiteboards,
 * sponsors.
 */
public class Imported {

    /**
	 * Creates a PDF listing all the presentations.
	 * @param filename the resulting PDF file
	 * @param source path where the source PDF of the foreword can be found
	 * @throws IOException
	 * @throws DocumentException
     * @return
	 */
    public int createPdf(final String filename, final String source) throws IOException, DocumentException {
        final Rectangle rect = Dimensions.getDimension(true, Dimension.BODY);
        final Document document = new Document(rect, 0, 0, 0, 0);
        final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();
        if (source == null) {
            writer.setPageEmpty(false);
        } else {
            final PdfReader reader = new PdfReader(source);
            Image image;
            float offsetX, offsetY;
            for (int p = 1; p <= reader.getNumberOfPages(); p++) {
                image = Image.getInstance(writer.getImportedPage(reader, p));
                image.setRotationDegrees(-reader.getPageRotation(p));
                image.scaleToFit(rect.getWidth(), rect.getHeight());
                offsetX = rect.getLeft() + (rect.getWidth() - image.getScaledWidth()) / 2;
                offsetY = rect.getBottom() + (rect.getHeight() - image.getScaledHeight()) / 2;
                image.setAbsolutePosition(offsetX, offsetY);
                document.add(image);
                document.newPage();
            }
        }
        document.close();
        return writer.getPageNumber() - 1;
    }
}
