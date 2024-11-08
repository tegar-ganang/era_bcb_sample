package questions.importpages;

import java.io.FileOutputStream;
import java.io.IOException;
import com.lowagie.text.Chapter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Section;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class HelloWorldImportedPages {

    public static final String SOURCE = "results/questions/importpages/portrait_landscape.pdf";

    public static final String RESULT = "results/questions/importpages/portrait_landscape_thumbs.pdf";

    public static void main(String[] args) {
        createPdf(SOURCE);
        Document document = new Document(PageSize.A4);
        try {
            PdfReader reader = new PdfReader(SOURCE);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(RESULT));
            document.open();
            PdfImportedPage page;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                page = writer.getImportedPage(reader, i);
                Image image = Image.getInstance(page);
                image.scalePercent(15f);
                image.setBorder(Rectangle.BOX);
                image.setBorderWidth(3f);
                image.setBorderColor(new GrayColor(0.5f));
                image.setRotationDegrees(-reader.getPageRotation(i));
                document.add(image);
                document.add(new Paragraph("This is page: " + i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        document.close();
    }

    /**
	 * Generates a PDF file with bookmarks.
	 * 
	 * @param filename
	 *            the filename of the PDF file.
	 */
    private static void createPdf(String filename) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filename));
            document.open();
            document.add(new Paragraph("In this document, we are going to say hello to different beings in different languages."));
            document.newPage();
            Paragraph hello = new Paragraph("(English:) hello, " + "(Esperanto:) he, alo, saluton, (Latin:) heu, ave, " + "(French:) allô, (Italian:) ciao, (German:) hallo, he, heda, holla, " + "(Portuguese:) alô, olá, hei, psiu, bom día, (Dutch:) hallo, dag, " + "(Spanish:) ola, eh, (Catalan:) au, bah, eh, ep, " + "(Swedish:) hej, hejsan(Danish:) hallo, dav, davs, goddag, hej, " + "(Norwegian:) hei; morn, (Papiamento:) halo; hallo; kí tal, " + "(Faeroese:) halló, hoyr, (Turkish:) alo, merhaba, (Albanian:) tungjatjeta");
            Chapter universe = new Chapter("To the Universe:", 1);
            Section section;
            section = universe.addSection("to the World:");
            section.add(hello);
            section = universe.addSection("to the Sun:");
            section.add(hello);
            section = universe.addSection("to the Moon:");
            section.add(hello);
            section = universe.addSection("to the Stars:");
            section.add(hello);
            document.add(universe);
            Chapter people = new Chapter("To the People:", 2);
            section = people.addSection("to mothers and fathers:");
            section.add(hello);
            section = people.addSection("to brothers and sisters:");
            section.add(hello);
            section = people.addSection("to wives and husbands:");
            section.add(hello);
            section = people.addSection("to sons and daughters:");
            section.add(hello);
            section = people.addSection("to complete strangers:");
            section.add(hello);
            document.add(people);
            document.setPageSize(PageSize.A4.rotate());
            Chapter animals = new Chapter("To the Animals:", 3);
            section = animals.addSection("to cats and dogs:");
            section.add(hello);
            section = animals.addSection("to birds and bees:");
            section.add(hello);
            section = animals.addSection("to farm animals and wild animals:");
            section.add(hello);
            section = animals.addSection("to bugs and beatles:");
            section.add(hello);
            section = animals.addSection("to fish and shellfish:");
            section.add(hello);
            document.add(animals);
        } catch (DocumentException de) {
            System.err.println(de.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        document.close();
    }
}
