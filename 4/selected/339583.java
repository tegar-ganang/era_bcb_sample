package net.sourceforge.javautil.common.reflection.cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import net.sourceforge.javautil.common.reflection.ReflectionContext;

/**
 * A class property descriptor and access bridge. A property
 * is a getter and/or setter that follows the Java Beans specification.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: ClassProperty.java 2722 2011-01-16 05:38:59Z ponderator $
 */
public class ClassProperty extends ClassMember implements IClassMemberWritableValue {

    /**
	 * The name of the property
	 */
    protected final String name;

    /**
	 * The reader method of the property, null if not readable
	 */
    protected final ClassMethod reader;

    /**
	 * The writer method of the property, null if not writable
	 */
    protected final ClassMethod writer;

    /**
	 * The type of this property
	 */
    protected final Class<?> type;

    /**
	 * @param descriptor The {@link ClassMember#descriptor}
	 * @param name The {@link #name}
	 * @param reader The {@link #reader}
	 * @param writer The {@link #writer}
	 */
    public ClassProperty(ClassDescriptor descriptor, String name, ClassMethod reader, ClassMethod writer) {
        super(descriptor);
        this.name = name;
        this.reader = reader;
        this.writer = writer;
        this.type = reader != null ? reader.getReturnType() : writer.getParameterTypes()[0];
    }

    /**
	 * @return The descriptor
	 */
    public ClassDescriptor<?> getDescriptor() {
        return descriptor;
    }

    /**
	 * If this is a write only property, the annotation will be retrieved from the
	 * writer method, otherwise from the reader method.
	 *  
	 * @param <A> The annotation type
	 * @param clazz The annotation class
	 * @return The annotation if present, otherwise null
	 */
    public <A extends Annotation> A getAnnotation(Class<A> clazz) {
        A annotation = reader != null ? reader.getAnnotation(clazz) : null;
        return annotation == null && writer != null ? writer.getAnnotation(clazz) : annotation;
    }

    @Override
    public Annotation[] getAnnotations() {
        return reader == null ? writer.getAnnotations() : reader.getAnnotations();
    }

    @Override
    public Class getBaseType() {
        return reader == null ? writer.getBaseType() : reader.getBaseType();
    }

    @Override
    public Method getJavaMember() {
        return reader == null ? writer.getJavaMember() : reader.getJavaMember();
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(this.getJavaMember().getModifiers());
    }

    /**
	 * @return The {@link #name}
	 */
    public String getName() {
        return name;
    }

    /**
	 * @return The {@link #reader}
	 */
    public ClassMethod getReader() {
        return reader;
    }

    /**
	 * @return The {@link #writer}
	 */
    public ClassMethod getWriter() {
        return writer;
    }

    /**
	 * If false, {@link #getValue(Object)} will fail.
	 * 
	 * @return True if this property is readable, otherwise false
	 */
    public boolean isReadable() {
        return this.reader != null;
    }

    /**
	 * If false, {@link #setValue(Object, Object)} will fail.
	 * 
	 * @return True if this property is writable, otherwise false
	 */
    public boolean isWritable() {
        return this.writer != null;
    }

    /**
	 * @return The {@link #type}
	 */
    public Class<?> getType() {
        return type;
    }

    /**
	 * @return The generic type of this property
	 */
    public Type getGenericType() {
        return reader == null ? writer.getGenericParameterTypes()[0] : reader.getGenericReturnType();
    }

    /**
	 * @return A descriptor for the {@link #type}
	 */
    public ClassDescriptor<?> getTypeDescriptor() {
        return descriptor.cache.getDescriptor(type);
    }

    /**
	 * Set the current value of the property on the instance or class.
	 * 
	 * @param instance The instance on which to set the property, null if it is a static property
	 * @param value The value to assign to the property of the instance/class
	 * 
	 * @see #isWritable()
	 * @throws ClassPropertyAccessException
	 */
    public void setValue(Object instance, Object value) {
        if (writer == null) throw new ClassPropertyAccessException(descriptor, this, "Property not writable");
        writer.invoke(instance, value);
    }

    /**
	 * Get the current value of the property on the instance or class.
	 * 
	 * @param instance The instance on which to get the property, null if it is a static property
	 * @return The current value of the property
	 * 
	 * @see #isReadable()
	 * @throws ClassPropertyAccessException
	 */
    public Object getValue(Object instance) {
        if (reader == null) throw new ClassPropertyAccessException(descriptor, this, "Property not readable");
        return reader.invoke(instance);
    }
}
