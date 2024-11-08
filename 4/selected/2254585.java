package net.sourceforge.jpdfoverlap.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import net.sourceforge.jpdfoverlap.exception.PdfRendererException;

;

/**
 * This class provides the method to overlap two or more different pdf files.
 * @author Angel Ruiz
 *
 */
public class PdfRenderer {

    public static void overlayPdf(File[] pdfDocuments, String fileNamePdfOutput) throws PdfRendererException {
        try {
            FileOutputStream fos = new FileOutputStream(fileNamePdfOutput);
            PdfReader baseReader = new PdfReader(pdfDocuments[0].getAbsolutePath());
            Rectangle pageSize = baseReader.getPageSize(1);
            Document document = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            document.newPage();
            for (int i = 0; i < pdfDocuments.length; i++) {
                PdfReader reader = new PdfReader(pdfDocuments[i].getAbsolutePath());
                PdfImportedPage page = writer.getImportedPage(reader, 1);
                cb.addTemplate(page, 0, 0);
            }
            document.close();
        } catch (IOException e) {
            String error = "Ha ocurrido un error al leer los ficheros pdf";
            throw new PdfRendererException(error, e);
        } catch (DocumentException e) {
            String error = "Ha ocurrido un error al procesar los ficheros pdf";
            throw new PdfRendererException(error, e);
        }
    }

    public static void overlayPdf(List pdfDocuments, String fileNamePdfOutput) throws PdfRendererException {
        try {
            FileOutputStream fos = new FileOutputStream(fileNamePdfOutput);
            PdfReader baseReader = new PdfReader(((File) pdfDocuments.get(0)).getAbsolutePath());
            Rectangle pageSize = baseReader.getPageSize(1);
            Document document = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            document.newPage();
            for (Iterator it = pdfDocuments.iterator(); it.hasNext(); ) {
                File pdfFile = (File) it.next();
                PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
                PdfImportedPage page = writer.getImportedPage(reader, 1);
                cb.addTemplate(page, 0, 0);
            }
            document.close();
        } catch (IOException e) {
            String error = "Ha ocurrido un error al leer los ficheros pdf";
            throw new PdfRendererException(error, e);
        } catch (DocumentException e) {
            String error = "Ha ocurrido un error al procesar los ficheros pdf";
            throw new PdfRendererException(error, e);
        }
    }
}
