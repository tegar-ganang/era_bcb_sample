package net.sf.kdgcommons.buffer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.Random;
import junit.framework.TestCase;
import net.sf.kdgcommons.io.IOUtil;

public class TestMappedFileBuffer extends TestCase {

    private File _testFile;

    @Override
    protected void setUp() throws IOException {
        _testFile = File.createTempFile("TestMappedFileBuffer", ".tmp");
        _testFile.deleteOnExit();
    }

    @Override
    protected void tearDown() throws IOException {
        _testFile.delete();
    }

    /**
     *  Writes a "walking byte" pattern into the test file. This will cause
     *  problems with any retrievals that don't look at the correct location.
     */
    private void writeDefaultContent(int length) throws Exception {
        FileOutputStream fos = new FileOutputStream(_testFile);
        try {
            BufferedOutputStream out = new BufferedOutputStream(fos);
            for (int ii = 0; ii < length; ii++) out.write(ii % 256);
            out.flush();
            out.close();
        } catch (Exception ex) {
            IOUtil.closeQuietly(fos);
            throw ex;
        }
    }

    private void writeExplicitContent(long offset, int... bytes) throws Exception {
        RandomAccessFile out = new RandomAccessFile(_testFile, "rwd");
        try {
            out.seek(offset);
            for (int b : bytes) out.write(b);
        } finally {
            IOUtil.closeQuietly(out);
        }
    }

    public void testSmallFileSingleSegmentReadWrite() throws Exception {
        writeDefaultContent(256);
        assertEquals(256L, _testFile.length());
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        assertEquals(256L, buf.capacity());
        assertEquals(0x01020304, buf.getInt(1));
        buf.put(0L, (byte) 0x12);
        assertEquals(0x12, buf.get(0L));
        buf.putShort(16L, (short) 0x1234);
        assertEquals(0x1234, buf.getShort(16L));
        buf.putInt(32L, 0x12345678);
        assertEquals(0x12345678, buf.getInt(32L));
        buf.putLong(48L, 0x8765432112345678L);
        assertEquals(0x8765432112345678L, buf.getLong(48L));
        buf.putFloat(64L, 1234.5f);
        assertEquals(1234.5f, buf.getFloat(64L), .01f);
        buf.putDouble(96L, 1234567890.125);
        assertEquals(1234567890.125, buf.getDouble(96L), .0001f);
        buf.putDouble(96L, 1234567890.125);
        assertEquals(1234567890.125, buf.getDouble(96L), .0001f);
        buf.putChar(112L, 'ģ');
        assertEquals('ģ', buf.getChar(112L));
    }

    public void testMediumFileMultipleSegments() throws Exception {
        writeExplicitContent(8192, 0x00);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        for (int ii = 0; ii < 8; ii++) {
            long offset = ii * 1024 + 257;
            assertEquals(0, buf.getInt(offset));
            buf.putInt(offset, 0x12345678);
            assertEquals(0x12345678, buf.getInt(offset));
        }
    }

    public void testSlice() throws Exception {
        writeExplicitContent(8191, 0x00);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        ByteBuffer slice = buf.slice(4094L);
        buf.putInt(4096L, 0x12345678);
        assertEquals(0x00001234, slice.getInt(0));
        slice.put(1023, (byte) 0x01);
        assertEquals(0x01, slice.get(1023));
        slice.put(1025, (byte) 0x01);
        assertEquals(0x01, slice.get(1025));
        try {
            slice.get(1026);
            fail("able to access outside file bounds");
        } catch (IndexOutOfBoundsException ee) {
        }
    }

    public void testSetOrder() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        buf.setByteOrder(ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.getByteOrder());
        buf.putInt(0, 0x12345678);
        buf.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buf.getByteOrder());
        assertEquals(0x78563412, buf.getInt(0));
    }

    public void testFailWriteToReadOnlyBuffer() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, false);
        buf.getInt(8000L);
        try {
            buf.putInt(8000L, 0x12345678);
            fail("able to write to read-only buffer");
        } catch (ReadOnlyBufferException ee) {
        }
    }

    public void testClone() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf1 = new MappedFileBuffer(_testFile, 1024, true);
        MappedFileBuffer buf2 = buf1.clone();
        buf1.putInt(3172, 0x12345678);
        assertEquals(0x12345678, buf2.getInt(3172));
        assertNotSame(buf1.buffer(1234), buf2.buffer(1234));
    }

    public void testGetFile() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        assertSame(_testFile, buf.file());
    }

    public void testIsWritable() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf1 = new MappedFileBuffer(_testFile, 1024, true);
        MappedFileBuffer buf2 = new MappedFileBuffer(_testFile, 1024, false);
        assertTrue(buf1.isWritable());
        assertFalse(buf2.isWritable());
    }

    public void testBulkOperations() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        byte[] a1 = buf.getBytes(1, 256);
        assertEquals(256, a1.length);
        assertEquals(1, a1[0]);
        assertEquals(100, a1[99]);
        assertEquals(254, a1[253] & 0xFF);
        byte[] a2 = buf.getBytes(1, 4099);
        assertEquals(4099, a2.length);
        assertEquals(1, a2[0]);
        assertEquals(100, a2[99]);
        assertEquals(254, a2[253] & 0xFF);
        assertEquals(1, a2[4096]);
        assertEquals(0, a2[1023]);
        assertEquals(1, a2[1024]);
        assertEquals(2, a2[1025]);
        assertEquals(0, a2[2047]);
        assertEquals(1, a2[2048]);
        assertEquals(2, a2[2049]);
        byte[] a3 = new byte[256];
        (new Random()).nextBytes(a3);
        buf.putBytes(256, a3);
        assertEquals(0xFF, buf.get(255) & 0xFF);
        assertEquals(0x00, buf.get(512) & 0xFF);
        for (int ii = 0; ii < a3.length; ii++) assertEquals("byte " + (256 + ii), a3[ii], buf.get(256 + ii));
        byte[] a4 = new byte[4096];
        (new Random()).nextBytes(a4);
        buf.putBytes(256, a4);
        assertEquals(0xFF, buf.get(255) & 0xFF);
        assertEquals(0x00, buf.get(4352) & 0xFF);
        for (int ii = 0; ii < a4.length; ii++) assertEquals("byte " + (256 + ii), a4[ii], buf.get(256 + ii));
    }

    public void testBulkOperationFailureAtEndOfFile() throws Exception {
        writeDefaultContent(8192);
        MappedFileBuffer buf = new MappedFileBuffer(_testFile, 1024, true);
        try {
            buf.getBytes(8000, 256);
            fail("able to retrieve past end of file");
        } catch (IndexOutOfBoundsException ex) {
        }
        try {
            buf.putBytes(8000, new byte[256]);
            fail("able to write past end of file");
        } catch (IndexOutOfBoundsException ex) {
        }
    }
}
