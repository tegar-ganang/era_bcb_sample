package org.hsqldb.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Utilities that test classes can call to execute a specified command and to
 * evaluate the exit status and output of said execution.
 * harnessInstance.exec() executes the given program (Java or not).
 * Any time thereafter, harnessInstance can be interrogated for exit
 * status and text output.
 *
 * ExecHarness can emulate user interaction with SqlTool, but you can
 * not use ExecHarness interactively.
 *
 * To execute java classes, you can either give the classpath by setting the
 * environmental var before running this program, or by giving the classpath
 * switch to the target program.  Classpath switches used for invoking
 * this ExecHarness class WILL NOT EFFECT java executions by ExecHarness.
 * E.g. the java invocation
 * "java org.hsqldb.test.ExecHarness java -cp newcp Cname" will give Cname
 * classpath of 'newcp', but the following WILL NOT:
 * "java -cp newcp org.hsqldb.test.ExecHarness java Cname".
 * It's often easier to just set (and export if necessary) CLASSPATH before
 * invoking ExecHarness.
 *
 * Same applies to java System Properties.  You must supply them after the
 * 2nd "java".
 *
 * @see main() for an example of use.
 */
public class ExecHarness {

    private static final String SYNTAX_MSG = "SYNTAX:  java org.hsqldb.test.ExecHarness targetprogram [args...]";

    private static final int MAX_PROG_OUTPUT = 10240;

    /**
     * To test the ExecHarness class itself.
     * (Basically, a sanity check).
     *
     * Note that we always exec another process.  This makes it safe to
     * execute Java classes which may call System.exit().
     *
     * @param sa sa[0] is the program to be run.
     *           Remaining arguments will be passed as command-line args
     *           to the sa[0] program.
     */
    public static void main(String[] sa) throws IOException, FileNotFoundException, InterruptedException {
        byte[] localBa = new byte[10240];
        if (sa.length < 1) {
            System.err.println(SYNTAX_MSG);
            System.exit(1);
        }
        String progname = sa[0];
        System.err.println("Enter any input that you want passed to SqlTool via stdin\n" + "(end with EOF, like Ctrl-D or Ctrl-Z+ENTER):");
        File tmpFile = File.createTempFile("ExecHarness-", ".input");
        String specifiedCharSet = System.getProperty("harness.charset");
        String charset = ((specifiedCharSet == null) ? DEFAULT_CHARSET : specifiedCharSet);
        FileOutputStream fos = new FileOutputStream(tmpFile);
        int i;
        while ((i = System.in.read(localBa)) > 0) {
            fos.write(localBa, 0, i);
        }
        fos.close();
        ExecHarness harness = new ExecHarness(progname);
        harness.setArgs(shift(sa));
        harness.setInput(tmpFile);
        harness.exec();
        tmpFile.delete();
        int retval = harness.getExitValue();
        System.err.println("STDOUT ******************************************");
        System.out.print(harness.getStdout());
        System.err.println("ERROUT ******************************************");
        System.err.print(harness.getErrout());
        System.err.println("*************************************************");
        System.err.println(progname + " exited with value " + retval);
        harness.clear();
        System.exit(retval);
    }

    File input = null;

    String program = null;

    int exitValue = 0;

    boolean executed = false;

    String[] mtStringArray = {};

    String[] args = mtStringArray;

    private byte[] ba = new byte[MAX_PROG_OUTPUT + 1];

    private String stdout = null;

    private String errout = null;

    private static final String DEFAULT_CHARSET = "US-ASCII";

    public void exec() throws IOException, InterruptedException {
        InputStream stream;
        int i;
        int writePointer;
        if (executed) {
            throw new IllegalStateException("You have already executed '" + program + "'.  Run clear().");
        }
        Process proc = Runtime.getRuntime().exec(unshift(program, args));
        OutputStream outputStream = proc.getOutputStream();
        if (input != null) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(input));
            while ((i = bis.read(ba)) > 0) {
                outputStream.write(ba, 0, i);
            }
        }
        outputStream.close();
        stream = proc.getInputStream();
        writePointer = 0;
        while ((i = stream.read(ba, writePointer, ba.length - writePointer)) > 0) {
            writePointer += i;
        }
        if (i > -1) {
            throw new IOException(program + " generated > " + (ba.length - 1) + " bytes of standard output");
        }
        stream.close();
        executed = true;
        stdout = new String(ba, 0, writePointer);
        stream = proc.getErrorStream();
        writePointer = 0;
        while ((i = stream.read(ba, writePointer, ba.length - writePointer)) > 0) {
            writePointer += i;
        }
        if (i > -1) {
            throw new IOException(program + " generated > " + (ba.length - 1) + " bytes of error output");
        }
        stream.close();
        errout = new String(ba, 0, writePointer);
        exitValue = proc.waitFor();
    }

    public void clear() {
        args = mtStringArray;
        executed = false;
        stdout = errout = null;
        input = null;
    }

    public String getStdout() {
        return stdout;
    }

    public String getErrout() {
        return errout;
    }

    /**
     * @param inFile  There is no size limit on the input file.
     */
    public void setInput(File inFile) throws IllegalStateException {
        if (executed) {
            throw new IllegalStateException("You have already executed '" + program + "'.  Run clear().");
        }
        input = inFile;
    }

    public void setArgs(String[] inArgs) throws IllegalStateException {
        if (executed) {
            throw new IllegalStateException("You have already executed '" + program + "'.  Run clear().");
        }
        args = inArgs;
    }

    public void setArgs(List list) throws IllegalStateException {
        setArgs(listToPrimitiveArray(list));
    }

    int getExitValue() throws IllegalStateException {
        if (!executed) {
            throw new IllegalStateException("You have not executed '" + program + "' yet");
        }
        return exitValue;
    }

    /**
     * Create an ExecHarness instance which can invoke the given program.
     *
     * @param inName Name of the external program (like "cat" or "java").
     */
    public ExecHarness(String inName) {
        program = inName;
    }

    /**
     * These utility methods really belong in a class in the util package.
     */
    public static String[] unshift(String newHead, String[] saIn) {
        String[] saOut = new String[saIn.length + 1];
        saOut[0] = newHead;
        for (int i = 1; i < saOut.length; i++) {
            saOut[i] = saIn[i - 1];
        }
        return saOut;
    }

    public static String[] shift(String[] saIn) {
        String[] saOut = new String[saIn.length - 1];
        for (int i = 0; i < saOut.length; i++) {
            saOut[i] = saIn[i + 1];
        }
        return saOut;
    }

    public static String[] listToPrimitiveArray(List list) {
        String[] saOut = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            saOut[i] = (String) list.get(i);
        }
        return saOut;
    }

    public static String[] push(String newTail, String[] saIn) {
        String[] saOut = new String[saIn.length + 1];
        for (int i = 0; i < saIn.length; i++) {
            saOut[i] = saIn[i];
        }
        saOut[saOut.length - 1] = newTail;
        return saOut;
    }

    public static String[] pop(String[] saIn) {
        String[] saOut = new String[saIn.length - 1];
        for (int i = 0; i < saOut.length; i++) {
            saOut[i] = saIn[i];
        }
        return saOut;
    }

    public static String stringArrayToString(String[] sa) {
        StringBuffer sb = new StringBuffer("{");
        for (int i = 0; i < sa.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(sa[i]);
        }
        return sb.toString() + '}';
    }
}
