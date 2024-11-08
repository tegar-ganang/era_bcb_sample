package com.gc.iotools.stream.reader;

import static org.junit.Assert.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullWriter;
import com.gc.iotools.stream.utils.SlowWriter;

public class TestReaderWriterTee {

    @org.junit.Test
    public void testEnableCopy() throws Exception {
        final String reference = "1234567890abcde";
        final StringWriter wrappedWriter = new StringWriter();
        final TeeReaderWriter teeStream = new TeeReaderWriter(new StringReader(reference), wrappedWriter);
        final char[] cbuf = new char[5];
        teeStream.read(cbuf);
        assertEquals("first 5 char", "12345", new String(cbuf));
        teeStream.enableCopy(false);
        teeStream.read(cbuf);
        assertEquals("5-10 char", "67890", new String(cbuf));
        teeStream.enableCopy(true);
        teeStream.close();
        final String innerContent = wrappedWriter.toString();
        assertEquals("Content dumped", "12345abcde", innerContent);
    }

    @org.junit.Test
    public void testMarkAndReset() throws Exception {
        final BigDocumentReader bis = new BigDocumentReader(131072);
        final String reference = new String(IOUtils.toCharArray(bis));
        bis.resetToBeginning();
        final StringWriter osString = new StringWriter();
        final Reader teeStream = new TeeReaderWriter(bis, osString);
        teeStream.read(new char[5]);
        teeStream.mark(50);
        teeStream.read(new char[25]);
        teeStream.reset();
        teeStream.close();
        final String result = osString.toString();
        assertEquals("Arrays are equal", reference, result);
    }

    @org.junit.Test
    public void testMultipleStreams() throws Exception {
        final BigDocumentReader bis = new BigDocumentReader(2048);
        final String reference = new String(IOUtils.toCharArray(bis));
        bis.resetToBeginning();
        final Writer osString[] = { new StringWriter(), new StringWriter(), new SlowWriter(105, new StringWriter()) };
        final TeeReaderWriter teeStream = new TeeReaderWriter(bis, true, osString);
        teeStream.close();
        for (final Writer stringWriter : osString) {
            final String result;
            if (stringWriter instanceof StringWriter) {
                result = ((StringWriter) stringWriter).toString();
            } else {
                result = ((StringWriter) ((SlowWriter) stringWriter).getRawStream()).toString();
            }
            assertEquals("Arrays equal", reference, result);
        }
        final long[] wtime = teeStream.getWriteTime();
        assertEquals("array length", 3, wtime.length);
        assertTrue("Time stream 1 less 100 ms [" + wtime[0] + "]", 100 > wtime[0]);
        assertTrue("Time stream 3 more 100 ms", 100 < wtime[2]);
    }

    @org.junit.Test
    public void testReadAtSpecificPosition() throws Exception {
        final char[] referenceString = "123".toCharArray();
        final StringReader bais = new StringReader(new String(referenceString));
        final StringWriter baos = new StringWriter();
        final Reader teeStream = new TeeReaderWriter(bais, baos);
        final char[] readBuffer = new char[256];
        Arrays.fill(readBuffer, 'i');
        teeStream.read(readBuffer, 1, 128);
        final char[] referenceBuffer = new char[256];
        Arrays.fill(referenceBuffer, 'i');
        referenceBuffer[1] = '1';
        referenceBuffer[2] = '2';
        referenceBuffer[3] = '3';
        assertArrayEquals("Strings are equal", referenceBuffer, readBuffer);
    }

    @org.junit.Test
    public void testReadWrite() throws Exception {
        final String reference = "testString";
        final Reader reader = new StringReader(reference);
        final StringWriter osString = new StringWriter();
        final Reader teeStream = new TeeReaderWriter(reader, osString);
        IOUtils.copy(teeStream, new NullWriter());
        teeStream.close();
        osString.toString();
    }

    @org.junit.Test
    public void testSuddenClose() throws Exception {
        final String testBytes = "testString";
        final Reader reader = new StringReader(testBytes);
        final StringWriter osString = new StringWriter();
        final Reader teeStream = new TeeReaderWriter(reader, osString);
        teeStream.close();
        final String result = osString.toString();
        assertEquals("Strings are equal", testBytes, result);
    }
}
