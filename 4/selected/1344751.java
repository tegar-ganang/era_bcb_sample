package android.os;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MemoryFileTest extends AndroidTestCase {

    private void compareBuffers(byte[] buffer1, byte[] buffer2, int length) throws Exception {
        for (int i = 0; i < length; i++) {
            if (buffer1[i] != buffer2[i]) {
                throw new Exception("readBytes did not read back what writeBytes wrote");
            }
        }
    }

    public void testPurge() throws Exception {
        List<MemoryFile> files = new ArrayList<MemoryFile>();
        try {
            while (true) {
                MemoryFile newFile = new MemoryFile("MemoryFileTest", 10000000);
                newFile.allowPurging(true);
                newFile.writeBytes(testString, 0, 0, testString.length);
                files.add(newFile);
                for (MemoryFile file : files) {
                    try {
                        file.readBytes(testString, 0, 0, testString.length);
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        } finally {
            for (MemoryFile fileToClose : files) {
                fileToClose.close();
            }
        }
    }

    @SmallTest
    public void testRun() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        byte[] buffer = new byte[testString.length];
        file.writeBytes(testString, 0, 2000, testString.length);
        file.readBytes(buffer, 2000, 0, testString.length);
        compareBuffers(testString, buffer, testString.length);
        buffer = new byte[testString.length];
        OutputStream os = file.getOutputStream();
        os.write(testString);
        InputStream is = file.getInputStream();
        is.mark(testString.length);
        is.read(buffer);
        compareBuffers(testString, buffer, testString.length);
        buffer = new byte[testString.length];
        is.reset();
        is.read(buffer);
        compareBuffers(testString, buffer, testString.length);
        file.close();
    }

    private void readIndexOutOfBoundsException(int offset, int count, String msg) throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", testString.length);
        try {
            file.writeBytes(testString, 0, 0, testString.length);
            InputStream is = file.getInputStream();
            byte[] buffer = new byte[testString.length + 10];
            try {
                is.read(buffer, offset, count);
                fail(msg);
            } catch (IndexOutOfBoundsException ex) {
            } finally {
                is.close();
            }
        } finally {
            file.close();
        }
    }

    @SmallTest
    public void testReadNegativeOffset() throws Exception {
        readIndexOutOfBoundsException(-1, 5, "read() with negative offset should throw IndexOutOfBoundsException");
    }

    @SmallTest
    public void testReadNegativeCount() throws Exception {
        readIndexOutOfBoundsException(5, -1, "read() with negative length should throw IndexOutOfBoundsException");
    }

    @SmallTest
    public void testReadOffsetOverflow() throws Exception {
        readIndexOutOfBoundsException(testString.length + 10, 5, "read() with offset outside buffer should throw IndexOutOfBoundsException");
    }

    @SmallTest
    public void testReadOffsetCountOverflow() throws Exception {
        readIndexOutOfBoundsException(testString.length, 11, "read() with offset + count outside buffer should throw IndexOutOfBoundsException");
    }

    @SmallTest
    public void testReadEOF() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", testString.length);
        try {
            file.writeBytes(testString, 0, 0, testString.length);
            InputStream is = file.getInputStream();
            try {
                byte[] buffer = new byte[testString.length + 10];
                assertEquals(testString.length, is.read(buffer));
                compareBuffers(testString, buffer, testString.length);
                assertEquals(-1, is.read());
            } finally {
                is.close();
            }
        } finally {
            file.close();
        }
    }

    @SmallTest
    public void testCloseClose() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        byte[] data = new byte[512];
        file.writeBytes(data, 0, 0, 128);
        file.close();
        file.close();
    }

    @SmallTest
    public void testCloseRead() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        file.close();
        try {
            byte[] data = new byte[512];
            assertEquals(128, file.readBytes(data, 0, 0, 128));
            fail("readBytes() after close() did not throw IOException.");
        } catch (IOException e) {
        }
    }

    @SmallTest
    public void testCloseWrite() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        file.close();
        try {
            byte[] data = new byte[512];
            file.writeBytes(data, 0, 0, 128);
            fail("writeBytes() after close() did not throw IOException.");
        } catch (IOException e) {
        }
    }

    @SmallTest
    public void testCloseAllowPurging() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        byte[] data = new byte[512];
        file.writeBytes(data, 0, 0, 128);
        file.close();
        try {
            file.allowPurging(true);
            fail("allowPurging() after close() did not throw IOException.");
        } catch (IOException e) {
        }
    }

    @LargeTest
    public void testCloseLeak() throws Exception {
        for (int i = 0; i < 1025; i++) {
            MemoryFile file = new MemoryFile("MemoryFileTest", 5000000);
            file.writeBytes(testString, 0, 0, testString.length);
            file.close();
        }
    }

    @SmallTest
    public void testIsMemoryFile() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        FileDescriptor fd = file.getFileDescriptor();
        assertNotNull(fd);
        assertTrue(fd.valid());
        assertTrue(MemoryFile.isMemoryFile(fd));
        file.close();
        assertFalse(MemoryFile.isMemoryFile(FileDescriptor.in));
        assertFalse(MemoryFile.isMemoryFile(FileDescriptor.out));
        assertFalse(MemoryFile.isMemoryFile(FileDescriptor.err));
        File tempFile = File.createTempFile("MemoryFileTest", ".tmp", getContext().getFilesDir());
        assertNotNull(file);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
            FileDescriptor fileFd = out.getFD();
            assertNotNull(fileFd);
            assertFalse(MemoryFile.isMemoryFile(fileFd));
        } finally {
            if (out != null) {
                out.close();
            }
            tempFile.delete();
        }
    }

    @SmallTest
    public void testFileDescriptor() throws Exception {
        MemoryFile file = new MemoryFile("MemoryFileTest", 1000000);
        MemoryFile ref = new MemoryFile(file.getFileDescriptor(), file.length(), "r");
        byte[] buffer;
        file.writeBytes(testString, 0, 2000, testString.length);
        buffer = new byte[testString.length];
        ref.readBytes(buffer, 2000, 0, testString.length);
        compareBuffers(testString, buffer, testString.length);
        file.close();
        ref.close();
    }

    private static final byte[] testString = new byte[] { 3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7, 9, 3, 2, 3, 8, 4, 6, 2, 6, 4, 3, 3, 8, 3, 2, 7, 9, 5, 0, 2, 8, 8, 4, 1, 9, 7, 1, 6, 9, 3, 9, 9, 3, 7, 5, 1, 0, 5, 8, 2, 0, 9, 7, 4, 9, 4, 4, 5, 9, 2, 3, 0, 7, 8, 1, 6, 4, 0, 6, 2, 8, 6, 2, 0, 8, 9, 9, 8, 6, 2, 8, 0, 3, 4, 8, 2, 5, 3, 4, 2, 1, 1, 7, 0, 6, 7, 9, 8, 2, 1, 4, 8, 0, 8, 6, 5, 1, 3, 2, 8, 2, 3, 0, 6, 6, 4, 7, 0, 9, 3, 8, 4, 4, 6, 0, 9, 5, 5, 0, 5, 8, 2, 2, 3, 1, 7, 2, 5, 3, 5, 9, 4, 0, 8, 1, 2, 8, 4, 8, 1, 1, 1, 7, 4, 5, 0, 2, 8, 4, 1, 0, 2, 7, 0, 1, 9, 3, 8, 5, 2, 1, 1, 0, 5, 5, 5, 9, 6, 4, 4, 6, 2, 2, 9, 4, 8, 9, 5, 4, 9, 3, 0, 3, 8, 1, 9, 6, 4, 4, 2, 8, 8, 1, 0, 9, 7, 5, 6, 6, 5, 9, 3, 3, 4, 4, 6, 1, 2, 8, 4, 7, 5, 6, 4, 8, 2, 3, 3, 7, 8, 6, 7, 8, 3, 1, 6, 5, 2, 7, 1, 2, 0, 1, 9, 0, 9, 1, 4, 5, 6, 4, 8, 5, 6, 6, 9, 2, 3, 4, 6, 0, 3, 4, 8, 6, 1, 0, 4, 5, 4, 3, 2, 6, 6, 4, 8, 2, 1, 3, 3, 9, 3, 6, 0, 7, 2, 6, 0, 2, 4, 9, 1, 4, 1, 2, 7, 3, 7, 2, 4, 5, 8, 7, 0, 0, 6, 6, 0, 6, 3, 1, 5, 5, 8, 8, 1, 7, 4, 8, 8, 1, 5, 2, 0, 9, 2, 0, 9, 6, 2, 8, 2, 9, 2, 5, 4, 0, 9, 1, 7, 1, 5, 3, 6, 4, 3, 6, 7, 8, 9, 2, 5, 9, 0, 3, 6, 0, 0, 1, 1, 3, 3, 0, 5, 3, 0, 5, 4, 8, 8, 2, 0, 4, 6, 6, 5, 2, 1, 3, 8, 4, 1, 4, 6, 9, 5, 1, 9, 4, 1, 5, 1, 1, 6, 0, 9, 4, 3, 3, 0, 5, 7, 2, 7, 0, 3, 6, 5, 7, 5, 9, 5, 9, 1, 9, 5, 3, 0, 9, 2, 1, 8, 6, 1, 1, 7, 3, 8, 1, 9, 3, 2, 6, 1, 1, 7, 9, 3, 1, 0, 5, 1, 1, 8, 5, 4, 8, 0, 7, 4, 4, 6, 2, 3, 7, 9, 9, 6, 2, 7, 4, 9, 5, 6, 7, 3, 5, 1, 8, 8, 5, 7, 5, 2, 7, 2, 4, 8, 9, 1, 2, 2, 7, 9, 3, 8, 1, 8, 3, 0, 1, 1, 9, 4, 9, 1, 2, 9, 8, 3, 3, 6, 7, 3, 3, 6, 2, 4, 4, 0, 6, 5, 6, 6, 4, 3, 0, 8, 6, 0, 2, 1, 3, 9, 4, 9, 4, 6, 3, 9, 5, 2, 2, 4, 7, 3, 7, 1, 9, 0, 7, 0, 2, 1, 7, 9, 8, 6, 0, 9, 4, 3, 7, 0, 2, 7, 7, 0, 5, 3, 9, 2, 1, 7, 1, 7, 6, 2, 9, 3, 1, 7, 6, 7, 5, 2, 3, 8, 4, 6, 7, 4, 8, 1, 8, 4, 6, 7, 6, 6, 9, 4, 0, 5, 1, 3, 2, 0, 0, 0, 5, 6, 8, 1, 2, 7, 1, 4, 5, 2, 6, 3, 5, 6, 0, 8, 2, 7, 7, 8, 5, 7, 7, 1, 3, 4, 2, 7, 5, 7, 7, 8, 9, 6, 0, 9, 1, 7, 3, 6, 3, 7, 1, 7, 8, 7, 2, 1, 4, 6, 8, 4, 4, 0, 9, 0, 1, 2, 2, 4, 9, 5, 3, 4, 3, 0, 1, 4, 6, 5, 4, 9, 5, 8, 5, 3, 7, 1, 0, 5, 0, 7, 9, 2, 2, 7, 9, 6, 8, 9, 2, 5, 8, 9, 2, 3, 5, 4, 2, 0, 1, 9, 9, 5, 6, 1, 1, 2, 1, 2, 9, 0, 2, 1, 9, 6, 0, 8, 6, 4, 0, 3, 4, 4, 1, 8, 1, 5, 9, 8, 1, 3, 6, 2, 9, 7, 7, 4, 7, 7, 1, 3, 0, 9, 9, 6, 0, 5, 1, 8, 7, 0, 7, 2, 1, 1, 3, 4, 9, 9, 9, 9, 9, 9, 8, 3, 7, 2, 9, 7, 8, 0, 4, 9, 9, 5, 1, 0, 5, 9, 7, 3, 1, 7, 3, 2, 8, 1, 6, 0, 9, 6, 3, 1, 8, 5, 9, 5, 0, 2, 4, 4, 5, 9, 4, 5, 5, 3, 4, 6, 9, 0, 8, 3, 0, 2, 6, 4, 2, 5, 2, 2, 3, 0, 8, 2, 5, 3, 3, 4, 4, 6, 8, 5, 0, 3, 5, 2, 6, 1, 9, 3, 1, 1, 8, 8, 1, 7, 1, 0, 1, 0, 0, 0, 3, 1, 3, 7, 8, 3, 8, 7, 5, 2, 8, 8, 6, 5, 8, 7, 5, 3, 3, 2, 0, 8, 3, 8, 1, 4, 2, 0, 6, 1, 7, 1, 7, 7, 6, 6, 9, 1, 4, 7, 3, 0, 3, 5, 9, 8, 2, 5, 3, 4, 9, 0, 4, 2, 8, 7, 5, 5, 4, 6, 8, 7, 3, 1, 1, 5, 9, 5, 6, 2, 8, 6, 3, 8, 8, 2, 3, 5, 3, 7, 8, 7, 5, 9, 3, 7, 5, 1, 9, 5, 7, 7, 8, 1, 8, 5, 7, 7, 8, 0, 5, 3, 2, 1, 7, 1, 2, 2, 6, 8, 0, 6, 6, 1, 3, 0, 0, 1, 9, 2, 7, 8, 7, 6, 6, 1, 1, 1, 9, 5, 9, 0, 9, 2, 1, 6, 4, 2, 0, 1, 9, 8, 9, 3, 8, 0, 9, 5, 2, 5, 7, 2, 0, 1, 0, 6, 5, 4, 8, 5, 8, 6, 3, 2, 7, 8, 8, 6, 5, 9, 3, 6, 1, 5, 3, 3, 8, 1, 8, 2, 7, 9, 6, 8, 2, 3, 0, 3, 0, 1, 9, 5, 2, 0, 3, 5, 3, 0, 1, 8, 5, 2, 9, 6, 8, 9, 9, 5, 7, 7, 3, 6, 2, 2, 5, 9, 9, 4, 1, 3, 8, 9, 1, 2, 4, 9, 7, 2, 1, 7, 7, 5, 2, 8, 3, 4, 7, 9, 1, 3, 1, 5, 1, 5, 5, 7, 4, 8, 5, 7, 2, 4, 2, 4, 5, 4, 1, 5, 0, 6, 9, 5, 9, 5, 0, 8, 2, 9, 5, 3, 3, 1, 1, 6, 8, 6, 1, 7, 2, 7, 8, 5, 5, 8, 8, 9, 0, 7, 5, 0, 9, 8, 3, 8, 1, 7, 5, 4, 6, 3, 7, 4, 6, 4, 9, 3, 9, 3, 1, 9, 2, 5, 5, 0, 6, 0, 4, 0, 0, 9, 2, 7, 7, 0, 1, 6, 7, 1, 1, 3, 9, 0, 0, 9, 8, 4, 8, 8, 2, 4, 0, 1, 2, 8, 5, 8, 3, 6, 1, 6, 0, 3, 5, 6, 3, 7, 0, 7, 6, 6, 0, 1, 0, 4, 7, 1, 0, 1, 8, 1, 9, 4, 2, 9, 5, 5, 5, 9, 6, 1, 9, 8, 9, 4, 6, 7, 6, 7, 8, 3, 7, 4, 4, 9, 4, 4, 8, 2, 5, 5, 3, 7, 9, 7, 7, 4, 7, 2, 6, 8, 4, 7, 1, 0, 4, 0, 4, 7, 5, 3, 4, 6, 4, 6, 2, 0, 8, 0, 4, 6, 6, 8, 4, 2, 5, 9, 0, 6, 9, 4, 9, 1, 2, 9, 3, 3, 1, 3, 6, 7, 7, 0, 2, 8, 9, 8, 9, 1, 5, 2, 1, 0, 4, 7, 5, 2, 1, 6, 2, 0, 5, 6, 9, 6, 6, 0, 2, 4, 0, 5, 8, 0, 3, 8, 1, 5, 0, 1, 9, 3, 5, 1, 1, 2, 5, 3, 3, 8, 2, 4, 3, 0, 0, 3, 5, 5, 8, 7, 6, 4, 0, 2, 4, 7, 4, 9, 6, 4, 7, 3, 2, 6, 3, 9, 1, 4, 1, 9, 9, 2, 7, 2, 6, 0, 4, 2, 6, 9, 9, 2, 2, 7, 9, 6, 7, 8, 2, 3, 5, 4, 7, 8, 1, 6, 3, 6, 0, 0, 9, 3, 4, 1, 7, 2, 1, 6, 4, 1, 2, 1, 9, 9, 2, 4, 5, 8, 6, 3, 1, 5, 0, 3, 0, 2, 8, 6, 1, 8, 2, 9, 7, 4, 5, 5, 5, 7, 0, 6, 7, 4, 9, 8, 3, 8, 5, 0, 5, 4, 9, 4, 5, 8, 8, 5, 8, 6, 9, 2, 6, 9, 9, 5, 6, 9, 0, 9, 2, 7, 2, 1, 0, 7, 9, 7, 5, 0, 9, 3, 0, 2, 9, 5, 5, 3, 2, 1, 1, 6, 5, 3, 4, 4, 9, 8, 7, 2, 0, 2, 7, 5, 5, 9, 6, 0, 2, 3, 6, 4, 8, 0, 6, 6, 5, 4, 9, 9, 1, 1, 9, 8, 8, 1, 8, 3, 4, 7, 9, 7, 7, 5, 3, 5, 6, 6, 3, 6, 9, 8, 0, 7, 4, 2, 6, 5, 4, 2, 5, 2, 7, 8, 6, 2, 5, 5, 1, 8, 1, 8, 4, 1, 7, 5, 7, 4, 6, 7, 2, 8, 9, 0, 9, 7, 7, 7, 7, 2, 7, 9, 3, 8, 0, 0, 0, 8, 1, 6, 4, 7, 0, 6, 0, 0, 1, 6, 1, 4, 5, 2, 4, 9, 1, 9, 2, 1, 7, 3, 2, 1, 7, 2, 1, 4, 7, 7, 2, 3, 5, 0, 1, 4, 1, 4, 4, 1, 9, 7, 3, 5, 6, 8, 5, 4, 8, 1, 6, 1, 3, 6, 1, 1, 5, 7, 3, 5, 2, 5, 5, 2, 1, 3, 3, 4, 7, 5, 7, 4, 1, 8, 4, 9, 4, 6, 8, 4, 3, 8, 5, 2, 3, 3, 2, 3, 9, 0, 7, 3, 9, 4, 1, 4, 3, 3, 3, 4, 5, 4, 7, 7, 6, 2, 4, 1, 6, 8, 6, 2, 5, 1, 8, 9, 8, 3, 5, 6, 9, 4, 8, 5, 5, 6, 2, 0, 9, 9, 2, 1, 9, 2, 2, 2, 1, 8, 4, 2, 7, 2, 5, 5, 0, 2, 5, 4, 2, 5, 6, 8, 8, 7, 6, 7, 1, 7, 9, 0, 4, 9, 4, 6, 0, 1, 6, 5, 3, 4, 6, 6, 8, 0, 4, 9, 8, 8, 6, 2, 7, 2, 3, 2, 7, 9, 1, 7, 8, 6, 0, 8, 5, 7, 8, 4, 3, 8, 3, 8, 2, 7, 9, 6, 7, 9, 7, 6, 6, 8, 1, 4, 5, 4, 1, 0, 0, 9, 5, 3, 8, 8, 3, 7, 8, 6, 3, 6, 0, 9, 5, 0, 6, 8, 0, 0, 6, 4, 2, 2, 5, 1, 2, 5, 2, 0, 5, 1, 1, 7, 3, 9, 2, 9, 8, 4, 8, 9, 6, 0, 8, 4, 1, 2, 8, 4, 8, 8, 6, 2, 6, 9, 4, 5, 6, 0, 4, 2, 4, 1, 9, 6, 5, 2, 8, 5, 0, 2, 2, 2, 1, 0, 6, 6, 1, 1, 8, 6, 3, 0, 6, 7, 4, 4, 2, 7, 8, 6, 2, 2, 0, 3, 9, 1, 9, 4, 9, 4, 5, 0, 4, 7, 1, 2, 3, 7, 1, 3, 7, 8, 6, 9, 6, 0, 9, 5, 6, 3, 6, 4, 3, 7, 1, 9, 1, 7, 2, 8, 7, 4, 6, 7, 7, 6, 4, 6, 5, 7, 5, 7, 3, 9, 6, 2, 4, 1, 3, 8, 9, 0, 8, 6, 5, 8, 3, 2, 6, 4, 5, 9, 9, 5, 8, 1, 3, 3, 9, 0, 4, 7, 8, 0, 2, 7, 5, 9, 0, 0, 9, 9, 4, 6, 5, 7, 6, 4, 0, 7, 8, 9, 5, 1, 2, 6, 9, 4, 6, 8, 3, 9, 8, 3, 5, 2, 5, 9, 5, 7, 0, 9, 8, 2, 5, 8, 2, 2, 6, 2, 0, 5, 2, 2, 4, 8, 9, 4, 0, 7, 7, 2, 6, 7, 1, 9, 4, 7, 8, 2, 6, 8, 4, 8, 2, 6, 0, 1, 4, 7, 6, 9, 9, 0, 9, 0, 2, 6, 4, 0, 1, 3, 6, 3, 9, 4, 4, 3, 7, 4, 5, 5, 3, 0, 5, 0, 6, 8, 2, 0, 3, 4, 9, 6, 2, 5, 2, 4, 5, 1, 7, 4, 9, 3, 9, 9, 6, 5, 1, 4, 3, 1, 4, 2, 9, 8, 0, 9, 1, 9, 0, 6, 5, 9, 2, 5, 0, 9, 3, 7, 2, 2, 1, 6, 9, 6, 4, 6, 1, 5, 1, 5, 7, 0, 9, 8, 5, 8, 3, 8, 7, 4, 1, 0, 5, 9, 7, 8, 8, 5, 9, 5, 9, 7, 7, 2, 9, 7, 5, 4, 9, 8, 9, 3, 0, 1, 6, 1, 7, 5, 3, 9, 2, 8, 4, 6, 8, 1, 3, 8, 2, 6, 8, 6, 8, 3, 8, 6, 8, 9, 4, 2, 7, 7, 4, 1, 5, 5, 9, 9, 1, 8, 5, 5, 9, 2, 5, 2, 4, 5, 9, 5, 3, 9, 5, 9, 4, 3, 1, 0, 4, 9, 9, 7, 2, 5, 2, 4, 6, 8, 0, 8, 4, 5, 9, 8, 7, 2, 7, 3, 6, 4, 4, 6, 9, 5, 8, 4, 8, 6, 5, 3, 8, 3, 6, 7, 3, 6, 2, 2, 2, 6, 2, 6, 0, 9, 9, 1, 2, 4, 6, 0, 8, 0, 5, 1, 2, 4, 3, 8, 8, 4, 3, 9, 0, 4, 5, 1, 2, 4, 4, 1, 3, 6, 5, 4, 9, 7, 6, 2, 7, 8, 0, 7, 9, 7, 7, 1, 5, 6, 9, 1, 4, 3, 5, 9, 9, 7, 7, 0, 0, 1, 2, 9, 6, 1, 6, 0, 8, 9, 4, 4, 1, 6, 9, 4, 8, 6, 8, 5, 5, 5, 8, 4, 8, 4, 0, 6, 3, 5, 3, 4, 2, 2, 0, 7, 2, 2, 2, 5, 8, 2, 8, 4, 8, 8, 6, 4, 8, 1, 5, 8, 4, 5, 6, 0, 2, 8, 5, 0, 6, 0, 1, 6, 8, 4, 2, 7, 3, 9, 4, 5, 2, 2, 6, 7, 4, 6, 7, 6, 7, 8, 8, 9, 5, 2, 5, 2, 1, 3, 8, 5, 2, 2, 5, 4, 9, 9, 5, 4, 6, 6, 6, 7, 2, 7, 8, 2, 3, 9, 8, 6, 4, 5, 6, 5, 9, 6, 1, 1, 6, 3, 5, 4, 8, 8, 6, 2, 3, 0, 5, 7, 7, 4, 5, 6, 4, 9, 8, 0, 3, 5, 5, 9, 3, 6, 3, 4, 5, 6, 8, 1, 7, 4, 3, 2, 4, 1, 1, 2, 5, 1, 5, 0, 7, 6, 0, 6, 9, 4, 7, 9, 4, 5, 1, 0, 9, 6, 5, 9, 6, 0, 9, 4, 0, 2, 5, 2, 2, 8, 8, 7, 9, 7, 1, 0, 8, 9, 3, 1, 4, 5, 6, 6, 9, 1, 3, 6, 8, 6, 7, 2, 2, 8, 7, 4, 8, 9, 4, 0, 5, 6, 0, 1, 0, 1, 5, 0, 3, 3, 0, 8, 6, 1, 7, 9, 2, 8, 6, 8, 0, 9, 2, 0, 8, 7, 4, 7, 6, 0, 9, 1, 7, 8, 2, 4, 9, 3, 8, 5, 8, 9, 0, 0, 9, 7, 1, 4, 9, 0, 9, 6, 7, 5, 9, 8, 5, 2, 6, 1, 3, 6, 5, 5, 4, 9, 7, 8, 1, 8, 9, 3, 1, 2, 9, 7, 8, 4, 8, 2, 1, 6, 8, 2, 9, 9, 8, 9, 4, 8, 7, 2, 2, 6, 5, 8, 8, 0, 4, 8, 5, 7, 5, 6, 4, 0, 1, 4, 2, 7, 0, 4, 7, 7, 5, 5, 5, 1, 3, 2, 3, 7, 9, 6, 4, 1, 4, 5, 1, 5, 2, 3, 7, 4, 6, 2, 3, 4, 3, 6, 4, 5, 4, 2, 8, 5, 8, 4, 4, 4, 7, 9, 5, 2, 6, 5, 8, 6, 7, 8, 2, 1, 0, 5, 1, 1, 4, 1, 3, 5, 4, 7, 3, 5, 7, 3, 9, 5, 2, 3, 1, 1, 3, 4, 2, 7, 1, 6, 6, 1, 0, 2, 1, 3, 5, 9, 6, 9, 5, 3, 6, 2, 3, 1, 4, 4, 2, 9, 5, 2, 4, 8, 4, 9, 3, 7, 1, 8, 7, 1, 1, 0, 1, 4, 5, 7, 6, 5, 4, 0, 3, 5, 9, 0, 2, 7, 9, 9, 3, 4, 4, 0, 3, 7, 4, 2, 0, 0, 7, 3, 1, 0, 5, 7, 8, 5, 3, 9, 0, 6, 2, 1, 9, 8, 3, 8, 7, 4, 4, 7, 8, 0, 8, 4, 7, 8, 4, 8, 9, 6, 8, 3, 3, 2, 1, 4, 4, 5, 7, 1, 3, 8, 6, 8, 7, 5, 1, 9, 4, 3, 5, 0, 6, 4, 3, 0, 2, 1, 8, 4, 5, 3, 1, 9, 1, 0, 4, 8, 4, 8, 1, 0, 0, 5, 3, 7, 0, 6, 1, 4, 6, 8, 0, 6, 7, 4, 9, 1, 9, 2, 7, 8, 1, 9, 1, 1, 9, 7, 9, 3, 9, 9, 5, 2, 0, 6, 1, 4, 1, 9, 6, 6, 3, 4, 2, 8, 7, 5, 4, 4, 4, 0, 6, 4, 3, 7, 4, 5, 1, 2, 3, 7, 1, 8, 1, 9, 2, 1, 7, 9, 9, 9, 8, 3, 9, 1, 0, 1, 5, 9, 1, 9, 5, 6, 1, 8, 1, 4, 6, 7, 5, 1, 4, 2, 6, 9, 1, 2, 3, 9, 7, 4, 8, 9, 4, 0, 9, 0, 7, 1, 8, 6, 4, 9, 4, 2, 3, 1, 9, 6, 1, 5, 6, 7, 9, 4, 5, 2, 0, 8, 0, 9, 5, 1, 4, 6, 5, 5, 0, 2, 2, 5, 2, 3, 1, 6, 0, 3, 8, 8, 1, 9, 3, 0, 1, 4, 2, 0, 9, 3, 7, 6, 2, 1, 3, 7, 8, 5, 5, 9, 5, 6, 6, 3, 8, 9, 3, 7, 7, 8, 7, 0, 8, 3, 0, 3, 9, 0, 6, 9, 7, 9, 2, 0, 7, 7, 3, 4, 6, 7, 2, 2, 1, 8, 2, 5, 6, 2, 5, 9, 9, 6, 6, 1, 5, 0, 1, 4, 2, 1, 5, 0, 3, 0, 6, 8, 0, 3, 8, 4, 4, 7, 7, 3, 4, 5, 4, 9, 2, 0, 2, 6, 0, 5, 4, 1, 4, 6, 6, 5, 9, 2, 5, 2, 0, 1, 4, 9, 7, 4, 4, 2, 8, 5, 0, 7, 3, 2, 5, 1, 8, 6, 6, 6, 0, 0, 2, 1, 3, 2, 4, 3, 4, 0, 8, 8, 1, 9, 0, 7, 1, 0, 4, 8, 6, 3, 3, 1, 7, 3, 4, 6, 4, 9, 6, 5, 1, 4, 5, 3, 9, 0, 5, 7, 9, 6, 2, 6, 8, 5, 6, 1, 0, 0, 5, 5, 0, 8, 1, 0, 6, 6, 5, 8, 7, 9, 6, 9, 9, 8, 1, 6, 3, 5, 7, 4, 7, 3, 6, 3, 8, 4, 0, 5, 2, 5, 7, 1, 4, 5, 9, 1, 0, 2, 8, 9, 7, 0, 6, 4, 1, 4, 0, 1, 1, 0, 9, 7, 1, 2, 0, 6, 2, 8, 0, 4, 3, 9, 0, 3, 9, 7, 5, 9, 5, 1, 5, 6, 7, 7, 1, 5, 7, 7, 0, 0, 4, 2, 0, 3, 3, 7, 8, 6, 9, 9, 3, 6, 0, 0, 7, 2, 3, 0, 5, 5, 8, 7, 6, 3, 1, 7, 6, 3, 5, 9, 4, 2, 1, 8, 7, 3, 1, 2, 5, 1, 4, 7, 1, 2, 0, 5, 3, 2, 9, 2, 8, 1, 9, 1, 8, 2, 6, 1, 8, 6, 1, 2, 5, 8, 6, 7, 3, 2, 1, 5, 7, 9, 1, 9, 8, 4, 1, 4, 8, 4, 8, 8, 2, 9, 1, 6, 4, 4, 7, 0, 6, 0, 9, 5, 7, 5, 2, 7, 0, 6, 9, 5, 7, 2, 2, 0, 9, 1, 7, 5, 6, 7, 1, 1, 6, 7, 2, 2, 9, 1, 0, 9, 8, 1, 6, 9, 0, 9, 1, 5, 2, 8, 0, 1, 7, 3, 5, 0, 6, 7, 1, 2, 7, 4, 8, 5, 8, 3, 2, 2, 2, 8, 7, 1, 8, 3, 5, 2, 0, 9, 3, 5, 3, 9, 6, 5, 7, 2, 5, 1, 2, 1, 0, 8, 3, 5, 7, 9, 1, 5, 1, 3, 6, 9, 8, 8, 2, 0, 9, 1, 4, 4, 4, 2, 1, 0, 0, 6, 7, 5, 1, 0, 3, 3, 4, 6, 7, 1, 1, 0, 3, 1, 4, 1, 2, 6, 7, 1, 1, 1, 3, 6, 9, 9, 0, 8, 6, 5, 8, 5, 1, 6, 3, 9, 8, 3, 1, 5, 0, 1, 9, 7, 0, 1, 6, 5, 1, 5, 1, 1, 6, 8, 5, 1, 7, 1, 4, 3, 7, 6, 5, 7, 6, 1, 8, 3, 5, 1, 5, 5, 6, 5, 0, 8, 8, 4, 9, 0, 9, 9, 8, 9, 8, 5, 9, 9, 8, 2, 3, 8, 7, 3, 4, 5, 5, 2, 8, 3, 3, 1, 6, 3, 5, 5, 0, 7, 6, 4, 7, 9, 1, 8, 5, 3, 5, 8, 9, 3, 2, 2, 6, 1, 8, 5, 4, 8, 9, 6, 3, 2, 1, 3, 2, 9, 3, 3, 0, 8, 9, 8, 5, 7, 0, 6, 4, 2, 0, 4, 6, 7, 5, 2, 5, 9, 0, 7, 0, 9, 1, 5, 4, 8, 1, 4, 1, 6, 5, 4, 9, 8, 5, 9, 4, 6, 1, 6, 3, 7, 1, 8, 0, 2, 7, 0, 9, 8, 1, 9, 9, 4, 3, 0, 9, 9, 2, 4, 4, 8, 8, 9, 5, 7, 5, 7, 1, 2, 8, 2, 8, 9, 0, 5, 9, 2, 3, 2, 3, 3, 2, 6, 0, 9, 7, 2, 9, 9, 7, 1, 2, 0, 8, 4, 4, 3, 3, 5, 7, 3, 2, 6, 5, 4, 8, 9, 3, 8, 2, 3, 9, 1, 1, 9, 3, 2, 5, 9, 7, 4, 6, 3, 6, 6, 7, 3, 0, 5, 8, 3, 6, 0, 4, 1, 4, 2, 8, 1, 3, 8, 8, 3, 0, 3, 2, 0, 3, 8, 2, 4, 9, 0, 3, 7, 5, 8, 9, 8, 5, 2, 4, 3, 7, 4, 4, 1, 7, 0, 2, 9, 1, 3, 2, 7, 6, 5, 6, 1, 8, 0, 9, 3, 7, 7, 3, 4, 4, 4, 0, 3, 0, 7, 0, 7, 4, 6, 9, 2, 1, 1, 2, 0, 1, 9, 1, 3, 0, 2, 0, 3, 3, 0, 3, 8, 0, 1, 9, 7, 6, 2, 1, 1, 0, 1, 1, 0, 0, 4, 4, 9, 2, 9, 3, 2, 1, 5, 1, 6, 0, 8, 4, 2, 4, 4, 4, 8, 5, 9, 6, 3, 7, 6, 6, 9, 8, 3, 8, 9, 5, 2, 2, 8, 6, 8, 4, 7, 8, 3, 1, 2, 3, 5, 5, 2, 6, 5, 8, 2, 1, 3, 1, 4, 4, 9, 5, 7, 6, 8, 5, 7, 2, 6, 2, 4, 3, 3, 4, 4, 1, 8, 9, 3, 0, 3, 9, 6, 8, 6, 4, 2, 6, 2, 4, 3, 4, 1, 0, 7, 7, 3, 2, 2, 6, 9, 7, 8, 0, 2, 8, 0, 7, 3, 1, 8, 9, 1, 5, 4, 4, 1, 1, 0, 1, 0, 4, 4, 6, 8, 2, 3, 2, 5, 2, 7, 1, 6, 2, 0, 1, 0, 5, 2, 6, 5, 2, 2, 7, 2, 1, 1, 1, 6, 6, 0, 3, 9, 6, 6, 6, 5, 5, 7, 3, 0, 9, 2, 5, 4, 7, 1, 1, 0, 5, 5, 7, 8, 5, 3, 7, 6, 3, 4, 6, 6, 8, 2, 0, 6, 5, 3, 1, 0, 9, 8, 9, 6, 5, 2, 6, 9, 1, 8, 6, 2, 0, 5, 6, 4, 7, 6, 9, 3, 1, 2, 5, 7, 0, 5, 8, 6, 3, 5, 6, 6, 2, 0, 1, 8, 5, 5, 8, 1, 0, 0, 7, 2, 9, 3, 6, 0, 6, 5, 9, 8, 7, 6, 4, 8, 6, 1, 1, 7, 9, 1, 0, 4, 5, 3, 3, 4, 8, 8, 5, 0, 3, 4, 6, 1, 1, 3, 6, 5, 7, 6, 8, 6, 7, 5, 3, 2, 4, 9, 4, 4, 1, 6, 6, 8, 0, 3, 9, 6, 2, 6, 5, 7, 9, 7, 8, 7, 7, 1, 8, 5, 5, 6, 0, 8, 4, 5, 5, 2, 9, 6, 5, 4, 1, 2, 6, 6, 5, 4, 0, 8, 5, 3, 0, 6, 1, 4, 3, 4, 4, 4, 3, 1, 8, 5, 8, 6, 7, 6, 9, 7, 5, 1, 4, 5, 6, 6, 1, 4, 0, 6, 8, 0, 0, 7, 0, 0, 2, 3, 7, 8, 7, 7, 6, 5, 9, 1, 3, 4, 4, 0, 1, 7, 1, 2, 7, 4, 9, 4, 7, 0, 4, 2, 0, 5, 6, 2, 2, 3, 0, 5, 3, 8, 9, 9, 4, 5, 6, 1, 3, 1, 4, 0, 7, 1, 1, 2, 7, 0, 0, 0, 4, 0, 7, 8, 5, 4, 7, 3, 3, 2, 6, 9, 9, 3, 9, 0, 8, 1, 4, 5, 4, 6, 6, 4, 6, 4, 5, 8, 8, 0, 7, 9, 7, 2, 7, 0, 8, 2, 6, 6, 8, 3, 0, 6, 3, 4, 3, 2, 8, 5, 8, 7, 8, 5, 6, 9, 8, 3, 0, 5, 2, 3, 5, 8, 0, 8, 9, 3, 3, 0, 6, 5, 7, 5, 7, 4, 0, 6, 7, 9, 5, 4, 5, 7, 1, 6, 3, 7, 7, 5, 2, 5, 4, 2, 0, 2, 1, 1, 4, 9, 5, 5, 7, 6, 1, 5, 8, 1, 4, 0, 0, 2, 5, 0, 1, 2, 6, 2, 2, 8, 5, 9, 4, 1, 3, 0, 2, 1, 6, 4, 7, 1, 5, 5, 0, 9, 7, 9, 2, 5, 9, 2, 3, 0, 9, 9, 0, 7, 9, 6, 5, 4, 7, 3, 7, 6, 1, 2, 5, 5, 1, 7, 6, 5, 6, 7, 5, 1, 3, 5, 7, 5, 1, 7, 8, 2, 9, 6, 6, 6, 4, 5, 4, 7, 7, 9, 1, 7, 4, 5, 0, 1, 1, 2, 9, 9, 6, 1, 4, 8, 9, 0, 3, 0, 4, 6, 3, 9, 9, 4, 7, 1, 3, 2, 9, 6, 2, 1, 0, 7, 3, 4, 0, 4, 3, 7, 5, 1, 8, 9, 5, 7, 3, 5, 9, 6, 1, 4, 5, 8, 9, 0, 1, 9, 3, 8, 9, 7, 1, 3, 1, 1, 1, 7, 9, 0, 4, 2, 9, 7, 8, 2, 8, 5, 6, 4, 7, 5, 0, 3, 2, 0, 3, 1, 9, 8, 6, 9, 1, 5, 1, 4, 0, 2, 8, 7, 0, 8, 0, 8, 5, 9, 9, 0, 4, 8, 0, 1, 0, 9, 4, 1, 2, 1, 4, 7, 2, 2, 1, 3, 1, 7, 9, 4, 7, 6, 4, 7, 7, 7, 2, 6, 2, 2, 4, 1, 4, 2, 5, 4, 8, 5, 4, 5, 4, 0, 3, 3, 2, 1, 5, 7, 1, 8, 5, 3, 0, 6, 1, 4, 2, 2, 8, 8, 1, 3, 7, 5, 8, 5, 0, 4, 3, 0, 6, 3, 3, 2, 1, 7, 5, 1, 8, 2, 9, 7, 9, 8, 6, 6, 2, 2, 3, 7, 1, 7, 2, 1, 5, 9, 1, 6, 0, 7, 7, 1, 6, 6, 9, 2, 5, 4, 7, 4, 8, 7, 3, 8, 9, 8, 6, 6, 5, 4, 9, 4, 9, 4, 5, 0, 1, 1, 4, 6, 5, 4, 0, 6, 2, 8, 4, 3, 3, 6, 6, 3, 9, 3, 7, 9, 0, 0, 3, 9, 7, 6, 9, 2, 6, 5, 6, 7, 2, 1, 4, 6, 3, 8, 5, 3, 0, 6, 7, 3, 6, 0, 9, 6, 5, 7, 1, 2, 0, 9, 1, 8, 0, 7, 6, 3, 8, 3, 2, 7, 1, 6, 6, 4, 1, 6, 2, 7, 4, 8, 8, 8, 8, 0, 0, 7, 8, 6, 9, 2, 5, 6, 0, 2, 9, 0, 2, 2, 8, 4, 7, 2, 1, 0, 4, 0, 3, 1, 7, 2, 1, 1, 8, 6, 0, 8, 2, 0, 4, 1, 9, 0, 0, 0, 4, 2, 2, 9, 6, 6, 1, 7, 1, 1, 9, 6, 3, 7, 7, 9, 2, 1, 3, 3, 7, 5, 7, 5, 1, 1, 4, 9, 5, 9, 5, 0, 1, 5, 6, 6, 0, 4, 9, 6, 3, 1, 8, 6, 2, 9, 4, 7, 2, 6, 5, 4, 7, 3, 6, 4, 2, 5, 2, 3, 0, 8, 1, 7, 7, 0, 3, 6, 7, 5, 1, 5, 9, 0, 6, 7, 3, 5, 0, 2, 3, 5, 0, 7, 2, 8, 3, 5, 4, 0, 5, 6, 7, 0, 4, 0, 3, 8, 6, 7, 4, 3, 5, 1, 3, 6, 2, 2, 2, 2, 4, 7, 7, 1, 5, 8, 9, 1, 5, 0, 4, 9, 5, 3, 0, 9, 8, 4, 4, 4, 8, 9, 3, 3, 3, 0, 9, 6, 3, 4, 0, 8, 7, 8, 0, 7, 6, 9, 3, 2, 5, 9, 9, 3, 9, 7, 8, 0, 5, 4, 1, 9, 3, 4, 1, 4, 4, 7, 3, 7, 7, 4, 4, 1, 8, 4, 2, 6, 3, 1, 2, 9, 8, 6, 0, 8, 0, 9, 9, 8, 8, 8, 6, 8, 7, 4, 1, 3, 2, 6, 0, 4, 7, 2, 1, 5, 6, 9, 5, 1, 6, 2, 3, 9, 6, 5, 8, 6, 4, 5, 7, 3, 0, 2, 1, 6, 3, 1, 5, 9, 8, 1, 9, 3, 1, 9, 5, 1, 6, 7, 3, 5, 3, 8, 1, 2, 9, 7, 4, 1, 6, 7, 7, 2, 9, 4, 7, 8, 6, 7, 2, 4, 2, 2, 9, 2, 4, 6, 5, 4, 3, 6, 6, 8, 0, 0, 9, 8, 0, 6, 7, 6, 9, 2, 8, 2, 3, 8, 2, 8, 0, 6, 8, 9, 9, 6, 4, 0, 0, 4, 8, 2, 4, 3, 5, 4, 0, 3, 7, 0, 1, 4, 1, 6, 3, 1, 4, 9, 6, 5, 8, 9, 7, 9, 4, 0, 9, 2, 4, 3, 2, 3, 7, 8, 9, 6, 9, 0, 7, 0, 6, 9, 7, 7, 9, 4, 2, 2, 3, 6, 2, 5, 0, 8, 2, 2, 1, 6, 8, 8, 9, 5, 7, 3, 8, 3, 7, 9, 8, 6, 2, 3, 0, 0, 1, 5, 9, 3, 7, 7, 6, 4, 7, 1, 6, 5, 1, 2, 2, 8, 9, 3, 5, 7, 8, 6, 0, 1, 5, 8, 8, 1, 6, 1, 7, 5, 5, 7, 8, 2, 9, 7, 3, 5, 2, 3, 3, 4, 4, 6, 0, 4, 2, 8, 1, 5, 1, 2, 6, 2, 7, 2, 0, 3, 7, 3, 4, 3, 1, 4, 6, 5, 3, 1, 9, 7, 7, 7, 7, 4, 1, 6, 0, 3, 1, 9, 9, 0, 6, 6, 5, 5, 4, 1, 8, 7, 6, 3, 9, 7, 9, 2, 9, 3, 3, 4, 4, 1, 9, 5, 2, 1, 5, 4, 1, 3, 4, 1, 8, 9, 9, 4, 8, 5, 4, 4, 4, 7, 3, 4, 5, 6, 7, 3, 8, 3, 1, 6, 2, 4, 9, 9, 3, 4, 1, 9, 1, 3, 1, 8, 1, 4, 8, 0, 9, 2, 7, 7, 7, 7, 1, 0, 3, 8, 6, 3, 8, 7, 7, 3, 4, 3, 1, 7, 7, 2, 0, 7, 5, 4, 5, 6, 5, 4, 5, 3, 2, 2, 0, 7, 7, 7, 0, 9, 2, 1, 2, 0, 1, 9, 0, 5, 1, 6, 6, 0, 9, 6, 2, 8, 0, 4, 9, 0, 9, 2, 6, 3, 6, 0, 1, 9, 7, 5, 9, 8, 8, 2, 8, 1, 6, 1, 3, 3, 2, 3, 1, 6, 6, 6, 3, 6, 5, 2, 8, 6, 1, 9, 3, 2, 6, 6, 8, 6, 3, 3, 6, 0, 6, 2, 7, 3, 5, 6, 7, 6, 3, 0, 3, 5, 4, 4, 7, 7, 6, 2, 8, 0, 3, 5, 0, 4, 5, 0, 7, 7, 7, 2, 3, 5, 5, 4, 7, 1, 0, 5, 8, 5, 9, 5, 4, 8, 7, 0, 2, 7, 9, 0, 8, 1, 4, 3, 5, 6, 2, 4, 0, 1, 4, 5, 1, 7, 1, 8, 0, 6, 2, 4, 6, 4, 3, 6, 2, 6, 7, 9, 4, 5, 6, 1, 2, 7, 5, 3, 1, 8, 1, 3, 4, 0, 7, 8, 3, 3, 0, 3, 3, 6, 2, 5, 4, 2, 3, 2, 7, 8, 3, 9, 4, 4, 9, 7, 5, 3, 8, 2, 4, 3, 7, 2, 0, 5, 8, 3, 5, 3, 1, 1, 4, 7, 7, 1, 1, 9, 9, 2, 6, 0, 6, 3, 8, 1, 3, 3, 4, 6, 7, 7, 6, 8, 7, 9, 6, 9, 5, 9, 7, 0, 3, 0, 9, 8, 3, 3, 9, 1, 3, 0, 7, 7, 1, 0, 9, 8, 7, 0, 4, 0, 8, 5, 9, 1, 3, 3, 7, 4, 6, 4, 1, 4, 4, 2, 8, 2, 2, 7, 7, 2, 6, 3, 4, 6, 5, 9, 4, 7, 0, 4, 7, 4, 5, 8, 7, 8, 4, 7, 7, 8, 7, 2, 0, 1, 9, 2, 7, 7, 1, 5, 2, 8, 0, 7, 3, 1, 7, 6, 7, 9, 0, 7, 7, 0, 7, 1, 5, 7, 2, 1, 3, 4, 4, 4, 7, 3, 0, 6, 0, 5, 7, 0, 0, 7, 3, 3, 4, 9, 2, 4, 3, 6, 9, 3, 1, 1, 3, 8, 3, 5, 0, 4, 9, 3, 1, 6, 3, 1, 2, 8, 4, 0, 4, 2, 5, 1, 2, 1, 9, 2, 5, 6, 5, 1, 7, 9, 8, 0, 6, 9, 4, 1, 1, 3, 5, 2, 8, 0, 1, 3, 1, 4, 7, 0, 1, 3, 0, 4, 7, 8, 1, 6, 4, 3, 7, 8, 8, 5, 1, 8, 5, 2, 9, 0, 9, 2, 8, 5, 4, 5, 2, 0, 1, 1, 6, 5, 8, 3, 9, 3, 4, 1, 9, 6, 5, 6, 2, 1, 3, 4, 9, 1, 4, 3, 4, 1, 5, 9, 5, 6, 2, 5, 8, 6, 5, 8, 6, 5, 5, 7, 0, 5, 5, 2, 6, 9, 0, 4, 9, 6, 5, 2, 0, 9, 8, 5, 8, 0, 3, 3, 8, 5, 0, 7, 2, 2, 4, 2, 6, 4, 8, 2, 9, 3, 9, 7, 2, 8, 5, 8, 4, 7, 8, 3, 1, 6, 3, 0, 5, 7, 7, 7, 7, 5, 6, 0, 6, 8, 8, 8, 7, 6, 4, 4, 6, 2, 4, 8, 2, 4, 6, 8, 5, 7, 9, 2, 6, 0, 3, 9, 5, 3, 5, 2, 7, 7, 3, 4, 8, 0, 3, 0, 4, 8, 0, 2, 9, 0, 0, 5, 8, 7, 6, 0, 7, 5, 8, 2, 5, 1, 0, 4, 7, 4, 7, 0, 9, 1, 6, 4, 3, 9, 6, 1, 3, 6, 2, 6, 7, 6, 0, 4, 4, 9, 2, 5, 6, 2, 7, 4, 2, 0, 4, 2, 0, 8, 3, 2, 0, 8, 5, 6, 6, 1, 1, 9, 0, 6, 2, 5, 4, 5, 4, 3, 3, 7, 2, 1, 3, 1, 5, 3, 5, 9, 5, 8, 4, 5, 0, 6, 8, 7, 7, 2, 4, 6, 0, 2, 9, 0, 1, 6, 1, 8, 7, 6, 6, 7, 9, 5, 2, 4, 0, 6, 1, 6, 3, 4, 2, 5, 2, 2, 5, 7, 7, 1, 9, 5, 4, 2, 9, 1, 6, 2, 9, 9, 1, 9, 3, 0, 6, 4, 5, 5, 3, 7, 7, 9, 9, 1, 4, 0, 3, 7, 3, 4, 0, 4, 3, 2, 8, 7, 5, 2, 6, 2, 8, 8, 8, 9, 6, 3, 9, 9, 5, 8, 7, 9, 4, 7, 5, 7, 2, 9, 1, 7, 4, 6, 4, 2, 6, 3, 5, 7, 4, 5, 5, 2, 5, 4, 0, 7, 9, 0, 9, 1, 4, 5, 1, 3, 5, 7, 1, 1, 1, 3, 6, 9, 4, 1, 0, 9, 1, 1, 9, 3, 9, 3, 2, 5, 1, 9, 1, 0, 7, 6, 0, 2, 0, 8, 2, 5, 2, 0, 2, 6, 1, 8, 7, 9, 8, 5, 3, 1, 8, 8, 7, 7, 0, 5, 8, 4, 2, 9, 7, 2, 5, 9, 1, 6, 7, 7, 8, 1, 3, 1, 4, 9, 6, 9, 9, 0, 0, 9, 0, 1, 9, 2, 1, 1, 6, 9, 7, 1, 7, 3, 7, 2, 7, 8, 4, 7, 6, 8, 4, 7, 2, 6, 8, 6, 0, 8, 4, 9, 0, 0, 3, 3, 7, 7, 0, 2, 4, 2, 4, 2, 9, 1, 6, 5, 1, 3, 0, 0, 5, 0, 0, 5, 1, 6, 8, 3, 2, 3, 3, 6, 4, 3, 5, 0, 3, 8, 9, 5, 1, 7, 0, 2, 9, 8, 9, 3, 9, 2, 2, 3, 3, 4, 5, 1, 7, 2, 2, 0, 1, 3, 8, 1, 2, 8, 0, 6, 9, 6, 5, 0, 1, 1, 7, 8, 4, 4, 0, 8, 7, 4, 5, 1, 9, 6, 0, 1, 2, 1, 2, 2, 8, 5, 9, 9, 3, 7, 1, 6, 2, 3, 1, 3, 0, 1, 7, 1, 1, 4, 4, 4, 8, 4, 6, 4, 0, 9, 0, 3, 8, 9, 0, 6, 4, 4, 9, 5, 4, 4, 4, 0, 0, 6, 1, 9, 8, 6, 9, 0, 7, 5, 4, 8, 5, 1, 6, 0, 2, 6, 3, 2, 7, 5, 0, 5, 2, 9, 8, 3, 4, 9, 1, 8, 7, 4, 0, 7, 8, 6, 6, 8, 0, 8, 8, 1, 8, 3, 3, 8, 5, 1, 0, 2, 2, 8, 3, 3, 4, 5, 0, 8, 5, 0, 4, 8, 6, 0, 8, 2, 5, 0, 3, 9, 3, 0, 2, 1, 3, 3, 2, 1, 9, 7, 1, 5, 5, 1, 8, 4, 3, 0, 6, 3, 5, 4, 5, 5, 0, 0, 7, 6, 6, 8, 2, 8, 2, 9, 4, 9, 3, 0, 4, 1, 3, 7, 7, 6, 5, 5, 2, 7, 9, 3, 9, 7, 5, 1, 7, 5, 4, 6, 1, 3, 9, 5, 3, 9, 8, 4, 6, 8, 3, 3, 9, 3, 6, 3, 8, 3, 0, 4, 7, 4, 6, 1, 1, 9, 9, 6, 6, 5, 3, 8, 5, 8, 1, 5, 3, 8, 4, 2, 0, 5, 6, 8, 5, 3, 3, 8, 6, 2, 1, 8, 6, 7, 2, 5, 2, 3, 3, 4, 0, 2, 8, 3, 0, 8, 7, 1, 1, 2, 3, 2, 8, 2, 7, 8, 9, 2, 1, 2, 5, 0, 7, 7, 1, 2, 6, 2, 9, 4, 6, 3, 2, 2, 9, 5, 6, 3, 9, 8, 9, 8, 9, 8, 9, 3, 5, 8, 2, 1, 1, 6, 7, 4, 5, 6, 2, 7, 0, 1, 0, 2, 1, 8, 3, 5, 6, 4, 6, 2, 2, 0, 1, 3, 4, 9, 6, 7, 1, 5, 1, 8, 8, 1, 9, 0, 9, 7, 3, 0, 3, 8, 1, 1, 9, 8, 0, 0, 4, 9, 7, 3, 4, 0, 7, 2, 3, 9, 6, 1, 0, 3, 6, 8, 5, 4, 0, 6, 6, 4, 3, 1, 9, 3, 9, 5, 0, 9, 7, 9, 0, 1, 9, 0, 6, 9, 9, 6, 3, 9, 5, 5, 2, 4, 5, 3, 0, 0, 5, 4, 5, 0, 5, 8, 0, 6, 8, 5, 5, 0, 1, 9, 5, 6, 7, 3, 0, 2, 2, 9, 2, 1, 9, 1, 3, 9, 3, 3, 9, 1, 8, 5, 6, 8, 0, 3, 4, 4, 9, 0, 3, 9, 8, 2, 0, 5, 9, 5, 5, 1, 0, 0, 2, 2, 6, 3, 5, 3, 5, 3, 6, 1, 9, 2, 0, 4, 1, 9, 9, 4, 7, 4, 5, 5, 3, 8, 5, 9, 3, 8, 1, 0, 2, 3, 4, 3, 9, 5, 5, 4, 4, 9, 5, 9, 7, 7, 8, 3, 7, 7, 9, 0, 2, 3, 7, 4, 2, 1, 6, 1, 7, 2, 7, 1, 1, 1, 7, 2, 3, 6, 4, 3, 4, 3, 5, 4, 3, 9, 4, 7, 8, 2, 2, 1, 8, 1, 8, 5, 2, 8, 6, 2, 4, 0, 8, 5, 1, 4, 0, 0, 6, 6, 6, 0, 4, 4, 3, 3, 2, 5, 8, 8, 8, 5, 6, 9, 8, 6, 7, 0, 5, 4, 3, 1, 5, 4, 7, 0, 6, 9, 6, 5, 7, 4, 7, 4, 5, 8, 5, 5, 0, 3, 3, 2, 3, 2, 3, 3, 4, 2, 1, 0, 7, 3, 0, 1, 5, 4, 5, 9, 4, 0, 5, 1, 6, 5, 5, 3, 7, 9, 0, 6, 8, 6, 6, 2, 7, 3, 3, 3, 7, 9, 9, 5, 8, 5, 1, 1, 5, 6, 2, 5, 7, 8, 4, 3, 2, 2, 9, 8, 8, 2, 7, 3, 7, 2, 3, 1, 9, 8, 9, 8, 7, 5, 7, 1, 4, 1, 5, 9, 5, 7, 8, 1, 1, 1, 9, 6, 3, 5, 8, 3, 3, 0, 0, 5, 9, 4, 0, 8, 7, 3, 0, 6, 8, 1, 2, 1, 6, 0, 2, 8, 7, 6, 4, 9, 6, 2, 8, 6, 7, 4, 4, 6, 0, 4, 7, 7, 4, 6, 4, 9, 1, 5, 9, 9, 5, 0, 5, 4, 9, 7, 3, 7, 4, 2, 5, 6, 2, 6, 9, 0, 1, 0, 4, 9, 0, 3, 7, 7, 8, 1, 9, 8, 6, 8, 3, 5, 9, 3, 8, 1, 4, 6, 5, 7, 4, 1, 2, 6, 8, 0, 4, 9, 2, 5, 6, 4, 8, 7, 9, 8, 5, 5, 6, 1, 4, 5, 3, 7, 2, 3, 4, 7, 8, 6, 7, 3, 3, 0, 3, 9, 0, 4, 6, 8, 8, 3, 8, 3, 4, 3, 6, 3, 4, 6, 5, 5, 3, 7, 9, 4, 9, 8, 6, 4, 1, 9, 2, 7, 0, 5, 6, 3, 8, 7, 2, 9, 3, 1, 7, 4, 8, 7, 2, 3, 3, 2, 0, 8, 3, 7, 6, 0, 1, 1, 2, 3, 0, 2, 9, 9, 1, 1, 3, 6, 7, 9, 3, 8, 6, 2, 7, 0, 8, 9, 4, 3, 8, 7, 9, 9, 3, 6, 2, 0, 1, 6, 2, 9, 5, 1, 5, 4, 1, 3, 3, 7, 1, 4, 2, 4, 8, 9, 2, 8, 3, 0, 7, 2, 2, 0, 1, 2, 6, 9, 0, 1, 4, 7, 5, 4, 6, 6, 8, 4, 7, 6, 5, 3, 5, 7, 6, 1, 6, 4, 7, 7, 3, 7, 9, 4, 6, 7, 5, 2, 0, 0, 4, 9, 0, 7, 5, 7, 1, 5, 5, 5, 2, 7, 8, 1, 9, 6, 5, 3, 6, 2, 1, 3, 2, 3, 9, 2, 6, 4, 0, 6, 1, 6, 0, 1, 3, 6, 3, 5, 8, 1, 5, 5, 9, 0, 7, 4, 2, 2, 0, 2, 0, 2, 0, 3, 1, 8, 7, 2, 7, 7, 6, 0, 5, 2, 7, 7, 2, 1, 9, 0, 0, 5, 5, 6, 1, 4, 8, 4, 2, 5, 5, 5, 1, 8, 7, 9, 2, 5, 3, 0, 3, 4, 3, 5, 1, 3, 9, 8, 4, 4, 2, 5, 3, 2, 2, 3, 4, 1, 5, 7, 6, 2, 3, 3, 6, 1, 0, 6, 4, 2, 5, 0, 6, 3, 9, 0, 4, 9, 7, 5, 0, 0, 8, 6, 5, 6, 2, 7, 1, 0, 9, 5, 3, 5, 9, 1, 9, 4, 6, 5, 8, 9, 7, 5, 1, 4, 1, 3, 1, 0, 3, 4, 8, 2, 2, 7, 6, 9, 3, 0, 6, 2, 4, 7, 4, 3, 5, 3, 6, 3, 2, 5, 6, 9, 1, 6, 0, 7, 8, 1, 5, 4, 7, 8, 1, 8, 1, 1, 5, 2, 8, 4, 3, 6, 6, 7, 9, 5, 7, 0, 6, 1, 1, 0, 8, 6, 1, 5, 3, 3, 1, 5, 0, 4, 4, 5, 2, 1, 2, 7, 4, 7, 3, 9, 2, 4, 5, 4, 4, 9, 4, 5, 4, 2, 3, 6, 8, 2, 8, 8, 6, 0, 6, 1, 3, 4, 0, 8, 4, 1, 4, 8, 6, 3, 7, 7, 6, 7, 0, 0, 9, 6, 1, 2, 0, 7, 1, 5, 1, 2, 4, 9, 1, 4, 0, 4, 3, 0, 2, 7, 2, 5, 3, 8, 6, 0, 7, 6, 4, 8, 2, 3, 6, 3, 4, 1, 4, 3, 3, 4, 6, 2, 3, 5, 1, 8, 9, 7, 5, 7, 6, 6, 4, 5, 2, 1, 6, 4, 1, 3, 7, 6, 7, 9, 6, 9, 0, 3, 1, 4, 9, 5, 0, 1, 9, 1, 0, 8, 5, 7, 5, 9, 8, 4, 4, 2, 3, 9, 1, 9, 8, 6, 2, 9, 1, 6, 4, 2, 1, 9, 3, 9, 9, 4, 9, 0, 7, 2, 3, 6, 2, 3, 4, 6, 4, 6, 8, 4, 4, 1, 1, 7, 3, 9, 4, 0, 3, 2, 6, 5, 9, 1, 8, 4, 0, 4, 4, 3, 7, 8, 0, 5, 1, 3, 3, 3, 8, 9, 4, 5, 2, 5, 7, 4, 2, 3, 9, 9, 5, 0, 8, 2, 9, 6, 5, 9, 1, 2, 2, 8, 5, 0, 8, 5, 5, 5, 8, 2, 1, 5, 7, 2, 5, 0, 3, 1, 0, 7, 1, 2, 5, 7, 0, 1, 2, 6, 6, 8, 3, 0, 2, 4, 0, 2, 9, 2, 9, 5, 2, 5, 2, 2, 0, 1, 1, 8, 7, 2, 6, 7, 6, 7, 5, 6, 2, 2, 0, 4, 1, 5, 4, 2, 0, 5, 1, 6, 1, 8, 4, 1, 6, 3, 4, 8, 4, 7, 5, 6, 5, 1, 6, 9, 9, 9, 8, 1, 1, 6, 1, 4, 1, 0, 1, 0, 0, 2, 9, 9, 6, 0, 7, 8, 3, 8, 6, 9, 0, 9, 2, 9, 1, 6, 0, 3, 0, 2, 8, 8, 4, 0, 0, 2, 6, 9, 1, 0, 4, 1, 4, 0, 7, 9, 2, 8, 8, 6, 2, 1, 5, 0, 7, 8, 4, 2, 4, 5, 1, 6, 7, 0, 9, 0, 8, 7, 0, 0, 0, 6, 9, 9, 2, 8, 2, 1, 2, 0, 6, 6, 0, 4, 1, 8, 3, 7, 1, 8, 0, 6, 5, 3, 5, 5, 6, 7, 2, 5, 2, 5, 3, 2, 5, 6, 7, 5, 3, 2, 8, 6, 1, 2, 9, 1, 0, 4, 2, 4, 8, 7, 7, 6, 1, 8, 2, 5, 8, 2, 9, 7, 6, 5, 1, 5, 7, 9, 5, 9, 8, 4, 7, 0, 3, 5, 6, 2, 2, 2, 6, 2, 9, 3, 4, 8, 6, 0, 0, 3, 4, 1, 5, 8, 7, 2, 2, 9, 8, 0, 5, 3, 4, 9, 8, 9, 6, 5, 0, 2, 2, 6, 2, 9, 1, 7, 4, 8, 7, 8, 8, 2, 0, 2, 7, 3, 4, 2, 0, 9, 2, 2, 2, 2, 4, 5, 3, 3, 9, 8, 5, 6, 2, 6, 4, 7, 6, 6, 9, 1, 4, 9, 0, 5, 5, 6, 2, 8, 4, 2, 5, 0, 3, 9, 1, 2, 7, 5, 7, 7, 1, 0, 2, 8, 4, 0, 2, 7, 9, 9, 8, 0, 6, 6, 3, 6, 5, 8, 2, 5, 4, 8, 8, 9, 2, 6, 4, 8, 8, 0, 2, 5, 4, 5, 6, 6, 1, 0, 1, 7, 2, 9, 6, 7, 0, 2, 6, 6, 4, 0, 7, 6, 5, 5, 9, 0, 4, 2, 9, 0, 9, 9, 4, 5, 6, 8, 1, 5, 0, 6, 5, 2, 6, 5, 3, 0, 5, 3, 7, 1, 8, 2, 9, 4, 1, 2, 7, 0, 3, 3, 6, 9, 3, 1, 3, 7, 8, 5, 1, 7, 8, 6, 0, 9, 0, 4, 0, 7, 0, 8, 6, 6, 7, 1, 1, 4, 9, 6, 5, 5, 8, 3, 4, 3, 4, 3, 4, 7, 6, 9, 3, 3, 8, 5, 7, 8, 1, 7, 1, 1, 3, 8, 6, 4, 5, 5, 8, 7, 3, 6, 7, 8, 1, 2, 3, 0, 1, 4, 5, 8, 7, 6, 8, 7, 1, 2, 6, 6, 0, 3, 4, 8, 9, 1, 3, 9, 0, 9, 5, 6, 2, 0, 0, 9, 9, 3, 9, 3, 6, 1, 0, 3, 1, 0, 2, 9, 1, 6, 1, 6, 1, 5, 2, 8, 8, 1, 3, 8, 4, 3, 7, 9, 0, 9, 9, 0, 4, 2, 3, 1, 7, 4, 7, 3, 3, 6, 3, 9, 4, 8, 0, 4, 5, 7, 5, 9, 3, 1, 4, 9, 3, 1, 4, 0, 5, 2, 9, 7, 6, 3, 4, 7, 5, 7, 4, 8, 1, 1, 9, 3, 5, 6, 7, 0, 9, 1, 1, 0, 1, 3, 7, 7, 5, 1, 7, 2, 1, 0, 0, 8, 0, 3, 1, 5, 5, 9, 0, 2, 4, 8, 5, 3, 0, 9, 0, 6, 6, 9, 2, 0, 3, 7, 6, 7, 1, 9, 2, 2, 0, 3, 3, 2, 2, 9, 0, 9, 4, 3, 3, 4, 6, 7, 6, 8, 5, 1, 4, 2, 2, 1, 4, 4, 7, 7, 3, 7, 9, 3, 9, 3, 7, 5, 1, 7, 0, 3, 4, 4, 3, 6, 6, 1, 9, 9, 1, 0, 4, 0, 3, 3, 7, 5, 1, 1, 1, 7, 3, 5, 4, 7, 1, 9, 1, 8, 5, 5, 0, 4, 6, 4, 4, 9, 0, 2, 6, 3, 6, 5, 5, 1, 2, 8, 1, 6, 2, 2, 8, 8, 2, 4, 4, 6, 2, 5, 7, 5, 9, 1, 6, 3, 3, 3, 0, 3, 9, 1, 0, 7, 2, 2, 5, 3, 8, 3, 7, 4, 2, 1, 8, 2, 1, 4, 0, 8, 8, 3, 5, 0, 8, 6, 5, 7, 3, 9, 1, 7, 7, 1, 5, 0, 9, 6, 8, 2, 8, 8, 7, 4, 7, 8, 2, 6, 5, 6, 9, 9, 5, 9, 9, 5, 7, 4, 4, 9, 0, 6, 6, 1, 7, 5, 8, 3, 4, 4, 1, 3, 7, 5, 2, 2, 3, 9, 7, 0, 9, 6, 8, 3, 4, 0, 8, 0, 0, 5, 3, 5, 5, 9, 8, 4, 9, 1, 7, 5, 4, 1, 7, 3, 8, 1, 8, 8, 3, 9, 9, 9, 4, 4, 6, 9, 7, 4, 8, 6, 7, 6, 2, 6, 5, 5, 1, 6, 5, 8, 2, 7, 6, 5, 8, 4, 8, 3, 5, 8, 8, 4, 5, 3, 1, 4, 2, 7, 7, 5, 6, 8, 7, 9, 0, 0, 2, 9, 0, 9, 5, 1, 7, 0, 2, 8, 3, 5, 2, 9, 7, 1, 6, 3, 4, 4, 5, 6, 2, 1, 2, 9, 6, 4, 0, 4, 3, 5, 2, 3, 1, 1, 7, 6, 0, 0, 6, 6, 5, 1, 0, 1, 2, 4, 1, 2, 0, 0, 6, 5, 9, 7, 5, 5, 8, 5, 1, 2, 7, 6, 1, 7, 8, 5, 8, 3, 8, 2, 9, 2, 0, 4, 1, 9, 7, 4, 8, 4, 4, 2, 3, 6, 0, 8, 0, 0, 7, 1, 9, 3, 0, 4, 5, 7, 6, 1, 8, 9, 3, 2, 3, 4, 9, 2, 2, 9, 2, 7, 9, 6, 5, 0, 1, 9, 8, 7, 5, 1, 8, 7, 2, 1, 2, 7, 2, 6, 7, 5, 0, 7, 9, 8, 1, 2, 5, 5, 4, 7, 0, 9, 5, 8, 9, 0, 4, 5, 5, 6, 3, 5, 7, 9, 2, 1, 2, 2, 1, 0, 3, 3, 3, 4, 6, 6, 9, 7, 4, 9, 9, 2, 3, 5, 6, 3, 0, 2, 5, 4, 9, 4, 7, 8, 0, 2, 4, 9, 0, 1, 1, 4, 1, 9, 5, 2, 1, 2, 3, 8, 2, 8, 1, 5, 3, 0, 9, 1, 1, 4, 0, 7, 9, 0, 7, 3, 8, 6, 0, 2, 5, 1, 5, 2, 2, 7, 4, 2, 9, 9, 5, 8, 1, 8, 0, 7, 2, 4, 7, 1, 6, 2, 5, 9, 1, 6, 6, 8, 5, 4, 5, 1, 3, 3, 3, 1, 2, 3, 9, 4, 8, 0, 4, 9, 4, 7, 0, 7, 9, 1, 1, 9, 1, 5, 3, 2, 6, 7, 3, 4, 3, 0, 2, 8, 2, 4, 4, 1, 8, 6, 0, 4, 1, 4, 2, 6, 3, 6, 3, 9, 5, 4, 8, 0, 0, 0, 4, 4, 8, 0, 0, 2, 6, 7, 0, 4, 9, 6, 2, 4, 8, 2, 0, 1, 7, 9, 2, 8, 9, 6, 4, 7, 6, 6, 9, 7, 5, 8, 3, 1, 8, 3, 2, 7, 1, 3, 1, 4, 2, 5, 1, 7, 0, 2, 9, 6, 9, 2, 3, 4, 8, 8, 9, 6, 2, 7, 6, 6, 8, 4, 4, 0, 3, 2, 3, 2, 6, 0, 9, 2, 7, 5, 2, 4, 9, 6, 0, 3, 5, 7, 9, 9, 6, 4, 6, 9, 2, 5, 6, 5, 0, 4, 9, 3, 6, 8, 1, 8, 3, 6, 0, 9, 0, 0, 3, 2, 3, 8, 0, 9, 2, 9, 3, 4, 5, 9, 5, 8, 8, 9, 7, 0, 6, 9, 5, 3, 6, 5, 3, 4, 9, 4, 0, 6, 0, 3, 4, 0, 2, 1, 6, 6, 5, 4, 4, 3, 7, 5, 5, 8, 9, 0, 0, 4, 5, 6, 3, 2, 8, 8, 2, 2, 5, 0, 5, 4, 5, 2, 5, 5, 6, 4, 0, 5, 6, 4, 4, 8, 2, 4, 6, 5, 1, 5, 1, 8, 7, 5, 4, 7, 1, 1, 9, 6, 2, 1, 8, 4, 4, 3, 9, 6, 5, 8, 2, 5, 3, 3, 7, 5, 4, 3, 8, 8, 5, 6, 9, 0, 9, 4, 1, 1, 3, 0, 3, 1, 5, 0, 9, 5, 2, 6, 1, 7, 9, 3, 7, 8, 0, 0, 2, 9, 7, 4, 1, 2, 0, 7, 6, 6, 5, 1, 4, 7, 9, 3, 9, 4, 2, 5, 9, 0, 2, 9, 8, 9, 6, 9, 5, 9, 4, 6, 9, 9, 5, 5, 6, 5, 7, 6, 1, 2, 1, 8, 6, 5, 6, 1, 9, 6, 7, 3, 3, 7, 8, 6, 2, 3, 6, 2, 5, 6, 1, 2, 5, 2, 1, 6, 3, 2, 0, 8, 6, 2, 8, 6, 9, 2, 2, 2, 1, 0, 3, 2, 7, 4, 8, 8, 9, 2, 1, 8, 6, 5, 4, 3, 6, 4, 8, 0, 2, 2, 9, 6, 7, 8, 0, 7, 0, 5, 7, 6, 5, 6, 1, 5, 1, 4, 4, 6, 3, 2, 0, 4, 6, 9, 2, 7, 9, 0, 6, 8, 2, 1, 2, 0, 7, 3, 8, 8, 3, 7, 7, 8, 1, 4, 2, 3, 3, 5, 6, 2, 8, 2, 3, 6, 0, 8, 9, 6, 3, 2, 0, 8, 0, 6, 8, 2, 2, 2, 4, 6, 8, 0, 1, 2, 2, 4, 8, 2, 6, 1, 1, 7, 7, 1, 8, 5, 8, 9, 6, 3, 8, 1, 4, 0, 9, 1, 8, 3, 9, 0, 3, 6, 7, 3, 6, 7, 2, 2, 2, 0, 8, 8, 8, 3, 2, 1, 5, 1, 3, 7, 5, 5, 6, 0, 0, 3, 7, 2, 7, 9, 8, 3, 9, 4, 0, 0, 4, 1, 5, 2, 9, 7, 0, 0, 2, 8, 7, 8, 3, 0, 7, 6, 6, 7, 0, 9, 4, 4, 4, 7, 4, 5, 6, 0, 1, 3, 4, 5, 5, 6, 4, 1, 7, 2, 5, 4, 3, 7, 0, 9, 0, 6, 9, 7, 9, 3, 9, 6, 1, 2, 2, 5, 7, 1, 4, 2, 9, 8, 9, 4, 6, 7, 1, 5, 4, 3, 5, 7, 8, 4, 6, 8, 7, 8, 8, 6, 1, 4, 4, 4, 5, 8, 1, 2, 3, 1, 4, 5, 9, 3, 5, 7, 1, 9, 8, 4, 9, 2, 2, 5, 2, 8, 4, 7, 1, 6, 0, 5, 0, 4, 9, 2, 2, 1, 2, 4, 2, 4, 7, 0, 1, 4, 1, 2, 1, 4, 7, 8, 0, 5, 7, 3, 4, 5, 5, 1, 0, 5, 0, 0, 8, 0, 1, 9, 0, 8, 6, 9, 9, 6, 0, 3, 3, 0, 2, 7, 6, 3, 4, 7, 8, 7, 0, 8, 1, 0, 8, 1, 7, 5, 4, 5, 0, 1, 1, 9, 3, 0, 7, 1, 4, 1, 2, 2, 3, 3, 9, 0, 8, 6, 6, 3, 9, 3, 8, 3, 3, 9, 5, 2, 9, 4, 2, 5, 7, 8, 6, 9, 0, 5, 0, 7, 6, 4, 3, 1, 0, 0, 6, 3, 8, 3, 5, 1, 9, 8, 3, 4, 3, 8, 9, 3, 4, 1, 5, 9, 6, 1, 3, 1, 8, 5, 4, 3, 4, 7, 5, 4, 6, 4, 9, 5, 5, 6, 9, 7, 8, 1, 0, 3, 8, 2, 9, 3, 0, 9, 7, 1, 6, 4, 6, 5, 1, 4, 3, 8, 4, 0, 7, 0, 0, 7, 0, 7, 3, 6, 0, 4, 1, 1, 2, 3, 7, 3, 5, 9, 9, 8, 4, 3, 4, 5, 2, 2, 5, 1, 6, 1, 0, 5, 0, 7, 0, 2, 7, 0, 5, 6, 2, 3, 5, 2, 6, 6, 0, 1, 2, 7, 6, 4, 8, 4, 8, 3, 0, 8, 4, 0, 7, 6, 1, 1, 8, 3, 0, 1, 3, 0, 5, 2, 7, 9, 3, 2, 0, 5, 4, 2, 7, 4, 6, 2, 8, 6, 5, 4, 0, 3, 6, 0, 3, 6, 7, 4, 5, 3, 2, 8, 6, 5, 1, 0, 5, 7, 0, 6, 5, 8, 7, 4, 8, 8, 2, 2, 5, 6, 9, 8, 1, 5, 7, 9, 3, 6, 7, 8, 9, 7, 6, 6, 9, 7, 4, 2, 2, 0, 5, 7, 5, 0, 5, 9, 6, 8, 3, 4, 4, 0, 8, 6, 9, 7, 3, 5, 0, 2, 0, 1, 4, 1, 0, 2, 0, 6, 7, 2, 3, 5, 8, 5, 0, 2, 0, 0, 7, 2, 4, 5, 2, 2, 5, 6, 3, 2, 6, 5, 1, 3, 4, 1, 0, 5, 5, 9, 2, 4, 0, 1, 9, 0, 2, 7, 4, 2, 1, 6, 2, 4, 8, 4, 3, 9, 1, 4, 0, 3, 5, 9, 9, 8, 9, 5, 3, 5, 3, 9, 4, 5, 9, 0, 9, 4, 4, 0, 7, 0, 4, 6, 9, 1, 2, 0, 9, 1, 4, 0, 9, 3, 8, 7, 0, 0, 1, 2, 6, 4, 5, 6, 0, 0, 1, 6, 2, 3, 7, 4, 2, 8, 8, 0, 2, 1, 0, 9, 2, 7, 6, 4, 5, 7, 9, 3, 1, 0, 6, 5, 7, 9, 2, 2, 9, 5, 5, 2, 4, 9, 8, 8, 7, 2, 7, 5, 8, 4, 6, 1, 0, 1, 2, 6, 4, 8, 3, 6, 9, 9, 9, 8, 9, 2, 2, 5, 6, 9, 5, 9, 6, 8, 8, 1, 5, 9, 2, 0, 5, 6, 0, 0, 1, 0, 1, 6, 5, 5, 2, 5, 6, 3, 7, 5, 6, 7, 8 };
}
