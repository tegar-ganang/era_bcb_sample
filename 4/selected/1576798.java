package com.dyuproject.protostuff.runtime;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import com.dyuproject.protostuff.ByteString;
import com.dyuproject.protostuff.Input;
import com.dyuproject.protostuff.Output;

/**
 * Base class for numeric id strategies.
 *
 * @author David Yu
 * @created Mar 28, 2012
 */
public abstract class NumericIdStrategy extends IdStrategy {

    protected static final int CID_BOOL = 0, CID_BYTE = 1, CID_CHAR = 2, CID_SHORT = 3, CID_INT32 = 4, CID_INT64 = 5, CID_FLOAT = 6, CID_DOUBLE = 7, CID_STRING = 16, CID_BYTES = 17, CID_BYTE_ARRAY = 18, CID_BIGDECIMAL = 19, CID_BIGINTEGER = 20, CID_DATE = 21, CID_OBJECT = 22, CID_ENUM_SET = 23, CID_ENUM_MAP = 24, CID_ENUM = 25, CID_COLLECTION = 26, CID_MAP = 27, CID_POJO = 28, CID_CLASS = 29, CID_DELEGATE = 30;

    protected void writeArrayIdTo(Output output, Class<?> componentType) throws IOException {
        assert !componentType.isArray();
        final RegisteredDelegate<?> rd = getRegisteredDelegate(componentType);
        if (rd != null) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, (rd.id << 5) | CID_DELEGATE, false);
            return;
        }
        final RuntimeFieldFactory<?> inline = RuntimeFieldFactory.getInline(componentType);
        if (inline != null) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, getPrimitiveOrScalarId(componentType, inline.id), false);
        } else if (componentType.isEnum()) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, getEnumId(componentType), false);
        } else if (Object.class == componentType) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, CID_OBJECT, false);
        } else if (Class.class == componentType) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, CID_CLASS, false);
        } else if (!componentType.isInterface() && !Modifier.isAbstract(componentType.getModifiers())) {
            output.writeUInt32(RuntimeFieldFactory.ID_ARRAY, getId(componentType), false);
        } else {
            output.writeString(RuntimeFieldFactory.ID_ARRAY_MAPPED, componentType.getName(), false);
        }
    }

    protected void transferArrayId(Input input, Output output, int fieldNumber, boolean mapped) throws IOException {
        if (mapped) input.transferByteRangeTo(output, true, fieldNumber, false); else output.writeUInt32(fieldNumber, input.readUInt32(), false);
    }

    protected Class<?> resolveArrayComponentTypeFrom(Input input, boolean mapped) throws IOException {
        return mapped ? RuntimeEnv.loadClass(input.readString()) : resolveClass(input.readUInt32());
    }

    protected void writeClassIdTo(Output output, Class<?> componentType, boolean array) throws IOException {
        assert !componentType.isArray();
        final int id = array ? RuntimeFieldFactory.ID_CLASS_ARRAY : RuntimeFieldFactory.ID_CLASS;
        final RegisteredDelegate<?> rd = getRegisteredDelegate(componentType);
        if (rd != null) {
            output.writeUInt32(id, (rd.id << 5) | CID_DELEGATE, false);
            return;
        }
        final RuntimeFieldFactory<?> inline = RuntimeFieldFactory.getInline(componentType);
        if (inline != null) {
            output.writeUInt32(id, getPrimitiveOrScalarId(componentType, inline.id), false);
        } else if (componentType.isEnum()) {
            output.writeUInt32(id, getEnumId(componentType), false);
        } else if (Object.class == componentType) {
            output.writeUInt32(id, CID_OBJECT, false);
        } else if (Class.class == componentType) {
            output.writeUInt32(id, CID_CLASS, false);
        } else if (!componentType.isInterface() && !Modifier.isAbstract(componentType.getModifiers())) {
            output.writeUInt32(id, getId(componentType), false);
        } else {
            output.writeString(id + 1, componentType.getName(), false);
        }
    }

    protected void transferClassId(Input input, Output output, int fieldNumber, boolean mapped, boolean array) throws IOException {
        if (mapped) input.transferByteRangeTo(output, true, fieldNumber, false); else output.writeUInt32(fieldNumber, input.readUInt32(), false);
    }

    protected Class<?> resolveClassFrom(Input input, boolean mapped, boolean array) throws IOException {
        return mapped ? RuntimeEnv.loadClass(input.readString()) : resolveClass(input.readUInt32());
    }

    private static int getPrimitiveOrScalarId(Class<?> clazz, int id) {
        if (clazz.isPrimitive()) return id - 1;
        return id < 9 ? ((id - 1) | 0x08) : (id + 7);
    }

    private Class<?> resolveClass(int id) {
        final int type = id & 0x1F;
        if (type < 16) {
            final boolean primitive = type < 8;
            switch(type & 0x07) {
                case CID_BOOL:
                    return primitive ? boolean.class : Boolean.class;
                case CID_BYTE:
                    return primitive ? byte.class : Byte.class;
                case CID_CHAR:
                    return primitive ? char.class : Character.class;
                case CID_SHORT:
                    return primitive ? short.class : Short.class;
                case CID_INT32:
                    return primitive ? int.class : Integer.class;
                case CID_INT64:
                    return primitive ? long.class : Long.class;
                case CID_FLOAT:
                    return primitive ? float.class : Float.class;
                case CID_DOUBLE:
                    return primitive ? double.class : Double.class;
                default:
                    throw new RuntimeException("Should not happen.");
            }
        }
        switch(type) {
            case CID_STRING:
                return String.class;
            case CID_BYTES:
                return ByteString.class;
            case CID_BYTE_ARRAY:
                return byte[].class;
            case CID_BIGDECIMAL:
                return BigDecimal.class;
            case CID_BIGINTEGER:
                return BigInteger.class;
            case CID_DATE:
                return Date.class;
            case CID_OBJECT:
                return Object.class;
            case CID_ENUM_SET:
                return EnumSet.class;
            case CID_ENUM_MAP:
                return EnumMap.class;
            case CID_ENUM:
                return enumClass(id >>> 5);
            case CID_COLLECTION:
                return collectionClass(id >>> 5);
            case CID_MAP:
                return mapClass(id >>> 5);
            case CID_POJO:
                return pojoClass(id >>> 5);
            case CID_CLASS:
                return Class.class;
            case CID_DELEGATE:
                return delegateClass(id >>> 5);
        }
        throw new RuntimeException("Should not happen.");
    }

    protected abstract RegisteredDelegate<?> getRegisteredDelegate(Class<?> clazz);

    protected abstract Class<?> enumClass(int id);

    protected abstract Class<?> delegateClass(int id);

    protected abstract Class<?> collectionClass(int id);

    protected abstract Class<?> mapClass(int id);

    protected abstract Class<?> pojoClass(int id);

    protected abstract int getEnumId(Class<?> clazz);

    protected abstract int getId(Class<?> clazz);

    protected static <T> ArrayList<T> newList(int size) {
        List<T> l = Collections.nCopies(size, null);
        return new ArrayList<T>(l);
    }

    protected static <T> void grow(ArrayList<T> list, int size) {
        int previousSize = list.size();
        list.ensureCapacity(size);
        List<T> l = Collections.nCopies(size - previousSize, null);
        list.addAll(l);
    }

    protected static final class RegisteredDelegate<T> {

        public final int id;

        public final Delegate<T> delegate;

        RegisteredDelegate(int id, Delegate<T> delegate) {
            this.id = id;
            this.delegate = delegate;
        }
    }
}
