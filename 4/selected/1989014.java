package serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Creation date: Nov 12, 2007
 * Use case: 
 *
 * @author: cristian.rasch
 */
public class SerializationUtils {

    private static ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private static DataOutputStream dos = new DataOutputStream(baos);

    public static byte[] writeObject(Serializable deflatable) throws IOException {
        baos.reset();
        deflatable.serialize(dos);
        return baos.toByteArray();
    }

    public static byte[] writeObjectArray(Serializable[] serializables) throws IOException {
        baos.reset();
        dos.writeInt(serializables.length);
        for (int i = 0; i < serializables.length; i++) {
            byte[] data = writeObject(serializables[i]);
            dos.writeInt(data.length);
            dos.write(data);
        }
        return baos.toByteArray();
    }

    public static Serializable readObject(byte[] data, Class clazz) throws IOException {
        if (!Serializable.class.isAssignableFrom(clazz)) return null;
        Serializable deflatable = null;
        try {
            deflatable = (Serializable) clazz.newInstance();
        } catch (InstantiationException ie) {
            throw new IOException(ie.getMessage());
        } catch (IllegalAccessException iae) {
            throw new IOException(iae.getMessage());
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        deflatable.deserialize(dis);
        return deflatable;
    }

    public static Serializable readObject(byte[] data, Class clazz, int offset, int length) throws IOException {
        if (!Serializable.class.isAssignableFrom(clazz)) return null;
        Serializable deflatable = null;
        try {
            deflatable = (Serializable) clazz.newInstance();
        } catch (InstantiationException ie) {
            throw new IOException(ie.getMessage());
        } catch (IllegalAccessException iae) {
            throw new IOException(iae.getMessage());
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length);
        DataInputStream dis = new DataInputStream(bais);
        deflatable.deserialize(dis);
        return deflatable;
    }

    public static Serializable[] readObjectArray(byte[] data, Class clazz) throws IOException {
        if (!Serializable.class.isAssignableFrom(clazz)) return null;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        int size = dis.readInt();
        Serializable[] serializables = new Serializable[size];
        int objectSize, offset = 8;
        for (int i = 0; i < serializables.length; i++) {
            objectSize = dis.readInt();
            serializables[i] = SerializationUtils.readObject(data, clazz, offset, objectSize);
            offset += objectSize + 4;
            dis.skipBytes(objectSize);
        }
        return serializables;
    }

    public static byte[] serialize(Object param) throws IOException {
        baos.reset();
        if (param.getClass().isArray()) {
            Object[] arr = (Object[]) param;
            write(new Integer(arr.length), dos);
            for (int i = 0; i < arr.length; i++) write(arr[i], dos);
        } else if (param instanceof Vector) {
            Vector vector = (Vector) param;
            write(new Integer(vector.size()), dos);
            Enumeration elems = vector.elements();
            while (elems.hasMoreElements()) write(elems.nextElement(), dos);
        } else {
            write(param, dos);
        }
        return baos.toByteArray();
    }

    private static void write(Object obj, DataOutput out) throws IOException {
        if (obj instanceof Boolean) {
            out.writeBoolean(((Boolean) obj).booleanValue());
        } else if (obj instanceof Byte) {
            out.writeByte(((Byte) obj).byteValue());
        } else if (obj instanceof Character) {
            dos.writeChar(((Character) obj).charValue());
        } else if (obj instanceof Short) {
            out.writeShort(((Short) obj).shortValue());
        } else if (obj instanceof Integer) {
            out.writeInt(((Integer) obj).intValue());
        } else if (obj instanceof Long) {
            out.writeLong(((Long) obj).longValue());
        } else if (obj instanceof Float) {
            out.writeFloat(((Float) obj).floatValue());
        } else if (obj instanceof String) {
            out.writeUTF((String) obj);
        } else if (Serializable.class.isInstance(obj)) {
            out.write(writeObject((Serializable) (obj)));
        }
    }

    public static String pullString(InputStream is) throws IOException {
        baos.reset();
        int data;
        while ((data = is.read()) != -1) baos.write(data);
        return new String(baos.toByteArray());
    }

    public static String pullString(DataInputStream dis, int length) throws IOException {
        byte[] data = new byte[length];
        dis.readFully(data);
        return new String(data);
    }
}
