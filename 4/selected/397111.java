package php.java.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import php.java.bridge.Util;
import php.java.bridge.http.IContext;

/**
 * This class implements the ScriptEngine.<p>
 * Example:<p>
 * <code>
 * ScriptEngine e = (new ScriptEngineManager()).getEngineByName("php");<br>
 * try { e.eval(&lt;?php foo() ?&gt;"); } catch (ScriptException e) { ... }<br>
 * </code>
 * @author jostb
 *
 */
public class PhpScriptEngine extends AbstractPhpScriptEngine {

    /**
     * Create a new ScriptEngine with a default context.
     */
    public PhpScriptEngine() {
        super(new PhpScriptEngineFactory());
    }

    /**
     * Create a new ScriptEngine from a factory.
     * @param factory The factory
     * @see #getFactory()
     */
    public PhpScriptEngine(PhpScriptEngineFactory factory) {
        super(factory);
    }

    /**
     * Create a new ScriptEngine with bindings.
     * @param n the bindings
     */
    public PhpScriptEngine(Bindings n) {
        this();
        setBindings(n, ScriptContext.ENGINE_SCOPE);
    }

    protected Reader getLocalReader(Reader reader, boolean embedJavaInc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out);
        try {
            Reader localReader = null;
            char[] buf = new char[Util.BUF_SIZE];
            int c;
            String stdHeader = embedJavaInc ? null : ((IContext) getContext()).getRedirectURL("/JavaBridge");
            localReader = new StringReader(getStandardHeader(stdHeader));
            while ((c = localReader.read(buf)) > 0) w.write(buf, 0, c);
            localReader.close();
            localReader = null;
            while ((c = reader.read(buf)) > 0) w.write(buf, 0, c);
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
        if ((continuation != null) || (reader == null)) release();
        if (reader == null) return null;
        setNewContextFactory();
        Reader localReader = null;
        try {
            localReader = getLocalReader(reader, false);
            this.script = doEval(localReader, context);
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
            release();
        }
        return resultProxy;
    }

    protected Object doEvalCompiledPhp(Reader reader, ScriptContext context) throws ScriptException {
        if ((continuation != null) || (reader == null)) release();
        if (reader == null) return null;
        setNewContextFactory();
        try {
            this.script = doEval(reader, context);
        } catch (Exception e) {
            Util.printStackTrace(e);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof ScriptException) throw (ScriptException) e;
            throw new ScriptException(e);
        } finally {
            release();
        }
        return resultProxy;
    }
}
