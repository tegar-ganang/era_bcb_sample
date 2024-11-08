package com.google.monitoring.runtime.instrumentation;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instruments bytecodes that allocate heap memory to call a recording hook.
 * This will add a static invocation to a recorder function to any bytecode that
 * looks like it will be allocating heap memory allowing users to implement heap
 * profiling schemes.
 *
 * @author Ami Fischman
 * @author Jeremy Manson
 */
public class AllocationInstrumenter implements ClassFileTransformer {

    static final Logger logger = Logger.getLogger(AllocationInstrumenter.class.getName());

    private static volatile boolean canRewriteBootstrap;

    static boolean canRewriteClass(String className, ClassLoader loader) {
        if (((loader == null) && !canRewriteBootstrap) || className.startsWith("java/lang/ThreadLocal")) {
            return false;
        }
        if (className.startsWith("ognl/")) {
            return false;
        }
        return true;
    }

    AllocationInstrumenter() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        AllocationRecorder.setInstrumentation(inst);
        try {
            Class.forName("sun.security.provider.PolicyFile");
        } catch (Throwable t) {
        }
        if (!inst.isRetransformClassesSupported()) {
            System.err.println("Some JDK classes are already loaded and " + "will not be instrumented.");
        }
        if (AllocationRecorder.class.getClassLoader() != null) {
            canRewriteBootstrap = false;
            System.err.println("Class loading breakage: " + "Will not be able to instrument JDK classes");
            return;
        }
        canRewriteBootstrap = true;
        inst.addTransformer(new ConstructorInstrumenter(), inst.isRetransformClassesSupported());
        List<String> args = Arrays.asList(agentArgs == null ? new String[0] : agentArgs.split(","));
        if (!args.contains("manualOnly")) {
            bootstrap(inst);
        }
    }

    private static void bootstrap(Instrumentation inst) {
        inst.addTransformer(new AllocationInstrumenter(), inst.isRetransformClassesSupported());
        if (!canRewriteBootstrap) {
            return;
        }
        Class<?>[] classes = inst.getAllLoadedClasses();
        ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
        for (int i = 0; i < classes.length; i++) {
            if (inst.isModifiableClass(classes[i])) {
                classList.add(classes[i]);
            }
        }
        Class<?>[] workaround = new Class<?>[classList.size()];
        try {
            inst.retransformClasses(classList.toArray(workaround));
        } catch (UnmodifiableClassException e) {
            System.err.println("AllocationInstrumenter was unable to " + "retransform early loaded classes.");
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] origBytes) {
        if (!canRewriteClass(className, loader)) {
            return null;
        }
        return instrument(origBytes, loader);
    }

    /**
   * Given the bytes representing a class, go through all the bytecode in it and
   * instrument any occurences of new/newarray/anewarray/multianewarray with
   * pre- and post-allocation hooks.  Even more fun, intercept calls to the
   * reflection API's Array.newInstance() and instrument those too.
   *
   * @param originalBytes the original <code>byte[]</code> code.
   * @param recorderClass the <code>String</code> internal name of the class
   * containing the recorder method to run.
   * @param recorderMethod the <code>String</code> name of the recorder method
   * to run.
   * @return the instrumented <code>byte[]</code> code.
   */
    public static byte[] instrument(byte[] originalBytes, String recorderClass, String recorderMethod, ClassLoader loader) {
        try {
            ClassReader cr = new ClassReader(originalBytes);
            ClassWriter cw = new StaticClassWriter(cr, ClassWriter.COMPUTE_FRAMES, loader);
            VerifyingClassAdapter vcw = new VerifyingClassAdapter(cw, originalBytes, cr.getClassName());
            ClassAdapter adapter = new AllocationClassAdapter(vcw, recorderClass, recorderMethod);
            cr.accept(adapter, ClassReader.SKIP_FRAMES);
            return vcw.toByteArray();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to instrument class.", e);
            throw e;
        } catch (Error e) {
            logger.log(Level.WARNING, "Failed to instrument class.", e);
            throw e;
        }
    }

    /**
   * @see #instrument(byte[], String, String, ClassLoader)
   * documentation for the 4-arg version.  This is a convenience
   * version that uses the recorder in this class.
   */
    public static byte[] instrument(byte[] originalBytes, ClassLoader loader) {
        return instrument(originalBytes, "com/google/monitoring/runtime/instrumentation/AllocationRecorder", "recordAllocation", loader);
    }
}
