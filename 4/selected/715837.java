package com.itextpdf.devoxx.helpers;

import java.io.IOException;
import com.itextpdf.devoxx.properties.Dimensions;
import com.itextpdf.devoxx.properties.MyColors;
import com.itextpdf.devoxx.properties.MyFonts;
import com.itextpdf.devoxx.properties.MyProperties;
import com.itextpdf.devoxx.properties.Dimensions.Dimension;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Event that adds the background of each page and the page numbers.
 */
public class EventBackgroundAndPageNumbers extends PdfPageEventHelper {

    /** Template for the odd pages. */
    protected PdfImportedPage oddPage;

    /** Template for the even pages. */
    protected PdfImportedPage evenPage;

    /** Indicates if pagenumbers are to be added. */
    protected boolean pagenumbers = true;

    /** BaseFont used for the page numbers */
    protected BaseFont bf;

    /** X,Y coordinate of the odd page numbers. */
    protected float[] pagenumberOdd = new float[2];

    /** X,Y coordinate of the even page numbers. */
    protected float[] pagenumberEven = new float[2];

    /** Rectangle for the tab. */
    protected PdfArray tabOdd;

    /** Rectangle for the tab. */
    protected PdfArray tabEven;

    /**
	 * Initialization of the member variables upon opening the document.
	 * @see com.itextpdf.text.pdf.PdfPageEventHelper#onOpenDocument(com.itextpdf.text.pdf.PdfWriter, com.itextpdf.text.Document)
	 */
    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        PdfReader reader;
        Rectangle rect;
        try {
            reader = new PdfReader(MyProperties.getOdd());
            oddPage = writer.getImportedPage(reader, 1);
            reader = new PdfReader(MyProperties.getEven());
            evenPage = writer.getImportedPage(reader, 1);
            bf = MyFonts.getBaseFont(false, false);
            rect = Dimensions.getDimension(false, Dimension.BODY);
            pagenumberOdd[0] = rect.getRight() + 35;
            pagenumberOdd[1] = rect.getBottom() - 17;
            rect = Dimensions.getDimension(true, Dimension.BODY);
            pagenumberEven[0] = rect.getLeft() - 35;
            pagenumberEven[1] = rect.getBottom() - 17;
            tabOdd = new PdfArray();
            tabEven = new PdfArray();
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        } catch (DocumentException e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
	 * Changes the vertices of the tab that has to be colored.
	 * @param s the name of a tab
	 */
    public void setTabs(String s) {
        tabOdd = Dimensions.getTab(false, s);
        tabEven = Dimensions.getTab(true, s);
    }

    /**
	 * Every time a page is finished a background and a page number are added
	 * @see com.itextpdf.text.pdf.PdfPageEventHelper#onEndPage(com.itextpdf.text.pdf.PdfWriter, com.itextpdf.text.Document)
	 */
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        int i = writer.getPageNumber() + 2;
        boolean even = (i % 2 == 0);
        PdfContentByte content = writer.getDirectContentUnder();
        PdfArray vertices;
        if (even) {
            content.addTemplate(evenPage, 0, 0);
            vertices = tabEven;
        } else {
            content.addTemplate(oddPage, 0, 0);
            vertices = tabOdd;
        }
        content.saveState();
        PdfGState state = new PdfGState();
        state.setFillOpacity(0.5f);
        state.setBlendMode(PdfGState.BM_MULTIPLY);
        content.setGState(state);
        content.setColorFill(MyColors.getColor("TAB"));
        content.moveTo(vertices.getAsNumber(0).floatValue(), vertices.getAsNumber(1).floatValue());
        for (int v = 2; v < vertices.size(); v += 2) {
            content.lineTo(vertices.getAsNumber(v).floatValue(), vertices.getAsNumber(v + 1).floatValue());
        }
        content.fill();
        content.restoreState();
        if (pagenumbers) {
            content.saveState();
            content.setColorFill(MyColors.getColor("TAB"));
            content.beginText();
            content.setFontAndSize(bf, 16);
            content.showTextAligned(Element.ALIGN_CENTER, String.valueOf(i), even ? pagenumberEven[0] : pagenumberOdd[0], even ? pagenumberEven[1] : pagenumberOdd[1], 0);
            content.endText();
            content.restoreState();
        }
        even = !even;
        document.setPageSize(Dimensions.getDimension(even, Dimension.MEDIABOX));
        writer.setCropBoxSize(Dimensions.getDimension(even, Dimension.CROPBOX));
        writer.setBoxSize("trim", Dimensions.getDimension(even, Dimension.TRIMBOX));
        writer.setBoxSize("bleed", Dimensions.getDimension(even, Dimension.BLEEDBOX));
    }

    /**
	 * At some point page numbers are no longer necessary;
	 * this method changes the state from "page numbers needed"
	 * to "no more page numbers".
	 */
    public void setNoMorePageNumbers() {
        pagenumbers = false;
    }
}
