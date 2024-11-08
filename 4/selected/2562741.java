package org.sc.tree.ssa;

import static org.sc.tree.Statements.ASSEMBLER;
import static org.sc.tree.Statements.ASSIGNMENT;
import static org.sc.tree.Statements.ASSIGNMENT_POP;
import static org.sc.tree.Statements.BIT_COMPLEMENT;
import static org.sc.tree.Statements.B_AND_EXPR;
import static org.sc.tree.Statements.B_OR_EXPR;
import static org.sc.tree.Statements.B_XOR_EXPR;
import static org.sc.tree.Statements.CALL_FUNC;
import static org.sc.tree.Statements.COMMA_LIST;
import static org.sc.tree.Statements.CONCAT_EXPR;
import static org.sc.tree.Statements.DECLARE_LIST;
import static org.sc.tree.Statements.DISCARD_STMT;
import static org.sc.tree.Statements.DIVIDE_EXPR;
import static org.sc.tree.Statements.EMPTY_STMT;
import static org.sc.tree.Statements.EQUAL;
import static org.sc.tree.Statements.FETCH_VAR;
import static org.sc.tree.Statements.FOR_IMP_ITER;
import static org.sc.tree.Statements.GREATER_EQUAL;
import static org.sc.tree.Statements.GREATER_THAN;
import static org.sc.tree.Statements.IFELSE_STMT;
import static org.sc.tree.Statements.IF_STMT;
import static org.sc.tree.Statements.LESS_EQUAL;
import static org.sc.tree.Statements.LESS_THAN;
import static org.sc.tree.Statements.L_AND_EXPR;
import static org.sc.tree.Statements.L_NOT;
import static org.sc.tree.Statements.L_OR_EXPR;
import static org.sc.tree.Statements.MINUS_EXPR;
import static org.sc.tree.Statements.MODULUS_EXPR;
import static org.sc.tree.Statements.MULTIPLY_EXPR;
import static org.sc.tree.Statements.NEW_REF;
import static org.sc.tree.Statements.NOT_EQUAL;
import static org.sc.tree.Statements.PLUS_EXPR;
import static org.sc.tree.Statements.POST_DECREMENT;
import static org.sc.tree.Statements.POST_INCREMENT;
import static org.sc.tree.Statements.PRECEDENCE_STMT;
import static org.sc.tree.Statements.PRE_DECREMENT;
import static org.sc.tree.Statements.PRE_INCREMENT;
import static org.sc.tree.Statements.PRINT_STMT;
import static org.sc.tree.Statements.PUSH_VALUE;
import static org.sc.tree.Statements.REF_CALL;
import static org.sc.tree.Statements.RET_STMT;
import static org.sc.tree.Statements.SIGN_INVERT;
import static org.sc.tree.Statements.STMT_LIST;
import static org.sc.tree.Statements.VARIABLE;
import static org.sc.tree.Statements.statementNames;
import static org.sc.tree.ssa.SSAOpTypes.ADD;
import static org.sc.tree.ssa.SSAOpTypes.ASM;
import static org.sc.tree.ssa.SSAOpTypes.ASSIGN;
import static org.sc.tree.ssa.SSAOpTypes.BIT_AND;
import static org.sc.tree.ssa.SSAOpTypes.BIT_INV;
import static org.sc.tree.ssa.SSAOpTypes.BIT_OR;
import static org.sc.tree.ssa.SSAOpTypes.BIT_XOR;
import static org.sc.tree.ssa.SSAOpTypes.CALL;
import static org.sc.tree.ssa.SSAOpTypes.CONCAT;
import static org.sc.tree.ssa.SSAOpTypes.DIVIDE;
import static org.sc.tree.ssa.SSAOpTypes.EQ;
import static org.sc.tree.ssa.SSAOpTypes.GT;
import static org.sc.tree.ssa.SSAOpTypes.GT_EQ;
import static org.sc.tree.ssa.SSAOpTypes.IFELSE;
import static org.sc.tree.ssa.SSAOpTypes.LOAD;
import static org.sc.tree.ssa.SSAOpTypes.LOGIC_INV;
import static org.sc.tree.ssa.SSAOpTypes.LOOP;
import static org.sc.tree.ssa.SSAOpTypes.LOOP_BODY;
import static org.sc.tree.ssa.SSAOpTypes.LOOP_COND;
import static org.sc.tree.ssa.SSAOpTypes.LOOP_INC;
import static org.sc.tree.ssa.SSAOpTypes.LOOP_INIT;
import static org.sc.tree.ssa.SSAOpTypes.LT;
import static org.sc.tree.ssa.SSAOpTypes.LT_EQ;
import static org.sc.tree.ssa.SSAOpTypes.MODULUS;
import static org.sc.tree.ssa.SSAOpTypes.MULTIPLY;
import static org.sc.tree.ssa.SSAOpTypes.NE;
import static org.sc.tree.ssa.SSAOpTypes.NEW;
import static org.sc.tree.ssa.SSAOpTypes.PHI;
import static org.sc.tree.ssa.SSAOpTypes.PUSH;
import static org.sc.tree.ssa.SSAOpTypes.RCALL;
import static org.sc.tree.ssa.SSAOpTypes.RET;
import static org.sc.tree.ssa.SSAOpTypes.SCOPE;
import static org.sc.tree.ssa.SSAOpTypes.SIGN_INV;
import static org.sc.tree.ssa.SSAOpTypes.SUB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sc.com.SemanticException;
import org.sc.tree.CodeLocation;
import org.sc.tree.FunctionSymbol;
import org.sc.tree.ParseTreeNode;
import org.sc.tree.ParseTreeTransformer;
import org.sc.tree.ScriptSymbol;
import org.sc.tree.Statements;
import org.sc.tree.SymbolType;
import org.sc.tree.Type;

public class SSATransformer extends ParseTreeTransformer {

    @Override
    public SSATree transform(FunctionSymbol func) throws SemanticException {
        SSATree tree = new SSATree();
        List<SSATreeNode> nodes = new ArrayList<SSATreeNode>();
        ParseTreeNode node = func.getParseTree();
        SSASymbolTable table = tree.getSymbolTable();
        CodeLocation loc = new CodeLocation(func.getFilename(), func.getLineno());
        SSAVariable[] args = new SSAVariable[func.getArgumentCount()];
        int i = 0;
        for (ScriptSymbol arg : func.getArguments()) {
            table.declare(arg.getReturnType(), arg.getName(), loc);
            args[i++] = table.write(arg.getName(), loc);
        }
        tree.setFunc(func);
        tree.setArgs(args);
        transformNode(tree, false, node, nodes);
        if (nodes.size() == 1) {
            tree.setRoot(nodes.get(0));
        } else if (nodes.size() > 1) {
            SSATreeNode root = tree.createScopeNode(nodes);
            tree.setRoot(root);
            System.err.println("W: Wrapped multiple nodes in single root ssa-node");
        }
        return tree;
    }

    public boolean isLeaf(ParseTreeNode node) {
        return isLeaf(node.getStatementType());
    }

    public boolean isLeaf(int stmt) {
        switch(stmt) {
            case PUSH_VALUE:
            case FETCH_VAR:
                return true;
        }
        return false;
    }

    public boolean isUnary(int type) {
        switch(type) {
            case LOGIC_INV:
            case BIT_INV:
            case SIGN_INV:
            case RET:
            case ASSIGN:
                return true;
        }
        return false;
    }

    protected SSAValue convertLeaf(SSASymbolTable table, ParseTreeNode leaf) throws SemanticException {
        ScriptSymbol sym = leaf.getSymbol();
        Object value;
        Type type = sym.getType();
        if (type.baseType != SymbolType.SYM_INVOKABLE) {
            type = leaf.getReturnType();
            value = sym.getValue();
        } else {
            value = sym;
        }
        if (leaf.getStatementType() == FETCH_VAR) {
            return table.read(sym.getName(), _gl(leaf));
        }
        return new SSAConstant(type, value);
    }

    protected void createOpNode(SSATree tree, boolean decl, int type, ParseTreeNode node, List<SSATreeNode> seq) throws SemanticException {
        ParseTreeNode left = node.getSubNode(0);
        ParseTreeNode right;
        SSAValue lhs, rhs;
        SSASymbolTable table = tree.getSymbolTable();
        SSATreeNode res;
        CodeLocation loc = _gl(node);
        Type retType = node.getReturnType();
        if ((type == ADD || type == SUB) && node.getSubNodeCount() == 1) {
            String varname = left.getSymbol().getName();
            lhs = table.read(varname, loc);
            res = _sl(tree.createOpNode(type, table.write(varname, loc), lhs, new SSAConstant(retType, type == ADD ? +1 : -1)), loc);
            seq.add(res);
            return;
        }
        if (isLeaf(left)) {
            lhs = convertLeaf(table, left);
        } else {
            SSATreeNode lnode;
            transformNode(tree, decl, left, seq);
            lnode = seq.get(seq.size() - 1);
            lhs = lnode.getAssign();
        }
        if (isUnary(type)) {
            SSAVariable var;
            if (type == ASSIGN) {
                var = table.write(node.getSymbol().getName(), loc);
            } else {
                var = table.makeTemp(retType);
            }
            res = tree.createOpNode(type, var, lhs);
            _sl(res, loc);
            seq.add(res);
            return;
        }
        right = node.getSubNode(1);
        if (isLeaf(right)) {
            rhs = convertLeaf(table, right);
        } else {
            SSATreeNode rnode;
            transformNode(tree, decl, right, seq);
            rnode = seq.get(seq.size() - 1);
            rhs = rnode.getAssign();
        }
        res = tree.createOpNode(type, table.makeTemp(retType), lhs, rhs);
        _sl(res, loc);
        seq.add(res);
    }

    protected void transformNode(SSATree tree, boolean decl, ParseTreeNode node, List<SSATreeNode> nodes) throws SemanticException {
        SSATreeNode ssaNode;
        int stmt = node.getStatementType();
        SSASymbolTable table = tree.getSymbolTable();
        List<SSATreeNode> children;
        CodeLocation loc = _gl(node);
        switch(stmt) {
            case PRECEDENCE_STMT:
            case DISCARD_STMT:
                transformNode(tree, decl, node.getSubNode(0), nodes);
                break;
            case VARIABLE:
            case ASSIGNMENT:
            case ASSIGNMENT_POP:
                ScriptSymbol sym = node.getSymbol();
                if (!table.has(sym.getName(), loc)) {
                    if (!decl) {
                        throw new SemanticException("Undeclared variable " + sym.getName() + " at line " + loc.lineno + " in " + loc.fname);
                    }
                    table.declare(sym.getType(), sym.getName(), loc);
                }
                if (stmt == VARIABLE) {
                    break;
                }
            case PLUS_EXPR:
            case MINUS_EXPR:
            case B_AND_EXPR:
            case B_OR_EXPR:
            case B_XOR_EXPR:
            case L_AND_EXPR:
            case L_OR_EXPR:
            case MULTIPLY_EXPR:
            case DIVIDE_EXPR:
            case MODULUS_EXPR:
            case RET_STMT:
            case SIGN_INVERT:
            case LESS_THAN:
            case LESS_EQUAL:
            case GREATER_THAN:
            case GREATER_EQUAL:
            case EQUAL:
            case NOT_EQUAL:
            case BIT_COMPLEMENT:
            case PRE_INCREMENT:
            case PRE_DECREMENT:
            case CONCAT_EXPR:
                createOpNode(tree, decl, pts2ssa(stmt), node, nodes);
                break;
            case POST_INCREMENT:
            case POST_DECREMENT:
                int type = stmt == POST_INCREMENT ? ADD : SUB;
                String varname = node.getSubNode(0).getSymbol().getName();
                Type t = node.getReturnType();
                SSAVariable v;
                v = table.read(varname, loc);
                nodes.add(_sl(tree.createOpNode(type, table.write(varname, loc), v, new SSAConstant(t, 1)), loc));
                nodes.add(_sl(tree.createOpNode(ASSIGN, table.makeTemp(t), v), loc));
                break;
            case DECLARE_LIST:
                if (decl) {
                    throw new AssertionError("Invalid parse tree");
                }
                decl = true;
            case COMMA_LIST:
            case STMT_LIST:
            case CALL_FUNC:
            case PRINT_STMT:
            case REF_CALL:
            case NEW_REF:
                final int cc = node.getSubNodeCount();
                SSAValue vars[] = null;
                if (stmt == STMT_LIST) {
                    table.pushScope();
                    children = new ArrayList<SSATreeNode>();
                } else {
                    children = nodes;
                    if (stmt == CALL_FUNC || stmt == PRINT_STMT || stmt == REF_CALL || stmt == NEW_REF) {
                        vars = new SSAValue[cc + (stmt == REF_CALL ? 1 : 0)];
                    }
                }
                for (int i = 0; i < cc; i++) {
                    ParseTreeNode sub = node.getSubNode(i);
                    if (isLeaf(sub)) {
                        SSAValue val;
                        SSATreeNode n;
                        if (stmt != CALL_FUNC && stmt != PRINT_STMT && stmt != REF_CALL && stmt != NEW_REF) {
                            throw new AssertionError("Should not meet a leaf node here");
                        }
                        val = convertLeaf(table, sub);
                        n = _sl(tree.createOpNode(val.isConst() ? PUSH : LOAD, table.makeTemp(val.getType()), val), loc);
                        nodes.add(n);
                        vars[i] = n.getAssign();
                        continue;
                    }
                    transformNode(tree, decl, sub, children);
                    if (stmt == CALL_FUNC || stmt == PRINT_STMT || stmt == REF_CALL) {
                        SSATreeNode l = children.get(children.size() - 1);
                        SSAVariable la = l.getAssign();
                        if (!table.isTemp(la)) {
                            l = _sl(tree.createOpNode(LOAD, table.makeTemp(la.getType()), la), loc);
                            nodes.add(l);
                            la = l.getAssign();
                        }
                        vars[i] = la;
                    }
                }
                if (stmt == STMT_LIST) {
                    SSAVariable[] svars = table.popScope();
                    ssaNode = tree.createScopeNode(children);
                    _sl(ssaNode, loc);
                    nodes.add(ssaNode);
                    for (SSAVariable var : svars) {
                        table.bump(var);
                    }
                } else if (stmt == CALL_FUNC || stmt == PRINT_STMT || stmt == REF_CALL || stmt == NEW_REF) {
                    FunctionSymbol func = (FunctionSymbol) node.getSymbol();
                    int ssaType;
                    switch(stmt) {
                        case CALL_FUNC:
                        case PRINT_STMT:
                            ssaType = CALL;
                            break;
                        case NEW_REF:
                            ssaType = NEW;
                            break;
                        case REF_CALL:
                            ssaType = RCALL;
                            break;
                        default:
                            throw new AssertionError();
                    }
                    ssaNode = _sl(tree.createOpNode(ssaType, table.makeTemp(node.getReturnType()), vars), loc);
                    if (stmt == REF_CALL) {
                        vars[vars.length - 1] = table.read(func.getName(), loc);
                    } else {
                        ssaNode.setFunc(func);
                    }
                    nodes.add(ssaNode);
                }
                break;
            case IF_STMT:
            case IFELSE_STMT:
                SSAValue ifcond;
                boolean hasElse = node.getSubNodeCount() > 2;
                List<SSATreeNode> ifNodes = new ArrayList<SSATreeNode>();
                List<SSATreeNode> elseNodes = new ArrayList<SSATreeNode>();
                SSAVariable[] ifVars;
                SSAVariable[] elseVars;
                SSATreeNode ifNode;
                Map<String, SSAVariable> vmap = new HashMap<String, SSAVariable>();
                if (isLeaf(node.getSubNode(0))) {
                    ifcond = convertLeaf(table, node.getSubNode(0));
                } else {
                    transformNode(tree, decl, node.getSubNode(0), nodes);
                    ifcond = nodes.get(nodes.size() - 1).getAssign();
                }
                table.pushScope();
                transformNode(tree, decl, node.getSubNode(1), ifNodes);
                ifVars = table.popScope();
                if (hasElse) {
                    table.pushScope();
                    transformNode(tree, decl, node.getSubNode(2), elseNodes);
                    elseVars = table.popScope();
                    for (SSAVariable var : elseVars) {
                        String name = table.getBasename(var);
                        vmap.put(name, var);
                    }
                }
                ifNode = _sl(tree.createOpNode(IFELSE, table.makeTemp(new Type(SymbolType.SYM_VOID)), ifcond), loc);
                ifNode.setChildren(new SSATreeNode[] { _sl(tree.createScopeNode(SCOPE, ifNodes), loc), _sl(tree.createScopeNode(SCOPE, elseNodes), loc) });
                nodes.add(ifNode);
                for (SSAVariable var : ifVars) {
                    String name = table.getBasename(var);
                    SSAVariable old = hasElse ? vmap.remove(name) : null;
                    if (old == null) {
                        old = table.read(name, loc);
                    }
                    nodes.add(_sl(tree.createOpNode(PHI, table.write(name, loc), var, old), loc));
                }
                for (SSAVariable var : vmap.values()) {
                    String name = table.getBasename(var);
                    SSAVariable old = table.read(name, loc);
                    nodes.add(_sl(tree.createOpNode(PHI, table.write(name, loc), var, old), loc));
                }
                break;
            case FOR_IMP_ITER:
                List<SSATreeNode> init = new ArrayList<SSATreeNode>();
                List<SSATreeNode> cond = new ArrayList<SSATreeNode>();
                List<SSATreeNode> inc = new ArrayList<SSATreeNode>();
                List<SSATreeNode> body = new ArrayList<SSATreeNode>();
                table = tree.getSymbolTable();
                table.pushScope();
                transformNode(tree, decl, node.getSubNode(0), init);
                transformNode(tree, decl, node.getSubNode(1), cond);
                transformNode(tree, decl, node.getSubNode(2), inc);
                transformNode(tree, decl, node.getSubNode(3), body);
                nodes.add(_sl(tree.createScopeNode(LOOP, new SSATreeNode[] { _sl(tree.createScopeNode(LOOP_INIT, init), node.getSubNode(0)), _sl(tree.createScopeNode(LOOP_COND, cond), node.getSubNode(1)), _sl(tree.createScopeNode(LOOP_INC, inc), node.getSubNode(2)), _sl(tree.createScopeNode(LOOP_BODY, body), node.getSubNode(3)) }), loc));
                vars = table.popScope();
                for (SSAVariable var : (SSAVariable[]) vars) {
                    String name = table.getBasename(var);
                    SSAVariable old = table.read(name, loc);
                    nodes.add(tree.createOpNode(PHI, table.write(name, loc), var, old));
                }
                break;
            case ASSEMBLER:
                SSAValue[] asm = new SSAValue[node.getSubNodeCount()];
                Pattern varPat = Pattern.compile("\\$\\{([^}]+)\\}");
                Map<String, Boolean> reads = new HashMap<String, Boolean>();
                Map<String, Boolean> writes = new HashMap<String, Boolean>();
                SSAVariable[] r, w;
                int i;
                table = tree.getSymbolTable();
                for (i = 0; i < asm.length; i++) {
                    String a = (String) node.getSubNode(i).getSymbol().getValue();
                    Matcher ma = varPat.matcher(a);
                    boolean store = false;
                    if (a.matches("\\S+store.*") || a.matches("^iinc.*")) {
                        store = true;
                    }
                    while (ma.find()) {
                        for (int j = 1; j <= ma.groupCount(); j++) {
                            String varName = ma.group(j);
                            Map<String, Boolean> map = store ? writes : reads;
                            if (map.get(varName) != null) {
                                continue;
                            }
                            map.put(varName, Boolean.TRUE);
                        }
                    }
                    asm[i] = new SSAConstant(new Type(SymbolType.SYM_ASM_INST), a);
                }
                ssaNode = tree.createOpNode(ASM, null, asm);
                nodes.add(ssaNode);
                r = new SSAVariable[reads.size()];
                w = new SSAVariable[writes.size()];
                i = 0;
                for (String n : reads.keySet()) {
                    r[i++] = table.read(n, loc);
                }
                i = 0;
                for (String n : writes.keySet()) {
                    SSAVariable cur = table.read(n, loc);
                    w[i] = table.write(n, loc);
                    nodes.add(_sl(tree.createOpNode(PHI, table.write(n, loc), cur, w[i]), loc));
                    ++i;
                }
                ssaNode.setAsmReads(r);
                ssaNode.setAsmWrites(w);
                _sl(ssaNode, loc);
                break;
            case EMPTY_STMT:
                break;
            default:
                throw new IllegalArgumentException("Unsupport node " + statementNames[stmt] + " (" + node.getFilename() + ":" + node.getLineno() + ")");
        }
    }

    protected CodeLocation _gl(ParseTreeNode node) {
        return new CodeLocation(node.getFilename(), node.getLineno());
    }

    protected SSATreeNode _sl(SSATreeNode ssa, CodeLocation loc) {
        return ssa.setLoc(loc);
    }

    protected SSATreeNode _sl(SSATreeNode ssa, ParseTreeNode node) {
        return ssa.setLoc(_gl(node));
    }

    protected int pts2ssa(int stmt) {
        int res;
        switch(stmt) {
            case PRE_INCREMENT:
            case PLUS_EXPR:
                res = ADD;
                break;
            case PRE_DECREMENT:
            case MINUS_EXPR:
                res = SUB;
                break;
            case MULTIPLY_EXPR:
                res = MULTIPLY;
                break;
            case DIVIDE_EXPR:
                res = DIVIDE;
                break;
            case MODULUS_EXPR:
                res = MODULUS;
                break;
            case B_AND_EXPR:
                res = BIT_AND;
                break;
            case B_OR_EXPR:
                res = BIT_OR;
                break;
            case B_XOR_EXPR:
                res = BIT_XOR;
                break;
            case BIT_COMPLEMENT:
                res = BIT_INV;
                break;
            case SIGN_INVERT:
                res = SIGN_INV;
                break;
            case L_NOT:
                res = LOGIC_INV;
                break;
            case RET_STMT:
                res = RET;
                break;
            case GREATER_THAN:
                res = GT;
                break;
            case LESS_THAN:
                res = LT;
                break;
            case GREATER_EQUAL:
                res = GT_EQ;
                break;
            case LESS_EQUAL:
                res = LT_EQ;
                break;
            case EQUAL:
                res = EQ;
                break;
            case NOT_EQUAL:
                res = NE;
                break;
            case CONCAT_EXPR:
                res = CONCAT;
                break;
            case ASSIGNMENT_POP:
            case ASSIGNMENT:
                res = ASSIGN;
                break;
            case PRINT_STMT:
            case CALL_FUNC:
                res = CALL;
                break;
            default:
                throw new IllegalArgumentException("Cannot convert " + Statements.statementNames[stmt] + " (" + stmt + ") to a SSA-type");
        }
        return res;
    }
}
