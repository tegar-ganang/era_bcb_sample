package org.hitchhackers.tools.jmx;

import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.hitchhackers.tools.jmx.commands.CommandBase;

/**
 * entry point for console calls (shell scripts etc.)
 * 
 * @author Philipp Traeder
 */
public class JMXConsoleTool {

    public JMXConsoleTool() {
        super();
    }

    public static void main(String[] args) {
        JMXConsoleTool client = new JMXConsoleTool();
        client.run(args);
    }

    private void initLog4J(String[] args) {
        Level desiredLevel = Level.INFO;
        Pattern pattern = Pattern.compile("^(?:--)debug$");
        for (String arg : args) {
            if (pattern.matcher(arg).matches()) {
                desiredLevel = Level.DEBUG;
                break;
            }
        }
        Appender consoleAppender = new ConsoleAppender();
        consoleAppender.setLayout(new PatternLayout("%-5p %m%n"));
        Logger.getRootLogger().addAppender(consoleAppender);
        Logger.getRootLogger().setLevel(desiredLevel);
    }

    private void printGlobalUsage() {
        System.out.println();
        System.out.println(CommandBase.USAGE_TITLE);
        System.out.println();
        System.out.println("Please pass the name of the command to run as first parameter - known commands are:");
        String[] commandNames = CommandProcessor.getCommandNames();
        for (String commandName : commandNames) {
            System.out.println("  " + commandName);
        }
        System.out.println();
        System.out.println("If you call a command without any parameters like this");
        System.out.println("  ./jmx_console.sh browse");
        System.out.println("you'll get a detailed help screen about this command.");
        System.exit(1);
    }

    public void run(String[] args) {
        initLog4J(args);
        if (args.length < 1) {
            printGlobalUsage();
        }
        String commandName = args[0];
        String[] newArgs = new String[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            newArgs[i] = args[i + 1];
        }
        CommandProcessor commandProcessor = null;
        try {
            commandProcessor = new CommandProcessor(commandName);
            commandProcessor.init(newArgs);
            String result = commandProcessor.execute();
            System.out.println(result);
            System.exit(0);
        } catch (HelpRequiredException e) {
            commandProcessor.getCommand().printUsage();
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] : " + e.getMessage());
            if ((commandProcessor != null) && (commandProcessor.getCommand() != null)) {
                commandProcessor.getCommand().printUsage();
            } else {
                printGlobalUsage();
            }
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not establish connection to VM via JMX : ");
            e.printStackTrace(System.err);
            System.exit(2);
        } catch (Throwable t) {
            System.err.println("There occurred a very unexpected error:");
            t.printStackTrace();
            System.exit(3);
        }
    }
}
