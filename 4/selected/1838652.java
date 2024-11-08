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
 * Generated class for filling 'TY2009_VT_IN112.pdf'
 */
public class TY2009_VT_IN112 {

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
        fields.setFieldProperty("AngelVenture CurrentYearContribution", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AngelVenture CurrentYearContribution", "textfont", baseFont, null);
        fields.setField("AngelVenture CurrentYearContribution", this.get_AngelVenture_CurrentYearContribution());
        fields.setFieldProperty("ElderlyDisabledCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ElderlyDisabledCredit", "textfont", baseFont, null);
        fields.setField("ElderlyDisabledCredit", this.get_ElderlyDisabledCredit());
        fields.setFieldProperty("TotalAdditionsToVtTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalAdditionsToVtTax", "textfont", baseFont, null);
        fields.setField("TotalAdditionsToVtTax", this.get_TotalAdditionsToVtTax());
        fields.setFieldProperty("AgeQuestion", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AgeQuestion", "textfont", baseFont, null);
        fields.setField("AgeQuestion", this.get_AgeQuestion());
        fields.setFieldProperty("FEINOfEntity", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FEINOfEntity", "textfont", baseFont, null);
        fields.setField("FEINOfEntity", this.get_FEINOfEntity());
        fields.setFieldProperty("VtHighEdInvestment CreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VtHighEdInvestment CreditAmount", "textfont", baseFont, null);
        fields.setField("VtHighEdInvestment CreditAmount", this.get_VtHighEdInvestment_CreditAmount());
        fields.setFieldProperty("CreditTaxPaidOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditTaxPaidOtherState", "textfont", baseFont, null);
        fields.setField("CreditTaxPaidOtherState", this.get_CreditTaxPaidOtherState());
        fields.setFieldProperty("FedForm4972Tax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FedForm4972Tax", "textfont", baseFont, null);
        fields.setField("FedForm4972Tax", this.get_FedForm4972Tax());
        fields.setFieldProperty("AdjustedGrossIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AdjustedGrossIncome", "textfont", baseFont, null);
        fields.setField("AdjustedGrossIncome", this.get_AdjustedGrossIncome());
        fields.setFieldProperty("QualifiedSaleMobileHomePark CreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("QualifiedSaleMobileHomePark CreditAmount", "textfont", baseFont, null);
        fields.setField("QualifiedSaleMobileHomePark CreditAmount", this.get_QualifiedSaleMobileHomePark_CreditAmount());
        fields.setFieldProperty("SubtractoinsFromVtTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SubtractoinsFromVtTax", "textfont", baseFont, null);
        fields.setField("SubtractoinsFromVtTax", this.get_SubtractoinsFromVtTax());
        fields.setFieldProperty("VTSubtractionSubtotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTSubtractionSubtotal", "textfont", baseFont, null);
        fields.setField("VTSubtractionSubtotal", this.get_VTSubtractionSubtotal());
        fields.setFieldProperty("RecaptureVtCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RecaptureVtCredits", "textfont", baseFont, null);
        fields.setField("RecaptureVtCredits", this.get_RecaptureVtCredits());
        fields.setFieldProperty("QualifiedPlansTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("QualifiedPlansTax", "textfont", baseFont, null);
        fields.setField("QualifiedPlansTax", this.get_QualifiedPlansTax());
        fields.setFieldProperty("EICFederalAmt", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EICFederalAmt", "textfont", baseFont, null);
        fields.setField("EICFederalAmt", this.get_EICFederalAmt());
        fields.setFieldProperty("VTBusSolarEnergyCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTBusSolarEnergyCredit", "textfont", baseFont, null);
        fields.setField("VTBusSolarEnergyCredit", this.get_VTBusSolarEnergyCredit());
        fields.setFieldProperty("VermontTotalEarnedIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VermontTotalEarnedIncome", "textfont", baseFont, null);
        fields.setField("VermontTotalEarnedIncome", this.get_VermontTotalEarnedIncome());
        fields.setFieldProperty("RecaptureFederalInvCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RecaptureFederalInvCredit", "textfont", baseFont, null);
        fields.setField("RecaptureFederalInvCredit", this.get_RecaptureFederalInvCredit());
        fields.setFieldProperty("TotalSubtractoinsFromVtTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalSubtractoinsFromVtTax", "textfont", baseFont, null);
        fields.setField("TotalSubtractoinsFromVtTax", this.get_TotalSubtractoinsFromVtTax());
        fields.setFieldProperty("CharitableHousing CarryForward", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CharitableHousing CarryForward", "textfont", baseFont, null);
        fields.setField("CharitableHousing CarryForward", this.get_CharitableHousing_CarryForward());
        fields.setFieldProperty("ComputedTaxCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ComputedTaxCredit", "textfont", baseFont, null);
        fields.setField("ComputedTaxCredit", this.get_ComputedTaxCredit());
        fields.setFieldProperty("VtHighEdInvestment CurrentYearContribution", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VtHighEdInvestment CurrentYearContribution", "textfont", baseFont, null);
        fields.setField("VtHighEdInvestment CurrentYearContribution", this.get_VtHighEdInvestment_CurrentYearContribution());
        fields.setFieldProperty("CharitableHousing Earned", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CharitableHousing Earned", "textfont", baseFont, null);
        fields.setField("CharitableHousing Earned", this.get_CharitableHousing_Earned());
        fields.setFieldProperty("Text232", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text232", "textfont", baseFont, null);
        fields.setField("Text232", this.get_Text232());
        fields.setFieldProperty("NameOfEntity", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NameOfEntity", "textfont", baseFont, null);
        fields.setField("NameOfEntity", this.get_NameOfEntity());
        fields.setFieldProperty("BusinessSolarEnergyNewCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessSolarEnergyNewCredit", "textfont", baseFont, null);
        fields.setField("BusinessSolarEnergyNewCredit", this.get_BusinessSolarEnergyNewCredit());
        fields.setFieldProperty("IncomeTaxableToBoth", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IncomeTaxableToBoth", "textfont", baseFont, null);
        fields.setField("IncomeTaxableToBoth", this.get_IncomeTaxableToBoth());
        fields.setFieldProperty("NetTaxPaidToOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NetTaxPaidToOtherState", "textfont", baseFont, null);
        fields.setField("NetTaxPaidToOtherState", this.get_NetTaxPaidToOtherState());
        fields.setFieldProperty("FarmIncAveragingCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FarmIncAveragingCredit", "textfont", baseFont, null);
        fields.setField("FarmIncAveragingCredit", this.get_FarmIncAveragingCredit());
        fields.setFieldProperty("AdditionsSubTotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AdditionsSubTotal", "textfont", baseFont, null);
        fields.setField("AdditionsSubTotal", this.get_AdditionsSubTotal());
        fields.setFieldProperty("VTPortionWages", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTPortionWages", "textfont", baseFont, null);
        fields.setField("VTPortionWages", this.get_VTPortionWages());
        fields.setFieldProperty("NameOtherState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NameOtherState", "textfont", baseFont, null);
        fields.setField("NameOtherState", this.get_NameOtherState());
        fields.setFieldProperty("QualifiedSaleMobileHomePark CarryForward", "textsize", new Float(20.2), null);
        fields.setFieldProperty("QualifiedSaleMobileHomePark CarryForward", "textfont", baseFont, null);
        fields.setField("QualifiedSaleMobileHomePark CarryForward", this.get_QualifiedSaleMobileHomePark_CarryForward());
        fields.setFieldProperty("ChildAndDependentCareExp", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ChildAndDependentCareExp", "textfont", baseFont, null);
        fields.setField("ChildAndDependentCareExp", this.get_ChildAndDependentCareExp());
        fields.setFieldProperty("EICAdjustment", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EICAdjustment", "textfont", baseFont, null);
        fields.setField("EICAdjustment", this.get_EICAdjustment());
        fields.setFieldProperty("CreditTotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditTotal", "textfont", baseFont, null);
        fields.setField("CreditTotal", this.get_CreditTotal());
        fields.setFieldProperty("VTLocalStateObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTLocalStateObligations", "textfont", baseFont, null);
        fields.setField("VTLocalStateObligations", this.get_VTLocalStateObligations());
        fields.setFieldProperty("VTAdditions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTAdditions", "textfont", baseFont, null);
        fields.setField("VTAdditions", this.get_VTAdditions());
        fields.setFieldProperty("FederalWages", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalWages", "textfont", baseFont, null);
        fields.setField("FederalWages", this.get_FederalWages());
        fields.setFieldProperty("EarnedIncomeCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EarnedIncomeCredit", "textfont", baseFont, null);
        fields.setField("EarnedIncomeCredit", this.get_EarnedIncomeCredit());
        fields.setFieldProperty("NonVTLocalStateObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NonVTLocalStateObligations", "textfont", baseFont, null);
        fields.setField("NonVTLocalStateObligations", this.get_NonVTLocalStateObligations());
        fields.setFieldProperty("TotalLocalStateObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalLocalStateObligations", "textfont", baseFont, null);
        fields.setField("TotalLocalStateObligations", this.get_TotalLocalStateObligations());
        fields.setFieldProperty("EICNumQualChildren", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EICNumQualChildren", "textfont", baseFont, null);
        fields.setField("EICNumQualChildren", this.get_EICNumQualChildren());
        fields.setFieldProperty("QualifiedSaleMobileHomePark Earned", "textsize", new Float(20.2), null);
        fields.setFieldProperty("QualifiedSaleMobileHomePark Earned", "textfont", baseFont, null);
        fields.setField("QualifiedSaleMobileHomePark Earned", this.get_QualifiedSaleMobileHomePark_Earned());
        fields.setFieldProperty("VermontOtherEarnedIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VermontOtherEarnedIncome", "textfont", baseFont, null);
        fields.setField("VermontOtherEarnedIncome", this.get_VermontOtherEarnedIncome());
        fields.setFieldProperty("CharitableHousing CreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CharitableHousing CreditAmount", "textfont", baseFont, null);
        fields.setField("CharitableHousing CreditAmount", this.get_CharitableHousing_CreditAmount());
        fields.setFieldProperty("AngelVenture CreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AngelVenture CreditAmount", "textfont", baseFont, null);
        fields.setField("AngelVenture CreditAmount", this.get_AngelVenture_CreditAmount());
        fields.setFieldProperty("FederalTotalEarnedIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalTotalEarnedIncome", "textfont", baseFont, null);
        fields.setField("FederalTotalEarnedIncome", this.get_FederalTotalEarnedIncome());
        fields.setFieldProperty("InvestmentTaxCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("InvestmentTaxCredit", "textfont", baseFont, null);
        fields.setField("InvestmentTaxCredit", this.get_InvestmentTaxCredit());
        fields.setFieldProperty("VTIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTIncomeTax", "textfont", baseFont, null);
        fields.setField("VTIncomeTax", this.get_VTIncomeTax());
        fields.setFieldProperty("BusinessSolarEnergyCarryforward", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessSolarEnergyCarryforward", "textfont", baseFont, null);
        fields.setField("BusinessSolarEnergyCarryforward", this.get_BusinessSolarEnergyCarryforward());
        fields.setFieldProperty("VTEICCreditSubtotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTEICCreditSubtotal", "textfont", baseFont, null);
        fields.setField("VTEICCreditSubtotal", this.get_VTEICCreditSubtotal());
        fields.setFieldProperty("CommercialFilm CreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CommercialFilm CreditAmount", "textfont", baseFont, null);
        fields.setField("CommercialFilm CreditAmount", this.get_CommercialFilm_CreditAmount());
        fields.setFieldProperty("CommercialFilm Earned", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CommercialFilm Earned", "textfont", baseFont, null);
        fields.setField("CommercialFilm Earned", this.get_CommercialFilm_Earned());
        fields.setFieldProperty("FederalOtherEarnedIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalOtherEarnedIncome", "textfont", baseFont, null);
        fields.setField("FederalOtherEarnedIncome", this.get_FederalOtherEarnedIncome());
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
	 * Class member corresponding to the field 'AngelVenture CurrentYearContribution' in the PDF.
	 */
    private String _AngelVenture_CurrentYearContribution = "";

    /**
	 * Mutator Method for xAngelVenture CurrentYearContribution
	 * @param AngelVenture CurrentYearContribution the new value for 'AngelVenture CurrentYearContribution'
	 */
    public void set_AngelVenture_CurrentYearContribution(String _AngelVenture_CurrentYearContribution) {
        this._AngelVenture_CurrentYearContribution = _AngelVenture_CurrentYearContribution;
    }

    /**
	 * Accessor Method for xAngelVenture CurrentYearContribution
	 * @return the value of 'AngelVenture CurrentYearContribution'
	 */
    public String get_AngelVenture_CurrentYearContribution() {
        return this._AngelVenture_CurrentYearContribution;
    }

    /**
	 * Class member corresponding to the field 'ElderlyDisabledCredit' in the PDF.
	 */
    private String _ElderlyDisabledCredit = "";

    /**
	 * Mutator Method for xElderlyDisabledCredit
	 * @param ElderlyDisabledCredit the new value for 'ElderlyDisabledCredit'
	 */
    public void set_ElderlyDisabledCredit(String _ElderlyDisabledCredit) {
        this._ElderlyDisabledCredit = _ElderlyDisabledCredit;
    }

    /**
	 * Accessor Method for xElderlyDisabledCredit
	 * @return the value of 'ElderlyDisabledCredit'
	 */
    public String get_ElderlyDisabledCredit() {
        return this._ElderlyDisabledCredit;
    }

    /**
	 * Class member corresponding to the field 'TotalAdditionsToVtTax' in the PDF.
	 */
    private String _TotalAdditionsToVtTax = "";

    /**
	 * Mutator Method for xTotalAdditionsToVtTax
	 * @param TotalAdditionsToVtTax the new value for 'TotalAdditionsToVtTax'
	 */
    public void set_TotalAdditionsToVtTax(String _TotalAdditionsToVtTax) {
        this._TotalAdditionsToVtTax = _TotalAdditionsToVtTax;
    }

    /**
	 * Accessor Method for xTotalAdditionsToVtTax
	 * @return the value of 'TotalAdditionsToVtTax'
	 */
    public String get_TotalAdditionsToVtTax() {
        return this._TotalAdditionsToVtTax;
    }

    /**
	 * Class member corresponding to the field 'AgeQuestion' in the PDF.
	 */
    private String _AgeQuestion = "";

    /**
	 * Mutator Method for xAgeQuestion
	 * @param AgeQuestion the new value for 'AgeQuestion'
	 */
    public void set_AgeQuestion(String _AgeQuestion) {
        this._AgeQuestion = _AgeQuestion;
    }

    /**
	 * Accessor Method for xAgeQuestion
	 * @return the value of 'AgeQuestion'
	 */
    public String get_AgeQuestion() {
        return this._AgeQuestion;
    }

    /**
	 * Class member corresponding to the field 'FEINOfEntity' in the PDF.
	 */
    private String _FEINOfEntity = "";

    /**
	 * Mutator Method for xFEINOfEntity
	 * @param FEINOfEntity the new value for 'FEINOfEntity'
	 */
    public void set_FEINOfEntity(String _FEINOfEntity) {
        this._FEINOfEntity = _FEINOfEntity;
    }

    /**
	 * Accessor Method for xFEINOfEntity
	 * @return the value of 'FEINOfEntity'
	 */
    public String get_FEINOfEntity() {
        return this._FEINOfEntity;
    }

    /**
	 * Class member corresponding to the field 'VtHighEdInvestment CreditAmount' in the PDF.
	 */
    private String _VtHighEdInvestment_CreditAmount = "";

    /**
	 * Mutator Method for xVtHighEdInvestment CreditAmount
	 * @param VtHighEdInvestment CreditAmount the new value for 'VtHighEdInvestment CreditAmount'
	 */
    public void set_VtHighEdInvestment_CreditAmount(String _VtHighEdInvestment_CreditAmount) {
        this._VtHighEdInvestment_CreditAmount = _VtHighEdInvestment_CreditAmount;
    }

    /**
	 * Accessor Method for xVtHighEdInvestment CreditAmount
	 * @return the value of 'VtHighEdInvestment CreditAmount'
	 */
    public String get_VtHighEdInvestment_CreditAmount() {
        return this._VtHighEdInvestment_CreditAmount;
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
	 * Class member corresponding to the field 'FedForm4972Tax' in the PDF.
	 */
    private String _FedForm4972Tax = "";

    /**
	 * Mutator Method for xFedForm4972Tax
	 * @param FedForm4972Tax the new value for 'FedForm4972Tax'
	 */
    public void set_FedForm4972Tax(String _FedForm4972Tax) {
        this._FedForm4972Tax = _FedForm4972Tax;
    }

    /**
	 * Accessor Method for xFedForm4972Tax
	 * @return the value of 'FedForm4972Tax'
	 */
    public String get_FedForm4972Tax() {
        return this._FedForm4972Tax;
    }

    /**
	 * Class member corresponding to the field 'AdjustedGrossIncome' in the PDF.
	 */
    private String _AdjustedGrossIncome = "";

    /**
	 * Mutator Method for xAdjustedGrossIncome
	 * @param AdjustedGrossIncome the new value for 'AdjustedGrossIncome'
	 */
    public void set_AdjustedGrossIncome(String _AdjustedGrossIncome) {
        this._AdjustedGrossIncome = _AdjustedGrossIncome;
    }

    /**
	 * Accessor Method for xAdjustedGrossIncome
	 * @return the value of 'AdjustedGrossIncome'
	 */
    public String get_AdjustedGrossIncome() {
        return this._AdjustedGrossIncome;
    }

    /**
	 * Class member corresponding to the field 'QualifiedSaleMobileHomePark CreditAmount' in the PDF.
	 */
    private String _QualifiedSaleMobileHomePark_CreditAmount = "";

    /**
	 * Mutator Method for xQualifiedSaleMobileHomePark CreditAmount
	 * @param QualifiedSaleMobileHomePark CreditAmount the new value for 'QualifiedSaleMobileHomePark CreditAmount'
	 */
    public void set_QualifiedSaleMobileHomePark_CreditAmount(String _QualifiedSaleMobileHomePark_CreditAmount) {
        this._QualifiedSaleMobileHomePark_CreditAmount = _QualifiedSaleMobileHomePark_CreditAmount;
    }

    /**
	 * Accessor Method for xQualifiedSaleMobileHomePark CreditAmount
	 * @return the value of 'QualifiedSaleMobileHomePark CreditAmount'
	 */
    public String get_QualifiedSaleMobileHomePark_CreditAmount() {
        return this._QualifiedSaleMobileHomePark_CreditAmount;
    }

    /**
	 * Class member corresponding to the field 'SubtractoinsFromVtTax' in the PDF.
	 */
    private String _SubtractoinsFromVtTax = "";

    /**
	 * Mutator Method for xSubtractoinsFromVtTax
	 * @param SubtractoinsFromVtTax the new value for 'SubtractoinsFromVtTax'
	 */
    public void set_SubtractoinsFromVtTax(String _SubtractoinsFromVtTax) {
        this._SubtractoinsFromVtTax = _SubtractoinsFromVtTax;
    }

    /**
	 * Accessor Method for xSubtractoinsFromVtTax
	 * @return the value of 'SubtractoinsFromVtTax'
	 */
    public String get_SubtractoinsFromVtTax() {
        return this._SubtractoinsFromVtTax;
    }

    /**
	 * Class member corresponding to the field 'VTSubtractionSubtotal' in the PDF.
	 */
    private String _VTSubtractionSubtotal = "";

    /**
	 * Mutator Method for xVTSubtractionSubtotal
	 * @param VTSubtractionSubtotal the new value for 'VTSubtractionSubtotal'
	 */
    public void set_VTSubtractionSubtotal(String _VTSubtractionSubtotal) {
        this._VTSubtractionSubtotal = _VTSubtractionSubtotal;
    }

    /**
	 * Accessor Method for xVTSubtractionSubtotal
	 * @return the value of 'VTSubtractionSubtotal'
	 */
    public String get_VTSubtractionSubtotal() {
        return this._VTSubtractionSubtotal;
    }

    /**
	 * Class member corresponding to the field 'RecaptureVtCredits' in the PDF.
	 */
    private String _RecaptureVtCredits = "";

    /**
	 * Mutator Method for xRecaptureVtCredits
	 * @param RecaptureVtCredits the new value for 'RecaptureVtCredits'
	 */
    public void set_RecaptureVtCredits(String _RecaptureVtCredits) {
        this._RecaptureVtCredits = _RecaptureVtCredits;
    }

    /**
	 * Accessor Method for xRecaptureVtCredits
	 * @return the value of 'RecaptureVtCredits'
	 */
    public String get_RecaptureVtCredits() {
        return this._RecaptureVtCredits;
    }

    /**
	 * Class member corresponding to the field 'QualifiedPlansTax' in the PDF.
	 */
    private String _QualifiedPlansTax = "";

    /**
	 * Mutator Method for xQualifiedPlansTax
	 * @param QualifiedPlansTax the new value for 'QualifiedPlansTax'
	 */
    public void set_QualifiedPlansTax(String _QualifiedPlansTax) {
        this._QualifiedPlansTax = _QualifiedPlansTax;
    }

    /**
	 * Accessor Method for xQualifiedPlansTax
	 * @return the value of 'QualifiedPlansTax'
	 */
    public String get_QualifiedPlansTax() {
        return this._QualifiedPlansTax;
    }

    /**
	 * Class member corresponding to the field 'EICFederalAmt' in the PDF.
	 */
    private String _EICFederalAmt = "";

    /**
	 * Mutator Method for xEICFederalAmt
	 * @param EICFederalAmt the new value for 'EICFederalAmt'
	 */
    public void set_EICFederalAmt(String _EICFederalAmt) {
        this._EICFederalAmt = _EICFederalAmt;
    }

    /**
	 * Accessor Method for xEICFederalAmt
	 * @return the value of 'EICFederalAmt'
	 */
    public String get_EICFederalAmt() {
        return this._EICFederalAmt;
    }

    /**
	 * Class member corresponding to the field 'VTBusSolarEnergyCredit' in the PDF.
	 */
    private String _VTBusSolarEnergyCredit = "";

    /**
	 * Mutator Method for xVTBusSolarEnergyCredit
	 * @param VTBusSolarEnergyCredit the new value for 'VTBusSolarEnergyCredit'
	 */
    public void set_VTBusSolarEnergyCredit(String _VTBusSolarEnergyCredit) {
        this._VTBusSolarEnergyCredit = _VTBusSolarEnergyCredit;
    }

    /**
	 * Accessor Method for xVTBusSolarEnergyCredit
	 * @return the value of 'VTBusSolarEnergyCredit'
	 */
    public String get_VTBusSolarEnergyCredit() {
        return this._VTBusSolarEnergyCredit;
    }

    /**
	 * Class member corresponding to the field 'VermontTotalEarnedIncome' in the PDF.
	 */
    private String _VermontTotalEarnedIncome = "";

    /**
	 * Mutator Method for xVermontTotalEarnedIncome
	 * @param VermontTotalEarnedIncome the new value for 'VermontTotalEarnedIncome'
	 */
    public void set_VermontTotalEarnedIncome(String _VermontTotalEarnedIncome) {
        this._VermontTotalEarnedIncome = _VermontTotalEarnedIncome;
    }

    /**
	 * Accessor Method for xVermontTotalEarnedIncome
	 * @return the value of 'VermontTotalEarnedIncome'
	 */
    public String get_VermontTotalEarnedIncome() {
        return this._VermontTotalEarnedIncome;
    }

    /**
	 * Class member corresponding to the field 'RecaptureFederalInvCredit' in the PDF.
	 */
    private String _RecaptureFederalInvCredit = "";

    /**
	 * Mutator Method for xRecaptureFederalInvCredit
	 * @param RecaptureFederalInvCredit the new value for 'RecaptureFederalInvCredit'
	 */
    public void set_RecaptureFederalInvCredit(String _RecaptureFederalInvCredit) {
        this._RecaptureFederalInvCredit = _RecaptureFederalInvCredit;
    }

    /**
	 * Accessor Method for xRecaptureFederalInvCredit
	 * @return the value of 'RecaptureFederalInvCredit'
	 */
    public String get_RecaptureFederalInvCredit() {
        return this._RecaptureFederalInvCredit;
    }

    /**
	 * Class member corresponding to the field 'TotalSubtractoinsFromVtTax' in the PDF.
	 */
    private String _TotalSubtractoinsFromVtTax = "";

    /**
	 * Mutator Method for xTotalSubtractoinsFromVtTax
	 * @param TotalSubtractoinsFromVtTax the new value for 'TotalSubtractoinsFromVtTax'
	 */
    public void set_TotalSubtractoinsFromVtTax(String _TotalSubtractoinsFromVtTax) {
        this._TotalSubtractoinsFromVtTax = _TotalSubtractoinsFromVtTax;
    }

    /**
	 * Accessor Method for xTotalSubtractoinsFromVtTax
	 * @return the value of 'TotalSubtractoinsFromVtTax'
	 */
    public String get_TotalSubtractoinsFromVtTax() {
        return this._TotalSubtractoinsFromVtTax;
    }

    /**
	 * Class member corresponding to the field 'CharitableHousing CarryForward' in the PDF.
	 */
    private String _CharitableHousing_CarryForward = "";

    /**
	 * Mutator Method for xCharitableHousing CarryForward
	 * @param CharitableHousing CarryForward the new value for 'CharitableHousing CarryForward'
	 */
    public void set_CharitableHousing_CarryForward(String _CharitableHousing_CarryForward) {
        this._CharitableHousing_CarryForward = _CharitableHousing_CarryForward;
    }

    /**
	 * Accessor Method for xCharitableHousing CarryForward
	 * @return the value of 'CharitableHousing CarryForward'
	 */
    public String get_CharitableHousing_CarryForward() {
        return this._CharitableHousing_CarryForward;
    }

    /**
	 * Class member corresponding to the field 'ComputedTaxCredit' in the PDF.
	 */
    private String _ComputedTaxCredit = "";

    /**
	 * Mutator Method for xComputedTaxCredit
	 * @param ComputedTaxCredit the new value for 'ComputedTaxCredit'
	 */
    public void set_ComputedTaxCredit(String _ComputedTaxCredit) {
        this._ComputedTaxCredit = _ComputedTaxCredit;
    }

    /**
	 * Accessor Method for xComputedTaxCredit
	 * @return the value of 'ComputedTaxCredit'
	 */
    public String get_ComputedTaxCredit() {
        return this._ComputedTaxCredit;
    }

    /**
	 * Class member corresponding to the field 'VtHighEdInvestment CurrentYearContribution' in the PDF.
	 */
    private String _VtHighEdInvestment_CurrentYearContribution = "";

    /**
	 * Mutator Method for xVtHighEdInvestment CurrentYearContribution
	 * @param VtHighEdInvestment CurrentYearContribution the new value for 'VtHighEdInvestment CurrentYearContribution'
	 */
    public void set_VtHighEdInvestment_CurrentYearContribution(String _VtHighEdInvestment_CurrentYearContribution) {
        this._VtHighEdInvestment_CurrentYearContribution = _VtHighEdInvestment_CurrentYearContribution;
    }

    /**
	 * Accessor Method for xVtHighEdInvestment CurrentYearContribution
	 * @return the value of 'VtHighEdInvestment CurrentYearContribution'
	 */
    public String get_VtHighEdInvestment_CurrentYearContribution() {
        return this._VtHighEdInvestment_CurrentYearContribution;
    }

    /**
	 * Class member corresponding to the field 'CharitableHousing Earned' in the PDF.
	 */
    private String _CharitableHousing_Earned = "";

    /**
	 * Mutator Method for xCharitableHousing Earned
	 * @param CharitableHousing Earned the new value for 'CharitableHousing Earned'
	 */
    public void set_CharitableHousing_Earned(String _CharitableHousing_Earned) {
        this._CharitableHousing_Earned = _CharitableHousing_Earned;
    }

    /**
	 * Accessor Method for xCharitableHousing Earned
	 * @return the value of 'CharitableHousing Earned'
	 */
    public String get_CharitableHousing_Earned() {
        return this._CharitableHousing_Earned;
    }

    /**
	 * Class member corresponding to the field 'Text232' in the PDF.
	 */
    private String _Text232 = "";

    /**
	 * Mutator Method for xText232
	 * @param Text232 the new value for 'Text232'
	 */
    public void set_Text232(String _Text232) {
        this._Text232 = _Text232;
    }

    /**
	 * Accessor Method for xText232
	 * @return the value of 'Text232'
	 */
    public String get_Text232() {
        return this._Text232;
    }

    /**
	 * Class member corresponding to the field 'NameOfEntity' in the PDF.
	 */
    private String _NameOfEntity = "";

    /**
	 * Mutator Method for xNameOfEntity
	 * @param NameOfEntity the new value for 'NameOfEntity'
	 */
    public void set_NameOfEntity(String _NameOfEntity) {
        this._NameOfEntity = _NameOfEntity;
    }

    /**
	 * Accessor Method for xNameOfEntity
	 * @return the value of 'NameOfEntity'
	 */
    public String get_NameOfEntity() {
        return this._NameOfEntity;
    }

    /**
	 * Class member corresponding to the field 'BusinessSolarEnergyNewCredit' in the PDF.
	 */
    private String _BusinessSolarEnergyNewCredit = "";

    /**
	 * Mutator Method for xBusinessSolarEnergyNewCredit
	 * @param BusinessSolarEnergyNewCredit the new value for 'BusinessSolarEnergyNewCredit'
	 */
    public void set_BusinessSolarEnergyNewCredit(String _BusinessSolarEnergyNewCredit) {
        this._BusinessSolarEnergyNewCredit = _BusinessSolarEnergyNewCredit;
    }

    /**
	 * Accessor Method for xBusinessSolarEnergyNewCredit
	 * @return the value of 'BusinessSolarEnergyNewCredit'
	 */
    public String get_BusinessSolarEnergyNewCredit() {
        return this._BusinessSolarEnergyNewCredit;
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
	 * Class member corresponding to the field 'FarmIncAveragingCredit' in the PDF.
	 */
    private String _FarmIncAveragingCredit = "";

    /**
	 * Mutator Method for xFarmIncAveragingCredit
	 * @param FarmIncAveragingCredit the new value for 'FarmIncAveragingCredit'
	 */
    public void set_FarmIncAveragingCredit(String _FarmIncAveragingCredit) {
        this._FarmIncAveragingCredit = _FarmIncAveragingCredit;
    }

    /**
	 * Accessor Method for xFarmIncAveragingCredit
	 * @return the value of 'FarmIncAveragingCredit'
	 */
    public String get_FarmIncAveragingCredit() {
        return this._FarmIncAveragingCredit;
    }

    /**
	 * Class member corresponding to the field 'AdditionsSubTotal' in the PDF.
	 */
    private String _AdditionsSubTotal = "";

    /**
	 * Mutator Method for xAdditionsSubTotal
	 * @param AdditionsSubTotal the new value for 'AdditionsSubTotal'
	 */
    public void set_AdditionsSubTotal(String _AdditionsSubTotal) {
        this._AdditionsSubTotal = _AdditionsSubTotal;
    }

    /**
	 * Accessor Method for xAdditionsSubTotal
	 * @return the value of 'AdditionsSubTotal'
	 */
    public String get_AdditionsSubTotal() {
        return this._AdditionsSubTotal;
    }

    /**
	 * Class member corresponding to the field 'VTPortionWages' in the PDF.
	 */
    private String _VTPortionWages = "";

    /**
	 * Mutator Method for xVTPortionWages
	 * @param VTPortionWages the new value for 'VTPortionWages'
	 */
    public void set_VTPortionWages(String _VTPortionWages) {
        this._VTPortionWages = _VTPortionWages;
    }

    /**
	 * Accessor Method for xVTPortionWages
	 * @return the value of 'VTPortionWages'
	 */
    public String get_VTPortionWages() {
        return this._VTPortionWages;
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
	 * Class member corresponding to the field 'QualifiedSaleMobileHomePark CarryForward' in the PDF.
	 */
    private String _QualifiedSaleMobileHomePark_CarryForward = "";

    /**
	 * Mutator Method for xQualifiedSaleMobileHomePark CarryForward
	 * @param QualifiedSaleMobileHomePark CarryForward the new value for 'QualifiedSaleMobileHomePark CarryForward'
	 */
    public void set_QualifiedSaleMobileHomePark_CarryForward(String _QualifiedSaleMobileHomePark_CarryForward) {
        this._QualifiedSaleMobileHomePark_CarryForward = _QualifiedSaleMobileHomePark_CarryForward;
    }

    /**
	 * Accessor Method for xQualifiedSaleMobileHomePark CarryForward
	 * @return the value of 'QualifiedSaleMobileHomePark CarryForward'
	 */
    public String get_QualifiedSaleMobileHomePark_CarryForward() {
        return this._QualifiedSaleMobileHomePark_CarryForward;
    }

    /**
	 * Class member corresponding to the field 'ChildAndDependentCareExp' in the PDF.
	 */
    private String _ChildAndDependentCareExp = "";

    /**
	 * Mutator Method for xChildAndDependentCareExp
	 * @param ChildAndDependentCareExp the new value for 'ChildAndDependentCareExp'
	 */
    public void set_ChildAndDependentCareExp(String _ChildAndDependentCareExp) {
        this._ChildAndDependentCareExp = _ChildAndDependentCareExp;
    }

    /**
	 * Accessor Method for xChildAndDependentCareExp
	 * @return the value of 'ChildAndDependentCareExp'
	 */
    public String get_ChildAndDependentCareExp() {
        return this._ChildAndDependentCareExp;
    }

    /**
	 * Class member corresponding to the field 'EICAdjustment' in the PDF.
	 */
    private String _EICAdjustment = "";

    /**
	 * Mutator Method for xEICAdjustment
	 * @param EICAdjustment the new value for 'EICAdjustment'
	 */
    public void set_EICAdjustment(String _EICAdjustment) {
        this._EICAdjustment = _EICAdjustment;
    }

    /**
	 * Accessor Method for xEICAdjustment
	 * @return the value of 'EICAdjustment'
	 */
    public String get_EICAdjustment() {
        return this._EICAdjustment;
    }

    /**
	 * Class member corresponding to the field 'CreditTotal' in the PDF.
	 */
    private String _CreditTotal = "";

    /**
	 * Mutator Method for xCreditTotal
	 * @param CreditTotal the new value for 'CreditTotal'
	 */
    public void set_CreditTotal(String _CreditTotal) {
        this._CreditTotal = _CreditTotal;
    }

    /**
	 * Accessor Method for xCreditTotal
	 * @return the value of 'CreditTotal'
	 */
    public String get_CreditTotal() {
        return this._CreditTotal;
    }

    /**
	 * Class member corresponding to the field 'VTLocalStateObligations' in the PDF.
	 */
    private String _VTLocalStateObligations = "";

    /**
	 * Mutator Method for xVTLocalStateObligations
	 * @param VTLocalStateObligations the new value for 'VTLocalStateObligations'
	 */
    public void set_VTLocalStateObligations(String _VTLocalStateObligations) {
        this._VTLocalStateObligations = _VTLocalStateObligations;
    }

    /**
	 * Accessor Method for xVTLocalStateObligations
	 * @return the value of 'VTLocalStateObligations'
	 */
    public String get_VTLocalStateObligations() {
        return this._VTLocalStateObligations;
    }

    /**
	 * Class member corresponding to the field 'VTAdditions' in the PDF.
	 */
    private String _VTAdditions = "";

    /**
	 * Mutator Method for xVTAdditions
	 * @param VTAdditions the new value for 'VTAdditions'
	 */
    public void set_VTAdditions(String _VTAdditions) {
        this._VTAdditions = _VTAdditions;
    }

    /**
	 * Accessor Method for xVTAdditions
	 * @return the value of 'VTAdditions'
	 */
    public String get_VTAdditions() {
        return this._VTAdditions;
    }

    /**
	 * Class member corresponding to the field 'FederalWages' in the PDF.
	 */
    private String _FederalWages = "";

    /**
	 * Mutator Method for xFederalWages
	 * @param FederalWages the new value for 'FederalWages'
	 */
    public void set_FederalWages(String _FederalWages) {
        this._FederalWages = _FederalWages;
    }

    /**
	 * Accessor Method for xFederalWages
	 * @return the value of 'FederalWages'
	 */
    public String get_FederalWages() {
        return this._FederalWages;
    }

    /**
	 * Class member corresponding to the field 'EarnedIncomeCredit' in the PDF.
	 */
    private String _EarnedIncomeCredit = "";

    /**
	 * Mutator Method for xEarnedIncomeCredit
	 * @param EarnedIncomeCredit the new value for 'EarnedIncomeCredit'
	 */
    public void set_EarnedIncomeCredit(String _EarnedIncomeCredit) {
        this._EarnedIncomeCredit = _EarnedIncomeCredit;
    }

    /**
	 * Accessor Method for xEarnedIncomeCredit
	 * @return the value of 'EarnedIncomeCredit'
	 */
    public String get_EarnedIncomeCredit() {
        return this._EarnedIncomeCredit;
    }

    /**
	 * Class member corresponding to the field 'NonVTLocalStateObligations' in the PDF.
	 */
    private String _NonVTLocalStateObligations = "";

    /**
	 * Mutator Method for xNonVTLocalStateObligations
	 * @param NonVTLocalStateObligations the new value for 'NonVTLocalStateObligations'
	 */
    public void set_NonVTLocalStateObligations(String _NonVTLocalStateObligations) {
        this._NonVTLocalStateObligations = _NonVTLocalStateObligations;
    }

    /**
	 * Accessor Method for xNonVTLocalStateObligations
	 * @return the value of 'NonVTLocalStateObligations'
	 */
    public String get_NonVTLocalStateObligations() {
        return this._NonVTLocalStateObligations;
    }

    /**
	 * Class member corresponding to the field 'TotalLocalStateObligations' in the PDF.
	 */
    private String _TotalLocalStateObligations = "";

    /**
	 * Mutator Method for xTotalLocalStateObligations
	 * @param TotalLocalStateObligations the new value for 'TotalLocalStateObligations'
	 */
    public void set_TotalLocalStateObligations(String _TotalLocalStateObligations) {
        this._TotalLocalStateObligations = _TotalLocalStateObligations;
    }

    /**
	 * Accessor Method for xTotalLocalStateObligations
	 * @return the value of 'TotalLocalStateObligations'
	 */
    public String get_TotalLocalStateObligations() {
        return this._TotalLocalStateObligations;
    }

    /**
	 * Class member corresponding to the field 'EICNumQualChildren' in the PDF.
	 */
    private String _EICNumQualChildren = "";

    /**
	 * Mutator Method for xEICNumQualChildren
	 * @param EICNumQualChildren the new value for 'EICNumQualChildren'
	 */
    public void set_EICNumQualChildren(String _EICNumQualChildren) {
        this._EICNumQualChildren = _EICNumQualChildren;
    }

    /**
	 * Accessor Method for xEICNumQualChildren
	 * @return the value of 'EICNumQualChildren'
	 */
    public String get_EICNumQualChildren() {
        return this._EICNumQualChildren;
    }

    /**
	 * Class member corresponding to the field 'QualifiedSaleMobileHomePark Earned' in the PDF.
	 */
    private String _QualifiedSaleMobileHomePark_Earned = "";

    /**
	 * Mutator Method for xQualifiedSaleMobileHomePark Earned
	 * @param QualifiedSaleMobileHomePark Earned the new value for 'QualifiedSaleMobileHomePark Earned'
	 */
    public void set_QualifiedSaleMobileHomePark_Earned(String _QualifiedSaleMobileHomePark_Earned) {
        this._QualifiedSaleMobileHomePark_Earned = _QualifiedSaleMobileHomePark_Earned;
    }

    /**
	 * Accessor Method for xQualifiedSaleMobileHomePark Earned
	 * @return the value of 'QualifiedSaleMobileHomePark Earned'
	 */
    public String get_QualifiedSaleMobileHomePark_Earned() {
        return this._QualifiedSaleMobileHomePark_Earned;
    }

    /**
	 * Class member corresponding to the field 'VermontOtherEarnedIncome' in the PDF.
	 */
    private String _VermontOtherEarnedIncome = "";

    /**
	 * Mutator Method for xVermontOtherEarnedIncome
	 * @param VermontOtherEarnedIncome the new value for 'VermontOtherEarnedIncome'
	 */
    public void set_VermontOtherEarnedIncome(String _VermontOtherEarnedIncome) {
        this._VermontOtherEarnedIncome = _VermontOtherEarnedIncome;
    }

    /**
	 * Accessor Method for xVermontOtherEarnedIncome
	 * @return the value of 'VermontOtherEarnedIncome'
	 */
    public String get_VermontOtherEarnedIncome() {
        return this._VermontOtherEarnedIncome;
    }

    /**
	 * Class member corresponding to the field 'CharitableHousing CreditAmount' in the PDF.
	 */
    private String _CharitableHousing_CreditAmount = "";

    /**
	 * Mutator Method for xCharitableHousing CreditAmount
	 * @param CharitableHousing CreditAmount the new value for 'CharitableHousing CreditAmount'
	 */
    public void set_CharitableHousing_CreditAmount(String _CharitableHousing_CreditAmount) {
        this._CharitableHousing_CreditAmount = _CharitableHousing_CreditAmount;
    }

    /**
	 * Accessor Method for xCharitableHousing CreditAmount
	 * @return the value of 'CharitableHousing CreditAmount'
	 */
    public String get_CharitableHousing_CreditAmount() {
        return this._CharitableHousing_CreditAmount;
    }

    /**
	 * Class member corresponding to the field 'AngelVenture CreditAmount' in the PDF.
	 */
    private String _AngelVenture_CreditAmount = "";

    /**
	 * Mutator Method for xAngelVenture CreditAmount
	 * @param AngelVenture CreditAmount the new value for 'AngelVenture CreditAmount'
	 */
    public void set_AngelVenture_CreditAmount(String _AngelVenture_CreditAmount) {
        this._AngelVenture_CreditAmount = _AngelVenture_CreditAmount;
    }

    /**
	 * Accessor Method for xAngelVenture CreditAmount
	 * @return the value of 'AngelVenture CreditAmount'
	 */
    public String get_AngelVenture_CreditAmount() {
        return this._AngelVenture_CreditAmount;
    }

    /**
	 * Class member corresponding to the field 'FederalTotalEarnedIncome' in the PDF.
	 */
    private String _FederalTotalEarnedIncome = "";

    /**
	 * Mutator Method for xFederalTotalEarnedIncome
	 * @param FederalTotalEarnedIncome the new value for 'FederalTotalEarnedIncome'
	 */
    public void set_FederalTotalEarnedIncome(String _FederalTotalEarnedIncome) {
        this._FederalTotalEarnedIncome = _FederalTotalEarnedIncome;
    }

    /**
	 * Accessor Method for xFederalTotalEarnedIncome
	 * @return the value of 'FederalTotalEarnedIncome'
	 */
    public String get_FederalTotalEarnedIncome() {
        return this._FederalTotalEarnedIncome;
    }

    /**
	 * Class member corresponding to the field 'InvestmentTaxCredit' in the PDF.
	 */
    private String _InvestmentTaxCredit = "";

    /**
	 * Mutator Method for xInvestmentTaxCredit
	 * @param InvestmentTaxCredit the new value for 'InvestmentTaxCredit'
	 */
    public void set_InvestmentTaxCredit(String _InvestmentTaxCredit) {
        this._InvestmentTaxCredit = _InvestmentTaxCredit;
    }

    /**
	 * Accessor Method for xInvestmentTaxCredit
	 * @return the value of 'InvestmentTaxCredit'
	 */
    public String get_InvestmentTaxCredit() {
        return this._InvestmentTaxCredit;
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
	 * Class member corresponding to the field 'BusinessSolarEnergyCarryforward' in the PDF.
	 */
    private String _BusinessSolarEnergyCarryforward = "";

    /**
	 * Mutator Method for xBusinessSolarEnergyCarryforward
	 * @param BusinessSolarEnergyCarryforward the new value for 'BusinessSolarEnergyCarryforward'
	 */
    public void set_BusinessSolarEnergyCarryforward(String _BusinessSolarEnergyCarryforward) {
        this._BusinessSolarEnergyCarryforward = _BusinessSolarEnergyCarryforward;
    }

    /**
	 * Accessor Method for xBusinessSolarEnergyCarryforward
	 * @return the value of 'BusinessSolarEnergyCarryforward'
	 */
    public String get_BusinessSolarEnergyCarryforward() {
        return this._BusinessSolarEnergyCarryforward;
    }

    /**
	 * Class member corresponding to the field 'VTEICCreditSubtotal' in the PDF.
	 */
    private String _VTEICCreditSubtotal = "";

    /**
	 * Mutator Method for xVTEICCreditSubtotal
	 * @param VTEICCreditSubtotal the new value for 'VTEICCreditSubtotal'
	 */
    public void set_VTEICCreditSubtotal(String _VTEICCreditSubtotal) {
        this._VTEICCreditSubtotal = _VTEICCreditSubtotal;
    }

    /**
	 * Accessor Method for xVTEICCreditSubtotal
	 * @return the value of 'VTEICCreditSubtotal'
	 */
    public String get_VTEICCreditSubtotal() {
        return this._VTEICCreditSubtotal;
    }

    /**
	 * Class member corresponding to the field 'CommercialFilm CreditAmount' in the PDF.
	 */
    private String _CommercialFilm_CreditAmount = "";

    /**
	 * Mutator Method for xCommercialFilm CreditAmount
	 * @param CommercialFilm CreditAmount the new value for 'CommercialFilm CreditAmount'
	 */
    public void set_CommercialFilm_CreditAmount(String _CommercialFilm_CreditAmount) {
        this._CommercialFilm_CreditAmount = _CommercialFilm_CreditAmount;
    }

    /**
	 * Accessor Method for xCommercialFilm CreditAmount
	 * @return the value of 'CommercialFilm CreditAmount'
	 */
    public String get_CommercialFilm_CreditAmount() {
        return this._CommercialFilm_CreditAmount;
    }

    /**
	 * Class member corresponding to the field 'CommercialFilm Earned' in the PDF.
	 */
    private String _CommercialFilm_Earned = "";

    /**
	 * Mutator Method for xCommercialFilm Earned
	 * @param CommercialFilm Earned the new value for 'CommercialFilm Earned'
	 */
    public void set_CommercialFilm_Earned(String _CommercialFilm_Earned) {
        this._CommercialFilm_Earned = _CommercialFilm_Earned;
    }

    /**
	 * Accessor Method for xCommercialFilm Earned
	 * @return the value of 'CommercialFilm Earned'
	 */
    public String get_CommercialFilm_Earned() {
        return this._CommercialFilm_Earned;
    }

    /**
	 * Class member corresponding to the field 'FederalOtherEarnedIncome' in the PDF.
	 */
    private String _FederalOtherEarnedIncome = "";

    /**
	 * Mutator Method for xFederalOtherEarnedIncome
	 * @param FederalOtherEarnedIncome the new value for 'FederalOtherEarnedIncome'
	 */
    public void set_FederalOtherEarnedIncome(String _FederalOtherEarnedIncome) {
        this._FederalOtherEarnedIncome = _FederalOtherEarnedIncome;
    }

    /**
	 * Accessor Method for xFederalOtherEarnedIncome
	 * @return the value of 'FederalOtherEarnedIncome'
	 */
    public String get_FederalOtherEarnedIncome() {
        return this._FederalOtherEarnedIncome;
    }
}
