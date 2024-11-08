package org.callbackparams.internal.template;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.callbackparams.AdaptiveRule;
import org.callbackparams.AdaptiveRule.TestRun;
import org.callbackparams.support.MethodHashKey;

/**
 * @author Henrik Kaipe
 */
public class AdaptiveRulesPackager implements java.lang.reflect.InvocationHandler, Constants {

    /**
     * Specifies details of a Visitor super-interface for
     * {@link org.callbackparams.internal.CallbackMethodProxyingRebyter}.
     * @see 
     */
    public static class VisitorParentGenerator {

        private final String interfaceName;

        private Type enclosingType;

        private List adaptiveRuleFields;

        private List testMethods;

        public VisitorParentGenerator(Class enclosingClass, List adaptiveRuleFields, List testMethods) {
            this.enclosingType = Type.getType(enclosingClass);
            this.adaptiveRuleFields = adaptiveRuleFields;
            this.testMethods = testMethods;
            this.interfaceName = defineInterfaceNameOfNestedVisitorParent(enclosingClass);
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public byte[] generateByteCode() {
            return defineNestedVisitorParent(interfaceName, enclosingType, adaptiveRuleFields, testMethods);
        }
    }

    /**
     * Will be rebyted when classes are reloaded for the CallbackParams testrun!
     * As super-interfaces it will get the nested (artificial)
     * VisitorParent_x interfaces of the test-class and its super-classes.
     * It will itself get a single method, which return-type will be the
     * (rebyted) test-class.
     */
    public interface Visitor {
    }

    private static final Map testMethodMap = new HashMap();

    private static final List adaptiveRuleFields = new ArrayList();

    private static final Class[] proxiedInterfaces = { Visitor.class };

    private static final Class testClass;

    private final TestrunCallbacks testInstance;

    public AdaptiveRulesPackager(TestrunCallbacks testInstance) {
        this.testInstance = testInstance;
    }

    static {
        Class tempTestClass = null;
        final Method[] visitorMethods = Visitor.class.getMethods();
        Map hashedVisitorMethods = new HashMap();
        for (int i = 0; i < visitorMethods.length; ++i) {
            final Method m = visitorMethods[i];
            final Class[] params = m.getParameterTypes();
            if (0 == params.length) {
                tempTestClass = m.getReturnType();
            } else if (params[0].isArray()) {
                try {
                    Field f = params[0].getComponentType().getDeclaredField(m.getName());
                    try {
                        f.setAccessible(true);
                    } catch (SecurityException ignoreAndHopeForTheBest) {
                    }
                    adaptiveRuleFields.add(f);
                } catch (Exception x) {
                    throw new Error(x.getMessage());
                }
            } else {
                MethodHashKey key = new MethodHashKey(params[0].getName(), m.getName(), asTestMethodSignature(m.getReturnType(), params));
                hashedVisitorMethods.put(key, m);
            }
        }
        testClass = tempTestClass;
        while (false == hashedVisitorMethods.isEmpty()) {
            final Method[] testClassMethods = tempTestClass.getDeclaredMethods();
            for (int i = 0; i < testClassMethods.length; ++i) {
                final Method m = testClassMethods[i];
                final Method visitorMethod = (Method) hashedVisitorMethods.remove(MethodHashKey.getHashKey(m));
                if (null != visitorMethod) {
                    try {
                        m.setAccessible(true);
                    } catch (SecurityException ignoreAndHopeForTheBest) {
                    }
                    testMethodMap.put(MethodHashKey.getHashKey(visitorMethod), m);
                }
            }
            tempTestClass = tempTestClass.getSuperclass();
        }
    }

    private static String asTestMethodSignature(Class returnType, Class[] visitorMethodParams) {
        final Type[] argTypes = new Type[visitorMethodParams.length - 1];
        for (int i = 0; i < argTypes.length; ++i) {
            argTypes[i] = Type.getType(visitorMethodParams[i + 1]);
        }
        return Type.getMethodSignature(Type.getType(returnType), argTypes);
    }

    private static String defineInterfaceNameOfNestedVisitorParent(Class enclosingClass) {
        String interfNamePrefix = enclosingClass.getName() + "$VisitorParent_";
        String interfName = null;
        try {
            for (int suffix = 0; true; ++suffix) {
                interfName = interfNamePrefix + suffix;
                enclosingClass.getClassLoader().loadClass(interfName);
            }
        } catch (ClassNotFoundException x) {
            return interfName;
        }
    }

    private static byte[] defineNestedVisitorParent(String interfName, Type enclosingType, List adaptiveRules, List testMethods) {
        ClassGen cg = new ClassGen(interfName, Object.class.getName(), "<generated>", ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, null);
        ConstantPoolGen cp = cg.getConstantPool();
        if (false == adaptiveRules.isEmpty()) {
            Type[] arg_types = { new ArrayType(enclosingType, 1) };
            for (Iterator i = adaptiveRules.iterator(); i.hasNext(); ) {
                final Field ruleField = (Field) i.next();
                cg.addMethod(new MethodGen(ACC_PUBLIC | ACC_ABSTRACT, Type.VOID, arg_types, null, ruleField.getName(), interfName, null, cp).getMethod());
            }
        }
        if (false == testMethods.isEmpty()) {
            VisitorMethodArgtypesFactory argsFactory = new VisitorMethodArgtypesFactory(enclosingType);
            for (Iterator i = testMethods.iterator(); i.hasNext(); ) {
                final Method m = (Method) i.next();
                MethodGen mg = new MethodGen(ACC_PUBLIC | ACC_ABSTRACT, Type.getType(m.getReturnType()), argsFactory.asVisitorMethodArgtypes(m.getParameterTypes()), null, m.getName(), interfName, null, cp);
                if (0 < m.getExceptionTypes().length) {
                    mg.addException(Throwable.class.getName());
                }
                cg.addMethod(mg.getMethod());
            }
        }
        return cg.getJavaClass().getBytes();
    }

    public static InstructionList defineTestMethodDetourCall(InstructionFactory factory, Method m) {
        final InstructionList il = new InstructionList();
        il.append(factory.ALOAD_0);
        il.append(factory.createInvoke(AdaptiveRulesPackager.class.getName(), "fetchTestrunVisitor", Type.getType(Visitor.class), new Type[] { Type.getType(TestrunCallbacks.class) }, INVOKESTATIC));
        il.append(factory.DUP);
        IfInstruction ifNull = new IFNULL(null);
        il.append(ifNull);
        Type declaringClassType = Type.getType(m.getDeclaringClass());
        Type[] argTypes = new VisitorMethodArgtypesFactory(declaringClassType).asVisitorMethodArgtypes(m.getParameterTypes());
        il.append(factory.ACONST_NULL);
        for (int i = 1; i < argTypes.length; ++i) {
            il.append(factory.createLoad(argTypes[i], i));
        }
        il.append(factory.createInvoke(Visitor.class.getName(), m.getName(), Type.getType(m.getReturnType()), argTypes, INVOKEINTERFACE));
        il.append(factory.createReturn(Type.getType(m.getReturnType())));
        ifNull.setTarget(il.append(factory.POP));
        return il;
    }

    public static Visitor fetchTestrunVisitor(TestrunCallbacks testInstance) {
        if (testInstance.currentlyWrappedWithinAdaptiveRules || false == testClass.isInstance(testInstance) || adaptiveRuleFields.isEmpty()) {
            return null;
        } else {
            return (Visitor) java.lang.reflect.Proxy.newProxyInstance(Visitor.class.getClassLoader(), proxiedInterfaces, new AdaptiveRulesPackager(testInstance));
        }
    }

    private TestRun wrapTestrunWithRule(final AdaptiveRule rule, final TestRun testRun) {
        return new TestRun() {

            public Object executeTestMethod() throws Throwable {
                return rule.evaluate(testRun);
            }
        };
    }

    private Object executeWithAdaptiveRules(TestRun testRun) throws Throwable {
        for (Iterator i = adaptiveRuleFields.iterator(); i.hasNext(); ) {
            final Field ruleField = (Field) i.next();
            AdaptiveRule rule = (AdaptiveRule) ruleField.get(testInstance);
            if (null != rule) {
                testRun = wrapTestrunWithRule(rule, testRun);
            }
        }
        return testRun.executeTestMethod();
    }

    public Object invoke(Object proxy, final Method m, final Object[] args) throws Throwable {
        testInstance.currentlyWrappedWithinAdaptiveRules = true;
        try {
            return executeWithAdaptiveRules(new AdaptiveRule.TestRun() {

                public Object executeTestMethod() throws Throwable {
                    try {
                        Method testMethod = (Method) testMethodMap.get(MethodHashKey.getHashKey(m));
                        return testMethod.invoke(testInstance, asTestMethodArgs(args));
                    } catch (InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                }
            });
        } finally {
            testInstance.currentlyWrappedWithinAdaptiveRules = false;
        }
    }

    private static Object[] asTestMethodArgs(final Object[] args) {
        if (1 == args.length) {
            return null;
        } else {
            final Object[] testMethodArgs = new Object[args.length - 1];
            for (int i = 0; i < testMethodArgs.length; ++i) {
                testMethodArgs[i] = args[i + 1];
            }
            return testMethodArgs;
        }
    }

    public static byte[] generateRebytedVisitor(Class testClass, Set superInterfaceNames) {
        String[] superInterfaces = (String[]) superInterfaceNames.toArray(new String[superInterfaceNames.size()]);
        ClassGen cg = new ClassGen(Visitor.class.getName(), Object.class.getName(), "<regenerated>", ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, superInterfaces);
        MethodGen mg = new MethodGen(ACC_PUBLIC | ACC_ABSTRACT, Type.getType(testClass), new Type[] {}, null, "testClass", Visitor.class.getName(), null, cg.getConstantPool());
        cg.addMethod(mg.getMethod());
        return cg.getJavaClass().getBytes();
    }

    private static class VisitorMethodArgtypesFactory {

        final Type enclosingType;

        VisitorMethodArgtypesFactory(Type enclosingType) {
            this.enclosingType = enclosingType;
        }

        Type[] asVisitorMethodArgtypes(final Class[] testMethodParams) {
            final Type[] argTypes = new Type[testMethodParams.length + 1];
            argTypes[0] = enclosingType;
            for (int i = 0; i < testMethodParams.length; ++i) {
                argTypes[i + 1] = Type.getType(testMethodParams[i]);
            }
            return argTypes;
        }
    }
}
