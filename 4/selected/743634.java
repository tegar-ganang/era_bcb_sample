package edu.umn.cs.nlp.mt.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Scanner;
import edu.umn.cs.nlp.util.CommandLineParser;
import edu.umn.cs.nlp.util.CommandLineParser.Option;

public class ConvertToLowercase {

    public static void convert(Scanner input, Locale locale, PrintStream output) throws IOException {
        while (input.hasNextLine()) {
            output.println(input.nextLine().toLowerCase(locale));
        }
    }

    public static void main(String[] args) {
        CommandLineParser commandLine = new CommandLineParser();
        Option<String> input_file = commandLine.addStringOption('i', "input", "FILE_NAME", "input file, specify - for standard input");
        Option<String> output_file = commandLine.addStringOption('o', "output", "FILE_NAME", "input file, specify - for standard output");
        Option<String> input_encoding = commandLine.addStringOption('e', "encoding", "ENCODING", "UTF-8", "input encoding");
        Option<String> output_encoding = commandLine.addStringOption("output-encoding", "ENCODING", "UTF-8", "output encoding");
        Option<String> language = commandLine.addStringOption('l', "language", "LANGUAGE", "", "lowercase two-letter ISO-639 code");
        Option<String> country = commandLine.addStringOption('c', "country", "COUNTRY", "", "uppercase two-letter ISO-3166 code");
        commandLine.parse(args);
        Scanner input = null;
        if (commandLine.getValue(input_file).equals("-")) {
            input = new Scanner(System.in, commandLine.getValue(input_encoding));
        } else {
            try {
                input = new Scanner(new File(commandLine.getValue(input_file)), commandLine.getValue(input_encoding));
            } catch (FileNotFoundException e) {
                System.err.println("Specified input file cannot be found: " + commandLine.getValue(input_file));
                System.exit(-1);
            }
        }
        PrintStream output = null;
        if (commandLine.getValue(output_file).equals("-")) {
            try {
                output = new PrintStream(System.out, true, commandLine.getValue(output_encoding));
            } catch (UnsupportedEncodingException e) {
                System.err.println("Unsupported output encoding: " + commandLine.getValue(output_encoding) + " - using system default encoding");
                output = System.out;
            }
        } else {
            try {
                try {
                    output = new PrintStream(new File(commandLine.getValue(output_file)), commandLine.getValue(output_encoding));
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Unsupported output encoding: " + commandLine.getValue(output_encoding) + " - using system default encoding");
                    output = new PrintStream(new File(commandLine.getValue(output_file)));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        Locale locale = null;
        if (commandLine.hasValue(language) && !commandLine.getValue(language).equals("")) {
            if (commandLine.hasValue(country) && !commandLine.getValue(country).equals("")) {
                locale = new Locale(commandLine.getValue(language), commandLine.getValue(country));
            } else {
                locale = new Locale(commandLine.getValue(language));
            }
        } else {
            locale = Locale.getDefault();
        }
        try {
            if (input != null && locale != null && output != null) {
                convert(input, locale, output);
            } else {
                System.out.println("Invalid argument somewhere");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
