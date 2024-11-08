package minire;

import java.io.*;
import java.util.*;

/**
 * Actual execution base for a minre script.
 *
 * @author Nicolas Papin
 */
public class MinReProgram {

    /**
	 * Our actual script, represented as an abstract syntax tree.
	 */
    private AST<String> mScript;

    /**
	 * A global dictionary of values for use during execution.  This should be set to null when not executing.
	 */
    private IDStore mSymbolTable;

    public MinReProgram(AST<String> script) {
        mScript = script;
    }

    /**
	 * Executes the MinRe Script.
	 */
    public void execute() {
        mSymbolTable = new IDStore();
        ASTNode<String> list = mScript.root.getChildren().get(1);
        ArrayList<ASTNode<String>> children;
        while ((children = list.getChildren()).size() > 1) {
            exeStatement(children.get(0));
            list = children.get(1);
        }
        mSymbolTable = null;
    }

    /**
	 * Executes an individual statement.
	 * 
	 * @param stmt the statement to be executed.
	 */
    private void exeStatement(ASTNode<String> stmt) {
        ArrayList<ASTNode<String>> children = stmt.getChildren();
        if (children.size() > 1 && children.get(1).getValue().equals("=")) {
            assignmentStatement(children.get(0).getValue(), children.get(2));
        } else {
            String operation = children.get(0).getValue();
            if (operation.equals("print")) {
                printStatement(children.get(2));
            } else if (operation.equals("replace")) {
                File[] f = fileNames(children.get(5));
                String rep = children.get(3).getValue();
                ID id = new ID(children.get(1).getValue(), f[0].getPath());
                replace(id, rep, f[0], f[1]);
            } else if (operation.equals("recursivereplace")) {
                File[] f = fileNames(children.get(5));
                String rep = children.get(3).getValue();
                recursiveReplace(children.get(1).getValue(), rep, f[0], f[1]);
            }
        }
    }

    /**
	 * Handles standard replacement.
	 * 
	 * @param id a string list representing the instances to be replaced.
	 * @param rep the string to replace the found instances with.
	 * @param src the source file to replace from.
	 * @param dest the destination file.
	 */
    private void replace(ID id, String rep, File src, File dest) {
        RandomAccessFile fis = null;
        PrintWriter pw = null;
        try {
            if (!dest.exists()) dest.createNewFile();
            fis = new RandomAccessFile(src, "r");
            pw = new PrintWriter(dest);
            int location = 0;
            char next;
            fis.seek(location);
            while (location < fis.length()) {
                String currMatch = id.atLocation(location);
                if (currMatch != null) {
                    location += currMatch.length();
                    fis.seek(location);
                    pw.print(rep);
                } else {
                    next = (char) fis.read();
                    location++;
                    pw.print(next);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("A file specified by the replace method was not found.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } catch (IOException ex) {
            System.out.println("Error attempting to read or write to specified file.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ex) {
            }
            if (pw != null) pw.close();
        }
    }

    /**
	 * Handles recursive replacement.
	 * Warning, this is a terrible implementation.  On huge files this will be incredibly slow and resource intensive.
	 * This also buffers the entire src file as it performs the recursive replace.
	 * 
	 * @param src the source file to replace from.
	 * @param rep the string to replace the found instances with.
	 * @param dest the destination file.
	 */
    private void recursiveReplace(String regex, String rep, File src, File dest) {
        String buffer = "";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            int next;
            while ((next = fis.read()) != -1) {
                buffer += (char) next;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("A file specified by the recursive replace method was not found.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } catch (IOException ex) {
            System.out.println("Error attempting to read specified file.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ex) {
            }
        }
        String newStr = recursiveReplace(regex, rep, buffer);
        PrintWriter pw = null;
        try {
            if (!dest.exists()) dest.createNewFile();
            pw = new PrintWriter(dest);
            pw.append(newStr);
        } catch (FileNotFoundException ex) {
            System.out.println("A file specified by the recursive replace method was not found.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } catch (IOException ex) {
            System.out.println("Error attempting to write to specified file.");
            System.out.println(ex.getMessage());
            System.exit(0);
        } finally {
            if (pw != null) pw.close();
        }
    }

    /**
	 * Performs recursive replace on a string buffer and returns the new buffer.
	 * 
	 * @param regex the regex to replace.
	 * @param rep the replacement value.
	 * @param src the string to recursively replace from.
	 * @return the finalized string after replacement.
	 */
    private String recursiveReplace(String regex, String rep, String src) {
        String ret = "";
        ID id = ID.fromString(regex, src);
        if (id.num().i == 0) return src;
        int location = 0;
        while (location < src.length()) {
            String currMatch = id.atLocation(location);
            if (currMatch != null) {
                location += currMatch.length();
                ret += rep;
                continue;
            } else {
                ret += src.charAt(location);
                location++;
            }
        }
        String newRet = recursiveReplace(regex, rep, ret);
        return newRet;
    }

    /**
	 * Assigns the variable represented by the given string to the specified value.
	 * 
	 * @param id the id of the variable to assign.
	 * @param args arguments for the assignment.
	 */
    private void assignmentStatement(String id, ASTNode<String> args) {
        ArrayList<ASTNode<String>> children = args.getChildren();
        if (children.get(0).isTerminal()) {
            String op = children.get(0).getValue();
            if (op.equals("#")) {
                ID exp = evaluateExpression(children.get(1));
                if (exp.isInt) {
                    System.out.println("Error while trying to assign value: Expected string-list for # assignment to: " + id + ".");
                    System.exit(0);
                }
                mSymbolTable.putValue(id, exp.num());
            } else if (op.equals("maxfreqstring")) {
                ID m = mSymbolTable.getValue(children.get(2).getValue());
                mSymbolTable.putValue(id, m.maxFreqStr());
            }
        } else if (children.get(0).getValue().equals("<exp>")) {
            mSymbolTable.putValue(id, evaluateExpression(children.get(0)));
        }
    }

    /**
	 * Executes a print statement.
	 * 
	 * @param args the list of arguments to print.
	 */
    private void printStatement(ASTNode<String> args) {
        List<ID> printArgs = evaluateExpList(args);
        for (Object p : printArgs) {
            System.out.println(p);
        }
    }

    /**
	 * Returns a list of the evaluations of all of the given expressions.
	 * 
	 * @param expList the expression list.
	 * @return the evaluated values.
	 */
    private List<ID> evaluateExpList(ASTNode<String> expList) {
        ArrayList<ASTNode<String>> children = expList.getChildren();
        ASTNode<String> exp = children.get(0), tail = children.get(1);
        ArrayList<ID> ret = new ArrayList<ID>();
        while (tail.getChildren().size() > 1) {
            ret.add(evaluateExpression(exp));
            children = tail.getChildren();
            exp = children.get(1);
            tail = children.get(2);
        }
        ret.add(evaluateExpression(exp));
        return ret;
    }

    /**
	 * Evaluates an expression.
	 * 
	 * @param exp the expression to evaluate.
	 * @return the value of the expression.
	 */
    private ID evaluateExpression(ASTNode<String> exp) {
        ArrayList<ASTNode<String>> children = exp.getChildren();
        if (children.get(0).isTerminal()) {
            String op = children.get(0).getValue();
            if (op.equals("(")) {
                return evaluateExpression(children.get(1));
            } else {
                ID ret = mSymbolTable.getValue(op);
                if (ret == null) {
                    System.out.println("Error.  Attempting to access uninitialized variable.");
                    System.exit(0);
                }
                return ret;
            }
        } else if (children.get(0).getValue().equals("<term>")) {
            ASTNode<String> expTail = children.get(1);
            ID term = evaluateTerm(children.get(0));
            ID tail = evaluateExpTail(expTail);
            if (tail != null) {
                String op = expTail.getChildren().get(0).getChildren().get(0).getValue();
                if (op.equals("union")) {
                    return term.union(tail);
                } else if (op.equals("inters")) {
                    return term.inters(tail);
                } else if (op.equals("diff")) {
                    return term.diff(tail);
                } else {
                    System.out.println("Unsupported binary operation: " + op);
                    System.exit(0);
                }
            }
            return term;
        }
        return null;
    }

    /**
	 * Evaluates a term.
	 * 
	 * @param term the term element.
	 * @return the ID.
	 */
    private ID evaluateTerm(ASTNode<String> term) {
        ArrayList<ASTNode<String>> children = term.getChildren();
        File f = evaluateFileName(children.get(3));
        return new ID(children.get(1).getValue(), f.getPath());
    }

    /**
	 * Evaluates all items of an expression tail.
	 * 
	 * @param expTail the expression tail.
	 * @return the ID.
	 */
    private ID evaluateExpTail(ASTNode<String> expTail) {
        ArrayList<ASTNode<String>> children = expTail.getChildren();
        if (children.get(0).isTerminal()) {
            return null;
        } else {
            expTail = children.get(2);
            ID term = evaluateTerm(children.get(1));
            ID tail = evaluateExpTail(expTail);
            if (tail != null) {
                String op = expTail.getChildren().get(0).getChildren().get(0).getValue();
                if (op.equals("union")) {
                    return term.union(tail);
                } else if (op.equals("inters")) {
                    return term.inters(tail);
                } else if (op.equals("diff")) {
                    return term.diff(tail);
                } else {
                    System.out.println("Unsupported binary operation: " + op);
                    System.exit(0);
                }
            }
            return term;
        }
    }

    /**
	 * Returns the files from a file-names element.
	 * 
	 * @param fileNames the element to parse.
	 * @return the files (should be 2).
	 */
    private File[] fileNames(ASTNode<String> fileNames) {
        ArrayList<ASTNode<String>> children = fileNames.getChildren();
        return new File[] { evaluateFileName(children.get(0)), evaluateFileName(children.get(2)) };
    }

    /**
	 * Parses an element only containing a file name.
	 * 
	 * @param fileName the element to parse.
	 * @return the file.
	 */
    private File evaluateFileName(ASTNode<String> fileName) {
        ArrayList<ASTNode<String>> children = fileName.getChildren();
        File ret;
        String fName = children.get(0).getValue();
        ret = new File(fName);
        return ret;
    }

    /**
	 * Used for holding ID mappings.
	 *
	 * @author Nicolas Papin
	 */
    private class IDStore {

        /**
		 * The mapping for string-list ids.
		 */
        private Map<String, ID> mValues;

        public IDStore() {
            mValues = new HashMap<String, ID>();
        }

        /**
		 * Returns the mapping of a string-list attribute, or null if it does not exist.
		 * This returning null does not necessarily imply that a numeric entry of the same name does not exist.
		 * 
		 * @param id the identifier to get.
		 * @return the value.
		 */
        public ID getValue(String id) {
            return mValues.get(id);
        }

        /**
		 * Puts a string-list value at the given id.
		 * 
		 * @param id the id.
		 * @param value the value.
		 */
        public void putValue(String id, ID value) {
            mValues.put(id, value);
        }
    }
}
