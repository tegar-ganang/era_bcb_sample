package questions.importpages;

import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class NameCards {

    public static final String[] SHEET = { "results/questions/importpages/sheet1.pdf", "results/questions/importpages/sheet2.pdf" };

    public static void main(String[] args) {
        try {
            NameCard.createOneCard();
            createSheet(1);
            createSheet(2);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createSheet(int p) throws DocumentException, IOException {
        Rectangle rect = new Rectangle(PageSize.A4);
        Document document = new Document(rect);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(SHEET[p - 1]));
        document.open();
        PdfContentByte canvas = writer.getDirectContentUnder();
        PdfReader reader = new PdfReader(NameCard.CARD);
        PdfImportedPage front = writer.getImportedPage(reader, p);
        float x = rect.getWidth() / 2 - front.getWidth();
        float y = (rect.getHeight() - (front.getHeight() * 5)) / 2;
        canvas.setLineWidth(0.5f);
        canvas.moveTo(x, y - 15);
        canvas.lineTo(x, y);
        canvas.lineTo(x - 15, y);
        canvas.moveTo(x + front.getWidth(), y - 15);
        canvas.lineTo(x + front.getWidth(), y);
        canvas.moveTo(x + front.getWidth() * 2, y - 15);
        canvas.lineTo(x + front.getWidth() * 2, y);
        canvas.lineTo(x + front.getWidth() * 2 + 15, y);
        canvas.stroke();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                canvas.addTemplate(front, x, y);
                x += front.getWidth();
            }
            x = rect.getWidth() / 2 - front.getWidth();
            y += front.getHeight();
            canvas.moveTo(x, y);
            canvas.lineTo(x - 15, y);
            canvas.moveTo(x + front.getWidth() * 2, y);
            canvas.lineTo(x + front.getWidth() * 2 + 15, y);
            canvas.stroke();
        }
        canvas.moveTo(x, y + 15);
        canvas.lineTo(x, y);
        canvas.moveTo(x + front.getWidth(), y + 15);
        canvas.lineTo(x + front.getWidth(), y);
        canvas.moveTo(x + front.getWidth() * 2, y + 15);
        canvas.lineTo(x + front.getWidth() * 2, y);
        canvas.stroke();
        document.close();
    }
}
