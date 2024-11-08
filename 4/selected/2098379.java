package net.sf.kdgcommons.buffer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel.MapMode;
import junit.framework.TestCase;

public class TestBufferUtil extends TestCase {

    /**
     *  Generates a <code>byte[]</code> the is filled with a repeated sequence
     *  of 0x00 to 0xFF. This is typically used as a "background" array for
     *  tests.
     */
    private static byte[] newByteArray(int size) {
        byte[] data = new byte[size];
        for (int ii = 0; ii < data.length; ii++) data[ii] = (byte) (ii % 256);
        return data;
    }

    public void testMapFile() throws Exception {
        byte[] testData = newByteArray(1024);
        ByteBuffer localBuf = ByteBuffer.wrap(testData);
        File file = File.createTempFile("TestIOUtil", ".tmp");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        out.write(testData);
        out.close();
        ByteBuffer fileBuf1 = BufferUtil.map(file, 0, file.length(), MapMode.READ_ONLY);
        assertEquals(localBuf.limit(), fileBuf1.limit());
        assertEquals(localBuf.capacity(), fileBuf1.capacity());
        for (int ii = 0; ii < testData.length; ii++) assertEquals("byte " + ii, localBuf.get(ii), fileBuf1.get(ii));
        try {
            fileBuf1.putInt(0, 0xA5A55A5A);
            fail("able to write to read-only mapping");
        } catch (ReadOnlyBufferException ex) {
        }
        ByteBuffer fileBuf2 = BufferUtil.map(file, 128, 16, MapMode.READ_WRITE);
        assertEquals(localBuf.getLong(128), fileBuf2.getLong(0));
        ByteBuffer fileBuf3 = BufferUtil.map(file, 0, file.length(), MapMode.READ_WRITE);
        fileBuf3.putInt(16, 0xA5A55A5A);
        assertEquals(0xA5A55A5A, fileBuf1.getInt(16));
    }

    public void testGetUTF8String() throws Exception {
        String expected = "abç✧cd";
        byte[] expectedBytes = expected.getBytes("UTF-8");
        byte[] testData = newByteArray(128);
        System.arraycopy(expectedBytes, 0, testData, 10, expectedBytes.length);
        ByteBuffer buf = ByteBuffer.wrap(testData);
        assertEquals(expected, BufferUtil.getUTF8String(buf, 10, expectedBytes.length));
    }

    public void testGetChars() throws Exception {
        String expected = "abç✧cd";
        byte[] testData = newByteArray(128);
        ByteBuffer buf = ByteBuffer.wrap(testData);
        buf.position(10);
        for (int ii = 0; ii < expected.length(); ii++) buf.putChar(expected.charAt(ii));
        char[] chars = BufferUtil.getChars(buf, 10, expected.length());
        assertEquals(expected, new String(chars));
    }
}
