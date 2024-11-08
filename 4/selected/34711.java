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
 * Generated class for filling 'HS131'
 */
public class HS131 {

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
        fields.setFieldProperty("OwnedByRelatedFarmer", "textsize", new Float(20.2), null);
        fields.setFieldProperty("OwnedByRelatedFarmer", "textfont", baseFont, null);
        fields.setField("OwnedByRelatedFarmer", this.get_OwnedByRelatedFarmer());
        fields.setFieldProperty("LifeEstateHolder", "textsize", new Float(20.2), null);
        fields.setFieldProperty("LifeEstateHolder", "textfont", baseFont, null);
        fields.setField("LifeEstateHolder", this.get_LifeEstateHolder());
        fields.setFieldProperty("City", "textsize", new Float(20.2), null);
        fields.setFieldProperty("City", "textfont", baseFont, null);
        fields.setField("City", this.get_City());
        fields.setFieldProperty("CrossingTownBoundaries", "textsize", new Float(20.2), null);
        fields.setFieldProperty("CrossingTownBoundaries", "textfont", baseFont, null);
        fields.setField("CrossingTownBoundaries", this.get_CrossingTownBoundaries());
        fields.setFieldProperty("usechgdate", "textsize", new Float(20.2), null);
        fields.setFieldProperty("usechgdate", "textfont", baseFont, null);
        fields.setField("usechgdate", this.get_usechgdate());
        fields.setFieldProperty("hs131HomesteadLocation", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs131HomesteadLocation", "textfont", baseFont, null);
        fields.setField("hs131HomesteadLocation", this.get_hs131HomesteadLocation());
        fields.setFieldProperty("Secondary TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("Secondary TaxpayerSSN", this.get_Secondary_TaxpayerSSN());
        fields.setFieldProperty("disc131", "textsize", new Float(20.2), null);
        fields.setFieldProperty("disc131", "textfont", baseFont, null);
        fields.setField("disc131", this.get_disc131());
        fields.setFieldProperty("BusinessRentUseImprovementsYes", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessRentUseImprovementsYes", "textfont", baseFont, null);
        fields.setField("BusinessRentUseImprovementsYes", this.get_BusinessRentUseImprovementsYes());
        fields.setFieldProperty("hs131StateofLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs131StateofLegalResidence", "textfont", baseFont, null);
        fields.setField("hs131StateofLegalResidence", this.get_hs131StateofLegalResidence());
        fields.setFieldProperty("FirstName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("FirstName", "textfont", baseFont, null);
        fields.setField("FirstName", this.get_FirstName());
        fields.setFieldProperty("AddressLine1", "textsize", new Float(20.2), null);
        fields.setFieldProperty("AddressLine1", "textfont", baseFont, null);
        fields.setField("AddressLine1", this.get_AddressLine1());
        fields.setFieldProperty("RevocableTrust", "textsize", new Float(20.2), null);
        fields.setFieldProperty("RevocableTrust", "textfont", baseFont, null);
        fields.setField("RevocableTrust", this.get_RevocableTrust());
        fields.setFieldProperty("TaxpayerSSN", "textsize", new Float(20.2), null);
        fields.setFieldProperty("TaxpayerSSN", "textfont", baseFont, null);
        fields.setField("TaxpayerSSN", this.get_TaxpayerSSN());
        fields.setFieldProperty("hdbox", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hdbox", "textfont", baseFont, null);
        fields.setField("hdbox", this.get_hdbox());
        fields.setFieldProperty("hs131TownCityLegalResidence", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs131TownCityLegalResidence", "textfont", baseFont, null);
        fields.setField("hs131TownCityLegalResidence", this.get_hs131TownCityLegalResidence());
        fields.setFieldProperty("State", "textsize", new Float(20.2), null);
        fields.setFieldProperty("State", "textfont", baseFont, null);
        fields.setField("State", this.get_State());
        fields.setFieldProperty("BusinessRentUseImprovementsNo", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessRentUseImprovementsNo", "textfont", baseFont, null);
        fields.setField("BusinessRentUseImprovementsNo", this.get_BusinessRentUseImprovementsNo());
        fields.setFieldProperty("ZIPCode", "textsize", new Float(20.2), null);
        fields.setFieldProperty("ZIPCode", "textfont", baseFont, null);
        fields.setField("ZIPCode", this.get_ZIPCode());
        fields.setFieldProperty("BusinessUse", "textsize", new Float(20.2), null);
        fields.setFieldProperty("BusinessUse", "textfont", baseFont, null);
        fields.setField("BusinessUse", this.get_BusinessUse());
        fields.setFieldProperty("hs131SpanNumber", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs131SpanNumber", "textfont", baseFont, null);
        fields.setField("hs131SpanNumber", this.get_hs131SpanNumber());
        fields.setFieldProperty("dobhs131", "textsize", new Float(20.2), null);
        fields.setFieldProperty("dobhs131", "textfont", baseFont, null);
        fields.setField("dobhs131", this.get_dobhs131());
        fields.setFieldProperty("Secondary LastName", "textsize", new Float(20.2), null);
        fields.setFieldProperty("Secondary LastName", "textfont", baseFont, null);
        fields.setField("Secondary LastName", this.get_Secondary_LastName());
        fields.setFieldProperty("usebx", "textsize", new Float(20.2), null);
        fields.setFieldProperty("usebx", "textfont", baseFont, null);
        fields.setField("usebx", this.get_usebx());
        fields.setFieldProperty("dateclose", "textsize", new Float(20.2), null);
        fields.setFieldProperty("dateclose", "textfont", baseFont, null);
        fields.setField("dateclose", this.get_dateclose());
        fields.setFieldProperty("hs131SchoolDistrict", "textsize", new Float(20.2), null);
        fields.setFieldProperty("hs131SchoolDistrict", "textfont", baseFont, null);
        fields.setField("hs131SchoolDistrict", this.get_hs131SchoolDistrict());
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
	 * Class member corresponding to the field 'usechgdate' in the PDF.
	 */
    private String _usechgdate = "";

    /**
	 * Mutator Method for xusechgdate
	 * @param usechgdate the new value for 'usechgdate'
	 */
    public void set_usechgdate(String _usechgdate) {
        this._usechgdate = _usechgdate;
    }

    /**
	 * Accessor Method for xusechgdate
	 * @return the value of 'usechgdate'
	 */
    public String get_usechgdate() {
        return this._usechgdate;
    }

    /**
	 * Class member corresponding to the field 'hs131HomesteadLocation' in the PDF.
	 */
    private String _hs131HomesteadLocation = "";

    /**
	 * Mutator Method for xhs131HomesteadLocation
	 * @param hs131HomesteadLocation the new value for 'hs131HomesteadLocation'
	 */
    public void set_hs131HomesteadLocation(String _hs131HomesteadLocation) {
        this._hs131HomesteadLocation = _hs131HomesteadLocation;
    }

    /**
	 * Accessor Method for xhs131HomesteadLocation
	 * @return the value of 'hs131HomesteadLocation'
	 */
    public String get_hs131HomesteadLocation() {
        return this._hs131HomesteadLocation;
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
	 * Class member corresponding to the field 'disc131' in the PDF.
	 */
    private String _disc131 = "";

    /**
	 * Mutator Method for xdisc131
	 * @param disc131 the new value for 'disc131'
	 */
    public void set_disc131(String _disc131) {
        this._disc131 = _disc131;
    }

    /**
	 * Accessor Method for xdisc131
	 * @return the value of 'disc131'
	 */
    public String get_disc131() {
        return this._disc131;
    }

    /**
	 * Class member corresponding to the field 'BusinessRentUseImprovementsYes' in the PDF.
	 */
    private String _BusinessRentUseImprovementsYes = "";

    /**
	 * Mutator Method for xBusinessRentUseImprovementsYes
	 * @param BusinessRentUseImprovementsYes the new value for 'BusinessRentUseImprovementsYes'
	 */
    public void set_BusinessRentUseImprovementsYes(String _BusinessRentUseImprovementsYes) {
        this._BusinessRentUseImprovementsYes = _BusinessRentUseImprovementsYes;
    }

    /**
	 * Accessor Method for xBusinessRentUseImprovementsYes
	 * @return the value of 'BusinessRentUseImprovementsYes'
	 */
    public String get_BusinessRentUseImprovementsYes() {
        return this._BusinessRentUseImprovementsYes;
    }

    /**
	 * Class member corresponding to the field 'hs131StateofLegalResidence' in the PDF.
	 */
    private String _hs131StateofLegalResidence = "";

    /**
	 * Mutator Method for xhs131StateofLegalResidence
	 * @param hs131StateofLegalResidence the new value for 'hs131StateofLegalResidence'
	 */
    public void set_hs131StateofLegalResidence(String _hs131StateofLegalResidence) {
        this._hs131StateofLegalResidence = _hs131StateofLegalResidence;
    }

    /**
	 * Accessor Method for xhs131StateofLegalResidence
	 * @return the value of 'hs131StateofLegalResidence'
	 */
    public String get_hs131StateofLegalResidence() {
        return this._hs131StateofLegalResidence;
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
	 * Class member corresponding to the field 'hdbox' in the PDF.
	 */
    private String _hdbox = "";

    /**
	 * Mutator Method for xhdbox
	 * @param hdbox the new value for 'hdbox'
	 */
    public void set_hdbox(String _hdbox) {
        this._hdbox = _hdbox;
    }

    /**
	 * Accessor Method for xhdbox
	 * @return the value of 'hdbox'
	 */
    public String get_hdbox() {
        return this._hdbox;
    }

    /**
	 * Class member corresponding to the field 'hs131TownCityLegalResidence' in the PDF.
	 */
    private String _hs131TownCityLegalResidence = "";

    /**
	 * Mutator Method for xhs131TownCityLegalResidence
	 * @param hs131TownCityLegalResidence the new value for 'hs131TownCityLegalResidence'
	 */
    public void set_hs131TownCityLegalResidence(String _hs131TownCityLegalResidence) {
        this._hs131TownCityLegalResidence = _hs131TownCityLegalResidence;
    }

    /**
	 * Accessor Method for xhs131TownCityLegalResidence
	 * @return the value of 'hs131TownCityLegalResidence'
	 */
    public String get_hs131TownCityLegalResidence() {
        return this._hs131TownCityLegalResidence;
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
	 * Class member corresponding to the field 'BusinessRentUseImprovementsNo' in the PDF.
	 */
    private String _BusinessRentUseImprovementsNo = "";

    /**
	 * Mutator Method for xBusinessRentUseImprovementsNo
	 * @param BusinessRentUseImprovementsNo the new value for 'BusinessRentUseImprovementsNo'
	 */
    public void set_BusinessRentUseImprovementsNo(String _BusinessRentUseImprovementsNo) {
        this._BusinessRentUseImprovementsNo = _BusinessRentUseImprovementsNo;
    }

    /**
	 * Accessor Method for xBusinessRentUseImprovementsNo
	 * @return the value of 'BusinessRentUseImprovementsNo'
	 */
    public String get_BusinessRentUseImprovementsNo() {
        return this._BusinessRentUseImprovementsNo;
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
	 * Class member corresponding to the field 'hs131SpanNumber' in the PDF.
	 */
    private String _hs131SpanNumber = "";

    /**
	 * Mutator Method for xhs131SpanNumber
	 * @param hs131SpanNumber the new value for 'hs131SpanNumber'
	 */
    public void set_hs131SpanNumber(String _hs131SpanNumber) {
        this._hs131SpanNumber = _hs131SpanNumber;
    }

    /**
	 * Accessor Method for xhs131SpanNumber
	 * @return the value of 'hs131SpanNumber'
	 */
    public String get_hs131SpanNumber() {
        return this._hs131SpanNumber;
    }

    /**
	 * Class member corresponding to the field 'dobhs131' in the PDF.
	 */
    private String _dobhs131 = "";

    /**
	 * Mutator Method for xdobhs131
	 * @param dobhs131 the new value for 'dobhs131'
	 */
    public void set_dobhs131(String _dobhs131) {
        this._dobhs131 = _dobhs131;
    }

    /**
	 * Accessor Method for xdobhs131
	 * @return the value of 'dobhs131'
	 */
    public String get_dobhs131() {
        return this._dobhs131;
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
	 * Class member corresponding to the field 'usebx' in the PDF.
	 */
    private String _usebx = "";

    /**
	 * Mutator Method for xusebx
	 * @param usebx the new value for 'usebx'
	 */
    public void set_usebx(String _usebx) {
        this._usebx = _usebx;
    }

    /**
	 * Accessor Method for xusebx
	 * @return the value of 'usebx'
	 */
    public String get_usebx() {
        return this._usebx;
    }

    /**
	 * Class member corresponding to the field 'dateclose' in the PDF.
	 */
    private String _dateclose = "";

    /**
	 * Mutator Method for xdateclose
	 * @param dateclose the new value for 'dateclose'
	 */
    public void set_dateclose(String _dateclose) {
        this._dateclose = _dateclose;
    }

    /**
	 * Accessor Method for xdateclose
	 * @return the value of 'dateclose'
	 */
    public String get_dateclose() {
        return this._dateclose;
    }

    /**
	 * Class member corresponding to the field 'hs131SchoolDistrict' in the PDF.
	 */
    private String _hs131SchoolDistrict = "";

    /**
	 * Mutator Method for xhs131SchoolDistrict
	 * @param hs131SchoolDistrict the new value for 'hs131SchoolDistrict'
	 */
    public void set_hs131SchoolDistrict(String _hs131SchoolDistrict) {
        this._hs131SchoolDistrict = _hs131SchoolDistrict;
    }

    /**
	 * Accessor Method for xhs131SchoolDistrict
	 * @return the value of 'hs131SchoolDistrict'
	 */
    public String get_hs131SchoolDistrict() {
        return this._hs131SchoolDistrict;
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
