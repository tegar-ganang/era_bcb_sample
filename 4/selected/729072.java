package client.loggers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Concrete Logger implementation that creates text-based
 * log files, with the extension .log.
 *
 * @author Daniel Wood (daniel-g-wood@users.sourceforge.net)
 * @version 2010.02.25
 */
public class TextLogger extends Logger {

    /**
     * Prefix to each entry stamp.
     */
    public static final String ENTRY_PREFIX = "[";

    /**
     * Format to use for each log timestamp.
     */
    public static final String ENTRY_STAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Suffix to each entry stamp.
     */
    public static final String ENTRY_SUFFIX = "] ";

    /**
     * Extension to use for output files.
     */
    public static final String FILENAME_EXT = ".log.txt";

    /**
     * Format to use for filenames.
     */
    public static final String FILENAME_FORMAT = "yyyy-MM-dd_HH-mm";

    /**
     * Prefix output files with this.
     */
    public static final String FILENAME_PREFIX = "asr_";

    /**
     * Terminate each entry.
     */
    public static final String ENTRY_TERMINATOR = "." + System.getProperty("line.separator");

    /**
     * Store full filename.
     */
    private String filename;

    /**
     * Initialise the TextLogger, creating the necessary
     * FileWriter.
     */
    public TextLogger() {
        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT);
        filename = FILENAME_PREFIX;
        filename += sdf.format(Calendar.getInstance().getTime());
        filename += FILENAME_EXT;
    }

    /**
     * Set the target number of cycles.
     * @param targetCycles cycles to run.
     */
    public void setTargetCycles(int targetCycles) {
        totalCycles = targetCycles;
    }

    /**
     * Triggered at the start of a simulation run.
     * @param  comment allows user to add comments.
     * @return true    if successfully logged.
     */
    @Override
    public boolean runStarted(String comment) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry("Simulation run started. There will be " + totalCycles + " cycles in total." + " Comment: " + comment));
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered at the start of a cycle.
     * @param  cycleNumber the number of the cycle.
     * @return true    if successfully logged.
     */
    @Override
    public boolean cycleStarted(int cycleNumber) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry("Cycle " + cycleNumber + " of " + totalCycles + " started"));
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered when a cycle is completed.
     * @param  cycleNumber the number of the cycle.
     * @param  result      short result info string.
     * @return true        if successfully logged.
     */
    @Override
    public boolean cycleCompleted(int cycleNumber, String result) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry("Cycle " + cycleNumber + " of " + totalCycles + " finished. Result: " + result));
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered when config is initialised from a file.
     * @param configFile
     * @param  result   was the file readable?
     * @return true     if successfully logged.
     */
    @Override
    public boolean configRead(String configFile, boolean result) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            if (result) {
                bw.write(createLogEntry("Config read from " + configFile + " successfully"));
            } else {
                bw.write(createLogEntry("Error! Config could not be read from " + configFile));
            }
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered when a config tuple is set.
     * @param  key   key stored in map.
     * @param  value value stored in map.
     * @return true  if successfully logged.
     */
    @Override
    public boolean configSet(String key, String value) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry(key + " was set in config as " + value));
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered when a value is requested from config.
     * @param  key    key provided to map.
     * @param  value  value returned from map.
     * @param  result was the tuple found?
     * @return true   if successfully logged.
     */
    @Override
    public boolean configGet(String key, String value, boolean result) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            if (result) {
                bw.write(createLogEntry(key + " successfully retrieved from config. Result: " + value));
            } else {
                bw.write(createLogEntry("Warning! " + key + " was requested from config, but could not be found"));
            }
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Triggered when a non-recoverable error occurs during a cycle.
     * @param  cycleNumber the number of the cycle.
     * @param  ex          an exception object.
     * @return true        if successfully logged.
     */
    @Override
    public boolean fatalError(int cycleNumber, Exception ex) {
        StringBuffer stackTrace = new StringBuffer();
        StackTraceElement[] traceLines = ex.getStackTrace();
        for (int i = 0; i < traceLines.length; i++) {
            stackTrace.append("  -> ");
            stackTrace.append(traceLines[i].toString());
            stackTrace.append("\r\n");
        }
        String entryDetails = "Error! An exception occurred on cycle " + cycleNumber + ".";
        entryDetails += System.getProperty("line.separator");
        entryDetails += "  -> Details: " + ex.toString() + " " + ex.getMessage();
        entryDetails += System.getProperty("line.separator");
        entryDetails += stackTrace;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry(entryDetails));
            bw.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Triggered at the end of a simulation run.
     * @param comment
     * @return true    if successfully logged.
     */
    @Override
    public boolean runEnded(String comment) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(createLogEntry(comment));
            bw.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Create a log entry string, including the time stamp
     * and any entry text.
     * @param  entryText the text to use in the log entry.
     * @return full log entry string.
     */
    private String createLogEntry(String entryText) {
        SimpleDateFormat sdf = new SimpleDateFormat(ENTRY_STAMP_FORMAT);
        String entry = ENTRY_PREFIX + sdf.format(Calendar.getInstance().getTime()) + ENTRY_SUFFIX;
        entry += entryText;
        entry += ENTRY_TERMINATOR;
        return entry;
    }
}
