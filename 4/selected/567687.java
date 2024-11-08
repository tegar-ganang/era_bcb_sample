package stabilizer.runner;

import java.io.*;
import java.util.jar.*;
import java.util.zip.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Constants;

public class StabilizerPacker implements Constants {

    private static Selector mySelector = new Selector();

    private static final String output_jar_name = "stabilizer-package.jar";

    private static String stabilizer_tmpdir_name = "instrumented";

    public static void runMain(String[] argv) throws IOException {
        FileOutputStream fos = new FileOutputStream(output_jar_name);
        JarOutputStream out_jar_stream = new JarOutputStream(fos);
        for (int argno = 0; argno < argv.length; argno++) {
            if (argv[argno].endsWith(".jar")) {
                instrument_jar_file(argv[argno], out_jar_stream);
            } else {
                String class_name = argv[argno];
                if (!class_name.endsWith(".class")) class_name = class_name + ".class";
                JarEntry je = new JarEntry(class_name.toString());
                je.setMethod(ZipEntry.DEFLATED);
                out_jar_stream.putNextEntry(je);
                String instr_class_file = instrument_class(argv[argno], new ClassParser(argv[argno]));
                FileInputStream src = new FileInputStream(instr_class_file);
                copyStreamToJarStream(out_jar_stream, src);
                src.close();
                if (!((new File(instr_class_file)).delete())) System.out.println("Warning: unable to delete file: " + instr_class_file);
            }
        }
        if (out_jar_stream != null) out_jar_stream.close();
        System.out.println("Finishing pack bytecode!");
    }

    private static void instrument_jar_file(String name, JarOutputStream jout) throws IOException {
        JarInputStream jin = new JarInputStream(new FileInputStream(name));
        while (true) {
            JarEntry je = jin.getNextJarEntry();
            if (je == null) break;
            if (je.isDirectory()) continue;
            JarEntry new_je = new JarEntry(je.getName());
            jout.putNextEntry(new_je);
            if (je.getName().endsWith(".class")) {
                String instr_class_file = instrument_class(je.getName(), new ClassParser(name, je.getName()));
                FileInputStream src = new FileInputStream(instr_class_file);
                copyStreamToJarStream(jout, src);
                src.close();
                if (!((new File(instr_class_file)).delete())) System.out.println("Warning: unable to delete file: " + instr_class_file);
            } else copyStreamToJarStream(jout, jin);
        }
    }

    private static void copyStreamToJarStream(JarOutputStream jout, InputStream src) throws IOException {
        byte[] buf = new byte[512];
        do {
            int bread = src.read(buf);
            if (bread <= 0) break;
            jout.write(buf, 0, bread);
        } while (true);
        jout.closeEntry();
    }

    private static String instrument_class(String filename, ClassParser cp) {
        System.out.println("Instrumenting class \"" + filename + '"');
        JavaClass clazz = null;
        try {
            clazz = cp.parse();
        } catch (ClassFormatError cfe) {
            System.out.println("Exception while parsing file " + filename + "\n" + cfe);
            System.exit(2);
        } catch (IOException ioe) {
            System.out.println("Error while reading file " + filename + "\n" + ioe);
            System.exit(2);
        }
        if (clazz == null) {
            System.out.println("\nUnable to find class " + cp + " ignoring this class");
            System.exit(2);
        }
        ConstantPoolGen cpgen = new ConstantPoolGen(clazz.getConstantPool());
        String className = clazz.getClassName();
        if (isSelected(className)) {
            clazz = Instrumenter.instrument(clazz);
        }
        String instrumented_class_filename = null;
        try {
            instrumented_class_filename = (stabilizer_tmpdir_name + File.separator + clazz.getClassName().replace('.', File.separatorChar));
            clazz.dump(instrumented_class_filename);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(32);
        }
        return instrumented_class_filename;
    }

    public static Selector getSelector() {
        return mySelector;
    }

    protected static boolean isSelected(String className) {
        return mySelector.isSelected(className);
    }
}
