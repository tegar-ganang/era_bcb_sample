package com.generatescape.pdf;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.jdom.Text;
import com.generatescape.baseobjects.ArticleObject;
import com.generatescape.baseobjects.CONSTANTS;
import com.generatescape.preferences.PrefPageOne;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import framework.BrowserApp;

public class PDFDocumentFactory {

    private static final String NO_TITLE_AVAILABLE = "No Title Available";

    private static final String CR = "\n";

    private static final String CREATED = "Created: ";

    private static PDFDocumentConfig config;

    private boolean feeddescs = false;

    /** Value to decide whether a Table fits on a page */
    private static final int CHARACTERS_PER_PAGE = 3000;

    /** Document to write */
    private Document document;

    /**
   * Instantiate a new PDFDocumentFactory
   * 
   * @param rssChannel
   *          The selected Channel
   * @param fileOS
   *          File to write document into
   * @param format
   *          PDF
   * @throws DocumentException
   *           in all cases
   */
    public PDFDocumentFactory(FileOutputStream fileOS, boolean chapters, boolean feeddescs) throws DocumentException {
        this.feeddescs = feeddescs;
        config = new PDFDocumentConfig();
        config.initDefaultFonts();
        document = new Document();
        initPDF(fileOS);
    }

    /**
   * Writes meta data, footer and articles
   * 
   * @param channels
   *          The articles to write
   * @throws DocumentException
   *           If an warning occurs
   */
    public void createPDF(ArrayList channels) throws DocumentException {
        DocumentException documentException = null;
        addMetaData();
        try {
            addFooter();
            document.open();
            addImage();
            addChannel(channels);
        } catch (DocumentException e) {
            documentException = e;
        }
        document.close();
        if (documentException != null) {
            throw documentException;
        }
    }

    /**
   * Writes an image
   * 
   */
    private void addImage() {
        try {
            Image jpg = null;
            jpg = Image.getInstance(BrowserApp.getPath() + "\\icons\\bigblogzoocatlogo.png");
            document.add(jpg);
        } catch (BadElementException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
   * Remove HTML tags from the given String
   * 
   * @param str
   *          The String to remove the Tags from
   * @return String The String with all tags removed
   */
    private static String stripTags(String str) {
        if (str == null || str.indexOf('<') == -1 || str.indexOf('>') == -1) {
            return str;
        }
        str = CONSTANTS.HTML_TAG_REGEX_PATTERN.matcher(str).replaceAll("");
        return str;
    }

    /**
   * Init the pdf
   * 
   * @param file
   *          The path to the file where to save the document after generation
   * @throws DocumentException
   *           If an error occurs
   */
    private void initPDF(FileOutputStream file) throws DocumentException {
        PdfWriter.getInstance(document, file).setPageEvent(new PdfPageEventHelper() {

            public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
                writer.getDirectContent();
            }
        });
    }

    /**
   * Write footer into document
   */
    private void addFooter() {
        Chunk footerChunk = new Chunk(CREATED + new Date().toString(), config.getSmallItalicFont());
        HeaderFooter footer = new HeaderFooter(new Phrase(footerChunk), false);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setBorder(1);
        document.setFooter(footer);
    }

    /** Write meta informations t */
    private void addMetaData() {
        document.addTitle(PrefPageOne.getValue(CONSTANTS.PREF_FEED_TITLE));
        document.addCreationDate();
        document.addCreator(PrefPageOne.getValue(CONSTANTS.PREF_FEED_URL));
    }

    /**
   * Add articles to the document
   * 
   * @param articles
   *          The articles
   * @throws DocumentException
   *           in all cases
   */
    private void addChannel(ArrayList articles) throws DocumentException {
        for (Iterator iter = articles.iterator(); iter.hasNext(); ) {
            ArticleObject element = (ArticleObject) iter.next();
            document.add(new Paragraph());
            if (feeddescs) {
                String title = element.getChannelTitle() + CR + element.getTitle();
                writeArticle(element, title);
            } else {
                writeArticle(element, element.getTitle());
            }
        }
    }

    /**
   * Write a article to the document
   * 
   * @param article
   *          article to write
   * @param articleTitle
   *          title of the article
   * @throws DocumentException
   *           in all cases
   */
    private void writeArticle(ArticleObject article, String articleTitle) throws DocumentException {
        Table table = new Table(1, 2);
        table.setBorder(0);
        table.setPadding(7);
        table.setWidth(100);
        Cell titleCell = new Cell();
        titleCell.setBorderColor(Color.GRAY);
        Paragraph title = new Paragraph();
        Chunk titleChunk = new Chunk(articleTitle);
        if (article.getUrl() != null && !article.getUrl().equals("")) {
            titleChunk.setFont(config.getLinkFont());
            titleChunk.setAnchor(article.getUrl());
        } else {
            titleChunk.setFont(config.getBoldFont());
        }
        String feedTitle = (article.getTitle() != null && !article.getTitle().equals("")) ? article.getTitle() : (NO_TITLE_AVAILABLE);
        titleChunk.setGenericTag(feedTitle);
        title.add(titleChunk);
        titleCell.add(title);
        table.addCell(titleCell);
        Cell descriptionCell = new Cell();
        descriptionCell.setBorderColor(Color.GRAY);
        StringBuffer description = new StringBuffer();
        description.append(Text.normalizeString(article.getDescription()));
        Paragraph descriptionContainer = new Paragraph();
        Chunk descriptionChunk;
        descriptionChunk = new Chunk(stripTags(description.toString()), config.getNormalFont());
        descriptionContainer.add(descriptionChunk);
        descriptionCell.add(descriptionContainer);
        table.addCell(descriptionCell);
        Cell dateCell = new Cell();
        dateCell.setBorderColor(Color.GRAY);
        StringBuffer date = new StringBuffer();
        date.append((article.getDateFound()));
        Paragraph dateContainer = new Paragraph();
        Chunk dateChunk;
        dateChunk = new Chunk((date.toString()), config.getNormalFont());
        dateContainer.add(dateChunk);
        dateCell.add(dateContainer);
        table.addCell(dateCell);
        table.setTableFitsPage(descriptionChunk.content().length() < CHARACTERS_PER_PAGE);
        document.add(table);
    }
}
