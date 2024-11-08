import java.io.FileOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Calendar;

public class GenericIteratorWriter {

    public static final String writer__ARGUMENT_CONTROL_PREFIX = "jsa:";

    public static final String writer__ARGUMENT_OutputFolder = "o=";

    public static final String writer__ARGUMENT_BackupFolder = "b=";

    public static final String writer__ARGUMENT_NoBackup = "B";

    public static final String writer__UITEXT_Method = "method ";

    public static final String writer__UITEXT_Main = "main ";

    public static final String writer__UITEXT_ExceptionIn = "Exception in ";

    public static final String writer__UITEXT_ColonNewLine = ":\n";

    public static final String writer__UITEXT_NewLine = "\n";

    public static final String writer__UITEXT_Section = "section ";

    public static final String writer__UITEXT_SavedFile = "Saved file:       ";

    public static final String writer__UITEXT_UnableToSaveFile = "Unable to save file: ";

    public static final String writer__UITEXT_UnableToBackupFile = "Unable to backup file: ";

    public static final String writer__UITEXT_ToBackupFolder = " to backup folder: ";

    public static final String writer__UITEXT_BackupFolderColon = "Backup folder: ";

    public static final String writer__UITEXT_BackupFolderExistFailure = " does not exist and cannot be created.";

    public static final String writer__UITEXT_BackupFolderNotAFolder = " is not a folder.";

    public static final String writer__UITEXT_BackupFolderNotWritable = " is not writable.";

    public static final String writer__UITEXT_CodeWriterState = "Code Writer State: ";

    public static final String writer__UITEXT_GetFileIndexEquals = "\n_getFileIndex()    = ";

    public static final String writer__UITEXT_GetFullFileNameEquals = "\n_getFullFileName() = ";

    public static final String writer__UITEXT_GetOutputFolderEquals = "\n_getOutputFolder() = ";

    public static final String writer__UITEXT_ErrorHeader = "\n\n--- CodeWriter Error Description Start ---\n\n";

    public static final String writer__UITEXT_ErrorFooter = "\n--- CodeWriter Error Description End -----\n\n";

    public static final String writer__UITEXT_PlaceHolderException = "This placeholder Exception should never be thrown: there is an error in the WriterFormat.";

    public static final int writer__FILE_BUFFER_SIZE = 4096;

    protected String[] writer__iFileNameRoots = new String[] {};

    protected int writer__iNumFiles = 0;

    protected String writer__iFileNamePrefix = "";

    protected String writer__iFileNameSuffix = "";

    protected String writer__iBackupPrefix = "";

    protected String writer__iBackupSuffix = "";

    protected StringBuffer writer__iCurrentText = new StringBuffer();

    protected int writer__iCurrentFileIndex = 0;

    protected String[] writer__iArgs = new String[0];

    protected int writer__iNumArgs = 0;

    protected boolean writer__iSave = true;

    protected boolean writer__iBackup = true;

    protected String writer__iOutputFolder = ".";

    protected String writer__iBackupFolder = ".";

    protected Hashtable writer__iProperties = new Hashtable();

    protected boolean writer__iPropertiesInitialised = false;

    private static final void usageAndExit(String msg) {
        System.out.println("\n" + msg + "\nUsage:" + "\njostraca java.lang.GenericIterator iterator-spec-file.txt" + "\njostraca java.lang.GenericIterator Name" + "\njostraca java.lang.GenericIterator Name package.name" + "\n");
        System.exit(1);
    }

    private void inlineObjectValue(String rType, String rTypeObject) {
        _insert("\n    ");
        _insert(rTypeObject);
        _insert(" object_");
        _insert(rType);
        _insert(" = (");
        _insert(rTypeObject);
        _insert(") object;\n");
        if (!rType.equals(rTypeObject)) {
            _insert("    ");
            _insert(rType);
            _insert(" value_");
            _insert(rType);
            _insert(" = object_");
            _insert(rType);
            _insert(".");
            _insert(rType);
            _insert("Value();\n");
        } else {
            _insert("    ");
            _insert(rType);
            _insert(" value_");
            _insert(rType);
            _insert(" = object_");
            _insert(rType);
            _insert(";\n");
        }
    }

    private static final String makeTypeObject(String rType) {
        if ("boolean".equals(rType)) {
            return "Boolean";
        }
        if ("byte".equals(rType)) {
            return "Byte";
        }
        if ("char".equals(rType)) {
            return "Char";
        }
        if ("short".equals(rType)) {
            return "Short";
        }
        if ("int".equals(rType)) {
            return "Integer";
        }
        if ("long".equals(rType)) {
            return "Long";
        }
        if ("float".equals(rType)) {
            return "Float";
        }
        if ("double".equals(rType)) {
            return "Double";
        }
        return rType;
    }

    /** Execute Write */
    public static void main(String rArgs[]) {
        GenericIteratorWriter codeWriter = new GenericIteratorWriter();
        try {
            codeWriter.writer__initialize();
            codeWriter.writer__handleArgs(rArgs);
            codeWriter.writer__write();
        } catch (Exception e) {
            codeWriter.writer__handleException(writer__UITEXT_ExceptionIn + writer__UITEXT_Method + writer__UITEXT_Main, e);
        }
    }

    public void writer__initialize() {
        writer__iCurrentFileIndex = 0;
        writer__setDefaults();
    }

    /** Main loop.
  *  Template script is placed here in the @body section.
  */
    public void writer__write() throws Exception {
        if (false) {
            throw new Exception(writer__UITEXT_PlaceHolderException);
        }
        String writer__currentSection = "init";
        try {
            if (false) {
                throw new Exception(writer__UITEXT_PlaceHolderException);
            }
            String noFirstArgMsg = "Please specify Iterator name and package or iterator-spec-file.txt.";
            String[] specObjectNames = new String[] {};
            String specPackage = "";
            String objectSpecFileName = _getFirstUserArg();
            if (0 == objectSpecFileName.length()) {
                usageAndExit(noFirstArgMsg);
            }
            java.io.File objectSpecFile = new java.io.File(objectSpecFileName);
            if (!objectSpecFile.exists()) {
                specObjectNames = new String[] { objectSpecFileName };
                String packageFromArg = _getSecondUserArg();
                if (0 < packageFromArg.length()) {
                    specPackage = "package " + packageFromArg + ";";
                }
            } else {
                Vector specObjectNamesVector = new Vector();
                String line = null;
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(objectSpecFile));
                while (null != (line = br.readLine())) {
                    if (line.startsWith("package")) {
                        specPackage = line;
                    } else {
                        line = line.trim();
                        if (0 < line.length()) {
                            specObjectNamesVector.addElement(line);
                        }
                    }
                }
                specObjectNames = new String[specObjectNamesVector.size()];
                specObjectNamesVector.copyInto(specObjectNames);
            }
            _setFileNameRoots(specObjectNames);
            _setFileNameSuffix("Iterator.java");
            int writer__numFiles = _getNumFiles();
            int writer__fileI = 0;
            writer__next_file: for (writer__fileI = 0; writer__fileI < writer__numFiles; writer__fileI++) {
                try {
                    if (false) {
                        throw new Exception(writer__UITEXT_PlaceHolderException);
                    }
                    if (!writer__startFile()) {
                        continue writer__next_file;
                    }
                    writer__currentSection = "body";
                    String type = _getFileNameRoot();
                    String typeObject = makeTypeObject(type);
                    _insert("\n\n\n");
                    _insert(specPackage);
                    _insert("\n\nimport java.util.Date;\n\n\n\nimport java.util.Iterator;\n\n\n/** ");
                    _insert(type);
                    _insert("Iterator for Java 1.3 */\npublic class ");
                    _insert(type);
                    _insert("Iterator {\n\n  Iterator iIterator;\n\n  public ");
                    _insert(type);
                    _insert("Iterator( Iterator rIterator ) {\n    iIterator = rIterator;\n  }\n\n  public boolean hasNext() {\n    return iIterator.hasNext();\n  }\n\n  public ");
                    _insert(type);
                    _insert(" next() {\n    Object object = iIterator.next();\n    ");
                    inlineObjectValue(type, typeObject);
                    _insert("\n    return value_");
                    _insert(type);
                    _insert(";\n  }\n\n  public void remove() {\n    iIterator.remove();\n  }\n\n  public Iterator getIterator() {\n    return iIterator;\n  }\n\n}\n\n/* Jostraca Generated File ( www.jostraca.org )\n * Library Template: ");
                    _insert(_getProperty("jostraca.template.name"));
                    _insert(" (version ");
                    _insert(_getProperty("jostraca.template.version"));
                    _insert(") \n */\n\n\n\n");
                    _insert("\n\n\n");
                    _insert(" \n\n");
                    _insert("\n");
                    if (!writer__endFile()) {
                        continue writer__next_file;
                    }
                } catch (Exception e) {
                    writer__handleException(writer__UITEXT_ExceptionIn + writer__UITEXT_Section + writer__currentSection, e);
                }
                writer__nextFile();
            }
        } catch (Exception e) {
            writer__handleException(writer__UITEXT_ExceptionIn + writer__UITEXT_Section + writer__currentSection, e);
        }
    }

    /** Start writing a file. */
    public boolean writer__startFile() {
        writer__iCurrentText = new StringBuffer(writer__FILE_BUFFER_SIZE);
        return true;
    }

    /** End writing a file. */
    public boolean writer__endFile() {
        boolean endOK = true;
        String fileName = _getFullFileName();
        String filePath = writer__iOutputFolder + "\\" + fileName;
        if (writer__iBackup) {
            try {
                writer__backup(filePath, fileName, writer__iBackupFolder);
            } catch (Exception e) {
                writer__handleException(writer__UITEXT_UnableToBackupFile + filePath + writer__UITEXT_ToBackupFolder + writer__iBackupFolder, e);
                endOK = false;
            }
        }
        if (endOK && writer__iSave) {
            try {
                writer__save(filePath, writer__iCurrentText.toString());
                writer__userMessage(writer__UITEXT_SavedFile + filePath + writer__UITEXT_NewLine);
            } catch (Exception e) {
                writer__handleException(writer__UITEXT_UnableToSaveFile + filePath, e);
                endOK = false;
            }
        }
        return endOK;
    }

    /** Move to next file. */
    public void writer__nextFile() {
        writer__iCurrentFileIndex = writer__iCurrentFileIndex + 1;
    }

    /** Handle command line arguments to CodeWriter. */
    public void writer__handleArgs(String[] rArgs) {
        String argName_OutputFolder = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_OutputFolder;
        String argName_BackupFolder = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_BackupFolder;
        String argName_NoBackup = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_NoBackup;
        int numArgs = rArgs.length;
        for (int argI = 0; argI < numArgs; argI++) {
            if (rArgs[argI].startsWith(argName_OutputFolder)) {
                _setOutputFolder(rArgs[argI].substring(argName_OutputFolder.length()));
            } else if (rArgs[argI].startsWith(argName_BackupFolder)) {
                _setBackupFolder(rArgs[argI].substring(argName_BackupFolder.length()));
            } else if (argName_NoBackup.equals(rArgs[argI])) {
                _backup(false);
            }
        }
        writer__setArgs(rArgs.length, rArgs);
    }

    /** Set defaults from configuration property set. */
    public void writer__setDefaults() {
        _setFileNameRoot("GeneratedFile");
        _setFileNameSuffix(".java");
        _setOutputFolder(".");
        _setBackupFolder(".jostraca");
        _setBackupPrefix("-");
        _setBackupSuffix("-backup.txt");
        _backup("true".equals("false"));
    }

    /** Store command line arguments */
    public void writer__setArgs(int rNumArgs, String[] rArgs) {
        writer__iNumArgs = rNumArgs;
        writer__iArgs = rArgs;
    }

    /** Print a user message */
    public void writer__userMessage(String rMessage) {
        System.out.print(rMessage);
    }

    /** Handle exceptions: print an explanation for user. */
    public void writer__handleException(String rMessage, Exception rException) {
        StringBuffer userMsg = new StringBuffer(111);
        userMsg.append(writer__UITEXT_ErrorHeader);
        userMsg.append(writer__describeState() + rMessage + writer__UITEXT_ColonNewLine);
        StringWriter stringWriter = new StringWriter();
        rException.printStackTrace(new PrintWriter(stringWriter));
        userMsg.append(stringWriter.toString());
        userMsg.append(writer__UITEXT_ErrorFooter);
        writer__userMessage(userMsg.toString());
    }

    /** Provide a concise description of the state of the CodeWriter. */
    public String writer__describeState() {
        String currentState = writer__UITEXT_CodeWriterState + writer__UITEXT_GetFileIndexEquals + _getFileIndex() + writer__UITEXT_GetFullFileNameEquals + _getFullFileName() + writer__UITEXT_GetOutputFolderEquals + _getOutputFolder() + writer__UITEXT_NewLine;
        return currentState;
    }

    /** Save written files to disk.
  *  @param rFilePath Save location.
  *  @param rContent  File content.
  */
    public void writer__save(String rFilePath, String rContent) throws Exception {
        StringReader sr = new StringReader(rContent);
        BufferedReader br = new BufferedReader(sr);
        FileWriter fw = new FileWriter(rFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        String line;
        while (null != (line = br.readLine())) {
            bw.write(line);
            bw.newLine();
        }
        bw.close();
        br.close();
    }

    /** Read file from disk.
  *  @param rFilePath.
  */
    public String writer__read(String rFilePath) throws Exception {
        File file = new File(rFilePath);
        FileReader in = new FileReader(file);
        int size = (int) file.length();
        char[] data = new char[size];
        int charsRead = 0;
        while (charsRead < size) {
            charsRead += in.read(data, charsRead, size - charsRead);
        }
        return new String(data);
    }

    /** Backup overwritten files, if they exist.
  *  Backups have the format:
  *  [YYYYMMDDhhmmss][prefix][filename][suffix]
  *  @param rFilePath     Full Path of File to backup (including name).
  *  @param rFileName     Name of File to backup.
  *  @param rBackupFolder Folder to place backups in.
  */
    public void writer__backup(String rFilePath, String rFileName, String rBackupFolder) throws Exception {
        File backupFolder = new File(rBackupFolder);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdir()) {
                throw new Exception(writer__UITEXT_BackupFolderColon + backupFolder + writer__UITEXT_BackupFolderExistFailure);
            }
        }
        if (!backupFolder.isDirectory()) {
            throw new Exception(writer__UITEXT_BackupFolderColon + backupFolder + writer__UITEXT_BackupFolderNotAFolder);
        }
        if (!backupFolder.canWrite()) {
            throw new Exception(writer__UITEXT_BackupFolderColon + backupFolder + writer__UITEXT_BackupFolderNotWritable);
        }
        Calendar calendar = Calendar.getInstance();
        String year_yyyy = _align(String.valueOf(calendar.get(Calendar.YEAR)), "0", 4, 'r');
        String month_mm = _align(String.valueOf((1 + calendar.get(Calendar.MONTH))), "0", 2, 'r');
        String day_dd = _align(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), "0", 2, 'r');
        String hour_hh = _align(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), "0", 2, 'r');
        String minute_mm = _align(String.valueOf(calendar.get(Calendar.MINUTE)), "0", 2, 'r');
        String second_ss = _align(String.valueOf(calendar.get(Calendar.SECOND)), "0", 2, 'r');
        String dateTime = year_yyyy + month_mm + day_dd + hour_hh + minute_mm + second_ss;
        String backupFileName = dateTime + writer__iBackupPrefix + rFileName + writer__iBackupSuffix;
        File backupFilePath = new File(rBackupFolder, backupFileName);
        File fileToBackup = new File(rFilePath);
        if (fileToBackup.exists()) {
            String fileContents = writer__read(rFilePath);
            writer__save(backupFilePath.getPath(), fileContents);
        }
    }

    /** Set compile time properties. */
    public void writer__initProperties() {
    }

    /** Set the prefix of the files to be written.
  *  @param rPrefix Written files prefix.
  */
    public void _setFileNamePrefix(String rPrefix) {
        if (null == rPrefix) {
            return;
        }
        writer__iFileNamePrefix = rPrefix;
    }

    /** Get prefix of files to be written. */
    public String _getFileNamePrefix() {
        return writer__iFileNamePrefix;
    }

    /** Set the suffix of the files to be written.
  *  @param rSuffix Written files suffix.
  */
    public void _setFileNameSuffix(String rSuffix) {
        if (null == rSuffix) {
            return;
        }
        writer__iFileNameSuffix = rSuffix;
    }

    /** Get suffix of files to be written. */
    public String _getFileNameSuffix() {
        return writer__iFileNameSuffix;
    }

    /** Set the full name of the file to be written.
  *  Prefix and Suffix are set to empty
  *  @param rName Full name of the file to write.
  */
    public void _setFullFileName(String rName) {
        _setFileNamePrefix("");
        _setFileNameRoot(rName);
        _setFileNameSuffix("");
    }

    /** Get the full name of current file being generated. */
    public String _getFullFileName() {
        return _getFileNamePrefix() + _getFileNameRoot() + _getFileNameSuffix();
    }

    /** Set the names of the files to be written.
  *  Prefix and Suffix are set to empty
  *  @param rName Full name of the file to write.
  */
    public void _setFullFileNames(String[] rNames) {
        _setFileNamePrefix("");
        _setFileNameRoots(rNames);
        _setFileNameSuffix("");
    }

    /** Get the full names of the files to be written. */
    public String[] _getFullFileNames() {
        String[] fileNameRoots = _getFileNameRoots();
        int numFiles = fileNameRoots.length;
        String[] fullFileNames = new String[numFiles];
        String fileNamePrefix = _getFileNamePrefix();
        String fileNameSuffix = _getFileNameSuffix();
        for (int fileI = 0; fileI < numFiles; fileI++) {
            fullFileNames[fileI] = fileNamePrefix + fileNameRoots[fileI] + fileNameSuffix;
        }
        return fullFileNames;
    }

    /** Set the root of the name of the file to be written.
  *  @param rFileNameRoot Root of the name of file to be written.
  */
    public void _setFileNameRoot(String rFileNameRoot) {
        if (null == rFileNameRoot) {
            return;
        }
        _setFileNameRoots(new String[] { rFileNameRoot });
    }

    /** Get the root of the name of current file being generated. */
    public String _getFileNameRoot() {
        if (0 < writer__iFileNameRoots.length) {
            return writer__iFileNameRoots[writer__iCurrentFileIndex];
        }
        return "";
    }

    /** Set the roots of the names of the files to be written.
  *  @param rFileNameRoots Roots of names of files to be written.
  */
    public void _setFileNameRoots(String[] rFileNameRoots) {
        if (null == rFileNameRoots) {
            return;
        }
        String[] roots = (String[]) rFileNameRoots.clone();
        int numRoots = roots.length;
        for (int rootI = 0; rootI < numRoots; rootI++) {
            if (null == roots[rootI]) {
                roots[rootI] = "";
            }
        }
        writer__iFileNameRoots = roots;
        writer__iNumFiles = numRoots;
    }

    /** Get roots of the names of files to be written. */
    public String[] _getFileNameRoots() {
        return writer__iFileNameRoots;
    }

    /** Get index of file currently being generated. */
    public int _getFileIndex() {
        return writer__iCurrentFileIndex;
    }

    /** Get number of generated files. */
    public int _getNumFiles() {
        return writer__iNumFiles;
    }

    /** Set output folder.
  *  @param rOutputFolder Folder to output generated code to.
  */
    public void _setOutputFolder(String rOutputFolder) {
        writer__iOutputFolder = rOutputFolder;
    }

    /** Get output folder. */
    public String _getOutputFolder() {
        return writer__iOutputFolder;
    }

    /** Set backup folder.
  *  @param rBackupFolder Folder to backup overwritten files to.
  */
    public void _setBackupFolder(String rBackupFolder) {
        writer__iBackupFolder = writer__iOutputFolder + "\\" + rBackupFolder;
    }

    /** Get backup folder. */
    public String _getBackupFolder() {
        return writer__iBackupFolder;
    }

    /** Set the suffix of backup files.
  *  @param rSuffix Backup files suffix.
  */
    public void _setBackupSuffix(String rSuffix) {
        if (null == rSuffix) {
            return;
        }
        writer__iBackupSuffix = rSuffix;
    }

    /** Set the prefix of backup files.
  *  @param rPrefix Backup files prefix.
  */
    public void _setBackupPrefix(String rPrefix) {
        if (null == rPrefix) {
            return;
        }
        writer__iBackupPrefix = rPrefix;
    }

    /** Set to true if written files are to be backed up to disk automatically.
  *  @param rBackup True => Backup files to disk.
  */
    public void _backup(boolean rBackup) {
        writer__iBackup = rBackup;
    }

    /** Set to true if written files are to be saved to disk automatically.
  *  @param rSave True => Save written files to disk.
  */
    public void _save(boolean rSave) {
        writer__iSave = rSave;
    }

    /** Get compile time property
  *  @param rName Name of property to get.
  */
    public String _getProperty(String rName) {
        String result = "";
        if (!writer__iPropertiesInitialised) {
            writer__initProperties();
        }
        if (writer__iProperties.containsKey(rName)) {
            result = (String) writer__iProperties.get(rName);
        }
        return result;
    }

    /** Get first user arg - that is, first arg with no writer__ARGUMENT_CONTROL_PREFIX. */
    public String _getFirstUserArg() {
        return _getUserArg(0);
    }

    /** Get second user arg - that is, second arg with no writer__ARGUMENT_CONTROL_PREFIX. */
    public String _getSecondUserArg() {
        return _getUserArg(1);
    }

    /** Get third user arg - that is, third arg with no writer__ARGUMENT_CONTROL_PREFIX. */
    public String _getThirdUserArg() {
        return _getUserArg(2);
    }

    /** Get user arg at specified ordinal.
  *  @param rOrdinal ordinal of user arg to get.
  */
    public String _getUserArg(int rOrdinal) {
        if (null == writer__iArgs) {
            return "";
        }
        int ordinal = 0;
        int numArgs = writer__iArgs.length;
        next_arg: for (int argI = 0; argI < numArgs; argI++) {
            if (writer__iArgs[argI].startsWith(writer__ARGUMENT_CONTROL_PREFIX)) {
                continue next_arg;
            } else {
                if (ordinal == rOrdinal) {
                    return writer__iArgs[argI];
                } else {
                    ordinal++;
                }
            }
        }
        return "";
    }

    /** Get command line arguments to CodeWriter. */
    public String[] _getArgs() {
        return writer__iArgs;
    }

    /** Get number of command line arguments to CodeWriter. */
    public int _getNumArgs() {
        return writer__iNumArgs;
    }

    /** Insert text into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rText Text to insert.
  */
    public void _insert(String rText) {
        writer__iCurrentText.append(rText);
    }

    /** Insert string representation of object into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rObject Object to insert.
  */
    public void _insert(Object rObject) {
        writer__iCurrentText.append("" + rObject);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rInt int to insert
  */
    public void _insert(int rInt) {
        writer__iCurrentText.append(rInt);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rLong long to insert
  */
    public void _insert(long rLong) {
        writer__iCurrentText.append(rLong);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rShort short to insert
  */
    public void _insert(short rShort) {
        writer__iCurrentText.append(rShort);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rByte byte to insert
  */
    public void _insert(byte rByte) {
        writer__iCurrentText.append(rByte);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rDouble double to insert
  */
    public void _insert(double rDouble) {
        writer__iCurrentText.append(rDouble);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rFloat float to insert
  */
    public void _insert(float rFloat) {
        writer__iCurrentText.append(rFloat);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rChar char to insert
  */
    public void _insert(char rChar) {
        writer__iCurrentText.append(rChar);
    }

    /** Insert string representation of primitive data type into written file.
  *  Abbreviated by <%=foobar%>.
  *  @param rBoolean boolean to insert
  */
    public void _insert(boolean rBoolean) {
        writer__iCurrentText.append(rBoolean);
    }

    /** Create a String containing specified number of spaces.
  *  @param rNumSpaces Number of spaces to place in String
  */
    public String _spaces(int rNumSpaces) {
        int numSpaces = rNumSpaces;
        if (0 > numSpaces) {
            numSpaces *= -1;
        }
        StringBuffer spaces = new StringBuffer(numSpaces);
        for (int spaceI = 0; spaceI < numSpaces; spaceI++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    /** Left align String with spaces. */
    public String _left(String rText, int rColWidth) {
        return _align(rText, " ", rColWidth, 'l');
    }

    /** Right align String with spaces. */
    public String _right(String rText, int rColWidth) {
        return _align(rText, " ", rColWidth, 'r');
    }

    /** Center align String with spaces. */
    public String _center(String rText, int rColWidth) {
        return _align(rText, " ", rColWidth, 'c');
    }

    /** Align text within background text to specified column width.
  *  Alignment can be 'l': left, 'c': center, 'r': right
  */
    public String _align(String rText, String rBackText, int rColWidth, char rAlignment) {
        String result = rText;
        if (null == rText) {
            result = "";
        } else if (null != rBackText) {
            try {
                int textLen = rText.length();
                if (rColWidth > textLen) {
                    int backTextLen = rBackText.length();
                    int remainWidth = rColWidth - textLen;
                    int backTextRepeats = remainWidth / backTextLen;
                    int backTextRemain = remainWidth % backTextLen;
                    String back = "";
                    for (int backTextI = 0; backTextI < backTextRepeats; backTextI++) {
                        back = back + rBackText;
                    }
                    back = back + rBackText.substring(0, backTextRemain);
                    switch(rAlignment) {
                        case 'l':
                            result = result + back;
                            break;
                        case 'c':
                            result = back.substring(0, (back.length() / 2)) + result + back.substring((back.length() / 2));
                            break;
                        case 'r':
                            result = back + result;
                            break;
                    }
                }
            } catch (Exception e) {
                result = rText;
            }
        }
        return result;
    }

    /** Set current text of file currently being generated. */
    public void _setText(String rText) {
        writer__iCurrentText = new StringBuffer(rText);
    }

    /** Get current text of file currently being generated. */
    public String _getText() {
        return writer__iCurrentText.toString();
    }
}
