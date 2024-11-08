package awilkins.util;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utilities
 */
public class Util {

    /**
   * Creates new Util private because this is a static function library
   */
    private Util() {
    }

    /**
   * Convience to save checking nulls before using {@link Object#equals(Object)}method. Semantically equal to
   * <code>a.equals(b)</code> once you know that both <code>a</code> and <code>b</code> are not null.
   * 
   * @param a
   *          Object to be tested for equality with <code>b</code>
   * @param b
   *          Object to be tested for equality with <code>a</code>
   * @return true if both objects are <code>null</code>, or they are both not <code>null</code> and
   *         <code>a.equals(b)</code>
   */
    public static boolean equals(Object a, Object b) {
        boolean aNull = (a == null);
        boolean bNull = (b == null);
        if (aNull && bNull) {
            return true;
        }
        if (aNull != bNull) {
            return false;
        }
        return a.equals(b);
    }

    /**
   * For each object in the <code>newObjects</code> add it to the <code>existingObjects</code> collection, but only
   * if it is not already present.
   * 
   * @param existingObjects
   * @param newObjects
   */
    public static void addOnlyNewObjects(Collection existingObjects, Collection newObjects) {
        if (newObjects == null || existingObjects == null) {
            return;
        }
        newObjects = new ArrayList(newObjects);
        newObjects.removeAll(existingObjects);
        existingObjects.addAll(newObjects);
    }

    /**
   * Does the <code>testingCollection</code> contain only objects present in <code>referenceCollection</code>? May
   * still return true if <code>referenceCollection</code> contains objects not present in
   * <code>testingCollection</code>.
   * 
   * @return true if all the objects in <code>testingCollection</code> are also present in
   *         <code>referenceCollection</code>?
   */
    public static boolean containsOnly(Collection testingCollection, Collection referenceCollection) {
        testingCollection = new ArrayList(testingCollection);
        testingCollection.removeAll(referenceCollection);
        return testingCollection.isEmpty();
    }

    /**
   * Examine the named system property, and return a boolean interpretation of it's value, or <code>defaultValue</code>
   * if it is not present.
   * 
   * @return boolean interpretation of a System Property
   */
    public static final boolean getSystemPropertyAsBoolean(String propertyName, boolean defaultValue) {
        boolean value = defaultValue;
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            value = Boolean.valueOf(propertyValue).booleanValue();
        }
        return value;
    }

    /**
   * Examine the named system property (prefixed by propertyClass.getName() and a dot, and return a boolean
   * interpretation of it's value, or <code>defaultValue</code> if it is not present.
   * 
   * @return boolean interpretation of a System Property
   */
    public static final boolean getSystemPropertyAsBoolean(Class propertyClass, String propertyName, boolean defaultValue) {
        boolean value = defaultValue;
        String propertyValue = System.getProperty(propertyClass.getName() + "." + propertyName);
        if (propertyValue != null) {
            value = Boolean.valueOf(propertyValue).booleanValue();
        }
        return value;
    }

    /**
   * Set the System Properties from the <code>newProperties</code>. This differs from
   * {@link java.lang.System#setProperties(java.util.Properties) System.setProperties(Properties)}in that this will
   * only set properties that exist in <code>newProperties</code> leaving other existing values unaffected.
   * 
   * @param newProperties
   *          new values for some system properties
   */
    public static final void addSystemProperties(Properties newProperties) {
        try {
            Enumeration propertyNames = newProperties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                String value = newProperties.getProperty(name);
                System.setProperty(name, value);
            }
        } catch (AccessControlException e) {
            final Class thisClass = Util.class;
            System.err.println("AccessControlException attempting to set System Properties.  Ensure access for this class ('" + thisClass.getName() + "') allows System Property writes");
            System.err.println("Need to grant permission to this class: \n\tpermission java.util.PropertyPermission \"*\", \"read,write\";");
            throw e;
        }
    }
}
