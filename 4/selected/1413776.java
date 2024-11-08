package com.itextpdf.devoxx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import com.itextpdf.devoxx.helpers.EventBackgroundAndPageNumbers;
import com.itextpdf.devoxx.pojos.Section;
import com.itextpdf.devoxx.properties.Dimensions;
import com.itextpdf.devoxx.properties.MyFonts;
import com.itextpdf.devoxx.properties.MyProperties;
import com.itextpdf.devoxx.properties.Dimensions.Dimension;
import com.itextpdf.devoxx.sections.Imported;
import com.itextpdf.devoxx.sections.Index;
import com.itextpdf.devoxx.sections.Presentations;
import com.itextpdf.devoxx.sections.Schedules;
import com.itextpdf.devoxx.sections.Speakers;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * This is the master class that creates the complete conference guide.
 */
public class ConferenceGuide {

    /** Keeps track of the page numbers and creates an overview. */
    public Index index;

    /** Keeps track of the type page. */
    public boolean even;

    /**
	 * Creates separate PDF files, each containing a section of the Conference Guide
	 * @param eventURI the event URI
	 * @throws IOException
	 * @throws DocumentException
	 */
    private void createSections(final String eventURI) throws IOException, DocumentException {
        index = new Index();
        index.setPageNumber(3);
        index.addTitle(Index.INFO.getTitle());
        index.reservePages(1);
        importPages(MyProperties.getBefore());
        index.reservePages(1);
        int n;
        index.addTitle(Speakers.INFO.getTitle());
        final Speakers speakers = new Speakers();
        n = speakers.createPdf(eventURI);
        index.reservePages(n);
        index.markOffset();
        index.addTitle(Presentations.INFO.getTitle());
        Presentations presentations = new Presentations();
        n = presentations.createPdf(eventURI, index);
        index.reservePages(n);
        index.reservePages(1);
        importPages(MyProperties.getAfter());
        index.createPdf();
        createSchedules(eventURI);
    }

    /**
	 * Creates the Schedules
	 * @param eventURI the event URI
	 * @throws DocumentException 
	 * @throws IOException 
	 */
    private void createSchedules(final String eventURI) throws IOException, DocumentException {
        final Schedules schedules = new Schedules();
        schedules.createPdf(eventURI);
    }

    /**
	 * Creates pages that aren't generated but imported.
	 * @param  content information about the content that needs to be imported
	 * @throws DocumentException 
	 * @throws IOException 
	 */
    private void importPages(final Section[] content) throws IOException, DocumentException {
        int n;
        for (final Section aContent : content) {
            index.addTitle(aContent.getTitle());
            n = new Imported().createPdf(aContent.getOutput(), aContent.getInput());
            index.reservePages(n);
        }
    }

    /**
	 * Imports all the pages from an existing PDF into the Conference Guide.
	 * @param document the Document object
	 * @param content a PdfContentByte
	 * @param reader reads the existing document 
	 * @param title the caption of the pages that will be imported
	 * @throws DocumentException
	 */
    private void importPages(final Document document, final PdfContentByte content, final PdfReader reader, final String title) throws DocumentException {
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            addTitleLeft(content, Dimensions.getTitleArea(even), title, MyFonts.TITLE);
            content.addTemplate(content.getPdfWriter().getImportedPage(reader, i), Dimensions.getOffsetX(even), Dimensions.getOffsetY(even));
            document.newPage();
            even = !even;
        }
    }

    /**
	 * Imports all the pages from an existing PDF into the Conference Guide.
	 * @param document the Document object
	 * @param content a PdfContentByte
	 * @param pages
     * @param event
     * @throws DocumentException
	 * @throws IOException 
	 */
    private void importPages(final Document document, final PdfContentByte content, final Section[] pages, final EventBackgroundAndPageNumbers event) throws DocumentException, IOException {
        for (final Section page : pages) {
            event.setTabs(page.getTab());
            importPages(document, content, new PdfReader(page.getOutput()), page.getTitle());
        }
    }

    /**
	 * Adds a place holder page for ads.
	 * @param document the Document
	 * @param canvas the PdfContentByte object
	 * @throws DocumentException
	 */
    private void addAdPage(final Document document, final PdfContentByte canvas) throws DocumentException {
        addTitleLeft(canvas, Dimensions.getTitleArea(even), MyProperties.getMiddle()[3].getTitle(), MyFonts.TITLE);
        document.newPage();
        even = !even;
    }

    /**
	 * Adds a title to the left in the title bar.
	 * @param content
	 * @param rect
	 * @param title
	 * @param font
	 * @throws DocumentException
	 */
    public void addTitleLeft(final PdfContentByte content, final Rectangle rect, final String title, final Font font) throws DocumentException {
        ColumnText.showTextAligned(content, Element.ALIGN_LEFT, new Phrase(title, font), rect.getLeft(), rect.getBottom(), 0);
    }

    /**
	 * Adds a title to the right in the title bar.
	 * @param content
	 * @param rect
	 * @param title
	 * @param font
	 * @throws DocumentException
	 */
    public void addTitleRight(final PdfContentByte content, final Rectangle rect, final String title, final Font font) throws DocumentException {
        ColumnText.showTextAligned(content, Element.ALIGN_RIGHT, new Phrase(title, font), rect.getRight(), rect.getBottom(), 0);
    }

    /**
	 * Creates the full conference guide.
     *
	 * @param eventURI the name of the conference guide
     * @throws com.itextpdf.text.DocumentException
     * @throws java.io.IOException
	 */
    public void createPdf(final String eventURI) throws IOException, DocumentException {
        createSections(eventURI);
        even = false;
        final Document document = new Document(Dimensions.getDimension(even, Dimension.MEDIABOX));
        final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(MyProperties.getOutput()));
        writer.setViewerPreferences(PdfWriter.PageLayoutTwoColumnRight);
        writer.setCropBoxSize(Dimensions.getDimension(even, Dimension.CROPBOX));
        writer.setBoxSize("trim", Dimensions.getDimension(even, Dimension.TRIMBOX));
        writer.setBoxSize("bleed", Dimensions.getDimension(even, Dimension.BLEEDBOX));
        final EventBackgroundAndPageNumbers event = new EventBackgroundAndPageNumbers();
        writer.setPageEvent(event);
        document.open();
        final PdfContentByte content = writer.getDirectContent();
        event.setTabs(Index.INFO.getTab());
        importPages(document, content, new PdfReader(Index.INFO.getOutput()), Index.INFO.getTitle());
        importPages(document, content, MyProperties.getBefore(), event);
        addAdPage(document, content);
        PdfReader reader = new PdfReader(Presentations.INFO.getOutput());
        String[] titles = { "", "" };
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            titles = index.getSubtitle(titles, i);
            event.setTabs(titles[0].toLowerCase());
            addTitleLeft(content, Dimensions.getTitleArea(even), titles[0], MyFonts.TITLE);
            addTitleRight(content, Dimensions.getTitleArea(even), titles[1], MyFonts.DATE);
            content.addTemplate(writer.getImportedPage(reader, i), Dimensions.getOffsetX(even), Dimensions.getOffsetY(even));
            document.newPage();
            even = !even;
        }
        addAdPage(document, content);
        importPages(document, content, MyProperties.getAfter(), event);
        int total = writer.getPageNumber() - 1;
        event.setNoMorePageNumbers();
        event.setTabs(Schedules.INFO.getTab());
        reader = new PdfReader(Schedules.INFO.getOutput());
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            addTitleLeft(content, Dimensions.getTitleArea(even), Schedules.INFO.getTitle(), MyFonts.TITLE);
            content.addTemplate(writer.getImportedPage(reader, i), Dimensions.getOffsetX(even), Dimensions.getOffsetY(even));
            document.newPage();
            even = !even;
        }
        document.close();
        final File file = new File(MyProperties.getOutput());
        final byte[] original = new byte[(int) file.length()];
        final FileInputStream f = new FileInputStream(file);
        f.read(original);
        reader = new PdfReader(original);
        final List<Integer> ranges = new ArrayList<Integer>();
        for (int i = 1; i <= total; i++) {
            ranges.add(i);
            if (i == total / 2) {
                for (int j = total + 1; j <= reader.getNumberOfPages(); j++) {
                    ranges.add(j);
                }
            }
        }
        reader.selectPages(ranges);
        final PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(MyProperties.getOutput()));
        stamper.close();
    }

    /**
	 * Creates the conference guide.
	 * @param args no arguments needed
	 * @throws JSONException 
	 * @throws IOException
	 * @throws DocumentException
	 */
    public static void main(String[] args) throws JSONException, IOException, DocumentException {
        ConferenceGuide guide = new ConferenceGuide();
        guide.createPdf(MyProperties.getEventURI());
    }
}
