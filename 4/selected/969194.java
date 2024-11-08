package org.tm4j.topicmap.cmd;

import jargs.gnu.CmdLineParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tm4j.topicmap.TopicMap;
import org.tm4j.topicmap.TopicMapProvider;
import org.tm4j.topicmap.cmd.TopicMapList.ParseException;
import org.tm4j.topicmap.utils.TopicMapMerger;
import org.tm4j.topicmap.utils.TopicMapSerializer;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Implements a command line program to merge two or more topic maps.
 * The specified input files are merged using the rules defined in XTM 1.0.
 * The results of the merge are written to the specified output file, or to the
 * stdout stream if no output file is specified.
 *
 * @author <a href="mailto:kal_ahmed@users.sourceforge.net">Kal Ahmed</a>
 * @author <a href="mailto:haasfg@users.sourceforge.net">Florian G. Haas</a>
 */
public class Merge extends AppBase {

    private CmdLineParser.Option verboseOption;

    private CmdLineParser.Option outputFileOption;

    private CmdLineParser.Option includeDoctypeOption;

    private CmdLineParser.Option systemIdOption;

    private CmdLineParser.Option publicIdOption;

    private CmdLineParser.Option encodingOption;

    private CmdLineParser.Option indentationOption;

    private CmdLineParser.Option baseURIOption;

    private CmdLineParser.Option showUsageOption;

    private CmdLineParser.Option minimizeTopicsOption;

    /** An instance of the serialization helper class. */
    private TopicMapSerializer serializer = new TopicMapSerializer();

    /** A jakarta-commons log. */
    private Log logger = LogFactory.getLog(this.getClass());

    /**
     * Creates and initialises the Merge class with the specified
     * command-line arguments.
     */
    public Merge(String[] args) throws Exception {
        System.setProperty(TopicMapProvider.OPT_STATIC_MERGE, "true");
        initialise(args);
    }

    /**
     * The processing function of the merge process.
     * This function parses the command-line and then parses and merges each of
     * the topic maps specified on the command-line.
     * The results are written to the specified destination file or to stdout.
     */
    public void run(CmdLineParser argsParser) {
        Boolean showUsage = (Boolean) argsParser.getOptionValue(showUsageOption);
        if ((showUsage != null) && (showUsage.booleanValue())) {
            showUsage();
            return;
        }
        TopicMapMerger merger = getMerger();
        try {
            configureMerger(merger, argsParser);
            configureSerializer(serializer, argsParser);
            TopicMap tm = loadTopicMap(argsParser);
            serializer.setTopicMap(tm);
            serializer.serialize();
            serializer.closeOutputStream();
        } catch (FileNotFoundException fnfe) {
            System.err.println(fnfe.getMessage());
            showTryAgain();
        } catch (Exception e) {
            String msg = "Unexpected exception.";
            logger.fatal(msg, e);
        }
    }

    /**
     * Command-line entry point. Creates and runs the merge process.
     */
    public static void main(String[] args) {
        try {
            Merge app = new Merge(args);
        } catch (Exception e) {
            handleException(e);
        }
    }

    protected static void showTryAgain() {
        System.err.println("\nTry java " + Merge.class.getName() + " --help for more information.\n");
    }

    /**
     * Handles an exception by printing the associated detail message
     * to <code>System.err</code>.
     *
     * @param e the exception being reported.
     */
    protected static void handleException(Exception e) {
        System.err.println("\n" + e.getMessage());
        e.printStackTrace();
        showTryAgain();
    }

    /** Shows a usage message. */
    protected static void showUsage() {
        String usage = "\n" + "The Merge utility parses and merges one or more Topic Maps in XTM 1.0 syntax.\n" + "\n" + "Merge reads from standard input or the specified input file(s), and writes to\n" + "standard output or the specified output file." + "\n" + "\n" + "Usage: java org.tm4j.topicmap.cmd.Merge [OPTION]... [FILE [-b|--baseuri uri]...\n" + "\n" + "-o  --output-file      write to specified output file \n" + "                         (default: write to standard output)\n" + "-d  --include-doctype  include XTM 1.0 !DOCTYPE declaration in merged topic map\n" + "                         (default: don't)\n" + "-S  --system-id        use this SYSTEM identifier\n" + "-P  --public-id        use this PUBLIC identifier\n" + "-B  --output-base-uri  use this base URI if xml:base attribute is missing\n" + "-e  --encoding         use this output encoding\n" + "-I  --indentation      indent using this many spaces\n" + "-n  --nomerge          do not merge in topic maps referenced through \n" + "                         <mergeMap> or <topicRef> elements\n" + "-c  --consistent       make the topic map consistent, eliminating duplicate\n" + "                         names, occurrences and associations.\n" + "-m  --minimize-topics  use subjectIndicatorRef or resourceRef and avoid\n" + "                         writing a topic element where possible" + "-?  --help             print this message.\n" + "\n" + "Examples:\n" + "java org.tm4j.topicmap.cmd.Merge -o result.xtm -e ISO-8859-1 map1.xtm -b http://www.mycorp.com/topicmaps/map1.xtm map2.xtm\n" + "java org.tm4j.topicmap.cmd.Merge -o result.xtm -d -I 4 < map1.xtm\n" + "/usr/bin/somecoolxtmgenerator | java org.tm4j.topicmap.cmd.Merge -o result.xtm\n";
        System.err.println(usage);
    }

    /**
     * Registers arguments with command-line parser.
     *
     * @param argsParser
     */
    public void setupApplicationArgs(CmdLineParser argsParser) {
        verboseOption = argsParser.addBooleanOption('v', "verbose");
        outputFileOption = argsParser.addStringOption('o', "output-file");
        includeDoctypeOption = argsParser.addBooleanOption('d', "include-doctype");
        systemIdOption = argsParser.addStringOption('S', "system-id");
        publicIdOption = argsParser.addStringOption('P', "public-id");
        encodingOption = argsParser.addStringOption('e', "encoding");
        indentationOption = argsParser.addIntegerOption('I', "indentation");
        baseURIOption = argsParser.addStringOption('B', "output-base-uri");
        minimizeTopicsOption = argsParser.addBooleanOption('m', "minimize-topics");
        showUsageOption = argsParser.addBooleanOption('?', "help");
    }

    /**
     * Sets application-specific options on a {@link TopicMapMerger}.
     *
     * @param merger the merger to be configured
     * @param argsParser the command-line args parser used to read the
     * configuration options.
     */
    private void configureMerger(TopicMapMerger merger, CmdLineParser argsParser) throws ClassNotFoundException, ParseException {
        String baseURI = (String) argsParser.getOptionValue(baseURIOption);
        if (baseURI != null) {
            merger.setDefaultBaseUri(baseURI);
        }
    }

    /**
     * Sets application-specific options on a {@link TopicMapSerializer}.
     *
     * @param serializer the serializer to be configured
     * @param argsParser the command-line args parser used to read the
     * configuration options.
     */
    private void configureSerializer(TopicMapSerializer serializer, CmdLineParser argsParser) throws IOException {
        String outputFileName = (String) argsParser.getOptionValue(outputFileOption);
        if (outputFileName == null) {
            serializer.setOutputStream(System.out);
        } else {
            serializer.setOutputFile(outputFileName);
        }
        Integer indentation = (Integer) argsParser.getOptionValue(indentationOption);
        if (indentation != null) {
            serializer.setIndentation(indentation.intValue());
        }
        String encoding = (String) argsParser.getOptionValue(encodingOption);
        if (encoding != null) {
            serializer.setEncoding(encoding);
        }
        Boolean includeDoctye = (Boolean) argsParser.getOptionValue(includeDoctypeOption);
        if (includeDoctye != null) {
            serializer.setIncludeDoctype(includeDoctye.booleanValue());
        }
        String publicId = (String) argsParser.getOptionValue(publicIdOption);
        String systemId = (String) argsParser.getOptionValue(systemIdOption);
        if (publicId != null) {
            serializer.setPublicId(publicId);
        }
        if (systemId != null) {
            serializer.setSystemId(systemId);
        }
        Boolean minimizeTopics = (Boolean) argsParser.getOptionValue(minimizeTopicsOption);
        if (minimizeTopics != null) {
            serializer.setWriteStubs(!minimizeTopics.booleanValue());
        }
    }
}
