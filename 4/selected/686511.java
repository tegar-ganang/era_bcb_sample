package clojure.lang;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.*;
import java.util.regex.Matcher;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.net.URL;
import java.net.JarURLConnection;
import java.nio.charset.Charset;

public class RT {

    public static final Boolean T = Boolean.TRUE;

    public static final Boolean F = Boolean.FALSE;

    public static final String LOADER_SUFFIX = "__init";

    static final IPersistentMap DEFAULT_IMPORTS = map(Symbol.create("Boolean"), Boolean.class, Symbol.create("Byte"), Byte.class, Symbol.create("Character"), Character.class, Symbol.create("Class"), Class.class, Symbol.create("ClassLoader"), ClassLoader.class, Symbol.create("Compiler"), Compiler.class, Symbol.create("Double"), Double.class, Symbol.create("Enum"), Enum.class, Symbol.create("Float"), Float.class, Symbol.create("InheritableThreadLocal"), InheritableThreadLocal.class, Symbol.create("Integer"), Integer.class, Symbol.create("Long"), Long.class, Symbol.create("Math"), Math.class, Symbol.create("Number"), Number.class, Symbol.create("Object"), Object.class, Symbol.create("Package"), Package.class, Symbol.create("Process"), Process.class, Symbol.create("ProcessBuilder"), ProcessBuilder.class, Symbol.create("Runtime"), Runtime.class, Symbol.create("RuntimePermission"), RuntimePermission.class, Symbol.create("SecurityManager"), SecurityManager.class, Symbol.create("Short"), Short.class, Symbol.create("StackTraceElement"), StackTraceElement.class, Symbol.create("StrictMath"), StrictMath.class, Symbol.create("String"), String.class, Symbol.create("StringBuffer"), StringBuffer.class, Symbol.create("StringBuilder"), StringBuilder.class, Symbol.create("System"), System.class, Symbol.create("Thread"), Thread.class, Symbol.create("ThreadGroup"), ThreadGroup.class, Symbol.create("ThreadLocal"), ThreadLocal.class, Symbol.create("Throwable"), Throwable.class, Symbol.create("Void"), Void.class, Symbol.create("Appendable"), Appendable.class, Symbol.create("CharSequence"), CharSequence.class, Symbol.create("Cloneable"), Cloneable.class, Symbol.create("Comparable"), Comparable.class, Symbol.create("Iterable"), Iterable.class, Symbol.create("Readable"), Readable.class, Symbol.create("Runnable"), Runnable.class, Symbol.create("Callable"), Callable.class, Symbol.create("BigInteger"), BigInteger.class, Symbol.create("BigDecimal"), BigDecimal.class, Symbol.create("ArithmeticException"), ArithmeticException.class, Symbol.create("ArrayIndexOutOfBoundsException"), ArrayIndexOutOfBoundsException.class, Symbol.create("ArrayStoreException"), ArrayStoreException.class, Symbol.create("ClassCastException"), ClassCastException.class, Symbol.create("ClassNotFoundException"), ClassNotFoundException.class, Symbol.create("CloneNotSupportedException"), CloneNotSupportedException.class, Symbol.create("EnumConstantNotPresentException"), EnumConstantNotPresentException.class, Symbol.create("Exception"), Exception.class, Symbol.create("IllegalAccessException"), IllegalAccessException.class, Symbol.create("IllegalArgumentException"), IllegalArgumentException.class, Symbol.create("IllegalMonitorStateException"), IllegalMonitorStateException.class, Symbol.create("IllegalStateException"), IllegalStateException.class, Symbol.create("IllegalThreadStateException"), IllegalThreadStateException.class, Symbol.create("IndexOutOfBoundsException"), IndexOutOfBoundsException.class, Symbol.create("InstantiationException"), InstantiationException.class, Symbol.create("InterruptedException"), InterruptedException.class, Symbol.create("NegativeArraySizeException"), NegativeArraySizeException.class, Symbol.create("NoSuchFieldException"), NoSuchFieldException.class, Symbol.create("NoSuchMethodException"), NoSuchMethodException.class, Symbol.create("NullPointerException"), NullPointerException.class, Symbol.create("NumberFormatException"), NumberFormatException.class, Symbol.create("RuntimeException"), RuntimeException.class, Symbol.create("SecurityException"), SecurityException.class, Symbol.create("StringIndexOutOfBoundsException"), StringIndexOutOfBoundsException.class, Symbol.create("TypeNotPresentException"), TypeNotPresentException.class, Symbol.create("UnsupportedOperationException"), UnsupportedOperationException.class, Symbol.create("AbstractMethodError"), AbstractMethodError.class, Symbol.create("AssertionError"), AssertionError.class, Symbol.create("ClassCircularityError"), ClassCircularityError.class, Symbol.create("ClassFormatError"), ClassFormatError.class, Symbol.create("Error"), Error.class, Symbol.create("ExceptionInInitializerError"), ExceptionInInitializerError.class, Symbol.create("IllegalAccessError"), IllegalAccessError.class, Symbol.create("IncompatibleClassChangeError"), IncompatibleClassChangeError.class, Symbol.create("InstantiationError"), InstantiationError.class, Symbol.create("InternalError"), InternalError.class, Symbol.create("LinkageError"), LinkageError.class, Symbol.create("NoClassDefFoundError"), NoClassDefFoundError.class, Symbol.create("NoSuchFieldError"), NoSuchFieldError.class, Symbol.create("NoSuchMethodError"), NoSuchMethodError.class, Symbol.create("OutOfMemoryError"), OutOfMemoryError.class, Symbol.create("StackOverflowError"), StackOverflowError.class, Symbol.create("ThreadDeath"), ThreadDeath.class, Symbol.create("UnknownError"), UnknownError.class, Symbol.create("UnsatisfiedLinkError"), UnsatisfiedLinkError.class, Symbol.create("UnsupportedClassVersionError"), UnsupportedClassVersionError.class, Symbol.create("VerifyError"), VerifyError.class, Symbol.create("VirtualMachineError"), VirtualMachineError.class, Symbol.create("Thread$UncaughtExceptionHandler"), Thread.UncaughtExceptionHandler.class, Symbol.create("Thread$State"), Thread.State.class, Symbol.create("Deprecated"), Deprecated.class, Symbol.create("Override"), Override.class, Symbol.create("SuppressWarnings"), SuppressWarnings.class);

    public static Charset UTF8 = Charset.forName("UTF-8");

    public static final Namespace CLOJURE_NS = Namespace.findOrCreate(Symbol.create("clojure.core"));

    public static final Var OUT = Var.intern(CLOJURE_NS, Symbol.create("*out*"), new OutputStreamWriter(System.out, UTF8));

    public static final Var IN = Var.intern(CLOJURE_NS, Symbol.create("*in*"), new LineNumberingPushbackReader(new InputStreamReader(System.in, UTF8)));

    public static final Var ERR = Var.intern(CLOJURE_NS, Symbol.create("*err*"), new PrintWriter(new OutputStreamWriter(System.err, UTF8), true));

    static final Keyword TAG_KEY = Keyword.intern(null, "tag");

    public static final Var AGENT = Var.intern(CLOJURE_NS, Symbol.create("*agent*"), null);

    public static final Var MACRO_META = Var.intern(CLOJURE_NS, Symbol.create("*macro-meta*"), null);

    public static final Var MATH_CONTEXT = Var.intern(CLOJURE_NS, Symbol.create("*math-context*"), null);

    static Keyword LINE_KEY = Keyword.intern(null, "line");

    static Keyword FILE_KEY = Keyword.intern(null, "file");

    public static final Var USE_CONTEXT_CLASSLOADER = Var.intern(CLOJURE_NS, Symbol.create("*use-context-classloader*"), null);

    static final Symbol LOAD_FILE = Symbol.create("load-file");

    static final Symbol IN_NAMESPACE = Symbol.create("in-ns");

    static final Symbol NAMESPACE = Symbol.create("ns");

    static final Symbol IDENTICAL = Symbol.create("identical?");

    static final Var CMD_LINE_ARGS = Var.intern(CLOJURE_NS, Symbol.create("*command-line-args*"), null);

    public static final Var CURRENT_NS = Var.intern(CLOJURE_NS, Symbol.create("*ns*"), CLOJURE_NS);

    static final Var FLUSH_ON_NEWLINE = Var.intern(CLOJURE_NS, Symbol.create("*flush-on-newline*"), T);

    static final Var PRINT_META = Var.intern(CLOJURE_NS, Symbol.create("*print-meta*"), F);

    static final Var PRINT_READABLY = Var.intern(CLOJURE_NS, Symbol.create("*print-readably*"), T);

    static final Var PRINT_DUP = Var.intern(CLOJURE_NS, Symbol.create("*print-dup*"), F);

    static final Var WARN_ON_REFLECTION = Var.intern(CLOJURE_NS, Symbol.create("*warn-on-reflection*"), F);

    static final Var ALLOW_UNRESOLVED_VARS = Var.intern(CLOJURE_NS, Symbol.create("*allow-unresolved-vars*"), F);

    static final Var IN_NS_VAR = Var.intern(CLOJURE_NS, Symbol.create("in-ns"), F);

    static final Var NS_VAR = Var.intern(CLOJURE_NS, Symbol.create("ns"), F);

    static final Var PRINT_INITIALIZED = Var.intern(CLOJURE_NS, Symbol.create("print-initialized"));

    static final Var PR_ON = Var.intern(CLOJURE_NS, Symbol.create("pr-on"));

    static final IFn inNamespace = new AFn() {

        public Object invoke(Object arg1) throws Exception {
            Symbol nsname = (Symbol) arg1;
            Namespace ns = Namespace.findOrCreate(nsname);
            CURRENT_NS.set(ns);
            return ns;
        }
    };

    public static List<String> processCommandLine(String[] args) {
        List<String> arglist = Arrays.asList(args);
        int split = arglist.indexOf("--");
        if (split >= 0) {
            CMD_LINE_ARGS.bindRoot(RT.seq(arglist.subList(split + 1, args.length)));
            return arglist.subList(0, split);
        }
        return arglist;
    }

    public static final Object[] EMPTY_ARRAY = new Object[] {};

    public static final Comparator DEFAULT_COMPARATOR = new Comparator() {

        public int compare(Object o1, Object o2) {
            return Util.compare(o1, o2);
        }
    };

    static AtomicInteger id = new AtomicInteger(1);

    private static DynamicClassLoader ROOT_CLASSLOADER = null;

    public static void addURL(Object url) throws Exception {
        URL u = (url instanceof String) ? (new URL((String) url)) : (URL) url;
        getRootClassLoader().addURL(u);
    }

    public static final Object EOS = new Object();

    public static final Object SKIP = new Object();

    public static final IFn EMPTY_GEN = new AFn() {

        public synchronized Object invoke() throws Exception {
            return EOS;
        }
    };

    static {
        Keyword dockw = Keyword.intern(null, "doc");
        Keyword arglistskw = Keyword.intern(null, "arglists");
        Symbol namesym = Symbol.create("name");
        OUT.setTag(Symbol.create("java.io.Writer"));
        CURRENT_NS.setTag(Symbol.create("clojure.lang.Namespace"));
        AGENT.setMeta(map(dockw, "The agent currently running an action on this thread, else nil"));
        AGENT.setTag(Symbol.create("clojure.lang.Agent"));
        MATH_CONTEXT.setTag(Symbol.create("java.math.MathContext"));
        Var nv = Var.intern(CLOJURE_NS, NAMESPACE, inNamespace);
        nv.setMacro();
        Var v;
        v = Var.intern(CLOJURE_NS, IN_NAMESPACE, inNamespace);
        v.setMeta(map(dockw, "Sets *ns* to the namespace named by the symbol, creating it if needed.", arglistskw, list(vector(namesym))));
        v = Var.intern(CLOJURE_NS, LOAD_FILE, new AFn() {

            public Object invoke(Object arg1) throws Exception {
                return Compiler.loadFile((String) arg1);
            }
        });
        v.setMeta(map(dockw, "Sequentially read and evaluate the set of forms contained in the file.", arglistskw, list(vector(namesym))));
        v = Var.intern(CLOJURE_NS, IDENTICAL, new AFn() {

            public Object invoke(Object arg1, Object arg2) throws Exception {
                return arg1 == arg2 ? RT.T : RT.F;
            }
        });
        v.setMeta(map(dockw, "Tests if 2 arguments are the same object", arglistskw, list(vector(Symbol.create("x"), Symbol.create("y")))));
        try {
            doInit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Var var(String ns, String name) {
        return Var.intern(Namespace.findOrCreate(Symbol.intern(null, ns)), Symbol.intern(null, name));
    }

    public static Var var(String ns, String name, Object init) {
        return Var.intern(Namespace.findOrCreate(Symbol.intern(null, ns)), Symbol.intern(null, name), init);
    }

    public static void loadResourceScript(String name) throws Exception {
        loadResourceScript(name, true);
    }

    public static void maybeLoadResourceScript(String name) throws Exception {
        loadResourceScript(name, false);
    }

    public static void loadResourceScript(String name, boolean failIfNotFound) throws Exception {
        loadResourceScript(RT.class, name, failIfNotFound);
    }

    public static void loadResourceScript(Class c, String name) throws Exception {
        loadResourceScript(c, name, true);
    }

    public static void loadResourceScript(Class c, String name, boolean failIfNotFound) throws Exception {
        int slash = name.lastIndexOf('/');
        String file = slash >= 0 ? name.substring(slash + 1) : name;
        InputStream ins = baseLoader().getResourceAsStream(name);
        if (ins != null) {
            try {
                Compiler.load(new InputStreamReader(ins, UTF8), name, file);
            } finally {
                ins.close();
            }
        } else if (failIfNotFound) {
            throw new FileNotFoundException("Could not locate Clojure resource on classpath: " + name);
        }
    }

    public static void init() throws Exception {
        ((PrintWriter) RT.ERR.deref()).println("No need to call RT.init() anymore");
    }

    public static long lastModified(URL url, String libfile) throws Exception {
        if (url.getProtocol().equals("jar")) {
            return ((JarURLConnection) url.openConnection()).getJarFile().getEntry(libfile).getTime();
        } else {
            return url.openConnection().getLastModified();
        }
    }

    static void compile(String cljfile) throws Exception {
        InputStream ins = baseLoader().getResourceAsStream(cljfile);
        if (ins != null) {
            try {
                Compiler.compile(new InputStreamReader(ins, UTF8), cljfile, cljfile.substring(1 + cljfile.lastIndexOf("/")));
            } finally {
                ins.close();
            }
        } else throw new FileNotFoundException("Could not locate Clojure resource on classpath: " + cljfile);
    }

    public static void load(String scriptbase) throws Exception {
        load(scriptbase, true);
    }

    public static void load(String scriptbase, boolean failIfNotFound) throws Exception {
        String classfile = scriptbase + LOADER_SUFFIX + ".class";
        String cljfile = scriptbase + ".clj";
        URL classURL = baseLoader().getResource(classfile);
        URL cljURL = baseLoader().getResource(cljfile);
        boolean loaded = false;
        if ((classURL != null && (cljURL == null || lastModified(classURL, classfile) > lastModified(cljURL, cljfile))) || classURL == null) {
            try {
                Var.pushThreadBindings(RT.map(CURRENT_NS, CURRENT_NS.deref(), WARN_ON_REFLECTION, WARN_ON_REFLECTION.deref()));
                loaded = (loadClassForName(scriptbase.replace('/', '.') + LOADER_SUFFIX) != null);
            } finally {
                Var.popThreadBindings();
            }
        }
        if (!loaded && cljURL != null) {
            if (booleanCast(Compiler.COMPILE_FILES.deref())) compile(cljfile); else loadResourceScript(RT.class, cljfile);
        } else if (!loaded && failIfNotFound) throw new FileNotFoundException(String.format("Could not locate %s or %s on classpath: ", classfile, cljfile));
    }

    static void doInit() throws Exception {
        load("clojure/core");
        load("clojure/zip", false);
        load("clojure/xml", false);
        load("clojure/set", false);
        Var.pushThreadBindings(RT.map(CURRENT_NS, CURRENT_NS.deref(), WARN_ON_REFLECTION, WARN_ON_REFLECTION.deref()));
        try {
            Symbol USER = Symbol.create("user");
            Symbol CLOJURE = Symbol.create("clojure.core");
            Var in_ns = var("clojure.core", "in-ns");
            Var refer = var("clojure.core", "refer");
            in_ns.invoke(USER);
            refer.invoke(CLOJURE);
            maybeLoadResourceScript("user.clj");
        } finally {
            Var.popThreadBindings();
        }
    }

    public static int nextID() {
        return id.getAndIncrement();
    }

    public static ISeq seq(Object coll) {
        if (coll instanceof ASeq) return (ASeq) coll; else if (coll instanceof LazySeq) return ((LazySeq) coll).seq(); else return seqFrom(coll);
    }

    public static Stream stream(final Object coll) throws Exception {
        if (coll == null) return new Stream(EMPTY_GEN); else if (coll instanceof Streamable) return ((Streamable) coll).stream(); else if (coll instanceof Fn) return new Stream((IFn) coll); else if (coll instanceof Iterable) return new Stream(new IteratorStream(((Iterable) coll).iterator())); else if (coll.getClass().isArray()) return ArrayStream.createFromObject(coll); else if (coll instanceof String) return ArrayStream.createFromObject(((String) coll).toCharArray()); else return new Stream(new ASeq.Src(RT.seq(coll)));
    }

    static ISeq seqFrom(Object coll) {
        if (coll instanceof Seqable) return ((Seqable) coll).seq(); else if (coll == null) return null; else if (coll instanceof Iterable) return IteratorSeq.create(((Iterable) coll).iterator()); else if (coll.getClass().isArray()) return ArraySeq.createFromObject(coll); else if (coll instanceof String) return StringSeq.create((String) coll); else if (coll instanceof Map) return seq(((Map) coll).entrySet()); else {
            Class c = coll.getClass();
            Class sc = c.getSuperclass();
            throw new IllegalArgumentException("Don't know how to create ISeq from: " + c.getSimpleName());
        }
    }

    public static ISeq keys(Object coll) {
        return APersistentMap.KeySeq.create(seq(coll));
    }

    public static ISeq vals(Object coll) {
        return APersistentMap.ValSeq.create(seq(coll));
    }

    public static IPersistentMap meta(Object x) {
        if (x instanceof IMeta) return ((IMeta) x).meta();
        return null;
    }

    public static int count(Object o) {
        if (o == null) return 0; else if (o instanceof Counted) return ((Counted) o).count(); else if (o instanceof IPersistentCollection) {
            ISeq s = seq(o);
            o = null;
            int i = 0;
            for (; s != null; s = s.next()) {
                if (s instanceof Counted) return i + s.count();
                i++;
            }
            return i;
        } else if (o instanceof String) return ((String) o).length(); else if (o instanceof Collection) return ((Collection) o).size(); else if (o instanceof Map) return ((Map) o).size(); else if (o.getClass().isArray()) return Array.getLength(o);
        throw new UnsupportedOperationException("count not supported on this type: " + o.getClass().getSimpleName());
    }

    public static IPersistentCollection conj(IPersistentCollection coll, Object x) {
        if (coll == null) return new PersistentList(x);
        return coll.cons(x);
    }

    public static ISeq cons(Object x, Object coll) {
        if (coll == null) return new PersistentList(x); else if (coll instanceof ISeq) return new Cons(x, (ISeq) coll); else return new Cons(x, seq(coll));
    }

    public static Object first(Object x) {
        if (x instanceof ISeq) return ((ISeq) x).first();
        ISeq seq = seq(x);
        if (seq == null) return null;
        return seq.first();
    }

    public static Object second(Object x) {
        return first(next(x));
    }

    public static Object third(Object x) {
        return first(next(next(x)));
    }

    public static Object fourth(Object x) {
        return first(next(next(next(x))));
    }

    public static ISeq next(Object x) {
        if (x instanceof ISeq) return ((ISeq) x).next();
        ISeq seq = seq(x);
        if (seq == null) return null;
        return seq.next();
    }

    public static ISeq more(Object x) {
        if (x instanceof ISeq) return ((ISeq) x).more();
        ISeq seq = seq(x);
        if (seq == null) return PersistentList.EMPTY;
        return seq.more();
    }

    public static Object peek(Object x) {
        if (x == null) return null;
        return ((IPersistentStack) x).peek();
    }

    public static Object pop(Object x) {
        if (x == null) return null;
        return ((IPersistentStack) x).pop();
    }

    public static Object get(Object coll, Object key) {
        if (coll == null) return null; else if (coll instanceof Associative) return ((Associative) coll).valAt(key); else if (coll instanceof Map) {
            Map m = (Map) coll;
            return m.get(key);
        } else if (coll instanceof IPersistentSet) {
            IPersistentSet set = (IPersistentSet) coll;
            return set.get(key);
        } else if (key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
            int n = ((Number) key).intValue();
            if (n >= 0 && n < count(coll)) return nth(coll, n);
            return null;
        }
        return null;
    }

    public static Object get(Object coll, Object key, Object notFound) {
        if (coll == null) return notFound; else if (coll instanceof Associative) return ((Associative) coll).valAt(key, notFound); else if (coll instanceof Map) {
            Map m = (Map) coll;
            if (m.containsKey(key)) return m.get(key);
            return notFound;
        } else if (coll instanceof IPersistentSet) {
            IPersistentSet set = (IPersistentSet) coll;
            if (set.contains(key)) return set.get(key);
            return notFound;
        } else if (key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
            int n = ((Number) key).intValue();
            return n >= 0 && n < count(coll) ? nth(coll, n) : notFound;
        }
        return notFound;
    }

    public static Associative assoc(Object coll, Object key, Object val) {
        if (coll == null) return new PersistentArrayMap(new Object[] { key, val });
        return ((Associative) coll).assoc(key, val);
    }

    public static Object contains(Object coll, Object key) {
        if (coll == null) return F; else if (coll instanceof Associative) return ((Associative) coll).containsKey(key) ? T : F; else if (coll instanceof IPersistentSet) return ((IPersistentSet) coll).contains(key) ? T : F; else if (coll instanceof Map) {
            Map m = (Map) coll;
            return m.containsKey(key) ? T : F;
        } else if (key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
            int n = ((Number) key).intValue();
            return n >= 0 && n < count(coll);
        }
        return F;
    }

    public static Object find(Object coll, Object key) {
        if (coll == null) return null; else if (coll instanceof Associative) return ((Associative) coll).entryAt(key); else {
            Map m = (Map) coll;
            if (m.containsKey(key)) return new MapEntry(key, m.get(key));
            return null;
        }
    }

    public static ISeq findKey(Keyword key, ISeq keyvals) throws Exception {
        while (keyvals != null) {
            ISeq r = keyvals.next();
            if (r == null) throw new Exception("Malformed keyword argslist");
            if (keyvals.first() == key) return r;
            keyvals = r.next();
        }
        return null;
    }

    public static Object dissoc(Object coll, Object key) throws Exception {
        if (coll == null) return null;
        return ((IPersistentMap) coll).without(key);
    }

    public static Object nth(Object coll, int n) {
        if (coll == null) return null; else if (coll instanceof IPersistentVector) return ((IPersistentVector) coll).nth(n); else if (coll instanceof String) return Character.valueOf(((String) coll).charAt(n)); else if (coll.getClass().isArray()) return Reflector.prepRet(Array.get(coll, n)); else if (coll instanceof RandomAccess) return ((List) coll).get(n); else if (coll instanceof Matcher) return ((Matcher) coll).group(n); else if (coll instanceof Map.Entry) {
            Map.Entry e = (Map.Entry) coll;
            if (n == 0) return e.getKey(); else if (n == 1) return e.getValue();
            throw new IndexOutOfBoundsException();
        } else if (coll instanceof Sequential) {
            ISeq seq = RT.seq(coll);
            coll = null;
            for (int i = 0; i <= n && seq != null; ++i, seq = seq.next()) {
                if (i == n) return seq.first();
            }
            throw new IndexOutOfBoundsException();
        } else throw new UnsupportedOperationException("nth not supported on this type: " + coll.getClass().getSimpleName());
    }

    public static Object nth(Object coll, int n, Object notFound) {
        if (coll == null) return notFound; else if (n < 0) return notFound; else if (coll instanceof IPersistentVector) {
            IPersistentVector v = (IPersistentVector) coll;
            if (n < v.count()) return v.nth(n);
            return notFound;
        } else if (coll instanceof String) {
            String s = (String) coll;
            if (n < s.length()) return Character.valueOf(s.charAt(n));
            return notFound;
        } else if (coll.getClass().isArray()) {
            if (n < Array.getLength(coll)) return Reflector.prepRet(Array.get(coll, n));
            return notFound;
        } else if (coll instanceof RandomAccess) {
            List list = (List) coll;
            if (n < list.size()) return list.get(n);
            return notFound;
        } else if (coll instanceof Matcher) {
            Matcher m = (Matcher) coll;
            if (n < m.groupCount()) return m.group(n);
            return notFound;
        } else if (coll instanceof Map.Entry) {
            Map.Entry e = (Map.Entry) coll;
            if (n == 0) return e.getKey(); else if (n == 1) return e.getValue();
            return notFound;
        } else if (coll instanceof Sequential) {
            ISeq seq = RT.seq(coll);
            coll = null;
            for (int i = 0; i <= n && seq != null; ++i, seq = seq.next()) {
                if (i == n) return seq.first();
            }
            return notFound;
        } else throw new UnsupportedOperationException("nth not supported on this type: " + coll.getClass().getSimpleName());
    }

    public static Object assocN(int n, Object val, Object coll) {
        if (coll == null) return null; else if (coll instanceof IPersistentVector) return ((IPersistentVector) coll).assocN(n, val); else if (coll instanceof Object[]) {
            Object[] array = ((Object[]) coll);
            array[n] = val;
            return array;
        } else return null;
    }

    static boolean hasTag(Object o, Object tag) {
        return Util.equals(tag, RT.get(RT.meta(o), TAG_KEY));
    }

    /**
 * ********************* Boxing/casts ******************************
 */
    public static Object box(Object x) {
        return x;
    }

    public static Character box(char x) {
        return Character.valueOf(x);
    }

    public static Object box(boolean x) {
        return x ? T : F;
    }

    public static Object box(Boolean x) {
        return x;
    }

    public static Number box(byte x) {
        return x;
    }

    public static Number box(short x) {
        return x;
    }

    public static Number box(int x) {
        return x;
    }

    public static Number box(long x) {
        return x;
    }

    public static Number box(float x) {
        return x;
    }

    public static Number box(double x) {
        return x;
    }

    public static char charCast(Object x) {
        if (x instanceof Character) return ((Character) x).charValue();
        return (char) ((Number) x).intValue();
    }

    public static boolean booleanCast(Object x) {
        if (x instanceof Boolean) return ((Boolean) x).booleanValue();
        return x != null;
    }

    public static byte byteCast(Object x) {
        return ((Number) x).byteValue();
    }

    public static short shortCast(Object x) {
        return ((Number) x).shortValue();
    }

    public static int intCast(Object x) {
        if (x instanceof Number) return ((Number) x).intValue();
        return ((Character) x).charValue();
    }

    public static int intCast(char x) {
        return x;
    }

    public static int intCast(byte x) {
        return x;
    }

    public static int intCast(short x) {
        return x;
    }

    public static int intCast(int x) {
        return x;
    }

    public static int intCast(float x) {
        return (int) x;
    }

    public static int intCast(long x) {
        return (int) x;
    }

    public static int intCast(double x) {
        return (int) x;
    }

    public static long longCast(Object x) {
        return ((Number) x).longValue();
    }

    public static long longCast(int x) {
        return x;
    }

    public static long longCast(float x) {
        return (long) x;
    }

    public static long longCast(long x) {
        return x;
    }

    public static long longCast(double x) {
        return (long) x;
    }

    public static float floatCast(Object x) {
        return ((Number) x).floatValue();
    }

    public static float floatCast(int x) {
        return x;
    }

    public static float floatCast(float x) {
        return x;
    }

    public static float floatCast(long x) {
        return x;
    }

    public static float floatCast(double x) {
        return (float) x;
    }

    public static double doubleCast(Object x) {
        return ((Number) x).doubleValue();
    }

    public static double doubleCast(int x) {
        return x;
    }

    public static double doubleCast(float x) {
        return x;
    }

    public static double doubleCast(long x) {
        return x;
    }

    public static double doubleCast(double x) {
        return x;
    }

    public static IPersistentMap map(Object... init) {
        if (init == null) return PersistentArrayMap.EMPTY; else if (init.length <= PersistentArrayMap.HASHTABLE_THRESHOLD) return new PersistentArrayMap(init);
        return PersistentHashMap.create(init);
    }

    public static IPersistentSet set(Object... init) {
        return PersistentHashSet.create(init);
    }

    public static IPersistentVector vector(Object... init) {
        return LazilyPersistentVector.createOwning(init);
    }

    public static IPersistentVector subvec(IPersistentVector v, int start, int end) {
        if (end < start || start < 0 || end > v.count()) throw new IndexOutOfBoundsException();
        if (start == end) return PersistentVector.EMPTY;
        return new APersistentVector.SubVector(null, v, start, end);
    }

    /**
 * **************************************** list support *******************************
 */
    public static ISeq list() {
        return null;
    }

    public static ISeq list(Object arg1) {
        return new PersistentList(arg1);
    }

    public static ISeq list(Object arg1, Object arg2) {
        return listStar(arg1, arg2, null);
    }

    public static ISeq list(Object arg1, Object arg2, Object arg3) {
        return listStar(arg1, arg2, arg3, null);
    }

    public static ISeq list(Object arg1, Object arg2, Object arg3, Object arg4) {
        return listStar(arg1, arg2, arg3, arg4, null);
    }

    public static ISeq list(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        return listStar(arg1, arg2, arg3, arg4, arg5, null);
    }

    public static ISeq listStar(Object arg1, ISeq rest) {
        return (ISeq) cons(arg1, rest);
    }

    public static ISeq listStar(Object arg1, Object arg2, ISeq rest) {
        return (ISeq) cons(arg1, cons(arg2, rest));
    }

    public static ISeq listStar(Object arg1, Object arg2, Object arg3, ISeq rest) {
        return (ISeq) cons(arg1, cons(arg2, cons(arg3, rest)));
    }

    public static ISeq listStar(Object arg1, Object arg2, Object arg3, Object arg4, ISeq rest) {
        return (ISeq) cons(arg1, cons(arg2, cons(arg3, cons(arg4, rest))));
    }

    public static ISeq listStar(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, ISeq rest) {
        return (ISeq) cons(arg1, cons(arg2, cons(arg3, cons(arg4, cons(arg5, rest)))));
    }

    public static ISeq arrayToList(Object[] a) throws Exception {
        ISeq ret = null;
        for (int i = a.length - 1; i >= 0; --i) ret = (ISeq) cons(a[i], ret);
        return ret;
    }

    public static Object[] toArray(Object coll) throws Exception {
        if (coll == null) return EMPTY_ARRAY; else if (coll instanceof Object[]) return (Object[]) coll; else if (coll instanceof Collection) return ((Collection) coll).toArray(); else if (coll instanceof Map) return ((Map) coll).entrySet().toArray(); else if (coll instanceof String) {
            char[] chars = ((String) coll).toCharArray();
            Object[] ret = new Object[chars.length];
            for (int i = 0; i < chars.length; i++) ret[i] = chars[i];
            return ret;
        } else if (coll.getClass().isArray()) {
            ISeq s = (seq(coll));
            Object[] ret = new Object[count(s)];
            for (int i = 0; i < ret.length; i++, s = s.next()) ret[i] = s.first();
            return ret;
        } else throw new Exception("Unable to convert: " + coll.getClass() + " to Object[]");
    }

    public static Object[] seqToArray(ISeq seq) {
        int len = length(seq);
        Object[] ret = new Object[len];
        for (int i = 0; seq != null; ++i, seq = seq.next()) ret[i] = seq.first();
        return ret;
    }

    public static Object seqToTypedArray(ISeq seq) throws Exception {
        Class type = (seq != null) ? seq.first().getClass() : Object.class;
        return seqToTypedArray(type, seq);
    }

    public static Object seqToTypedArray(Class type, ISeq seq) throws Exception {
        Object ret = Array.newInstance(type, length(seq));
        for (int i = 0; seq != null; ++i, seq = seq.next()) Array.set(ret, i, seq.first());
        return ret;
    }

    public static int length(ISeq list) {
        int i = 0;
        for (ISeq c = list; c != null; c = c.next()) {
            i++;
        }
        return i;
    }

    public static int boundedLength(ISeq list, int limit) throws Exception {
        int i = 0;
        for (ISeq c = list; c != null && i <= limit; c = c.next()) {
            i++;
        }
        return i;
    }

    static Character readRet(int ret) {
        if (ret == -1) return null;
        return box((char) ret);
    }

    public static Character readChar(Reader r) throws Exception {
        int ret = r.read();
        return readRet(ret);
    }

    public static Character peekChar(Reader r) throws Exception {
        int ret;
        if (r instanceof PushbackReader) {
            ret = r.read();
            ((PushbackReader) r).unread(ret);
        } else {
            r.mark(1);
            ret = r.read();
            r.reset();
        }
        return readRet(ret);
    }

    public static int getLineNumber(Reader r) {
        if (r instanceof LineNumberingPushbackReader) return ((LineNumberingPushbackReader) r).getLineNumber();
        return 0;
    }

    public static LineNumberingPushbackReader getLineNumberingReader(Reader r) {
        if (isLineNumberingReader(r)) return (LineNumberingPushbackReader) r;
        return new LineNumberingPushbackReader(r);
    }

    public static boolean isLineNumberingReader(Reader r) {
        return r instanceof LineNumberingPushbackReader;
    }

    public static String resolveClassNameInContext(String className) {
        return className;
    }

    public static boolean suppressRead() {
        return false;
    }

    public static String printString(Object x) {
        try {
            StringWriter sw = new StringWriter();
            print(x, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object readString(String s) {
        PushbackReader r = new PushbackReader(new StringReader(s));
        try {
            return LispReader.read(r, true, null, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void print(Object x, Writer w) throws Exception {
        if (PRINT_INITIALIZED.isBound() && RT.booleanCast(PRINT_INITIALIZED.deref())) PR_ON.invoke(x, w); else {
            boolean readably = booleanCast(PRINT_READABLY.deref());
            if (x instanceof Obj) {
                Obj o = (Obj) x;
                if (RT.count(o.meta()) > 0 && ((readably && booleanCast(PRINT_META.deref())) || booleanCast(PRINT_DUP.deref()))) {
                    IPersistentMap meta = o.meta();
                    w.write("#^");
                    if (meta.count() == 1 && meta.containsKey(TAG_KEY)) print(meta.valAt(TAG_KEY), w); else print(meta, w);
                    w.write(' ');
                }
            }
            if (x == null) w.write("nil"); else if (x instanceof ISeq || x instanceof IPersistentList) {
                w.write('(');
                printInnerSeq(seq(x), w);
                w.write(')');
            } else if (x instanceof String) {
                String s = (String) x;
                if (!readably) w.write(s); else {
                    w.write('"');
                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        switch(c) {
                            case '\n':
                                w.write("\\n");
                                break;
                            case '\t':
                                w.write("\\t");
                                break;
                            case '\r':
                                w.write("\\r");
                                break;
                            case '"':
                                w.write("\\\"");
                                break;
                            case '\\':
                                w.write("\\\\");
                                break;
                            case '\f':
                                w.write("\\f");
                                break;
                            case '\b':
                                w.write("\\b");
                                break;
                            default:
                                w.write(c);
                        }
                    }
                    w.write('"');
                }
            } else if (x instanceof IPersistentMap) {
                w.write('{');
                for (ISeq s = seq(x); s != null; s = s.next()) {
                    IMapEntry e = (IMapEntry) s.first();
                    print(e.key(), w);
                    w.write(' ');
                    print(e.val(), w);
                    if (s.next() != null) w.write(", ");
                }
                w.write('}');
            } else if (x instanceof IPersistentVector) {
                IPersistentVector a = (IPersistentVector) x;
                w.write('[');
                for (int i = 0; i < a.count(); i++) {
                    print(a.nth(i), w);
                    if (i < a.count() - 1) w.write(' ');
                }
                w.write(']');
            } else if (x instanceof IPersistentSet) {
                w.write("#{");
                for (ISeq s = seq(x); s != null; s = s.next()) {
                    print(s.first(), w);
                    if (s.next() != null) w.write(" ");
                }
                w.write('}');
            } else if (x instanceof Character) {
                char c = ((Character) x).charValue();
                if (!readably) w.write(c); else {
                    w.write('\\');
                    switch(c) {
                        case '\n':
                            w.write("newline");
                            break;
                        case '\t':
                            w.write("tab");
                            break;
                        case ' ':
                            w.write("space");
                            break;
                        case '\b':
                            w.write("backspace");
                            break;
                        case '\f':
                            w.write("formfeed");
                            break;
                        case '\r':
                            w.write("return");
                            break;
                        default:
                            w.write(c);
                    }
                }
            } else if (x instanceof Class) {
                w.write("#=");
                w.write(((Class) x).getName());
            } else if (x instanceof BigDecimal && readably) {
                w.write(x.toString());
                w.write('M');
            } else if (x instanceof Var) {
                Var v = (Var) x;
                w.write("#=(var " + v.ns.name + "/" + v.sym + ")");
            } else w.write(x.toString());
        }
    }

    private static void printInnerSeq(ISeq x, Writer w) throws Exception {
        for (ISeq s = x; s != null; s = s.next()) {
            print(s.first(), w);
            if (s.next() != null) w.write(' ');
        }
    }

    public static void formatAesthetic(Writer w, Object obj) throws IOException {
        if (obj == null) w.write("null"); else w.write(obj.toString());
    }

    public static void formatStandard(Writer w, Object obj) throws IOException {
        if (obj == null) w.write("null"); else if (obj instanceof String) {
            w.write('"');
            w.write((String) obj);
            w.write('"');
        } else if (obj instanceof Character) {
            w.write('\\');
            char c = ((Character) obj).charValue();
            switch(c) {
                case '\n':
                    w.write("newline");
                    break;
                case '\t':
                    w.write("tab");
                    break;
                case ' ':
                    w.write("space");
                    break;
                case '\b':
                    w.write("backspace");
                    break;
                case '\f':
                    w.write("formfeed");
                    break;
                default:
                    w.write(c);
            }
        } else w.write(obj.toString());
    }

    public static Object format(Object o, String s, Object... args) throws Exception {
        Writer w;
        if (o == null) w = new StringWriter(); else if (Util.equals(o, T)) w = (Writer) OUT.deref(); else w = (Writer) o;
        doFormat(w, s, ArraySeq.create(args));
        if (o == null) return w.toString();
        return null;
    }

    public static ISeq doFormat(Writer w, String s, ISeq args) throws Exception {
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i++);
            switch(Character.toLowerCase(c)) {
                case '~':
                    char d = s.charAt(i++);
                    switch(Character.toLowerCase(d)) {
                        case '%':
                            w.write('\n');
                            break;
                        case 't':
                            w.write('\t');
                            break;
                        case 'a':
                            if (args == null) throw new IllegalArgumentException("Missing argument");
                            RT.formatAesthetic(w, RT.first(args));
                            args = RT.next(args);
                            break;
                        case 's':
                            if (args == null) throw new IllegalArgumentException("Missing argument");
                            RT.formatStandard(w, RT.first(args));
                            args = RT.next(args);
                            break;
                        case '{':
                            int j = s.indexOf("~}", i);
                            if (j == -1) throw new IllegalArgumentException("Missing ~}");
                            String subs = s.substring(i, j);
                            for (ISeq sargs = RT.seq(RT.first(args)); sargs != null; ) sargs = doFormat(w, subs, sargs);
                            args = RT.next(args);
                            i = j + 2;
                            break;
                        case '^':
                            if (args == null) return null;
                            break;
                        case '~':
                            w.write('~');
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported ~ directive: " + d);
                    }
                    break;
                default:
                    w.write(c);
            }
        }
        return args;
    }

    public static Object[] setValues(Object... vals) {
        if (vals.length > 0) return vals;
        return null;
    }

    public static ClassLoader makeClassLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                getRootClassLoader();
                return new DynamicClassLoader(baseLoader());
            }
        });
    }

    public static ClassLoader baseLoader() {
        if (booleanCast(USE_CONTEXT_CLASSLOADER.deref())) return Thread.currentThread().getContextClassLoader(); else if (ROOT_CLASSLOADER != null) return ROOT_CLASSLOADER;
        return Compiler.class.getClassLoader();
    }

    public static Class classForName(String name) throws ClassNotFoundException {
        return Class.forName(name, true, baseLoader());
    }

    public static Class loadClassForName(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, true, baseLoader());
        } catch (ClassNotFoundException e) {
            if (e.getCause() == null) return null;
            throw e;
        }
    }

    public static float aget(float[] xs, int i) {
        return xs[i];
    }

    public static float aset(float[] xs, int i, float v) {
        xs[i] = v;
        return v;
    }

    public static int alength(float[] xs) {
        return xs.length;
    }

    public static float[] aclone(float[] xs) {
        return xs.clone();
    }

    public static double aget(double[] xs, int i) {
        return xs[i];
    }

    public static double aset(double[] xs, int i, double v) {
        xs[i] = v;
        return v;
    }

    public static int alength(double[] xs) {
        return xs.length;
    }

    public static double[] aclone(double[] xs) {
        return xs.clone();
    }

    public static int aget(int[] xs, int i) {
        return xs[i];
    }

    public static int aset(int[] xs, int i, int v) {
        xs[i] = v;
        return v;
    }

    public static int alength(int[] xs) {
        return xs.length;
    }

    public static int[] aclone(int[] xs) {
        return xs.clone();
    }

    public static long aget(long[] xs, int i) {
        return xs[i];
    }

    public static long aset(long[] xs, int i, long v) {
        xs[i] = v;
        return v;
    }

    public static int alength(long[] xs) {
        return xs.length;
    }

    public static long[] aclone(long[] xs) {
        return xs.clone();
    }

    public static char aget(char[] xs, int i) {
        return xs[i];
    }

    public static char aset(char[] xs, int i, char v) {
        xs[i] = v;
        return v;
    }

    public static int alength(char[] xs) {
        return xs.length;
    }

    public static char[] aclone(char[] xs) {
        return xs.clone();
    }

    public static byte aget(byte[] xs, int i) {
        return xs[i];
    }

    public static byte aset(byte[] xs, int i, byte v) {
        xs[i] = v;
        return v;
    }

    public static int alength(byte[] xs) {
        return xs.length;
    }

    public static byte[] aclone(byte[] xs) {
        return xs.clone();
    }

    public static short aget(short[] xs, int i) {
        return xs[i];
    }

    public static short aset(short[] xs, int i, short v) {
        xs[i] = v;
        return v;
    }

    public static int alength(short[] xs) {
        return xs.length;
    }

    public static short[] aclone(short[] xs) {
        return xs.clone();
    }

    public static boolean aget(boolean[] xs, int i) {
        return xs[i];
    }

    public static boolean aset(boolean[] xs, int i, boolean v) {
        xs[i] = v;
        return v;
    }

    public static int alength(boolean[] xs) {
        return xs.length;
    }

    public static boolean[] aclone(boolean[] xs) {
        return xs.clone();
    }

    public static Object aget(Object[] xs, int i) {
        return xs[i];
    }

    public static Object aset(Object[] xs, int i, Object v) {
        xs[i] = v;
        return v;
    }

    public static int alength(Object[] xs) {
        return xs.length;
    }

    public static Object[] aclone(Object[] xs) {
        return xs.clone();
    }

    public static Object aget(Object xs, int i) {
        return Reflector.prepRet(Array.get(xs, i));
    }

    public static Object aset(Object xs, int i, Object v) {
        Array.set(xs, i, v);
        return v;
    }

    public static int alength(Object xs) {
        return Array.getLength(xs);
    }

    public static synchronized DynamicClassLoader getRootClassLoader() {
        if (ROOT_CLASSLOADER == null) ROOT_CLASSLOADER = new DynamicClassLoader();
        return ROOT_CLASSLOADER;
    }
}
