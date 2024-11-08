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
 * Generated class for filling 'TY2009_VT_HI144.pdf'
 */
public class TY2009_VT_HI144 {

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
        fields.setFieldProperty("Text1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text1", "textfont", baseFont, null);
        fields.setField("Text1", this.get_Text1());
        fields.setFieldProperty("CashPublicAssistance", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CashPublicAssistance", "textfont", baseFont, null);
        fields.setField("CashPublicAssistance", this.get_CashPublicAssistance());
        fields.setFieldProperty("UnemployWorkersComp", "textsize", new Float(20.2), null);
        fields.setFieldProperty("UnemployWorkersComp", "textfont", baseFont, null);
        fields.setField("UnemployWorkersComp", this.get_UnemployWorkersComp());
        fields.setFieldProperty("TotalHouseholdIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalHouseholdIncome", "textfont", baseFont, null);
        fields.setField("TotalHouseholdIncome", this.get_TotalHouseholdIncome());
        fields.setFieldProperty("InterestDividends", "textsize", new Float(20.2), null);
        fields.setFieldProperty("InterestDividends", "textfont", baseFont, null);
        fields.setField("InterestDividends", this.get_InterestDividends());
        fields.setFieldProperty("Pensions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Pensions", "textfont", baseFont, null);
        fields.setField("Pensions", this.get_Pensions());
        fields.setFieldProperty("AdjustedIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AdjustedIncome", "textfont", baseFont, null);
        fields.setField("AdjustedIncome", this.get_AdjustedIncome());
        fields.setFieldProperty("BusinessIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessIncome", "textfont", baseFont, null);
        fields.setField("BusinessIncome", this.get_BusinessIncome());
        fields.setFieldProperty("IncomeUSObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IncomeUSObligations", "textfont", baseFont, null);
        fields.setField("IncomeUSObligations", this.get_IncomeUSObligations());
        fields.setFieldProperty("TotalIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalIncome", "textfont", baseFont, null);
        fields.setField("TotalIncome", this.get_TotalIncome());
        fields.setFieldProperty("OtherIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OtherIncome", "textfont", baseFont, null);
        fields.setField("OtherIncome", this.get_OtherIncome());
        fields.setFieldProperty("Text117", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text117", "textfont", baseFont, null);
        fields.setField("Text117", this.get_Text117());
        fields.setFieldProperty("FederalAdjustments", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalAdjustments", "textfont", baseFont, null);
        fields.setField("FederalAdjustments", this.get_FederalAdjustments());
        fields.setFieldProperty("AlimonySupport", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AlimonySupport", "textfont", baseFont, null);
        fields.setField("AlimonySupport", this.get_AlimonySupport());
        fields.setFieldProperty("Text118", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text118", "textfont", baseFont, null);
        fields.setField("Text118", this.get_Text118());
        fields.setFieldProperty("Text119", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text119", "textfont", baseFont, null);
        fields.setField("Text119", this.get_Text119());
        fields.setFieldProperty("SocialSecurityRailRoadBenefits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SocialSecurityRailRoadBenefits", "textfont", baseFont, null);
        fields.setField("SocialSecurityRailRoadBenefits", this.get_SocialSecurityRailRoadBenefits());
        fields.setFieldProperty("ChildSupportPaid", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ChildSupportPaid", "textfont", baseFont, null);
        fields.setField("ChildSupportPaid", this.get_ChildSupportPaid());
        fields.setFieldProperty("TotalAdjustments", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalAdjustments", "textfont", baseFont, null);
        fields.setField("TotalAdjustments", this.get_TotalAdjustments());
        fields.setFieldProperty("FarmPartnerships", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FarmPartnerships", "textfont", baseFont, null);
        fields.setField("FarmPartnerships", this.get_FarmPartnerships());
        fields.setFieldProperty("OtherPersonsNameSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OtherPersonsNameSSN", "textfont", baseFont, null);
        fields.setField("OtherPersonsNameSSN", this.get_OtherPersonsNameSSN());
        fields.setFieldProperty("RentalIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RentalIncome", "textfont", baseFont, null);
        fields.setField("RentalIncome", this.get_RentalIncome());
        fields.setFieldProperty("CapitalGain", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CapitalGain", "textfont", baseFont, null);
        fields.setField("CapitalGain", this.get_CapitalGain());
        fields.setFieldProperty("Wages", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Wages", "textfont", baseFont, null);
        fields.setField("Wages", this.get_Wages());
        fields.setFieldProperty("SSMedicareWithheld", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SSMedicareWithheld", "textfont", baseFont, null);
        fields.setField("SSMedicareWithheld", this.get_SSMedicareWithheld());
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
	 * Class member corresponding to the field 'Text1' in the PDF.
	 */
    private String _Text1 = "";

    /**
	 * Mutator Method for xText1
	 * @param Text1 the new value for 'Text1'
	 */
    public void set_Text1(String _Text1) {
        this._Text1 = _Text1;
    }

    /**
	 * Accessor Method for xText1
	 * @return the value of 'Text1'
	 */
    public String get_Text1() {
        return this._Text1;
    }

    /**
	 * Class member corresponding to the field 'CashPublicAssistance' in the PDF.
	 */
    private String _CashPublicAssistance = "";

    /**
	 * Mutator Method for xCashPublicAssistance
	 * @param CashPublicAssistance the new value for 'CashPublicAssistance'
	 */
    public void set_CashPublicAssistance(String _CashPublicAssistance) {
        this._CashPublicAssistance = _CashPublicAssistance;
    }

    /**
	 * Accessor Method for xCashPublicAssistance
	 * @return the value of 'CashPublicAssistance'
	 */
    public String get_CashPublicAssistance() {
        return this._CashPublicAssistance;
    }

    /**
	 * Class member corresponding to the field 'UnemployWorkersComp' in the PDF.
	 */
    private String _UnemployWorkersComp = "";

    /**
	 * Mutator Method for xUnemployWorkersComp
	 * @param UnemployWorkersComp the new value for 'UnemployWorkersComp'
	 */
    public void set_UnemployWorkersComp(String _UnemployWorkersComp) {
        this._UnemployWorkersComp = _UnemployWorkersComp;
    }

    /**
	 * Accessor Method for xUnemployWorkersComp
	 * @return the value of 'UnemployWorkersComp'
	 */
    public String get_UnemployWorkersComp() {
        return this._UnemployWorkersComp;
    }

    /**
	 * Class member corresponding to the field 'TotalHouseholdIncome' in the PDF.
	 */
    private String _TotalHouseholdIncome = "";

    /**
	 * Mutator Method for xTotalHouseholdIncome
	 * @param TotalHouseholdIncome the new value for 'TotalHouseholdIncome'
	 */
    public void set_TotalHouseholdIncome(String _TotalHouseholdIncome) {
        this._TotalHouseholdIncome = _TotalHouseholdIncome;
    }

    /**
	 * Accessor Method for xTotalHouseholdIncome
	 * @return the value of 'TotalHouseholdIncome'
	 */
    public String get_TotalHouseholdIncome() {
        return this._TotalHouseholdIncome;
    }

    /**
	 * Class member corresponding to the field 'InterestDividends' in the PDF.
	 */
    private String _InterestDividends = "";

    /**
	 * Mutator Method for xInterestDividends
	 * @param InterestDividends the new value for 'InterestDividends'
	 */
    public void set_InterestDividends(String _InterestDividends) {
        this._InterestDividends = _InterestDividends;
    }

    /**
	 * Accessor Method for xInterestDividends
	 * @return the value of 'InterestDividends'
	 */
    public String get_InterestDividends() {
        return this._InterestDividends;
    }

    /**
	 * Class member corresponding to the field 'Pensions' in the PDF.
	 */
    private String _Pensions = "";

    /**
	 * Mutator Method for xPensions
	 * @param Pensions the new value for 'Pensions'
	 */
    public void set_Pensions(String _Pensions) {
        this._Pensions = _Pensions;
    }

    /**
	 * Accessor Method for xPensions
	 * @return the value of 'Pensions'
	 */
    public String get_Pensions() {
        return this._Pensions;
    }

    /**
	 * Class member corresponding to the field 'AdjustedIncome' in the PDF.
	 */
    private String _AdjustedIncome = "";

    /**
	 * Mutator Method for xAdjustedIncome
	 * @param AdjustedIncome the new value for 'AdjustedIncome'
	 */
    public void set_AdjustedIncome(String _AdjustedIncome) {
        this._AdjustedIncome = _AdjustedIncome;
    }

    /**
	 * Accessor Method for xAdjustedIncome
	 * @return the value of 'AdjustedIncome'
	 */
    public String get_AdjustedIncome() {
        return this._AdjustedIncome;
    }

    /**
	 * Class member corresponding to the field 'BusinessIncome' in the PDF.
	 */
    private String _BusinessIncome = "";

    /**
	 * Mutator Method for xBusinessIncome
	 * @param BusinessIncome the new value for 'BusinessIncome'
	 */
    public void set_BusinessIncome(String _BusinessIncome) {
        this._BusinessIncome = _BusinessIncome;
    }

    /**
	 * Accessor Method for xBusinessIncome
	 * @return the value of 'BusinessIncome'
	 */
    public String get_BusinessIncome() {
        return this._BusinessIncome;
    }

    /**
	 * Class member corresponding to the field 'IncomeUSObligations' in the PDF.
	 */
    private String _IncomeUSObligations = "";

    /**
	 * Mutator Method for xIncomeUSObligations
	 * @param IncomeUSObligations the new value for 'IncomeUSObligations'
	 */
    public void set_IncomeUSObligations(String _IncomeUSObligations) {
        this._IncomeUSObligations = _IncomeUSObligations;
    }

    /**
	 * Accessor Method for xIncomeUSObligations
	 * @return the value of 'IncomeUSObligations'
	 */
    public String get_IncomeUSObligations() {
        return this._IncomeUSObligations;
    }

    /**
	 * Class member corresponding to the field 'TotalIncome' in the PDF.
	 */
    private String _TotalIncome = "";

    /**
	 * Mutator Method for xTotalIncome
	 * @param TotalIncome the new value for 'TotalIncome'
	 */
    public void set_TotalIncome(String _TotalIncome) {
        this._TotalIncome = _TotalIncome;
    }

    /**
	 * Accessor Method for xTotalIncome
	 * @return the value of 'TotalIncome'
	 */
    public String get_TotalIncome() {
        return this._TotalIncome;
    }

    /**
	 * Class member corresponding to the field 'OtherIncome' in the PDF.
	 */
    private String _OtherIncome = "";

    /**
	 * Mutator Method for xOtherIncome
	 * @param OtherIncome the new value for 'OtherIncome'
	 */
    public void set_OtherIncome(String _OtherIncome) {
        this._OtherIncome = _OtherIncome;
    }

    /**
	 * Accessor Method for xOtherIncome
	 * @return the value of 'OtherIncome'
	 */
    public String get_OtherIncome() {
        return this._OtherIncome;
    }

    /**
	 * Class member corresponding to the field 'Text117' in the PDF.
	 */
    private String _Text117 = "";

    /**
	 * Mutator Method for xText117
	 * @param Text117 the new value for 'Text117'
	 */
    public void set_Text117(String _Text117) {
        this._Text117 = _Text117;
    }

    /**
	 * Accessor Method for xText117
	 * @return the value of 'Text117'
	 */
    public String get_Text117() {
        return this._Text117;
    }

    /**
	 * Class member corresponding to the field 'FederalAdjustments' in the PDF.
	 */
    private String _FederalAdjustments = "";

    /**
	 * Mutator Method for xFederalAdjustments
	 * @param FederalAdjustments the new value for 'FederalAdjustments'
	 */
    public void set_FederalAdjustments(String _FederalAdjustments) {
        this._FederalAdjustments = _FederalAdjustments;
    }

    /**
	 * Accessor Method for xFederalAdjustments
	 * @return the value of 'FederalAdjustments'
	 */
    public String get_FederalAdjustments() {
        return this._FederalAdjustments;
    }

    /**
	 * Class member corresponding to the field 'AlimonySupport' in the PDF.
	 */
    private String _AlimonySupport = "";

    /**
	 * Mutator Method for xAlimonySupport
	 * @param AlimonySupport the new value for 'AlimonySupport'
	 */
    public void set_AlimonySupport(String _AlimonySupport) {
        this._AlimonySupport = _AlimonySupport;
    }

    /**
	 * Accessor Method for xAlimonySupport
	 * @return the value of 'AlimonySupport'
	 */
    public String get_AlimonySupport() {
        return this._AlimonySupport;
    }

    /**
	 * Class member corresponding to the field 'Text118' in the PDF.
	 */
    private String _Text118 = "";

    /**
	 * Mutator Method for xText118
	 * @param Text118 the new value for 'Text118'
	 */
    public void set_Text118(String _Text118) {
        this._Text118 = _Text118;
    }

    /**
	 * Accessor Method for xText118
	 * @return the value of 'Text118'
	 */
    public String get_Text118() {
        return this._Text118;
    }

    /**
	 * Class member corresponding to the field 'Text119' in the PDF.
	 */
    private String _Text119 = "";

    /**
	 * Mutator Method for xText119
	 * @param Text119 the new value for 'Text119'
	 */
    public void set_Text119(String _Text119) {
        this._Text119 = _Text119;
    }

    /**
	 * Accessor Method for xText119
	 * @return the value of 'Text119'
	 */
    public String get_Text119() {
        return this._Text119;
    }

    /**
	 * Class member corresponding to the field 'SocialSecurityRailRoadBenefits' in the PDF.
	 */
    private String _SocialSecurityRailRoadBenefits = "";

    /**
	 * Mutator Method for xSocialSecurityRailRoadBenefits
	 * @param SocialSecurityRailRoadBenefits the new value for 'SocialSecurityRailRoadBenefits'
	 */
    public void set_SocialSecurityRailRoadBenefits(String _SocialSecurityRailRoadBenefits) {
        this._SocialSecurityRailRoadBenefits = _SocialSecurityRailRoadBenefits;
    }

    /**
	 * Accessor Method for xSocialSecurityRailRoadBenefits
	 * @return the value of 'SocialSecurityRailRoadBenefits'
	 */
    public String get_SocialSecurityRailRoadBenefits() {
        return this._SocialSecurityRailRoadBenefits;
    }

    /**
	 * Class member corresponding to the field 'ChildSupportPaid' in the PDF.
	 */
    private String _ChildSupportPaid = "";

    /**
	 * Mutator Method for xChildSupportPaid
	 * @param ChildSupportPaid the new value for 'ChildSupportPaid'
	 */
    public void set_ChildSupportPaid(String _ChildSupportPaid) {
        this._ChildSupportPaid = _ChildSupportPaid;
    }

    /**
	 * Accessor Method for xChildSupportPaid
	 * @return the value of 'ChildSupportPaid'
	 */
    public String get_ChildSupportPaid() {
        return this._ChildSupportPaid;
    }

    /**
	 * Class member corresponding to the field 'TotalAdjustments' in the PDF.
	 */
    private String _TotalAdjustments = "";

    /**
	 * Mutator Method for xTotalAdjustments
	 * @param TotalAdjustments the new value for 'TotalAdjustments'
	 */
    public void set_TotalAdjustments(String _TotalAdjustments) {
        this._TotalAdjustments = _TotalAdjustments;
    }

    /**
	 * Accessor Method for xTotalAdjustments
	 * @return the value of 'TotalAdjustments'
	 */
    public String get_TotalAdjustments() {
        return this._TotalAdjustments;
    }

    /**
	 * Class member corresponding to the field 'FarmPartnerships' in the PDF.
	 */
    private String _FarmPartnerships = "";

    /**
	 * Mutator Method for xFarmPartnerships
	 * @param FarmPartnerships the new value for 'FarmPartnerships'
	 */
    public void set_FarmPartnerships(String _FarmPartnerships) {
        this._FarmPartnerships = _FarmPartnerships;
    }

    /**
	 * Accessor Method for xFarmPartnerships
	 * @return the value of 'FarmPartnerships'
	 */
    public String get_FarmPartnerships() {
        return this._FarmPartnerships;
    }

    /**
	 * Class member corresponding to the field 'OtherPersonsNameSSN' in the PDF.
	 */
    private String _OtherPersonsNameSSN = "";

    /**
	 * Mutator Method for xOtherPersonsNameSSN
	 * @param OtherPersonsNameSSN the new value for 'OtherPersonsNameSSN'
	 */
    public void set_OtherPersonsNameSSN(String _OtherPersonsNameSSN) {
        this._OtherPersonsNameSSN = _OtherPersonsNameSSN;
    }

    /**
	 * Accessor Method for xOtherPersonsNameSSN
	 * @return the value of 'OtherPersonsNameSSN'
	 */
    public String get_OtherPersonsNameSSN() {
        return this._OtherPersonsNameSSN;
    }

    /**
	 * Class member corresponding to the field 'RentalIncome' in the PDF.
	 */
    private String _RentalIncome = "";

    /**
	 * Mutator Method for xRentalIncome
	 * @param RentalIncome the new value for 'RentalIncome'
	 */
    public void set_RentalIncome(String _RentalIncome) {
        this._RentalIncome = _RentalIncome;
    }

    /**
	 * Accessor Method for xRentalIncome
	 * @return the value of 'RentalIncome'
	 */
    public String get_RentalIncome() {
        return this._RentalIncome;
    }

    /**
	 * Class member corresponding to the field 'CapitalGain' in the PDF.
	 */
    private String _CapitalGain = "";

    /**
	 * Mutator Method for xCapitalGain
	 * @param CapitalGain the new value for 'CapitalGain'
	 */
    public void set_CapitalGain(String _CapitalGain) {
        this._CapitalGain = _CapitalGain;
    }

    /**
	 * Accessor Method for xCapitalGain
	 * @return the value of 'CapitalGain'
	 */
    public String get_CapitalGain() {
        return this._CapitalGain;
    }

    /**
	 * Class member corresponding to the field 'Wages' in the PDF.
	 */
    private String _Wages = "";

    /**
	 * Mutator Method for xWages
	 * @param Wages the new value for 'Wages'
	 */
    public void set_Wages(String _Wages) {
        this._Wages = _Wages;
    }

    /**
	 * Accessor Method for xWages
	 * @return the value of 'Wages'
	 */
    public String get_Wages() {
        return this._Wages;
    }

    /**
	 * Class member corresponding to the field 'SSMedicareWithheld' in the PDF.
	 */
    private String _SSMedicareWithheld = "";

    /**
	 * Mutator Method for xSSMedicareWithheld
	 * @param SSMedicareWithheld the new value for 'SSMedicareWithheld'
	 */
    public void set_SSMedicareWithheld(String _SSMedicareWithheld) {
        this._SSMedicareWithheld = _SSMedicareWithheld;
    }

    /**
	 * Accessor Method for xSSMedicareWithheld
	 * @return the value of 'SSMedicareWithheld'
	 */
    public String get_SSMedicareWithheld() {
        return this._SSMedicareWithheld;
    }
}
