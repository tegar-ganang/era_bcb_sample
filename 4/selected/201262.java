package org.apache.harmony.luni.tests.java.io;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

public class RandomAccessFileTest extends junit.framework.TestCase {

    public String fileName;

    public boolean ufile = true;

    java.io.RandomAccessFile raf;

    java.io.File f;

    String unihw = "Helࠁlo World";

    public String fileString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_java_io_BufferedOutputStream\nTest_java_io_ByteArrayInputStream\nTest_java_io_ByteArrayOutputStream\nTest_java_io_DataInputStream\nTest_java_io_File\nTest_java_io_FileDescriptor\nTest_java_io_FileInputStream\nTest_java_io_FileNotFoundException\nTest_java_io_FileOutputStream\nTest_java_io_FilterInputStream\nTest_java_io_FilterOutputStream\nTest_java_io_InputStream\nTest_java_io_IOException\nTest_java_io_OutputStream\nTest_java_io_PrintStream\nTest_RandomAccessFile\nTest_java_io_SyncFailedException\nTest_java_lang_AbstractMethodError\nTest_java_lang_ArithmeticException\nTest_java_lang_ArrayIndexOutOfBoundsException\nTest_java_lang_ArrayStoreException\nTest_java_lang_Boolean\nTest_java_lang_Byte\nTest_java_lang_Character\nTest_java_lang_Class\nTest_java_lang_ClassCastException\nTest_java_lang_ClassCircularityError\nTest_java_lang_ClassFormatError\nTest_java_lang_ClassLoader\nTest_java_lang_ClassNotFoundException\nTest_java_lang_CloneNotSupportedException\nTest_java_lang_Double\nTest_java_lang_Error\nTest_java_lang_Exception\nTest_java_lang_ExceptionInInitializerError\nTest_java_lang_Float\nTest_java_lang_IllegalAccessError\nTest_java_lang_IllegalAccessException\nTest_java_lang_IllegalArgumentException\nTest_java_lang_IllegalMonitorStateException\nTest_java_lang_IllegalThreadStateException\nTest_java_lang_IncompatibleClassChangeError\nTest_java_lang_IndexOutOfBoundsException\nTest_java_lang_InstantiationError\nTest_java_lang_InstantiationException\nTest_java_lang_Integer\nTest_java_lang_InternalError\nTest_java_lang_InterruptedException\nTest_java_lang_LinkageError\nTest_java_lang_Long\nTest_java_lang_Math\nTest_java_lang_NegativeArraySizeException\nTest_java_lang_NoClassDefFoundError\nTest_java_lang_NoSuchFieldError\nTest_java_lang_NoSuchMethodError\nTest_java_lang_NullPointerException\nTest_java_lang_Number\nTest_java_lang_NumberFormatException\nTest_java_lang_Object\nTest_java_lang_OutOfMemoryError\nTest_java_lang_RuntimeException\nTest_java_lang_SecurityManager\nTest_java_lang_Short\nTest_java_lang_StackOverflowError\nTest_java_lang_String\nTest_java_lang_StringBuffer\nTest_java_lang_StringIndexOutOfBoundsException\nTest_java_lang_System\nTest_java_lang_Thread\nTest_java_lang_ThreadDeath\nTest_java_lang_ThreadGroup\nTest_java_lang_Throwable\nTest_java_lang_UnknownError\nTest_java_lang_UnsatisfiedLinkError\nTest_java_lang_VerifyError\nTest_java_lang_VirtualMachineError\nTest_java_lang_vm_Image\nTest_java_lang_vm_MemorySegment\nTest_java_lang_vm_ROMStoreException\nTest_java_lang_vm_VM\nTest_java_lang_Void\nTest_java_net_BindException\nTest_java_net_ConnectException\nTest_java_net_DatagramPacket\nTest_java_net_DatagramSocket\nTest_java_net_DatagramSocketImpl\nTest_java_net_InetAddress\nTest_java_net_NoRouteToHostException\nTest_java_net_PlainDatagramSocketImpl\nTest_java_net_PlainSocketImpl\nTest_java_net_Socket\nTest_java_net_SocketException\nTest_java_net_SocketImpl\nTest_java_net_SocketInputStream\nTest_java_net_SocketOutputStream\nTest_java_net_UnknownHostException\nTest_java_util_ArrayEnumerator\nTest_java_util_Date\nTest_java_util_EventObject\nTest_java_util_HashEnumerator\nTest_java_util_Hashtable\nTest_java_util_Properties\nTest_java_util_ResourceBundle\nTest_java_util_tm\nTest_java_util_Vector\n";

    /**
     * @tests java.io.RandomAccessFile#RandomAccessFile(java.io.File,
     *        java.lang.String)
     */
    public void test_ConstructorLjava_io_FileLjava_lang_String() throws Exception {
        RandomAccessFile raf = new java.io.RandomAccessFile(f, "rw");
        raf.write(20);
        raf.seek(0);
        assertEquals("Incorrect int read/written", 20, raf.read());
        raf.close();
        raf = new java.io.RandomAccessFile(f, "rwd");
        raf.write(20);
        raf.seek(0);
        assertEquals("Incorrect int read/written", 20, raf.read());
        raf.close();
        raf = new java.io.RandomAccessFile(f, "rws");
        raf.write(20);
        raf.seek(0);
        assertEquals("Incorrect int read/written", 20, raf.read());
        raf.close();
        File f = File.createTempFile("xxx", "yyy");
        f.deleteOnExit();
        raf = new RandomAccessFile(f, "rws");
        raf.close();
        f = File.createTempFile("xxx", "yyy");
        f.deleteOnExit();
        raf = new RandomAccessFile(f, "rwd");
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#RandomAccessFile(java.lang.String,
     *        java.lang.String)
     */
    public void test_ConstructorLjava_lang_StringLjava_lang_String() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.write("Test".getBytes(), 0, 4);
        raf.close();
        raf = new java.io.RandomAccessFile(fileName, "rwd");
        raf.write("Test".getBytes(), 0, 4);
        raf.close();
        raf = new java.io.RandomAccessFile(fileName, "rws");
        raf.write("Test".getBytes(), 0, 4);
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#RandomAccessFile(java.lang.String,
     *        java.lang.String)
     */
    public void test_ConstructorLjava_lang_StringLjava_lang_String_I() throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile("", "r");
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
        } finally {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        }
        try {
            raf = new RandomAccessFile(new File(""), "r");
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
        } finally {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        }
        File tmpDir = new File("tmpDir");
        assertTrue(tmpDir.mkdir());
        tmpDir.deleteOnExit();
        try {
            raf = new RandomAccessFile(tmpDir.getPath(), "r");
            fail("should throw FileNotFoundException.");
        } catch (FileNotFoundException e) {
        } finally {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        }
    }

    /**
     * @tests java.io.RandomAccessFile#close()
     */
    public void test_close() {
        try {
            RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
            raf.close();
            raf.write("Test".getBytes(), 0, 4);
            fail("Failed to close file properly");
        } catch (IOException e) {
        }
    }

    /**
     * @tests java.io.RandomAccessFile#getFD()
     */
    public void test_getFD() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        assertTrue("Returned invalid fd", raf.getFD().valid());
        raf.close();
        assertFalse("Returned valid fd after close", raf.getFD().valid());
    }

    /**
     * @tests java.io.RandomAccessFile#getFilePointer()
     */
    public void test_getFilePointer() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.write(fileString.getBytes(), 0, 1000);
        assertEquals("Incorrect filePointer returned", 1000, raf.getFilePointer());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#length()
     */
    public void test_length() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.write(fileString.getBytes());
        assertEquals("Incorrect length returned", fileString.length(), raf.length());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#read()
     */
    public void test_read() throws IOException {
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes("UTF-8"), 0, fileString.length());
        fos.close();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        assertEquals("Incorrect bytes returned from read", fileString.charAt(0), raf.read());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[])
     */
    public void test_read$B() throws IOException {
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        byte[] rbuf = new byte[4000];
        raf.read(rbuf);
        assertEquals("Incorrect bytes returned from read", fileString, new String(rbuf, 0, fileString.length()));
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[], int, int)
     */
    public void test_read$BII() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        byte[] rbuf = new byte[4000];
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        raf.read(rbuf, 0, fileString.length());
        assertEquals("Incorrect bytes returned from read", fileString, new String(rbuf, 0, fileString.length()));
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readBoolean()
     */
    public void test_readBoolean() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBoolean(true);
        raf.seek(0);
        assertTrue("Incorrect boolean read/written", raf.readBoolean());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readByte()
     */
    public void test_readByte() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeByte(127);
        raf.seek(0);
        assertEquals("Incorrect bytes read/written", 127, raf.readByte());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readChar()
     */
    public void test_readChar() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeChar('T');
        raf.seek(0);
        assertEquals("Incorrect char read/written", 'T', raf.readChar());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readDouble()
     */
    public void test_readDouble() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeDouble(Double.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect double read/written", Double.MAX_VALUE, raf.readDouble(), 0);
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readFloat()
     */
    public void test_readFloat() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeFloat(Float.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect float read/written", Float.MAX_VALUE, raf.readFloat(), 0);
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readFully(byte[])
     */
    public void test_readFully$B() throws IOException {
        byte[] buf = new byte[10];
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBytes("HelloWorld");
        raf.seek(0);
        raf.readFully(buf);
        assertEquals("Incorrect bytes read/written", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readFully(byte[], int, int)
     */
    public void test_readFully$BII() throws IOException {
        byte[] buf = new byte[10];
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBytes("HelloWorld");
        raf.seek(0);
        raf.readFully(buf, 0, buf.length);
        assertEquals("Incorrect bytes read/written", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
        try {
            raf.readFully(buf, 0, buf.length);
            fail("Reading past end of buffer did not throw EOFException");
        } catch (EOFException e) {
        }
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readInt()
     */
    public void test_readInt() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeInt(Integer.MIN_VALUE);
        raf.seek(0);
        assertEquals("Incorrect int read/written", Integer.MIN_VALUE, raf.readInt());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readLine()
     */
    public void test_readLine() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        String s = "Goodbye\nCruel\nWorld\n";
        raf.write(s.getBytes("UTF-8"), 0, s.length());
        raf.seek(0);
        assertEquals("Goodbye", raf.readLine());
        assertEquals("Cruel", raf.readLine());
        assertEquals("World", raf.readLine());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readLong()
     */
    public void test_readLong() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeLong(Long.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect long read/written", Long.MAX_VALUE, raf.readLong());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readShort()
     */
    public void test_readShort() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeShort(Short.MIN_VALUE);
        raf.seek(0);
        assertEquals("Incorrect long read/written", Short.MIN_VALUE, raf.readShort());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readUnsignedByte()
     */
    public void test_readUnsignedByte() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeByte(-1);
        raf.seek(0);
        assertEquals("Incorrect byte read/written", 255, raf.readUnsignedByte());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readUnsignedShort()
     */
    public void test_readUnsignedShort() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeShort(-1);
        raf.seek(0);
        assertEquals("Incorrect byte read/written", 65535, raf.readUnsignedShort());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#readUTF()
     */
    public void test_readUTF() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeUTF(unihw);
        raf.seek(0);
        assertEquals("Incorrect utf string read", unihw, raf.readUTF());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#seek(long)
     */
    public void test_seekJ() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.write(fileString.getBytes(), 0, fileString.length());
        raf.seek(12);
        assertEquals("Seek failed to set filePointer", 12, raf.getFilePointer());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#skipBytes(int)
     */
    public void test_skipBytesI() throws IOException {
        byte[] buf = new byte[5];
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBytes("HelloWorld");
        raf.seek(0);
        raf.skipBytes(5);
        raf.readFully(buf);
        assertEquals("Failed to skip bytes", "World", new String(buf, 0, 5, "UTF-8"));
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#write(byte[])
     */
    public void test_write$B() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        byte[] nullByteArray = null;
        try {
            raf.write(nullByteArray);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        byte[] rbuf = new byte[4000];
        raf.write(fileString.getBytes());
        raf.close();
        try {
            raf.write(nullByteArray);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        raf.write(new byte[0]);
        try {
            raf.write(fileString.getBytes());
            fail("should throw IOException");
        } catch (IOException e) {
        }
        FileInputStream fis = new java.io.FileInputStream(fileName);
        fis.read(rbuf, 0, fileString.length());
        assertEquals("Incorrect bytes written", fileString, new String(rbuf, 0, fileString.length()));
        fis.close();
    }

    /**
     * @tests java.io.RandomAccessFile#write(byte[], int, int)
     */
    public void test_write$BII() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        byte[] rbuf = new byte[4000];
        raf.write(fileString.getBytes(), 0, fileString.length());
        raf.close();
        FileInputStream fis = new java.io.FileInputStream(fileName);
        fis.read(rbuf, 0, fileString.length());
        assertEquals("Incorrect bytes written", fileString, new String(rbuf, 0, fileString.length()));
        fis.close();
    }

    /**
     * @tests java.io.RandomAccessFile#write(byte[], int, int)
     */
    public void test_write_$BII_Exception() throws IOException {
        raf = new java.io.RandomAccessFile(f, "rw");
        byte[] nullByteArray = null;
        byte[] byteArray = new byte[10];
        try {
            raf.write(nullByteArray, -1, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(nullByteArray, 0, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(nullByteArray, 1, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(nullByteArray, 1, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(nullByteArray, 1, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(byteArray, -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(byteArray, -1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(byteArray, -1, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(byteArray, 0, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        raf.write(byteArray, 0, 0);
        raf.write(byteArray, 0, byteArray.length);
        raf.write(byteArray, 1, 0);
        raf.write(byteArray, byteArray.length, 0);
        try {
            raf.write(byteArray, byteArray.length + 1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(byteArray, byteArray.length + 1, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        raf.close();
        try {
            raf.write(nullByteArray, -1, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            raf.write(byteArray, -1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(byteArray, 0, 1);
            fail("should throw IOException");
        } catch (IOException e) {
        }
        try {
            raf.write(byteArray, 0, byteArray.length);
            fail("should throw IOException");
        } catch (IOException e) {
        }
        try {
            raf.write(byteArray, 1, 1);
            fail("should throw IOException");
        } catch (IOException e) {
        }
        try {
            raf.write(byteArray, byteArray.length + 1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
        raf.write(byteArray, 0, 0);
        raf.write(byteArray, byteArray.length, 0);
    }

    /**
     * @tests java.io.RandomAccessFile#write(int)
     */
    public void test_writeI() throws IOException {
        byte[] rbuf = new byte[4000];
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.write('t');
        raf.close();
        FileInputStream fis = new java.io.FileInputStream(fileName);
        fis.read(rbuf, 0, 1);
        assertEquals("Incorrect byte written", 't', rbuf[0]);
        fis.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeBoolean(boolean)
     */
    public void test_writeBooleanZ() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBoolean(true);
        raf.seek(0);
        assertTrue("Incorrect boolean read/written", raf.readBoolean());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeByte(int)
     */
    public void test_writeByteI() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeByte(127);
        raf.seek(0);
        assertEquals("Incorrect byte read/written", 127, raf.readByte());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeBytes(java.lang.String)
     */
    public void test_writeBytesLjava_lang_String() throws IOException {
        byte[] buf = new byte[10];
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeBytes("HelloWorld");
        raf.seek(0);
        raf.readFully(buf);
        assertEquals("Incorrect bytes read/written", "HelloWorld", new String(buf, 0, 10, "UTF-8"));
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeChar(int)
     */
    public void test_writeCharI() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeChar('T');
        raf.seek(0);
        assertEquals("Incorrect char read/written", 'T', raf.readChar());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeChars(java.lang.String)
     */
    public void test_writeCharsLjava_lang_String() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeChars("HelloWorld");
        char[] hchars = new char[10];
        "HelloWorld".getChars(0, 10, hchars, 0);
        raf.seek(0);
        for (int i = 0; i < hchars.length; i++) assertEquals("Incorrect string written", hchars[i], raf.readChar());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeDouble(double)
     */
    public void test_writeDoubleD() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeDouble(Double.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect double read/written", Double.MAX_VALUE, raf.readDouble(), 0);
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeFloat(float)
     */
    public void test_writeFloatF() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeFloat(Float.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect float read/written", Float.MAX_VALUE, raf.readFloat(), 0);
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeInt(int)
     */
    public void test_writeIntI() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeInt(Integer.MIN_VALUE);
        raf.seek(0);
        assertEquals("Incorrect int read/written", Integer.MIN_VALUE, raf.readInt());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeLong(long)
     */
    public void test_writeLongJ() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeLong(Long.MAX_VALUE);
        raf.seek(0);
        assertEquals("Incorrect long read/written", Long.MAX_VALUE, raf.readLong());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeShort(int)
     */
    public void test_writeShortI() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeShort(Short.MIN_VALUE);
        raf.seek(0);
        assertEquals("Incorrect long read/written", Short.MIN_VALUE, raf.readShort());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#writeUTF(java.lang.String)
     */
    public void test_writeUTFLjava_lang_String() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        raf.writeUTF(unihw);
        raf.seek(0);
        assertEquals("Incorrect utf string", unihw, raf.readUTF());
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#seek(long)
     * 
     * Regression for HARMONY-374
     */
    public void test_seekI() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        try {
            raf.seek(-1);
            fail("IOException must be thrown if pos < 0");
        } catch (IOException e) {
        }
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[], int, int)
     * 
     * Regression for HARMONY-377
     */
    public void test_readBII() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        try {
            raf.read(new byte[1], -1, 1);
            fail("IndexOutOfBoundsException must be thrown if off <0");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.read(new byte[1], 0, -1);
            fail("IndexOutOfBoundsException must be thrown if len <0");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.read(new byte[1], 0, 5);
            fail("IndexOutOfBoundsException must be thrown if off+len > b.length");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.read(new byte[10], Integer.MAX_VALUE, 5);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.read(new byte[10], 5, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
        }
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[],int,int) 
     */
    public void test_read_$BII_IndexOutOfBoundsException() throws IOException {
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        byte[] rbuf = new byte[100];
        raf.close();
        try {
            raf.read(rbuf, -1, 0);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[],int,int) 
     */
    public void test_read_$BII_IOException() throws IOException {
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        byte[] rbuf = new byte[100];
        raf.close();
        int read = raf.read(rbuf, 0, 0);
        assertEquals(0, read);
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[])
     */
    public void test_read_$B_IOException() throws IOException {
        FileOutputStream fos = new java.io.FileOutputStream(fileName);
        fos.write(fileString.getBytes(), 0, fileString.length());
        fos.close();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        byte[] rbuf = new byte[0];
        raf.close();
        int read = raf.read(rbuf);
        assertEquals(0, read);
    }

    /**
     * @tests java.io.RandomAccessFile#read(byte[],int,int) 
     */
    public void test_read_$BII_NullPointerException() throws IOException {
        File f = File.createTempFile("tmp", "tmp");
        f.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        byte[] rbuf = null;
        try {
            raf.read(rbuf, 0, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
        raf.close();
    }

    /**
     * @tests java.io.RandomAccessFile#write(byte[], int, int)
     * 
     * Regression for HARMONY-377
     */
    public void test_writeBII() throws IOException {
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "rw");
        try {
            raf.write(new byte[1], -1, 1);
            fail("IndexOutOfBoundsException must be thrown if off <0");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(new byte[1], 0, -1);
            fail("IndexOutOfBoundsException must be thrown if len <0");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(new byte[1], 0, 5);
            fail("IndexOutOfBoundsException must be thrown if off+len > b.length");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(new byte[10], Integer.MAX_VALUE, 5);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            raf.write(new byte[10], 5, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
        }
        raf.close();
    }

    /**
     * Regression for HARMONY-69
     */
    public void testRandomAccessFile_String_String() throws IOException {
        f.createNewFile();
        RandomAccessFile raf = new java.io.RandomAccessFile(fileName, "r");
        FileChannel fcr = raf.getChannel();
        try {
            fcr.lock(0L, Long.MAX_VALUE, false);
            fail("NonWritableChannelException expected!");
        } catch (NonWritableChannelException e) {
        }
        raf.close();
    }

    public void testRandomAccessFile_seekMoreThan2gb() throws IOException {
        if (File.separator != "/") {
            return;
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.write(1);
        raf.write(2);
        raf.seek(2147483647);
        raf.write(3);
        raf.write(4);
        raf.write(5);
        raf.write(6);
        raf.seek(0);
        assertEquals("seek 0", 1, raf.read());
        raf.seek(2147483649L);
        assertEquals("seek >2gb", 5, raf.read());
        raf.seek(0);
        assertEquals("seek back to 0", 1, raf.read());
        raf.close();
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() throws Exception {
        super.setUp();
        f = File.createTempFile("raf", "tst");
        if (!f.delete()) {
            fail("Unable to delete test file : " + f);
        }
        fileName = f.getAbsolutePath();
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        if (f.exists()) {
            f.delete();
        }
        super.tearDown();
    }
}
