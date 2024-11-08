import java.io.FileOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Calendar;

public class JostracaCodeWriter {

    public static final String writer__ARGUMENT_CONTROL_PREFIX = "jsa:";

    public static final String writer__ARGUMENT_OutputFolder = "o=";

    public static final String writer__ARGUMENT_CodeWriterOutputFolder = "O=";

    public static final String writer__ARGUMENT_BackupFolder = "b=";

    public static final String writer__ARGUMENT_NoBackup = "B";

    public static final String writer__UITEXT_Method = "method ";

    public static final String writer__UITEXT_Main = "main ";

    public static final String writer__UITEXT_ExceptionIn = "Exception in ";

    public static final String writer__UITEXT_ColonNewLine = ":\n";

    public static final String writer__UITEXT_NewLine = "\n";

    public static final String writer__UITEXT_Section = "section ";

    public static final String writer__UITEXT_SavedFile = "Saved file:     ";

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
        System.out.println("\n" + msg + "\nUsage:" + "\njostraca java.lang.GenericCollection collection-spec-file.txt" + "\njostraca java.lang.GenericCollection Name" + "\njostraca java.lang.GenericCollection Name package.name" + "\n");
        System.exit(1);
    }

    private void inlineObjectValue(String rType, String rTypeObject) {
        _insert("\r\n    ");
        _insert(rTypeObject);
        _insert(" object_");
        _insert(rType);
        _insert(" = (");
        _insert(rTypeObject);
        _insert(") object;\r\n");
        if (!rType.equals(rTypeObject)) {
            _insert("    ");
            _insert(rType);
            _insert(" value_");
            _insert(rType);
            _insert(" = object_");
            _insert(rType);
            _insert(".");
            _insert(rType);
            _insert("Value();\r\n");
        } else {
            _insert("    ");
            _insert(rType);
            _insert(" value_");
            _insert(rType);
            _insert(" = object_");
            _insert(rType);
            _insert(";\r\n");
        }
    }

    private void inlineMakeObject(String rType, String rTypeObject) {
        _insert("\r\n");
        if (!rType.equals(rTypeObject)) {
            _insert("    ");
            _insert(rTypeObject);
            _insert(" object_");
            _insert(rType);
            _insert(" = new ");
            _insert(rTypeObject);
            _insert("( value_");
            _insert(rType);
            _insert(" );\r\n");
        } else {
            _insert("    ");
            _insert(rTypeObject);
            _insert(" object_");
            _insert(rType);
            _insert(" = value_");
            _insert(rType);
            _insert(";\r\n");
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
        JostracaCodeWriter codeWriter = new JostracaCodeWriter();
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
            String noFirstArgMsg = "Please specify Collection name and package or collection-spec-file.txt.";
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
            _setFileNameSuffix("Collection.java");
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
                    _insert("\r\n\r\n\r\n");
                    _insert(specPackage);
                    _insert("\r\n\r\nimport java.util.Collection;\r\n\r\n\r\n/** ");
                    _insert(type);
                    _insert("Collection for Java 1.3 */\r\npublic class ");
                    _insert(type);
                    _insert("Collection {\r\n\r\n  private Collection iCollection;\r\n\r\n  public ");
                    _insert(type);
                    _insert("Collection( Collection rCollection ) {\r\n    iCollection = rCollection;\r\n  }\r\n\r\n\r\n  public boolean add( ");
                    _insert(type);
                    _insert(" r");
                    _insert(type);
                    _insert(" ) {\r\n    ");
                    _insert(type);
                    _insert(" value_");
                    _insert(type);
                    _insert(" = r");
                    _insert(type);
                    _insert(";\r\n    ");
                    inlineMakeObject(type, typeObject);
                    _insert("\r\n    return iCollection.add( object_");
                    _insert(type);
                    _insert(" );\r\n  }\r\n\r\n  public boolean addAll( ");
                    _insert(type);
                    _insert("Collection r");
                    _insert(type);
                    _insert("Collection ) {\r\n    return iCollection.addAll( r");
                    _insert(type);
                    _insert("Collection.getCollection() );\r\n  }\r\n\r\n  public void clear() {\r\n    iCollection.clear();\r\n  }\r\n\r\n  public boolean contains( ");
                    _insert(type);
                    _insert(" r");
                    _insert(type);
                    _insert(" ) {\r\n    ");
                    _insert(type);
                    _insert(" value_");
                    _insert(type);
                    _insert(" = r");
                    _insert(type);
                    _insert(";\r\n    ");
                    inlineMakeObject(type, typeObject);
                    _insert("\r\n    return iCollection.contains( object_");
                    _insert(type);
                    _insert(" );\r\n  }\r\n\r\n  public boolean containsAll( ");
                    _insert(type);
                    _insert("Collection r");
                    _insert(type);
                    _insert("Collection ) {\r\n    return iCollection.containsAll( r");
                    _insert(type);
                    _insert("Collection.getCollection() );\r\n  }\r\n\r\n  public boolean equals( Object rObject ) {\r\n    if( rObject instanceof ");
                    _insert(type);
                    _insert("Collection ) {\r\n      return iCollection.equals( ((");
                    _insert(type);
                    _insert("Collection)rObject).getCollection() );\r\n    }\r\n    else {\r\n      return false;\r\n    }\r\n  }\r\n\r\n  public int hashCode() {\r\n    return iCollection.hashCode();\r\n  }\r\n\r\n  public boolean isEmpty() {\r\n    return iCollection.isEmpty();\r\n  }\r\n\r\n  public ");
                    _insert(type);
                    _insert("Iterator iterator() {\r\n    return new ");
                    _insert(type);
                    _insert("Iterator( iCollection.iterator() );\r\n  }\r\n\r\n  public boolean remove( ");
                    _insert(type);
                    _insert(" r");
                    _insert(type);
                    _insert(" ) {\r\n    ");
                    _insert(type);
                    _insert(" value_");
                    _insert(type);
                    _insert(" = r");
                    _insert(type);
                    _insert(";\r\n    ");
                    inlineMakeObject(type, typeObject);
                    _insert("\r\n    return iCollection.remove( object_");
                    _insert(type);
                    _insert(" );\r\n  }\r\n\r\n  public boolean removeAll( ");
                    _insert(type);
                    _insert("Collection r");
                    _insert(type);
                    _insert("Collection ) {\r\n    return iCollection.removeAll( r");
                    _insert(type);
                    _insert("Collection.getCollection() );\r\n  }\r\n\r\n  public boolean retainAll( ");
                    _insert(type);
                    _insert("Collection r");
                    _insert(type);
                    _insert("Collection ) {\r\n    return iCollection.retainAll( r");
                    _insert(type);
                    _insert("Collection.getCollection() );\r\n  }\r\n\r\n  public int size() {\r\n    return iCollection.size();\r\n  }\r\n\r\n  public ");
                    _insert(type);
                    _insert("[] toArray() {\r\n    return toArray( new ");
                    _insert(type);
                    _insert("[] {} );\r\n  }    \r\n\r\n  public ");
                    _insert(type);
                    _insert("[] toArray( ");
                    _insert(type);
                    _insert("[] r");
                    _insert(type);
                    _insert(" ) {\r\n    \r\n    ");
                    _insert(typeObject);
                    _insert("[] objectArray_");
                    _insert(type);
                    _insert(" = (");
                    _insert(typeObject);
                    _insert("[]) iCollection.toArray( new ");
                    _insert(typeObject);
                    _insert("[] {} );\r\n    ");
                    if (type.equals(typeObject)) {
                        _insert("\r\n    return objectArray_");
                        _insert(type);
                        _insert(";\r\n    ");
                    } else {
                        _insert("\r\n\r\n    ");
                        _insert(type);
                        _insert("[] ");
                        _insert(type);
                        _insert("Array = new ");
                        _insert(type);
                        _insert("[ objectArray_");
                        _insert(type);
                        _insert(".length ];\r\n    for( int ");
                        _insert(type);
                        _insert("I = 0; ");
                        _insert(type);
                        _insert("I < objectArray_");
                        _insert(type);
                        _insert(".length; ");
                        _insert(type);
                        _insert("I++ ) {\r\n      ");
                        _insert(type);
                        _insert("Array[ ");
                        _insert(type);
                        _insert("I ] = objectArray_");
                        _insert(type);
                        _insert("[");
                        _insert(type);
                        _insert("I].");
                        _insert(type);
                        _insert("Value();\r\n    }\r\n    return ");
                        _insert(type);
                        _insert("Array;\r\n    ");
                    }
                    _insert("\r\n  }\r\n\r\n\r\n  public Collection getCollection() {\r\n    return iCollection;\r\n  }\r\n\r\n}\r\n\r\n/* Jostraca Generated File ( www.jostraca.org )\r\n * Library Template: ");
                    _insert(_getProperty("jostraca.template.name"));
                    _insert(" (version ");
                    _insert(_getProperty("jostraca.template.version"));
                    _insert(") \r\n */\r\n\r\n\r\n\r\n");
                    _insert("\r\n\r\n\r\n");
                    _insert(" \r\n\r\n");
                    _insert(" \r\n\r\n");
                    _insert("\r\n\r\n\r\n");
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
        String argName_CodeWriterOutputFolder = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_CodeWriterOutputFolder;
        String argName_BackupFolder = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_BackupFolder;
        String argName_NoBackup = writer__ARGUMENT_CONTROL_PREFIX + writer__ARGUMENT_NoBackup;
        String argValue_OutputFolder = ".";
        String argValue_CodeWriterOutputFolder = "";
        String argValue_NoBackup = "false";
        String argValue_BackupFolder = ".jostraca";
        int numArgs = rArgs.length;
        for (int argI = 0; argI < numArgs; argI++) {
            if (rArgs[argI].startsWith(argName_OutputFolder)) {
                argValue_OutputFolder = rArgs[argI].substring(argName_OutputFolder.length());
            } else if (rArgs[argI].startsWith(argName_CodeWriterOutputFolder)) {
                argValue_CodeWriterOutputFolder = rArgs[argI].substring(argName_CodeWriterOutputFolder.length());
            } else if (rArgs[argI].startsWith(argName_BackupFolder)) {
                argValue_BackupFolder = rArgs[argI].substring(argName_BackupFolder.length());
            } else if (argName_NoBackup.equals(rArgs[argI])) {
                argValue_NoBackup = "true";
            }
        }
        writer__setOutputFolders(argValue_OutputFolder, argValue_CodeWriterOutputFolder);
        _backup("false".equals(argValue_NoBackup));
        _setBackupFolder(argValue_BackupFolder);
        writer__setArgs(rArgs.length, rArgs);
    }

    /** Set defaults from configuration property set. */
    public void writer__setDefaults() {
        _setBackupPrefix("-");
        _setBackupSuffix("-backup.txt");
        _setFileNameSuffix(".java");
    }

    /** Set output folders for CodeWriter.
  *  The CodeWriter writes generated files to the standard output folder (-o),
  *  unless a CodeWriter specific output folder is specified (-O). This allows
  *  The CodeWriter source file to be placed in a separate location
  *  to the files generated by the CodeWriter.
  */
    public void writer__setOutputFolders(String rArgValue_outputFolder, String rArgValue_codeWriterOutputFolder) {
        _setOutputFolder(rArgValue_outputFolder);
        if (0 < rArgValue_codeWriterOutputFolder.length()) {
            _setOutputFolder(rArgValue_codeWriterOutputFolder);
        }
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
        FileOutputStream fout = new FileOutputStream(rFilePath);
        PrintWriter pout = new PrintWriter(fout);
        pout.print(rContent);
        pout.close();
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
        String[] propertyList = new String[] { "parse.SectionMarker", "!", "jostraca.TemplateScript.CanonicalName.c", "c", "main.ExecuteCodeWriter", "yes", "main.CodeWriter.argument.name.OutputFolder", "o=", "main.ExternalControllerOptions", "", "lang.CommentLineSuffix", "", "main.DumpTemplate", "no", "lang.StringEscapeTransform", "JavaStringEscapeTransform", "jostraca.regexp.AnyWhiteSpace", "\\s*", "jostraca.standard.TemplateTextTransforms", "TextElementTransform, AppendNewLineTransform", "lang.TrueString", "true", "main.CodeWriter.argument.value.OutputFolder", ".", "parse.regexp.CloseInnerChar", "%", "jostraca.LocalConfigFileName", "local.conf", "lang.InsertSuffix", ");", "main.MakeBackup", "no", "lang.CommentLinePrefix", "//", "parse.regexp.DirectiveMarker", "@", "parse.regexp.CloseOuterChar", ">", "jostraca.TemplateScript.CanonicalName.rb", "ruby", "lang.NameValueList.itemSeparator", ",", "jostraca.regexp.SubmatchDirectiveName", "([a-zA-Z_:|][0-9a-zA-Z_:|]*)", "main.ExternalController", "java", "lang.InsertPrefix", "_insert(", "parse.OpenInnerChar", "%", "lang.TemplateTextTransforms", "JavaStringEscapeTransform, TextElementTransform, AppendNewLineTransform", "main.IncludeBase", "", "lang.FalseString", "false", "parse.OpenOuterChar", "<", "jostraca.regexp.template.IncludeDirective", "<%\\s*@include\\s+(\".+?\"|[^ ]+)\\s*([a-z- ]*)\\s*%>", "jostraca.regexp.MatchExpression", "^\\s*=(\\s*.*)$", "parse.ExpressionMarker", "=", "jostraca.standard.TemplateDirectiveTransforms", "", "main.SaveCodeWriter", "yes", "jostraca.regexp.template.ConfDirective", "<%\\s*@conf\\s+(.+?)\\s*%>", "parse.DirectiveMarker", "@", "jostraca.template.isLibrary", "true", "main.CodeWriterSuffix", ".java", "main.CodeWriter.argument.value.NoBackup", "false", "jostraca.regexp.SubmatchSectionName", "([a-zA-Z_:][0-9a-zA-Z_:]*)", "jostraca.TemplateScript.CanonicalName.jv", "java", "jostraca.TemplateScript.CanonicalName.py", "python", "jostraca.regexp.template.IncludeBlockDirective", "<%\\s*@include-block\\s+[\\\"']?(.+?)[\\\"']?\\s+/(.+?)/\\s*%>", "main.CodeWriterPrefix", "", "main.FileNameSuffix", ".java", "jostraca.template.path", "e:\\web\\proj\\jostraca\\conf\\..\\templates\\src\\java\\util\\GenericCollection.jtm", "main.CodeWriter.argument.name.BackupFolder", "b=", "jostraca.Location", "e:\\web\\proj\\jostraca\\conf\\..", "jostraca.TemplateScript.CanonicalName.pl", "perl", "lang.TemplateScriptTransforms", "AppendNewLineTransform", "main.CodeWriter.argument.ControlPrefix", "jsa:", "java.CodeWriter.package", "", "main.FileNamePrefix", "", "jostraca.regexp.template.IncludeBaseDirective", "<%\\s*@include-base\\s+[\\\"']?(.+?)[\\\"']?\\s*%>", "lang.TextQuote", "\"", "lang.NameValueList.valueQuote", "\"", "main.TemplateScript", "java", "jostraca.regexp.MatchDirective", "^\\s*@([a-zA-Z_:|][0-9a-zA-Z_:|]*)(\\s*.*)$", "main.CodeWriter.argument.name.CodeWriterOutputFolder", "O=", "main.CodeWriter.argument.value.BackupFolder", ".jostraca", "jostraca.template.name", "java.util.GenericCollection", "jostraca.standard.TemplateScriptTransforms", "AppendNewLineTransform", "main.CodeWriterController", "org.jostraca.BasicJavaCodeWriterController", "lang.NameValueList.nameQuote", "\"", "parse.CloseInnerChar", "%", "jostraca.regexp.IsExpression", "^\\s*=", "parse.CloseOuterChar", ">", "main.DumpPropertySet", "no", "lang.CodeWriterTransforms", "SimpleIndentTransform", "jostraca.template.folder", "e:\\web\\proj\\jostraca\\conf\\..\\templates\\src\\java\\util", "parse.Directives", "SectionDirective, OneLineDirective, CollapseWhiteSpaceDirective", "jostraca.properties.modifiers", "NameValueListPSM", "lang.CommentBlockSuffix", "*/", "main.OutputFolder", ".", "jostraca.TemplateScript.CanonicalName.perl", "perl", "main.JostracaVersion", "0.3", "jostraca.regexp.SubmatchAnyWhiteSpaceAnyCharsAtEnd", "(\\s*.*)$", "main.CodeWriterCompiler", "org.jostraca.BasicJavaCompiler", "java.CodeWriter.extends", "", "main.CodeWriter", "JostracaCodeWriter", "jostraca.regexp.AnyWhiteSpaceAtStart", "^\\s*", "lang.CommentBlockPrefix", "/*", "main.CodeWriterOptions", "String ", "main.CodeWriter.argument.name.NoBackup", "B", "lang.TemplateDirectiveTransforms", "", "jostraca.TemplateScript.CanonicalName.ruby", "ruby", "parse.regexp.ExpressionMarker", "=", "lang.TemplateExpressionTransforms", "ExpressionElementTransform, AppendNewLineTransform", "main.ExternalCompilerOptions", "", "parse.regexp.SectionMarker", "!", "parse.DeclarationMarker", "!", "jostraca.system.separator", "\\", "jostraca.regexp.MatchSectionName", "^\\s*([a-zA-Z_:][0-9a-zA-Z_:]*)!(\\s*.*)$", "lang.NameValueList.pairSeparator", ",", "java.CodeWriter.implements", "", "jostraca.regexp.MatchDeclaration", "^\\s*!(\\s*.*)$", "jostraca.TemplateScript.CanonicalName.python", "python", "main.CodeWriter.argument.value.CodeWriterOutputFolder", "", "jostraca.strict.version", "yes", "jostraca.version", "0.3", "main.CompileCodeWriter", "yes", "parse.regexp.OpenInnerChar", "%", "main.CodeWriterFormat", "format/BasicJavaWriterFormat.jwf", "main.AlsoGenerate", "", "jostraca.system.pathSeparator", ";", "main.BackupFolder", ".jostraca", "parse.regexp.OpenOuterChar", "<", "main.CodeBuilder", "org.jostraca.BasicCodeBuilder", "main.BackupSuffix", "-backup.txt", "jostraca.regexp.IsDirective", "^\\s*@", "main.TemplateParser", "org.jostraca.BasicTemplateParser", "main.ExternalCompiler", "jikes", "parse.regexp.DeclarationMarker", "!", "lang.section.all.Modifiers", "BlockIndenter", "jostraca.template.version", "0.1", "main.BackupPrefix", "-", "main.ShowDocumentation", "no", "main.TemplatePath", "e:\\web\\proj\\jostraca\\conf\\../templates/src", "jostraca.standard.TemplateExpressionTransforms", "ExpressionElementTransform, AppendNewLineTransform", "jostraca.standard.Directives", "SectionDirective, OneLineDirective, CollapseWhiteSpaceDirective", "jostraca.TemplateScript.CanonicalName.java", "java" };
        int numProperties = propertyList.length;
        for (int propI = 0; propI < numProperties; propI += 2) {
            writer__iProperties.put(propertyList[propI], propertyList[propI + 1]);
        }
        writer__iPropertiesInitialised = true;
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

    /** Get index of file currently being generated. */
    public int _getFileIndex() {
        return writer__iCurrentFileIndex;
    }

    /** Get number of generated files. */
    public int _getNumFiles() {
        return writer__iNumFiles;
    }

    /** Get current text of file currently being generated. */
    public String _getText() {
        return writer__iCurrentText.toString();
    }

    /** Set current text of file currently being generated. */
    public void _setText(String rText) {
        writer__iCurrentText = new StringBuffer(rText);
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

    /** Set to true if written files are to be saved to disk automatically.
  *  @param rSave True => Save written files to disk.
  */
    public void _save(boolean rSave) {
        writer__iSave = rSave;
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

    /** Set to true if written files are to be backed up to disk automatically.
  *  @param rBackup True => Backup files to disk.
  */
    public void _backup(boolean rBackup) {
        writer__iBackup = rBackup;
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

    /** Get command line arguments to CodeWriter. */
    public String[] _getArgs() {
        return writer__iArgs;
    }

    /** Get number of command line arguments to CodeWriter. */
    public int _getNumArgs() {
        return writer__iNumArgs;
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
}
