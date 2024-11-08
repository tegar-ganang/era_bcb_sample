package com.stateofflow.invariantj.build;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;
import com.stateofflow.invariantj.InvariantViolationError;
import com.stateofflow.invariantj.ThreadState;
import com.stateofflow.invariantj.Utils;

class ClassInstrumenter {

    private static final String THREAD_STATE_EXIT_METHOD_CALL = ThreadState.class.getName() + "#exit();";

    private static final String THREAD_STATE_ENTER_METHOD_CALL = ThreadState.class.getName() + "#enter(this);";

    private static final String START_OF_INVARIANT_METHOD_NAME = "isInvariant";

    private static final String CHECK_INVARIANT_METHOD_NAME = Utils.INSERTED_MEMBERS_NAME_PREFIX + "checkInvariant";

    private static final String METHOD_ENTRY_METHOD_NAME = Utils.INSERTED_MEMBERS_NAME_PREFIX + "methodEntry";

    private static final String METHOD_EXIT_METHOD_NAME = Utils.INSERTED_MEMBERS_NAME_PREFIX + "methodExit";

    private static final String SUPERCLASS_CHECK_INVARIANT_METHOD_FIELD_NAME = Utils.INSERTED_MEMBERS_NAME_PREFIX + "superclassCheckInvariantMethod";

    private static final String SUPERCLASS_CONSTRUCTOR_COMPLETE_FIELD_NAME = Utils.INSERTED_MEMBERS_NAME_PREFIX + "superclassConstructorComplete";

    private interface PrintWriterClosure {

        void execute(PrintWriter writer) throws NotFoundException;
    }

    private CtMethod checkOwnInvariantMethod;

    private CtMethod checkInvariantMethod;

    private CtClass toInstrument;

    public void instrument(CtClass instrumentee) throws NotFoundException, CannotCompileException {
        if (instrumentee.isInterface()) {
            return;
        }
        this.toInstrument = instrumentee;
        createRequiredMembers();
        decorateCheckableBehaviours();
    }

    private void createRequiredMembers() throws CannotCompileException, NotFoundException {
        createSuperclassCheckInvariantMethodSetup();
        createSuperclassConstructorCompletionField();
        checkOwnInvariantMethod = createCheckOwnInvariantMethod();
        checkInvariantMethod = createCheckInvariantMethod();
        createEnterMethod();
        createExitMethod();
    }

    private void createSuperclassConstructorCompletionField() throws CannotCompileException {
        addField(Modifier.PRIVATE | Modifier.FINAL, CtClass.booleanType, ClassInstrumenter.SUPERCLASS_CONSTRUCTOR_COMPLETE_FIELD_NAME);
    }

    private void createSuperclassCheckInvariantMethodSetup() throws CannotCompileException, NotFoundException {
        CtClass methodClass = toInstrument.getClassPool().get(Method.class.getName());
        addField(Modifier.PRIVATE | Modifier.STATIC, methodClass, ClassInstrumenter.SUPERCLASS_CHECK_INVARIANT_METHOD_FIELD_NAME);
        methodClass.detach();
        CtConstructor staticInitializer = toInstrument.makeClassInitializer();
        staticInitializer.insertAfter(ClassInstrumenter.SUPERCLASS_CHECK_INVARIANT_METHOD_FIELD_NAME + " = com.stateofflow.invariantj.Utils.getSuperclassCheckOwnInvariantMethodIfExists(" + toInstrument.getName() + ".class);");
    }

    private void createEnterMethod() throws CannotCompileException {
        addMethod("private void " + ClassInstrumenter.METHOD_ENTRY_METHOD_NAME + "() {" + ClassInstrumenter.CHECK_INVARIANT_METHOD_NAME + "(null);" + ClassInstrumenter.THREAD_STATE_ENTER_METHOD_CALL + "}");
    }

    private void createExitMethod() throws CannotCompileException {
        addMethod("private void " + ClassInstrumenter.METHOD_EXIT_METHOD_NAME + "(Throwable t) {" + ClassInstrumenter.THREAD_STATE_EXIT_METHOD_CALL + "if (!(t instanceof " + InvariantViolationError.class.getName() + ")) {" + ClassInstrumenter.CHECK_INVARIANT_METHOD_NAME + "(t);" + "}" + "}");
    }

    private CtMethod createCheckOwnInvariantMethod() throws CannotCompileException, NotFoundException {
        final CtMethod[] invariantMethods = findInvariantMethods();
        return addMethod(new PrintWriterClosure() {

            public void execute(PrintWriter writer) {
                writer.println("public void " + getCheckOwnInvariantMethodName() + "(Throwable t) {");
                writer.println("  if (!" + ClassInstrumenter.SUPERCLASS_CONSTRUCTOR_COMPLETE_FIELD_NAME + " || " + ThreadState.class.getName() + "#isInFlow(this)) {");
                writer.println("    return;");
                writer.println("  }");
                writer.println("  " + ThreadState.class.getName() + ".beginInvariantCheck();");
                String qualifiedSuperclassCheckInvariantMethodFieldName = toInstrument.getName() + "#" + ClassInstrumenter.SUPERCLASS_CHECK_INVARIANT_METHOD_FIELD_NAME;
                writer.println("  if (" + qualifiedSuperclassCheckInvariantMethodFieldName + " != null) {");
                writer.println("    " + qualifiedSuperclassCheckInvariantMethodFieldName + ".invoke(this, com.stateofflow.invariantj.Utils#NULL_OBJECT_ARRAY);");
                writer.println("  }");
                writeInvariantChecks(invariantMethods, writer);
                writer.println("  " + ThreadState.class.getName() + ".endInvariantCheck(t);");
                writer.println("}");
            }
        });
    }

    private CtMethod[] findInvariantMethods() throws NotFoundException {
        CtMethod[] allMethods = toInstrument.getDeclaredMethods();
        Set invariantMethods = new HashSet();
        for (int i = 0; i < allMethods.length; i++) {
            CtMethod method = allMethods[i];
            if (isInvariantMethod(method)) {
                invariantMethods.add(method);
            }
        }
        return (CtMethod[]) invariantMethods.toArray(new CtMethod[invariantMethods.size()]);
    }

    private String getInvariantNameFromMethodName(String invariantMethodName) {
        return invariantMethodName.substring(ClassInstrumenter.START_OF_INVARIANT_METHOD_NAME.length());
    }

    private CtMethod createCheckInvariantMethod() throws CannotCompileException {
        return addMethod("public void " + ClassInstrumenter.CHECK_INVARIANT_METHOD_NAME + "(Throwable t) {" + getCheckOwnInvariantMethodCallExpression("t") + "}");
    }

    private void decorateCheckableBehaviours() throws NotFoundException, CannotCompileException {
        decorateAllConstructorsWithSuperclassConstructorCompletionFlagUpdate();
        decorateCheckableBehavioursWithInvariantChecks();
    }

    private void decorateAllConstructorsWithSuperclassConstructorCompletionFlagUpdate() throws CannotCompileException {
        CtConstructor[] constructors = toInstrument.getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            constructors[i].insertBeforeBody(ClassInstrumenter.SUPERCLASS_CONSTRUCTOR_COMPLETE_FIELD_NAME + " = true;");
        }
    }

    private void decorateCheckableBehavioursWithInvariantChecks() throws NotFoundException, CannotCompileException {
        CtBehavior[] behaviours = toInstrument.getDeclaredBehaviors();
        for (int i = 0; i < behaviours.length; i++) {
            CtBehavior method = behaviours[i];
            if (shouldDecorate(method)) {
                decorateBehaviour(method);
            }
        }
    }

    private boolean shouldDecorate(CtBehavior behavior) throws NotFoundException {
        int modifiers = behavior.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            return false;
        }
        if (behavior instanceof CtConstructor) {
            return true;
        }
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            return false;
        }
        return !(isInvariantMethod((CtMethod) behavior) || behavior.equals(checkInvariantMethod) || behavior.equals(checkOwnInvariantMethod));
    }

    private void decorateBehaviour(CtBehavior behavior) throws CannotCompileException, NotFoundException {
        MethodInfo methodInfo = behavior.getMethodInfo();
        if (methodInfo.isConstructor()) {
            decorateConstructor((CtConstructor) behavior);
        } else if (methodInfo.isMethod()) {
            decorateMethod((CtMethod) behavior);
        }
    }

    private void decorateConstructor(CtConstructor constructor) throws CannotCompileException {
        constructor.insertBeforeBody(ClassInstrumenter.THREAD_STATE_ENTER_METHOD_CALL);
        constructor.insertAfter(ClassInstrumenter.THREAD_STATE_EXIT_METHOD_CALL);
        constructor.insertAfter(getCheckOwnInvariantMethodCallExpression("null"));
    }

    private void decorateMethod(CtMethod method) throws CannotCompileException, NotFoundException {
        final CtMethod wrapped = prepareWrappedMethod(method);
        final boolean toString = isStandardToStringMethod(method);
        String code = withPrintWriter(new PrintWriterClosure() {

            public void execute(PrintWriter writer) {
                writer.println("{");
                writer.println("  " + (toString ? ClassInstrumenter.THREAD_STATE_ENTER_METHOD_CALL : ClassInstrumenter.METHOD_ENTRY_METHOD_NAME + "();"));
                writer.println("  Throwable t = null;");
                writer.println("  try {");
                writer.println("    return $proceed($$);");
                writer.println("  } catch (Exception e) {");
                writer.println("    t = e;");
                writer.println("    throw e;");
                writer.println("  } catch (" + InvariantViolationError.class.getName() + " e) {");
                writer.println("    t = e;");
                writer.println("    throw e;");
                writer.println("  } finally {");
                writer.println("    " + (toString ? ClassInstrumenter.THREAD_STATE_EXIT_METHOD_CALL : ClassInstrumenter.METHOD_EXIT_METHOD_NAME + "(t);"));
                writer.println("  }");
                writer.println("}");
            }
        });
        method.setBody(code, "this", wrapped.getName());
    }

    private CtMethod prepareWrappedMethod(CtMethod method) throws CannotCompileException {
        CtMethod wrapped = new CtMethod(method, toInstrument, null);
        wrapped.setName("_" + wrapped.getName());
        wrapped.setModifiers(Modifier.setPrivate(wrapped.getModifiers()));
        toInstrument.addMethod(wrapped);
        return wrapped;
    }

    private CtMethod addMethod(PrintWriterClosure closure) throws CannotCompileException, NotFoundException {
        return addMethod(withPrintWriter(closure));
    }

    private CtMethod addMethod(String sourceCode) throws CannotCompileException {
        CtMethod m = CtNewMethod.make(sourceCode, toInstrument);
        toInstrument.addMethod(m);
        return m;
    }

    private void addField(int modifiers, CtClass type, String name) throws CannotCompileException {
        CtField field = new CtField(type, name, toInstrument);
        field.setModifiers(modifiers);
        toInstrument.addField(field);
    }

    private boolean isInvariantMethod(CtMethod method) throws NotFoundException {
        return !Modifier.isStatic(method.getModifiers()) && method.getMethodInfo().isMethod() && method.getName().startsWith(ClassInstrumenter.START_OF_INVARIANT_METHOD_NAME) && method.getParameterTypes().length == 0 && method.getReturnType().equals(CtClass.booleanType);
    }

    private boolean isStandardToStringMethod(CtMethod method) throws NotFoundException {
        return method.getName().equals("toString") && method.getParameterTypes().length == 0;
    }

    private String withPrintWriter(PrintWriterClosure closure) throws NotFoundException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        closure.execute(printWriter);
        printWriter.close();
        return stringWriter.getBuffer().toString();
    }

    private String getCheckOwnInvariantMethodName() {
        return Utils.getCheckOwnInvariantMethodName(toInstrument.getName());
    }

    private String getCheckOwnInvariantMethodCallExpression(String argument) {
        return getCheckOwnInvariantMethodName() + "(" + argument + ");";
    }

    private void writeInvariantChecks(final CtMethod[] invariantMethods, PrintWriter writer) {
        if (invariantMethods.length > 0) {
            for (int i = 0; i < invariantMethods.length; i++) {
                String invariantMethodName = invariantMethods[i].getName();
                writer.println("  try {");
                writer.println("    if (!" + invariantMethodName + "()) {");
                writer.println("      " + ThreadState.class.getName() + ".addFailedInvariant(\"" + getInvariantNameFromMethodName(invariantMethodName) + "\");");
                writer.println("    }");
                writer.println("  } catch (Exception ex) {");
                writer.println("    " + ThreadState.class.getName() + ".addExceptionInInvariantCheck(\"" + getInvariantNameFromMethodName(invariantMethodName) + "\", ex);");
                writer.println("  } catch (com.stateofflow.invariantj.InvariantViolationError ex) {");
                writer.println("    " + ThreadState.class.getName() + ".addExceptionInInvariantCheck(\"" + getInvariantNameFromMethodName(invariantMethodName) + "\", ex);");
                writer.println("  }");
            }
        }
    }
}
