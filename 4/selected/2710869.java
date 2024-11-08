package java.beans;

import java.lang.reflect.Method;

/**
 ** PropertyDescriptor describes information about a JavaBean property,
 ** by which we mean a property that has been exposed via a pair of
 ** get and set methods.  (There may be no get method, which means
 ** the property is write-only, or no set method, which means the
 ** the property is read-only.)<P>
 **
 ** The constraints put on get and set methods are:<P>
 ** <OL>
 ** <LI>A get method must have signature
 **     <CODE>&lt;propertyType&gt; &lt;getMethodName&gt;()</CODE></LI>
 ** <LI>A set method must have signature
 **     <CODE>void &lt;setMethodName&gt;(&lt;propertyType&gt;)</CODE></LI>
 ** <LI>Either method type may throw any exception.</LI>
 ** <LI>Both methods must be public.</LI>
 ** </OL>
 **
 ** @author John Keiser
 ** @author Robert Schuster (thebohemian@gmx.net)
 ** @since 1.1
 ** @status updated to 1.4
 **/
public class PropertyDescriptor extends FeatureDescriptor {

    Class propertyType;

    Method getMethod;

    Method setMethod;

    Class propertyEditorClass;

    boolean bound;

    boolean constrained;

    PropertyDescriptor(String name) {
        setName(name);
    }

    /** Create a new PropertyDescriptor by introspection.
     ** This form of constructor creates the PropertyDescriptor by
     ** looking for a getter method named <CODE>get&lt;name&gt;()</CODE>
     ** (or, optionally, if the property is boolean,
     ** <CODE>is&lt;name&gt;()</CODE>) and
     ** <CODE>set&lt;name&gt;()</CODE> in class
     ** <CODE>&lt;beanClass&gt;</CODE>, where &lt;name&gt; has its
     ** first letter capitalized by the constructor.<P>
     **
     ** Note that using this constructor the given property must be read- <strong>and</strong>
     ** writeable. If the implementation does not both, a read and a write method, an
     ** <code>IntrospectionException</code> is thrown.
     **
     ** <B>Implementation note:</B> If there is both are both isXXX and
     ** getXXX methods, the former is used in preference to the latter.
     ** We do not check that an isXXX method returns a boolean. In both
     ** cases, this matches the behaviour of JDK 1.4<P>
     **
     ** @param name the programmatic name of the property, usually
     **             starting with a lowercase letter (e.g. fooManChu
     **             instead of FooManChu).
     ** @param beanClass the class the get and set methods live in.
     ** @exception IntrospectionException if the methods are not found 
     **            or invalid.
     **/
    public PropertyDescriptor(String name, Class beanClass) throws IntrospectionException {
        setName(name);
        if (name.length() == 0) {
            throw new IntrospectionException("empty property name");
        }
        String caps = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        findMethods(beanClass, "is" + caps, "get" + caps, "set" + caps);
        if (getMethod == null) {
            throw new IntrospectionException("Cannot find a is" + caps + " or get" + caps + " method");
        }
        if (setMethod == null) {
            throw new IntrospectionException("Cannot find a " + caps + " method");
        }
        propertyType = checkMethods(getMethod, setMethod);
    }

    /** Create a new PropertyDescriptor by introspection.
     ** This form of constructor allows you to specify the
     ** names of the get and set methods to search for.<P>
     **
     ** <B>Implementation note:</B> If there is a get method (or
     ** boolean isXXX() method), then the return type of that method
     ** is used to find the set method.  If there is no get method,
     ** then the set method is searched for exhaustively.<P>
     **
     ** <B>Spec note:</B>
     ** If there is no get method and multiple set methods with
     ** the same name and a single parameter (different type of course),
     ** then an IntrospectionException is thrown.  While Sun's spec
     ** does not state this, it can make Bean behavior different on
     ** different systems (since method order is not guaranteed) and as
     ** such, can be treated as a bug in the spec.  I am not aware of
     ** whether Sun's implementation catches this.
     **
     ** @param name the programmatic name of the property, usually
     **             starting with a lowercase letter (e.g. fooManChu
     **             instead of FooManChu).
     ** @param beanClass the class the get and set methods live in.
     ** @param getMethodName the name of the get method or <code>null</code> if the property is write-only.
     ** @param setMethodName the name of the set method or <code>null</code> if the property is read-only.
     ** @exception IntrospectionException if the methods are not found 
     **            or invalid.
     **/
    public PropertyDescriptor(String name, Class beanClass, String getMethodName, String setMethodName) throws IntrospectionException {
        setName(name);
        findMethods(beanClass, getMethodName, null, setMethodName);
        if (getMethod == null && getMethodName != null) {
            throw new IntrospectionException("Cannot find a getter method called " + getMethodName);
        }
        if (setMethod == null && setMethodName != null) {
            throw new IntrospectionException("Cannot find a setter method called " + setMethodName);
        }
        propertyType = checkMethods(getMethod, setMethod);
    }

    /** Create a new PropertyDescriptor using explicit Methods.
     ** Note that the methods will be checked for conformance to standard
     ** Property method rules, as described above at the top of this class.
     **<br>
     ** It is possible to call this method with both <code>Method</code> arguments
     ** being <code>null</code>. In such a case the property type is <code>null</code>.
     ** 
     ** @param name the programmatic name of the property, usually
     **             starting with a lowercase letter (e.g. fooManChu
     **             instead of FooManChu).
     ** @param readMethod the read method or <code>null</code> if the property is write-only.
     ** @param writeMethod the write method or <code>null</code> if the property is read-only.
     ** @exception IntrospectionException if the methods are not found 
     **            or invalid.
     **/
    public PropertyDescriptor(String name, Method readMethod, Method writeMethod) throws IntrospectionException {
        setName(name);
        getMethod = readMethod;
        setMethod = writeMethod;
        propertyType = checkMethods(getMethod, setMethod);
    }

    /** Get the property type.
     ** This is the type the get method returns and the set method
     ** takes in.
     **/
    public Class getPropertyType() {
        return propertyType;
    }

    /** Get the get method.  Why they call it readMethod here and
     ** get everywhere else is beyond me.
     **/
    public Method getReadMethod() {
        return getMethod;
    }

    /** Sets the read method.<br/>
     * The read method is used to retrieve the value of a property. A legal
     * read method must have no arguments. Its return type must not be
     * <code>void</code>. If this methods succeeds the property type
     * is adjusted to the return type of the read method.<br/>
     * <br/>
     * It is legal to set the read and the write method to <code>null</code>
     * or provide method which have been declared in distinct classes.
     * 
     * @param readMethod The new method to be used or <code>null</code>.
     * @throws IntrospectionException If the given method is invalid.
     * @since 1.2
     */
    public void setReadMethod(Method readMethod) throws IntrospectionException {
        propertyType = checkMethods(readMethod, setMethod);
        getMethod = readMethod;
    }

    /** Get the set method.  Why they call it writeMethod here and
     ** set everywhere else is beyond me.
     **/
    public Method getWriteMethod() {
        return setMethod;
    }

    /** Sets the write method.<br/>
     * The write method is used to set the value of a property. A legal write method
     * must have a single argument which can be assigned to the property. If no
     * read method exists the property type changes to the argument type of the
     * write method.<br/>
     * <br/>
     * It is legal to set the read and the write method to <code>null</code>
     * or provide method which have been declared in distinct classes.
     * 
     * @param writeMethod The new method to be used or <code>null</code>.
     * @throws IntrospectionException If the given method is invalid.
     * @since 1.2
     */
    public void setWriteMethod(Method writeMethod) throws IntrospectionException {
        propertyType = checkMethods(getMethod, writeMethod);
        setMethod = writeMethod;
    }

    /** Get whether the property is bound.  Defaults to false. **/
    public boolean isBound() {
        return bound;
    }

    /** Set whether the property is bound.
     ** As long as the the bean implements addPropertyChangeListener() and
     ** removePropertyChangeListener(), setBound(true) may safely be called.<P>
     ** If these things are not true, then the behavior of the system
     ** will be undefined.<P>
     **
     ** When a property is bound, its set method is required to fire the
     ** <CODE>PropertyChangeListener.propertyChange())</CODE> event
     ** after the value has changed.
     ** @param bound whether the property is bound or not.
     **/
    public void setBound(boolean bound) {
        this.bound = bound;
    }

    /** Get whether the property is constrained.  Defaults to false. **/
    public boolean isConstrained() {
        return constrained;
    }

    /** Set whether the property is constrained.
     ** If the set method throws <CODE>java.beans.PropertyVetoException</CODE>
     ** (or subclass thereof) and the bean implements addVetoableChangeListener()
     ** and removeVetoableChangeListener(), then setConstrained(true) may safely
     ** be called.  Otherwise, the system behavior is undefined.
     ** <B>Spec note:</B> given those strict parameters, it would be nice if it
     ** got set automatically by detection, but oh well.<P>
     ** When a property is constrained, its set method is required to:<P>
     ** <OL>
     ** <LI>Fire the <CODE>VetoableChangeListener.vetoableChange()</CODE>
     **     event notifying others of the change and allowing them a chance to
     **     say it is a bad thing.</LI>
     ** <LI>If any of the listeners throws a PropertyVetoException, then
     **     it must fire another vetoableChange() event notifying the others
     **     of a reversion to the old value (though, of course, the change
     **     was never made).  Then it rethrows the PropertyVetoException and
     **     exits.</LI>
     ** <LI>If all has gone well to this point, the value may be changed.</LI>
     ** </OL>
     ** @param constrained whether the property is constrained or not.
     **/
    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }

    /** Get the PropertyEditor class.  Defaults to null. **/
    public Class getPropertyEditorClass() {
        return propertyEditorClass;
    }

    /** Set the PropertyEditor class.  If the class does not implement
     ** the PropertyEditor interface, you will likely get an exception
     ** late in the game.
     ** @param propertyEditorClass the PropertyEditor class for this 
     **        class to use.
     **/
    public void setPropertyEditorClass(Class propertyEditorClass) {
        this.propertyEditorClass = propertyEditorClass;
    }

    private void findMethods(Class beanClass, String getMethodName1, String getMethodName2, String setMethodName) throws IntrospectionException {
        try {
            if (getMethodName1 != null) {
                try {
                    getMethod = beanClass.getMethod(getMethodName1, new Class[0]);
                } catch (NoSuchMethodException e) {
                }
            }
            if (getMethod == null && getMethodName2 != null) {
                try {
                    getMethod = beanClass.getMethod(getMethodName2, new Class[0]);
                } catch (NoSuchMethodException e) {
                }
            }
            if (setMethodName != null) {
                if (getMethod != null) {
                    Class propertyType = getMethod.getReturnType();
                    if (propertyType == Void.TYPE) {
                        String msg = "The property's read method has return type 'void'";
                        throw new IntrospectionException(msg);
                    }
                    Class[] setArgs = new Class[] { propertyType };
                    try {
                        setMethod = beanClass.getMethod(setMethodName, setArgs);
                    } catch (NoSuchMethodException e) {
                    }
                } else if (getMethodName1 == null && getMethodName2 == null) {
                    Method[] methods = beanClass.getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        if (methods[i].getName().equals(setMethodName) && methods[i].getParameterTypes().length == 1 && methods[i].getReturnType() == Void.TYPE) {
                            setMethod = methods[i];
                            break;
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            String msg = "SecurityException thrown on attempt to access methods.";
            throw new IntrospectionException(msg);
        }
    }

    /** Checks whether the given <code>Method</code> instances are legal read and
     * write methods. The following requirements must be met:<br/>
     * <ul>
     * <li>the read method must not have an argument</li>
     * <li>the read method must have a non void return type</li>
     * <li>the read method may not exist</li>
     * <li>the write method must have a single argument</li>
     * <li>the property type and the read method's return type must be assignable from the
     * write method's argument type</li>
     * <li>the write method may not exist</li>
     * </ul>
     * While checking the methods a common new property type is calculated. If the method
     * succeeds this property type is returned.<br/>
     * <br/>
     * For compatibility this has to be noted:<br/>
     * The two methods are allowed to be defined in two distinct classes and may both be null.
     * 
     * @param readMethod The new read method to check.
     * @param writeMethod The new write method to check.
     * @return The common property type of the two method.
     * @throws IntrospectionException If any of the above requirements are not met.
     */
    private Class checkMethods(Method readMethod, Method writeMethod) throws IntrospectionException {
        Class newPropertyType = propertyType;
        if (readMethod != null) {
            if (readMethod.getParameterTypes().length > 0) {
                throw new IntrospectionException("read method has unexpected parameters");
            }
            newPropertyType = readMethod.getReturnType();
            if (newPropertyType == Void.TYPE) {
                throw new IntrospectionException("read method return type is void");
            }
        }
        if (writeMethod != null) {
            if (writeMethod.getParameterTypes().length != 1) {
                String msg = "write method does not have exactly one parameter";
                throw new IntrospectionException(msg);
            }
            if (readMethod == null) {
                newPropertyType = writeMethod.getParameterTypes()[0];
            } else {
                if (newPropertyType != null && !newPropertyType.isAssignableFrom(writeMethod.getParameterTypes()[0])) {
                    throw new IntrospectionException("read and write method are not compatible");
                }
            }
        }
        return newPropertyType;
    }

    /**
     * Return a hash code for this object, conforming to the contract described
     * in {@link Object#hashCode()}.
     * @return the hash code
     * @since 1.5
     */
    public int hashCode() {
        return ((propertyType == null ? 0 : propertyType.hashCode()) | (propertyEditorClass == null ? 0 : propertyEditorClass.hashCode()) | (bound ? Boolean.TRUE : Boolean.FALSE).hashCode() | (constrained ? Boolean.TRUE : Boolean.FALSE).hashCode() | (getMethod == null ? 0 : getMethod.hashCode()) | (setMethod == null ? 0 : setMethod.hashCode()));
    }

    /** Compares this <code>PropertyDescriptor</code> against the
     * given object.
     * Two PropertyDescriptors are equals if
     * <ul>
     * <li>the read methods are equal</li>
     * <li>the write methods are equal</li>
     * <li>the property types are equals</li>
     * <li>the property editor classes are equal</li>
     * <li>the flags (constrained and bound) are equal</li>
     * </ul>
     * @return Whether both objects are equal according to the rules given above.
     * @since 1.4
    */
    public boolean equals(Object o) {
        if (o instanceof PropertyDescriptor) {
            PropertyDescriptor that = (PropertyDescriptor) o;
            boolean samePropertyType = (propertyType == null) ? that.propertyType == null : propertyType.equals(that.propertyType);
            boolean samePropertyEditorClass = (propertyEditorClass == null) ? that.propertyEditorClass == null : propertyEditorClass.equals(that.propertyEditorClass);
            boolean sameFlags = bound == that.bound && constrained == that.constrained;
            boolean sameReadMethod = (getMethod == null) ? that.getMethod == null : getMethod.equals(that.getMethod);
            boolean sameWriteMethod = (setMethod == null) ? that.setMethod == null : setMethod.equals(that.setMethod);
            return samePropertyType && sameFlags && sameReadMethod && sameWriteMethod && samePropertyEditorClass;
        } else {
            return false;
        }
    }
}
