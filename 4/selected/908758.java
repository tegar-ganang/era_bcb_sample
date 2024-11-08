package org.apache.harmony.luni.tests.java.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.io.SerializablePermission;
import java.io.StreamCorruptedException;
import java.lang.reflect.Proxy;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import junit.framework.TestCase;
import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

@SuppressWarnings("serial")
public class ObjectInputStreamTest extends TestCase implements Serializable {

    ObjectInputStream ois;

    ObjectOutputStream oos;

    ByteArrayOutputStream bao;

    public class SerializableTestHelper implements Serializable {

        public String aField1;

        public String aField2;

        SerializableTestHelper() {
            aField1 = null;
            aField2 = null;
        }

        SerializableTestHelper(String s, String t) {
            aField1 = s;
            aField2 = t;
        }

        private void readObject(ObjectInputStream ois) throws Exception {
            ObjectInputStream.GetField fields = ois.readFields();
            aField1 = (String) fields.get("aField1", "Zap");
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            ObjectOutputStream.PutField fields = oos.putFields();
            fields.put("aField1", aField1);
            oos.writeFields();
        }

        public String getText1() {
            return aField1;
        }

        public void setText1(String s) {
            aField1 = s;
        }

        public String getText2() {
            return aField2;
        }

        public void setText2(String s) {
            aField2 = s;
        }
    }

    public static class A1 implements Serializable {

        private static final long serialVersionUID = 5942584913446079661L;

        B1 b1 = new B1();

        B1 b2 = b1;

        Vector v = new Vector();
    }

    public static class B1 implements Serializable {

        int i = 5;

        Hashtable h = new Hashtable();
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    public void test_readObjectMissingClasses() throws Exception {
        SerializationTest.verifySelf(new A1(), new SerializableAssert() {

            public void assertDeserialized(Serializable initial, Serializable deserialized) {
                assertEquals(5, ((A1) deserialized).b1.i);
            }
        });
    }

    /**
     * @tests java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    public void test_ConstructorLjava_io_InputStream() throws IOException {
        oos.writeDouble(Double.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.close();
        oos.close();
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(new byte[90]));
            fail("StreamCorruptedException expected");
        } catch (StreamCorruptedException e) {
        }
    }

    /**
     * @tests java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
     */
    public void test_ConstructorLjava_io_InputStream_subtest0() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            Permission golden = new SerializablePermission("enableSubclassImplementation");

            @Override
            public void checkPermission(Permission p) {
                if (golden.equals(p)) {
                    throw new SecurityException();
                }
            }
        });
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream obout = new ObjectOutputStream(out);
            obout.write(0);
            obout.close();
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            new ObjectInputStream(in);
            in.reset();
            new ObjectInputStream(in) {
            };
            in.reset();
            try {
                new ObjectInputStream(in) {

                    @Override
                    public Object readUnshared() throws IOException, ClassNotFoundException {
                        return null;
                    }
                };
                fail("should throw SecurityException 1");
            } catch (SecurityException e) {
            }
            in.reset();
            try {
                new ObjectInputStream(in) {

                    @Override
                    public GetField readFields() throws IOException, ClassNotFoundException, NotActiveException {
                        return null;
                    }
                };
                fail("should throw SecurityException 2");
            } catch (SecurityException e) {
            }
        } finally {
            System.setSecurityManager(sm);
        }
    }

    /**
     * @tests {@link java.io.ObjectInputStream#resolveProxyClass(String[])}
     */
    public void test_resolveProxyClass() throws IOException, ClassNotFoundException {
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        MockObjectInputStream mockIn = new MockObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        Class[] clazzs = { java.io.ObjectInputStream.class, java.io.Reader.class };
        for (int i = 0; i < clazzs.length; i++) {
            Class clazz = clazzs[i];
            Class[] interfaceNames = clazz.getInterfaces();
            String[] interfaces = new String[interfaceNames.length];
            int index = 0;
            for (Class c : interfaceNames) {
                interfaces[index] = c.getName();
                index++;
            }
            Class<?> s = mockIn.resolveProxyClass(interfaces);
            if (Proxy.isProxyClass(s)) {
                Class[] implementedInterfaces = s.getInterfaces();
                for (index = 0; index < implementedInterfaces.length; index++) {
                    assertEquals(interfaceNames[index], implementedInterfaces[index]);
                }
            } else {
                fail("Should return a proxy class that implements the interfaces named in a proxy class descriptor");
            }
        }
        mockIn.close();
    }

    class MockObjectInputStream extends ObjectInputStream {

        public MockObjectInputStream(InputStream input) throws StreamCorruptedException, IOException {
            super(input);
        }

        @Override
        public Class<?> resolveProxyClass(String[] interfaceNames) throws IOException, ClassNotFoundException {
            return super.resolveProxyClass(interfaceNames);
        }
    }

    /**
     * @tests java.io.ObjectInputStream#available()
     */
    public void test_available() throws IOException {
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect bytes", 10, ois.available());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#close()
     */
    public void test_close() throws IOException {
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#defaultReadObject()
     */
    public void test_defaultReadObject() throws Exception {
        String s = "HelloWorld";
        oos.writeObject(s);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        try {
            ois.defaultReadObject();
            fail("NotActiveException expected");
        } catch (NotActiveException e) {
        } finally {
            ois.close();
        }
    }

    /**
     * @tests java.io.ObjectInputStream#read()
     */
    public void test_read() throws IOException {
        oos.write('T');
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect byte value", 'T', ois.read());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#read(byte[], int, int)
     */
    public void test_read$BII() throws IOException {
        byte[] buf = new byte[10];
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.read(buf, 0, 10);
        ois.close();
        assertEquals("Read incorrect bytes", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
    }

    /**
     * @tests java.io.ObjectInputStream#readBoolean()
     */
    public void test_readBoolean() throws IOException {
        oos.writeBoolean(true);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect boolean value", ois.readBoolean());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readByte()
     */
    public void test_readByte() throws IOException {
        oos.writeByte(127);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect byte value", 127, ois.readByte());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readChar()
     */
    public void test_readChar() throws IOException {
        oos.writeChar('T');
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect char value", 'T', ois.readChar());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readDouble()
     */
    public void test_readDouble() throws IOException {
        oos.writeDouble(Double.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect double value", ois.readDouble() == Double.MAX_VALUE);
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readFields()
     */
    public void test_readFields() throws Exception {
        SerializableTestHelper sth;
        oos.writeObject(new SerializableTestHelper("Gabba", "Jabba"));
        oos.flush();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        sth = (SerializableTestHelper) (ois.readObject());
        assertEquals("readFields / writeFields failed--first field not set", "Gabba", sth.getText1());
        assertNull("readFields / writeFields failed--second field should not have been set", sth.getText2());
    }

    /**
     * @tests java.io.ObjectInputStream#readFloat()
     */
    public void test_readFloat() throws IOException {
        oos.writeFloat(Float.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect float value", ois.readFloat() == Float.MAX_VALUE);
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[])
     */
    public void test_readFully$B() throws IOException {
        byte[] buf = new byte[10];
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.readFully(buf);
        ois.close();
        assertEquals("Read incorrect bytes", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
    }

    /**
     * @tests java.io.ObjectInputStream#readFully(byte[], int, int)
     */
    public void test_readFully$BII() throws IOException {
        byte[] buf = new byte[10];
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.readFully(buf, 0, 10);
        ois.close();
        assertEquals("Read incorrect bytes", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
    }

    /**
     * @tests java.io.ObjectInputStream#readInt()
     */
    public void test_readInt() throws IOException {
        oos.writeInt(Integer.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect int value", ois.readInt() == Integer.MAX_VALUE);
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readLine()
     */
    @SuppressWarnings("deprecation")
    public void test_readLine() throws IOException {
        oos.writeBytes("HelloWorld\nSecondLine");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.readLine();
        assertEquals("Read incorrect string value", "SecondLine", ois.readLine());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readLong()
     */
    public void test_readLong() throws IOException {
        oos.writeLong(Long.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect long value", ois.readLong() == Long.MAX_VALUE);
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    public void test_readObject() throws Exception {
        String s = "HelloWorld";
        oos.writeObject(s);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect Object value", s, ois.readObject());
        ois.close();
        byte[] cName = C.class.getName().getBytes("UTF-8");
        byte[] aName = A.class.getName().getBytes("UTF-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] begStream = new byte[] { (byte) 0xac, (byte) 0xed, (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72, (byte) 0x00 };
        out.write(begStream, 0, begStream.length);
        out.write(cName.length);
        out.write(cName, 0, cName.length);
        byte[] midStream = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x21, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x72, (byte) 0x00 };
        out.write(midStream, 0, midStream.length);
        out.write(aName.length);
        out.write(aName, 0, aName.length);
        byte[] endStream = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0b, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x4c, (byte) 0x00, (byte) 0x04, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65, (byte) 0x74, (byte) 0x00, (byte) 0x12, (byte) 0x4c, (byte) 0x6a, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2f, (byte) 0x6c, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x2f, (byte) 0x53, (byte) 0x74, (byte) 0x72, (byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x3b, (byte) 0x78, (byte) 0x70, (byte) 0x74, (byte) 0x00, (byte) 0x04, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65 };
        out.write(endStream, 0, endStream.length);
        out.flush();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
        Object o = ois.readObject();
        assertEquals(C.class, o.getClass());
        assertNull(new ObjectInputStream() {
        }.readObject());
    }

    /**
     * @tests java.io.ObjectInputStream#readObjectOverride()
     */
    public void test_readObjectOverride() throws Exception {
        assertNull(new ObjectInputStream() {

            @Override
            public Object readObjectOverride() throws IOException, ClassNotFoundException {
                return super.readObjectOverride();
            }
        }.readObjectOverride());
    }

    public static class A implements Serializable {

        private static final long serialVersionUID = 11L;

        public String name = "name";
    }

    public static class B extends A {
    }

    public static class C extends B {

        private static final long serialVersionUID = 33L;
    }

    /**
     * @tests java.io.ObjectInputStream#readObject()
     */
    public void test_readObjectCorrupt() throws IOException, ClassNotFoundException {
        byte[] bytes = { 00, 00, 00, 0x64, 0x43, 0x48, (byte) 0xFD, 0x71, 00, 00, 0x0B, (byte) 0xB8, 0x4D, 0x65 };
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream in = new ObjectInputStream(bin);
            in.readObject();
            fail("Unexpected read of corrupted stream");
        } catch (StreamCorruptedException e) {
        }
    }

    /**
     * @tests java.io.ObjectInputStream#readShort()
     */
    public void test_readShort() throws IOException {
        oos.writeShort(Short.MAX_VALUE);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertTrue("Read incorrect short value", ois.readShort() == Short.MAX_VALUE);
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readUnsignedByte()
     */
    public void test_readUnsignedByte() throws IOException {
        oos.writeByte(-1);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect unsignedByte value", 255, ois.readUnsignedByte());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readUnsignedShort()
     */
    public void test_readUnsignedShort() throws IOException {
        oos.writeShort(-1);
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect unsignedShort value", 65535, ois.readUnsignedShort());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#readUTF()
     */
    public void test_readUTF() throws IOException {
        oos.writeUTF("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        assertEquals("Read incorrect utf value", "HelloWorld", ois.readUTF());
        ois.close();
    }

    /**
     * @tests java.io.ObjectInputStream#skipBytes(int)
     */
    public void test_skipBytesI() throws IOException {
        byte[] buf = new byte[10];
        oos.writeBytes("HelloWorld");
        oos.close();
        ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray()));
        ois.skipBytes(5);
        ois.read(buf, 0, 5);
        ois.close();
        assertEquals("Skipped incorrect bytes", "World", new String(buf, 0, 5, "UTF-8"));
        try {
            new ObjectInputStream() {
            }.skipBytes(0);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
        }
    }

    public void test_readObject_withPrimitiveClass() throws Exception {
        File file = new File("test.ser");
        file.deleteOnExit();
        Test test = new Test();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        out.writeObject(test);
        out.close();
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        Test another = (Test) in.readObject();
        in.close();
        assertEquals(test, another);
    }

    public static class ObjectOutputStreamWithWriteDesc extends ObjectOutputStream {

        public ObjectOutputStreamWithWriteDesc(OutputStream os) throws IOException {
            super(os);
        }

        @Override
        public void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        }
    }

    public static class ObjectIutputStreamWithReadDesc extends ObjectInputStream {

        private Class returnClass;

        public ObjectIutputStreamWithReadDesc(InputStream is, Class returnClass) throws IOException {
            super(is);
            this.returnClass = returnClass;
        }

        @Override
        public ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            return ObjectStreamClass.lookup(returnClass);
        }
    }

    static class TestClassForSerialization implements Serializable {

        private static final long serialVersionUID = 1L;
    }

    public void test_ClassDescriptor() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStreamWithWriteDesc oos = new ObjectOutputStreamWithWriteDesc(baos);
        oos.writeObject(String.class);
        oos.close();
        Class cls = TestClassForSerialization.class;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectIutputStreamWithReadDesc ois = new ObjectIutputStreamWithReadDesc(bais, cls);
        Object obj = ois.readObject();
        ois.close();
        assertEquals(cls, obj);
    }

    public static class ObjectOutputStreamWithWriteDesc1 extends ObjectOutputStream {

        public ObjectOutputStreamWithWriteDesc1(OutputStream os) throws IOException {
            super(os);
        }

        @Override
        public void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
            super.writeClassDescriptor(desc);
        }
    }

    public static class ObjectIutputStreamWithReadDesc1 extends ObjectInputStream {

        public ObjectIutputStreamWithReadDesc1(InputStream is) throws IOException {
            super(is);
        }

        @Override
        public ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            return super.readClassDescriptor();
        }
    }

    public static class ObjectInputStreamWithResolve extends ObjectInputStream {

        public ObjectInputStreamWithResolve(InputStream in) throws IOException {
            super(in);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (desc.getName().equals("org.apache.harmony.luni.tests.pkg1.TestClass")) {
                return org.apache.harmony.luni.tests.pkg2.TestClass.class;
            }
            return super.resolveClass(desc);
        }
    }

    public void test_resolveClass() throws Exception {
        org.apache.harmony.luni.tests.pkg1.TestClass to1 = new org.apache.harmony.luni.tests.pkg1.TestClass();
        to1.i = 555;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(to1);
        oos.flush();
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStreamWithResolve(bais);
        org.apache.harmony.luni.tests.pkg2.TestClass to2 = (org.apache.harmony.luni.tests.pkg2.TestClass) ois.readObject();
        if (to2.i != to1.i) {
            fail("Wrong object read. Expected val: " + to1.i + ", got: " + to2.i);
        }
    }

    static class ObjectInputStreamWithResolveObject extends ObjectInputStream {

        public static Integer intObj = Integer.valueOf(1000);

        public ObjectInputStreamWithResolveObject(InputStream in) throws IOException {
            super(in);
            enableResolveObject(true);
        }

        @Override
        protected Object resolveObject(Object obj) throws IOException {
            if (obj instanceof Integer) {
                obj = intObj;
            }
            return super.resolveObject(obj);
        }
    }

    /**
     * @tests java.io.ObjectInputStream#resolveObject(Object)
     */
    public void test_resolveObjectLjava_lang_Object() throws Exception {
        Integer original = new Integer(10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.flush();
        oos.close();
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStreamWithResolveObject ois = new ObjectInputStreamWithResolveObject(bais);
        Integer actual = (Integer) ois.readObject();
        ois.close();
        assertEquals(ObjectInputStreamWithResolveObject.intObj, actual);
    }

    public void test_readClassDescriptor() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStreamWithWriteDesc1 oos = new ObjectOutputStreamWithWriteDesc1(baos);
        ObjectStreamClass desc = ObjectStreamClass.lookup(TestClassForSerialization.class);
        oos.writeClassDescriptor(desc);
        oos.close();
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectIutputStreamWithReadDesc1 ois = new ObjectIutputStreamWithReadDesc1(bais);
        Object obj = ois.readClassDescriptor();
        ois.close();
        assertEquals(desc.getClass(), obj.getClass());
        bais = new ByteArrayInputStream(bytes);
        ExceptionalBufferedInputStream bis = new ExceptionalBufferedInputStream(bais);
        ois = new ObjectIutputStreamWithReadDesc1(bis);
        bis.setEOF(true);
        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
        } finally {
            ois.close();
        }
        bais = new ByteArrayInputStream(bytes);
        bis = new ExceptionalBufferedInputStream(bais);
        ois = new ObjectIutputStreamWithReadDesc1(bis);
        bis.setException(new IOException());
        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
        } finally {
            ois.close();
        }
        bais = new ByteArrayInputStream(bytes);
        bis = new ExceptionalBufferedInputStream(bais);
        ois = new ObjectIutputStreamWithReadDesc1(bis);
        bis.setCorrupt(true);
        try {
            obj = ois.readClassDescriptor();
        } catch (IOException e) {
        } finally {
            ois.close();
        }
    }

    static class ExceptionalBufferedInputStream extends BufferedInputStream {

        private boolean eof = false;

        private IOException exception = null;

        private boolean corrupt = false;

        public ExceptionalBufferedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            if (exception != null) {
                throw exception;
            }
            if (eof) {
                return -1;
            }
            if (corrupt) {
                return 0;
            }
            return super.read();
        }

        public void setEOF(boolean eof) {
            this.eof = eof;
        }

        public void setException(IOException exception) {
            this.exception = exception;
        }

        public void setCorrupt(boolean corrupt) {
            this.corrupt = corrupt;
        }
    }

    public static class ObjectIutputStreamWithReadDesc2 extends ObjectInputStream {

        private Class returnClass;

        public ObjectIutputStreamWithReadDesc2(InputStream is, Class returnClass) throws IOException {
            super(is);
            this.returnClass = returnClass;
        }

        @Override
        public ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass osc = super.readClassDescriptor();
            if (osc.getName().equals(returnClass.getName())) {
                return ObjectStreamClass.lookup(returnClass);
            }
            return osc;
        }
    }

    public void test_readClassDescriptor_1() throws IOException, ClassNotFoundException {
        A a = new A();
        a.name = "It's a test";
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        ObjectOutputStream out = new ObjectOutputStream(pout);
        ObjectInputStream in = new ObjectIutputStreamWithReadDesc2(pin, A.class);
        out.writeObject(a);
        A a1 = (A) in.readObject();
        assertEquals("Single case: incorrectly read the field of A", a.name, a1.name);
        HashMap m = new HashMap();
        a = new A();
        a.name = "It's a test 0";
        a1 = new A();
        a1.name = "It's a test 1";
        m.put("0", a);
        m.put("1", a1);
        out.writeObject(m);
        HashMap m1 = (HashMap) in.readObject();
        assertEquals("Incorrectly read the field of A", a.name, ((A) m1.get("0")).name);
        assertEquals("Incorrectly read the field of A1", a1.name, ((A) m1.get("1")).name);
    }

    public void test_registerValidation() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        try {
            ois.registerValidation(null, 256);
            fail("NotActiveException should be thrown");
        } catch (NotActiveException nae) {
        }
        baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new RegisterValidationClass());
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream fis = new ObjectInputStream(bais);
        fis.readObject();
    }

    private static class RegisterValidationClass implements Serializable {

        @SuppressWarnings("unused")
        private A a = new A();

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            stream.registerValidation(new MockObjectInputValidation(), 0);
        }
    }

    private static class MockObjectInputValidation implements ObjectInputValidation {

        public void validateObject() throws InvalidObjectException {
        }
    }

    public void test_readObject_array() throws Exception {
        final String resourcePrefix = ObjectInputStreamTest.class.getPackage().getName().replace('.', '/');
        ObjectInputStream oin = new ObjectInputStream(this.getClass().getClassLoader().getResourceAsStream("serialization/" + resourcePrefix + "/test_array_strings.ser"));
        TestArray testArray = (TestArray) oin.readObject();
        String[] strings = new String[] { "AAA", "BBB" };
        assertTrue(java.util.Arrays.equals(strings, testArray.array));
        oin = new ObjectInputStream(this.getClass().getClassLoader().getResourceAsStream("serialization/" + resourcePrefix + "/test_array_integers.ser"));
        testArray = (TestArray) oin.readObject();
        Integer[] integers = new Integer[] { 10, 20 };
        assertTrue(java.util.Arrays.equals(integers, testArray.array));
    }

    public static class TestExtObject implements Externalizable {

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(10);
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            in.readInt();
        }
    }

    static class TestObjectOutputStream extends ObjectOutputStream {

        private ObjectStreamClass[] objs;

        private int pos = 0;

        public TestObjectOutputStream(OutputStream out, ObjectStreamClass[] objs) throws IOException {
            super(out);
            this.objs = objs;
        }

        @Override
        protected void writeClassDescriptor(ObjectStreamClass osc) throws IOException {
            objs[pos++] = osc;
        }
    }

    static class TestObjectInputStream extends ObjectInputStream {

        private ObjectStreamClass[] objs;

        private int pos = 0;

        public TestObjectInputStream(InputStream in, ObjectStreamClass[] objs) throws IOException {
            super(in);
            this.objs = objs;
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            return objs[pos++];
        }
    }

    public void test_readObject_replacedClassDescriptor() throws Exception {
        ObjectStreamClass[] objs = new ObjectStreamClass[1000];
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        ObjectOutputStream oout = new TestObjectOutputStream(pout, objs);
        oout.writeObject(new TestExtObject());
        oout.writeObject("test");
        oout.close();
        ObjectInputStream oin = new TestObjectInputStream(pin, objs);
        oin.readObject();
        oin.readObject();
    }

    public void test_readObject_replacedClassField() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        FieldReplacementTestClass obj = new FieldReplacementTestClass(1234);
        oos.writeObject(obj);
        out.flush();
        out.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(in);
        try {
            FieldReplacementTestClass result = (FieldReplacementTestClass) ois.readObject();
            fail("should throw ClassCastException");
        } catch (ClassCastException e) {
        }
        ois.close();
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oos = new ObjectOutputStream(bao = new ByteArrayOutputStream());
    }

    public static class FieldReplacementTestClass implements Serializable {

        private FieldClass c;

        public FieldReplacementTestClass(int i) {
            super();
            c = new FieldClass(i);
        }
    }

    public static class FieldClass implements Serializable {

        private int i;

        public FieldClass(int i) {
            super();
            this.i = i;
        }

        protected Object writeReplace() throws ObjectStreamException {
            return new ReplacementFieldClass(i);
        }
    }

    public static class ReplacementFieldClass implements Serializable {

        private int i;

        public ReplacementFieldClass(int i) {
            super();
            this.i = i;
        }
    }
}

class TestArray implements Serializable {

    private static final long serialVersionUID = 1L;

    public Object[] array;

    public TestArray(Object[] array) {
        this.array = array;
    }
}

class Test implements Serializable {

    private static final long serialVersionUID = 1L;

    Class classes[] = new Class[] { byte.class, short.class, int.class, long.class, boolean.class, char.class, float.class, double.class, void.class };

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Test)) {
            return false;
        }
        return Arrays.equals(classes, ((Test) o).classes);
    }
}
