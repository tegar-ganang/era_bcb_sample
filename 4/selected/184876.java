package com.rbnb.inds.exec;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.xml.sax.Attributes;

/**
  * Base type for all execution commands.
  */
public abstract class Command {

    public Command(Attributes attr) {
        String temp;
        temp = attr.getValue("initialDirectory");
        if (temp == null || temp.length() == 0) initialDirectory = "."; else initialDirectory = temp;
        logFile = attr.getValue("logFile");
        tag = (temp = attr.getValue("tag")) == null ? "" : temp;
        id = getClass().getSimpleName() + '_' + (++commandCount);
        temp = attr.getValue("classification");
        if (temp == null || temp.length() == 0) classification = getCommandProperties().get("classification"); else classification = attr.getValue("classification");
        try {
            stdOutTempFile = File.createTempFile("exeman" + id, ".out.log");
            stdOutPagedFile = new PagedFile(stdOutTempFile, 1000);
            stdOutTempFile.deleteOnExit();
            localStdOutStream = new FileOutputStream(stdOutTempFile);
            stdErrTempFile = File.createTempFile("exeman" + id, ".err.log");
            stdErrTempFile.deleteOnExit();
            stdErrPagedFile = new PagedFile(stdErrTempFile, 1000);
            localStdErrStream = new FileOutputStream(stdErrTempFile);
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
	  * Perform cleanup operations on shutdown.
	  *
	  * @since 2009/03/10
	  */
    public final void cleanup() {
        try {
            localStdOutStream.close();
            localStdErrStream.close();
            stdOutTempFile.delete();
            stdErrTempFile.delete();
        } catch (java.io.IOException ioe) {
            System.err.println("WARNING: " + ioe.getMessage());
        }
    }

    /**
	  * Starts execution of a command.
	  *
	  * @return true if the command has completed synchronously; false if the
	  *  program continues to run.
	  */
    public final void startExecution() throws java.io.IOException {
        if (logFile != null) {
            logStream = new FileOutputStream(initialDirectory + "/" + logFile);
        }
        executionComplete = doExecute();
    }

    /**
	  * If the command has already stopped this fucntion does nothing.
	  */
    public final void stopExecution() {
        if (executionComplete) return;
        doKill();
        executionComplete = true;
    }

    public final void waitFor() throws InterruptedException {
        doWaitFor();
        executionComplete = true;
    }

    public final void addInput(Port p) {
        inputs.add(p);
    }

    public final void addOutput(Port p) {
        outputs.add(p);
    }

    /**
	  * Returns an unmodifiable view of the input connections.
	  */
    public final java.util.List<Port> getInputs() {
        return java.util.Collections.unmodifiableList(inputs);
    }

    /**
	  * Returns an unmodifiable view of the input connections.
	  */
    public final java.util.List<Port> getOutputs() {
        return java.util.Collections.unmodifiableList(outputs);
    }

    public final OutputStream getLocalStdOutStream() {
        return localStdOutStream;
    }

    public final OutputStream getLocalStdErrStream() {
        return localStdErrStream;
    }

    public final String getStdOutString(int pageSize, int page) {
        return stdOutPagedFile.getPage(pageSize, page);
    }

    public final String getStdErrString(int pageSize, int page) {
        return stdErrPagedFile.getPage(pageSize, page);
    }

    public int getCommandOutPageCount() {
        return stdOutPagedFile.getPageCount();
    }

    public int getCommandErrorPageCount() {
        return stdErrPagedFile.getPageCount();
    }

    public final String getInitialDirectory() {
        return initialDirectory;
    }

    public final String getLogfile() {
        return logFile;
    }

    public final String getId() {
        return id;
    }

    public final String getTag() {
        return tag;
    }

    public final boolean isExecutionComplete() {
        return executionComplete;
    }

    public final String getClassification() {
        return classification;
    }

    final java.io.OutputStream getLogStream() {
        return logStream;
    }

    public final String getXmlSnippet() {
        return xmlSnippet;
    }

    final void setXmlSnippet(String xmlSnippet) {
        this.xmlSnippet = xmlSnippet;
    }

    /**
	  * Obtain the map of keys to values for the specified Command subclass.
	  */
    protected final java.util.Map<String, String> getCommandProperties() {
        return commandProperties.get().get(getClass());
    }

    protected abstract boolean doExecute() throws java.io.IOException;

    protected void doKill() {
    }

    protected void doWaitFor() throws InterruptedException {
    }

    public InputStream getStdOut() {
        return null;
    }

    public InputStream getStdErr() {
        return null;
    }

    public abstract String getPrettyName();

    public String getChildConfiguration() {
        return "";
    }

    public String toString() {
        return xmlSnippet;
    }

    private final String initialDirectory, logFile, id, tag, classification;

    private String xmlSnippet;

    private java.io.OutputStream logStream;

    private final ArrayList<Port> inputs = new ArrayList<Port>(), outputs = new ArrayList<Port>();

    private final File stdOutTempFile, stdErrTempFile;

    private final PagedFile stdOutPagedFile, stdErrPagedFile;

    private final FileOutputStream localStdOutStream, localStdErrStream;

    private boolean executionComplete = false;

    public static void putCommandProperties(Class<? extends Command> c, java.util.Map<String, String> props) {
        commandProperties.get().put(c, props);
    }

    protected static String file2string(java.io.File file) {
        java.io.StringWriter sw = new java.io.StringWriter();
        char buff[] = new char[1024];
        try {
            java.io.FileReader fr = new java.io.FileReader(file);
            int nRead;
            while ((nRead = fr.read(buff)) > 0) sw.write(buff, 0, nRead);
            fr.close();
        } catch (java.io.IOException ioe) {
            return "CONFIG FILE READ FAILED!!!";
        }
        return sw.toString();
    }

    /**
	  * Incremented with each new command to generate unique names.
	  */
    private static int commandCount = 0;

    /**
	  * Messy, isn't it?  This is why C++ has typedefs.
	  */
    private static final ThreadLocal<java.util.Map<Class<? extends Command>, java.util.Map<String, String>>> commandProperties = new ThreadLocal<java.util.Map<Class<? extends Command>, java.util.Map<String, String>>>() {

        protected java.util.Map<Class<? extends Command>, java.util.Map<String, String>> initialValue() {
            return new java.util.HashMap<Class<? extends Command>, java.util.Map<String, String>>();
        }
    };
}
