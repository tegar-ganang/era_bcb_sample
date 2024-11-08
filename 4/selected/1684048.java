package com.itextpdf.demo.forms;

import java.io.IOException;
import com.itextpdf.text.Document;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;

public class MyPageEvent extends PdfPageEventHelper {

    protected PdfTemplate background;

    protected float[] name = new float[4];

    protected float[] bio = new float[4];

    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            PdfReader reader = new PdfReader(FormFillOut1.FORM);
            background = writer.getImportedPage(reader, 1);
            AcroFields fields = reader.getAcroFields();
            FieldPosition pos;
            pos = fields.getFieldPositions("name").get(0);
            fillArray(name, pos.position);
            pos = fields.getFieldPositions("bio").get(0);
            fillArray(bio, pos.position);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

    private void fillArray(float[] array, Rectangle position) {
        array[0] = position.getLeft();
        array[1] = position.getBottom();
        array[2] = position.getRight();
        array[3] = position.getTop();
    }

    public float[] getName() {
        return name;
    }

    public float[] getBio() {
        return bio;
    }

    public void onEndPage(PdfWriter writer, Document document) {
        writer.getDirectContentUnder().addTemplate(background, 0, 0);
    }
}
