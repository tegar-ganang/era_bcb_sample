package com.rapidminer.gui.report;

import java.io.IOException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

/** 
 * This class provides some post production features used by PdfReportStream. 
 * 
 * @author Helge Homburg
 * @version $Id$
 */
public class ReportPostProduction extends PdfPageEventHelper {

    private PdfReader reader;

    private boolean useTemplate;

    private boolean landscape;

    private float currentPagePosition;

    private boolean printPageNumbers;

    public ReportPostProduction(PdfReader reader, boolean useTemplate, boolean printPageNumbers, boolean landscape) {
        super();
        this.reader = reader;
        this.useTemplate = useTemplate;
        this.landscape = landscape;
        this.printPageNumbers = printPageNumbers;
    }

    public float getCurrentPagePosition() {
        return this.currentPagePosition;
    }

    public void enablePageNumberPrinting(boolean printPageNumbers) {
        this.printPageNumbers = printPageNumbers;
    }

    public void enableTemplate(boolean useTemplate) {
        this.useTemplate = useTemplate;
    }

    @Override
    public void onParagraphEnd(PdfWriter writer, Document document, float position) {
        this.currentPagePosition = position;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        this.currentPagePosition = document.getPageSize().getHeight();
        if (useTemplate) {
            try {
                PdfContentByte cb = writer.getDirectContentUnder();
                PdfImportedPage templatePage = writer.getImportedPage(reader, 1);
                if (landscape) {
                    cb.addTemplate(templatePage, 0, -1, 1, 0, 0, document.getPageSize().getHeight());
                } else {
                    cb.addTemplate(templatePage, 1, 0, 0, 1, 0, 0);
                }
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }
        if (printPageNumbers) {
            PdfContentByte cb = writer.getDirectContent();
            int currentPageNumber = writer.getCurrentPageNumber();
            int textAlignment;
            int horizontalTextPosition = 0;
            if (currentPageNumber % 2 == 0) {
                textAlignment = Element.ALIGN_LEFT;
                horizontalTextPosition = (int) document.left() + 20;
            } else {
                textAlignment = Element.ALIGN_RIGHT;
                horizontalTextPosition = (int) document.right() - 20;
            }
            try {
                cb.beginText();
                cb.setFontAndSize(BaseFont.createFont("Helvetica", BaseFont.WINANSI, false), 12.0f);
                cb.showTextAligned(textAlignment, "" + currentPageNumber, horizontalTextPosition, document.bottom() - 17, 0);
                cb.endText();
            } catch (IOException e) {
            } catch (DocumentException e) {
            }
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        if (reader != null) {
            this.reader.close();
        }
    }
}
