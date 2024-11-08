package net.sourceforge.comeback.prolog2JAR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Map;
import net.sourceforge.comeback.io.Assembler;
import net.sourceforge.comeback.io.ClassWriter;
import net.sourceforge.comeback.io.JarEntryWriter;
import net.sourceforge.comeback.io.PatternClassFilter;
import net.sourceforge.comeback.prolog.abstractParser.Parser;
import net.sourceforge.comeback.prolog.prologParser.ast.PrologProgram;
import net.sourceforge.comeback.prolog.prologParser.impl.PrologParserImpl;
import net.sourceforge.comeback.prolog2JAR.adaptergen.AdapterAnnotationGenerator;
import net.sourceforge.comeback.prolog2JAR.adaptergen.AdapterLookupGenerator;
import net.sourceforge.comeback.prolog2JAR.adaptergen.FactVisitor;
import net.sourceforge.comeback.prolog2JAR.adaptergen.GenerationContext;
import net.sourceforge.comeback.prolog2JAR.info.TypeGenerationInfo;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Type;

/**
 * Main class of the ComeBack! Java adapter generator.
 *
 * @author Michael Rudolf
 */
public class Main {

    /**
     * No instances needed.
     */
    private Main() {
    }

    /**
     * Creates a ClassLoader capable of loading classes from the given class
     * path. If the given path is not well-formed, an exception will be thrown.
     *
     * @param classpath the class path to create a ClassLoader for
     */
    private static URLClassLoader createClassLoaderFromClassPath(String classpath) throws MalformedURLException {
        String[] paths = classpath.split(File.pathSeparator);
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            urls[i] = new File(paths[i]).toURL();
        }
        return URLClassLoader.newInstance(urls);
    }

    public static void main(final String args[]) {
        if (args.length < 2 || (args.length == 3 && !"-d".equals(args[0]))) {
            System.out.println("Usage: AdapterGenerator [Options] <PrologFile> <ClassPath>");
            System.out.println("       where the framework is the first element in the class path");
            System.out.println("Options: -d Enables the generation of debug output");
            return;
        }
        boolean debug = args.length == 3;
        File prologFile = new File(debug ? args[1] : args[0]);
        if (!prologFile.isFile()) {
            System.out.println("The given file does not exist.");
            return;
        }
        File targetFile = new File(prologFile.getParentFile(), prologFile.getName().substring(0, prologFile.getName().lastIndexOf(".")) + ".jar");
        String source;
        try {
            source = FileUtils.readFileToString(prologFile);
        } catch (IOException ex) {
            System.out.println("Error reading file: " + ex.getMessage());
            return;
        }
        Parser myParser = new PrologParserImpl();
        PrologProgram myProgram;
        try {
            myProgram = (PrologProgram) myParser.parse(source);
        } catch (Exception ex) {
            System.out.println("Could not parse file: " + ex.getMessage());
            return;
        }
        try {
            FactVisitor myVisitor = new FactVisitor();
            myVisitor.visit(myProgram);
            Map<String, TypeGenerationInfo> adapterLayer = myVisitor.getAdapterLayer();
            if (adapterLayer.size() == 0) {
                System.out.println("No adapters to generate, " + "did you forget to run the comeback rules?");
                return;
            }
            URLClassLoader classLoader = createClassLoaderFromClassPath(debug ? args[2] : args[1]);
            ClassWriter adapterWriter = new ClassWriter(new JarEntryWriter(targetFile));
            AdapterAnnotationGenerator annotationGenerator = new AdapterAnnotationGenerator();
            Type annotationType = Type.getObjectType("net/sourceforge/comeback/Adapter");
            adapterWriter.writeClass(annotationType.getClassName(), annotationGenerator.createAnnotation(annotationType));
            AdapterLookupGenerator lookupWriter = new AdapterLookupGenerator(Type.getObjectType("net/sourceforge/comeback/Adapters"), annotationType);
            GenerationContext context = new GenerationContext(adapterLayer, classLoader, annotationType, lookupWriter, adapterWriter);
            context.setGenerateDebugOutput(debug);
            Iterator<TypeGenerationInfo> iterator = adapterLayer.values().iterator();
            while (iterator.hasNext()) {
                TypeGenerationInfo info = iterator.next();
                context.generateAdapter(info);
            }
            ByteArrayOutputStream serialized = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(serialized);
            out.writeObject(adapterLayer);
            out.close();
            adapterWriter.writeResource("net/sourceforge/comeback/adapterlayer.ser", serialized.toByteArray());
            String[] sharedClassesPrefixes = new String[2];
            Assembler assembler = new Assembler(adapterWriter);
            PatternClassFilter filter = new PatternClassFilter();
            URL url = Main.class.getResource("/" + Main.class.getName().replace('.', '/') + ".class");
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            sharedClassesPrefixes[0] = assembler.assemble(connection.getJarFile(), filter);
            filter = new PatternClassFilter();
            url = Type.class.getResource("/" + Type.class.getName().replace('.', '/') + ".class");
            connection = (JarURLConnection) url.openConnection();
            sharedClassesPrefixes[1] = assembler.assemble(connection.getJarFile(), filter);
            String tmp = debug ? args[2] : args[1];
            adapterWriter.writeClass(lookupWriter.getType().getClassName(), lookupWriter.createAdapterLookup(adapterLayer.values(), tmp.substring(tmp.lastIndexOf(File.pathSeparator) + 1), sharedClassesPrefixes));
            adapterWriter.close();
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }
}
