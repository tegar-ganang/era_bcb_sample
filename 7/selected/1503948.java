package org.jikesrvm.scheduler;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jikesrvm.VM;
import org.jikesrvm.VM_Callbacks;
import org.jikesrvm.VM_CommandLineArgs;
import org.jikesrvm.classloader.VM_Atom;
import org.jikesrvm.classloader.VM_Class;
import org.jikesrvm.classloader.VM_ClassLoader;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.classloader.VM_TypeReference;
import org.jikesrvm.runtime.VM_Reflection;
import org.vmmagic.pragma.Entrypoint;

/**
 * Thread in which user's "main" program runs.
 */
public final class VM_MainThread extends Thread {

    private final String[] args;

    private final String[] agents;

    private VM_Method mainMethod;

    protected boolean launched = false;

    private static final boolean dbg = false;

    /**
   * Create "main" thread.
   * Taken: args[0]    = name of class containing "main" method
   *        args[1..N] = parameters to pass to "main" method
   */
    public VM_MainThread(String[] args) {
        super("MainThread");
        setDaemon(false);
        this.agents = VM_CommandLineArgs.getJavaAgentArgs();
        this.args = args;
        if (dbg) {
            VM.sysWriteln("VM_MainThread(args.length == ", args.length, "): constructor done");
        }
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
    @Override
    @Entrypoint
    public void run() {
        launched = true;
        if (dbg) VM.sysWriteln("VM_MainThread.run() starting ");
        ClassLoader cl = VM_ClassLoader.getApplicationClassLoader();
        setContextClassLoader(cl);
        runAgents(cl);
        if (dbg) VM.sysWrite("[VM_MainThread.run() loading class to run... ");
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
        if (dbg) VM.sysWrite("[VM_MainThread.run() making arg list... ");
        String[] mainArgs = new String[args.length - 1];
        for (int i = 0, n = mainArgs.length; i < n; ++i) {
            mainArgs[i] = args[i + 1];
        }
        if (dbg) VM.sysWriteln("made.]");
        if (dbg) VM.sysWrite("[VM_MainThread.run() compiling main(String[])... ");
        mainMethod.compile();
        if (dbg) VM.sysWriteln("compiled.]");
        VM_Callbacks.notifyStartup();
        if (dbg) VM.sysWriteln("[VM_MainThread.run() invoking \"main\" method... ");
        VM_Reflection.invoke(mainMethod, null, new Object[] { mainArgs }, false);
        if (dbg) VM.sysWriteln("  VM_MainThread.run(): \"main\" method completed.]");
    }
}
