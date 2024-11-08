package php.java.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import php.java.bridge.PhpProcedure;
import php.java.bridge.Util;
import php.java.bridge.http.IContext;

/**
 * This class implements the ScriptEngine and the Invocable interface.<p>
 * Example:
 * <blockquote>
 * <code>
 * ScriptEngine e = (new ScriptEngineManager()).getEngineByName("php-invocable");<br>
 * e.eval(&lt;? function f() {return java_server_name();}?&gt;<br>
 * System.out.println(((Invocable)e).invokeFunction("f", new Object[]{}));<br>
 * ((Closeable)e).close();<br>
 * </code>
 * </blockquote><br>
 * Another example which invokes a remote PHP "method" bound in the closed-over PHP environment. The PHP script "hello.php":
 * <blockquote>
 * <code>
 * &lt;?php require_once("java/Java.inc");<br>
 * function f() {return java_server_name();};<br>
 * java_call_with_continuation(java_closure());<br>
 * ?&gt;<br>
 * </code>
 * </blockquote><br>
 *  The Java code:
 * <blockquote>
 * <code>
 * ScriptEngine e = (new ScriptEngineManager()).getEngineByName("php-invocable");<br>
 * e.eval(new php.java.script.URLReader(new URL("http://localhost/hello.php")));<br>
 * System.out.println(((Invocable)e).invokeMethod(e.get("php.java.bridge.PhpProcedure"), "f", new Object[]{}));<br>
 * ((Closeable)e).close();<br>
 * </code>
 * </blockquote>
 */
public class InvocablePhpScriptEngine extends AbstractPhpScriptEngine implements Invocable {

    private static final String X_JAVABRIDGE_INCLUDE = Util.X_JAVABRIDGE_INCLUDE;

    private static final String PHP_JAVA_CONTEXT_CALL_JAVA_CLOSURE = "<?php java_context()->call(java_closure()); ?>";

    protected static final Object EMPTY_INCLUDE = "@";

    private static boolean registeredHook = false;

    private static final List engines = new LinkedList();

    private static final String PHP_EMPTY_SCRIPT = "<?php ?>";

    /**
     * Create a new ScriptEngine with a default context.
     */
    public InvocablePhpScriptEngine() {
        this(new PhpScriptEngineFactory());
    }

    /**
     * Create a new ScriptEngine from a factory.
     * @param factory The factory
     * @see #getFactory()
     */
    public InvocablePhpScriptEngine(PhpScriptEngineFactory factory) {
        super(factory);
    }

    /**
     * Create a new ScriptEngine with bindings.
     * @param n the bindings
     */
    public InvocablePhpScriptEngine(Bindings n) {
        this();
        setBindings(n, ScriptContext.ENGINE_SCOPE);
    }

    protected Object invoke(String methodName, Object[] args) throws ScriptException, NoSuchMethodException {
        if (methodName == null) {
            release();
            return null;
        }
        if (scriptClosure == null) {
            if (Util.logLevel > 4) Util.warn("Evaluating an empty script either because eval() has not been called or release() has been called.");
            eval(PHP_EMPTY_SCRIPT);
        }
        try {
            return invoke(scriptClosure, methodName, args);
        } catch (php.java.bridge.Request.AbortException e) {
            release();
            throw new ScriptException(e);
        } catch (NoSuchMethodError e) {
            throw new NoSuchMethodException(String.valueOf(e.getMessage()));
        }
    }

    /**{@inheritDoc}*/
    public Object invokeFunction(String methodName, Object[] args) throws ScriptException, NoSuchMethodException {
        return invoke(methodName, args);
    }

    private void checkPhpClosure(Object thiz) {
        if (thiz == null) throw new IllegalStateException("PHP script did not pass its continuation to us!. Please check if the previous call to eval() reported any errors. Or else check if it called OUR continuation.");
    }

    protected Object invoke(Object thiz, String methodName, Object[] args) throws ScriptException, NoSuchMethodException {
        checkPhpClosure(thiz);
        PhpProcedure proc = (PhpProcedure) (Proxy.getInvocationHandler(thiz));
        try {
            return proc.invoke(script, methodName, args);
        } catch (ScriptException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (NoSuchMethodError e) {
            throw new NoSuchMethodException(String.valueOf(e.getMessage()));
        } catch (Error er) {
            throw er;
        } catch (Throwable e) {
            throw new PhpScriptException("Invocation threw exception ", e);
        }
    }

    /**{@inheritDoc}*/
    public Object invokeMethod(Object thiz, String methodName, Object[] args) throws ScriptException, NoSuchMethodException {
        return invoke(thiz, methodName, args);
    }

    /**{@inheritDoc}*/
    public Object getInterface(Class clasz) {
        checkPhpClosure(script);
        return getInterface(script, clasz);
    }

    /**{@inheritDoc}*/
    public Object getInterface(Object thiz, Class clasz) {
        checkPhpClosure(thiz);
        Class[] interfaces = clasz == null ? Util.ZERO_PARAM : new Class[] { clasz };
        return PhpProcedure.createProxy(interfaces, (PhpProcedure) Proxy.getInvocationHandler(thiz));
    }

    protected Reader getLocalReader(Reader reader, boolean embedJavaInc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out);
        String stdHeader = embedJavaInc ? null : ((IContext) getContext()).getRedirectURL("/JavaBridge");
        Reader localReader = new StringReader(getStandardHeader(stdHeader));
        char[] buf = new char[Util.BUF_SIZE];
        int c;
        try {
            while ((c = localReader.read(buf)) > 0) w.write(buf, 0, c);
            localReader.close();
            localReader = null;
            while ((c = reader.read(buf)) > 0) w.write(buf, 0, c);
            localReader = new StringReader(PHP_JAVA_CONTEXT_CALL_JAVA_CLOSURE);
            while ((c = localReader.read(buf)) > 0) w.write(buf, 0, c);
            localReader.close();
            localReader = null;
            w.close();
            w = null;
            localReader = new InputStreamReader(new ByteArrayInputStream(out.toByteArray()));
            return localReader;
        } finally {
            if (w != null) try {
                w.close();
            } catch (IOException e) {
            }
        }
    }

    protected Object doEvalPhp(Reader reader, ScriptContext context) throws ScriptException {
        if (reader instanceof URLReader) return eval((URLReader) reader, context);
        if ((continuation != null) || (reader == null)) release();
        if (reader == null) return null;
        setNewContextFactory();
        env.put(X_JAVABRIDGE_INCLUDE, EMPTY_INCLUDE);
        Reader localReader = null;
        try {
            localReader = getLocalReader(reader, false);
            this.script = doEval(localReader, context);
            if (this.script != null) {
                this.scriptClosure = script;
            }
        } catch (Exception e) {
            Util.printStackTrace(e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof ScriptException) throw (ScriptException) e;
            throw new ScriptException(e);
        } finally {
            if (localReader != null) try {
                localReader.close();
            } catch (IOException e) {
            }
            handleRelease();
        }
        return resultProxy;
    }

    protected Object doEvalCompiledPhp(Reader reader, ScriptContext context) throws ScriptException {
        if ((continuation != null) || (reader == null)) release();
        if (reader == null) return null;
        setNewContextFactory();
        env.put(X_JAVABRIDGE_INCLUDE, EMPTY_INCLUDE);
        try {
            this.script = doEval(reader, context);
            if (this.script != null) {
                this.scriptClosure = script;
            }
        } catch (Exception e) {
            Util.printStackTrace(e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof ScriptException) throw (ScriptException) e;
            throw new ScriptException(e);
        } finally {
            handleRelease();
        }
        return resultProxy;
    }

    protected Object eval(URLReader reader, ScriptContext context) throws ScriptException {
        if ((continuation != null) || (reader == null)) release();
        if (reader == null) return null;
        setNewContextFactory();
        env.put(X_JAVABRIDGE_INCLUDE, EMPTY_INCLUDE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out);
        try {
            this.script = doEval(reader, context);
            if (this.script != null) {
                this.scriptClosure = script;
            }
        } catch (Exception e) {
            Util.printStackTrace(e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof ScriptException) throw (ScriptException) e;
            throw new ScriptException(e);
        } finally {
            if (w != null) try {
                w.close();
            } catch (IOException e) {
            }
            handleRelease();
        }
        return resultProxy;
    }

    protected void handleRelease() {
        synchronized (engines) {
            if (!registeredHook) {
                registeredHook = true;
                try {
                    Runtime.getRuntime().addShutdownHook(new Util.Thread() {

                        public void run() {
                            if (engines == null) return;
                            synchronized (engines) {
                                for (Iterator ii = engines.iterator(); ii.hasNext(); ii.remove()) {
                                    InvocablePhpScriptEngine e = (InvocablePhpScriptEngine) ii.next();
                                    e.releaseInternal();
                                }
                            }
                        }
                    });
                } catch (SecurityException e) {
                }
            }
            engines.add(this);
        }
    }

    private void releaseInternal() {
        super.release();
    }

    /**{@inheritDoc}*/
    public void release() {
        synchronized (engines) {
            releaseInternal();
            engines.remove(this);
        }
    }
}
