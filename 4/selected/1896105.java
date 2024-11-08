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
 * Generated class for filling 'TY2009_VT_IN111.pdf'
 */
public class TY2009_VT_IN111 {

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
        fields.setFieldProperty("VTTaxAfterCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxAfterCredits", "textfont", baseFont, null);
        fields.setField("VTTaxAfterCredits", this.get_VTTaxAfterCredits());
        fields.setFieldProperty("VermontCampaignFund", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VermontCampaignFund", "textfont", baseFont, null);
        fields.setField("VermontCampaignFund", this.get_VermontCampaignFund());
        fields.setFieldProperty("Exemptions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Exemptions", "textfont", baseFont, null);
        fields.setField("Exemptions", this.get_Exemptions());
        fields.setFieldProperty("FederalAdjustedGrossIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalAdjustedGrossIncome", "textfont", baseFont, null);
        fields.setField("FederalAdjustedGrossIncome", this.get_FederalAdjustedGrossIncome());
        fields.setFieldProperty("BalanceDueWithReturn", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BalanceDueWithReturn", "textfont", baseFont, null);
        fields.setField("BalanceDueWithReturn", this.get_BalanceDueWithReturn());
        fields.setFieldProperty("Secondary FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary FirstName", "textfont", baseFont, null);
        fields.setField("Secondary FirstName", this.get_Secondary_FirstName());
        fields.setFieldProperty("TotalDonations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalDonations", "textfont", baseFont, null);
        fields.setField("TotalDonations", this.get_TotalDonations());
        fields.setFieldProperty("LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LastName", "textfont", baseFont, null);
        fields.setField("LastName", this.get_LastName());
        fields.setFieldProperty("Check Box1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Check Box1", "textfont", baseFont, null);
        fields.setField("Check Box1", this.get_Check_Box1());
        fields.setFieldProperty("Check Box2", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Check Box2", "textfont", baseFont, null);
        fields.setField("Check Box2", this.get_Check_Box2());
        fields.setFieldProperty("AdjustedVTIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AdjustedVTIncomeTax", "textfont", baseFont, null);
        fields.setField("AdjustedVTIncomeTax", this.get_AdjustedVTIncomeTax());
        fields.setFieldProperty("SpouseCUPartnerName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SpouseCUPartnerName", "textfont", baseFont, null);
        fields.setField("SpouseCUPartnerName", this.get_SpouseCUPartnerName());
        fields.setFieldProperty("BonusDepreciation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BonusDepreciation", "textfont", baseFont, null);
        fields.setField("BonusDepreciation", this.get_BonusDepreciation());
        fields.setFieldProperty("ChildrensTrustFund", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ChildrensTrustFund", "textfont", baseFont, null);
        fields.setField("ChildrensTrustFund", this.get_ChildrensTrustFund());
        fields.setFieldProperty("VTTaxableIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxableIncome", "textfont", baseFont, null);
        fields.setField("VTTaxableIncome", this.get_VTTaxableIncome());
        fields.setFieldProperty("RealEstateWithholding", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RealEstateWithholding", "textfont", baseFont, null);
        fields.setField("RealEstateWithholding", this.get_RealEstateWithholding());
        fields.setFieldProperty("SchoolDistrict", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SchoolDistrict", "textfont", baseFont, null);
        fields.setField("SchoolDistrict", this.get_SchoolDistrict());
        fields.setFieldProperty("FedTaxableWithAdditions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FedTaxableWithAdditions", "textfont", baseFont, null);
        fields.setField("FedTaxableWithAdditions", this.get_FedTaxableWithAdditions());
        fields.setFieldProperty("StateofLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateofLegalResidence", "textfont", baseFont, null);
        fields.setField("StateofLegalResidence", this.get_StateofLegalResidence());
        fields.setFieldProperty("UseTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("UseTax", "textfont", baseFont, null);
        fields.setField("UseTax", this.get_UseTax());
        fields.setFieldProperty("TotalVTCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalVTCredits", "textfont", baseFont, null);
        fields.setField("TotalVTCredits", this.get_TotalVTCredits());
        fields.setFieldProperty("TaxWithheld", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxWithheld", "textfont", baseFont, null);
        fields.setField("TaxWithheld", this.get_TaxWithheld());
        fields.setFieldProperty("FederalTaxableIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FederalTaxableIncome", "textfont", baseFont, null);
        fields.setField("FederalTaxableIncome", this.get_FederalTaxableIncome());
        fields.setFieldProperty("StateIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateIncomeTax", "textfont", baseFont, null);
        fields.setField("StateIncomeTax", this.get_StateIncomeTax());
        fields.setFieldProperty("BusEntityPayments", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusEntityPayments", "textfont", baseFont, null);
        fields.setField("BusEntityPayments", this.get_BusEntityPayments());
        fields.setFieldProperty("LowIncChildDepCare", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LowIncChildDepCare", "textfont", baseFont, null);
        fields.setField("LowIncChildDepCare", this.get_LowIncChildDepCare());
        fields.setFieldProperty("Preparer SSN PTIN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Preparer SSN PTIN", "textfont", baseFont, null);
        fields.setField("Preparer SSN PTIN", this.get_Preparer_SSN_PTIN());
        fields.setFieldProperty("Check Box9", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Check Box9", "textfont", baseFont, null);
        fields.setField("Check Box9", this.get_Check_Box9());
        fields.setFieldProperty("CapitalGain", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CapitalGain", "textfont", baseFont, null);
        fields.setField("CapitalGain", this.get_CapitalGain());
        fields.setFieldProperty("IncomeAdjustment", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IncomeAdjustment", "textfont", baseFont, null);
        fields.setField("IncomeAdjustment", this.get_IncomeAdjustment());
        fields.setFieldProperty("Preparer Phone", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Preparer Phone", "textfont", baseFont, null);
        fields.setField("Preparer Phone", this.get_Preparer_Phone());
        fields.setFieldProperty("ZIPCode", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ZIPCode", "textfont", baseFont, null);
        fields.setField("ZIPCode", this.get_ZIPCode());
        fields.setFieldProperty("AddressLine1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AddressLine1", "textfont", baseFont, null);
        fields.setField("AddressLine1", this.get_AddressLine1());
        fields.setFieldProperty("UnderpaymentPenaltyInterest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("UnderpaymentPenaltyInterest", "textfont", baseFont, null);
        fields.setField("UnderpaymentPenaltyInterest", this.get_UnderpaymentPenaltyInterest());
        fields.setFieldProperty("OverpaymentCreditedPropTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OverpaymentCreditedPropTax", "textfont", baseFont, null);
        fields.setField("OverpaymentCreditedPropTax", this.get_OverpaymentCreditedPropTax());
        fields.setFieldProperty("OverpaymentAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OverpaymentAmount", "textfont", baseFont, null);
        fields.setField("OverpaymentAmount", this.get_OverpaymentAmount());
        fields.setFieldProperty("TownCityLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TownCityLegalResidence", "textfont", baseFont, null);
        fields.setField("TownCityLegalResidence", this.get_TownCityLegalResidence());
        fields.setFieldProperty("TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("TaxpayerSSN", this.get_TaxpayerSSN());
        fields.setFieldProperty("Secondary TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("Secondary TaxpayerSSN", this.get_Secondary_TaxpayerSSN());
        fields.setFieldProperty("TotalBalanceDue", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalBalanceDue", "textfont", baseFont, null);
        fields.setField("TotalBalanceDue", this.get_TotalBalanceDue());
        fields.setFieldProperty("BonusDepreciationAdj", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BonusDepreciationAdj", "textfont", baseFont, null);
        fields.setField("BonusDepreciationAdj", this.get_BonusDepreciationAdj());
        fields.setFieldProperty("VTIncomeTaxCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTIncomeTaxCredits", "textfont", baseFont, null);
        fields.setField("VTIncomeTaxCredits", this.get_VTIncomeTaxCredits());
        fields.setFieldProperty("TotalVTTaxes", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalVTTaxes", "textfont", baseFont, null);
        fields.setField("TotalVTTaxes", this.get_TotalVTTaxes());
        fields.setFieldProperty("OtherAdditions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OtherAdditions", "textfont", baseFont, null);
        fields.setField("OtherAdditions", this.get_OtherAdditions());
        fields.setFieldProperty("EIC", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EIC", "textfont", baseFont, null);
        fields.setField("EIC", this.get_EIC());
        fields.setFieldProperty("Secondary LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary LastName", "textfont", baseFont, null);
        fields.setField("Secondary LastName", this.get_Secondary_LastName());
        fields.setFieldProperty("FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FirstName", "textfont", baseFont, null);
        fields.setField("FirstName", this.get_FirstName());
        fields.setFieldProperty("FilingStatus", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FilingStatus", "textfont", baseFont, null);
        fields.setField("FilingStatus", this.get_FilingStatus());
        fields.setFieldProperty("NonGameWildlife", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NonGameWildlife", "textfont", baseFont, null);
        fields.setField("NonGameWildlife", this.get_NonGameWildlife());
        fields.setFieldProperty("StateMuniInterest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateMuniInterest", "textfont", baseFont, null);
        fields.setField("StateMuniInterest", this.get_StateMuniInterest());
        fields.setFieldProperty("RefundAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RefundAmount", "textfont", baseFont, null);
        fields.setField("RefundAmount", this.get_RefundAmount());
        fields.setFieldProperty("TotalVTTaxesAndContributions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalVTTaxesAndContributions", "textfont", baseFont, null);
        fields.setField("TotalVTTaxesAndContributions", this.get_TotalVTTaxesAndContributions());
        fields.setFieldProperty("EstimatedPaymentTotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EstimatedPaymentTotal", "textfont", baseFont, null);
        fields.setField("EstimatedPaymentTotal", this.get_EstimatedPaymentTotal());
        fields.setFieldProperty("TaxesPaidOtherStateCreditAmt", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxesPaidOtherStateCreditAmt", "textfont", baseFont, null);
        fields.setField("TaxesPaidOtherStateCreditAmt", this.get_TaxesPaidOtherStateCreditAmt());
        fields.setFieldProperty("SpouseCUPartnerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SpouseCUPartnerSSN", "textfont", baseFont, null);
        fields.setField("SpouseCUPartnerSSN", this.get_SpouseCUPartnerSSN());
        fields.setFieldProperty("ItemizedDeductionAddBack", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ItemizedDeductionAddBack", "textfont", baseFont, null);
        fields.setField("ItemizedDeductionAddBack", this.get_ItemizedDeductionAddBack());
        fields.setFieldProperty("VTIncomeWithAdditions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTIncomeWithAdditions", "textfont", baseFont, null);
        fields.setField("VTIncomeWithAdditions", this.get_VTIncomeWithAdditions());
        fields.setFieldProperty("PreparerBusinessName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("PreparerBusinessName", "textfont", baseFont, null);
        fields.setField("PreparerBusinessName", this.get_PreparerBusinessName());
        fields.setFieldProperty("TotalPaymentsAndCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalPaymentsAndCredits", "textfont", baseFont, null);
        fields.setField("TotalPaymentsAndCredits", this.get_TotalPaymentsAndCredits());
        fields.setFieldProperty("City", "textsize", new Float(20.2), null);
        fields.setFieldProperty("City", "textfont", baseFont, null);
        fields.setField("City", this.get_City());
        fields.setFieldProperty("RenterRebate", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RenterRebate", "textfont", baseFont, null);
        fields.setField("RenterRebate", this.get_RenterRebate());
        fields.setFieldProperty("Text16", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text16", "textfont", baseFont, null);
        fields.setField("Text16", this.get_Text16());
        fields.setFieldProperty("InterestUSObligations", "textsize", new Float(20.2), null);
        fields.setFieldProperty("InterestUSObligations", "textfont", baseFont, null);
        fields.setField("InterestUSObligations", this.get_InterestUSObligations());
        fields.setFieldProperty("State", "textsize", new Float(20.2), null);
        fields.setFieldProperty("State", "textfont", baseFont, null);
        fields.setField("State", this.get_State());
        fields.setFieldProperty("VTIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTIncomeTax", "textfont", baseFont, null);
        fields.setField("VTIncomeTax", this.get_VTIncomeTax());
        fields.setFieldProperty("OverpaymentCreditedNxtYr", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OverpaymentCreditedNxtYr", "textfont", baseFont, null);
        fields.setField("OverpaymentCreditedNxtYr", this.get_OverpaymentCreditedNxtYr());
        fields.setFieldProperty("TotalSubtractions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalSubtractions", "textfont", baseFont, null);
        fields.setField("TotalSubtractions", this.get_TotalSubtractions());
        fields.setFieldProperty("OtherSubtractions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OtherSubtractions", "textfont", baseFont, null);
        fields.setField("OtherSubtractions", this.get_OtherSubtractions());
        fields.setFieldProperty("PreparerFirmIDNumber", "textsize", new Float(20.2), null);
        fields.setFieldProperty("PreparerFirmIDNumber", "textfont", baseFont, null);
        fields.setField("PreparerFirmIDNumber", this.get_PreparerFirmIDNumber());
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
	 * Class member corresponding to the field 'VTTaxAfterCredits' in the PDF.
	 */
    private String _VTTaxAfterCredits = "";

    /**
	 * Mutator Method for xVTTaxAfterCredits
	 * @param VTTaxAfterCredits the new value for 'VTTaxAfterCredits'
	 */
    public void set_VTTaxAfterCredits(String _VTTaxAfterCredits) {
        this._VTTaxAfterCredits = _VTTaxAfterCredits;
    }

    /**
	 * Accessor Method for xVTTaxAfterCredits
	 * @return the value of 'VTTaxAfterCredits'
	 */
    public String get_VTTaxAfterCredits() {
        return this._VTTaxAfterCredits;
    }

    /**
	 * Class member corresponding to the field 'VermontCampaignFund' in the PDF.
	 */
    private String _VermontCampaignFund = "";

    /**
	 * Mutator Method for xVermontCampaignFund
	 * @param VermontCampaignFund the new value for 'VermontCampaignFund'
	 */
    public void set_VermontCampaignFund(String _VermontCampaignFund) {
        this._VermontCampaignFund = _VermontCampaignFund;
    }

    /**
	 * Accessor Method for xVermontCampaignFund
	 * @return the value of 'VermontCampaignFund'
	 */
    public String get_VermontCampaignFund() {
        return this._VermontCampaignFund;
    }

    /**
	 * Class member corresponding to the field 'Exemptions' in the PDF.
	 */
    private String _Exemptions = "";

    /**
	 * Mutator Method for xExemptions
	 * @param Exemptions the new value for 'Exemptions'
	 */
    public void set_Exemptions(String _Exemptions) {
        this._Exemptions = _Exemptions;
    }

    /**
	 * Accessor Method for xExemptions
	 * @return the value of 'Exemptions'
	 */
    public String get_Exemptions() {
        return this._Exemptions;
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
	 * Class member corresponding to the field 'BalanceDueWithReturn' in the PDF.
	 */
    private String _BalanceDueWithReturn = "";

    /**
	 * Mutator Method for xBalanceDueWithReturn
	 * @param BalanceDueWithReturn the new value for 'BalanceDueWithReturn'
	 */
    public void set_BalanceDueWithReturn(String _BalanceDueWithReturn) {
        this._BalanceDueWithReturn = _BalanceDueWithReturn;
    }

    /**
	 * Accessor Method for xBalanceDueWithReturn
	 * @return the value of 'BalanceDueWithReturn'
	 */
    public String get_BalanceDueWithReturn() {
        return this._BalanceDueWithReturn;
    }

    /**
	 * Class member corresponding to the field 'Secondary FirstName' in the PDF.
	 */
    private String _Secondary_FirstName = "";

    /**
	 * Mutator Method for xSecondary FirstName
	 * @param Secondary FirstName the new value for 'Secondary FirstName'
	 */
    public void set_Secondary_FirstName(String _Secondary_FirstName) {
        this._Secondary_FirstName = _Secondary_FirstName;
    }

    /**
	 * Accessor Method for xSecondary FirstName
	 * @return the value of 'Secondary FirstName'
	 */
    public String get_Secondary_FirstName() {
        return this._Secondary_FirstName;
    }

    /**
	 * Class member corresponding to the field 'TotalDonations' in the PDF.
	 */
    private String _TotalDonations = "";

    /**
	 * Mutator Method for xTotalDonations
	 * @param TotalDonations the new value for 'TotalDonations'
	 */
    public void set_TotalDonations(String _TotalDonations) {
        this._TotalDonations = _TotalDonations;
    }

    /**
	 * Accessor Method for xTotalDonations
	 * @return the value of 'TotalDonations'
	 */
    public String get_TotalDonations() {
        return this._TotalDonations;
    }

    /**
	 * Class member corresponding to the field 'LastName' in the PDF.
	 */
    private String _LastName = "";

    /**
	 * Mutator Method for xLastName
	 * @param LastName the new value for 'LastName'
	 */
    public void set_LastName(String _LastName) {
        this._LastName = _LastName;
    }

    /**
	 * Accessor Method for xLastName
	 * @return the value of 'LastName'
	 */
    public String get_LastName() {
        return this._LastName;
    }

    /**
	 * Class member corresponding to the field 'Check Box1' in the PDF.
	 */
    private String _Check_Box1 = "";

    /**
	 * Mutator Method for xCheck Box1
	 * @param Check Box1 the new value for 'Check Box1'
	 */
    public void set_Check_Box1(String _Check_Box1) {
        this._Check_Box1 = _Check_Box1;
    }

    /**
	 * Accessor Method for xCheck Box1
	 * @return the value of 'Check Box1'
	 */
    public String get_Check_Box1() {
        return this._Check_Box1;
    }

    /**
	 * Class member corresponding to the field 'Check Box2' in the PDF.
	 */
    private String _Check_Box2 = "";

    /**
	 * Mutator Method for xCheck Box2
	 * @param Check Box2 the new value for 'Check Box2'
	 */
    public void set_Check_Box2(String _Check_Box2) {
        this._Check_Box2 = _Check_Box2;
    }

    /**
	 * Accessor Method for xCheck Box2
	 * @return the value of 'Check Box2'
	 */
    public String get_Check_Box2() {
        return this._Check_Box2;
    }

    /**
	 * Class member corresponding to the field 'AdjustedVTIncomeTax' in the PDF.
	 */
    private String _AdjustedVTIncomeTax = "";

    /**
	 * Mutator Method for xAdjustedVTIncomeTax
	 * @param AdjustedVTIncomeTax the new value for 'AdjustedVTIncomeTax'
	 */
    public void set_AdjustedVTIncomeTax(String _AdjustedVTIncomeTax) {
        this._AdjustedVTIncomeTax = _AdjustedVTIncomeTax;
    }

    /**
	 * Accessor Method for xAdjustedVTIncomeTax
	 * @return the value of 'AdjustedVTIncomeTax'
	 */
    public String get_AdjustedVTIncomeTax() {
        return this._AdjustedVTIncomeTax;
    }

    /**
	 * Class member corresponding to the field 'SpouseCUPartnerName' in the PDF.
	 */
    private String _SpouseCUPartnerName = "";

    /**
	 * Mutator Method for xSpouseCUPartnerName
	 * @param SpouseCUPartnerName the new value for 'SpouseCUPartnerName'
	 */
    public void set_SpouseCUPartnerName(String _SpouseCUPartnerName) {
        this._SpouseCUPartnerName = _SpouseCUPartnerName;
    }

    /**
	 * Accessor Method for xSpouseCUPartnerName
	 * @return the value of 'SpouseCUPartnerName'
	 */
    public String get_SpouseCUPartnerName() {
        return this._SpouseCUPartnerName;
    }

    /**
	 * Class member corresponding to the field 'BonusDepreciation' in the PDF.
	 */
    private String _BonusDepreciation = "";

    /**
	 * Mutator Method for xBonusDepreciation
	 * @param BonusDepreciation the new value for 'BonusDepreciation'
	 */
    public void set_BonusDepreciation(String _BonusDepreciation) {
        this._BonusDepreciation = _BonusDepreciation;
    }

    /**
	 * Accessor Method for xBonusDepreciation
	 * @return the value of 'BonusDepreciation'
	 */
    public String get_BonusDepreciation() {
        return this._BonusDepreciation;
    }

    /**
	 * Class member corresponding to the field 'ChildrensTrustFund' in the PDF.
	 */
    private String _ChildrensTrustFund = "";

    /**
	 * Mutator Method for xChildrensTrustFund
	 * @param ChildrensTrustFund the new value for 'ChildrensTrustFund'
	 */
    public void set_ChildrensTrustFund(String _ChildrensTrustFund) {
        this._ChildrensTrustFund = _ChildrensTrustFund;
    }

    /**
	 * Accessor Method for xChildrensTrustFund
	 * @return the value of 'ChildrensTrustFund'
	 */
    public String get_ChildrensTrustFund() {
        return this._ChildrensTrustFund;
    }

    /**
	 * Class member corresponding to the field 'VTTaxableIncome' in the PDF.
	 */
    private String _VTTaxableIncome = "";

    /**
	 * Mutator Method for xVTTaxableIncome
	 * @param VTTaxableIncome the new value for 'VTTaxableIncome'
	 */
    public void set_VTTaxableIncome(String _VTTaxableIncome) {
        this._VTTaxableIncome = _VTTaxableIncome;
    }

    /**
	 * Accessor Method for xVTTaxableIncome
	 * @return the value of 'VTTaxableIncome'
	 */
    public String get_VTTaxableIncome() {
        return this._VTTaxableIncome;
    }

    /**
	 * Class member corresponding to the field 'RealEstateWithholding' in the PDF.
	 */
    private String _RealEstateWithholding = "";

    /**
	 * Mutator Method for xRealEstateWithholding
	 * @param RealEstateWithholding the new value for 'RealEstateWithholding'
	 */
    public void set_RealEstateWithholding(String _RealEstateWithholding) {
        this._RealEstateWithholding = _RealEstateWithholding;
    }

    /**
	 * Accessor Method for xRealEstateWithholding
	 * @return the value of 'RealEstateWithholding'
	 */
    public String get_RealEstateWithholding() {
        return this._RealEstateWithholding;
    }

    /**
	 * Class member corresponding to the field 'SchoolDistrict' in the PDF.
	 */
    private String _SchoolDistrict = "";

    /**
	 * Mutator Method for xSchoolDistrict
	 * @param SchoolDistrict the new value for 'SchoolDistrict'
	 */
    public void set_SchoolDistrict(String _SchoolDistrict) {
        this._SchoolDistrict = _SchoolDistrict;
    }

    /**
	 * Accessor Method for xSchoolDistrict
	 * @return the value of 'SchoolDistrict'
	 */
    public String get_SchoolDistrict() {
        return this._SchoolDistrict;
    }

    /**
	 * Class member corresponding to the field 'FedTaxableWithAdditions' in the PDF.
	 */
    private String _FedTaxableWithAdditions = "";

    /**
	 * Mutator Method for xFedTaxableWithAdditions
	 * @param FedTaxableWithAdditions the new value for 'FedTaxableWithAdditions'
	 */
    public void set_FedTaxableWithAdditions(String _FedTaxableWithAdditions) {
        this._FedTaxableWithAdditions = _FedTaxableWithAdditions;
    }

    /**
	 * Accessor Method for xFedTaxableWithAdditions
	 * @return the value of 'FedTaxableWithAdditions'
	 */
    public String get_FedTaxableWithAdditions() {
        return this._FedTaxableWithAdditions;
    }

    /**
	 * Class member corresponding to the field 'StateofLegalResidence' in the PDF.
	 */
    private String _StateofLegalResidence = "";

    /**
	 * Mutator Method for xStateofLegalResidence
	 * @param StateofLegalResidence the new value for 'StateofLegalResidence'
	 */
    public void set_StateofLegalResidence(String _StateofLegalResidence) {
        this._StateofLegalResidence = _StateofLegalResidence;
    }

    /**
	 * Accessor Method for xStateofLegalResidence
	 * @return the value of 'StateofLegalResidence'
	 */
    public String get_StateofLegalResidence() {
        return this._StateofLegalResidence;
    }

    /**
	 * Class member corresponding to the field 'UseTax' in the PDF.
	 */
    private String _UseTax = "";

    /**
	 * Mutator Method for xUseTax
	 * @param UseTax the new value for 'UseTax'
	 */
    public void set_UseTax(String _UseTax) {
        this._UseTax = _UseTax;
    }

    /**
	 * Accessor Method for xUseTax
	 * @return the value of 'UseTax'
	 */
    public String get_UseTax() {
        return this._UseTax;
    }

    /**
	 * Class member corresponding to the field 'TotalVTCredits' in the PDF.
	 */
    private String _TotalVTCredits = "";

    /**
	 * Mutator Method for xTotalVTCredits
	 * @param TotalVTCredits the new value for 'TotalVTCredits'
	 */
    public void set_TotalVTCredits(String _TotalVTCredits) {
        this._TotalVTCredits = _TotalVTCredits;
    }

    /**
	 * Accessor Method for xTotalVTCredits
	 * @return the value of 'TotalVTCredits'
	 */
    public String get_TotalVTCredits() {
        return this._TotalVTCredits;
    }

    /**
	 * Class member corresponding to the field 'TaxWithheld' in the PDF.
	 */
    private String _TaxWithheld = "";

    /**
	 * Mutator Method for xTaxWithheld
	 * @param TaxWithheld the new value for 'TaxWithheld'
	 */
    public void set_TaxWithheld(String _TaxWithheld) {
        this._TaxWithheld = _TaxWithheld;
    }

    /**
	 * Accessor Method for xTaxWithheld
	 * @return the value of 'TaxWithheld'
	 */
    public String get_TaxWithheld() {
        return this._TaxWithheld;
    }

    /**
	 * Class member corresponding to the field 'FederalTaxableIncome' in the PDF.
	 */
    private String _FederalTaxableIncome = "";

    /**
	 * Mutator Method for xFederalTaxableIncome
	 * @param FederalTaxableIncome the new value for 'FederalTaxableIncome'
	 */
    public void set_FederalTaxableIncome(String _FederalTaxableIncome) {
        this._FederalTaxableIncome = _FederalTaxableIncome;
    }

    /**
	 * Accessor Method for xFederalTaxableIncome
	 * @return the value of 'FederalTaxableIncome'
	 */
    public String get_FederalTaxableIncome() {
        return this._FederalTaxableIncome;
    }

    /**
	 * Class member corresponding to the field 'StateIncomeTax' in the PDF.
	 */
    private String _StateIncomeTax = "";

    /**
	 * Mutator Method for xStateIncomeTax
	 * @param StateIncomeTax the new value for 'StateIncomeTax'
	 */
    public void set_StateIncomeTax(String _StateIncomeTax) {
        this._StateIncomeTax = _StateIncomeTax;
    }

    /**
	 * Accessor Method for xStateIncomeTax
	 * @return the value of 'StateIncomeTax'
	 */
    public String get_StateIncomeTax() {
        return this._StateIncomeTax;
    }

    /**
	 * Class member corresponding to the field 'BusEntityPayments' in the PDF.
	 */
    private String _BusEntityPayments = "";

    /**
	 * Mutator Method for xBusEntityPayments
	 * @param BusEntityPayments the new value for 'BusEntityPayments'
	 */
    public void set_BusEntityPayments(String _BusEntityPayments) {
        this._BusEntityPayments = _BusEntityPayments;
    }

    /**
	 * Accessor Method for xBusEntityPayments
	 * @return the value of 'BusEntityPayments'
	 */
    public String get_BusEntityPayments() {
        return this._BusEntityPayments;
    }

    /**
	 * Class member corresponding to the field 'LowIncChildDepCare' in the PDF.
	 */
    private String _LowIncChildDepCare = "";

    /**
	 * Mutator Method for xLowIncChildDepCare
	 * @param LowIncChildDepCare the new value for 'LowIncChildDepCare'
	 */
    public void set_LowIncChildDepCare(String _LowIncChildDepCare) {
        this._LowIncChildDepCare = _LowIncChildDepCare;
    }

    /**
	 * Accessor Method for xLowIncChildDepCare
	 * @return the value of 'LowIncChildDepCare'
	 */
    public String get_LowIncChildDepCare() {
        return this._LowIncChildDepCare;
    }

    /**
	 * Class member corresponding to the field 'Preparer SSN PTIN' in the PDF.
	 */
    private String _Preparer_SSN_PTIN = "";

    /**
	 * Mutator Method for xPreparer SSN PTIN
	 * @param Preparer SSN PTIN the new value for 'Preparer SSN PTIN'
	 */
    public void set_Preparer_SSN_PTIN(String _Preparer_SSN_PTIN) {
        this._Preparer_SSN_PTIN = _Preparer_SSN_PTIN;
    }

    /**
	 * Accessor Method for xPreparer SSN PTIN
	 * @return the value of 'Preparer SSN PTIN'
	 */
    public String get_Preparer_SSN_PTIN() {
        return this._Preparer_SSN_PTIN;
    }

    /**
	 * Class member corresponding to the field 'Check Box9' in the PDF.
	 */
    private String _Check_Box9 = "";

    /**
	 * Mutator Method for xCheck Box9
	 * @param Check Box9 the new value for 'Check Box9'
	 */
    public void set_Check_Box9(String _Check_Box9) {
        this._Check_Box9 = _Check_Box9;
    }

    /**
	 * Accessor Method for xCheck Box9
	 * @return the value of 'Check Box9'
	 */
    public String get_Check_Box9() {
        return this._Check_Box9;
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
	 * Class member corresponding to the field 'IncomeAdjustment' in the PDF.
	 */
    private String _IncomeAdjustment = "";

    /**
	 * Mutator Method for xIncomeAdjustment
	 * @param IncomeAdjustment the new value for 'IncomeAdjustment'
	 */
    public void set_IncomeAdjustment(String _IncomeAdjustment) {
        this._IncomeAdjustment = _IncomeAdjustment;
    }

    /**
	 * Accessor Method for xIncomeAdjustment
	 * @return the value of 'IncomeAdjustment'
	 */
    public String get_IncomeAdjustment() {
        return this._IncomeAdjustment;
    }

    /**
	 * Class member corresponding to the field 'Preparer Phone' in the PDF.
	 */
    private String _Preparer_Phone = "";

    /**
	 * Mutator Method for xPreparer Phone
	 * @param Preparer Phone the new value for 'Preparer Phone'
	 */
    public void set_Preparer_Phone(String _Preparer_Phone) {
        this._Preparer_Phone = _Preparer_Phone;
    }

    /**
	 * Accessor Method for xPreparer Phone
	 * @return the value of 'Preparer Phone'
	 */
    public String get_Preparer_Phone() {
        return this._Preparer_Phone;
    }

    /**
	 * Class member corresponding to the field 'ZIPCode' in the PDF.
	 */
    private String _ZIPCode = "";

    /**
	 * Mutator Method for xZIPCode
	 * @param ZIPCode the new value for 'ZIPCode'
	 */
    public void set_ZIPCode(String _ZIPCode) {
        this._ZIPCode = _ZIPCode;
    }

    /**
	 * Accessor Method for xZIPCode
	 * @return the value of 'ZIPCode'
	 */
    public String get_ZIPCode() {
        return this._ZIPCode;
    }

    /**
	 * Class member corresponding to the field 'AddressLine1' in the PDF.
	 */
    private String _AddressLine1 = "";

    /**
	 * Mutator Method for xAddressLine1
	 * @param AddressLine1 the new value for 'AddressLine1'
	 */
    public void set_AddressLine1(String _AddressLine1) {
        this._AddressLine1 = _AddressLine1;
    }

    /**
	 * Accessor Method for xAddressLine1
	 * @return the value of 'AddressLine1'
	 */
    public String get_AddressLine1() {
        return this._AddressLine1;
    }

    /**
	 * Class member corresponding to the field 'UnderpaymentPenaltyInterest' in the PDF.
	 */
    private String _UnderpaymentPenaltyInterest = "";

    /**
	 * Mutator Method for xUnderpaymentPenaltyInterest
	 * @param UnderpaymentPenaltyInterest the new value for 'UnderpaymentPenaltyInterest'
	 */
    public void set_UnderpaymentPenaltyInterest(String _UnderpaymentPenaltyInterest) {
        this._UnderpaymentPenaltyInterest = _UnderpaymentPenaltyInterest;
    }

    /**
	 * Accessor Method for xUnderpaymentPenaltyInterest
	 * @return the value of 'UnderpaymentPenaltyInterest'
	 */
    public String get_UnderpaymentPenaltyInterest() {
        return this._UnderpaymentPenaltyInterest;
    }

    /**
	 * Class member corresponding to the field 'OverpaymentCreditedPropTax' in the PDF.
	 */
    private String _OverpaymentCreditedPropTax = "";

    /**
	 * Mutator Method for xOverpaymentCreditedPropTax
	 * @param OverpaymentCreditedPropTax the new value for 'OverpaymentCreditedPropTax'
	 */
    public void set_OverpaymentCreditedPropTax(String _OverpaymentCreditedPropTax) {
        this._OverpaymentCreditedPropTax = _OverpaymentCreditedPropTax;
    }

    /**
	 * Accessor Method for xOverpaymentCreditedPropTax
	 * @return the value of 'OverpaymentCreditedPropTax'
	 */
    public String get_OverpaymentCreditedPropTax() {
        return this._OverpaymentCreditedPropTax;
    }

    /**
	 * Class member corresponding to the field 'OverpaymentAmount' in the PDF.
	 */
    private String _OverpaymentAmount = "";

    /**
	 * Mutator Method for xOverpaymentAmount
	 * @param OverpaymentAmount the new value for 'OverpaymentAmount'
	 */
    public void set_OverpaymentAmount(String _OverpaymentAmount) {
        this._OverpaymentAmount = _OverpaymentAmount;
    }

    /**
	 * Accessor Method for xOverpaymentAmount
	 * @return the value of 'OverpaymentAmount'
	 */
    public String get_OverpaymentAmount() {
        return this._OverpaymentAmount;
    }

    /**
	 * Class member corresponding to the field 'TownCityLegalResidence' in the PDF.
	 */
    private String _TownCityLegalResidence = "";

    /**
	 * Mutator Method for xTownCityLegalResidence
	 * @param TownCityLegalResidence the new value for 'TownCityLegalResidence'
	 */
    public void set_TownCityLegalResidence(String _TownCityLegalResidence) {
        this._TownCityLegalResidence = _TownCityLegalResidence;
    }

    /**
	 * Accessor Method for xTownCityLegalResidence
	 * @return the value of 'TownCityLegalResidence'
	 */
    public String get_TownCityLegalResidence() {
        return this._TownCityLegalResidence;
    }

    /**
	 * Class member corresponding to the field 'TaxpayerSSN' in the PDF.
	 */
    private String _TaxpayerSSN = "";

    /**
	 * Mutator Method for xTaxpayerSSN
	 * @param TaxpayerSSN the new value for 'TaxpayerSSN'
	 */
    public void set_TaxpayerSSN(String _TaxpayerSSN) {
        this._TaxpayerSSN = _TaxpayerSSN;
    }

    /**
	 * Accessor Method for xTaxpayerSSN
	 * @return the value of 'TaxpayerSSN'
	 */
    public String get_TaxpayerSSN() {
        return this._TaxpayerSSN;
    }

    /**
	 * Class member corresponding to the field 'Secondary TaxpayerSSN' in the PDF.
	 */
    private String _Secondary_TaxpayerSSN = "";

    /**
	 * Mutator Method for xSecondary TaxpayerSSN
	 * @param Secondary TaxpayerSSN the new value for 'Secondary TaxpayerSSN'
	 */
    public void set_Secondary_TaxpayerSSN(String _Secondary_TaxpayerSSN) {
        this._Secondary_TaxpayerSSN = _Secondary_TaxpayerSSN;
    }

    /**
	 * Accessor Method for xSecondary TaxpayerSSN
	 * @return the value of 'Secondary TaxpayerSSN'
	 */
    public String get_Secondary_TaxpayerSSN() {
        return this._Secondary_TaxpayerSSN;
    }

    /**
	 * Class member corresponding to the field 'TotalBalanceDue' in the PDF.
	 */
    private String _TotalBalanceDue = "";

    /**
	 * Mutator Method for xTotalBalanceDue
	 * @param TotalBalanceDue the new value for 'TotalBalanceDue'
	 */
    public void set_TotalBalanceDue(String _TotalBalanceDue) {
        this._TotalBalanceDue = _TotalBalanceDue;
    }

    /**
	 * Accessor Method for xTotalBalanceDue
	 * @return the value of 'TotalBalanceDue'
	 */
    public String get_TotalBalanceDue() {
        return this._TotalBalanceDue;
    }

    /**
	 * Class member corresponding to the field 'BonusDepreciationAdj' in the PDF.
	 */
    private String _BonusDepreciationAdj = "";

    /**
	 * Mutator Method for xBonusDepreciationAdj
	 * @param BonusDepreciationAdj the new value for 'BonusDepreciationAdj'
	 */
    public void set_BonusDepreciationAdj(String _BonusDepreciationAdj) {
        this._BonusDepreciationAdj = _BonusDepreciationAdj;
    }

    /**
	 * Accessor Method for xBonusDepreciationAdj
	 * @return the value of 'BonusDepreciationAdj'
	 */
    public String get_BonusDepreciationAdj() {
        return this._BonusDepreciationAdj;
    }

    /**
	 * Class member corresponding to the field 'VTIncomeTaxCredits' in the PDF.
	 */
    private String _VTIncomeTaxCredits = "";

    /**
	 * Mutator Method for xVTIncomeTaxCredits
	 * @param VTIncomeTaxCredits the new value for 'VTIncomeTaxCredits'
	 */
    public void set_VTIncomeTaxCredits(String _VTIncomeTaxCredits) {
        this._VTIncomeTaxCredits = _VTIncomeTaxCredits;
    }

    /**
	 * Accessor Method for xVTIncomeTaxCredits
	 * @return the value of 'VTIncomeTaxCredits'
	 */
    public String get_VTIncomeTaxCredits() {
        return this._VTIncomeTaxCredits;
    }

    /**
	 * Class member corresponding to the field 'TotalVTTaxes' in the PDF.
	 */
    private String _TotalVTTaxes = "";

    /**
	 * Mutator Method for xTotalVTTaxes
	 * @param TotalVTTaxes the new value for 'TotalVTTaxes'
	 */
    public void set_TotalVTTaxes(String _TotalVTTaxes) {
        this._TotalVTTaxes = _TotalVTTaxes;
    }

    /**
	 * Accessor Method for xTotalVTTaxes
	 * @return the value of 'TotalVTTaxes'
	 */
    public String get_TotalVTTaxes() {
        return this._TotalVTTaxes;
    }

    /**
	 * Class member corresponding to the field 'OtherAdditions' in the PDF.
	 */
    private String _OtherAdditions = "";

    /**
	 * Mutator Method for xOtherAdditions
	 * @param OtherAdditions the new value for 'OtherAdditions'
	 */
    public void set_OtherAdditions(String _OtherAdditions) {
        this._OtherAdditions = _OtherAdditions;
    }

    /**
	 * Accessor Method for xOtherAdditions
	 * @return the value of 'OtherAdditions'
	 */
    public String get_OtherAdditions() {
        return this._OtherAdditions;
    }

    /**
	 * Class member corresponding to the field 'EIC' in the PDF.
	 */
    private String _EIC = "";

    /**
	 * Mutator Method for xEIC
	 * @param EIC the new value for 'EIC'
	 */
    public void set_EIC(String _EIC) {
        this._EIC = _EIC;
    }

    /**
	 * Accessor Method for xEIC
	 * @return the value of 'EIC'
	 */
    public String get_EIC() {
        return this._EIC;
    }

    /**
	 * Class member corresponding to the field 'Secondary LastName' in the PDF.
	 */
    private String _Secondary_LastName = "";

    /**
	 * Mutator Method for xSecondary LastName
	 * @param Secondary LastName the new value for 'Secondary LastName'
	 */
    public void set_Secondary_LastName(String _Secondary_LastName) {
        this._Secondary_LastName = _Secondary_LastName;
    }

    /**
	 * Accessor Method for xSecondary LastName
	 * @return the value of 'Secondary LastName'
	 */
    public String get_Secondary_LastName() {
        return this._Secondary_LastName;
    }

    /**
	 * Class member corresponding to the field 'FirstName' in the PDF.
	 */
    private String _FirstName = "";

    /**
	 * Mutator Method for xFirstName
	 * @param FirstName the new value for 'FirstName'
	 */
    public void set_FirstName(String _FirstName) {
        this._FirstName = _FirstName;
    }

    /**
	 * Accessor Method for xFirstName
	 * @return the value of 'FirstName'
	 */
    public String get_FirstName() {
        return this._FirstName;
    }

    /**
	 * Class member corresponding to the field 'FilingStatus' in the PDF.
	 */
    private String _FilingStatus = "";

    /**
	 * Mutator Method for xFilingStatus
	 * @param FilingStatus the new value for 'FilingStatus'
	 */
    public void set_FilingStatus(String _FilingStatus) {
        this._FilingStatus = _FilingStatus;
    }

    /**
	 * Accessor Method for xFilingStatus
	 * @return the value of 'FilingStatus'
	 */
    public String get_FilingStatus() {
        return this._FilingStatus;
    }

    /**
	 * Class member corresponding to the field 'NonGameWildlife' in the PDF.
	 */
    private String _NonGameWildlife = "";

    /**
	 * Mutator Method for xNonGameWildlife
	 * @param NonGameWildlife the new value for 'NonGameWildlife'
	 */
    public void set_NonGameWildlife(String _NonGameWildlife) {
        this._NonGameWildlife = _NonGameWildlife;
    }

    /**
	 * Accessor Method for xNonGameWildlife
	 * @return the value of 'NonGameWildlife'
	 */
    public String get_NonGameWildlife() {
        return this._NonGameWildlife;
    }

    /**
	 * Class member corresponding to the field 'StateMuniInterest' in the PDF.
	 */
    private String _StateMuniInterest = "";

    /**
	 * Mutator Method for xStateMuniInterest
	 * @param StateMuniInterest the new value for 'StateMuniInterest'
	 */
    public void set_StateMuniInterest(String _StateMuniInterest) {
        this._StateMuniInterest = _StateMuniInterest;
    }

    /**
	 * Accessor Method for xStateMuniInterest
	 * @return the value of 'StateMuniInterest'
	 */
    public String get_StateMuniInterest() {
        return this._StateMuniInterest;
    }

    /**
	 * Class member corresponding to the field 'RefundAmount' in the PDF.
	 */
    private String _RefundAmount = "";

    /**
	 * Mutator Method for xRefundAmount
	 * @param RefundAmount the new value for 'RefundAmount'
	 */
    public void set_RefundAmount(String _RefundAmount) {
        this._RefundAmount = _RefundAmount;
    }

    /**
	 * Accessor Method for xRefundAmount
	 * @return the value of 'RefundAmount'
	 */
    public String get_RefundAmount() {
        return this._RefundAmount;
    }

    /**
	 * Class member corresponding to the field 'TotalVTTaxesAndContributions' in the PDF.
	 */
    private String _TotalVTTaxesAndContributions = "";

    /**
	 * Mutator Method for xTotalVTTaxesAndContributions
	 * @param TotalVTTaxesAndContributions the new value for 'TotalVTTaxesAndContributions'
	 */
    public void set_TotalVTTaxesAndContributions(String _TotalVTTaxesAndContributions) {
        this._TotalVTTaxesAndContributions = _TotalVTTaxesAndContributions;
    }

    /**
	 * Accessor Method for xTotalVTTaxesAndContributions
	 * @return the value of 'TotalVTTaxesAndContributions'
	 */
    public String get_TotalVTTaxesAndContributions() {
        return this._TotalVTTaxesAndContributions;
    }

    /**
	 * Class member corresponding to the field 'EstimatedPaymentTotal' in the PDF.
	 */
    private String _EstimatedPaymentTotal = "";

    /**
	 * Mutator Method for xEstimatedPaymentTotal
	 * @param EstimatedPaymentTotal the new value for 'EstimatedPaymentTotal'
	 */
    public void set_EstimatedPaymentTotal(String _EstimatedPaymentTotal) {
        this._EstimatedPaymentTotal = _EstimatedPaymentTotal;
    }

    /**
	 * Accessor Method for xEstimatedPaymentTotal
	 * @return the value of 'EstimatedPaymentTotal'
	 */
    public String get_EstimatedPaymentTotal() {
        return this._EstimatedPaymentTotal;
    }

    /**
	 * Class member corresponding to the field 'TaxesPaidOtherStateCreditAmt' in the PDF.
	 */
    private String _TaxesPaidOtherStateCreditAmt = "";

    /**
	 * Mutator Method for xTaxesPaidOtherStateCreditAmt
	 * @param TaxesPaidOtherStateCreditAmt the new value for 'TaxesPaidOtherStateCreditAmt'
	 */
    public void set_TaxesPaidOtherStateCreditAmt(String _TaxesPaidOtherStateCreditAmt) {
        this._TaxesPaidOtherStateCreditAmt = _TaxesPaidOtherStateCreditAmt;
    }

    /**
	 * Accessor Method for xTaxesPaidOtherStateCreditAmt
	 * @return the value of 'TaxesPaidOtherStateCreditAmt'
	 */
    public String get_TaxesPaidOtherStateCreditAmt() {
        return this._TaxesPaidOtherStateCreditAmt;
    }

    /**
	 * Class member corresponding to the field 'SpouseCUPartnerSSN' in the PDF.
	 */
    private String _SpouseCUPartnerSSN = "";

    /**
	 * Mutator Method for xSpouseCUPartnerSSN
	 * @param SpouseCUPartnerSSN the new value for 'SpouseCUPartnerSSN'
	 */
    public void set_SpouseCUPartnerSSN(String _SpouseCUPartnerSSN) {
        this._SpouseCUPartnerSSN = _SpouseCUPartnerSSN;
    }

    /**
	 * Accessor Method for xSpouseCUPartnerSSN
	 * @return the value of 'SpouseCUPartnerSSN'
	 */
    public String get_SpouseCUPartnerSSN() {
        return this._SpouseCUPartnerSSN;
    }

    /**
	 * Class member corresponding to the field 'ItemizedDeductionAddBack' in the PDF.
	 */
    private String _ItemizedDeductionAddBack = "";

    /**
	 * Mutator Method for xItemizedDeductionAddBack
	 * @param ItemizedDeductionAddBack the new value for 'ItemizedDeductionAddBack'
	 */
    public void set_ItemizedDeductionAddBack(String _ItemizedDeductionAddBack) {
        this._ItemizedDeductionAddBack = _ItemizedDeductionAddBack;
    }

    /**
	 * Accessor Method for xItemizedDeductionAddBack
	 * @return the value of 'ItemizedDeductionAddBack'
	 */
    public String get_ItemizedDeductionAddBack() {
        return this._ItemizedDeductionAddBack;
    }

    /**
	 * Class member corresponding to the field 'VTIncomeWithAdditions' in the PDF.
	 */
    private String _VTIncomeWithAdditions = "";

    /**
	 * Mutator Method for xVTIncomeWithAdditions
	 * @param VTIncomeWithAdditions the new value for 'VTIncomeWithAdditions'
	 */
    public void set_VTIncomeWithAdditions(String _VTIncomeWithAdditions) {
        this._VTIncomeWithAdditions = _VTIncomeWithAdditions;
    }

    /**
	 * Accessor Method for xVTIncomeWithAdditions
	 * @return the value of 'VTIncomeWithAdditions'
	 */
    public String get_VTIncomeWithAdditions() {
        return this._VTIncomeWithAdditions;
    }

    /**
	 * Class member corresponding to the field 'PreparerBusinessName' in the PDF.
	 */
    private String _PreparerBusinessName = "";

    /**
	 * Mutator Method for xPreparerBusinessName
	 * @param PreparerBusinessName the new value for 'PreparerBusinessName'
	 */
    public void set_PreparerBusinessName(String _PreparerBusinessName) {
        this._PreparerBusinessName = _PreparerBusinessName;
    }

    /**
	 * Accessor Method for xPreparerBusinessName
	 * @return the value of 'PreparerBusinessName'
	 */
    public String get_PreparerBusinessName() {
        return this._PreparerBusinessName;
    }

    /**
	 * Class member corresponding to the field 'TotalPaymentsAndCredits' in the PDF.
	 */
    private String _TotalPaymentsAndCredits = "";

    /**
	 * Mutator Method for xTotalPaymentsAndCredits
	 * @param TotalPaymentsAndCredits the new value for 'TotalPaymentsAndCredits'
	 */
    public void set_TotalPaymentsAndCredits(String _TotalPaymentsAndCredits) {
        this._TotalPaymentsAndCredits = _TotalPaymentsAndCredits;
    }

    /**
	 * Accessor Method for xTotalPaymentsAndCredits
	 * @return the value of 'TotalPaymentsAndCredits'
	 */
    public String get_TotalPaymentsAndCredits() {
        return this._TotalPaymentsAndCredits;
    }

    /**
	 * Class member corresponding to the field 'City' in the PDF.
	 */
    private String _City = "";

    /**
	 * Mutator Method for xCity
	 * @param City the new value for 'City'
	 */
    public void set_City(String _City) {
        this._City = _City;
    }

    /**
	 * Accessor Method for xCity
	 * @return the value of 'City'
	 */
    public String get_City() {
        return this._City;
    }

    /**
	 * Class member corresponding to the field 'RenterRebate' in the PDF.
	 */
    private String _RenterRebate = "";

    /**
	 * Mutator Method for xRenterRebate
	 * @param RenterRebate the new value for 'RenterRebate'
	 */
    public void set_RenterRebate(String _RenterRebate) {
        this._RenterRebate = _RenterRebate;
    }

    /**
	 * Accessor Method for xRenterRebate
	 * @return the value of 'RenterRebate'
	 */
    public String get_RenterRebate() {
        return this._RenterRebate;
    }

    /**
	 * Class member corresponding to the field 'Text16' in the PDF.
	 */
    private String _Text16 = "";

    /**
	 * Mutator Method for xText16
	 * @param Text16 the new value for 'Text16'
	 */
    public void set_Text16(String _Text16) {
        this._Text16 = _Text16;
    }

    /**
	 * Accessor Method for xText16
	 * @return the value of 'Text16'
	 */
    public String get_Text16() {
        return this._Text16;
    }

    /**
	 * Class member corresponding to the field 'InterestUSObligations' in the PDF.
	 */
    private String _InterestUSObligations = "";

    /**
	 * Mutator Method for xInterestUSObligations
	 * @param InterestUSObligations the new value for 'InterestUSObligations'
	 */
    public void set_InterestUSObligations(String _InterestUSObligations) {
        this._InterestUSObligations = _InterestUSObligations;
    }

    /**
	 * Accessor Method for xInterestUSObligations
	 * @return the value of 'InterestUSObligations'
	 */
    public String get_InterestUSObligations() {
        return this._InterestUSObligations;
    }

    /**
	 * Class member corresponding to the field 'State' in the PDF.
	 */
    private String _State = "";

    /**
	 * Mutator Method for xState
	 * @param State the new value for 'State'
	 */
    public void set_State(String _State) {
        this._State = _State;
    }

    /**
	 * Accessor Method for xState
	 * @return the value of 'State'
	 */
    public String get_State() {
        return this._State;
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
	 * Class member corresponding to the field 'OverpaymentCreditedNxtYr' in the PDF.
	 */
    private String _OverpaymentCreditedNxtYr = "";

    /**
	 * Mutator Method for xOverpaymentCreditedNxtYr
	 * @param OverpaymentCreditedNxtYr the new value for 'OverpaymentCreditedNxtYr'
	 */
    public void set_OverpaymentCreditedNxtYr(String _OverpaymentCreditedNxtYr) {
        this._OverpaymentCreditedNxtYr = _OverpaymentCreditedNxtYr;
    }

    /**
	 * Accessor Method for xOverpaymentCreditedNxtYr
	 * @return the value of 'OverpaymentCreditedNxtYr'
	 */
    public String get_OverpaymentCreditedNxtYr() {
        return this._OverpaymentCreditedNxtYr;
    }

    /**
	 * Class member corresponding to the field 'TotalSubtractions' in the PDF.
	 */
    private String _TotalSubtractions = "";

    /**
	 * Mutator Method for xTotalSubtractions
	 * @param TotalSubtractions the new value for 'TotalSubtractions'
	 */
    public void set_TotalSubtractions(String _TotalSubtractions) {
        this._TotalSubtractions = _TotalSubtractions;
    }

    /**
	 * Accessor Method for xTotalSubtractions
	 * @return the value of 'TotalSubtractions'
	 */
    public String get_TotalSubtractions() {
        return this._TotalSubtractions;
    }

    /**
	 * Class member corresponding to the field 'OtherSubtractions' in the PDF.
	 */
    private String _OtherSubtractions = "";

    /**
	 * Mutator Method for xOtherSubtractions
	 * @param OtherSubtractions the new value for 'OtherSubtractions'
	 */
    public void set_OtherSubtractions(String _OtherSubtractions) {
        this._OtherSubtractions = _OtherSubtractions;
    }

    /**
	 * Accessor Method for xOtherSubtractions
	 * @return the value of 'OtherSubtractions'
	 */
    public String get_OtherSubtractions() {
        return this._OtherSubtractions;
    }

    /**
	 * Class member corresponding to the field 'PreparerFirmIDNumber' in the PDF.
	 */
    private String _PreparerFirmIDNumber = "";

    /**
	 * Mutator Method for xPreparerFirmIDNumber
	 * @param PreparerFirmIDNumber the new value for 'PreparerFirmIDNumber'
	 */
    public void set_PreparerFirmIDNumber(String _PreparerFirmIDNumber) {
        this._PreparerFirmIDNumber = _PreparerFirmIDNumber;
    }

    /**
	 * Accessor Method for xPreparerFirmIDNumber
	 * @return the value of 'PreparerFirmIDNumber'
	 */
    public String get_PreparerFirmIDNumber() {
        return this._PreparerFirmIDNumber;
    }
}
