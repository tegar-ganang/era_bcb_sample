package java.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** <p><code>DefaultPersistenceDelegate</code> is a {@link PersistenceDelegate}
 * implementation that can be used to serialize objects which adhere to the
 * Java Beans naming convention.</p>
 * 
 * @author Robert Schuster (robertschuster@fsfe.org)
 * @since 1.4
 */
public class DefaultPersistenceDelegate extends PersistenceDelegate {

    private String[] constructorPropertyNames;

    /** Using this constructor the object to be serialized will be instantiated
   * with the default non-argument constructor.
   */
    public DefaultPersistenceDelegate() {
    }

    /** This constructor allows to specify which Bean properties appear
   * in the constructor.
   * 
   * <p>The implementation reads the mentioned properties from the Bean
   * instance and applies it in the given order to a corresponding
   * constructor.</p>
   * 
   * @param constructorPropertyNames The properties the Bean's constructor
   * should be given to.
   */
    public DefaultPersistenceDelegate(String[] constructorPropertyNames) {
        this.constructorPropertyNames = constructorPropertyNames;
    }

    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        try {
            return (constructorPropertyNames != null && constructorPropertyNames.length > 0 && oldInstance.getClass().getDeclaredMethod("equals", new Class[] { Object.class }) != null) ? oldInstance.equals(newInstance) : super.mutatesTo(oldInstance, newInstance);
        } catch (NoSuchMethodException nsme) {
            return super.mutatesTo(oldInstance, newInstance);
        }
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        Object[] args = null;
        try {
            if (constructorPropertyNames != null) {
                args = new Object[constructorPropertyNames.length];
                PropertyDescriptor[] propertyDescs = Introspector.getBeanInfo(oldInstance.getClass()).getPropertyDescriptors();
                for (int i = 0; i < constructorPropertyNames.length; i++) {
                    for (int j = 0; j < propertyDescs.length; j++) {
                        if (propertyDescs[i].getName().equals(constructorPropertyNames[i])) {
                            Method readMethod = propertyDescs[i].getReadMethod();
                            args[i] = readMethod.invoke(oldInstance, null);
                        }
                    }
                }
            }
        } catch (IllegalAccessException iae) {
            out.getExceptionListener().exceptionThrown(iae);
        } catch (IllegalArgumentException iarge) {
            out.getExceptionListener().exceptionThrown(iarge);
        } catch (InvocationTargetException ite) {
            out.getExceptionListener().exceptionThrown(ite);
        } catch (IntrospectionException ie) {
            out.getExceptionListener().exceptionThrown(ie);
        }
        return new Expression(oldInstance, oldInstance.getClass(), "new", args);
    }

    protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
        try {
            PropertyDescriptor[] propertyDescs = Introspector.getBeanInfo(oldInstance.getClass()).getPropertyDescriptors();
            for (int i = 0; i < propertyDescs.length; i++) {
                Method readMethod = propertyDescs[i].getReadMethod();
                Method writeMethod = propertyDescs[i].getWriteMethod();
                if (readMethod != null && writeMethod != null) {
                    Object oldValue = readMethod.invoke(oldInstance, null);
                    if (oldValue != null) out.writeStatement(new Statement(oldInstance, writeMethod.getName(), new Object[] { oldValue }));
                }
            }
        } catch (IntrospectionException ie) {
            out.getExceptionListener().exceptionThrown(ie);
        } catch (IllegalAccessException iae) {
            out.getExceptionListener().exceptionThrown(iae);
        } catch (InvocationTargetException ite) {
            out.getExceptionListener().exceptionThrown(ite);
        }
    }
}
