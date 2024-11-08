package de.fmf.pdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import org.htmlparser.util.Translate;
import org.jdom.JDOMException;
import org.jfree.chart.JFreeChart;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import de.fmf.pdf.template.PdfPrintConroler;
import de.fmf.pdf.template.PrintItem;

public class PdfDocumentPrinter {

    private Document d;

    private String file;

    private PdfContentByte cb;

    private Rectangle documentSize;

    private PdfPrintConroler pdfc;

    private static PdfWriter writer;

    public PdfDocumentPrinter() {
    }

    /**
	 * generate a new pdf document ... the first page is inserted implicit
	 */
    public PdfDocumentPrinter(String file) throws FileNotFoundException, DocumentException {
        this.d = new Document(PageSize.A4);
        documentSize = d.getPageSize();
        this.file = (file != null) ? file : "test.pdf";
        writer = PdfWriter.getInstance(d, new FileOutputStream(this.file));
        d.open();
        cb = writer.getDirectContent();
        try {
            setDefaultFont();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultFont() throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        cb.setFontAndSize(bf, 10);
        cb.setColorStroke(Color.black);
        cb.setColorFill(Color.black);
    }

    public void setDefaultFont(int size) throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        cb.setFontAndSize(bf, size);
    }

    public void setMiniFont() throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        cb.setFontAndSize(bf, 1);
    }

    public void setFontAndSize(String font, int size) throws DocumentException, IOException {
        BaseFont bf = BaseFont.createFont(font, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        cb.setFontAndSize(bf, size);
    }

    public void setColor(Color color) {
        cb.setColorStroke(color);
        cb.setColorFill(color);
    }

    public void addParagraph(String para) throws DocumentException {
        Paragraph p = new Paragraph(para, new Font(Font.HELVETICA, 14));
        p.setAlignment(Paragraph.ALIGN_CENTER);
        d.add(p);
    }

    public void addPhrase(String phrase) throws DocumentException {
        Phrase p = new Phrase(phrase + "\n", new Font(Font.HELVETICA, 8));
        d.add(p);
    }

    public void addImage(File f) throws DocumentException, MalformedURLException, IOException {
        Image img = Image.getInstance(f.toString());
        d.add(img);
    }

    public void addImage(File f, int x_position, int y_position) throws MalformedURLException, IOException, DocumentException {
        Image img = Image.getInstance(f.toString());
        cb.addImage(img, img.getWidth(), 0, 0, img.getHeight(), x_position, y_position);
    }

    public void addText(String text, int x, int y) throws DocumentException, IOException {
        cb.beginText();
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, text, x, y, 0);
        cb.endText();
    }

    public void newPage() {
        d.newPage();
    }

    public String insertTemplate(File control) throws IOException, DocumentException, JDOMException {
        pdfc = new PdfPrintConroler();
        pdfc.loadDB(control);
        this.d = new Document(PageSize.A4);
        documentSize = d.getPageSize();
        writer = PdfWriter.getInstance(d, new FileOutputStream(pdfc.getPrintOtuputFile()));
        d.open();
        cb = writer.getDirectContent();
        try {
            setDefaultFont();
        } catch (IOException e) {
            e.printStackTrace();
        }
        insertTemplate(pdfc.getPrintTemplateFile(), control);
        return pdfc.getPrintOtuputFile();
    }

    public void insertTemplate(String pdfTemplate, File control) throws IOException, DocumentException, JDOMException {
        PdfReader reader = new PdfReader(pdfTemplate);
        for (int currentPage = 1; currentPage <= reader.getNumberOfPages(); currentPage++) {
            PdfImportedPage page = writer.getImportedPage(reader, currentPage);
            writer.getDirectContent().addTemplate(page, 1, 0, 0, 1, 0, 0);
            if (pdfc.getPrintHelpLines()) overlayLines();
            if (pdfc.getPrintDetailedCoordinates()) overlayDetailedCoordinates();
            if (pdfc.getPrintSimpleCoordinates()) overlaySimpleCoordinates();
            setDefaultFont();
            ArrayList items = pdfc.getPrintItems();
            for (Iterator iter = items.iterator(); iter.hasNext(); ) {
                PrintItem e = (PrintItem) iter.next();
                if (e.getPageNumber() == currentPage) {
                    switch(e.getType()) {
                        case PrintItem.TEXT:
                            if (e.getFont().equals("default")) setDefaultFont(e.getSize()); else setFontAndSize(e.getFont(), e.getSize());
                            if (pdfc.getPrintPositioningHelper()) {
                                setColor(Color.red);
                                drawPositioningHelpLines(e.getX_coord(), e.getY_coord());
                            } else {
                                setColor(Color.black);
                            }
                            addText(e.getText(), e.getX_coord(), e.getY_coord());
                            break;
                        default:
                            break;
                    }
                }
            }
            if (currentPage != reader.getNumberOfPages()) newPage();
        }
    }

    public void drawPositioningHelpLines(int x, int y) {
        cb.moveTo(x, y);
        cb.lineTo(x + 50, y);
        cb.lineTo(x - 50, y);
        cb.moveTo(x, y + 3);
        cb.lineTo(x + 50, y + 3);
        cb.lineTo(x - 50, y + 3);
        cb.moveTo(x, y + 6);
        cb.lineTo(x + 50, y + 6);
        cb.lineTo(x - 50, y + 6);
        cb.moveTo(x, y + 9);
        cb.lineTo(x + 50, y + 9);
        cb.lineTo(x - 50, y + 9);
        cb.moveTo(x, y);
        cb.lineTo(x, y + 50);
        cb.lineTo(x, y - 50);
        cb.stroke();
    }

    public void overlayLines() throws DocumentException, IOException {
        cb.setLineWidth(0f);
        for (int i = 0; i < documentSize.getWidth(); i = i + 10) {
            cb.moveTo(i, 0);
            cb.lineTo(i, documentSize.getHeight());
        }
        for (int i = 0; i < documentSize.getHeight(); i = i + 10) {
            cb.moveTo(0, i);
            cb.lineTo(documentSize.getWidth(), i);
        }
        cb.stroke();
    }

    public void overlaySimpleCoordinates() throws DocumentException, IOException {
        setDefaultFont();
        cb.beginText();
        for (int i = 0; i < documentSize.getHeight(); i = i + 10) {
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, i + "", 0, i, 0);
        }
        for (int i = 0; i < documentSize.getWidth(); i = i + 20) {
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, i + "", i, 0, 0);
        }
        cb.endText();
    }

    public void overlayDetailedCoordinates() throws DocumentException, IOException {
        setMiniFont();
        cb.beginText();
        for (int y = 0; y < documentSize.getHeight(); y = y + 10) {
            for (int x = 0; x < documentSize.getWidth(); x = x + 10) {
                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, x + "/" + y, x, y, 0);
            }
        }
        cb.endText();
    }

    public static void convertToPdf(JFreeChart chart, int width, int height, String filename) throws FileNotFoundException, DocumentException {
        Document document = new Document(new Rectangle(width, height));
        PdfWriter writer;
        writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        document.open();
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate(width, height);
        Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
        Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(g2d, r2d);
        g2d.dispose();
        cb.addTemplate(tp, 0, 0);
        document.close();
    }

    public void addDiagram(JFreeChart chart, float width, float height) {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate(width, height);
        Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
        Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(g2d, r2d);
        g2d.dispose();
        cb.addTemplate(tp, 0, 0);
    }

    public void addDiagram(JFreeChart chart) {
        addDiagram(chart, d.getPageSize().getWidth(), d.getPageSize().getHeight());
    }

    public void close() {
        d.close();
    }
}
