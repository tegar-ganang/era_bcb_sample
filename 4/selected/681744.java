package fr.x9c.cadmium.primitives.cadmium;

import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fr.x9c.cadmium.kernel.AbstractCodeRunner;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Fatal;
import fr.x9c.cadmium.kernel.Misc;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;
import fr.x9c.cadmium.primitives.stdlib.Compare;
import fr.x9c.cadmium.primitives.stdlib.Hash;
import fr.x9c.cadmium.util.CustomClassLoader;

/**
 * This class provides primitives related to {@link java.lang.reflect.Proxy}.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Proxies {

    /** Identifier for 'encode_value' internal function. */
    private static final String ENCODE_VALUE = "cadmium_encode_value";

    /** Identifier for 'decode_value' internal function. */
    private static final String DECODE_VALUE = "cadmium_decode_value";

    /**
     * No instance of this class.
     */
    private Proxies() {
    }

    /**
     * Constructs a proxy that implements a given interface and that
     * calls ocaml closures according to a binding list.
     * @param ctxt context
     * @param clss interfaces to implement
     * @param list binding list
     * @return corresponing proxy
     * @throws Fail.Exception if proxy creation fails
     */
    @Primitive
    public static Value cadmium_register(final CodeRunner ctxt, final Value clss, final Value list) throws Fail.Exception {
        try {
            final Object o = Proxy.newProxyInstance(CustomClassLoader.INSTANCE, convertClassArray(clss), new BindingsHandler(decodeBindingsList(list), ctxt));
            return Cadmium.createObject(o);
        } catch (final Exception e) {
            Cadmium.fail(ctxt, e);
            return Value.UNIT;
        }
    }

    /**
     * Constructs a proxy that implements a given interface and that
     * calls ocaml object methods of the same name.
     * @param ctxt context
     * @param clss interfaces to implement
     * @param obj ocaml object to call methods on
     * @return corresponing proxy
     * @throws Fail.Exception if proxy creation fails
     */
    @Primitive
    public static Value cadmium_register_object(final CodeRunner ctxt, final Value clss, final Value obj) throws Fail.Exception {
        try {
            final Object o = Proxy.newProxyInstance(CustomClassLoader.INSTANCE, convertClassArray(clss), new ObjectHandler(obj, ctxt, null));
            return Cadmium.createObject(o);
        } catch (final Exception e) {
            Cadmium.fail(ctxt, e);
            return Value.UNIT;
        }
    }

    /**
     * Constructs a proxy that implements a given interface and that
     * calls ocaml object methods whose names are determined using
     * a translation table.
     * @param ctxt context
     * @param clss interfaces to implement
     * @param obj ocaml object to call methods on
     * @param transl translation table from methods to ocaml method names
     * @return corresponing proxy
     * @throws Fail.Exception if proxy creation fails
     */
    @Primitive
    public static Value cadmium_register_object_transl(final CodeRunner ctxt, final Value clss, final Value obj, final Value transl) throws Fail.Exception {
        try {
            final Object o = Proxy.newProxyInstance(CustomClassLoader.INSTANCE, convertClassArray(clss), new ObjectHandler(obj, ctxt, decodeTranslList(transl)));
            return Cadmium.createObject(o);
        } catch (final Exception e) {
            Cadmium.fail(ctxt, e);
            return Value.UNIT;
        }
    }

    /**
     * Converts a list of <i>java_class</i> to an array of
     * {@link java.lang.Class}.
     * @param clList list to convert from
     * @return class array
     */
    private static Class[] convertClassArray(final Value clList) {
        final List<Class> res = new ArrayList<Class>();
        Value list = clList;
        while (list.isBlock()) {
            final Block listBlock = list.asBlock();
            res.add((Class) listBlock.get(0).asBlock().asCustom());
            list = listBlock.get(1);
        }
        final Class[] tmp = new Class[res.size()];
        return res.toArray(tmp);
    }

    /**
     * Converts a list of <i>java_method * (java_value list -> java_value)</i>
     * to a map from methods to corresponding closures.
     * @param bindings list to convert from
     * @return map from methods to closures
     */
    private static Map<Method, Value> decodeBindingsList(final Value bindings) {
        final Map<Method, Value> res = new HashMap<Method, Value>();
        Value list = bindings;
        while (list.isBlock()) {
            final Block listBlock = list.asBlock();
            final Block binding = listBlock.get(0).asBlock();
            res.put((Method) binding.get(0).asBlock().asCustom(), binding.get(1));
            list = listBlock.get(1);
        }
        return res;
    }

    /**
     * Converts a list of <i>java_method * string</i> to a map
     * from methods to corresponding names.
     * @param transl list to convert from
     * @return map from methods to names
     */
    private static Map<Method, String> decodeTranslList(final Value transl) {
        final Map<Method, String> res = new HashMap<Method, String>();
        Value list = transl;
        while (list.isBlock()) {
            final Block listBlock = list.asBlock();
            final Block cpl = listBlock.get(0).asBlock();
            res.put((Method) cpl.get(0).asBlock().asCustom(), cpl.get(1).asBlock().asString());
            list = listBlock.get(1);
        }
        return res;
    }

    /**
     * This class is the invocation handler used for method dispatching
     * with a list of bindings.
     */
    private static final class BindingsHandler implements InvocationHandler {

        /** Bindings from called methods to ocaml functions. */
        private final Map<Method, Value> bindings;

        /** Underlying code runner. */
        private final CodeRunner codeRunner;

        /**
         * Constructs a new bindings handler.
         * @param b bindings from methods to ocaml functions
         * @param cr code runner
         */
        private BindingsHandler(final Map<Method, Value> b, final CodeRunner cr) {
            this.bindings = b;
            this.codeRunner = cr;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Value v = this.bindings.get(method);
            if (v != null) {
                try {
                    final int len = args != null ? args.length : 0;
                    final Value[] params = new Value[len];
                    for (int i = 0; i < len; i++) {
                        params[i] = Cadmium.encodeObject(args[i]);
                    }
                    final Value res = this.codeRunner.callback(v, Cadmium.createList(params));
                    return Cadmium.decodeObject(res);
                } catch (final Fail.Exception fe) {
                    final Context ctxt = this.codeRunner.getContext();
                    final Value exn = fe.asValue(ctxt.getGlobalData());
                    final Value javaExn = ctxt.getCallback("Cadmium.Java_exception");
                    try {
                        if ((javaExn != null) && exn.isBlock() && (exn.asBlock().sizeValues() >= 1) && (Compare.caml_equal(this.codeRunner, exn.asBlock().get(0), javaExn) == Value.TRUE)) {
                            final Object obj = exn.asBlock().get(1).asBlock().get(2).asBlock().asCustom();
                            ((Throwable) obj).fillInStackTrace();
                            if (obj instanceof Error) {
                                throw (Error) obj;
                            } else if (obj instanceof RuntimeException) {
                                throw (RuntimeException) obj;
                            }
                        } else {
                            final Channel ch = ctxt.getChannel(Channel.STDERR);
                            if ((ch != null) && (ch.asOutputStream() != null)) {
                                final String msg = Misc.convertException(fe.asValue(ctxt.getGlobalData()));
                                final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                                err.println("Error in proxy: exception " + msg);
                                err.close();
                            }
                        }
                    } catch (final Fail.Exception fe2) {
                        final Channel ch = ctxt.getChannel(Channel.STDERR);
                        if ((ch != null) && (ch.asOutputStream() != null)) {
                            final String msg = Misc.convertException(fe2.asValue(ctxt.getGlobalData()));
                            final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                            err.println("Error in proxy: exception " + msg);
                            err.close();
                        }
                    }
                } catch (final Fatal.Exception fe) {
                    final Context ctxt = this.codeRunner.getContext();
                    final Channel ch = ctxt.getChannel(Channel.STDERR);
                    if ((ch != null) && (ch.asOutputStream() != null)) {
                        final String msg = fe.getMessage();
                        final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                        err.println("Error in proxy: exception " + msg);
                        err.close();
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }

    /**
     * This class is the invocation handler used for method dispatching
     * to an ocaml object value (possibly with method name translation).
     */
    private static final class ObjectHandler implements InvocationHandler {

        /** Object to call methods on. */
        private final Value object;

        /** Underlying code runner. */
        private final CodeRunner codeRunner;

        /** Map from called methods to ocaml method names. */
        private final Map<Method, String> translation;

        /**
         * Constructs a new object handler.
         * @param obj object value to call methods on
         * @param cr code runner
         * @param transl translation table from methods to names
         *               - can be <tt>null</tt>
         */
        private ObjectHandler(final Value obj, final CodeRunner cr, final Map<Method, String> transl) {
            this.object = obj;
            this.codeRunner = cr;
            this.translation = transl;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final String methodName = this.translation != null ? this.translation.get(method) : method.getName();
            if (methodName != null) {
                try {
                    final Context ctxt = this.codeRunner.getContext();
                    final Value encoder = ctxt.getCallback(Proxies.ENCODE_VALUE);
                    final Value decoder = ctxt.getCallback(Proxies.DECODE_VALUE);
                    final Value h = Hash.hashVariant(methodName);
                    final Value meth = AbstractCodeRunner.getMethod(this.object, h.getRawValue());
                    final int len = args != null ? args.length : 0;
                    final Value[] params = new Value[len + 1];
                    params[0] = this.object;
                    for (int i = 0; i < len; i++) {
                        params[i + 1] = this.codeRunner.callback(encoder, Cadmium.encodeObject(args[i]));
                    }
                    final Value rawResult = this.codeRunner.callback(meth, params);
                    final Value decodedResult = this.codeRunner.callback(decoder, rawResult);
                    return Cadmium.decodeObject(decodedResult);
                } catch (final Fail.Exception fe) {
                    final Context ctxt = this.codeRunner.getContext();
                    final Value exn = fe.asValue(ctxt.getGlobalData());
                    final Value javaExn = ctxt.getCallback("Cadmium.Java_exception");
                    try {
                        if ((javaExn != null) && exn.isBlock() && (exn.asBlock().sizeValues() >= 1) && (Compare.caml_equal(this.codeRunner, exn.asBlock().get(0), javaExn) == Value.TRUE)) {
                            final Object obj = exn.asBlock().get(1).asBlock().get(2).asBlock().asCustom();
                            ((Throwable) obj).fillInStackTrace();
                            if (obj instanceof Error) {
                                throw (Error) obj;
                            } else if (obj instanceof RuntimeException) {
                                throw (RuntimeException) obj;
                            }
                        } else {
                            final Channel ch = ctxt.getChannel(Channel.STDERR);
                            if ((ch != null) && (ch.asOutputStream() != null)) {
                                final String msg = Misc.convertException(fe.asValue(ctxt.getGlobalData()));
                                final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                                err.println("Error in proxy: exception " + msg);
                                err.close();
                            }
                        }
                    } catch (final Fail.Exception fe2) {
                        final Channel ch = ctxt.getChannel(Channel.STDERR);
                        if ((ch != null) && (ch.asOutputStream() != null)) {
                            final String msg = Misc.convertException(fe2.asValue(ctxt.getGlobalData()));
                            final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                            err.println("Error in proxy: exception " + msg);
                            err.close();
                        }
                    }
                } catch (final Fatal.Exception fe) {
                    final Context ctxt = this.codeRunner.getContext();
                    final Channel ch = ctxt.getChannel(Channel.STDERR);
                    if ((ch != null) && (ch.asOutputStream() != null)) {
                        final String msg = fe.getMessage();
                        final PrintStream err = new PrintStream(ch.asOutputStream(), true);
                        err.println("Error in proxy: exception " + msg);
                        err.close();
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }
}
