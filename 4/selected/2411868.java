package sijapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class Preprocessor {

    private Hashtable defines;

    private Hashtable localDefines = new Hashtable();

    private BufferedReader reader;

    private BufferedWriter writer;

    private int lineNum;

    private boolean stop;

    private boolean skip;

    private Stack skipStack = new Stack();

    private boolean done;

    private Stack doneStack = new Stack();

    public Preprocessor(Hashtable defines) {
        this.defines = defines;
    }

    public Scanner.Token[] evalExpr(Scanner.Token[] tokens) throws SijappException {
        Vector t = new Vector();
        for (int i = 0; i < tokens.length; i++) {
            t.add(tokens[i]);
        }
        main: while (true) {
            for (int i = 0; i < t.size() - 2; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                Scanner.Token t3 = (Scanner.Token) t.get(i + 2);
                if ((t1.getType() == Scanner.Token.T_EXPR_PRS_LEFT) && (t2.getType() == Scanner.Token.T_BOOL) && (t3.getType() == Scanner.Token.T_EXPR_PRS_RIGHT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, t2);
                    continue main;
                }
            }
            for (int i = 0; i < t.size() - 2; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                Scanner.Token t3 = (Scanner.Token) t.get(i + 2);
                if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) this.localDefines.get(t1.getValue());
                    String right = (String) this.localDefines.get(t3.getValue());
                    if (((left == null) && (right != null)) || ((left != null) && (right == null))) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                        continue main;
                    } else if ((left == null) && (right == null)) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) this.localDefines.get(t1.getValue());
                    String right = (String) t3.getValue();
                    if (left == null) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(this.localDefines.containsKey(t1.getValue()))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String right = (String) this.localDefines.get(t1.getValue());
                    String left = (String) t1.getValue();
                    if (left == null) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) t1.getValue();
                    String right = (String) t3.getValue();
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(left.equals(right))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(this.localDefines.containsKey(t3.getValue()))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_EQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                    continue main;
                }
            }
            for (int i = 0; i < t.size() - 2; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                Scanner.Token t3 = (Scanner.Token) t.get(i + 2);
                if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) this.localDefines.get(t1.getValue());
                    String right = (String) this.localDefines.get(t3.getValue());
                    if (((left == null) && (right != null)) || ((left != null) && (right == null))) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                        continue main;
                    } else if ((left == null) && (right == null)) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) this.localDefines.get(t1.getValue());
                    String right = (String) t3.getValue();
                    if (left == null) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_IDENT) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!this.localDefines.containsKey(t1.getValue()))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String right = (String) this.localDefines.get(t1.getValue());
                    String left = (String) t1.getValue();
                    if (left == null) {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(true)));
                        continue main;
                    } else {
                        t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!left.equals(right))));
                        continue main;
                    }
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    String left = (String) t1.getValue();
                    String right = (String) t3.getValue();
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!left.equals(right))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_STRING) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_IDENT)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!this.localDefines.containsKey(t3.getValue()))));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_STRING)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                    continue main;
                } else if ((t1.getType() == Scanner.Token.T_EXPR_DEF) && (t2.getType() == Scanner.Token.T_EXPR_NEQ) && (t3.getType() == Scanner.Token.T_EXPR_DEF)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(false)));
                    continue main;
                }
            }
            for (int i = 0; i < t.size() - 1; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                if ((t1.getType() == Scanner.Token.T_EXPR_NOT) && (t2.getType() == Scanner.Token.T_BOOL)) {
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(!((Boolean) t2.getValue()).booleanValue())));
                    continue main;
                }
            }
            for (int i = 0; i < t.size() - 2; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                Scanner.Token t3 = (Scanner.Token) t.get(i + 2);
                if ((t1.getType() == Scanner.Token.T_BOOL) && (t2.getType() == Scanner.Token.T_EXPR_AND) && (t3.getType() == Scanner.Token.T_BOOL)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(((Boolean) t1.getValue()).booleanValue() && ((Boolean) t3.getValue()).booleanValue())));
                    continue main;
                }
            }
            for (int i = 0; i < t.size() - 2; i++) {
                Scanner.Token t1 = (Scanner.Token) t.get(i);
                Scanner.Token t2 = (Scanner.Token) t.get(i + 1);
                Scanner.Token t3 = (Scanner.Token) t.get(i + 2);
                if ((t1.getType() == Scanner.Token.T_BOOL) && (t2.getType() == Scanner.Token.T_EXPR_OR) && (t3.getType() == Scanner.Token.T_BOOL)) {
                    t.remove(i + 2);
                    t.remove(i + 1);
                    t.remove(i);
                    t.add(i, new Scanner.Token(Scanner.Token.T_BOOL, new Boolean(((Boolean) t1.getValue()).booleanValue() || ((Boolean) t3.getValue()).booleanValue())));
                    continue main;
                }
            }
            break;
        }
        Scanner.Token[] ret = new Scanner.Token[t.size()];
        t.copyInto(ret);
        return (ret);
    }

    public void evalCond(Scanner.Token[] tokens) throws SijappException {
        Scanner.Token[] remainingTokens = new Scanner.Token[tokens.length - 2];
        System.arraycopy(tokens, 2, remainingTokens, 0, tokens.length - 2);
        if ((tokens.length >= 2) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_IF)) {
            Scanner.Token[] resultingTokens = this.evalExpr(remainingTokens);
            if ((resultingTokens.length != 1) || (resultingTokens[0].getType() != Scanner.Token.T_BOOL)) {
                throw (new SijappException("Syntax error"));
            }
            boolean result = ((Boolean) resultingTokens[0].getValue()).booleanValue();
            this.skipStack.push(new Boolean(this.skip));
            this.doneStack.push(new Boolean(this.done));
            this.skip |= !result;
            this.done = result;
        } else if ((tokens.length >= 2) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_ELSEIF)) {
            Scanner.Token[] resultingTokens = this.evalExpr(remainingTokens);
            if ((resultingTokens.length != 1) || (resultingTokens[0].getType() != Scanner.Token.T_BOOL)) {
                throw (new SijappException("Syntax error"));
            }
            boolean result = ((Boolean) resultingTokens[0].getValue()).booleanValue();
            this.skip = ((Boolean) this.skipStack.peek()).booleanValue() || this.done || !result;
            this.done |= result;
        } else if ((tokens.length == 2) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_ELSE)) {
            this.skip = ((Boolean) this.skipStack.peek()).booleanValue() || this.done;
            this.done = true;
        } else if ((tokens.length == 2) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_END)) {
            this.skip = ((Boolean) this.skipStack.pop()).booleanValue();
            this.done = ((Boolean) this.doneStack.pop()).booleanValue();
        } else {
            throw (new SijappException("Syntax error"));
        }
    }

    public void evalEcho(Scanner.Token[] tokens) throws SijappException {
        if ((tokens.length != 1) || (tokens[0].getType() != Scanner.Token.T_STRING)) {
            throw (new SijappException("Syntax error"));
        } else if (!this.skip) {
            String s = (String) tokens[0].getValue();
            try {
                writer.write(s, 0, s.length());
                writer.newLine();
            } catch (IOException e) {
                throw (new SijappException("An I/O error occured"));
            }
        }
    }

    public void evalEnv(Scanner.Token[] tokens) throws SijappException {
        if ((tokens.length == 3) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_DEF) && (tokens[2].getType() == Scanner.Token.T_IDENT)) {
            if (!this.skip) {
                this.localDefines.put(tokens[2].getValue(), "defined");
            }
        } else if ((tokens.length == 4) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_DEF) && (tokens[2].getType() == Scanner.Token.T_IDENT) && (tokens[3].getType() == Scanner.Token.T_STRING)) {
            if (!this.skip) {
                this.localDefines.put(tokens[2].getValue(), tokens[3].getValue());
            }
        } else if ((tokens.length == 3) && (tokens[0].getType() == Scanner.Token.T_SEP) && (tokens[1].getType() == Scanner.Token.T_CMD2_UNDEF) && (tokens[2].getType() == Scanner.Token.T_IDENT)) {
            if (!this.skip) {
                this.localDefines.remove(tokens[2].getValue());
            }
        } else {
            throw (new SijappException("Syntax error"));
        }
    }

    public void evalExit(Scanner.Token[] tokens) throws SijappException {
        if (tokens.length != 0) {
            throw (new SijappException("Syntax error"));
        } else if (!this.skip) {
            this.stop = true;
        }
    }

    public void eval(Scanner.Token[] tokens) throws SijappException {
        if (tokens.length < 3) {
            throw (new SijappException("Syntax error"));
        }
        if ((tokens[0].getType() != Scanner.Token.T_MAGIC_BEGIN) || (tokens[tokens.length - 1].getType() != Scanner.Token.T_MAGIC_END)) {
            throw (new SijappException("Syntax error"));
        }
        Scanner.Token[] remainingTokens = new Scanner.Token[tokens.length - 3];
        System.arraycopy(tokens, 2, remainingTokens, 0, tokens.length - 3);
        switch(tokens[1].getType()) {
            case Scanner.Token.T_CMD1_COND:
                this.evalCond(remainingTokens);
                break;
            case Scanner.Token.T_CMD1_ECHO:
                this.evalEcho(remainingTokens);
                break;
            case Scanner.Token.T_CMD1_ENV:
                this.evalEnv(remainingTokens);
                break;
            case Scanner.Token.T_CMD1_EXIT:
                this.evalExit(remainingTokens);
                break;
            default:
                throw (new SijappException("Syntax error"));
        }
    }

    public void run(BufferedReader reader, BufferedWriter writer) throws SijappException {
        this.localDefines.clear();
        for (Enumeration keys = this.defines.keys(); keys.hasMoreElements(); ) {
            String key = new String((String) keys.nextElement());
            String value = new String((String) this.defines.get(key));
            this.localDefines.put(key, value);
        }
        this.reader = reader;
        this.writer = writer;
        this.lineNum = 1;
        this.stop = false;
        this.skip = false;
        try {
            String line;
            while ((line = this.reader.readLine()) != null) {
                Scanner.Token[] tokens = Scanner.scan(line);
                if (tokens.length == 0) {
                    if (!skip) {
                        this.writer.write(line, 0, line.length());
                        this.writer.newLine();
                    }
                } else {
                    this.eval(tokens);
                    if (this.stop) {
                        return;
                    }
                }
                this.lineNum++;
            }
        } catch (SijappException e) {
            throw (new SijappException(this.lineNum + ": " + e.getMessage()));
        } catch (IOException e) {
            throw (new SijappException("An I/O error occured"));
        }
    }
}
