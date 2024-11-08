package org.jaffa.tools.loadtest;

import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;

/**
 *
 * @author  MaheshD
 */
public class TestResultLogger {

    private Writer writer;

    /** Creates a new instance of TestResultLogger
     * @param fileName  File name to which the Test Results are written
     */
    public TestResultLogger(String webRoot, String fileName) {
        try {
            writer = new FileWriter(fileName);
            writer.write("Web-Root" + "," + webRoot + "\n");
            writer.write("Thread");
            writer.write(",");
            writer.write("Iteration");
            writer.write(",");
            writer.write("Start Time");
            writer.write(",");
            writer.write("End Time");
            writer.write(",");
            writer.write("Duration");
            writer.write(",");
            writer.write("Test Case");
            writer.write(",");
            writer.write("Status");
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Writes out the Result Set to the file.
     * @param threadNo thread number which it is running
     * @param iteration which Iteration its running
     * @param startTime the start time of the unit test
     * @param endTime the end time of the unit test
     * @param duration time taken for the unit test to run in milliseconds
     * @param testCase  name of the test case .
     */
    public synchronized void output(int threadNo, int iteration, String startTime, String endTime, String duration, String testCase, String success) {
        String testResult = Integer.toString(threadNo) + "," + Integer.toString(iteration) + "," + startTime + "," + endTime + "," + duration + "," + testCase + "," + success + "\n";
        try {
            writer.write(testResult);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
