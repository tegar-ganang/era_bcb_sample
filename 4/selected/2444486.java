package com.sun.tools.javac.comp;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.*;
import java.util.Iterator;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.PtolemyConstants;
import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;

public class EventInternal extends Internal {

    ListBuffer<JCTree> contractDefs;

    Attr attr;

    Symtab syms;

    Enter enter;

    public EventInternal(TreeMaker make, Names names, Attr attr, Enter enter, Symtab syms) {
        super(make, names);
        this.attr = attr;
        this.enter = enter;
        this.syms = syms;
        specCounter = 0;
        contractDefs = new ListBuffer<JCTree>();
    }

    public void generateInternalInterfaceDef(JCEventDecl tree, int pos) {
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        JCModifiers publicMods = mods(Flags.PUBLIC);
        JCModifiers publicSyntheticMods = mods(Flags.PUBLIC);
        JCModifiers privateMods = mods(Flags.PRIVATE);
        JCModifiers noMods = mods(0);
        JCModifiers publicInterfaceMods = mods(Flags.PUBLIC | Flags.INTERFACE);
        JCModifiers publicFinalMods = mods(Flags.PUBLIC | Flags.FINAL);
        JCModifiers publicStaticMods = mods(Flags.PUBLIC | Flags.STATIC);
        JCModifiers privateStaticMods = mods(Flags.PRIVATE | Flags.STATIC);
        JCModifiers privateStaticFinalMods = mods(Flags.PRIVATE | Flags.STATIC | Flags.FINAL);
        int i = 1;
        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC, lb(ann(PtolemyConstants.CONTEXT_VARIABLE_ANN_TYPE_NAME, args(assign("index", intc(i))))));
            defs.append(method(contextVariableMods, contextVariable.getName().toString(), (JCExpression) contextVariable.getType()));
            i++;
        }
        defs.append(method(publicSyntheticMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(), params(), throwing(id("Throwable"))));
        JCModifiers registerMods = mods(Flags.PUBLIC | Flags.STATIC | Flags.SYNCHRONIZED, lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));
        JCModifiers handlersChangedMods = mods(Flags.PRIVATE | Flags.STATIC | Flags.SYNCHRONIZED, lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));
        JCModifiers announceMods = mods(Flags.PUBLIC | Flags.STATIC, lb(ann(PtolemyConstants.IGNORE_REFINEMENT_CHECKING_ANN_TYPE_NAME)));
        defs.append(clazz(publicInterfaceMods, PtolemyConstants.EVENT_HANDLER_IFACE_NAME, defs(method(publicSyntheticMods, PtolemyConstants.EVENT_HANDLER_METHOD_NAME, tree.getReturnType(), params(var(noMods, "next", id(tree.getSimpleName()))), throwing(id("Throwable"))))));
        ListBuffer<JCTree> eventFrameDefs = defs(var(publicStaticMods, PtolemyConstants.HANDLERS_LIST_NAME, ta(select("java.util.List"), typeargs(id(PtolemyConstants.EVENT_HANDLER_IFACE_NAME)))), var(publicStaticMods, "cachedHandlerRecords", PtolemyConstants.EVENT_FRAME_TYPE_NAME), method(handlersChangedMods, PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME, voidt(), params(), body(var(noMods, "i", select("java.util.Iterator"), apply(PtolemyConstants.HANDLERS_LIST_NAME, "iterator")), var(noMods, "newRecords", PtolemyConstants.EVENT_FRAME_TYPE_NAME, newt(PtolemyConstants.EVENT_FRAME_TYPE_NAME, args(id("i")))), es(apply("recordWriteLock", "lock")), tryt(body(es(assign("cachedHandlerRecords", id("newRecords")))), body(es(apply("recordWriteLock", "unlock")))))), method(registerMods, PtolemyConstants.REGISTER_METHOD_NAME, PtolemyConstants.EVENT_HANDLER_IFACE_NAME, params(var(noMods, "h", PtolemyConstants.EVENT_HANDLER_IFACE_NAME)), body(ift(isNull(PtolemyConstants.HANDLERS_LIST_NAME), es(assign(PtolemyConstants.HANDLERS_LIST_NAME, apply("java.util.Collections", "synchronizedList", args(newt(ta(select("java.util.ArrayList"), typeargs(id(PtolemyConstants.EVENT_HANDLER_IFACE_NAME))), args())))))), es(apply(PtolemyConstants.HANDLERS_LIST_NAME, "add", args(id("h")))), es(apply(PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME)), returnt(id("h")))), method(registerMods, PtolemyConstants.UNREGISTER_METHOD_NAME, PtolemyConstants.EVENT_HANDLER_IFACE_NAME, params(var(noMods, "h", PtolemyConstants.EVENT_HANDLER_IFACE_NAME)), body(ift(isNull(PtolemyConstants.HANDLERS_LIST_NAME), returnt(id("h"))), ift(nott(apply(PtolemyConstants.HANDLERS_LIST_NAME, "contains", args(id("h")))), returnt(id("h"))), es(apply(PtolemyConstants.HANDLERS_LIST_NAME, "remove", args(apply(PtolemyConstants.HANDLERS_LIST_NAME, "lastIndexOf", args(id("h")))))), ift(apply(PtolemyConstants.HANDLERS_LIST_NAME, "isEmpty"), body(es(assign("cachedHandlerRecords", nullt())), es(assign(PtolemyConstants.HANDLERS_LIST_NAME, nullt()))), es(apply(PtolemyConstants.HANDLERS_CHANGED_METHOD_NAME))), returnt(id("h")))), var(privateStaticMods, "body", id(tree.getSimpleName())), method(announceMods, PtolemyConstants.ANNOUNCE_METHOD_NAME, tree.getReturnType(), params(var(noMods, "ev", id(tree.getSimpleName()))), throwing(id("Throwable")), body(var(noMods, "record", PtolemyConstants.EVENT_FRAME_TYPE_NAME), es(apply("recordReadLock", "lock")), tryt(body(es(assign("record", id("cachedHandlerRecords")))), body(es(apply("recordReadLock", "unlock")))), ift(notNull(select("record.nextRecord")), es(assign(select("EventFrame.body"), id("ev")))), isVoid(tree.getReturnType()) ? es(apply("record.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(id("record")))) : returnt(apply("record.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(id("record")))))), method(publicMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(), params(), throwing(id("Throwable")), body(ift(notNull(select("nextRecord.handler")), isVoid(tree.getReturnType()) ? body(es(apply("nextRecord.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(id("nextRecord")))), returnt()) : returnt(apply("nextRecord.handler", PtolemyConstants.EVENT_HANDLER_METHOD_NAME, args(id("nextRecord"))))), isVoid(tree.getReturnType()) ? es(apply("EventFrame.body", PtolemyConstants.INVOKE_METHOD_NAME)) : returnt(apply("EventFrame.body", PtolemyConstants.INVOKE_METHOD_NAME)))), var(privateMods, "handler", PtolemyConstants.EVENT_HANDLER_IFACE_NAME), var(privateMods, "nextRecord", PtolemyConstants.EVENT_FRAME_TYPE_NAME), constructor(privateMods, params(var(noMods, "chain", select("java.util.Iterator"))), body(es(supert()), ift(apply("chain", "hasNext"), body(es(assign("handler", cast(PtolemyConstants.EVENT_HANDLER_IFACE_NAME, apply("chain", "next")))), es(assign("nextRecord", newt(PtolemyConstants.EVENT_FRAME_TYPE_NAME, args(id("chain"))))), returnt())), es(assign("nextRecord", nullt())))), var(privateStaticFinalMods, "recordLock", select("java.util.concurrent.locks.ReentrantReadWriteLock"), newt(select("java.util.concurrent.locks.ReentrantReadWriteLock"))), var(privateStaticFinalMods, "recordReadLock", select("java.util.concurrent.locks.Lock"), apply("recordLock", "readLock")), var(privateStaticFinalMods, "recordWriteLock", select("java.util.concurrent.locks.Lock"), apply("recordLock", "writeLock")));
        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC);
            eventFrameDefs.append(method(contextVariableMods, contextVariable.getName(), (JCExpression) contextVariable.getType(), body(returnt(apply("body", contextVariable.getName().toString())))));
        }
        defs.append(clazz(publicFinalMods, PtolemyConstants.EVENT_FRAME_TYPE_NAME, implementing(id(tree.getSimpleName())), eventFrameDefs));
        ListBuffer<JCTree> eventClosureDefs = new ListBuffer<JCTree>();
        ListBuffer<JCVariableDecl> constructorParams = new ListBuffer<JCVariableDecl>();
        ListBuffer<JCStatement> constructorBodyStatements = new ListBuffer<JCStatement>();
        for (JCTree c : tree.getContextVariables()) {
            JCVariableDecl contextVariable = (JCVariableDecl) c;
            JCModifiers contextVariableMods = mods(Flags.PUBLIC | Flags.FINAL);
            eventClosureDefs.append(var(privateMods, contextVariable.getName().toString(), (JCExpression) contextVariable.getType()));
            eventClosureDefs.append(method(contextVariableMods, contextVariable.getName(), (JCExpression) contextVariable.getType(), body(returnt(id(contextVariable.getName().toString())))));
            constructorParams.append(var(noMods, contextVariable.getName().toString(), (JCExpression) contextVariable.getType()));
            constructorBodyStatements.append(es(assign(select(thist(), contextVariable.getName().toString()), id(contextVariable.getName()))));
        }
        eventClosureDefs.append(method(publicMods, PtolemyConstants.INVOKE_METHOD_NAME, tree.getReturnType(), params(), isVoid(tree.getReturnType()) ? body() : body(returnt(defaultt(tree.getReturnType())))));
        eventClosureDefs.append(constructor(publicMods, constructorParams, body(constructorBodyStatements)));
        defs.append(clazz(publicStaticMods, PtolemyConstants.EVENT_CLOSURE_TYPE_NAME, implementing(id(tree.getSimpleName())), eventClosureDefs));
        JCModifiers internalMods = mods(tree.getModifiers().flags | Flags.INTERFACE, lb(ann(PtolemyConstants.EVENT_TYPE_DECL_ANN_TYPE_NAME), ann(PtolemyConstants.EVENT_CONTRACT_DECL_ANN_TYPE_NAME, args(assign("assumesBlock", tree.contract == null ? stringc("null") : stringc(tree.contract.assumesBlock.toString()))))));
        tree.mods = internalMods;
        tree.defs = defs.toList();
    }

    public int generateSpecFields(JCEventDecl eventTree, JCEventContract tree, Env<AttrContext> predicateEnv, Env<AttrContext> assumesBlockEnv) {
        Env<AttrContext> env = tree.assumesBlock == null ? assumesBlockEnv : predicateEnv;
        JCModifiers publicStaticMods = mods(Flags.PUBLIC | Flags.STATIC);
        tree.requiresPredicateInternal = new TreeCopier<Object>(make0()).<JCExpression>copy(tree.requiresPredicate);
        attr.oldInScope = true;
        attr.attribExpr(tree.requiresPredicateInternal, env, syms.booleanType);
        attr.oldInScope = false;
        ListBuffer<JCVariableDecl> requiresOldFields = translateOldInvocationsToFields(eventTree.sym, tree.requiresPredicateInternal, true, tree.assumesBlock == null, null, assumesBlockEnv, syms, attr);
        Iterator<JCVariableDecl> requiresOldFieldsIterator = requiresOldFields.iterator();
        while (requiresOldFieldsIterator.hasNext()) {
            JCVariableDecl field = requiresOldFieldsIterator.next();
            field.mods = publicStaticMods;
            contractDefs.append(field);
            VarSymbol nextSymbol = new VarSymbol(PUBLIC, field.name, oldFieldTypes.get(field.name), eventTree.sym);
            enter.enterScope(env).enter(nextSymbol);
            if (tree.assumesBlock != null) {
                eventTree.sym.oldVariableNames = eventTree.sym.oldVariableNames.append(field.name);
                eventTree.sym.oldVariableTypes = eventTree.sym.oldVariableTypes.append(oldFieldTypes.get(field.name));
                eventTree.sym.oldVariableRealExpressions = eventTree.sym.oldVariableRealExpressions.append(oldFieldExpressions.get(field.name));
            }
        }
        tree.ensuresPredicateInternal = new TreeCopier<Object>(make0()).<JCExpression>copy(tree.ensuresPredicate);
        attr.oldInScope = true;
        attr.attribExpr(tree.ensuresPredicateInternal, env, syms.booleanType);
        attr.oldInScope = false;
        ListBuffer<JCVariableDecl> ensuresOldFields = translateOldInvocationsToFields(eventTree.sym, tree.ensuresPredicateInternal, true, tree.assumesBlock == null, null, assumesBlockEnv, syms, attr);
        Iterator<JCVariableDecl> ensuresOldFieldsIterator = ensuresOldFields.iterator();
        while (ensuresOldFieldsIterator.hasNext()) {
            JCVariableDecl field = ensuresOldFieldsIterator.next();
            boolean alreadyDefined = false;
            requiresOldFieldsIterator = requiresOldFields.iterator();
            while (requiresOldFieldsIterator.hasNext()) if (requiresOldFieldsIterator.next().name.toString().equals(field.name.toString())) alreadyDefined = true;
            if (!alreadyDefined) {
                field.mods = publicStaticMods;
                contractDefs.append(field);
                VarSymbol nextSymbol = new VarSymbol(PUBLIC, field.name, oldFieldTypes.get(field.name), eventTree.sym);
                enter.enterScope(env).enter(nextSymbol);
                if (tree.assumesBlock != null) {
                    eventTree.sym.oldVariableNames = eventTree.sym.oldVariableNames.append(field.name);
                    eventTree.sym.oldVariableTypes = eventTree.sym.oldVariableTypes.append(oldFieldTypes.get(field.name));
                    eventTree.sym.oldVariableRealExpressions = eventTree.sym.oldVariableRealExpressions.append(oldFieldExpressions.get(field.name));
                }
            }
        }
        attr.attribTree(tree, env, NIL, Type.noType);
        attr.attribExpr(tree.requiresPredicateInternal, env, syms.booleanType);
        attr.attribExpr(tree.ensuresPredicateInternal, env, syms.booleanType);
        ListBuffer<JCVariableDecl> requiresFreeVariables = freeVariables(tree.requiresPredicateInternal);
        ListBuffer<JCVariableDecl> ensuresFreeVariables = freeVariables(tree.ensuresPredicateInternal);
        if (tree.assumesBlock != null) {
            Iterator<JCVariableDecl> reqFVIterator = requiresFreeVariables.iterator();
            while (reqFVIterator.hasNext()) {
                JCVariableDecl fv = reqFVIterator.next();
                eventTree.sym.requiresFreeVariableNames = eventTree.sym.requiresFreeVariableNames.append(fv.name);
            }
            Iterator<JCVariableDecl> ensFVIterator = ensuresFreeVariables.iterator();
            while (ensFVIterator.hasNext()) {
                JCVariableDecl fv = ensFVIterator.next();
                eventTree.sym.ensuresFreeVariableNames = eventTree.sym.ensuresFreeVariableNames.append(fv.name);
            }
        }
        contractDefs.append(method(publicStaticMods, "requires" + specCounter, booleant(), requiresFreeVariables, body(returnt(tree.requiresPredicateInternal))));
        contractDefs.append(method(publicStaticMods, "ensures" + specCounter, booleant(), ensuresFreeVariables, body(returnt(tree.ensuresPredicateInternal))));
        return specCounter++;
    }

    public JCClassDecl generateContractClassDef(JCEventDecl tree, Env<AttrContext> predicateEnv, Env<AttrContext> assumesBlockEnv) {
        generateSpecFields(tree, tree.contract, predicateEnv, assumesBlockEnv);
        class SpecScanner extends TreeScanner {

            public JCEventDecl eventTree;

            public Env<AttrContext> predicateEnv;

            public Env<AttrContext> assumesBlockEnv;

            public void visitEventContract(JCEventContract tree) {
                generateSpecFields(eventTree, tree, predicateEnv, assumesBlockEnv);
            }
        }
        SpecScanner specScanner = new SpecScanner();
        specScanner.eventTree = tree;
        specScanner.predicateEnv = predicateEnv;
        specScanner.assumesBlockEnv = assumesBlockEnv;
        specScanner.scan(tree.contract.assumesBlock);
        JCModifiers contractClassMods = mods(Flags.PUBLIC | Flags.STATIC);
        JCClassDecl contractClass = clazz(contractClassMods, PtolemyConstants.CONTRACT_TYPE_NAME, contractDefs);
        tree.defs = tree.defs.append(contractClass);
        return contractClass;
    }
}
