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
 * Generated class for filling 'HS145'
 */
public class HS145 {

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
        fields.setFieldProperty("Secondary FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary FirstName", "textfont", baseFont, null);
        fields.setField("Secondary FirstName", this.get_Secondary_FirstName());
        fields.setFieldProperty("HousesiteEducationTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteEducationTax", "textfont", baseFont, null);
        fields.setField("HousesiteEducationTax", this.get_HousesiteEducationTax());
        fields.setFieldProperty("ownerhs145", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ownerhs145", "textfont", baseFont, null);
        fields.setField("ownerhs145", this.get_ownerhs145());
        fields.setFieldProperty("SellHousesiteN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SellHousesiteN", "textfont", baseFont, null);
        fields.setField("SellHousesiteN", this.get_SellHousesiteN());
        fields.setFieldProperty("AllocatedEducationTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedEducationTax", "textfont", baseFont, null);
        fields.setField("AllocatedEducationTax", this.get_AllocatedEducationTax());
        fields.setFieldProperty("City", "textsize", new Float(20.2), null);
        fields.setFieldProperty("City", "textfont", baseFont, null);
        fields.setField("City", this.get_City());
        fields.setFieldProperty("SellHousesiteY", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SellHousesiteY", "textfont", baseFont, null);
        fields.setField("SellHousesiteY", this.get_SellHousesiteY());
        fields.setFieldProperty("HousesiteValue", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteValue", "textfont", baseFont, null);
        fields.setField("HousesiteValue", this.get_HousesiteValue());
        fields.setFieldProperty("hs145TownCityLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs145TownCityLegalResidence", "textfont", baseFont, null);
        fields.setField("hs145TownCityLegalResidence", this.get_hs145TownCityLegalResidence());
        fields.setFieldProperty("hs145SpanNumber", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs145SpanNumber", "textfont", baseFont, null);
        fields.setField("hs145SpanNumber", this.get_hs145SpanNumber());
        fields.setFieldProperty("LotRent", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LotRent", "textfont", baseFont, null);
        fields.setField("LotRent", this.get_LotRent());
        fields.setFieldProperty("Secondary TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("Secondary TaxpayerSSN", this.get_Secondary_TaxpayerSSN());
        fields.setFieldProperty("FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FirstName", "textfont", baseFont, null);
        fields.setField("FirstName", this.get_FirstName());
        fields.setFieldProperty("AddressLine1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AddressLine1", "textfont", baseFont, null);
        fields.setField("AddressLine1", this.get_AddressLine1());
        fields.setFieldProperty("HousesiteMunicipalTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteMunicipalTax", "textfont", baseFont, null);
        fields.setField("HousesiteMunicipalTax", this.get_HousesiteMunicipalTax());
        fields.setFieldProperty("FullYearResidentN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FullYearResidentN", "textfont", baseFont, null);
        fields.setField("FullYearResidentN", this.get_FullYearResidentN());
        fields.setFieldProperty("AllocatedMunicipalTaxCont", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedMunicipalTaxCont", "textfont", baseFont, null);
        fields.setField("AllocatedMunicipalTaxCont", this.get_AllocatedMunicipalTaxCont());
        fields.setFieldProperty("amendbox", "textsize", new Float(20.2), null);
        fields.setFieldProperty("amendbox", "textfont", baseFont, null);
        fields.setField("amendbox", this.get_amendbox());
        fields.setFieldProperty("TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("TaxpayerSSN", this.get_TaxpayerSSN());
        fields.setFieldProperty("dobhs145", "textsize", new Float(20.2), null);
        fields.setFieldProperty("dobhs145", "textfont", baseFont, null);
        fields.setField("dobhs145", this.get_dobhs145());
        fields.setFieldProperty("State", "textsize", new Float(20.2), null);
        fields.setFieldProperty("State", "textfont", baseFont, null);
        fields.setField("State", this.get_State());
        fields.setFieldProperty("ClaimedAsDependentN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ClaimedAsDependentN", "textfont", baseFont, null);
        fields.setField("ClaimedAsDependentN", this.get_ClaimedAsDependentN());
        fields.setFieldProperty("ZIPCode", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ZIPCode", "textfont", baseFont, null);
        fields.setField("ZIPCode", this.get_ZIPCode());
        fields.setFieldProperty("hs145SchoolDistrict", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs145SchoolDistrict", "textfont", baseFont, null);
        fields.setField("hs145SchoolDistrict", this.get_hs145SchoolDistrict());
        fields.setFieldProperty("AllocatedMunicipalTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedMunicipalTax", "textfont", baseFont, null);
        fields.setField("AllocatedMunicipalTax", this.get_AllocatedMunicipalTax());
        fields.setFieldProperty("AllocatedEducationTaxCont", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedEducationTaxCont", "textfont", baseFont, null);
        fields.setField("AllocatedEducationTaxCont", this.get_AllocatedEducationTaxCont());
        fields.setFieldProperty("ClaimedAsDependentY", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ClaimedAsDependentY", "textfont", baseFont, null);
        fields.setField("ClaimedAsDependentY", this.get_ClaimedAsDependentY());
        fields.setFieldProperty("disc145", "textsize", new Float(20.2), null);
        fields.setFieldProperty("disc145", "textfont", baseFont, null);
        fields.setField("disc145", this.get_disc145());
        fields.setFieldProperty("FullYearResidentY", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FullYearResidentY", "textfont", baseFont, null);
        fields.setField("FullYearResidentY", this.get_FullYearResidentY());
        fields.setFieldProperty("hs145StateofLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs145StateofLegalResidence", "textfont", baseFont, null);
        fields.setField("hs145StateofLegalResidence", this.get_hs145StateofLegalResidence());
        fields.setFieldProperty("Secondary LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary LastName", "textfont", baseFont, null);
        fields.setField("Secondary LastName", this.get_Secondary_LastName());
        fields.setFieldProperty("hs145HomesteadLocation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs145HomesteadLocation", "textfont", baseFont, null);
        fields.setField("hs145HomesteadLocation", this.get_hs145HomesteadLocation());
        fields.setFieldProperty("HouseholdIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HouseholdIncome", "textfont", baseFont, null);
        fields.setField("HouseholdIncome", this.get_HouseholdIncome());
        fields.setFieldProperty("LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LastName", "textfont", baseFont, null);
        fields.setField("LastName", this.get_LastName());
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
	 * Class member corresponding to the field 'HousesiteEducationTax' in the PDF.
	 */
    private String _HousesiteEducationTax = "";

    /**
	 * Mutator Method for xHousesiteEducationTax
	 * @param HousesiteEducationTax the new value for 'HousesiteEducationTax'
	 */
    public void set_HousesiteEducationTax(String _HousesiteEducationTax) {
        this._HousesiteEducationTax = _HousesiteEducationTax;
    }

    /**
	 * Accessor Method for xHousesiteEducationTax
	 * @return the value of 'HousesiteEducationTax'
	 */
    public String get_HousesiteEducationTax() {
        return this._HousesiteEducationTax;
    }

    /**
	 * Class member corresponding to the field 'ownerhs145' in the PDF.
	 */
    private String _ownerhs145 = "";

    /**
	 * Mutator Method for xownerhs145
	 * @param ownerhs145 the new value for 'ownerhs145'
	 */
    public void set_ownerhs145(String _ownerhs145) {
        this._ownerhs145 = _ownerhs145;
    }

    /**
	 * Accessor Method for xownerhs145
	 * @return the value of 'ownerhs145'
	 */
    public String get_ownerhs145() {
        return this._ownerhs145;
    }

    /**
	 * Class member corresponding to the field 'SellHousesiteN' in the PDF.
	 */
    private String _SellHousesiteN = "";

    /**
	 * Mutator Method for xSellHousesiteN
	 * @param SellHousesiteN the new value for 'SellHousesiteN'
	 */
    public void set_SellHousesiteN(String _SellHousesiteN) {
        this._SellHousesiteN = _SellHousesiteN;
    }

    /**
	 * Accessor Method for xSellHousesiteN
	 * @return the value of 'SellHousesiteN'
	 */
    public String get_SellHousesiteN() {
        return this._SellHousesiteN;
    }

    /**
	 * Class member corresponding to the field 'AllocatedEducationTax' in the PDF.
	 */
    private String _AllocatedEducationTax = "";

    /**
	 * Mutator Method for xAllocatedEducationTax
	 * @param AllocatedEducationTax the new value for 'AllocatedEducationTax'
	 */
    public void set_AllocatedEducationTax(String _AllocatedEducationTax) {
        this._AllocatedEducationTax = _AllocatedEducationTax;
    }

    /**
	 * Accessor Method for xAllocatedEducationTax
	 * @return the value of 'AllocatedEducationTax'
	 */
    public String get_AllocatedEducationTax() {
        return this._AllocatedEducationTax;
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
	 * Class member corresponding to the field 'SellHousesiteY' in the PDF.
	 */
    private String _SellHousesiteY = "";

    /**
	 * Mutator Method for xSellHousesiteY
	 * @param SellHousesiteY the new value for 'SellHousesiteY'
	 */
    public void set_SellHousesiteY(String _SellHousesiteY) {
        this._SellHousesiteY = _SellHousesiteY;
    }

    /**
	 * Accessor Method for xSellHousesiteY
	 * @return the value of 'SellHousesiteY'
	 */
    public String get_SellHousesiteY() {
        return this._SellHousesiteY;
    }

    /**
	 * Class member corresponding to the field 'HousesiteValue' in the PDF.
	 */
    private String _HousesiteValue = "";

    /**
	 * Mutator Method for xHousesiteValue
	 * @param HousesiteValue the new value for 'HousesiteValue'
	 */
    public void set_HousesiteValue(String _HousesiteValue) {
        this._HousesiteValue = _HousesiteValue;
    }

    /**
	 * Accessor Method for xHousesiteValue
	 * @return the value of 'HousesiteValue'
	 */
    public String get_HousesiteValue() {
        return this._HousesiteValue;
    }

    /**
	 * Class member corresponding to the field 'hs145TownCityLegalResidence' in the PDF.
	 */
    private String _hs145TownCityLegalResidence = "";

    /**
	 * Mutator Method for xhs145TownCityLegalResidence
	 * @param hs145TownCityLegalResidence the new value for 'hs145TownCityLegalResidence'
	 */
    public void set_hs145TownCityLegalResidence(String _hs145TownCityLegalResidence) {
        this._hs145TownCityLegalResidence = _hs145TownCityLegalResidence;
    }

    /**
	 * Accessor Method for xhs145TownCityLegalResidence
	 * @return the value of 'hs145TownCityLegalResidence'
	 */
    public String get_hs145TownCityLegalResidence() {
        return this._hs145TownCityLegalResidence;
    }

    /**
	 * Class member corresponding to the field 'hs145SpanNumber' in the PDF.
	 */
    private String _hs145SpanNumber = "";

    /**
	 * Mutator Method for xhs145SpanNumber
	 * @param hs145SpanNumber the new value for 'hs145SpanNumber'
	 */
    public void set_hs145SpanNumber(String _hs145SpanNumber) {
        this._hs145SpanNumber = _hs145SpanNumber;
    }

    /**
	 * Accessor Method for xhs145SpanNumber
	 * @return the value of 'hs145SpanNumber'
	 */
    public String get_hs145SpanNumber() {
        return this._hs145SpanNumber;
    }

    /**
	 * Class member corresponding to the field 'LotRent' in the PDF.
	 */
    private String _LotRent = "";

    /**
	 * Mutator Method for xLotRent
	 * @param LotRent the new value for 'LotRent'
	 */
    public void set_LotRent(String _LotRent) {
        this._LotRent = _LotRent;
    }

    /**
	 * Accessor Method for xLotRent
	 * @return the value of 'LotRent'
	 */
    public String get_LotRent() {
        return this._LotRent;
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
	 * Class member corresponding to the field 'HousesiteMunicipalTax' in the PDF.
	 */
    private String _HousesiteMunicipalTax = "";

    /**
	 * Mutator Method for xHousesiteMunicipalTax
	 * @param HousesiteMunicipalTax the new value for 'HousesiteMunicipalTax'
	 */
    public void set_HousesiteMunicipalTax(String _HousesiteMunicipalTax) {
        this._HousesiteMunicipalTax = _HousesiteMunicipalTax;
    }

    /**
	 * Accessor Method for xHousesiteMunicipalTax
	 * @return the value of 'HousesiteMunicipalTax'
	 */
    public String get_HousesiteMunicipalTax() {
        return this._HousesiteMunicipalTax;
    }

    /**
	 * Class member corresponding to the field 'FullYearResidentN' in the PDF.
	 */
    private String _FullYearResidentN = "";

    /**
	 * Mutator Method for xFullYearResidentN
	 * @param FullYearResidentN the new value for 'FullYearResidentN'
	 */
    public void set_FullYearResidentN(String _FullYearResidentN) {
        this._FullYearResidentN = _FullYearResidentN;
    }

    /**
	 * Accessor Method for xFullYearResidentN
	 * @return the value of 'FullYearResidentN'
	 */
    public String get_FullYearResidentN() {
        return this._FullYearResidentN;
    }

    /**
	 * Class member corresponding to the field 'AllocatedMunicipalTaxCont' in the PDF.
	 */
    private String _AllocatedMunicipalTaxCont = "";

    /**
	 * Mutator Method for xAllocatedMunicipalTaxCont
	 * @param AllocatedMunicipalTaxCont the new value for 'AllocatedMunicipalTaxCont'
	 */
    public void set_AllocatedMunicipalTaxCont(String _AllocatedMunicipalTaxCont) {
        this._AllocatedMunicipalTaxCont = _AllocatedMunicipalTaxCont;
    }

    /**
	 * Accessor Method for xAllocatedMunicipalTaxCont
	 * @return the value of 'AllocatedMunicipalTaxCont'
	 */
    public String get_AllocatedMunicipalTaxCont() {
        return this._AllocatedMunicipalTaxCont;
    }

    /**
	 * Class member corresponding to the field 'amendbox' in the PDF.
	 */
    private String _amendbox = "";

    /**
	 * Mutator Method for xamendbox
	 * @param amendbox the new value for 'amendbox'
	 */
    public void set_amendbox(String _amendbox) {
        this._amendbox = _amendbox;
    }

    /**
	 * Accessor Method for xamendbox
	 * @return the value of 'amendbox'
	 */
    public String get_amendbox() {
        return this._amendbox;
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
	 * Class member corresponding to the field 'dobhs145' in the PDF.
	 */
    private String _dobhs145 = "";

    /**
	 * Mutator Method for xdobhs145
	 * @param dobhs145 the new value for 'dobhs145'
	 */
    public void set_dobhs145(String _dobhs145) {
        this._dobhs145 = _dobhs145;
    }

    /**
	 * Accessor Method for xdobhs145
	 * @return the value of 'dobhs145'
	 */
    public String get_dobhs145() {
        return this._dobhs145;
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
	 * Class member corresponding to the field 'ClaimedAsDependentN' in the PDF.
	 */
    private String _ClaimedAsDependentN = "";

    /**
	 * Mutator Method for xClaimedAsDependentN
	 * @param ClaimedAsDependentN the new value for 'ClaimedAsDependentN'
	 */
    public void set_ClaimedAsDependentN(String _ClaimedAsDependentN) {
        this._ClaimedAsDependentN = _ClaimedAsDependentN;
    }

    /**
	 * Accessor Method for xClaimedAsDependentN
	 * @return the value of 'ClaimedAsDependentN'
	 */
    public String get_ClaimedAsDependentN() {
        return this._ClaimedAsDependentN;
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
	 * Class member corresponding to the field 'hs145SchoolDistrict' in the PDF.
	 */
    private String _hs145SchoolDistrict = "";

    /**
	 * Mutator Method for xhs145SchoolDistrict
	 * @param hs145SchoolDistrict the new value for 'hs145SchoolDistrict'
	 */
    public void set_hs145SchoolDistrict(String _hs145SchoolDistrict) {
        this._hs145SchoolDistrict = _hs145SchoolDistrict;
    }

    /**
	 * Accessor Method for xhs145SchoolDistrict
	 * @return the value of 'hs145SchoolDistrict'
	 */
    public String get_hs145SchoolDistrict() {
        return this._hs145SchoolDistrict;
    }

    /**
	 * Class member corresponding to the field 'AllocatedMunicipalTax' in the PDF.
	 */
    private String _AllocatedMunicipalTax = "";

    /**
	 * Mutator Method for xAllocatedMunicipalTax
	 * @param AllocatedMunicipalTax the new value for 'AllocatedMunicipalTax'
	 */
    public void set_AllocatedMunicipalTax(String _AllocatedMunicipalTax) {
        this._AllocatedMunicipalTax = _AllocatedMunicipalTax;
    }

    /**
	 * Accessor Method for xAllocatedMunicipalTax
	 * @return the value of 'AllocatedMunicipalTax'
	 */
    public String get_AllocatedMunicipalTax() {
        return this._AllocatedMunicipalTax;
    }

    /**
	 * Class member corresponding to the field 'AllocatedEducationTaxCont' in the PDF.
	 */
    private String _AllocatedEducationTaxCont = "";

    /**
	 * Mutator Method for xAllocatedEducationTaxCont
	 * @param AllocatedEducationTaxCont the new value for 'AllocatedEducationTaxCont'
	 */
    public void set_AllocatedEducationTaxCont(String _AllocatedEducationTaxCont) {
        this._AllocatedEducationTaxCont = _AllocatedEducationTaxCont;
    }

    /**
	 * Accessor Method for xAllocatedEducationTaxCont
	 * @return the value of 'AllocatedEducationTaxCont'
	 */
    public String get_AllocatedEducationTaxCont() {
        return this._AllocatedEducationTaxCont;
    }

    /**
	 * Class member corresponding to the field 'ClaimedAsDependentY' in the PDF.
	 */
    private String _ClaimedAsDependentY = "";

    /**
	 * Mutator Method for xClaimedAsDependentY
	 * @param ClaimedAsDependentY the new value for 'ClaimedAsDependentY'
	 */
    public void set_ClaimedAsDependentY(String _ClaimedAsDependentY) {
        this._ClaimedAsDependentY = _ClaimedAsDependentY;
    }

    /**
	 * Accessor Method for xClaimedAsDependentY
	 * @return the value of 'ClaimedAsDependentY'
	 */
    public String get_ClaimedAsDependentY() {
        return this._ClaimedAsDependentY;
    }

    /**
	 * Class member corresponding to the field 'disc145' in the PDF.
	 */
    private String _disc145 = "";

    /**
	 * Mutator Method for xdisc145
	 * @param disc145 the new value for 'disc145'
	 */
    public void set_disc145(String _disc145) {
        this._disc145 = _disc145;
    }

    /**
	 * Accessor Method for xdisc145
	 * @return the value of 'disc145'
	 */
    public String get_disc145() {
        return this._disc145;
    }

    /**
	 * Class member corresponding to the field 'FullYearResidentY' in the PDF.
	 */
    private String _FullYearResidentY = "";

    /**
	 * Mutator Method for xFullYearResidentY
	 * @param FullYearResidentY the new value for 'FullYearResidentY'
	 */
    public void set_FullYearResidentY(String _FullYearResidentY) {
        this._FullYearResidentY = _FullYearResidentY;
    }

    /**
	 * Accessor Method for xFullYearResidentY
	 * @return the value of 'FullYearResidentY'
	 */
    public String get_FullYearResidentY() {
        return this._FullYearResidentY;
    }

    /**
	 * Class member corresponding to the field 'hs145StateofLegalResidence' in the PDF.
	 */
    private String _hs145StateofLegalResidence = "";

    /**
	 * Mutator Method for xhs145StateofLegalResidence
	 * @param hs145StateofLegalResidence the new value for 'hs145StateofLegalResidence'
	 */
    public void set_hs145StateofLegalResidence(String _hs145StateofLegalResidence) {
        this._hs145StateofLegalResidence = _hs145StateofLegalResidence;
    }

    /**
	 * Accessor Method for xhs145StateofLegalResidence
	 * @return the value of 'hs145StateofLegalResidence'
	 */
    public String get_hs145StateofLegalResidence() {
        return this._hs145StateofLegalResidence;
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
	 * Class member corresponding to the field 'hs145HomesteadLocation' in the PDF.
	 */
    private String _hs145HomesteadLocation = "";

    /**
	 * Mutator Method for xhs145HomesteadLocation
	 * @param hs145HomesteadLocation the new value for 'hs145HomesteadLocation'
	 */
    public void set_hs145HomesteadLocation(String _hs145HomesteadLocation) {
        this._hs145HomesteadLocation = _hs145HomesteadLocation;
    }

    /**
	 * Accessor Method for xhs145HomesteadLocation
	 * @return the value of 'hs145HomesteadLocation'
	 */
    public String get_hs145HomesteadLocation() {
        return this._hs145HomesteadLocation;
    }

    /**
	 * Class member corresponding to the field 'HouseholdIncome' in the PDF.
	 */
    private String _HouseholdIncome = "";

    /**
	 * Mutator Method for xHouseholdIncome
	 * @param HouseholdIncome the new value for 'HouseholdIncome'
	 */
    public void set_HouseholdIncome(String _HouseholdIncome) {
        this._HouseholdIncome = _HouseholdIncome;
    }

    /**
	 * Accessor Method for xHouseholdIncome
	 * @return the value of 'HouseholdIncome'
	 */
    public String get_HouseholdIncome() {
        return this._HouseholdIncome;
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
}
