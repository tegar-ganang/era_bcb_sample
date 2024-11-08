package net.sf.docbook_utils.maven.plugins.htmlcleaner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import net.sourceforge.argval.ArgumentValidation;
import net.sourceforge.argval.collection.CollectionUtil;
import net.sourceforge.argval.impl.ArgumentValidationImpl;
import net.sourceforge.argval.lang.SystemConstants;
import org.codehaus.plexus.util.DirectoryScanner;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlCleanerRunner {

    /** The default source file patterns are {@value} [old way {@code "*&#42;/*.html", "*&#42;/*.xhtml", "*&#42;/*.xml"} ]. */
    public static final String[] DEFAULT_FILES = new String[] { "**/*.html", "**/*.xhtml", "**/*.xml" };

    /** The HtmlCleaner default configuration file name. */
    public static final String DEFAULT_FILE_NAME_HTML_CLEANER_CONFIGURATION = "html-cleaner.properties";

    /** File extension HTML {@value}. */
    public static final String FILE_EXT_HTML = "html";

    /** File extension XHTML {@value}. */
    public static final String FILE_EXT_XHTML = "xhtml";

    /** pattern to match the suffix of the input filename (to replace with the OUTPUT_SUFFIX) */
    private static final String SUFFIX_PATTERN = "\\.[^.]*$";

    /** The logging instance. */
    protected static Logger logger = LoggerFactory.getLogger(HtmlCleanerRunner.class);

    /** The Html Cleaner instance. */
    private HtmlCleaner cleaner;

    /** The base working directory. */
    private File userDir;

    /** The Html Cleaner configuration. */
    private Properties htmlCleanerConfig;

    /** The replace extension Map, contains source extension and destination extension names. */
    private Map<String, String> replaceExtensionMap = new HashMap<String, String>();

    public HtmlCleanerRunner() {
        this(null);
    }

    public HtmlCleanerRunner(File userDir) {
        this(userDir, null);
    }

    public HtmlCleanerRunner(File userDir, Properties htmlCleanerConfig) {
        this(userDir, htmlCleanerConfig, null);
    }

    public HtmlCleanerRunner(File userDir, Properties htmlCleanerConfig, Map<String, String> replaceExtensionMap) {
        super();
        if (userDir == null) {
            userDir = new File(System.getProperty(SystemConstants.USER_DIR));
        }
        ArgumentValidation argVal = new ArgumentValidationImpl();
        argVal.isValidWhenDirectory("userDir", userDir);
        if (argVal.containsIllegalArgument()) throw argVal.createIllegalArgumentException();
        this.userDir = userDir;
        cleaner = new HtmlCleaner();
        setConfiguration(htmlCleanerConfig);
        this.replaceExtensionMap = new HashMap<String, String>();
        if (replaceExtensionMap != null) {
            for (Entry<String, String> entry : replaceExtensionMap.entrySet()) {
                this.replaceExtensionMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     *
     * @param userDir - Base directory, which can be the Java environment variable 'user.dir',
     *                  from which the process starts, but also the Maven 'baseDir' (project directory).
     * @param sourceDir - The directory from which the source (input) html files will be picked up.
     * @param destinationDir - The directory where the tidied (clena up) html files will be placed.
     * @param sourcePatternArray - The source file(s) pattern(s).
     * @param props - The HtmlCleaner properties (configuration of HtmlCleaner)
     */
    public void execute(File sourceDir, File destinationDir, String[] sourcePatternArray, Properties htmlCleanerConfig) {
        logger.debug("Using sourceDir '" + sourceDir + "'  destinationDir '" + destinationDir + "'.");
        List<String> expandWildCards = expandWildCards(sourceDir, sourcePatternArray);
        if (expandWildCards.size() == 0) {
            logger.warn("No source html files found for the given source-pattern(s).");
            logger.warn("Argument 'sourcePatternArray' value " + CollectionUtil.toString(sourcePatternArray));
        }
        for (String sourceFileName : expandWildCards) {
            final File sourceFile = new File(sourceDir, sourceFileName);
            String ouputFileName = sourceFileName;
            String extension = ouputFileName.substring(ouputFileName.lastIndexOf(".") + 1);
            logger.error("extension" + extension);
            if (replaceExtensionMap.containsKey(extension)) {
                logger.error("replace with " + replaceExtensionMap.get(extension));
                ouputFileName = ouputFileName.replaceAll(SUFFIX_PATTERN, "." + replaceExtensionMap.get(extension));
            }
            File outputFile = new File(destinationDir, ouputFileName);
            execute(sourceFile, outputFile, htmlCleanerConfig);
        }
    }

    public void execute(File sourceFile, File destinationFile, Properties htmlCleanerConfig) {
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
            execute(reader, writer, htmlCleanerConfig);
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

    public void execute(Reader reader, Writer writer, Properties htmlCleanerConfig) throws IOException {
        if (htmlCleanerConfig != null) {
            setConfiguration(htmlCleanerConfig);
        }
        TagNode node = cleaner.clean(reader);
        CleanerProperties cleanerProps = cleaner.getProperties();
        PrettyXmlSerializer xmlSerializer = new PrettyXmlSerializer(cleanerProps);
        xmlSerializer.writeXml(node, writer, "utf-8");
    }

    void setConfiguration(Properties htmlCleanerConfig) {
        if (htmlCleanerConfig != null && this.htmlCleanerConfig != htmlCleanerConfig) {
            this.htmlCleanerConfig = htmlCleanerConfig;
        } else {
            logger.info("No HtmlCleaner properties given, using default configuration.");
        }
        if (this.htmlCleanerConfig == null) {
            this.htmlCleanerConfig = new Properties();
        }
    }

    public void showConfiguration(StringWriter writer) {
        logger.error("Method  public void showConfiguration(StringWriter writer)  is not implemented.");
    }

    public Properties loadConfigurationFromFile(File fileHtmlCleanerConfig) {
        if (fileHtmlCleanerConfig == null) {
            fileHtmlCleanerConfig = getDefaultConfigurationFile();
            logger.info("Argument 'fileHtmlCleanerConfig' is null, using default configuration file '" + fileHtmlCleanerConfig + "'.");
        }
        if (!fileHtmlCleanerConfig.exists() || !fileHtmlCleanerConfig.isFile()) {
            logger.warn("Excepted configuration file '" + trimPath(userDir, fileHtmlCleanerConfig) + "'.");
        } else {
            try {
                Properties props = new Properties();
                props = loadProperties(fileHtmlCleanerConfig);
                return props;
            } catch (IOException ioe) {
                logger.error("Unable to load configuration file '" + trimPath(userDir, fileHtmlCleanerConfig) + "'.", ioe);
            }
        }
        return null;
    }

    public File getDefaultConfigurationFile() {
        File fileHtmlCleanerConfig;
        String fileNameHtmlCleanerConfig = DEFAULT_FILE_NAME_HTML_CLEANER_CONFIGURATION;
        File configDir = new File(userDir, "src/etc/html-cleaner");
        fileHtmlCleanerConfig = new File(configDir, fileNameHtmlCleanerConfig);
        return fileHtmlCleanerConfig;
    }

    public static Properties loadProperties(File fileHtmlCleanerConfig) throws IOException {
        Properties props = new Properties();
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(fileHtmlCleanerConfig);
            props.load(inStream);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe) {
                    logger.warn("Unable to close input stream, for file '" + fileHtmlCleanerConfig + "'", ioe);
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
