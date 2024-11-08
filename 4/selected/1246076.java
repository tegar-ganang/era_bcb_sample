package org.vexi.build.jpp;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import org.vexi.util.Problem;

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
 *   //#
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

    public static Map<String, Object> getDefinesFromArgument(String define) {
        Map defs = new HashMap();
        if (define != null) {
            StringTokenizer st = new StringTokenizer(define.toUpperCase(), ",");
            while (st.hasMoreTokens()) {
                String d = st.nextToken().trim();
                String[] ss = d.split("=", 2);
                String k = ss[0];
                String v;
                if (ss.length == 1) v = ""; else v = ss[1];
                defs.put(k, v);
            }
        }
        return defs;
    }

    private SourcePath path;

    private Map<String, Object> defines = new HashMap();

    private Map[] repeatreplaces = null;

    private Vector sinceLastRepeat = null;

    private List<Problem> err = new ArrayList();

    private String[] pragma_tokens = {};

    private int enumSwitch = 0;

    public interface SourcePath {

        Reader getFile(String sourceName) throws IOException;
    }

    public Preprocessor(SourcePath path, String defines) {
        this(path, getDefinesFromArgument(defines));
    }

    public Preprocessor(SourcePath path, Map<String, Object> defines) {
        this.path = path;
        this.defines = defines;
    }

    public List<Problem> preprocess(String sourceName, Writer out) throws IOException {
        Reader in = path.getFile(sourceName);
        return preprocess(sourceName, in, out);
    }

    public List<Problem> preprocess(String sourceName, Reader in, Writer out) throws IOException {
        return new IntraFile(sourceName, in, out).process();
    }

    private class IntraFile {

        final String sourceName;

        final LineNumberReader in;

        final PrintWriter out;

        public IntraFile(String sourceName, Reader reader, Writer writer) {
            this.sourceName = sourceName;
            this.in = new LineNumberReader(reader);
            this.out = new PrintWriter(writer);
        }

        private void addProblem(boolean e, String msg) {
            err.add(new Problem(e, sourceName, in.getLineNumber(), msg));
        }

        private void w(String msg) {
            addProblem(false, msg);
        }

        private void e(String msg) {
            addProblem(true, msg);
        }

        /** process data from reader, write to writer, return vector of errors */
        public List<Problem> process() throws IOException {
            err.clear();
            try {
                String s = null;
                PROCESS: while ((s = in.readLine()) != null) {
                    if (sinceLastRepeat != null) sinceLastRepeat.addElement(s);
                    String trimmed = s.trim();
                    if (trimmed.startsWith("//#define ")) {
                        if (trimmed.length() == 9 || trimmed.charAt(9) != ' ') {
                            e("#define badly formed, ignored");
                            continue PROCESS;
                        }
                        int keyStart = indexOfNotWS(trimmed, 9);
                        if (keyStart == -1) {
                            e("#define requires KEY");
                            continue PROCESS;
                        }
                        int keyEnd = indexOfWS(trimmed, keyStart);
                        int macroStart = trimmed.indexOf('(');
                        int macroEnd = trimmed.indexOf(')');
                        if (macroStart > keyEnd) {
                            if (macroEnd < keyEnd) {
                                e("#define key contains invalid char: ')'");
                                continue PROCESS;
                            }
                            macroStart = macroEnd = -1;
                        }
                        if (macroStart == 0) {
                            e("#define macro requires name");
                            continue PROCESS;
                        } else if (macroStart > 0) {
                            if (macroStart > macroEnd) {
                                e("#define macro badly formed");
                                continue PROCESS;
                            }
                            if (macroStart + 1 == macroEnd) {
                                e("#define macro requires property name");
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
                            defines.put(key, fm);
                        } else {
                            String key = trimmed.substring(keyStart, keyEnd);
                            String val = trimmed.substring(keyEnd).trim();
                            defines.put(key, val);
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
                                repeatreplaces = new Map[st2.countTokens()];
                                for (int i = 0; i < repeatreplaces.length; i++) repeatreplaces[i] = (Map) new HashMap(defines);
                            }
                            for (int i = 0; st2.hasMoreTokens() && i < repeatreplaces.length; i++) repeatreplaces[i].put(key, st2.nextToken());
                        }
                        sinceLastRepeat = new Vector();
                        out.print("\n");
                    } else if (trimmed.startsWith("//#end")) {
                        if (sinceLastRepeat == null) {
                            w("#end orphaned");
                            continue PROCESS;
                        }
                        Map save = defines;
                        out.print("\n");
                        for (int i = 0; i < repeatreplaces.length; i++) {
                            defines = repeatreplaces[i];
                            for (int j = 0; j < sinceLastRepeat.size() - 1; j++) out.print(processLine((String) sinceLastRepeat.elementAt(j), true));
                        }
                        sinceLastRepeat = null;
                        defines = save;
                    } else if (trimmed.startsWith("//#ifdef")) {
                        String expr = trimmed.substring(8).trim().toUpperCase();
                        out.println(trimmed);
                        boolean useCode = defines.containsKey(expr);
                        while (true) {
                            trimmed = in.readLine();
                            if (trimmed == null) {
                                w("missing #endif");
                                break;
                            }
                            trimmed = trimmed.trim();
                            if (trimmed.startsWith("//#endif")) break;
                            if (trimmed.startsWith("//#else")) useCode = !useCode; else if (!useCode) out.print("// ");
                            out.print(processLine(trimmed, false));
                        }
                        out.println("//#endif " + expr);
                    } else if (trimmed.startsWith("//#switch") || trimmed.startsWith("//#jsswitch") || trimmed.startsWith("//#ifswitch")) {
                        int expStart = trimmed.indexOf('(') + 1;
                        if (expStart < 1) {
                            e("expected ( in #switch");
                            continue PROCESS;
                        }
                        int expEnd = trimmed.lastIndexOf(')');
                        if (expEnd == -1) {
                            e("expected ) in #switch");
                            continue PROCESS;
                        }
                        if (expEnd - expStart <= 1) {
                            e("badly formed #switch statement");
                            continue PROCESS;
                        }
                        String expr = trimmed.substring(expStart, expEnd);
                        if (trimmed.startsWith("//#jsswitch")) expr = "JSU.toString(" + expr + ")";
                        boolean useifs = trimmed.startsWith("//#if");
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
                                    e("expected opening of String literal");
                                    continue PROCESS;
                                }
                                int strEnd = trimmed.indexOf('\"', strStart);
                                if (strEnd == -1) {
                                    e("expected closing of String literal");
                                    continue PROCESS;
                                }
                                key = trimmed.substring(strStart, strEnd);
                                Hashtable thisCase = byLength[key.length()];
                                if (thisCase == null) byLength[key.length()] = thisCase = new Hashtable();
                                thisCase.put(key, "");
                                int caseEnd = trimmed.indexOf(':', strEnd) + 1;
                                if (caseEnd < 1) {
                                    e("expected :");
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
                        out.print("final String ccSwitch" + enumSwitch + " = " + expr + ";  ");
                        if (useifs) {
                            out.print("if(false){\n} ");
                            for (int i = 0; i < 255; i++) {
                                if (byLength[i] == null) continue;
                                buildTrieIfs("", byLength[i]);
                            }
                        } else {
                            out.print("SUCCESS:do { switch(ccSwitch" + enumSwitch + ".length()) {\n");
                            for (int i = 0; i < 255; i++) {
                                if (byLength[i] == null) continue;
                                out.print("case " + i + ": { switch(ccSwitch" + enumSwitch + ".charAt(0)) { ");
                                buildTrie("", byLength[i]);
                                out.print("}; break; }  ");
                            }
                            out.print("} /* switch */ ");
                            if (Default != null) out.print(" " + Default);
                            out.print(" } while(false); /* OUTER */\n");
                        }
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
                    } else if (trimmed.startsWith("//#include")) {
                        trimmed = trimmed.substring("//#include".length()).trim();
                        Reader r = path.getFile(trimmed);
                        if (r == null) {
                            e("Could not find file: " + trimmed);
                        } else {
                            new IntraFile(trimmed, new LineNumberReader(r), out).process();
                        }
                    } else {
                        out.print(processLine(s, false));
                    }
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                e(sw.toString());
            }
            return err;
        }

        private void buildTrie(String prefix, Map<String, String> cases) {
            Iterator<String> caseKeys = cases.keySet().iterator();
            List<String> keys = new ArrayList();
            while (caseKeys.hasNext()) keys.add(caseKeys.next());
            Collections.sort(keys);
            for (int i = 0; i < keys.size(); i++) {
                if (!keys.get(i).startsWith(prefix)) continue;
                String prefixPlusOne = keys.get(i).substring(0, prefix.length() + 1);
                if (i < keys.size() - 1 && prefixPlusOne.equals((keys.get(i + 1).substring(0, prefix.length() + 1)))) {
                    out.print("case \'" + prefixPlusOne.charAt(prefixPlusOne.length() - 1) + "\': { ");
                    out.print("switch(ccSwitch" + enumSwitch + ".charAt(" + (prefix.length() + 1) + ")) { ");
                    buildTrie(prefixPlusOne, cases);
                    out.print("} break; } ");
                    while (i < keys.size() && prefixPlusOne.equals(keys.get(i).substring(0, prefix.length() + 1))) i++;
                    if (i < keys.size()) {
                        i--;
                        continue;
                    }
                } else {
                    out.print("case \'" + prefixPlusOne.charAt(prefixPlusOne.length() - 1) + "\': ");
                    String code = cases.get(keys.get(i));
                    code = code.substring(0, code.length());
                    String key = keys.get(i);
                    out.print("if (\"" + key + "\".equals(ccSwitch" + enumSwitch + ")) { if (true) do { " + code + " } while(false); break SUCCESS; } break;  ");
                }
            }
        }

        private void buildTrieIfs(String prefix, Map<String, String> cases) {
            Iterator<String> caseKeys = cases.keySet().iterator();
            List<String> keys = new ArrayList();
            while (caseKeys.hasNext()) keys.add(caseKeys.next());
            Collections.sort(keys);
            for (int i = 0; i < keys.size(); i++) {
                if (!keys.get(i).startsWith(prefix)) continue;
                String code = cases.get(keys.get(i));
                code = code.substring(0, code.length());
                String key = keys.get(i);
                out.print("else if (\"" + key + "\".equals(ccSwitch" + enumSwitch + ")) { " + code + "  } ");
            }
        }

        private FunctionMacro createFunctionMacro(String unboundString, String expression) {
            boolean error = false;
            String[] unbound = unboundString.split(",");
            for (int i = 0; i < unbound.length; i++) {
                unbound[i] = unbound[i].trim();
                if (unbound[i].length() == 0) {
                    e("#define macro property " + i + " requires name");
                    error = true;
                }
            }
            if (error) return null;
            FunctionMacro fm = new FunctionMacro(unbound, expression);
            return fm;
        }

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
                Object val = defines.get(tok);
                if (val == null) {
                    lineout.append(tok);
                    i = j - 1;
                } else if (val instanceof FunctionMacro) {
                    if (s.charAt(j) != '(') {
                        lineout.append(tok);
                        i = j - 1;
                        return;
                    }
                    lineout.append(processFuncMacro((FunctionMacro) val, s.substring(j + 1, indexOfClosingBracket(s, j))));
                    i = indexOfClosingBracket(s, j);
                } else {
                    lineout.append(val);
                    i = j - 1;
                }
            }
        }

        private String processLine(String s, boolean deleteLineEndings) throws IOException {
            return lh.processLine(s, deleteLineEndings);
        }

        LineHelper lh = new LineHelper();

        public String processFuncMacro(FunctionMacro fm, String argString) throws IOException {
            LineHelper old = lh;
            try {
                lh = new LineHelper();
                argString = lh.processLine(argString, false);
                List bound = splitArgs(argString);
                if (bound.size() != fm.unbound.length) {
                    w("#define macro called with incorrect number of args");
                }
                String r = fm.expression;
                Iterator I = bound.iterator();
                for (int i = 0; I.hasNext(); i++) {
                    String b = ((String) I.next()).trim();
                    r = replaceAll(r, fm.unbound[i], b);
                }
                return r;
            } finally {
                lh = old;
            }
        }
    }

    public class FunctionMacro {

        public String[] unbound = new String[] {};

        public String expression = null;

        public FunctionMacro(String[] unbound, String expression) {
            this.expression = expression;
            this.unbound = unbound;
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
}
