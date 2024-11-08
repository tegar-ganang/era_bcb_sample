package com.unitvectory.categoryclassification;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Performs the double check on the data.
 * 
 * @author Jared Hatfield
 * 
 */
public class DoubleCheck {

    /**
     * The main method for performing the analysis.
     * 
     * <br />
     * <br />
     * <b>Arguments:</b><br />
     * 
     * <p>
     * <i>-i FILENAME</i> Name of the input file
     * </p>
     * 
     * <p>
     * <i>-o FILENAME</i> Name of the output file
     * </p>
     * 
     * <p>
     * <i>-s FILENAME</i> Name of the stop word file (optional)
     * </p>
     * 
     * <p>
     * <i>-p NUM</i> The weight for positive classes, default of 1 (optional)
     * </p>
     * 
     * <p>
     * <i>-n NUM</i> The weight for negative classes, default of 1 (optional)
     * </p>
     * 
     * <p>
     * <i>-w</i> Flag to overwrite output file (optional)
     * </p>
     * 
     * <p>
     * <i>-f</i> Flag to force overwrite of data directory (optional)
     * </p>
     * 
     * <p>
     * <i>-q</i> Flag to quite output (optional)
     * </p>
     * 
     * <p>
     * <i>-x</i> Flag to have output be formatted as XML (optional)
     * </p>
     * 
     * <br />
     * 
     * <b>Usage:</b><br />
     * 
     * <p>
     * java -jar ccdc.java -i data.xml -o report.txt
     * </p>
     * 
     * <p>
     * java -jar ccdc.java -i data.xml -o report.txt -p 100 -n 0.001
     * </p>
     * 
     * @param args
     *            The command line arguments.
     */
    public static void main(String[] args) {
        String inputFileName = DoubleCheck.getParam(args, "-i");
        String outputFileName = DoubleCheck.getParam(args, "-o");
        String stopFileName = DoubleCheck.getParam(args, "-s");
        String bayesPositive = DoubleCheck.getParam(args, "-p");
        String bayesNegative = DoubleCheck.getParam(args, "-n");
        boolean quietFlag = DoubleCheck.isParam(args, "-q");
        boolean xmlFlag = DoubleCheck.isParam(args, "-x");
        List<String> stopWords = new ArrayList<String>();
        try {
            if (inputFileName == null) {
                DoubleCheck.writeConsole("Error: no input file specified.\n", quietFlag);
                return;
            } else if (outputFileName == null) {
                DoubleCheck.writeConsole("Error: no output file specified.\n", quietFlag);
                return;
            }
            File output = new File(outputFileName);
            if (output.exists() && DoubleCheck.isParam(args, "-w")) {
                output.delete();
            } else if (output.exists()) {
                DoubleCheck.writeConsole("Error: output file already exists.\n", quietFlag);
                return;
            }
            if (stopFileName != null) {
                File stop = new File(stopFileName);
                if (!stop.exists()) {
                    DoubleCheck.writeConsole("Error: stil file not found.\n", quietFlag);
                    return;
                } else {
                    stopWords = DataProcessor.getStopList(stopFileName);
                }
            }
            double positive = 1;
            try {
                if (bayesPositive != null) {
                    positive = Double.parseDouble(bayesPositive);
                }
            } catch (Exception e) {
            }
            double negative = 1;
            try {
                if (bayesNegative != null) {
                    negative = Double.parseDouble(bayesNegative);
                }
            } catch (Exception e) {
            }
            Data data = DataProcessor.loadData(inputFileName);
            if (data == null) {
                DoubleCheck.writeConsole("Error: The data was not loaded successfully.\n", quietFlag);
                return;
            }
            File main = new File("data");
            if (main.exists() && DoubleCheck.isParam(args, "-f")) {
                DataProcessor.deleteDir(main);
            } else if (main.exists()) {
                DoubleCheck.writeConsole("Error: The temporary data director found.\n", quietFlag);
                return;
            } else {
                main.mkdir();
            }
            StringBuilder sb = new StringBuilder();
            Suggestions suggestions = new Suggestions();
            DoubleCheck.writeConsole("Processing...\n", quietFlag);
            double goal = data.getCategories().size();
            for (int i = 0; i < data.getCategories().size(); i++) {
                Category cat = data.getCategories().get(i);
                File loc = DataProcessor.saveFiles(main, stopWords, data, cat);
                List<String> misclassified = ClassificationValidation.run(loc, positive, negative);
                for (int j = 0; j < misclassified.size(); j++) {
                    String result = misclassified.get(j);
                    String[] info = result.split(":");
                    ItemSuggestion s = data.misclassifiedValue(info[0], cat, Double.parseDouble(info[1]));
                    suggestions.getSuggestions().add(s);
                    sb.append(s + "\n");
                }
                if (misclassified.size() > 0) {
                    sb.append("\n");
                }
                double percent = Math.round((i / goal) * 10000.0) / 100.0;
                DoubleCheck.writeConsole("\r" + percent + "%", quietFlag);
            }
            DoubleCheck.writeConsole("\r100.0%\n", quietFlag);
            DoubleCheck.writeConsole("Cleaning up...\n", quietFlag);
            DataProcessor.deleteDir(main);
            if (xmlFlag) {
                Serializer serializer = new Persister();
                serializer.write(suggestions, output);
            } else {
                DataProcessor.writeFile(output, sb.toString());
            }
            DoubleCheck.writeConsole("Success!\n", quietFlag);
        } catch (Exception e) {
            DoubleCheck.writeConsole("Error: " + e.getMessage() + "\n", quietFlag);
        }
    }

    /**
     * Write to the console.
     * 
     * @param val
     *            The string to write.
     * @param quiet
     *            The quiet flag.
     */
    private static void writeConsole(String val, boolean quiet) {
        if (!quiet) {
            System.out.print(val);
        }
    }

    /**
     * Gets the parameter following the argument.
     * 
     * @param args
     *            The array of arguments.
     * @param name
     *            The name of the argument.
     * @return The value for the argument.
     */
    private static String getParam(String[] args, String name) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name) && (i + 1) < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Tests if an argument is present.
     * 
     * @param args
     *            The array of arguments.
     * @param name
     *            The name of the argument.
     * @return True if the argument was found; otherwise false.
     */
    private static boolean isParam(String[] args, String name) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                return true;
            }
        }
        return false;
    }
}
