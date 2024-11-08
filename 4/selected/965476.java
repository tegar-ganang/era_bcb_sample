package jist.runtime;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import jargs.gnu.*;
import org.apache.log4j.*;

/** 
 * Primary entry-point into the JIST system. Performs cmd-line parsing, and 
 * general initialisation of the simulation system.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Main.java,v 1.1 2006/10/21 00:03:53 lmottola Exp $
 * @since JIST1.0
 */
public final class Main {

    /** 
   * JiST version.
   */
    public static final String VERSION = "1.0.0";

    /**
   * debugging: whether to keep event traces (some overhead).
   */
    public static final boolean EVENT_TRACE = false;

    /** 
   * debugging: event causality trace depth. 
   * (0=no cut-off, beware: will accumulate everything) 
   */
    public static final int EVENT_TRACE_DEPTH = 5;

    /**
   * debugging: whether to compute event locations (high overhead).
   */
    public static final boolean EVENT_LOCATION = false;

    /**
   * Whether rewritten classes should be cached.
   */
    public static final boolean REWRITE_CACHE = true;

    /**
   * Whether certain checks should be turned on. (Java 1.3.x compatibility)
   */
    public static final boolean ASSERT = false;

    /**
   * Whether to count up events of each type.
   */
    public static final boolean COUNT_EVENTS = false;

    /**
   * Whether to assume a single controller.
   */
    public static final boolean SINGLE_CONTROLLER = true;

    /**
   * Event window size of GUI log.
   */
    public static final int GUILOG_SIZE = 0;

    /**
   * Interval between JiST controller progress output.
   */
    public static final long CONTROLLER_DISPLAY_INTERVAL = 10 * 1000;

    /**
   * Interval between JiST server progress output to job queue.
   */
    public static final long SERVER_DISPLAY_INTERVAL = 60 * 1000;

    /**
   * Interval between JiST server pings to client.
   */
    public static final long CLIENT_PING_INTERVAL = 60 * 1000;

    /**
   * Interval for queue to check that server is still alive.
   */
    public static final long SERVER_QUEUE_RELEASE_INTERVAL = 5 * 60 * 1000;

    /** Default jist server (RMI) port. */
    public static final int JIST_PORT = 3000;

    /** Default jist properties file name. */
    public static final String JIST_PROPERTIES = "jist.properties";

    /**
   * Display JiST version information.
   */
    public static void showVersion() {
        System.out.println("JiST v" + VERSION + ", Java in Simulation Time Runtime.");
        System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
        System.out.println();
    }

    /**
   * Display JiST syntax help.
   */
    public static void showUsage() {
        System.out.println("Usage: jist [-r host[:port]] [switches] <sim>   <-- engine mode");
        System.out.println("       jist -S [-p port] [-q] [-r host:[port]]  <-- server modes");
        System.out.println("       jist -v | -h");
        System.out.println();
        System.out.println("  -h, --help      display this help information");
        System.out.println("  -v, --version   display version information");
        System.out.println();
        System.out.println("engine:");
        System.out.println("  -c, --conf        specify properties file [jist.properties]");
        System.out.println("  -l, --logger      specify simulation logger class");
        System.out.println("  --bsh             run input with BeanShell script engine");
        System.out.println("  --jpy             run input with Jython script engine");
        System.out.println("  --nocache         disable rewriter cache");
        System.out.println("  -r, --remote      specify remote job or processing server");
        System.out.println("  where: ");
        System.out.println("    <sim>  is:      simulation program with command-line arguments, or");
        System.out.println("                    simulation script with command-line arguments");
        System.out.println("                       ('--' implies interactive shell)");
        System.out.println("server:");
        System.out.println("  -S, --server      jist server mode");
        System.out.println("  -p, --port        listen for jobs on given port [3000]");
        System.out.println("  -q, --queue       act only as job queue server, do not process");
        System.out.println("  -r, --remote      process jobs from remote queue");
        System.out.println("  -x, --proxy       perform RMI connections via a proxy");
        System.out.println();
    }

    /**
   * Parsed JiST command-line options.
   */
    public static class CommandLineOptions implements Serializable {

        /** help option. */
        public boolean help = false;

        /** version option. */
        public boolean version = false;

        /** simulation program name. */
        public String sim = null;

        /** properties file. */
        public String properties = null;

        /** custom logger class name. */
        public String logger = null;

        /** beanshell mode. */
        public boolean bsh = false;

        /** jython mode. */
        public boolean jpy = false;

        /** do not use rewriter cache. */
        public boolean nocache = false;

        /** remote server job queue. */
        public Node remote = null;

        /** server mode. */
        public boolean server = false;

        /** listen port. */
        public int port = 0;

        /** job queue mode. */
        public boolean queue = false;

        /** command-line parameters to simulation program. */
        public String[] args = new String[0];

        /** rmi proxy point. */
        public Node proxy = null;
    }

    /**
   * Parse command-line arguments.
   *
   * @param args command-line arguments
   * @return command-line options structure
   * @throws CmdLineParser.OptionException illegal command line option
   * @throws java.net.UnknownHostException unable to parse remote host address:port
   */
    private static CommandLineOptions parseCommandLineOptions(String[] args) throws CmdLineParser.OptionException, java.net.UnknownHostException {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");
        CmdLineParser.Option opt_properties = parser.addStringOption('c', "conf");
        CmdLineParser.Option opt_logger = parser.addStringOption('l', "logger");
        CmdLineParser.Option opt_bsh = parser.addBooleanOption('.', "bsh");
        CmdLineParser.Option opt_jpy = parser.addBooleanOption(',', "jpy");
        CmdLineParser.Option opt_nocache = parser.addBooleanOption(']', "nocache");
        CmdLineParser.Option opt_remote = parser.addStringOption('r', "remote");
        CmdLineParser.Option opt_server = parser.addBooleanOption('S', "server");
        CmdLineParser.Option opt_port = parser.addStringOption('p', "port");
        CmdLineParser.Option opt_queue = parser.addBooleanOption('q', "queue");
        CmdLineParser.Option opt_proxy = parser.addStringOption('x', "proxy");
        parser.parse(args);
        CommandLineOptions options = new CommandLineOptions();
        if (parser.getOptionValue(opt_help) != null) {
            options.help = true;
        }
        if (parser.getOptionValue(opt_version) != null) {
            options.version = true;
        }
        if (parser.getOptionValue(opt_properties) != null) {
            options.properties = (String) parser.getOptionValue(opt_properties);
        }
        if (parser.getOptionValue(opt_logger) != null) {
            options.logger = (String) parser.getOptionValue(opt_logger);
        }
        if (parser.getOptionValue(opt_bsh) != null) {
            options.bsh = true;
        }
        if (parser.getOptionValue(opt_jpy) != null) {
            options.jpy = true;
        }
        if (parser.getOptionValue(opt_nocache) != null) {
            options.nocache = true;
        }
        if (parser.getOptionValue(opt_remote) != null) {
            options.remote = Node.parse((String) parser.getOptionValue(opt_remote), JIST_PORT);
        }
        if (parser.getOptionValue(opt_server) != null) {
            options.server = true;
        }
        if (parser.getOptionValue(opt_port) != null) {
            options.port = Integer.parseInt((String) parser.getOptionValue(opt_port));
        }
        if (parser.getOptionValue(opt_queue) != null) {
            options.queue = true;
        }
        if (parser.getOptionValue(opt_proxy) != null) {
            options.proxy = Node.parse((String) parser.getOptionValue(opt_proxy), ProxyPoint.PROXY_PORT);
        }
        String[] rest = parser.getRemainingArgs();
        if (rest.length > 0) {
            options.sim = rest[0];
            options.args = new String[rest.length - 1];
            System.arraycopy(rest, 1, options.args, 0, options.args.length);
        }
        return options;
    }

    /** whether jist is running. */
    private static boolean running = false;

    /** current job. */
    private static RemoteJist.Job currentJob = null;

    /**
   * Return whether JiST is running.
   *
   * @return whether JiST is running
   */
    public static boolean isRunning() {
        return running;
    }

    /**
   * Run a single simulation with given command-line options.
   *
   * @param options command-line options
   * @param properties jist properties
   * @param remote jist client stub
   * @param serverOut local server output stream; null for local execution
   * @param ping remote client ping object, or null if local
   */
    public static void runSimulation(CommandLineOptions options, Properties properties, RemoteJist.JistClientRemote remote, PrintStream serverOut, RemoteJist.PingRemote ping) {
        try {
            if (properties != null) {
                Logger.getRootLogger().setLevel(Level.OFF);
                PropertyConfigurator.configure(properties);
            } else {
                BasicConfigurator.configure();
                Logger.getRootLogger().setLevel(Level.OFF);
            }
            if (options.bsh || options.jpy || options.sim != null) {
                String cachedir = options.nocache ? null : System.getProperty("java.io.tmpdir");
                Rewriter rewriter = new Rewriter(null, cachedir, remote, serverOut);
                Thread.currentThread().setContextClassLoader(rewriter);
                Controller controller = Controller.newController(rewriter);
                if (options.bsh) {
                    Bootstrap.create(JistAPI.RUN_BSH, controller, options.sim, options.args, null);
                } else if (options.jpy) {
                    Bootstrap.create(JistAPI.RUN_JPY, controller, options.sim, options.args, null);
                } else if (options.sim != null) {
                    Bootstrap.create(JistAPI.RUN_CLASS, controller, options.sim, options.args, null);
                }
                if (options.logger != null) {
                    controller.setLog(Class.forName(options.logger, true, rewriter));
                }
                try {
                    controller.start();
                    Thread t = startClientPingThread(ping, controller);
                    controller.join();
                    if (t != null) {
                        t.interrupt();
                        while (t.isAlive()) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } finally {
                    Throwable t = controller.reset();
                    if (t != null) {
                        if (t instanceof VirtualMachineError) {
                            throw (VirtualMachineError) t;
                        } else {
                            t.printStackTrace();
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Simulation class not found: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Redirect output streams and run simulation.
   *
   * @param options command-line options
   * @param properties jist properties
   * @param remote jist client stub
   */
    public static void runSimulationRedirect(CommandLineOptions options, Properties properties, RemoteJist.JistClientRemote remote) {
        PrintStream lout = System.out, lerr = System.err;
        try {
            PrintStream rout = null, rerr = null;
            rout = new RemoteIO.PrintStreamWithExceptions(new PrintStream(new RemoteIO.RemoteOutputStream(remote.getStdOut())));
            rerr = new RemoteIO.PrintStreamWithExceptions(new PrintStream(new RemoteIO.RemoteOutputStream(remote.getStdErr())));
            System.setOut(rout);
            System.setErr(rerr);
            try {
                try {
                    runSimulation(options, properties, remote, lout, remote);
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (JistException e) {
                    e.printStackTrace();
                }
                if (rerr != null) rerr.flush();
                if (rout != null) rout.flush();
            } finally {
                System.setOut(lout);
                System.setErr(lerr);
            }
            if (rerr != null) rerr.close();
            if (rout != null) rout.close();
        } catch (Exception e) {
            lout.println("client output connection failure!");
        }
    }

    /**
   * Run a JiST client.
   *
   * @param options command-line options
   * @throws MalformedURLException never
   * @throws NotBoundException remote JiST server not initialized
   * @throws RemoteException rpc failure
   */
    public static void runClient(CommandLineOptions options) throws MalformedURLException, NotBoundException, RemoteException {
        if (options.properties == null) {
            options.properties = JIST_PROPERTIES;
        }
        Properties properties = null;
        try {
            File f = new File(options.properties);
            FileInputStream fin = new FileInputStream(f);
            properties = new Properties();
            properties.load(fin);
            fin.close();
        } catch (IOException e) {
            properties = null;
        }
        if (options.remote != null) {
            RemoteJist.JobQueueServerRemote jqs = RemoteJist.JobQueueServer.getRemote(options.remote);
            RemoteJist.JistClient client = new RemoteJist.JistClient();
            RemoteJist.Job job = new RemoteJist.Job();
            options.remote = null;
            job.options = options;
            job.properties = properties;
            job.client = client;
            jqs.addJob(job, false);
            try {
                synchronized (client) {
                    client.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            RemoteJist.JistClientRemote client = new RemoteJist.JistClientLocal();
            runSimulation(options, properties, client, null, null);
        }
    }

    /**
   * Try to force a flush of all memory from previous simulation.
   *
   * @throws InterruptedException local server simulation thread interuptted
   */
    public static void recycleMem() throws InterruptedException {
        final long baseThreshold = 20 * 1024 * 1024;
        final long iterThreshold = 1 * 1024 * 1024;
        final int iter = 5;
        final int pause = 2000;
        System.gc();
        System.runFinalization();
        Thread.sleep(pause);
        for (int i = 0; i < iter && Util.getUsedMemory() > iterThreshold; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(pause);
        }
        if (Util.getUsedMemory() > baseThreshold) {
            System.out.println("Houston, we have a memory problem!");
            System.out.println("Memory used = " + Util.getUsedMemory() + " bytes.");
        }
    }

    /**
   * Dequeue and process jobs from queue.
   *
   * @param jqs (remote) job queue
   * @throws RemoteException rpc failure
   * @throws InterruptedException local server simulation thread interuptted
   */
    public static void jobPump(RemoteJist.JobQueueServerRemote jqs) throws RemoteException, InterruptedException {
        final String waitMsg = "** Waiting for simulation... ";
        final String execMsg = "** Executing simulation: ";
        long maxmem = Runtime.getRuntime().maxMemory();
        System.out.println(waitMsg);
        Thread display = startDisplayThread(jqs);
        while (true) {
            jqs.waitForJob(maxmem);
            currentJob = jqs.getJob(maxmem);
            if (currentJob == null) continue;
            try {
                currentJob.client.ping();
                System.out.println(execMsg + currentJob);
                try {
                    runSimulationRedirect(currentJob.options, currentJob.properties, currentJob.client);
                    currentJob.client.done();
                } catch (RemoteException e) {
                    throw e;
                } catch (OutOfMemoryError e) {
                    System.out.println("out of memory!");
                    currentJob.mem = maxmem;
                    jqs.addJob(currentJob, true);
                } catch (Throwable t) {
                    System.out.println("UNHANDLED SIMULATION PROCESSING EXCEPTION AT SERVER:");
                    t.printStackTrace(System.out);
                }
            } catch (RemoteException e) {
                System.out.println("client control connection failure!");
            }
            recycleMem();
            currentJob = null;
            System.out.println(waitMsg);
        }
    }

    /**
   * Start server display thread to report simulation status to server queue.
   *
   * @param jqs remote job queue server
   * @return display thread
   */
    public static Thread startDisplayThread(final RemoteJist.JobQueueServerRemote jqs) {
        Runnable runner = new Runnable() {

            public void run() {
                synchronized (this) {
                    this.notify();
                }
                try {
                    String host = (new Node(1)).getHostString();
                    RemoteIO.RemoteOutputStreamRemote jqsOut = jqs.getStdOut();
                    while (isRunning()) {
                        Controller c = Controller.getActiveController();
                        String msg = host + ":";
                        if (c.isRunning()) {
                            long memused = Util.getUsedMemory();
                            long seconds = (long) ((System.currentTimeMillis() - c.getStartTime()) / 1000.0);
                            msg += " mem=" + (memused / 1024 / 1024) + "M";
                            msg += " t=" + Util.getHMS(seconds);
                            msg += " sim-time=" + c.getSimulationTimeString();
                            msg += "\n  " + currentJob + "\n";
                            jqsOut.write(msg.getBytes());
                        }
                        try {
                            Thread.sleep(SERVER_DISPLAY_INTERVAL);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (RemoteException e) {
                } catch (IOException e) {
                }
            }
        };
        Thread t = new Thread(runner);
        t.setDaemon(true);
        synchronized (runner) {
            t.start();
            try {
                runner.wait();
            } catch (InterruptedException e) {
            }
        }
        return t;
    }

    /**
   * Initiate a server thread to ping client, and abort simulation if client dies.
   *
   * @param ping remote ping object, or null if local
   * @param controller controller running simulation
   * @return ping thread, or null if ping object is null
   */
    public static Thread startClientPingThread(final RemoteJist.PingRemote ping, final Controller controller) {
        if (ping == null) return null;
        Runnable runner = new Runnable() {

            public void run() {
                synchronized (this) {
                    this.notify();
                }
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        ping.ping();
                        Thread.sleep(CLIENT_PING_INTERVAL);
                    }
                } catch (InterruptedException e) {
                } catch (RemoteException e) {
                    controller.endAt(0);
                }
            }
        };
        Thread t = new Thread(runner);
        t.setDaemon(true);
        synchronized (runner) {
            t.start();
            try {
                runner.wait();
            } catch (InterruptedException e) {
            }
        }
        return t;
    }

    /**
   * Run a JiST server.
   *
   * @param host server name
   * @param options command-line options
   * @throws MalformedURLException never
   * @throws NotBoundException remote JiST server not initialized
   * @throws AlreadyBoundException unable to initialize local server
   * @throws RemoteException rpc failure
   * @throws InterruptedException local server simulation thread interuptted
   */
    public static void runServer(String host, CommandLineOptions options) throws MalformedURLException, NotBoundException, AlreadyBoundException, RemoteException, InterruptedException {
        if (options.remote == null && options.port == 0) {
            options.port = JIST_PORT;
        }
        if (options.port != 0) {
            Registry r = LocateRegistry.createRegistry(options.port);
            RemoteJist.JobQueueServer jqs = new RemoteJist.JobQueueServer(options.port, System.out);
            r.bind(RemoteJist.JobQueueServer.JIST_JOBSERVER_RMI_NAME, jqs);
            System.out.println("Listening for simulations on " + host + ":" + options.port + "...");
            if (!options.queue) {
                jobPump(jqs);
            }
        }
        if (options.remote != null) {
            final int minpause = 5 * 1000, maxpause = 120 * 1000;
            int pause = minpause;
            while (true) {
                try {
                    RemoteJist.JobQueueServerRemote jqs = RemoteJist.JobQueueServer.getRemote(options.remote);
                    pause = minpause;
                    jobPump(jqs);
                } catch (RemoteException e) {
                    System.out.println("Connection to queue server failed! Will try to reconnect in " + (pause / 1000) + " seconds.");
                    Thread.sleep(pause);
                    pause = Math.min(maxpause, pause * 2);
                }
            }
        }
    }

    /**
   * JiST command-line entry point.
   *
   * @param args command-line arguments
   */
    public static void main(String[] args) {
        running = true;
        try {
            CommandLineOptions options = parseCommandLineOptions(args);
            boolean script = options.bsh || options.jpy;
            if (options.help || (options.sim == null && !script && !options.server)) {
                showVersion();
                showUsage();
                return;
            }
            if (options.version) {
                showVersion();
                return;
            }
            if (options.server) {
                if (options.properties != null) {
                    System.out.println("invalid server mode option '-c'; type 'jist -h' for syntax");
                    return;
                }
                if (options.logger != null) {
                    System.out.println("invalid server mode option '-l'; type 'jist -h' for syntax");
                    return;
                }
                if (script) {
                    System.out.println("invalid server mode option '--bsh' or '--jpy'; type 'jist -h' for syntax");
                    return;
                }
                if (options.nocache) {
                    System.out.println("invalid server mode option '--nocache'; type 'jist -h' for syntax");
                    return;
                }
                if (options.sim != null) {
                    System.out.println("can not provide simulation program to server mode; type 'jist -h' for syntax");
                }
                if (options.remote != null && options.port != 0) {
                    System.out.println("server should have either a local or remote job queue; type 'jist -h' for syntax");
                    return;
                }
            } else {
                if (options.port != 0) {
                    System.out.println("invalid client mode option '-p'; type 'jist -h' for syntax");
                    return;
                }
                if (options.queue) {
                    System.out.println("invalid client mode option '-q'; type 'jist -h' for syntax");
                    return;
                }
                if (options.remote != null && options.sim == null) {
                    System.out.println("client should have job to queue; type 'jist -h' for syntax");
                    return;
                }
            }
            if (options.proxy != null) {
                ProxyPoint.setRmiProxy(options.proxy.getHost(), options.proxy.getPort());
            }
            if (options.server) {
                String host = (new Node(1)).getHostString();
                System.setProperty("java.rmi.server.hostname", host);
                showVersion();
                runServer(host, options);
            } else {
                runClient(options);
            }
        } catch (CmdLineParser.OptionException e) {
            System.out.println("Error parsing command line: " + e.getMessage());
        } catch (java.net.UnknownHostException e) {
            System.out.println("Unknown host: " + e.getMessage());
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
        } catch (MalformedURLException e) {
            System.out.println("Bad URL: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (NotBoundException e) {
            System.out.println("Not bound: " + e.getMessage());
        } catch (AlreadyBoundException e) {
            System.out.println("Not bound: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted: " + e.getMessage());
        } finally {
            running = false;
        }
    }
}
