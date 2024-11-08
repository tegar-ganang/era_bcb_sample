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
 * Generated class for filling 'IN117'
 */
public class IN117 {

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
        fields.setFieldProperty("IN111BonusDepAdd", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IN111BonusDepAdd", "textfont", baseFont, null);
        fields.setField("IN111BonusDepAdd", this.get_IN111BonusDepAdd());
        fields.setFieldProperty("IN111Interest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IN111Interest", "textfont", baseFont, null);
        fields.setField("IN111Interest", this.get_IN111Interest());
        fields.setFieldProperty("VTAGI", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTAGI", "textfont", baseFont, null);
        fields.setField("VTAGI", this.get_VTAGI());
        fields.setFieldProperty("IN111Obligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IN111Obligations", "textfont", baseFont, null);
        fields.setField("IN111Obligations", this.get_IN111Obligations());
        fields.setFieldProperty("VTIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTIncomeTax", "textfont", baseFont, null);
        fields.setField("VTIncomeTax", this.get_VTIncomeTax());
        fields.setFieldProperty("BonusDepreciationSub", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BonusDepreciationSub", "textfont", baseFont, null);
        fields.setField("BonusDepreciationSub", this.get_BonusDepreciationSub());
        fields.setFieldProperty("Sch117Line13", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch117Line13", "textfont", baseFont, null);
        fields.setField("Sch117Line13", this.get_Sch117Line13());
        fields.setFieldProperty("Sch117Line5", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch117Line5", "textfont", baseFont, null);
        fields.setField("Sch117Line5", this.get_Sch117Line5());
        fields.setFieldProperty("Sch117Line19", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch117Line19", "textfont", baseFont, null);
        fields.setField("Sch117Line19", this.get_Sch117Line19());
        fields.setFieldProperty("Sch117Line8", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch117Line8", "textfont", baseFont, null);
        fields.setField("Sch117Line8", this.get_Sch117Line8());
        fields.setFieldProperty("CreditTaxPaidOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditTaxPaidOtherState", "textfont", baseFont, null);
        fields.setField("CreditTaxPaidOtherState", this.get_CreditTaxPaidOtherState());
        fields.setFieldProperty("countryName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("countryName", "textfont", baseFont, null);
        fields.setField("countryName", this.get_countryName());
        fields.setFieldProperty("BonusDepreciationAdd", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BonusDepreciationAdd", "textfont", baseFont, null);
        fields.setField("BonusDepreciationAdd", this.get_BonusDepreciationAdd());
        fields.setFieldProperty("FederalAdjustedGrossIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalAdjustedGrossIncome", "textfont", baseFont, null);
        fields.setField("FederalAdjustedGrossIncome", this.get_FederalAdjustedGrossIncome());
        fields.setFieldProperty("IN111BonuseDepSub", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IN111BonuseDepSub", "textfont", baseFont, null);
        fields.setField("IN111BonuseDepSub", this.get_IN111BonuseDepSub());
        fields.setFieldProperty("NonVTObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NonVTObligations", "textfont", baseFont, null);
        fields.setField("NonVTObligations", this.get_NonVTObligations());
        fields.setFieldProperty("Sch117Line16", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch117Line16", "textfont", baseFont, null);
        fields.setField("Sch117Line16", this.get_Sch117Line16());
        fields.setFieldProperty("NetTaxPaidToOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NetTaxPaidToOtherState", "textfont", baseFont, null);
        fields.setField("NetTaxPaidToOtherState", this.get_NetTaxPaidToOtherState());
        fields.setFieldProperty("NameOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NameOtherState", "textfont", baseFont, null);
        fields.setField("NameOtherState", this.get_NameOtherState());
        fields.setFieldProperty("IncomeTaxableToBoth", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IncomeTaxableToBoth", "textfont", baseFont, null);
        fields.setField("IncomeTaxableToBoth", this.get_IncomeTaxableToBoth());
        fields.setFieldProperty("InterestIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("InterestIncome", "textfont", baseFont, null);
        fields.setField("InterestIncome", this.get_InterestIncome());
        fields.setFieldProperty("ModAGI", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ModAGI", "textfont", baseFont, null);
        fields.setField("ModAGI", this.get_ModAGI());
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
	 * Class member corresponding to the field 'IN111BonusDepAdd' in the PDF.
	 */
    private String _IN111BonusDepAdd = "";

    /**
	 * Mutator Method for xIN111BonusDepAdd
	 * @param IN111BonusDepAdd the new value for 'IN111BonusDepAdd'
	 */
    public void set_IN111BonusDepAdd(String _IN111BonusDepAdd) {
        this._IN111BonusDepAdd = _IN111BonusDepAdd;
    }

    /**
	 * Accessor Method for xIN111BonusDepAdd
	 * @return the value of 'IN111BonusDepAdd'
	 */
    public String get_IN111BonusDepAdd() {
        return this._IN111BonusDepAdd;
    }

    /**
	 * Class member corresponding to the field 'IN111Interest' in the PDF.
	 */
    private String _IN111Interest = "";

    /**
	 * Mutator Method for xIN111Interest
	 * @param IN111Interest the new value for 'IN111Interest'
	 */
    public void set_IN111Interest(String _IN111Interest) {
        this._IN111Interest = _IN111Interest;
    }

    /**
	 * Accessor Method for xIN111Interest
	 * @return the value of 'IN111Interest'
	 */
    public String get_IN111Interest() {
        return this._IN111Interest;
    }

    /**
	 * Class member corresponding to the field 'VTAGI' in the PDF.
	 */
    private String _VTAGI = "";

    /**
	 * Mutator Method for xVTAGI
	 * @param VTAGI the new value for 'VTAGI'
	 */
    public void set_VTAGI(String _VTAGI) {
        this._VTAGI = _VTAGI;
    }

    /**
	 * Accessor Method for xVTAGI
	 * @return the value of 'VTAGI'
	 */
    public String get_VTAGI() {
        return this._VTAGI;
    }

    /**
	 * Class member corresponding to the field 'IN111Obligations' in the PDF.
	 */
    private String _IN111Obligations = "";

    /**
	 * Mutator Method for xIN111Obligations
	 * @param IN111Obligations the new value for 'IN111Obligations'
	 */
    public void set_IN111Obligations(String _IN111Obligations) {
        this._IN111Obligations = _IN111Obligations;
    }

    /**
	 * Accessor Method for xIN111Obligations
	 * @return the value of 'IN111Obligations'
	 */
    public String get_IN111Obligations() {
        return this._IN111Obligations;
    }

    /**
	 * Class member corresponding to the field 'VTIncomeTax' in the PDF.
	 */
    private String _VTIncomeTax = "";

    /**
	 * Mutator Method for xVTIncomeTax
	 * @param VTIncomeTax the new value for 'VTIncomeTax'
	 */
    public void set_VTIncomeTax(String _VTIncomeTax) {
        this._VTIncomeTax = _VTIncomeTax;
    }

    /**
	 * Accessor Method for xVTIncomeTax
	 * @return the value of 'VTIncomeTax'
	 */
    public String get_VTIncomeTax() {
        return this._VTIncomeTax;
    }

    /**
	 * Class member corresponding to the field 'BonusDepreciationSub' in the PDF.
	 */
    private String _BonusDepreciationSub = "";

    /**
	 * Mutator Method for xBonusDepreciationSub
	 * @param BonusDepreciationSub the new value for 'BonusDepreciationSub'
	 */
    public void set_BonusDepreciationSub(String _BonusDepreciationSub) {
        this._BonusDepreciationSub = _BonusDepreciationSub;
    }

    /**
	 * Accessor Method for xBonusDepreciationSub
	 * @return the value of 'BonusDepreciationSub'
	 */
    public String get_BonusDepreciationSub() {
        return this._BonusDepreciationSub;
    }

    /**
	 * Class member corresponding to the field 'Sch117Line13' in the PDF.
	 */
    private String _Sch117Line13 = "";

    /**
	 * Mutator Method for xSch117Line13
	 * @param Sch117Line13 the new value for 'Sch117Line13'
	 */
    public void set_Sch117Line13(String _Sch117Line13) {
        this._Sch117Line13 = _Sch117Line13;
    }

    /**
	 * Accessor Method for xSch117Line13
	 * @return the value of 'Sch117Line13'
	 */
    public String get_Sch117Line13() {
        return this._Sch117Line13;
    }

    /**
	 * Class member corresponding to the field 'Sch117Line5' in the PDF.
	 */
    private String _Sch117Line5 = "";

    /**
	 * Mutator Method for xSch117Line5
	 * @param Sch117Line5 the new value for 'Sch117Line5'
	 */
    public void set_Sch117Line5(String _Sch117Line5) {
        this._Sch117Line5 = _Sch117Line5;
    }

    /**
	 * Accessor Method for xSch117Line5
	 * @return the value of 'Sch117Line5'
	 */
    public String get_Sch117Line5() {
        return this._Sch117Line5;
    }

    /**
	 * Class member corresponding to the field 'Sch117Line19' in the PDF.
	 */
    private String _Sch117Line19 = "";

    /**
	 * Mutator Method for xSch117Line19
	 * @param Sch117Line19 the new value for 'Sch117Line19'
	 */
    public void set_Sch117Line19(String _Sch117Line19) {
        this._Sch117Line19 = _Sch117Line19;
    }

    /**
	 * Accessor Method for xSch117Line19
	 * @return the value of 'Sch117Line19'
	 */
    public String get_Sch117Line19() {
        return this._Sch117Line19;
    }

    /**
	 * Class member corresponding to the field 'Sch117Line8' in the PDF.
	 */
    private String _Sch117Line8 = "";

    /**
	 * Mutator Method for xSch117Line8
	 * @param Sch117Line8 the new value for 'Sch117Line8'
	 */
    public void set_Sch117Line8(String _Sch117Line8) {
        this._Sch117Line8 = _Sch117Line8;
    }

    /**
	 * Accessor Method for xSch117Line8
	 * @return the value of 'Sch117Line8'
	 */
    public String get_Sch117Line8() {
        return this._Sch117Line8;
    }

    /**
	 * Class member corresponding to the field 'CreditTaxPaidOtherState' in the PDF.
	 */
    private String _CreditTaxPaidOtherState = "";

    /**
	 * Mutator Method for xCreditTaxPaidOtherState
	 * @param CreditTaxPaidOtherState the new value for 'CreditTaxPaidOtherState'
	 */
    public void set_CreditTaxPaidOtherState(String _CreditTaxPaidOtherState) {
        this._CreditTaxPaidOtherState = _CreditTaxPaidOtherState;
    }

    /**
	 * Accessor Method for xCreditTaxPaidOtherState
	 * @return the value of 'CreditTaxPaidOtherState'
	 */
    public String get_CreditTaxPaidOtherState() {
        return this._CreditTaxPaidOtherState;
    }

    /**
	 * Class member corresponding to the field 'countryName' in the PDF.
	 */
    private String _countryName = "";

    /**
	 * Mutator Method for xcountryName
	 * @param countryName the new value for 'countryName'
	 */
    public void set_countryName(String _countryName) {
        this._countryName = _countryName;
    }

    /**
	 * Accessor Method for xcountryName
	 * @return the value of 'countryName'
	 */
    public String get_countryName() {
        return this._countryName;
    }

    /**
	 * Class member corresponding to the field 'BonusDepreciationAdd' in the PDF.
	 */
    private String _BonusDepreciationAdd = "";

    /**
	 * Mutator Method for xBonusDepreciationAdd
	 * @param BonusDepreciationAdd the new value for 'BonusDepreciationAdd'
	 */
    public void set_BonusDepreciationAdd(String _BonusDepreciationAdd) {
        this._BonusDepreciationAdd = _BonusDepreciationAdd;
    }

    /**
	 * Accessor Method for xBonusDepreciationAdd
	 * @return the value of 'BonusDepreciationAdd'
	 */
    public String get_BonusDepreciationAdd() {
        return this._BonusDepreciationAdd;
    }

    /**
	 * Class member corresponding to the field 'FederalAdjustedGrossIncome' in the PDF.
	 */
    private String _FederalAdjustedGrossIncome = "";

    /**
	 * Mutator Method for xFederalAdjustedGrossIncome
	 * @param FederalAdjustedGrossIncome the new value for 'FederalAdjustedGrossIncome'
	 */
    public void set_FederalAdjustedGrossIncome(String _FederalAdjustedGrossIncome) {
        this._FederalAdjustedGrossIncome = _FederalAdjustedGrossIncome;
    }

    /**
	 * Accessor Method for xFederalAdjustedGrossIncome
	 * @return the value of 'FederalAdjustedGrossIncome'
	 */
    public String get_FederalAdjustedGrossIncome() {
        return this._FederalAdjustedGrossIncome;
    }

    /**
	 * Class member corresponding to the field 'IN111BonuseDepSub' in the PDF.
	 */
    private String _IN111BonuseDepSub = "";

    /**
	 * Mutator Method for xIN111BonuseDepSub
	 * @param IN111BonuseDepSub the new value for 'IN111BonuseDepSub'
	 */
    public void set_IN111BonuseDepSub(String _IN111BonuseDepSub) {
        this._IN111BonuseDepSub = _IN111BonuseDepSub;
    }

    /**
	 * Accessor Method for xIN111BonuseDepSub
	 * @return the value of 'IN111BonuseDepSub'
	 */
    public String get_IN111BonuseDepSub() {
        return this._IN111BonuseDepSub;
    }

    /**
	 * Class member corresponding to the field 'NonVTObligations' in the PDF.
	 */
    private String _NonVTObligations = "";

    /**
	 * Mutator Method for xNonVTObligations
	 * @param NonVTObligations the new value for 'NonVTObligations'
	 */
    public void set_NonVTObligations(String _NonVTObligations) {
        this._NonVTObligations = _NonVTObligations;
    }

    /**
	 * Accessor Method for xNonVTObligations
	 * @return the value of 'NonVTObligations'
	 */
    public String get_NonVTObligations() {
        return this._NonVTObligations;
    }

    /**
	 * Class member corresponding to the field 'Sch117Line16' in the PDF.
	 */
    private String _Sch117Line16 = "";

    /**
	 * Mutator Method for xSch117Line16
	 * @param Sch117Line16 the new value for 'Sch117Line16'
	 */
    public void set_Sch117Line16(String _Sch117Line16) {
        this._Sch117Line16 = _Sch117Line16;
    }

    /**
	 * Accessor Method for xSch117Line16
	 * @return the value of 'Sch117Line16'
	 */
    public String get_Sch117Line16() {
        return this._Sch117Line16;
    }

    /**
	 * Class member corresponding to the field 'NetTaxPaidToOtherState' in the PDF.
	 */
    private String _NetTaxPaidToOtherState = "";

    /**
	 * Mutator Method for xNetTaxPaidToOtherState
	 * @param NetTaxPaidToOtherState the new value for 'NetTaxPaidToOtherState'
	 */
    public void set_NetTaxPaidToOtherState(String _NetTaxPaidToOtherState) {
        this._NetTaxPaidToOtherState = _NetTaxPaidToOtherState;
    }

    /**
	 * Accessor Method for xNetTaxPaidToOtherState
	 * @return the value of 'NetTaxPaidToOtherState'
	 */
    public String get_NetTaxPaidToOtherState() {
        return this._NetTaxPaidToOtherState;
    }

    /**
	 * Class member corresponding to the field 'NameOtherState' in the PDF.
	 */
    private String _NameOtherState = "";

    /**
	 * Mutator Method for xNameOtherState
	 * @param NameOtherState the new value for 'NameOtherState'
	 */
    public void set_NameOtherState(String _NameOtherState) {
        this._NameOtherState = _NameOtherState;
    }

    /**
	 * Accessor Method for xNameOtherState
	 * @return the value of 'NameOtherState'
	 */
    public String get_NameOtherState() {
        return this._NameOtherState;
    }

    /**
	 * Class member corresponding to the field 'IncomeTaxableToBoth' in the PDF.
	 */
    private String _IncomeTaxableToBoth = "";

    /**
	 * Mutator Method for xIncomeTaxableToBoth
	 * @param IncomeTaxableToBoth the new value for 'IncomeTaxableToBoth'
	 */
    public void set_IncomeTaxableToBoth(String _IncomeTaxableToBoth) {
        this._IncomeTaxableToBoth = _IncomeTaxableToBoth;
    }

    /**
	 * Accessor Method for xIncomeTaxableToBoth
	 * @return the value of 'IncomeTaxableToBoth'
	 */
    public String get_IncomeTaxableToBoth() {
        return this._IncomeTaxableToBoth;
    }

    /**
	 * Class member corresponding to the field 'InterestIncome' in the PDF.
	 */
    private String _InterestIncome = "";

    /**
	 * Mutator Method for xInterestIncome
	 * @param InterestIncome the new value for 'InterestIncome'
	 */
    public void set_InterestIncome(String _InterestIncome) {
        this._InterestIncome = _InterestIncome;
    }

    /**
	 * Accessor Method for xInterestIncome
	 * @return the value of 'InterestIncome'
	 */
    public String get_InterestIncome() {
        return this._InterestIncome;
    }

    /**
	 * Class member corresponding to the field 'ModAGI' in the PDF.
	 */
    private String _ModAGI = "";

    /**
	 * Mutator Method for xModAGI
	 * @param ModAGI the new value for 'ModAGI'
	 */
    public void set_ModAGI(String _ModAGI) {
        this._ModAGI = _ModAGI;
    }

    /**
	 * Accessor Method for xModAGI
	 * @return the value of 'ModAGI'
	 */
    public String get_ModAGI() {
        return this._ModAGI;
    }
}
