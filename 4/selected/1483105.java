package org.retro.scheme;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;
import org.apache.log4j.BasicConfigurator;

/**
 * Default scheme object, mostly used as a set of static methods, it may be useful
 * for instantiating an object for stat checking.
 * <p>
 * <pre>
 *
 *  _record = new BotDatabaseRecord("somemsg");
 *  msg2 = "(x #f #f)";
 *  res = _scheme.getStringInput("(define bot-new-object (org.retro.scheme.BotSchemeObject. ))");
 *  _res = _scheme.getStringInput("(.setVector bot-new-object (list->vector '" + msg2 + "))");
 *  BotSchemeObject _o2 = (BotSchemeObject)_scheme.objectStringInput("bot-new-object");
 *  _record.setObjectField(_o2);
 *  _record.setMsg(msg2);
 *  _data.addRecord(_record);
 *
 * </pre>
 * 
 * @author Peter Norvig
 * @author Berlin Brown
 */
public class Scheme {

    /**
	 * If enabled, log all scheme messages for later access.
	 */
    private boolean logMessagesFlag = false;

    private StringBuffer logSchemeMessages = new StringBuffer();

    private Stack saveListObjects = null;

    private long incomingDataLen = 0;

    private long incomingLinesIndex = 0;

    public static String[] ARGS;

    public static boolean EXIT = false;

    public static InputPort input = new InputPort(System.in);

    public static PrintWriter output = new PrintWriter(System.out, true);

    public static PrintWriter error = new PrintWriter(System.err, true);

    public static Symbol EVAL = Symbol.intern("eval");

    public static Symbol LOAD = Symbol.intern("load");

    public static boolean INTERRUPTABLE = false;

    public static void interrupt(Thread t) {
        INTERRUPTABLE = true;
        t.interrupt();
    }

    public static void interruptCheck() {
        if (INTERRUPTABLE && Thread.interrupted()) {
            INTERRUPTABLE = false;
            throw new JschemeThrowable("Execution was interrupted.");
        }
    }

    /**
	 * Enable logging scheme messages only, for printing a full scheme set.
	 *
	 * @see #printLogMessages()
	 */
    public void setLogMessagesFlag(boolean b) {
        logMessagesFlag = b;
    }

    /**
	 * If the logging flag is set, return a string of all scheme messages sent to the object.
	 *
	 * @see #setLogMessagesFlag(boolean)
	 */
    public String printLogMessages() {
        return logSchemeMessages.toString();
    }

    public static DynamicEnvironment INTERACTION_ENVIRONMENT = new DynamicEnvironment();

    public static DynamicEnvironment NULL_ENVIRONMENT = new DynamicEnvironment();

    public static DynamicEnvironment INITIAL_ENVIRONMENT = null;

    public static DynamicEnvironment getInteractionEnvironment() {
        return INTERACTION_ENVIRONMENT;
    }

    public static DynamicEnvironment getNullEnvironment() {
        return NULL_ENVIRONMENT;
    }

    public static DynamicEnvironment getInitialEnvironment() {
        return INITIAL_ENVIRONMENT;
    }

    private static HashMap environmentCache = new HashMap();

    static {
        if (!Primitive.primitives_loaded) Primitive.loadPrimitives();
        INTERACTION_ENVIRONMENT.setValue(Symbol.intern("null"), null);
        NULL_ENVIRONMENT.lockDown();
        INITIAL_ENVIRONMENT = new DynamicEnvironment(INTERACTION_ENVIRONMENT);
        INITIAL_ENVIRONMENT.lockDown();
    }

    public Scheme() {
        BasicConfigurator.configure();
    }

    public static void main(String[] files) {
        ARGS = files;
        if (!Primitive.primitives_loaded) Primitive.loadPrimitives();
        INTERACTION_ENVIRONMENT.setValue(Symbol.intern("null"), null);
        NULL_ENVIRONMENT.lockDown();
        INITIAL_ENVIRONMENT = new DynamicEnvironment(INTERACTION_ENVIRONMENT);
        INITIAL_ENVIRONMENT.lockDown();
        if (!loadInit()) defaultMain(files);
    }

    private String stringifyCharStack(char ch) {
        StringBuffer buf = new StringBuffer();
        boolean quoted = false;
        switch(ch) {
            case '\b':
                buf.append(quoted ? "\\b" : "\b");
                break;
            case '\t':
                buf.append(quoted ? "\\t" : "\t");
                break;
            case '\n':
                buf.append(quoted ? "\\n" : "\n");
                break;
            case '\f':
                buf.append(quoted ? "\\f" : "\f");
                break;
            case '\r':
                buf.append(quoted ? "\\r" : "\r");
                break;
            case '\"':
                buf.append(quoted ? "\\\"" : "\"");
                break;
            case '\\':
                buf.append(quoted ? "\\\\" : "\\");
                break;
            default:
                buf.append(ch);
        }
        return buf.toString();
    }

    public int findProperObject(Object x) {
        boolean quoted = false;
        if (saveListObjects == null) {
            return -1;
        }
        if (x == Pair.EMPTY) {
            saveListObjects.push((String) "()");
        } else if (x == null) {
            saveListObjects.push("#null");
        } else if (x instanceof Boolean) {
            if (Boolean.TRUE.equals(x)) saveListObjects.push("#t"); else saveListObjects.push("#f");
        } else if (x == Boolean.TRUE) {
            saveListObjects.push("#t");
        } else if (x == Boolean.FALSE) {
            saveListObjects.push("#f");
        } else if (x instanceof Pair) {
            convertPairStack(saveListObjects);
        } else if (x instanceof Character) {
            saveListObjects.push(x);
        } else if (x instanceof String) {
            saveListObjects.push(x);
        } else if (x instanceof Object[]) {
            saveListObjects.push(x);
        } else if (x instanceof Number) {
            saveListObjects.push(x);
        } else {
            saveListObjects.push(x);
        }
        return -1;
    }

    public void convertPairStack(Stack _stk) {
        String special = null;
        if (special != null) {
            _stk.push(special);
        } else {
        }
    }

    /**
	 * Evalulate the string and return the object after evaluation, typical
	 * objects returned include Boolean, Pair, or Symbol.
	 * 
	 * @param _inscheme String to evaluate
	 * @see #getStringInput(String)
	 * @see #runEval(String)     
	 */
    public Object objectStringInput(String _inscheme) throws SchemeException {
        incomingDataLen += _inscheme.length();
        incomingLinesIndex++;
        InputPort inp = new InputPort(new StringReader(_inscheme));
        StringWriter _strOut = new StringWriter();
        PrintWriter _outWriter = new PrintWriter(new BufferedWriter(_strOut));
        if (logMessagesFlag) {
            logSchemeMessages.append(_inscheme + "\n");
        }
        return eval(inp.read());
    }

    /**
	 * This method is not attached to the Scheme object and is a quick
	 * and easy way to evaluate simple scheme expressions.
	 * 
	 * @param _in				Default Scheme Message to Evaluate
	 * @return 					The scheme evaluation of the input
	 * @throws SchemeException	The incoming message is invalid
	 */
    public static Object singleObjectStringInput(String _in) throws SchemeException {
        InputPort inp = new InputPort(new StringReader(_in));
        return eval(inp.read());
    }

    /**
	 * Get a java object array from the scheme list->vector call.
	 */
    public static Object vectorStringInput(String _in) throws SchemeException {
        StringBuffer buf = new StringBuffer();
        buf.append("(list->vector '" + _in + ")");
        InputPort inp = new InputPort(new StringReader(buf.toString()));
        return eval(inp.read());
    }

    /**
	 * This method may be useful,combined with <code>vectorToString</code>, it will
	 * convert an Object-Array filled with scheme.Symbols into a normal tokenized string.
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
    public static String vectorToString(Object obj) throws Exception {
        StringBuffer buf = new StringBuffer();
        if (obj instanceof Object[]) {
            Object[] v = (Object[]) obj;
            int i = 0;
            for (i = 0; i < v.length; i++) {
                if (v[i] instanceof Symbol) {
                    buf.append(((Symbol) v[i]).getName() + " ");
                }
            }
            return buf.toString().trim();
        } else {
            throw new Exception("Invalid Object Vector");
        }
    }

    /**
	 * A quick and dirty way of creating a bot-scheme-object, it may be more useful to
	 * use the Scheme object and getStringInput/objectStringInput as opposed to this
	 * static method.
	 *
	 * <p>
	 * Typical input includes a scheme list: (x #f #f)
	 * <pre>     
	 *  _res = _scheme.getStringInput("(define bot-new-object (org.retro.scheme.BotSchemeObject. ))");
	 * </pre>
	 *
	 * @see #getStringInput(String)     
	 */
    public static Object createBotSchemeObject(String msg) throws SchemeException {
        singleObjectStringInput("(define bot-new-object (org.retro.scheme.BotSchemeObject. ))");
        singleObjectStringInput("(.setVector bot-new-object (list->vector '" + msg + "))");
        BotSchemeObject o = (BotSchemeObject) singleObjectStringInput("bot-new-object");
        o.setMessage(msg);
        return o;
    }

    /**
	 * see runEval below for running multiple evaluations from a Scheme Object.
	 *
	 * @see #runEval(String)
	 * @see #objectStringInput(String)
	 */
    public String getStringInput(String _inscheme) throws SchemeException {
        incomingDataLen += _inscheme.length();
        incomingLinesIndex++;
        InputPort inp = new InputPort(new StringReader(_inscheme));
        StringWriter _strOut = new StringWriter();
        PrintWriter _outWriter = new PrintWriter(new BufferedWriter(_strOut));
        U.write(eval(inp.read()), _outWriter, false);
        inp = new InputPort(new StringReader(_inscheme));
        _outWriter.flush();
        if (logMessagesFlag) {
            logSchemeMessages.append(_inscheme + "\n");
        }
        return _strOut.toString();
    }

    /** 
	 * Use this to run multiple evaluations on a string
	 * see... getStringInput above for running single evaluation
	 *
	 * @author Berlin Brown
	 * @see #getStringInput(String)
	 */
    public String runEval(String _inscheme) throws SchemeException {
        incomingDataLen += _inscheme.length();
        incomingLinesIndex++;
        InputPort inp = new InputPort(new StringReader(_inscheme));
        StringWriter _strOut = new StringWriter();
        PrintWriter _outWriter = new PrintWriter(new BufferedWriter(_strOut));
        U.write(load(inp), _outWriter, false);
        _outWriter.flush();
        if (logMessagesFlag) {
            logSchemeMessages.append(_inscheme + "\n");
        }
        return _strOut.toString();
    }

    public static void defaultMain(String[] files) {
        String main = null;
        String[] mainArgs = null;
        int i = 0;
        while (i < (files == null ? 0 : files.length)) {
            if (files[i].startsWith("(")) load(new InputPort(new StringReader(files[i]))); else if (files[i].startsWith("-")) {
                if (files[i].equals("-s")) U.useJavaSyntax = false; else if (files[i].equals("-j")) U.useJavaSyntax = true; else if (files[i].equals("-main")) {
                    i = i + 1;
                    main = files[i];
                    mainArgs = consumeArgs(files, i + 1);
                    break;
                } else usage(files[i]);
            } else {
                load(files[i]);
            }
            i = i + 1;
        }
        if (main == null) {
            runJscheme();
        } else if (!main.equals("none")) {
            try {
                SI.call(main, mainArgs);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void usage(String arg) {
        System.out.println("usage: java org.retro.scheme.Scheme");
        System.exit(1);
    }

    private static String[] consumeArgs(String[] files, int i) {
        String[] result = new String[files.length - i];
        int j = 0;
        while (i < files.length) result[j++] = files[i++];
        return result;
    }

    public static void runJscheme() {
        if (EXIT) System.exit(0);
        Scheme.error.println("\n" + Version.VERSION + "\n");
        readEvalWriteLoop("> ");
        System.exit(0);
    }

    public static boolean loadInit() {
        InputPort in = open("init.scm");
        if (in != null) {
            load(in);
            return true;
        } else return false;
    }

    public static void readEvalWriteLoop(String prompt) {
        Object x;
        if (!EXIT) for (; ; ) {
            try {
                if (EXIT) break;
                output.print(prompt);
                output.flush();
                if ((x = input.read()) == InputPort.EOF) break;
                U.write(eval(x), output, true);
                output.println();
                output.flush();
            } catch (Throwable e) {
                e.printStackTrace(error);
            }
        }
    }

    public static Object load(Object fileName) {
        String name = fileName.toString();
        InputPort iport = open(name);
        if (iport == null) {
            return E.warn("(load) can't open \"" + fileName + "\"");
        } else {
            return load(iport);
        }
    }

    private static DynamicEnvironment loadEnvironment(String cacheKey, String filename, DynamicEnvironment newEnv, HashMap environmentCache) {
        InputPort iport = open(filename);
        if (iport == null) {
            E.warn("can't open \"" + filename + "\"");
            return null;
        } else {
            load(iport);
            newEnv.lockDown();
            environmentCache.put(cacheKey, newEnv);
            return newEnv;
        }
    }

    private static DynamicEnvironment loadEnvironment(Class c, DynamicEnvironment newEnv, HashMap environmentCache) {
        Invoke.invokeStatic(c, "load", new Object[] {});
        newEnv.lockDown();
        environmentCache.put(c, newEnv);
        return newEnv;
    }

    public static DynamicEnvironment loadEnvironment(Object x) {
        Object cacheKey = x;
        Object cached = environmentCache.get(cacheKey);
        try {
            if (x instanceof String) {
                cacheKey = (new java.io.File((String) x)).getCanonicalFile().toString().intern();
                cached = environmentCache.get(cacheKey);
            }
        } catch (Exception e) {
            return null;
        }
        if (cached != null) return (DynamicEnvironment) cached; else {
            synchronized (INTERACTION_ENVIRONMENT) {
                DynamicEnvironment prevEnv = INTERACTION_ENVIRONMENT;
                try {
                    DynamicEnvironment newEnv = new DynamicEnvironment(INITIAL_ENVIRONMENT);
                    INTERACTION_ENVIRONMENT = newEnv;
                    if (x instanceof String) return loadEnvironment((String) cacheKey, (String) x, newEnv, environmentCache); else if (x instanceof Class) return loadEnvironment((Class) x, newEnv, environmentCache); else {
                        E.error("ERROR: not a string or class: " + x);
                        return null;
                    }
                } finally {
                    INTERACTION_ENVIRONMENT = prevEnv;
                }
            }
        }
    }

    public static Boolean environmentImport(Object x, Object prefix) {
        synchronized (INTERACTION_ENVIRONMENT) {
            DynamicEnvironment env = loadEnvironment(x);
            if ((prefix instanceof String)) {
                INTERACTION_ENVIRONMENT.importBindings(env, (String) prefix);
                return Boolean.TRUE;
            } else if ((prefix == U.MISSING) || ((prefix instanceof Boolean) && ((Boolean) prefix) == Boolean.FALSE)) {
                INTERACTION_ENVIRONMENT.importBindings(env, null);
                return Boolean.TRUE;
            } else {
                E.error("(environment-import): prefix is not string or #f: " + prefix);
                return Boolean.FALSE;
            }
        }
    }

    public static Boolean languageImport(Object x) {
        synchronized (INTERACTION_ENVIRONMENT) {
            DynamicEnvironment env = loadEnvironment(x);
            INTERACTION_ENVIRONMENT.importBindings(env, null, true);
            return Boolean.TRUE;
        }
    }

    public static InputPort open(String name) {
        InputPort ip = null;
        if (ip == null) ip = openFile(name);
        if (ip == null) ip = openResource(name);
        if (ip == null) ip = openURL(name);
        return ip;
    }

    public static InputPort openURL(String url) {
        try {
            return new InputPort((InputStream) (new URL(url)).getContent());
        } catch (java.net.MalformedURLException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static InputPort openFile(String name) {
        try {
            return new InputPort(new FileInputStream(name));
        } catch (java.io.IOException fnf) {
            return null;
        } catch (SecurityException se) {
            return null;
        } catch (Throwable e) {
            e.printStackTrace(error);
            return null;
        }
    }

    public static InputPort openResource(String name) {
        try {
            ClassLoader loader = Import.getClassLoader();
            InputStream stream = (loader == null) ? ClassLoader.getSystemResourceAsStream(name) : loader.getResourceAsStream(name);
            return (stream == null) ? null : new InputPort(stream);
        } catch (Throwable e) {
            System.err.println("In openResource(" + name + "):");
            e.printStackTrace(error);
            return null;
        }
    }

    public static Object load(InputPort in) {
        while (true) {
            if (EXIT) {
                System.exit(0);
            }
            try {
                Object x = in.read();
                if (x == InputPort.EOF) return U.TRUE; else evalToplevel(x, INTERACTION_ENVIRONMENT);
            } catch (Exception e) {
                E.warn("Error during load (lineno " + in.getLineNumber() + "): ", e);
                e.printStackTrace(error);
            }
        }
    }

    public static Object evalToplevel(Object x, DynamicEnvironment env) {
        if (U.isPair(x)) {
            Object mx = Macro.expand((Pair) x);
            if (x != mx) {
                return evalToplevel(mx, env);
            } else if (U.first(x) == Symbol.BEGIN) {
                Object xs = U.rest(x);
                Object result = null;
                while (U.isPair(xs)) {
                    result = eval(U.first(xs), env);
                    xs = U.rest(xs);
                }
                return result;
            } else return eval(x, env);
        } else return eval(x, env);
    }

    public static Object eval(Object x) {
        return eval(x, Scheme.INTERACTION_ENVIRONMENT);
    }

    public static Object eval(Object x, Object env) {
        DynamicEnvironment dynamicEnv = ((env == U.MISSING) ? INTERACTION_ENVIRONMENT : (DynamicEnvironment) env);
        Object analyzedCode = analyze(x, dynamicEnv, LexicalEnvironment.NULLENV);
        return execute(analyzedCode, LexicalEnvironment.NULLENV);
    }

    public static Object analyze(Object x, DynamicEnvironment dynamicEnv, LexicalEnvironment lexenv) {
        if (x instanceof Symbol) {
            LocalVariable localvar = lexenv.lookup((Symbol) x);
            if (localvar == null) return new DynamicVariable((Symbol) x, dynamicEnv); else return localvar;
        } else if (!U.isPair(x)) return new Object[] { Symbol.QUOTE, x }; else {
            Object f = U.first(x), xm;
            int len = ((Pair) x).length();
            if (Symbol.LAMBDA == f && len >= 3) {
                Object args = U.second(x);
                LexicalEnvironment lexenv2 = new LexicalEnvironment(args, null, lexenv);
                return new Closure(args, analyze(toBody(U.rest(U.rest(x))), dynamicEnv, lexenv2), lexenv);
            } else if (Symbol.MACRO == f && len >= 3) {
                Object args = U.second(x);
                LexicalEnvironment lexenv2 = new LexicalEnvironment(args, null, lexenv);
                return new Macro(args, analyze(toBody(U.rest(U.rest(x))), dynamicEnv, lexenv2), lexenv);
            } else if (2 == len && Symbol.BEGIN == f) return analyze(U.second(x), dynamicEnv, lexenv); else if (x != (xm = Macro.expand((Pair) x))) return analyze(xm, dynamicEnv, lexenv); else if (Symbol.OR == f && len == 1) return new Object[] { Symbol.QUOTE, U.FALSE }; else if (Symbol.IF == f && len == 3) return analyze(U.append(U.list(x, U.list(U.FALSE))), dynamicEnv, lexenv); else if (Symbol.QUOTE == f && len == 2) return new Object[] { Symbol.QUOTE, U.second(x) }; else {
                checkLength(f, len, x);
                Object[] xv = U.listToVector(x);
                if (!isSpecial(f)) xv[0] = analyze(xv[0], dynamicEnv, lexenv);
                for (int i = 1; i < xv.length; i++) {
                    xv[i] = analyze(xv[i], dynamicEnv, lexenv);
                }
                return xv;
            }
        }
    }

    public static Object execute(Object x, LexicalEnvironment lexenv) {
        do {
            if (!(x instanceof Object[])) {
                if (x instanceof DynamicVariable) return ((DynamicVariable) x).getDynamicValue(); else if (x instanceof LocalVariable) return lexenv.get((LocalVariable) x); else return ((Closure) x).copy(lexenv);
            } else {
                Object[] xv = (Object[]) x;
                Object f = xv[0];
                if (f == Symbol.QUOTE) return xv[1]; else if (f == Symbol.IF) x = (U.to_bool(execute(xv[1], lexenv))) ? xv[2] : xv[3]; else if (f == Symbol.BEGIN) x = executeButLast(xv, lexenv); else if (f == Symbol.OR) {
                    int xvlm1 = xv.length - 1;
                    for (int i = 1; i < xvlm1; i++) {
                        Object result = execute(xv[i], lexenv);
                        if (U.toBool(result) != U.FALSE) return result;
                    }
                    x = xv[xvlm1];
                } else if (f == Symbol.SET && xv[1] instanceof DynamicVariable) return ((DynamicVariable) xv[1]).setDynamicValue(execute(xv[2], lexenv)); else if (f == Symbol.SET && xv[1] instanceof LocalVariable) return lexenv.set((LocalVariable) xv[1], execute(xv[2], lexenv)); else {
                    if (INTERRUPTABLE) interruptCheck();
                    try {
                        f = executef(f, lexenv);
                        if (f instanceof Closure) {
                            Closure c = (Closure) f;
                            x = c.body;
                            lexenv = new LexicalEnvironment(c.parms, c.makeArgArray(xv, lexenv), c.lexenv);
                        } else {
                            Procedure p = U.toProc(f);
                            return p.apply(p.makeArgArray(xv, lexenv));
                        }
                    } catch (RuntimeException e) {
                        if (e.getMessage() == "continuation") throw e;
                        if ((e instanceof JschemeThrowable) && (e.getMessage() == null)) throw e; else throw new BacktraceException(e, xv, lexenv);
                    }
                }
            }
        } while (true);
    }

    private static Object executef(Object x, LexicalEnvironment lexenv) {
        if (x instanceof DynamicVariable) return ((DynamicVariable) x).getDynamicValue(); else if (x instanceof LocalVariable) return lexenv.get((LocalVariable) x); else if (x instanceof Closure) return ((Closure) x).copy(lexenv); else return execute(x, lexenv);
    }

    private static boolean isSpecial(Object f) {
        return f == Symbol.SET || f == Symbol.IF || f == Symbol.BEGIN || f == Symbol.OR || f == Symbol.QUOTE;
    }

    private static void checkLength(Object f, int len, Object x) {
        if ((f == Symbol.LAMBDA && len < 3) || (f == Symbol.MACRO && len < 3) || (f == Symbol.SET && len != 3) || (f == Symbol.IF && len != 4) || (f == Symbol.BEGIN && len <= 2) || (f == Symbol.OR && len < 2) || (f == Symbol.QUOTE && len != 2)) E.warn("wrong number of arguments for syntax:", x);
    }

    private static Object toBody(Object exps) {
        Pair parts = extractDefines(Pair.EMPTY, U.toPair(exps));
        Pair defines = ((Pair) parts.first);
        Pair body = ((Pair) parts.rest);
        if (U.isPair(defines)) {
            Pair vars = Pair.EMPTY;
            Pair sets = Pair.EMPTY;
            Pair vals = Pair.EMPTY;
            Pair ds = defines;
            while (U.isPair(ds)) {
                Pair d = ((Pair) ds.first);
                ds = ((Pair) ds.rest);
                vars = new Pair(U.second(d), vars);
                sets = new Pair(U.list(Symbol.SET, d.second(), d.third()), sets);
                vals = new Pair(U.FALSE, vals);
            }
            Pair begin = new Pair(Symbol.BEGIN, U.append(U.list(sets.reverse(), body)));
            return new Pair(U.list(Symbol.LAMBDA, vars.reverse(), begin), vals);
        } else return new Pair(Symbol.BEGIN, body);
    }

    private static Pair extractDefines(Pair defines, Pair body) {
        if (!U.isPair(body)) return new Pair(defines.reverse(), body); else if (startsWith(body.first, Symbol.BEGIN)) return extractDefines(defines, (Pair) U.append(U.list(U.rest(U.first(body)), body.rest))); else if (startsWith(body.first, Symbol.DEFINE)) return extractDefines(new Pair(simplifyDefine(((Pair) body.first)), defines), U.toList(body.rest)); else return new Pair(defines.reverse(), checkForDefines(body));
    }

    private static Object checkForDefines(Pair body) {
        if (!U.isPair(body)) return body; else if (startsWith(body.first, Symbol.BEGIN)) return checkForDefines((Pair) U.append(U.list(U.rest(U.first(body)), body.rest))); else if (startsWith(body.first, Symbol.DEFINE)) {
            return E.error("Jscheme requires all embedded defines to appear first in procedure bodies\n" + "You must move " + U.stringify(U.first(body)) + " up\n");
        } else return new Pair(body.first, checkForDefines((Pair) body.rest));
    }

    private static boolean startsWith(Object list, Object atom) {
        return (U.isPair(list)) && (U.first(list) == atom);
    }

    private static Pair simplifyDefine(Pair definition) {
        Object var = U.second(definition);
        if (var instanceof Pair) {
            Pair var2 = (Pair) var;
            Object name = var2.first;
            Object args = var2.rest;
            Object body = U.rest(U.rest(definition));
            return new Pair(Symbol.DEFINE, new Pair(name, U.list(new Pair(Symbol.LAMBDA, new Pair(args, body)))));
        } else return definition;
    }

    private static Object executeButLast(Object[] xv, LexicalEnvironment lexenv) {
        for (int i = 1; i < xv.length - 1; i++) {
            execute(xv[i], lexenv);
        }
        return xv[xv.length - 1];
    }

    public static final Object[] makeArgArray(Procedure p, Pair args) {
        Object[] argArray = new Object[p.nParms()];
        int nargs = args.length();
        if (nargs < p.minArgs) E.error("\nToo few arguments to procedure " + p.name + " expected at least " + p.minArgs + ", but found " + nargs + " arguments:\n***************\n    " + U.stringify(args) + "\n************\n");
        if (nargs > p.maxArgs) E.error("\nToo many arguments to procedure " + p.name + " expected at most " + p.maxArgs + ", but found " + nargs + " arguments:\n***************\n    " + U.stringify(args) + "\n************\n");
        for (int i = 0; i < p.minArgs; i++, args = U.toList(args.rest)) {
            argArray[i] = args.first;
        }
        if (p.maxArgs > p.minArgs) {
            if (p.maxArgs == p.minArgs + 1) argArray[p.minArgs] = (U.isPair(args)) ? args.first : U.MISSING; else argArray[p.minArgs] = args;
        }
        return argArray;
    }
}
