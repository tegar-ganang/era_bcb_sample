package jaxlib.arc.bzip2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import jaxlib.io.file.Files;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.logging.Log;
import jaxlib.system.Processes;
import junit.framework.TestCase;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BZip2Test.java 3016 2011-11-28 06:17:26Z joerg_wassmer $
 */
public final class BZip2Test extends TestCase {

    private static final Log log = Log.logger();

    static final boolean haveBZip2Command = haveBZip2Command();

    private static boolean haveBZip2Command() {
        try {
            Process p = Runtime.getRuntime().exec("bzip2");
            p.destroy();
            return true;
        } catch (final IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static File[] bzip2Files = new File[9];

    private static File rawFile;

    private static File rawFileSmall;

    private static File bzip2Small;

    public static void assertFileContentEquals(File expectedFile, File actualFile) throws IOException {
        final long size = expectedFile.length();
        assertEquals("file size", size, actualFile.length());
        BufferedInputStream expectedIn = null;
        BufferedInputStream actualIn = null;
        try {
            expectedIn = new BufferedInputStream(new FileInputStream(expectedFile));
            actualIn = new BufferedInputStream(new FileInputStream(actualFile));
            for (long i = 0; i < size; i++) {
                int a = expectedIn.read();
                int b = actualIn.read();
                if (a != b) {
                    fail("mismatch at offset " + i + ": expected byte 0x" + Integer.toHexString(a) + " but got 0x" + Integer.toHexString(b) + "\nexpectedFile=" + expectedFile + "\nactualFile=" + actualFile);
                }
            }
        } finally {
            if (expectedIn != null) expectedIn.close();
            if (actualIn != null) actualIn.close();
        }
    }

    public static void assertStreamContentEquals(InputStream expectedIn, InputStream actualIn) throws IOException {
        for (long i = 0; true; i++) {
            int a = expectedIn.read();
            int b = actualIn.read();
            if (a != b) {
                fail("mismatch at offset " + i + ": expected byte 0x" + Integer.toHexString(a) + " but got 0x" + Integer.toHexString(b) + "\nexpectedIn=" + expectedIn + "\nactualIn=" + actualIn);
            }
            if (a < 0) break;
        }
    }

    /**
   * Used by BZip2InputStreamTest and BZip2OutputStreamTest
   */
    static File getRawFile() throws IOException {
        File rawFile = BZip2Test.rawFile;
        if ((rawFile != null) && rawFile.isFile()) return rawFile;
        rawFile = File.createTempFile("jaxlib-bzip2-test", ".raw").getAbsoluteFile();
        rawFile.deleteOnExit();
        final int size = 12345678;
        RandomAccessFile rf = new RandomAccessFile(rawFile, "rw");
        rf.setLength(size);
        rf.close();
        FileOutputStream fout = new FileOutputStream(rawFile, false);
        BufferedXOutputStream out = new BufferedXOutputStream(fout);
        int written = 0;
        for (int i = 0; i <= 255; i++) {
            for (int j = (100 + (i & 1)); --j >= 0; ) {
                out.write(i);
                written++;
            }
        }
        while (written % 4 != 0) out.write(written++);
        final Random r = new Random();
        while (written < size) {
            out.writeInt(written);
            out.writeInt(written);
            written += 8;
        }
        out.close();
        BZip2Test.rawFile = rawFile;
        log.info(rawFile.length());
        return rawFile;
    }

    private static File getRawFileSmall() throws IOException {
        File rawFile = BZip2Test.rawFileSmall;
        if ((rawFile != null) && rawFile.isFile()) return rawFile;
        rawFile = File.createTempFile("jaxlib-bzip2-test", ".raw").getAbsoluteFile();
        rawFile.deleteOnExit();
        FileOutputStream fout = new FileOutputStream(rawFile);
        BufferedXOutputStream out = new BufferedXOutputStream(fout);
        for (int i = Integer.MAX_VALUE - 1000; i < Integer.MAX_VALUE; i++) {
            out.writeInt(i);
        }
        out.close();
        BZip2Test.rawFileSmall = rawFile;
        return rawFile;
    }

    /**
   * Used by BZip2InputStreamTest and BZip2OutputStreamTest
   */
    static File getBZip2File(int blockSize) throws IOException {
        if (!haveBZip2Command) throw new RuntimeException("command 'bzip2' unavailable");
        return getBZip2File(blockSize, false);
    }

    private static File getBZip2FileSmall() throws IOException {
        return getBZip2File(1, true);
    }

    private static File getBZip2File(int blockSize, boolean small) throws IOException {
        File bzip2File = small ? BZip2Test.bzip2Small : BZip2Test.bzip2Files[blockSize - 1];
        if ((bzip2File != null) && bzip2File.isFile()) return bzip2File;
        File rawFile = small ? getRawFileSmall() : getRawFile();
        try {
            Thread.sleep(5000);
        } catch (final Exception ex) {
            throw new Error(ex);
        }
        Process p = Runtime.getRuntime().exec(new String[] { "bzip2", "-z", "-k", "-" + String.valueOf(blockSize), rawFile.getPath() });
        int exitValue = Processes.execute(p, (Appendable) System.out);
        if (exitValue != 0) throw new RuntimeException("native bzip2 command exited with value " + exitValue);
        String s = rawFile.getPath();
        File bzip2File0 = new File(rawFile.getPath() + ".bz2");
        if (!bzip2File0.isFile()) throw new RuntimeException("unable to determine output file of bzip2 command");
        bzip2File = new File(bzip2File0.getParent(), "jaxlib-bzip2-test-blocksize" + blockSize + ".bz2");
        bzip2File0.renameTo(bzip2File);
        bzip2File.deleteOnExit();
        if (small) BZip2Test.bzip2Small = bzip2File; else BZip2Test.bzip2Files[blockSize - 1] = bzip2File;
        return bzip2File;
    }

    public BZip2Test(String name) {
        super(name);
    }

    public void testCompress() throws InterruptedException, IOException {
        File rawFile = getRawFileSmall();
        File zipped = new File(rawFile + ".bz2");
        zipped.deleteOnExit();
        BZip2.main(new String[] { "-z", "-k", "-f", rawFile.getPath() });
        assertTrue(zipped.isFile());
        long modTime = zipped.lastModified();
        Thread.sleep(5000);
        BufferedInputStream expectIn = new BufferedInputStream(new FileInputStream(rawFile));
        FileInputStream fin = Files.openLockedFileInputStream(zipped);
        fin.getFD().sync();
        fin.getChannel().force(true);
        log.info(zipped + ": " + zipped.length());
        BZip2InputStream actualIn = new BZip2InputStream(new BufferedXInputStream(fin));
        try {
            assertStreamContentEquals(expectIn, actualIn);
        } finally {
            expectIn.close();
            actualIn.close();
        }
        Thread.sleep(1000);
        BZip2.main(new String[] { "-zkf", rawFile.getPath() });
        assertTrue(zipped.isFile());
        assertFalse(zipped.lastModified() == modTime);
        modTime = zipped.lastModified();
        BZip2.main(new String[] { "-z", "-k", rawFile.getPath() });
        assertTrue(zipped.isFile());
        assertEquals(modTime, zipped.lastModified());
        BZip2.main(new String[] { "-z", "-f", rawFile.getPath() });
        assertFalse(rawFile.isFile());
        assertTrue(zipped.isFile());
        zipped.delete();
    }

    public void testDecompress() throws IOException {
        File bz = getBZip2FileSmall();
        File original = getRawFileSmall();
        File unzipped = new File(bz.getPath().substring(0, bz.getPath().length() - ".bz2".length()));
        unzipped.deleteOnExit();
        BZip2.main(new String[] { "-d", "-t", bz.getPath() });
        assertFalse(unzipped.isFile());
        assertTrue(bz.isFile());
        BZip2.main(new String[] { "--decompress", "--test", bz.getPath() });
        assertFalse(unzipped.isFile());
        assertTrue(bz.isFile());
        BZip2.main(new String[] { "-dfk", bz.getPath() });
        assertTrue(unzipped.isFile());
        assertTrue(bz.isFile());
        assertFileContentEquals(original, unzipped);
        unzipped.delete();
        BZip2.main(new String[] { "-h", "-d", "--force", bz.getPath() });
        assertFalse(unzipped.isFile());
        assertTrue(bz.isFile());
        BZip2.main(new String[] { "-d", "--force", bz.getPath() });
        assertTrue(unzipped.isFile());
        assertFalse(bz.isFile());
        unzipped.delete();
    }

    public void test_calgary_bib() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/bib");
    }

    public void test_calgary_book1() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/book1");
    }

    public void test_calgary_book2() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/book2");
    }

    public void test_calgary_geo() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/geo");
    }

    public void test_calgary_news() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/news");
    }

    public void test_calgary_obj1() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/obj1");
    }

    public void test_calgary_obj2() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/obj2");
    }

    public void test_calgary_paper1() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper1");
    }

    public void test_calgary_paper2() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper2");
    }

    public void test_calgary_paper3() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper3");
    }

    public void test_calgary_paper4() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper4");
    }

    public void test_calgary_paper5() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper5");
    }

    public void test_calgary_paper6() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/paper6");
    }

    public void test_calgary_pic() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/pic");
    }

    public void test_calgary_progc() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/progc");
    }

    public void test_calgary_progl() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/progl");
    }

    public void test_calgary_progp() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/progp");
    }

    public void test_calgary_trans() throws IOException {
        runTestOnResource("/jaxlib/arc/resources/calgary/trans");
    }

    private void runTestOnResource(String resourceName) throws IOException {
        for (int i = 1; i <= 9; i++) {
            File tempFile = File.createTempFile("test", null);
            tempFile.deleteOnExit();
            try {
                long rawSize = runTestOnResource(resourceName, i, tempFile);
                if (rawSize < (i * 100000)) break;
            } finally {
                tempFile.delete();
            }
        }
    }

    private long runTestOnResource(String resourceName, int blockSize100k, File file) throws IOException {
        BufferedXInputStream raw = new BufferedXInputStream(getClass().getResourceAsStream(resourceName));
        BZip2OutputStream out = new BZip2OutputStream(new BufferedXOutputStream(new FileOutputStream(file)), blockSize100k);
        long rawSize;
        try {
            rawSize = raw.transferTo(out, -1);
        } finally {
            out.close();
            raw.close();
        }
        out = null;
        log.info("\nresource            = " + resourceName + "\nblock size (100k)   = " + blockSize100k + "\noriginal size       = " + rawSize + "\ncompressed size     = " + file.length() + "\ncompressed/original = " + ((double) file.length() / (double) rawSize));
        BZip2InputStream in = new BZip2InputStream(new BufferedXInputStream(new FileInputStream(file)));
        raw = new BufferedXInputStream(getClass().getResourceAsStream(resourceName));
        try {
            for (int b; (b = raw.read()) >= 0; ) assertEquals(b, in.read());
            assertEquals(-1, in.read());
        } finally {
            in.close();
            raw.close();
        }
        return rawSize;
    }
}
