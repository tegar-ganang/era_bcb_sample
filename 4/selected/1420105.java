package questions.stamppages;

import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfRectangle;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

public class AddCropbox {

    public static final String RESOURCE = "resources/questions/pdfs/withoutcropbox.pdf";

    public static final String RESULT1 = "results/questions/stamppages/withcropbox.pdf";

    public static final String RESULT2 = "results/questions/stamppages/clippedpages.pdf";

    public static void main(String[] args) {
        try {
            PdfImportedPage page;
            PdfDictionary pageDict;
            PdfReader reader = new PdfReader(RESOURCE);
            Document document = new Document(reader.getPageSizeWithRotation(1));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(RESULT2));
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            int n = reader.getNumberOfPages();
            for (int i = 1; i <= n; i++) {
                page = writer.getImportedPage(reader, 1);
                cb.rectangle(0, 52, 612, 668);
                cb.clip();
                cb.newPath();
                cb.addTemplate(page, 0, 0);
                pageDict = reader.getPageN(i);
                PdfArray crop = new PdfRectangle(0, 52, 612, 720);
                pageDict.put(PdfName.CROPBOX, crop);
            }
            document.close();
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(RESULT1));
            stamper.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}
