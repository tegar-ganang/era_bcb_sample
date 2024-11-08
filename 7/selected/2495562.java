package com.memoire.foo;

import com.memoire.foo.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class FooClass {

    private static FooCategory pkg_ = null;

    public static final FooCategory init() {
        if (pkg_ == null) {
            pkg_ = FooCategory.create(Class.class);
            pkg_.alias("&class");
            pkg_.setMessage("new", FooClass.class, "newMsg");
            pkg_.setMessage("new[]", FooClass.class, "newArrayMsg");
            pkg_.setMessage("field", FooClass.class, "fieldMsg");
            pkg_.setMessage("method", FooClass.class, "methodMsg");
            pkg_.setMessage("constructor", FooClass.class, "constructorMsg");
            pkg_.setMessage("category", FooClass.class, "categoryMsg");
            pkg_.setMessage("message", FooClass.class, "messageEval", false);
            pkg_.setMessage("message:", FooClass.class, "messageRaw", false);
        }
        return pkg_;
    }

    public static Object newMsg(Object[] _p) {
        FooLib.checkClassArgument(Class.class, _p[0], 0);
        Class c = (Class) _p[0];
        if (c.isArray()) {
            int l = _p.length - 1;
            int[] d = new int[l];
            for (int i = 0; i < l; i++) {
                c = c.getComponentType();
                d[i] = FooLib.toInteger(_p[i + 1]).intValue();
            }
            return Array.newInstance(c, d);
        }
        Constructor m = FooLib.getConstructor(c, c.getName(), _p.length - 1);
        if (m == null) {
            StringBuffer s = new StringBuffer(c.getName() + "(");
            for (int j = 0; j < _p.length; j++) {
                if (j > 0) s.append(',');
                s.append(FooLib.getClassName(_p[j]));
            }
            s.append(')');
            throw new RuntimeException("constructor not found:" + s);
        }
        Object[] q = new Object[_p.length - 1];
        for (int i = 0; i < q.length; i++) q[i] = _p[i + 1];
        return FooLib.invokeConstructor(m, q);
    }

    public static Object newArrayMsg(Object[] _p) {
        FooLib.checkClassArgument(Class.class, _p[0], 0);
        if (_p.length < 2) FooLib.checkNumberArgument(2, _p.length);
        if (_p.length > 3) FooLib.checkNumberArgument(3, _p.length);
        Class c = (Class) _p[0];
        int d = FooLib.toInteger(_p[1]).intValue();
        Object v = _p[2];
        Object r = Array.newInstance(c, d);
        for (int i = 0; i < d; i++) Array.set(r, i, v);
        return r;
    }

    public static Object fieldMsg(Class _clazz, String _name) {
        return FooLib.getField(_clazz, _name);
    }

    public static Object methodMsg(Class _clazz, String _signature) {
        return FooLib.getMethod(_clazz, _signature);
    }

    public static Object constructorMsg(Class _clazz, String _signature) {
        return FooLib.getConstructor(_clazz, _signature);
    }

    public static FooCategory categoryMsg(Class _clazz) {
        return FooCategory.create(_clazz);
    }

    public static Object messageEval(Object[] _o) {
        FooLib.checkClassArgument(Class.class, _o[0], 0);
        _o[0] = FooCategory.create((Class) _o[0]);
        return FooCategory.messageEval(_o);
    }

    public static Object messageRaw(Object[] _o) {
        FooLib.checkClassArgument(Class.class, _o[0], 0);
        _o[0] = FooCategory.create((Class) _o[0]);
        return FooCategory.messageRaw(_o);
    }
}
