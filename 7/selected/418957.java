package net.sf.refactorit.audit.rules;

import net.sf.refactorit.audit.AuditRule;
import net.sf.refactorit.audit.CorrectiveAction;
import net.sf.refactorit.audit.MultiTargetGroupingAction;
import net.sf.refactorit.audit.RuleViolation;
import net.sf.refactorit.audit.SimpleViolation;
import net.sf.refactorit.classmodel.BinCIType;
import net.sf.refactorit.classmodel.BinCatchParameter;
import net.sf.refactorit.classmodel.BinConstructor;
import net.sf.refactorit.classmodel.BinInitializer;
import net.sf.refactorit.classmodel.BinLocalVariable;
import net.sf.refactorit.classmodel.BinMember;
import net.sf.refactorit.classmodel.BinMethod;
import net.sf.refactorit.classmodel.BinParameter;
import net.sf.refactorit.classmodel.BinVariable;
import net.sf.refactorit.classmodel.CompilationUnit;
import net.sf.refactorit.classmodel.expressions.BinExpression;
import net.sf.refactorit.classmodel.statements.BinLocalVariableDeclaration;
import net.sf.refactorit.common.util.MultiValueMap;
import net.sf.refactorit.loader.Comment;
import net.sf.refactorit.query.usage.UnusedVariablesIndexer;
import net.sf.refactorit.refactorings.RefactoringStatus;
import net.sf.refactorit.refactorings.changesignature.ChangeMethodSignatureRefactoring;
import net.sf.refactorit.refactorings.changesignature.MethodSignatureChange;
import net.sf.refactorit.refactorings.rename.RenameMethod;
import net.sf.refactorit.source.edit.StringEraser;
import net.sf.refactorit.source.edit.StringInserter;
import net.sf.refactorit.source.format.BinFormatter;
import net.sf.refactorit.source.format.BinModifierFormatter;
import net.sf.refactorit.source.format.FormatSettings;
import net.sf.refactorit.transformations.TransformationList;
import net.sf.refactorit.transformations.TransformationManager;
import net.sf.refactorit.ui.dialog.RitDialog;
import net.sf.refactorit.ui.module.ModuleManager;
import net.sf.refactorit.ui.module.RefactorItAction;
import net.sf.refactorit.ui.module.RefactorItContext;
import net.sf.refactorit.ui.module.TreeRefactorItContext;
import net.sf.refactorit.ui.refactoring.rename.RenameAction;
import net.sf.refactorit.utils.AuditProfileUtils;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnusedLocalVariableRule extends AuditRule {

    public static final String NAME = "unused_variable";

    private boolean skip_catch_params = true;

    private boolean skip_method_params = false;

    private MultiValueMap foundMethods = new MultiValueMap();

    public void init() {
        super.init();
        final Element configuration = getConfiguration();
        skip_catch_params = AuditProfileUtils.getBooleanOption(configuration, "skip", "catch_parameters", skip_catch_params);
        skip_method_params = AuditProfileUtils.getBooleanOption(configuration, "skip", "method_parameters", skip_method_params);
    }

    public void visit(CompilationUnit sf) {
        if (sf.getMainType() != null) {
            super.visit(sf);
        }
    }

    public void visit(BinMethod aMethod) {
        handleUnusedVariables(aMethod);
    }

    public void visit(BinConstructor aConstructor) {
        handleUnusedVariables(aConstructor);
    }

    public void visit(BinInitializer anInitializer) {
        handleUnusedVariables(anInitializer);
    }

    private void handleUnusedVariables(BinMember aMember) {
        final BinCIType ownerCIType = aMember.getOwner().getBinCIType();
        if (ownerCIType.isInterface()) {
            return;
        }
        BinLocalVariable[] unusedVars = UnusedVariablesIndexer.getUnusedVariablesFor(aMember);
        int code = 0;
        for (int i = 0; i < unusedVars.length; ++i) {
            if ((!unusedVars[i].isImplied()) && (isAllowed(unusedVars[i]))) {
                handleVariable(unusedVars[i]);
            }
        }
    }

    private void handleVariable(BinLocalVariable var) {
        BinMethod method;
        BinParameter param;
        boolean isDefinitelyViolative = true;
        if (var instanceof BinParameter) {
            param = (BinParameter) var;
            method = param.getMethod();
            if (method != null && !method.isFinal() && !method.isStatic()) {
                if (method.isOverriddenOrOverrides()) {
                    foundMethods.put(method, new Integer(param.getIndex()));
                    isDefinitelyViolative = false;
                }
            }
        }
        if (isDefinitelyViolative) {
            String str = getMessage(var);
            if (str.startsWith("Unused local")) {
                addViolation(new UnusedLocalVariable(var, str));
            } else {
                addViolation(new UnusedParameter(var, str));
            }
        }
    }

    private String getMessage(BinLocalVariable var) {
        String msg;
        if (var instanceof BinParameter) {
            msg = "method parameter";
        } else if (var instanceof BinCatchParameter) {
            msg = "catch parameter";
        } else {
            msg = "local variable";
        }
        return "Unused " + msg;
    }

    private boolean isAllowed(BinVariable var) {
        if (var instanceof BinCatchParameter) {
            return !skip_catch_params;
        } else if (var instanceof BinParameter) {
            return !skip_method_params;
        } else return true;
    }

    public void postProcess() {
        BinMethod method;
        List hierarchy;
        int paramIdx;
        while (!foundMethods.keySet().isEmpty()) {
            method = (BinMethod) foundMethods.keySet().iterator().next();
            paramIdx = ((Integer) foundMethods.get(method).get(0)).intValue();
            hierarchy = method.findAllOverridesOverriddenInHierarchy();
            hierarchy.add(method);
            if (isUselessInAllHierarchy(foundMethods, hierarchy, paramIdx)) {
                BinMethod hMethod;
                for (int i = 0; i < hierarchy.size(); i++) {
                    hMethod = (BinMethod) hierarchy.get(i);
                    addViolation(new UnusedParameter(hMethod.getParameters()[paramIdx], "Globally unused method parameter"));
                }
            }
            Integer param = new Integer(paramIdx);
            for (int i = 0; i < hierarchy.size(); i++) {
                BinMethod meth = (BinMethod) hierarchy.get(i);
                foundMethods.remove(meth, param);
            }
        }
        sortViolations();
    }

    private boolean isUselessInAllHierarchy(MultiValueMap foundMethods, List hierarchy, int paramIdx) {
        boolean result = true;
        BinMethod method;
        List paramIndexes;
        for (int i = 0; i < hierarchy.size(); i++) {
            method = (BinMethod) hierarchy.get(i);
            if ((method.getCompilationUnit() != null) && (foundMethods.keySet().contains(method))) {
                if (!foundMethods.remove(method, new Integer(paramIdx))) {
                    result = false;
                    break;
                }
            } else {
                result = false;
                break;
            }
        }
        return result;
    }

    public static boolean alreadyExistsSameName(BinMethod method, String name, int idx, boolean global) {
        BinParameter[] params = getNewParameters(method.getParameters(), idx);
        if (global) {
            List hierarchy = method.findAllOverridesOverriddenInHierarchy();
            hierarchy.add(method);
            for (int i = 0; i < hierarchy.size(); i++) {
                if (((BinMethod) hierarchy.get(i)).getOwner().getBinCIType().getDeclaredMethod(name, params) != null) return true;
            }
            return false;
        } else {
            if (method.getOwner().getBinCIType().getDeclaredMethod(name, params) != null) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static BinParameter[] getNewParameters(BinParameter[] old, int idx) {
        if ((old == null) || (old.length < 2) || (idx < 0) || (idx > old.length - 1)) {
            return new BinParameter[0];
        }
        BinParameter[] newParams = new BinParameter[old.length - 1];
        for (int i = 0; i < idx; i++) {
            newParams[i] = old[i];
        }
        for (int i = idx; i < old.length - 1; i++) {
            newParams[i] = old[i + 1];
        }
        return newParams;
    }
}

class UnusedLocalVariable extends SimpleViolation {

    String message;

    public UnusedLocalVariable(BinLocalVariable var, String type) {
        super(var.getOwner(), var.getNameAstOrNull(), type, "refact.audit.unused_variable");
        setTargetItem(var);
        message = type;
    }

    public BinMember getSpecificOwnerMember() {
        BinVariable variable = (BinVariable) getTargetItem();
        return variable.getParentMember();
    }

    public BinLocalVariable getVariable() {
        return (BinLocalVariable) getTargetItem();
    }

    public List getCorrectiveActions() {
        if (message.startsWith("Unused local")) {
            return Collections.singletonList(RemoveUnusedLocalVarAction.instance);
        } else {
            return Collections.EMPTY_LIST;
        }
    }
}

class UnusedParameter extends SimpleViolation {

    String message;

    public UnusedParameter(BinLocalVariable var, String type) {
        super(var.getOwner(), var.getNameAstOrNull(), type, "refact.audit.unused_variable");
        setTargetItem(var);
        message = type;
    }

    public BinMember getSpecificOwnerMember() {
        BinVariable variable = (BinVariable) getTargetItem();
        return ((BinParameter) variable).getMethod();
    }

    public BinLocalVariable getVariable() {
        return (BinLocalVariable) getTargetItem();
    }

    public List getCorrectiveActions() {
        if (message.startsWith("Unused method") || message.startsWith("Globally")) {
            return Collections.singletonList(DeleteFromSignatureAction.INSTANCE);
        } else {
            return Collections.EMPTY_LIST;
        }
    }
}

class RemoveUnusedLocalVarAction extends MultiTargetGroupingAction {

    static final RemoveUnusedLocalVarAction instance = new RemoveUnusedLocalVarAction();

    public String getKey() {
        return "refactorit.audit.action.unused_variable.remove_unused_local";
    }

    public String getName() {
        return "Remove unused local variable";
    }

    public String getMultiTargetName() {
        return "Remove unused local variable(s)";
    }

    public Set run(TransformationManager manager, TreeRefactorItContext context, List violations) {
        Set result = new HashSet();
        Map m = new HashMap();
        for (Iterator i = violations.iterator(); i.hasNext(); ) {
            RuleViolation viol = (RuleViolation) i.next();
            if (!(viol instanceof UnusedLocalVariable)) {
                continue;
            }
            UnusedLocalVariable v = (UnusedLocalVariable) viol;
            BinLocalVariable var = v.getVariable();
            BinLocalVariableDeclaration decl = (BinLocalVariableDeclaration) var.getParent();
            ArrayList varsToRemove = (ArrayList) m.get(decl);
            if (varsToRemove == null) varsToRemove = new ArrayList();
            varsToRemove.add(var);
            m.put(decl, varsToRemove);
        }
        for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
            BinLocalVariableDeclaration decl = (BinLocalVariableDeclaration) i.next();
            List varsToRemove = (ArrayList) m.get(decl);
            BinLocalVariable[] allVars = (BinLocalVariable[]) decl.getVariables();
            String indent = FormatSettings.getIndentString(decl.getIndent());
            for (int j = 0; j < allVars.length; j++) {
                BinLocalVariable var = allVars[j];
                BinExpression be;
                String res = "";
                Comment lec;
                String modifiers = var.getTypeAndModifiersNodeText().trim();
                if (!varsToRemove.contains(var)) {
                    res = indent + (new BinModifierFormatter(var.getModifiers())).print() + " " + BinFormatter.format(var.getTypeRef()) + " " + var.getName();
                    if ((be = var.getExpression()) != null) res += " = " + be.getText() + ";";
                }
                if (j < allVars.length - 1) res += FormatSettings.LINEBREAK;
                manager.add(new StringInserter(decl.getCompilationUnit(), decl.getCompoundAst().getStartLine(), 0, res));
            }
            manager.add(new StringEraser(decl));
            result.add(decl.getCompilationUnit());
        }
        return result;
    }
}

/**
 * Changes method signature (deletes one parameter and offers rename dialog in
 * case of conflicts after signature change)
 */
class DeleteFromSignatureAction extends CorrectiveAction {

    static final DeleteFromSignatureAction INSTANCE = new DeleteFromSignatureAction();

    private int testRenameDefiner = 1;

    private boolean isGlobal = false;

    public String getKey() {
        return "refactorit.audit.action.unused_variable.delete_from_signature";
    }

    public String getName() {
        return "Remove parameter from method's hierarchy signatures";
    }

    public void setTestRun(boolean testRun) {
        super.setTestRun(testRun);
        testRenameDefiner = 1;
    }

    public boolean isMultiTargetsSupported() {
        return false;
    }

    public Set run(TreeRefactorItContext context, List violations) {
        RuleViolation violation = (RuleViolation) violations.get(0);
        if (violation instanceof UnusedParameter) {
            if (violation.getMessage().startsWith("Globally")) {
                isGlobal = true;
            } else if (violation.getMessage().startsWith("Unused method")) {
                isGlobal = false;
            } else {
                return Collections.EMPTY_SET;
            }
        } else {
            return Collections.EMPTY_SET;
        }
        CompilationUnit compilationUnit = violation.getCompilationUnit();
        BinParameter param = (BinParameter) ((UnusedParameter) violation).getVariable();
        BinMethod method = param.getMethod();
        boolean dialogResult = true;
        TransformationList transList = new TransformationList();
        if (UnusedLocalVariableRule.alreadyExistsSameName(method, method.getName(), param.getIndex(), isGlobal)) {
            RefactorItAction rename = ModuleManager.getAction(method.getClass(), RenameAction.KEY);
            RefactorItContext new_context = context.copy();
            new_context.setState(method.getName());
            do {
                transList.clear();
                if (isTestRun()) {
                    RenameMethod renRef = new RenameMethod(context, method);
                    String newMethName = method.getName() + "New" + testRenameDefiner;
                    renRef.setNewName(newMethName);
                    renRef.setRenameInJavadocs(true);
                    if (!UnusedLocalVariableRule.alreadyExistsSameName(method, newMethName, param.getIndex(), true)) {
                        transList.merge(renRef.performChange());
                        if (renRef.getStatus().isOk()) {
                            break;
                        }
                    }
                    testRenameDefiner++;
                } else {
                    dialogResult = ((RenameAction) rename).run(new_context, method, transList);
                    if ((dialogResult) && (UnusedLocalVariableRule.alreadyExistsSameName(method, ((RenameAction) rename).getNewName(), param.getIndex(), true))) {
                        RitDialog.showMessageDialog(context, "Sorry, Object with such name already exists. Try once again");
                    } else {
                        break;
                    }
                }
            } while (true);
        }
        if (dialogResult) {
            ChangeMethodSignatureRefactoring ref = new ChangeMethodSignatureRefactoring(method);
            MethodSignatureChange change = ref.createSingatureChange();
            change.deleteParameter(param.getIndex());
            ref.setChange(change);
            TransformationManager manager = new TransformationManager(null);
            manager.setShowPreview(!isTestRun());
            transList.merge(ref.performChange());
            manager.add(transList);
            final RefactoringStatus status = manager.performTransformations();
            if (status.isCancel()) {
                return Collections.EMPTY_SET;
            }
        }
        return Collections.singleton(compilationUnit);
    }
}
