package org.granite.generator.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.granite.generator.as3.As3BeanGenerator;
import org.granite.generator.as3.DefaultJava2As3;
import org.granite.generator.as3.Java2As3;
import org.granite.generator.reflect.JClass;

public class As3BeanAntTask extends Task {

    private String outputdir = ".";

    private String uid = "uid";

    private String entitytemplate = null;

    private String entitybasetemplate = null;

    private String interfacetemplate = null;

    private String interfacebasetemplate = null;

    private String generictemplate = null;

    private String genericbasetemplate = null;

    private String java2as3class = null;

    private Path classpath = null;

    private List<FileSet> fileSets = new ArrayList<FileSet>();

    public void setOutputdir(String outputdir) {
        this.outputdir = outputdir;
    }

    public void setJava2as3class(String java2as3class) {
        this.java2as3class = java2as3class;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEntitytemplate(String entitytemplate) {
        this.entitytemplate = entitytemplate;
    }

    public void setEntitybasetemplate(String entitybasetemplate) {
        this.entitybasetemplate = entitybasetemplate;
    }

    public void setInterfacetemplate(String interfacetemplate) {
        this.interfacetemplate = interfacetemplate;
    }

    public void setInterfacebasetemplate(String interfacebasetemplate) {
        this.interfacebasetemplate = interfacebasetemplate;
    }

    public void setGenerictemplate(String generictemplate) {
        this.generictemplate = generictemplate;
    }

    public void setGenericbasetemplate(String genericbasetemplate) {
        this.genericbasetemplate = genericbasetemplate;
    }

    public void addFileset(FileSet fileSet) {
        fileSets.add(fileSet);
    }

    public void setClasspath(Path path) {
        if (classpath == null) classpath = path; else classpath.append(path);
    }

    public Path createClasspath() {
        if (classpath == null) classpath = new Path(getProject());
        return classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    @Override
    public void execute() throws BuildException {
        log("Using output dir: " + outputdir, Project.MSG_INFO);
        log("Using classpath: " + classpath, Project.MSG_INFO);
        AntClassLoader loader = getProject().createClassLoader(classpath);
        try {
            loader.setThreadContextLoader();
            log("Loading all Java classes referenced by inner fileset(s) {", Project.MSG_INFO);
            Map<Class<?>, File> classFilesMap = new HashMap<Class<?>, File>();
            for (FileSet fileSet : fileSets) {
                DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
                scanner.setCaseSensitive(true);
                scanner.scan();
                StringBuilder sb = new StringBuilder("    ");
                String[] names = scanner.getIncludedFiles();
                for (String name : names) {
                    if (name.endsWith(".class")) {
                        if (!name.contains("$")) {
                            log(name, Project.MSG_VERBOSE);
                            try {
                                File jFile = new File(scanner.getBasedir(), name);
                                if (!jFile.exists()) throw new FileNotFoundException(jFile.toString());
                                String jClassName = name.substring(0, name.length() - 6).replace(File.separatorChar, '.');
                                Class<?> jClass = loader.loadClass(jClassName);
                                sb.setLength(4);
                                sb.append(jClass.toString());
                                log(sb.toString(), Project.MSG_INFO);
                                classFilesMap.put(jClass, jFile);
                            } catch (Exception e) {
                                log(getStackTrace(e));
                                throw new BuildException("Could not load Java class file: " + name, e);
                            }
                        }
                    } else log("Skipping non class file: " + name, Project.MSG_WARN);
                }
            }
            log("}", Project.MSG_INFO);
            Java2As3 j2As3 = null;
            if (java2as3class == null) java2as3class = DefaultJava2As3.class.getName();
            log("Instantiating Java2As3 class: " + java2as3class, Project.MSG_INFO);
            try {
                j2As3 = (Java2As3) loader.loadClass(java2as3class).newInstance();
            } catch (Exception e) {
                log(getStackTrace(e));
                throw new BuildException("Could not instantiate Java2As3 class: " + java2as3class, e);
            }
            log("Introspecting loaded classes...", Project.MSG_INFO);
            Map<Class<?>, JClass> classJClassMap = null;
            try {
                classJClassMap = JClass.forTypes(classFilesMap);
            } catch (Exception e) {
                log(getStackTrace(e));
                throw new BuildException("Could not introspect Java classes", e);
            }
            log("Setting up the generator...", Project.MSG_INFO);
            As3BeanGenerator generator = new As3BeanGenerator(new AntLogger(this), Collections.unmodifiableMap(classJClassMap), j2As3, uid);
            try {
                if (entitytemplate != null) {
                    log("Loading custom entity template: " + entitytemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.ENTITY_TID, loadScript(entitytemplate));
                }
                if (entitybasetemplate != null) {
                    log("Loading custom entity base template: " + entitybasetemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.ENTITY_BASE_TID, loadScript(entitybasetemplate));
                }
                if (interfacetemplate != null) {
                    log("Loading custom interface template: " + interfacetemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.INTERFACE_TID, loadScript(interfacetemplate));
                }
                if (interfacebasetemplate != null) {
                    log("Loading custom interface base template: " + interfacebasetemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.INTERFACE_BASE_TID, loadScript(interfacebasetemplate));
                }
                if (generictemplate != null) {
                    log("Loading custom generic template: " + generictemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.GENERIC_TID, loadScript(generictemplate));
                }
                if (genericbasetemplate != null) {
                    log("Loading custom generic base template: " + genericbasetemplate, Project.MSG_INFO);
                    generator.setTemplateScript(As3BeanGenerator.GENERIC_BASE_TID, loadScript(genericbasetemplate));
                }
            } catch (IOException e) {
                log(getStackTrace(e));
                throw new BuildException("Error while loading custom template", e);
            }
            log("Calling the generator for each Java class {", Project.MSG_INFO);
            int count = 0;
            for (Map.Entry<Class<?>, JClass> entry : classJClassMap.entrySet()) {
                try {
                    count += generator.generate(entry.getValue(), outputdir);
                } catch (Exception e) {
                    log(getStackTrace(e));
                    throw new BuildException("Could not generate AS3 beans for: " + entry.getKey() + " (" + entry.getValue() + ')', e);
                }
            }
            log("}", Project.MSG_INFO);
            log("Files affected: " + count + (count == 0 ? " (nothing to do)." : "."));
        } finally {
            if (loader != null) loader.resetThreadContextLoader();
        }
    }

    private static String loadScript(String path) throws IOException {
        Reader reader = null;
        try {
            StringWriter sw = new StringWriter();
            reader = new BufferedReader(new FileReader(path));
            int c = -1;
            while ((c = reader.read()) != -1) sw.write(c);
            return sw.toString();
        } finally {
            if (reader != null) reader.close();
        }
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
