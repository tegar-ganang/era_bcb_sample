package org.granite.generator.gsp.ejb3.hibernate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.granite.generator.AntLogger;

public class AS3BeanAntTask extends Task {

    private String outputdir = ".";

    private String entitytemplate = null;

    private String entitybasetemplate = null;

    private String id = "id";

    private String uid = "uid";

    private List<FileSet> fileSets = new ArrayList<FileSet>();

    public void setOutputdir(String outputdir) {
        this.outputdir = outputdir;
    }

    public void setEntitytemplate(String entitytemplate) {
        this.entitytemplate = entitytemplate;
    }

    public void setEntitybasetemplate(String entitybasetemplate) {
        this.entitybasetemplate = entitybasetemplate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void addFileset(FileSet fileSet) {
        fileSets.add(fileSet);
    }

    @Override
    public void execute() throws BuildException {
        log("Using output dir: " + outputdir);
        try {
            List<URL> classpath = new ArrayList<URL>(fileSets.size());
            for (FileSet fileSet : fileSets) {
                DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
                classpath.add(scanner.getBasedir().toURL());
            }
            URLClassLoader loader = URLClassLoader.newInstance(classpath.toArray(new URL[0]));
            Thread.currentThread().setContextClassLoader(loader);
        } catch (Exception e) {
            log(getStackTrace(e));
            throw new BuildException("Could not setup generation classpath", e);
        }
        AS3BeanGenerator generator = new AS3BeanGenerator(new AntLogger(this), id, uid);
        if (entitytemplate != null) {
            log("Using custom entity template: " + entitytemplate);
            try {
                generator.setEntityScript(loadScript(entitytemplate));
            } catch (IOException e) {
                log(getStackTrace(e));
                throw new BuildException("Could not load template: " + entitytemplate, e);
            }
        }
        if (entitybasetemplate != null) {
            log("Using custom entity base template: " + entitybasetemplate);
            try {
                generator.setEntityBaseScript(loadScript(entitybasetemplate));
            } catch (IOException e) {
                log(getStackTrace(e));
                throw new BuildException("Could not load basetemplate: " + entitybasetemplate, e);
            }
        }
        for (FileSet fileSet : fileSets) {
            DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
            scanner.setCaseSensitive(true);
            scanner.scan();
            String[] names = scanner.getIncludedFiles();
            for (String name : names) {
                if (name.endsWith(".class")) {
                    if (!name.contains("$")) {
                        try {
                            File jFile = new File(scanner.getBasedir(), name);
                            log("Loading Java class file: " + name);
                            if (!jFile.exists()) throw new FileNotFoundException(jFile.toString());
                            long lastModified = jFile.lastModified();
                            String jClassName = name.substring(0, name.length() - 6).replace(File.separatorChar, '.');
                            Class<?> jClass = Thread.currentThread().getContextClassLoader().loadClass(jClassName);
                            generator.generate(jClass, lastModified, outputdir);
                        } catch (Exception e) {
                            log(getStackTrace(e));
                            throw new BuildException("Could not generate AS3 beans for Java class file: " + name, e);
                        }
                    }
                } else log("Skipping non class file: " + name, Project.MSG_WARN);
            }
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
