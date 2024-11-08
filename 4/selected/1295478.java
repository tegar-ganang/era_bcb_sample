package net.sf.nanomvc.flow.runtime;

import java.lang.reflect.Method;
import net.sf.nanomvc.core.NanoRuntimeException;

public class PropertyAccessor {

    private Method readMethod;

    private Method writeMethod;

    public PropertyAccessor(final Method readMethod, final Method writeMethod) {
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public Object read(Object object) {
        try {
            return readMethod.invoke(object);
        } catch (Exception e) {
            throw new NanoRuntimeException(e);
        }
    }

    public void write(Object object, Object value) {
        try {
            writeMethod.invoke(object, value);
        } catch (Exception e) {
            throw new NanoRuntimeException(e);
        }
    }
}
