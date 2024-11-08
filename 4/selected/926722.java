package org.epo.jbps.heart.xmlparser;

import java.io.*;
import org.xml.sax.*;
import org.apache.log4j.Logger;
import org.epo.jbps.heart.request.*;
import org.epo.jbps.heart.main.*;
import org.epo.jbps.generic.*;
import org.epo.jbps.util.Tools;
import org.epoline.jsf.utils.Log4jManager;
import java.util.zip.*;

/**
 * Class describing the state EndingDocset (when encountering /Docset)
 *
 * @author Infotel Conseil
 */
public class ParserStateEndingDocset extends ParserState {

    /**
 * ParserStateEndingDocset constructor
 * @param int theEventId, Attribute theAtts
 */
    public ParserStateEndingDocset(int theEventId, Attributes theAtts) {
        super();
    }

    /**
 * Merge the converted files if necessary or put in sub-directory
 * @param theXmlHandler org.epo.jbps.heart.xmlparser.SaxHandler
 * @param theEventId int
 * @param theAtts org.xml.sax.Attributes
 * @exception org.epo.jbps.heart.main.BpsProcessException The exception description.
 */
    public void mergeConvertedFiles(SaxHandler theXmlHandler, int theEventId, Attributes theAtts) throws BpsProcessException {
        String myOutputFormat;
        String myFinalFile;
        String myHdrFile;
        RandomAccessFile myHdrFileAccess;
        int myTempElemNb;
        String myTempFile;
        String myTempFile2;
        RandomAccessFile myFinalFileAccess;
        RandomAccessFile myTempFileAccess;
        final int myBufferSize;
        byte[] myBuffer;
        int myNbRead;
        String myRequestDir;
        File myRequestDirId;
        myOutputFormat = "?";
        myFinalFile = null;
        myHdrFile = "header.ps";
        myTempElemNb = 0;
        myTempFile = null;
        myTempFile2 = null;
        myFinalFileAccess = null;
        myTempFileAccess = null;
        myBufferSize = 4096;
        myBuffer = new byte[myBufferSize];
        myNbRead = 0;
        myRequestDir = null;
        myRequestDirId = null;
        myOutputFormat = theXmlHandler.getRequestData().getDocSet().getOutputFormat();
        try {
            myFinalFile = theXmlHandler.getRequestData().getDocSet().getGenFileName();
            myRequestDir = theXmlHandler.getOutputDir() + File.separatorChar + theXmlHandler.getRequestData().getBpaRequest().getRequestNumber().trim() + "@" + ((BpsProcess) (theXmlHandler.getJBpsThread())).getSysParam().getBpsManagerName();
            myRequestDirId = new File(myRequestDir);
            if (myRequestDirId.exists() == false) myRequestDirId.mkdir();
            if (myFinalFile.charAt(0) != '/') {
                myFinalFile = myRequestDir + File.separatorChar + myFinalFile;
            } else {
                myFinalFile = myFinalFile.replace('/', '~');
                myFinalFile = myRequestDir + File.separatorChar + myFinalFile;
            }
            if (myOutputFormat.equalsIgnoreCase(DocSet.OUTPUT_FORMAT_PS)) {
                myFinalFileAccess = new RandomAccessFile(myFinalFile, "rw");
                myHdrFileAccess = new RandomAccessFile(theXmlHandler.getStaticDir() + File.separatorChar + myHdrFile, "r");
                while ((myNbRead = myHdrFileAccess.read(myBuffer, 0, myBufferSize)) != -1) {
                    myFinalFileAccess.write(myBuffer, 0, myNbRead);
                }
                myHdrFileAccess.close();
                myTempElemNb = theXmlHandler.getRequestData().getDocSet().getSize();
                for (int i = 0; i < myTempElemNb; i++) {
                    myTempFile = theXmlHandler.getRequestData().getDocSet().getTempElem(i).getTempFileName();
                    String myDuplexChoice = null;
                    if (theXmlHandler.getRequestData().getDocSet().getTempElem(i).getDocsetObject().getType() == DocsetObject.DOC) myDuplexChoice = ((Doc) (theXmlHandler.getRequestData().getDocSet().getTempElem(i).getDocsetObject())).getMethod(); else if (theXmlHandler.getRequestData().getDocSet().getTempElem(i).getDocsetObject().getType() == DocsetObject.APPDOC) myDuplexChoice = ((AppDoc) (theXmlHandler.getRequestData().getDocSet().getTempElem(i).getDocsetObject())).getMethod();
                    String myStapleStatus = null;
                    myStapleStatus = theXmlHandler.getRequestData().getDocSet().getTempElem(i).getStaplingStatus();
                    if (myDuplexChoice != null) myFinalFileAccess.writeBytes("(" + myDuplexChoice + ") BPSDuplex\r\n");
                    if (myStapleStatus != null) myFinalFileAccess.writeBytes("(" + myStapleStatus + ") BPSStaple\r\n");
                    for (int j = 0; j < theXmlHandler.getRequestData().getDocSet().getTempElem(i).getNbPages(); j++) {
                        myTempFile2 = myTempFile + "." + j;
                        if (myTempFile2 != null) {
                            myTempFileAccess = new RandomAccessFile(myTempFile2, "r");
                            while ((myNbRead = myTempFileAccess.read(myBuffer, 0, myBufferSize)) != -1) {
                                myFinalFileAccess.write(myBuffer, 0, myNbRead);
                            }
                            myTempFileAccess.close();
                        }
                    }
                }
                myFinalFileAccess.close();
            }
            if (myOutputFormat.equalsIgnoreCase(DocSet.OUTPUT_FORMAT_PDF)) {
                myTempElemNb = theXmlHandler.getRequestData().getDocSet().getSize();
                for (int i = 0; i < myTempElemNb; i++) {
                    myTempFile = theXmlHandler.getRequestData().getDocSet().getTempElem(i).getTempFileName();
                    for (int j = 0; j < theXmlHandler.getRequestData().getDocSet().getTempElem(i).getNbPages(); j++) {
                        myTempFile2 = myTempFile + "." + j;
                        if (myTempFile2 != null) {
                            int mySnowReturn = 0;
                            System.gc();
                        }
                    }
                }
            }
            if (myOutputFormat.equalsIgnoreCase(DocSet.OUTPUT_FORMAT_TIFF)) {
                String inputDir = theXmlHandler.getRequestData().getWorkDirectory() + File.separatorChar + "DocSet" + (theXmlHandler.getRequestData().getSize() - 1);
                String[] myFullList = sortTiffFileList(Tools.getFileList(inputDir), theXmlHandler);
                File myOutputDir = new File(myRequestDir + File.separatorChar + theXmlHandler.getRequestData().getDocSet().getGenFileName());
                myOutputDir.mkdir();
                for (int i = 0; i < myFullList.length; i++) {
                    myTempFile = myRequestDir + File.separatorChar + theXmlHandler.getRequestData().getDocSet().getGenFileName() + File.separatorChar + constructTiffFilename("" + (i + 1));
                    Tools.copyFile(inputDir + File.separatorChar + myFullList[i], myTempFile);
                }
            }
            if (myOutputFormat.equalsIgnoreCase(DocSet.OUTPUT_FORMAT_THUMB)) {
                String inputDir = theXmlHandler.getRequestData().getWorkDirectory() + File.separatorChar + "DocSet" + (theXmlHandler.getRequestData().getSize() - 1);
                String[] myFullList = Tools.getFileList(inputDir);
                File myOutputDir = new File(myRequestDir + File.separatorChar + theXmlHandler.getRequestData().getDocSet().getGenFileName());
                myOutputDir.mkdir();
                for (int i = 0; i < myFullList.length; i++) {
                    myTempFile = myRequestDir + File.separatorChar + theXmlHandler.getRequestData().getDocSet().getGenFileName() + File.separatorChar + myFullList[i].substring(myFullList[i].lastIndexOf('.') + 1);
                    Tools.copyFile(inputDir + File.separatorChar + myFullList[i], myTempFile);
                }
            }
            if (myOutputFormat.equalsIgnoreCase(DocSet.OUTPUT_FORMAT_ASIS)) {
                String inputDir = theXmlHandler.getRequestData().getWorkDirectory() + File.separatorChar + "DocSet" + (theXmlHandler.getRequestData().getSize() - 1);
                String[] myFullList = Tools.getFileList(inputDir);
                File myOutputDir = new File(myFinalFile);
                myOutputDir.mkdir();
                for (int i = 0; i < myFullList.length; i++) {
                    myTempFile = myFinalFile + File.separatorChar + myFullList[i];
                    Tools.copyFile(inputDir + File.separatorChar + myFullList[i], myTempFile);
                }
            }
        } catch (IOException error) {
            throw (new BpsProcessException(BpsException.ERR_PROCESS_RETRY, "Error in file management for docset ending : " + error.getMessage()));
        } catch (BpsException e) {
            throw (new BpsProcessException(BpsException.ERR_PROCESS_RETRY, "Error in file management for docset ending : " + e.getMessage()));
        }
    }

    /**
 * Next states of the current state depending on incoming event
 * @return ParserState
 * @param SaxHandler theXmlHandler,  int eventId , Attributes theAtts
 */
    public ParserState nextState(SaxHandler theXmlHandler, int theEventId, Attributes theAtts) {
        ParserState myNextState = null;
        switch(theEventId) {
            case XmlTagsTable.DOCSET:
                myNextState = new ParserStateProcessingDocset(theEventId, theAtts);
                break;
            default:
                myNextState = this;
        }
        return (myNextState);
    }

    /**
 * This method is triggered when entering in the current state.
 * @param theXmlHandler SaxHandler
 */
    public void onEntry(SaxHandler theXmlHandler) {
        DocSet myDocset = null;
        myDocset = theXmlHandler.getRequestData().getDocSet();
        theXmlHandler.getRequestData().addDocSet(myDocset);
    }

    /**
 * This method is triggered when exiting the current state.
 * @param SaxHandler theXmlHandler
 */
    public void onExit(SaxHandler theXmlHandler) {
        int nbCopies = 0;
        int myStartDocset = 0;
        int myCpt1 = 0;
        String myOriginalFile = "";
        String myFinalFile = "";
        String myDestFile = "";
        String myRequestDir = "";
        DocSet myDocset = null;
        myDocset = theXmlHandler.getRequestData().getDocSet();
        nbCopies = myDocset.getCopies();
        myStartDocset = myDocset.getSize();
        myRequestDir = theXmlHandler.getOutputDir() + File.separatorChar + theXmlHandler.getRequestData().getBpaRequest().getRequestNumber().trim() + "@" + ((BpsProcess) (theXmlHandler.getJBpsThread())).getSysParam().getBpsManagerName();
        myFinalFile = myDocset.getGenFileName();
        myOriginalFile = myFinalFile;
        if (myFinalFile.charAt(0) == '/') {
            myFinalFile = myFinalFile.replace('/', '~');
        }
        myFinalFile = myRequestDir + File.separatorChar + myFinalFile;
        try {
            for (myCpt1 = myStartDocset; myCpt1 < myStartDocset + nbCopies - 1; myCpt1++) {
                if (myOriginalFile.charAt(0) == '/') {
                    myOriginalFile = myDestFile.replace('/', '~');
                }
                myDestFile = (myCpt1 - myStartDocset + 1) + "_" + myOriginalFile;
                myDocset.setGenFileName(myDestFile);
                myDestFile = myRequestDir + File.separatorChar + myDestFile;
                theXmlHandler.getRequestData().addDocSet(myDocset);
                Tools.copyFile(myFinalFile, myDestFile);
            }
        } catch (BpsException e) {
        }
    }

    /**
 * Method triggered while runnning in the current state
 * @param SaxHandler theXmlHandler, int theEventId, AttributeList theAtts
 * @exception BpsProcessException
 */
    public void run(SaxHandler theXmlHandler, int theEventId, Attributes theAtts) throws BpsProcessException {
        if (theXmlHandler.getRequestData().getDocSet().getZipOption().equals("NO")) mergeConvertedFiles(theXmlHandler, theEventId, theAtts); else zipDocsetFiles(theXmlHandler, theEventId, theAtts);
    }

    /**
 * Zip the docset files in a zip file
 * Creation date: (22/02/02 15:57:47)
 * @param theXmlHandler org.epo.jbps.heart.xmlparser.SaxHandler
 * @param theEventId int
 * @param theAtts org.xml.sax.Attributes
 * @exception org.epo.jbps.heart.main.BpsProcessException
 */
    public void zipDocsetFiles(SaxHandler theXmlHandler, int theEventId, Attributes theAtts) throws BpsProcessException {
        ZipOutputStream myZipOut = null;
        BufferedInputStream myDocumentInputStream = null;
        String myFinalFile = null;
        String myTargetPath = null;
        String myTargetFileName = null;
        String myInputFileName = null;
        byte[] myBytesBuffer = null;
        int myLength = 0;
        try {
            myZipOut = new ZipOutputStream(new FileOutputStream(myFinalFile));
            myZipOut.putNextEntry(new ZipEntry(myTargetPath + myTargetFileName));
            myDocumentInputStream = new BufferedInputStream(new FileInputStream(myInputFileName));
            while ((myLength = myDocumentInputStream.read(myBytesBuffer, 0, 4096)) != -1) myZipOut.write(myBytesBuffer, 0, myLength);
            myZipOut.closeEntry();
            myZipOut.close();
        } catch (FileNotFoundException e) {
            throw (new BpsProcessException(BpsProcessException.ERR_OPEN_FILE, "FileNotFoundException while building zip dest file"));
        } catch (IOException e) {
            throw (new BpsProcessException(BpsProcessException.ERR_OPEN_FILE, "IOException while building zip dest file"));
        }
    }

    /**
 * The constructTiffFileName(String fileName) method construct a valid zero left paded filename.
 * + 0001 at the end
 * Example : if page number of tiff document is 2, then file name is 00020001.tif
 * 			 if page number of tiff document is 21, then file name is 00210001.tif
 * @return String
 * The complete file name
 * @param fileName
 * The original file name
 */
    private String constructTiffFilename(String fileName) {
        String retString = "";
        for (int i = 0; i < 4 - fileName.length(); i++) {
            retString = retString + '0';
        }
        retString = retString + fileName + "0001.tif";
        return retString;
    }

    /**
 * The sortTiffFileList(String[] theFileList) method sorts the list of tiff files
 * in page order (page 10 not after 1 but after 9)
 * @return String []
 * The sorted file list
 * @param theFileList
 * The non sorted file list
 */
    private String[] sortTiffFileList(String[] theFileList, SaxHandler theXmlHandler) {
        String[] myFileList = new String[theFileList.length];
        int myOldDocNum = 0, myNewDocNum = 0, myOldPageNum = 0, myNewPageNum = 0;
        for (int i = 0; i < theFileList.length; i++) {
            for (int j = 0; j < theFileList.length; j++) {
                myNewDocNum = Integer.parseInt(theFileList[j].substring(0, theFileList[j].lastIndexOf("_")));
                myNewPageNum = Integer.parseInt(theFileList[j].substring(theFileList[j].lastIndexOf(".") + 1)) + 1;
                if (myNewDocNum >= myOldDocNum) {
                    if (myNewDocNum > myOldDocNum) myOldPageNum = 0;
                    if (myNewPageNum == (myOldPageNum + 1)) {
                        myFileList[i] = theFileList[j];
                        myOldDocNum = myNewDocNum;
                        myOldPageNum = myNewPageNum;
                        break;
                    }
                }
            }
        }
        return myFileList;
    }
}
