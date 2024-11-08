package org.jsorb.generator;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class manages generating a JavaScript-compatible class for a Java
 * class. 
 * 
 * @author Brad Koehn, nVISIA
 */
public class EntityGenerator {

    /** our indent */
    private String indent = "    ";

    /** our package template */
    private String packageTemplate = "JsOrb.jspackage('{package}');\r\n" + "\r\n";

    /** our constructor template */
    private String constructorTemplate = "{class} = function() {\r\n" + "{indent}{superclass}.call(this);\r\n" + "{indent}this.classname = '{class}';\r\n" + "{attributes}" + "};\r\n" + "\r\n" + "JsOrb.extend({class}, {superclass});\r\n" + "\r\n";

    /** static finals template */
    private String constantTemplate = "{class}.{variable} = {type}({value});\r\n" + "\r\n";

    /** attribute metatdata template */
    private String attributeTemplate = "{indent}this.jsOrbAttributes['{property}'] = new Object();\r\n" + "{indent}this.jsOrbAttributes['{property}'].type = '{type}';\r\n" + "{indent}this.jsOrbAttributes['{property}'].getter = {class}.prototype.{getter};\r\n" + "{indent}this.jsOrbAttributes['{property}'].setter = {class}.prototype.{setter};\r\n" + "{indent}this.jsOrbAttributes['{property}'].addListener = {class}.prototype.add{Property}Listener;\r\n" + "{indent}this.jsOrbAttributes['{property}'].removeListener = {class}.prototype.remove{Property}Listener;\r\n";

    ;

    /** array attribute initializer template */
    private String arrayInitializerTemplate = "{indent}this.{property} = new {collectiontype}();\r\n" + "{indent}this.{property}.collectiontype = '{collectiontype}';\r\n";

    /** get method template */
    private String getMethodTemplate = "{class}.prototype.{method} = function() {\r\n" + "{indent}return this.{property};\r\n" + "};\r\n" + "\r\n";

    /** get method template */
    private String getDateMethodTemplate = "{class}.prototype.{method} = function() {\r\n" + "{indent}if (this.{property}) {\r\n" + "{indent}{indent}return new Date(this.{property});\r\n" + "{indent}} else {\r\n" + "{indent}{indent}return null;\r\n" + "{indent}}\r\n" + "};\r\n" + "\r\n";

    /** set method template */
    private String setMethodTemplate = "{class}.prototype.{method} = function(value) {\r\n" + "{indent}if ((value) && (value.valueOf)) {\r\n" + "{indent}{indent}value = value.valueOf();\r\n" + "{indent}}\r\n" + "{indent}this.{property} = this.castProperty('{property}', value);\r\n" + "{indent}this.fireEvent('{property}');\r\n" + "};\r\n" + "\r\n";

    /** add property listener method template */
    private String addPropertyListenerTemplate = "{class}.prototype.add{Property}Listener = function(listener) {\r\n" + "{indent}this.addListener(listener, '{property}');\r\n" + "};\r\n" + "\r\n";

    /** add property listener method template */
    private String removePropertyListenerTemplate = "{class}.prototype.remove{Property}Listener = function(listener) {\r\n" + "{indent}this.removeListener(listener, '{property}');\r\n" + "};\r\n" + "\r\n";

    /** pattern for class name */
    private final Pattern classPattern = Pattern.compile("\\{class\\}");

    /** pattern for superclass name */
    private final Pattern superClassPattern = Pattern.compile("\\{superclass\\}");

    /** pattern for method name */
    private final Pattern methodPattern = Pattern.compile("\\{method\\}");

    /** pattern for property name */
    private final Pattern propertyPattern = Pattern.compile("\\{property\\}");

    /** pattern for property name, initial cap */
    private final Pattern propertyCappedPattern = Pattern.compile("\\{Property\\}");

    /** pattern for indent */
    private final Pattern indentPattern = Pattern.compile("\\{indent\\}");

    /** pattern for package */
    private final Pattern packagePattern = Pattern.compile("\\{package\\}");

    /** pattern for type */
    private final Pattern typePattern = Pattern.compile("\\{type\\}");

    /** pattern for attributes */
    private final Pattern attributesPattern = Pattern.compile("\\{attributes\\}");

    /** pattern for collection type */
    private final Pattern collectionTypePattern = Pattern.compile("\\{collectiontype\\}");

    /** pattern for variable type */
    private final Pattern variablePattern = Pattern.compile("\\{variable\\}");

    /** pattern for value type */
    private final Pattern valuePattern = Pattern.compile("\\{value\\}");

    /** pattern for value type */
    private final Pattern getterPattern = Pattern.compile("\\{getter\\}");

    /** pattern for value type */
    private final Pattern setterPattern = Pattern.compile("\\{setter\\}");

    /** pattern for removing setter from type declaration */
    private final Pattern removeSetterPattern = Pattern.compile("^.*\\.setter = .*$", Pattern.MULTILINE);

    /** java type to js type map for constants */
    private final Map javaTypeToJsTypeMap;

    {
        javaTypeToJsTypeMap = new HashMap();
        javaTypeToJsTypeMap.put(long.class, "Long");
        javaTypeToJsTypeMap.put(Long.class, "Long");
        javaTypeToJsTypeMap.put(int.class, "Integer");
        javaTypeToJsTypeMap.put(Integer.class, "Integer");
        javaTypeToJsTypeMap.put(short.class, "Short");
        javaTypeToJsTypeMap.put(Short.class, "Short");
        javaTypeToJsTypeMap.put(byte.class, "Byte");
        javaTypeToJsTypeMap.put(Byte.class, "Byte");
        javaTypeToJsTypeMap.put(char.class, "Char");
        javaTypeToJsTypeMap.put(Character.class, "Char");
        javaTypeToJsTypeMap.put(String.class, "String");
        javaTypeToJsTypeMap.put(double.class, "Double");
        javaTypeToJsTypeMap.put(Double.class, "Double");
        javaTypeToJsTypeMap.put(boolean.class, "Boolean");
        javaTypeToJsTypeMap.put(Boolean.class, "Boolean");
        javaTypeToJsTypeMap.put(float.class, "Float");
        javaTypeToJsTypeMap.put(Float.class, "Float");
    }

    private final Map javaArrayTypeToJsTypeMap;

    {
        javaArrayTypeToJsTypeMap = new HashMap();
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Object;", "object-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.String;", "string-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Character;", "char-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Long;", "long-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Integer;", "int-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Short;", "short-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Byte;", "byte-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Double;", "double-array");
        javaArrayTypeToJsTypeMap.put("[Ljava.lang.Float;", "float-array");
    }

    private Map javaClassToJsTypeMap;

    {
        javaClassToJsTypeMap = new HashMap();
        javaClassToJsTypeMap.put(String.class, "string");
        javaClassToJsTypeMap.put(Date.class, "date");
        javaClassToJsTypeMap.put(java.sql.Date.class, "date");
        javaClassToJsTypeMap.put(Timestamp.class, "date");
        javaClassToJsTypeMap.put(Boolean.class, "boolean");
        javaClassToJsTypeMap.put(boolean.class, "boolean");
        javaClassToJsTypeMap.put(Integer.class, "int");
        javaClassToJsTypeMap.put(int.class, "int");
        javaClassToJsTypeMap.put(Long.class, "long");
        javaClassToJsTypeMap.put(long.class, "long");
        javaClassToJsTypeMap.put(Short.class, "short");
        javaClassToJsTypeMap.put(short.class, "short");
        javaClassToJsTypeMap.put(Byte.class, "byte");
        javaClassToJsTypeMap.put(byte.class, "byte");
        javaClassToJsTypeMap.put(Float.class, "float");
        javaClassToJsTypeMap.put(float.class, "float");
        javaClassToJsTypeMap.put(Double.class, "double");
        javaClassToJsTypeMap.put(double.class, "double");
    }

    /**
	 * sets the indent 
	 * @param newIndent new indent
	 */
    public void setIndentChars(String newIndent) {
        indent = newIndent;
    }

    /**
	 * Generates a JavaScript definition of a Java class.
	 * 
	 * @param clazz the Class whose definition we wish to generate
	 * @param awriter where to send the JavaScript
	 * @throws IOException if the writer fails to write
	 * @throws IntrospectionException if the bean introspection fails
	 * @throws IllegalAccessException if the bean introspection fails
	 */
    public void generateClass(final Class clazz, Writer awriter) throws IOException, IntrospectionException, IllegalAccessException {
        PrintWriter writer = new PrintWriter(awriter);
        String className = getJsQualifiedClassName(clazz.getName());
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        createPackage(writer, className);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        createConstructor(writer, className, clazz, propertyDescriptors);
        createConstants(writer, className, clazz);
        for (int i = 0; i < propertyDescriptors.length; i++) {
            String property = propertyDescriptors[i].getName();
            createGetMethod(writer, className, propertyDescriptors[i].getReadMethod(), property);
            createSetMethod(writer, className, propertyDescriptors[i].getWriteMethod(), property);
        }
    }

    /**
	 * Creates "constant" declarations for the class. A constant is defined as
	 * any public static final field in the class that's a primitive, a primitive
	 * object wrapper, or a string. 
	 * 
	 * @param clazz the Class whose definition we wish to generate
	 * @param className our class name
	 * @param writer where to send the JavaScript
	 * @throws IllegalAccessException if the bean introspection fails
	 */
    private void createConstants(PrintWriter writer, String className, Class clazz) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            final int modifierMask = Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC;
            if (((fields[i].getModifiers() & modifierMask) == modifierMask) && (javaTypeToJsTypeMap.get(fields[i].getType()) != null)) {
                String constant = classPattern.matcher(constantTemplate).replaceAll(className);
                constant = variablePattern.matcher(constant).replaceAll(fields[i].getName());
                constant = valuePattern.matcher(constant).replaceAll(fields[i].get(null).toString());
                constant = typePattern.matcher(constant).replaceAll((String) javaTypeToJsTypeMap.get(fields[i].getType()));
                writer.print(constant);
            }
        }
    }

    /**
	 * Converts a fully-qualified class name into a js qualified class name
	 * 
	 * @param fqcn the fully qualified class name (e.g., com.foo.Bar)
	 * @return a javascript-compatible qualified class name (e.g., com_foo_Bar)
	 */
    private String getJsQualifiedClassName(String fqcn) {
        return fqcn;
    }

    /**
	 * Writes our package declaration
	 * @param writer our writer
	 * @param className our class name
	 */
    private void createPackage(PrintWriter writer, String className) {
        String packageDeclaration = packagePattern.matcher(packageTemplate).replaceAll(className);
        writer.print(packageDeclaration);
    }

    /**
	 * Builds a constructor 
	 * 
	 * @param writer our writer
	 * @param className the non-qualified class name
	 * @param clazz the class whose constructor we're generating
	 * @param propertyDescriptors the property descriptors for the class
	 */
    private void createConstructor(PrintWriter writer, String className, Class clazz, PropertyDescriptor[] propertyDescriptors) {
        String superclass = "js.lang.EntityObject";
        if (clazz.getSuperclass() != Object.class) {
            superclass = getJsQualifiedClassName(clazz.getSuperclass().getName());
        }
        String constructor = constructorTemplate;
        constructor = classPattern.matcher(constructor).replaceAll(className);
        constructor = superClassPattern.matcher(constructor).replaceAll(superclass);
        constructor = indentPattern.matcher(constructor).replaceAll(indent);
        StringBuffer attributes = new StringBuffer();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            if (propertyDescriptors[i].getName().equals("class")) {
                continue;
            }
            String type = getPropertyType(propertyDescriptors[i]);
            String typeDeclaration = propertyPattern.matcher(attributeTemplate).replaceAll(propertyDescriptors[i].getName());
            typeDeclaration = typePattern.matcher(typeDeclaration).replaceAll(type);
            typeDeclaration = classPattern.matcher(typeDeclaration).replaceAll(className);
            typeDeclaration = getterPattern.matcher(typeDeclaration).replaceAll(propertyDescriptors[i].getReadMethod().getName());
            Method setterMethod = propertyDescriptors[i].getWriteMethod();
            if (setterMethod != null) {
                typeDeclaration = setterPattern.matcher(typeDeclaration).replaceAll(setterMethod.getName());
            } else {
                typeDeclaration = removeSetterPattern.matcher(typeDeclaration).replaceAll("");
            }
            typeDeclaration = propertyCappedPattern.matcher(typeDeclaration).replaceAll(toInitialCap(propertyDescriptors[i].getName()));
            typeDeclaration = indentPattern.matcher(typeDeclaration).replaceAll(indent);
            if ((type.equals("js.util.List")) || (type.equals("js.util.Map")) || (type.equals("js.util.Set")) || (type.endsWith("-array"))) {
                if (type.endsWith("-array")) {
                    type = "Array";
                }
                String arrayInitializer = propertyPattern.matcher(arrayInitializerTemplate).replaceAll(propertyDescriptors[i].getName());
                arrayInitializer = collectionTypePattern.matcher(arrayInitializer).replaceAll(type);
                arrayInitializer = indentPattern.matcher(arrayInitializer).replaceAll(indent);
                attributes.append(arrayInitializer);
            }
            attributes.append(typeDeclaration);
        }
        constructor = attributesPattern.matcher(constructor).replaceAll(attributes.toString());
        writer.write(constructor);
    }

    /**
	 * Determines the type of a property
	 * @param propertyDescriptor decriptor
	 * @return the property type
	 */
    private String getPropertyType(PropertyDescriptor propertyDescriptor) {
        String type;
        Class propertyType = propertyDescriptor.getPropertyType();
        String propertyTypeName = propertyType.getName();
        if (Set.class.isAssignableFrom(propertyType)) {
            type = "js.util.Set";
        } else if (List.class.isAssignableFrom(propertyType)) {
            type = "js.util.List";
        } else if (propertyType.isArray()) {
            if (propertyType.isPrimitive()) {
                type = propertyTypeName + "-array";
            } else {
                type = (String) javaArrayTypeToJsTypeMap.get(propertyTypeName);
                if (type == null) {
                    type = propertyTypeName + "-array";
                }
            }
        } else if (javaClassToJsTypeMap.get(propertyType) != null) {
            type = (String) javaClassToJsTypeMap.get(propertyType);
        } else if (Map.class.isAssignableFrom(propertyType)) {
            type = "js.util.Map";
        } else {
            type = propertyType.getName();
        }
        return type;
    }

    /**
	 * Builds a get method
	 * 
	 * @param writer our writer
	 * @param className the non-qualified class name
	 * @param readMethod the read method
	 * @param propertyName the name of the property
	 */
    private void createGetMethod(PrintWriter writer, String className, Method readMethod, String propertyName) {
        if ((readMethod != null) && (!"class".equals(propertyName))) {
            String getMethod;
            if (Date.class.isAssignableFrom(readMethod.getReturnType())) {
                getMethod = getDateMethodTemplate;
            } else {
                getMethod = getMethodTemplate;
            }
            getMethod = classPattern.matcher(getMethod).replaceAll(className);
            getMethod = methodPattern.matcher(getMethod).replaceAll(readMethod.getName());
            getMethod = propertyPattern.matcher(getMethod).replaceAll(propertyName);
            getMethod = indentPattern.matcher(getMethod).replaceAll(indent);
            writer.write(getMethod);
        }
    }

    /** pattern for primitive objects */
    static final Pattern PRIMITIVE_PATTERN = Pattern.compile("^java.lang.(Long|Integer|Short|Character|Byte|Double|Float)$");

    /**
	 * Builds a set method
	 * 
	 * @param writer our writer
	 * @param className the non-qualified class name
	 * @param writeMethod the write method
	 * @param propertyName the name of the property
	 */
    private void createSetMethod(PrintWriter writer, String className, Method writeMethod, String propertyName) {
        if (writeMethod != null) {
            String setMethod = setMethodTemplate;
            setMethod = classPattern.matcher(setMethod).replaceAll(className);
            setMethod = methodPattern.matcher(setMethod).replaceAll(writeMethod.getName());
            setMethod = propertyPattern.matcher(setMethod).replaceAll(propertyName);
            setMethod = indentPattern.matcher(setMethod).replaceAll(indent);
            writer.write(setMethod);
            createAddListenerMethod(writer, className, propertyName);
            createRemoveListenerMethod(writer, className, propertyName);
        }
    }

    /**
	 * builds an add property listener method
	 * @param writer our writer
	 * @param className our non-qualified class name
	 * @param propertyName our property
	 */
    private void createAddListenerMethod(PrintWriter writer, String className, String propertyName) {
        String addListenerMethod = addPropertyListenerTemplate;
        addListenerMethod = classPattern.matcher(addListenerMethod).replaceAll(className);
        addListenerMethod = propertyPattern.matcher(addListenerMethod).replaceAll(propertyName);
        propertyName = toInitialCap(propertyName);
        addListenerMethod = propertyCappedPattern.matcher(addListenerMethod).replaceAll(propertyName);
        addListenerMethod = indentPattern.matcher(addListenerMethod).replaceAll(indent);
        writer.write(addListenerMethod);
    }

    /**
	 * builds an remove property listener method
	 * @param writer our writer
	 * @param className our non-qualified class name
	 * @param propertyName our property
	 */
    private void createRemoveListenerMethod(PrintWriter writer, String className, String propertyName) {
        String removeListenerMethod = removePropertyListenerTemplate;
        removeListenerMethod = classPattern.matcher(removeListenerMethod).replaceAll(className);
        removeListenerMethod = propertyPattern.matcher(removeListenerMethod).replaceAll(propertyName);
        propertyName = toInitialCap(propertyName);
        removeListenerMethod = propertyCappedPattern.matcher(removeListenerMethod).replaceAll(propertyName);
        removeListenerMethod = indentPattern.matcher(removeListenerMethod).replaceAll(indent);
        writer.write(removeListenerMethod);
    }

    /**
	 * Changes the first letter of a String to upper case
	 * @param string a String, e.g., "foo"
	 * @return a String with the first letter in upper case, e.g., "Foo"
	 */
    private String toInitialCap(String string) {
        char[] chars = string.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }
}
