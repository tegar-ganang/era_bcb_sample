package org.beanutopia.reflect;

import com.google.common.base.Function;
import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import org.beanutopia.exception.PropertyDefinitionException;
import org.beanutopia.function.Injection;
import static org.beanutopia.reflect.Reflector.reader;
import static org.beanutopia.reflect.Reflector.writer;
import org.beanutopia.slot.Slot;

/**
 * @author: Yardena
 * @date: Oct 27, 2009 5:12:08 PM
 */
public class ReflectSlot<V> implements Slot<Object, V> {

    protected GetMethod<V> reader;

    protected SetMethod<V> writer;

    private ReflectSlot(@Nullable Class objectClass, String propertyName, @Nullable Class<V> valueClass, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        reader = reader(objectClass, propertyName, valueClass, getterPrefix);
        writer = writer(objectClass, propertyName, valueClass, setterPrefix);
    }

    public Function<Object, V> getProjection() {
        return reader;
    }

    public Injection<Object, V> getInjection() {
        return writer;
    }

    public Predicate<Object> supported() {
        return predicate;
    }

    public Slot<Object, V> initialize() throws PropertyDefinitionException {
        reader.initialize();
        writer.initialize();
        return this;
    }

    private final Predicate<Object> predicate = new Predicate<Object>() {

        public boolean apply(Object o) {
            return reader.supported().apply(o) && writer.supported().apply(o);
        }
    };

    public static <V> Slot<Object, V> reflector(String propertyName) {
        return reflector(propertyName, "get", "set");
    }

    public static <V> Slot<Object, V> reflector(String propertyName, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        return reflector(null, propertyName, null, getterPrefix, setterPrefix);
    }

    public static <V> Slot<Object, V> reflector(@Nullable Class objectClass, String propertyName, @Nullable Class<V> valueClass) {
        return reflector(objectClass, propertyName, valueClass, "get", "set");
    }

    public static <V> Slot<Object, V> reflector(@Nullable Class objectClass, String propertyName, @Nullable Class<V> valueClass, @Nullable String getterPrefix, @Nullable String setterPrefix) {
        return new ReflectSlot<V>(objectClass, propertyName, valueClass, getterPrefix, setterPrefix);
    }
}
