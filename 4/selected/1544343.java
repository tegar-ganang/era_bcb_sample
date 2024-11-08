package test.net.sf.japi.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.sf.japi.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link IOUtils}.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class IOUtilsTest {

    /** Tests that {@link IOUtils#copy(InputStream, OutputStream)} works.
     * @throws IOException (unexpected)
     */
    @Test
    public void testCopy() throws IOException {
        final byte[] input = { 0x00, 0x01, 0x7F, 0x03, 0x40 };
        final byte[] verification = input.clone();
        Assert.assertNotSame("Expecting verification to be a new array.", input, verification);
        final ByteArrayInputStream in = new ByteArrayInputStream(input);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        final byte[] output = out.toByteArray();
        Assert.assertTrue("Expecting input to be unchanged.", Arrays.equals(verification, input));
        Assert.assertTrue("Expecting output to be like input.", Arrays.equals(verification, output));
        Assert.assertNotSame("Expecting output to be a new array.", input, output);
        Assert.assertNotSame("Expecting output to be a new array.", verification, output);
    }

    /** Tests that {@link IOUtils#lines(Reader)} works.
     * @throws IOException (unexpected)
     */
    @Test
    public void testLines() throws IOException {
        final String input = "line1\nline2\nline3";
        final Reader in = new StringReader(input);
        final Iterable<String> lineIterable = IOUtils.lines(in);
        final Iterator<String> lineIterator = lineIterable.iterator();
        Assert.assertTrue(lineIterator.hasNext());
        Assert.assertEquals("line1", lineIterator.next());
        try {
            lineIterator.remove();
            Assert.fail();
        } catch (final UnsupportedOperationException ignore) {
        }
        Assert.assertTrue(lineIterator.hasNext());
        Assert.assertEquals("line2", lineIterator.next());
        Assert.assertTrue(lineIterator.hasNext());
        Assert.assertEquals("line3", lineIterator.next());
        Assert.assertFalse(lineIterator.hasNext());
        try {
            lineIterator.next();
            Assert.fail();
        } catch (final NoSuchElementException ignore) {
        }
    }
}
