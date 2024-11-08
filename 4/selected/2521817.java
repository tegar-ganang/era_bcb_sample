package net.sourceforge.mobileporting.gump.gumplet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.mobileporting.gump.GumpletException;
import net.sourceforge.mobileporting.gump.Resource;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import net.sourceforge.mobileporting.gump.gumplet.BaseCodeGumplet;

/**
 * Helper class for writing code modifying gumplets.
 * <p>
 * Contains various helper methods for injecting classes, or modifying
 * existing classes.  Many of these methods operate on sets of classes.
 * There are various methods to help construct sets, by taking horizontal
 * or vertical slices of the class hierarchy.
 * @author Graham Hughes
 */
public abstract class CodeModifier extends BaseCodeGumplet {

    private ClassPool pool;

    private Set<CtClass> sourceClasses;

    private Set<CtClass> modifiedClasses;

    private Set<Resource> resourceFiles;

    protected abstract void process() throws NotFoundException, CannotCompileException;

    @Override
    public void modify(Set<CtClass> source, Set<CtClass> modified, Set<Resource> resources) throws GumpletException {
        sourceClasses = source;
        modifiedClasses = modified;
        resourceFiles = resources;
        log("Applying " + getClass().getSimpleName());
        if (!sourceClasses.isEmpty()) {
            pool = ((CtClass) (sourceClasses.toArray()[0])).getClassPool();
            try {
                process();
            } catch (Exception e) {
                throw new GumpletException(e);
            }
        } else {
            info("no source classes");
        }
    }

    /**
	 * Get a CtClass for a given class.
	 */
    protected CtClass getCtClass(Class<?> clazz) throws NotFoundException {
        return pool.get(clazz.getName());
    }

    /**
	 * Get a method from the class with the given signature.
	 * @throws NotFoundException if no method matches
	 */
    protected CtMethod getMethod(CtClass cc, String signature) throws NotFoundException {
        CtMethod method = null;
        for (CtBehavior m : getMethods(cc)) {
            if (m instanceof CtMethod && signature.equals(getSignature(m))) {
                method = (CtMethod) m;
            }
        }
        if (method == null) {
            throw new NotFoundException("can't find " + signature);
        }
        return method;
    }

    private void checkForEmptySet(Set<CtClass> classes, String s) {
        if (classes.isEmpty()) {
            info(s + ": no classes");
        }
    }

    /**
	 * Replace calls to specific methods with code fragments. The "replacements"
	 * map maps signatures to fragments.
	 * @throws CannotCompileException 
	 */
    protected void replaceCalls(Set<CtClass> classes, Map<String, String> replacements) throws CannotCompileException {
        checkForEmptySet(classes, "replaceCalls()");
        Editor editor = new Editor(replacements);
        runEditor(classes, editor);
    }

    protected void replaceCalls(Set<CtClass> classes, String signature, String fragment) throws CannotCompileException {
        replaceCalls(classes, makeMap(new String[][] { { signature, fragment } }));
    }

    /**
	 * Replace accesses to the specified field with given "reader" and/or "writer"
	 * fragments.  Either fragment can be null, in which case, no replacement occurs.
	 * @throws CannotCompileException
	 */
    protected void replaceFieldAccess(Set<CtClass> classes, String fieldName, String reader, String writer) throws CannotCompileException {
        checkForEmptySet(classes, "replaceFieldAccess()");
        Editor editor = new Editor(fieldName, reader, writer);
        runEditor(classes, editor);
    }

    private void runEditor(Set<CtClass> classes, Editor editor) throws CannotCompileException {
        for (CtClass cc : classes) {
            cc.instrument(editor);
            if (editor.isModified()) {
                modified(cc);
                editor.reset();
            }
        }
    }

    /**
	 * Add a method to all the specified classes.
	 * @throws CannotCompileException 
	 */
    protected void injectMethod(Set<CtClass> classes, CtMethod method) throws CannotCompileException {
        checkForEmptySet(classes, "injectMethod()");
        for (CtClass cc : classes) {
            CtMethod newMethod = new CtMethod(method, cc, null);
            cc.addMethod(newMethod);
            modified(cc);
        }
    }

    protected void injectMethod(Set<CtClass> classes, String src) throws CannotCompileException {
        checkForEmptySet(classes, "injectMethod()");
        for (CtClass cc : classes) {
            CtMethod newMethod = CtNewMethod.make(src, cc);
            cc.addMethod(newMethod);
            modified(cc);
        }
    }

    /**
	 * Inject a static method into the build, to replace an API instance method.
	 * Instance reference will become first parameter to static method.
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void injectStaticReplacementMethod(String oldSig, String newName) throws NotFoundException, CannotCompileException {
        CtClass container = ensureClassInWorkingSet(net.sourceforge.mobileporting.portlib.MethodContainer.class);
        String newSig = constructStaticReplacementMethodSignature(oldSig, newName);
        CtClass myClass = getCtClass(this.getClass());
        CtMethod method = new CtMethod(getMethod(myClass, newSig), container, null);
        container.addMethod(method);
        info("injected " + getSignature(method));
        modified(container);
        patchCallToStaticMethod(oldSig, container, method);
    }

    /**
	 * Inject a static method into the build, to replace an API instance method.
	 * Instance reference will become first parameter to static method.
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void injectStaticReplacementMethod(String oldSig, String newName, Class<?> container) throws NotFoundException, CannotCompileException {
        CtClass ctContainer = ensureClassInWorkingSet(container);
        String newSig = constructStaticReplacementMethodSignature(oldSig, newName);
        CtMethod method = ctContainer.getMethod(newName, newSig);
        patchCallToStaticMethod(oldSig, ctContainer, method);
    }

    private void patchCallToStaticMethod(String oldSig, CtClass ctContainer, CtMethod method) throws NotFoundException, CannotCompileException {
        String name = ctContainer.getName() + "." + method.getName();
        Set<CtClass> classes = allClassesExcept(ctContainer.getName());
        if (method.getReturnType() == CtClass.voidType) {
            replaceCalls(classes, oldSig, "{ " + name + "($0, $$); }");
        } else {
            replaceCalls(classes, oldSig, "{ $_ = " + name + "($0, $$); }");
        }
    }

    private String constructStaticReplacementMethodSignature(String oldSig, String newName) {
        String newSig;
        int endOfName = oldSig.indexOf('(');
        String sig = oldSig.substring(endOfName + 1);
        int endOfClassName = oldSig.lastIndexOf('.', endOfName);
        String className = oldSig.substring(0, endOfClassName);
        className = "L" + className.replace('.', '/') + ";";
        newSig = newName + "(" + className + sig;
        return newSig;
    }

    private CtClass ensureClassInWorkingSet(Class<?> c) throws NotFoundException {
        Set<CtClass> set = singleClass(c);
        if (set.isEmpty()) {
            injectClass(c);
            set = singleClass(c);
        }
        return set.iterator().next();
    }

    /**
	 * Add all fields specified by the list, to all specified classes.
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void injectFields(Set<CtClass> classes, Set<CtField> fields) throws CannotCompileException, NotFoundException {
        checkForEmptySet(classes, "injectFields()");
        for (CtClass cc : classes) {
            for (CtField f : fields) {
                CtField newField = new CtField(f.getType(), f.getName(), cc);
                newField.setModifiers(f.getModifiers());
                cc.addField(newField);
                modified(cc);
            }
        }
    }

    /**
	 * Copy fields from "this" class to the target classes.
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void injectFields(Set<CtClass> classes, String[] names) throws NotFoundException, CannotCompileException {
        CtClass myClass = getCtClass(this.getClass());
        Set<CtField> fields = new HashSet<CtField>();
        for (String name : names) {
            fields.add(myClass.getField(name));
        }
        injectFields(classes, fields);
    }

    /**
	 * Copy methods from "this" class to the target classes. 
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void injectMethods(Set<CtClass> classes, String[] signatures) throws NotFoundException, CannotCompileException {
        CtClass myClass = getCtClass(this.getClass());
        for (String signature : signatures) {
            CtMethod method = getMethod(myClass, signature);
            injectMethod(classes, method);
        }
    }

    /**
	 * Add specified class to working set.
	 * @throws NotFoundException
	 */
    protected void injectClass(Class<?> clazz) throws NotFoundException {
        injectClass(getCtClass(clazz));
    }

    protected void injectClass(CtClass cc) {
        info("injecting class " + cc.getName());
        sourceClasses.add(cc);
        modifiedClasses.add(cc);
    }

    /**
	 * Add the newClass to the working set, and insert it into the
	 * hierarchy beneath the specified class.
	 * <p>
	 * That is: all classes that extend "beneath" will now extend
	 * "newClass" instead.
	 * @throws NotFoundException if newClass cannot be found
	 * @throws CannotCompileException is newClass cannot be added as superclass
	 */
    protected void insertClass(Class<?> newClass, Class<?> beneath) throws IllegalArgumentException, NotFoundException, CannotCompileException {
        injectClass(newClass);
        CtClass newSuper = getCtClass(newClass);
        for (CtClass cc : topClasses(beneath)) {
            cc.setSuperclass(newSuper);
            modified(cc);
        }
    }

    /**
	 * Add the interface to all the specified classes.
	 * @throws NotFoundException 
	 */
    protected void addInterface(Set<CtClass> classes, Class<?> interf) throws NotFoundException {
        checkForEmptySet(classes, "addInterface()");
        CtClass ccInterface = getCtClass(interf);
        for (CtClass cc : classes) {
            cc.addInterface(ccInterface);
            modified(cc);
        }
    }

    /**
	 * Change all references to "oldClass" to "newClass" in the specified classes.
	 * @throws NotFoundException 
	 */
    protected void replaceClass(Set<CtClass> classes, Class<?> oldClass, Class<?> newClass) throws NotFoundException {
        checkForEmptySet(classes, "replaceClass()");
        injectClass(newClass);
        for (CtClass cc : classes) {
            cc.replaceClassName(oldClass.getName(), newClass.getName());
            modified(cc);
        }
    }

    /**
	 * Inject the specified code fragment at the start of the specified method,
	 * in all the specified classes.
	 * <p>
	 * When injecting into constructors, injection occurs <i>after</i> the call
	 * to the super/this constructor.
	 * @throws CannotCompileException
	 */
    protected void injectAtStart(Set<CtClass> classes, String signature, String fragment) throws CannotCompileException {
        checkForEmptySet(classes, "injectAtStart()");
        for (CtClass cc : classes) {
            for (CtBehavior cb : getMethods(cc)) {
                if (signature.equals(getSignature(cb))) {
                    if (cb instanceof CtConstructor) {
                        ((CtConstructor) cb).insertBeforeBody(fragment);
                    } else {
                        cb.insertBefore(fragment);
                    }
                    modified(cc);
                }
            }
        }
    }

    /**
	 * Inject the specified code fragment at the end of the specified method,
	 * in all the specified classes.
	 * @throws CannotCompileException
	 */
    protected void injectAtEnd(Set<CtClass> classes, String signature, String fragment) throws CannotCompileException {
        checkForEmptySet(classes, "injectAtEnd()");
        for (CtClass cc : classes) {
            for (CtBehavior cb : getMethods(cc)) {
                if (signature.equals(getSignature(cb))) {
                    cb.insertAfter(fragment);
                    modified(cc);
                }
            }
        }
    }

    /**
	 * Rename a method or field in all specified classes.
	 * <p>
	 * To rename a method, specify the signature, for example "hideNotify()V".
	 */
    protected void rename(Set<CtClass> classes, String oldName, String newName) {
        checkForEmptySet(classes, "rename(" + oldName + ")");
        if (oldName.indexOf('(') >= 0) {
            for (CtClass cc : classes) {
                CtMethod[] methods = cc.getDeclaredMethods();
                for (CtMethod method : methods) {
                    if (oldName.equals(getSignature(method))) {
                        method.setName(newName);
                        modified(cc);
                    }
                }
            }
        } else {
            for (CtClass cc : classes) {
                try {
                    CtField field = cc.getDeclaredField(oldName);
                    field.setName(newName);
                    modified(cc);
                } catch (NotFoundException e) {
                }
            }
        }
    }

    /**
	 * Wrap every method with the specified signature in the specified classes
	 * with a "try..catch".
	 * <p>
	 * "catchClause" is a block of Java source code, and must end with a return or
	 * throw statement.  The caught exception can be referenced as "$e". 
	 * @throws NotFoundException 
	 * @throws CannotCompileException 
	 */
    protected void wrapWithCatch(Set<CtClass> classes, String methodSignature, Class<?> exception, String catchClause) throws NotFoundException, CannotCompileException {
        CtClass exceptionType = getCtClass(exception);
        for (CtClass cc : classes) {
            CtMethod[] methods = cc.getDeclaredMethods();
            for (CtMethod method : methods) {
                if (methodSignature.equals(getSignature(method))) {
                    method.addCatch(catchClause, exceptionType);
                    modified(cc);
                }
            }
        }
    }

    /**
	 * Intercept and ignore all exceptions (and errors) in the specified methods.
	 * <p>
	 * For example, you can prevent exceptions thrown in keyPressed() from
	 * killing the application.
	 * <pre>
	 * killExceptions(compatibleClasses(Canvas.class), "keyPressed(I)V");
	 * </pre>
	 * @throws CannotCompileException 
	 * @throws NotFoundException 
	 */
    protected void killExceptions(Set<CtClass> classes, String methodSignature) throws NotFoundException, CannotCompileException {
        wrapWithCatch(classes, methodSignature, Throwable.class, "{ return; }");
    }

    /**
	 * Apply subclass-specific processing to every constructor and method
	 * in all of the specified classes.  Subclass must override processMethod().
	 */
    protected void processMethods(Set<CtClass> classes) throws NotFoundException, CannotCompileException {
        checkForEmptySet(classes, "processMethods()");
        for (CtClass cc : classes) {
            for (CtBehavior cb : getMethods(cc)) {
                processMethod(cc, cb);
            }
        }
    }

    /**
	 * Subclasses using processMethods() must override this to provide
	 * processing.  Subclasses should call modified() for each class they
	 * change, to ensure that modified classes are written to file later.
	 */
    protected void processMethod(@SuppressWarnings("unused") CtClass cc, @SuppressWarnings("unused") CtBehavior cb) throws NotFoundException, CannotCompileException {
        throw new IllegalStateException("processMethod() not overridden");
    }

    /**
	 */
    protected void modified(CtClass cc) {
        if (!modifiedClasses.contains(cc)) {
            info("modified: " + cc.getName());
            modifiedClasses.add(cc);
        }
    }

    /**
	 * Get entire working set of classes.
	 */
    protected Set<CtClass> allClasses() {
        Set<CtClass> set = new HashSet<CtClass>();
        set.addAll(sourceClasses);
        return set;
    }

    /**
	 * Get all classes in working set, except the one specified.
	 */
    protected Set<CtClass> allClassesExcept(String name) {
        Set<CtClass> set = allClasses();
        set.removeAll(singleClass(name));
        return set;
    }

    protected Set<CtClass> allClassesExcept(Class<?> clazz) {
        return allClassesExcept(clazz.getName());
    }

    /**
	 * Get a single class from the working set.
	 */
    protected Set<CtClass> singleClass(String name) {
        Set<CtClass> set = new HashSet<CtClass>();
        CtClass cc = findClass(name);
        if (cc != null) {
            set.add(cc);
        }
        return set;
    }

    protected Set<CtClass> singleClass(Class<?> clazz) {
        return singleClass(clazz.getName());
    }

    /**
	 * Get direct subclasses of the specified class.
	 * @throws NotFoundException 
	 */
    protected Set<CtClass> topClasses(Class<?> superClass) throws NotFoundException {
        Set<CtClass> set = new HashSet<CtClass>();
        for (CtClass cc : sourceClasses) {
            if (superClass.equals(cc.getSuperclass())) {
                set.add(cc);
            }
        }
        return set;
    }

    /**
	 * Get all (direct or indirect) subclasses of the specified class
	 * that themselves have no subclasses.
	 * If "superClass" is an interface, classes implementing that
	 * interface will be returned.
	 * If "superClass" is in the workingSet (and has no subclasses), it
	 * will be returned.
	 * @throws NotFoundException 
	 */
    protected Set<CtClass> bottomClasses(Class<?> superClass) throws NotFoundException {
        Set<CtClass> set = new HashSet<CtClass>();
        Set<CtClass> eliminated = new HashSet<CtClass>();
        CtClass ccSuper = getCtClass(superClass);
        set.addAll(sourceClasses);
        for (CtClass cc : set) {
            if (!cc.subtypeOf(ccSuper)) {
                eliminated.add(cc);
            }
            CtClass ccThisSuper = cc.getSuperclass();
            if (set.contains(ccThisSuper)) {
                eliminated.add(ccThisSuper);
            }
        }
        set.removeAll(eliminated);
        return set;
    }

    /**
	 * Get all classes from the working-set that are compatible with the
	 * specified class (or interface).
	 * <p>
	 * If the specified class is in the working-set, then it will be included.
	 * @throws NotFoundException 
	 */
    protected Set<CtClass> compatibleClasses(Class<?> clazz) throws NotFoundException {
        CtClass superCtClass = getCtClass(clazz);
        Set<CtClass> set = new HashSet<CtClass>();
        for (CtClass cc : sourceClasses) {
            if (cc.subtypeOf(superCtClass)) {
                set.add(cc);
            }
        }
        return set;
    }

    /**
	 * Helper method for making maps from an array, for replaceCalls().
	 */
    protected Map<String, String> makeMap(String[][] array) {
        Map<String, String> map = new HashMap<String, String>(array.length);
        for (String[] item : array) {
            if (item.length != 2) {
                throw new IllegalArgumentException("elements must be in pairs");
            }
            map.put(item[0], item[1]);
        }
        return map;
    }

    protected CtClass findClass(String name) {
        return findClass(sourceClasses, name);
    }

    protected Resource findResource(String name) {
        return findResource(resourceFiles, name);
    }

    /**
	 * This class is used by replaceCalls() to edit the method calls, and by
	 * replaceFieldAccess() in the same way.
	 */
    private static class Editor extends ExprEditor {

        private Map<String, String> replacements;

        private boolean modified = false;

        private String fieldName;

        private String fieldReader;

        private String fieldWriter;

        Editor(Map<String, String> newReplacements) {
            replacements = newReplacements;
            modified = false;
        }

        Editor(String field, String reader, String writer) {
            fieldName = field;
            fieldReader = reader;
            fieldWriter = writer;
            modified = false;
        }

        @Override
        public void edit(MethodCall mc) throws CannotCompileException {
            if (replacements != null) {
                try {
                    String name = mc.getClassName() + "." + mc.getMethodName() + mc.getMethod().getSignature();
                    String newCode = replacements.get(name);
                    if (newCode != null) {
                        mc.replace(newCode);
                        modified = true;
                    }
                } catch (NotFoundException e) {
                    throw new CannotCompileException(e);
                }
            }
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
            if (fieldName != null) {
                String fieldFound = f.getClassName() + "." + f.getFieldName();
                if (fieldFound.equals(fieldName)) {
                    if (f.isReader()) {
                        if (fieldReader != null) {
                            f.replace(fieldReader);
                        }
                    } else {
                        if (fieldWriter != null) {
                            f.replace(fieldWriter);
                        }
                    }
                    modified = true;
                }
            }
        }

        public boolean isModified() {
            return modified;
        }

        public void reset() {
            modified = false;
        }
    }
}
