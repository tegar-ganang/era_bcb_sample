package net.sf.gateway.mef.pdf.ty2009;

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
        fields.setFieldProperty("ItemDeductionLine3", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ItemDeductionLine3", "textfont", baseFont, null);
        fields.setField("ItemDeductionLine3", this.get_ItemDeductionLine3());
        fields.setFieldProperty("Sch154Line17", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line17", "textfont", baseFont, null);
        fields.setField("Sch154Line17", this.get_Sch154Line17());
        fields.setFieldProperty("MotorVehicleTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("MotorVehicleTax", "textfont", baseFont, null);
        fields.setField("MotorVehicleTax", this.get_MotorVehicleTax());
        fields.setFieldProperty("ItemizedDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ItemizedDeductions", "textfont", baseFont, null);
        fields.setField("ItemizedDeductions", this.get_ItemizedDeductions());
        fields.setFieldProperty("Sch154LIne15", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154LIne15", "textfont", baseFont, null);
        fields.setField("Sch154LIne15", this.get_Sch154LIne15());
        fields.setFieldProperty("AllowFedStandDed", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllowFedStandDed", "textfont", baseFont, null);
        fields.setField("AllowFedStandDed", this.get_AllowFedStandDed());
        fields.setFieldProperty("ItemizedDedAm", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ItemizedDedAm", "textfont", baseFont, null);
        fields.setField("ItemizedDedAm", this.get_ItemizedDedAm());
        fields.setFieldProperty("NewMotorVehicleTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NewMotorVehicleTax", "textfont", baseFont, null);
        fields.setField("NewMotorVehicleTax", this.get_NewMotorVehicleTax());
        fields.setFieldProperty("Sch154Line18", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line18", "textfont", baseFont, null);
        fields.setField("Sch154Line18", this.get_Sch154Line18());
        fields.setFieldProperty("StateIncomeDedAmt", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateIncomeDedAmt", "textfont", baseFont, null);
        fields.setField("StateIncomeDedAmt", this.get_StateIncomeDedAmt());
        fields.setFieldProperty("Sch154Line10", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line10", "textfont", baseFont, null);
        fields.setField("Sch154Line10", this.get_Sch154Line10());
        fields.setFieldProperty("Text79", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text79", "textfont", baseFont, null);
        fields.setField("Text79", this.get_Text79());
        fields.setFieldProperty("StateIncomeTaxDed", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateIncomeTaxDed", "textfont", baseFont, null);
        fields.setField("StateIncomeTaxDed", this.get_StateIncomeTaxDed());
        fields.setFieldProperty("Sch154Line14", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line14", "textfont", baseFont, null);
        fields.setField("Sch154Line14", this.get_Sch154Line14());
        fields.setFieldProperty("Sch154Line9", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line9", "textfont", baseFont, null);
        fields.setField("Sch154Line9", this.get_Sch154Line9());
        fields.setFieldProperty("Sch154Line19", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line19", "textfont", baseFont, null);
        fields.setField("Sch154Line19", this.get_Sch154Line19());
        fields.setFieldProperty("DeductionSubAmt", "textsize", new Float(20.2), null);
        fields.setFieldProperty("DeductionSubAmt", "textfont", baseFont, null);
        fields.setField("DeductionSubAmt", this.get_DeductionSubAmt());
        fields.setFieldProperty("Sch154Line12", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch154Line12", "textfont", baseFont, null);
        fields.setField("Sch154Line12", this.get_Sch154Line12());
        fields.setFieldProperty("ItemDeducitonLine11", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ItemDeducitonLine11", "textfont", baseFont, null);
        fields.setField("ItemDeducitonLine11", this.get_ItemDeducitonLine11());
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
	 * Class member corresponding to the field 'ItemDeductionLine3' in the PDF.
	 */
    private String _ItemDeductionLine3 = "";

    /**
	 * Mutator Method for xItemDeductionLine3
	 * @param ItemDeductionLine3 the new value for 'ItemDeductionLine3'
	 */
    public void set_ItemDeductionLine3(String _ItemDeductionLine3) {
        this._ItemDeductionLine3 = _ItemDeductionLine3;
    }

    /**
	 * Accessor Method for xItemDeductionLine3
	 * @return the value of 'ItemDeductionLine3'
	 */
    public String get_ItemDeductionLine3() {
        return this._ItemDeductionLine3;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line17' in the PDF.
	 */
    private String _Sch154Line17 = "";

    /**
	 * Mutator Method for xSch154Line17
	 * @param Sch154Line17 the new value for 'Sch154Line17'
	 */
    public void set_Sch154Line17(String _Sch154Line17) {
        this._Sch154Line17 = _Sch154Line17;
    }

    /**
	 * Accessor Method for xSch154Line17
	 * @return the value of 'Sch154Line17'
	 */
    public String get_Sch154Line17() {
        return this._Sch154Line17;
    }

    /**
	 * Class member corresponding to the field 'MotorVehicleTax' in the PDF.
	 */
    private String _MotorVehicleTax = "";

    /**
	 * Mutator Method for xMotorVehicleTax
	 * @param MotorVehicleTax the new value for 'MotorVehicleTax'
	 */
    public void set_MotorVehicleTax(String _MotorVehicleTax) {
        this._MotorVehicleTax = _MotorVehicleTax;
    }

    /**
	 * Accessor Method for xMotorVehicleTax
	 * @return the value of 'MotorVehicleTax'
	 */
    public String get_MotorVehicleTax() {
        return this._MotorVehicleTax;
    }

    /**
	 * Class member corresponding to the field 'ItemizedDeductions' in the PDF.
	 */
    private String _ItemizedDeductions = "";

    /**
	 * Mutator Method for xItemizedDeductions
	 * @param ItemizedDeductions the new value for 'ItemizedDeductions'
	 */
    public void set_ItemizedDeductions(String _ItemizedDeductions) {
        this._ItemizedDeductions = _ItemizedDeductions;
    }

    /**
	 * Accessor Method for xItemizedDeductions
	 * @return the value of 'ItemizedDeductions'
	 */
    public String get_ItemizedDeductions() {
        return this._ItemizedDeductions;
    }

    /**
	 * Class member corresponding to the field 'Sch154LIne15' in the PDF.
	 */
    private String _Sch154LIne15 = "";

    /**
	 * Mutator Method for xSch154LIne15
	 * @param Sch154LIne15 the new value for 'Sch154LIne15'
	 */
    public void set_Sch154LIne15(String _Sch154LIne15) {
        this._Sch154LIne15 = _Sch154LIne15;
    }

    /**
	 * Accessor Method for xSch154LIne15
	 * @return the value of 'Sch154LIne15'
	 */
    public String get_Sch154LIne15() {
        return this._Sch154LIne15;
    }

    /**
	 * Class member corresponding to the field 'AllowFedStandDed' in the PDF.
	 */
    private String _AllowFedStandDed = "";

    /**
	 * Mutator Method for xAllowFedStandDed
	 * @param AllowFedStandDed the new value for 'AllowFedStandDed'
	 */
    public void set_AllowFedStandDed(String _AllowFedStandDed) {
        this._AllowFedStandDed = _AllowFedStandDed;
    }

    /**
	 * Accessor Method for xAllowFedStandDed
	 * @return the value of 'AllowFedStandDed'
	 */
    public String get_AllowFedStandDed() {
        return this._AllowFedStandDed;
    }

    /**
	 * Class member corresponding to the field 'ItemizedDedAm' in the PDF.
	 */
    private String _ItemizedDedAm = "";

    /**
	 * Mutator Method for xItemizedDedAm
	 * @param ItemizedDedAm the new value for 'ItemizedDedAm'
	 */
    public void set_ItemizedDedAm(String _ItemizedDedAm) {
        this._ItemizedDedAm = _ItemizedDedAm;
    }

    /**
	 * Accessor Method for xItemizedDedAm
	 * @return the value of 'ItemizedDedAm'
	 */
    public String get_ItemizedDedAm() {
        return this._ItemizedDedAm;
    }

    /**
	 * Class member corresponding to the field 'NewMotorVehicleTax' in the PDF.
	 */
    private String _NewMotorVehicleTax = "";

    /**
	 * Mutator Method for xNewMotorVehicleTax
	 * @param NewMotorVehicleTax the new value for 'NewMotorVehicleTax'
	 */
    public void set_NewMotorVehicleTax(String _NewMotorVehicleTax) {
        this._NewMotorVehicleTax = _NewMotorVehicleTax;
    }

    /**
	 * Accessor Method for xNewMotorVehicleTax
	 * @return the value of 'NewMotorVehicleTax'
	 */
    public String get_NewMotorVehicleTax() {
        return this._NewMotorVehicleTax;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line18' in the PDF.
	 */
    private String _Sch154Line18 = "";

    /**
	 * Mutator Method for xSch154Line18
	 * @param Sch154Line18 the new value for 'Sch154Line18'
	 */
    public void set_Sch154Line18(String _Sch154Line18) {
        this._Sch154Line18 = _Sch154Line18;
    }

    /**
	 * Accessor Method for xSch154Line18
	 * @return the value of 'Sch154Line18'
	 */
    public String get_Sch154Line18() {
        return this._Sch154Line18;
    }

    /**
	 * Class member corresponding to the field 'StateIncomeDedAmt' in the PDF.
	 */
    private String _StateIncomeDedAmt = "";

    /**
	 * Mutator Method for xStateIncomeDedAmt
	 * @param StateIncomeDedAmt the new value for 'StateIncomeDedAmt'
	 */
    public void set_StateIncomeDedAmt(String _StateIncomeDedAmt) {
        this._StateIncomeDedAmt = _StateIncomeDedAmt;
    }

    /**
	 * Accessor Method for xStateIncomeDedAmt
	 * @return the value of 'StateIncomeDedAmt'
	 */
    public String get_StateIncomeDedAmt() {
        return this._StateIncomeDedAmt;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line10' in the PDF.
	 */
    private String _Sch154Line10 = "";

    /**
	 * Mutator Method for xSch154Line10
	 * @param Sch154Line10 the new value for 'Sch154Line10'
	 */
    public void set_Sch154Line10(String _Sch154Line10) {
        this._Sch154Line10 = _Sch154Line10;
    }

    /**
	 * Accessor Method for xSch154Line10
	 * @return the value of 'Sch154Line10'
	 */
    public String get_Sch154Line10() {
        return this._Sch154Line10;
    }

    /**
	 * Class member corresponding to the field 'Text79' in the PDF.
	 */
    private String _Text79 = "";

    /**
	 * Mutator Method for xText79
	 * @param Text79 the new value for 'Text79'
	 */
    public void set_Text79(String _Text79) {
        this._Text79 = _Text79;
    }

    /**
	 * Accessor Method for xText79
	 * @return the value of 'Text79'
	 */
    public String get_Text79() {
        return this._Text79;
    }

    /**
	 * Class member corresponding to the field 'StateIncomeTaxDed' in the PDF.
	 */
    private String _StateIncomeTaxDed = "";

    /**
	 * Mutator Method for xStateIncomeTaxDed
	 * @param StateIncomeTaxDed the new value for 'StateIncomeTaxDed'
	 */
    public void set_StateIncomeTaxDed(String _StateIncomeTaxDed) {
        this._StateIncomeTaxDed = _StateIncomeTaxDed;
    }

    /**
	 * Accessor Method for xStateIncomeTaxDed
	 * @return the value of 'StateIncomeTaxDed'
	 */
    public String get_StateIncomeTaxDed() {
        return this._StateIncomeTaxDed;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line14' in the PDF.
	 */
    private String _Sch154Line14 = "";

    /**
	 * Mutator Method for xSch154Line14
	 * @param Sch154Line14 the new value for 'Sch154Line14'
	 */
    public void set_Sch154Line14(String _Sch154Line14) {
        this._Sch154Line14 = _Sch154Line14;
    }

    /**
	 * Accessor Method for xSch154Line14
	 * @return the value of 'Sch154Line14'
	 */
    public String get_Sch154Line14() {
        return this._Sch154Line14;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line9' in the PDF.
	 */
    private String _Sch154Line9 = "";

    /**
	 * Mutator Method for xSch154Line9
	 * @param Sch154Line9 the new value for 'Sch154Line9'
	 */
    public void set_Sch154Line9(String _Sch154Line9) {
        this._Sch154Line9 = _Sch154Line9;
    }

    /**
	 * Accessor Method for xSch154Line9
	 * @return the value of 'Sch154Line9'
	 */
    public String get_Sch154Line9() {
        return this._Sch154Line9;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line19' in the PDF.
	 */
    private String _Sch154Line19 = "";

    /**
	 * Mutator Method for xSch154Line19
	 * @param Sch154Line19 the new value for 'Sch154Line19'
	 */
    public void set_Sch154Line19(String _Sch154Line19) {
        this._Sch154Line19 = _Sch154Line19;
    }

    /**
	 * Accessor Method for xSch154Line19
	 * @return the value of 'Sch154Line19'
	 */
    public String get_Sch154Line19() {
        return this._Sch154Line19;
    }

    /**
	 * Class member corresponding to the field 'DeductionSubAmt' in the PDF.
	 */
    private String _DeductionSubAmt = "";

    /**
	 * Mutator Method for xDeductionSubAmt
	 * @param DeductionSubAmt the new value for 'DeductionSubAmt'
	 */
    public void set_DeductionSubAmt(String _DeductionSubAmt) {
        this._DeductionSubAmt = _DeductionSubAmt;
    }

    /**
	 * Accessor Method for xDeductionSubAmt
	 * @return the value of 'DeductionSubAmt'
	 */
    public String get_DeductionSubAmt() {
        return this._DeductionSubAmt;
    }

    /**
	 * Class member corresponding to the field 'Sch154Line12' in the PDF.
	 */
    private String _Sch154Line12 = "";

    /**
	 * Mutator Method for xSch154Line12
	 * @param Sch154Line12 the new value for 'Sch154Line12'
	 */
    public void set_Sch154Line12(String _Sch154Line12) {
        this._Sch154Line12 = _Sch154Line12;
    }

    /**
	 * Accessor Method for xSch154Line12
	 * @return the value of 'Sch154Line12'
	 */
    public String get_Sch154Line12() {
        return this._Sch154Line12;
    }

    /**
	 * Class member corresponding to the field 'ItemDeducitonLine11' in the PDF.
	 */
    private String _ItemDeducitonLine11 = "";

    /**
	 * Mutator Method for xItemDeducitonLine11
	 * @param ItemDeducitonLine11 the new value for 'ItemDeducitonLine11'
	 */
    public void set_ItemDeducitonLine11(String _ItemDeducitonLine11) {
        this._ItemDeducitonLine11 = _ItemDeducitonLine11;
    }

    /**
	 * Accessor Method for xItemDeducitonLine11
	 * @return the value of 'ItemDeducitonLine11'
	 */
    public String get_ItemDeducitonLine11() {
        return this._ItemDeducitonLine11;
    }
}
