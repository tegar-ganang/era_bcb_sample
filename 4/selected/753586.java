package org.apache.myfaces.trinidadinternal.ui.data.bean;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidadinternal.util.JavaIntrospector;
import org.apache.myfaces.trinidadinternal.share.expl.Coercions;
import org.apache.myfaces.trinidadinternal.ui.UIXRenderingContext;
import org.apache.myfaces.trinidadinternal.ui.data.MutableDataObject;

/**
 * The IntrospectionAdapter class can adapt a JavaBean-style
 * class into a DataObject.  It also supports some features
 * that JavaBeans does not:
 * <ol>
 *   <li>Public fields (that is - instance variables) are supported
 *   <li>Package-private classes are supported - but only if
 *       its properties can be retrieved after first casting
 *       to a public superclass or public interface.  So, for instance,
 *       if getFoo() is first defined in a package-private class,
 *       the "foo" property won't be accessible.  But if it's defined
 *       in a public interface that is implemented by that package-private
 *       class, then it will be accessible.
 * </ol>
 * <p>
 * IntrospectionAdapter will automatically cache the results
 * of introspection per-class, so it's fairly efficient.  Developers
 * may wish to create a more performant adapter class for beans
 * that are performance-critical;  see the BuildBeanDOAdapter tool.
 * <p>
 * @see org.apache.myfaces.trinidadinternal.ui.data.bean.BeanAdapterUtils
 * @see org.apache.myfaces.trinidadinternal.ui.tools.BuildBeanDOAdapter
 * @version $Name:  $ ($Revision: 245 $) $Date: 2008-11-25 19:05:42 -0500 (Tue, 25 Nov 2008) $
 * @deprecated This class comes from the old Java 1.2 UIX codebase and should not be used anymore.
 */
@Deprecated
public class IntrospectionAdapter implements BeanDOAdapter {

    /**
   * Creates an adapter around an object instance.
   */
    public static BeanDOAdapter getAdapter(Object instance) {
        if (instance == null) return null;
        Class<?> objClass = instance.getClass();
        ClassInfo classInfo = _getIntrospectionInfo(objClass);
        BeanDOAdapter adapter = classInfo.hasWriters() ? new MutableIntrospectionAdapter(objClass, classInfo) : new IntrospectionAdapter(objClass, classInfo);
        adapter.setInstance(instance);
        return adapter;
    }

    /**
   * Attaches an instance of the bean class to the adapter.
   */
    public void setInstance(Object instance) {
        if ((instance != null) && !_class.isInstance(instance)) throw new IllegalArgumentException(_LOG.getMessage("NOT_AN_INSTANCE", new Object[] { instance.toString(), _class }));
        _instance = instance;
    }

    public Object selectValue(UIXRenderingContext context, Object select) {
        Object instance = _instance;
        if ((instance == null) || (select == null)) return null;
        if (".".equals(select)) return instance;
        return _classInfo.selectValue(context, select, instance);
    }

    public void updateValue(UIXRenderingContext context, Object select, Object value) {
        Object instance = _instance;
        if ((instance != null) && (select != null)) _classInfo.updateValue(context, select, value, instance);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(40);
        String className = getClass().getName();
        buffer.append(className);
        buffer.append('[');
        buffer.append(_instance);
        buffer.append(']');
        return buffer.toString();
    }

    IntrospectionAdapter(Class<?> objClass, ClassInfo classInfo) {
        _class = objClass;
        _classInfo = classInfo;
    }

    /**
   * @deprecated This class comes from the old Java 1.2 UIX codebase and should not be used anymore.
   */
    @Deprecated
    private static class MutableIntrospectionAdapter extends IntrospectionAdapter implements MutableDataObject {

        public MutableIntrospectionAdapter(Class<?> objClass, ClassInfo classInfo) {
            super(objClass, classInfo);
        }
    }

    private static ClassInfo _getIntrospectionInfo(Class<?> objClass) {
        ClassInfo classInfo = _sIntrospectionInfo.get(objClass);
        if (classInfo == null) {
            classInfo = _createIntrospectionInfo(objClass);
            if (classInfo == null) classInfo = _sEmptyClassInfo;
            _sIntrospectionInfo.put(objClass, classInfo);
        }
        return classInfo;
    }

    private static ClassInfo _createIntrospectionInfo(Class<?> objClass) {
        if (objClass == Object.class) return null;
        Map<String, PropertyReader> readers = null;
        Map<String, PropertyWriter> writers = null;
        if (!Modifier.isPublic(objClass.getModifiers())) {
            Class<?> superClass = objClass.getSuperclass();
            if (superClass != null) {
                ClassInfo superClassInfo = _getIntrospectionInfo(superClass);
                if (superClassInfo != null) {
                    readers = new HashMap<String, PropertyReader>();
                    writers = new HashMap<String, PropertyWriter>();
                    superClassInfo.putAllReadersInto(readers);
                    superClassInfo.putAllWritersInto(writers);
                }
            }
            Class[] interfaces = objClass.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                ClassInfo interfaceInfo = _getIntrospectionInfo(interfaces[i]);
                if (interfaceInfo != null) {
                    if (readers == null) readers = new HashMap<String, PropertyReader>();
                    interfaceInfo.putAllReadersInto(readers);
                    if (writers == null) writers = new HashMap<String, PropertyWriter>();
                    interfaceInfo.putAllWritersInto(writers);
                }
            }
        } else {
            readers = new HashMap<String, PropertyReader>();
            writers = new HashMap<String, PropertyWriter>();
            try {
                BeanInfo info = JavaIntrospector.getBeanInfo(objClass);
                PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
                if (descriptors != null) {
                    for (int i = 0; i < descriptors.length; i++) {
                        String name = descriptors[i].getName();
                        Method reader = descriptors[i].getReadMethod();
                        Method writer = descriptors[i].getWriteMethod();
                        if (name != null) {
                            if (reader != null) {
                                readers.put(name, new MethodReader(reader));
                            }
                            if (writer != null) {
                                writers.put(name, new MethodWriter(writer));
                            }
                        }
                    }
                }
                Field[] fields = objClass.getFields();
                for (int i = 0; i < fields.length; i++) {
                    FieldReaderWriter frw = new FieldReaderWriter(fields[i]);
                    readers.put(fields[i].getName(), frw);
                    writers.put(fields[i].getName(), frw);
                }
            } catch (IntrospectionException ie) {
                _LOG.severe(ie);
            }
        }
        if (readers == null && writers == null) return null;
        return new ClassInfo(readers, writers);
    }

    private static class ClassInfo {

        public ClassInfo(Map<String, PropertyReader> readers, Map<String, PropertyWriter> writers) {
            _readers = readers;
            _writers = writers;
        }

        public void putAllReadersInto(Map<String, PropertyReader> into) {
            into.putAll(_readers);
        }

        public void putAllWritersInto(Map<String, PropertyWriter> into) {
            into.putAll(_writers);
        }

        public boolean hasWriters() {
            return (_writers != null && _writers.size() > 0);
        }

        public Object selectValue(UIXRenderingContext context, Object select, Object instance) {
            Object o = _readers.get(select);
            if (o == null) {
                _reportMissingProperty(select, instance);
            } else {
                try {
                    return ((PropertyReader) o).readProperty(context, instance);
                } catch (IllegalAccessException iae) {
                    _LOG.severe(iae);
                }
            }
            return null;
        }

        public void updateValue(UIXRenderingContext context, Object select, Object value, Object instance) {
            Object o = _writers.get(select);
            if (o == null) {
                _reportMissingProperty(select, instance);
            } else {
                try {
                    ((PropertyWriter) o).writeProperty(context, instance, value);
                } catch (IllegalAccessException iae) {
                    _LOG.severe(iae);
                }
            }
        }

        private void _reportMissingProperty(Object select, Object instance) {
            if (_LOG.isFine()) {
                _LOG.fine("No bean property named " + select + " on instance:" + instance + " of class:" + instance.getClass().getName());
            }
        }

        private Map<String, PropertyReader> _readers;

        private Map<String, PropertyWriter> _writers;
    }

    private static interface PropertyReader {

        public Object readProperty(UIXRenderingContext context, Object instance) throws IllegalAccessException;
    }

    private static interface PropertyWriter {

        public void writeProperty(UIXRenderingContext context, Object instance, Object value) throws IllegalAccessException;
    }

    private static class MethodReader implements PropertyReader {

        public MethodReader(Method method) {
            _method = method;
        }

        public Object readProperty(UIXRenderingContext context, Object instance) throws IllegalAccessException {
            try {
                return _method.invoke(instance);
            } catch (InvocationTargetException ite) {
                _logInvocationTargetException(ite);
            }
            return null;
        }

        private final Method _method;
    }

    private static class MethodWriter implements PropertyWriter {

        public MethodWriter(Method method) {
            _method = method;
            _type = method.getParameterTypes()[0];
        }

        public void writeProperty(UIXRenderingContext context, Object instance, Object value) throws IllegalAccessException {
            value = _coerceType(value, _type);
            if ((value == null) && _type.isPrimitive()) return;
            try {
                _method.invoke(instance, new Object[] { value });
            } catch (InvocationTargetException ite) {
                _logInvocationTargetException(ite);
            }
        }

        private final Method _method;

        private final Class<?> _type;
    }

    private static class FieldReaderWriter implements PropertyReader, PropertyWriter {

        public FieldReaderWriter(Field field) {
            _field = field;
        }

        public Object readProperty(UIXRenderingContext context, Object instance) throws IllegalAccessException {
            return _field.get(instance);
        }

        public void writeProperty(UIXRenderingContext context, Object instance, Object value) throws IllegalAccessException {
            value = _coerceType(value, _field.getType());
            _field.set(instance, value);
        }

        private final Field _field;
    }

    private static Object _coerceType(Object value, Class<?> type) {
        return Coercions.coerce(value, type);
    }

    private static void _logInvocationTargetException(InvocationTargetException ite) {
        Throwable t = ite.getTargetException();
        if (t instanceof RuntimeException) {
            throw ((RuntimeException) t);
        } else {
            _LOG.severe(t);
        }
    }

    private Class<?> _class;

    private Object _instance;

    private ClassInfo _classInfo;

    private static Map<Class<?>, ClassInfo> _sIntrospectionInfo;

    private static final ClassInfo _sEmptyClassInfo = new ClassInfo(new HashMap<String, PropertyReader>(1), new HashMap<String, PropertyWriter>(1));

    static {
        _sIntrospectionInfo = Collections.synchronizedMap(new WeakHashMap<Class<?>, ClassInfo>());
    }

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(IntrospectionAdapter.class);
}
