package net.sf.refactorit.refactorings.encapsulatefield;

import net.sf.refactorit.classmodel.BinArrayType;
import net.sf.refactorit.classmodel.BinCIType;
import net.sf.refactorit.classmodel.BinClass;
import net.sf.refactorit.classmodel.BinField;
import net.sf.refactorit.classmodel.BinInterface;
import net.sf.refactorit.classmodel.BinMethod;
import net.sf.refactorit.classmodel.BinModifier;
import net.sf.refactorit.classmodel.BinParameter;
import net.sf.refactorit.classmodel.BinPrimitiveType;
import net.sf.refactorit.classmodel.BinSourceConstruct;
import net.sf.refactorit.classmodel.BinTypeRef;
import net.sf.refactorit.classmodel.CompilationUnit;
import net.sf.refactorit.common.util.CollectionUtil;
import net.sf.refactorit.query.usage.EncapsulateFieldIndexer;
import net.sf.refactorit.query.usage.EncapsulationInvocationData;
import net.sf.refactorit.query.usage.ManagingIndexer;
import net.sf.refactorit.query.usage.MethodIndexer;
import net.sf.refactorit.query.usage.filters.BinMethodSearchFilter;
import net.sf.refactorit.query.usage.filters.BinVariableSearchFilter;
import net.sf.refactorit.refactorings.AbstractRefactoring;
import net.sf.refactorit.refactorings.MemberVisibilityAnalyzer;
import net.sf.refactorit.refactorings.PropertyNameUtil;
import net.sf.refactorit.refactorings.RefactoringStatus;
import net.sf.refactorit.refactorings.minaccess.MinimizeAccessUtil;
import net.sf.refactorit.source.edit.ModifierEditor;
import net.sf.refactorit.source.edit.StringInserter;
import net.sf.refactorit.source.format.BinMethodFormatter;
import net.sf.refactorit.source.format.BinModifierFormatter;
import net.sf.refactorit.source.format.BinTypeFormatter;
import net.sf.refactorit.source.format.FormatSettings;
import net.sf.refactorit.transformations.TransformationList;
import net.sf.refactorit.ui.module.RefactorItContext;
import net.sf.refactorit.utils.EncapsulateUtils;
import net.sf.refactorit.utils.GetterSetterUtils;
import net.sf.refactorit.utils.TypeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author  Tanel Alumae
 */
public class EncapsulateField extends AbstractRefactoring implements Comparable {

    public static String key = "refactoring.encapsulatefield";

    private BinField field;

    private BinCIType hostingClass;

    private List usages;

    private boolean encapsulateRead = true;

    private boolean encapsulateWrite = true;

    public boolean encR = true;

    public boolean encW = true;

    private String setterName;

    private String getterName;

    private int fieldVisibility = -1;

    private int getterVisibility = BinModifier.PUBLIC;

    private int setterVisibility = BinModifier.PUBLIC;

    private boolean accessorsChanged = true;

    private ManagingIndexer supervisor = null;

    private boolean enabled = false;

    public EncapsulateField(RefactorItContext context, BinField field) {
        super("EncapsulateField", context);
        this.field = field;
        setterName = PropertyNameUtil.getDefaultSetterName(field);
        getterName = PropertyNameUtil.getDefaultGetterName(field)[0];
        hostingClass = field.getOwner().getBinCIType();
    }

    /**
   * @see net.sf.refactorit.refactorings.Refactoring#checkPreconditions
   */
    public RefactoringStatus checkPreconditions() {
        RefactoringStatus status = new RefactoringStatus();
        if (getField().getOwner().getBinCIType() instanceof BinInterface) {
            status.addEntry("Cannot encapsulate field of an interface", RefactoringStatus.ERROR);
        }
        return status;
    }

    /**
   * @see net.sf.refactorit.refactorings.Refactoring#checkUserInput
   */
    public RefactoringStatus checkUserInput() {
        RefactoringStatus status = new RefactoringStatus();
        checkAccessorSuitabilities(status);
        return status;
    }

    public RefactoringStatus checkUserInput(RefactoringStatus status) {
        checkAccessorSuitabilities(status);
        return status;
    }

    /**
   * Checks whether the encapsulation can be done using the current getter and
   * setter names and their visibilities.
   *
   */
    private void checkAccessorSuitabilities(RefactoringStatus status) {
        encR = encapsulateRead;
        encW = encapsulateWrite;
        if (encapsulateRead) {
            BinMethod getter = getGetterMethod();
            List getterErrors = new ArrayList();
            if (getter != null) {
                status.addEntry("Getter '" + getter + "' already exists", RefactoringStatus.WARNING);
                if (!GetterSetterUtils.isGetterMethod(getter, getField(), new String[] { getterName }, false)) {
                    getterErrors.add("Signature not suitable for a getter method!");
                } else {
                    checkThrows(getter, getterErrors);
                    checkVisibilityChange(getter, getGetterVisibility(), getterErrors);
                }
                if (getterErrors.size() > 0) {
                    status.addEntry("Cannot use '" + getter + "' for getter", getterErrors, RefactoringStatus.ERROR);
                }
            } else {
                getter = EncapsulateUtils.createVirtualGetter(getterName, field, getGetterVisibility());
                checkVirtualAccessor(getter, getterErrors);
                if (getterErrors.size() > 0) {
                    status.addEntry("Cannot create getter '" + getter + "'", getterErrors, RefactoringStatus.ERROR);
                    encR = false;
                }
            }
        }
        if (encapsulateWrite) {
            BinMethod setter = getSetterMethod();
            List setterErrors = new ArrayList();
            if (setter != null) {
                status.addEntry("Setter '" + setter + "' already exists", RefactoringStatus.WARNING);
                if (!GetterSetterUtils.isSetterMethod(setter, getField(), setterName, false)) {
                    setterErrors.add("Signature not suitable for a setter method!");
                } else {
                    checkThrows(setter, setterErrors);
                    checkVisibilityChange(setter, getSetterVisibility(), setterErrors);
                }
                if (setterErrors.size() > 0) {
                    status.addEntry("Cannot use '" + setter + "' for setter", setterErrors, RefactoringStatus.ERROR);
                }
            } else {
                setter = EncapsulateUtils.createVirtualSetter(setterName, field, getSetterVisibility());
                checkVirtualAccessor(setter, setterErrors);
                if (setterErrors.size() > 0) {
                    status.addEntry("Cannot create setter '" + setter + "'", setterErrors, RefactoringStatus.ERROR);
                }
            }
        }
    }

    /**
   * Checks that a methods does not throw an exception. If it does, adds and
   * error entry to the RefactoringStatus object.
   */
    private void checkThrows(BinMethod method, List errors) {
        if ((method != null) && (method.getThrows().length > 0)) {
            errors.add("Method already exists but throws exception(s)");
        }
    }

    private void checkVisibilityChange(BinMethod method, int visibility, List errors) {
        if (BinModifier.hasFlag(method.getAccessModifier(), visibility)) {
            return;
        } else {
            List usages = getInvocationsForMethod(method);
            int[] allowedVisibilities = MinimizeAccessUtil.findMethodAccessRights(method, usages);
            Arrays.sort(allowedVisibilities);
            if (Arrays.binarySearch(allowedVisibilities, visibility) < 0) {
                errors.add("The visibility of the method cannot be changed to " + new BinModifierFormatter(visibility, true).print());
            }
        }
    }

    /**
   * Checks whether a to-be-created accessor doesn't conflict with the current
   * classmodel.
   *
   * @param method virtual to-be-created accessor
   * @param errors list of error messages that is populated
   */
    private void checkVirtualAccessor(BinMethod method, List errors) {
        for (final Iterator i = hostingClass.getTypeRef().getAllSubclasses().iterator(); i.hasNext(); ) {
            BinCIType subtype = ((BinTypeRef) i.next()).getBinCIType();
            BinMethod[] methods = subtype.getDeclaredMethods();
            for (int pos = 0, max = methods.length; pos < max; pos++) {
                BinMethod current = methods[pos];
                if (current.isApplicable(method) && method.isAccessible(hostingClass, subtype) && !current.isAbstract()) {
                    CollectionUtil.addNew(errors, "Conflicts with the existing method '" + current + "'");
                }
            }
        }
        for (final Iterator i = hostingClass.getTypeRef().getAllSupertypes().iterator(); i.hasNext(); ) {
            BinCIType supertype = ((BinTypeRef) i.next()).getBinCIType();
            BinMethod[] methods = supertype.getDeclaredMethods();
            for (int pos = 0, max = methods.length; pos < max; pos++) {
                BinMethod current = methods[pos];
                if (current.isApplicable(method) && current.isAccessible(supertype, hostingClass) && !current.isAbstract()) {
                    CollectionUtil.addNew(errors, "Conflicts with the existing method '" + current + "'");
                }
            }
        }
    }

    /**
   * @see net.sf.refactorit.refactorings.Refactoring#performChange
   */
    public TransformationList performChange() {
        TransformationList transList = new TransformationList();
        if (this.fieldVisibility != -1) {
            int changedAccessModifier = -1;
            MemberVisibilityAnalyzer accessAnalyzer = new MemberVisibilityAnalyzer(this.field);
            int allowedAccess = accessAnalyzer.getPosterioriFieldAccess(this.getAllUsages(), this.usages);
            if (this.fieldVisibility == BinModifier.PRIVATE) {
                if (allowedAccess != BinModifier.PRIVATE) {
                    changedAccessModifier = allowedAccess;
                }
            } else if (this.fieldVisibility == BinModifier.PACKAGE_PRIVATE) {
                if ((allowedAccess != BinModifier.PRIVATE) && (allowedAccess != BinModifier.PACKAGE_PRIVATE)) {
                    changedAccessModifier = allowedAccess;
                }
            } else if (this.fieldVisibility == BinModifier.PROTECTED) {
                if (allowedAccess == BinModifier.PUBLIC) {
                    changedAccessModifier = allowedAccess;
                }
            }
            if (changedAccessModifier != -1) {
                transList.getStatus().addEntry("Couldn't change field access to " + getAccessName(fieldVisibility) + ".\n" + "Using " + getAccessName(changedAccessModifier) + " access instead.", RefactoringStatus.WARNING);
                this.fieldVisibility = changedAccessModifier;
            }
        }
        Map usageMap = new HashMap();
        for (int i = 0; i < usages.size(); ++i) {
            EncapsulationInvocationData id = (EncapsulationInvocationData) usages.get(i);
            if (id.isEncapsulationPossible()) {
                CompilationUnit sf = id.getCompilationUnit();
                List usagesInSource = (List) usageMap.get(sf);
                if (usagesInSource == null) {
                    usagesInSource = new ArrayList();
                    usageMap.put(sf, usagesInSource);
                }
                usagesInSource.add(id);
            }
        }
        for (Iterator i = usageMap.keySet().iterator(); i.hasNext(); ) {
            CompilationUnit compilationUnit = (CompilationUnit) i.next();
            new EncapsulateEditor(field, getterName, setterName, (List) usageMap.get(compilationUnit)).generateEditors(transList);
        }
        int column = 0;
        int line = 0;
        BinClass hostingClass = (BinClass) getField().getOwner().getBinCIType();
        line = hostingClass.getEndLine();
        StringBuffer buffer = new StringBuffer();
        if (encapsulateRead) {
            BinMethod getter = getGetterMethod();
            if (getter == null) {
                buffer.append(createGetterBody());
            } else {
                transList.add(new ModifierEditor(getter, BinModifier.setFlags(getter.getModifiers(), this.getGetterVisibility())));
            }
        }
        if (encapsulateWrite) {
            BinMethod setter = getSetterMethod();
            if (setter == null) {
                buffer.append(createSetterBody());
            } else {
                transList.add(new ModifierEditor(setter, BinModifier.setFlags(setter.getModifiers(), this.getSetterVisibility())));
            }
        }
        if (buffer.length() > 0) {
            StringInserter inserter = new StringInserter(hostingClass.getCompilationUnit(), line, column, buffer.toString());
            transList.add(inserter);
        }
        if (this.fieldVisibility != -1) {
            transList.add(new ModifierEditor(field, BinModifier.setFlags(field.getModifiers(), fieldVisibility)));
        }
        return transList;
    }

    String getCorrectTypeName(BinTypeRef aTypeRef) {
        if (aTypeRef.isPrimitiveType()) {
            return aTypeRef.getName();
        }
        if (aTypeRef.isArray()) {
            BinArrayType at = (BinArrayType) aTypeRef.getBinType();
            return getCorrectTypeName(at.getArrayType()) + at.getDimensionString();
        }
        String name = TypeUtil.getShortestUnderstandableName(aTypeRef.getBinCIType(), getField().getOwner().getBinCIType());
        if ("".equals(name)) {
            name = aTypeRef.getName();
        }
        return name;
    }

    /**
   * Creates a method with a body for the getter.
   *
   * @return getter body
   */
    private String createGetterBody() {
        int modifier = this.getGetterVisibility();
        if (field.isStatic()) {
            modifier |= BinModifier.STATIC;
        }
        BinMethod getter = new BinMethod(getterName, BinParameter.NO_PARAMS, getField().getTypeRef(), modifier, BinMethod.Throws.NO_THROWS);
        getter.setOwner(field.getOwner());
        BinMethodFormatter formatter = (BinMethodFormatter) getter.getFormatter();
        String result = FormatSettings.LINEBREAK + formatter.formHeader();
        int baseIndent = new BinTypeFormatter(field.getOwner().getBinCIType()).getMemberIndent();
        result += FormatSettings.getIndentString(baseIndent + FormatSettings.getBlockIndent());
        result += "return ";
        if (field.isStatic()) {
            result += hostingClass.getName();
        } else {
            result += "this";
        }
        result += "." + getField().getName() + ";";
        result += FormatSettings.LINEBREAK;
        result += formatter.formFooter();
        return result;
    }

    /**
   * Creates a method with a body for the setter.
   *
   * @return setter body
   */
    private String createSetterBody() {
        int modifier = this.getGetterVisibility();
        if (field.isStatic()) {
            modifier |= BinModifier.STATIC;
        }
        BinParameter[] params = new BinParameter[1];
        params[0] = new BinParameter(getField().getName(), getField().getTypeRef(), BinModifier.FINAL);
        BinMethod setter = new BinMethod(setterName, params, BinPrimitiveType.VOID.getTypeRef(), modifier, BinMethod.Throws.NO_THROWS);
        setter.setOwner(field.getOwner());
        BinMethodFormatter formatter = (BinMethodFormatter) setter.getFormatter();
        String result = FormatSettings.LINEBREAK + formatter.formHeader();
        int baseIndent = new BinTypeFormatter(field.getOwner().getBinCIType()).getMemberIndent();
        result += FormatSettings.getIndentString(baseIndent + FormatSettings.getBlockIndent());
        if (field.isStatic()) {
            result += hostingClass.getName();
        } else {
            result += "this";
        }
        result += "." + getField().getName() + " = " + getField().getName() + ";";
        result += FormatSettings.LINEBREAK;
        result += formatter.formFooter();
        return result;
    }

    /**
   * @return field to be encapsulated
   **/
    public BinField getField() {
        return field;
    }

    /**
   * Sets field usages that are to be replaced with method calls.
   *
   **/
    public void setUsages(List usages) {
        this.usages = usages;
    }

    /**
   * Gets field usages that are to be replaced with method calls.
   *
   **/
    public List getAllUsages() {
        List usages = getSupervisor(field).getInvocations();
        return usages;
    }

    /**
   * Returns usages that can be encapsulated according to the
   * current state of the object. That is, only read, write, or
   * both usages.
   */
    public List getEncapsulateUsages() {
        return getAllUsages(encapsulateRead, encapsulateWrite);
    }

    /**
   * Returns only usages where variable is read, written, or both.
   **/
    public List getAllUsages(boolean read, boolean write) {
        List allUsages = getAllUsages();
        if (read && write) {
            return allUsages;
        } else {
            List usages = new ArrayList();
            for (Iterator i = allUsages.iterator(); i.hasNext(); ) {
                EncapsulationInvocationData data = (EncapsulationInvocationData) i.next();
                BinSourceConstruct sourceConstruct = (BinSourceConstruct) data.getInConstruct();
                if ((read && (BinVariableSearchFilter.isReadAccess(sourceConstruct))) || (write && (BinVariableSearchFilter.isWriteAccess(sourceConstruct)))) {
                    usages.add(data);
                }
            }
            return usages;
        }
    }

    /**
   * Sets name of the setter method for the field to be encapsulated.
   *
   * @param setterName name of the setter method
   */
    public void setSetterName(String setterName) {
        if (!this.setterName.equals(setterName)) {
            this.setterName = setterName;
            this.accessorsChanged = true;
        }
    }

    /**
   * Sets name of the getter method for the field to be encapsulated.
   *
   * @param getterName name of the setter method
   */
    public void setGetterName(String getterName) {
        if (!this.getterName.equals(getterName)) {
            this.getterName = getterName;
            this.accessorsChanged = true;
        }
    }

    public ManagingIndexer getSupervisor(BinField field) {
        if ((supervisor == null) || (accessorsChanged)) {
            supervisor = new ManagingIndexer();
            new EncapsulateFieldIndexer(supervisor, field, getterName, setterName);
            if (field.isPrivate()) {
                supervisor.visit(field.getCompilationUnit());
            } else {
                supervisor.visit(field.getOwner().getProject());
            }
        }
        this.accessorsChanged = false;
        return supervisor;
    }

    /**
   * @return getter for curent encapsulation, null if not present
   */
    private BinMethod getGetterMethod() {
        return hostingClass.getDeclaredMethod(getterName, BinTypeRef.NO_TYPEREFS);
    }

    /**
   * @return setter for curent encapsulation, null if not present
   */
    private BinMethod getSetterMethod() {
        return hostingClass.getDeclaredMethod(setterName, new BinParameter[] { new BinParameter(field.getName(), field.getTypeRef(), 0) });
    }

    public int getFieldVisibility() {
        return this.fieldVisibility;
    }

    public void setFieldVisibility(int fieldVisibility) {
        this.fieldVisibility = fieldVisibility;
    }

    private String getAccessName(int modifier) {
        switch(modifier) {
            case (BinModifier.PRIVATE):
                return "private";
            case (BinModifier.PACKAGE_PRIVATE):
                return "package private";
            case (BinModifier.PROTECTED):
                return "protected";
            case (BinModifier.PUBLIC):
                return "public";
            default:
                throw new RuntimeException("getAccessName() called with invalid parameter: " + modifier);
        }
    }

    public int getGetterVisibility() {
        return this.getterVisibility;
    }

    public void setGetterVisibility(int getterVisibility) {
        this.getterVisibility = getterVisibility;
    }

    public int getSetterVisibility() {
        return this.setterVisibility;
    }

    public void setSetterVisibility(int setterVisibility) {
        this.setterVisibility = setterVisibility;
    }

    public boolean isEncapsulateRead() {
        return this.encapsulateRead;
    }

    public void setEncapsulateRead(boolean encapsulateRead) {
        this.encapsulateRead = encapsulateRead;
    }

    public boolean isEncapsulateWrite() {
        return this.encapsulateWrite;
    }

    public void setEncapsulateWrite(boolean encapsulateWrite) {
        this.encapsulateWrite = encapsulateWrite;
    }

    private static List getInvocationsForMethod(BinMethod method) {
        ManagingIndexer supervisor = new ManagingIndexer();
        new MethodIndexer(supervisor, method, new BinMethodSearchFilter(true, true, true, true, false, false, false, false, false));
        supervisor.visit(method.getProject());
        return supervisor.getInvocations();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled() {
        this.enabled = true;
    }

    public String getDescription() {
        return super.getDescription();
    }

    public int compareTo(Object o) {
        return this.field.getName().compareTo(((EncapsulateField) o).getField().getName());
    }

    public String getKey() {
        return key;
    }
}
