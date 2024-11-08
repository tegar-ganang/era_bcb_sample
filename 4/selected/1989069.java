package idebughc.testsuite;

import idebughc.*;

/**
 * <p> TestSuite is the black-box testsuite for the Debug class. </p>
 *
 * @version $Date: 2009-06-15 16:04:12 $
 * @author Joseph R. Kiniry <joe@kindsoftware.com>
 *
 * @note The actual code of the IDebug test suite.
 */
public class TestSuiteThread extends Thread {

    private boolean value;

    private boolean success = true;

    private String testMode = null;

    /**
   * Create a new TestSuiteThread with the specified test mode.
   *
   * @param tm the test mode for this test suite thread.  Exactly one
   * of the following strings: "console", "servletlog", "window", "writer".
   */
    TestSuiteThread(String tm) {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(tm) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "TestSuiteThread(java.lang.String)", true), jassParameters);
        if (!((tm.equals("console") || tm.equals("servletlog") || tm.equals("window") || tm.equals("writer")))) throw new jass.runtime.PreconditionException("idebughc.testsuite.TestSuiteThread", "TestSuiteThread(java.lang.String)", 73, "tm_valid");
        this.testMode = tm;
        if (!((testMode == tm))) throw new jass.runtime.PostconditionException("idebughc.testsuite.TestSuiteThread", "TestSuiteThread(java.lang.String)", 78, "testMode_is_valid");
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "TestSuiteThread(java.lang.String)", false), jassParameters);
    }

    public void run() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "run()", true), jassParameters);
        Debug debug = new Debug();
        System.out.println("TESTING IDEBUG PACKAGE.\n");
        System.out.println("Using test mode " + testMode + ".\n");
        System.out.println("Class-global testing\n" + "====================");
        DebugConstants debugConstants = debug.getDebugConstants();
        DebugOutput debugOutput = null;
        if (testMode.equals("console")) {
            debugOutput = new ConsoleOutput(debug);
        } else if (testMode.equals("servletlog")) {
            debugOutput = new ServletLogOutput(debug);
        } else if (testMode.equals("window")) {
            debugOutput = new WindowOutput(debug);
        } else if (testMode.equals("writer")) {
            debugOutput = new WriterOutput(debug);
        } else throw new RuntimeException("Illegal test mode: " + testMode);
        debug.setOutputInterface(debugOutput);
        success &= (!debugOutput.println(debugConstants.ASSERTION_LEVEL, "FAILED"));
        if (!success) System.err.println("FALURE #0");
        success &= (!debugOutput.println(debugConstants.ASSERTION, "FAILED"));
        if (!success) System.err.println("FALURE #1");
        debug.turnOn();
        success &= debugOutput.println(debugConstants.FAILURE_LEVEL, "PASSED");
        if (!success) System.err.println("FALURE #2");
        success &= debugOutput.println(debugConstants.FAILURE, "PASSED");
        if (!success) System.err.println("FALURE #3");
        debug.setLevel(debugConstants.LEVEL_MIN - 1);
        success &= (debug.getLevel() != (debugConstants.LEVEL_MIN - 1));
        if (!success) System.err.println("FALURE #4");
        debug.setLevel(debugConstants.ERROR_LEVEL);
        success &= (!debugOutput.println(debugConstants.ERROR_LEVEL - 1, "FAILED"));
        if (!success) System.err.println("FALURE #5");
        success &= (!debugOutput.println(debugConstants.WARNING, "FAILED"));
        if (!success) System.err.println("FALURE #6");
        success &= debugOutput.println(debugConstants.ERROR_LEVEL, "PASSED");
        if (!success) System.err.println("FALURE #7");
        success &= debugOutput.println(debugConstants.ERROR, "PASSED");
        if (!success) System.err.println("FALURE #8");
        success &= debugOutput.println(debugConstants.ERROR_LEVEL + 1, "PASSED");
        if (!success) System.err.println("FALURE #9");
        success &= debugOutput.println(debugConstants.CRITICAL, "PASSED");
        if (!success) System.err.println("FALURE #10");
        success &= debugOutput.println(debugConstants.ASSERTION_LEVEL, "PASSED");
        if (!success) System.err.println("FALURE #11");
        success &= debugOutput.println(debugConstants.ASSERTION, "PASSED");
        if (!success) System.err.println("FALURE #12");
        debug.setLevel(debugConstants.LEVEL_MAX + 1);
        success &= (debug.getLevel() != (debugConstants.LEVEL_MIN + 1));
        if (!success) System.err.println("FALURE #13");
        debug.setLevel(0);
        success &= debugOutput.println(0, "PASSED");
        if (!success) System.err.println("FALURE #14");
        success &= debugOutput.println(debugConstants.NOTICE_LEVEL, "PASSED");
        if (!success) System.err.println("FALURE #15");
        success &= debugOutput.println(debugConstants.NOTICE, "PASSED");
        if (!success) System.err.println("FALURE #16");
        success &= debugOutput.println(debugConstants.ASSERTION_LEVEL, "PASSED");
        if (!success) System.err.println("FALURE #17");
        success &= debugOutput.println(debugConstants.ASSERTION, "PASSED");
        if (!success) System.err.println("FALURE #18");
        success &= debug.addCategory("NETWORK_6", 6);
        if (!success) System.err.println("FALURE #19");
        success &= debug.addCategory("NETWORK_5", 5);
        if (!success) System.err.println("FALURE #20");
        success &= debug.addCategory("NETWORK_4", 4);
        if (!success) System.err.println("FALURE #21");
        debug.setLevel(5);
        success &= debugOutput.println(5, "PASSED");
        if (!success) System.err.println("FALURE #23");
        success &= debugOutput.println("NETWORK_5", "PASSED");
        if (!success) System.err.println("FALURE #24");
        success &= (!debugOutput.println("NETWORK_4", "FAILED"));
        if (!success) System.err.println("FALURE #25");
        success &= debugOutput.println("NETWORK_6", "PASSED");
        if (!success) System.err.println("FALURE #26");
        success &= debug.removeCategory("NETWORK_5");
        if (!success) System.err.println("FALURE #27");
        success &= (!debugOutput.println("NETWORK_5", "FAILED"));
        if (!success) System.err.println("FALURE #28");
        success &= !debugOutput.println(debugConstants.LEVEL_MIN - 1, "FAILED");
        if (!success) System.err.println("FALURE #29");
        success &= !debugOutput.println(debugConstants.LEVEL_MAX + 1, "FAILED");
        if (!success) System.err.println("FALURE #30");
        debug.turnOff();
        System.out.println("\nPer-thread testing\n" + "====================");
        Context debugContext = new Context(new DefaultDebugConstants(), new ConsoleOutput(debug));
        debugContext.turnOn();
        debugContext.setLevel(debugConstants.ERROR_LEVEL);
        success &= debugContext.addCategory("PERTHREAD-1", debugConstants.ERROR_LEVEL - 1);
        if (!success) System.err.println("FALURE #31");
        success &= debugContext.addCategory("PERTHREAD+1", debugConstants.ERROR_LEVEL + 1);
        if (!success) System.err.println("FALURE #32");
        debug.addContext(debugContext);
        success &= debugOutput.println(debugConstants.ERROR_LEVEL, "SUCCESS");
        if (!success) System.err.println("FALURE #33");
        success &= debugOutput.println(debugConstants.ERROR_LEVEL + 1, "SUCCESS");
        if (!success) System.err.println("FALURE #34");
        success &= (!debugOutput.println(debugConstants.ERROR_LEVEL - 1, "FAILURE"));
        if (!success) System.err.println("FALURE #35");
        success &= (!debugOutput.println("PERTHREAD-1", "FAILURE"));
        if (!success) System.err.println("FALURE #36");
        success &= debugOutput.println("PERTHREAD+1", "SUCCESS");
        if (!success) System.err.println("FALURE #37");
        debugContext.setLevel(debugConstants.ERROR_LEVEL - 1);
        success &= debugOutput.println(debugConstants.ERROR_LEVEL + 1, "SUCCESS");
        if (!success) System.err.println("FALURE #39");
        success &= debugOutput.println(debugConstants.ERROR_LEVEL - 1, "SUCCESS");
        if (!success) System.err.println("FALURE #40");
        success &= debugOutput.println("PERTHREAD-1", "SUCCESS");
        if (!success) System.err.println("FALURE #41");
        success &= debugOutput.println("PERTHREAD+1", "SUCCESS");
        if (!success) System.err.println("FALURE #42");
        debug.turnOn();
        debug.setLevel(debugConstants.ERROR_LEVEL);
        debugContext.setLevel(debugConstants.CRITICAL_LEVEL);
        success &= debugOutput.println(debugConstants.CRITICAL, "SUCCESS");
        if (!success) System.err.println("FALURE #43");
        success &= (!debugOutput.println(debugConstants.NOTICE, "FAILURE"));
        if (!success) System.err.println("FALURE #44");
        success &= debugOutput.println(debugConstants.ERROR, "SUCCESS");
        if (!success) System.err.println("FALURE #45");
        success &= debugOutput.println(debugConstants.FAILURE, "SUCCESS");
        if (!success) System.err.println("FALURE #46");
        System.out.println("Testing concluded.");
        if (success) {
            System.out.println("Debugging tests succeeded!\n\n");
            System.exit(0);
        } else {
            System.out.println("Debugging tests failed!\n\n");
            System.exit(-1);
        }
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "run()", false), jassParameters);
    }

    protected void finalize() throws java.lang.Throwable {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "finalize()", true), jassParameters);
        super.finalize();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "finalize()", false), jassParameters);
    }

    public boolean equals(java.lang.Object par0) {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(par0) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "equals(java.lang.Object)", true), jassParameters);
        boolean returnValue = super.equals(par0);
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(returnValue) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "equals(java.lang.Object)", false), jassParameters);
        return returnValue;
    }

    public java.lang.String toString() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "toString()", true), jassParameters);
        java.lang.String returnValue = super.toString();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(returnValue) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "toString()", false), jassParameters);
        return returnValue;
    }

    public void interrupt() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "interrupt()", true), jassParameters);
        super.interrupt();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "interrupt()", false), jassParameters);
    }

    public boolean isInterrupted() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "isInterrupted()", true), jassParameters);
        boolean returnValue = super.isInterrupted();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(returnValue) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "isInterrupted()", false), jassParameters);
        return returnValue;
    }

    public void destroy() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "destroy()", true), jassParameters);
        super.destroy();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "destroy()", false), jassParameters);
    }

    public java.lang.ClassLoader getContextClassLoader() {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "getContextClassLoader()", true), jassParameters);
        java.lang.ClassLoader returnValue = super.getContextClassLoader();
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(returnValue) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "getContextClassLoader()", false), jassParameters);
        return returnValue;
    }

    public void setContextClassLoader(java.lang.ClassLoader par0) {
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jass.runtime.traceAssertion.Parameter[] jassParameters;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] { new jass.runtime.traceAssertion.Parameter(par0) };
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "setContextClassLoader(java.lang.ClassLoader)", true), jassParameters);
        super.setContextClassLoader(par0);
        jass.runtime.traceAssertion.CommunicationManager.internalAction = true;
        jassParameters = new jass.runtime.traceAssertion.Parameter[] {};
        jass.runtime.traceAssertion.CommunicationManager.internalAction = false;
        jass.runtime.traceAssertion.CommunicationManager.communicate(this, new jass.runtime.traceAssertion.MethodReference("idebughc.testsuite", "TestSuiteThread", "setContextClassLoader(java.lang.ClassLoader)", false), jassParameters);
    }
}
