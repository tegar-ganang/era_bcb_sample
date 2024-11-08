package edu.rice.cs.cunit.instrumentors.threadCheck;

import edu.rice.cs.cunit.classFile.ClassFile;
import edu.rice.cs.cunit.classFile.ClassFileTools;
import edu.rice.cs.cunit.classFile.MethodInfo;
import edu.rice.cs.cunit.classFile.attributes.AAnnotationsAttributeInfo;
import edu.rice.cs.cunit.classFile.attributes.AAttributeInfo;
import edu.rice.cs.cunit.classFile.attributes.CodeAttributeInfo;
import edu.rice.cs.cunit.classFile.code.InstructionList;
import edu.rice.cs.cunit.classFile.code.Opcode;
import edu.rice.cs.cunit.classFile.code.instructions.*;
import edu.rice.cs.cunit.classFile.constantPool.*;
import edu.rice.cs.cunit.classFile.constantPool.visitors.*;
import edu.rice.cs.cunit.instrumentors.InstrumentorException;
import edu.rice.cs.cunit.threadCheck.Combine;
import edu.rice.cs.cunit.threadCheck.OnlyRunBy;
import edu.rice.cs.cunit.threadCheck.ThreadCheck;
import edu.rice.cs.cunit.threadCheck.ThreadCheckException;
import edu.rice.cs.cunit.threadCheck.predicates.CombinePredicateTemplate;
import edu.rice.cs.cunit.util.Debug;
import edu.rice.cs.cunit.util.ILambda;
import edu.rice.cs.cunit.util.SoftHashMap;
import edu.rice.cs.cunit.util.Types;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Instrumentor to add calls to ThreadCheck.checkCurrentThreadName/Id/Group to check if the current thread is not
 * allowed to execute a class or method.
 * <p/>
 * This instrumentor checks for every method if there are @NotRunBy or @OnlyRunBy annotations attached to the method,
 * the containing class, the same method in one of the superclasses or interfaces, or a superclass or interface, and
 * then at the beginning of the method inserts calls to ThreadCheck..
 *
 * @author Mathias Ricken
 */
public class AddPredicateThreadCheckStrategy extends AAddThreadCheckStrategy {

    /**
     * Hash table from a fully-qualified class name of the @Combine-type annotation to the generated
     * predicate annotation record. Only the PredicateAnnotationRecord.valueList has not been filled in.
     */
    protected SoftHashMap<String, PredicateAnnotationRecord> _generatedPredicateRecords;

    /**
     * The predicate class file which is cloned for the auto-generated predicates.
     */
    protected ClassFile _templatePredicateClassFile;

    /**
     * Prefix for the parameter that determines the output directory of the generated class files.
     */
    public static final String PRED_OUT_DIR_PARAM_PREFIX = "pred-out-dir=";

    /**
     * The output directory for the generated class files, corresponding to the default package.
     */
    protected File _classOutputDir = null;

    /**
     * The directory where the generated class files go, corresponding to the package in _predicatePackage,
     * or null if none set (that is ok unless we have to auto-generate class files).
     */
    protected File _predicatePackageDir = null;

    /**
     * Prefix for the parameter that determines the package of the generated class files.
     */
    public static final String PRED_OUT_PACKAGE_PARAM_PREFIX = "pred-out-package=";

    /**
     * The package for the generated class files, or null if none set (that is ok unless we have to auto-generate
     * class files).
     */
    protected String _predicatePackage = null;

    /**
     * Constructor for this strategy.
     * @param shared data shared among all AThreadCheckStrategy instances
     * @param sharedAdd data for all AAddThreadCheckStrategy instances
     */
    public AddPredicateThreadCheckStrategy(SharedData shared, SharedAddData sharedAdd) {
        this(new ArrayList<String>(), shared, sharedAdd);
    }

    /**
     * Constructor for this strategy.
     * @param parameters parameters for the instrumentors
     * @param shared data shared among all AThreadCheckStrategy instances
     * @param sharedAdd data for all AAddThreadCheckStrategy instances
     */
    public AddPredicateThreadCheckStrategy(List<String> parameters, SharedData shared, SharedAddData sharedAdd) {
        super(parameters, shared, sharedAdd);
        for (String p : parameters) {
            if (p.toLowerCase().startsWith(PRED_OUT_DIR_PARAM_PREFIX)) {
                _classOutputDir = new File(p.substring(PRED_OUT_DIR_PARAM_PREFIX.length()));
            } else if (p.toLowerCase().startsWith(PRED_OUT_PACKAGE_PARAM_PREFIX)) {
                _predicatePackage = p.substring(PRED_OUT_PACKAGE_PARAM_PREFIX.length());
            }
        }
        ClassFileTools.ClassLocation tcl = ClassFileTools.findClassFile(CombinePredicateTemplate.class.getName(), _sharedData.getClassPath());
        if (tcl == null) {
            throw new ClassNotFoundWarning(CombinePredicateTemplate.class.getName(), _sharedData.getCurrentClassName());
        }
        boolean fromJAR = (tcl.getJarFile() != null);
        try {
            tcl.close();
        } catch (IOException ioe) {
        }
        _templatePredicateClassFile = tcl.getClassFile();
        String cptName = CombinePredicateTemplate.class.getName();
        if (_predicatePackage == null) {
            _predicatePackage = cptName.substring(0, cptName.lastIndexOf('.'));
        }
        if (_classOutputDir == null) {
            if (!fromJAR) {
                File dir = tcl.getFile().getParentFile();
                int last = cptName.length();
                while ((last = cptName.lastIndexOf('.', last)) >= 0) {
                    dir = dir.getParentFile();
                    --last;
                }
                _classOutputDir = dir;
            } else {
                for (String pathStr : _sharedData.getClassPath()) {
                    File path = new File(pathStr);
                    if (path.isDirectory()) {
                        _classOutputDir = path;
                        break;
                    }
                }
            }
        }
        if (_classOutputDir != null) {
            if (_predicatePackage.length() > 0) {
                _predicatePackageDir = new File(_classOutputDir, _predicatePackage.replace('.', '/'));
            } else {
                _predicatePackageDir = _classOutputDir;
            }
            Debug.out.println("Output dir : " + _classOutputDir);
            Debug.out.println("Package    : " + _predicatePackage);
            Debug.out.println("Package dir: " + _predicatePackageDir);
            _classOutputDir.mkdirs();
            _predicatePackageDir.mkdirs();
        } else {
            Debug.out.println("No directory for auto-generated classes set.");
        }
        _generatedPredicateRecords = new SoftHashMap<String, PredicateAnnotationRecord>();
    }

    /**
     * Instrument the class.
     *
     * @param cf class file info
     */
    public void instrument(final ClassFile cf) {
        _sharedData.setCurrentClassName(cf.getThisClassName());
        for (MethodInfo mi : cf.getMethods()) {
            if ((mi.getAccessFlags() & (ClassFile.ACC_NATIVE | ClassFile.ACC_ABSTRACT)) == 0) {
                long beginMillis = System.currentTimeMillis();
                ThreadCheckAnnotationRecord methodAR = getMethodAnnotations(cf, mi);
                long endMillis = System.currentTimeMillis();
                _sharedAddData.cacheInfo.addTimeSpent(endMillis - beginMillis);
                if (!methodAR.empty()) {
                    boolean changed = false;
                    InstructionList il = new InstructionList(mi.getCodeAttributeInfo().getCode());
                    if (mi.getName().toString().equals("<init>")) {
                        boolean ctorCalled = false;
                        do {
                            if (il.getOpcode() == Opcode.INVOKESPECIAL) {
                                ReferenceInstruction ri = (ReferenceInstruction) il.getInstr();
                                short method = Types.shortFromBytes(ri.getBytecode(), 1);
                                MethodPoolInfo mpi = cf.getConstantPoolItem(method).execute(CheckMethodVisitor.singleton(), null);
                                if (mpi.getNameAndType().getName().toString().equals("<init>")) {
                                    ClassFile curcf = cf;
                                    while ((curcf.getThisClassName() != null) && (!curcf.getThisClassName().equals(""))) {
                                        if (curcf.getThisClass().toString().equals(mpi.getClassInfo().getName().toString())) {
                                            ctorCalled = true;
                                            break;
                                        }
                                        ClassFileTools.ClassLocation cl = null;
                                        try {
                                            cl = ClassFileTools.findClassFile(curcf.getSuperClassName(), _sharedData.getClassPath());
                                            if (cl != null) {
                                                curcf = cl.getClassFile();
                                            } else {
                                                _sharedData.addClassNotFoundWarning(new ClassNotFoundWarning(curcf.getSuperClassName(), _sharedData.getCurrentClassName()));
                                                break;
                                            }
                                        } finally {
                                            try {
                                                if (cl != null) cl.close();
                                            } catch (IOException e) {
                                            }
                                        }
                                    }
                                    if (ctorCalled) {
                                        break;
                                    }
                                }
                            }
                        } while (il.advanceIndex());
                        if (ctorCalled) {
                            boolean res = il.advanceIndex();
                            assert res == true;
                        } else {
                            il.setIndex(0);
                            _sharedAddData.otherWarnings.add(new OnlyAfterRealizedWarning("ignored, no this() or super() call found in constructor " + cf.getThisClassName() + "." + mi.getName() + mi.getDescriptor()));
                            methodAR.allowEventThread = OnlyRunBy.EVENT_THREAD.NO;
                        }
                    }
                    int maxStack = 0;
                    if (methodAR.predicateAnnotations.size() > 0) {
                        for (PredicateAnnotationRecord par : methodAR.predicateAnnotations) {
                            int stackUsage = 0;
                            try {
                                if ((par.predicateClass != null) && (par.predicateMI != null)) {
                                    changed = true;
                                    stackUsage = insertPredicateCall(cf, mi, il, par);
                                } else {
                                    if (_predicatePackageDir == null) {
                                        throw new ThreadCheckException("No directory found on class path for auto-generated classes; " + "to auto-generate class files, there has to be a directory on the classpath " + "where these class files can be stored");
                                    }
                                    changed = true;
                                    stackUsage = insertPredicateCall(cf, mi, il, getGeneratedPredicate(cf, par, mi.getDescriptor().toString()));
                                }
                            } catch (ClassNotFoundWarning cnfw) {
                                _sharedData.addClassNotFoundWarning(cnfw);
                            } finally {
                                if (stackUsage > maxStack) {
                                    maxStack = stackUsage;
                                }
                            }
                        }
                    }
                    if (changed) {
                        mi.getCodeAttributeInfo().setCode(il.getCode());
                        CodeAttributeInfo.CodeProperties cProps = mi.getCodeAttributeInfo().getProperties();
                        cProps.maxStack = Math.max(Math.max(5, maxStack), cProps.maxStack);
                        mi.getCodeAttributeInfo().setProperties(cProps.maxStack, cProps.maxLocals);
                    }
                }
            }
        }
    }

    /**
     * Insert a call to the predicate sp
     * @param cf current class file
     * @param mi current method information
     * @param il instruction list
     * @param par predicate annotation record describing the check to add
     * @return stack usage
     */
    protected int insertPredicateCall(ClassFile cf, MethodInfo mi, InstructionList il, PredicateAnnotationRecord par) {
        ReferenceInstruction loadStringInstr = new ReferenceInstruction(Opcode.LDC_W, (short) 0);
        ReferenceInstruction checkCallInstr = new ReferenceInstruction(Opcode.INVOKESTATIC, (short) 0);
        ReferenceInstruction exceptionCallInstr = new ReferenceInstruction(Opcode.INVOKESTATIC, (short) 0);
        ReferenceInstruction predicateCallInstr = new ReferenceInstruction(Opcode.INVOKESTATIC, (short) 0);
        int stackUsage = 0;
        ConstantPool cp = cf.getConstantPool();
        int checkPredicateCallIndex;
        if (!par.passArguments) {
            checkPredicateCallIndex = cf.addMethodToConstantPool(ThreadCheck.class.getName().replace('.', '/'), "checkCurrentThread_Predicate", "(ZLjava/lang/String;)V");
        } else {
            checkPredicateCallIndex = cf.addMethodToConstantPool(ThreadCheck.class.getName().replace('.', '/'), "checkCurrentThread_Predicate", "([Ljava/lang/Object;ZLjava/lang/String;)V");
        }
        checkCallInstr.setReference(checkPredicateCallIndex);
        int predicateCallIndex = cf.addMethodToConstantPool(par.predicateClass.replace('.', '/'), par.predicateMI.getName().toString(), par.predicateMI.getDescriptor().toString());
        predicateCallInstr.setReference(predicateCallIndex);
        int exceptionPredicateCallIndex;
        if (!par.passArguments) {
            exceptionPredicateCallIndex = cf.addMethodToConstantPool(ThreadCheck.class.getName().replace('.', '/'), "checkCurrentThread_PredicateException", "(Ljava/lang/Throwable;Ljava/lang/String;)V");
        } else {
            exceptionPredicateCallIndex = cf.addMethodToConstantPool(ThreadCheck.class.getName().replace('.', '/'), "checkCurrentThread_PredicateException", "(Ljava/lang/Throwable;[Ljava/lang/Object;Ljava/lang/String;)V");
        }
        exceptionCallInstr.setReference(exceptionPredicateCallIndex);
        boolean res;
        final int startIndex = il.getIndex();
        if (par.passArguments) {
            loadArguments(cf, mi, il);
            il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
            res = il.advanceIndex();
            assert res == true;
        }
        if ((mi.getAccessFlags() & ClassFile.ACC_STATIC) == 0) {
            il.insertInstr(new GenericInstruction(Opcode.ALOAD_0), mi.getCodeAttributeInfo());
        } else {
            il.insertInstr(new GenericInstruction(Opcode.ACONST_NULL), mi.getCodeAttributeInfo());
        }
        ++stackUsage;
        res = il.advanceIndex();
        assert res == true;
        if (par.passArguments) {
            il.insertInstr(new GenericInstruction(Opcode.SWAP), mi.getCodeAttributeInfo());
            res = il.advanceIndex();
            assert res == true;
        }
        for (int i = 0; i < par.valueList.size(); ++i) {
            AAnnotationsAttributeInfo.Annotation.AMemberValue mv = par.valueList.get(i);
            String paramType = par.paramTypes.get(par.paramNames.get(i));
            stackUsage += loadAndTransferMemberValue(mv, paramType, cf, mi, il);
        }
        il.insertInstr(predicateCallInstr, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        int[] l;
        AUTFPoolInfo predicateAnnotName = new ASCIIPoolInfo(par.annotation.getType(), cp);
        l = cf.addConstantPoolItems(new APoolInfo[] { predicateAnnotName });
        predicateAnnotName = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
        StringPoolInfo predicateAnnotStr = new StringPoolInfo(predicateAnnotName, cp);
        l = cf.addConstantPoolItems(new APoolInfo[] { predicateAnnotStr });
        predicateAnnotStr = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<StringPoolInfo, Object>() {

            public StringPoolInfo defaultCase(APoolInfo host, Object o) {
                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be StringPoolInfo");
            }

            public StringPoolInfo stringCase(StringPoolInfo host, Object o) {
                return host;
            }
        }, null);
        loadStringInstr.setReference(l[0]);
        il.insertInstr(loadStringInstr, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        ++stackUsage;
        il.insertInstr(checkCallInstr, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        final int endIndex = il.getIndex();
        if (par.passArguments) {
            loadArguments(cf, mi, il);
        }
        loadStringInstr.setReference(l[0]);
        il.insertInstr(loadStringInstr, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        il.insertInstr(exceptionCallInstr, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        final int skipIndex = il.getIndex();
        il.setIndex(endIndex);
        WideBranchInstruction gotoInstr = new WideBranchInstruction(Opcode.GOTO_W, skipIndex);
        il.insertInstr(gotoInstr, mi.getCodeAttributeInfo());
        il.setIndex(skipIndex);
        res = il.advanceIndex();
        assert res == true;
        CodeAttributeInfo.ExceptionTableEntry[] excTable = new CodeAttributeInfo.ExceptionTableEntry[mi.getCodeAttributeInfo().getExceptionTableEntries().length + 1];
        System.arraycopy(mi.getCodeAttributeInfo().getExceptionTableEntries(), 0, excTable, 0, mi.getCodeAttributeInfo().getExceptionTableEntries().length);
        excTable[excTable.length - 1] = new CodeAttributeInfo.ExceptionTableEntry((short) il.getPCFromIndex(startIndex), (short) il.getPCFromIndex(endIndex), (short) il.getPCFromIndex(endIndex + 1), (short) 0);
        mi.getCodeAttributeInfo().setExceptionTableEntries(excTable);
        return (par.passArguments) ? stackUsage + 9 : stackUsage;
    }

    private void loadArguments(final ClassFile cf, final MethodInfo mi, final InstructionList il) {
        boolean res;
        AUTFPoolInfo objectClassName = new ASCIIPoolInfo("java/lang/Object", cf.getConstantPool());
        int[] l = cf.addConstantPoolItems(new APoolInfo[] { objectClassName });
        objectClassName = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
        ClassPoolInfo objectClass = new ClassPoolInfo(objectClassName, cf.getConstantPool());
        l = cf.addConstantPoolItems(new APoolInfo[] { objectClass });
        objectClass = cf.getConstantPoolItem(l[0]).execute(CheckClassVisitor.singleton(), null);
        ReferenceInstruction anewarray = new ReferenceInstruction(Opcode.ANEWARRAY, cf.getConstantPool().indexOf(objectClass));
        String sig = mi.getDescriptor().toString();
        List<String> methodArgTypes = ClassFileTools.getSignatures(sig.substring(1, sig.lastIndexOf(')')));
        IntegerPoolInfo count = new IntegerPoolInfo(methodArgTypes.size(), cf.getConstantPool());
        l = cf.addConstantPoolItems(new APoolInfo[] { count });
        count = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

            public IntegerPoolInfo defaultCase(APoolInfo host, Object param) {
                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
            }

            public IntegerPoolInfo intCase(IntegerPoolInfo host, Object param) {
                return host;
            }
        }, null);
        il.insertInstr(new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(count)), mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        il.insertInstr(anewarray, mi.getCodeAttributeInfo());
        res = il.advanceIndex();
        assert res == true;
        int lvIndex = (ClassFile.ACC_STATIC == (mi.getAccessFlags() & ClassFile.ACC_STATIC)) ? 0 : 1;
        int nextLVIndex;
        int arrayIndex = 0;
        for (String type : methodArgTypes) {
            IntegerPoolInfo pi;
            ReferenceInstruction ldc_w;
            switch(type.charAt(0)) {
                case '[':
                case 'L':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    il.insertInstr(Opcode.getShortestLoadStoreInstruction(Opcode.ALOAD, (short) lvIndex), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    break;
                case 'B':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Byte", "(B)V", Opcode.getShortestLoadStoreInstruction(Opcode.ILOAD, (short) lvIndex));
                    break;
                case 'C':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Character", "(C)V", Opcode.getShortestLoadStoreInstruction(Opcode.ILOAD, (short) lvIndex));
                    break;
                case 'I':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Integer", "(I)V", Opcode.getShortestLoadStoreInstruction(Opcode.ILOAD, (short) lvIndex));
                    break;
                case 'S':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Short", "(S)V", Opcode.getShortestLoadStoreInstruction(Opcode.ILOAD, (short) lvIndex));
                    break;
                case 'Z':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Boolean", "(Z)V", Opcode.getShortestLoadStoreInstruction(Opcode.ILOAD, (short) lvIndex));
                    break;
                case 'F':
                    nextLVIndex = lvIndex + 1;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Float", "(F)V", Opcode.getShortestLoadStoreInstruction(Opcode.FLOAD, (short) lvIndex));
                    break;
                case 'J':
                    nextLVIndex = lvIndex + 2;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Long", "(L)V", Opcode.getShortestLoadStoreInstruction(Opcode.LLOAD, (short) lvIndex));
                    break;
                case 'D':
                    nextLVIndex = lvIndex + 2;
                    il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    pi = new IntegerPoolInfo(arrayIndex, cf.getConstantPool());
                    l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                    pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                        public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                            throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                        }

                        public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                            return host;
                        }
                    }, null);
                    ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(pi));
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    insertCtorCall(cf, mi, il, "java/lang/Double", "(D)V", Opcode.getShortestLoadStoreInstruction(Opcode.DLOAD, (short) lvIndex));
                    break;
                case 'V':
                    throw new ThreadCheckException("One of the method parameters had type void (processing " + cf.getThisClassName() + ", " + mi.getName().toString() + mi.getDescriptor().toString() + ")");
                default:
                    throw new ThreadCheckException("One of the method parameters had an unknown type '" + type.charAt(0) + "' (processing " + cf.getThisClassName() + ", " + mi.getName().toString() + mi.getDescriptor().toString() + ")");
            }
            il.insertInstr(new GenericInstruction(Opcode.AASTORE), mi.getCodeAttributeInfo());
            res = il.advanceIndex();
            assert res == true;
            ++arrayIndex;
            lvIndex = nextLVIndex;
        }
    }

    /**
     * Transfer the annotation member value to the class file.
     * @param value annotation pair value
     * @param paramType parameter type, or null if that information is not available
     * @param cf target class file
     * @param mi method info
     * @param il instruction list, or null if this is a member value to be loaded
     * @return stack space required (2 for long and double, 1 otherwise)
     */
    private int loadAndTransferMemberValue(AAnnotationsAttributeInfo.Annotation.AMemberValue value, final String paramType, final ClassFile cf, final MethodInfo mi, final InstructionList il) {
        return value.execute(new AAnnotationsAttributeInfo.Annotation.IMemberValueVisitor<Integer, Object>() {

            public Integer constantMemberCase(AAnnotationsAttributeInfo.Annotation.ConstantMemberValue host, Object o) {
                return host.getConstValue().execute(new ADefaultPoolInfoVisitor<Integer, Object>() {

                    public Integer defaultCase(APoolInfo host, Object param) {
                        throw new BadPredicateAnnotationWarning("Unexpected annotation member constant value: " + host.getClass().getName() + " (processing " + _sharedData.getCurrentClassName() + ")");
                    }

                    private Integer primitive1Case(APoolInfo host) {
                        if (il != null) {
                            ReferenceInstruction ldc_w = new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(host));
                            il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                            boolean res = il.advanceIndex();
                            assert res == true;
                            return 1;
                        } else {
                            return 0;
                        }
                    }

                    private Integer primitive2Case(APoolInfo host) {
                        if (il != null) {
                            ReferenceInstruction ldc2_w = new ReferenceInstruction(Opcode.LDC2_W, cf.getConstantPool().indexOf(host));
                            il.insertInstr(ldc2_w, mi.getCodeAttributeInfo());
                            boolean res = il.advanceIndex();
                            assert res == true;
                            return 2;
                        } else {
                            return 0;
                        }
                    }

                    private Integer utfCase(AUTFPoolInfo host) {
                        StringPoolInfo spi = new StringPoolInfo(host, cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { spi });
                        spi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<StringPoolInfo, Object>() {

                            public StringPoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be StringPoolInfo");
                            }

                            public StringPoolInfo stringCase(StringPoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return primitive1Case(spi);
                    }

                    public Integer intCase(IntegerPoolInfo host, Object param) {
                        IntegerPoolInfo pi = new IntegerPoolInfo(host.getIntValue(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                            public IntegerPoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                            }

                            public IntegerPoolInfo intCase(IntegerPoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return primitive1Case(pi);
                    }

                    public Integer floatCase(FloatPoolInfo host, Object param) {
                        FloatPoolInfo pi = new FloatPoolInfo(host.getFloatValue(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<FloatPoolInfo, Object>() {

                            public FloatPoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be FloatPoolInfo");
                            }

                            public FloatPoolInfo floatCase(FloatPoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return primitive1Case(pi);
                    }

                    public Integer longCase(LongPoolInfo host, Object param) {
                        LongPoolInfo pi = new LongPoolInfo(host.getLongValue(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<LongPoolInfo, Object>() {

                            public LongPoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be LongPoolInfo");
                            }

                            public LongPoolInfo longCase(LongPoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return primitive2Case(pi);
                    }

                    public Integer doubleCase(DoublePoolInfo host, Object param) {
                        DoublePoolInfo pi = new DoublePoolInfo(host.getDoubleValue(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<DoublePoolInfo, Object>() {

                            public DoublePoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be DoublePoolInfo");
                            }

                            public DoublePoolInfo doubleCase(DoublePoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return primitive2Case(pi);
                    }

                    public Integer asciizCase(ASCIIPoolInfo host, Object param) {
                        ASCIIPoolInfo pi = new ASCIIPoolInfo(host.toString(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<ASCIIPoolInfo, Object>() {

                            public ASCIIPoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be ASCIIPoolInfo");
                            }

                            public ASCIIPoolInfo asciizCase(ASCIIPoolInfo host, Object param) {
                                return host;
                            }
                        }, null);
                        return utfCase(pi);
                    }

                    public Integer unicodeCase(UnicodePoolInfo host, Object param) {
                        UnicodePoolInfo pi = new UnicodePoolInfo(host.toString(), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { pi });
                        pi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<UnicodePoolInfo, Object>() {

                            public UnicodePoolInfo defaultCase(APoolInfo host, Object o) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be UnicodePoolInfo");
                            }

                            public UnicodePoolInfo unicodeCase(UnicodePoolInfo host, Object o) {
                                return host;
                            }
                        }, null);
                        return utfCase(pi);
                    }
                }, null);
            }

            public Integer enumMemberCase(AAnnotationsAttributeInfo.Annotation.EnumMemberValue host, Object o) {
                String enumTypeName = host.getTypeName().toString();
                AUTFPoolInfo typeNamePoolItem = new ASCIIPoolInfo(enumTypeName, cf.getConstantPool());
                int[] l = cf.addConstantPoolItems(new APoolInfo[] { typeNamePoolItem });
                typeNamePoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
                AUTFPoolInfo simpleTypeNamePoolItem = new ASCIIPoolInfo(enumTypeName.substring(1, enumTypeName.length() - 1), cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { simpleTypeNamePoolItem });
                simpleTypeNamePoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
                AUTFPoolInfo valueNamePoolItem = new ASCIIPoolInfo(host.getConstValue().toString(), cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { valueNamePoolItem });
                valueNamePoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
                ClassPoolInfo cpi = new ClassPoolInfo(simpleTypeNamePoolItem, cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { cpi });
                cpi = cf.getConstantPoolItem(l[0]).execute(CheckClassVisitor.singleton(), null);
                NameAndTypePoolInfo natpi = new NameAndTypePoolInfo(valueNamePoolItem, typeNamePoolItem, cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { natpi });
                natpi = cf.getConstantPoolItem(l[0]).execute(CheckNameAndTypeVisitor.singleton(), null);
                FieldPoolInfo fpi = new FieldPoolInfo(cpi, natpi, cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { fpi });
                fpi = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<FieldPoolInfo, Object>() {

                    public FieldPoolInfo defaultCase(APoolInfo host, Object param) {
                        throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be FieldPoolInfo");
                    }

                    public FieldPoolInfo fieldCase(FieldPoolInfo host, Object param) {
                        return host;
                    }
                }, null);
                if (il != null) {
                    ReferenceInstruction getstatic = new ReferenceInstruction(Opcode.GETSTATIC, l[0]);
                    il.insertInstr(getstatic, mi.getCodeAttributeInfo());
                    boolean res = il.advanceIndex();
                    assert res == true;
                    return 1;
                } else {
                    return 0;
                }
            }

            public Integer classMemberCase(AAnnotationsAttributeInfo.Annotation.ClassMemberValue host, Object o) {
                String classTypeName = host.getClassName().toString();
                AUTFPoolInfo typeNamePoolItem = new ASCIIPoolInfo(classTypeName.substring(1, classTypeName.length() - 1), cf.getConstantPool());
                int[] l = cf.addConstantPoolItems(new APoolInfo[] { typeNamePoolItem });
                typeNamePoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
                ClassPoolInfo cpi = new ClassPoolInfo(typeNamePoolItem, cf.getConstantPool());
                l = cf.addConstantPoolItems(new APoolInfo[] { cpi });
                cpi = cf.getConstantPoolItem(l[0]).execute(CheckClassVisitor.singleton(), null);
                if (il != null) {
                    ReferenceInstruction ldc_w = new ReferenceInstruction(Opcode.LDC_W, l[0]);
                    il.insertInstr(ldc_w, mi.getCodeAttributeInfo());
                    boolean res = il.advanceIndex();
                    assert res == true;
                    return 1;
                } else {
                    return 0;
                }
            }

            public Integer annotationMemberCase(AAnnotationsAttributeInfo.Annotation.AnnotationMemberValue host, Object o) {
                transferAnnotation(host.getAnnotation(), cf, mi);
                return 0;
            }

            public Integer arrayMemberCase(AAnnotationsAttributeInfo.Annotation.ArrayMemberValue host, Object o) {
                String elementType = null;
                if (paramType != null) {
                    elementType = paramType.substring(1);
                }
                if (il != null) {
                    if (elementType == null) {
                        _sharedData.addBadPredicateAnnotWarning(new BadPredicateAnnotationWarning("Element type unknown for array for predicate in method " + mi.getName().toString() + " in " + cf.getThisClassName() + " (processing " + _sharedData.getCurrentClassName() + ")"));
                        il.insertInstr(new GenericInstruction(Opcode.ACONST_NULL), mi.getCodeAttributeInfo());
                        boolean res = il.advanceIndex();
                        assert res == true;
                        return 1;
                    }
                    if (elementType.startsWith("L") && elementType.endsWith(";")) {
                        AUTFPoolInfo eltClassName = new ASCIIPoolInfo(elementType.substring(1, elementType.length() - 1), cf.getConstantPool());
                        int[] l = cf.addConstantPoolItems(new APoolInfo[] { eltClassName });
                        eltClassName = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
                        ClassPoolInfo eltClass = new ClassPoolInfo(eltClassName, cf.getConstantPool());
                        l = cf.addConstantPoolItems(new APoolInfo[] { eltClass });
                        eltClass = cf.getConstantPoolItem(l[0]).execute(CheckClassVisitor.singleton(), null);
                        ReferenceInstruction anewarray = new ReferenceInstruction(Opcode.ANEWARRAY, cf.getConstantPool().indexOf(eltClass));
                        IntegerPoolInfo count = new IntegerPoolInfo(host.getEntries().length, cf.getConstantPool());
                        l = cf.addConstantPoolItems(new APoolInfo[] { count });
                        count = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                            public IntegerPoolInfo defaultCase(APoolInfo host, Object param) {
                                throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                            }

                            public IntegerPoolInfo intCase(IntegerPoolInfo host, Object param) {
                                return host;
                            }
                        }, null);
                        il.insertInstr(new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(count)), mi.getCodeAttributeInfo());
                        boolean res = il.advanceIndex();
                        assert res == true;
                        il.insertInstr(anewarray, mi.getCodeAttributeInfo());
                        res = il.advanceIndex();
                        assert res == true;
                        int index = 0;
                        int maxStack = 0;
                        byte[] bytes = new byte[] { Opcode.SIPUSH, 0, 0 };
                        for (AAnnotationsAttributeInfo.Annotation.AMemberValue value : host.getEntries()) {
                            il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                            res = il.advanceIndex();
                            assert res == true;
                            Types.bytesFromShort((short) index, bytes, 1);
                            il.insertInstr(new GenericInstruction(bytes), mi.getCodeAttributeInfo());
                            res = il.advanceIndex();
                            assert res == true;
                            int curStack = loadAndTransferMemberValue(value, elementType, cf, mi, il);
                            il.insertInstr(new GenericInstruction(Opcode.AASTORE), mi.getCodeAttributeInfo());
                            res = il.advanceIndex();
                            assert res == true;
                            if (curStack > maxStack) {
                                maxStack = curStack;
                            }
                            ++index;
                        }
                        return 3 + maxStack;
                    }
                    if (elementType.length() == 1) {
                        char ch = elementType.charAt(0);
                        byte atype = 0;
                        GenericInstruction astore = null;
                        switch(ch) {
                            case 'B':
                                atype = 8;
                                astore = new GenericInstruction(Opcode.BASTORE);
                                break;
                            case 'C':
                                atype = 5;
                                astore = new GenericInstruction(Opcode.CASTORE);
                                break;
                            case 'D':
                                atype = 7;
                                astore = new GenericInstruction(Opcode.DASTORE);
                                break;
                            case 'F':
                                atype = 6;
                                astore = new GenericInstruction(Opcode.FASTORE);
                                break;
                            case 'I':
                                atype = 10;
                                astore = new GenericInstruction(Opcode.IASTORE);
                                break;
                            case 'J':
                                atype = 11;
                                astore = new GenericInstruction(Opcode.LASTORE);
                                break;
                            case 'S':
                                atype = 9;
                                astore = new GenericInstruction(Opcode.SASTORE);
                                break;
                            case 'Z':
                                atype = 4;
                                astore = new GenericInstruction(Opcode.BASTORE);
                                break;
                        }
                        if (atype != 0) {
                            GenericInstruction newarray = new GenericInstruction(Opcode.NEWARRAY, atype);
                            IntegerPoolInfo count = new IntegerPoolInfo(host.getEntries().length, cf.getConstantPool());
                            int[] l = cf.addConstantPoolItems(new APoolInfo[] { count });
                            count = cf.getConstantPoolItem(l[0]).execute(new ADefaultPoolInfoVisitor<IntegerPoolInfo, Object>() {

                                public IntegerPoolInfo defaultCase(APoolInfo host, Object param) {
                                    throw new ClassFormatError("Info is of type " + host.getClass().getName() + ", needs to be IntegerPoolInfo");
                                }

                                public IntegerPoolInfo intCase(IntegerPoolInfo host, Object param) {
                                    return host;
                                }
                            }, null);
                            il.insertInstr(new ReferenceInstruction(Opcode.LDC_W, cf.getConstantPool().indexOf(count)), mi.getCodeAttributeInfo());
                            boolean res = il.advanceIndex();
                            assert res == true;
                            il.insertInstr(newarray, mi.getCodeAttributeInfo());
                            res = il.advanceIndex();
                            assert res == true;
                            int index = 0;
                            int maxStack = 0;
                            byte[] bytes = new byte[] { Opcode.SIPUSH, 0, 0 };
                            for (AAnnotationsAttributeInfo.Annotation.AMemberValue value : host.getEntries()) {
                                il.insertInstr(new GenericInstruction(Opcode.DUP), mi.getCodeAttributeInfo());
                                res = il.advanceIndex();
                                assert res == true;
                                Types.bytesFromShort((short) index, bytes, 1);
                                il.insertInstr(new GenericInstruction(bytes), mi.getCodeAttributeInfo());
                                res = il.advanceIndex();
                                assert res == true;
                                int curStack = loadAndTransferMemberValue(value, elementType, cf, mi, il);
                                il.insertInstr(astore, mi.getCodeAttributeInfo());
                                res = il.advanceIndex();
                                assert res == true;
                                if (curStack > maxStack) {
                                    maxStack = curStack;
                                }
                                ++index;
                            }
                            return 3 + maxStack;
                        }
                    }
                    _sharedData.addBadPredicateAnnotWarning(new BadPredicateAnnotationWarning("Element type " + elementType + " not recognized for array for predicate in method " + mi.getName().toString() + " in " + cf.getThisClassName() + " (processing " + _sharedData.getCurrentClassName() + ")"));
                    il.insertInstr(new GenericInstruction(Opcode.ACONST_NULL), mi.getCodeAttributeInfo());
                    boolean res = il.advanceIndex();
                    assert res == true;
                    return 1;
                } else {
                    return 0;
                }
            }
        }, null);
    }

    /**
     * Transfer the annotation to the class file.
     * @param a annotation
     * @param cf target class file
     * @param mi method info
     */
    protected void transferAnnotation(AAnnotationsAttributeInfo.Annotation a, final ClassFile cf, final MethodInfo mi) {
        int[] l;
        AUTFPoolInfo annPoolItem = new ASCIIPoolInfo(a.getType(), cf.getConstantPool());
        l = cf.addConstantPoolItems(new APoolInfo[] { annPoolItem });
        annPoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
        for (AAnnotationsAttributeInfo.Annotation.NameValuePair nvp : a.getPairs()) {
            AUTFPoolInfo pairNamePoolItem = new ASCIIPoolInfo(nvp.getName().toString(), cf.getConstantPool());
            l = cf.addConstantPoolItems(new APoolInfo[] { pairNamePoolItem });
            pairNamePoolItem = cf.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
            loadAndTransferMemberValue(nvp.getValue(), null, cf, mi, null);
        }
    }

    /**
     * Return a generated predicate annotation record for a @Combine-type annotation.
     * @param cf current class file
     * @param par predicate annotation record describing the @Combine-type annotation
     * @param miDescriptor method destrictor
     * @return generated predicate annotation record that can be used like that of a @PredicateLink-type annotation
     */
    protected PredicateAnnotationRecord getGeneratedPredicate(ClassFile cf, PredicateAnnotationRecord par, String miDescriptor) {
        final PredicateAnnotationRecord gr = generatePredicateAnnotationRecord(par, miDescriptor);
        gr.valueList = new ArrayList<AAnnotationsAttributeInfo.Annotation.AMemberValue>();
        performCombineTreeWalk(par, new ILambda.Ternary<Object, String, String, AAnnotationsAttributeInfo.Annotation.AMemberValue>() {

            public Object apply(String param1, String param2, AAnnotationsAttributeInfo.Annotation.AMemberValue param3) {
                gr.valueList.add(param3);
                return null;
            }
        }, "");
        return gr;
    }

    /**
     * Generate the predicate annotation record for the @Combine-type annotation that contains all information
     * except for the valueList.
     * @param par predicate annotation record describing the @Combine-type annotation
     * @param miDescriptor method destrictor
     * @return predicate annotation record without valueList
     */
    protected PredicateAnnotationRecord generatePredicateAnnotationRecord(PredicateAnnotationRecord par, String miDescriptor) {
        String annotClass = par.annotation.getType().substring(1, par.annotation.getType().length() - 1).replace('/', '.');
        String methodName = getMethodName(par);
        String hashKey = annotClass + CLASS_SIG_SEPARATOR_STRING + methodName;
        PredicateAnnotationRecord gr = _generatedPredicateRecords.get(hashKey);
        if (gr != null) {
            _sharedAddData.cacheInfo.incCombinePredicateCacheHit();
            return gr;
        } else {
            _sharedAddData.cacheInfo.incCombinePredicateCacheMiss();
        }
        String predicateClass = ((_predicatePackage.length() > 0) ? (_predicatePackage + ".") : "") + annotClass + "Pred";
        ClassFile predicateCF = null;
        File clonedFile = new File(_predicatePackageDir, annotClass.replace('.', '/') + "Pred.class");
        if (clonedFile.exists() && clonedFile.isFile() && clonedFile.canRead()) {
            try {
                predicateCF = new ClassFile(new FileInputStream(clonedFile));
            } catch (IOException ioe) {
                throw new ThreadCheckException("Could not open predicate class file, source=" + clonedFile, ioe);
            }
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                _templatePredicateClassFile.write(baos);
                predicateCF = new ClassFile(new ByteArrayInputStream(baos.toByteArray()));
            } catch (IOException ioe) {
                throw new ThreadCheckException("Could not open predicate template class file", ioe);
            }
        }
        clonedFile.getParentFile().mkdirs();
        final ArrayList<String> paramNames = new ArrayList<String>();
        final HashMap<String, String> paramTypes = new HashMap<String, String>();
        performCombineTreeWalk(par, new ILambda.Ternary<Object, String, String, AAnnotationsAttributeInfo.Annotation.AMemberValue>() {

            public Object apply(String param1, String param2, AAnnotationsAttributeInfo.Annotation.AMemberValue param3) {
                paramNames.add(param1);
                paramTypes.put(param1, param2);
                return null;
            }
        }, "");
        ArrayList<PredicateAnnotationRecord> memberPARs = new ArrayList<PredicateAnnotationRecord>();
        for (String key : par.combinedPredicates.keySet()) {
            for (PredicateAnnotationRecord memberPAR : par.combinedPredicates.get(key)) {
                if ((memberPAR.predicateClass != null) && (memberPAR.predicateMI != null)) {
                    memberPARs.add(memberPAR);
                } else {
                    memberPARs.add(generatePredicateAnnotationRecord(memberPAR, miDescriptor));
                }
            }
        }
        AUTFPoolInfo predicateClassNameItem = new ASCIIPoolInfo(predicateClass.replace('.', '/'), predicateCF.getConstantPool());
        int[] l = predicateCF.addConstantPoolItems(new APoolInfo[] { predicateClassNameItem });
        predicateClassNameItem = predicateCF.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
        ClassPoolInfo predicateClassItem = new ClassPoolInfo(predicateClassNameItem, predicateCF.getConstantPool());
        l = predicateCF.addConstantPoolItems(new APoolInfo[] { predicateClassItem });
        predicateClassItem = predicateCF.getConstantPoolItem(l[0]).execute(CheckClassVisitor.singleton(), null);
        predicateCF.setThisClass(predicateClassItem);
        StringBuilder sb = new StringBuilder();
        sb.append("(Ljava/lang/Object;");
        if (par.passArguments) {
            sb.append("[Ljava/lang/Object;");
        }
        for (String key : paramNames) {
            sb.append(paramTypes.get(key));
        }
        sb.append(")Z");
        String methodDesc = sb.toString();
        MethodInfo templateMI = null;
        MethodInfo predicateMI = null;
        for (MethodInfo mi : predicateCF.getMethods()) {
            if ((mi.getName().toString().equals(methodName)) && (mi.getDescriptor().toString().equals(methodDesc))) {
                predicateMI = mi;
                break;
            } else if ((mi.getName().toString().equals("template")) && (mi.getDescriptor().toString().startsWith("(")) && (mi.getDescriptor().toString().endsWith(")Z"))) {
                templateMI = mi;
            }
        }
        if ((templateMI == null) && (predicateMI == null)) {
            throw new ThreadCheckException("Could not find template predicate method in class file");
        }
        if (predicateMI == null) {
            AUTFPoolInfo namecpi = new ASCIIPoolInfo(methodName, predicateCF.getConstantPool());
            l = predicateCF.addConstantPoolItems(new APoolInfo[] { namecpi });
            namecpi = predicateCF.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
            AUTFPoolInfo descpi = new ASCIIPoolInfo(methodDesc, predicateCF.getConstantPool());
            l = predicateCF.addConstantPoolItems(new APoolInfo[] { descpi });
            descpi = predicateCF.getConstantPoolItem(l[0]).execute(CheckUTFVisitor.singleton(), null);
            ArrayList<AAttributeInfo> list = new ArrayList<AAttributeInfo>();
            for (AAttributeInfo a : templateMI.getAttributes()) {
                try {
                    AAttributeInfo clonedA = (AAttributeInfo) a.clone();
                    list.add(clonedA);
                } catch (CloneNotSupportedException e) {
                    throw new InstrumentorException("Could not clone method attributes");
                }
            }
            predicateMI = new MethodInfo(templateMI.getAccessFlags(), namecpi, descpi, list.toArray(new AAttributeInfo[] {}));
            predicateCF.getMethods().add(predicateMI);
            CodeAttributeInfo.CodeProperties props = predicateMI.getCodeAttributeInfo().getProperties();
            props.maxLocals += paramTypes.size() + 1 + (par.passArguments ? 1 : 0);
            InstructionList il = new InstructionList(predicateMI.getCodeAttributeInfo().getCode());
            if ((par.combineMode == Combine.Mode.OR) || (par.combineMode == Combine.Mode.XOR) || (par.combineMode == Combine.Mode.IMPLIES)) {
                il.insertInstr(new GenericInstruction(Opcode.ICONST_0), predicateMI.getCodeAttributeInfo());
            } else {
                il.insertInstr(new GenericInstruction(Opcode.ICONST_1), predicateMI.getCodeAttributeInfo());
            }
            boolean res;
            res = il.advanceIndex();
            assert res == true;
            int accumVarIndex = props.maxLocals - 1;
            AInstruction loadAccumInstr;
            AInstruction storeAccumInstr;
            if (accumVarIndex < 256) {
                loadAccumInstr = new GenericInstruction(Opcode.ILOAD, (byte) accumVarIndex);
                storeAccumInstr = new GenericInstruction(Opcode.ISTORE, (byte) accumVarIndex);
            } else {
                byte[] bytes = new byte[] { Opcode.ILOAD, 0, 0 };
                Types.bytesFromShort((short) accumVarIndex, bytes, 1);
                loadAccumInstr = new WideInstruction(bytes);
                bytes[0] = Opcode.ISTORE;
                storeAccumInstr = new WideInstruction(bytes);
            }
            il.insertInstr(storeAccumInstr, predicateMI.getCodeAttributeInfo());
            res = il.advanceIndex();
            assert res == true;
            int maxStack = 0;
            int paramIndex = 1;
            int lvIndex = 1;
            if (par.passArguments) {
                lvIndex += 1;
            }
            int memberCount = 0;
            for (PredicateAnnotationRecord memberPAR : memberPARs) {
                ++memberCount;
                il.insertInstr(new GenericInstruction(Opcode.ALOAD_0), predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                int curStack = 1;
                if (memberPAR.passArguments) {
                    if (par.passArguments) {
                        il.insertInstr(new GenericInstruction(Opcode.ALOAD_1), predicateMI.getCodeAttributeInfo());
                        res = il.advanceIndex();
                        assert res == true;
                        curStack += 1;
                    }
                }
                for (int paramNameIndex = 0; paramNameIndex < memberPAR.paramNames.size(); ++paramNameIndex) {
                    String t = memberPAR.paramTypes.get(memberPAR.paramNames.get(paramNameIndex));
                    if (t.length() == 0) {
                        throw new ThreadCheckException("Length of parameter type no. " + paramIndex + " string is 0 in " + predicateMI.getName() + " in class " + predicateCF.getThisClassName());
                    }
                    byte opcode;
                    int nextLVIndex = lvIndex;
                    switch(t.charAt(0)) {
                        case 'I':
                        case 'B':
                        case 'C':
                        case 'S':
                        case 'Z':
                            opcode = Opcode.ILOAD;
                            nextLVIndex += 1;
                            curStack += 1;
                            break;
                        case 'F':
                            opcode = Opcode.FLOAD;
                            nextLVIndex += 1;
                            curStack += 1;
                            break;
                        case '[':
                        case 'L':
                            opcode = Opcode.ALOAD;
                            nextLVIndex += 1;
                            curStack += 1;
                            break;
                        case 'J':
                            opcode = Opcode.LLOAD;
                            nextLVIndex += 2;
                            curStack += 2;
                            break;
                        case 'D':
                            opcode = Opcode.DLOAD;
                            nextLVIndex += 2;
                            curStack += 2;
                            break;
                        default:
                            throw new ThreadCheckException("Parameter type no. " + paramIndex + ", " + t + ", is unknown in " + predicateMI.getName() + " in class " + predicateCF.getThisClassName());
                    }
                    AInstruction load = Opcode.getShortestLoadStoreInstruction(opcode, (short) lvIndex);
                    il.insertInstr(load, predicateMI.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    ++paramIndex;
                    lvIndex = nextLVIndex;
                }
                if (curStack > maxStack) {
                    maxStack = curStack;
                }
                ReferenceInstruction predicateCallInstr = new ReferenceInstruction(Opcode.INVOKESTATIC, (short) 0);
                int predicateCallIndex = predicateCF.addMethodToConstantPool(memberPAR.predicateClass.replace('.', '/'), memberPAR.predicateMI.getName().toString(), memberPAR.predicateMI.getDescriptor().toString());
                predicateCallInstr.setReference(predicateCallIndex);
                il.insertInstr(predicateCallInstr, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                if ((par.combineMode == Combine.Mode.NOT) || ((par.combineMode == Combine.Mode.IMPLIES) && (memberCount == 1))) {
                    il.insertInstr(new GenericInstruction(Opcode.ICONST_1), predicateMI.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    il.insertInstr(new GenericInstruction(Opcode.SWAP), predicateMI.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                    il.insertInstr(new GenericInstruction(Opcode.ISUB), predicateMI.getCodeAttributeInfo());
                    res = il.advanceIndex();
                    assert res == true;
                }
                il.insertInstr(loadAccumInstr, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                if (par.combineMode == Combine.Mode.OR) {
                    il.insertInstr(new GenericInstruction(Opcode.IOR), predicateMI.getCodeAttributeInfo());
                } else if ((par.combineMode == Combine.Mode.AND) || (par.combineMode == Combine.Mode.NOT)) {
                    il.insertInstr(new GenericInstruction(Opcode.IAND), predicateMI.getCodeAttributeInfo());
                } else if (par.combineMode == Combine.Mode.XOR) {
                    il.insertInstr(new GenericInstruction(Opcode.IADD), predicateMI.getCodeAttributeInfo());
                } else if (par.combineMode == Combine.Mode.IMPLIES) {
                    il.insertInstr(new GenericInstruction(Opcode.IOR), predicateMI.getCodeAttributeInfo());
                } else {
                    assert false;
                }
                res = il.advanceIndex();
                assert res == true;
                il.insertInstr(storeAccumInstr, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
            }
            if (par.combineMode == Combine.Mode.XOR) {
                il.insertInstr(loadAccumInstr, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                il.insertInstr(new GenericInstruction(Opcode.ICONST_1), predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                il.insertInstr(new GenericInstruction(Opcode.ICONST_0), predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                WideBranchInstruction br2 = new WideBranchInstruction(Opcode.GOTO_W, il.getIndex() + 1);
                il.insertInstr(br2, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                int jumpIndex = il.getIndex();
                il.insertInstr(new GenericInstruction(Opcode.ICONST_1), predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
                res = il.rewindIndex(3);
                assert res == true;
                BranchInstruction br1 = new BranchInstruction(Opcode.IF_ICMPEQ, jumpIndex);
                il.insertInstr(br1, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex(4);
                assert res == true;
            } else {
                il.insertInstr(loadAccumInstr, predicateMI.getCodeAttributeInfo());
                res = il.advanceIndex();
                assert res == true;
            }
            il.deleteInstr(predicateMI.getCodeAttributeInfo());
            predicateMI.getCodeAttributeInfo().setCode(il.getCode());
            props.maxStack = Math.max(maxStack, 2);
            predicateMI.getCodeAttributeInfo().setProperties(props.maxStack, props.maxLocals);
            try {
                FileOutputStream fos = new FileOutputStream(clonedFile);
                predicateCF.write(fos);
                fos.close();
            } catch (IOException e) {
                throw new ThreadCheckException("Could not write cloned predicate class file, target=" + clonedFile);
            }
        }
        gr = new PredicateAnnotationRecord(par.annotation, predicateClass, predicateMI, paramNames, paramTypes, new ArrayList<AAnnotationsAttributeInfo.Annotation.AMemberValue>(), par.passArguments, null, new HashMap<String, ArrayList<PredicateAnnotationRecord>>());
        _generatedPredicateRecords.put(hashKey, gr);
        return gr;
    }

    /**
     * Walk the tree of combined predicates and apply the lambda for each parameter name-parameter type-value triple.
     * @param par record describing the combined predicate annotation
     * @param lambda lambda to apply to each parameter name-parameter type-value triple; return value is ignored
     * @param suffix string suffix for variable names to distinguish them
     */
    protected void performCombineTreeWalk(PredicateAnnotationRecord par, ILambda.Ternary<Object, String, String, AAnnotationsAttributeInfo.Annotation.AMemberValue> lambda, String suffix) {
        if (par.combineMode == null) {
            int startIndex = 0;
            for (int i = startIndex; i < par.paramNames.size(); ++i) {
                String name = par.paramNames.get(i);
                lambda.apply(name + suffix, par.paramTypes.get(name), par.valueList.get(i));
            }
        } else {
            for (String key : par.combinedPredicates.keySet()) {
                int i = 0;
                for (PredicateAnnotationRecord memberPAR : par.combinedPredicates.get(key)) {
                    performCombineTreeWalk(memberPAR, lambda, "$" + key + "$" + i + suffix);
                    ++i;
                }
            }
        }
    }

    /**
     * Return the name of the predicate method for a @Combine-type annotation, taking annotation array sizes
     * into account. The method will start with the method name "check", perform a pre-order tree traversal, and
     * for every annotation array found, it will append a string of the format "$value$member$$1$id$$$4", where
     * "$value$$0$member$$1$id" represents the path of @Combine-type annotation members that have lead to this
     * annotation array, with the last element being the name of the annotation array itself, and the number
     * after the "$$$" represents the size of the array. If an annotation member on the path is an array, then
     * the index of the array element chosen is represented after the array member name and "$$".
     * In the example above, "$value$member$$1$id$$$4", the array "id" has size 4. It is contained in element 1
     * of the array "member", which is contained in the non-array member "value".
     * @param par record describing the combined predicate annotation
     * @return method name
     */
    protected String getMethodName(PredicateAnnotationRecord par) {
        StringBuilder sb = new StringBuilder("check");
        if (par.combineMode != null) {
            getMethodNameHelper(par, sb, "");
        }
        return sb.toString();
    }

    /**
     * Return part of the method name.
     * @param par record describing the combined predicate annotation
     * @param sb string builder accumulating the nethod name
     * @param suffix string suffix for variable names to distinguish them
     */
    protected void getMethodNameHelper(PredicateAnnotationRecord par, StringBuilder sb, String suffix) {
        if (par.combineMode != null) {
            for (String key : par.combinedPredicates.keySet()) {
                if (par.paramTypes.get(key).startsWith("[")) {
                    sb.append(suffix);
                    sb.append('$');
                    sb.append(key);
                    sb.append("$$$");
                    sb.append(par.combinedPredicates.get(key).size());
                    int i = 0;
                    for (PredicateAnnotationRecord memberPAR : par.combinedPredicates.get(key)) {
                        getMethodNameHelper(memberPAR, sb, suffix + "$" + key + "$$" + i);
                        ++i;
                    }
                } else {
                    for (PredicateAnnotationRecord memberPAR : par.combinedPredicates.get(key)) {
                        getMethodNameHelper(memberPAR, sb, suffix + "$" + key);
                    }
                }
            }
        }
    }
}
