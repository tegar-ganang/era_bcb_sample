package net.sf.qifcon.commandline;

import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.qifcon.quicken.QuickenLexer;
import net.sf.qifcon.quicken.QuickenParser;
import net.sf.qifcon.quicken.QuickenTransformer;
import net.sf.qifcon.quicken.XmlTreeParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xalan.xslt.EnvironmentCheck;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * QifCon Main class.
 *
 * @author <a href="mailto:baerrach@gmail.com">Barrie Treloar</a>
 * @version 0.8.0
 */
public final class Main {

    /**
     * Default encoding to use for files.
     */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * Usage command syntax.
     */
    private static final String USAGE_COMMAND_SYNTAX = "qifcon <options> transformation_file1 ... transformation_filen";

    /**
     * Usage HelpFormatter padding for description.
     */
    private static final int USAGE_DESCRIPTION_PAD = 3;

    /**
     * Usage HelpFormatter footer.
     */
    private static final String USAGE_FOOTER = null;

    /**
     * Usage HelpFormatter header.
     */
    private static final String USAGE_HEADER = null;

    /**
     * Usage HelpFormatter left padding.
     */
    private static final int USAGE_LEFTPAD = 1;

    /**
     * Usage HelpFormatter max characters to display per line.
     */
    private static final int USAGE_MAX_CHARACTERS_TO_DISPLAY = 80;

    /**
     * QifCon Version. TODO: Replace this with value from pom
     */
    private static final String VERSION = "0.11-SNAPSHOT";

    /**
     * Debug longopt.
     */
    static final String OPTION_DEBUG_LONGOPT = "debug";

    /**
     * Debug shortopt.
     */
    static final String OPTION_DEBUG_SHORTOPT = "d";

    /**
     * Encoding longopt
     */
    static final String OPTION_ENCODING_LONGOPT = "encoding";

    /**
     * Help longopt.
     */
    static final String OPTION_HELP_LONGOPT = "help";

    /**
     * Help shortopt.
     */
    static final String OPTION_HELP_SHORTOPT = "h";

    /**
     * Input longopt.
     */
    static final String OPTION_INPUT_LONGOPT = "input";

    /**
     * Input shortopt.
     */
    static final String OPTION_INPUT_SHORTOPT = "i";

    /**
     * Output longopt.
     */
    static final String OPTION_OUTPUT_LONGOPT = "output";

    /**
     * Output shortopt.
     */
    static final String OPTION_OUTPUT_SHORTOPT = "o";

    /**
     * Verbose Output shortopt
     */
    static final String OPTION_VERBOSE_OUTPUT_SHORTOPT = "X";

    /**
     * Verbose Output longopt
     */
    static final String OPTION_VERBOSE_OUTPUT_LONGOPT = "verbose";

    /**
     * Version longopt.
     */
    static final String OPTION_VERSION_LONGOPT = "version";

    /**
     * Version shortopt.
     */
    static final String OPTION_VERSION_SHORTOPT = "v";

    /**
     * The encoding of input and output files, defaults to "ISO-8859-1"
     */
    private String encoding = DEFAULT_ENCODING;

    /**
     * @return the Encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Creates and configures Main object from the command line.
     *
     * @param args
     *            command line arguments
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.doMain(args);
    }

    /**
     * Whether to debug transformation.
     */
    private boolean debug;

    /**
     * Input Resource to transform.
     */
    private Resource input;

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Non-running options, for help and version.
     */
    private Options nonRunningOptions = new Options();

    /**
     * Normal options.
     */
    private Options options = new Options();

    /**
     * Output resource to write results of transformation to.
     */
    private Resource output;

    /**
     * Flag to indicate whether help should be displayed.
     */
    private boolean printHelp;

    /**
     * Flag to indicate whether version details should be displayed.
     */
    private boolean printVersion;

    /**
     * The transformation resources to apply to the input file.
     */
    private Resource[] transformationFiles;

    /**
     * Create an instance of the QifCon console program.
     */
    public Main() {
        setupOptions();
    }

    /**
     * Run the main method of this class.
     *
     * @param args
     *            the command line arguments.
     */
    public void doMain(String[] args) {
        try {
            initialise(args);
            if (printHelp) {
                System.out.println(getUsage());
                return;
            }
            if (printVersion) {
                System.out.println(getVersion());
                return;
            }
        } catch (Exception e) {
            System.err.println(ClassUtils.getShortClassName(e.getClass()) + ": " + e.getMessage());
            System.err.println(getUsage());
            logger.debug(e.getMessage(), e);
            return;
        }
        try {
            run();
        } catch (Exception e) {
            logger.fatal("Failed to convert.", e);
        }
    }

    /**
     * @return the inputFile
     */
    public Resource getInput() {
        return input;
    }

    /**
     * @return the outputFile
     */
    public Resource getOutput() {
        return output;
    }

    /**
     * @return the transformationFiles
     */
    public Resource[] getTransformationFiles() {
        return transformationFiles;
    }

    /**
     * Inititalize the program with the provided command line arguments. If a non running option is found then parsing
     * will stop without initialising any other options.
     *
     * @param args
     *            the command line arguments
     * @throws ParseException
     *             if the command line arguments could not be parsed correctly
     */
    public void initialise(String[] args) throws ParseException {
        logger.debug("Command Line: " + Arrays.toString(args));
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        line = parser.parse(nonRunningOptions, args, true);
        if (line.hasOption(OPTION_HELP_SHORTOPT)) {
            printHelp = true;
            return;
        }
        if (line.hasOption(OPTION_VERSION_SHORTOPT)) {
            printVersion = true;
            return;
        }
        line = parser.parse(options, args);
        if (line.hasOption(OPTION_DEBUG_SHORTOPT)) {
            setDebug(true);
        }
        if (line.hasOption(OPTION_VERBOSE_OUTPUT_SHORTOPT)) {
            Logger.getRootLogger().setLevel(Level.DEBUG);
        }
        if (line.hasOption(OPTION_INPUT_SHORTOPT)) {
            String filename = line.getOptionValue(OPTION_INPUT_SHORTOPT);
            Resource resource = new FileSystemResource(filename);
            if (!resource.exists()) {
                throw new MissingArgumentException("Input file does not exist: " + filename);
            }
            setInput(resource);
        }
        if (line.hasOption(OPTION_OUTPUT_SHORTOPT)) {
            setOutput(new FileSystemResource(line.getOptionValue(OPTION_OUTPUT_SHORTOPT)));
        }
        if (line.hasOption(OPTION_ENCODING_LONGOPT)) {
            setEncoding(line.getOptionValue(OPTION_ENCODING_LONGOPT));
        }
        String[] arguments = line.getArgs();
        if (arguments.length == 0) {
            throw new MissingArgumentException("Error: No transformation files specified.");
        }
        List<Resource> files = new ArrayList<Resource>(arguments.length);
        for (String argument : arguments) {
            Resource transformationFile = new FileSystemResource(argument);
            if (!transformationFile.exists()) {
                transformationFile = new ClassPathResource(argument);
                if (!transformationFile.exists()) {
                    throw new MissingArgumentException("Error: Transformation file does not exist: " + argument);
                }
            }
            files.add(transformationFile);
        }
        setTransformationFiles(files.toArray(new Resource[0]));
    }

    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @return the printHelp
     */
    public boolean isPrintHelp() {
        return printHelp;
    }

    /**
     * @return the printVersion
     */
    public boolean isPrintVersion() {
        return printVersion;
    }

    /**
     * Execute the transformations.
     *
     * @throws Exception
     *             transformation failures.
     */
    public void run() throws Exception {
        DataInputStream inputStream = null;
        inputStream = new DataInputStream(input.getInputStream());
        QuickenLexer lexer = new QuickenLexer(inputStream);
        QuickenParser parser = new QuickenParser(lexer);
        parser.quickenInterchangeFormat();
        XmlTreeParser xmlTreeParser = new XmlTreeParser();
        String transformationResults = xmlTreeParser.quickenInterchangeFormat(parser.getAST());
        for (Resource transformationFile : transformationFiles) {
            QuickenTransformer transformer = new QuickenTransformer();
            transformer.setEncoding(getEncoding());
            transformer.setDebug(isDebug());
            transformer.setName(transformationFile.getFilename());
            transformer.setTransformation(transformationFile);
            transformationResults = transformer.transform(transformationResults);
        }
        FileUtils.writeStringToFile(output.getFile(), transformationResults, getEncoding());
        logger.info("Transformation complete. Results available at: " + output.getFile().getPath());
    }

    /**
     * @param shouldDebug
     *            the debug to set
     */
    public void setDebug(boolean shouldDebug) {
        this.debug = shouldDebug;
    }

    /**
     * @param encoding
     *            the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @param inputFile
     *            the inputFile to set
     */
    public void setInput(Resource inputFile) {
        this.input = inputFile;
    }

    /**
     * @param outputFile
     *            the outputFile to set
     */
    public void setOutput(Resource outputFile) {
        this.output = outputFile;
    }

    /**
     * @param shouldPrintHelp
     *            the printHelp to set
     */
    public void setPrintHelp(boolean shouldPrintHelp) {
        this.printHelp = shouldPrintHelp;
    }

    /**
     * @param shouldPrintVersion
     *            the printVersion to set
     */
    public void setPrintVersion(boolean shouldPrintVersion) {
        this.printVersion = shouldPrintVersion;
    }

    /**
     * @param theTransformationFiles
     *            the transformationFiles to set
     */
    public void setTransformationFiles(Resource[] theTransformationFiles) {
        this.transformationFiles = theTransformationFiles;
    }

    /**
     * @return usage information.
     */
    private String getUsage() {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter w = new StringWriter();
        formatter.printHelp(new PrintWriter(w), USAGE_MAX_CHARACTERS_TO_DISPLAY, USAGE_COMMAND_SYNTAX, USAGE_HEADER, options, USAGE_LEFTPAD, USAGE_DESCRIPTION_PAD, USAGE_FOOTER);
        return w.toString();
    }

    /**
     * @return version information as per http://www.gnu.org/prep/standards/html_node/Command_002dLine-Interfaces.html
     */
    private String getVersion() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.append("QIF Converter " + VERSION + "\n");
        (new EnvironmentCheck()).checkEnvironment(printWriter);
        return stringWriter.getBuffer().toString();
    }

    /**
     * Setup the options handling.
     *
     */
    @SuppressWarnings("static-access")
    private void setupOptions() {
        Option help = OptionBuilder.withLongOpt(OPTION_HELP_LONGOPT).withDescription("Display usage information").create(OPTION_HELP_SHORTOPT);
        options.addOption(help);
        nonRunningOptions.addOption(help);
        Option version = OptionBuilder.withLongOpt(OPTION_VERSION_LONGOPT).withDescription("Display version information").create(OPTION_VERSION_SHORTOPT);
        options.addOption(version);
        nonRunningOptions.addOption(version);
        options.addOption(OptionBuilder.withLongOpt(OPTION_DEBUG_LONGOPT).withDescription("Enable debugging of XML and XSL files during transformation").create(OPTION_DEBUG_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_VERBOSE_OUTPUT_LONGOPT).withDescription("Enable verbose logging output of execution plan").create(OPTION_VERBOSE_OUTPUT_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_INPUT_LONGOPT).withArgName("file").hasArg().withDescription("QIF input file to convert from").isRequired().create(OPTION_INPUT_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_OUTPUT_LONGOPT).withArgName("file").hasArg().withDescription("Output file name to write to").isRequired().create(OPTION_OUTPUT_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_ENCODING_LONGOPT).withArgName("encoding").hasArg().withDescription("Encoding of files to read and write").create());
    }
}
