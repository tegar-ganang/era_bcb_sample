package org.simpleframework.xml.load;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The <code>PropertyScanner</code> object is used to scan an object 
 * for matching get and set methods for an XML annotation. This will
 * scan for annotated methods starting with the most specialized
 * class up the class hierarchy. Thus, annotated methods can be 
 * overridden in a type specialization.
 * <p>
 * The annotated methods must be either a getter or setter method
 * following the Java Beans naming conventions. This convention is
 * such that a method must begin with "get", "set", or "is". A pair
 * of set and get methods for an annotation must make use of the
 * same type. For instance if the return type for the get method
 * was <code>String</code> then the set method must have a single
 * argument parameter that takes a <code>String</code> type.
 * <p>
 * For a method to be considered there must be both the get and set
 * methods. If either method is missing then the scanner fails with
 * an exception. Also, if an annotation marks a method which does
 * not follow Java Bean naming conventions an exception is thrown.
 *    
 * @author Niall Gallagher
 */
class PropertyProvider extends PropertyMap {

    /**
    * This is the factory used to produce method parts for a property.
    */
    private MethodPartFactory factory;

    /**
    * This is the hierarchy of class types within the scanned class.
    */
    private Hierarchy hierarchy;

    /**
    * This is used to collect all the set methods from the object.
    */
    private PartMap write;

    /**
    * This is used to collect all the get methods from the object.
    */
    private PartMap read;

    /**
    * This is the type of the object that is being scanned.
    */
    private Class type;

    /**
    * Constructor for the <code>PropertyScanner</code> object. This is
    * used to create an object that will scan the specified class
    * such that all bean property methods can be paired under the
    * XML annotation specified within the class.
    * 
    * @param type this is the type that is to be scanned for methods
    * 
    * @throws Exception thrown if there was a problem scanning
    */
    public PropertyProvider(Class type) throws Exception {
        this.factory = new MethodPartFactory();
        this.hierarchy = new Hierarchy(type);
        this.write = new PartMap();
        this.read = new PartMap();
        this.type = type;
        this.scan(type);
    }

    /**
    * This method is used to scan the class hierarchy for each class
    * in order to extract methods that contain XML annotations. If
    * a method is annotated it is converted to a contact so that
    * it can be used during serialization and deserialization.
    * 
    * @param type this is the type to be scanned for methods
    * 
    * @throws Exception thrown if the object schema is invalid
    */
    private void scan(Class type) throws Exception {
        for (Class next : hierarchy) {
            scan(type, next);
        }
        build();
    }

    /**
    * This is used to scan the declared methods within the specified
    * class. Each method will be checked to determine if it contains
    * an XML element and can be used as a <code>Contact</code> for
    * an entity within the object.
    * 
    * @param real this is the actual type of the object scanned
    * @param type this is one of the super classes for the object
    * 
    * @throws Exception thrown if the class schema is invalid
    */
    private void scan(Class real, Class type) throws Exception {
        Method[] method = type.getDeclaredMethods();
        for (int i = 0; i < method.length; i++) {
            scan(method[i]);
        }
    }

    /**
    * This is used to classify the specified method into either a get
    * or set method. If the method is neither then an exception is
    * thrown to indicate that the XML annotations can only be used
    * with methods following the Java Bean naming conventions. Once
    * the method is classified is is added to either the read or 
    * write map so that it can be paired after scanning is complete.
    * 
    * @param method this is the method that is to be classified
    * @param label this is the annotation applied to the method
    */
    private void scan(Method method) throws Exception {
        MethodPart part = factory.getInstance(method);
        if (part != null) {
            scan(part);
        }
    }

    /**
    * This is used to classify the specified method into either a get
    * or set method. If the method is neither then an exception is
    * thrown to indicate that the XML annotations can only be used
    * with methods following the Java Bean naming conventions. Once
    * the method is classified is is added to either the read or 
    * write map so that it can be paired after scanning is complete.
    * 
    * @param method this is the method that is to be classified
    * @param label this is the annotation applied to the method
    */
    private void scan(MethodPart part) {
        MethodType type = part.getMethodType();
        if (type == MethodType.GET) {
            process(part, read);
        }
        if (type == MethodType.IS) {
            process(part, read);
        }
        if (type == MethodType.SET) {
            process(part, write);
        }
    }

    /**
    * This is used to determine whether the specified method can be
    * inserted into the given <code>PartMap</code>. This ensures 
    * that only the most specialized method is considered, which 
    * enables annotated methods to be overridden in subclasses.
    * 
    * @param method this is the method part that is to be inserted
    * @param map this is the part map used to contain the method
    */
    private void process(MethodPart method, PartMap map) {
        String name = method.getName();
        if (!map.containsKey(name)) {
            map.put(name, method);
        }
    }

    /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    *  
    * @throws Exception thrown if there is a problem matching methods
    */
    private void build() throws Exception {
        for (String name : read) {
            MethodPart part = read.get(name);
            if (part != null) {
                build(part, name);
            }
        }
    }

    /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    * 
    * @param read this is a get method that has been extracted
    * @param name this is the Java Bean methos name to be matched   
    *  
    * @throws Exception thrown if there is a problem matching methods
    */
    private void build(MethodPart read, String name) throws Exception {
        MethodPart match = write.take(name);
        if (match != null) {
            build(read, match);
        }
    }

    /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    * 
    * @param read this is a get method that has been extracted
    * @param write this is the write method to compare details with      
    *  
    * @throws Exception thrown if there is a problem matching methods
    */
    private void build(MethodPart read, MethodPart write) throws Exception {
        String name = read.getName();
        Class type = read.getType();
        if (type == write.getType()) {
            put(name, new Property(read, write));
        }
    }

    /**
    * The <code>PartMap</code> is used to contain method parts using
    * the Java Bean method name for the part. This ensures that the
    * scanned and extracted methods can be acquired using a common 
    * name, which should be the parsed Java Bean method name.
    * 
    * @see org.simpleframework.xml.load.MethodPart
    */
    private class PartMap extends LinkedHashMap<String, MethodPart> implements Iterable<String> {

        /**
       * This returns an iterator for the Java Bean method names for
       * the <code>MethodPart</code> objects that are stored in the
       * map. This allows names to be iterated easily in a for loop.
       * 
       * @return this returns an iterator for the method name keys
       */
        public Iterator<String> iterator() {
            return keySet().iterator();
        }

        /**
       * This is used to acquire the method part for the specified
       * method name. This will remove the method part from this map
       * so that it can be checked later to ensure what remains.
       * 
       * @param name this is the method name to get the method with       
       * 
       * @return this returns the method part for the given key
       */
        public MethodPart take(String name) {
            return remove(name);
        }
    }
}
