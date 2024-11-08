package org.edits;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.edits.models.EDITSModel;

/**
 * @author Milen Kouylekov
 */
public class Edits {

    public static final String COMMAND = "command";

    public static final String COST_SCHEME = "scheme";

    public static final String DISTANCE_ALGORITHM = "algorithm";

    public static final String EDITS_MODEL = "model";

    public static final String EDITS_PATH = "EDITS_PATH";

    private static final String EDITS_TEMP_PATH = "EDITS_TEMP_PATH";

    public static final String ENGINE_OPTIMIZER = "optimizer";

    public static final String ENTAILMENT_ENGINE = "engine";

    public static final String GENERIC = "generic";

    public static final String MATCHER = "matcher";

    private static EditsRegistry registry;

    public static final String RULES_EXTRACTOR = "extractor";

    public static final String RULES_REPOSITORY = "repository";

    private static String systemPath;

    private static String tempdir;

    public static final String TEXT_ANONOTATOR = "annotator";

    public static final String VERSION = "3.0";

    public static final String WEIGHT_CALCULATOR = "weight";

    public Edits(CommandExecutorHanlder handler) throws Exception {
        this(System.getenv(EDITS_PATH), System.getenv(EDITS_TEMP_PATH), handler);
    }

    public Edits(String path, String working, CommandExecutorHanlder handler) throws Exception {
        if (path == null) throw new Exception("$EDITS_PATH is not specified!");
        systemPath = new File(path).getCanonicalPath() + "/";
        if (working == null) working = System.getProperty("user.home") + "/.edits/";
        FileTools.checkDirectory(systemPath);
        File x = new File(working);
        if (!x.exists()) x.mkdir();
        tempdir = new File(working).getCanonicalPath() + "/tmp/";
        x = new File(tempdir);
        if (x.exists()) EDITSModel.delete(x);
        x.mkdir();
        setVerbose(false);
        registry = new EditsRegistry(systemPath, handler);
        registry.load();
    }

    public String typeToTitle(String type) {
        StringBuilder buff = new StringBuilder();
        StringTokenizer toker = new StringTokenizer(type, "-", false);
        while (toker.hasMoreTokens()) {
            String token = toker.nextToken();
            token = token.substring(0, 1).toUpperCase() + token.substring(1) + " ";
            buff.append(token);
        }
        return buff.toString();
    }

    public static Logger defaultLogger() {
        Logger logger = Logger.getLogger("main");
        return logger;
    }

    /**
	 * Converts a stack trace into a string.
	 * 
	 * @param aThrowable
	 * @return
	 */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    public static void handle(String[] args, CommandExecutorHanlder handler) throws Exception {
        if (args.length == 0) {
            Edits.printEditsInfo(false);
            System.exit(0);
        }
        CommandLine line = null;
        try {
            line = new BasicParser().parse(registry.shellOptionsOne(), args);
        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n");
            return;
        }
        long start = System.currentTimeMillis();
        handler.execute(line);
        double end = System.currentTimeMillis();
        end = end - start;
        String s = " milliseconds.";
        if (end > 1000) {
            end = end / 1000;
            s = " seconds.";
            if (end > 60) {
                end = end / 60;
                s = " minutes.";
                if (end > 60) {
                    end = end / 60;
                    s = " hours.";
                }
            }
        }
        end = Math.round(end * 100.0) / 100.0;
        System.out.println("Execution time was " + end + s);
    }

    public static void main(String[] args) {
        try {
            CommandExecutorHanlder handler = (CommandExecutorHanlder) Class.forName(args[0]).newInstance();
            String[] nargs = new String[args.length - 1];
            for (int i = 0; i < args.length - 1; i++) nargs[i] = args[i + 1];
            new Edits(handler);
            handle(nargs, handler);
        } catch (Exception e) {
            e.printStackTrace();
            Logger logger = Logger.getLogger("edits.main");
            logger.debug(e.getMessage(), e);
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static String path() {
        return systemPath;
    }

    public static void printEditsInfo(boolean longo) throws Exception {
        List<Options> options = Edits.registry().shellOptions();
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        HelpFormatter formatter = new HelpFormatter();
        printWriter.println("EDITS - Edit Distance Textual Entailment Suite - " + VERSION);
        String descriptionFile = Edits.path() + "share/descriptions/description.txt";
        if (new File(descriptionFile).exists()) {
            printWriter.println("");
            String s = FileTools.loadString(descriptionFile);
            formatter.printWrapped(printWriter, 120, s);
        }
        printWriter.println("");
        formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "edits", options.get(0));
        printWriter.println("\nDefault Options");
        formatter.printOptions(printWriter, 120, options.get(1), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
        if (longo) {
            printWriter.println("\nModule Options");
            formatter.printOptions(printWriter, 120, options.get(2), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
            printWriter.println("\nModule Defintions");
            formatter.printOptions(printWriter, 120, options.get(3), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
            printWriter.println("\nModule Aliases");
            formatter.printOptions(printWriter, 120, options.get(4), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
        }
        System.out.println(result.toString());
    }

    public static EditsRegistry registry() {
        return registry;
    }

    public static void setVerbose(boolean verbose) {
        System.out.println(path() + "share/log4j/log4j-verbose.properties");
        if (verbose) PropertyConfigurator.configure(path() + "share/log4j/log4j-verbose.properties"); else PropertyConfigurator.configure(path() + "share/log4j/log4j.properties");
    }

    public static String tempdir() throws Exception {
        return tempdir;
    }
}
