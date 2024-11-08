package net.sf.gateway.mef.pdf.ty2011;

import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.BaseColor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Generated class for filling 'IN153'
 */
public class IN153 {

    /**
	 * Form Filler -- Call this method once you have set all the form fields.
	 * @param src The source -- a fillable PDF.
	 * @param dest The destination -- a filled PDF.
	 * @throws IOException thrown when one of the files cannot be opened.
	 * @throws DocumentException thrown when one of the fields cannot be filled.
	 */
    public void fill(String src, String dest, String user) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(src);
        FileOutputStream writer = new FileOutputStream(dest);
        PdfStamper stamper = new PdfStamper(reader, writer);
        stamper.setEncryption(true, "", "Gu7ruc*YAWaStEbr", 0);
        AcroFields fields = stamper.getAcroFields();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD);
        font.setSize((float) 20.2);
        BaseFont baseFont = font.getBaseFont();
        fields.setFieldProperty("Sch1531Line21", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line21", "textfont", baseFont, null);
        fields.setField("Sch1531Line21", this.get_Sch1531Line21());
        fields.setFieldProperty("Sch1531Line2b", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line2b", "textfont", baseFont, null);
        fields.setField("Sch1531Line2b", this.get_Sch1531Line2b());
        fields.setFieldProperty("Sch1531Line17", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line17", "textfont", baseFont, null);
        fields.setField("Sch1531Line17", this.get_Sch1531Line17());
        fields.setFieldProperty("Sch1531Line10", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line10", "textfont", baseFont, null);
        fields.setField("Sch1531Line10", this.get_Sch1531Line10());
        fields.setFieldProperty("Sch1531Line13b", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line13b", "textfont", baseFont, null);
        fields.setField("Sch1531Line13b", this.get_Sch1531Line13b());
        fields.setFieldProperty("Sch1531Line22", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line22", "textfont", baseFont, null);
        fields.setField("Sch1531Line22", this.get_Sch1531Line22());
        fields.setFieldProperty("Sch1531Line5a", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line5a", "textfont", baseFont, null);
        fields.setField("Sch1531Line5a", this.get_Sch1531Line5a());
        fields.setFieldProperty("Sch1531Line3", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line3", "textfont", baseFont, null);
        fields.setField("Sch1531Line3", this.get_Sch1531Line3());
        fields.setFieldProperty("Sch1531Line19", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line19", "textfont", baseFont, null);
        fields.setField("Sch1531Line19", this.get_Sch1531Line19());
        fields.setFieldProperty("Sch1531Line12", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line12", "textfont", baseFont, null);
        fields.setField("Sch1531Line12", this.get_Sch1531Line12());
        fields.setFieldProperty("Sch1531Line4", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line4", "textfont", baseFont, null);
        fields.setField("Sch1531Line4", this.get_Sch1531Line4());
        fields.setFieldProperty("Sch1531Line2a", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line2a", "textfont", baseFont, null);
        fields.setField("Sch1531Line2a", this.get_Sch1531Line2a());
        fields.setFieldProperty("Sch1531Line13a", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line13a", "textfont", baseFont, null);
        fields.setField("Sch1531Line13a", this.get_Sch1531Line13a());
        fields.setFieldProperty("Sch1531Line18", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line18", "textfont", baseFont, null);
        fields.setField("Sch1531Line18", this.get_Sch1531Line18());
        fields.setFieldProperty("Sch1531Line1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line1", "textfont", baseFont, null);
        fields.setField("Sch1531Line1", this.get_Sch1531Line1());
        fields.setFieldProperty("Sch1531Line5b", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line5b", "textfont", baseFont, null);
        fields.setField("Sch1531Line5b", this.get_Sch1531Line5b());
        fields.setFieldProperty("Sch1531Line16", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line16", "textfont", baseFont, null);
        fields.setField("Sch1531Line16", this.get_Sch1531Line16());
        fields.setFieldProperty("Sch1531Line5e", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line5e", "textfont", baseFont, null);
        fields.setField("Sch1531Line5e", this.get_Sch1531Line5e());
        fields.setFieldProperty("Sch1531Line9", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line9", "textfont", baseFont, null);
        fields.setField("Sch1531Line9", this.get_Sch1531Line9());
        fields.setFieldProperty("Sch1531Line13c", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line13c", "textfont", baseFont, null);
        fields.setField("Sch1531Line13c", this.get_Sch1531Line13c());
        fields.setFieldProperty("Sch1531Line11", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line11", "textfont", baseFont, null);
        fields.setField("Sch1531Line11", this.get_Sch1531Line11());
        fields.setFieldProperty("Sch1531Line6", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line6", "textfont", baseFont, null);
        fields.setField("Sch1531Line6", this.get_Sch1531Line6());
        fields.setFieldProperty("Sch1531Line15", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line15", "textfont", baseFont, null);
        fields.setField("Sch1531Line15", this.get_Sch1531Line15());
        fields.setFieldProperty("Sch1531Line8", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line8", "textfont", baseFont, null);
        fields.setField("Sch1531Line8", this.get_Sch1531Line8());
        fields.setFieldProperty("Sch1531Line5d", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line5d", "textfont", baseFont, null);
        fields.setField("Sch1531Line5d", this.get_Sch1531Line5d());
        fields.setFieldProperty("Sch1531Line20", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line20", "textfont", baseFont, null);
        fields.setField("Sch1531Line20", this.get_Sch1531Line20());
        fields.setFieldProperty("Sch1531Line14", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line14", "textfont", baseFont, null);
        fields.setField("Sch1531Line14", this.get_Sch1531Line14());
        fields.setFieldProperty("Sch1531Line7", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line7", "textfont", baseFont, null);
        fields.setField("Sch1531Line7", this.get_Sch1531Line7());
        fields.setFieldProperty("Sch1531Line5c", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch1531Line5c", "textfont", baseFont, null);
        fields.setField("Sch1531Line5c", this.get_Sch1531Line5c());
        stamper.setFormFlattening(true);
        stamper.setFullCompression();
        for (int i = 0; i < reader.getNumberOfPages() + 1; i++) {
            PdfContentByte overContent = stamper.getOverContent(i);
            if (overContent != null) {
                overContent.beginText();
                font = FontFactory.getFont(FontFactory.TIMES_ITALIC);
                font.setColor(BaseColor.BLUE);
                baseFont = font.getBaseFont();
                overContent.setColorFill(BaseColor.BLUE);
                overContent.setFontAndSize(baseFont, 24);
                overContent.showTextAligned(Element.ALIGN_RIGHT | Element.ALIGN_TOP, "Electronically filed via Modernized eFile", 20, 175, 90);
                overContent.endText();
                overContent.beginText();
                font = FontFactory.getFont(FontFactory.TIMES);
                font.setColor(BaseColor.RED);
                baseFont = font.getBaseFont();
                overContent.setColorFill(BaseColor.RED);
                overContent.setFontAndSize(baseFont, 8);
                overContent.showTextAligned(Element.ALIGN_CENTER | Element.ALIGN_BOTTOM, "Retrieved by " + user + " on " + new Date().toString(), 220, 3, 0);
                overContent.endText();
            }
        }
        stamper.close();
        reader.close();
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line21' in the PDF.
	 */
    private String _Sch1531Line21 = "";

    /**
	 * Mutator Method for xSch1531Line21
	 * @param Sch1531Line21 the new value for 'Sch1531Line21'
	 */
    public void set_Sch1531Line21(String _Sch1531Line21) {
        this._Sch1531Line21 = _Sch1531Line21;
    }

    /**
	 * Accessor Method for xSch1531Line21
	 * @return the value of 'Sch1531Line21'
	 */
    public String get_Sch1531Line21() {
        return this._Sch1531Line21;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line2b' in the PDF.
	 */
    private String _Sch1531Line2b = "";

    /**
	 * Mutator Method for xSch1531Line2b
	 * @param Sch1531Line2b the new value for 'Sch1531Line2b'
	 */
    public void set_Sch1531Line2b(String _Sch1531Line2b) {
        this._Sch1531Line2b = _Sch1531Line2b;
    }

    /**
	 * Accessor Method for xSch1531Line2b
	 * @return the value of 'Sch1531Line2b'
	 */
    public String get_Sch1531Line2b() {
        return this._Sch1531Line2b;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line17' in the PDF.
	 */
    private String _Sch1531Line17 = "";

    /**
	 * Mutator Method for xSch1531Line17
	 * @param Sch1531Line17 the new value for 'Sch1531Line17'
	 */
    public void set_Sch1531Line17(String _Sch1531Line17) {
        this._Sch1531Line17 = _Sch1531Line17;
    }

    /**
	 * Accessor Method for xSch1531Line17
	 * @return the value of 'Sch1531Line17'
	 */
    public String get_Sch1531Line17() {
        return this._Sch1531Line17;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line10' in the PDF.
	 */
    private String _Sch1531Line10 = "";

    /**
	 * Mutator Method for xSch1531Line10
	 * @param Sch1531Line10 the new value for 'Sch1531Line10'
	 */
    public void set_Sch1531Line10(String _Sch1531Line10) {
        this._Sch1531Line10 = _Sch1531Line10;
    }

    /**
	 * Accessor Method for xSch1531Line10
	 * @return the value of 'Sch1531Line10'
	 */
    public String get_Sch1531Line10() {
        return this._Sch1531Line10;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line13b' in the PDF.
	 */
    private String _Sch1531Line13b = "";

    /**
	 * Mutator Method for xSch1531Line13b
	 * @param Sch1531Line13b the new value for 'Sch1531Line13b'
	 */
    public void set_Sch1531Line13b(String _Sch1531Line13b) {
        this._Sch1531Line13b = _Sch1531Line13b;
    }

    /**
	 * Accessor Method for xSch1531Line13b
	 * @return the value of 'Sch1531Line13b'
	 */
    public String get_Sch1531Line13b() {
        return this._Sch1531Line13b;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line22' in the PDF.
	 */
    private String _Sch1531Line22 = "";

    /**
	 * Mutator Method for xSch1531Line22
	 * @param Sch1531Line22 the new value for 'Sch1531Line22'
	 */
    public void set_Sch1531Line22(String _Sch1531Line22) {
        this._Sch1531Line22 = _Sch1531Line22;
    }

    /**
	 * Accessor Method for xSch1531Line22
	 * @return the value of 'Sch1531Line22'
	 */
    public String get_Sch1531Line22() {
        return this._Sch1531Line22;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line5a' in the PDF.
	 */
    private String _Sch1531Line5a = "";

    /**
	 * Mutator Method for xSch1531Line5a
	 * @param Sch1531Line5a the new value for 'Sch1531Line5a'
	 */
    public void set_Sch1531Line5a(String _Sch1531Line5a) {
        this._Sch1531Line5a = _Sch1531Line5a;
    }

    /**
	 * Accessor Method for xSch1531Line5a
	 * @return the value of 'Sch1531Line5a'
	 */
    public String get_Sch1531Line5a() {
        return this._Sch1531Line5a;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line3' in the PDF.
	 */
    private String _Sch1531Line3 = "";

    /**
	 * Mutator Method for xSch1531Line3
	 * @param Sch1531Line3 the new value for 'Sch1531Line3'
	 */
    public void set_Sch1531Line3(String _Sch1531Line3) {
        this._Sch1531Line3 = _Sch1531Line3;
    }

    /**
	 * Accessor Method for xSch1531Line3
	 * @return the value of 'Sch1531Line3'
	 */
    public String get_Sch1531Line3() {
        return this._Sch1531Line3;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line19' in the PDF.
	 */
    private String _Sch1531Line19 = "";

    /**
	 * Mutator Method for xSch1531Line19
	 * @param Sch1531Line19 the new value for 'Sch1531Line19'
	 */
    public void set_Sch1531Line19(String _Sch1531Line19) {
        this._Sch1531Line19 = _Sch1531Line19;
    }

    /**
	 * Accessor Method for xSch1531Line19
	 * @return the value of 'Sch1531Line19'
	 */
    public String get_Sch1531Line19() {
        return this._Sch1531Line19;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line12' in the PDF.
	 */
    private String _Sch1531Line12 = "";

    /**
	 * Mutator Method for xSch1531Line12
	 * @param Sch1531Line12 the new value for 'Sch1531Line12'
	 */
    public void set_Sch1531Line12(String _Sch1531Line12) {
        this._Sch1531Line12 = _Sch1531Line12;
    }

    /**
	 * Accessor Method for xSch1531Line12
	 * @return the value of 'Sch1531Line12'
	 */
    public String get_Sch1531Line12() {
        return this._Sch1531Line12;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line4' in the PDF.
	 */
    private String _Sch1531Line4 = "";

    /**
	 * Mutator Method for xSch1531Line4
	 * @param Sch1531Line4 the new value for 'Sch1531Line4'
	 */
    public void set_Sch1531Line4(String _Sch1531Line4) {
        this._Sch1531Line4 = _Sch1531Line4;
    }

    /**
	 * Accessor Method for xSch1531Line4
	 * @return the value of 'Sch1531Line4'
	 */
    public String get_Sch1531Line4() {
        return this._Sch1531Line4;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line2a' in the PDF.
	 */
    private String _Sch1531Line2a = "";

    /**
	 * Mutator Method for xSch1531Line2a
	 * @param Sch1531Line2a the new value for 'Sch1531Line2a'
	 */
    public void set_Sch1531Line2a(String _Sch1531Line2a) {
        this._Sch1531Line2a = _Sch1531Line2a;
    }

    /**
	 * Accessor Method for xSch1531Line2a
	 * @return the value of 'Sch1531Line2a'
	 */
    public String get_Sch1531Line2a() {
        return this._Sch1531Line2a;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line13a' in the PDF.
	 */
    private String _Sch1531Line13a = "";

    /**
	 * Mutator Method for xSch1531Line13a
	 * @param Sch1531Line13a the new value for 'Sch1531Line13a'
	 */
    public void set_Sch1531Line13a(String _Sch1531Line13a) {
        this._Sch1531Line13a = _Sch1531Line13a;
    }

    /**
	 * Accessor Method for xSch1531Line13a
	 * @return the value of 'Sch1531Line13a'
	 */
    public String get_Sch1531Line13a() {
        return this._Sch1531Line13a;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line18' in the PDF.
	 */
    private String _Sch1531Line18 = "";

    /**
	 * Mutator Method for xSch1531Line18
	 * @param Sch1531Line18 the new value for 'Sch1531Line18'
	 */
    public void set_Sch1531Line18(String _Sch1531Line18) {
        this._Sch1531Line18 = _Sch1531Line18;
    }

    /**
	 * Accessor Method for xSch1531Line18
	 * @return the value of 'Sch1531Line18'
	 */
    public String get_Sch1531Line18() {
        return this._Sch1531Line18;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line1' in the PDF.
	 */
    private String _Sch1531Line1 = "";

    /**
	 * Mutator Method for xSch1531Line1
	 * @param Sch1531Line1 the new value for 'Sch1531Line1'
	 */
    public void set_Sch1531Line1(String _Sch1531Line1) {
        this._Sch1531Line1 = _Sch1531Line1;
    }

    /**
	 * Accessor Method for xSch1531Line1
	 * @return the value of 'Sch1531Line1'
	 */
    public String get_Sch1531Line1() {
        return this._Sch1531Line1;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line5b' in the PDF.
	 */
    private String _Sch1531Line5b = "";

    /**
	 * Mutator Method for xSch1531Line5b
	 * @param Sch1531Line5b the new value for 'Sch1531Line5b'
	 */
    public void set_Sch1531Line5b(String _Sch1531Line5b) {
        this._Sch1531Line5b = _Sch1531Line5b;
    }

    /**
	 * Accessor Method for xSch1531Line5b
	 * @return the value of 'Sch1531Line5b'
	 */
    public String get_Sch1531Line5b() {
        return this._Sch1531Line5b;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line16' in the PDF.
	 */
    private String _Sch1531Line16 = "";

    /**
	 * Mutator Method for xSch1531Line16
	 * @param Sch1531Line16 the new value for 'Sch1531Line16'
	 */
    public void set_Sch1531Line16(String _Sch1531Line16) {
        this._Sch1531Line16 = _Sch1531Line16;
    }

    /**
	 * Accessor Method for xSch1531Line16
	 * @return the value of 'Sch1531Line16'
	 */
    public String get_Sch1531Line16() {
        return this._Sch1531Line16;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line5e' in the PDF.
	 */
    private String _Sch1531Line5e = "";

    /**
	 * Mutator Method for xSch1531Line5e
	 * @param Sch1531Line5e the new value for 'Sch1531Line5e'
	 */
    public void set_Sch1531Line5e(String _Sch1531Line5e) {
        this._Sch1531Line5e = _Sch1531Line5e;
    }

    /**
	 * Accessor Method for xSch1531Line5e
	 * @return the value of 'Sch1531Line5e'
	 */
    public String get_Sch1531Line5e() {
        return this._Sch1531Line5e;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line9' in the PDF.
	 */
    private String _Sch1531Line9 = "";

    /**
	 * Mutator Method for xSch1531Line9
	 * @param Sch1531Line9 the new value for 'Sch1531Line9'
	 */
    public void set_Sch1531Line9(String _Sch1531Line9) {
        this._Sch1531Line9 = _Sch1531Line9;
    }

    /**
	 * Accessor Method for xSch1531Line9
	 * @return the value of 'Sch1531Line9'
	 */
    public String get_Sch1531Line9() {
        return this._Sch1531Line9;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line13c' in the PDF.
	 */
    private String _Sch1531Line13c = "";

    /**
	 * Mutator Method for xSch1531Line13c
	 * @param Sch1531Line13c the new value for 'Sch1531Line13c'
	 */
    public void set_Sch1531Line13c(String _Sch1531Line13c) {
        this._Sch1531Line13c = _Sch1531Line13c;
    }

    /**
	 * Accessor Method for xSch1531Line13c
	 * @return the value of 'Sch1531Line13c'
	 */
    public String get_Sch1531Line13c() {
        return this._Sch1531Line13c;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line11' in the PDF.
	 */
    private String _Sch1531Line11 = "";

    /**
	 * Mutator Method for xSch1531Line11
	 * @param Sch1531Line11 the new value for 'Sch1531Line11'
	 */
    public void set_Sch1531Line11(String _Sch1531Line11) {
        this._Sch1531Line11 = _Sch1531Line11;
    }

    /**
	 * Accessor Method for xSch1531Line11
	 * @return the value of 'Sch1531Line11'
	 */
    public String get_Sch1531Line11() {
        return this._Sch1531Line11;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line6' in the PDF.
	 */
    private String _Sch1531Line6 = "";

    /**
	 * Mutator Method for xSch1531Line6
	 * @param Sch1531Line6 the new value for 'Sch1531Line6'
	 */
    public void set_Sch1531Line6(String _Sch1531Line6) {
        this._Sch1531Line6 = _Sch1531Line6;
    }

    /**
	 * Accessor Method for xSch1531Line6
	 * @return the value of 'Sch1531Line6'
	 */
    public String get_Sch1531Line6() {
        return this._Sch1531Line6;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line15' in the PDF.
	 */
    private String _Sch1531Line15 = "";

    /**
	 * Mutator Method for xSch1531Line15
	 * @param Sch1531Line15 the new value for 'Sch1531Line15'
	 */
    public void set_Sch1531Line15(String _Sch1531Line15) {
        this._Sch1531Line15 = _Sch1531Line15;
    }

    /**
	 * Accessor Method for xSch1531Line15
	 * @return the value of 'Sch1531Line15'
	 */
    public String get_Sch1531Line15() {
        return this._Sch1531Line15;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line8' in the PDF.
	 */
    private String _Sch1531Line8 = "";

    /**
	 * Mutator Method for xSch1531Line8
	 * @param Sch1531Line8 the new value for 'Sch1531Line8'
	 */
    public void set_Sch1531Line8(String _Sch1531Line8) {
        this._Sch1531Line8 = _Sch1531Line8;
    }

    /**
	 * Accessor Method for xSch1531Line8
	 * @return the value of 'Sch1531Line8'
	 */
    public String get_Sch1531Line8() {
        return this._Sch1531Line8;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line5d' in the PDF.
	 */
    private String _Sch1531Line5d = "";

    /**
	 * Mutator Method for xSch1531Line5d
	 * @param Sch1531Line5d the new value for 'Sch1531Line5d'
	 */
    public void set_Sch1531Line5d(String _Sch1531Line5d) {
        this._Sch1531Line5d = _Sch1531Line5d;
    }

    /**
	 * Accessor Method for xSch1531Line5d
	 * @return the value of 'Sch1531Line5d'
	 */
    public String get_Sch1531Line5d() {
        return this._Sch1531Line5d;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line20' in the PDF.
	 */
    private String _Sch1531Line20 = "";

    /**
	 * Mutator Method for xSch1531Line20
	 * @param Sch1531Line20 the new value for 'Sch1531Line20'
	 */
    public void set_Sch1531Line20(String _Sch1531Line20) {
        this._Sch1531Line20 = _Sch1531Line20;
    }

    /**
	 * Accessor Method for xSch1531Line20
	 * @return the value of 'Sch1531Line20'
	 */
    public String get_Sch1531Line20() {
        return this._Sch1531Line20;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line14' in the PDF.
	 */
    private String _Sch1531Line14 = "";

    /**
	 * Mutator Method for xSch1531Line14
	 * @param Sch1531Line14 the new value for 'Sch1531Line14'
	 */
    public void set_Sch1531Line14(String _Sch1531Line14) {
        this._Sch1531Line14 = _Sch1531Line14;
    }

    /**
	 * Accessor Method for xSch1531Line14
	 * @return the value of 'Sch1531Line14'
	 */
    public String get_Sch1531Line14() {
        return this._Sch1531Line14;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line7' in the PDF.
	 */
    private String _Sch1531Line7 = "";

    /**
	 * Mutator Method for xSch1531Line7
	 * @param Sch1531Line7 the new value for 'Sch1531Line7'
	 */
    public void set_Sch1531Line7(String _Sch1531Line7) {
        this._Sch1531Line7 = _Sch1531Line7;
    }

    /**
	 * Accessor Method for xSch1531Line7
	 * @return the value of 'Sch1531Line7'
	 */
    public String get_Sch1531Line7() {
        return this._Sch1531Line7;
    }

    /**
	 * Class member corresponding to the field 'Sch1531Line5c' in the PDF.
	 */
    private String _Sch1531Line5c = "";

    /**
	 * Mutator Method for xSch1531Line5c
	 * @param Sch1531Line5c the new value for 'Sch1531Line5c'
	 */
    public void set_Sch1531Line5c(String _Sch1531Line5c) {
        this._Sch1531Line5c = _Sch1531Line5c;
    }

    /**
	 * Accessor Method for xSch1531Line5c
	 * @return the value of 'Sch1531Line5c'
	 */
    public String get_Sch1531Line5c() {
        return this._Sch1531Line5c;
    }
}
