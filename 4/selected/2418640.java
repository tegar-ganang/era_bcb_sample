package com.google.gwt.search.jsio.rebind;

import com.google.gwt.search.jsio.client.Constructor;
import com.google.gwt.search.jsio.client.Global;
import com.google.gwt.search.jsio.client.JSWrapper;
import com.google.gwt.search.jsio.client.NoIdentity;
import com.google.gwt.search.jsio.client.ReadOnly;
import com.google.gwt.search.jsio.client.impl.JSONWrapperUtil;
import com.google.gwt.search.jsio.client.impl.MetaDataName;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.HasMetaData;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Generator that provides implementations of JSWrapper.
 */
public class JSWrapperGenerator extends Generator {

    /**
   * The name of the field within the backing object that refers back to the
   * JSWrapper object.
   */
    public static final String BACKREF = "__gwtPeer";

    /**
   * The name of the static field that contains the class's Extractor instance.
   */
    protected static final String EXTRACTOR = "__extractor";

    /**
   * Singleton instance of the FragmentGeneratorOracle for the system.
   */
    protected static final FragmentGeneratorOracle FRAGMENT_ORACLE = new FragmentGeneratorOracle();

    /**
   * The name of the backing object field.
   */
    protected static final String OBJ = "jsoPeer";

    /**
   * Allows the metadata warning to be turned off to prevent log spam.
   */
    private static final boolean SUPPRESS_WARNINGS = Boolean.getBoolean("JSWrapper.suppressMetaWarnings");

    /**
   * Extract an Annotation. If the requested Annotation does not exist on the
   * target node, the target's metadata will be examined for a tag based on the
   * requested Annotation's {@link MetaDataName} meta-annotation. If the target
   * has metadata which can be interpreted as the return type of the requested
   * Annotation's value method, a {@link Proxy} will be synthesized. The proxy
   * mode is only to support existing functionality, all new features should be
   * added via new annotations.
   *
   * @param <A> the desired type of Annotation
   * @param <M> the type of object to search
   * @param logger a logger
   * @param target the object to search
   * @param annotation the desired type of annotation
   * @return an instance of the requested annotation, or <code>null</code> if
   *         the annotation is not present and an instance of the annotation
   *         cannot be synthesized due to lack of metadata
   * @throws UnableToCompleteException if metadata with the correct tag exists
   *           but cannot be interpreted as the return type of the annotation's
   *           value method
   */
    @SuppressWarnings("deprecation")
    static <A extends Annotation, M extends HasAnnotations & HasMetaData> A hasTag(TreeLogger logger, M target, final Class<A> annotation) throws UnableToCompleteException {
        logger = logger.branch(TreeLogger.TRACE, "Looking for annotation/meta " + annotation.getName(), null);
        A toReturn = target.getAnnotation(annotation);
        if (toReturn != null) {
            logger.log(TreeLogger.TRACE, "Found Annotation instance", null);
            return toReturn;
        }
        MetaDataName metaDataName = annotation.getAnnotation(MetaDataName.class);
        if (metaDataName == null) {
            logger.log(TreeLogger.TRACE, "No legacy support for this annotation", null);
            return null;
        }
        String tagName = metaDataName.value();
        boolean hasTag = false;
        for (String tag : target.getMetaDataTags()) {
            if (tagName.equals(tag)) {
                hasTag = true;
                if (!SUPPRESS_WARNINGS) {
                    logger.log(TreeLogger.WARN, target + " uses deprecated metadata.  Replace with annotation " + annotation.getName(), null);
                }
                break;
            }
        }
        if (!hasTag) {
            logger.log(TreeLogger.TRACE, "No metadata with tag " + tagName, null);
            return null;
        }
        Object value;
        try {
            Method valueMethod = annotation.getMethod("value");
            Class<?> returnType = valueMethod.getReturnType();
            String[][] metaData = target.getMetaData(metaDataName.value());
            Object annotationDefaultValue;
            try {
                annotationDefaultValue = annotation.getMethod("value").getDefaultValue();
            } catch (NoSuchMethodException e) {
                annotationDefaultValue = null;
            }
            if (annotationDefaultValue == null && metaData.length == 0) {
                logger.log(TreeLogger.ERROR, "Metadata " + tagName + " must appear exactly once", null);
                throw new UnableToCompleteException();
            } else if (returnType.equals(String.class)) {
                if (metaData[0].length == 1) {
                    logger.log(TreeLogger.TRACE, "Using value from metadata", null);
                    value = metaData[0][0];
                } else if (annotationDefaultValue != null) {
                    value = annotationDefaultValue;
                } else {
                    logger.log(TreeLogger.ERROR, "Metadata " + tagName + " must have exactly one value", null);
                    throw new UnableToCompleteException();
                }
            } else if (annotationDefaultValue != null) {
                logger.log(TreeLogger.TRACE, "Using annotation's default value", null);
                value = annotationDefaultValue;
            } else {
                logger.log(TreeLogger.ERROR, "Can't deal with return type " + returnType.getName(), null);
                throw new UnableToCompleteException();
            }
        } catch (NoSuchMethodException e) {
            value = null;
        }
        final Object finalValue = value;
        Object proxy = Proxy.newProxyInstance(annotation.getClassLoader(), new Class<?>[] { annotation }, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.equals("annotationType")) {
                    return annotation;
                } else if (name.equals("hashCode")) {
                    return 0;
                } else if (name.equals("toString")) {
                    return "Proxy for type " + annotation.getName() + " : " + finalValue;
                } else if (name.equals("value")) {
                    return finalValue;
                } else if (method.getDefaultValue() != null) {
                    return method.getDefaultValue();
                }
                throw new RuntimeException("Don't know how to service " + name + " in type " + method.getDeclaringClass().getName());
            }
        });
        return annotation.cast(proxy);
    }

    /**
   * Get the erased type of the parameterization of the JSWrapper. Returns
   * <code>null</code> if JSWrapper is not in the class's inhertence
   * hierarchy.
   */
    private static JClassType findJSWrapperParameterization(TypeOracle oracle, JClassType extendsJSWrapper) {
        if (extendsJSWrapper == null) {
            return null;
        }
        JClassType rawJSWrapper = oracle.findType(JSWrapper.class.getName()).getErasedType();
        JParameterizedType asParam = extendsJSWrapper.isParameterized();
        if (asParam != null && asParam.getErasedType().equals(rawJSWrapper)) {
            return asParam.getTypeArgs()[0].getErasedType();
        }
        JClassType toReturn = findJSWrapperParameterization(oracle, extendsJSWrapper.getSuperclass());
        if (toReturn != null) {
            return toReturn;
        }
        for (JClassType implemented : extendsJSWrapper.getImplementedInterfaces()) {
            toReturn = findJSWrapperParameterization(oracle, implemented);
            if (toReturn != null) {
                return toReturn;
            }
        }
        return null;
    }

    /**
   * Entry point into the Generator.
   */
    @Override
    public final String generate(TreeLogger logger, GeneratorContext context, java.lang.String typeName) throws UnableToCompleteException {
        final TypeOracle typeOracle = context.getTypeOracle();
        final JClassType sourceType = typeOracle.findType(typeName);
        if (sourceType == null) {
            logger.log(TreeLogger.ERROR, "Could not find requested typeName", null);
            throw new UnableToCompleteException();
        }
        final String generatedSimpleSourceName = "__" + sourceType.getName().replaceAll("\\.", "__") + "Impl";
        final ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(sourceType.getPackage().getName(), generatedSimpleSourceName);
        f.addImport(GWT.class.getName());
        f.addImport(JavaScriptObject.class.getName());
        f.addImport("com.google.gwt.search.jsio.client.*");
        f.addImport("com.google.gwt.search.jsio.client.impl.*");
        if (sourceType.isClass() != null) {
            f.setSuperclass(sourceType.getQualifiedSourceName());
        } else if (sourceType.isInterface() != null) {
            f.addImplementedInterface(sourceType.getQualifiedSourceName());
        } else {
            logger.log(TreeLogger.ERROR, "Requested JClassType is neither a class nor an interface.", null);
            throw new UnableToCompleteException();
        }
        final PrintWriter out = context.tryCreate(logger, sourceType.getPackage().getName(), generatedSimpleSourceName);
        if (out != null) {
            final SourceWriter sw = f.createSourceWriter(context, out);
            final Map<String, Task> propertyAccessors = TaskFactory.extractMethods(logger, typeOracle, sourceType, getPolicy());
            FragmentGeneratorContext fragmentContext = new FragmentGeneratorContext();
            fragmentContext.parentLogger = logger;
            fragmentContext.fragmentGeneratorOracle = FRAGMENT_ORACLE;
            fragmentContext.typeOracle = typeOracle;
            fragmentContext.sw = sw;
            fragmentContext.objRef = "this.@" + f.getCreatedClassName() + "::" + OBJ;
            fragmentContext.simpleTypeName = generatedSimpleSourceName;
            fragmentContext.qualifiedTypeName = f.getCreatedClassName();
            fragmentContext.returnType = sourceType;
            fragmentContext.creatorFixups = new HashSet<JClassType>();
            fragmentContext.readOnly = hasTag(logger, sourceType, ReadOnly.class) != null;
            fragmentContext.maintainIdentity = !(fragmentContext.readOnly || hasTag(logger, sourceType, NoIdentity.class) != null);
            fragmentContext.tasks = propertyAccessors.values();
            validateType(propertyAccessors, fragmentContext);
            writeBoilerplate(logger, fragmentContext);
            if (!fragmentContext.readOnly) {
                writeEmptyFieldInitializerMethod(logger, propertyAccessors, fragmentContext);
            }
            writeMethods(fragmentContext, propertyAccessors);
            writeFixups(logger, typeOracle, sw, fragmentContext.creatorFixups);
            sw.commit(logger);
        }
        return f.getCreatedClassName();
    }

    /**
   * Specifies the first parameter of imported methods to pass to the imported
   * JavaScript function.
   */
    protected int getImportOffset() {
        return 0;
    }

    protected TaskFactory.Policy getPolicy() {
        return TaskFactory.WRAPPER_POLICY;
    }

    /**
   * Extracts the parameter from a setter method that contains the value to
   * store into the backing object.
   */
    protected JParameter getSetterParameter(JMethod setter) {
        return setter.getParameters()[0];
    }

    /**
   * Aggregate pre-write validation checks.
   */
    protected void validateType(Map<String, Task> propertyAccessors, FragmentGeneratorContext context) throws UnableToCompleteException {
        boolean error = false;
        for (Task pair : propertyAccessors.values()) {
            error |= pair.validate(this, context);
        }
        if (error) {
            throw new UnableToCompleteException();
        }
    }

    /**
   * Writes common boilerplate code for all implementations.
   */
    protected void writeBoilerplate(TreeLogger logger, FragmentGeneratorContext context) throws UnableToCompleteException {
        SourceWriter sw = context.sw;
        TypeOracle typeOracle = context.typeOracle;
        JType returnType = context.returnType;
        sw.print("private JavaScriptObject ");
        sw.print(OBJ);
        sw.println(";");
        sw.print("public ");
        sw.print(context.simpleTypeName);
        sw.println("() {");
        sw.indent();
        sw.println("setJavaScriptObject(__nativeInit());");
        sw.outdent();
        sw.println("}");
        JClassType asClass = context.returnType.isClassOrInterface();
        Constructor constructorAnnotation = hasTag(logger, asClass, Constructor.class);
        Global globalAnnotation = hasTag(logger, asClass, Global.class);
        String constructor;
        if (globalAnnotation != null) {
            constructor = globalAnnotation.value();
        } else if (constructorAnnotation != null) {
            constructor = "new " + constructorAnnotation.value() + "()";
        } else {
            boolean hasImports = false;
            for (Task t : context.tasks) {
                hasImports |= t.imported != null;
                if (hasImports) {
                    break;
                }
            }
            if (!hasImports) {
                constructor = "{}";
            } else {
                constructor = "null";
            }
        }
        JClassType parameterization = findJSWrapperParameterization(context.typeOracle, asClass);
        if (parameterization == null) {
            parameterization = asClass;
        }
        sw.println("private native JavaScriptObject __nativeInit() /*-{");
        sw.indent();
        sw.print("return ");
        sw.print(constructor);
        sw.println(";");
        sw.outdent();
        sw.println("}-*/;");
        sw.println("public JavaScriptObject getJavaScriptObject() {");
        sw.indent();
        sw.print("return ");
        sw.print(OBJ);
        sw.println(";");
        sw.outdent();
        sw.println("}");
        sw.println("public void setJSONData(String data)");
        sw.println("throws JSONWrapperException {");
        sw.indent();
        sw.println("setJavaScriptObject(JSONWrapperUtil.evaluate(data));");
        sw.outdent();
        sw.println("}");
        sw.print("public " + parameterization.getParameterizedQualifiedSourceName() + " setJavaScriptObject(");
        sw.println("JavaScriptObject obj) {");
        sw.indent();
        sw.println("if (obj != null) {");
        sw.indent();
        for (Task t : context.tasks) {
            if (t.imported != null) {
                String fieldName = t.getFieldName(logger);
                sw.print("assert JSONWrapperUtil.hasField(obj, \"");
                sw.print(fieldName);
                sw.print("\") : \"Backing JSO missing imported function ");
                sw.print(fieldName);
                sw.println("\";");
            }
        }
        sw.outdent();
        sw.println("}");
        sw.println("return setJavaScriptObjectNative(obj);");
        sw.outdent();
        sw.println("}");
        sw.print("public native " + context.simpleTypeName + " setJavaScriptObjectNative(JavaScriptObject obj) /*-{");
        sw.indent();
        if (context.maintainIdentity) {
            sw.print("if (");
            sw.print(context.objRef);
            sw.println(") {");
            sw.indent();
            sw.print("delete ");
            sw.print(context.objRef);
            sw.print(".");
            sw.print(BACKREF);
            sw.println(";");
            sw.outdent();
            sw.println("}");
        }
        sw.println("if (!obj) {");
        sw.indent();
        sw.print(context.objRef);
        sw.println(" = null;");
        sw.println("return this;");
        sw.outdent();
        sw.println("}");
        if (context.maintainIdentity) {
            sw.print("if (obj.");
            sw.print(BACKREF);
            sw.println(") {");
            sw.indent();
            sw.println("@" + JSONWrapperUtil.class.getName() + "::throwMultipleWrapperException()();");
            sw.outdent();
            sw.println("}");
        }
        sw.print(context.objRef);
        sw.println(" = obj;");
        if (context.maintainIdentity) {
            sw.print(context.objRef);
            sw.print(".");
            sw.print(BACKREF);
            sw.println(" = this;");
        }
        if (!context.readOnly) {
            sw.print("this.@");
            sw.print(context.qualifiedTypeName);
            sw.print("::__initializeEmptyFields(Lcom/google/gwt/core/client/JavaScriptObject;)(");
            sw.print(context.objRef);
            sw.println(");");
        }
        sw.println("return this;");
        sw.outdent();
        sw.println("}-*/;");
        sw.println("public final Extractor<" + parameterization.getParameterizedQualifiedSourceName() + "> getExtractor() {");
        sw.indent();
        sw.print("return ");
        sw.print(EXTRACTOR);
        sw.println(";");
        sw.outdent();
        sw.println("}");
        sw.print("private final static Extractor ");
        sw.print(EXTRACTOR);
        sw.print(" = new Extractor() {");
        sw.indent();
        FragmentGeneratorContext subParams = new FragmentGeneratorContext(context);
        subParams.parameterName = "obj";
        FragmentGenerator fragmentGenerator = context.fragmentGeneratorOracle.findFragmentGenerator(logger, typeOracle, returnType);
        sw.println("public native Object fromJS(JavaScriptObject obj) /*-{");
        sw.indent();
        sw.print("return ");
        fragmentGenerator.fromJS(subParams);
        sw.println(";");
        sw.outdent();
        sw.println("}-*/;");
        sw.println("public native JavaScriptObject toJS(Object obj) /*-{");
        sw.indent();
        sw.print("return ");
        fragmentGenerator.toJS(subParams);
        sw.println(";");
        sw.outdent();
        sw.println("}-*/;");
        sw.outdent();
        sw.println("};");
    }

    protected void writeConstructor(FragmentGeneratorContext context, JMethod constructor) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing constructor " + constructor.getName(), null);
        SourceWriter sw = context.sw;
        JParameter[] parameters = constructor.getParameters();
        if (parameters == null) {
            parameters = new JParameter[0];
        }
        sw.print("public native ");
        sw.print(constructor.getReturnType().getQualifiedSourceName());
        sw.print(" ");
        sw.print(constructor.getName());
        sw.print("(");
        for (int i = 0; i < parameters.length; i++) {
            JType returnType = parameters[i].getType();
            JParameterizedType pType = returnType.isParameterized();
            if (pType != null) {
                sw.print(pType.getRawType().getQualifiedSourceName());
            } else {
                sw.print(returnType.getQualifiedSourceName());
            }
            sw.print(" ");
            sw.print(parameters[i].getName());
            if (i < parameters.length - 1) {
                sw.print(", ");
            }
        }
        sw.print(")");
        sw.println(" /*-{");
        sw.indent();
        JType returnType = constructor.getReturnType();
        sw.print("var jsReturn = ");
        sw.print("new ");
        Constructor constructorAnnotation = hasTag(logger, constructor, Constructor.class);
        sw.print(constructorAnnotation.value());
        sw.print("(");
        for (int i = getImportOffset(); i < parameters.length; i++) {
            JType subType = parameters[i].getType();
            FragmentGeneratorContext subParams = new FragmentGeneratorContext(context);
            subParams.returnType = subType;
            subParams.parameterName = parameters[i].getName();
            FragmentGenerator fragmentGenerator = context.fragmentGeneratorOracle.findFragmentGenerator(logger, context.typeOracle, subType);
            if (fragmentGenerator == null) {
                logger.log(TreeLogger.ERROR, "No fragment generator for " + returnType.getQualifiedSourceName(), null);
                throw new UnableToCompleteException();
            }
            fragmentGenerator.toJS(subParams);
            if (i < parameters.length - 1) {
                sw.print(", ");
            }
        }
        sw.println(");");
        FragmentGeneratorContext subContext = new FragmentGeneratorContext(context);
        subContext.returnType = returnType;
        subContext.parameterName = "jsReturn";
        sw.println("return this.@" + JSWrapper.class.getName() + "::setJavaScriptObject(Lcom/google/gwt/core/client/JavaScriptObject;)(jsReturn);");
        sw.outdent();
        sw.println("}-*/;");
    }

    /**
   * Provides a method to encapsulate empty field initialization.
   */
    protected void writeEmptyFieldInitializerMethod(final TreeLogger logger, final Map<String, Task> propertyAccessors, final FragmentGeneratorContext context) throws UnableToCompleteException {
        SourceWriter sw = context.sw;
        JClassType returnType = context.returnType.isClassOrInterface();
        sw.println("private native void __initializeEmptyFields(JavaScriptObject jso) /*-{");
        sw.indent();
        FragmentGeneratorContext subContext = new FragmentGeneratorContext(context);
        subContext.parameterName = "jso";
        writeEmptyFieldInitializers(subContext);
        subContext.tasks = TaskFactory.extractMethods(logger, subContext.typeOracle, returnType, TaskFactory.EXPORTER_POLICY).values();
        writeMethodBindings(subContext);
        sw.outdent();
        sw.println("}-*/;");
    }

    /**
   * Ensures that no field referenced by generated logic will ever return an
   * undefined value. This allows every subsequent getFoo() call to simply
   * return the field value, without having to check it for an undefined value.
   */
    protected void writeEmptyFieldInitializers(FragmentGeneratorContext context) throws UnableToCompleteException {
        SourceWriter sw = context.sw;
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing field initializers", null);
        for (Task task : context.tasks) {
            final String fieldName = task.getFieldName(logger);
            if (task.getter == null) {
                continue;
            }
            final JType returnType = task.getter.getReturnType();
            FragmentGenerator fragmentGenerator = FRAGMENT_ORACLE.findFragmentGenerator(logger, context.typeOracle, returnType);
            sw.print("if (!");
            sw.print(context.parameterName);
            sw.print(".hasOwnProperty('");
            sw.print(fieldName);
            sw.println("')) {");
            sw.indent();
            sw.print(context.parameterName);
            sw.print(".");
            sw.print(fieldName);
            sw.print(" = ");
            sw.print(fragmentGenerator.defaultValue(context.typeOracle, returnType));
            sw.println(";");
            sw.outdent();
            sw.println("}");
        }
    }

    protected void writeFixups(TreeLogger logger, TypeOracle typeOracle, SourceWriter sw, Set<JClassType> creatorFixups) throws UnableToCompleteException {
        for (JClassType asClass : creatorFixups) {
            JParameterizedType pType = asClass.isParameterized();
            if (pType != null) {
                asClass = pType.getRawType();
            }
            sw.print("private static ");
            sw.print(asClass.getQualifiedSourceName());
            sw.print(" __create__");
            sw.print(asClass.getQualifiedSourceName().replaceAll("\\.", "_"));
            sw.println("() {");
            sw.indent();
            sw.print("return (");
            sw.print(asClass.getQualifiedSourceName());
            sw.print(")GWT.create(");
            sw.print(asClass.getQualifiedSourceName());
            sw.println(".class);");
            sw.outdent();
            sw.println("}");
        }
    }

    protected void writeGetter(FragmentGeneratorContext context, JMethod getter) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing getter " + getter.getName(), null);
        TypeOracle typeOracle = context.typeOracle;
        SourceWriter sw = context.sw;
        final JType returnType = getter.getReturnType();
        FragmentGenerator fragmentGenerator = FRAGMENT_ORACLE.findFragmentGenerator(logger, typeOracle, context.returnType);
        sw.print("public native ");
        sw.print(returnType.getQualifiedSourceName());
        sw.print(" ");
        sw.print(getter.getName());
        sw.print("(");
        JParameter[] params = getter.getParameters();
        for (int i = 0; i < params.length; i++) {
            sw.print(params[i].getType().getQualifiedSourceName());
            sw.print(" ");
            sw.print(params[i].getName());
            if (i < params.length - 1) {
                sw.print(", ");
            }
        }
        sw.print(")");
        sw.println(" /*-{");
        sw.indent();
        sw.print("return ");
        fragmentGenerator.fromJS(context);
        sw.println(";");
        sw.outdent();
        sw.println("}-*/;");
    }

    protected void writeImported(FragmentGeneratorContext context, JMethod imported) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing import " + imported.getName(), null);
        SourceWriter sw = context.sw;
        JParameter[] parameters = imported.getParameters();
        if (parameters == null) {
            parameters = new JParameter[0];
        }
        sw.print("public native ");
        sw.print(imported.getReturnType().getQualifiedSourceName());
        sw.print(" ");
        sw.print(imported.getName());
        sw.print("(");
        for (int i = 0; i < parameters.length; i++) {
            JType returnType = parameters[i].getType();
            JParameterizedType pType = returnType.isParameterized();
            if (pType != null) {
                sw.print(pType.getRawType().getQualifiedSourceName());
            } else {
                sw.print(returnType.getQualifiedSourceName());
            }
            sw.print(" ");
            sw.print(parameters[i].getName());
            if (i < parameters.length - 1) {
                sw.print(", ");
            }
        }
        sw.print(")");
        sw.println(" /*-{");
        sw.indent();
        final JType returnType = imported.getReturnType();
        if (!JPrimitiveType.VOID.equals(returnType.isPrimitive())) {
            sw.print("var jsReturn = ");
        }
        sw.print(context.objRef);
        sw.print(".");
        sw.print(context.fieldName);
        sw.print("(");
        for (int i = getImportOffset(); i < parameters.length; i++) {
            JType subType = parameters[i].getType();
            FragmentGeneratorContext subParams = new FragmentGeneratorContext(context);
            subParams.returnType = subType;
            subParams.parameterName = parameters[i].getName();
            FragmentGenerator fragmentGenerator = context.fragmentGeneratorOracle.findFragmentGenerator(logger, context.typeOracle, subType);
            if (fragmentGenerator == null) {
                logger.log(TreeLogger.ERROR, "No fragment generator for " + returnType.getQualifiedSourceName(), null);
                throw new UnableToCompleteException();
            }
            fragmentGenerator.toJS(subParams);
            if (i < parameters.length - 1) {
                sw.print(", ");
            }
        }
        sw.println(");");
        if (!JPrimitiveType.VOID.equals(returnType.isPrimitive())) {
            FragmentGeneratorContext subContext = new FragmentGeneratorContext(context);
            subContext.returnType = returnType;
            subContext.parameterName = "jsReturn";
            FragmentGenerator fragmentGenerator = FRAGMENT_ORACLE.findFragmentGenerator(logger, context.typeOracle, returnType);
            sw.print("return ");
            fragmentGenerator.fromJS(subContext);
            sw.println(";");
        }
        sw.outdent();
        sw.println("}-*/;");
    }

    protected void writeMethodBindings(FragmentGeneratorContext context) throws UnableToCompleteException {
        SourceWriter sw = context.sw;
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing method bindings initializers", null);
        for (Task task : context.tasks) {
            final String fieldName = task.getFieldName(logger);
            if (task.exported != null) {
                sw.print(context.parameterName);
                sw.print(".");
                sw.print(fieldName);
                sw.print(" = ");
                FragmentGeneratorContext subContext = new FragmentGeneratorContext(context);
                subContext.parameterName = "this." + BACKREF;
                JSFunctionFragmentGenerator.writeFunctionForMethod(subContext, task.exported);
                sw.println(";");
            }
        }
    }

    /**
   * Write the field, getter, and setter for the properties we know about. Also
   * write BusObjectImpl methods for Map-style access.
   */
    protected void writeMethods(FragmentGeneratorContext context, Map<String, Task> propertyAccessors) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing methods", null);
        for (Task task : propertyAccessors.values()) {
            context.fieldName = task.getFieldName(logger);
            writeSingleTask(context, task);
        }
    }

    protected void writeSetter(FragmentGeneratorContext context, JMethod setter) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing setter " + setter.getName(), null);
        TypeOracle typeOracle = context.typeOracle;
        SourceWriter sw = context.sw;
        JType parameterType = context.returnType;
        FragmentGenerator fragmentGenerator = FRAGMENT_ORACLE.findFragmentGenerator(logger, typeOracle, context.returnType);
        if (fragmentGenerator == null) {
            throw new UnableToCompleteException();
        }
        JParameterizedType pType = parameterType.isParameterized();
        if (pType != null) {
            parameterType = pType.getRawType();
        }
        sw.print("public native void ");
        sw.print(setter.getName());
        sw.print("(");
        JParameter[] params = setter.getParameters();
        for (int i = 0; i < params.length; i++) {
            sw.print(params[i].getType().getQualifiedSourceName());
            sw.print(" ");
            sw.print(params[i].getName());
            if (i < params.length - 1) {
                sw.print(", ");
            }
        }
        sw.println(") /*-{");
        sw.indent();
        sw.print(context.objRef);
        sw.print(".");
        sw.print(context.fieldName);
        sw.print(" = ");
        fragmentGenerator.toJS(context);
        sw.println(";");
        sw.outdent();
        sw.println("}-*/;");
    }

    protected void writeSingleTask(FragmentGeneratorContext context, Task task) throws UnableToCompleteException {
        TreeLogger logger = context.parentLogger.branch(TreeLogger.DEBUG, "Writing Task " + task.getFieldName(context.parentLogger), null);
        context = new FragmentGeneratorContext(context);
        context.parentLogger = logger;
        logger.log(TreeLogger.DEBUG, "Implementing task " + context.fieldName, null);
        if (task.getter != null) {
            context.returnType = task.getter.getReturnType();
            context.parameterName = context.objRef + "." + context.fieldName;
            writeGetter(context, task.getter);
        }
        if (task.imported != null) {
            context.returnType = task.imported.getReturnType();
            writeImported(context, task.imported);
        }
        if (task.setter != null) {
            if (context.readOnly) {
                logger.log(TreeLogger.ERROR, "Unable to write property setter on read-only wrapper.", null);
                throw new UnableToCompleteException();
            }
            JParameter parameter = getSetterParameter(task.setter);
            context.returnType = parameter.getType();
            context.parameterName = parameter.getName();
            writeSetter(context, task.setter);
        }
        if (task.constructor != null) {
            context.returnType = task.constructor.getReturnType();
            context.parameterName = "this.";
            context.objRef = "this";
            writeConstructor(context, task.constructor);
        }
    }
}
