package org.ibex.tool;

import java.util.*;
import java.io.*;

/**
 *   A VERY crude, inefficient Java preprocessor
 * <pre>
 *   //#define FOO bar baz       -- replace all instances of token FOO with "bar baz"
 *   //#repeat foo/bar baz/bop   -- DUPLICATE everything between here and //#end,
 *                                  replacing foo with bar and baz with bop in the *second* copy
 *   //#switch(EXPR)             -- switch on strings
 *       case "case1":
 *   //#end
 *
 *   //#ifdef FOO                -- includes contents if FOO passed as define to preprocessor
 *       [code]
 *   //#else
 *       [code run if !FOO]
 *   //#endif
 *    
 *   //#set {public/protected/static} name	 -- Define a set  
 *   	{ "foo" , "bar" }
 *   //#end
 *   //#map						 -- Define a map - TODO 
 *   	{ "foo" : "bar" , ... }
 *   //#end
 *   
 *   
 *   //#pragma tokens FOO        --declares a list of strings that are always treated a separate tokens
 *                                 (eg. FOOBAR will now be treated as two tokens, FOO & BAR)
 *                                
 *   
 *   
 *</pre>
 *<p>
 *   Replacements are done on a token basis.  Tokens are defined as a
 *   sequence of characters which all belong to a single class.  The
 *   two character classes are:
 *   <ul>
 *   <li>- [a-zA-Z0-9_]</li>
 *   <li>- all other non-whitespace characters</li>
 *   </ul>
 * </p>
 * <p>  
 *   Preprocessor makes use of several optional system properties:
 * <ul>
 *    <li>- ibex.tool.preprocessor.define</li>
 *    <li>- ibex.tool.preprocessor.inputdir</li>
 *    <li>- ibex.tool.preprocessor.outputdir</li>
 * </ul>  
 *  
 *</p>
 *   @author adam@ibex.org, crawshaw@ibex.org
 */
public class Preprocessor {

    public static String replaceAll(String source, String regexp, String replaceWith) {
        return source.replaceAll(regexp, replaceWith);
    }

    public static List getDefinesFromArgument(String define) {
        List defs = new ArrayList();
        if (define != null) {
            StringTokenizer st = new StringTokenizer(define.toUpperCase(), ",");
            while (st.hasMoreTokens()) defs.add(st.nextToken().trim());
        }
        return defs;
    }

    public static void main(String[] args) throws Exception {
        String define = System.getProperty("ibex.tool.preprocessor.define");
        List defs = getDefinesFromArgument(define);
        String inputdir = System.getProperty("ibex.tool.preprocessor.inputdir");
        if (inputdir == null) inputdir = "src/";
        String outputdir = System.getProperty("ibex.tool.preprocessor.outputdir");
        if (outputdir == null) outputdir = "build/java/";
        if (args.length == 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
            Preprocessor cc = new Preprocessor(br, bw, defs);
            Vector err = cc.process();
            bw.flush();
            boolean errors = false;
            for (int i = 0; i < err.size(); i++) {
                if (err.get(i) instanceof Error) errors = true;
                System.err.println(err.get(i));
            }
            if (errors) throw new Exception();
        } else {
            for (int i = 0; i < args.length; i++) {
                if (!args[i].endsWith(".java") && !args[i].endsWith(".jpp")) continue;
                File source = new File(args[i]);
                File target = new File(replaceAll(replaceAll(args[i], inputdir, outputdir), ".jpp", ".java"));
                if (target.exists() && target.lastModified() > source.lastModified()) continue;
                System.err.println("preprocessing " + args[i]);
                new File(target.getParent()).mkdirs();
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(target)));
                Preprocessor cc = new Preprocessor(br, bw, defs);
                Vector err = cc.process();
                bw.flush();
                boolean errors = false;
                for (int j = 0; j < err.size(); j++) {
                    if (err.get(j) instanceof Error) errors = true;
                    System.err.println(err.get(j));
                }
                if (errors) System.exit(-1);
            }
        }
    }

    private Reader r;

    private Writer w;

    private LineNumberReader in;

    private PrintWriter out;

    private Hashtable replace = new Hashtable();

    private Hashtable[] repeatreplaces = null;

    private Vector sinceLastRepeat = null;

    private Vector err = new Vector();

    private List defs;

    private String[] pragma_tokens = {};

    private int enumSwitch = 0;

    public Preprocessor(Reader reader, Writer writer, List d) {
        setReader(reader);
        setWriter(writer);
        defs = d;
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
        try {
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
                        String key = trimmed.substring(keyStart, macroStart);
                        String unbound = trimmed.substring(macroStart + 1, macroEnd);
                        String expression = "";
                        String line = trimmed.substring(keyEnd).trim();
                        while (line.endsWith("\\")) {
                            expression += line.substring(0, line.length() - 1);
                            line = in.readLine().trim();
                            if (line.startsWith("//")) line = line.substring(2).trim();
                            out.print("\n");
                        }
                        expression += line;
                        FunctionMacro fm = createFunctionMacro(unbound, expression);
                        if (fm == null) continue PROCESS;
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
                        out.print("\n");
                    }
                    StringTokenizer st = new StringTokenizer(trimmed, " ");
                    repeatreplaces = null;
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken().trim();
                        String key = tok.substring(0, tok.indexOf('/'));
                        String vals = tok.substring(tok.indexOf('/') + 1);
                        StringTokenizer st2 = new StringTokenizer(vals, "/");
                        if (repeatreplaces == null) {
                            repeatreplaces = new Hashtable[st2.countTokens()];
                            for (int i = 0; i < repeatreplaces.length; i++) repeatreplaces[i] = (Hashtable) replace.clone();
                        }
                        for (int i = 0; st2.hasMoreTokens() && i < repeatreplaces.length; i++) repeatreplaces[i].put(key, st2.nextToken());
                    }
                    sinceLastRepeat = new Vector();
                    out.print("\n");
                } else if (trimmed.startsWith("//#end")) {
                    if (sinceLastRepeat == null) {
                        err.add(new Warning("#end orphaned"));
                        continue PROCESS;
                    }
                    Hashtable save = replace;
                    out.print("\n");
                    for (int i = 0; i < repeatreplaces.length; i++) {
                        replace = repeatreplaces[i];
                        for (int j = 0; j < sinceLastRepeat.size() - 1; j++) out.print(processLine((String) sinceLastRepeat.elementAt(j), true));
                    }
                    sinceLastRepeat = null;
                    replace = save;
                } else if (trimmed.startsWith("//#ifdef")) {
                    String expr = trimmed.substring(8).trim().toUpperCase();
                    out.println(trimmed);
                    boolean useCode = defs.contains(expr);
                    for (trimmed = in.readLine().trim(); !trimmed.startsWith("//#endif"); trimmed = in.readLine().trim()) {
                        if (trimmed.startsWith("//#else")) useCode = !useCode; else if (!useCode) out.print("// ");
                        out.print(processLine(trimmed, false));
                    }
                    out.println("//#endif " + expr);
                } else if (trimmed.startsWith("//#switch") || trimmed.startsWith("//#jsswitch")) {
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
                    if (trimmed.startsWith("//#jsswitch")) expr = "JSU.toString(" + expr + ")";
                    out.print("final String ccSwitch" + enumSwitch + " = " + expr + ";  ");
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
                            Hashtable thisCase = byLength[key.length()];
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
                } else if (trimmed.startsWith("//#set")) {
                    trimmed = trimmed.substring(6);
                    boolean isStatic = trimmed.contains("static");
                    String name = trimmed.substring(trimmed.lastIndexOf(' ') + 1);
                    out.print(processLine(trimmed, true) + " = new HashSet();\n");
                    out.print(isStatic ? "static{" : "{");
                    for (trimmed = in.readLine().trim(); !trimmed.startsWith("//#end"); trimmed = in.readLine().trim()) {
                        if (!trimmed.startsWith("//")) trimmed = trimmed.replaceAll("([^,]+),?", name + ".add($1);");
                        out.print(processLine(trimmed, false));
                    }
                    out.print("}");
                } else if (trimmed.startsWith("//#pragma tokens ")) {
                    trimmed = trimmed.substring("//#pragma tokens ".length()).trim();
                    pragma_tokens = trimmed.split("\\s+");
                } else {
                    out.print(processLine(s, false));
                }
            }
        } catch (Exception e) {
            err.add(new Error(e.getMessage()));
        }
        return err;
    }

    private static class MyVec {

        private Object[] store;

        private int size = 0;

        public MyVec() {
            this(10);
        }

        public MyVec(int i) {
            store = new Object[i];
        }

        public MyVec(int i, Object[] store) {
            size = i;
            this.store = store;
        }

        private void grow() {
            grow(store.length * 2);
        }

        private void grow(int newsize) {
            Object[] newstore = new Object[newsize];
            System.arraycopy(store, 0, newstore, 0, size);
            store = newstore;
        }

        public void removeAllElements() {
            for (int i = 0; i < size; i++) store[i] = null;
            size = 0;
        }

        public void toArray(Object[] o) {
            for (int i = 0; i < size; i++) o[i] = store[i];
        }

        public int indexOf(Object o) {
            for (int i = 0; i < size; i++) if (o == null ? store[i] == null : store[i].equals(o)) return i;
            return -1;
        }

        public void addElement(Object o) {
            if (size >= store.length - 1) grow();
            store[size++] = o;
        }

        public Object peek() {
            return lastElement();
        }

        public Object elementAt(int i) {
            return store[i];
        }

        public Object lastElement() {
            if (size == 0) return null;
            return store[size - 1];
        }

        public void push(Object o) {
            addElement(o);
        }

        public Object pop() {
            Object ret = lastElement();
            if (size > 0) store[size--] = null;
            return ret;
        }

        public int size() {
            return size;
        }

        public void setSize(int newSize) {
            if (newSize < 0) throw new RuntimeException("tried to set size to negative value");
            if (newSize > store.length) grow(newSize * 2);
            if (newSize < size) for (int i = newSize; i < size; i++) store[i] = null;
            size = newSize;
        }

        public void copyInto(Object[] out) {
            for (int i = 0; i < size; i++) out[i] = store[i];
        }

        public void fromArray(Object[] in) {
            setSize(in.length);
            for (int i = 0; i < size; i++) store[i] = in[i];
        }

        public void removeElementAt(int i) {
            if (i >= size || i < 0) throw new RuntimeException("tried to remove an element outside the vector's limits");
            for (int j = i; j < size - 1; j++) store[j] = store[j + 1];
            setSize(size - 1);
        }

        public void setElementAt(Object o, int i) {
            if (i >= size) setSize(i);
            store[i] = o;
        }

        public void removeElement(Object o) {
            int idx = indexOf(o);
            if (idx != -1) removeElementAt(idx);
        }

        public void insertElementAt(Object o, int at) {
            if (size == store.length) grow();
            for (int i = size; i > at; i--) store[i] = store[i - 1];
            store[at] = o;
            size++;
        }

        public interface CompareFunc {

            public int compare(Object a, Object b);
        }

        public void sort(CompareFunc c) {
            sort(this, null, c, 0, size - 1);
        }

        public static void sort(MyVec a, MyVec b, CompareFunc c) {
            if (b != null && a.size != b.size) throw new IllegalArgumentException("MyVec a and b must be of equal size");
            sort(a, b, c, 0, a.size - 1);
        }

        private static final void sort(MyVec a, MyVec b, CompareFunc c, int start, int end) {
            Object tmpa, tmpb = null;
            if (start >= end) return;
            if (end - start <= 6) {
                for (int i = start + 1; i <= end; i++) {
                    tmpa = a.store[i];
                    if (b != null) tmpb = b.store[i];
                    int j;
                    for (j = i - 1; j >= start; j--) {
                        if (c.compare(a.store[j], tmpa) <= 0) break;
                        a.store[j + 1] = a.store[j];
                        if (b != null) b.store[j + 1] = b.store[j];
                    }
                    a.store[j + 1] = tmpa;
                    if (b != null) b.store[j + 1] = tmpb;
                }
                return;
            }
            Object pivot = a.store[end];
            int lo = start - 1;
            int hi = end;
            do {
                while (c.compare(a.store[++lo], pivot) < 0) {
                }
                while ((hi > lo) && c.compare(a.store[--hi], pivot) > 0) {
                }
                swap(a, lo, hi);
                if (b != null) swap(b, lo, hi);
            } while (lo < hi);
            swap(a, lo, end);
            if (b != null) swap(b, lo, end);
            sort(a, b, c, start, lo - 1);
            sort(a, b, c, lo + 1, end);
        }

        private static final void swap(MyVec vec, int a, int b) {
            if (a != b) {
                Object tmp = vec.store[a];
                vec.store[a] = vec.store[b];
                vec.store[b] = tmp;
            }
        }
    }

    private void buildTrie(String prefix, Hashtable cases) {
        Enumeration caseKeys = cases.keys();
        MyVec keys = new MyVec();
        while (caseKeys.hasMoreElements()) keys.addElement(caseKeys.nextElement());
        keys.sort(new MyVec.CompareFunc() {

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
        return lh.processLine(s, deleteLineEndings);
    }

    LineHelper lh = new LineHelper();

    class LineHelper {

        StringBuffer lineout = null;

        int i = -1;

        int j = -1;

        String s;

        private String processLine(String line, boolean deleteLineEndings) throws IOException {
            s = line;
            if (deleteLineEndings && s.indexOf("//") != -1) s = s.substring(0, s.indexOf("//"));
            lineout = new StringBuffer();
            for (i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c)) {
                    lineout.append(c);
                    continue;
                }
                if (Character.isLetter(c) || Character.isDigit(c) || c == '_') for (j = i; j < s.length(); j++) {
                    c = s.charAt(j);
                    if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') break;
                } else for (j = i; j < s.length(); j++) {
                    c = s.charAt(j);
                    if (Character.isLetter(c) || Character.isDigit(c) || c == '_' || Character.isWhitespace(c)) break;
                }
                String tok = s.substring(i, j);
                processToken(tok);
            }
            if (!deleteLineEndings) lineout.append("\n");
            return lineout.toString();
        }

        private void processToken(String tok) throws IOException {
            for (int k = 0; k < pragma_tokens.length; k++) {
                String pragTok = pragma_tokens[k];
                if (tok.length() > pragTok.length()) {
                    int index = tok.indexOf(pragTok);
                    if (index != -1) {
                        int d = j;
                        j = i + index;
                        if (index > 0) {
                            processToken(tok.substring(0, index));
                        }
                        j = i + pragTok.length();
                        processToken(pragTok);
                        j = d;
                        if (index + pragTok.length() < tok.length()) {
                            processToken(tok.substring(index + pragTok.length()));
                        } else i = j - 1;
                        return;
                    }
                }
            }
            Object val = replace.get(tok);
            if (val == null) {
                lineout.append(tok);
                i = j - 1;
            } else if (val instanceof FunctionMacro) {
                if (s.charAt(j) != '(') {
                    lineout.append(tok);
                    i = j - 1;
                    return;
                }
                lineout.append(((FunctionMacro) val).process(s.substring(j + 1, indexOfClosingBracket(s, j))));
                i = indexOfClosingBracket(s, j);
            } else {
                lineout.append(val);
                i = j - 1;
            }
        }
    }

    private List splitArgs(String s) {
        List args = new ArrayList();
        int nesting = 0;
        int lastComma = 0;
        int i = 0;
        for (i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') nesting++; else if (s.charAt(i) == ')') nesting--; else if (s.charAt(i) == ',' && nesting == 0) {
                String arg = s.substring(lastComma, i);
                args.add(arg);
                lastComma = i + 1;
            }
        }
        String arg = s.substring(lastComma, i);
        args.add(arg);
        return args;
    }

    private static int indexOfClosingBracket(String s, int beginIndex) {
        if (s == null || beginIndex >= s.length()) return -1;
        int nesting = 0;
        for (; beginIndex < s.length(); beginIndex++) {
            if (s.charAt(beginIndex) == '(') nesting++;
            if (s.charAt(beginIndex) == ')') {
                nesting--;
                if (nesting == 0) return beginIndex;
            }
        }
        return -1;
    }

    private static int indexOfWS(String s, int beginIndex) {
        if (s == null || beginIndex >= s.length()) return -1;
        for (; beginIndex < s.length(); beginIndex++) {
            if (s.charAt(beginIndex) == ' ') return beginIndex;
        }
        return s.length();
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

        public int getLine() {
            return line;
        }

        public String getMessage() {
            return msg;
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

    private FunctionMacro createFunctionMacro(String unboundString, String expression) {
        boolean error = false;
        String[] unbound = unboundString.split(",");
        for (int i = 0; i < unbound.length; i++) {
            unbound[i] = unbound[i].trim();
            if (unbound[i].length() == 0) {
                err.add(new Error("#define macro property " + i + " requires name"));
                error = true;
            }
        }
        if (error) return null;
        FunctionMacro fm = new FunctionMacro(unbound, expression);
        return fm;
    }

    public class FunctionMacro {

        LineHelper lh;

        public String[] unbound = new String[] {};

        public String expression = null;

        public FunctionMacro(String[] unbound, String expression) {
            this.expression = expression;
            this.unbound = unbound;
        }

        public String process(String argString) throws IOException {
            if (lh != null) {
                err.add(new Warning("#define macro called with incorrect number of lines"));
                return "";
            }
            try {
                LineHelper lh = new LineHelper();
                argString = lh.processLine(argString, false);
                List bound = splitArgs(argString);
                if (bound.size() != unbound.length) {
                    err.add(new Warning("#define macro called with incorrect number of args"));
                }
                String r = expression;
                Iterator I = bound.iterator();
                for (int i = 0; I.hasNext(); i++) {
                    String b = ((String) I.next()).trim();
                    r = replaceAll(r, unbound[i], b);
                }
                return r;
            } finally {
                lh = null;
            }
        }
    }
}
