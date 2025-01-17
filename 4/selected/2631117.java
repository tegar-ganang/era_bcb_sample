package in_action.chapter02;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RandomAccessFileOrArray;
import com.lowagie.text.pdf.TextField;

/**
 * This example was written by Bruno Lowagie. It is part of the book 'iText in
 * Action' by Manning Publications. 
 * ISBN: 1932394796
 * http://www.1t3xt.com/docs/book.php 
 * http://www.manning.com/lowagie/
 */
public class HelloWorldStampCopy {

    /**
	 * Generates a PDF in multiple passes. First do the stamping, then do the
	 * copying.
	 * 
	 * @param args
	 *            no arguments needed here
	 */
    public static void main(String[] args) {
        System.out.println("Chapter 2: example HelloWorldStampCopy");
        System.out.println("-> Creates a PDF in multiple passes;");
        System.out.println("   first do the stamping, then the copying.");
        System.out.println("-> jars needed: iText.jar");
        System.out.println("-> files generated in /results subdirectory:");
        System.out.println("   HelloLetter.pdf");
        System.out.println("   HelloWorldStampCopy.pdf");
        createPdf("results/in_action/chapter02/HelloLetter.pdf", "field", "value");
        PdfReader reader;
        PdfStamper stamper;
        AcroFields form;
        try {
            RandomAccessFileOrArray letter = new RandomAccessFileOrArray("results/in_action/chapter02/HelloLetter.pdf");
            reader = new PdfReader(letter, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, baos);
            form = stamper.getAcroFields();
            form.setField("field", "World,");
            stamper.setFormFlattening(true);
            stamper.close();
            reader = new PdfReader(baos.toByteArray());
            Document document = new Document(reader.getPageSizeWithRotation(1));
            PdfCopy writer = new PdfCopy(document, new FileOutputStream("results/in_action/chapter02/HelloWorldStampCopy.pdf"));
            document.open();
            writer.addPage(writer.getImportedPage(reader, 1));
            reader = new PdfReader(letter, null);
            baos = new ByteArrayOutputStream();
            stamper = new PdfStamper(reader, baos);
            form = stamper.getAcroFields();
            form.setField("field", "People,");
            stamper.setFormFlattening(true);
            stamper.close();
            reader = new PdfReader(baos.toByteArray());
            writer.addPage(writer.getImportedPage(reader, 1));
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Generates a PDF file with an AcroForm.
	 * 
	 * @param filename
	 *            the filename of the PDF file.
	 * @param field
	 *            name of a fields that has to be added to the form
	 * @param value
	 *            value of a fields that has to be added to the form
	 */
    private static void createPdf(String filename, String field, String value) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            PdfContentByte cb = writer.getDirectContent();
            cb.beginText();
            cb.setFontAndSize(bf, 12);
            cb.setLeading(16);
            cb.moveText(36f, 788f);
            cb.showText("Dear");
            cb.newlineShowText("I just wanted to say Hello.");
            cb.endText();
            TextField tf = new TextField(writer, new Rectangle(64, 785, 340, 800), field);
            tf.setFontSize(12);
            tf.setFont(bf);
            tf.setText(value);
            tf.setTextColor(new GrayColor(0.5f));
            writer.addAnnotation(tf.getTextField());
        } catch (DocumentException de) {
            System.err.println(de.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        document.close();
    }
}
