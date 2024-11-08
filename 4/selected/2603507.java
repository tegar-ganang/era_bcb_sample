package net.sourceforge.jseditor.editors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;

/**
 * DOCUMENT ME!
 * 
 * @author Addi 
 */
public class JSParser {

    public static final String FUNCTION = "function";

    /**
	 * line separator
	 */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
	 * Array of system types to ignore.
	 */
    private static String[] systemClassNames = { "Array", "String" };

    protected HashMap systemClassMap = new HashMap();

    protected IDocument sourceDocument;

    protected HashMap functions = new HashMap();

    protected HashMap classes = new HashMap();

    protected HashMap globalVariables = new HashMap();

    protected List elementList = new LinkedList();

    protected JSSyntaxScanner scanner = new JSSyntaxScanner();

    /**
	 * Constructor for JSParser.
	 */
    public JSParser() {
        super();
        int i;
        for (i = 0; i < systemClassNames.length; i++) {
            String aName = systemClassNames[i];
            systemClassMap.put(aName, aName);
        }
    }

    /**
	 * Returns a string containing the contents of the given file.  Returns an empty string if there
	 * were any errors reading the file.
	 * @param file
	 * 
	 * @return
	 */
    protected static String getText(IFile file) {
        try {
            InputStream in = file.getContents();
            return streamToString(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    protected static String streamToString(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read = in.read(buf);
        while (read > 0) {
            out.write(buf, 0, read);
            read = in.read(buf);
        }
        return out.toString();
    }

    /**
	 * Skips ahead and finds next non-whitespace token.
	 *
	 */
    public IToken nextNonWhitespaceToken() {
        IToken aToken = scanner.nextToken();
        while (!aToken.isEOF() && aToken.isWhitespace()) {
            aToken = scanner.nextToken();
        }
        return aToken;
    }

    /**
	 * Parses the input given by the argument.
	 * 
	 * @param file  the element containing the input text
	 * 
	 * @return an element collection representing the parsed input
	 */
    public List parse(IFile file) {
        return parse(new Document(getText(file)));
    }

    /**
	 * Parses the input given by the argument.
	 * 
	 * @param aSourceDocument  the element containing the input text
	 * 
	 * @return an element collection representing the parsed input
	 */
    public List parse(IDocument aSourceDocument) {
        sourceDocument = aSourceDocument;
        scanner.setRange(sourceDocument, 0, sourceDocument.getLength());
        IToken token = scanner.nextToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (token.equals(JSSyntaxScanner.TOKEN_FUNCTION)) {
                addFunction(expression, offset, length);
            }
            if (token.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
                if (functions.containsKey(expression) || classes.containsKey(expression)) {
                    token = nextNonWhitespaceToken();
                    if (token.equals(JSSyntaxScanner.TOKEN_MEMBER)) {
                        detectInstanceMethod(offset, expression);
                    } else {
                        detectClassMethod(token, offset, expression);
                    }
                } else {
                    if (expression.equals("var")) {
                        detectGlobalVariable();
                    }
                }
            }
            token = scanner.nextToken();
        }
        return elementList;
    }

    private void addFunction(String expression, int offset, int length) {
        String functionSignature = getNaked(expression);
        int braceOffset = functionSignature.indexOf("(");
        String functionName = functionSignature.substring(0, braceOffset).trim();
        String arguments = functionSignature.substring(functionSignature.indexOf("("), functionSignature.indexOf(")") + 1);
        if (functionName.indexOf(".") >= 0) {
            if (functionName.indexOf(".prototype.") >= 0) {
                String className = functionName.substring(0, functionName.indexOf("."));
                String memberName = functionName.substring(functionName.lastIndexOf(".") + 1);
                JSInstanceMethodElement aMethod = this.addInstanceMethod(memberName, className, arguments, offset, offset, length);
                detectInstanceMethodContext(className, aMethod);
            } else {
                String className = functionName.substring(0, functionName.indexOf("."));
                if (functions.containsKey(className) || classes.containsKey(className)) {
                    String memberName = functionName.substring(functionName.lastIndexOf(".") + 1);
                    JSFunctionElement aMethod = this.addClassMethod(memberName, className, arguments, offset, offset, length);
                }
            }
        } else {
            if (!functions.containsKey(functionName)) {
                JSFunctionElement aFunction = new JSFunctionElement(functionName, arguments, offset, length);
                elementList.add(aFunction);
                functions.put(functionName, aFunction);
                detectFunctionContext(aFunction);
            }
        }
    }

    /**
	 *
	 */
    private void checkForSpecialGlobalTypes(JSGlobalVariableElement aVariable) {
        IToken token = nextNonWhitespaceToken();
        if (!token.isEOF()) {
            if (!checkForDynamicClass(aVariable, token)) {
                checkForAnonymousFunction(aVariable, token);
            }
        }
    }

    /**
	 *
	 */
    private boolean checkForDynamicClass(JSGlobalVariableElement aVariable, IToken rhsToken) {
        if (rhsToken.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("new")) {
                IToken token = nextNonWhitespaceToken();
                if (!token.isEOF()) {
                    if (token.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
                        offset = scanner.getTokenOffset();
                        length = scanner.getTokenLength();
                        expression = getExpression(offset, length);
                        if (!isSystemClass(expression)) {
                            JSClassElement aClass = findOrCreateClass(aVariable.getName());
                            if (aClass != null) {
                                aClass.setPrototype(true);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
	 *
	 */
    private boolean checkForAnonymousFunction(JSGlobalVariableElement aVariable, IToken rhsToken) {
        if (rhsToken.equals(JSSyntaxScanner.TOKEN_FUNCTION)) {
            String functionName = aVariable.getName();
            int offset = aVariable.getOffset();
            int length = aVariable.getLength();
            int functionOffset = scanner.getTokenOffset();
            int functionLength = scanner.getTokenLength();
            String functionSignature = getExpression(functionOffset, functionLength);
            String arguments = getArgumentString(functionSignature);
            JSFunctionElement aFunction = new JSFunctionElement(functionName, arguments, offset, functionOffset - offset + functionLength);
            elementList.add(aFunction);
            functions.put(functionName, aFunction);
            elementList.remove(aVariable);
            globalVariables.remove(functionName);
            detectFunctionContext(aFunction);
            return true;
        }
        return false;
    }

    /**
	 *
	 */
    private String getExpression(int offset, int length) {
        String expression;
        try {
            expression = sourceDocument.get(offset, length);
        } catch (BadLocationException e) {
            expression = "";
        }
        return expression;
    }

    /**
	 *
	 */
    private void detectGlobalVariable() {
        IToken token;
        int length;
        int offset;
        token = nextNonWhitespaceToken();
        if (!token.isEOF()) {
            if (token.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
                int varOffset = scanner.getTokenOffset();
                length = scanner.getTokenLength();
                String variableName = getExpression(varOffset, length);
                token = nextNonWhitespaceToken();
                if (!token.isEOF()) {
                    offset = scanner.getTokenOffset();
                    length = scanner.getTokenLength();
                    String expression = getExpression(offset, length);
                    if (expression.equals("=")) {
                        JSGlobalVariableElement aVariable = addGlobalVariable(variableName, varOffset);
                        checkForSpecialGlobalTypes(aVariable);
                    }
                }
            }
        }
    }

    private void detectClassMethod(IToken token, int classOffset, String className) {
        int offset = scanner.getTokenOffset();
        int length = scanner.getTokenLength();
        String expression = getExpression(offset, length);
        if (expression.equals(".")) {
            token = nextNonWhitespaceToken();
            if (!token.isEOF()) {
                offset = scanner.getTokenOffset();
                length = scanner.getTokenLength();
                String memberName = getExpression(offset, length);
                token = nextNonWhitespaceToken();
                if (!token.isEOF()) {
                    offset = scanner.getTokenOffset();
                    length = scanner.getTokenLength();
                    expression = getExpression(offset, length);
                    if (expression.equals("=")) {
                        token = nextNonWhitespaceToken();
                        int tokenOffset = scanner.getTokenOffset();
                        int tokenLength = scanner.getTokenLength();
                        if (token.equals(JSSyntaxScanner.TOKEN_FUNCTION)) {
                            String functionSignature = getExpression(tokenOffset, tokenLength);
                            String arguments = getArgumentString(functionSignature);
                            JSFunctionElement aMethod = addClassMethod(memberName, className, arguments, classOffset, tokenOffset, tokenLength);
                        } else {
                            addClassVariable(memberName, className, classOffset);
                        }
                    }
                }
            }
        }
    }

    private String getArgumentString(String functionSignature) {
        return functionSignature.substring(functionSignature.indexOf("("), functionSignature.indexOf(")") + 1);
    }

    private void detectInstanceMethod(int classOffset, String className) {
        String expression;
        IToken token;
        int length;
        int offset;
        token = nextNonWhitespaceToken();
        if (!token.isEOF()) {
            offset = scanner.getTokenOffset();
            length = scanner.getTokenLength();
            expression = getExpression(offset, length);
            if (expression.equals(".")) {
                token = nextNonWhitespaceToken();
                if (!token.isEOF()) {
                    offset = scanner.getTokenOffset();
                    length = scanner.getTokenLength();
                    String memberName = getExpression(offset, length);
                    token = nextNonWhitespaceToken();
                    if (!token.isEOF()) {
                        offset = scanner.getTokenOffset();
                        length = scanner.getTokenLength();
                        expression = getExpression(offset, length);
                        if (expression.equals("=")) {
                            token = nextNonWhitespaceToken();
                            if (token.equals(JSSyntaxScanner.TOKEN_FUNCTION)) {
                                int functionOffset = scanner.getTokenOffset();
                                int functionLength = scanner.getTokenLength();
                                String functionSignature = getExpression(functionOffset, functionLength);
                                String arguments = getArgumentString(functionSignature);
                                JSInstanceMethodElement aMethod = addInstanceMethod(memberName, className, arguments, classOffset, functionOffset, functionLength);
                                detectInstanceMethodContext(className, aMethod);
                            } else {
                                addInstanceVariable(memberName, className, classOffset, (".prototype.").length());
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseInstanceMethodContext(String className, JSFunctionElement aMethod) {
        IToken token;
        token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("}")) {
                return;
            } else if (expression.equals("{")) {
                parseInstanceMethodContext(className, aMethod);
            } else if (token.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
                if (expression.equals("this")) {
                    handleThisReference(className, offset);
                }
            }
            token = nextNonWhitespaceToken();
        }
    }

    private void detectInstanceMethodContext(String className, JSFunctionElement aMethod) {
        IToken token;
        token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("{")) {
                parseInstanceMethodContext(className, aMethod);
                return;
            }
            token = nextNonWhitespaceToken();
        }
    }

    private void parseClassMethodContext(JSFunctionElement aMethod) {
        IToken token;
        token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("}")) {
                return;
            } else if (expression.equals("{")) {
                parseClassMethodContext(aMethod);
            }
            token = nextNonWhitespaceToken();
        }
    }

    private void detectClassMethodContext(JSFunctionElement aMethod) {
        IToken token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("{")) {
                parseClassMethodContext(aMethod);
                return;
            }
            token = nextNonWhitespaceToken();
        }
    }

    private void handleThisReference(String className, int expressionStart) {
        IToken token = nextNonWhitespaceToken();
        if (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals(".")) {
                token = nextNonWhitespaceToken();
                if (!token.isEOF()) {
                    int memberStart = scanner.getTokenOffset();
                    length = scanner.getTokenLength();
                    String memberName = getExpression(memberStart, length);
                    token = nextNonWhitespaceToken();
                    if (!token.isEOF()) {
                        offset = scanner.getTokenOffset();
                        length = scanner.getTokenLength();
                        expression = getExpression(offset, length);
                        if (expression.equals("=")) {
                            addInstanceVariable(memberName, className, expressionStart, 1 + 4 - className.length());
                        }
                    }
                }
            }
        }
    }

    private void parseFunctionContext(JSFunctionElement aFunction) {
        IToken token;
        token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("}")) {
                return;
            } else if (expression.equals("{")) {
                parseFunctionContext(aFunction);
            } else if (token.equals(JSSyntaxScanner.TOKEN_DEFAULT)) {
                if (expression.equals("this")) {
                    handleThisReference(aFunction.getName(), offset);
                }
            }
            token = nextNonWhitespaceToken();
        }
    }

    private void detectFunctionContext(JSFunctionElement aFunction) {
        IToken token = nextNonWhitespaceToken();
        while (!token.isEOF()) {
            int offset = scanner.getTokenOffset();
            int length = scanner.getTokenLength();
            String expression = getExpression(offset, length);
            if (expression.equals("{")) {
                parseFunctionContext(aFunction);
                return;
            }
            token = nextNonWhitespaceToken();
        }
    }

    private JSInstanceMethodElement addInstanceMethod(String memberName, String className, String arguments, int classOffset, int functionOffset, int functionLength) {
        int signatureLength = functionOffset - classOffset + functionLength;
        JSInstanceMethodElement aMethod = new JSInstanceMethodElement(memberName, arguments, classOffset, signatureLength);
        findOrCreateClass(className).addChildElement(aMethod);
        return aMethod;
    }

    private JSFunctionElement addClassMethod(String memberName, String className, String arguments, int classOffset, int functionOffset, int functionLength) {
        JSClassElement aClass = findOrCreateClass(className);
        int signatureLength = functionOffset - classOffset + functionLength;
        JSFunctionElement aMethod;
        if (aClass.isPrototype()) {
            aMethod = new JSInstanceMethodElement(memberName, arguments, classOffset, signatureLength);
            aClass.addChildElement(aMethod);
            detectInstanceMethodContext(className, aMethod);
        } else {
            aMethod = new JSClassMethodElement(memberName, arguments, classOffset, signatureLength);
            aClass.addChildElement(aMethod);
            detectClassMethodContext(aMethod);
        }
        return aMethod;
    }

    private JSElement addClassVariable(String memberName, String className, int classOffset) {
        JSElement aVariable;
        JSClassElement aClass = findOrCreateClass(className);
        if (aClass.isPrototype()) {
            aVariable = new JSInstanceVariableElement(memberName, classOffset, className.length() + memberName.length() + 1);
        } else {
            aVariable = new JSClassVariableElement(memberName, classOffset, className.length() + memberName.length() + 1);
        }
        aClass.addChildElement(aVariable);
        return aVariable;
    }

    private JSInstanceVariableElement addInstanceVariable(String memberName, String className, int classOffset, int paddingWidth) {
        JSInstanceVariableElement aVariable = new JSInstanceVariableElement(memberName, classOffset, className.length() + memberName.length() + paddingWidth);
        findOrCreateClass(className).addChildElement(aVariable);
        return aVariable;
    }

    private JSGlobalVariableElement addGlobalVariable(String variableName, int offset) {
        JSGlobalVariableElement aVariable;
        if (!globalVariables.containsKey(variableName)) {
            aVariable = new JSGlobalVariableElement(variableName, offset, variableName.length());
            elementList.add(aVariable);
            globalVariables.put(variableName, aVariable);
        } else {
            aVariable = (JSGlobalVariableElement) classes.get(variableName);
        }
        return aVariable;
    }

    private JSClassElement findOrCreateClass(String className) {
        JSClassElement aClass = null;
        if (!classes.containsKey(className)) {
            if (functions.containsKey(className)) {
                JSFunctionElement constructor = (JSFunctionElement) functions.get(className);
                aClass = new JSClassElement(className, constructor.getStart(), constructor.getLength());
                aClass.addChildElement(constructor);
                elementList.remove(constructor);
                elementList.add(aClass);
                classes.put(className, aClass);
            } else if (globalVariables.containsKey(className)) {
                JSGlobalVariableElement aVariable = (JSGlobalVariableElement) globalVariables.get(className);
                aClass = new JSClassElement(className, aVariable.getStart(), aVariable.getLength());
                elementList.remove(aVariable);
                elementList.add(aClass);
                classes.put(className, aClass);
                globalVariables.remove(className);
            } else {
                aClass = new JSClassElement(className, 0, 0);
                elementList.add(aClass);
                classes.put(className, aClass);
            }
        } else {
            aClass = (JSClassElement) classes.get(className);
        }
        return aClass;
    }

    public boolean isSystemClass(String aClassName) {
        return systemClassMap.containsKey(aClassName);
    }

    /**
	 * Method getNaked.
	 * @param funcName
	 */
    private String getNaked(String funcName) {
        if (funcName == null) {
            return null;
        }
        funcName = funcName.trim().substring(FUNCTION.length()).trim();
        funcName = replaceInString(funcName.trim(), LINE_SEPARATOR, "");
        StringBuffer strBuf = new StringBuffer("");
        int len = funcName.length();
        boolean wasSpace = false;
        for (int i = 0; i < len; i++) {
            char ch = funcName.charAt(i);
            if (ch == ' ') {
                wasSpace = true;
            } else {
                if (wasSpace) {
                    strBuf.append(' ');
                }
                strBuf.append(ch);
                wasSpace = false;
            }
        }
        return strBuf.toString();
    }

    /**
	 * replace in a string a string sequence with another string sequence
	 */
    public static String replaceInString(String source, String whatBefore, String whatAfter) {
        if (null == source || source.length() == 0) {
            return source;
        }
        int beforeLen = whatBefore.length();
        if (beforeLen == 0) {
            return source;
        }
        StringBuffer result = new StringBuffer("");
        int lastIndex = 0;
        int index = source.indexOf(whatBefore, lastIndex);
        while (index >= 0) {
            result.append(source.substring(lastIndex, index));
            result.append(whatAfter);
            lastIndex = index + beforeLen;
            index = source.indexOf(whatBefore, lastIndex);
        }
        result.append(source.substring(lastIndex));
        return result.toString();
    }
}
