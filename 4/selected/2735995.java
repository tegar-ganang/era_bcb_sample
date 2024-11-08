package com.kni.etl.ketl.reader;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.CodingErrorAction;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.kni.etl.EngineConstants;
import com.kni.etl.FieldLevelFastInputChannel;
import com.kni.etl.SourceFieldDefinition;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.ETLOutPort;
import com.kni.etl.ketl.ETLStep;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.qa.QAEventGenerator;
import com.kni.etl.ketl.qa.QAForFileReader;
import com.kni.etl.ketl.smp.ETLThreadManager;
import com.kni.etl.stringtools.FastSimpleDateFormat;
import com.kni.etl.util.ManagedFastInputChannel;
import com.kni.etl.util.XMLHelper;
import com.kni.util.Arrays;
import com.kni.util.FileTools;

/**
 * The Class NIOFileReader.
 */
public class NIOFileReader extends ETLReader implements QAForFileReader {

    @Override
    protected String getVersion() {
        return "$LastChangedRevision: 491 $";
    }

    /**
	 * The Class FileETLOutPort.
	 */
    public class FileETLOutPort extends ETLOutPort {

        /** The sf. */
        SourceFieldDefinition sf;

        /**
		 * Gets the source field definition.
		 * 
		 * @return the source field definition
		 */
        public SourceFieldDefinition getSourceFieldDefinition() {
            if (this.sf == null) this.sf = this.getSourceFieldDefinitions(this);
            return this.sf;
        }

        /** The type map. */
        Class[] typeMap = { String.class, Double.class, Integer.class, Float.class, Long.class, Short.class, Date.class, Boolean.class, Byte.class, Byte[].class, Character.class, Character[].class };

        /** The type methods. */
        String[] typeMethods = { "FieldLevelFastInputChannel.toString(${chars}, ${length})", "FieldLevelFastInputChannel.toDouble(${chars}, ${length})", "FieldLevelFastInputChannel.toInteger(${chars}, ${length})", "FieldLevelFastInputChannel.toFloat(${chars}, ${length})", "FieldLevelFastInputChannel.toLong(${chars}, ${length})", "FieldLevelFastInputChannel.toShort(${chars}, ${length})", "FieldLevelFastInputChannel.toDate(${chars}, ${length}, ${dateformatter},${parseposition})", "FieldLevelFastInputChannel.toBoolean(${chars}, ${length})", "FieldLevelFastInputChannel.toByte(${chars}, ${length})", "FieldLevelFastInputChannel.toByteArray(${chars}, ${length})", "FieldLevelFastInputChannel.toChar(${chars}, ${length})", "FieldLevelFastInputChannel.toCharArray(${chars}, ${length})" };

        /**
		 * Instantiates a new file ETL out port.
		 * 
		 * @param esOwningStep
		 *            the es owning step
		 * @param esSrcStep
		 *            the es src step
		 */
        public FileETLOutPort(ETLStep esOwningStep, ETLStep esSrcStep) {
            super(esOwningStep, esSrcStep);
        }

        /**
		 * Gets the source field definitions.
		 * 
		 * @param port
		 *            the port
		 * 
		 * @return the source field definitions
		 */
        private final SourceFieldDefinition getSourceFieldDefinitions(FileETLOutPort port) {
            NamedNodeMap nmAttrs;
            String mstrDefaultFieldDelimeter = null;
            Element nlOut = port.getXMLConfig();
            SourceFieldDefinition srcFieldDefinition = new SourceFieldDefinition();
            nmAttrs = nlOut.getAttributes();
            nmAttrs.getNamedItem(NIOFileReader.NAME);
            if (mstrDefaultFieldDelimeter == null) {
                mstrDefaultFieldDelimeter = XMLHelper.getAttributeAsString(nlOut.getParentNode().getAttributes(), NIOFileReader.DELIMITER, null);
            }
            if (mstrDefaultFieldDelimeter == null) {
                ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "FileReader: No default delimiter has been specified, system default field delimiter '" + NIOFileReader.DEFAULT_FIELD_DELIMITER + "' will be used for fields without delimiters specified.");
                mstrDefaultFieldDelimeter = NIOFileReader.DEFAULT_FIELD_DELIMITER;
            }
            srcFieldDefinition.MaxLength = XMLHelper.getAttributeAsInt(nmAttrs, NIOFileReader.MAXIMUM_LENGTH, srcFieldDefinition.MaxLength);
            srcFieldDefinition.FixedLength = XMLHelper.getAttributeAsInt(nmAttrs, NIOFileReader.FIXED_LENGTH, srcFieldDefinition.FixedLength);
            srcFieldDefinition.ReadOrder = EngineConstants.resolveValueFromConstant(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.READ_ORDER, Integer.toString(srcFieldDefinition.ReadOrder)), srcFieldDefinition.ReadOrder);
            srcFieldDefinition.ReadOrderSequence = XMLHelper.getAttributeAsInt(nmAttrs, NIOFileReader.READ_ORDER_SEQUENCE, srcFieldDefinition.ReadOrderSequence);
            srcFieldDefinition.AutoTruncate = XMLHelper.getAttributeAsBoolean(nmAttrs, NIOFileReader.AUTOTRUNCATE, srcFieldDefinition.AutoTruncate);
            srcFieldDefinition.setDelimiter(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.DELIMITER, mstrDefaultFieldDelimeter));
            srcFieldDefinition.setEscapeCharacter(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.ESCAPE_CHAR, null));
            srcFieldDefinition.setEscapeDoubleQuotes(XMLHelper.getAttributeAsBoolean(nmAttrs, NIOFileReader.ESCAPE_DOUBLEQUOTES, false));
            srcFieldDefinition.setNullIf(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.NULLIF, null));
            srcFieldDefinition.setQuoteStart(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.QUOTESTART, srcFieldDefinition.getQuoteStart()));
            srcFieldDefinition.setQuoteEnd(XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.QUOTEEND, srcFieldDefinition.getQuoteEnd()));
            srcFieldDefinition.FormatString = XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.FORMAT_STRING, srcFieldDefinition.FormatString);
            srcFieldDefinition.DefaultValue = XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.DEFAULT_VALUE, srcFieldDefinition.DefaultValue);
            srcFieldDefinition.keepDelimiter = XMLHelper.getAttributeAsBoolean(nmAttrs, "KEEPDELIMITER", false);
            srcFieldDefinition.PartitionField = XMLHelper.getAttributeAsBoolean(nmAttrs, NIOFileReader.PARTITION_KEY, false);
            srcFieldDefinition.setInternal(XMLHelper.getAttributeAsString(nmAttrs, "INTERNAL", null));
            srcFieldDefinition.ObjectType = EngineConstants.resolveObjectNameToID(XMLHelper.getAttributeAsString(nmAttrs, "OBJECTTYPE", null));
            srcFieldDefinition.DataType = port.getPortClass();
            String trimStr = XMLHelper.getAttributeAsString(nmAttrs, NIOFileReader.TRIM, "FALSE");
            if ((trimStr != null) && trimStr.equalsIgnoreCase("TRUE")) {
                srcFieldDefinition.TrimValue = true;
            }
            return srcFieldDefinition;
        }

        @Override
        public String generateCode(int portReferenceIndex) throws KETLThreadException {
            this.getSourceFieldDefinition();
            String sfRef = this.mstrName + "FieldDef";
            NIOFileReader.this.getCodeField("SourceFieldDefinition", "((com.kni.etl.ketl.reader.NIOFileReader.FileETLOutPort)this.getOwner().getOutPort(" + portReferenceIndex + ")).getSourceFieldDefinition()", false, true, sfRef);
            if (this.sf.hasInternal()) {
                switch(this.sf.internal) {
                    case FILENAME:
                        return this.getCodeGenerationReferenceObject() + "[" + this.mesStep.getUsedPortIndex(this) + "] = ((com.kni.etl.ketl.reader.NIOFileReader)this.getOwner()).getFileName();";
                    case FILEPATH:
                        return this.getCodeGenerationReferenceObject() + "[" + this.mesStep.getUsedPortIndex(this) + "] = ((com.kni.etl.ketl.reader.NIOFileReader)this.getOwner()).getFilePath();";
                    default:
                        throw new KETLThreadException("No method has been defined to handle internal " + this.sf.internal + ", contact support", this);
                }
            }
            StringBuilder code = new StringBuilder("\n// handle negative codes and keep trying to resolve\ndo { res = ");
            if (this.sf.FixedLength > 0) code.append(" this.mReader.readFixedLengthField( " + sfRef + ".FixedLength," + sfRef + ".getQuoteStartAsChars(), " + sfRef + ".getQuoteEndAsChars(), buf);"); else code.append("this.mReader.readDelimitedField(" + sfRef + ".getDelimiterAsChars(), " + sfRef + ".getQuoteStartAsChars(), " + sfRef + ".getQuoteEndAsChars(), " + sfRef + ".mEscapeDoubleQuotes, " + sfRef + ".escapeChar, " + sfRef + ".MaxLength, " + sfRef + ".AverageLength, buf, " + sfRef + ".AutoTruncate," + sfRef + ".keepDelimiter);");
            code.append(" if(res <0) {char[] tmp = (char[]) this.getOwner().handlePortEventCode(res," + Arrays.searchArray(NIOFileReader.this.mOutPorts, this) + "); if(tmp != null) buf=tmp;}} while(res < 0);");
            if (this.isUsed()) {
                if (this.sf.MaxLength > -1) code.append("res = res > " + this.sf.MaxLength + "?" + this.sf.MaxLength + ":res;");
                if (this.sf.NullIf != null) code.append("res =  com.kni.etl.ketl.reader.NIOFileReader.charArrayEquals(buf, res, " + NIOFileReader.this.getCodeField("char[]", "\"" + new String(this.sf.NullIfCharArray) + "\".toCharArray()", true, true, null) + "," + this.sf.NullIfCharArray.length + ")?0:res;");
                if (this.sf.DefaultValue != null) {
                    if (this.sf.DataType == String.class) {
                        code.append("if(res == 0) " + this.getCodeGenerationReferenceObject() + "[" + this.mesStep.getUsedPortIndex(this) + "] = " + NIOFileReader.this.getCodeField("String", "\"" + this.sf.DefaultValue + "\"", true, true, null) + ";");
                    } else code.append("if(res == 0) " + NIOFileReader.this.getCodeField("String", "\"" + this.sf.DefaultValue + "\"", true, true, null) + ".getChars(0," + this.sf.DefaultValue.length() + ",buf,0);");
                }
                code.append("try{" + (this.sf.position != null ? sfRef + ".position.setIndex(0);\n" : "") + this.getCodeGenerationReferenceObject() + "[" + this.mesStep.getUsedPortIndex(this) + "] = (res == 0?null:");
                int res = Arrays.searchArray(this.typeMap, this.sf.DataType);
                String method;
                if (res < 0) method = NIOFileReader.this.getMethodMapFromSystemXML("CREATEOBJECT", NIOFileReader.class, this.sf.DataType, "The datatype " + this.sf.DataType + " is not directly supported, please add mapping of the form [Class name].[Static Method Supported parameter Char[] ${chars} and Char Length ${length}, returning required datatype]"); else method = this.typeMethods[res];
                method = EngineConstants.replaceParameter(method, "chars", "buf");
                method = EngineConstants.replaceParameter(method, "length", "res");
                method = EngineConstants.replaceParameter(method, "parseposition", sfRef + ".position");
                method = EngineConstants.replaceParameter(method, "dateformatter", sfRef + ".DateFormatter");
                code.append(method + ");");
                code.append("} catch (Exception e) { this.getOwner().handlePortException(e," + portReferenceIndex + "); }");
            }
            return code.toString();
        }

        @Override
        public boolean containsCode() throws KETLThreadException {
            return true;
        }
    }

    /** The ALLO w_ DUPLICATE s_ ATTRIBUTE. */
    public static String ALLOW_DUPLICATES_ATTRIBUTE = "ALLOWDUPLICATES";

    /** The Constant AUTOTRUNCATE. */
    public static final String AUTOTRUNCATE = "AUTOTRUNCATE";

    /** The CHARACTERSE t_ ATTRIB. */
    public static String CHARACTERSET_ATTRIB = "CHARACTERSET";

    /** The CODINGERRORACTIO n_ ATTRIB. */
    public static String CODINGERRORACTION_ATTRIB = "CODINGERRORACTION";

    /** The Constant DATATYPE. */
    public static final String DATATYPE = "DATATYPE";

    /** The DEFAUL t_ FIEL d_ DELIMITER. */
    public static String DEFAULT_FIELD_DELIMITER = ",";

    /** The DEFAUL t_ ALLO w_ INVALI d_ LAS t_ RECORD. */
    public static boolean DEFAULT_ALLOW_INVALID_LAST_RECORD = false;

    /** The DEFAUL t_ RECOR d_ DELIMITER. */
    public static String DEFAULT_RECORD_DELIMITER = "\n";

    /** The DEFAUL t_ VALUE. */
    public static String DEFAULT_VALUE = "DEFAULTVALUE";

    /** The DELETESOURC e_ ATTRIB. */
    public static String DELETESOURCE_ATTRIB = "DELETESOURCE";

    /** The DELIMITER. */
    public static String DELIMITER = "DELIMITER";

    /** The Constant ESCAPE_CHAR. */
    public static final String ESCAPE_CHAR = "ESCAPECHARACTER";

    /** The Constant ESCAPE_DOUBLEQUOTES. */
    public static final String ESCAPE_DOUBLEQUOTES = "ESCAPEDOUBLEQUOTES";

    /** The Constant FIXED_LENGTH. */
    public static final String FIXED_LENGTH = "FIXEDLENGTH";

    /** The FORMA t_ STRING. */
    public static String FORMAT_STRING = "FORMATSTRING";

    /** The IGNOR e_ ACTION. */
    public static String IGNORE_ACTION = "IGNORE";

    /** The ALLO w_ INVALI d_ LAS t_ RECORD. */
    public static String ALLOW_INVALID_LAST_RECORD = "ALLOWINVALIDLASTRECORD";

    /** The MA x_ RECOR d_ DELIMITE r_ LENGTH. */
    public static int MAX_RECORD_DELIMITER_LENGTH = 1;

    /** The MAXIMU m_ LENGTH. */
    public static String MAXIMUM_LENGTH = "MAXIMUMLENGTH";

    /** The MOVESOURC e_ ATTRIB. */
    public static String MOVESOURCE_ATTRIB = "MOVESOURCE";

    /** The NAME. */
    public static String NAME = "NAME";

    /** The Constant NULLIF. */
    public static final String NULLIF = "NULLIF";

    /** The Constant OK_RECORD. */
    protected static final int OK_RECORD = 0;

    /** The Constant PARTIAL_RECORD. */
    private static final int PARTIAL_RECORD = -1;

    /** The Constant PARTITION_KEY. */
    public static final String PARTITION_KEY = "PARTITIONKEY";

    /** The PATH. */
    public static String PATH = "PATH";

    /** The QUOTEEND. */
    public static String QUOTEEND = "QUOTEEND";

    /** The QUOTESTART. */
    public static String QUOTESTART = "QUOTESTART";

    /** The REA d_ ORDER. */
    public static String READ_ORDER = "READORDER";

    /** The REA d_ ORDE r_ SEQUENCE. */
    public static String READ_ORDER_SEQUENCE = "READORDERSEQUENCE";

    /** The RECOR d_ DELIMITER. */
    public static String RECORD_DELIMITER = "RECORD_DELIMITER";

    /** The REPLAC e_ ACTION. */
    public static String REPLACE_ACTION = "REPLACE";

    /** The REPOR t_ ACTION. */
    public static String REPORT_ACTION = "REPORT";

    /** The SAMPL e_ EVER y_ ATTRIBUTE. */
    public static String SAMPLE_EVERY_ATTRIBUTE = "SAMPLEEVERY";

    /** The SEARCHPATH. */
    public static String SEARCHPATH = "SEARCHPATH";

    /** The SKI p_ LINES. */
    public static String SKIP_LINES = "SKIPLINES";

    /** The SOR t_ BUFFE r_ PE r_ FILE. */
    public static String SORT_BUFFER_PER_FILE = "SORTBUFFERPERFILE";

    /** The TRIM. */
    public static String TRIM = "TRIM";

    /** The ZIPPED. */
    public static String ZIPPED = "ZIPPED";

    /**
	 * Char array equals.
	 * 
	 * @param a
	 *            the a
	 * @param len
	 *            the len
	 * @param a2
	 *            the a2
	 * @param len2
	 *            the len2
	 * 
	 * @return true, if successful
	 */
    public static final boolean charArrayEquals(char[] a, int len, char[] a2, int len2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;
        if (len != len2) return false;
        for (int i = 0; i < len; i++) if (a[i] != a2[i]) return false;
        return true;
    }

    /**
	 * Dedup file list.
	 * 
	 * @param pSource
	 *            the source
	 * 
	 * @return the array list
	 */
    public static ArrayList dedupFileList(ArrayList pSource) {
        HashSet nl = new HashSet();
        for (Object o : pSource) {
            String file = (String) o;
            if (nl.add(file) == false) ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "Duplicate file found in search will be ignored, use " + NIOFileReader.ALLOW_DUPLICATES_ATTRIBUTE + "=\"TRUE\" attribute to allow for duplicate files. File: " + file);
        }
        return new ArrayList(nl);
    }

    /** The buf. */
    private char[] buf;

    /** The bytes read. */
    protected long bytesRead = 0;

    /** The ma files. */
    protected ArrayList maFiles = new ArrayList();

    /** The allow duplicates. */
    protected boolean mAllowDuplicates = false;

    /** The mb allow invalid last record. */
    private boolean mbAllowInvalidLastRecord;

    /** The mc default record delimter. */
    private char mcDefaultRecordDelimter;

    /** The coding error action. */
    private String mCharacterSet, mCodingErrorAction;

    /** The current file channel. */
    private ManagedFastInputChannel mCurrentFileChannel = null;

    /** The delete source. */
    private boolean mDeleteSource = false;

    /** The IO buffer size. */
    private int mIOBufferSize;

    /** The mi skip lines. */
    private int miSkipLines;

    /** The max line length. */
    private int mMaxLineLength;

    /** The move source. */
    private String mMoveSource = null;

    /** The mstr default field delimeter. */
    private String mstrDefaultFieldDelimeter;

    /** The mstr default record delimter. */
    private String mstrDefaultRecordDelimter;

    /** The mv ready files. */
    protected Vector<ManagedFastInputChannel> mvReadyFiles = new Vector<ManagedFastInputChannel>();

    /** The open channels. */
    protected int openChannels = 0;

    /**
	 * Instantiates a new NIO file reader.
	 * 
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitionID
	 *            the partition ID
	 * @param pPartition
	 *            the partition
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public NIOFileReader(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    /**
	 * Close.
	 * 
	 * @param file
	 *            the file
	 * @param pCause
	 *            the cause
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    protected final void close(ManagedFastInputChannel file, int pCause) throws IOException {
        switch(pCause) {
            case PARTIAL_RECORD:
                ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Partial record at end of file");
                break;
        }
        file.close();
        this.openChannels--;
    }

    /**
	 * Delete files.
	 */
    private void deleteFiles() {
        for (Object o : this.maFiles) {
            File fn = new File((String) o);
            if (fn.exists()) {
                if (fn.delete()) {
                    ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Deleted file: " + fn.getAbsolutePath());
                } else ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Failed to delete file: " + fn.getAbsolutePath());
            }
        }
    }

    @Override
    protected String generateCoreImports() {
        return super.generateCoreImports() + "import com.kni.etl.util.ManagedFastInputChannel;\n" + "import com.kni.etl.FieldLevelFastInputChannel;\n" + "import com.kni.etl.SourceFieldDefinition;\n";
    }

    /**
	 * Gets the open channels.
	 * 
	 * @return the open channels
	 */
    public int getOpenChannels() {
        return this.openChannels;
    }

    /** The oc nm. */
    private String ocNm;

    /** The buffer length. */
    private int mBufferLength;

    /** The zipped. */
    private boolean mZipped;

    /** The CURREN t_ FIL e_ CHANNEL. */
    private static String CURRENT_FILE_CHANNEL = "((com.kni.etl.ketl.reader.NIOFileReader)this.getOwner()).getCurrentFileChannel().mReader";

    @Override
    protected String generatePortMappingCode() throws KETLThreadException {
        StringBuilder sb = new StringBuilder();
        this.ocNm = this.getCodeField("int", "((com.kni.etl.ketl.reader.NIOFileReader)this.getOwner()).getOpenChannels()", false, false, "openChannel");
        this.getCodeField("FieldLevelFastInputChannel", this.ocNm + " > 0?" + NIOFileReader.CURRENT_FILE_CHANNEL + ":null", false, true, "mReader");
        this.getCodeField("char[]", "((com.kni.etl.ketl.reader.NIOFileReader)this.getOwner()).getBuffer()", false, true, "buf");
        this.getCodeField("boolean", "false", false, true, "partialRecord");
        sb.append(this.getRecordExecuteMethodHeader() + "\n");
        sb.append("if (this." + this.ocNm + " == 0) return COMPLETE;");
        if (this.mOutPorts != null) for (int i = 0; i < this.mOutPorts.length; i++) {
            if (i == 0) sb.append("partialRecord = false;");
            sb.append(this.mOutPorts[i].generateCode(i) + "\n");
            if (i == 0) sb.append("partialRecord = true;");
        }
        sb.append(this.getRecordExecuteMethodFooter() + "\n");
        return sb.toString();
    }

    @Override
    protected String getRecordExecuteMethodFooter() {
        return " partialRecord = false;}catch(java.io.EOFException e) {if(partialRecord){ throw new KETLReadException(\"Partial record at end of file\");}try {Object res = this.getOwner().handleException(e); if(res == null){return COMPLETE;}if(res != null && res instanceof FieldLevelFastInputChannel) { this.mReader = (FieldLevelFastInputChannel) res;return SKIP_RECORD;} } catch(Exception e1){throw new KETLReadException(e1);}" + super.getRecordExecuteMethodFooter();
    }

    public String getCharacterSet() {
        return this.mCharacterSet;
    }

    /**
	 * Gets the current file channel.
	 * 
	 * @return the current file channel
	 */
    public ManagedFastInputChannel getCurrentFileChannel() {
        return this.mCurrentFileChannel;
    }

    public String getDefaultFieldDelimeter() {
        return this.mstrDefaultFieldDelimeter;
    }

    public char getDefaultRecordDelimter() {
        return this.mcDefaultRecordDelimter;
    }

    /**
	 * Gets the file channels.
	 * 
	 * @param astrPaths
	 *            the astr paths
	 * 
	 * @return the file channels
	 * 
	 * @throws Exception
	 *             the exception
	 */
    int getFileChannels(FileToRead[] astrPaths) throws Exception {
        int iNumPaths = 0;
        if (astrPaths == null) {
            return 0;
        }
        if (this.mAllowDuplicates == false) {
            this.maFiles = NIOFileReader.dedupFileList(this.maFiles);
        }
        for (FileToRead element : astrPaths) {
            FileInputStream fi;
            try {
                File f = new File(element.filePath);
                this.bytesRead += f.length();
                fi = new FileInputStream(f);
                this.openChannels++;
                ManagedFastInputChannel rf = new ManagedFastInputChannel();
                rf.mfChannel = fi.getChannel();
                rf.mPath = element.filePath;
                rf.file = f;
                this.mvReadyFiles.add(rf);
                this.maFiles.add(element);
                iNumPaths++;
            } catch (Exception e) {
                while (this.mvReadyFiles.size() > 0) {
                    ManagedFastInputChannel fs = this.mvReadyFiles.remove(0);
                    this.close(fs, NIOFileReader.OK_RECORD);
                }
                throw new Exception("Failed to open file: " + e.toString());
            }
        }
        return iNumPaths;
    }

    public String getFilePath() {
        return this.mCurrentFileChannel.file.getAbsolutePath();
    }

    public String getFileName() {
        return this.mCurrentFileChannel.file.getName();
    }

    /**
	 * Gets the files.
	 * 
	 * @return the files
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private boolean getFiles() throws Exception {
        ArrayList files = new ArrayList();
        for (int i = 0; i < this.maParameters.size(); i++) {
            String[] fileNames = FileTools.getFilenames(this.getParameterValue(i, NIOFileReader.SEARCHPATH));
            if (fileNames != null) {
                for (String element : fileNames) {
                    files.add(new FileToRead(element, i));
                }
            }
        }
        if (files.size() == 0) return false;
        ArrayList partitionFileList = new ArrayList();
        for (int i = 0; i < files.size(); i++) {
            if (i % this.partitions == this.partitionID) partitionFileList.add(files.get(i));
        }
        FileToRead[] finalFileList = new FileToRead[partitionFileList.size()];
        partitionFileList.toArray(finalFileList);
        if (finalFileList.length > 0) {
            if (this.getFileChannels(finalFileList) <= 0) {
                return false;
            }
        }
        while (this.mCurrentFileChannel == null && this.mvReadyFiles.size() > 0) this.mCurrentFileChannel = this.getReader(this.mvReadyFiles.remove(0));
        return true;
    }

    /** The complete file list. */
    private ArrayList completeFileList = null;

    public ArrayList getOpenFiles() {
        if (this.completeFileList == null) {
            ArrayList files = new ArrayList();
            for (int i = 0; i < this.maParameters.size(); i++) {
                String[] fileNames = FileTools.getFilenames(this.getParameterValue(i, NIOFileReader.SEARCHPATH));
                if (fileNames != null) {
                    for (String element : fileNames) {
                        files.add(element);
                    }
                }
            }
            this.completeFileList = files;
        }
        return this.completeFileList;
    }

    @Override
    public String getQAClass(String strQAType) {
        if (strQAType.equalsIgnoreCase(QAEventGenerator.SIZE_TAG)) {
            return QAForFileReader.QA_SIZE_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.STRUCTURE_TAG)) {
            return QAForFileReader.QA_STRUCTURE_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.VALUE_TAG)) {
            return QAForFileReader.QA_VALUE_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.AMOUNT_TAG)) {
            return QAForFileReader.QA_AMOUNT_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.AGE_TAG)) {
            return QAForFileReader.QA_AGE_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.ITEMCHECK_TAG)) {
            return QAForFileReader.QA_ITEM_CHECK_CLASSNAME;
        }
        if (strQAType.equalsIgnoreCase(QAEventGenerator.RECORDCHECK_TAG)) {
            return QAForFileReader.QA_RECORD_CHECK_CLASSNAME;
        }
        return super.getQAClass(strQAType);
    }

    /**
	 * Gets the reader.
	 * 
	 * @param file
	 *            the file
	 * 
	 * @return the reader
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private ManagedFastInputChannel getReader(ManagedFastInputChannel file) throws Exception {
        try {
            CodingErrorAction action = CodingErrorAction.REPORT;
            if (this.mCodingErrorAction.equalsIgnoreCase(NIOFileReader.IGNORE_ACTION)) action = CodingErrorAction.IGNORE; else if (this.mCodingErrorAction.equalsIgnoreCase(NIOFileReader.IGNORE_ACTION)) action = CodingErrorAction.IGNORE;
            file.mReader = new FieldLevelFastInputChannel(file.mfChannel, "r", this.mIOBufferSize, this.mCharacterSet, this.mZipped, action);
            if (this.mbAllowInvalidLastRecord) {
                file.mReader.allowForNoDelimeterAtEOF(true);
            } else file.mReader.allowForNoDelimeterAtEOF(false);
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Reading file " + file.mPath);
            try {
                for (int x = 0; x < this.miSkipLines; x++) {
                    for (ETLOutPort element : this.mOutPorts) {
                        SourceFieldDefinition sf = ((FileETLOutPort) element).sf;
                        int res;
                        do {
                            if (sf.FixedLength > 0) {
                                res = file.mReader.readFixedLengthField(sf.FixedLength, sf.getQuoteStartAsChars(), sf.getQuoteEndAsChars(), this.buf);
                            } else {
                                res = file.mReader.readDelimitedField(sf.getDelimiterAsChars(), sf.getQuoteStartAsChars(), sf.getQuoteEndAsChars(), sf.mEscapeDoubleQuotes, sf.escapeChar, sf.MaxLength, sf.AverageLength, this.buf, sf.AutoTruncate);
                            }
                            if (res < 0) {
                                char[] tmp = (char[]) this.handlePortEventCode(res, 4);
                                if (tmp != null) this.buf = tmp;
                            }
                        } while (res < 0);
                    }
                }
            } catch (EOFException e) {
                ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, "Attempted to skip " + this.miSkipLines + " records but end of file reached");
                this.close(file, NIOFileReader.OK_RECORD);
                file = null;
            }
        } catch (Exception e) {
            this.close(file, NIOFileReader.OK_RECORD);
            for (Object o : this.mvReadyFiles) {
                ManagedFastInputChannel fc = (ManagedFastInputChannel) o;
                this.close(fc, NIOFileReader.OK_RECORD);
            }
            throw new Exception("Failed to open file: " + e.toString());
        }
        return file;
    }

    public int getSkipLines() {
        return this.miSkipLines;
    }

    public SourceFieldDefinition[] getSourceFieldDefinition() {
        SourceFieldDefinition[] sf = new SourceFieldDefinition[this.mOutPorts.length];
        for (int i = 0; i < sf.length; i++) {
            sf[i] = ((FileETLOutPort) this.mOutPorts[i]).sf;
        }
        return sf;
    }

    /**
	 * Handle event.
	 * 
	 * @param eventCode
	 *            the event code
	 * @param portIndex
	 *            the port index
	 * 
	 * @return the object
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    protected Object handleEvent(int eventCode, int portIndex) throws IOException, KETLThreadException {
        switch(eventCode) {
            case FieldLevelFastInputChannel.END_OF_FILE:
                if (this.mCurrentFileChannel.mReader.isEndOfFile()) {
                    this.close(this.mCurrentFileChannel, portIndex < this.mOutPorts.length - 1 ? NIOFileReader.PARTIAL_RECORD : NIOFileReader.OK_RECORD);
                    return null;
                } else throw new KETLThreadException("Problem passing field", this);
            case FieldLevelFastInputChannel.BUFFER_TO_SMALL:
                if (this.buf.length > this.mMaxLineLength * 4) {
                    throw new KETLThreadException("Field " + this.mOutPorts[portIndex].mstrName + " length is greater than max line length of " + this.mMaxLineLength, this);
                }
                this.buf = new char[this.buf.length * 2];
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Increased buffer size to allow for larger fields");
                return this.buf;
            default:
                throw new KETLThreadException("Result from field level parser unknown: " + eventCode, this);
        }
    }

    @Override
    public Object handleEventCode(int eventCode) {
        return super.handleEventCode(eventCode);
    }

    @Override
    public Object handleException(Exception e) throws Exception {
        if (e instanceof EOFException) {
            this.close(this.mCurrentFileChannel, NIOFileReader.OK_RECORD);
            while (this.mvReadyFiles.size() > 0) {
                this.mCurrentFileChannel = this.getReader(this.mvReadyFiles.remove(0));
                if (this.mCurrentFileChannel != null) return this.mCurrentFileChannel.mReader;
            }
            if (this.mDeleteSource) this.deleteFiles(); else if (this.mMoveSource != null) this.moveFiles();
            return null;
        }
        return super.handleException(e);
    }

    @Override
    public Object handlePortEventCode(int eventCode, int portIndex) throws IOException, KETLThreadException {
        switch(eventCode) {
            case FieldLevelFastInputChannel.END_OF_FILE:
                if (this.mCurrentFileChannel.mReader.isEndOfFile()) {
                    this.close(this.getCurrentFileChannel(), portIndex < this.mOutPorts.length - 1 ? NIOFileReader.PARTIAL_RECORD : NIOFileReader.OK_RECORD);
                    return null;
                } else throw new KETLThreadException("Problem passing field", this);
            case FieldLevelFastInputChannel.BUFFER_TO_SMALL:
                if (this.buf.length > this.mMaxLineLength * 4) {
                    throw new KETLThreadException("Field " + this.mOutPorts[portIndex].mstrName + " length is greater than max line length of " + this.mMaxLineLength, this);
                }
                this.buf = new char[this.buf.length * 2];
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Increased buffer size to allow for larger fields");
                return this.buf;
            default:
                throw new KETLThreadException("Result from field level parser unknown: " + eventCode, this);
        }
    }

    @Override
    public Object handlePortException(Exception e, int portIndex) throws KETLThreadException {
        ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Unexpected error reading file " + e.toString());
        if (this.mRecordCounter >= 0) throw new KETLThreadException("Check record " + this.mRecordCounter + (portIndex >= 0 ? ", field " + (portIndex + 1) : ""), this);
        while (this.mvReadyFiles.size() > 0) {
            this.mCurrentFileChannel = this.mvReadyFiles.remove(0);
            try {
                this.close(this.mCurrentFileChannel, NIOFileReader.OK_RECORD);
            } catch (IOException e1) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Could not close file channel - " + e.toString());
            }
        }
        return null;
    }

    public boolean ignoreLastRecord() {
        return this.mbAllowInvalidLastRecord;
    }

    /**
	 * DOCUMENT ME!.
	 * 
	 * @param xmlSourceNode
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    @Override
    protected int initialize(Node xmlSourceNode) throws KETLThreadException {
        int res;
        if ((res = super.initialize(xmlSourceNode)) != 0) {
            return res;
        }
        if (this.maParameters == null) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "No complete parameter sets found, check that the following exist:\n" + this.getRequiredTagsMessage());
            return -2;
        }
        this.mAllowDuplicates = XMLHelper.getAttributeAsBoolean(xmlSourceNode.getAttributes(), NIOFileReader.ALLOW_DUPLICATES_ATTRIBUTE, false);
        this.mCharacterSet = XMLHelper.getAttributeAsString(xmlSourceNode.getAttributes(), NIOFileReader.CHARACTERSET_ATTRIB, java.nio.charset.Charset.defaultCharset().name());
        this.mDeleteSource = XMLHelper.getAttributeAsBoolean(xmlSourceNode.getAttributes(), NIOFileReader.DELETESOURCE_ATTRIB, false);
        this.mMoveSource = XMLHelper.getAttributeAsString(xmlSourceNode.getAttributes(), NIOFileReader.MOVESOURCE_ATTRIB, null);
        this.mCharacterSet = XMLHelper.getAttributeAsString(xmlSourceNode.getAttributes(), NIOFileReader.CHARACTERSET_ATTRIB, null);
        this.mCodingErrorAction = XMLHelper.getAttributeAsString(xmlSourceNode.getAttributes(), NIOFileReader.CODINGERRORACTION_ATTRIB, NIOFileReader.REPORT_ACTION);
        this.mZipped = XMLHelper.getAttributeAsBoolean(xmlSourceNode.getAttributes(), NIOFileReader.ZIPPED, false);
        this.miSkipLines = XMLHelper.getAttributeAsInt(xmlSourceNode.getAttributes(), NIOFileReader.SKIP_LINES, 0);
        this.mstrDefaultRecordDelimter = XMLHelper.getAttributeAsString(xmlSourceNode.getAttributes(), NIOFileReader.RECORD_DELIMITER, NIOFileReader.DEFAULT_RECORD_DELIMITER);
        this.mbAllowInvalidLastRecord = (XMLHelper.getAttributeAsBoolean(xmlSourceNode.getAttributes(), NIOFileReader.ALLOW_INVALID_LAST_RECORD, NIOFileReader.DEFAULT_ALLOW_INVALID_LAST_RECORD));
        this.mIOBufferSize = XMLHelper.getAttributeAsInt(xmlSourceNode.getAttributes(), "IOBUFFER", 16384);
        this.mMaxLineLength = XMLHelper.getAttributeAsInt(xmlSourceNode.getAttributes(), "MAXLINELENGTH", 16384);
        this.mBufferLength = this.mMaxLineLength;
        SourceFieldDefinition lastNonInternalSf = null;
        for (int i = 0; i < this.mOutPorts.length; i++) {
            SourceFieldDefinition sf = ((FileETLOutPort) this.mOutPorts[i]).getSourceFieldDefinition();
            if (sf.AverageLength == 0) sf.AverageLength = FieldLevelFastInputChannel.MAXFIELDLENGTH;
            if (sf.MaxLength < 0) sf.MaxLength = this.mMaxLineLength;
            if (this.mBufferLength < ((sf.getQuoteStart() == null ? 0 : sf.getQuoteStartLength()) + sf.MaxLength + (sf.getQuoteEnd() == null ? 0 : sf.getQuoteEndLength()))) this.mBufferLength = ((sf.getQuoteStart() == null ? 0 : sf.getQuoteStartLength()) + sf.MaxLength + (sf.getQuoteEnd() == null ? 0 : sf.getQuoteEndLength()));
            if (sf.hasInternal() == false) lastNonInternalSf = sf;
        }
        if (lastNonInternalSf != null) lastNonInternalSf.setDelimiter(this.mstrDefaultRecordDelimter);
        ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Initial max line length accounting for quotes: " + this.mBufferLength);
        this.buf = new char[this.mBufferLength];
        for (ETLOutPort element : this.mOutPorts) {
            SourceFieldDefinition sf = ((FileETLOutPort) element).sf;
            if (java.util.Date.class.isAssignableFrom(sf.DataType)) {
                sf.DateFormatter = new FastSimpleDateFormat(sf.FormatString);
                sf.position = new ParsePosition(0);
            }
        }
        try {
            if (this.getFiles() == false) {
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "No files found");
                for (int i = 0; i < this.maParameters.size(); i++) {
                    String searchPath = this.getParameterValue(i, NIOFileReader.SEARCHPATH);
                    ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Search path(s): " + searchPath);
                }
                throw new KETLThreadException("No files found, check search paths", this);
            }
        } catch (Exception e) {
            throw new KETLThreadException(e, this);
        }
        return 0;
    }

    /**
	 * Move files.
	 */
    private void moveFiles() {
        for (Object o : this.maFiles) {
            File fn = new File((String) o);
            File dir = new File(this.mMoveSource);
            if (fn.renameTo(new File(dir, fn.getName())) == false) ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Failed to move file: " + fn.getAbsolutePath()); else ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Moved file: " + fn.getAbsolutePath());
        }
    }

    @Override
    protected ETLOutPort getNewOutPort(com.kni.etl.ketl.ETLStep srcStep) {
        return new FileETLOutPort(this, this);
    }

    /**
	 * Gets the buffer.
	 * 
	 * @return the buffer
	 */
    public char[] getBuffer() {
        return this.buf;
    }

    @Override
    protected void close(boolean success, boolean jobFailed) {
        if (this.mvReadyFiles == null) return;
        for (Object o : this.mvReadyFiles) {
            ManagedFastInputChannel rf = (ManagedFastInputChannel) o;
            try {
                rf.close();
            } catch (IOException e) {
                ResourcePool.LogException(e, this);
            }
        }
    }
}
