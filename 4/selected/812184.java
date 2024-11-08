package org.amlfilter.test.loader.parser.implementations;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.List;
import org.amlfilter.test.loader.parser.ParserException;
import org.amlfilter.test.loader.parser.XMLParser;
import org.amlfilter.util.DataFileUtils;
import org.amlfilter.util.DocIdExtractor_EUAddressOtherField;
import org.amlfilter.util.DocIdProcessor;
import org.amlfilter.util.GeneralConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.xmlpull.v1.XmlPullParser;

public class EuParser extends XMLParser {

    /**
	 * Identification number to determine if 
	 * a de-serialized file is compatible with this class.
	 */
    private static final long serialVersionUID = 2244112523232452625L;

    private String dateType_;

    private StringBuilder firstNames_;

    private boolean isEntity_;

    private boolean isNewName_;

    private boolean isPrimaryName_;

    private HashSet<String> xPathToDateType_ = new HashSet<String>();

    private String xPathToEntityName_;

    private String mEntityNameNodeName;

    private String xPathToMiddleName_;

    private HashSet<String> xPathToNameType_ = new HashSet<String>();

    protected HashSet<String> mXPathOtherInfoInAddress = new HashSet<String>();

    /**
	 * Constructor
	 */
    public EuParser() {
        setEntityCodePrefix("GEU_");
        mOutput_first_lastName_separator = " ";
        mFieldSeparator = GeneralConstants.AMLF_TAB_FORMAT_SEPARATOR;
        isPrimaryName_ = true;
        isNewName_ = true;
        firstNames_ = new StringBuilder();
        dateType_ = "";
        mEntityRecordNodeName = "ENTITY";
        mEntityNameNodeName = "NAME";
        mXPathToFirstName.add("/WHOLE/ENTITY/NAME/FIRSTNAME");
        xPathToMiddleName_ = "/WHOLE/ENTITY/NAME/MIDDLENAME";
        mXPathToLastName = "/WHOLE/ENTITY/NAME/LASTNAME";
        xPathToEntityName_ = "/WHOLE/ENTITY/NAME/WHOLENAME";
        mXPathToDOB.add("/WHOLE/ENTITY/BIRTH/DATE");
        mXPathToPlaceOfBirth = "/WHOLE/ENTITY/BIRTH/COUNTRY";
        mXPathToAddressCountry.add("/WHOLE/ENTITY/ADDRESS/COUNTRY");
        mXPathToCitizenship = "/WHOLE/ENTITY/CITIZEN/COUNTRY";
        mXPathToCountryValue = "/WHOLE/ENTITY/PASSPORT/COUNTRY";
        mXPathToEntityCode.add("/WHOLE/ENTITY/@Id");
        mXPathToIdNumber.add("/WHOLE/ENTITY/PASSPORT/NUMBER");
        mXPathOtherInfoInAddress.add("/WHOLE/ENTITY/ADDRESS/OTHER");
        mXPathToCategory = "";
        mXPathToPersonType = "/WHOLE/ENTITY/@Type";
        mXPathToAlias = "";
        mXPathToAlternativeSpelling = "";
        mXPathToGender = "/WHOLE/ENTITY/NAME/GENDER";
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
        boolean isPositionPending = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                mListXmlNodes.add(mXMLPullParser.getName());
                mXmlPath.append("/").append(mXMLPullParser.getName());
                if (mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
                    isRecordOpened = true;
                    isEntity_ = true;
                    isPositionPending = true;
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
                } else {
                    mLog.error("The closing tag does not match the last opened tag.");
                    throw new ParserException();
                }
                if (mXMLPullParser.getName().equals(mEntityNameNodeName)) {
                    dumpNameComponentsIntoAkaList();
                }
                mXmlPath = recreateXpath(mListXmlNodes);
                if (mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
                    isRecordOpened = false;
                    dumpNameComponentsIntoAkaList();
                    translateAndCleanRecord();
                    persistRecord(mOutputFileChannel);
                    resetMembers();
                    recCount++;
                    if (recCount % 10000 == 0) {
                        System.out.println(" - recCount = " + recCount);
                    }
                }
            } else if ((eventType == XmlPullParser.TEXT) && isRecordOpened) {
                nTextNodeSpan = mXMLPullParser.getText();
                if (null != nTextNodeSpan && !nTextNodeSpan.trim().isEmpty()) {
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
        } else if (pXmlPath.equals(mXPathToLastName)) {
            activeName.setSurName(pText);
        } else if (mXPathToFirstName.contains(pXmlPath)) {
            activeName.setFirstName(pText);
        } else if (pXmlPath.equals(xPathToMiddleName_)) {
            activeName.setMiddleName(pText);
        } else if (pXmlPath.equals(xPathToEntityName_)) {
            activeName.setEntityName(pText);
        } else if (xPathToDateType_.contains(pXmlPath)) {
            dateType_ = pText;
        } else if (mXPathToDOB.contains(pXmlPath)) {
            getDob().add(pText);
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
        } else if (mXPathOtherInfoInAddress.contains(pXmlPath)) {
            List<String> idListInOtherField = DocIdExtractor_EUAddressOtherField.extractIdsFromString_EUAddressOtherField(pText);
            if (null != idListInOtherField) {
                getIdDocument().addAll(idListInOtherField);
            }
        } else if (pXmlPath.equals(mXPathToAlias)) {
            getListAKAs().add(pText);
        } else if (pXmlPath.equals(mXPathToAlternativeSpelling)) {
            getListAKAs().add(pText);
        } else if (pXmlPath.equals(mXPathToPersonType)) {
            getType().append(pText);
        } else if (pXmlPath.equals(mXPathToGender)) {
            if (getGender().length() == 0) {
                getGender().append(pText);
            }
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
        retVal = GeneralConstants.RECORD_ACTION_ADD;
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
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        String workingDir = "/raid/opt/data/compressed/load_test/";
        String outputFile = "temp/amlf-test-eu_data.nrd";
        String dataFile = "input/10059696294808_GEU.xml";
        String pCountryTranslatorFile = "d:/data/marco/teky/workspace/amlf-admin/WEB-INF/country_iso_dictionary.txt";
        boolean pAppendToPreviousData = false;
        EuParser euParser = new EuParser();
        euParser.setFieldSeparator(GeneralConstants.AMLF_TAB_FORMAT_SEPARATOR);
        euParser.setOutputFile(workingDir + outputFile);
        euParser.setWorkingDir(workingDir);
        euParser.setCountryTranslatorFile(pCountryTranslatorFile);
        PropertyConfigurator.configure(workingDir + "resources/log4j.parserTester.properties");
        euParser.setPreloadIndex(pAppendToPreviousData);
        euParser.initialize();
        euParser.extractRecords(workingDir + dataFile);
        euParser.finalizeProcess();
    }
}
