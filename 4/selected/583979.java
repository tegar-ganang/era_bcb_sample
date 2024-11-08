package com.intellij.uiDesigner.compiler;

import com.intellij.uiDesigner.UIFormXmlConstants;
import com.intellij.uiDesigner.lw.*;
import com.intellij.uiDesigner.shared.BorderType;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author yole
 */
public class AsmCodeGenerator {

    private LwRootContainer myRootContainer;

    private ClassLoader myLoader;

    private ArrayList myErrors;

    private ArrayList myWarnings;

    private Map myIdToLocalMap = new HashMap();

    private static final String CONSTRUCTOR_NAME = "<init>";

    private String myClassToBind;

    private byte[] myPatchedData;

    private static Map myContainerLayoutCodeGenerators = new HashMap();

    private static Map myComponentLayoutCodeGenerators = new HashMap();

    private static Map myPropertyCodeGenerators = new HashMap();

    public static final String SETUP_METHOD_NAME = "$$$setupUI$$$";

    public static final String GET_ROOT_COMPONENT_METHOD_NAME = "$$$getRootComponent$$$";

    public static final String CREATE_COMPONENTS_METHOD_NAME = "createUIComponents";

    public static final String LOAD_LABEL_TEXT_METHOD = "$$$loadLabelText$$$";

    public static final String LOAD_BUTTON_TEXT_METHOD = "$$$loadButtonText$$$";

    private static final Type ourButtonGroupType = Type.getType(ButtonGroup.class);

    private static final Type ourBorderFactoryType = Type.getType(BorderFactory.class);

    private static final Type ourBorderType = Type.getType(Border.class);

    private static final Method ourCreateTitledBorderMethod = Method.getMethod("javax.swing.border.TitledBorder createTitledBorder(javax.swing.border.Border,java.lang.String,int,int,java.awt.Font,java.awt.Color)");

    private NestedFormLoader myFormLoader;

    private final boolean myIgnoreCustomCreation;

    static {
        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_INTELLIJ, new GridLayoutCodeGenerator());
        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_GRIDBAG, new GridBagLayoutCodeGenerator());
        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_BORDER, new SimpleLayoutCodeGenerator(Type.getType(BorderLayout.class)));
        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_CARD, new SimpleLayoutCodeGenerator(Type.getType(CardLayout.class)));
        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_FLOW, new FlowLayoutCodeGenerator());
        myComponentLayoutCodeGenerators.put(LwSplitPane.class, new SplitPaneLayoutCodeGenerator());
        myComponentLayoutCodeGenerators.put(LwTabbedPane.class, new TabbedPaneLayoutCodeGenerator());
        myComponentLayoutCodeGenerators.put(LwScrollPane.class, new ScrollPaneLayoutCodeGenerator());
        myComponentLayoutCodeGenerators.put(LwToolBar.class, new ToolBarLayoutCodeGenerator());
        myPropertyCodeGenerators.put(String.class.getName(), new StringPropertyCodeGenerator());
        myPropertyCodeGenerators.put(Dimension.class.getName(), new DimensionPropertyCodeGenerator());
        myPropertyCodeGenerators.put(Insets.class.getName(), new InsetsPropertyCodeGenerator());
        myPropertyCodeGenerators.put(Rectangle.class.getName(), new RectanglePropertyCodeGenerator());
        myPropertyCodeGenerators.put(Color.class.getName(), new ColorPropertyCodeGenerator());
        myPropertyCodeGenerators.put(Font.class.getName(), new FontPropertyCodeGenerator());
        myPropertyCodeGenerators.put(Icon.class.getName(), new IconPropertyCodeGenerator());
        myPropertyCodeGenerators.put(ListModel.class.getName(), new ListModelPropertyCodeGenerator(DefaultListModel.class));
        myPropertyCodeGenerators.put(ComboBoxModel.class.getName(), new ListModelPropertyCodeGenerator(DefaultComboBoxModel.class));
        myPropertyCodeGenerators.put("java.lang.Enum", new EnumPropertyCodeGenerator());
    }

    public AsmCodeGenerator(LwRootContainer rootContainer, ClassLoader loader, NestedFormLoader formLoader, final boolean ignoreCustomCreation) {
        myFormLoader = formLoader;
        myIgnoreCustomCreation = ignoreCustomCreation;
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        if (rootContainer == null) {
            throw new IllegalArgumentException("rootContainer cannot be null");
        }
        myRootContainer = rootContainer;
        myLoader = loader;
        myErrors = new ArrayList();
        myWarnings = new ArrayList();
    }

    public void patchFile(final File classFile) {
        if (!classFile.exists()) {
            myErrors.add(new FormErrorInfo(null, "Class to bind does not exist: " + myRootContainer.getClassToBind()));
            return;
        }
        FileInputStream fis;
        try {
            byte[] patchedData;
            fis = new FileInputStream(classFile);
            try {
                patchedData = patchClass(fis);
                if (patchedData == null) {
                    return;
                }
            } finally {
                fis.close();
            }
            FileOutputStream fos = new FileOutputStream(classFile);
            try {
                fos.write(patchedData);
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            myErrors.add(new FormErrorInfo(null, "Cannot read or write class file " + classFile.getPath() + ": " + e.toString()));
        }
    }

    public byte[] patchClass(InputStream classStream) {
        myClassToBind = myRootContainer.getClassToBind();
        if (myClassToBind == null) {
            myWarnings.add(new FormErrorInfo(null, "No class to bind specified"));
            return null;
        }
        if (myRootContainer.getComponentCount() != 1) {
            myErrors.add(new FormErrorInfo(null, "There should be only one component at the top level"));
            return null;
        }
        String nonEmptyPanel = Utils.findNotEmptyPanelWithXYLayout(myRootContainer.getComponent(0));
        if (nonEmptyPanel != null) {
            myErrors.add(new FormErrorInfo(nonEmptyPanel, "There are non empty panels with XY layout. Please lay them out in a grid."));
            return null;
        }
        ClassReader reader;
        try {
            reader = new ClassReader(classStream);
        } catch (IOException e) {
            myErrors.add(new FormErrorInfo(null, "Error reading class data stream"));
            return null;
        }
        FirstPassClassVisitor visitor = new FirstPassClassVisitor();
        reader.accept(visitor, true);
        ClassWriter cw = new ClassWriter(true);
        reader.accept(new FormClassVisitor(cw, visitor.isExplicitSetupCall()), false);
        myPatchedData = cw.toByteArray();
        return myPatchedData;
    }

    public FormErrorInfo[] getErrors() {
        return (FormErrorInfo[]) myErrors.toArray(new FormErrorInfo[myErrors.size()]);
    }

    public FormErrorInfo[] getWarnings() {
        return (FormErrorInfo[]) myWarnings.toArray(new FormErrorInfo[myWarnings.size()]);
    }

    public byte[] getPatchedData() {
        return myPatchedData;
    }

    static void pushPropValue(GeneratorAdapter generator, String propertyClass, Object value) {
        PropertyCodeGenerator codeGen = (PropertyCodeGenerator) myPropertyCodeGenerators.get(propertyClass);
        if (codeGen == null) {
            throw new RuntimeException("Unknown property class " + propertyClass);
        }
        codeGen.generatePushValue(generator, value);
    }

    static Class getComponentClass(String className, final ClassLoader classLoader) throws CodeGenerationException {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new CodeGenerationException(null, "Class not found: " + className);
        } catch (UnsupportedClassVersionError e) {
            throw new CodeGenerationException(null, "Unsupported class version error: " + className);
        }
    }

    public static Type typeFromClassName(final String className) {
        return Type.getType("L" + className.replace('.', '/') + ";");
    }

    class FormClassVisitor extends ClassAdapter {

        private String myClassName;

        private String mySuperName;

        private Map myFieldDescMap = new HashMap();

        private Map myFieldAccessMap = new HashMap();

        private boolean myHaveCreateComponentsMethod = false;

        private int myCreateComponentsAccess;

        private final boolean myExplicitSetupCall;

        public FormClassVisitor(final ClassVisitor cv, final boolean explicitSetupCall) {
            super(cv);
            myExplicitSetupCall = explicitSetupCall;
        }

        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            myClassName = name;
            mySuperName = superName;
            for (Iterator iterator = myPropertyCodeGenerators.values().iterator(); iterator.hasNext(); ) {
                PropertyCodeGenerator propertyCodeGenerator = (PropertyCodeGenerator) iterator.next();
                propertyCodeGenerator.generateClassStart(this, name, myLoader);
            }
        }

        public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
            if (name.equals(SETUP_METHOD_NAME) || name.equals(GET_ROOT_COMPONENT_METHOD_NAME) || name.equals(LOAD_BUTTON_TEXT_METHOD) || name.equals(LOAD_LABEL_TEXT_METHOD)) {
                return null;
            }
            if (name.equals(CREATE_COMPONENTS_METHOD_NAME) && desc.equals("()V")) {
                myHaveCreateComponentsMethod = true;
                myCreateComponentsAccess = access;
            }
            final MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (name.equals(CONSTRUCTOR_NAME) && !myExplicitSetupCall) {
                return new FormConstructorVisitor(methodVisitor, myClassName, mySuperName);
            }
            return methodVisitor;
        }

        MethodVisitor visitNewMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
            myFieldDescMap.put(name, desc);
            myFieldAccessMap.put(name, new Integer(access));
            return super.visitField(access, name, desc, signature, value);
        }

        public void visitEnd() {
            final boolean haveCustomCreateComponents = Utils.getCustomCreateComponentCount(myRootContainer) > 0 && !myIgnoreCustomCreation;
            if (haveCustomCreateComponents && !myHaveCreateComponentsMethod) {
                myErrors.add(new FormErrorInfo(null, "Form contains components with Custom Create option but no createUIComponents() method"));
            }
            Method method = Method.getMethod("void " + SETUP_METHOD_NAME + " ()");
            GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, method, null, null, cv);
            if (haveCustomCreateComponents && myHaveCreateComponentsMethod) {
                generator.visitVarInsn(Opcodes.ALOAD, 0);
                int opcode = myCreateComponentsAccess == Opcodes.ACC_PRIVATE ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
                generator.visitMethodInsn(opcode, myClassName, CREATE_COMPONENTS_METHOD_NAME, "()V");
            }
            buildSetupMethod(generator);
            final String rootBinding = myRootContainer.getComponent(0).getBinding();
            if (rootBinding != null && myFieldDescMap.containsKey(rootBinding)) {
                buildGetRootComponenMethod();
            }
            for (Iterator iterator = myPropertyCodeGenerators.values().iterator(); iterator.hasNext(); ) {
                PropertyCodeGenerator propertyCodeGenerator = (PropertyCodeGenerator) iterator.next();
                propertyCodeGenerator.generateClassEnd(this);
            }
            super.visitEnd();
        }

        private void buildGetRootComponenMethod() {
            final Type componentType = Type.getType(JComponent.class);
            final Method method = new Method(GET_ROOT_COMPONENT_METHOD_NAME, componentType, new Type[0]);
            GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, method, null, null, cv);
            final LwComponent topComponent = (LwComponent) myRootContainer.getComponent(0);
            final String binding = topComponent.getBinding();
            generator.loadThis();
            generator.getField(typeFromClassName(myClassName), binding, Type.getType((String) myFieldDescMap.get(binding)));
            generator.returnValue();
            generator.endMethod();
        }

        private void buildSetupMethod(final GeneratorAdapter generator) {
            try {
                final LwComponent topComponent = (LwComponent) myRootContainer.getComponent(0);
                generateSetupCodeForComponent(topComponent, generator, -1);
                generateComponentReferenceProperties(topComponent, generator);
                generateButtonGroups(myRootContainer, generator);
            } catch (CodeGenerationException e) {
                myErrors.add(new FormErrorInfo(e.getComponentId(), e.getMessage()));
            }
            generator.returnValue();
            generator.endMethod();
        }

        private void generateSetupCodeForComponent(final LwComponent lwComponent, final GeneratorAdapter generator, final int parentLocal) throws CodeGenerationException {
            String className;
            if (lwComponent instanceof LwNestedForm) {
                LwRootContainer nestedFormContainer;
                LwNestedForm nestedForm = (LwNestedForm) lwComponent;
                try {
                    nestedFormContainer = myFormLoader.loadForm(nestedForm.getFormFileName());
                } catch (Exception e) {
                    throw new CodeGenerationException(lwComponent.getId(), e.getMessage());
                }
                if (nestedFormContainer.getComponentCount() == 0) {
                    return;
                }
                if (nestedFormContainer.getComponent(0).getBinding() == null) {
                    throw new CodeGenerationException(lwComponent.getId(), "No binding on root component of nested form " + nestedForm.getFormFileName());
                }
                try {
                    Utils.validateNestedFormLoop(nestedForm.getFormFileName(), myFormLoader);
                } catch (RecursiveFormNestingException e) {
                    throw new CodeGenerationException(lwComponent.getId(), "Recursive form nesting is not allowed");
                }
                className = myFormLoader.getClassToBindName(nestedFormContainer);
            } else {
                className = getComponentCodeGenerator(lwComponent.getParent()).mapComponentClass(lwComponent.getComponentClassName());
            }
            Type componentType = typeFromClassName(className);
            int componentLocal = generator.newLocal(componentType);
            myIdToLocalMap.put(lwComponent.getId(), new Integer(componentLocal));
            Class componentClass = getComponentClass(className, myLoader);
            validateFieldBinding(lwComponent, componentClass);
            if (myIgnoreCustomCreation) {
                boolean creatable = true;
                if ((componentClass.getModifiers() & Modifier.PRIVATE) != 0) {
                    creatable = false;
                } else {
                    try {
                        final Constructor constructor = componentClass.getConstructor(new Class[0]);
                        if ((constructor.getModifiers() & Modifier.PUBLIC) == 0) {
                            creatable = false;
                        }
                    } catch (NoSuchMethodException ex) {
                        creatable = false;
                    }
                }
                if (!creatable) {
                    componentClass = Utils.suggestReplacementClass(componentClass);
                    componentType = Type.getType(componentClass);
                }
            }
            if (!lwComponent.isCustomCreate() || myIgnoreCustomCreation) {
                generator.newInstance(componentType);
                generator.dup();
                generator.invokeConstructor(componentType, Method.getMethod("void <init>()"));
                generator.storeLocal(componentLocal);
                generateFieldBinding(lwComponent, generator, componentLocal);
            } else {
                final String binding = lwComponent.getBinding();
                if (binding == null) {
                    throw new CodeGenerationException(lwComponent.getId(), "Only components bound to fields can have custom creation code");
                }
                generator.loadThis();
                generator.getField(getMainClassType(), binding, Type.getType((String) myFieldDescMap.get(binding)));
                generator.storeLocal(componentLocal);
            }
            if (lwComponent instanceof LwContainer) {
                LwContainer lwContainer = (LwContainer) lwComponent;
                getComponentCodeGenerator(lwContainer).generateContainerLayout(lwContainer, generator, componentLocal);
            }
            generateComponentProperties(lwComponent, componentClass, generator, componentLocal);
            if (!(lwComponent.getParent() instanceof LwRootContainer)) {
                final LayoutCodeGenerator parentCodeGenerator = getComponentCodeGenerator(lwComponent.getParent());
                if (lwComponent instanceof LwNestedForm) {
                    componentLocal = getNestedFormComponent(generator, componentClass, componentLocal);
                }
                parentCodeGenerator.generateComponentLayout(lwComponent, generator, componentLocal, parentLocal);
            }
            if (lwComponent instanceof LwContainer) {
                LwContainer container = (LwContainer) lwComponent;
                generateBorder(container, generator, componentLocal);
                for (int i = 0; i < container.getComponentCount(); i++) {
                    generateSetupCodeForComponent((LwComponent) container.getComponent(i), generator, componentLocal);
                }
            }
        }

        private int getNestedFormComponent(GeneratorAdapter generator, Class componentClass, int formLocal) throws CodeGenerationException {
            final Type componentType = Type.getType(JComponent.class);
            int componentLocal = generator.newLocal(componentType);
            generator.loadLocal(formLocal);
            generator.invokeVirtual(Type.getType(componentClass), new Method(GET_ROOT_COMPONENT_METHOD_NAME, componentType, new Type[0]));
            generator.storeLocal(componentLocal);
            return componentLocal;
        }

        private LayoutCodeGenerator getComponentCodeGenerator(final LwContainer container) {
            LayoutCodeGenerator generator = (LayoutCodeGenerator) myComponentLayoutCodeGenerators.get(container.getClass());
            if (generator != null) {
                return generator;
            }
            LwContainer parent = container;
            while (parent != null) {
                final String layoutManager = parent.getLayoutManager();
                if (layoutManager != null && layoutManager.length() > 0) {
                    if (layoutManager.equals(UIFormXmlConstants.LAYOUT_FORM) && !myContainerLayoutCodeGenerators.containsKey(UIFormXmlConstants.LAYOUT_FORM)) {
                        myContainerLayoutCodeGenerators.put(UIFormXmlConstants.LAYOUT_FORM, new FormLayoutCodeGenerator());
                    }
                    generator = (LayoutCodeGenerator) myContainerLayoutCodeGenerators.get(layoutManager);
                    if (generator != null) {
                        return generator;
                    }
                }
                parent = parent.getParent();
            }
            return GridLayoutCodeGenerator.INSTANCE;
        }

        private void generateComponentProperties(final LwComponent lwComponent, final Class componentClass, final GeneratorAdapter generator, final int componentLocal) throws CodeGenerationException {
            final LwIntrospectedProperty[] introspectedProperties = lwComponent.getAssignedIntrospectedProperties();
            for (int i = 0; i < introspectedProperties.length; i++) {
                final LwIntrospectedProperty property = introspectedProperties[i];
                if (property instanceof LwIntroComponentProperty) {
                    continue;
                }
                final String propertyClass = property.getCodeGenPropertyClassName();
                if (myIgnoreCustomCreation) {
                    try {
                        Class setterClass;
                        if (propertyClass.equals(Integer.class.getName())) {
                            setterClass = int.class;
                        } else if (propertyClass.equals(Boolean.class.getName())) {
                            setterClass = boolean.class;
                        } else if (propertyClass.equals(Double.class.getName())) {
                            setterClass = double.class;
                        } else if (propertyClass.equals(Float.class.getName())) {
                            setterClass = float.class;
                        } else {
                            setterClass = Class.forName(propertyClass);
                        }
                        componentClass.getMethod(property.getWriteMethodName(), new Class[] { setterClass });
                    } catch (Exception e) {
                        continue;
                    }
                }
                final PropertyCodeGenerator propGen = (PropertyCodeGenerator) myPropertyCodeGenerators.get(propertyClass);
                if (propGen != null && propGen.generateCustomSetValue(lwComponent, componentClass, property, generator, componentLocal)) {
                    continue;
                }
                generator.loadLocal(componentLocal);
                Object value = lwComponent.getPropertyValue(property);
                Type setterArgType;
                if (propertyClass.equals(Integer.class.getName())) {
                    generator.push(((Integer) value).intValue());
                    setterArgType = Type.INT_TYPE;
                } else if (propertyClass.equals(Boolean.class.getName())) {
                    generator.push(((Boolean) value).booleanValue());
                    setterArgType = Type.BOOLEAN_TYPE;
                } else if (propertyClass.equals(Double.class.getName())) {
                    generator.push(((Double) value).doubleValue());
                    setterArgType = Type.DOUBLE_TYPE;
                } else if (propertyClass.equals(Float.class.getName())) {
                    generator.push(((Float) value).floatValue());
                    setterArgType = Type.FLOAT_TYPE;
                } else {
                    if (propGen == null) {
                        continue;
                    }
                    propGen.generatePushValue(generator, value);
                    setterArgType = typeFromClassName(property.getPropertyClassName());
                }
                Type declaringType = (property.getDeclaringClassName() != null) ? typeFromClassName(property.getDeclaringClassName()) : Type.getType(componentClass);
                generator.invokeVirtual(declaringType, new Method(property.getWriteMethodName(), Type.VOID_TYPE, new Type[] { setterArgType }));
            }
            generateClientProperties(lwComponent, componentClass, generator, componentLocal);
        }

        private void generateClientProperties(final LwComponent lwComponent, final Class componentClass, final GeneratorAdapter generator, final int componentLocal) throws CodeGenerationException {
            HashMap props = lwComponent.getDelegeeClientProperties();
            for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry e = (Map.Entry) iterator.next();
                generator.loadLocal(componentLocal);
                generator.push((String) e.getKey());
                Object value = e.getValue();
                if (value instanceof StringDescriptor) {
                    generator.push(((StringDescriptor) value).getValue());
                } else {
                    Type valueType = Type.getType(value.getClass());
                    generator.newInstance(valueType);
                    generator.dup();
                    if (value instanceof Boolean) {
                        generator.push(((Boolean) value).booleanValue());
                        generator.invokeConstructor(valueType, Method.getMethod("void <init>(boolean)"));
                    } else if (value instanceof Integer) {
                        generator.push(((Integer) value).intValue());
                        generator.invokeConstructor(valueType, Method.getMethod("void <init>(int)"));
                    } else if (value instanceof Double) {
                        generator.push(((Double) value).doubleValue());
                        generator.invokeConstructor(valueType, Method.getMethod("void <init>(double)"));
                    } else {
                        throw new CodeGenerationException(lwComponent.getId(), "Unknown client property value type");
                    }
                }
                Type componentType = Type.getType(componentClass);
                Type objectType = Type.getType(Object.class);
                generator.invokeVirtual(componentType, new Method("putClientProperty", Type.VOID_TYPE, new Type[] { objectType, objectType }));
            }
        }

        private void generateComponentReferenceProperties(final LwComponent component, final GeneratorAdapter generator) throws CodeGenerationException {
            if (component instanceof LwNestedForm) return;
            int componentLocal = ((Integer) myIdToLocalMap.get(component.getId())).intValue();
            final LayoutCodeGenerator layoutCodeGenerator = getComponentCodeGenerator(component.getParent());
            Class componentClass = getComponentClass(layoutCodeGenerator.mapComponentClass(component.getComponentClassName()), myLoader);
            final LwIntrospectedProperty[] introspectedProperties = component.getAssignedIntrospectedProperties();
            for (int i = 0; i < introspectedProperties.length; i++) {
                final LwIntrospectedProperty property = introspectedProperties[i];
                if (property instanceof LwIntroComponentProperty) {
                    String targetId = (String) component.getPropertyValue(property);
                    if (targetId != null && targetId.length() > 0) {
                        final Integer targetLocalInt = (Integer) myIdToLocalMap.get(targetId);
                        if (targetLocalInt != null) {
                            int targetLocal = targetLocalInt.intValue();
                            generator.loadLocal(componentLocal);
                            generator.loadLocal(targetLocal);
                            Type declaringType = (property.getDeclaringClassName() != null) ? typeFromClassName(property.getDeclaringClassName()) : Type.getType(componentClass);
                            generator.invokeVirtual(declaringType, new Method(property.getWriteMethodName(), Type.VOID_TYPE, new Type[] { typeFromClassName(property.getPropertyClassName()) }));
                        }
                    }
                }
            }
            if (component instanceof LwContainer) {
                LwContainer container = (LwContainer) component;
                for (int i = 0; i < container.getComponentCount(); i++) {
                    generateComponentReferenceProperties((LwComponent) container.getComponent(i), generator);
                }
            }
        }

        private void generateButtonGroups(final LwRootContainer rootContainer, final GeneratorAdapter generator) throws CodeGenerationException {
            IButtonGroup[] groups = rootContainer.getButtonGroups();
            if (groups.length > 0) {
                int groupLocal = generator.newLocal(ourButtonGroupType);
                for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
                    String[] ids = groups[groupIndex].getComponentIds();
                    if (ids.length > 0) {
                        generator.newInstance(ourButtonGroupType);
                        generator.dup();
                        generator.invokeConstructor(ourButtonGroupType, Method.getMethod("void <init>()"));
                        generator.storeLocal(groupLocal);
                        if (groups[groupIndex].isBound() && !myIgnoreCustomCreation) {
                            validateFieldClass(groups[groupIndex].getName(), ButtonGroup.class, null);
                            generator.loadThis();
                            generator.loadLocal(groupLocal);
                            generator.putField(getMainClassType(), groups[groupIndex].getName(), ourButtonGroupType);
                        }
                        for (int i = 0; i < ids.length; i++) {
                            Integer localInt = (Integer) myIdToLocalMap.get(ids[i]);
                            if (localInt != null) {
                                generator.loadLocal(groupLocal);
                                generator.loadLocal(localInt.intValue());
                                generator.invokeVirtual(ourButtonGroupType, Method.getMethod("void add(javax.swing.AbstractButton)"));
                            }
                        }
                    }
                }
            }
        }

        private void generateFieldBinding(final LwComponent lwComponent, final GeneratorAdapter generator, final int componentLocal) throws CodeGenerationException {
            final String binding = lwComponent.getBinding();
            if (binding != null) {
                Integer access = (Integer) myFieldAccessMap.get(binding);
                if ((access.intValue() & Opcodes.ACC_STATIC) != 0) {
                    throw new CodeGenerationException(lwComponent.getId(), "Cannot bind: field is static: " + myClassToBind + "." + binding);
                }
                if ((access.intValue() & Opcodes.ACC_FINAL) != 0) {
                    throw new CodeGenerationException(lwComponent.getId(), "Cannot bind: field is final: " + myClassToBind + "." + binding);
                }
                generator.loadThis();
                generator.loadLocal(componentLocal);
                generator.putField(getMainClassType(), binding, Type.getType((String) myFieldDescMap.get(binding)));
            }
        }

        private Type getMainClassType() {
            return Type.getType("L" + myClassName + ";");
        }

        private void validateFieldBinding(LwComponent component, final Class componentClass) throws CodeGenerationException {
            String binding = component.getBinding();
            if (binding == null) return;
            validateFieldClass(binding, componentClass, component.getId());
        }

        private void validateFieldClass(String binding, Class componentClass, String componentId) throws CodeGenerationException {
            if (!myFieldDescMap.containsKey(binding)) {
                throw new CodeGenerationException(componentId, "Cannot bind: field does not exist: " + myClassToBind + "." + binding);
            }
            final Type fieldType = Type.getType((String) myFieldDescMap.get(binding));
            if (fieldType.getSort() != Type.OBJECT) {
                throw new CodeGenerationException(componentId, "Cannot bind: field is of primitive type: " + myClassToBind + "." + binding);
            }
            Class fieldClass;
            try {
                fieldClass = myLoader.loadClass(fieldType.getClassName());
            } catch (ClassNotFoundException e) {
                throw new CodeGenerationException(componentId, "Class not found: " + fieldType.getClassName());
            }
            if (!fieldClass.isAssignableFrom(componentClass)) {
                throw new CodeGenerationException(componentId, "Cannot bind: Incompatible types. Cannot assign " + componentClass.getName() + " to field " + myClassToBind + "." + binding);
            }
        }

        private void generateBorder(final LwContainer container, final GeneratorAdapter generator, final int componentLocal) {
            final BorderType borderType = container.getBorderType();
            final StringDescriptor borderTitle = container.getBorderTitle();
            final String borderFactoryMethodName = borderType.getBorderFactoryMethodName();
            final boolean borderNone = borderType.equals(BorderType.NONE);
            if (!borderNone || borderTitle != null) {
                generator.loadLocal(componentLocal);
                if (!borderNone) {
                    if (borderType.equals(BorderType.LINE)) {
                        if (container.getBorderColor() == null) {
                            Type colorType = Type.getType(Color.class);
                            generator.getStatic(colorType, "black", colorType);
                        } else {
                            pushPropValue(generator, Color.class.getName(), container.getBorderColor());
                        }
                        generator.invokeStatic(ourBorderFactoryType, new Method(borderFactoryMethodName, ourBorderType, new Type[] { Type.getType(Color.class) }));
                    } else if (borderType.equals(BorderType.EMPTY) && container.getBorderSize() != null) {
                        Insets size = container.getBorderSize();
                        generator.push(size.top);
                        generator.push(size.left);
                        generator.push(size.bottom);
                        generator.push(size.right);
                        generator.invokeStatic(ourBorderFactoryType, new Method(borderFactoryMethodName, ourBorderType, new Type[] { Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE }));
                    } else {
                        generator.invokeStatic(ourBorderFactoryType, new Method(borderFactoryMethodName, ourBorderType, new Type[0]));
                    }
                } else {
                    generator.push((String) null);
                }
                pushBorderProperties(container, generator, borderTitle, componentLocal);
                generator.invokeStatic(ourBorderFactoryType, ourCreateTitledBorderMethod);
                generator.invokeVirtual(Type.getType(JComponent.class), Method.getMethod("void setBorder(javax.swing.border.Border)"));
            }
        }

        private void pushBorderProperties(final LwContainer container, final GeneratorAdapter generator, final StringDescriptor borderTitle, final int componentLocal) {
            pushPropValue(generator, "java.lang.String", borderTitle);
            generator.push(container.getBorderTitleJustification());
            generator.push(container.getBorderTitlePosition());
            final FontDescriptor font = container.getBorderTitleFont();
            if (font == null) {
                generator.push((String) null);
            } else {
                FontPropertyCodeGenerator.generatePushFont(generator, componentLocal, container, font, "getFont");
            }
            if (container.getBorderTitleColor() == null) {
                generator.push((String) null);
            } else {
                pushPropValue(generator, Color.class.getName(), container.getBorderTitleColor());
            }
        }
    }

    private static class FormConstructorVisitor extends MethodAdapter {

        private final String myClassName;

        private final String mySuperName;

        private boolean callsSelfConstructor = false;

        public FormConstructorVisitor(final MethodVisitor mv, final String className, final String superName) {
            super(mv);
            myClassName = className;
            mySuperName = superName;
        }

        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
            super.visitMethodInsn(opcode, owner, name, desc);
            if (opcode == Opcodes.INVOKESPECIAL && name.equals(CONSTRUCTOR_NAME)) {
                if (owner.equals(myClassName)) {
                    callsSelfConstructor = true;
                    return;
                }
                if (owner.equals(mySuperName) && !callsSelfConstructor) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, myClassName, SETUP_METHOD_NAME, "()V");
                }
            }
        }
    }

    private static class FirstPassClassVisitor extends ClassAdapter {

        private boolean myExplicitSetupCall = false;

        public FirstPassClassVisitor() {
            super(new EmptyVisitor());
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals(CONSTRUCTOR_NAME)) {
                final FirstPassConstructorVisitor visitor = new FirstPassConstructorVisitor();
                return visitor;
            }
            return null;
        }

        public boolean isExplicitSetupCall() {
            return myExplicitSetupCall;
        }

        private class FirstPassConstructorVisitor extends MethodAdapter {

            public FirstPassConstructorVisitor() {
                super(new EmptyVisitor());
            }

            public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
                if (name.equals(SETUP_METHOD_NAME)) {
                    myExplicitSetupCall = true;
                }
            }
        }
    }
}
