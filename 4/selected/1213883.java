package com.darkhonor.rage.libs;

import com.darkhonor.rage.model.Question;
import com.darkhonor.rage.model.TestCase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import org.apache.log4j.Logger;

/**
 * The TestCaseGenThread runs a Raptor program for a given test file and saves
 * the output of the program to the {@link Question} object for each {@link TestCase}.
 *
 * @author Alexander.Ackerman 
 *
 * @version 1.0.0
 *
 */
public class TestCaseGenThread implements Runnable {

    /**
     * No argument constructor.  Initializes each of the object elements.
     */
    public TestCaseGenThread() {
        processBuilder = new ProcessBuilder();
        testFile = null;
        question = new Question();
        node = null;
        type = 0;
    }

    /**
     * Constructor for TestCaseGenThread object.
     *
     * @param prefs The stored preferences for the user
     * @param pb    The process builder used to create the thread
     * @param file  The test program to use
     * @param question  The question object to store the results to
     */
    public TestCaseGenThread(Preferences prefs, ProcessBuilder pb, File file, Question question, int type) {
        this.processBuilder = pb;
        this.testFile = file;
        this.question = question;
        this.node = prefs;
        this.type = type;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 10);
            Iterator testCaseIterator = question.getTestCases().iterator();
            while (testCaseIterator.hasNext()) {
                TestCase testCase = (TestCase) testCaseIterator.next();
                File tempIn = File.createTempFile("rage", ".tmp");
                File tempOut = File.createTempFile("rage", ".tmp");
                FileOutputStream outFile = new FileOutputStream(tempIn);
                FileChannel outChannel = outFile.getChannel();
                outChannel.lock();
                for (int i = 0; i < testCase.getInputs().size(); i++) {
                    buffer.put(testCase.getInputs().get(i).getBytes());
                    buffer.put(System.getProperty("line.separator").getBytes());
                    buffer.flip();
                    outChannel.write(buffer);
                    buffer.clear();
                }
                outFile.close();
                outChannel.close();
                List<String> cmd = new ArrayList<String>();
                if (type == RAGEConst.RAPTOR_QUESTION) {
                    cmd.add(node.get("RaptorExecutable", RAGEConst.DEFAULT_RAPTOR_EXECUTABLE));
                    cmd.add("\"" + testFile.getCanonicalPath() + "\"");
                    cmd.add("/run");
                    cmd.add("\"" + tempIn.getCanonicalPath() + "\"");
                    cmd.add("\"" + tempOut.getCanonicalPath() + "\"");
                } else if (type == RAGEConst.PROCESSING_QUESTION) {
                    cmd.add(node.get("RunmeExecutable", RAGEConst.DEFAULT_PROCESSING_RUNNER));
                    cmd.add(question.getName());
                    for (int i = 0; i < testCase.getInputs().size(); i++) {
                        cmd.add(testCase.getInputs().get(i));
                    }
                    cmd.add(">" + tempOut.getCanonicalPath());
                } else {
                    LOGGER.error("Unsupported Question Type");
                    throw new UnsupportedOperationException();
                }
                LOGGER.debug("Command: " + cmd);
                processBuilder.command(cmd);
                Process p = processBuilder.start();
                Long startTimeInNanoSec = System.nanoTime();
                Long delayInNanoSec;
                if (node.getBoolean("InfiniteLoopDetection", true)) {
                    LOGGER.debug("Infinite loop detection is enabled");
                    try {
                        delayInNanoSec = Long.parseLong(node.get("Threshold", "10")) * 1000000000;
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid Threshold value.  Defaulting to 10");
                        delayInNanoSec = new Long(10 * 1000000000);
                    }
                    boolean timeFlag = true;
                    while (timeFlag) {
                        try {
                            int val = p.exitValue();
                            timeFlag = false;
                        } catch (IllegalThreadStateException e) {
                            Long elapsedTime = System.nanoTime() - startTimeInNanoSec;
                            if (elapsedTime > delayInNanoSec) {
                                LOGGER.warn("Threshold time exceeded.");
                                p.destroy();
                                timeFlag = false;
                            }
                            Thread.sleep(50);
                        }
                    }
                } else {
                    LOGGER.debug("Infinite loop detection is not enabled");
                    p.waitFor();
                }
                File newTemp = null;
                BufferedReader inFile;
                try {
                    inFile = new BufferedReader(new FileReader(tempOut));
                } catch (FileNotFoundException ex) {
                    LOGGER.error("The file is in use by another process.");
                    newTemp = File.createTempFile("rage", ".tmp");
                    inFile = new BufferedReader(new FileReader(newTemp));
                }
                String line = null;
                while ((line = inFile.readLine()) != null) {
                    testCase.addOutput(line);
                }
                inFile.close();
                tempIn.delete();
                tempOut.delete();
                if (newTemp != null) {
                    newTemp.delete();
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Exception: " + ex.getLocalizedMessage());
        } catch (InterruptedException ex) {
            LOGGER.warn("Thread interrupted");
        } catch (ThreadDeath td) {
            throw td;
        }
    }

    private ProcessBuilder processBuilder;

    private File testFile;

    private Question question;

    private Preferences node;

    private int type;

    private static final Logger LOGGER = Logger.getLogger(TestCaseGenThread.class);
}
