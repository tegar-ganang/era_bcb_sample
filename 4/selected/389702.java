package marla.ide.r;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import marla.ide.resource.Configuration.ConfigType;
import marla.ide.resource.ConfigurationException;

/**
 * Interfaces with R through the command line interface
 * @author Ryan Morehart
 */
public final class RProcessor {

    /**
	 * Sent to R to force an output we can recognize as finishing the command set
	 */
    private final String SENTINEL_STRING_CMD = "print('---MARLA R OUTPUT END---')\n";

    /**
	 * Return from R for the command given in SENTINEL_STRING_CMD, we watch for this result
	 */
    private final String SENTINEL_STRING_RETURN = "[1] \"---MARLA R OUTPUT END---\"";

    /**
	 * Pattern used to recognize single R commands. Used by execute() to protect from
	 * hangs resulting from multiple commands being passed in. Does not allow
	 * strings with newlines in them, use \n instead.
	 */
    private final Pattern singleCmdPatt = Pattern.compile("^[^\\n;]+[\\n;]?$");

    /**
	 * Pattern used to recognize doubles in R output, mainly for use with vectors
	 */
    private final Pattern doublePatt = Pattern.compile("(?<=\\s)(-?[0-9]+(\\.[0-9]+)?(e[+-][0-9]+)?|NaN|-?Inf)(?=\\s|$)");

    /**
	 * Pattern used to recognize strings in R output, mainly for use with vectors
	 */
    private final Pattern stringPatt = Pattern.compile("\"(([^\\n]|\\\")*?)\"");

    /**
	 * Pattern used to recognize booleans in R output, mainly for use with vectors
	 */
    private final Pattern booleanPatt = Pattern.compile("(?<=\\s)(FALSE|TRUE)(?=\\s|$)");

    /**
	 * Path to the R executable, used if R has to be reloaded after it dies
	 */
    private static String rPath = null;

    /**
	 * Single instance of RProcessor that we allow
	 */
    private static RProcessor singleRProcessor = null;

    /**
	 * Directory that R is running from
	 */
    private String workingDirectory = null;

    /**
	 * The R process itself
	 */
    private Process rProc = null;

    /**
	 * The R process's output stream, where we get the results from
	 */
    private BufferedReader procOut = null;

    /**
	 * The R process's input stream, where we send commands to be run
	 */
    private BufferedOutputStream procIn = null;

    /**
	 * Synchronization variable
	 */
    private final Object processSync = new Object();

    /**
	 * Denotes the mode the RProcessor is
	 */
    private RecordMode recordMode = RecordMode.DISABLED;

    /**
	 * Denotes how/if the RProcessor should dump to console interactions with R
	 */
    private static RecordMode debugOutputMode = RecordMode.DISABLED;

    /**
	 * Record of output returned from R
	 */
    private StringBuilder interactionRecord = new StringBuilder();

    /**
	 * Path of the most recently output graphic
	 */
    private String lastPngName = null;

    /**
	 * Stores the next value to use for the "unique" name generator
	 */
    private long uniqueValCounter = 0;

    /**
	 * Enumeration denoting the record mode the R processor can use
	 */
    public enum RecordMode {

        DISABLED, CMDS_ONLY, OUTPUT_ONLY, FULL
    }

    ;

    /**
	 * Creates a new R instance that can be fed commands
	 * @param newRPath R executable to run
	 */
    private RProcessor(String newRPath) {
        try {
            if (newRPath == null) throw new ConfigurationException("R processor not configured yet", ConfigType.R);
            ProcessBuilder builder = new ProcessBuilder(rPath, "--slave", "--no-readline");
            builder.directory(new File(System.getProperty("java.io.tmpdir")));
            workingDirectory = builder.directory().getAbsolutePath();
            builder.redirectErrorStream(true);
            rProc = builder.start();
            procOut = new BufferedReader(new InputStreamReader(rProc.getInputStream()));
            procIn = (BufferedOutputStream) rProc.getOutputStream();
            execute("options(error=dump.frames, warn=-1, device=png)");
            Boolean isWorking = executeBoolean("1==1");
            if (!isWorking) throw new ConfigurationException("'" + rPath + "' does not appear to be a valid R installation", ConfigType.R);
        } catch (IOException ex) {
            throw new ConfigurationException("R could not be executed at '" + rPath + "'", ConfigType.R);
        }
    }

    /**
	 * Sets the default location to look for R
	 * @param newRPath New location of the R binary
	 * @return The previously assigned location of R
	 */
    public static String setRLocation(String newRPath) {
        String oldPath = rPath;
        try {
            rPath = newRPath;
            if (oldPath == null || !oldPath.equals(newRPath)) restartInstance();
        } catch (ConfigurationException ex) {
            rPath = oldPath;
            throw ex;
        }
        return oldPath;
    }

    /**
	 * Gets the currently set default location to look for R
	 * @return The assigned location of R
	 */
    public static String getRLocation() {
        return rPath;
    }

    /**
	 * Creates a new instance of R which can be fed commands. Assumes R is accessible on the path.
	 * If it isn't, RProcessor then searches for an installation alongside itself (in an
	 * R directory, so the R executable is at R/bin/R), then in common system install
	 * locations for Windows, Linux, and OSX.
	 * @return Instance of RProcessor that can be used for calculations
	 */
    public static RProcessor getInstance() {
        try {
            if (singleRProcessor == null) singleRProcessor = new RProcessor(rPath);
            return singleRProcessor;
        } catch (RProcessorException ex) {
            throw new ConfigurationException("R installation not found", ConfigType.R);
        }
    }

    /**
	 * Kills any existing instances of the RProcessor and starts a new one.
	 * @return Newly created RProcessor instance
	 */
    public static RProcessor restartInstance() {
        if (singleRProcessor != null) {
            singleRProcessor.close();
            singleRProcessor = null;
        }
        return getInstance();
    }

    /**
	 * Returns true if there is a running RProcessor instance
	 * @return true if there is an instance, false otherwise
	 */
    public static boolean hasInstance() {
        return (singleRProcessor != null);
    }

    /**
	 * Ensures that R is killed cleanly if at all possible
	 */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Exception ex) {
            rProc.destroy();
        } finally {
            super.finalize();
        }
    }

    /**
	 * Kills R process
	 */
    public void close() {
        try {
            if (!isRunning()) return;
            byte[] cmdArray = "q()".getBytes();
            procIn.write(cmdArray, 0, cmdArray.length);
            procIn.flush();
            procIn.close();
            procOut.close();
            rProc.waitFor();
        } catch (Exception ex) {
        } finally {
            rProc.destroy();
            procIn = null;
            procOut = null;
            System.gc();
        }
    }

    /**
	 * Checks if the current R process is still running and accessible
	 * @return true if the process may be used, false otherwise
	 */
    public boolean isRunning() {
        if (rProc == null || procIn == null || procOut == null) return false; else return true;
    }

    /**
	 * Passes the given string onto R just as if you typed it at the command line. Only a single
	 * command may be executed by this command. If the user wants to run multiple commands as a
	 * group, use execute(ArrayList<String>). Throws all errors/warnings as exceptions
	 * @param cmd R command to execute
	 * @return String output from R. Use one of the parse functions to processor further
	 */
    public String execute(String cmd) {
        return execute(cmd, false);
    }

    /**
	 * Passes the given string onto R just as if you typed it at the command line. Only a single
	 * command may be executed by this command. If the user wants to run multiple commands as a
	 * group, use execute(ArrayList<String>).
	 * @param cmd R command to execute
	 * @param ignoreErrors true if errors and warnings from R should be ignored and just
	 *		be returned with the rest of the output. If false, exceptions are thrown
	 *		on either occurrence
	 * @return String output from R. Use one of the parse functions to processor further
	 */
    public String execute(String cmd, boolean ignoreErrors) {
        if (!isRunning()) throw new RProcessorDeadException("R process has been closed.");
        Matcher m = singleCmdPatt.matcher(cmd);
        if (!m.matches()) throw new RProcessorException("execute() may only be given one command at a time");
        StringBuilder sentinelCmd = new StringBuilder(cmd.trim());
        sentinelCmd.append('\n');
        if (recordMode == RecordMode.CMDS_ONLY || recordMode == RecordMode.FULL) interactionRecord.append(sentinelCmd);
        if (debugOutputMode == RecordMode.CMDS_ONLY || debugOutputMode == RecordMode.FULL) System.out.print("> " + sentinelCmd);
        try {
            String results = null;
            boolean errorOccurred = false;
            synchronized (processSync) {
                sentinelCmd.append(this.SENTINEL_STRING_CMD);
                byte[] cmdArray = sentinelCmd.toString().getBytes();
                procIn.write(cmdArray, 0, cmdArray.length);
                procIn.flush();
                StringBuilder sb = new StringBuilder();
                String line = procOut.readLine();
                while (line != null && !line.equals(this.SENTINEL_STRING_RETURN)) {
                    sb.append(line);
                    sb.append('\n');
                    if (!ignoreErrors && (line.startsWith("Error") || line.startsWith("Warning"))) errorOccurred = true;
                    line = procOut.readLine();
                }
                results = sb.toString();
            }
            if (recordMode == RecordMode.OUTPUT_ONLY || recordMode == RecordMode.FULL) interactionRecord.append(results);
            if (debugOutputMode == RecordMode.OUTPUT_ONLY || debugOutputMode == RecordMode.FULL) System.out.print(results);
            if (errorOccurred) throw new RProcessorException("R: " + results);
            return results;
        } catch (IOException ex) {
            close();
            throw new RProcessorException("Unable to read or write to the R instance", ex);
        }
    }

    /**
	 * Calls execute(String) for each of the commands given in the cmds array. Commands will
	 * be automatically terminated with a newline if they does not have one.
	 * @param cmds List of R commands to execute
	 * @return ArrayList of Strings, where each entry is the output from one of the commands given.
	 */
    public List<String> execute(List<String> cmds) {
        List<String> output = new ArrayList<String>(cmds.size());
        for (String cmd : cmds) {
            output.add(execute(cmd));
        }
        return output;
    }

    /**
	 * Convenience function that executes the given command and parses the result as a single double
	 * value. An exception is thrown if there is not exactly one double in the output.
	 * @param cmd R command to execute
	 * @return Double value of the R call
	 */
    public Double executeDouble(String cmd) {
        return parseDouble(execute(cmd));
    }

    /**
	 * Convenience function that executes the given command and parses the result as a vector
	 * of doubles. An exception is thrown if the output does not contain numerical values.
	 * @param cmd R command to execute
	 * @return ArrayList of doubles that the R command returned
	 */
    public List<Double> executeDoubleArray(String cmd) {
        return parseDoubleArray(execute(cmd));
    }

    /**
	 * Convenience function that executes the given command and parses the result as a string. An
	 * exception is thrown if there is not exactly one string in the output.
	 * @param cmd R command to execute
	 * @return String value of the R call
	 */
    public String executeString(String cmd) {
        return parseString(execute(cmd));
    }

    /**
	 * Convenience function that executes the given command and parses the result as a vector of
	 * strings. An exception is thrown if there are no strings in the output.
	 * @param cmd R command to execute
	 * @return ArrayList of strings that the R command returned
	 */
    public List<String> executeStringArray(String cmd) {
        return parseStringArray(execute(cmd));
    }

    /**
	 * Convenience function that executes the given command and parses the result as a boolean. An
	 * exception is thrown if there is not exactly one boolean in the output.
	 * @param cmd R command to execute
	 * @return String value of the R call
	 */
    public Boolean executeBoolean(String cmd) {
        return parseBoolean(execute(cmd));
    }

    /**
	 * Convenience function that executes the given command and parses the result as a vector of
	 * booleans. An exception is thrown if there are no booleans in the output.
	 * @param cmd R command to execute
	 * @return ArrayList of strings that the R command returned
	 */
    public List<Boolean> executeBooleanArray(String cmd) {
        return parseBooleanArray(execute(cmd));
    }

    /**
	 * Runs the given command and saves it into a new, unique variable. The variable name used
	 * is returned as a string. Only one command may be given, an exception is thrown if this
	 * isn't true.
	 * @param cmd R command to execute
	 * @return R variable name that contains the results of the executed command
	 */
    public String executeSave(String cmd) {
        String varName = getUniqueName();
        execute(varName + " = " + cmd);
        return varName;
    }

    /**
	 * Takes the given R output and attempts to parse it as a single double value. An exception is
	 * thrown if there isn't exactly one numerical value in the output.
	 * @param rOutput R output, as returned by execute(String)
	 * @return Double value contained in the output
	 */
    public Double parseDouble(String rOutput) {
        ArrayList<Double> arr = parseDoubleArray(rOutput);
        if (arr.size() != 1) throw new RProcessorParseException("The R result was not a single double value");
        return arr.get(0);
    }

    /**
	 * Takes the given R output and attempts to parse it as a vector of doubles. An exception is
	 * thrown if there are no numerical values.
	 * @param rOutput R output, as returned by execute(String)
	 * @return ArrayList of Doubles from the output
	 */
    public ArrayList<Double> parseDoubleArray(String rOutput) {
        ArrayList<Double> vals = new ArrayList<Double>();
        try {
            Matcher m = doublePatt.matcher(rOutput);
            while (m.find()) {
                String d = m.group();
                if (d.endsWith("Inf")) vals.add(Double.POSITIVE_INFINITY); else if (d.endsWith("-Inf")) vals.add(Double.NEGATIVE_INFINITY); else vals.add(Double.valueOf(d));
            }
        } catch (NumberFormatException ex) {
            throw new RProcessorParseException("The R result did not contain numeric values");
        }
        if (vals.isEmpty()) throw new RProcessorParseException("The R result is not a vector of doubles");
        return vals;
    }

    /**
	 * Takes the given R output and attempts to parse it as a single string value. An exception
	 * is thrown if there isn't exactly one string value in the output.
	 * @param rOutput R output, as returned by execute(String)
	 * @return String value contained in the output
	 */
    public String parseString(String rOutput) {
        ArrayList<String> arr = parseStringArray(rOutput);
        if (arr.size() != 1) throw new RProcessorParseException("The R result was not a single string value");
        return arr.get(0);
    }

    /**
	 * Takes the given R output and attempts to parse it as a vector of strings. An exception is
	 * thrown if the output contains no strings.
	 * @param rOutput R output, as returned by execute(String)
	 * @return ArrayList of Strings from the output
	 */
    public ArrayList<String> parseStringArray(String rOutput) {
        ArrayList<String> vals = new ArrayList<String>();
        Matcher m = stringPatt.matcher(rOutput);
        while (m.find()) {
            vals.add(m.group(1));
        }
        if (vals.isEmpty()) throw new RProcessorParseException("The R result is not a vector of strings");
        return vals;
    }

    /**
	 * Takes the given R output and attempts to parse it as a single string value. An exception
	 * is thrown if there isn't exactly one string value in the output.
	 * @param rOutput R output, as returned by execute(String)
	 * @return String value contained in the output
	 */
    public Boolean parseBoolean(String rOutput) {
        List<Boolean> arr = parseBooleanArray(rOutput);
        if (arr.size() != 1) throw new RProcessorParseException("The R result was not a single boolean value");
        return arr.get(0);
    }

    /**
	 * Takes the given R output and attempts to parse it as a vector of strings. An exception is
	 * thrown if the output contains no strings.
	 * @param rOutput R output, as returned by execute(String)
	 * @return ArrayList of Strings from the output
	 */
    public List<Boolean> parseBooleanArray(String rOutput) {
        List<Boolean> vals = new ArrayList<Boolean>();
        try {
            Matcher m = booleanPatt.matcher(rOutput);
            while (m.find()) {
                vals.add(Boolean.valueOf(m.group(1)));
            }
        } catch (NumberFormatException ex) {
            throw new RProcessorParseException("The R result did not contain boolean values");
        }
        if (vals.isEmpty()) throw new RProcessorParseException("The R result is not a vector of booleans");
        return vals;
    }

    /**
	 * Sets the given variable with the value given
	 * @param val Value to store in the variable
	 * @return Name of the variable used
	 */
    public String setVariable(Object val) {
        return setVariable(getUniqueName(), val);
    }

    /**
	 * Sets the given variable with the value given
	 * @param name R-conforming variable name
	 * @param val Value to store in the variable
	 * @return Name of the variable used
	 */
    public String setVariable(String name, Object val) {
        if (val instanceof Double) {
            Double dVal = (Double) val;
            if (dVal == Double.POSITIVE_INFINITY) execute(name + " = Inf"); else if (dVal == Double.NEGATIVE_INFINITY) execute(name + " = -Inf"); else execute(name + " = " + val);
        } else if (val instanceof Boolean) execute(name + " = " + val.toString().toUpperCase()); else execute(name + " = \"" + val + '"');
        return name;
    }

    /**
	 * Sets a new unique variable with a vector of the values given
	 * @param vals Array of values to store in the variable
	 * @return Name of the variable used
	 */
    public String setVariable(List<Object> vals) {
        return setVariable(getUniqueName(), vals);
    }

    /**
	 * Sets the given variable with a vector of the values given. Values may be either
	 * Doubles or Strings (anything unrecognized is assumed to be a string).
	 * @param name R-conforming variable name
	 * @param vals Array of values to store in the variable
	 * @return Name of the variable used
	 */
    public String setVariable(String name, List<Object> vals) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(name);
        cmd.append(" = c(");
        if (!vals.isEmpty()) {
            if (vals.get(0) instanceof Double) {
                for (Object val : vals) {
                    Double dVal = (Double) val;
                    if (dVal == Double.POSITIVE_INFINITY) cmd.append("Inf"); else if (dVal == Double.NEGATIVE_INFINITY) cmd.append("-Inf"); else cmd.append(val);
                    cmd.append(", ");
                }
            } else if (vals.get(0) instanceof Boolean) {
                for (Object val : vals) {
                    cmd.append(val.toString().toUpperCase());
                    cmd.append(", ");
                }
            } else {
                for (Object val : vals) {
                    cmd.append('"');
                    cmd.append(val);
                    cmd.append("\", ");
                }
            }
            cmd.replace(cmd.length() - 2, cmd.length() - 1, "");
        }
        cmd.append(")\n");
        execute(cmd.toString());
        return name;
    }

    /**
	 * Creates a new graphic device with the necessary options for passing. An exception is thrown
	 * if the device creation fails.
	 * back to the GUI. Returns the path to the file that will hold the output.
	 * @return Path where the new graphics device will write to
	 */
    public String startGraphicOutput() {
        lastPngName = getUniqueName() + ".png";
        execute("png(filename='" + lastPngName + "')");
        return new File(workingDirectory + "/" + lastPngName).getAbsolutePath();
    }

    /**
	 * Stops the current graphic device, flushing it to disk.
	 * @return Path where the new graphic has been written to
	 */
    public String stopGraphicOutput() {
        String pngName = lastPngName;
        lastPngName = null;
        execute("dev.off()");
        File f = new File(workingDirectory + "/" + pngName);
        f.deleteOnExit();
        return f.getAbsolutePath();
    }

    /**
	 * Returns a list of the names of libraries currently installed in the 
	 * user's R installation
	 * @return List of library names, suitable for passing to loadLibrary
	 */
    public List<String> getLibraryList() {
        List<String> libs = new ArrayList<String>();
        String list = execute("library()");
        throw new RProcessorException("Not yet implemented");
    }

    /**
	 * Loads the given library into R. If it is not installed, attempts to 
	 * automatically install it
	 * @param lib Name of the library to load
	 * @return true if load succeeds, false otherwise
	 */
    public boolean loadLibrary(String lib) {
        Boolean loaded = false;
        try {
            loaded = executeBoolean("library('" + lib + "', logical.return=T)");
        } catch (RProcessorException ex) {
            if (ex.getMessage().contains("no package")) loaded = false; else throw ex;
        }
        if (!loaded) {
            try {
                execute("install.packages('" + lib + "', repos='http://cran.r-project.org')");
                loaded = executeBoolean("library('" + lib + "', logical.return=T)");
            } catch (RProcessorException ex) {
                loaded = false;
            }
        }
        return loaded;
    }

    /**
	 * Sets the recording mode for the processor
	 * @param mode RecordMode to place the processor in.
	 * @return The mode the RProcessor was in before the switch
	 */
    public RecordMode setRecorderMode(RecordMode mode) {
        RecordMode oldMode = recordMode;
        recordMode = mode;
        return oldMode;
    }

    /**
	 * Returns the current processor recording mode
	 * @return The mode the RProcessor is currently in
	 */
    public RecordMode getRecorderMode() {
        return recordMode;
    }

    /**
	 * Sets how much the processor should output to the console. Useful debugging operations
	 * @param mode RecordMode to place the processor in.
	 * @return The mode the RProcessor was in before the switch
	 */
    public static RecordMode setDebugMode(RecordMode mode) {
        RecordMode oldMode = debugOutputMode;
        debugOutputMode = mode;
        return oldMode;
    }

    /**
	 * Returns how much the processor is outputting to the console
	 * @return The mode the RProcessor is currently in
	 */
    public static RecordMode getDebugMode() {
        return debugOutputMode;
    }

    /**
	 * Retrieves the recorded input and output with R since the last fetch
	 * @return String of all the commands and their output executed since the last fetch
	 */
    public String fetchInteraction() {
        String sent = interactionRecord.toString();
        interactionRecord = new StringBuilder();
        return sent;
    }

    /**
	 * Returns a unique variable name for use in this R instance
	 * @return New unique name
	 */
    public String getUniqueName() {
        uniqueValCounter++;
        if (uniqueValCounter < 0) uniqueValCounter = 0;
        return "marlaUnique" + uniqueValCounter;
    }

    /**
	 * Allows for direct testing of the RProcessor execute() function
	 */
    public static void main(String[] args) {
        RProcessor proc = null;
        try {
            proc = RProcessor.getInstance();
            RProcessor.setDebugMode(RecordMode.FULL);
            proc.setRecorderMode(RecordMode.DISABLED);
            Scanner sc = new Scanner(System.in);
            while (proc.isRunning()) {
                System.out.print("CMD: ");
                String line = sc.nextLine();
                try {
                    proc.execute(line);
                } catch (RProcessorException ex) {
                    System.out.println("Ex handler: " + ex.getMessage());
                }
            }
        } finally {
            proc.close();
        }
    }
}
