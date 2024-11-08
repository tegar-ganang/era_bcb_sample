package com.xmultra.processor.split;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.xmultra.XmultraConfig;
import com.xmultra.log.Console;
import com.xmultra.processor.DestinationWriter;
import com.xmultra.log.ErrorLogEntry;
import com.xmultra.log.MessageLogEntry;
import com.xmultra.processor.Processor;
import com.xmultra.util.CallbackRegistry;
import com.xmultra.util.DateUtils;
import com.xmultra.util.FileUtils;
import com.xmultra.util.InitMapHolder;
import com.xmultra.util.InvalidConfigFileFormatException;
import com.xmultra.util.ListHolder;
import com.xmultra.util.StringSplitResult;
import com.xmultra.util.StringSplitter;
import com.xmultra.util.Strings;
import com.xmultra.util.SyncFlag;
import com.xmultra.util.XmlParseUtils;
import com.xmultra.watcher.WakeAble;

/**
 * SplitProcessor provides breaks a larger file up into smaller files.
 *
 * @author      Wayne W. Weber
 * @version     $Revision: #1 $
 * @since       1.2
 */
public class SplitProcessor extends Processor {

    /**
    * Updated automatically by source control management.
    */
    public static final String VERSION = "@version $Revision: #1 $";

    private File destLocation = null;

    private File srcDoneLocFile = null;

    private File srcLocation = null;

    private File fileToSplit = null;

    private FileUtils fileUtils = null;

    private ReadFileResults readFileResults = new ReadFileResults();

    private String delimiterPattern = null;

    private String endPattern = null;

    private boolean outputStartAndEndMarks = true;

    private String fileNamePrefix = "";

    private String fileNameSuffix = "";

    private String fileNameGeneration = null;

    private String fileNameParsingPattern = null;

    private String fileNameSequenceSeparator = null;

    private String lastFileName = "";

    private String splitType = null;

    private boolean copyHeaderLine = false;

    private String startPattern = null;

    private Strings strings = null;

    private StringSplitter stringSplitter = null;

    private boolean usingDelimiter = false;

    private boolean atLeastOneSplit = false;

    private boolean splitByLines = false;

    private char[] readBuffer = null;

    private int fileNameGroupInParsingPattern = 0;

    private int outputFileSizeMaxBytes = 0;

    private int outputFileSizeMaxLines = 0;

    private DestinationWriter destWriter = null;

    /**
     * Initializes the Processor object after it has been created.
     *
     * @param n         This Processor's corresponding element in Xmultra's
     *                  main configuration file. A child of the Processor
     *                  element. Has setup info.
     *
     * @param imh       Holds references to utility and log objects.
     *
     * @param sf        Used to communicate between threads.
     *
     * @param stopFlag  Goes false when the thread running in this
     *                  "run()" method should end.
     *
     * @return True if  init was successful.
     */
    public boolean init(Node n, InitMapHolder imh, SyncFlag sf, SyncFlag stopFlag) {
        if (!super.init(n, imh, sf, stopFlag)) return false;
        Element splitProcessorEl = (Element) n;
        msgEntry = new MessageLogEntry(this, VERSION);
        errEntry = new ErrorLogEntry(this, VERSION);
        strings = new Strings();
        fileUtils = (FileUtils) initMapHolder.getEntry(InitMapHolder.FILE_UTILS);
        xmlParseUtils = (XmlParseUtils) initMapHolder.getEntry(InitMapHolder.XML_PARSE_UTILS);
        stringSplitter = new StringSplitter(strings);
        srcLocation = getLocationDirectory(XmultraConfig.SRC_LOCATION_ELEMENT);
        if (srcLocation == null) return false;
        srcDoneLocFile = getLocationDirectory(XmultraConfig.SRC_DONE_LOCATION_ELEMENT);
        if (srcDoneLocFile == null) return false;
        destLocation = getLocationDirectory(XmultraConfig.DEST_LOCATION_ELEMENT);
        if (destLocation == null) return false;
        fileNameSuffix = xmlParseUtils.getAttributeValueFromGrandChildNode(aProcessorNode, XmultraConfig.LOCATIONS_ELEMENT, XmultraConfig.DEST_LOCATION_ELEMENT, XmultraConfig.FILENAME_SUFFIX_ATTRIBUTE);
        if (fileNameSuffix == null) fileNameSuffix = "";
        fileNamePrefix = xmlParseUtils.getAttributeValueFromGrandChildNode(aProcessorNode, XmultraConfig.LOCATIONS_ELEMENT, XmultraConfig.DEST_LOCATION_ELEMENT, XmultraConfig.FILENAME_PREFIX_ATTRIBUTE);
        if (fileNamePrefix == null) fileNamePrefix = "";
        splitType = splitProcessorEl.getAttribute(XmultraConfig.SPLIT_TYPE);
        if (splitProcessorEl.getAttribute(XmultraConfig.COPY_HEADER_LINE).equalsIgnoreCase("Yes")) {
            this.copyHeaderLine = true;
        }
        delimiterPattern = splitProcessorEl.getAttribute(XmultraConfig.DELIMITER_MARK_PATTERN);
        delimiterPattern = xmlParseUtils.conditionAttributeValue(delimiterPattern, false);
        startPattern = splitProcessorEl.getAttribute(XmultraConfig.START_MARK_PATTERN);
        startPattern = xmlParseUtils.conditionAttributeValue(startPattern, false);
        endPattern = splitProcessorEl.getAttribute(XmultraConfig.END_MARK_PATTERN);
        endPattern = xmlParseUtils.conditionAttributeValue(endPattern, false);
        if (splitProcessorEl.getAttribute(XmultraConfig.OUTPUT_START_AND_END_MARKS).equals("No")) {
            this.outputStartAndEndMarks = false;
        }
        fileNameGeneration = splitProcessorEl.getAttribute(XmultraConfig.FILENAME_GENERATION);
        fileNameParsingPattern = splitProcessorEl.getAttribute(XmultraConfig.FILENAME_PARSING_PATTERN);
        fileNameParsingPattern = xmlParseUtils.conditionAttributeValue(fileNameParsingPattern, false);
        if (fileNameGeneration.equals(XmultraConfig.FILENAME_PARSE_FROM_SOURCE) && (fileNameParsingPattern == null || fileNameParsingPattern.equals(""))) {
            msgEntry.setAppContext("init()");
            msgEntry.setMessageText("If '" + XmultraConfig.FILENAME_GENERATION + "' equals '" + XmultraConfig.FILENAME_PARSE_FROM_SOURCE + ",' " + XmultraConfig.FILENAME_PARSING_PATTERN + " must be provided.");
            msgEntry.setError("No " + XmultraConfig.FILENAME_PARSING_PATTERN + "; defaulting " + XmultraConfig.FILENAME_GENERATION + " to " + XmultraConfig.FILENAME_GENERATE_UNIQUE_ID + ".");
            logger.logWarning(msgEntry);
        }
        String fileNameGroupInParsingPatternStr = splitProcessorEl.getAttribute(XmultraConfig.FILENAME_GROUP_IN_PARSING_PATTERN);
        fileNameGroupInParsingPattern = Integer.parseInt(fileNameGroupInParsingPatternStr);
        fileNameSequenceSeparator = splitProcessorEl.getAttribute(XmultraConfig.FILENAME_SEQUENCE_SEPARATOR);
        String outputFileSizeMaxKBytesStr = splitProcessorEl.getAttribute(XmultraConfig.OUTPUT_FILESIZE_MAX_KBYTES);
        String outputFileSizeMaxLinesStr = splitProcessorEl.getAttribute(XmultraConfig.OUTPUT_FILESIZE_MAX_LINES);
        if (outputFileSizeMaxKBytesStr != null && !outputFileSizeMaxKBytesStr.equals("")) {
            try {
                outputFileSizeMaxBytes = 1024 * Integer.parseInt(outputFileSizeMaxKBytesStr);
            } catch (NumberFormatException e) {
                errEntry.setAppContext("init()");
                errEntry.setAppMessage("'" + outputFileSizeMaxKBytesStr + "' is an invalid value for '" + XmultraConfig.OUTPUT_FILESIZE_MAX_KBYTES + "' attribute in a 'SplitProcessor' element.");
                logger.logError(errEntry);
                return false;
            }
        }
        if (this.splitType.equalsIgnoreCase(XmultraConfig.SPLIT_TYPE_LINE_COUNT) && outputFileSizeMaxLinesStr != null && !outputFileSizeMaxLinesStr.equals("")) {
            try {
                outputFileSizeMaxLines = Integer.parseInt(outputFileSizeMaxLinesStr);
                this.splitByLines = true;
            } catch (NumberFormatException e) {
                errEntry.setAppContext("init()");
                errEntry.setAppMessage("'" + outputFileSizeMaxKBytesStr + "' is an invalid value for '" + XmultraConfig.OUTPUT_FILESIZE_MAX_LINES + "' attribute in a 'SplitProcessor' element.");
                logger.logError(errEntry);
                return false;
            }
        }
        if ((outputFileSizeMaxBytes == 0 && outputFileSizeMaxLines == 0) || (outputFileSizeMaxBytes > 0 && outputFileSizeMaxLines > 0)) {
            errEntry.setAppContext("init()");
            errEntry.setAppMessage("In the 'SplitProcessor' element, the attribute '" + XmultraConfig.OUTPUT_FILESIZE_MAX_KBYTES + "' or the attribute '" + XmultraConfig.OUTPUT_FILESIZE_MAX_LINES + "' must be specified, but not both.");
            logger.logError(errEntry);
            return false;
        }
        if (splitType.equals(XmultraConfig.SPLIT_TYPE_DELIMITER_MARK)) {
            usingDelimiter = true;
            startPattern = "";
            endPattern = delimiterPattern;
        }
        readBuffer = new char[2 * outputFileSizeMaxBytes];
        return true;
    }

    /**
     * Set up the DestinationWriter.
     *
     * The DestinationWriter allows file naming options that are an alternative to the
     * "GenerateUniqueId" option for file name generation when the file names of the
     * output files are not parsed from the input source.
     *
     * To use the DestinationWriter class, configure it as ...
     *   <SplitProcessor ...>
     *       <Locations>
     *         <DestinationLocation .../>
     *       </Locations>
     *     </HttpGetProcessor>
     *
     * @return true if successful; false otherwise.
     */
    private boolean initializeDestWriter() {
        if (this.destWriter == null) {
            this.destWriter = new DestinationWriter();
            try {
                if (!destWriter.init(this.initMapHolder, super.aProcessorNode, new DateUtils())) {
                    throw new InvalidConfigFileFormatException();
                }
            } catch (InvalidConfigFileFormatException e) {
                this.errEntry.setThrowable(e);
                this.errEntry.setAppContext("initializeDestWriter()");
                this.errEntry.setAppMessage("Error reading a DestinationLocation directory");
                this.errEntry.setSubjectSendEmail("Error reading a DestinationLocation directory");
                this.logger.logError(errEntry);
                this.destWriter = null;
                return false;
            }
        }
        return true;
    }

    /**
     * Makes this class runnable. This method is called when the Thread.start()
     * method is invoked. Calls the main loop of this thread.
     */
    public void run() {
        try {
            runIt();
        } catch (Throwable e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("run()");
            errEntry.setDocInfo(fileToSplit.toString());
            errEntry.setAppMessage("Final try/catch caught 'uncaught' exception");
            errEntry.setSubjectSendEmail("Final try/catch block caught 'uncaught' exception");
            logger.logError(errEntry);
        }
    }

    /**
     * Has the main loop of this thread.
     */
    public void runIt() {
        if (!processorStopSyncFlag.getFlag()) super.notifyAndStartWaiting();
        while (!processorStopSyncFlag.getFlag()) {
            splitFiles(listHolder);
            if (Console.getConsoleMode("9")) {
                System.exit(0);
            }
            if (processorStopSyncFlag.getFlag()) break;
            super.notifyAndStartWaiting();
        }
        msgEntry.setAppContext("run()");
        msgEntry.setMessageText("Exiting SplitProcessor");
        logger.logProcess(msgEntry);
        processorSyncFlag.setFlag(false);
        processorStopSyncFlag.setFlag(false);
    }

    /**
     * Splits the passed in list of files.
     *
     * @param listHolder Holds the list of files to be split.
     */
    private void splitFiles(ListHolder listHolder) {
        List list = listHolder.getList();
        int index = listHolder.getIndex();
        for (; index < list.size(); index++) {
            fileToSplit = (File) list.get(index);
            if (fileToSplit == null) {
                continue;
            }
            String logMessage = splitFile(fileToSplit);
            if (logMessage != null) {
                msgEntry.setAppContext("run()");
                msgEntry.setMessageText(logMessage + fileToSplit.toString());
                msgEntry.setSendToSystemLog(false);
                logger.logProcess(msgEntry);
            }
            if (!Console.getConsoleMode("9")) {
                fileUtils.moveFileToDoneLocation(fileToSplit, srcDoneLocFile.toString());
            }
            WakeAble wakeAble = CallbackRegistry.getFromWakeAbleRegistry(destLocation.toString());
            if (wakeAble != null) {
                wakeAble.wakeUp();
            }
            if (processorStopSyncFlag.getFlag()) break;
        }
        listHolder.setIndex(index);
    }

    /**
     * Reads the passed in file and splits it up into smaller files.
     *
     * @param fileToSplit File to split up.
     *
     * @return True if no error.
     */
    private String splitFile(File fileToSplit) {
        BufferedReader bufReader = null;
        this.atLeastOneSplit = false;
        int splitCount = 0;
        try {
            InputStreamReader isReader = new InputStreamReader(new FileInputStream(fileToSplit), FileUtils.UTF_8_ENCODING);
            bufReader = new BufferedReader(isReader, FileUtils.BUFFER_SIZE);
            String tail = "";
            String fileName = "";
            List stringList = new ArrayList();
            List startMarkList = new ArrayList();
            List endMarkList = new ArrayList();
            StringSplitResult result = null;
            String splitOutString = null;
            String startMark = null;
            String endMark = null;
            String headerLine = null;
            int headerLines = 0;
            if (this.copyHeaderLine) {
                ReadFileResults readResults = readLinesFromFile(bufReader, 1);
                headerLine = readResults.output;
                headerLines = 1;
                if (readResults.endOfFile) {
                    return "Could not split up: ";
                }
            }
            while (true) {
                ReadFileResults readResults = null;
                if (this.splitByLines) {
                    readResults = readLinesFromFile(bufReader, outputFileSizeMaxLines - headerLines);
                } else {
                    readResults = readBytesFromFile(bufReader, this.readBuffer);
                }
                String readString = readResults.output;
                if (readString == null) break;
                if (readString.equals("") && tail.equals("")) break;
                if (!readResults.endOfFile || splitCount > 0) {
                    splitCount++;
                }
                if (splitByLines) {
                    fileName = getFileName(fileToSplit, splitCount, readString);
                    writeSplitFile(fileName, readString, headerLine);
                    if (readResults.endOfFile) {
                        break;
                    }
                } else {
                    String stringToSplit = tail + readString;
                    result = stringSplitter.splitString(startPattern, endPattern, this.outputStartAndEndMarks, stringToSplit);
                    stringList = result.getSplitList();
                    startMarkList = result.getStartMarkList();
                    endMarkList = result.getEndMarkList();
                    for (int i = 0; i < stringList.size(); i++) {
                        splitOutString = (String) stringList.get(i);
                        if (this.outputStartAndEndMarks) {
                            fileName = getFileName(fileToSplit, splitCount, splitOutString);
                        } else {
                            startMark = (String) startMarkList.get(i);
                            endMark = (String) endMarkList.get(i);
                            fileName = getFileName(fileToSplit, splitCount, startMark + splitOutString + endMark);
                        }
                        writeSplitFile(fileName, splitOutString, headerLine);
                    }
                    tail = getTail(fileName, result);
                    if ((endPattern.equals("") || this.usingDelimiter) && (stringList.size() == 0) && (readResults.endOfFile == true)) {
                        writeSplitFile(fileName, tail, headerLine);
                        break;
                    }
                    if ((stringList.size() == 0) && readString.equals("")) break;
                }
            }
        } catch (IOException e) {
            errEntry.setThrowable(e);
            errEntry.setAppContext("splitFile()");
            errEntry.setDocInfo(fileToSplit.toString());
            errEntry.setAppMessage("Error reading file.");
            logger.logError(errEntry);
            return null;
        } finally {
            try {
                if (bufReader != null) bufReader.close();
            } catch (IOException e) {
                errEntry.setThrowable(e);
                errEntry.setAppContext("splitFile()");
                errEntry.setDocInfo(fileToSplit.toString());
                errEntry.setAppMessage("Error reading file.");
                logger.logError(errEntry);
                return null;
            }
        }
        if (atLeastOneSplit) {
            return "Split up: ";
        }
        return "Could not split up: ";
    }

    /**
     * Finds the tail of the passed in StringSplitResult. If that tail
     * is too large, logs it and looks for the text after the next mark
     * in the tail. This is to prevent too large of a file from being
     * parsed out (and potentially sucking up all available memory).
     *
     * @param fileName The previous file. Used to log error message.
     * @param result   The result which contains the tail.
     *
     * @return The tail.
     */
    private String getTail(String fileName, StringSplitResult result) {
        String tail = result.getTail();
        if (tail.length() < outputFileSizeMaxBytes) {
            return tail;
        }
        if (!fileName.equals(lastFileName)) {
            msgEntry.setAppContext("splitFile()");
            msgEntry.setMessageText("The '" + XmultraConfig.OUTPUT_FILESIZE_MAX_KBYTES + "' attribute in a 'SplitProcessor' element " + "specifies the split out file must be less than " + (outputFileSizeMaxBytes / 1024) + " KBytes long. ");
            msgEntry.setError("Split out file after following split out file too large: " + fileName);
            logger.logWarning(msgEntry);
        }
        lastFileName = fileName;
        String markPattern = null;
        if (!this.endPattern.equals("")) markPattern = this.endPattern; else markPattern = this.startPattern;
        if (strings.matches(markPattern, tail)) {
            tail = strings.getGroup(0) + strings.getPostMatch();
        } else tail = "";
        return tail;
    }

    /**
     * Writes the passed in string to a file. Parses out or generates
     * a file name. Logs the file written.
     *
     * @param fileName name of the output file
     * @param splitOutString the String to be written to a file.
     */
    private void writeSplitFile(String fileName, String splitOutString) {
        writeSplitFile(fileName, splitOutString, null);
    }

    /**
     * Writes the passed in string to a file. Parses out or generates
     * a filename. Logs the file written.
     *
     * @param fileName name of the output file
     * @param splitOutString the String to be written to a file.
     * @param headerLine line to be written at the top of each split-out file
     */
    private void writeSplitFile(String fileName, String splitOutString, String headerLine) {
        processorSyncFlag.restartWaitUntilFalse();
        if (splitOutString == null || splitOutString.equals("")) {
            return;
        }
        this.atLeastOneSplit = true;
        boolean destinationWriterReady = initializeDestWriter();
        String fileContents;
        if (headerLine != null && !headerLine.equals("")) {
            fileContents = headerLine + splitOutString;
        } else {
            fileContents = splitOutString;
        }
        if (destinationWriterReady) {
            if (destWriter.writeStringToLocations(fileName, fileContents)) {
                msgEntry.setDocInfo(fileName);
                msgEntry.setMessageText("SplitProcessor wrote '" + fileName + "' to destinations.");
                logger.logProcess(msgEntry);
            }
        }
        msgEntry.setMessageText("Split out file: " + fileName);
        logger.logProcess(msgEntry);
        if (splitOutString.length() > outputFileSizeMaxBytes && !this.splitByLines) {
            msgEntry.setAppContext("splitFile()");
            msgEntry.setMessageText("The '" + XmultraConfig.OUTPUT_FILESIZE_MAX_KBYTES + "' attribute in a 'SplitProcessor' element " + "specifies the split out file must be less than " + (outputFileSizeMaxBytes / 1024) + " KBytes long. ");
            msgEntry.setError("Split out file is too large: " + fileName);
            logger.logWarning(msgEntry);
        }
    }

    /**
     * Gets a filename, either by finding it in the passed in String or
     * by generating a unique one.
     *
     * @param fileToSplit Input file
     * @param splitCount  Number of split files so far from one input file
     * @param data        Contents of file to be written, which may contain
     *                    the output file name
     *
     * @return The filename.
     */
    private String getFileName(File fileToSplit, int splitCount, String data) {
        String fileName = "";
        if (fileNameGeneration.equals(XmultraConfig.FILENAME_PARSE_FROM_SOURCE)) {
            if (!this.fileNameParsingPattern.equals("") && strings.matches(this.fileNameParsingPattern, data)) {
                fileName = strings.getGroup(this.fileNameGroupInParsingPattern);
                if (fileName != null && !fileName.equals("")) {
                    fileName = strings.substitute("[\\/]", "_", fileName);
                }
            }
        } else if (fileNameGeneration.equals(XmultraConfig.DST_FILE_NAME_TYPE_SOURCE)) {
            fileName = fileToSplit.getName();
            if (splitCount > 0) {
                fileName += this.fileNameSequenceSeparator + Integer.toString(splitCount);
            }
        }
        if (fileName.equals("")) {
            fileName = FileUtils.getUniqueFileName(fileNamePrefix, fileNameSuffix);
        }
        return fileName;
    }

    /**
     * Reads until at least the specified number of bytes are
     * read into the passed in byte array buffer.
     *
     * @param bufReader      fileReader
     *
     * @param readBuf        The byte array buffer read into.
     *
     * @return The ReadFileResults object returned contains the String read
     *         from the file. It also has a boolean indicating if the end-of-
     *         file has been reached.
     *
     */
    private ReadFileResults readBytesFromFile(BufferedReader bufReader, char[] readBuf) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        int bytesRead = 0;
        int bufferSize = FileUtils.BUFFER_SIZE;
        int bytesToRead = readBuf.length / 2;
        this.readFileResults.endOfFile = false;
        if (bufferSize > bytesToRead) {
            bufferSize = bytesToRead;
        }
        while (stringBuffer.length() < bytesToRead) {
            bytesRead = bufReader.read(readBuf, 0, bufferSize);
            if (bytesRead == -1) {
                this.readFileResults.endOfFile = true;
                break;
            }
            stringBuffer.append(readBuf, 0, bytesRead);
        }
        readFileResults.output = stringBuffer.toString();
        return readFileResults;
    }

    /**
     * Reads until at least the specified number of lines are
     * read into the passed in byte array buffer.
     *
     * @param input          FileReader, possibly with file pointer positioned
     *                       in the middle of a file to resume reading where
     *                       the last call to this method left off.
     *
     * @param maxLines       Maximum number of lines to read
     *
     * @return The ReadFileResults object returned contains the String read
     *         from the file. It also has a boolean indicating if the end-of-
     *         file has been reached.
     *
     */
    private ReadFileResults readLinesFromFile(BufferedReader input, int maxLines) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        this.readFileResults.endOfFile = false;
        String line = null;
        int linesRead = 0;
        while (linesRead < maxLines) {
            line = input.readLine();
            linesRead++;
            if (line == null) {
                this.readFileResults.endOfFile = true;
                break;
            }
            stringBuffer.append(line);
            stringBuffer.append("\n");
        }
        readFileResults.output = stringBuffer.toString();
        return readFileResults;
    }

    /**
     * Contains the output of a partially read-in file.
     *
     * @author Wayne W. Weber
     * @since 1.3
     */
    private class ReadFileResults {

        private String output = null;

        private boolean endOfFile = false;
    }
}
