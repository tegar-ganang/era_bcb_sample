package java.io;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import sun.misc.SoftCache;

/**
 * Serialization's descriptor for classes.
 * It contains the name and serialVersionUID of the class.
 * <br>
 * The ObjectStreamClass for a specific class loaded in this Java VM can
 * be found/created using the lookup method.<p>
 * The algorithm to compute the SerialVersionUID is described in 
 * <a href="../../../guide/serialization/spec/class.doc4.html"> Object Serialization Specification, Section 4.4, Stream Unique Identifiers</a>.
 *
 * @author	Mike Warres
 * @author	Roger Riggs
 * @version 1.98 02/02/00
 * @see ObjectStreamField
 * @see <a href="../../../guide/serialization/spec/class.doc.html"> Object Serialization Specification, Section 4, Class Descriptors</a>
 * @since   JDK1.1
 */
public class ObjectStreamClass implements Serializable {

    /** serialPersistentFields value indicating no serializable fields */
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];

    private static final long serialVersionUID = -6120832682080437368L;

    private static final ObjectStreamField[] serialPersistentFields = NO_FIELDS;

    /** cache mapping local classes -> descriptors */
    private static final SoftCache localDescs = new SoftCache(10);

    /** cache mapping field group/local desc pairs -> field reflectors */
    private static final SoftCache reflectors = new SoftCache(10);

    /** class associated with this descriptor (if any) */
    private Class cl;

    /** name of class represented by this descriptor */
    private String name;

    /** serialVersionUID of represented class (null if not computed yet) */
    private volatile Long suid;

    /** true if represents dynamic proxy class */
    private boolean isProxy;

    /** true if represented class implements Serializable */
    private boolean serializable;

    /** true if represented class implements Externalizable */
    private boolean externalizable;

    /** true if desc has data written by class-defined writeObject method */
    private boolean hasWriteObjectData;

    /** 
     * true if desc has externalizable data written in block data format; this
     * must be true by default to accomodate ObjectInputStream subclasses which
     * override readClassDescriptor() to return class descriptors obtained from
     * ObjectStreamClass.lookup() (see 4461737)
     */
    private boolean hasBlockExternalData = true;

    /** exception (if any) thrown while attempting to resolve class */
    private ClassNotFoundException resolveEx;

    /** exception (if any) to be thrown if deserialization attempted */
    private InvalidClassException deserializeEx;

    /** exception (if any) to be thrown if default serialization attempted */
    private InvalidClassException defaultSerializeEx;

    /** serializable fields */
    private ObjectStreamField[] fields;

    /** aggregate marshalled size of primitive fields */
    private int primDataSize;

    /** number of non-primitive fields */
    private int numObjFields;

    /** reflector for setting/getting serializable field values */
    private FieldReflector fieldRefl;

    /** data layout of serialized objects described by this class desc */
    private volatile ClassDataSlot[] dataLayout;

    /** class-defined writeObject method, or null if none */
    private Method writeObjectMethod;

    /** class-defined readObject method, or null if none */
    private Method readObjectMethod;

    /** class-defined readObjectNoData method, or null if none */
    private Method readObjectNoDataMethod;

    /** class-defined writeReplace method, or null if none */
    private Method writeReplaceMethod;

    /** class-defined readResolve method, or null if none */
    private Method readResolveMethod;

    /** local class descriptor for represented class (may point to self) */
    private ObjectStreamClass localDesc;

    /** superclass descriptor appearing in stream */
    private ObjectStreamClass superDesc;

    /** Class to be initialized -- for back-port to J2ME Foundation */
    private Class initClass;

    /**
     * Initializes native code.
     */
    private static native void initNative();

    static {
        initNative();
    }

    private static native void getFieldIDs(ObjectStreamField[] fields, long[] primFieldIDs, long[] objFieldIDs);

    /** 
     * Find the descriptor for a class that can be serialized. 
     * Creates an ObjectStreamClass instance if one does not exist 
     * yet for class. Null is returned if the specified class does not 
     * implement java.io.Serializable or java.io.Externalizable.
     *
     * @param cl class for which to get the descriptor
     * @return the class descriptor for the specified class
     */
    public static ObjectStreamClass lookup(Class cl) {
        return lookup(cl, false);
    }

    /**
     * The name of the class described by this descriptor.
     *
     * @return a <code>String</code> representing the fully qualified name of
     * the class
     */
    public String getName() {
        return name;
    }

    /**
     * Return the serialVersionUID for this class.
     * The serialVersionUID defines a set of classes all with the same name
     * that have evolved from a common root class and agree to be serialized
     * and deserialized using a common format.
     * NonSerializable classes have a serialVersionUID of 0L.
     *
     * @return the SUID of the class described by this descriptor
     */
    public long getSerialVersionUID() {
        if (suid == null) {
            suid = (Long) AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    return new Long(computeDefaultSUID(cl));
                }
            });
        }
        return suid.longValue();
    }

    /**
     * Return the class in the local VM that this version is mapped to.
     * Null is returned if there is no corresponding local class.
     *
     * @return the <code>Class</code> instance that this descriptor represents
     */
    public Class forClass() {
        return cl;
    }

    /**
     * Return an array of the fields of this serializable class.
     *
     * @return an array containing an element for each persistent
     * field of this class. Returns an array of length zero if
     * there are no fields.
     * @since 1.2
     */
    public ObjectStreamField[] getFields() {
        return getFields(true);
    }

    /**
     * Get the field of this class by name.
     *
     * @param name the name of the data field to look for
     * @return The ObjectStreamField object of the named field or null if there
     * is no such named field.
     */
    public ObjectStreamField getField(String name) {
        return getField(name, null);
    }

    /**
     * Return a string describing this ObjectStreamClass.
     */
    public String toString() {
        return name + ": static final long serialVersionUID = " + getSerialVersionUID() + "L;";
    }

    /**
     * Looks up and returns class descriptor for given class, or null if class
     * is non-serializable and "all" is set to false.
     * 
     * @param	cl class to look up
     * @param	all if true, return descriptors for all classes; if false, only
     * 		return descriptors for serializable classes
     */
    static ObjectStreamClass lookup(Class cl, boolean all) {
        if (!(all || Serializable.class.isAssignableFrom(cl))) {
            return null;
        }
        Object entry;
        EntryFuture future = null;
        synchronized (localDescs) {
            if ((entry = localDescs.get(cl)) == null) {
                localDescs.put(cl, future = new EntryFuture());
            }
        }
        if (entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        } else if (entry instanceof EntryFuture) {
            entry = ((EntryFuture) entry).get();
        } else if (entry == null) {
            try {
                entry = new ObjectStreamClass(cl);
            } catch (Throwable th) {
                entry = th;
            }
            future.set(entry);
            synchronized (localDescs) {
                localDescs.put(cl, entry);
            }
        }
        if (entry instanceof ObjectStreamClass) {
            return (ObjectStreamClass) entry;
        } else if (entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if (entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }

    /**
     * Placeholder used in class descriptor and field reflector lookup tables
     * for an entry in the process of being initialized.  (Internal) callers
     * which receive an EntryFuture as the result of a lookup should call the
     * get() method of the EntryFuture; this will return the actual entry once
     * it is ready for use and has been set().  To conserve objects,
     * EntryFutures synchronize on themselves.
     */
    private static class EntryFuture {

        private static final Object unset = new Object();

        private Object entry = unset;

        synchronized void set(Object entry) {
            if (this.entry != unset) {
                throw new IllegalStateException();
            }
            this.entry = entry;
            notifyAll();
        }

        synchronized Object get() {
            boolean interrupted = false;
            while (entry == unset) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                AccessController.doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
            }
            return entry;
        }
    }

    /**
     * Creates local class descriptor representing given class.
     */
    private ObjectStreamClass(final Class cl) {
        this.cl = cl;
        name = cl.getName();
        isProxy = Proxy.isProxyClass(cl);
        serializable = Serializable.class.isAssignableFrom(cl);
        externalizable = Externalizable.class.isAssignableFrom(cl);
        Class superCl = cl.getSuperclass();
        superDesc = (superCl != null) ? lookup(superCl, false) : null;
        localDesc = this;
        if (serializable) {
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    suid = getDeclaredSUID(cl);
                    fields = getSerialFields(cl);
                    computeFieldOffsets();
                    if (externalizable) {
                        initClass = cl;
                    } else {
                        initClass = getSerializableInitClass(cl);
                        writeObjectMethod = getPrivateMethod(cl, "writeObject", new Class[] { ObjectOutputStream.class }, Void.TYPE);
                        readObjectMethod = getPrivateMethod(cl, "readObject", new Class[] { ObjectInputStream.class }, Void.TYPE);
                        readObjectNoDataMethod = getPrivateMethod(cl, "readObjectNoData", new Class[0], Void.TYPE);
                        hasWriteObjectData = (writeObjectMethod != null);
                    }
                    writeReplaceMethod = getInheritableMethod(cl, "writeReplace", new Class[0], Object.class);
                    readResolveMethod = getInheritableMethod(cl, "readResolve", new Class[0], Object.class);
                    return null;
                }
            });
        } else {
            suid = new Long(0);
            fields = NO_FIELDS;
        }
        try {
            fieldRefl = getReflector(fields, this);
        } catch (InvalidClassException ex) {
            throw new InternalError();
        }
        if (initClass == null) {
            deserializeEx = new InvalidClassException(name, "no valid constructor");
        }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getField() == null) {
                defaultSerializeEx = new InvalidClassException(name, "unmatched serializable field(s) declared");
            }
        }
    }

    /**
     * Creates blank class descriptor which should be initialized via a
     * subsequent call to initProxy(), initNonProxy() or readNonProxy().
     */
    ObjectStreamClass() {
    }

    /**
     * Initializes class descriptor representing a proxy class.
     */
    void initProxy(Class cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        isProxy = true;
        serializable = true;
        suid = new Long(0);
        fields = NO_FIELDS;
        if (cl != null) {
            localDesc = lookup(cl, true);
            if (!localDesc.isProxy) {
                throw new InvalidClassException("cannot bind proxy descriptor to a non-proxy class");
            }
            name = localDesc.name;
            externalizable = localDesc.externalizable;
            initClass = localDesc.initClass;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            deserializeEx = localDesc.deserializeEx;
        }
        fieldRefl = getReflector(fields, localDesc);
    }

    /**
     * Initializes class descriptor representing a non-proxy class.
     */
    void initNonProxy(ObjectStreamClass model, Class cl, ClassNotFoundException resolveEx, ObjectStreamClass superDesc) throws InvalidClassException {
        this.cl = cl;
        this.resolveEx = resolveEx;
        this.superDesc = superDesc;
        name = model.name;
        suid = new Long(model.getSerialVersionUID());
        isProxy = false;
        serializable = model.serializable;
        externalizable = model.externalizable;
        hasBlockExternalData = model.hasBlockExternalData;
        hasWriteObjectData = model.hasWriteObjectData;
        fields = model.fields;
        primDataSize = model.primDataSize;
        numObjFields = model.numObjFields;
        if (cl != null) {
            localDesc = lookup(cl, true);
            if (localDesc.isProxy) {
                throw new InvalidClassException("cannot bind non-proxy descriptor to a proxy class");
            }
            if (serializable == localDesc.serializable && !cl.isArray() && suid.longValue() != localDesc.getSerialVersionUID()) {
                throw new InvalidClassException(localDesc.name, "local class incompatible: " + "stream classdesc serialVersionUID = " + suid + ", local class serialVersionUID = " + localDesc.getSerialVersionUID());
            }
            if (!classNamesEqual(name, localDesc.name)) {
                throw new InvalidClassException(localDesc.name, "local class name incompatible with stream class " + "name \"" + name + "\"");
            }
            if ((serializable == localDesc.serializable) && (externalizable != localDesc.externalizable)) {
                throw new InvalidClassException(localDesc.name, "Serializable incompatible with Externalizable");
            }
            if ((serializable != localDesc.serializable) || (externalizable != localDesc.externalizable) || !(serializable || externalizable)) {
                deserializeEx = new InvalidClassException(localDesc.name, "class invalid for deserialization");
            }
            initClass = localDesc.initClass;
            writeObjectMethod = localDesc.writeObjectMethod;
            readObjectMethod = localDesc.readObjectMethod;
            readObjectNoDataMethod = localDesc.readObjectNoDataMethod;
            writeReplaceMethod = localDesc.writeReplaceMethod;
            readResolveMethod = localDesc.readResolveMethod;
            if (deserializeEx == null) {
                deserializeEx = localDesc.deserializeEx;
            }
        }
        fieldRefl = getReflector(fields, localDesc);
        fields = fieldRefl.getFields();
    }

    /**
     * Reads non-proxy class descriptor information from given input stream.
     * The resulting class descriptor is not fully functional; it can only be
     * used as input to the ObjectInputStream.resolveClass() and
     * ObjectStreamClass.initNonProxy() methods.
     */
    void readNonProxy(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        suid = new Long(in.readLong());
        isProxy = false;
        byte flags = in.readByte();
        externalizable = ((flags & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0);
        serializable = (externalizable || ((flags & ObjectStreamConstants.SC_SERIALIZABLE) != 0));
        hasWriteObjectData = ((flags & ObjectStreamConstants.SC_WRITE_METHOD) != 0);
        hasBlockExternalData = ((flags & ObjectStreamConstants.SC_BLOCK_DATA) != 0);
        int numFields = in.readShort();
        fields = (numFields > 0) ? new ObjectStreamField[numFields] : NO_FIELDS;
        for (int i = 0; i < numFields; i++) {
            char tcode = (char) in.readByte();
            String name = in.readUTF();
            String signature = ((tcode == 'L') || (tcode == '[')) ? in.readTypeString() : new String(new char[] { tcode });
            fields[i] = new ObjectStreamField(name, signature, false);
        }
        computeFieldOffsets();
    }

    /**
     * Writes non-proxy class descriptor information to given output stream.
     */
    void writeNonProxy(ObjectOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeLong(getSerialVersionUID());
        byte flags = 0;
        if (externalizable) {
            flags |= ObjectStreamConstants.SC_EXTERNALIZABLE;
            int protocol = out.getProtocolVersion();
            if (protocol != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                flags |= ObjectStreamConstants.SC_BLOCK_DATA;
            }
        } else if (serializable) {
            flags |= ObjectStreamConstants.SC_SERIALIZABLE;
        }
        if (hasWriteObjectData) {
            flags |= ObjectStreamConstants.SC_WRITE_METHOD;
        }
        out.writeByte(flags);
        out.writeShort(fields.length);
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            out.writeByte(f.getTypeCode());
            out.writeUTF(f.getName());
            if (!f.isPrimitive()) {
                out.writeTypeString(f.getTypeString());
            }
        }
    }

    /**
     * Returns ClassNotFoundException (if any) thrown while attempting to
     * resolve local class corresponding to this class descriptor.
     */
    ClassNotFoundException getResolveException() {
        return resolveEx;
    }

    /**
     * Throws an InvalidClassException if object instances referencing this
     * class descriptor should not be allowed to deserialize.
     */
    void checkDeserialize() throws InvalidClassException {
        if (deserializeEx != null) {
            throw deserializeEx;
        }
    }

    /**
     * Throws an InvalidClassException if objects whose class is represented by
     * this descriptor should not be permitted to use default serialization
     * (e.g., if the class declares serializable fields that do not correspond
     * to actual fields, and hence must use the GetField API).
     */
    void checkDefaultSerialize() throws InvalidClassException {
        if (defaultSerializeEx != null) {
            throw defaultSerializeEx;
        }
    }

    /**
     * Returns superclass descriptor.  Note that on the receiving side, the
     * superclass descriptor may be bound to a class that is not a superclass
     * of the subclass descriptor's bound class.
     */
    ObjectStreamClass getSuperDesc() {
        return superDesc;
    }

    /**
     * Returns the "local" class descriptor for the class associated with this
     * class descriptor (i.e., the result of
     * ObjectStreamClass.lookup(this.forClass())) or null if there is no class
     * associated with this descriptor.
     */
    ObjectStreamClass getLocalDesc() {
        return localDesc;
    }

    /**
     * Returns arrays of ObjectStreamFields representing the serializable
     * fields of the represented class.  If copy is true, a clone of this class
     * descriptor's field array is returned, otherwise the array itself is
     * returned.
     */
    ObjectStreamField[] getFields(boolean copy) {
        return copy ? (ObjectStreamField[]) fields.clone() : fields;
    }

    /**
     * Looks up a serializable field of the represented class by name and type.
     * A specified type of null matches all types, Object.class matches all
     * non-primitive types, and any other non-null type matches assignable
     * types only.  Returns matching field, or null if no match found.
     */
    ObjectStreamField getField(String name, Class type) {
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            if (f.getName().equals(name)) {
                if (type == null) {
                    return f;
                } else if (type == Object.class) {
                    if (!f.isPrimitive()) {
                        return f;
                    }
                } else {
                    Class ftype = f.getType();
                    if (ftype != null && type.isAssignableFrom(ftype)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns true if class descriptor represents a dynamic proxy class, false
     * otherwise.
     */
    boolean isProxy() {
        return isProxy;
    }

    /**
     * Returns true if represented class implements Externalizable, false
     * otherwise.
     */
    boolean isExternalizable() {
        return externalizable;
    }

    /**
     * Returns true if represented class implements Serializable, false
     * otherwise.
     */
    boolean isSerializable() {
        return serializable;
    }

    /**
     * Returns true if class descriptor represents externalizable class that
     * has written its data in 1.2 (block data) format, false otherwise.
     */
    boolean hasBlockExternalData() {
        return hasBlockExternalData;
    }

    /**
     * Returns true if class descriptor represents serializable (but not
     * externalizable) class which has written its data via a custom
     * writeObject() method, false otherwise.
     */
    boolean hasWriteObjectData() {
        return hasWriteObjectData;
    }

    /**
     * Returns true if represented class is serializable/externalizable and can
     * be instantiated by the serialization runtime--i.e., if it is
     * externalizable and defines a public no-arg constructor, or if it is
     * non-externalizable and its first non-serializable superclass defines an
     * accessible no-arg constructor.  Otherwise, returns false.
     */
    boolean isInstantiable() {
        return (initClass != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant writeObject method.  Otherwise,
     * returns false.
     */
    boolean hasWriteObjectMethod() {
        return (writeObjectMethod != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObject method.  Otherwise,
     * returns false.
     */
    boolean hasReadObjectMethod() {
        return (readObjectMethod != null);
    }

    /**
     * Returns true if represented class is serializable (but not
     * externalizable) and defines a conformant readObjectNoData method.
     * Otherwise, returns false.
     */
    boolean hasReadObjectNoDataMethod() {
        return (readObjectNoDataMethod != null);
    }

    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant writeReplace method.  Otherwise, returns false.
     */
    boolean hasWriteReplaceMethod() {
        return (writeReplaceMethod != null);
    }

    /**
     * Returns true if represented class is serializable or externalizable and
     * defines a conformant readResolve method.  Otherwise, returns false.
     */
    boolean hasReadResolveMethod() {
        return (readResolveMethod != null);
    }

    /**
     * Creates a new instance of the represented class.  If the class is
     * externalizable, invokes its public no-arg constructor; otherwise, if the
     * class is serializable, invokes the no-arg constructor of the first
     * non-serializable superclass.  Throws UnsupportedOperationException if
     * this class descriptor is not associated with a class, if the associated
     * class is non-serializable or if the appropriate no-arg constructor is
     * inaccessible/unavailable.
     */
    Object newInstance() throws InstantiationException, InvocationTargetException, UnsupportedOperationException, IllegalAccessException {
        if (initClass != null) {
            return ObjectInputStream.allocateNewObject(cl, initClass);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the writeObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define writeObject.
     */
    void invokeWriteObject(Object obj, ObjectOutputStream out) throws IOException, UnsupportedOperationException {
        if (writeObjectMethod != null) {
            try {
                writeObjectMethod.invoke(obj, new Object[] { out });
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readObject method of the represented serializable class.
     * Throws UnsupportedOperationException if this class descriptor is not
     * associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObject.
     */
    void invokeReadObject(Object obj, ObjectInputStream in) throws ClassNotFoundException, IOException, UnsupportedOperationException {
        if (readObjectMethod != null) {
            try {
                readObjectMethod.invoke(obj, new Object[] { in });
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) th;
                } else if (th instanceof IOException) {
                    throw (IOException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readObjectNoData method of the represented serializable
     * class.  Throws UnsupportedOperationException if this class descriptor is
     * not associated with a class, or if the class is externalizable,
     * non-serializable or does not define readObjectNoData.
     */
    void invokeReadObjectNoData(Object obj) throws IOException, UnsupportedOperationException {
        if (readObjectNoDataMethod != null) {
            try {
                readObjectNoDataMethod.invoke(obj, new Object[0]);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the writeReplace method of the represented serializable class and
     * returns the result.  Throws UnsupportedOperationException if this class
     * descriptor is not associated with a class, or if the class is
     * non-serializable or does not define writeReplace.
     */
    Object invokeWriteReplace(Object obj) throws IOException, UnsupportedOperationException {
        if (writeReplaceMethod != null) {
            try {
                return writeReplaceMethod.invoke(obj, new Object[0]);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError();
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Invokes the readResolve method of the represented serializable class and
     * returns the result.  Throws UnsupportedOperationException if this class
     * descriptor is not associated with a class, or if the class is
     * non-serializable or does not define readResolve.
     */
    Object invokeReadResolve(Object obj) throws IOException, UnsupportedOperationException {
        if (readResolveMethod != null) {
            try {
                return readResolveMethod.invoke(obj, new Object[0]);
            } catch (InvocationTargetException ex) {
                Throwable th = ex.getTargetException();
                if (th instanceof ObjectStreamException) {
                    throw (ObjectStreamException) th;
                } else {
                    throwMiscException(th);
                    throw new InternalError();
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Class representing the portion of an object's serialized form allotted
     * to data described by a given class descriptor.  If "hasDesc" is false,
     * the object's serialized form does not contain data associated with the
     * class descriptor.
     */
    static class ClassDataSlot {

        /** class descriptor "occupying" this slot */
        final ObjectStreamClass desc;

        /** true if serialized form includes data for this slot's descriptor */
        final boolean hasData;

        ClassDataSlot(ObjectStreamClass desc, boolean hasData) {
            this.desc = desc;
            this.hasData = hasData;
        }
    }

    /**
     * Returns array of ClassDataSlot instances representing the data layout
     * (including superclass data) for serialized objects described by this
     * class descriptor.  ClassDataSlots are ordered by inheritance with those
     * containing "higher" superclasses appearing first.  The final
     * ClassDataSlot contains a reference to this descriptor.
     */
    ClassDataSlot[] getClassDataLayout() throws InvalidClassException {
        if (dataLayout == null) {
            dataLayout = getClassDataLayout0();
        }
        return dataLayout;
    }

    private ClassDataSlot[] getClassDataLayout0() throws InvalidClassException {
        ArrayList slots = new ArrayList();
        Class start = cl, end = cl;
        while (end != null && Serializable.class.isAssignableFrom(end)) {
            end = end.getSuperclass();
        }
        for (ObjectStreamClass d = this; d != null; d = d.superDesc) {
            String searchName = (d.cl != null) ? d.cl.getName() : d.name;
            Class match = null;
            for (Class c = start; c != end; c = c.getSuperclass()) {
                if (searchName.equals(c.getName())) {
                    match = c;
                    break;
                }
            }
            if (match != null) {
                for (Class c = start; c != match; c = c.getSuperclass()) {
                    slots.add(new ClassDataSlot(ObjectStreamClass.lookup(c, true), false));
                }
                start = match.getSuperclass();
            }
            slots.add(new ClassDataSlot(d.getVariantFor(match), true));
        }
        for (Class c = start; c != end; c = c.getSuperclass()) {
            slots.add(new ClassDataSlot(ObjectStreamClass.lookup(c, true), false));
        }
        Collections.reverse(slots);
        return (ClassDataSlot[]) slots.toArray(new ClassDataSlot[slots.size()]);
    }

    /**
     * Returns aggregate size (in bytes) of marshalled primitive field values
     * for represented class.
     */
    int getPrimDataSize() {
        return primDataSize;
    }

    /**
     * Returns number of non-primitive serializable fields of represented
     * class.
     */
    int getNumObjFields() {
        return numObjFields;
    }

    /**
     * Fetches the serializable primitive field values of object obj and
     * marshals them into byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    void getPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.getPrimFieldValues(obj, buf);
    }

    /**
     * Sets the serializable primitive fields of object obj using values
     * unmarshalled from byte array buf starting at offset 0.  It is the
     * responsibility of the caller to ensure that obj is of the proper type if
     * non-null.
     */
    void setPrimFieldValues(Object obj, byte[] buf) {
        fieldRefl.setPrimFieldValues(obj, buf);
    }

    /**
     * Fetches the serializable object field values of object obj and stores
     * them in array vals starting at offset 0.  It is the responsibility of
     * the caller to ensure that obj is of the proper type if non-null.
     */
    void getObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.getObjFieldValues(obj, vals);
    }

    /**
     * Sets the serializable object fields of object obj using values from
     * array vals starting at offset 0.  It is the responsibility of the caller
     * to ensure that obj is of the proper type if non-null.
     */
    void setObjFieldValues(Object obj, Object[] vals) {
        fieldRefl.setObjFieldValues(obj, vals);
    }

    /**
     * Calculates and sets serializable field offsets, as well as primitive
     * data size and object field count totals.
     */
    private void computeFieldOffsets() {
        primDataSize = 0;
        numObjFields = 0;
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i];
            switch(f.getTypeCode()) {
                case 'Z':
                case 'B':
                    f.setOffset(primDataSize++);
                    break;
                case 'C':
                case 'S':
                    f.setOffset(primDataSize);
                    primDataSize += 2;
                    break;
                case 'I':
                case 'F':
                    f.setOffset(primDataSize);
                    primDataSize += 4;
                    break;
                case 'J':
                case 'D':
                    f.setOffset(primDataSize);
                    primDataSize += 8;
                    break;
                case '[':
                case 'L':
                    f.setOffset(numObjFields++);
                    break;
                default:
                    throw new InternalError();
            }
        }
    }

    /**
     * If given class is the same as the class associated with this class
     * descriptor, returns reference to this class descriptor.  Otherwise,
     * returns variant of this class descriptor bound to given class.
     */
    private ObjectStreamClass getVariantFor(Class cl) throws InvalidClassException {
        if (this.cl == cl) {
            return this;
        }
        ObjectStreamClass desc = new ObjectStreamClass();
        if (isProxy) {
            desc.initProxy(cl, null, superDesc);
        } else {
            desc.initNonProxy(this, cl, null, superDesc);
        }
        return desc;
    }

    /**
     * Returns first non-serializable superclass, or null if none is found.
     * 
     * Added for JDK 1.4 back-port
     */
    private static Class getSerializableInitClass(Class cl) {
        Class initCl = cl;
        while (Serializable.class.isAssignableFrom(initCl)) {
            if ((initCl = initCl.getSuperclass()) == null) {
                return null;
            }
        }
        return initCl;
    }

    /**
     * Returns non-static, non-abstract method with given signature provided it
     * is defined by or accessible (via inheritance) by the given class, or
     * null if no match found.  Access checks are disabled on the returned
     * method (if any).
     */
    private static Method getInheritableMethod(Class cl, String name, Class[] argTypes, Class returnType) {
        Method meth = null;
        Class defCl = cl;
        while (defCl != null) {
            try {
                meth = defCl.getDeclaredMethod(name, argTypes);
                break;
            } catch (NoSuchMethodException ex) {
                defCl = defCl.getSuperclass();
            }
        }
        if ((meth == null) || (meth.getReturnType() != returnType)) {
            return null;
        }
        meth.setAccessible(true);
        int mods = meth.getModifiers();
        if ((mods & (Modifier.STATIC | Modifier.ABSTRACT)) != 0) {
            return null;
        } else if ((mods & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0) {
            return meth;
        } else if ((mods & Modifier.PRIVATE) != 0) {
            return (cl == defCl) ? meth : null;
        } else {
            return packageEquals(cl, defCl) ? meth : null;
        }
    }

    /**
     * Returns non-static private method with given signature defined by given
     * class, or null if none found.  Access checks are disabled on the
     * returned method (if any).
     */
    private static Method getPrivateMethod(Class cl, String name, Class[] argTypes, Class returnType) {
        try {
            Method meth = cl.getDeclaredMethod(name, argTypes);
            meth.setAccessible(true);
            int mods = meth.getModifiers();
            return ((meth.getReturnType() == returnType) && ((mods & Modifier.STATIC) == 0) && ((mods & Modifier.PRIVATE) != 0)) ? meth : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns true if classes are defined in the same package, false
     * otherwise.
     */
    private static boolean packageEquals(Class cl1, Class cl2) {
        Package pkg1 = cl1.getPackage(), pkg2 = cl2.getPackage();
        return ((pkg1 == pkg2) || ((pkg1 != null) && (pkg1.equals(pkg2))));
    }

    /**
     * Compares class names for equality, ignoring package names.  Returns true
     * if class names equal, false otherwise.
     */
    private static boolean classNamesEqual(String name1, String name2) {
        name1 = name1.substring(name1.lastIndexOf('.') + 1);
        name2 = name2.substring(name2.lastIndexOf('.') + 1);
        return name1.equals(name2);
    }

    /**
     * Returns JVM type signature for given class.
     */
    static String getClassSignature(Class cl) {
        StringBuffer sbuf = new StringBuffer();
        while (cl.isArray()) {
            sbuf.append('[');
            cl = cl.getComponentType();
        }
        if (cl.isPrimitive()) {
            if (cl == Integer.TYPE) {
                sbuf.append('I');
            } else if (cl == Byte.TYPE) {
                sbuf.append('B');
            } else if (cl == Long.TYPE) {
                sbuf.append('J');
            } else if (cl == Float.TYPE) {
                sbuf.append('F');
            } else if (cl == Double.TYPE) {
                sbuf.append('D');
            } else if (cl == Short.TYPE) {
                sbuf.append('S');
            } else if (cl == Character.TYPE) {
                sbuf.append('C');
            } else if (cl == Boolean.TYPE) {
                sbuf.append('Z');
            } else if (cl == Void.TYPE) {
                sbuf.append('V');
            } else {
                throw new InternalError();
            }
        } else {
            sbuf.append('L' + cl.getName().replace('.', '/') + ';');
        }
        return sbuf.toString();
    }

    /**
     * Returns JVM type signature for given list of parameters and return type.
     */
    private static String getMethodSignature(Class[] paramTypes, Class retType) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            sbuf.append(getClassSignature(paramTypes[i]));
        }
        sbuf.append(')');
        sbuf.append(getClassSignature(retType));
        return sbuf.toString();
    }

    /**
     * Convenience method for throwing an exception that is either a
     * RuntimeException, Error, or of some unexpected type (in which case it is
     * wrapped inside an IOException).
     */
    private static void throwMiscException(Throwable th) throws IOException {
        if (th instanceof RuntimeException) {
            throw (RuntimeException) th;
        } else if (th instanceof Error) {
            throw (Error) th;
        } else {
            IOException ex = new IOException("unexpected exception type : " + th);
            throw ex;
        }
    }

    /**
     * Returns ObjectStreamField array describing the serializable fields of
     * the given class.  Serializable fields backed by an actual field of the
     * class are represented by ObjectStreamFields with corresponding non-null
     * Field objects.
     */
    private static ObjectStreamField[] getSerialFields(Class cl) {
        ObjectStreamField[] fields;
        if (Serializable.class.isAssignableFrom(cl) && !Externalizable.class.isAssignableFrom(cl) && !Proxy.isProxyClass(cl) && !cl.isInterface()) {
            if ((fields = getDeclaredSerialFields(cl)) == null) {
                fields = getDefaultSerialFields(cl);
            }
            Arrays.sort(fields);
        } else {
            fields = NO_FIELDS;
        }
        return fields;
    }

    /**
     * Returns serializable fields of given class as defined explicitly by a
     * "serialPersistentFields" field, or null if no appropriate
     * "serialPersistendFields" field is defined.  Serializable fields backed
     * by an actual field of the class are represented by ObjectStreamFields
     * with corresponding non-null Field objects.  For compatibility with past
     * releases, a "serialPersistentFields" field with a null value is
     * considered equivalent to not declaring "serialPersistentFields".
     */
    private static ObjectStreamField[] getDeclaredSerialFields(Class cl) {
        ObjectStreamField[] serialPersistentFields = null;
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                serialPersistentFields = (ObjectStreamField[]) f.get(null);
            }
        } catch (Exception ex) {
        }
        if (serialPersistentFields == null) {
            return null;
        } else if (serialPersistentFields.length == 0) {
            return NO_FIELDS;
        }
        ObjectStreamField[] boundFields = new ObjectStreamField[serialPersistentFields.length];
        for (int i = 0; i < serialPersistentFields.length; i++) {
            ObjectStreamField spf = serialPersistentFields[i];
            try {
                Field f = cl.getDeclaredField(spf.getName());
                if ((f.getType() == spf.getType()) && ((f.getModifiers() & Modifier.STATIC) == 0)) {
                    boundFields[i] = new ObjectStreamField(f, spf.isUnshared(), true);
                }
            } catch (NoSuchFieldException ex) {
            }
            if (boundFields[i] == null) {
                boundFields[i] = new ObjectStreamField(spf.getName(), spf.getType(), spf.isUnshared());
            }
        }
        return boundFields;
    }

    /**
     * Returns array of ObjectStreamFields corresponding to all non-static
     * non-transient fields declared by given class.  Each ObjectStreamField
     * contains a Field object for the field it represents.  If no default
     * serializable fields exist, NO_FIELDS is returned.
     */
    private static ObjectStreamField[] getDefaultSerialFields(Class cl) {
        Field[] clFields = cl.getDeclaredFields();
        ArrayList list = new ArrayList();
        int mask = Modifier.STATIC | Modifier.TRANSIENT;
        for (int i = 0; i < clFields.length; i++) {
            if ((clFields[i].getModifiers() & mask) == 0) {
                list.add(new ObjectStreamField(clFields[i], false, true));
            }
        }
        int size = list.size();
        return (size == 0) ? NO_FIELDS : (ObjectStreamField[]) list.toArray(new ObjectStreamField[size]);
    }

    /**
     * Returns explicit serial version UID value declared by given class, or
     * null if none.
     */
    private static Long getDeclaredSUID(Class cl) {
        try {
            Field f = cl.getDeclaredField("serialVersionUID");
            int mask = Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                f.setAccessible(true);
                return new Long(f.getLong(null));
            }
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Computes the default serial version UID value for the given class.
     */
    private static long computeDefaultSUID(Class cl) {
        if (!Serializable.class.isAssignableFrom(cl) || Proxy.isProxyClass(cl)) {
            return 0L;
        }
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeUTF(cl.getName());
            int classMods = cl.getModifiers() & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
            Method[] methods = cl.getDeclaredMethods();
            if ((classMods & Modifier.INTERFACE) != 0) {
                classMods = (methods.length > 0) ? (classMods | Modifier.ABSTRACT) : (classMods & ~Modifier.ABSTRACT);
            }
            dout.writeInt(classMods);
            if (!cl.isArray()) {
                Class[] interfaces = cl.getInterfaces();
                String[] ifaceNames = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    ifaceNames[i] = interfaces[i].getName();
                }
                Arrays.sort(ifaceNames);
                for (int i = 0; i < ifaceNames.length; i++) {
                    dout.writeUTF(ifaceNames[i]);
                }
            }
            Field[] fields = cl.getDeclaredFields();
            MemberSignature[] fieldSigs = new MemberSignature[fields.length];
            for (int i = 0; i < fields.length; i++) {
                fieldSigs[i] = new MemberSignature(fields[i]);
            }
            Arrays.sort(fieldSigs, new Comparator() {

                public int compare(Object o1, Object o2) {
                    String name1 = ((MemberSignature) o1).name;
                    String name2 = ((MemberSignature) o2).name;
                    return name1.compareTo(name2);
                }
            });
            for (int i = 0; i < fieldSigs.length; i++) {
                MemberSignature sig = fieldSigs[i];
                int mods = sig.member.getModifiers();
                if (((mods & Modifier.PRIVATE) == 0) || ((mods & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }
            if (hasStaticInitializer(cl)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(Modifier.STATIC);
                dout.writeUTF("()V");
            }
            Constructor[] cons = cl.getDeclaredConstructors();
            MemberSignature[] consSigs = new MemberSignature[cons.length];
            for (int i = 0; i < cons.length; i++) {
                consSigs[i] = new MemberSignature(cons[i]);
            }
            Arrays.sort(consSigs, new Comparator() {

                public int compare(Object o1, Object o2) {
                    String sig1 = ((MemberSignature) o1).signature;
                    String sig2 = ((MemberSignature) o2).signature;
                    return sig1.compareTo(sig2);
                }
            });
            for (int i = 0; i < consSigs.length; i++) {
                MemberSignature sig = consSigs[i];
                int mods = sig.member.getModifiers();
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }
            MemberSignature[] methSigs = new MemberSignature[methods.length];
            for (int i = 0; i < methods.length; i++) {
                methSigs[i] = new MemberSignature(methods[i]);
            }
            Arrays.sort(methSigs, new Comparator() {

                public int compare(Object o1, Object o2) {
                    MemberSignature ms1 = (MemberSignature) o1;
                    MemberSignature ms2 = (MemberSignature) o2;
                    int comp = ms1.name.compareTo(ms2.name);
                    if (comp == 0) {
                        comp = ms1.signature.compareTo(ms2.signature);
                    }
                    return comp;
                }
            });
            for (int i = 0; i < methSigs.length; i++) {
                MemberSignature sig = methSigs[i];
                int mods = sig.member.getModifiers();
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }
            dout.flush();
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException ex) {
            throw new InternalError();
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage());
        }
    }

    /**
     * Returns true if the given class defines a static initializer method,
     * false otherwise.
     */
    private static native boolean hasStaticInitializer(Class cl);

    /**
     * Class for computing and caching field/constructor/method signatures
     * during serialVersionUID calculation.
     */
    private static class MemberSignature {

        public final Member member;

        public final String name;

        public final String signature;

        public MemberSignature(Field field) {
            member = field;
            name = field.getName();
            signature = getClassSignature(field.getType());
        }

        public MemberSignature(Constructor cons) {
            member = cons;
            name = cons.getName();
            signature = getMethodSignature(cons.getParameterTypes(), Void.TYPE);
        }

        public MemberSignature(Method meth) {
            member = meth;
            name = meth.getName();
            signature = getMethodSignature(meth.getParameterTypes(), meth.getReturnType());
        }
    }

    private static class FieldReflector {

        /** fields to operate on */
        private final ObjectStreamField[] fields;

        /** number of primitive fields */
        private final int numPrimFields;

        private long[] primFieldIDs;

        private long[] objFieldIDs;

        /** field data offsets */
        private final int[] offsets;

        /** field type codes */
        private final char[] typeCodes;

        /** field types */
        private final Class[] types;

        /**
	 * Constructs FieldReflector capable of setting/getting values from the
	 * subset of fields whose ObjectStreamFields contain non-null
	 * reflective Field objects.  ObjectStreamFields with null Fields are
	 * treated as filler, for which get operations return default values
	 * and set operations discard given values.
	 */
        FieldReflector(ObjectStreamField[] fields) {
            this.fields = fields;
            int nfields = fields.length;
            offsets = new int[nfields];
            typeCodes = new char[nfields];
            ArrayList typeList = new ArrayList();
            for (int i = 0; i < nfields; i++) {
                ObjectStreamField f = fields[i];
                Field rf = f.getField();
                offsets[i] = f.getOffset();
                typeCodes[i] = f.getTypeCode();
                if (!f.isPrimitive()) {
                    typeList.add((rf != null) ? rf.getType() : null);
                }
            }
            types = (Class[]) typeList.toArray(new Class[typeList.size()]);
            numPrimFields = nfields - types.length;
            primFieldIDs = new long[numPrimFields];
            objFieldIDs = new long[types.length];
            getFieldIDs(fields, primFieldIDs, objFieldIDs);
        }

        /**
	 * Returns list of ObjectStreamFields representing fields operated on
	 * by this reflector.  The shared/unshared values and Field objects
	 * contained by ObjectStreamFields in the list reflect their bindings
	 * to locally defined serializable fields.
	 */
        ObjectStreamField[] getFields() {
            return fields;
        }

        /**
	 * Fetches the serializable primitive field values of object obj and
	 * marshals them into byte array buf starting at offset 0.  The caller
	 * is responsible for ensuring that obj is of the proper type.
	 */
        void getPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            ObjectOutputStream.getPrimitiveFieldValues(obj, primFieldIDs, typeCodes, buf);
        }

        /**
	 * Sets the serializable primitive fields of object obj using values
	 * unmarshalled from byte array buf starting at offset 0.  The caller
	 * is responsible for ensuring that obj is of the proper type.
	 */
        void setPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            ObjectInputStream.setPrimitiveFieldValues(obj, primFieldIDs, typeCodes, buf);
        }

        /**
	 * Fetches the serializable object field values of object obj and
	 * stores them in array vals starting at offset 0.  The caller is
	 * responsible for ensuring that obj is of the proper type.
	 */
        void getObjFieldValues(Object obj, Object[] vals) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = numPrimFields; i < fields.length; i++) {
                switch(typeCodes[i]) {
                    case 'L':
                    case '[':
                        vals[offsets[i]] = ObjectOutputStream.getObjectFieldValue(obj, objFieldIDs[i - numPrimFields]);
                        break;
                    default:
                        throw new InternalError();
                }
            }
        }

        /**
	 * Sets the serializable object fields of object obj using values from
	 * array vals starting at offset 0.  The caller is responsible for
	 * ensuring that obj is of the proper type; however, attempts to set a
	 * field with a value of the wrong type will trigger an appropriate
	 * ClassCastException.
	 */
        void setObjFieldValues(Object obj, Object[] vals) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = numPrimFields; i < fields.length; i++) {
                if (objFieldIDs[i - numPrimFields] == 0L) continue;
                switch(typeCodes[i]) {
                    case 'L':
                    case '[':
                        Object val = vals[offsets[i]];
                        if (val != null && !types[i - numPrimFields].isInstance(val)) {
                            Field f = fields[i].getField();
                            throw new ClassCastException("cannot assign instance of " + val.getClass().getName() + " to field " + f.getDeclaringClass().getName() + "." + f.getName() + " of type " + f.getType().getName() + " in instance of " + obj.getClass().getName());
                        }
                        ObjectInputStream.setObjectFieldValue(obj, objFieldIDs[i - numPrimFields], types[i - numPrimFields], val);
                        break;
                    default:
                        throw new InternalError();
                }
            }
        }
    }

    /**
     * Matches given set of serializable fields with serializable fields
     * described by the given local class descriptor, and returns a
     * FieldReflector instance capable of setting/getting values from the
     * subset of fields that match (non-matching fields are treated as filler,
     * for which get operations return default values and set operations
     * discard given values).  Throws InvalidClassException if unresolvable
     * type conflicts exist between the two sets of fields.
     */
    private static FieldReflector getReflector(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        Class cl = (localDesc != null && fields.length > 0) ? localDesc.cl : null;
        Object key = new FieldReflectorKey(cl, fields);
        Object entry;
        EntryFuture future = null;
        synchronized (reflectors) {
            if ((entry = reflectors.get(key)) == null) {
                reflectors.put(key, future = new EntryFuture());
            }
        }
        if (entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        } else if (entry instanceof EntryFuture) {
            entry = ((EntryFuture) entry).get();
        } else if (entry == null) {
            try {
                entry = new FieldReflector(matchFields(fields, localDesc));
            } catch (Throwable th) {
                entry = th;
            }
            future.set(entry);
            synchronized (reflectors) {
                reflectors.put(key, entry);
            }
        }
        if (entry instanceof FieldReflector) {
            return (FieldReflector) entry;
        } else if (entry instanceof InvalidClassException) {
            throw (InvalidClassException) entry;
        } else if (entry instanceof RuntimeException) {
            throw (RuntimeException) entry;
        } else if (entry instanceof Error) {
            throw (Error) entry;
        } else {
            throw new InternalError("unexpected entry: " + entry);
        }
    }

    /**
     * FieldReflector cache lookup key.  Keys are considered equal if they
     * refer to the same class and equivalent field formats.
     */
    private static class FieldReflectorKey {

        private final Class cl;

        private final String sigs;

        private final int hash;

        FieldReflectorKey(Class cl, ObjectStreamField[] fields) {
            this.cl = cl;
            StringBuffer sbuf = new StringBuffer();
            for (int i = 0; i < fields.length; i++) {
                ObjectStreamField f = fields[i];
                sbuf.append(f.getName()).append(f.getSignature());
            }
            sigs = sbuf.toString();
            hash = System.identityHashCode(cl) + sigs.hashCode();
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof FieldReflectorKey)) {
                return false;
            }
            FieldReflectorKey key = (FieldReflectorKey) obj;
            return (cl == key.cl && sigs.equals(key.sigs));
        }
    }

    /**
     * Matches given set of serializable fields with serializable fields
     * obtained from the given local class descriptor (which contain bindings
     * to reflective Field objects).  Returns list of ObjectStreamFields in
     * which each ObjectStreamField whose signature matches that of a local
     * field contains a Field object for that field; unmatched
     * ObjectStreamFields contain null Field objects.  Shared/unshared settings
     * of the returned ObjectStreamFields also reflect those of matched local
     * ObjectStreamFields.  Throws InvalidClassException if unresolvable type
     * conflicts exist between the two sets of fields.
     */
    private static ObjectStreamField[] matchFields(ObjectStreamField[] fields, ObjectStreamClass localDesc) throws InvalidClassException {
        ObjectStreamField[] localFields = (localDesc != null) ? localDesc.fields : NO_FIELDS;
        ObjectStreamField[] matches = new ObjectStreamField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ObjectStreamField f = fields[i], m = null;
            for (int j = 0; j < localFields.length; j++) {
                ObjectStreamField lf = localFields[j];
                if (f.getName().equals(lf.getName())) {
                    if ((f.isPrimitive() || lf.isPrimitive()) && f.getTypeCode() != lf.getTypeCode()) {
                        throw new InvalidClassException(localDesc.name, "incompatible types for field " + f.getName());
                    }
                    if (lf.getField() != null) {
                        m = new ObjectStreamField(lf.getField(), lf.isUnshared(), false);
                    } else {
                        m = new ObjectStreamField(lf.getName(), lf.getSignature(), lf.isUnshared());
                    }
                }
            }
            if (m == null) {
                m = new ObjectStreamField(f.getName(), f.getSignature(), false);
            }
            m.setOffset(f.getOffset());
            matches[i] = m;
        }
        return matches;
    }
}
