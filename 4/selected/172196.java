package org.psepr.WatchChannel;

/**
 * @author Robert Adams <robert.adams@intel.com>
 */
public class WatchChannel {

    /** CvsId */
    public static final String CvsId = "$Id: WatchChannel.java 33 2007-03-08 18:31:28Z Misterblue $";

    private static boolean keepWaiting;

    private static int verbose;

    /**
     * TODO
     */
    public WatchChannel() {
        super();
    }

    private static void Invocation() {
        System.out.println("Listen on the specified channel for events.");
        System.out.println("Invocation: WatchChannel --channel channelName");
        System.out.println("                         --type typeOfMessage");
        System.out.println("                         --service serviceName");
        System.out.println("                         --password servicePassword");
        System.out.println("                         --pretty");
        System.out.println("                         --raw");
        System.out.println("                         --config configurationFile");
        System.out.println("                         --verbose");
        System.out.println("                         --version");
        System.out.println("                         --help");
        System.out.println("If 'typeOfMessage' is not specified, it defaults to all messages.");
        System.out.println("'pretty' says to pretty print (default). 'raw' says to output payload in one line.");
        System.out.println("If service/password not specified, defaults to config file defaults");
        System.out.println("Multiple instances of 'verbose' increase verbosity.");
        System.out.println("Multiple configuration files are specified with multiple 'config's");
    }

    /**
     * @return version number
     */
    private static String version() {
        String revision = "$Revision: 33 $";
        revision = revision.replaceAll("\\$Revision: ", "");
        return revision.substring(0, revision.length() - 2);
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        EventPrinter printer = new EventPrinter();
        ChannelWatcher watcher = new ChannelWatcher(printer);
        args = watcher.readArgs(args);
        verbose = 0;
        for (int jj = 0; jj < args.length; jj++) {
            if (args[jj].equalsIgnoreCase("--pretty")) {
                printer.setPrettyPrint(true);
            } else if (args[jj].equalsIgnoreCase("--raw")) {
                printer.setPrettyPrint(false);
            } else if (args[jj].equalsIgnoreCase("--verbose")) {
                verbose++;
            } else if (args[jj].equalsIgnoreCase("--version")) {
                System.out.println("WatchChannel " + version());
                System.out.println("Copyright (c) 2005 Intel Corporation - All rights reserved.");
                System.exit(1);
            } else if (args[jj].equalsIgnoreCase("--help")) {
                Invocation();
                System.exit(0);
            } else {
                System.out.println("Parameter '" + args[jj] + "' not recognized.");
                Invocation();
                System.exit(0);
            }
        }
        if (watcher.getChannel() == null) {
            System.out.println("A channel to watch must be specified.");
            Invocation();
            return;
        }
        if (verbose > 0) {
            System.out.println("Watching for events of type " + watcher.getType() + " on channel " + watcher.getChannel());
        }
        keepWaiting = true;
        try {
            watcher.connect();
            while (keepWaiting) {
                Thread.sleep(30000);
                if (!watcher.isConnected()) {
                    System.out.println("WatchChannel: lease is not active.  Exiting");
                    keepWaiting = false;
                }
            }
        } catch (Exception e) {
            System.out.println("WatchChannel: exception while waiting:" + e.toString());
            keepWaiting = false;
        }
        watcher.disconnect();
    }
}
