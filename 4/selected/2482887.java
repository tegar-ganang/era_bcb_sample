package com.evolve.autotest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import com.evolve.autotest.cli.ConsolAppender;
import com.evolve.autotest.util.Configurations;

/**
 * The main entry point to the application
 * @author qbacfre
 *
 */
public class AutoTestCommandLine {

    private static AutoTest testRunner;

    private static BufferedReader userInputReader;

    private static ConsolAppender consol;

    private static Configurations config;

    /**
	 * Runs a command line version.
	 * (Mandatory) First argument must be the location of the autotest properties file
	 * (Optional) Location of the logging config file. If this is left out the command line
	 * version of autotest will be run. 
	 * @param args 
	 */
    public static void main(String[] args) {
        testRunner = new AutoTest();
        config = Configurations.getInstance();
        if (args.length == 1) {
            loadProperties(args[0]);
            consol = new ConsolAppender();
            BasicConfigurator.configure(consol);
            writeHello();
            runPrompt();
        } else if (args.length == 2) {
            loadProperties(args[0]);
            PropertyConfigurator.configure(args[1]);
            testRunner.loadActions();
            testRunner.loadTests();
            testRunner.runTests();
            testRunner.handleResults();
        } else {
            printUsage();
        }
    }

    /**
	 * Starts the command prompt
	 */
    private static void runPrompt() {
        writePrompt();
        String command = readInput();
        if (command.equalsIgnoreCase("exit")) {
            exit();
        } else if (command.equalsIgnoreCase("load")) {
            endMessage();
            testRunner.loadActions();
            endMessage();
        } else if (command.equalsIgnoreCase("compile")) {
            endMessage();
            testRunner.loadTests();
            endMessage();
        } else if (command.equalsIgnoreCase("run")) {
            endMessage();
            testRunner.runTests();
            endMessage();
        } else if (command.equalsIgnoreCase("help")) {
            endMessage();
            consol.writeln("Commands:\n  load - Load actions\n  compile - Compile test cases\n  run - Run test cases\n  exit - Exit the prompt\n  help - Print help text");
            endMessage();
        } else {
            endMessage();
            consol.writeln("Unknown action");
            endMessage();
        }
        runPrompt();
    }

    private static void exit() {
        consol.writeln("Good by...");
        System.exit(0);
    }

    private static String readInput() {
        if (userInputReader == null) {
            userInputReader = new BufferedReader(new InputStreamReader(System.in));
        }
        try {
            return userInputReader.readLine();
        } catch (IOException e) {
            consol.write("Unable to read input");
            System.exit(1);
        }
        return null;
    }

    /**
	 * Writes a output message
	 * @param string
	 */
    private static void writeMessage(String string) {
        String dashes = "------------------------------------------------------------------------\n";
        consol.write(dashes + string + "\n");
    }

    private static void endMessage() {
        String dashes = "------------------------------------------------------------------------";
        consol.writeln(dashes);
    }

    /**
	 * Writes the prompt
	 */
    private static void writePrompt() {
        consol.write("> ");
    }

    private static void writeHello() {
        consol.writeln("AutoTest Version 0.1");
    }

    private static void printUsage() {
        System.out.println("Parameters not set.");
        System.out.println("Usage: <config_file> [<logging config file>]");
    }

    private static void loadProperties(String propertiesFile) {
        if (!config.loadFromFile(propertiesFile)) {
            consol.writeln("Unable to load properties");
            exit();
        }
    }
}
