package org.auroraide.server.jci.compilers.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.problems.CompilationProblem;
import org.apache.commons.jci.problems.CompilationProblemHandler;
import org.apache.commons.jci.readers.MemoryResourceReader;
import org.apache.commons.jci.stores.MemoryResourceStore;
import org.apache.commons.jci.stores.ResourceStore;
import org.auroraide.server.compiler.util.ClassEntity;
import org.auroraide.server.compiler.util.ProblemEntity;
import org.auroraide.server.jci.IJCICompiler;
import org.auroraide.server.jci.JavaCompilerFactory;

public class JCICompilerImpl implements IJCICompiler {

    private JavaCompiler compiler;

    private ClassLoader classloader;

    private JavaCompilerSettings settings;

    private ClassEntity classEntity;

    private List<ProblemEntity> problemEntities;

    private CommandLine cmd;

    @SuppressWarnings("static-access")
    public JCICompilerImpl(String type, String[] args) throws Exception {
        classEntity = new ClassEntity();
        problemEntities = new ArrayList<ProblemEntity>();
        final Options options = new Options();
        options.addOption(OptionBuilder.withArgName("a.jar:b.jar").hasArg().withValueSeparator(':').withDescription("Specify where to find user class files").create("classpath"));
        options.addOption(OptionBuilder.withArgName("release").hasArg().withDescription("Provide source compatibility with specified release").create("source"));
        options.addOption(OptionBuilder.withArgName("release").hasArg().withDescription("Generate class files for specific VM version").create("target"));
        options.addOption(OptionBuilder.withArgName("path").hasArg().withDescription("Specify where to find input source files").create("sourcepath"));
        options.addOption(OptionBuilder.withArgName("directory").hasArg().withDescription("Specify where to place generated class files").create("d"));
        options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("Stop compilation after these number of errors").create("Xmaxerrs"));
        options.addOption(OptionBuilder.withArgName("num").hasArg().withDescription("Stop compilation after these number of warning").create("Xmaxwarns"));
        options.addOption(OptionBuilder.withDescription("Generate no warnings").create("nowarn"));
        final CommandLineParser parser = new GnuParser();
        cmd = parser.parse(options, args, true);
        classloader = JCICompilerImpl.class.getClassLoader();
        @SuppressWarnings("unused") File sourcepath = new File(".");
        @SuppressWarnings("unused") File targetpath = new File(".");
        int maxerrs = 10;
        int maxwarns = 10;
        final boolean nowarn = cmd.hasOption("nowarn");
        compiler = new JavaCompilerFactory().createCompiler(type.trim().toLowerCase());
        settings = compiler.createDefaultSettings();
        for (Iterator<?> it = cmd.iterator(); it.hasNext(); ) {
            final Option option = (Option) it.next();
            if ("classpath".equals(option.getOpt())) {
                final String[] values = option.getValues();
                final URL[] urls = new URL[values.length];
                for (int i = 0; i < urls.length; i++) {
                    URI uri = new File(values[i]).toURI();
                    urls[i] = uri.toURL();
                }
                classloader = new URLClassLoader(urls);
            } else if ("source".equals(option.getOpt())) {
                settings.setSourceVersion(option.getValue());
            } else if ("target".equals(option.getOpt())) {
                settings.setTargetVersion(option.getValue());
            } else if ("sourcepath".equals(option.getOpt())) {
                sourcepath = new File(option.getValue());
            } else if ("d".equals(option.getOpt())) {
                targetpath = new File(option.getValue());
            } else if ("Xmaxerrs".equals(option.getOpt())) {
                maxerrs = Integer.parseInt(option.getValue());
            } else if ("Xmaxwarns".equals(option.getOpt())) {
                maxwarns = Integer.parseInt(option.getValue());
            }
        }
        final int maxErrors = maxerrs;
        final int maxWarnings = maxwarns;
        compiler.setCompilationProblemHandler(new CompilationProblemHandler() {

            int errors = 0;

            int warnings = 0;

            public boolean handle(final CompilationProblem pProblem) {
                if (pProblem.isError()) {
                    String kind = "Error";
                    String description = pProblem.getMessage();
                    String source = classEntity.getClassName() + ".java";
                    String path = classEntity.getProjectName() + "/" + classEntity.getPackageName().replace('.', '/') + "/" + classEntity.getClassName();
                    String line = String.valueOf(pProblem.getStartLine());
                    ProblemEntity problemEntity = new ProblemEntity(kind, description, source, path, line);
                    problemEntities.add(problemEntity);
                    errors++;
                    if (errors >= maxErrors) {
                        return false;
                    }
                } else {
                    if (!nowarn) {
                        String kind = "Warning";
                        String description = pProblem.getMessage();
                        String source = classEntity.getClassName() + ".java";
                        String path = classEntity.getProjectName() + "/" + classEntity.getPackageName().replace('.', '/') + "/" + classEntity.getClassName();
                        String line = String.valueOf(pProblem.getStartLine());
                        ProblemEntity problemEntity = new ProblemEntity(kind, description, source, path, line);
                        problemEntities.add(problemEntity);
                    }
                    warnings++;
                    if (warnings >= maxWarnings) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    public boolean compile() {
        if (classEntity == null || classEntity.getClassName().isEmpty() || classEntity.getPackageName().isEmpty() || classEntity.getContent().isEmpty()) throw new RuntimeException();
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(writer);
        out.write(classEntity.getContent());
        out.close();
        final MemoryResourceReader reader = new MemoryResourceReader();
        reader.add(classEntity.getPackageName().replace(".", "/") + "/" + classEntity.getClassName() + ".java", writer.toByteArray());
        final ResourceStore store = new MemoryResourceStore();
        final String[] resource = cmd.getArgs();
        final CompilationResult result = compiler.compile(resource, reader, store, classloader);
        this.classEntity.setBytecode(store.read(classEntity.getPackageName().replace(".", "/") + "/" + classEntity.getClassName() + ".class"));
        if (result.getErrors().length > 0 || result.getWarnings().length > 0) return false;
        return true;
    }

    public void loadClass(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public String runClass() {
        String back = "Runtime Error";
        if (classEntity == null || classEntity.getClassName().isEmpty() || classEntity.getPackageName().isEmpty() || classEntity.getContent().isEmpty()) throw new RuntimeException();
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(writer);
        out.write(classEntity.getContent());
        out.close();
        final MemoryResourceReader reader = new MemoryResourceReader();
        reader.add(classEntity.getPackageName().replace(".", "/") + "/" + classEntity.getClassName() + ".java", writer.toByteArray());
        final ResourceStore store = new MemoryResourceStore();
        final String[] resource = cmd.getArgs();
        final CompilationResult result = compiler.compile(resource, reader, store, classloader);
        this.classEntity.setBytecode(store.read(classEntity.getPackageName().replace(".", "/") + "/" + classEntity.getClassName() + ".class"));
        if (result.getErrors().length > 0 || result.getWarnings().length > 0) return "Compiler Errors";
        MemoClassLoader classLoader = new MemoClassLoader();
        Class<?> clazz = classLoader.defineClass(store, classEntity.getPackageName().replace(".", "/") + "/" + classEntity.getClassName() + ".class");
        Method mainMethod;
        try {
            mainMethod = clazz.getMethod("main", String[].class);
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            PrintStream printOut = new PrintStream(byteOutput);
            System.setOut(printOut);
            mainMethod.invoke(null, new Object[] { null });
            if (byteOutput.size() > 0) return byteOutput.toString();
        } catch (Exception e) {
            back = e.toString();
            e.printStackTrace();
            return back;
        }
        return back;
    }

    public void setOptions(String[] compilerOptions) {
    }

    public List<ProblemEntity> getProblems() {
        return problemEntities;
    }

    public static void main(String args[]) throws Exception {
        IJCICompiler compiler = new JCICompilerImpl("jsr199", args);
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("package com.compiler;");
        out.println("public class HelloWorld {");
        out.println("  public static void main(String args[]) {");
        out.println("    System.out.println(\"Hello World!\");");
        out.println("  }");
        out.println("}");
        out.close();
        ClassEntity classEntity = new ClassEntity("AuroraIDE", "com.compiler", "HelloWorld", writer.toString());
        compiler.loadClass(classEntity);
        boolean success = compiler.compile();
        if (!success) {
            Iterator<?> i = compiler.getProblems().iterator();
            while (i.hasNext()) {
                ProblemEntity pe = (ProblemEntity) i.next();
                System.out.println(pe.getKind());
                System.out.println(pe.getDescription());
                System.out.println(pe.getResource());
                System.out.println(pe.getPath());
                System.out.println(pe.getLocation());
            }
        } else {
            System.out.println(compiler.runClass());
        }
    }
}
