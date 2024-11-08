package it.crs4.seal.common;

import it.crs4.seal.common.ClusterUtils;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SealToolParser {

    public static final File DefaultConfigFile = new File(System.getProperty("user.home"), ".sealrc");

    public static final int DEFAULT_MIN_REDUCE_TASKS = 0;

    private int minReduceTasks;

    /**
	 * Configuration object used to parse the command line, cached for further queries.
	 */
    private Configuration myconf;

    private Options options;

    private Option opt_nReduceTasks;

    private Option opt_configFileOverride;

    private Integer nReduceTasks;

    private String configSection;

    protected ArrayList<Path> inputs;

    private Path outputDir;

    /**
	 * Construct a SealToolParser instance.
	 *
	 * The instance is set to read the properties in configuration file's section sectionName,
	 * in addition to the default section.  Properties set on the command line will override
	 * the file's settings.  In addition to the standard command line options implemented by
	 * SealToolParser, you can add new ones by providing them in moreOpts.
	 *
	 * @param configSection Name of section of configuration to load, in addition to DEFAULT.
	 * If null, only DEFAULT is loaded
	 *
	 * @param moreOpts Additional options to parse from command line.
	 */
    public SealToolParser(String configSection, Options moreOpts) {
        options = new Options();
        opt_nReduceTasks = OptionBuilder.withDescription("Number of reduce tasks to use.").hasArg().withArgName("INT").withLongOpt("num-reducers").create("r");
        options.addOption(opt_nReduceTasks);
        opt_configFileOverride = OptionBuilder.withDescription("Override default Seal config file (" + DefaultConfigFile + ")").hasArg().withArgName("FILE").withLongOpt("seal-config").create("sc");
        options.addOption(opt_configFileOverride);
        if (moreOpts != null) {
            for (Object opt : moreOpts.getOptions()) options.addOption((Option) opt);
        }
        nReduceTasks = null;
        inputs = new ArrayList<Path>(10);
        outputDir = null;
        this.configSection = (configSection == null) ? "" : configSection;
        minReduceTasks = DEFAULT_MIN_REDUCE_TASKS;
        myconf = null;
    }

    /**
	 * Set the minimum acceptable number of reduce tasks.
	 * If a user specifies a number lower than this limit parseOptions will raise
	 * an error.
	 */
    public void setMinReduceTasks(int x) {
        if (x < 0) throw new IllegalArgumentException("minimum number of reduce tasks must be >= 0");
        minReduceTasks = x;
    }

    public int getMinReduceTasks() {
        return minReduceTasks;
    }

    protected void loadConfig(Configuration conf, File fname) throws ParseException, IOException {
        ConfigFileParser parser = new ConfigFileParser();
        try {
            parser.load(new FileReader(fname));
            Iterator<ConfigFileParser.KvPair> it = parser.getSectionIterator(configSection);
            ConfigFileParser.KvPair pair;
            while (it.hasNext()) {
                pair = it.next();
                conf.set(pair.getKey(), pair.getValue());
            }
        } catch (FormatException e) {
            throw new ParseException("Error reading config file " + fname + ". " + e);
        }
    }

    /**
	 * Decides whether to use an rc file, and if so which one.
	 *
	 * This method is necessary only because we'd like the user to be able to override the default
	 * location of the seal configuration file ($HOME/.sealrc).  So, it scans
	 * the command line arguments looking for a user-specified seal configuration file.
	 * If one is specified, it verifies that it exists and is readable.  If none is specified
	 * it checks to see whether a configuration file is available at the default location,
	 * and if it is the method verifies that it is readable.
	 *
	 * If a config file is found and is readable, its path is returned as a File object.  On the other
	 * hand, if a config file isn't found the method returns null.
	 *
	 * @param args command line arguments
	 * @exception ParseException raise if the file specified on the cmd line doesn't exist or isn't readable.
	 */
    protected File getRcFile(String[] args) throws ParseException {
        File fname = null;
        String shortOpt = "--" + opt_configFileOverride.getOpt();
        String longOpt = "--" + opt_configFileOverride.getLongOpt();
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals(shortOpt) || args[i].equals(longOpt)) {
                if (i + 1 >= args.length) throw new ParseException("Missing file argument to " + args[i]);
                fname = new File(args[i + 1]);
                break;
            }
        }
        if (fname != null) {
            if (!fname.exists()) throw new ParseException("Configuration file " + fname + " doesn't exist");
            if (!fname.canRead()) throw new ParseException("Can't read configuration file " + fname);
        } else {
            if (DefaultConfigFile.exists()) {
                if (DefaultConfigFile.canRead()) fname = DefaultConfigFile; else {
                    LogFactory.getLog(SealToolParser.class).warn("Seal configuration file " + DefaultConfigFile + " isn't readable");
                }
            }
        }
        return fname;
    }

    /**
	 * Set properties useful for the whole Seal suite.
	 */
    protected void setDefaultProperties(Configuration conf) {
        conf.set("mapred.compress.map.output", "true");
    }

    public CommandLine parseOptions(Configuration conf, String[] args) throws ParseException, IOException {
        myconf = conf;
        setDefaultProperties(conf);
        File configFile = getRcFile(args);
        if (configFile != null) loadConfig(conf, configFile);
        CommandLine line = new GenericOptionsParser(conf, options, args).getCommandLine();
        if (line == null) throw new ParseException("Error parsing command line");
        if (line.hasOption(opt_nReduceTasks.getOpt())) {
            String rString = line.getOptionValue(opt_nReduceTasks.getOpt());
            try {
                int r = Integer.parseInt(rString);
                if (r >= minReduceTasks) nReduceTasks = r; else throw new ParseException("Number of reducers must be greater than or equal to " + minReduceTasks + " (got " + rString + ")");
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid number of reduce tasks '" + rString + "'");
            }
        }
        String[] otherArgs = line.getArgs();
        if (otherArgs.length < 2) throw new ParseException("You must provide input and output paths"); else {
            FileSystem fs;
            for (int i = 0; i < otherArgs.length - 1; ++i) {
                Path p = new Path(otherArgs[i]);
                fs = p.getFileSystem(conf);
                p = p.makeQualified(fs);
                FileStatus[] files = fs.globStatus(p);
                if (files != null && files.length > 0) {
                    for (FileStatus status : files) inputs.add(status.getPath());
                } else throw new ParseException("Input path " + p.toString() + " doesn't exist");
            }
            outputDir = new Path(otherArgs[otherArgs.length - 1]);
            fs = outputDir.getFileSystem(conf);
            outputDir = outputDir.makeQualified(fs);
            if (fs.exists(outputDir)) throw new ParseException("Output path " + outputDir.toString() + " already exists.  Won't overwrite");
        }
        return line;
    }

    /**
	 * Get total number of reduce tasks to run.
	 * This option parser must have already parsed the command line.
	 */
    public int getNReduceTasks(int defaultPerNode) throws java.io.IOException {
        if (myconf == null) throw new IllegalStateException("getNReduceTasks called before parsing the command line.");
        if (defaultPerNode < 0) throw new IllegalArgumentException("Invalid number of default reduce tasks per node: " + defaultPerNode);
        if (nReduceTasks == null) {
            nReduceTasks = ClusterUtils.getNumberTaskTrackers(myconf) * defaultPerNode;
            return nReduceTasks;
        } else return nReduceTasks;
    }

    /**
	 * Return the specified output path.
	 */
    public Path getOutputPath() {
        return outputDir;
    }

    /**
	 * An iterable list of Path items.
	 * Allows you to use the foreach loop, as in:
	 * <code>
	 * for (Path p: parser.getInputPaths())
	 *   System.out.println(p.toString());
	 * </code>
	 */
    public static class InputPathList implements Iterable<Path> {

        private Iterator<Path> it;

        public InputPathList(Iterator<Path> i) {
            it = i;
        }

        public Iterator<Path> iterator() {
            return it;
        }
    }

    public InputPathList getInputPaths() {
        return new InputPathList(inputs.iterator());
    }

    public int getNumInputPaths() {
        return inputs.size();
    }

    public void defaultUsageError(String toolName) {
        defaultUsageError(toolName, null);
    }

    /**
	 * Prints help and exits with code 3.
	 */
    public void defaultUsageError(String toolName, String msg) {
        System.err.print("Usage error");
        if (msg != null) System.err.println(":  " + msg);
        System.err.print("\n");
        System.setOut(System.err);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("hadoop " + toolName + " [options] <in>+ <out>", options);
        System.exit(3);
    }
}
