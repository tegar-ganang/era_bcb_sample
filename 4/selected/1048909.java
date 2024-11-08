package simple.xml.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * The <code>MethodContact</code> object is acts as a contact that
 * can set and get data to and from an object using methods. This 
 * requires a get method and a set method that share the same class
 * type for the return and parameter respectively.
 *
 * @author Niall Gallagher
 *
 * @see simple.xml.load.MethodScanner
 */
final class MethodContact implements Contact {

    /**
    * This is the label that marks both the set and get methods.
    */
    private Annotation label;

    /**
    * This is the read method which is used to get the value.
    */
    private Method read;

    /**
    * This is the write method which is used to set the value.
    */
    private Method write;

    /**
    * This is the type associated with this point of contact.
    */
    private Class type;

    /**
    * Constructor for the <code>MethodContact</code> object. This is
    * used to compose a point of contact that makes use of a get and
    * set method on a class. The specified methods will be invoked
    * during the serialization process to read and write values.
    *
    * @param read this forms the get method for the object
    * @param write this forms the get method for the object
    */
    public MethodContact(MethodPart read, MethodPart write) {
        this.label = read.getAnnotation();
        this.write = write.getMethod();
        this.read = read.getMethod();
        this.type = read.getType();
    }

    /**
    * This will provide the contact type. The contact type is the
    * class that is to be set and get on the object. This represents
    * the return type for the get and the parameter for the set.
    *
    * @return this returns the type that this contact represents
    */
    public Class getType() {
        return type;
    }

    /**
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serializaed and deserialized from the object.
    *
    * @return this provides the annotation associated with this
    */
    public Annotation getAnnotation() {
        return label;
    }

    /**
    * This is used to set the specified value on the provided object.
    * The value provided must be an instance of the contact class so
    * that it can be set without a runtime class compatibility error.
    *
    * @param source this is the object to set the value on
    * @param value this is the value that is to be set on the object
    */
    public void set(Object source, Object value) throws Exception {
        write.invoke(source, value);
    }

    /**
    * This is used to get the specified value on the provided object.
    * The value returned from this method will be an instance of the
    * contact class type. If the returned object is of a different
    * type then the serialization process will fail.
    *
    * @param source this is the object to acquire the value from
    *
    * @return this is the value that is acquired from the object
    */
    public Object get(Object source) throws Exception {
        return read.invoke(source);
    }
}