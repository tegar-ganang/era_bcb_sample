package com.gc.iotools.stream.is;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import com.gc.iotools.stream.utils.SlowOutputStream;

public class TestInputStreamOutputStreamTee {

    @org.junit.Test
    public void testEnableCopy() throws Exception {
        final BigDocumentIstream bis = new BigDocumentIstream(1024);
        final byte[] reference = IOUtils.toByteArray(bis);
        bis.resetToBeginning();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final TeeInputStreamOutputStream teeStream = new TeeInputStreamOutputStream(bis, bos);
        teeStream.read(new byte[5]);
        teeStream.enableCopy(false);
        teeStream.read(new byte[24]);
        teeStream.enableCopy(true);
        teeStream.close();
        final byte[] result = bos.toByteArray();
        final byte[] reference1 = new byte[1000];
        System.arraycopy(reference, 0, reference1, 0, 5);
        System.arraycopy(reference, 29, reference1, 5, 995);
        assertArrayEquals("Arrays equal", reference1, result);
    }

    @org.junit.Test
    public void testMarkAndReset() throws Exception {
        final BigDocumentIstream bis = new BigDocumentIstream(131072);
        final byte[] reference = IOUtils.toByteArray(bis);
        bis.resetToBeginning();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final InputStream teeStream = new TeeInputStreamOutputStream(bis, bos);
        teeStream.read(new byte[5]);
        teeStream.mark(50);
        teeStream.read(new byte[25]);
        teeStream.reset();
        teeStream.close();
        final byte[] result = bos.toByteArray();
        assertArrayEquals("Arrays equal", reference, result);
    }

    @org.junit.Test
    public void testMultipleStreams() throws Exception {
        final BigDocumentIstream bis = new BigDocumentIstream(2048);
        final byte[] reference = IOUtils.toByteArray(bis);
        bis.resetToBeginning();
        final OutputStream bos[] = { new ByteArrayOutputStream(), new ByteArrayOutputStream(), new SlowOutputStream(105, new ByteArrayOutputStream()) };
        final TeeInputStreamOutputStream teeStream = new TeeInputStreamOutputStream(bis, true, bos);
        teeStream.close();
        for (final OutputStream byteArrayOutputStream : bos) {
            final byte[] result;
            if (byteArrayOutputStream instanceof ByteArrayOutputStream) {
                result = ((ByteArrayOutputStream) byteArrayOutputStream).toByteArray();
            } else {
                result = ((ByteArrayOutputStream) ((SlowOutputStream) byteArrayOutputStream).getRawStream()).toByteArray();
            }
            assertArrayEquals("Arrays equal", reference, result);
        }
        final long[] wtime = teeStream.getWriteTime();
        assertEquals("array length", 3, wtime.length);
        assertTrue("Time stream 1 less 100 ms [" + wtime[0] + "]", 100 > wtime[0]);
        assertTrue("Time stream 3 more 100 ms", 100 < wtime[2]);
    }

    @org.junit.Test
    public void testReadAtSpecificPosition() throws Exception {
        final byte[] referenceBytes = "123".getBytes();
        final ByteArrayInputStream bais = new ByteArrayInputStream(referenceBytes);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream teeStream = new TeeInputStreamOutputStream(bais, baos);
        final byte[] readBytes = new byte[512];
        Arrays.fill(readBytes, ((byte) 0));
        teeStream.read(readBytes, 1, 256);
        assertTrue("Array uguali", Arrays.equals(referenceBytes, baos.toByteArray()));
    }

    @org.junit.Test
    public void testReadWrite() throws Exception {
        final byte[] testBytes = "testString".getBytes();
        final InputStream istream = new ByteArrayInputStream(testBytes);
        final ByteArrayOutputStream destination = new ByteArrayOutputStream();
        final InputStream teeStream = new TeeInputStreamOutputStream(istream, destination);
        IOUtils.copy(teeStream, new NullOutputStream());
        teeStream.close();
        assertArrayEquals("array are equals", testBytes, destination.toByteArray());
    }

    @org.junit.Test
    public void testSuddenClose() throws Exception {
        final byte[] referenceBytes = "testString".getBytes();
        final InputStream istream = new ByteArrayInputStream(referenceBytes);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final InputStream teeStream = new TeeInputStreamOutputStream(istream, bos);
        teeStream.close();
        final byte[] result = bos.toByteArray();
        assertArrayEquals("Arrays equal", referenceBytes, result);
    }
}
