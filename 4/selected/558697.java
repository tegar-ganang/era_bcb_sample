package ca.ucalgary.ebe.j3dperfunit.anttask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Assertions;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LoaderUtils;

public class J3DPerfUnitTask extends Task {

    private CommandlineJava commandline = new CommandlineJava();

    private Vector tests = new Vector();

    private Vector batchTests = new Vector();

    private Vector formatters = new Vector();

    private File vmDir = null;

    private Integer timeout = null;

    private boolean summary = false;

    private boolean reloading = true;

    private String summaryValue = "";

    private boolean newEnvironment = false;

    private Environment env = new Environment();

    private boolean includeAntRuntime = true;

    private Path antRuntimeClasses = null;

    private boolean showOutput = false;

    private File tmpDir;

    private AntClassLoader classLoader = null;

    private Permissions perm = null;

    private static final int STRING_BUFFER_SIZE = 128;

    /**
	    * If true, force ant to re-classload all classes for each JUnit TestCase
	    *
	    * @param value force class reloading for each test case
	    */
    public void setReloading(boolean value) {
        reloading = value;
    }

    /**
	     * If true, smartly filter the stack frames of
	     * JUnit errors and failures before reporting them.
	     *
	     * <p>This property is applied on all BatchTest (batchtest) and
	     * J3DPerfUnitTest (test) however it can possibly be overridden by their
	     * own properties.</p>
	     * @param value <tt>false</tt> if it should not filter, otherwise
	     * <tt>true<tt>
	     *
	     * @since Ant 1.5
	     */
    public void setFiltertrace(boolean value) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setFiltertrace(value);
        }
    }

    /**
	     * If true, stop the build process when there is an error in a test.
	     * This property is applied on all BatchTest (batchtest) and J3DPerfUnitTest
	     * (test) however it can possibly be overridden by their own
	     * properties.
	     * @param value <tt>true</tt> if it should halt, otherwise
	     * <tt>false</tt>
	     *
	     * @since Ant 1.2
	     */
    public void setHaltonerror(boolean value) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setHaltonerror(value);
        }
    }

    /**
	     * Property to set to "true" if there is a error in a test.
	     *
	     * <p>This property is applied on all BatchTest (batchtest) and
	     * J3DPerfUnitTest (test), however, it can possibly be overriden by
	     * their own properties.</p>
	     * @param propertyName the name of the property to set in the
	     * event of an error.
	     *
	     * @since Ant 1.4
	     */
    public void setErrorProperty(String propertyName) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setErrorProperty(propertyName);
        }
    }

    /**
	     * If true, stop the build process if a test fails
	     * (errors are considered failures as well).
	     * This property is applied on all BatchTest (batchtest) and
	     * J3DPerfUnitTest (test) however it can possibly be overridden by their
	     * own properties.
	     * @param value <tt>true</tt> if it should halt, otherwise
	     * <tt>false</tt>
	     *
	     * @since Ant 1.2
	     */
    public void setHaltonfailure(boolean value) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setHaltonfailure(value);
        }
    }

    /**
	     * Property to set to "true" if there is a failure in a test.
	     *
	     * <p>This property is applied on all BatchTest (batchtest) and
	     * J3DPerfUnitTest (test), however, it can possibly be overriden by
	     * their own properties.</p>
	     * @param propertyName the name of the property to set in the
	     * event of an failure.
	     *
	     * @since Ant 1.4
	     */
    public void setFailureProperty(String propertyName) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setFailureProperty(propertyName);
        }
    }

    /**
	     * If true, JVM should be forked for each test.
	     *
	     * <p>It avoids interference between testcases and possibly avoids
	     * hanging the build.  this property is applied on all BatchTest
	     * (batchtest) and J3DPerfUnitTest (test) however it can possibly be
	     * overridden by their own properties.</p>
	     * @param value <tt>true</tt> if a JVM should be forked, otherwise
	     * <tt>false</tt>
	     * @see #setTimeout
	     *
	     * @since Ant 1.2
	     */
    public void setFork(boolean value) {
        Enumeration e = allTests();
        while (e.hasMoreElements()) {
            BaseTest test = (BaseTest) e.nextElement();
            test.setFork(value);
        }
    }

    /**
	     * If true, print one-line statistics for each test, or "withOutAndErr"
	     * to also show standard output and error.
	     *
	     * Can take the values on, off, and withOutAndErr.
	     * @param value <tt>true</tt> to print a summary,
	     * <tt>withOutAndErr</tt> to include the test&apos;s output as
	     * well, <tt>false</tt> otherwise.
	     * @see SummaryJ3DPerfUnitResultFormatter
	     *
	     * @since Ant 1.2
	     */
    public void setPrintsummary(SummaryAttribute value) {
        summaryValue = value.getValue();
        summary = value.asBoolean();
    }

    /**
	     * Print summary enumeration values.
	     */
    public static class SummaryAttribute extends EnumeratedAttribute {

        /**
	         * list the possible values
	         * @return  array of allowed values
	         */
        public String[] getValues() {
            return new String[] { "true", "yes", "false", "no", "on", "off", "withOutAndErr" };
        }

        /**
	         * gives the boolean equivalent of the authorized values
	         * @return boolean equivalent of the value
	         */
        public boolean asBoolean() {
            String value = getValue();
            return "true".equals(value) || "on".equals(value) || "yes".equals(value) || "withOutAndErr".equals(value);
        }
    }

    /**
	     * Set the timeout value (in milliseconds).
	     *
	     * <p>If the test is running for more than this value, the test
	     * will be canceled. (works only when in 'fork' mode).</p>
	     * @param value the maximum time (in milliseconds) allowed before
	     * declaring the test as 'timed-out'
	     * @see #setFork(boolean)
	     *
	     * @since Ant 1.2
	     */
    public void setTimeout(Integer value) {
        timeout = value;
    }

    /**
	     * Set the maximum memory to be used by all forked JVMs.
	     * @param   max     the value as defined by <tt>-mx</tt> or <tt>-Xmx</tt>
	     *                  in the java command line options.
	     *
	     * @since Ant 1.2
	     */
    public void setMaxmemory(String max) {
        commandline.setMaxmemory(max);
    }

    /**
	     * The command used to invoke the Java Virtual Machine,
	     * default is 'java'. The command is resolved by
	     * java.lang.Runtime.exec(). Ignored if fork is disabled.
	     *
	     * @param   value   the new VM to use instead of <tt>java</tt>
	     * @see #setFork(boolean)
	     *
	     * @since Ant 1.2
	     */
    public void setJvm(String value) {
        commandline.setVm(value);
    }

    /**
	     * Adds a JVM argument; ignored if not forking.
	     *
	     * @return create a new JVM argument so that any argument can be
	     * passed to the JVM.
	     * @see #setFork(boolean)
	     *
	     * @since Ant 1.2
	     */
    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }

    /**
	     * The directory to invoke the VM in. Ignored if no JVM is forked.
	     * @param   dir     the directory to invoke the JVM from.
	     * @see #setFork(boolean)
	     *
	     * @since Ant 1.2
	     */
    public void setVMDir(File vmDir) {
        this.vmDir = vmDir;
    }

    /**
	     * Adds a system property that tests can access.
	     * This might be useful to tranfer Ant properties to the
	     * testcases when JVM forking is not enabled.
	     *
	     * @since Ant 1.3
	     * @deprecated since ant 1.6
	     * @param sysp environment variable to add
	     */
    public void addSysproperty(Environment.Variable sysp) {
        commandline.addSysproperty(sysp);
    }

    /**
	     * Adds a system property that tests can access.
	     * This might be useful to tranfer Ant properties to the
	     * testcases when JVM forking is not enabled.
	     * @param sysp new environment variable to add
	     * @since Ant 1.6
	     */
    public void addConfiguredSysproperty(Environment.Variable sysp) {
        String testString = sysp.getContent();
        getProject().log("sysproperty added : " + testString, Project.MSG_DEBUG);
        commandline.addSysproperty(sysp);
    }

    /**
	     * Adds a set of properties that will be used as system properties
	     * that tests can access.
	     *
	     * This might be useful to tranfer Ant properties to the
	     * testcases when JVM forking is not enabled.
	     *
	     * @param sysp set of properties to be added
	     * @since Ant 1.6
	     */
    public void addSyspropertyset(PropertySet sysp) {
        commandline.addSyspropertyset(sysp);
    }

    /**
	     * Adds path to classpath used for tests.
	     *
	     * @return reference to the classpath in the embedded java command line
	     * @since Ant 1.2
	     */
    public Path createClasspath() {
        return commandline.createClasspath(getProject()).createPath();
    }

    /**
	     * Adds a path to the bootclasspath.
	     * @return reference to the bootclasspath in the embedded java command line
	     * @since Ant 1.6
	     */
    public Path createBootclasspath() {
        return commandline.createBootclasspath(getProject()).createPath();
    }

    /**
	     * Adds an environment variable; used when forking.
	     *
	     * <p>Will be ignored if we are not forking a new VM.</p>
	     * @param var environment variable to be added
	     * @since Ant 1.5
	     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
	     * If true, use a new environment when forked.
	     *
	     * <p>Will be ignored if we are not forking a new VM.</p>
	     *
	     * @param newenv boolean indicating if setting a new environment is wished
	     * @since Ant 1.5
	     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
	     * Add a new single testcase.
	     * @param   test    a new single testcase
	     * @see J3DPerfUnitTest
	     *
	     * @since Ant 1.2
	     */
    public void addTest(J3DPerfUnitTest test) {
        tests.addElement(test);
    }

    /**
	     * Adds a set of tests based on pattern matching.
	     *
	     * @return  a new instance of a batch test.
	     * @see BatchTest
	     *
	     * @since Ant 1.2
	     */
    public BatchTest createBatchTest() {
        BatchTest test = new BatchTest(getProject());
        batchTests.addElement(test);
        return test;
    }

    /**
	     * If true, include ant.jar, optional.jar and junit.jar in the forked VM.
	     *
	     * @param b include ant run time yes or no
	     * @since Ant 1.5
	     */
    public void setIncludeantruntime(boolean b) {
        includeAntRuntime = b;
    }

    /**
	     * If true, send any output generated by tests to Ant's logging system
	     * as well as to the formatters.
	     * By default only the formatters receive the output.
	     *
	     * <p>Output will always be passed to the formatters and not by
	     * shown by default.  This option should for example be set for
	     * tests that are interactive and prompt the user to do
	     * something.</p>
	     *
	     * @param showOutput if true, send output to Ant's logging system too
	     * @since Ant 1.5
	     */
    public void setShowOutput(boolean showOutput) {
        this.showOutput = showOutput;
    }

    /**
	     * Assertions to enable in this program (if fork=true)
	     * @since Ant 1.6
	     * @param asserts assertion set
	     */
    public void addAssertions(Assertions asserts) {
        if (commandline.getAssertions() != null) {
            throw new BuildException("Only one assertion declaration is allowed");
        }
        commandline.setAssertions(asserts);
    }

    /**
	     * Sets the permissions for the application run inside the same JVM.
	     * @since Ant 1.6
	     * @return .
	     */
    public Permissions createPermissions() {
        if (perm == null) {
            perm = new Permissions();
        }
        return perm;
    }

    /**
	     * Creates a new JUnitRunner and enables fork of a new Java VM.
	     *
	     * @throws Exception under ??? circumstances
	     * @since Ant 1.2
	     */
    public J3DPerfUnitTask() throws Exception {
    }

    /**
	     * Where Ant should place temporary files.
	     *
	     * @param tmpDir location where temporary files should go to
	     * @since Ant 1.6
	     */
    public void setTempdir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    /**
	     * Adds the jars or directories containing Ant, this task and
	     * JUnit to the classpath - this should make the forked JVM work
	     * without having to specify them directly.
	     *
	     * @since Ant 1.4
	     */
    public void init() {
        antRuntimeClasses = new Path(getProject());
        addClasspathEntry("/org/apache/tools/ant/launch/AntMain.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
    }

    /**
	     * Runs the testcase.
	     *
	     * @throws BuildException in case of test failures or errors
	     * @since Ant 1.2
	     */
    public void execute() throws BuildException {
        Enumeration list = getIndividualTests();
        String srcRptFile = "";
        String destRptFile = "";
        while (list.hasMoreElements()) {
            J3DPerfUnitTest test = (J3DPerfUnitTest) list.nextElement();
            if (test.shouldRun(getProject())) {
                try {
                    execute(test);
                    try {
                        srcRptFile = getProject().resolveFile(".").getAbsolutePath() + "\\" + test.getName() + ".txt";
                        destRptFile = getProject().resolveFile(test.getName() + ".txt", new File(test.getTodir())).getAbsolutePath();
                        FileChannel srcChannel = new FileInputStream(srcRptFile).getChannel();
                        FileChannel dstChannel = new FileOutputStream(destRptFile).getChannel();
                        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                        srcChannel.close();
                        dstChannel.close();
                        if (!srcRptFile.equals(destRptFile)) {
                            new File(srcRptFile).delete();
                        }
                    } catch (IOException e) {
                    }
                } catch (BuildException e) {
                    e.printStackTrace();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	     * Run the tests.
	     * @param arg one JunitTest
	     * @throws BuildException in case of test failures or errors
	     * @throws CloneNotSupportedException 
	     */
    protected void execute(J3DPerfUnitTest arg) throws BuildException, CloneNotSupportedException {
        J3DPerfUnitTest test = (J3DPerfUnitTest) arg.clone();
        if (test.getTodir() == null) {
            test.setTodir(getProject().resolveFile("."));
        }
        if (test.getOutfile() == null) {
            test.setOutfile("TEST-" + test.getName());
        }
        int exitValue = 0;
        boolean wasKilled = false;
        ExecuteWatchdog watchdog = createWatchdog();
        exitValue = executeAsForked(test, watchdog);
        if (watchdog != null) {
            wasKilled = watchdog.killedProcess();
        }
    }

    /**
	     * Execute a testcase by forking a new JVM. The command will block until
	     * it finishes. To know if the process was destroyed or not, use the
	     * <tt>killedProcess()</tt> method of the watchdog class.
	     * @param  test       the testcase to execute.
	     * @param  watchdog   the watchdog in charge of cancelling the test if it
	     * exceeds a certain amount of time. Can be <tt>null</tt>, in this case
	     * the test could probably hang forever.
	     * @throws BuildException in case of error creating a temporary property file,
	     * or if the junit process can not be forked
	     * @throws CloneNotSupportedException 
	     */
    private int executeAsForked(J3DPerfUnitTest test, ExecuteWatchdog watchdog) throws BuildException, CloneNotSupportedException {
        if (perm != null) {
            log("Permissions ignored when running in forked mode!", Project.MSG_WARN);
        }
        CommandlineJava cmd = (CommandlineJava) commandline.clone();
        cmd.setClassname(test.getName());
        System.out.println(cmd.describeCommand());
        System.out.println(cmd.describeJavaCommand());
        if (includeAntRuntime) {
            Vector v = Execute.getProcEnvironment();
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                if (s.startsWith("CLASSPATH=")) {
                    cmd.createClasspath(getProject()).createPath().append(new Path(getProject(), s.substring(10)));
                }
            }
            log("Implicitly adding " + antRuntimeClasses + " to CLASSPATH", Project.MSG_VERBOSE);
            cmd.createClasspath(getProject()).createPath().append(antRuntimeClasses);
        }
        if (summary) {
            log("Running " + test.getName(), Project.MSG_INFO);
            cmd.createArgument().setValue("formatter" + "=org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter");
        }
        cmd.createArgument().setValue("showoutput=" + String.valueOf(showOutput));
        StringBuffer formatterArg = new StringBuffer(STRING_BUFFER_SIZE);
        File propsFile = FileUtils.newFileUtils().createTempFile("junit", ".properties", tmpDir != null ? tmpDir : getProject().getBaseDir());
        propsFile.deleteOnExit();
        cmd.createArgument().setValue("propsfile=" + propsFile.getAbsolutePath());
        Hashtable p = getProject().getProperties();
        Properties props = new Properties();
        for (Enumeration e = p.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            props.put(key, p.get(key));
        }
        try {
            FileOutputStream outstream = new FileOutputStream(propsFile);
            props.store(outstream, "Ant JUnitTask generated properties file");
            outstream.close();
        } catch (java.io.IOException e) {
            propsFile.delete();
            throw new BuildException("Error creating temporary properties " + "file.", e, getLocation());
        }
        Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN), watchdog);
        execute.setCommandline(cmd.getCommandline());
        execute.setAntRun(getProject());
        if (vmDir != null) {
            execute.setWorkingDirectory(vmDir);
        }
        String[] environment = env.getVariables();
        if (environment != null) {
            for (int i = 0; i < environment.length; i++) {
                log("Setting environment variable: " + environment[i], Project.MSG_VERBOSE);
            }
        }
        execute.setNewenvironment(newEnvironment);
        execute.setEnvironment(environment);
        log(cmd.describeCommand(), Project.MSG_VERBOSE);
        int retVal;
        try {
            retVal = execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, getLocation());
        } finally {
            if (watchdog != null && watchdog.killedProcess()) {
            }
            if (!propsFile.delete()) {
                throw new BuildException("Could not delete temporary " + "properties file.");
            }
        }
        return retVal;
    }

    /**
	     * Pass output sent to System.out to the TestRunner so it can
	     * collect ot for the formatters.
	     *
	     * @param output output coming from System.out
	     * @since Ant 1.5
	     */
    protected void handleOutput(String output) {
    }

    /**
	     * @see Task#handleInput(byte[], int, int)
	     *
	     * @since Ant 1.6
	     */
    protected int handleInput(byte[] buffer, int offset, int length) throws IOException {
        return super.handleInput(buffer, offset, length);
    }

    /**
	     * Pass output sent to System.out to the TestRunner so it can
	     * collect ot for the formatters.
	     *
	     * @param output output coming from System.out
	     * @since Ant 1.5.2
	     */
    protected void handleFlush(String output) {
    }

    /**
	     * Pass output sent to System.err to the TestRunner so it can
	     * collect it for the formatters.
	     *
	     * @param output output coming from System.err
	     * @since Ant 1.5
	     */
    public void handleErrorOutput(String output) {
    }

    /**
	     * Pass output sent to System.err to the TestRunner so it can
	     * collect it for the formatters.
	     *
	     * @param output coming from System.err
	     * @since Ant 1.5.2
	     */
    public void handleErrorFlush(String output) {
    }

    /**
	     * @return <tt>null</tt> if there is a timeout value, otherwise the
	     * watchdog instance.
	     *
	     * @throws BuildException under unspecified circumstances
	     * @since Ant 1.2
	     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog(timeout.intValue());
    }

    /**
	     * Get the default output for a formatter.
	     *
	     * @return default output stream for a formatter
	     * @since Ant 1.3
	     */
    protected OutputStream getDefaultOutput() {
        return new LogOutputStream(this, Project.MSG_INFO);
    }

    /**
	     * Merge all individual tests from the batchtest with all individual tests
	     * and return an enumeration over all <tt>J3DPerfUnitTest</tt>.
	     *
	     * @return enumeration over individual tests
	     * @since Ant 1.3
	     */
    protected Enumeration getIndividualTests() {
        final int count = batchTests.size();
        final Enumeration[] enums = new Enumeration[count + 1];
        for (int i = 0; i < count; i++) {
            BatchTest batchtest = (BatchTest) batchTests.elementAt(i);
            enums[i] = batchtest.elements();
        }
        enums[enums.length - 1] = tests.elements();
        return Enumerations.fromCompound(enums);
    }

    /**
	     * return an enumeration listing each test, then each batchtest
	     * @return enumeration
	     * @since Ant 1.3
	     */
    protected Enumeration allTests() {
        Enumeration[] enums = { tests.elements(), batchTests.elements() };
        return Enumerations.fromCompound(enums);
    }

    /**
	     * Search for the given resource and add the directory or archive
	     * that contains it to the classpath.
	     *
	     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
	     * getResource doesn't contain the name of the archive.</p>
	     *
	     * @param resource resource that one wants to lookup
	     * @since Ant 1.4
	     */
    protected void addClasspathEntry(String resource) {
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        } else {
            resource = "org/apache/tools/ant/taskdefs/optional/junit/" + resource;
        }
        File f = LoaderUtils.getResourceSource(getClass().getClassLoader(), resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            antRuntimeClasses.createPath().setLocation(f);
        } else {
            log("Couldn\'t find " + resource, Project.MSG_DEBUG);
        }
    }
}
