package org.zodiak.command;

import java.util.HashMap;
import java.util.Map;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * This class is the main entry point for all command-line tools.  It expects
 * its first argument to be the name of a command.  It executes the command
 * with the rest of the arguments.
 *
 * @author Steven R. Farley
 */
public class Main {

    private Main() {
    }

    /**
   * This maps command names to Command instances.
   */
    private static final Map<String, Command> commands;

    static {
        commands = new HashMap<String, Command>();
        commands.put("grep", new Grep());
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("The first argument must be a command.");
            usage();
            System.exit(-1);
        }
        String commandName = args[0];
        Command command = commands.get(commandName);
        if (command == null) {
            System.err.println("Unknown command: " + commandName);
            usage();
            System.exit(-1);
        }
        String[] commandArgs = new String[args.length - 1];
        for (int i = 0; i < commandArgs.length; i++) {
            commandArgs[i] = args[i + 1];
        }
        Object options = command.getOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(commandArgs);
            command.validate();
            command.execute();
            System.exit(0);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            usage(parser, commandName);
            System.exit(1);
        } catch (CommandException e) {
            System.err.print(e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println(String.format(" (%s)", cause.getMessage()));
            } else {
                System.out.println();
            }
            if (e.isUsageError()) {
                usage(parser, commandName);
            }
            System.exit(2);
        } catch (RuntimeException e) {
            System.err.println("Unexpected error:");
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static void usage(CmdLineParser parser, String commandName) {
        if (commandName != null) {
            System.err.println(String.format("od%s [options...] arguments...", commandName));
        } else {
            System.err.println("java -jar zodiak.jar command [options...] arguments...");
        }
        if (parser != null) {
            parser.printUsage(System.err);
        } else {
            System.err.println("commands: grep");
        }
    }

    private static void usage() {
        usage(null, null);
    }
}
