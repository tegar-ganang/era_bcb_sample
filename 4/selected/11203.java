package fr.x9c.ocamlscripting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import fr.x9c.cadmium.kernel.ByteCodeParameters;
import fr.x9c.cadmium.kernel.CadmiumException;
import fr.x9c.cadmium.kernel.Fatal;
import fr.x9c.cadmium.kernel.Interpreter;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.primitives.cadmium.Cadmium;
import fr.x9c.cadmium.support.Helper;
import fr.x9c.cadmium.util.CustomClassLoader;
import fr.x9c.cadmium.util.RandomAccessInputStream;
import ocaml.compilers.cafesterolMain;

/**
 * This class implements a script engine based on Cadmium/Cafesterol. <br>
 * Cadmium is used to run a modified Objective Caml toplevel for script
 * evaluation. Cafesterol is used to compile script that can be subsequently
 * run.
 *
 * @author <a href="mailto:ocamlscripting@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
public final class OCamlScriptEngine implements ScriptEngine, Compilable, Invocable {

    /** Identifier for next generated package (script compilation). */
    private static final AtomicInteger ID = new AtomicInteger();

    /** Scripts context. */
    private ScriptContext context;

    /** Interpreter running toplevel. */
    private final Interpreter interpreter;

    /** Library paths. */
    private final List<String> libraryPaths;

    /** Loaded libraries, useful for script compilation. */
    private final List<String> libraries;

    /** Standard input. */
    final RedirectedInputStream in;

    /** Standard output. */
    final RedirectedOutputStream out;

    /** Standard error output. */
    final RedirectedOutputStream err;

    /** Standard output. */
    private PrintStream printOut;

    /** Standard error output. */
    private PrintStream printErr;

    /**
     * Constructs a script engine by initializing a modified OCaml toplevel.
     * @throws RuntimeException if any error occurs during toplevel
     *                          initialization
     */
    public OCamlScriptEngine() throws RuntimeException {
        this.context = new SimpleScriptContext();
        this.libraryPaths = new LinkedList<String>();
        this.libraryPaths.add("+cadmium");
        this.libraries = new LinkedList<String>();
        this.in = new RedirectedInputStream(System.in);
        this.out = new RedirectedOutputStream(System.out);
        this.err = new RedirectedOutputStream(System.err);
        this.printOut = new PrintStream(this.out);
        this.printErr = new PrintStream(this.err);
        try {
            final int SIZE = 1024;
            final byte[] buffer = new byte[SIZE];
            final File tmp = File.createTempFile("ocamlscript", ".toplevel");
            final InputStream is = OCamlScriptEngine.class.getResourceAsStream("script");
            final OutputStream os = new FileOutputStream(tmp);
            int read = is.read(buffer);
            while (read != -1) {
                os.write(buffer, 0, read);
                read = is.read(buffer);
            }
            is.close();
            os.close();
            final RandomAccessInputStream bytecode = new RandomAccessInputStream(tmp);
            final ByteCodeParameters params = new ByteCodeParameters(new String[] { "-w", "a" }, false, false, this.in, this.printOut, this.printErr, false, false, true, "Unix", false, tmp.getAbsolutePath(), true, null, false, false, false, false, 64 * 1024, 64 * 1024, new String[0], true);
            this.interpreter = new Interpreter(params, new File("."), bytecode);
            bytecode.close();
            this.interpreter.execute();
        } catch (final IOException ioe) {
            throw new RuntimeException("Unable to create script engine", ioe);
        } catch (final Fatal.Exception fe) {
            throw new RuntimeException("Unable to create script engine", fe);
        } catch (final CadmiumException ce) {
            throw new RuntimeException("Unable to create script engine", ce);
        }
    }

    /**
     * Adds a library path.
     * @param path library path to add
     * @throws NullPointerException if passed path is null
     * @throws ScriptEngine if an error occurs while trying to add directory
     */
    public void addLibraryPath(final String path) throws NullPointerException, ScriptException {
        if (path != null) {
            if (!this.libraryPaths.contains(path)) {
                this.libraryPaths.add(path);
            }
            try {
                this.interpreter.execute("javax.script.directory", Helper.createString(path));
            } catch (final CadmiumException ce) {
                throw new ScriptException(ce);
            }
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Loads an OCaml library.
     * @param lib library name <b>with no extension</b>
     * @throws NullPointerException if passed library name
     * @throws ScriptEngine if an error occurs while trying to load library
     */
    public void loadLibrary(final String lib) throws NullPointerException, ScriptException {
        if (lib != null) {
            if (!this.libraries.contains(lib)) {
                this.libraries.add(lib);
            }
            try {
                this.interpreter.execute("javax.script.load", Helper.createString(lib + ".cma"));
            } catch (final CadmiumException ce) {
                throw new ScriptException(ce);
            }
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Bindings createBindings() {
        return new OCamlBindings();
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final Reader reader) throws NullPointerException, ScriptException {
        try {
            return evaluate(readerToString(reader), this.context);
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final Reader reader, final Bindings n) throws NullPointerException, ScriptException {
        try {
            return evaluate(readerToString(reader), getScriptContext(n));
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final Reader reader, final ScriptContext context) throws NullPointerException, ScriptException {
        try {
            return evaluate(readerToString(reader), (OCamlContext) context);
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final String script) throws NullPointerException, ScriptException {
        return evaluate(script, this.context);
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final String script, final Bindings n) throws NullPointerException, ScriptException {
        return evaluate(script, getScriptContext(n));
    }

    /**
     * {@inheritDoc}
     */
    public Object eval(final String script, final ScriptContext context) throws NullPointerException, ScriptException {
        return evaluate(script, (OCamlContext) context);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(final String key) throws NullPointerException, IllegalArgumentException {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Bindings getBindings(final int scope) throws IllegalArgumentException {
        return this.context.getBindings(scope);
    }

    /**
     * {@inheritDoc}
     */
    public ScriptContext getContext() {
        return this.context;
    }

    /**
     * {@inheritDoc}
     */
    public ScriptEngineFactory getFactory() {
        return new OCamlScriptEngineFactory();
    }

    /**
     * {@inheritDoc}
     */
    public void put(final String key, final Object value) throws NullPointerException, IllegalArgumentException {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setBindings(final Bindings bindings, final int scope) throws NullPointerException, IllegalArgumentException {
        this.context.setBindings(bindings, scope);
    }

    /**
     * Returns a map containing all bindings (global and engine ones),
     * globals bindings being superseded by engine ones with the same name.
     * @return a map containing all bindings
     */
    Map<String, Value> getBindings() {
        final Map<String, Value> res = new HashMap<String, Value>();
        final Bindings globalScope = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (globalScope != null) {
            for (String key : globalScope.keySet()) {
                res.put(key, OCamlBindings.convert(globalScope.get(key)));
            }
        }
        final Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
        for (String key : engineScope.keySet()) {
            res.put(key, OCamlBindings.convert(engineScope.get(key)));
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    public void setContext(final ScriptContext context) throws NullPointerException {
        in.redirect(new ReaderInputStream(context.getReader()));
        out.redirect(new WriterOutputStream(context.getWriter()));
        err.redirect(new WriterOutputStream(context.getErrorWriter()));
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public CompiledScript compile(final String script) throws NullPointerException, ScriptException {
        if (script == null) {
            throw new NullPointerException("null script");
        }
        try {
            final int id = ID.getAndIncrement();
            final String pack = "fr.x9c.ocamlscripting.generated" + id;
            final File ml = File.createTempFile("ocamlscript", ".ml");
            final File jar = File.createTempFile("ocamlscript", ".jar");
            final Writer writer = new FileWriter(ml);
            writer.write(script);
            writer.close();
            final List<String> paramList = new LinkedList<String>();
            for (String s : this.libraryPaths) {
                paramList.add("-I");
                paramList.add(s);
            }
            for (String s : this.libraries) {
                paramList.add(s + ".cmja");
            }
            paramList.add("-w");
            paramList.add("a");
            paramList.add("-scripting");
            paramList.add("-cadmium-parameter");
            paramList.add("exitStoppingJVM=off");
            paramList.add("-java-package");
            paramList.add(pack);
            paramList.add("-o");
            paramList.add(jar.getAbsolutePath());
            paramList.add(ml.getAbsolutePath());
            final String[] params = paramList.toArray(new String[paramList.size()]);
            cafesterolMain.mainWithReturn(params);
            final URLClassLoader loader = new URLClassLoader(new URL[] { new URL("file://" + jar.getAbsolutePath()) }, CustomClassLoader.INSTANCE);
            final Class cl = Class.forName(pack + ".cafesterolMain", true, loader);
            final Method main = cl.getMethod("mainScripting", String[].class, Map.class, InputStream.class, PrintStream.class, PrintStream.class);
            return new OCamlCompiledScript(this, main, this.context);
        } catch (final NoSuchMethodException nsme) {
            throw new ScriptException(nsme);
        } catch (final ClassNotFoundException cnfe) {
            throw new ScriptException(cnfe);
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CompiledScript compile(final Reader script) throws NullPointerException, ScriptException {
        if (script == null) {
            throw new NullPointerException("null script");
        }
        try {
            return compile(readerToString(script));
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
    }

    /**
     * Throws an <tt>UnsupportedOperationException</tt>.
     * @throws UnsupportedOperationException always
     */
    public Object invokeMethod(final Object thiz, final String name, final Object... args) throws ScriptException, NoSuchMethodException, NullPointerException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object invokeFunction(final String name, final Object... args) throws ScriptException, NoSuchMethodException, NullPointerException {
        try {
            this.out.flush();
            this.err.flush();
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
        try {
            final int len = args.length;
            final Value[] arguments = new Value[len];
            for (int i = 0; i < len; i++) {
                arguments[i] = OCamlBindings.convert(args[i]);
            }
            return this.interpreter.execute(name, arguments);
        } catch (final CadmiumException ce) {
            throw new ScriptException(ce);
        }
    }

    /**
     * Throws an <tt>UnsupportedOperationException</tt>.
     * @throws UnsupportedOperationException always
     */
    public <T> T getInterface(final Class<T> clasz) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws an <tt>UnsupportedOperationException</tt>.
     * @throws UnsupportedOperationException always
     */
    public <T> T getInterface(final Object thiz, final Class<T> clasz) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    /**
     * Actually evaluates a script.
     * @param script script to evaluate - should not be <tt>null</tt>
     * @param context evaluation context - should not be <tt>null</tt>
     * @return evaluation result, as a <tt>Value</tt> instance
     */
    private Object evaluate(final String script, final ScriptContext context) throws ScriptException {
        assert script != null : "null script";
        assert context != null : "null context";
        try {
            this.out.flush();
            this.err.flush();
        } catch (final IOException ioe) {
            throw new ScriptException(ioe);
        }
        try {
            return this.interpreter.executeWithBindings("javax.script.eval", getBindings(), Helper.createString(script));
        } catch (final CadmiumException ce) {
            throw new ScriptException(ce);
        }
    }

    /**
     * Reads the characters from a passed reader to build a string.
     * @param reader characters source - should not be <tt>null</tt>
     * @return the string constructed by characters read from passed reader
     * @throws IOException if an error occurs while reading characters
     */
    private static String readerToString(final Reader reader) throws IOException {
        assert reader != null : "null reader";
        final int SIZE = 1024;
        final StringWriter res = new StringWriter(SIZE);
        final char[] buffer = new char[SIZE];
        int read = reader.read(buffer);
        while (read != -1) {
            res.write(buffer, 0, read);
            read = reader.read(buffer);
        }
        return res.toString();
    }

    protected ScriptContext getScriptContext(Bindings bindings) {
        ScriptContext result = new SimpleScriptContext();
        if (bindings != null) {
            result.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        }
        if (context.getBindings(ScriptContext.GLOBAL_SCOPE) != null) {
            result.setBindings(context.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        }
        result.setReader(context.getReader());
        result.setWriter(context.getWriter());
        result.setErrorWriter(context.getErrorWriter());
        return result;
    }
}
