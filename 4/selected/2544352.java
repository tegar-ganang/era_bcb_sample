package questions.importpages;

import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Utilities;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.CMYKColor;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class NameCard {

    public static final String RESULT = "results/questions/importpages/imported_card.pdf";

    public static final String CARD = "results/questions/importpages/card.pdf";

    public static final String LOGO = "resources/questions/pdfs/logo.pdf";

    public static final String FONT = "resources/questions/fonts/Tuffy.otf";

    public static void main(String[] args) {
        try {
            createOneCard();
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(RESULT));
            PdfReader reader = new PdfReader(CARD);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            canvas.addTemplate(writer.getImportedPage(reader, 1), 36, 600);
            canvas.addTemplate(writer.getImportedPage(reader, 2), 200, 600);
            canvas.moveTo(0, 600);
            canvas.lineTo(595, 600);
            canvas.stroke();
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createOneCard() throws DocumentException, IOException {
        Rectangle rect = new Rectangle(Utilities.millimetersToPoints(86.5f), Utilities.millimetersToPoints(55));
        Document document = new Document(rect);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(CARD));
        writer.setViewerPreferences(PdfWriter.PrintScalingNone);
        document.open();
        PdfReader reader = new PdfReader(LOGO);
        Image img = Image.getInstance(writer.getImportedPage(reader, 1));
        img.scaleToFit(rect.getWidth() / 1.5f, rect.getHeight() / 1.5f);
        img.setAbsolutePosition((rect.getWidth() - img.getScaledWidth()) / 2, (rect.getHeight() - img.getScaledHeight()) / 2);
        document.add(img);
        document.newPage();
        BaseFont bf = BaseFont.createFont(FONT, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        Font font = new Font(bf, 12);
        font.setColor(new CMYKColor(1, 0.5f, 0, 0.467f));
        ColumnText column = new ColumnText(writer.getDirectContent());
        Paragraph p;
        p = new Paragraph("Bruno Lowagie\n1T3XT\nbruno@1t3xt.com", font);
        p.setAlignment(Element.ALIGN_CENTER);
        column.addElement(p);
        column.setSimpleColumn(0, 0, rect.getWidth(), rect.getHeight() * 0.75f);
        column.go();
        document.close();
    }
}
