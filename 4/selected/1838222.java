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
 * Generated class for filling 'HS122'
 */
public class HS122 {

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
        fields.setFieldProperty("RentalUse", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RentalUse", "textfont", baseFont, null);
        fields.setField("RentalUse", this.get_RentalUse());
        fields.setFieldProperty("Secondary FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary FirstName", "textfont", baseFont, null);
        fields.setField("Secondary FirstName", this.get_Secondary_FirstName());
        fields.setFieldProperty("HousesiteEducationTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteEducationTax", "textfont", baseFont, null);
        fields.setField("HousesiteEducationTax", this.get_HousesiteEducationTax());
        fields.setFieldProperty("TownCityLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TownCityLegalResidence", "textfont", baseFont, null);
        fields.setField("TownCityLegalResidence", this.get_TownCityLegalResidence());
        fields.setFieldProperty("PreparerFirmIDNumber", "textsize", new Float(20.2), null);
        fields.setFieldProperty("PreparerFirmIDNumber", "textfont", baseFont, null);
        fields.setField("PreparerFirmIDNumber", this.get_PreparerFirmIDNumber());
        fields.setFieldProperty("OwnedByRelatedFarmer", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OwnedByRelatedFarmer", "textfont", baseFont, null);
        fields.setField("OwnedByRelatedFarmer", this.get_OwnedByRelatedFarmer());
        fields.setFieldProperty("AllocatedEducationTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedEducationTax", "textfont", baseFont, null);
        fields.setField("AllocatedEducationTax", this.get_AllocatedEducationTax());
        fields.setFieldProperty("LifeEstateHolder", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LifeEstateHolder", "textfont", baseFont, null);
        fields.setField("LifeEstateHolder", this.get_LifeEstateHolder());
        fields.setFieldProperty("City", "textsize", new Float(20.2), null);
        fields.setFieldProperty("City", "textfont", baseFont, null);
        fields.setField("City", this.get_City());
        fields.setFieldProperty("FullYearResident", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FullYearResident", "textfont", baseFont, null);
        fields.setField("FullYearResident", this.get_FullYearResident());
        fields.setFieldProperty("HousesiteValue", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteValue", "textfont", baseFont, null);
        fields.setField("HousesiteValue", this.get_HousesiteValue());
        fields.setFieldProperty("Preparer Phone", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Preparer Phone", "textfont", baseFont, null);
        fields.setField("Preparer Phone", this.get_Preparer_Phone());
        fields.setFieldProperty("PreparerBusinessName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("PreparerBusinessName", "textfont", baseFont, null);
        fields.setField("PreparerBusinessName", this.get_PreparerBusinessName());
        fields.setFieldProperty("CrossingTownBoundaries", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CrossingTownBoundaries", "textfont", baseFont, null);
        fields.setField("CrossingTownBoundaries", this.get_CrossingTownBoundaries());
        fields.setFieldProperty("LotRent", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LotRent", "textfont", baseFont, null);
        fields.setField("LotRent", this.get_LotRent());
        fields.setFieldProperty("Secondary TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("Secondary TaxpayerSSN", this.get_Secondary_TaxpayerSSN());
        fields.setFieldProperty("Preparer SSN PTIN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Preparer SSN PTIN", "textfont", baseFont, null);
        fields.setField("Preparer SSN PTIN", this.get_Preparer_SSN_PTIN());
        fields.setFieldProperty("FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FirstName", "textfont", baseFont, null);
        fields.setField("FirstName", this.get_FirstName());
        fields.setFieldProperty("AddressLine1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AddressLine1", "textfont", baseFont, null);
        fields.setField("AddressLine1", this.get_AddressLine1());
        fields.setFieldProperty("HousesiteMunicipalTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HousesiteMunicipalTax", "textfont", baseFont, null);
        fields.setField("HousesiteMunicipalTax", this.get_HousesiteMunicipalTax());
        fields.setFieldProperty("TotalParcelAcres", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TotalParcelAcres", "textfont", baseFont, null);
        fields.setField("TotalParcelAcres", this.get_TotalParcelAcres());
        fields.setFieldProperty("RevocableTrust", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RevocableTrust", "textfont", baseFont, null);
        fields.setField("RevocableTrust", this.get_RevocableTrust());
        fields.setFieldProperty("TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("TaxpayerSSN", this.get_TaxpayerSSN());
        fields.setFieldProperty("OwnershipInterest", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OwnershipInterest", "textfont", baseFont, null);
        fields.setField("OwnershipInterest", this.get_OwnershipInterest());
        fields.setFieldProperty("SellHousesite", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SellHousesite", "textfont", baseFont, null);
        fields.setField("SellHousesite", this.get_SellHousesite());
        fields.setFieldProperty("ClaimedAsDependent", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ClaimedAsDependent", "textfont", baseFont, null);
        fields.setField("ClaimedAsDependent", this.get_ClaimedAsDependent());
        fields.setFieldProperty("State", "textsize", new Float(20.2), null);
        fields.setFieldProperty("State", "textfont", baseFont, null);
        fields.setField("State", this.get_State());
        fields.setFieldProperty("ZIPCode", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ZIPCode", "textfont", baseFont, null);
        fields.setField("ZIPCode", this.get_ZIPCode());
        fields.setFieldProperty("BusinessUse", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessUse", "textfont", baseFont, null);
        fields.setField("BusinessUse", this.get_BusinessUse());
        fields.setFieldProperty("AllocatedMunicipalTax", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AllocatedMunicipalTax", "textfont", baseFont, null);
        fields.setField("AllocatedMunicipalTax", this.get_AllocatedMunicipalTax());
        fields.setFieldProperty("Secondary LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary LastName", "textfont", baseFont, null);
        fields.setField("Secondary LastName", this.get_Secondary_LastName());
        fields.setFieldProperty("SpanNumber", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SpanNumber", "textfont", baseFont, null);
        fields.setField("SpanNumber", this.get_SpanNumber());
        fields.setFieldProperty("ClaimantDateOfBirth", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ClaimantDateOfBirth", "textfont", baseFont, null);
        fields.setField("ClaimantDateOfBirth", this.get_ClaimantDateOfBirth());
        fields.setFieldProperty("HouseholdIncome", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HouseholdIncome", "textfont", baseFont, null);
        fields.setField("HouseholdIncome", this.get_HouseholdIncome());
        fields.setFieldProperty("SchoolDistrict", "textsize", new Float(20.2), null);
        fields.setFieldProperty("SchoolDistrict", "textfont", baseFont, null);
        fields.setField("SchoolDistrict", this.get_SchoolDistrict());
        fields.setFieldProperty("StateofLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("StateofLegalResidence", "textfont", baseFont, null);
        fields.setField("StateofLegalResidence", this.get_StateofLegalResidence());
        fields.setFieldProperty("BusinessRentUseImprovements", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessRentUseImprovements", "textfont", baseFont, null);
        fields.setField("BusinessRentUseImprovements", this.get_BusinessRentUseImprovements());
        fields.setFieldProperty("HomesteadLocation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("HomesteadLocation", "textfont", baseFont, null);
        fields.setField("HomesteadLocation", this.get_HomesteadLocation());
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
	 * Class member corresponding to the field 'RentalUse' in the PDF.
	 */
    private String _RentalUse = "";

    /**
	 * Mutator Method for xRentalUse
	 * @param RentalUse the new value for 'RentalUse'
	 */
    public void set_RentalUse(String _RentalUse) {
        this._RentalUse = _RentalUse;
    }

    /**
	 * Accessor Method for xRentalUse
	 * @return the value of 'RentalUse'
	 */
    public String get_RentalUse() {
        return this._RentalUse;
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

    /**
	 * Class member corresponding to the field 'OwnedByRelatedFarmer' in the PDF.
	 */
    private String _OwnedByRelatedFarmer = "";

    /**
	 * Mutator Method for xOwnedByRelatedFarmer
	 * @param OwnedByRelatedFarmer the new value for 'OwnedByRelatedFarmer'
	 */
    public void set_OwnedByRelatedFarmer(String _OwnedByRelatedFarmer) {
        this._OwnedByRelatedFarmer = _OwnedByRelatedFarmer;
    }

    /**
	 * Accessor Method for xOwnedByRelatedFarmer
	 * @return the value of 'OwnedByRelatedFarmer'
	 */
    public String get_OwnedByRelatedFarmer() {
        return this._OwnedByRelatedFarmer;
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
	 * Class member corresponding to the field 'LifeEstateHolder' in the PDF.
	 */
    private String _LifeEstateHolder = "";

    /**
	 * Mutator Method for xLifeEstateHolder
	 * @param LifeEstateHolder the new value for 'LifeEstateHolder'
	 */
    public void set_LifeEstateHolder(String _LifeEstateHolder) {
        this._LifeEstateHolder = _LifeEstateHolder;
    }

    /**
	 * Accessor Method for xLifeEstateHolder
	 * @return the value of 'LifeEstateHolder'
	 */
    public String get_LifeEstateHolder() {
        return this._LifeEstateHolder;
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
	 * Class member corresponding to the field 'FullYearResident' in the PDF.
	 */
    private String _FullYearResident = "";

    /**
	 * Mutator Method for xFullYearResident
	 * @param FullYearResident the new value for 'FullYearResident'
	 */
    public void set_FullYearResident(String _FullYearResident) {
        this._FullYearResident = _FullYearResident;
    }

    /**
	 * Accessor Method for xFullYearResident
	 * @return the value of 'FullYearResident'
	 */
    public String get_FullYearResident() {
        return this._FullYearResident;
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
	 * Class member corresponding to the field 'CrossingTownBoundaries' in the PDF.
	 */
    private String _CrossingTownBoundaries = "";

    /**
	 * Mutator Method for xCrossingTownBoundaries
	 * @param CrossingTownBoundaries the new value for 'CrossingTownBoundaries'
	 */
    public void set_CrossingTownBoundaries(String _CrossingTownBoundaries) {
        this._CrossingTownBoundaries = _CrossingTownBoundaries;
    }

    /**
	 * Accessor Method for xCrossingTownBoundaries
	 * @return the value of 'CrossingTownBoundaries'
	 */
    public String get_CrossingTownBoundaries() {
        return this._CrossingTownBoundaries;
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
	 * Class member corresponding to the field 'TotalParcelAcres' in the PDF.
	 */
    private String _TotalParcelAcres = "";

    /**
	 * Mutator Method for xTotalParcelAcres
	 * @param TotalParcelAcres the new value for 'TotalParcelAcres'
	 */
    public void set_TotalParcelAcres(String _TotalParcelAcres) {
        this._TotalParcelAcres = _TotalParcelAcres;
    }

    /**
	 * Accessor Method for xTotalParcelAcres
	 * @return the value of 'TotalParcelAcres'
	 */
    public String get_TotalParcelAcres() {
        return this._TotalParcelAcres;
    }

    /**
	 * Class member corresponding to the field 'RevocableTrust' in the PDF.
	 */
    private String _RevocableTrust = "";

    /**
	 * Mutator Method for xRevocableTrust
	 * @param RevocableTrust the new value for 'RevocableTrust'
	 */
    public void set_RevocableTrust(String _RevocableTrust) {
        this._RevocableTrust = _RevocableTrust;
    }

    /**
	 * Accessor Method for xRevocableTrust
	 * @return the value of 'RevocableTrust'
	 */
    public String get_RevocableTrust() {
        return this._RevocableTrust;
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
	 * Class member corresponding to the field 'OwnershipInterest' in the PDF.
	 */
    private String _OwnershipInterest = "";

    /**
	 * Mutator Method for xOwnershipInterest
	 * @param OwnershipInterest the new value for 'OwnershipInterest'
	 */
    public void set_OwnershipInterest(String _OwnershipInterest) {
        this._OwnershipInterest = _OwnershipInterest;
    }

    /**
	 * Accessor Method for xOwnershipInterest
	 * @return the value of 'OwnershipInterest'
	 */
    public String get_OwnershipInterest() {
        return this._OwnershipInterest;
    }

    /**
	 * Class member corresponding to the field 'SellHousesite' in the PDF.
	 */
    private String _SellHousesite = "";

    /**
	 * Mutator Method for xSellHousesite
	 * @param SellHousesite the new value for 'SellHousesite'
	 */
    public void set_SellHousesite(String _SellHousesite) {
        this._SellHousesite = _SellHousesite;
    }

    /**
	 * Accessor Method for xSellHousesite
	 * @return the value of 'SellHousesite'
	 */
    public String get_SellHousesite() {
        return this._SellHousesite;
    }

    /**
	 * Class member corresponding to the field 'ClaimedAsDependent' in the PDF.
	 */
    private String _ClaimedAsDependent = "";

    /**
	 * Mutator Method for xClaimedAsDependent
	 * @param ClaimedAsDependent the new value for 'ClaimedAsDependent'
	 */
    public void set_ClaimedAsDependent(String _ClaimedAsDependent) {
        this._ClaimedAsDependent = _ClaimedAsDependent;
    }

    /**
	 * Accessor Method for xClaimedAsDependent
	 * @return the value of 'ClaimedAsDependent'
	 */
    public String get_ClaimedAsDependent() {
        return this._ClaimedAsDependent;
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
	 * Class member corresponding to the field 'BusinessUse' in the PDF.
	 */
    private String _BusinessUse = "";

    /**
	 * Mutator Method for xBusinessUse
	 * @param BusinessUse the new value for 'BusinessUse'
	 */
    public void set_BusinessUse(String _BusinessUse) {
        this._BusinessUse = _BusinessUse;
    }

    /**
	 * Accessor Method for xBusinessUse
	 * @return the value of 'BusinessUse'
	 */
    public String get_BusinessUse() {
        return this._BusinessUse;
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
	 * Class member corresponding to the field 'SpanNumber' in the PDF.
	 */
    private String _SpanNumber = "";

    /**
	 * Mutator Method for xSpanNumber
	 * @param SpanNumber the new value for 'SpanNumber'
	 */
    public void set_SpanNumber(String _SpanNumber) {
        this._SpanNumber = _SpanNumber;
    }

    /**
	 * Accessor Method for xSpanNumber
	 * @return the value of 'SpanNumber'
	 */
    public String get_SpanNumber() {
        return this._SpanNumber;
    }

    /**
	 * Class member corresponding to the field 'ClaimantDateOfBirth' in the PDF.
	 */
    private String _ClaimantDateOfBirth = "";

    /**
	 * Mutator Method for xClaimantDateOfBirth
	 * @param ClaimantDateOfBirth the new value for 'ClaimantDateOfBirth'
	 */
    public void set_ClaimantDateOfBirth(String _ClaimantDateOfBirth) {
        this._ClaimantDateOfBirth = _ClaimantDateOfBirth;
    }

    /**
	 * Accessor Method for xClaimantDateOfBirth
	 * @return the value of 'ClaimantDateOfBirth'
	 */
    public String get_ClaimantDateOfBirth() {
        return this._ClaimantDateOfBirth;
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
	 * Class member corresponding to the field 'BusinessRentUseImprovements' in the PDF.
	 */
    private String _BusinessRentUseImprovements = "";

    /**
	 * Mutator Method for xBusinessRentUseImprovements
	 * @param BusinessRentUseImprovements the new value for 'BusinessRentUseImprovements'
	 */
    public void set_BusinessRentUseImprovements(String _BusinessRentUseImprovements) {
        this._BusinessRentUseImprovements = _BusinessRentUseImprovements;
    }

    /**
	 * Accessor Method for xBusinessRentUseImprovements
	 * @return the value of 'BusinessRentUseImprovements'
	 */
    public String get_BusinessRentUseImprovements() {
        return this._BusinessRentUseImprovements;
    }

    /**
	 * Class member corresponding to the field 'HomesteadLocation' in the PDF.
	 */
    private String _HomesteadLocation = "";

    /**
	 * Mutator Method for xHomesteadLocation
	 * @param HomesteadLocation the new value for 'HomesteadLocation'
	 */
    public void set_HomesteadLocation(String _HomesteadLocation) {
        this._HomesteadLocation = _HomesteadLocation;
    }

    /**
	 * Accessor Method for xHomesteadLocation
	 * @return the value of 'HomesteadLocation'
	 */
    public String get_HomesteadLocation() {
        return this._HomesteadLocation;
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
