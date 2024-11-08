package jacky.lanlan.song.io;

import jacky.lanlan.song.io.IOUtils;
import jacky.lanlan.song.io.stream.NullInputStreamTest;
import jacky.lanlan.song.io.stream.YellOnCloseInputStreamTest;
import jacky.lanlan.song.io.stream.YellOnFlushAndCloseOutputStreamTest;
import java.io.*;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * JUnit tests for IOUtils copy methods.
 * 
 * @author Jeff Turner
 * @author Matthew Hawthorne
 * @author Jeremias Maerki
 * @author Stephen Colebourne
 * @version $Id: IOUtilsCopyTestCase.java 481854 2006-12-03 18:30:07Z
 *          scolebourne $
 * @see IOUtils
 */
public class IOUtilsCopyTest extends FileBasedTest {

    private static final int FILE_SIZE = 1024 * 4 + 1;

    private byte[] inData = generateTestData(FILE_SIZE);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCopy_inputStreamToOutputStream() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, false, true);
        int count = IOUtils.copy(in, out);
        assertTrue("Not all bytes were read", in.available() == 0);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_inputStreamToOutputStream_nullIn() throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy((InputStream) null, out);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_inputStreamToOutputStream_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        try {
            IOUtils.copy(in, (OutputStream) null);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    /**
   * Test Copying file > 2GB - see issue# IO-84
   */
    @Test
    public void testCopy_inputStreamToOutputStream_IO84() throws Exception {
        long size = (long) Integer.MAX_VALUE + (long) 1;
        InputStream in = new NullInputStreamTest(size);
        OutputStream out = new OutputStream() {

            @Override
            public void write(int b) throws IOException {
            }

            @Override
            public void write(byte[] b) throws IOException {
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
            }
        };
        assertEquals(-1, IOUtils.copy(in, out));
        in.close();
        assertEquals("copyLarge()", size, IOUtils.copyLarge(in, out));
    }

    @Test
    public void testCopy_inputStreamToWriter() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        YellOnFlushAndCloseOutputStreamTest out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        IOUtils.copy(in, writer);
        out.off();
        writer.flush();
        assertTrue("Not all bytes were read", in.available() == 0);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_inputStreamToWriter_nullIn() throws Exception {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        try {
            IOUtils.copy((InputStream) null, writer);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_inputStreamToWriter_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        try {
            IOUtils.copy(in, (Writer) null);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_inputStreamToWriter_Encoding() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        YellOnFlushAndCloseOutputStreamTest out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        IOUtils.copy(in, writer, "UTF8");
        out.off();
        writer.flush();
        assertTrue("Not all bytes were read", in.available() == 0);
        byte[] bytes = baout.toByteArray();
        bytes = new String(bytes, "UTF8").getBytes("US-ASCII");
        assertTrue("Content differs", Arrays.equals(inData, bytes));
    }

    @Test
    public void testCopy_inputStreamToWriter_Encoding_nullIn() throws Exception {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        try {
            IOUtils.copy((InputStream) null, writer, "UTF8");
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_inputStreamToWriter_Encoding_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        try {
            IOUtils.copy(in, (Writer) null, "UTF8");
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_inputStreamToWriter_Encoding_nullEncoding() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        YellOnFlushAndCloseOutputStreamTest out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        IOUtils.copy(in, writer, null);
        out.off();
        writer.flush();
        assertTrue("Not all bytes were read", in.available() == 0);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_readerToOutputStream() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, false, true);
        IOUtils.copy(reader, out);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_readerToOutputStream_nullIn() throws Exception {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        try {
            IOUtils.copy((Reader) null, out);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_readerToOutputStream_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        try {
            IOUtils.copy(reader, (OutputStream) null);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_readerToOutputStream_Encoding() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, false, true);
        IOUtils.copy(reader, out, "UTF16");
        byte[] bytes = baout.toByteArray();
        bytes = new String(bytes, "UTF16").getBytes("US-ASCII");
        assertTrue("Content differs", Arrays.equals(inData, bytes));
    }

    @Test
    public void testCopy_readerToOutputStream_Encoding_nullIn() throws Exception {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        try {
            IOUtils.copy((Reader) null, out, "UTF16");
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_readerToOutputStream_Encoding_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        try {
            IOUtils.copy(reader, (OutputStream) null, "UTF16");
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_readerToOutputStream_Encoding_nullEncoding() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, false, true);
        IOUtils.copy(reader, out, null);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_readerToWriter() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        YellOnFlushAndCloseOutputStreamTest out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        int count = IOUtils.copy(reader, writer);
        out.off();
        writer.flush();
        assertEquals("The number of characters returned by copy is wrong", inData.length, count);
        assertEquals("Sizes differ", inData.length, baout.size());
        assertTrue("Content differs", Arrays.equals(inData, baout.toByteArray()));
    }

    @Test
    public void testCopy_readerToWriter_nullIn() throws Exception {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        OutputStream out = new YellOnFlushAndCloseOutputStreamTest(baout, true, true);
        Writer writer = new OutputStreamWriter(baout, "US-ASCII");
        try {
            IOUtils.copy((Reader) null, writer);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    @Test
    public void testCopy_readerToWriter_nullOut() throws Exception {
        InputStream in = new ByteArrayInputStream(inData);
        in = new YellOnCloseInputStreamTest(in);
        Reader reader = new InputStreamReader(in, "US-ASCII");
        try {
            IOUtils.copy(reader, (Writer) null);
            fail();
        } catch (NullPointerException ex) {
        }
    }

    /**
   * Test Copying file > 2GB - see issue# IO-84
   */
    @Test
    public void testCopy_readerToWriter_IO84() throws Exception {
        long size = (long) Integer.MAX_VALUE + (long) 1;
        Reader reader = new NullReaderTest(size);
        Writer writer = new NullWriterTest();
        assertEquals(-1, IOUtils.copy(reader, writer));
        reader.close();
        assertEquals("copyLarge()", size, IOUtils.copyLarge(reader, writer));
    }
}
