package net.sf.joafip.store.service.bytecode.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.instrument.IllegalClassFormatException;
import net.sf.joafip.asm.ClassReader;
import net.sf.joafip.asm.util.CheckClassAdapter;

/**
 * invoke java agent transformer. use for for debug
 * 
 * @author luc peuvrier
 * 
 */
@SuppressWarnings("PMD")
public final class MainCheckAndDump {

    private MainCheckAndDump() {
        super();
    }

    public static void main(final String[] args) {
        try {
            final MainCheckAndDump main = new MainCheckAndDump();
            main.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() throws IllegalClassFormatException, IOException {
        final String jvmClassName = "net/sf/joafip/store/service/bytecode/agent/NestedTryCatch";
        final InputStream stream = ClassLoader.getSystemResourceAsStream(jvmClassName + ".class");
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int read;
        while ((read = stream.read()) != -1) {
            byteArrayOutputStream.write(read);
        }
        byteArrayOutputStream.close();
        stream.close();
        final byte[] classfileBuffer = byteArrayOutputStream.toByteArray();
        final ClassReader cr = new ClassReader(classfileBuffer);
        final PrintWriter pw = new PrintWriter("logs/dump.txt");
        CheckClassAdapter.verify(cr, true, pw);
    }
}
