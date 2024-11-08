package xbird.storage.io;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import junitx.framework.ArrayAssert;
import org.junit.Test;

public class SegmentsTest extends TestCase {

    public SegmentsTest() {
        super(SegmentsTest.class.getName());
    }

    @Test
    public void testFixedSegmentWrite() throws IOException {
        File tmpFile = new File("testFixedSegmentWrite.tmp");
        tmpFile.deleteOnExit();
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        System.out.println("tmpFile: " + tmpFile.getAbsolutePath());
        FixedSegments segment = new FixedSegments(tmpFile, 3);
        byte[] b0 = new byte[] { 2, 2, 23 };
        segment.write(0, b0);
        byte[] b1 = new byte[] { 2, 1, 24 };
        segment.write(1, b1);
        byte[] b3 = new byte[] { 2, 43, 1 };
        segment.write(3, b3);
        byte[] b2 = new byte[] { 1, 3, 3 };
        segment.write(2, b2);
        byte[] b4 = new byte[] { 6, 1 };
        segment.write(4, b4);
        byte[] b5 = new byte[] { 9, 3, 4 };
        segment.write(5, b5);
        segment.flush(true);
        FixedSegments readSegment = new FixedSegments(tmpFile, 3);
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b1, readSegment.read(1));
        ArrayAssert.assertEquals(b3, readSegment.read(3));
        ArrayAssert.assertEquals(b2, readSegment.read(2));
        ArrayAssert.assertEquals(b4, readSegment.read(4));
        ArrayAssert.assertEquals(b5, readSegment.read(5));
        tmpFile.delete();
    }

    @Test
    public void testVSegmentsWrite() throws IOException {
        File tmpFile = new File("testVSegmentsWrite.tmp");
        tmpFile.deleteOnExit();
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        System.out.println("tmpFile: " + tmpFile.getAbsolutePath());
        VarSegments segment = new VarSegments(tmpFile);
        byte[] b0 = new byte[] { 2, 2, 23 };
        segment.write(0, b0);
        byte[] b1 = new byte[] { 2, 1, 24 };
        segment.write(1, b1);
        byte[] b3 = new byte[] { 2, 43, 1 };
        segment.write(3, b3);
        byte[] b2 = new byte[] { 1, 3, 3 };
        segment.write(2, b2);
        byte[] b4 = new byte[] { 6, 1 };
        segment.write(4, b4);
        byte[] b5 = new byte[] { 9, 3, 4 };
        segment.write(5, b5);
        segment.flush(true);
        VarSegments readSegment = new VarSegments(tmpFile);
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b1, readSegment.read(1));
        ArrayAssert.assertEquals(b3, readSegment.read(3));
        ArrayAssert.assertEquals(b2, readSegment.read(2));
        ArrayAssert.assertEquals(b4, readSegment.read(4));
        ArrayAssert.assertEquals(b5, readSegment.read(5));
        tmpFile.delete();
    }

    @Test
    public void testVSegmentsWriteUpdate() throws IOException {
        File tmpFile = new File("testVSegmentsWriteUpdate.tmp");
        tmpFile.deleteOnExit();
        if (tmpFile.exists()) {
            assertTrue("delete file failed: " + tmpFile.getAbsolutePath(), tmpFile.delete());
        }
        System.out.println("tmpFile: " + tmpFile.getAbsolutePath());
        VarSegments segment = new VarSegments(tmpFile);
        byte[] b0 = new byte[] { 2, 2, 23 };
        segment.write(0, b0);
        byte[] b1 = new byte[] { 2, 1, 24 };
        segment.write(1, b1);
        byte[] b3 = new byte[] { 2, 43, 1 };
        segment.write(3, b3);
        byte[] b2 = new byte[] { 1, 3, 3 };
        segment.write(2, b2);
        segment.write(2, b3);
        byte[] b4 = new byte[] { 6, 1 };
        segment.write(4, b4);
        byte[] b5 = new byte[] { 9, 3, 4 };
        segment.write(5, b5);
        segment.write(5, b4);
        segment.flush(true);
        VarSegments readSegment = new VarSegments(tmpFile);
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b0, readSegment.read(0));
        ArrayAssert.assertEquals(b1, readSegment.read(1));
        ArrayAssert.assertEquals(b3, readSegment.read(3));
        ArrayAssert.assertEquals(b3, readSegment.read(2));
        ArrayAssert.assertEquals(b4, readSegment.read(4));
        readSegment.write(4, b3);
        ArrayAssert.assertEquals(b3, readSegment.read(4));
        ArrayAssert.assertEquals(b4, readSegment.read(5));
        tmpFile.delete();
    }
}
