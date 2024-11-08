package edu.caece.langprocessor.syntax.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.caece.langprocessor.semantic.SymbolType;
import edu.caece.langprocessor.semantic.SymbolsTable;
import edu.caece.langprocessor.semantic.SymbolsTableItem;
import edu.caece.langprocessor.syntax.tree.nodes.AddTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.AssignmentTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.BeginTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.CallTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionEqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionEvenTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionGreaterEqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionGreaterTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionLowerEqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionLowerTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionOddTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConditionTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ConstTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.DefblockTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.DefcmdConstTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.DefcmdVarTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.DivTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.DoTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.EndOfProgramTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.EndOfStatementTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.EndTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.EqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.EvenTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ExpressionAddTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ExpressionSubsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ExpressionTermTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.FactorIdTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.FactorTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.GreaterEqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.GreaterTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.IdListTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.IdentifierTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.IfTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.LeftParenthesisTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.LowerEqualsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.LowerTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.MainBlockTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.MultTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.NumericalTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.OddTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ProcBlockTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ProcedureTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ProgTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ReadlnTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.RightParenthesisTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.SeparatorTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.SimpleTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtAssignmentTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtCallTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtIfTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtListTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtReadlnTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtStmtsBlockTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtWhileTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtWriteTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtWritelnTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StmtsBlockTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.StringTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.SubsTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.TermDivTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.TermFactorTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.TermMultTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.ThenTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.VarTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WhileTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WrParamsTailIdTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WrParamsTailTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WriteTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WritelnTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WrparamsIdTreeNode;
import edu.caece.langprocessor.syntax.tree.nodes.WrparamsStringTreeNode;

public class CodeGeneratorTreeVisitor implements TreeNodeVisitor {

    private Map<String, String> identifierToMemoryNameMapper;

    private Map<String, MemoryItem> memory;

    private StringBuilder dataSegment;

    private StringBuilder mainCode;

    private StringBuilder proceduresCode;

    private final Tree parseTree;

    private final SymbolsTable symTable;

    private NameFactory labelFactory;

    private NameFactory strNameFactory;

    private boolean inUseWriteCrLf;

    private boolean inUseWriteNum;

    private boolean inUseWriteStr;

    private boolean inUseReadln;

    public CodeGeneratorTreeVisitor(Tree parseTree, SymbolsTable table) {
        this.parseTree = parseTree;
        this.symTable = table;
        this.mainCode = new StringBuilder();
        this.proceduresCode = new StringBuilder();
        this.dataSegment = new StringBuilder();
        this.identifierToMemoryNameMapper = new HashMap<String, String>();
        this.memory = new HashMap<String, MemoryItem>();
        this.labelFactory = new NameFactory("LABEL");
        this.strNameFactory = new NameFactory("str");
        this.inUseWriteCrLf = false;
        this.inUseWriteNum = false;
        this.inUseWriteStr = false;
        this.inUseReadln = false;
    }

    public String generateCode() {
        generateNewIdentifiersNamesFromSymbolsTable(symTable, identifierToMemoryNameMapper);
        setupMemory(memory, symTable, identifierToMemoryNameMapper);
        TreeNode root = this.parseTree.getRoot();
        root.accept(this);
        generateDataSegment(memory);
        mainCode.insert(0, dataSegment);
        mainCode.append(proceduresCode.toString());
        return mainCode.toString();
    }

    private void generateDataSegment(Map<String, MemoryItem> memory) {
        dataSegment.append("DATA SEGMENT\n");
        for (MemoryItem element : memory.values()) {
            dataSegment.append(element.getName() + " ");
            dataSegment.append(element.getType() + " ");
            dataSegment.append(element.getValue() + "\n");
        }
        dataSegment.append("DATA ENDS\n");
        dataSegment.append("\nSTACK SEGMENT\nDB 200 DUP(0)\nSTACK ENDS\n");
    }

    private void generateNewIdentifiersNamesFromSymbolsTable(SymbolsTable symTable, Map<String, String> identifierToMemoryNameMapper) {
        Integer index = new Integer(0);
        for (SymbolsTableItem element : symTable.getSymbols()) {
            identifierToMemoryNameMapper.put(element.getId(), element.getId() + "_" + index.toString());
            index++;
        }
    }

    private void navigateChildren(TreeNode node) {
        if (parseTree.hasChildren(node)) {
            List<TreeNode> children = parseTree.getChildren(node);
            for (TreeNode child : children) {
                child.setCodeBuilder(node.getCodeBuilder());
                child.accept(this);
            }
        }
    }

    private String readln() {
        StringBuilder readlnCode = new StringBuilder();
        readlnCode.append("DIG MACRO DIGBASE\n");
        readlnCode.append("CMP AL, digbase\nJL inicioread\nCMP AL, '9'\n");
        readlnCode.append("JG inicioread\nMOV AH, 0Eh\nINT 10h\nMOV [BP-1], 03h\n");
        readlnCode.append("MOV CL, AL\nSUB CL, 48\nMOV AX, SI\nMOV BX, 000Ah\n");
        readlnCode.append("MUL BX\nADD AX, CX\nMOV SI, AX\nENDM\n");
        readlnCode.append("writeBS MACRO\nMOV AH, 0Eh\nINT 10h\n");
        readlnCode.append("MOV AL, ' '\nINT 10h\nMOV AL, 08h\nINT 10h\nENDM\n");
        readlnCode.append("readln PROC NEAR\nPUSH  BP\nMOV BP, SP\nSUB SP, 1\n");
        readlnCode.append("SUB SP, 1\nPUSH AX\nPUSH  BX\nPUSH CX\nPUSH DX\n");
        readlnCode.append("PUSH SI\nMOV [BP-1], 00h\nMOV [BP-2], 00h\n");
        readlnCode.append("MOV SI, 0000h\nMOV BX, 0\nMOV CX, 0\ninicioread:\n");
        readlnCode.append("MOV AH, 0\nINT 16h\nCMP [BP-1], 00h\nJE estado0\n");
        readlnCode.append("CMP [BP-1], 01h\nJE estado1\nCMP [BP-1], 02h\n");
        readlnCode.append("JE estado2\nCMP [BP-1], 03h\nJE estado3\n");
        readlnCode.append("estado0:\nCMP AL, 0Dh\nJE inicioread\n");
        readlnCode.append("CMP AL, '0'\nJNE estado0a\nMOV [BP-1], 01h\n");
        readlnCode.append("MOV AH, 0Eh\nINT 10h\n");
        readlnCode.append("JMP inicioread\nestado0a:\n");
        readlnCode.append("CMP AL, '-'\nJNE estado0b\nCMP [BP+4], 0000h\n");
        readlnCode.append("JE inicioread\nMOV [BP-1], 02h\nMOV [BP-2], 01h\n");
        readlnCode.append("MOV AH, 0Eh\nINT 10h\nJMP  inicioread\n");
        readlnCode.append("estado0b:\nDIG '1'\nJMP  inicioread\n");
        readlnCode.append("estado1:\nCMP AL, 0Dh\n");
        readlnCode.append("JE finread\nCMP AL, 08h\nJNE inicioread\n");
        readlnCode.append("writeBS\nMOV [BP-1], 00h\nJMP inicioread\n");
        readlnCode.append("estado2:\nCMP AL, 0Dh\nJE inicioread\n");
        readlnCode.append("CMP AL, 08h\nJNE estado2a\nwriteBS\n");
        readlnCode.append("MOV [BP-1], 00h\nMOV [BP-2], 00h\nJMP inicioread\n");
        readlnCode.append("estado2a:\nDIG '1'\n");
        readlnCode.append("JMP inicioread\nestado3:\nCMP AL, 0Dh\n");
        readlnCode.append("JE finread\nCMP AL, 08h\nJNE estado3a\n");
        readlnCode.append("writeBS\nMOV AX, SI\nMOV dx, 0\n");
        readlnCode.append("MOV BX, 000Ah\nDIV BX\nMOV SI, AX\n");
        readlnCode.append("CMP SI, 0\nJNE inicioread\nCMP [BP-2], 00h\n");
        readlnCode.append("JNE estado3bs1\nMOV [BP-1], 00h\nJMP inicioread\n");
        readlnCode.append("estado3bs1:\nMOV [BP-1], 02h\nJMP inicioread\n");
        readlnCode.append("estado3a:\nDIG '0'\nJMP inicioread\n");
        readlnCode.append("\nfinread:\nCMP [BP-2], 00h\n");
        readlnCode.append("JE finread2\nNEG SI\nfinread2:\nMOV [BP+6], SI\n");
        readlnCode.append("POP SI\nPOP DX\nPOP CX\nPOP BX\nPOP AX\n");
        readlnCode.append("MOV SP, BP\nPOP BP\nRET 2\n");
        readlnCode.append("readln ENDP\n");
        return readlnCode.toString();
    }

    private void resolveBinaryOperationChildren(TreeNode node) {
        List<TreeNode> children = parseTree.getChildren(node);
        TreeNode item1 = children.get(0);
        item1.setCodeBuilder(node.getCodeBuilder());
        TreeNode item2 = children.get(2);
        item2.setCodeBuilder(node.getCodeBuilder());
        item2.accept(this);
        item1.setCodeBuilder(node.getCodeBuilder());
        item1.accept(this);
        node.getCodeBuilder().append("POP AX\n");
        node.getCodeBuilder().append("POP BX\n");
    }

    private void resolveCompare(TreeNode node) {
        resolveBinaryOperationChildren(node);
        node.getCodeBuilder().append("CMP AX, BX\n");
    }

    private void resolveUnaryOperation(TreeNode node) {
        List<TreeNode> children = parseTree.getChildren(node);
        children.get(1).setCodeBuilder(node.getCodeBuilder());
        children.get(1).accept(this);
        node.getCodeBuilder().append("POP AX\n");
    }

    private void setupMemory(Map<String, MemoryItem> memory, SymbolsTable table, Map<String, String> identifierToMemoryNameMapper) {
        for (SymbolsTableItem item : symTable.getSymbols()) {
            if (item.getType().equals(SymbolType.constType) || item.getType().equals(SymbolType.varType)) {
                String id = identifierToMemoryNameMapper.get(item.getId());
                MemoryItem memItem = new MemoryItem(id, "DW", "0");
                memory.put(id, memItem);
            }
        }
    }

    public void visitAddTreeNode(AddTreeNode node) {
    }

    public void visitAssignmentTreeNode(AssignmentTreeNode node) {
    }

    public void visitBeginTreeNode(BeginTreeNode node) {
    }

    public void visitCallTreeNode(CallTreeNode node) {
    }

    public void visitConditionEqualsTreeNode(ConditionEqualsTreeNode node) {
        resolveCompare(node);
        node.getCodeBuilder().append("JNE ");
    }

    public void visitConditionEvenTreeNode(ConditionEvenTreeNode node) {
        resolveUnaryOperation(node);
        node.getCodeBuilder().append("SHR AX, 1\n");
        node.getCodeBuilder().append("JC ");
    }

    public void visitConditionGreaterEqualsTreeNode(ConditionGreaterEqualsTreeNode node) {
        resolveCompare(node);
        node.getCodeBuilder().append("JNGE ");
    }

    public void visitConditionGreaterTreeNode(ConditionGreaterTreeNode node) {
        resolveCompare(node);
        node.getCodeBuilder().append("JNG ");
    }

    public void visitConditionLowerEqualsTreeNode(ConditionLowerEqualsTreeNode node) {
        resolveCompare(node);
        node.getCodeBuilder().append("JNLE ");
    }

    public void visitConditionLowerTreeNode(ConditionLowerTreeNode node) {
        resolveCompare(node);
        node.getCodeBuilder().append("JNL ");
    }

    public void visitConditionOddTreeNode(ConditionOddTreeNode node) {
        resolveUnaryOperation(node);
        node.getCodeBuilder().append("SHR AX, 1\n");
        node.getCodeBuilder().append("JNC ");
    }

    public void visitConditionTreeNode(ConditionTreeNode node) {
        navigateChildren(node);
    }

    public void visitConstTreeNode(ConstTreeNode node) {
    }

    public void visitDefblockTreeNode(DefblockTreeNode node) {
        navigateChildren(node);
    }

    public void visitDefcmdConstTreeNode(DefcmdConstTreeNode node) {
        List<TreeNode> children = parseTree.getChildren(node);
        String constValue = children.get(3).toString();
        String id = identifierToMemoryNameMapper.get(children.get(1).getGrammarItem().toString());
        MemoryItem item = this.memory.get(id);
        item.setValue(constValue);
    }

    public void visitDefcmdVarTreeNode(DefcmdVarTreeNode node) {
    }

    public void visitDivTreeNode(DivTreeNode node) {
    }

    public void visitDoTreeNode(DoTreeNode node) {
    }

    public void visitEndOfProgramTreeNode(EndOfProgramTreeNode node) {
        node.getCodeBuilder().append("MOV AX, 4C00H\nINT 21h\n");
    }

    public void visitEndOfStatementTreeNode(EndOfStatementTreeNode node) {
    }

    public void visitEndTreeNode(EndTreeNode node) {
    }

    public void visitEqualsTreeNode(EqualsTreeNode node) {
    }

    public void visitEvenTreeNode(EvenTreeNode node) {
    }

    public void visitExpressionAddTreeNode(ExpressionAddTreeNode node) {
        resolveBinaryOperationChildren(node);
        node.getCodeBuilder().append("ADD AX, BX\n");
        node.getCodeBuilder().append("PUSH AX\n");
    }

    public void visitExpressionSubsTreeNode(ExpressionSubsTreeNode node) {
        resolveBinaryOperationChildren(node);
        node.getCodeBuilder().append("SUB AX, BX\n");
        node.getCodeBuilder().append("PUSH AX\n");
    }

    public void visitExpressionTermTreeNode(ExpressionTermTreeNode node) {
        navigateChildren(node);
    }

    public void visitFactorIdTreeNode(FactorIdTreeNode node) {
        String id = parseTree.getChildren(node).get(0).getGrammarItem().toString();
        node.getCodeBuilder().append("PUSH ");
        node.getCodeBuilder().append(identifierToMemoryNameMapper.get(id) + "\n");
    }

    public void visitFactorTreeNode(FactorTreeNode node) {
        navigateChildren(node);
    }

    public void visitGreaterEqualsTreeNode(GreaterEqualsTreeNode node) {
    }

    public void visitGreaterTreeNode(GreaterTreeNode node) {
    }

    public void visitIdentifierTreeNode(IdentifierTreeNode node) {
    }

    public void visitIdListTreeNode(IdListTreeNode node) {
    }

    public void visitIfTreeNode(IfTreeNode node) {
    }

    public void visitLeftParenthesisTreeNode(LeftParenthesisTreeNode node) {
    }

    public void visitLoweEqualsTreeNode(LowerEqualsTreeNode node) {
    }

    public void visitLowerTreeNode(LowerTreeNode node) {
    }

    public void visitMainBlockTreeNode(MainBlockTreeNode node) {
        node.setCodeBuilder(mainCode);
        node.getCodeBuilder().append("MAIN PROC FAR\n");
        node.getCodeBuilder().append("MOV AX, DATA\n");
        node.getCodeBuilder().append("MOV DS, AX\n");
        navigateChildren(node);
        node.getCodeBuilder().append("MAIN ENDP\n");
    }

    public void visitMultTreeNode(MultTreeNode node) {
    }

    public void visitNumericalTreeNode(NumericalTreeNode node) {
        node.getCodeBuilder().append("PUSH " + node.getGrammarItem().toString() + "\n");
    }

    public void visitOddTreeNode(OddTreeNode node) {
    }

    public void visitProcblockTreeNode(ProcBlockTreeNode node) {
        node.setCodeBuilder(proceduresCode);
        if (parseTree.hasChildren(node)) {
            List<TreeNode> children = parseTree.getChildren(node);
            TreeNode procName = children.get(1);
            TreeNode defBlock = children.get(2);
            TreeNode stmtsBlock = children.get(4);
            TreeNode procBlock = children.get(6);
            String strProcName = procName.getGrammarItem().getValue();
            node.getCodeBuilder().append("\n" + strProcName + " PROC NEAR\n");
            node.getCodeBuilder().append("PUSH AX\n");
            node.getCodeBuilder().append("PUSH BX\n");
            node.getCodeBuilder().append("PUSH CX\n");
            node.getCodeBuilder().append("PUSH DX\n");
            defBlock.setCodeBuilder(node.getCodeBuilder());
            defBlock.accept(this);
            stmtsBlock.setCodeBuilder(node.getCodeBuilder());
            stmtsBlock.accept(this);
            node.getCodeBuilder().append("POP DX\n");
            node.getCodeBuilder().append("POP CX\n");
            node.getCodeBuilder().append("POP BX\n");
            node.getCodeBuilder().append("POP AX\n");
            node.getCodeBuilder().append("RET\n");
            node.getCodeBuilder().append(strProcName + " ENDP\n");
            procBlock.setCodeBuilder(node.getCodeBuilder());
            procBlock.accept(this);
        }
    }

    public void visitProcedureTreeNode(ProcedureTreeNode node) {
    }

    public void visitProgTreeNode(ProgTreeNode node) {
        mainCode.append("\nCODE SEGMENT\n");
        mainCode.append("ASSUME CS:CODE,DS:DATA,SS:STACK\n");
        navigateChildren(node);
        if (inUseWriteCrLf) proceduresCode.append("\n" + writeCrLf());
        if (inUseWriteNum) proceduresCode.append("\n" + writeNum());
        if (inUseWriteStr) proceduresCode.append("\n" + writeStr());
        if (inUseReadln) proceduresCode.append("\n" + readln());
        proceduresCode.append("CODE ENDS\n");
    }

    public void visitReadlnTreeNode(ReadlnTreeNode node) {
    }

    public void visitRightParenthesisTreeNode(RightParenthesisTreeNode node) {
    }

    public void visitSeparatorTreeNode(SeparatorTreeNode node) {
    }

    public void visitSimpleTreeNode(SimpleTreeNode node) {
        navigateChildren(node);
    }

    public void visitStmsCallTreeNode(StmtCallTreeNode node) {
        List<TreeNode> children = parseTree.getChildren(node);
        String procName = children.get(1).getGrammarItem().getValue();
        node.getCodeBuilder().append("CALL " + procName + "\n");
    }

    public void visitStmtAssignmentTreeNode(StmtAssignmentTreeNode node) {
        if (parseTree.hasChildren(node)) {
            List<TreeNode> children = parseTree.getChildren(node);
            String identifier = identifierToMemoryNameMapper.get(children.get(0).getGrammarItem().getValue());
            children.get(2).setCodeBuilder(node.getCodeBuilder());
            children.get(2).accept(this);
            node.getCodeBuilder().append("POP " + identifier + "\n");
        }
    }

    public void visitStmtIfTreeNode(StmtIfTreeNode node) {
        String label = labelFactory.getLabel();
        List<TreeNode> children = parseTree.getChildren(node);
        children.get(1).setCodeBuilder(node.getCodeBuilder());
        children.get(1).accept(this);
        node.getCodeBuilder().append(label + "\n");
        children.get(3).setCodeBuilder(node.getCodeBuilder());
        children.get(3).accept(this);
        node.getCodeBuilder().append(label + ": ");
    }

    public void visitStmtListTreeNode(StmtListTreeNode node) {
        navigateChildren(node);
    }

    public void visitStmtReadlnTreeNode(StmtReadlnTreeNode node) {
        inUseReadln = true;
        node.getCodeBuilder().append("PUSH 0000h\n");
        node.getCodeBuilder().append("PUSH 0001h\n");
        node.getCodeBuilder().append("CALL readln\n");
        node.getCodeBuilder().append("POP AX\n");
        List<TreeNode> children = parseTree.getChildren(node);
        String id = children.get(1).getGrammarItem().getValue();
        id = identifierToMemoryNameMapper.get(id);
        node.getCodeBuilder().append("MOV " + id + ", AX\n");
    }

    public void visitStmtsBlockTreeNode(StmtsBlockTreeNode node) {
        navigateChildren(node);
    }

    public void visitStmtStmtsBlockTreeNode(StmtStmtsBlockTreeNode node) {
        navigateChildren(node);
    }

    public void visitStmtWhileTreeNode(StmtWhileTreeNode node) {
        String labelCondition = labelFactory.getLabel();
        List<TreeNode> children = parseTree.getChildren(node);
        node.getCodeBuilder().append(labelCondition + ": ");
        children.get(1).setCodeBuilder(node.getCodeBuilder());
        children.get(1).accept(this);
        String labelEnd = labelFactory.getLabel();
        node.getCodeBuilder().append(labelEnd + "\n");
        children.get(3).setCodeBuilder(node.getCodeBuilder());
        children.get(3).accept(this);
        node.getCodeBuilder().append("JMP " + labelCondition + "\n");
        node.getCodeBuilder().append(labelEnd + ": ");
    }

    public void visitStmtWritelnTreeNode(StmtWritelnTreeNode node) {
        inUseWriteCrLf = true;
        navigateChildren(node);
        node.getCodeBuilder().append("CALL writeCRLF\n");
    }

    public void visitStmtWriteTreeNode(StmtWriteTreeNode node) {
        inUseWriteNum = true;
        List<TreeNode> children = parseTree.getChildren(node);
        String id = children.get(1).getGrammarItem().getValue();
        id = identifierToMemoryNameMapper.get(id);
        node.getCodeBuilder().append("PUSH 0000h \n");
        node.getCodeBuilder().append("PUSH " + id + "\n");
        node.getCodeBuilder().append("CALL writeNUM\n");
    }

    public void visitStringTreeNode(StringTreeNode node) {
    }

    public void visitSubsTreeNode(SubsTreeNode node) {
    }

    public void visitTermDivTreeNode(TermDivTreeNode node) {
        resolveBinaryOperationChildren(node);
        node.getCodeBuilder().append("DIV BX\n");
        node.getCodeBuilder().append("PUSH AX\n");
    }

    public void visitTermFactorTreeNode(TermFactorTreeNode node) {
        navigateChildren(node);
    }

    public void visitTermMultTreeNode(TermMultTreeNode node) {
        resolveBinaryOperationChildren(node);
        node.getCodeBuilder().append("MUL BX\n");
        node.getCodeBuilder().append("PUSH AX\n");
    }

    public void visitThenTreeNode(ThenTreeNode node) {
    }

    public void visitVarTreeNode(VarTreeNode node) {
    }

    public void visitWhileTreeNode(WhileTreeNode node) {
    }

    public void visitWritelnTreeNode(WritelnTreeNode node) {
    }

    public void visitWriteTreeNode(WriteTreeNode node) {
    }

    public void visitWrparamsIdTreeNode(WrparamsIdTreeNode node) {
        inUseWriteNum = true;
        List<TreeNode> children = parseTree.getChildren(node);
        String id = children.get(0).getGrammarItem().getValue();
        id = identifierToMemoryNameMapper.get(id);
        node.getCodeBuilder().append("PUSH 0000h \n");
        node.getCodeBuilder().append("PUSH " + id + "\n");
        node.getCodeBuilder().append("CALL writeNUM\n");
    }

    public void visitWrparamsStringTreeNode(WrparamsStringTreeNode node) {
        inUseWriteStr = true;
        List<TreeNode> children = parseTree.getChildren(node);
        String strValue = children.get(0).getGrammarItem().getValue();
        String strName = strNameFactory.getLabel();
        MemoryItem item = new MemoryItem(strName, "DB", strValue);
        this.memory.put(strName, item);
        node.getCodeBuilder().append("PUSH OFFSET " + strName + " \n");
        node.getCodeBuilder().append("PUSH " + strValue.length() + " \n");
        node.getCodeBuilder().append("CALL writeSTR \n");
        children.get(2).setCodeBuilder(node.getCodeBuilder());
        children.get(2).accept(this);
    }

    public void visitWrParamsTailIdTreeNode(WrParamsTailIdTreeNode node) {
        inUseWriteNum = true;
        List<TreeNode> children = parseTree.getChildren(node);
        String id = children.get(0).getGrammarItem().getValue();
        id = identifierToMemoryNameMapper.get(id);
        node.getCodeBuilder().append("PUSH 0000h \n");
        node.getCodeBuilder().append("PUSH " + id + "\n");
        node.getCodeBuilder().append("CALL writeNUM\n");
        children.get(2).setCodeBuilder(node.getCodeBuilder());
        children.get(2).accept(this);
    }

    public void visitWrParamsTailTreeNode(WrParamsTailTreeNode node) {
        navigateChildren(node);
    }

    private String writeCrLf() {
        StringBuilder writeCRLF = new StringBuilder();
        writeCRLF.append("writeCRLF PROC NEAR\n");
        writeCRLF.append("PUSH AX\nMOV AL, 0Dh\nMOV AH, 0Eh\nINT 10h\n");
        writeCRLF.append("MOV AL, 0Ah\nMOV AH, 0Eh\nINT 10h\n");
        writeCRLF.append("POP AX\nRET\n");
        writeCRLF.append("writeCRLF ENDP\n");
        return writeCRLF.toString();
    }

    private String writeNum() {
        StringBuilder writeNum = new StringBuilder();
        writeNum.append("writeNUM PROC NEAR\nPUSH BP\nMOV BP, SP\n");
        writeNum.append("SUB SP, 1\nSUB SP, 6\nPUSH AX\nPUSH BX\nPUSH CX\n");
        writeNum.append("PUSH DX\nPUSH SI\nMOV [BP-1], 00h\nMOV AX, [BP+4]\n");
        writeNum.append("CMP [BP+6], 0\nJE comenzar\nCMP AX, 0\nJGE comenzar\n");
        writeNum.append("NEG AX\nMOV [BP-1], 01h\ncomenzar:\nMOV BX, 10\n");
        writeNum.append("MOV CX, 0\nMOV SI, BP\nSUB SI, 8\nproxdiv:\nDEC SI\n");
        writeNum.append("XOR DX,DX\nDIV BX\nADD dl, 48\nMOV [SI], dl\nINC CX\n");
        writeNum.append("CMP AX, 0\nJNZ proxdiv\nCMP [BP-1], 00h\nJZ mostrar\n");
        writeNum.append("DEC SI\nMOV [SI], '-'\nINC CX\nmostrar:\nPUSH SI\n");
        writeNum.append("PUSH CX\nCALL writeSTR\nPOP SI\nPOP DX\nPOP CX\n");
        writeNum.append("POP BX\nPOP AX\nMOV SP, BP\nPOP BP\n");
        writeNum.append("RET 4\nwriteNUM ENDP\n");
        return writeNum.toString();
    }

    private String writeStr() {
        StringBuilder writeStr = new StringBuilder();
        writeStr.append("writeSTR PROC NEAR \n");
        writeStr.append("PUSH  BP\nMOV BP, SP\nPUSH AX\n");
        writeStr.append("PUSH BX\nPUSH CX\nPUSH SI\n");
        writeStr.append("MOV SI, [BP+6]\nMOV CX, [BP+4]\n");
        writeStr.append("XOR BX, BX\n");
        writeStr.append("loop:\nMOV AL, [SI]\nMOV AH, 0Eh\n");
        writeStr.append("INT 10h\nINC BX\n");
        writeStr.append("INC SI\nCMP BX, CX\nJNE loop\nPOP SI\n");
        writeStr.append("POP CX\nPOP BX\nPOP AX\n");
        writeStr.append("POP BP\nRET 4\n");
        writeStr.append("writeSTR ENDP \n");
        return writeStr.toString();
    }
}
