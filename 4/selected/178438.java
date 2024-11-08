package com.asl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.HtmlWriter;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfEncodings;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

public class ApplicationContextTest extends BaseTest {

    public void testStartAppContext() throws Exception {
        assertNotNull("Application Context should not be null", getApplicationContext());
    }

    public static void main(String[] args) {
        System.getProperty("os.name");
        try {
            PdfReader reader = new PdfReader("/home/asl/books/hibernate_In_Action.pdf");
            PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(reader);
            String text = pdfTextExtractor.getTextFromPage(1);
            System.out.println(text);
            Map info = reader.getInfo();
            for (Iterator i = info.keySet().iterator(); i.hasNext(); ) {
                String key = (String) i.next();
                String value = (String) info.get(key);
                System.out.println(key + ": " + value);
            }
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("HelloWorldImportedPages.pdf"));
            document.open();
            PdfImportedPage page = writer.getImportedPage(reader, 1);
            Image image = Image.getInstance(page);
            byte b[] = image.getOriginalData();
            int type = image.getOriginalType();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}
