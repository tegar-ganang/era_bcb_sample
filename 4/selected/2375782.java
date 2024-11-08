package net.sf.gateway.mef.pdf;

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
import java.util.UUID;

/**
 * Generated class for filling 'TY2009_VT_IN153.pdf'
 */
public class TY2009_VT_IN153 {

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
        stamper.setEncryption(true, "", UUID.randomUUID().toString(), 0);
        AcroFields fields = stamper.getAcroFields();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD);
        font.setSize((float) 20.2);
        BaseFont baseFont = font.getBaseFont();
        fields.setFieldProperty("Text478", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text478", "textfont", baseFont, null);
        fields.setField("Text478", this.get_Text478());
        fields.setFieldProperty("Sch153Line18", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line18", "textfont", baseFont, null);
        fields.setField("Sch153Line18", this.get_Sch153Line18());
        fields.setFieldProperty("Sch153Line19", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line19", "textfont", baseFont, null);
        fields.setField("Sch153Line19", this.get_Sch153Line19());
        fields.setFieldProperty("Sch153Line16", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line16", "textfont", baseFont, null);
        fields.setField("Sch153Line16", this.get_Sch153Line16());
        fields.setFieldProperty("Sch153Line17", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line17", "textfont", baseFont, null);
        fields.setField("Sch153Line17", this.get_Sch153Line17());
        fields.setFieldProperty("Sch153Line11", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line11", "textfont", baseFont, null);
        fields.setField("Sch153Line11", this.get_Sch153Line11());
        fields.setFieldProperty("Sch153Line10", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line10", "textfont", baseFont, null);
        fields.setField("Sch153Line10", this.get_Sch153Line10());
        fields.setFieldProperty("Sch153Line15", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line15", "textfont", baseFont, null);
        fields.setField("Sch153Line15", this.get_Sch153Line15());
        fields.setFieldProperty("Sch153Line51", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line51", "textfont", baseFont, null);
        fields.setField("Sch153Line51", this.get_Sch153Line51());
        fields.setFieldProperty("Sch153Line14", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line14", "textfont", baseFont, null);
        fields.setField("Sch153Line14", this.get_Sch153Line14());
        fields.setFieldProperty("Sch153Line50", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line50", "textfont", baseFont, null);
        fields.setField("Sch153Line50", this.get_Sch153Line50());
        fields.setFieldProperty("Sch153Line13", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line13", "textfont", baseFont, null);
        fields.setField("Sch153Line13", this.get_Sch153Line13());
        fields.setFieldProperty("Sch153Line12", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line12", "textfont", baseFont, null);
        fields.setField("Sch153Line12", this.get_Sch153Line12());
        fields.setFieldProperty("Sch153Line55", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line55", "textfont", baseFont, null);
        fields.setField("Sch153Line55", this.get_Sch153Line55());
        fields.setFieldProperty("Sch153Line54", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line54", "textfont", baseFont, null);
        fields.setField("Sch153Line54", this.get_Sch153Line54());
        fields.setFieldProperty("Sch153Line53", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line53", "textfont", baseFont, null);
        fields.setField("Sch153Line53", this.get_Sch153Line53());
        fields.setFieldProperty("Sch153Line52", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line52", "textfont", baseFont, null);
        fields.setField("Sch153Line52", this.get_Sch153Line52());
        fields.setFieldProperty("Sch153Line59", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line59", "textfont", baseFont, null);
        fields.setField("Sch153Line59", this.get_Sch153Line59());
        fields.setFieldProperty("Sch153Line58", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line58", "textfont", baseFont, null);
        fields.setField("Sch153Line58", this.get_Sch153Line58());
        fields.setFieldProperty("Sch153Line57", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line57", "textfont", baseFont, null);
        fields.setField("Sch153Line57", this.get_Sch153Line57());
        fields.setFieldProperty("Sch153Line56", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line56", "textfont", baseFont, null);
        fields.setField("Sch153Line56", this.get_Sch153Line56());
        fields.setFieldProperty("Text468", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text468", "textfont", baseFont, null);
        fields.setField("Text468", this.get_Text468());
        fields.setFieldProperty("Sch153Line27", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line27", "textfont", baseFont, null);
        fields.setField("Sch153Line27", this.get_Sch153Line27());
        fields.setFieldProperty("Sch153Line28", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line28", "textfont", baseFont, null);
        fields.setField("Sch153Line28", this.get_Sch153Line28());
        fields.setFieldProperty("Sch153Line20", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line20", "textfont", baseFont, null);
        fields.setField("Sch153Line20", this.get_Sch153Line20());
        fields.setFieldProperty("Sch153Line22", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line22", "textfont", baseFont, null);
        fields.setField("Sch153Line22", this.get_Sch153Line22());
        fields.setFieldProperty("Sch153Line21", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line21", "textfont", baseFont, null);
        fields.setField("Sch153Line21", this.get_Sch153Line21());
        fields.setFieldProperty("Sch153Line60", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line60", "textfont", baseFont, null);
        fields.setField("Sch153Line60", this.get_Sch153Line60());
        fields.setFieldProperty("Sch153Line24", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line24", "textfont", baseFont, null);
        fields.setField("Sch153Line24", this.get_Sch153Line24());
        fields.setFieldProperty("Sch153Line23", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line23", "textfont", baseFont, null);
        fields.setField("Sch153Line23", this.get_Sch153Line23());
        fields.setFieldProperty("Sch153Line62", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line62", "textfont", baseFont, null);
        fields.setField("Sch153Line62", this.get_Sch153Line62());
        fields.setFieldProperty("Sch153Line26", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line26", "textfont", baseFont, null);
        fields.setField("Sch153Line26", this.get_Sch153Line26());
        fields.setFieldProperty("Sch153Line25", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line25", "textfont", baseFont, null);
        fields.setField("Sch153Line25", this.get_Sch153Line25());
        fields.setFieldProperty("Sch153Line64", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line64", "textfont", baseFont, null);
        fields.setField("Sch153Line64", this.get_Sch153Line64());
        fields.setFieldProperty("Sch153Line63", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line63", "textfont", baseFont, null);
        fields.setField("Sch153Line63", this.get_Sch153Line63());
        fields.setFieldProperty("Sch153Line66", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line66", "textfont", baseFont, null);
        fields.setField("Sch153Line66", this.get_Sch153Line66());
        fields.setFieldProperty("Sch153Line65", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line65", "textfont", baseFont, null);
        fields.setField("Sch153Line65", this.get_Sch153Line65());
        fields.setFieldProperty("Sch153Line68", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line68", "textfont", baseFont, null);
        fields.setField("Sch153Line68", this.get_Sch153Line68());
        fields.setFieldProperty("Sch153Line67", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line67", "textfont", baseFont, null);
        fields.setField("Sch153Line67", this.get_Sch153Line67());
        fields.setFieldProperty("Sch153Line69", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line69", "textfont", baseFont, null);
        fields.setField("Sch153Line69", this.get_Sch153Line69());
        fields.setFieldProperty("Text458", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text458", "textfont", baseFont, null);
        fields.setField("Text458", this.get_Text458());
        fields.setFieldProperty("Sch153Line38", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line38", "textfont", baseFont, null);
        fields.setField("Sch153Line38", this.get_Sch153Line38());
        fields.setFieldProperty("Sch153Line39", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line39", "textfont", baseFont, null);
        fields.setField("Sch153Line39", this.get_Sch153Line39());
        fields.setFieldProperty("Sch153Line73", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line73", "textfont", baseFont, null);
        fields.setField("Sch153Line73", this.get_Sch153Line73());
        fields.setFieldProperty("Sch153Line8", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line8", "textfont", baseFont, null);
        fields.setField("Sch153Line8", this.get_Sch153Line8());
        fields.setFieldProperty("Sch153Line37", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line37", "textfont", baseFont, null);
        fields.setField("Sch153Line37", this.get_Sch153Line37());
        fields.setFieldProperty("Sch153Line72", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line72", "textfont", baseFont, null);
        fields.setField("Sch153Line72", this.get_Sch153Line72());
        fields.setFieldProperty("Sch153Line7", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line7", "textfont", baseFont, null);
        fields.setField("Sch153Line7", this.get_Sch153Line7());
        fields.setFieldProperty("Sch153Line36", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line36", "textfont", baseFont, null);
        fields.setField("Sch153Line36", this.get_Sch153Line36());
        fields.setFieldProperty("Sch153Line71", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line71", "textfont", baseFont, null);
        fields.setField("Sch153Line71", this.get_Sch153Line71());
        fields.setFieldProperty("Sch153Line35", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line35", "textfont", baseFont, null);
        fields.setField("Sch153Line35", this.get_Sch153Line35());
        fields.setFieldProperty("Sch153Line70", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line70", "textfont", baseFont, null);
        fields.setField("Sch153Line70", this.get_Sch153Line70());
        fields.setFieldProperty("Sch153Line9", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line9", "textfont", baseFont, null);
        fields.setField("Sch153Line9", this.get_Sch153Line9());
        fields.setFieldProperty("Sch153Line34", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line34", "textfont", baseFont, null);
        fields.setField("Sch153Line34", this.get_Sch153Line34());
        fields.setFieldProperty("Sch153Line33", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line33", "textfont", baseFont, null);
        fields.setField("Sch153Line33", this.get_Sch153Line33());
        fields.setFieldProperty("Sch153Line32", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line32", "textfont", baseFont, null);
        fields.setField("Sch153Line32", this.get_Sch153Line32());
        fields.setFieldProperty("Sch153Line6", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line6", "textfont", baseFont, null);
        fields.setField("Sch153Line6", this.get_Sch153Line6());
        fields.setFieldProperty("Sch153Line1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line1", "textfont", baseFont, null);
        fields.setField("Sch153Line1", this.get_Sch153Line1());
        fields.setFieldProperty("Sch153Line74", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line74", "textfont", baseFont, null);
        fields.setField("Sch153Line74", this.get_Sch153Line74());
        fields.setFieldProperty("Sch153Line49", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line49", "textfont", baseFont, null);
        fields.setField("Sch153Line49", this.get_Sch153Line49());
        fields.setFieldProperty("Age70Question", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Age70Question", "textfont", baseFont, null);
        fields.setField("Age70Question", this.get_Age70Question());
        fields.setFieldProperty("Sch153Line46", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line46", "textfont", baseFont, null);
        fields.setField("Sch153Line46", this.get_Sch153Line46());
        fields.setFieldProperty("Sch153Line45", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line45", "textfont", baseFont, null);
        fields.setField("Sch153Line45", this.get_Sch153Line45());
        fields.setFieldProperty("Sch153Line48", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line48", "textfont", baseFont, null);
        fields.setField("Sch153Line48", this.get_Sch153Line48());
        fields.setFieldProperty("Sch153Line47", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line47", "textfont", baseFont, null);
        fields.setField("Sch153Line47", this.get_Sch153Line47());
        fields.setFieldProperty("Sch153Line42", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line42", "textfont", baseFont, null);
        fields.setField("Sch153Line42", this.get_Sch153Line42());
        fields.setFieldProperty("Sch153Line41", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line41", "textfont", baseFont, null);
        fields.setField("Sch153Line41", this.get_Sch153Line41());
        fields.setFieldProperty("Sch153Line44", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line44", "textfont", baseFont, null);
        fields.setField("Sch153Line44", this.get_Sch153Line44());
        fields.setFieldProperty("Sch153Line43", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line43", "textfont", baseFont, null);
        fields.setField("Sch153Line43", this.get_Sch153Line43());
        fields.setFieldProperty("Sch153Line40", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch153Line40", "textfont", baseFont, null);
        fields.setField("Sch153Line40", this.get_Sch153Line40());
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
	 * Class member corresponding to the field 'Text478' in the PDF.
	 */
    private String _Text478 = "";

    /**
	 * Mutator Method for xText478
	 * @param Text478 the new value for 'Text478'
	 */
    public void set_Text478(String _Text478) {
        this._Text478 = _Text478;
    }

    /**
	 * Accessor Method for xText478
	 * @return the value of 'Text478'
	 */
    public String get_Text478() {
        return this._Text478;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line18' in the PDF.
	 */
    private String _Sch153Line18 = "";

    /**
	 * Mutator Method for xSch153Line18
	 * @param Sch153Line18 the new value for 'Sch153Line18'
	 */
    public void set_Sch153Line18(String _Sch153Line18) {
        this._Sch153Line18 = _Sch153Line18;
    }

    /**
	 * Accessor Method for xSch153Line18
	 * @return the value of 'Sch153Line18'
	 */
    public String get_Sch153Line18() {
        return this._Sch153Line18;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line19' in the PDF.
	 */
    private String _Sch153Line19 = "";

    /**
	 * Mutator Method for xSch153Line19
	 * @param Sch153Line19 the new value for 'Sch153Line19'
	 */
    public void set_Sch153Line19(String _Sch153Line19) {
        this._Sch153Line19 = _Sch153Line19;
    }

    /**
	 * Accessor Method for xSch153Line19
	 * @return the value of 'Sch153Line19'
	 */
    public String get_Sch153Line19() {
        return this._Sch153Line19;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line16' in the PDF.
	 */
    private String _Sch153Line16 = "";

    /**
	 * Mutator Method for xSch153Line16
	 * @param Sch153Line16 the new value for 'Sch153Line16'
	 */
    public void set_Sch153Line16(String _Sch153Line16) {
        this._Sch153Line16 = _Sch153Line16;
    }

    /**
	 * Accessor Method for xSch153Line16
	 * @return the value of 'Sch153Line16'
	 */
    public String get_Sch153Line16() {
        return this._Sch153Line16;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line17' in the PDF.
	 */
    private String _Sch153Line17 = "";

    /**
	 * Mutator Method for xSch153Line17
	 * @param Sch153Line17 the new value for 'Sch153Line17'
	 */
    public void set_Sch153Line17(String _Sch153Line17) {
        this._Sch153Line17 = _Sch153Line17;
    }

    /**
	 * Accessor Method for xSch153Line17
	 * @return the value of 'Sch153Line17'
	 */
    public String get_Sch153Line17() {
        return this._Sch153Line17;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line11' in the PDF.
	 */
    private String _Sch153Line11 = "";

    /**
	 * Mutator Method for xSch153Line11
	 * @param Sch153Line11 the new value for 'Sch153Line11'
	 */
    public void set_Sch153Line11(String _Sch153Line11) {
        this._Sch153Line11 = _Sch153Line11;
    }

    /**
	 * Accessor Method for xSch153Line11
	 * @return the value of 'Sch153Line11'
	 */
    public String get_Sch153Line11() {
        return this._Sch153Line11;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line10' in the PDF.
	 */
    private String _Sch153Line10 = "";

    /**
	 * Mutator Method for xSch153Line10
	 * @param Sch153Line10 the new value for 'Sch153Line10'
	 */
    public void set_Sch153Line10(String _Sch153Line10) {
        this._Sch153Line10 = _Sch153Line10;
    }

    /**
	 * Accessor Method for xSch153Line10
	 * @return the value of 'Sch153Line10'
	 */
    public String get_Sch153Line10() {
        return this._Sch153Line10;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line15' in the PDF.
	 */
    private String _Sch153Line15 = "";

    /**
	 * Mutator Method for xSch153Line15
	 * @param Sch153Line15 the new value for 'Sch153Line15'
	 */
    public void set_Sch153Line15(String _Sch153Line15) {
        this._Sch153Line15 = _Sch153Line15;
    }

    /**
	 * Accessor Method for xSch153Line15
	 * @return the value of 'Sch153Line15'
	 */
    public String get_Sch153Line15() {
        return this._Sch153Line15;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line51' in the PDF.
	 */
    private String _Sch153Line51 = "";

    /**
	 * Mutator Method for xSch153Line51
	 * @param Sch153Line51 the new value for 'Sch153Line51'
	 */
    public void set_Sch153Line51(String _Sch153Line51) {
        this._Sch153Line51 = _Sch153Line51;
    }

    /**
	 * Accessor Method for xSch153Line51
	 * @return the value of 'Sch153Line51'
	 */
    public String get_Sch153Line51() {
        return this._Sch153Line51;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line14' in the PDF.
	 */
    private String _Sch153Line14 = "";

    /**
	 * Mutator Method for xSch153Line14
	 * @param Sch153Line14 the new value for 'Sch153Line14'
	 */
    public void set_Sch153Line14(String _Sch153Line14) {
        this._Sch153Line14 = _Sch153Line14;
    }

    /**
	 * Accessor Method for xSch153Line14
	 * @return the value of 'Sch153Line14'
	 */
    public String get_Sch153Line14() {
        return this._Sch153Line14;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line50' in the PDF.
	 */
    private String _Sch153Line50 = "";

    /**
	 * Mutator Method for xSch153Line50
	 * @param Sch153Line50 the new value for 'Sch153Line50'
	 */
    public void set_Sch153Line50(String _Sch153Line50) {
        this._Sch153Line50 = _Sch153Line50;
    }

    /**
	 * Accessor Method for xSch153Line50
	 * @return the value of 'Sch153Line50'
	 */
    public String get_Sch153Line50() {
        return this._Sch153Line50;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line13' in the PDF.
	 */
    private String _Sch153Line13 = "";

    /**
	 * Mutator Method for xSch153Line13
	 * @param Sch153Line13 the new value for 'Sch153Line13'
	 */
    public void set_Sch153Line13(String _Sch153Line13) {
        this._Sch153Line13 = _Sch153Line13;
    }

    /**
	 * Accessor Method for xSch153Line13
	 * @return the value of 'Sch153Line13'
	 */
    public String get_Sch153Line13() {
        return this._Sch153Line13;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line12' in the PDF.
	 */
    private String _Sch153Line12 = "";

    /**
	 * Mutator Method for xSch153Line12
	 * @param Sch153Line12 the new value for 'Sch153Line12'
	 */
    public void set_Sch153Line12(String _Sch153Line12) {
        this._Sch153Line12 = _Sch153Line12;
    }

    /**
	 * Accessor Method for xSch153Line12
	 * @return the value of 'Sch153Line12'
	 */
    public String get_Sch153Line12() {
        return this._Sch153Line12;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line55' in the PDF.
	 */
    private String _Sch153Line55 = "";

    /**
	 * Mutator Method for xSch153Line55
	 * @param Sch153Line55 the new value for 'Sch153Line55'
	 */
    public void set_Sch153Line55(String _Sch153Line55) {
        this._Sch153Line55 = _Sch153Line55;
    }

    /**
	 * Accessor Method for xSch153Line55
	 * @return the value of 'Sch153Line55'
	 */
    public String get_Sch153Line55() {
        return this._Sch153Line55;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line54' in the PDF.
	 */
    private String _Sch153Line54 = "";

    /**
	 * Mutator Method for xSch153Line54
	 * @param Sch153Line54 the new value for 'Sch153Line54'
	 */
    public void set_Sch153Line54(String _Sch153Line54) {
        this._Sch153Line54 = _Sch153Line54;
    }

    /**
	 * Accessor Method for xSch153Line54
	 * @return the value of 'Sch153Line54'
	 */
    public String get_Sch153Line54() {
        return this._Sch153Line54;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line53' in the PDF.
	 */
    private String _Sch153Line53 = "";

    /**
	 * Mutator Method for xSch153Line53
	 * @param Sch153Line53 the new value for 'Sch153Line53'
	 */
    public void set_Sch153Line53(String _Sch153Line53) {
        this._Sch153Line53 = _Sch153Line53;
    }

    /**
	 * Accessor Method for xSch153Line53
	 * @return the value of 'Sch153Line53'
	 */
    public String get_Sch153Line53() {
        return this._Sch153Line53;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line52' in the PDF.
	 */
    private String _Sch153Line52 = "";

    /**
	 * Mutator Method for xSch153Line52
	 * @param Sch153Line52 the new value for 'Sch153Line52'
	 */
    public void set_Sch153Line52(String _Sch153Line52) {
        this._Sch153Line52 = _Sch153Line52;
    }

    /**
	 * Accessor Method for xSch153Line52
	 * @return the value of 'Sch153Line52'
	 */
    public String get_Sch153Line52() {
        return this._Sch153Line52;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line59' in the PDF.
	 */
    private String _Sch153Line59 = "";

    /**
	 * Mutator Method for xSch153Line59
	 * @param Sch153Line59 the new value for 'Sch153Line59'
	 */
    public void set_Sch153Line59(String _Sch153Line59) {
        this._Sch153Line59 = _Sch153Line59;
    }

    /**
	 * Accessor Method for xSch153Line59
	 * @return the value of 'Sch153Line59'
	 */
    public String get_Sch153Line59() {
        return this._Sch153Line59;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line58' in the PDF.
	 */
    private String _Sch153Line58 = "";

    /**
	 * Mutator Method for xSch153Line58
	 * @param Sch153Line58 the new value for 'Sch153Line58'
	 */
    public void set_Sch153Line58(String _Sch153Line58) {
        this._Sch153Line58 = _Sch153Line58;
    }

    /**
	 * Accessor Method for xSch153Line58
	 * @return the value of 'Sch153Line58'
	 */
    public String get_Sch153Line58() {
        return this._Sch153Line58;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line57' in the PDF.
	 */
    private String _Sch153Line57 = "";

    /**
	 * Mutator Method for xSch153Line57
	 * @param Sch153Line57 the new value for 'Sch153Line57'
	 */
    public void set_Sch153Line57(String _Sch153Line57) {
        this._Sch153Line57 = _Sch153Line57;
    }

    /**
	 * Accessor Method for xSch153Line57
	 * @return the value of 'Sch153Line57'
	 */
    public String get_Sch153Line57() {
        return this._Sch153Line57;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line56' in the PDF.
	 */
    private String _Sch153Line56 = "";

    /**
	 * Mutator Method for xSch153Line56
	 * @param Sch153Line56 the new value for 'Sch153Line56'
	 */
    public void set_Sch153Line56(String _Sch153Line56) {
        this._Sch153Line56 = _Sch153Line56;
    }

    /**
	 * Accessor Method for xSch153Line56
	 * @return the value of 'Sch153Line56'
	 */
    public String get_Sch153Line56() {
        return this._Sch153Line56;
    }

    /**
	 * Class member corresponding to the field 'Text468' in the PDF.
	 */
    private String _Text468 = "";

    /**
	 * Mutator Method for xText468
	 * @param Text468 the new value for 'Text468'
	 */
    public void set_Text468(String _Text468) {
        this._Text468 = _Text468;
    }

    /**
	 * Accessor Method for xText468
	 * @return the value of 'Text468'
	 */
    public String get_Text468() {
        return this._Text468;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line27' in the PDF.
	 */
    private String _Sch153Line27 = "";

    /**
	 * Mutator Method for xSch153Line27
	 * @param Sch153Line27 the new value for 'Sch153Line27'
	 */
    public void set_Sch153Line27(String _Sch153Line27) {
        this._Sch153Line27 = _Sch153Line27;
    }

    /**
	 * Accessor Method for xSch153Line27
	 * @return the value of 'Sch153Line27'
	 */
    public String get_Sch153Line27() {
        return this._Sch153Line27;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line28' in the PDF.
	 */
    private String _Sch153Line28 = "";

    /**
	 * Mutator Method for xSch153Line28
	 * @param Sch153Line28 the new value for 'Sch153Line28'
	 */
    public void set_Sch153Line28(String _Sch153Line28) {
        this._Sch153Line28 = _Sch153Line28;
    }

    /**
	 * Accessor Method for xSch153Line28
	 * @return the value of 'Sch153Line28'
	 */
    public String get_Sch153Line28() {
        return this._Sch153Line28;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line20' in the PDF.
	 */
    private String _Sch153Line20 = "";

    /**
	 * Mutator Method for xSch153Line20
	 * @param Sch153Line20 the new value for 'Sch153Line20'
	 */
    public void set_Sch153Line20(String _Sch153Line20) {
        this._Sch153Line20 = _Sch153Line20;
    }

    /**
	 * Accessor Method for xSch153Line20
	 * @return the value of 'Sch153Line20'
	 */
    public String get_Sch153Line20() {
        return this._Sch153Line20;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line22' in the PDF.
	 */
    private String _Sch153Line22 = "";

    /**
	 * Mutator Method for xSch153Line22
	 * @param Sch153Line22 the new value for 'Sch153Line22'
	 */
    public void set_Sch153Line22(String _Sch153Line22) {
        this._Sch153Line22 = _Sch153Line22;
    }

    /**
	 * Accessor Method for xSch153Line22
	 * @return the value of 'Sch153Line22'
	 */
    public String get_Sch153Line22() {
        return this._Sch153Line22;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line21' in the PDF.
	 */
    private String _Sch153Line21 = "";

    /**
	 * Mutator Method for xSch153Line21
	 * @param Sch153Line21 the new value for 'Sch153Line21'
	 */
    public void set_Sch153Line21(String _Sch153Line21) {
        this._Sch153Line21 = _Sch153Line21;
    }

    /**
	 * Accessor Method for xSch153Line21
	 * @return the value of 'Sch153Line21'
	 */
    public String get_Sch153Line21() {
        return this._Sch153Line21;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line60' in the PDF.
	 */
    private String _Sch153Line60 = "";

    /**
	 * Mutator Method for xSch153Line60
	 * @param Sch153Line60 the new value for 'Sch153Line60'
	 */
    public void set_Sch153Line60(String _Sch153Line60) {
        this._Sch153Line60 = _Sch153Line60;
    }

    /**
	 * Accessor Method for xSch153Line60
	 * @return the value of 'Sch153Line60'
	 */
    public String get_Sch153Line60() {
        return this._Sch153Line60;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line24' in the PDF.
	 */
    private String _Sch153Line24 = "";

    /**
	 * Mutator Method for xSch153Line24
	 * @param Sch153Line24 the new value for 'Sch153Line24'
	 */
    public void set_Sch153Line24(String _Sch153Line24) {
        this._Sch153Line24 = _Sch153Line24;
    }

    /**
	 * Accessor Method for xSch153Line24
	 * @return the value of 'Sch153Line24'
	 */
    public String get_Sch153Line24() {
        return this._Sch153Line24;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line23' in the PDF.
	 */
    private String _Sch153Line23 = "";

    /**
	 * Mutator Method for xSch153Line23
	 * @param Sch153Line23 the new value for 'Sch153Line23'
	 */
    public void set_Sch153Line23(String _Sch153Line23) {
        this._Sch153Line23 = _Sch153Line23;
    }

    /**
	 * Accessor Method for xSch153Line23
	 * @return the value of 'Sch153Line23'
	 */
    public String get_Sch153Line23() {
        return this._Sch153Line23;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line62' in the PDF.
	 */
    private String _Sch153Line62 = "";

    /**
	 * Mutator Method for xSch153Line62
	 * @param Sch153Line62 the new value for 'Sch153Line62'
	 */
    public void set_Sch153Line62(String _Sch153Line62) {
        this._Sch153Line62 = _Sch153Line62;
    }

    /**
	 * Accessor Method for xSch153Line62
	 * @return the value of 'Sch153Line62'
	 */
    public String get_Sch153Line62() {
        return this._Sch153Line62;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line26' in the PDF.
	 */
    private String _Sch153Line26 = "";

    /**
	 * Mutator Method for xSch153Line26
	 * @param Sch153Line26 the new value for 'Sch153Line26'
	 */
    public void set_Sch153Line26(String _Sch153Line26) {
        this._Sch153Line26 = _Sch153Line26;
    }

    /**
	 * Accessor Method for xSch153Line26
	 * @return the value of 'Sch153Line26'
	 */
    public String get_Sch153Line26() {
        return this._Sch153Line26;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line25' in the PDF.
	 */
    private String _Sch153Line25 = "";

    /**
	 * Mutator Method for xSch153Line25
	 * @param Sch153Line25 the new value for 'Sch153Line25'
	 */
    public void set_Sch153Line25(String _Sch153Line25) {
        this._Sch153Line25 = _Sch153Line25;
    }

    /**
	 * Accessor Method for xSch153Line25
	 * @return the value of 'Sch153Line25'
	 */
    public String get_Sch153Line25() {
        return this._Sch153Line25;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line64' in the PDF.
	 */
    private String _Sch153Line64 = "";

    /**
	 * Mutator Method for xSch153Line64
	 * @param Sch153Line64 the new value for 'Sch153Line64'
	 */
    public void set_Sch153Line64(String _Sch153Line64) {
        this._Sch153Line64 = _Sch153Line64;
    }

    /**
	 * Accessor Method for xSch153Line64
	 * @return the value of 'Sch153Line64'
	 */
    public String get_Sch153Line64() {
        return this._Sch153Line64;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line63' in the PDF.
	 */
    private String _Sch153Line63 = "";

    /**
	 * Mutator Method for xSch153Line63
	 * @param Sch153Line63 the new value for 'Sch153Line63'
	 */
    public void set_Sch153Line63(String _Sch153Line63) {
        this._Sch153Line63 = _Sch153Line63;
    }

    /**
	 * Accessor Method for xSch153Line63
	 * @return the value of 'Sch153Line63'
	 */
    public String get_Sch153Line63() {
        return this._Sch153Line63;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line66' in the PDF.
	 */
    private String _Sch153Line66 = "";

    /**
	 * Mutator Method for xSch153Line66
	 * @param Sch153Line66 the new value for 'Sch153Line66'
	 */
    public void set_Sch153Line66(String _Sch153Line66) {
        this._Sch153Line66 = _Sch153Line66;
    }

    /**
	 * Accessor Method for xSch153Line66
	 * @return the value of 'Sch153Line66'
	 */
    public String get_Sch153Line66() {
        return this._Sch153Line66;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line65' in the PDF.
	 */
    private String _Sch153Line65 = "";

    /**
	 * Mutator Method for xSch153Line65
	 * @param Sch153Line65 the new value for 'Sch153Line65'
	 */
    public void set_Sch153Line65(String _Sch153Line65) {
        this._Sch153Line65 = _Sch153Line65;
    }

    /**
	 * Accessor Method for xSch153Line65
	 * @return the value of 'Sch153Line65'
	 */
    public String get_Sch153Line65() {
        return this._Sch153Line65;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line68' in the PDF.
	 */
    private String _Sch153Line68 = "";

    /**
	 * Mutator Method for xSch153Line68
	 * @param Sch153Line68 the new value for 'Sch153Line68'
	 */
    public void set_Sch153Line68(String _Sch153Line68) {
        this._Sch153Line68 = _Sch153Line68;
    }

    /**
	 * Accessor Method for xSch153Line68
	 * @return the value of 'Sch153Line68'
	 */
    public String get_Sch153Line68() {
        return this._Sch153Line68;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line67' in the PDF.
	 */
    private String _Sch153Line67 = "";

    /**
	 * Mutator Method for xSch153Line67
	 * @param Sch153Line67 the new value for 'Sch153Line67'
	 */
    public void set_Sch153Line67(String _Sch153Line67) {
        this._Sch153Line67 = _Sch153Line67;
    }

    /**
	 * Accessor Method for xSch153Line67
	 * @return the value of 'Sch153Line67'
	 */
    public String get_Sch153Line67() {
        return this._Sch153Line67;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line69' in the PDF.
	 */
    private String _Sch153Line69 = "";

    /**
	 * Mutator Method for xSch153Line69
	 * @param Sch153Line69 the new value for 'Sch153Line69'
	 */
    public void set_Sch153Line69(String _Sch153Line69) {
        this._Sch153Line69 = _Sch153Line69;
    }

    /**
	 * Accessor Method for xSch153Line69
	 * @return the value of 'Sch153Line69'
	 */
    public String get_Sch153Line69() {
        return this._Sch153Line69;
    }

    /**
	 * Class member corresponding to the field 'Text458' in the PDF.
	 */
    private String _Text458 = "";

    /**
	 * Mutator Method for xText458
	 * @param Text458 the new value for 'Text458'
	 */
    public void set_Text458(String _Text458) {
        this._Text458 = _Text458;
    }

    /**
	 * Accessor Method for xText458
	 * @return the value of 'Text458'
	 */
    public String get_Text458() {
        return this._Text458;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line38' in the PDF.
	 */
    private String _Sch153Line38 = "";

    /**
	 * Mutator Method for xSch153Line38
	 * @param Sch153Line38 the new value for 'Sch153Line38'
	 */
    public void set_Sch153Line38(String _Sch153Line38) {
        this._Sch153Line38 = _Sch153Line38;
    }

    /**
	 * Accessor Method for xSch153Line38
	 * @return the value of 'Sch153Line38'
	 */
    public String get_Sch153Line38() {
        return this._Sch153Line38;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line39' in the PDF.
	 */
    private String _Sch153Line39 = "";

    /**
	 * Mutator Method for xSch153Line39
	 * @param Sch153Line39 the new value for 'Sch153Line39'
	 */
    public void set_Sch153Line39(String _Sch153Line39) {
        this._Sch153Line39 = _Sch153Line39;
    }

    /**
	 * Accessor Method for xSch153Line39
	 * @return the value of 'Sch153Line39'
	 */
    public String get_Sch153Line39() {
        return this._Sch153Line39;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line73' in the PDF.
	 */
    private String _Sch153Line73 = "";

    /**
	 * Mutator Method for xSch153Line73
	 * @param Sch153Line73 the new value for 'Sch153Line73'
	 */
    public void set_Sch153Line73(String _Sch153Line73) {
        this._Sch153Line73 = _Sch153Line73;
    }

    /**
	 * Accessor Method for xSch153Line73
	 * @return the value of 'Sch153Line73'
	 */
    public String get_Sch153Line73() {
        return this._Sch153Line73;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line8' in the PDF.
	 */
    private String _Sch153Line8 = "";

    /**
	 * Mutator Method for xSch153Line8
	 * @param Sch153Line8 the new value for 'Sch153Line8'
	 */
    public void set_Sch153Line8(String _Sch153Line8) {
        this._Sch153Line8 = _Sch153Line8;
    }

    /**
	 * Accessor Method for xSch153Line8
	 * @return the value of 'Sch153Line8'
	 */
    public String get_Sch153Line8() {
        return this._Sch153Line8;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line37' in the PDF.
	 */
    private String _Sch153Line37 = "";

    /**
	 * Mutator Method for xSch153Line37
	 * @param Sch153Line37 the new value for 'Sch153Line37'
	 */
    public void set_Sch153Line37(String _Sch153Line37) {
        this._Sch153Line37 = _Sch153Line37;
    }

    /**
	 * Accessor Method for xSch153Line37
	 * @return the value of 'Sch153Line37'
	 */
    public String get_Sch153Line37() {
        return this._Sch153Line37;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line72' in the PDF.
	 */
    private String _Sch153Line72 = "";

    /**
	 * Mutator Method for xSch153Line72
	 * @param Sch153Line72 the new value for 'Sch153Line72'
	 */
    public void set_Sch153Line72(String _Sch153Line72) {
        this._Sch153Line72 = _Sch153Line72;
    }

    /**
	 * Accessor Method for xSch153Line72
	 * @return the value of 'Sch153Line72'
	 */
    public String get_Sch153Line72() {
        return this._Sch153Line72;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line7' in the PDF.
	 */
    private String _Sch153Line7 = "";

    /**
	 * Mutator Method for xSch153Line7
	 * @param Sch153Line7 the new value for 'Sch153Line7'
	 */
    public void set_Sch153Line7(String _Sch153Line7) {
        this._Sch153Line7 = _Sch153Line7;
    }

    /**
	 * Accessor Method for xSch153Line7
	 * @return the value of 'Sch153Line7'
	 */
    public String get_Sch153Line7() {
        return this._Sch153Line7;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line36' in the PDF.
	 */
    private String _Sch153Line36 = "";

    /**
	 * Mutator Method for xSch153Line36
	 * @param Sch153Line36 the new value for 'Sch153Line36'
	 */
    public void set_Sch153Line36(String _Sch153Line36) {
        this._Sch153Line36 = _Sch153Line36;
    }

    /**
	 * Accessor Method for xSch153Line36
	 * @return the value of 'Sch153Line36'
	 */
    public String get_Sch153Line36() {
        return this._Sch153Line36;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line71' in the PDF.
	 */
    private String _Sch153Line71 = "";

    /**
	 * Mutator Method for xSch153Line71
	 * @param Sch153Line71 the new value for 'Sch153Line71'
	 */
    public void set_Sch153Line71(String _Sch153Line71) {
        this._Sch153Line71 = _Sch153Line71;
    }

    /**
	 * Accessor Method for xSch153Line71
	 * @return the value of 'Sch153Line71'
	 */
    public String get_Sch153Line71() {
        return this._Sch153Line71;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line35' in the PDF.
	 */
    private String _Sch153Line35 = "";

    /**
	 * Mutator Method for xSch153Line35
	 * @param Sch153Line35 the new value for 'Sch153Line35'
	 */
    public void set_Sch153Line35(String _Sch153Line35) {
        this._Sch153Line35 = _Sch153Line35;
    }

    /**
	 * Accessor Method for xSch153Line35
	 * @return the value of 'Sch153Line35'
	 */
    public String get_Sch153Line35() {
        return this._Sch153Line35;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line70' in the PDF.
	 */
    private String _Sch153Line70 = "";

    /**
	 * Mutator Method for xSch153Line70
	 * @param Sch153Line70 the new value for 'Sch153Line70'
	 */
    public void set_Sch153Line70(String _Sch153Line70) {
        this._Sch153Line70 = _Sch153Line70;
    }

    /**
	 * Accessor Method for xSch153Line70
	 * @return the value of 'Sch153Line70'
	 */
    public String get_Sch153Line70() {
        return this._Sch153Line70;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line9' in the PDF.
	 */
    private String _Sch153Line9 = "";

    /**
	 * Mutator Method for xSch153Line9
	 * @param Sch153Line9 the new value for 'Sch153Line9'
	 */
    public void set_Sch153Line9(String _Sch153Line9) {
        this._Sch153Line9 = _Sch153Line9;
    }

    /**
	 * Accessor Method for xSch153Line9
	 * @return the value of 'Sch153Line9'
	 */
    public String get_Sch153Line9() {
        return this._Sch153Line9;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line34' in the PDF.
	 */
    private String _Sch153Line34 = "";

    /**
	 * Mutator Method for xSch153Line34
	 * @param Sch153Line34 the new value for 'Sch153Line34'
	 */
    public void set_Sch153Line34(String _Sch153Line34) {
        this._Sch153Line34 = _Sch153Line34;
    }

    /**
	 * Accessor Method for xSch153Line34
	 * @return the value of 'Sch153Line34'
	 */
    public String get_Sch153Line34() {
        return this._Sch153Line34;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line33' in the PDF.
	 */
    private String _Sch153Line33 = "";

    /**
	 * Mutator Method for xSch153Line33
	 * @param Sch153Line33 the new value for 'Sch153Line33'
	 */
    public void set_Sch153Line33(String _Sch153Line33) {
        this._Sch153Line33 = _Sch153Line33;
    }

    /**
	 * Accessor Method for xSch153Line33
	 * @return the value of 'Sch153Line33'
	 */
    public String get_Sch153Line33() {
        return this._Sch153Line33;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line32' in the PDF.
	 */
    private String _Sch153Line32 = "";

    /**
	 * Mutator Method for xSch153Line32
	 * @param Sch153Line32 the new value for 'Sch153Line32'
	 */
    public void set_Sch153Line32(String _Sch153Line32) {
        this._Sch153Line32 = _Sch153Line32;
    }

    /**
	 * Accessor Method for xSch153Line32
	 * @return the value of 'Sch153Line32'
	 */
    public String get_Sch153Line32() {
        return this._Sch153Line32;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line6' in the PDF.
	 */
    private String _Sch153Line6 = "";

    /**
	 * Mutator Method for xSch153Line6
	 * @param Sch153Line6 the new value for 'Sch153Line6'
	 */
    public void set_Sch153Line6(String _Sch153Line6) {
        this._Sch153Line6 = _Sch153Line6;
    }

    /**
	 * Accessor Method for xSch153Line6
	 * @return the value of 'Sch153Line6'
	 */
    public String get_Sch153Line6() {
        return this._Sch153Line6;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line1' in the PDF.
	 */
    private String _Sch153Line1 = "";

    /**
	 * Mutator Method for xSch153Line1
	 * @param Sch153Line1 the new value for 'Sch153Line1'
	 */
    public void set_Sch153Line1(String _Sch153Line1) {
        this._Sch153Line1 = _Sch153Line1;
    }

    /**
	 * Accessor Method for xSch153Line1
	 * @return the value of 'Sch153Line1'
	 */
    public String get_Sch153Line1() {
        return this._Sch153Line1;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line74' in the PDF.
	 */
    private String _Sch153Line74 = "";

    /**
	 * Mutator Method for xSch153Line74
	 * @param Sch153Line74 the new value for 'Sch153Line74'
	 */
    public void set_Sch153Line74(String _Sch153Line74) {
        this._Sch153Line74 = _Sch153Line74;
    }

    /**
	 * Accessor Method for xSch153Line74
	 * @return the value of 'Sch153Line74'
	 */
    public String get_Sch153Line74() {
        return this._Sch153Line74;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line49' in the PDF.
	 */
    private String _Sch153Line49 = "";

    /**
	 * Mutator Method for xSch153Line49
	 * @param Sch153Line49 the new value for 'Sch153Line49'
	 */
    public void set_Sch153Line49(String _Sch153Line49) {
        this._Sch153Line49 = _Sch153Line49;
    }

    /**
	 * Accessor Method for xSch153Line49
	 * @return the value of 'Sch153Line49'
	 */
    public String get_Sch153Line49() {
        return this._Sch153Line49;
    }

    /**
	 * Class member corresponding to the field 'Age70Question' in the PDF.
	 */
    private String _Age70Question = "";

    /**
	 * Mutator Method for xAge70Question
	 * @param Age70Question the new value for 'Age70Question'
	 */
    public void set_Age70Question(String _Age70Question) {
        this._Age70Question = _Age70Question;
    }

    /**
	 * Accessor Method for xAge70Question
	 * @return the value of 'Age70Question'
	 */
    public String get_Age70Question() {
        return this._Age70Question;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line46' in the PDF.
	 */
    private String _Sch153Line46 = "";

    /**
	 * Mutator Method for xSch153Line46
	 * @param Sch153Line46 the new value for 'Sch153Line46'
	 */
    public void set_Sch153Line46(String _Sch153Line46) {
        this._Sch153Line46 = _Sch153Line46;
    }

    /**
	 * Accessor Method for xSch153Line46
	 * @return the value of 'Sch153Line46'
	 */
    public String get_Sch153Line46() {
        return this._Sch153Line46;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line45' in the PDF.
	 */
    private String _Sch153Line45 = "";

    /**
	 * Mutator Method for xSch153Line45
	 * @param Sch153Line45 the new value for 'Sch153Line45'
	 */
    public void set_Sch153Line45(String _Sch153Line45) {
        this._Sch153Line45 = _Sch153Line45;
    }

    /**
	 * Accessor Method for xSch153Line45
	 * @return the value of 'Sch153Line45'
	 */
    public String get_Sch153Line45() {
        return this._Sch153Line45;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line48' in the PDF.
	 */
    private String _Sch153Line48 = "";

    /**
	 * Mutator Method for xSch153Line48
	 * @param Sch153Line48 the new value for 'Sch153Line48'
	 */
    public void set_Sch153Line48(String _Sch153Line48) {
        this._Sch153Line48 = _Sch153Line48;
    }

    /**
	 * Accessor Method for xSch153Line48
	 * @return the value of 'Sch153Line48'
	 */
    public String get_Sch153Line48() {
        return this._Sch153Line48;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line47' in the PDF.
	 */
    private String _Sch153Line47 = "";

    /**
	 * Mutator Method for xSch153Line47
	 * @param Sch153Line47 the new value for 'Sch153Line47'
	 */
    public void set_Sch153Line47(String _Sch153Line47) {
        this._Sch153Line47 = _Sch153Line47;
    }

    /**
	 * Accessor Method for xSch153Line47
	 * @return the value of 'Sch153Line47'
	 */
    public String get_Sch153Line47() {
        return this._Sch153Line47;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line42' in the PDF.
	 */
    private String _Sch153Line42 = "";

    /**
	 * Mutator Method for xSch153Line42
	 * @param Sch153Line42 the new value for 'Sch153Line42'
	 */
    public void set_Sch153Line42(String _Sch153Line42) {
        this._Sch153Line42 = _Sch153Line42;
    }

    /**
	 * Accessor Method for xSch153Line42
	 * @return the value of 'Sch153Line42'
	 */
    public String get_Sch153Line42() {
        return this._Sch153Line42;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line41' in the PDF.
	 */
    private String _Sch153Line41 = "";

    /**
	 * Mutator Method for xSch153Line41
	 * @param Sch153Line41 the new value for 'Sch153Line41'
	 */
    public void set_Sch153Line41(String _Sch153Line41) {
        this._Sch153Line41 = _Sch153Line41;
    }

    /**
	 * Accessor Method for xSch153Line41
	 * @return the value of 'Sch153Line41'
	 */
    public String get_Sch153Line41() {
        return this._Sch153Line41;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line44' in the PDF.
	 */
    private String _Sch153Line44 = "";

    /**
	 * Mutator Method for xSch153Line44
	 * @param Sch153Line44 the new value for 'Sch153Line44'
	 */
    public void set_Sch153Line44(String _Sch153Line44) {
        this._Sch153Line44 = _Sch153Line44;
    }

    /**
	 * Accessor Method for xSch153Line44
	 * @return the value of 'Sch153Line44'
	 */
    public String get_Sch153Line44() {
        return this._Sch153Line44;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line43' in the PDF.
	 */
    private String _Sch153Line43 = "";

    /**
	 * Mutator Method for xSch153Line43
	 * @param Sch153Line43 the new value for 'Sch153Line43'
	 */
    public void set_Sch153Line43(String _Sch153Line43) {
        this._Sch153Line43 = _Sch153Line43;
    }

    /**
	 * Accessor Method for xSch153Line43
	 * @return the value of 'Sch153Line43'
	 */
    public String get_Sch153Line43() {
        return this._Sch153Line43;
    }

    /**
	 * Class member corresponding to the field 'Sch153Line40' in the PDF.
	 */
    private String _Sch153Line40 = "";

    /**
	 * Mutator Method for xSch153Line40
	 * @param Sch153Line40 the new value for 'Sch153Line40'
	 */
    public void set_Sch153Line40(String _Sch153Line40) {
        this._Sch153Line40 = _Sch153Line40;
    }

    /**
	 * Accessor Method for xSch153Line40
	 * @return the value of 'Sch153Line40'
	 */
    public String get_Sch153Line40() {
        return this._Sch153Line40;
    }
}
