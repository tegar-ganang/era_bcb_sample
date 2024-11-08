package com.itextpdf.demo.speakers;

import java.io.IOException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;

public class BackgroundAndPageNumber extends PdfPageEventHelper {

    protected PdfTemplate odd;

    protected FieldPosition odd_title;

    protected FieldPosition odd_body;

    protected PdfTemplate even;

    protected FieldPosition even_title;

    protected FieldPosition even_body;

    protected BaseFont bf;

    public float[] getTitleOffset(boolean odd) {
        if (odd) return new float[] { odd_title.position.getLeft(), odd_title.position.getBottom() }; else return new float[] { even_title.position.getLeft(), even_title.position.getBottom() };
    }

    public float[] getBodyOffset(boolean odd) {
        if (odd) return new float[] { odd_body.position.getLeft(), odd_body.position.getBottom() }; else return new float[] { even_body.position.getLeft(), even_body.position.getBottom() };
    }

    public float getBodyHeight(boolean odd) {
        if (odd) return odd_body.position.getHeight(); else return even_body.position.getHeight();
    }

    public float getBodyWidth(boolean odd) {
        if (odd) return odd_body.position.getWidth(); else return even_body.position.getWidth();
    }

    public void onOpenDocument(PdfWriter writer, Document document) {
        PdfReader reader;
        AcroFields form;
        try {
            reader = new PdfReader("resources/odd.pdf");
            odd = writer.getImportedPage(reader, 1);
            form = reader.getAcroFields();
            odd_title = form.getFieldPositions("title").get(0);
            odd_body = form.getFieldPositions("body").get(0);
            reader = new PdfReader("resources/even.pdf");
            even = writer.getImportedPage(reader, 1);
            form = reader.getAcroFields();
            even_title = form.getFieldPositions("title").get(0);
            even_body = form.getFieldPositions("body").get(0);
            bf = BaseFont.createFont();
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        int page = writer.getPageNumber();
        float x, y;
        if (page % 2 == 1) {
            canvas.addTemplate(odd, 0, 0);
            x = odd_body.position.getRight() + 40;
            y = odd_body.position.getBottom() - 5;
        } else {
            canvas.addTemplate(even, 0, 0);
            x = odd_body.position.getLeft() - 20;
            y = odd_body.position.getBottom() - 5;
        }
        canvas.beginText();
        canvas.setFontAndSize(bf, 24);
        canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf(page), x, y, 0);
        canvas.endText();
    }
}
