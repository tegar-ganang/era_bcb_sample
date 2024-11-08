package engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Class to generate and compile CmdList.
 * 
 * @author Robert J. C. Himmelmann (robert-h@gmx.de)
 */
public class CmdInit {

    private static final Set<String> dict = new TreeSet<String>();

    private static final Set<String> dictNames = new TreeSet<String>();

    private static final Set<String> dictNamesTurtle = new TreeSet<String>();

    private static void loadClass1(final String name) {
        if (!cmdName(name).equals(name.substring(name.lastIndexOf('.') + 2))) dict.add(name); else if (name.contains("turtle")) dictNamesTurtle.add(name); else dictNames.add(name);
    }

    private static String cmdName(final String name) {
        try {
            final Class<?> c = Class.forName(name);
            final Constructor<?> con = c.getConstructor();
            final Command c1 = (Command) con.newInstance();
            return c1.getName();
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        } catch (final SecurityException e) {
            e.printStackTrace();
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        } catch (final VerifyError e) {
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
            System.out.println(e);
        }
        System.exit(1);
        return null;
    }

    private static void addCmdsUnziped(final File dir, final String currdir) {
        for (final File f : dir.listFiles()) if (f.isDirectory()) addCmdsUnziped(f, currdir + f.getName() + "."); else if (f.isFile()) {
            final String s = f.getName();
            if (!(s.startsWith("_") && s.endsWith(".class") && !s.contains("$"))) continue;
            loadClass1(currdir + s.replace(".class", ""));
        }
    }

    private static void addCmdsJar(final String file) {
        ZipFile zip;
        try {
            zip = new ZipFile(file);
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry e = entries.nextElement();
                final String s = e.getName();
                if (s.contains("_") && s.endsWith(".class") && !s.contains("$")) loadClass1(s.replace(".class", "").replace('/', '.'));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    static {
        if (new File("engine").isDirectory()) addCmdsUnziped(new File("engine"), "engine."); else addCmdsJar("pagkalos.jar");
    }

    public static void main(final String[] args) throws IOException {
        final PrintStream out = new PrintStream("engine/CmdList.java");
        out.println();
        out.println("// Do not edit by hand. Use CmdInit.");
        out.println("package engine;");
        out.println("import java.util.Map;");
        out.println("import java.util.HashMap;");
        out.println("public class CmdList {");
        out.println("public final static Map<String, Command>	dict			= new HashMap<String, Command>();");
        out.println("public final static Map<String, String>	dictNames		= new HashMap<String, String>();");
        out.println("public final static Map<String, String>	dictNamesTurtle	= new HashMap<String, String>();");
        out.println("static {");
        for (final String name : dict) out.println("dict.put(\"" + cmdName(name) + "\", new " + name + "());");
        for (final String name : dictNames) out.println("dictNames.put(\"" + cmdName(name) + "\", \"" + name + "\");");
        for (final String name : dictNamesTurtle) out.println("dictNamesTurtle.put(\"" + cmdName(name) + "\", \"" + name + "\");");
        out.println("}}");
        final Process p = Runtime.getRuntime().exec("javac engine/CmdList.java");
        int read;
        while ((read = p.getErrorStream().read()) != -1) System.out.write(read);
    }
}
