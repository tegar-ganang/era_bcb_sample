package net.sf.docbook_utils.maven.plugins.java2html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.sourceforge.argval.ArgumentValidation;
import net.sourceforge.argval.collection.CollectionUtil;
import net.sourceforge.argval.impl.ArgumentValidationImpl;
import net.sourceforge.argval.lang.SystemConstants;
import org.codehaus.plexus.util.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.java2html.converter.IJavaSourceConverter;
import de.java2html.converter.JavaSourceConverterProvider;
import de.java2html.javasource.JavaSource;
import de.java2html.javasource.JavaSourceParser;
import de.java2html.options.JavaSourceConversionOptions;

public class Java2HtmlRunner {

    /** The default source file patterns are {@value}. */
    public static final String[] DEFAULT_FILES = new String[] { "**/*.java" };

    /** The Java2Html default configuration file name {@value}. */
    public static final String DEFAULT_FILE_NAME_JAVA2HTML_CONFIGURATION = "java2html.properties";

    private static Logger logger = LoggerFactory.getLogger(Java2HtmlRunner.class);

    /** pattern to match the suffix of the input filename (to replace with the OUTPUT_SUFFIX) */
    private static final String SUFFIX_PATTERN = "\\.[^.]*$";

    private final File userDir;

    private final SortedSet<String> conversionSet;

    private Properties java2HtmlConfig;

    /** The replace extension Map, contains source extension and destination extension names. */
    private Map<String, String> replaceExtensionMap = new HashMap<String, String>();

    public Java2HtmlRunner(File userDir, Properties java2HtmlConfig, Map<String, String> replaceExtensionMap) {
        super();
        if (userDir == null) {
            userDir = new File(System.getProperty(SystemConstants.USER_DIR));
        }
        ArgumentValidation argVal = new ArgumentValidationImpl();
        argVal.isValidWhenDirectory("userDir", userDir);
        if (argVal.containsIllegalArgument()) throw argVal.createIllegalArgumentException();
        this.userDir = userDir;
        setConfiguration(java2HtmlConfig);
        this.replaceExtensionMap = new HashMap<String, String>();
        if (replaceExtensionMap != null) {
            for (Entry<String, String> entry : replaceExtensionMap.entrySet()) {
                this.replaceExtensionMap.put(entry.getKey(), entry.getValue());
            }
        }
        conversionSet = new TreeSet<String>();
        for (String converterName : JavaSourceConverterProvider.getAllConverterNames()) {
            conversionSet.add(converterName);
        }
    }

    public void execute(File sourceDir, File destinationDir, String[] sourcePatternArray, String conversionType, Properties java2HtmlConfig) {
        logger.debug("Using sourceDir '" + sourceDir + "'  destinationDir '" + destinationDir + "'.");
        List<String> expandWildCards = expandWildCards(sourceDir, sourcePatternArray);
        if (expandWildCards.size() == 0) {
            logger.warn("No source (Java) files found for the given source-pattern(s).");
            logger.warn("Argument 'sourcePatternArray' value " + CollectionUtil.toString(sourcePatternArray));
        }
        for (String sourceFileName : expandWildCards) {
            final File sourceFile = new File(sourceDir, sourceFileName);
            String ouputFileName = sourceFileName;
            logger.error(" suffix split '" + CollectionUtil.toString(ouputFileName.split(SUFFIX_PATTERN)) + "'");
            String extension = ouputFileName.substring(ouputFileName.lastIndexOf(".") + 1);
            logger.error("extension " + extension);
            if (replaceExtensionMap.containsKey(extension)) {
                logger.error("replace with " + replaceExtensionMap.get(extension));
                ouputFileName = ouputFileName.replaceAll(SUFFIX_PATTERN, "." + replaceExtensionMap.get(extension));
            } else {
                logger.error("TODO implement some default, based on 'conversionType', extension...");
            }
            File outputFile = new File(destinationDir, ouputFileName);
            execute(sourceFile, outputFile, conversionType, java2HtmlConfig);
        }
    }

    public void execute(File sourceFile, File destinationFile, String conversionType, Properties java2HtmlConfig) {
        FileReader reader = null;
        Writer writer = null;
        try {
            reader = new FileReader(sourceFile);
            logger.info("Using source file: " + trimPath(userDir, sourceFile));
            if (!destinationFile.getParentFile().exists()) {
                createDirectory(destinationFile.getParentFile());
            }
            writer = new FileWriter(destinationFile);
            logger.info("Destination file:  " + trimPath(userDir, destinationFile));
            execute(reader, writer, conversionType, java2HtmlConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    writer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void execute(Reader reader, Writer writer, String conversionType, Properties java2HtmlConfig) throws IOException {
        JavaSource javaSource = null;
        javaSource = new JavaSourceParser().parse(reader);
        ArgumentValidation argVal = new ArgumentValidationImpl();
        argVal.isValidWhenInSet("conversionType", conversionType, conversionSet);
        if (argVal.containsIllegalArgument()) throw argVal.createIllegalArgumentException();
        IJavaSourceConverter converter = JavaSourceConverterProvider.getJavaSourceConverterByName(conversionType);
        converter.convert(javaSource, getConversionOption(), writer);
    }

    void setConfiguration(Properties java2HtmlConfig) {
        if (java2HtmlConfig != null && this.java2HtmlConfig != java2HtmlConfig) {
            this.java2HtmlConfig = java2HtmlConfig;
        } else {
            logger.info("No HTML to Java properties given, using default configuration.");
        }
        if (this.java2HtmlConfig == null) {
            this.java2HtmlConfig = new Properties();
        }
    }

    private JavaSourceConversionOptions getConversionOption() {
        return JavaSourceConversionOptions.getDefault();
    }

    public File getDefaultConfigurationFile() {
        File fileJava2HtmlConfig;
        String fileNameJava2HtmlConfig = DEFAULT_FILE_NAME_JAVA2HTML_CONFIGURATION;
        File configDir = new File(userDir, "src/etc/java2html");
        fileJava2HtmlConfig = new File(configDir, fileNameJava2HtmlConfig);
        return fileJava2HtmlConfig;
    }

    public static Properties loadProperties(File fileJtidyConfig) throws IOException {
        Properties props = new Properties();
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(fileJtidyConfig);
            props.load(inStream);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe) {
                    logger.warn("Unable to close input stream, for file '" + fileJtidyConfig + "'", ioe);
                } finally {
                    inStream = null;
                }
            }
        }
        return props;
    }

    /**
     * Expands the given {@code wildCardFiles} argument into a List of Strings, containing
     * all the files located from the {@code baseDir}, which full fill the wild card requirements.
     * <pre>
     * filename.ext
     * directory/filename.ext
     * directory/*.ext
     * directory/directory/*.ext
     * directory/*&#42;/*.ext
     * *&#42;/*.ext
     * </pre>
     *
     * @param wildCardFiles - array of wild card strings
     * @return the list of files that apply to the given wild card array
     */
    public static List<String> expandWildCards(File baseDir, String[] wildCardFiles) {
        if (baseDir == null || (!baseDir.isDirectory()) || (!baseDir.exists())) {
            throw new IllegalArgumentException("Argument 'baseDir', should be an existing directory.");
        }
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        scanner.setIncludes(wildCardFiles);
        scanner.scan();
        return Arrays.asList(scanner.getIncludedFiles());
    }

    /**
     * Create a directory, if it not already exists.
     *
     * @param dir - the directory to create
     * @throws IOException  in case the creation fails
     */
    static void createDirectory(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory '" + dir + "'.");
        }
    }

    /**
     * Trim the prefixDir from the given file.
     * <p>
     * Example:<br />
     * prefixDir: {@code /home/user/workspace/project}<br />
     * file: {@code /home/user/workspace/project/src/docbkx/manual/chapter/table.csv}<br />
     * returns: {@code src/docbkx/manual/chapter/table.csv}
     *
     * @param prefixDir - the prefix path to remove
     * @param file - the file, which gets trimmed
     * @return the file as {@code String} without the
     */
    static String trimPath(File prefixDir, File file) {
        return file.getPath().replace(prefixDir.getPath() + File.separator, "");
    }
}
