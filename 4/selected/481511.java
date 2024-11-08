package net.sf.jpasecurity.mapping;

import java.lang.reflect.Method;
import net.sf.jpasecurity.util.ReflectionUtils;

/**
 * @author Arne Limburg
 */
public class ReflectionMethodAccessStrategy implements PropertyAccessStrategy {

    private Method readMethod;

    private Method writeMethod;

    public ReflectionMethodAccessStrategy(Method readMethod, Method writeMethod) {
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public Object getPropertyValue(Object target) {
        return ReflectionUtils.invokeMethod(target, readMethod);
    }

    public void setPropertyValue(Object target, Object value) {
        ReflectionUtils.invokeMethod(target, writeMethod, value);
    }
}
