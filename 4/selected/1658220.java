package net.sf.qifcon.csv2qif;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.log4j.Logger;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.event.report.HtmlReportGenerator;
import org.milyn.payload.StringResult;
import org.milyn.payload.StringSource;
import org.milyn.resource.URIResourceLocator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * QifCon CSV2QIF Main class.
 *
 * @author <a href="mailto:baerrach@gmail.com">Barrie Treloar</a>
 * @version 0.9.0
 */
public final class Main {

    /**
     * Default encoding to use for files.
     */
    public static final String DEFAULT_ENCODING = "ISO-8859-1";

    /**
     * Usage command syntax.
     */
    private static final String USAGE_COMMAND_SYNTAX = "csv2qif <options>";

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
     * QifCon csv2qif Version. TODO: Replace this with value from pom
     */
    private static final String VERSION = "0.9-SNAPSHOT";

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
        if (arguments.length != 0) {
            throw new UnrecognizedOptionException("Error: Unknown extra arguments: " + Arrays.asList(arguments));
        }
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
        StringResult result = null;
        Source inputStream = new StreamSource(input.getInputStream());
        String[] pipeline = new String[] { "smooks-bank-account-csv.xml", "smooks-bank-account-csv-to-qifxml.xml", "smooks-bank-account-qifxml-to-qif.xml" };
        for (String configuration : pipeline) {
            URIResourceLocator uriResourceLocator = new URIResourceLocator();
            InputStream configurationAsInputStream = uriResourceLocator.getResource(configuration);
            Smooks smooks = new Smooks();
            result = new StringResult();
            try {
                smooks.addConfigurations(configurationAsInputStream);
                ExecutionContext executionContext = smooks.createExecutionContext();
                if (isDebug()) {
                    File reportFile = new File(output.getFile().getParent(), output.getFile().getName() + configuration + "-report.html");
                    executionContext.setEventListener(new HtmlReportGenerator(reportFile.getPath()));
                }
                smooks.filterSource(executionContext, inputStream, result);
            } finally {
                smooks.close();
            }
            inputStream = new StringSource(result.getResult());
        }
        FileUtils.writeStringToFile(output.getFile(), result.getResult(), getEncoding());
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
        return "QIF Converter " + VERSION;
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
        options.addOption(OptionBuilder.withLongOpt(OPTION_INPUT_LONGOPT).withArgName("file").hasArg().withDescription("CSV input file to convert from").isRequired().create(OPTION_INPUT_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_OUTPUT_LONGOPT).withArgName("file").hasArg().withDescription("Output file name to write to").isRequired().create(OPTION_OUTPUT_SHORTOPT));
        options.addOption(OptionBuilder.withLongOpt(OPTION_ENCODING_LONGOPT).withArgName("encoding").hasArg().withDescription("Encoding of files to read and write").create());
    }
}
