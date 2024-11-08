package part4.chapter14;

import java.io.FileOutputStream;
import java.io.IOException;
import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

public class TransformationMatrix3 {

    /** The resulting PDF. */
    public static final String RESULT = "results/part4/chapter14/transformation_matrix3.pdf";

    /** A PDF with the iText logo that will be transformed */
    public static final String RESOURCE = "resources/pdfs/logo.pdf";

    /**
     * Creates a PDF document.
     * @param filename the path to the new PDF document
     * @throws DocumentException 
     * @throws IOException
     */
    public void createPdf(String filename) throws IOException, DocumentException {
        Rectangle rect = new Rectangle(-595, -842, 595, 842);
        Document document = new Document(rect);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();
        PdfContentByte canvas = writer.getDirectContent();
        canvas.moveTo(-595, 0);
        canvas.lineTo(595, 0);
        canvas.moveTo(0, -842);
        canvas.lineTo(0, 842);
        canvas.stroke();
        PdfReader reader = new PdfReader(RESOURCE);
        PdfTemplate template = writer.getImportedPage(reader, 1);
        canvas.saveState();
        canvas.addTemplate(template, 0, 0);
        AffineTransform af = new AffineTransform();
        af.translate(-595, 0);
        af.scale(0.5, 0.5);
        canvas.transform(af);
        canvas.addTemplate(template, 0, 0);
        canvas.concatCTM(AffineTransform.getTranslateInstance(595, 595));
        canvas.addTemplate(template, 0, 0);
        canvas.restoreState();
        canvas.saveState();
        af = new AffineTransform(1, 0, 0.4, 1, -750, -650);
        canvas.addTemplate(template, af);
        canvas.restoreState();
        canvas.saveState();
        af = new AffineTransform(0, -1, -1, 0, 650, 0);
        canvas.addTemplate(template, af);
        af = new AffineTransform(0, -0.2f, -0.5f, 0, 350, 0);
        canvas.addTemplate(template, af);
        canvas.restoreState();
        document.close();
    }

    /**
     * Main method.
     *
     * @param    args    no arguments needed
     * @throws DocumentException 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, DocumentException {
        new TransformationMatrix3().createPdf(RESULT);
    }
}
