package org.openorb.orb.rmi;

import java.util.HashMap;
import java.util.Map;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.openorb.util.ReflectionUtils;
import org.openorb.util.RepoIDHelper;

/**
 * This is a default implementation for a Remote interface
 *
 * @author Jerome Daniel
 * @author Michael Rumpf
 */
public final class RMIObjectStreamClass {

    /**
     * Keep the stream classes in thread-local hashmaps.
     * This avoids expensive global mutex locks.
     */
    private static final ThreadLocal STREAM_CLASSES = new ThreadLocal();

    private static final Map GLOBAL_CLASSES = new HashMap();

    private static org.openorb.orb.core.ORBSingleton s_orb = new org.openorb.orb.core.ORBSingleton();

    private static RMIObjectStreamClass s_strosc = new RMIObjectStreamClass(java.lang.String.class, "IDL:omg.org/CORBA/WStringValue:1.0", 0L);

    private static RMIObjectStreamClass s_anyosc = new RMIObjectStreamClass(org.omg.CORBA.Any.class, "RMI:org.omg.CORBA.Any:0000000000000000", 0L);

    private static RMIObjectStreamClass s_tcosc = new RMIObjectStreamClass(org.omg.CORBA.TypeCode.class, "RMI:org.omg.CORBA.TypeCode:0000000000000000", 0L);

    private static RMIObjectStreamClass s_cdosc = new RMIObjectStreamClass(java.lang.Class.class, "RMI:javax.rmi.CORBA.ClassDesc:2BABDA04587ADCCC:CFBF02CF5294176B", 0x2BABDA04587ADCCCL);

    private ObjectStreamClass m_delegate;

    private long m_repo_id_hash;

    private String m_repository_id;

    private boolean m_idl_entity = false;

    private boolean m_externalizable = false;

    private boolean m_custom_marshaled = false;

    private RMIObjectStreamClass[] m_all_classes = null;

    private Class m_class_base;

    private Method m_read_object_method;

    private Method m_write_object_method;

    private Method m_read_resolve_method;

    private Method m_write_replace_method;

    private org.omg.CORBA.TypeCode m_type;

    private Field[] m_fields;

    static {
        try {
            Class t = RMIObjectStreamClass.class;
            s_anyosc.m_read_object_method = t.getDeclaredMethod("readAny", new Class[] { org.omg.CORBA.portable.InputStream.class });
            s_anyosc.m_write_object_method = t.getDeclaredMethod("writeAny", new Class[] { org.omg.CORBA.portable.OutputStream.class, org.omg.CORBA.Any.class });
            s_tcosc.m_read_object_method = t.getDeclaredMethod("readTypeCode", new Class[] { org.omg.CORBA.portable.InputStream.class });
            s_tcosc.m_write_object_method = t.getDeclaredMethod("writeTypeCode", new Class[] { org.omg.CORBA.portable.OutputStream.class, org.omg.CORBA.TypeCode.class });
        } catch (NoSuchMethodException ex) {
            throw new NoSuchMethodError("No method readAny/writeAny/readTypeCode/wrteTypeCode found (" + ex + ")");
        }
        s_strosc.m_type = s_orb.create_value_box_tc(s_strosc.getRepoID(), "WStringValue", s_orb.get_primitive_tc(TCKind.tk_wstring));
        s_anyosc.m_type = s_orb.create_value_box_tc(s_anyosc.getRepoID(), "Any", s_orb.get_primitive_tc(TCKind.tk_any));
        s_tcosc.m_type = s_orb.create_value_box_tc(s_tcosc.getRepoID(), "TypeCode", s_orb.get_primitive_tc(TCKind.tk_TypeCode));
        org.omg.CORBA.ValueMember[] members = new org.omg.CORBA.ValueMember[2];
        members[0] = new org.omg.CORBA.ValueMember();
        members[0].name = "codebase";
        members[0].type = s_strosc.m_type;
        members[0].access = org.omg.CORBA.PUBLIC_MEMBER.value;
        members[1] = new org.omg.CORBA.ValueMember();
        members[1].name = "repid";
        members[1].type = s_strosc.m_type;
        members[1].access = org.omg.CORBA.PUBLIC_MEMBER.value;
        s_cdosc.m_type = s_orb.create_value_tc(s_cdosc.getRepoID(), "ClassDesc", org.omg.CORBA.VM_NONE.value, s_orb.get_primitive_tc(TCKind.tk_null), members);
    }

    private RMIObjectStreamClass(Class clz, String repoID, long repoIDHash) {
        addToLocalCache(clz, this);
        m_delegate = ObjectStreamClass.lookup(clz);
        m_repository_id = repoID.intern();
        m_repo_id_hash = repoIDHash;
        m_idl_entity = isIDLEntity(clz);
        addToGlobalCache(clz, this);
    }

    /**
     * Check whether the class is an IDLEntity.
     *
     * @param clz The class to check.
     * @return True when IDLEntity is assignable from c, false otherwise.
     */
    private static boolean isIDLEntity(Class clz) {
        boolean result = false;
        if (clz != null) {
            result = org.omg.CORBA.portable.IDLEntity.class.isAssignableFrom(clz);
        }
        return result;
    }

    private static boolean isCustomMarshaled(Class clz) {
        boolean result = false;
        if (clz != null) {
            if (!org.omg.CORBA.portable.IDLEntity.class.isAssignableFrom(clz)) {
                if (org.omg.CORBA.Any.class.isAssignableFrom(clz)) {
                    clz = org.omg.CORBA.Any.class;
                } else if (org.omg.CORBA.TypeCode.class.isAssignableFrom(clz)) {
                    clz = org.omg.CORBA.TypeCode.class;
                }
                if (java.io.Externalizable.class.isAssignableFrom(clz)) {
                    result = true;
                } else {
                    if (!clz.isArray()) {
                        result = ReflectionUtils.hasWriteObjectMethod(clz);
                        if (!result) {
                            result = isCustomMarshaled(clz.getSuperclass());
                        }
                    }
                }
            }
        }
        return result;
    }

    private static org.omg.CORBA.TypeCode getTypeCodeFromHelper(Class helper) throws InvalidClassException {
        org.omg.CORBA.TypeCode result = null;
        try {
            result = (org.omg.CORBA.TypeCode) helper.getMethod("type", new Class[0]).invoke(null, new Object[0]);
        } catch (InvocationTargetException ex) {
            Throwable real = ex.getTargetException();
            if (real instanceof RuntimeException) {
                throw (RuntimeException) real;
            }
            if (real instanceof Error) {
                throw (Error) real;
            }
            throw new InvalidClassException(helper.getName(), "Exception while invoking type method (" + real + ")");
        } catch (IllegalAccessException ex) {
            throw new InvalidClassException(helper.getName(), "Exception while invoking type method (" + ex + ")");
        } catch (NoSuchMethodException ex) {
            throw new InvalidClassException(helper.getName(), "Exception while invoking type method (" + ex + ")");
        }
        return result;
    }

    private RMIObjectStreamClass(Class clz, boolean suppressPackageCheck) throws InvalidClassException {
        Class lookupClz = clz;
        addToLocalCache(lookupClz, this);
        if (org.omg.CORBA.Any.class.isAssignableFrom(clz)) {
            clz = org.omg.CORBA.Any.class;
        } else if (org.omg.CORBA.TypeCode.class.isAssignableFrom(clz)) {
            clz = org.omg.CORBA.TypeCode.class;
        }
        m_repository_id = RepoIDHelper.getRepoID(clz);
        m_repo_id_hash = RepoIDHelper.getRepoIDHash(clz);
        m_delegate = ObjectStreamClass.lookup(clz);
        if (clz.isArray()) {
            String[] names = RepoIDHelper.mangleClassName(clz);
            Class cmpt = clz.getComponentType();
            TypeCode contained = lookupTypeCode(cmpt);
            m_type = s_orb.create_value_box_tc(getRepoID(), names[1], s_orb.create_sequence_tc(0, contained));
            addToGlobalCache(lookupClz, this);
            return;
        }
        m_custom_marshaled = isCustomMarshaled(clz);
        org.omg.CORBA.TypeCode baseType = null;
        ObjectStreamField[] fields = m_delegate.getFields();
        boolean[] isPublic = new boolean[fields.length];
        int totalPublic = 0;
        RMIObjectStreamClass parent = null;
        m_fields = new Field[fields.length];
        for (int i = 0; i < m_fields.length; ++i) {
            ObjectStreamField currentField = fields[i];
            try {
                m_fields[i] = clz.getDeclaredField(currentField.getName());
                m_fields[i].setAccessible(true);
                if (Modifier.isPublic(m_fields[i].getModifiers())) {
                    isPublic[i] = true;
                    totalPublic++;
                }
            } catch (NoSuchFieldException ex) {
            }
        }
        m_idl_entity = isIDLEntity(clz);
        if (isIDLEntity()) {
            if (org.omg.CORBA.Any.class.isAssignableFrom(clz)) {
                m_type = s_anyosc.m_type;
                m_read_object_method = s_anyosc.m_read_object_method;
                m_write_object_method = s_anyosc.m_write_object_method;
            } else if (org.omg.CORBA.TypeCode.class.isAssignableFrom(clz)) {
                m_type = s_tcosc.m_type;
                m_read_object_method = s_tcosc.m_read_object_method;
                m_write_object_method = s_tcosc.m_write_object_method;
            } else {
                Class helper = UtilDelegateImpl.locateHelperClass(clz);
                if (helper == null) {
                    throw new InvalidClassException("Unable to find class '" + clz.getName() + "Helper'", "ClassNotFoundException");
                }
                String[] names = RepoIDHelper.mangleClassName(clz);
                m_type = s_orb.create_value_box_tc(getRepoID(), names[1], getTypeCodeFromHelper(helper));
                try {
                    m_read_object_method = helper.getMethod("read", new Class[] { org.omg.CORBA.portable.InputStream.class });
                    m_write_object_method = helper.getMethod("write", new Class[] { org.omg.CORBA.portable.OutputStream.class, clz });
                } catch (NoSuchMethodException ex) {
                    throw new InvalidClassException(helper.getName(), "Couldn't find read/write method (" + ex + ")");
                }
            }
        } else {
            if (java.io.Externalizable.class.isAssignableFrom(clz)) {
                Class base = clz.getSuperclass();
                while (java.io.Serializable.class.isAssignableFrom(base) && parent == null) {
                    try {
                        parent = lookup(base);
                    } catch (InvalidClassException ex) {
                        try {
                            parent = new RMIObjectStreamClass(base, true);
                        } catch (InvalidClassException ex1) {
                            base = base.getSuperclass();
                        }
                    }
                }
                if (parent != null) {
                    baseType = parent.m_type;
                }
                if (!suppressPackageCheck) {
                    try {
                        clz.getConstructor(new Class[0]);
                    } catch (NoSuchMethodException ex) {
                        throw new InvalidClassException(base.getName() + "Missing no-arg constructor for class (" + ex + ")");
                    }
                }
                m_externalizable = true;
                m_class_base = clz;
                m_fields = null;
            } else {
                m_class_base = clz.getSuperclass();
                if (m_class_base != null && java.io.Serializable.class.isAssignableFrom(m_class_base)) {
                    try {
                        parent = lookup(m_class_base);
                    } catch (InvalidClassException ex) {
                        parent = new RMIObjectStreamClass(m_class_base, true);
                    }
                }
                if (parent != null) {
                    baseType = parent.m_type;
                    m_class_base = parent.m_class_base;
                    m_all_classes = new RMIObjectStreamClass[parent.m_all_classes.length + 1];
                    System.arraycopy(parent.m_all_classes, 0, m_all_classes, 0, parent.m_all_classes.length);
                    m_all_classes[parent.m_all_classes.length] = this;
                } else {
                    m_all_classes = new RMIObjectStreamClass[] { this };
                }
                if (m_class_base != null && !m_class_base.equals(java.lang.Object.class)) {
                    java.lang.reflect.Constructor ctor;
                    try {
                        ctor = m_class_base.getDeclaredConstructor(new Class[0]);
                    } catch (NoSuchMethodException ex) {
                        throw new InvalidClassException(m_class_base.getName() + "Missing no-arg constructor for class (" + ex + ")");
                    }
                    int modf = ctor.getModifiers();
                    if (Modifier.isPrivate(modf) || !(suppressPackageCheck || Modifier.isPublic(modf) || Modifier.isProtected(modf) || m_class_base.getPackage().equals(clz.getPackage()))) {
                        throw new InvalidClassException(m_class_base.getName(), "IllegalAccessException");
                    }
                }
                if (!clz.isArray()) {
                    m_read_object_method = ReflectionUtils.getReadObjectMethod(clz);
                    m_write_object_method = ReflectionUtils.getWriteObjectMethod(clz);
                    m_write_replace_method = ReflectionUtils.getWriteReplaceMethod(clz);
                    m_read_resolve_method = ReflectionUtils.getReadResolveMethod(clz);
                }
            }
            short type_modifier = isCustomMarshaled() ? org.omg.CORBA.VM_CUSTOM.value : org.omg.CORBA.VM_NONE.value;
            if (baseType == null) {
                baseType = s_orb.get_primitive_tc(TCKind.tk_null);
            }
            org.omg.CORBA.ValueMember[] tcMembers = new org.omg.CORBA.ValueMember[m_externalizable ? totalPublic : fields.length];
            if (fields.length > 0) {
                String[] memberNames = new String[tcMembers.length];
                int upto = 0;
                for (int i = 0; i < fields.length; ++i) {
                    if (!m_externalizable || isPublic[i]) {
                        tcMembers[upto] = new org.omg.CORBA.ValueMember();
                        memberNames[upto] = fields[i].getName();
                        Class fType = fields[i].getType();
                        RMIObjectStreamClass sc = (RMIObjectStreamClass) getFromCache(fType);
                        if (sc != null) {
                            if (sc.type() == null) {
                                tcMembers[upto].type = s_orb.create_recursive_tc(sc.getRepoID());
                            } else {
                                tcMembers[upto].type = sc.type();
                            }
                        } else {
                            tcMembers[upto].type = lookupTypeCode(fType);
                        }
                        tcMembers[upto].access = isPublic[i] ? org.omg.CORBA.PUBLIC_MEMBER.value : org.omg.CORBA.PRIVATE_MEMBER.value;
                        ++upto;
                    }
                }
                RepoIDHelper.mangleMemberNames(clz, memberNames);
                for (int i = 0; i < memberNames.length; ++i) {
                    tcMembers[i].name = memberNames[i];
                }
            }
            try {
                m_type = s_orb.create_value_tc(getRepoID(), "", type_modifier, baseType, tcMembers);
            } catch (org.omg.CORBA.BAD_PARAM ex) {
                throw new InvalidClassException(clz.getName(), "Duplicate member name (" + ex + ")");
            }
        }
        addToGlobalCache(lookupClz, this);
    }

    /**
     * Try to get the corresponding RMIObjectStreamClass from an internal hash map.
     * This method also checks whether the class is serializable at all. This check
     * fails if the class is not derived from Serializable or if it's on of the following
     * types: org.omg.CORBA.Object, org.omg.CORBA.portable.ValueBase, or java.rmi.Remote.
     *
     * @param clz The class for which to get the corresponding RMIObjectStreamClass for.
     * @return The RMIObjectStreamClass from the internal hash map if an entry was found
     * or a new instance.
     * @throws InvalidClassException When the class is neither assignable from java.io.Serializable
     * or org.omg.CORBA.portable.ValueBase.
     */
    public static RMIObjectStreamClass lookup(Class clz) throws InvalidClassException {
        RMIObjectStreamClass ret = getFromCache(clz);
        if (ret == null) {
            if (!java.io.Serializable.class.isAssignableFrom(clz) || org.omg.CORBA.Object.class.isAssignableFrom(clz) || org.omg.CORBA.portable.ValueBase.class.isAssignableFrom(clz) || java.rmi.Remote.class.isAssignableFrom(clz)) {
                return null;
            }
            ret = new RMIObjectStreamClass(clz, false);
        }
        return ret;
    }

    /**
     * Create typecode from runtime class. This will only successfully return
     * typecodes for types which can be marshalled.
     *
     * @param clz The class for which to get the type code.
     * @return the target object's typecode, or null if the target object is
     * not serializable.
     * @throws InvalidClassException When the method type() could not be invoked on the
     * helper class of clz.
     */
    public static org.omg.CORBA.TypeCode lookupTypeCode(Class clz) throws InvalidClassException {
        if (clz.isPrimitive()) {
            if (clz.equals(boolean.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_boolean);
            } else if (clz.equals(byte.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_octet);
            } else if (clz.equals(short.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_short);
            } else if (clz.equals(int.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_long);
            } else if (clz.equals(long.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_longlong);
            } else if (clz.equals(float.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_float);
            } else if (clz.equals(double.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_double);
            } else if (clz.equals(char.class)) {
                return s_orb.get_primitive_tc(TCKind.tk_wchar);
            } else {
                throw new InternalError("Unknown primitive type");
            }
        }
        if (org.omg.CORBA.Object.class.equals(clz)) {
            return s_orb.get_primitive_tc(TCKind.tk_objref);
        }
        if (Object.class.equals(clz)) {
            return s_orb.get_primitive_tc(TCKind.tk_abstract_interface);
        }
        if (java.rmi.Remote.class.isAssignableFrom(clz)) {
            RMIRemoteStreamClass rsc = RMIRemoteStreamClass.lookup(clz);
            if (rsc == null) {
                return null;
            }
            return rsc.getInterfaceTypesNoCopy()[0];
        }
        if (org.omg.CORBA.portable.IDLEntity.class.isAssignableFrom(clz)) {
            if (clz.equals(org.omg.CORBA.Any.class)) {
                return s_anyosc.type();
            }
            if (clz.equals(org.omg.CORBA.TypeCode.class)) {
                return s_tcosc.type();
            }
            Class helper = UtilDelegateImpl.locateHelperClass(clz);
            if (helper == null) {
                throw new InvalidClassException("Unable to find class '" + clz.getName() + "Helper'", "ClassNotFoundException");
            }
            return getTypeCodeFromHelper(helper);
        }
        if (java.io.Serializable.class.isAssignableFrom(clz)) {
            RMIObjectStreamClass osc = RMIObjectStreamClass.lookup(clz);
            if (osc != null) {
                return osc.type();
            }
        }
        String clzName = clz.getName();
        Package pkg = clz.getPackage();
        String npkg = clzName;
        if (pkg != null && pkg.getName().length() > 0) {
            npkg = clzName.substring(pkg.getName().length() + 1);
        }
        return s_orb.create_abstract_interface_tc("RMI:" + clzName + ":0000000000000000", npkg);
    }

    /**
     * Returns the hashed repository ID value, as specified in the corba spec
     * 10.6.2.
     *
     * @return The repository ID hash value.
     */
    public long getRepoIDHash() {
        return m_repo_id_hash;
    }

    /**
     * Return repository ID string.
     *
     * @return The repository ID.
     */
    public String getRepoID() {
        return m_repository_id;
    }

    /**
     * Return base class.
     *
     * @return The base class.
     */
    public Class getBaseClass() {
        return m_class_base;
    }

    /**
     * True if the class is custom marshaled.
     *
     * @return True is the class is custom marshaled, false otherwise.
     */
    public boolean isCustomMarshaled() {
        return m_custom_marshaled;
    }

    /**
     * True if the class is externalizable.
     *
     * @return True is the class is externalizable, false otherwise.
     */
    public boolean isExternalizable() {
        return m_externalizable;
    }

    /**
     * True if the target has the writeObject method.
     *
     * @return True is the target has a writeObject() method, false otherwise.
     */
    public boolean hasWriteObject() {
        return !m_idl_entity && m_write_object_method != null;
    }

    /**
     * True if the target has the writeObject method.
     *
     * @return True is the target has a readObject() method, false otherwise.
     */
    public boolean hasReadObject() {
        return !m_idl_entity && m_read_object_method != null;
    }

    /**
     * True if the class is a boxed IDL entity.
     *
     * @return True is the class is a boxed IDL entity, false otherwise.
     */
    public boolean isIDLEntity() {
        return m_idl_entity;
    }

    /**
     * Return the stream classes for the parent classes. For externalizable
     * and IDLEntities this will return null.
     *
     * @return An array of the stream classes of all parents.
     */
    public RMIObjectStreamClass[] getAllStreamClasses() {
        return m_all_classes;
    }

    /**
     * Return the class in the local VM that this version is mapped to.
     * Null is returned if there is no corresponding local class.
     *
     * @return the <code>Class</code> instance that this descriptor represents
     */
    public Class forClass() {
        return m_delegate.forClass();
    }

    /**
     * The name of the class described by this descriptor.
     *
     * @return a <code>String</code> representing the fully qualified name of
     * the class
     */
    public String getName() {
        return m_delegate.getName();
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
        return m_delegate.getSerialVersionUID();
    }

    /**
     * Return a string describing this ObjectStreamClass.
     *
     * @return A description of this instance.
     */
    public String toString() {
        return m_delegate.toString();
    }

    /**
     * Return the typecode.
     *
     * @return The type code of this instance.
     */
    public org.omg.CORBA.TypeCode type() {
        return m_type;
    }

    /**
     * Return the object stream class.
     *
     * @return the object stream class of this instance.
     */
    public ObjectStreamClass getObjectStreamClass() {
        return m_delegate;
    }

    /**
     * Return an array of the fields of this serializable class.
     * returns the serialized fields for this class.
     */
    public ObjectStreamField[] getObjectStreamFields() {
        return m_delegate.getFields();
    }

    /**
     * Return an array of the fields of this serializable class.
     * Unlike the standard object stream class this returns reflection
     * fields rather than ObjectStreamFields, however the array is still
     * sorted in the correct order for serialization.
     *
     * @return an array containing an element for each persistent
     * field of this class. Returns an array of length zero if
     * there are no fields.
     */
    Field[] getSerializedFields() {
        return m_fields;
    }

    /**
     * Call the readObject method.
     *
     * @param target The object on which to call the readObject() method.
     * @param is The input stream to pass to the readObject() method.
     * @return True if readObject was called, false otherwise.
     * @throws IOException An exception that might occur during the readObject() call.
     */
    boolean readObject(Object target, java.io.ObjectInputStream is) throws IOException, ClassNotFoundException {
        return ReflectionUtils.readObject(m_read_object_method, target, is);
    }

    /**
     * Call the writeObject method.
     *
     * @param target The object on which to call the writeReplace() method.
     * @param os The output stream to pass to the writeObject() method.
     * @throws IOException That might occur during the writeReplace() call.
     */
    void writeObject(Object target, java.io.ObjectOutputStream os) throws IOException {
        ReflectionUtils.writeObject(m_write_object_method, target, os);
    }

    /**
     * Call the readResolve method.
     *
     * @param target The object on which to call the readResolve() method.
     * @return The resolved object returned from readResolve().
     */
    Object readResolve(Object target) throws ObjectStreamException {
        return ReflectionUtils.readResolve(m_read_resolve_method, target);
    }

    /**
     * Call the writeReplace method.
     *
     * @param target The object on which to call the writeReplace() method.
     * @return The replaced object returned from writeReplace().
     */
    Object writeReplace(Object target) throws ObjectStreamException {
        return ReflectionUtils.writeReplace(m_write_replace_method, target);
    }

    /**
     * Call the IDLEntity read operation in the helper class.
     *
     * @param is The input stream to pass to the readObject() method.
     * @return The object returned from the read() call.
     */
    Object read(org.omg.CORBA.portable.InputStream is) {
        if (!isIDLEntity()) {
            throw new IllegalStateException("IDLEntity");
        }
        try {
            return m_read_object_method.invoke(null, new Object[] { is });
        } catch (InvocationTargetException ex) {
            Throwable nex = ex.getTargetException();
            if (nex instanceof RuntimeException) {
                throw (RuntimeException) nex;
            }
            if (nex instanceof Error) {
                throw (Error) nex;
            }
            throw new Error("Unexpected exception (" + ex.getTargetException() + ")");
        } catch (Exception ex) {
            throw new Error("Unexpected exception (" + ex + ")");
        }
    }

    /**
     * Call the IDLEntity write operation in the helper class.
     *
     * @param os The output stream to pass to the write() method.
     * @param value The value for which to call the write() method.
     * @return The object returned from the write() method.
     */
    Object write(org.omg.CORBA.portable.OutputStream os, Object value) {
        if (!isIDLEntity()) {
            throw new IllegalStateException("IDLEntity");
        }
        try {
            return m_write_object_method.invoke(null, new Object[] { os, value });
        } catch (InvocationTargetException ex) {
            Throwable nex = ex.getTargetException();
            if (nex instanceof RuntimeException) {
                throw (RuntimeException) nex;
            }
            if (nex instanceof Error) {
                throw (Error) nex;
            }
            throw new Error("Unexpected exception (" + ex.getTargetException() + ")");
        } catch (Exception ex) {
            throw new Error("Unexpected exception (" + ex + ")");
        }
    }

    private static org.omg.CORBA.Any readAny(org.omg.CORBA.portable.InputStream is) {
        org.omg.CORBA.ORB orb = null;
        if (is instanceof org.openorb.orb.io.ExtendedInputStream) {
            orb = ((org.openorb.orb.io.ExtendedInputStream) is).orb();
        } else {
            orb = org.openorb.orb.rmi.DefaultORB.getORB();
        }
        org.omg.CORBA.Any any = orb.create_any();
        any.read_value(is, is.read_TypeCode());
        return any;
    }

    private static void writeAny(org.omg.CORBA.portable.OutputStream os, org.omg.CORBA.Any any) {
        os.write_TypeCode(any.type());
        any.write_value(os);
    }

    private static org.omg.CORBA.TypeCode readTypeCode(org.omg.CORBA.portable.InputStream is) {
        return is.read_TypeCode();
    }

    private static void writeTypeCode(org.omg.CORBA.portable.OutputStream os, org.omg.CORBA.TypeCode tc) {
        os.write_TypeCode(tc);
    }

    /**
     * Keep the stream classes as thread local storage to avoid expensive mutex locks.
     * When threads die then the content of the map will be gc'ed.
     */
    private static Map getStreamClassesMap() {
        Map map = (Map) STREAM_CLASSES.get();
        if (map == null) {
            map = new HashMap();
            map.put(java.lang.String.class, s_strosc);
            map.put(org.omg.CORBA.Any.class, s_anyosc);
            map.put(org.omg.CORBA.TypeCode.class, s_tcosc);
            map.put(java.lang.Class.class, s_cdosc);
            STREAM_CLASSES.set(map);
        }
        return map;
    }

    /**
     * Add a stream instance for a class to the cache.
     * Store the instance in the thread local cache for a faster non-synchronized
     * access. Keep it in a global map too cause threads could have been started
     * temporary.
     */
    private static void addToLocalCache(Class clz, RMIObjectStreamClass strclz) {
        Map map = getStreamClassesMap();
        map.put(clz, strclz);
    }

    private static void addToGlobalCache(Class clz, RMIObjectStreamClass strclz) {
        synchronized (GLOBAL_CLASSES) {
            if (GLOBAL_CLASSES.get(clz) == null) {
                GLOBAL_CLASSES.put(clz, strclz);
            }
        }
    }

    /**
     * Get a stream instance for a class from the cache.
     */
    private static RMIObjectStreamClass getFromCache(Class clz) {
        Map map = getStreamClassesMap();
        RMIObjectStreamClass strclz = (RMIObjectStreamClass) map.get(clz);
        if (strclz == null) {
            synchronized (GLOBAL_CLASSES) {
                strclz = (RMIObjectStreamClass) GLOBAL_CLASSES.get(clz);
            }
            if (strclz != null) {
                map.put(clz, strclz);
            }
        }
        return strclz;
    }
}
