package net.sourceforge.jdefprog.weaver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jdefprog.agent.Agent;
import net.sourceforge.jdefprog.agent.ClassTransformer;
import net.sourceforge.jdefprog.agent.InstrumentationConfiguration;
import net.sourceforge.jdefprog.agent.JDefProgClassTransformer;
import net.sourceforge.jdefprog.agent.filters.AllClassesFilter;
import net.sourceforge.jdefprog.agent.filters.ClassFilter;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

public class ClassWeaver {

    public void weave(File src, File dest) throws IOException {
        weave(src, src, dest);
    }

    public void weave(File src, File origin, File dest) throws IOException {
        System.out.println("Weaving '" + src + "'");
        if (!src.exists()) {
            System.err.println("File '" + src + "' doesn't exist");
            return;
        }
        if (src.isDirectory()) {
            File[] children = src.listFiles();
            for (File child : children) {
                weave(child, origin, dest);
            }
        } else {
            if (src.getAbsolutePath().endsWith(".class")) {
                System.out.println("Analyzing '" + src.getAbsolutePath() + "'");
                weaveClassFile(src, origin, dest);
            }
        }
    }

    private boolean debugInWeawedCodeEnabled = false;

    private ClassTransformer getClassFileTransformer(ClassFilter filter) {
        Agent agent = new Agent();
        InstrumentationConfiguration conf = new InstrumentationConfiguration(debugInWeawedCodeEnabled);
        JDefProgClassTransformer ct = agent.getClassTransformer(filter, conf);
        ct.setMarkClasses(true);
        return ct;
    }

    private void weaveClassFile(File file, File origin, File dest) throws IOException {
        InputStream is = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(is);
        ClassFile cf = new ClassFile(dis);
        System.out.println("ClassName '" + cf.getName() + "'");
        ClassPool cp = ClassPool.getDefault();
        byte[] modifiedBytecode = null;
        try {
            CtClass loadedClass = cp.get(cf.getName());
            System.out.println("Loaded");
            modifiedBytecode = this.getClassFileTransformer(new AllClassesFilter(true)).transform(loadedClass.toBytecode(), cf.getName());
            System.out.println("Bytecode modified");
            if (modifiedBytecode == null) {
                modifiedBytecode = loadedClass.toBytecode();
            }
            System.out.println("Bytecode ready to write");
            System.out.println("Detached");
            loadedClass.defrost();
            CtClass modifiedClass = cp.makeClass(new ByteArrayInputStream(modifiedBytecode));
            System.out.println("Modified class done");
            modifiedClass.writeFile(dest.getAbsolutePath());
            System.out.println("Wrote");
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Two parameters expected:
	 * 1) Src dir
	 * 2) Dest dir
	 */
    public static void main(String[] args) {
        System.out.println("ClassWeaver");
        System.out.flush();
        Level logLevl = Level.FINEST;
        OutputStream outputStream = new ByteArrayOutputStream();
        PrintStream dumperStream = new PrintStream(outputStream);
        net.sourceforge.jdefprog.annocheck.MclAnalysisConfiguration.getInstance().setMclDumperStream(dumperStream);
        Logger gl = Logger.getLogger("");
        gl.addHandler(new ConsoleHandler());
        gl.setLevel(logLevl);
        for (Handler h : gl.getHandlers()) {
            h.setLevel(logLevl);
        }
        ClassWeaver weaver = new ClassWeaver();
        String toWeave = null;
        if (args.length == 0) {
            toWeave = ".";
        } else {
            toWeave = args[0];
        }
        try {
            ClassPool.getDefault().insertClassPath(toWeave);
        } catch (NotFoundException e1) {
            e1.printStackTrace();
        }
        String destDir;
        if (args.length < 2) {
            destDir = ".";
        } else {
            destDir = args[1];
        }
        System.out.println("toWeave='" + toWeave + "'");
        try {
            weaver.weave(new File(toWeave), new File(destDir));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
