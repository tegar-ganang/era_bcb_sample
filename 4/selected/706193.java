package org.ibex.js;

import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import org.ibex.util.Hash;
import org.ibex.util.Log;
import org.ibex.util.Task;

/** The minimum set of functionality required for objects which are manipulated by JavaScript */
public class JS extends org.ibex.util.BalancedTree {

    public static boolean checkAssertions = false;

    public static final Object METHOD = new Object();

    public final JS unclone() {
        return _unclone();
    }

    public Enumeration keys() throws JSExn {
        return entries == null ? emptyEnumeration : entries.keys();
    }

    public Object get(Object key) throws JSExn {
        return entries == null ? null : entries.get(key, null);
    }

    public void put(Object key, Object val) throws JSExn {
        (entries == null ? entries = new Hash() : entries).put(key, null, val);
    }

    public Object callMethod(Object method, Object a0, Object a1, Object a2, Object[] rest, int nargs) throws JSExn {
        throw new JSExn("attempted to call the null value (method " + method + ")");
    }

    public Object call(Object a0, Object a1, Object a2, Object[] rest, int nargs) throws JSExn {
        throw new JSExn("you cannot call this object (class=" + this.getClass().getName() + ")");
    }

    JS _unclone() {
        return this;
    }

    public static class Cloneable extends JS {

        public Object jsclone() throws JSExn {
            return new Clone(this);
        }
    }

    public static class Clone extends JS.Cloneable {

        protected JS.Cloneable clonee = null;

        JS _unclone() {
            return clonee.unclone();
        }

        public JS.Cloneable getClonee() {
            return clonee;
        }

        public Clone(JS.Cloneable clonee) {
            this.clonee = clonee;
        }

        public boolean equals(Object o) {
            if (!(o instanceof JS)) return false;
            return unclone() == ((JS) o).unclone();
        }

        public Enumeration keys() throws JSExn {
            return clonee.keys();
        }

        public Object get(Object key) throws JSExn {
            return clonee.get(key);
        }

        public void put(Object key, Object val) throws JSExn {
            clonee.put(key, val);
        }

        public Object callMethod(Object method, Object a0, Object a1, Object a2, Object[] rest, int nargs) throws JSExn {
            return clonee.callMethod(method, a0, a1, a2, rest, nargs);
        }

        public Object call(Object a0, Object a1, Object a2, Object[] rest, int nargs) throws JSExn {
            return clonee.call(a0, a1, a2, rest, nargs);
        }
    }

    /** log a message with the current JavaScript sourceName/line */
    public static void log(Object message) {
        info(message);
    }

    public static void debug(Object message) {
        Log.debug(Interpreter.getSourceName() + ":" + Interpreter.getLine(), message);
    }

    public static void info(Object message) {
        Log.info(Interpreter.getSourceName() + ":" + Interpreter.getLine(), message);
    }

    public static void warn(Object message) {
        Log.warn(Interpreter.getSourceName() + ":" + Interpreter.getLine(), message);
    }

    public static void error(Object message) {
        Log.error(Interpreter.getSourceName() + ":" + Interpreter.getLine(), message);
    }

    public static class NotPauseableException extends Exception {

        NotPauseableException() {
        }
    }

    /** returns a callback which will restart the context; expects a value to be pushed onto the stack when unpaused */
    public static UnpauseCallback pause() throws NotPauseableException {
        Interpreter i = Interpreter.current();
        if (i.pausecount == -1) throw new NotPauseableException();
        i.pausecount++;
        return new JS.UnpauseCallback(i);
    }

    public static class UnpauseCallback implements Task {

        Interpreter i;

        UnpauseCallback(Interpreter i) {
            this.i = i;
        }

        public void perform() throws JSExn {
            unpause(null);
        }

        public void unpause(Object o) throws JSExn {
            i.stack.push(o);
            i.resume();
        }
    }

    /** coerce an object to a Boolean */
    public static boolean toBoolean(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return ((Boolean) o).booleanValue();
        if (o instanceof Long) return ((Long) o).longValue() != 0;
        if (o instanceof Integer) return ((Integer) o).intValue() != 0;
        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            return d != 0.0 && d == d;
        }
        if (o instanceof String) return ((String) o).length() != 0;
        return true;
    }

    /** coerce an object to a Long */
    public static long toLong(Object o) {
        try {
            return toNumber(o).longValue();
        } catch (JSExn jsexn) {
            throw new Error(jsexn);
        }
    }

    /** coerce an object to an Int */
    public static int toInt(Object o) {
        try {
            return toNumber(o).intValue();
        } catch (JSExn jsexn) {
            throw new Error(jsexn);
        }
    }

    /** coerce an object to a Double */
    public static double toDouble(Object o) {
        try {
            return toNumber(o).doubleValue();
        } catch (JSExn jsexn) {
            throw new Error(jsexn);
        }
    }

    /** coerce an object to a Number */
    public static Number toNumber(Object o) throws JSExn {
        if (o == null) return ZERO;
        if (o instanceof Number) return ((Number) o);
        if (o instanceof String) try {
            return N((String) o);
        } catch (NumberFormatException e) {
            return N(Double.NaN);
        }
        if (o instanceof Boolean) return ((Boolean) o).booleanValue() ? N(1) : ZERO;
        throw new JSExn("Can not use object of type " + o.getClass().getName() + " as a number");
    }

    /** coerce an object to a String */
    public static String toString(Object o) throws JSExn {
        if (o == null) return "null";
        if (o instanceof String) return (String) o;
        if (o instanceof Integer || o instanceof Long || o instanceof Boolean) return o.toString();
        if (o instanceof JSArray) return ((JSArray) o).join(",");
        if (o instanceof JSDate) return o.toString();
        if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if ((int) d == d) return Integer.toString((int) d);
            return o.toString();
        }
        return "[" + o.getClass().getName() + "]";
    }

    public static final Integer ZERO = new Integer(0);

    public static final Object T = Boolean.TRUE;

    public static final Object F = Boolean.FALSE;

    public static final Boolean B(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    public static final Boolean B(int i) {
        return i == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    public static final Number N(String s) {
        return s.indexOf('.') == -1 ? N(Integer.parseInt(s)) : new Double(s);
    }

    public static final Number N(double d) {
        return (int) d == d ? N((int) d) : new Double(d);
    }

    public static final Number N(long l) {
        return N((int) l);
    }

    private static final Integer[] smallIntCache = new Integer[65535 / 4];

    private static final Integer[] largeIntCache = new Integer[65535 / 4];

    public static final Number N(int i) {
        Integer ret = null;
        int idx = i + smallIntCache.length / 2;
        if (idx < smallIntCache.length && idx > 0) {
            ret = smallIntCache[idx];
            if (ret != null) return ret;
        } else ret = largeIntCache[Math.abs(idx % largeIntCache.length)];
        if (ret == null || ret.intValue() != i) {
            ret = new Integer(i);
            if (idx < smallIntCache.length && idx > 0) smallIntCache[idx] = ret; else largeIntCache[Math.abs(idx % largeIntCache.length)] = ret;
        }
        return ret;
    }

    private static Enumeration emptyEnumeration = new Enumeration() {

        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    private Hash entries = null;

    public static JS fromReader(String sourceName, int firstLine, Reader sourceCode) throws IOException {
        return JSFunction._fromReader(sourceName, firstLine, sourceCode);
    }

    public static JS cloneWithNewParentScope(JS j, JSScope s) {
        return ((JSFunction) j)._cloneWithNewParentScope(s);
    }

    /** override and return true to allow placing traps on this object.
     *  if isRead true, this is a read trap, otherwise write trap
     **/
    protected boolean isTrappable(Object name, boolean isRead) {
        return true;
    }

    /** performs a put, triggering traps if present; traps are run in an unpauseable interpreter */
    public void putAndTriggerTraps(Object key, Object value) throws JSExn {
        Trap t = getTrap(key);
        if (t != null) t.invoke(value); else put(key, value);
    }

    /** performs a get, triggering traps if present; traps are run in an unpauseable interpreter */
    public Object getAndTriggerTraps(Object key) throws JSExn {
        Trap t = getTrap(key);
        if (t != null) return t.invoke(); else return get(key);
    }

    /** retrieve a trap from the entries hash */
    protected final Trap getTrap(Object key) {
        return entries == null ? null : (Trap) entries.get(key, Trap.class);
    }

    /** retrieve a trap from the entries hash */
    protected final void putTrap(Object key, Trap value) {
        if (entries == null) entries = new Hash();
        entries.put(key, Trap.class, value);
    }

    /** adds a trap, avoiding duplicates */
    protected void addTrap(Object name, JSFunction f) throws JSExn {
        if (f.numFormalArgs > 1) throw new JSExn("traps must take either one argument (write) or no arguments (read)");
        boolean isRead = f.numFormalArgs == 0;
        if (!isTrappable(name, isRead)) throw new JSExn("not allowed " + (isRead ? "read" : "write") + " trap on property: " + name);
        for (Trap t = getTrap(name); t != null; t = t.next) if (t.f == f) return;
        putTrap(name, new Trap(this, name.toString(), f, (Trap) getTrap(name)));
    }

    /** deletes a trap, if present */
    protected void delTrap(Object name, JSFunction f) {
        Trap t = (Trap) getTrap(name);
        if (t == null) return;
        if (t.f == f) {
            putTrap(t.name, t.next);
            return;
        }
        for (; t.next != null; t = t.next) if (t.next.f == f) {
            t.next = t.next.next;
            return;
        }
    }
}
