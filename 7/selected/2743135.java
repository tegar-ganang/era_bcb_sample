package org.jikesrvm;

import org.jikesrvm.classloader.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Manifest;
import java.util.jar.JarFile;

/**
 * Thread in which user's "main" program runs.
 *
 * @author Bowen Alpern
 * @author Derek Lieber
 * @modified Steven Augart
 */
class MainThread extends Thread {

    private String[] args;

    private final String[] agents;

    private VM_Method mainMethod;

    protected boolean launched = false;

    private static final boolean dbg = false;

    /**
   * Create "main" thread.
   * Taken: args[0]    = name of class containing "main" method
   *        args[1..N] = parameters to pass to "main" method
   */
    MainThread(String[] args) {
        super(args);
        this.agents = VM_CommandLineArgs.getArgs(VM_CommandLineArgs.JAVAAGENT_ARG);
        this.args = args;
        this.vmdata.isMainThread = true;
        this.vmdata.isSystemThread = false;
        if (dbg) VM.sysWriteln("MainThread(args.length == ", args.length, "): constructor done");
    }

    private void runAgents(ClassLoader cl) {
        if (agents.length > 0) {
            Instrumentation instrumenter = gnu.java.lang.JikesRVMSupport.createInstrumentation();
            java.lang.JikesRVMSupport.initializeInstrumentation(instrumenter);
            for (String agent : agents) {
                int equalsIndex = agent.indexOf('=');
                String agentJar;
                String agentOptions;
                if (equalsIndex != -1) {
                    agentJar = agent.substring(0, equalsIndex);
                    agentOptions = agent.substring(equalsIndex + 1);
                } else {
                    agentJar = agent;
                    agentOptions = "";
                }
                runAgent(instrumenter, cl, agentJar, agentOptions);
            }
        }
    }

    private static void runAgent(Instrumentation instrumenter, ClassLoader cl, String agentJar, String agentOptions) {
        Manifest mf = null;
        try {
            JarFile jf = new JarFile(agentJar);
            mf = jf.getManifest();
        } catch (Exception e) {
            VM.sysWriteln("vm: IO Exception opening JAR file ", agentJar, ": ", e.getMessage());
            VM.sysExit(VM.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
        }
        if (mf == null) {
            VM.sysWriteln("The jar file is missing the manifest: ", agentJar);
            VM.sysExit(VM.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
        }
        String agentClassName = mf.getMainAttributes().getValue("Premain-Class");
        if (agentClassName == null) {
            VM.sysWriteln("The jar file is missing the Premain-Class manifest entry for the agent class: ", agentJar);
            VM.sysExit(VM.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
        }
        try {
            Class<?> agentClass = cl.loadClass(agentClassName);
            Method agentPremainMethod = agentClass.getMethod("premain", new Class<?>[] { String.class, Instrumentation.class });
            agentPremainMethod.invoke(null, new Object[] { agentOptions, instrumenter });
        } catch (InvocationTargetException e) {
        } catch (Throwable e) {
            VM.sysWriteln("Failed to run the agent's premain: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public String toString() {
        return "MainThread";
    }

    VM_Method getMainMethod() {
        return mainMethod;
    }

    /**
   * Run "main" thread.
   * 
   * This code could be made a little shorter by relying on VM_Reflection
   * to do the classloading and compilation.  We intentionally do it here
   * to give us a chance to provide error messages that are specific to
   * not being able to find the main class the user wants to run.
   * This may be a little silly, since it results in code duplication
   * just to provide debug messages in a place where very little is actually
   * likely to go wrong, but there you have it....
   */
    public void run() {
        if (dbg) VM.sysWriteln("MainThread.run() starting ");
        ClassLoader cl = VM_ClassLoader.getApplicationClassLoader();
        setContextClassLoader(cl);
        runAgents(cl);
        if (dbg) VM.sysWrite("[MainThread.run() loading class to run... ");
        VM_Class cls = null;
        try {
            VM_Atom mainAtom = VM_Atom.findOrCreateUnicodeAtom(args[0].replace('.', '/'));
            VM_TypeReference mainClass = VM_TypeReference.findOrCreate(cl, mainAtom.descriptorFromClassName());
            cls = mainClass.resolve().asClass();
            cls.resolve();
            cls.instantiate();
            cls.initialize();
        } catch (NoClassDefFoundError e) {
            if (dbg) VM.sysWrite("failed.]");
            VM.sysWrite(e + "\n");
            return;
        }
        if (dbg) VM.sysWriteln("loaded.]");
        mainMethod = cls.findMainMethod();
        if (mainMethod == null) {
            VM.sysWrite(cls + " doesn't have a \"public static void main(String[])\" method to execute\n");
            return;
        }
        if (dbg) VM.sysWrite("[MainThread.run() making arg list... ");
        String[] mainArgs = new String[args.length - 1];
        for (int i = 0, n = mainArgs.length; i < n; ++i) mainArgs[i] = args[i + 1];
        if (dbg) VM.sysWriteln("made.]");
        if (dbg) VM.sysWrite("[MainThread.run() compiling main(String[])... ");
        mainMethod.compile();
        if (dbg) VM.sysWriteln("compiled.]");
        VM_Callbacks.notifyStartup();
        launched = true;
        if (dbg) VM.sysWriteln("[MainThread.run() invoking \"main\" method... ");
        VM_Reflection.invoke(mainMethod, null, new Object[] { mainArgs }, false);
        if (dbg) VM.sysWriteln("  MainThread.run(): \"main\" method completed.]");
    }
}
