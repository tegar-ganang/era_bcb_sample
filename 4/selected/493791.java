package org.amlfilter.test.loader.parser.implementations;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.amlfilter.test.loader.parser.ParserException;
import org.amlfilter.test.loader.parser.XMLParser;
import org.amlfilter.util.AlgorithmUtils;
import org.amlfilter.util.DataFileUtils;
import org.amlfilter.util.DocIdProcessor;
import org.amlfilter.util.GeneralConstants;
import org.amlfilter.util.ObjectUtils;
import org.apache.log4j.Logger;
import org.xmlpull.v1.XmlPullParser;

/**
 * This implementation creates a double index: entity code and blacklist name.
 * 
 * @author Marco
 * @deprecated
 *
 */
public class FactivaParser_doubleIndex extends XMLParser {

    /**
	 * Identification number to determine if 
	 * a de-serialized file is compatible with this class.
	 */
    private static final long serialVersionUID = 1224441125224732625L;

    protected Map<String, HashSet<String>> mIndexForOutputFile_blacklistName = null;

    private String dateType_;

    private StringBuilder firstNames_;

    private boolean isEntity_;

    private boolean isNewName_;

    private boolean isPrimaryName_;

    private HashSet<String> xPathToDateType_ = new HashSet<String>();

    private String xPathToEntityName_;

    private String xPathToMaidenName_;

    private String xPathToMiddleName_;

    private HashSet<String> xPathToNameType_ = new HashSet<String>();

    /**
	 * Constructor
	 */
    public FactivaParser_doubleIndex() {
        setEntityCodePrefix("FCT_");
        mOutput_first_lastName_separator = " ";
        mOutput_in_record_separator = GeneralConstants.AMPERSAND_TOKEN;
        mFieldSeparator = "\t--\t";
        isPrimaryName_ = true;
        isNewName_ = true;
        firstNames_ = new StringBuilder();
        dateType_ = "";
        mPersonRecordNodeName = "Person";
        mEntityRecordNodeName = "Entity";
        mXPathToRecordAction.add("/PFA/Records/Person/@action");
        mXPathToRecordAction.add("/PFA/Records/Entity/@action");
        mXPathToFirstName.add("/PFA/Records/Person/NameDetails/Name/NameValue/FirstName");
        xPathToMiddleName_ = "/PFA/Records/Person/NameDetails/Name/NameValue/MiddleName";
        mXPathToLastName = "/PFA/Records/Person/NameDetails/Name/NameValue/Surname";
        xPathToMaidenName_ = "/PFA/Records/Person/NameDetails/Name/NameValue/MaidenName";
        xPathToEntityName_ = "/PFA/Records/Entity/NameDetails/Name/NameValue/EntityName";
        xPathToNameType_.add("/PFA/Records/Person/NameDetails/Name/@NameType");
        xPathToNameType_.add("/PFA/Records/Entity/NameDetails/Name/@NameType");
        mXPathToDOB.add("/PFA/Records/Person/DateDetails/Date/DateValue/@Year");
        mXPathToDOB.add("/PFA/Records/Entity/DateDetails/Date/DateValue/@Year");
        xPathToDateType_.add("/PFA/Records/Person/DateDetails/Date/@DateType");
        xPathToDateType_.add("/PFA/Records/Entity/DateDetails/Date/@DateType");
        mValidDateTypesForInception.add("Date of Birth");
        mValidDateTypesForInception.add("Date of Registration");
        mXPathToPlaceOfBirth = "/PFA/Records/Person/BirthPlace/Place/@name";
        mXPathToAddressCountry.add("/PFA/Records/Entity/CompanyDetails/AddressCountry");
        mXPathToAddressCountry.add("/PFA/Records/Person/Address/AddressCountry");
        mXPathToCitizenship = "NOT USED???";
        mXPathToCountryType = "/PFA/Records/Person/CountryDetails/Country/@CountryType";
        mXPathToCountryValue = "/PFA/Records/Person/CountryDetails/Country/CountryValue/@Code";
        mXPathToEntityCode.add("/PFA/Records/Person/@id");
        mXPathToEntityCode.add("/PFA/Records/Entity/@id");
        mXPathToIdType.add("/PFA/Records/Person/IDNumberTypes/ID/@IDType");
        mXPathToIdType.add("/PFA/Records/Entity/IDNumberTypes/ID/@IDType");
        mXPathToIdNumber.add("/PFA/Records/Person/IDNumberTypes/ID/IDValue");
        mXPathToIdNumber.add("/PFA/Records/Entity/IDNumberTypes/ID/IDValue");
        mXPathToCategory = "";
        mXPathToPersonType = "";
        mXPathToAlias = "";
        mXPathToAlternativeSpelling = "";
        mXPathToGender = "/PFA/Records/Person/Gender";
        mXPathToSanctionsReference.add("/PFA/Records/Person/SanctionsReferences/Reference");
        mXPathToSanctionsReference.add("/PFA/Records/Entity/SanctionsReferences/Reference");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@SinceDay");
        mXPathIgnoreSet.add("/PFA/Records/Person/Deceased");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@SinceDay");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@ToDay");
        mXPathIgnoreSet.add("/PFA/Records/Person/Address/URL");
        mXPathIgnoreSet.add("/PFA/Records/Person/DateDetails/Date/DateValue/@Dnotes");
        mXPathIgnoreSet.add("/PFA/Records/Person/ProfileNotes");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@ToYear");
        mXPathIgnoreSet.add("/PFA/Records/Person/SourceDescription/Source/@name");
        mXPathIgnoreSet.add("/PFA/Records/Person/Address/AddressCity");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@SinceMonth");
        mXPathIgnoreSet.add("/PFA/Records/Person/Address/AddressLine");
        mXPathIgnoreSet.add("/PFA/Records/Person/NameDetails/Name/NameValue/TitleHonorific");
        mXPathIgnoreSet.add("/PFA/Records/Person/NameDetails/Name/NameValue/SingleStringName");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@ToMonth");
        mXPathIgnoreSet.add("/PFA/Records/Person/Descriptions/Description/@Description2");
        mXPathIgnoreSet.add("/PFA/Records/Person/ActiveStatus");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle");
        mXPathIgnoreSet.add("/PFA/Records/Person/@date");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@SinceMonth");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@ToMonth");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@OccCat");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@ToDay");
        mXPathIgnoreSet.add("/PFA/Records/Person/Images/Image/@URL");
        mXPathIgnoreSet.add("/PFA/Records/Person/SanctionsReferences/Reference/@SinceYear");
        mXPathIgnoreSet.add("/PFA/Records/Person/Descriptions/Description/@Description1");
        mXPathIgnoreSet.add("/PFA/Records/Person/IDNumberTypes/ID/IDValue/@IDnotes");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@SinceYear");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/OccTitle/@ToYear");
        mXPathIgnoreSet.add("/PFA/Records/Person/RoleDetail/Roles/@RoleType");
        mXPathIgnoreSet.add("/PFA/Records/Entity/Descriptions/Description/@Description3");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CompanyDetails/AddressLine");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CompanyDetails/AddressCity");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselFlag");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselCallSign");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselType");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselTonnage");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselGRT");
        mXPathIgnoreSet.add("/PFA/Records/Entity/VesselDetails/VesselOwner");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CompanyDetails/URL");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SourceDescription/Source/@name");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SourceDescription/Source/@name");
        mXPathIgnoreSet.add("/PFA/Records/Entity/ActiveStatus");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@SinceMonth");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@SinceDay");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@SinceYear");
        mXPathIgnoreSet.add("/PFA/Records/Entity/Descriptions/Description/@Description1");
        mXPathIgnoreSet.add("/PFA/Records/Entity/Descriptions/Description/@Description2");
        mXPathIgnoreSet.add("/PFA/Records/Entity/@date");
        mXPathIgnoreSet.add("/PFA/Associations/PublicFigure/Associate/@code");
        mXPathIgnoreSet.add("/PFA/Associations/PublicFigure/Associate/@ex");
        mXPathIgnoreSet.add("/PFA/Associations/PublicFigure/Associate/@id");
        mXPathIgnoreSet.add("/PFA/Associations/PublicFigure/@id");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CountryDetails/Country/CountryValue/@Code");
        mXPathIgnoreSet.add("/PFA/Records/Entity/ProfileNotes");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CountryDetails/Country/@CountryType");
        mXPathIgnoreSet.add("/PFA/Records/Entity/CountryDetails/Country/CountryValue/@Code");
        mXPathIgnoreSet.add("/PFA/Records/Entity/ProfileNotes");
        mXPathIgnoreSet.add("/PFA/Associations/SpecialEntity/Associate/@ex");
        mXPathIgnoreSet.add("/PFA/Associations/SpecialEntity/Associate/@code");
        mXPathIgnoreSet.add("/PFA/Associations/SpecialEntity/Associate/@id");
        mXPathIgnoreSet.add("/PFA/Associations/SpecialEntity/@id");
        mXPathIgnoreSet.add("/PFA/Records/Person/NameDetails/Name/NameValue/OriginalScriptName");
        mXPathIgnoreSet.add("/PFA/Records/Person/DateDetails/Date/DateValue/@Month");
        mXPathIgnoreSet.add("/PFA/Records/Person/DateDetails/Date/DateValue/@Day");
        mXPathIgnoreSet.add("/PFA/Records/Person/NameDetails/Name/NameValue/Suffix");
        mXPathIgnoreSet.add("/PFA/Records/Entity/NameDetails/Name/NameValue/OriginalScriptName");
        mXPathIgnoreSet.add("/PFA/CountryList/CountryName/@code");
        mXPathIgnoreSet.add("/PFA/CountryList/CountryName/@name");
        mXPathIgnoreSet.add("/PFA/CountryList/CountryName/@IsTerritory");
        mXPathIgnoreSet.add("/PFA/CountryList/CountryName/@ProfileURL");
        mXPathIgnoreSet.add("/PFA/SanctionsReferencesList/ReferenceName/@code");
        mXPathIgnoreSet.add("/PFA/SanctionsReferencesList/ReferenceName/@name");
        mXPathIgnoreSet.add("/PFA/SanctionsReferencesList/ReferenceName/@status");
        mXPathIgnoreSet.add("/PFA/SanctionsReferencesList/ReferenceName/@Description2Id");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@ToYear");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@ToDay");
        mXPathIgnoreSet.add("/PFA/Records/Entity/SanctionsReferences/Reference/@ToMonth");
        mXPathIgnoreSet.add("/PFA/OccupationList/Occupation/@code");
        mXPathIgnoreSet.add("/PFA/OccupationList/Occupation/@name");
        mXPathIgnoreSet.add("/PFA/RelationshipList/Relationship/@code");
        mXPathIgnoreSet.add("/PFA/RelationshipList/Relationship/@name");
        mXPathIgnoreSet.add("/PFA/Description1List/Description1Name/@Description1Id");
        mXPathIgnoreSet.add("/PFA/Description1List/Description1Name/@RecordType");
        mXPathIgnoreSet.add("/PFA/NameTypeList/NameType/@NameTypeID");
        mXPathIgnoreSet.add("/PFA/NameTypeList/NameType/@RecordType");
        mXPathIgnoreSet.add("/PFA/RoleTypeList/RoleType/@Id");
        mXPathIgnoreSet.add("/PFA/RoleTypeList/RoleType/@name");
        mXPathIgnoreSet.add("/PFA/Records/Entity/DateDetails/Date/DateValue/@Day");
        mXPathIgnoreSet.add("/PFA/Records/Entity/DateDetails/Date/DateValue/@Month");
        mXPathIgnoreSet.add("/PFA/Records/Entity/IDNumberTypes/ID/IDValue/@IDnotes");
        mXPathIgnoreSet.add("/PFA/Description2List/Description2Name/@Description2Id");
        mXPathIgnoreSet.add("/PFA/Records/Entity/DateDetails/Date/DateValue/@Dnotes");
        mXPathIgnoreSet.add("/PFA/Description2List/Description2Name/@Description1Id");
        mXPathIgnoreSet.add("/PFA/Description3List/Description3Name/@Description3Id");
        mXPathIgnoreSet.add("/PFA/Description3List/Description3Name/@Description2Id");
        mXPathIgnoreSet.add("/PFA/DateTypeList/DateType/@RecordType");
        mXPathIgnoreSet.add("/PFA/DateTypeList/DateType/@Id");
        mXPathIgnoreSet.add("/PFA/DateTypeList/DateType/@name");
        mLog = Logger.getLogger(getClass());
        mFileReader = null;
    }

    /**
	 * Builds a generic record string. Each implementation will override this with its own.
	 * 
	 * @return   formatted string
	 */
    public String buildOutputRecordString() throws Exception {
        return buildRecordStringForMasterFile();
    }

    public void extractRecords(String pFilePath) throws Exception {
        System.out.println("# extractRecords() ... Parsing: " + pFilePath);
        setInputFileName(pFilePath);
        FileInputStream fis = new FileInputStream(getInputFileName());
        mFileReader = new InputStreamReader(fis, "UTF-8");
        mXMLPullParser.setInput(mFileReader);
        int listSize = 0;
        int eventType;
        String nTextNodeSpan;
        boolean isRecordOpened = false;
        int recCount = -1;
        StringBuilder tempSb = new StringBuilder();
        eventType = mXMLPullParser.getEventType();
        int numberOfAttributes = 0;
        eventType = mXMLPullParser.getEventType();
        String startTag = null;
        String endTag = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                mListXmlNodes.add(mXMLPullParser.getName());
                mXmlPath.append("/").append(mXMLPullParser.getName());
                if (mXMLPullParser.getName().equals(mPersonRecordNodeName)) {
                    isRecordOpened = true;
                    isEntity_ = false;
                    startTag = "<Person id=\"";
                    endTag = "</Person>";
                } else if (mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
                    isRecordOpened = true;
                    isEntity_ = true;
                    startTag = "<Entity id=\"";
                    endTag = "</Entity>";
                }
                numberOfAttributes = mXMLPullParser.getAttributeCount();
                if (numberOfAttributes > 0) {
                    for (int i = 0; i < numberOfAttributes; i++) {
                        tempSb.delete(0, tempSb.length());
                        tempSb.append(mXmlPath.toString()).append(mAttribute_prefix_in_path).append(mXMLPullParser.getAttributeName(i));
                        processNrEvent(tempSb.toString(), mXMLPullParser.getAttributeValue(i));
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                listSize = mListXmlNodes.size();
                if (mListXmlNodes.get(listSize - 1).equals(mXMLPullParser.getName())) {
                    mListXmlNodes.remove(listSize - 1);
                    listSize--;
                    mXmlPath = new StringBuilder();
                    for (int i = 0; i < listSize; i++) {
                        mXmlPath.append(SLASH).append(mListXmlNodes.get(i));
                    }
                } else {
                    mLog.error("The closing tag does not match the last opened tag.");
                    throw new ParserException();
                }
                if (mXMLPullParser.getName().equals(mPersonRecordNodeName) || mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
                    isRecordOpened = false;
                    translateAndCleanRecord();
                    persistRecord(mOutputFileChannel);
                    getListAKAs().add(getName().toString());
                    resetMembers();
                    recCount++;
                    if (recCount % 10000 == 0) {
                        System.out.println(" - recCount = " + recCount);
                    }
                }
            } else if ((eventType == XmlPullParser.TEXT) && isRecordOpened) {
                nTextNodeSpan = mXMLPullParser.getText();
                if (!nTextNodeSpan.equals("") && null != nTextNodeSpan) {
                    processNrEvent(mXmlPath.toString(), nTextNodeSpan);
                } else {
                    mLog.debug("Empty element: " + mXmlPath.toString());
                }
            } else {
                mLog.debug("Unhandled event type: " + eventType);
            }
            eventType = mXMLPullParser.next();
        }
    }

    private void informCountry(String pXmlPath, String pText) {
        if (getCountryType().toString().equalsIgnoreCase("Citizenship")) {
            getCitizenshipCountry().add(pText);
        } else if (getCountryType().toString().equalsIgnoreCase("Resident of")) {
            getAddressCountry().add(pText);
        }
    }

    private void informNotProcessed(String pXmlPath, String pTag) {
        if (!mXPathIgnoreSet.contains(pXmlPath)) {
            mLog.debug("* The following node has not been extracted: " + pXmlPath + " (" + pTag + ") " + getName());
        }
    }

    protected void processNrEvent(String pXmlPath, String pText) {
        if (mXPathIgnoreSet.contains(pXmlPath)) {
            return;
        }
        if (mXPathToRecordAction.contains(pXmlPath)) {
            setRecordAction(pText);
        } else if (mXPathToFirstName.contains(pXmlPath)) {
            if (isPrimaryName_) {
                getName().append(pText).append(mOutput_first_lastName_separator);
            } else {
                getListAKAs().add(pText);
                isNewName_ = false;
            }
        } else if (pXmlPath.equals(xPathToMiddleName_)) {
            if (isPrimaryName_) {
                getName().append(pText).append(mOutput_first_lastName_separator);
            } else {
                if (isNewName_) {
                    getListAKAs().add(pText);
                    isNewName_ = false;
                } else {
                    getListAKAs().set(getListAKAs().size() - 1, getListAKAs().get(getListAKAs().size() - 1).concat(mOutput_first_lastName_separator).concat(pText));
                }
            }
        } else if (pXmlPath.equals(mXPathToLastName)) {
            if (isPrimaryName_) {
                firstNames_.delete(0, firstNames_.length());
                firstNames_.append(getName()).append(mOutput_first_lastName_separator);
                getName().append(pText);
            } else {
                if (isNewName_) {
                    getListAKAs().add(pText);
                    isNewName_ = false;
                } else {
                    getListAKAs().set(getListAKAs().size() - 1, getListAKAs().get(getListAKAs().size() - 1).concat(mOutput_first_lastName_separator).concat(pText));
                }
            }
        } else if (pXmlPath.equals(xPathToMaidenName_)) {
            if (isPrimaryName_) {
                getName().append(mOutput_first_lastName_separator).append(pText);
            } else {
                if (isNewName_) {
                    getListAKAs().add(pText);
                    isNewName_ = false;
                } else {
                    getListAKAs().set(getListAKAs().size() - 1, getListAKAs().get(getListAKAs().size() - 1).concat(mOutput_first_lastName_separator).concat(pText));
                }
            }
        } else if (pXmlPath.equals(xPathToEntityName_)) {
            if (isPrimaryName_) {
                getName().append(pText);
            } else {
                getListAKAs().add(pText);
                isNewName_ = false;
            }
        } else if (xPathToNameType_.contains(pXmlPath)) {
            isNewName_ = true;
            setType(new StringBuilder());
            if (isEntity_) {
                getType().append(mEntityRecordNodeName);
            } else {
                getType().append(mPersonRecordNodeName);
            }
            if (pText.equals("Primary Name")) {
                isPrimaryName_ = true;
            } else {
                isPrimaryName_ = false;
            }
        } else if (xPathToDateType_.contains(pXmlPath)) {
            dateType_ = pText;
        } else if (mXPathToDOB.contains(pXmlPath)) {
            if (doesStringMatchAnyValueInSet(mValidDateTypesForInception, dateType_)) {
                getDob().add(pText);
            }
        } else if (pXmlPath.equals(mXPathToPlaceOfBirth)) {
            getPobCountry().add(pText);
        } else if (mXPathToAddressCountry.contains(pXmlPath)) {
            getAddressCountry().add(pText);
        } else if (pXmlPath.equals(mXPathToCitizenship)) {
            getCitizenshipCountry().add(pText);
        } else if (pXmlPath.equals(mXPathToCountryType)) {
            setCountryType(new StringBuilder());
            getCountryType().append(pText);
        } else if (pXmlPath.equals(mXPathToCountryValue)) {
            informCountry(pXmlPath, pText);
        } else if (mXPathToEntityCode.contains(pXmlPath)) {
            mEntityCode.append(pText);
        } else if (mXPathToIdType.contains(pXmlPath)) {
            setIdType(pText);
        } else if (mXPathToIdNumber.contains(pXmlPath)) {
            if (DocIdProcessor.isDocIdTypeSignificant(getIdType())) {
                getIdDocument().add(pText);
            }
        } else if (pXmlPath.equals(mXPathToAlias)) {
            getListAKAs().add(pText);
        } else if (pXmlPath.equals(mXPathToAlternativeSpelling)) {
            getListAKAs().add(pText);
        } else if (pXmlPath.equals(mXPathToPersonType)) {
            getType().append(pText);
        } else if (pXmlPath.equals(mXPathToGender)) {
            getGender().append(pText);
        } else if (mXPathToSanctionsReference.contains(pXmlPath)) {
            getEntity_sources().add(pText);
        } else {
            boolean found = false;
            if (!found) {
                informNotProcessed(pXmlPath, pText);
            }
        }
    }

    protected String translateRecordAction(String pIncomingRecordAction) {
        String retVal = null;
        pIncomingRecordAction = pIncomingRecordAction.trim();
        if (getRecordAction().equalsIgnoreCase("add")) {
            retVal = GeneralConstants.RECORD_ACTION_ADD;
        } else if (getRecordAction().equalsIgnoreCase("chg")) {
            retVal = GeneralConstants.RECORD_ACTION_CHANGE;
        } else if (getRecordAction().equalsIgnoreCase("del")) {
            retVal = GeneralConstants.RECORD_ACTION_DELETE;
        } else {
            throw new IllegalStateException("The action to translate is not known: " + pIncomingRecordAction);
        }
        return retVal;
    }

    protected void identifyRecordPositionInOriginalFile(FileInputStream pFis, String pStartingTag, String pEndingTag) throws Exception {
        int numBytesToRead = 10000;
        FileChannel fc = pFis.getChannel();
        long initialPosition = fc.position();
        long aproximateOffset = initialPosition - numBytesToRead;
        pStartingTag += getEntityCode().toString();
        String block = DataFileUtils.readStringAt(fc, aproximateOffset, numBytesToRead);
        long start = block.indexOf(pStartingTag);
        if (start < 0) {
            numBytesToRead = numBytesToRead * 4;
            aproximateOffset = initialPosition - numBytesToRead;
            block = DataFileUtils.readStringAt(fc, aproximateOffset, numBytesToRead);
            start = block.indexOf(pStartingTag);
            if (start < 0) {
                throw new IllegalStateException("Not able to acquire the start of the record.");
            }
        }
        long end = block.indexOf(pEndingTag, (int) start);
        if (start > end) {
            numBytesToRead = numBytesToRead * 5;
            block = DataFileUtils.readStringAt(fc, aproximateOffset, numBytesToRead);
            end = block.indexOf(pEndingTag, (int) start + numBytesToRead / 5);
            if (start > end) {
                System.out.println("EntityCode: " + getEntityCode());
                numBytesToRead = numBytesToRead * 10;
                block = DataFileUtils.readStringAt(fc, aproximateOffset, numBytesToRead);
                end = block.indexOf(pEndingTag, (int) start + numBytesToRead / 10);
                if (start > end) {
                    System.out.println("Block length: " + block.length());
                    System.out.println("EntityCode: " + getEntityCode());
                    throw new IllegalStateException("Not able to acquire the end of the record.");
                }
            }
        }
        start += aproximateOffset;
        end += aproximateOffset + pEndingTag.length();
        setOriginalRecordStartOffset(start);
        setOriginalRecordSize(end - start);
    }

    /**
	 * This overloads the original method by dealing also with the blacklist name index.
	 */
    public void persistRecord(FileChannel pFc) throws Exception {
        String strRecord = buildOutputRecordString();
        long linePos = 0L;
        if (null == getRecordAction()) {
            System.out.println("- No action for record : " + mEntityCode);
            throw new IllegalArgumentException("** the record : " + mEntityCode + " has no action.");
        }
        if (getRecordAction().equalsIgnoreCase("ADD")) {
            linePos = DataFileUtils.appendLine(pFc, strRecord);
            mIndexForOutputFile_entityCode.put(mEntityCode.toString(), linePos);
            setBlackListNameIndex(getListAKAs(), mEntityCode.toString(), mIndexForOutputFile_blacklistName);
        } else if (getRecordAction().equalsIgnoreCase("CHG")) {
            linePos = DataFileUtils.appendLine(pFc, strRecord);
            mIndexForOutputFile_entityCode.put(mEntityCode.toString(), linePos);
            setBlackListNameIndex(getListAKAs(), mEntityCode.toString(), mIndexForOutputFile_blacklistName);
        } else if (getRecordAction().equalsIgnoreCase("DEL")) {
            mIndexForOutputFile_entityCode.remove(mEntityCode.toString());
        } else {
            throw new IllegalArgumentException("** The processed node does not have info on the action to take about the record. Entity code : " + mEntityCode);
        }
    }

    private void setBlackListNameIndex(List<String> pNames, String pEntityCode, Map<String, HashSet<String>> pIndexMap) throws Exception {
        List<String> listOfBLNames = getBLNames(pNames);
        for (Iterator iterator = listOfBLNames.iterator(); iterator.hasNext(); ) {
            String name = (String) iterator.next();
            HashSet<String> entityCodes = pIndexMap.get(name);
            if (null == entityCodes) {
                entityCodes = new HashSet<String>();
            }
            entityCodes.add(pEntityCode);
            pIndexMap.put(name, entityCodes);
        }
    }

    private List<String> getBLNames(List<String> pNames) {
        List<String> retList = new ArrayList<String>();
        for (int i = 0; i < pNames.size(); i++) {
            String nameToAdd = AlgorithmUtils.cleanString(pNames.get(i));
            retList.add(nameToAdd);
        }
        return retList;
    }

    /**
	 * Finalizes the parsing process.
	 * This overloads the original method by persisting also the blacklist name index.
	 */
    public void finalizeProcess() throws Exception {
        try {
            mFileReader.close();
            mOutputFileChannel.close();
            ObjectUtils.persistObjectToFile(mIndexForOutputFile_entityCode, mOutputIndexFile);
            ObjectUtils.persistObjectToFile(mIndexForOutputFile_blacklistName, mOutputIndexFile.replaceAll(".ind", ".bln.ind"));
        } catch (IOException iOException) {
            mLog.error(iOException);
            throw new ParserException();
        }
    }

    /**
	 * Opens the input file, creates an empty document_ and
	 * creates an instance of an XPathFactory
	 */
    public void initialize() throws Exception {
        super.initialize();
        if (getPreloadIndex()) {
            mIndexForOutputFile_blacklistName = (Map<String, HashSet<String>>) ObjectUtils.readObjectFromFile(getOutputIndexFile().replaceAll(".ind", ".bln.ind"));
        } else {
            mIndexForOutputFile_blacklistName = new HashMap<String, HashSet<String>>();
        }
    }
}
