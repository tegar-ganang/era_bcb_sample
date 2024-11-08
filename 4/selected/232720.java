package jather;

import java.net.URL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.ChannelException;

/**
 * The Main executive command line class.
 * 
 * @author neil
 * 
 */
public class MainExecutive {

    private static final Log log = LogFactory.getLog(MainExecutive.class);

    private Executive executive;

    /**
	 * Print the usage.
	 * 
	 * @param options
	 *            the options to print.
	 * @param code
	 *            the exit code.
	 */
    public static void usage(Options options, int code) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(MainExecutive.class.getName(), options);
        System.exit(code);
    }

    /**
	 * The command line entry point.
	 * 
	 * @param args
	 *            the command line arguments.
	 */
    @SuppressWarnings("static-access")
    public static final void main(String[] args) throws Exception {
        MainExecutive exec = new MainExecutive();
        Options options = new Options();
        options.addOption("h", false, "Show this help.");
        Option channelName = OptionBuilder.withArgName("name").hasArg().withDescription("connect to the channel name").create("n");
        options.addOption(channelName);
        Option slots = OptionBuilder.withArgName("int").hasArg().withDescription("the number or executive slots to provide").create("s");
        options.addOption(slots);
        Option maxSize = OptionBuilder.withArgName("int").hasArg().withDescription("the maximum cluster size to operate in").create("m");
        options.addOption(slots);
        Option props = OptionBuilder.withArgName("props").hasArg().withDescription("the channel props string").create("p");
        options.addOption(props);
        Option url = OptionBuilder.withArgName("url").hasArg().withDescription("the channel props url").create("u");
        options.addOption(url);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption('h')) {
            usage(options, 0);
        }
        if (cmd.hasOption('n')) {
            exec.getExecutive().setClusterName(cmd.getOptionValue('n'));
        }
        if (cmd.hasOption('s')) {
            exec.getExecutive().setSlotCount(Integer.parseInt(cmd.getOptionValue('s')));
        }
        if (cmd.hasOption('p')) {
            exec.getExecutive().setChannelStringProps(cmd.getOptionValue('p'));
        }
        if (cmd.hasOption('m')) {
            exec.getExecutive().setMaxGroupSize(Integer.parseInt(cmd.getOptionValue('m')));
        }
        if (cmd.hasOption('u')) {
            exec.getExecutive().setChannelUrlProps(new URL(cmd.getOptionValue('u')));
        }
        log.info("Using slots:" + exec.getExecutive().getSlotCount());
        log.info("Using max group size:" + exec.getExecutive().getMaxGroupSize());
        log.info("Using channel name:" + exec.getExecutive().getClusterName());
        log.info("Using channel props:" + exec.getExecutive().getChannelStringProps());
        log.info("Using channel URL props:" + exec.getExecutive().getChannelUrlProps());
        exec.run();
    }

    /**
	 * The main application execution method.
	 */
    public void run() throws ChannelException {
        getExecutive().start();
    }

    /**
	 * The local executive.
	 * 
	 * @return the executive
	 */
    protected Executive getExecutive() {
        if (executive == null) {
            executive = new Executive();
        }
        return executive;
    }

    /**
	 * The local executive.
	 * 
	 * @param executive
	 *            the executive to set
	 */
    protected void setExecutive(Executive executive) {
        this.executive = executive;
    }
}
