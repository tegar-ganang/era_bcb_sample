package org.amlfilter.test.loader.parser.implementations;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import org.amlfilter.test.loader.parser.ParserException;
import org.amlfilter.test.loader.parser.XMLParser;
import org.amlfilter.util.DataFileUtils;
import org.amlfilter.util.DocIdProcessor;
import org.amlfilter.util.GeneralConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.xmlpull.v1.XmlPullParser;

public class SdnParser extends XMLParser {

    /**
	 * Identification number to determine if 
	 * a de-serialized file is compatible with this class.
	 */
    private static final long serialVersionUID = 2244112523232452625L;

    private String dateType_;

    private boolean isNewName_;

    private String mXPathToAkaFirstName;

    private String mXPathToAkaLastName;

    /**
	 * Constructor
	 */
    public SdnParser() {
        setEntityCodePrefix("SDN_");
        mOutput_first_lastName_separator = " ";
        mFieldSeparator = GeneralConstants.AMLF_TAB_FORMAT_SEPARATOR;
        isNewName_ = true;
        dateType_ = "";
        mEntityRecordNodeName = "sdnEntry";
        mXPathToFirstName.add("/sdnList/sdnEntry/firstName");
        mXPathToLastName = "/sdnList/sdnEntry/lastName";
        mXPathToAkaFirstName = "/sdnList/sdnEntry/akaList/aka/firstName";
        mXPathToAkaLastName = "/sdnList/sdnEntry/akaList/aka/lastName";
        mXPathToDOB.add("/sdnList/sdnEntry/dateOfBirthList/dateOfBirthItem/dateOfBirth");
        mXPathToPlaceOfBirth = "/sdnList/sdnEntry/placeOfBirthList/placeOfBirthItem/placeOfBirth";
        mXPathToAddressCountry.add("/sdnList/sdnEntry/addressList/address/country");
        mXPathToCitizenship = "/sdnList/sdnEntry/citizenshipList/citizenship/country";
        mXPathToEntityCode.add("/sdnList/sdnEntry/uid");
        mXPathToIdNumber.add("/sdnList/sdnEntry/idList/id/idNumber");
        mXPathToCategory = "";
        mXPathToPersonType = "/sdnList/sdnEntry/sdnType";
        mXPathToNewNameTrigger.add("/sdnList/sdnEntry/akaList/aka");
        mXPathToNewNameTrigger.add("/sdnList/sdnEntry/programList");
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
        eventType = mXMLPullParser.getEventType();
        eventType = mXMLPullParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                mListXmlNodes.add(mXMLPullParser.getName());
                mXmlPath.append("/").append(mXMLPullParser.getName());
                if (mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
                    isRecordOpened = true;
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
                if (mXPathToNewNameTrigger.contains(mXmlPath.toString())) {
                    dumpNameComponentsIntoAkaList();
                }
                mXmlPath = recreateXpath(mListXmlNodes);
                if (mXMLPullParser.getName().equals(mPersonRecordNodeName) || mXMLPullParser.getName().equals(mEntityRecordNodeName)) {
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
                    mLog.debug("Empty element: " + mXmlPath.toString() + "\t text: '" + nTextNodeSpan + "'");
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
            activeName.setFirstName(pText);
        } else if (pXmlPath.equals(mXPathToLastName)) {
            activeName.setSurName(pText);
        } else if (pXmlPath.equals(mXPathToAkaFirstName)) {
            activeName.setFirstName(pText);
        } else if (pXmlPath.equals(mXPathToAkaLastName)) {
            activeName.setSurName(pText);
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
        String outputFile = "temp/amlf-test-sdn_data.nrd";
        String dataFile = "input/10151805566955_SDN.xml";
        String pCountryTranslatorFile = "d:/data/marco/teky/workspace/amlf-admin/WEB-INF/country_iso_dictionary.txt";
        boolean pAppendToPreviousData = false;
        SdnParser sdnParser = new SdnParser();
        sdnParser.setFieldSeparator(GeneralConstants.AMLF_TAB_FORMAT_SEPARATOR);
        sdnParser.setOutputFile(workingDir + outputFile);
        sdnParser.setWorkingDir(workingDir);
        sdnParser.setCountryTranslatorFile(pCountryTranslatorFile);
        PropertyConfigurator.configure(workingDir + "resources/log4j.parserTester.properties");
        sdnParser.setPreloadIndex(pAppendToPreviousData);
        sdnParser.initialize();
        sdnParser.extractRecords(workingDir + dataFile);
        sdnParser.finalizeProcess();
    }
}
