package jmxsh;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

class ConnectCmd implements Command {

    private static final ConnectCmd instance = new ConnectCmd();

    static ConnectCmd getInstance() {
        return instance;
    }

    /** Initialize private variables.  Setup the command-line options. */
    private ConnectCmd() {
        this.opts = new Options();
        this.opts.addOption(OptionBuilder.withLongOpt("server").withDescription("Server to connect to (in the form of an URL).").withArgName("SERVER").hasArg().create("s"));
        this.opts.addOption(OptionBuilder.withLongOpt("host").withDescription("Host to connect to.").withArgName("HOST").hasArg().create("h"));
        this.opts.addOption(OptionBuilder.withLongOpt("port").withDescription("Port to connect to.").withArgName("PORT").hasArg().create("p"));
        this.opts.addOption(OptionBuilder.withLongOpt("url_path").withDescription("Path portion of the JMX Service URL.").withArgName("PATH").hasArg().create("T"));
        this.opts.addOption(OptionBuilder.withLongOpt("user").withArgName("USER").hasArg().withDescription("Connect with this username.").create("U"));
        this.opts.addOption(OptionBuilder.withLongOpt("password").withArgName("PASSWORD").hasArg().withDescription("Connect with this password.").create("P"));
        this.opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display usage help.").hasArg(false).create("?"));
        this.opts.addOption(OptionBuilder.withLongOpt("protocol").withDescription("Choose a connection protocol (rmi|jmxmp), default rmi.").hasArg(true).withArgName("PROTOCOL").create("R"));
    }

    private CommandLine parseCommandLine(TclObject argv[]) throws ParseException {
        String[] args = new String[argv.length - 1];
        for (int i = 0; i < argv.length - 1; i++) args[i] = argv[i + 1].toString();
        CommandLine cl = (new PosixParser()).parse(this.opts, args);
        return cl;
    }

    private void connect(CommandLine commandLine) {
        String serverUrl = commandLine.getOptionValue("server");
        String host = commandLine.getOptionValue("host");
        String protocol = commandLine.getOptionValue("protocol", "rmi");
        String path = commandLine.getOptionValue("url_path");
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");
        int port = -1;
        if (commandLine.hasOption("port")) {
            port = Integer.parseInt(commandLine.getOptionValue("port"));
        }
        if (Main.interactive) {
            Readline rl = Readline.getInstance();
            if (user != null && password == null) {
                password = rl.readline("Password: ", '*');
            }
        }
        if (serverUrl != null) {
            Jmx.getInstance().connect(serverUrl, user, password);
        } else {
            Jmx.getInstance().connect(host, port, protocol, path, user, password);
        }
    }

    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
        try {
            CommandLine cl = parseCommandLine(argv);
            if (cl.hasOption("help")) {
                new HelpFormatter().printHelp("jmx_connect ", "======================================================================", this.opts, "======================================================================", true);
                System.out.println("jmx_connect establishes a connection to a JMX server.");
                return;
            }
            this.connect(cl);
        } catch (ParseException e) {
            throw new TclException(interp, e.getMessage(), 1);
        } catch (RuntimeException e) {
            logger.error("Runtime Exception", e);
            throw new TclException(interp, "Runtime exception.", 1);
        }
    }

    private static Logger logger = Logger.getLogger(ConnectCmd.class);

    private Options opts;
}
