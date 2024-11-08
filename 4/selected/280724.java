package org.gwtoolbox.bean.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.gwtoolbox.bean.client.*;
import org.gwtoolbox.bean.client.annotation.AttributesClass;
import org.gwtoolbox.bean.client.annotation.Bean;
import org.gwtoolbox.bean.client.annotation.Description;
import org.gwtoolbox.bean.client.annotation.DisplayName;
import org.gwtoolbox.bean.client.validation.BeanValidator;
import org.gwtoolbox.bean.rebind.validation.BeanValidatorGenerator;
import org.gwtoolbox.commons.generator.rebind.AnnotationWrapper;
import org.gwtoolbox.commons.generator.rebind.EasyTreeLogger;
import org.gwtoolbox.commons.generator.rebind.GeneratorUtils;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Helper generator that generates the bean info classes for a specific bean type.
 *
 * @author Uri Boness
 */
public class BeanInfoGenerator {

    private static volatile int typeNameCounter = 0;

    private static final ThreadLocal<Set<String>> typeRegistryThreadLocal = new ThreadLocal<Set<String>>();

    public static String generate(TreeLogger logger, GeneratorContext context, JClassType beanType) throws UnableToCompleteException {
        String packageName = beanType.getPackage().getName();
        String className = "gtx__" + beanType.getSimpleSourceName() + "BeanInfo";
        String qualifiedBeanClassName = packageName + "." + className;
        SourceWriter sourceWriter = getSourceWriter(logger, context, packageName, className);
        if (sourceWriter == null) {
            return qualifiedBeanClassName;
        }
        typeRegistryThreadLocal.set(new HashSet<String>());
        write(new EasyTreeLogger(logger), sourceWriter, context, beanType, className);
        typeRegistryThreadLocal.remove();
        sourceWriter.commit(logger);
        return qualifiedBeanClassName;
    }

    private static SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext context, String packageName, String beanClassName) {
        PrintWriter printWriter = context.tryCreate(logger, packageName, beanClassName);
        if (printWriter == null) {
            return null;
        }
        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, beanClassName);
        composerFactory.addImport(BeanInfo.class.getName());
        composerFactory.addImport(BeanInfoRegistry.class.getName());
        composerFactory.addImport(PropertyDescriptor.class.getName());
        composerFactory.addImport(AbstractPropertyDescriptor.class.getName());
        composerFactory.addImport(Map.class.getName());
        composerFactory.addImport(HashMap.class.getName());
        composerFactory.addImport(Type.class.getName());
        composerFactory.addImport(TypeImpl.class.getName());
        composerFactory.addImport(TypeRegistry.class.getName());
        composerFactory.addImport(PathValueProxy.class.getName());
        composerFactory.addImport(DefaultPathValueProxy.class.getName());
        composerFactory.addImport(PathDescriptorProxy.class.getName());
        composerFactory.addImport(DefaultPathDescriptorProxy.class.getName());
        composerFactory.addImport(UnsupportedOperationException.class.getName());
        composerFactory.addImport(TypeMismatchException.class.getName());
        composerFactory.addImport(BeanValidator.class.getName());
        composerFactory.addImplementedInterface(BeanInfo.class.getName());
        return composerFactory.createSourceWriter(context, printWriter);
    }

    private static void write(EasyTreeLogger logger, SourceWriter writer, GeneratorContext context, JClassType beanType, String className) throws UnableToCompleteException {
        logger = logger.branchDebug("Writing bean info for '" + beanType.getQualifiedSourceName() + "'");
        BeanOracle beanOracle = new BeanOracleBuilder(context.getTypeOracle()).build(logger, beanType);
        JProperty[] properties = beanOracle.getProperties();
        StringBuilder sb = new StringBuilder();
        for (JProperty property : properties) {
            String descriptorClassName = property.getName() + "_Descriptor";
            String varName = property.getName() + "_descriptor";
            writer.println("private final " + descriptorClassName + " " + varName + " = new " + descriptorClassName + "(this);");
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(varName);
        }
        writer.println("private final PropertyDescriptor[] descriptors = new PropertyDescriptor[] {");
        writer.indent();
        writer.println(sb.toString());
        writer.outdent();
        writer.println("};");
        writer.println("private final BeanInfoRegistry beanInfoRegistry;");
        writer.println();
        writer.println("public " + className + "(BeanInfoRegistry beanInfoRegistry) {");
        writer.println("    this.beanInfoRegistry = beanInfoRegistry;");
        writer.println("}");
        writer.println("public BeanInfoRegistry getRegistry() {");
        writer.println("    return beanInfoRegistry;");
        writer.println("}");
        writer.println("public Object newInstance() {");
        writer.println("    return new " + beanType.getQualifiedSourceName() + "();");
        writer.println("}");
        writer.println("private final static Type type;");
        writer.println();
        writer.println("static {");
        writer.indent();
        String typeVarName = writeTypeDesclaration(logger, writer, context, beanType);
        writer.println("type = " + typeVarName + ";");
        writer.outdent();
        writer.println("}");
        writer.println("public Type getType() {");
        writer.println("    return type;");
        writer.println("}");
        writer.println("public PropertyDescriptor[] getPropertyDescriptors() {");
        writer.println("    return descriptors;");
        writer.println("}");
        writer.println("public PropertyDescriptor getPropertyDescriptor(String propertyName) {");
        writer.indent();
        for (JProperty property : properties) {
            writer.println("if (\"" + property.getName() + "\".equals(propertyName)) {");
            writer.println("    return " + property.getName() + "_descriptor;");
            writer.println("}");
            writer.println();
        }
        writer.println("throw new IllegalArgumentException(\"Unknown property '\" + propertyName + \"' in bean '" + beanType.getQualifiedSourceName() + "'\");");
        writer.outdent();
        writer.println("}");
        writer.println("public String getDisplayName() {");
        writer.println("    return \"" + resolveDisplayName(beanType) + "\";");
        writer.println("}");
        writer.println("public String getDescription() {");
        writer.println("    return \"" + resolveDescription(beanType) + "\";");
        writer.println("}");
        String validatorClassName = BeanValidatorGenerator.generate(logger, context, beanType);
        writer.println("private final static BeanValidator<" + beanType.getQualifiedSourceName() + "> validator = new " + validatorClassName + "();");
        writer.println();
        writer.println("public BeanValidator<" + beanType.getQualifiedSourceName() + "> getValidator() {");
        writer.println("    return validator;");
        writer.println("}");
        writer.println();
        writer.println("public PathValueProxy getPathValueProxy(String propertyPath) {");
        writer.println("    return new DefaultPathValueProxy(this, propertyPath);");
        writer.println("}");
        writer.println();
        writer.println("public PathDescriptorProxy getPathDescriptorProxy(String propertyPath) {");
        writer.println("    return new DefaultPathDescriptorProxy(this, propertyPath);");
        writer.println("}");
        writer.println();
        writer.beginJavaDocComment();
        writer.println("Property Descriptor Inner Classes");
        writer.endJavaDocComment();
        for (JProperty property : properties) {
            writeDescriptorClass(logger, writer, context, beanType, property);
            writer.println();
            writer.println();
        }
    }

    private static String writeTypeDesclaration(EasyTreeLogger logger, SourceWriter writer, GeneratorContext context, JType type) throws UnableToCompleteException {
        String typeId = type.toString();
        String varName = nextTypeVarName();
        if (typeRegistryThreadLocal.get().contains(typeId)) {
            writer.println("Type " + varName + " = TypeRegistry.get().findType(\"" + typeId + "\");");
            return varName;
        }
        writer.println("Type " + varName + " = null;");
        boolean array = type.isArray() != null;
        boolean bean = type.isClass() != null && type.isClass().isAnnotationPresent(Bean.class);
        JClassType collectionType = context.getTypeOracle().findType(Collection.class.getName());
        boolean collection = type.isClassOrInterface() != null && type.isClassOrInterface().isAssignableTo(collectionType);
        JClassType mapType = context.getTypeOracle().findType(Map.class.getName());
        boolean map = type.isClassOrInterface() != null && type.isClassOrInterface().isAssignableTo(mapType);
        boolean primitive = type.isPrimitive() != null;
        boolean number = isNumber(type, context.getTypeOracle());
        JClassType dateType = context.getTypeOracle().findType(Date.class.getName());
        boolean date = type.isClassOrInterface() != null && type.isClassOrInterface().isAssignableTo(dateType);
        boolean isEnum = type.isEnum() != null;
        if (array) {
            JType componentType = type.isArray().getComponentType();
            writeTypeDesclaration(logger, writer, context, componentType);
            writer.println(varName + " = new TypeImpl(" + type.getQualifiedSourceName() + ".class, false, false, false, false, false, false, false, \"" + componentType.toString() + "\");");
        } else if (collection) {
            JClassType elementType = BeanGeneratorUtils.findCollectionElementType(type, context);
            writeTypeDesclaration(logger, writer, context, elementType);
            writer.println(varName + " = new TypeImpl(" + type.getQualifiedSourceName() + ".class, false, false, true, false, false, false, false, \"" + elementType.toString() + "\");");
        } else if (map) {
            writer.println(varName + " = new TypeImpl(" + type.getQualifiedSourceName() + ".class, false, false, false, true, false, false, false, null);");
        } else {
            writer.println(varName + " = new TypeImpl(" + type.getQualifiedSourceName() + ".class, " + primitive + ", " + bean + ", " + collection + ", " + map + ", " + date + ", " + number + ", " + isEnum + ", null);");
        }
        writer.outdent();
        typeRegistryThreadLocal.get().add(typeId);
        writer.println("TypeRegistry.get().register(\"" + typeId + "\", " + varName + ");");
        return varName;
    }

    private static boolean isNumber(JType type, TypeOracle typeOracle) throws UnableToCompleteException {
        JClassType numberType = typeOracle.findType(Number.class.getName());
        if (type.isClassOrInterface() != null) {
            return numberType.isAssignableFrom(type.isClassOrInterface());
        }
        if (type.isPrimitive() != null) {
            return typeOracle.findType(type.isPrimitive().getQualifiedBoxedSourceName()).isAssignableTo(numberType);
        }
        return false;
    }

    private static void writeDescriptorClass(EasyTreeLogger logger, SourceWriter writer, GeneratorContext context, JClassType beanType, JProperty property) throws UnableToCompleteException {
        String boxedTypeName = boxedTypeName(property.getType());
        String beanTypeName = beanType.getQualifiedSourceName();
        String propertyTypeId = property.getType().toString();
        logger = logger.branchDebug("Writing property descriptor for '" + property.getName() + "'");
        writer.println("private static class " + property.getName() + "_Descriptor extends AbstractPropertyDescriptor<" + beanTypeName + ", " + boxedTypeName + "> {");
        writer.println("    public " + property.getName() + "_Descriptor(BeanInfo beanInfo) {");
        writer.println("        super(\"" + property.getName() + "\", \"" + propertyTypeId + "\", beanInfo);");
        writer.println("        buildPropertyType();");
        writer.println("    }");
        writer.println("public boolean isReadable() {");
        writer.println("    return " + (property.getGetter() != null) + ";");
        writer.println("}");
        writer.println("public boolean isWritable() {");
        writer.println("    return " + (property.getSetter() != null) + ";");
        writer.println("}");
        logger.debug("writing setter method for '" + property.getName() + "' ('" + boxedTypeName + "')");
        writer.println("    public void setValue(" + beanTypeName + " bean, " + boxedTypeName + " value) {");
        if (property.getSetter() != null) {
            if (property.getType().isPrimitive() != null) {
                writer.println("        if (value == null) {");
                writer.println("            throw new TypeMismatchException(" + property.getType().getQualifiedSourceName() + ".class, null);");
                writer.println("        }");
            }
            writer.println("        bean." + property.getSetter().getName() + "(value);");
        } else {
            writer.println("        throw new UnsupportedOperationException(\"Property '" + property.getName() + "' of bean '" + beanType.getQualifiedSourceName() + "' is not writable (it has no setter\");");
        }
        writer.println("    }");
        logger.debug("writing getter method for '" + property.getName() + "' ('" + boxedTypeName + "')");
        writer.println("    public " + boxedTypeName + " getValue(" + beanTypeName + " bean) {");
        if (property.getGetter() != null) {
            writer.println("        return bean." + property.getGetter().getName() + "();");
        } else {
            writer.println("        throw new UnsupportedOperationException(\"Property '" + property.getName() + "' of bean '" + beanType.getQualifiedSourceName() + "' is not readable (it has no getter\");");
        }
        writer.println("    }");
        String displayName = resolveDisplayName(property);
        if (displayName != null) {
            writer.println("    public String getDisplayName() {");
            writer.println("        return \"" + displayName + "\";");
            writer.println("    }");
        }
        String description = resolveDescription(property);
        if (description != null) {
            writer.println("    public String getDescription() {");
            writer.println("        return \"" + description + "\";");
            writer.println("    }");
        }
        writer.println("    private static Type<" + boxedTypeName + "> buildPropertyType() {");
        String typeVarName = writeTypeDesclaration(logger, writer, context, property.getType());
        writer.println("        return " + typeVarName + ";");
        writer.println("    }");
        writer.indent();
        writeAttributes(logger, writer, context, beanType, property);
        writer.outdent();
        writer.println("}");
    }

    private static void writeAttributes(EasyTreeLogger logger, SourceWriter writer, GeneratorContext context, JClassType beanType, JProperty property) throws UnableToCompleteException {
        writer.println("private final static Map<String, Map<String, Object>> attributesByClassName = new HashMap<String, Map<String, Object>>();");
        writer.println();
        List<Annotation> annotations = extratAllAnnotations(property);
        int i = 0;
        for (Annotation annotation : annotations) {
            if (!annotation.annotationType().isAnnotationPresent(AttributesClass.class)) {
                continue;
            }
            AttributesClass attributesClass = annotation.annotationType().getAnnotation(AttributesClass.class);
            Class<? extends Map<String, Object>> attributesType = attributesClass.value();
            writer.println("static {");
            String typeName = attributesType.getName();
            String varName = "attributes_" + (i++);
            writer.println("    " + typeName + " " + varName + " = new " + typeName + "();");
            AnnotationWrapper wrapper = new AnnotationWrapper(annotation);
            for (String attributeName : wrapper.getAttributeNames()) {
                Object value = wrapper.getValue(attributeName);
                String valueText = String.valueOf(value);
                if (String.class.isInstance(value)) {
                    valueText = "\"" + valueText + "\"";
                } else if (Enum.class.isInstance(value)) {
                    valueText = value.getClass().getName() + "." + ((Enum) value).name();
                }
                writer.println("    " + varName + ".put(\"" + attributeName + "\", " + valueText + ");");
            }
            writer.println("    attributesByClassName.put(\"" + typeName + "\", " + varName + ");");
            writer.println("}");
            writer.println();
        }
        writer.println("public boolean hasAttributes(Class<? extends Map<String, Object>> attributesType) {");
        writer.println("    return attributesByClassName.containsKey(attributesType.getName());");
        writer.println("}");
        writer.println();
        writer.println("public <T extends Map<String, Object>> T getAttributes(Class<T> attributesType) {");
        writer.println("    return (T) attributesByClassName.get(attributesType.getName());");
        writer.println("}");
    }

    private static List<Annotation> extratAllAnnotations(JProperty property) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        JMethod jmethod = property.getSetter();
        if (jmethod != null) {
            Method method = GeneratorUtils.resolveMethod(jmethod);
            annotations.addAll(Arrays.asList(method.getAnnotations()));
        }
        jmethod = property.getGetter();
        if (jmethod != null) {
            Method method = GeneratorUtils.resolveMethod(jmethod);
            annotations.addAll(Arrays.asList(method.getAnnotations()));
        }
        JField jfield = property.getField();
        if (jfield != null) {
            Field field = GeneratorUtils.resolveField(jfield);
            annotations.addAll(Arrays.asList(field.getAnnotations()));
        }
        return annotations;
    }

    private static String boxedTypeName(JType type) {
        JPrimitiveType primitiveType = type.isPrimitive();
        if (primitiveType == null) {
            return type.getQualifiedSourceName();
        }
        return primitiveType.getQualifiedBoxedSourceName();
    }

    private static String resolveDisplayName(JProperty property) {
        JMethod method = property.getSetter();
        if (method != null && method.isAnnotationPresent(DisplayName.class)) {
            return method.getAnnotation(DisplayName.class).value();
        }
        method = property.getGetter();
        if (method != null && method.isAnnotationPresent(DisplayName.class)) {
            return method.getAnnotation(DisplayName.class).value();
        }
        JField field = property.getField();
        if (field != null && field.isAnnotationPresent(DisplayName.class)) {
            return field.getAnnotation(DisplayName.class).value();
        }
        return null;
    }

    private static String resolveDescription(JProperty property) {
        JMethod method = property.getSetter();
        if (method != null && method.isAnnotationPresent(Description.class)) {
            return method.getAnnotation(Description.class).value();
        }
        method = property.getGetter();
        if (method != null && method.isAnnotationPresent(Description.class)) {
            return method.getAnnotation(Description.class).value();
        }
        JField field = property.getField();
        if (field != null && field.isAnnotationPresent(Description.class)) {
            return field.getAnnotation(Description.class).value();
        }
        return null;
    }

    private static String resolveDisplayName(JClassType beanType) {
        if (beanType.isAnnotationPresent(DisplayName.class)) {
            return beanType.getAnnotation(DisplayName.class).value();
        }
        return beanType.getSimpleSourceName();
    }

    private static String resolveDescription(JClassType beanType) {
        if (beanType.isAnnotationPresent(Description.class)) {
            return beanType.getAnnotation(Description.class).value();
        }
        return "";
    }

    private static String nextTypeVarName() {
        return "type" + (typeNameCounter++);
    }
}
