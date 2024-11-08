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
 * Generated class for filling 'IN119'
 */
public class IN119 {

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
        fields.setFieldProperty("Earned LiftsElevatorsSprinklers", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned LiftsElevatorsSprinklers", "textfont", baseFont, null);
        fields.setField("Earned LiftsElevatorsSprinklers", this.get_Earned_LiftsElevatorsSprinklers());
        fields.setFieldProperty("AllowablePortion", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllowablePortion", "textfont", baseFont, null);
        fields.setField("AllowablePortion", this.get_AllowablePortion());
        fields.setFieldProperty("Carryforward CommericalBldgCodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward CommericalBldgCodeImprovements", "textfont", baseFont, null);
        fields.setField("Carryforward CommericalBldgCodeImprovements", this.get_Carryforward_CommericalBldgCodeImprovements());
        fields.setFieldProperty("CreditSubtotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditSubtotal", "textfont", baseFont, null);
        fields.setField("CreditSubtotal", this.get_CreditSubtotal());
        fields.setFieldProperty("Earned PayrollTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned PayrollTax", "textfont", baseFont, null);
        fields.setField("Earned PayrollTax", this.get_Earned_PayrollTax());
        fields.setFieldProperty("ApplicableTaxAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ApplicableTaxAmount", "textfont", baseFont, null);
        fields.setField("ApplicableTaxAmount", this.get_ApplicableTaxAmount());
        fields.setFieldProperty("Carryforward SustainableTechRDTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward SustainableTechRDTax", "textfont", baseFont, null);
        fields.setField("Carryforward SustainableTechRDTax", this.get_Carryforward_SustainableTechRDTax());
        fields.setFieldProperty("Earned CommericalBldgCodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned CommericalBldgCodeImprovements", "textfont", baseFont, null);
        fields.setField("Earned CommericalBldgCodeImprovements", this.get_Earned_CommericalBldgCodeImprovements());
        fields.setFieldProperty("Sch112TotalCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Sch112TotalCredits", "textfont", baseFont, null);
        fields.setField("Sch112TotalCredits", this.get_Sch112TotalCredits());
        fields.setFieldProperty("Earned ExportTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned ExportTax", "textfont", baseFont, null);
        fields.setField("Earned ExportTax", this.get_Earned_ExportTax());
        fields.setFieldProperty("CreditAmount CapitalInvestmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount CapitalInvestmentTax", "textfont", baseFont, null);
        fields.setField("CreditAmount CapitalInvestmentTax", this.get_CreditAmount_CapitalInvestmentTax());
        fields.setFieldProperty("CreditAmount PayrollTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount PayrollTax", "textfont", baseFont, null);
        fields.setField("CreditAmount PayrollTax", this.get_CreditAmount_PayrollTax());
        fields.setFieldProperty("VTTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTax", "textfont", baseFont, null);
        fields.setField("VTTax", this.get_VTTax());
        fields.setFieldProperty("CreditSubtotalAmt", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditSubtotalAmt", "textfont", baseFont, null);
        fields.setField("CreditSubtotalAmt", this.get_CreditSubtotalAmt());
        fields.setFieldProperty("Earned AffordableHousing", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned AffordableHousing", "textfont", baseFont, null);
        fields.setField("Earned AffordableHousing", this.get_Earned_AffordableHousing());
        fields.setFieldProperty("Earned HistoricRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned HistoricRehabilitation", "textfont", baseFont, null);
        fields.setField("Earned HistoricRehabilitation", this.get_Earned_HistoricRehabilitation());
        fields.setFieldProperty("TotalIncomeTaxCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalIncomeTaxCredits", "textfont", baseFont, null);
        fields.setField("TotalIncomeTaxCredits", this.get_TotalIncomeTaxCredits());
        fields.setFieldProperty("Carryforward LiftsElevatorsSprinklers", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward LiftsElevatorsSprinklers", "textfont", baseFont, null);
        fields.setField("Carryforward LiftsElevatorsSprinklers", this.get_Carryforward_LiftsElevatorsSprinklers());
        fields.setFieldProperty("TotalOSCR", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalOSCR", "textfont", baseFont, null);
        fields.setField("TotalOSCR", this.get_TotalOSCR());
        fields.setFieldProperty("VTHousingCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTHousingCredit", "textfont", baseFont, null);
        fields.setField("VTHousingCredit", this.get_VTHousingCredit());
        fields.setFieldProperty("Carryforward HighTechBusiness", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward HighTechBusiness", "textfont", baseFont, null);
        fields.setField("Carryforward HighTechBusiness", this.get_Carryforward_HighTechBusiness());
        fields.setFieldProperty("VTTaxAttributableK", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxAttributableK", "textfont", baseFont, null);
        fields.setField("VTTaxAttributableK", this.get_VTTaxAttributableK());
        fields.setFieldProperty("CreditClaimed", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditClaimed", "textfont", baseFont, null);
        fields.setField("CreditClaimed", this.get_CreditClaimed());
        fields.setFieldProperty("K1Income", "textsize", new Float(20.2), null);
        fields.setFieldProperty("K1Income", "textfont", baseFont, null);
        fields.setField("K1Income", this.get_K1Income());
        fields.setFieldProperty("Earned RehabilitationCertfiedHistBldg", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned RehabilitationCertfiedHistBldg", "textfont", baseFont, null);
        fields.setField("Earned RehabilitationCertfiedHistBldg", this.get_Earned_RehabilitationCertfiedHistBldg());
        fields.setFieldProperty("Carryforward ResearchDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward ResearchDevelopmentTax", "textfont", baseFont, null);
        fields.setField("Carryforward ResearchDevelopmentTax", this.get_Carryforward_ResearchDevelopmentTax());
        fields.setFieldProperty("Carryforward PayrollTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward PayrollTax", "textfont", baseFont, null);
        fields.setField("Carryforward PayrollTax", this.get_Carryforward_PayrollTax());
        fields.setFieldProperty("VTTaxAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxAmount", "textfont", baseFont, null);
        fields.setField("VTTaxAmount", this.get_VTTaxAmount());
        fields.setFieldProperty("CreditAmount RehabilitationCertfiedHistBldg", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount RehabilitationCertfiedHistBldg", "textfont", baseFont, null);
        fields.setField("CreditAmount RehabilitationCertfiedHistBldg", this.get_CreditAmount_RehabilitationCertfiedHistBldg());
        fields.setFieldProperty("Carryforward ExportTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward ExportTax", "textfont", baseFont, null);
        fields.setField("Carryforward ExportTax", this.get_Carryforward_ExportTax());
        fields.setFieldProperty("Text164", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text164", "textfont", baseFont, null);
        fields.setField("Text164", this.get_Text164());
        fields.setFieldProperty("VTCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTCredits", "textfont", baseFont, null);
        fields.setField("VTCredits", this.get_VTCredits());
        fields.setFieldProperty("Carryforward HistoricRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward HistoricRehabilitation", "textfont", baseFont, null);
        fields.setField("Carryforward HistoricRehabilitation", this.get_Carryforward_HistoricRehabilitation());
        fields.setFieldProperty("Carryforward FacadeImprovement", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward FacadeImprovement", "textfont", baseFont, null);
        fields.setField("Carryforward FacadeImprovement", this.get_Carryforward_FacadeImprovement());
        fields.setFieldProperty("Earned HighTechBusiness", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned HighTechBusiness", "textfont", baseFont, null);
        fields.setField("Earned HighTechBusiness", this.get_Earned_HighTechBusiness());
        fields.setFieldProperty("MaxAllowableVEPCCredit", "textsize", new Float(20.2), null);
        fields.setFieldProperty("MaxAllowableVEPCCredit", "textfont", baseFont, null);
        fields.setField("MaxAllowableVEPCCredit", this.get_MaxAllowableVEPCCredit());
        fields.setFieldProperty("CreditAmount LiftsElevatorsSprinklers", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount LiftsElevatorsSprinklers", "textfont", baseFont, null);
        fields.setField("CreditAmount LiftsElevatorsSprinklers", this.get_CreditAmount_LiftsElevatorsSprinklers());
        fields.setFieldProperty("Earned WorkforceDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned WorkforceDevelopmentTax", "textfont", baseFont, null);
        fields.setField("Earned WorkforceDevelopmentTax", this.get_Earned_WorkforceDevelopmentTax());
        fields.setFieldProperty("CreditAmount FacadeImprovement", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount FacadeImprovement", "textfont", baseFont, null);
        fields.setField("CreditAmount FacadeImprovement", this.get_CreditAmount_FacadeImprovement());
        fields.setFieldProperty("CreditAmount ExportTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount ExportTax", "textfont", baseFont, null);
        fields.setField("CreditAmount ExportTax", this.get_CreditAmount_ExportTax());
        fields.setFieldProperty("Carryforward WoodProductsManufacturer", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward WoodProductsManufacturer", "textfont", baseFont, null);
        fields.setField("Carryforward WoodProductsManufacturer", this.get_Carryforward_WoodProductsManufacturer());
        fields.setFieldProperty("Earned CodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned CodeImprovements", "textfont", baseFont, null);
        fields.setField("Earned CodeImprovements", this.get_Earned_CodeImprovements());
        fields.setFieldProperty("Carryforward CodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward CodeImprovements", "textfont", baseFont, null);
        fields.setField("Carryforward CodeImprovements", this.get_Carryforward_CodeImprovements());
        fields.setFieldProperty("Earned ResearchDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned ResearchDevelopmentTax", "textfont", baseFont, null);
        fields.setField("Earned ResearchDevelopmentTax", this.get_Earned_ResearchDevelopmentTax());
        fields.setFieldProperty("Earned CapitalInvestmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned CapitalInvestmentTax", "textfont", baseFont, null);
        fields.setField("Earned CapitalInvestmentTax", this.get_Earned_CapitalInvestmentTax());
        fields.setFieldProperty("VTAdjustedIncomeTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTAdjustedIncomeTax", "textfont", baseFont, null);
        fields.setField("VTAdjustedIncomeTax", this.get_VTAdjustedIncomeTax());
        fields.setFieldProperty("Earned SustainableTechRDTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned SustainableTechRDTax", "textfont", baseFont, null);
        fields.setField("Earned SustainableTechRDTax", this.get_Earned_SustainableTechRDTax());
        fields.setFieldProperty("CreditAmount ResearchDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount ResearchDevelopmentTax", "textfont", baseFont, null);
        fields.setField("CreditAmount ResearchDevelopmentTax", this.get_CreditAmount_ResearchDevelopmentTax());
        fields.setFieldProperty("Carryforward AffordableHousing", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward AffordableHousing", "textfont", baseFont, null);
        fields.setField("Carryforward AffordableHousing", this.get_Carryforward_AffordableHousing());
        fields.setFieldProperty("Carryforward CapitalInvestmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward CapitalInvestmentTax", "textfont", baseFont, null);
        fields.setField("Carryforward CapitalInvestmentTax", this.get_Carryforward_CapitalInvestmentTax());
        fields.setFieldProperty("EntityFEIN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EntityFEIN", "textfont", baseFont, null);
        fields.setField("EntityFEIN", this.get_EntityFEIN());
        fields.setFieldProperty("CreditAmount CodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount CodeImprovements", "textfont", baseFont, null);
        fields.setField("CreditAmount CodeImprovements", this.get_CreditAmount_CodeImprovements());
        fields.setFieldProperty("CreditAmount SustainableTechExport", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount SustainableTechExport", "textfont", baseFont, null);
        fields.setField("CreditAmount SustainableTechExport", this.get_CreditAmount_SustainableTechExport());
        fields.setFieldProperty("EntrepreneurSeedCapitalFund", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EntrepreneurSeedCapitalFund", "textfont", baseFont, null);
        fields.setField("EntrepreneurSeedCapitalFund", this.get_EntrepreneurSeedCapitalFund());
        fields.setFieldProperty("VTTaxSubtotal", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxSubtotal", "textfont", baseFont, null);
        fields.setField("VTTaxSubtotal", this.get_VTTaxSubtotal());
        fields.setFieldProperty("VTTaxAmountAfterCredits", "textsize", new Float(20.2), null);
        fields.setFieldProperty("VTTaxAmountAfterCredits", "textfont", baseFont, null);
        fields.setField("VTTaxAmountAfterCredits", this.get_VTTaxAmountAfterCredits());
        fields.setFieldProperty("CreditAmount WorkforceDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount WorkforceDevelopmentTax", "textfont", baseFont, null);
        fields.setField("CreditAmount WorkforceDevelopmentTax", this.get_CreditAmount_WorkforceDevelopmentTax());
        fields.setFieldProperty("CreditAmount CommericalBldgCodeImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount CommericalBldgCodeImprovements", "textfont", baseFont, null);
        fields.setField("CreditAmount CommericalBldgCodeImprovements", this.get_CreditAmount_CommericalBldgCodeImprovements());
        fields.setFieldProperty("Carryforward RehabilitationCertfiedHistBldg", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward RehabilitationCertfiedHistBldg", "textfont", baseFont, null);
        fields.setField("Carryforward RehabilitationCertfiedHistBldg", this.get_Carryforward_RehabilitationCertfiedHistBldg());
        fields.setFieldProperty("Carryforward WorkforceDevelopmentTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward WorkforceDevelopmentTax", "textfont", baseFont, null);
        fields.setField("Carryforward WorkforceDevelopmentTax", this.get_Carryforward_WorkforceDevelopmentTax());
        fields.setFieldProperty("EntityName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("EntityName", "textfont", baseFont, null);
        fields.setField("EntityName", this.get_EntityName());
        fields.setFieldProperty("CreditAmount WoodProductsManufacturer", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount WoodProductsManufacturer", "textfont", baseFont, null);
        fields.setField("CreditAmount WoodProductsManufacturer", this.get_CreditAmount_WoodProductsManufacturer());
        fields.setFieldProperty("CreditAmount HighTechBusiness", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount HighTechBusiness", "textfont", baseFont, null);
        fields.setField("CreditAmount HighTechBusiness", this.get_CreditAmount_HighTechBusiness());
        fields.setFieldProperty("Kratio", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Kratio", "textfont", baseFont, null);
        fields.setField("Kratio", this.get_Kratio());
        fields.setFieldProperty("TotalCreditAllowable", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalCreditAllowable", "textfont", baseFont, null);
        fields.setField("TotalCreditAllowable", this.get_TotalCreditAllowable());
        fields.setFieldProperty("Earned WoodProductsManufacturer", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned WoodProductsManufacturer", "textfont", baseFont, null);
        fields.setField("Earned WoodProductsManufacturer", this.get_Earned_WoodProductsManufacturer());
        fields.setFieldProperty("Text162", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Text162", "textfont", baseFont, null);
        fields.setField("Text162", this.get_Text162());
        fields.setFieldProperty("Carryforward OlderHistBldgRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward OlderHistBldgRehabilitation", "textfont", baseFont, null);
        fields.setField("Carryforward OlderHistBldgRehabilitation", this.get_Carryforward_OlderHistBldgRehabilitation());
        fields.setFieldProperty("Carryforward SustainableTechExport", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Carryforward SustainableTechExport", "textfont", baseFont, null);
        fields.setField("Carryforward SustainableTechExport", this.get_Carryforward_SustainableTechExport());
        fields.setFieldProperty("CreditAmount HistoricRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount HistoricRehabilitation", "textfont", baseFont, null);
        fields.setField("CreditAmount HistoricRehabilitation", this.get_CreditAmount_HistoricRehabilitation());
        fields.setFieldProperty("CreditAmount OlderHistBldgRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount OlderHistBldgRehabilitation", "textfont", baseFont, null);
        fields.setField("CreditAmount OlderHistBldgRehabilitation", this.get_CreditAmount_OlderHistBldgRehabilitation());
        fields.setFieldProperty("StatutoryCreditLimitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StatutoryCreditLimitation", "textfont", baseFont, null);
        fields.setField("StatutoryCreditLimitation", this.get_StatutoryCreditLimitation());
        fields.setFieldProperty("TotalEATICAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalEATICAmount", "textfont", baseFont, null);
        fields.setField("TotalEATICAmount", this.get_TotalEATICAmount());
        fields.setFieldProperty("Earned OlderHistBldgRehabilitation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned OlderHistBldgRehabilitation", "textfont", baseFont, null);
        fields.setField("Earned OlderHistBldgRehabilitation", this.get_Earned_OlderHistBldgRehabilitation());
        fields.setFieldProperty("CreditsWithoutEATIC", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditsWithoutEATIC", "textfont", baseFont, null);
        fields.setField("CreditsWithoutEATIC", this.get_CreditsWithoutEATIC());
        fields.setFieldProperty("WorksheetCreditAmount", "textsize", new Float(20.2), null);
        fields.setFieldProperty("WorksheetCreditAmount", "textfont", baseFont, null);
        fields.setField("WorksheetCreditAmount", this.get_WorksheetCreditAmount());
        fields.setFieldProperty("Earned SustainableTechExport", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned SustainableTechExport", "textfont", baseFont, null);
        fields.setField("Earned SustainableTechExport", this.get_Earned_SustainableTechExport());
        fields.setFieldProperty("CreditAmount SustainableTechRDTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount SustainableTechRDTax", "textfont", baseFont, null);
        fields.setField("CreditAmount SustainableTechRDTax", this.get_CreditAmount_SustainableTechRDTax());
        fields.setFieldProperty("Earned FacadeImprovement", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Earned FacadeImprovement", "textfont", baseFont, null);
        fields.setField("Earned FacadeImprovement", this.get_Earned_FacadeImprovement());
        fields.setFieldProperty("CreditAmount AffordableHousing", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CreditAmount AffordableHousing", "textfont", baseFont, null);
        fields.setField("CreditAmount AffordableHousing", this.get_CreditAmount_AffordableHousing());
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
	 * Class member corresponding to the field 'Earned LiftsElevatorsSprinklers' in the PDF.
	 */
    private String _Earned_LiftsElevatorsSprinklers = "";

    /**
	 * Mutator Method for xEarned LiftsElevatorsSprinklers
	 * @param Earned LiftsElevatorsSprinklers the new value for 'Earned LiftsElevatorsSprinklers'
	 */
    public void set_Earned_LiftsElevatorsSprinklers(String _Earned_LiftsElevatorsSprinklers) {
        this._Earned_LiftsElevatorsSprinklers = _Earned_LiftsElevatorsSprinklers;
    }

    /**
	 * Accessor Method for xEarned LiftsElevatorsSprinklers
	 * @return the value of 'Earned LiftsElevatorsSprinklers'
	 */
    public String get_Earned_LiftsElevatorsSprinklers() {
        return this._Earned_LiftsElevatorsSprinklers;
    }

    /**
	 * Class member corresponding to the field 'AllowablePortion' in the PDF.
	 */
    private String _AllowablePortion = "";

    /**
	 * Mutator Method for xAllowablePortion
	 * @param AllowablePortion the new value for 'AllowablePortion'
	 */
    public void set_AllowablePortion(String _AllowablePortion) {
        this._AllowablePortion = _AllowablePortion;
    }

    /**
	 * Accessor Method for xAllowablePortion
	 * @return the value of 'AllowablePortion'
	 */
    public String get_AllowablePortion() {
        return this._AllowablePortion;
    }

    /**
	 * Class member corresponding to the field 'Carryforward CommericalBldgCodeImprovements' in the PDF.
	 */
    private String _Carryforward_CommericalBldgCodeImprovements = "";

    /**
	 * Mutator Method for xCarryforward CommericalBldgCodeImprovements
	 * @param Carryforward CommericalBldgCodeImprovements the new value for 'Carryforward CommericalBldgCodeImprovements'
	 */
    public void set_Carryforward_CommericalBldgCodeImprovements(String _Carryforward_CommericalBldgCodeImprovements) {
        this._Carryforward_CommericalBldgCodeImprovements = _Carryforward_CommericalBldgCodeImprovements;
    }

    /**
	 * Accessor Method for xCarryforward CommericalBldgCodeImprovements
	 * @return the value of 'Carryforward CommericalBldgCodeImprovements'
	 */
    public String get_Carryforward_CommericalBldgCodeImprovements() {
        return this._Carryforward_CommericalBldgCodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'CreditSubtotal' in the PDF.
	 */
    private String _CreditSubtotal = "";

    /**
	 * Mutator Method for xCreditSubtotal
	 * @param CreditSubtotal the new value for 'CreditSubtotal'
	 */
    public void set_CreditSubtotal(String _CreditSubtotal) {
        this._CreditSubtotal = _CreditSubtotal;
    }

    /**
	 * Accessor Method for xCreditSubtotal
	 * @return the value of 'CreditSubtotal'
	 */
    public String get_CreditSubtotal() {
        return this._CreditSubtotal;
    }

    /**
	 * Class member corresponding to the field 'Earned PayrollTax' in the PDF.
	 */
    private String _Earned_PayrollTax = "";

    /**
	 * Mutator Method for xEarned PayrollTax
	 * @param Earned PayrollTax the new value for 'Earned PayrollTax'
	 */
    public void set_Earned_PayrollTax(String _Earned_PayrollTax) {
        this._Earned_PayrollTax = _Earned_PayrollTax;
    }

    /**
	 * Accessor Method for xEarned PayrollTax
	 * @return the value of 'Earned PayrollTax'
	 */
    public String get_Earned_PayrollTax() {
        return this._Earned_PayrollTax;
    }

    /**
	 * Class member corresponding to the field 'ApplicableTaxAmount' in the PDF.
	 */
    private String _ApplicableTaxAmount = "";

    /**
	 * Mutator Method for xApplicableTaxAmount
	 * @param ApplicableTaxAmount the new value for 'ApplicableTaxAmount'
	 */
    public void set_ApplicableTaxAmount(String _ApplicableTaxAmount) {
        this._ApplicableTaxAmount = _ApplicableTaxAmount;
    }

    /**
	 * Accessor Method for xApplicableTaxAmount
	 * @return the value of 'ApplicableTaxAmount'
	 */
    public String get_ApplicableTaxAmount() {
        return this._ApplicableTaxAmount;
    }

    /**
	 * Class member corresponding to the field 'Carryforward SustainableTechRDTax' in the PDF.
	 */
    private String _Carryforward_SustainableTechRDTax = "";

    /**
	 * Mutator Method for xCarryforward SustainableTechRDTax
	 * @param Carryforward SustainableTechRDTax the new value for 'Carryforward SustainableTechRDTax'
	 */
    public void set_Carryforward_SustainableTechRDTax(String _Carryforward_SustainableTechRDTax) {
        this._Carryforward_SustainableTechRDTax = _Carryforward_SustainableTechRDTax;
    }

    /**
	 * Accessor Method for xCarryforward SustainableTechRDTax
	 * @return the value of 'Carryforward SustainableTechRDTax'
	 */
    public String get_Carryforward_SustainableTechRDTax() {
        return this._Carryforward_SustainableTechRDTax;
    }

    /**
	 * Class member corresponding to the field 'Earned CommericalBldgCodeImprovements' in the PDF.
	 */
    private String _Earned_CommericalBldgCodeImprovements = "";

    /**
	 * Mutator Method for xEarned CommericalBldgCodeImprovements
	 * @param Earned CommericalBldgCodeImprovements the new value for 'Earned CommericalBldgCodeImprovements'
	 */
    public void set_Earned_CommericalBldgCodeImprovements(String _Earned_CommericalBldgCodeImprovements) {
        this._Earned_CommericalBldgCodeImprovements = _Earned_CommericalBldgCodeImprovements;
    }

    /**
	 * Accessor Method for xEarned CommericalBldgCodeImprovements
	 * @return the value of 'Earned CommericalBldgCodeImprovements'
	 */
    public String get_Earned_CommericalBldgCodeImprovements() {
        return this._Earned_CommericalBldgCodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'Sch112TotalCredits' in the PDF.
	 */
    private String _Sch112TotalCredits = "";

    /**
	 * Mutator Method for xSch112TotalCredits
	 * @param Sch112TotalCredits the new value for 'Sch112TotalCredits'
	 */
    public void set_Sch112TotalCredits(String _Sch112TotalCredits) {
        this._Sch112TotalCredits = _Sch112TotalCredits;
    }

    /**
	 * Accessor Method for xSch112TotalCredits
	 * @return the value of 'Sch112TotalCredits'
	 */
    public String get_Sch112TotalCredits() {
        return this._Sch112TotalCredits;
    }

    /**
	 * Class member corresponding to the field 'Earned ExportTax' in the PDF.
	 */
    private String _Earned_ExportTax = "";

    /**
	 * Mutator Method for xEarned ExportTax
	 * @param Earned ExportTax the new value for 'Earned ExportTax'
	 */
    public void set_Earned_ExportTax(String _Earned_ExportTax) {
        this._Earned_ExportTax = _Earned_ExportTax;
    }

    /**
	 * Accessor Method for xEarned ExportTax
	 * @return the value of 'Earned ExportTax'
	 */
    public String get_Earned_ExportTax() {
        return this._Earned_ExportTax;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount CapitalInvestmentTax' in the PDF.
	 */
    private String _CreditAmount_CapitalInvestmentTax = "";

    /**
	 * Mutator Method for xCreditAmount CapitalInvestmentTax
	 * @param CreditAmount CapitalInvestmentTax the new value for 'CreditAmount CapitalInvestmentTax'
	 */
    public void set_CreditAmount_CapitalInvestmentTax(String _CreditAmount_CapitalInvestmentTax) {
        this._CreditAmount_CapitalInvestmentTax = _CreditAmount_CapitalInvestmentTax;
    }

    /**
	 * Accessor Method for xCreditAmount CapitalInvestmentTax
	 * @return the value of 'CreditAmount CapitalInvestmentTax'
	 */
    public String get_CreditAmount_CapitalInvestmentTax() {
        return this._CreditAmount_CapitalInvestmentTax;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount PayrollTax' in the PDF.
	 */
    private String _CreditAmount_PayrollTax = "";

    /**
	 * Mutator Method for xCreditAmount PayrollTax
	 * @param CreditAmount PayrollTax the new value for 'CreditAmount PayrollTax'
	 */
    public void set_CreditAmount_PayrollTax(String _CreditAmount_PayrollTax) {
        this._CreditAmount_PayrollTax = _CreditAmount_PayrollTax;
    }

    /**
	 * Accessor Method for xCreditAmount PayrollTax
	 * @return the value of 'CreditAmount PayrollTax'
	 */
    public String get_CreditAmount_PayrollTax() {
        return this._CreditAmount_PayrollTax;
    }

    /**
	 * Class member corresponding to the field 'VTTax' in the PDF.
	 */
    private String _VTTax = "";

    /**
	 * Mutator Method for xVTTax
	 * @param VTTax the new value for 'VTTax'
	 */
    public void set_VTTax(String _VTTax) {
        this._VTTax = _VTTax;
    }

    /**
	 * Accessor Method for xVTTax
	 * @return the value of 'VTTax'
	 */
    public String get_VTTax() {
        return this._VTTax;
    }

    /**
	 * Class member corresponding to the field 'CreditSubtotalAmt' in the PDF.
	 */
    private String _CreditSubtotalAmt = "";

    /**
	 * Mutator Method for xCreditSubtotalAmt
	 * @param CreditSubtotalAmt the new value for 'CreditSubtotalAmt'
	 */
    public void set_CreditSubtotalAmt(String _CreditSubtotalAmt) {
        this._CreditSubtotalAmt = _CreditSubtotalAmt;
    }

    /**
	 * Accessor Method for xCreditSubtotalAmt
	 * @return the value of 'CreditSubtotalAmt'
	 */
    public String get_CreditSubtotalAmt() {
        return this._CreditSubtotalAmt;
    }

    /**
	 * Class member corresponding to the field 'Earned AffordableHousing' in the PDF.
	 */
    private String _Earned_AffordableHousing = "";

    /**
	 * Mutator Method for xEarned AffordableHousing
	 * @param Earned AffordableHousing the new value for 'Earned AffordableHousing'
	 */
    public void set_Earned_AffordableHousing(String _Earned_AffordableHousing) {
        this._Earned_AffordableHousing = _Earned_AffordableHousing;
    }

    /**
	 * Accessor Method for xEarned AffordableHousing
	 * @return the value of 'Earned AffordableHousing'
	 */
    public String get_Earned_AffordableHousing() {
        return this._Earned_AffordableHousing;
    }

    /**
	 * Class member corresponding to the field 'Earned HistoricRehabilitation' in the PDF.
	 */
    private String _Earned_HistoricRehabilitation = "";

    /**
	 * Mutator Method for xEarned HistoricRehabilitation
	 * @param Earned HistoricRehabilitation the new value for 'Earned HistoricRehabilitation'
	 */
    public void set_Earned_HistoricRehabilitation(String _Earned_HistoricRehabilitation) {
        this._Earned_HistoricRehabilitation = _Earned_HistoricRehabilitation;
    }

    /**
	 * Accessor Method for xEarned HistoricRehabilitation
	 * @return the value of 'Earned HistoricRehabilitation'
	 */
    public String get_Earned_HistoricRehabilitation() {
        return this._Earned_HistoricRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'TotalIncomeTaxCredits' in the PDF.
	 */
    private String _TotalIncomeTaxCredits = "";

    /**
	 * Mutator Method for xTotalIncomeTaxCredits
	 * @param TotalIncomeTaxCredits the new value for 'TotalIncomeTaxCredits'
	 */
    public void set_TotalIncomeTaxCredits(String _TotalIncomeTaxCredits) {
        this._TotalIncomeTaxCredits = _TotalIncomeTaxCredits;
    }

    /**
	 * Accessor Method for xTotalIncomeTaxCredits
	 * @return the value of 'TotalIncomeTaxCredits'
	 */
    public String get_TotalIncomeTaxCredits() {
        return this._TotalIncomeTaxCredits;
    }

    /**
	 * Class member corresponding to the field 'Carryforward LiftsElevatorsSprinklers' in the PDF.
	 */
    private String _Carryforward_LiftsElevatorsSprinklers = "";

    /**
	 * Mutator Method for xCarryforward LiftsElevatorsSprinklers
	 * @param Carryforward LiftsElevatorsSprinklers the new value for 'Carryforward LiftsElevatorsSprinklers'
	 */
    public void set_Carryforward_LiftsElevatorsSprinklers(String _Carryforward_LiftsElevatorsSprinklers) {
        this._Carryforward_LiftsElevatorsSprinklers = _Carryforward_LiftsElevatorsSprinklers;
    }

    /**
	 * Accessor Method for xCarryforward LiftsElevatorsSprinklers
	 * @return the value of 'Carryforward LiftsElevatorsSprinklers'
	 */
    public String get_Carryforward_LiftsElevatorsSprinklers() {
        return this._Carryforward_LiftsElevatorsSprinklers;
    }

    /**
	 * Class member corresponding to the field 'TotalOSCR' in the PDF.
	 */
    private String _TotalOSCR = "";

    /**
	 * Mutator Method for xTotalOSCR
	 * @param TotalOSCR the new value for 'TotalOSCR'
	 */
    public void set_TotalOSCR(String _TotalOSCR) {
        this._TotalOSCR = _TotalOSCR;
    }

    /**
	 * Accessor Method for xTotalOSCR
	 * @return the value of 'TotalOSCR'
	 */
    public String get_TotalOSCR() {
        return this._TotalOSCR;
    }

    /**
	 * Class member corresponding to the field 'VTHousingCredit' in the PDF.
	 */
    private String _VTHousingCredit = "";

    /**
	 * Mutator Method for xVTHousingCredit
	 * @param VTHousingCredit the new value for 'VTHousingCredit'
	 */
    public void set_VTHousingCredit(String _VTHousingCredit) {
        this._VTHousingCredit = _VTHousingCredit;
    }

    /**
	 * Accessor Method for xVTHousingCredit
	 * @return the value of 'VTHousingCredit'
	 */
    public String get_VTHousingCredit() {
        return this._VTHousingCredit;
    }

    /**
	 * Class member corresponding to the field 'Carryforward HighTechBusiness' in the PDF.
	 */
    private String _Carryforward_HighTechBusiness = "";

    /**
	 * Mutator Method for xCarryforward HighTechBusiness
	 * @param Carryforward HighTechBusiness the new value for 'Carryforward HighTechBusiness'
	 */
    public void set_Carryforward_HighTechBusiness(String _Carryforward_HighTechBusiness) {
        this._Carryforward_HighTechBusiness = _Carryforward_HighTechBusiness;
    }

    /**
	 * Accessor Method for xCarryforward HighTechBusiness
	 * @return the value of 'Carryforward HighTechBusiness'
	 */
    public String get_Carryforward_HighTechBusiness() {
        return this._Carryforward_HighTechBusiness;
    }

    /**
	 * Class member corresponding to the field 'VTTaxAttributableK' in the PDF.
	 */
    private String _VTTaxAttributableK = "";

    /**
	 * Mutator Method for xVTTaxAttributableK
	 * @param VTTaxAttributableK the new value for 'VTTaxAttributableK'
	 */
    public void set_VTTaxAttributableK(String _VTTaxAttributableK) {
        this._VTTaxAttributableK = _VTTaxAttributableK;
    }

    /**
	 * Accessor Method for xVTTaxAttributableK
	 * @return the value of 'VTTaxAttributableK'
	 */
    public String get_VTTaxAttributableK() {
        return this._VTTaxAttributableK;
    }

    /**
	 * Class member corresponding to the field 'CreditClaimed' in the PDF.
	 */
    private String _CreditClaimed = "";

    /**
	 * Mutator Method for xCreditClaimed
	 * @param CreditClaimed the new value for 'CreditClaimed'
	 */
    public void set_CreditClaimed(String _CreditClaimed) {
        this._CreditClaimed = _CreditClaimed;
    }

    /**
	 * Accessor Method for xCreditClaimed
	 * @return the value of 'CreditClaimed'
	 */
    public String get_CreditClaimed() {
        return this._CreditClaimed;
    }

    /**
	 * Class member corresponding to the field 'K1Income' in the PDF.
	 */
    private String _K1Income = "";

    /**
	 * Mutator Method for xK1Income
	 * @param K1Income the new value for 'K1Income'
	 */
    public void set_K1Income(String _K1Income) {
        this._K1Income = _K1Income;
    }

    /**
	 * Accessor Method for xK1Income
	 * @return the value of 'K1Income'
	 */
    public String get_K1Income() {
        return this._K1Income;
    }

    /**
	 * Class member corresponding to the field 'Earned RehabilitationCertfiedHistBldg' in the PDF.
	 */
    private String _Earned_RehabilitationCertfiedHistBldg = "";

    /**
	 * Mutator Method for xEarned RehabilitationCertfiedHistBldg
	 * @param Earned RehabilitationCertfiedHistBldg the new value for 'Earned RehabilitationCertfiedHistBldg'
	 */
    public void set_Earned_RehabilitationCertfiedHistBldg(String _Earned_RehabilitationCertfiedHistBldg) {
        this._Earned_RehabilitationCertfiedHistBldg = _Earned_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Accessor Method for xEarned RehabilitationCertfiedHistBldg
	 * @return the value of 'Earned RehabilitationCertfiedHistBldg'
	 */
    public String get_Earned_RehabilitationCertfiedHistBldg() {
        return this._Earned_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Class member corresponding to the field 'Carryforward ResearchDevelopmentTax' in the PDF.
	 */
    private String _Carryforward_ResearchDevelopmentTax = "";

    /**
	 * Mutator Method for xCarryforward ResearchDevelopmentTax
	 * @param Carryforward ResearchDevelopmentTax the new value for 'Carryforward ResearchDevelopmentTax'
	 */
    public void set_Carryforward_ResearchDevelopmentTax(String _Carryforward_ResearchDevelopmentTax) {
        this._Carryforward_ResearchDevelopmentTax = _Carryforward_ResearchDevelopmentTax;
    }

    /**
	 * Accessor Method for xCarryforward ResearchDevelopmentTax
	 * @return the value of 'Carryforward ResearchDevelopmentTax'
	 */
    public String get_Carryforward_ResearchDevelopmentTax() {
        return this._Carryforward_ResearchDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'Carryforward PayrollTax' in the PDF.
	 */
    private String _Carryforward_PayrollTax = "";

    /**
	 * Mutator Method for xCarryforward PayrollTax
	 * @param Carryforward PayrollTax the new value for 'Carryforward PayrollTax'
	 */
    public void set_Carryforward_PayrollTax(String _Carryforward_PayrollTax) {
        this._Carryforward_PayrollTax = _Carryforward_PayrollTax;
    }

    /**
	 * Accessor Method for xCarryforward PayrollTax
	 * @return the value of 'Carryforward PayrollTax'
	 */
    public String get_Carryforward_PayrollTax() {
        return this._Carryforward_PayrollTax;
    }

    /**
	 * Class member corresponding to the field 'VTTaxAmount' in the PDF.
	 */
    private String _VTTaxAmount = "";

    /**
	 * Mutator Method for xVTTaxAmount
	 * @param VTTaxAmount the new value for 'VTTaxAmount'
	 */
    public void set_VTTaxAmount(String _VTTaxAmount) {
        this._VTTaxAmount = _VTTaxAmount;
    }

    /**
	 * Accessor Method for xVTTaxAmount
	 * @return the value of 'VTTaxAmount'
	 */
    public String get_VTTaxAmount() {
        return this._VTTaxAmount;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount RehabilitationCertfiedHistBldg' in the PDF.
	 */
    private String _CreditAmount_RehabilitationCertfiedHistBldg = "";

    /**
	 * Mutator Method for xCreditAmount RehabilitationCertfiedHistBldg
	 * @param CreditAmount RehabilitationCertfiedHistBldg the new value for 'CreditAmount RehabilitationCertfiedHistBldg'
	 */
    public void set_CreditAmount_RehabilitationCertfiedHistBldg(String _CreditAmount_RehabilitationCertfiedHistBldg) {
        this._CreditAmount_RehabilitationCertfiedHistBldg = _CreditAmount_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Accessor Method for xCreditAmount RehabilitationCertfiedHistBldg
	 * @return the value of 'CreditAmount RehabilitationCertfiedHistBldg'
	 */
    public String get_CreditAmount_RehabilitationCertfiedHistBldg() {
        return this._CreditAmount_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Class member corresponding to the field 'Carryforward ExportTax' in the PDF.
	 */
    private String _Carryforward_ExportTax = "";

    /**
	 * Mutator Method for xCarryforward ExportTax
	 * @param Carryforward ExportTax the new value for 'Carryforward ExportTax'
	 */
    public void set_Carryforward_ExportTax(String _Carryforward_ExportTax) {
        this._Carryforward_ExportTax = _Carryforward_ExportTax;
    }

    /**
	 * Accessor Method for xCarryforward ExportTax
	 * @return the value of 'Carryforward ExportTax'
	 */
    public String get_Carryforward_ExportTax() {
        return this._Carryforward_ExportTax;
    }

    /**
	 * Class member corresponding to the field 'Text164' in the PDF.
	 */
    private String _Text164 = "";

    /**
	 * Mutator Method for xText164
	 * @param Text164 the new value for 'Text164'
	 */
    public void set_Text164(String _Text164) {
        this._Text164 = _Text164;
    }

    /**
	 * Accessor Method for xText164
	 * @return the value of 'Text164'
	 */
    public String get_Text164() {
        return this._Text164;
    }

    /**
	 * Class member corresponding to the field 'VTCredits' in the PDF.
	 */
    private String _VTCredits = "";

    /**
	 * Mutator Method for xVTCredits
	 * @param VTCredits the new value for 'VTCredits'
	 */
    public void set_VTCredits(String _VTCredits) {
        this._VTCredits = _VTCredits;
    }

    /**
	 * Accessor Method for xVTCredits
	 * @return the value of 'VTCredits'
	 */
    public String get_VTCredits() {
        return this._VTCredits;
    }

    /**
	 * Class member corresponding to the field 'Carryforward HistoricRehabilitation' in the PDF.
	 */
    private String _Carryforward_HistoricRehabilitation = "";

    /**
	 * Mutator Method for xCarryforward HistoricRehabilitation
	 * @param Carryforward HistoricRehabilitation the new value for 'Carryforward HistoricRehabilitation'
	 */
    public void set_Carryforward_HistoricRehabilitation(String _Carryforward_HistoricRehabilitation) {
        this._Carryforward_HistoricRehabilitation = _Carryforward_HistoricRehabilitation;
    }

    /**
	 * Accessor Method for xCarryforward HistoricRehabilitation
	 * @return the value of 'Carryforward HistoricRehabilitation'
	 */
    public String get_Carryforward_HistoricRehabilitation() {
        return this._Carryforward_HistoricRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'Carryforward FacadeImprovement' in the PDF.
	 */
    private String _Carryforward_FacadeImprovement = "";

    /**
	 * Mutator Method for xCarryforward FacadeImprovement
	 * @param Carryforward FacadeImprovement the new value for 'Carryforward FacadeImprovement'
	 */
    public void set_Carryforward_FacadeImprovement(String _Carryforward_FacadeImprovement) {
        this._Carryforward_FacadeImprovement = _Carryforward_FacadeImprovement;
    }

    /**
	 * Accessor Method for xCarryforward FacadeImprovement
	 * @return the value of 'Carryforward FacadeImprovement'
	 */
    public String get_Carryforward_FacadeImprovement() {
        return this._Carryforward_FacadeImprovement;
    }

    /**
	 * Class member corresponding to the field 'Earned HighTechBusiness' in the PDF.
	 */
    private String _Earned_HighTechBusiness = "";

    /**
	 * Mutator Method for xEarned HighTechBusiness
	 * @param Earned HighTechBusiness the new value for 'Earned HighTechBusiness'
	 */
    public void set_Earned_HighTechBusiness(String _Earned_HighTechBusiness) {
        this._Earned_HighTechBusiness = _Earned_HighTechBusiness;
    }

    /**
	 * Accessor Method for xEarned HighTechBusiness
	 * @return the value of 'Earned HighTechBusiness'
	 */
    public String get_Earned_HighTechBusiness() {
        return this._Earned_HighTechBusiness;
    }

    /**
	 * Class member corresponding to the field 'MaxAllowableVEPCCredit' in the PDF.
	 */
    private String _MaxAllowableVEPCCredit = "";

    /**
	 * Mutator Method for xMaxAllowableVEPCCredit
	 * @param MaxAllowableVEPCCredit the new value for 'MaxAllowableVEPCCredit'
	 */
    public void set_MaxAllowableVEPCCredit(String _MaxAllowableVEPCCredit) {
        this._MaxAllowableVEPCCredit = _MaxAllowableVEPCCredit;
    }

    /**
	 * Accessor Method for xMaxAllowableVEPCCredit
	 * @return the value of 'MaxAllowableVEPCCredit'
	 */
    public String get_MaxAllowableVEPCCredit() {
        return this._MaxAllowableVEPCCredit;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount LiftsElevatorsSprinklers' in the PDF.
	 */
    private String _CreditAmount_LiftsElevatorsSprinklers = "";

    /**
	 * Mutator Method for xCreditAmount LiftsElevatorsSprinklers
	 * @param CreditAmount LiftsElevatorsSprinklers the new value for 'CreditAmount LiftsElevatorsSprinklers'
	 */
    public void set_CreditAmount_LiftsElevatorsSprinklers(String _CreditAmount_LiftsElevatorsSprinklers) {
        this._CreditAmount_LiftsElevatorsSprinklers = _CreditAmount_LiftsElevatorsSprinklers;
    }

    /**
	 * Accessor Method for xCreditAmount LiftsElevatorsSprinklers
	 * @return the value of 'CreditAmount LiftsElevatorsSprinklers'
	 */
    public String get_CreditAmount_LiftsElevatorsSprinklers() {
        return this._CreditAmount_LiftsElevatorsSprinklers;
    }

    /**
	 * Class member corresponding to the field 'Earned WorkforceDevelopmentTax' in the PDF.
	 */
    private String _Earned_WorkforceDevelopmentTax = "";

    /**
	 * Mutator Method for xEarned WorkforceDevelopmentTax
	 * @param Earned WorkforceDevelopmentTax the new value for 'Earned WorkforceDevelopmentTax'
	 */
    public void set_Earned_WorkforceDevelopmentTax(String _Earned_WorkforceDevelopmentTax) {
        this._Earned_WorkforceDevelopmentTax = _Earned_WorkforceDevelopmentTax;
    }

    /**
	 * Accessor Method for xEarned WorkforceDevelopmentTax
	 * @return the value of 'Earned WorkforceDevelopmentTax'
	 */
    public String get_Earned_WorkforceDevelopmentTax() {
        return this._Earned_WorkforceDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount FacadeImprovement' in the PDF.
	 */
    private String _CreditAmount_FacadeImprovement = "";

    /**
	 * Mutator Method for xCreditAmount FacadeImprovement
	 * @param CreditAmount FacadeImprovement the new value for 'CreditAmount FacadeImprovement'
	 */
    public void set_CreditAmount_FacadeImprovement(String _CreditAmount_FacadeImprovement) {
        this._CreditAmount_FacadeImprovement = _CreditAmount_FacadeImprovement;
    }

    /**
	 * Accessor Method for xCreditAmount FacadeImprovement
	 * @return the value of 'CreditAmount FacadeImprovement'
	 */
    public String get_CreditAmount_FacadeImprovement() {
        return this._CreditAmount_FacadeImprovement;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount ExportTax' in the PDF.
	 */
    private String _CreditAmount_ExportTax = "";

    /**
	 * Mutator Method for xCreditAmount ExportTax
	 * @param CreditAmount ExportTax the new value for 'CreditAmount ExportTax'
	 */
    public void set_CreditAmount_ExportTax(String _CreditAmount_ExportTax) {
        this._CreditAmount_ExportTax = _CreditAmount_ExportTax;
    }

    /**
	 * Accessor Method for xCreditAmount ExportTax
	 * @return the value of 'CreditAmount ExportTax'
	 */
    public String get_CreditAmount_ExportTax() {
        return this._CreditAmount_ExportTax;
    }

    /**
	 * Class member corresponding to the field 'Carryforward WoodProductsManufacturer' in the PDF.
	 */
    private String _Carryforward_WoodProductsManufacturer = "";

    /**
	 * Mutator Method for xCarryforward WoodProductsManufacturer
	 * @param Carryforward WoodProductsManufacturer the new value for 'Carryforward WoodProductsManufacturer'
	 */
    public void set_Carryforward_WoodProductsManufacturer(String _Carryforward_WoodProductsManufacturer) {
        this._Carryforward_WoodProductsManufacturer = _Carryforward_WoodProductsManufacturer;
    }

    /**
	 * Accessor Method for xCarryforward WoodProductsManufacturer
	 * @return the value of 'Carryforward WoodProductsManufacturer'
	 */
    public String get_Carryforward_WoodProductsManufacturer() {
        return this._Carryforward_WoodProductsManufacturer;
    }

    /**
	 * Class member corresponding to the field 'Earned CodeImprovements' in the PDF.
	 */
    private String _Earned_CodeImprovements = "";

    /**
	 * Mutator Method for xEarned CodeImprovements
	 * @param Earned CodeImprovements the new value for 'Earned CodeImprovements'
	 */
    public void set_Earned_CodeImprovements(String _Earned_CodeImprovements) {
        this._Earned_CodeImprovements = _Earned_CodeImprovements;
    }

    /**
	 * Accessor Method for xEarned CodeImprovements
	 * @return the value of 'Earned CodeImprovements'
	 */
    public String get_Earned_CodeImprovements() {
        return this._Earned_CodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'Carryforward CodeImprovements' in the PDF.
	 */
    private String _Carryforward_CodeImprovements = "";

    /**
	 * Mutator Method for xCarryforward CodeImprovements
	 * @param Carryforward CodeImprovements the new value for 'Carryforward CodeImprovements'
	 */
    public void set_Carryforward_CodeImprovements(String _Carryforward_CodeImprovements) {
        this._Carryforward_CodeImprovements = _Carryforward_CodeImprovements;
    }

    /**
	 * Accessor Method for xCarryforward CodeImprovements
	 * @return the value of 'Carryforward CodeImprovements'
	 */
    public String get_Carryforward_CodeImprovements() {
        return this._Carryforward_CodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'Earned ResearchDevelopmentTax' in the PDF.
	 */
    private String _Earned_ResearchDevelopmentTax = "";

    /**
	 * Mutator Method for xEarned ResearchDevelopmentTax
	 * @param Earned ResearchDevelopmentTax the new value for 'Earned ResearchDevelopmentTax'
	 */
    public void set_Earned_ResearchDevelopmentTax(String _Earned_ResearchDevelopmentTax) {
        this._Earned_ResearchDevelopmentTax = _Earned_ResearchDevelopmentTax;
    }

    /**
	 * Accessor Method for xEarned ResearchDevelopmentTax
	 * @return the value of 'Earned ResearchDevelopmentTax'
	 */
    public String get_Earned_ResearchDevelopmentTax() {
        return this._Earned_ResearchDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'Earned CapitalInvestmentTax' in the PDF.
	 */
    private String _Earned_CapitalInvestmentTax = "";

    /**
	 * Mutator Method for xEarned CapitalInvestmentTax
	 * @param Earned CapitalInvestmentTax the new value for 'Earned CapitalInvestmentTax'
	 */
    public void set_Earned_CapitalInvestmentTax(String _Earned_CapitalInvestmentTax) {
        this._Earned_CapitalInvestmentTax = _Earned_CapitalInvestmentTax;
    }

    /**
	 * Accessor Method for xEarned CapitalInvestmentTax
	 * @return the value of 'Earned CapitalInvestmentTax'
	 */
    public String get_Earned_CapitalInvestmentTax() {
        return this._Earned_CapitalInvestmentTax;
    }

    /**
	 * Class member corresponding to the field 'VTAdjustedIncomeTax' in the PDF.
	 */
    private String _VTAdjustedIncomeTax = "";

    /**
	 * Mutator Method for xVTAdjustedIncomeTax
	 * @param VTAdjustedIncomeTax the new value for 'VTAdjustedIncomeTax'
	 */
    public void set_VTAdjustedIncomeTax(String _VTAdjustedIncomeTax) {
        this._VTAdjustedIncomeTax = _VTAdjustedIncomeTax;
    }

    /**
	 * Accessor Method for xVTAdjustedIncomeTax
	 * @return the value of 'VTAdjustedIncomeTax'
	 */
    public String get_VTAdjustedIncomeTax() {
        return this._VTAdjustedIncomeTax;
    }

    /**
	 * Class member corresponding to the field 'Earned SustainableTechRDTax' in the PDF.
	 */
    private String _Earned_SustainableTechRDTax = "";

    /**
	 * Mutator Method for xEarned SustainableTechRDTax
	 * @param Earned SustainableTechRDTax the new value for 'Earned SustainableTechRDTax'
	 */
    public void set_Earned_SustainableTechRDTax(String _Earned_SustainableTechRDTax) {
        this._Earned_SustainableTechRDTax = _Earned_SustainableTechRDTax;
    }

    /**
	 * Accessor Method for xEarned SustainableTechRDTax
	 * @return the value of 'Earned SustainableTechRDTax'
	 */
    public String get_Earned_SustainableTechRDTax() {
        return this._Earned_SustainableTechRDTax;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount ResearchDevelopmentTax' in the PDF.
	 */
    private String _CreditAmount_ResearchDevelopmentTax = "";

    /**
	 * Mutator Method for xCreditAmount ResearchDevelopmentTax
	 * @param CreditAmount ResearchDevelopmentTax the new value for 'CreditAmount ResearchDevelopmentTax'
	 */
    public void set_CreditAmount_ResearchDevelopmentTax(String _CreditAmount_ResearchDevelopmentTax) {
        this._CreditAmount_ResearchDevelopmentTax = _CreditAmount_ResearchDevelopmentTax;
    }

    /**
	 * Accessor Method for xCreditAmount ResearchDevelopmentTax
	 * @return the value of 'CreditAmount ResearchDevelopmentTax'
	 */
    public String get_CreditAmount_ResearchDevelopmentTax() {
        return this._CreditAmount_ResearchDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'Carryforward AffordableHousing' in the PDF.
	 */
    private String _Carryforward_AffordableHousing = "";

    /**
	 * Mutator Method for xCarryforward AffordableHousing
	 * @param Carryforward AffordableHousing the new value for 'Carryforward AffordableHousing'
	 */
    public void set_Carryforward_AffordableHousing(String _Carryforward_AffordableHousing) {
        this._Carryforward_AffordableHousing = _Carryforward_AffordableHousing;
    }

    /**
	 * Accessor Method for xCarryforward AffordableHousing
	 * @return the value of 'Carryforward AffordableHousing'
	 */
    public String get_Carryforward_AffordableHousing() {
        return this._Carryforward_AffordableHousing;
    }

    /**
	 * Class member corresponding to the field 'Carryforward CapitalInvestmentTax' in the PDF.
	 */
    private String _Carryforward_CapitalInvestmentTax = "";

    /**
	 * Mutator Method for xCarryforward CapitalInvestmentTax
	 * @param Carryforward CapitalInvestmentTax the new value for 'Carryforward CapitalInvestmentTax'
	 */
    public void set_Carryforward_CapitalInvestmentTax(String _Carryforward_CapitalInvestmentTax) {
        this._Carryforward_CapitalInvestmentTax = _Carryforward_CapitalInvestmentTax;
    }

    /**
	 * Accessor Method for xCarryforward CapitalInvestmentTax
	 * @return the value of 'Carryforward CapitalInvestmentTax'
	 */
    public String get_Carryforward_CapitalInvestmentTax() {
        return this._Carryforward_CapitalInvestmentTax;
    }

    /**
	 * Class member corresponding to the field 'EntityFEIN' in the PDF.
	 */
    private String _EntityFEIN = "";

    /**
	 * Mutator Method for xEntityFEIN
	 * @param EntityFEIN the new value for 'EntityFEIN'
	 */
    public void set_EntityFEIN(String _EntityFEIN) {
        this._EntityFEIN = _EntityFEIN;
    }

    /**
	 * Accessor Method for xEntityFEIN
	 * @return the value of 'EntityFEIN'
	 */
    public String get_EntityFEIN() {
        return this._EntityFEIN;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount CodeImprovements' in the PDF.
	 */
    private String _CreditAmount_CodeImprovements = "";

    /**
	 * Mutator Method for xCreditAmount CodeImprovements
	 * @param CreditAmount CodeImprovements the new value for 'CreditAmount CodeImprovements'
	 */
    public void set_CreditAmount_CodeImprovements(String _CreditAmount_CodeImprovements) {
        this._CreditAmount_CodeImprovements = _CreditAmount_CodeImprovements;
    }

    /**
	 * Accessor Method for xCreditAmount CodeImprovements
	 * @return the value of 'CreditAmount CodeImprovements'
	 */
    public String get_CreditAmount_CodeImprovements() {
        return this._CreditAmount_CodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount SustainableTechExport' in the PDF.
	 */
    private String _CreditAmount_SustainableTechExport = "";

    /**
	 * Mutator Method for xCreditAmount SustainableTechExport
	 * @param CreditAmount SustainableTechExport the new value for 'CreditAmount SustainableTechExport'
	 */
    public void set_CreditAmount_SustainableTechExport(String _CreditAmount_SustainableTechExport) {
        this._CreditAmount_SustainableTechExport = _CreditAmount_SustainableTechExport;
    }

    /**
	 * Accessor Method for xCreditAmount SustainableTechExport
	 * @return the value of 'CreditAmount SustainableTechExport'
	 */
    public String get_CreditAmount_SustainableTechExport() {
        return this._CreditAmount_SustainableTechExport;
    }

    /**
	 * Class member corresponding to the field 'EntrepreneurSeedCapitalFund' in the PDF.
	 */
    private String _EntrepreneurSeedCapitalFund = "";

    /**
	 * Mutator Method for xEntrepreneurSeedCapitalFund
	 * @param EntrepreneurSeedCapitalFund the new value for 'EntrepreneurSeedCapitalFund'
	 */
    public void set_EntrepreneurSeedCapitalFund(String _EntrepreneurSeedCapitalFund) {
        this._EntrepreneurSeedCapitalFund = _EntrepreneurSeedCapitalFund;
    }

    /**
	 * Accessor Method for xEntrepreneurSeedCapitalFund
	 * @return the value of 'EntrepreneurSeedCapitalFund'
	 */
    public String get_EntrepreneurSeedCapitalFund() {
        return this._EntrepreneurSeedCapitalFund;
    }

    /**
	 * Class member corresponding to the field 'VTTaxSubtotal' in the PDF.
	 */
    private String _VTTaxSubtotal = "";

    /**
	 * Mutator Method for xVTTaxSubtotal
	 * @param VTTaxSubtotal the new value for 'VTTaxSubtotal'
	 */
    public void set_VTTaxSubtotal(String _VTTaxSubtotal) {
        this._VTTaxSubtotal = _VTTaxSubtotal;
    }

    /**
	 * Accessor Method for xVTTaxSubtotal
	 * @return the value of 'VTTaxSubtotal'
	 */
    public String get_VTTaxSubtotal() {
        return this._VTTaxSubtotal;
    }

    /**
	 * Class member corresponding to the field 'VTTaxAmountAfterCredits' in the PDF.
	 */
    private String _VTTaxAmountAfterCredits = "";

    /**
	 * Mutator Method for xVTTaxAmountAfterCredits
	 * @param VTTaxAmountAfterCredits the new value for 'VTTaxAmountAfterCredits'
	 */
    public void set_VTTaxAmountAfterCredits(String _VTTaxAmountAfterCredits) {
        this._VTTaxAmountAfterCredits = _VTTaxAmountAfterCredits;
    }

    /**
	 * Accessor Method for xVTTaxAmountAfterCredits
	 * @return the value of 'VTTaxAmountAfterCredits'
	 */
    public String get_VTTaxAmountAfterCredits() {
        return this._VTTaxAmountAfterCredits;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount WorkforceDevelopmentTax' in the PDF.
	 */
    private String _CreditAmount_WorkforceDevelopmentTax = "";

    /**
	 * Mutator Method for xCreditAmount WorkforceDevelopmentTax
	 * @param CreditAmount WorkforceDevelopmentTax the new value for 'CreditAmount WorkforceDevelopmentTax'
	 */
    public void set_CreditAmount_WorkforceDevelopmentTax(String _CreditAmount_WorkforceDevelopmentTax) {
        this._CreditAmount_WorkforceDevelopmentTax = _CreditAmount_WorkforceDevelopmentTax;
    }

    /**
	 * Accessor Method for xCreditAmount WorkforceDevelopmentTax
	 * @return the value of 'CreditAmount WorkforceDevelopmentTax'
	 */
    public String get_CreditAmount_WorkforceDevelopmentTax() {
        return this._CreditAmount_WorkforceDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount CommericalBldgCodeImprovements' in the PDF.
	 */
    private String _CreditAmount_CommericalBldgCodeImprovements = "";

    /**
	 * Mutator Method for xCreditAmount CommericalBldgCodeImprovements
	 * @param CreditAmount CommericalBldgCodeImprovements the new value for 'CreditAmount CommericalBldgCodeImprovements'
	 */
    public void set_CreditAmount_CommericalBldgCodeImprovements(String _CreditAmount_CommericalBldgCodeImprovements) {
        this._CreditAmount_CommericalBldgCodeImprovements = _CreditAmount_CommericalBldgCodeImprovements;
    }

    /**
	 * Accessor Method for xCreditAmount CommericalBldgCodeImprovements
	 * @return the value of 'CreditAmount CommericalBldgCodeImprovements'
	 */
    public String get_CreditAmount_CommericalBldgCodeImprovements() {
        return this._CreditAmount_CommericalBldgCodeImprovements;
    }

    /**
	 * Class member corresponding to the field 'Carryforward RehabilitationCertfiedHistBldg' in the PDF.
	 */
    private String _Carryforward_RehabilitationCertfiedHistBldg = "";

    /**
	 * Mutator Method for xCarryforward RehabilitationCertfiedHistBldg
	 * @param Carryforward RehabilitationCertfiedHistBldg the new value for 'Carryforward RehabilitationCertfiedHistBldg'
	 */
    public void set_Carryforward_RehabilitationCertfiedHistBldg(String _Carryforward_RehabilitationCertfiedHistBldg) {
        this._Carryforward_RehabilitationCertfiedHistBldg = _Carryforward_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Accessor Method for xCarryforward RehabilitationCertfiedHistBldg
	 * @return the value of 'Carryforward RehabilitationCertfiedHistBldg'
	 */
    public String get_Carryforward_RehabilitationCertfiedHistBldg() {
        return this._Carryforward_RehabilitationCertfiedHistBldg;
    }

    /**
	 * Class member corresponding to the field 'Carryforward WorkforceDevelopmentTax' in the PDF.
	 */
    private String _Carryforward_WorkforceDevelopmentTax = "";

    /**
	 * Mutator Method for xCarryforward WorkforceDevelopmentTax
	 * @param Carryforward WorkforceDevelopmentTax the new value for 'Carryforward WorkforceDevelopmentTax'
	 */
    public void set_Carryforward_WorkforceDevelopmentTax(String _Carryforward_WorkforceDevelopmentTax) {
        this._Carryforward_WorkforceDevelopmentTax = _Carryforward_WorkforceDevelopmentTax;
    }

    /**
	 * Accessor Method for xCarryforward WorkforceDevelopmentTax
	 * @return the value of 'Carryforward WorkforceDevelopmentTax'
	 */
    public String get_Carryforward_WorkforceDevelopmentTax() {
        return this._Carryforward_WorkforceDevelopmentTax;
    }

    /**
	 * Class member corresponding to the field 'EntityName' in the PDF.
	 */
    private String _EntityName = "";

    /**
	 * Mutator Method for xEntityName
	 * @param EntityName the new value for 'EntityName'
	 */
    public void set_EntityName(String _EntityName) {
        this._EntityName = _EntityName;
    }

    /**
	 * Accessor Method for xEntityName
	 * @return the value of 'EntityName'
	 */
    public String get_EntityName() {
        return this._EntityName;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount WoodProductsManufacturer' in the PDF.
	 */
    private String _CreditAmount_WoodProductsManufacturer = "";

    /**
	 * Mutator Method for xCreditAmount WoodProductsManufacturer
	 * @param CreditAmount WoodProductsManufacturer the new value for 'CreditAmount WoodProductsManufacturer'
	 */
    public void set_CreditAmount_WoodProductsManufacturer(String _CreditAmount_WoodProductsManufacturer) {
        this._CreditAmount_WoodProductsManufacturer = _CreditAmount_WoodProductsManufacturer;
    }

    /**
	 * Accessor Method for xCreditAmount WoodProductsManufacturer
	 * @return the value of 'CreditAmount WoodProductsManufacturer'
	 */
    public String get_CreditAmount_WoodProductsManufacturer() {
        return this._CreditAmount_WoodProductsManufacturer;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount HighTechBusiness' in the PDF.
	 */
    private String _CreditAmount_HighTechBusiness = "";

    /**
	 * Mutator Method for xCreditAmount HighTechBusiness
	 * @param CreditAmount HighTechBusiness the new value for 'CreditAmount HighTechBusiness'
	 */
    public void set_CreditAmount_HighTechBusiness(String _CreditAmount_HighTechBusiness) {
        this._CreditAmount_HighTechBusiness = _CreditAmount_HighTechBusiness;
    }

    /**
	 * Accessor Method for xCreditAmount HighTechBusiness
	 * @return the value of 'CreditAmount HighTechBusiness'
	 */
    public String get_CreditAmount_HighTechBusiness() {
        return this._CreditAmount_HighTechBusiness;
    }

    /**
	 * Class member corresponding to the field 'Kratio' in the PDF.
	 */
    private String _Kratio = "";

    /**
	 * Mutator Method for xKratio
	 * @param Kratio the new value for 'Kratio'
	 */
    public void set_Kratio(String _Kratio) {
        this._Kratio = _Kratio;
    }

    /**
	 * Accessor Method for xKratio
	 * @return the value of 'Kratio'
	 */
    public String get_Kratio() {
        return this._Kratio;
    }

    /**
	 * Class member corresponding to the field 'TotalCreditAllowable' in the PDF.
	 */
    private String _TotalCreditAllowable = "";

    /**
	 * Mutator Method for xTotalCreditAllowable
	 * @param TotalCreditAllowable the new value for 'TotalCreditAllowable'
	 */
    public void set_TotalCreditAllowable(String _TotalCreditAllowable) {
        this._TotalCreditAllowable = _TotalCreditAllowable;
    }

    /**
	 * Accessor Method for xTotalCreditAllowable
	 * @return the value of 'TotalCreditAllowable'
	 */
    public String get_TotalCreditAllowable() {
        return this._TotalCreditAllowable;
    }

    /**
	 * Class member corresponding to the field 'Earned WoodProductsManufacturer' in the PDF.
	 */
    private String _Earned_WoodProductsManufacturer = "";

    /**
	 * Mutator Method for xEarned WoodProductsManufacturer
	 * @param Earned WoodProductsManufacturer the new value for 'Earned WoodProductsManufacturer'
	 */
    public void set_Earned_WoodProductsManufacturer(String _Earned_WoodProductsManufacturer) {
        this._Earned_WoodProductsManufacturer = _Earned_WoodProductsManufacturer;
    }

    /**
	 * Accessor Method for xEarned WoodProductsManufacturer
	 * @return the value of 'Earned WoodProductsManufacturer'
	 */
    public String get_Earned_WoodProductsManufacturer() {
        return this._Earned_WoodProductsManufacturer;
    }

    /**
	 * Class member corresponding to the field 'Text162' in the PDF.
	 */
    private String _Text162 = "";

    /**
	 * Mutator Method for xText162
	 * @param Text162 the new value for 'Text162'
	 */
    public void set_Text162(String _Text162) {
        this._Text162 = _Text162;
    }

    /**
	 * Accessor Method for xText162
	 * @return the value of 'Text162'
	 */
    public String get_Text162() {
        return this._Text162;
    }

    /**
	 * Class member corresponding to the field 'Carryforward OlderHistBldgRehabilitation' in the PDF.
	 */
    private String _Carryforward_OlderHistBldgRehabilitation = "";

    /**
	 * Mutator Method for xCarryforward OlderHistBldgRehabilitation
	 * @param Carryforward OlderHistBldgRehabilitation the new value for 'Carryforward OlderHistBldgRehabilitation'
	 */
    public void set_Carryforward_OlderHistBldgRehabilitation(String _Carryforward_OlderHistBldgRehabilitation) {
        this._Carryforward_OlderHistBldgRehabilitation = _Carryforward_OlderHistBldgRehabilitation;
    }

    /**
	 * Accessor Method for xCarryforward OlderHistBldgRehabilitation
	 * @return the value of 'Carryforward OlderHistBldgRehabilitation'
	 */
    public String get_Carryforward_OlderHistBldgRehabilitation() {
        return this._Carryforward_OlderHistBldgRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'Carryforward SustainableTechExport' in the PDF.
	 */
    private String _Carryforward_SustainableTechExport = "";

    /**
	 * Mutator Method for xCarryforward SustainableTechExport
	 * @param Carryforward SustainableTechExport the new value for 'Carryforward SustainableTechExport'
	 */
    public void set_Carryforward_SustainableTechExport(String _Carryforward_SustainableTechExport) {
        this._Carryforward_SustainableTechExport = _Carryforward_SustainableTechExport;
    }

    /**
	 * Accessor Method for xCarryforward SustainableTechExport
	 * @return the value of 'Carryforward SustainableTechExport'
	 */
    public String get_Carryforward_SustainableTechExport() {
        return this._Carryforward_SustainableTechExport;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount HistoricRehabilitation' in the PDF.
	 */
    private String _CreditAmount_HistoricRehabilitation = "";

    /**
	 * Mutator Method for xCreditAmount HistoricRehabilitation
	 * @param CreditAmount HistoricRehabilitation the new value for 'CreditAmount HistoricRehabilitation'
	 */
    public void set_CreditAmount_HistoricRehabilitation(String _CreditAmount_HistoricRehabilitation) {
        this._CreditAmount_HistoricRehabilitation = _CreditAmount_HistoricRehabilitation;
    }

    /**
	 * Accessor Method for xCreditAmount HistoricRehabilitation
	 * @return the value of 'CreditAmount HistoricRehabilitation'
	 */
    public String get_CreditAmount_HistoricRehabilitation() {
        return this._CreditAmount_HistoricRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount OlderHistBldgRehabilitation' in the PDF.
	 */
    private String _CreditAmount_OlderHistBldgRehabilitation = "";

    /**
	 * Mutator Method for xCreditAmount OlderHistBldgRehabilitation
	 * @param CreditAmount OlderHistBldgRehabilitation the new value for 'CreditAmount OlderHistBldgRehabilitation'
	 */
    public void set_CreditAmount_OlderHistBldgRehabilitation(String _CreditAmount_OlderHistBldgRehabilitation) {
        this._CreditAmount_OlderHistBldgRehabilitation = _CreditAmount_OlderHistBldgRehabilitation;
    }

    /**
	 * Accessor Method for xCreditAmount OlderHistBldgRehabilitation
	 * @return the value of 'CreditAmount OlderHistBldgRehabilitation'
	 */
    public String get_CreditAmount_OlderHistBldgRehabilitation() {
        return this._CreditAmount_OlderHistBldgRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'StatutoryCreditLimitation' in the PDF.
	 */
    private String _StatutoryCreditLimitation = "";

    /**
	 * Mutator Method for xStatutoryCreditLimitation
	 * @param StatutoryCreditLimitation the new value for 'StatutoryCreditLimitation'
	 */
    public void set_StatutoryCreditLimitation(String _StatutoryCreditLimitation) {
        this._StatutoryCreditLimitation = _StatutoryCreditLimitation;
    }

    /**
	 * Accessor Method for xStatutoryCreditLimitation
	 * @return the value of 'StatutoryCreditLimitation'
	 */
    public String get_StatutoryCreditLimitation() {
        return this._StatutoryCreditLimitation;
    }

    /**
	 * Class member corresponding to the field 'TotalEATICAmount' in the PDF.
	 */
    private String _TotalEATICAmount = "";

    /**
	 * Mutator Method for xTotalEATICAmount
	 * @param TotalEATICAmount the new value for 'TotalEATICAmount'
	 */
    public void set_TotalEATICAmount(String _TotalEATICAmount) {
        this._TotalEATICAmount = _TotalEATICAmount;
    }

    /**
	 * Accessor Method for xTotalEATICAmount
	 * @return the value of 'TotalEATICAmount'
	 */
    public String get_TotalEATICAmount() {
        return this._TotalEATICAmount;
    }

    /**
	 * Class member corresponding to the field 'Earned OlderHistBldgRehabilitation' in the PDF.
	 */
    private String _Earned_OlderHistBldgRehabilitation = "";

    /**
	 * Mutator Method for xEarned OlderHistBldgRehabilitation
	 * @param Earned OlderHistBldgRehabilitation the new value for 'Earned OlderHistBldgRehabilitation'
	 */
    public void set_Earned_OlderHistBldgRehabilitation(String _Earned_OlderHistBldgRehabilitation) {
        this._Earned_OlderHistBldgRehabilitation = _Earned_OlderHistBldgRehabilitation;
    }

    /**
	 * Accessor Method for xEarned OlderHistBldgRehabilitation
	 * @return the value of 'Earned OlderHistBldgRehabilitation'
	 */
    public String get_Earned_OlderHistBldgRehabilitation() {
        return this._Earned_OlderHistBldgRehabilitation;
    }

    /**
	 * Class member corresponding to the field 'CreditsWithoutEATIC' in the PDF.
	 */
    private String _CreditsWithoutEATIC = "";

    /**
	 * Mutator Method for xCreditsWithoutEATIC
	 * @param CreditsWithoutEATIC the new value for 'CreditsWithoutEATIC'
	 */
    public void set_CreditsWithoutEATIC(String _CreditsWithoutEATIC) {
        this._CreditsWithoutEATIC = _CreditsWithoutEATIC;
    }

    /**
	 * Accessor Method for xCreditsWithoutEATIC
	 * @return the value of 'CreditsWithoutEATIC'
	 */
    public String get_CreditsWithoutEATIC() {
        return this._CreditsWithoutEATIC;
    }

    /**
	 * Class member corresponding to the field 'WorksheetCreditAmount' in the PDF.
	 */
    private String _WorksheetCreditAmount = "";

    /**
	 * Mutator Method for xWorksheetCreditAmount
	 * @param WorksheetCreditAmount the new value for 'WorksheetCreditAmount'
	 */
    public void set_WorksheetCreditAmount(String _WorksheetCreditAmount) {
        this._WorksheetCreditAmount = _WorksheetCreditAmount;
    }

    /**
	 * Accessor Method for xWorksheetCreditAmount
	 * @return the value of 'WorksheetCreditAmount'
	 */
    public String get_WorksheetCreditAmount() {
        return this._WorksheetCreditAmount;
    }

    /**
	 * Class member corresponding to the field 'Earned SustainableTechExport' in the PDF.
	 */
    private String _Earned_SustainableTechExport = "";

    /**
	 * Mutator Method for xEarned SustainableTechExport
	 * @param Earned SustainableTechExport the new value for 'Earned SustainableTechExport'
	 */
    public void set_Earned_SustainableTechExport(String _Earned_SustainableTechExport) {
        this._Earned_SustainableTechExport = _Earned_SustainableTechExport;
    }

    /**
	 * Accessor Method for xEarned SustainableTechExport
	 * @return the value of 'Earned SustainableTechExport'
	 */
    public String get_Earned_SustainableTechExport() {
        return this._Earned_SustainableTechExport;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount SustainableTechRDTax' in the PDF.
	 */
    private String _CreditAmount_SustainableTechRDTax = "";

    /**
	 * Mutator Method for xCreditAmount SustainableTechRDTax
	 * @param CreditAmount SustainableTechRDTax the new value for 'CreditAmount SustainableTechRDTax'
	 */
    public void set_CreditAmount_SustainableTechRDTax(String _CreditAmount_SustainableTechRDTax) {
        this._CreditAmount_SustainableTechRDTax = _CreditAmount_SustainableTechRDTax;
    }

    /**
	 * Accessor Method for xCreditAmount SustainableTechRDTax
	 * @return the value of 'CreditAmount SustainableTechRDTax'
	 */
    public String get_CreditAmount_SustainableTechRDTax() {
        return this._CreditAmount_SustainableTechRDTax;
    }

    /**
	 * Class member corresponding to the field 'Earned FacadeImprovement' in the PDF.
	 */
    private String _Earned_FacadeImprovement = "";

    /**
	 * Mutator Method for xEarned FacadeImprovement
	 * @param Earned FacadeImprovement the new value for 'Earned FacadeImprovement'
	 */
    public void set_Earned_FacadeImprovement(String _Earned_FacadeImprovement) {
        this._Earned_FacadeImprovement = _Earned_FacadeImprovement;
    }

    /**
	 * Accessor Method for xEarned FacadeImprovement
	 * @return the value of 'Earned FacadeImprovement'
	 */
    public String get_Earned_FacadeImprovement() {
        return this._Earned_FacadeImprovement;
    }

    /**
	 * Class member corresponding to the field 'CreditAmount AffordableHousing' in the PDF.
	 */
    private String _CreditAmount_AffordableHousing = "";

    /**
	 * Mutator Method for xCreditAmount AffordableHousing
	 * @param CreditAmount AffordableHousing the new value for 'CreditAmount AffordableHousing'
	 */
    public void set_CreditAmount_AffordableHousing(String _CreditAmount_AffordableHousing) {
        this._CreditAmount_AffordableHousing = _CreditAmount_AffordableHousing;
    }

    /**
	 * Accessor Method for xCreditAmount AffordableHousing
	 * @return the value of 'CreditAmount AffordableHousing'
	 */
    public String get_CreditAmount_AffordableHousing() {
        return this._CreditAmount_AffordableHousing;
    }
}
