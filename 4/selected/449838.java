package com.infinity.wavemvc.rebind;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import org.apache.commons.beanutils.PropertyUtils;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.infinity.wavemvc.client.mvc.WaveBean;
import com.infinity.wavemvc.client.mvc.WaveMVCException;

public class WaveBeanGenerator extends Generator {

    private static final boolean DUMPING_GENERATED_JAVA_TO_CONSOLE = false;

    private static final boolean DUMPING_GENERATED_JAVA_TO_SOURCE_FILES = false;

    private static final String SOURCE_PATH = "gen-src/";

    private String packageName;

    private String className;

    private Class<? extends WaveBean> superClass;

    private PrintWriter console;

    private PrintWriter fsConsole;

    @Override
    @SuppressWarnings("unchecked")
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {
        TypeOracle typeOracle = context.getTypeOracle();
        try {
            JClassType classType = typeOracle.getType(typeName);
            packageName = classType.getPackage().getName();
            className = classType.getSimpleSourceName() + "Proxy";
            superClass = (Class<? extends WaveBean>) Class.forName(typeName);
            if (DUMPING_GENERATED_JAVA_TO_CONSOLE) {
                console = new PrintWriter(System.out);
                generateClass(logger, context, console);
            }
            if (DUMPING_GENERATED_JAVA_TO_SOURCE_FILES) {
                String pathname = SOURCE_PATH + packageName.replace('.', '/') + '/';
                new File(pathname).mkdirs();
                String fileName = className + ".java";
                fsConsole = new PrintWriter(new FileOutputStream(pathname + fileName));
                generateClass(logger, context, fsConsole);
            }
            generateClass(logger, context, context.tryCreate(logger, packageName, className));
        } catch (ClassNotFoundException e) {
            throw new WaveMVCException("Unable to create bean for generation information", e);
        } catch (Exception e) {
            logger.log(TreeLogger.ERROR, "Error in generating the proxy object for " + typeName, e);
        }
        return packageName + "." + className;
    }

    private void generateClass(TreeLogger logger, GeneratorContext context, PrintWriter printWriter) {
        if (printWriter == null) {
            return;
        }
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, className);
        composer.setSuperclass(superClass.getName());
        composer.addImport("com.infinity.wavemvc.client.mvc.WaveClientHelper");
        composer.addImplementedInterface("com.infinity.wavemvc.client.mvc.WaveSyncableBean");
        SourceWriter sw = composer.createSourceWriter(context, printWriter);
        sw.println();
        generateProperties(sw);
        generateDefaultConstructor(sw);
        sw.println();
        generateSetUUID(sw);
        generateAccessors(sw);
        generateSetValueMethod(sw);
        sw.outdent();
        sw.println("}");
        printWriter.flush();
        if (printWriter != console && printWriter != fsConsole) {
            context.commit(logger, printWriter);
        }
    }

    private void generateSetUUID(SourceWriter sw) {
        sw.println("public void setUUID(String UUIDValue) {");
        sw.indent();
        sw.println("uuid = UUIDValue;");
        sw.outdent();
        sw.println("}");
        sw.println();
    }

    private void generateSetValueMethod(SourceWriter sw) {
        sw.println("public void setSyncValueFromWave(String memberName, Object valueObject) {");
        sw.indent();
        PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors(superClass);
        boolean firstProp = true;
        for (PropertyDescriptor pd : props) {
            Method read = pd.getReadMethod();
            Method write = pd.getWriteMethod();
            if (read != null && write != null) {
                if (!firstProp) {
                    sw.print("} else ");
                } else {
                    firstProp = false;
                }
                sw.println("if (memberName.equals(\"" + pd.getDisplayName() + "\")) {");
                sw.indent();
                Class<?> propType = pd.getPropertyType();
                if (propType.isPrimitive()) {
                    String type = propType.getName();
                    String parseMeType = "********* " + type + "  ";
                    if ("int".equals(type)) {
                        parseMeType = "Integer.parseInt";
                    } else if ("boolean".equals(type)) {
                        parseMeType = "Boolean.parseBoolean";
                    } else if ("byte".equals(type)) {
                        parseMeType = "Byte.parseByte";
                    } else if ("char".equals(type)) {
                        parseMeType = "(char)Integer.parseInt";
                    } else if ("double".equals(type)) {
                        parseMeType = "Double.parseDouble";
                    } else if ("float".equals(type)) {
                        parseMeType = "Float.parseFloat";
                    } else if ("long".equals(type)) {
                        parseMeType = "Long.parseLong";
                    } else if ("short".equals(type)) {
                        parseMeType = "Short.parseShort";
                    }
                    sw.println("super." + write.getName() + "(" + parseMeType + "((String) valueObject));");
                } else {
                    sw.println("super." + write.getName() + "((" + read.getReturnType().getCanonicalName() + ") valueObject);");
                }
                sw.outdent();
            }
        }
        sw.println("}");
        sw.outdent();
        sw.println("}");
    }

    private void generateAccessors(SourceWriter sw) {
        sw.println("public String getUUID() {");
        sw.indent();
        sw.println("return uuid;");
        sw.outdent();
        sw.println("}");
        sw.println();
        sw.println("public void setSettableValues(boolean settable) {");
        sw.indent();
        sw.println("propertySettable = settable;");
        sw.outdent();
        sw.println("}");
        sw.println();
        PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors(superClass);
        for (PropertyDescriptor pd : props) {
            Method write = pd.getWriteMethod();
            Method read = pd.getReadMethod();
            if (write != null && read != null) {
                StringBuffer buff = new StringBuffer();
                Class<?> propType = read.getReturnType();
                buff.append("public void ").append(write.getName()).append("(").append(propType.getCanonicalName()).append(" newVal) {");
                sw.println(buff.toString());
                sw.indent();
                sw.println("if (propertySettable) {");
                sw.indent();
                sw.println("super." + write.getName() + "(newVal);");
                sw.outdent();
                sw.println("} else {");
                sw.indent();
                sw.println("WaveClientHelper.syncData(this, \"" + pd.getName() + "\", String.valueOf(newVal));");
                sw.outdent();
                sw.println("}");
                sw.outdent();
                sw.println("}");
            }
        }
        sw.println();
    }

    private void generateProperties(SourceWriter sw) {
        sw.println("private String uuid = WaveClientHelper.generateUUID();");
        sw.println("private boolean propertySettable = false;");
        sw.println();
    }

    private void generateDefaultConstructor(SourceWriter sw) {
        sw.println("public " + className + "() {");
        sw.indent();
        sw.println("setSettableValues(false);");
        sw.println("WaveClientHelper.addWaveBean(this);");
        sw.outdent();
        sw.println("}");
        sw.println();
    }
}
