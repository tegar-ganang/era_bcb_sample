package jaxlib.io.stream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.jaxlib.junit.XTestCase;

/**
 * TODO: comment
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: RandomAccessFilesTest.java 1044 2004-04-06 16:37:29Z joerg_wassmer $
 */
public final class RandomAccessFilesTest extends XTestCase {

    private static SoftReference<byte[]> testData;

    private static SoftReference<RandomAccessFile> testFile;

    private static final int DATA_LENGTH = 1000000;

    public static void main(String[] args) {
        runSuite(RandomAccessFilesTest.class);
    }

    private static boolean contentEquals(RandomAccessFile f, byte[] a) throws IOException {
        return f.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, DATA_LENGTH).equals(ByteBuffer.wrap(a));
    }

    private static byte[] data() {
        byte[] a = testData == null ? null : testData.get();
        if (a == null) {
            a = new byte[DATA_LENGTH];
            byte b = Byte.MIN_VALUE;
            for (int i = DATA_LENGTH; --i >= 0; b++) a[i] = b;
            testData = new SoftReference(a);
        }
        return a;
    }

    private static RandomAccessFile file() throws IOException {
        RandomAccessFile a = testFile == null ? null : testFile.get();
        if (a == null) {
            File f = File.createTempFile("RandomAccessFileTest", ".tmp");
            f.deleteOnExit();
            a = new RandomAccessFile(f, "rw");
            testFile = new SoftReference(a);
        }
        a.setLength(DATA_LENGTH);
        a.seek(0);
        a.write(data());
        a.seek(0);
        return a;
    }

    public RandomAccessFilesTest(String name) {
        super(name);
    }

    public void testCopy() throws IOException {
        implCopyIntern(1000);
        implCopyIntern(100000);
    }

    private void implCopyIntern(int len) throws IOException {
        byte[] a = data();
        RandomAccessFile f = file();
        int from = 0;
        int to = len;
        System.arraycopy(a, from, a, to, len);
        RandomAccessFiles.copy(f, from, f, to, len);
        assertTrue(contentEquals(f, a));
        from = len;
        to = 0;
        System.arraycopy(a, from, a, to, len);
        RandomAccessFiles.copy(f, from, f, to, len);
        assertTrue(contentEquals(f, a));
        from = 0;
        to = len >> 1;
        System.arraycopy(a, from, a, to, len);
        RandomAccessFiles.copy(f, from, f, to, len);
        assertTrue(contentEquals(f, a));
    }
}
