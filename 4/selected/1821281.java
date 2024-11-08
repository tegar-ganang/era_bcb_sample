package com.optit.report;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import com.optit.generator.Generator;
import com.optit.logger.Logger;

/**
 * This class handles the reporting of a given Result List
 * @author Gerald Venzl
 */
public class Reporter {

    public static enum FORMAT {

        CSV, TEXT
    }

    ;

    private static int iOverallCharacters = 100;

    private LinkedHashMap<String, LinkedHashMap<String, Result>> allTestResults;

    private Integer sessions;

    private Integer rows;

    private Integer data = Generator.MAX_SIZE;

    private FORMAT fmt;

    private PrintStream writer;

    /**
	 * Inner class that supports a human readable format of an amount of bytes
	 * @author Gerald Venzl
	 *
	 */
    private static class HumanReadable {

        private static long teraByte = (long) 1000 * 1000 * 1000 * 1000;

        private static long gigaByte = 1000 * 1000 * 1000;

        private static long megaByte = 1000 * 1000;

        private static long kiloByte = 1000;

        /**
		 * Constructs a human readable String with the amount of bytes
		 * @param bytes Amount of bytes that should be made human readable
		 * @return A human readable String object with the amount of bytes
		 */
        public static String makeHumanReadable(int bytes) {
            return makeHumanReadable(Integer.valueOf(bytes));
        }

        /**
		 * Constructs a human readable String with the amount of bytes
		 * @param bytes Amount of bytes that should be made human readable
		 * @return A human readable String object with the amount of bytes
		 */
        public static String makeHumanReadable(Integer bytes) {
            if (bytes >= teraByte) {
                return ((bytes / teraByte) + "TB");
            } else if (bytes >= gigaByte) {
                return ((bytes / gigaByte) + "GB");
            } else if (bytes >= megaByte) {
                return ((bytes / megaByte) + "MB");
            } else if (bytes >= kiloByte) {
                return ((bytes / kiloByte) + "KB");
            } else {
                return bytes + "B";
            }
        }
    }

    /**
	 * Constructs a Reporter instance
	 * @param allTestResults The overall test results object to report
	 * @param sessions The amount of session that loaded the data
	 * @param rows The amount of rows that got loaded per each session
	 * @param fileName The file name for an CSV format output file
	 * @throws FileNotFoundException If there is any problem with the file creation/write permissions
	 */
    public Reporter(LinkedHashMap<String, LinkedHashMap<String, Result>> allTestResults, Integer sessions, Integer rows, String fileName) throws FileNotFoundException {
        this.allTestResults = allTestResults;
        this.sessions = sessions;
        this.rows = rows;
        if (fileName != null && !fileName.isEmpty()) {
            this.fmt = FORMAT.CSV;
            writer = new PrintStream(fileName);
        } else {
            this.fmt = FORMAT.TEXT;
            writer = System.out;
        }
    }

    /**
	 * Prints the results of the overall test object
	 */
    public void printResults() {
        Logger.log("Writing results...");
        printConfigurationResults();
        switch(fmt) {
            case CSV:
                {
                    printCSVResults();
                    break;
                }
            case TEXT:
                {
                    printStandardOutResults();
                    break;
                }
            default:
                {
                    printStandardOutResults();
                    break;
                }
        }
        Logger.log("Writing results... DONE");
    }

    /**
	 * Prints the configuration part of the test cases
	 */
    private void printConfigurationResults() {
        writer.println("Data loaded: " + HumanReadable.makeHumanReadable(sessions * rows * data));
        writer.println("Sessions: " + sessions);
        writer.println("Data per session loaded: " + HumanReadable.makeHumanReadable(rows * data));
        writer.println("Rows per session: " + rows);
        writer.println("Data per session per row loaded: " + HumanReadable.makeHumanReadable(data));
    }

    /**
	 * Prints the results into the standard output
	 */
    private void printStandardOutResults() {
        String lineSeparator = System.getProperty("line.separator");
        writer.println();
        writer.println("RESULTS:");
        writer.println();
        Iterator<Entry<String, LinkedHashMap<String, Result>>> allTests = allTestResults.entrySet().iterator();
        while (allTests.hasNext()) {
            Entry<String, LinkedHashMap<String, Result>> testEntry = allTests.next();
            String outputMainString = "Test case: " + testEntry.getKey();
            String outputThreadsString = "";
            long overallMilliSeconds = 0;
            Iterator<Result> results = testEntry.getValue().values().iterator();
            while (results.hasNext()) {
                Result result = results.next();
                overallMilliSeconds += result.getDuration();
                String threadString = "    Thread " + result.getTestCaseNumber() + ":";
                for (int i = threadString.length(); i < (iOverallCharacters - String.valueOf(result.getDuration()).length()); i++) {
                    threadString += " ";
                }
                threadString = threadString + result.getDuration() + " ms" + lineSeparator;
                outputThreadsString += threadString;
            }
            for (int i = outputMainString.length(); i < (iOverallCharacters - String.valueOf(overallMilliSeconds / sessions).length()); i++) {
                outputMainString += " ";
            }
            writer.print(outputMainString + (overallMilliSeconds / sessions) + " ms (avg)" + lineSeparator + outputThreadsString);
            writer.println();
        }
    }

    /**
	 * Prints the results into a CSV file
	 */
    private void printCSVResults() {
        String lineSeparator = ",";
        writer.println();
        writer.print("Testcase" + lineSeparator);
        for (int iThread = 1; iThread <= sessions; iThread++) {
            writer.print("Thread " + iThread + " (ms)" + lineSeparator);
        }
        writer.println("AVG (ms)");
        Iterator<Entry<String, LinkedHashMap<String, Result>>> allTests = allTestResults.entrySet().iterator();
        while (allTests.hasNext()) {
            Entry<String, LinkedHashMap<String, Result>> testEntry = allTests.next();
            writer.print(testEntry.getKey() + lineSeparator);
            long overallMilliSeconds = 0;
            Iterator<Result> results = testEntry.getValue().values().iterator();
            while (results.hasNext()) {
                Result result = results.next();
                overallMilliSeconds += result.getDuration();
                writer.print(result.getDuration() + lineSeparator);
            }
            writer.println(overallMilliSeconds / sessions);
        }
    }
}
