package org.apache.commons.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

/**
 * @author Lyor G.
 * @since Nov 23, 2011 10:00:56 AM
 */
public class ExtraIOUtilsTest extends AbstractTestSupport {

    private final byte[] TEST_DATA = new byte[ExtraIOUtils.DEFAULT_BUFFER_SIZE];

    public ExtraIOUtilsTest() {
        RANDOMIZER.nextBytes(TEST_DATA);
    }

    @Test
    public void testExactCopySize() throws IOException {
        final int size = Byte.SIZE + RANDOMIZER.nextInt(TEST_DATA.length - Long.SIZE);
        final InputStream in = new ByteArrayInputStream(TEST_DATA);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        final int cpySize = ExtraIOUtils.copy(in, out, size);
        assertEquals("Mismatched copy size", size, cpySize);
        final byte[] subArray = ArrayUtils.subarray(TEST_DATA, 0, size), outArray = out.toByteArray();
        assertArrayEquals("Mismatched data", subArray, outArray);
    }

    @Test
    public void testCopyOverSize() throws IOException {
        final InputStream in = new ByteArrayInputStream(TEST_DATA);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(TEST_DATA.length);
        final int cpySize = ExtraIOUtils.copy(in, out, TEST_DATA.length + Long.SIZE);
        assertEquals("Mismatched copy size", TEST_DATA.length, cpySize);
        final byte[] outArray = out.toByteArray();
        assertArrayEquals("Mismatched data", TEST_DATA, outArray);
    }

    @Test
    public void testCopyUnknownSize() throws IOException {
        final InputStream in = new ByteArrayInputStream(TEST_DATA);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(TEST_DATA.length);
        final int cpySize = ExtraIOUtils.copy(in, out, (-1));
        assertEquals("Mismatched copy size", TEST_DATA.length, cpySize);
        final byte[] outArray = out.toByteArray();
        assertArrayEquals("Mismatched data", TEST_DATA, outArray);
    }
}
