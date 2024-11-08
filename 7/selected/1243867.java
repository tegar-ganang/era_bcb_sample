package jfun.yan;

import jfun.util.SerializableMethod;
import jfun.yan.function.Function;

/**
 * For an instance method with the receiver object not fixed.
 * <p>
 * Zephyr Business Solution
 *
 * @author Ben Yu
 *
 */
final class FloatingMethodFunction implements Function {

    private final Class type;

    private final SerializableMethod mtd;

    public boolean isConcrete() {
        return false;
    }

    FloatingMethodFunction(final Class type, final java.lang.reflect.Method mtd) {
        this.type = type;
        this.mtd = new SerializableMethod(mtd);
    }

    public Class getReturnType() {
        return mtd.getMethod().getReturnType();
    }

    public Class[] getParameterTypes() {
        final Class[] ptypes = mtd.getMethod().getParameterTypes();
        final Class[] r = new Class[ptypes.length + 1];
        r[0] = type;
        for (int i = 1; i < r.length; i++) {
            r[i] = ptypes[i - 1];
        }
        return r;
    }

    public Object call(Object[] args) throws Throwable {
        final Object[] nargs = new Object[args.length - 1];
        for (int i = 0; i < nargs.length; i++) {
            nargs[i] = args[i + 1];
        }
        try {
            return mtd.getMethod().invoke(args[0], nargs);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw Utils.wrapInvocationException(e);
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof FloatingMethodFunction) {
            final FloatingMethodFunction other = (FloatingMethodFunction) obj;
            return type.equals(other.type) && mtd.equals(other.mtd);
        } else return false;
    }

    public int hashCode() {
        return type.hashCode() * 31 + mtd.hashCode();
    }

    public String toString() {
        return jfun.util.Misc.getTypeName(type) + "::" + mtd.getMethod().getName() + jfun.util.StringUtils.listString("(", ",", ")", mtd.getMethod().getParameterTypes());
    }

    public String getName() {
        return mtd.getMethod().getName();
    }
}
