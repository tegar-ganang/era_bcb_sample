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
 * Generated class for filling 'IN154'
 */
public class IN154 {

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
        fields.setFieldProperty("1544", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1544", "textfont", baseFont, null);
        fields.setField("1544", this.get_1544());
        fields.setFieldProperty("1549", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1549", "textfont", baseFont, null);
        fields.setField("1549", this.get_1549());
        fields.setFieldProperty("1542", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1542", "textfont", baseFont, null);
        fields.setField("1542", this.get_1542());
        fields.setFieldProperty("1546", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1546", "textfont", baseFont, null);
        fields.setField("1546", this.get_1546());
        fields.setFieldProperty("1548", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1548", "textfont", baseFont, null);
        fields.setField("1548", this.get_1548());
        fields.setFieldProperty("1543", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1543", "textfont", baseFont, null);
        fields.setField("1543", this.get_1543());
        fields.setFieldProperty("1547", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1547", "textfont", baseFont, null);
        fields.setField("1547", this.get_1547());
        fields.setFieldProperty("1541", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1541", "textfont", baseFont, null);
        fields.setField("1541", this.get_1541());
        fields.setFieldProperty("15410", "textsize", new Float(20.2), null);
        fields.setFieldProperty("15410", "textfont", baseFont, null);
        fields.setField("15410", this.get_15410());
        fields.setFieldProperty("1545", "textsize", new Float(20.2), null);
        fields.setFieldProperty("1545", "textfont", baseFont, null);
        fields.setField("1545", this.get_1545());
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
	 * Class member corresponding to the field '1544' in the PDF.
	 */
    private String _1544 = "";

    /**
	 * Mutator Method for x1544
	 * @param 1544 the new value for '1544'
	 */
    public void set_1544(String _1544) {
        this._1544 = _1544;
    }

    /**
	 * Accessor Method for x1544
	 * @return the value of '1544'
	 */
    public String get_1544() {
        return this._1544;
    }

    /**
	 * Class member corresponding to the field '1549' in the PDF.
	 */
    private String _1549 = "";

    /**
	 * Mutator Method for x1549
	 * @param 1549 the new value for '1549'
	 */
    public void set_1549(String _1549) {
        this._1549 = _1549;
    }

    /**
	 * Accessor Method for x1549
	 * @return the value of '1549'
	 */
    public String get_1549() {
        return this._1549;
    }

    /**
	 * Class member corresponding to the field '1542' in the PDF.
	 */
    private String _1542 = "";

    /**
	 * Mutator Method for x1542
	 * @param 1542 the new value for '1542'
	 */
    public void set_1542(String _1542) {
        this._1542 = _1542;
    }

    /**
	 * Accessor Method for x1542
	 * @return the value of '1542'
	 */
    public String get_1542() {
        return this._1542;
    }

    /**
	 * Class member corresponding to the field '1546' in the PDF.
	 */
    private String _1546 = "";

    /**
	 * Mutator Method for x1546
	 * @param 1546 the new value for '1546'
	 */
    public void set_1546(String _1546) {
        this._1546 = _1546;
    }

    /**
	 * Accessor Method for x1546
	 * @return the value of '1546'
	 */
    public String get_1546() {
        return this._1546;
    }

    /**
	 * Class member corresponding to the field '1548' in the PDF.
	 */
    private String _1548 = "";

    /**
	 * Mutator Method for x1548
	 * @param 1548 the new value for '1548'
	 */
    public void set_1548(String _1548) {
        this._1548 = _1548;
    }

    /**
	 * Accessor Method for x1548
	 * @return the value of '1548'
	 */
    public String get_1548() {
        return this._1548;
    }

    /**
	 * Class member corresponding to the field '1543' in the PDF.
	 */
    private String _1543 = "";

    /**
	 * Mutator Method for x1543
	 * @param 1543 the new value for '1543'
	 */
    public void set_1543(String _1543) {
        this._1543 = _1543;
    }

    /**
	 * Accessor Method for x1543
	 * @return the value of '1543'
	 */
    public String get_1543() {
        return this._1543;
    }

    /**
	 * Class member corresponding to the field '1547' in the PDF.
	 */
    private String _1547 = "";

    /**
	 * Mutator Method for x1547
	 * @param 1547 the new value for '1547'
	 */
    public void set_1547(String _1547) {
        this._1547 = _1547;
    }

    /**
	 * Accessor Method for x1547
	 * @return the value of '1547'
	 */
    public String get_1547() {
        return this._1547;
    }

    /**
	 * Class member corresponding to the field '1541' in the PDF.
	 */
    private String _1541 = "";

    /**
	 * Mutator Method for x1541
	 * @param 1541 the new value for '1541'
	 */
    public void set_1541(String _1541) {
        this._1541 = _1541;
    }

    /**
	 * Accessor Method for x1541
	 * @return the value of '1541'
	 */
    public String get_1541() {
        return this._1541;
    }

    /**
	 * Class member corresponding to the field '15410' in the PDF.
	 */
    private String _15410 = "";

    /**
	 * Mutator Method for x15410
	 * @param 15410 the new value for '15410'
	 */
    public void set_15410(String _15410) {
        this._15410 = _15410;
    }

    /**
	 * Accessor Method for x15410
	 * @return the value of '15410'
	 */
    public String get_15410() {
        return this._15410;
    }

    /**
	 * Class member corresponding to the field '1545' in the PDF.
	 */
    private String _1545 = "";

    /**
	 * Mutator Method for x1545
	 * @param 1545 the new value for '1545'
	 */
    public void set_1545(String _1545) {
        this._1545 = _1545;
    }

    /**
	 * Accessor Method for x1545
	 * @return the value of '1545'
	 */
    public String get_1545() {
        return this._1545;
    }
}
