package org.ibex.util;

import gnu.regexp.*;
import java.util.*;
import java.io.*;

/**
 *   A VERY crude, inefficient Java preprocessor
 *
 *   //#define FOO bar baz       -- replace all instances of token FOO with "bar baz"
 *   //#replace foo/bar baz/bop  -- DUPLICATE everything between here and //#end,
 *                                  replacing foo with bar and baz with bop in the *second* copy
 *   //#switch(EXPR)             -- switch on strings
 *       case "case1":
 *   //#end
 *
 *   Replacements are done on a token basis.  Tokens are defined as a
 *   sequence of characters which all belong to a single class.  The
 *   two character classes are:
 *
 *     - [a-zA-Z0-9_]
 *     - all other non-whitespace characters
 */
public class Preprocessor {

    public static String replaceAll(String source, String regexp, String replaceWith) {
        try {
            RE re = new RE(regexp, 0, RESyntax.RE_SYNTAX_PERL5);
            return (String) re.substituteAll(source, replaceWith);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        boolean h = args.length > 0 && args[0].equalsIgnoreCase("-h");
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        FileReader br = new FileReader(inputFile);
        FileWriter bw = new FileWriter(outputFile);
        Preprocessor cc = new Preprocessor(br, bw, h);
        Vector err = cc.process();
        bw.flush();
        boolean errors = false;
        for (int i = 0; i < err.size(); i++) {
            if (err.get(i) instanceof Error) errors = true;
            System.err.println(err.get(i));
        }
        if (errors) throw new Exception();
    }

    private Reader r;

    private Writer w;

    private LineNumberReader in;

    private PrintWriter out;

    private boolean humanReadable;

    private Hashtable replace = new Hashtable();

    private Hashtable repeatreplace = null;

    private Vector sinceLastRepeat = null;

    private Vector err = new Vector();

    private int enumSwitch = 0;

    public Preprocessor(Reader reader, Writer writer, boolean humanReadable) {
        setReader(reader);
        setWriter(writer);
        this.humanReadable = humanReadable;
    }

    public void setReader(Reader reader) {
        r = reader;
        if (r != null) in = new LineNumberReader(r);
    }

    public Reader getReader() {
        return r;
    }

    public void setWriter(Writer writer) {
        w = writer;
        if (w != null) out = new PrintWriter(w);
    }

    public Writer getWriter() {
        return w;
    }

    /** process data from reader, write to writer, return vector of errors */
    public Vector process() throws IOException {
        err.clear();
        if (humanReadable) out.println("// This is Human-Readable output");
        String s = null;
        PROCESS: while ((s = in.readLine()) != null) {
            if (sinceLastRepeat != null) sinceLastRepeat.addElement(s);
            String trimmed = s.trim();
            if (trimmed.startsWith("//#define ")) {
                if (trimmed.length() == 9 || trimmed.charAt(9) != ' ') {
                    err.add(new Error("#define badly formed, ignored"));
                    continue PROCESS;
                }
                int keyStart = indexOfNotWS(trimmed, 9);
                if (keyStart == -1) {
                    err.add(new Error("#define requires KEY"));
                    continue PROCESS;
                }
                int keyEnd = indexOfWS(trimmed, keyStart);
                int macroStart = trimmed.indexOf('(');
                int macroEnd = trimmed.indexOf(')');
                if (macroStart > keyEnd) {
                    if (macroEnd < keyEnd) {
                        err.add(new Error("#define key contains invalid char: ')'"));
                        continue PROCESS;
                    }
                    macroStart = macroEnd = -1;
                }
                if (macroStart == 0) {
                    err.add(new Error("#define macro requires name"));
                    continue PROCESS;
                } else if (macroStart > 0) {
                    if (macroStart > macroEnd) {
                        err.add(new Error("#define macro badly formed"));
                        continue PROCESS;
                    }
                    if (macroStart + 1 == macroEnd) {
                        err.add(new Error("#define macro requires property name"));
                        continue PROCESS;
                    }
                    JSFunctionMacro fm = new JSFunctionMacro();
                    String key = trimmed.substring(keyStart, macroStart);
                    String unbound = trimmed.substring(macroStart + 1, macroEnd);
                    int unboundDiv = unbound.indexOf(',');
                    if (unboundDiv == -1) {
                        fm.unbound1 = unbound;
                    } else {
                        fm.unbound1 = unbound.substring(0, unboundDiv);
                        fm.unbound2 = unbound.substring(unboundDiv + 1);
                        if (fm.unbound1.length() == 0) {
                            err.add(new Error("#define macro property 1 requires name"));
                            continue PROCESS;
                        }
                        if (fm.unbound2.length() == 0) {
                            err.add(new Error("#define macro property 1 requires name"));
                            continue PROCESS;
                        }
                    }
                    fm.expression = trimmed.substring(keyEnd).trim();
                    replace.put(key, fm);
                } else {
                    String key = trimmed.substring(keyStart, keyEnd);
                    String val = trimmed.substring(keyEnd).trim();
                    replace.put(key, val);
                }
                out.print("\n");
            } else if (trimmed.startsWith("//#repeat ")) {
                trimmed = trimmed.substring(9);
                while (trimmed.charAt(trimmed.length() - 1) == '\\') {
                    String s2 = in.readLine().trim();
                    if (s2.startsWith("//")) s2 = s2.substring(2).trim();
                    trimmed = trimmed.substring(0, trimmed.length() - 1) + " " + s2;
                    if (!humanReadable) out.print("\n");
                }
                StringTokenizer st = new StringTokenizer(trimmed, " ");
                repeatreplace = (Hashtable) replace.clone();
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken().trim();
                    String key = tok.substring(0, tok.indexOf('/'));
                    String val = tok.substring(tok.indexOf('/') + 1);
                    repeatreplace.put(key, val);
                }
                sinceLastRepeat = new Vector();
                if (!humanReadable) out.print("\n");
            } else if (trimmed.startsWith("//#end")) {
                if (sinceLastRepeat == null) {
                    err.add(new Warning("#end orphaned"));
                    continue PROCESS;
                }
                Hashtable save = replace;
                replace = repeatreplace;
                out.print("\n");
                for (int i = 0; i < sinceLastRepeat.size() - 1; i++) {
                    out.print(processLine((String) sinceLastRepeat.elementAt(i), true));
                    if (humanReadable) out.print("\n");
                }
                sinceLastRepeat = null;
                replace = save;
            } else if (trimmed.startsWith("//#switch")) {
                int expStart = trimmed.indexOf('(') + 1;
                if (expStart < 1) {
                    err.add(new Error("expected ( in #switch"));
                    continue PROCESS;
                }
                int expEnd = trimmed.lastIndexOf(')');
                if (expEnd == -1) {
                    err.add(new Error("expected ) in #switch"));
                    continue PROCESS;
                }
                if (expEnd - expStart <= 1) {
                    err.add(new Error("badly formed #switch statement"));
                    continue PROCESS;
                }
                String expr = trimmed.substring(expStart, expEnd);
                out.print("final String ccSwitch" + enumSwitch + " = (String)(" + expr + ");  ");
                out.print("SUCCESS:do { switch(ccSwitch" + enumSwitch + ".length()) {\n");
                Hashtable[] byLength = new Hashtable[255];
                String key = null;
                String Default = null;
                for (trimmed = in.readLine().trim(); !trimmed.startsWith("//#end"); trimmed = in.readLine().trim()) {
                    if (trimmed.startsWith("default:")) {
                        Default = processLine(trimmed.substring(8), false);
                        continue;
                    }
                    if (trimmed.startsWith("case ")) {
                        int strStart = trimmed.indexOf('\"') + 1;
                        if (strStart < 1) {
                            err.add(new Error("expected opening of String literal"));
                            continue PROCESS;
                        }
                        int strEnd = trimmed.indexOf('\"', strStart);
                        if (strEnd == -1) {
                            err.add(new Error("expected closing of String literal"));
                            continue PROCESS;
                        }
                        key = trimmed.substring(strStart, strEnd);
                        Hashtable thisCase = (Hashtable) byLength[key.length()];
                        if (thisCase == null) byLength[key.length()] = thisCase = new Hashtable();
                        thisCase.put(key, "");
                        int caseEnd = trimmed.indexOf(':', strEnd) + 1;
                        if (caseEnd < 1) {
                            err.add(new Error("expected :"));
                            continue PROCESS;
                        }
                        trimmed = trimmed.substring(caseEnd);
                    }
                    if (key != null) {
                        Hashtable hash = byLength[key.length()];
                        hash.put(key, (String) hash.get(key) + replaceAll(processLine(trimmed, false), "//[^\"]*$", "").trim() + "\n");
                    } else {
                        out.print(processLine(trimmed, false));
                    }
                }
                for (int i = 0; i < 255; i++) {
                    if (byLength[i] == null) continue;
                    out.print("case " + i + ": { switch(ccSwitch" + enumSwitch + ".charAt(0)) { ");
                    buildTrie("", byLength[i]);
                    out.print("}; break; }  ");
                }
                out.print("} /* switch */ ");
                if (Default != null) out.print(" " + Default);
                out.print(" } while(false); /* OUTER */\n");
                enumSwitch++;
            } else {
                out.print(processLine(s, false));
            }
        }
        return err;
    }

    private void buildTrie(String prefix, Hashtable cases) {
        Enumeration caseKeys = cases.keys();
        Vec keys = new Vec();
        while (caseKeys.hasMoreElements()) keys.addElement(caseKeys.nextElement());
        keys.sort(new Vec.CompareFunc() {

            public int compare(Object a, Object b) {
                return ((String) a).compareTo((String) b);
            }
        });
        for (int i = 0; i < keys.size(); i++) {
            if (!((String) keys.elementAt(i)).startsWith(prefix)) continue;
            String prefixPlusOne = ((String) keys.elementAt(i)).substring(0, prefix.length() + 1);
            if (i < keys.size() - 1 && prefixPlusOne.equals((((String) keys.elementAt(i + 1)).substring(0, prefix.length() + 1)))) {
                out.print("case \'" + prefixPlusOne.charAt(prefixPlusOne.length() - 1) + "\': { ");
                out.print("switch(ccSwitch" + enumSwitch + ".charAt(" + (prefix.length() + 1) + ")) { ");
                buildTrie(prefixPlusOne, cases);
                out.print("} break; } ");
                while (i < keys.size() && prefixPlusOne.equals(((String) keys.elementAt(i)).substring(0, prefix.length() + 1))) i++;
                if (i < keys.size()) {
                    i--;
                    continue;
                }
            } else {
                out.print("case \'" + prefixPlusOne.charAt(prefixPlusOne.length() - 1) + "\': ");
                String code = (String) cases.get(keys.elementAt(i));
                code = code.substring(0, code.length());
                String key = (String) keys.elementAt(i);
                out.print("if (\"" + key + "\".equals(ccSwitch" + enumSwitch + ")) { if (true) do { " + code + " } while(false); break SUCCESS; } break;  ");
            }
        }
    }

    private String processLine(String s, boolean deleteLineEndings) throws IOException {
        if (deleteLineEndings && s.indexOf("//") != -1) s = s.substring(0, s.indexOf("//"));
        String ret = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') {
                ret += c;
                continue;
            }
            int j;
            for (j = i; j < s.length(); j++) {
                c = s.charAt(j);
                if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') break;
            }
            String tok = s.substring(i, j);
            Object val = replace.get(tok);
            if (val == null) {
                ret += tok;
                i = j - 1;
            } else if (val instanceof JSFunctionMacro) {
                if (s.charAt(j) != '(') {
                    ret += tok;
                    i = j - 1;
                    continue;
                }
                ret += ((JSFunctionMacro) val).process(s.substring(j + 1, s.indexOf(')', j)));
                i = s.indexOf(')', j);
            } else {
                ret += val;
                i = j - 1;
            }
        }
        if (!deleteLineEndings) ret += "\n";
        return ret;
    }

    private static int indexOfWS(String s) {
        return indexOfWS(s, 0);
    }

    private static int indexOfWS(String s, int beginIndex) {
        if (s == null || beginIndex >= s.length()) return -1;
        for (; beginIndex < s.length(); beginIndex++) {
            if (s.charAt(beginIndex) == ' ') return beginIndex;
        }
        return s.length();
    }

    private static int indexOfNotWS(String s) {
        return indexOfWS(s, 0);
    }

    private static int indexOfNotWS(String s, int beginIndex) {
        if (s == null || beginIndex >= s.length()) return -1;
        for (; beginIndex < s.length(); beginIndex++) {
            if (s.charAt(beginIndex) != ' ') return beginIndex;
        }
        return -1;
    }

    public class Warning {

        protected String msg;

        protected int line;

        public Warning() {
            msg = "";
        }

        public Warning(String m) {
            msg = m;
            if (in != null) line = in.getLineNumber();
        }

        public String toString() {
            return "WARNING Line " + line + ": " + msg;
        }
    }

    public class Error extends Warning {

        public Error() {
            super();
        }

        public Error(String m) {
            super(m);
        }

        public String toString() {
            return "ERROR Line " + line + ": " + msg;
        }
    }

    public static class JSFunctionMacro {

        public String unbound1 = null;

        public String unbound2 = null;

        public String expression = null;

        public String process(String args) {
            String bound1 = null;
            String bound2 = null;
            if (unbound2 == null) {
                bound1 = args;
                return replaceAll(expression, unbound1, bound1);
            } else {
                bound1 = args.substring(0, args.indexOf(','));
                bound2 = args.substring(args.indexOf(',') + 1);
                return replaceAll(replaceAll(expression, unbound1, bound1), unbound2, bound2);
            }
        }
    }
}
