package net.sf.rcpforms.experimenting.model.bean;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import net.sf.rcpforms.experimenting.model.bean.util.BeanUtil;
import net.sf.rcpforms.experimenting.model.bean.util.BeanUtil.BeanClassInfo;
import org.apache.log4j.Logger;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.internal.adaptor.IModel;

public class ModelHelper {

    @SuppressWarnings("all")
    private static final Logger log = Logger.getLogger(ModelHelper.class);

    /**
	 * {@link IUIModel#isDirty() Checkt} rekursiv den <b>dirty-status</b>
	 * dieses <code>model</code>'s sowie aller seiner Untermodelle. Properties
	 * eines Array- oder Collection-Typs werden interativ nach Ver�nderung
	 * �berpr�ft. 
	 * 
	 * @param model the model
	 * 
	 * @return true, if is model dirty recursive
	 */
    public static boolean isModelDirtyRecursive(final IUIModel model) {
        if (model == null) {
            return false;
        }
        if (model.isDirty()) {
            return true;
        }
        final BeanClassInfo inputInfo = BeanUtil.getBeanClassInfo(model.getClass());
        for (final PropertyDescriptor descriptor : inputInfo.propertyDescriptors) {
            final String name = descriptor.getName();
            final Object value = BeanUtil.getProperty(model, name, true);
            if (value != null) {
                if (value instanceof IUIModel) {
                    if (isModelDirtyRecursive((IUIModel) value)) {
                        return true;
                    }
                    if (value instanceof Object[]) {
                        final Object[] array = (Object[]) value;
                        for (final Object object : array) {
                            if (object instanceof IUIModel) {
                                if (isModelDirtyRecursive((IUIModel) object)) {
                                    return true;
                                }
                            }
                        }
                    } else if (value instanceof Collection) {
                        final Collection collection = (Collection) value;
                        for (final Object object : collection) {
                            if (object instanceof IUIModel) {
                                if (isModelDirtyRecursive((IUIModel) object)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static <T> List<T> cloneListShallow(final List<T> list) {
        if (list == null) {
            return list;
        }
        if (list instanceof Vector<?>) {
            return (Vector<T>) ((Vector<T>) list).clone();
        } else if (list instanceof ArrayList<?>) {
            return (ArrayList<T>) ((ArrayList<T>) list).clone();
        } else if (list instanceof WritableList) {
            final ArrayList copy = new ArrayList(list.size() + 1);
            copy.addAll(list);
            return new WritableList(copy, ((WritableList) list).getElementType());
        } else {
            try {
                final Class<? extends Object> clazz = list.getClass();
                final Method cloneMethod = clazz.getMethod("clone", new Class[0]);
                if (!cloneMethod.isAccessible()) {
                    cloneMethod.setAccessible(true);
                }
                if (Modifier.isPublic(cloneMethod.getModifiers())) {
                    final List<T> cloned = (List<T>) cloneMethod.invoke(list, new Object[0]);
                    return cloned;
                }
            } catch (final NoSuchMethodException e) {
            } catch (final IllegalArgumentException e) {
            } catch (final IllegalAccessException e) {
            } catch (final InvocationTargetException e) {
            }
        }
        throw new IllegalStateException("list-cloneing: List of type '" + list.getClass() + "' does not support clone()...");
    }

    public static List<?> cloneListDeep(final List<?> list) {
        if (list == null) {
            return null;
        }
        final List result = cloneListShallow(list);
        final int len = result.size();
        for (int i = 0; i < len; i++) {
            final Object item = result.get(i);
            if (item instanceof OIDJavaBean) {
                final OIDJavaBean bean = (OIDJavaBean) item;
                final OIDJavaBean cloned = bean.clone();
                result.set(i, cloned);
            } else if (item instanceof List) {
                final List cloned = cloneListShallow(list);
                result.set(i, cloned);
            }
        }
        return result;
    }

    public static <T extends OIDJavaBean> List<T> cloneListOfModelsDeep(final List<T> list) {
        if (list == null) {
            return null;
        }
        final List<T> result = cloneListShallow(list);
        final int len = result.size();
        for (int i = 0; i < len; i++) {
            final T cloned = (T) result.get(i).clone();
            result.set(i, cloned);
        }
        return result;
    }

    public static <T extends OIDJavaBean> T cloneBeanDeep(final T bean) {
        if (bean == null) {
            return null;
        }
        try {
            final T result = BeanUtil.cloneBean(bean);
            final BeanInfo beanInfo = Introspector.getBeanInfo(result.getClass());
            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (final PropertyDescriptor descriptor : propertyDescriptors) {
                final Class<?> propertyType = descriptor.getPropertyType();
                if (IModel.class.isAssignableFrom(propertyType)) {
                    final Method readMethod = descriptor.getReadMethod();
                    final Method writeMethod = descriptor.getWriteMethod();
                    if (!readMethod.isAccessible()) {
                        readMethod.setAccessible(true);
                    }
                    if (!writeMethod.isAccessible()) {
                        writeMethod.setAccessible(true);
                    }
                    if (readMethod != null && writeMethod != null && readMethod.isAccessible() && writeMethod.isAccessible()) {
                        final Object child0 = readMethod.invoke(result, new Object[0]);
                        final OIDJavaBean child = cloneBeanDeep((OIDJavaBean) child0);
                        writeMethod.invoke(result, child);
                    }
                } else if (List.class.isAssignableFrom(propertyType)) {
                    final Method readMethod = descriptor.getReadMethod();
                    final Method writeMethod = descriptor.getWriteMethod();
                    if (!readMethod.isAccessible()) {
                        readMethod.setAccessible(true);
                    }
                    if (!writeMethod.isAccessible()) {
                        writeMethod.setAccessible(true);
                    }
                    if (readMethod != null && writeMethod != null && readMethod.isAccessible() && writeMethod.isAccessible()) {
                        final List child0 = (List) readMethod.invoke(result, new Object[0]);
                        final List clonedList = cloneListDeep(child0);
                        writeMethod.invoke(result, clonedList);
                    }
                }
            }
            return result;
        } catch (final Throwable ex) {
            final String error = "OkAuthServiceSimulator: ERROR: can't clone bean '" + bean.getClass().getName() + "': " + ex.getMessage();
            log.error(error, ex);
            return null;
        }
    }

    public static void activateModel(final OIDJavaBean model) {
        if (model == null) {
            return;
        }
        model.activateSelfawareness(true);
        try {
            final BeanInfo beanInfo = Introspector.getBeanInfo(model.getClass());
            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (final PropertyDescriptor descriptor : propertyDescriptors) {
                if (OIDJavaBean.class.isAssignableFrom(descriptor.getPropertyType())) {
                    final Method readMethod = descriptor.getReadMethod();
                    if (readMethod != null && readMethod.isAccessible()) {
                        final Object child = readMethod.invoke(model, new Object[0]);
                        activateModel((OIDJavaBean) child);
                    }
                }
            }
        } catch (final Throwable ex) {
            final String error = "OkAuthServiceSimulator: ERROR: " + ex.getMessage();
            log.error(error, ex);
        }
    }

    public static void activateModel(final List<? extends OIDJavaBean> list) {
        for (final OIDJavaBean one : list) {
            activateModel(one);
        }
    }
}
