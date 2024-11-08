package com.spoledge.audao.test.gwt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Emulator of the GWT serialization interfaces.
 */
public class GWTSerializationEmulator {

    private WriterImpl writer;

    private ReaderImpl reader;

    private HashMap<Class<?>, Method[]> methods = new HashMap<Class<?>, Method[]>();

    @SuppressWarnings("unchecked")
    public <T> T passThrough(T dto) throws SerializationException {
        Method[] ms = getMethods(dto.getClass());
        try {
            ms[0].invoke(null, createWriter(), dto);
            Object o = ms[1].invoke(null, createReader());
            ms[2].invoke(null, getReader(), o);
            return (T) o;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof SerializationException) throw (SerializationException) t; else throw new RuntimeException(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SerializationStreamWriter createWriter() {
        writer = new WriterImpl();
        return writer;
    }

    public SerializationStreamReader createReader() {
        reader = new ReaderImpl(writer.getByteArray());
        return reader;
    }

    public SerializationStreamReader getReader() {
        return reader;
    }

    private Method[] getMethods(Class<?> clazz) {
        Method[] ret = methods.get(clazz);
        if (ret != null) return ret;
        try {
            Class<?> c2 = Class.forName(clazz.getName() + "_CustomFieldSerializer");
            ret = new Method[3];
            ret[0] = c2.getMethod("serialize", SerializationStreamWriter.class, clazz);
            ret[1] = c2.getMethod("instantiate", SerializationStreamReader.class);
            ret[2] = c2.getMethod("deserialize", SerializationStreamReader.class, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    private static class WriterImpl implements SerializationStreamWriter {

        ByteArrayOutputStream bos;

        ObjectOutputStream oos;

        WriterImpl() {
            try {
                bos = new ByteArrayOutputStream(4096);
                oos = new ObjectOutputStream(bos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        byte[] getByteArray() {
            try {
                oos.flush();
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void writeBoolean(boolean val) throws SerializationException {
            try {
                oos.writeBoolean(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeByte(byte val) throws SerializationException {
            try {
                oos.writeByte(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeChar(char val) throws SerializationException {
            try {
                oos.writeChar(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeShort(short val) throws SerializationException {
            try {
                oos.writeShort(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeInt(int val) throws SerializationException {
            try {
                oos.writeInt(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeLong(long val) throws SerializationException {
            try {
                oos.writeLong(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeFloat(float val) throws SerializationException {
            try {
                oos.writeFloat(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeDouble(double val) throws SerializationException {
            try {
                oos.writeDouble(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeString(String val) throws SerializationException {
            try {
                oos.writeUTF(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public void writeObject(Object val) throws SerializationException {
            try {
                oos.writeObject(val);
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static class ReaderImpl implements SerializationStreamReader {

        ObjectInputStream ois;

        ReaderImpl(byte[] arr) {
            try {
                ois = new ObjectInputStream(new ByteArrayInputStream(arr));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean readBoolean() throws SerializationException {
            try {
                return ois.readBoolean();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public byte readByte() throws SerializationException {
            try {
                return ois.readByte();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public char readChar() throws SerializationException {
            try {
                return ois.readChar();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public short readShort() throws SerializationException {
            try {
                return ois.readShort();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public int readInt() throws SerializationException {
            try {
                return ois.readInt();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public long readLong() throws SerializationException {
            try {
                return ois.readLong();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public float readFloat() throws SerializationException {
            try {
                return ois.readFloat();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public double readDouble() throws SerializationException {
            try {
                return ois.readDouble();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public String readString() throws SerializationException {
            try {
                return ois.readUTF();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
        }

        public Object readObject() throws SerializationException {
            try {
                return ois.readObject();
            } catch (Exception e) {
                throw new SerializationException(e);
            }
        }
    }
}
