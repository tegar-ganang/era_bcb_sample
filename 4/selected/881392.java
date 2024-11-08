package br.gov.demoiselle.eclipse.main.core.configapp;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import br.gov.demoiselle.eclipse.main.IPluginFacade;
import br.gov.demoiselle.eclipse.util.configapp.MessageHelper;
import br.gov.demoiselle.eclipse.util.utility.CoreConstants;
import br.gov.demoiselle.eclipse.util.utility.EclipseUtil;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;
import br.gov.demoiselle.eclipse.util.utility.FrameworkUtil;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.ClassRepresentation;
import br.gov.demoiselle.eclipse.util.utility.classwriter.EnumHelper;
import br.gov.demoiselle.eclipse.util.utility.classwriter.FieldHelper;
import br.gov.demoiselle.eclipse.util.utility.plugin.Configurator;

/**
 * Implements read/write messages from/in project
 * This class uses reflection to find messages from selected project in workspace and
 * writes all new messages in the java file
 * @author CETEC/CTJEE
 * see IPluginFacade
 */
public class MessageFacade implements IPluginFacade<MessageHelper>, CoreConstants {

    private String packageName;

    private String path;

    private String xml;

    /**
	 * Read implemented Messages Classes (Error, Info, Fatal) on selected project
	 */
    public List<MessageHelper> read() {
        Configurator reader = new Configurator();
        this.packageName = reader.readMessagePackage(xml);
        ArrayList<MessageHelper> msgs = new ArrayList<MessageHelper>();
        try {
            Class<?> clazzError = EclipseUtil.getClass(packageName + "." + CLASS_NAME_ERRORMESSAGE);
            if (clazzError != null) {
                msgs.addAll(extractMessagesFromClass(clazzError, SEVERITY_ERROR));
            }
        } catch (Exception e) {
            System.err.println("Fail on reading Error Messages class! \n maybe not implemented yet! \n");
        }
        try {
            Class<?> clazzInfo = EclipseUtil.getClass(packageName + "." + CLASS_NAME_INFOMESSAGE);
            if (clazzInfo != null) {
                msgs.addAll(extractMessagesFromClass(clazzInfo, SEVERITY_INFO));
            }
        } catch (Exception e) {
            System.err.println("Fail on reading Info Messages class! \n maybe not implemented yet! \n");
        }
        try {
            Class<?> clazzFatal = EclipseUtil.getClass(packageName + "." + CLASS_NAME_FATALMESSAGE);
            if (clazzFatal != null) {
                msgs.addAll(extractMessagesFromClass(clazzFatal, SEVERITY_FATAL));
            }
        } catch (Exception e) {
            System.err.println("Fail on reading Fatal Messages class! \n maybe not implemented yet! \n");
        }
        return msgs;
    }

    /**
	 * Extracts messages (methods) from class
	 * @param clazz Message Class
	 * @param severity Severity of clazz 
	 * @return List<MessageHelper> Methods from clazz
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
    public List<MessageHelper> extractMessagesFromClass(Class<?> clazz, String severity) throws IllegalArgumentException, IllegalAccessException {
        ArrayList<MessageHelper> msgs = new ArrayList<MessageHelper>();
        HashMap<String, String> enums = FrameworkUtil.extractIMessageEnums(clazz);
        for (String key : enums.keySet()) {
            MessageHelper message = new MessageHelper();
            message.setName(key);
            message.setMessage(enums.get(key));
            message.setSeverity(severity);
            msgs.add(message);
        }
        return msgs;
    }

    /**
	 * Create Messages Classes from List
	 *  @param messages List of messages 
	 */
    public void write(List<MessageHelper> messages) throws CoreException {
        if (hasEnums(messages, SEVERITY_ERROR)) {
            ClassHelper clazzError = generateClassMessage(CLASS_NAME_ERRORMESSAGE, SEVERITY_ERROR, messages);
            FileUtil.writeClassFile(path, clazzError, false, false);
        }
        if (hasEnums(messages, SEVERITY_FATAL)) {
            ClassHelper clazzFatal = generateClassMessage(CLASS_NAME_FATALMESSAGE, SEVERITY_FATAL, messages);
            FileUtil.writeClassFile(path, clazzFatal, false, false);
        }
        if (hasEnums(messages, SEVERITY_INFO)) {
            ClassHelper clazzInfo = generateClassMessage(CLASS_NAME_INFOMESSAGE, SEVERITY_INFO, messages);
            FileUtil.writeClassFile(path, clazzInfo, false, false);
        }
        Configurator reader = new Configurator();
        reader.writeMessagePackage(packageName, xml, false);
        EclipseUtil.updateProject();
    }

    /**
	 * Generates a Message Class
	 * @param className
	 * @param severity
	 * @param messages
	 * @return
	 */
    public ClassHelper generateClassMessage(String className, String severity, List<MessageHelper> messages) {
        ClassHelper clazz = new ClassHelper();
        clazz.setPackageName(packageName);
        clazz.setInterfaces(new ArrayList<ClassRepresentation>());
        clazz.getInterfaces().add(FrameworkUtil.getIMessage());
        if (clazz.getImports() == null) {
            clazz.setImports(new ArrayList<ClassRepresentation>());
        }
        clazz.getImports().add(new ClassRepresentation("java.util.Locale"));
        clazz.getImports().add(FrameworkUtil.getSeverity());
        clazz.setName(className);
        List<EnumHelper> enums = new ArrayList<EnumHelper>();
        for (MessageHelper message : messages) {
            if (message.getSeverity().equals(severity)) {
                EnumHelper e = new EnumHelper();
                e.setName(message.getName());
                List<String> params = new ArrayList<String>();
                params.add("\"" + message.getMessage() + "\"");
                e.setParameters(params);
                enums.add(e);
            }
        }
        clazz.setEnums(enums);
        clazz.setFields(new ArrayList<FieldHelper>());
        FieldHelper field = new FieldHelper();
        field.setName("label");
        field.setType(new ClassRepresentation("java.lang.String"));
        field.setModifier(Modifier.PRIVATE);
        field.setHasGetMethod(false);
        field.setHasSetMethod(false);
        clazz.getFields().add(field);
        clazz.setStringMethods(getMethods(severity, className));
        return clazz;
    }

    /**
	 * 
	 * @param type
	 * @param className
	 * @return List of defined Methods for Message Class
	 */
    public List<String> getMethods(String type, String className) {
        List<String> methods = new ArrayList<String>();
        methods.add("private " + className + "(String label) {this.label = label;}");
        methods.add("public String getKey() {return this.toString();}");
        methods.add("public String getLabel() {return label;}");
        methods.add("public Locale getLocale() {return new Locale(\"pt\", \"BR\");}");
        methods.add("public Severity getSeverity() {return Severity." + type.toUpperCase() + ";}");
        methods.add("public String getResourceName() {return \"error\";}");
        return methods;
    }

    /**
	 * 
	 * @param messages
	 * @param type
	 * @return if it has a Enumeration type
	 */
    public boolean hasEnums(List<MessageHelper> messages, String type) {
        if (messages != null) {
            for (MessageHelper message : messages) {
                if (message.getSeverity().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
	 * 
	 * @return Qualified name of Package for Message Classes
	 */
    public String getPackageName() {
        if (packageName == null || packageName.equals("")) {
            Configurator reader = new Configurator();
            this.packageName = reader.readMessagePackage(xml);
        }
        return packageName;
    }

    /**
	 * remove all Message Classes
	 */
    public void clean() {
        Configurator reader = new Configurator();
        this.packageName = reader.readMessagePackage(xml);
        try {
            FileUtil.removeClass(packageName + "." + CLASS_NAME_ERRORMESSAGE);
        } catch (Exception e) {
            System.out.println("No Messages class! \n maybe not implemented yet! \n" + e.getMessage());
        }
        try {
            FileUtil.removeClass(packageName + "." + CLASS_NAME_INFOMESSAGE);
        } catch (Exception e) {
            System.out.println("No info Messages class! \n maybe not implemented yet! \n" + e.getMessage());
        }
        try {
            FileUtil.removeClass(packageName + "." + CLASS_NAME_FATALMESSAGE);
        } catch (Exception e) {
            System.out.println("No Fatal Messages class! \n maybe not implemented yet! \n" + e.getMessage());
        }
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
	 * Write the Package Name for Messages on Demoiselle's File configuration
	 * @throws CoreException
	 */
    public void writePackage() throws CoreException {
        Configurator reader = new Configurator();
        reader.writeMessagePackage(packageName, xml, false);
        EclipseUtil.updateProject();
    }
}
