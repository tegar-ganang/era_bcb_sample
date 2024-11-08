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
 * Generated class for filling 'TY2009_VT_IN113.pdf'
 */
public class TY2009_VT_IN113 {

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
        fields.setFieldProperty("Federal DomesticProductionActivities", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal DomesticProductionActivities", "textfont", baseFont, null);
        fields.setField("Federal DomesticProductionActivities", this.get_Federal_DomesticProductionActivities());
        fields.setFieldProperty("Vermont TaxableIRADistributions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TaxableIRADistributions", "textfont", baseFont, null);
        fields.setField("Vermont TaxableIRADistributions", this.get_Vermont_TaxableIRADistributions());
        fields.setFieldProperty("Vermont UnemploymentCompensation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont UnemploymentCompensation", "textfont", baseFont, null);
        fields.setField("Vermont UnemploymentCompensation", this.get_Vermont_UnemploymentCompensation());
        fields.setFieldProperty("Federal FarmIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal FarmIncome", "textfont", baseFont, null);
        fields.setField("Federal FarmIncome", this.get_Federal_FarmIncome());
        fields.setFieldProperty("Vermont SelfEmploymentDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont SelfEmploymentDeductions", "textfont", baseFont, null);
        fields.setField("Vermont SelfEmploymentDeductions", this.get_Vermont_SelfEmploymentDeductions());
        fields.setFieldProperty("ResidentToDate", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ResidentToDate", "textfont", baseFont, null);
        fields.setField("ResidentToDate", this.get_ResidentToDate());
        fields.setFieldProperty("Vermont TotalAdjustments", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TotalAdjustments", "textfont", baseFont, null);
        fields.setField("Vermont TotalAdjustments", this.get_Vermont_TotalAdjustments());
        fields.setFieldProperty("Federal CapitalGainLoss", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal CapitalGainLoss", "textfont", baseFont, null);
        fields.setField("Federal CapitalGainLoss", this.get_Federal_CapitalGainLoss());
        fields.setFieldProperty("Text29", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text29", "textfont", baseFont, null);
        fields.setField("Text29", this.get_Text29());
        fields.setFieldProperty("Federal EducatorExpenses", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal EducatorExpenses", "textfont", baseFont, null);
        fields.setField("Federal EducatorExpenses", this.get_Federal_EducatorExpenses());
        fields.setFieldProperty("Vermont TotalIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TotalIncome", "textfont", baseFont, null);
        fields.setField("Vermont TotalIncome", this.get_Vermont_TotalIncome());
        fields.setFieldProperty("Vermont FarmIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont FarmIncome", "textfont", baseFont, null);
        fields.setField("Vermont FarmIncome", this.get_Vermont_FarmIncome());
        fields.setFieldProperty("Federal OtherDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal OtherDeductions", "textfont", baseFont, null);
        fields.setField("Federal OtherDeductions", this.get_Federal_OtherDeductions());
        fields.setFieldProperty("Vermont TaxableSocialSecurity", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TaxableSocialSecurity", "textfont", baseFont, null);
        fields.setField("Vermont TaxableSocialSecurity", this.get_Vermont_TaxableSocialSecurity());
        fields.setFieldProperty("RailroadRetirement", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RailroadRetirement", "textfont", baseFont, null);
        fields.setField("RailroadRetirement", this.get_RailroadRetirement());
        fields.setFieldProperty("Federal WagesSalariesTips", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal WagesSalariesTips", "textfont", baseFont, null);
        fields.setField("Federal WagesSalariesTips", this.get_Federal_WagesSalariesTips());
        fields.setFieldProperty("Vermont TaxableInterest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TaxableInterest", "textfont", baseFont, null);
        fields.setField("Vermont TaxableInterest", this.get_Vermont_TaxableInterest());
        fields.setFieldProperty("Federal UnemploymentCompensation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal UnemploymentCompensation", "textfont", baseFont, null);
        fields.setField("Federal UnemploymentCompensation", this.get_Federal_UnemploymentCompensation());
        fields.setFieldProperty("AdjustedGrossIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AdjustedGrossIncome", "textfont", baseFont, null);
        fields.setField("AdjustedGrossIncome", this.get_AdjustedGrossIncome());
        fields.setFieldProperty("Federal TaxableTaxRefunds", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TaxableTaxRefunds", "textfont", baseFont, null);
        fields.setField("Federal TaxableTaxRefunds", this.get_Federal_TaxableTaxRefunds());
        fields.setFieldProperty("VtIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VtIncome", "textfont", baseFont, null);
        fields.setField("VtIncome", this.get_VtIncome());
        fields.setFieldProperty("Federal TaxablePensionsAnnuities", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TaxablePensionsAnnuities", "textfont", baseFont, null);
        fields.setField("Federal TaxablePensionsAnnuities", this.get_Federal_TaxablePensionsAnnuities());
        fields.setFieldProperty("TotalNonVtIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalNonVtIncome", "textfont", baseFont, null);
        fields.setField("TotalNonVtIncome", this.get_TotalNonVtIncome());
        fields.setFieldProperty("ResidentFromDate", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ResidentFromDate", "textfont", baseFont, null);
        fields.setField("ResidentFromDate", this.get_ResidentFromDate());
        fields.setFieldProperty("Federal MovingExpenses", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal MovingExpenses", "textfont", baseFont, null);
        fields.setField("Federal MovingExpenses", this.get_Federal_MovingExpenses());
        fields.setFieldProperty("NonresidentCommericalFilm", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NonresidentCommericalFilm", "textfont", baseFont, null);
        fields.setField("NonresidentCommericalFilm", this.get_NonresidentCommericalFilm());
        fields.setFieldProperty("Vermont CapitalGainLoss", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont CapitalGainLoss", "textfont", baseFont, null);
        fields.setField("Vermont CapitalGainLoss", this.get_Vermont_CapitalGainLoss());
        fields.setFieldProperty("Federal TotalIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TotalIncome", "textfont", baseFont, null);
        fields.setField("Federal TotalIncome", this.get_Federal_TotalIncome());
        fields.setFieldProperty("Federal EarlyWithdrawlSavings", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal EarlyWithdrawlSavings", "textfont", baseFont, null);
        fields.setField("Federal EarlyWithdrawlSavings", this.get_Federal_EarlyWithdrawlSavings());
        fields.setFieldProperty("Federal AlimonyReceived", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal AlimonyReceived", "textfont", baseFont, null);
        fields.setField("Federal AlimonyReceived", this.get_Federal_AlimonyReceived());
        fields.setFieldProperty("Federal AlimonyPaid", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal AlimonyPaid", "textfont", baseFont, null);
        fields.setField("Federal AlimonyPaid", this.get_Federal_AlimonyPaid());
        fields.setFieldProperty("Federal Other", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal Other", "textfont", baseFont, null);
        fields.setField("Federal Other", this.get_Federal_Other());
        fields.setFieldProperty("Federal HealthSavingsAccount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal HealthSavingsAccount", "textfont", baseFont, null);
        fields.setField("Federal HealthSavingsAccount", this.get_Federal_HealthSavingsAccount());
        fields.setFieldProperty("Vermont EmployeeDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont EmployeeDeductions", "textfont", baseFont, null);
        fields.setField("Vermont EmployeeDeductions", this.get_Vermont_EmployeeDeductions());
        fields.setFieldProperty("ActiveDutyMIlitaryPay", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ActiveDutyMIlitaryPay", "textfont", baseFont, null);
        fields.setField("ActiveDutyMIlitaryPay", this.get_ActiveDutyMIlitaryPay());
        fields.setFieldProperty("DisabledPersonSupport", "textsize", new Float(20.2), null);
        fields.setFieldProperty("DisabledPersonSupport", "textfont", baseFont, null);
        fields.setField("DisabledPersonSupport", this.get_DisabledPersonSupport());
        fields.setFieldProperty("AmericansWithDisabilities", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AmericansWithDisabilities", "textfont", baseFont, null);
        fields.setField("AmericansWithDisabilities", this.get_AmericansWithDisabilities());
        fields.setFieldProperty("Vermont TaxableTaxRefunds", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TaxableTaxRefunds", "textfont", baseFont, null);
        fields.setField("Vermont TaxableTaxRefunds", this.get_Vermont_TaxableTaxRefunds());
        fields.setFieldProperty("VtTelecommunicationAuthority", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VtTelecommunicationAuthority", "textfont", baseFont, null);
        fields.setField("VtTelecommunicationAuthority", this.get_VtTelecommunicationAuthority());
        fields.setFieldProperty("Vermont HealthSavingsAccount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont HealthSavingsAccount", "textfont", baseFont, null);
        fields.setField("Vermont HealthSavingsAccount", this.get_Vermont_HealthSavingsAccount());
        fields.setFieldProperty("IncomeAdjustmentPct", "textsize", new Float(20.2), null);
        fields.setFieldProperty("IncomeAdjustmentPct", "textfont", baseFont, null);
        fields.setField("IncomeAdjustmentPct", this.get_IncomeAdjustmentPct());
        fields.setFieldProperty("Vermont BusinessIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont BusinessIncome", "textfont", baseFont, null);
        fields.setField("Vermont BusinessIncome", this.get_Vermont_BusinessIncome());
        fields.setFieldProperty("Federal TaxableInterest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TaxableInterest", "textfont", baseFont, null);
        fields.setField("Federal TaxableInterest", this.get_Federal_TaxableInterest());
        fields.setFieldProperty("Vermont PartnershipSCorpLLC", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont PartnershipSCorpLLC", "textfont", baseFont, null);
        fields.setField("Vermont PartnershipSCorpLLC", this.get_Vermont_PartnershipSCorpLLC());
        fields.setFieldProperty("Vermont AlimonyPaid", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont AlimonyPaid", "textfont", baseFont, null);
        fields.setField("Vermont AlimonyPaid", this.get_Vermont_AlimonyPaid());
        fields.setFieldProperty("Vermont Dividends", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont Dividends", "textfont", baseFont, null);
        fields.setField("Vermont Dividends", this.get_Vermont_Dividends());
        fields.setFieldProperty("Federal EducationDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal EducationDeductions", "textfont", baseFont, null);
        fields.setField("Federal EducationDeductions", this.get_Federal_EducationDeductions());
        fields.setFieldProperty("Vermont Other", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont Other", "textfont", baseFont, null);
        fields.setField("Vermont Other", this.get_Vermont_Other());
        fields.setFieldProperty("Vermont IRA", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont IRA", "textfont", baseFont, null);
        fields.setField("Vermont IRA", this.get_Vermont_IRA());
        fields.setFieldProperty("Federal SelfEmploymentDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal SelfEmploymentDeductions", "textfont", baseFont, null);
        fields.setField("Federal SelfEmploymentDeductions", this.get_Federal_SelfEmploymentDeductions());
        fields.setFieldProperty("NonVtIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("NonVtIncome", "textfont", baseFont, null);
        fields.setField("NonVtIncome", this.get_NonVtIncome());
        fields.setFieldProperty("Vermont AlimonyReceived", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont AlimonyReceived", "textfont", baseFont, null);
        fields.setField("Vermont AlimonyReceived", this.get_Vermont_AlimonyReceived());
        fields.setFieldProperty("Vermont OtherDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont OtherDeductions", "textfont", baseFont, null);
        fields.setField("Vermont OtherDeductions", this.get_Vermont_OtherDeductions());
        fields.setFieldProperty("Federal EmployeeDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal EmployeeDeductions", "textfont", baseFont, null);
        fields.setField("Federal EmployeeDeductions", this.get_Federal_EmployeeDeductions());
        fields.setFieldProperty("Federal BusinessIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal BusinessIncome", "textfont", baseFont, null);
        fields.setField("Federal BusinessIncome", this.get_Federal_BusinessIncome());
        fields.setFieldProperty("Vermont MovingExpenses", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont MovingExpenses", "textfont", baseFont, null);
        fields.setField("Vermont MovingExpenses", this.get_Vermont_MovingExpenses());
        fields.setFieldProperty("Federal IRA", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal IRA", "textfont", baseFont, null);
        fields.setField("Federal IRA", this.get_Federal_IRA());
        fields.setFieldProperty("Vermont RentsRoyaltiesEstatesTrusts", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont RentsRoyaltiesEstatesTrusts", "textfont", baseFont, null);
        fields.setField("Vermont RentsRoyaltiesEstatesTrusts", this.get_Vermont_RentsRoyaltiesEstatesTrusts());
        fields.setFieldProperty("Vermont EarlyWithdrawlSavings", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont EarlyWithdrawlSavings", "textfont", baseFont, null);
        fields.setField("Vermont EarlyWithdrawlSavings", this.get_Vermont_EarlyWithdrawlSavings());
        fields.setFieldProperty("Federal RentsRoyaltiesEstatesTrusts", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal RentsRoyaltiesEstatesTrusts", "textfont", baseFont, null);
        fields.setField("Federal RentsRoyaltiesEstatesTrusts", this.get_Federal_RentsRoyaltiesEstatesTrusts());
        fields.setFieldProperty("Vermont EducationDeductions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont EducationDeductions", "textfont", baseFont, null);
        fields.setField("Vermont EducationDeductions", this.get_Vermont_EducationDeductions());
        fields.setFieldProperty("ResidentState", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ResidentState", "textfont", baseFont, null);
        fields.setField("ResidentState", this.get_ResidentState());
        fields.setFieldProperty("FedEmpOpportunity", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FedEmpOpportunity", "textfont", baseFont, null);
        fields.setField("FedEmpOpportunity", this.get_FedEmpOpportunity());
        fields.setFieldProperty("Federal TaxableSocialSecurity", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TaxableSocialSecurity", "textfont", baseFont, null);
        fields.setField("Federal TaxableSocialSecurity", this.get_Federal_TaxableSocialSecurity());
        fields.setFieldProperty("VTPortionAGI", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTPortionAGI", "textfont", baseFont, null);
        fields.setField("VTPortionAGI", this.get_VTPortionAGI());
        fields.setFieldProperty("Federal TotalAdjustments", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TotalAdjustments", "textfont", baseFont, null);
        fields.setField("Federal TotalAdjustments", this.get_Federal_TotalAdjustments());
        fields.setFieldProperty("Vermont DomesticProductionActivities", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont DomesticProductionActivities", "textfont", baseFont, null);
        fields.setField("Vermont DomesticProductionActivities", this.get_Vermont_DomesticProductionActivities());
        fields.setFieldProperty("Vermont TaxablePensionsAnnuities", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont TaxablePensionsAnnuities", "textfont", baseFont, null);
        fields.setField("Vermont TaxablePensionsAnnuities", this.get_Vermont_TaxablePensionsAnnuities());
        fields.setFieldProperty("Federal PartnershipSCorpLLC", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal PartnershipSCorpLLC", "textfont", baseFont, null);
        fields.setField("Federal PartnershipSCorpLLC", this.get_Federal_PartnershipSCorpLLC());
        fields.setFieldProperty("Vermont WagesSalariesTips", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont WagesSalariesTips", "textfont", baseFont, null);
        fields.setField("Vermont WagesSalariesTips", this.get_Vermont_WagesSalariesTips());
        fields.setFieldProperty("Federal Dividends", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal Dividends", "textfont", baseFont, null);
        fields.setField("Federal Dividends", this.get_Federal_Dividends());
        fields.setFieldProperty("Vermont EducatorExpenses", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Vermont EducatorExpenses", "textfont", baseFont, null);
        fields.setField("Vermont EducatorExpenses", this.get_Vermont_EducatorExpenses());
        fields.setFieldProperty("Federal TaxableIRADistributions", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Federal TaxableIRADistributions", "textfont", baseFont, null);
        fields.setField("Federal TaxableIRADistributions", this.get_Federal_TaxableIRADistributions());
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
	 * Class member corresponding to the field 'Federal DomesticProductionActivities' in the PDF.
	 */
    private String _Federal_DomesticProductionActivities = "";

    /**
	 * Mutator Method for xFederal DomesticProductionActivities
	 * @param Federal DomesticProductionActivities the new value for 'Federal DomesticProductionActivities'
	 */
    public void set_Federal_DomesticProductionActivities(String _Federal_DomesticProductionActivities) {
        this._Federal_DomesticProductionActivities = _Federal_DomesticProductionActivities;
    }

    /**
	 * Accessor Method for xFederal DomesticProductionActivities
	 * @return the value of 'Federal DomesticProductionActivities'
	 */
    public String get_Federal_DomesticProductionActivities() {
        return this._Federal_DomesticProductionActivities;
    }

    /**
	 * Class member corresponding to the field 'Vermont TaxableIRADistributions' in the PDF.
	 */
    private String _Vermont_TaxableIRADistributions = "";

    /**
	 * Mutator Method for xVermont TaxableIRADistributions
	 * @param Vermont TaxableIRADistributions the new value for 'Vermont TaxableIRADistributions'
	 */
    public void set_Vermont_TaxableIRADistributions(String _Vermont_TaxableIRADistributions) {
        this._Vermont_TaxableIRADistributions = _Vermont_TaxableIRADistributions;
    }

    /**
	 * Accessor Method for xVermont TaxableIRADistributions
	 * @return the value of 'Vermont TaxableIRADistributions'
	 */
    public String get_Vermont_TaxableIRADistributions() {
        return this._Vermont_TaxableIRADistributions;
    }

    /**
	 * Class member corresponding to the field 'Vermont UnemploymentCompensation' in the PDF.
	 */
    private String _Vermont_UnemploymentCompensation = "";

    /**
	 * Mutator Method for xVermont UnemploymentCompensation
	 * @param Vermont UnemploymentCompensation the new value for 'Vermont UnemploymentCompensation'
	 */
    public void set_Vermont_UnemploymentCompensation(String _Vermont_UnemploymentCompensation) {
        this._Vermont_UnemploymentCompensation = _Vermont_UnemploymentCompensation;
    }

    /**
	 * Accessor Method for xVermont UnemploymentCompensation
	 * @return the value of 'Vermont UnemploymentCompensation'
	 */
    public String get_Vermont_UnemploymentCompensation() {
        return this._Vermont_UnemploymentCompensation;
    }

    /**
	 * Class member corresponding to the field 'Federal FarmIncome' in the PDF.
	 */
    private String _Federal_FarmIncome = "";

    /**
	 * Mutator Method for xFederal FarmIncome
	 * @param Federal FarmIncome the new value for 'Federal FarmIncome'
	 */
    public void set_Federal_FarmIncome(String _Federal_FarmIncome) {
        this._Federal_FarmIncome = _Federal_FarmIncome;
    }

    /**
	 * Accessor Method for xFederal FarmIncome
	 * @return the value of 'Federal FarmIncome'
	 */
    public String get_Federal_FarmIncome() {
        return this._Federal_FarmIncome;
    }

    /**
	 * Class member corresponding to the field 'Vermont SelfEmploymentDeductions' in the PDF.
	 */
    private String _Vermont_SelfEmploymentDeductions = "";

    /**
	 * Mutator Method for xVermont SelfEmploymentDeductions
	 * @param Vermont SelfEmploymentDeductions the new value for 'Vermont SelfEmploymentDeductions'
	 */
    public void set_Vermont_SelfEmploymentDeductions(String _Vermont_SelfEmploymentDeductions) {
        this._Vermont_SelfEmploymentDeductions = _Vermont_SelfEmploymentDeductions;
    }

    /**
	 * Accessor Method for xVermont SelfEmploymentDeductions
	 * @return the value of 'Vermont SelfEmploymentDeductions'
	 */
    public String get_Vermont_SelfEmploymentDeductions() {
        return this._Vermont_SelfEmploymentDeductions;
    }

    /**
	 * Class member corresponding to the field 'ResidentToDate' in the PDF.
	 */
    private String _ResidentToDate = "";

    /**
	 * Mutator Method for xResidentToDate
	 * @param ResidentToDate the new value for 'ResidentToDate'
	 */
    public void set_ResidentToDate(String _ResidentToDate) {
        this._ResidentToDate = _ResidentToDate;
    }

    /**
	 * Accessor Method for xResidentToDate
	 * @return the value of 'ResidentToDate'
	 */
    public String get_ResidentToDate() {
        return this._ResidentToDate;
    }

    /**
	 * Class member corresponding to the field 'Vermont TotalAdjustments' in the PDF.
	 */
    private String _Vermont_TotalAdjustments = "";

    /**
	 * Mutator Method for xVermont TotalAdjustments
	 * @param Vermont TotalAdjustments the new value for 'Vermont TotalAdjustments'
	 */
    public void set_Vermont_TotalAdjustments(String _Vermont_TotalAdjustments) {
        this._Vermont_TotalAdjustments = _Vermont_TotalAdjustments;
    }

    /**
	 * Accessor Method for xVermont TotalAdjustments
	 * @return the value of 'Vermont TotalAdjustments'
	 */
    public String get_Vermont_TotalAdjustments() {
        return this._Vermont_TotalAdjustments;
    }

    /**
	 * Class member corresponding to the field 'Federal CapitalGainLoss' in the PDF.
	 */
    private String _Federal_CapitalGainLoss = "";

    /**
	 * Mutator Method for xFederal CapitalGainLoss
	 * @param Federal CapitalGainLoss the new value for 'Federal CapitalGainLoss'
	 */
    public void set_Federal_CapitalGainLoss(String _Federal_CapitalGainLoss) {
        this._Federal_CapitalGainLoss = _Federal_CapitalGainLoss;
    }

    /**
	 * Accessor Method for xFederal CapitalGainLoss
	 * @return the value of 'Federal CapitalGainLoss'
	 */
    public String get_Federal_CapitalGainLoss() {
        return this._Federal_CapitalGainLoss;
    }

    /**
	 * Class member corresponding to the field 'Text29' in the PDF.
	 */
    private String _Text29 = "";

    /**
	 * Mutator Method for xText29
	 * @param Text29 the new value for 'Text29'
	 */
    public void set_Text29(String _Text29) {
        this._Text29 = _Text29;
    }

    /**
	 * Accessor Method for xText29
	 * @return the value of 'Text29'
	 */
    public String get_Text29() {
        return this._Text29;
    }

    /**
	 * Class member corresponding to the field 'Federal EducatorExpenses' in the PDF.
	 */
    private String _Federal_EducatorExpenses = "";

    /**
	 * Mutator Method for xFederal EducatorExpenses
	 * @param Federal EducatorExpenses the new value for 'Federal EducatorExpenses'
	 */
    public void set_Federal_EducatorExpenses(String _Federal_EducatorExpenses) {
        this._Federal_EducatorExpenses = _Federal_EducatorExpenses;
    }

    /**
	 * Accessor Method for xFederal EducatorExpenses
	 * @return the value of 'Federal EducatorExpenses'
	 */
    public String get_Federal_EducatorExpenses() {
        return this._Federal_EducatorExpenses;
    }

    /**
	 * Class member corresponding to the field 'Vermont TotalIncome' in the PDF.
	 */
    private String _Vermont_TotalIncome = "";

    /**
	 * Mutator Method for xVermont TotalIncome
	 * @param Vermont TotalIncome the new value for 'Vermont TotalIncome'
	 */
    public void set_Vermont_TotalIncome(String _Vermont_TotalIncome) {
        this._Vermont_TotalIncome = _Vermont_TotalIncome;
    }

    /**
	 * Accessor Method for xVermont TotalIncome
	 * @return the value of 'Vermont TotalIncome'
	 */
    public String get_Vermont_TotalIncome() {
        return this._Vermont_TotalIncome;
    }

    /**
	 * Class member corresponding to the field 'Vermont FarmIncome' in the PDF.
	 */
    private String _Vermont_FarmIncome = "";

    /**
	 * Mutator Method for xVermont FarmIncome
	 * @param Vermont FarmIncome the new value for 'Vermont FarmIncome'
	 */
    public void set_Vermont_FarmIncome(String _Vermont_FarmIncome) {
        this._Vermont_FarmIncome = _Vermont_FarmIncome;
    }

    /**
	 * Accessor Method for xVermont FarmIncome
	 * @return the value of 'Vermont FarmIncome'
	 */
    public String get_Vermont_FarmIncome() {
        return this._Vermont_FarmIncome;
    }

    /**
	 * Class member corresponding to the field 'Federal OtherDeductions' in the PDF.
	 */
    private String _Federal_OtherDeductions = "";

    /**
	 * Mutator Method for xFederal OtherDeductions
	 * @param Federal OtherDeductions the new value for 'Federal OtherDeductions'
	 */
    public void set_Federal_OtherDeductions(String _Federal_OtherDeductions) {
        this._Federal_OtherDeductions = _Federal_OtherDeductions;
    }

    /**
	 * Accessor Method for xFederal OtherDeductions
	 * @return the value of 'Federal OtherDeductions'
	 */
    public String get_Federal_OtherDeductions() {
        return this._Federal_OtherDeductions;
    }

    /**
	 * Class member corresponding to the field 'Vermont TaxableSocialSecurity' in the PDF.
	 */
    private String _Vermont_TaxableSocialSecurity = "";

    /**
	 * Mutator Method for xVermont TaxableSocialSecurity
	 * @param Vermont TaxableSocialSecurity the new value for 'Vermont TaxableSocialSecurity'
	 */
    public void set_Vermont_TaxableSocialSecurity(String _Vermont_TaxableSocialSecurity) {
        this._Vermont_TaxableSocialSecurity = _Vermont_TaxableSocialSecurity;
    }

    /**
	 * Accessor Method for xVermont TaxableSocialSecurity
	 * @return the value of 'Vermont TaxableSocialSecurity'
	 */
    public String get_Vermont_TaxableSocialSecurity() {
        return this._Vermont_TaxableSocialSecurity;
    }

    /**
	 * Class member corresponding to the field 'RailroadRetirement' in the PDF.
	 */
    private String _RailroadRetirement = "";

    /**
	 * Mutator Method for xRailroadRetirement
	 * @param RailroadRetirement the new value for 'RailroadRetirement'
	 */
    public void set_RailroadRetirement(String _RailroadRetirement) {
        this._RailroadRetirement = _RailroadRetirement;
    }

    /**
	 * Accessor Method for xRailroadRetirement
	 * @return the value of 'RailroadRetirement'
	 */
    public String get_RailroadRetirement() {
        return this._RailroadRetirement;
    }

    /**
	 * Class member corresponding to the field 'Federal WagesSalariesTips' in the PDF.
	 */
    private String _Federal_WagesSalariesTips = "";

    /**
	 * Mutator Method for xFederal WagesSalariesTips
	 * @param Federal WagesSalariesTips the new value for 'Federal WagesSalariesTips'
	 */
    public void set_Federal_WagesSalariesTips(String _Federal_WagesSalariesTips) {
        this._Federal_WagesSalariesTips = _Federal_WagesSalariesTips;
    }

    /**
	 * Accessor Method for xFederal WagesSalariesTips
	 * @return the value of 'Federal WagesSalariesTips'
	 */
    public String get_Federal_WagesSalariesTips() {
        return this._Federal_WagesSalariesTips;
    }

    /**
	 * Class member corresponding to the field 'Vermont TaxableInterest' in the PDF.
	 */
    private String _Vermont_TaxableInterest = "";

    /**
	 * Mutator Method for xVermont TaxableInterest
	 * @param Vermont TaxableInterest the new value for 'Vermont TaxableInterest'
	 */
    public void set_Vermont_TaxableInterest(String _Vermont_TaxableInterest) {
        this._Vermont_TaxableInterest = _Vermont_TaxableInterest;
    }

    /**
	 * Accessor Method for xVermont TaxableInterest
	 * @return the value of 'Vermont TaxableInterest'
	 */
    public String get_Vermont_TaxableInterest() {
        return this._Vermont_TaxableInterest;
    }

    /**
	 * Class member corresponding to the field 'Federal UnemploymentCompensation' in the PDF.
	 */
    private String _Federal_UnemploymentCompensation = "";

    /**
	 * Mutator Method for xFederal UnemploymentCompensation
	 * @param Federal UnemploymentCompensation the new value for 'Federal UnemploymentCompensation'
	 */
    public void set_Federal_UnemploymentCompensation(String _Federal_UnemploymentCompensation) {
        this._Federal_UnemploymentCompensation = _Federal_UnemploymentCompensation;
    }

    /**
	 * Accessor Method for xFederal UnemploymentCompensation
	 * @return the value of 'Federal UnemploymentCompensation'
	 */
    public String get_Federal_UnemploymentCompensation() {
        return this._Federal_UnemploymentCompensation;
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
	 * Class member corresponding to the field 'Federal TaxableTaxRefunds' in the PDF.
	 */
    private String _Federal_TaxableTaxRefunds = "";

    /**
	 * Mutator Method for xFederal TaxableTaxRefunds
	 * @param Federal TaxableTaxRefunds the new value for 'Federal TaxableTaxRefunds'
	 */
    public void set_Federal_TaxableTaxRefunds(String _Federal_TaxableTaxRefunds) {
        this._Federal_TaxableTaxRefunds = _Federal_TaxableTaxRefunds;
    }

    /**
	 * Accessor Method for xFederal TaxableTaxRefunds
	 * @return the value of 'Federal TaxableTaxRefunds'
	 */
    public String get_Federal_TaxableTaxRefunds() {
        return this._Federal_TaxableTaxRefunds;
    }

    /**
	 * Class member corresponding to the field 'VtIncome' in the PDF.
	 */
    private String _VtIncome = "";

    /**
	 * Mutator Method for xVtIncome
	 * @param VtIncome the new value for 'VtIncome'
	 */
    public void set_VtIncome(String _VtIncome) {
        this._VtIncome = _VtIncome;
    }

    /**
	 * Accessor Method for xVtIncome
	 * @return the value of 'VtIncome'
	 */
    public String get_VtIncome() {
        return this._VtIncome;
    }

    /**
	 * Class member corresponding to the field 'Federal TaxablePensionsAnnuities' in the PDF.
	 */
    private String _Federal_TaxablePensionsAnnuities = "";

    /**
	 * Mutator Method for xFederal TaxablePensionsAnnuities
	 * @param Federal TaxablePensionsAnnuities the new value for 'Federal TaxablePensionsAnnuities'
	 */
    public void set_Federal_TaxablePensionsAnnuities(String _Federal_TaxablePensionsAnnuities) {
        this._Federal_TaxablePensionsAnnuities = _Federal_TaxablePensionsAnnuities;
    }

    /**
	 * Accessor Method for xFederal TaxablePensionsAnnuities
	 * @return the value of 'Federal TaxablePensionsAnnuities'
	 */
    public String get_Federal_TaxablePensionsAnnuities() {
        return this._Federal_TaxablePensionsAnnuities;
    }

    /**
	 * Class member corresponding to the field 'TotalNonVtIncome' in the PDF.
	 */
    private String _TotalNonVtIncome = "";

    /**
	 * Mutator Method for xTotalNonVtIncome
	 * @param TotalNonVtIncome the new value for 'TotalNonVtIncome'
	 */
    public void set_TotalNonVtIncome(String _TotalNonVtIncome) {
        this._TotalNonVtIncome = _TotalNonVtIncome;
    }

    /**
	 * Accessor Method for xTotalNonVtIncome
	 * @return the value of 'TotalNonVtIncome'
	 */
    public String get_TotalNonVtIncome() {
        return this._TotalNonVtIncome;
    }

    /**
	 * Class member corresponding to the field 'ResidentFromDate' in the PDF.
	 */
    private String _ResidentFromDate = "";

    /**
	 * Mutator Method for xResidentFromDate
	 * @param ResidentFromDate the new value for 'ResidentFromDate'
	 */
    public void set_ResidentFromDate(String _ResidentFromDate) {
        this._ResidentFromDate = _ResidentFromDate;
    }

    /**
	 * Accessor Method for xResidentFromDate
	 * @return the value of 'ResidentFromDate'
	 */
    public String get_ResidentFromDate() {
        return this._ResidentFromDate;
    }

    /**
	 * Class member corresponding to the field 'Federal MovingExpenses' in the PDF.
	 */
    private String _Federal_MovingExpenses = "";

    /**
	 * Mutator Method for xFederal MovingExpenses
	 * @param Federal MovingExpenses the new value for 'Federal MovingExpenses'
	 */
    public void set_Federal_MovingExpenses(String _Federal_MovingExpenses) {
        this._Federal_MovingExpenses = _Federal_MovingExpenses;
    }

    /**
	 * Accessor Method for xFederal MovingExpenses
	 * @return the value of 'Federal MovingExpenses'
	 */
    public String get_Federal_MovingExpenses() {
        return this._Federal_MovingExpenses;
    }

    /**
	 * Class member corresponding to the field 'NonresidentCommericalFilm' in the PDF.
	 */
    private String _NonresidentCommericalFilm = "";

    /**
	 * Mutator Method for xNonresidentCommericalFilm
	 * @param NonresidentCommericalFilm the new value for 'NonresidentCommericalFilm'
	 */
    public void set_NonresidentCommericalFilm(String _NonresidentCommericalFilm) {
        this._NonresidentCommericalFilm = _NonresidentCommericalFilm;
    }

    /**
	 * Accessor Method for xNonresidentCommericalFilm
	 * @return the value of 'NonresidentCommericalFilm'
	 */
    public String get_NonresidentCommericalFilm() {
        return this._NonresidentCommericalFilm;
    }

    /**
	 * Class member corresponding to the field 'Vermont CapitalGainLoss' in the PDF.
	 */
    private String _Vermont_CapitalGainLoss = "";

    /**
	 * Mutator Method for xVermont CapitalGainLoss
	 * @param Vermont CapitalGainLoss the new value for 'Vermont CapitalGainLoss'
	 */
    public void set_Vermont_CapitalGainLoss(String _Vermont_CapitalGainLoss) {
        this._Vermont_CapitalGainLoss = _Vermont_CapitalGainLoss;
    }

    /**
	 * Accessor Method for xVermont CapitalGainLoss
	 * @return the value of 'Vermont CapitalGainLoss'
	 */
    public String get_Vermont_CapitalGainLoss() {
        return this._Vermont_CapitalGainLoss;
    }

    /**
	 * Class member corresponding to the field 'Federal TotalIncome' in the PDF.
	 */
    private String _Federal_TotalIncome = "";

    /**
	 * Mutator Method for xFederal TotalIncome
	 * @param Federal TotalIncome the new value for 'Federal TotalIncome'
	 */
    public void set_Federal_TotalIncome(String _Federal_TotalIncome) {
        this._Federal_TotalIncome = _Federal_TotalIncome;
    }

    /**
	 * Accessor Method for xFederal TotalIncome
	 * @return the value of 'Federal TotalIncome'
	 */
    public String get_Federal_TotalIncome() {
        return this._Federal_TotalIncome;
    }

    /**
	 * Class member corresponding to the field 'Federal EarlyWithdrawlSavings' in the PDF.
	 */
    private String _Federal_EarlyWithdrawlSavings = "";

    /**
	 * Mutator Method for xFederal EarlyWithdrawlSavings
	 * @param Federal EarlyWithdrawlSavings the new value for 'Federal EarlyWithdrawlSavings'
	 */
    public void set_Federal_EarlyWithdrawlSavings(String _Federal_EarlyWithdrawlSavings) {
        this._Federal_EarlyWithdrawlSavings = _Federal_EarlyWithdrawlSavings;
    }

    /**
	 * Accessor Method for xFederal EarlyWithdrawlSavings
	 * @return the value of 'Federal EarlyWithdrawlSavings'
	 */
    public String get_Federal_EarlyWithdrawlSavings() {
        return this._Federal_EarlyWithdrawlSavings;
    }

    /**
	 * Class member corresponding to the field 'Federal AlimonyReceived' in the PDF.
	 */
    private String _Federal_AlimonyReceived = "";

    /**
	 * Mutator Method for xFederal AlimonyReceived
	 * @param Federal AlimonyReceived the new value for 'Federal AlimonyReceived'
	 */
    public void set_Federal_AlimonyReceived(String _Federal_AlimonyReceived) {
        this._Federal_AlimonyReceived = _Federal_AlimonyReceived;
    }

    /**
	 * Accessor Method for xFederal AlimonyReceived
	 * @return the value of 'Federal AlimonyReceived'
	 */
    public String get_Federal_AlimonyReceived() {
        return this._Federal_AlimonyReceived;
    }

    /**
	 * Class member corresponding to the field 'Federal AlimonyPaid' in the PDF.
	 */
    private String _Federal_AlimonyPaid = "";

    /**
	 * Mutator Method for xFederal AlimonyPaid
	 * @param Federal AlimonyPaid the new value for 'Federal AlimonyPaid'
	 */
    public void set_Federal_AlimonyPaid(String _Federal_AlimonyPaid) {
        this._Federal_AlimonyPaid = _Federal_AlimonyPaid;
    }

    /**
	 * Accessor Method for xFederal AlimonyPaid
	 * @return the value of 'Federal AlimonyPaid'
	 */
    public String get_Federal_AlimonyPaid() {
        return this._Federal_AlimonyPaid;
    }

    /**
	 * Class member corresponding to the field 'Federal Other' in the PDF.
	 */
    private String _Federal_Other = "";

    /**
	 * Mutator Method for xFederal Other
	 * @param Federal Other the new value for 'Federal Other'
	 */
    public void set_Federal_Other(String _Federal_Other) {
        this._Federal_Other = _Federal_Other;
    }

    /**
	 * Accessor Method for xFederal Other
	 * @return the value of 'Federal Other'
	 */
    public String get_Federal_Other() {
        return this._Federal_Other;
    }

    /**
	 * Class member corresponding to the field 'Federal HealthSavingsAccount' in the PDF.
	 */
    private String _Federal_HealthSavingsAccount = "";

    /**
	 * Mutator Method for xFederal HealthSavingsAccount
	 * @param Federal HealthSavingsAccount the new value for 'Federal HealthSavingsAccount'
	 */
    public void set_Federal_HealthSavingsAccount(String _Federal_HealthSavingsAccount) {
        this._Federal_HealthSavingsAccount = _Federal_HealthSavingsAccount;
    }

    /**
	 * Accessor Method for xFederal HealthSavingsAccount
	 * @return the value of 'Federal HealthSavingsAccount'
	 */
    public String get_Federal_HealthSavingsAccount() {
        return this._Federal_HealthSavingsAccount;
    }

    /**
	 * Class member corresponding to the field 'Vermont EmployeeDeductions' in the PDF.
	 */
    private String _Vermont_EmployeeDeductions = "";

    /**
	 * Mutator Method for xVermont EmployeeDeductions
	 * @param Vermont EmployeeDeductions the new value for 'Vermont EmployeeDeductions'
	 */
    public void set_Vermont_EmployeeDeductions(String _Vermont_EmployeeDeductions) {
        this._Vermont_EmployeeDeductions = _Vermont_EmployeeDeductions;
    }

    /**
	 * Accessor Method for xVermont EmployeeDeductions
	 * @return the value of 'Vermont EmployeeDeductions'
	 */
    public String get_Vermont_EmployeeDeductions() {
        return this._Vermont_EmployeeDeductions;
    }

    /**
	 * Class member corresponding to the field 'ActiveDutyMIlitaryPay' in the PDF.
	 */
    private String _ActiveDutyMIlitaryPay = "";

    /**
	 * Mutator Method for xActiveDutyMIlitaryPay
	 * @param ActiveDutyMIlitaryPay the new value for 'ActiveDutyMIlitaryPay'
	 */
    public void set_ActiveDutyMIlitaryPay(String _ActiveDutyMIlitaryPay) {
        this._ActiveDutyMIlitaryPay = _ActiveDutyMIlitaryPay;
    }

    /**
	 * Accessor Method for xActiveDutyMIlitaryPay
	 * @return the value of 'ActiveDutyMIlitaryPay'
	 */
    public String get_ActiveDutyMIlitaryPay() {
        return this._ActiveDutyMIlitaryPay;
    }

    /**
	 * Class member corresponding to the field 'DisabledPersonSupport' in the PDF.
	 */
    private String _DisabledPersonSupport = "";

    /**
	 * Mutator Method for xDisabledPersonSupport
	 * @param DisabledPersonSupport the new value for 'DisabledPersonSupport'
	 */
    public void set_DisabledPersonSupport(String _DisabledPersonSupport) {
        this._DisabledPersonSupport = _DisabledPersonSupport;
    }

    /**
	 * Accessor Method for xDisabledPersonSupport
	 * @return the value of 'DisabledPersonSupport'
	 */
    public String get_DisabledPersonSupport() {
        return this._DisabledPersonSupport;
    }

    /**
	 * Class member corresponding to the field 'AmericansWithDisabilities' in the PDF.
	 */
    private String _AmericansWithDisabilities = "";

    /**
	 * Mutator Method for xAmericansWithDisabilities
	 * @param AmericansWithDisabilities the new value for 'AmericansWithDisabilities'
	 */
    public void set_AmericansWithDisabilities(String _AmericansWithDisabilities) {
        this._AmericansWithDisabilities = _AmericansWithDisabilities;
    }

    /**
	 * Accessor Method for xAmericansWithDisabilities
	 * @return the value of 'AmericansWithDisabilities'
	 */
    public String get_AmericansWithDisabilities() {
        return this._AmericansWithDisabilities;
    }

    /**
	 * Class member corresponding to the field 'Vermont TaxableTaxRefunds' in the PDF.
	 */
    private String _Vermont_TaxableTaxRefunds = "";

    /**
	 * Mutator Method for xVermont TaxableTaxRefunds
	 * @param Vermont TaxableTaxRefunds the new value for 'Vermont TaxableTaxRefunds'
	 */
    public void set_Vermont_TaxableTaxRefunds(String _Vermont_TaxableTaxRefunds) {
        this._Vermont_TaxableTaxRefunds = _Vermont_TaxableTaxRefunds;
    }

    /**
	 * Accessor Method for xVermont TaxableTaxRefunds
	 * @return the value of 'Vermont TaxableTaxRefunds'
	 */
    public String get_Vermont_TaxableTaxRefunds() {
        return this._Vermont_TaxableTaxRefunds;
    }

    /**
	 * Class member corresponding to the field 'VtTelecommunicationAuthority' in the PDF.
	 */
    private String _VtTelecommunicationAuthority = "";

    /**
	 * Mutator Method for xVtTelecommunicationAuthority
	 * @param VtTelecommunicationAuthority the new value for 'VtTelecommunicationAuthority'
	 */
    public void set_VtTelecommunicationAuthority(String _VtTelecommunicationAuthority) {
        this._VtTelecommunicationAuthority = _VtTelecommunicationAuthority;
    }

    /**
	 * Accessor Method for xVtTelecommunicationAuthority
	 * @return the value of 'VtTelecommunicationAuthority'
	 */
    public String get_VtTelecommunicationAuthority() {
        return this._VtTelecommunicationAuthority;
    }

    /**
	 * Class member corresponding to the field 'Vermont HealthSavingsAccount' in the PDF.
	 */
    private String _Vermont_HealthSavingsAccount = "";

    /**
	 * Mutator Method for xVermont HealthSavingsAccount
	 * @param Vermont HealthSavingsAccount the new value for 'Vermont HealthSavingsAccount'
	 */
    public void set_Vermont_HealthSavingsAccount(String _Vermont_HealthSavingsAccount) {
        this._Vermont_HealthSavingsAccount = _Vermont_HealthSavingsAccount;
    }

    /**
	 * Accessor Method for xVermont HealthSavingsAccount
	 * @return the value of 'Vermont HealthSavingsAccount'
	 */
    public String get_Vermont_HealthSavingsAccount() {
        return this._Vermont_HealthSavingsAccount;
    }

    /**
	 * Class member corresponding to the field 'IncomeAdjustmentPct' in the PDF.
	 */
    private String _IncomeAdjustmentPct = "";

    /**
	 * Mutator Method for xIncomeAdjustmentPct
	 * @param IncomeAdjustmentPct the new value for 'IncomeAdjustmentPct'
	 */
    public void set_IncomeAdjustmentPct(String _IncomeAdjustmentPct) {
        this._IncomeAdjustmentPct = _IncomeAdjustmentPct;
    }

    /**
	 * Accessor Method for xIncomeAdjustmentPct
	 * @return the value of 'IncomeAdjustmentPct'
	 */
    public String get_IncomeAdjustmentPct() {
        return this._IncomeAdjustmentPct;
    }

    /**
	 * Class member corresponding to the field 'Vermont BusinessIncome' in the PDF.
	 */
    private String _Vermont_BusinessIncome = "";

    /**
	 * Mutator Method for xVermont BusinessIncome
	 * @param Vermont BusinessIncome the new value for 'Vermont BusinessIncome'
	 */
    public void set_Vermont_BusinessIncome(String _Vermont_BusinessIncome) {
        this._Vermont_BusinessIncome = _Vermont_BusinessIncome;
    }

    /**
	 * Accessor Method for xVermont BusinessIncome
	 * @return the value of 'Vermont BusinessIncome'
	 */
    public String get_Vermont_BusinessIncome() {
        return this._Vermont_BusinessIncome;
    }

    /**
	 * Class member corresponding to the field 'Federal TaxableInterest' in the PDF.
	 */
    private String _Federal_TaxableInterest = "";

    /**
	 * Mutator Method for xFederal TaxableInterest
	 * @param Federal TaxableInterest the new value for 'Federal TaxableInterest'
	 */
    public void set_Federal_TaxableInterest(String _Federal_TaxableInterest) {
        this._Federal_TaxableInterest = _Federal_TaxableInterest;
    }

    /**
	 * Accessor Method for xFederal TaxableInterest
	 * @return the value of 'Federal TaxableInterest'
	 */
    public String get_Federal_TaxableInterest() {
        return this._Federal_TaxableInterest;
    }

    /**
	 * Class member corresponding to the field 'Vermont PartnershipSCorpLLC' in the PDF.
	 */
    private String _Vermont_PartnershipSCorpLLC = "";

    /**
	 * Mutator Method for xVermont PartnershipSCorpLLC
	 * @param Vermont PartnershipSCorpLLC the new value for 'Vermont PartnershipSCorpLLC'
	 */
    public void set_Vermont_PartnershipSCorpLLC(String _Vermont_PartnershipSCorpLLC) {
        this._Vermont_PartnershipSCorpLLC = _Vermont_PartnershipSCorpLLC;
    }

    /**
	 * Accessor Method for xVermont PartnershipSCorpLLC
	 * @return the value of 'Vermont PartnershipSCorpLLC'
	 */
    public String get_Vermont_PartnershipSCorpLLC() {
        return this._Vermont_PartnershipSCorpLLC;
    }

    /**
	 * Class member corresponding to the field 'Vermont AlimonyPaid' in the PDF.
	 */
    private String _Vermont_AlimonyPaid = "";

    /**
	 * Mutator Method for xVermont AlimonyPaid
	 * @param Vermont AlimonyPaid the new value for 'Vermont AlimonyPaid'
	 */
    public void set_Vermont_AlimonyPaid(String _Vermont_AlimonyPaid) {
        this._Vermont_AlimonyPaid = _Vermont_AlimonyPaid;
    }

    /**
	 * Accessor Method for xVermont AlimonyPaid
	 * @return the value of 'Vermont AlimonyPaid'
	 */
    public String get_Vermont_AlimonyPaid() {
        return this._Vermont_AlimonyPaid;
    }

    /**
	 * Class member corresponding to the field 'Vermont Dividends' in the PDF.
	 */
    private String _Vermont_Dividends = "";

    /**
	 * Mutator Method for xVermont Dividends
	 * @param Vermont Dividends the new value for 'Vermont Dividends'
	 */
    public void set_Vermont_Dividends(String _Vermont_Dividends) {
        this._Vermont_Dividends = _Vermont_Dividends;
    }

    /**
	 * Accessor Method for xVermont Dividends
	 * @return the value of 'Vermont Dividends'
	 */
    public String get_Vermont_Dividends() {
        return this._Vermont_Dividends;
    }

    /**
	 * Class member corresponding to the field 'Federal EducationDeductions' in the PDF.
	 */
    private String _Federal_EducationDeductions = "";

    /**
	 * Mutator Method for xFederal EducationDeductions
	 * @param Federal EducationDeductions the new value for 'Federal EducationDeductions'
	 */
    public void set_Federal_EducationDeductions(String _Federal_EducationDeductions) {
        this._Federal_EducationDeductions = _Federal_EducationDeductions;
    }

    /**
	 * Accessor Method for xFederal EducationDeductions
	 * @return the value of 'Federal EducationDeductions'
	 */
    public String get_Federal_EducationDeductions() {
        return this._Federal_EducationDeductions;
    }

    /**
	 * Class member corresponding to the field 'Vermont Other' in the PDF.
	 */
    private String _Vermont_Other = "";

    /**
	 * Mutator Method for xVermont Other
	 * @param Vermont Other the new value for 'Vermont Other'
	 */
    public void set_Vermont_Other(String _Vermont_Other) {
        this._Vermont_Other = _Vermont_Other;
    }

    /**
	 * Accessor Method for xVermont Other
	 * @return the value of 'Vermont Other'
	 */
    public String get_Vermont_Other() {
        return this._Vermont_Other;
    }

    /**
	 * Class member corresponding to the field 'Vermont IRA' in the PDF.
	 */
    private String _Vermont_IRA = "";

    /**
	 * Mutator Method for xVermont IRA
	 * @param Vermont IRA the new value for 'Vermont IRA'
	 */
    public void set_Vermont_IRA(String _Vermont_IRA) {
        this._Vermont_IRA = _Vermont_IRA;
    }

    /**
	 * Accessor Method for xVermont IRA
	 * @return the value of 'Vermont IRA'
	 */
    public String get_Vermont_IRA() {
        return this._Vermont_IRA;
    }

    /**
	 * Class member corresponding to the field 'Federal SelfEmploymentDeductions' in the PDF.
	 */
    private String _Federal_SelfEmploymentDeductions = "";

    /**
	 * Mutator Method for xFederal SelfEmploymentDeductions
	 * @param Federal SelfEmploymentDeductions the new value for 'Federal SelfEmploymentDeductions'
	 */
    public void set_Federal_SelfEmploymentDeductions(String _Federal_SelfEmploymentDeductions) {
        this._Federal_SelfEmploymentDeductions = _Federal_SelfEmploymentDeductions;
    }

    /**
	 * Accessor Method for xFederal SelfEmploymentDeductions
	 * @return the value of 'Federal SelfEmploymentDeductions'
	 */
    public String get_Federal_SelfEmploymentDeductions() {
        return this._Federal_SelfEmploymentDeductions;
    }

    /**
	 * Class member corresponding to the field 'NonVtIncome' in the PDF.
	 */
    private String _NonVtIncome = "";

    /**
	 * Mutator Method for xNonVtIncome
	 * @param NonVtIncome the new value for 'NonVtIncome'
	 */
    public void set_NonVtIncome(String _NonVtIncome) {
        this._NonVtIncome = _NonVtIncome;
    }

    /**
	 * Accessor Method for xNonVtIncome
	 * @return the value of 'NonVtIncome'
	 */
    public String get_NonVtIncome() {
        return this._NonVtIncome;
    }

    /**
	 * Class member corresponding to the field 'Vermont AlimonyReceived' in the PDF.
	 */
    private String _Vermont_AlimonyReceived = "";

    /**
	 * Mutator Method for xVermont AlimonyReceived
	 * @param Vermont AlimonyReceived the new value for 'Vermont AlimonyReceived'
	 */
    public void set_Vermont_AlimonyReceived(String _Vermont_AlimonyReceived) {
        this._Vermont_AlimonyReceived = _Vermont_AlimonyReceived;
    }

    /**
	 * Accessor Method for xVermont AlimonyReceived
	 * @return the value of 'Vermont AlimonyReceived'
	 */
    public String get_Vermont_AlimonyReceived() {
        return this._Vermont_AlimonyReceived;
    }

    /**
	 * Class member corresponding to the field 'Vermont OtherDeductions' in the PDF.
	 */
    private String _Vermont_OtherDeductions = "";

    /**
	 * Mutator Method for xVermont OtherDeductions
	 * @param Vermont OtherDeductions the new value for 'Vermont OtherDeductions'
	 */
    public void set_Vermont_OtherDeductions(String _Vermont_OtherDeductions) {
        this._Vermont_OtherDeductions = _Vermont_OtherDeductions;
    }

    /**
	 * Accessor Method for xVermont OtherDeductions
	 * @return the value of 'Vermont OtherDeductions'
	 */
    public String get_Vermont_OtherDeductions() {
        return this._Vermont_OtherDeductions;
    }

    /**
	 * Class member corresponding to the field 'Federal EmployeeDeductions' in the PDF.
	 */
    private String _Federal_EmployeeDeductions = "";

    /**
	 * Mutator Method for xFederal EmployeeDeductions
	 * @param Federal EmployeeDeductions the new value for 'Federal EmployeeDeductions'
	 */
    public void set_Federal_EmployeeDeductions(String _Federal_EmployeeDeductions) {
        this._Federal_EmployeeDeductions = _Federal_EmployeeDeductions;
    }

    /**
	 * Accessor Method for xFederal EmployeeDeductions
	 * @return the value of 'Federal EmployeeDeductions'
	 */
    public String get_Federal_EmployeeDeductions() {
        return this._Federal_EmployeeDeductions;
    }

    /**
	 * Class member corresponding to the field 'Federal BusinessIncome' in the PDF.
	 */
    private String _Federal_BusinessIncome = "";

    /**
	 * Mutator Method for xFederal BusinessIncome
	 * @param Federal BusinessIncome the new value for 'Federal BusinessIncome'
	 */
    public void set_Federal_BusinessIncome(String _Federal_BusinessIncome) {
        this._Federal_BusinessIncome = _Federal_BusinessIncome;
    }

    /**
	 * Accessor Method for xFederal BusinessIncome
	 * @return the value of 'Federal BusinessIncome'
	 */
    public String get_Federal_BusinessIncome() {
        return this._Federal_BusinessIncome;
    }

    /**
	 * Class member corresponding to the field 'Vermont MovingExpenses' in the PDF.
	 */
    private String _Vermont_MovingExpenses = "";

    /**
	 * Mutator Method for xVermont MovingExpenses
	 * @param Vermont MovingExpenses the new value for 'Vermont MovingExpenses'
	 */
    public void set_Vermont_MovingExpenses(String _Vermont_MovingExpenses) {
        this._Vermont_MovingExpenses = _Vermont_MovingExpenses;
    }

    /**
	 * Accessor Method for xVermont MovingExpenses
	 * @return the value of 'Vermont MovingExpenses'
	 */
    public String get_Vermont_MovingExpenses() {
        return this._Vermont_MovingExpenses;
    }

    /**
	 * Class member corresponding to the field 'Federal IRA' in the PDF.
	 */
    private String _Federal_IRA = "";

    /**
	 * Mutator Method for xFederal IRA
	 * @param Federal IRA the new value for 'Federal IRA'
	 */
    public void set_Federal_IRA(String _Federal_IRA) {
        this._Federal_IRA = _Federal_IRA;
    }

    /**
	 * Accessor Method for xFederal IRA
	 * @return the value of 'Federal IRA'
	 */
    public String get_Federal_IRA() {
        return this._Federal_IRA;
    }

    /**
	 * Class member corresponding to the field 'Vermont RentsRoyaltiesEstatesTrusts' in the PDF.
	 */
    private String _Vermont_RentsRoyaltiesEstatesTrusts = "";

    /**
	 * Mutator Method for xVermont RentsRoyaltiesEstatesTrusts
	 * @param Vermont RentsRoyaltiesEstatesTrusts the new value for 'Vermont RentsRoyaltiesEstatesTrusts'
	 */
    public void set_Vermont_RentsRoyaltiesEstatesTrusts(String _Vermont_RentsRoyaltiesEstatesTrusts) {
        this._Vermont_RentsRoyaltiesEstatesTrusts = _Vermont_RentsRoyaltiesEstatesTrusts;
    }

    /**
	 * Accessor Method for xVermont RentsRoyaltiesEstatesTrusts
	 * @return the value of 'Vermont RentsRoyaltiesEstatesTrusts'
	 */
    public String get_Vermont_RentsRoyaltiesEstatesTrusts() {
        return this._Vermont_RentsRoyaltiesEstatesTrusts;
    }

    /**
	 * Class member corresponding to the field 'Vermont EarlyWithdrawlSavings' in the PDF.
	 */
    private String _Vermont_EarlyWithdrawlSavings = "";

    /**
	 * Mutator Method for xVermont EarlyWithdrawlSavings
	 * @param Vermont EarlyWithdrawlSavings the new value for 'Vermont EarlyWithdrawlSavings'
	 */
    public void set_Vermont_EarlyWithdrawlSavings(String _Vermont_EarlyWithdrawlSavings) {
        this._Vermont_EarlyWithdrawlSavings = _Vermont_EarlyWithdrawlSavings;
    }

    /**
	 * Accessor Method for xVermont EarlyWithdrawlSavings
	 * @return the value of 'Vermont EarlyWithdrawlSavings'
	 */
    public String get_Vermont_EarlyWithdrawlSavings() {
        return this._Vermont_EarlyWithdrawlSavings;
    }

    /**
	 * Class member corresponding to the field 'Federal RentsRoyaltiesEstatesTrusts' in the PDF.
	 */
    private String _Federal_RentsRoyaltiesEstatesTrusts = "";

    /**
	 * Mutator Method for xFederal RentsRoyaltiesEstatesTrusts
	 * @param Federal RentsRoyaltiesEstatesTrusts the new value for 'Federal RentsRoyaltiesEstatesTrusts'
	 */
    public void set_Federal_RentsRoyaltiesEstatesTrusts(String _Federal_RentsRoyaltiesEstatesTrusts) {
        this._Federal_RentsRoyaltiesEstatesTrusts = _Federal_RentsRoyaltiesEstatesTrusts;
    }

    /**
	 * Accessor Method for xFederal RentsRoyaltiesEstatesTrusts
	 * @return the value of 'Federal RentsRoyaltiesEstatesTrusts'
	 */
    public String get_Federal_RentsRoyaltiesEstatesTrusts() {
        return this._Federal_RentsRoyaltiesEstatesTrusts;
    }

    /**
	 * Class member corresponding to the field 'Vermont EducationDeductions' in the PDF.
	 */
    private String _Vermont_EducationDeductions = "";

    /**
	 * Mutator Method for xVermont EducationDeductions
	 * @param Vermont EducationDeductions the new value for 'Vermont EducationDeductions'
	 */
    public void set_Vermont_EducationDeductions(String _Vermont_EducationDeductions) {
        this._Vermont_EducationDeductions = _Vermont_EducationDeductions;
    }

    /**
	 * Accessor Method for xVermont EducationDeductions
	 * @return the value of 'Vermont EducationDeductions'
	 */
    public String get_Vermont_EducationDeductions() {
        return this._Vermont_EducationDeductions;
    }

    /**
	 * Class member corresponding to the field 'ResidentState' in the PDF.
	 */
    private String _ResidentState = "";

    /**
	 * Mutator Method for xResidentState
	 * @param ResidentState the new value for 'ResidentState'
	 */
    public void set_ResidentState(String _ResidentState) {
        this._ResidentState = _ResidentState;
    }

    /**
	 * Accessor Method for xResidentState
	 * @return the value of 'ResidentState'
	 */
    public String get_ResidentState() {
        return this._ResidentState;
    }

    /**
	 * Class member corresponding to the field 'FedEmpOpportunity' in the PDF.
	 */
    private String _FedEmpOpportunity = "";

    /**
	 * Mutator Method for xFedEmpOpportunity
	 * @param FedEmpOpportunity the new value for 'FedEmpOpportunity'
	 */
    public void set_FedEmpOpportunity(String _FedEmpOpportunity) {
        this._FedEmpOpportunity = _FedEmpOpportunity;
    }

    /**
	 * Accessor Method for xFedEmpOpportunity
	 * @return the value of 'FedEmpOpportunity'
	 */
    public String get_FedEmpOpportunity() {
        return this._FedEmpOpportunity;
    }

    /**
	 * Class member corresponding to the field 'Federal TaxableSocialSecurity' in the PDF.
	 */
    private String _Federal_TaxableSocialSecurity = "";

    /**
	 * Mutator Method for xFederal TaxableSocialSecurity
	 * @param Federal TaxableSocialSecurity the new value for 'Federal TaxableSocialSecurity'
	 */
    public void set_Federal_TaxableSocialSecurity(String _Federal_TaxableSocialSecurity) {
        this._Federal_TaxableSocialSecurity = _Federal_TaxableSocialSecurity;
    }

    /**
	 * Accessor Method for xFederal TaxableSocialSecurity
	 * @return the value of 'Federal TaxableSocialSecurity'
	 */
    public String get_Federal_TaxableSocialSecurity() {
        return this._Federal_TaxableSocialSecurity;
    }

    /**
	 * Class member corresponding to the field 'VTPortionAGI' in the PDF.
	 */
    private String _VTPortionAGI = "";

    /**
	 * Mutator Method for xVTPortionAGI
	 * @param VTPortionAGI the new value for 'VTPortionAGI'
	 */
    public void set_VTPortionAGI(String _VTPortionAGI) {
        this._VTPortionAGI = _VTPortionAGI;
    }

    /**
	 * Accessor Method for xVTPortionAGI
	 * @return the value of 'VTPortionAGI'
	 */
    public String get_VTPortionAGI() {
        return this._VTPortionAGI;
    }

    /**
	 * Class member corresponding to the field 'Federal TotalAdjustments' in the PDF.
	 */
    private String _Federal_TotalAdjustments = "";

    /**
	 * Mutator Method for xFederal TotalAdjustments
	 * @param Federal TotalAdjustments the new value for 'Federal TotalAdjustments'
	 */
    public void set_Federal_TotalAdjustments(String _Federal_TotalAdjustments) {
        this._Federal_TotalAdjustments = _Federal_TotalAdjustments;
    }

    /**
	 * Accessor Method for xFederal TotalAdjustments
	 * @return the value of 'Federal TotalAdjustments'
	 */
    public String get_Federal_TotalAdjustments() {
        return this._Federal_TotalAdjustments;
    }

    /**
	 * Class member corresponding to the field 'Vermont DomesticProductionActivities' in the PDF.
	 */
    private String _Vermont_DomesticProductionActivities = "";

    /**
	 * Mutator Method for xVermont DomesticProductionActivities
	 * @param Vermont DomesticProductionActivities the new value for 'Vermont DomesticProductionActivities'
	 */
    public void set_Vermont_DomesticProductionActivities(String _Vermont_DomesticProductionActivities) {
        this._Vermont_DomesticProductionActivities = _Vermont_DomesticProductionActivities;
    }

    /**
	 * Accessor Method for xVermont DomesticProductionActivities
	 * @return the value of 'Vermont DomesticProductionActivities'
	 */
    public String get_Vermont_DomesticProductionActivities() {
        return this._Vermont_DomesticProductionActivities;
    }

    /**
	 * Class member corresponding to the field 'Vermont TaxablePensionsAnnuities' in the PDF.
	 */
    private String _Vermont_TaxablePensionsAnnuities = "";

    /**
	 * Mutator Method for xVermont TaxablePensionsAnnuities
	 * @param Vermont TaxablePensionsAnnuities the new value for 'Vermont TaxablePensionsAnnuities'
	 */
    public void set_Vermont_TaxablePensionsAnnuities(String _Vermont_TaxablePensionsAnnuities) {
        this._Vermont_TaxablePensionsAnnuities = _Vermont_TaxablePensionsAnnuities;
    }

    /**
	 * Accessor Method for xVermont TaxablePensionsAnnuities
	 * @return the value of 'Vermont TaxablePensionsAnnuities'
	 */
    public String get_Vermont_TaxablePensionsAnnuities() {
        return this._Vermont_TaxablePensionsAnnuities;
    }

    /**
	 * Class member corresponding to the field 'Federal PartnershipSCorpLLC' in the PDF.
	 */
    private String _Federal_PartnershipSCorpLLC = "";

    /**
	 * Mutator Method for xFederal PartnershipSCorpLLC
	 * @param Federal PartnershipSCorpLLC the new value for 'Federal PartnershipSCorpLLC'
	 */
    public void set_Federal_PartnershipSCorpLLC(String _Federal_PartnershipSCorpLLC) {
        this._Federal_PartnershipSCorpLLC = _Federal_PartnershipSCorpLLC;
    }

    /**
	 * Accessor Method for xFederal PartnershipSCorpLLC
	 * @return the value of 'Federal PartnershipSCorpLLC'
	 */
    public String get_Federal_PartnershipSCorpLLC() {
        return this._Federal_PartnershipSCorpLLC;
    }

    /**
	 * Class member corresponding to the field 'Vermont WagesSalariesTips' in the PDF.
	 */
    private String _Vermont_WagesSalariesTips = "";

    /**
	 * Mutator Method for xVermont WagesSalariesTips
	 * @param Vermont WagesSalariesTips the new value for 'Vermont WagesSalariesTips'
	 */
    public void set_Vermont_WagesSalariesTips(String _Vermont_WagesSalariesTips) {
        this._Vermont_WagesSalariesTips = _Vermont_WagesSalariesTips;
    }

    /**
	 * Accessor Method for xVermont WagesSalariesTips
	 * @return the value of 'Vermont WagesSalariesTips'
	 */
    public String get_Vermont_WagesSalariesTips() {
        return this._Vermont_WagesSalariesTips;
    }

    /**
	 * Class member corresponding to the field 'Federal Dividends' in the PDF.
	 */
    private String _Federal_Dividends = "";

    /**
	 * Mutator Method for xFederal Dividends
	 * @param Federal Dividends the new value for 'Federal Dividends'
	 */
    public void set_Federal_Dividends(String _Federal_Dividends) {
        this._Federal_Dividends = _Federal_Dividends;
    }

    /**
	 * Accessor Method for xFederal Dividends
	 * @return the value of 'Federal Dividends'
	 */
    public String get_Federal_Dividends() {
        return this._Federal_Dividends;
    }

    /**
	 * Class member corresponding to the field 'Vermont EducatorExpenses' in the PDF.
	 */
    private String _Vermont_EducatorExpenses = "";

    /**
	 * Mutator Method for xVermont EducatorExpenses
	 * @param Vermont EducatorExpenses the new value for 'Vermont EducatorExpenses'
	 */
    public void set_Vermont_EducatorExpenses(String _Vermont_EducatorExpenses) {
        this._Vermont_EducatorExpenses = _Vermont_EducatorExpenses;
    }

    /**
	 * Accessor Method for xVermont EducatorExpenses
	 * @return the value of 'Vermont EducatorExpenses'
	 */
    public String get_Vermont_EducatorExpenses() {
        return this._Vermont_EducatorExpenses;
    }

    /**
	 * Class member corresponding to the field 'Federal TaxableIRADistributions' in the PDF.
	 */
    private String _Federal_TaxableIRADistributions = "";

    /**
	 * Mutator Method for xFederal TaxableIRADistributions
	 * @param Federal TaxableIRADistributions the new value for 'Federal TaxableIRADistributions'
	 */
    public void set_Federal_TaxableIRADistributions(String _Federal_TaxableIRADistributions) {
        this._Federal_TaxableIRADistributions = _Federal_TaxableIRADistributions;
    }

    /**
	 * Accessor Method for xFederal TaxableIRADistributions
	 * @return the value of 'Federal TaxableIRADistributions'
	 */
    public String get_Federal_TaxableIRADistributions() {
        return this._Federal_TaxableIRADistributions;
    }
}
