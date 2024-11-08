package barde;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;
import cbonar.env.Option;
import barde.DuFu;
import barde.log.LogReader;
import barde.log.Message;
import barde.t4c.T4CClientReader;
import barde.t4c.T4CClientWriter;
import barde.t4c.T4CLogGenerator;
import barde.writers.LogWriter;

/**
 * A benchmark for Barde.<br>
 * TODO : currently rewriting it with ConsoleApplication
 * <ul>
 * <li>generate a HTML page of the rapport
 * <li>archive the generated log in a jar if the -keep arg is given
 * <li>print the classes used : the LogReader, the LogView, ...
 * <li>print more stats
 * <li>allow to specify more parameters, like the number of views, their type, ...
 * <li>use the adequate {@link LogWriter}s to generate the temp. log files
 * </ul>
 * Currently, it provides :
 * <ul>
 * <li>the total number of messages
 * <li>the total number of channels
 * <li>the total number of avatars (sources)
 * <li>the amount of memory used
 * <li>the time taken to generate the random log file
 * <li>the time taken to read the log file
 * </ul>
 * @author cbonar
 */
public class Benchmark extends DuFu {

    public static final int EXIT_OK = 0;

    public static final int ERROR_GEN = 1;

    public static final int ERROR_RUN = 2;

    /** option to specify the number of messages to generate */
    protected static Option OPTION_GEN = new Option("-+gen").addArg("");

    /** option to specify the log file to use for testing */
    protected static Option OPTION_LOG = new Option("-+log").addArg("");

    /** number of messages to generate */
    private long logsize = -1;

    /** filename of the log to read */
    private String logfile = null;

    public Benchmark() {
        addOption(OPTION_LOG);
        addOption(OPTION_GEN);
    }

    /**
	 * Creates a temporary file and calls {@link #generateLog(OutputStream)} on it.
	 * @return the path to the created log file, or null if no file was created
	 * @throws ParseException if an error happened during the generation
	 */
    public File generateLog() throws IOException, ParseException {
        try {
            String filename = null;
            File tmpDir = new File(System.getProperty("java.io.tmpdir", "."));
            File tmpFile = File.createTempFile(getClass().getName(), ".log", tmpDir);
            OutputStream os = new FileOutputStream(tmpFile);
            System.out.println("! Creating temporary log file : " + tmpFile.getAbsolutePath() + ".");
            System.out.print("! Generating log...");
            Date start = new Date();
            generateLog(os);
            Date end = new Date();
            System.out.println(" done in " + (double) (end.getTime() - start.getTime()) / 1000 + " seconds.");
            return tmpFile;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("! Couldn't create temporary file.");
            System.exit(ERROR_GEN);
            return null;
        }
    }

    /**
	 * Generates a full log file, with random messages.
	 * @throws ParseException if an error happened during the generation
	 */
    public void generateLog(OutputStream os) throws IOException, ParseException {
        LogWriter writer = new T4CClientWriter(os, this.conf);
        LogReader reader = new T4CLogGenerator(this.logsize, this.conf);
        for (Message message = reader.read(); message != null; message = reader.read()) writer.write(message);
        writer.close();
    }

    /**
	 * @see cbonar.env.ConsoleApplication#handleArgs(List<String>))
	 */
    protected Option handleArgs(List<String> args) {
        if (OPTION_GEN.matches(args)) {
            try {
                this.logsize = Long.parseLong(args.get(1));
            } catch (NumberFormatException nfe) {
                logger.severe("Not a valid long : " + args.get(1));
            }
            return OPTION_GEN;
        } else if (OPTION_LOG.matches(args)) {
            this.logfile = args.get(1);
            if (args.get(1).toLowerCase().matches("-+(gen)")) {
                try {
                    this.logsize = Long.parseLong(args.get(2));
                    return OPTION_LOG;
                } catch (NumberFormatException nfe) {
                    logger.severe("Not a valid Long : " + args.get(2));
                    return null;
                }
            }
            return OPTION_LOG;
        } else return super.handleArgs(args);
    }

    /** runs the benchmark */
    private void run() {
        System.out.println("! Statistics for '" + this.logfile + "'");
        System.out.println();
        try {
            ArrayList<IntCounter> channels = new ArrayList<IntCounter>();
            ArrayList<IntCounter> avatars = new ArrayList<IntCounter>();
            Runtime rt = Runtime.getRuntime();
            LogReader reader = new T4CClientReader(new FileInputStream(this.logfile), this.conf);
            List<Message> messages = Collections.synchronizedList(new ArrayList<Message>());
            long nMessages = 0;
            System.out.println("! Messages : [total number , used memory in KB]");
            System.out.println("! -------------");
            Date start = new Date();
            for (Message next = reader.read(); next != null; next = reader.read()) {
                if (nMessages % 1024 == 0) System.out.println(nMessages + "\t" + (rt.totalMemory() - rt.freeMemory()) / 1000);
                IntCounter newChanCount = new IntCounter(next.getChannel(), 1);
                int chanIndex = channels.indexOf(newChanCount);
                if (chanIndex >= 0) ((IntCounter) channels.get(chanIndex)).incCount(1); else channels.add(newChanCount);
                IntCounter newAvCount = new IntCounter(next.getAvatar(), 1);
                int avIndex = avatars.indexOf(newAvCount);
                if (avIndex >= 0) ((IntCounter) avatars.get(avIndex)).incCount(1); else avatars.add(newAvCount);
                messages.add(next);
                nMessages++;
            }
            Date end = new Date();
            System.out.println("! " + nMessages + " messages ; used memory : " + (rt.totalMemory() - rt.freeMemory()) / 1000 + "K ; read in " + (double) (end.getTime() - start.getTime()) / 1000 + " seconds.");
            TreeSet sortedChannels = new TreeSet();
            for (Iterator cit = channels.iterator(); cit.hasNext(); ) sortedChannels.add(cit.next());
            System.out.println();
            System.out.println("! Channels (total : " + channels.size() + ") : [name , instances]");
            System.out.println("! -------------");
            long channols = 0;
            for (Iterator cit = sortedChannels.iterator(); cit.hasNext(); ) {
                IntCounter chan = (IntCounter) cit.next();
                channols += chan.getCount();
                System.out.println(chan.getName() + "\t\t\t" + chan.getCount());
            }
            System.out.println("! Total number of instances -> " + channols);
            TreeSet sortedAvatars = new TreeSet();
            for (Iterator avit = avatars.iterator(); avit.hasNext(); ) sortedAvatars.add(avit.next());
            System.out.println();
            System.out.println("! Avatars (total : " + avatars.size() + ") : [name , instances]");
            System.out.println("! -------------");
            long avators = 0;
            for (Iterator avit = sortedAvatars.iterator(); avit.hasNext(); ) {
                IntCounter avat = (IntCounter) avit.next();
                avators += avat.getCount();
                System.out.println(avat.getName() + "\t\t\t" + avat.getCount());
            }
            System.out.println("! Total number of instances -> " + avators);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ERROR_RUN);
        }
    }

    /**
	 * NOTE : CLASSPATH must contain the path to the resource bundles (*.properties)
	 * @param args
	 */
    public static void main(String[] args) {
        Benchmark bmark = new Benchmark();
        bmark.conf = ResourceBundle.getBundle("barde_t4c", Locale.getDefault());
        bmark.parseArgs(args);
        if (bmark.logsize > 0) {
            try {
                if (bmark.logfile == null) bmark.generateLog(System.out); else {
                    File tmpFile = bmark.generateLog();
                    bmark.logfile = tmpFile.getAbsolutePath();
                    bmark.run();
                    tmpFile.deleteOnExit();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(ERROR_GEN);
            }
        } else if (bmark.logfile != null) bmark.run(); else System.err.println(bmark.usage("Missing input file."));
        System.exit(EXIT_OK);
    }
}

class IntCounter implements Comparable {

    String name;

    int count;

    /**
	 * @param name	name of this counter
	 * @param count	initial count
	 */
    public IntCounter(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return this.name;
    }

    public int getCount() {
        return this.count;
    }

    public void incCount(int increment) {
        this.count += increment;
    }

    public int compareTo(Object o) {
        IntCounter ico = (IntCounter) o;
        int intCompared = this.count - ico.count;
        if (intCompared == 0) return -this.name.compareTo(ico.getName()); else return -intCompared;
    }

    public boolean equals(Object obj) {
        return ((IntCounter) obj).getName().equals(this.name);
    }
}
