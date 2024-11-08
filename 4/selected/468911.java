package jaxlib.arc.bzip2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import junit.framework.TestCase;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.system.Processes;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BZip2OutputStreamTest.java 2548 2008-03-10 11:51:17Z joerg_wassmer $
 */
public final class BZip2OutputStreamTest extends TestCase {

    public BZip2OutputStreamTest(String name) {
        super(name);
    }

    private void runTestForBlockSize(int blockSize) throws IOException {
        if (!BZip2Test.haveBZip2Command) {
            Logger.global.warning("Test skipped, native bzip2 command unavailable");
            return;
        }
        File rawFile = BZip2Test.getRawFile();
        File bzFile = File.createTempFile("jaxlib-bzip2-outputstream-blocksize" + blockSize, ".bz2");
        bzFile.deleteOnExit();
        BZip2OutputStream out = new BZip2OutputStream(new FileOutputStream(bzFile), blockSize);
        FileInputStream in = new FileInputStream(rawFile);
        try {
            out.transferFrom(in, -1);
        } finally {
            in.close();
            out.close();
        }
        Process p = Runtime.getRuntime().exec(new String[] { "bzip2", "-d", "-k", "-" + String.valueOf(blockSize), bzFile.getPath() });
        int exitValue = Processes.execute(p, (Appendable) System.out);
        assertEquals("bzip2 command exit value", 0, exitValue);
        File decompressedFile = new File(bzFile.getPath().substring(0, bzFile.getPath().length() - 4));
        if (!decompressedFile.isFile()) throw new RuntimeException("unable to find outputfile of bzip2 command");
        decompressedFile.deleteOnExit();
        assertEquals(rawFile.length(), decompressedFile.length());
        BufferedXInputStream expectIn = new BufferedXInputStream(new FileInputStream(rawFile));
        BufferedXInputStream gotIn = new BufferedXInputStream(new FileInputStream(decompressedFile));
        try {
            BZip2Test.assertStreamContentEquals(expectIn, gotIn);
        } finally {
            expectIn.close();
            gotIn.close();
        }
        Logger.global.info("rawFile: " + rawFile.length() + "; bzip: " + bzFile.length());
    }

    public void testBlockSize1() throws IOException {
        runTestForBlockSize(1);
    }

    public void testBlockSize2() throws IOException {
        runTestForBlockSize(2);
    }

    public void testBlockSize3() throws IOException {
        runTestForBlockSize(3);
    }

    public void testBlockSize4() throws IOException {
        runTestForBlockSize(4);
    }

    public void testBlockSize5() throws IOException {
        runTestForBlockSize(5);
    }

    public void testBlockSize6() throws IOException {
        runTestForBlockSize(6);
    }

    public void testBlockSize7() throws IOException {
        runTestForBlockSize(7);
    }

    public void testBlockSize8() throws IOException {
        runTestForBlockSize(8);
    }

    public void testBlockSize9() throws IOException {
        runTestForBlockSize(9);
    }

    public void testEmptyFile() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        BZip2OutputStream out = new BZip2OutputStream(bout);
        out.close();
        BZip2Test.assertStreamContentEquals(new ByteArrayInputStream(new byte[0]), new BZip2InputStream(new ByteArrayInputStream(bout.toByteArray())));
    }
}
