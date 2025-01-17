package piramide.multimodal.preprocessor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import piramide.multimodal.preprocessor.exceptions.EvaluationException;
import piramide.multimodal.preprocessor.exceptions.UserDefinedErrorException;

public class DirectoryPreprocessor {

    public static final String LANG_JAVA = "java";

    public static final String LANG_PYTHON = "python";

    public static final String LANG_CSHARD = "cshard";

    public static final String LANG_CPLUSPLUS = "cplusplus";

    public static final String LANG_VISUAL_BASIC = "vb";

    public static final String LANG_PERL = "perl";

    public static final String DIRECTIVE_START_SLASH = "//#";

    public static final String DIRECTIVE_START_SHARD = "#//";

    public static final String DIRECTIVE_START_APOST = "'//";

    private HashMap<String, String> languageDirectiveStart = new HashMap<String, String>();

    final Preprocessor preprocessor;

    final Map<String, Object> variables;

    public DirectoryPreprocessor(File variablesFile) throws IOException {
        this.preprocessor = new Preprocessor();
        this.variables = loadVariables(variablesFile);
        this.languageDirectiveStart.put(LANG_JAVA, DIRECTIVE_START_SLASH);
        this.languageDirectiveStart.put(LANG_CSHARD, DIRECTIVE_START_SLASH);
        this.languageDirectiveStart.put(LANG_CPLUSPLUS, DIRECTIVE_START_SLASH);
        this.languageDirectiveStart.put(LANG_VISUAL_BASIC, DIRECTIVE_START_APOST);
        this.languageDirectiveStart.put(LANG_PERL, DIRECTIVE_START_SHARD);
        this.languageDirectiveStart.put(LANG_PYTHON, DIRECTIVE_START_SHARD);
    }

    private Map<String, Object> loadVariables(File variablesFile) throws IOException {
        final String variablesText = FileUtils.readFileToString(variablesFile);
        final JSONObject variablesDictionary = (JSONObject) JSONValue.parse(variablesText);
        final Map<String, Object> returningMap = new HashMap<String, Object>();
        for (Object currentField : variablesDictionary.keySet()) {
            returningMap.put(currentField.toString(), variablesDictionary.get(currentField));
        }
        return returningMap;
    }

    public void preprocess(String inputDirectory, String outputDirectory, String extensionName, String codeLanguage) throws IOException, UserDefinedErrorException, EvaluationException {
        final File fInputDirectory = new File(inputDirectory);
        System.out.println(inputDirectory);
        final String[] files = fInputDirectory.list();
        for (String fileName : files) {
            final String fullCurrentFileName = inputDirectory + File.separator + fileName;
            final String fullCurrentOutputFileName = outputDirectory + File.separator + fileName;
            final File currentInputFile = new File(fullCurrentFileName);
            final File currentOutputFile = new File(fullCurrentOutputFileName);
            if (currentInputFile.isDirectory()) {
                currentOutputFile.mkdir();
                preprocess(fullCurrentFileName, fullCurrentOutputFileName, extensionName, codeLanguage);
            } else if (fullCurrentFileName.endsWith(extensionName)) {
                final String inputFileContent = FileUtils.readFileToString(currentInputFile);
                final String directiveStart = this.languageDirectiveStart.get(codeLanguage);
                final PreprocessorResult result = this.preprocessor.preprocess(fullCurrentFileName, directiveStart, inputFileContent, this.variables);
                final String outputFileContent = result.getPreprocessedCode();
                FileUtils.writeStringToFile(currentOutputFile, outputFileContent);
            } else {
                FileUtils.copyFile(currentInputFile, currentOutputFile);
            }
        }
    }
}
