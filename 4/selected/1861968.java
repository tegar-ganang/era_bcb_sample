package unclej.validate;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Calls the {@link ClassInstrumenter} to modify class files so that validation metadata on parameters is checked.
 * To transform all loaded classes, run with <tt>-javaagent:unclej.jar</tt>.
 * @author scottv
 */
public class InstrumentParameters implements ClassFileTransformer {

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(true);
        LocalVariableVisitor varVisitor = new LocalVariableVisitor();
        reader.accept(varVisitor, false);
        reader.accept(new ClassInstrumenter(varVisitor.getVariables(), writer), false);
        return writer.toByteArray();
    }

    public static void premain(String options, Instrumentation inst) {
        inst.addTransformer(new InstrumentParameters());
    }
}
