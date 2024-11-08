package issrg.SAWS;

import issrg.SAWS.util.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.apache.soap.util.xml.*;
import org.xml.sax.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.security.AlgorithmParameters;
import javax.crypto.spec.*;
import java.security.KeyStore;
import java.security.Key;
import java.security.cert.Certificate;
import javax.security.auth.callback.*;
import java.lang.reflect.Constructor;
import javax.servlet.http.*;
import issrg.SAWS.callback.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * This is the SAWS Server class. It provides all the necessary API methods for SAWS API clients. 
 *
 * @author W. Xu
 */
public class SAWSServer {

    private static final issrg.utils.Version version = new issrg.utils.Version("issrg/SAWS/version", "saws");

    private Callback[] cbs = null;

    private boolean writingHeartBeating = false;

    private String encryptionKeystoreLocation = null;

    private String signingKeystoreLocation = null;

    private int numberOfPasswordShares = 2;

    private int numberOfEncPasswordShares = 2;

    private int heartbeatInterval = 0;

    private String rootCA = null;

    private String trustedLocation = null;

    private String vtPKC = null;

    private int signRecordNumber;

    private String SAWSInterface = null;

    private Map UserDNIDMap = new Hashtable();

    private Map UserIDPKMap = new Hashtable();

    private String logEncryption = "no";

    private int debugLevel = SAWSConstant.ErrorInfo;

    private String signingAlg = null;

    private String hashAlgorithm = "SHA1";

    private String latestLogFilename = null;

    private int SNFromTCB = -1;

    private byte[] AccHashFromTCB = null;

    private CallbackHandler callbackHandler = new SAWSCmdPromptCallbackHandler();

    private String callBackHandlerClass = "issrg.SAWS.callback.SAWSCmdPromptCallbackHandler";

    private TCBContentRW tcbContent = null;

    private LogFilenameClass lfc = null;

    private LogFileWriter currentLogFileWriter = null;

    private TCBKeystoreManagement tcbKM = null;

    private String logFileRoot = null;

    private String currentInspectionLogFilename = null;

    byte[] currentInspecitonAccHash = null;

    byte[] currentInspectionSignature = null;

    private String previousLogFilenameFromLogRoot = null;

    private String previousLogFilenameFromLogRecord = null;

    public Vector logFileList = new Vector();

    public int currentReadingFileNo = 0;

    public Vector recordBlockListFromOneLogFile = null;

    private SecretKey sawsTCBSecretKey = null;

    private PBEParameterSpec paramSpec = null;

    Vector waitingRecordList0 = new Vector();

    Vector waitingRecordList1 = new Vector();

    boolean busyFlag0V = false;

    boolean busyFlag1V = false;

    Thread thread = null;

    int recordCount = 0;

    private long currentTime;

    private java.util.Timer timer = null;

    private TimerTask tt2 = null;

    private boolean closed = false;

    /**
     * @aggregation composite
     */
    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(SAWSServer.class.getName());

    /**
   * This class is used to hold a message record block waiting to be written to the log file.
   */
    class WaitingRecordBlock {

        public WaitingRecordBlock() {
        }

        public WaitingRecordBlock(byte[] messageBlock, byte recordType, byte encryptionFlag, byte userID) {
            this.messageBlock = messageBlock;
            this.recordType = recordType;
            this.encryptionFlag = encryptionFlag;
            this.userID = userID;
        }

        public byte[] messageBlock = null;

        public byte recordType;

        public byte encryptionFlag;

        public byte userID;
    }

    /**
   * This method is the constructor of SAWSServer. It is for SAWS web service interface.
   * It is equivalent to the following constructor when flag = 1 (see SAWSServer(int flag))
   */
    public SAWSServer() {
        readConf();
        sawsInit();
        sawsStart();
    }

    /**
   * This method is the constructor of SAWSServer.
   * It accepts flag values 0 or 1. The flag=0 is for SAWS command line mode. The initialization process for logging client records is run when flag=1. It consists of reading the TCB(Trusted Computing Base) content to get the last log file name, accumulated hash 
   * and last record sequence number. It also creates a new log file to start logging and adds the information to the TCB location 
   * (e.g. name, random number). The initialization process includes verification of the last log file, creating a list of the 
   * log files in the log repository and verifying the log file chain if the administrator decides to.The chain can be constructured since each log file contains a record indicating the previous log 
   * file name. The first log file name contains a record indicating that "this is the first log file"
   * 
   * @param int flag =0: for SAWS command line mode initialisation;  
   * flag=1: on top of flag=0, initializes SAWSServer for preparing to record client records. 
   * 
   */
    public SAWSServer(int flag) {
        readConf();
        if (flag == 1) {
            sawsInit();
        }
    }

    /**
   * This method is to read the configuration file, called by the SAWSServer constructor.
   * 
   */
    private void readConf() {
        java.net.URL configURL = this.getClass().getResource("saws.xml");
        File configFile = new File("saws.xml");
        if (!(configFile.exists())) configFile = new File(configURL.getFile());
        java.io.BufferedReader in = null;
        try {
            in = new java.io.BufferedReader(new java.io.FileReader(configFile));
        } catch (Exception e) {
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e.toString());
            System.exit(-1);
        }
        DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();
        org.w3c.dom.Document doc = null;
        try {
            doc = xdb.parse(new InputSource(in));
        } catch (Exception e) {
            if (debugLevel >= SAWSConstant.ErrorInfo) {
                sawsDebugLog.write(e.toString() + "\nThe SAWS configuration file saws.xml could not be read." + "\nPlease check Line " + ((SAXParseException) e).getLineNumber() + " and Column " + ((SAXParseException) e).getColumnNumber() + " in saws.xml.");
            }
            this.showMessage("The SAWS configuration file saws.xml could not be read." + "\nPlease check Line " + ((SAXParseException) e).getLineNumber() + " and Column " + ((SAXParseException) e).getColumnNumber() + " in saws.xml.", SAWSTextOutputCallback.ERROR);
            System.exit(-1);
        }
        org.w3c.dom.Element root = null;
        if (doc != null) {
            root = doc.getDocumentElement();
        }
        NodeList nl4 = root.getElementsByTagName("CallbackHandler");
        org.w3c.dom.Element e5 = (org.w3c.dom.Element) nl4.item(0);
        String className = null;
        if (e5 != null) {
            className = DOMUtils.getAttribute(e5, "class");
            if (className != null) {
                this.callBackHandlerClass = className;
                String errorMessage = null;
                try {
                    if (this.callBackHandlerClass.equals("issrg.SAWS.callback.SAWSFileCallbackHandler")) {
                        String inputFile = DOMUtils.getAttribute(e5, "inputFile");
                        String outputFile = DOMUtils.getAttribute(e5, "outputFile");
                        this.callbackHandler = new SAWSFileCallbackHandler(inputFile, outputFile);
                    } else {
                        this.callbackHandler = (CallbackHandler) (Class.forName(this.callBackHandlerClass).newInstance());
                    }
                } catch (IllegalArgumentException iae) {
                    errorMessage = iae.getMessage();
                } catch (InstantiationException ie) {
                    errorMessage = "The callback handler class \"" + className + "\" could not be instantiated." + "\nPlease check the class name in the configuration file (saws.xml).";
                } catch (ClassNotFoundException cnfe) {
                    errorMessage = "The callback handler class \"" + className + "\" could not be found." + "\nPlease check the class name in the configuration file (saws.xml)" + " or the class path.";
                } catch (IllegalAccessException iae) {
                    errorMessage = "The callback handler class \"" + className + "\" could not be accessed." + "\nPlease check the permissions to run the specified class.";
                } finally {
                    if (errorMessage != null) {
                        String[] options = { "Continue", "Stop" };
                        if (debugLevel >= SAWSConstant.ErrorInfo) {
                            sawsDebugLog.write(errorMessage);
                        }
                        errorMessage = errorMessage + "\n\nSAWS can use a default callback handler. Please select \"Continue\"" + "\nto use the default handler, or select \"Stop\" to finish SAWS.";
                        int selection = this.createConfirmCallback(errorMessage, options, SAWSChoiceCallback.WARNING, "CallbackHandlerError");
                        if (selection == 1) {
                            System.exit(-1);
                        }
                    }
                }
            }
        }
        NodeList nl = root.getElementsByTagName("SAWSBasic");
        org.w3c.dom.Element e = (org.w3c.dom.Element) nl.item(0);
        encryptionKeystoreLocation = DOMUtils.getAttribute(e, "encryptionKeystoreLocation");
        rootCA = DOMUtils.getAttribute(e, "rootCA");
        if (rootCA != null) {
            File rootCAFile = new File(rootCA);
            if (!rootCAFile.exists()) {
                this.showMessage("SAWS can't find the rootCA public key certificate." + rootCA + "\n\nSAWS will stop and the SAWS administrator needs to put the root CA public key certificate" + "\nin the correct position as specified in the SAWS configuration file saws.xml.", SAWSTextOutputCallback.WARNING);
                System.exit(-1);
            }
        } else {
            this.showMessage("The rootCA public key certificate was not specified." + "\n\nSAWS will stop and the SAWS administrator needs to specify the root CA public key certificate" + "\nin the SAWS configuration file saws.xml.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        signRecordNumber = Integer.parseInt(DOMUtils.getAttribute(e, "signRecordNumber"));
        numberOfEncPasswordShares = Integer.parseInt(DOMUtils.getAttribute(e, "numberOfEncPasswordShares"));
        heartbeatInterval = Integer.parseInt(DOMUtils.getAttribute(e, "heartbeatInterval"));
        logFileRoot = DOMUtils.getAttribute(e, "logFileRoot");
        vtPKC = DOMUtils.getAttribute(e, "vtPKC");
        SAWSInterface = DOMUtils.getAttribute(e, "SAWSInterface");
        logEncryption = DOMUtils.getAttribute(e, "logEncryption");
        debugLevel = Integer.parseInt(DOMUtils.getAttribute(e, "debugLevel"));
        sawsDebugLog.write(e.toString() + "\nLog encryption: " + logEncryption);
        NodeList nl2 = root.getElementsByTagName("TPMAdvanced");
        org.w3c.dom.Element e2 = (org.w3c.dom.Element) nl2.item(0);
        signingKeystoreLocation = DOMUtils.getAttribute(e2, "signingKeystoreLocation");
        numberOfPasswordShares = Integer.parseInt(DOMUtils.getAttribute(e2, "numberOfPasswordShares"));
        trustedLocation = DOMUtils.getAttribute(e2, "trustedLocation");
        String s1 = DOMUtils.getAttribute(e2, "accumulatedHashAlgorithm");
        if (s1 != null) {
            if (s1.equalsIgnoreCase("SHA-1") || s1.equalsIgnoreCase("SHA1") || s1.equalsIgnoreCase("SHA")) {
                this.hashAlgorithm = "SHA1";
            } else if (s1.equalsIgnoreCase("MD5")) {
                this.hashAlgorithm = "MD5";
            } else if (s1.equalsIgnoreCase("SHA-256") || s1.equalsIgnoreCase("SHA256")) {
                this.hashAlgorithm = "SHA256";
            } else if (s1.equalsIgnoreCase("SHA-384") || s1.equalsIgnoreCase("SHA384")) {
                this.hashAlgorithm = "SHA384";
            } else if (s1.equalsIgnoreCase("SHA-512") || s1.equalsIgnoreCase("SHA512")) {
                this.hashAlgorithm = "SHA512";
            } else {
                this.showMessage("The hash algorithm specified in SAWS configuration file (saws.xml) is not supported." + "\n\nSAWS will stop and the SAWS administrator needs to specify the the correct algorithm," + "\nor remove the specification from the configuration file to use the default algorithm (SHA-1).", SAWSTextOutputCallback.WARNING);
                System.exit(-1);
            }
        }
        this.signingAlg = DOMUtils.getAttribute(e2, "signingAlgorithm");
        NodeList nl3 = root.getElementsByTagName("UserInfo");
        int leng = nl3.getLength();
        for (int i = 0; i < leng; i++) {
            org.w3c.dom.Element e3 = (org.w3c.dom.Element) nl3.item(i);
            String UserDNString = DOMUtils.getAttribute(e3, "userDN");
            UserDNString = issrg.utils.RFC2253NameParser.toCanonicalDN(UserDNString).toUpperCase();
            Byte UserIDByte = new Byte((byte) Integer.parseInt(DOMUtils.getAttribute(e3, "userID")));
            String userPKC = DOMUtils.getAttribute(e3, "userPKC");
            PublicKey userPK = null;
            if (userPKC != null) {
                userPK = retrievePublicKey(userPKC);
            }
            if (UserDNString != null && UserIDByte != null) {
                UserDNIDMap.put(UserDNString, UserIDByte);
            }
            if (userPK != null && UserIDByte != null) {
                UserIDPKMap.put(UserIDByte, userPK);
            }
        }
    }

    /**
   * This method is retrieve the public key from the PKC file.
   * 
   * @param PKCFilename The name of the file with the PKC.
   * 
   * @return The public key.
   * 
   */
    private PublicKey retrievePublicKey(String PKCFilename) {
        File f = new File(PKCFilename);
        if (!f.exists()) {
            String[] options = { "Continue", "Stop SAWS" };
            int select = this.createConfirmCallback("SAWS can't find the PKC file " + PKCFilename + ". " + "\nYou will not be able to use the corresponding user Private Key to read the log file " + "\nif the log file is encrypted. " + "\nDo you want to continue? \n", options, SAWSChoiceCallback.WARNING, "MissingPKCFile");
            if (select == 1) System.exit(-1);
            return null;
        }
        PublicKey pk = null;
        try {
            FileInputStream fis = new FileInputStream(PKCFilename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            java.security.cert.CertificateFactory cf = null;
            cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate certTemp = cf.generateCertificate(bis);
            pk = certTemp.getPublicKey();
        } catch (Exception e2) {
            if (debugLevel > SAWSConstant.ErrorInfo) sawsDebugLog.write(e2.toString());
            String[] options = { "Continue", "Stop SAWS" };
            int select = this.createConfirmCallback("SAWS can't read the public key from the PKC file " + PKCFilename + "." + "\nYou will not be able to use the corresponding user Private Key to read the log file " + "\nif the log file is encrypted. " + "\nDo you want to continue? \n", options, SAWSChoiceCallback.WARNING, "ReadingPKWarning");
            if (select == 1) System.exit(-1);
            return null;
        }
        return pk;
    }

    /**
     * This method verifies if the signing algorithm is valid.
     * 
     * @param pkAlgorithm The public key algorithm to check if 
     * the signing algorithm is compatible with it.
     */
    private void validateSigningAlgorithm(String pkAlgorithm) {
        if (this.signingAlg == null) {
            String[] options = { "OK", "NO-ABORT" };
            int selection = this.createConfirmCallback("No signing algorithm was specified in SAWS configuration file (saws.xml)." + "\n\nWould you like to proceed using SHA1with" + pkAlgorithm + " as signing algorithm?", options, SAWSTextOutputCallback.WARNING, "Signing Algorithm");
            if (selection == 1) {
                System.exit(-1);
            } else {
                this.signingAlg = "SHA1with" + pkAlgorithm;
            }
        } else {
            int index = this.signingAlg.toUpperCase().indexOf("WITH");
            if (index < 0) {
                this.showMessage("The signing algorithm in SAWS configuration file (saws.xml) is not valid." + "\nThe algorithm must be in the following format: <digest algorithm>WITH<signing private key algorithm>." + "\nPlease correct the configuration file." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
                System.exit(-1);
            }
            String hashAlg = this.signingAlg.substring(0, index);
            String encAlg = this.signingAlg.substring(index + 4);
            if (!encAlg.equalsIgnoreCase(pkAlgorithm)) {
                this.showMessage("The signing algorithm in SAWS configuration file (saws.xml) is not compatible with " + "\nthe signing public key algorithm: " + pkAlgorithm + "." + "\nPlease correct the configuration file." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
                System.exit(-1);
            }
            if (encAlg.equalsIgnoreCase("DSA")) {
                if (hashAlg.equalsIgnoreCase("SHA1") || hashAlg.equalsIgnoreCase("SHA-1") || hashAlg.equalsIgnoreCase("SHA")) {
                    this.signingAlg = "SHA1withDSA";
                } else {
                    this.showMessage("The signing algorithm in SAWS configuration file (saws.xml) is not supported." + "\nDSA can only be used with SHA1. Please use SHA1withDSA in the configuration file." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
                    System.exit(-1);
                }
            } else if (encAlg.equalsIgnoreCase("RSA")) {
                if (hashAlg.equalsIgnoreCase("SHA-1") || hashAlg.equalsIgnoreCase("SHA1") || hashAlg.equalsIgnoreCase("SHA")) {
                    this.signingAlg = "SHA1withRSA";
                } else if (hashAlg.equalsIgnoreCase("SHA-256") || hashAlg.equalsIgnoreCase("SHA256")) {
                    this.signingAlg = "SHA256withRSA";
                } else if (hashAlg.equalsIgnoreCase("SHA-384") || hashAlg.equalsIgnoreCase("SHA384")) {
                    this.signingAlg = "SHA384withRSA";
                } else if (hashAlg.equalsIgnoreCase("SHA-512") || hashAlg.equalsIgnoreCase("SHA512")) {
                    this.signingAlg = "SHA512withRSA";
                } else if (hashAlg.equalsIgnoreCase("MD5")) {
                } else {
                    this.showMessage("The signing algorithm in SAWS configuration file (saws.xml) is not supported." + "\nRSA can be used with SHA1, MD5, SHA256, SHA384 or SHA512 (e.g. SHA256withRSA)." + "\nPlease correct the configuration file." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
                    System.exit(-1);
                }
            }
        }
    }

    /**
   * This method is the initialisation method of SAWSServer for preparing to record client records. 
   * 
   */
    private void sawsInit() {
        File logRoot = new File(logFileRoot);
        if (!logRoot.exists()) logRoot.mkdir();
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.checkSigningKeystoreFile();
        tcbKM.checkEncKeystoreFile();
        tcbKM.readKeystores();
        PublicKey signingPK = tcbKM.getsawsSigningPublicKey();
        this.validateSigningAlgorithm(signingPK.getAlgorithm());
        sawsTCBSecretKey = tcbKM.getsawsTCBSecretKey();
        if (sawsTCBSecretKey == null) {
            this.showMessage("SAWS cannot generate the necessary secret key for the TCB. " + "There is probably some problem with your Java cryptography libray." + "\nSAWS will stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        paramSpec = tcbKM.getparamSpec();
        readTCBContent(trustedLocation);
        if ((previousLogFilenameFromLogRoot == null) && (currentInspectionLogFilename == null)) {
            String[] options = { "OK", "NO-ABORT" };
            int selection = this.createConfirmCallback("This is the first time that SAWS has been started.", options, SAWSChoiceCallback.WARNING, "FirstTimeInitialization");
            if (selection == 1) {
                System.exit(-1);
            }
        }
        latestLogFilename = lfc.generateNewLogFileName();
        currentLogFileWriter = new LogFileWriter(logFileRoot, latestLogFilename, tcbKM.getvtEncryptionPublicKey(), tcbKM.getsawsEncryptionPublicKey(), tcbKM.getsawsSigningPrivateKey(), tcbKM.getbaSigningPublicKeyCert(), this.hashAlgorithm, null, UserIDPKMap, this.signingAlg, this.callbackHandler);
        currentLogFileWriter.setDebugLevel(debugLevel);
        int ret = currentLogFileWriter.prepareNewLog();
        if (ret != 0) {
            System.exit(-1);
        }
        tcbContent = new TCBContentRW(trustedLocation, sawsTCBSecretKey, paramSpec, debugLevel, this.callbackHandler);
        tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
        int result = tcbContent.write();
        if (result != 0) {
            this.showMessage("SAWS cannot write to TCB correctly." + "\nSAWS will stop.\n", SAWSTextOutputCallback.WARNING);
            closeLog();
            System.exit(-1);
        }
        if ((previousLogFilenameFromLogRoot == null) && (currentInspectionLogFilename == null)) {
            currentLogFileWriter.createLastFileRecord("This is the very first SAWS log file.", "null".getBytes(), "null".getBytes());
        } else if ((previousLogFilenameFromLogRoot != null) && (currentInspectionLogFilename == null)) {
            logFileList.addElement(previousLogFilenameFromLogRoot);
            currentLogFileWriter.createSAWSRecord(("" + SAWSConstant.TrustedLocationMissingErrCode + "; Trusted Location is missing and reconstructed.").getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
            currentInspectionLogFilename = previousLogFilenameFromLogRoot;
            verifyOneLogFile(logFileRoot, currentInspectionLogFilename);
            currentLogFileWriter.createLastFileRecord(currentInspectionLogFilename, currentInspecitonAccHash, currentInspectionSignature);
            currentInspectionLogFilename = previousLogFilenameFromLogRecord;
            verifyLogFileChainWithPrompt(logFileRoot, currentInspectionLogFilename);
        } else if (currentInspectionLogFilename != null) {
            logFileList.addElement(currentInspectionLogFilename);
            verifyOneLogFile(logFileRoot, currentInspectionLogFilename);
            currentLogFileWriter.createLastFileRecord(currentInspectionLogFilename, currentInspecitonAccHash, currentInspectionSignature);
            currentInspectionLogFilename = previousLogFilenameFromLogRecord;
            verifyLogFileChainWithPrompt(logFileRoot, currentInspectionLogFilename);
        }
    }

    /**
   * This method is to start SAWSServer to record client records. 
   * 
   */
    public void sawsStart() {
        if (debugLevel > SAWSConstant.VerboseInfo && logFileList.size() != 0) {
            for (int i = logFileList.size() - 1; i >= 0; --i) {
                String logFilename = (String) logFileList.get(i);
                sawsDebugLog.write("Log file name in SAWSServer start: " + logFilename);
            }
        }
        if (this.closed) {
            this.showMessage("SAWS log file is already closed. SAWS have to be initialized again.", SAWSTextOutputCallback.WARNING);
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write("SAWS log file is already closed when trying to start SAWS.");
            }
        }
        String[] options = { "Yes, continue", "No, stop SAWS" };
        int selection = this.createConfirmCallback("SAWS has finished its initilisation process. " + "\nNow SAWS can start to record client log records. Do you want to continue? \n", options, SAWSChoiceCallback.WARNING, "StartRecordingLogs");
        if (selection == 1) {
            closeLog();
            System.exit(-1);
        }
        thread = new WritingThread();
        thread.start();
        currentTime = System.currentTimeMillis();
        if (heartbeatInterval != 0) {
            setHeartbeatWriter(heartbeatInterval);
        }
    }

    /**
   * This method is to read the trusted location, to get the latest log filename, SN, AccHash
   * 
   * @param trustedLocationLocal The path to the trusted location.
   */
    private void readTCBContent(String trustedLocationLocal) {
        if (trustedLocationLocal == null) {
            this.showMessage("The trustedLocation is not present in the SAWS configuration file." + "\nPlease set it correctly in the SAWS configuration file. \n" + "\nSAWS will now stop.", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        File TCBFile = new File(trustedLocationLocal);
        lfc = new LogFilenameClass(this.debugLevel);
        if (!TCBFile.exists()) {
            String[] options = { "1. Create Trusted Location", "2. Stop SAWS", "3. Rebuild Trusted Location" };
            int selection = this.createConfirmCallback("SAWS cannot find the Trusted Location." + "\n\nOption 1: A Trusted Location does not exist because this is the first time SAWS has been started. " + "\nSAWS should create a new Trusted Location. " + "\nOption 2: The Trusted Location has been lost due to computer failure, or the configuration file is wrong. " + "\nSAWS should stop and then be manually restarted." + "\nOption 3: The Trusted Location has been lost due to a compromise or computer failure. " + "\nSAWS should try to rebuild it.", options, SAWSChoiceCallback.WARNING, "TrustedLocationNotFound");
            if (selection == 1) System.exit(-1);
            if (selection == 2) {
                previousLogFilenameFromLogRoot = lfc.findLatestLogFileName(logFileRoot);
                currentInspectionLogFilename = null;
                AccHashFromTCB = null;
            }
            if (selection == 0) {
                previousLogFilenameFromLogRoot = null;
                currentInspectionLogFilename = null;
                AccHashFromTCB = null;
            }
        } else {
            previousLogFilenameFromLogRoot = lfc.findLatestLogFileName(logFileRoot);
            tcbContent = new TCBContentRW(trustedLocationLocal, sawsTCBSecretKey, paramSpec, debugLevel, this.callbackHandler);
            int ret = tcbContent.read();
            if (ret != 0) {
                String[] options = { "SAWS Stop", "Rebuild TCBLocation" };
                int selection = this.createConfirmCallback("Data corruption for the Trusted Location. " + "\nOption 1: The Trusted Location has been tampered with. " + "\nSAWS should stop and then the administrator needs to investigate it manually." + "\nOption 2: SAWS will rebuild the Trusted Location.", options, SAWSChoiceCallback.WARNING, "TrustedLocationDataCorruption");
                if (selection == 0) System.exit(-1);
                if (selection == 1) {
                    previousLogFilenameFromLogRoot = lfc.findLatestLogFileName(logFileRoot);
                    currentInspectionLogFilename = null;
                    AccHashFromTCB = null;
                }
            } else {
                currentInspectionLogFilename = tcbContent.getLastFilename();
                SNFromTCB = tcbContent.getLastSN();
                AccHashFromTCB = tcbContent.getLastAccHash();
                File cLogFile = new File(logFileRoot, currentInspectionLogFilename);
                if (!cLogFile.exists()) {
                    String[] options = { "Stop", "Continue" };
                    int selection = this.createConfirmCallback("SAWS cannot find the current log file for verification: " + currentInspectionLogFilename + " \n\nOption 1: The current log file is missing because of tampering. " + "\nSAWS will stop and the SAWS administrator needs to check it manually. " + "\nOption 2: The current log file is missing because of tampering or computer failure. " + "\nSAWS will continue to verify the next previous file. ", options, SAWSChoiceCallback.WARNING, "MissingCurrentVerifyingLogFile");
                    if (selection == 0) System.exit(-1);
                    if (selection == 1) {
                        currentInspectionLogFilename = previousLogFilenameFromLogRoot;
                    }
                }
            }
        }
    }

    /**
   * This method simply pickup all the previous log file names one by one through the log file
   * chain. 
   * 
   * @param logFileRoot the log file root
   * @param cLogFileName the current log file name in the log file chain
   */
    private void pickupAllPreviousLogFileNames(String logFileRoot, String cLogFileName) {
        currentInspectionLogFilename = cLogFileName;
        while (currentInspectionLogFilename != null) {
            File logF = new File(logFileRoot, currentInspectionLogFilename);
            if (logF.exists()) {
                pickupOnePreviousLogFileName(logFileRoot, currentInspectionLogFilename);
                currentInspectionLogFilename = previousLogFilenameFromLogRecord;
            } else {
                this.showMessage("SAWS cannot find the previous log file for reading: " + currentInspectionLogFilename + " \n\nSAWS will stop and the SAWS administrator needs to restart SAWS to check it. ", SAWSTextOutputCallback.WARNING);
                currentLogFileWriter.createSAWSRecord(("" + SAWSConstant.LogFileMissingErrCode + ";" + currentInspectionLogFilename + ";This log file is missing.").getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                closeLog();
                System.exit(-1);
            }
        }
    }

    /**
   * This method simply pickup the previous log file name from the current log file. 
   * The previous log file name is stored in Vector logFileList. 
   * 
   * @param logFileRoot the log file root
   * @param cLogFileName the current log file name in the log file chain
   *
   */
    private void pickupOnePreviousLogFileName(String logFileRoot, String inspectLogFile) {
        String cLogFilename = inspectLogFile;
        LogFileReader rr = new LogFileReader(debugLevel, this.callbackHandler);
        rr.setSAWSPrivateKey(tcbKM.getsawsEncryptionPrivateKey());
        try {
            rr.setLogFilename(logFileRoot, cLogFilename);
        } catch (logReadingException e) {
            this.showMessage("The error for the log file " + cLogFilename + " is: " + SAWSConstant.getErrorString(e.getErrorCode()) + "\n\nThis log file cannot be read. " + "\nSAWS will stop. Please restart SAWS to check it. ", SAWSTextOutputCallback.WARNING);
            closeLog();
            System.exit(-1);
        }
        byte[] secureRandomB = null;
        try {
            try {
                secureRandomB = rr.getSecureRandomNumber(tcbKM.getsawsEncryptionPrivateKey());
            } catch (logReadingException secureHashError) {
                secureHashError.setErrorCode(SAWSConstant.SecureRandomRecordErrCode);
                throw secureHashError;
            }
            previousLogFilenameFromLogRecord = rr.findPreviousLogfileName(secureRandomB);
            if (previousLogFilenameFromLogRecord != null) {
                logFileList.addElement(previousLogFilenameFromLogRecord);
            }
        } catch (logReadingException e) {
            this.showMessage("The error for the log file " + cLogFilename + " is: " + SAWSConstant.getErrorString(e.getErrorCode()) + "\n\nThis log file has been tampered with and it cannot be read. " + "\nSAWS will stop. Please restart SAWS to check it. ", SAWSTextOutputCallback.WARNING);
            closeLog();
            System.exit(-1);
        }
        return;
    }

    /**
   * This method is to verify the log file chain with prompts to allow SAWS administrator to decide 
   * whether to verify the log files or not.
   * 
   * @param logFileRoot the log file root
   * @param cLogFileName the first log file in the log file chain
   *
   */
    private void verifyLogFileChainWithPrompt(String logFileRoot, String cLogFileName) {
        currentInspectionLogFilename = cLogFileName;
        AccHashFromTCB = null;
        boolean inspectAll = false;
        while (currentInspectionLogFilename != null) {
            int selection = 0;
            if (!inspectAll) {
                String[] options = { "1. No", "2. Check it", "3. Check all" };
                selection = this.createConfirmCallback("This log file: " + currentInspectionLogFilename + " was created by SAWS before." + " Do you want SAWS to check it? " + "\n\nOption 1: No, SAWS will ignore the log file and start to record client records." + "\nOption 2: Yes, SAWS will check this log file only" + "\nOption 3: SAWS will check this log file and all previous log files " + "linked by this log file.", options, SAWSChoiceCallback.WARNING, "CheckExistingLogFile");
                if (selection == 0) {
                    pickupAllPreviousLogFileNames(logFileRoot, currentInspectionLogFilename);
                    return;
                }
                if (selection == 2) inspectAll = true;
            }
            File logF = new File(logFileRoot, currentInspectionLogFilename);
            if (logF.exists()) {
                verifyOneLogFile(logFileRoot, currentInspectionLogFilename);
                currentInspectionLogFilename = previousLogFilenameFromLogRecord;
            } else {
                String[] options = { "Stop", "Continue" };
                selection = this.createConfirmCallback("SAWS cannot find the log file for verification: " + currentInspectionLogFilename + "." + "\n\nOption 1: This log file is missing because of tampering. " + "\nSAWS will stop and the SAWS administrator needs to check it manually. " + "\nOption 2: The current log file is missing because of tampering or computer failure. " + "\nSAWS will continue to verify the next previous file. ", options, SAWSChoiceCallback.WARNING, "CheckExistingLogFile");
                currentLogFileWriter.createSAWSRecord(("" + SAWSConstant.LogFileMissingErrCode + ";" + currentInspectionLogFilename + ";" + selection + ";This log file is missing.").getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                if (selection == 0) {
                    closeLog();
                    System.exit(-1);
                }
                if (selection == 1) {
                    currentInspectionLogFilename = lfc.getPreviousLogFilename(logFileRoot, currentInspectionLogFilename);
                }
            }
        }
    }

    /** 
   * This method verifies a given log file. After it is invoked, recordBlockListFromOneLogFile will be filled with the log records from the log file. 
   *
   * @param logFileRoot String is the log file root.
   * @param inspectLogFile String is the inspection log file name. 
   *
   * @return int 0: verify OK; otherwise: some error found
   */
    private int verifyOneLogFile(String logFileRoot, String inspectLogFile) {
        String cLogFilename = inspectLogFile;
        LogFileReader rr = new LogFileReader(debugLevel, this.callbackHandler);
        rr.setSAWSPrivateKey(tcbKM.getsawsEncryptionPrivateKey());
        try {
            rr.setLogFilename(logFileRoot, cLogFilename);
        } catch (logReadingException e) {
            this.showMessage("The error for the log file " + cLogFilename + " is: " + SAWSConstant.getErrorString(e.getErrorCode()) + "\n\nThis log file cannot be read. " + "\nSAWS will stop. Please restart SAWS to check it. ", SAWSTextOutputCallback.WARNING);
            closeLog();
            System.exit(-1);
        }
        byte[] secureRandomB = null;
        try {
            try {
                secureRandomB = rr.getSecureRandomNumber(tcbKM.getsawsEncryptionPrivateKey());
            } catch (logReadingException secureRandomError) {
                secureRandomError.setErrorCode(SAWSConstant.SecureRandomRecordErrCode);
                throw secureRandomError;
            }
            rr.checkLogFile(secureRandomB);
            rr.checkSignature(tcbKM.getrootCAPublicKey());
            recordBlockListFromOneLogFile = rr.getRecordBlockList();
            int currentRecordWriteCount = rr.getCurrentSN();
            currentInspecitonAccHash = rr.getAccumulatedHashFromLog();
            currentInspectionSignature = rr.getSignatureFromLog();
            if (AccHashFromTCB != null) {
                if (SNFromTCB != currentRecordWriteCount) {
                    String[] options = { "Stop", "Continue after tampering", "Continue after crash" };
                    int selection = this.createConfirmCallback("The last sequence number " + currentRecordWriteCount + " of the log file " + cLogFilename + "\nis not equal to the last sequence number " + SNFromTCB + " in the Trusted Location" + "\n\nOption 1: Either the log file or the Trusted Location has been tampered with. " + "\nSAWS will stop and the SAWS administrator needs to check it manually." + "\nOption 2: Either the log file or the Trusted Location has been tampered with, " + "\nSAWS will record this incident, update the Trusted Location and continue." + "\nOption 3: This error is due to computer crash. " + "\nSAWS will record this incident, update the Trusted Location and continue.", options, SAWSChoiceCallback.WARNING, "SequenceNumberDifferentFromTCB");
                    currentLogFileWriter.createSAWSRecord(("" + SAWSConstant.SNNotMatchBetweenLogAndTCBLocationErrCode + "; " + cLogFilename + "; " + currentRecordWriteCount + "; " + SNFromTCB + "; " + selection + "; Sequence number does not match.").getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                    if (selection == 0) {
                        closeLog();
                        System.exit(-1);
                    }
                }
                if (utility.toHexString(AccHashFromTCB).compareTo(utility.toHexString(currentInspecitonAccHash)) != 0) {
                    String[] options = { "Stop", "Continue after tampering", "Continue after crash" };
                    int selection = this.createConfirmCallback("The accumulated hash of the log file " + cLogFilename + "is not equal to the one in the Trusted Location" + "\n\nOption 1: Either the log file or the Trusted Location has been tampered with. " + "\nSAWS will stop and the SAWS administrator needs to check it manually." + "\nOption 2: Either the log file or the Trusted Location has been tampered with, " + "\nSAWS will record this incident, update the Trusted Location and continue." + "\nOption 3: This error is due to computer crash. " + "\nSAWS will record this incident, update the Trusted Location and continue.", options, SAWSChoiceCallback.WARNING, "AccHashDifferentFromTCB");
                    currentLogFileWriter.createSAWSRecord(("" + SAWSConstant.AccHashNotMatchBetweenLogAndTCBLocationErrCode + "; " + cLogFilename + "; " + utility.toHexString(currentInspecitonAccHash) + "; " + utility.toHexString(AccHashFromTCB) + "; " + selection + "; accumualted hash does not match.").getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                    if (selection == 0) {
                        closeLog();
                        System.exit(-1);
                    }
                }
            }
            previousLogFilenameFromLogRecord = rr.getPreviousLogfileName();
        } catch (logReadingException e) {
            if (e.getErrorCode() != SAWSConstant.LogFileIncompleteErrCode) {
                String[] options = { "Stop", "Continue after tampering", "Continue after crash" };
                int selection = this.createConfirmCallback("The error for the log file " + cLogFilename + " is: " + SAWSConstant.getErrorString(e.getErrorCode()) + "\n\nOption 1: This log file has been tampered with and it cannot be recovered. " + "\nSAWS will record this incident and stop." + "The SAWS administrator needs to check it manually." + "\nOption 2: The log file has been tampered with. SAWS will record this incident and continue." + "\nOption 3: This error is due to computer crash. SAWS will record this incident and continue. ", options, SAWSChoiceCallback.WARNING, "CannotRecoverLogFile");
                currentLogFileWriter.createSAWSRecord(("" + e.getErrorCode() + "; " + cLogFilename + ";" + selection + "; " + e.getSequence() + "; " + SAWSConstant.getErrorString(e.getErrorCode())).getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                if (selection == 0) {
                    closeLog();
                    System.exit(-1);
                }
                currentInspecitonAccHash = "error".getBytes();
                currentInspectionSignature = "error".getBytes();
                previousLogFilenameFromLogRecord = lfc.getPreviousLogFilename(logFileRoot, cLogFilename);
            }
            if (e.getErrorCode() == SAWSConstant.LogFileIncompleteErrCode) {
                String[] options = { "Stop", "Recover this log", "Ignore this log" };
                int selection = this.createConfirmCallback("The error for the log file " + cLogFilename + " is: " + SAWSConstant.getErrorString(e.getErrorCode()) + "\n\nOption 1: This log file has been tampered with. " + "\nSAWS will record this incident and stop." + "The SAWS administrator needs to check it manually." + "\nOption 2: This error is a computer crash. SAWS will recover this log file and continue." + "\nOption 3: SAWS will ignore this log and continue. ", options, SAWSChoiceCallback.WARNING, "ReadingLogFileError");
                currentLogFileWriter.createSAWSRecord((e.getErrorCode() + "; " + cLogFilename + "; " + selection + "; " + e.getSequence() + "; " + SAWSConstant.getErrorString(e.getErrorCode())).getBytes(), SAWSConstant.SysAuditorNotificationType, SAWSConstant.NoEncryptionFlag);
                if (selection == 0) {
                    closeLog();
                    System.exit(-1);
                }
                if (selection == 1) {
                    LogFileWriter repairWriter = new LogFileWriter(logFileRoot, cLogFilename, tcbKM.getvtEncryptionPublicKey(), tcbKM.getsawsEncryptionPublicKey(), tcbKM.getsawsSigningPrivateKey(), tcbKM.getbaSigningPublicKeyCert(), this.hashAlgorithm, secureRandomB, null, this.signingAlg);
                    int ret = repairWriter.repairLog(logFileRoot, cLogFilename, secureRandomB, e);
                    currentInspecitonAccHash = repairWriter.getAccHash();
                    currentInspectionSignature = repairWriter.getSignature();
                    previousLogFilenameFromLogRecord = lfc.getPreviousLogFilename(logFileRoot, cLogFilename);
                    if (ret != 0) {
                        return -1;
                    }
                }
                if (selection == 2) {
                    currentInspecitonAccHash = "error".getBytes();
                    currentInspectionSignature = "error".getBytes();
                    previousLogFilenameFromLogRecord = lfc.getPreviousLogFilename(logFileRoot, cLogFilename);
                }
            }
        }
        if (previousLogFilenameFromLogRecord != null && currentReadingFileNo == 0) {
            logFileList.addElement(previousLogFilenameFromLogRecord);
        }
        return 0;
    }

    /**
   * This method is to read saws log files one at a time from the first log file to the last log file 
   * in the log file list logFileList. So you can invoke it again and again unitl null is returned.  
   *
   * @return Vector the Vector of log records
   */
    public Vector sawsReadOneLogFile() {
        if (currentReadingFileNo == logFileList.size()) {
            return null;
        }
        verifyOneLogFile(logFileRoot, (String) logFileList.get(logFileList.size() - 1 - (currentReadingFileNo++)));
        return recordBlockListFromOneLogFile;
    }

    /**
   * This method closes the current log file.
   */
    public void closeLog() {
        if (this.tt2 != null) {
            ((HeartbeatRecordWriting) this.tt2).setStop(true);
            timer.cancel();
        }
        currentLogFileWriter.finalizeLogFile();
        tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
        int result = tcbContent.write();
        if (result != 0) {
            this.showMessage("SAWS cannot write to TCB correctly. " + "\nSAWS will stop. \n", SAWSTextOutputCallback.WARNING);
            System.exit(-1);
        }
        this.closed = true;
    }

    /**
   * This method is used by the SAWS API client to send a log message to SAWS server.
   * No encryption is used by this method. 
   * 
   * @param messageBlock is the log message to be sent. 
   * @return The status of the record (RecordStatus).
   */
    public RecordStatus sendLogRecord(byte[] messageBlock) {
        byte flag;
        if ((logEncryption == null) || (logEncryption.compareTo("no") == 0)) {
            flag = (byte) SAWSConstant.NoEncryptionFlag;
        } else {
            flag = (byte) SAWSConstant.SymmetricEncryptionFlag;
        }
        return sendLogRecord(messageBlock, flag);
    }

    /**
   * This method is used by the SAWS API client to send a log message to SAWS server. 
   * Since this log message is coming from a SAWS client, so the log message type is certainly 
   * SAWSClientLogDataType.
   *
   * @param messageBlock is the log message to be sent. 
   * @param encryptionFlag encryption flag to indicate the encryption requirement. 
   * SAWSConstant.NoEncryptionFlag: log with no encryption;
   * SAWSConstant.SymmetricEncryptionFlag: log with symmetric encryption;  
   * SAWSConstant.CommandFlag: command for SAWS to perform: the command contents are contained in messageBlock. 
   * 
   * @return The status of the record (RecordStatus).
   */
    public RecordStatus sendLogRecord(byte[] messageBlock, byte encryptionFlag) {
        if (this.closed) {
            return new RecordStatus(-1, SAWSConstant.LogFileClosed);
        }
        if ((encryptionFlag != SAWSConstant.NoEncryptionFlag) && (encryptionFlag != SAWSConstant.SymmetricEncryptionFlag) && (encryptionFlag != SAWSConstant.CommandFlag)) {
            return new RecordStatus(-1, SAWSConstant.InvalidEncryptionFlag);
        }
        String mBlock = new String(messageBlock);
        if ((encryptionFlag == (byte) SAWSConstant.CommandFlag) && (mBlock.compareTo("closeLogFile") == 0)) {
            while ((busyFlag0V == true) || (busyFlag1V == true)) {
                if (debugLevel > SAWSConstant.NoInfo) System.err.println("wait.............");
            }
            currentLogFileWriter.finalizeLogFile();
            tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
            tcbContent.write();
            this.closed = true;
            System.exit(0);
        }
        byte userID = (byte) 0x00;
        String userdn = null;
        if (SAWSInterface.compareTo("webservice") == 0) {
            userdn = getSSLDN();
            Byte B1 = (Byte) UserDNIDMap.get(userdn);
            if (B1 == null) return new RecordStatus(-1, SAWSConstant.UnauthorizedUser);
            userID = (byte) (B1.byteValue());
        }
        if (busyFlag0V == false) {
            if (debugLevel > SAWSConstant.NoInfo) {
                sawsDebugLog.write("\nThis is in sending thread when busyFlag0V == false");
            }
            busyFlag0V = true;
            WaitingRecordBlock wRecordBlock = new WaitingRecordBlock(messageBlock, SAWSConstant.SAWSClientLogDataType, encryptionFlag, userID);
            waitingRecordList0.addElement(wRecordBlock);
            busyFlag0V = false;
        } else {
            if (debugLevel > SAWSConstant.VerboseInfo) {
                sawsDebugLog.write("\nThis is in sending thread when busyFlag1V == false");
            }
            busyFlag1V = true;
            WaitingRecordBlock wRecordBlock = new WaitingRecordBlock(messageBlock, SAWSConstant.SAWSClientLogDataType, encryptionFlag, userID);
            waitingRecordList1.addElement(wRecordBlock);
            busyFlag1V = false;
        }
        while (writingHeartBeating) {
        }
        thread.run();
        return new RecordStatus(0, currentLogFileWriter.getCurrentRecordWriteCount());
    }

    /**
   * This method is used by the SAWS command line mode to output the PKC request file from 
   * the signing keystore.
   */
    public void outputPKCRequest() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.outputPKCRequest();
    }

    /**
   * This method is used by the SAWS command line mode to output the PKC
   * in the signing keystore.
   */
    public void exportSigningPKC() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.exportSigningPKC();
    }

    /**
   * This method is used by the SAWS command line mode to create the encryption keystore
   * 
   */
    public void createEncryptionKeystore() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.createEncryptionKeystore();
    }

    /**
   * This method is used by the SAWS command line mode to create the signing keystore
   * 
   */
    public void createSigningKeystore() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.createSigningKeystore();
    }

    /**
   * This method is used by the SAWS command line mode to import the root CA PKC into the signing keystore
   * 
   */
    public void importRootCA() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.checkSigningKeystoreFile();
        tcbKM.importRootCA();
    }

    /**
   * This method is used by the SAWS command line mode to import the signing PKC which is issued by 
   * root CA into the signing keystore
   * 
   */
    public void importSigningPKC() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.checkSigningKeystoreFile();
        tcbKM.importSigningPKC();
    }

    /**
   * This method is used by the SAWS command line mode to list all entries in the signing keystore
   * 
   */
    public void listSigningKeystore() {
        tcbKM = new TCBKeystoreManagement(signingKeystoreLocation, numberOfPasswordShares, encryptionKeystoreLocation, numberOfEncPasswordShares, rootCA, vtPKC, debugLevel, this.signingAlg, this.callbackHandler);
        tcbKM.checkSigningKeystoreFile();
        tcbKM.listSigningKeystore();
    }

    /**
   * This method is to get the DN from the SSL certificate. It is only used in web services interfaces.
   *
   * 
   * @return String the DN of the SSL certificate. 
   */
    private String getSSLDN() {
        String issuerDN = null;
        org.apache.axis.MessageContext mct = org.apache.axis.AxisEngine.getCurrentMessageContext();
        org.apache.axis.MessageContext context = mct.getCurrentContext();
        HttpServletRequest req = (HttpServletRequest) context.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);
        String certAttribute = "javax.servlet.request.X509Certificate";
        java.security.cert.X509Certificate[] certificate = (java.security.cert.X509Certificate[]) req.getAttribute(certAttribute);
        if (certificate != null) {
            java.security.cert.X509Certificate certificateSource = certificate[0];
            if (certificateSource != null) {
                issuerDN = issrg.utils.RFC2253NameParser.toCanonicalDN(certificateSource.getSubjectDN().getName()).toUpperCase();
                if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("DN is: " + issuerDN);
            }
        } else {
            this.showMessage("Certificate is null.", SAWSTextOutputCallback.WARNING);
        }
        return issuerDN;
    }

    /**
   * This class is the writing thread of the SAWS server. It reads waitingRecordList0 and 
   * waitingRecordList1 and write the record blocks in them into the current log file. 
   *
   */
    class WritingThread extends Thread {

        WaitingRecordBlock wRecordBlock = new WaitingRecordBlock();

        public void run() {
            {
                if ((busyFlag0V == false) && (waitingRecordList0.size() > 0)) {
                    busyFlag0V = true;
                    boolean done = false;
                    while (waitingRecordList0.size() > 0) {
                        if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("this is in writing thread: " + recordCount);
                        recordCount++;
                        wRecordBlock = (WaitingRecordBlock) waitingRecordList0.get(0);
                        currentLogFileWriter.createSAWSRecord(wRecordBlock.messageBlock, wRecordBlock.recordType, wRecordBlock.userID, wRecordBlock.encryptionFlag, null);
                        waitingRecordList0.remove(0);
                        done = true;
                    }
                    if (done) {
                        tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
                        tcbContent.write();
                        if (debugLevel > SAWSConstant.NoInfo) {
                            sawsDebugLog.write("TCB written.");
                        }
                        currentTime = System.currentTimeMillis();
                        if (recordCount > signRecordNumber) {
                            startANewLog();
                        }
                    }
                    busyFlag0V = false;
                }
                if ((busyFlag1V == false) && (waitingRecordList1.size() > 0)) {
                    busyFlag1V = true;
                    boolean done = false;
                    while (waitingRecordList1.size() > 0) {
                        if (debugLevel > SAWSConstant.NoInfo) sawsDebugLog.write("this is in writing thread: " + recordCount);
                        recordCount++;
                        wRecordBlock = (WaitingRecordBlock) waitingRecordList1.get(0);
                        currentLogFileWriter.createSAWSRecord(wRecordBlock.messageBlock, wRecordBlock.recordType, wRecordBlock.userID, wRecordBlock.encryptionFlag, null);
                        waitingRecordList1.remove(0);
                        done = true;
                    }
                    if (done) {
                        tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
                        tcbContent.write();
                        if (debugLevel > SAWSConstant.NoInfo) {
                            sawsDebugLog.write("TCB written.");
                        }
                        currentTime = System.currentTimeMillis();
                        if (recordCount > signRecordNumber) {
                            startANewLog();
                        }
                    }
                    busyFlag1V = false;
                }
            }
        }
    }

    /**
   * This method is to close the current log file and then start a new log file. 
   *
   */
    private void startANewLog() {
        currentLogFileWriter.finalizeLogFile();
        currentInspecitonAccHash = currentLogFileWriter.getAccHash();
        currentInspectionSignature = currentLogFileWriter.getSignature();
        previousLogFilenameFromLogRecord = latestLogFilename;
        latestLogFilename = lfc.generateNewLogFileName();
        currentLogFileWriter = new LogFileWriter(logFileRoot, latestLogFilename, tcbKM.getvtEncryptionPublicKey(), tcbKM.getsawsEncryptionPublicKey(), tcbKM.getsawsSigningPrivateKey(), tcbKM.getbaSigningPublicKeyCert(), this.hashAlgorithm, null, UserIDPKMap, this.signingAlg, this.callbackHandler);
        currentLogFileWriter.setDebugLevel(debugLevel);
        int ret = currentLogFileWriter.prepareNewLog();
        if (ret != 0) {
            System.exit(-1);
        }
        currentLogFileWriter.createLastFileRecord(previousLogFilenameFromLogRecord, currentInspecitonAccHash, currentInspectionSignature);
        tcbContent.setTCBContent(latestLogFilename, currentLogFileWriter.getCurrentRecordWriteCount(), currentLogFileWriter.getAccHash());
        tcbContent.write();
        recordCount = 0;
    }

    /**
   * This sub class is for generating heartbeat records. 
   */
    public class HeartbeatRecordWriting extends TimerTask {

        private volatile boolean stop = false;

        public HeartbeatRecordWriting() {
        }

        public void run() {
            if (!stop) {
                long t1 = System.currentTimeMillis();
                if (debugLevel > SAWSConstant.NoInfo) System.out.println("In HeartbeatRecordWriting: busyFlag0V * busyFlag1V " + busyFlag0V + "*" + busyFlag1V);
                if ((busyFlag0V == false) && (busyFlag1V == false) && (heartbeatInterval >= 5) && ((t1 - currentTime) >= heartbeatInterval - 5)) {
                    writingHeartBeating = true;
                    busyFlag0V = true;
                    WaitingRecordBlock wRecordBlock = new WaitingRecordBlock(utility.longToByteArray(t1), SAWSConstant.SysHeartbeatType, SAWSConstant.NoEncryptionFlag, (byte) 0x00);
                    waitingRecordList0.addElement(wRecordBlock);
                    busyFlag0V = false;
                    currentTime = t1;
                    thread.run();
                    writingHeartBeating = false;
                }
            } else {
                this.cancel();
            }
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }

    /**
   * This method sets the heartbeat writer. 
   *
   * @param interval The interval that the system must wait to write a heartbeat.
   */
    private void setHeartbeatWriter(int interval) {
        tt2 = new HeartbeatRecordWriting();
        timer = new java.util.Timer(true);
        timer.schedule(tt2, 1000, interval);
    }

    /**
   * Method to create the callback (SAWSTextOutputCallback) with the message to be 
   * presented to the user and send it to the callback handler.
   * 
   * @param message The message to be presented.
   * @param type The type of the message (SAWSTextOutputCallback.WARNING, 
   * SAWSTextOutputCallback.ERROR, SAWSTextOutputCallback.INFORMATION)
   */
    private void showMessage(String message, int type) {
        this.cbs = new Callback[1];
        this.cbs[0] = new SAWSTextOutputCallback(type, message);
        try {
            this.callbackHandler.handle(this.cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
    }

    /**
   * Method that creates a callback (SAWSChoiceCallback) that asks the user
   * to choose one option, and sends it to the callback handler.
   * 
   * @param prompt The message to be presented to the user.
   * @param options The available options for the user.
   * @param type The type of the callback (Warning, Information, etc.)
   * 
   * @return the selected option.
   */
    private int createConfirmCallback(String prompt, String[] options, int type, String key) {
        this.cbs = new Callback[1];
        this.cbs[0] = new SAWSChoiceCallback(prompt, options, key, 0, type);
        try {
            this.callbackHandler.handle(cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
        return ((SAWSChoiceCallback) this.cbs[0]).getSelectedIndex();
    }

    /**
   * This main method is SAWS command-line working mode. 
   */
    public static void main(String[] args) throws Exception {
        System.out.println("Secure Audit Web-Service v" + version.getVersion());
        String sIn = null;
        try {
            if (args.length > 0) {
                sIn = args[0];
            } else {
                System.out.println("\nSAWS is now working in the keystore creation mode. Please select the following options:");
                System.out.println("\nOption 1: Create an encryption keystore.");
                System.out.println("\nOption 2: Create a signing keystore.");
                System.out.println("\nOption 3: Import the rootCA specified in the SAWS configuration file into the signing keystore. " + "This is required by keytool to be able to later import the PKC issued by this rootCA into the signing keystore.");
                System.out.println("\nOption 4: Output a PKC request file from the signing keystore.");
                System.out.println("\nOption 5: Input the PKC issued by the rootCA into the signing keystore.");
                System.out.println("\nOption 6: List all the entries in the signing keystore.");
                System.out.println("\nOption 7: Export the Signing PKC from the sining keystore.");
                System.out.println("\nOption 8: Test mode: SAWS will create a new log file and check old log files, then close the new log file. " + "This is for testing purposes. ");
                System.out.println("\n\nPlease input your choice (1, 2, 3, 4, 5, 6, 7 or 8) or any other input to stop: ");
                InputStreamReader is = new InputStreamReader(System.in);
                BufferedReader systemIn = new BufferedReader(is);
                sIn = systemIn.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
        if ((sIn.compareTo("1") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.createEncryptionKeystore();
        }
        if ((sIn.compareTo("2") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.createSigningKeystore();
        }
        if ((sIn.compareTo("3") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.importRootCA();
        }
        if ((sIn.compareTo("4") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.outputPKCRequest();
        }
        if ((sIn.compareTo("5") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.importSigningPKC();
        }
        if ((sIn.compareTo("6") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.listSigningKeystore();
        }
        if ((sIn.compareTo("7") == 0)) {
            SAWSServer sw = new SAWSServer(0);
            sw.exportSigningPKC();
        }
        if ((sIn.compareTo("8") == 0)) {
            SAWSServer sw = new SAWSServer(1);
            sw.sawsStart();
            System.out.println("Writing 3 log records...");
            sw.sendLogRecord("This is a test 1.".getBytes());
            sw.sendLogRecord("This is a test 2.".getBytes());
            sw.sendLogRecord("This is a test 3.".getBytes());
            System.out.println("closing log");
            sw.closeLog();
            System.exit(0);
        }
        if (args == null || args.length == 0) {
            System.exit(0);
        }
    }
}
