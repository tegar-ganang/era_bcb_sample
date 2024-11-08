package org.infozone.tools.janalyzer;

import koala.dynamicjava.parser.wrapper.*;
import koala.dynamicjava.tree.*;
import java.util.List;
import java.util.ListIterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * This class parses a existing JAVA class and format's the source code
 * after the Code Conventions from Sun and therefore from the Apache Software Foundation.
 * It uses the Syntax tree from the DynamicJava SourceCodeParser and transform each node of
 * this tree back into lines of code.
 * All output is handled by the JavaCodeOutput class. Also comments only handled by the Output
 * class, because they don't exist in a syntax tree.
 *
 *
 * JCC in the comment stands for
 * <a href=http://xml.apache.org/source.html>Coding Conventions</a>.
 *
 * <pre>
 *
 * TODO:
 *  - forgotten expressions they not used in the prowler package
 *  - indentation in nested conditional operator statements
 *  - comments after the last expression in a method and the trailing } are deleted!!!
 *  - split level in binary expressions
 *  - label:
 *  - more documentation
 *  - quality checking such:
 *     - constants as ABC_DEF
 *  - JavaCodeMetricManager
 *  - metric'S
 *     - method counting and so on
 *
 * Problems:
 *  - trailling comments can't be inserted after the automatically wrapped lines, so
 *      they are inserted before the statements
 *  - At this moment only the trailling comment on the same line as the statement starts
 *      is checked
 * </pre>
 *
 * @version $Revision: 1.2 $ $Date: 2004/01/06 20:06:06 $
 * @author <a href="http://www.softwarebuero.de">SMB</a>
 */
public final class JavaCodeAnalyzer extends java.lang.Object {

    /** The string for concating several smaller output strings to one line. */
    private String outLine = "";

    /** The output class */
    private JavaCodeOutput jco;

    /** The expression helper class */
    private ExpressionHelper eh;

    /** The level to help correct parentheses to binary expressions */
    private int currentBinaryExpressionLevel = 0;

    /** Source File is a interface Declaration, so methods doesnt have a body */
    private boolean isInterface = false;

    public JavaCodeAnalyzer(String filenameIn, String filenameOut, String lineLength) {
        try {
            File tmp = File.createTempFile("JavaCodeAnalyzer", "tmp");
            BufferedReader br = new BufferedReader(new FileReader(filenameIn));
            BufferedWriter out = new BufferedWriter(new FileWriter(tmp));
            while (br.ready()) {
                out.write(br.read());
            }
            br.close();
            out.close();
            jco = new JavaCodeOutput(tmp, filenameOut, lineLength);
            SourceCodeParser p = new JavaCCParserFactory().createParser(new FileReader(tmp), null);
            List statements = p.parseCompilationUnit();
            ListIterator it = statements.listIterator();
            eh = new ExpressionHelper(this, jco);
            Node n;
            printLog("Parsed file " + filenameIn + "\n");
            while (it.hasNext()) {
                n = (Node) it.next();
                parseObject(n);
            }
            tmp.delete();
        } catch (Exception e) {
            System.err.println(getClass() + ": " + e);
        }
    }

    /**
     * This method parses each Node in the syntax tree.
     * The String out is used for concatenation of the whole single strings.
     *
     * Errors:
     *   On FieldDeclaration: only one declaration per line is in the tree.
     *			JCC 6.1
     *
     */
    protected void parseObject(Node aNode) {
        ListIterator it;
        jco.setCommentEnd(aNode.getBeginLine());
        printLog(aNode, "parseObject" + aNode);
        if (aNode instanceof PackageDeclaration) {
            jco.printPackageComment(aNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut("package " + jco.getHighSplitLevel() + ((PackageDeclaration) aNode).getName() + ";");
            printOut();
            return;
        }
        if (aNode instanceof ImportDeclaration) {
            jco.printImportComment(aNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut("import " + jco.getHighSplitLevel() + ((ImportDeclaration) aNode).getName());
            if (((ImportDeclaration) aNode).isPackage()) {
                addToOut(".*");
                printLog(aNode, "import statement with *");
            }
            printOut(";");
            return;
        }
        if (aNode instanceof ClassDeclaration) {
            ClassDeclaration classNode = (ClassDeclaration) aNode;
            jco.printClassComment(classNode);
            jco.setCommentStart(aNode.getBeginLine());
            isInterface = false;
            setToOut(ConstantsManager.getModifierString(classNode.getAccessFlags()));
            addToOut(jco.getLowSplitLevel() + "class " + classNode.getName() + " " + jco.getHighSplitLevel() + "extends " + classNode.getSuperclass());
            List interfaces = classNode.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                it = interfaces.listIterator();
                addToOut(" " + jco.getHighSplitLevel() + "implements ");
                while (it.hasNext()) {
                    addToOut(it.next() + "," + jco.getMiddleSplitLevel() + " ");
                }
                if (getOut().endsWith("," + jco.getMiddleSplitLevel() + " ")) {
                    setToOut(getOut().substring(0, getOut().length() - ("," + jco.getMiddleSplitLevel() + " ").length()));
                }
            }
            printOut(" {");
            it = classNode.getMembers().listIterator();
            jco.increaseIndent();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            jco.decreaseIndent();
            printOut("}");
            return;
        }
        if (aNode instanceof InterfaceDeclaration) {
            InterfaceDeclaration interfaceNode = (InterfaceDeclaration) aNode;
            jco.printInterfaceComment(interfaceNode);
            jco.setCommentStart(aNode.getBeginLine());
            isInterface = true;
            setToOut(ConstantsManager.getModifierString(interfaceNode.getAccessFlags()));
            addToOut(jco.getLowSplitLevel() + "interface " + interfaceNode.getName() + " ");
            List interfaces = interfaceNode.getInterfaces();
            if (interfaces != null && !interfaces.isEmpty()) {
                it = interfaces.listIterator();
                addToOut(jco.getHighSplitLevel() + "extends ");
                while (it.hasNext()) {
                    addToOut(it.next() + "," + jco.getMiddleSplitLevel() + " ");
                }
                if (getOut().endsWith("," + jco.getMiddleSplitLevel() + " ")) {
                    setToOut(getOut().substring(0, getOut().length() - ("," + jco.getMiddleSplitLevel() + " ").length()));
                }
            }
            printOut(" {");
            it = interfaceNode.getMembers().listIterator();
            jco.increaseIndent();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            jco.decreaseIndent();
            printOut("}");
            return;
        }
        if (aNode instanceof FieldDeclaration) {
            FieldDeclaration fieldNode = (FieldDeclaration) aNode;
            jco.printFieldComment(fieldNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut(ConstantsManager.getModifierString(fieldNode.getAccessFlags()));
            addToOut(eh.getTypeString(fieldNode.getType()) + " ");
            addToOut(fieldNode.getName());
            if (fieldNode.getInitializer() != null) {
                addToOut(" = " + jco.getHighSplitLevel());
                eh.addSuperConditionString(fieldNode.getInitializer());
            }
            addToOut(";");
            printOut();
            return;
        }
        if (aNode instanceof VariableDeclaration) {
            VariableDeclaration varNode = (VariableDeclaration) aNode;
            jco.printVariableComment(varNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut("");
            if (varNode.isFinal()) {
                addToOut("final ");
            }
            addToOut(eh.getTypeString(varNode.getType()) + " ");
            addToOut(varNode.getName());
            if (varNode.getInitializer() != null) {
                addToOut(" = " + jco.getHighSplitLevel());
                eh.addSuperConditionString(varNode.getInitializer());
            }
            addToOut(";");
            printOut();
            return;
        }
        if (aNode instanceof MethodDeclaration) {
            MethodDeclaration methodNode = (MethodDeclaration) aNode;
            jco.printMethodComment(methodNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut(ConstantsManager.getModifierString(methodNode.getAccessFlags()));
            addToOut(eh.getTypeString(methodNode.getReturnType()) + " ");
            addToOut(methodNode.getName() + "(" + getParametersString(methodNode.getParameters()) + ")");
            it = methodNode.getExceptions().listIterator();
            if (it.hasNext()) {
                addToOut(" " + jco.getHighSplitLevel() + "throws");
            }
            while (it.hasNext()) {
                addToOut(" " + (String) it.next() + "," + jco.getMiddleSplitLevel());
            }
            if (getOut().endsWith("," + jco.getMiddleSplitLevel())) {
                setToOut(getOut().substring(0, getOut().length() - ("," + jco.getMiddleSplitLevel()).length()));
            }
            if (ConstantsManager.getModifierString(methodNode.getAccessFlags()).indexOf("abstract") == -1 && !isInterface) {
                parseObject(methodNode.getBody());
                jco.setCommentEnd(aNode.getEndLine());
                jco.printComment();
                jco.setCommentStart(aNode.getEndLine());
                printOut();
            } else {
                addToOut(";");
                printOut();
            }
            return;
        }
        if (aNode instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorNode = (ConstructorDeclaration) aNode;
            jco.printConstructorComment(constructorNode);
            jco.setCommentStart(aNode.getBeginLine());
            setToOut(ConstantsManager.getModifierString(constructorNode.getAccessFlags()));
            addToOut(constructorNode.getName() + "(" + getParametersString(constructorNode.getParameters()) + ") ");
            it = constructorNode.getExceptions().listIterator();
            if (it.hasNext()) {
                addToOut(jco.getHighSplitLevel() + "throws ");
            }
            while (it.hasNext()) {
                addToOut((String) it.next() + "," + jco.getMiddleSplitLevel() + " ");
            }
            if (getOut().endsWith("," + jco.getMiddleSplitLevel() + " ")) {
                setToOut(getOut().substring(0, getOut().length() - ("," + jco.getMiddleSplitLevel() + " ").length()));
            }
            addToOut("{");
            printOut();
            jco.increaseIndent();
            if (constructorNode.getConstructorInvocation() != null) {
                parseObject(constructorNode.getConstructorInvocation());
            }
            it = constructorNode.getStatements().listIterator();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            jco.decreaseIndent();
            printOut("}");
            return;
        }
        if (aNode instanceof ConstructorInvocation) {
            ConstructorInvocation ci = (ConstructorInvocation) aNode;
            if (ci.isSuper()) {
                setToOut("super(");
            } else {
                setToOut("this(");
            }
            eh.addConditionListString(ci.getArguments());
            printOut(");");
            return;
        }
        if (aNode instanceof ClassInitializer) {
            ClassInitializer ci = (ClassInitializer) aNode;
            jco.printComment();
            jco.setCommentStart(aNode.getBeginLine());
            setToOut("static");
            if (ci.getBlock() != null) {
                parseObject(ci.getBlock());
            }
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            printOut();
            return;
        }
        if (aNode instanceof BlockStatement) {
            printOut(" {");
            jco.increaseIndent();
            it = ((BlockStatement) aNode).getStatements().listIterator();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            jco.decreaseIndent();
            setToOut("} ");
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            return;
        }
        jco.printComment();
        jco.setCommentStart(aNode.getBeginLine());
        if (aNode instanceof EmptyStatement) {
            printLog(aNode, "empty statement.");
            printOut(";");
            return;
        }
        if (aNode instanceof ReturnStatement) {
            setToOut("return");
            if (((ReturnStatement) aNode).getExpression() != null) {
                addToOut(" " + jco.getMiddleSplitLevel());
                eh.addSuperConditionString(((ReturnStatement) aNode).getExpression());
            }
            addToOut(";");
            printOut();
            return;
        }
        if (aNode instanceof IfThenElseStatement) {
            setToOut("if (");
            eh.addSuperConditionString(((IfThenElseStatement) aNode).getCondition());
            addToOut(")");
            insertBlockStatement(((IfThenElseStatement) aNode).getThenStatement());
            printNestedIfThenElse(((IfThenElseStatement) aNode).getElseStatement());
            printOut();
            return;
        }
        if (aNode instanceof IfThenStatement) {
            setToOut("if (");
            eh.addSuperConditionString(((IfThenStatement) aNode).getCondition());
            addToOut(")");
            insertBlockStatement(((IfThenStatement) aNode).getThenStatement());
            printOut();
            return;
        }
        if (aNode instanceof SynchronizedStatement) {
            setToOut("synchronized (");
            eh.addSuperConditionString(((SynchronizedStatement) aNode).getLock());
            addToOut(")");
            parseObject(((SynchronizedStatement) aNode).getBody());
            printOut();
            return;
        }
        if (aNode instanceof WhileStatement) {
            WhileStatement ws = (WhileStatement) aNode;
            setToOut("while (");
            eh.addSuperConditionString(ws.getCondition());
            addToOut(")");
            if (ws.getBody() instanceof EmptyStatement || ws.getBody() instanceof BlockStatement && ((BlockStatement) ws.getBody()).getStatements().size() < 1) {
                printOut(";");
            } else {
                insertBlockStatement(ws.getBody());
                printOut();
            }
            return;
        }
        if (aNode instanceof DoStatement) {
            setToOut("do");
            parseObject(((DoStatement) aNode).getBody());
            addToOut("while (");
            eh.addSuperConditionString(((DoStatement) aNode).getCondition());
            addToOut(");");
            printOut();
            return;
        }
        if (aNode instanceof ForStatement) {
            ForStatement fs = (ForStatement) aNode;
            setToOut("for (");
            addVariableDeclarationListString(fs.getInitialization());
            addToOut(";");
            if (fs.getCondition() != null) {
                addToOut(" ");
            }
            eh.addSuperConditionString(fs.getCondition());
            addToOut(";");
            eh.addConditionListString(fs.getUpdate(), false);
            addToOut(")");
            if (fs.getBody() instanceof EmptyStatement || fs.getBody() instanceof BlockStatement && ((BlockStatement) fs.getBody()).getStatements().size() < 1) {
                printOut(";");
            } else {
                insertBlockStatement(fs.getBody());
                printOut();
            }
            return;
        }
        if (aNode instanceof SwitchStatement) {
            SwitchStatement ss = (SwitchStatement) aNode;
            setToOut("switch (");
            eh.addSuperConditionString(ss.getSelector());
            addToOut(") {");
            printOut();
            it = ss.getBindings().listIterator();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            jco.setCommentEnd(aNode.getEndLine());
            jco.printComment();
            jco.setCommentStart(aNode.getEndLine());
            printOut("}");
            return;
        }
        if (aNode instanceof SwitchBlock) {
            SwitchBlock sb = (SwitchBlock) aNode;
            if (sb.getExpression() != null) {
                setToOut("case ");
                eh.addSuperConditionString(sb.getExpression());
                addToOut(":");
            } else {
                setToOut("default:");
            }
            if (sb.getStatements() != null) {
                it = sb.getStatements().listIterator();
                if (it.hasNext()) {
                    Node node = (Node) it.next();
                    if (node instanceof BlockStatement) {
                        parseObject(node);
                        jco.increaseIndent();
                        printOut();
                        jco.decreaseIndent();
                    } else {
                        printOut();
                        jco.increaseIndent();
                        parseObject(node);
                        jco.decreaseIndent();
                    }
                }
                while (it.hasNext()) {
                    Node node = (Node) it.next();
                    jco.increaseIndent();
                    parseObject(node);
                    jco.decreaseIndent();
                    if (node instanceof BlockStatement) {
                        jco.increaseIndent();
                        printOut();
                        jco.decreaseIndent();
                    }
                }
                jco.setCommentEnd(aNode.getEndLine());
                jco.printComment();
                jco.setCommentStart(aNode.getEndLine());
            } else {
                printOut();
            }
            return;
        }
        if (aNode instanceof TryStatement) {
            TryStatement tryS = (TryStatement) aNode;
            setToOut("try");
            parseObject(tryS.getTryBlock());
            it = tryS.getCatchStatements().listIterator();
            while (it.hasNext()) {
                parseObject((Node) it.next());
            }
            if (tryS.getFinallyBlock() != null) {
                addToOut("finally");
                parseObject(((TryStatement) aNode).getFinallyBlock());
                printOut();
            } else {
                printOut();
            }
            return;
        }
        if (aNode instanceof CatchStatement) {
            addToOut("catch (");
            FormalParameter fp = ((CatchStatement) aNode).getException();
            addToOut(eh.getTypeString(fp.getType()) + " " + fp.getName() + ")");
            parseObject(((CatchStatement) aNode).getBlock());
            return;
        }
        if (aNode instanceof ThrowStatement) {
            setToOut("throw ");
            eh.addSuperConditionString(((ThrowStatement) aNode).getExpression());
            printOut(";");
            return;
        }
        if (aNode instanceof ContinueStatement) {
            printOut("continue;");
            return;
        }
        if (aNode instanceof BreakStatement) {
            printOut("break;");
            return;
        }
        if (aNode instanceof Expression) {
            setToOut("");
            eh.addSuperConditionString((Expression) aNode);
            addToOut(";");
            printOut();
            return;
        }
        printErr(aNode, "parseObject Node " + aNode + " not found on line " + aNode.getBeginLine());
        return;
    }

    /**
     *
     */
    private void insertBlockStatement(Node node) {
        if (node instanceof BlockStatement) {
            parseObject(node);
        } else {
            printOut(" {");
            jco.increaseIndent();
            parseObject(node);
            jco.decreaseIndent();
            setToOut("} ");
        }
    }

    /**
     * Prints nested if-then-else.
     */
    private void printNestedIfThenElse(Node aNode) {
        if (aNode instanceof IfThenElseStatement) {
            addToOut("else if (");
            eh.addSuperConditionString(((IfThenElseStatement) aNode).getCondition());
            addToOut(")");
            insertBlockStatement(((IfThenElseStatement) aNode).getThenStatement());
            if (((IfThenElseStatement) aNode).getElseStatement() instanceof IfThenElseStatement) {
                printNestedIfThenElse(((IfThenElseStatement) aNode).getElseStatement());
            } else {
                addToOut("else");
                insertBlockStatement(((IfThenElseStatement) aNode).getElseStatement());
            }
        } else {
            addToOut("else");
            insertBlockStatement(aNode);
        }
    }

    /**
     * Help method to encapsulate a often used loop in parseObject(declarations).
     *
     * @return A String consist of several parameters in a List separated by
     * ',MiddleSplitLevel ' with a LowSplitLevel if more than one element in it
     *
     */
    private String getParametersString(List someParameters) {
        boolean findSome = false;
        String ret = "";
        if (someParameters != null) {
            ret += jco.getLowSplitLevel();
            ListIterator it = someParameters.listIterator();
            while (it.hasNext()) {
                findSome = true;
                ret += " ";
                FormalParameter param = (FormalParameter) it.next();
                if (param.isFinal()) {
                    ret += "final ";
                }
                ret += eh.getTypeString(param.getType()) + " " + param.getName() + "," + jco.getMiddleSplitLevel();
            }
            if (ret.endsWith("," + jco.getMiddleSplitLevel())) {
                ret = ret.substring(0, ret.length() - ("," + jco.getMiddleSplitLevel()).length());
            }
        }
        if (findSome) {
            ret += " ";
        }
        return ret;
    }

    /**
     * Help method to encapsulate a used loop in parseObject(ForStatement).
     *
     * A String consist of several parameters in a List separated by
     * ',MiddleSplitLevel ' with a LowSplitLevel if more than one element in it
     *
     */
    private void addVariableDeclarationListString(List someParameters) {
        Node node;
        if (someParameters != null) {
            addToOut(jco.getLowSplitLevel());
            ListIterator it = someParameters.listIterator();
            if (it.hasNext()) {
                node = (Node) it.next();
                if (node instanceof VariableDeclaration) {
                    VariableDeclaration vd = (VariableDeclaration) node;
                    if (vd.isFinal()) {
                        addToOut("final ");
                    }
                    addToOut(eh.getTypeString(vd.getType()) + " ");
                    addToOut(vd.getName());
                    if (vd.getInitializer() != null) {
                        addToOut(" = " + jco.getHighSplitLevel());
                        eh.addSuperConditionString(vd.getInitializer());
                    }
                } else {
                    eh.addSuperConditionString((Expression) node);
                }
                addToOut("," + jco.getMiddleSplitLevel() + " ");
            }
            while (it.hasNext()) {
                node = (Node) it.next();
                if (node instanceof VariableDeclaration) {
                    VariableDeclaration vd = (VariableDeclaration) node;
                    addToOut(vd.getName());
                    if (vd.getInitializer() != null) {
                        addToOut(" = " + jco.getHighSplitLevel());
                        eh.addSuperConditionString(vd.getInitializer());
                    }
                } else {
                    eh.addSuperConditionString((Expression) node);
                }
                addToOut("," + jco.getMiddleSplitLevel() + " ");
            }
            if (getOut().endsWith("," + jco.getMiddleSplitLevel() + " ")) {
                setToOut(getOut().substring(0, getOut().length() - ("," + jco.getMiddleSplitLevel() + " ").length()));
            }
        }
    }

    /**
     * Adds the specified string to the global variable LineOut
     * @param add The String to add.
     */
    public void addToOut(String add) {
        outLine += add;
    }

    public void setToOut(String string) {
        outLine = string;
    }

    public void printOut() {
        jco.println(outLine);
        setToOut("");
    }

    /**
     * Adds the specified String and invoke the println method in the Outputclass.
     * Set the lineOut class member to ""
     *
     * @param add The String to add.
     */
    public void printOut(String add) {
        addToOut(add);
        printOut();
    }

    public String getOut() {
        return outLine;
    }

    /**
     * This method logs warnings on code violations.
     * @param aLine
     *
     */
    private void printLog(Node aNode, String aLine) {
        ;
    }

    private void printLog(String aLine) {
        ;
    }

    /**
     * This method print debug messages.
     * @param aLine
     *
     */
    private void printErr(Node aNode, String aLine) {
        System.err.println(getClass() + " ERROR: " + aLine);
    }

    /** */
    public static void printHelp() {
        System.out.println("usage: jAnalyzer [-o=<outputfile>|-] [-linelength=<number>] [-h|-?|-help] inputfile");
    }

    /**
     *
     *
     */
    public static void main(String[] argv) {
        String fileIn = null;
        String fileOut = null;
        String lineLength = null;
        int i = 0;
        if (argv.length > 0) {
            for (i = 0; i < argv.length; i++) {
                if (argv[i].startsWith("-o=")) {
                    fileOut = argv[i].substring("-o=".length());
                    continue;
                }
                if (argv[i].startsWith("-linelength=")) {
                    lineLength = argv[i].substring("-linelength=".length());
                    continue;
                }
                if (argv[i].equals("-h") || argv[i].equals("-?") || argv[i].equals("-help")) {
                    printHelp();
                    System.exit(0);
                }
                fileIn = argv[i];
                break;
            }
        }
        if (fileIn == null) {
            printHelp();
            System.exit(0);
        }
        if (fileOut != null && fileOut.equals("-")) {
            new JavaCodeAnalyzer(fileIn, fileIn, lineLength);
        } else {
            new JavaCodeAnalyzer(fileIn, fileOut, lineLength);
        }
        if (fileOut != null && fileOut.equals("-")) {
            while (i < argv.length) {
                new JavaCodeAnalyzer(argv[i], argv[i], lineLength);
                i++;
            }
        } else {
            while (i < argv.length) {
                new JavaCodeAnalyzer(argv[i], fileOut, lineLength);
                i++;
            }
        }
    }
}
