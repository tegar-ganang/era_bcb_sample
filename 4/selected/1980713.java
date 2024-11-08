package fr.albin.compiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import fr.albin.data.model.codes.Codes;
import fr.albin.util.FileUtils;

/**
 * Allows to specify a class with getters and setters. The source will be
 * generated on the fly and then compiled.
 * 
 * @author avigier
 * 
 */
public class ClassGenerator {

    public ClassGenerator() {
        this.importList = new ArrayList<String>();
        this.interfaceList = new ArrayList<String>();
        this.fields = new HashMap<String, Class<?>>();
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public void addImport(String importName) {
        this.importList.add(importName);
    }

    public void addInterface(String className) {
        this.interfaceList.add(className);
    }

    public void addAccessors(String field, Class<?> type) {
        this.fields.put(field, type);
    }

    protected File getFilePackage(File baseDir, String fileExt) {
        File result = null;
        if (this.packageName == null) {
            result = new File(baseDir, this.className + fileExt);
        } else {
            String packagePath = this.packageName.replaceAll("\\.", "/");
            File temp = new File(baseDir, packagePath);
            result = new File(temp, this.className + fileExt);
        }
        return result;
    }

    public void buildClass(File sourceDir, File binaryDir) throws Exception {
        File source = this.getFilePackage(sourceDir, ".java");
        this.generateSource(source);
        boolean result = this.compileSource(source);
        if (result) {
            File newBinClass = new File(source.getParent(), source.getName().replaceAll("\\.java", "\\.class"));
            File binaryClass = null;
            binaryClass = this.getFilePackage(binaryDir, ".class");
            try {
                FileUtils.copyFile(newBinClass, binaryClass);
                newBinClass.delete();
            } catch (Exception e) {
                LOGGER.error("Exception : ", e);
            }
        }
    }

    protected void generateSource(File source) throws IOException {
        LOGGER.info("Generating source file for " + source.getAbsolutePath());
        FileWriter writer = new FileWriter(source);
        if (this.packageName != null) {
            writer.write("package " + this.packageName + ";\n\n");
        }
        writer.write("public class " + className);
        if (this.superClass != null) {
            writer.write(" extends " + this.superClass);
        }
        String interfaceString = "";
        if (this.interfaceList.size() != 0) {
            Iterator<String> iterator = this.interfaceList.iterator();
            while (iterator.hasNext()) {
                if (interfaceString.length() == 0) {
                    interfaceString += " implements ";
                } else {
                    interfaceString += ", ";
                }
                interfaceString += iterator.next();
            }
        }
        writer.write(interfaceString);
        writer.write("{\n");
        this.createAccessors(writer);
        writer.write("}");
        writer.close();
    }

    protected boolean compileSource(File source) {
        LOGGER.info("Compiling source " + source.getAbsolutePath());
        String[] args = { source.getAbsolutePath() };
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        boolean result = com.sun.tools.javac.Main.compile(args, writer) == 0;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Source compilation output : " + stringWriter.getBuffer());
        }
        return result;
    }

    protected void createAccessors(Writer writer) throws IOException {
        Set<String> fieldNames = this.fields.keySet();
        Iterator<String> iterator = fieldNames.iterator();
        while (iterator.hasNext()) {
            String field = iterator.next();
            Class<?> clazz = this.fields.get(field);
            writer.write("private " + clazz.getName() + " " + field + ";\n");
            writer.write("public " + clazz.getName() + " get" + StringUtils.capitalize(field) + "(){\n");
            if (clazz == Codes.class) {
                writer.write("  if (this." + field + " == null) {\n");
                writer.write("    this." + field + " = new " + clazz.getName() + "();}\n");
            }
            writer.write("  return this." + field + ";\n}\n");
            writer.write("public void set" + StringUtils.capitalize(field) + "(" + clazz.getName() + " value" + "){\n");
            writer.write("  this." + field + " = value;\n}\n");
        }
    }

    Map<String, Class<?>> fields;

    private List<String> importList;

    private List<String> interfaceList;

    private String superClass;

    private String packageName;

    private String className;

    private static final Logger LOGGER = Logger.getLogger(ClassGenerator.class);
}
