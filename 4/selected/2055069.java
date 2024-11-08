package jaxlib.arc.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import junit.framework.Assert;
import junit.framework.TestCase;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.logging.Log;

/**
 * @author  joerg.wassmer@web.de
 * @since   JaXLib 1.0
 * @version $Id: TarTestUtils.java 2730 2009-04-21 01:12:29Z joerg_wassmer $
 */
final class TarTestUtils {

    private TarTestUtils() {
        super();
    }

    private static final Log log = Log.logger();

    private static File testArchiveFile = null;

    private static File testArchiveRoot = null;

    static final char FIRST_EXPECTED_CHAR = '0';

    private static final Set<File> filesInArchive = new HashSet<File>();

    static final boolean LONG_NAMES = true;

    private static final int COUNT_FILES = 32;

    private static int entryCount;

    static char nextExpectedChar(char prev) {
        if (prev == '9') return '-'; else if (prev == '-') return '0'; else return (char) (prev + 1);
    }

    static File getArchiveBaseDir() {
        try {
            getTestArchiveFile();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
        return TarTestUtils.testArchiveRoot;
    }

    /**
   * The first element is the base directory.
   */
    static Set<File> getFilesInArchive() {
        try {
            getTestArchiveFile();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
        return Collections.unmodifiableSet(TarTestUtils.filesInArchive);
    }

    static int getCountEntriesInTestArchive() {
        return TarTestUtils.entryCount + 1;
    }

    static int getCountFilesInTestArchive() {
        return COUNT_FILES;
    }

    static File getTestArchiveFile() throws IOException {
        if (TarTestUtils.testArchiveFile == null) {
            TarTestUtils.testArchiveFile = File.createTempFile("jaxlib.arc.tar.TarTestUtils.archiveFile", ".tar").getCanonicalFile();
        }
        if (!TarTestUtils.testArchiveFile.isFile() || (TarTestUtils.testArchiveFile.length() == 0)) {
            TarTestUtils.filesInArchive.clear();
            TarTestUtils.testArchiveRoot = File.createTempFile("jaxlib.arc.tar.TarTestUtils", null);
            TarTestUtils.testArchiveRoot.delete();
            TarTestUtils.testArchiveRoot.mkdirs();
            TarTestUtils.testArchiveRoot.deleteOnExit();
            TarTestUtils.testArchiveRoot = TarTestUtils.testArchiveRoot.getCanonicalFile();
            StringBuilder sb = new StringBuilder();
            TarTestUtils.entryCount = 0;
            for (int i = 0; i < COUNT_FILES; i++) {
                sb.setLength(0);
                File f;
                if (LONG_NAMES && ((i & 1) == 0)) {
                    while (sb.length() < 100) {
                        sb.append('d').append(i).append('/');
                        TarTestUtils.entryCount++;
                    }
                    f = new File(TarTestUtils.testArchiveRoot, sb.toString());
                    f.mkdirs();
                    sb.setLength(0);
                    f = new File(f, sb.append("file").append(i).toString());
                } else {
                    f = new File(TarTestUtils.testArchiveRoot, sb.append("file").append(i).toString());
                }
                f.deleteOnExit();
                TarTestUtils.entryCount++;
                TarTestUtils.filesInArchive.add(f);
            }
            sb = null;
            for (final File f : TarTestUtils.filesInArchive) {
                f.delete();
                final FileOutputStream out = new FileOutputStream(f);
                final long len = writeTestData(out, -1);
                out.flush();
                out.getChannel().force(true);
                out.getFD().sync();
                out.close();
            }
            TarTestUtils.testArchiveFile.delete();
            Process tarProc = Runtime.getRuntime().exec(new String[] { "tar", "-cf", TarTestUtils.testArchiveFile.getPath(), TarTestUtils.testArchiveRoot.getPath() });
            try {
                int tarExit = tarProc.waitFor();
                if (tarExit != 0) {
                    InputStream err = tarProc.getErrorStream();
                    for (int b; (b = err.read()) >= 0; ) System.out.write(b);
                    throw new RuntimeException("tar exited with error " + tarExit);
                }
                InputStream in = tarProc.getInputStream();
                for (int b; (b = in.read()) >= 0; ) ;
            } catch (final InterruptedException ex) {
                throw (InterruptedIOException) new InterruptedIOException().initCause(ex);
            } finally {
                tarProc.destroy();
            }
            TarTestUtils.filesInArchive.add(TarTestUtils.testArchiveRoot);
            testArchiveUsingTar(TarTestUtils.testArchiveFile);
        }
        return TarTestUtils.testArchiveFile;
    }

    static long testArchiveUsingTar(File f) throws IOException {
        Process tarProc = Runtime.getRuntime().exec(new String[] { "tar", "--to-stdout", "-xf", f.getAbsolutePath() });
        try {
            InputStream in = new BufferedXInputStream(tarProc.getInputStream());
            boolean failNotEquals = false;
            char expectedChar = FIRST_EXPECTED_CHAR;
            char expChar = 0;
            char gotChar = 0;
            long readed = 0;
            for (int b; ((b = in.read()) >= 0); ) {
                readed++;
                if (b == '0') {
                    expectedChar = nextExpectedChar('0');
                } else {
                    if (!failNotEquals && (expectedChar != b)) {
                        expChar = expectedChar;
                        gotChar = (char) b;
                        failNotEquals = true;
                    }
                    expectedChar = nextExpectedChar(expectedChar);
                }
            }
            Assert.assertTrue(readed >= 0);
            StringBuilder errStr = new StringBuilder();
            InputStream err = tarProc.getErrorStream();
            for (int b; (b = err.read()) >= 0; ) errStr.append((char) b);
            int tarExit = tarProc.waitFor();
            if (tarExit == 0) {
                if (errStr.length() > 0) Logger.global.warning(errStr.toString());
            } else Assert.fail("tar exited with error " + tarExit + ":\n" + errStr);
            if (failNotEquals) Assert.fail("damaged content, expected '" + expChar + "' but got '" + gotChar + "'");
            return readed;
        } catch (final InterruptedException ex) {
            throw (InterruptedIOException) new InterruptedIOException().initCause(ex);
        } finally {
            tarProc.destroy();
        }
    }

    static void testArchiveUsingTarFile(File f) throws IOException {
        TarFile tarFile = new TarFile(f);
        try {
            int entryNum = 0;
            for (TarEntry entry : tarFile.getEntries().values()) {
                entryNum++;
                if (entry.getType() == TarEntryType.NORMAL) {
                    InputStream in = tarFile.getInputStream(entry);
                    try {
                        testInputStream(entry, entryNum, in);
                    } finally {
                        in.close();
                    }
                }
            }
            tarFile.close();
            Assert.assertEquals(false, tarFile.isOpen());
        } finally {
            tarFile.close();
        }
    }

    static void testArchiveUsingTarInputStream(final File f) throws IOException {
        Assert.assertTrue(getCountEntriesInTestArchive() > 1);
        InputStream fin = new BufferedXInputStream(new FileInputStream(f));
        try {
            TarInputStream tin = new TarInputStream(fin);
            int countEntries = 0;
            for (TarEntry entry; (entry = tin.openEntry()) != null; ) {
                countEntries++;
                testInputStream(entry, countEntries, tin);
            }
            tin.close();
            Assert.assertEquals(false, tin.isOpen());
            Assert.assertEquals(getCountEntriesInTestArchive(), countEntries);
        } finally {
            fin.close();
        }
    }

    static void testInputStream(final TarEntry entry, final int entryNum, final InputStream in) throws IOException {
        entry.verify();
        char expectedChar = FIRST_EXPECTED_CHAR;
        for (long i = 0, hi = entry.getSize(); i < hi; i++) {
            final int b = in.read();
            if (b < 0) throw new java.io.EOFException();
            if (b > 0xff) throw new AssertionError(b);
            final char actual = (char) b;
            if (expectedChar != actual) Assert.fail("error at offset " + i + ": expected char '" + expectedChar + "' but got '" + actual + "' (" + (int) actual + "); size=" + hi + "; entryNum=" + entryNum + "; archive offset=" + entry.getOffset() + "; entry=\n" + entry);
            expectedChar = nextExpectedChar(expectedChar);
        }
        Assert.assertEquals(-1, in.read());
    }

    static long writeTestData(OutputStream out, int length) throws IOException {
        char c = FIRST_EXPECTED_CHAR;
        if (length < 0) length = (int) (8192 * Math.random());
        byte[] buf = new byte[length];
        for (int i = 0; i < length; i++) {
            buf[i] = (byte) c;
            c = nextExpectedChar(c);
        }
        out.write(buf);
        return length;
    }
}
