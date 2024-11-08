package jmxsh;

import org.apache.commons.cli.*;
import tcl.lang.*;

class SetCmd implements Command {

    private static final SetCmd instance = new SetCmd();

    static SetCmd getInstance() {
        return instance;
    }

    private SetCmd() {
        opts = new Options();
        opts.addOption(OptionBuilder.withLongOpt("server").withDescription("Server containing mbean.").withArgName("SERVER").hasArg().create("s"));
        opts.addOption(OptionBuilder.withLongOpt("mbean").withDescription("MBean containing attribute.").withArgName("MBEAN").hasArg().create("m"));
        opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display usage help.").hasArg(false).create("?"));
    }

    private CommandLine parseCommandLine(TclObject argv[]) throws ParseException {
        String[] args = new String[argv.length - 1];
        for (int i = 0; i < argv.length - 1; i++) args[i] = argv[i + 1].toString();
        CommandLine cl = (new PosixParser()).parse(opts, args);
        return cl;
    }

    private void getDefaults(Interp interp) {
        server = null;
        mbean = null;
        attrop = null;
        try {
            server = interp.getVar("SERVER", TCL.GLOBAL_ONLY).toString();
            mbean = interp.getVar("MBEAN", TCL.GLOBAL_ONLY).toString();
            attrop = interp.getVar("ATTROP", TCL.GLOBAL_ONLY).toString();
        } catch (TclException e) {
        }
    }

    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
        try {
            CommandLine cl = parseCommandLine(argv);
            String args[] = cl.getArgs();
            String attribute = null;
            TclObject newvalue = null;
            if (cl.hasOption("help")) {
                new HelpFormatter().printHelp("jmx_set [-?] [-s server] [-m mbean] [ATTRIBUTE] NEW_VALUE", "======================================================================", opts, "======================================================================", false);
                System.out.println("jmx_set updates the current value of the given attribute.");
                System.out.println("If you do not specify server, mbean, or ATTRIBUTE, then the");
                System.out.println("values in the global variables SERVER, MBEAN, and ATTROP,");
                System.out.println("respectively, will be used.");
                return;
            }
            getDefaults(interp);
            server = cl.getOptionValue("server", server);
            mbean = cl.getOptionValue("mbean", mbean);
            attrop = cl.getOptionValue("attrop", attrop);
            if (args.length > 1) {
                attribute = args[0];
                newvalue = argv[argv.length - args.length + 1];
            } else {
                attribute = attrop;
                newvalue = argv[argv.length - args.length];
            }
            if (server == null) {
                throw new TclException(interp, "No server specified; please set SERVER variable or use -s option.", TCL.ERROR);
            }
            if (mbean == null) {
                throw new TclException(interp, "No mbean specified; please set MBEAN variable or use -m option.", TCL.ERROR);
            }
            if (attribute == null) {
                throw new TclException(interp, "No attribute specified; please set ATTROP variable or add it to the command line.", TCL.ERROR);
            }
            if (newvalue == null) {
                throw new TclException(interp, "No new value provided for the attribute.", TCL.ERROR);
            }
            Jmx.getInstance().setAttribute(server, mbean, attribute, newvalue);
        } catch (ParseException e) {
            throw new TclException(interp, e.getMessage(), 1);
        } catch (RuntimeException e) {
            throw new TclException(interp, e.getMessage(), 1);
        }
    }

    private String server;

    private String mbean;

    private String attrop;

    private Options opts;
}
