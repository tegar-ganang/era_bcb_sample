package org.openorb.orb.rmi;

import java.io.Externalizable;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.rmi.CORBA.Util;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.omg.CORBA.WStringValueHelper;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.SendingContext._CodeBaseStub;
import org.omg.SendingContext.CodeBase;
import org.openorb.orb.iiop.CDRInputStream;
import org.openorb.orb.iiop.CDROutputStream;
import org.openorb.orb.util.Trace;
import org.openorb.util.RepoIDHelper;
import org.openorb.util.NumberCache;
import org.openorb.util.CharacterCache;

/**
 * This class is able to marshal and to unmarshal a RMI value.
 *
 * @author Jerome Daniel
 * @author Michael Rumpf
 */
public final class ValueHandlerImpl extends AbstractLogEnabled implements javax.rmi.CORBA.ValueHandler {

    private static final Object SYNC_INSTANCE = new Object();

    private static ValueHandlerImpl s_value_handler_instance;

    private static DeserializationKernel s_kernel = DeserializationKernelFactory.retrieveDeserializationKernel();

    private static org.omg.CORBA.WStringValueHelper s_string_helper = new org.omg.CORBA.WStringValueHelper();

    /**
     * Only ever used for non-openorb streams.
     */
    private static ThreadLocal s_ri_local = new ThreadLocal();

    /**
     * Constructor.
     */
    private ValueHandlerImpl() {
        if (s_value_handler_instance != null) {
            throw new Error("Multiple copies of ValueHandlerImpl instantiated");
        }
    }

    /**
     * Create a value handler instance. Only one instance will
     * be created, as the ValueHandler is a singleton for the ORB
     * instance. Subsequent calls to this method return the same
     * instance.
     *
     * @param logger The logger for the ValueHandler instance.
     * @return The singleton ValueHandler instance.
     */
    public static synchronized ValueHandlerImpl createValueHandler(Logger logger) {
        if (s_value_handler_instance == null) {
            s_value_handler_instance = new ValueHandlerImpl();
            s_value_handler_instance.enableLogging(logger);
        }
        return s_value_handler_instance;
    }

    /**
     * Only ever used for non-openorb streams.
     */
    private static class ReadIndirectTable {

        private int m_level;

        private Map m_map = new HashMap();

        public ReadIndirectTable() {
            m_level = 0;
            m_map = new HashMap();
        }

        public ReadIndirectTable(int level, HashMap map) {
            m_level = level;
            m_map = map;
        }

        public int getLevel() {
            return m_level;
        }

        public void setLevel(int level) {
            m_level = level;
        }

        public int incLevel() {
            m_level++;
            return m_level;
        }

        public int decLevel() {
            m_level--;
            return m_level;
        }

        public Map getMap() {
            return m_map;
        }

        public void setMap(HashMap map) {
            m_map = map;
        }
    }

    /**
     * The writeValue method can be used to write GIOP data, including RMI remote
     * objects and serialized data objects, to an underlying portable OutputStream.
     *
     * The implementation of the writeValue method interacts with the core Java
     * serialization machinery. The data generated during serialization is written using the
     * underlying OutputStream object.
     *
     * @param out_sub The stream to write the value to.
     * @param value The value to write to the stream.
     */
    public void writeValue(org.omg.CORBA.portable.OutputStream out_sub, Serializable value) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeValue( " + out_sub + ", " + value + " )");
        }
        OutputStream out = (OutputStream) out_sub;
        if (value instanceof String) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("writeValue: wstring to be written");
            }
            out.write_wstring((String) value);
            return;
        }
        if (value instanceof Class) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("writeValue: ClassDesc to be written");
            }
            writeClassDesc(out, (Class) value);
            return;
        }
        if (value.getClass().isArray()) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("writeValue: array to be written");
            }
            writeArray(out, value);
            return;
        }
        RMIObjectStreamClass sc;
        if (out instanceof CDROutputStream) {
            sc = ((CDROutputStream) out).getObjectStreamClass();
        } else {
            try {
                sc = RMIObjectStreamClass.lookup(value.getClass());
            } catch (InvalidClassException ex) {
                if (getLogger() != null && getLogger().isErrorEnabled()) {
                    getLogger().error("Exception looking up the class " + value.getClass().getName() + ".", ex);
                }
                throw new org.omg.CORBA.MARSHAL("Exception looking up the class " + value.getClass().getName() + " (" + ex + ")");
            }
        }
        if (sc.isIDLEntity()) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("writeValue: IDLEntity to be written");
            }
            sc.write(out, value);
            return;
        }
        try {
            RMIObjectOutputStream oos = new RMIObjectOutputStream(this, out);
            if (sc.isExternalizable()) {
                out.write_octet((byte) 1);
                ((Externalizable) value).writeExternal(oos);
                return;
            }
            RMIObjectStreamClass[] scc = sc.getAllStreamClasses();
            for (int i = 0; i < scc.length; ++i) {
                if (scc[i].hasWriteObject()) {
                    if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                        getLogger().debug("writeValue: hasWriteObject scc[" + i + "] = " + scc[i]);
                    }
                    out.write_octet((byte) 1);
                    out.write_boolean(false);
                    oos.setContext(value, scc[i]);
                    scc[i].writeObject(value, oos);
                } else {
                    if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                        getLogger().debug("writeValue: defaultWriteObject scc[" + i + "] = " + scc[i]);
                    }
                    defaultWriteObject(out, value, scc[i]);
                }
            }
        } catch (IOException ex) {
            throw UtilDelegateImpl.mapIOSysException(ex);
        }
    }

    /**
     * The readValue method can be used to read GIOP data, including RMI remote
     * objects and serialized data objects, from an underlying portable InputStream.
     *
     * The implementation of the readValue method interacts with the core Java
     * serialization machinery. The data required during deserialization is read using the
     * underlying InputStream object.
     *
     * @param in_sub The input stream to read the value from.
     * @param offset the offset in the stream of the value being unmarshaled.
     * @param expected Java class of the value to be unmarshaled.
     * @param repoID repository ID unmarshaled from the value header by the caller of
     *        readValue.
     * @param sender the sending context object passed in the optional service
     *        context tagged SendingContextRunTime in the GIOP header, if
     *        any, or null if no sending context was passed.
     * @return The value read from the stream.
     */
    public Serializable readValue(org.omg.CORBA.portable.InputStream in_sub, int offset, Class expected, String repoID, org.omg.SendingContext.RunTime sender) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("readValue( " + in_sub + ", " + offset + ", " + expected + ", " + repoID + ", " + sender + " )");
        }
        if (expected == java.lang.Object.class && repoID != null) {
            expected = null;
        }
        String codebase = null;
        InputStream in = (InputStream) in_sub;
        if (in instanceof CDRInputStream) {
            codebase = ((CDRInputStream) in).getValueCodebase();
        } else {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: checking for indirection");
            }
            Serializable ret = checkReadIndirect(offset);
            if (ret != null) {
                return ret;
            }
        }
        final boolean isWString;
        if (expected == null) {
            isWString = repoID.equals(WStringValueHelper.id());
        } else {
            isWString = String.class.isAssignableFrom(expected);
        }
        if (isWString) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: wstring to be read");
            }
            return in.read_wstring();
        }
        final boolean isClassDesc;
        if (expected == null) {
            isClassDesc = repoID.startsWith("RMI:javax.rmi.CORBA.ClassDesc:");
        } else {
            isClassDesc = Class.class.isAssignableFrom(expected);
        }
        if (isClassDesc) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: ClassDesc to be read");
            }
            return readClassDesc(in);
        }
        Class clz = null;
        if (expected != null) {
            int idxColon = repoID.indexOf(':', 4);
            if (idxColon != -1) {
                String clzFromRepoId = repoID.substring(4, idxColon);
                if (clzFromRepoId.equals(expected)) {
                    if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                        getLogger().debug("readValue: clz from the repoID matches the expected" + " class ( repoID=" + repoID + ", expected=" + expected + " )");
                    }
                    clz = expected;
                }
            }
        }
        if (clz == null) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: We try to get the class from the sending" + " context now ( codebase=" + codebase + ", sender=" + sender + " )");
            }
            if (codebase == null && sender != null) {
                CodeBase cb;
                if (sender instanceof CodeBase) {
                    cb = (CodeBase) sender;
                } else {
                    _CodeBaseStub stub = new _CodeBaseStub();
                    stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl) sender)._get_delegate());
                    cb = stub;
                }
                try {
                    codebase = cb.implementation(repoID);
                } catch (org.omg.CORBA.BAD_OPERATION ex) {
                }
            }
            String clzName = RepoIDHelper.unmangleRepoIDtoClassName(repoID);
            if (clzName == null) {
                if (getLogger() != null && getLogger().isErrorEnabled()) {
                    getLogger().error("Could not convert classname from" + " repository ID: \"" + repoID + "\"");
                }
                throw new org.omg.CORBA.MARSHAL("Could not convert classname from" + " repository ID: \"" + repoID + "\"");
            }
            try {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("readValue: Util.loadClass( " + clzName + ", " + codebase + ", null )");
                }
                clz = Util.loadClass(clzName, codebase, (ClassLoader) null);
            } catch (ClassNotFoundException ex) {
                if (getLogger() != null && getLogger().isErrorEnabled()) {
                    getLogger().error("Couldn't find class " + clzName + ".", ex);
                }
                throw new org.omg.CORBA.MARSHAL("Couldn't find class " + clzName + " (" + ex + ")");
            }
        }
        if (clz.isArray()) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: array to be read");
            }
            return readArray(in, offset, clz);
        }
        RMIObjectStreamClass sc;
        try {
            sc = RMIObjectStreamClass.lookup(clz);
        } catch (InvalidClassException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Class is not compatible with serialization: " + clz.toString() + ".", ex);
            }
            throw new org.omg.CORBA.MARSHAL("Class is not compatible with serialization: " + clz.toString() + " (" + ex + ")");
        }
        if (sc == null) {
            throw new org.omg.CORBA.MARSHAL("Unable to locate serialization data for class: " + clz.toString());
        }
        if (sc.isIDLEntity()) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: IDLEntity to be read");
            }
            return (Serializable) sc.read(in);
        }
        try {
            Serializable value = null;
            try {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("readValue: allocateNewObject( " + clz + ", " + sc.getBaseClass() + ")");
                }
                value = (Serializable) s_kernel.allocateNewObject(clz, sc.getBaseClass());
            } catch (Error e) {
                throw e;
            }
            boolean doPop = addReadIndirect(in, offset, value);
            RMIObjectInputStream ois = new RMIObjectInputStream(this, in);
            if (!sc.isExternalizable()) {
                RMIObjectStreamClass[] scc = sc.getAllStreamClasses();
                for (int i = 0; i < scc.length; ++i) {
                    ois.setContext(value, scc[i]);
                    if (scc[i].hasWriteObject()) {
                        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                            getLogger().debug("readValue: hasWriteObject scc[" + i + "] = " + scc[i]);
                        }
                        if (in.read_octet() != 1) {
                            throw new org.omg.CORBA.MARSHAL();
                        }
                        if (in.read_boolean()) {
                            defaultReadObject(in, value, sc);
                            scc[i].readObject(value, ois);
                        } else if (!scc[i].readObject(value, ois)) {
                            defaultReadObject(in, value, scc[i]);
                        }
                    } else if (!scc[i].readObject(value, ois)) {
                        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                            getLogger().debug("readValue: defaultReadObject scc[" + i + "] = " + scc[i]);
                        }
                        defaultReadObject(in, value, scc[i]);
                    }
                }
            } else {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("readValue: Externalizable to be read");
                }
                if (in.read_octet() != 1) {
                    throw new org.omg.CORBA.MARSHAL();
                }
                ((Externalizable) value).readExternal(ois);
            }
            if (doPop) {
                popReadIndirect();
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: readResolve to be called");
            }
            value = (java.io.Serializable) sc.readResolve(value);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("readValue: Successfully finished ( value = " + value + " )");
            }
            return value;
        } catch (ClassNotFoundException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("ClassNotFoundException occured!", ex);
            }
            throw new org.omg.CORBA.MARSHAL("ClassNotFoundException occured! (" + ex + ")");
        } catch (IOException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("IOException occured!", ex);
            }
            throw UtilDelegateImpl.mapIOSysException(ex);
        } catch (Exception ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Unknown exception occured!", ex);
            }
            throw new org.omg.CORBA.UNKNOWN("Unexpected exception occured! (" + ex + ")");
        }
    }

    /**
     * This method returns the RMI-style repository ID string for clz.
     *
     * @param clz The class to get the RMI reopsitory id for.
     * @return The RMI repository id for the class clz.
     */
    public String getRMIRepositoryID(Class clz) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("getRMIRepositoryID( " + clz + " )");
        }
        if (org.omg.CORBA.Object.class.isAssignableFrom(clz)) {
            if (org.omg.CORBA.portable.ObjectImpl.class.isAssignableFrom(clz)) {
                String[] ids;
                try {
                    ids = ((org.omg.CORBA.portable.ObjectImpl) clz.newInstance())._ids();
                    if (ids.length > 0) {
                        return ids[0];
                    }
                } catch (Exception ex) {
                }
            }
            return "IDL:omg.org/CORBA/Object:1.0";
        }
        if (Serializable.class.isAssignableFrom(clz)) {
            RMIObjectStreamClass descrip;
            try {
                descrip = RMIObjectStreamClass.lookup(clz);
                if (descrip != null) {
                    return descrip.getRepoID();
                }
            } catch (InvalidClassException ex) {
            }
        }
        return "RMI:" + RepoIDHelper.mangleClassName(clz)[0] + ":0000000000000000";
    }

    /**
     * This method returns true if the value is custom marshaled and therefore requires a
     * chunked encoding, and false otherwise.
     *
     * @param clz The class to check whether it is custom marshaled or not.
     * @return True if class clz is custom marshalled, false otherwise.
     */
    public boolean isCustomMarshaled(Class clz) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("isCustomMarshaled( " + clz + " )");
        }
        try {
            RMIObjectStreamClass descrip = RMIObjectStreamClass.lookup(clz);
            if (descrip != null) {
                return descrip.isCustomMarshaled();
            }
        } catch (InvalidClassException ex) {
        }
        return false;
    }

    /**
     * This method returns the ValueHandler object's SendingContext::RunTime object reference,
     * which is used to construct the SendingContextRuntTime service context.
     *
     * @return null, needs to be implemented.
     */
    public org.omg.SendingContext.RunTime getRunTimeCodeBase() {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("getRunTimeCodeBase() NOT IMPLEMENTED!");
        }
        return null;
    }

    /**
     * This method returns the serialization replacement for the value object.
     *
     * @param value The value for which to call the writeReplace method.
     * @return The replaced value returned by the writeReplace method.
     */
    public Serializable writeReplace(Serializable value) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeReplace( " + value + " )");
        }
        RMIObjectStreamClass descrip = null;
        try {
            if (value != null) {
                descrip = RMIObjectStreamClass.lookup(value.getClass());
            }
        } catch (InvalidClassException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Exception while looking up the class " + value.getClass().getName() + ".", ex);
            }
            throw new org.omg.CORBA.MARSHAL("Exception while looking up the class " + value.getClass().getName() + " (" + ex + ")");
        }
        if (descrip == null) {
            return value;
        }
        return writeReplaceExt(value, descrip);
    }

    public Serializable writeReplaceExt(Serializable value, RMIObjectStreamClass descrip) {
        if (getLogger() != null && getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeReplaceExt( " + value + ", " + descrip + " )");
        }
        try {
            Object rpl = descrip.writeReplace(value);
            if (value == rpl) {
                return value;
            }
            if (rpl instanceof Serializable) {
                return (Serializable) rpl;
            }
            throw new org.omg.CORBA.MARSHAL("Attempted to write-replace a non-serializable value");
        } catch (java.io.ObjectStreamException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Exception while writing to object stream.", ex);
            }
            throw new org.omg.CORBA.MARSHAL("Exception while writing to object stream (" + ex + ")");
        }
    }

    /**
     * Called by the CDROutputStream class if defaultWriteObject is called, or to write the
     * default data for a default serialization. Does not write value header.
     */
    void defaultWriteObject(OutputStream os, Serializable value, RMIObjectStreamClass osc) throws NotSerializableException {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("defaultWriteObject( " + os + ", " + value + ", " + osc + " )");
        }
        Field[] fields = osc.getSerializedFields();
        try {
            for (int i = 0; i < fields.length; ++i) {
                Class ft = fields[i].getType();
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("defaultWriteObject: writing field '" + fields[i].getName() + "' type=" + ft.getName());
                }
                if (ft.isPrimitive()) {
                    if (ft.equals(boolean.class)) {
                        os.write_boolean(fields[i].getBoolean(value));
                    } else if (ft.equals(byte.class)) {
                        os.write_octet(fields[i].getByte(value));
                    } else if (ft.equals(short.class)) {
                        os.write_short(fields[i].getShort(value));
                    } else if (ft.equals(int.class)) {
                        os.write_long(fields[i].getInt(value));
                    } else if (ft.equals(long.class)) {
                        os.write_longlong(fields[i].getLong(value));
                    } else if (ft.equals(float.class)) {
                        os.write_float(fields[i].getFloat(value));
                    } else if (ft.equals(double.class)) {
                        os.write_double(fields[i].getDouble(value));
                    } else if (ft.equals(char.class)) {
                        os.write_wchar(fields[i].getChar(value));
                    } else {
                        throw new Error("Unknown primitive type");
                    }
                } else {
                    Object fvalue = fields[i].get(value);
                    if (fvalue != null && !(fvalue instanceof Serializable)) {
                        throw new NotSerializableException(fvalue.getClass().getName());
                    }
                    writeStreamValue(os, ft, (Serializable) fvalue);
                }
            }
        } catch (IllegalAccessException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Illegal access exception.", ex);
            }
            throw new Error("Illegal access exception (" + ex + ")");
        } catch (IllegalArgumentException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Illegal argument exception.", ex);
            }
            throw new Error("Illegal argument exception (" + ex + ")");
        }
    }

    /**
     * Create put fields.
     */
    java.io.ObjectOutputStream.PutField putFields(Serializable value, RMIObjectStreamClass osc) throws java.io.IOException {
        return new PutFieldImpl(value, osc);
    }

    /**
     * Write the put fields created with putFields.
     */
    void writeFields(OutputStream os, Serializable value, java.io.ObjectOutputStream.PutField fields) throws java.io.IOException {
        ((PutFieldImpl) fields).write(os);
    }

    /**
     * PutField implementation class.
     * This class is used when members are serialized via the ObjectOutputStream.PutField
     * mechanism.
     */
    private class PutFieldImpl extends java.io.ObjectOutputStream.PutField {

        private final Map m_field_values = new HashMap();

        private final Map m_fields = new HashMap();

        private final RMIObjectStreamClass m_object_stream_class;

        /**
         * Constructor.
         */
        PutFieldImpl(Serializable value, RMIObjectStreamClass osc) {
            if (getLogger().isDebugEnabled() && Trace.isLow()) {
                getLogger().debug("PutFieldImpl( " + value + ", " + osc + " )");
            }
            m_object_stream_class = osc;
            ObjectStreamField[] fields = osc.getObjectStreamFields();
            for (int i = 0; i < fields.length; ++i) {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("PutFieldImpl: fields[ " + i + " ] = " + fields[i].getName());
                }
                m_fields.put(fields[i].getName(), fields[i]);
            }
        }

        private void checkFieldName(String name) {
            if (!m_fields.containsKey(name)) {
                throw new IllegalArgumentException("Field " + name + " is not a serial field");
            }
        }

        public void put(String name, boolean val) throws IllegalArgumentException {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, val ? Boolean.TRUE : Boolean.FALSE);
        }

        public void put(String name, byte val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getByte(val));
        }

        public void put(String name, char val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, CharacterCache.getCharacter(val));
        }

        public void put(String name, double val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getDouble(val));
        }

        public void put(String name, float val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getFloat(val));
        }

        public void put(String name, int val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getInteger(val));
        }

        public void put(String name, long val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getLong(val));
        }

        public void put(String name, Object val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, val);
        }

        public void put(String name, short val) {
            checkFieldName(name);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("PutFieldImpl.put( " + name + ", " + val + " )");
            }
            m_field_values.put(name, NumberCache.getShort(val));
        }

        public void write(java.io.ObjectOutput out) {
            throw new Error("Not implemented!");
        }

        public void write(OutputStream os) {
            if (getLogger().isDebugEnabled() && Trace.isLow()) {
                getLogger().debug("PutFieldImpl.write( " + os + " )");
            }
            ObjectStreamField[] fields = m_object_stream_class.getObjectStreamFields();
            for (int i = 0; i < fields.length; ++i) {
                ObjectStreamField field = fields[i];
                String fldname = field.getName();
                Object val = m_field_values.get(fldname);
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("PutFieldImpl.write: fields[" + i + "].getName()=" + fldname + ", value=" + val);
                }
                if (val != null) {
                    Class ft = field.getType();
                    if (ft.isPrimitive()) {
                        if (ft.equals(boolean.class)) {
                            os.write_boolean(((Boolean) val).booleanValue());
                        } else if (ft.equals(byte.class)) {
                            os.write_octet(((Byte) val).byteValue());
                        } else if (ft.equals(short.class)) {
                            os.write_short(((Short) val).shortValue());
                        } else if (ft.equals(int.class)) {
                            os.write_long(((Integer) val).intValue());
                        } else if (ft.equals(long.class)) {
                            os.write_longlong(((Long) val).longValue());
                        } else if (ft.equals(float.class)) {
                            os.write_float(((Float) val).floatValue());
                        } else if (ft.equals(double.class)) {
                            os.write_double(((Double) val).doubleValue());
                        } else if (ft.equals(char.class)) {
                            os.write_wchar(((Character) val).charValue());
                        } else {
                            throw new Error("Unknown primitive type");
                        }
                    } else {
                        writeStreamValue(os, ft, val);
                    }
                }
            }
        }
    }

    /**
     * Called by the CDROutputStream class if defaultReadObject is called, or to write the
     * default data for a default serialization, or if the custom serialization
     * requests it. Does not write value header.
     */
    void defaultReadObject(InputStream is, Serializable value, RMIObjectStreamClass osc) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("defaultReadObject( " + is + ", <value not yet deserialized>, " + osc + " )");
        }
        Field[] fields = osc.getSerializedFields();
        try {
            for (int i = 0; i < fields.length; ++i) {
                Class ft = fields[i].getType();
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("defaultReadObject: reading field '" + fields[i].getName() + "' type=" + ft.getName());
                }
                if (ft.isPrimitive()) {
                    if (ft.equals(boolean.class)) {
                        boolean v = is.read_boolean();
                        s_kernel.setBooleanField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(byte.class)) {
                        byte v = is.read_octet();
                        s_kernel.setByteField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(short.class)) {
                        short v = is.read_short();
                        s_kernel.setShortField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(int.class)) {
                        int v = is.read_long();
                        s_kernel.setIntField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(long.class)) {
                        long v = is.read_longlong();
                        s_kernel.setLongField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(float.class)) {
                        float v = is.read_float();
                        s_kernel.setFloatField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(double.class)) {
                        double v = is.read_double();
                        s_kernel.setDoubleField(osc.forClass(), fields[i].getName(), value, v);
                    } else if (ft.equals(char.class)) {
                        char v = is.read_wchar();
                        s_kernel.setCharField(osc.forClass(), fields[i].getName(), value, v);
                    } else {
                        throw new Error("Unknown primitive type");
                    }
                } else {
                    Object v = readStreamValue(is, ft);
                    if (v != null) {
                        s_kernel.setObjectField(osc.forClass(), fields[i].getName(), value, v);
                    }
                }
            }
        } catch (Exception ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Impossible exception occured.", ex);
            }
            throw new Error("Impossible exception occured (" + ex + ")");
        }
    }

    /**
     * Get the read fields.
     */
    java.io.ObjectInputStream.GetField readFields(InputStream is, Serializable value, RMIObjectStreamClass osc) {
        return new GetFieldImpl(osc, is);
    }

    private class GetFieldImpl extends java.io.ObjectInputStream.GetField {

        private final Map m_field_values = new HashMap();

        private final RMIObjectStreamClass m_object_stream_class;

        GetFieldImpl(RMIObjectStreamClass osc, InputStream is) {
            if (getLogger().isDebugEnabled() && Trace.isLow()) {
                getLogger().debug("GetFieldImpl( " + osc + ", " + is + " )");
            }
            m_object_stream_class = osc;
            ObjectStreamField[] fields = m_object_stream_class.getObjectStreamFields();
            try {
                for (int i = 0; i < fields.length; ++i) {
                    Class ft = fields[i].getType();
                    if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                        getLogger().debug("GetFieldImpl: field[ " + i + " ] = " + fields[i].getName() + ", type=" + ft.getName());
                    }
                    if (ft.isPrimitive()) {
                        if (ft.equals(boolean.class)) {
                            m_field_values.put(fields[i].getName(), (is.read_boolean() ? Boolean.TRUE : Boolean.FALSE));
                        } else if (ft.equals(byte.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getByte(is.read_octet()));
                        } else if (ft.equals(short.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getShort(is.read_short()));
                        } else if (ft.equals(int.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getInteger(is.read_long()));
                        } else if (ft.equals(long.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getLong(is.read_longlong()));
                        } else if (ft.equals(float.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getFloat(is.read_float()));
                        } else if (ft.equals(double.class)) {
                            m_field_values.put(fields[i].getName(), NumberCache.getDouble(is.read_double()));
                        } else if (ft.equals(char.class)) {
                            m_field_values.put(fields[i].getName(), CharacterCache.getCharacter(is.read_wchar()));
                        } else {
                            throw new Error("Unknown primitive type");
                        }
                    } else {
                        m_field_values.put(fields[i].getName(), readStreamValue(is, ft));
                    }
                }
            } catch (IllegalArgumentException ex) {
                if (getLogger() != null && getLogger().isErrorEnabled()) {
                    getLogger().error("Illegal argument exception.", ex);
                }
                throw new Error("Illegal argument exception (" + ex + ")");
            } catch (Exception ex) {
                if (getLogger() != null && getLogger().isErrorEnabled()) {
                    getLogger().error("Unknown exception!", ex);
                }
                throw new Error("Unknown exception (" + ex + ")");
            }
        }

        private void checkFieldName(String name) {
            if (!m_field_values.containsKey(name)) {
                throw new IllegalArgumentException("Field " + name + " is not a serial field.");
            }
        }

        public boolean defaulted(String str) throws IllegalArgumentException {
            checkFieldName(str);
            return false;
        }

        public boolean get(String str, boolean param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Boolean)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Boolean");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Boolean) val).booleanValue();
        }

        public byte get(String str, byte param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Byte)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Byte");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Byte) val).byteValue();
        }

        public short get(String str, short param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Short)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Short");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Short) val).shortValue();
        }

        public int get(String str, int param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Integer)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Integer");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Integer) val).intValue();
        }

        public long get(String str, long param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Long)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Long");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Long) val).longValue();
        }

        public float get(String str, float param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Float)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Float");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Float) val).floatValue();
        }

        public double get(String str, double param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Double)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Double");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Double) val).doubleValue();
        }

        public char get(String str, char param) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (!(val instanceof Character)) {
                throw new IllegalArgumentException("Value " + val + " is not of type Character");
            }
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return ((Character) val).charValue();
        }

        public Object get(String str, Object obj) throws IllegalArgumentException {
            checkFieldName(str);
            Object val = m_field_values.get(str);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("GetFieldImpl.get( " + str + ", " + val + " )");
            }
            return val;
        }

        public java.io.ObjectStreamClass getObjectStreamClass() {
            return m_object_stream_class.getObjectStreamClass();
        }
    }

    /**
     * Write class descriptor.
     */
    private void writeClassDesc(OutputStream os, Class clz) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeClassDesc( " + os + ", " + clz + " )");
        }
        os.write_value(Util.getCodebase(clz), s_string_helper);
        os.write_value(getRMIRepositoryID(clz), s_string_helper);
    }

    /**
     * Write class descriptor.
     */
    private Class readClassDesc(InputStream is) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("readClassDesc( " + is + " )");
        }
        String codebase = (String) is.read_value(s_string_helper);
        String repoID = (String) is.read_value(s_string_helper);
        if (repoID.equals(WStringValueHelper.id())) {
            return String.class;
        }
        if (repoID.startsWith("RMI:javax.rmi.CORBA.ClassDesc:")) {
            return Class.class;
        }
        String clzName = RepoIDHelper.unmangleRepoIDtoClassName(repoID);
        if (clzName == null) {
            throw new org.omg.CORBA.MARSHAL("Could not find classname from repository ID: \"" + repoID + "\"");
        }
        try {
            return Util.loadClass(clzName, codebase, (ClassLoader) null);
        } catch (ClassNotFoundException ex) {
            if (getLogger() != null && getLogger().isErrorEnabled()) {
                getLogger().error("Couldn't find class " + clzName + ".", ex);
            }
            throw new org.omg.CORBA.MARSHAL("Couldn't find class " + clzName + " (" + ex + ")");
        }
    }

    /**
     * Write array type.
     */
    private void writeArray(OutputStream os, Serializable value) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeArray( " + os + ", " + value + " )");
        }
        int len = Array.getLength(value);
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug("writeArray: writing array length = " + len);
        }
        os.write_long(len);
        if (len == 0) {
            return;
        }
        Class cmpt = value.getClass().getComponentType();
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug("writeArray: writing array of type '" + cmpt.getName() + "'");
        }
        if (cmpt.isPrimitive()) {
            if (cmpt.equals(boolean.class)) {
                os.write_boolean_array((boolean[]) value, 0, len);
            } else if (cmpt.equals(byte.class)) {
                os.write_octet_array((byte[]) value, 0, len);
            } else if (cmpt.equals(short.class)) {
                os.write_short_array((short[]) value, 0, len);
            } else if (cmpt.equals(int.class)) {
                os.write_long_array((int[]) value, 0, len);
            } else if (cmpt.equals(long.class)) {
                os.write_longlong_array((long[]) value, 0, len);
            } else if (cmpt.equals(float.class)) {
                os.write_float_array((float[]) value, 0, len);
            } else if (cmpt.equals(double.class)) {
                os.write_double_array((double[]) value, 0, len);
            } else if (cmpt.equals(char.class)) {
                os.write_wchar_array((char[]) value, 0, len);
            } else {
                throw new Error("Unknown primtive type");
            }
        } else {
            Object[] arr = (Object[]) value;
            for (int i = 0; i < len; ++i) {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("writeArray: array[ " + i + " ] = " + arr[i]);
                }
                writeStreamValue(os, cmpt, arr[i]);
            }
        }
    }

    /**
     * Read array type.
     */
    private Serializable readArray(InputStream is, int offset, Class clz) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("readArray( " + is + ", " + offset + ", " + clz + " )");
        }
        int len = is.read_long();
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug("writeArray: reading array of length = " + len);
        }
        Class cmpt = clz.getComponentType();
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug("writeArray: reading array of type '" + cmpt.getName() + "'");
        }
        if (cmpt.isPrimitive()) {
            if (cmpt.equals(boolean.class)) {
                boolean[] value = new boolean[len];
                is.read_boolean_array(value, 0, len);
                return value;
            } else if (cmpt.equals(byte.class)) {
                byte[] value = new byte[len];
                is.read_octet_array(value, 0, len);
                return value;
            } else if (cmpt.equals(short.class)) {
                short[] value = new short[len];
                is.read_short_array(value, 0, len);
                return value;
            } else if (cmpt.equals(int.class)) {
                int[] value = new int[len];
                is.read_long_array(value, 0, len);
                return value;
            } else if (cmpt.equals(long.class)) {
                long[] value = new long[len];
                is.read_longlong_array(value, 0, len);
                return value;
            } else if (cmpt.equals(float.class)) {
                float[] value = new float[len];
                is.read_float_array(value, 0, len);
                return value;
            } else if (cmpt.equals(double.class)) {
                double[] value = new double[len];
                is.read_double_array(value, 0, len);
                return value;
            } else if (cmpt.equals(char.class)) {
                char[] value = new char[len];
                is.read_wchar_array(value, 0, len);
                return value;
            } else {
                throw new Error("Unknown primitive type");
            }
        } else {
            Object[] value = (Object[]) Array.newInstance(cmpt, len);
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug("writeArray: array of type '" + cmpt.getName() + "' to be read");
            }
            boolean doPop = addReadIndirect(is, offset, value);
            for (int i = 0; i < len; ++i) {
                Object obj = readStreamValue(is, cmpt);
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug("writeArray: array[ " + i + " ] = " + obj);
                }
                value[i] = obj;
            }
            if (doPop) {
                popReadIndirect();
            }
            return value;
        }
    }

    /**
     * Write a declared member type to the stream. This will write the header.
     */
    private void writeStreamValue(OutputStream os, Class decl, Object value) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("writeStreamValue( " + os + ", " + decl + ", " + value + " )");
        }
        if (decl.isArray()) {
            os.write_value((Serializable) value);
        } else {
            java.lang.Object obj = value;
            if (!org.omg.CORBA.Object.class.isAssignableFrom(decl) && java.rmi.Remote.class.isAssignableFrom(decl) && obj != null) {
                obj = UtilDelegateImpl.exportRemote(os, (java.rmi.Remote) value);
            }
            os.write_abstract_interface(obj);
        }
    }

    /**
     * Reads a value from an InputStream.
     */
    private Object readStreamValue(InputStream is, Class decl) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("readStreamValue( " + is + ", " + decl + " )");
        }
        if (decl.isArray()) {
            return is.read_value(decl);
        }
        Object obj = is.read_abstract_interface(decl);
        if (obj != null && java.rmi.Remote.class.isAssignableFrom(decl)) {
            return PortableRemoteObjectDelegateImpl.narrowExt(obj, decl, false);
        }
        return obj;
    }

    /**
     * Check for read indirection. This is only neccicary on non-openorb
     * streams.
     */
    private Serializable checkReadIndirect(int offset) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("checkReadIndirect( " + offset + " )");
        }
        ReadIndirectTable tbl = (ReadIndirectTable) s_ri_local.get();
        if (tbl != null) {
            return (Serializable) tbl.getMap().get(NumberCache.getInteger(offset));
        }
        return null;
    }

    /**
     * Returns true if pop must be called.
     *
     * This one is called right after an instance is created using the internal
     * allocateNewObject() method. Thus the class' internal fields are not completely
     * set up. Trying to log this incomplete value (no deserialization has happened
     * yet) may lead to an NPE because the toString() method accesses internal members
     * that are null at this point in time (e.g. BigDecimal, the intValue, i.e. the
     * BigInteger, is still null, and trying to call an method like intValue.operation()
     * causes a NPE).
     */
    private boolean addReadIndirect(InputStream in, int offset, Serializable value) {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("addReadIndirect( " + in + ", " + offset + ", <value not yet deserialized> )");
        }
        if (in instanceof CDRInputStream) {
            ((CDRInputStream) in).addIndirect(offset, value);
            return false;
        } else {
            ReadIndirectTable tbl = (ReadIndirectTable) s_ri_local.get();
            if (tbl == null) {
                tbl = new ReadIndirectTable();
                s_ri_local.set(tbl);
            }
            tbl.incLevel();
            tbl.getMap().put(NumberCache.getInteger(offset), value);
            return true;
        }
    }

    private void popReadIndirect() {
        if (getLogger().isDebugEnabled() && Trace.isLow()) {
            getLogger().debug("popReadIndirect()");
        }
        ReadIndirectTable tbl = (ReadIndirectTable) s_ri_local.get();
        if (tbl.decLevel() == 0) {
            tbl.getMap().clear();
        }
    }
}
