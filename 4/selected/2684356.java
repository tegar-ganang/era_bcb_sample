package it.battlehorse.ioc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * The java agent which performs the bytecode manipulation. The agent can work in debug mode, which
 * activates a more detailed logging (to System.out) and a validation layer to verify bytecode modifications.
 * To enable the debug mode, set the <code>ioctransformer.debug</code> system property to <code>true</code>
 * at JVM startup time.
 * 
 * @author battlehorse
 * @since Apr 8, 2006
 */
public class IOCTransformer implements ClassFileTransformer {

    private static boolean DEBUG;

    static {
        String slDebug = System.getProperty("ioctransformer.debug", "false");
        DEBUG = slDebug.equalsIgnoreCase("true");
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (DEBUG) System.out.println("IOCT: Parsing class " + className);
        ClassReader creader = null;
        try {
            creader = new ClassReader(new ByteArrayInputStream(classfileBuffer));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ConstructorVisitor cv = new ConstructorVisitor();
        ClassAnnotationVisitor cav = new ClassAnnotationVisitor(cv);
        creader.accept(cav, ClassReader.SKIP_DEBUG);
        if (cv.getConstructors().size() > 0) {
            if (DEBUG) System.out.println("IOCT: Enhancing " + className);
            ClassWriter cw = new ClassWriter(0);
            ClassVisitor cvisitor = null;
            if (DEBUG) {
                CheckClassAdapter chk = new CheckClassAdapter(cw);
                cvisitor = chk;
            } else {
                cvisitor = cw;
            }
            ClassConstructorWriter writer = new ClassConstructorWriter(cv.getConstructors(), cvisitor);
            creader.accept(writer, ClassReader.SKIP_DEBUG);
            return cw.toByteArray();
        } else return null;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new IOCTransformer());
    }
}
